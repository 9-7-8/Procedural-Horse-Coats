# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses: a Mendelian genotype of **allele objects** (extension,
agouti + seal, white, champagne, a `T` "Test" diagnostic) drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, rolled speed/health
stats, and an epigenetic seed; and there's a self-contained "horse dimension"
reached by a hay-bale portal. Long-term aim is a 1.12.2 backport, which is
why the logic is quarantined in a game-free module.

**Docs split:**
- **`README.md`** is **user-facing only** now - what the mod does, how to play
  it, install, license. No status tables, no architecture, no API notes.
  Don't put dev content there.
- **`CLAUDE.md`** (this file) is the dev/working notes: status, the 26.1.2 API
  differences, gotchas, next steps.
- **`breeding.md`** is the single source of truth for the breeding / horse-
  record / pedigree / **stat-inheritance** system. Keep it current when you
  touch any of that; don't re-document it here or in README (a pointer is
  fine).

## Status snapshot (keep this current)

- **`common/`** - compiles; **117 JUnit tests pass** (`./gradlew
  :common:test`). Covers `genetics/` (allele/gene model, `Genotype` code
  round-trip, breeding), `coat/` + `coat/pattern/` (the overlay pipeline -
  `CoatTextureComposer`, `GradientLut`, `BayCoat`, `CoatRegions`, the genes),
  `coat/skin/` (`HorseSkinGeometry` projection engine),
  `name/` (incl. `breedNth`), `horse/` (pedigree + `HorseStats` -> `breeding.md`).
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes; only two `getGuiLeft/getGuiTop`
  deprecation warnings) against the real NeoForge `26.1.2.100` SDK.
- **`runServer`** - boots clean to `Done (...)! For help`; all dimensions,
  attachments, SavedData, payloads, the `hay_portal` block + block entity, and
  the `ClientConfig` all register with no errors.
- **`runClient`** - actively play-tested over the 2026-08-30 session; see below.

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

- **NOT verified in-game (the backlog):**
  - the **`breedNth` foal-name scheme past foal 1** - breed one pair 7+ times
    and confirm: foal 2 = the other combo, foals 3-6 = one parent half + a
    random word, foal 7+ = fully random, no repeats. (`HorseNames` unit tests
    cover the branch logic; the live `HorseAncestryData.offspringCount(dam,
    sire)` feeding it is the unproven link.)
  - **`FamilyTreeScreen` scroll mode** - the `familyTree.scrollBar = true`
    client-config path (wheel + right-edge thumb instead of shrinking).
  - **the allele/gene coat overlay pipeline in-game** (2026-08-31 rework):
    every adult now renders a `GeneticCoatTextureFactory`-generated 128px
    texture on `HdHorseModel`. Check black / chestnut / champagne (gold) /
    white read right; bay has black points + random leg/face black; seal shows
    tan up the legs; eyes survive; foals still fine on vanilla `*_baby`; the
    family-tree node; save -> reload keeps the epigenetic seed (same coat).
    `bakeCoatSamples` output looked right offline; not yet run in the client.
  - the earlier **Test coat** on a live horse was owner-verified 2026-08-31
    (smooth gradient); the projection engine (`HorseSkinGeometry`) is proven.
  - the inventory-panel **stat tint** (green/amber/red vs `parentStats`), the
    **paper dump** `vs parents` line, foal stats actually landing on the
    entity's attributes, and **legacy 4-char / no-stat saves** still loading.
  - "**clock on a tamed foal** no longer also mounts you" (cancel fires
    client-side too now).
  - **riding a tamed horse through water** (`HorseWaterRidingHandler` feel).
  - the **roped-horse right-click** portal shortcut.
  - the instanced-plot dimension basics that predate this session (void outside
    walls, random-Y plots, no pop-in, teardown on leave) - last owner-confirmed
    long ago, before the reworks.
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
    roll) -> `breeding.md`.
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

**Alleles are objects now, not string positions.** A `Genotype` is one
`AllelePair` per registered `Gene` (`common/genetics/`). It still round-trips
through a compact **code string** (2 symbols per gene, in `Genes.codeOrder()`)
for persistence / sync / the pedigree record - `Genotype.parse(code)` /
`toCode()`. Shorter legacy codes still parse: a missing trailing locus is read
as that gene's wild-type (`"EeAawwtt"` -> `"EeAawwttcc"`, `"EeAaWw"` -> add
`"ttcc"`, `"EeAa"` -> add `"wwttcc"`).

Built-in genes (namespace `horsegenetics` = the `<modauthor>`; a third party
registers under its own). Order below is `Genes.codeOrder()` - **append** new
genes so legacy codes keep padding correctly:

