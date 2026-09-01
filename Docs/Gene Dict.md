# Gene Dict

Every gene in the horse-genetics model. Code in `common/genetics/genes/`,
registered in `common/genetics/Genes.java`. See **CLAUDE.md** for the
architecture, **Docs/breeding.md** for inheritance.

---

## The pipeline in one paragraph

Every coat pixel starts at **max red + max black pigment** (a black horse).
**Natural** genes (`Gene.isNatural()`, the default) each get a
`restrict(pair, ctx)` turn to push the shared `PigmentField` (per-texel `red` /
`black` in `[0,1]`) down - they do *nothing else*. The field is then resolved
to colour through `redblackgradient.png` (`GradientLut`) - fully restricted →
transparent, resolves-to-pure-black → 80% opacity. Finally the one
**non-natural** gene (Test) paints an ARGB layer **flat on top** of the
resolved coat (opaque layer texels win outright - visible even on a white
coat). The result is composited onto the white template (adult or foal), and
the eye texels are copied back verbatim.

Because natural genes only move the pigment sample, **champagne-on-bay differs
from champagne-on-black, cream-on-chestnut differs from cream-on-bay, and
anything on white is invisible.**

Foals go through the exact same pipeline on the `Skin.BABY` geometry + the
128px `horse_white_baby.png` template. **Grey** is the only gene that reads
age: it greys adults only, so a foal is born its base colour.

Non-deterministic genes take all randomness from `ctx.epigeneticsFor(key)` -
rolled once at birth, replayed every regen.

---

## The projection engine (`common/coat/skin/HorseSkinGeometry`)

Genes don't think in texels. They think in **horse body-space** and let
`HorseSkinGeometry` translate. That's what lets a coat rule be a smooth
function of position and still come out **seamless across every body part** -
no visible join where the neck meets the head, no mismatch where a leg's front
face meets its side face.

**The coordinate frame** (right-handed, model units; 1 unit = 1/16 block =
`TEXELS_PER_UNIT` = 2 texels on the 128px sheet):

| axis | 0 point | + direction | rough meaning of a "plain function of this axis" |
|------|---------|-------------|--------------------------------------------------|
| **X** | rear edge of the tail | toward the nose | a front-to-back gradient (tail → nose) |
| **Y** | underside of the hooves | straight up | ventral → dorsal (belly → topline) |
| **Z** | the centre plane | the horse's **right** | left ↔ right; `z = 0` is the spine / centreline |

One **absolute** scale per mesh (`ADULT`, `BABY`), with the origins read off
the mesh itself (X=0 = the backmost tail texel, Y=0 = the hoof undersides). So
two different parts that occupy the same body-space region sample the coat
function at the *same* value - which is exactly why an X-function gradient has
no seams.

**Parts and faces.** Every `Part` is an axis-aligned box (rotated parts - the
foal's tilted neck/head/ears - use their *rest-pose* AABB, so faces there are
approximate). Each box has six `Face`s, each looking down one axis and spanned
by the other two:

- **NOSE / TAIL** face along X, spanned by (Z, Y) - the front and back caps.
- **TOP / BOTTOM** face along Y, spanned by (X, Z) - the **dorsal** and
  **ventral** surfaces.
- **RIGHT / LEFT** face along Z, spanned by (X, Y) - the two flanks.

So "paint the topline black" = the TOP faces; "black up the belly" = BOTTOM
faces; "a centreline blaze" = texels with small `|z|` on the head's TOP/RIGHT/
LEFT faces. `CoatRegions` wraps the common ones (`restrictAll`, `blackenPart`,
`whitenLowerLeg`, `whitenBlaze`, …); a gene calls those with `ctx.skin()` and
never touches a `px,py` directly.

**Round trips.** `forEachTexel(skin, [part], visitor)` walks every mapped texel
handing back its `(part, face, BodyPoint)`; `sample(px, py)` / `project(point)`
go the other way. `bounds(skin, part)` / `bodyBounds(skin)` give the extents a
gene normalises against (e.g. bay's leg-black height is a fraction of
`bounds(leg).span(Y)`). The foal mesh has no MANE or MUZZLE part - calls for
those are silently skipped.

---

# Natural genes

## `horsegenetics.extension` - Extension

| | |
|---|---|
| alleles | `E` (dominant, wild-type), `e` (recessive) |
| inheritance | simple dominant/recessive |
| wild frequency | 50/50 per allele (2 `nextBoolean`) |
| deterministic? | yes |
| visible when | `ee` |

