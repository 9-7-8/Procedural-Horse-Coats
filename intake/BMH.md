The **Bosnian Mountain Horse**—**Bosanski brdski konj**—should be a rare, compact Balkan mountain workhorse with unusually strong breed evidence for a dark base-color and dun-focused genetic pool. Pure founders should overwhelmingly read as dark bay, bay dun, black, or mouse dun, with chestnut, gray, pinto, and leopard-patterned coats excluded under the modern breeding program; its exceptional niche is sure-footed, low-maintenance mountain durability rather than speed or jumping specialization. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

> **Schema caveat:** The supplied mod wiki could not be retrieved by the available reader. This JSON follows the readable format in your example. Before compiling, reconcile its field names, allele IDs, commonness enum, source enum, and folder location with the current `common/breed/spec/` implementation.

## Identity & flavour

The **Bosnian Mountain Horse**, in Bosnian **Bosanski brdski konj** or **Босански брдски коњ**, is the only indigenous domestic-horse breed of Bosnia and Herzegovina. It is also called the **Bosnian Pony**, but “pony” can undersell it: this is a true mountain utility horse, bred for difficult country and practical survival rather than a small ornamental animal. The breed arose from the hardy Balkan mountain-horse population shaped over centuries by Bosnia’s steep, rocky terrain, limited winter forage, and need for an animal that could work every day with minimal fuss. It remains a central piece of Bosnian rural heritage, even as mechanization and war-era disruption reduced its numbers and increased the need for conservation breeding. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

