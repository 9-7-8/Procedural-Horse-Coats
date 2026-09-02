# To be verified

The rolling **`runClient` checklist**. Two halves:

- **Open issues** - seen in-game, reproducible, *not yet fixed*. Fix them and
  delete the entry.
- **Still to verify** - built but not yet confirmed in a play session.

Confirmed items are **deleted**, not ticked - CLAUDE.md's "Owner-verified
in-game" block is the permanent record.

Machine note: this dev laptop needs `java.exe`/`javaw.exe` pinned to the NVIDIA
GPU + the FML splash disabled or the JVM hard-crashes in the AMD GL driver -
see CLAUDE.md "Running the game".

---

## Open issues (seen in-game 2026-09-01, not fixed)

- [ ] **Splash is missing the other face markings** - only the centreline blaze
  (`CoatRegions.whitenBlaze`) is implemented. Add the rest of the family: star,
  snip, stripe, bald face.
- [ ] **Splash leg edges are too clean** - `whitenLowerLeg` cuts at a hard
  `point.y() <= cutoff` line, so a sock's top edge is a perfect ring. Break it
  up (per-texel epigenetic jitter, or the smoothstep treatment `BayCoat.fade`
  already uses).

## Wanted (not built yet)

- [ ] **Splash isn't really incomplete dominant** - it's tagged
  `INCOMPLETE_DOMINANT` (so the catalogue gives `Spl/spl` and `Spl/Spl` their
  own pens) but `SplashGene.restrict` never reads the dose, so the two render
  identically. Homozygous splash should give **much larger** markings - higher
  stockings, a wide blaze or bald face, body patches. Compare gallery pens
  **#11 `eeaa nSpl`** and **#19 `eeaa SplSpl`** - identical today, and the
  clearest place to check the fix.
- [ ] **Genetic eye colour** - the eyes themselves render correctly everywhere,
  but they're copied verbatim from the template (`CoatRegions.redrawEyes`).
  Wants a gene, and the classic hook is blue eyes on cream double-dilutes.

## Still to verify

