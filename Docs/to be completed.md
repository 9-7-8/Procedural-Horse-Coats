# To be completed

The **future-features backlog**. Nothing in this file is implemented; nothing
in it should be treated as a description of the program. It is the long list of
genes and systems the mod is aiming at, plus - for each one - **what would have
to change in the code to accommodate it**.

How it relates to the other docs:

- **`Docs/to be verified.md`** is "built, not yet seen in-game". This file is
  the opposite end: "not built at all".
- **`Docs/Gene Dict.md`** documents genes that **exist**. When a gene here
  ships, write it up there and **delete its row from this file**.
- **`Docs/breeding.md`** owns inheritance. Any inheritance rule sketched here
  (maternal mtDNA, embryonic lethals, polygenic traits) moves there on
  implementation.
- **`CLAUDE.md`** owns status and the "Known gaps / next steps" list. That list
  stays short and near-term; the long-range material lives here. A gap that
  gets a real design in this file can be reduced to a pointer there.

Order of the file:

1. [The three-phase pigment pipeline](#1-the-three-phase-pigment-pipeline) - the
   architectural prerequisite for almost everything else.
2. [Gene priority and processing order](#2-gene-priority-and-processing-order)
3. [Adding a gene: the modder-facing API](#3-adding-a-gene-the-modder-facing-api)
4. [The gene backlog](#4-the-gene-backlog) - the master table, annotated.
5. [Loci that aren't one-gene-two-alleles](#5-loci-that-arent-one-gene-two-alleles)
6. [Non-coat genes: performance, size, health](#6-non-coat-genes-performance-size-health)
7. [Horse care: milking and gated healing](#7-horse-care-milking-and-gated-healing)
8. [The horse dimension: back to random pens](#8-the-horse-dimension-back-to-random-pens)
9. [The custom horse spawner (creative only)](#9-the-custom-horse-spawner-creative-only)
10. [What breaks at scale](#10-what-breaks-at-scale)
11. [Suggested order of work](#11-suggested-order-of-work)
12. [Decisions still open](#12-decisions-still-open)

**This file is work to be done, not reasoning about why.** The *why* - what the
mod is for, what it refuses to do, and the rules every change has to respect
(determinism above all) - lives in **`Docs/Philosophy.md`**. When something here
needs a justification longer than a clause, that justification belongs there and
this file should link to it.

---

## 1. The three-phase pigment pipeline

### The idea

A magic Minecraft horse's cells carry **five pigments**:

| pigment | wild-type level | who moves it |
|---------|-----------------|--------------|
| **eumelanin** (black) | max | natural genes, downward only |
| **phaeomelanin** (red) | max | natural genes, downward only |
| **red** | 0 | magical genes, upward only |
| **green** | 0 | magical genes, upward only |
| **blue** | 0 | magical genes, upward only |

The wild-type horse is therefore **maximal eumelanin + phaeomelanin, zero
RGB** - a pure black horse - and the pipeline runs in three phases:

1. **Natural (melanin) phase.** Every visible natural gene gets a turn to
   *reduce* how much eumelanin / phaeomelanin a given cell can produce.
   Downward only; this is exactly today's `PigmentField` + `Gene.restrict`.
2. **Resolve.** The surviving `(phaeomelanin, eumelanin)` pair is looked up in
   the red/black gradient and becomes an **RGB colour**. Today's
   `GradientLut.sample` step.
3. **Magical (RGB) phase.** Every visible magical gene gets a turn to **add or
   remove** red, green and/or blue at a given cell. Signed, and accumulated
   **unclamped** - see below.

The worked example: a plain black horse, then bay restriction (natural, phase
1), then cream dilution (natural, phase 1) - so far a buckskin. Then the
magical **zebra** gene (phase 3) *removes* a large amount of all three channels
along a set of stripes, which clamps to zero and renders as a buckskin with
black stripes.

**A gene is natural or magical, never both** - mutually exclusive, declared,
not inferred; a gene that wants both registers as two genes. **Natural is
reserved for genes that exist in real life** (`Docs/Philosophy.md` §6).
Priority (section 2) orders genes *within* a phase; it does not decide the
phase.

### The RGB channels: nominally 0-255, actually `int`

- Each channel **starts at 0** and is nominally a 0-255 scale. The **guidance
  for a gene author is to think of 0-255 as 0-100%** and to write the gene's
  numbers on that scale.
- The **true accumulator range is `Integer.MIN_VALUE` to
  `Integer.MAX_VALUE`.** Genes add and subtract freely, without clamping, and
  the value is only **capped to 0-255 at the moment the overlay is converted
  into actual colour**.
- That headroom is the point. A fantasy gene can apply *so much* blue that no
  combination of other genes can pull it back under 255 - the horse is blue,
  unconditionally, and the author didn't have to know what else the horse
  carries. Zebra is the same trick with the sign flipped: subtract enough from
  all three channels that the stripe is black no matter what is underneath.
- **This is the ordering answer for phase 3.** Signed integer addition is
  associative and exact, so the magical phase is **order-independent and
  drift-free** - two genes that both touch blue give the same result either
  way round. (Gene order still matters for phase 1, and the deterministic order
  in section 2 still applies; phase 3 just stops being a place where a bug can
  hide.) It also means phase 3 needs no float maths at all, which sidesteps
  the linear-vs-sRGB question entirely for additive genes.

### It is an overlay, not flat paint

The RGB result is **applied over whatever the natural phase produced**, not
stamped on top of it. Painting flat would throw away the shading the coat
already has - the dapples in a grey, a bay's points, the gradient's own
variation - and leave a dead, uniform patch of colour.

So a magical gene modifies the resolved natural colour rather than replacing
it, and the existing multiply-onto-the-template step still runs afterwards, so
the template's own detail (hooves, nostrils, the shading between mane strands)
survives on a magical coat exactly as it does today.

**Exactly which blend that is - straight signed add on the resolved RGB, or
something closer to a real overlay/soft-light blend - is deliberately left open
until there's something to look at.** Start with the straight add (it is what
the unclamped `int` model describes, and it is the only one that keeps phase 3
order-independent), bake some samples, and tweak from there. Anything fancier
than an add reintroduces order-dependence, so if the blend changes, re-read
this section's ordering claim.

### Genes read the current state and return a delta

Every gene ships a function of the shape:

```
delta = gene.contribution(pair, ctx, naturalCoat, magicalDeltas)
```

- It is handed the **current natural coat** (the pigment field as the natural
  phase has left it so far) **and the current magical deltas** - the
  accumulated signed RGB, **not** the composited image.
- It returns **its own delta**, to be applied to whichever field its phase owns.
- It does not mutate shared state. Two genes handed the same inputs return the
  same outputs, always.

**Why read access is required.** The target cases are things like "add a red
border to every white area" and "turn all the black to pink" - both have to
*find* the region before painting it, and neither is expressible if a gene can
only write blind, which is what today's `overlayLayer` does.

**Why deltas and not the composite.** A gene reading the composited image would
be reading the template's shading and the eye texels too, and would be coupled
to compositing order. Reading the deltas keeps a gene looking at *pigment
decisions*, which is the thing it can reason about.

**Consequences worth building for:**

- Both fields need **read-only views** to hand out - a gene that writes through
  its input parameter has just broken the "returns a delta" contract quietly.
- The delta a gene returns is sparse in practice (most genes touch a minority
  of texels). A dense `int[]` per gene per bake is simple and probably fine at
  16 384 texels; measure before optimizing.
- This is a bigger change than "rename `overlayLayer`": it converts
  `Gene.restrict(pair, ctx)` from *mutate the shared field* to *return a
  delta*, and every shipped gene changes shape with it. Worth doing at the same
  time as the phase-3 work rather than twice.
- It makes each gene independently testable - hand it a synthetic natural coat,
  assert on the delta - which nothing in the current design allows.

### Why this is a real change and not a rename

Today phase 3 does exist, but it is not additive colour - it is
`Gene.overlayLayer(pair, ctx, int[] layer)`, an ARGB layer that is painted
**flat on top**: "an opaque layer texel replaces whatever the natural pass
resolved there". That is a *replace*, and it has exactly one consumer
(`TestGene`). Replace can't express "add 40% red to whatever is already
there", which is what the zebra example and every future magical gene want -
and it can't express two magical genes interacting, because the second one
overwrites the first instead of stacking on it.

### The concrete changes

- **A new field type next to `PigmentField`.** Call it `ColorField`: per texel
  a **signed `int`** `r`, `g`, `b` plus an **`alpha`/opacity** channel, with
  `add(px, py, dr, dg, db)` and **no clamping until conversion** -
  the accumulator is a full `int` per channel per texel (three `int[]`s, 16 384
  texels each, so ~200 KB of scratch; fine).
  - Seed it from the resolved natural colour so a gene that adds nothing
    changes nothing.
  - Guard the accumulation against `int` overflow, or a gene author's
    "obviously large" `Integer.MAX_VALUE / 2` plus a second one wraps negative
    and turns the always-blue horse black. Saturating add, or accumulate in
    `long` and cap once.
  - It needs an explicit alpha because transparency currently rides on the
    pigment channels: `CoatTextureComposer.TRANSPARENT_EPS` says "both
    pigments essentially zero -> transparent", which is how dominant white and
    splash markings work. Once phase 3 can add colour to a transparent texel,
    "no pigment" and "no paint" stop being the same statement and need separate
    storage. **A magical gene can paint a dominant-white horse** - settled:
    dominant white is a *natural* gene, and every magical gene runs after every
    natural one, so a positive RGB contribution must be able to make a
    transparent texel opaque again.
- **`Gene`'s coat hook splits in two, and both become pure.**
  - Natural phase: `restrict(...)` stops mutating `ctx.pigment()` and instead
    **returns a pigment delta**, given read-only views of the natural coat and
    the magical deltas.
  - Magical phase: `overlayLayer(pair, ctx, int[] layer)` is **replaced** by a
    `tint(...)` that likewise **returns an RGB delta** from the same two
    inputs.
  - The Javadoc on `Gene` (which currently spells out the two-pass model in
    full) and on `CoatTextureComposer` both describe the pipeline normatively
    and must be rewritten in the same change.
- **`CoatBuildContext` gains the colour field** alongside `pigment()` and
  loses the raw `int[] overlay()` accessor, or keeps it as the composited
  output only.
- **`CoatTextureComposer.compose` grows a third loop.** Order becomes: natural
  loop -> resolve into the `ColorField` -> magical loop -> composite onto the
  template -> `redrawEyes`. The `PURE_BLACK_ALPHA` (80% opacity on pure black)
  rule and the multiply-onto-template step both live *after* phase 3 and need
  re-checking against magical colours - a magical gene that paints bright cyan
  will be multiplied by the template's shading, which is probably what we want
  (mane strands still read) but has never been tried.
- **`Genes.overlayOrder()` becomes `magicalOrder()`**, and both it and
  `naturalOrder()` become *derived* from the priority numbers rather than
  hand-written lists (section 2).
- **`TestGene` is the migration canary.** It is the only non-natural gene, so
  port it first: it currently paints its gradient flat; under the new model it
  should set RGB directly (an "opaque paint" is `alpha = 1` plus an absolute
  `set`, not an `add`).
- **Tests.** New `common` tests for `ColorField` clamping/additivity, for the
  three-phase ordering, and a regression test that a horse with no magical
  genes composes **byte-identically** to today's output. That last one is the
  cheapest safety net for the whole refactor.
- **`./gradlew :common:bakeCoatSamples`** keeps working unchanged and is the
  visual check.

### Knock-ons

- **Texture cache keys** (`CoatData.textureKey()`) don't change shape - the
  genotype code plus the epigenetic fingerprint still identifies the coat.
- **Nothing in `neoforge-26.1.2/` should need to change**, which is the test of
  whether the refactor stayed inside `common/`. If the renderer needs touching,
  something leaked.

---

## 2. Gene priority and processing order

### The model

Two different priorities, at two different levels. They are easy to confuse, so
the names should stay distinct in code and docs.

**1. Gene priority - hard-coded, per gene, decides processing order.**

- Every `Gene` declares a fixed `int` priority. It is a constant of the gene,
  not data on a horse, and it never varies between horses.
- **The phase is declared separately** (section 1): a gene is natural or
  magical, mutually exclusive. Priority orders genes; it does not choose the
  phase.
- **`0`-`99` is the natural band by convention.** Two-digit numbers are where
  the real-world genes shipped by this mod live.
- **`100` and up is the magical band** - `100` itself is magical, and **every
  three-or-more-digit priority is magical**. The number inside the band is the
  mod author's choice.
- **The band is a convention, not a constraint.** Registering outside it
  **logs a warning** and carries on; it does not fail the load. The warning is
  there so a modder who squats on `50` finds out, not so the game refuses to
  start.
- The engine walks **all natural genes first, in ascending priority**, then
  **all magical genes, in ascending priority**. Because the bands are only a
  convention, the phase - not the number - is what separates the two passes.
- **Ties are broken alphabetically by gene key**, so two mods that both pick
  `5000` still produce one fixed, reproducible order. Arbitrary, but
  deterministic - which is the requirement (`Docs/Philosophy.md` §2).
- **The same order is used to roll a founder's genes** (see below), so a gene
  can only ever look at genes with a *lower* priority than its own.

**2. Epigenetic priority - per allele copy, decides which copy expresses.**

This one already exists and already works: `AlleleEpigenetics.priority` rides
on each allele copy, is inherited with it, and is deconflicted at birth so a
horse never carries a tie at one gene. Its job is to answer *which copy's
epigenetics the coat reads*:

- **heterozygote** - the dominant copy expresses (an `AllelePair` is
  canonicalized dominant-first);
- **homozygote**, or any case where both copies would express - the copy with
  the **higher** epigenetic priority wins.

It selects a **seed**, never an order. It cannot move a gene in the processing
queue, and it must never be allowed to - see `Docs/Philosophy.md` §2.

### Founder spawn chance, and why it uses the same order

**Every gene declares a flat 0-100% chance of appearing in a random founder
horse** - replacing the frequency that each gene currently hand-rolls inside
its own `randomPair`.

The chance may be **genome-aware**: a gene is handed the partial genome built
so far and returns its own probability, so "champagne is twice as likely on a
chestnut" is expressible without either gene knowing about the other's
internals. That is only sound because founders are rolled **in priority
order**, which means:

- a gene can inspect **only genes with a lower priority than its own** - the
  ones already decided;
- so every magical gene can see every natural gene (the bands guarantee it),
  but not vice versa;
- and a gene asking about a higher-priority gene is a programming error the
  registry should catch, not an empty answer it should paper over.

Notes for the implementation:

- **Multi-allele genes need the semantics pinned down.** "A 5% chance the gene
  appears" is clear for a two-allele gene. With five alleles it has to mean
  something like "5% chance this horse carries a non-wild-type allele, then a
  weighted second draw picks which" - and whether that draw is per allele copy
  or per horse changes how often homozygotes turn up. Worth settling with the
  first three-allele gene (section 5.1).
- The draw must stay **deterministic given the RNG stream**, so the roll order
  is the sorted order and nothing else.
- This is the natural home for the weighted-allele helper section 5.1 wants,
  and it is what lets a tier-1 declarative gene exist without writing any
  probability code.

### What exists today

- Processing order is two hand-written lists in `Genes`: `NATURAL_ORDER`
  (extension → agouti → cream → pearl → champagne → grey → white → splash) and
  `OVERLAY_ORDER`. Adding a gene means editing them, the ordering isn't visible
  from the gene class, and a third-party gene can't insert itself at all.
- `AlleleEpigenetics.priority` works as described above; its Javadoc notes it
  is "kept as a full-range int because more uses are planned".
  **Under this design that stays its only job** - the planned second use
  (reordering genes) is explicitly rejected, and the Javadoc should say so.

### The concrete changes

- **`Gene` gains `int priority()`** - no default, so every gene has to answer.
- **`Genes` sorts.** `naturalOrder()`, `magicalOrder()` (renamed from
  `overlayOrder()`) and `all()` become derived: one sort on
  `(priority, geneKey)` at registry-freeze time. Keep the method names so
  callers don't change; make the lists a *result*, not a *source*.
- **`codeOrder()` uses the same comparator.** Today the code string's segment
  order is a hand-written list, which is fine for a closed registry but breaks
  the moment a third-party gene exists (section 3) - the code has to read the
  same on every install regardless of load order. Same sort, same answer,
  everywhere.
- **Check the band at registration, and only warn.** A natural gene at `100`+
  or a magical gene below `100` gets a **log warning** naming the gene and the
  convention; the load continues. Duplicate gene keys, by contrast, *are* a
  hard failure - that one really is unrecoverable.
- **Pick the reserved numbers with room to grow.** The current natural order
  maps cleanly onto a spaced-out numbering, leaving `0`-`9` free and gaps to
  insert into:

  | priority | gene | why here |
  |---|---|---|
  | 1 | **sex (XX/XY)** | must be first - every X-linked gene reads it (section 5.3) |
  | 10 | extension | decides which pigment exists at all |
  | 20 | agouti | restricts black to the points; **sets pigment absolutely** |
  | 30 | MATP (cream + pearl) | dilutions, must run after the absolute set |
  | 40 | champagne | dilution |
  | 50 | grey | remaps onto the neutral column |
  | 60 | white | masks everything |
  | 70 | splash | white markings, set absolutely |

- **The ordering constraint is now an authoring rule, and needs writing down.**
  Phase 1 is multiplicative restriction, which is commutative - *except* where
  a gene sets pigment absolutely. `BayCoat` sets its points absolutely
  (`setBlack(1.0)` / `setRed(0.0)`), and the whole `PigmentField.dilute` design
  exists because the dilutions have to run **after** that. So the reserved band
  isn't just a list of numbers, it's a dependency order: **absolute setters get
  low numbers, dilutions get higher ones.** Document the constraint on
  `PigmentField` and on the priority field, or the next gene added at the wrong
  number will produce a bay whose mane doesn't dilute and nobody will know why.
- **`Docs/breeding.md`** already documents epigenetic-priority inheritance and
  the deconflict rule; extend it there rather than re-describing it here.

---

## 3. Adding a gene: the modder-facing API

A stated goal: **adding a gene should be easy**, for this mod and for other
mods. Today it is not. `Genes` is a closed registry (`private static final
List<Gene> CODE_ORDER = List.of(...)`), a gene is a hand-written class
implementing an interface with eight methods, and three separate hand-written
orderings have to be edited in step. Section 2's derived orderings remove one
third of that; the rest is this section.

### What a gene is allowed to do

A gene has **three kinds of effect**, and may have any combination of them -
including none of one, or all three:

1. **The natural coat** - restrict eumelanin / phaeomelanin in phase 1
   (section 1). Every gene shipped today does only this. **Reserved for genes
   that exist in real life** (`Docs/Philosophy.md` §6).
2. **The artificial coat** - add or remove R / G / B in phase 3 (section 1).
3. **Status effects** - the non-coat traits: stats, body size, health, and
   whatever else the design lands on. Sketched in section 6; **the full list is
   still to come and this section will need revisiting when it lands.**

**A gene is natural or magical, never both** - the two coat effects are
mutually exclusive, and a gene that wants both registers as two genes. Status
effects are orthogonal: either kind of gene can have them, or a gene can be
status-only with no coat effect at all.

Whatever it does, a gene's coat contribution is the **pure delta function**
from section 1: given the natural coat and the magical deltas as they stand, it
returns its own delta.

The tiers below exist so a gene that only does one of these is cheap to write.

### Allele rules

Binding on every gene, this mod's and anyone else's:

- **Tokens are alphanumeric** - `a-z`, `A-Z`, `0-9`, nothing else. No spaces,
  no punctuation. This keeps the code string's `/` and `-` separators
  unambiguous for free, and it lets the real scientific allele names
  (`SW1`, `W36`, `D1`, `TE2`, `d1`) be used verbatim as tokens.
- **Length 1 to 128 characters**, with the strong guidance to **keep it to
  1-5**. Tokens end up on pen signs, in the info panel and in the genotype
  code; a 40-character token is legal and will look terrible everywhere.
- **A gene supports an indefinite number of alleles.** Two is the common case,
  not the assumption - the builder, the base classes, the pair enumeration and
  the founder draw all have to work for `n`.
- **The wild type is the least magical version of the gene**, and by convention
  it is **`n`**. For a real-world gene that's the ordinary allele; for a
  magical gene it's "this horse doesn't have the magic". Naming it consistently
  matters more than it looks: `n` is what a horse carries at every gene it
  doesn't express, so it's the most-printed token in the mod, and
  `GeneCodeDisplay.shortForm` leans on recognizing it to keep a genotype
  readable.

The numbered alleles in the master table (`SW1`-`SW10`, `W1`-`W36`, `D1`-`D4`,
`TE1`/`TE2`, `d1`/`d2`) are therefore legal tokens exactly as written - no
renaming scheme needed.

### Three tiers, so simple genes stay simple

Most genes on the backlog are one line of maths. Silver dapple is "multiply
eumelanin by 0.45 and leave phaeomelanin alone". It should not need a class.

- **Tier 1 - declarative.** A builder for the common cases, no class at all:

  ```
  Gene.natural("mymod.silver")
      .priority(45)
      .alleles("Z", "z")            // most dominant first; last is wild type
      .dominance(DOMINANT)
      .wildFrequency(1, 60)          // 1 in 60 carries the variant
      .restrictBlack(0.55f)          // the whole coat effect
      .build();
  ```

  Covers: uniform dilutions, uniform restrictions, and "no coat effect at all"
  (the performance / health genes in section 6, which only need alleles +
  inheritance + a trait contribution).
- **Tier 2 - abstract base classes.** For a gene with real logic but a normal
  shape. `AbstractNaturalGene` and `AbstractMagicalGene` take the alleles,
  dominance and frequency in their constructor and pre-answer everything the
  interface asks except the one method that matters - `restrict` or `tint`.
  An `AbstractPatternGene` on top of that adds the `BodyNoise` boilerplate and
  the "pull N epigenetic knobs off the expressing copy" helper that `GreyCoat`
  and `BayCoat` both hand-roll today.
- **Tier 3 - raw `Gene`.** Still there for anything unusual. Nothing is taken
  away; the tiers just mean a modder rarely reaches this far.

**What the base classes absorb** (all of it boilerplate today): building the
`Allele` objects and their keys, `precedence`, `fromToken`, `randomPair` from a
declared frequency, the `isVisible` / `isDeterministic` defaults, the dominance
metadata, and handing you the seeded RNG (`ctx.epigeneticsFor(key())`) instead
of making you find it.

### An open registry

- `Genes` becomes a real registry: register during startup, **freeze**, then
  serve. Frozen means `codeOrder()` and friends can be computed once and cached
  (they're on the hot path for every code parse).
- The registration hook lives in **`neoforge-26.1.2/`**, not `common/` - a
  mod-bus event (`RegisterHorseGenesEvent`) or a `DeferredRegister`-alike that
  calls into `common`'s plain-Java registry. `common/` stays Minecraft-free;
  that rule doesn't bend for this.
- **Load order must not matter** - orderings are sorted, never registration
  order (sections 2 and 3).
- Genes need a **namespaced key** (`<modid>.<gene>`); the existing
  `<modauthor>.<gene>` convention already does this, so it's a validation rule,
  not a new format.

### The contract a modder has to honour

Worth stating in the Javadoc of the base class, because a violation shows up as
someone *else's* horse rendering wrong:

- Allele tokens: **letters only, 1-128 characters** (aim for 1-5), unique
  within the gene, with **`n` as the wild type** - see "Allele rules" above.
- Declare the phase honestly. **Magical unless the gene exists in real life**,
  and never both.
- Priority **should** be `>= 100` for a magical gene; the `0`-`99` band is this
  mod's by convention and squatting on it only earns a log warning
  (section 2).
- All randomness through `ctx.epigeneticsFor(...)`. No unseeded RNG, no
  entity-derived input, no mutable static state (`Docs/Philosophy.md` §2).
- The coat contribution must be a **pure function** of its inputs that
  **returns a delta** rather than writing through its arguments (section 1).
- Only inspect **lower-priority** genes in a genome-aware spawn chance; the
  higher ones haven't been rolled yet (section 2).
- Declare `isDeterministic` honestly: claiming determinism you don't have
  poisons the coat cache for every horse sharing your genotype.

### What third-party genes break

- **The genotype code gains a segment when a gene mod is added.** Existing
  horses' codes then have the wrong segment count. The mod's standing rule is
  "no legacy / back-compat code" - but that rule was written for a single dev
  changing his own format, and "player installs a mod" is a different, normal
  event. Needs a policy: pad missing segments with wild type on parse
  (forgiving, hides genuine corruption) or reject and re-roll (clean, loses
  the horse). **Leaning toward padding with wild type**, since it's exactly
  what "this horse doesn't carry that gene" means. Removing a mod is the
  mirror case: drop unknown segments, warn once.
- **The dimension** stops caring, once section 8's revert to random pens
  lands - a random corridor is indifferent to how many genes are registered.
  A modded gene does still widen the per-pen sign, which is the one place a
  new gene shows up in the world.
- **`CoatTextureId`** encodes the whole code into an `Identifier` path, so more
  genes means longer paths (section 10).

---

## 4. The gene backlog

The master table, annotated. **Status** is against the code as it stands:
*shipped* = exists in `common/genetics/genes/`, *partial* = something with that
name exists but is not this, *new* = nothing exists.

**4.1-4.4 are the natural genes** - they all exist in real life, so they all
belong in the `0`-`99` band and all run before any magical gene. The scientific
allele names are legal tokens as written (section 3). **4.5 is the magical
list**, which is open-ended by design.

### 4.1 Coat colour / dilution

| Locus | Alleles | Inheritance | Status | Notes on what it needs |
|---|---|---|---|---|
| **Red factor (MC1R)** | `E`, `e`, `ea` | recessive | **partial** - `ExtensionGene` has `E`/`e` | Add the third allele `ea`. First real 3-allele locus, so it's the natural place to prove multi-allele support (section 5). `ea` is phenotypically red like `e`; the interest is in the code string and the carrier display. |
| **Agouti (ASIP)** | `A`, `a` | recessive | **shipped** | Real horses have `A+`/`A`/`At`/`a`; the master table's two-allele form is what we have. Seal stays the top of bay's epigenetic distribution, not an allele. |
| **Cream (MATP)** | `Cr`, `prl`, `sun`, `sno`, wild `C` | semi-dominant; recessive for pearl | **partial** - two *separate* genes, `CreamGene` + `PearlGene`, combined once in `CreamPearlDilution` | The big cleanup: **merge into one 5-allele MATP locus**. Removes the fiction that a horse can be `Cr/Cr` *and* `prl/prl`, which is currently expressible and biologically impossible. Breaks the genotype code (two segments become one) - fine, per the no-back-compat rule. The dose table moves from `CreamPearlDilution` onto the locus. `sun` / `sno` are new dilutions on top. |
| **Pearl** | `prl` | recessive | **partial** - see above | Stops being its own gene after the merge. |
| **Champagne** | `Ch`, `ch` | dominant | **shipped** | No change. |
| **Dun (TBX3)** | `D`, `d1`, `d2` | `D` dominant; `d1`/`d2` non-diluting | **new** | 3 alleles with **non-uniform dominance** (`D` > `d1` = `d2` for dilution, but `d1` carries primitive markings and `d2` doesn't) - so `DominancePattern` being *per gene* is not enough (section 5.1). Coat work: body dilution **plus** primitive markings. The dorsal stripe is a clean body-space function (`z ≈ 0` along the topline); leg barring is horizontal banding in `y` on the legs. Wants new `CoatRegions.dorsalStripe` / `legBarring` helpers. |
| **Silver dapple (PMEL17)** | `Z`, `z` | dominant | **new** | Dilutes **eumelanin only** - a `restrictBlack`-shaped pass that must not touch phaeomelanin, so a chestnut carrying it looks unchanged. The tier-1 declarative case (section 3) almost exactly. The trap is priority: it must sit **above agouti's number** or a bay's mane won't lighten (section 2). Health: `Z/Z` → MCOA (section 6). |
| **Mushroom** | `Mu`, `mu` | recessive | **new** | Dilutes **phaeomelanin only**, homozygous only. The mirror of silver; same shape, same tier. |
| **Tiger eye** | `TE1`, `TE2`, wild | recessive | **new** | No coat effect at all - **eye colour**. Blocked on an eye-colour system: `CoatRegions.redrawEyes` copies eye texels verbatim from the template, so there is no colour channel to write to. Needs `redrawEyes` to take a colour plus a resolver that also handles the classic blue-eyed cream double-dilute. Already on CLAUDE.md's gaps list as "genetic eye colour"; this is the gene that would consume it. |

### 4.2 White spotting and patterns

| Locus | Alleles | Inheritance | Status | Notes on what it needs |
|---|---|---|---|---|
| **Grey** | `G`, `g` | dominant | **shipped** (`GreyCoat` dapple grey) | With **aging deliberately out of scope** (section 6.4), grey's progression stays what it is today: drawn once from the `G` copy's epigenetics and fixed for life, so one grey is a steel four-year-old and another is near-white, but neither changes. That is now a **decision, not a gap**. Flea-bitten grey and melanoma risk are parked with aging. |
| **Leopard complex (TRPM1)** | `LP`, `lp` | incomplete dominant | **new** | The most pattern-heavy gene on the list: leopard, blanket, snowcap, varnish roan, plus the appaloosa "characteristics" (striped hooves, mottled skin, white sclera). Wants a `BodyNoise` spot field plus a separate blanket mask, and heterozygote vs homozygote **must** differ - that's the point of the pattern series. Realistically **PATN1** comes with it, so budget for a modifier gene. Health: `LP/LP` → CSNB. |
| **Tobiano** | `To`, `to` | dominant | **new** | Rounded vertical white patches that **cross the topline** - the defining shape. Body-space friendly: white where a low-frequency noise field crosses a threshold, biased toward crossing the topline. |
| **Overo (frame)** | `Ov`, `ov` | dominant | **new** | Horizontal white that **does not** cross the topline - the inverse constraint of tobiano, and a good test that both can be expressed in one noise framework. Health: `Ov/Ov` → **lethal white foal syndrome**, the first lethal in the model (section 6.4). |
| **Splashed white SW1 (MITF)** | `SW1`, `sw1` | incomplete dominant | **partial** - `SplashGene` is essentially this | Two open issues already logged in `Docs/to be verified.md`: it doesn't read its own dose (so `Spl/Spl` looks like `Spl/spl`) and the face-marking family is just the centreline blaze. Fixing the dose is the prerequisite for the rest of the SW series making any sense. |
| **Splashed white SW2-SW10** | `SW2`…`SW10` | completely dominant | **new** | **Not** one locus: SW1/3/5/6/8/10 are on **MITF**, SW2/4/7/9 are on **PAX3**. So two multi-allele loci - and MITF also carries SW1, i.e. today's `SplashGene` becomes the MITF locus with 7 alleles. Section 5.2. |
| **Sabino 1** | `SB1`, `sb1` | incomplete dominant | **new** | White roaning + belly splash + high leg white. Its own look, but shares `CoatRegions` helpers with splash - and fixing `whitenLowerLeg`'s hard `y <= cutoff` ring (an open issue today) benefits both. On **KIT**. |
| **Dominant white (KIT)** | `W1`…`W36`, wild | dominant | **partial** - `WhiteGene` is a single all-or-nothing `W` | Today's white is `COMPLETE_DOMINANT` and masks every other gene, which is exactly what keeps the gallery tractable (one white pen). The real series is 36 alleles of graded extent, most **not** fully masking. Expanding it is the single largest combinatorial change in this document (section 10) and should be deliberately deferred, or capped at a handful of representative alleles. |
| **Roan** | `Rn`, `rn` | dominant | **new** | Mixed white hairs over the body, head and legs left dark. At 128px the "mixed hairs" read is a high-frequency `BodyNoise` dither masked off the head and lower legs - the closest thing to a pure `BodyNoise` gene on the list, and the cheapest pattern gene to build. Also KIT-linked in reality. |
| **Brindle (MBTPS2)** | `Br`, `br` | **X-linked** | **new** | Vertical striping in a darker or lighter shade of the base coat, following the body's lines. **The one sex-linked gene** (section 5.3), and the reason that scaffolding is needed at all - stallions are hemizygous and can never be carriers, so brindle runs visibly down one side of a pedigree. Pattern work is a body-space stripe function, sharing whatever the zebra gene and dun's leg barring establish; the interesting part is the inheritance, not the drawing. **Build the scaffolding first, then this gene as its proof.** |

### 4.3 Performance and body size

None of these touch the coat. All are blocked on the trait framework in
section 6.

| Locus | Alleles | Inheritance | Notes |
|---|---|---|---|
| **MSTN (myostatin)** | `C` (sprint), `T` (stamina) | incompletely dominant | The obvious first non-coat gene: additive, two alleles, maps to movement speed vs. stamina. Would replace part of today's random `HorseStats` roll with something Mendelian. |
| **DMRT3** | `A` (gait variant), wild | dominant | Gait - a **movement/animation** change, not a stat. The only gene here that needs renderer + movement work in `neoforge-26.1.2/`; lowest priority for that reason. |
| **PDK4**, **CKM** | multiple, not fully catalogued | polygenic | **Blocked on the 6.3 decision.** A marker set if polygenic survives; one atomic gene with a few alleles if it doesn't. |
| **Chr 1 QTL (RYR2)**, **ECA 1/8/9/26 QTLs** | QTL regions | polygenic | Jumping ability. Blocked twice over: the 6.3 decision, **and** jump strength isn't tracked at all today (`HorseRecords` handles speed + health only). If polygenic is cut, this collapses to a single atomic "jumper" gene. |
| ~~**Mitochondrial DNA**~~ | haplotype variants | maternal | **Cut.** Maternal inheritance is non-Mendelian, and the mod has decided not to model non-Mendelian inheritance at all (section 5.3). If the jumping/dressage flavour is still wanted, model it as an ordinary autosomal gene and accept the abstraction. |
| **LCORL / NCAPG** | `C` (big), `T` (small) | additive / codominant | Body **height**. Needs an entity-scale path in `neoforge-26.1.2/`: a scale attribute if 26.1.2 exposes one - **unverified, check before designing** - otherwise renderer-side scaling plus a hitbox change. Also interacts with foal geometry, which is already approximate. |
| **HMGA2** | `G` (tall), `A` (small) | additive | Same scale path. The `A` allele's insulin association is a hook if metabolic conditions ever land. |

### 4.4 Health and dwarfism

All **recessive**, most **severe**. They need the health system in section 6.4
before any of them can be written; without it they are inert genotype entries.
In game they surface two ways: a **lethal** foal is born and then dies shortly
after birth, and everything sub-lethal is **fewer hearts** (a reduced max-health
attribute).

| Locus | Alleles | Effect | Notes |
|---|---|---|---|
| **ACAN** | `D1`, `D2`, `D3*`, `D4`, wild | chondrodysplastic dwarfism | 5 alleles, and **any two** non-functional copies are affected - `D1/D4` is affected, not just `D1/D1`. So the health check cannot be "is it homozygous?"; it has to be a predicate on the whole `AllelePair`. Visually it's a **body-proportion** change: the LCORL scale machinery plus per-part scaling. |
| **B4GALT7** | mutant, wild | Friesian dwarfism | Limbs and ribs affected, head near-normal - again per-part scaling. |
| **FFS1 (PLOD1)** | mutant, wild | fragile foal syndrome | Lethal: affected foals die shortly after birth. |
| **EFIH (RAPGEF5)** | mutant, wild | no parathyroid glands | Lethal. |
| **NFS (ST14)** | mutant, wild | naked foal syndrome | The one health gene with a **coat** effect: an affected foal is near-hairless. A template swap, or a heavy phase-1 pass. Lethal, though in reality slowly - here it collapses to the same "dies as a foal" path. |
| **Skeletal atavism (SHOX/CRLF2)** | mutant, wild | abnormal limb growth | Lethal. |
| **MET** | mutant, wild | embryonic lethal | Homozygous embryos die **before birth**, so this one is checked at conception and the pairing simply produces no foal - a different code path from the others (section 6.4). |

Plus the health effects riding on colour genes already listed: **CSNB**
(`LP/LP`), **MCOA** (`Z/Z`), **LWFS** (`Ov/Ov`, lethal), and deafness on some
splashed-white variants. **Melanoma in greys is parked with aging** - without
an age axis it would be either "born with it" or nothing.

### 4.5 Magical genes - the to-do list

Invented genes, phase 3, priority `100`+. Unlike 4.1-4.4 this list has no
upstream source to be complete against - **add to it freely**. Each needs a
priority, an allele set with `n` as wild type, a spawn chance, and a delta
function (sections 1-3).

| Gene | Sketch | What it needs |
|---|---|---|
| **Zebra stripes** | Colour production driven to nothing along body-space stripes, so the horse keeps its own coat between black stripes. | The proof-of-concept magical gene: a large *negative* RGB contribution, which is what validates the unclamped signed model. Stripe function in body space; shares its maths with dun's leg barring and brindle's striping, so build one stripe helper and let all three use it. |
| **Pink mane and tail** | Mane, tail (and optionally forelock/feathering) rendered pink regardless of base coat. | **The cheapest magical gene, and the best first one.** `CoatRegions` already has mane and tail fills, so the delta is "large positive red + blue on these parts". Ideal tier-1 declarative test: if this needs more than a few lines, the authoring API isn't done. Worth alleles for a couple of intensities rather than one on/off. |
| **Cutie marks** | An emblem on each flank, mirrored left and right. | The most involved one on the list, and the first gene that wants an **asset** rather than pure maths - see the note below. |

**Cutie marks, in more detail**, because they break new ground:

- **Placement** needs a flank/haunch region in body space, mirrored across
  `z = 0`. `CoatRegions` has no such helper; it's a natural addition, and
  useful to any future gene that wants a patch in a specific place.
- **The mark itself** can come from a sprite atlas or from procedural shapes.
  A sprite atlas is far easier to make *look good* and is what the fiction
  wants (marks are emblems, not blobs) - but it makes this the first gene with
  a **texture dependency**, which has knock-ons: `common/` must stay
  Minecraft-free, so the atlas has to arrive as an `int[]` the way
  `GradientLut` already does, and a modder adding their own marks needs a way
  to register theirs.
- **How is the mark chosen?** Two models, and they play very differently:
  **allele per mark** (each mark is a heritable, breedable thing - a family
  line has *its* mark) or **one gene plus epigenetics** (the mark varies
  continuously and inherits with the allele copy). The first is the more
  interesting breeding game and fits the philosophy better; the second scales
  to hundreds of marks without hundreds of alleles. A hybrid - alleles for
  mark *families*, epigenetics for the variation within one - is probably the
  answer.
- **Colour** is separate from shape and could be its own gene, which would make
  mark and colour independently heritable. That's more fun and costs one more
  locus.
- **Scale check**: many alleles at one locus is exactly the case section 5.1
  has to support anyway, so this gene should come *after* multi-allele support.

**Capability tests rather than planned genes** - two shapes worth keeping in
mind because they're what the read-access API in section 1 exists for: "add a
red border to every white area" and "turn all the black to pink". Neither is a
committed gene; both are the acceptance test for whether a magical gene can
actually see what's underneath it.

---

## 5. Loci that aren't one-gene-two-alleles

Three structural assumptions get in the way, and all three are worth fixing
**before** the gene rollout rather than during it.

### 5.1 Three or more alleles per locus

Mostly already supported, and better than expected:

- `AllelePair` + `Gene.precedence(allele)` already order any number of alleles.
- `GenotypeCatalog.allPairsOf(gene)` already enumerates `n(n+1)/2` pairs for
  any `n`.
- The code string already tolerates arbitrary tokens (`Spl`, `prl`, `Cr`).

What does **not** hold:

- **`Gene.dominance()` is per gene, not per pair.** With `D`/`d1`/`d2`, or
  `W1`…`W36`, different pairs at the same locus have different dominance
  relationships. `DominancePattern` needs to become a function of the *pair*
  (or a per-allele dominance rank plus a pairwise override table).
  `GenotypeCatalog.distinctPairsOf` is the caller that changes - and it is also
  the one that decides how big the gallery is.
- **`randomPair(rng)` is hand-rolled per gene**, with the wild frequency baked
  into the draw. With 5 or 39 alleles that becomes a weighted table: declare an
  allele **frequency weight** and let one shared helper draw from it, so a gene
  class stops owning its own probability arithmetic. That helper is also what
  tier-1 genes (section 3) need in order to exist at all. Every gene currently
  documents its draw count and `Genome.breedWith`'s "2 `nextBoolean()` per
  gene" contract is asserted in tests - a weighted draw changes **founder**
  draws only, not breeding draws, but `Docs/breeding.md` must be updated in
  step.

### 5.2 One physical gene, several traits

**KIT** carries dominant white, tobiano *and* sabino 1. **MITF** carries SW1,
3, 5, 6, 8, 10; **PAX3** carries SW2, 4, 7, 9. **MATP** carries cream and
pearl.

Two ways to model it:

- **(a) One `Gene` per physical locus, many alleles.** Biologically honest,
  gets linkage for free (you can't inherit tobiano and W20 from the same
  parental chromosome unless they're the same allele), and shrinks the genotype
  code. Costs: the dominance-per-pair work above, and a `KIT` class that has to
  express many visually unrelated patterns.
- **(b) Keep them as separate `Gene`s** - what the code does for cream/pearl
  today - and accept that impossible genotypes exist.

**Recommend (a)**, starting with the cream/pearl merge: smallest case, and it
already has a shared resolver (`CreamPearlDilution`) to fold in.

### 5.3 Sex-linked genes - the one non-Mendelian exception

Inheritance is otherwise strictly Mendelian (`Docs/Philosophy.md` §5 has the
scope and the reasoning). **X-linked genes are the deliberate exception**, and
they need scaffolding before the first one can exist. **Brindle (MBTPS2)** is
the driving case - a real coat gene, and a real X-linked one.

**The biology to model**: mares are XX and carry two copies; stallions are XY
and carry **one**. So a stallion is never a carrier - whatever he has on his X,
he shows - and he passes it to every daughter and no son. A mare shows a
recessive X-linked trait only when homozygous. That asymmetry is the whole
appeal: a trait that runs visibly down one side of a pedigree.

**Sex itself becomes a gene.** `XX` / `XY` is registered like any other locus,
with alleles `X` and `Y`, at **priority 1** so it is always resolved first
(section 2). This is the change that makes the rest cheap:

- **Sex inheritance falls out of ordinary Mendelian breeding** with no special
  case at all. A mare is `X/X` and can only pass `X`; a stallion is `X/Y` and
  passes one or the other at 50/50. That is exactly what the existing two
  `nextBoolean()` per gene already does.
- **It resolves the ordering problem by construction.** X-linked genes need the
  foal's sex before they can segregate, and at priority 1 the sex gene is
  already decided by the time any of them are rolled - no reordering, no
  coupling to a separate `Sex` roll.
- **`HorseRecord.sex` should become derived**, not stored alongside the
  genotype. Two sources of truth for the same fact is a bug waiting to happen,
  and the genotype is now the authoritative one. Keep `Sex` as the enum the
  rest of the code reads; compute it from the locus.
- **It gets sex into the genotype code**, which the custom spawner (section 9)
  and the family tree both want anyway.

**Hemizygous notation.** A hemizygous allele is **displayed** with an `X-` or
`Y-` prefix - brindle's `Brn` renders as `X-Brn`. Because `-` is the gene
separator and can never appear inside an allele token (section 3), the prefix
is unambiguous on sight. **The code string is unaffected**: the token stays
`Brn`, and the game knows from the gene's declared inheritance mode how to
render it.

**Proposal for the storage question this leaves open** (my inference from the
notation, not yet your call): let the stallion's second slot hold the reserved
**`Y`** allele - the same `Y` the sex gene uses - meaning "this locus does not
exist on the Y chromosome". Then:

- `AllelePair` stays two-slotted, so **no structural change** to `Genotype`,
  and every existing consumer keeps working.
- **`Epigenome` alignment survives untouched** - still one entry per slot - and
  the `Y` slot simply carries epigenetics nothing ever reads. That removes what
  was going to be the fiddliest part of this change.
- A stallion's brindle locus reads `Brn/Y`, displayed `X-Brn`; a mare's reads
  `Brn/Brn`, displayed `X-Brn X-Brn`.

**What still has to change:**

- **`Gene` declares its inheritance mode** - autosomal (default) or X-linked.
  A gene shouldn't have to think about it beyond that declaration.
- **`breedWith` grows one case**: for an X-linked gene, the sire contributes
  his `X` allele to a filly and his `Y` to a colt, rather than a free coin
  flip. The dam is unchanged. It reads the already-decided sex locus.
- **Expression gets simpler, not harder.** A hemizygous stallion has one real
  allele, so it expresses - no dominance question, and no epigenetic-priority
  tie-break (that exists only to choose between two real copies).
- **`GenotypeCatalog`** enumerates every pair per gene, which would produce
  nonsense pairs like `Y/Y` for an X-linked locus. It needs to know the mode.
- **The founder spawn chance** (section 2) has to respect sex for an X-linked
  locus - a colt gets one draw, a filly two.
- **Surfaces**: the info panel and paper dump want *hemizygous* wording and the
  `X-` prefix, not a fake homozygote; a punnett square for an X-linked gene is
  a different square and needs its own path.

Not planned: Y-linked genes, imprinting, mosaicism.

---

## 6. Non-coat genes: performance, size, health

Today `Gene` is *defined* by its coat contribution: the interface's entire
Javadoc is the pigment pipeline, and its only non-trivial methods are
`restrict` and `overlayLayer`. Everything in sections 5.3 and 5.4 needs a
second kind of contribution.

### 6.1 The shape of the change

Add **capability interfaces** a gene may additionally implement, rather than
widening `Gene` with methods most genes ignore:

```
interface StatContribution  { void contribute(AllelePair, TraitBuilder); }   // speed, health, jump, stamina
interface BodyContribution  { void contribute(AllelePair, BodyBuilder); }    // overall + per-part scale
interface ViabilityRule     { Viability check(AllelePair, Genotype); }       // lethal at conception / at birth / fine
interface ConditionRule     { void conditions(AllelePair, ConditionSink); }  // CSNB, MCOA, deafness - flavour + UI
```

plus a `common/horse/HorseTraits` resolver that walks the genotype once and
produces a plain-data `Traits` record (stats, body scale, conditions,
viability). Same discipline as the coat: **all of it in `common/`**, with
`neoforge-26.1.2/server/HorseRecords` translating to Minecraft attributes the
way it already does for speed and health. It's also the tier-1 case from
section 3 - a performance gene has no coat effect at all, so declaring alleles
plus one stat contribution should be the whole gene.

### 6.2 Stats become (partly) Mendelian

`HorseStats` is explicitly a placeholder - a random roll in
`[0.75*min, 1.5*max]` of the parents. Target: the genetic contribution comes
from `HorseTraits`, and the roll survives only as **environmental noise** on
top. Consequences:

- `HorseRecord` gains fields (jump, stamina, height) - it is already a record
  with a large constructor, and `ParentStats`, the info panel, the paper dump
  and `FamilyTreeScreen`'s colouring all key off speed + health today.
- `Docs/breeding.md` owns this and would need a substantial rewrite.
- `backfillStatsIfMissing` becomes "recompute from genotype", which is
  *better* - it stops being a guess.

### 6.3 Polygenic traits - **on the fence, may be cut**

PDK4, CKM and the jumping QTLs aren't single loci in reality. The model that
fits the existing machinery is a **marker set**: N unlinked biallelic loci,
each an ordinary tier-1 gene with an additive weight and no coat effect, summed
by `HorseTraits`. No new inheritance code; the cost is N extra segments in the
genotype code and an N-fold catalogue blow-up (section 8).

**But the whole idea is under review**, and the argument against it is a good
one: **atomic genes are more fun.** A gene that does one legible thing is a gene
a player can breed *for*. Five anonymous markers that sum into a hidden speed
number are five segments of genotype code that nobody can aim at, and they push
against the "a player should be able to plan" principle - a trait you can only
influence statistically is exactly the kind of thing that reads as a dice roll.
Atomic genes also give more customization surface in the spawner UI, and they
are simpler for modders.

Three ways this can land:

1. **Cut polygenic entirely.** Every performance trait becomes one atomic gene
   with a handful of alleles - "this horse has the sprint allele" rather than
   "this horse scored 3 of 5 speed markers". Loses realism, gains legibility,
   removes a whole subsystem. **Currently the leaning option.**
2. **Keep it, but only where the realism earns it** - the racing QTLs and
   nothing else, accepting they're a background statistical layer.
3. **Fold it into group traits (6.5)**, which are wanted regardless. A group
   trait already reads several loci and produces one outcome; a polygenic trait
   is the additive special case of that. If 6.5 gets built, 6.3 may not need to
   exist as its own concept at all.

**Option 3 is worth checking before deciding option 1** - it may be that
"polygenic" is just a `TraitRule` with a sum in it, in which case there's
nothing to cut. Whichever way it goes, decide before the performance genes in
section 4.3 get written, since they are its only consumers.

### 6.4 Health: fewer hearts, and foals that don't make it

**Aging is out of scope.** Horses don't get older, don't grey out over time and
don't accumulate age-related conditions - simulating that risks feeling bad for
players who get attached to a horse. The option is deliberately left open (the
composer would need an age input, and grey/flea-bitten/melanoma would all
consume it), but nothing in this document should assume it. Two consequences,
both already noted above: **grey's fixed progression is a decision, not a
gap**, and **melanoma is parked**.

What health genes actually do in game:

- **Sub-lethal: fewer hearts.** A health gene lowers the horse's max-health
  attribute, so an affected horse visibly has fewer hearts than a normal one.
  This lands on an existing path - `HorseRecords.applyStatsToEntity` already
  sets the `MAX_HEALTH` base value from the record, and `HorseStats` already
  owns the number - so the genetic contribution just becomes another input to
  the health figure via `HorseTraits`. Cheapest possible mechanic, and it needs
  no new subsystem at all.
  - Watch the interaction with `applyStatsToEntity(..., fullHeal)`: a foal born
    with a low max health should start at *its* max, and a reload must not
    heal it. The existing `fullHeal` flag already draws that line correctly.
  - `HorseRecord.hasStats()` treats `0.0` as "not recorded yet", so a genetic
    health value must never resolve to exactly zero - clamp to a minimum
    (half a heart) and let the damage path do the killing.
- **Lethal: the foal dies shortly after birth.** The foal *is* born - it gets a
  record, a name and a place in the family tree - and then takes damage until
  it dies. Concretely: mark the foal at birth from its `ViabilityRule`, and
  have a tick handler apply a small recurring damage until it dies, over a few
  seconds rather than instantly, so the player sees what happened.
  - Needs: a data attachment for the condition, a tick handler, and a damage
    source that reads as "genetic defect" rather than an unexplained death.
  - Give the player feedback - a chat line naming the condition beats a foal
    that silently drops dead.
  - `HorseAncestryData` already tolerates ancestors it can't find, so a
    short-lived foal in the pedigree is fine.
- **Embryonic lethal (MET).** Checked at conception in `HorseBreedingHandler`:
  if the drawn foal genotype is inviable, no foal is produced. The foal
  genotype is drawn *before* the check, so `Genome.breedWith` stays untouched
  and the check just reads its result.
- **Config toggle** (`Docs/Philosophy.md` §8). A **server/common** setting, so
  it would be the mod's first `ServerConfig` - `ClientConfig` exists but is the
  wrong side. Three positions, not two: full genetics with deaths; genetics and
  reduced hearts but no deaths; off.
- **Carrier display.** `Gene.dominance()` metadata already exists and is read
  only by `GenotypeCatalog`; "carrier of X" wording in the info panel is
  already listed in CLAUDE.md as a wanted consumer, and becomes far more
  valuable once recessive lethals exist - it's the difference between a
  breeding programme and a lottery. A punnett / expected-foal display is the
  natural companion.

### 6.5 Traits that come from a *group* of genes

Wanted, and not the same thing as section 6.3's additive marker sets: **two or
more genes, each with their own alleles, that only together trigger an
outcome.** Two genes with two alleles each, where a particular combination
across both is what does something.

This is epistasis / complementation, and it needs a home:

- **It is not a gene**, so it can't live on the `Gene` interface. It wants a
  separate registered thing - call it a `TraitRule` - that declares which gene
  keys it reads and is handed those `AllelePair`s.
- **It is still Mendelian.** Each gene inherits normally and independently; the
  rule just *reads* the result. No new inheritance code, which is what keeps
  this compatible with 6.3.
- **It may affect the coat** - settled. That means a coat-affecting rule needs
  the same things a gene needs: a **phase** (natural or magical) and a
  **priority**, so the composer knows when to run it. Which argues strongly for
  modelling a `TraitRule` as a **pseudo-gene**: same delta function, same
  ordering, same determinism rules, differing only in that its inputs are
  several `AllelePair`s instead of one. Reusing the gene pipeline wholesale is
  much cheaper than a parallel one, and it means the spawner UI and the coat
  cache need no special case.
  - The one thing it can't inherit from `Gene`: it has no alleles of its own,
    so it contributes nothing to the genotype code and never appears in a
    `Genes` allele lookup. Keep it out of `codeOrder()`.
- **Determinism**: rules must be sorted like genes (`Docs/Philosophy.md` §2), and a rule
  reading a gene that isn't registered has to fail loudly rather than default.
- **Visibility to the player.** A trait with no single gene behind it is
  invisible in a genotype code and unguessable in a punnett square. It needs a
  UI surface of its own or it will read as randomness - which is exactly what
  the philosophy says not to do.

Cream + pearl is the existing gene pair that *looks* like this case but isn't:
they're one physical locus modelled as two genes, and section 5.2 merges them
rather than writing a rule.

### 6.6 Status effects - to be specified

The third kind of gene effect (section 3) is a list of "status-y" traits that
hasn't been written down yet. Sections 7.1-7.4 cover the ones already known
(stats, body size, health); **this subsection is a placeholder for the rest**,
and the capability interfaces in 7.1 should not be considered final until it
lands.

---

## 7. Horse care: milking and gated healing

Two features that apply to **every** horse, genes or no genes. Neither is
genetic, but both interact with the genetics: the health genes in section 6.4
lower a horse's max health, and healing is how it gets back to that max.

### 7.1 All adult mares can be milked

Right-click an adult mare with a bucket, get milk.

- **The sex is already there.** `HorseRecord.sex` is a `Sex` on every horse and
  is set at spawn, so this is a record lookup plus an age check, not new state.
- Goes in `HorseInteractionHandler`, which already handles the clock and the
  stick. **The trap it already documents**: `PlayerInteractEvent.EntityInteract`
  fires on **both sides**, and any item used on a tamed horse is otherwise
  turned into a mount by vanilla - so the event must be cancelled on the client
  too or the client predicts a mount and rubber-bands. Do the bucket swap
  server-side, cancel both sides.

**The rules, settled:**

- **The mare must be tamed.** An untamed mare is not a dairy animal.
- **The mare must be at full health.** This is the hook into 8.2 - a horse that
  isn't kept watered and fed doesn't heal, doesn't reach full health, and so
  can't be milked. It gives the care system a second reason to exist, and it
  means a genetically frail horse (fewer max hearts, section 6.4) is milkable
  as soon as it's topped up at *its* max, not the species max.
- **Roughly an hour's cooldown**, for balance. Needs per-horse state: a small
  data attachment holding the game time of the last milking.
  - **Pin down which hour.** A real-world hour is 72 000 ticks; a Minecraft
    hour is 1 000 ticks (and a full Minecraft day is only 20 real minutes, so
    an in-game hour is barely over a real minute - almost certainly not what
    "for balance" means). Store a tick count either way, and store the
    **game time**, not a countdown, so the cooldown survives a reload.
- **Milking a stallion gets you kicked**: a tiny amount of damage, about half a
  heart. Better than a silent no-op, and it makes the sex of a horse something
  the player learns by doing.
- **Foals do nothing** - but say something, or it reads as a bug.

**Dependency:** the full-health rule only bites once 7.2 exists, and it makes
hearts the shared readout of the health genes, the care system and milking -
so build 7.2 first.

### 7.2 Horses only heal near water **and** food

A horse regains health only when it is near **both**:

- a **water source** - a water block, flowing water, a water cauldron;
- a **food source** - crops, grass, a hay bale.

Notes on building it:

- **Vanilla horses have no passive regeneration** - they heal from being fed
  items. So this is *adding* a regen mechanic that is gated, not gating an
  existing one.
- **Hand-feeding is exempt, and always works.** Feeding a horse by hand feeds
  it and heals it, with **no water requirement** - the gate is on *passive*
  regeneration only. So the player always has a direct, reliable way to heal a
  horse, and the water+food rule is about horses looking after themselves in a
  pasture. That also keeps a stabled horse from becoming unhealable in a
  biome with no water.
- **Use block tags, not a hard-coded list.** `horsegenetics:horse_water` and
  `horsegenetics:horse_food` make the rule data-driven, let a pack add its own
  crops, and mean a modder's block can qualify. This is the same
  extensibility argument as the gene registry (section 3).
- Hook: `EntityTickEvent.Post`, which already carries the portal dwell timers
  and the water-riding handler.
- **Performance is the real design constraint.** A block scan per horse per
  tick is not viable with a pen full of horses - and the horse dimension
  spawns them by the hundred. Scan on a slow interval (every 20-40 ticks),
  stagger the phase by entity id so they don't all scan on the same tick,
  cache the last answer on the entity, and keep the radius small.
- **The dimension's pens already satisfy this by accident** - every pen has a
  full water cauldron and a hay bale sunk flush with the grass (they were put
  there to stop horses hopping the wall). Worth keeping that in mind as the
  free test bed, and worth *not* breaking when the pens change (section 8).
- **Interaction with the health genes**: a horse heals toward *its own* max
  health, so a genetically frail horse with 4 hearts stops at 4. The lethal
  foals in 7.4 must out-damage this regen, or a lethal foal parked next to a
  hay bale never dies.
- Player feedback: an unhealing horse is invisible until someone notices it
  isn't recovering. Particles when it does heal, or a line in the info panel
  naming what's missing, turn a mystery into a mechanic.

---

## 8. The horse dimension: back to random pens

**Revert the gallery.** The catalogue-driven dimension (one pen per entry in
`GenotypeCatalog`, built 2026-09-01 and not yet play-tested) goes back to
**random genotypes per pen**, which is what it did before. Keep the two things
that were worth keeping:

- **Pairs**: each pen holds two horses - a mare and a stallion - with the
  **same genome**.
- **A sign out front listing the genome**, as the catalogue version does.

This also **resolves the combinatorial problem in section 10** - a random
corridor doesn't care that the genotype space is 10^19, so the gallery stops
being the thing that blocks adding genes.

### What changes in the code

- `DebugPenManager.buildPen` goes back to rolling a genotype per pen instead of
  reading `GenotypeCatalog.get(index)`. Randomness here is legitimate: these
  are **foundation horses with no parents**, which is exactly where the
  determinism contract allows a draw (`Docs/Philosophy.md` §2).
- **The corridor stops having an end.** `LAST_SEGMENT_INDEX` (currently
  `ceil(size / 2) - 1` = 216, giving 1 519 blocks) and the `buildEndCap` call
  go away, or become an arbitrary length. `ensureGeneratedAheadOfPlayer` keeps
  extending as you walk, as it always did.
- **The entrance tally sign** (`Genotypes / 19,683 / Distinct / 434 pens`) no
  longer means anything - drop it, or replace it with something that's true of
  a random corridor.
- **The per-pen sign stays** - `GeneCodeDisplay.shortForm` + `wrap(genotype, 3,
  15)`, positioned to the right of the gate as you face the pen.
- **The sign-fits test changes shape.** It currently asserts every *catalogue
  entry* fits three 15-char lines. With random genotypes there is no finite
  list to check, so it becomes either a property test over many random draws or
  an assertion that `wrap` truncates gracefully. Don't just delete it - a sign
  that silently drops a gene is exactly the kind of bug it was written for.
- **`GenotypeCatalog` itself can stay.** Nothing forces its removal, and
  `Gene.dominance()` plus the distinct-pair reduction are useful to a
  punnett/expected-foal display (section 6.4) and to tests. It just stops
  driving the dimension. Its unit tests stay valid.
- **`tearDown` is unaffected** - it already discards entities and forgets their
  ancestry records, which matters more with random horses, not less.
- One thing to re-check: the note that "a rebuilt plot is byte-identical, so an
  X slot handed back to the free list can be overwritten in place" **is no
  longer true** once pens are random. Either air-fill on teardown, or confirm
  that overwriting a different random corridor in place leaves nothing stale
  (leftover signs from a longer previous corridor are the likely artifact).

### The pair shares a genotype, not an epigenome

**Settled**: the mare and stallion in a pen carry the **same genetic code** and
**different epigenetics** - which is what the catalogue version already does,
so `buildPen` needs no change here.

Both horses are independent founder rolls (`Docs/Philosophy.md` §2) with only
the genotype pinned, so a pen shows one genotype twice, differently.

---

## 9. The custom horse spawner (creative only)

A creative-mode screen for building a horse gene by gene and spawning it. It is
the tool the whole genetics system has been missing: today the only ways to see
a specific genotype are to breed for it, to walk the gallery, or to bake
samples outside the game.

### What it does

- **Every registered gene, every allele.** One row per gene, with both allele
  copies selectable from that gene's allele list. It has to be built off the
  registry (section 3), so a modded gene appears without the screen knowing
  about it.
- **Per-allele epigenetics.** Each allele copy gets its priority and seed
  shown, a **reroll** button, and a **type-it-in** field for an exact value.
  This is the first place epigenetics become directly visible to a player
  rather than an invisible modifier - which makes it a debugging tool as much
  as a creative one.
- **A live preview.** The horse redraws as the genotype changes.
- **Spawn.**

### Building it

- **Creative only, checked on the server.** The screen opens client-side, but
  the spawn arrives as a payload and the *server* must re-check that the sender
  is in creative (and permitted) before acting. A client-side-only check is
  bypassable, and this payload spawns arbitrary entities.
- **The preview already has a proven technique.** `FamilyTreeScreen.
  drawHorseModel` builds a render state from a throwaway client-only `Horse`
  and injects the coat straight onto `GeneticHorseRenderState`, deliberately
  without touching `ClientCoatCache`. Reuse it exactly - polluting the cache
  from a preview would leak edited horses into the world's rendering.
- **The preview needs a coat per edit.** Every change bakes a texture, and a
  player dragging through allele options will bake dozens. Debounce, and reuse
  the texture key so an already-baked combination is free - the cache key is a
  pure function of the genome (`Docs/Philosophy.md` §2), which is exactly what makes this
  cheap.
- **The screen must scale to an unknown gene count.** Nine genes fit on a
  screen; twenty-seven don't, and a modded install has no upper bound. Scrolling
  plus grouping (colour / pattern / performance / health, echoing section 10's
  info-panel note) from the start, not retrofitted.
- **26.1.2's GUI layer is retained-mode** - `extractRenderState(
  GuiGraphicsExtractor, ...)` rather than `render(...)`, mouse events as
  `MouseButtonEvent`. `FamilyTreeScreen` is the working reference for a
  from-scratch screen in this codebase, including `enableScissor` clipping and
  `g.entity(...)`.
- **How is it opened?** A creative-only item, a keybind, or a command. A
  keybind is closest to the existing `DebugKeyBindings` pattern, which is
  already gated on `!FMLEnvironment.isProduction()` - but this feature should be
  gated on *creative mode*, not on a dev build, so it needs its own gate.
- **Round-trip the code.** Paste a genotype code in, get the horse; copy the
  current horse's code out. Nearly free once the screen exists, and it turns
  every bug report into something reproducible - which is the determinism
  contract paying off.

---

## 10. What breaks at scale

The master table is ~27 loci against today's 9, several with many alleles, and
third-party genes (section 3) make the count open-ended. That is not a linear
change.

- **The genotype space.** Today: `3^9 = 19 683` genotypes. With the full table
  (KIT merged at 39 alleles, MITF 7, PAX3 5, MATP 5, ACAN 5, MC1R 3, dun 3,
  tiger eye 3, the rest biallelic) the raw product is on the order of
  **1.85 × 10^19**.
- **The gallery would have died - but it's being reverted anyway.** A
  catalogue-driven dimension (one pen per `GenotypeCatalog` entry: 434 pens,
  1 519 blocks) cannot survive a genotype space of 10^19, and the catalogue's
  reductions buy a constant factor, not an order of magnitude. **Section 8
  settles this** by going back to random pens, which don't care how large the
  space is. If a *systematic* gallery is ever wanted again, the shape that
  scales is a **wing per locus** - each gene's pairs on a fixed wild-type
  background, bounded by alleles rather than by their product, and it extends
  naturally to a modded gene adding its own wing.
- **The genotype code string.** One segment per gene, with tokens like `SW10` /
  `W36` / `D3`: 27+ segments puts a code in the low hundreds of characters, and
  a mod pack with several gene mods pushes it further. Everything carrying it
  grows in step - `HorseRecord.geneticCode`, the attachments,
  `CoatSyncPayload`, `HorseRecordSyncPayload`, and the epigenome code, which is
  already the fatter of the two (`<priority>:<seed hex>` **twice per gene**).
  Worth a compact binary encoding for the payloads at that point; keep the
  string form for saves and debugging.
- **`CoatTextureId` and `Identifier` path length.** The texture id is `coat/` +
  an injective encoding of the texture key, and the encoding *expands*
  (`A-Z` → 2 chars, anything else → 5). A 300-character code becomes a
  >1 000-character path, which `Identifier` will not accept. The fix is to key
  the texture on a **hash** of the code plus a short prefix, keeping the
  existing `KEY_BY_ID` collision tripwire - which caught the original
  flat-white-horse bug and would become load-bearing rather than paranoid, so
  give it a real collision-resolution path (append a counter) instead of
  throwing.
- **Wild-spawn frequency.** With a dozen recessive lethals in the pool, founder
  frequencies need a pass: rare enough that wild horses are healthy, common
  enough that a breeder eventually meets one. `Docs/Gene Dict.md` records wild
  frequency per gene and is the place for that table.
- **Display.** `GeneCodeDisplay.shortForm` / `wrap` exist precisely so a sign
  isn't a wall of wild-type slots, and a test asserts every catalogue entry
  fits a sign. At 27 loci the sign approach caps out; the info panel needs
  grouping (colour / pattern / performance / health) and probably scrolling.
- **Composer cost.** Phase 1 is O(genes × texels) at 16 384 texels per sheet.
  27 genes is still trivial; the pattern genes' `BodyNoise` sampling is the
  real cost. Coats are cached per texture key, so this only bites on first
  bake - but the gallery bakes hundreds at once.

---

## 11. Suggested order of work

Each step is useful on its own and unblocks the next.

1. **Gene priority + derived orderings** (section 2). Small, self-contained,
   and it deletes the three hand-maintained lists that make every later step
   annoying. Add the determinism tests `Docs/Philosophy.md` §2 calls for alongside it.
2. **Three-phase pipeline** (section 1). Port `TestGene`; add the
   byte-identical regression test. Nothing new is possible until colour is
   additive.
3. **The first magical genes** (section 4.5) - **pink mane and tail** first,
   because `CoatRegions` already has the mane and tail fills and it should be a
   few lines; then **zebra**, which proves the *negative* half of the unclamped
   signed model and builds the body-space stripe helper that dun and brindle
   both reuse. Cutie marks come much later - they need multi-allele support and
   an asset pipeline.
4. **The gene-authoring tiers** (section 3) - base classes first, the
   declarative builder second, the open registry third. Doing it here means
   every gene from step 5 onward is written the easy way instead of being
   rewritten later.
5. **Multi-allele support** (section 5.1) + **merge cream and pearl into
   MATP**. Smallest real multi-allele locus, with an existing resolver to fold
   in, and the weighted-frequency helper it needs is also what tier-1 genes
   need.
6. **Cheap coat genes, to bank wins**: silver dapple, mushroom, roan - and to
   prove the tiers, silver should be about six lines.
7. **Health, the cheap half** (section 6.4): genetic contribution to max health
   via `HorseTraits` -> `applyStatsToEntity`. No new subsystem, visible
   immediately as hearts, and it makes the health genes worth carrying.
8. **Trait framework proper** (section 6.1) with **MSTN** as its first
   consumer, then the size genes once the entity-scale question is answered.
9. **Revert the dimension to random pens** (section 8) - small, and it unblocks
   adding genes freely by taking the catalogue off the critical path. Could
   just as well go first; it's independent of everything above.
10. **Lethals** (section 6.4) - **MET** first (a conception check only, no death
    behaviour to design), then the born-and-dies path with its config toggle
    and its carrier display. Note the ordering dependency on section 7.2: a
    lethal foal has to out-damage the new regen.
11. **Sex as a gene, then sex-linked scaffolding, then brindle** (section 5.3).
    Registering `X`/`Y` at priority 1 is small and worth doing early - it makes
    sex inheritance fall out of ordinary breeding and removes the ordering
    problem X-linked genes would otherwise create. The scaffolding on top
    touches `breedWith` and `GenotypeCatalog`, so do it while the breeding
    tests are still small. Brindle is its proof.
12. **The pattern genes** - tobiano, overo, LP, sabino - in ascending order of
    difficulty, once the gallery can show them.
13. **Splash / SW series** and **KIT dominant white** last: they need
    dominance-per-pair, they are the biggest combinatorial hit, and the two
    existing splash bugs (dose, sock edges) should be fixed first.

**The custom horse spawner** (section 9) is worth pulling early - as soon as
the registry from step 4 exists. It is the fastest way to look at any genotype
without breeding for it, so every gene after it is quicker to build and check,
and it makes the phase-3 work in step 2 visible immediately.

**Independent of the whole chain**, and doable whenever: **milking** (section
8.1) and **gated healing** (section 7.2). Neither touches the genetics, though
8.2 should land before the lethal foals in step 10, and milking's full-health
rule depends on 8.2 existing to be interesting.

Parked, and only worth doing if the decision changes: **aging** (section 6.4)
and everything downstream of it - grey progression over time, flea-bitten grey,
melanoma. **Eye colour as a channel** is not parked but is independent of
everything above; it unblocks tiger eye and blue-eyed creams whenever it's
convenient.

---

## 12. Decisions still open

**Settled since the first draft** (kept here briefly so a later session doesn't
reopen them): phase 3 is a **signed add/subtract on unclamped `int` channels**,
capped only at conversion, applied over the natural result - zebra is a large
negative contribution, not a special case; a magical gene **can** paint a
dominant-white horse, because white is natural and magical genes run after; a
magical gene **does** see both the natural coat and the accumulated magical
deltas; **one gene cannot act in both phases**; `100` **is magical** and all
three-digit priorities are magical; the `0`-`99` band is **convention with a
log warning**, not enforcement; pen pairs share a **genotype only**;
hand-feeding **bypasses** the healing gate; milking needs a **tamed, full-health
mare** with a cooldown; allele tokens are **alphanumeric**, 1-128 chars, wild
type `n`; **group traits may affect the coat**, so they get a phase and a
priority like a gene (section 6.5); and inheritance is **Mendelian with exactly
one exception** - **X-linked genes are in** (section 5.3), crossing over,
maternal inheritance and imprinting are not. **Sex is itself a gene** (`X`/`Y`
at priority 1), and a hemizygous allele **displays** with an `X-`/`Y-` prefix
while the code string keeps the bare token.

Still open:

- **The exact phase-3 blend.** Straight signed add to start; revisit after
  baking samples (section 1). Anything fancier than an add reintroduces
  order-dependence.
- **Does the hemizygous slot hold a reserved `Y` allele?** (Section 5.3.) The
  display form is settled (`X-Brn`, with the code string unchanged), but the
  storage isn't. Using `Y` keeps `AllelePair` two-slotted and leaves the
  epigenome's alignment untouched, which removes the fiddliest part of the
  change - recommended, but it's your call.
- **Does the polygenic system survive?** (Section 6.3.) Leaning toward cutting
  it in favour of atomic genes; check first whether group traits (6.5) already
  subsume it. Decide before the performance genes get written.
- **How is a cutie mark chosen - allele per mark, or one gene plus
  epigenetics?** (Section 4.5.) Allele-per-mark is the better breeding game;
  epigenetics scales further. A hybrid (alleles for families, epigenetics
  within) is probably right. Also: is mark colour its own gene?
- **What "a 0-100% chance the gene appears" means for a multi-allele gene.**
  (Section 2.) One draw for "carries a variant" then a weighted pick, or
  weights per allele copy? It changes how often homozygotes appear.
- **How long is milking's "about an hour"?** (Section 7.1.) A real hour is
  72 000 ticks; a Minecraft hour is 1 000 and a whole Minecraft day is 20 real
  minutes.
- **What happens to a saved horse when a gene mod is added or removed?**
  (Section 3.) The no-back-compat rule was written for a solo dev changing his
  own format, not for a player installing a mod. Leaning toward padding
  missing segments with wild type on parse and dropping unknown ones with a
  warning - but it should be decided once and written into
  `Docs/breeding.md`.
- **How much health does the mod want?** Fewer hearts is uncontroversial; foals
  that die is the tonal question. Current answer: implement the genetics for
  everything and gate the deaths behind config.
- **Does aging ever land?** Deliberately deferred, deliberately not
  foreclosed. If it does, the composer needs an age input and grey gets a real
  progression curve; nothing else in this document changes.
- ~~**Linkage.**~~ **Settled: no.** No crossing over, no recombination, every
  gene independent (section 5.3). Merging KIT still groups its alleles onto one
  locus, which is a modelling choice, not linkage.
- **The 1.12.2 backport.** Everything in this document is `common/`-side except
  entity scale, gait animation, the health tick handler, and the gene
  registration hook. Those four are the only places the long-term backport goal
  should shape the design.
