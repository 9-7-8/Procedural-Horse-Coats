# Philosophy

Why this mod is built the way it is. Not what it does (`README.md`), not how it
works (`CLAUDE.md`), not what's coming (`Docs/to be completed.md`) - **why**.

Read this before making a design decision. Most of the arguments in the other
docs are downstream of something here, and when two good options conflict, this
is the tie-breaker.

---

## The one-sentence version

**Real Mendelian genetics, so breeding is a game you can actually play; a
procedurally generated coat, so the space of outcomes is functionally infinite;
and determinism throughout, so a surprising horse is a discovery rather than a
dice roll.**

---

## 1. Breeding should be a game of skill, not a slot machine

Mendelian inheritance is the point, not set dressing. Two copies of every gene,
one from each parent, dominance and recessives, carriers that hide a trait for
generations and then produce it. That is already a good game - it's why animal
breeding is a hobby in real life - and it's a game that **rewards knowing
things**.

That means:

- A player who understands the model can **plan**. If you want a cremello, you
  can work out what to breed and how long it will take. You can hold onto a
  plain-looking mare because you know what she carries.
- Surprises are **explainable after the fact**. A foal that comes out
  unexpectedly should always have an answer, and the answer should be in the
  genotype - not "the RNG felt like it".
- Rare things stay rare **because of the genetics**, not because of a
  low-probability roll bolted on top.

The failure state this is guarding against: a player breeds two horses,
something unexpected comes out, and their conclusion is "I guess it's random."
The moment that happens, every breeding decision after it is arbitrary, and the
whole system collapses into a slot machine with a long lever.

## 2. Determinism is the load-bearing wall

**The same genetic code with the same alleles always produces the same horse.**
Every client, the server, a unit test, a sample bake, this launch and the next
one, with any set of mods loaded.

This is the rule that outranks the others, and it's worth being explicit about
*why* it matters so much, because it looks like a technical concern and isn't:

- **It's what makes the genetics feel real.** A horse's appearance is a *fact
  about its genome*, not a decoration that happened to be applied to it. Two
  horses with the same code look the same because they *are* the same, and
  that's what makes a genotype worth reasoning about.
- **It's what makes surprise legitimate.** A player can only feel "I bred
  something remarkable" if the remarkable thing was determined by the breeding.
  If the same pairing could have produced it or not, the achievement is hollow.
- **It's what makes the whole thing debuggable.** A genetic code in a bug
  report reproduces the horse exactly. This has already paid for itself once -
  the flat-white-horse bug was found because a coat that should have been
  reproducible wasn't.

It is also structurally load-bearing: a coat is baked once per texture key and
reused by every horse sharing that key, so a coat that isn't a pure function of
its key doesn't render *wrong*, it renders as **another horse's coat**.

### What the contract forbids

- **No unseeded randomness.** All per-horse variation comes from the epigenetic
  seed of the allele copy that expresses. No `new Random()`, no
  `Math.random()`, no `RandomSource` from the level.
- **No input the key doesn't capture.** Not entity UUID, not position, not
  world seed, not wall-clock time, not tick count. If something is to be
  depended on, it has to become part of the texture key first.
- **No iteration-order luck.** Registries and per-gene maps need a defined
  order; a `HashMap` walked in iteration order is a latent non-determinism.
- **No mod-load-order dependence.** With third-party genes, registry contents
  arrive in any order, so every ordering the engine uses - processing, code
  string, catalogue - is **computed by sorting**, never taken from
  registration order.
- **No order-dependent float drift.** A total, stable processing order is what
  keeps two runs bit-identical rather than merely similar.
- **No mutable static state on a gene.** A gene is a singleton shared by every
  horse on the server; a cached field is a cross-contamination bug.
- **Nothing may reorder genes per horse.** Gene order comes from hard-coded
  numbers. Per-allele epigenetic priority chooses *which copy's seed
  expresses*, never *what runs when* - if it could, two horses with the same
  genotype and seeds could diverge while sharing a cache entry.

