// Adds the READ-ONLY old-prefix alias to Genes.java AFTER apply.mjs has run (so the literal old id survives).
//   node tools/rename/apply-alias.mjs          (dry run: says what it would do)
//   node tools/rename/apply-alias.mjs --apply
// Idempotent: does nothing if the alias is already there. The same edit as staged/read-old-prefix.patch, which is
// the reference for a PRE-rename tree; this script finds Genes.java wherever the package move put it.
// Also see staged/LegacyPrefixTest.java.txt for the test to add next to it. WRITTEN, NEVER RUN.
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { root, git, config } from './common.mjs';

const apply = process.argv.includes('--apply');
const path = git(['ls-files', '*/common/genetics/Genes.java']).trim().split('\n').filter(Boolean)[0];
if (!path) { console.error('Genes.java not found'); process.exit(1); }
let t = readFileSync(join(root, path), 'utf8');
if (t.includes('LEGACY_PREFIX')) { console.log('alias already present in ' + path); process.exit(0); }
const legacy = config.oldId + '.';
const edits = [
  [/    public static Gene byKey\(String geneKey\) \{\n        Gene g = byKey\.get\(geneKey\);\n/,
`    /**
     * Genes saved before the rename carry the old namespace in their key ("${legacy}extension"). The old
     * prefix is READ for the whole 0.6 series and never written (rename treatment, Decided); it goes when the
     * next compat series starts. Pasted codes, saved designer links and drop-in files from before keep working.
     */
    private static final String LEGACY_PREFIX = "${legacy}";

    private static String modernKey(String key) {
        if (key != null && key.startsWith(LEGACY_PREFIX)) {
            return NS + "." + key.substring(LEGACY_PREFIX.length());
        }
        return key;
    }

    public static Gene byKey(String geneKey) {
        Gene g = byKey.get(modernKey(geneKey));
`],
  [/    public static Gene byKeyOrNull\(String geneKey\) \{\n        return byKey\.get\(geneKey\);/,
   `    public static Gene byKeyOrNull(String geneKey) {\n        return byKey.get(modernKey(geneKey));`],
  [/    public static Allele allele\(String alleleKey\) \{\n        Allele a = alleleByKey\.get\(alleleKey\);/,
   `    public static Allele allele(String alleleKey) {\n        Allele a = alleleByKey.get(modernKey(alleleKey));`],
];
for (const [re, to] of edits) {
  if (!re.test(t)) { console.error('pattern not found in ' + path + ': ' + re); process.exit(1); }
  t = t.replace(re, to);
}
console.log((apply ? 'wrote ' : 'would write ') + path);
if (apply) writeFileSync(join(root, path), t);
