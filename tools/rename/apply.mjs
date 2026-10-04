// The mechanical rename. DEFAULT IS A DRY RUN: it prints what it would do and changes nothing.
//   node tools/rename/apply.mjs            (dry run)
//   node tools/rename/apply.mjs --apply    (does it)
// Refuses to --apply unless: no TODO is left in rename.config.json, the git tree is clean, and you are not on main.
// Steps, in order (each is one commit-sized unit; the script does them all and leaves the result UNCOMMITTED so
// you can read `git diff --stat` first):
//   1  Java package: oldPackage -> newPackage in text files, and move the package directories.
//   2  Id token: every lowercase `horsegenetics` -> `ixoras_horses` in tracked text files (not the excluded folders).
//   3  Path segments and file names containing the old id are renamed with `git mv`, deepest first.
//   4  Display strings from rename.config.json.
// It never touches binary files, the excluded prefixes, or anything git does not track.
// What it does NOT do (see tools/rename/README.txt): the config folder rename, the old-prefix read alias, the
// refuse-to-load guard, the world converter, the re-bakes, the GitHub repo rename.
// WRITTEN, NEVER RUN.
import { writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { mkdirSync } from 'node:fs';
import { config, root, trackedFiles, isText, read, git, treeIsClean, todos, count } from './common.mjs';

const apply = process.argv.includes('--apply');
const say = (m) => console.log(m);

if (apply) {
  const bad = todos();
  if (bad.length) { console.error('refusing: rename.config.json still has TODO for: ' + bad.join(', ')); process.exit(1); }
  if (!treeIsClean()) { console.error('refusing: the git tree is not clean. Commit or stash first.'); process.exit(1); }
  const branch = git(['rev-parse', '--abbrev-ref', 'HEAD']).trim();
  if (branch === 'main') { console.error('refusing: do this on a branch, not main.'); process.exit(1); }
} else {
  say('DRY RUN (add --apply to change anything). TODOs: ' + (todos().join(', ') || 'none'));
}

const replaceInFiles = (label, pairs, files) => {
  let changed = 0;
  let tokens = 0;
  for (const f of files) {
    if (!isText(f)) continue;
    const t = read(f);
    let u = t;
    for (const [a, b] of pairs) u = u.split(a).join(b);
    if (u !== t) {
      changed++;
      tokens += pairs.reduce((n, [a]) => n + count(t, a), 0);
      if (apply) writeFileSync(join(root, f), u);
    }
  }
  say(`${label}: ${changed} files, ${tokens} replacements`);
};

// 1 - package
let files = trackedFiles();
const pkgFrom = config.oldPackage;
const pkgTo = config.newPackage === 'TODO' ? '<newPackage>' : config.newPackage;
replaceInFiles('1a package text', [[pkgFrom, pkgTo]], files);
const dirFrom = pkgFrom.split('.').join('/');
const dirTo = pkgTo.split('.').join('/');
const pkgDirs = new Set();
for (const f of files) {
  const i = f.indexOf('/' + dirFrom + '/');
  if (i >= 0) pkgDirs.add(f.slice(0, i) + '/' + dirFrom);
}
for (const d of [...pkgDirs].sort()) {
  say(`1b move ${d} -> ${d.replace(dirFrom, dirTo)}`);
  if (apply) {
    const dest = d.replace(dirFrom, dirTo);
    mkdirSync(join(root, dirname(dest)), { recursive: true });
    git(['mv', d, dest]);
  }
}

// 2 - id token (re-list: the moves changed paths)
files = apply ? trackedFiles() : files;
replaceInFiles('2 id token', [[config.oldId, config.newId]], files);

// 3 - path segments and file names containing the old id, deepest first
files = apply ? trackedFiles() : files;
const renames = new Map();
for (const f of files) {
  const parts = f.split('/');
  for (let i = 0; i < parts.length; i++) {
    if (parts[i].includes(config.oldId)) {
      const from = parts.slice(0, i + 1).join('/');
      renames.set(from, from.split('/').slice(0, -1).concat(parts[i].split(config.oldId).join(config.newId)).join('/'));
    }
  }
}
const ordered = [...renames.entries()].sort((a, b) => b[0].split('/').length - a[0].split('/').length);
for (const [from, to] of ordered) {
  say(`3 move ${from} -> ${to}`);
  if (apply) {
    mkdirSync(join(root, dirname(to)), { recursive: true });
    git(['mv', from, to]);
  }
}

// 4 - display strings
files = apply ? trackedFiles() : files;
replaceInFiles('4 display strings', config.displayReplacements, files);

say(apply ? '\nDone, uncommitted. Now: node tools/rename/check.mjs, then the re-bakes in tools/rename/README.txt.' : '\nDry run finished. Nothing changed.');