| gene | key | alleles (symbols) | wild-type | notes |
|------|-----|-------------------|-----------|-------|
| Extension | `horsegenetics.extension` | `E` / `e` | `E` | `ee` = chestnut (all black pigment restricted) |
| Agouti | `horsegenetics.agouti` | `A` / `S` (=A^t seal) / `a` | `a` | `A_` bay, `S_` seal - **both non-deterministic** |
| White | `horsegenetics.white` | `W` / `w` | `w` | `W_` dominant white - full restriction, transparent overlay |
| Test | `horsegenetics.test` | `T` / `t` | `t` | diagnostic - paints the `TestCoatPattern` gradient |
| Champagne | `horsegenetics.champagne` | `C` (=Ch) / `c` | `c` | dominant dilution -> the gradient's gold column |

`Genotype.phenotype()` still returns the coarse `CoatPhenotype`
(`CHESTNUT`/`BLACK`/`BAY`/`WHITE`; seal and champagne fold into the nearest) -
only used now for **foal `*_baby` textures** and family-tree fallback.

`Genotype.random(rng)` - each gene rolls its own pair (see the gene class for
draw count / order); `codeOrder()` overall. Wild frequencies:
`WhiteGene.WILD_WHITE_ALLELE_ODDS` = **50** per allele (~4% white),
`TestGene.WILD_TEST_ODDS` = **4** (one roll -> `Tt`, ~25% carriers, deliberately
high), `ChampagneGene.WILD_CHAMPAGNE_ALLELE_ODDS` = **40** per allele, agouti
roughly 45% `A` / 45% `a` / 10% `S` per allele. `breedWith` = **2
`nextBoolean()` per gene** (child allele from each parent), `codeOrder()`.

`Gene.isVisible(pair, genotype)` / `isDeterministic(pair, genotype)` take the
**whole genotype** (agouti is invisible on a chestnut - no black to restrict).
`Genotype.hasVisibleNonDeterministic()` = "the coat texture must be generated
per horse" (any bay / seal / marking); otherwise it's one of a shared set.

## The coat overlay pipeline (`common/coat/pattern/` + `client/GeneticCoatTextureFactory`)

Coats are **generated**, not picked from vanilla PNGs. Every pixel starts at
"max red + max black pigment" (a black horse); genes restrict pigment down; the
survivors are looked up in a colour gradient; the result is multiplied onto the
white-horse template.

- **`CoatData`** = `Genotype` + a `long epigeneticSeed`, rolled **once at
  birth** by `CoatGenerator.generate` and persisted. Deterministic coats ignore
  it (all identical -> shared texture); non-deterministic coats feed it into
  each gene's own `SeededRng` so the same horse regenerates the same skin.
  `CoatData.textureKey()` = code, plus `@<seed>` only when non-deterministic.
- **`CoatTextureComposer.compose(genotype, seed, template, GradientLut)`**
  (pure, 128px `int[]` ARGB):
  1. **restrict** - `Genes.restrictionOrder()` (`extension, agouti, champagne,
     white`), each visible gene mutates the shared `PigmentField` (per-texel
     `red` / `black` in `[0,1]`).
  2. **resolve** - each mapped texel's `(red, black)` -> `GradientLut.sample`
     -> ARGB; a fully-restricted texel becomes transparent.
  3. **paint** - `Genes.paintOrder()` (`test`) draws ARGB straight over the
     resolved overlay.
  4. **multiply** overlay x template per channel, **keeping the template's
     alpha** (silhouette stays exactly the vanilla white horse).
  5. **eyes** - copied verbatim from the template (`CoatRegions.EYE_RECTS`).
- **`GradientLut`** wraps `assets/horsegenetics/textures/coat/redblackgradient.png`
  (hand-authored, 500x500): **left** = more red pigment, **bottom** = more
  black; `(1,1)` bottom-left = black, `(1,0)` top-left = chestnut, `(0,0)`
  top-right = white, and a **champagne-gold column** near the horizontal middle.
  `sample(red, black)`: `x = (1-red)*(w-1)`, `y = black*(h-1)`, bilinear.
- **`CoatRegions`** - reusable region helpers (paint / restrict a `Part`, the
  hooves / mane / tail / ears, `blackenLowerLeg` up to a fraction,
  `blackenFace`, `redrawEyes`).
- **`BayCoat.apply(ctx, epiRng)`** - the bay generator: knock body black down
  to `BODY_BLACK` (0.32, red kept), hard-black the mane / tail / ears, black up
  each leg + the face a random amount (per-leg fractions + a face fraction from
  the epigenetic RNG), hooves always black. `AgoutiGene` calls it for `A_`;
  `S_` gets an inline "tan creeps up the lower legs" pass.
