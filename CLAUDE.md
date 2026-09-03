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
- **`wiki/gene-creator/`** is the interactive gene editor - now a *folder*, not a
  single page (`index.html` + `creator.css` + `js/*` + generated `assets/` and
  `fixtures/`). It is a **real tool, not a mockup**: it runs a JavaScript port of
  the coat pipeline, previews a gene on a 3D horse over any base coat, and
  exports the JSON the game loads. It does **not** use the wiki's `styles.css` /
  `nav.js` - it owns the whole window.
- **`wiki/gene-format.html`** is the single source of truth for the **data-driven
  gene file format** - the header, the knobs, every mask and every op, and the
  `effects` block's shape. When a mask, an op or an effect verb changes shape,
  update it in the same change.
- **`wiki/gene-effects.html`** is the single source of truth for the **`effects`
  block** - every verb and its parameters, the triggers, the condition flags,
  the execution / merge model, and the **modular contract for adding a new
  effect** (one `AbilityType` declaration; the parser never changes). Update it
  in the same change as any effect verb, flag, or trigger.
- **`wiki/horse-traits.html`** is the single source of truth for the wider
  **trait / effect architecture** - the mostly-unbuilt behaviour system
  (selectors, auras, pools, goals) that `effects` is a first slice of, plus the
  Waterborn worked example and the slice-vs-architecture map. It points at
  `gene-effects.html` for the built reference.
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
- **`wiki/horse-care.html`** is the single source of truth for the **non-genetic
  horse-care systems** - gated healing, bond tiers, herd formation, the shared
  slow tick and the block tags. Update it in the same change as
  `HorseCareHandler` / `BondFollowGoal` / `HorseCareAttachment`.
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
- **`wiki/roadmap.html`** is the **long-range backlog** - the gene wishlist and
  the systems it needs (hard-coded gene priority, the modder-facing
  gene-authoring API, multi-allele loci, non-coat and health genes), each with
  notes on what would have to change - plus the non-gene features (mare milking,
  the custom horse spawner) and the **planned revert of the genotype gallery**
  to random pens. **Only not-yet-done work lives here**: a section is either
  unbuilt or marked *partly built* with just the remainder; anything finished is
  deleted and written up on its own page (`wiki/gene-*.html`,
  `wiki/breeding.html`, `wiki/horse-care.html`, `wiki/pipeline.html`). Section
  numbers are stable (a retired section keeps its slot as a pointer) so
  cross-references don't rot. It is **work items only**; the reasoning lives in
  `wiki/philosophy.html`. Its §21 keeps a **settled** list so a later session
  doesn't reopen those calls - the load-bearing ones: **Mendelian + X-linked +
  Y-linked only** (polygenic cut; a stallion carries a real `X` *and* `Y`);
  **aging out entirely** (flea-bitten grey / melanoma cut with it); health =
  fewer hearts + lethal foals behind a default-on config; one gene per physical
  locus (KIT/MITF/MATP) with the gene's own `tint` handling every allele combo,
  **no dominance-per-pair table**; founder frequency declared **per genotype**
  (percentages, auto-normalised) not per allele, and a separate per-gene
  **chaos-carrot chance function** of the same shape (§14.1/§19); the genotype
  stored as a **list keyed by gene**, each gene carrying its order number, so
  adding/removing a gene just triggers a coat regen (no padding, no back-compat);
  signed-unclamped `int` phase-3 with context-aware genes intended; **every time-gate is once per
  24 000-tick MC day**; herd comfort buff stays passive regen (no stamina); the
  villager **exists**; the gene DB is a plain progressive record (no fog of war,
  nothing hidden); a magic carrot's timing = a vanilla golden carrot (temporary
  window, coloured heart particles per type); mutinogenic re-rolls every allele
  copy a fed parent passes on; the seed jar stores enough sire data for the
  family tree; sheared look = render-layer overlay; magic-carrot rarity defaults
  to the gold-ingot tier and a gene can opt out. **Still open**: custom-entity
  subclass vs attachments (§11, owner wants to discuss), how a cutie mark is
  chosen, the 1.12.2 backport surface.
- **New with the wiki conversion**, and not derived from any old markdown -
  keep them current too:
  - **`wiki/genetics-model.html`** - the Mendelian model as implemented:
    alleles as objects, the code string, the **combination table**
    (`Expression`) and `FounderTable` that replaced dominance, the per-allele
    epigenome, `GenotypeCatalog`, and what the texture key captures.
  - **`wiki/pipeline.html`** - the three-phase coat pipeline in full
    (`PigmentField` / `ColorField` / `GradientLut` / `CoatTextureId` / the
    golden test). This is where the coat machinery is documented now; CLAUDE.md
    keeps a summary.
  - **`wiki/body-space.html`** - `HorseSkinGeometry`, `CoatRegions`,
    `BodyNoise`, `BodyStripes`, `PatchNoise`.
  - **`wiki/modding.html`** + **`wiki/api-reference.html`** - the
    **modder-facing** docs: how to write a gene (two worked walkthroughs, the
    allele rules, the determinism contract, the pitfalls table) and the
    class-by-class abstraction reference. When a public type in `common/`
    changes shape, update `api-reference.html` in the same change.

## Status snapshot (keep this current)

- **Built 2026-09-03, NOT yet play-tested: the combination-table rewrite
  (roadmap Tier 1 §2, Tier 2 §5.1/§5.2).** `DominancePattern` is **deleted**.
  A gene no longer declares a dominance label; it declares an **`Expression`
  per distinct outcome** - id, display name, a human-readable description, a
  `wildType` flag ("this combination changes nothing"), a `masks` flag, a
  `deterministic` flag and **its own paint function** - plus one function
  `expressionOf(AllelePair)` mapping any combination to one of them. Several
  pairs sharing an expression *is* what "dominant" meant; only the
  double-variant landing on a non-wild-type outcome *is* what "recessive"
  meant; two variant alleles each with an outcome plus a third for the pair of
  them *is* codominance. Works for any number of alleles with no special case.
  Landing with it:
  - **`Gene.randomPair(rng)` → `FounderTable`** - a weight per allele
    *combination* as percentages, sparse (an unlisted combination never occurs),
    normalised-with-a-warning, **one `nextFloat()` per gene per founder**, with a
    `FounderContext` for a genome-aware distribution that *throws* if asked
    about a gene not yet rolled. `FounderTable.hardyWeinberg(variant, baseline,
    p)` is the convenience that reproduces the old "1 in N per allele" numbers.
    The **test gene** is why this matters: 25% of founders are `T/t` and **none**
    are `T/T`, which no per-allele frequency can express.
  - **Cream + pearl → `MatpGene`** (`horsegenetics.matp`, priority 40) - the
    proving case and the mod's first three-allele locus: `Cr`/`prl`/`N`, six
    combinations, four outcomes. Kills the impossible `Cr/Cr`-and-`prl/prl`
    genotype; `coat/pattern/CreamPearlDilution` is gone (the dose table is a
    six-row `switch` on the gene). **17 built-in genes**, 19 in-game.
  - **`Allele` lost `visible` / `deterministic`** (both are properties of a
    *combination*) and **gained an `order`** - its index in `alleles()`, which
    is a slot order for `AllelePair`'s canonical form and **not** dominance.
    `Gene.precedence` and `AllelePair.dominant()` are gone.
  - **`Gene.wildType()` → `defaultAllele()`** - what a code segment-less gene
    reads as, a *parsing* default. The word "wild type" now means an expression.
  - **The JSON gene format went to `"format": 2`** - `dominance` and `wildOdds`
    replaced by an `expressions` table (each entry with `when`, `wildType`,
    `masks`, `description`, its own `layers` and its own `effects`) and a
    `founders` table. The parser proves the table is **total and unambiguous**
    over every `n(n+1)/2` combination: a gap, an overlap, or an unreachable
    catch-all is a load error naming the offending combination. Both shipped
    genes and all six examples rewritten.
  - **The epigenome moved onto `HorseRecord`** - see the separate bullet below.
  - `GenotypeCatalog.distinctPairsOf` now groups pairs by the **expression**
    they land on, with **every wild type counting as one group** ("changes
    nothing" is one *look*). Exact rather than an approximation, and it shrank
    the gallery from 331 778 pens to **98 306** (splash 3→2, MATP 9→4);
    `totalGenotypes()` is **258 280 326**.
  - `coat-golden.txt` regenerated - **every coat changed**, because founder
    draws and the gene set both moved. `:common:test` **213 green**,
    `:neoforge-26.1.2:build` green, parity **3 832 checks / 48 cases** (up 48:
    the fixtures now pin which expression each combination resolves to),
    `runServer` boots clean (`19 segments`, `loaded 2 data-driven gene(s)`).
  - **Old saves will not reproduce their horses** - the code lost a segment and
    the founder draw changed. Dev only; start a fresh world.
  - Docs: `wiki/genetics-model.html#combinations`, `wiki/gene-format.html`,
    `wiki/gene-matp.html`, `wiki/modding.html`, `wiki/api-reference.html`.
    Checklist: `wiki/verification.html` §0.
- **Built 2026-09-03, NOT yet play-tested: the epigenome lives on
  `HorseRecord`.** `HorseRecord` gained `epigenomeCode` beside `geneticCode`
  (plus `genotype()` / `epigenome()` / `genome()` / `hasGenome()` /
  `withGenome()`), and `data/HorseCoatAttachment` + the `horsegenetics:horse_coat`
  attachment are **deleted**. Both are heritable facts assigned once at birth, so
  storing the genotype in two places was one fact twice - and keeping the
  epigenome *off* the record is why `FamilyTreeScreen` had to invent an
  ancestor's coat from its UUID. It now draws **the real coat** (closing old
  known gap #9's second half). `HorseRecords.newFounder` rolls the whole genome;
  `HorseBreedingHandler` / `StallionSeedJarHandler` read the parent genome off
  the record; the record attachment gained `copyOnDeath`. `HorseRecordCodecs`
  serialises `epigenome_code` as an optional field defaulting to `""`, which is
  the `hasGenome()` sentinel.
- **Built 2026-09-03, owner-verified: the earlier data-model rewrite (roadmap
  Tier 1, §2).** Every `Gene` now declares `int priority()`; `codeOrder()` /
  `naturalOrder()` / `magicalOrder()` are all *derived* by one sort on
  `(priority, key)` over built-ins + `SpecGene`s together (no hand-written
  lists, loaded genes interleave by priority). The genotype / epigenome code
  strings became **gene-keyed and tolerant** (`<geneKey>=<a>/<b>`; missing gene
  = wild type, unknown gene = dropped, `""` = wild type) - see "The genetics
  model" below. `coat-golden.txt` regenerated: every **deterministic** coat is
  byte-identical (the pipeline is untouched); non-deterministic rows shifted
  because `Epigenome.random` / `fromSeed` now draw per-gene seeds in the new
  `codeOrder()` - a *stored* epigenome code round-trips unchanged, only the
  seed-derived stand-in moved. `:common:test` 195 green, `:neoforge:build`
  green, `runServer` boots clean (`20 segments` with the two shipped spec
  genes). NeoForge needed **no** source changes - it delegates all code
  parsing to `common/`. Closes old known-gap #18 (`GeneCodeDisplay` now derives
  its gene list, so spec genes show). **Owner-verified in-game 2026-09-03:**
  wild horses spawn and render correctly, right-click paper genome dump works.
  Still unconfirmed: bred foal, seed-jar round-trip, a spec gene actually
  showing in the display (needs a horse carrying Suntouched/Waterborn) -
  `wiki/verification.html` §0.
