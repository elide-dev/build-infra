import { describe, expect, test } from "bun:test";
import { BinstatInfoRecord } from "./api.mjs";

const base = {
  name: "elide",
  size: 100,
  sha256: "c281f3e9808bad2968ec30445dec73ccca71760c450f840ebf95247583c57c44",
  revision: "2afd3163d8540103fe64a75fa033b817aaae0b13",
  os: "linux",
  arch: "amd64",
  timestamp: 1762908124936,
};

describe("BinstatInfoRecord mode", () => {
  test("accepts a record without mode (backwards compatible)", () => {
    expect(() => BinstatInfoRecord.parse(base)).not.toThrow();
  });

  test("accepts mode=release and mode=dev", () => {
    expect(() =>
      BinstatInfoRecord.parse({ ...base, mode: "release" }),
    ).not.toThrow();
    expect(() =>
      BinstatInfoRecord.parse({ ...base, mode: "dev" }),
    ).not.toThrow();
  });

  test("rejects an unknown mode", () => {
    expect(() =>
      BinstatInfoRecord.parse({ ...base, mode: "nightly" }),
    ).toThrow();
  });
});
