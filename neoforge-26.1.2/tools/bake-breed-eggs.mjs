#!/usr/bin/env node
// Generate the per-breed breed spawn egg art wiring: one item model per breed
// that has a texture, and the select that picks between them.
//
// WHY THIS EXISTS
// The breed spawn egg is ONE registered item carrying a `horsegenetics:breed`
// component, because a breed a player drops into .minecraft/phc/breeds/ after
// the jar was built has no registry entry and must still get an egg. That is
// not negotiable - see item/BreedSpawnEggItem. So per-breed art cannot be one
// item per breed; it has to be a client-side select on the component VALUE.
//
// That select is one case per breed with art, and each case needs a model file
// whose only content is a texture id. Hand-writing forty-odd of those is a job
// nobody does correctly twice, and getting one texture id wrong fails silently
// as a purple chequerboard on the one breed nobody happens to look at. So the
// art on disk is the source of truth and the wiring is derived from it:
//
//   node neoforge-26.1.2/tools/bake-breed-eggs.mjs
//
// Drop a new <breed_id>.png into textures/item/breed_egg/, re-run, done.
//
// NOT EVERY BREED HAS ART, AND THAT IS THE STEADY STATE
// There are far more breeds than drawn eggs, and there always will be - the
// player-added ones can never have art in this jar. Breeds without a texture
// fall through to `fallback`, which is the vanilla-horse-egg model the item
// used for every breed before this existed. A mixed tab is the correct
// behaviour, not a half-finished state: the tooltip has always been what tells
// you the breed.
//
// IT VALIDATES RATHER THAN TRUSTS
// Every texture must name a real breed id. A file called `gypsey vanner.png`
// or `halfinger.png` (both of which arrived in the intake spelled that way)
// would otherwise bake a case that can never match, because no horse will ever
// carry that component value - a silent no-op, which is the worst kind. A
// texture that matches no breed is a hard error here.
//
// UNVERIFIED: the select-on-component schema below (`minecraft:select` with
// property `minecraft:component`, `cases[].when` matching a string component
// value, plus `fallback`) is taken from the Items model definition docs, where
// the component property arrived in 1.21.5 / 25w03a. It reads correctly for
// 26.1.2 and the item model definition format here matches the one existing
// `minecraft:condition` use (items/signed_transfer_paper.json), but it has not
// yet been seen rendering in game - check the creative tab shows drawn eggs and
// not chequerboards.

import { mkdirSync, writeFileSync, readdirSync, readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const NS = "horsegenetics";

const A = join(root, "src/main/resources/assets", NS);
const BREEDS = join(root, "../common/src/main/resources/horsegenetics/breeds");

/** Where the drawn eggs live, and the id prefix they are referenced by. */
const ART_DIR = join(A, "textures/item/breed_egg");
const ART_REF = `${NS}:item/breed_egg`;
const MODEL_DIR = join(A, "models/item/breed_egg");
const MODEL_REF = `${NS}:item/breed_egg`;

/** The model every breed used before there was art, and the one they fall back to. */
const FALLBACK_MODEL = `${NS}:item/breed_spawn_egg`;

// --- the breeds that actually exist -----------------------------------------

/** Every declared breed id, read off the breed sheets rather than remembered. */
function breedIds() {
    const ids = new Set();
    for (const file of readdirSync(BREEDS)) {
        if (!file.endsWith(".json") || file === "index.json") continue;
        const json = JSON.parse(readFileSync(join(BREEDS, file), "utf8"));
        if (json.id) ids.add(json.id);
    }
    return ids;
}

// --- the art on disk ---------------------------------------------------------

const ids = breedIds();
const art = readdirSync(ART_DIR)
    .filter((f) => f.endsWith(".png"))
    .map((f) => f.slice(0, -4))
    .sort();

const unknown = art.filter((id) => !ids.has(id));
if (unknown.length > 0) {
    console.error(
        `bake-breed-eggs: ${unknown.length} texture(s) name no breed that exists:\n` +
            unknown.map((id) => `  textures/item/breed_egg/${id}.png`).join("\n") +
            `\n\nA case on a value no horse can carry never matches and never draws.` +
            ` Rename the file to the breed's id (see common/.../horsegenetics/breeds/).`,
    );
    process.exit(1);
}
if (art.length === 0) {
    console.error("bake-breed-eggs: no textures in textures/item/breed_egg - nothing to bake.");
    process.exit(1);
}

// --- one model per drawn breed ----------------------------------------------

mkdirSync(MODEL_DIR, { recursive: true });
for (const id of art) {
    writeFileSync(
        join(MODEL_DIR, `${id}.json`),
        JSON.stringify({ parent: "minecraft:item/generated", textures: { layer0: `${ART_REF}/${id}` } }, null, 2) + "\n",
    );
}

// --- the select that picks between them -------------------------------------

const definition = {
    model: {
        type: "minecraft:select",
        property: "minecraft:component",
        component: `${NS}:breed`,
        cases: art.map((id) => ({
            when: id,
            model: { type: "minecraft:model", model: `${MODEL_REF}/${id}` },
        })),
        fallback: { type: "minecraft:model", model: FALLBACK_MODEL },
    },
};
writeFileSync(join(A, "items/breed_spawn_egg.json"), JSON.stringify(definition, null, 2) + "\n");

console.log(
    `bake-breed-eggs: ${art.length} drawn egg${art.length === 1 ? "" : "s"}, ` +
        `${ids.size - art.length} breed(s) on the vanilla fallback.`,
);
