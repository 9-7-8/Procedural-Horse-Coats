# Gene Dict

Every gene in the horse-genetics model. Code in `common/genetics/genes/`,
registered in `common/genetics/Genes.java`. See **CLAUDE.md** for the
architecture, **Docs/breeding.md** for inheritance.

---

## The pipeline in one paragraph

Every coat pixel starts at **max red + max black pigment** (a black horse) and
**zero magical colour**. **Natural** genes (`Gene.isNatural()`, the default)
each get a `restrict(pair, ctx, coat)` turn to push a `PigmentField` (per-texel
`red` / `black` in `[0,1]`) down - they do *nothing else*, and downward only.
The field is then resolved to colour through `redblackgradient.png`
(`GradientLut`) - fully restricted → transparent, resolves-to-pure-black → 80%
opacity - into a `ColorField`. Finally every **magical** gene (only Test so far)
gets a `tint(pair, ctx, coat, colour)` turn to **add signed R/G/B** on top of
that, accumulated uncapped and only clamped to 0-255 at conversion. The result
is composited onto the white template (adult or foal), and the eye texels are
copied back verbatim.

A gene is **natural or magical, never both**, and both hooks are **pure**: a
gene gets read-only views of the state so far and *returns* its contribution,
so it can be tested on its own. Full machinery in **CLAUDE.md**.

Because natural genes only move the pigment sample, **champagne-on-bay differs
from champagne-on-black, cream-on-chestnut differs from cream-on-bay, and
anything on white is invisible.**

Foals go through the exact same pipeline on the `Skin.BABY` geometry + the
128px `horse_white_baby.png` template. **Grey** is the only gene that reads
age: it greys adults only, so a foal is born its base colour.

Non-deterministic genes take all randomness from `ctx.epigeneticsFor(key)`.
That runs on the epigenetic seed of the **allele copy that expresses** at that
gene - heterozygote: the dominant copy; homozygote: the higher-**priority** one.
Seeds are rolled once for a founder and **inherited unchanged** with the allele
after that, so bay point heights and grey dapples run in families. See
**Docs/breeding.md** for the inheritance rules.

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
| **dominance** | `DOMINANT` - `Ee` is a plain black-capable horse, same as `EE` |
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
| **dominance** | `DOMINANT` - one `A` is enough for bay; `Aa` and `AA` are the same horse |
| wild frequency | 50/50 per allele (2 `nextBoolean`) |
| deterministic? | **no** when `A_` on a black-capable horse |
| visible when | `A_` **and** `genotype.hasBlackPigment()` |
| epigenetics | 5 `nextFloat` - point extent, then one jitter per leg |

`A_` → bay via `BayCoat.apply`: body black knocked to `BayCoat.BODY_BLACK`
(0.32, red kept → red-brown body); mane / tail / ears full black; black climbs
the legs + the face by a **random** amount and **fades out at its top edge**.
The heights come off the expressed `A` copy's epigenetics as **one "point
extent"** number spread **uniformly** over the whole range (`extent =
nextFloat()`), which is what makes bays actually run from low socks to seal
instead of clustering at the bottom the way the old `f*f` product did:

| number | formula | note |
|---|---|---|
| leg height | `0.15 + extent * 0.80` | the horse's average, then each leg is jittered `±14%` independently - four socks are never exactly level |
| face height | `0.04 + extent² * 0.62` | squared, so the face only climbs on the horses whose legs already did (seal, not "socks + a black face") |

The bottom
`SOLID_PORTION` = **0.3** of the band is solid black, then a **smoothstep**
fade to nothing over the rest - smoothstep (flat slope at both ends) so the
black dissolves into the body colour with no visible cut-off line. Hooves
always black (`HOOF_FRACTION` = 0.12).

**Seal brown** is just the top of this distribution - a high leg/face roll
("black creeps most of the way up"). There is no separate seal gene.

Owner-verified in-game **2026-09-01**: agouti renders correctly, the leg/face
fade reads right, and seal-as-a-high-roll works.

**Bay's black and the dilutions (fixed + owner-verified in-game 2026-09-01).** `BayCoat` paints its
points *absolutely* (`CoatRegions.blackenPart` / `blackenLowerLeg` /
`rampBlack*` all do `setBlack(1.0)` + `setRed(0.0)`) and agouti runs **before**
cream / pearl / champagne in `Genes.naturalOrder()`. That is fine on its own -
the dilutions do run after and *do* scale the points' black. What broke was the
**gradient**, not the ordering: with `red = 0` the gradient's zero-red column
stays visually jet black all the way down to `black ~0.4`, so scaling black
alone moved the sample without changing the colour (single cream's
`black *= 0.7` landed on `#111111`, double pearl's `*0.60` on `#272727`).

