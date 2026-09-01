# To be verified

Things built but **not yet confirmed in a `runClient` session**. Review after
each run and tick / delete what holds. Newest work at the top.

Machine note: this dev laptop needs `java.exe`/`javaw.exe` pinned to the NVIDIA
GPU + the FML splash disabled or the JVM hard-crashes in the AMD GL driver -
see CLAUDE.md "Running the game".

---

## 2026-09-01 - the *real* flat-white bug: coat texture id collisions

`./gradlew :common:test` = **106 pass** (4 new in `CoatTextureIdTest`);
`:neoforge-26.1.2:build` clean. Not yet run in the client.

- [ ] **Chestnuts / bays rendering as the default white horse** - root cause was
  **not** the compose pipeline (samples bake correctly; only `white.png` is
  identical to the template, as it should be). It was
  `GeneticCoatTextureFactory`'s old `sanitize()`, which lower-cased the texture
  key to build the `Identifier` path - throwing away the **dominance** encoding,
  so `E/e`≡`e/e`, `W/w`≡`w/w`, `A/a`≡`a/a`, `Spl/spl`≡`spl/spl`, `T/t`≡`t/t`.
  All **19 683** genotypes folded onto **27** texture ids (only champagne /
  cream / pearl survived, their tokens differing in letters, not case).
  `TextureManager#register` is a silent `Map.put` that closes the loser, so
  every *deterministic* coat in a bucket - chestnut (`ee`), black (`aa E_`),
  dominant white - rendered whichever one baked **last**; a plain white horse
  whenever that was the `W_` one. Now `CoatTextureId.encode` (injective) +
  a `KEY_BY_ID` tripwire that throws on any future collision.
  **To verify:** roam a horse-dimension corridor / breed a batch with at least
  one `W_` white in view, and confirm chestnuts stay chestnut, blacks stay
  black, and no horse silently adopts a neighbour's coat. (Bays were mostly
  spared - their key carries `@<seed>` - so a "bay" that went white was almost
  certainly an `ee A_` chestnut.)
- [ ] **The `[coat] >> FLAT WHITE` line does not catch this class of bug** - the
  victim's own bake is correct, it just loses its `Identifier`. If a flat-white
  horse turns up again *and* its `[coat]` line says `FLAT WHITE`, that's a
  genuinely empty overlay and a different bug.

## 2026-08-31 (pm2) - white-horse bug, bay fade, grey, gene panel, pen amenities

`./gradlew :common:test` = 99 pass; `:neoforge-26.1.2:build` clean. Not yet run
in the client.

- [ ] **Flat-white bug** - `GeneticHorseRenderer` no longer adds vanilla's
  `HorseMarkingLayer`. Wild horses / grown foals that rolled `Markings.WHITE`
  were rendering as a flat white horse over a correct coat. Breed a bunch,
  grow the foals up, and roam wild spawns: **no unexplained flat-white horses**.
  If any remain, the new debug chat line (below) will say `>> FLAT WHITE` when
  the compose overlay itself came out empty - a different, deeper bug.
- [ ] **Debug coat-gen chat line** (dev build only) - every coat bake drops
  `[coat] adult <code> @<seed>  det|per-horse` in chat (and `>> FLAT WHITE` if
  the overlay was fully transparent). Confirm it fires on horse spawn / foal
  grow-up and is quiet in a release build.
- [ ] **Bay leg / face fade** - `SOLID_PORTION` 0.6 → 0.3 + **smoothstep**
  fade. The black up the legs / face should now dissolve into the body with
  **no hard cut-off line**. Tune `BayCoat.SOLID_PORTION` / the roll ranges if
  it's too gradual or too short.
- [ ] **Cream on bay black** - single cream now also does `black *= 0.7`, so a
  buckskin's points/legs are dark-smoky, not jet. Check buckskin + palomino
  still read right; `CreamPearlDilution.SINGLE_CREAM_BLACK` is the knob.
- [ ] **Grey** - `1/24 → 1/16` per allele, `KEEP 0.40 → 0.15`. Grey adults
  should now be obvious pale dapple-greys and turn up a bit more often. Grow a
  grey foal up and confirm the change is unmistakable.
- [ ] **Test gene** - now painted **flat on top** (was multiply). Should show
  the full pink→blue / red→yellow field on *any* base, including a black or
  white horse.
- [ ] **Foal eye** - `EYE_RECTS_BABY` moved to `{6,20}` / `{40,20}` (the head
  L/R faces). The foal's pupils should survive the coat, not get painted over.
