# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses: a Mendelian genotype of **allele objects** (extension,
agouti + seal, white, champagne, a `T` "Test" diagnostic) drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, rolled speed/health
stats, and an epigenetic seed; and there's a self-contained "horse dimension"
reached by a hay-bale portal. Long-term aim is a 1.12.2 backport, which is
why the logic is quarantined in a game-free module.

**Docs split:** everything except `README.md` and `CLAUDE.md` lives in
**`Docs/`**. New dev docs go there too - keep the repo root to those two.
- **`README.md`** is **user-facing only** now - what the mod does, how to play
  it, install, license. No status tables, no architecture, no API notes.
  Don't put dev content there.
- **`CLAUDE.md`** (this file) is the dev/working notes: status, the 26.1.2 API
  differences, gotchas, next steps.
- **`Docs/breeding.md`** is the single source of truth for the breeding / horse-
  record / pedigree / **stat-inheritance** system. Keep it current when you
  touch any of that; don't re-document it here or in README (a pointer is
  fine).
- **`Docs/Gene Dict.md`** is the single source of truth for **each gene** - alleles,
  generation function, wild frequency, dominance, natural/non-natural. Update
  it in the same change as any gene; CLAUDE.md keeps only the machinery + a
  one-line-per-gene table.
- **`Docs/to be verified.md`** is the rolling **`runClient` checklist** - what's
  built but not yet confirmed in-game. Update after every play session.

## Status snapshot (keep this current)

- **`common/`** - compiles; **117 JUnit tests pass** (`./gradlew :common:test`).
  Covers `genetics/` (allele/gene model - 9 genes incl. grey / cream / pearl / splash, `Genotype` code
  round-trip, breeding, `DominancePattern` + the `GenotypeCatalog` reduction of
  19 683 genotypes to 434 distinct coats), `coat/` + `coat/pattern/` (the overlay pipeline -
  `CoatTextureComposer`, `GradientLut`, `BayCoat`, `CoatRegions`, the genes,
  `CoatTextureId` texture-id injectivity),
  `coat/skin/` (`HorseSkinGeometry`), `name/` (`breedNth`),
  `horse/` (pedigree + `HorseStats` -> `Docs/breeding.md`).
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
    - a high roll of bay's two epigenetic numbers gives the seal look.
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
    **`Docs/to be verified.md`** - see "Known gaps" below; the bay/dilution one
    is now closed.

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

- **Built 2026-09-01, NOT yet play-tested:** the **genotype gallery** rework of
  the horse dimension - one pen per visually distinct genotype (434 of 19 683),
  per-pen genotype signs, the entrance tally sign, `Gene.dominance()` metadata,
  and the entity-only teardown that leaves blocks standing. Compiles, 117
  `common` tests pass, nothing seen in-game yet. Details in the horse-dimension
  section below; the in-game checklist is the top item in
  **`Docs/to be verified.md`**.
