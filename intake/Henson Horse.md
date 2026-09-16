The **Henson Horse**—*Cheval Henson*, the “Horse of the Bay of Somme”—should be a rare, consciously modern French trail breed with a nearly uniform **bay-dun** genetic identity. A pure Henson founder should always be dun and black-pigment capable, strongly favor agouti bay, and never generate red dun, grullo, gray, loud white patterns, or cream-derived colors; players should recognize it instantly as a golden-beige, dark-pointed, dorsal-striped coastal riding horse. [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265)

> **Schema caveat:** The provided Procedural Horse Genetics breed-wiki page could not be retrieved by the available reader. The JSON below follows the readable convention in your supplied example. Reconcile all key names, gene IDs, field types, source names, commonness enum values, and file location with the active `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Henson Horse**, French **Cheval Henson**, is a modern French saddle horse created in the **Bay of Somme**—particularly the Marquenterre area of Picardy, now Hauts-de-France. It is also known as the **Horse of the Bay of Somme**. The breed originated in the 1970s from Norwegian Fjord stallions crossed with local saddle-horse mares, including French riding, trotting, Thoroughbred-influenced, Anglo-Arabian, and Selle Français-type stock. The intent was unusually specific: retain the Fjord’s calm, primitive dun color, strength, and outdoor hardiness, but make the result taller, more athletic, and more comfortable for tourism and long trail riding. The Henson was officially recognized as a French breed in July 2003. [henson](https://www.henson.fr/en/the-henson-adventure/)

The Henson was made for **outdoor riding**, especially guided nature rides, trekking, beach-and-marsh exploration, endurance-style trail use, and riding tourism. In the Bay of Somme, Hensons have become part of the landscape as well as the visitor experience: calm enough for novice riders, robust enough to live outdoors in the coastal climate, and athletic enough to carry an adult on long rides through sand, wet grassland, pine woodland, and estuary country. It is not a specialist racehorse, a heavy draught animal, or an extreme sport jumper. It should feel like an exceptionally sensible, forward-moving all-day trail horse whose abilities invite players to explore rather than compete. [henson](https://www.henson.fr/en/the-henson-adventure/)

Hensons stand **1.50–1.60 m (14.3–15.3 hands)** at the withers. They resemble a refined Fjord: taller, less heavy, longer-legged, and more saddle-horse-like, but still broad through the chest and strong through the shoulder and hindquarters. The head is expressive and practical; the neck is set well on the shoulder but is commonly short and broad; the shoulder is long and sloping; the back and loin are strong; and the legs are solid enough for long days outdoors. Hooves should be dark rather than light. The mane may be solid black or two-toned black-and-gold/black-and-white, and the dorsal stripe is mandatory. Many Hensons also carry leg bars, reinforcing the breed’s unmistakable primitive-dun presentation. [energie-cheval](https://www.energie-cheval.fr/en/menu-secondaire/la-filiere/chevaux-de-territoire/cheval-de-territoire-henson/)

The Henson’s real genetic target is **bay dun**, called *baie sauvage* in French descriptions: a body from light sand to brown, dark points, dark mane and tail, and a black dorsal stripe. The current breeding goal is not simply “dun-colored horses”; it is the preservation of the bay-dun phenotype and removal of undesirable alleles while avoiding excessive inbreeding in a breed with relatively few active stallions. The breed standard allows bay, but the characteristic and preferred coat is dark beige-to-brown bay dun; gray, red-dun, grullo, cream, pearl, champagne, silver, white-patterned, and leopard-spotted founders would not belong in a strict Henson core. [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265)

The Henson is a cultural conservation success: it transformed a regional tourism idea into a recognized breed and a visible symbol of the Bay of Somme. Around 1,200 Hensons were reported in France, including roughly 200 at the Marquenterre breeding area, although that count is historical and should not be treated as a current census. In the mod, a player should recognize the Henson before reading its label: a medium-sized golden-bay trail horse, strongly marked with dorsal stripe and dark points, built to cross coastal wetlands rather than dominate a show ring. The mod does not simulate the Fjord-percentage rule of roughly 25–50%, mane trimming or two-tone mane structure, exact dorsal-stripe/leg-bar appearance, dark-hoof requirements, temperament suitability for novice riders, tourism training, coastal-wetland footing, or French studbook eligibility. [somme-tourisme](https://www.somme-tourisme.com/en/discover/the-somme-bay/henson-ride/)

## Breed JSON

```json
{
  "id": "henson_horse",
  "name": "Henson Horse",
  "type": "natural",
  "notes": "The Henson Horse is defined by its Bay of Somme origin, Fjord-derived bay-dun coat target, mandatory dorsal stripe, frequent leg barring, dark hooves, two-tone or dark mane, calm outdoor-riding disposition, and use in nature tourism and trail riding. Procedural Horse Genetics does not model the required 25–50% Fjord ancestry, mane trimming or exact two-tone mane pattern, dorsal-stripe width, leg-bar count, countershading, dark hoof pigment, novice-rider suitability, trekking training, beach and wetland footing, coastal pasture management, or French studbook and breeding-program eligibility.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:beach",
    "minecraft:stony_shore",
    "terralith:wetland",
    "terralith:marsh",
    "terralith:temperate_highlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 920,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 1.0
    },
    "agouti": {
      "A": 1.0
    },
    "dun": {
      "D": 1.0
    },

    "grey": {
      "N": 1.0
    },
    "cream": {
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
    "speed": 6,
    "jump": 5,
    "health": 8,
    "size": [
      1.00,
      1.11
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Extension | `E` 1.0 | A strict Henson founder pool should be fixed black-pigment capable. This prevents chestnut and red-dun founders, consistent with descriptions that Hensons do not produce red dun and with the breed’s targeted bay-dun standard.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Henson.php) |
| Agouti | `A` 1.0 | Fixing agouti produces bay rather than black as the underlying coat, preventing grullo/mouse-dun founders. The intended breed phenotype is bay dun, not generic “dun in any base color.”  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Henson.php) |
| Dun | `D` 1.0 | Every pure founder is dun. The dorsal stripe is a compulsory breed trait, and the Henson is explicitly characterized by its bay-dun coat. Dun is autosomal dominant and produces body dilution plus primitive markings through the *TBX3* locus.  [energie-cheval](https://www.energie-cheval.fr/en/menu-secondaire/la-filiere/chevaux-de-territoire/cheval-de-territoire-henson/) |
| No gray, cream, or other dilutions | Forced wild type | Gray, cream, pearl, champagne, silver, mushroom, and flaxen do not belong in the strict bay-dun founder pool and would undermine the immediately recognizable Henson silhouette.  [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265) |
| No white patterns | Forced wild type | White markings are discouraged in the breed and prohibited in breeding stallions to avoid their appearance in offspring. The mod cannot perfectly represent minor marking extent, so all named white-pattern loci are forced wild type.  [en.wikipedia](https://en.wikipedia.org/wiki/Henson_horse) |
| No roan, leopard, or brindle | Forced wild type | No reliable Henson breed-standard evidence supports roan, leopard complex, brindle, or related pattern loci in the core population. |
| Disorders | All clear | No defensible Henson-specific carrier-rate data was located for the listed inherited-disorder panel. The conservation work concerns color-genotype management and low active-stallion numbers, not published prevalence of these disorders.  [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265) |
| Speed | 6/10 | The Henson is a lightened Fjord-derived riding horse built for active trekking and outdoor tourism—not a racehorse, but more athletic and ground-covering than a pure Fjord or heavy draught type.  [henson](https://www.henson.fr/en/the-henson-adventure/) |
| Jump | 5/10 | A versatile trail horse should negotiate small natural obstacles, but the breed has no central sport-jumping selection history. |
| Health | 8/10 | Represents outdoor hardiness, sound trail use, and robust Fjord-derived heritage. It deliberately stays below maximum because a small breeding population requires active genetic-diversity management.  [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265) |
| Size | ×1.00–1.11 | Produces a medium riding horse slightly taller and more substantial than the generic baseline, consistent with the documented 14.3–15.3-hand range without placing literal height in the JSON.  [energie-cheval](https://www.energie-cheval.fr/en/menu-secondaire/la-filiere/chevaux-de-territoire/cheval-de-territoire-henson/) |

## Code map

| Location | Henson Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `henson_horse` natural-breed record with its name, notes, coastal biome list, controlled sources, rarity, price, bay-dun gene constraints, clear disorder panel, and body targets. |
| `common/breed/Breeds` | Register `henson_horse` for stable/cowboy stock, spawn eggs, menu and breed-book display, saved horse data, and genetic founder lookup. |
| `common/breed/BreedSource` | Enable `cowboy`, `spawn_egg`, and `stable`; omit `wild` because the Henson is a modern, deliberately managed French tourism breed rather than a free-ranging wild-herd population. |
| `common/breed/BreedBands` | Retain an empty epigenetic-band object. Fixed `E`, `A`, and `D` loci already provide a more accurate bay-dun identity than a cosmetic shade band. |
| `common/breed/spec/` | Add both deserialization and serialization support, replacing proposed JSON names and allele labels with the project’s actual code identifiers. |
| `common/breed/Commonness` | Verify that `RARE` maps to the intended `spawn_weight` of 1.5. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 5, health 8, and size ×1.00–1.11 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Force `E`, `A`, and `D` into every pure Henson founder and force all omitted coat loci wild type; generate all listed disorder loci as clear. |
| `common/breed/BreedLineage` | No Henson-specific behavior is required. |
| `common/genetics/SpliceOutcome` | Apply normal splice-carrot inheritance behavior; there is no Henson-specific exception. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use existing trait-axis and target-band types to model a medium-sized, trail-athletic, durable horse. |
| `common/breed/BandType` | Do not introduce a special `TRADITIONAL` or `BACHELOR` use unless the live code requires an explicit default. |

## Verification

1. Confirm **Henson Horse** appears in the H-menu’s Breeds tab and the breed book with the natural designation, rare commonness, Bay-of-Somme-style wetland/coastal biome description, managed source list, price, and notes.

2. Attempt wild herd generation in an eligible biome. No Henson pack should appear because `wild` is intentionally omitted. Confirm that the breed is available through cowboy offers, stable stock, and spawn eggs.

3. Confirm that an ordinary unassigned wild horse reads **Feral Mixed**, rather than Henson Horse.

4. Generate at least 500 Henson founders. Every one must be genetically `E_ A_ D_`; every visible founder should be bay dun with dark points and primitive dun markings. If the mod shows dorsal stripe and leg barring for `D`, every founder should display them according to normal renderer variation.

5. Verify that pure founders cannot generate chestnut, red dun, black, grullo/mouse dun, gray, palomino, buckskin, cream, pearl, champagne, silver dapple, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, brindle, or magical coats.

6. Inspect founder genomes directly. `extension`, `agouti`, and `dun` must be fixed to their desired Henson alleles; every remaining named coat locus must be wild type, and every listed disorder locus must be clear.

7. Compare generated Hensons with Fjords and ordinary saddle horses. They should feel like a taller, more riding-oriented Fjord derivative—calm, durable, moderately quick, and trail-competent—without outclassing specialist racers or jumpers.

## Sources

- [Association / Espaces Équestres Henson — “The Henson Adventure”](https://www.henson.fr/en/the-henson-adventure/): primary breed-organization source for Fjord-stallion × local-mare origin and official French recognition in July 2003. [henson](https://www.henson.fr/en/the-henson-adventure/)

- [Energie Cheval — Henson territorial-horse profile](https://www.energie-cheval.fr/en/menu-secondaire/la-filiere/chevaux-de-territoire/cheval-de-territoire-henson/): French regional equine-sector source for the Bay of Somme/Marquenterre origin, 1.50–1.60 m height range, mandatory dorsal line, common leg striping, Association du Cheval Henson, and 2003 recognition. [energie-cheval](https://www.energie-cheval.fr/en/menu-secondaire/la-filiere/chevaux-de-territoire/cheval-de-territoire-henson/)

- [Henson coat-color conservation thesis — “Couleur de la robe chez le cheval Henson”](https://dumas.ccsd.cnrs.fr/dumas-04411265): 2023 French academic thesis explicitly identifying bay dun as the Henson’s characteristic coat, examining desired and undesirable alleles, and proposing diversity-aware breeding plans for a small breed with few active stallions. [dumas.ccsd.cnrs](https://dumas.ccsd.cnrs.fr/dumas-04411265)

- [UC Davis Veterinary Genetics Laboratory — Dun Dilution](https://vgl.ucdavis.edu/test/dun-horse): authoritative genetic reference for dominant dun inheritance, its *TBX3* basis, pigment dilution, and primitive marking expression. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)

- [Somme Tourisme — Henson ride](https://www.somme-tourisme.com/en/discover/the-somme-bay/henson-ride/): regional tourism source for the Henson as a small, gentle, hardy golden horse and the historical approximate count of 1,200 in France, including 200 around Marquenterre. [somme-tourisme](https://www.somme-tourisme.com/en/discover/the-somme-bay/henson-ride/)