The fix is `PigmentField.dilute(keepRed, keepBlack, blackTint)`: every dilution
now also feeds a fraction of the **removed eumelanin back in as pheomelanin**,
which walks the sample sideways off that column into the warm browns where a
real diluted black lives. The ordering and the absolute bay point both stay as
they were. See each dose table below for the per-mode tint.

## `horsegenetics.cream` - Cream  &  `horsegenetics.pearl` - Pearl

Real-horse `SLC45A2`: allelic. Modelled here as **two genes** whose combined
effect is computed once, in `coat.pattern.CreamPearlDilution`.

| gene | alleles | dominance | wild frequency | deterministic? |
|---|---|---|---|---|
| cream | `Cr` (**incomplete** dominant), `N` (wild-type) | `INCOMPLETE_DOMINANT` - one `Cr` is a single dilution, two a double | `1 in 30` per allele | yes |
| pearl | `prl`, `N` (wild-type) | `INCOMPLETE_DOMINANT` - `prl/prl` is the mild uniform dilution, `Cr/prl` a double cream, so the heterozygote is its own thing | `1 in 22` per allele | yes |

Both are dilutions - they restrict / redistribute existing pigment, never add.
The combined rule (dose = number of `Cr` / `prl` copies):

| Cream | Pearl | mode | effect on the pigment field |
|---|---|---|---|
| 0 | 0-1 | none | - |
| 1 | 0 | **single cream** | keep red `0.45`, black `0.62`, tint `0.30` (buckskin on bay: golden body, **dark brown** points `#4B331A`) |
| 0 | 2 | **double pearl** | keep red `0.55`, black `0.52`, tint `0.28` - mild, uniform (apricot body, sepia points) |
| 1 | 1+ | **Cr/prl** | acts as double cream |
| 2+ | any | **double cream** | keep red `0.08`, black `0.38`, tint `0.33` (perlino: pale cream body, rusty points) |

"Keep" values multiply; **tint** is the share of a texel's black added back as
red (`PigmentField.dilute`) - see the bay section above for why scaling black
alone left every diluted bay's points jet. Red is always restricted harder than
black, so a diluted bay body fades to cream while the points hold smoky colour
(agouti still says *where*). **House rule (owner, 2026-09-01): no cream horse
keeps a pitch-black point.** A single-cream point may be a very dark brown but
never a void - so single cream carries a full tint, not the token amount a
real-world buckskin's black points would argue for. `CreamGene`
is the driver whenever a `Cr` is present; `PearlGene` drives only the
no-cream double-pearl case; either way `CreamPearlDilution.apply` runs at most
once.

Owner-verified in-game **2026-09-01**: buckskin, perlino and pearl bay all
render brown / rusty / sepia points over their diluted bodies.

*Not modelled yet:* blue eyes on the double dilutes.

## `horsegenetics.champagne` - Champagne

| | |
|---|---|
| alleles | `Ch` (dominant), `c` (recessive, wild-type) |
| inheritance | simple dominant, **not dose-dependent** |
| **dominance** | `DOMINANT` - one `Ch` gives the full dilution |
| wild frequency | `1 in 40` per allele |
| deterministic? | yes |

`Ch_` moves the pigment sample through `PigmentField.dilute`: keep red `0.55`,
keep black `0.42`, **tint `0.30`** (that share of the texel's black is added
back as red). Reads the *current* pigment, so champagne-on-X all differ:

