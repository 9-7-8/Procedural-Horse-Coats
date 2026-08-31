# Gene Dict

Every gene in the horse-genetics model: its alleles, inheritance, wild
frequency, and how it changes the coat. Code (`common/genetics/genes/`,
registered in `common/genetics/Genes.java`).

See **CLAUDE.md** for the surrounding architecture (the genotype code format,
the coat overlay pipeline, `HorseSkinGeometry`). See **breeding.md** for
inheritance / segregation.

---

## How a gene contributes to the coat

Every coat pixel starts at **max red + max black pigment** (a black horse).
Then:

1. **natural pass** - every visible *natural* gene (`Gene.isNatural()`, the
   default) gets a `restrict(pair, ctx)` turn to push the shared
   `PigmentField` (per-texel `red` / `black` in `[0,1]`) down. Natural genes do
   **nothing else** - no colour, just restriction. Order:
   `Genes.naturalOrder()` = extension → agouti → seal → champagne → white →
   splash.
2. **resolve** - each mapped texel's `(red, black)` is looked up in
   `redblackgradient.png` (`GradientLut`). Fully-restricted → transparent. A
   texel that resolves to **pure black** is knocked to **80% opacity** so black
   coats aren't a flat void.
3. **multiply pass** - every visible *non-natural* gene (`Genes.multiplyOrder()`
   = test only) fills an ARGB layer that is **multiplied** onto the resolved
   coat.
4. **composite** onto the white template (alpha-aware multiply, keep template
   alpha), then **eyes** copied straight from the template.

Because natural genes only move the pigment sample, **champagne-on-bay looks
different from champagne-on-black, and anything on white is invisible**.

Non-deterministic genes take all randomness from `ctx.epigeneticsFor(key)` -
seeded once at birth, replayed every regen.

---

## `horsegenetics.extension` - Extension

| | |
|---|---|
| **alleles** | `E` (dominant, wild-type), `e` (recessive) |
| **inheritance** | simple dominant/recessive |
| **wild frequency** | 50 / 50 per allele (2 `nextBoolean`) |
| **natural?** | yes |
| **deterministic?** | yes |
| **visible when** | `ee` |