- **Open issues + NOT verified in-game:** see **`Docs/to be verified.md`**.
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
  - `genetics/` - `Genotype` (+ `breedWith`), `CoatPhenotype`,
    `GeneticCodeCombiner`.
  - `coat/` - `CoatData`, `CoatGenerator`.
  - `name/` - `HorseNameGenerator` + `HorseNames` (`breed` = one-half-each;
    `breedNth` = varied by a pairing's foal count) + word tables under
    `src/main/resources/horsegenetics/names/`.
  - `horse/` - the pedigree domain model (`Sex`, `HorseRecord`,
    `HorseDatabase`, `InMemoryHorseDatabase`) and `HorseStats` (foal stat
    roll) -> `Docs/breeding.md`.
  - `Rng` - the randomness seam (`nextFloat` / `nextBoolean` /
    `nextInt(bound)`), implemented by `NeoRng` (wraps `RandomSource`) and, in
    tests, `FakeRng`.

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
cream, pearl - **append** new ones. **Full per-gene detail is in `Gene
Dict.md`** (natural genes first, then non-natural); one-liners:

| gene | alleles | dominance | in the wild | coat effect |
|------|---------|-----------|-------------|-------------|
| extension | `E`/`e` | dominant | 50/50 | `ee` = black restricted → chestnut |
| agouti | `A`/`a` | dominant | 50/50 | `A_` = bay + random leg/face black; a high roll = seal (non-det) |
| white | `W`/`w` | **complete** | 1/50 | `W_` = all pigment gone → transparent; masks every other gene |
| test | `T`/`t` | **complete** | 1/4 carrier | `T_` = paint the `TestCoatPattern` gradient **flat on top**, last (only **non-natural** gene; visible on any base incl. white) |
| champagne | `Ch`/`c` | dominant | 1/40 | dilute toward the gradient's gold; keeps bay's points chocolate (amber champagne) |
| splash | `Spl`/`spl` | incomplete\* | 1/20 | random white socks + face blaze (non-det) - **open issue:** only the blaze, the sock edges are a hard ring, and \*it doesn't read its dose yet (`Spl/Spl` should be much bigger markings) |
| grey | `G`/`g` | dominant | 1/16 | **adults only** - equally restrict both pigments to 0.15; foal born base colour - **open issue:** reads flat/near-white, wants a rework |
| cream | `Cr`/`N` | incomplete | 1/30 | incomplete-dominant dilution; interacts with pearl; never leaves a pitch-black point |
| pearl | `prl`/`N` | incomplete | 1/22 | dilution; `prl/prl` no-cream = mild uniform; `Cr/prl` = double cream |

**`Gene.dominance()`** (`common/genetics/DominancePattern`) is declared
metadata on every gene: `DOMINANT` / `RECESSIVE` / `INCOMPLETE_DOMINANT` /
`COMPLETE_DOMINANT` (= dominant **and** epistatic - while it shows, nothing
else is visible). `heterozygoteIsDistinct()` and `masksOtherGenes()` are the
two questions callers ask. Today's only consumer is `GenotypeCatalog`'s
gallery reduction, but it's per-gene metadata so a punnett/breeding UI can use
it too. No gene is `RECESSIVE` yet.

Cream + Pearl are allelic in reality; here two genes, combined once in
`coat.pattern.CreamPearlDilution` (dose table in `Docs/Gene Dict.md`).
Seal has **no gene** - it's the top of agouti's random distribution.

`Genotype.phenotype()` → coarse `CoatPhenotype` (`CHESTNUT`/`BLACK`/`BAY`/
`WHITE`; everything else ignored) - now only used for family-tree fallback
(foals are fully generated too).

`random(rng)` - each gene rolls its pair (draw counts in the gene class).
`breedWith` = **2 `nextBoolean()` per gene**. `Gene.isVisible(pair, genotype)`
/ `isDeterministic(pair, genotype)` see the whole genotype (agouti invisible on
chestnut; cream/pearl read each other). `Genotype.hasVisibleNonDeterministic()`
= "generate the texture per horse".

## The coat overlay pipeline (`common/coat/pattern/` + `client/GeneticCoatTextureFactory`)

Coats are **generated** for every horse - adult *and* foal. Per-gene detail in
**`Docs/Gene Dict.md`**; the machinery:

- **`CoatData`** = `Genotype` + `long epigeneticSeed` (rolled once at birth,
  persisted). `textureKey()` = code, plus `@<seed>` only when non-deterministic;
  the factory also keys on adult vs foal.
- **`CoatTextureComposer.compose(genotype, seed, Skin, adult, template, GradientLut)`**
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
  3. **overlay pass** - each visible non-natural gene (`Genes.overlayOrder()`
     = test) paints a layer **flat on top** of the overlay: an opaque layer
     texel replaces whatever the natural pass resolved there, so the effect
     shows the same on black, chestnut *or* white. (`overlayLayer`, layer
     pre-filled transparent = "no paint here".)
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
- **`GradientLut`** wraps `assets/horsegenetics/textures/coat/redblackgradient.png`
  (hand-authored, 500x500): left = more red, bottom = more black; `(1,1)` =
  black, `(1,0)` = chestnut, `(0,0)` = white, champagne-gold column near the
  middle. `sample(red, black)`: `x = (1-red)*(w-1)`, `y = black*(h-1)`.
- **`CoatRegions`** - reusable `Skin`-aware helpers (fill mane/tail/ears/hooves,
  paint/blacken/whiten a leg, `whitenBlaze`, `redrawEyes`). **Open issue:**
  `whitenLowerLeg` cuts at a hard `point.y() <= cutoff`, so every splash sock
  ends in a perfect ring; and `whitenBlaze` is the only face marking there is.
- **`BayCoat`** - the bay generator (two epigenetic numbers: one leg height,
  one face height; bottom `SOLID_PORTION` = **0.3** of the band solid, then a
  **smoothstep** fade to nothing - no hard cut-off line). Knobs: `BODY_BLACK`,
  `HOOF_FRACTION`, `SOLID_PORTION`. Verified in-game 2026-09-01 (seal included).
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
  far as it goes. Per-mode numbers: `Docs/Gene Dict.md`.
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

1. Horse spawns/breeds -> `HorseRecord` attached (genetic code) and, on the
   same join, a `HorseCoatAttachment` = `{genotype code, epigeneticSeed}`
   (`CoatGenerator.generate` rolls the seed once). Both via `ModAttachments`;
   the coat attachment default is `HorseCoatAttachment.UNASSIGNED` until the
   handler replaces it.
2. Not auto-synced -> the handler sends `CoatSyncPayload` `{entityId, code,
   seed}` and `HorseRecordSyncPayload` to trackers.
3. Client caches in `ClientCoatCache` (`CoatData`) / `ClientHorseRecordCache`,
   cleared on `LoggingOut`.
4. `GeneticHorseRenderer.extractRenderState` reads `ClientCoatCache` ->
   `GeneticHorseRenderState.coatData`; `getTextureLocation` ->
   `GeneticCoatTextureFactory.getOrCreate(coatData, renderState.isBaby)` -
   generated for adult and foal alike (`HdHorseModel` / `HdBabyHorseModel`
   handed to the super ctor; no per-entity model swap).


## Horse stats (speed / health)

Domain side (roll band, record fields, breeding flow) is in **`Docs/breeding.md`**.
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
- Still **no `.gitignore`**: `build/`, `run/`, `.gradle/`, `.idea/` show as
  noise. Worth adding.
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
  `run/config/fml.toml` has `earlyWindowControl = false`. `run/` is not
  tracked - if wiped, this resets to `true`.

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
  `totalGenotypes()` = the raw product (**19 683**); `size()` = the reduced
  catalogue (**434**); `get(i)` / `entries()` read the list, built once at class
  load. Nothing is hard-coded - register a gene (or an allele) and the
  catalogue, the corridor length and both signs widen on their own.
- **Two reductions**, both driven by `Gene.dominance()` (see below):
  - a gene whose heterozygote isn't distinct (`DOMINANT` / `RECESSIVE`)
    contributes only its **homozygotes** - `ee`/`EE`, not `Ee`;
  - a `COMPLETE_DOMINANT` gene **masks everything else**, so the catalogue keeps
    exactly **one** entry for it: the variant homozygote with every other gene
    at wild type. Hence one white pen (`EEaa WW`, #5) and one test pen
    (`EEaa TT`, #6) instead of a quarter of the corridor each.
  - Net: `2·2·2·3·2·3·3 = 432` unmasked + 1 white + 1 test = **434**.
- **Pen order**: segment `i` holds catalogue entry `2i` in the **right-hand**
  pen (`NORTH_PEN`, the `+Z` side - your right walking in from the portal) and
  `2i+1` on the left. The corridor reads `eeaa, EEaa, eeAA, EEAA, [white],
  [test], eeaa ChCh, ...`: extension exhausts before agouti moves. With an odd
  catalogue the final left-hand pen is simply not built.
- **Both horses in a pen share the genotype** but not the epigenetic seed, so
  they're two examples rather than two copies.
- **Signs** (`placeSign`, waxed standing oak, same text on both faces):
  - per pen, on the road one block out from the wall and **to the right of the
    gate** as you face the pen (`roadFacing().getOpposite().getClockWise()`, so
    the two sides of the road mirror): line 0 = `#<1-based catalogue number>`,
    then **`GeneCodeDisplay.shortForm`** - the same compact form the info panel
    and paper dump use, so a plain horse reads `eeaa`, not a wall of wild-type
    slots - greedily wrapped over the remaining 3 lines by
    `GeneCodeDisplay.wrap(genotype, 3, 15)`. A unit test asserts every
    catalogue entry fits (widest today: `eeaa SplSpl nCr`, 15 chars / ~80 px of
    a 90 px line).
  - `originX + 4` (three blocks in front of the return portal), facing west at
    the player's spawn: `Genotypes / 19,683 / Distinct / 434 pens`. Epigenetics
    are deliberately not counted in either number.
- **Length**: `LAST_SEGMENT_INDEX` = `ceil(size / 2) - 1` = 216, so the corridor
  is **1 519 blocks**. `ensureBuiltUpToIndex` clamps to it and calls
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

The **`runClient` checklist lives in `Docs/to be verified.md`** - both the
**open issues** found in-game and what's still unconfirmed. Keep that file
current after each session.

**Open rendering issues (found in-game 2026-09-01, deliberately not fixed
yet)** - full detail in `Docs/to be verified.md`:

- **Grey needs a rework, not a knob.** `KEEP = 0.15` flat on both pigments
  lands the body in the gradient's near-white corner (~`(227,221,215)`) and
  reads flat. Wants progressive-with-age + dapples, which needs an age input to
  the pipeline.
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

1. **Foal geometry is approximate** - `Skin.BABY` uses rest-pose AABBs and
   pre-resolved neck/head/ear pivots; markings on the foal face/neck can land
   loosely. Also the foal mesh has no MANE/MUZZLE part, so bay foal "black up
   the face" is coarse. Foals are also the top **unverified** item.
2. **Genetic eye colour** - the eyes render correctly but are copied verbatim
   from the template (`CoatRegions.redrawEyes`). Wants its own gene; the
   classic hook is blue eyes on cream double-dilutes.
3. **White markings beyond splash** - the framework is ready (natural +
   non-deterministic gene); sock distributions, roan and rabicano slot in the
   same way.
4. **More loci** - dun, pearl-cream stacking nuance, sooty.
5. **Coat realism** - flat gradient sample per pixel (no dappling, sooty
   shading, seasonal coat). `T` on a non-deterministic coat still bakes a
   unique (identical-looking) texture per horse.
6. **`breedNth` foal names past foal 1** / **`FamilyTreeScreen` scroll mode** /
   **stats surfaces** / **water-riding feel** / **epigenetic seed across a
   save-reload** - see `Docs/to be verified.md`.
7. **Use `Gene.dominance()` beyond the gallery** - the metadata is on every
   gene now (`DominancePattern`), but only `GenotypeCatalog` reads it. Obvious
   next consumers: a punnett/expected-foal display, "carrier" wording in the
   info panel, and `GeneCodeDisplay` deciding what's worth printing.
8. **Cleanups**: rename `DebugPenManager` / `DEBUG_LEVEL` /
   `horsegenetics:debug_pens` to non-"debug" names (needs a save-data
   migration or a one-time reset); fold speed/health into the gene model; a
   `.gitignore`; name-generation rework; real white-fog dimension effects
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
  documented **only** in `Docs/breeding.md`; **each gene** is documented **only** in
  `Docs/Gene Dict.md`; the **`runClient` checklist** is **only** in
  `Docs/to be verified.md`. Update the relevant file in the same change - a pointer
  from CLAUDE.md is fine, a copy is not.
- **No legacy / back-compat code.** Dev only, single tester, no saves to keep -
  when a format changes, change it and move on (no genotype-code padding, no
  attachment field fallbacks).
```
