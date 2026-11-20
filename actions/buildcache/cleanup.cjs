'use strict';
const fs = require('node:fs');
const path = require('node:path');
async function cleanup() {
  const state = process.env.STATE_relayState;
  if (state && path.basename(state) === 'relay.json' && path.basename(path.dirname(state)).startsWith('elide-build-cache-')) {
    try {
      const {port} = JSON.parse(fs.readFileSync(`${state}.ready`, 'utf8'));
      const {password} = JSON.parse(fs.readFileSync(state, 'utf8'));
      if (Number.isInteger(port) && port > 0 && port < 65536) await fetch(`http://127.0.0.1:${port}/_shutdown`, {
        method: 'POST', headers: {Authorization: `Basic ${Buffer.from(`elide:${password}`).toString('base64')}`}, signal: AbortSignal.timeout(2000), redirect: 'error',
      });
    } catch {} // Runner teardown also reaps job processes if the relay already exited.
    fs.rmSync(path.dirname(state), {recursive: true, force: true});
  }
  const init = process.env.STATE_gradleInit;
  if (init && /^elide-build-cache-[a-f0-9]+\.gradle$/.test(path.basename(init))) fs.rmSync(init, {force: true});
}
if (require.main === module) cleanup();
module.exports = {cleanup};
