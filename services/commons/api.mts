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
