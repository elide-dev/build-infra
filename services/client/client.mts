#!/usr/bin/env node
import { Command } from "commander";
import binstats, { type BinstatOptions } from "./actions/binstats.mjs";
import bincompare, { type BincompareOptions } from "./actions/bincompare.mjs";
import reportUpload from "./actions/report-upload.mjs";
import pkg from "./package.json" with { type: "json" };

const program = new Command();

program.name("elide-devops").description(pkg.description).version(pkg.version);

program
  .command("binstats")
  .description("Generate and then report stats about an Elide binary")
  .argument("<name>", "name of the binary")
  .argument("<path>", "path to the binary")
  .option("--debug", "Enable debug logging")
  .option("--dry", "Perform a dry run")
  .option(
    "--revision <string>",
    "Git revision of the binary; if none provided, will be determined (or fail)",
  )
  .option("--branch <string>", "Currently active/applicable Git branch, if any")
  .option("--tag <string>", "Currently active/applicable Git tag, if any")
  .option(
    "--os <string>",
    "Intended target operating system of the binary (one of `linux`, `macos`, `windows`)",
  )
  .option(
    "--arch <string>",
    "Intended target architecture of the binary (one of `amd64`, `arm64`)",
  )
  .option(
    "--libc <string>",
    "Intended target libc variant (one of `glibc`, `musl`), if applicable",
  )
  .action(async (name: string, path: string, options: BinstatOptions) =>
    binstats(name, path, options),
  );

program
  .command("bincompare")
  .description("Compare binary sizes between two revisions")
  .requiredOption("--base <string>", "Base revision (full 40-char SHA)")
  .requiredOption("--pr <string>", "PR revision (full 40-char SHA)")
  .option("--name <string>", "Binary name to compare", "whiplash")
  .option("--output <string>", "Write markdown to file instead of stdout")
  .option("--debug", "Enable debug logging")
  .action(async (options: BincompareOptions) => bincompare(options));

program
  .command("report-upload")
  .description("Upload a report glob or zip")
  .argument("<path>", "path to the zip or a glob")
  .action(reportUpload);

async function main() {
  await program.parseAsync();
}

main().then(
  () => {
    process.exit(0);
  },
  (err) => {
    console.error(err);
    process.exit(1);
  },
);
