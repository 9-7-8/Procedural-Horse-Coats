// Bake the config key table on wiki/config.html from the four config classes.
//
//   node wiki/tools/bake-config-keys.mjs           rewrite the generated span
//   node wiki/tools/bake-config-keys.mjs --check   exit 1 if the span is stale
//
// The source of truth is the Java: ServerConfig, ClientConfig, BreedSpawningConfig
// and CartsConfig. This reads their ModConfigSpec.Builder chains the way NeoForge
// does - a comment() waits for the next define (or push, which takes it as the
// section's comment), push()/pop() build the dotted path - and resolves a default
// that names a constant by reading that constant, in the same file or in common/.
// Nothing about a key is typed twice: the only hand-written thing here is HOME,
// which says what feature page each key belongs to.
//
// It hard-fails rather than writing a short table:
//   - every `.define*(` in a source must have become a row (a call this cannot
//     read is a call the table would silently drop);
//   - every key must have a HOME, and every HOME must name a live key and a
//     page#anchor that exists;
//   - a default, range or comment it cannot evaluate is an error, not a blank.
//
// Rebake row: procedures/rebake.txt, "A config key". The table is wiki prose, so
// row i (search index, links, agent text) runs after it.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const NF = 'neoforge-26.1.2/src/main/java/com/example/horsegenetics/neoforge';
const COMMON = 'common/src/main/java';
const PAGE = 'wiki/config.html';
const BEGIN = '<!-- BEGIN generated config keys: node wiki/tools/bake-config-keys.mjs -->';
const END = '<!-- END generated config keys -->';

// One entry per spec. `builder` is where the Builder chain starts in the file
// (a method or constructor name), `until` where it stops; CartsConfig keeps two
// specs in two inner classes, so each gets its own slice.
const SPECS = [
    { id: 'server', file: `${NF}/ServerConfig.java`, from: 'static {', until: 'SPEC = builder.build();',
      side: 'Server', where: '<code>.minecraft/phc/server.toml</code>, overridden per world by <code>&lt;world&gt;/phc/server.toml</code>',
      blurb: 'Gameplay. One answer for everybody on a server, synced to each client as it joins - a client may read these but never set them.' },
    { id: 'client', file: `${NF}/ClientConfig.java`, from: 'static {', until: 'SPEC = builder.build();',
      side: 'Client', where: '<code>.minecraft/phc/client.toml</code>',
      blurb: 'This machine only: what it draws, and a few remembered preferences. Two players on one server may disagree about every one of these.' },
    { id: 'breeds', file: `${NF}/BreedSpawningConfig.java`, from: 'public static ModConfigSpec build()', until: 'spec = b.build();',
      side: 'Common', where: '<code>.minecraft/phc/breed-spawning.toml</code>',
      blurb: 'Which horses a world has. Built from the installed breed roster at startup, so it is COMMON rather than synced: a dedicated server reads its own copy.' },
    { id: 'carts', file: `${NF}/carts/CartsConfig.java`, from: 'Common(final ModConfigSpec.Builder builder)', until: null, also: 'CartConfig(final ModConfigSpec.Builder builder, final CartKind kind)',
      side: 'Common', where: '<code>.minecraft/config/horsegenetics-carts.toml</code>',
      blurb: 'Per cart. How fast a horse pulls one is genetics, not configuration, so there is no speed here.' },
    { id: 'carts-client', file: `${NF}/carts/CartsConfig.java`, from: 'Client(final ModConfigSpec.Builder builder)', until: 'public static class Common',
      side: 'Client', where: '<code>.minecraft/config/horsegenetics-carts-client.toml</code>',
      blurb: 'What the supply cart draws on its bed.' },
];