| base | body | points |
|---|---|---|
| chestnut (`ee`) - **gold champagne** | gold `#E3B045` | same (no black to dilute) |
| black (`E_ aa`) - **classic champagne** | taupe `#834A24` | same |
| bay (`E_ A_`) - **amber champagne** | gold `#C28F39` | chocolate `#785E41` |

Owner-verified in-game **2026-09-01**: an amber champagne keeps its chocolate
points over a gold body.

The tint is what makes amber champagne work. The previous rule set red
*absolutely* (`0.45 + 0.10*red`), which lands within `0.10` of the same value
whether the texel is a red body or a black point, so a champagne bay came out
**flat gold with no points at all**; and it cut black so hard (`*0.18`) that a
classic champagne on black came out gold too, indistinguishable from the gold
champagne on chestnut.

## `horsegenetics.grey` - Grey

| | |
|---|---|
| alleles | `G` (dominant), `g` (recessive, wild-type) |
| inheritance | simple dominant |
| **dominance** | `DOMINANT` - one `G` greys the adult out |
| wild frequency | `1 in 16` per allele |
| deterministic? | **no** when `G_` (every grey adult is its own horse) |
| visible when | `G_` **and the horse is an adult** |

`G_` on an **adult** renders a **dapple grey** (`coat.pattern.GreyCoat`). A
**foal** is born whatever colour it would be without grey; `restrict` no-ops
for `!ctx.isAdult()`.

**Grey is a remap, not a restriction, and that is the whole point.** Greying
replaces pigmented hairs with white ones, and a mix of white and dark hairs
reads **neutral** - so a grey has to land on the gradient's **zero-red column**,
the only place the LUT is actually grey. Scaling red and black *together* walks
the sample down the diagonal instead, and the diagonal runs through the
gradient's golds: at an equal `keep` of `0.4` a black horse samples
`(150, 109, 56)`, a tan. That is why the old flat `KEEP = 0.15` had to sit
almost on top of white to look grey at all - and why every grey then looked
like the same white horse.

So `GreyCoat` works out how dark each texel currently is
(`0.55*red + 0.95*black`), puts that darkness back as **black** pigment scaled
by how far greying has gone, and keeps only a fading trace of the red. What was
underneath still shows: a greying chestnut ends lighter than a greying black,
and a barely-greyed horse keeps a rose / steel cast.

Four numbers come off the expressed `G` copy's epigenetics (plus a `long` that
seeds the dapple field):

| knob | range | what it does |
|---|---|---|
| **progression** | 0 .. 1 | keep = `lerp(0.46, 0.10, p)` - dark steel grey → nearly white. Fixed for life (no age input); this is what makes one grey different from the next |
| **dapple spacing** | 2.8 .. 5.0 body units | size of the dapples (the body is ~22 units long) |
| **dapple strength** | 0.5 .. 1 | contrast between a dapple centre and the web around it, up to `DAPPLE_DEPTH` = 0.42 (generous on purpose - most of a composed texel's variation is the white template's own shading). Scaled to peak mid-greying - a barely-started or almost-finished horse has little pigment left to vary |
| **point retention** | 0 .. 1 | how much longer mane / tail / ears / muzzle (full), head (half) and the lower legs (ramped, gone by mid-cannon) hold their colour. Scaled by `1 - progression`, so it's the young greys that show dark points on a light body |

Dapples come from `BodyNoise.cellDistance` - distance to the nearest point of a
jittered lattice, sampled in **body space** so the rings cross part seams
without a join, and warped by a low-frequency `BodyNoise.value` so the lattice
flows instead of gridding up. Near a lattice point = a dapple centre (lighter);
out in the gaps = the web (darker).

*Not modelled:* progressive greying with actual age, and flea-bitten grey. Both
want an age input to the pipeline.

## `horsegenetics.white` - Dominant white

