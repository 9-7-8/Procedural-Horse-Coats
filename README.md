# Horse Genetics

Procedural coat generation for horses, built for NeoForge 26.1.2 with an eye
toward backporting to 1.12.2 later.

## Why it's split this way

```
common/              <- pure Java. No Minecraft, no NeoForge, no imports
                        beyond the JDK. This is the part that survives a
                        version port unchanged.
neoforge-26.1.2/      <- everything Minecraft-specific: persistence,
                        networking, rendering. This is the part you rewrite
                        per version.
```

When you eventually backport to 1.12.2, you copy `common/` into the new
project as-is and write a new `forge-1.12.2/` module that implements the
same handful of responsibilities (assign a genotype, persist it, get it to
the client, turn it into pixels) using 1.12.2's APIs (Forge Capabilities
instead of Data Attachments, `IExtendedEntityProperties` or capabilities for
storage, immediate-mode GL for texture work instead of NativeImage/
DynamicTexture). The genetics math and the coat-generation rule itself never
change.

## What's genuinely implemented vs. placeholder

**Solid, should work as written:**
- `common/` in full - `Genotype`, `CoatPhenotype`, `CoatData`, `CoatGenerator`.
  This is real, testable logic with no external dependency. Worth writing a
  few JUnit tests against directly (parse/random/phenotype-derivation).
- The overall data flow: server rolls genotype → attachment persists it →
  packet syncs it to tracking clients → client caches it → renderer reads it.
- `HorseCoatAttachment` + `ModAttachments` - Data Attachments with a Codec is
  the current (26.1) NeoForge-correct way to attach and persist data on an
  entity you don't own.

**Placeholder - needs your attention before this looks right in-game:**
- `GeneticCoatTextureFactory.LEG_REGIONS` - fake pixel rectangles. Open
  `horse_brown.png` / `horse_black.png` (both ship inside the client jar,
  64x64) in an image editor, find where the four legs actually sit on the
  UV map, and replace the placeholder coordinates.