// A push() or default that is not a literal: what to print for it. Anything not
// listed here, and not a constant this can resolve, fails the bake.
const PLACEHOLDERS = {
    'breed.id()': '<breed id>',
    'kind.id()': '<cart id>',
    'breed.spawnWeight()': { text: "the breed file's", html: 'the breed file&rsquo;s' },
    'breed.biomes()': { text: "the breed file's", html: 'the breed file&rsquo;s' },
    'breed.spawnTime().id()': { text: "the breed file's", html: 'the breed file&rsquo;s' },
    '!production()': { text: 'true in a dev run, false in an install', html: '<code>true</code> in a dev run, <code>false</code> in an install' },
    'commonnessScale()': '(the commonness names and their weights, printed in the file)',
};

// The feature page each key belongs to. A new key fails the bake until it has one:
// the table is the index, the page is where the key is explained.
const HOME = {
    'health.mode': 'horse-body.html#config',
    'body.size': 'horse-body.html#body-size',
    'ride.instant_jump': 'horse-body.html#jumps-0920-meter',
    'ride.jump_cooldown_ticks': 'horse-body.html#jumps-0920-meter',
    'ride.jump_forward_boost': 'horse-body.html#jumps-0920-meter',
    'fertility.gestation_days': 'fertility.html#setting',
    'realm.breeding_rate_percent': 'fertility.html#realm-rate',
    'realm.pause_when_unloaded': 'fertility.html#realm-pause',
    'realm.release_emeralds': 'horse-realm.html#releasing',
    'wild.despawn_days': 'breeds.html#turnover',
    'wild.realm_handoff': 'horse-realm.html#wild-arrivals',
    'wild.topup_minimum': 'breeds.html#turnover',
    'wild.topup_cell_chunks': 'breeds.html#turnover',
    'wild.topup_player_radius': 'breeds.html#turnover',
    'ops.resurrect_grace_minutes': 'horse-afterlife.html#window',
    'ops.resurrect_budget_mb': 'horse-afterlife.html#window',
    'fertility.nearby_horse_cap': 'fertility.html#setting',
    'fertility.free_covers_per_day': 'fertility.html#setting-free-covers',
    'notices.owned_horse_damage': 'horse-care.html#hurt-notice',
    'notices.owned_horse_death': 'horse-care.html#death-notice',
    'notices.owned_horse_breeding': 'fertility.html#natural-notice',
    'behaviour.escape_health_fraction': 'horse-care.html#escape',
    'behaviour.emergency_stasis_fraction': 'horse-stasis.html#emergency-gameplay',
    'behaviour.last_stand': 'horse-care.html#last-stand',
    'behaviour.last_stand_health': 'horse-care.html#last-stand',
    'behaviour.last_stand_immunity_ticks': 'horse-care.html#last-stand',
    'behaviour.last_stand_rearm_fraction': 'horse-care.html#last-stand',
    'behaviour.leads_return': 'rider-comfort.html#leads',
    'behaviour.mounted_mining_penalty_removed': 'rider-comfort.html#mounted-mining',
    'behaviour.rightclick_equips_tack': 'rider-comfort.html#tack-equip',
    'behaviour.bond_decay_per_day': 'horse-care.html#bond-decay',
    'behaviour.bond_floor': 'horse-care.html#bond-decay',
    'behaviour.owner_only_riding': 'horse-care.html#riding',
    'behaviour.riding_allows_teams': 'horse-care.html#riding',
    'commands.horse_give': 'item-transfer-papers.html#horsegive',
    'commands.horse_jockey': 'item-jockey-passes.html#command',
    'undead.convert': 'undead-horses.html#config',
    'behaviour.jockey_pass_days': 'item-jockey-passes.html#command',
    'behaviour.send_home_payment_item': 'horse-browser.html#send-home',
    'behaviour.send_home_payment_count': 'horse-browser.html#send-home',
    'behaviour.send_home_cooldown_seconds': 'horse-browser.html#send-home',
    'chaos.exclude_mods': 'gene-lycan.html#chaos',
    'chaos.exclude_ids': 'gene-lycan.html#chaos',
    'debug.announce': 'architecture.html#verified-debug-announce',
    'debug.tools': 'horse-dimension.html',

    'familyTree.scrollBar': 'breeding.html#verified-family-tree',
    'nameplate.sexSymbol': 'gene-sex.html',
    'whistle.keyPrompt': 'item-whistles.html',
    'search.saved': 'breeding.html#saved-searches',
    'genes.filter': 'breeding.html#H',
    'tutorial.seen': 'progression.html',
    'coats.detailDistance': 'pipeline.html#bake-budget',
    'coats.bakeBudgetMs': 'pipeline.html#bake-budget',
    'parts.enabled': 'model-parts.html#settings',
    'parts.glow': 'model-parts.html#settings',
    'parts.detailDistance': 'model-parts.html#settings',
    'visual.rideFade': 'rider-comfort.html#ride-fade',
    'visual.rideFadeMinOpacity': 'rider-comfort.html#ride-fade',
    'visual.rideFadeStartPitch': 'rider-comfort.html#ride-fade',
    'visual.rideFadeFullPitch': 'rider-comfort.html#ride-fade',
    'naming.inheritedHalf': 'breeding.html#naming-policy',
    'naming.parentSource': 'breeding.html#naming-policy',

    'general.shipped_breeds_enabled': 'breeds.html#world-settings',
    'feral_mixed.enabled': 'breeds.html#world-settings',
    'feral_mixed.biomes': 'breeds.html#world-settings',
    'feral_mixed.herd_weight': 'breeds.html#world-settings',
    'magical_herds.enabled': 'breeds.html#magical-herds',
    'magical_herds.chance': 'breeds.html#magical-herds',
    'breeds.<breed id>.enabled': 'breeds.html#world-settings',
    'breeds.<breed id>.spawn_weight': 'breeds.html#world-settings',
    'breeds.<breed id>.biomes': 'breeds.html#world-settings',
    'breeds.<breed id>.spawn_time': 'breeds.html#world-settings',

    'carts.<cart id>.pull_animals': 'carts.html#animals',
    'carts.<cart id>.adventure_mode_interact': 'carts.html',
    'render_supplies': 'carts.html#supply',
    'render_supply_gear': 'carts.html#supply',
    'render_supply_flowers': 'carts.html#supply',
    'render_supply_paintings': 'carts.html#supply',
    'render_supply_wheel': 'carts.html#supply',
    'render_item_blacklist': 'carts.html#supply',
};

