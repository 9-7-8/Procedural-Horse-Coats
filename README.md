# Horse Genetics

A NeoForge mod that gives every horse a **genotype** and builds its coat,
its stats, and its family from that. Instead of a fixed set of horse
textures, coat colour follows real Mendelian inheritance, foals take after
their parents, and every horse carries a name and a pedigree you can inspect.

- **Minecraft:** 26.1.2
- **Loader:** NeoForge 26.1.2
- **License:** CC BY-NC 4.0
- **Wiki:** open `index.html` in a browser - a page per gene, with its alleles,
  how common it is in the wild, and what it does to a coat

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
| Cream / pearl | uncommon | One gene with three versions, so a horse gets one of four looks. One **cream** golds the red - a bay becomes a **buckskin**, a chestnut a **palomino**. Two creams wash almost everything out: **perlino**, **cremello**. Two **pearls** instead give an apricot body with sepia points. And one of each - the rarest of them - comes out like double cream. A cream horse never keeps pitch-black points. |
| Champagne | rare | Golds every base, each to its own colour - **gold champagne** on chestnut, **classic** (taupe) on black, **amber** (gold body, chocolate points) on bay. |
| Grey | uncommon | Greys **adults** only, over any base, into a **dapple grey** - a neutral grey coat patterned with rounded dapples. How far along a horse's greying is varies: some are a dark steel grey, some a mid dapple grey, some nearly white, and the ones that haven't got far keep a darker mane, tail and legs. A grey foal is born its base colour and turns grey when it grows up. |
| Dun | uncommon | Comes in three versions. Full **dun** lightens the body but keeps its colour - a black becomes a mouse-grey **grullo**, a bay a tan **bay dun**, a chestnut a pale **red dun** - and paints on **primitive markings**: a dark stripe down the spine from nose to tail, and faint bars across the legs. The middle version is far more common and much quieter: no lightening at all, just the **dorsal stripe**, so you get an ordinary bay or chestnut with a darker line down its back. The third does nothing. |
| Silver | rare | Lightens **black pigment only**. A black horse gets a chocolate body and a near-white mane and tail (**silver dapple**); a bay keeps its red body but its points and mane lighten (**silver bay**). A chestnut carrying it looks no different - it has no black to touch. |
| Mushroom | rare, hidden | **Two copies** turn a chestnut a flat sepia-khaki. On a black or bay it does almost nothing, so it is another one you carry unseen and breed toward. |

**White markings** are the last layer, and they come from six separate genes -
because in real horses they do. A horse has two copies of each gene, so two
markings that live on the *same* gene can never appear together, while two on
*different* genes stack up:

| Gene | In the wild | What it does |
|------|-------------|--------------|
| KIT | see below | The big one: **eight** versions of a single gene, from a horse with just a star and a sock, through **sabino** (tall jagged stockings, a belly patch, a wide blaze), up to **sabino-white** at ninety per cent white, and finally **dominant white** - solid white, no markings, hiding everything else the horse carries. Because they are all one gene, a horse gets **at most two** of them, and can never be both sabino-white and dominant white. |
| Splash (two genes) | one of them on **most horses** | White as if the horse were **dipped in paint from below**: leg white with a clean, sharp edge, white up the belly, a broad blaze. Splash comes from **two** different genes, so a horse can carry it twice over - and one that does is markedly whiter than one carrying either alone. The milder of the two is on about **nine wild horses in ten**, the way a minimal splash allele really is: one copy is roughly what an ordinary horse looks like, and it is the *second* copy people notice. Breed two horses you caught wild and about **one foal in five** comes out bolder than either parent. |
| Roan | uncommon | White hairs mixed evenly through the **body** while the head, mane, tail and lower legs stay solid - blue roan on a black, red roan on a chestnut. The density varies horse to horse. |
| Tobiano | rare | Big, smooth-edged **white patches that cross the topline**, with white legs and a coloured head. Every tobiano's patches are different. |
| Frame overo | rare | Ragged white on the **sides of the neck and barrel that stops short of the spine** - the mirror of tobiano - usually with a broad white face. Some carriers are marked so little you would never guess. **Careful with this one:** a foal that inherits frame from *both* parents is born pure white and does not survive. It happens one time in four when two carriers are bred. |

**The face** gets its own vocabulary, shared by all of those genes, so a marking
looks the same however the horse came by it. There are three pieces - a **star**
on the forehead, a **stripe** down the nose, a **snip** at the nostrils - and
every marking you can name is some combination of them: star and snip, star and
stripe, a stripe widening into a **blaze**, and a blaze widening into a **bald
face** that takes the eyes and the sides of the head. The more white a horse's
genes carry, the further along that ladder it tends to land, but it is a
tendency and not a rule: two horses with identical genes can wear a star and a
blaze. Star and snip are *detached* patches, with coloured face all round them.