- `GeneticHorseRenderer`'s reliance on `HorseRenderer` being generic enough
  to swap in a custom render state via `createRenderState()` - flagged
  inline. If vanilla's `HorseRenderer` turns out to hard-code
  `HorseRenderState`, you'll need to fully reimplement it instead of
  subclassing (copy vanilla's source, swap the texture line).
- The `build.gradle.kts` for the NeoForge module is written from the general
  shape of the ModDevGradle plugin, not verified against the real template.
  Diff it against `https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle`
  before trusting it to build.
- Entity rendering has kept changing release over release (the
  `submit()`/`SubmitNodeCollector` split that's landed for block entities);
  double-check the current NeoForged docs for whether `HorseRenderer` still
  exposes a simple `getTextureLocation()` override point in 26.1.2 specifically.

## The genetics, as implemented

Two loci, four alleles, one phenotype table:

| E locus | A locus | Phenotype |
|---|---|---|
| ee (any) | any | Chestnut |
| E_ | aa | Black |
| E_ | A_ | Bay (+ random leg black height) |

Genotype is stored as a 4-character code: E-locus alleles then A-locus
alleles, dominant first (`"EeAa"`, `"eeaa"`, `"EEAA"`, etc).

Bay horses get one extra value: `legBlackHeight`, a float in [0, 1] rolled
once at genotype-assignment time and persisted - not re-rolled on reload.
0.0 means the black barely creeps above the hoof; 1.0 means it comes all
the way up the leg. It's applied uniformly to all four legs for now;
per-leg variation is a natural next step once this is confirmed working.

## If you're merging this with a real MDK checkout

`neoforge-26.1.2/build.gradle` and the root `gradle.properties` here are
now based on the real values from NeoForge's official
`MDK-26.1.2-ModDevGradle` template (plugin version `2.0.144`, neo version
`26.1.2.100`) rather than guessed ones. If you cloned that template
separately, don't just drop its files at your repo root - its `build.gradle`
and `settings.gradle` assume a single-module project, which conflicts with
the `:common` / `:neoforge-26.1.2` split here. Merge the *values* (plugin
version, `neo_version`, the `foojay-resolver-convention` line in
`settings.gradle.kts`) into the files in this project instead of copying
the template's files over top of these.

Note `common/build.gradle.kts` is Kotlin DSL while
`neoforge-26.1.2/build.gradle` is Groovy DSL (matching the real MDK
template) - Gradle supports mixing DSLs per-module in a multi-module build,
so this isn't a problem, just slightly inconsistent to look at.

## Debug tool: infinite horse pens (dev-only)

Press **F6** while playing in a dev environment (`runClient`) to teleport
into a dedicated flat dimension (`horsegenetics:debug_pens`) and start
generating 20x20 fenced pens along +X, one gate each, a 10-block walkway
between them, extending forever as you walk. Each pen spawns two horses via
plain `addFreshEntity` - no special-cased genetics code was written for
them; `HorseGeneticsEventHandler` already listens for any horse joining any
level, so it assigns them a genotype the same way it would a wild-spawned
horse. This is the fastest way to eyeball a wide spread of genotypes/coats
at once.

**This is genuinely hidden in a production build, not just hard to find:**
the keybind is only registered inside `RegisterKeyMappingsEvent` if
`!FMLEnvironment.isProduction()`, so in a real jar it never appears in
Controls and can't be triggered. The server-side packet handler re-checks
the same flag independently, so a forged network packet against a
production server still no-ops. The only trace left in a shipped jar is two
harmless lang-file strings (`key.horsegenetics.debug_pens` and its
category) that are never displayed because the keybind object is never
created - cosmetic leftover, not a functional exposure.

**Known limitation:** the "how far have I generated" counter is in-memory
only and resets on server restart. Re-entering after a restart will re-run
generation starting from pen 0, which harmlessly re-places fence blocks
that are already there - `buildPen` checks for existing horses in a pen's
bounds before spawning more, so you won't get duplicate horses. If you want
this to survive restarts cleanly, the fix is to persist the counter via a
`SavedData` instead of a static field - not done here since a debug tool
resetting on restart is a reasonable default.

**Also flagged as unverified**, consistent with the caveats above: the
exact class name for the client tick event (`ClientTickEvent.Post`) - tick
event naming has shifted across NeoForge versions before, so confirm it
against current docs if it doesn't compile.

## Suggested next steps, in order

1. Set up the actual Gradle project (verify the build script against the
   real MDK), get `common/` compiling and write a few JUnit tests against
   `Genotype` and `CoatGenerator` directly - this validates the genetics
   logic without touching Minecraft at all.
2. Get the NeoForge module compiling. Expect the render-state generics
   issue flagged above to be the first real snag.
3. Fill in real `LEG_REGIONS` coordinates and confirm a bay horse in-game
   actually shows varying leg black height.
4. Breeding: right now every horse gets an independently random genotype on
   spawn. Real inheritance (combine two parents' alleles on breed) is the
   next feature, and it's entirely a `common/` change plus one new hook in
   the horse-breeding event on the NeoForge side.

<!-- FILE_LIST:START -->
## Repository file listing

_Auto-generated by generate_file_list.py - do not edit by hand, just re-run the script._

```
Procedural-Horse-Coats/
├── common/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── horsegenetics/
│   │                       └── common/
│   │                           ├── coat/
│   │                           │   ├── CoatData.java
│   │                           │   └── CoatGenerator.java
│   │                           ├── genetics/
│   │                           │   ├── CoatPhenotype.java
│   │                           │   └── Genotype.java
│   │                           └── Rng.java
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── neoforge-26.1.2/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── horsegenetics/
│   │       │               └── neoforge/
│   │       │                   ├── client/
│   │       │                   │   ├── ClientCoatCache.java
│   │       │                   │   ├── ClientSetup.java
│   │       │                   │   ├── DebugKeyBindings.java
│   │       │                   │   ├── DebugKeyHandler.java
│   │       │                   │   ├── GeneticCoatTextureFactory.java
│   │       │                   │   ├── GeneticHorseRenderer.java
│   │       │                   │   └── GeneticHorseRenderState.java
│   │       │                   ├── data/
│   │       │                   │   ├── HorseCoatAttachment.java
│   │       │                   │   └── ModAttachments.java
│   │       │                   ├── network/
│   │       │                   │   ├── CoatSyncPayload.java
│   │       │                   │   ├── ModNetworking.java
│   │       │                   │   └── RequestDebugPensPayload.java
│   │       │                   ├── server/
│   │       │                   │   ├── DebugPenManager.java
│   │       │                   │   ├── DebugPenTickHandler.java
│   │       │                   │   └── HorseGeneticsEventHandler.java
│   │       │                   ├── HorseGenetics.java
│   │       │                   └── NeoRng.java
│   │       └── resources/
│   │           ├── assets/
│   │           │   └── horsegenetics/
│   │           │       └── lang/
│   │           │           └── en_us.json
│   │           ├── data/
│   │           │   └── horsegenetics/
│   │           │       ├── dimension/
│   │           │       │   └── debug_pens.json
│   │           │       └── dimension_type/
│   │           │           └── debug_pens.json
│   │           └── META-INF/
│   │               └── neoforge.mods.toml
│   └── build.gradle
├── generate_file_list.py
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
├── settings.gradle.kts
└── TEMPLATE_LICENSE.txt
```
<!-- FILE_LIST:END -->