// ---------------------------------------------------------------- reading Java

const read = rel => fs.readFileSync(path.join(ROOT, rel), 'utf8');

class BakeError extends Error {}
function fail(msg) {
    throw new BakeError(msg);
}

/** Strip comments, keeping strings and character literals intact and offsets stable. */
function stripComments(src) {
    let out = '';
    for (let i = 0; i < src.length;) {
        const c = src[i], d = src[i + 1];
        if (c === '"' || c === '\'') {
            const j = endOfQuoted(src, i);
            out += src.slice(i, j);
            i = j;
        } else if (c === '/' && d === '/') {
            const j = src.indexOf('\n', i);
            const stop = j < 0 ? src.length : j;
            out += ' '.repeat(stop - i);
            i = stop;
        } else if (c === '/' && d === '*') {
            const j = src.indexOf('*/', i + 2) + 2;
            out += src.slice(i, j).replace(/[^\n]/g, ' ');
            i = j;
        } else {
            out += c;
            i++;
        }
    }
    return out;
}

function endOfQuoted(src, i) {
    const q = src[i];
    let j = i + 1;
    while (j < src.length && src[j] !== q) j += src[j] === '\\' ? 2 : 1;
    return j + 1;
}

/** The text between the '(' at `open` and its matching ')', and the index after it. */
function balanced(src, open) {
    let depth = 0;
    for (let i = open; i < src.length; i++) {
        const c = src[i];
        if (c === '"' || c === '\'') { i = endOfQuoted(src, i) - 1; continue; }
        if (c === '(' || c === '{' || c === '[') depth++;
        else if (c === ')' || c === '}' || c === ']') {
            depth--;
            if (depth === 0) return { inner: src.slice(open + 1, i), after: i + 1 };
        }
    }
    fail(`unbalanced parentheses at offset ${open}`);
}

