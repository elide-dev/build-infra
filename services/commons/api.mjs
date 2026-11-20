import * as z from "zod";
// Active model version.
export const modelVersion = "v1";
// Information about the binstat service.
export const binstatService = {
    name: "devstat",
    version: modelVersion,
    endpoint: "stats.devops.elide.cloud",
    methods: {
        binstat: "binstat",
    },
};
// Zod schema for validating binstat info records.
export const BinstatInfoRecord = z.object({
    name: z.string(),
    size: z.number(),
    gzip: z.optional(z.number()),
    zip: z.optional(z.number()),
    xz: z.optional(z.number()),
    sha256: z.hash("sha256", { enc: "hex" }),
    revision: z.hash("sha1", { enc: "hex" }),
    branch: z.optional(z.string()),
    tag: z.optional(z.string()),
    os: z.enum(["linux", "macos", "windows"]),
    arch: z.enum(["amd64", "arm64"]),
    libc: z.optional(z.enum(["glibc", "musl"])),
    mode: z.optional(z.enum(["release", "dev"])),
    timestamp: z.number(),
});
// Information about the reports service.
export const reportService = {
    name: "reports",
    version: modelVersion,
};
// Zod object schema for report metadata.
export const ReportMetadataRecord = z.object({});