Bosanski brdski konj was made for **pack transport, farm draught, riding, forestry, and mountain travel**. Its historical value was simple: where wheeled transport could not go, a Bosnian Mountain Horse could. It carried people and supplies over narrow, steep paths, worked small agricultural holdings, and tolerated cold, heat, sparse forage, and rugged footing. Today, the same traits make it useful for trekking, small-scale farm work, conservation grazing, and rewilding projects; a herd has been maintained in Croatia’s Velebit Mountains as part of efforts to preserve the breed’s hardy, semi-wild character. [discoverthehorse](https://www.discoverthehorse.com/breeds/bosnianmountainhorse)

A Bosnian Mountain Horse commonly stands about **12.3–14 hands**. It is compact, broad through the chest, short-backed, strongly boned, and built low enough to feel stable on a slope rather than tall and rangy. The head is generally plain and practical, often with a straight profile; the neck is short and muscular; the legs are sturdy; and the feet are durable for stone, mud, mountain paths, and poor pasture. It is not known for a huge draft-horse mane or elaborate feathering, though its practical coat and dense body help it cope with variable upland weather. The silhouette should say “small mountain pack horse”: deep-bodied, tough, and balanced rather than refined or flashy. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

The modern color identity is unusually strict and genetically interesting. A recent study genotyped **313 Bosnian Mountain Horses** using the International Association of Bosnian Mountain Horse Breeders database and found that **dark bay and black were the most representative colors**. The breeding program allows colors other than gray, pinto, chestnut, and spotted, provided the horse has no white markings; it also carries a measurable dun allele frequency of **0.09**, higher than older register-based prediction. The breed’s defining pure-founder palette should therefore be dark bay, ordinary bay, black, bay dun, and mouse dun—not gray, red, cream-dilute, pinto, or leopard-spotted. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

Bosnian Mountain Horses are hardy, tractable, frugal, sure-footed, and capable of long steady work. They should feel physically resilient rather than spectacular: not a Thoroughbred sprinter, not a purpose-bred show jumper, and not a heavy draught giant. Their special quality is reliability in places that punish more specialized horses. In Procedural Horse Genetics, players should recognize a Bosnian Mountain Horse at a glance as a compact dark mountain horse, often carrying primitive dun shading, whose genes are worth preserving because the breed is rare and at risk. The mod does not model exact hoof strength, load carrying, terrain-specific balance, hoof wear, low-input feeding efficiency, mountain navigation, conformation judging, white-marking restrictions, or the real breeding association’s registration decisions. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

## Breed JSON

```json
{
  "id": "bosnian_mountain_horse",
  "name": "Bosnian Mountain Horse",
  "type": "natural",
  "notes": "The Bosnian Mountain Horse is defined by compact mountain conformation, strong bone and feet, sure-footed movement on rocky ground, ability to work on sparse forage, practical pack and farm utility, and a modern breeding program that favors dark coats without white markings. Procedural Horse Genetics does not model hoof-horn strength, terrain-specific balance, load carrying, forage efficiency, exact mane or feathering, white-marking extent, conformation inspection, association registration decisions, mountain trekking training, forestry work, or conservation-grazing behavior.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:meadow",
    "minecraft:grove",
    "minecraft:old_growth_pine_taiga",
    "minecraft:stony_peaks",
    "minecraft:plains",
    "terralith:alpine_highlands",
    "terralith:alpine_grove",
    "terralith:rocky_mountains",
    "terralith:highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 760,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.96,
      "e": 0.04
    },
    "agouti": {
      "A": 0.76,
      "a": 0.24
    },

    "dun": {
      "N": 0.91,
      "D": 0.09
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
    "speed": 4,
    "jump": 5,
    "health": 9,
    "size": [
      0.86,
      0.98
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Basis |
|---|---:|---|
| Extension | `E` 0.96 / `e` 0.04 | Chestnut is excluded by the modern breeding program, while dark bay and black are the representative accepted colors. A near-fixed `E` pool makes chestnut exceptionally unlikely in pure founders without treating the breed as genetically incapable of the allele forever.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Agouti | `A` 0.76 / `a` 0.24 | The population must generate both dark bay and black as the two most representative coat classes. A high `A` rate retains bay as the main outcome, while a meaningful `a` pool gives black founders without making black dominant. This is a phenotype-structured implementation estimate because the cited study reports ASIP genotypes and phenotype patterns but its accessible abstract does not provide a population-wide `A/a` frequency table.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Dun | `D` 0.09 | Directly follows the measured dominant-dun allele frequency of 0.09 in the 313-horse Bosnian Mountain Horse study. It should yield uncommon bay dun and mouse dun horses, a signature primitive accent rather than the population default.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Gray, pinto, chestnut, spotted | Forced wild type or near-absent | The modern Bosnian Mountain Horse breeding program permits colors other than gray, pinto, chestnut, and spotted; this JSON therefore excludes gray, tobiano, other loud white patterns, leopard complex, and chestnut-derived founders. White-marking restrictions cannot be fully represented by this locus-only file.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Cream and other dilutions | Forced wild type | No evidence was found that cream, pearl, champagne, silver, mushroom, or flaxen should be part of the contemporary registered founder pool. |
| Disorders | All clear | No credible Bosnian Mountain Horse carrier-rate study was found for the mod’s named disorders. Do not import disorder rates from unrelated Balkan, Arabian, Quarter Horse, Friesian, or Warmblood populations. |
| Speed | 4/10 | Reflects a steady mountain transport, farm, and pack horse—not a racing specialist.  [discoverthehorse](https://www.discoverthehorse.com/breeds/bosnianmountainhorse) |
| Jump | 5/10 | Its agility and mountain footing justify a modestly above-basic jump tendency, without inventing a show-jumping pedigree. |
| Health | 9/10 | Represents strong ecological hardiness, low-input survival, and mountain durability. It is a gameplay heartiness score, not a disease-resistance claim.  [discoverthehorse](https://www.discoverthehorse.com/breeds/bosnianmountainhorse) |
| Size | ×0.86–0.98 | Produces a compact horse/large pony impression appropriate to the documented 12.3–14-hand range, while keeping direct real-world height out of the JSON.  [theequinest](https://theequinest.com/breeds/bosnian-pony) |

## Code map

| Location | Bosnian Mountain Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `bosnian_mountain_horse` record, natural type, display name, notes, sources, biome list, price, commonness, gene pools, disorder policy, and stat targets. |
| `common/breed/Breeds` | Register `bosnian_mountain_horse` for menus, books, spawning, founder creation, persistence, and breed lookup. |
| `common/breed/BreedSource` | Use existing `wild`, `cowboy`, `spawn_egg`, and `stable` sources. |
| `common/breed/BreedBands` | Accept an empty epigenetic-band map. Dark colors are created through the extension/agouti pool rather than a forced shade band. |
| `common/breed/spec/` | Serialize and deserialize the breed file in both directions. Replace proposed field and allele names with actual schema identifiers where needed. |
| `common/breed/Commonness` | Verify the current numerical ladder for `RARE`; this breed intends a `spawn_weight` of 1.5. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 5, health 9, and size ×0.86–0.98 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only `E/e`, `A/a`, and `D/d` from the listed pools. Force all omitted coat loci wild type and every disorder locus clear. |
| `common/breed/BreedLineage` | No breed-specific lineage override is needed. |
| `common/genetics/SpliceOutcome` | No special handling is needed; splice-carrot alleles use ordinary transmission logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use the existing stat axes and target-band objects for compact size and mountain-work performance. |
| `common/breed/BandType` | Do not add a special `TRADITIONAL` or `BACHELOR` case unless the live schema requires explicit band-type selection. |

## Verification

1. Confirm that **Bosnian Mountain Horse** appears in the H-menu’s Breeds tab and in the breed book with the natural classification, rare commonness, mountainous biome distribution, source list, price, and notes.

2. Trigger herd spawning in an enabled meadow, windswept hill, grove, or mountain biome. Every animal generated as part of one breed-selected pack must identify as **Bosnian Mountain Horse**, even when the herd includes bay, black, bay-dun, or mouse-dun individuals.

3. Confirm a lone wild horse that was not produced through a breed-pack selection path is labeled **Feral Mixed**, not Bosnian Mountain Horse.

4. Generate at least 1,000 pure founders and inspect genotype counts. Extension should remain overwhelmingly `E`; agouti should produce mostly dark-bay/bay horses with a meaningful black minority; the dun allele should approach 9% over a large sample.

5. Confirm that pure founders cannot generate chestnut, gray, cream, pearl, champagne, silver, mushroom, tobiano, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical coat phenotypes.

6. Confirm that all listed disorder loci remain clear across a large founder sample. Any disease allele should only enter a Bosnian Mountain Horse-descended line by later outcrossing or explicit mod genetics intervention.

7. Compare the breed to an ordinary riding horse, a dedicated racer, and a sport jumper. Bosnian Mountain Horses should read as compact, sturdy, and unusually hearty; they should be reliable climbers and explorers without surpassing specialized horses in flat speed or jumping power.

## Sources

- [Cotman et al. — “Variation in the ASIP and DUN Genes Responsible for Coat Colour in Bosnian Mountain Horses”](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405): study of 313 Bosnian Mountain Horses drawn from International Association of Bosnian Mountain Horse Breeders records. It reports dark bay and black as the most representative colors, a dominant-dun allele frequency of 0.09, genotype/phenotype discrepancies, and the breeding-program exclusion of gray, pinto, chestnut, and spotted horses. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

- [Schrimpf et al. — “Genome-Wide Homozygosity Patterns and Evidence for Selection in a Set of European Horse Breeds”](https://pmc.ncbi.nlm.nih.gov/articles/PMC6679042/): peer-reviewed genomic context for Bosnian Mountain Horse size selection, including the breed’s long-term selection toward approximately 135 cm at the withers and high prevalence of small-allele homozygosity at analyzed body-size loci. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6679042/)

- [Veterinaria Faculty of Sarajevo — “Evaluation of the Genetic Diversity and Population Structure of Potential Bosnian Mountain Horse”](https://veterinaria.unsa.ba/journal/index.php/vfs/article/download/74/74/474): Bosnian academic population-genetics context for identifying and preserving autochthonous Bosnian Mountain Horse germplasm. [veterinaria.unsa](https://veterinaria.unsa.ba/journal/index.php/vfs/article/download/74/74/474)

- [Rewilding Europe — Bosnian Mountain Horse herd in Velebit](https://rewildingeurope.com/news/five-more-hardy-bosnian-mountain-horses-now-reinforce-the-wild-living-herd-in-velebit/): conservation and rewilding context, including a wild-living herd in Croatia’s Velebit Mountains and the breed’s endangered status. [rewildingeurope](https://rewildingeurope.com/news/five-more-hardy-bosnian-mountain-horses-now-reinforce-the-wild-living-herd-in-velebit/)

- [Discover the Horse — Bosnian Mountain Horse](https://www.discoverthehorse.com/breeds/bosnianmountainhorse): practical breed context on historical transport and agricultural utility, sure-footedness, climate tolerance, low-care hardiness, and modern preservation. [discoverthehorse](https://www.discoverthehorse.com/breeds/bosnianmountainhorse)