/** Split an argument list on its top-level commas. */
function splitArgs(s) {
    const out = [];
    let depth = 0, start = 0;
    for (let i = 0; i < s.length; i++) {
        const c = s[i];
        if (c === '"' || c === '\'') { i = endOfQuoted(s, i) - 1; continue; }
        if (c === '(' || c === '{' || c === '[') depth++;
        else if (c === ')' || c === '}' || c === ']') depth--;
        else if (c === '<' && /\w$/.test(s.slice(0, i).trimEnd())) depth++;   // generics
        else if (c === '>' && depth > 0 && s[i - 1] !== '-') depth--;
        else if (c === ',' && depth === 0) { out.push(s.slice(start, i).trim()); start = i + 1; }
    }
    const last = s.slice(start).trim();
    if (last) out.push(last);
    return out;
}

function unescapeJava(lit) {
    return lit.slice(1, -1).replace(/\\(u[0-9a-fA-F]{4}|.)/g, (_, e) => {
        if (e[0] === 'u') return String.fromCharCode(parseInt(e.slice(1), 16));
        return { n: '\n', t: '\t', '"': '"', '\'': '\'', '\\': '\\' }[e] ?? e;
    });
}

// ---------------------------------------------------------------- constants

const sourceCache = new Map();
function javaSource(rel) {
    if (!sourceCache.has(rel)) sourceCache.set(rel, stripComments(read(rel)));
    return sourceCache.get(rel);
}

/** The file a fully qualified or simple class name lives in, from `context`'s imports. */
function classFile(name, contextFile) {
    if (name.startsWith('com.example.horsegenetics.common.')) {
        const rel = `${COMMON}/${name.replace(/\./g, '/')}.java`;
        if (fs.existsSync(path.join(ROOT, rel))) return rel;
    }
    const ctx = javaSource(contextFile);
    const imp = ctx.match(new RegExp(`import\\s+(com\\.example\\.horsegenetics\\.[\\w.]+\\.${name});`));
    if (imp) {
        const pkg = imp[1].startsWith('com.example.horsegenetics.common.') ? COMMON : 'neoforge-26.1.2/src/main/java';
        const rel = `${pkg}/${imp[1].replace(/\./g, '/')}.java`;
        if (fs.existsSync(path.join(ROOT, rel))) return rel;
    }
    const sibling = path.posix.join(path.posix.dirname(contextFile), `${name}.java`);
    if (fs.existsSync(path.join(ROOT, sibling))) return sibling;
    return null;
}

/** The initializer of a static final field `name` in `file`. */
function fieldInitializer(file, name) {
    const src = javaSource(file);
    const m = src.match(new RegExp(`\\b${name}\\s*=\\s*([^;]+);`));
    return m ? m[1].trim() : null;
}

/**
 * Evaluate a default / bound / comment fragment. Returns {text, html, list?}:
 * text for searching and the --check diff, html for the page.
 */
