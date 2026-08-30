# Horse Genetics - NeoForge 26.1.2 Mod

Procedural coat generation for horses, starting with basic Mendelian
genetics (extension + agouti loci) driving coat phenotype, with an eye
toward eventually backporting to 1.12.2.

## Status snapshot (keep this current)

- **`common/`** - compiles; **63 JUnit tests pass** (`./gradlew :common:test`).
  Genetics logic (`Genotype`, `CoatPhenotype`, `CoatData`, `CoatGenerator`)
  is verified against the phenotype table below.
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes) against the real NeoForge `26.1.2.100`
  SDK. The class set was originally written blind against docs; this session
  ported every file to the actual API - see "26.1.2 API notes".
- **`runClient`** - launches to the title screen. Mod loads, all six
  `@EventBusSubscriber` handlers register, GL + sound + resource reload come
  up clean.
- **`runServer`** - boots to `Done (...)! For help`, and
  `horsegenetics:debug_pens` comes up as a live `ServerLevel` (both debug
  dimension JSONs parse under this SDK).
- **NOT yet verified end-to-end:** the actual in-world experience -
  Singleplayer -> create world -> **F6** -> horses render with genotype
  coats in the pens. No automated check can drive that; it needs a human at
  the keyboard. The crash that used to block world creation (a stale
  `dimension_type` JSON) is fixed.
- **Machine caveat (this dev laptop):** hybrid graphics (NVIDIA RTX 3050 Ti
  + AMD integrated). `java.exe`/`javaw.exe` had to be pinned to the NVIDIA
  GPU and the FML splash disabled or the JVM hard-crashes in the AMD driver.
  See "Running the game".

## Architecture - read this before editing anything

Two-module Gradle project, split deliberately:

- **`common/`** - pure Java, zero Minecraft/NeoForge imports of any kind.
  Contains `Genotype`, `CoatPhenotype`, `CoatData`, `CoatGenerator`, `Rng`.
  This module is the part that survives a version port unchanged - if you
  find yourself wanting to import anything Minecraft-related here, stop:
  that logic belongs in the version module instead.
- **`neoforge-26.1.2/`** - everything Minecraft-specific: Data Attachments
  for persistence, custom packets for client sync, `NativeImage`-based
  runtime texture compositing for rendering, event handlers for spawn-time
  genotype assignment. Organized by concern: `client/`, `data/`, `network/`,
  `server/`.

When adding a feature, default to putting as much logic as possible in
`common/` and keeping the NeoForge module as thin plumbing around it. That
is what makes a future `forge-1.12.2/` module cheap to add.

## Build & test

```bash
./gradlew :common:test               # pure-Java genetics logic, no Minecraft - fastest loop
./gradlew :neoforge-26.1.2:build     # full compile + jar; slow first run (downloads the SDK)
./gradlew :neoforge-26.1.2:runClient # launch the game with the mod loaded
./gradlew :neoforge-26.1.2:runServer # headless dedicated server (DEBUG logging - huge log)
```

Run `:common:test` first when iterating on genetics - it doesn't touch
Minecraft at all.