- [ ] **Compact gene code everywhere it's shown to a human**
  (`GeneCodeDisplay.shortForm`): the mounted info panel (bare, word-wrapped),
  the **paper right-click dump** (`genetic code:` line - the raw form is gone
  now), the dev **`[coat]` chat line**, the `bakeCoatSamples` console output,
  and `Genotype`/`CoatData` `toString()`. Only the persistence / network /
  cache-key paths still use the full `toCode()` string. Format:
  `EeAa nSpl nCh CrCr` - ext+agouti first and joined, then only genes with a
  variant allele in the order splash, white, champagne, cream, pearl, grey,
  test; splash/champagne/cream/pearl as `nSpl`/`nCh`/`nCr`/`nprl` when het,
  doubled when homozygous. A code that won't parse (older/shorter gene set)
  degrades to `ee aa ww …` rather than slash-and-dash raw.
- [ ] **Pen amenities** - each horse-dimension pen has a full water cauldron
  in one gate-side corner and a hay bale in the other, **sunk to `floorY-1`**
  (tops flush with the grass) so the horses can't use them as a step over the
  wall. Confirm they generate, the water is reachable, and **the horses stay
  in the pen**.

## 2026-08-31 (pm) - seal-merge, grey, cream, pearl, foal pipeline

Offline `./gradlew :common:bakeCoatSamples` (writes `build/coat-samples/*.png`
and `*_foal.png`) looked right for all of the below. Not yet run in the client.

- [ ] **Foals** now render a generated 128px coat on `HdBabyHorseModel` (not
  the vanilla `*_baby` texture). Check every treatment applies: bay foal has
  black points + leg black, splash foal has white socks, champagne foal is
  gold, etc. The **baby head/neck geometry is a rest-pose AABB approximation** -
  watch the face/neck for odd dark patches or misplaced marks.
- [ ] **Seal is gone as a gene** - it's a high roll of bay's two epigenetic
  numbers (one leg height for all four, one face height). A `bay_high` sample
  ≈ seal. Agouti code segment is `A/a` only, no `S`.
- [ ] **Grey** (`G`): a **foal** is born its base colour; the **adult** greys
  (both pigments `*= 0.40`). Grow a grey foal up and confirm it changes.
- [ ] **Cream** (`Cr`, incomplete dominant) on bay: `Cr/N` = buckskin (golden
  body, **black** points); `Cr/Cr` = perlino (pale cream, smoky points).
  On chestnut: `Cr/N` = palomino.
- [ ] **Pearl** (`prl`, recessive): `prl/prl` with no cream = mild uniform
  dilution (apricot). `Cr/prl` = looks like `Cr/Cr` (perlino).
- [ ] **Black** lifted off pure void (80% opacity); **champagne reads off its
  base**; **white** = bare template; **test** = multiply tint (invisible on
  black).
- [ ] **Splash** (`Spl`): white climbs each leg a random amount + random face
  blaze (flat edges expected).
- [ ] **Eyes**: adult = tiny pupil + sclera dots; foal = pupil dot only.
- [ ] **Epigenetic seed persists** across save → reload (bay leg heights,
  splash markings unchanged).
- [ ] **"Spawn Test Horse World"** title-screen button (dev only) → Creative +
  cheats, hotbar = hay block / golden carrot / stick / clock / paper / lead.
- [ ] Family-tree node still draws a coat.

Tuning knobs: `BayCoat.BODY_BLACK` / `HOOF_FRACTION` / `SOLID_PORTION`,
`GreyGene` KEEP (0.40), `CreamPearlDilution` keep-factors, gene wild
frequencies, splash ranges, and `redblackgradient.png` itself.

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
- [ ] Test coat painted **flat on top** as the last step (was a full-replace
  texture 2026-08-31 am, briefly a multiply layer 2026-08-31 pm; now a plain
  opaque paint-over so it shows on any base).

## Confirmed (keep for context, delete when stale)

- 2026-08-31 - the **Test coat** gradient renders smooth and seamless across
  every body part on a live horse; `HorseSkinGeometry` projection engine is
  proven. (Pre-rework; the Test gene is now a multiply layer.)
- 2026-08-30 - hay-bale portal, horse dimension walls/floor, mounted info
  panel, `FamilyTreeScreen` shrink-to-fit + 3D nodes, breeding roll. (See
  CLAUDE.md "Owner-verified in-game (2026-08-30)".)
