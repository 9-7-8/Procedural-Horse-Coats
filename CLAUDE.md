# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses: a three-locus Mendelian genotype (extension + agouti +
white) drives coat phenotype; every horse also carries a name, a pedigree,
and rolled speed/health stats; and there's a self-contained "horse dimension"
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

- **`common/`** - compiles; **120 JUnit tests pass** (`./gradlew
  :common:test`). Covers `genetics/` (E/A/W loci, 6-char codes), `coat/`,
  `name/`, `horse/` (pedigree model + `HorseStats` + `ParentStats` ->
  `breeding.md`).
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes; only two `getGuiLeft/getGuiTop`
  deprecation warnings) against the real NeoForge `26.1.2.100` SDK.
- **`runServer`** - boots clean to `Done (...)! For help`; all dimensions,
  attachments, SavedData, payloads, the `hay_portal` block **and its block
  entity type** register with no errors.
- **`runClient`** - launched to the title screen in earlier sessions; F6 ->
  the horse dimension was owner-confirmed **long before** the reworks below.
  Not run since. **A user did try the hay-bale portal + carrot in-game and it
  did nothing** - the frame detector was reworked (try every air-neighbour of
  the clicked hay as a seed) but that fix is unverified. Activation was also
  changed **carrot -> golden carrot** afterwards.
- **Owner-verified in-game (2026-08-30):** golden-carrot portal activation;
  breeding (foal stats between parents, correct genetics, inherited names); the
  **info panel** (readable, buttons work, E no longer closes it while typing);
  the reworked **E/W pen-back walls**; **return teleport** (horses land beside
  the portal, unharmed); **no block break/place** in the horse dimension;
  `FamilyTreeScreen` **3D models** (right coat/pose).
