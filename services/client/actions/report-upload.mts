import { Command } from "commander";

export default async function reportUpload(context: Command) {
  const token = process.env["ELIDE_DEVOPS_API_TOKEN"];
  if (!token) {
    throw new Error(
      "ELIDE_DEVOPS_API_TOKEN environment variable is not set; cannot report binstats",
    );
  }
  console.log("Report upload...");
}
