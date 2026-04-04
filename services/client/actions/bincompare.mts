import {
  type BinstatInfo,
  type BinstatQueryResponse,
  binstatService as api,
  modelVersion,
} from "../../commons/api.mjs";

import fs from "node:fs/promises";
import byteSize from "byte-size";
import pkg from "../package.json" with { type: "json" };

export type BincompareOptions = {
  debug?: boolean;
  base: string;
  pr: string;
  name?: string;
  output?: string;
};

const REVISION_RE = /^[0-9a-f]{40}$/;

function buildQueryUrl(revision: string, name: string): string {
  return `https://${api.endpoint}/${api.name}/${api.version}/${api.methods.binstat}?revision=${revision}&name=${encodeURIComponent(name)}`;
}

function buildHeaders(token: string): Headers {
  const headers = new Headers();
  headers.set("authorization", `Bearer ${token}`);
  headers.set("x-api-version", modelVersion);
  headers.set("user-agent", `elide-devops-client/${pkg.version}`);
  return headers;
}

async function fetchStats(
  revision: string,
  name: string,
  token: string,
): Promise<BinstatQueryResponse> {
  const url = buildQueryUrl(revision, name);
  const resp = await fetch(url, { headers: buildHeaders(token) });
  if (!resp.ok) {
    throw new Error(
      `Failed to fetch stats for revision ${revision}: HTTP ${resp.status}`,
    );
  }
  return (await resp.json()) as BinstatQueryResponse;
}

type PlatformKey = string; // e.g. "linux-amd64"

function deduplicateByPlatform(
  results: BinstatInfo[],
): Map<PlatformKey, BinstatInfo> {
  const map = new Map<PlatformKey, BinstatInfo>();
  for (const row of results) {
    const key = `${row.os}-${row.arch}`;
    if (!map.has(key)) {
      map.set(key, row); // results are ordered by timestamp DESC, first wins
    }
  }
  return map;
}

function formatSize(bytes: number): string {
  const s = byteSize(bytes);
  return `${s.value} ${s.unit}`;
}

function formatDelta(delta: number): string {
  const sign = delta >= 0 ? "+" : "";
  const s = byteSize(Math.abs(delta));
  return `${sign}${s.value} ${s.unit}`;
}

function formatPct(pct: number): string {
  const sign = pct >= 0 ? "+" : "";
  return `${sign}${pct.toFixed(2)}%`;
}

function renderMarkdown(
  baseRevision: string,
  prRevision: string,
  baseMap: Map<PlatformKey, BinstatInfo>,
  prMap: Map<PlatformKey, BinstatInfo>,
): string {
  const baseSha = baseRevision.substring(0, 10);
  const prSha = prRevision.substring(0, 10);

  const lines: string[] = [];
  lines.push(`## Binary Size Report`);
  lines.push(``);
  lines.push(
    `Comparing \`${baseSha}\` (base) \u2192 \`${prSha}\` (PR):`,
  );
  lines.push(``);
  lines.push(`| Platform | Base | PR | Delta | % |`);
  lines.push(`|---|---|---|---|---|`);

  const platforms = [...prMap.keys()].sort();
  for (const platform of platforms) {
    const baseRow = baseMap.get(platform);
    const prRow = prMap.get(platform)!;
    if (!baseRow) {
      lines.push(
        `| ${platform} | _N/A_ | ${formatSize(prRow.size)} | — | — |`,
      );
      continue;
    }
    const delta = prRow.size - baseRow.size;
    const pct = baseRow.size !== 0 ? (delta / baseRow.size) * 100 : 0;
    lines.push(
      `| ${platform} | ${formatSize(baseRow.size)} | ${formatSize(prRow.size)} | ${formatDelta(delta)} | ${formatPct(pct)} |`,
    );
  }

  return lines.join("\n") + "\n";
}

export default async function bincompare(
  options: BincompareOptions,
): Promise<void> {
  const token = process.env["ELIDE_DEVOPS_API_TOKEN"];
  if (!token) {
    throw new Error(
      "ELIDE_DEVOPS_API_TOKEN environment variable is not set; cannot query binstats",
    );
  }

  const { base, pr, name = "whiplash", output, debug } = options;

  if (!REVISION_RE.test(base)) {
    throw new Error(`Invalid base revision (expected 40-char hex SHA): ${base}`);
  }
  if (!REVISION_RE.test(pr)) {
    throw new Error(`Invalid PR revision (expected 40-char hex SHA): ${pr}`);
  }

  if (debug) {
    console.debug(`Fetching stats for base=${base}, pr=${pr}, name=${name}`);
  }

  const [baseResponse, prResponse] = await Promise.all([
    fetchStats(base, name, token),
    fetchStats(pr, name, token),
  ]);

  if (baseResponse.results.length === 0) {
    console.error(
      `No base stats found for revision ${base}. Skipping comparison.`,
    );
    return;
  }
  if (prResponse.results.length === 0) {
    console.error(
      `No PR stats found for revision ${pr}. Skipping comparison.`,
    );
    return;
  }

  const baseMap = deduplicateByPlatform(baseResponse.results);
  const prMap = deduplicateByPlatform(prResponse.results);

  if (debug) {
    console.debug(
      `Base platforms: ${[...baseMap.keys()].join(", ")}`,
    );
    console.debug(
      `PR platforms: ${[...prMap.keys()].join(", ")}`,
    );
  }

  const markdown = renderMarkdown(base, pr, baseMap, prMap);

  if (output) {
    await fs.writeFile(output, markdown, "utf-8");
    console.info(`Comparison written to ${output}`);
  } else {
    process.stdout.write(markdown);
  }
}