- [ ] **The genotype gallery** (new, 2026-09-01) - **slated for revert**, so
  this is now low-value: the owner has decided the dimension goes back to
  **random** pens (keeping the pairs and the per-pen genome sign) - see
  `Docs/to be completed.md` §9. Verify only the parts that survive the revert
  (the per-pen sign, the pairs, the pen amenities); don't spend a session
  walking 1 519 blocks checking catalogue order. What it does *today*: one pen
  per entry in `GenotypeCatalog`, **434** visually distinct genotypes out of
  19 683 total, two per segment, right-hand pen = even index, **1 519 blocks**
  of corridor. Check in-game:
  - the entrance sign three blocks in front of the return portal reads
    `Genotypes / 19,683 / Distinct / 434 pens`;
  - the first six pens, right/left alternating, are `eeaa`, `EEaa`, `eeAA`,
    `EEAA`, `EEaa WW`, `EEaa TT` - i.e. extension exhausts before agouti moves,
    no heterozygote pens for dominant genes, and white and test get exactly one
    pen each;
  - pen signs read like the horse's own info panel (`eeaa nSpl`, not the full
    slash-and-dash code), wrap sensibly on the busiest ones
    (`EEAA SplSpl / ChCh CrCr / prlprl GG`) and don't run past the sign edge;
  - the gate sign really is to the **right** of the gate on both sides of the
    road (the two sides mirror), stands on the road surface, is readable from
    both faces, and is **waxed** (right-click doesn't open the edit screen);
  - both horses in a pen match the sign, and differ only where a
    non-deterministic gene (bay points, splash) makes them differ;
  - the corridor ends in a wall after pen 434 - now a walkable distance.
- [ ] **Leaving no longer clears blocks** - `tearDown` discards entities and
  forgets their ancestry records only. Check: leave and re-enter; the corridor
  is rebuilt over the old one with no leftovers, stragglers, or doubled horses,
  and leaving is instant. Then leave, re-enter, and walk *less* far than last
  time - the stale far end should still be correct geometry with no horses
  until you walk to it.
- [ ] **Foals** - the owner has only spot-checked these; needs a real pass.
  Every treatment should apply on `HdBabyHorseModel` (bay foal with black
  points + leg black, splash foal with white socks, champagne foal gold, a grey
  foal born its base colour and greying when grown). The **baby head/neck
  geometry is a rest-pose AABB approximation** - watch the face/neck for odd
  dark patches or misplaced marks.
- [ ] **Per-allele epigenetics + the grey rework** (new, 2026-09-01, not seen
  in-game). Epigenetics moved off the horse and onto the **allele copy**:
  `Epigenome` carries a `(priority, seed)` per copy, a foal inherits the copy's
  seed unchanged, and priority breaks the homozygote tie. Grey is a whole new
  generator. Check in-game:
  - **grey adults are grey, and dappled** - a neutral grey with visible rounded
    dapples, not the old flat near-white. Sample bakes put the dapple field itself
    at a **21-29% lightness modulation** on the mid greys (5% on a nearly-white
    old one, where there's no pigment left to vary);
  - **greys differ from each other** - stand two adjacent grey pens side by side
    (or two wild greys): one should read dark steel, another mid dapple,
    another nearly white, with different dapple sizes;
  - **the dapple field crosses part seams** with no join at the shoulder /
    haunch / neck (that's what body-space sampling is for);
  - **young greys keep darker points** (mane, tail, lower legs) - the
    point-retention knob is strongest on the least-greyed horses;
  - **a grey foal is still born its base colour** and greys once grown up
    (clock it up in the horse dimension);
  - **bay leg black really varies now** - wild bays should run from low socks to
    seal, and the **four legs of one horse should not stop level** (a small
    per-leg jitter). Bay's extent is now uniform, not the old `f*f` product that
    clustered everything at the bottom;
  - **a foal looks like its parent, not like a re-roll** - breed a seal-ish bay
    mare to a non-bay stallion; a foal that got her `A` should carry *her* point
    heights. Breed the same pair repeatedly: foals that inherited the same copy
    should be identical in that respect (no variation is deliberate for now);
  - **`Spl/Spl` vs `Spl/spl` epigenetics** - on a homozygote the *higher
    priority* copy's seed is used. No way to see the number in-game yet; the
    check is just that a homozygote renders stably (same markings every session).
- [ ] **Epigenome persists** across save → reload (bay leg heights, grey dapples
  and splash markings identical after a reload). The attachment format changed
  (`epigenetic_seed: long` → `epigenome: String`), so **an existing dev save's
  horses can't read their old coat attachment** - they'll re-found epigenetics
  (or the attachment errors outright). Start a fresh world, per the
  no-back-compat rule.
- [ ] **`breedNth` foal names past foal 1** - breed one pair 7+ times: foal 1 =
  parent combo, foal 2 = the other combo, foals 3-6 = one parent name half + a
  random word, foal 7+ = fully random, no repeats. (`HorseNames` is
  unit-tested; the live `HorseAncestryData.offspringCount(dam, sire)` link is
  unproven.)
- [ ] **`FamilyTreeScreen` scroll mode** - the default shrink-to-fit chart is
  confirmed good; the *other* path isn't. Flip `familyTree.scrollBar` true in
  `config/horsegenetics-client.toml` and check the wheel + right-edge thumb.
- [ ] **Stats surfaces** - foal speed/health landing on the entity's
  attributes; the inventory-panel green/amber/red tint vs `parentStats`; the
  paper dump's `vs parents` line.
- [ ] **Compact gene code** (`GeneCodeDisplay.shortForm`) on the surfaces that
  weren't specifically eyeballed: the paper right-click dump's `genetic code:`
  line and the mounted info panel's word-wrapped block.
- [ ] **Water riding** feel (`HorseWaterRidingHandler`) - speed cap and the
  submerged lift are a first guess.
- [ ] **Debug coat-gen chat line is quiet in a release build** - it fires
  correctly in dev; the `!FMLEnvironment.isProduction()` gate is unproven.
- [ ] **Dev test worlds delete themselves** (new, 2026-09-01,
  `client/DebugTestWorldCleanup`). Click **Spawn Test Horse World** on the title
  screen, play a bit, quit Minecraft normally (Save and Quit -> Quit Game).
  Check:
  - `neoforge-26.1.2/run/saves/` has **no** `test_horse_*` folder left, and the
    log has a `Deleted test horse world test_horse_... (on shutdown)` line;
  - the hand-made worlds next to it (`New World`, ...) are **untouched** - the
    sweep matches `test_horse_` + digits only;
  - spawn two test worlds in one session (button, quit to title, button again),
    quit - **both** are gone;
  - kill the client mid-world (`taskkill`, or Alt+F4 in a hang) so shutdown
    never runs, then relaunch: the leftover is cleared at startup with a
    `(left over from a previous run)` log line, and it's gone before the world
    list is shown.
