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
// ...and every task is also an advancement.
//
// The tree is baked off this same enum by bake-advancements.mjs, so the two can
// only disagree by somebody adding a task and not re-running the bake - at
// which point the new box ticks in the book and no toast ever comes up for it,
// which is precisely the half-wired state the check above exists to prevent.
// A stale advancement is the mirror of it: a task deleted from the enum leaves
// a card in the tree that nothing can ever complete.
const advDir = join(repo, "neoforge-26.1.2/src/main/resources/data/horsegenetics/advancement");
const baked = new Set();
const walkAdv = (dir, prefix) => {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) walkAdv(p, prefix + name + "/");
    else if (name.endsWith(".json")) baked.add(prefix + name.slice(0, -5));
  }
};
walkAdv(advDir, "");

const chapterOf = new Map();
for (const m of body.matchAll(/^\s{4}([A-Z][A-Z0-9_]*)\(Group\.([A-Z_]+),/gm)) {
  chapterOf.set(m[1], m[2].toLowerCase());
}
const wantedTasks = tasks.map((t) => `${chapterOf.get(t)}/${t.toLowerCase()}`);
const missing = wantedTasks.filter((a) => !baked.has(a));
// Everything that is not a leaf task: the root and the eight chapter nodes.
const chapters = new Set(chapterOf.values());
const orphans = [...baked].filter(
  (a) => !wantedTasks.includes(a) && a !== "root" && !chapters.has(a),
);
if (missing.length || orphans.length) {
  if (missing.length) {
    console.error(
      `check-progress-tasks: ${missing.length} task(s) with no advancement:\n  ` + missing.join("\n  "),
    );
  }
  if (orphans.length) {
    console.error(
      `check-progress-tasks: ${orphans.length} advancement(s) no task backs:\n  ` + orphans.join("\n  "),
    );
  }
  console.error("\nRun: node neoforge-26.1.2/tools/bake-advancements.mjs");
  process.exit(1);
}

console.log(
  `progress tasks OK - all ${tasks.length} are completed by something, and all ${tasks.length} are advancements`,
);