| | |
|---|---|
| alleles | `W` (dominant), `w` (recessive, wild-type) |
| inheritance | simple dominant |
| **dominance** | `COMPLETE_DOMINANT` - while `W` shows, **no other gene is visible**; every white horse looks alike |
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
| **dominance** | `INCOMPLETE_DOMINANT` **(aspirational - see the open issue below)** |
| wild frequency | `1 in 20` per allele |
| deterministic? | **no** |

Random white markings, "dipped in white from below". Removes both pigments
(→ transparent → white template) up **each leg independently** a random amount
(`0.15 + f*f*0.75` of leg height - socks .. stockings) plus a random
**face blaze** down the centreline (`whitenBlaze`: random half-width
`0.4 + f*1.4` body units, length `0.2 + f*0.75` of the head).

Owner-verified in-game **2026-09-01**: what's implemented renders correctly.

> **Open issue - three gaps.** (0) It is tagged `INCOMPLETE_DOMINANT` but
> doesn't act like it: `restrict` never looks at how many `Spl` copies the
> horse carries, so `Spl/spl` and `Spl/Spl` render identically. Homozygous
> splash should give **much larger** white markings (higher stockings, a wide
> blaze/bald face, body patches). The catalogue already gives the heterozygote
> its own pen, so the fix is visible the moment it lands.
> (1) The blaze is the **only** face marking; the
> rest of the family (star, snip, stripe, bald face) isn't built. (2)
> `CoatRegions.whitenLowerLeg` cuts at a hard `point.y() <= cutoff`, so every
> sock ends in a **perfect ring**. Break the edge up - per-texel epigenetic
> jitter, or the smoothstep treatment `BayCoat.fade` already uses.

---

# Magical genes

Invented genes. They run in **phase 3**, after the pigment field has been
resolved to colour, and they **add signed RGB** rather than restricting pigment
(`tint`, not `restrict`). Order among them is `Genes.magicalOrder()` =
**pink hair -> magic zebra -> test**; the additive ones commute, but pink hair
reads what it is painting over and Test paints flat, so the list is not
arbitrary - see each entry.

## `horsegenetics.magic_zebra` - Magic zebra

| | |
|---|---|
| alleles | `Mzeb` (dominant), `n` (wild-type) |
| inheritance | simple dominant - `Mzeb/n` and `Mzeb/Mzeb` look alike |
| **dominance** | `DOMINANT` |
| wild frequency | `1 in 100` **per allele** (~2% of wild horses carry, and a carrier shows) |
| natural? | **no** - magical (phase 3) |
| deterministic? | **no** - five knobs off the expressing `Mzeb` copy |

Black stripes hung from the **topline**, reaching down the horse's sides.

- **Not the natural zebra gene.** A real-world zebra-striping locus is a
  separate, later, *natural* gene. This one is invented and paints over whatever
  the melanin genes produced.
- **Strength is `-200%`** on all three channels (`MagicZebraGene.STRIPE_PERCENT`).
  Deliberate overkill, and the reason the phase-3 accumulator is an unclamped
  signed `int`: a resolved channel tops out at 100%, so a stripe lands hard on 0
  and reads black over *any* coat - cremello, chestnut, grey - without the gene
  knowing what else the horse carries. It also raises opacity, so the stripes
  show on a **dominant-white** horse.
- **The stripe field is `coat/pattern/BodyStripes`** - bands of near-constant
  body-space X, warped by `BodyNoise` so they wiggle and taper. The phase also
  carries a small **slant on `|z|`**, which bends each stripe into a shallow
  chevron over the back. That is not decoration: without it, every face
  perpendicular to X - chest, rump, the front and back of each leg - sits at one
  phase and renders as a flat band. `BodyStripes` is deliberately generic, and
  is what a natural **dun**'s leg barring and **brindle** should reuse.
