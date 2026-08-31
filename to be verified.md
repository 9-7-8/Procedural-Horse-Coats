# To be verified

Things built but **not yet confirmed in a `runClient` session**. Review after
each run and tick / delete what holds. Newest work at the top.

Machine note: this dev laptop needs `java.exe`/`javaw.exe` pinned to the NVIDIA
GPU + the FML splash disabled or the JVM hard-crashes in the AMD GL driver -
see CLAUDE.md "Running the game".

---

## 2026-08-31 - allele/gene rework + splash + debug button

Use the new **"Spawn Test Horse World"** button on the title screen (dev build
only): Creative + cheats, hotbar = hay block / golden carrot / stick / clock /
paper / lead. Breed a lot to see genotypes.

- [ ] **"Spawn Test Horse World" button** appears on the title screen (dev
  only), creates the world, drops you in Creative with the 6-item hotbar.
- [ ] **Any genotype renders** - spawn / breed a wide spread, nothing crashes,
  nothing renders garbled or invisible.
- [ ] **Black** is no longer a flat void - the pure-black-→-80%-opacity pass
  lets ~20% of the template texture show through.
- [ ] **Chestnut / champagne / white** read right; **champagne reads off its
  base** (champagne-on-black vs -bay vs -chestnut are visibly different golds;
  champagne-on-white is invisible).
- [ ] **Bay** - red-brown body, black mane/tail/ears/hooves, black up the legs
  + face a random amount (differs per horse / per leg).
- [ ] **Seal** (`Sl`) - dark body, **legs + face fade to near-black at the
  hoof/muzzle and ease back up a random amount** (a gradient, not a hard edge).
  `SealGene.BODY_BLACK` (0.82) - is the body dark-brown or still too black?
- [ ] **Splash** (`Spl`) - white climbs each leg a random amount + a random
  white face blaze; edges are flat (expected for now).
- [ ] **Test** (`T`) - the pink/blue-red/yellow gradient **multiplied** over
  the coat, tinting it rather than replacing it (invisible on pure black).
- [ ] **Eyes** survive on every coat as just the pupil dot + sclera dot (tiny).
- [ ] **Epigenetic seed persists** - note a bay/seal/splash horse's markings,
  save → quit → reload, markings unchanged.
- [ ] Agouti shows only `A`/`a` in the code (no stray `S`); seal is its own
  `Sl/sl` segment.
- [ ] Family-tree node still draws a coat for each horse.
- [ ] Foals still render the vanilla `*_baby` texture by phenotype.

Tuning knobs if something looks off: `BayCoat.BODY_BLACK` / `HOOF_FRACTION`,
`SealGene.BODY_BLACK` / `DEEPEST_BLACK`, the gene wild-frequency constants, the
splash leg/blaze ranges, and `redblackgradient.png` itself.

## Earlier - still open

- [ ] **`breedNth` foal names past foal 1** - breed one pair 7+ times: foal 1 =
  parent combo, foal 2 = the other combo, foals 3-6 = one parent name half + a
  random word, foal 7+ = fully random, no repeats. (`HorseNames` unit-tested;
  the live `HorseAncestryData.offspringCount(dam, sire)` link is unproven.)
- [ ] **`FamilyTreeScreen` scroll mode** - flip `familyTree.scrollBar` true in
  `config/horsegenetics-client.toml`, check the wheel + right-edge thumb path.
- [ ] **Stats surfaces** - foal speed/health landing on the entity's
  attributes; the inventory-panel green/amber/red tint vs `parentStats`; the
  paper dump's `vs parents` line.
- [ ] **Clock on a tamed foal** ages it up without also seating you.
- [ ] **Water riding** feel (`HorseWaterRidingHandler`).
- [ ] **Roped-horse right-click** portal shortcut.
- [ ] The instanced-plot horse dimension basics (void outside walls, random-Y
  plots, no pop-in, teardown on leave) - last confirmed before the reworks.

## Confirmed (keep for context, delete when stale)

- 2026-08-31 - the **Test coat** gradient renders smooth and seamless across
  every body part on a live horse; `HorseSkinGeometry` projection engine is
  proven. (Pre-rework; the Test gene is now a multiply layer.)
- 2026-08-30 - hay-bale portal, horse dimension walls/floor, mounted info
  panel, `FamilyTreeScreen` shrink-to-fit + 3D nodes, breeding roll. (See
  CLAUDE.md "Owner-verified in-game (2026-08-30)".)
