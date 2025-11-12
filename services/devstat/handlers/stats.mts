import { BinstatInfo, BinstatInfoRecord } from "../api.mjs";
import { decodeJsonAndValidate } from "../../commons/handler.mjs";
import { nanoid } from "nanoid";

function buildRecordKey(_record: BinstatInfo): string {
  return nanoid();
}

async function writeToAnalyticsEngine(
  record: BinstatInfo,
  env: Env,
): Promise<void> {
  try {
    env.BINSTAT_ANALYTICS.writeDataPoint({
      blobs: [record.os, record.arch, record.revision], // City, State
      doubles: [
        record.size || 0,
        record.gzip || 0,
        record.zip || 0,
        record.xz || 0,
      ],
      indexes: [record.name],
    });
    console.log("Wrote to analytics engine");
  } catch (err) {
    console.error("Failed to write to analytics engine", err);
  }
}

async function writeToD1(record: BinstatInfo, env: Env): Promise<void> {
  try {
    const recordKey = buildRecordKey(record);
    await env.BINSTAT_DB.prepare(
      `INSERT INTO 'binstats-v1' (key, name, sha256, revision, size, gzip, zip, xz, os, arch, timestamp) ` +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
    )
      .bind(
        recordKey,
        record.name,
        record.sha256,
        record.revision,
        record.size,
        record.gzip,
        record.zip,
        record.xz,
        record.os,
        record.arch,
        record.timestamp,
      )
      .run();
    console.log(`Wrote to D1 at key: '${recordKey}'`);
  } catch (err) {
    console.error("Failed to write to D1", err);
  }
}

export default async function handler(
  request: Request,
  env: Env,
  ctx: ExecutionContext,
): Promise<Response> {
  const payload = await decodeJsonAndValidate<BinstatInfo>(request, (data) =>
    BinstatInfoRecord.parse(data),
  );
  ctx.waitUntil(
    Promise.all([
      writeToAnalyticsEngine(payload, env),
      writeToD1(payload, env),
    ]),
  );
  return new Response(null, {
    status: 202,
    statusText: "accepted",
  });
}
