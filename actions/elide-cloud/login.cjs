const fs = require('node:fs/promises');

const {githubIdentity} = require('../../lib/github-oidc.cjs');

async function login() {
  const endpoint = 'https://auth.elide.cloud/token';
  const value = await githubIdentity(endpoint, process.env, fetch);
  const response = await fetch(endpoint, {
    method: 'POST', redirect: 'error',
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:token-exchange',
      subject_token_type: 'urn:ietf:params:oauth:token-type:id_token',
      subject_token: value,
    }),
  });
  if (!response.ok) throw new Error('Elide Cloud token exchange failed: ' + response.status);
  const {access_token: token} = await response.json();
  if (typeof token !== 'string' || !/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(token))
    throw new Error('Invalid Elide Cloud access token');
  console.log('::add-mask::' + token);
  await fs.appendFile(process.env.GITHUB_ENV, 'ELIDE_CLOUD_ACCESS_TOKEN=' + token + '\n');
}

login().catch(error => { console.error(error.message); process.exitCode = 1; });