- **Genes**: `ExtensionGene` (`ee` -> black = 0 everywhere), `WhiteGene` (`W_`
  -> red = black = 0 -> transparent), `ChampagneGene` (`Ch_` -> red ~= 0.5,
  black *= 0.18 -> the gold column), `TestGene` (`T_` -> paint the
  `TestCoatPattern` gradient over everything).
- **`GeneticCoatTextureFactory`** (client) loads the template + gradient once,
  runs `compose`, uploads a `DynamicTexture`, caches by `textureKey()`. Cleared
  on world exit. **Every adult** now renders with `HdHorseModel` + a generated
  texture; foals keep vanilla `*_baby` by `phenotype()`.
- Dev tool: `./gradlew :common:bakeCoatSamples` renders a strip of sample coats
  through the real pipeline to `build/coat-samples/` (no game launch).

### The HD horse model (`client/HdHorseModel`)

128px, fully non-mirrored UV layout - a structural copy of vanilla
`AbstractEquineModel.createBodyMesh` with two changes: every cube passes
`texScale = 0.5` and the layer bakes at 128x128 (`ClientSetup.HD_HORSE`), so
`CubeDefinition.bake` -> effective texture size `128*0.5 = 64` and **every
normalized UV is identical to vanilla** - the 2x sheet just gives each face 2x
the texels. The four legs and two ears get their own `texOffs` and **drop
`.mirror()`**. `horse_white.png` (in `common/.../assets/`) is the vanilla white
scaled 2x with the extra leg / ear patches seeded from vanilla;
`horse_white_vanilla64.png` is the untouched reference. Leg unwrap 32x30,
ear 12x8. Assets live in `common/` (portable) and
`neoforge-26.1.2/build.gradle`'s `processResources` folds `assets/**` in.

### `common/coat/skin/HorseSkinGeometry` - the body-space projection engine

Body-space grid (owner-verified in-game 2026-08-31 as smooth / seamless):
**X** 0 at the tail's rear edge -> +nose, **Y** 0 at the hoof bottom -> +up,
**Z** 0 at centre, **+Z = horse's right**; units are model units
(1 = 1/16 block = 2 texels). Each `Part` is an axis-aligned box with six
`Face`s (NOSE/TAIL span (Z,Y); TOP/BOTTOM span (X,Z); RIGHT/LEFT span (X,Y)).
`project(part, face, a, b)` -> `Texel`; `sample(px, py)` -> `Sample(part, face,
BodyPoint)` (memoised full-sheet grid); `forEachTexel(...)` walks mapped
texels. One absolute scale across all parts, so a coat that's a function of X
is seamless. Geometry table is lifted from `HdHorseModel` / vanilla
`createBodyMesh` and must stay in sync; rotated parts (head / neck / muzzle /
mane / ears / tail) use their rest-pose AABB - face projection there is
approximate.

## Data flow (server -> client -> pixels)

1. Horse spawns/breeds -> `HorseRecord` attached (genetic code) and, on the
   same join, a `HorseCoatAttachment` = `{genotype code, epigeneticSeed}`
   (`CoatGenerator.generate` rolls the seed; legacy saves with no seed get one
   backfilled on join). Both via `ModAttachments`.
2. Not auto-synced -> the handler sends `CoatSyncPayload` `{entityId, code,
   seed}` and `HorseRecordSyncPayload` to trackers.
3. Client caches in `ClientCoatCache` (`CoatData`) / `ClientHorseRecordCache`,
   cleared on `LoggingOut`.
4. `GeneticHorseRenderer.extractRenderState` reads `ClientCoatCache` ->
   `GeneticHorseRenderState.coatData`; `getTextureLocation` ->
   `GeneticCoatTextureFactory.getOrCreate(coatData)` (adult, generated) or the
   vanilla `*_baby` (foal). The renderer hands `HdHorseModel` to its super ctor
   as the adult model - no per-entity model swap any more.


## Horse stats (speed / health)

Domain side (roll band, record fields, breeding flow) is in **`breeding.md`**.
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
  drop). *Compiled; `dropLeash` runs in the evacuation path but the
  roped-horse shortcut isn't separately confirmed.*
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
built at a unique world X (`PLOT_SPACING_X` = 20 000 apart, X slots recycled
via a free-list) and a **random Y** (`PLOT_MIN_Y` + rand `PLOT_Y_RANGE`).
Plots never share chunks - "two players never land in the same place" holds
on a server with no real per-player-dimension work.

`PLOTS` is `Map<UUID player, Plot>`, strictly 1:1 - every `enter()` makes a
new plot and tears down that player's previous one, so a revisit always
regenerates and nothing left behind survives.