"Can this horse make black pigment at all." `E_` = yes, no effect. `ee` = black
restricted to 0 everywhere → chestnut (the gradient's left edge).

## `horsegenetics.agouti` - Agouti

| | |
|---|---|
| alleles | `A` (dominant), `a` (recessive, wild-type) |
| inheritance | simple dominant/recessive |
| wild frequency | 50/50 per allele (2 `nextBoolean`) |
| deterministic? | **no** when `A_` on a black-capable horse |
| visible when | `A_` **and** `genotype.hasBlackPigment()` |

`A_` → bay via `BayCoat.apply`: body black knocked to `BayCoat.BODY_BLACK`
(0.32, red kept → red-brown body); mane / tail / ears full black; black climbs
the legs + the face by a **random** amount and **fades out at its top edge**.
Two epigenetic numbers: **one leg height** (all four legs the same) and **one
face height** (`0.12 + f*f*0.85` and `0.04 + f*f*0.60`). The bottom
`SOLID_PORTION` = **0.3** of the band is solid black, then a **smoothstep**
fade to nothing over the rest - smoothstep (flat slope at both ends) so the
black dissolves into the body colour with no visible cut-off line. Hooves
always black (`HOOF_FRACTION` = 0.12).

**Seal brown** is just the top of this distribution - a high leg/face roll
("black creeps most of the way up"). There is no separate seal gene.

Owner-verified in-game **2026-09-01**: agouti renders correctly, the leg/face
fade reads right, and seal-as-a-high-roll works.

> **Open issue - bay's black does not take dilution.** `BayCoat` paints its
> points *absolutely* (`CoatRegions.blackenPart` / `blackenLowerLeg` /
> `rampBlack*` all do `setBlack(1.0)` + `setRed(0.0)`), and agouti runs
> **before** cream / pearl / champagne in `Genes.naturalOrder()`. So a diluted
> bay - buckskin, perlino, pearl bay, amber champagne - still renders **jet
> black** points instead of the smoky tone the dose table calls for. Fix needs
> the ordering and `CreamPearlDilution` looked at together; a *relative* bay
> point (`restrictBlack`-style, or a post-dilution re-application) would
> compose properly.

## `horsegenetics.cream` - Cream  &  `horsegenetics.pearl` - Pearl

Real-horse `SLC45A2`: allelic. Modelled here as **two genes** whose combined
effect is computed once, in `coat.pattern.CreamPearlDilution`.

| gene | alleles | wild frequency | deterministic? |
|---|---|---|---|
| cream | `Cr` (**incomplete** dominant), `N` (wild-type) | `1 in 30` per allele | yes |
| pearl | `prl` (recessive), `N` (wild-type) | `1 in 22` per allele | yes |

Both are dilutions - they restrict / redistribute existing pigment, never add.
The combined rule (dose = number of `Cr` / `prl` copies):

| Cream | Pearl | mode | effect on the pigment field |
|---|---|---|---|
| 0 | 0-1 | none | - |
| 1 | 0 | **single cream** | red `*= 0.45`, black `*= 0.7` (buckskin on bay: golden body, dark-but-not-jet smoky points) |
| 0 | 2 | **double pearl** | red `*= 0.55`, black `*= 0.60` - mild, uniform (apricot body, sepia points) |
| 1 | 1+ | **Cr/prl** | acts as double cream |
| 2+ | any | **double cream** | red `*= 0.08`, black `*= 0.38` (perlino: pale cream body, smoky points) |

Red is always restricted harder than black, so a diluted bay body fades to
cream while the points hold smoky colour (agouti still says *where*). Even
single cream touches the black: bay never *adds* black anywhere - its points
are just black it declined to restrict - so a real pigment dilution has to
reach them, not leave them jet. `CreamGene`
is the driver whenever a `Cr` is present; `PearlGene` drives only the
no-cream double-pearl case; either way `CreamPearlDilution.apply` runs at most
once.

*Not modelled yet:* blue eyes on the double dilutes.

## `horsegenetics.champagne` - Champagne

| | |
|---|---|
| alleles | `Ch` (dominant), `c` (recessive, wild-type) |
| inheritance | simple dominant, **not dose-dependent** |
| wild frequency | `1 in 40` per allele |
| deterministic? | yes |

`Ch_` moves the pigment sample: `red → 0.45 + 0.10*red` (roughly the gradient's
horizontal middle - its gold column), `black *= 0.18`. Reads the *current*
pigment, so champagne-on-X all differ.

## `horsegenetics.grey` - Grey

| | |
|---|---|
| alleles | `G` (dominant), `g` (recessive, wild-type) |
| inheritance | simple dominant |
| wild frequency | `1 in 16` per allele |
| deterministic? | yes |
| visible when | `G_` **and the horse is an adult** |

`G_` on an **adult** equally restricts both pigments (`red *= 0.15`,
`black *= 0.15`) - strong enough that a grey adult reads as an unmistakable
pale dapple-grey, not "a slightly washed-out black", but still short of
dominant white's total. A **foal** is born whatever colour it would be without
grey; `restrict` no-ops for `!ctx.isAdult()`. (Real grey is progressive with
age; this is one flat adult step - no year-by-year age input.)

> **Open issue - grey renders wrong; wants a rework, not a knob turn.** Seen
> in-game 2026-09-01. At `KEEP = 0.15` the body samples the gradient's
> near-white corner (roughly `(227,221,215)`), so a grey reads as flat and
> washed-out rather than as a dapple-grey. The flat single-step model is the
> real problem: this wants progressive greying driven by an **age input to the
> pipeline** plus actual dappling, not a different `KEEP`.

## `horsegenetics.white` - Dominant white

| | |
|---|---|
| alleles | `W` (dominant), `w` (recessive, wild-type) |
| inheritance | simple dominant |
| wild frequency | `1 in 50` per allele (~4% white) |
| deterministic? | yes |

`W_` sets red = black = 0 everywhere → transparent overlay → the white
template shows through, masking every other gene. This is the **only** coat
that should ever look like the bare template - if a chestnut or bay does, see
`CoatTextureId` in CLAUDE.md (fixed 2026-09-01).

## `horsegenetics.splash` - Splash white

| | |
|---|---|
| alleles | `Spl` (dominant), `spl` (recessive, wild-type) |
| inheritance | simple dominant (real splash is more complex) |
| wild frequency | `1 in 20` per allele |
| deterministic? | **no** |

Random white markings, "dipped in white from below". Removes both pigments
(→ transparent → white template) up **each leg independently** a random amount
(`0.15 + f*f*0.75` of leg height - socks .. stockings) plus a random
**face blaze** down the centreline (`whitenBlaze`: random half-width
`0.4 + f*1.4` body units, length `0.2 + f*0.75` of the head).

Owner-verified in-game **2026-09-01**: what's implemented renders correctly.

> **Open issue - two gaps.** (1) The blaze is the **only** face marking; the
> rest of the family (star, snip, stripe, bald face) isn't built. (2)
> `CoatRegions.whitenLowerLeg` cuts at a hard `point.y() <= cutoff`, so every
> sock ends in a **perfect ring**. Break the edge up - per-texel epigenetic
> jitter, or the smoothstep treatment `BayCoat.fade` already uses.

---

# Non-natural genes

## `horsegenetics.test` - Test (diagnostic)

| | |
|---|---|
| alleles | `T` (dominant), `t` (recessive, wild-type) |
| inheritance | simple dominant |
| wild frequency | `1 in 4` carriers (deliberately common) |
| natural? | **no** |
| deterministic? | yes |

`overlayLayer` fills a layer with the `TestCoatPattern` gradient (pink→blue
along body X, red→yellow along body Y); the composer paints it **flat on top**
of the resolved coat as the very last step, so the full colourful field is
visible on any base - black, chestnut, or white (it does *not* interact with
pigment restriction). Exercises `HorseSkinGeometry` end to end. Expect it
removed once the engine is trusted.

---

## Reusable helpers (`common/coat/pattern/CoatRegions`)

All take a `Skin`. `fillMane` / `fillTail` / `fillEars` / `fillHooves`,
`paintLowerLeg`, `blacken*` / `whitenLowerLeg` / `whitenBlaze`, `redrawEyes`
(adult: 2x2 pupil + 2x2 sclera per eye; foal: 2x2 pupil - the baby texture has
no bright sclera). Parts a mesh doesn't have (a foal has no MANE / MUZZLE) are
silently skipped.

**Eyes** are owner-verified in-game 2026-09-01: they survive the coat on every
horse seen, adult and foal. They are still copied **verbatim from the
template**, though - *wanted:* a **genetic eye-colour** gene. The classic hook
is blue eyes on cream double-dilutes (`Cr/Cr`, `Cr/prl`), which would make it
the first gene to read the cream/pearl dose for something other than pigment.

## Adding a gene

1. New `Gene` impl - alleles (any token strings), `randomPair`, and either
   `restrict` (natural) or `isNatural()=false` + `overlayLayer`.
2. Register in `Genes`: append to `CODE_ORDER`, and to `NATURAL_ORDER` (in
   effect order) or `OVERLAY_ORDER`.
3. Document it here.
