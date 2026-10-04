// READ-ONLY inventory of what apply.mjs would change. Writes nothing, changes nothing.
//   node tools/rename/plan.mjs
// WRITTEN, NEVER RUN.
import { config, trackedFiles, isText, read, count } from './common.mjs';

const files = trackedFiles();
const byDir = new Map();
const bump = (dir, key, n) => {
  if (!n) return;
  const m = byDir.get(dir) ?? {};
  m[key] = (m[key] ?? 0) + n;
  byDir.set(dir, m);
};
let pathHits = [];
let textFiles = 0;
let skippedBinary = 0;
for (const f of files) {
  const top = f.split('/').slice(0, 2).join('/');
  if (f.includes(config.oldId)) pathHits.push(f);
  if (!isText(f)) { skippedBinary++; continue; }
  textFiles++;
  const t = read(f);
  bump(top, 'id', count(t, config.oldId));
  bump(top, 'package', count(t, config.oldPackage));
  for (const [from] of config.displayReplacements) bump(top, 'display:' + from, count(t, from));
  if (config.configFolder && config.configFolder.old) bump(top, 'configFolder(' + config.configFolder.old + ')', count(t, '"' + config.configFolder.old + '"'));
}
console.log(`tracked (not excluded): ${files.length}; text: ${textFiles}; binary/other left alone: ${skippedBinary}`);
console.log(`paths containing "${config.oldId}": ${pathHits.length}`);
for (const [dir, m] of [...byDir.entries()].sort()) console.log(dir.padEnd(48), JSON.stringify(m));
console.log('\nFirst 25 paths that would be renamed:');
for (const p of pathHits.slice(0, 25)) console.log('  ' + p);
console.log('\nCompare with the treatment: about 8400 mentions in 1123 Java files, 1000 asset files, 428 data files (counted 2026-10-03).');
