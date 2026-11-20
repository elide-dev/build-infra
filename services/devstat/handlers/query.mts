import {
  type BinstatInfo,
  type BinstatQueryResponse,
} from "../../commons/api.mjs";
import { errorResponse } from "../../commons/handler.mjs";

const REVISION_RE = /^[0-9a-f]{40}$/;

export type BinstatQueryFilters = {
  revision: string;
  name: string;
  os?: string | null;
  arch?: string | null;
  mode?: string | null;
};

// Build the parameterized SELECT for a binstat query. `os`, `arch` and `mode`
// are only constrained when provided, so legacy queries behave as before.
export function buildBinstatQuery(filters: BinstatQueryFilters): {
  sql: string;
  params: string[];
} {
  let sql =
    "SELECT name, sha256, revision, size, gzip, zip, xz, os, arch, branch, tag, mode, timestamp " +
    "FROM 'binstats-v1' WHERE revision = ? AND name = ?";
  const params: string[] = [filters.revision, filters.name];

  if (filters.os) {
    sql += " AND os = ?";
    params.push(filters.os);
  }
  if (filters.arch) {
    sql += " AND arch = ?";
    params.push(filters.arch);
  }
  if (filters.mode) {
    sql += " AND mode = ?";
    params.push(filters.mode);
  }
  sql += " ORDER BY timestamp DESC";
  return { sql, params };
}

// Legacy rows predate the `mode` column and come back as NULL. Drop it so the
// response matches the optional (not nullable) `mode` contract: JSON omits the
// field rather than emitting `mode: null`.
export function omitNullMode(rows: BinstatInfo[]): BinstatInfo[] {
  return rows.map((row) =>
    row.mode == null ? { ...row, mode: undefined } : row,
  );
}

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
  const { sql, params } = buildBinstatQuery({
    revision,
    name,
    os: url.searchParams.get("os"),
    arch: url.searchParams.get("arch"),
    mode: url.searchParams.get("mode"),
  });

  const result = await env.BINSTAT_DB.prepare(sql)
    .bind(...params)
    .all<BinstatInfo>();

  const response: BinstatQueryResponse = {
    version: "v1",
    revision,
    results: omitNullMode(result.results || []),
  };

  return new Response(JSON.stringify(response), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}
