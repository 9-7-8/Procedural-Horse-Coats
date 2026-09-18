#!/usr/bin/env node
// Read an unattended test-yard run and report what it settled.
//
// The yard already says everything it does - DebugWorldWatch writes [watch],
// DebugYard* write [trace] test yard, BandLife writes [trace] herd - and a long
// run buries the answer in a hundred thousand lines. This pulls out the lines
// that decide a named gap and says, for each, what the run showed.
//
// It is deliberately a READER, not a judge, wherever a judgement needs the
// owner: it prints what happened and what the entry asked for, and only calls
// PASS/FAIL where the entry states a mechanical pass line.
//
//   node neoforge-26.1.2/tools/mine-yard-log.mjs [logfile ...]
//
// With no argument it reads run/logs/latest.log. Pass several to read across a
// midnight roll - latest.log restarts at 00:00 and the earlier part is in
// run/logs/<date>-N.log.gz, so an overnight run is always at least two files.

import { readFileSync, existsSync } from 'node:fs';
import { gunzipSync } from 'node:zlib';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const DEFAULT_LOG = resolve(HERE, '..', 'run', 'logs', 'latest.log');

function read(path) {
  if (!existsSync(path)) return '';
  const raw = readFileSync(path);
  return path.endsWith('.gz') ? gunzipSync(raw).toString('utf8') : raw.toString('utf8');
}

const files = process.argv.slice(2);
const paths = files.length ? files.map((f) => resolve(f)) : [DEFAULT_LOG];
const missing = paths.filter((p) => !existsSync(p));
if (missing.length) {
  console.error(`no such log: ${missing.join(', ')}`);
  process.exit(2);
}
const lines = paths.flatMap((p) => read(p).split(/\r?\n/));

