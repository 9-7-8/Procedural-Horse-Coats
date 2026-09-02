# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses: a Mendelian genotype of **allele objects** (extension,
agouti + seal, white, champagne, a `T` "Test" diagnostic) drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, rolled speed/health
stats, and an **epigenome** - a priority + epigenetic seed on *each allele
copy*, inherited with the allele - and there's a self-contained "horse
dimension" reached by a hay-bale portal. Long-term aim is a 1.12.2 backport, which is
why the logic is quarantined in a game-free module.

**Docs split:** the only two markdown files in the repo are `README.md` and
`CLAUDE.md`. **Everything else is the wiki** - `index.html` (the hub) +
`wiki/*.html`. The old `Docs/*.md` files were converted into wiki pages and
deleted; there is no markdown dev doc any more, and new dev docs go in `wiki/`
as HTML.

The wiki is now a **source of truth for the mod's behaviour**, not a side
project. Its shape:
- `wiki/styles.css` + `wiki/nav.js` are shared by every page. **The sidebar is
  built from the `SECTIONS` array in `nav.js`** - add a page there once and it
  appears on every page. No per-page nav markup.
- Each page is a standalone file: prism theme, then `styles.css`, then
  `<main class="content"><article class="doc">`. Copy an existing gene page as
  a template.
- `wiki/gene-creator.html` is the owner's interactive 3D gene editor - a
  separate workstream, untouched by the doc pass, and not a source of truth.
- **`README.md`** is **user-facing only** now - what the mod does, how to play
  it, install, license. No status tables, no architecture, no API notes.
  Don't put dev content there.
- **`CLAUDE.md`** (this file) is the dev/working notes: status, the 26.1.2 API
  differences, gotchas, next steps.
- **`wiki/breeding.html`** is the single source of truth for the breeding / horse-
  record / pedigree / **stat-inheritance** system. Keep it current when you
  touch any of that; don't re-document it here or in README (a pointer is
  fine).
- **`wiki/gene-*.html`** is the single source of truth for **each gene** - alleles,
  generation function, wild frequency, dominance, natural/magical. Update
  it in the same change as any gene; CLAUDE.md keeps only the machinery + a
  one-line-per-gene table.
- **`wiki/verification.html`** is the rolling **`runClient` checklist** - what's
  built but not yet confirmed in-game. Update after every play session.
- **`wiki/philosophy.html`** is the **why** - Mendelian breeding as a game of
  skill, procgen coats for functionally infinite outcomes, "interesting" being
  the player's word, and the deliberately-acknowledged abstractions (no
  crossing over, no aging; **X-linked genes are the one exception**). It also
  owns the **determinism contract** - the same code always makes the same
  horse, what that forbids, and the founder-only randomness rule - because
  that's a standing rule, not a task. It ends with a priority order for when
  two goals conflict. **Read it before making a design call**; most arguments
  in the other docs are downstream of it. Keep it short and principled - no
  implementation detail, no status, no task lists.
- **`wiki/roadmap.html`** is the **long-range backlog** - the full gene
  wishlist and the systems it needs (the three-phase pigment pipeline, the
  determinism contract, hard-coded gene priority, the modder-facing
  gene-authoring API, non-coat and health genes), each with notes on what would
  have to change - plus the non-gene features (mare milking, healing gated on
  nearby water + food, a creative-only custom horse spawner) and the **planned
  revert of the genotype gallery** to random pens. It is **work items only**;
  the reasoning behind them lives in `wiki/philosophy.html`. Nothing in it is
  implemented; when something ships it moves to its own `wiki/gene-*.html` page
  (or `wiki/breeding.html`) and is deleted there. Its "Decisions still open" section keeps
  a short list of what's already **settled** - aging out of scope, health as
  fewer hearts, the magical RGB phase being signed unclamped `int`s capped only
  at conversion, alphanumeric allele tokens with `n` as wild type - so a later
  session doesn't reopen them.
- **New with the wiki conversion**, and not derived from any old markdown -
  keep them current too:
  - **`wiki/genetics-model.html`** - the Mendelian model as implemented:
    alleles as objects, the code string, `DominancePattern`, the per-allele
    epigenome, `GenotypeCatalog`, and what the texture key captures.
  - **`wiki/pipeline.html`** - the three-phase coat pipeline in full
    (`PigmentField` / `ColorField` / `GradientLut` / `CoatTextureId` / the
    golden test). This is where the coat machinery is documented now; CLAUDE.md
    keeps a summary.
  - **`wiki/body-space.html`** - `HorseSkinGeometry`, `CoatRegions`,
    `BodyNoise`, `BodyStripes`.
  - **`wiki/modding.html`** + **`wiki/api-reference.html`** - the
    **modder-facing** docs: how to write a gene (two worked walkthroughs, the
    allele rules, the determinism contract, the pitfalls table) and the
    class-by-class abstraction reference. When a public type in `common/`
    changes shape, update `api-reference.html` in the same change.

## Status snapshot (keep this current)

- **`common/`** - compiles; **153 JUnit tests pass** (`./gradlew :common:test`).
  Covers `genetics/` (allele/gene model - **11 genes**: the 9 natural ones incl.
  grey / cream / pearl / splash, plus magic zebra and pink hair; `Genotype` code
  round-trip, breeding, the `Epigenome` / `Genome` per-allele epigenetics +
  priority tie-break, `DominancePattern` + the `GenotypeCatalog` reduction of
  177 147 genotypes to 1 730 distinct coats), `coat/` + `coat/pattern/` (the
  three-phase pipeline - `CoatTextureComposer`, `PigmentField`, `ColorField`,
  `GradientLut`, `BayCoat`, `GreyCoat`, `BodyStripes`, `CoatRegions`, the pure
  gene hooks, the `coat-golden.txt` byte-identity net, `CoatTextureId`
  texture-id injectivity),
  `coat/skin/` (`HorseSkinGeometry`), `name/` (`breedNth`),
  `horse/` (pedigree + `HorseStats` -> `wiki/breeding.html`).
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes; only two `getGuiLeft/getGuiTop`
  deprecation warnings) against the real NeoForge `26.1.2.100` SDK.
- **`runServer`** - boots clean to `Done (...)! For help`; all dimensions,
  attachments, SavedData, payloads, the `hay_portal` block + block entity, and
  the `ClientConfig` all register with no errors.
- **`runClient`** - actively play-tested over the 2026-08-30 and 2026-09-01
  sessions; see below.

- **Owner-verified in-game (2026-09-01):**
  - **The coat pipeline end to end.** Wild spawns show a **wide variety** of
    genotypes and every one renders its correct coat - **no more flat-white
    horses**. That was the `CoatTextureId` fix (see the coat-pipeline section):
    the old `sanitize()` lower-cased the texture key, folding all 19 683
    genotypes onto 27 `Identifier`s.
  - **Agouti / bay**: renders correctly, and **seal is properly gone as a gene**
    - a high roll of bay's epigenetics gives the seal look. (Verified against
    the *old* two-number roll; the generator was rewidened afterwards - see the
    coat-pipeline section - so the spread of leg heights is unverified, the
    mechanism isn't.)
  - **Splash**: renders correctly (leg white + centreline blaze).
  - **Eyes**: survive the coat on every horse seen, adult and foal.
  - **`FamilyTreeScreen`**: correct in full - nodes, coats, layout.
  - **Horse dimension**: correct in full, including the sunk pen amenities
    (water cauldron + hay bale flush with the grass) keeping horses penned.
  - **"Spawn Test Horse World"** title-screen button (dev only) works.
  - **Clock on a tamed foal** ages it to adult **without** also seating the
    player on it.
  - **Roped-horse portal shortcut**: right-clicking a `hay_portal` while
    leading a horse sends the horse through **and** drops the lead.
  - **Diluted bay points** (fixed and re-verified the same day): a bay carrying
    champagne / cream / pearl now shows real diluted points - amber champagne
    chocolate over gold, buckskin dark brown over gold, perlino rusty, pearl
    bay sepia - instead of the jet black they all rendered before. See the
    coat-pipeline section for the cause (`PigmentField.dilute`).
  - Three rendering issues found in the same session are logged in
    **`wiki/verification.html`** - see "Known gaps" below. The bay/dilution one
    closed the same day; **grey** closed later (the `GreyCoat` rework, built but
    not yet play-tested); the two **splash** ones are still open.