- **NOT verified in-game (the backlog):**
  - the instanced-plot horse dimension (void outside walls, random-Y plots,
    teardown on leave, no pop-in), **brick walls + double gates + 6-wide
    pens**. E/W pen-back walls verified. Still to check: the **wall behind the
    return portal** - now a gravel *floor strip* + floating glowstone + single
    wood wall + bedrock, every column on a bedrock/dirt base, and the E/W
    walls' plank+bedrock now carried forward to `originX-1` to close the
    corner seam;
  - the **hay-portal animated texture** - `hay_portal.png` (64x2304, 36 frames);
    `HayPortalRenderer` draws a **half-block opaque slab** (0.25..0.75 along the
    `AXIS`, two faces only) - opaque black backing pass so sky/clouds/water
    don't show through, then an `entityTranslucentEmissive` swirl on top;
    `HayPortalClientAnim` ramps it **12 -> 48 fps** as the local player's dwell
    approaches teleport. Replaces the End-portal starfield. Watch for: wrong
    face winding (portal invisible / inside-out), UV rotation, ramp feel, slab
    thickness/position;
  - hay-bale portals: frame detection, dwell **swirl-particle + chat
    countdown**, plot linking, roped-horse right-click;
  - horse coats rendering per-genotype, incl. the new **WHITE** (`horse_white`
    texture); bay black on the legs vs the face;
  - `FamilyTreeScreen` - **compressed rows + scrollbar** (`ROW_SPACING` =
    `BOX_H+6`; scrolls with the wheel + a right-edge thumb when 8
    great-grandparent rows don't fit; content scissored to `[VIEW_TOP,
    height-VIEW_BOTTOM_MARGIN]`). Models turn to **face the cursor**
    (`atan((cx-mouseX)/30) * 42deg` yaw) and `MODEL_LIFT` = 16 keeps feet in
    the row slot. Confirm nothing's cut off and the swing/scroll feel right;
  - rolled speed/health landing on foal attributes (band now
    `[0.75*min, 1.5*max]`, rounded up, uncapped);
  - "clock on a tamed foal no longer also mounts you" (cancel now fires
    client-side too);
  - tamed horses following the player out of the dimension on exit;
  - riding a tamed horse through water;
  - the breeding round-trip (see `breeding.md` -> "Not verified").
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
  - `name/` - `HorseNameGenerator` (+ `HorseNames.breed`) + word tables under
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
    `ModBlockEntities` + `HayPortalBlockEntity` (End-portal look).

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

Three loci, six alleles, one phenotype table. Genotype is a **6-char string**:
E-locus alleles, then A-locus, then W-locus, dominant written first per locus
(`Genotype.parse` / `Genotype.of` canonicalize). **Legacy 4-char codes**
(pre-W) still parse - the missing W locus is read as `ww`, so old saves are
fine.

| W locus | E locus  | A locus | Phenotype                       |
|---------|----------|---------|---------------------------------|
| W_      | any      | any     | **White** (`horse_white`) - masks everything |
| ww      | ee (any) | any     | Chestnut                        |
| ww      | E_       | aa      | Black                           |
| ww      | E_       | A_      | Bay (+ random `legBlackHeight`) |

`Genotype.random` rolls E/A 50/50 per allele and W as `1 in
WILD_WHITE_ALLELE_ODDS` (**50**) per allele - so wild whites are uncommon
(~4%). `breedWith` now does **6** `nextBoolean()` draws (E,E,A,A,W,W).
`CoatPhenotype` has a `WHITE` constant; `CoatData.solid(WHITE)` /
`CoatGenerator.generate` handle it (no leg roll). The renderer maps WHITE ->
`horse_white` / `horse_white_baby`.

`legBlackHeight` is a float in [0, 1], rolled once per bay horse at
genotype-assignment time via `CoatGenerator.generate`, then persisted -
**never re-roll it** on load or the coat visibly changes between sessions.
Applied uniformly to all four legs; per-leg variation is a future
enhancement, not a bug.

Wild horses roll an **independently random** genotype on spawn. Inheritance,
identity/sex/name/pedigree, **and the speed/health stat roll** live in the
two-layer record system - all documented in **`breeding.md`**. One-line
version: every horse carries a `HorseRecord` (id / sex / first+last+barn name
/ genetic code / parent ids / tamer / breeder / generation / speed / health /
parentStats) as a Data Attachment, mirrored into a per-world
`HorseAncestryData` SavedData, coat derived from `record.geneticCode()`,
synced to clients for the inventory panel + family tree.

## Data flow (server -> client -> pixels)

1. Horse spawns/breeds -> a `HorseRecord` is attached (see `breeding.md`);
   its `geneticCode` drives `CoatGenerator.generate`, stored via
   `ModAttachments.HORSE_COAT` (a Data Attachment with a `MapCodec`).
2. Attachments are **not** auto-synced, so the handler also sends a
   `CoatSyncPayload` (coat) and `HorseRecordSyncPayload` (record) to tracking
   players.
3. Client stores payloads in `ClientCoatCache` / `ClientHorseRecordCache`
   (keyed by entity id; record cache also by UUID) - rebuilt from network
   traffic each session, cleared on `LoggingOut` (`ClientLifecycleHandler`).
4. `GeneticHorseRenderer` reads `ClientCoatCache` in `extractRenderState`;
   for bay adults it calls `GeneticCoatTextureFactory` to composite a runtime
   texture from the vanilla bay + black textures. Foals use the vanilla
   `*_baby` texture for their phenotype (no leg compositing on the baby
   model).

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
  ownership and closes the image (don't also close it -
  `GeneticCoatTextureFactory.generate` keeps `base` out of its
  try-with-resources).
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
  `ScreenEvent.Init.Post#addListener` and `ScreenEvent.Render.Post#
  getGuiGraphics()` still bolt widgets/overlays onto vanilla screens.
  `AbstractContainerScreen#getGuiLeft()` / `getGuiTop()` are
  deprecated-for-removal but still work (used by `HorseScreenHooks`).
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
  - Geometry: draws **only the two faces on the portal's plane axis**, as a
    **half-block slab centred in the block** (`SLAB_MIN`..`SLAB_MAX` =
    0.25..0.75 along that axis) via `emitFace(dir, from, to, ...)` -> so it
    reads like a thin portal, not a cube. Winding: `right = up x normal`,
    corners BL/BR/TR/TL.
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
  `Set<Relative>`. *Not verified in-game for non-players.*
- **Leash**: `Mob implements Leashable`; `isLeashed()`, `getLeashHolder()`
  (-> `Entity`), `dropLeash()` (drops the lead item), `removeLeash()` (no
  drop). *Signatures compiled; not verified in-game.*
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
  rings the entity with `PORTAL` / `REVERSE_PORTAL` particles (denser as it
  nears zero) and a **player** gets a per-second countdown line in chat
  (`"Portal -> N seconds..."`, last 5 s) plus a one-time "grabs hold" message
  on tick 1. `LAST_COUNTDOWN` dedupes the lines. The dwell guard now accepts
  any `AbstractHorse`, not just `Horse`.
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

