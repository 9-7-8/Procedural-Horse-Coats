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

- [ ] **Grey renders wrong** - needs a rework, not just a knob turn. `GreyGene`
  is a single flat adult step (`KEEP = 0.15` on both pigments, so the body
  samples the gradient's near-white corner at roughly `(227,221,215)`). Real
  grey is progressive with age and dapples; this reads flat. Revisit alongside
  an age input to the pipeline.
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

- [ ] **The genotype gallery** (new, 2026-09-01) - the horse dimension now
  builds one pen per entry in `GenotypeCatalog`: **434** visually distinct
  genotypes out of 19 683 total, two per segment, right-hand pen = even index,
  **1 519 blocks** of corridor. Check in-game:
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
- [ ] **Epigenetic seed persists** across save → reload (bay leg heights and
  splash markings identical after a reload).
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
