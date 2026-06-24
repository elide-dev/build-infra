import { describe, expect, test } from "bun:test";
import { buildBinstatQuery, omitNullMode } from "./query.mjs";
import type { BinstatInfo } from "../../commons/api.mjs";

const rev = "2afd3163d8540103fe64a75fa033b817aaae0b13";

describe("buildBinstatQuery", () => {
  test("omits the mode filter when mode is not provided", () => {
    const { sql, params } = buildBinstatQuery({
      revision: rev,
      name: "whiplash",
    });
    expect(sql).not.toContain("mode = ?");
    expect(params).toEqual([rev, "whiplash"]);
  });

  test("adds the mode filter only when provided", () => {
    const { sql, params } = buildBinstatQuery({
      revision: rev,
      name: "whiplash",
      mode: "dev",
    });
    expect(sql).toContain("AND mode = ?");
    expect(params).toEqual([rev, "whiplash", "dev"]);
  });

  test("composes os, arch and mode filters in order", () => {
    const { sql, params } = buildBinstatQuery({
      revision: rev,
      name: "whiplash",
      os: "linux",
      arch: "amd64",
      mode: "release",
    });
    expect(sql).toContain("AND os = ?");
    expect(sql).toContain("AND arch = ?");
    expect(sql).toContain("AND mode = ?");
    expect(params).toEqual([rev, "whiplash", "linux", "amd64", "release"]);
    expect(sql.endsWith("ORDER BY timestamp DESC")).toBe(true);
  });

  test("selects the mode column", () => {
    const { sql } = buildBinstatQuery({ revision: rev, name: "whiplash" });
    expect(sql).toContain(", mode, timestamp ");
  });
});

describe("omitNullMode", () => {
  test("drops a NULL mode (legacy row) so JSON omits it, keeps a real mode", () => {
    const rows = [
      { os: "linux", arch: "amd64", size: 1, mode: null },
      { os: "macos", arch: "arm64", size: 2, mode: "dev" },
    ] as unknown as BinstatInfo[];
    const out = omitNullMode(rows);
    expect("mode" in JSON.parse(JSON.stringify(out[0]))).toBe(false);
    expect(out[1].mode).toBe("dev");
  });
});