**Unverified in-game**: `Entity#teleportTo(ServerLevel, ...)` for non-players,
`Mob#getLeashHolder()` / `dropLeash()`, the dwell swirl particles + chat
countdown, and whether the dwell/teleport loop feels right; the animated-texture
portal (winding / UVs / fps ramp). **Verified 2026-08-30:** frame flood-fill +
golden-carrot activation; return teleport (horses land beside the portal
unharmed).

Known limitation: `PLOTS` is in-memory, so a server restart orphans blocks a
dead plot left in the void (harmless - 20k+ blocks away, and `onLogin` keeps
players out). `tearDown` air-fills the whole plot AABB (~150k `setBlock`)
each exit - fine for now.

## Known gaps / next steps

Nearly everything below needs a `runClient` session with a player in-world;
`build` + `runServer` only prove it registers and boots.

1. **Hay-bale portal dwell + look** - ride the 10 s / 3 s dwell timers both
   directions: the **swirl particles thicken**, the **chat countdown** appears,
   and the **portal texture animates faster** the longer you stand in it (12 ->
   48 fps). Check the texture shows (not invisible / inside-out - if so reverse
   the winding in `HayPortalRenderer.emitFace`), isn't rotated oddly, that the
   sky / clouds / water no longer show through it, that it's a **thin centred
   slab** (~half a block), and that it looks right on a multi-block portal.
   Push a horse through, try the roped-horse right-click.
2. *(done)* Return teleport - horses land 5 up / beside the portal, unharmed.
3. **Walk a plot** (F6) - void outside the walls, no pop-in, one mare + one
   stallion per **6-wide brick-walled** pen with a **two-wide gate**, corner
   torches stick, paper in the hotbar, horses take no damage. E/W pen-back walls
   + block protection verified. Still to check: the **wall behind the portal**
   (gravel floor strip + floating glowstone + one wood wall + bedrock, **no
   dark band / missing blocks**) and that the **E/W walls now meet it with no
   gap at the corner**. Leave and re-enter - old corridor gone, tamed horses
   you own came out with you.
4. **WHITE coat** - a `W_` horse renders `horse_white` (adult + `*_baby`);
   breed `Ww x ww` and check ~half the foals are white.
5. **Stats** - foal roll + genetics + name inheritance is **owner-verified**
   (2026-08-30). Still to eyeball in-game: foal stats landing on the entity's
   attributes, the inventory panel tinting speed/health green/amber/red vs
   `parentStats`, the paper dump `vs parents` line, and that old 4-char-code /
   no-stat saves still load.
6. **Clock no longer mounts you** - clock on a tamed foal in the horse
   dimension should just age it up.
7. **`FamilyTreeScreen`** - *(models' coat/pose + cursor-following verified.)*
   This session: rows compressed to `ROW_SPACING = BOX_H+6`, and the whole
   chart **scrolls** (wheel + right-edge thumb) when 8 great-grandparent rows
   don't fit; content is scissored to `[VIEW_TOP, height-VIEW_BOTTOM_MARGIN]`;
   `MODEL_LIFT` bumped to 16 so feet stay in the slot. Confirm nothing is cut
   off at the bottom now, the scrollbar behaves, and re-centring resets scroll.
   If the horse should track the cursor a full 360deg rather than swivel
   ~±63deg, switch `HorseScreenHooks`... er, `FamilyTreeScreen.drawHorseModel`
   to an unbounded `atan2`.
8. **Water riding**, **bay leg texture** (`LEG_REGIONS = {48,25,16,11}`),
   **breeding round-trip** (`breeding.md` -> "Not verified") - as before.
9. **Cleanups**: rename `DebugPenManager` / `DEBUG_LEVEL` /
   `horsegenetics:debug_pens` to non-"debug" names (needs a save-data
   migration or a one-time reset); fold speed/health/white into one Mendelian
   model; per-leg `legBlackHeight`; more loci (cream, dun, gray); a
   `.gitignore`; name-generation rework; real white-fog dimension effects
   (needs a client dimension-effects mixin); the stray `neoforge.mods.toml`
   duplicate. (The portal now uses the user's own animated `hay_portal.png`, so
   "gold instead of purple" is moot.)

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
