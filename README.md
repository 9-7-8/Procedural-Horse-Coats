# Horse Genetics

A NeoForge mod that gives every horse a **genotype** and builds its coat,
its stats, and its family from that. Instead of a fixed set of horse
textures, coat colour follows real Mendelian inheritance, foals take after
their parents, and every horse carries a name and a pedigree you can inspect.

- **Minecraft:** 26.1.2
- **Loader:** NeoForge 26.1.2
- **License:** CC BY-NC 4.0

---

## What it does

### Coats from genetics

There is no fixed set of horse textures. Every coat is drawn from scratch,
pixel by pixel, out of that horse's genotype - so two horses with the same
genes look the same, and a horse that inherits an unusual combination gets a
coat nothing else in the world has.

**The base colour** comes from two genes:

| Genotype | Coat |
|----------|------|
| `ee` (any agouti) | Chestnut - red all over |
| `E_` `aa` | Black |
| `E_` `A_` | Bay - red-brown body with black points, and a randomly-rolled amount of black climbing the legs and face. A high roll gives the near-black "seal" look. |

**Dilutions** then lighten whatever is underneath, so each one looks different
depending on the base it lands on:

| Gene | In the wild | What it does |
|------|-------------|--------------|
| Cream | uncommon | One copy golds the red - a bay becomes a **buckskin**, a chestnut a **palomino**. Two copies wash almost everything out: **perlino**, **cremello**. A cream horse never keeps pitch-black points. |
| Pearl | uncommon | Two copies (with no cream) give an apricot body with sepia points. One cream + one pearl acts like double cream. |
| Champagne | rare | Golds every base, each to its own colour - **gold champagne** on chestnut, **classic** (taupe) on black, **amber** (gold body, chocolate points) on bay. |
| Grey | uncommon | Greys **adults** only, over any base, into a **dapple grey** - a neutral grey coat patterned with rounded dapples. How far along a horse's greying is varies: some are a dark steel grey, some a mid dapple grey, some nearly white, and the ones that haven't got far keep a darker mane, tail and legs. A grey foal is born its base colour and turns grey when it grows up. |

**White markings** are the last layer:

| Gene | In the wild | What it does |
|------|-------------|--------------|
| Splash | uncommon | White socks and a face blaze, rolled per horse - no two are quite alike. |
| Dominant white | rare | Solid **white**, no markings. Masks everything else. |

Wild horses roll a random genotype when they spawn. Bred foals inherit one
allele from each parent at every gene, so colour passes down the way it does
in real horses - two black horses can still throw a chestnut foal if both
carry a hidden `e`, a hidden cream can surface generations later, and a single
hidden `W` turns a foal solid white.

Some traits aren't fixed by the genes alone. How far bay's black climbs, how
far along a grey has greyed, where splash puts its socks - none of that is
written in the gene itself, so a paddock of bays is a paddock of individuals,
not copies.

These details ride along **with the allele**, not with the horse. A wild horse
rolls them when it spawns and keeps them for life; a foal inherits them from
whichever parent gave it that allele. So a mare with black most of the way up
her legs passes that on to the foals that inherit her bay allele, and a line
can be bred toward a look as well as toward a colour.

### Names and pedigree

Every horse gets a generated two-part name (a first name and a last name).

- **Rename** with a **name tag**: the text before the first space becomes the
  first name, the rest becomes the last name. The name tag is consumed. One
  of the two halves may be left blank, but not both.
- **Barn name**: an optional short nickname (up to 16 characters) you can set
  and change at any time from the horse's inventory screen. If set, it's what
  shows above the horse.
- A bred foal is named after its parents. The **first** foal of a pair takes
  one parent's first name and the other's last name; the **second** gets the
  opposite mix; the next few keep just one name from a parent and roll the
  other half fresh; and once a pair has had six foals, the rest get entirely
  new names - so a prolific pair never keeps churning out the same two names.

Open a tamed horse's inventory (**press E while riding**) for a grey panel on
the left of the screen (toggle it with the tab button on its edge) showing
the horse's name, sex, generation, genotype, speed, health, and who bred or
tamed it - plus a **Family Tree** button that opens a clickable pedigree
chart back to great-grandparents, with a little turning 3D model of each
horse. A foal's speed and health are tinted **green** if they beat both
parents, **amber** if they beat one, **red** if they trail both.

Right-click a horse with **paper** to print the same information to chat.

### Inherited stats

Alongside the genotype, a foal's **movement speed** and **max health** are
rolled from its parents: each lands somewhere between **75% of the lower
parent and 150% of the higher one**, and is rounded up with **no cap**. So a
determined breeder can push a bloodline to ridiculous numbers over
generations - and there's always some spread. (These are random for now; a
future version folds them into the genetic model.)

### Riding through water

A **tamed** horse can now be ridden across water. It floats at the surface and
swims slowly in whatever direction you steer - handy for short crossings, not
a replacement for a boat.

### The horse dimension

A private, self-contained space that shows you **every horse in the game** -
a straight fenced corridor, lit and walled, with a mare and a stallion in
every pen. It isn't a random sample: there is exactly one pen for each coat
the genetics can produce, in order, and a **sign on the ground beside each
gate** naming that pen's genes. A sign just inside the entrance tells you how
many there are. Walk it end to end and you have seen the lot.

The two horses in a pen have identical genes, so any difference between them
is the random variation a horse is born with - handy for seeing how much a
single genotype can vary. Horses can't be hurt in here, and nothing can be
built or broken.

**Getting in:** build a rectangular frame out of **hay bales** (same sizes as
a nether portal - at least 2 wide and 3 tall on the inside, built vertically)
and right-click a frame block with a **golden carrot**. The opening fills
with a swirling portal. Stand in it and it spins up over **10 seconds** -
with a countdown in chat - then sends you through.

**Getting back:** every trip drops you next to a matching hay-bale portal at
the start of your corridor. Stand in it for 10 seconds to return to exactly
the portal you came from.

**Bringing horses home:** push a horse into the portal (it travels after
**3 seconds**), or right-click the portal while **leading horses on a lead** -
each roped horse walks in, its lead drops on the ground, and it follows you
through. When you step into the exit portal to leave, **every tamed horse
you own is teleported back with you** (if you're the last person in the
dimension, all tamed horses come out).

Each visit is its own fresh instance, and every horse and item in it is
cleared out once you leave - so anything you left behind, and any horse you
didn't tame, is gone for good.

---

## Installation

1. Install **NeoForge for Minecraft 26.1.2**.
2. Drop the mod `.jar` into your `mods/` folder.
3. Launch. No configuration is required.

Works client-side and on dedicated servers; both sides need the mod.

One optional client setting (`config/horsegenetics-client.toml`): the Family
Tree screen shrinks the whole chart to fit your window by default; set
`familyTree.scrollBar = true` to keep it full size and scroll instead.

---

## Building from source

Requires JDK 25 (the build provisions it automatically if you don't have it).

```bash
./gradlew build
```

The built jar lands in `neoforge-26.1.2/build/libs/`.

---

## License

Released under **CC BY-NC 4.0**. Forks and derivatives are welcome without
asking, but must credit this repository and link back, and no part of this
code may ship in a paid derivative that has no free version available.
Donations or tips on an otherwise-free derivative are fine.
