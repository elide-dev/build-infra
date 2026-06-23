import { describe, expect, test } from "bun:test";
import { buildBinstatQuery } from "./query.mjs";

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