const stamp = (l) => (l.match(/^\[(\d\d\w\w\w\d{4} \d\d:\d\d:\d\d)/) || [, '?'])[1];
const has = (re) => lines.filter((l) => re.test(l));
const section = (title) => console.log(`\n${'='.repeat(72)}\n${title}\n${'='.repeat(72)}`);
const verdict = (ok, text) => console.log(`  ${ok === null ? '·' : ok ? 'PASS' : 'FAIL'}  ${text}`);

// ---------------------------------------------------------------- run shape
section('The run');
const first = lines.find((l) => /^\[/.test(l));
const last = [...lines].reverse().find((l) => /^\[/.test(l));
console.log(`  files    ${paths.length}  (${paths.map((p) => p.split(/[\\/]/).pop()).join(', ')})`);
console.log(`  lines    ${lines.length.toLocaleString()}`);
console.log(`  from     ${stamp(first || '')}`);
console.log(`  to       ${stamp(last || '')}`);

const crashes = has(/Preparing crash report|Exception in thread|OutOfMemoryError|Watchdog/);
verdict(crashes.length === 0, `${crashes.length} crash/exception line(s)`);
crashes.slice(0, 5).forEach((l) => console.log(`        ${l.slice(0, 160)}`));

// ------------------------------------------------- gaps 260/262: soundness
section('Gaps 260/262 - the yard\'s own soundness check');
const sound = has(/test yard built and walkable/);
const unsound = has(/test yard is NOT sound/);
verdict(unsound.length === 0 && sound.length > 0,
  `${sound.length} walkable, ${unsound.length} NOT sound`);
unsound.slice(0, 5).forEach((l) => console.log(`        ${l.replace(/^.*\[Debug\] /, '')}`));

// ------------------------------------------------------- gap 234: closeKin
section('Gap 234 - a dispersing filly must not be recruited by her brother');
const left = has(/left the band it was born into/);
const id = (l) => (l.match(/\[([0-9a-f]{8})\]/) || [, '?'])[1];
const label = (l) => (l.match(/"([^"]+)"/) || [, ''])[1];
const colt = left.find((l) => /COLT/.test(label(l)));
const filly = left.find((l) => /FILLY/.test(label(l)));
console.log(`  dispersals logged: ${left.length}`);
if (colt && filly) {
  const coltId = id(colt);
  // The last watch line that mentions her tells us the band she settled in.
  const watch = [...lines].reverse().find((l) => /\[watch\]/.test(l) && /FILLY: LEAVES NOW/.test(l));
  const hers = watch && (watch.match(/FILLY: LEAVES NOW: [A-Za-z ]*of ([0-9a-f]{8})/) || [])[1];
  console.log(`  colt  ${coltId} dispersed at ${stamp(colt)}`);
  console.log(`  filly ${id(filly)} dispersed at ${stamp(filly)}, now in band ${hers || '(no watch line yet)'}`);
  if (hers) {
    verdict(hers !== coltId,
      hers === coltId
        ? `she joined her brother's band (${hers}) - closeKin did not refuse him`
        : `she joined ${hers}, which is not her brother's band (${coltId})`);
  } else {
    verdict(null, 'no [watch] line for her yet - let the run go longer');
  }
} else {
  verdict(null, 'the LEAVING HOME pen has not dispersed both siblings yet');
}

// ----------------------------------------------- gap 245/225: ratio pens
section('Gaps 245/225 - the ratio pens, real draws against the shadow');
const tallies = new Map();
for (const l of lines) {
  const m = l.match(/RATIO ([A-Z0-9 ]+?) conception .*?shadow tally \d+: (.*?) \| expect (.*)$/);
  if (m) tallies.set(m[1].trim(), { shadow: m[2].trim(), expect: m[3].trim() });
}
const foals = new Map();
for (const l of lines) {
  const m = l.match(/RATIO ([A-Z0-9 ]+?): foal \d+ is ([^|]+?)\s*(?:\||$)/);
  if (!m) continue;
  const pen = m[1].trim();
  if (!foals.has(pen)) foals.set(pen, new Map());
  const k = m[2].trim();
  foals.get(pen).set(k, (foals.get(pen).get(k) || 0) + 1);
}
const pens = [...new Set([...tallies.keys(), ...foals.keys()])].sort();
if (!pens.length) verdict(null, 'no ratio conceptions logged yet');
for (const pen of pens) {
  const real = foals.get(pen);
  const n = real ? [...real.values()].reduce((a, b) => a + b, 0) : 0;
  console.log(`\n  ${pen}`);
  console.log(`    real   n=${n}  ${real ? [...real].map(([k, v]) => `${k} ${v}`).join(', ') : '(none born yet)'}`);
  if (tallies.has(pen)) {
    console.log(`    shadow ${tallies.get(pen).shadow}`);
    console.log(`    expect ${tallies.get(pen).expect}`);
  }
}

// ---------------------------------------------------- gap 258: re-covering
section('Gap 258 - a mare who loses a pregnancy is covered again');
const loss = has(/early loss/);
const cover = has(/natural cover/);
console.log(`  early loss lines:    ${loss.length}`);
console.log(`  natural cover lines: ${cover.length}`);
if (loss.length) {
  const lastLoss = loss[loss.length - 1];
  const after = lines.indexOf(lastLoss);
  const coveredAfter = lines.slice(after).filter((l) => /natural cover/.test(l)).length;
  verdict(coveredAfter > 0,
    `${coveredAfter} natural cover(s) after the last early loss (${stamp(lastLoss)})`);
} else {
  verdict(null, 'no early loss yet - the pass needs at least one, then a cover after it');
}
const refusals = {};
for (const l of lines) {
  const m = l.match(/not covered: ([a-z][a-z ,]*)/);
  if (m) refusals[m[1].trim()] = (refusals[m[1].trim()] || 0) + 1;
}
const refKeys = Object.keys(refusals);
console.log(`  refusal reasons: ${refKeys.length ? refKeys.map((k) => `${k} x${refusals[k]}`).join('; ') : '(none)'}`);
verdict(refKeys.some((k) => /hurt/.test(k)) || null,
  'a hurt mare should say so once a minute rather than going silent - silence is what hid this gap');

// -------------------------------------------------------- gap 243: lycans
section('Gap 243 - a flying lycan must land, not fall out of the world');
// NOT a bare /outOfWorld/. Several pens print their own pass line in prose -
// "(expect within 8; a sheep 'from outOfWorld' is a FAIL)" - so grepping the
// word alone reports the sign on the pen as though it were the failure it warns
// about. A real one is a death line; the expectation text always says "expect".
const outOfWorld = has(/outOfWorld/).filter((l) => !/expect|'from outOfWorld'/.test(l));
const setDown = has(/set down at/);
verdict(outOfWorld.length === 0, `${outOfWorld.length} outOfWorld death(s)`);
outOfWorld.slice(0, 4).forEach((l) => console.log(`        ${l.slice(0, 150)}`));
console.log(`  'set down at' lines: ${setDown.length}` +
  (setDown.length === 0 ? '  (the landing path is still unexercised - needs a flying shifter)' : ''));

// ------------------------------------------------------ gap 259: packing
section('Gap 259 - no pen\'s watch line may list a horse from the pen next door');
// The entry's own two measurables, and nothing inferred. Guessing which horse
// belongs to which pen from its label does not work - a pen called DAM DEFENCE
// holds horses called DAM and FOAL, several pens are one YardPens group on
// purpose, and a name-prefix rule flags all of them. So report the counts the
// log states outright and leave the reading to whoever knows the pens.
const penLines = lines.filter((l) => /\[watch\] /.test(l) && / \| social: /.test(l));
const overCount = [];
for (const l of lines) {
  const m = l.match(/\[watch\] ([A-Z0-9 :'-]+?) \| horses (\d+)/);
  const nc = l.match(/not covered: [^|]*?(\d+) other horses/);
  if (m && nc && Number(nc[1]) > Number(m[2])) {
    overCount.push({ pen: m[1].trim(), held: Number(m[2]), counted: Number(nc[1]), at: stamp(l) });
  }
}
console.log(`  social watch lines: ${penLines.length}`);
verdict(overCount.length === 0,
  `${overCount.length} line(s) counting more horses than the pen holds`);
overCount.slice(0, 6).forEach((o) =>
  console.log(`        ${o.at}  ${o.pen}: holds ${o.held}, counted ${o.counted}`));
const occupancy = new Map();
for (const l of lines) {
  const m = l.match(/\[watch\] ([A-Z0-9 :'-]+?) \| horses (\d+)/);
  if (m) {
    const pen = m[1].trim();
    const n = Number(m[2]);
    const prev = occupancy.get(pen);
    if (!prev || n > prev.max) occupancy.set(pen, { max: n });
  }
}
const busiest = [...occupancy].sort((a, b) => b[1].max - a[1].max).slice(0, 6);
console.log('  peak occupancy by pen (a pen well over its stock is the bug to chase):');
busiest.forEach(([pen, o]) => console.log(`        ${String(o.max).padStart(3)}  ${pen}`));

// ------------------------------------------------------- mutation, births
section('Mutation and births');
const births = has(/\[trace\] (foal|birth)/i);
const mutations = has(/mutation|magical gene neither parent/i);
console.log(`  birth lines:    ${births.length}`);
console.log(`  mutation lines: ${mutations.length}`);
mutations.slice(0, 5).forEach((l) => console.log(`        ${l.slice(0, 160)}`));

section('Done');
console.log('  Anything marked · needs either a longer run or a human. Nothing here');
console.log('  closes a gap on its own - put the evidence on the owning page.\n');
