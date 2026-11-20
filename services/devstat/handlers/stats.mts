import { BinstatInfo, BinstatInfoRecord } from "../../commons/api.mjs";
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
      blobs: [
        record.os || "unknown-os",
        record.arch || "unknown-arch",
        record.revision || "unknown-revision",
      ],
      doubles: [
        record.size || 0,
        record.gzip || 0,
        record.zip || 0,
        record.xz || 0,
      ],
      indexes: [record.name || "unknown-name"],
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
      `INSERT INTO 'binstats-v1' (key, name, sha256, revision, size, gzip, zip, xz, os, arch, branch, tag, mode, timestamp) ` +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
    )
      .bind(
        recordKey,
        record.name,
        record.sha256,
        record.revision || "",
        record.size || 0,
        record.gzip || 0,
        record.zip || 0,
        record.xz || 0,
        record.os || "",
        record.arch || "",
        record.branch || "",
        record.tag || "",
        record.mode ?? null,
        record.timestamp ?? +new Date(),
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
  await writeToD1(payload, env);
  ctx.waitUntil(writeToAnalyticsEngine(payload, env));
  return new Response(null, {
    status: 202,
    statusText: "accepted",
  });
}