- **`common/`** - compiles; **213 JUnit tests pass** (`./gradlew :common:test`).
  Covers `genetics/` (allele/gene model - **17 genes**: the 15 natural ones
  (extension, agouti, champagne, splash, grey, **MATP** (cream + pearl, three
  alleles), **dun**, **silver**, **mushroom**, **roan**, **tobiano**, **frame**,
  **sabino**, plus the masking dominant white), and magic zebra + pink hair;
  `Genotype` code round-trip, breeding, the `Epigenome` / `Genome` per-allele
  epigenetics + priority tie-break, `GenomeSample` - a genome detached from a
  horse, for the stallion seed jar - `Expression` + `FounderTable` + the
  `GenotypeCatalog` reduction of 258 280 326 genotypes to 98 306 distinct
  coats), `coat/` + `coat/pattern/` (the
  three-phase pipeline - `CoatTextureComposer`, `PigmentField`, `ColorField`,
  `GradientLut`, `BayCoat`, `GreyCoat`, `BodyStripes`, `CoatRegions`, the pure
  gene hooks, the `coat-golden.txt` byte-identity net, `CoatTextureId`
  texture-id injectivity),
  `coat/skin/` (`HorseSkinGeometry`), `name/` (`breedNth`),
  `horse/` (pedigree + `HorseStats` -> `wiki/breeding.html`), and
  `genetics/spec/` (the **data-driven gene** format: `GeneSpec`, `Json`,
  `GeneSpecParser`, `SpecSchema`, `SpecValues`, `SpecGene`, `GeneSpecLoader`,
  plus `coat/pattern/SpecPainter`; and the **gene `effects`** path -
  `GeneAbility`, `AbilityType`, `SpecAbilities` - the Minecraft-specific
  things a data-driven gene does beyond the coat - see below).
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes; only two `getGuiLeft/getGuiTop`
  deprecation warnings) against the real NeoForge `26.1.2.100` SDK.
- **`runServer`** - boots clean to `Done (...)! For help`; all dimensions,
  attachments, SavedData, payloads (incl. `spawn_custom_horse`), the
  `hay_portal` block + block entity, the `custom_horse_spawn_egg` item, and
  the `ClientConfig` all register with no errors. Re-verified 2026-09-02 with
  the shipped **Waterborn + Suntouched** genes loaded (`[genes] loaded 2
  data-driven gene(s): example.suntouched, example.waterborn ... 13 segments`)
  and again after the **gameplay-layer items** + the `stored_genome` data
  component were added (`Loaded 1531 recipes`, 16 new shapeless recipes among
  them; the component registers with no error - the 26.1.2 path the roadmap
  flagged as unverified now works). Re-verified again 2026-09-02 after the
  **horse-care** attachment (`horse_care`), the `care_sync` payload and the
  `horse_water` / `horse_food` block tags were added - still boots clean to
  `Done`.
- **`runClient`** - actively play-tested over the 2026-08-30, 2026-09-01 and
  2026-09-02 sessions; see below.

