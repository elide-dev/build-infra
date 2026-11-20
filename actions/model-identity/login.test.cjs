const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {exchange} = require('./login.cjs');

for (const provider of ['openai','claude']) {
  test(provider+' exchanges only the audience-specific assertion', async t => {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(),'model-identity-'));
    t.after(() => fs.rm(dir,{recursive:true,force:true}));
    const env = {INPUT_PROVIDER:provider, 'INPUT_SERVICE-ACCOUNT':'account', 'INPUT_IDENTITY-PROVIDER':'issuer',
      'INPUT_FEDERATION-RULE':'rule', INPUT_ORGANIZATION:'org', ACTIONS_ID_TOKEN_REQUEST_TOKEN:'runner-token',
      ACTIONS_ID_TOKEN_REQUEST_URL:'https://token.actions.githubusercontent.com/job', GITHUB_ENV:path.join(dir,'env')};
    const openai = provider === 'openai';
    await exchange(env,async (url,init) => {
      if (url instanceof URL) {
        assert.equal(url.searchParams.get('audience'),openai?'https://api.openai.com/v1':'https://api.anthropic.com');
        assert.equal(init.headers.authorization,'Bearer runner-token');
        return Response.json({value:'assertion'});
      }
      assert.equal(url,openai?'https://auth.openai.com/oauth/token':'https://api.anthropic.com/v1/oauth/token');
      assert.equal(init.redirect,'error');
      const body = JSON.parse(init.body);
      assert.equal(openai?body.subject_token:body.assertion,'assertion');
      assert.equal(body.service_account_id,'account');
      return Response.json({access_token:'fixture-token'});
    });
    assert.equal(await fs.readFile(env.GITHUB_ENV,'utf8'),(openai?'OPENAI_API_KEY':'ANTHROPIC_AUTH_TOKEN')+'=fixture-token\n');
  });
}
