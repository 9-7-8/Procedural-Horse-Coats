# Horse Genetics

Procedural coat generation for horses, built for **NeoForge 26.1.2**
(`neo_version` `26.1.2.100`, JDK 25), with an eye toward backporting to
1.12.2 later. Instead of shipping a PNG per phenotype, the mod assigns each
horse a genotype, derives a coat from it, and — for bay horses — composites
the leg pattern from vanilla textures at runtime.

## Status

| Piece | State |
|---|---|
| `common/` genetics logic | Compiles. **63 JUnit tests pass** (`./gradlew :common:test`). |
| `neoforge-26.1.2/` module | Compiles and assembles (`./gradlew :neoforge-26.1.2:build`). Ported from a docs-only draft to the real 26.1.2.100 API. |
| Client launch (`runClient`) | Boots to the title screen. Mod loads, all six event handlers register. |
| Dedicated server (`runServer`) | Boots to `Done!`. The `horsegenetics:debug_pens` dimension loads as a live level. |
| In-world (Singleplayer → world → **F6** → coats render in the pens) | **Not yet driven end-to-end.** Needs a human at the keyboard. The world-creation crash that used to block this is fixed. |

If you're picking this up: the immediate goal is a *minimum stable version*
— get into a world, press F6, watch horses spawn in the pens with varied
coats, no crash. Everything compiles and both entry points boot; that last
in-world lap is what's left.

See **`CLAUDE.md`** for the working notes: the full list of 26.1.2 API
differences the port had to absorb, the dev-machine GPU workaround, and the
per-file known gaps.

## Repo structure

```
horse-genetics/
├── settings.gradle.kts     <- declares the two modules + JDK auto-provisioning
├── gradle.properties       <- mod_id, versions, neo_version (26.1.2.100)
├── gradlew(.bat), gradle/wrapper/   <- wrapper, shared by both modules
├── common/                 <- pure Java, backports unchanged
│   ├── build.gradle.kts
│   └── src/{main,test}/java/...
└── neoforge-26.1.2/        <- everything Minecraft-specific
    ├── build.gradle        <- Groovy, from MDK-26.1.2-ModDevGradle (plugin 2.0.144)
    └── src/main/{java,resources}/...
```

There is deliberately no `build.gradle`, `settings.gradle`, or `src/` at the
repo root — those belong to the MDK template's single-module layout and
would fight the two-module split. If you re-clone the MDK for reference,
pull *values* out of it (plugin version, `neo_version`), not files.

## Why it's split this way

- **`common/`** — pure Java. No Minecraft, no NeoForge, no imports beyond
  the JDK. `Genotype`, `CoatPhenotype`, `CoatData`, `CoatGenerator`, `Rng`.
  This is the part that survives a version port unchanged.
- **`neoforge-26.1.2/`** — everything Minecraft-specific: persistence (Data
  Attachments), networking (custom payloads), rendering (`NativeImage` /
  `DynamicTexture` compositing), spawn-time event handlers. This is the part
  you rewrite per version.

When you backport to 1.12.2, you copy `common/` in as-is and write a new
`forge-1.12.2/` module implementing the same four responsibilities — assign
a genotype, persist it, sync it to the client, turn it into pixels — with
1.12.2 APIs (Forge Capabilities instead of Data Attachments, immediate-mode
GL instead of `NativeImage`). The genetics math and the coat rule never
change.

## The genetics, as implemented

Two loci, four alleles, one phenotype table:

| E locus  | A locus | Phenotype                       |
|----------|---------|---------------------------------|
| ee (any) | any     | Chestnut                        |
| E_       | aa      | Black                           |
| E_       | A_      | Bay (+ random leg black height) |

Genotype is stored as a 4-character code: E-locus alleles then A-locus
alleles, dominant first (`"EeAa"`, `"eeaa"`, `"EEAA"`). `Genotype.parse` /
`Genotype.of` canonicalize allele order.

Bay horses carry one extra value: `legBlackHeight`, a float in [0, 1] rolled
**once** at genotype-assignment time and persisted — never re-rolled on
reload. 0.0 = black barely above the hoof; 1.0 = black all the way up. It's
applied uniformly to all four legs for now; per-leg variation is a natural
follow-up.