And then there are the ones no real horse has:

| Gene | In the wild | What it does |
|------|-------------|--------------|
| Magic zebra | rare | Black **stripes**, hung from the spine and reaching down the horse's sides. They go black over *anything* - a cremello, a dapple grey, even a solid white horse. How far down they reach is its own horse's business: some are striped just across the back, some nearly to the hooves. |
| Pink hair | **hidden** | A **pink mane and tail**, whatever colour the rest of the horse is. **Both** parents have to pass it on before a foal has pink hair, so this is one you go looking for and breed toward rather than stumble across - the clearest of the mod's hidden genes, alongside pearl and mushroom. (A foal gets the tail; the mane comes in when it grows up.) |
| Waterborn | rare | **Neon-blue stripes** through the mane and tail, and the horse **walks on the surface of water** instead of wading. It leaves a trail of blue sparks where it treads, and a **tamed mare** can be milked with an empty bucket for a bucket of water. |
| Suntouched | rare | A **mane and tail of molten gold** that stay bright in the dark, a faint drift of gold sparks, and a body that **casts light** - a Suntouched horse lights the ground around it like a torch. |
| Mane colour | uncommon | A mane in **any colour there is** - one solid colour, or bands of colour running across it. A horse that inherits one of each gets *both*, in **two different colours**: banded over a solid base. The colour belongs to the copy it came from, so a mane you like breeds true. |
| Tail colour | uncommon | The same thing again for the tail, and it is a **separate gene** - so a horse can be one colour at one end and another at the other. A foal shows its tail colour straight away; the mane waits until it grows up. |
| Light | rare | Gold **hooves**, a gold **mane**, or gold **eyes**, all of which glow in the dark - and whichever it is, the horse **lights the ground** around it like a torch. There are three versions of this gene and none of them beats the others, so a horse carrying two shows *both*. |
| Healer | **hidden** | A horse that slowly **mends anyone standing near it**, marked by a red stripe down the middle of its mane. Both parents have to pass it on, so it is one to breed toward. The stripe is faint on some horses and vivid on others; that says nothing about the healing. |
| Magic body size | **very common** | Nearly every horse carries one copy of this, which is why no two horses in a paddock are quite the same size - usually about a tenth bigger or a tenth smaller, and how much is that horse's own and **passes to its foals**. Two copies of the same kind **add together**, and no wild horse is ever born with two: the really large and really small horses are ones somebody bred. One of each cancels out. |
| Milk | **hidden** | Put a bucket under a mare and get milk, as usual. But two copies of one hidden version and **any** horse fills the bucket with **water**; two of the other and it fills with **lava**. Neither shows in a single copy, so the only way to find one is to breed for it - and a water horse and a lava horse can never have a foal together. |
| Verdant | **hidden** | A horse that changes the ground it walks on: **mycelium**, **moss** or **grass** creeping out from its hooves, a block at a time. Three hidden versions, and each one needs **both** parents to pass on the *same* one. |
| Particle | rare | A horse that **trails something as it moves** - flames, souls, snowflakes, hearts, portal motes, drifting glyphs. There are **forty versions of this one gene**, so a horse can carry two of them and never more, and most pairs mean one of the two is hidden and only its foals will tell you it was there. Some pairs *do* show both at once. The **colour**, **where on the horse it comes from** - the head, the back, the hooves, the tail - and **how much** of it there is all belong to the copy that carries them, so no two horses of the same kind look alike, and a foal that inherits the copy inherits the exact look. About one wild horse in thirteen trails something; any *particular* one is far rarer than that. |

Wild horses roll a random genotype when they spawn. Bred foals inherit one
allele from each parent at every gene, so colour passes down the way it does
in real horses - two black horses can still throw a chestnut foal if both
carry a hidden `e`, a pearl hidden for generations can surface when two
carriers finally meet, and a single hidden dominant white turns a foal solid
white.

Some traits aren't fixed by the genes alone. How far bay's black climbs, how
far along a grey has greyed, how high the white climbs on a splash - none of
that is
written in the gene itself, so a paddock of bays is a paddock of individuals,
not copies.

These details ride along **with the allele**, not with the horse. A wild horse
rolls them when it spawns and keeps them for life; a foal inherits them from
whichever parent gave it that allele. So a mare with black most of the way up
her legs passes that on to the foals that inherit her bay allele, and a line
can be bred toward a look as well as toward a colour.

