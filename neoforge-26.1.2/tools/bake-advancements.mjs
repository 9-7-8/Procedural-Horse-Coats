// Every ProgressTask is also a real advancement. This writes them.
//
//   node neoforge-26.1.2/tools/bake-advancements.mjs
//
// The checklist in the Getting Started book and the advancement tree are the
// same hundred things, and they must not be two lists: the enum in common/ is
// the source, this is the bake, and the tree is derived. Change a task's title
// or hint and re-run this - nothing at runtime reads the enum to build an
// advancement, because advancements are data and have to exist before a world
// loads.
//
// WHAT IT WRITES, into neoforge-26.1.2/src/main/resources/data/horsegenetics/advancement/
//   root.json          the tab - completed by any task at all
//   <chapter>.json     one per ProgressTask.Group - completed by anything in it
//   <chapter>/<task>.json   one per task
// The whole directory is rewritten every run: a task deleted from the enum has
// its advancement deleted too, which is the point. Nothing else may live there.
//
// TEXT IS LITERAL, NOT A TRANSLATION KEY. The titles and hints are English
// inside a Java enum in common/, so a lang key would be a second copy of them
// maintained by hand - exactly what this bake exists to prevent. The mod is
// English-only throughout for the same reason.
//
// The description is the FIRST SENTENCE of the task's hint. The hints are
// written to be read in the book with the whole paragraph around them; an
// advancement toast has one line. The first sentence is always the "what to do"
// one - that is a rule the enum's own javadoc states, so this is not a guess.
//
// ICONS are the one thing here that is not derived, because no line of the enum
// knows about items. They are hand-assigned below and validated against the
// real item lists - the mod's own from its item definition files, vanilla's
// from the patched jar when a build has produced one. An icon naming an item
// that does not exist does not crash: the advancement silently fails to load
// and the player is missing one box with nothing in the log to say why.

import { readFileSync, readdirSync, writeFileSync, rmSync, mkdirSync, existsSync,
         openSync, readSync, closeSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, "../..");
const enumFile = join(repo, "common/src/main/java/com/example/horsegenetics/common/progress/ProgressTask.java");
const outDir = join(repo, "neoforge-26.1.2/src/main/resources/data/horsegenetics/advancement");
const itemDefs = join(repo, "neoforge-26.1.2/src/main/resources/assets/horsegenetics/items");
const patchedJar = join(repo, "neoforge-26.1.2/build/moddev/artifacts/minecraft-patched-26.1.2.100.jar");

// ---------------------------------------------------------------- the enum

const src = readFileSync(enumFile, "utf8");
const body = src.slice(src.indexOf("public enum ProgressTask {"), src.indexOf("/** The headings"));

// Entries start at exactly four spaces of indent and are the only thing in the
// file shaped NAME(Group.X - the same anchor check-progress-tasks.mjs uses.
const starts = [...body.matchAll(/^\s{4}([A-Z][A-Z0-9_]*)\(Group\.([A-Z_]+),/gm)];
if (starts.length === 0) {
  console.error("bake-advancements: found no tasks - has the enum been reshaped?");
  process.exit(1);
}

const tasks = starts.map((m, n) => {
  const chunk = body.slice(m.index, n + 1 < starts.length ? starts[n + 1].index : body.length);
  // Every string literal in the entry, in order: the title first, then the
  // hint in however many concatenated pieces it was wrapped into.
  const strings = [...chunk.matchAll(/"((?:[^"\\]|\\.)*)"/g)].map((s) => s[1]);
  if (strings.length < 2) {
    console.error(`bake-advancements: ${m[1]} has no title and hint - cannot bake it`);
    process.exit(1);
  }
  return {
    name: m[1],
    id: m[1].toLowerCase(),
    chapter: m[2].toLowerCase(),
    title: strings[0],
    hint: strings.slice(1).join(""),
  };
});

// The chapters, in the enum's own order, with their English titles read off
// Group so a renamed chapter cannot drift from the book.
const groupBody = src.slice(src.indexOf("public enum Group {"));
const chapters = [...groupBody.matchAll(/^\s{8}([A-Z_]+)\("([^"]+)"\)/gm)].map((m) => ({
  id: m[1].toLowerCase(),
  title: m[2],
}));
if (chapters.length === 0) {
  console.error("bake-advancements: found no Group entries");
  process.exit(1);
}

