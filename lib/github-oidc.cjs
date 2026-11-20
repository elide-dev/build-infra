async function githubIdentity(audience, env, fetcher) {
  if (!env.ACTIONS_ID_TOKEN_REQUEST_URL || !env.ACTIONS_ID_TOKEN_REQUEST_TOKEN)
    throw new Error('The workflow job needs id-token: write');
  const url = new URL(env.ACTIONS_ID_TOKEN_REQUEST_URL);
  if (url.protocol !== 'https:' || !url.hostname.endsWith('.actions.githubusercontent.com') || url.username || url.password)
    throw new Error('Unexpected GitHub OIDC issuer endpoint');
  url.searchParams.set('audience', audience);
  const response = await fetcher(url, {redirect:'error', headers:{authorization:'Bearer '+env.ACTIONS_ID_TOKEN_REQUEST_TOKEN}});
  if (!response.ok) throw new Error('GitHub identity request failed: '+response.status);
  const {value} = await response.json();
  if (typeof value !== 'string' || !value) throw new Error('Missing GitHub identity token');
  return value;
}
module.exports = {githubIdentity};
