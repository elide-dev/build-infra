'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const net = require('node:net');
const {once} = require('node:events');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {promisify} = require('node:util');
const exec = promisify(require('node:child_process').execFile);
const {createRelay} = require('./relay.cjs');

async function listen(t,server) {
 server.listen(0,'127.0.0.1'); await once(server,'listening');
 t.after(()=>{server.closeAllConnections?.();server.close();});
 return `http://127.0.0.1:${server.address().port}`;
}

test('real sccache writes through the authenticated relay and hits after a fresh daemon/local cache', {skip: !process.env.SCCACHE_TEST_BINARY, timeout: 60000},async t=>{
 const seen=[];
 const blobs=new Map();
 const upstream=process.env.SCCACHE_TEST_ENDPOINT || await listen(t,http.createServer(async(req,res)=>{
   seen.push([req.method,req.url]);
   assert.equal(req.headers.authorization,'Bearer repo:refs/heads/main');
   if(req.method==='PROPFIND') {
     assert.equal(req.headers.depth,'0');
     res.writeHead(207,{'Content-Type':'application/xml'});
     res.end(`<d:multistatus xmlns:d="DAV:"><d:response><d:href>${req.url}</d:href><d:propstat><d:prop><d:getlastmodified>Thu, 01 Jan 1970 00:00:00 GMT</d:getlastmodified><d:getcontentlength>0</d:getcontentlength><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>`); return;
   }
   if(req.method==='MKCOL') {res.writeHead(201);res.end();return;}
   if(req.method==='PUT') {const body=[];for await(const chunk of req)body.push(chunk);blobs.set(req.url,Buffer.concat(body));res.writeHead(201);res.end();return;}
   if(blobs.has(req.url)) {const body=blobs.get(req.url);res.writeHead(200,{'Content-Length':body.length});res.end(body);return;}
   res.writeHead(404);res.end();
 }));
 const endpoint=await listen(t,createRelay({endpoint:upstream,password:'fixture',namespace:'v1',getToken:async()=> 'repo:refs/heads/main'}));
 const root=await fs.mkdtemp(path.join(os.tmpdir(),'sccache-contract-'));
 t.after(()=>fs.rm(root,{recursive:true,force:true}));
 const reserved=net.createServer();reserved.listen(0,'127.0.0.1');await once(reserved,'listening');
 const port=reserved.address().port;await new Promise(resolve=>reserved.close(resolve));
 const env=Object.fromEntries(Object.entries(process.env).filter(([key])=>!key.startsWith('SCCACHE_')));
 Object.assign(env,{SCCACHE_SERVER_PORT:String(port),SCCACHE_WEBDAV_ENDPOINT:endpoint+'/sccache',SCCACHE_WEBDAV_USERNAME:'elide',SCCACHE_WEBDAV_PASSWORD:'fixture',SCCACHE_DIR:path.join(root,'local'),SCCACHE_IDLE_TIMEOUT:'0',SCCACHE_CONF:path.join(root,'config'),SCCACHE_LOG:'debug',SCCACHE_ERROR_LOG:path.join(root,'debug.log')});
 await fs.writeFile(env.SCCACHE_CONF,'');
 const source=path.join(root,'input.c'), object=path.join(root,'input.o');
 await fs.writeFile(source,'int cache_contract(void) {return 17;}');
 const binary=process.env.SCCACHE_TEST_BINARY;
 const command=(args)=>exec(binary,args,{env,cwd:root,timeout:20000});
 try {
  for(let attempt=0;attempt<2;attempt++) {
   if(attempt) {await fs.rm(env.SCCACHE_DIR,{recursive:true,force:true});await fs.rm(object);}
   await command(['--start-server']);
   await command(['cc','-c',source,'-o',object]);
   let stats;
   for(let i=0;i<100;i++) {
    stats=JSON.parse((await command(['--show-stats','--stats-format=json'])).stdout).stats;
    if(stats.cache_write_errors || stats.cache_writes || Object.values(stats.cache_hits.counts).some(value=>value>0))break;
    await new Promise(resolve=>setTimeout(resolve,50));
   }
   assert.equal(stats.cache_write_errors,0,JSON.stringify({stats,seen}));
   if(!attempt) assert.ok(stats.cache_writes>=1,JSON.stringify({stats,seen}));
   else assert.ok(Object.values(stats.cache_hits.counts).some(value=>value>=1),JSON.stringify({stats,seen}));
   assert.ok((await fs.stat(object)).size>0);
   await command(['--stop-server']);
  }
 } catch(error) { console.error((await fs.readFile(env.SCCACHE_ERROR_LOG,'utf8')).split('\n').slice(-55).join('\n')); throw error; } finally {await command(['--stop-server']).catch(()=>{});}
});