// -------------------------------------------------------------- the writing

/** What a chapter node's card says under its title. One line each, by hand. */
const CHAPTER_BLURB = {
  wild: "What horses get up to when nobody owns them.",
  husbandry: "Keeping a horse of your own fed, sound and willing.",
  farming: "Hay, carts, and the work a horse is actually for.",
  basic_breeding: "Getting a third horse out of two.",
  genetics: "Reading what a horse carries, and choosing what it passes on.",
  magic: "The horses no real stable ever held.",
  projects: "The outcomes you have to set out to breed.",
  people: "The people who deal in horses.",
};

const CHAPTER_ICON = {
  wild: "minecraft:grass_block",
  husbandry: "minecraft:saddle",
  farming: "minecraft:wheat",
  basic_breeding: "minecraft:golden_carrot",
  genetics: "horsegenetics:research_paper",
  magic: "minecraft:amethyst_shard",
  projects: "minecraft:experience_bottle",
  people: "minecraft:emerald",
};

/** Rare, or a long way in. A purple frame, and vanilla's challenge fanfare. */
const CHALLENGE = new Set([
  "WILD_TAKEOVER_FIGHT", "STEER_BAREBACK", "BOND_FOLLOWS", "USE_INTERDIMENSIONAL_TICKET",
  "TWINS_FRATERNAL", "TWINS_IDENTICAL", "FOAL_HOM_RECESSIVE",
  "BREED_MUTATION", "TAME_DHAMPIR", "MEET_MAGICAL_HERD",
]);

/** Not rare, but the moment something opens up. */
const GOAL = new Set([
  "FORM_HERD", "HEAL_TO_FULL", "WHISTLE_ENDER", "BREED_FOAL", "DISCOVER_GENE",
  "LIGHT_PORTAL", "ENTER_DIMENSION", "MAGICAL_GENE_DISCOVERED", "BUY_ARCANE_HORSE", "MEET_COWBOY",
]);

