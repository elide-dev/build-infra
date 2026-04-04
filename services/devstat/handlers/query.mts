import {
  type BinstatInfo,
  type BinstatQueryResponse,
} from "../../commons/api.mjs";
import { errorResponse } from "../../commons/handler.mjs";

const REVISION_RE = /^[0-9a-f]{40}$/;

export default async function handler(
  request: Request,
  env: Env,
): Promise<Response> {
  const url = new URL(request.url);
  const revision = url.searchParams.get("revision");
  if (!revision || !REVISION_RE.test(revision)) {
    throw errorResponse(
      "query parameter 'revision' is required and must be a 40-char hex SHA",
      400,
    );
  }

  const name = url.searchParams.get("name") || "whiplash";
  const os = url.searchParams.get("os");
  const arch = url.searchParams.get("arch");

  let sql =
    "SELECT name, sha256, revision, size, gzip, zip, xz, os, arch, branch, tag, timestamp " +
    "FROM 'binstats-v1' WHERE revision = ? AND name = ?";
  const params: string[] = [revision, name];

  if (os) {
    sql += " AND os = ?";
    params.push(os);
  }
  if (arch) {
    sql += " AND arch = ?";
    params.push(arch);
  }
  sql += " ORDER BY timestamp DESC";

  const result = await env.BINSTAT_DB.prepare(sql)
    .bind(...params)
    .all<BinstatInfo>();

  const response: BinstatQueryResponse = {
    version: "v1",
    revision,
    results: result.results || [],
  };

  return new Response(JSON.stringify(response), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}