- **Epigenetic knobs**, off the `Mzeb` copy in this order: `nextLong()` (the
  stripe field's seed), then `nextFloat()` for **spacing** (2.2-4.2 body units),
  **width** (0.32-0.56 of a period), **bend** (0.6-2.2 units of warp) and
  **reach** (0.35-0.95 of the drop from topline to hooves, with the last quarter
  spent fading out). So one magic zebra is striped just over the back and
  another to the hooves. A foal that inherits the copy inherits the pattern.
- Everything **above** the topline - head, neck, mane, ears - is inside the
  stripes at full strength.

## `horsegenetics.pink_hair` - Pink hair

| | |
|---|---|
| alleles | `Pihr` (recessive), `n` (wild-type, dominant) |
| inheritance | simple **recessive** - only `Pihr/Pihr` shows |
| **dominance** | `RECESSIVE` - the first gene in the mod that is |
| wild frequency | `1 in 12` **per allele**, so ~1 wild horse in 144 shows it and many more carry it |
| natural? | **no** - magical (phase 3) |
| deterministic? | yes |

The **mane and tail** turn pink.

- **It reads before it writes.** Flat paint would throw away the shading the
  natural phase gave those strands and leave a dead pink patch, so the gene asks
  `ColorView.visible` what each hair texel currently looks like and returns the
  delta that walks it **82%** of the way to hot pink (`255,105,180`). The mane
  keeps its own light and dark while ending up unmistakably pink on a black, a
  chestnut or a cremello alike. It raises opacity too, so a dominant-white horse
  gets pink hair rather than nothing.
- **A blind `add` cannot do this** - and that is the point of phase-3 read
  access. To reach pink on a black mane a fixed delta has to push so hard that a
  pale mane saturates to white. The cost is that this gene is
  **order-dependent**, which is why it runs *before* magic zebra: stripes should
  black out pink hair, not the other way round.
- **A recessive is something you breed for** - a single `Pihr` is invisible, so
  finding one is a breeding problem rather than a spotting problem. That's the
  reason for the comparatively common allele frequency.
- **Foals get a pink tail only.** The foal mesh has no `MANE` part, so the mane
  comes in with adulthood.
- One intensity, no per-horse variation yet; alleles for a couple of intensities
  are the obvious extension.

## `horsegenetics.test` - Test (diagnostic)

| | |
|---|---|
| alleles | `T` (dominant), `t` (recessive, wild-type) |
| inheritance | simple dominant |
| **dominance** | `COMPLETE_DOMINANT` - painted flat and opaque, so one `T` hides whatever is underneath |
| wild frequency | `1 in 4` carriers (deliberately common) |
| natural? | **no** - magical (phase 3) |
| deterministic? | yes |

`tint` returns a delta built with `ColorField.set` - **flat, opaque paint**
rather than the additive tint every other magical gene should use - carrying the
`TestCoatPattern` gradient (pink→blue along body X, red→yellow along body Y).
Setting instead of adding is what makes the field read the same on any base -
black, chestnut, or white (it does *not* interact with pigment restriction) -
and it's reserved for `COMPLETE_DOMINANT` genes for exactly that reason.
Exercises `HorseSkinGeometry` end to end. Expect it removed once the engine is
trusted.

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

1. New `Gene` impl - alleles (any token strings), `randomPair`,
   `dominance()` (see `DominancePattern`; it decides how many pens the gene
   gets in the horse dimension's gallery), and either `restrict` (natural) or
   `isNatural()=false` + `tint` (magical). Never both hooks.
   - `restrict` takes `coat.mutableCopy()`, paints into it and returns it;
     return `null` for "no contribution". Never write through the view.
   - `tint` returns `ColorField.deltaLike(colour)` filled with `add` - signed,
     and order-independent because of it. `set` is flat paint and is only for a
     gene that masks everything. To paint a texel the natural phase left
     transparent (dominant white, a splash marking) you must raise
     `addOpacity`/`set`; colour alone won't show there.
2. Register in `Genes`: append to `CODE_ORDER`, and to `NATURAL_ORDER` (in
   effect order) or `MAGICAL_ORDER`.
3. Document it here.
4. If the gene changes an existing coat, regenerate
   `common/src/test/resources/coat-golden.txt` - see `CoatPipelineGoldenTest`.
