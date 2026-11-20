import {
  type Architecture,
  type BinstatInfo,
  type BuildMode,
  type OperatingSystem,
  type LibCTarget,
  binstatService as api,
  modelVersion,
  BinstatInfoRecord,
} from "../../commons/api.mjs";

import { existsSync } from "node:fs";
import syncFs from "node:fs";
import fs from "node:fs/promises";
import crypto from "node:crypto";
import { simpleGit, type SimpleGit } from "simple-git";
import byteSize from "byte-size";
import pkg from "../package.json" with { type: "json" };

export type BinstatOptions = {
  debug?: boolean;
  dry?: boolean;
  revision?: string;
  branch?: string;
  tag?: string;
  os?: string;
  arch?: string;
  libc?: string;
  mode?: string;
};

async function generateSourceControlState(
  git: SimpleGit,
  options: BinstatOptions,
): Promise<{
  revision: string;
  branch?: string;
  tag?: string;
}> {
  const isGitRepo = await git.checkIsRepo();
  const revision =
    options.revision || (isGitRepo ? await git.revparse("HEAD") : undefined);
  if (!revision) {
    throw new Error(
      "Git revision is required but could not be determined, and was not provided via `--revision`",
    );
  }
  const branch =
    options.branch ||
    (isGitRepo ? await git.revparse(["--abbrev-ref", "HEAD"]) : undefined);
  const tag = options.tag || undefined;
  return { revision, branch, tag };
}

async function calculateSha256Digest(path: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash("sha256");
    const stream = syncFs.createReadStream(path);
    stream.on("data", (data) => hash.update(data));
    stream.on("end", () => resolve(hash.digest("hex")));
    stream.on("error", (err) => reject(err));
  });
}

async function resolveBinaryOrHostInfo(
  path: string,
  options: BinstatOptions,
): Promise<{
  os: OperatingSystem;
  arch: Architecture;
  libc?: LibCTarget;
}> {
  let os: OperatingSystem;
  let arch: Architecture;
  let libc: LibCTarget | undefined;

  if (options.os) {
    os = options.os as OperatingSystem;
    if (!["linux", "macos", "windows"].includes(os)) {
      throw new Error(`Invalid OS specified: ${os}`);
    }
  } else {
    // resolve from host
    switch (process.platform) {
      case "linux":
        os = "linux";
        break;
      case "darwin":
        os = "macos";
        break;
      case "win32":
        os = "windows";
        break;
      default:
        throw new Error(`Unsupported host platform: ${process.platform}`);
    }
  }
  if (options.arch) {
    arch = options.arch as Architecture;
    if (!["amd64", "arm64"].includes(arch)) {
      throw new Error(`Invalid architecture specified: ${arch}`);
    }
  } else {
    // resolve from host
    switch (process.arch) {
      case "x64":
        arch = "amd64";
        break;
      case "arm64":
        arch = "arm64";
        break;
      default:
        throw new Error(`Unsupported host architecture: ${process.arch}`);
    }
  }
  if (os === "linux") {
    if (options.libc) {
      libc = options.libc as LibCTarget;
      if (!["glibc", "musl"].includes(libc)) {
        throw new Error(`Invalid libc specified: ${libc}`);
      }
    } else {
      // default to glibc
      libc = "glibc";
    }
  }
  return { os, arch, libc };
}

export default async function binstats(
  name: string,
  path: string,
  options: BinstatOptions,
) {
  const token = process.env["ELIDE_DEVOPS_API_TOKEN"];
  if (!token) {
    throw new Error(
      "ELIDE_DEVOPS_API_TOKEN environment variable is not set; cannot report binstats",
    );
  }
  const git: SimpleGit = simpleGit({});

  console.info("Generating binstats...");

  if (!name) {
    throw new Error("Binary name is required");
  }
  if (!existsSync(path)) {
    throw new Error(`Binary not found at path: ${path}`);
  }
  // must be a file, must be readable
  const stat = await fs.stat(path);

  if (!stat.isFile()) {
    throw new Error(`Path is not a file: ${path}`);
  }

  const infos = Promise.all([
    generateSourceControlState(git, options),
    resolveBinaryOrHostInfo(path, options),
    calculateSha256Digest(path),
  ]);

  const [gitState, targetingInfo, digest] = await infos;

  const binstats: BinstatInfo = {
    name,
    size: stat.size,
    ...gitState,
    ...targetingInfo,
    sha256: digest,
    timestamp: +new Date(),
  };
  if (options.mode) {
    if (!["release", "dev"].includes(options.mode)) {
      throw new Error(`Invalid mode specified: ${options.mode}`);
    }
    binstats.mode = options.mode as BuildMode;
  }
  if (options.debug) {
    console.debug("Generated binstats:", JSON.stringify(binstats, null, 2));
  }
  try {
    BinstatInfoRecord.parse(binstats);
  } catch (err) {
    throw new Error(`Generated binstat info is invalid: ${err}`);
  }

  const headers = new Headers();
  headers.set("content-type", "application/json");
  headers.set("authorization", `Bearer ${token}`);
  headers.set("x-api-version", modelVersion);
  headers.set("user-agent", `elide-devops-client/${pkg.version}`);

  const req: Request = new Request(
    new URL(
      `https://${api.endpoint}/${api.name}/${api.version}/${api.methods.binstat}`,
    ),
    {
      method: "POST",
      headers,
      body: JSON.stringify(binstats),
    },
  );

  const fmt = byteSize(stat.size);
  if (!options.dry) {
    console.log("Reporting stats...");
    const start = +new Date();
    try {
      const resp: Response = await fetch(req);
      const duration = +new Date() - start;
      if (!resp.ok) {
        throw new Error("Non-OK HTTP response code: " + resp.status);
      }
      console.info(`Binstats reported in ${duration}ms (${fmt})`);
      return;
    } catch (err) {
      throw new Error(`Failed to report binstats: ${err}`);
    }
  } else {
    console.info(`Dry run enabled; binstats not reported (${fmt}).`);
  }
}
