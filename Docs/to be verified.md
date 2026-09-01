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

- [ ] **Bay's black points don't take dilution** - on a bay carrying a dilute
  (cream / pearl / champagne), the black leg / face / mane / tail regions still
  render as **pure black** instead of the diluted smoky tone. A buckskin's
  points should be dark-smoky, a perlino's paler still.
  Suspected cause: `BayCoat.apply` uses `CoatRegions.blackenPart` /
  `blackenLowerLeg` / `rampBlack*`, which **`setBlack(1.0)` + `setRed(0.0)`
  absolutely**, and `Genes.naturalOrder()` runs agouti *before* cream / pearl /
  champagne - so a later dilution scales a value agouti has already pinned to
  the top, or the dilution's own keep-factor is applied to red only. Check
  `CreamPearlDilution` and the ordering together; a relative
  (`restrictBlack`-style) bay point would compose better than an absolute set.
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

- [ ] **Genetic eye colour** - the eyes themselves render correctly everywhere,
  but they're copied verbatim from the template (`CoatRegions.redrawEyes`).
  Wants a gene, and the classic hook is blue eyes on cream double-dilutes.

## Still to verify

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
