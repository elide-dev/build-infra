import { describe, expect, test } from "bun:test";
import { buildQueryUrl, deduplicateByPlatform } from "./bincompare.mjs";
import type { BinstatInfo } from "../../commons/api.mjs";

const rev = "2afd3163d8540103fe64a75fa033b817aaae0b13";

describe("buildQueryUrl", () => {
  test("omits mode when not provided", () => {
    const url = buildQueryUrl(rev, "whiplash");
    expect(url).not.toContain("mode=");
    expect(url).toContain(`revision=${rev}`);
    expect(url).toContain("name=whiplash");
  });

  test("appends mode when provided", () => {
    const url = buildQueryUrl(rev, "whiplash", "dev");
    expect(url).toContain("&mode=dev");
  });
});

describe("deduplicateByPlatform", () => {
  test("keeps the first row per os-arch (newest under timestamp DESC)", () => {
    const rows = [
      { os: "linux", arch: "amd64", size: 10 },
      { os: "linux", arch: "amd64", size: 20 },
      { os: "macos", arch: "arm64", size: 30 },
    ] as unknown as BinstatInfo[];
    const map = deduplicateByPlatform(rows);
    expect(map.size).toBe(2);
    expect(map.get("linux-amd64")?.size).toBe(10);
    expect(map.get("macos-arm64")?.size).toBe(30);
  });
});