function evaluate(expr, file) {
    expr = expr.trim();
    if (expr in PLACEHOLDERS) {
        const p = PLACEHOLDERS[expr];
        return typeof p === 'string' ? { text: p, html: esc(p) } : p;
    }
    if (/^"(?:[^"\\]|\\.)*"$/.test(expr)) {
        const s = unescapeJava(expr);
        return { text: s, html: esc(s), string: true };
    }
    if (/^-?[\d_]+(\.[\d_]+)?([eE]-?\d+)?[dDfFL]?$/.test(expr)) {
        const n = expr.replace(/_/g, '').replace(/[dDfFL]$/, '');
        return { text: n, html: `<code>${n}</code>` };
    }
    if (expr === 'true' || expr === 'false') return { text: expr, html: `<code>${expr}</code>` };
    // String concatenation: every part must evaluate.
    const parts = splitConcat(expr);
    if (parts.length > 1) {
        const vals = parts.map(p => evaluate(p, file));
        const s = vals.map(v => v.text).join('');
        return { text: s, html: esc(s), string: true };
    }
    // Empty lists, written three ways.
    if (/^(java\.util\.)?List\.of\(\)$/.test(expr) || /^new ArrayList<\w*>\(\)$/.test(expr)) {
        return { text: '[]', html: '<code>[]</code>', list: [] };
    }
    // A list built from string literals: Arrays.asList("a", "b") and the like.
    const literals = [...expr.matchAll(/"((?:[^"\\]|\\.)*)"/g)];
    if (/^new ArrayList<\w*>\((java\.util\.)?Arrays\.asList\(/.test(expr) && literals.length) {
        const list = literals.map(m => unescapeJava(`"${m[1]}"`));
        return { text: list.join(', '), html: list.map(v => `<code>${esc(v)}</code>`).join(', '), list };
    }
    // A local list filled by .add() calls, as CartsConfig's blacklist is.
    if (/^\w+$/.test(expr)) {
        const src = javaSource(file);
        const adds = [...src.matchAll(new RegExp(`\\b${expr}\\.add\\(("(?:[^"\\\\]|\\\\.)*")\\)`, 'g'))];
        if (adds.length && new RegExp(`ArrayList<String>\\s+${expr}\\s*=\\s*new ArrayList<>\\(\\)`).test(src)) {
            const list = adds.map(m => unescapeJava(m[1]));
            return { text: `[${list.join(', ')}]`, html: list.map(v => `<code>${esc(v)}</code>`).join(', '), list };
        }
    }
    // A constant: NAME, Class.NAME, or a fully qualified one.
    const m = expr.match(/^((?:[\w]+\.)*?)([A-Z][A-Z0-9_]*)$/);
    if (m) {
        const owner = m[1].replace(/\.$/, '');
        const target = owner ? classFile(owner.includes('.') && !owner.startsWith('com.') ? owner.split('.').pop() : owner, file) : file;
        const init = target ? fieldInitializer(target, m[2]) : null;
        if (init != null) return evaluate(init, target);
        // Not a field anybody declares: Type.VALUE is an enum constant.
        if (owner) return { text: m[2], html: `<code>${m[2]}</code>`, enumType: owner };
        fail(`cannot find the constant ${expr} (from ${file})`);
    }
    fail(`cannot evaluate "${expr}" in ${file} - add it to PLACEHOLDERS if it is meant to be shown as words`);
}

function splitConcat(s) {
    const out = [];
    let depth = 0, start = 0;
    for (let i = 0; i < s.length; i++) {
        const c = s[i];
        if (c === '"' || c === '\'') { i = endOfQuoted(s, i) - 1; continue; }
        if (c === '(') depth++;
        else if (c === ')') depth--;
        else if (c === '+' && depth === 0) { out.push(s.slice(start, i).trim()); start = i + 1; }
    }
    out.push(s.slice(start).trim());
    return out;
}

/** The constants of enum `type` ("HealthMode", "NamingPolicy.InheritedHalf"), from `file`'s view. */
function enumConstants(type, file) {
    const segs = type.split('.');
    let target = segs.length > 1 ? classFile(segs[0], file) : file;
    if (!target) target = classFile(segs[segs.length - 1], file);
    if (!target) fail(`cannot find enum ${type} (from ${file})`);
    const src = javaSource(target);
    const name = segs[segs.length - 1];
    const at = src.search(new RegExp(`enum\\s+${name}\\b[^{]*\\{`));
    if (at < 0) fail(`no enum ${name} in ${target}`);
    const body = src.slice(src.indexOf('{', at) + 1);
    const head = body.slice(0, body.search(/[;}]/));
    return head.split(',').map(s => s.trim().match(/^([A-Z][A-Z0-9_]*)/)).filter(Boolean).map(x => x[1]);
}

// ---------------------------------------------------------------- walking a spec

