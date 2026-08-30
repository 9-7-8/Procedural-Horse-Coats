# Horse Genetics - NeoForge 26.1.2 Mod

Procedural coat generation for horses, starting with basic Mendelian
genetics (extension + agouti loci) driving coat phenotype, with an eye
toward eventually backporting to 1.12.2.

## Architecture - read this before editing anything

This is a two-module Gradle project, split deliberately:

- **`common/`** - pure Java, zero Minecraft/NeoForge imports of any kind.
  Contains `Genotype`, `CoatPhenotype`, `CoatData`, `CoatGenerator`, `Rng`.
  This module is the part that survives a version port unchanged - if you
  find yourself wanting to import anything Minecraft-related here, stop:
  that logic belongs in the version module instead.
- **`neoforge-26.1.2/`** - everything Minecraft-specific: Data Attachments
  for persistence, custom packets for client sync, NativeImage-based
  runtime texture compositing for rendering, event handlers for spawn-time
  genotype assignment.

When adding a feature, default to putting as much logic as possible in
`common/` and keeping the NeoForge module as thin plumbing around it. This
is what makes a future `forge-1.12.2/` module cheap to add later.

## Build & test

```bash
./gradlew :common:test              # pure-Java genetics logic, no Minecraft needed - fast
./gradlew :neoforge-26.1.2:build    # full compile, slow first run (downloads NeoForge/Minecraft deps)
./gradlew :neoforge-26.1.2:runClient   # launch the game with the mod loaded
```

Run `:common:test` first when iterating on genetics logic - it doesn't
touch Minecraft at all and is the fastest feedback loop in this repo.

Requires JDK 25 (NeoForge 26.1.2's requirement). The `foojay-resolver-convention`
plugin in `settings.gradle.kts` should auto-provision it if it's missing.

## The genetics model, as implemented

Two loci, four alleles, one phenotype table. Genotype is a 4-character
string: E-locus alleles then A-locus alleles, dominant allele written first
per locus (`Genotype.parse`/`Genotype.of` canonicalize this automatically).

| E locus  | A locus | Phenotype                       |
|----------|---------|----------------------------------|
| ee (any) | any     | Chestnut                        |
| E_       | aa      | Black                            |
| E_       | A_      | Bay (+ random `legBlackHeight`)  |

`legBlackHeight` is a float in [0, 1], rolled once per bay horse at
genotype-assignment time via `CoatGenerator.generate`, then persisted -
**never re-roll it** on world load or the horse's coat will visibly change
between sessions. It's applied uniformly to all four legs currently;
per-leg variation is a reasonable future enhancement, not a bug to fix.

Every horse currently gets an independently random genotype on spawn
(`HorseGeneticsEventHandler`, listens for `EntityJoinLevelEvent` on any
`Horse`). Real inheritance (combining two parents' alleles on breeding) is
not implemented yet - see "Known gaps" below.

## Data flow (server -> client -> pixels)

1. Horse spawns -> `HorseGeneticsEventHandler` rolls a `Genotype`, calls
   `CoatGenerator.generate`, stores the result via `ModAttachments.HORSE_COAT`
   (a NeoForge Data Attachment with a Codec).
2. Attachments are **not** auto-synced to clients, so the handler also sends
   a `CoatSyncPayload` to tracking players.
3. Client stores incoming payloads in `ClientCoatCache` (keyed by entity id) -
   this is a workaround, not a persistence layer; it's rebuilt from network
   traffic every session.
4. `GeneticHorseRenderer` reads `ClientCoatCache` in `extractRenderState`,
   and for bay horses calls `GeneticCoatTextureFactory` to composite a
   runtime texture from the vanilla bay + black textures.

## Known gaps / things flagged as unverified - check these before trusting the code blindly

These were written against NeoForge 26.1.2 docs but not compiled/tested,
since they were produced outside an actual dev environment. Treat them as
"probably right, verify on first build":

- **`GeneticHorseRenderer`**: assumes `HorseRenderer`/`AbstractHorseRenderer`
  is generic enough to swap in `GeneticHorseRenderState` via a covariant
  `createRenderState()` override. If vanilla hard-codes `HorseRenderState`,
  this needs a full reimplementation instead of a subclass - copy vanilla's
  renderer source and modify the texture-selection line directly.
- **`GeneticCoatTextureFactory.LEG_REGIONS`**: placeholder pixel rectangles.
  Real coordinates need to come from opening `horse_brown.png` /
  `horse_black.png` (64x64, ship inside the client jar) in an image editor
  and finding where the legs actually sit on the UV map.
- **`DebugKeyHandler`**: uses `ClientTickEvent.Post` - tick event naming has
  shifted across NeoForge versions before; confirm this class name still
  exists in 26.1.2 if it fails to compile.
- **`DebugPenManager.teleportAndGenerate`**: uses `ServerPlayer#teleportTo`
  with a specific argument signature that has had minor shape changes
  across versions - verify against the actual 26.1.2 method signature.
- **Entity rendering generally**: the `submit()`/`SubmitNodeCollector` split
  that landed for block entity renderers may also apply to entity renderers
  by 26.1.2 - double check current docs if `getTextureLocation()` doesn't
  behave as expected.

When you resolve one of these, update this file so future sessions don't
re-flag it as a caveat.

## Debug tool: dev-only horse pen generator

Press **F6** in a dev environment (`runClient`) to teleport into
`horsegenetics:debug_pens` (a dedicated flat dimension) and generate 20x20
fenced pens along +X, each with two horses, forever, as you walk. Useful for
eyeballing a wide spread of genotypes/coats at once without manually
spawning horses one at a time.

This is gated to never exist in a production build:
`DebugKeyBindings` only registers the keybind if `!FMLEnvironment.isProduction()`,
and `ModNetworking`'s server-side handler re-checks the same flag
independently, so a forged network packet against a production server still
no-ops. Do not remove these checks or relax them "just for testing" without
putting them back before anything ships.

Known limitation: the pen-generation progress counter is in-memory only and
resets on server restart (see `DebugPenManager`). This is an accepted
tradeoff for a debug tool, not something that needs fixing unless it
actually becomes annoying.

## License

CC BY-NC 4.0 (see `LICENSE`). In practice: forks/derivatives are welcome
without asking permission, but must credit the original repo and link back,
and any portion of this code may not appear in a derivative that's sold
with no free version available. Donations/tips on an otherwise-free
derivative are fine. Keep this in mind if ever asked to add or vendor
third-party code - check its license is compatible before pulling it in.

## Conventions

- Keep `common/` free of Minecraft imports - this is a hard rule, not a
  preference.
- New Minecraft-version-specific logic goes in `neoforge-26.1.2/`, organized
  by concern: `client/` (rendering, keybinds, client-side caches), `data/`
  (persistence), `network/` (packets), `server/` (spawn/event logic).
- Flag genuinely unverified/unconfirmed API usage in comments the same way
  the existing code does - it's more useful to future sessions (and to the
  project owner) than silent confidence.
