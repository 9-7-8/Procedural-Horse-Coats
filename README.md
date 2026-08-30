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
