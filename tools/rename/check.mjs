// READ-ONLY verification after apply.mjs. Reports leftovers; changes nothing; runs no build.
//   node tools/rename/check.mjs
// WRITTEN, NEVER RUN.
import { config, trackedFiles, isText, read, count, fileExists } from './common.mjs';

const files = trackedFiles();
let fail = 0;
const left = { id: [], package: [], display: [] };
for (const f of files) {
  if (f.includes(config.oldId)) left.id.push('PATH ' + f);
  if (!isText(f)) continue;
  const t = read(f);
  if (count(t, config.oldId)) left.id.push(f);
  if (count(t, config.oldPackage)) left.package.push(f);
  if (count(t, config.oldDisplay)) left.display.push(f);
}
for (const [k, v] of Object.entries(left)) {
  console.log(`${k}: ${v.length} file(s) still carry the old ${k}`);
  for (const f of v.slice(0, 15)) console.log('  ' + f);
  if (k !== 'display' && v.length) fail++;
}
const must = [
  ['gradle.properties', 'mod_id=' + config.newId],
  ['neoforge-26.1.2/src/main/resources/META-INF/neoforge.mods.toml', 'modId = "' + config.newId + '"'],
];
for (const [f, needle] of must) {
  const ok = fileExists(f) && read(f).includes(needle);
  console.log((ok ? 'ok   ' : 'FAIL ') + f + ' contains ' + needle);
  if (!ok) fail++;
}
const mixins = 'neoforge-26.1.2/src/main/resources/' + config.newId + '.mixins.json';
console.log((fileExists(mixins) ? 'ok   ' : 'FAIL ') + mixins + ' exists');
if (!fileExists(mixins)) fail++;
console.log(fail ? '\nCHECK FAILED' : '\nCHECK PASSED (this proves nothing about the build: run the re-bakes)');
process.exit(fail ? 1 : 0);