### Where randomness is allowed to happen at all

Exactly one place: **the creation of a foundation horse** - a wild spawn, a
`/summon`, a horse the dimension places. Those have no parents, so there is
nothing to inherit and something has to be invented.

Everywhere else a horse's genetics are a **pure function of its parents**.
Breeding does consume randomness - segregation has to pick a copy - but that is
*inheritance*, not invention: the alleles and their seeds all come from the
parents unchanged. **A foal must never go through the founder path.**

Practical corollary, and it's a strict one: **a horse's appearance can never
drift after birth.** No re-rolls on reload, no per-tick variation, no
appearance that depends on where it's standing or what time it is. Any future
feature wanting per-horse variation takes it from the epigenome, not from a
fresh draw.

### How it gets checked

Three tests worth keeping alive: composing the same genome twice gives
identical pixels; shuffling gene registration order changes nothing; and a
stored hash for a handful of reference genotypes, so an accidental change to a
shared helper is caught rather than silently restyling every horse.

## 3. The coat should have functionally infinite outcomes

Vanilla Minecraft has seven horse colours and five markings: 35 horses, and
you've seen them all in an afternoon. That's the thing being replaced.

Coats here are **generated**, not picked from a list. Genes restrict pigment per
pixel, the survivors get looked up in a hand-authored gradient, and the result
is multiplied onto a white template. Add the epigenetics - a per-allele-copy
seed that varies how a gene expresses, inherited with the allele - and the space
stops being enumerable. Two bays with the same code have different point
heights. Two greys are at different stages of dappling.

The goal is that a player can look at a horse and think *I have not seen that
one before*, and be right, for a very long time.

This is why the pipeline is a pipeline and not a lookup table, and why every new
gene has to be a **function** over the horse's body rather than a sprite. It's
more work per gene. It's the entire point.

## 4. "Interesting" is the player's word, not ours

There is no scoring function on a horse. No rarity tier, no "legendary" tag,
nothing telling a player which coat is the good one.

The mod's job is to make a **large space of visibly different outcomes**, all
reachable by breeding, and then get out of the way. One player breeds for the
palest cremello they can get. Another wants the most extreme splash markings.
Another is chasing a specific dapple pattern they saw once, or breeding for
speed and doesn't care what the horse looks like, or trying to fix a rare
recessive in a closed line just to prove they can.

All of those are the same feature. Design consequences:

- **Don't put a value judgement in the model.** No gene is "better". Rarity is
  a frequency, not a quality.
- **Show players what they need to plan**, and let them decide what to aim
  for - carrier information, a punnett display, the genotype itself.
- **Variety beats polish on any single outcome.** A gene that adds a new axis
  of visible difference is worth more than one that perfects an existing look.

## 5. Abstraction is a feature, and we say so out loud

The genetics are **deliberately a simplification**, and the simplifications are
chosen, not accidental:

- **No crossing over, no recombination.** Every gene segregates independently.
  Two genes on the same real chromosome are unlinked here.
- **No maternal or mitochondrial inheritance.**
- **No imprinting, no mosaicism.**
- **No aging.** Horses don't get older, don't grey out over time, don't develop
  age-related conditions. (Reopenable - but not assumed.)
- **Real loci get collapsed.** Several distinct real-world genes are one gene
  here, and several real alleles are dropped.

The test each of these passes: **does modelling it make breeding a better game,
or just a more accurate simulation?** Linkage makes a punnett square lie.
Aging makes a player watch something they're attached to decline. Neither buys
enough to pay for what it costs.

Being explicit about this is itself the philosophy. A player who knows the
rules - including which ones we broke - can trust their own reasoning
completely. A player who suspects there might be *hidden* rules can't trust any
of it, which lands right back in "I guess it's random". The abstractions are
published, not concealed.

### The one exception: sex linkage

**X-linked genes are in**, and they are the single deliberate departure from
strict Mendelian inheritance. Brindle is the case that earns it.