- **Built 2026-09-02, NOT yet play-tested:** the **seven remaining visual
  natural genes** (roadmap §§4.1-4.2), all hand-written `Gene`s in
  `common/genetics/genes/`:
  - **dilutions** - `DunGene` (`D`/`d`, DOMINANT: mild body dilution +
    primitive markings - a dorsal stripe + leg barring that skip the dilution,
    via new `CoatRegions.dorsalStripe` / `legBar`), `SilverGene` (`Z`/`z`,
    DOMINANT: eumelanin-only, chocolate body + flaxen mane/tail, chestnut
    unaffected - runs right after agouti in `naturalOrder()`), `MushroomGene`
    (`Mu`/`mu`, RECESSIVE: pheomelanin-only, chestnut -> sepia).
  - **white patterns** (all non-deterministic, epigenetic seed on the variant
    copy) - `RoanGene` (`Rn`/`rn`, DOMINANT: near-binary white-hair dither,
    density tapering back-to-front so it feathers into the solid face),
    `TobianoGene` (`To`/`to`, DOMINANT: big crisp patches from a topline-biased
    `PatchNoise.field`, white legs), `FrameGene` (`Ov`/`ov`, DOMINANT for the
    coat - **`Ov/Ov` lethal white deliberately not modelled**: bold jagged-edged
    patches in an absolute-Y flank band on BODY/NECK + a bald face), `SabinoGene`
    (`SB1`/`sb1`, INCOMPLETE_DOMINANT and **it reads its own dose** - the
    counter to splash's open issue: dose 1 = jagged stockings + belly + blaze,
    dose 2 = "sabino-white").
  - New helper `coat/pattern/PatchNoise` (warped 3-octave fractal `field` +
    2-octave `fbm2`) - the spotting genes need patch fields that cross seams
    smoothly *and* aren't one lattice cell wide; single-octave `BodyNoise`
    gridded up into visible squares. `CoatRegions.dorsalStripe` /
    `legBar` are the dun primitive-marking helpers.
  - **Reworked 2026-09-02 after owner feedback on the first bakes:** grullo
    was warm-brown not mouse-grey (dun's `keepRed` now scales to ~0 by the
    texel's black content, so a black base lands on the LUT's neutral column);
    mushroom read as no-op (`keepRed` 0.44 -> 0.12); silver's mane came out
    chestnut not flaxen (mane now pulls red down too); roan made hard 100%
    white pixels and had a hard edge at the face (now near-binary flecks +
    a smooth rear->front density falloff); tobiano was ~80% white with gold
    fringing (higher threshold, hard-binary decision - a half-scaled black
    texel reads gold on the LUT); frame produced almost nothing (per-part
    `sideWeight` left the barrel untouched -> absolute-Y band); sabino showed
    axis-aligned squares (`PatchNoise` instead of raw `BodyNoise.value`).
  - Registry was 18 genes then; it is **17** now that cream and pearl merged
    (19 in-game with the two shipped spec genes). `GeneCodeDisplay`'s trailing
    order gained all seven. `coat-golden.txt` regenerated (10 new cases).
    `GenotypeCatalog` blew up to 331 778 distinct coats; the combination-table
    rewrite brought that back to **98 306** (`totalGenotypes()` 258 280 326),
    so the gallery corridor is ~344 000 blocks of lazily-built pens - still an
    argument for roadmap §9. The
    exhaustive `2^genes` / `3^genes` tests (`CoatTextureIdTest`,
    `CoatTextureComposerTest`'s combo sweep) were converted to **seeded
    sampling** - enumerating them is no longer tractable. Sample bakes
    (`:common:bakeCoatSamples`, 12 new) look right after the rework; nothing
    seen in-game - checklist in `wiki/verification.html`.

- **Built 2026-09-02, NOT yet play-tested:** the **gameplay-layer items**
  (roadmap wiki §§11-19, first slice). 19 new `Item`s in `item/ModItems`:
  `horse_hair` + `horse_hair_bundle` (4 hair &harr; 1 bundle, roadmap §12.2's
  first two rungs), four breeding carrots
  (`mutinogenic`/`chaos`/`stabilizer`/`magnifier`), one generic
  `magic_gene_carrot` (per-gene parameterisation needs a data component -
  deferred), `placeholder_gene_book` (literal name "PLACEHOLDER GENE BOOK",
  stands in for the research paper), `empty_seed_jar` + `stallion_seed_jar`
  (both `SeedJarItem`, tooltip from the `stored_genome` component), four tickets,
  three whistles, and `stall_sign` + `bound_stall_sign`. The 17 non-sign items
  have **owner-supplied textures** (tickets share one, whistles share one); the
  two stall signs borrow `minecraft:item/oak_sign` - per-tier / real art is a
  follow-up in `wiki/verification.html` §15. New `item/ModCreativeTabs`
  registers one **Horse Genetics** tab holding all of them (20 with the custom
  spawn egg, which also still shows in vanilla Spawn Eggs). The
  **dev test-world hotbar** (`server/DebugTestWorldHandler`) gives the spawn egg
  on slot 0 (tools shifted to 1-6) and one of every new item in the main
  inventory. Recipes are datapack JSON under `data/horsegenetics/recipe/`
  (singular). **Tickets are inert** (stall-teleport needs stall blocks that
  don't exist); the carrots do nothing yet. The **whistles work** - see below.
- **Built 2026-09-02, NOT yet play-tested:** the **whistles** (`item/WhistleItem`).
  Right-click anywhere to teleport every tamed horse you own within range
  (basic 16 / golden 32 / echo 64 blocks), same dimension, not ridden, to a
  grid of spots beside you; ~3 s use cooldown, a chime, a chat count. Leashed
  horses are unleashed and come; a horse already within 3 blocks or one you are
  riding is left alone. This is the owner's "area recall" reading of roadmap
  §11; the bond-gated version waits on bond, and "what echo adds" beyond range
  is still open. Checklist `wiki/verification.html` §17.
- **Built 2026-09-02, NOT yet play-tested:** the **stall system** (roadmap §11).
  A new `bound_horse` data component (`data/BoundHorse`), a `stall_sign` /
  `bound_stall_sign` item (`item/StallSignItem`, texture borrowed from
  `minecraft:item/oak_sign`), a server-global `data/StallData` SavedData of
  `data/StallRecord`s (one per bound horse), a flood-fill `server/StallDetector`,
  and `server/StallDebug` (the "debug overlay" - a particle wireframe + chat
  summary). Flow: right-click a horse with a blank `stall_sign`
  (`server/StallSignHandler`) -> it becomes a `bound_stall_sign` carrying that
  horse's UUID + name; right-click the **outside** face of a wall with the bound
  sign -> `StallSignItem.useOn` drops a real `oak_wall_sign` with the horse's
  name and flood-fills the block **behind** that wall (this layer ± 1, air only,
  &le; `MAX_BLOCKS` 512) - an enclosed area becomes that horse's stall, its
  outline flashed with `HAPPY_VILLAGER` particles. Breaking the sign
  (`BreakBlockEvent`) releases the stall. Dev keybind **F7**
  (`key.horsegenetics.show_stalls` -> `RequestStallHighlightPayload`, dev-gated
  in `ModNetworking`) re-flashes every stall's outline near the player + prints
  a summary. **No teleport-to-stall yet** (that's the tickets, still inert); no
  client-side persistent wireframe (particles only). Checklist
  `wiki/verification.html` §18.
- **Built 2026-09-02, NOT yet play-tested:** the **stallion seed jar** first
  slice (roadmap §15.1). New `common/genetics/GenomeSample` (a `Genotype` +
  `Epigenome` detached from a horse as code strings, with `breedInto(mareGenome,
  rng)`); new `data/StoredGenome` + `data/ModDataComponents` registering the
  `horsegenetics:stored_genome` data component (persistent `Codec` +
  networked `StreamCodec`); `server/HorseBreedingHandler` refactored so its
  foal-building body is a reusable `applyBredFoal(...)`; new
  `server/StallionSeedJarHandler` - right-click a tamed adult **stallion** with
  an `empty_seed_jar` &rarr; a `stallion_seed_jar` stamped with his genome, sex,
  UUID, name, speed/health; right-click a tamed adult **mare** with a filled jar
  &rarr; a foal bred immediately through `applyBredFoal` from her live genome +
  the jar's stored one, jar consumed, mare put on the vanilla breeding cooldown.
  **Both ends require the horse to be in breeding mode** (`isInLove()` - fed a
  carrot/apple, works in creative); the op consumes that love state. The jar
  **transforms in the player's hand** (creative included). **No** real
  breeding-carrot gate (vanilla love is the stand-in), **no** gestation state
  (foal is immediate). Partly owner-confirmed 2026-09-02 (collection + tooltip);
  `:common:test` 194, `:neoforge-26.1.2:build`, `runServer` all green.

- **Built 2026-09-02, NOT yet play-tested:** the **horse-care systems** -
  gated healing (roadmap §7.2) plus bond + herds (§13), the first slice of
  both, sharing **one** slow tick as the roadmap demands. All in
  `neoforge-26.1.2/`, nothing in `common/`:
  - `data/HorseCareAttachment` - a `copyOnDeath` attachment
    (`horsegenetics:horse_care`) holding `bond` (0-100), an `Optional<UUID>`
    herd id, the `bondToday`/`dayStamp` daily-cap pair, a `bondTicks`
    fractional accumulator and a `togetherTicks` herd-formation counter;
    `behaviourTier()` (0 vanilla / 1 face / 2 approach / 3 follow).
  - `server/HorseCareHandler` - `EntityTickEvent.Post`, every **30 ticks**,
    staggered by `tickCount + entityId`. **Gated healing:** only for a hurt
    horse, a 3-block scan for a block in `#horsegenetics:horse_water` (or any
    `#minecraft:water` fluid) **and** one in `#horsegenetics:horse_food`;
    `heal(1)` + `HEART` particles per hit (`2` if in a herd). **Bond:**
    proximity (~+0.5/min), riding (~+1/min), feeding (+2, on
    `EntityInteract` with any `isFood` stack from the owner); cap **+15 per
    24 000-tick day**; foal-in-herd doubles the accrual. **Herds:**
    `togetherTicks` +30 with company / -30 alone; ≥ 12 000 → join a
    neighbour's herd or mint a `UUID` and pull in herdless neighbours; decays
    to 0 → leave. Attachment written back only on change; sync packet only
    when bond / in-herd changed.
  - `server/BondFollowGoal` - one `Goal` added at priority 4 to every `Horse`
    on join (dedup via `goalSelector.getAvailableGoals()`), inert below tier 1
    / while leashed / while ridden; tier 1 look-only, tier 2 paths only if a
    route exists, tier 3 follows persistently. Real pathfinding, never a
    teleport.
  - `network/HorseCareSyncPayload` → `client/ClientHorseCareCache` (cleared on
    `LoggingOut`); `client/HorseScreenHooks` shows a `bond N  <tier>  • herd`
    line. Care sync also sent on `StartTracking`.
  - Two block tags: `data/horsegenetics/tags/block/horse_water.json`,
    `horse_food.json`.
  - `:common:test` (195), `:neoforge-26.1.2:build`, `runServer` (boots clean,
    tags load with no error) all green. Checklist:
    `wiki/verification.html` §0; machinery: `wiki/horse-care.html`.
  - **Not in this slice:** milking (§7.1), shearing/sleeping bond (shearing
    unbuilt), a stored herd alpha, any stamina resource.

- **Owner-verified in-game (2026-09-03):**
  - **The data-model rewrite** (gene priority + derived orderings + gene-keyed
    tolerant code strings): wild horses spawn and render their correct coats -
    no regression from the new `codeOrder()` / code format - and the
    right-click **paper genome dump** works. Not yet checked: a bred foal, the
    stallion-seed-jar round-trip, and a spec gene (Suntouched/Waterborn)
    appearing in the short genome display.

- **Owner-verified in-game (2026-09-02):**
  - **Custom horse spawn egg** end to end - the egg, the age/sex/genome editor,
    and the spawn (see the status bullet below for the two follow-up fixes).
  - **Waterborn's coat**: the neon-blue streaks in the mane and tail render.
  - **Waterborn's particle trail**: the blue dust at the hooves on the move
    works. `walk_on_water` and the tamed-mare milking are **not** yet
    confirmed - `wiki/verification.html` §13.
  - **Suntouched's `glow`**: the emissive gold mane + tail render (bright in the
    dark), the gold-dust `emitter` shows, and the horse **lights the area around
    it** (the trailing `minecraft:light` block). Light cleanup on death/unload,
    the foal case, and hard base coats (cremello, dominant white) are **not** yet
    confirmed - `wiki/verification.html` §13.
  - **Stallion seed jar** - collecting from a stallion and impregnating a mare
    produces a real bred foal, and the jar carries the **correct** genome. Two
    bugs found and fixed the same day: the jar didn't visibly change in creative
    (now transforms in hand), and it worked with the horse not in breeding mode
    (now both ends require `isInLove()`). The **short genome display string**
    (info panel / tooltip) is **missing the magical genes** - a
    `GeneCodeDisplay` bug, data is fine (open issue in `wiki/verification.html`).
    The breeding-mode gate itself is built-but-unretested.

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

- **Built 2026-09-02, NOT yet play-tested:** **data-driven genes** and the
  **rebuilt gene creator**. A gene that fits the format is now a **JSON file**
  dropped in `config/horsegenetics/genes/` - no Java, no rebuild - and
  `wiki/gene-creator/` is the tool that writes it, previewing the gene on a 3D
  horse over any of 15 base coats before you export. Nothing about the eleven
  built-in genes changed: no gene file ships by default, so the registry, the
  genotype code, the gallery numbers and `coat-golden.txt` are all untouched.
  See "Data-driven genes" below; in-game checklist in `wiki/verification.html`.
- **Built 2026-09-02, partly play-tested:** **gene `effects`** - a data-driven
  gene can carry Minecraft-specific behaviour alongside its coat `layers`.
  Six verbs (`traversal`, `attribute`, `emitter`, `mob_effect`, `yield`,
  `glow`), each with an optional boolean `when` and a `minDose`. `common/`
  parses and validates all six (`GeneAbility` records / one `AbilityType`
  per-verb declaration / a generic `GeneSpecParser.readAbility` / `SpecAbilities`,
  unit-tested); the NeoForge translator
  (`server/GeneAbilityHandler`, `server/GeneYieldHandler`) executes
  `traversal` + `emitter` + `mob_effect` + `yield` + `glow`. **`attribute` is
  the one verb parsed but not executed yet** (logged once). `mob_effect`
  resolves the id against the registry and keeps a hidden/ambient effect topped
  up on the `self` / `rider` target on its `refresh` beat (duration
  `refresh + 20`, so a `when` going false lets it decay - no explicit removal).
  **`glow`** has two independent halves: `light` (0-15) maintains a trailing
  `minecraft:light` block server-side (moved on block change, cleared on
  `EntityLeaveLevelEvent` / `when` false, skipped in the gallery dimension,
  air-only placement), and `parts` (a body-region list - a new `AbilityType`
  `Kind.PARTS` parsed through `PartGroups.expand`) drives
  `client/EmissiveCoatLayer`, which redraws those coat parts full-bright over
  the base coat via a second baked texture. `walk_on_water` is an approximation
  (surface buoyancy, not a solid plane). Two shipped genes exercise it:
  **Waterborn** (`traversal` + `emitter` + `yield`) - neon-blue mane/tail
  streaks + blue particle trail **owner-confirmed in-game (2026-09-02)**,
  `walk_on_water` and the tamed-mare milking not yet - and **Suntouched**
  (`glow` + `emitter`) - light 12 + emissive gold mane + a gold-dust aura, the
  emissive mane and the area lighting both **owner-confirmed in-game
  (2026-09-02)**.
  Reference: `wiki/gene-effects.html` (verbs + the "add an effect" contract),
  `wiki/gene-waterborn.html` + `wiki/gene-suntouched.html` (the genes),
  `wiki/horse-traits.html` (the wider architecture); checklist
  `wiki/verification.html` §13.
- **Play-tested 2026-09-02, works:** the **custom horse spawn egg**
  (`item/ModItems` -> `CUSTOM_HORSE_SPAWN_EGG`). A plain `Item` reusing the
  vanilla `minecraft:item/horse_spawn_egg` texture (identical icon), in the
  Spawn Eggs creative tab. Right-clicking it (`client/CustomHorseSpawnEggClient`
  cancels the interaction) opens `client/CustomHorseSpawnScreen`: pick **age**
  (Adult/Foal), **sex** (Mare/Stallion), and a **genome** - starts as a bare
  `EEaa` (extension `E/E` + agouti `a/a`, both required and not removable),
  `+` lists the genes not yet added, each row has two allele buttons that cycle
  through `gene.alleles()` (fine for 3+ alleles); a newly added gene defaults
  to **homozygous for its first non-wild-type allele** (the one that does
  something), not wild/wild. **Spawn** sends `network/SpawnCustomHorsePayload` -> the server
  parses the code, spawns a `Horse` at the player's look target, and
  `HorseRecords.apply(newFounder(..., sex, code))` **before** `addFreshEntity`
  so `HorseGeneticsEventHandler` keeps the genome (epigenome still rolled
  fresh). Non-creative players lose one egg. All custom horses get default
  (not randomised) speed/health - fine for a genome-test tool. Owner-tested:
  the egg + editor + spawn all work; two follow-ups fixed the same day - a
  full-screen dim in `extractRenderState` was drawing over the buttons
  (widgets render *during* `super.extractRenderState`, so the backdrop is now
  narrow strips in the gaps), and the gene picker + genome list could run off
  the bottom of a short screen and hide the last gene (now bounded multi-column
  in the picker, mouse-wheel scroll in the editor). A gene only shows in the
  list (and can only be spawned) if it is actually registered.
- **Behaviour change 2026-09-02: Waterborn + Suntouched ship loaded.** To make
  the data-driven-effects work testable in-game, `example.waterborn` and
  `example.suntouched` are registered via a **classpath gene index** -
  `neoforge-26.1.2/src/main/resources/horsegenetics/genes/index.json`
  (`["suntouched.json", "waterborn.json"]`) + the two files beside it, which
  `GeneSpecLoader.fromClasspath()` picks up in the mod constructor. These are the
  **first (and so far only) gene files to ship**, breaking the "no gene ships by
  default" invariant on purpose (the owner OK'd it): the in-game genotype code is
  now **19 segments** (13 at the time), `GenotypeCatalog`/the gallery are ~**4x** (each shipped
  `DOMINANT` two-allele gene doubles them), and shorter saved horses won't parse.
  **`:common:test` is unaffected** - the index lives in the neoforge module's
  resources, not on the `common` test classpath, so `Genes` stays at 11
  built-ins there (17 now) and `coat-golden.txt` + `SpecGeneTest`'s `BUILT_IN_GENES`
  still hold. The horse dimension will be overhauled later regardless.
- **Built 2026-09-02, `glow` owner-confirmed in-game:** **Suntouched**
  (`example.suntouched`, allele `Sntch`/`n`, DOMINANT magical, wildOdds 128,
  priority 210), plus the **`mob_effect`** and **`glow`** verb translators
  (`mob_effect` has no shipped user and is unverified). Suntouched is a spec gene
  shipped as its own file: one deterministic coat layer (`PARTS` on `HAIR` x
  `TOWARD` gold `#ffcf47` at 88%) plus an `effects` block of `glow`
  (`light: 12`, `parts: ["HAIR"]`) and an `emitter` (gold `#ffcf47` dust,
  `interval` 6, `body` anchor). It is the worked example for `glow`:
  `server/GeneAbilityHandler.reconcileGlow` maintains the light block and
  `client/EmissiveCoatLayer` + `GeneticCoatTextureFactory.getOrCreateEmissive`
  draw the full-bright mane. `mob_effect` was wired in the same pass
  (`applyMobEffect`) but nothing shipped uses it now. See
  `wiki/gene-suntouched.html`; checklist `wiki/verification.html` §13.
- **Docs 2026-09-02, no behaviour change:** the **`Docs/*.md` -> wiki
  conversion**. All five markdown docs are gone; their content lives in
  `wiki/*.html` (see the Docs-split section above), the four Javadoc comments
  that named them were repointed, and `Gene Dict.md` was split into **a page
  per gene** - all **11** now documented, where only 4 were before (and two of
  those pointed at a `CreamPearlGene.java` that has never existed). Two pages
  are new rather than converted: **`wiki/modding.html`** (how to write a gene)
  and **`wiki/api-reference.html`** (class abstractions), which is the
  modder-facing documentation `wiki/roadmap.html` §3 assumed would exist.
  `wiki/nav.js` builds every sidebar from one array, so a new page is one line.
  **No Java behaviour changed** - only Javadoc text - and `:common:test` (153)
  and `:neoforge-26.1.2:build` are both green. Nothing to play-test.
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
  the horse dimension - one pen per visually distinct genotype (98 306 of
  258 280 326 as of 2026-09-03),
  per-pen genotype signs, the entrance tally sign, the per-gene distinctness
  metadata (`Gene.dominance()` then, the expression table now),
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
  - `genetics/` - `Gene` + `Allele` + `AllelePair`, `Expression` (one outcome a
    gene can produce, with its own paint function - what replaced
    `DominancePattern`), `FounderTable` + `FounderContext` (the wild-population
    weight per allele combination - what replaced `Gene.randomPair`),
    `Genotype` (+ `breedWith`), `Epigenome` /
    `AlleleEpigenetics` (the priority + seed on each allele copy), `Genome`
    (the two together, and the breeding that keeps them aligned),
    `GenomeSample` (a `Genome` frozen to code strings and taken off the horse -
    what the stallion seed jar carries; `breedInto(mareGenome, rng)` runs the
    ordinary draw), `CoatPhenotype`, `GeneticCodeCombiner`.
  - `coat/` - `CoatData`, `CoatGenerator`; `coat/pattern/` holds the
    three-phase pipeline (`CoatTextureComposer`, the `PigmentField` /
    `ColorField` accumulators and their read-only `PigmentView` / `ColorView`
    faces) and the reusable body-space noise: `BodyNoise` (single-octave
    value + Worley), `BodyStripes` (X-oriented stripe field) and `PatchNoise`
    (warped 3-octave fractal for white-spotting patches - `field` + `fbm2`).
  - `name/` - `HorseNameGenerator` + `HorseNames` (`breed` = one-half-each;
    `breedNth` = varied by a pairing's foal count) + word tables under
    `src/main/resources/horsegenetics/names/`.
  - `horse/` - the pedigree domain model (`Sex`, `HorseRecord`,
    `HorseDatabase`, `InMemoryHorseDatabase`) and `HorseStats` (foal stat
    roll) -> `wiki/breeding.html`.
  - `genetics/spec/` - the **data-driven gene** path: `GeneSpec` (the format as
    records), `Json` (a hand-rolled parser - `common/` takes no dependencies),
    `GeneSpecParser`, `SpecSchema` (the one declaration of what each mask and op
    accepts), `SpecValues` (the per-horse knob draw), `SpecGene` (a `Gene` that
    reads a spec), `GeneSpecLoader`, and the two dev tools `SpecFixtureTool` /
    `CreatorAssetTool`. The painting is `coat/pattern/SpecPainter`.
  - `Rng` - the randomness seam (`nextFloat` / `nextBoolean` /
    `nextInt(bound)` / `nextLong`), implemented by `NeoRng` (wraps
    `RandomSource`) and, in tests, `FakeRng`.

  This is the part that survives a version port unchanged. If you want to
  import anything Minecraft-related here, stop.
- **`neoforge-26.1.2/`** - everything Minecraft-specific, by concern:
  - `client/` - renderer, texture compositing (incl. `EmissiveCoatLayer` for a
    `glow` gene's full-bright coat parts), client caches, the inventory
    hooks + `FamilyTreeScreen`, keybind, lifecycle cleanup.
  - `data/` - Data Attachments. **Two** on a horse: `horsegenetics:horse_record`
    (a `HorseRecord`, which carries the whole genome - genotype *and*
    epigenome - since 2026-09-03; there is no separate coat attachment) and
    `horsegenetics:horse_care` (`HorseCareAttachment`, the non-genetic bond /
    herd / daily-cap state). Plus the ancestry `SavedData`, codecs;
    `ModDataComponents` (the item
    data components: `stored_genome` / `StoredGenome` on the stallion seed
    jar, `bound_horse` / `BoundHorse` on a bound stall sign); and `StallData`
    / `StallRecord` (server-global SavedData of assigned stalls).
  - `network/` - custom payloads + `ModNetworking`.
  - `server/` - event handlers, the horse-dimension builder, the portal
    manager, the record adapter (`HorseRecords`); `HorseBreedingHandler`
    (natural breeding + the shared `applyBredFoal`), `StallionSeedJarHandler`
    (seed collection + mare impregnation, reusing `applyBredFoal`), the
    stall trio `StallSignHandler` (bind / sign-break cleanup) + `StallDetector`
    (the enclosed-area flood-fill) + `StallDebug` (particle-outline overlay),
    and the **horse-care** pair `HorseCareHandler` (the one 30-tick scan for
    gated healing + bond + herd formation) + `BondFollowGoal` (the single
    bond-tier AI goal). See `wiki/horse-care.html`.
  - `block/` - `ModBlocks` + `HayPortalBlock` (the only registered block),
    `ModBlockEntities` + `HayPortalBlockEntity` (drives the animated
    `hay_portal.png` slab renderer).
  - `item/` - `ModItems` (the **custom horse spawn egg** - a dev tool, a
    reskin of the horse spawn egg that opens a genome/age/sex editor first;
    the editor screen is `client/CustomHorseSpawnScreen`, the spawn itself
    goes through `network/SpawnCustomHorsePayload` - plus the 17
    **gameplay-layer items**, roadmap §§11-19 first slice; the two
    `SeedJarItem`s, the three `WhistleItem`s and the two `StallSignItem`s have
    behaviour, the rest don't yet), `SeedJarItem` (tooltip), `WhistleItem`
    (`use` -> recall your tamed horses in a radius), `StallSignItem` (`useOn` ->
    place an oak wall sign + flood-fill the stall behind it), and
    `ModCreativeTabs` (one **Horse Genetics** tab). Recipes are datapack JSON
    under `resources/data/horsegenetics/recipe/`.

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

# gene-creator tooling (see "Data-driven genes")
./gradlew :common:bakeSpecFixtures   # what the real Java spec engine produces
node wiki/gene-creator/tools/check-parity.mjs   # ...and does the creator's JS agree?
./gradlew :common:bakeCreatorAssets  # regenerate the creator's inlined textures + examples
```

**Run the parity check whenever you touch `SpecPainter`, `SpecSchema`,
`BodyNoise`/`BodyStripes`, or any of `wiki/gene-creator/js/`.** It is the only
thing standing between the creator and quietly previewing a horse the game will
not breed - it has already caught a wrong `nextLong()` port and four schema
defaults that had drifted apart.

Run `:common:test` first when iterating on genetics/stats - it doesn't touch
Minecraft. Requires JDK 25; `foojay-resolver-convention` in
`settings.gradle.kts` auto-provisions it.

`runServer` here does not auto-stop - it sits at the console after `Done`.
Kill it (`taskkill`, or a PowerShell `Stop-Process` on the recent `java`
pids) when the smoke test has printed `Done (...)! For help`.

## The genetics model, as implemented

**Alleles are objects.** A `Genotype` is one `AllelePair` per registered
`Gene` (`common/genetics/`), held internally as a `Map<geneKey, AllelePair>`.
It round-trips through a **gene-keyed code string**: one
`<geneKey>=<a>/<b>` segment per gene in `Genes.codeOrder()`, segments joined by
`-`, the two alleles joined by `/`, dominant first. Allele tokens can be **any
run of characters** (`Spl`, `Cr`, `prl`, `Ch`, `N`, ...). Example:
`"horsegenetics.extension=E/e-horsegenetics.agouti=A/a-..."`. **Parsing is
tolerant** (dev only, no saves): a registered gene with no segment reads as its
wild type; a segment naming an unregistered gene is dropped; the empty string
is the all-wild-type genotype. A bad *allele token* on a known gene is still a
hard error. `Epigenome`'s code string is the same shape
(`<geneKey>=<pri>:<seedhex>/<pri>:<seedhex>`), with a deterministic
key-seeded placeholder filling any gene a stored code predates. **No positional
/ legacy parsing** - so adding or removing a gene is just a coat regeneration.

**Gene priority + derived orderings.** Every `Gene` declares an
`int priority()` (a fixed constant of the gene, no default). All three
orderings are *derived* by one sort of **every** registered gene - built-in and
`SpecGene` alike - on `(priority, key())`: `codeOrder()`/`all()` = the whole
sorted list, `naturalOrder()` = it filtered to `isNatural()`, `magicalOrder()`
= filtered to the magical genes. There are no hand-written lists any more, and
a data-driven natural gene at priority 45 lands *between* the built-in MATP
(40) and champagne (50) - loaded genes are not appended. Bands are a
convention: `0-99` natural, `100+` magical; `Genes.register` logs a warning for
a gene outside its phase's band (via `System.getLogger`) but carries on. Ties
break alphabetically by key. Built-in priorities: extension 10, agouti 20,
silver 30, mushroom 32, dun 34, **MATP 40**, champagne 50, grey 55, white 60,
roan 70, tobiano 72, frame 74, sabino 76, splash 80, pink hair 110, magic
zebra 120, test 900. Within the natural band **low = sets pigment absolutely,
higher = dilution** (agouti's absolute points must precede
`PigmentField.dilute`). `AlleleEpigenetics.priority` is unrelated - it picks a
*seed*, never an order. `GenotypeCatalog` is lazy and invalidated on every
registration.

`Genes.codeOrder()` (derived) = extension, agouti, silver, mushroom, dun,
MATP, champagne, grey, white, roan, tobiano, frame, sabino, splash,
pink hair, magic zebra, test. `naturalOrder()` (phase-1 pigment restriction) =
the same list minus the three magical genes - silver / mushroom / dun sit
right after agouti so the points exist to dilute, the white-pattern genes just
before splash. **Full per-gene detail is in `wiki/gene-*.html`** (one page per
gene); one-liners:

| gene | alleles | outcomes (per combination) | in the wild | coat effect |
|------|---------|-----------------------------|-------------|-------------|
| extension | `E`/`e` | wild (`E_`), `chestnut` (`ee`) | 25/50/25 | `ee` = black restricted → chestnut |
| agouti | `A`/`a` | wild (`aa`), `bay` (`A_`) | 25/50/25 | `A_` = bay; one uniform "point extent" off the `A` copy sets leg + face black, each leg jittered; a high roll = seal (non-det). Reports wild on a chestnut via `expressionIn` |
| white | `W`/`w` | wild, `white` **(masks)** | 1/50 per allele | `W_` = all pigment gone → transparent; masks every other gene |
| test | `T`/`t` | wild, `test-overlay` **(masks)** | **25% `T/t`, 0% `T/T`** | `T_` = paint the `TestCoatPattern` gradient **flat on top** in phase 3 (magical; visible on any base incl. white). Its founder table is why frequency is per *combination* |
| champagne | `Ch`/`c` | wild, `champagne` | 1/40 per allele | dilute toward the gradient's gold; keeps bay's points chocolate (amber champagne) |
| splash | `Spl`/`spl` | wild, `splash` | 1/20 per allele | random white socks + face blaze (non-det) - **open issue:** only the blaze, the sock edges are a hard ring, and **both variant combinations map to the one expression**, i.e. it still doesn't read its dose. The table now *says* that rather than hiding it behind a mislabelled tag |
| grey | `G`/`g` | wild, `grey` | 1/16 per allele | **adults only** - **dapple grey** (`GreyCoat`): remaps onto the gradient's neutral column, per-horse progression / dapple size / dapple strength / point retention (non-det); foal born base colour |
| MATP | `Cr`/`prl`/`N` | wild (`N/N`), `pearl-carrier` (`prl/N`, a wild type), `single-cream` (`Cr/N`), `classic-pearl` (`prl/prl`), `double-dilute` (`Cr/Cr`, `Cr/prl`) | `Cr` 1/30, `prl` 1/22 | **three alleles, six combinations**: cream and pearl are one locus. Never leaves a pitch-black point. Was two genes + `CreamPearlDilution` |
| magic zebra | `Mzeb`/`n` | wild, `zebra` | 1/100 per allele | **magical** - black stripes hung from the topline, `-200%` on all three channels so they read black over any coat incl. dominant white (non-det) |
| pink hair | `Pihr`/`n` | wild, `pink-carrier` (a wild type), `pink-hair` | 1/12 per allele | **magical** - mane + tail walked 82% toward hot pink; reads what it paints over, so it keeps the strand shading (foal: tail only). The clearest carrier locus: two of three combinations are wild types |
| dun | `D`/`d` | wild, `dun` | 1/24 per allele | mild body dilution + **primitive markings** (dorsal stripe full length, faint leg bars) that *skip* the dilution so they read dark; `CoatRegions.dorsalStripe`/`legBar`. 2-allele form (real locus is `D`/`d1`/`d2` - now expressible, just not written) |
| silver | `Z`/`z` | wild, `silver` | 1/60 per allele | eumelanin-**only** dilution → chocolate body + near-flaxen mane/tail; chestnut carrier looks unchanged. Runs after agouti. Dapples are a follow-up |
| mushroom | `Mu`/`mu` | wild, `mushroom-carrier` (a wild type), `mushroom` | 1/34 per allele | pheomelanin-**only** dilution, `Mu/Mu` only → chestnut becomes flat sepia; near-invisible on black/bay |
| roan | `Rn`/`rn` | wild, `roan` | 1/30 per allele | high-freq `BodyNoise` white-hair dither on the barrel + upper legs; head / mane / tail / lower legs stay solid (non-det) |
| tobiano | `To`/`to` | wild, `tobiano` | 1/50 per allele | big smooth-edged white patches from a low-freq noise field **biased toward the topline** so they cross the back; white legs, coloured head (non-det) |
| frame | `Ov`/`ov` | wild, `frame` | 1/55 per allele | flank patches that **never cross the topline** (noise × a spine→0 weight) + a bald face; legs coloured. **`Ov/Ov` lethal white is not modelled** - it wants a third expression when health lands (non-det) |
| sabino | `SB1`/`sb1` | wild, `sabino1` (`SB1/sb1`), `sabino-white` (`SB1/SB1`) | 1/45 per allele | **the gene that broke the dominance vocabulary**: all three combinations land somewhere different. `SB1/sb1` = jagged stockings + belly patch + broad blaze; `SB1/SB1` = "sabino-white", 90%+ white (non-det) |

**Expressions, not dominance.** `common/genetics/Expression` is one *outcome*
a gene can produce: `id` (stable, unique in the gene - the gallery dedups on
it), `name`, a human-readable `description` for the gene dictionary and the
wiki, `wildType` ("this combination changes nothing" - no painter, skipped by
the composer, excluded from the texture key, reads as absent in the display),
`masks` ("while this shows nothing else is visible"), `deterministic`, and
**the paint function itself** (`restrict` for a natural gene, `tint` for a
magical one - never both). A gene declares its outcomes as constants and maps
any pair to one with `expressionOf(AllelePair)`; `expressionIn(pair, genotype)`
is the same question in genotype context and is what the pipeline calls (only
agouti overrides it). `isVisible` / `isDeterministic` are now *derived*.

There is **no dominance property and there will not be one.** "Dominant" and
"recessive" only describe a two-allele locus, cannot express codominance, and
are shorthand for *which combinations happen to share an outcome* - which the
table says directly, for any number of alleles. Several pairs on one expression
= dominant; only the double-variant off the wild type = recessive; MATP's
`Cr`/`prl`/`N` = codominance. A gene may declare **several wild types** when
silent combinations deserve different wording (MATP's `pearl-carrier`); the
gallery collapses them all into one pen, because "changes nothing" is one look.

**`FounderTable`** replaces `randomPair`: a weight per allele *combination* as
percentages, sparse, normalised-with-a-warning, **one `nextFloat()` per gene
per founder**, in `codeOrder()`. `FounderContext` carries the genes already
rolled for a genome-aware distribution and *throws* if asked about a later one.
`FounderTable.hardyWeinberg(variant, baseline, p)` computes the three
two-allele numbers the old "1 in N" meant.

Seal has **no gene** - it's the top of agouti's random distribution. Cream and
pearl are **one gene** (`MatpGene`), which is what the multi-allele model bought.

`Genotype.phenotype()` → coarse `CoatPhenotype` (`CHESTNUT`/`BLACK`/`BAY`/
`WHITE`; everything else ignored) - now only used for family-tree fallback
(foals are fully generated too).

`random(rng)` - each gene draws its pair from its `FounderTable`: **1
`nextFloat()` per gene**. `breedWith` = **2 `nextBoolean()` per gene**. `Gene.isVisible(pair, genotype)`
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
- `priority` (the epigenetic one, `AlleleEpigenetics.priority`) has no other
  consumer, and **by design it never will**: it picks *which copy's seed
  expresses*, never the order genes are processed in. Gene processing order
  comes from `Gene.priority()` (a fixed per-gene constant; `Genes` sorts on
  `(priority, key)`), so two horses with the same genotype and the same seeds
  can't diverge on epigenetic priority alone and silently share a coat cache
  entry. It stays a full-range int for headroom.

Full inheritance detail: **`wiki/breeding.html`**.

## Data-driven genes + the gene creator (`common/genetics/spec/`, `wiki/gene-creator/`)

**The point:** a gene that fits a fixed set of shapes is a **JSON file**, not a
Java class. Drop it in `config/horsegenetics/genes/` and restart. Full format
reference is **`wiki/gene-format.html`**; the machinery:

- **`GeneSpec`** is the format as records (**`"format": 2`** since the
  combination-table rewrite): a header (key, alleles, priority) plus an
  **`expressions`** table and a **`founders`** table. Each expression names the
  combinations that land on it (`when`, a list or a token→count map; exactly one
  entry may omit it and catch the rest), carries `wildType` / `masks` /
  `varies` / a human-readable `description`, and holds **its own `layers` and
  its own `effects`**. The parser proves the table is **total and unambiguous**
  over all `n(n+1)/2` combinations - a gap, an overlap or an unreachable
  catch-all is a load error naming the offending combination. A layer is
  **where** (`Mask`s, folded into one coverage value per texel) times **what**
  (an `Op`), applied *scaled by* that coverage - so a spec gene's edges are soft
  by construction, which is the one thing each hand-written gene had to remember
  separately (splash's hard sock ring is the counter-example).
- **`SpecSchema`** is the single declaration of which parameters each mask and
  op accepts and what each defaults to. The parser validates against it (an
  unknown key is an error naming the key and listing the legal ones), the
  creator builds its forms from a mirror of it, and `wiki/gene-format.html`
  quotes it. **Add a mask or an op in all three, or the tool and the game
  drift.**
- **Values.** Any numeric parameter is a constant, `"$knob"`, an inline
  `{min,max}` (which the parser turns into an anonymous knob), or
  `{"perDose":[a,b,c]}` (which counts copies of the **first-declared** allele -
  a within-one-expression convenience, largely superseded now that a different
  dose can simply be a different expression). `SpecValues` draws every knob once, in declaration
  order, off `ctx.epigeneticsFor(key)` - so the determinism contract holds
  unchanged. A `per: "leg"` knob with a `spread` reproduces `BayCoat`'s
  "one extent for the horse, each leg jittered" in one line of JSON.
- **`SpecGene`** answers the whole `Gene` interface from the spec, so nothing
  downstream knows or cares that it came from a file - genotype code, breeding,
  the catalogue, the coat.
- **Loading** is `GeneSpecLoader`: a classpath index (for genes shipped inside a
  mod) plus a real folder walked in filename order. **Two genes now ship** -
  `example.suntouched` and `example.waterborn`, via
  `neoforge/src/main/resources/horsegenetics/genes/index.json`, added 2026-09-02
  to make the effects work testable (see the status snapshot). So **in-game** the
  genotype code is **19 segments** and the gallery numbers are ~4x;
  **`:common:test` still sees the 17 built-ins** because that index is not on its
  classpath, so `coat-golden.txt` is untouched.
  `neoforge/ModGeneSpecs` calls it **from the mod constructor**, which is the
  earliest hook there is: every registration lengthens the genotype code by a
  segment, so a gene registered after something has parsed a code would
  invalidate it. A bad file is logged and skipped, never fatal.

### `effects` - Minecraft behaviour on a data-driven gene

An **expression** may carry an **`effects`** array alongside its `layers` - the
things a gene makes the horse *do*, not the pixels it paints. Because effects
hang off the outcome, a homozygote and a heterozygote can grant entirely
different behaviour by being different expressions, with nothing comparing
doses. Closed set of six verbs
(`traversal`, `attribute`, `emitter`, `mob_effect`, `yield`, `glow`); each takes an
optional boolean **`when`** (flags + `all`/`any`/`not`) and a **`minDose`**
(1 = any expressing copy, 2 = homozygous). `common/` owns the vocabulary and
the parse (`GeneAbility` records, one `AbilityType` module per verb, a
  generic `GeneSpecParser.readAbility`,
and `SpecAbilities.activeFor(Genotype)` which picks the expressed ones); it
never touches Minecraft. The **NeoForge translator** is `server/`
`GeneAbilityHandler` (an `EntityTickEvent.Post` that evaluates conditions and
applies `traversal`, fires `emitter`s, keeps `mob_effect`s topped up via
`applyMobEffect`, and reconciles `glow`'s light block via `reconcileGlow` +
an `EntityLeaveLevelEvent` cleanup) and `server/GeneYieldHandler` (an
`EntityInteract` handler for `yield`). Both short-circuit when
`SpecAbilities.anyLoaded()` is false (true again only if both shipped genes are
removed). `glow` also has a **client** half - `GeneticHorseRenderer` reads the
expressed `Glow.emissiveParts()`, `GeneticCoatTextureFactory.getOrCreateEmissive`
bakes a full-bright mask, and `client/EmissiveCoatLayer` (a twin of vanilla's
`HorseMarkingLayer`) draws it with `RenderTypes.eyes(...)`. It is the only verb
with a client-render component.
**`attribute` is the one verb carried but not executed yet** - the handler logs
it once (and logs `mob_effect:<id>` once if an effect id fails to resolve).
`walk_on_water` is surface buoyancy + "don't sink", not a real collision plane.
**Not play-tested.** Full reference: `wiki/gene-effects.html`; worked examples
`wiki/gene-waterborn.html` + `wiki/gene-suntouched.html`; the wider architecture
`wiki/horse-traits.html`. **Built-in Java genes do not use this path** - it is
spec-only.

### The creator, and why it can be trusted

`wiki/gene-creator/` is not a mockup. It runs a **JavaScript port of the coat
pipeline** - `geometry.js` (`HorseSkinGeometry`), `noise.js`
(`BodyNoise`/`BodyStripes`, on a hand-written two-word u64 because the hash is
64-bit integer maths and JS numbers are doubles), `fields.js`
(`PigmentField`/`ColorField`/`GradientLut`/the composer), `base-coats.js` (the
real natural genes, for the 15 base coats a new gene is previewed over) and
`spec-engine.js` (the twin of `SpecPainter`, plus a `java.util.Random` port so a
preview seed draws the numbers the game would).

- The 3D horse is **built from the geometry tables**, not loaded from a GLB, so
  the UV layout is the game's by construction and a click maps back through the
  same sample grid the pipeline uses. (It also means no file fetch, which
  `file://` blocks.) The old `wiki/horse.glb` and `wiki/horse_white.png` are
  gone with it.
- Everything is a **classic script** with an `HG` namespace and the textures are
  inlined data URIs, because ES modules and cross-file image reads are both
  blocked under `file://`. The two generated files come from
  `./gradlew :common:bakeCreatorAssets`.
- **A port drifts, and a drifted preview is worse than none** - it shows a horse
  the game will not breed, convincingly. So `./gradlew :common:bakeSpecFixtures`
  writes what the real Java engine produces (48 cases: 4 example genes x 3 seeds
  x 2 doses x adult/foal, plus the whole `SpecSchema` table) and
  `node wiki/gene-creator/tools/check-parity.mjs` re-runs it in JS - **3 784
  checks**. It earns its keep: it caught `nextLong()` ported as a concatenation
  where Java does a signed add, and four schema defaults that had drifted (the
  creator omits any setting left at its default, so a mismatched default writes
  a file that plays differently from how it previewed - silently, and only for
  the settings you never touched).

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

1. **Wild spawn / `/summon` / gallery horse** -> one `HorseRecord` attachment
   carrying **both** the genotype code and the epigenome code
   (`HorseRecords.newFounder` -> `CoatGenerator.generate` -> a founder
   `Epigenome.random`). There is **no separate coat attachment** any more; the
   record default is `HorseRecord.unassigned(uuid)`, whose empty
   `epigenomeCode` is the `hasGenome()` sentinel the join handler tests.
   **Breeding is different**: `HorseBreedingHandler` builds the foal's genome
   itself from `damGenome.breedWith(sireGenome)` and writes it into the foal's
   record, because the inherited epigenetics can only be read while both
   parents are in hand - the join handler would re-roll them. It then sees
   `hasGenome()` true and leaves it alone.
   **The custom horse spawn egg** is a third path: `ModNetworking`'s
   `SpawnCustomHorsePayload` handler applies a founder record with the
   player-picked code + sex **before** `addFreshEntity`, so the join handler
   takes its "already has a real record" branch and keeps the genome; the coat
   attachment is still unset at that point, so a founder `Epigenome.random` is
   rolled exactly as for a wild spawn.
   **The stallion seed jar** is a fourth path, and it behaves like breeding:
   `StallionSeedJarHandler` builds the foal `Horse` itself, reads the mare's
   genome live and the sire's from the jar's `StoredGenome`, and calls the same
   `HorseBreedingHandler.applyBredFoal` - which writes the coat attachment
   before `addFreshEntity`, so the join handler leaves it alone.
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
- **Wall signs, placed from code** (the stall sign does this):
  `Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, dir)`
  (`FACING` is a horizontal `EnumProperty<Direction>`), `level.setBlock(pos,
  state, Block.UPDATE_ALL)`, then `((SignBlockEntity) level.getBlockEntity(pos))
  .updateText(t -> t.setMessage(line, Component), /*front=*/true)` -
  `updateText` takes a `UnaryOperator<SignText>` and `SignText.setMessage`
  returns a new `SignText` (chain the calls).
- **`BreakBlockEvent`** (`net.neoforged.neoforge.event.level.block`): fires
  *before* removal, so `event.getState()` is still the block being broken;
  `getLevel()` -> `LevelAccessor`, `getPos()`, `getPlayer()`. Cancelable.
- **`SavedDataType<T>(Identifier, Supplier<T>, Codec<T>)`** + `server
  .getDataStorage().computeIfAbsent(TYPE)` is the server-global SavedData
  pattern (`HorseAncestryData`, `StallData`). `ResourceKey<Level>` round-trips
  in a codec via `ResourceKey.codec(Registries.DIMENSION)`.
- **`ServerLevel#sendParticles(ServerPlayer, T, boolean overrideLimiter,
  boolean alwaysRender, x, y, z, int count, dx, dy, dz, double speed)`** -
  the per-player overload; `alwaysRender = true` defeats distance culling
  (used for the stall debug outline).

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
`ModNetworking`'s `RequestDebugPensPayload` handler re-checks). **F7** is the
other dev keybind - `key.horsegenetics.show_stalls`, flashes the particle
outline of nearby stalls (`RequestStallHighlightPayload` -> `StallDebug`, also
re-checked against `isProduction()`). The class /
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
  `allPairsOf(gene)` = every unordered `AllelePair`, all `n(n+1)/2` of them
  (`ee`, `Ee`, `EE`); `distinctPairsOf(gene)` keeps one representative per
  distinct `Expression`; `totalGenotypes()` = the raw product
  (**258 280 326**); `size()` = the reduced catalogue (**98 306**); `get(i)` /
  `entries()` read the list, built once at class load. Nothing is hard-coded - register a gene (or an allele) and the
  catalogue, the corridor length and both signs widen on their own.
- **Two reductions**, both read straight off the gene's expression table with
  no dominance metadata in the middle:
  - **pairs landing on the same `Expression` collapse** to one representative -
    the homozygous pair where the group has one, so a pen reads `EE` not `Ee`.
    **Every wild type is one group**, however many the gene declares, because
    "changes nothing" is one look: MATP's `pearl-carrier` shares a pen with its
    plain `N/N`. This is exact, where the old "drop the heterozygote unless the
    gene is incomplete dominant" rule was an approximation;
  - an expression that **`masks`** hides everything else, so the catalogue keeps
    exactly **one** entry for it: that combination with every other gene at a
    wild type. Hence one white pen (`EEaa WW`) and one test pen (`EEaa TT`)
    instead of a huge fraction of the corridor each.
  - Net: `2^13 · 4 (MATP) · 3 (sabino) = 98 304` unmasked + 1 white + 1 test =
    **98 306**. Splash dropped from 3 pens to 2 (its two variant combinations
    are one expression) and MATP from cream×3 · pearl×3 = 9 to 4.
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
    `GeneCodeDisplay.wrap(genotype, 3, 15)`. **At 18 genes the widest labels
    come nowhere near fitting** three 15-char lines - a horse loaded up on the
    white-pattern + dilution genes runs to well over a hundred chars - and
    `wrap` deliberately overflows its **last** line rather than dropping a
    gene, so those signs read very wide in-game. The unit test asserts only
    that nothing is lost and that the overflow doesn't grow past 140. The real fix is the planned revert to random
    pens (`wiki/roadmap.html` §9), which retires the per-genotype sign
    entirely - so this is deliberately left alone.
  - `originX + 4` (three blocks in front of the return portal), facing west at
    the player's spawn: `Genotypes / 387,420,489 / Distinct / 331,778 pens`.
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

**The seven new visual genes (2026-09-02, reworked once after owner feedback) -
remaining follow-ups, none seen in-game:**

- **Dun** leg barring is a hand-rolled Y-phase; roadmap §4.1 wants it to reuse
  `BodyStripes` (which runs on X). The third allele (`d1` marked / `d2`
  unmarked) used to need more than one `DominancePattern`; the combination
  table can express it now, so it is a gene rewrite rather than a framework
  change. (Grullo now lands on the LUT neutral column - `keepRed` scales to 0
  by the texel's black content.)
- **Silver** has no dapples yet - v1 is the dilution only. A deterministic
  (fixed-seed) `BodyNoise` dapple modulation is the obvious next step. The
  flaxen mane currently reads a little gold rather than pale.
- **Frame** models the coat only; `Ov/Ov` lethal white (roadmap §4.2 / §6.4,
  the first lethal in the model) is not built - the homozygote just renders as
  an ordinary frame. Coverage is deliberately bold (0.52-0.74); may want
  trimming once seen in-game.
- **Sabino / roan / tobiano / frame** thresholds and densities are eyeballed
  off sample bakes, not play-tested - expect a coverage retune once on the 3D
  model. Roan still shows the odd 1px gold fleck at a fleck edge (the LUT
  diagonal); tobiano/frame are hard-binary so they don't.
- **The wide white-pattern genes don't compose an eye-safe check** - like
  splash, a big `SB1/SB1` or a topline-crossing tobiano could in principle wipe
  the eye texels; `CoatRegions.redrawEyes` runs last so eyes always come back,
  but confirm in-game.

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
   (the second half of this gap - the epigenome living on the entity, so
   `FamilyTreeScreen` had to invent an ancestor's coat from its UUID - was
   closed 2026-09-03 by moving the epigenome onto `HorseRecord`.)
10. **Nothing reads the expression table but the coat and the gallery.** Every
   gene now carries, per combination, a display name and a human-readable
   sentence saying what it does - written for the gene dictionary and the wiki,
   and read by neither yet. The obvious consumers: a punnett / expected-foal
   display, "carrier of X" wording in the info panel (MATP's `pearl-carrier`
   and pink hair's `pink-carrier` already have the sentence), a generated gene
   dictionary, and `GeneCodeDisplay` deciding what is worth printing. Also
   unread: `Expression.masks()` outside `GenotypeCatalog`, and
   `Gene.name()`.
11. **Cleanups**: rename `DebugPenManager` / `DEBUG_LEVEL` /
   `horsegenetics:debug_pens` to non-"debug" names (needs a save-data
   migration or a one-time reset); fold speed/health into the gene model;
   name-generation rework; real white-fog dimension effects
   (needs a client dimension-effects mixin); the stray `neoforge.mods.toml`
   duplicate.
12. **The wiki is now load-bearing, so it can rot.** `wiki/api-reference.html`
   hand-transcribes public signatures out of `common/` and
   `wiki/gene-*.html` hand-transcribes each gene's constants - neither is
   generated, so both drift silently the moment a signature or a tuning number
   changes. Nothing checks *those*. The one place this is now guarded is the
   **gene creator**: `check-parity.mjs` compares its schema mirror and its whole
   preview engine against the real Java. That is the model for the rest - the
   cheap version elsewhere is a `:common:test` that greps the gene pages for the
   constants they quote. Until then the other pages are a discipline item, which
   is why they are in the session-end routine.
13. **Data-driven genes cover markings and dilutions, not everything.** The
   format has no expression language and no way to read another gene, so the
   three built-ins that genuinely need one still can't be expressed as specs:
   **grey** (its remap onto the gradient's neutral column reads the coat's
   darkness *and* rewrites both channels together - `PIGMENT` masking gets close
   but not there), **cream/pearl** (they read *each other's* dose), and
   **bay**'s exact face-follows-legs coupling. Those stay Java, which is fine -
   the tiers were always meant to bottom out at a real class. What would move
   the line: a `dose` mask on another gene, and a `REMAP` op.
14. **The creator has no in-page parity button.** Parity is checked by a Node
   script at the terminal, so the tool itself will happily show you a stale
   preview if you edit `js/` and don't run it. Loading `fixtures/expected.json`
   in the page and self-checking on boot would close that.
15. **Gene `effects` are a thin slice and mostly untested.** `attribute` still
   parses but the translator doesn't apply it (logged once). `mob_effect` is
   wired (`applyMobEffect`). `glow` is wired both sides - the light half is a
   trailing `minecraft:light` block (janky vs a mixin-based dynamic light: lags
   a gallop, air-only, orphans on a server crash, skipped in the gallery), the
   emissive half is `EmissiveCoatLayer` reading the composed coat. `emitter`'s
   own `light` kind is still a no-op (use `glow`); `emitter` otherwise only does
   particles; `yield` recognises a fixed handful of output items;
   `walk_on_water` is buoyancy, not a solid plane. Conditions are boolean, not
   the architecture's 0-1 scalars. There is no trait registry, no `on_change`,
   no selectors/auras/pools. The gene creator can't edit an `effects` block -
   it's hand-written. Only Waterborn's coat + trail are confirmed in-game;
   Suntouched and every other effect verb are unverified
   (`wiki/verification.html` §13). The full plan is `wiki/horse-traits.html`.

16. **Some gameplay-layer items still have no behaviour.** The seed jars,
   whistles and stall signs work; still unwired: shearing to get `horse_hair`,
   any carrot effect on the breeding draw, and **the tickets** - owner's intent
   is that a ticket teleports its bound horse back to its stall, which is now
   possible (stalls exist - `StallData` / `StallRecord.center()`), it's just not
   built. The `magic_gene_carrot` is one generic item because per-gene targeting
   wants a data component (`wiki/roadmap.html` §14.2, §19);
   `placeholder_gene_book` replaces the real research paper. Tickets share one
   texture, whistles share one, stall signs borrow `oak_sign` - per-tier / real
   art is a follow-up (`wiki/verification.html` §15).
17. **The stallion seed jar is a first slice, not the §15.1 flow.** Collection
   and impregnation are wired (`StallionSeedJarHandler` + the `stored_genome`
   component + `GenomeSample` + `HorseBreedingHandler.applyBredFoal`). The gate
   is **vanilla love** (`isInLove()`), not one of this mod's breeding carrots
   (they still do nothing); there's **no gestation** (the foal appears
   immediately, like vanilla breeding), and the jar carries **no carrot
   effects**. The synthetic sire record uses the stored donor UUID as its
   pedigree edge, so the family tree may not find the sire node. Gestation is
   the "genuinely new" piece per the roadmap; the real carrot gate waits on
   §14. Owner tests 2026-09-02: (a) held jar didn't change in creative -> fixed,
   transforms in hand now; (b) worked with no breeding-mode requirement ->
   fixed, both ends now require `isInLove()` and consume it.
   `wiki/verification.html` §16.
18. **Fixed 2026-09-03 (data-model rewrite), not play-tested.** The short
   genome string now shows data-driven genes: `GeneCodeDisplay` derives its
   trailing gene list from `Genes.codeOrder()` (built-ins in a curated display
   order, then `Genes.loaded()`) and derives the "wild type means absent"
   test from `dominance()`, so the shipped Suntouched / Waterborn appear.
   Confirm in-game (info panel, paper dump, seed-jar tooltip).
19. **The stall system is detection + storage only.** A stall gets defined and
   persisted (`StallData`), but nothing *uses* it yet: no teleport-to-stall, no
   "assigned pen" behaviour, no auto-return. Cleanup is thin - a stall only goes
   away if its exact sign block is broken (`BreakBlockEvent`); rebuilding a wall
   elsewhere, or removing the horse, leaves a stale record. The sign is a plain
   vanilla `oak_wall_sign` (no marker that it's a stall sign beyond the
   `StallData` entry keyed on its pos). The "debug overlay" is server-emitted
   `HAPPY_VILLAGER` particles on the F7 keybind (one flash per press) + a chat
   summary - not a persistent client wireframe, which would need a
   `RenderLevelStageEvent` renderer (the 26.1.2 render pipeline changed enough
   that this was deliberately deferred). Flood-fill is air-only and capped at
   `StallDetector.MAX_BLOCKS` (512).

20. **Horse care is a first slice, and unplayed.** `HorseCareHandler` +
   `BondFollowGoal` + `HorseCareAttachment` cover §7.2 gated healing and §13
   bond/herds; details and what's deferred are in `wiki/horse-care.html`, the
   in-game checklist in `wiki/verification.html` §0. Specifics still open:
   **milking (§7.1)** is not built (its "full health" rule is the hook into
   the healing gate); **bond has no shearing/sleeping source** and there is
   **no console command to set it**, so testing the tiers means grinding or
   temporarily lowering the thresholds; **herd alpha** is computed-on-demand
   with nothing consuming it yet; the **comfort buff** is restated as +1 regen
   because the mod has no stamina; the healing scan uses **block tags** so a
   pack can extend `horse_water` / `horse_food`, but bucket/fluid-source
   placement inside a waterlogged block only counts via the `#minecraft:water`
   fluid check, not the block tag. Feed-bond fires on `EntityInteract` for any
   `isFood` stack and is **not** dose/temper-aware.

## License

CC BY-NC 4.0 (see `LICENSE`). Forks/derivatives are welcome without asking
but must credit the original repo and link back, and no portion may appear in
a paid derivative with no free version available. Donations/tips on an
otherwise-free derivative are fine. Before vendoring third-party code, check
its licence is compatible.

## Conventions

- Keep `common/` free of Minecraft imports - a hard rule.
- New version-specific logic goes in `neoforge-26.1.2/`, by concern
  (`client/` / `data/` / `network/` / `server/` / `block/` / `item/`).
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
  `wiki/api-reference.html`; the **non-genetic horse-care systems** (gated
  healing, bond, herds) **only** in `wiki/horse-care.html`. Update the relevant
  file in the same change - a pointer from CLAUDE.md is fine, a copy is not.
- **The wiki has one nav.** A new page goes in the `SECTIONS` array in
  `wiki/nav.js` and nowhere else; never hand-write a sidebar into a page.
  (`wiki/gene-creator/` is the one exception - it is an app, not a page, and
  owns its own chrome.)
- **The data-driven gene format** is documented **only** in
  `wiki/gene-format.html`. A new mask or op has to land in four places in the
  same change: `SpecSchema.java`, `SpecPainter.java`,
  `wiki/gene-creator/js/schema.js` + `spec-engine.js`, and that page. Then
  re-run `:common:bakeSpecFixtures` and `check-parity.mjs`.
- **A new `effects` verb** lands in **four** places, only one of them shared:
  a `record` on `GeneAbility`, one `register(new AbilityType(...))` on
  `AbilityType` (name + params + defaults + validation + record builder - the
  parser reads this generically, no `readAbility` change unless the verb needs a
  new param `Kind`, as `glow`'s `parts` needed `Kind.PARTS`), a `case` in the
  NeoForge translator (`server/GeneAbilityHandler` tick switch, or
  `server/GeneYieldHandler` for interaction), and a section in
  `wiki/gene-effects.html`. A verb with a **client-render** component (so far
  only `glow`) also needs a `RenderLayer` + a `GeneticHorseRenderState` field +
  the bake in `GeneticCoatTextureFactory` - not just a translator `case`. A new
  **condition flag** is two lines (`AbilityType.CONDITION_FLAGS` +
  `GeneAbilityHandler.flagHolds`); a new **trigger** also touches
  `GeneSpecParser.readTrigger`. None of it is part of `SpecSchema` or the parity
  check - effects don't paint.
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

**0. Pre-flight.** `./gradlew :common:test`, `./gradlew
:neoforge-26.1.2:build`, and - if anything under `common/genetics/spec/`,
`SpecPainter` or `wiki/gene-creator/js/` was touched -
`node wiki/gene-creator/tools/check-parity.mjs`. Don't push red. If something fails and can't be fixed
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
- **`wiki/gene-format.html`** - if a mask, an op or the file header moved. If
  so, also re-run `./gradlew :common:bakeSpecFixtures` and
  `node wiki/gene-creator/tools/check-parity.mjs`, and commit the regenerated
  `wiki/gene-creator/fixtures/expected.json`.
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