const ICON = {
  // 1. Horses in the wild
  DISCOVER_BREED: "minecraft:written_book",
  BREED_EGG: "horsegenetics:breed_spawn_egg",
  WILD_FOOD_DRIFT: "minecraft:wheat",
  WILD_FAVOURITE_RUN: "minecraft:apple",
  CROUCH_FEED_TAME: "minecraft:carrot",
  TAME_MARE: "minecraft:pink_dye",
  TAME_STALLION: "minecraft:light_blue_dye",
  WILD_KICK: "minecraft:iron_boots",
  WILD_BOLT: "minecraft:feather",
  WILD_READ_SOCIAL: "minecraft:book",
  WILD_SPAR: "minecraft:shield",
  WILD_DAM_FOAL: "minecraft:egg",
  WILD_BAND_STALLION: "minecraft:iron_horse_armor",
  WILD_TAKEOVER_FIGHT: "minecraft:iron_sword",
  WILD_DISPLACE: "minecraft:bucket",
  WILD_GROOM: "minecraft:brush",
  WILD_GRAZE: "minecraft:short_grass",
  WILD_HUNT: "minecraft:beef",
  FORM_HERD: "minecraft:lead",

  // 2. Husbandry
  NAME_HORSE: "minecraft:name_tag",
  BARN_NAME: "minecraft:paper",
  FEED_BY_HAND: "minecraft:sugar",
  FAVOURITE_FOOD: "minecraft:golden_apple",
  HORSE_INJURED: "minecraft:bone",
  FEED_HUNGRY_HORSE: "minecraft:bread",
  HEAL_AT_WATER: "minecraft:water_bucket",
  HEAL_TO_FULL: "minecraft:glistering_melon_slice",
  SHEAR_HORSE: "minecraft:shears",
  BOND_HORSE: "minecraft:leather",
  BOND_ATTENTIVE: "minecraft:copper_ingot",
  BOND_APPROACHES: "minecraft:iron_ingot",
  BOND_FOLLOWS: "minecraft:gold_ingot",
  RIDE_BAREBACK: "minecraft:leather_boots",
  STEER_BAREBACK: "minecraft:diamond",
  BUILD_STALL: "horsegenetics:stall_sign",
  STALL_DOUBLE_GATE: "horsegenetics:oak_double_fence_gate",
  HANG_PEN_SIGN: "horsegenetics:holding_pen_sign",
  CRAFT_BLANK_TICKET: "horsegenetics:blank_ticket",
  USE_TICKET: "horsegenetics:basic_ticket",
  USE_BOUND_TICKET: "horsegenetics:bound_ticket",
  USE_INTERDIMENSIONAL_TICKET: "horsegenetics:interdimensional_ticket",
  USE_PEN_TICKET: "horsegenetics:holding_pen_ticket",
  WHISTLE_BASIC: "horsegenetics:basic_whistle",
  WHISTLE_GOLDEN: "horsegenetics:golden_whistle",
  WHISTLE_ECHO: "horsegenetics:echo_whistle",
  WHISTLE_ENDER: "horsegenetics:ender_whistle",
  DYE_TACK: "minecraft:leather_horse_armor",
  MILK_MARE: "minecraft:milk_bucket",

  // 3. Farming and haulage
  PLACE_CART: "horsegenetics:cart_wheel",
  HITCH_CART: "minecraft:lead",
  DRIVE_WAGON: "horsegenetics:oak_wagon",
  DRIVE_PLOW: "horsegenetics:oak_plow",
  DRIVE_REAPER: "horsegenetics:oak_reaper",
  DRIVE_SEED_DRILL: "horsegenetics:oak_seed_drill",
  DRIVE_SUPPLY_CART: "horsegenetics:oak_supply_cart",
  DRIVE_ANIMAL_CART: "horsegenetics:oak_animal_cart",
  BALE_HAY: "minecraft:hay_block",
  PLANT_GOLDEN_CARROT: "horsegenetics:golden_carrot_seeds",
  HARVEST_GOLDEN_CARROT: "minecraft:golden_carrot",

  // 4. Basic breeding
  NATURAL_COVER: "minecraft:poppy",
  VET_KIT_USE: "horsegenetics:vet_kit",
  BREED_FOAL: "horsegenetics:custom_horse_spawn_egg",
  FILL_SEED_JAR: "horsegenetics:empty_seed_jar",
  USE_SEED_JAR: "horsegenetics:stallion_seed_jar",
  GELD_HORSE: "minecraft:shears",
  OWN_GELDING: "minecraft:gray_dye",
  BOND_GELDING: "minecraft:iron_ingot",

  // 5. Genetics
  GENE_BOOK: "minecraft:book",
  DISCOVER_GENE: "minecraft:spyglass",
  BUILD_SHELF: "horsegenetics:equine_research_shelf",
  FILE_PAPER: "minecraft:bookshelf",
  COPY_PAPER: "minecraft:writable_book",
  GENE_CARROT: "horsegenetics:known_gene_splice_carrot",
  USE_GENE_CARROT: "minecraft:carrot_on_a_stick",
  CARROT_STABILIZER: "horsegenetics:stabilizer_carrot",
  CARROT_MAGNIFIER: "horsegenetics:magnifier_carrot",
  CARROT_UNKNOWN_GENE: "horsegenetics:unknown_gene_splice_carrot",
  CARROT_UNKNOWN_EPIGENETIC: "horsegenetics:unknown_epigenetic_splice_carrot",
  CARROT_DILUTION: "horsegenetics:dilution_gene_splice_carrot",
  CARROT_WHITE: "horsegenetics:white_gene_splice_carrot",
  CARROT_MARKING: "horsegenetics:marking_gene_splice_carrot",
  CARROT_PERFORMANCE: "horsegenetics:performance_gene_splice_carrot",
  LIGHT_PORTAL: "minecraft:hay_block",
  ENTER_DIMENSION: "minecraft:ender_pearl",
  BRING_HORSE_HOME: "minecraft:ender_eye",

  // 6. Magical horses
  MAGICAL_GENE_DISCOVERED: "minecraft:glow_ink_sac",
  BREED_MUTATION: "minecraft:nether_star",
  CARROT_MAGICAL: "horsegenetics:magical_gene_splice_carrot",
  MEET_DHAMPIR: "minecraft:spider_eye",
  TAME_DHAMPIR: "minecraft:fermented_spider_eye",
  MEET_MAGICAL_HERD: "minecraft:glowstone_dust",
  TAME_MAGICAL_HORSE: "minecraft:enchanted_book",
  BUY_ARCANE_HORSE: "minecraft:emerald_block",
  SUN_SENSITIVE_SEEN: "minecraft:fire_charge",
  BLOOD_BITE_SEEN: "minecraft:redstone",

  // 7. Breeding projects
  TWINS_FRATERNAL: "minecraft:egg",
  TWINS_IDENTICAL: "minecraft:diamond",
  FOAL_HETEROZYGOUS: "minecraft:quartz",
  FOAL_HOM_DOMINANT: "minecraft:quartz_block",
  FOAL_HOM_RECESSIVE: "minecraft:lapis_lazuli",

  // 8. The villagers
  TRADE_EQUESTRIAN: "horsegenetics:suppliers_post",
  MEET_COWBOY: "horsegenetics:cowboy_hitch",
  TRANSFER_PAPER: "horsegenetics:signed_transfer_paper",
};

