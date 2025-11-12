import * as z from "zod";

// Active model version.
export const modelVersion = "v1";

// Operating system tag values.
export type OperatingSystem = "linux" | "macos" | "windows";

// Architecture tag values.
export type Architecture = "amd64" | "arm64";

// Combined platform tag values.
export type PlatformTag =
  | "linux-amd64"
  | "linux-arm64"
  | "macos-amd64"
  | "macos-arm64";

// LibC target tag values.
export type LibCTarget = "glibc" | "musl";

// Information about the binstat service.
export const binstatService = {
  name: "devstat",
  version: modelVersion,
};

// Information posted to the bin-stat endpoint.
export type BinstatInfo = {
  // Name of the binary.
  name: string;

  // Size of the binary in bytes.
  size: number;

  // Gzipped size of the binary in bytes.
  gzip: number;

  // Zipped size of the binary in bytes.
  zip: number;

  // Xzipped size of the binary in bytes.
  xz: number;

  // SHA256 hash of the binary, hex-encoded.
  sha256: string;

  // Git revision that produced this binary.
  revision: string;

  // Operating system the binary was built for.
  os: OperatingSystem;

  // CPU architecture the binary was built for.
  arch: Architecture;

  // LibC target the binary was built against.
  libc?: LibCTarget;

  // Timestamp from the sender, as a Unix timestamp in seconds.
  timestamp: number;
};

// Zod schema for validating binstat info records.
export const BinstatInfoRecord = z.object({
  name: z.string(),
  size: z.number(),
  gzip: z.number(),
  zip: z.number(),
  xz: z.number(),
  sha256: z.hash("sha256", { enc: "hex" }),
  revision: z.string(),
  os: z.enum(["linux", "macos", "windows"]),
  arch: z.enum(["amd64", "arm64"]),
  libc: z.optional(z.enum(["glibc", "musl"])),
  timestamp: z.number(),
});

// Outer request payload structure for bin-stat postings.
export type BinstatInfoRequest = {
  version: "v1";
  type: "binstat";
  data: BinstatInfo;
};

// Information about the reports service.
export const reportService = {
  name: "reports",
  version: modelVersion,
};

// Enumerates known types of reports.
export enum ReportType {}

// Schema for report metadata.
export type ReportMetadata = {};

// Zod object schema for report metadata.
export const ReportMetadataRecord = z.object({});

// Outer request payload structure for dev report submission.
export type SubmitReportRequest = {
  version: "v1";
  type: "report";
  data: ReportMetadata;
};
