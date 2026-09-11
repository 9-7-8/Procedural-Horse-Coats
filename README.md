# Horse Genetics

**A Minecraft mod that gives every horse a real genome.** Coat color isn't
picked from a fixed list of textures - it's generated pixel by pixel from a
Mendelian genotype of allele objects, the same way a real horse's coat comes
from its DNA. Breed two horses and the foal inherits its colors, patterns,
stats, and even hidden carrier genes the honest way, through segregation and
dominance, not a random roll. It's genetic horse breeding, procedural coat
generation, and pedigree tracking built as one Minecraft mod - for anyone
who's wanted Rimworld/Sims-style genetics, a Wildermyth-esque family tree, or
a serious horse-breeding sim, but for horses in Minecraft.

- **Minecraft:** 26.1.2
- **Loader:** [NeoForge](https://neoforged.net/)
- **License:** [CC BY-NC 4.0](#license) - free to use, share, and build on
- **Wiki:** **[9-7-8.github.io/Procedural-Horse-Coats](https://9-7-8.github.io/Procedural-Horse-Coats/)**
  - every gene's alleles, odds, and effects; the breed list; the breeding and
    stat-inheritance rules; the coat-generation engine; and a walkthrough for
    writing your own gene, no Java required

---

## What it does

**Genetics, not textures.** Every horse carries a full genotype - dozens of
genes covering base color, dilutions, white spotting patterns, and more -
stored as real allele pairs. Its coat is drawn fresh from that genotype onto
a 128px texture, so two horses with the same genes look the same, and an
unusual combination produces a coat nothing else in the world has. There are
functionally infinite possible coats.

**Some traits run down one side of the family.** Brindle - irregular vertical
striping - sits on the X chromosome, so a stallion carries one copy and can
never be a hidden carrier, while a mare needs two. A brindle stallion throws no
brindle sons at all and every daughter a carrier, so the pattern skips a
generation and comes back through the mares. A brindle mare is a real breeding
project.

**Eye colour is inherited too.** A splashed white or dominant white horse has
blue eyes; a horse carrying two copies of tiger eye - a gene essentially confined
to the Puerto Rican Paso Fino - has a bright amber iris and an entirely ordinary
coat. Blue wins over amber on a horse that is both, because there is no pigment
left in the iris to be amber.

**Breeding is Mendelian.** Foals inherit one allele from each parent at every
gene, so recessive traits can hide for generations and surface unexpectedly,
two ordinary-looking parents can throw a surprising foal, and a pedigree
becomes something worth actually keeping. Some real-world genetic disorders
are modeled too - most survivable-but-visible, a few lethal - so breeding
carelessly has real consequences, with a server setting to dial that down or
off.

**Every horse has a pedigree.** A generated two-part name, a family tree you
can browse back several generations (with a little 3D model of each
ancestor), and speed/health/jump/size stats that come out of its genes rather
than a dice roll - so "breeding for speed" means finding and fixing the right
alleles, not getting lucky.

**Wild horses come in breeds.** Real-world breeds - from Shires to Arabians
to Falabellas - spawn in herds keyed to biome, each with its own build, color
pool, and temperament, and cross-breeding produces properly labeled crosses
and mixes.

**Genes beyond real biology.** Alongside the natural coat genes, there's a
whole layer of magical genes with no real-world counterpart: particle
trails, glowing manes, size and speed genes, water-walking, milk that isn't
milk, and more - built to combine in ways nobody explicitly coded. A breed
carries only the magic (and the disorders) it was made with; stray magical
traits turn up in unbred wild horses, and breeding them into a line is up to
you.

**Write your own genes with no code.** An in-browser
[gene creator](https://9-7-8.github.io/Procedural-Horse-Coats/wiki/gene-creator/)
lets you design a new gene - where it paints, what it does - and preview it
live on a 3D horse over a range of base coats. Export the file, drop it in
your world's config folder, and it inherits, breeds, and shows up in-game
exactly like a built-in gene.

**Make your own breeds.** A breed is where a whole idea lives: not a
"zombie gene" but a zombie *breed* - rotted-looking markings, a bad temper,
slow and tough. The in-browser
[breed designer](https://9-7-8.github.io/Procedural-Horse-Coats/wiki/breed-designer/)
walks you through one step at a time - colour, size, dilutions, white, eyes,
magical markings, health, diet, temper, abilities, speed, jump, where it lives -
and shows the horses it makes as you go. Export it (any time; it saves your
progress), put the file in `.minecraft/phc/breeds/`, and restart: the game
starts spawning it in the biomes you picked, and the cowboy, stables and spawn
eggs know it too. `.minecraft/phc/breed-spawning.toml` (below) is where you
switch off or reshape the breeds the mod ships, if yours should have the world
to themselves. The `phc` folder is made the first time the game runs with
the mod, and the **Breeds** tab of the in-game **H** menu has a button that opens it.

**A whole world of it.** A player-built portal leads to a self-contained
horse dimension for browsing genotypes, plus stallion seed jars, whistles,
shearing, a gene database you fill in by discovery, and more.

All of that is documented in depth on **[the wiki](https://9-7-8.github.io/Procedural-Horse-Coats/)**
rather than here - it's the source of truth for every gene, every rule, and
every system, and it stays current as the mod grows.

---

## Installation

1. Install **[NeoForge](https://neoforged.net/)** for **Minecraft 26.1.2**.
2. Download the `.jar` from
   **[Releases](https://github.com/9-7-8/Procedural-Horse-Coats/releases)** and drop it
   into your `mods/` folder.
3. Launch. No configuration is required.

Works client-side and on dedicated servers; both sides need the mod.

The mod's settings live in **`.minecraft/phc/`**, beside the breeds folder -
`breed-spawning.toml`, `server.toml` and `client.toml`.

**`breed-spawning.toml` decides which horses your world has.** Every breed the mod
ships has a section - switch it off, make it rarer or commoner, move it to other
biomes, or give it day or night hours - and one switch turns all of them off at
once. Feral Mixed, the unbred wild horse, has its own section too. A wild horse that
has nothing it is allowed to be simply does not spawn, so turning off the shipped
breeds and Feral Mixed leaves a world with only the breeds you made, living only
where you put them.

Two things in `server.toml` are worth knowing about, because
they are the only two the mod turns off for you. `health.mode` decides how much of
the disease layer a world plays with, and **`body.size` decides whether the size
genes actually resize the horse** - on by default, but a Falabella and a Percheron
have differently-sized *hitboxes*, so if you would rather every saddle, lead and
fence gap sit exactly where vanilla puts it, set it `false`. Both are server-side,
and neither changes what a horse carries or passes to its foals.

**Every release so far is a development release**, and there is no back-compatibility
layer - a new version will not load a world made by an older one. What changed in each
is on
**[the releases page](https://9-7-8.github.io/Procedural-Horse-Coats/wiki/releases.html)**.

**Currently built for 26.1.2 only.** Porting to other Minecraft versions -
both backward (a long-planned 1.12.2 backport) and forward, as new versions
release - is on the roadmap, but not yet built. The genetics/coat engine was
deliberately written as a Minecraft-free Java module for exactly this reason,
so a port is mostly wiring rather than a rewrite. If you'd like to help with
one, see below.

---

## Building from source

Requires JDK 25 (the build provisions it automatically if you don't have it).

```bash
./gradlew build
```

The built jar lands in `neoforge-26.1.2/build/libs/`.

---

## Looking for help

This is a solo project and there's more of it planned than one person can
build alone. If any of this sounds fun, **open an issue or a discussion on
the [GitHub repo](https://github.com/9-7-8/Procedural-Horse-Coats)** - a
"I'd like to work on X" is a great way to start.

- **New magic genes.** The magical side of the mod (particle trails, glow,
  size/speed/health genes, water-walking, and so on) is deliberately
  open-ended, and the [gene creator](https://9-7-8.github.io/Procedural-Horse-Coats/wiki/gene-creator/)
  means a new *coat* gene doesn't need a line of Java. Effect-bearing genes
  (behavior beyond the coat) currently need a hand-written JSON block - see
  the [gene effects](https://9-7-8.github.io/Procedural-Horse-Coats/wiki/gene-effects.html)
  page - but the vocabulary is designed to grow.
- **New magic breeds.** The 49 real-world breeds are just the start; a breed
  built around a magical theme (rather than a real-world one) is completely
  uncharted territory and I'd love to see what people come up with.
- **3D modeling, especially.** This is the area I most want help with. The
  horse model, render layers, and any future mane/tail/marking geometry are
  all built by hand against Minecraft's model format, and I am not a 3D
  artist. If you are, or want to be, there is a lot of room to make the
  horses themselves - not just their coats - much better looking.

No contribution guidelines exist yet beyond "open an issue and let's talk" -
the project is small enough that that's still the right amount of process.

---

## License

**[CC BY-NC 4.0](LICENSE)** - Attribution-NonCommercial.

In short:

- **Share and adapt it freely**, for any purpose, including one you charge
  money for overall.
- **Credit this repository** and link back to it.
- **Don't sell a derivative with no free version available.** A modpack,
  plugin, or fork that includes any part of this project has to have a
  genuinely free way to get it - accepting donations or tips on an otherwise
  free release is fine; requiring payment is not.

The [full license file](LICENSE) has the complete plain-language summary and
links to the legal text; that file is authoritative if anything here reads
differently.