const ROOT_ICON = "minecraft:saddle";
const ROOT_TITLE = "Horse Genetics";
const ROOT_BLURB = "Everything there is to find out about horses.";
// Vanilla's own tiling background, reused rather than drawn: it is the hay-bale
// one behind the husbandry tab, which is the right room for this.
const ROOT_BACKGROUND = "minecraft:gui/advancements/backgrounds/husbandry";

/** The first sentence of a hint, which is the one that says what to do. */
function firstSentence(hint) {
  const end = hint.search(/\.(\s|$)/);
  return end < 0 ? hint : hint.slice(0, end + 1);
}

function display(icon, title, description, extra = {}) {
  return { icon: { id: icon }, title: { text: title }, description: { text: description }, ...extra };
}

function criterion(conditions) {
  const c = { trigger: "horsegenetics:progress_task" };
  if (conditions && Object.keys(conditions).length) {
    c.conditions = conditions;
  }
  return c;
}

// ------------------------------------------------------------- icons exist?

const modItems = new Set(
  readdirSync(itemDefs).filter((f) => f.endsWith(".json")).map((f) => "horsegenetics:" + f.slice(0, -5)),
);
/**
 * The names of everything in a zip, read out of its central directory.
 *
 * <p>By hand rather than by shelling out to `jar` or `unzip`, neither of which
 * is on a Windows PATH by default - and the first go at this did shell out,
 * and died on this machine. It reads the directory only, not the 200MB of
 * entries behind it.
 */
const EOCD_SIGNATURE = Buffer.from([0x50, 0x4b, 0x05, 0x06]);
const ZIP64_LOCATOR_SIGNATURE = Buffer.from([0x50, 0x4b, 0x06, 0x07]);

function zipEntryNames(file) {
  const fd = openSync(file, "r");
  try {
    const size = statSync(file).size;
    const tailLen = Math.min(size, 66560); // 64K comment + the record itself
    const tail = Buffer.alloc(tailLen);
    readSync(fd, tail, 0, tailLen, size - tailLen);
    const eocd = tail.lastIndexOf(EOCD_SIGNATURE);
    if (eocd < 0) throw new Error("no end-of-central-directory record");

    let cdSize = tail.readUInt32LE(eocd + 12);
    let cdOffset = tail.readUInt32LE(eocd + 16);
    if (cdOffset === 0xffffffff || cdSize === 0xffffffff || tail.readUInt16LE(eocd + 10) === 0xffff) {
      // Zip64: the real numbers are in a record the locator points at.
      const loc = tail.lastIndexOf(ZIP64_LOCATOR_SIGNATURE);
      if (loc < 0) throw new Error("zip64 sizes but no locator");
      const z64At = Number(tail.readBigUInt64LE(loc + 8));
      const z64 = Buffer.alloc(56);
      readSync(fd, z64, 0, 56, z64At);
      cdSize = Number(z64.readBigUInt64LE(40));
      cdOffset = Number(z64.readBigUInt64LE(48));
    }

    const cd = Buffer.alloc(cdSize);
    readSync(fd, cd, 0, cdSize, cdOffset);
    const names = [];
    let at = 0;
    while (at + 46 <= cd.length && cd.readUInt32LE(at) === 0x02014b50) {
      const nameLen = cd.readUInt16LE(at + 28);
      const extraLen = cd.readUInt16LE(at + 30);
      const commentLen = cd.readUInt16LE(at + 32);
      names.push(cd.toString("utf8", at + 46, at + 46 + nameLen));
      at += 46 + nameLen + extraLen + commentLen;
    }
    return names;
  } finally {
    closeSync(fd);
  }
}

