'use strict';
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');
const net = require('node:net');
const {spawn} = require('node:child_process');

function emit(file, key, value) {
  const delimiter = `ELIDE_${crypto.randomBytes(16).toString('hex')}`;
  fs.appendFileSync(file, `${key}<<${delimiter}\n${value}\n${delimiter}\n`);
}
function input(name, fallback = '') {return process.env[`INPUT_${name.toUpperCase().replaceAll('-', '_')}`] || fallback;}
function validateOrigin(raw) {
  const origin = new URL(raw);
  const privateIPv4 = net.isIP(origin.hostname) === 4 && (origin.hostname.startsWith('10.') || origin.hostname === '127.0.0.1');
  if (origin.username || origin.password || origin.search || origin.hash || origin.pathname !== '/' || (origin.protocol !== 'https:' && !(origin.protocol === 'http:' && privateIPv4))) throw new Error('Use an HTTPS or private GCP cache origin');
  return origin;
}
function gradleScript() {return `import org.gradle.caching.http.HttpBuildCache
def endpoint = System.getenv('ELIDE_GRADLE_CACHE_URL')
if (endpoint) {
    gradle.settingsEvaluated { settings ->
        settings.buildCache {
            remote(HttpBuildCache) {
                url = endpoint
                allowInsecureProtocol = true // Authenticated relay binds only to loopback.
                push = true // The gateway isolates writes by verified repository/ref.
                credentials {
                    username = 'elide'
                    password = System.getenv('ELIDE_BUILD_CACHE_PASSWORD')
                }
            }
        }
    }
}
`;}
async function setup() {
  const output = (k, v) => emit(process.env.GITHUB_OUTPUT, k, v);
  let state, child, init;
  try {
    if (!process.env.ACTIONS_ID_TOKEN_REQUEST_URL || !process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN) throw new Error('Job needs permissions: id-token: write');
    const origin = validateOrigin(input('endpoint'));
    const namespace = input('namespace', 'v1');
    if (!/^[A-Za-z0-9_-]{1,64}$/.test(namespace)) throw new Error('Invalid namespace');
    const response = await fetch(new URL('/_health', origin), {signal: AbortSignal.timeout(2000), redirect: 'error'});
    if (!response.ok) throw new Error('Cache health check failed');
    const root = fs.mkdtempSync(path.join(process.env.RUNNER_TEMP || os.tmpdir(), 'elide-build-cache-'));
    fs.chmodSync(root, 0o700);
    const password = crypto.randomBytes(32).toString('hex');
    process.stdout.write(`::add-mask::${password}\n`);
    // Clients may log HTTP headers in diagnostics; mask the transformed secret too.
    process.stdout.write(`::add-mask::${Buffer.from(`elide:${password}`).toString('base64')}\n`);
    state = path.join(root, 'relay.json');
    fs.writeFileSync(state, JSON.stringify({endpoint: origin.origin, password, namespace}), {mode: 0o600});
    const env = {};
    for (const key of ['PATH','Path','SystemRoot','TEMP','TMP','HOME','USERPROFILE','NODE_EXTRA_CA_CERTS','HTTPS_PROXY','HTTP_PROXY','NO_PROXY','RUNNER_TRACKING_ID','ACTIONS_ID_TOKEN_REQUEST_URL','ACTIONS_ID_TOKEN_REQUEST_TOKEN']) if (process.env[key]) env[key] = process.env[key];
    child = spawn(process.execPath, [path.join(__dirname, 'relay.cjs')], {cwd: root, env, detached: true, stdio: 'ignore', windowsHide: true});
    child.unref();
    emit(process.env.GITHUB_STATE, 'relayState', state);
    let ready;
    for (let i = 0; i < 150; i++) {
      if (fs.existsSync(`${state}.ready`)) {ready = JSON.parse(fs.readFileSync(`${state}.ready`));break;}
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    if (!ready) throw new Error('Cache relay did not become ready');
    const endpoint = `http://127.0.0.1:${ready.port}`;
    // Check signed identity against the gateway before changing client settings.
    const preflight = await fetch(`${endpoint}/sccache/setup-probe-not-present`, {
      headers: {Authorization: `Basic ${Buffer.from(`elide:${password}`).toString('base64')}`}, signal: AbortSignal.timeout(10000),
    });
    if (preflight.headers.get('x-elide-relay-error')) throw new Error('Cache became unreachable during identity check');
    if (![200,404].includes(preflight.status)) throw new Error('Cache rejected this job identity');
    const values = {
      ELIDE_BUILD_CACHE_ENABLED: 'true', ELIDE_BUILD_CACHE_PASSWORD: password,
      ELIDE_GRADLE_CACHE_URL: `${endpoint}/gradle/`,
      SCCACHE_WEBDAV_ENDPOINT: `${endpoint}/sccache`, SCCACHE_WEBDAV_USERNAME: 'elide',
      SCCACHE_WEBDAV_PASSWORD: password,
      SCCACHE_WEBDAV_KEY_PREFIX: '', SCCACHE_WEBDAV_RW_MODE: 'READ_WRITE',
    };
    if (input('gradle', 'true') === 'true') {
      const home = process.env.GRADLE_USER_HOME || path.join(os.homedir(), '.gradle');
      const directory = path.join(home, 'init.d');fs.mkdirSync(directory, {recursive: true});
      init = path.join(directory, `elide-build-cache-${crypto.randomBytes(8).toString('hex')}.gradle`);
      fs.writeFileSync(init, gradleScript(), {mode: 0o600});
      emit(process.env.GITHUB_STATE, 'gradleInit', init);
      values.GRADLE_OPTS = `${process.env.GRADLE_OPTS || ''} -Dorg.gradle.caching=true`.trim();
    }
    for (const [key,value] of Object.entries(values)) emit(process.env.GITHUB_ENV, key, value);
    output('enabled', 'true');output('endpoint', endpoint);
    console.log('Shared build cache connected; GitHub identity refreshes per job, with isolated repository/ref indexes.');
  } catch (error) {
    if (child?.pid) {try {process.kill(child.pid);} catch {}}
    if (init) fs.rmSync(init, {force: true});
    if (state) fs.rmSync(path.dirname(state), {recursive: true, force: true});
    output('enabled', 'false');
    if (input('required') === 'true') {console.error('::error::Shared build cache unavailable: '+error.message);process.exitCode = 1;}
    else console.log('::notice::Shared build cache unavailable; existing cache configuration is preserved.');
  }
}
if (require.main === module) setup().catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = {emit, gradleScript, validateOrigin};