Every horse currently gets an **independently random** genotype on spawn.
Real inheritance (combining two parents' alleles on breeding) is not
implemented yet — it's a `common/` change plus one horse-breeding event
hook.

## What's solid vs. placeholder

**Solid:**
- All of `common/` — real, tested logic with no external dependency.
- The data flow: server rolls genotype → attachment persists it → packet
  syncs it to tracking clients → client caches it → renderer reads it.
- `HorseCoatAttachment` + `ModAttachments` — Data Attachment with a
  `MapCodec` is the 26.1-correct way to attach/persist data on an entity you
  don't own.
- The NeoForge module against the real SDK — the renderer, keybind,
  networking, dimension, and attachment code all compile and the entry
  points boot. (`HorseRenderer` turned out to be `final`; `GeneticHorseRenderer`
  extends `AbstractHorseRenderer` and copies vanilla's constructor — see
  `CLAUDE.md`.)

**Placeholder / unverified:**
- `GeneticCoatTextureFactory.LEG_REGIONS` — fake pixel rectangles. Open
  `horse_brown.png` / `horse_black.png` (64x64, in the client jar) and fill
  in where the legs actually sit on the UV map. Until then a bay horse
  composites *a* texture, just not a leg-shaped one.
- Rendering has never been watched on screen. The pieces compile and are
  traced; nobody has seen a bay horse's legs yet.
- The debug dimension has a plain day/night cycle (this SDK moved
  time-of-day control to a new WorldClock/Timeline system; the old
  `fixed_time` field is gone).

## Debug tool: infinite horse pens (dev-only)

Press **F6** in a dev environment (`runClient`) to teleport into a dedicated
flat dimension (`horsegenetics:debug_pens`) and start generating 20x20
fenced pens along +X — one gate each, a 10-block walkway between — extending
forever as you walk. Each pen spawns two horses via plain `addFreshEntity`;
`HorseGeneticsEventHandler` assigns them genotypes exactly as it would a
wild spawn. Fastest way to eyeball a wide spread of coats at once.

**Genuinely hidden in a production build**, not just obscure: the keybind is
only registered when `!FMLEnvironment.isProduction()`, so in a real jar it
never appears in Controls and can't be triggered. The server-side packet
handler re-checks the same flag, so a forged packet against a production
server also no-ops. The only trace in a shipped jar is two unused lang
strings.

**Known limitation:** the "how far have I generated" counter is in-memory
only and resets on server restart. Re-entering re-runs generation from pen 0,
which harmlessly re-places existing fence blocks — `buildPen` checks for
horses already in a pen before spawning, so no duplicates. Persist via
`SavedData` only if it becomes annoying.

## Running the game — dev-machine note

On a hybrid-graphics laptop (NVIDIA + AMD integrated), the JVM can
hard-crash in the AMD OpenGL driver (`atio6axx.dll`) at `glfwCreateWindow`,
before the mod even loads. The fix used here: pin `java.exe` / `javaw.exe`
to the high-performance GPU (Windows Settings → System → Display → Graphics,
or `HKCU\Software\Microsoft\DirectX\UserGpuPreferences`), and
`earlyWindowControl = false` in `run/config/fml.toml` to skip the FML splash
window. Details in `CLAUDE.md`.

## Suggested next steps, in order

1. **Drive the in-world flow once** — create a world, press F6, confirm
   horses spawn in the pens with visibly varied coats and nothing crashes
   after a minute. This is the gap between "compiles + boots" and "minimum
   stable".
2. Fill in real `LEG_REGIONS` coordinates; confirm a bay horse in-game
   shows varying leg black height.
3. Breeding / real inheritance: combine two parents' alleles on breed
   (`common/` change + one NeoForge event hook).
4. Add a `.gitignore` (`build/`, `run/`, `.gradle/`, `.idea/`).
5. Later polish: per-leg variation, more loci (cream, dun, gray),
   permanent-noon in the debug dimension.

<!-- FILE_LIST:START -->
## Repository file listing

_Auto-generated by generate_file_list.py - do not edit by hand, just re-run the script._

```
Procedural-Horse-Coats/
├── common/
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   │       └── com/
│   │   │           └── example/
│   │   │               └── horsegenetics/
│   │   │                   └── common/
│   │   │                       ├── coat/
│   │   │                       │   ├── CoatData.java
│   │   │                       │   └── CoatGenerator.java
│   │   │                       ├── genetics/
│   │   │                       │   ├── CoatPhenotype.java
│   │   │                       │   └── Genotype.java
│   │   │                       └── Rng.java
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── horsegenetics/
│   │                       └── common/
│   │                           ├── coat/
│   │                           │   ├── CoatDataTest.java
│   │                           │   └── CoatGeneratorTest.java
│   │                           ├── genetics/
│   │                           │   └── GenotypeTest.java
│   │                           └── testutil/
│   │                               └── FakeRng.java
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
│   │       ├── resources/
│   │       │   ├── assets/
│   │       │   │   └── horsegenetics/
│   │       │   │       └── lang/
│   │       │   │           └── en_us.json
│   │       │   ├── data/
│   │       │   │   └── horsegenetics/
│   │       │   │       ├── dimension/
│   │       │   │       │   └── debug_pens.json
│   │       │   │       └── dimension_type/
│   │       │   │           └── debug_pens.json
│   │       │   └── META-INF/
│   │       │       └── neoforge.mods.toml
│   │       └── neoforge.mods.toml
│   └── build.gradle
├── CLAUDE.md
├── generate_file_list.py
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
└── settings.gradle.kts
```
<!-- FILE_LIST:END -->
