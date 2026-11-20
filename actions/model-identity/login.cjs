const fs = require('node:fs/promises');
const {githubIdentity} = require('../../lib/github-oidc.cjs');

async function exchange(env, fetcher = fetch) {
  const account = env['INPUT_SERVICE-ACCOUNT'];
  if (!account) throw new Error('Missing service-account');
  let audience, endpoint, body, variable;
  if (env.INPUT_PROVIDER === 'openai') {
    const id = env['INPUT_IDENTITY-PROVIDER'];
    if (!id) throw new Error('OpenAI requires identity-provider');
    audience = 'https://api.openai.com/v1';
    endpoint = 'https://auth.openai.com/oauth/token';
    variable = 'OPENAI_API_KEY';
    body = {grant_type:'urn:ietf:params:oauth:grant-type:token-exchange',
      subject_token_type:'urn:ietf:params:oauth:token-type:jwt', identity_provider_id:id, service_account_id:account};
    body.subject_token = await githubIdentity(audience, env, fetcher);
  } else if (env.INPUT_PROVIDER === 'claude') {
    const rule = env['INPUT_FEDERATION-RULE'];
    if (!rule || !env.INPUT_ORGANIZATION) throw new Error('Claude requires organization and federation-rule');
    audience = 'https://api.anthropic.com';
    endpoint = audience + '/v1/oauth/token';
    variable = 'ANTHROPIC_AUTH_TOKEN';
    body = {grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer', organization_id:env.INPUT_ORGANIZATION,
      federation_rule_id:rule, service_account_id:account, assertion:await githubIdentity(audience, env, fetcher)};
    if (env.INPUT_WORKSPACE) body.workspace_id = env.INPUT_WORKSPACE;
  } else throw new Error('provider must be openai or claude');
  const response = await fetcher(endpoint,{method:'POST',redirect:'error',headers:{'content-type':'application/json'},body:JSON.stringify(body)});
  if (!response.ok) throw new Error('Provider token exchange failed: '+response.status);
  const {access_token:token} = await response.json();
  if (typeof token !== 'string' || !/^[A-Za-z0-9._~+/=-]+$/.test(token)) throw new Error('Invalid provider bearer token');
  console.log('::add-mask::'+token);
  await fs.appendFile(env.GITHUB_ENV, variable+'='+token+'\n');
}
if (require.main === module) exchange(process.env).catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = {exchange};
