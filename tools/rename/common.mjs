// Shared helpers for plan.mjs / apply.mjs / check.mjs. WRITTEN, NEVER RUN.
import { execFileSync } from 'node:child_process';
import { readFileSync, existsSync } from 'node:fs';
import { dirname, join, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const here = dirname(fileURLToPath(import.meta.url));
export const root = join(here, '..', '..');
export const config = JSON.parse(readFileSync(join(here, 'rename.config.json'), 'utf8'));

/** Every tracked file (repo-relative, forward slashes), minus the excluded prefixes. */
export function trackedFiles() {
  const out = execFileSync('git', ['ls-files', '-z'], { cwd: root, maxBuffer: 1 << 28 }).toString('utf8');
  return out.split('\0').filter(Boolean).filter((f) => !config.excludePrefixes.some((p) => f.startsWith(p)));
}

export function isText(file) {
  const ext = extname(file).slice(1).toLowerCase();
  return config.textExtensions.includes(ext);
}

export function read(file) {
  return readFileSync(join(root, file), 'utf8');
}

export function count(haystack, needle) {
  if (!needle) return 0;
  let n = 0;
  for (let i = haystack.indexOf(needle); i !== -1; i = haystack.indexOf(needle, i + needle.length)) n++;
  return n;
}

export function git(args) {
  return execFileSync('git', args, { cwd: root, maxBuffer: 1 << 28 }).toString('utf8');
}

export function treeIsClean() {
  return git(['status', '--porcelain']).trim() === '';
}

export function todos() {
  const bad = [];
  const walk = (o, p) => {
    for (const [k, v] of Object.entries(o)) {
      if (k.startsWith('_')) continue;
      if (v === 'TODO') bad.push(p + k);
      else if (v && typeof v === 'object' && !Array.isArray(v)) walk(v, p + k + '.');
    }
  };
  walk(config, '');
  return bad;
}

export function fileExists(f) {
  return existsSync(join(root, f));
}
