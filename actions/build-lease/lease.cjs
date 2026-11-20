const fs = require('node:fs/promises');

async function acquire(env, fetcher = fetch) {
  const project = env.INPUT_PROJECT;
  const ttlSeconds = Number(env['INPUT_TTL-SECONDS']);
  if (!project || !['true', 'false'].includes(env.INPUT_WRITABLE) ||
      !Number.isSafeInteger(ttlSeconds) || ttlSeconds < 60 || ttlSeconds > 21600)
    throw new Error('Invalid project, writable setting or lease lifetime');
  if (!env.ELIDE_CLOUD_ACCESS_TOKEN) throw new Error('Run the elide-cloud login action first');
  const response = await fetcher('https://build.elide.cloud/v1/leases', {
    method: 'POST', redirect: 'error',
    headers: {authorization: 'Bearer ' + env.ELIDE_CLOUD_ACCESS_TOKEN, 'content-type': 'application/json'},
    body: JSON.stringify({project, sharing: 'org', writable: env.INPUT_WRITABLE === 'true', ttlSeconds}),
  });
  if (!response.ok) throw new Error('Build lease request failed: ' + response.status);
  const {token} = await response.json();
  if (typeof token !== 'string' || !/^[0-9a-f]{64}$/.test(token)) throw new Error('Invalid lease response');
  console.log('::add-mask::' + token);
  await fs.appendFile(env.GITHUB_STATE, 'lease=' + token + '\n');
  await fs.appendFile(env.GITHUB_ENV, 'ELIDE_CLOUD_BUILD_LEASE=' + token + '\n');
}

if (require.main === module) acquire(process.env).catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = {acquire};