const DEFINE = /\.(define|defineInRange|defineEnum|defineList|defineListAllowEmpty|defineInList)\s*\(/y;

function walk(spec) {
    const src = javaSource(spec.file);
    const slices = [];
    const start = src.indexOf(spec.from);
    if (start < 0) fail(`${spec.file}: cannot find "${spec.from}"`);
    let stop = spec.until ? src.indexOf(spec.until, start) : -1;
    if (spec.until && stop < 0) fail(`${spec.file}: cannot find "${spec.until}"`);
    if (stop < 0) stop = src.length;
    slices.push([start, stop]);

    const rows = [];
    const sectionComments = [];
    const pathStack = [];
    let pending = null;      // a comment waiting for the define (or push) that takes it
    let rawDefines = 0;

    const scan = (from, to, inner) => {
        for (let i = from; i < to; i++) {
            const c = src[i];
            if (c === '"' || c === '\'') { i = endOfQuoted(src, i) - 1; continue; }
            if (c !== '.') continue;
            const rest = src.slice(i, i + 40);
            let m;
            if ((m = rest.match(/^\.comment\s*\(/))) {
                const { inner: args, after } = balanced(src, i + m[0].length - 1);
                pending = splitArgs(args);      // evaluated by whatever takes it
                i = after - 1;
            } else if ((m = rest.match(/^\.push\s*\(/))) {
                const { inner: arg, after } = balanced(src, i + m[0].length - 1);
                const seg = arg.trim();
                const name = /^"/.test(seg) ? unescapeJava(seg) : evaluate(seg, spec.file).text;
                pathStack.push(name);
                // A section's comment may be built from the loop variable ("The " +
                // kind.id() ...); only a key's own comment has to be readable.
                let lines = [];
                try { lines = (pending ?? []).map(a => evaluate(a, spec.file).text); } catch (e) { if (!(e instanceof BakeError)) throw e; }
                sectionComments.push({ path: pathStack.join('.'), lines });
                pending = null;
                i = after - 1;
            } else if ((m = rest.match(/^\.pop\s*\(\s*\)/))) {
                if (!pathStack.length) fail(`${spec.file}: pop() with nothing pushed`);
                pathStack.pop();
                i += m[0].length - 1;
            } else {
                DEFINE.lastIndex = 0;
                const d = DEFINE.exec(rest);
                if (!d) continue;
                rawDefines++;
                const { inner: args, after } = balanced(src, i + d[0].length - 1);
                const own = pending && pending.map(a => evaluate(a, spec.file).text);
                rows.push(row(spec, d[1], splitArgs(args), [...pathStack], own, sectionComments));
                pending = null;
                i = after - 1;
            }
        }
        // CartsConfig builds each cart's section in a constructor the walk reaches by
        // a loop, not by reading on: descend into it where the loop calls it.
        if (inner) inner();
    };

    if (spec.also) {
        // Read the outer constructor up to `new CartConfig(...)`, then the per-cart one.
        const at = src.indexOf(spec.also);
        if (at < 0) fail(`${spec.file}: cannot find "${spec.also}"`);
        const ctorBody = balanced(src, src.indexOf('{', start));
        const outerEnd = ctorBody.after;
        const call = src.indexOf('new CartConfig(', start);
        scan(start, call, null);
        const cartBody = balanced(src, src.indexOf('{', at));
        scan(src.indexOf('{', at), cartBody.after, null);
        scan(call, outerEnd, null);
    } else {
        scan(start, stop, null);
    }

    // The disproof check: every define call in the slice became a row.
    const text = spec.also
        ? src.slice(start, balanced(src, src.indexOf('{', start)).after) + src.slice(src.indexOf(spec.also), balanced(src, src.indexOf('{', src.indexOf(spec.also))).after)
        : src.slice(start, stop);
    const counted = (text.match(/\.define\w*\s*\(/g) || []).length;
    if (counted !== rows.length) fail(`${spec.file} (${spec.id}): ${counted} define calls but ${rows.length} rows read`);
    if (pathStack.length) fail(`${spec.file}: ${pathStack.length} push() left open`);
    return { rows, sections: sectionComments };
}

function row(spec, kind, args, pathStack, comment, sections) {
    const key = [...pathStack, unescapeJava(args[0])].join('.');
    let lines = comment;
    if (!lines) {
        // No comment of its own: the enclosing section's comment documents it as a
        // "  name - ..." line, as BreedSpawningConfig's per-breed keys are.
        const leaf = key.split('.').pop();
        for (let p = pathStack.length; p > 0 && !lines; p--) {
            const sec = sections.find(s => s.path === pathStack.slice(0, p).join('.'));
            if (!sec) continue;
            const at = sec.lines.findIndex(l => new RegExp(`^\\s+${leaf}\\s+-`).test(l));
            if (at < 0) continue;
            lines = [sec.lines[at].replace(new RegExp(`^\\s+${leaf}\\s+-\\s*`), '')];
            for (let j = at + 1; j < sec.lines.length && /^\s{6,}/.test(sec.lines[j]); j++) lines.push(sec.lines[j].trim());
        }
    }
    if (!lines || !lines.length) fail(`${spec.file}: ${key} has no comment, and no section comment describes it`);

    const def = evaluate(args[1], spec.file);
    let range;
    switch (kind) {
        case 'defineInRange': {
            const lo = evaluate(args[2], spec.file), hi = evaluate(args[3], spec.file);
            range = { text: `${lo.text} to ${hi.text}`, html: `${lo.html} &ndash; ${hi.html}` };
            break;
        }
        case 'defineEnum': {
            if (!def.enumType) fail(`${key}: an enum default that is not Type.VALUE`);
            const values = enumConstants(def.enumType, spec.file);
            range = { text: values.join(', '), html: values.map(v => `<code>${v}</code>`).join(', ') };
            break;
        }
        case 'defineInList': {
            const allowed = evaluate(args[2], spec.file);
            if (!allowed.list) fail(`${key}: cannot read the allowed values ${args[2]}`);
            range = { text: allowed.list.join(', '), html: allowed.html };
            break;
        }
        case 'defineList':
        case 'defineListAllowEmpty':
            range = { text: 'a list', html: 'a list' };
            break;
        default:
            range = def.list
                ? { text: 'a list', html: 'a list' }
                : def.string
                    ? { text: 'text', html: 'text' }
                    : /^(true|false)$/.test(def.text) || /dev run/.test(def.text)
                        ? { text: 'true / false', html: '<code>true</code> / <code>false</code>' }
                        : { text: '', html: '' };
    }
    let defaultHtml = def.html;
    if (def.string) defaultHtml = def.text === '' ? '<code>""</code> (blank)' : `<code>"${esc(def.text)}"</code>`;
    if (def.list && !def.list.length) defaultHtml = '<code>[]</code>';
    return { key, side: spec.side, default: { text: def.text, html: defaultHtml }, range, lines };
}

// ---------------------------------------------------------------- writing HTML

function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/** A config comment as HTML: wrapped lines joined, indented and list lines kept apart. */
function commentHtml(lines) {
    // A file comment is hard-wrapped. An indented line is a list item ("  FULL - ...")
    // and a deeper-indented one wraps the item above it; an unindented line wraps the
    // paragraph above it, unless that was a list, which it then follows.
    const out = [];
    let prev = -1;
    for (const raw of lines) {
        if (!raw.trim()) { prev = -1; continue; }
        const indent = raw.match(/^\s*/)[0].length;
        const line = esc(raw.trim());
        const wraps = out.length && prev >= 0 && (indent === 0 ? prev === 0 : indent > prev + 1 && prev > 0);
        if (wraps) {
            out[out.length - 1] += ' ' + line;
        } else {
            out.push(indent > 0 ? `&nbsp;&nbsp;${line}` : line);
            prev = indent;
        }
    }
    return out.join('<br>');
}

function pageExists(href) {
    const [file, anchor] = href.split('#');
    const p = path.join(ROOT, 'wiki', file);
    if (!fs.existsSync(p)) return false;
    if (!anchor) return true;
    return fs.readFileSync(p, 'utf8').includes(`id="${anchor}"`);
}

/** Does the key's home page actually mention it? A key only this table names is flagged. */
function mentioned(key, href) {
    const t = fs.readFileSync(path.join(ROOT, 'wiki', href.split('#')[0]), 'utf8');
    // As written in the file, too: "[magical_herds] chance" is the same key.
    const dot = key.lastIndexOf('.');
    const section = key.slice(0, dot), leaf = key.slice(dot + 1);
    if (t.includes(key)) return true;
    if (key.includes('<')) return t.includes(leaf);
    return dot > 0 && t.includes(`[${section}]`) && t.includes(leaf);
}

function build() {
    const used = new Set();
    let html = `${BEGIN}\n`;
    let total = 0, onlyHere = 0;
    for (const spec of SPECS) {
        const { rows } = walk(spec);
        html += `<h3 id="keys-${spec.id}">${esc(spec.side)} &mdash; ${spec.where}</h3>\n`;
        html += `<p>${esc(spec.blurb)} ${rows.length === 1 ? 'One key' : `${rows.length} keys`}, from <code>${esc(path.posix.basename(spec.file))}</code>.</p>\n`;
        html += '<div class="table-wrap"><table class="config-keys">\n';
        html += '<thead><tr><th>Key</th><th>Default</th><th>Allowed</th><th>What it does</th></tr></thead>\n<tbody>\n';
        for (const r of rows) {
            const href = HOME[r.key];
            if (!href) fail(`${r.key} (${spec.file}) has no HOME page - add it to HOME in this script`);
            if (!pageExists(href)) fail(`${r.key}: HOME ${href} does not exist (page or #anchor)`);
            used.add(r.key);
            const only = !mentioned(r.key, href);
            if (only) onlyHere++;
            total++;
            html += `<tr id="key-${spec.id}-${r.key.replace(/[^\w-]+/g, '_')}">`
                + `<td><code>${esc(r.key)}</code></td>`
                + `<td>${r.default.html}</td>`
                + `<td>${r.range.html}</td>`
                + `<td>${commentHtml(r.lines)} <a class="config-home" href="${href}">${only ? 'Feature page (does not name this key yet)' : 'Feature page'}</a></td>`
                + '</tr>\n';
        }
        html += '</tbody></table></div>\n';
    }
    for (const k of Object.keys(HOME)) if (!used.has(k)) fail(`HOME names ${k}, which no config defines any more - delete it`);
    html += `<p class="config-keys-foot">${total} keys in all. ${onlyHere} of them are named on no feature page but this one.</p>\n`;
    html += END;
    return { html, total, onlyHere };
}

// ---------------------------------------------------------------- main

let built;
try {
    built = build();
} catch (e) {
    if (!(e instanceof BakeError)) throw e;
    console.error(`bake-config-keys: ${e.message}`);
    process.exit(1);
}
const { html, total, onlyHere } = built;
const pagePath = path.join(ROOT, PAGE);
const page = fs.readFileSync(pagePath, 'utf8');
const a = page.indexOf(BEGIN), b = page.indexOf(END);
if (a < 0 || b < 0) {
    console.error(`bake-config-keys: ${PAGE} has no generated span (${BEGIN} ... ${END})`);
    process.exit(1);
}
// In the page's own line endings: a checkout with core.autocrlf hands it back CRLF,
// and an LF span inside it would read as stale on every --check.
const nl = page.includes('\r\n') ? '\r\n' : '\n';
const next = page.slice(0, a) + html.replace(/\n/g, nl) + page.slice(b + END.length);
if (process.argv.includes('--check')) {
    if (next !== page) {
        console.error(`bake-config-keys: ${PAGE} is stale - run node wiki/tools/bake-config-keys.mjs`);
        process.exit(1);
    }
    console.log(`config keys OK: ${total} keys, ${onlyHere} named nowhere else`);
} else {
    if (next !== page) fs.writeFileSync(pagePath, next);
    console.log(`${next !== page ? 'wrote' : 'unchanged'} ${PAGE}: ${total} keys, ${onlyHere} named nowhere else`);
}