- **Owner-verified in-game (2026-08-30):**
  - **Hay-bale portal**: golden-carrot lighting; the animated `hay_portal.png`
    texture renders, **faces the player** (thin half-block slab), is opaque (no
    sky/clouds/water through it), and its fps **ramps 12 -> 48** as you dwell;
    **gold** dwell-swirl particles + the chat countdown; the End-portal
    starfield effect (before it was replaced by the custom texture); **return
    teleport** drops your tamed horses in the air beside the overworld portal,
    unharmed (10 s invuln), not sucked back.
  - **Horse dimension**: the reworked **E/W pen-back walls** (gravel strip
    flush to the pen, glowstone above, one wood wall, bedrock); the layered
    **wall behind the portal** (gravel floor strip + floating glowstone + wood
    wall + bedrock, no dark band / missing blocks) with the E/W walls carried
    to `originX-1` so the corner has no gap; **no block break / place** anywhere
    in the dimension (survival + creative).
  - **Info panel** (mounted horse GUI): readable shadow-free text, the "View
    Family Tree" / Set buttons + barn box visible and clickable, and **E no
    longer closes the screen while the barn box is focused** (Enter submits,
    Esc closes).
  - **`FamilyTreeScreen`**: per-node **3D horse models** in the right coat/pose
    that **turn to face the cursor**; the chart **shrinks to fit** the window
    (`boxW`/`boxH`/`uiScale` from `rebuildNodes`, full names via `drawFitted`'s
    pose `scale`) - no column overlap, nothing cut off, names legible, no
    scroll bar by default.
  - **Breeding**: a foal rolls speed/health between its parents, a correctly
    combined genetic code, and (for the pairing's **first** foal) a name
    combining both parents.

- **Built 2026-09-02, NOT yet play-tested:** the first two **magical genes** -
  **magic zebra** (`Mzeb`, dominant, 1/100 per allele) and **pink hair**
  (`Pihr`, **recessive**, 1/12 per allele). Both are phase-3 genes; details in
  `wiki/gene-*.html`, in-game checklist in `wiki/verification.html`. They take
  the registry to **11 genes**, which moves a lot of derived numbers: the code
  string is 11 segments, `breedWith` draws 22 booleans, and the gallery goes
  from 434 pens / 1 519 blocks to **1 730 pens / 6 055 blocks** of corridor.
  Sample bakes look right (stripes read black over cremello and over dominant
  white; pink manes keep their strand shading on black, chestnut and perlino
  alike); nothing seen in-game.
- **Built 2026-09-02, behaviour-neutral by construction:** the **three-phase
  pigment pipeline** (`wiki/roadmap.html` §1). Phase 3 is now a signed,
  uncapped `ColorField` that magical genes *add* into, both gene hooks are pure
  (read-only views in, a contribution out), and `CoatBuildContext` no longer
  carries scratch space. **No coat changed**: `CoatPipelineGoldenTest` hashes
  20 genotypes × 3 seeds × adult/foal and every one is byte-identical to the
  pre-refactor bake, so there is **nothing new to play-test** - it's groundwork
  for the magical genes. Nothing in `neoforge-26.1.2/` needed touching, which
  was the test of whether the refactor stayed inside `common/`.
- **Built 2026-09-01, NOT yet play-tested:** **per-allele epigenetics** and the
  **dapple-grey rework**. Epigenetics moved off the horse and onto the allele
  copy (`Epigenome` / `Genome` / `AlleleEpigenetics`, each copy carrying a
  `priority` + `epigeneticSeed`, inherited unchanged by a foal); `GreyGene` now
  renders a real dapple grey through the new `GreyCoat` + `BodyNoise`; and
  bay's leg black is a uniform per-horse extent with per-leg jitter instead of
  the old low-biased single number. Compiles, 138 `common` tests pass, sample
  bakes look right, nothing seen in-game yet - checklist in
  **`wiki/verification.html`**.
- **Built 2026-09-01, NOT yet play-tested:** the **genotype gallery** rework of
  the horse dimension - one pen per visually distinct genotype (1 730 of 177 147),
  per-pen genotype signs, the entrance tally sign, `Gene.dominance()` metadata,
  and the entity-only teardown that leaves blocks standing. Compiles, 138
  `common` tests pass, nothing seen in-game yet. Details in the horse-dimension
  section below; the in-game checklist is the top item in
  **`wiki/verification.html`**.
- **Built 2026-09-01, NOT yet play-tested:** the **dev test-world auto-delete**
  - `client/DebugTestWorldCleanup` wipes every `test_horse_*` save on client
  shutdown (and sweeps leftovers on the next start), so the button stops
  filling `run/saves`. See "Running the game".
- **Open issues + NOT verified in-game:** see **`wiki/verification.html`**.
  Open issues are grey, and splash (face markings, leg edges, and it not
  reading its own dose); after the gallery, the top unverified item is
  **foals** (only spot-checked). Update it after each `runClient`.
- **Machine caveat (this dev laptop):** hybrid graphics (NVIDIA RTX 3050 Ti +
  AMD integrated). `java.exe`/`javaw.exe` are pinned to the NVIDIA GPU and the
  FML splash is disabled, or the JVM hard-crashes in the AMD GL driver. See
  "Running the game".

## Architecture - read this before editing anything

Two-module Gradle project, split deliberately:

- **`common/`** - pure Java, **zero** Minecraft/NeoForge imports (not even DFU
  `Codec`s). Subpackages by concern:
  - `genetics/` - `Genotype` (+ `breedWith`), `Epigenome` /
    `AlleleEpigenetics` (the priority + seed on each allele copy), `Genome`
    (the two together, and the breeding that keeps them aligned),
    `CoatPhenotype`, `GeneticCodeCombiner`.
  - `coat/` - `CoatData`, `CoatGenerator`; `coat/pattern/` holds the
    three-phase pipeline (`CoatTextureComposer`, the `PigmentField` /
    `ColorField` accumulators and their read-only `PigmentView` / `ColorView`
    faces) and `BodyNoise`, the reusable body-space noise any future patterned
    gene should build on.
  - `name/` - `HorseNameGenerator` + `HorseNames` (`breed` = one-half-each;
    `breedNth` = varied by a pairing's foal count) + word tables under
    `src/main/resources/horsegenetics/names/`.
  - `horse/` - the pedigree domain model (`Sex`, `HorseRecord`,
    `HorseDatabase`, `InMemoryHorseDatabase`) and `HorseStats` (foal stat
    roll) -> `wiki/breeding.html`.
  - `Rng` - the randomness seam (`nextFloat` / `nextBoolean` /
    `nextInt(bound)` / `nextLong`), implemented by `NeoRng` (wraps
    `RandomSource`) and, in tests, `FakeRng`.

  This is the part that survives a version port unchanged. If you want to
  import anything Minecraft-related here, stop.
- **`neoforge-26.1.2/`** - everything Minecraft-specific, by concern:
  - `client/` - renderer, texture compositing, client caches, the inventory
    hooks + `FamilyTreeScreen`, keybind, lifecycle cleanup.
  - `data/` - Data Attachments, the ancestry `SavedData`, codecs.
  - `network/` - custom payloads + `ModNetworking`.
  - `server/` - event handlers, the horse-dimension builder, the portal
    manager, the record adapter (`HorseRecords`).
  - `block/` - `ModBlocks` + `HayPortalBlock` (the only registered block),
    `ModBlockEntities` + `HayPortalBlockEntity` (drives the animated
    `hay_portal.png` slab renderer).

  Its job is to **translate** - build/read `common` types and shuttle them in
  and out of Minecraft's systems; the logic stays in `common/`.

When adding a feature, put as much as possible in `common/` and keep the
NeoForge module thin. That's what makes a future `forge-1.12.2/` module cheap.

## Build & test

```bash
./gradlew :common:test               # pure-Java logic, no Minecraft - fastest loop
./gradlew :neoforge-26.1.2:build     # full compile + jar; slow first run (downloads the SDK)
./gradlew :neoforge-26.1.2:runClient # launch the game with the mod
./gradlew :neoforge-26.1.2:runServer # headless dedicated server (DEBUG logging - huge log)
```

Run `:common:test` first when iterating on genetics/stats - it doesn't touch
Minecraft. Requires JDK 25; `foojay-resolver-convention` in
`settings.gradle.kts` auto-provisions it.

`runServer` here does not auto-stop - it sits at the console after `Done`.
Kill it (`taskkill`, or a PowerShell `Stop-Process` on the recent `java`
pids) when the smoke test has printed `Done (...)! For help`.

## The genetics model, as implemented

**Alleles are objects.** A `Genotype` is one `AllelePair` per registered
`Gene` (`common/genetics/`). It round-trips through a **code string**: one
segment per gene in `Genes.codeOrder()`, segments joined by `-`, the two
alleles of a gene joined by `/`, dominant first. Allele tokens can be **any run
of characters** (`Spl`, `Cr`, `prl`, `Ch`, `N`, ...). Example:
`"E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-Cr/N-N/N"`. **No legacy / short-code
handling** - dev only, no saves to keep.

`Genes.codeOrder()` = extension, agouti, white, test, champagne, splash, grey,
cream, pearl, magic zebra, pink hair - **append** new ones. **Full per-gene detail is in `Gene
Dict.md`** (natural genes first, then magical); one-liners:

| gene | alleles | dominance | in the wild | coat effect |
|------|---------|-----------|-------------|-------------|
| extension | `E`/`e` | dominant | 50/50 | `ee` = black restricted → chestnut |
| agouti | `A`/`a` | dominant | 50/50 | `A_` = bay; one uniform "point extent" off the `A` copy sets leg + face black, each leg jittered; a high roll = seal (non-det) |
| white | `W`/`w` | **complete** | 1/50 | `W_` = all pigment gone → transparent; masks every other gene |
| test | `T`/`t` | **complete** | 1/4 carrier | `T_` = paint the `TestCoatPattern` gradient **flat on top** in phase 3 (the only **magical** gene; visible on any base incl. white) |
| champagne | `Ch`/`c` | dominant | 1/40 | dilute toward the gradient's gold; keeps bay's points chocolate (amber champagne) |
| splash | `Spl`/`spl` | incomplete\* | 1/20 | random white socks + face blaze (non-det) - **open issue:** only the blaze, the sock edges are a hard ring, and \*it doesn't read its dose yet (`Spl/Spl` should be much bigger markings) |
| grey | `G`/`g` | dominant | 1/16 | **adults only** - **dapple grey** (`GreyCoat`): remaps onto the gradient's neutral column, per-horse progression / dapple size / dapple strength / point retention (non-det); foal born base colour |
| cream | `Cr`/`N` | incomplete | 1/30 | incomplete-dominant dilution; interacts with pearl; never leaves a pitch-black point |
| pearl | `prl`/`N` | incomplete | 1/22 | dilution; `prl/prl` no-cream = mild uniform; `Cr/prl` = double cream |
| magic zebra | `Mzeb`/`n` | dominant | 1/100 | **magical** - black stripes hung from the topline, `-200%` on all three channels so they read black over any coat incl. dominant white (non-det) |
| pink hair | `Pihr`/`n` | **recessive** | 1/12 carrier | **magical** - mane + tail walked 82% toward hot pink; reads what it paints over, so it keeps the strand shading (foal: tail only) |

**`Gene.dominance()`** (`common/genetics/DominancePattern`) is declared
metadata on every gene: `DOMINANT` / `RECESSIVE` / `INCOMPLETE_DOMINANT` /
`COMPLETE_DOMINANT` (= dominant **and** epistatic - while it shows, nothing
else is visible). `heterozygoteIsDistinct()` and `masksOtherGenes()` are the
two questions callers ask. Today's only consumer is `GenotypeCatalog`'s
gallery reduction, but it's per-gene metadata so a punnett/breeding UI can use
it too. **Pink hair is the only `RECESSIVE` gene** - the first one where the
heterozygote is a carrier you cannot see.

Cream + Pearl are allelic in reality; here two genes, combined once in
`coat.pattern.CreamPearlDilution` (dose table in `wiki/gene-*.html`).
Seal has **no gene** - it's the top of agouti's random distribution.

`Genotype.phenotype()` → coarse `CoatPhenotype` (`CHESTNUT`/`BLACK`/`BAY`/
`WHITE`; everything else ignored) - now only used for family-tree fallback
(foals are fully generated too).

`random(rng)` - each gene rolls its pair (draw counts in the gene class).
`breedWith` = **2 `nextBoolean()` per gene**. `Gene.isVisible(pair, genotype)`
/ `isDeterministic(pair, genotype)` see the whole genotype (agouti invisible on
chestnut; cream/pearl read each other). `Genotype.hasVisibleNonDeterministic()`
= "generate the texture per horse".

### Epigenetics are tied to the allele, not to the horse

Each **allele copy** a horse carries has an `AlleleEpigenetics(int priority,
long epigeneticSeed)`. `Epigenome` holds one per copy, **aligned** slot-for-slot
with the `Genotype`'s `AllelePair`s; `Genome` = `Genotype` + `Epigenome`, and it
exists because only a breeding pass that draws both at once can keep that
alignment true. Both round-trip through code strings (the epigenome's is
`<priority>:<seed hex>/<priority>:<seed hex>` per gene, `-` joined, same gene
order).

- **Expression** (`Epigenome.expressed(gene, genotype)`) - heterozygote: the
  dominant copy (an `AllelePair` is canonicalized dominant-first). Homozygote:
  both express, so the tie goes to the **higher priority**. That seed is what
  `CoatBuildContext.epigeneticsFor(geneKey)` runs on.
- **Inheritance** (`Genome.breedWith`) - the allele half is bit-for-bit
  `Genotype.breedWith`; each inherited allele brings its parent copy's priority
  and seed **verbatim**, no re-roll and (deliberately, for now) no variation.
  Copies are re-aligned after `AllelePair` sorts them. If both copies arrive on
  the same priority, one extra `nextBoolean()` bumps the second **±1**
  (`AlleleEpigenetics.deconflict`), clamped to `[1, Integer.MAX_VALUE]` - so a
  horse never carries a tie. Draws: **2 `nextBoolean()` per gene, +1 per tie.**
- **Founders only** roll fresh epigenetics (`Epigenome.random` via
  `CoatGenerator.generate`). A foal must never go through that path - see the
  data-flow section.
- `priority` has no other consumer, and **by design it never will**: it picks
  *which copy's seed expresses*, never the order genes are processed in. Gene
  order comes from a hard-coded per-gene number (planned - see
  `wiki/roadmap.html` §3), so that two horses with the same genotype and
  the same seeds can't diverge on priority alone and silently share a coat
  cache entry. It stays a full-range int for headroom.

Full inheritance detail: **`wiki/breeding.html`**.

## The three-phase coat pipeline (`common/coat/pattern/` + `client/GeneticCoatTextureFactory`)

Coats are **generated** for every horse - adult *and* foal. Per-gene detail in
**`wiki/gene-*.html`**; the machinery:

- **Every gene is either natural or magical, never both** (`Gene.isNatural()`,
  declared not inferred; a gene wanting both registers as two). A **natural**
  gene only pushes red/black pigment *down* in phase 1; a **magical** gene only
  adds *signed RGB* in phase 3, after the pigment has been resolved to colour.
  Natural is reserved for genes that exist in real life
  (`wiki/philosophy.html` §6).
- **Both coat hooks are pure.** A gene is handed **read-only views** of the
  state so far (`PigmentView`, `ColorView`) and **returns** its contribution;
  it never draws into shared scratch. `CoatBuildContext` therefore carries no
  fields any more - it's just genotype / epigenome / skin / adult /
  `epigeneticsFor`, and the composer owns both fields. Same inputs → same
  output, and a gene can be unit-tested against a synthetic coat on its own
  (`GeneCoatHookTest`).
  - Phase 1: `PigmentField restrict(pair, ctx, PigmentView coat)` - take
    `coat.mutableCopy()`, paint into it, return it; `null` = no contribution.
    (The doc's original sketch also handed phase 1 the magical field; it
    doesn't exist yet at that point in the bake, so it isn't passed.)
  - Phase 3: `ColorField tint(pair, ctx, PigmentView coat, ColorView colour)` -
    return a delta, or `null`. It gets the resolved natural coat too, so a gene
    can *find* a region (all the black, all the white) before painting it.

- **`CoatData`** = a `Genome` (`Genotype` + `Epigenome`), assigned once at
  birth and persisted. `textureKey()` = the genotype code, plus
  `@<fingerprint hex>` only when non-deterministic -
  `Epigenome.visibleFingerprint(genotype)` digests just the *expressed* seeds of
  genes that are visible **and** non-deterministic, so epigenetics a horse can't
  show don't fork the texture cache. The factory also keys on adult vs foal.
- **`CoatTextureComposer.compose(genotype, epigenome, Skin, adult, template, GradientLut)`**
  → 128px `int[]` ARGB:
  1. **natural pass** - every pixel starts max red + max black; each visible
     natural gene (`Genes.naturalOrder()` = extension → agouti → cream → pearl →
     champagne → grey → white → splash) pushes the `PigmentField` down.
  2. **resolve** - `(red, black)` → `GradientLut`. Both pigments **≈ 0**
     (`≤ TRANSPARENT_EPS` = 0.001 - only dominant white / a splash marking,
     which `setRed(0)`/`setBlack(0)` exactly) → transparent. The cutoff is
     deliberately far below any *dilution* floor (grey keeps 0.15; grey on a
     double-dilute cream still ≈ 0.012) - a 0.02 cutoff here was turning grey
     cremello / perlino chestnuts and bays into flat white horses.
     **Resolves to pure black → 80% opacity** (`PURE_BLACK_ALPHA`) so black
     isn't a flat void.
  3. **magical (RGB) pass** - each visible magical gene (`Genes.magicalOrder()`
     = pink hair → magic zebra → test) returns a signed RGB delta, folded into the `ColorField` by
     integer addition. Because that's associative and exact, ordinary magical
     genes are **order-independent and drift-free**. `ColorField.set` is the
     escape hatch - flat opaque paint that *replaces* the accumulator (order
     *does* matter for those), used only by a `COMPLETE_DOMINANT` gene that
     must read the same on black, chestnut *or* white. Test is the only one.
     **`magicalOrder()` is not arbitrary**, despite the additivity: pink hair
     *reads* what it's painting over (so it's order-dependent by choice), and
     Test paints flat and masks everything, so it has to run **last**.
  4. **composite** onto the template, **alpha-aware** multiply (`blend`),
     keeping template alpha. Because it's a *multiply*, the template's own dark
     detail - hooves, nostrils, the shading between mane strands - survives on
     every coat, cremello included. Near-black texels there are expected and
     are **not** a gene failing to dilute; check the `PigmentField` values, not
     the composed pixels, when chasing one.
  5. **eyes** - `CoatRegions.redrawEyes(skin, …)` copies them verbatim (adult:
     2x2 pupil + 2x2 sclera at `{6,42}`/`{28,42}`; foal: 2x2 pupil at
     `{6,20}`/`{40,20}` - the head's L/R faces, not the front blob).

  Natural genes only move the pigment sample → champagne / cream / pearl all
  read off whatever is underneath; anything on white is invisible.
- **`ColorField`** is the phase-3 accumulator: per texel a **signed `int`**
  r/g/b **plus a separate opacity**, seeded from the resolved colour.
  - **Nothing is capped until `argb()`.** That headroom is the point: a gene
    can add so much blue that no combination of other genes pulls it back
    under 255 - the horse is blue unconditionally and its author never had to
    know what else it carries. Zebra is the same trick with the sign flipped.
    `add` **saturates** at `Integer.MIN/MAX_VALUE` rather than wrapping, so
    "obviously large" numbers stacked twice can't flip an always-blue horse
    black.
  - **Opacity is its own channel, deliberately.** Transparency used to ride on
    the pigment channels (both ≈ 0 → the bald template shows). Once phase 3 can
    add colour to a texel carrying no pigment, "no pigment" and "no paint" stop
    being the same statement. A magical gene *may* paint a dominant-white horse
    - white is natural, and every magical gene runs after every natural one -
    but it has to say so with `addOpacity`/`set`; colour alone on a transparent
    texel shows nothing.
  - The blend onto the resolved colour is a **straight signed add**, which is
    what keeps phase 3 order-independent. Anything fancier (a real
    overlay/soft-light) reintroduces order-dependence - if it ever changes,
    re-check that claim.
- **`CoatPipelineGoldenTest`** is the machinery's safety net: 20 genotypes ×
  3 seeds × adult/foal, hashed. It exists to prove a change to the *pipeline*
  leaves every horse byte-identical. When a **gene** deliberately changes,
  regenerate `common/src/test/resources/coat-golden.txt` (delete it, run the
  test, copy `common/build/coat-golden.txt` back) and say so in the commit.
- **`GradientLut`** wraps `assets/horsegenetics/textures/coat/redblackgradient.png`
  (hand-authored, 500x500): left = more red, bottom = more black; `(1,1)` =
  black, `(1,0)` = chestnut, `(0,0)` = white, champagne-gold column near the
  middle. `sample(red, black)`: `x = (1-red)*(w-1)`, `y = black*(h-1)`.
  **The `red = 0` column is the only neutral grey ramp in the whole LUT**; the
  *diagonal* runs through the golds (equal keep `0.4` on a black horse samples
  `(150,109,56)`, a tan). Any effect that should read grey has to walk the
  sample toward that column, not scale both pigments - that's the whole reason
  for `GreyCoat`'s remap.
- **`GreyCoat`** - the dapple-grey generator. Reads how dark each texel
  currently is (`0.55*red + 0.95*black`), writes that darkness back as **black**
  scaled by the horse's greying progression, and keeps only a fading trace of
  the red - so a grey lands on the gradient's neutral column while a greying
  chestnut still ends lighter than a greying black. Four epigenetic knobs off
  the `G` copy (+ a `long` seeding the dapple field): **progression** (keep
  `lerp(0.46, 0.10, p)`, steel → near-white), **dapple spacing** (2.8-5.0 body
  units), **dapple strength** (contrast, peaked mid-greying), **point
  retention** (mane/tail/ears/muzzle full, head half, lower legs ramped; scaled
  by `1 - progression`). Dapples come from `BodyNoise`.
- **`BodyStripes`** - the reusable stripe field, pure and in body space:
  bands of near-constant X warped by `BodyNoise`, plus a small **slant on
  `|z|`** that bends each stripe into a shallow chevron over the back. The
  slant isn't decoration - without it every face perpendicular to X (chest,
  rump, the front and back of each leg) sits at one phase and renders as a flat
  band. Magic zebra is the first caller; a natural **dun**'s leg barring and
  **brindle** should reuse it rather than reinvent it.
- **`BodyNoise`** - pure `(seed, x, y, z)` noise sampled in **body space**, so a
  pattern crosses part seams without a join. `cellDistance` = distance to the
  nearest jittered-lattice point, normalized (centres = dapples, gaps = the web
  between them); `value` = smooth value noise, used to warp the lattice off the
  grid. No state, no `Random` - a rebuilt coat is identical.
- **`CoatRegions`** - reusable `Skin`-aware helpers (fill mane/tail/ears/hooves,
  paint/blacken/whiten a leg, `whitenBlaze`, `redrawEyes`). **Open issue:**
  `whitenLowerLeg` cuts at a hard `point.y() <= cutoff`, so every splash sock
  ends in a perfect ring; and `whitenBlaze` is the only face marking there is.
- **`BayCoat`** - the bay generator. One **uniform** per-horse "point extent"
  off the `A` copy (`leg = 0.15 + extent*0.80`, `face = 0.04 + extent²*0.62`),
  then each of the four legs jittered `±14%` independently - so bays actually
  spread from low socks to seal instead of clustering low the way the old
  `f*f` product did, and one horse's four socks are never exactly level. Bottom
  `SOLID_PORTION` = **0.3** of the band solid, then a **smoothstep** fade to
  nothing - no hard cut-off line. Knobs: `BODY_BLACK`, `HOOF_FRACTION`,
  `SOLID_PORTION`, `LEG_JITTER`. Verified in-game 2026-09-01 (seal included);
  the widened distribution is **not** yet play-tested.
  Its points are set *absolutely* (`setBlack(1.0)` / `setRed(0.0)` via
  `CoatRegions.blackenPart` / `blackenLowerLeg`) and that's fine - the
  dilutions run after and scale them. What was **not** fine is that a point
  carries `red = 0`, and the gradient's zero-red column reads jet black down to
  `black ~0.4`, so scaling black alone never changed the colour. Every dilution
  now goes through **`PigmentField.dilute(keepRed, keepBlack, blackTint)`**,
  which also adds `blackTint * black` back as *red* - walking the sample
  sideways off that column into the warm browns. Amber champagne keeps
  chocolate points over a gold body, perlino rusty ones, a buckskin dark brown.
  **House rule: no cream horse keeps a pitch-black point** - dark brown is as
  far as it goes. Per-mode numbers: `wiki/gene-*.html`.
- **`GeneticCoatTextureFactory`** (client) loads the adult + foal templates +
  gradient once, runs `compose`, uploads a `DynamicTexture`, caches by
  `textureKey()` + `:adult`/`:foal`, cleared on world exit.
  - The `Identifier` it registers each texture under is
    **`coat/` + `CoatTextureId.encode(key)`** (`common/coat/`), an *injective*
    map into Minecraft's legal path charset: `a-z0-9` verbatim, `A-Z` → `.` +
    lower-case, everything else → `_` + 4 hex. **Do not go back to
    lower-casing / `[^A-Za-z0-9_] -> _`**: case *is* the dominance encoding, so
    that folded all 19 683 genotypes onto **27** ids
    (`E/e`≡`e/e`, `W/w`≡`w/w`, `A/a`≡`a/a`, …). `TextureManager#register` is a
    silent `Map.put` that **closes the loser**, so every deterministic coat in a
    bucket rendered whichever one baked last - a plain white horse whenever that
    was a dominant-white `W_` one. That was the real "chestnut/bay renders as the
    default white horse" bug (the `[coat] >> FLAT WHITE` debug line does *not*
    catch it: the victim's own bake is correct, it just loses its id).
    `KEY_BY_ID` in the factory is a tripwire that throws if two keys ever share
    an id again.
- Dev tool: `./gradlew :common:bakeCoatSamples` → `build/coat-samples/*.png`
  and `*_foal.png` (no game launch).

### The HD horse models (`client/HdHorseModel`, `client/HdBabyHorseModel`)

128px, per-part UV - structural copies of vanilla `AbstractEquineModel.
createBodyMesh` / `BabyHorseModel.createBabyMesh` with every cube at
`texScale = 0.5` and the layer baked at 128x128 (`ClientSetup.HD_HORSE` /
`HD_HORSE_BABY`), so `CubeDefinition.bake` → effective texture size
`128*0.5 = 64` and **every normalized UV is identical to vanilla** - the 2x
sheet just gives each face 2x the texels. The adult model additionally
re-`texOffs`'s the four legs / two ears onto their own patches and drops
`.mirror()`; the baby model already has per-leg patches so it's a straight 2x
pass. `horse_white.png` / `horse_white_baby.png` (in `common/.../assets/`, with
`*_vanilla64.png` references) are the vanilla white sheets scaled 2x.
`GeneticHorseRenderer` hands both models to its super ctor as adult / baby - no
per-entity model swap. It deliberately **does not add vanilla's
`HorseMarkingLayer`**: that layer paints `horse_markings_white.png` etc. over
the whole texture, so any horse (wild spawn or foal) that rolled
`Markings.WHITE` rendered as a **flat white horse** on top of a correct
generated coat. All white markings here come from the splash gene inside the
coat texture.

### `common/coat/skin/HorseSkinGeometry` - the body-space projection engine

Two meshes: `Skin.ADULT` and `Skin.BABY`. Body-space grid (adult verified
in-game 2026-08-31 as smooth / seamless): **X** 0 at the tail's rear edge →
+nose, **Y** 0 at the hoof bottom → +up, **Z** 0 at centre, **+Z = horse's
right**; model units (1 = 1/16 block = 2 texels). Each `Part` is an
axis-aligned box with six `Face`s (NOSE/TAIL span (Z,Y); TOP/BOTTOM span (X,Z);
RIGHT/LEFT span (X,Y)); the foal mesh has no MANE / MUZZLE. Static no-`Skin`
methods target ADULT; `Skin`-first overloads pick the mesh (`bounds`, `sample`,
`forEachTexel`, `project`, `bodyBounds`, `hasPart`). One absolute scale per
mesh, so an X-function coat is seamless. Geometry tables are lifted from the two
HD models; **rotated parts use their rest-pose AABB** (the foal's neck/head/ear
pivots are pre-resolved through the tilted neck) - face projection there is
approximate.


## Data flow (server -> client -> pixels)

1. **Wild spawn / `/summon` / gallery horse** -> `HorseRecord` attached
   (genetic code) and, on the same join, a `HorseCoatAttachment` =
   `{genotype code, epigenome code}` (`CoatGenerator.generate` -> a founder
   `Epigenome.random`). Both via `ModAttachments`; the coat attachment default
   is `HorseCoatAttachment.UNASSIGNED` until the handler replaces it.
   **Breeding is different**: `HorseBreedingHandler` writes the foal's coat
   attachment itself, from `damGenome.breedWith(sireGenome)`, because the
   inherited epigenetics can only be read while both parents are in hand - the
   join handler would re-roll them. It then sees an assigned attachment and
   leaves it alone.
2. Not auto-synced -> the handler sends `CoatSyncPayload` `{entityId, code,
   epigenome}` and `HorseRecordSyncPayload` to trackers (on every join, plus
   `StartTracking`).
3. Client caches in `ClientCoatCache` (`CoatData`) / `ClientHorseRecordCache`,
   cleared on `LoggingOut`.
4. `GeneticHorseRenderer.extractRenderState` reads `ClientCoatCache` ->
   `GeneticHorseRenderState.coatData`; `getTextureLocation` ->
   `GeneticCoatTextureFactory.getOrCreate(coatData, renderState.isBaby)` -
   generated for adult and foal alike (`HdHorseModel` / `HdBabyHorseModel`
   handed to the super ctor; no per-entity model swap).


## Horse stats (speed / health)

Domain side (roll band, record fields, breeding flow) is in **`wiki/breeding.html`**.
The Minecraft-attribute side, in `server/HorseRecords`:

- `entitySpeed(horse)` / `entityHealth(horse)` =
  `horse.getAttributeValue(Attributes.MOVEMENT_SPEED / MAX_HEALTH)`.
- `newFounder(...)` copies those onto the founder record via `.withStats(...)`.
- `HorseBreedingHandler` rolls the foal's stats from the parent records
  (`HorseStats.rollFoalStat`, band **`[0.75*min, 1.5*max]`**, **no cap**),
  stores them on the child record with a `ParentStats.of(damSpeed, sireSpeed,
  damHealth, sireHealth)` snapshot (for UI colouring), and calls
  `applyStatsToEntity(child, record, fullHeal=true)`.
- **Rounding lives in the `HorseRecord` ctor**: `ceilHealth` (whole number),
  `ceilSpeed` (3 decimals). Every stored value is rounded up; callers don't
  need to round.
- `applyStatsToEntity(horse, record, fullHeal)` sets the attribute **base**
  values (`AttributeInstance#setBaseValue`); `fullHeal` true = set current HP
  to the new max (newborn), false = only clamp current HP down if it now
  exceeds max (reload, so no free heal).
- `backfillStatsIfMissing(horse)` fills `0.0` stats from the live entity;
  `HorseGeneticsEventHandler.onHorseJoin` calls it for reloaded/bred horses
  and then `applyStatsToEntity(..., false)`.
- `0.0` on a record = "not recorded yet"; `HorseRecord.hasStats()` is
  `speed > 0 && health > 0`. Paper dump + inventory panel show `(unrolled)` /
  `-` in that case; when `parentStats` is present, both surfaces show
  `above both` / `between` / `below both` (paper) or a green/amber/red tint
  (`ParentStats.rankSpeed` / `rankHealth`, panel).

Still random, **not Mendelian** - `HorseStats` is an explicit placeholder.
Jump strength is not tracked yet.

## Riding through water (`HorseWaterRidingHandler`)

Vanilla already floats a **ridden** horse at the water surface (horses are in
the `minecraft:can_float_while_ridden` entity tag) and there is **no**
water-triggered auto-dismount - the only dismount is sneak
(`Player#wantsToStopRiding` = `isShiftKeyDown`). What's missing is usable
speed: in-water `travelInWater` gives ~0.02 b/t.

The handler, on `EntityTickEvent.Post` for a tamed, player-ridden
`AbstractHorse` that `isInWater()` and `isLocalInstanceAuthoritative()`
(so it runs on the controlling client, where movement is simulated, and on
the server for non-player control): adds a small upward `deltaMovement` when
the horse is submerged (keeps the rider's head out), and blends horizontal
`deltaMovement` toward a capped `WATER_RIDE_SPEED` (0.09 b/t) in the
direction the rider steers (`rider.zza`/`xxa`, rotated the way vanilla's
`moveRelative` does). **Feel is a first guess - unverified in-game.**

## 26.1.2 API notes (what the port actually required)

This SDK is further from mainline 1.21.x than the version numbers suggest.

- **`ResourceLocation` is now `net.minecraft.resources.Identifier`** - same
  surface (`withDefaultNamespace`, `fromNamespaceAndPath`), different name.
- **Horse classes moved** `net.minecraft.world.entity.animal.horse` ->
  `...animal.equine` (`Horse`, `Variant`, `Markings`, `AbstractHorse`).
- **`HorseRenderer` is `final`.** `GeneticHorseRenderer` extends
  `AbstractHorseRenderer<Horse, HorseRenderState, HorseModel>` and copies
  vanilla `HorseRenderer`'s constructor body verbatim. Render-state stays the
  vanilla `HorseRenderState` so the copied layers type-check;
  `createRenderState()` covariantly returns the `GeneticHorseRenderState`
  subclass and every instance really is that subclass.
- **`getTextureLocation(S)` is still the texture hook** - the `submit()` /
  `SubmitNodeCollector` split didn't swallow it for entities.
- **`EventBusSubscriber` dropped `bus()` / `Bus`.** `IModBusEvent` subtypes
  auto-route to the mod bus, everything else to the game bus.
- **`AttachmentType.Builder#serialize` takes a `MapCodec`**, not `Codec`.
- **`AttachmentType.builder(Function<IAttachmentHolder, T>)`** overload lets
  the default read the holder (e.g. `entity.getUUID()`).
- **`KeyMapping` conflict-context ctor takes a `KeyMapping.Category`** (a
  record keyed by an `Identifier`), not a lang key. Debug keybind reuses
  `KeyMapping.Category.MISC`.
- **`EntityType#create` needs an `EntitySpawnReason`** -
  `create(level, EntitySpawnReason.COMMAND)`.
- **`DynamicTexture(NativeImage)` is gone** - use
  `DynamicTexture(Supplier<String> label, NativeImage image)`; it takes
  ownership and closes the image (don't also close it). `NativeImage.getPixel
  (x,y)` / `setPixel(x,y,argb)` are **ARGB** (`ARGB.fromABGR`/`toABGR` under
  the hood); `getWidth()` / `getHeight()`. `GeneticCoatTextureFactory` builds
  a fresh `new NativeImage(128,128,false)`, `setPixel`s the composer's `int[]`
  in, and hands it to the `DynamicTexture`.
- **Serverbound packets**: `PacketDistributor.sendToServer` ->
  `net.neoforged.neoforge.client.network.ClientPacketDistributor`.
  `sendToPlayer` / `sendToPlayersTrackingEntity` stayed on `PacketDistributor`.
- **Reach the server** via `((ServerLevel) player.level()).getServer()` or
  `player.level().getServer()` (`Level#getServer()` exists;
  `ServerPlayer#getServer()` was the one that didn't resolve).
- **`dimension_type` JSON schema changed heavily.** Gone: `ultrawarm`,
  `natural`, `piglin_safe`, `respawn_anchor_works`, `bed_works`, `has_raids`,
  `fixed_time`, `effects`. New **required**: `has_ender_dragon_fight` (bool).
  `effects` -> `skybox` (`"none"` / `"overworld"` / `"end"`). Time-of-day
  moved to a WorldClock/Timeline registry (permanent noon would need
  `default_clock` / `timelines`). A stale `dimension_type` JSON fails
  `RegistryDataLoader` and takes down the client (`ReportedException` ->
  `Stopping!`) the moment a world is created - reads as "singleplayer is
  broken". `data/horsegenetics/dimension_type/debug_pens.json` is
  `min_y: 0, height/logical_height: 512` (512 for random-Y plot headroom).
- **Flat-generator `dimension/debug_pens.json`** schema unchanged;
  `"structure_overrides": []` disables villages/etc. It's now `the_void`
  biome + a single `air` layer (generates nothing - `DebugPenManager` builds
  the floor itself).
- **`com.mojang.authlib.GameProfile` is a record** - `profile.name()` /
  `profile.id()`, no `getName()`.
- **Attributes are `Holder<Attribute>`.** `LivingEntity#getAttributeValue(
  Holder<Attribute>)` -> double; `#getAttribute(Holder<Attribute>)` ->
  `@Nullable AttributeInstance` with `setBaseValue(double)`. `Attributes.
  MOVEMENT_SPEED` / `MAX_HEALTH` are holders. Horses register both by default
  (`createBaseHorseAttributes`); `WATER_MOVEMENT_EFFICIENCY` is **not** on
  horses, so don't rely on it.
- **Ride/movement gates**: `Entity#isLocalInstanceAuthoritative()` (public
  final) is the "this side simulates the movement" check - true on the
  controlling client for a player-ridden mob, on the server otherwise.
  `Entity#getControllingPassenger()` -> `@Nullable LivingEntity`.
  `Mob#moveTo(...)` was renamed **`snapTo(...)`** (same overloads; y arg must
  be `double`). No water auto-dismount anywhere in `Entity` / `LivingEntity`
  / `Player`.
- **The GUI layer is retained-mode.** `GuiGraphics` -> `net.minecraft.client
  .gui.GuiGraphicsExtractor`; screens/widgets override
  `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float)`
  instead of `render(...)`; text `graphics.text(font, comp, x, y, argb)` /
  `centeredText(...)`; rects `graphics.fill(x0,y0,x1,y1,argb)`; textures
  `graphics.blit(Identifier, x0,y0,x1,y1, u0,u1,v0,v1)`. Mouse:
  `mouseClicked(MouseButtonEvent event, boolean doubleClick)` (`event.x()` /
  `.y()` / `.button()`), same for `mouseReleased` / `mouseDragged`. NeoForge
  `ScreenEvent.Init.Post#addListener` and the `ScreenEvent.Render.*` events
  (each `#getGuiGraphics()`) still bolt widgets/overlays onto vanilla screens.
  `AbstractContainerScreen#getGuiLeft()` / `getGuiTop()` are
  deprecated-for-removal but still work (used by `HorseScreenHooks`).
  - **`ScreenEvent.Render` timing**: `Pre` fires *before* the dimmed backdrop,
    `Background` after the backdrop but before the widget stratum, `Post` after
    everything. To draw a panel *behind* your own buttons but *over* the
    backdrop, use `Background` (that's what `HorseScreenHooks` learned the hard
    way - `Pre` gets painted over, `Post` covers the buttons).
  - **Swallowing a keybind while a text field is focused**:
    `ScreenEvent.KeyPressed.Pre` (`getKeyCode()` / `getKeyEvent()`), cancel it
    so the screen's `keyInventory` handler doesn't close the GUI; forward the
    key to the box yourself for backspace/arrows. `CharacterTyped` is a
    separate event, so letters still type (`HorseScreenHooks.onKeyPressed`).
  - **Scaling GUI text**: `g.pose()` is a `Matrix3x2fStack` -
    `pushMatrix()` / `translate(x,y)` / `scale(s)` / `popMatrix()` around a
    `g.text(font, comp, 0, 0, argb)` call scales it about `(x,y)`
    (`FamilyTreeScreen.drawFitted`). `g.enableScissor(x0,y0,x1,y1)` /
    `disableScissor()` clip everything including `g.entity(...)`.
  - **A live 3D entity in a screen**: build the render state yourself
    (`Minecraft.getEntityRenderDispatcher().getRenderer(e).createRenderState(e,
    1f)`, then `state.shadowPieces.clear()`, poke `LivingEntityRenderState`
    angles) and `g.entity(state, scale, translation, rot, camRot, x0,y0,x1,y1)`
    - see `FamilyTreeScreen.drawHorseModel`, which uses a throwaway client-only
    `Horse` and injects the coat onto `GeneticHorseRenderState` directly (no
    `ClientCoatCache` pollution).
- **`SavedData` is gutted** to a `dirty` flag - no `save(CompoundTag)`.
  Persistence is `SavedDataType<T>(Identifier id, Supplier<T> ctor,
  Codec<T> codec)` via `SavedDataStorage#computeIfAbsent`
  (`ServerLevel#getDataStorage()` per level,
  `MinecraftServer#getDataStorage()` server-global - the ancestry DB uses the
  server-global one, so it lands in `<save>/data/` = per world).
- **Block registry**: `DeferredRegister.createBlocks(modid)` ->
  `.registerBlock(name, Function<Properties, ? extends B>,
  Supplier<Properties>)` - third arg is a **`Supplier<Properties>`**, not a
  bare `Properties` -> `DeferredBlock<B>`. `ModBlocks.register(modEventBus)`
  and `ModBlockEntities.register(modEventBus)` are called from the
  `HorseGenetics` constructor. No `BlockItem` for `hay_portal` (placed only
  by code).
- **`hay_portal` block entity + renderer**: `HayPortalBlock extends
  BaseEntityBlock`, `getRenderShape` -> `RenderShape.INVISIBLE`,
  `newBlockEntity` -> `HayPortalBlockEntity extends TheEndPortalBlockEntity`
  (subclassed only so `AbstractEndPortalRenderer`'s `T extends
  TheEndPortalBlockEntity` bound is satisfied; its protected ctor is callable
  from a subclass). `ModBlockEntities` registers the type via `new
  BlockEntityType<>(HayPortalBlockEntity::new, ModBlocks.HAY_PORTAL.get())`.
  `HayPortalRenderer extends AbstractEndPortalRenderer<HayPortalBlockEntity,
  HayPortalRenderState>` **for the plumbing only** (`facesToShow` population +
  BER registration) - `submit(...)` no longer touches the End-portal shader.
  `HayPortalRenderState extends EndPortalRenderState` adds `axis` (from the
  block's `AXIS` state), set in an `extractRenderState` override.
  - Geometry: `AXIS` (`HORIZONTAL_AXIS`) is the axis the portal *plane* runs
    along (vanilla nether convention). The slab is thin along the **other**
    axis (`thinAxis` = X<->Z swap), inset to `SLAB_MIN`..`SLAB_MAX` (0.25..0.75)
    there, and only the two faces with `dir.getAxis() == thinAxis` are drawn -
    so it faces the player, not edge-on. (Getting this backwards was the
    "portal is sideways" bug.) `emitFace(dir, from, to, ...)`, winding
    `right = up x normal`, corners BL/BR/TR/TL.
  - Two passes per face: **opaque black** (`RenderTypes.entitySolid(TEXTURE)`,
    colour 0,0,0 - `ENTITY_SOLID` has no alpha discard + writes depth, so the
    sky / clouds / water can't show through) then the **animated swirl**
    (`RenderTypes.entityTranslucentEmissive(TEXTURE, false)`, colour white,
    full-bright `0x00F000F0`, `OverlayTexture.NO_OVERLAY`).
  - Texture `assets/horsegenetics/textures/block/hay_portal.png` - 64-wide
    vertical strip of **36** 64x64 frames. Frame chosen by V offset;
    `HayPortalClientAnim.currentFrame(36)` advances it by wall-clock time at a
    speed that ramps **12 fps -> 48 fps** as a client-estimated "charge" (local
    player standing in a `hay_portal` block, ramp ~10 s / decay ~2 s) rises.
    The `.png.mcmeta` (36 frame entries) is **not** read - kept for docs / a
    possible future atlas move.
  - `HayPortalBlockEntity#shouldRenderFace` renders a face only if the
    neighbour isn't another `HayPortalBlock` (multi-block portal shows just its
    outer shell). Registered in `ClientSetup`. Blockstate JSON: 2 axis variants
    -> one model that only sets a `particle` texture.
  - **To restore the End-portal starfield:** put back `submitCube(
    state.facesToShow, RenderTypes.endPortal(), poseStack, submitNodeCollector)`
    in `submit` (git history).
- **Mojang un-typo'd** `BlockBehaviour.Properties.noCollission()` ->
  **`noCollision()`**.
- **Spawn point**: no `Level#getSharedSpawnPos()`. Use
  `level.getRespawnData().pos()` (`LevelData.RespawnData` record:
  `globalPos()` / `yaw()` / `pitch()` + a `pos()` convenience).
- **Cross-dimension teleport for any entity**: `Entity#teleportTo(ServerLevel
  level, double x,y,z, Set<Relative>, float yRot, float xRot, boolean
  resetCamera)` - pulled up to `Entity`. `Set.of()` target-types to
  `Set<Relative>`. *Verified 2026-08-30 for non-player entities (horses come
  back through the return portal).*
- **Leash**: `Mob implements Leashable`; `isLeashed()`, `getLeashHolder()`
  (-> `Entity`), `dropLeash()` (drops the lead item), `removeLeash()` (no
  drop). *Verified 2026-09-01: the roped-horse portal shortcut sends the horse
  through **and** drops the lead.*
- **Event hooks used this pass**: `LivingIncomingDamageEvent` (cancelable,
  pre-mitigation - horse invulnerability); `EntityTickEvent.Post` (portal
  dwell timers, water riding, tamer tracking);
  `PlayerInteractEvent.RightClickBlock` (`getPos()`, `getItemStack()`,
  `setCanceled` + `setCancellationResult(InteractionResult)`);
  `PlayerEvent.PlayerChangedDimensionEvent` (`getFrom()`/`getTo()` ->
  `ResourceKey<Level>`), `PlayerLoggedIn/OutEvent`.
- **`PlayerInteractEvent.EntityInteract` fires on both sides.** For
  interactions that vanilla would otherwise turn into a mount (any item on a
  tamed horse), you must `setCanceled(true)` **on the client too**, or the
  client predicts the mount and rubber-bands. Do the state mutation
  server-only, cancel on both. (`HorseInteractionHandler` clock/stick.)
- **Standing signs** (not currently used - kept for reference):
  `Blocks.OAK_SIGN` + `StandingSignBlock.ROTATION` (0-15, from
  `RotationSegment.convertToSegment(...)`); text via `SignBlockEntity#
  getFrontText()` -> `SignText#setMessage(line, Component)` (returns a new
  one) -> `setText(text, true)` + `setChanged()` + `sendBlockUpdated`.

## Build setup notes

- `common/build.gradle.kts` declares its own `repositories { mavenCentral() }`
  and `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` - both
  required under Gradle 9.
- Mod metadata is `src/main/resources/META-INF/neoforge.mods.toml`;
  `minecraft` dependency `versionRange = "[26.1.2,)"` (the MDK's
  `[1.26.1,1.27)` does not match the version string `26.1.2` and makes FML
  refuse the mod). There's a stray un-packaged duplicate at
  `src/main/neoforge.mods.toml` (wrong dir, from a `git mv`) - delete it or
  move it under `resources/META-INF/`; don't let the two drift.
- **Config**: `ClientConfig` (`net.neoforged.neoforge.common.ModConfigSpec`,
  `ModConfig.Type.CLIENT`) registered from the `HorseGenetics(IEventBus,
  ModContainer)` constructor. One key so far: `familyTree.scrollBar` (default
  `false` = shrink the Family Tree to fit; `true` = full-size + scroll). Read
  via `ClientConfig.familyTreeScrollBar()` which swallows the not-yet-loaded
  `IllegalStateException`.
- **`.gitignore`** (repo root) covers `build/`, `.gradle/`, `.idea/`, `**/run/*`,
  any `saves/`, and the JVM's `hs_err_pid*.log` droppings - the tracked tree is
  down from 2 446 files to 148 (source, docs, the gradle wrapper). The **one
  deliberate exception** is `run/config/fml.toml`, kept tracked by a
  `!**/run/config/fml.toml` negation because it carries this laptop's
  `earlyWindowControl = false` launch fix; the negation needs the
  `!**/run/config/` + `**/run/config/*` pair above it to work, so don't collapse
  those three lines.
- `generate_file_list.py` and the README file-tree block were removed this
  session (README is user-facing now).

## Running the game

`build` only assembles the jar. `runClient` / `runServer` launch MC
`26.1.2.100` with the mod. IntelliJ Gradle sync generates the run configs.

Two machine-specific launch blockers on this dev laptop (NVIDIA RTX 3050 Ti +
AMD integrated, AMD driver from 2023):

- **JVM hard-crash (`EXCEPTION_ACCESS_VIOLATION` in `atio6axx.dll`) at
  `glfwCreateWindow`** - the process ran on the old AMD integrated driver.
  Fix: Windows per-app GPU preference pinning `java.exe` / `javaw.exe` to
  "High performance" (`HKCU\Software\Microsoft\DirectX\UserGpuPreferences`,
  `GpuPreference=2;`). Durable; a driver update would also fix it.
- **FML early-loading splash window** hits the same crash one step earlier.
  `run/config/fml.toml` has `earlyWindowControl = false`. `run/` is otherwise
  git-ignored, but **that one file is deliberately still tracked** (see "Build
  setup notes") so a `git clean` can't reset it to `true`.

### The "Spawn Test Horse World" button cleans up after itself

`client/DebugTitleScreenButton` (dev only) creates a throwaway creative world
named by `DebugTestWorldCleanup.newDirectoryName()` = `test_horse_<millis>`.
`client/DebugTestWorldCleanup` deletes those worlds again, in two sweeps over
`saves/`, both matching **only** `test_horse_` + digits so a hand-made world is
never a candidate:

- **`ClientStoppedEvent`** - the normal path. It's posted on the game bus from
  `Minecraft#destroy` *after* the disconnect has halted the integrated server
  and spun waiting for it to finish saving, and after `close()` - so nothing
  still holds the save folder, and it's the last hook before `System.exit`.
  (`ClientStoppingEvent` would be too early: the server is still running.)
- **`ClientStartedEvent`** - the safety net, for a crash, a `taskkill`, or a
  delete Windows refused because a handle lingered. Runs before anything opens
  a world.

Both are `ClientLifecycleEvent` subclasses, which do **not** implement
`IModBusEvent`, so `@EventBusSubscriber(value = Dist.CLIENT)` routes them to
the game bus. A failed delete only warns - the next launch retries.

## The horse dimension (`server/DebugPenManager` + portals)

It is a **normal feature** now, not debug-only - reachable by hay-bale portal
in any build. Only the **F6 shortcut** is dev-gated (`DebugKeyBindings`
registers the keybind only if `!FMLEnvironment.isProduction()`, and
`ModNetworking`'s `RequestDebugPensPayload` handler re-checks). The class /
dimension / `ResourceKey` names still say "debug" (`DebugPenManager`,
`DEBUG_LEVEL`, `horsegenetics:debug_pens`) - a rename is deferred to avoid
breaking existing save data mid-session; treat the names as legacy.

### The dimension is a void; each visit is a private, disposable instance

`dimension/debug_pens.json` generates **nothing** (`the_void` biome, one
`air` layer). Every visit gets its own `DebugPenManager.Plot`: a corridor
built at a unique world X (`PLOT_SPACING_X` = the catalogue corridor + 1 000
= **2 526** blocks apart, X slots recycled via a free-list) and a **fixed Y**
(`PLOT_BASE_Y` = 128). Plots never share chunks - "two players never land in
the same place" holds on a server with no real per-player-dimension work.

`PLOTS` is `Map<UUID player, Plot>`, strictly 1:1 - every `enter()` makes a
new plot and tears down that player's previous one, so a revisit always
regenerates and no live horse left behind survives.

**Leaving clears entities, not blocks.** The gallery is deterministic (fixed
catalogue, fixed `PLOT_BASE_Y`, fixed length), so an X slot handed back to the
free list is rebuilt with byte-identical geometry and the stale corridor is
overwritten in place - there's nothing to gain from air-filling it first.
`tearDown` is therefore O(entities), not O(blocks walked). It also
`HorseAncestryData.forget(...)`s each discarded horse, or every visit would
leave ~868 throwaway gallery records in the save forever. (A record that
*references* a forgotten horse as a parent is left alone - `ancestorsOf`
already skips ancestors it can't find, so a foal bred in the dimension and
taken home keeps working with a missing parent node.)

- `teleportAndGenerate(player)` (F6) -> `enter(player, player's current dim,
  player's current pos)`.
- `enter(player, returnDim, returnPos)` -> allocate plot, build lookahead,
  build the return portal, teleport to `(originX + 3.5, baseY + 1, 0.5)`
  facing +X, drop a **paper** in the first free hotbar slot.
- `leave(server, playerUUID)` -> `tearDown`: discard non-player entities in
  the plot AABB, forget their ancestry records, return the X slot. **Blocks are
  left standing** (see above). Fired from
  `PortalEventHandler.onChangedDimension` (from == DEBUG_LEVEL) and
  `onLogout`.
- **Leaving takes your horses.** As a player's exit-portal dwell hits tick 1
  they get a chat warning ("anything left behind is lost forever, tamed
  horses come with you"). At teleport, `teleportThroughPortal` calls
  `DebugPenManager.evacuateTamedHorses(debug, plot, onlyOwner, dest,
  destPos)` - `onlyOwner = player.getUUID()` if `portalLevel.players()` still
  has someone else, `null` (all tamed horses) if this player is the last one
  out. Owner match is `horse.getOwnerReference().getUUID()`. Untamed / other
  players' horses are then discarded by `tearDown`. Each evacuated horse is
  **dropped in the air** ~2 blocks above the return portal, spread on a 3-wide
  grid, with **10 s of invulnerability** so the short fall can't hurt it
  (`HorsePortalManager.placeReturningHorse` ->
  `PortalEventHandler.grantReturnInvulnerability`: sets `setInvulnerable(true)`,
  parks the id in `RETURN_INVULN` + `COOLDOWN` for `RETURN_INVULN_TICKS`, then
  `onEntityTick` clears it). No terrain is carved. A lone horse that dwells in
  the exit portal itself gets the same treatment.
- `PortalEventHandler.onLogin` bounces a player who logs in inside the dim
  with no live plot (server restart) to the overworld spawn.
- `DebugPenTickHandler` (`PlayerTickEvent.Post`, **not** dev-gated anymore)
  -> `ensureGeneratedAheadOfPlayer(player)` extends the corridor as you walk.

### The gallery: one pen per *visually distinct* genotype (`common/genetics/GenotypeCatalog`)

The dimension is a **gallery of the genotype catalogue** - two horses for every
genotype that looks different from every other.

- **`GenotypeCatalog`** (pure `common/`, unit-tested) is the enumeration.
  `allPairsOf(gene)` = every unordered `AllelePair`, **least dominant first**
  (`ee`, `Ee`, `EE`); `distinctPairsOf(gene)` applies the dominance reduction;
  `totalGenotypes()` = the raw product (**177 147**); `size()` = the reduced
  catalogue (**1 730**); `get(i)` / `entries()` read the list, built once at class
  load. Nothing is hard-coded - register a gene (or an allele) and the
  catalogue, the corridor length and both signs widen on their own.
- **Two reductions**, both driven by `Gene.dominance()` (see below):
  - a gene whose heterozygote isn't distinct (`DOMINANT` / `RECESSIVE`)
    contributes only its **homozygotes** - `ee`/`EE`, not `Ee`;
  - a `COMPLETE_DOMINANT` gene **masks everything else**, so the catalogue keeps
    exactly **one** entry for it: the variant homozygote with every other gene
    at wild type. Hence one white pen (`EEaa WW`, #5) and one test pen
    (`EEaa TT`, #6) instead of a quarter of the corridor each.
  - Net: `2·2·2·3·2·3·3·2·2 = 1 728` unmasked + 1 white + 1 test = **1 730**
    (the last two 2s are magic zebra and pink hair - a `RECESSIVE` gene reduces
    exactly like a `DOMINANT` one, homozygotes only).
- **Pen order**: segment `i` holds catalogue entry `2i` in the **right-hand**
  pen (`NORTH_PEN`, the `+Z` side - your right walking in from the portal) and
  `2i+1` on the left. The corridor reads `eeaa, EEaa, eeAA, EEAA, [white],
  [test], eeaa ChCh, ...`: extension exhausts before agouti moves. With an odd
  catalogue the final left-hand pen is simply not built.
- **Both horses in a pen share the genotype** but not the epigenome, so
  they're two examples rather than two copies.
- **Signs** (`placeSign`, waxed standing oak, same text on both faces):
  - per pen, on the road one block out from the wall and **to the right of the
    gate** as you face the pen (`roadFacing().getOpposite().getClockWise()`, so
    the two sides of the road mirror): line 0 = `#<1-based catalogue number>`,
    then **`GeneCodeDisplay.shortForm`** - the same compact form the info panel
    and paper dump use, so a plain horse reads `eeaa`, not a wall of wild-type
    slots - greedily wrapped over the remaining 3 lines by
    `GeneCodeDisplay.wrap(genotype, 3, 15)`. **At 11 genes the widest labels no
    longer fit**: `eeaa SplSpl ChCh CrCr prlprl GG MzebMzeb PihrPihr` is 49
    chars against 3x15, and `wrap` deliberately overflows its **last** line
    rather than dropping a gene, so those signs read wide in-game (worst case
    27 chars). The unit test now asserts only that nothing is lost and that the
    overflow doesn't grow past 30. The real fix is the planned revert to random
    pens (`wiki/roadmap.html` §9), which retires the per-genotype sign
    entirely - so this is deliberately left alone.
  - `originX + 4` (three blocks in front of the return portal), facing west at
    the player's spawn: `Genotypes / 177,147 / Distinct / 1,730 pens`.
    Epigenetics are deliberately not counted in either number.
- **Length**: `LAST_SEGMENT_INDEX` = `ceil(size / 2) - 1` = 864, so the corridor
  is **6 055 blocks** (it was 1 519 at 9 genes - each new gene multiplies it, which
  is its own argument for §9's revert to random pens). `ensureBuiltUpToIndex` clamps to it and calls
  `buildEndCap` (the mirror of `buildStartCap`) on the last segment. Pens are
  still built lazily as you walk.

### Layout (`DebugPenManager`)

Geometry is relative to `plot.originX` (+X) and `plot.baseY` (grass surface).
Cross-section (Z) from centre out:

```
road (gravel z -3..3) | pen 6x20 (brick walls) | gravel strip + glowstone above (z 24) | oak-plank wood wall (z 25) | bedrock core (z 26) | VOID
```

- **Floor**: laid by `DebugPenManager` (generator makes none) -
  bedrock/dirt/dirt/grass at `baseY-3..baseY`, only for `z` in [-26, 26].
  Beyond the bedrock core: open void.
- **Gravel road** down the centre, continuous along X.
- **One pen per side**: `PEN_LEN_X` = **6**, `PEN_DEPTH_Z` = 20. Perimeter is
  `Blocks.BRICK_WALL`; the road-side edge has a **two-wide** oak-fence-gate
  opening (`gateX`, `gateX+1` where `gateX = x0 + PEN_LEN_X/2 - 1`) - a
  1-wide gate lets horses escape. Torches on the four corner wall posts
  (corners get `up=true`, so a solid top). The two **gate-side interior
  corners** hold amenities, one block in from the road-side wall
  (`zGateInner = zRoad + signum(zBack-zRoad)`) and **sunk to `floorY-1`** so
  their tops are flush with the grass (a full block at floorY was a step the
  horses hopped the 1-high wall from): a full `WATER_CAULDRON` (`LEVEL=3`) at
  `x0+1`, a `HAY_BLOCK` at `xMax-1`.
- **E/W walls** (behind the pens), flush against the pen back edge
  (`PEN_FAR_Z` = ±23), no grass gap: `GRAVEL_STRIP_Z` = ±24 (gravel, with a
  glowstone line directly above at `baseY+10`), `WALL_PLANK_Z` = ±25 (a
  **single** oak-plank wood wall, `baseY .. baseY+9`), `WALL_BEDROCK_Z` = ±26
  (bedrock core, `baseY-3 .. baseY+9`).
- **Wall behind the return portal** (`buildStartCap`): pushed 2 blocks back from
  `originX-1` to `originX-3`, layered like the E/W walls. Bedrock core at
  `originX-3` (`baseY-3 .. baseY+9`); a single oak-plank wood wall at
  `originX-2` (`baseY .. baseY+9`) on a bedrock/dirt base; a gravel **floor
  strip** at `originX-1` `baseY` (also on a bedrock/dirt base) with a glowstone
  line floating at `baseY+10`. The little nook between it and the portal
  (`originX`, `originX-1` above the floor) is open air. **Return portal** (hay
  frame + `hay_portal`, axis Z) at `originX + 1`. (An earlier version made
  `originX-1` a full-height *gravel column* with no base - it fell into the void
  and left a gap; hence everything now stands on bedrock/dirt.)
- Pens repeat every `PERIOD` (= `PEN_LEN_X + 1` = 7); `LOOKAHEAD_PENS` = 30
  built up front.
- Each pen: **one mare + one stallion** (`spawnHorse(..., Sex, geneticCode)`
  pre-sets the founder record so `onHorseJoin` doesn't re-roll sex *or*
  genotype; `newFounder` also copies the entity's speed/health onto the
  record).
- Horses in `DEBUG_LEVEL` **take no damage**
  (`HorseGeneticsEventHandler.noHorseDamageInDebugDimension` cancels
  `LivingIncomingDamageEvent` for any `AbstractHorse`).
- `DEBUG_LEVEL` terrain is **read-only**: `noBlockBreakInDebugDimension`
  (`BreakBlockEvent`) and `noBlockPlaceInDebugDimension`
  (`BlockEvent.EntityPlaceEvent`, which also covers `EntityMultiPlaceEvent`)
  both cancel in that dimension. Bucket/fluid placement isn't covered yet.
- `HorseInteractionHandler` in `DEBUG_LEVEL`: **stick** tames an untamed
  horse, **clock** ages a foal to adult (`setAge(0)`). *Verified 2026-09-01:
  the clock ages a tamed foal without the interaction falling through to a
  mount - see the `EntityInteract`-fires-on-both-sides note above.*
- Non-horse mobs refused entry by
  `HorseGeneticsEventHandler.keepDebugDimensionHorsesOnly`.

Name word tables (`common/src/main/resources/horsegenetics/names/`) must be
on the server runtime classpath - `HorseRecords`' static
`HorseNameGenerator.fromResources()` throws on class-load if missing.

## Hay-bale portals (`block/HayPortalBlock`, `server/HorsePortalManager`, `server/PortalEventHandler`)

- **`horsegenetics:hay_portal`** - cosmetic `BaseEntityBlock`: `noCollision`,
  `noOcclusion`, indestructible (`strength(-1, 3600000)`), light 11, no loot,
  `PushReaction.DESTROY`, `AXIS` state (now also drives the render slab
  orientation), `RenderShape.INVISIBLE`. Visual is the mod's own **animated
  texture** (`textures/block/hay_portal.png`, 36-frame vertical strip) drawn by
  `HayPortalRenderer` as a **half-block-thick opaque slab** centred in the
  block, frame rate ramping **12->48 fps** via `HayPortalClientAnim` as you
  dwell (see the API note for mechanics / changing frames / restoring the
  End-portal starfield).
- **Lighting one**: build a hay-bale frame (nether-portal inner sizes: 2..21
  wide, 3..21 tall, **vertical**) **outside** the horse dim, right-click a
  frame block with a **golden carrot**. `HorsePortalManager.findFrame` tries
  **every** in-plane air-neighbour of the clicked hay as a flood-fill seed
  (the old code took the first and broke on corner / far-post clicks -
  suspected cause of the user's "did nothing"), floods the enclosed air in
  the frame plane (axis X then Z), requires a filled rectangle fully bounded
  by hay, fills it with `hay_portal`. Golden carrot consumed; the player gets
  a chat line on success **or** failure (with the size rule).
- **Dwell teleport**: `PortalEventHandler.onEntityTick` counts ticks a
  player/horse stands in a `hay_portal` block (checks feet + `y+0.9`).
  **Player 200 t (10 s)**, **horse 60 t (3 s)** -> `teleportThroughPortal`.
  A 100-tick cooldown blocks instant re-trigger (returning horses are parked
  in that cooldown for their 10 s invuln window so they can't be yanked back
  through the portal they land on). While the counter runs, `spawnPortalSwirl`
  rings the entity with **gold `DustParticleOptions`** (denser as it nears
  zero - vanilla has no gold portal particle) and a **player** gets a
  per-second countdown line in chat (`"Portal -> N seconds..."`, last 5 s) plus
  a one-time "grabs hold" message on tick 1. `LAST_COUNTDOWN` dedupes the
  lines. The dwell guard accepts any `AbstractHorse`, not just `Horse`.
  - Non-debug portal + player -> `DebugPenManager.enter(player, that dim,
    portalPos.above())` - fresh plot, return wired to that portal.
  - Debug-dim portal + player -> `plotContaining(x)` -> `plot.returnDim` /
    `plot.returnPos`, **and `evacuateTamedHorses`** (see the dimension
    section). A leashed horse `dropLeash()`s. Non-player entities in a
    non-debug portal: no-op (documented limitation).
- **Roped-horse shortcut**: right-click a `hay_portal` while leading horses
  (`onRightClickBlock`, scans `getEntitiesOfClass(Mob, inflate(12),
  isLeashed && getLeashHolder == player)`) - each horse `snapTo` the portal
  block, `dropLeash()`, dwell counter seeded so it teleports in ~3 s.

**Verified 2026-08-30:** frame flood-fill + golden-carrot activation; the
animated portal texture (faces the player, opaque, 12->48 fps ramp); gold-dust
swirl + chat countdown; cross-dimension `teleportTo` for horses; return
teleport (tamed horses land beside the overworld portal, unharmed).
**Verified 2026-09-01:** the `PortalEventHandler.onRightClickBlock` roped-horse
shortcut - right-clicking a `hay_portal` while leading a horse sends it through
and drops the lead.
**Still unverified**: whether the 10 s player dwell feels too long.

Known limitation: `PLOTS` is in-memory, so a server restart orphans a dead
plot's blocks **and its horses** in the void (harmless - the next plot at that
X rebuilds identical geometry over them, `buildPen` sees the surviving horses
and doesn't double-spawn, and `onLogin` keeps players out of a plotless
dimension). Their ancestry records are the one thing that leaks on a crash;
a clean exit forgets them.

## Known gaps / next steps

The **`runClient` checklist lives in `wiki/verification.html`** - both the
**open issues** found in-game and what's still unconfirmed. Keep that file
current after each session. The **long-range** backlog (the full gene wishlist,
per-allele stack priority, the modder-facing gene API, non-coat and health
genes) lives in **`wiki/roadmap.html`**; this list stays near-term.

**Fixed since that session:** grey (was "flat near-white, wants a rework") is
now the `GreyCoat` dapple grey - built, unit-tested and sample-baked, **not yet
seen in-game**.

**Open rendering issues (found in-game 2026-09-01, deliberately not fixed
yet)** - full detail in `wiki/verification.html`:

- **Splash is only the centreline blaze + plain socks.** Missing the rest of
  the face-marking family (star, snip, stripe, bald face), and
  `whitenLowerLeg`'s hard `y <= cutoff` cut makes each sock a perfect ring -
  wants epigenetic jitter or the `BayCoat.fade` smoothstep treatment.
- **Splash isn't actually incomplete dominant.** It's *tagged*
  `INCOMPLETE_DOMINANT` (so the gallery gives `Spl/spl` and `Spl/Spl` their own
  pens) but `restrict` never reads the dose, so the two render identically.
  Homozygous splash should be **much bigger** - higher stockings, a wide blaze
  or bald face, body patches. Gallery pens **#11** and **#19** are the side-by-
  side check.

Design follow-ups (not just "go look at it"):

1. **Grey has no age - and that's now a decision, not a gap.** Horse **aging is
   deliberately out of scope** (it risks feeling bad for a player attached to a
   horse), so `GreyCoat`'s progression stays drawn once from the `G` copy's
   epigenetics and fixed for life: one grey is a steel four-year-old, another
   near-white, neither changes. Flea-bitten grey and grey melanoma are parked
   with it. The option isn't foreclosed - reopening it means giving the
   composer a real age input, which today only knows adult vs foal. See
   `wiki/roadmap.html` §7.4.
2. **Foal geometry is approximate** - `Skin.BABY` uses rest-pose AABBs and
   pre-resolved neck/head/ear pivots; markings on the foal face/neck can land
   loosely. Also the foal mesh has no MANE/MUZZLE part, so bay foal "black up
   the face" is coarse. Foals are also the top **unverified** item.
3. **Genetic eye colour** - the eyes render correctly but are copied verbatim
   from the template (`CoatRegions.redrawEyes`). Wants its own gene; the
   classic hook is blue eyes on cream double-dilutes.
4. **White markings beyond splash** - the framework is ready (natural +
   non-deterministic gene); sock distributions, roan and rabicano slot in the
   same way.
5. **More loci** - dun, pearl-cream stacking nuance, sooty.
6. **Coat realism** - outside grey's dapples, every gene still samples the
   gradient flat per pixel (no sooty shading, no seasonal coat). `BodyNoise` is
   the reusable seam for the next one. `T` on a non-deterministic coat still bakes a
   unique (identical-looking) texture per horse.
7. **Phase 3 now has three inhabitants and the blend question is answered.**
   Magic zebra validated the *negative* half of the unclamped signed model
   (`-200%` reads black over any coat, dominant white included); pink hair
   showed that a **blind add is not enough** on its own - to reach pink on a
   black mane a fixed delta has to push hard enough to saturate a pale mane to
   white, so it reads `ColorView.visible` and returns the delta that walks the
   texel toward its target. So: *straight signed add* stays the blend, but the
   useful magical genes will read first, and `magicalOrder()` matters more than
   §1 assumed. `naturalOrder()` / `magicalOrder()` are still hand-written lists
   - making them *derived* is the gene-priority work (`wiki/roadmap.html`
   §2), not done here.
8. **`breedNth` foal names past foal 1** / **`FamilyTreeScreen` scroll mode** /
   **stats surfaces** / **water-riding feel** / **the epigenome across a
   save-reload** - see `wiki/verification.html`.
9. **Epigenetics follow-ups** - a foal copies a parent's per-allele seed
   **exactly**, with no variation, so a closed line converges on one look;
   and the epigenome lives on the entity, not on `HorseRecord`, so
   `FamilyTreeScreen` draws ancestors from `Epigenome.fromSeed(record UUID)` -
   a plausible stand-in, not the real coat.
10. **Use `Gene.dominance()` beyond the gallery** - the metadata is on every
   gene now (`DominancePattern`), but only `GenotypeCatalog` reads it. Obvious
   next consumers: a punnett/expected-foal display, "carrier" wording in the
   info panel, and `GeneCodeDisplay` deciding what's worth printing.
11. **Cleanups**: rename `DebugPenManager` / `DEBUG_LEVEL` /
   `horsegenetics:debug_pens` to non-"debug" names (needs a save-data
   migration or a one-time reset); fold speed/health into the gene model;
   name-generation rework; real white-fog dimension effects
   (needs a client dimension-effects mixin); the stray `neoforge.mods.toml`
   duplicate.

## License

CC BY-NC 4.0 (see `LICENSE`). Forks/derivatives are welcome without asking
but must credit the original repo and link back, and no portion may appear in
a paid derivative with no free version available. Donations/tips on an
otherwise-free derivative are fine. Before vendoring third-party code, check
its licence is compatible.

## Conventions

- Keep `common/` free of Minecraft imports - a hard rule.
- New version-specific logic goes in `neoforge-26.1.2/`, by concern
  (`client/` / `data/` / `network/` / `server/` / `block/`).
- Flag genuinely unverified API usage in comments the way the existing code
  does - more useful to the next session than silent confidence.
- When you resolve something flagged here as unverified or a known gap,
  update this file in the same change.
- **`README.md` is user-facing only.** No status, architecture, API notes, or
  file listings there. All of that lives in `CLAUDE.md`.
- The breeding / pedigree / horse-record / **stat-inheritance** system is
  documented **only** in `wiki/breeding.html`; **each gene** is documented **only** in
  `wiki/gene-*.html`; the **`runClient` checklist** is **only** in
  `wiki/verification.html`; the **design rationale** is **only** in
  `wiki/philosophy.html` and the **future backlog** **only** in
  `wiki/roadmap.html`; the **coat machinery** is in `wiki/pipeline.html` +
  `wiki/body-space.html`, and the **modder API** in `wiki/modding.html` +
  `wiki/api-reference.html`. Update the relevant file in the same change - a
  pointer from CLAUDE.md is fine, a copy is not.
- **The wiki has one nav.** A new page goes in the `SECTIONS` array in
  `wiki/nav.js` and nowhere else; never hand-write a sidebar into a page.
- **Keep the why out of the backlog.** `wiki/roadmap.html` is work items;
  when a justification there runs longer than a clause it belongs in
  `wiki/philosophy.html` with a pointer back.
- **No legacy / back-compat code.** Dev only, single tester, no saves to keep -
  when a format changes, change it and move on (no genotype-code padding, no
  attachment field fallbacks).

## Ending a session

The routine to run when the owner says **"end the session"** (or "wrap up",
"we're done for today"). It is a *fixed order* - the docs pass comes after the
code is pushed, so it's a review of where the session actually landed rather
than a running commentary written mid-change.

**0. Pre-flight.** `./gradlew :common:test` and `./gradlew
:neoforge-26.1.2:build`. Don't push red. If something fails and can't be fixed
in the time left, still push - but say so in the commit message and put it at
the top of `wiki/verification.html`.

**1. Commit and push the session's code.**

- `git status --short` first and read it. The repo now has a `.gitignore`, so
  `build/`, `.gradle/`, `.idea/` and `run/` stay out of `git add -A` and the
  status should be short enough to actually read. If build or run output *does*
  show up, a pattern is wrong - fix the pattern, don't `git add` around it.
- The owner works directly on **`main`**, which tracks `origin/main`. Commit
  and push there; don't branch.
- One commit for the session's work, with a **descriptive** message: what
  changed and *why*, not a file list. Multi-paragraph is fine and preferred
  when the session did more than one thing. End it with the `Co-Authored-By:`
  and `Claude-Session:` trailers the harness specifies.

**2. Then update the docs to the current state of the program.** Not "what I
changed today" - *what is true now*. Walk all five, in this order, and honour
the doc-split rules under "Conventions":

- **`CLAUDE.md`** - the **status snapshot** first (test count, what compiles,
  what's owner-verified vs built-but-unplayed), then any section the session
  invalidated, then **"Known gaps / next steps"**: delete what got fixed, add
  what got discovered, renumber the list.
- **`wiki/verification.html`** - the `runClient` checklist. Delete items the
  owner confirmed in-game this session (they move to CLAUDE.md's
  "Owner-verified" block, which is the permanent record); add a concrete,
  checkable entry for anything built-but-unplayed, including *what to look at*
  and *where* (which pen, which screen, which key).
- **`wiki/gene-*.html`** - if any gene's alleles, generation function, wild
  frequency, dominance or coat effect moved.
- **`wiki/breeding.html`** - if breeding, pedigree, horse records or stat
  inheritance moved.
- **`wiki/pipeline.html`** / **`wiki/body-space.html`** - if the coat machinery
  or the projection engine moved.
- **`wiki/api-reference.html`** / **`wiki/modding.html`** - if a public
  `common/` type changed shape, or if the gene-authoring contract moved.
- **`wiki/nav.js`** - if any page was added or renamed.
- **`README.md`** - only if the *player-facing* experience changed. It stays
  user-facing: no status, no architecture, no API notes.

**3. Commit and push the doc update as its own commit.** Step 2 always leaves
the tree dirty; a session must not end with unpushed doc changes.

**4. Verify clean.** `git status --short` empty (bar untracked noise you
deliberately left) and `git log origin/main..HEAD` empty. If either isn't, fix
it before stopping.

**5. End the session** with a short terminal summary - what shipped, what's
newly waiting in `wiki/verification.html`, and the one thing the next session
should pick up first. Then stop: no new work, no "while I'm here" refactors.
