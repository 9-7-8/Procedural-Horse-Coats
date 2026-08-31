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

Each horse has a genotype at three loci - extension, agouti and white - that
decides its coat:

| Genotype | Coat |
|----------|------|
| `W_` (any) | Solid **white**, no markings - dominant, masks everything else. Rare in the wild. |
| `ee` (any agouti) | Chestnut |
| `E_` `aa` | Black |
| `E_` `A_` | Bay - with a randomly-rolled amount of black on the legs |

Wild horses roll a random genotype when they spawn. Bred foals inherit one
allele from each parent at each locus, so coat colour passes down the way it
does in real horses - two black horses can still throw a chestnut foal if
both carry a hidden `e`, and a single hidden `W` turns a foal solid white.

### Names and pedigree

Every horse gets a generated two-part name (a first name and a last name).

- **Rename** with a **name tag**: the text before the first space becomes the
  first name, the rest becomes the last name. The name tag is consumed. One
  of the two halves may be left blank, but not both.
- **Barn name**: an optional short nickname (up to 16 characters) you can set
  and change at any time from the horse's inventory screen. If set, it's what
  shows above the horse.
- A bred foal's name is built from its parents - the first name of one and
  the last name of the other.

Open a tamed horse's inventory (**press E while riding**) for a grey panel on
the left of the screen (toggle it with the tab button on its edge) showing
the horse's name, sex, generation, genotype, speed, health, and who bred or
tamed it - plus a **Family Tree** button that opens a clickable pedigree
chart back to great-grandparents. A foal's speed and health are tinted
**green** if they beat both parents, **amber** if they beat one, **red** if
they trail both.

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

A private, self-contained space full of horse pens - a straight fenced
corridor with a mare and a stallion in every pen, lit and walled so you can
walk it end to end. Good for seeing a lot of coats at once, or as a quiet
place to keep a breeding herd. Horses can't be hurt there.

**Getting in:** build a rectangular frame out of **hay bales** (same sizes as
a nether portal - at least 2 wide and 3 tall on the inside, built vertically)
and right-click a frame block with a **carrot**. The opening fills with a
portal. Stand in it for **10 seconds** to travel.

**Getting back:** every trip drops you next to a matching hay-bale portal at
the start of your corridor. Stand in it for 10 seconds to return to exactly
the portal you came from.

**Bringing horses home:** push a horse into the portal (it travels after
**3 seconds**), or right-click the portal while **leading horses on a lead** -
each roped horse walks in, its lead drops on the ground, and it follows you
through. When you step into the exit portal to leave, **every tamed horse
you own is teleported back with you** (if you're the last person in the
dimension, all tamed horses come out).

Each visit is its own fresh instance, and it's cleared out once you leave, so
any items or untamed horses left inside are gone for good.

---

## Installation

1. Install **NeoForge for Minecraft 26.1.2**.
2. Drop the mod `.jar` into your `mods/` folder.
3. Launch. No configuration is required.

Works client-side and on dedicated servers; both sides need the mod.

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
