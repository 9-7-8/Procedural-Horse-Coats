The **Wielkopolski** is best modeled as a rare Polish warmblood with a broad, solid-color pool and a true sport-horse identity: athletic, long-lined, capable across riding, driving, cross-country, and jumping rather than narrowly specialized for one discipline. The strongest genetic evidence available is an unusually large Polish studbook analysis of **7,664** bay, black, and chestnut horses, which makes its extension and agouti pool more defensible than the usual phenotype guesswork. [eurekamag](https://eurekamag.com/research/003/455/003455834.php)

> **Schema caveat:** The Procedural Horse Genetics breed wiki could not be retrieved by the available reader. The JSON uses the proposed readable convention from your examples. Before compilation, adjust every field key, locus ID, source/commonness enum, and target-band structure to the current `common/breed/spec/` format.

## Identity & flavour

The **Wielkopolski**, also called the **Wielkopolska**, **Great Poland Horse**, or **Mazursko-Poznański**, is a Polish warmblood breed from central and western Poland, especially the historic Wielkopolska region around Poznań. It was formally created in **1964** by combining two related Polish types: the Poznań/Pozan horse of western Poland and the Mazury/Masuren horse of the northeast. The name *Mazursko-Poznański* preserves that two-population origin, while *Wielkopolski* ties the breed to Greater Poland. Today, the Polish Horse Breeders Association (**PZHK**) maintains the Wielkopolska studbook as one of Poland’s nationally recognized horse-breed books. [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski)

The Wielkopolski was made to be a versatile utility and riding horse. Its predecessors worked farms and transport routes, served as cavalry and military mounts, and were shaped by Trakehner, Thoroughbred, Arabian, Anglo-Arabian, and Hanoverian influence. That mix created a horse that can carry real bone and substance without becoming a draught breed: athletic enough for riding sport, practical enough for general work, and level-headed enough to function outside a single high-pressure discipline. Modern Wielkopolskis are especially associated with cross-country and eventing-type work, but they can also suit dressage, show jumping, driving, and all-round riding. [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski)

Wielkopolski horses generally stand **157–168 cm (15.2–16.2 hands)**, though individuals may fall outside that practical range. They have a fine, small, straight-profiled head with alert eyes; a long strong neck; sloping shoulders; a broad deep chest; a muscular, compact, deep body; powerful hindquarters; and well-defined legs with strong joints and tendons. The model should read as a substantial classic European warmblood: more solid and farm-rooted than a Thoroughbred, cleaner and more athletic than a coldblood, and less exaggerated than a modern specialized showjumper. [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski)

The breed standard accepts **any solid color**, and the historic studbook analysis focuses on bay, black, and chestnut—the core Wielkopolski palette. Bay and chestnut are commonly described as the leading colors, with black, brown, and gray also occurring. Cream-derived colors appear to exist but should be rare, while roan is specifically excluded from breeding in at least one detailed breed reference. The appropriate mod population is therefore bay-led, with a strong chestnut minority, some black, occasional gray, very low cream, and no roan. White should be restrained: the breed can have ordinary face and leg markings, but it is not a pinto or leopard-pattern specialty. [eurekamag](https://eurekamag.com/research/003/455/003455834.php)

The Wielkopolski matters in this mod because it rewards players who want to breed a durable, capable European warmblood without chasing a single visual gimmick. The goal is a tall, athletic, versatile horse whose genetics produce recognizable bay/chestnut/black sport-horse families and whose performance trend supports speed, jumping, and reliable heartiness. The mod does not model Trakehner percentage, Arabian refinement, pedigree eligibility, sport-horse conformation scoring, cross-country courage, dressage collection, carriage-driving skill, military history, training, rider compatibility, or competition results. [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski)

## Breed JSON

```json
{
  "id": "wielkopolski",
  "name": "Wielkopolski",
  "type": "natural",
  "notes": "The Wielkopolski is defined by a Polish warmblood pedigree, sport-horse conformation, long strong neck, sloping shoulder, deep muscular body, powerful hindquarters, clean strong limbs, and versatile ability in riding, driving, cross-country, and general work. Procedural Horse Genetics does not model Poznań-versus-Mazury ancestry, Trakehner/Arabian/Thoroughbred percentage, individual conformation inspection, dressage collection, cross-country courage, jump technique, harness skill, competition training, studbook eligibility, performance testing, rider compatibility, or athletic career results.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:grove",
    "minecraft:windswept_forest",
    "terralith:temperate_highlands",
    "terralith:lowlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1040,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.72,
      "a": 0.28
    },

    "grey": {
      "N": 0.93,
      "G": 0.07
    },
    "cream": {
      "N": 0.98,
      "Cr": 0.02
    },

    "dun": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },
    "champagne": {
      "N": 1.0
    },
    "silver": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 1.0
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 1.0
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
    "roan": {
      "N": 1.0
    },
    "rabicano": {
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
    "jump": 7,
    "health": 8,
    "size": [
      1.05,
      1.17
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Extension | `E` 0.70 / `e` 0.30 | A Polish study used 7,664 registered bay, black, and chestnut Wielkopolski horses across five studbook volumes to analyze the breed’s MC1R/extension structure. The accessible citation confirms the dataset but does not expose numerical allele frequencies, so the chosen rate is a transparent game approximation intended to keep chestnut common but secondary to black-pigment bases.  [eurekamag](https://eurekamag.com/research/003/455/003455834.php) |
| Agouti | `A` 0.72 / `a` 0.28 | The same study confirms a genuine basic-color population structured around bay, black, and chestnut. This pool favors bay, retains a meaningful black minority, and combines with `e` to create the strong chestnut component described in modern breed summaries. It is an interpretation, not a directly quoted ASIP rate.  [eurekamag](https://eurekamag.com/research/003/455/003455834.php) |
| Gray | `G` 0.07 | Gray exists among standard Wielkopolski colors but is not typically described as a leading coat. A low rate keeps it visible in long-term breeding without making it a breed signature.  [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski) |
| Cream | `Cr` 0.02 | Cream-derived individuals are described as existing but rare. A minimal cream pool allows unusual buckskin, palomino, smoky-black, and occasional double-dilute descendants while preserving the traditional solid bay/chestnut/black center.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Wielkopolski.php) |
| Roan | Forced wild type | A detailed breed reference states that roan horses are prohibited from breeding. The mod should therefore exclude `Rn` from pure founders even if related breeds or historical outcrosses could theoretically introduce it.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Wielkopolski.php) |
| Major white and leopard patterns | Forced wild type | The core breed standard calls for solid colors, and no reliable registry source was located supporting tobiano, leopard complex, splash, frame, or other major pattern alleles as a normal pure-founder Wielkopolski pool. Minor ordinary markings remain outside the locus model. |
| Disorders | All clear | No defensible Wielkopolski-specific carrier frequencies were located for ACAN dwarfism, PLOD1, MET, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | 7/10 | The Wielkopolski has light-horse ancestry and a sport-horse role, with historic transport/cavalry use and modern riding, driving, and cross-country aptitude.  [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski) |
| Jump | 7/10 | Cross-country and sport riding are repeatedly associated with the breed; this should be a clear strength without giving it specialist modern-showjumper dominance.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Wielkopolski.php) |
| Health | 8/10 | Encodes a robust, versatile warmblood built from practical Polish farm, cavalry, and riding ancestry—not immunity from disease.  [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski) |
| Size | ×1.05–1.17 | Produces a substantial 15.2–16.2-hand Polish warmblood type, with literal height retained only in flavor text.  [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski) |

## Verification

1. Confirm **Wielkopolski** appears in the H-menu’s Breeds tab and the breed book with its natural classification, rare commonness, managed source list, central-European grassland biome profile, price, and sport-warmblood notes.

2. Confirm no wild pack spawns Wielkopolskis because `wild` is intentionally omitted. Verify availability from cowboy sales, stable selection, and spawn eggs.

3. Confirm an ordinary lone wild horse reads **Feral Mixed**, not Wielkopolski.

4. Generate at least 1,000 founders. The population should be bay-led, with many chestnuts, a meaningful black group, occasional gray, and very rare cream-derived horses. It should never become a gray, pinto, leopard, roan, or dilute-focused breed.

5. Inspect loci. Only extension, agouti, gray, and cream should contain non-wild alleles. Every pure founder must be roan-clear, and all other named color, pattern, magical, and disorder loci should remain wild type or clear.

6. Verify trait interactions:
   - `E_ A_` produces bay.
   - `E_ aa` produces black.
   - `ee` produces chestnut.
   - `Cr` creates rare cream-derived variants on those bases.
   - `G` progressively masks the underlying base color according to the mod’s normal gray system.

7. Compare generated Wielkopolskis with a Polish sport horse, Thoroughbred, Trakehner, and heavier Polish farm horse. They should read as robust, athletic, tall all-round warmbloods: fast and jump-capable, but not as extreme as specialized racing or elite show-jumping lines.

## Sources

- [Polish Horse Breeders Association (PZHK)](https://www.pzhk.pl/en/pzhk/about-us/): official Polish studbook authority; confirms that PZHK maintains the Wielkopolska studbook and handles equine registration and passports in Poland. [pzhk](https://www.pzhk.pl/en/pzhk/about-us/)

- [Brodacki et al. — “Genetic Structure of Wielkopolski Horse Population with Respect to Basic Coat Colours”](https://eurekamag.com/research/003/455/003455834.php): primary study citation describing analysis of 7,664 bay, black, and chestnut horses recorded across five Wielkopolski studbook volumes at two basic-color loci. [eurekamag](https://eurekamag.com/research/003/455/003455834.php)

- [Wielkopolski breed overview](https://en.wikipedia.org/wiki/Wielkopolski): secondary source for 1964 formation, Poznań and Mazury foundation breeds, Trakehner/Arabian/Thoroughbred/Anglo-Arab influence, general conformation, 157–168 cm height, solid-color acceptance, and sport/farm versatility. [en.wikipedia](https://en.wikipedia.org/wiki/Wielkopolski)

- [Greater Poland breeders’ history](https://breedingnews.com/swiecicki-willingness-phenotype-and-good-health/): regional breeding-history context for nineteenth-century Wielkopolska horse registration, farm and transportation roles, cavalry-mount breeding, continuity through wartime, and modern regional performance selection. [breedingnews](https://breedingnews.com/swiecicki-willingness-phenotype-and-good-health/)

- [Wielkopolski sport-breed profile](https://hi3.horseisle.com/www/bbb/Wielkopolski.php): secondary supporting reference for cross-country association, historic breed development, common bay/chestnut palette, rare cream dilution, and stated roan exclusion. Treat its white-pattern claims cautiously; the pure-founder file uses the more conservative solid-color interpretation. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Wielkopolski.php)