let vanillaItems = null;
if (existsSync(patchedJar)) {
  // The item definition files under assets/minecraft/items/ are one per item,
  // so their names ARE the item registry.
  vanillaItems = new Set(
    zipEntryNames(patchedJar)
      .filter((l) => l.startsWith("assets/minecraft/items/") && l.endsWith(".json"))
      .map((l) => "minecraft:" + l.slice("assets/minecraft/items/".length, -5)),
  );
}

const badIcons = [];
function checkIcon(id, usedBy) {
  if (id.startsWith("horsegenetics:")) {
    if (!modItems.has(id)) badIcons.push(`${usedBy}: ${id} is not one of this mod's items`);
  } else if (vanillaItems && !vanillaItems.has(id)) {
    badIcons.push(`${usedBy}: ${id} is not a vanilla item`);
  }
}

// -------------------------------------------------------------------- write

rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

const written = [];
function write(path, json) {
  const file = join(outDir, path + ".json");
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(json, null, 2) + "\n");
  written.push(path);
}

checkIcon(ROOT_ICON, "root");
write("root", {
  criteria: { any_task: criterion({}) },
  display: display(ROOT_ICON, ROOT_TITLE, ROOT_BLURB, {
    background: ROOT_BACKGROUND,
    show_toast: false,
    announce_to_chat: false,
  }),
});

for (const chapter of chapters) {
  const icon = CHAPTER_ICON[chapter.id];
  const blurb = CHAPTER_BLURB[chapter.id];
  if (!icon || !blurb) {
    console.error(`bake-advancements: chapter ${chapter.id} has no icon or blurb - add one above`);
    process.exit(1);
  }
  checkIcon(icon, chapter.id);
  write(chapter.id, {
    parent: "horsegenetics:root",
    criteria: { any_in_chapter: criterion({ chapter: chapter.id }) },
    display: display(icon, chapter.title, blurb, { frame: "goal" }),
  });
}

const known = new Set(chapters.map((c) => c.id));
for (const task of tasks) {
  if (!known.has(task.chapter)) {
    console.error(`bake-advancements: ${task.name} is in group ${task.chapter}, which Group does not have`);
    process.exit(1);
  }
  const icon = ICON[task.name];
  if (!icon) {
    console.error(`bake-advancements: ${task.name} has no icon - add one to ICON in this file`);
    process.exit(1);
  }
  checkIcon(icon, task.name);
  const frame = CHALLENGE.has(task.name) ? "challenge" : GOAL.has(task.name) ? "goal" : "task";
  write(`${task.chapter}/${task.id}`, {
    parent: `horsegenetics:${task.chapter}`,
    criteria: { done: criterion({ task: task.id }) },
    display: display(icon, task.title, firstSentence(task.hint), frame === "task" ? {} : { frame }),
  });
}

if (badIcons.length) {
  console.error("bake-advancements: icons naming items that do not exist:\n  " + badIcons.join("\n  "));
  console.error("\nNothing crashes on one of these - the advancement just never loads. Fix them.");
  process.exit(1);
}

console.log(
  `advancements baked: ${written.length} files - 1 root, ${chapters.length} chapters, ${tasks.length} tasks` +
    (vanillaItems ? "" : "\n  (vanilla icons unchecked - no patched jar built yet)"),
);