Requires JDK 25 (the SDK's requirement). The `foojay-resolver-convention`
plugin in `settings.gradle.kts` auto-provisions it if missing.

## The genetics model, as implemented

Two loci, four alleles, one phenotype table. Genotype is a 4-character
string: E-locus alleles then A-locus alleles, dominant allele written first
per locus (`Genotype.parse` / `Genotype.of` canonicalize this automatically).

| E locus  | A locus | Phenotype                       |
|----------|---------|---------------------------------|
| ee (any) | any     | Chestnut                        |
| E_       | aa      | Black                           |
| E_       | A_      | Bay (+ random `legBlackHeight`) |

`legBlackHeight` is a float in [0, 1], rolled once per bay horse at
genotype-assignment time via `CoatGenerator.generate`, then persisted -
**never re-roll it** on world load or the horse's coat visibly changes
between sessions. It's applied uniformly to all four legs currently; per-leg
variation is a reasonable future enhancement, not a bug to fix.

Every horse currently gets an independently random genotype on spawn
(`HorseGeneticsEventHandler`, listening for `EntityJoinLevelEvent` on any
`Horse`). Real inheritance (combining two parents' alleles on breeding) is
not implemented - see "Known gaps".

## Data flow (server -> client -> pixels)

1. Horse spawns -> `HorseGeneticsEventHandler` rolls a `Genotype`, calls
   `CoatGenerator.generate`, stores the result via `ModAttachments.HORSE_COAT`
   (a NeoForge Data Attachment with a `MapCodec`).
2. Attachments are **not** auto-synced to clients, so the handler also sends
   a `CoatSyncPayload` to tracking players.
3. Client stores incoming payloads in `ClientCoatCache` (keyed by entity id)
   - a workaround, not a persistence layer; rebuilt from network traffic
   every session.
4. `GeneticHorseRenderer` reads `ClientCoatCache` in `extractRenderState`,
   and for bay horses calls `GeneticCoatTextureFactory` to composite a
   runtime texture from the vanilla bay + black textures.

## 26.1.2 API notes (what the port actually required)

This SDK is further from mainline 1.21.x than the version numbers suggest.
Differences from older NeoForge / Yarn-era assumptions:

- **`ResourceLocation` is now `net.minecraft.resources.Identifier`** - same
  surface (`withDefaultNamespace`, `fromNamespaceAndPath`), different name.
  Touches most Minecraft-facing files.
- **Horse classes moved** from `net.minecraft.world.entity.animal.horse` to
  `net.minecraft.world.entity.animal.equine` (`Horse`, `Variant`,
  `Markings`, `AbstractHorse`).
- **`HorseRenderer` is `final`.** `GeneticHorseRenderer` extends
  `AbstractHorseRenderer<Horse, HorseRenderState, HorseModel>` and copies
  vanilla `HorseRenderer`'s constructor body (models + marking / equipment
  layers) verbatim. It keeps the render-state generic as vanilla
  `HorseRenderState` so those copied layers type-check;
  `createRenderState()` covariantly returns the `GeneticHorseRenderState`
  subclass, and `EntityRenderer.createRenderState(entity, partialTick)`
  always routes through it, so every state instance really is the subclass.
- **`getTextureLocation(S)` is still the texture hook** - the
  `submit()` / `SubmitNodeCollector` split did not swallow it for entities.
- **`EventBusSubscriber` dropped `bus()` and `Bus`.** Routing is automatic:
  `IModBusEvent` subtypes go to the mod bus, everything else to the game bus.
- **`AttachmentType.Builder#serialize` takes a `MapCodec`**, not a `Codec` -
  `HorseCoatAttachment` exposes `MAP_CODEC` (via
  `RecordCodecBuilder.mapCodec`) with `CODEC = MAP_CODEC.codec()` kept for
  other callers.
- **`KeyMapping`'s conflict-context constructor takes a
  `KeyMapping.Category`** (a record keyed by an `Identifier`), not a lang-key
  string. The debug keybind reuses the built-in `KeyMapping.Category.MISC`.
- **`EntityType#create` needs an `EntitySpawnReason`** -
  `create(level, EntitySpawnReason.COMMAND)` for the debug-pen spawns.
- **`DynamicTexture(NativeImage)` is gone** - use
  `DynamicTexture(Supplier<String> label, NativeImage image)`. The texture
  takes ownership of the image and closes it, so don't also close it
  yourself (`GeneticCoatTextureFactory.generate` keeps `base` out of its
  try-with-resources for this reason).
- **Serverbound packets**: `PacketDistributor.sendToServer` moved to
  `net.neoforged.neoforge.client.network.ClientPacketDistributor`. The
  `sendToPlayer` / `sendToPlayersTrackingEntity` server-side helpers stayed
  on `PacketDistributor`.
- **`ServerPlayer` has no `getServer()`** in this SDK - reach the server via
  `((ServerLevel) player.level()).getServer()`.
- **`dimension_type` JSON schema changed heavily.** `data/horsegenetics/
  dimension_type/debug_pens.json` had to be rewritten. Gone: `ultrawarm`,
  `natural`, `piglin_safe`, `respawn_anchor_works`, `bed_works`,
  `has_raids`, `fixed_time`, `effects`. New **required** key:
  `has_ender_dragon_fight` (bool). `effects` -> `skybox`
  (`"none"` / `"overworld"` / `"end"`). Time-of-day control moved to a new
  WorldClock/Timeline registry system - the debug dimension has a normal
  day/night cycle for now (permanent noon would need `default_clock` /
  `timelines`). A stale `dimension_type` JSON fails `RegistryDataLoader` and
  takes down the whole client (`ReportedException` -> `Stopping!`) the
  moment you create a world - i.e. it reads as "singleplayer is broken".
  The flat-generator `dimension/debug_pens.json` schema was unchanged.

## Build setup notes

- `common/build.gradle.kts` declares its own `repositories { mavenCentral() }`
  and `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` - both
  required under Gradle 9 (`settings.gradle.kts` only configures
  `pluginManagement` repos, and Gradle 9 no longer auto-adds the JUnit
  Platform launcher to the test runtime classpath).
- The mod metadata is `src/main/resources/META-INF/neoforge.mods.toml`. It
  declares `minecraft` dependency `versionRange = "[26.1.2,)"` - the MDK
  template's `[1.26.1,1.27)` does **not** match this SDK's Minecraft version
  string `26.1.2`, so that range makes FML refuse to load the mod. There is
  a stray, un-packaged duplicate at `src/main/neoforge.mods.toml` (wrong
  directory) left over from a `git mv`; delete it or move it under
  `resources/META-INF/` - don't let the two drift.
- There is no `.gitignore`. `build/`, `run/`, `.gradle/`, `.idea/` all show
  as untracked/modified noise. Worth adding one.

## Running the game

`./gradlew :neoforge-26.1.2:build` only assembles the jar. `runClient` /
`runServer` launch Minecraft `26.1.2.100` with the mod. In IntelliJ, a
Gradle sync generates **runClient** / **runServer** run configs.

Two machine-specific launch blockers were hit on this dev laptop (hybrid
graphics: NVIDIA RTX 3050 Ti + AMD integrated, AMD driver from 2023):

- **JVM hard-crash (`EXCEPTION_ACCESS_VIOLATION` in `atio6axx.dll`) at
  `glfwCreateWindow`.** The process ran on the old AMD integrated driver,
  which segfaults on GL window creation. Fix: Windows per-app GPU preference
  pinning `java.exe` / `javaw.exe` to "High performance"
  (`HKCU\Software\Microsoft\DirectX\UserGpuPreferences`, value
  `GpuPreference=2;` - same as Settings > System > Display > Graphics). This
  is the durable fix; updating the AMD driver would also resolve it.
- **FML early-loading splash window** hits the same crash one step earlier.
  `run/config/fml.toml` has `earlyWindowControl = false` to skip it. With
  `java.exe` pinned to the NVIDIA GPU this can probably go back to `true`;
  it's left off because the splash is purely cosmetic. `run/` is not tracked
  - if it's wiped, this resets to `true`.

## Debug tool: dev-only horse pen generator

Press **F6** in a dev environment (`runClient`) to teleport into
`horsegenetics:debug_pens` (a dedicated flat dimension) and generate 20x20
fenced pens along +X, each with two horses, forever, as you walk. Fastest
way to eyeball a wide spread of genotypes/coats at once.

Flow: `DebugKeyHandler` (client tick) -> `RequestDebugPensPayload` ->
`ModNetworking` server handler -> `DebugPenManager.teleportAndGenerate` ->
`getLevel(DEBUG_LEVEL)` + `player.teleportTo(...)`. Pens are filled via
plain `addFreshEntity`; `HorseGeneticsEventHandler` assigns those horses a
genotype the same way it would a wild spawn.

Gated to never exist in a production build: `DebugKeyBindings` only
registers the keybind if `!FMLEnvironment.isProduction()`, and
`ModNetworking`'s server-side handler re-checks the same flag independently,
so a forged packet against a production server still no-ops. Do not remove
or relax these checks "just for testing" without restoring them.

Known limitation: the pen-generation progress counter is in-memory only
(`DebugPenManager`, static field) and resets on server restart -
`buildPen` checks for existing horses before spawning, so a restart just
re-places fence blocks that are already there. Accepted tradeoff for a
debug tool; persist via `SavedData` only if it becomes annoying.

## Known gaps / next steps

1. **Drive the in-world flow once** - Singleplayer -> world -> F6 -> confirm
   horses appear in pens with visibly varied coats, no crash after a minute.
   This is the one thing standing between "compiles + boots" and "minimum
   stable version".
2. **`GeneticCoatTextureFactory.LEG_REGIONS`** - still placeholder pixel
   rectangles. Open `horse_brown.png` / `horse_black.png` (64x64, in the
   client jar) in an image editor and fill in where the four legs sit on the
   UV map. Everything else in that class is wired correctly; until then a
   bay horse composites *a* texture, just not a leg-shaped one.
3. **Real inheritance** - breeding still rolls an independent random
   genotype per foal. Combining two parents' alleles is a `common/` change
   plus one horse-breeding event hook on the NeoForge side.
4. **Rendering not visually confirmed** - the renderer, texture compositing
   and packet round-trip compile and the pieces are traced, but no one has
   watched a bay horse's legs on screen yet.
5. Per-leg `legBlackHeight` variation, more loci (cream, dun, gray), and
   permanent-noon in the debug dimension are all later polish.

## License

CC BY-NC 4.0 (see `LICENSE`). In practice: forks/derivatives are welcome
without asking permission, but must credit the original repo and link back,
and no portion of this code may appear in a derivative that's sold with no
free version available. Donations/tips on an otherwise-free derivative are
fine. If ever asked to add or vendor third-party code, check its license is
compatible first.

## Conventions

- Keep `common/` free of Minecraft imports - a hard rule, not a preference.
- New version-specific logic goes in `neoforge-26.1.2/`, organized by
  concern: `client/`, `data/`, `network/`, `server/`.
- Flag genuinely unverified/unconfirmed API usage in comments the way the
  existing code does - more useful to future sessions than silent confidence.
- When you resolve something flagged here as unverified or a "known gap",
  update this file in the same change.
