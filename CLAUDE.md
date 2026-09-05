# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses: a Mendelian genotype of **allele objects** (extension,
agouti + seal, the white-pattern loci, champagne, a `T` "Test" diagnostic)
drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, rolled speed/health
stats, and an **epigenome** - a priority + epigenetic seed on *each allele
copy*, inherited with the allele - and there's a self-contained "horse
dimension" reached by a hay-bale portal. Long-term aim is a 1.12.2 backport, which is
why the logic is quarantined in a game-free module.

**Docs split:** the only two markdown files in the repo are `README.md` and
`CLAUDE.md`. **Everything else is the wiki** - `index.html` (the hub) +
`wiki/*.html`. The old `Docs/*.md` files were converted into wiki pages and
deleted; there is no markdown dev doc any more, and new dev docs go in `wiki/`
as HTML.

The wiki is now a **source of truth for the mod's behaviour**, not a side
project. Its shape:
- `wiki/styles.css` + `wiki/nav.js` are shared by every page. **The sidebar is
  built from the `SECTIONS` array in `nav.js`** - add a page there once and it
  appears on every page. No per-page nav markup.
- Each page is a standalone file: prism theme, then `styles.css`, then
  `<main class="content"><article class="doc">`. Copy an existing gene page as
  a template.
- **`wiki/gene-creator/`** is the interactive gene editor - now a *folder*, not a
  single page (`index.html` + `creator.css` + `js/*` + generated `assets/` and
  `fixtures/`). It is a **real tool, not a mockup**: it runs a JavaScript port of
  the coat pipeline, previews a gene on a 3D horse over any base coat, and
  exports the JSON the game loads. It does **not** use the wiki's `styles.css` /
  `nav.js` - it owns the whole window.
- **`wiki/gene-format.html`** is the single source of truth for the **data-driven
  gene file format** - the header, the knobs, every mask and every op, and the
  `effects` block's shape. When a mask, an op or an effect verb changes shape,
  update it in the same change.
- **`wiki/gene-effects.html`** is the single source of truth for the **`effects`
  block** - every verb and its parameters, the triggers, the condition flags,
  the execution / merge model, and the **modular contract for adding a new
  effect** (one `AbilityType` declaration; the parser never changes). Update it
  in the same change as any effect verb, flag, or trigger.
- **`wiki/horse-traits.html`** is the single source of truth for the wider
  **trait / effect architecture** - the mostly-unbuilt behaviour system
  (selectors, auras, pools, goals) that `effects` is a first slice of, plus the
  Waterborn worked example and the slice-vs-architecture map. It points at
  `gene-effects.html` for the built reference.
- **`README.md`** is **user-facing only** now - what the mod does, how to play
  it, install, license. No status tables, no architecture, no API notes.
  Don't put dev content there.
- **`CLAUDE.md`** (this file) is the dev/working notes: status, the 26.1.2 API
  differences, gotchas, next steps.
- **`wiki/breeding.html`** is the single source of truth for the breeding / horse-
  record / pedigree / **stat-inheritance** system. Keep it current when you
  touch any of that; don't re-document it here or in README (a pointer is
  fine).
- **`wiki/gene-*.html`** is the single source of truth for **each gene** - alleles,
  generation function, wild frequency, dominance, natural/magical. Update
  it in the same change as any gene; CLAUDE.md keeps only the machinery + a
  one-line-per-gene table.
- **`wiki/horse-body.html`** is the single source of truth for the **trait /
  size / health system** - `HorseTraits`, the baselines, `TraitContribution` /
  `HealthContribution`, `Condition` / `Severity` / `Viability`, the two lethal
  paths, `ServerConfig`, and the four attributes it writes. Update it in the
  same change as anything under `common/trait/`, `LethalFoalHandler` or
  `HorseRecords.applyTraitsToEntity`. **CLAUDE.md keeps a summary, not a copy.**
- **`wiki/horse-care.html`** is the single source of truth for the **non-genetic
  horse-care systems** - gated healing, bond tiers, herd formation, the shared
  slow tick and the block tags. Update it in the same change as
  `HorseCareHandler` / `BondFollowGoal` / `HorseCareAttachment`.
- **`wiki/verification.html`** is the rolling **`runClient` checklist** - what's
  built but not yet confirmed in-game. Update after every play session.
- **`wiki/philosophy.html`** is the **why** - Mendelian breeding as a game of
  skill, procgen coats for functionally infinite outcomes, "interesting" being
  the player's word, and the deliberately-acknowledged abstractions (no
  crossing over, no aging; **X-linked genes are the one exception**). It also
  owns the **determinism contract** - the same code always makes the same
  horse, what that forbids, and the founder-only randomness rule - because
  that's a standing rule, not a task. It ends with a priority order for when
  two goals conflict. **Read it before making a design call**; most arguments
  in the other docs are downstream of it. Keep it short and principled - no
  implementation detail, no status, no task lists.
- **`wiki/roadmap.html`** is the **long-range backlog** - the gene wishlist and
  the systems it needs (the modder-facing gene-authoring API, sex-*linked*
  inheritance, the leopard complex, face markings as a family, gene metadata),
  each with notes on what would have to change - plus the whole unbuilt gameplay
  layer (§§11-19) and mare milking. **Only not-yet-done work lives here**: a
  section is either unbuilt or marked *partly built* with just the remainder;
  anything finished is deleted and written up on its own page (`wiki/gene-*.html`,
  `wiki/genetics-model.html`, `wiki/horse-body.html`, `wiki/breeding.html`,
  `wiki/horse-care.html`, `wiki/pipeline.html`). Section
  numbers are stable (a retired section keeps its slot as a pointer) so
  cross-references don't rot. It is **work items only**; the reasoning lives in
  `wiki/philosophy.html`. Its §21 keeps a **settled** list so a later session
  doesn't reopen those calls - the load-bearing ones: **Mendelian + X-linked +
  Y-linked only** (polygenic cut; a stallion carries a real `X` *and* `Y`);
  **aging out entirely** (flea-bitten grey / melanoma cut with it); health =
  fewer hearts + lethal foals behind a default-on config; one gene per physical
  locus (KIT/MITF/MATP) with the gene's own `tint` handling every allele combo,
  **no dominance-per-pair table**; founder frequency declared **per genotype**
  (percentages, auto-normalised) not per allele, and a separate per-gene
  **chaos-carrot chance function** of the same shape (§14.1/§19); the genotype
  stored as a **list keyed by gene**, each gene carrying its order number, so
  adding/removing a gene just triggers a coat regen (no padding, no back-compat);
  signed-unclamped `int` phase-3 with context-aware genes intended; **every time-gate is once per
  24 000-tick MC day**; herd comfort buff stays passive regen (no stamina); the
  villager **exists**; the gene DB is a plain progressive record (no fog of war,
  nothing hidden); a magic carrot's timing = a vanilla golden carrot (temporary
  window, coloured heart particles per type); mutinogenic re-rolls every allele
  copy a fed parent passes on; the seed jar stores enough sire data for the
  family tree; sheared look = render-layer overlay; magic-carrot rarity defaults
  to the gold-ingot tier and a gene can opt out. **Still open**: custom-entity
  subclass vs attachments (§11, owner wants to discuss), how a cutie mark is
  chosen, the 1.12.2 backport surface.
- **New with the wiki conversion**, and not derived from any old markdown -
  keep them current too:
  - **`wiki/genetics-model.html`** - the Mendelian model as implemented:
    alleles as objects, the code string, the **combination table**
    (`Expression`) and `FounderTable` that replaced dominance, the per-allele
    epigenome, `GenotypeCatalog`, and what the texture key captures.
  - **`wiki/pipeline.html`** - the three-phase coat pipeline in full
    (`PigmentField` / `ColorField` / `GradientLut` / `CoatTextureId` / the
    golden test). This is where the coat machinery is documented now; CLAUDE.md
    keeps a summary.
  - **`wiki/body-space.html`** - `HorseSkinGeometry`, `CoatRegions`,
    `BodyNoise`, `BodyStripes`, `PatchNoise`.
  - **`wiki/modding.html`** + **`wiki/api-reference.html`** - the
    **modder-facing** docs: how to write a gene (two worked walkthroughs, the
    allele rules, the determinism contract, the pitfalls table) and the
    class-by-class abstraction reference. When a public type in `common/`
    changes shape, update `api-reference.html` in the same change.

## Status snapshot (keep this current)

