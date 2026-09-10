// Every ProgressTask must be completed by something.
//
// A checklist with an item nobody can tick is worse than a shorter checklist:
// it reads as a bug and the player cannot tell which one is broken. The enum
// lives in common/ and the hooks are scattered across the NeoForge module by
// design - each next to whatever event already existed - so nothing but a sweep
// can tell you that all of them are reachable.
//
//   node neoforge-26.1.2/tools/check-progress-tasks.mjs

import { readFileSync, readdirSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, "../..");
const enumFile = join(repo, "common/src/main/java/com/example/horsegenetics/common/progress/ProgressTask.java");
const roots = [join(repo, "neoforge-26.1.2/src/main/java")];

const src = readFileSync(enumFile, "utf8");
const body = src.slice(src.indexOf("public enum ProgressTask {"), src.indexOf("/** The headings"));
const tasks = [...body.matchAll(/^\s{4}([A-Z][A-Z0-9_]*)\(Group\./gm)].map((m) => m[1]);
if (tasks.length === 0) {
  console.error("check-progress-tasks: found no tasks - has the enum been reshaped?");
  process.exit(1);
}

let java = "";
for (const root of roots) {
  const walk = (dir) => {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name);
      if (statSync(p).isDirectory()) walk(p);
      else if (name.endsWith(".java")) java += readFileSync(p, "utf8");
    }
  };
  walk(root);
}

// Plain indexOf rather than a regex: an escaped word boundary does not survive
// every way this file might be written, and a check that silently matches
// nothing is worse than no check. Task names are unique and upper-case, so a
// substring hit cannot be a false positive for a different task.
const unwired = tasks.filter((t) => !java.includes("ProgressTask." + t));
if (unwired.length) {
  console.error(
    `check-progress-tasks: ${unwired.length} task(s) nothing can ever complete:\n  ` +
      unwired.join("\n  ") +
      "\n\nEither hook them, or take them off the checklist.",
  );
  process.exit(1);
}
console.log(`progress tasks OK - all ${tasks.length} are completed by something`);
