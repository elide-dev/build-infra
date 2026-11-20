const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {acquire} = require('./lease.cjs');
const {revoke} = require('./revoke.cjs');

test('passes the account credential only to issuance and revokes with the returned lease', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(),'lease-test-'));
  t.after(() => fs.rm(root,{recursive:true,force:true}));
  const env = {INPUT_PROJECT:'runtime', INPUT_WRITABLE:'false', 'INPUT_TTL-SECONDS':'300',
    ELIDE_CLOUD_ACCESS_TOKEN:'fixture', GITHUB_ENV:path.join(root,'env'), GITHUB_STATE:path.join(root,'state')};
  const lease = 'a'.repeat(64);
  await acquire(env, async (url, init) => {
    assert.equal(url, 'https://build.elide.cloud/v1/leases');
    assert.equal(init.headers.authorization, 'Bearer fixture');
    assert.deepEqual(JSON.parse(init.body), {project:'runtime',sharing:'org',writable:false,ttlSeconds:300});
    return Response.json({token:lease},{status:201});
  });
  assert.equal(await fs.readFile(env.GITHUB_ENV,'utf8'), 'ELIDE_CLOUD_BUILD_LEASE='+lease+'\n');
  assert.equal(await fs.readFile(env.GITHUB_STATE,'utf8'), 'lease='+lease+'\n');
  await revoke(lease, async (url, init) => {
    assert.equal(url,'https://build.elide.cloud/v1/lease');
    assert.equal(init.method,'DELETE');
    assert.equal(init.headers.authorization,'Bearer '+lease);
    return new Response(null,{status:204});
  });
});
