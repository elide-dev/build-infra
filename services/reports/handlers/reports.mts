import { ReportMetadata, ReportMetadataRecord } from "../../commons/api.mjs";
import { decodeJsonAndValidate } from "../../commons/handler.mjs";
import { nanoid } from "nanoid";

function buildRecordKey(_record: ReportMetadata): string {
  return nanoid();
}

async function writeToR2(
  key: string,
  record: ReportMetadata,
  env: Env,
): Promise<void> {
  try {
    // nothing yet
    console.log("would write to r2");
  } catch (err) {
    console.error("Failed to write to R2", err);
  }
}

async function writeToD1(
  key: string,
  record: ReportMetadata,
  env: Env,
): Promise<void> {
  try {
    console.log("would write to d1");
    // await env.BINSTAT_DB.prepare(
    //   `INSERT INTO 'binstats-v1' (key, name, sha256, revision, size, gzip, zip, xz, os, arch, timestamp) ` +
    //     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
    // )
    //   .bind(
    //     recordKey,
    //     record.name,
    //     record.sha256,
    //     record.revision,
    //     record.size,
    //     record.gzip,
    //     record.zip,
    //     record.xz,
    //     record.os,
    //     record.arch,
    //     record.timestamp,
    //   )
    //   .run();
    console.log(`Wrote to D1 at key: '${key}'`);
  } catch (err) {
    console.error("Failed to write to D1", err);
  }
}

export default async function handler(
  request: Request,
  env: Env,
  ctx: ExecutionContext,
): Promise<Response> {
  const payload = await decodeJsonAndValidate<ReportMetadata>(request, (data) =>
    ReportMetadataRecord.parse(data),
  );

  const recordKey = buildRecordKey(payload);

  console.log(
    "received report: ",
    typeof payload,
    JSON.stringify(payload, null, 2),
  );

  ctx.waitUntil(
    Promise.all([
      writeToR2(recordKey, payload, env),
      writeToD1(recordKey, payload, env),
    ]),
  );
  return new Response(null, {
    status: 202,
    statusText: "accepted",
  });
}
