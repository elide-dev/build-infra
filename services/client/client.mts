import { Command } from "commander";
import binstats from "./actions/binstats.mjs";
import reportUpload from "./actions/report-upload.mjs";
import pkg from "./package.json";

const program = new Command();

program.name("elide-devops").description(pkg.description).version(pkg.version);

program
  .command("binstats")
  .description("Generate and then report stats about an Elide binary")
  .argument("<string>", "path to the binary")
  .action(binstats);

program
  .command("report-upload")
  .description("Upload a report glob or zip")
  .argument("<string>", "path to the zip or a glob")
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
