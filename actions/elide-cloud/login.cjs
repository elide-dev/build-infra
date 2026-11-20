const fs = require('node:fs/promises');

async function login() {
  const endpoint = 'https://auth.elide.cloud/token';
  const url = new URL(process.env.ACTIONS_ID_TOKEN_REQUEST_URL);
  url.searchParams.set('audience', endpoint);
  const identity = await fetch(url, {
    redirect: 'error',
    headers: {authorization: 'Bearer ' + process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN},
  });
  if (!identity.ok) throw new Error('GitHub OIDC request failed: ' + identity.status);
  const {value} = await identity.json();
  if (typeof value !== 'string' || !value) throw new Error('Missing GitHub identity token');
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
