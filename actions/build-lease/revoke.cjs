async function revoke(token, fetcher = fetch) {
  if (!token) return;
  const response = await fetcher('https://build.elide.cloud/v1/lease', {
    method: 'DELETE', redirect: 'error', headers: {authorization: 'Bearer ' + token},
  });
  if (!response.ok && response.status !== 401) throw new Error('Build lease revocation failed: ' + response.status);
}
if (require.main === module) revoke(process.env.STATE_lease).catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = {revoke};
