import * as z from "zod";

import {
  modelVersion,
  OperatingSystem,
  Architecture,
  PlatformTag,
  LibCTarget,
} from "../commons/api.mjs"

export {
  modelVersion,
  OperatingSystem,
  Architecture,
  PlatformTag,
  LibCTarget,
}

// Information about the reports service.
export const reportService = {
  name: "reports",
  version: modelVersion,
};

// Enumerates known types of reports.
export enum ReportType {}

// Schema for report metadata.
export type ReportMetadata = {}

// Zod object schema for report metadata.
export const ReportMetadataRecord = z.object({})

// Outer request payload structure for dev report submission.
export type SubmitReportRequest = {
  version: "v1";
  type: "report";
  data: ReportMetadata;
};
