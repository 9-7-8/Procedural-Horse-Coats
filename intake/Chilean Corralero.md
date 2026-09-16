The **Chilean Corralero**—formally the **Caballo de Pura Raza Chilena**, commonly the **Chilean Horse** or **Chilean Criollo**—should be a compact, dark-solid, high-heartiness stock horse specialized for cattle and Chilean rodeo. It is one of the clearest cases where the mod should favor *functional type* over flashy color: strong, agile, courageous, short-coupled, and small enough for steep terrain, with an official height range of about **13.1–14.2 hands**. [en.wikipedia](https://en.wikipedia.org/wiki/Chilean_horse)

## Identity & flavour

The **Chilean Corralero**, properly the **Chilean Horse** or *Caballo de Pura Raza Chilena*, is Chile’s national Criollo horse and the traditional mount of the **huaso**. “Corralero” refers to its cattle-working character: this is the horse of the *medialuna*, Chile’s crescent-shaped rodeo arena, where mounted riders guide and stop cattle with speed, balance, courage, and intense cow sense. The breed descends from Iberian horses brought to Chile by Spanish colonists in the sixteenth century, then shaped for nearly five centuries in a geographically isolated country of mountain passes, valleys, dry plains, and ranch country. [en.wikipedia](https://en.wikipedia.org/wiki/Chilean_horse)

The breed is one of the oldest continuously registered horse populations in the Americas. Its formal Chilean studbook began in **1893**, making it the oldest registered native horse breed in South America and among the oldest stock-horse registries in the Western Hemisphere. The registry was initially open to locally bred Chilean horses of the right type, then closed to unregistered animals in 1934. A detailed cow-horse breed standard followed in 1921. These dates matter to the mod’s identity: a Corralero is not merely an Iberian-looking ranch horse, but a deliberately conserved domestic lineage shaped by Chilean ranch work and the national rodeo tradition. [internationalequineinformation](https://internationalequineinformation.com/en/the-purebred-chilean-horse-for-conquistador-magazine/)

Chilean Corraleros are compact, muscular, broad, and exceptionally balanced. Mares ideally stand about **1.40 m** or 13.3 hands, while stallions ideally stand about **1.42 m** or 14 hands. The accepted range is 1.36–1.46 m for mares and 1.38–1.48 m for stallions, approximately 13.1–14.2 hands. They have a broad, expressive head; short-to-medium muscular neck; broad deep chest; short strong back; powerful, rounded hindquarters; short, sturdy limbs; thick skin; abundant often wavy mane and tail; little feather; and black, proportionate hooves with concave soles. It should look like a small but powerful cattle horse, not a tall Quarter Horse, a refined polo pony, or a lightly built Criollo racer. [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82)

All hair-coat colors are formally acceptable **except total or partial albino/white or cream-type washed-out coats**, though dark solid colors are preferred. In practice, black, dark bay/brown, bay, chestnut, dun, buckskin, palomino, and occasional gray or roan occur. The pure coat should be solid: normal face and leg markings are common, but pinto patterns and Appaloosa/leopard spotting do not belong in the baseline Corralero pool. Cream is appropriate only at a low enough frequency that double-cream foals remain uncommon; real registry standards exclude the washed-out white/cream end of the spectrum, and traditional breeders favor dark, covered coats. [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82)

Corraleros are famous for courage, trainability, cow instinct, abrupt athletic turns, physical toughness, and sure-footed travel over mountain terrain. In Procedural Horse Genetics, a player should breed toward a compact, dark, strong, highly durable South American stock horse with good acceleration and practical jumping—but it should not outclass a dedicated racer in pure speed or a European warmblood in show-jumping scope. The mod does not model cattle sense, medialuna rules, the *atajada*, rodeo scoring, ranch training, huaso tack, thick skin, wavy mane, concave hoof sole, mountain footing, subjective bravery, or actual registry eligibility. [internationalequineinformation](https://internationalequineinformation.com/en/the-purebred-chilean-horse-for-conquistador-magazine/)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics wiki linked in the original request could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the supplied example. Before compiling, reconcile exact field names, allele symbols, source enums, price units, `Commonness` names, `MATP` cream behavior, and stat-target serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The Chilean Horse standard provides unusually strong rules for height and accepted/excluded phenotypes, but it does not provide modern population-wide allele frequencies for `MC1R`, `ASIP`, `MATP`, dun, gray, roan, or marking loci. Numerical coat pools below are transparent **gameplay approximations**, constructed to make dark solid colors dominant while allowing documented buckskin, palomino, dun, gray, roan, and chestnut minorities. No direct Chilean Corralero carrier-frequency study was found for the requested disorder panel. [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82)

```json
{
  "id": "chilean_corralero",
  "name": "Chilean Corralero",
  "type": "natural",
  "notes": "The Chilean Corralero, formally the Caballo de Pura Raza Chilena or Chilean Horse, is a compact Chilean Criollo stock horse defined by closed-studbook ancestry, cattle work, Chilean rodeo, cow sense, courage, quick turning, mountain sure-footedness, thick skin, black concave hooves, abundant wavy mane and tail, and traditional huaso culture. Procedural Horse Genetics does not model cattle sense, medialuna competition, atajada technique, rodeo scoring, ranch training, huaso tack, thick skin, hoof shape, wavy mane, mountain footing, stamina conditioning, registry inspection, or subjective courage and rideability.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:grove",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 780,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.76,
      "e": 0.24
    },
    "agouti": {
      "A": 0.66,
      "a": 0.34
    },

    "cream": {
      "N": 0.93,
      "Cr": 0.07
    },
    "dun": {
      "N": 0.90,
      "D": 0.10
    },
    "grey": {
      "N": 0.94,
      "G": 0.06
    },
    "roan": {
      "N": 0.95,
      "Rn": 0.05
    },
    "flaxen": {
      "N": 0.93,
      "f": 0.07
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
    },

    "champagne": {
      "N": 1.0
    },
    "silver": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.97,
      "SB1": 0.03
    },
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_1": {
      "N": 1.0
    },
    "splash_white_2": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 1.0
    },
    "leopard_complex": {
      "N": 1.0
    },
    "patn1": {
      "N": 1.0
    },
    "patn2": {
      "N": 1.0
    },
    "brindle": {
      "N": 1.0
    },

    "magical_mane_tail": {
      "N": 1.0
    },
    "magical_healer": {
      "N": 1.0
    },
    "magical_zebra": {
      "N": 1.0
    },
    "magical_milk": {
      "N": 1.0
    },
    "magical_light": {
      "N": 1.0
    },
    "magical_particle": {
      "N": 1.0
    },
    "magical_verdant": {
      "N": 1.0
    }
  },

  "disorder_genes": {
    "ACAN_dwarfism": {
      "N": 1.0
    },
    "PLOD1_friesian_dwarfism": {
      "N": 1.0
    },
    "MET_lethal_white": {
      "N": 1.0
    },
    "PRKDC_SCID": {
      "N": 1.0
    },
    "TOE1_CA": {
      "N": 1.0
    },
    "MYO5A_LFS": {
      "N": 1.0
    },
    "GBE1_GBED": {
      "N": 1.0
    },
    "CVM": {
      "N": 1.0
    },
    "megaesophagus": {
      "N": 1.0
    },
    "SCN4A_HYPP": {
      "N": 1.0
    },
    "GYS1_PSSM1": {
      "N": 1.0
    },
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 7,
    "jump": 5,
    "health": 9,
    "size": [
      0.89,
      0.99
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Chilean Corralero × Chilean Corralero produces Chilean Corralero. Chilean Corralero × another pure breed produces a Chilean Corralero cross. A Chilean Corralero cross bred back to pure Chilean Corralero remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every compact dark Criollo-type cow horse as Chilean Corralero: real identity depends on the Chilean closed studbook and breed lineage, not rodeo suitability, coat, or body shape alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Closed Chilean Criollo stock-horse breed | The Chilean studbook began in 1893 and closed to unregistered animals in 1934. The record should therefore represent a recognizable managed breed, not generic South American feral horses.  [artbycrane](https://www.artbycrane.com/horse_breeds/light_horse_breeds/chilean.html) |
| Base colors | `E: 0.76 / e: 0.24`; `A: 0.66 / a: 0.34` | Produces a predominantly dark base-color population: bay, dark bay/brown, and black, with chestnut/sorrel a smaller component. The Chilean standard prefers dark solid colors and notes a bias against chestnuts, especially sorrels. Values are gameplay approximations.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Cream | `Cr: 0.07` | Buckskin, palomino, smoky black, cremello, perlino, and similar cream-derived outcomes are biologically possible in Chilean ancestry, but the Chilean standard excludes total/partial white or cream washed-out coats. A low allele rate keeps `Cr/Cr` outcomes very uncommon and preserves the dark preference.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Double cream | Engine must respect registry exclusion | `Cr/Cr` is viable biology, but its white/cream phenotype would be ineligible under the Chilean breed standard. The mod can still generate it after carrier matings, but breed-book flavour should mark it nonstandard rather than deleting its genotype.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Dun | `D: 0.10` | Dun is documented in Chilean Horse descriptions and suits the broad Criollo-derived palette. Keep it below bay/dark colors so it remains a minority.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Chilean.php) |
| Gray | `G: 0.06` | Gray occurs but is not preferred in the dark-solid-centered standard. Use a low rate.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Roan | `Rn: 0.05` | Roan occurs, but the official standard notes a preference away from roans. It should be a small minority rather than a breed hallmark.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Flaxen/rabicano | `f: 0.07`, `Rb: 0.03` | Low modifier rates add plausible variation in chestnut hair and subtle tail/flank white-hair texture. They are gameplay approximations, not registry frequency data.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Chilean.php) |
| Sabino 1 | `SB1: 0.03` | Face and leg markings are common, while solid coats are preferred. A very low expression-capable white-marking locus can provide ordinary markings, but must be removed if the renderer makes it pinto-like. |
| Pinto loci | Tobiano, frame, splash, KIT white forced wild type | The intended Corralero phenotype is solid. The standard allows normal markings but does not support pinto founder populations; `frame` is also excluded to avoid unsupported lethal-white risk.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type patterning is not part of the baseline Chilean Corralero presentation. |
| Other dilutions | Champagne, silver, pearl, mushroom forced wild type | No adequate source supports seeding them in the pure founder pool. |
| Disorders | All named loci clear | No defensible Chilean Corralero-specific carrier rates were found for the requested disease panel. Do not import Quarter Horse, Arabian, warmblood, or unrelated Criollo disease frequencies.  [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1939-1676.2008.0167.x) |
| Speed | `7/10` | Represents stock-horse acceleration, agility, rodeo response, and mountain travel, not elite flat-racing speed.  [theequinest](https://theequinest.com/breeds/chilean-corralero) |
| Jump | `5/10` | Athletic and sure-footed enough for terrain and general riding, but not a purpose-bred show-jumping population. |
| Health | `9/10` | Represents documented hardiness, robustness, mountain ability, and four-and-a-half centuries of working selection—not disease immunity.  [artbycrane](https://www.artbycrane.com/horse_breeds/light_horse_breeds/chilean.html) |
| Size | `×0.89–0.99` | Matches the strict 1.36–1.48 m range: compact and powerful, below standard large riding-horse height.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82) |

## Disorder approach

Every named disorder locus is **clear** in the pure Chilean Corralero founder pool.

This is a strict evidence decision. The breed has a closed long-running studbook and a culturally important, functionally selected population, but no credible Chilean Corralero-specific carrier-rate dataset was located for ACAN dwarfism, WFFS, lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

That means:

- Do not import `SCN4A` HYPP, `GBE1` GBED, or `PPIB` HERDA rates from Quarter Horses or Paint Horses merely because all are western-style cattle breeds.
- Do not import `PLOD1` WFFS rates from European warmbloods merely because Corraleros can jump.
- Do not import Arabian-line SCID, CA, or LFS rates because Iberian colonial ancestry is not Arabian population evidence.
- Do not seed `GYS1` PSSM1 based on generic muscle-disease studies. PSSM1 is a dominant disorder and requires direct population evidence before entering a founder pool. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1939-1676.2008.0167.x)

The low `Cr` entry is **not** a disease. It allows the genetics engine to produce occasional cream dilutions, including rare `Cr/Cr` foals. Those foals would be biologically viable but outside real Chilean breed registration preferences, which is a registry rule rather than a genetic-disorder rule.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `chilean_corralero` as a natural Chilean stock-horse record, including Corralero/Caballo de Pura Raza Chilena naming, Chilean biome palette, dark-color pool, clear disease policy, and compact athletic stats. |
| `common/breed/Breeds` | Register `chilean_corralero` for wild packs, stable/cowboy access, spawn eggs, H-menu display, breed books, commands, save serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. Wild spawning is gameplay-oriented; real Corraleros are managed closed-studbook horses. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band object. Do not use bands to fake thick skin, wavy mane, black hooves, cow sense, or rodeo ability. |
| `common/breed/spec/` | Reconcile the sample format with the actual bidirectional parser/writer. Confirm `Cr` dosage behavior, whether `Cr/Cr` can be marked nonstandard without deleting its biology, and the exact locus IDs for gray, dun, roan, flaxen, and sabino. |
| `common/breed/Commonness` | Confirm that `UNCOMMON` maps to the intended rarity ladder and that direct `spawn_weight: 3` is valid. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 5, health 9, and size ×0.89–0.99 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll the dark-biased base-color and low-frequency dilution pool, force pinto/leopard/magical and all named disease loci wild type or clear, then apply compact high-heartiness stock-horse body targets. |
| `common/breed/BreedLineage` | Use standard pure/cross/Mixed behavior. A compact dark cattle horse should not gain Chilean Corralero lineage based only on coat, biome, or performance scores. |
| `common/genetics/SpliceOutcome` | No exception. Spliced alleles transmit normally, including an intentionally introduced pinto, leopard, disease, or double-cream outcome that lies outside typical pure-breed registration rules. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize small sturdy size, strong stock-horse speed, moderate jumping, and high health targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the real file format requires an explicit default. |

## Verification

1. Confirm **Chilean Corralero** appears in the H-menu’s Breeds tab and breed book with Chilean origin, *Caballo de Pura Raza Chilena* identity, cattle-and-rodeo role, dark-solid coat policy, compact 13.1–14.2-hand body range, and high-heartiness stat profile.

2. Spawn repeated packs in plains, dry meadow, savanna, badlands margins, windswept hills, groves, and river valleys. Every horse in a selected Corralero pack should display the **Chilean Corralero** lineage label.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even when it is short, dark bay, black, cow-horse-shaped, or generated in dry hill country.

4. Generate at least 1,000 pure founders. The sample should center on dark bay/brown, bay, and black; chestnut should be a minority. Smaller numbers of buckskin, palomino, dun, gray, roan, flaxen chestnut, and subtle rabicano outcomes may occur.

5. Confirm that pure founders do not generate champagne, silver dapple, pearl, mushroom, tobiano, frame overo, splash, strong dominant-white patterns, leopard complex, PATN spotting, brindle, or magical coats.

6. Verify cream-dose handling:
- `N/Cr × N/N` should transmit `Cr` to about half of offspring.
- `N/Cr × N/Cr` should yield approximately 25% `Cr/Cr`.
- The engine should render double-cream biology normally but flag or describe that phenotype as **nonstandard for the real Chilean registry**, rather than treating it as a disease or silently converting it to white-pattern genetics.

7. Verify ordinary inheritance:
- `G/N` founders should gray progressively, rather than appearing white from birth.
- `Rn/N` founders should be roan but retain dark head/points rather than progressively graying.
- `D` founders should show true dun dilution plus primitive markings.
- `SB1` should create only modest face/leg marking expression; remove it if it produces pinto-like body white.

8. Confirm all named disorder loci remain clear in a large pure-founder sample. No WFFS, HYPP, PSSM1, GBED, HERDA, SCID, CA, LFS, or other listed disease allele should originate from pure Corralero founders.

9. Test default lineage:
- Chilean Corralero × Chilean Corralero → Chilean Corralero.
- Chilean Corralero × Argentine Criollo → Chilean Corralero cross.
- Chilean Corralero × American Quarter Horse → Chilean Corralero cross.
- Chilean Corralero cross × pure Chilean Corralero → the existing Chilean Corralero cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Check mature stats. The breed should remain clearly compact and powerful, with good practical travel speed and stock-horse athleticism, moderate jumping, and excellent health/heartiness. It should not rival specialist Thoroughbreds in raw sprinting, European jumpers in scope, or giant drafts in size.

## Sources

- [Chilean Horse Breed Standard](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82): English-language Chilean Horse standard for ideal and accepted heights, coat-color rules, solid/dark preference, exclusions for white/cream washes, hoof preference, and 1977 height-rule revision. [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82)

- [Asociación de Criadores de Caballos de Raza Chilena—standard catalogue](https://m.caballoyrodeo.cl/portal_rodeo/pdf_catalogo.php?expo=224): Spanish primary-source catalogue for sex-specific height limits, chest circumference, cannon size, “all colors acceptable, solids preferred” rule, thick skin, wavy mane/tail, little feather, black hooves, and adult exhibition requirements. [m.caballoyrodeo](https://m.caballoyrodeo.cl/portal_rodeo/pdf_catalogo.php?expo=224)

- [Asociación de Criadores—exhibition catalogue](https://m.caballoyrodeo.cl/portal_rodeo/pdf_catalogo.php?expo=305): corroborating Spanish breed-standard material for height, solid-color preference, hoof characteristics, and mature-animal requirements. [m.caballoyrodeo](https://m.caballoyrodeo.cl/portal_rodeo/pdf_catalogo.php?expo=305)

- [The Purebred Chilean Horse](https://internationalequineinformation.com/en/the-purebred-chilean-horse-for-conquistador-magazine/): history of Spanish colonial origins, 1893 registry, 1934 closure, 1921 cow-horse standard, compact size, mountain sure-footedness, cow instinct, trainability, and Chilean rodeo role. [internationalequineinformation](https://internationalequineinformation.com/en/the-purebred-chilean-horse-for-conquistador-magazine/)

- [Chilean Horse breed overview](https://en.wikipedia.org/wiki/Chilean_horse): supplementary naming reference for Chilean Corralero, Chilean Criollo, and *Caballo de Pura Raza Chilena*. [en.wikipedia](https://en.wikipedia.org/wiki/Chilean_horse)