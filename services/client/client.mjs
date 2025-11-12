import { Command } from 'commander';
import binstats from "./actions/binstats.mjs";
import reportUpload from "./actions/report-upload.mjs";
const program = new Command();
program
    .name('elide-devops')
    .description('Elide devops utility CLI')
    .version('0.1.0');
program.command('binstats')
    .description('Generate and then report stats about an Elide binary')
    .argument('<string>', 'path to the binary')
    .action(binstats);
program.command('report-upload')
    .description('Upload a report glob or zip')
    .argument('<string>', 'path to the zip or a glob')
    .action(reportUpload);
async function main() {
    await program.parseAsync();
}
main().then(() => {
    process.exit(0);
}, (err) => {
    console.error(err);
    process.exit(1);
});