This one passes the test where the others fail. Sex linkage doesn't make
breeding less legible - it makes it *more interesting to reason about*, and the
reasoning is still exact. A stallion carries one copy and therefore can never
be a carrier: whatever is on his X, he shows, and he passes it to every
daughter and no son. A mare needs two. That produces a trait which visibly runs
down one side of a pedigree, and a player who knows the rule can predict it
perfectly and plan around it.

That's the distinction that matters here. An abstraction gets dropped when it
**adds unpredictability without adding decisions**. Sex linkage adds
decisions - it makes *which parent* carries a gene matter, which nothing else
in the model does - while staying completely predictable. So it stays, and it
is the only one.

## 6. Real and magical, clearly separated

Two kinds of gene, and the line between them is hard:

- **Natural genes are only genes that exist in real life.** They work like real
  pigment: they restrict how much eumelanin and phaeomelanin a cell can
  produce. They can only ever take colour *away*, which is why every real horse
  colour is a subtraction from black.
- **Magical genes are everything invented for this mod.** They work on RGB
  directly, adding and removing colour after the natural pigment has resolved,
  and they can do things no real horse does.

A gene is one or the other, never both. The separation is a promise to the
player: **the real-world genetics are real.** A bay is a bay for the actual
reason a bay is a bay. If a horse is glowing, that's a magical gene and it's
labelled as one - it isn't the "realistic" system quietly being unrealistic.

It also keeps the fantasy from constraining the simulation. Magical genes run
last and can paint over anything, so they never need the natural model to bend
for them.

## 7. Other people should be able to add to this

The gene model is meant to be **extended by other mods**, and that's a design
constraint from the start rather than a feature to add later.

Why it matters philosophically and not just practically: a genetics system with
a fixed gene list has a ceiling, and every player eventually hits it. A system
anyone can add a locus to doesn't. The functionally-infinite promise in §3 is
much easier to keep if the mod isn't the only source of genes.

What that demands of the design:

- **A gene declares itself**, rather than being wired into hand-maintained
  lists. Priority, phase, alleles, spawn chance, coat function - all on the
  gene.
- **Simple genes must be genuinely simple.** A dilution that multiplies black
  by 0.45 should be a few lines, not a class implementing eight methods.
- **Load order can never matter.** Every ordering is computed by sorting, so a
  horse looks the same regardless of which mods loaded in what order - which is
  §2 again, arriving from a different direction.
- **The contract has to be stated, not assumed.** A third-party gene that
  breaks determinism corrupts *other people's* horses through a shared texture
  cache. That has to be written down where a modder will read it.

## 8. The horse is something you look after

Horses are animals you keep, not equipment you own. The care features exist to
give that some texture - a horse heals when it has water and food nearby, a
tamed mare in good health can be milked - and to make the world you build for
them matter.

The rule these follow: **add texture, don't add chores.** A mechanic that makes
a player tend to their horses is good. A mechanic that punishes a player for
going on a long trip is not. Hence hand-feeding always works and never needs
water: the player always has a direct, reliable way to look after a horse, and
the ambient rules are about horses looking after themselves in a pasture you
made well.

Health genetics land in the same place: a genetically frail horse has **fewer
hearts**, which is legible at a glance and needs no new system to express. The
harshest consequence - a foal that inherits two lethal copies and dies - is real
genetics and stays in, but it's config-gated, because a mod that is a breeding
game for one player is a horse game for another.

---

## When these conflict

Rough priority, for the cases where two of these pull apart:

1. **Determinism** (§2) wins over everything. A feature that can't be made
   deterministic doesn't ship.
2. **Mendelian legibility** (§1) wins over accuracy. If a real-world mechanism
   makes breeding harder to reason about, abstract it away (§5) and say so.
3. **Variety** (§3) wins over polish on any single outcome.
4. **Player-defined goals** (§4) win over designer-defined ones. When in doubt,
   expose information and let the player decide.
5. **Extensibility** (§7) wins over convenience for us. A shortcut that only
   works because we control every gene is a shortcut that breaks the first time
   someone else adds one.