- **Built 2026-09-05, NOT yet play-tested: magic speed, magic health and magic
  jump - three more genes on the `MagicSizeGene` pattern.** `MagicSpeedGene`
  (`horsegenetics.magic_speed`, priority 141, `Swift`/`Sluggish`/`n`),
  `MagicHealthGene` (`horsegenetics.magic_health`, 142, `Hardy`/`Frail`/`n`),
  `MagicJumpGene` (`horsegenetics.magic_jump`, 143, `Springy`/`Leaden`/`n`).
  **42 built-in genes, 44 in-game.** Shape of it:
  - **One shared base, `AbstractMagicStatGene`** (the ManeColor/TailColor ->
    HairColor precedent). It is the magic body size design generalised to the
    three *additive* stats: codominant, three alleles, `MEAN_DELTA` 0.10 /
    `SIGMA_DELTA` 0.07 / `MIN_DELTA` 0.01 per copy, one `nextGaussian()` off
    **each copy's own** epigenetic seed (`AlleleRandomness.copy(slot)`), both
    copies added (`up` positive, `down` negative), six combinations = **six
    outcomes**, all `wildType`. Founder table written out **40% up/n, 40%
    down/n, 20% n/n** - heterozygotes only, ~80% carrier rate, so most wild
    horses now carry all four body-stat genes. `MagicSizeGene` stays its own
    class - it multiplies *scale*, which has the two-stage natural+magical
    clamp; the other three have no natural clamp.
  - **New on `TraitBuilder`: `multiplySpeedUnclamped` / `multiplyHealthUnclamped`
    / `multiplyJumpUnclamped`**, the exact counterparts of
    `multiplyScaleUnclamped` - applied after every addition, clamped only to
    new `HorseTraits.MAGICAL_MIN_FACTOR` / `MAGICAL_MAX_FACTOR` (**0.1 - 10**,
    the "10x either way" cap). The bounded Gaussian keeps the real reach near
    **2.04x** at two maximal copies, so the guard never fires - same story as
    size. `MIN_HEALTH` (0.5 heart) is still applied last, so `Frail/Frail` +
    a disorder can't zero a horse.
  - **It multiplies the natural loci, it doesn't add.** A magic-speed horse
    that is also `mstn=C/C` is faster than a plain magic-speed horse by the
    same *ratio* - `MagicBodyStatGenesTest.theMagicScalesTheNaturalStat` pins
    it. A magically fast pony is still slower than a magically fast racehorse.
  - **Magic health is deliberately NOT a `HealthContribution`.** That marker is
    what `health.mode = OFF` suppresses, and it is for *disorders*; turning
    those off must not strip a horse's magical vigour. `Frail/Frail` is a horse
    with fewer hearts, not a sick one - no `Condition`, never suppressed. (Same
    call milk's `Watr/Lava` lethal made.)
  - **Paints nothing** - all outcomes `wildType`, so `affectsCoat()` false, out
    of the texture key, `GenotypeCatalog` collapses each to one entry:
    `size()` **unchanged at 462 422 019**. `totalGenotypes()` went to
    **17 644 404 871 265 791 068 979 200 000** (x216, three loci x 6 carryable
    pairs).
  - New `MagicBodyStatGenesTest` (16 tests). `:common:test` **348 green**,
    `:neoforge-26.1.2:build` green. `SpecGeneTest.BUILT_IN_GENES` 39 -> 42.
    Two brittle statistical tests were widened where the seed-stream reshuffle
    tipped them: `ShowcaseGenotypesTest` magical-share upper bound 0.60 -> 0.66
    (it is a floor, not a rate), and `GenotypeCatalogTest`'s raw-product
    comparison moved to `BigInteger` (the `long` had already wrapped - gap #13).
  - **`coat-golden.txt` regenerated, but no coat moved** - all 450 rows are
    byte-identical bar the three new `=n/n` code segments per line. Per-gene
    epigenetic seeds are keyed by gene, not by stream position, so inserting
    genes at priority 141-143 doesn't shift particle / light / verdant / test.
  - **Old saves will not parse.** Genotype code went 41 -> 44 segments. Dev
    only; start a fresh world.
  - **Deliberately not built:** per-part scaling, a coat marking (they are the
    magic body size family and it has none either), per-allele founder
    frequencies, environmental noise on the natural stats (still zero - gap
    #24).
  - Docs: `wiki/gene-magic-speed.html` / `-health` / `-jump` (new),
    `wiki/nav.js` (new "Magical body-stat genes" section, `gene-body-size.html`
    moved into it), `wiki/genetics-model.html` (table + counts + raw total),
    `wiki/horse-body.html`, `wiki/api-reference.html`, `wiki/modding.html`,
    `wiki/roadmap.html`, `index.html`, `README.md`. Checklist:
    `wiki/verification.html` §0d.

- **Built 2026-09-05, NOT yet play-tested: `PAX3` `SW2` is the ordinary horse -
  90% of founders carry one copy.** Owner's call, and it is how minimal splash
  works in life: a mild splash allele is near-ubiquitous, and what one copy buys
  a horse is roughly what most horses look like. `Pax3Gene`'s founder table went
  from `hardyWeinberg(SW2 2%, SW4 0.5%)` to a written-out
  **90% `SW2/N` / 1% `SW4/N` / 9% `N/N`**.
  - **It had to be written out, for two independent reasons.** Hardy-Weinberg's
    heterozygote share is `2pq`, which **peaks at 50%** - there is no allele
    frequency anywhere that makes 90% of a randomly-mating population
    heterozygous, so the table has to say it directly. And left to HWE, one copy
    on nine horses in ten would put **81% of wild horses at `SW2/SW2`**, the bold
    outcome - not a pattern any more, the base coat. Heterozygotes only, baseline
    last, the `MagicSizeGene` rule.
  - **Measured effect on the wild population** (20 000 founder draws, 400 baked
    coats): horses showing any white-pattern locus **33.3% -> 93.6%**; mean white
    coverage of a wild-caught horse **32.5% -> 49.5%**; a foal of two wild-caught
    parents is `splash-bold` **0.07% -> 20.5%**. Bold splash is now the commonest
    thing a player breeds by accident, and a horse that is *also* `MITF` splash is
    the usual case rather than a rarity - which is the interaction the two-locus
    split exists to show.
  - **It makes the splash calibration bug load-bearing.** The audit that opened
    this session found that `WhitePattern.splash` measures its waterline against
    the **whole-horse** height (hoof to ear tip, span 33.75) while the barrel top
    is at 0.622 of that and the legs only reach 0.326. So `S_SPLASH = 0.34` puts
    the line at frac 0.325-0.386 - i.e. **all four legs entirely white** - and
    that is now what 90% of wild horses look like. "Minimal marking" currently
    means four white legs. Fixing the mapping (`s = 1` should land at the crest,
    not the ear tip) is no longer optional tuning; see the known-gaps entry.
  - `:common:test` **333 green**. `coat-golden.txt` **untouched** - the golden
    cases are explicit codes, not founder draws, so nothing about any individual
    coat moved. No format change; old saves parse.
  - **Deliberately not done:** the same treatment for the particle locus. It was
    asked for on the premise that a heterozygote is silent, and it is not -
    `ParticleGene.expressionOf` returns `singles[a]` when the second copy is `n`,
    so **one variant copy visibly trails its particle**. At 90% het, 90% of wild
    horses would trail something (today: 7.7%). Owner chose to skip it rather
    than take that tonal shift.

- **Built 2026-09-05, NOT yet play-tested: face markings become a family - star,
  stripe and snip.** All four white loci now draw the head from one shared
  vocabulary, `WhitePattern.faceMarking(epi, skin, strength, jag)` +
  `WhitePattern.FaceMarking`, which closes the standing "every locus draws the
  same centreline stripe" gap. Machinery is `wiki/pipeline.html#face-markings`;
  the shape of it:
  - **Three components, not eight named shapes.** Horsemen name eight or nine
    markings, but they are **three independent components** - a patch on the
    forehead, a band down the nose, a patch at the nostrils - plus one width, and
    every named marking is a combination of them. `describe()` reads the term back
    off the components ("star and snip", "blaze to the nostrils", "bald face");
    nothing ever chose it, which is the check that three booleans and a width
    really do span the vocabulary. `FaceMarkingTest` asserts every name is
    reachable.
  - **Star and snip are the point** - both are *detached* patches, white with
    coloured face on every side, which the old painter structurally could not
    draw: it was one centreline band starting at the nose and running back, so its
    whole vocabulary was stripe / blaze / bald face. Measured: a star-only marking
    is 12 texels on `HEAD.TOP` and **nothing on the muzzle**; a snip-only is 7
    texels on `MUZZLE.TOP` and **nothing on the head**.
  - **Face space is `t` from poll (0) to nose tip (1)**, measured along body-space
    `x` over head *and* muzzle together, so the same numbers mean the same anatomy
    on the adult (separate `MUZZLE` box) and the foal (no muzzle box at all). The
    eyes sit near `t = 0.4` on both meshes, which is what anchors the star at 0.30
    and the snip at 0.90.
  - **Strength picks the distribution, not the marking.** A locus does not decide
    a horse has a snip; it decides how much white the horse tends toward. At
    `S_MINIMAL` 0.12 that is star 37% / nothing 29% / snip 9% / star+snip 12%; at
    sabino 0.42 a bare face is essentially gone; at 0.62 it is a blaze; at 0.93 a
    bald face three times in five. **This is what finally makes `W20/N` mean its
    own description** - "a star and a sock" - which was prose nothing implemented.
  - **`jag` carries the sabino/splash difference onto the face** - 0.42 for `KIT`,
    0.11 for the two splash loci, 0.34 for frame. Same reason their body margins
    differ, one parameter rather than two painters.
  - **A blaze no longer wraps under the jaw.** `Face.BOTTOM` on the head and
    muzzle only whitens at bald-face width. The old painter tested the centreline
    on *every* plane of the box, so the underside of the jaw went white on every
    blaze from every locus - a real bug, found while wiring this up.
  - **The draw is fixed and unconditional: one long and eight floats, every
    time**, including for components that turn out absent. The particle locus's
    lesson, applied: a draw made only when a flag is set silently repaints every
    horse in every save the first time that flag's odds move. `FaceMarkingTest`
    runs an empty marking and a bald face through a nine-value `FakeRng` and
    asserts both exhaust it.
  - **`EdnrbGene` stopped hand-rolling its own face.** It was a bare
    `|z| <= faceHalf` over the whole head with no top and no shape; it is now
    `FACE_STRENGTH = 0.80` / `FACE_JAG = 0.34` on the shared vocabulary, which is
    the bald-faced pattern frame is supposed to be. `FACE_HALF_MIN`/`_RANGE` and
    `WhitePattern.withinFace` are gone.
  - New `FaceMarkingTest` (12 tests). `:common:test` **333 green**,
    `:neoforge-26.1.2:build` green, creator parity **3 832 checks / 48 cases**
    (untouched - the creator only ports `KIT`'s dominant-white outcome, not
    `WhitePattern`). `coat-golden.txt` regenerated: **330 of 450 rows
    byte-identical**, and the 120 that moved are exactly the rows carrying a
    `KIT` / `MITF` / `PAX3` / `EDNRB` variant. No format change, so old saves
    still parse - those horses just repaint.
  - **Deliberately not built:** giving tobiano a face marking (real tobianos
    commonly carry a star or blaze, but the gene's documented behaviour is a
    coloured head and that is a separate call); **medicine hat / war shield**,
    which is not a face marking at all but a retention rule on a near-white horse
    - logged in `wiki/roadmap.html` §4.2, and see gap #29 below; surfacing
    `describe()` anywhere player-facing (it is a natural feed for the gene
    dictionary and the info panel - gap #10).
  - Docs: `wiki/pipeline.html` (new "Face markings" section),
    `wiki/api-reference.html`, `wiki/gene-kit.html`, `wiki/gene-mitf.html`,
    `wiki/gene-pax3.html`, `wiki/gene-ednrb.html`, `wiki/roadmap.html` §4.2.
    Checklist: `wiki/verification.html`.

- **Built 2026-09-04, NOT yet play-tested: the particle locus - forty alleles on
  one gene.** `ParticleGene` (`horsegenetics.particle`, priority 150), the largest
  gene in the mod by a wide margin: **40 variant alleles + `n`, 861 combinations,
  87 outcomes**. **39 built-in genes, 41 in-game.** A horse trails a particle as it
  moves - flames, souls, snow, hearts, portal motes. Machinery is
  `wiki/gene-particle.html`; the shape of it:
  - **One locus, not forty genes, and that is the whole design.** Forty independent
    two-allele genes would let a horse carry all forty, so "*which* of these does
    this horse trail" stops having an answer and every serious line converges on a
    horse emitting everything. One locus says the opposite **structurally**: a horse
    has two copies of the chromosome, so it shows **at most two, ever**, and a third
    is not rare but impossible. The `KitGene` argument at forty times the scale.
  - **Rank, and the copy a horse is hiding.** Every allele carries a rank and the
    alleles are **declared in rank order** - load-bearing rather than tidy, because
    `AllelePair` canonicalizes on `Allele.order()`, so declaring them this way is
    what puts the shown copy in slot 0 and the hidden one in slot 1. Two
    non-codominant alleles meet and the lower rank wins; the loser is carried
    silently and passed on intact, indistinguishable from a wild type. That is what
    makes a locus this wide **breedable** rather than merely large.
  - **Codominance is by family** - one `group` string per allele, and two *different*
    alleles of one group both show at once. Nine families cover 29 of the 40 alleles
    and produce the **46 double outcomes**; the other 11 never stack. **The flames
    and the smokes are one family of eight, not two of four** (any `-flm` stacks with
    any `-smk`), which is 28 of the 46 by itself - a unit test pins it, because
    getting that grouping wrong would quietly delete most of the locus. Two tokens
    that look related and are not: `Dstrn` (enchanting glyphs) is no `Dst`, `Lmstr`
    (totem sparks) is no `-str`.
  - **Everything visible about it is epigenetic.** The allele names a particle and
    nothing else; **colour, second colour, body site, count and one spare `data`
    number** are drawn per **allele copy**, in that fixed order, every time -
    including for particles that use none of them, because *the draw order is the
    contract* and a conditional draw silently rewrites every horse in every save.
    So two `Rflm/n` horses are not the same horse, and each half of a codominant
    pair is independent: red flames off the front hooves and blue smoke off the tail
    is one horse nobody designed. A foal that inherits the copy inherits the exact
    number.
  - **New machinery: `common/genetics/EpigeneticAbilityContribution`** - the exact
    twin of `EpigeneticTraitContribution`, one layer along. `AbilityContribution` is
    a pure function of the genotype, which is right wherever the alleles fix the
    behaviour (two `Hlr/Hlr` horses heal identically, and should); this locus is the
    case it cannot express. **`AlleleRandomness` moved from `common/trait/` to
    `common/genetics/`** so both sides can use it without the two packages depending
    on each other, and gained the `forGene(gene, genotype, epigenome)` factory
    `HorseTraits` used to keep private. `HorseAbilities.activeFor` gained an
    `Epigenome` overload; **the translator's per-horse cache is now keyed on both
    code strings**, or it would hand one horse another horse's colours.
  - **The `emitter` verb grew**, and all of it is usable from a gene file today -
    which is the property the shared vocabulary was chosen for. New: `color2`,
    `count` (1-16), `data` (a normalised `[0,1)` standing in for a shriek's delay or
    a sculk charge's roll), and **five body sites** beside the four single-point
    anchors - `spine`, `hooves`, `front_hooves`, `back_hooves`, `tail`. A
    multi-point site is re-picked **per particle**, so a firing of four off `hooves`
    really does come off different hooves. Positions come from the live bounding box
    and yaw, which already has `Attributes.SCALE` applied, so a magically enormous
    horse trails from its own hooves.
  - **`particleFor` is a registry lookup now, not a name table.** Only the ~8
    parameterised particles are written out; everything else is
    `BuiltInRegistries.PARTICLE_TYPE` + `instanceof SimpleParticleType`. Forty case
    labels would have been forty chances to mistype a field name that does not match
    its own id (`TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS` is registered as
    `trial_spawner_detection_ominous`), and it would go stale the next time the game
    adds a particle. **The colour-carrying options want ARGB and read the alpha**
    (`ColorParticleOption.getAlpha`), so a bare `0xRRGGBB` is fully transparent -
    `DustParticleOptions` is the exception and takes plain RGB, which is why the two
    are written differently.
  - **The source list was Bedrock particle names.** Ten have no Java equivalent and
    carry a substitute (`magnesium_salts` -> `white_ash`, `small_soul_fire_flame` ->
    `copper_fire_flame`, `sculk_sensor_redstone` -> `vibration`, `oozing_emitter` ->
    `item_slime`, `warden_dig` -> `block`(sculk), `crop_growth_emitter` ->
    `happy_villager`, `evoker_spell` -> `instant_effect`, plus four straight
    renames). **Two were cut rather than substituted** - `Bigflm` and `Cndlflm` both
    land on a particle another allele already has, and Java flames are
    `SimpleParticleType`s carrying neither colour nor scale, so the pairs would have
    been indistinguishable. Forty alleles, not forty-two.
  - **It paints nothing.** All 87 outcomes are wild types, so `affectsCoat()` is
    false, the locus is out of the texture key, and `GenotypeCatalog` collapses 861
    combinations to **one** entry - `size()` is **unchanged at 462 422 019**. Forty
    alleles for no catalogue growth at all.
  - **`GenotypeCatalog.totalGenotypes()` is a `BigInteger` now, and the old number
    was already wrong.** The particle locus pushed the product past 2^63 - and
    checking that revealed the `long` had **already wrapped** before this session:
    the documented 3 028 898 126 035 238 912 was nonsense, and the true
    pre-particle figure was **94 874 633 669 214 259 200 000**. It is now
    **81 687 059 589 193 477 171 200 000**. `size()` still saturates at
    `Integer.MAX_VALUE`, which is right for an index bound callers loop over; this
    is a statistic nothing indexes, so the honest answer costs nothing. No
    production caller - the entrance sign that used it is long gone.
  - **Founder frequency is 0.1% per variant, the same for all forty**, because no
    particle is the ordinary one. Forty of them puts the variant share at 4%, so
    about **7.7%** of wild horses trail something while any *named* particle is
    roughly 1 in 500 - and a codominant double is about **1 in 10 000**, i.e. you
    cannot catch one. Same rule as the health loci, pointed at something worth
    having.
  - New `ParticleGeneTest` (21 tests). `:common:test` **321 green**,
    `:neoforge-26.1.2:build` green, creator parity **3 832 checks / 48 cases**,
    `runServer` boots clean (`41 segments`, `loaded 2 data-driven gene(s)`, zero
    errors). `coat-golden.txt` regenerated - every row moved, because the gene set
    moved and every derived epigenetic seed moved with its position.
  - **Old saves will not parse.** The genotype code went 40 -> 41 segments. Dev
    only; start a fresh world.
  - **Deliberately not built:** any coat marking for the locus (it does not need one
    - the gene *is* the visible thing, which is the answer to the complaint milk and
    verdant attract); per-allele founder frequencies (no particle is more ordinary
    than another); an epigenetic *emission rate* (density is already the `count`
    draw, and a second density knob would fight it).
  - Docs: `wiki/gene-particle.html` (new), `wiki/nav.js`, `wiki/gene-effects.html`
    (the emitter's new fields, the anchor table, which particles read what),
    `wiki/genetics-model.html`, `wiki/api-reference.html`, `wiki/modding.html`,
    `index.html`, `README.md`. Checklist: `wiki/verification.html` §0c.

- **Built 2026-09-04, NOT yet play-tested: seven magical utility genes, designed
  as a set.** The point of them is *combination* - broad epigenetic ranges and
  independent loci, so that a ten-times healer with a striped mane that spreads
  moss is a horse nobody wrote a line of code for. **38 built-in genes, 40
  in-game** at the time (39 / 41 now, with the particle locus). Machinery is
  `wiki/gene-milk.html` and its six siblings; the shape of it:
  - **`MilkGene`** (`horsegenetics.milk`, priority 130, `Watr`/`Lava`/`n`) - a
    bucket gets milk from a grown mare, **water** from any `Watr/Watr` horse,
    **lava** from any `Lava/Lava` one. Both variants are recessive to the wild
    type *and to each other*, so it is the cleanest double-carrier locus in the
    mod: you cannot catch a lava horse, only breed one. **`Watr/Lava` is an
    embryonic lethal** - the same path as `MET` (`canOccur` false, the breeding
    handler reads the drawn genotype and cancels), but deliberately **not** a
    `HealthContribution`: `health.mode` governs *disorders*, and turning them
    off must not quietly make a water/lava horse possible.
  - **`MagicSizeGene`** (`horsegenetics.body_size`, 140, `Big`/`Small`/`n`) -
    **codominant, and revised 2026-09-04 after the first draft**. Every allele
    copy carries a percentage and **both copies contribute**: the size is one
    plus their sum, `Big` positive and `Small` negative. Six combinations, **six
    outcomes** - the one locus in the mod where every combination genuinely
    differs, which is what codominance is. `Big/Small` lands *near* 1.0 rather
    than exactly on it, because the two percentages are independent draws;
    forcing an exact zero would need a special case contradicting "the
    percentages add", and the residual usefully says "this horse carries both
    extremes".
    - **The distribution is normal**, mean **10%** and sigma **7%** per copy,
      floored at 1% so a `Big` allele can never come out making a horse smaller.
      That shape is load-bearing: **80% of wild horses carry a copy**, so if one
      copy were dramatic then dramatic would be the baseline and nothing would
      read as unusual. Instead the population has a quiet continuous spread you
      notice across a paddock rather than on any one horse.
    - **Only heterozygotes are born wild** - the founder table is written out
      (`Big/n` 40%, `Small/n` 40%, `n/n` 20%) rather than derived from
      Hardy-Weinberg, because random mating is exactly what it is not. **Every
      doubled horse in the world is one somebody bred**: the health loci's rule,
      pointed at something worth having.
    - It is the mod's first **`EpigeneticTraitContribution`**, and the first
      caller of the new **`AlleleRandomness.copy(slot)`** - a codominant gene
      cannot ask for "the copy that expresses", which would count one allele
      twice and the other not at all.
    - **`Rng.nextGaussian()`** is a new default method: Irwin-Hall (twelve
      uniforms minus six) rather than Box-Muller, for two reasons that both
      matter here. Its tails are **bounded at +/-6 sigma**, so a trait deciding how
      big a horse is has a hard bound on absurdity and the guard clamps never
      fire; and **all-0.5 inputs give exactly 0**, so `MidpointRng` lands on the
      mean (Box-Muller would hand it 1.18 sigma below it).
    - The practical ceiling is now about **2.04x** (two copies at +6 sigma), not the
      first draft's 10x - and it takes *two* good copies to pass even the natural
      `MAX_SCALE`, which is the point of making it codominant. The magical
      bounds stay as guards rather than as the design.
  - **`ManeColorGene`** / **`TailColorGene`** (112 / 114, `Mnsld`/`Mnstrp`/`n` and
    `Tlsld`/`Tlstrp`/`n`, sharing `HairColorGene`) - solid or banded hair in any
    colour, and the **heterozygote is both at once in two different colours**.
    That is the first thing in the mod that needs *both* allele copies' seeds
    rather than the expressing one, hence the new
    **`CoatBuildContext.epigeneticsForCopy(key, slot)`**; asking for the expressed
    copy would paint the stripes in the base colour. Two loci and not one, so a
    red mane and a blue tail is breedable.
  - **`LightGene`** (160, `Lthf`/`Ltmn`/`Lteye`/`n`) - gold hooves / mane / eyes,
    each glowing, plus torch-strength world light. **Ten combinations, seven
    outcomes, genuinely codominant**: three alleles each dominant to the wild type
    and to *none of each other*, which no dominance ranking can express and the
    combination table says in one row each. Any variant copy lights the horse; the
    alleles decide only *where* it shows.
  - **`HealerGene`** (116, `Hlr`/`n`, recessive) - players within 3 blocks mend,
    and a red stripe runs down the centre of the mane so a player can *see* it.
    The stripe's opacity is the gene's one epigenetic value; **it says nothing
    about the healing**, deliberately - a mark that encoded a stat would make a
    horse's value legible from a screenshot.
  - **`VerdantGene`** (180, `mush`/`moss`/`grass`/`n`) - spreads mycelium, moss or
    grass from the hooves. **Every variant needs two of itself**; `mush/moss` is
    not half of each, it is nothing. A different shape from milk's mutually
    recessive pair, whose clash is *lethal* where this one is merely inert.
  - **The three that paint nothing** (milk, size, verdant) reuse the non-coat
    trick: every outcome is a `wildType`, so `affectsCoat()` is false, they are
    out of the texture key, and the gallery collapses each to one entry. **Seven
    genes for four genes' worth of catalogue growth.** `GenotypeCatalog.size()` is
    now **462 422 019** and `totalGenotypes()` was
    **94 874 633 669 214 259 200 000** at this point. (The figure originally
    recorded here, 3 028 898 126 035 238 912, was a wrapped `long` - the overflow
    was found and fixed when the particle locus landed. See that entry.)
  - **New machinery, four pieces, all in `common/`:**
    - **`trait/EpigeneticTraitContribution`** + `HorseTraits.resolve(genotype,
      epigenome, healthGenetics)` - a trait whose *magnitude* is on the allele
      copy. Determinism is untouched: the `Rng` is a `SeededRng` on the expressing
      copy's stored, heritable seed, exactly as the coat's is. `resolve(genotype)`
      with no epigenome now answers with the **midpoint** (`MidpointRng`, new),
      which is the honest answer to a question about a genotype rather than a horse.
    - **`TraitBuilder.multiplyScaleUnclamped`** - applied *after* the natural
      `MIN_SCALE`/`MAX_SCALE` clamp and bounded only by the new
      `MAGICAL_MIN_SCALE`/`MAGICAL_MAX_SCALE` (0.1-10). The exact counterpart of
      the coat's uncapped phase-3 accumulator, and it composes right: a magically
      enormous pony is still smaller than a magically enormous draught horse.
    - **`coat/pattern/CoatOverlay` + `CoatOverlayContribution`** - a **fourth coat
      phase**, after the composite *and after `redrawEyes`*. It exists for exactly
      two things the earlier phases structurally cannot do: colouring the **eyes**
      (phase 5 restores them from the template, so a gene wanting them has to run
      later) and carrying an **emissive texel mask** ("this glows" is not a
      colour, so neither accumulator has a channel for it).
      `CoatTextureComposer.bake` returns `Baked(argb, emissive)`; `compose` is the
      pixels alone, so the golden test and every existing caller were untouched.
    - **`genetics/AbilityContribution`** - a built-in gene can now grant game
      behaviour, using the **same `GeneAbility` vocabulary** a data-driven gene's
      `effects` block parses into. `SpecAbilities` was renamed **`HorseAbilities`**
      because it stopped being about the spec path. The alternative - a second
      vocabulary for built-ins - would have meant writing the translator twice and
      would have let a behaviour exist for Java genes and not for gene files.
  - **Two new effect verbs**, both written for a built-in gene and both usable
    from JSON today, which is the property the shared vocabulary was chosen for:
    **`healing`** (target / radius / amount / interval / `max_targets` - the cap
    `wiki/gene-effects.html` requires of any radius effect) and **`spread`**
    (`cover` / radius / chance / interval). `spread`'s `cover` is a **vocabulary
    word, not a block id**: what "spreading moss" eats is a family of conversions
    plus a rule, and that judgement needs the block registry, so it lives in the
    translator.
  - **The emissive path is now texel-level.** `getOrCreateEmissive` folds the
    bake's own mask together with a spec `Glow`'s part list, and the renderer asks
    for it every frame (a `NO_GLOW` sentinel is cached, because `computeIfAbsent`
    will not store a `null` and would recompose the coat per frame for every
    ordinary horse). That is what lets light glow four *hooves* and two *eyes*,
    neither of which is a `Part`.
  - **Gold eyes keep a pupil.** `CoatOverlay.shadeToward` scales the target colour
    by the texel's own luma before blending, so the sclera goes gold and the pupil
    stays black; a flat lerp turned the whole eye into one gold rectangle.
  - **The size gene broke the walking animation, and the fix is one line.**
    Vanilla advances the leg-swing phase (`walkAnimationPos`) from the **world
    distance the entity moved** and nothing else; the only size compensation
    anywhere in it is a hard-coded `isBaby() ? 3.0F : 1.0F`. Nothing consults
    `Attributes.SCALE`, because before this mod nothing changed it - so a scaled
    horse walked with its feet sliding, worse the bigger it got, and mirrored on
    a small one. `GeneticHorseRenderer.stretchGaitToSize` divides the phase by
    `renderState.scale` after `super.extractRenderState`, which makes a bigger
    horse take proportionally longer, slower strides. Amplitude
    (`walkAnimationSpeed`) is deliberately untouched - it is a 0-1 multiplier on
    an angle, and an angle already scales with the model. No-op at scale 1.
  - **The spawn egg previews size.** `CustomHorseSpawnScreen` resolves the
    genome's `Traits` and scales the preview model by it, so **Reroll epi.**
    visibly resizes the horse, with a `size 1.14x` readout under the panel
    whenever it is not ordinary size. Framing is deliberately *not* refitted to
    the result - fitting a big horse back into the panel would cancel exactly the
    thing being previewed - so the model is capped at 2.5x and the readout says
    `(preview capped)` past that. The number never caps.
  - **Founder tables use a `LinkedHashMap`, baseline last** - `Map.of` iteration
    order is salted per JVM start, so a multi-allele table built from one would
    have made a world's founders **unreproducible**. Caught by
    `GenotypeTest.randomDrawsOneFloatPerGeneInGeneOrder`.
  - `coat-golden.txt` regenerated (**75 cases now**, up from 57): every row moved,
    because the gene set moved and every derived epigenetic seed moved with it.
    New `MagicalUtilityGenesTest` (32 tests). `:common:test` **300 green**,
    `:neoforge-26.1.2:build` green, creator parity **3 832 checks / 48 cases**,
    `runServer` boots clean (`40 segments`, `loaded 2 data-driven gene(s)`).
  - **Old saves will not parse.** The genotype code went 33 -> 40 segments. Dev
    only; start a fresh world.
  - **Deliberately not built:** a coat marking for milk or verdant (both are
    invisible until you put a bucket under the horse or watch the floor - logged
    as a gap, and it sits awkwardly beside healer, which draws a stripe precisely
    so you can see what it does); an epigenetic colour for light (vanilla light
    has no hue, so a blue-glowing horse would still cast white light); any stat
    change from the size locus (a ten-times horse is a spectacle, not a better
    horse); milking's §7 *rules* - tamed, full health, once a day, the stallion
    kick - four of which are limits of the effect vocabulary rather than of milk,
    so they are logged in `wiki/roadmap.html` §7 rather than special-cased.
  - Docs: seven new `wiki/gene-*.html`, `wiki/nav.js`, `wiki/gene-effects.html`
    (the two verbs + the "both kinds of gene" note), `wiki/pipeline.html` (phase
    6), `wiki/horse-body.html` (the epigenetic twin + the two-stage scale),
    `wiki/genetics-model.html`, `wiki/api-reference.html`, `wiki/modding.html`
    (walkthrough 4 + both-copies + epigenetic traits), `wiki/horse-traits.html`,
    `wiki/roadmap.html` §7, `index.html`. Checklist:
    `wiki/verification.html` §0a.

- **Built 2026-09-04, NOT yet play-tested: the horse dimension goes back to
  random pens** (roadmap §8, done). The genotype gallery is retired before
  anyone ever walked it.
  - **`common/genetics/ShowcaseGenotypes`** (new, pure, 6 tests) is the draw: an
    ordinary `Genotype.random` founder roll with a **floor** under it. Always at
    least one **natural** coat gene expressing beyond extension and agouti; with
    `MAGICAL_CHANCE` = **0.5**, a **magical** one too. A draw that already
    clears the floor is left exactly as it fell.
  - **Two rules keep it honest.** Forcing only ever keeps a combination that
    actually expresses *in this genotype* (`Genotype.shows`), so a chestnut is
    never handed an agouti it will not paint. And a combination that **`masks`**
    neither counts toward the floor nor is ever forced - a quarter of all
    founders carry the diagnostic test gene, which paints flat over everything,
    so counting it would quietly exempt a quarter of the corridor.
  - **The one honest gap**: the floor guarantees a gene *expresses*, not that it
    is *perceptible*. Mushroom on a black horse is a real expression that looks
    like nothing, and nothing in the model can answer "would a player see this".
    Accepted rather than special-cased; flagged in `wiki/verification.html`.
  - **`Genotype.with(AllelePair)`** is the founder-only setter it needed - one
    locus replaced, the shape of `withSex`, which now delegates to it.
  - **The corridor is a fixed 2 000 pens** (`DebugPenManager.PEN_COUNT`, ~7 000
    blocks), an arbitrary number rather than one derived from the genotype
    space - which is the whole point. `MAX_GALLERY_PENS`, `galleryPens()` and
    the catalogue tally sign are gone; the entrance sign now reads `Horse Pens /
    2,000 pens / random genome / mare + stallion`. `PLOT_SPACING_X` drops from
    2 526 to 8 007.
  - **Two correctness fixes the randomness forced.** `buildPen` now **clears
    untamed horses** in the pen before stocking (a pen is built once per plot,
    so anything standing there belongs to a previous occupant of that recycled X
    slot and has nothing to do with the sign just written); and `plotBox` covers
    the **whole** X slot rather than stopping at `highestIndex`, or a slot where
    a previous visitor walked further would accumulate horses forever. The
    "leaving clears entities, not blocks" claim still holds, for a new reason -
    see the horse-dimension section.
  - **`GenotypeCatalog` is untouched and still used**, by the tests and by
    whatever punnett display gets built - it just stopped driving the dimension.
    `size()` is still 2 064 387. **Known gap #12 is closed** by this rather than
    by a cap.
  - The catalogue's per-entry sign-fits test stays; `ShowcaseGenotypesTest` adds
    the **property-test twin over random draws** the roadmap asked for (a sign
    may overflow its last line, it may never drop a gene).
  - `:common:test` **268 green**, `:neoforge-26.1.2:build` green.
    `coat-golden.txt` untouched - nothing about the pipeline or any gene moved.
- **Built 2026-09-04, NOT yet play-tested: the custom horse spawn egg's editor
  was rebuilt** (roadmap §9, most of what was left). `client/
  CustomHorseSpawnScreen` is now **gene list left / live 3D horse centre /
  controls right**, and the horse that spawns is the horse you were looking at.
  - **Every registered gene is a row, alphabetically by `Gene.name()`** - "ACAN,
    Agouti, B4GALT7, Champagne, EDNRB (frame overo), KIT (white spotting)..."
    rather than `horsegenetics.ednrb` in registry order. The sex locus is
    excluded - the Mare/Stallion button owns it. The old modal `+ Add gene`
    picker is gone; the list *is* the picker.
  - **The list is a catalogue you add from.** A gene starts **off** the horse
    and draws as a plain name; **clicking the row adds it**, and only then does
    it grow two allele buttons and an `x`. Every gene carrying two baseline
    buttons from the start was a wall of `N/N` that said nothing - a horse
    carries every locus whether or not you have touched it, so what the list is
    for is picking the handful you want to see. `Row.added` is held explicitly,
    not inferred from the alleles, so a gene you added and cycled back to
    baseline stays on the horse.
  - **A gene is added homozygous for the allele that does something** - the
    first allele that is not `defaultAllele()`, doubled. If a horse cannot carry
    that pair (`KIT`'s four nonviable `W` homozygotes, `MET`'s `met/met`) the
    next one it can, and failing all of them one copy against the baseline.
    Either copy can then be cycled anywhere, baseline included.
  - **A row that expresses says so** - the name turns green and the
    `Expression.name()` prints under it (`no effect` otherwise). That is one
    more consumer for the expression prose (known gap #10).
  - **A live 3D preview** in the middle, reusing `FamilyTreeScreen`'s technique
    exactly: a throwaway client-only `Horse`, the `CoatData` injected straight
    onto `GeneticHorseRenderState`, **never** through `ClientCoatCache`. The
    coat is rebuilt only when the genotype-plus-epigenome key moves, and the
    texture factory caches by `textureKey()` on top of that, so editing is cheap.
  - **`Reroll epi.` is the point of the change.** The screen holds a real
    `Epigenome` and previews with it, so re-rolling flips through the bay leg
    heights / grey dapples / splash edges one genotype can produce - the first
    surface anywhere that makes per-allele epigenetics visible. It is then sent
    **with** the genotype (`SpawnCustomHorsePayload` gained `epigenomeCode`) and
    written straight in via the new `HorseRecords.newFounder(horse, rng,
    Genome)`. The server used to roll its own, which made the preview a
    suggestion rather than a preview.
  - **Copy code / Paste code** round-trip the genotype code through the
    clipboard (roadmap §9's "turns every bug report into something
    reproducible"). Genotype only - the epigenome has its own button, and a code
    you can paste into chat wants to stay one line. A pasted locus sitting at
    its baseline is **not** marked added, or a paste would come back as 30-odd
    rows of `N/N`. Plus a **Clear genes**.
  - **Creative-only, re-checked on the server.** `handleSpawnCustomHorse` now
    requires `getAbilities().instabuild` **and** that the sender is holding the
    egg; the payload spawns an arbitrary entity with an arbitrary genome, so the
    client-side gate was worth nothing on its own. The survival
    egg-consumption path is gone with it - it can't be reached any more.
  - **Still not built** (the remainder of roadmap §9): the per-allele
    epigenetics editor proper - each copy's priority and seed shown
    individually with a type-it-in field. At 30-odd genes that is 60 fields, so
    it wants a per-gene expander rather than a flat list.

- **Built 2026-09-04, NOT yet play-tested: the trait / size / health system, and
  the death of the random stat roll (roadmap Tier 2 §6.1, Tier 3 §7, §4.3, §4.4,
  §6.2-6.4).** Thirteen new genes, a new `common/trait/` package, and the removal
  of the last non-genetic randomness on a horse. **31 built-in genes, 33
  in-game.** Machinery is `wiki/horse-body.html`; the shape of it:
  - **`HorseStats` is deleted.** A foal's speed and health used to be a uniform
    draw from `[0.75*min(parents), 1.5*max(parents)]` - an uncapped random walk
    with no genetics in it, where two full siblings could differ by a factor of
    two and "breeding for speed" was breeding for luck. **`HorseRecord` lost its
    `speed` and `health` fields** (and `hasStats` / `withStats` / `ceilSpeed` /
    `ceilHealth`), exactly as it lost `sex`: a stored derived value can only go
    stale against the alleles beside it. `HorseRecord.traits()` resolves on
    demand. `StoredGenome` and `HorseRecordCodecs` lost the two fields too.
  - **`common/trait/`** - `HorseTraits.resolve(genotype[, healthGenetics])` walks
    `Genes.codeOrder()` once, hands every `TraitContribution` a `TraitBuilder`,
    and returns a `Traits(speed, health, jump, scale, conditions)`. **Pure**: no
    `Rng`, no epigenetics, no entity. Baselines `0.1875 / 22.0 / 0.5 / 1.0`, a
    little under vanilla's midpoints so the variant alleles are what push a horse
    up. `MIN_HEALTH = 1.0` - a genetic health value must never resolve to zero.
  - **One capability interface, not the roadmap's four.** `TraitContribution`
    (+ a `HealthContribution` marker) replaced `StatContribution` /
    `BodyContribution` / `ViabilityRule` / `ConditionRule`: they all run in the
    same walk, and the genes that need any of them mostly need several at once.
    **Viability is derived** from the worst `Condition.severity()`, so a gene
    cannot declare a horse lethal without saying what killed it.
  - **Additions are applied before multipliers**, so trait resolution is
    order-independent by construction and gene priority buys nothing here.
    Multipliers exist only for scale, because dwarfism is *proportional* - a
    dwarf pony is smaller than either alone.
  - **All thirteen genes paint nothing**: every expression is `wildType`, which
    now explicitly means "changes nothing *about the coat*". So `affectsCoat()`
    is false for all of them, they are out of the texture key, and
    `GenotypeCatalog` collapses each locus to one entry - **`size()` is unchanged
    at 2 064 387**, while `totalGenotypes()` went to **292 822 943 423 500 800**.
    Thirteen genes for zero gallery growth; the sex locus's trick, reused.
  - **Priority sub-band 80-99 is now "non-coat"**: mstn 80, pdk4 81, ckm 82,
    ryr2 83, lcorl 84, hmga2 85, acan 86, b4galt7 87, plod1 88, rapgef5 89,
    st14 90, shox 91, met 92. They sort after every painting gene and before the
    magical band.
  - **Performance / size (6):** `MstnGene` (codominant; each `C` buys 0.020 speed
    and costs 2 health - with no stamina resource, endurance is paid in hearts),
    `Pdk4Gene`, `CkmGene` (speed), `Ryr2Gene` (**jump strength is tracked for the
    first time**), `LcorlGene` + `Hmga2Gene` (**size**).
  - **`Attributes.SCALE` answers the roadmap's flagged-unverified question.** It
    exists in 26.1.2, is on `LivingEntity.createLivingAttributes` (so every
    living entity has it), and vanilla scales the **model and the hitbox** from
    it. No renderer work, no hitbox work - the whole size system is one attribute
    write. `JUMP_STRENGTH` is there too.
  - **Health (7):** `AcanGene` (**5 alleles, 15 combinations**; affected is "no
    working copy left", so `D1/D4` is affected - the check is a predicate on the
    pair, not `homozygous()`, which is the clearest argument yet for the
    combination table), `B4galt7Gene` (the one survivable disorder), `Plod1Gene`,
    `Rapgef5Gene`, `St14Gene`, `ShoxGene` (lethal at birth) and `MetGene`
    (**lethal at conception**, `canOccur = false`). Six of them extend a new
    `RecessiveDisorderGene` base - two alleles, three combinations, only the
    double-variant does anything.
  - **Four colour genes gained a disorder**: `SilverGene` `Z/Z` -> MCOA
    (impairing, -2 health), `EdnrbGene` `O/O` -> **overo lethal white, which now
    actually kills** (closing the "the death is not modelled" gap), and
    `MitfGene` / `Pax3Gene` homozygotes -> deafness, shared as **one** condition
    with two causes (`TraitBuilder` de-dups on id) and `INFORMATIONAL`, because
    the mod has no hearing to take away.
  - **No founder is ever affected** - verified over 200 000 draws. Every health
    founder table lists only the clear horse and the carrier: a wild-caught horse
    is an adult that survived, so the *only* way to see a disorder is to breed
    two carriers. Carrier rates 1.4%-4%.
  - **`server/LethalFoalHandler`** - born, then dies. **Stores nothing**: being
    lethal is a property of the genotype, so it re-reads it each second and a
    foal that logs out mid-death still dies on the way back in. Damage is
    **28% of the foal's own max health, floor 2, once a second**, which
    out-damages `HorseCareHandler`'s regen at any health value (roadmap §7's
    open item, closed). Guards: **babies only** (an adult spawned by the egg is a
    debug tool for looking at a coat), not in the horse dimension, and only on
    `health.mode = FULL`. New datapack damage type
    `horsegenetics:genetic_defect` + a lang key.
  - **The conception lethal is one branch**: `applyBredFoal` now returns
    `boolean`, `onBabySpawn` cancels the event on `false`, and
    `StallionSeedJarHandler` discards the foal (spending the jar and the mare's
    love, as a real pairing would). `Genotype.breedWith` is untouched, so the
    odds stay the ordinary one-in-four.
  - **`ServerConfig`** (the mod's first) - `health.mode` = `FULL` (default) /
    `NO_DEATHS` / `OFF`. **The genes are registered and inherited identically in
    all three**; the setting only governs whether what a horse carries is allowed
    to affect it. `OFF` is `resolve(g, false)`, which skips every
    `HealthContribution`.
  - **Surfaces read it.** The info panel shows speed / health / **jump** / **size
    (as a word)** plus a condition list, reading speed and health off the *live
    entity attributes* (authoritative and synced) and the conditions off the
    genotype; the paper dump prints all four plus every condition with its
    description. That partly closes old gap #10 - the expression/condition prose
    is finally read by something.
  - `coat-golden.txt` regenerated: **312 of 342 rows byte-identical**. Only the
    30 rows involving pink hair / magic zebra / test moved, because those three
    genes shifted position in `codeOrder()` and their derived epigenetic seeds
    moved with them. Nothing else about any coat changed - the pipeline was not
    touched. New `HorseTraitsTest` (10) + `HealthGenesTest` (8);
    `:common:test` **262 green**, `:neoforge-26.1.2:build` green, `runServer`
    boots clean (`33 segments`, `loaded 2 data-driven gene(s)`, the server config
    file generates correctly).
  - **Old saves will not parse.** The genotype code went 20 -> 33 segments. Dev
    only; start a fresh world.
  - **Deliberately not built:** DMRT3 (gait - needs animation work); per-part
    scaling for the two dwarfisms (`SCALE` is one number for the whole entity);
    ST14's near-hairless coat (phase 1 only *removes* pigment, and a de-pigmented
    mane reads as a *white* mane); CSNB (rides on `LP/LP`, and the leopard complex
    does not exist); environmental noise on the stats (deliberately zero - see
    the gaps list).
  - Docs: `wiki/horse-body.html` (new, the machinery), thirteen new
    `wiki/gene-*.html`, `wiki/nav.js`, `wiki/breeding.html`,
    `wiki/genetics-model.html`, `wiki/api-reference.html`, `wiki/modding.html`
    (walkthrough 3), `wiki/roadmap.html` §§4.3/4.4/6/7, `wiki/gene-silver.html`,
    `gene-ednrb.html`, `gene-mitf.html`, `gene-pax3.html`, `index.html`,
    `README.md`. Checklist: `wiki/verification.html` §0b.

- **Built and owner-verified in-game 2026-09-04: the white-pattern rewrite -
  four real loci replace four made-up genes.** `WhiteGene`, `SplashGene`,
  `SabinoGene` and `FrameGene` are **deleted**. In their place, named for the
  genes they model: **`KitGene`** (`horsegenetics.kit`, priority 76, **eight
  alleles**), **`MitfGene`** (`horsegenetics.mitf`, 78, four), **`Pax3Gene`**
  (`horsegenetics.pax3`, 79, three) and **`EdnrbGene`** (`horsegenetics.ednrb`,
  74, two). Still **18 built-in genes, 20 in-game**. The rule the owner set:
  *only alleles at exactly the same locus share a gene* - so tobiano (an
  inversion near `KIT`, not a `KIT` variant) and roan (region-mapped, causal
  change unresolved) stay their own genes and compose freely with everything.
  Landing with it:
  - **`KIT` absorbs dominant white and sabino**, which were two alleles of one
    real gene modelled as two independent genes - so a horse could be
    homozygous sabino *and* dominant white, a genotype that cannot exist
    because a horse has two copies of chromosome 3 and no more. Alleles
    `W22`/`W13`/`W10`/`W5`/`W23`/`SB1`/`W20`/`N`; **36 combinations, 32
    carryable, 8 outcomes**. `W20` is the booster the source describes (subtle
    alone, adds white beside another variant); `SB1` is the one documented
    viable dose series; the rest are "dominant with variable expression". Only
    a table says all three at once.
  - **Splash splits in two**, because it really is two genes (`MITF` and
    `PAX3`). That is not bookkeeping: a horse carrying one copy at each is
    markedly whiter than either alone - a genotype one gene cannot express at
    all, since one gene has two slots. `SW6`-`SW8` are deliberately folded into
    `SW5`: the source describes all four in word-for-word identical terms, so
    four alleles would be four indistinguishable rows.
  - **`EDNRB` gains `lethal-white`.** `O/O` is Overo Lethal White Syndrome, and
    the model now distinguishes **two kinds of lethal**. An *embryonic* lethal
    (`KIT`'s four nonviable `W` homozygotes, `SW3/SW3`, `SW4/SW4`) gets
    `canOccur = false` - no pen, not counted, not a founder. `O/O` is **born**,
    so it occurs, has its own masking all-white outcome and gets a pen; it is
    simply absent from the founder table, because a founder is an adult horse.
    **The death is not modelled** (no health system) - the foal lives. Deliberate;
    `wiki/roadmap.html` §6.4.
  - **Two shared painters, `coat/pattern/WhitePattern`** - `sabino` (the `KIT`
    shape: ragged margins growing inward from legs, belly, face, then torn body
    patches) and `splash` (the `MITF`/`PAX3` shape: a hard, wobbled waterline
    rising up the horse). Each takes one `strength` in `[0,1]`; each *outcome*
    picks a number on that ramp. One painter per family, not per allele - the
    difference between two alleles at one locus is overwhelmingly a difference
    of degree, and eight bespoke painters would be the same painter eight times
    pretending the differences were principled.
  - **"White finds white": both painters read the coat they are handed** and
    raise their own strength by how much of it is already de-pigmented (splash
    `0.55`, sabino `0.35`). This is load-bearing, not a flourish. Painted
    blindly, `SW1/N + SW2/N` measured **44%** white against 45% and 42% alone -
    two waterlines at the same height are one waterline, and the whole point of
    the split would have been invisible. With it: **70%**. It is also what makes
    `W20` a booster and what makes frame-plus-splash louder than either. One
    line per painter, no interaction table anywhere.
  - **Two rendering bugs caught in the sample bakes and fixed before landing.**
    `KIT` body white thresholded a high-frequency fractal and came out
    salt-and-pepper - a perfectly good *roan* and completely wrong for sabino;
    it now uses a low-frequency `PatchNoise.field` plus a fine jag on the
    threshold, the same recipe frame uses. And the splash waterline's one-pixel
    fade painted a **gold fringe** along the whole horse (a half-scaled black
    texel samples the LUT's warm diagonal), so the cut is now hard and the
    irregularity comes from wobbling *where* the line falls.
  - **`FounderTable.hardyWeinberg(Map<Allele,Double>, Predicate<AllelePair>)`** -
    the multi-allele generalisation. `KIT` has 36 combinations and four
    homozygous lethals; hand-tabulating that is not transparency, it is an
    invitation to a typo nobody would ever see. Excluded combinations are
    dropped and the rest **rescaled** - which is the biology: a lethal is absent
    from the adult population you observe.
  - **`GenotypeCatalog` is now computed on demand, not materialised.** `size()`
    is arithmetic; `get(i)` reads an odometer over each gene's non-masking
    distinct pairs, with the one entry per masking combination appended after
    them (so masked pens moved from mid-corridor to the tail). It had to change:
    the catalogue is **2 064 387** pens and an eager `List<Genotype>` that size
    is hundreds of megabytes. `totalGenotypes()` is **55 099 802 880**.
  - **The gallery was capped.** 2M pens is a ~7.2M-block corridor - a quarter of
    the way to the world border, leaving room for four plots in the dimension.
    `MAX_GALLERY_PENS` = 20 000 held it for one session. **Superseded
    2026-09-04**: the corridor is random pens now and there is no cap, no
    catalogue tally sign and no `MAX_GALLERY_PENS`.
  - **`CoatRegions.whitenLowerLeg` / `whitenBlaze` now have no callers.** Both
    cut hard, which is why every splash sock used to end in a perfect ring -
    that old known gap is gone with the gene rather than fixed. Kept as helpers,
    with the caveat written into their javadoc.
  - Measured coverage through the real pipeline (three seeds): `KIT` 5.7 / 11 /
    15 / 25 / 25 / 73 / 94 / 100 %; `MITF` 45 / 72 / 91; `PAX3` 42 / 70;
    `EDNRB` 42 / 100. New `WhitePatternGenesTest` (12 tests) pins that each
    table is total, that every declared outcome is reachable, that the ladder is
    **monotone at every step**, that the viability rules hold, and that the two
    splash loci and `W20` actually stack. **Also closes old known gap #12**:
    `CoatPipelineGoldenTest.override` now *throws* on an unknown gene instead of
    silently leaving the segment alone, and the five stale `cream` / `pearl`
    cases were re-pointed at `matp`.
  - `coat-golden.txt` regenerated (57 cases now, up from 42); **every coat
    changed** - the gene set moved, so every founder draw and every derived
    epigenetic seed moved with it. `:common:test` **248 green**,
    `:neoforge-26.1.2:build` green, creator parity **3 832 checks / 48 cases**,
    `runServer` boots clean (`20 segments`, `loaded 2 data-driven gene(s)`).
  - **Old saves will not parse their white-pattern loci.** Dev only; start a
    fresh world. Summon tokens moved: **`kit=SB1/N`**, **`mitf=SW1/N`**,
    **`pax3=SW2/N`**, **`ednrb=O/N`**.
  - **Deliberately not built: the leopard complex (`LP` / `TRPM1`) and
    `PATN1`** - the appaloosa family. It is a new pattern family (leopard,
    blanket, snowcap, varnish roan, plus white sclera / striped hooves /
    mottled skin), not an overhaul of the white markings that exist: it needs a
    spot field *and* a blanket mask, neither of which `WhitePattern`'s two
    shapes cover, and `PATN1` would be the model's first modifier gene. Logged
    in `wiki/roadmap.html` §4.2.
  - **Owner-verified in-game 2026-09-04**, as a general confirmation that it
    all renders correctly rather than an item-by-item walk of the checklist.
    The one thing left in `wiki/verification.html` is the **lethal-white
    breeding ratio** - that two `O/N` carriers throw an all-white foal about one
    time in four - because that is a statistic over many foals, not something a
    play session can see, and recording it as confirmed would be overstating
    what was checked.
  - Docs: `wiki/gene-kit.html`, `wiki/gene-mitf.html`, `wiki/gene-pax3.html`,
    `wiki/gene-ednrb.html` (all new; `gene-white`/`gene-splash`/`gene-sabino`/
    `gene-frame` deleted), `wiki/nav.js`, `wiki/genetics-model.html`,
    `wiki/pipeline.html` (the "white finds white" section),
    `wiki/api-reference.html`, `wiki/modding.html`, `wiki/roadmap.html` §4.2 /
    §5.2, `index.html`, `README.md`. Checklist: `wiki/verification.html` §0.

- **Built 2026-09-03, NOT yet play-tested: dun becomes a three-allele locus
  (roadmap §4.1).** `DunGene` now carries `D` / `d1` / `d2` - six combinations,
  three outcomes - and **`d1` is the allele that draws the dorsal stripe without
  diluting anything**, so a horse can carry primitive markings and not be a dun.
  Landing with it:
  - **The locus with two dominance orders.** Dilution reads `D > d1 = d2`;
    marking reads `D = d1 > d2`. No single label covers both, which makes this
    the clearest argument yet for the combination table - clearer than MATP,
    which at least had *one* order.
  - **`d2`, not the old catch-all `d`, is `defaultAllele()`** (the only allele
    that draws nothing). So the allele tokens moved: **summon with
    `dun=D/d2`**. `CoatSampleTool`, `CoatPipelineGoldenTest` and the three
    `LegacyCode` test strings were updated with it.
  - **How an undiluted horse shows a darker stripe.** Phase 1 is downward-only,
    so `d1` cannot paint a dark line - and does not need to: a primitive marking
    is *countershading*, so both marked outcomes run **one painter** and differ
    only in constants, the marking being the region the dilution is lerped off.
    `d1` **never touches black** (the gradient's whole `black = 1` row is pure
    black and `PURE_BLACK_ALPHA` gives such a texel 80% opacity, so nudging it
    off that row makes it *fully opaque* and therefore **darker** than its own
    stripe) and takes red only where there is red - `keepRed` ramps *up* to 1 as
    the texel's black rises, the mirror of `D`'s ramp *down* to 0. Consequence,
    and the right one: **`d1` on a solid black composes byte-identically to a
    plain black horse.** A real non-dun black shows no markings either.
  - **Leg bars stay `D`-only** (`primitive(..., legBars)`); `d1` is the dorsal
    stripe alone.
  - Founder table written out as six weights, Hardy-Weinberg at `p(D) = 1/24`
    and `p(d1) = 1/10`. The three `D` rows still sum to **8.16%**, exactly the
    old two-allele number - `d1` split the non-dun population rather than making
    duns rarer. About **18%** of wild horses now carry a stripe and no dilution.
  - The gallery grew by half: dun contributes **3** distinct pens instead of 2,
    so `GenotypeCatalog.size()` is **147 458** and `totalGenotypes()` is
    **1 033 121 304** (dun's `allPairsOf` doubled, 3 → 6).
  - `coat-golden.txt` regenerated: **all 240 pre-existing rows are
    byte-identical**, including every `D` one - the only change is 12 new `d1`
    cases. Nothing about any other coat moved. New `DunGeneTest` (8 tests) pins
    the table, the founder shares, the spine-vs-flank contrast on both marked
    outcomes, the black-coat no-op and the `D`-only leg bars. `:common:test`
    **235 green**, `:neoforge-26.1.2:build` green.
  - **Also fixed in passing:** `:common:bakeCoatSamples` had been broken since
    the MATP merge - its `build()` helper still wrote *positional* code segments
    into what is now a gene-keyed string, and three samples named the retired
    `cream` / `pearl` genes. Both fixed; two `dun_marked_*` samples added.
  - **Old saves will not parse their dun locus.** Dev only; start a fresh world.
  - Docs: `wiki/gene-dun.html` (rewritten), `wiki/genetics-model.html`,
    `wiki/roadmap.html` §4.1. Checklist: `wiki/verification.html` §0.

- **Built 2026-09-03, NOT yet play-tested: sex is a gene (roadmap §5.3, first
  half).** `SexGene` (`horsegenetics.sex`, **priority 1** - the first gene in
  `codeOrder()`), alleles `X`/`Y`: `X/X` is a mare, `X/Y` a stallion, `Y/Y`
  cannot occur. **18 built-in genes**, 20 in-game. Landing with it:
  - **Both outcomes are wild types** - the first gene in the model that paints
    nothing, ever. So the gallery does **not** widen (`GenotypeCatalog`
    collapses every wild type into one group): still **98 306** pens, while
    `totalGenotypes()` doubles to **516 560 652** (every genotype now genuinely
    comes in two sexes).
  - **`Gene.affectsCoat()`** (derived: "is any of my outcomes not a wild type?")
    plus **`Genotype.coatCode()`** - `toCode()` minus the genes that can never
    paint. `CoatData.textureKey()` runs on that, so a mare and a stallion of the
    same colour still share **one** baked texture instead of doubling the cache.
    Same reasoning as the epigenetics already excluded from the key.
  - **`Gene.canOccur(AllelePair)`** (default `true`) - `GenotypeCatalog.
    allPairsOf` filters on it, so `Y/Y` gets no pen and is not counted.
    `expressionOf` still answers for it (a hand-written code reads as a
    stallion) because parsing is tolerant.
  - **`HorseRecord.sex` is gone as a field** - `sex()` reads the locus out of
    `geneticCode` via `Genotype.sexOf(String)` (a segment scan, not a full
    parse: the info panel asks per frame). The `"sex"` codec key and
    `StoredGenome`'s `sex` component went with it, and
    `HorseRecords.randomSex` is **deleted**. `HorseRecord.founder` / `bred` lost
    their `Sex` parameter.
  - **A foal's sex is inherited, not rolled** - the dam is `X/X` and gives an
    `X`, the sire gives his `X` or `Y` 50/50, all through the same
    `breedWith` draw as every other gene. No special case anywhere.
    `Genotype.withSex(Sex)` / `Genome.withSex(Sex)` is the founder-only way to
    *choose* one (the custom spawn egg); the egg's gene list leaves the sex
    locus out, since its Mare/Stallion button owns it.
  - `coat-golden.txt` regenerated - **90 of 240 rows are byte-identical** (the
    deterministic coats; the pipeline didn't move), the other 150 shifted
    because the code gained a segment and every gene's derived epigenetic seed
    moved with its position. `:common:test` **227 green**,
    `:neoforge-26.1.2:build` green, parity **3 832 checks / 48 cases**,
    `runServer` boots clean (`20 segments`, `loaded 2 data-driven gene(s)`).
  - **Old saves will not reproduce their horses.** Dev only; start a fresh world.
  - Docs: `wiki/gene-sex.html` (new), `wiki/genetics-model.html`,
    `wiki/breeding.html`, `wiki/pipeline.html`, `wiki/api-reference.html`,
    `wiki/roadmap.html` §5.3. Checklist: `wiki/verification.html` §0.

- **Built 2026-09-03, NOT yet play-tested: the combination-table rewrite
  (roadmap Tier 1 §2, Tier 2 §5.1/§5.2).** `DominancePattern` is **deleted**.
  A gene no longer declares a dominance label; it declares an **`Expression`
  per distinct outcome** - id, display name, a human-readable description, a
  `wildType` flag ("this combination changes nothing"), a `masks` flag, a
  `deterministic` flag and **its own paint function** - plus one function
  `expressionOf(AllelePair)` mapping any combination to one of them. Several
  pairs sharing an expression *is* what "dominant" meant; only the
  double-variant landing on a non-wild-type outcome *is* what "recessive"
  meant; two variant alleles each with an outcome plus a third for the pair of
  them *is* codominance. Works for any number of alleles with no special case.
  Landing with it:
  - **`Gene.randomPair(rng)` → `FounderTable`** - a weight per allele
    *combination* as percentages, sparse (an unlisted combination never occurs),
    normalised-with-a-warning, **one `nextFloat()` per gene per founder**, with a
    `FounderContext` for a genome-aware distribution that *throws* if asked
    about a gene not yet rolled. `FounderTable.hardyWeinberg(variant, baseline,
    p)` is the convenience that reproduces the old "1 in N per allele" numbers.
    The **test gene** is why this matters: 25% of founders are `T/t` and **none**
    are `T/T`, which no per-allele frequency can express.
  - **Cream + pearl → `MatpGene`** (`horsegenetics.matp`, priority 40) - the
    proving case and the mod's first three-allele locus: `Cr`/`prl`/`N`, six
    combinations, four outcomes. Kills the impossible `Cr/Cr`-and-`prl/prl`
    genotype; `coat/pattern/CreamPearlDilution` is gone (the dose table is a
    six-row `switch` on the gene). 17 built-in genes at the time (18 now, with
    sex); 19 in-game, 20 now.
  - **`Allele` lost `visible` / `deterministic`** (both are properties of a
    *combination*) and **gained an `order`** - its index in `alleles()`, which
    is a slot order for `AllelePair`'s canonical form and **not** dominance.
    `Gene.precedence` and `AllelePair.dominant()` are gone.
  - **`Gene.wildType()` → `defaultAllele()`** - what a code segment-less gene
    reads as, a *parsing* default. The word "wild type" now means an expression.
  - **The JSON gene format went to `"format": 2`** - `dominance` and `wildOdds`
    replaced by an `expressions` table (each entry with `when`, `wildType`,
    `masks`, `description`, its own `layers` and its own `effects`) and a
    `founders` table. The parser proves the table is **total and unambiguous**
    over every `n(n+1)/2` combination: a gap, an overlap, or an unreachable
    catch-all is a load error naming the offending combination. Both shipped
    genes and all six examples rewritten.
  - **The epigenome moved onto `HorseRecord`** - see the separate bullet below.
  - `GenotypeCatalog.distinctPairsOf` now groups pairs by the **expression**
    they land on, with **every wild type counting as one group** ("changes
    nothing" is one *look*). Exact rather than an approximation, and it shrank
    the gallery from 331 778 pens to **98 306** (splash 3→2, MATP 9→4);
    `totalGenotypes()` was **258 280 326** (**516 560 652** since the sex gene).
  - `coat-golden.txt` regenerated - **every coat changed**, because founder
    draws and the gene set both moved. `:common:test` **213 green** at the time,
    `:neoforge-26.1.2:build` green, parity **3 832 checks / 48 cases** (up 48:
    the fixtures now pin which expression each combination resolves to),
    `runServer` boots clean (`19 segments` at the time, 20 since the sex gene;
    `loaded 2 data-driven gene(s)`).
  - **Old saves will not reproduce their horses** - the code lost a segment and
    the founder draw changed. Dev only; start a fresh world.
  - Docs: `wiki/genetics-model.html#combinations`, `wiki/gene-format.html`,
    `wiki/gene-matp.html`, `wiki/modding.html`, `wiki/api-reference.html`.
    Checklist: `wiki/verification.html` §0.
- **Built 2026-09-03, NOT yet play-tested: the epigenome lives on
  `HorseRecord`.** `HorseRecord` gained `epigenomeCode` beside `geneticCode`
  (plus `genotype()` / `epigenome()` / `genome()` / `hasGenome()` /
  `withGenome()`), and `data/HorseCoatAttachment` + the `horsegenetics:horse_coat`
  attachment are **deleted**. Both are heritable facts assigned once at birth, so
  storing the genotype in two places was one fact twice - and keeping the
  epigenome *off* the record is why `FamilyTreeScreen` had to invent an
  ancestor's coat from its UUID. It now draws **the real coat** (closing old
  known gap #9's second half). `HorseRecords.newFounder` rolls the whole genome;
  `HorseBreedingHandler` / `StallionSeedJarHandler` read the parent genome off
  the record; the record attachment gained `copyOnDeath`. `HorseRecordCodecs`
  serialises `epigenome_code` as an optional field defaulting to `""`, which is
  the `hasGenome()` sentinel.
- **Built 2026-09-03, owner-verified: the earlier data-model rewrite (roadmap
  Tier 1, §2).** Every `Gene` now declares `int priority()`; `codeOrder()` /
  `naturalOrder()` / `magicalOrder()` are all *derived* by one sort on
  `(priority, key)` over built-ins + `SpecGene`s together (no hand-written
  lists, loaded genes interleave by priority). The genotype / epigenome code
  strings became **gene-keyed and tolerant** (`<geneKey>=<a>/<b>`; missing gene
  = wild type, unknown gene = dropped, `""` = wild type) - see "The genetics
  model" below. `coat-golden.txt` regenerated: every **deterministic** coat is
  byte-identical (the pipeline is untouched); non-deterministic rows shifted
  because `Epigenome.random` / `fromSeed` now draw per-gene seeds in the new
  `codeOrder()` - a *stored* epigenome code round-trips unchanged, only the
  seed-derived stand-in moved. `:common:test` 195 green, `:neoforge:build`
  green, `runServer` boots clean (`20 segments` with the two shipped spec
  genes). NeoForge needed **no** source changes - it delegates all code
  parsing to `common/`. Closes old known-gap #18 (`GeneCodeDisplay` now derives
  its gene list, so spec genes show). **Owner-verified in-game 2026-09-03:**
  wild horses spawn and render correctly, right-click paper genome dump works.
  Still unconfirmed: bred foal, seed-jar round-trip, a spec gene actually
  showing in the display (needs a horse carrying Suntouched/Waterborn) -
  `wiki/verification.html` §0.
- **`common/`** - compiles; **348 JUnit tests pass** (`./gradlew :common:test`).
  Covers `trait/` (the non-coat body: `HorseTraits` / `Traits` / `Condition` /
  `TraitBuilder` / `EpigeneticTraitContribution` -> `wiki/horse-body.html`) and
  `genetics/` (allele/gene model - **42 genes**, 21 that paint and 21 that never
  do: **sex**, the 15 natural ones (extension, agouti, champagne,
  grey, **MATP** (cream + pearl, three alleles), **dun** (three alleles),
  **silver**, **mushroom**, **roan**, **tobiano**, and the four white-pattern
  loci **`KIT`** (eight alleles - sabino + the `W` series + dominant white),
  **`MITF`** and **`PAX3`** (splash, which really is two genes) and **`EDNRB`**
  (frame + lethal white)), magic zebra + pink hair, the **magical utility +
  body-stat genes** - **mane colour** + **tail colour** (three alleles each, a per-copy
  hue), **healer**, **light** (four alleles, codominant), **milk** (three
  alleles, one lethal pair), **magic body size** (codominant, epigenetic, on most
  horses), **particle** (**forty alleles**, epigenetic, paints nothing) and
  **verdant** (four alleles) - and the **thirteen non-coat
  genes** - performance (**MSTN**, **PDK4**, **CKM**), jump (**RYR2**), size
  (**LCORL**, **HMGA2**) and health (**ACAN** with five alleles, **B4GALT7**,
  **PLOD1**, **RAPGEF5**, **ST14**, **SHOX**, **MET**);
  `Genotype` code round-trip, breeding, the `Epigenome` / `Genome` per-allele
  epigenetics + priority tie-break, `GenomeSample` - a genome detached from a
  horse, for the stallion seed jar - `Expression` + `FounderTable` + the
  `GenotypeCatalog` reduction of 17 644 404 871 265 791 068 979 200 000 genotypes
  to 462 422 019 distinct coats), `coat/` + `coat/pattern/` (the
  pipeline - `CoatTextureComposer`, `PigmentField`, `ColorField`, `CoatOverlay`,
  `GradientLut`, `BayCoat`, `GreyCoat`, `WhitePattern`, `BodyStripes`,
  `HairPattern`, `CoatRegions`, the pure
  gene hooks, the `coat-golden.txt` byte-identity net, `CoatTextureId`
  texture-id injectivity),
  `coat/skin/` (`HorseSkinGeometry`), `name/` (`breedNth`),
  `horse/` (pedigree -> `wiki/breeding.html`), `trait/` (the non-coat body ->
  `wiki/horse-body.html`), and
  `genetics/spec/` (the **data-driven gene** format: `GeneSpec`, `Json`,
  `GeneSpecParser`, `SpecSchema`, `SpecValues`, `SpecGene`, `GeneSpecLoader`,
  plus `coat/pattern/SpecPainter`; and the **gene `effects`** path -
  `GeneAbility`, `AbilityType`, `HorseAbilities` - the Minecraft-specific
  things a data-driven gene does beyond the coat - see below).
- **`neoforge-26.1.2/`** - compiles and assembles (`./gradlew
  :neoforge-26.1.2:build` passes; only two `getGuiLeft/getGuiTop`
  deprecation warnings) against the real NeoForge `26.1.2.100` SDK.
- **`runServer`** - boots clean to `Done (...)! For help`; all dimensions,
  attachments, SavedData, payloads (incl. `spawn_custom_horse`), the
  `hay_portal` block + block entity, the `custom_horse_spawn_egg` item, and
  the `ClientConfig` all register with no errors. Re-verified 2026-09-02 with
  the shipped **Waterborn + Suntouched** genes loaded (`[genes] loaded 2
  data-driven gene(s): example.suntouched, example.waterborn ... 13 segments`)
  and again after the **gameplay-layer items** + the `stored_genome` data
  component were added (`Loaded 1531 recipes`, 16 new shapeless recipes among
  them; the component registers with no error - the 26.1.2 path the roadmap
  flagged as unverified now works). Re-verified again 2026-09-02 after the
  **horse-care** attachment (`horse_care`), the `care_sync` payload and the
  `horse_water` / `horse_food` block tags were added - still boots clean to
  `Done`.
- **`runClient`** - actively play-tested over the 2026-08-30, 2026-09-01 and
  2026-09-02 sessions; see below.

- **Built 2026-09-02, NOT yet play-tested:** the **seven remaining visual
  natural genes** (roadmap §§4.1-4.2), all hand-written `Gene`s in
  `common/genetics/genes/`:
  - **dilutions** - `DunGene` (then `D`/`d`, now the three-allele
    `D`/`d1`/`d2`: mild body dilution +
    primitive markings - a dorsal stripe + leg barring that skip the dilution,
    via new `CoatRegions.dorsalStripe` / `legBar`), `SilverGene` (`Z`/`z`,
    DOMINANT: eumelanin-only, chocolate body + flaxen mane/tail, chestnut
    unaffected - runs right after agouti in `naturalOrder()`), `MushroomGene`
    (`Mu`/`mu`, RECESSIVE: pheomelanin-only, chestnut -> sepia).
  - **white patterns** (all non-deterministic, epigenetic seed on the variant
    copy) - `RoanGene` (`Rn`/`rn`, DOMINANT: near-binary white-hair dither,
    density tapering back-to-front so it feathers into the solid face),
    `TobianoGene` (`To`/`to`, DOMINANT: big crisp patches from a topline-biased
    `PatchNoise.field`, white legs), `FrameGene` (`Ov`/`ov`: bold jagged-edged
    patches in an absolute-Y flank band on BODY/NECK + a bald face) and
    `SabinoGene` (`SB1`/`sb1`, dose 1 = jagged stockings + belly + blaze,
    dose 2 = "sabino-white"). **The last two are gone** - the white-pattern
    rewrite (2026-09-04) moved frame to `EdnrbGene` and sabino into `KitGene`,
    and both painters became strengths on the shared `WhitePattern`.
  - New helper `coat/pattern/PatchNoise` (warped 3-octave fractal `field` +
    2-octave `fbm2`) - the spotting genes need patch fields that cross seams
    smoothly *and* aren't one lattice cell wide; single-octave `BodyNoise`
    gridded up into visible squares. `CoatRegions.dorsalStripe` /
    `legBar` are the dun primitive-marking helpers.
  - **Reworked 2026-09-02 after owner feedback on the first bakes:** grullo
    was warm-brown not mouse-grey (dun's `keepRed` now scales to ~0 by the
    texel's black content, so a black base lands on the LUT's neutral column);
    mushroom read as no-op (`keepRed` 0.44 -> 0.12); silver's mane came out
    chestnut not flaxen (mane now pulls red down too); roan made hard 100%
    white pixels and had a hard edge at the face (now near-binary flecks +
    a smooth rear->front density falloff); tobiano was ~80% white with gold
    fringing (higher threshold, hard-binary decision - a half-scaled black
    texel reads gold on the LUT); frame produced almost nothing (per-part
    `sideWeight` left the barrel untouched -> absolute-Y band); sabino showed
    axis-aligned squares (`PatchNoise` instead of raw `BodyNoise.value`).
  - Registry was 18 genes then; it is **18** now - cream and pearl merged, then
    sex was added (20 in-game with the two shipped spec genes). `GeneCodeDisplay`'s trailing
    order gained all seven. `coat-golden.txt` regenerated (10 new cases).
    `GenotypeCatalog` blew up to 331 778 distinct coats; the combination-table
    rewrite brought that back to 98 306, and the white-pattern rewrite took it
    to **2 064 387** (`totalGenotypes()` **55 099 802 880**) - which is where
    the catalogue stopped being materialised at all and the gallery gained a
    cap - both since superseded by the revert to random pens. The
    exhaustive `2^genes` / `3^genes` tests (`CoatTextureIdTest`,
    `CoatTextureComposerTest`'s combo sweep) were converted to **seeded
    sampling** - enumerating them is no longer tractable. Sample bakes
    (`:common:bakeCoatSamples`, 12 new) look right after the rework; nothing
    seen in-game - checklist in `wiki/verification.html`.

- **Built 2026-09-02, NOT yet play-tested:** the **gameplay-layer items**
  (roadmap wiki §§11-19, first slice). 19 new `Item`s in `item/ModItems`:
  `horse_hair` + `horse_hair_bundle` (4 hair &harr; 1 bundle, roadmap §12.2's
  first two rungs), four breeding carrots
  (`mutinogenic`/`chaos`/`stabilizer`/`magnifier`), one generic
  `magic_gene_carrot` (per-gene parameterisation needs a data component -
  deferred), `placeholder_gene_book` (literal name "PLACEHOLDER GENE BOOK",
  stands in for the research paper), `empty_seed_jar` + `stallion_seed_jar`
  (both `SeedJarItem`, tooltip from the `stored_genome` component), four tickets,
  three whistles, and `stall_sign` + `bound_stall_sign`. The 17 non-sign items
  have **owner-supplied textures** (tickets share one, whistles share one); the
  two stall signs borrow `minecraft:item/oak_sign` - per-tier / real art is a
  follow-up in `wiki/verification.html` §15. New `item/ModCreativeTabs`
  registers one **Horse Genetics** tab holding all of them (20 with the custom
  spawn egg, which also still shows in vanilla Spawn Eggs). The
  **dev test-world hotbar** (`server/DebugTestWorldHandler`) gives the spawn egg
  on slot 0 (tools shifted to 1-6) and one of every new item in the main
  inventory. Recipes are datapack JSON under `data/horsegenetics/recipe/`
  (singular). **Tickets are inert** (stall-teleport needs stall blocks that
  don't exist); the carrots do nothing yet. The **whistles work** - see below.
- **Built 2026-09-02, NOT yet play-tested:** the **whistles** (`item/WhistleItem`).
  Right-click anywhere to teleport every tamed horse you own within range
  (basic 16 / golden 32 / echo 64 blocks), same dimension, not ridden, to a
  grid of spots beside you; ~3 s use cooldown, a chime, a chat count. Leashed
  horses are unleashed and come; a horse already within 3 blocks or one you are
  riding is left alone. This is the owner's "area recall" reading of roadmap
  §11; the bond-gated version waits on bond, and "what echo adds" beyond range
  is still open. Checklist `wiki/verification.html` §17.
- **Built 2026-09-02, NOT yet play-tested:** the **stall system** (roadmap §11).
  A new `bound_horse` data component (`data/BoundHorse`), a `stall_sign` /
  `bound_stall_sign` item (`item/StallSignItem`, texture borrowed from
  `minecraft:item/oak_sign`), a server-global `data/StallData` SavedData of
  `data/StallRecord`s (one per bound horse), a flood-fill `server/StallDetector`,
  and `server/StallDebug` (the "debug overlay" - a particle wireframe + chat
  summary). Flow: right-click a horse with a blank `stall_sign`
  (`server/StallSignHandler`) -> it becomes a `bound_stall_sign` carrying that
  horse's UUID + name; right-click the **outside** face of a wall with the bound
  sign -> `StallSignItem.useOn` drops a real `oak_wall_sign` with the horse's
  name and flood-fills the block **behind** that wall (this layer ± 1, air only,
  &le; `MAX_BLOCKS` 512) - an enclosed area becomes that horse's stall, its
  outline flashed with `HAPPY_VILLAGER` particles. Breaking the sign
  (`BreakBlockEvent`) releases the stall. Dev keybind **F7**
  (`key.horsegenetics.show_stalls` -> `RequestStallHighlightPayload`, dev-gated
  in `ModNetworking`) re-flashes every stall's outline near the player + prints
  a summary. **No teleport-to-stall yet** (that's the tickets, still inert); no
  client-side persistent wireframe (particles only). Checklist
  `wiki/verification.html` §18.
- **Built 2026-09-02, NOT yet play-tested:** the **stallion seed jar** first
  slice (roadmap §15.1). New `common/genetics/GenomeSample` (a `Genotype` +
  `Epigenome` detached from a horse as code strings, with `breedInto(mareGenome,
  rng)`); new `data/StoredGenome` + `data/ModDataComponents` registering the
  `horsegenetics:stored_genome` data component (persistent `Codec` +
  networked `StreamCodec`); `server/HorseBreedingHandler` refactored so its
  foal-building body is a reusable `applyBredFoal(...)`; new
  `server/StallionSeedJarHandler` - right-click a tamed adult **stallion** with
  an `empty_seed_jar` &rarr; a `stallion_seed_jar` stamped with his genome, sex,
  UUID, name, speed/health; right-click a tamed adult **mare** with a filled jar
  &rarr; a foal bred immediately through `applyBredFoal` from her live genome +
  the jar's stored one, jar consumed, mare put on the vanilla breeding cooldown.
  **Both ends require the horse to be in breeding mode** (`isInLove()` - fed a
  carrot/apple, works in creative); the op consumes that love state. The jar
  **transforms in the player's hand** (creative included). **No** real
  breeding-carrot gate (vanilla love is the stand-in), **no** gestation state
  (foal is immediate). Partly owner-confirmed 2026-09-02 (collection + tooltip);
  `:common:test` 194, `:neoforge-26.1.2:build`, `runServer` all green.

- **Built 2026-09-02, NOT yet play-tested:** the **horse-care systems** -
  gated healing (roadmap §7.2) plus bond + herds (§13), the first slice of
  both, sharing **one** slow tick as the roadmap demands. All in
  `neoforge-26.1.2/`, nothing in `common/`:
  - `data/HorseCareAttachment` - a `copyOnDeath` attachment
    (`horsegenetics:horse_care`) holding `bond` (0-100), an `Optional<UUID>`
    herd id, the `bondToday`/`dayStamp` daily-cap pair, a `bondTicks`
    fractional accumulator and a `togetherTicks` herd-formation counter;
    `behaviourTier()` (0 vanilla / 1 face / 2 approach / 3 follow).
  - `server/HorseCareHandler` - `EntityTickEvent.Post`, every **30 ticks**,
    staggered by `tickCount + entityId`. **Gated healing:** only for a hurt
    horse, a 3-block scan for a block in `#horsegenetics:horse_water` (or any
    `#minecraft:water` fluid) **and** one in `#horsegenetics:horse_food`;
    `heal(1)` + `HEART` particles per hit (`2` if in a herd). **Bond:**
    proximity (~+0.5/min), riding (~+1/min), feeding (+2, on
    `EntityInteract` with any `isFood` stack from the owner); cap **+15 per
    24 000-tick day**; foal-in-herd doubles the accrual. **Herds:**
    `togetherTicks` +30 with company / -30 alone; ≥ 12 000 → join a
    neighbour's herd or mint a `UUID` and pull in herdless neighbours; decays
    to 0 → leave. Attachment written back only on change; sync packet only
    when bond / in-herd changed.
  - `server/BondFollowGoal` - one `Goal` added at priority 4 to every `Horse`
    on join (dedup via `goalSelector.getAvailableGoals()`), inert below tier 1
    / while leashed / while ridden; tier 1 look-only, tier 2 paths only if a
    route exists, tier 3 follows persistently. Real pathfinding, never a
    teleport.
  - `network/HorseCareSyncPayload` → `client/ClientHorseCareCache` (cleared on
    `LoggingOut`); `client/HorseScreenHooks` shows a `bond N  <tier>  • herd`
    line. Care sync also sent on `StartTracking`.
  - Two block tags: `data/horsegenetics/tags/block/horse_water.json`,
    `horse_food.json`.
  - `:common:test` (195), `:neoforge-26.1.2:build`, `runServer` (boots clean,
    tags load with no error) all green. Checklist:
    `wiki/verification.html` §0; machinery: `wiki/horse-care.html`.
  - **Not in this slice:** milking (§7.1), shearing/sleeping bond (shearing
    unbuilt), a stored herd alpha, any stamina resource.

- **Owner-verified in-game (2026-09-04):**
  - **The white-pattern rewrite** - `KIT`, `MITF`, `PAX3` and `EDNRB` all render
    correctly: the eight-step `KIT` ladder reads as distinct steps, `KIT` body
    white is patches rather than confetti, the splash waterline is crisp with no
    gold fringe, the two splash loci visibly stack, `W20` boosts rather than
    acts alone, `O/O` renders pure white, and eyes survive the widest patterns.
    Confirmed as a whole, not item by item - see the note above for what that
    leaves open.

- **Owner-verified in-game (2026-09-03):**
  - **The data-model rewrite** (gene priority + derived orderings + gene-keyed
    tolerant code strings): wild horses spawn and render their correct coats -
    no regression from the new `codeOrder()` / code format - and the
    right-click **paper genome dump** works. Not yet checked: a bred foal, the
    stallion-seed-jar round-trip, and a spec gene (Suntouched/Waterborn)
    appearing in the short genome display.

- **Owner-verified in-game (2026-09-02):**
  - **Custom horse spawn egg** end to end - the egg, the age/sex/genome editor,
    and the spawn (see the status bullet below for the two follow-up fixes).
  - **Waterborn's coat**: the neon-blue streaks in the mane and tail render.
  - **Waterborn's particle trail**: the blue dust at the hooves on the move
    works. `walk_on_water` and the tamed-mare milking are **not** yet
    confirmed - `wiki/verification.html` §13.
  - **Suntouched's `glow`**: the emissive gold mane + tail render (bright in the
    dark), the gold-dust `emitter` shows, and the horse **lights the area around
    it** (the trailing `minecraft:light` block). Light cleanup on death/unload,
    the foal case, and hard base coats (cremello, dominant white) are **not** yet
    confirmed - `wiki/verification.html` §13.
  - **Stallion seed jar** - collecting from a stallion and impregnating a mare
    produces a real bred foal, and the jar carries the **correct** genome. Two
    bugs found and fixed the same day: the jar didn't visibly change in creative
    (now transforms in hand), and it worked with the horse not in breeding mode
    (now both ends require `isInLove()`). The **short genome display string**
    (info panel / tooltip) is **missing the magical genes** - a
    `GeneCodeDisplay` bug, data is fine (open issue in `wiki/verification.html`).
    The breeding-mode gate itself is built-but-unretested.

- **Owner-verified in-game (2026-09-01):**
  - **The coat pipeline end to end.** Wild spawns show a **wide variety** of
    genotypes and every one renders its correct coat - **no more flat-white
    horses**. That was the `CoatTextureId` fix (see the coat-pipeline section):
    the old `sanitize()` lower-cased the texture key, folding all 19 683
    genotypes onto 27 `Identifier`s.
  - **Agouti / bay**: renders correctly, and **seal is properly gone as a gene**
    - a high roll of bay's epigenetics gives the seal look. (Verified against
    the *old* two-number roll; the generator was rewidened afterwards - see the
    coat-pipeline section - so the spread of leg heights is unverified, the
    mechanism isn't.)
  - **Splash**: renders correctly (leg white + centreline blaze). *(That was
    the old single splash gene, retired 2026-09-04 for `MITF` + `PAX3`; the
    replacement is unverified.)*
  - **Eyes**: survive the coat on every horse seen, adult and foal.
  - **`FamilyTreeScreen`**: correct in full - nodes, coats, layout.
  - **Horse dimension**: correct in full, including the sunk pen amenities
    (water cauldron + hay bale flush with the grass) keeping horses penned.
  - **"Spawn Test Horse World"** title-screen button (dev only) works.
  - **Clock on a tamed foal** ages it to adult **without** also seating the
    player on it.
  - **Roped-horse portal shortcut**: right-clicking a `hay_portal` while
    leading a horse sends the horse through **and** drops the lead.
  - **Diluted bay points** (fixed and re-verified the same day): a bay carrying
    champagne / cream / pearl now shows real diluted points - amber champagne
    chocolate over gold, buckskin dark brown over gold, perlino rusty, pearl
    bay sepia - instead of the jet black they all rendered before. See the
    coat-pipeline section for the cause (`PigmentField.dilute`).
  - Three rendering issues found in the same session are logged in
    **`wiki/verification.html`** - see "Known gaps" below. The bay/dilution one
    closed the same day; **grey** closed later (the `GreyCoat` rework, built but
    not yet play-tested); the two **splash** ones were retired with the gene.

- **Owner-verified in-game (2026-08-30):**
  - **Hay-bale portal**: golden-carrot lighting; the animated `hay_portal.png`
    texture renders, **faces the player** (thin half-block slab), is opaque (no
    sky/clouds/water through it), and its fps **ramps 12 -> 48** as you dwell;
    **gold** dwell-swirl particles + the chat countdown; the End-portal
    starfield effect (before it was replaced by the custom texture); **return
    teleport** drops your tamed horses in the air beside the overworld portal,
    unharmed (10 s invuln), not sucked back.
  - **Horse dimension**: the reworked **E/W pen-back walls** (gravel strip
    flush to the pen, glowstone above, one wood wall, bedrock); the layered
    **wall behind the portal** (gravel floor strip + floating glowstone + wood
    wall + bedrock, no dark band / missing blocks) with the E/W walls carried
    to `originX-1` so the corner has no gap; **no block break / place** anywhere
    in the dimension (survival + creative).
  - **Info panel** (mounted horse GUI): readable shadow-free text, the "View
    Family Tree" / Set buttons + barn box visible and clickable, and **E no
    longer closes the screen while the barn box is focused** (Enter submits,
    Esc closes).
  - **`FamilyTreeScreen`**: per-node **3D horse models** in the right coat/pose
    that **turn to face the cursor**; the chart **shrinks to fit** the window
    (`boxW`/`boxH`/`uiScale` from `rebuildNodes`, full names via `drawFitted`'s
    pose `scale`) - no column overlap, nothing cut off, names legible, no
    scroll bar by default.
  - **Breeding**: a foal rolls speed/health between its parents, a correctly
    combined genetic code, and (for the pairing's **first** foal) a name
    combining both parents.

- **Built 2026-09-02, NOT yet play-tested:** **data-driven genes** and the
  **rebuilt gene creator**. A gene that fits the format is now a **JSON file**
  dropped in `config/horsegenetics/genes/` - no Java, no rebuild - and
  `wiki/gene-creator/` is the tool that writes it, previewing the gene on a 3D
  horse over any of 15 base coats before you export. Nothing about the eleven
  built-in genes changed: no gene file ships by default, so the registry, the
  genotype code, the gallery numbers and `coat-golden.txt` are all untouched.
  See "Data-driven genes" below; in-game checklist in `wiki/verification.html`.
- **Built 2026-09-02, partly play-tested:** **gene `effects`** - a data-driven
  gene can carry Minecraft-specific behaviour alongside its coat `layers`.
  Six verbs at the time, **eight since 2026-09-04** (`traversal`, `attribute`,
  `emitter`, `mob_effect`, `yield`, `glow`, `healing`, `spread`), each with an
  optional boolean `when` and a `minDose`. **`emitter` grew a `color2`, a
  `count`, a `data` number and five body-site anchors** when the particle locus
  landed. `common/`
  parses and validates all of them (`GeneAbility` records / one `AbilityType`
  per-verb declaration / a generic `GeneSpecParser.readAbility` / `HorseAbilities`,
  unit-tested); the NeoForge translator
  (`server/GeneAbilityHandler`, `server/GeneYieldHandler`) executes
  `traversal` + `emitter` + `mob_effect` + `yield` + `glow`. **`attribute` is
  the one verb parsed but not executed yet** (logged once). `mob_effect`
  resolves the id against the registry and keeps a hidden/ambient effect topped
  up on the `self` / `rider` target on its `refresh` beat (duration
  `refresh + 20`, so a `when` going false lets it decay - no explicit removal).
  **`glow`** has two independent halves: `light` (0-15) maintains a trailing
  `minecraft:light` block server-side (moved on block change, cleared on
  `EntityLeaveLevelEvent` / `when` false, skipped in the horse dimension,
  air-only placement), and `parts` (a body-region list - a new `AbilityType`
  `Kind.PARTS` parsed through `PartGroups.expand`) drives
  `client/EmissiveCoatLayer`, which redraws those coat parts full-bright over
  the base coat via a second baked texture. `walk_on_water` is an approximation
  (surface buoyancy, not a solid plane). Two shipped genes exercise it:
  **Waterborn** (`traversal` + `emitter` + `yield`) - neon-blue mane/tail
  streaks + blue particle trail **owner-confirmed in-game (2026-09-02)**,
  `walk_on_water` and the tamed-mare milking not yet - and **Suntouched**
  (`glow` + `emitter`) - light 12 + emissive gold mane + a gold-dust aura, the
  emissive mane and the area lighting both **owner-confirmed in-game
  (2026-09-02)**.
  Reference: `wiki/gene-effects.html` (verbs + the "add an effect" contract),
  `wiki/gene-waterborn.html` + `wiki/gene-suntouched.html` (the genes),
  `wiki/horse-traits.html` (the wider architecture); checklist
  `wiki/verification.html` §13.
- **Play-tested 2026-09-02, works:** the **custom horse spawn egg**
  (`item/ModItems` -> `CUSTOM_HORSE_SPAWN_EGG`). A plain `Item` reusing the
  vanilla `minecraft:item/horse_spawn_egg` texture (identical icon), in the
  Spawn Eggs creative tab. Right-clicking it (`client/CustomHorseSpawnEggClient`
  cancels the interaction) opens `client/CustomHorseSpawnScreen`. The egg, the
  editor and the spawn were all confirmed working, and two follow-ups were fixed
  the same day: a full-screen dim in `extractRenderState` was drawing over the
  buttons (widgets render *during* `super.extractRenderState`), and the gene
  list could run off the bottom of a short screen. **The editor itself was
  rebuilt 2026-09-04** - see the entry at the top of this list; what carries
  over is the egg, the interaction cancel, and the two drawing lessons. All
  custom horses get their body from their genotype like any other horse.
- **Behaviour change 2026-09-02: Waterborn + Suntouched ship loaded.** To make
  the data-driven-effects work testable in-game, `example.waterborn` and
  `example.suntouched` are registered via a **classpath gene index** -
  `neoforge-26.1.2/src/main/resources/horsegenetics/genes/index.json`
  (`["suntouched.json", "waterborn.json"]`) + the two files beside it, which
  `GeneSpecLoader.fromClasspath()` picks up in the mod constructor. These are the
  **first (and so far only) gene files to ship**, breaking the "no gene ships by
  default" invariant on purpose (the owner OK'd it): the in-game genotype code is
  now **20 segments** (13 at the time), `GenotypeCatalog`/the gallery are ~**4x** (each shipped
  `DOMINANT` two-allele gene doubles them), and shorter saved horses won't parse.
  **`:common:test` is unaffected** - the index lives in the neoforge module's
  resources, not on the `common` test classpath, so `Genes` stays at 11
  built-ins there (17 now) and `coat-golden.txt` + `SpecGeneTest`'s `BUILT_IN_GENES`
  still hold. The horse dimension will be overhauled later regardless.
- **Built 2026-09-02, `glow` owner-confirmed in-game:** **Suntouched**
  (`example.suntouched`, allele `Sntch`/`n`, DOMINANT magical, wildOdds 128,
  priority 210), plus the **`mob_effect`** and **`glow`** verb translators
  (`mob_effect` has no shipped user and is unverified). Suntouched is a spec gene
  shipped as its own file: one deterministic coat layer (`PARTS` on `HAIR` x
  `TOWARD` gold `#ffcf47` at 88%) plus an `effects` block of `glow`
  (`light: 12`, `parts: ["HAIR"]`) and an `emitter` (gold `#ffcf47` dust,
  `interval` 6, `body` anchor). It is the worked example for `glow`:
  `server/GeneAbilityHandler.reconcileGlow` maintains the light block and
  `client/EmissiveCoatLayer` + `GeneticCoatTextureFactory.getOrCreateEmissive`
  draw the full-bright mane. `mob_effect` was wired in the same pass
  (`applyMobEffect`) but nothing shipped uses it now. See
  `wiki/gene-suntouched.html`; checklist `wiki/verification.html` §13.
- **Docs 2026-09-02, no behaviour change:** the **`Docs/*.md` -> wiki
  conversion**. All five markdown docs are gone; their content lives in
  `wiki/*.html` (see the Docs-split section above), the four Javadoc comments
  that named them were repointed, and `Gene Dict.md` was split into **a page
  per gene** - all **11** now documented, where only 4 were before (and two of
  those pointed at a `CreamPearlGene.java` that has never existed). Two pages
  are new rather than converted: **`wiki/modding.html`** (how to write a gene)
  and **`wiki/api-reference.html`** (class abstractions), which is the
  modder-facing documentation `wiki/roadmap.html` §3 assumed would exist.
  `wiki/nav.js` builds every sidebar from one array, so a new page is one line.
  **No Java behaviour changed** - only Javadoc text - and `:common:test` (153)
  and `:neoforge-26.1.2:build` are both green. Nothing to play-test.
- **Built 2026-09-02, NOT yet play-tested:** the first two **magical genes** -
  **magic zebra** (`Mzeb`, dominant, 1/100 per allele) and **pink hair**
  (`Pihr`, **recessive**, 1/12 per allele). Both are phase-3 genes; details in
  `wiki/gene-*.html`, in-game checklist in `wiki/verification.html`. They take
  the registry to **11 genes**, which moves a lot of derived numbers: the code
  string is 11 segments, `breedWith` draws 22 booleans, and the gallery goes
  from 434 pens / 1 519 blocks to 1 730 pens / 6 055 blocks of corridor (long
  since overtaken - see the white-pattern entry).
  Sample bakes look right (stripes read black over cremello and over dominant
  white; pink manes keep their strand shading on black, chestnut and perlino
  alike); nothing seen in-game.
- **Built 2026-09-02, behaviour-neutral by construction:** the **three-phase
  pigment pipeline** (`wiki/roadmap.html` §1). Phase 3 is now a signed,
  uncapped `ColorField` that magical genes *add* into, both gene hooks are pure
  (read-only views in, a contribution out), and `CoatBuildContext` no longer
  carries scratch space. **No coat changed**: `CoatPipelineGoldenTest` hashes
  20 genotypes × 3 seeds × adult/foal and every one is byte-identical to the
  pre-refactor bake, so there is **nothing new to play-test** - it's groundwork
  for the magical genes. Nothing in `neoforge-26.1.2/` needed touching, which
  was the test of whether the refactor stayed inside `common/`.
- **Built 2026-09-01, NOT yet play-tested:** **per-allele epigenetics** and the
  **dapple-grey rework**. Epigenetics moved off the horse and onto the allele
  copy (`Epigenome` / `Genome` / `AlleleEpigenetics`, each copy carrying a
  `priority` + `epigeneticSeed`, inherited unchanged by a foal); `GreyGene` now
  renders a real dapple grey through the new `GreyCoat` + `BodyNoise`; and
  bay's leg black is a uniform per-horse extent with per-leg jitter instead of
  the old low-biased single number. Compiles, 138 `common` tests pass, sample
  bakes look right, nothing seen in-game yet - checklist in
  **`wiki/verification.html`**.
- **Built 2026-09-01, never play-tested, and superseded 2026-09-04:** the
  **genotype gallery** rework of the horse dimension - one pen per visually
  distinct genotype, the entrance tally sign, and the per-gene distinctness
  metadata. Reverted to random pens before anyone walked it. What survives from
  it: the per-pen genotype sign, the pair per pen, and the entity-only teardown
  that leaves blocks standing.
- **Built 2026-09-01, NOT yet play-tested:** the **dev test-world auto-delete**
  - `client/DebugTestWorldCleanup` wipes every `test_horse_*` save on client
  shutdown (and sweeps leftovers on the next start), so the button stops
  filling `run/saves`. See "Running the game".
- **Open issues + NOT verified in-game:** see **`wiki/verification.html`**.
  The two newest items are the top of the list, and they are the same play
  session: **`PAX3` `SW2` on 90% of founders** (does a herd still read as a
  population rather than one horse repeated - and does it read as *socks* or as
  four white legs, which would be gap #30 rather than the frequency) and the
  **face-marking family** (does a three-to-five-texel star read as a star at
  128px, does a snip land on the nostrils, is the star above the eyes given the
  head's approximate rest-pose projection). After those: the **particle locus**
  (§0c - forty particle ids, six body sites, none of it seen; the emitter-style
  ones and the ten Bedrock substitutions are the likeliest to read badly), the
  **seven magical utility genes** (§0a - and inside that, the **walking
  animation** of a scaled horse and whether the wild size spread reads right
  across a herd), the **trait / health layer** (§0b), the **random pens**
  and the **rebuilt spawn egg**, then **foals** (only spot-checked). Update it
  after each `runClient`.
- **Machine caveat (this dev laptop):** hybrid graphics (NVIDIA RTX 3050 Ti +
  AMD integrated). `java.exe`/`javaw.exe` are pinned to the NVIDIA GPU and the
  FML splash is disabled, or the JVM hard-crashes in the AMD GL driver. See
  "Running the game".

## Architecture - read this before editing anything

Two-module Gradle project, split deliberately:

- **`common/`** - pure Java, **zero** Minecraft/NeoForge imports (not even DFU
  `Codec`s). Subpackages by concern:
  - `genetics/` - `Gene` + `Allele` + `AllelePair`, `Expression` (one outcome a
    gene can produce, with its own paint function - what replaced
    `DominancePattern`), `FounderTable` + `FounderContext` (the wild-population
    weight per allele combination - what replaced `Gene.randomPair`),
    `Genotype` (+ `breedWith`), `Epigenome` /
    `AlleleEpigenetics` (the priority + seed on each allele copy), `Genome`
    (the two together, and the breeding that keeps them aligned),
    `GenomeSample` (a `Genome` frozen to code strings and taken off the horse -
    what the stallion seed jar carries; `breedInto(mareGenome, rng)` runs the
    ordinary draw), `ShowcaseGenotypes` (a founder draw with a floor under it -
    the horse dimension's stock; see the pens section), `AbilityContribution`
    (the capability a built-in gene implements to grant *game behaviour*, using
    the same `GeneAbility` vocabulary a data-driven gene's `effects` block parses
    into) and its per-horse twin **`EpigeneticAbilityContribution`** (the
    effect's *magnitude* is on the allele copy - the particle locus),
    **`AlleleRandomness`** (one gene's per-horse randomness, by expressing copy or
    by slot; it lives here rather than in `trait/` so both the trait and the
    ability sides can use it without the two packages depending on each other),
    `CoatPhenotype`, `GeneticCodeCombiner`.
  - `coat/` - `CoatData`, `CoatGenerator`; `coat/pattern/` holds the
    pipeline (`CoatTextureComposer`, the `PigmentField` /
    `ColorField` accumulators and their read-only `PigmentView` / `ColorView`
    faces, plus `CoatOverlay` + `CoatOverlayContribution` - the phase-4 sink for
    final pixels and the emissive texel mask) and the reusable body-space noise:
    `BodyNoise` (single-octave
    value + Worley), `BodyStripes` (X-oriented stripe field), `PatchNoise`
    (warped 3-octave fractal for white-spotting patches - `field` + `fbm2`) and
    `HairPattern` (the mane/tail painter: bands across a part's own longest
    axis, a centre stripe, and the bright-hue draw).
  - `name/` - `HorseNameGenerator` + `HorseNames` (`breed` = one-half-each;
    `breedNth` = varied by a pairing's foal count) + word tables under
    `src/main/resources/horsegenetics/names/`.
  - `horse/` - the pedigree domain model (`Sex`, `HorseRecord`, `ParentStats`,
    `HorseDatabase`, `InMemoryHorseDatabase`) -> `wiki/breeding.html`.
    **`Sex` is an enum the rest of the code reads, not a stored fact**:
    `HorseRecord` has no `sex` field and derives `sex()` from the sex locus in
    its genetic code. It has **no `speed` / `health` fields either**, for the
    same reason - `traits()` resolves them from the genotype.
  - `trait/` - the **non-coat body**: `HorseTraits` (the one walk),
    `Traits` / `Condition` / `Severity` / `Viability` (the result),
    `TraitBuilder` (the sink), and the capability interfaces
    `TraitContribution` / `EpigeneticTraitContribution` / `HealthContribution`.
    Depends on `genetics/` and
    nothing depends on it except the genes that contribute - no cycle.
    -> `wiki/horse-body.html`.
  - `genetics/spec/` - the **data-driven gene** path: `GeneSpec` (the format as
    records), `Json` (a hand-rolled parser - `common/` takes no dependencies),
    `GeneSpecParser`, `SpecSchema` (the one declaration of what each mask and op
    accepts), `SpecValues` (the per-horse knob draw), `SpecGene` (a `Gene` that
    reads a spec), `GeneSpecLoader`, and the two dev tools `SpecFixtureTool` /
    `CreatorAssetTool`. The painting is `coat/pattern/SpecPainter`.
  - `Rng` - the randomness seam (`nextFloat` / `nextBoolean` /
    `nextInt(bound)` / `nextLong`), implemented by `NeoRng` (wraps
    `RandomSource`) and, in tests, `FakeRng`.

  This is the part that survives a version port unchanged. If you want to
  import anything Minecraft-related here, stop.
- **`neoforge-26.1.2/`** - everything Minecraft-specific, by concern:
  - `client/` - renderer, texture compositing (incl. `EmissiveCoatLayer` for a
    `glow` gene's full-bright coat parts), client caches, the inventory
    hooks + `FamilyTreeScreen`, keybind, lifecycle cleanup.
  - `data/` - Data Attachments. **Two** on a horse: `horsegenetics:horse_record`
    (a `HorseRecord`, which carries the whole genome - genotype *and*
    epigenome - since 2026-09-03; there is no separate coat attachment) and
    `horsegenetics:horse_care` (`HorseCareAttachment`, the non-genetic bond /
    herd / daily-cap state). Plus the ancestry `SavedData`, codecs;
    `ModDataComponents` (the item
    data components: `stored_genome` / `StoredGenome` on the stallion seed
    jar, `bound_horse` / `BoundHorse` on a bound stall sign); and `StallData`
    / `StallRecord` (server-global SavedData of assigned stalls).
  - `network/` - custom payloads + `ModNetworking`.
  - `server/` - event handlers, the horse-dimension builder, the portal
    manager, the record adapter (`HorseRecords`, which now also resolves and
    writes the four body attributes); `LethalFoalHandler` (foals that do not
    make it - see `wiki/horse-body.html`); `HorseBreedingHandler`
    (natural breeding + the shared `applyBredFoal`), `StallionSeedJarHandler`
    (seed collection + mare impregnation, reusing `applyBredFoal`), the
    stall trio `StallSignHandler` (bind / sign-break cleanup) + `StallDetector`
    (the enclosed-area flood-fill) + `StallDebug` (particle-outline overlay),
    and the **horse-care** pair `HorseCareHandler` (the one 30-tick scan for
    gated healing + bond + herd formation) + `BondFollowGoal` (the single
    bond-tier AI goal). See `wiki/horse-care.html`.
  - `block/` - `ModBlocks` + `HayPortalBlock` (the only registered block),
    `ModBlockEntities` + `HayPortalBlockEntity` (drives the animated
    `hay_portal.png` slab renderer).
  - `item/` - `ModItems` (the **custom horse spawn egg** - a creative tool, a
    reskin of the horse spawn egg that opens a gene-list / preview / genome
    editor first; the editor screen is `client/CustomHorseSpawnScreen`, the
    spawn itself goes through `network/SpawnCustomHorsePayload` and is
    creative-gated on the server - plus the 17
    **gameplay-layer items**, roadmap §§11-19 first slice; the two
    `SeedJarItem`s, the three `WhistleItem`s and the two `StallSignItem`s have
    behaviour, the rest don't yet), `SeedJarItem` (tooltip), `WhistleItem`
    (`use` -> recall your tamed horses in a radius), `StallSignItem` (`useOn` ->
    place an oak wall sign + flood-fill the stall behind it), and
    `ModCreativeTabs` (one **Horse Genetics** tab). Recipes are datapack JSON
    under `resources/data/horsegenetics/recipe/`.

  Its job is to **translate** - build/read `common` types and shuttle them in
  and out of Minecraft's systems; the logic stays in `common/`.

When adding a feature, put as much as possible in `common/` and keep the
NeoForge module thin. That's what makes a future `forge-1.12.2/` module cheap.

## Build & test

```bash
./gradlew :common:test               # pure-Java logic, no Minecraft - fastest loop
./gradlew :neoforge-26.1.2:build     # full compile + jar; slow first run (downloads the SDK)
./gradlew :neoforge-26.1.2:runClient # launch the game with the mod
./gradlew :neoforge-26.1.2:runServer # headless dedicated server (DEBUG logging - huge log)

# gene-creator tooling (see "Data-driven genes")
./gradlew :common:bakeSpecFixtures   # what the real Java spec engine produces
node wiki/gene-creator/tools/check-parity.mjs   # ...and does the creator's JS agree?
./gradlew :common:bakeCreatorAssets  # regenerate the creator's inlined textures + examples
```

**Run the parity check whenever you touch `SpecPainter`, `SpecSchema`,
`BodyNoise`/`BodyStripes`, or any of `wiki/gene-creator/js/`.** It is the only
thing standing between the creator and quietly previewing a horse the game will
not breed - it has already caught a wrong `nextLong()` port and four schema
defaults that had drifted apart.

Run `:common:test` first when iterating on genetics/stats - it doesn't touch
Minecraft. Requires JDK 25; `foojay-resolver-convention` in
`settings.gradle.kts` auto-provisions it.

`runServer` here does not auto-stop - it sits at the console after `Done`.
Kill it (`taskkill`, or a PowerShell `Stop-Process` on the recent `java`
pids) when the smoke test has printed `Done (...)! For help`.

## The genetics model, as implemented

**Alleles are objects.** A `Genotype` is one `AllelePair` per registered
`Gene` (`common/genetics/`), held internally as a `Map<geneKey, AllelePair>`.
It round-trips through a **gene-keyed code string**: one
`<geneKey>=<a>/<b>` segment per gene in `Genes.codeOrder()`, segments joined by
`-`, the two alleles joined by `/`, dominant first. Allele tokens can be **any
run of characters** (`Spl`, `Cr`, `prl`, `Ch`, `N`, ...). Example:
`"horsegenetics.extension=E/e-horsegenetics.agouti=A/a-..."`. **Parsing is
tolerant** (dev only, no saves): a registered gene with no segment reads as its
wild type; a segment naming an unregistered gene is dropped; the empty string
is the all-wild-type genotype. A bad *allele token* on a known gene is still a
hard error. `Epigenome`'s code string is the same shape
(`<geneKey>=<pri>:<seedhex>/<pri>:<seedhex>`), with a deterministic
key-seeded placeholder filling any gene a stored code predates. **No positional
/ legacy parsing** - so adding or removing a gene is just a coat regeneration.

**Gene priority + derived orderings.** Every `Gene` declares an
`int priority()` (a fixed constant of the gene, no default). All three
orderings are *derived* by one sort of **every** registered gene - built-in and
`SpecGene` alike - on `(priority, key())`: `codeOrder()`/`all()` = the whole
sorted list, `naturalOrder()` = it filtered to `isNatural()`, `magicalOrder()`
= filtered to the magical genes. There are no hand-written lists any more, and
a data-driven natural gene at priority 45 lands *between* the built-in MATP
(40) and champagne (50) - loaded genes are not appended. Bands are a
convention: `0-99` natural, `100+` magical; `Genes.register` logs a warning for
a gene outside its phase's band (via `System.getLogger`) but carries on. Ties
break alphabetically by key. Built-in priorities: **sex 1**, extension 10, agouti 20,
silver 30, mushroom 32, dun 34, **MATP 40**, champagne 50, grey 55,
roan 70, tobiano 72, **EDNRB 74**, **KIT 76**, **MITF 78**, **PAX3 79**,
then the **non-coat sub-band 80-99** (mstn 80, pdk4 81, ckm 82, ryr2 83,
lcorl 84, hmga2 85, acan 86, b4galt7 87, plod1 88, rapgef5 89, st14 90,
shox 91, met 92 - all of which paint nothing, so their order among themselves
is arbitrary),
pink hair 110, **mane colour 112, tail colour 114, healer 116**, magic zebra 120,
**milk 130, body size 140, magic speed 141, magic health 142, magic jump 143,
particle 150, light 160, verdant 180**, test 900.
Within the natural band **low = sets pigment absolutely,
higher = dilution** (agouti's absolute points must precede
`PigmentField.dilute`). `AlleleEpigenetics.priority` is unrelated - it picks a
*seed*, never an order. `GenotypeCatalog` is lazy and invalidated on every
registration.

`Genes.codeOrder()` (derived) = **sex**, extension, agouti, silver, mushroom, dun,
MATP, champagne, grey, roan, tobiano, EDNRB, KIT, MITF, PAX3,
**MSTN, PDK4, CKM, RYR2, LCORL, HMGA2, ACAN, B4GALT7, PLOD1, RAPGEF5, ST14,
SHOX, MET**, pink hair, **mane colour, tail colour, healer**, magic zebra,
**milk, body size, magic speed, magic health, magic jump, particle, light,
verdant**, test. `naturalOrder()` (phase-1 pigment
restriction) = the same list minus the magical genes - silver / mushroom / dun sit
right after agouti so the points exist to dilute, and the six white-pattern
genes run last. Their order among *themselves* barely matters (they all zero
both pigments) with one exception: each reads how white the horse already is,
so a later one paints harder - see `WhitePattern` below. Sex and the thirteen non-coat genes are *in* `naturalOrder()` (they declare
`isNatural()`) but every one of their outcomes is a wild type, so the composer
skips them - as do milk, the four body-stat genes (size, magic speed, magic
health, magic jump), particle and verdant on the magical side:
**twenty-one of the forty-two built-ins never paint**, and
`Gene.affectsCoat()` is false for exactly those. What they do instead goes
through `common/trait/` (see `wiki/horse-body.html`) or
`common/genetics/AbilityContribution`.

**`magicalOrder()` is load-bearing**, and the utility genes tightened it:
pink hair (110) < mane colour (112) < tail colour (114) < healer (116) < magic
zebra (120). A coloured mane wins over a pink one; the healer's red stripe sits
*on top of* a coloured mane; and zebra's stripes still black out everything,
which is what its `-200%` was for. Light (160) paints in the **overlay** phase
as well, which runs after all of this and after the eyes are restored. **Full per-gene detail is in `wiki/gene-*.html`** (one page per
gene); one-liners:

| gene | alleles | outcomes (per combination) | in the wild | coat effect |
|------|---------|-----------------------------|-------------|-------------|
| sex | `X`/`Y` | `mare` (`X/X`), `stallion` (`X/Y`) - **both wild types**; `Y/Y` `canOccur` = false | 50/50 | **none, ever** - the only gene that paints nothing. Priority 1 so a future sex-linked gene reads a resolved sex; `HorseRecord.sex()` is derived from it |
| extension | `E`/`e` | wild (`E_`), `chestnut` (`ee`) | 25/50/25 | `ee` = black restricted → chestnut |
| agouti | `A`/`a` | wild (`aa`), `bay` (`A_`) | 25/50/25 | `A_` = bay; one uniform "point extent" off the `A` copy sets leg + face black, each leg jittered; a high roll = seal (non-det). Reports wild on a chestnut via `expressionIn` |
| test | `T`/`t` | wild, `test-overlay` **(masks)** | **25% `T/t`, 0% `T/T`** | `T_` = paint the `TestCoatPattern` gradient **flat on top** in phase 3 (magical; visible on any base incl. white). Its founder table is why frequency is per *combination* |
| champagne | `Ch`/`c` | wild, `champagne` | 1/40 per allele | dilute toward the gradient's gold; keeps bay's points chocolate (amber champagne) |
| grey | `G`/`g` | wild, `grey` | 1/16 per allele | **adults only** - **dapple grey** (`GreyCoat`): remaps onto the gradient's neutral column, per-horse progression / dapple size / dapple strength / point retention (non-det); foal born base colour |
| MATP | `Cr`/`prl`/`N` | wild (`N/N`), `pearl-carrier` (`prl/N`, a wild type), `single-cream` (`Cr/N`), `classic-pearl` (`prl/prl`), `double-dilute` (`Cr/Cr`, `Cr/prl`) | `Cr` 1/30, `prl` 1/22 | **three alleles, six combinations**: cream and pearl are one locus. Never leaves a pitch-black point. Was two genes + `CreamPearlDilution` |
| magic zebra | `Mzeb`/`n` | wild, `zebra` | 1/100 per allele | **magical** - black stripes hung from the topline, `-200%` on all three channels so they read black over any coat incl. dominant white (non-det) |
| pink hair | `Pihr`/`n` | wild, `pink-carrier` (a wild type), `pink-hair` | 1/12 per allele | **magical** - mane + tail walked 82% toward hot pink; reads what it paints over, so it keeps the strand shading (foal: tail only). The clearest carrier locus: two of three combinations are wild types |
| mane colour | `Mnsld`/`Mnstrp`/`n` | wild, `solid`, `striped`, `solid-striped` | 2.0% / 1.5% per allele | **magical** - the mane in any colour, solid or banded. **The heterozygote is both at once in two different colours**, which is why it is the only gene needing `epigeneticsForCopy` (one hue per allele copy, inherited with it). Foal: nothing, the foal mesh has no mane (non-det) |
| tail colour | `Tlsld`/`Tlstrp`/`n` | same four | 2.0% / 1.5% per allele | **magical** - mane colour's twin one locus over, sharing `HairColorGene`. Separate so a red mane and a blue tail is breedable. Shows on a foal (non-det) |
| healer | `Hlr`/`n` | wild, `healer-carrier`, `healer` | 9% per allele | **magical** - `Hlr/Hlr` only: a red stripe down the centre of the mane (opacity is the one epigenetic value) and a `healing` aura, 1 HP / 40 t to players within 3 blocks. The mark deliberately says nothing about the strength (non-det) |
| milk | `Watr`/`Lava`/`n` | `mares-milk`, `milk-carrier`, `water-milk`, `lava-milk`, `milk-lethal` - **all wild types** | `Watr` 8%, `Lava` 6%; no `Watr/Lava` | **magical, paints nothing** - a bucket gets milk (grown mare), water (`Watr/Watr`, any sex) or lava (`Lava/Lava`, any sex, 60 s cooldown) via a `yield`. Both variants recessive to the wild type *and each other*. `Watr/Lava` is an **embryonic lethal** (`canOccur` false), and deliberately not a `HealthContribution` |
| magic body size | `Big`/`Small`/`n` | six combinations, **six outcomes** - `giant`, `double-giant`, `tiny`, `double-tiny`, `balanced`, wild; all wild types | **80% of founders carry one copy**; heterozygotes only, no wild homozygote | **magical, paints nothing** - **codominant**: each copy carries a percentage and they **add**, `Big` positive and `Small` negative, applied through `multiplyScaleUnclamped` (after the natural clamp). Normal distribution, mean 10% and sigma 7% per copy, floored at 1%; bounded ceiling ~2.04x. The percentage is **epigenetic and per copy**, so a foal inherits its parent's exact number and two good copies are a breeding project |
| magic speed / health / jump | `Swift`/`Sluggish`/`n`, `Hardy`/`Frail`/`n`, `Springy`/`Leaden`/`n` | six combinations, **six outcomes** each; all wild types | **~80% of founders carry one copy** of each; heterozygotes only, no wild homozygote | **magical, paints nothing** - three siblings of magic body size on a shared `AbstractMagicStatGene` base. Same distribution (mean 10%, sigma 7% per copy, floor 1%, ~2.04x ceiling), same codominant per-copy epigenetic draw, but they **multiply** the resolved speed / max health / jump through `multiplySpeedUnclamped` / `multiplyHealthUnclamped` / `multiplyJumpUnclamped` - after every additive locus, bounded only by `MAGICAL_MIN/MAX_FACTOR` (0.1-10). So a magic-speed horse that is also `mstn=C/C` is faster *by the ratio*. Magic health is **not** a `HealthContribution` (the `health.mode=OFF` switch leaves it alone); `MIN_HEALTH` is still applied last |
| light | `Lthf`/`Ltmn`/`Lteye`/`n` | wild + six region combinations | 0.5% per variant allele | **magical** - gold, glowing hooves / mane / eyes and a torch-strength `glow`. **Ten combinations, seven outcomes, genuinely codominant**: each variant is dominant to `n` and to none of the others, so a horse shows everything it carries. Eyes and the emissive mask are written in the **overlay** phase |
| particle | 40 variants + `n` | wild + 40 single + 46 codominant double - **all wild types** | 0.1% per variant allele; ~7.7% of founders trail something | **magical, paints nothing** - the horse trails a particle while it moves. **Forty alleles on one locus**, so it shows at most two, ever. Non-codominant pairs hide the higher-ranked copy; nine families make 46 codominant doubles (the flames and smokes are **one** family of eight). Colour, second colour, body site, count and a spare `data` number are **epigenetic per allele copy** - the first `EpigeneticAbilityContribution` |
| verdant | `mush`/`moss`/`grass`/`n` | wild, `verdant-carrier`, `mycelium`, `moss`, `grass` - **all wild types** | 6% / 7% / 8% per allele | **magical, paints nothing** - spreads mycelium / moss / grass from the hooves via a `spread`, at most one block per beat. **Every variant needs two of itself**; a mixed pair is inert (where milk's is lethal) |
| dun | `D`/`d1`/`d2` | wild (`d2/d2`), `primitive-marks` (`d1/d1`, `d1/d2`), `dun` (any `D`) | `D` 1/24, `d1` 1/10 | **three alleles, two dominance orders**: dilution is `D > d1 = d2`, marking is `D = d1 > d2`. `D` = mild body dilution + **primitive markings** (dorsal stripe + leg bars) that *skip* the dilution so they read dark; `d1` = the dorsal stripe with **no** dilution, done as countershading (it never touches black, and takes red only where there is red - so on a solid black it is a byte-exact no-op, as a real non-dun black is). `CoatRegions.dorsalStripe`/`legBar` |
| silver | `Z`/`z` | wild, `silver` | 1/60 per allele | eumelanin-**only** dilution → chocolate body + near-flaxen mane/tail; chestnut carrier looks unchanged. Runs after agouti. Dapples are a follow-up |
| mushroom | `Mu`/`mu` | wild, `mushroom-carrier` (a wild type), `mushroom` | 1/34 per allele | pheomelanin-**only** dilution, `Mu/Mu` only → chestnut becomes flat sepia; near-invisible on black/bay |
| roan | `Rn`/`rn` | wild, `roan` | 1/30 per allele | high-freq `BodyNoise` white-hair dither on the barrel + upper legs; head / mane / tail / lower legs stay solid (non-det) |
| tobiano | `To`/`to` | wild, `tobiano` | 1/50 per allele | big smooth-edged white patches from a low-freq noise field **biased toward the topline** so they cross the back; white legs, coloured head (non-det) |
| EDNRB (frame) | `O`/`N` | wild, `frame`, `lethal-white` **(masks)** | `O` 1/55; **no `O/O` founder** | flank patches that **never cross the topline** (noise × a spine→0 weight) + a bald face; legs coloured. **`O/O` is Overo Lethal White** - born, all white, and the model's first real lethal: it `canOccur`, it gets a pen, and the *death* waits on the health system (non-det for `frame`) |
| KIT | `W22`/`W13`/`W10`/`W5`/`W23`/`SB1`/`W20`/`N` | wild, `minimal-white`, `modest-white`, `sabino`, `broad-white`, `extensive-white`, `near-white`, `dominant-white` **(masks)** | `W20` 6%, `SB1` 2.2%, the rest <1% | **eight alleles, 36 combinations, 32 carryable, 8 outcomes** - sabino and the `W` series are one gene, so a horse is one of them and never two. `W20` is a *booster* (subtle alone), `SB1` the one viable dose series, the strong `W`s "dominant with variable expression". Four homozygotes `canOccur = false` (embryonic lethal); compound heterozygotes are fine - the risk is *the same allele twice*. `WhitePattern.sabino` at a strength per outcome (non-det) |
| MITF (splash) | `SW3`/`SW1`/`SW5`/`N` | wild, `splash`, `splash-bold`, `splash-extensive` | `SW1` 4%, `SW5` 0.6%, `SW3` 0.4% | dipped in white from below, **hard-edged** waterline. `SW1/SW1` is the documented viable dose step; no `SW3/SW3`. `SW6`-`SW8` folded into `SW5` (the source words them identically) (non-det) |
| PAX3 (splash) | `SW2`/`SW4`/`N` | wild, `splash`, `splash-bold` | **90% `SW2/N`**, 1% `SW4/N`, 9% `N/N` - heterozygotes only, written out | **the second splash locus** - same painter, different gene, so a horse can be splash twice over and comes out markedly whiter than either alone. No `SW4/SW4` (never detected). A homozygote is **deaf** (informational) (non-det) |

**The thirteen non-coat genes** (priority 80-92). Every one of their outcomes is
a wild type, so **none of them paints anything** and none of them widens the
catalogue; what they do goes through `common/trait/`. Full detail in
`wiki/gene-*.html` + `wiki/horse-body.html`:

| gene | alleles | in the wild | what it does |
|------|---------|-------------|--------------|
| MSTN | `C`/`T` | p(C) = 0.35 | **codominant**, the speed/hardiness trade: each `C` = +0.020 speed and **-2 health**. The whole trade rides on `C`; `T` contributes zero, which is the rule everywhere (the baseline allele is worth nothing, so an all-wild-type horse *is* the baseline). With no stamina resource (§21), endurance is paid in hearts |
| PDK4 | `A`/`G` | p(A) = 0.25 | +0.018 speed per `A`. One **atomic** gene, not a marker set - a hidden polygenic sum is indistinguishable from a dice roll |
| CKM | `T`/`C` | p(T) = 0.20 | +0.015 speed per `T`. The weakest and second-rarest of the three speed loci, deliberately |
| RYR2 | `J`/`n` | p(J) = 0.20 | +0.09 **jump strength** per `J` - the first thing to move a stat the mod never tracked |
| LCORL | `L`/`n` | p(L) = 0.30 | **height**: +0.05 scale, +0.010 speed, +0.02 jump per `L`. `Attributes.SCALE` scales the model *and* the hitbox |
| HMGA2 | `p`/`N` | p(p) = 0.25 | **pony**: -0.06 scale, -0.008 speed, -0.02 jump, **+2 health** per `p`. The hearts are the point - small has to buy something |
| ACAN | `D1`/`D2`/`D3`/`D4`/`N` | 0.4% per variant, affected combos excluded | **5 alleles, 15 combinations**. Affected = **no working copy left**, so `D1/D4` is affected too - the check is a predicate on the pair, not `homozygous()`. `D1/D1` lethal at birth; every other `D/D` is a dwarf (scale x0.70, -6 health) |
| B4GALT7 | `d`/`N` | 4.0% carriers | Friesian dwarfism - scale x0.75, -5 health. **The one disorder a horse lives with**, and the only one that leaves the player a decision rather than a corpse |
| PLOD1 | `ffs`/`N` | 2.6% carriers | fragile foal syndrome - **lethal at birth** |
| RAPGEF5 | `efih`/`N` | 1.4% carriers | EFIH - **lethal at birth**, the most severe and the rarest |
| ST14 | `nfs`/`N` | 1.8% carriers | naked foal syndrome - **lethal at birth**. The coat half is *not* built (phase 1 only removes pigment; a de-pigmented mane is a *white* mane) |
| SHOX | `sa`/`N` | 2.0% carriers | skeletal atavism - **lethal at birth**. Pseudoautosomal, so it segregates like an autosome and needs none of §5.3 |
| MET | `met`/`N` | 3.0% carriers | **lethal at conception** - `canOccur = false`, no catalogue entry, and the pairing produces no foal at all. The opposite of `O/O`, which *is* born |

**Expressions, not dominance.** `common/genetics/Expression` is one *outcome*
a gene can produce: `id` (stable, unique in the gene - the catalogue dedups on
it), `name`, a human-readable `description` for the gene dictionary and the
wiki, `wildType` ("this combination changes nothing" - no painter, skipped by
the composer, excluded from the texture key, reads as absent in the display),
`masks` ("while this shows nothing else is visible"), `deterministic`, and
**the paint function itself** (`restrict` for a natural gene, `tint` for a
magical one - never both). A gene declares its outcomes as constants and maps
any pair to one with `expressionOf(AllelePair)`; `expressionIn(pair, genotype)`
is the same question in genotype context and is what the pipeline calls (only
agouti overrides it). `isVisible` / `isDeterministic` are now *derived*.

There is **no dominance property and there will not be one.** "Dominant" and
"recessive" only describe a two-allele locus, cannot express codominance, and
are shorthand for *which combinations happen to share an outcome* - which the
table says directly, for any number of alleles. Several pairs on one expression
= dominant; only the double-variant off the wild type = recessive; MATP's
`Cr`/`prl`/`N` = codominance. A gene may declare **several wild types** when
silent combinations deserve different wording (MATP's `pearl-carrier`); the
catalogue collapses them all into one entry, because "changes nothing" is one
look.

**`FounderTable`** replaces `randomPair`: a weight per allele *combination* as
percentages, sparse, normalised-with-a-warning, **one `nextFloat()` per gene
per founder**, in `codeOrder()`. `FounderContext` carries the genes already
rolled for a genome-aware distribution and *throws* if asked about a later one.
`FounderTable.hardyWeinberg(variant, baseline, p)` computes the three
two-allele numbers the old "1 in N" meant.

**Sex is a gene** (`horsegenetics.sex`, priority 1, `X`/`Y`) - see
`wiki/gene-sex.html`. `Genotype.sex()` / `Genotype.sexOf(String code)` read it;
`Genotype.withSex(Sex)` is the founder-only way to force one. Both its outcomes
are wild types, so it never reaches the coat: `Gene.affectsCoat()` is false and
`Genotype.coatCode()` (what `CoatData.textureKey()` runs on) leaves it out.
`Gene.canOccur(AllelePair)` rules out `Y/Y` for `GenotypeCatalog`.

**`Rng.nextGaussian()`** (default method) is the normal draw, built Irwin-Hall
(twelve uniforms minus six) rather than Box-Muller so that its tails are
**bounded at +/-6 sigma** and **all-0.5 inputs give exactly 0** - which is what lets
`MidpointRng` land on a distribution's mean instead of 1.18 sigma below it. Magic
body size is the only caller; `GAUSSIAN_SAMPLES` is part of a gene's draw-order
contract.

**Nothing about a horse is random any more except its founder draw.** Speed,
health, jump and size come out of the genotype (`common/trait/`), and the
per-allele epigenetics decide the rest; there is no third source.

Seal has **no gene** - it's the top of agouti's random distribution. Cream and
pearl are **one gene** (`MatpGene`), and dominant white and sabino are **one
gene** (`KitGene`, with six more `W` alleles beside them) - which is what the
multi-allele model bought, and what stops a horse being two things that live in
the same chromosome slot.

`Genotype.phenotype()` → coarse `CoatPhenotype` (`CHESTNUT`/`BLACK`/`BAY`/
`WHITE`; everything else ignored) - now only used for family-tree fallback
(foals are fully generated too).

`random(rng)` - each gene draws its pair from its `FounderTable`: **1
`nextFloat()` per gene**, sex among them. `breedWith` = **2 `nextBoolean()` per
gene**, which is also the whole of sex inheritance (the dam only has `X` to
give; the sire gives `X` or `Y` 50/50). `Gene.isVisible(pair, genotype)`
/ `isDeterministic(pair, genotype)` see the whole genotype (agouti invisible on
chestnut; cream/pearl read each other). `Genotype.hasVisibleNonDeterministic()`
= "generate the texture per horse".

### Epigenetics are tied to the allele, not to the horse

Each **allele copy** a horse carries has an `AlleleEpigenetics(int priority,
long epigeneticSeed)`. `Epigenome` holds one per copy, **aligned** slot-for-slot
with the `Genotype`'s `AllelePair`s; `Genome` = `Genotype` + `Epigenome`, and it
exists because only a breeding pass that draws both at once can keep that
alignment true. Both round-trip through code strings (the epigenome's is
`<priority>:<seed hex>/<priority>:<seed hex>` per gene, `-` joined, same gene
order).

- **Expression** (`Epigenome.expressed(gene, genotype)`) - heterozygote: the
  dominant copy (an `AllelePair` is canonicalized dominant-first). Homozygote:
  both express, so the tie goes to the **higher priority**. That seed is what
  `CoatBuildContext.epigeneticsFor(geneKey)` runs on.
- **Inheritance** (`Genome.breedWith`) - the allele half is bit-for-bit
  `Genotype.breedWith`; each inherited allele brings its parent copy's priority
  and seed **verbatim**, no re-roll and (deliberately, for now) no variation.
  Copies are re-aligned after `AllelePair` sorts them. If both copies arrive on
  the same priority, one extra `nextBoolean()` bumps the second **±1**
  (`AlleleEpigenetics.deconflict`), clamped to `[1, Integer.MAX_VALUE]` - so a
  horse never carries a tie. Draws: **2 `nextBoolean()` per gene, +1 per tie.**
- **Founders only** roll fresh epigenetics (`Epigenome.random` via
  `CoatGenerator.generate`). A foal must never go through that path - see the
  data-flow section.
- `priority` (the epigenetic one, `AlleleEpigenetics.priority`) has no other
  consumer, and **by design it never will**: it picks *which copy's seed
  expresses*, never the order genes are processed in. Gene processing order
  comes from `Gene.priority()` (a fixed per-gene constant; `Genes` sorts on
  `(priority, key)`), so two horses with the same genotype and the same seeds
  can't diverge on epigenetic priority alone and silently share a coat cache
  entry. It stays a full-range int for headroom.

Full inheritance detail: **`wiki/breeding.html`**.

## Data-driven genes + the gene creator (`common/genetics/spec/`, `wiki/gene-creator/`)

**The point:** a gene that fits a fixed set of shapes is a **JSON file**, not a
Java class. Drop it in `config/horsegenetics/genes/` and restart. Full format
reference is **`wiki/gene-format.html`**; the machinery:

- **`GeneSpec`** is the format as records (**`"format": 2`** since the
  combination-table rewrite): a header (key, alleles, priority) plus an
  **`expressions`** table and a **`founders`** table. Each expression names the
  combinations that land on it (`when`, a list or a token→count map; exactly one
  entry may omit it and catch the rest), carries `wildType` / `masks` /
  `varies` / a human-readable `description`, and holds **its own `layers` and
  its own `effects`**. The parser proves the table is **total and unambiguous**
  over all `n(n+1)/2` combinations - a gap, an overlap or an unreachable
  catch-all is a load error naming the offending combination. A layer is
  **where** (`Mask`s, folded into one coverage value per texel) times **what**
  (an `Op`), applied *scaled by* that coverage - so a spec gene's edges are soft
  by construction, which is the one thing each hand-written gene had to remember
  separately (the retired splash gene's hard sock ring was the counter-example;
  `WhitePattern` now owns the margin for every hand-written white gene).
- **`SpecSchema`** is the single declaration of which parameters each mask and
  op accepts and what each defaults to. The parser validates against it (an
  unknown key is an error naming the key and listing the legal ones), the
  creator builds its forms from a mirror of it, and `wiki/gene-format.html`
  quotes it. **Add a mask or an op in all three, or the tool and the game
  drift.**
- **Values.** Any numeric parameter is a constant, `"$knob"`, an inline
  `{min,max}` (which the parser turns into an anonymous knob), or
  `{"perDose":[a,b,c]}` (which counts copies of the **first-declared** allele -
  a within-one-expression convenience, largely superseded now that a different
  dose can simply be a different expression). `SpecValues` draws every knob once, in declaration
  order, off `ctx.epigeneticsFor(key)` - so the determinism contract holds
  unchanged. A `per: "leg"` knob with a `spread` reproduces `BayCoat`'s
  "one extent for the horse, each leg jittered" in one line of JSON.
- **`SpecGene`** answers the whole `Gene` interface from the spec, so nothing
  downstream knows or cares that it came from a file - genotype code, breeding,
  the catalogue, the coat.
- **Loading** is `GeneSpecLoader`: a classpath index (for genes shipped inside a
  mod) plus a real folder walked in filename order. **Two genes now ship** -
  `example.suntouched` and `example.waterborn`, via
  `neoforge/src/main/resources/horsegenetics/genes/index.json`, added 2026-09-02
  to make the effects work testable (see the status snapshot). So **in-game** the
  genotype code is **20 segments** and the gallery numbers are ~4x;
  **`:common:test` still sees the 17 built-ins** because that index is not on its
  classpath, so `coat-golden.txt` is untouched.
  `neoforge/ModGeneSpecs` calls it **from the mod constructor**, which is the
  earliest hook there is: every registration lengthens the genotype code by a
  segment, so a gene registered after something has parsed a code would
  invalidate it. A bad file is logged and skipped, never fatal.

### `effects` - Minecraft behaviour on a data-driven gene

An **expression** may carry an **`effects`** array alongside its `layers` - the
things a gene makes the horse *do*, not the pixels it paints. Because effects
hang off the outcome, a homozygote and a heterozygote can grant entirely
different behaviour by being different expressions, with nothing comparing
doses. Closed set of **eight** verbs
(`traversal`, `attribute`, `emitter`, `mob_effect`, `yield`, `glow`, `healing`,
`spread`); each takes an
optional boolean **`when`** (flags + `all`/`any`/`not`) and a **`minDose`**
(1 = any expressing copy, 2 = homozygous). `common/` owns the vocabulary and
the parse (`GeneAbility` records, one `AbilityType` module per verb, a
  generic `GeneSpecParser.readAbility`,
and `HorseAbilities.activeFor(Genotype)` which picks the expressed ones); it
never touches Minecraft. The **NeoForge translator** is `server/`
`GeneAbilityHandler` (an `EntityTickEvent.Post` that evaluates conditions and
applies `traversal`, fires `emitter`s, keeps `mob_effect`s topped up via
`applyMobEffect`, and reconciles `glow`'s light block via `reconcileGlow` +
an `EntityLeaveLevelEvent` cleanup) and `server/GeneYieldHandler` (an
`EntityInteract` handler for `yield`). Both short-circuit when
`HorseAbilities.anyLoaded()` is false - which, since the magical utility
genes were built in, it never is; the per-horse ability list is cached by
genetic code instead, which is where the cost was. `glow` also has a **client** half - `GeneticHorseRenderer` reads the
expressed `Glow.emissiveParts()`, `GeneticCoatTextureFactory.getOrCreateEmissive`
bakes a full-bright mask, and `client/EmissiveCoatLayer` (a twin of vanilla's
`HorseMarkingLayer`) draws it with `RenderTypes.eyes(...)`. It is the only verb
with a client-render component.
**`attribute` is the one verb carried but not executed yet** - the handler logs
it once (and logs `mob_effect:<id>` once if an effect id fails to resolve).
`walk_on_water` is surface buoyancy + "don't sink", not a real collision plane.
**Not play-tested.** Full reference: `wiki/gene-effects.html`; worked examples
`wiki/gene-waterborn.html` + `wiki/gene-suntouched.html`; the wider architecture
`wiki/horse-traits.html`. **Built-in Java genes use this path too** since
2026-09-04: a `Gene` may implement `common/genetics/AbilityContribution` and
return the same `GeneAbility` records, and `HorseAbilities` (renamed from
`SpecAbilities`, which had stopped being about the spec path) collects both. One
vocabulary, one translator - which is why `healing` and `spread`, both written
for built-in genes, were usable from a gene file the day they landed. The verb
set is now **eight**.

### The creator, and why it can be trusted

`wiki/gene-creator/` is not a mockup. It runs a **JavaScript port of the coat
pipeline** - `geometry.js` (`HorseSkinGeometry`), `noise.js`
(`BodyNoise`/`BodyStripes`, on a hand-written two-word u64 because the hash is
64-bit integer maths and JS numbers are doubles), `fields.js`
(`PigmentField`/`ColorField`/`GradientLut`/the composer), `base-coats.js` (the
real natural genes, for the 15 base coats a new gene is previewed over) and
`spec-engine.js` (the twin of `SpecPainter`, plus a `java.util.Random` port so a
preview seed draws the numbers the game would).

- The 3D horse is **built from the geometry tables**, not loaded from a GLB, so
  the UV layout is the game's by construction and a click maps back through the
  same sample grid the pipeline uses. (It also means no file fetch, which
  `file://` blocks.) The old `wiki/horse.glb` and `wiki/horse_white.png` are
  gone with it.
- Everything is a **classic script** with an `HG` namespace and the textures are
  inlined data URIs, because ES modules and cross-file image reads are both
  blocked under `file://`. The two generated files come from
  `./gradlew :common:bakeCreatorAssets`.
- **A port drifts, and a drifted preview is worse than none** - it shows a horse
  the game will not breed, convincingly. So `./gradlew :common:bakeSpecFixtures`
  writes what the real Java engine produces (48 cases: 4 example genes x 3 seeds
  x 2 doses x adult/foal, plus the whole `SpecSchema` table) and
  `node wiki/gene-creator/tools/check-parity.mjs` re-runs it in JS - **3 784
  checks**. It earns its keep: it caught `nextLong()` ported as a concatenation
  where Java does a signed add, and four schema defaults that had drifted (the
  creator omits any setting left at its default, so a mismatched default writes
  a file that plays differently from how it previewed - silently, and only for
  the settings you never touched).

## The coat pipeline (`common/coat/pattern/` + `client/GeneticCoatTextureFactory`)

Coats are **generated** for every horse - adult *and* foal. Per-gene detail in
**`wiki/gene-*.html`**; the machinery:

- **Every gene is either natural or magical, never both** (`Gene.isNatural()`,
  declared not inferred; a gene wanting both registers as two). A **natural**
  gene only pushes red/black pigment *down* in phase 1; a **magical** gene only
  adds *signed RGB* in phase 3, after the pigment has been resolved to colour.
  Natural is reserved for genes that exist in real life
  (`wiki/philosophy.html` §6).
- **Both coat hooks are pure.** A gene is handed **read-only views** of the
  state so far (`PigmentView`, `ColorView`) and **returns** its contribution;
  it never draws into shared scratch. `CoatBuildContext` therefore carries no
  fields any more - it's just genotype / epigenome / skin / adult /
  `epigeneticsFor`, and the composer owns both fields. Same inputs → same
  output, and a gene can be unit-tested against a synthetic coat on its own
  (`GeneCoatHookTest`).
  - Phase 1: `PigmentField restrict(pair, ctx, PigmentView coat)` - take
    `coat.mutableCopy()`, paint into it, return it; `null` = no contribution.
    (The doc's original sketch also handed phase 1 the magical field; it
    doesn't exist yet at that point in the bake, so it isn't passed.)
  - Phase 3: `ColorField tint(pair, ctx, PigmentView coat, ColorView colour)` -
    return a delta, or `null`. It gets the resolved natural coat too, so a gene
    can *find* a region (all the black, all the white) before painting it.

- **`CoatData`** = a `Genome` (`Genotype` + `Epigenome`), assigned once at
  birth and persisted. `textureKey()` = **`Genotype.coatCode()`** - the code
  minus every gene that can never paint (`Gene.affectsCoat()`; only sex, today,
  which is what stops a mare and a stallion of the same colour baking two
  identical textures) - plus `@<fingerprint hex>` only when non-deterministic -
  `Epigenome.visibleFingerprint(genotype)` digests just the *expressed* seeds of
  genes that are visible **and** non-deterministic, so epigenetics a horse can't
  show don't fork the texture cache. The factory also keys on adult vs foal.
  `coatCode()` is **not** a persistence format - it's lossy and nothing parses
  it back.
- **`CoatTextureComposer.compose(genotype, epigenome, Skin, adult, template, GradientLut)`**
  → 128px `int[]` ARGB:
  1. **natural pass** - every pixel starts max red + max black; each visible
     natural gene (`Genes.naturalOrder()` = extension → agouti → cream → pearl →
     champagne → grey → roan → tobiano → EDNRB → KIT → MITF → PAX3) pushes the
     `PigmentField` down.
  2. **resolve** - `(red, black)` → `GradientLut`. Both pigments **≈ 0**
     (`≤ TRANSPARENT_EPS` = 0.001 - only a white-pattern gene, all of which
     `setRed(0)`/`setBlack(0)` exactly) → transparent. The cutoff is
     deliberately far below any *dilution* floor (grey keeps 0.15; grey on a
     double-dilute cream still ≈ 0.012) - a 0.02 cutoff here was turning grey
     cremello / perlino chestnuts and bays into flat white horses.
     **Resolves to pure black → 80% opacity** (`PURE_BLACK_ALPHA`) so black
     isn't a flat void.
  3. **magical (RGB) pass** - each visible magical gene (`Genes.magicalOrder()`
     = pink hair → magic zebra → test) returns a signed RGB delta, folded into the `ColorField` by
     integer addition. Because that's associative and exact, ordinary magical
     genes are **order-independent and drift-free**. `ColorField.set` is the
     escape hatch - flat opaque paint that *replaces* the accumulator (order
     *does* matter for those), used only by a `COMPLETE_DOMINANT` gene that
     must read the same on black, chestnut *or* white. Test is the only one.
     **`magicalOrder()` is not arbitrary**, despite the additivity: pink hair
     *reads* what it's painting over (so it's order-dependent by choice), and
     Test paints flat and masks everything, so it has to run **last**.
  4. **composite** onto the template, **alpha-aware** multiply (`blend`),
     keeping template alpha. Because it's a *multiply*, the template's own dark
     detail - hooves, nostrils, the shading between mane strands - survives on
     every coat, cremello included. Near-black texels there are expected and
     are **not** a gene failing to dilute; check the `PigmentField` values, not
     the composed pixels, when chasing one.
  5. **eyes** - `CoatRegions.redrawEyes(skin, …)` copies them verbatim (adult:
     2x2 pupil + 2x2 sclera at `{6,42}`/`{28,42}`; foal: 2x2 pupil at
     `{6,20}`/`{40,20}` - the head's L/R faces, not the front blob).
  6. **overlay** (new 2026-09-04) - every gene implementing
     `CoatOverlayContribution` gets a `CoatOverlay` carrying the finished coat
     and may write **final pixels** and mark texels **emissive**. Almost nothing
     runs here; it exists for the only two things the earlier phases
     structurally cannot do. **The eyes**: step 5 restores them from the template
     precisely so a wide white pattern can never blind a horse, so a gene that
     wants to colour them *on purpose* has to run after it. **Emissiveness**:
     "this texel glows" is not a colour, so neither accumulator has a channel
     for it. `bake()` returns `Baked(argb, emissive)`; `compose()` is the pixels
     alone, which is why the golden test and every existing caller were
     untouched. A sink like `TraitBuilder` (absolute writes, nothing to fold);
     `base` is fixed, so no gene reads another's overlay. Two blend modes -
     `blendToward` (a lerp) and `shadeToward` (scaled by the texel's own luma
     first, so a gold eye keeps a dark pupil instead of becoming one gold
     rectangle).

  Natural genes only move the pigment sample → champagne / cream / pearl all
  read off whatever is underneath; anything on white is invisible.
- **`ColorField`** is the phase-3 accumulator: per texel a **signed `int`**
  r/g/b **plus a separate opacity**, seeded from the resolved colour.
  - **Nothing is capped until `argb()`.** That headroom is the point: a gene
    can add so much blue that no combination of other genes pulls it back
    under 255 - the horse is blue unconditionally and its author never had to
    know what else it carries. Zebra is the same trick with the sign flipped.
    `add` **saturates** at `Integer.MIN/MAX_VALUE` rather than wrapping, so
    "obviously large" numbers stacked twice can't flip an always-blue horse
    black.
  - **Opacity is its own channel, deliberately.** Transparency used to ride on
    the pigment channels (both ≈ 0 → the bald template shows). Once phase 3 can
    add colour to a texel carrying no pigment, "no pigment" and "no paint" stop
    being the same statement. A magical gene *may* paint a dominant-white horse
    - white is natural, and every magical gene runs after every natural one -
    but it has to say so with `addOpacity`/`set`; colour alone on a transparent
    texel shows nothing.
  - The blend onto the resolved colour is a **straight signed add**, which is
    what keeps phase 3 order-independent. Anything fancier (a real
    overlay/soft-light) reintroduces order-dependence - if it ever changes,
    re-check that claim.
- **`CoatPipelineGoldenTest`** is the machinery's safety net: 20 genotypes ×
  3 seeds × adult/foal, hashed. It exists to prove a change to the *pipeline*
  leaves every horse byte-identical. When a **gene** deliberately changes,
  regenerate `common/src/test/resources/coat-golden.txt` (delete it, run the
  test, copy `common/build/coat-golden.txt` back) and say so in the commit.
- **`GradientLut`** wraps `assets/horsegenetics/textures/coat/redblackgradient.png`
  (hand-authored, 500x500): left = more red, bottom = more black; `(1,1)` =
  black, `(1,0)` = chestnut, `(0,0)` = white, champagne-gold column near the
  middle. `sample(red, black)`: `x = (1-red)*(w-1)`, `y = black*(h-1)`.
  **The `red = 0` column is the only neutral grey ramp in the whole LUT**; the
  *diagonal* runs through the golds (equal keep `0.4` on a black horse samples
  `(150,109,56)`, a tan). Any effect that should read grey has to walk the
  sample toward that column, not scale both pigments - that's the whole reason
  for `GreyCoat`'s remap.
- **`GreyCoat`** - the dapple-grey generator. Reads how dark each texel
  currently is (`0.55*red + 0.95*black`), writes that darkness back as **black**
  scaled by the horse's greying progression, and keeps only a fading trace of
  the red - so a grey lands on the gradient's neutral column while a greying
  chestnut still ends lighter than a greying black. Four epigenetic knobs off
  the `G` copy (+ a `long` seeding the dapple field): **progression** (keep
  `lerp(0.46, 0.10, p)`, steel → near-white), **dapple spacing** (2.8-5.0 body
  units), **dapple strength** (contrast, peaked mid-greying), **point
  retention** (mane/tail/ears/muzzle full, head half, lower legs ramped; scaled
  by `1 - progression`). Dapples come from `BodyNoise`.
- **`BodyStripes`** - the reusable stripe field, pure and in body space:
  bands of near-constant X warped by `BodyNoise`, plus a small **slant on
  `|z|`** that bends each stripe into a shallow chevron over the back. The
  slant isn't decoration - without it every face perpendicular to X (chest,
  rump, the front and back of each leg) sits at one phase and renders as a flat
  band. Magic zebra is the first caller; a natural **dun**'s leg barring and
  **brindle** should reuse it rather than reinvent it.
- **`BodyNoise`** - pure `(seed, x, y, z)` noise sampled in **body space**, so a
  pattern crosses part seams without a join. `cellDistance` = distance to the
  nearest jittered-lattice point, normalized (centres = dapples, gaps = the web
  between them); `value` = smooth value noise, used to warp the lattice off the
  grid. No state, no `Random` - a rebuilt coat is identical.
- **`CoatRegions`** - reusable `Skin`-aware helpers (fill mane/tail/ears/hooves,
  paint/blacken/whiten a leg, `whitenBlaze`, `redrawEyes`). **Open issue:**
  `whitenLowerLeg` cuts at a hard `point.y() <= cutoff`, so every sock it draws
  ends in a perfect ring - which is why **no built-in gene calls it any more**;
  `WhitePattern` owns the margin now. `whitenBlaze` is likewise uncalled, and
  **the wider gap it stood for is closed** (2026-09-05):
  `WhitePattern.faceMarking` is the shared face vocabulary, and a star and a
  snip are real detached patches. Both helpers are now dead code kept only for
  their javadoc's warning.
- **`BayCoat`** - the bay generator. One **uniform** per-horse "point extent"
  off the `A` copy (`leg = 0.15 + extent*0.80`, `face = 0.04 + extent²*0.62`),
  then each of the four legs jittered `±14%` independently - so bays actually
  spread from low socks to seal instead of clustering low the way the old
  `f*f` product did, and one horse's four socks are never exactly level. Bottom
  `SOLID_PORTION` = **0.3** of the band solid, then a **smoothstep** fade to
  nothing - no hard cut-off line. Knobs: `BODY_BLACK`, `HOOF_FRACTION`,
  `SOLID_PORTION`, `LEG_JITTER`. Verified in-game 2026-09-01 (seal included);
  the widened distribution is **not** yet play-tested.
  Its points are set *absolutely* (`setBlack(1.0)` / `setRed(0.0)` via
  `CoatRegions.blackenPart` / `blackenLowerLeg`) and that's fine - the
  dilutions run after and scale them. What was **not** fine is that a point
  carries `red = 0`, and the gradient's zero-red column reads jet black down to
  `black ~0.4`, so scaling black alone never changed the colour. Every dilution
  now goes through **`PigmentField.dilute(keepRed, keepBlack, blackTint)`**,
  which also adds `blackTint * black` back as *red* - walking the sample
  sideways off that column into the warm browns. Amber champagne keeps
  chocolate points over a gold body, perlino rusty ones, a buckskin dark brown.
  **House rule: no cream horse keeps a pitch-black point** - dark brown is as
  far as it goes. Per-mode numbers: `wiki/gene-*.html`.
- **`GeneticCoatTextureFactory`** (client) loads the adult + foal templates +
  gradient once, runs `compose`, uploads a `DynamicTexture`, caches by
  `textureKey()` + `:adult`/`:foal`, cleared on world exit.
  - The `Identifier` it registers each texture under is
    **`coat/` + `CoatTextureId.encode(key)`** (`common/coat/`), an *injective*
    map into Minecraft's legal path charset: `a-z0-9` verbatim, `A-Z` → `.` +
    lower-case, everything else → `_` + 4 hex. **Do not go back to
    lower-casing / `[^A-Za-z0-9_] -> _`**: case *is* the dominance encoding, so
    that folded all 19 683 genotypes onto **27** ids
    (`E/e`≡`e/e`, `W/w`≡`w/w`, `A/a`≡`a/a`, …). `TextureManager#register` is a
    silent `Map.put` that **closes the loser**, so every deterministic coat in a
    bucket rendered whichever one baked last - a plain white horse whenever that
    was a dominant-white `W_` one. That was the real "chestnut/bay renders as the
    default white horse" bug (the `[coat] >> FLAT WHITE` debug line does *not*
    catch it: the victim's own bake is correct, it just loses its id).
    `KEY_BY_ID` in the factory is a tripwire that throws if two keys ever share
    an id again.
- Dev tool: `./gradlew :common:bakeCoatSamples` → `build/coat-samples/*.png`
  and `*_foal.png` (no game launch).

### The HD horse models (`client/HdHorseModel`, `client/HdBabyHorseModel`)

128px, per-part UV - structural copies of vanilla `AbstractEquineModel.
createBodyMesh` / `BabyHorseModel.createBabyMesh` with every cube at
`texScale = 0.5` and the layer baked at 128x128 (`ClientSetup.HD_HORSE` /
`HD_HORSE_BABY`), so `CubeDefinition.bake` → effective texture size
`128*0.5 = 64` and **every normalized UV is identical to vanilla** - the 2x
sheet just gives each face 2x the texels. The adult model additionally
re-`texOffs`'s the four legs / two ears onto their own patches and drops
`.mirror()`; the baby model already has per-leg patches so it's a straight 2x
pass. `horse_white.png` / `horse_white_baby.png` (in `common/.../assets/`, with
`*_vanilla64.png` references) are the vanilla white sheets scaled 2x.
`GeneticHorseRenderer` hands both models to its super ctor as adult / baby - no
per-entity model swap. It deliberately **does not add vanilla's
`HorseMarkingLayer`**: that layer paints `horse_markings_white.png` etc. over
the whole texture, so any horse (wild spawn or foal) that rolled
`Markings.WHITE` rendered as a **flat white horse** on top of a correct
generated coat. All white markings here come from the white-pattern loci,
inside the generated coat texture.

### `common/coat/skin/HorseSkinGeometry` - the body-space projection engine

Two meshes: `Skin.ADULT` and `Skin.BABY`. Body-space grid (adult verified
in-game 2026-08-31 as smooth / seamless): **X** 0 at the tail's rear edge →
+nose, **Y** 0 at the hoof bottom → +up, **Z** 0 at centre, **+Z = horse's
right**; model units (1 = 1/16 block = 2 texels). Each `Part` is an
axis-aligned box with six `Face`s (NOSE/TAIL span (Z,Y); TOP/BOTTOM span (X,Z);
RIGHT/LEFT span (X,Y)); the foal mesh has no MANE / MUZZLE. Static no-`Skin`
methods target ADULT; `Skin`-first overloads pick the mesh (`bounds`, `sample`,
`forEachTexel`, `project`, `bodyBounds`, `hasPart`). One absolute scale per
mesh, so an X-function coat is seamless. Geometry tables are lifted from the two
HD models; **rotated parts use their rest-pose AABB** (the foal's neck/head/ear
pivots are pre-resolved through the tilted neck) - face projection there is
approximate.


## Data flow (server -> client -> pixels)

1. **Wild spawn / `/summon` / a dimension pen horse** -> one `HorseRecord` attachment
   carrying **both** the genotype code and the epigenome code
   (`HorseRecords.newFounder` -> `CoatGenerator.generate` -> a founder
   `Epigenome.random`). There is **no separate coat attachment** any more; the
   record default is `HorseRecord.unassigned(uuid)`, whose empty
   `epigenomeCode` is the `hasGenome()` sentinel the join handler tests.
   **Breeding is different**: `HorseBreedingHandler` builds the foal's genome
   itself from `damGenome.breedWith(sireGenome)` and writes it into the foal's
   record, because the inherited epigenetics can only be read while both
   parents are in hand - the join handler would re-roll them. It then sees
   `hasGenome()` true and leaves it alone.
   **The custom horse spawn egg** is a third path: `ModNetworking`'s
   `SpawnCustomHorsePayload` handler applies a founder record with the
   player-picked code + sex **before** `addFreshEntity`, so the join handler
   takes its "already has a real record" branch and keeps the genome. Since
   2026-09-04 the **epigenome comes with it** rather than being rolled: the
   editor previews a live 3D horse, and a preview the spawn re-rolls is not a
   preview. That is the one founder path that does not call
   `CoatGenerator.generate`.
   **The stallion seed jar** is a fourth path, and it behaves like breeding:
   `StallionSeedJarHandler` builds the foal `Horse` itself, reads the mare's
   genome live and the sire's from the jar's `StoredGenome`, and calls the same
   `HorseBreedingHandler.applyBredFoal` - which writes the coat attachment
   before `addFreshEntity`, so the join handler leaves it alone.
2. Not auto-synced -> the handler sends `CoatSyncPayload` `{entityId, code,
   epigenome}` and `HorseRecordSyncPayload` to trackers (on every join, plus
   `StartTracking`).
3. Client caches in `ClientCoatCache` (`CoatData`) / `ClientHorseRecordCache`,
   cleared on `LoggingOut`.
4. `GeneticHorseRenderer.extractRenderState` reads `ClientCoatCache` ->
   `GeneticHorseRenderState.coatData`; `getTextureLocation` ->
   `GeneticCoatTextureFactory.getOrCreate(coatData, renderState.isBaby)` -
   generated for adult and foal alike (`HdHorseModel` / `HdBabyHorseModel`
   handed to the super ctor; no per-entity model swap).


## The horse's body (speed / health / jump / size)

**`wiki/horse-body.html` is the source of truth.** Summary:

- `HorseTraits.resolve(genotype[, epigenome][, healthGenetics])` in
  `common/trait/` returns a
  `Traits(speed, health, jump, scale, conditions)` - a **pure function of the
  genome**, with no entity state and no randomness beyond the stored epigenetic
  seeds. Baselines
  `0.1875 / 22.0 / 0.5 / 1.0`; `MIN_HEALTH = 1.0` so a genetic health value
  never resolves to zero.
- **A trait may be epigenetic** (`EpigeneticTraitContribution`, added
  2026-09-04 for the magical size locus): the gene is handed an
  `AlleleRandomness` offering `expressed()` (the copy the horse shows - right
  wherever one locus gives one result) and `copy(slot)` (**for a codominant
  gene**, where both copies contribute and asking for the expressed one would
  count one allele twice and the other never). Both are `SeededRng`s on stored,
  heritable copy seeds, exactly as the coat's are, so *how much* can vary between
  horses with identical alleles while staying deterministic and heritable. `resolve(genotype)` with no epigenome answers with the **midpoint**
  (`MidpointRng`) - the honest answer to a question about a genotype rather than
  a horse. `HorseRecord.traits()` and `HorseRecords.traitsOf` pass the record's
  own epigenome.
- **Scale has two stages and only one is clamped.** `MIN_SCALE`/`MAX_SCALE`
  (0.45-1.75) keep the natural loci honest; `TraitBuilder.multiplyScaleUnclamped`
  applies *after* that clamp and is bounded only by `MAGICAL_MIN_SCALE` /
  `MAGICAL_MAX_SCALE` (0.1-10). They compose right: a magically enormous pony is
  still smaller than a magically enormous draught horse.
- **Nothing is stored.** `HorseRecord` has no `speed` / `health` field, the same
  way it has no `sex` field; `record.traits()` resolves on demand. The old
  `HorseStats.rollFoalStat` (a uniform draw in `[0.75*min, 1.5*max]`) is
  **deleted**, and with it the last non-genetic randomness on a horse.
- A gene contributes by additionally implementing `TraitContribution` (or
  `HealthContribution`, the marker the config toggle reads). Additions are
  applied before scale multipliers, so resolution is order-independent.
- `HorseRecords.traitsOf(horse|record)` resolves honouring `ServerConfig`;
  `HorseRecords.applyTraitsToEntity(horse, traits, fullHeal)` writes the
  attribute **base** values for `MOVEMENT_SPEED`, `MAX_HEALTH`, `JUMP_STRENGTH`
  and **`SCALE`** (vanilla scales the model *and* the hitbox from it).
  `fullHeal` true = set current HP to the new max (newborn); false = only clamp
  HP down (reload, so no free heal).
- **It runs on every horse join**, not just at birth: vanilla has just
  randomised the entity's speed/health/jump, and this overwrites it with what
  the alleles say. Resolving rather than storing is what lets a re-tuned gene -
  or a change to `health.mode` - reach horses that already exist.
- `ParentStats` survives, now built from the two parents' *resolved* traits at
  birth. It stays a stored snapshot because a parent can be dead, sold or
  forgotten by the ancestry DB by the time anyone looks.

## Riding through water (`HorseWaterRidingHandler`)

Vanilla already floats a **ridden** horse at the water surface (horses are in
the `minecraft:can_float_while_ridden` entity tag) and there is **no**
water-triggered auto-dismount - the only dismount is sneak
(`Player#wantsToStopRiding` = `isShiftKeyDown`). What's missing is usable
speed: in-water `travelInWater` gives ~0.02 b/t.

The handler, on `EntityTickEvent.Post` for a tamed, player-ridden
`AbstractHorse` that `isInWater()` and `isLocalInstanceAuthoritative()`
(so it runs on the controlling client, where movement is simulated, and on
the server for non-player control): adds a small upward `deltaMovement` when
the horse is submerged (keeps the rider's head out), and blends horizontal
`deltaMovement` toward a capped `WATER_RIDE_SPEED` (0.09 b/t) in the
direction the rider steers (`rider.zza`/`xxa`, rotated the way vanilla's
`moveRelative` does). **Feel is a first guess - unverified in-game.**

## 26.1.2 API notes (what the port actually required)

This SDK is further from mainline 1.21.x than the version numbers suggest.

- **`ResourceLocation` is now `net.minecraft.resources.Identifier`** - same
  surface (`withDefaultNamespace`, `fromNamespaceAndPath`), different name.
- **Horse classes moved** `net.minecraft.world.entity.animal.horse` ->
  `...animal.equine` (`Horse`, `Variant`, `Markings`, `AbstractHorse`).
- **`HorseRenderer` is `final`.** `GeneticHorseRenderer` extends
  `AbstractHorseRenderer<Horse, HorseRenderState, HorseModel>` and copies
  vanilla `HorseRenderer`'s constructor body verbatim. Render-state stays the
  vanilla `HorseRenderState` so the copied layers type-check;
  `createRenderState()` covariantly returns the `GeneticHorseRenderState`
  subclass and every instance really is that subclass.
- **`getTextureLocation(S)` is still the texture hook** - the `submit()` /
  `SubmitNodeCollector` split didn't swallow it for entities.
- **`EventBusSubscriber` dropped `bus()` / `Bus`.** `IModBusEvent` subtypes
  auto-route to the mod bus, everything else to the game bus.
- **`AttachmentType.Builder#serialize` takes a `MapCodec`**, not `Codec`.
- **`AttachmentType.builder(Function<IAttachmentHolder, T>)`** overload lets
  the default read the holder (e.g. `entity.getUUID()`).
- **`KeyMapping` conflict-context ctor takes a `KeyMapping.Category`** (a
  record keyed by an `Identifier`), not a lang key. Debug keybind reuses
  `KeyMapping.Category.MISC`.
- **`EntityType#create` needs an `EntitySpawnReason`** -
  `create(level, EntitySpawnReason.COMMAND)`.
- **`DynamicTexture(NativeImage)` is gone** - use
  `DynamicTexture(Supplier<String> label, NativeImage image)`; it takes
  ownership and closes the image (don't also close it). `NativeImage.getPixel
  (x,y)` / `setPixel(x,y,argb)` are **ARGB** (`ARGB.fromABGR`/`toABGR` under
  the hood); `getWidth()` / `getHeight()`. `GeneticCoatTextureFactory` builds
  a fresh `new NativeImage(128,128,false)`, `setPixel`s the composer's `int[]`
  in, and hands it to the `DynamicTexture`.
- **Serverbound packets**: `PacketDistributor.sendToServer` ->
  `net.neoforged.neoforge.client.network.ClientPacketDistributor`.
  `sendToPlayer` / `sendToPlayersTrackingEntity` stayed on `PacketDistributor`.
- **Reach the server** via `((ServerLevel) player.level()).getServer()` or
  `player.level().getServer()` (`Level#getServer()` exists;
  `ServerPlayer#getServer()` was the one that didn't resolve).
- **`dimension_type` JSON schema changed heavily.** Gone: `ultrawarm`,
  `natural`, `piglin_safe`, `respawn_anchor_works`, `bed_works`, `has_raids`,
  `fixed_time`, `effects`. New **required**: `has_ender_dragon_fight` (bool).
  `effects` -> `skybox` (`"none"` / `"overworld"` / `"end"`). Time-of-day
  moved to a WorldClock/Timeline registry (permanent noon would need
  `default_clock` / `timelines`). A stale `dimension_type` JSON fails
  `RegistryDataLoader` and takes down the client (`ReportedException` ->
  `Stopping!`) the moment a world is created - reads as "singleplayer is
  broken". `data/horsegenetics/dimension_type/debug_pens.json` is
  `min_y: 0, height/logical_height: 512` (512 for random-Y plot headroom).
- **Flat-generator `dimension/debug_pens.json`** schema unchanged;
  `"structure_overrides": []` disables villages/etc. It's now `the_void`
  biome + a single `air` layer (generates nothing - `DebugPenManager` builds
  the floor itself).
- **`com.mojang.authlib.GameProfile` is a record** - `profile.name()` /
  `profile.id()`, no `getName()`.
- **Attributes are `Holder<Attribute>`.** `LivingEntity#getAttributeValue(
  Holder<Attribute>)` -> double; `#getAttribute(Holder<Attribute>)` ->
  `@Nullable AttributeInstance` with `setBaseValue(double)`. `Attributes.
  MOVEMENT_SPEED` / `MAX_HEALTH` are holders. Horses register both by default
  (`createBaseHorseAttributes`); `WATER_MOVEMENT_EFFICIENCY` is **not** on
  horses, so don't rely on it.
- **Ride/movement gates**: `Entity#isLocalInstanceAuthoritative()` (public
  final) is the "this side simulates the movement" check - true on the
  controlling client for a player-ridden mob, on the server otherwise.
  `Entity#getControllingPassenger()` -> `@Nullable LivingEntity`.
  `Mob#moveTo(...)` was renamed **`snapTo(...)`** (same overloads; y arg must
  be `double`). No water auto-dismount anywhere in `Entity` / `LivingEntity`
  / `Player`.
- **The GUI layer is retained-mode.** `GuiGraphics` -> `net.minecraft.client
  .gui.GuiGraphicsExtractor`; screens/widgets override
  `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float)`
  instead of `render(...)`; text `graphics.text(font, comp, x, y, argb)` /
  `centeredText(...)`; rects `graphics.fill(x0,y0,x1,y1,argb)`; textures
  `graphics.blit(Identifier, x0,y0,x1,y1, u0,u1,v0,v1)`. Mouse:
  `mouseClicked(MouseButtonEvent event, boolean doubleClick)` (`event.x()` /
  `.y()` / `.button()`), same for `mouseReleased` / `mouseDragged`. NeoForge
  `ScreenEvent.Init.Post#addListener` and the `ScreenEvent.Render.*` events
  (each `#getGuiGraphics()`) still bolt widgets/overlays onto vanilla screens.
  `AbstractContainerScreen#getGuiLeft()` / `getGuiTop()` are
  deprecated-for-removal but still work (used by `HorseScreenHooks`).
  - **`ScreenEvent.Render` timing**: `Pre` fires *before* the dimmed backdrop,
    `Background` after the backdrop but before the widget stratum, `Post` after
    everything. To draw a panel *behind* your own buttons but *over* the
    backdrop, use `Background` (that's what `HorseScreenHooks` learned the hard
    way - `Pre` gets painted over, `Post` covers the buttons).
  - **Swallowing a keybind while a text field is focused**:
    `ScreenEvent.KeyPressed.Pre` (`getKeyCode()` / `getKeyEvent()`), cancel it
    so the screen's `keyInventory` handler doesn't close the GUI; forward the
    key to the box yourself for backspace/arrows. `CharacterTyped` is a
    separate event, so letters still type (`HorseScreenHooks.onKeyPressed`).
  - **Scaling GUI text**: `g.pose()` is a `Matrix3x2fStack` -
    `pushMatrix()` / `translate(x,y)` / `scale(s)` / `popMatrix()` around a
    `g.text(font, comp, 0, 0, argb)` call scales it about `(x,y)`
    (`FamilyTreeScreen.drawFitted`). `g.enableScissor(x0,y0,x1,y1)` /
    `disableScissor()` clip everything including `g.entity(...)`.
  - **A live 3D entity in a screen**: build the render state yourself
    (`Minecraft.getEntityRenderDispatcher().getRenderer(e).createRenderState(e,
    1f)`, then `state.shadowPieces.clear()`, poke `LivingEntityRenderState`
    angles) and `g.entity(state, scale, translation, rot, camRot, x0,y0,x1,y1)`
    - see `FamilyTreeScreen.drawHorseModel`, which uses a throwaway client-only
    `Horse` and injects the coat onto `GeneticHorseRenderState` directly (no
    `ClientCoatCache` pollution).
- **`SavedData` is gutted** to a `dirty` flag - no `save(CompoundTag)`.
  Persistence is `SavedDataType<T>(Identifier id, Supplier<T> ctor,
  Codec<T> codec)` via `SavedDataStorage#computeIfAbsent`
  (`ServerLevel#getDataStorage()` per level,
  `MinecraftServer#getDataStorage()` server-global - the ancestry DB uses the
  server-global one, so it lands in `<save>/data/` = per world).
- **Block registry**: `DeferredRegister.createBlocks(modid)` ->
  `.registerBlock(name, Function<Properties, ? extends B>,
  Supplier<Properties>)` - third arg is a **`Supplier<Properties>`**, not a
  bare `Properties` -> `DeferredBlock<B>`. `ModBlocks.register(modEventBus)`
  and `ModBlockEntities.register(modEventBus)` are called from the
  `HorseGenetics` constructor. No `BlockItem` for `hay_portal` (placed only
  by code).
- **`hay_portal` block entity + renderer**: `HayPortalBlock extends
  BaseEntityBlock`, `getRenderShape` -> `RenderShape.INVISIBLE`,
  `newBlockEntity` -> `HayPortalBlockEntity extends TheEndPortalBlockEntity`
  (subclassed only so `AbstractEndPortalRenderer`'s `T extends
  TheEndPortalBlockEntity` bound is satisfied; its protected ctor is callable
  from a subclass). `ModBlockEntities` registers the type via `new
  BlockEntityType<>(HayPortalBlockEntity::new, ModBlocks.HAY_PORTAL.get())`.
  `HayPortalRenderer extends AbstractEndPortalRenderer<HayPortalBlockEntity,
  HayPortalRenderState>` **for the plumbing only** (`facesToShow` population +
  BER registration) - `submit(...)` no longer touches the End-portal shader.
  `HayPortalRenderState extends EndPortalRenderState` adds `axis` (from the
  block's `AXIS` state), set in an `extractRenderState` override.
  - Geometry: `AXIS` (`HORIZONTAL_AXIS`) is the axis the portal *plane* runs
    along (vanilla nether convention). The slab is thin along the **other**
    axis (`thinAxis` = X<->Z swap), inset to `SLAB_MIN`..`SLAB_MAX` (0.25..0.75)
    there, and only the two faces with `dir.getAxis() == thinAxis` are drawn -
    so it faces the player, not edge-on. (Getting this backwards was the
    "portal is sideways" bug.) `emitFace(dir, from, to, ...)`, winding
    `right = up x normal`, corners BL/BR/TR/TL.
  - Two passes per face: **opaque black** (`RenderTypes.entitySolid(TEXTURE)`,
    colour 0,0,0 - `ENTITY_SOLID` has no alpha discard + writes depth, so the
    sky / clouds / water can't show through) then the **animated swirl**
    (`RenderTypes.entityTranslucentEmissive(TEXTURE, false)`, colour white,
    full-bright `0x00F000F0`, `OverlayTexture.NO_OVERLAY`).
  - Texture `assets/horsegenetics/textures/block/hay_portal.png` - 64-wide
    vertical strip of **36** 64x64 frames. Frame chosen by V offset;
    `HayPortalClientAnim.currentFrame(36)` advances it by wall-clock time at a
    speed that ramps **12 fps -> 48 fps** as a client-estimated "charge" (local
    player standing in a `hay_portal` block, ramp ~10 s / decay ~2 s) rises.
    The `.png.mcmeta` (36 frame entries) is **not** read - kept for docs / a
    possible future atlas move.
  - `HayPortalBlockEntity#shouldRenderFace` renders a face only if the
    neighbour isn't another `HayPortalBlock` (multi-block portal shows just its
    outer shell). Registered in `ClientSetup`. Blockstate JSON: 2 axis variants
    -> one model that only sets a `particle` texture.
  - **To restore the End-portal starfield:** put back `submitCube(
    state.facesToShow, RenderTypes.endPortal(), poseStack, submitNodeCollector)`
    in `submit` (git history).
- **The walk animation ignores `Attributes.SCALE`, and that is a trap for any
  mod that changes it.** `LivingEntity.calculateEntityAnimation` feeds the raw
  world distance moved into `updateWalkAnimation`, which is
  `min(distance * 4, 1)` into `walkAnimation.update(target, 0.4F, isBaby() ?
  3.0F : 1.0F)`. That third argument is the *only* size compensation in the
  whole path - a foal's legs cycle 3x faster because they are short - and
  nothing anywhere consults the scale attribute. `LivingEntityRenderer` then
  scales the model by `state.scale` (= `entity.getScale()`, the SCALE attribute
  alone; `getAgeScale()` is separate). So a scaled entity swings ordinary-rate
  legs over ordinary ground with longer limbs, and its feet slide. The fix
  needs no mixin: divide `state.walkAnimationPos` in the renderer's
  `extractRenderState` after `super` - the phase is a monotonic accumulator, so
  scaling it after the fact is identical to having accumulated it slower. Leave
  `walkAnimationSpeed` alone; it is an amplitude multiplier on an angle, and an
  angle already scales with the model. See
  `GeneticHorseRenderer.stretchGaitToSize`.
- **Mojang un-typo'd** `BlockBehaviour.Properties.noCollission()` ->
  **`noCollision()`**.
- **Spawn point**: no `Level#getSharedSpawnPos()`. Use
  `level.getRespawnData().pos()` (`LevelData.RespawnData` record:
  `globalPos()` / `yaw()` / `pitch()` + a `pos()` convenience).
- **Cross-dimension teleport for any entity**: `Entity#teleportTo(ServerLevel
  level, double x,y,z, Set<Relative>, float yRot, float xRot, boolean
  resetCamera)` - pulled up to `Entity`. `Set.of()` target-types to
  `Set<Relative>`. *Verified 2026-08-30 for non-player entities (horses come
  back through the return portal).*
- **Leash**: `Mob implements Leashable`; `isLeashed()`, `getLeashHolder()`
  (-> `Entity`), `dropLeash()` (drops the lead item), `removeLeash()` (no
  drop). *Verified 2026-09-01: the roped-horse portal shortcut sends the horse
  through **and** drops the lead.*
- **Event hooks used this pass**: `LivingIncomingDamageEvent` (cancelable,
  pre-mitigation - horse invulnerability); `EntityTickEvent.Post` (portal
  dwell timers, water riding, tamer tracking);
  `PlayerInteractEvent.RightClickBlock` (`getPos()`, `getItemStack()`,
  `setCanceled` + `setCancellationResult(InteractionResult)`);
  `PlayerEvent.PlayerChangedDimensionEvent` (`getFrom()`/`getTo()` ->
  `ResourceKey<Level>`), `PlayerLoggedIn/OutEvent`.
- **`PlayerInteractEvent.EntityInteract` fires on both sides.** For
  interactions that vanilla would otherwise turn into a mount (any item on a
  tamed horse), you must `setCanceled(true)` **on the client too**, or the
  client predicts the mount and rubber-bands. Do the state mutation
  server-only, cancel on both. (`HorseInteractionHandler` clock/stick.)
- **Standing signs** (not currently used - kept for reference):
  `Blocks.OAK_SIGN` + `StandingSignBlock.ROTATION` (0-15, from
  `RotationSegment.convertToSegment(...)`); text via `SignBlockEntity#
  getFrontText()` -> `SignText#setMessage(line, Component)` (returns a new
  one) -> `setText(text, true)` + `setChanged()` + `sendBlockUpdated`.
- **Wall signs, placed from code** (the stall sign does this):
  `Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, dir)`
  (`FACING` is a horizontal `EnumProperty<Direction>`), `level.setBlock(pos,
  state, Block.UPDATE_ALL)`, then `((SignBlockEntity) level.getBlockEntity(pos))
  .updateText(t -> t.setMessage(line, Component), /*front=*/true)` -
  `updateText` takes a `UnaryOperator<SignText>` and `SignText.setMessage`
  returns a new `SignText` (chain the calls).
- **`BreakBlockEvent`** (`net.neoforged.neoforge.event.level.block`): fires
  *before* removal, so `event.getState()` is still the block being broken;
  `getLevel()` -> `LevelAccessor`, `getPos()`, `getPlayer()`. Cancelable.
- **`SavedDataType<T>(Identifier, Supplier<T>, Codec<T>)`** + `server
  .getDataStorage().computeIfAbsent(TYPE)` is the server-global SavedData
  pattern (`HorseAncestryData`, `StallData`). `ResourceKey<Level>` round-trips
  in a codec via `ResourceKey.codec(Registries.DIMENSION)`.
- **`ServerLevel#sendParticles(ServerPlayer, T, boolean overrideLimiter,
  boolean alwaysRender, x, y, z, int count, dx, dy, dz, double speed)`** -
  the per-player overload; `alwaysRender = true` defeats distance culling
  (used for the stall debug outline).

## Build setup notes

- `common/build.gradle.kts` declares its own `repositories { mavenCentral() }`
  and `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` - both
  required under Gradle 9.
- Mod metadata is `src/main/resources/META-INF/neoforge.mods.toml`;
  `minecraft` dependency `versionRange = "[26.1.2,)"` (the MDK's
  `[1.26.1,1.27)` does not match the version string `26.1.2` and makes FML
  refuse the mod). There's a stray un-packaged duplicate at
  `src/main/neoforge.mods.toml` (wrong dir, from a `git mv`) - delete it or
  move it under `resources/META-INF/`; don't let the two drift.
- **Config**: `ClientConfig` (`net.neoforged.neoforge.common.ModConfigSpec`,
  `ModConfig.Type.CLIENT`) registered from the `HorseGenetics(IEventBus,
  ModContainer)` constructor. One key so far: `familyTree.scrollBar` (default
  `false` = shrink the Family Tree to fit; `true` = full-size + scroll). Read
  via `ClientConfig.familyTreeScrollBar()` which swallows the not-yet-loaded
  `IllegalStateException`.
- **`.gitignore`** (repo root) covers `build/`, `.gradle/`, `.idea/`, `**/run/*`,
  any `saves/`, and the JVM's `hs_err_pid*.log` droppings - the tracked tree is
  down from 2 446 files to 148 (source, docs, the gradle wrapper). The **one
  deliberate exception** is `run/config/fml.toml`, kept tracked by a
  `!**/run/config/fml.toml` negation because it carries this laptop's
  `earlyWindowControl = false` launch fix; the negation needs the
  `!**/run/config/` + `**/run/config/*` pair above it to work, so don't collapse
  those three lines.
- `generate_file_list.py` and the README file-tree block were removed this
  session (README is user-facing now).

## Running the game

`build` only assembles the jar. `runClient` / `runServer` launch MC
`26.1.2.100` with the mod. IntelliJ Gradle sync generates the run configs.

Two machine-specific launch blockers on this dev laptop (NVIDIA RTX 3050 Ti +
AMD integrated, AMD driver from 2023):

- **JVM hard-crash (`EXCEPTION_ACCESS_VIOLATION` in `atio6axx.dll`) at
  `glfwCreateWindow`** - the process ran on the old AMD integrated driver.
  Fix: Windows per-app GPU preference pinning `java.exe` / `javaw.exe` to
  "High performance" (`HKCU\Software\Microsoft\DirectX\UserGpuPreferences`,
  `GpuPreference=2;`). Durable; a driver update would also fix it.
- **FML early-loading splash window** hits the same crash one step earlier.
  `run/config/fml.toml` has `earlyWindowControl = false`. `run/` is otherwise
  git-ignored, but **that one file is deliberately still tracked** (see "Build
  setup notes") so a `git clean` can't reset it to `true`.

### The "Spawn Test Horse World" button cleans up after itself

`client/DebugTitleScreenButton` (dev only) creates a throwaway creative world
named by `DebugTestWorldCleanup.newDirectoryName()` = `test_horse_<millis>`.
`client/DebugTestWorldCleanup` deletes those worlds again, in two sweeps over
`saves/`, both matching **only** `test_horse_` + digits so a hand-made world is
never a candidate:

- **`ClientStoppedEvent`** - the normal path. It's posted on the game bus from
  `Minecraft#destroy` *after* the disconnect has halted the integrated server
  and spun waiting for it to finish saving, and after `close()` - so nothing
  still holds the save folder, and it's the last hook before `System.exit`.
  (`ClientStoppingEvent` would be too early: the server is still running.)
- **`ClientStartedEvent`** - the safety net, for a crash, a `taskkill`, or a
  delete Windows refused because a handle lingered. Runs before anything opens
  a world.

Both are `ClientLifecycleEvent` subclasses, which do **not** implement
`IModBusEvent`, so `@EventBusSubscriber(value = Dist.CLIENT)` routes them to
the game bus. A failed delete only warns - the next launch retries.

## The horse dimension (`server/DebugPenManager` + portals)

It is a **normal feature** now, not debug-only - reachable by hay-bale portal
in any build. Only the **F6 shortcut** is dev-gated (`DebugKeyBindings`
registers the keybind only if `!FMLEnvironment.isProduction()`, and
`ModNetworking`'s `RequestDebugPensPayload` handler re-checks). **F7** is the
other dev keybind - `key.horsegenetics.show_stalls`, flashes the particle
outline of nearby stalls (`RequestStallHighlightPayload` -> `StallDebug`, also
re-checked against `isProduction()`). The class /
dimension / `ResourceKey` names still say "debug" (`DebugPenManager`,
`DEBUG_LEVEL`, `horsegenetics:debug_pens`) - a rename is deferred to avoid
breaking existing save data mid-session; treat the names as legacy.

### The dimension is a void; each visit is a private, disposable instance

`dimension/debug_pens.json` generates **nothing** (`the_void` biome, one
`air` layer). Every visit gets its own `DebugPenManager.Plot`: a corridor
built at a unique world X (`PLOT_SPACING_X` = the corridor + 1 000 = **8 007**
blocks apart, X slots recycled via a free-list) and a **fixed Y**
(`PLOT_BASE_Y` = 128). Plots never share chunks - "two players never land in
the same place" holds on a server with no real per-player-dimension work.

`PLOTS` is `Map<UUID player, Plot>`, strictly 1:1 - every `enter()` makes a
new plot and tears down that player's previous one, so a revisit always
regenerates and no live horse left behind survives.

**Leaving clears entities, not blocks.** The *geometry* is fixed (`PEN_COUNT`
pens, fixed `PLOT_BASE_Y`), so an X slot handed back to the free list is rebuilt
with byte-identical geometry on top of the old one. The *contents* are random
now and differ every visit, but every pen a player can reach is rebuilt from
index 0 upward as they walk - sign and horses together - so a stale genotype is
never on show, and `buildPen` clears untamed leftovers before it stocks. Only
pens past the new player's frontier hold anything old, and walking there
rebuilds them. `tearDown` is therefore still O(entities), not O(blocks walked);
its AABB covers the **whole** X slot (not just what this visit built), or a slot
where a previous visitor walked further would accumulate horses forever. It also
`HorseAncestryData.forget(...)`s each discarded horse, or every visit would
leave hundreds of throwaway pen records in the save forever. (A record that
*references* a forgotten horse as a parent is left alone - `ancestorsOf`
already skips ancestors it can't find, so a foal bred in the dimension and
taken home keeps working with a missing parent node.)

- `teleportAndGenerate(player)` (F6) -> `enter(player, player's current dim,
  player's current pos)`.
- `enter(player, returnDim, returnPos)` -> allocate plot, build lookahead,
  build the return portal, teleport to `(originX + 3.5, baseY + 1, 0.5)`
  facing +X, drop a **paper** in the first free hotbar slot.
- `leave(server, playerUUID)` -> `tearDown`: discard non-player entities in
  the plot AABB, forget their ancestry records, return the X slot. **Blocks are
  left standing** (see above). Fired from
  `PortalEventHandler.onChangedDimension` (from == DEBUG_LEVEL) and
  `onLogout`.
- **Leaving takes your horses.** As a player's exit-portal dwell hits tick 1
  they get a chat warning ("anything left behind is lost forever, tamed
  horses come with you"). At teleport, `teleportThroughPortal` calls
  `DebugPenManager.evacuateTamedHorses(debug, plot, onlyOwner, dest,
  destPos)` - `onlyOwner = player.getUUID()` if `portalLevel.players()` still
  has someone else, `null` (all tamed horses) if this player is the last one
  out. Owner match is `horse.getOwnerReference().getUUID()`. Untamed / other
  players' horses are then discarded by `tearDown`. Each evacuated horse is
  **dropped in the air** ~2 blocks above the return portal, spread on a 3-wide
  grid, with **10 s of invulnerability** so the short fall can't hurt it
  (`HorsePortalManager.placeReturningHorse` ->
  `PortalEventHandler.grantReturnInvulnerability`: sets `setInvulnerable(true)`,
  parks the id in `RETURN_INVULN` + `COOLDOWN` for `RETURN_INVULN_TICKS`, then
  `onEntityTick` clears it). No terrain is carved. A lone horse that dwells in
  the exit portal itself gets the same treatment.
- `PortalEventHandler.onLogin` bounces a player who logs in inside the dim
  with no live plot (server restart) to the overworld spawn.
- `DebugPenTickHandler` (`PlayerTickEvent.Post`, **not** dev-gated anymore)
  -> `ensureGeneratedAheadOfPlayer(player)` extends the corridor as you walk.

### The pens: one random showcase genotype each (`common/genetics/ShowcaseGenotypes`)

The dimension is a corridor of **2 000 pens**, each holding a mare and a
stallion rolled from **one random genotype**. `DebugPenManager.PEN_COUNT` is a
fixed, arbitrary number - **not** derived from anything - which is the whole
point of the revert: the corridor no longer grows when a gene is added.

- **`ShowcaseGenotypes.random(rng)`** (pure `common/`, unit-tested) is the draw:
  an ordinary `Genotype.random` founder roll with a **floor** under it.
  - The honest wild distribution is mostly plain horses - every founder table is
    weighted hard toward its baseline - which is correct for a wild spawn and
    useless for a corridor whose job is to show what the genes do.
  - So: **at least one natural coat gene beyond extension and agouti is
    expressing**, always, and with `MAGICAL_CHANCE` = **0.5** a magical gene is
    too. A draw that already clears the floor is left exactly as it fell; only
    an all-baseline draw gets a gene forced in.
  - Forcing picks a random combination of a random candidate gene and keeps only
    one that **actually expresses in this genotype** (`Genotype.shows`), so a
    chestnut is never handed an agouti it will not paint.
  - A combination that **`masks`** neither counts toward the floor nor is ever
    forced. That is load-bearing: a quarter of all founders carry the diagnostic
    test gene, which paints flat over everything, so counting it would quietly
    exempt a quarter of the corridor from the guarantee.
  - Candidates are derived from the registry (`naturalOrder()` / `magicalOrder()`
    filtered to `affectsCoat()`, minus extension and agouti), so a drop-in gene
    joins the pool on its own.
  - **The one honest gap**: the floor guarantees a gene *expresses*, not that it
    is *perceptible*. Mushroom on a black horse is a real expression that looks
    like nothing. Nothing in the model can currently answer "would a player see
    this", so this is accepted rather than special-cased.
  - It is a **founder path** - randomness here is legitimate because these horses
    have no parents. Nothing else calls it; a wild spawn is still a wild spawn.
- **`Genotype.with(AllelePair)`** is the new founder-only setter it needs - one
  locus replaced, the rest untouched, the same shape as `withSex` (which now
  delegates to it).
- **Both horses in a pen share the genotype** but not the epigenome, so they're
  two examples rather than two copies.
- **`buildPen` always stocks fresh.** A pen is built exactly once per plot, so
  anything already standing in it belongs to a *previous* occupant of that
  recycled X slot and has nothing to do with the sign just written. Untamed
  horses there are discarded (and forgotten from the ancestry DB) before the
  pair spawns; tamed ones are left alone, since a player can have ridden one
  ahead of the build frontier.
- **Signs** (`placeSign`, waxed standing oak, same text on both faces):
  - per pen, on the road one block out from the wall and **to the right of the
    gate** as you face the pen (`roadFacing().getOpposite().getClockWise()`, so
    the two sides of the road mirror): line 0 = `#<1-based pen number>`, then
    **`GeneCodeDisplay.shortForm`** - the same compact form the info panel and
    paper dump use - greedily wrapped over the remaining 3 lines by
    `GeneCodeDisplay.wrap(genotype, 3, 15)`. `wrap` deliberately overflows its
    **last** line rather than dropping a gene, so a busy horse still reads very
    wide; `ShowcaseGenotypesTest` pins that nothing is ever lost and that the
    overflow doesn't grow past 200 chars. Random labels overflow far less often
    than the exhaustive catalogue ones did, so this is left alone.
  - `originX + 4` (three blocks in front of the return portal), facing west at
    the player's spawn: `Horse Pens / 2,000 pens / random genome / mare +
    stallion`. The old catalogue tally (`Genotypes / <totalGenotypes()> /
    <size()> distinct / showing <n>`) is gone - none of it is true of a random
    corridor.
- **Length**: `PEN_COUNT` = **2 000**, so `LAST_SEGMENT_INDEX` = 999 and the
  corridor is **~7 000 blocks** - short enough to fly to the end of, and
  `PLOT_SPACING_X` (the corridor + 1 000) is back down to **8 007**.
  `ensureBuiltUpToIndex` clamps to it and calls `buildEndCap` (the mirror of
  `buildStartCap`) on the last segment. Pens are still built lazily as you walk.
- **`GenotypeCatalog` is untouched and still used** - by the tests, and by
  whatever punnett display gets built. It just no longer drives the dimension.
  `size()` is **462 422 019** and `totalGenotypes()`
  **81 687 059 589 193 477 171 200 000**; see "the genetics model" for what those
  numbers mean. (`size()` last moved with the magical utility genes and the
  particle locus did not touch it, having only one catalogue entry;
  `totalGenotypes()` is a `BigInteger` since the particle locus, and the figure
  recorded here before was a wrapped `long`. The corridor moved with neither,
  which is the whole point of the revert.)

### Layout (`DebugPenManager`)

Geometry is relative to `plot.originX` (+X) and `plot.baseY` (grass surface).
Cross-section (Z) from centre out:

```
road (gravel z -3..3) | pen 6x20 (brick walls) | gravel strip + glowstone above (z 24) | oak-plank wood wall (z 25) | bedrock core (z 26) | VOID
```

- **Floor**: laid by `DebugPenManager` (generator makes none) -
  bedrock/dirt/dirt/grass at `baseY-3..baseY`, only for `z` in [-26, 26].
  Beyond the bedrock core: open void.
- **Gravel road** down the centre, continuous along X.
- **One pen per side**: `PEN_LEN_X` = **6**, `PEN_DEPTH_Z` = 20. Perimeter is
  `Blocks.BRICK_WALL`; the road-side edge has a **two-wide** oak-fence-gate
  opening (`gateX`, `gateX+1` where `gateX = x0 + PEN_LEN_X/2 - 1`) - a
  1-wide gate lets horses escape. Torches on the four corner wall posts
  (corners get `up=true`, so a solid top). The two **gate-side interior
  corners** hold amenities, one block in from the road-side wall
  (`zGateInner = zRoad + signum(zBack-zRoad)`) and **sunk to `floorY-1`** so
  their tops are flush with the grass (a full block at floorY was a step the
  horses hopped the 1-high wall from): a full `WATER_CAULDRON` (`LEVEL=3`) at
  `x0+1`, a `HAY_BLOCK` at `xMax-1`.
- **E/W walls** (behind the pens), flush against the pen back edge
  (`PEN_FAR_Z` = ±23), no grass gap: `GRAVEL_STRIP_Z` = ±24 (gravel, with a
  glowstone line directly above at `baseY+10`), `WALL_PLANK_Z` = ±25 (a
  **single** oak-plank wood wall, `baseY .. baseY+9`), `WALL_BEDROCK_Z` = ±26
  (bedrock core, `baseY-3 .. baseY+9`).
- **Wall behind the return portal** (`buildStartCap`): pushed 2 blocks back from
  `originX-1` to `originX-3`, layered like the E/W walls. Bedrock core at
  `originX-3` (`baseY-3 .. baseY+9`); a single oak-plank wood wall at
  `originX-2` (`baseY .. baseY+9`) on a bedrock/dirt base; a gravel **floor
  strip** at `originX-1` `baseY` (also on a bedrock/dirt base) with a glowstone
  line floating at `baseY+10`. The little nook between it and the portal
  (`originX`, `originX-1` above the floor) is open air. **Return portal** (hay
  frame + `hay_portal`, axis Z) at `originX + 1`. (An earlier version made
  `originX-1` a full-height *gravel column* with no base - it fell into the void
  and left a gap; hence everything now stands on bedrock/dirt.)
- Pens repeat every `PERIOD` (= `PEN_LEN_X + 1` = 7); `LOOKAHEAD_PENS` = 30
  built up front.
- Each pen: **one mare + one stallion** (`spawnHorse(..., Sex, geneticCode)`
  pre-sets the founder record so `onHorseJoin` doesn't re-roll sex *or*
  genotype; `newFounder` also copies the entity's speed/health onto the
  record).
- Horses in `DEBUG_LEVEL` **take no damage**
  (`HorseGeneticsEventHandler.noHorseDamageInDebugDimension` cancels
  `LivingIncomingDamageEvent` for any `AbstractHorse`).
- `DEBUG_LEVEL` terrain is **read-only**: `noBlockBreakInDebugDimension`
  (`BreakBlockEvent`) and `noBlockPlaceInDebugDimension`
  (`BlockEvent.EntityPlaceEvent`, which also covers `EntityMultiPlaceEvent`)
  both cancel in that dimension. Bucket/fluid placement isn't covered yet.
- `HorseInteractionHandler` in `DEBUG_LEVEL`: **stick** tames an untamed
  horse, **clock** ages a foal to adult (`setAge(0)`). *Verified 2026-09-01:
  the clock ages a tamed foal without the interaction falling through to a
  mount - see the `EntityInteract`-fires-on-both-sides note above.*
- Non-horse mobs refused entry by
  `HorseGeneticsEventHandler.keepDebugDimensionHorsesOnly`.

Name word tables (`common/src/main/resources/horsegenetics/names/`) must be
on the server runtime classpath - `HorseRecords`' static
`HorseNameGenerator.fromResources()` throws on class-load if missing.

## Hay-bale portals (`block/HayPortalBlock`, `server/HorsePortalManager`, `server/PortalEventHandler`)

- **`horsegenetics:hay_portal`** - cosmetic `BaseEntityBlock`: `noCollision`,
  `noOcclusion`, indestructible (`strength(-1, 3600000)`), light 11, no loot,
  `PushReaction.DESTROY`, `AXIS` state (now also drives the render slab
  orientation), `RenderShape.INVISIBLE`. Visual is the mod's own **animated
  texture** (`textures/block/hay_portal.png`, 36-frame vertical strip) drawn by
  `HayPortalRenderer` as a **half-block-thick opaque slab** centred in the
  block, frame rate ramping **12->48 fps** via `HayPortalClientAnim` as you
  dwell (see the API note for mechanics / changing frames / restoring the
  End-portal starfield).
- **Lighting one**: build a hay-bale frame (nether-portal inner sizes: 2..21
  wide, 3..21 tall, **vertical**) **outside** the horse dim, right-click a
  frame block with a **golden carrot**. `HorsePortalManager.findFrame` tries
  **every** in-plane air-neighbour of the clicked hay as a flood-fill seed
  (the old code took the first and broke on corner / far-post clicks -
  suspected cause of the user's "did nothing"), floods the enclosed air in
  the frame plane (axis X then Z), requires a filled rectangle fully bounded
  by hay, fills it with `hay_portal`. Golden carrot consumed; the player gets
  a chat line on success **or** failure (with the size rule).
- **Dwell teleport**: `PortalEventHandler.onEntityTick` counts ticks a
  player/horse stands in a `hay_portal` block (checks feet + `y+0.9`).
  **Player 200 t (10 s)**, **horse 60 t (3 s)** -> `teleportThroughPortal`.
  A 100-tick cooldown blocks instant re-trigger (returning horses are parked
  in that cooldown for their 10 s invuln window so they can't be yanked back
  through the portal they land on). While the counter runs, `spawnPortalSwirl`
  rings the entity with **gold `DustParticleOptions`** (denser as it nears
  zero - vanilla has no gold portal particle) and a **player** gets a
  per-second countdown line in chat (`"Portal -> N seconds..."`, last 5 s) plus
  a one-time "grabs hold" message on tick 1. `LAST_COUNTDOWN` dedupes the
  lines. The dwell guard accepts any `AbstractHorse`, not just `Horse`.
  - Non-debug portal + player -> `DebugPenManager.enter(player, that dim,
    portalPos.above())` - fresh plot, return wired to that portal.
  - Debug-dim portal + player -> `plotContaining(x)` -> `plot.returnDim` /
    `plot.returnPos`, **and `evacuateTamedHorses`** (see the dimension
    section). A leashed horse `dropLeash()`s. Non-player entities in a
    non-debug portal: no-op (documented limitation).
- **Roped-horse shortcut**: right-click a `hay_portal` while leading horses
  (`onRightClickBlock`, scans `getEntitiesOfClass(Mob, inflate(12),
  isLeashed && getLeashHolder == player)`) - each horse `snapTo` the portal
  block, `dropLeash()`, dwell counter seeded so it teleports in ~3 s.

**Verified 2026-08-30:** frame flood-fill + golden-carrot activation; the
animated portal texture (faces the player, opaque, 12->48 fps ramp); gold-dust
swirl + chat countdown; cross-dimension `teleportTo` for horses; return
teleport (tamed horses land beside the overworld portal, unharmed).
**Verified 2026-09-01:** the `PortalEventHandler.onRightClickBlock` roped-horse
shortcut - right-clicking a `hay_portal` while leading a horse sends it through
and drops the lead.
**Still unverified**: whether the 10 s player dwell feels too long.

Known limitation: `PLOTS` is in-memory, so a server restart orphans a dead
plot's blocks **and its horses** in the void (harmless - the next plot at that
X rebuilds identical geometry over them, `buildPen` sees the surviving horses
and doesn't double-spawn, and `onLogin` keeps players out of a plotless
dimension). Their ancestry records are the one thing that leaks on a crash;
a clean exit forgets them.

## Known gaps / next steps

The **`runClient` checklist lives in `wiki/verification.html`** - both the
**open issues** found in-game and what's still unconfirmed. Keep that file
current after each session. The **long-range** backlog (the full gene wishlist,
per-allele stack priority, the modder-facing gene API, non-coat and health
genes) lives in **`wiki/roadmap.html`**; this list stays near-term.

**Fixed since that session:** grey (was "flat near-white, wants a rework") is
now the `GreyCoat` dapple grey - built, unit-tested and sample-baked, **not yet
seen in-game**.

**Both 2026-09-01 splash issues are closed** - not fixed, *superseded*. Splash
"not reading its own dose" and its "perfect ring" sock edges both went with the
gene: `MITF` and `PAX3` have real per-combination outcomes, and
`WhitePattern.splash` is one wobbled waterline whose crossing height on a leg is
irregular by construction. `CoatRegions.whitenLowerLeg` / `whitenBlaze` now have
no callers at all.

**Closed 2026-09-05 (built, not play-tested): the face-marking family.** All
four white loci draw the head from one shared vocabulary now -
`WhitePattern.faceMarking` + `FaceMarking`, three components plus a width, with
**star** and **snip** as real detached patches. See the status entry above and
`wiki/pipeline.html#face-markings`. What is left of it is a play-test (does a
three-to-five-texel star read as a star at 128px?) and the two follow-ups below.

**The 2026-09-02 visual genes (2026-09-02, reworked once after owner feedback) -
remaining follow-ups, none seen in-game:**

- **Dun** leg barring is a hand-rolled Y-phase; roadmap §4.1 wants it to reuse
  `BodyStripes` (which runs on X). The third allele **is built** (2026-09-03);
  what is left there is that `d1` shows only the dorsal stripe, where a real
  non-dun-1 horse can also carry faint bars and shoulder shadowing. (Grullo
  lands on the LUT neutral column - `keepRed` scales to 0 by the texel's black
  content.)
- **Silver** has no dapples yet - v1 is the dilution only. A deterministic
  (fixed-seed) `BodyNoise` dapple modulation is the obvious next step. The
  flaxen mane currently reads a little gold rather than pale.
- **`EDNRB` paints lethal white but doesn't kill.** `O/O` has its own
  all-white masking outcome and gets a pen, which is the honest half; the foal
  then lives, because there is no health system (roadmap §6.4). The *coat* half
  is done, so this is now waiting on that system rather than on gene work.
  Frame coverage is deliberately bold (0.52-0.74); may want trimming in-game.
- **Every white-pattern threshold is eyeballed off sample bakes**, not
  play-tested - the `KIT` strength ladder especially. The numbers are monotone
  (a unit test pins that) and measure 5.7 / 11 / 15 / 25 / 25 / 73 / 94 / 100 %
  coverage, but whether those read as *eight distinguishable steps* on a 3D
  horse is the open question. Roan still shows the odd 1px gold fleck at a
  fleck edge (the LUT diagonal); everything else is hard-binary so it doesn't.
- **"White finds white" has never been seen stacked more than two deep.** Each
  white-pattern painter raises its strength by how white the horse already is,
  which is what makes the two splash loci and `W20` mean anything - but a horse
  carrying tobiano *and* `KIT` *and* both splash loci compounds four times.
  It cannot run away (strength clamps at 1), but it has not been looked at.
- **The wide white-pattern genes don't compose an eye-safe check** - a big
  `SB1/SB1` or a topline-crossing tobiano could in principle wipe the eye
  texels; `CoatRegions.redrawEyes` runs last so eyes always come back, but
  confirm in-game.

Design follow-ups (not just "go look at it"):

1. **Grey has no age - and that's now a decision, not a gap.** Horse **aging is
   deliberately out of scope** (it risks feeling bad for a player attached to a
   horse), so `GreyCoat`'s progression stays drawn once from the `G` copy's
   epigenetics and fixed for life: one grey is a steel four-year-old, another
   near-white, neither changes. Flea-bitten grey and grey melanoma are parked
   with it. The option isn't foreclosed - reopening it means giving the
   composer a real age input, which today only knows adult vs foal. It is a
   **settled** call - `wiki/roadmap.html` §21 and the §6.4 note.
2. **Foal geometry is approximate** - `Skin.BABY` uses rest-pose AABBs and
   pre-resolved neck/head/ear pivots; markings on the foal face/neck can land
   loosely. Also the foal mesh has no MANE/MUZZLE part, so bay foal "black up
   the face" is coarse. Foals are also the top **unverified** item.
3. **Genetic eye colour** - the eyes render correctly but are copied verbatim
   from the template (`CoatRegions.redrawEyes`). Wants its own gene; the
   classic hook is blue eyes on cream double-dilutes.
4. **White markings beyond splash** - the framework is ready (natural +
   non-deterministic gene); sock distributions, roan and rabicano slot in the
   same way.
5. **More loci** - dun, pearl-cream stacking nuance, sooty.
6. **Coat realism** - outside grey's dapples, every gene still samples the
   gradient flat per pixel (no sooty shading, no seasonal coat). `BodyNoise` is
   the reusable seam for the next one. `T` on a non-deterministic coat still bakes a
   unique (identical-looking) texture per horse.
7. **Phase 3 now has three inhabitants and the blend question is answered.**
   Magic zebra validated the *negative* half of the unclamped signed model
   (`-200%` reads black over any coat, dominant white included); pink hair
   showed that a **blind add is not enough** on its own - to reach pink on a
   black mane a fixed delta has to push hard enough to saturate a pale mane to
   white, so it reads `ColorView.visible` and returns the delta that walks the
   texel toward its target. So: *straight signed add* stays the blend, but the
   useful magical genes will read first, and `magicalOrder()` matters more than
   §1 assumed. `naturalOrder()` / `magicalOrder()` are still hand-written lists
   - making them *derived* is the gene-priority work (`wiki/roadmap.html`
   §2), not done here.
8. **`breedNth` foal names past foal 1** / **`FamilyTreeScreen` scroll mode** /
   **stats surfaces** / **water-riding feel** / **the epigenome across a
   save-reload** - see `wiki/verification.html`.
9. **Epigenetics follow-ups** - a foal copies a parent's per-allele seed
   **exactly**, with no variation, so a closed line converges on one look;
   (the second half of this gap - the epigenome living on the entity, so
   `FamilyTreeScreen` had to invent an ancestor's coat from its UUID - was
   closed 2026-09-03 by moving the epigenome onto `HorseRecord`.)
10. **Partly fixed 2026-09-04; the carrier half is still open.** The info panel
   and the paper dump now read a horse's expressed `Condition`s and print their
   names and sentences, so the prose is finally read by something. What is
   **still** unread is the *carrier* wording - every health locus declares a
   sentence explaining what one copy means and nothing shows it - plus
   `Expression.masks()` outside `GenotypeCatalog` and `Gene.name()`. The
   punnett / expected-foal display is the natural companion and is what turns
   two carriers from a nasty surprise into a decision. Original entry:
   *Nothing reads the expression table but the coat and the catalogue.* Every
   gene now carries, per combination, a display name and a human-readable
   sentence saying what it does - written for the gene dictionary and the wiki,
   and read by neither yet. The obvious consumers: a punnett / expected-foal
   display, "carrier of X" wording in the info panel (MATP's `pearl-carrier`
   and pink hair's `pink-carrier` already have the sentence), a generated gene
   dictionary, and `GeneCodeDisplay` deciding what is worth printing.
   **`Gene.name()` and one more `Expression.name()` reader landed 2026-09-04**:
   the rebuilt spawn-egg editor lists genes by their display name and prints the
   outcome name under any row that expresses. Still unread: `Expression.masks()`
   outside `GenotypeCatalog`.
11. **Cleanups**: rename `DebugPenManager` / `DEBUG_LEVEL` /
   `horsegenetics:debug_pens` to non-"debug" names (needs a save-data
   migration or a one-time reset); name-generation rework; real white-fog dimension effects
   (needs a client dimension-effects mixin); the stray `neoforge.mods.toml`
   duplicate.
12. **Closed 2026-09-04. The gallery is gone; the catalogue stays as arithmetic.**
   `GenotypeCatalog` is no longer materialised (`size()` is arithmetic, `get(i)`
   reads an odometer) because at **2 064 387** entries an eager
   `List<Genotype>` is hundreds of megabytes. That was half the answer; the
   other half was that a corridor of that many pens is ~7.2 million blocks. A
   20 000-pen cap held it for one session and the revert to **random pens**
   (roadmap §8) closed it properly - the corridor is a fixed 2 000 pens and does
   not grow when a gene is added. What is left is a caller note, not a gap:
   `entries()` is a **lazy view**, so `entries().stream()` still walks and builds
   all two million - sample it, or ask the arithmetic. Three catalogue tests are
   seeded sampling for exactly that reason.
   *(The previous #12 - `CoatPipelineGoldenTest.override` silently ignoring an
   unknown gene, so a case naming a retired gene pinned nothing - is fixed: it
   throws now, and the five stale `cream` / `pearl` cases were re-pointed at
   `matp=`.)*
13. **The wiki is now load-bearing, so it can rot.** `wiki/api-reference.html`
   hand-transcribes public signatures out of `common/` and
   `wiki/gene-*.html` hand-transcribes each gene's constants - neither is
   generated, so both drift silently the moment a signature or a tuning number
   changes. Nothing checks *those*. The one place this is now guarded is the
   **gene creator**: `check-parity.mjs` compares its schema mirror and its whole
   preview engine against the real Java. That is the model for the rest - the
   cheap version elsewhere is a `:common:test` that greps the gene pages for the
   constants they quote. Until then the other pages are a discipline item, which
   is why they are in the session-end routine.
   **Confirmed by the 2026-09-04 roadmap audit**: `wiki/genetics-model.html` had
   drifted exactly this way - its gene table was headed "the eighteen registered
   genes", omitted all thirteen non-coat loci, and still said lethal white's
   death "waits on the health system" a day after that system shipped. The
   roadmap itself was the other half of the same rot: shipped sections were
   marked shipped rather than deleted, so `roadmap.html` had quietly become a
   second description of built behaviour. Both are fixed; **the lesson is that
   "mark it shipped" is not the same as "move it", and only moving it keeps one
   source of truth.**
   **It happened again in the same session** the magical utility genes landed:
   the end-of-session sweep found the gene count, the catalogue size, the raw
   genotype count and the code-segment length all stale in four different files
   (`roadmap.html` §10, `horse-body.html`, `verification.html`, and CLAUDE.md's
   own pens section) - every one of them a *derived number written out by hand*.
   That is the specific shape of this gap and it is now clear enough to act on:
   the cheap fix is a `:common:test` that greps the docs for the handful of
   numbers the code can compute (`Genes.codeOrder().size()`,
   `GenotypeCatalog.size()`, `totalGenotypes()`) and fails when a page disagrees.
   Prose about *how* a gene works survives a change; a number in prose almost
   never does.
   **And it is not only the prose.** Building the particle locus found that
   `GenotypeCatalog.totalGenotypes()` had **already overflowed its `long` and
   wrapped**, before this session - so the 3 028 898 126 035 238 912 written into
   CLAUDE.md, `genetics-model.html` and the roadmap was not a stale number, it was
   a number the code itself was computing wrongly and every doc had faithfully
   copied. It is a `BigInteger` now. The lesson sharpens the one above: a derived
   number written out by hand is a liability, and a derived number whose
   *computation* can silently saturate or wrap is worse, because re-deriving it
   reproduces the lie. The proposed test - grep the docs for the numbers the code
   can compute - would not have caught this one; what would is asserting the
   arithmetic cannot overflow.
14. **Data-driven genes cover markings and dilutions, not everything.** The
   format has no expression language and no way to read another gene, so the
   three built-ins that genuinely need one still can't be expressed as specs:
   **grey** (its remap onto the gradient's neutral column reads the coat's
   darkness *and* rewrites both channels together - `PIGMENT` masking gets close
   but not there), **cream/pearl** (they read *each other's* dose), and
   **bay**'s exact face-follows-legs coupling. Those stay Java, which is fine -
   the tiers were always meant to bottom out at a real class. What would move
   the line: a `dose` mask on another gene, and a `REMAP` op.
15. **The creator has no in-page parity button.** Parity is checked by a Node
   script at the terminal, so the tool itself will happily show you a stale
   preview if you edit `js/` and don't run it. Loading `fixtures/expected.json`
   in the page and self-checking on boot would close that.
16. **Gene `effects` are a thin slice and mostly untested** - though less thin
   since 2026-09-04: the set is **eight verbs**, `healing` and `spread` joined
   it, `emitter` grew a second colour / a count / a `data` number / five
   body-site anchors and a registry-backed particle lookup, and **built-in genes
   use the same vocabulary** through `AbilityContribution` (or
   `EpigeneticAbilityContribution`, where the effect varies per horse), so the
   path is no longer spec-only.
   `attribute` still
   parses but the translator doesn't apply it (logged once). `mob_effect` is
   wired (`applyMobEffect`). `glow` is wired both sides - the light half is a
   trailing `minecraft:light` block (janky vs a mixin-based dynamic light: lags
   a gallop, air-only, orphans on a server crash, skipped in the gallery), the
   emissive half is `EmissiveCoatLayer` reading the composed coat. `emitter`'s
   own `light` kind is still a no-op (use `glow`); `emitter` otherwise only does
   particles; `yield` recognises a fixed handful of output items;
   `walk_on_water` is buoyancy, not a solid plane. Conditions are boolean, not
   the architecture's 0-1 scalars. There is no trait registry, no `on_change`,
   no selectors/auras/pools. The gene creator can't edit an `effects` block -
   it's hand-written. `healing` and `spread` are both first drafts: `healing`
   caps at `max_targets` and does a real sphere check, and `spread` converts at
   most one block a beat off a narrow, hand-written block list - which is the
   piece most likely to be wrong in play (the risk is a horse eating something a
   player built with). Only Waterborn's coat + trail are confirmed in-game;
   Suntouched and every other effect verb are unverified
   (`wiki/verification.html` §13, §0a). The full plan is
   `wiki/horse-traits.html`.

17. **Some gameplay-layer items still have no behaviour.** The seed jars,
   whistles and stall signs work; still unwired: shearing to get `horse_hair`,
   any carrot effect on the breeding draw, and **the tickets** - owner's intent
   is that a ticket teleports its bound horse back to its stall, which is now
   possible (stalls exist - `StallData` / `StallRecord.center()`), it's just not
   built. The `magic_gene_carrot` is one generic item because per-gene targeting
   wants a data component (`wiki/roadmap.html` §14.2, §19);
   `placeholder_gene_book` replaces the real research paper. Tickets share one
   texture, whistles share one, stall signs borrow `oak_sign` - per-tier / real
   art is a follow-up (`wiki/verification.html` §15).
18. **The stallion seed jar is a first slice, not the §15.1 flow.** Collection
   and impregnation are wired (`StallionSeedJarHandler` + the `stored_genome`
   component + `GenomeSample` + `HorseBreedingHandler.applyBredFoal`). The gate
   is **vanilla love** (`isInLove()`), not one of this mod's breeding carrots
   (they still do nothing); there's **no gestation** (the foal appears
   immediately, like vanilla breeding), and the jar carries **no carrot
   effects**. The synthetic sire record uses the stored donor UUID as its
   pedigree edge, so the family tree may not find the sire node. Gestation is
   the "genuinely new" piece per the roadmap; the real carrot gate waits on
   §14. Owner tests 2026-09-02: (a) held jar didn't change in creative -> fixed,
   transforms in hand now; (b) worked with no breeding-mode requirement ->
   fixed, both ends now require `isInLove()` and consume it.
   `wiki/verification.html` §16.
19. **Fixed 2026-09-03 (data-model rewrite), not play-tested.** The short
   genome string now shows data-driven genes: `GeneCodeDisplay` derives its
   trailing gene list from `Genes.codeOrder()` (built-ins in a curated display
   order, then `Genes.loaded()`) and derives the "wild type means absent"
   test from `dominance()`, so the shipped Suntouched / Waterborn appear.
   Confirm in-game (info panel, paper dump, seed-jar tooltip).
20. **The stall system is detection + storage only.** A stall gets defined and
   persisted (`StallData`), but nothing *uses* it yet: no teleport-to-stall, no
   "assigned pen" behaviour, no auto-return. Cleanup is thin - a stall only goes
   away if its exact sign block is broken (`BreakBlockEvent`); rebuilding a wall
   elsewhere, or removing the horse, leaves a stale record. The sign is a plain
   vanilla `oak_wall_sign` (no marker that it's a stall sign beyond the
   `StallData` entry keyed on its pos). The "debug overlay" is server-emitted
   `HAPPY_VILLAGER` particles on the F7 keybind (one flash per press) + a chat
   summary - not a persistent client wireframe, which would need a
   `RenderLevelStageEvent` renderer (the 26.1.2 render pipeline changed enough
   that this was deliberately deferred). Flood-fill is air-only and capped at
   `StallDetector.MAX_BLOCKS` (512).

21. **Horse care is a first slice, and unplayed.** `HorseCareHandler` +
   `BondFollowGoal` + `HorseCareAttachment` cover §7.2 gated healing and §13
   bond/herds; details and what's deferred are in `wiki/horse-care.html`, the
   in-game checklist in `wiki/verification.html` §0. Specifics still open:
   **milking (§7.1)** is not built (its "full health" rule is the hook into
   the healing gate); **bond has no shearing/sleeping source** and there is
   **no console command to set it**, so testing the tiers means grinding or
   temporarily lowering the thresholds; **herd alpha** is computed-on-demand
   with nothing consuming it yet; the **comfort buff** is restated as +1 regen
   because the mod has no stamina; the healing scan uses **block tags** so a
   pack can extend `horse_water` / `horse_food`, but bucket/fluid-source
   placement inside a waterlogged block only counts via the `#minecraft:water`
   fluid check, not the block tag. Feed-bond fires on `EntityInteract` for any
   `isFood` stack and is **not** dose/temper-aware.

22. **Sex is a gene, but nothing is sex-*linked* yet** (roadmap §5.3, second
   half). The locus is built and `HorseRecord.sex` is derived from it, which was
   the prerequisite; what's left is the inheritance *mode*. Specifically:
   `Gene` has no autosomal / X-linked / Y-linked declaration; `breedWith` has no
   two extra cases (sire gives his `X` to a filly and his `Y` to a colt, mirrored
   for Y-linked); `Gene.canOccur` is the seam the catalogue already filters on
   but it doesn't consult a mode, so an `X/X` mare at a Y-linked locus would
   still be enumerated; a founder table can't yet say "a colt draws one allele
   here, a filly two"; and no surface writes the hemizygous `X-`/`Y-` prefix.
   **Brindle** (§4.2) is the gene that wants all of it. Also unbuilt: any
   *sexual dimorphism* in the coat - sex paints nothing and deliberately never
   will, so a stallion's crest would be a separate gene reading this one.

23. **The whole trait / size / health layer is unplayed.** Thirteen genes, a new
   subsystem, four attributes and a death handler, and none of it has been seen
   in-game - not a scaled horse, not a dying foal, not a refused pairing, not the
   config. The checklist is `wiki/verification.html` §0b, and the quickest way in
   is the custom spawn egg: adding a health gene defaults it to the affected
   genotype, so "add PLOD1, pick Foal, spawn" is a one-click lethal foal.
   Specific unknowns worth naming: **a scaled foal** (`Skin.BABY`'s projection is
   already approximate, and nothing has been rendered at 0.88 scale), **the rider
   position** on a large and a small horse, whether the **info panel still fits**
   (it gained two rows and a condition list), and whether the
   `death.attack.horsegenetics.genetic_defect` lang key actually resolves rather
   than showing a raw key.

24. **The stats are purely genetic, with no noise, and that is a decision.**
   Roadmap §6.2 left room for the random roll to survive as environmental jitter
   on top. It did not: two horses with the same genotype are byte-identical
   animals. If the numbers ever feel too tidy the place to reopen it is
   **epigenetic variation** - drawn from the expressing copy's seed, inherited
   with the allele, still deterministic - not a fresh die roll. Related: the
   **founder carrier rates (1.4%-4%) are guesses**, chosen so a wild horse is
   healthy and an inbred line is not, and it is entirely possible a player never
   meets a lethal at those numbers.

25. **Milk made an existing `yield` wart load-bearing.** A `yield` cancels the
   interaction on both sides so vanilla doesn't also read it as a mount - fine
   while the only yield in the game was Waterborn's, on a rare gene. **Every mare
   now has one**, so right-clicking any grown mare with an empty bucket gives
   milk rather than mounting, always; and while the 200-tick cooldown is running
   `fulfil` returns early but the event is still cancelled, so you get neither
   milk nor a mount and no feedback. Suspected from reading the code, not seen -
   `wiki/verification.html` §0a. Fixes in order of size: don't cancel when the
   cooldown blocks it; require sneak; or gate milking on `tamed`, which
   `wiki/roadmap.html` §7 wants anyway.

26. **Two magical genes do something a player cannot see.** Milk and verdant
   both paint nothing, so a lava-bearing horse and a moss-spreading one are
   indistinguishable from an ordinary horse until you put a bucket under them or
   watch the floor. That sits badly beside **healer**, which draws a red stripe
   precisely so the ability is legible, and beside the rule in
   `wiki/gene-effects.html` that an effect must be *perceivable*. The fix is a
   marking allele in each locus rather than a mark bolted on beside it, which is
   why it was not done in passing. `wiki/verification.html` §0a records it as
   accepted-for-now rather than as a bug.

27. **`GenotypeCatalog.size()` is an `int` and is now 462 million.** The
   seven magical utility genes multiplied the catalogue by 224 (mane 4 x tail 4
   x light 7 x healer 2). Nothing is materialised, so the memory cost is still
   zero, and the arithmetic is done in `long` and **saturates** at
   `Integer.MAX_VALUE` rather than wrapping - so the failure mode is a silently
   *truncated* catalogue, not a nonsense one. Headroom is about **4.6x**, which
   is roughly one more gene of light's shape. When it goes, `size()` and
   `get(int)` want to be `long` together, and every caller that indexes them
   wants re-checking in the same change. Nothing in play depends on it today -
   the pen corridor is a fixed 2 000 random pens and no longer walks the
   catalogue at all (gap #12). **The particle locus did not touch it** (forty
   alleles, one catalogue entry), so the headroom is unchanged.
   **The sibling problem is fixed**: `totalGenotypes()` was a `long` and had
   already wrapped; it is a `BigInteger` now. `size()` was deliberately left
   saturating, because it is an index bound callers loop over and a cap is a real
   safety property there - but that means the two now fail differently, and
   whoever widens `size()` should say so here.

28. **Two health genes are only half-drawn.** `B4GALT7`'s Friesian dwarfism
   should shorten the limbs and ribs and leave the head, but `Attributes.SCALE`
   is one number for the whole entity - per-part scaling means owning the horse
   model rather than borrowing vanilla's, so it renders as an overall
   three-quarter horse. `ST14`'s naked foal is reported and not drawn: phase 1
   can only push pigment *down*, and a de-pigmented mane reads as a *white* mane,
   which is a different horse and a worse lie than drawing nothing. Both are in
   `wiki/roadmap.html` §4.4. Also still absent: **CSNB** (rides on `LP/LP`, and
   the leopard complex does not exist), **DMRT3 / gait** (animation work), and
   **`TraitRule`** - two genes that only together trigger an outcome (§6.5).

29. **Medicine hat and the war shield - reachable, and half-built by accident.**
   A medicine hat is not a face marking and not a gene: it is a **retention**
   rule on a near-white horse, which keeps colour as a bonnet over the ears
   *and poll*, usually with a coloured shield on the chest.
   `WhitePattern.sabino` already never paints the ears, so a near-white sabino
   has coloured ones - but measured over five seeds, `kit=SB1/SB1` comes out
   **ears 0% white, poll 100%, mane 98.6%, chest 100%**, so it reads as two
   detached coloured ear boxes rather than a cap. The crest's `anchor` of 0.16
   is nothing against a `bodyCut` of 0.05 at strength 0.93, which is the same
   over-coverage the white audit found elsewhere. What it needs: the ear / poll
   anchor to actually hold at the top of the strength ramp, and a chest region
   to anchor beside it. Note `W22` dominant white and `O/O` lethal white are
   `masking` outcomes that `restrictAll`, so they take the ears too -
   correctly; those are not hats. `wiki/roadmap.html` §4.2.

30. **The white-pattern audit (2026-09-05) found four calibration defects and
   only one of them is fixed.** Fixed: face markings wrapped under the jaw
   (gone with the shared vocabulary). Still open, all four measured:
   - **`EdnrbGene`'s flank band does not bite on the barrel.** `BAND_LO` 0.28
     and `BAND_HI` 0.74 are fractions of `bodyBounds` - the **whole-horse**
     AABB, hoof to ear tip - but the barrel spans only 0.326-0.622 of that. So
     `side` is exactly 1.0 for every BODY texel and the only thing the band ever
     clips is the top fifth of the neck. Measured: **back 72.6% white**, flank
     77.8%. The gene's javadoc, its outcome description and `wiki/gene-ednrb.html`
     all say frame "never reaches the topline"; nothing implements that.
   - **`TobianoGene`'s topline bias lands on the mane.** `topY = bb.yMax()` is
     the **ear tip**, so the real topline (0.622) sits 35% up the ramp and gets
     `0.35 x 0.18 = 0.063` of the bias while the mane gets the full `0.173`.
     Measured on **every** seed: crest 93.9%, mane 80.2%, against 56.4% on the
     flank - a permanent flat white band along the top of the neck.
   - **`WhitePattern.splash` spends a third of its range above the horse's
     back.** Same root cause; see the `PAX3` status entry above, where it is now
     load-bearing rather than cosmetic.
   - **`cover` is calibrated as if `PatchNoise.field` were uniform, and it is a
     bell.** Measured over 67 200 texels x 12 seeds: p1 0.24, p50 0.51, p99 0.77.
     So `EdnrbGene`'s `cover` 0.52-0.74 (threshold 0.48-0.26) actually delivers
     **58%-98%** white, and `TobianoGene`'s 0.40-0.56 delivers **23%-68%** - a 3x
     swing where a 1.4x one was written.

   The shared fix is to give the painters a **topline reference**
   (`bounds(skin, Part.BODY).yMax()`) instead of the whole-horse AABB and
   re-express the constants against it, plus convert the `cover` knobs through
   the measured quantiles. All four move existing coats, so they want one change
   with a `coat-golden.txt` regeneration and the `WhitePatternGenesTest`
   monotonicity ladder re-checked.

## License

CC BY-NC 4.0 (see `LICENSE`). Forks/derivatives are welcome without asking
but must credit the original repo and link back, and no portion may appear in
a paid derivative with no free version available. Donations/tips on an
otherwise-free derivative are fine. Before vendoring third-party code, check
its licence is compatible.

## Conventions

- Keep `common/` free of Minecraft imports - a hard rule.
- New version-specific logic goes in `neoforge-26.1.2/`, by concern
  (`client/` / `data/` / `network/` / `server/` / `block/` / `item/`).
- Flag genuinely unverified API usage in comments the way the existing code
  does - more useful to the next session than silent confidence.
- When you resolve something flagged here as unverified or a known gap,
  update this file in the same change.
- **`README.md` is user-facing only.** No status, architecture, API notes, or
  file listings there. All of that lives in `CLAUDE.md`.
- The breeding / pedigree / horse-record / **stat-inheritance** system is
  documented **only** in `wiki/breeding.html`; **each gene** is documented **only** in
  `wiki/gene-*.html`; the **`runClient` checklist** is **only** in
  `wiki/verification.html`; the **design rationale** is **only** in
  `wiki/philosophy.html` and the **future backlog** **only** in
  `wiki/roadmap.html`; the **coat machinery** is in `wiki/pipeline.html` +
  `wiki/body-space.html`, and the **modder API** in `wiki/modding.html` +
  `wiki/api-reference.html`; the **non-genetic horse-care systems** (gated
  healing, bond, herds) **only** in `wiki/horse-care.html`; the **trait / size /
  health system** (speed, health, jump, size, disorders, the two lethal paths,
  `ServerConfig`) **only** in `wiki/horse-body.html`. Update the relevant
  file in the same change - a pointer from CLAUDE.md is fine, a copy is not.
- **The wiki has one nav.** A new page goes in the `SECTIONS` array in
  `wiki/nav.js` and nowhere else; never hand-write a sidebar into a page.
  (`wiki/gene-creator/` is the one exception - it is an app, not a page, and
  owns its own chrome.)
- **The data-driven gene format** is documented **only** in
  `wiki/gene-format.html`. A new mask or op has to land in four places in the
  same change: `SpecSchema.java`, `SpecPainter.java`,
  `wiki/gene-creator/js/schema.js` + `spec-engine.js`, and that page. Then
  re-run `:common:bakeSpecFixtures` and `check-parity.mjs`.
- **A new `effects` verb** lands in **four** places, only one of them shared:
  a `record` on `GeneAbility`, one `register(new AbilityType(...))` on
  `AbilityType` (name + params + defaults + validation + record builder - the
  parser reads this generically, no `readAbility` change unless the verb needs a
  new param `Kind`, as `glow`'s `parts` needed `Kind.PARTS`), a `case` in the
  NeoForge translator (`server/GeneAbilityHandler` tick switch, or
  `server/GeneYieldHandler` for interaction), and a section in
  `wiki/gene-effects.html`. A verb with a **client-render** component (so far
  only `glow`) also needs a `RenderLayer` + a `GeneticHorseRenderState` field +
  the bake in `GeneticCoatTextureFactory` - not just a translator `case`. A new
  **condition flag** is two lines (`AbilityType.CONDITION_FLAGS` +
  `GeneAbilityHandler.flagHolds`); a new **trigger** also touches
  `GeneSpecParser.readTrigger`. None of it is part of `SpecSchema` or the parity
  check - effects don't paint.
- **Keep the why out of the backlog.** `wiki/roadmap.html` is work items;
  when a justification there runs longer than a clause it belongs in
  `wiki/philosophy.html` with a pointer back.
- **No legacy / back-compat code.** Dev only, single tester, no saves to keep -
  when a format changes, change it and move on (no genotype-code padding, no
  attachment field fallbacks).

## Ending a session

The routine to run when the owner says **"end the session"** (or "wrap up",
"we're done for today"). It is a *fixed order* - the docs pass comes after the
code is pushed, so it's a review of where the session actually landed rather
than a running commentary written mid-change.

**0. Pre-flight.** `./gradlew :common:test`, `./gradlew
:neoforge-26.1.2:build`, and - if anything under `common/genetics/spec/`,
`SpecPainter` or `wiki/gene-creator/js/` was touched -
`node wiki/gene-creator/tools/check-parity.mjs`. Don't push red. If something fails and can't be fixed
in the time left, still push - but say so in the commit message and put it at
the top of `wiki/verification.html`.

**1. Commit and push the session's code.**

- `git status --short` first and read it. The repo now has a `.gitignore`, so
  `build/`, `.gradle/`, `.idea/` and `run/` stay out of `git add -A` and the
  status should be short enough to actually read. If build or run output *does*
  show up, a pattern is wrong - fix the pattern, don't `git add` around it.
- The owner works directly on **`main`**, which tracks `origin/main`. Commit
  and push there; don't branch.
- One commit for the session's work, with a **descriptive** message: what
  changed and *why*, not a file list. Multi-paragraph is fine and preferred
  when the session did more than one thing. End it with the `Co-Authored-By:`
  and `Claude-Session:` trailers the harness specifies.

**2. Then update the docs to the current state of the program.** Not "what I
changed today" - *what is true now*. Walk all five, in this order, and honour
the doc-split rules under "Conventions":

- **`CLAUDE.md`** - the **status snapshot** first (test count, what compiles,
  what's owner-verified vs built-but-unplayed), then any section the session
  invalidated, then **"Known gaps / next steps"**: delete what got fixed, add
  what got discovered, renumber the list.
- **`wiki/verification.html`** - the `runClient` checklist. Delete items the
  owner confirmed in-game this session (they move to CLAUDE.md's
  "Owner-verified" block, which is the permanent record); add a concrete,
  checkable entry for anything built-but-unplayed, including *what to look at*
  and *where* (which pen, which screen, which key).
- **`wiki/gene-*.html`** - if any gene's alleles, generation function, wild
  frequency, dominance or coat effect moved.
- **`wiki/breeding.html`** - if breeding, pedigree, horse records or stat
  inheritance moved.
- **`wiki/pipeline.html`** / **`wiki/body-space.html`** - if the coat machinery
  or the projection engine moved.
- **`wiki/api-reference.html`** / **`wiki/modding.html`** - if a public
  `common/` type changed shape, or if the gene-authoring contract moved.
- **`wiki/gene-format.html`** - if a mask, an op or the file header moved. If
  so, also re-run `./gradlew :common:bakeSpecFixtures` and
  `node wiki/gene-creator/tools/check-parity.mjs`, and commit the regenerated
  `wiki/gene-creator/fixtures/expected.json`.
- **`wiki/nav.js`** - if any page was added or renamed.
- **`README.md`** - only if the *player-facing* experience changed. It stays
  user-facing: no status, no architecture, no API notes.

**3. Commit and push the doc update as its own commit.** Step 2 always leaves
the tree dirty; a session must not end with unpushed doc changes.

**4. Verify clean.** `git status --short` empty (bar untracked noise you
deliberately left) and `git log origin/main..HEAD` empty. If either isn't, fix
it before stopping.

**5. End the session** with a short terminal summary - what shipped, what's
newly waiting in `wiki/verification.html`, and the one thing the next session
should pick up first. Then stop: no new work, no "while I'm here" refactors.