### Genes of your own

You can add genes without touching any code. Open `wiki/gene-creator/index.html`
in a browser: pick where on the horse your gene shows (legs, mane, face, a
stripe pattern, irregular patches) and what it does there (wash the colour out,
paint a marking, add colour that nothing else can cancel). It draws the result
on a 3D horse as you go, and you can flip through fifteen base coats to check it
still reads on a cremello and on a grey.

When it looks right, download the file and drop it into
`config/horsegenetics/genes/` in your game folder. Restart, and wild horses start
carrying it - it inherits, it breeds, and it gets its own slot in the genetic
code, exactly like the genes that ship with the mod. Delete the file to take it
away again.

A gene can also do things beyond colour - hand-written into the file for now,
not the creator: walk on water or lava, resist fire or fall damage, trail
particles as it moves, or let the horse be milked for a fluid. The
**Waterborn** gene above is the worked example.

Two things to know. **Adding or removing a gene changes the shape of every
horse's genetic code**, so horses you saved before the change won't load
afterwards - decide on your genes before you get attached to a herd. And a file
with a mistake in it is skipped with a note in the log rather than breaking
anything else.

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
the horse's name, sex, generation, genotype, speed, health, jump, size, any
disorders it has, and who bred or tamed it - plus a **Family Tree** button that
opens a clickable pedigree chart back to great-grandparents, with a little
turning 3D model of each horse. A foal's speed and health are tinted **green**
if they beat both parents, **amber** if they beat one, **red** if they trail
both.

Right-click a horse with **paper** to print the same information to chat.

### Stats you can breed for

A horse's **speed**, **max health**, **jump strength** and **size** are not
rolled. They come out of its genes, exactly the way its colour does - so two
horses with the same genotype are the same horse, and the only way to make a
line faster is to find and fix the alleles that make it faster.

Three separate genes contribute to speed, so it is a real breeding problem
rather than one switch to flip. One of them, the **sprint gene**, is a trade:
each copy makes the horse faster and costs it a heart, so the fastest horse
you can breed is also the frailest. A fourth gene decides how well a horse
jumps.

Two more decide **how big it is**. One makes horses taller - longer stride,
higher jump - and the other makes **ponies**: smaller, slower, lower-jumping,
and noticeably hardier. A pony is not a worse horse, it is a different one, and
which end of that you want is up to you. The difference is visible: a pony is
a genuinely smaller animal, and a smaller target.

A wild-caught horse is deliberately unremarkable. Everything at the top and the
bottom of the range is something you breed toward.

### Things that can go wrong

Real horses carry disorders, and so do these. **Seven genes** in the mod are
recessive diseases, and a wild horse is never affected by one - it survived to
adulthood, so at worst it is a **carrier**, and a carrier looks exactly like a
healthy horse. There is no way to tell by looking.

Breed two carriers of the same disorder and one foal in four is affected:

- **Two of them are survivable.** A dwarf foal grows up small, slow, and short
  of hearts, and it is yours to keep or not.
- **Five of them are not.** The foal is born, named, and added to your family
  tree, and then dies within a few seconds, with a message telling you which
  disorder it was. That message is worth reading: it tells you something about
  *both* parents at once.
- **One never gets that far.** Two carriers of it will feed, show hearts, and
  simply produce no foal at all - over and over, with any other partner working
  fine.

This is what makes a pedigree worth keeping. It is also the one part of the mod
you can turn off: a server setting (`health.mode` in
`config/horsegenetics-server.toml`) has three positions - the full thing, fewer
hearts but nothing dies, or no effect at all. The genes are inherited the same
way whichever you pick.

A couple of the colour genes carry a disorder too: a **doubly-silver** horse
has bad eyes and fewer hearts, a horse with a **double dose of splash** is
deaf, and **two frame overos** is the pure-white foal described above.

### Riding through water

A **tamed** horse can now be ridden across water. It floats at the surface and
swims slowly in whatever direction you steer - handy for short crossings, not
a replacement for a boat.

### The horse dimension

A private, self-contained space full of horses to look at - a straight fenced
corridor, lit and walled, two thousand pens long, with a mare and a stallion
in every pen and a **sign on the ground beside each gate** naming that pen's
genes.

Every pen rolls its own genome, and the roll has a floor under it: a pen
horse always shows **at least one coat gene beyond its base colour**, and
about half of them show a **magical** gene as well - so walking the corridor
is a tour of what the genes can do rather than a field of plain bays. Walk it
again, or leave and come back, and it is a different corridor.

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