- `teleportAndGenerate(player)` (F6) -> `enter(player, player's current dim,
  player's current pos)`.
- `enter(player, returnDim, returnPos)` -> allocate plot, build lookahead,
  build the return portal, teleport to `(originX + 3.5, baseY + 1, 0.5)`
  facing +X, drop a **paper** in the first free hotbar slot.
- `leave(server, playerUUID)` -> `tearDown`: discard non-player entities in
  the plot AABB, air-fill every block, return the X slot. Fired from
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
  (corners get `up=true`, so a solid top).
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
- Each pen: **one mare + one stallion** (`spawnHorse(..., Sex)` pre-sets the
  founder record so `onHorseJoin` doesn't re-roll sex; `newFounder` also
  copies the entity's speed/health onto the record).
- Horses in `DEBUG_LEVEL` **take no damage**
  (`HorseGeneticsEventHandler.noHorseDamageInDebugDimension` cancels
  `LivingIncomingDamageEvent` for any `AbstractHorse`).
- `DEBUG_LEVEL` terrain is **read-only**: `noBlockBreakInDebugDimension`
  (`BreakBlockEvent`) and `noBlockPlaceInDebugDimension`
  (`BlockEvent.EntityPlaceEvent`, which also covers `EntityMultiPlaceEvent`)
  both cancel in that dimension. Bucket/fluid placement isn't covered yet.
- `HorseInteractionHandler` in `DEBUG_LEVEL`: **stick** tames an untamed
  horse, **clock** ages a foal to adult (`setAge(0)`).
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
**Still unverified**: the `PortalEventHandler.onRightClickBlock` roped-horse
shortcut, and whether the 10 s player dwell feels too long.

Known limitation: `PLOTS` is in-memory, so a server restart orphans blocks a
dead plot left in the void (harmless - 20k+ blocks away, and `onLogin` keeps
players out). `tearDown` air-fills the whole plot AABB (~150k `setBlock`)
each exit - fine for now.

## Known gaps / next steps

The 2026-08-30 session play-tested and signed off most of the portal / horse-
dimension / Family-Tree / info-panel rework (see the Status snapshot's
"Owner-verified" list). What still needs a `runClient` pass:

1. **`breedNth` foal names past the first foal** - breed one mare+stallion
   7+ times and confirm the sequence: foal 1 = parent combo, foal 2 = the other
   combo, foals 3-6 = one parent name half + a random word, foal 7+ = fully
   random - no repeats. Only foal 1 has been checked so far. The live link is
   `HorseAncestryData.offspringCount(dam, sire)` feeding
   `HorseNames.breedNth` in `HorseBreedingHandler`.
2. **`FamilyTreeScreen` scroll mode** - flip `familyTree.scrollBar` to `true`
   in `config/horsegenetics-client.toml` and check the wheel + right-edge thumb
   path (the default shrink-to-fit path is verified). Also: if the model should
   track the cursor a full 360deg instead of the ~±63deg swivel, switch
   `drawHorseModel` to an unbounded `atan2`.
3. **The allele/gene coat overlay pipeline in the client** - see the Status
   snapshot's "NOT verified" list: black / chestnut / champagne / white read
   right, bay + seal points + random heights, eyes survive, foals fine, the
   family-tree node, and **save -> reload keeps the epigenetic seed** (coat
   doesn't change). Tune targets: `BayCoat.BODY_BLACK`, the seal leg pass, the
   agouti wild frequency, the `redblackgradient.png` art itself. Also check
   **legacy saves** (old 8-char codes / no `epigenetic_seed`) load and get a
   seed backfilled on join.
4. **Stats surfaces** - foal speed/health landing on the entity's attributes;
   the inventory-panel green/amber/red tint vs `parentStats`; the paper dump's
   `vs parents` line; **legacy no-stat saves** still loading.
5. **Clock no longer mounts you** - clock on a tamed foal in the horse
   dimension should just age it up, not also seat you.
6. **Water riding** feel (`HorseWaterRidingHandler`); the **roped-horse
   right-click** portal shortcut.
7. **Coat follow-ups**: per-horse coat texture for **foals** (baby UV isn't in
   `HorseSkinGeometry`); white **markings** as a real non-deterministic gene
   (the framework's ready - a `paint`/`restrict` gene tagged non-deterministic);
   more loci (cream, dun, gray, roan); the overlay is a flat gradient sample
   per pixel today (no dappling / shading); `T` on a non-deterministic coat
   still generates a unique (identical-looking) texture per horse.
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
  documented **only** in `breeding.md`. When you change how records are
  assigned, how codes combine, how stats are rolled or applied, or how
  ancestry is tracked/shown, update `breeding.md` in the same change - a
  pointer from here or README is fine, a copy is not.
```