"Can this horse make black pigment at all." `E_` = yes, no effect. `ee` =
`restrict` sets **black = 0 everywhere** → chestnut (only red survives → the
gradient's left edge).

## `horsegenetics.agouti` - Agouti

| | |
|---|---|
| **alleles** | `A` (dominant), `a` (recessive, wild-type) - **two only** |
| **inheritance** | simple dominant/recessive |
| **wild frequency** | 50 / 50 per allele (2 `nextBoolean`) |
| **natural?** | yes |
| **deterministic?** | **no** when `A_` on a black-capable horse |
| **visible when** | `A_` **and** `genotype.hasBlackPigment()` |

`A_` → **bay** via `BayCoat.apply`: body black knocked to `BayCoat.BODY_BLACK`
(0.32, red kept → red-brown body); mane / tail / ears full black; black climbs
each leg + the face a **random** amount (`0.15 + f*f*0.45` of leg height per
leg, `0.05 + f*0.30` up the face); hooves always black
(`BayCoat.HOOF_FRACTION` = 0.12). Seal brown is the separate `seal` gene, not
an agouti allele.

## `horsegenetics.white` - Dominant white

| | |
|---|---|
| **alleles** | `W` (dominant), `w` (recessive, wild-type) |
| **inheritance** | simple dominant |
| **wild frequency** | `1 in WhiteGene.WILD_WHITE_ALLELE_ODDS` = **50** per allele (~4% white) |
| **natural?** | yes |
| **deterministic?** | yes |
| **visible when** | `W_` |

`restrict` sets **red = black = 0 everywhere** → transparent overlay → the
white template shows through, masking every other gene.

## `horsegenetics.test` - Test (diagnostic)

| | |
|---|---|
| **alleles** | `T` (dominant), `t` (recessive, wild-type) |
| **inheritance** | simple dominant |
| **wild frequency** | `1 in TestGene.WILD_TEST_ODDS` = **4** (one roll → `Tt`; deliberately common) |
| **natural?** | **no** |
| **deterministic?** | yes |
| **visible when** | `T_` |

The one non-natural gene. `multiplyLayer` fills the layer with the
`TestCoatPattern` gradient (pink→blue along body X, red→yellow along body Y);
the composer **multiplies** it onto the resolved coat, so it *tints* whatever
is underneath (invisible on pure black - multiply by anything is still black).
Exercises `HorseSkinGeometry` end to end. Expect it removed once the engine is
trusted.

## `horsegenetics.champagne` - Champagne

| | |
|---|---|
| **alleles** | `Ch` (dominant), `c` (recessive, wild-type) |
| **inheritance** | simple dominant, **not dose-dependent** |
| **wild frequency** | `1 in ChampagneGene.WILD_CHAMPAGNE_ALLELE_ODDS` = **40** per allele |
| **natural?** | yes |
| **deterministic?** | yes |
| **visible when** | `Ch_` |

`restrict` moves the pigment sample: `red → 0.45 + 0.10*red` (roughly the
gradient's horizontal middle - its champagne-gold column), `black *= 0.18`
(lift eumelanin so a black coat reaches the gold). Reads off the *current*
pigment, so champagne-on-black / -bay / -chestnut all differ; champagne-on-white
is invisible.

## `horsegenetics.seal` - Seal brown

| | |
|---|---|
| **alleles** | `Sl` (dominant), `sl` (recessive, wild-type) |
| **inheritance** | simple dominant |
| **wild frequency** | `1 in SealGene.WILD_SEAL_ALLELE_ODDS` = **16** per allele |
| **natural?** | yes |
| **deterministic?** | **no** when `Sl_` on a black-capable horse |
| **visible when** | `Sl_` **and** `genotype.hasBlackPigment()` |

Real-horse `A^t`, split into its own gene. Body barely lightened (black kept at
`SealGene.BODY_BLACK` = 0.82 - **TUNE**). The lower legs and lower face fade to
`DEEPEST_BLACK` (0.985, just under pure so the gradient stays monotonic past
the black-lift) - **densest at the hoof / muzzle, easing back to the body level
by a random point** (`0.25 + f*0.55` of leg height / `0.20 + f*0.55` of head
length). A smooth ramp, not a hard edge.

## `horsegenetics.splash` - Splash white

| | |
|---|---|
| **alleles** | `Spl` (dominant), `spl` (recessive, wild-type) |
| **inheritance** | simple dominant (real splash is more complex) |
| **wild frequency** | `1 in SplashGene.WILD_SPLASH_ALLELE_ODDS` = **20** per allele |
| **natural?** | yes |
| **deterministic?** | **no** |
| **visible when** | `Spl_` |

White markings, "dipped in white from below". `restrict` removes **both**
pigments (→ transparent → white template) up **each leg independently** a
random amount (`0.15 + f*f*0.75` of leg height - usually socks, sometimes
stockings) plus a random **face blaze** down the centreline of the muzzle /
head (`whitenBlaze`, random half-width `0.4 + f*1.4` body units and length
`0.2 + f*0.75` of the head). Flat white for now - irregular edges are a
follow-up.

---

## Reusable helpers (`common/coat/pattern/CoatRegions`)

`fillMane` / `fillTail` / `fillEars` / `fillHooves`, `paintLowerLeg`,
`blackenPart` / `blackenLowerLeg` / `blackenFace`, `whitenLowerLeg` /
`whitenBlaze`, `redrawEyes` (`EYE_RECTS` = the 2x2 pupil + 2x2 sclera per eye,
copied verbatim from the template).

## Adding a gene

1. New `Gene` impl under `common/genetics/genes/` - alleles (any token
   strings), `randomPair`, and either `restrict` (natural) or
   `isNatural()=false` + `multiplyLayer`.
2. Register in `Genes`: append to `CODE_ORDER`, and to `NATURAL_ORDER` (in
   effect order) or `MULTIPLY_ORDER`.
3. It shows up in the code string automatically (segment appended). No legacy
   handling - dev only.
4. Document it here.
