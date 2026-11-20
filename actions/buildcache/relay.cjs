'use strict';
const http = require('node:http');
const crypto = require('node:crypto');
const fs = require('node:fs');

function tokenSource(env, fetcher = fetch) {
  let cached, pending;
  return async () => {
    if (cached && cached.expires > Date.now() + 60000) return cached.token;
    if (!pending) pending = (async () => {
      const url = new URL(env.ACTIONS_ID_TOKEN_REQUEST_URL);
      if (url.protocol !== 'https:' || !url.hostname.endsWith('.actions.githubusercontent.com') || url.username || url.password) throw Error('OIDC transport must use TLS');
      url.searchParams.set('audience', 'elide-build-cache');
      const response = await fetcher(url, {
        headers: {Authorization: `Bearer ${env.ACTIONS_ID_TOKEN_REQUEST_TOKEN}`},
        signal: AbortSignal.timeout(10000), redirect: 'error',
      });
      if (!response.ok) throw Error('GitHub OIDC request failed');
      const {value} = await response.json();
      const payload = JSON.parse(Buffer.from(value.split('.')[1], 'base64url'));
      if (!Number.isFinite(payload.exp) || payload.exp * 1000 <= Date.now() + 30000) throw Error('Expired OIDC token');
      cached = {token: value, expires: payload.exp * 1000};
      return value;
    })().finally(() => {pending = undefined;});
    return pending;
  };
}

function createRelay({endpoint, password, namespace, getToken, timeout = 120000, shutdown = () => {}}) {
  const target = new URL(endpoint);
  if (!['http:', 'https:'].includes(target.protocol) || target.username || target.password || target.search || target.hash || target.pathname !== '/') throw Error('Invalid cache origin');
  const transport = target.protocol === 'https:' ? require('node:https') : http;
  const expected = Buffer.from(`Basic ${Buffer.from(`elide:${password}`).toString('base64')}`);
  return http.createServer(async (req, res) => {
    const actual = Buffer.from(req.headers.authorization || '');
    if (actual.length !== expected.length || !crypto.timingSafeEqual(actual, expected)) {res.writeHead(401);res.end();return;}
    if (req.method === 'POST' && req.url === '/_shutdown') {res.writeHead(200);res.end();shutdown();return;}
    const metadata = ['PROPFIND', 'MKCOL'].includes(req.method);
    const validPath = metadata ? /^\/(gradle|sccache)\/[A-Za-z0-9/_+=.-]*$/.test(req.url) : /^\/(gradle|sccache)\/[A-Za-z0-9/_+=.-]+$/.test(req.url);
    if (!['GET', 'HEAD', 'PUT', 'PROPFIND', 'MKCOL'].includes(req.method) || !validPath || req.url.includes('..') || (req.method === 'PROPFIND' && req.headers.depth !== '0')) {res.writeHead(400);res.end();return;}
    let token;
    try {token = await getToken();} catch {req.resume();res.writeHead(503);res.end('Cache identity unavailable');return;}
    // Fixed origin and a strict path allowlist prevent forwarding job identity
    // to caller-selected hosts. Redirects are returned, never followed.
    const headers = {'authorization': `Bearer ${token}`, 'x-elide-cache-namespace': namespace};
    for (const name of ['content-length', 'content-type', 'depth']) if (req.headers[name]) headers[name] = req.headers[name];
    const upstream = transport.request(target, {path: req.url, method: req.method, headers}, response => {
      if (response.statusCode >= 300 && response.statusCode < 400) {response.resume();res.writeHead(502);res.end('Cache redirects are forbidden');return;}
      const outbound = {};
      for (const name of ['content-length', 'content-type', 'etag', 'cache-control']) if (response.headers[name]) outbound[name] = response.headers[name];
      res.writeHead(response.statusCode, outbound);response.pipe(res);
      response.on('error', () => res.destroy());
    });
    upstream.on('socket', socket => {
      if (socket.connecting) {
        const timer = setTimeout(() => upstream.destroy(), 2000);
        timer.unref();socket.once('connect', () => clearTimeout(timer));socket.once('close', () => clearTimeout(timer));
      }
    });
    upstream.setTimeout(timeout, () => upstream.destroy());
    upstream.on('error', () => {
      if (res.headersSent) res.destroy();
      // Reads degrade to a cache miss; the marker lets setup tell an outage from a real miss.
      else {res.writeHead(['GET', 'HEAD'].includes(req.method) ? 404 : 503, {'x-elide-relay-error': 'upstream'});res.end();}
    });
    req.on('aborted', () => upstream.destroy());
    res.on('close', () => {if (!res.writableEnded) upstream.destroy();});
    req.pipe(upstream);
  });
}

if (require.main === module) {
  const state = "relay.json";
  const {endpoint, password, namespace} = JSON.parse(fs.readFileSync(state, 'utf8'));
  const getToken = tokenSource(process.env);
  getToken().then(() => {
    const server = createRelay({endpoint, password, namespace, getToken, shutdown: () => {
      server.close(() => process.exit(0));
      setTimeout(() => process.exit(0), 5000).unref();
    }});
    server.listen(0, '127.0.0.1', () => {
      fs.writeFileSync(`${state}.ready`, JSON.stringify({port: server.address().port, pid: process.pid}), {mode: 0o600});
    });
  }).catch(() => process.exit(1));
}
module.exports = {createRelay, tokenSource};
