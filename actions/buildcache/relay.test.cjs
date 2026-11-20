'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const {once} = require('node:events');
const {createRelay, tokenSource} = require('./relay.cjs');
const {validateOrigin, gradleScript} = require('./setup.cjs');
const auth = {Authorization: 'Basic '+Buffer.from('elide:secret').toString('base64')};
async function listen(t, server) {
  server.listen(0, '127.0.0.1');await once(server,'listening');
  t.after(() => {server.closeAllConnections();server.close();});
  return `http://127.0.0.1:${server.address().port}`;
}
test('fixed upstream, authenticated streaming, ref namespace and bounded paths', async t => {
  let seen;
  const upstream = await listen(t, http.createServer(async (req,res) => {
    seen = {url:req.url,auth:req.headers.authorization,ns:req.headers['x-elide-cache-namespace']};
    const body=[];for await (const c of req) body.push(c);
    res.writeHead(req.method==='PUT'?201:200);res.end(Buffer.concat(body));
  }));
  const endpoint = await listen(t, createRelay({endpoint:upstream,password:'secret',namespace:'v2',getToken:async()=> 'signed-token'}));
  assert.equal((await fetch(endpoint+'/gradle/key')).status,401);
  assert.equal((await fetch(endpoint+'/sccache/a/b', {method:'PUT',headers:auth,body:'payload'})).status,201);
  assert.deepEqual(seen,{url:'/sccache/a/b',auth:'Bearer signed-token',ns:'v2'});
  assert.equal((await fetch(endpoint+'/sccache/key?host=evil', {headers:auth})).status,400);
  assert.equal((await fetch(endpoint+'/sccache/key',{method:'DELETE',headers:auth})).status,400);
  assert.equal(seen.url,'/sccache/a/b');
});
test('never follow redirect with job identity; authenticated shutdown', async t => {
  let shutdown=false;
  const upstream=await listen(t,http.createServer((req,res)=>{res.writeHead(307,{Location:'https://example.com/'});res.end();}));
  const endpoint=await listen(t,createRelay({endpoint:upstream,password:'secret',namespace:'v1',getToken:async()=> 'token',shutdown:()=>{shutdown=true;}}));
  assert.equal((await fetch(endpoint+'/gradle/key',{headers:auth})).status,502);
  assert.equal((await fetch(endpoint+'/_shutdown',{method:'POST'})).status,401);
  assert.equal(shutdown,false);
  assert.equal((await fetch(endpoint+'/_shutdown',{method:'POST',headers:auth})).status,200);
  assert.equal(shutdown,true);
});
test('GitHub token refresh is singleflight, scoped and never follows redirects',async()=>{
  let calls=0, exp=Math.floor(Date.now()/1000)+3600;
  const env={ACTIONS_ID_TOKEN_REQUEST_URL:'https://run.actions.githubusercontent.com/token?id=x',ACTIONS_ID_TOKEN_REQUEST_TOKEN:'request-secret'};
  const get=tokenSource(env,async(url,options)=>{
    calls++;assert.equal(url.searchParams.get('audience'),'elide-build-cache');assert.equal(options.redirect,'error');
    assert.equal(options.headers.Authorization,'Bearer request-secret');
    await new Promise(r=>setTimeout(r,5));
    return {ok:true,json:async()=>({value:'header.'+Buffer.from(JSON.stringify({exp})).toString('base64url')+'.signature'})};
  });
  const values=await Promise.all(Array.from({length:20},()=>get()));assert.equal(new Set(values).size,1);assert.equal(calls,1);
  await get();assert.equal(calls,1);
  await assert.rejects(tokenSource({...env,ACTIONS_ID_TOKEN_REQUEST_URL:'https://evil.example/token'})());
  exp=0;
  await assert.rejects(tokenSource(env,async()=>({ok:true,json:async()=>({value:'a.'+Buffer.from(JSON.stringify({exp})).toString('base64url')+'.c'})}))());
});
test('cache origin rejects lookalike private hosts and credential-bearing URLs',()=>{
  assert.equal(validateOrigin('http://10.0.0.1:8080').hostname,'10.0.0.1');
  for(const url of ['http://10.evil.example','http://example.com','https://u:p@example.com','https://example.com/path','https://example.com/?secret=x'])assert.throws(()=>validateOrigin(url));
  assert.match(gradleScript(),/System.getenv\('ELIDE_BUILD_CACHE_PASSWORD'\)/);
  assert.doesNotMatch(gradleScript(),/secret|request-token/);
});

test('WebDAV parent metadata is authenticated, depth-zero only and forwarded with job identity', async t => {
  const seen=[];
  const upstream=await listen(t,http.createServer((req,res)=>{
    seen.push({method:req.method,path:req.url,depth:req.headers.depth,auth:req.headers.authorization});
    res.writeHead(req.method==='PROPFIND'?207:201);res.end();
  }));
  const endpoint=await listen(t,createRelay({endpoint:upstream,password:'secret',namespace:'v1',getToken:async()=> 'signed-token'}));
  assert.equal((await fetch(endpoint+'/sccache/',{method:'PROPFIND',headers:{Depth:'0'}})).status,401);
  assert.equal((await fetch(endpoint+'/sccache/',{method:'PROPFIND',headers:{...auth,Depth:'0'}})).status,207);
  assert.equal((await fetch(endpoint+'/sccache/a/b/',{method:'MKCOL',headers:auth})).status,201);
  for(const depth of ['1','infinity']) assert.equal((await fetch(endpoint+'/sccache/',{method:'PROPFIND',headers:{...auth,Depth:depth}})).status,400);
  assert.equal(seen.length,2);
  assert.deepEqual(seen[0],{method:'PROPFIND',path:'/sccache/',depth:'0',auth:'Bearer signed-token'});
  assert.equal((await fetch(endpoint+'/sccache/',{method:'DELETE',headers:auth})).status,400);
});

test('upstream outage reads as a marked miss so setup cannot mistake it for verified identity', async t => {
  const dead=http.createServer();dead.listen(0,'127.0.0.1');await once(dead,'listening');
  const origin=`http://127.0.0.1:${dead.address().port}`;dead.close();await once(dead,'close');
  const endpoint=await listen(t,createRelay({endpoint:origin,password:'secret',namespace:'v1',getToken:async()=> 'signed-token'}));
  const miss=await fetch(endpoint+'/sccache/key',{headers:auth});
  assert.equal(miss.status,404);assert.equal(miss.headers.get('x-elide-relay-error'),'upstream');
  const write=await fetch(endpoint+'/sccache/key',{method:'PUT',headers:auth,body:'x'});
  assert.equal(write.status,503);assert.equal(write.headers.get('x-elide-relay-error'),'upstream');
});
