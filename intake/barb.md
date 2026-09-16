The **Barb** should be implemented as a compact, gray-dominant North African riding and cavalry breed: hardy, economical, fast over distance, and strongly associated with the Maghreb rather than any single modern nation. Its pure founder pool should permit only bay, black, chestnut, and especially progressive gray, with modest face and leg white possible but no pinto, leopard, cream, dun, champagne, silver, or other dilute/pattern loci. The only directly relevant disease evidence found is a 2021 MENA study that detected one `PRKDC` SCID carrier among Arab-Barb horses—not pure Barbs—so the scientifically strict pure-Barb file should keep the requested disorder panel clear. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

## Identity & flavour

The **Barb**, also called the **Berber horse**, **Barbary horse**, or *cheval Barbe*, is the historic horse of the **Maghreb**: especially Morocco, Algeria, Tunisia, and adjacent parts of North and West Africa. It is inseparable from Amazigh/Berber culture and from the long history of mounted travel, raiding, trade, hunting, racing, and cavalry across the Atlas foothills, dry plains, scrub, and desert margins. The Barb is ancient in cultural origin, but it is not a single untouched prehistoric population; modern national studbooks and the international World Organisation of the Barb Horse have shaped contemporary breeding and conservation. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

The Barb was made for movement where feed, water, and comfort could not be assumed. It carried cavalrymen, tribal riders, messengers, hunters, and travelers across stony ground and long dry distances. The breed’s important qualities are not flashy suspension or enormous stride length, but **stamina, thrift, heat tolerance, fast recovery, sound feet, and fierce practical toughness**. Barbs influenced European and New World riding-horse populations through Iberian contact, warfare, trade, and colonial movement, although sweeping claims that every Spanish horse or Mustang descends directly from the Barb should remain flavourful historical context rather than a simple genetic fact. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

A typical Barb stands about **14.1–15.1 hands**, roughly 145–155 cm, and weighs around 400–500 kg. It is light but not delicate: a dry, narrow head often with a convex or Roman profile; long mobile ears; an arched neck; relatively flat withers; short back; sloping croup; low-set tail; deep chest; fine but strong limbs; and hard, durable hooves. The build can look angular, almost austere, beside a modern warmblood, but it is built to last. Mane and tail are usually full but ordinary, with no genetic feathering or mane-shape hallmark. [chevauxdumonde](https://chevauxdumonde.com/en/horse/barb-horse)

The traditional pure-Barb palette is deliberately restrained: **gray, bay, black, and chestnut**. Gray is commonly described as the most frequent or characteristic adult color; bay, black, and chestnut remain valid base coats beneath it. Small white facial or lower-leg markings may occur, but body spotting is outside the intended pure population. A gray Barb is born with a pigmented base coat and lightens over time; it is not a cremello, dominant white, silver dapple, or “white horse.” In the mod, a Barb herd should read as sun-country cavalry stock: mostly gray adults, then bay, dark bay/brown, black, and chestnut non-grays, all solid and spare-looking. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

Barbs are alert, brave, intelligent, independent, and enduring. They can be sensitive and quick rather than dull or placid, but their defining game role is not “difficult horse”; it is the player’s compact long-distance mount for dry, rough maps. Players should breed toward a small, elegant, gray-heavy survival riding horse with good travel speed and excellent heartiness. The mod does not model heat tolerance, water economy, hard hoof horn, recovery after exertion, Roman head profile, tail set, Arab-Barb versus pure-Barb registry distinctions, fantasia training, cavalry discipline, cultural heritage, race conditioning, or subjective courage and sensitivity. [chevauxdumonde](https://chevauxdumonde.com/en/horse/barb-horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the user-provided example. Before compiling, align all field names, locus IDs, allele symbols, source values, rarity enums, pricing units, and target-band syntax with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** Available registry and breed sources strongly support the Barb’s historical range, size, utility, and restricted base-color/gray palette, but no representative pure-Barb genotype survey was located for Extension, Agouti, or gray. The values below are transparent **gameplay approximations**, not documented Maghreb-wide allele frequencies. All requested disease loci remain clear because the retrieved MENA study detected SCID in one **Arab-Barb**, not in pure Barb horses, and it does not establish a pure-Barb carrier rate. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

```json
{
  "id": "barb",
  "name": "Barb",
  "type": "natural",
  "notes": "The Barb, also called the Berber or Barbary horse, is a Maghreb riding and cavalry breed defined by regional North African pedigree, Amazigh cultural history, heat tolerance, feed thrift, stamina, recovery, hard feet, dry Roman-profiled conformation, low tail set, and traditional fantasia and distance-riding use. Procedural Horse Genetics does not model heat tolerance, water economy, hoof hardness, exact head profile, ear length, tail set, Arab-Barb pedigree distinctions, regional Moroccan/Algerian/Tunisian studbook rules, cavalry training, fantasia performance, race conditioning, rider skill, or individual courage and sensitivity.",

  "biomes": [
    "minecraft:desert",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:plains",
    "minecraft:windswept_hills",
    "minecraft:meadow",
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
  "price": 760,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.65,
      "a": 0.35
    },

    "grey": {
      "N": 0.55,
      "G": 0.45
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
    "dun": {
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
    "jump": 5,
    "health": 9,
    "size": [
      0.96,
      1.07
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Barb × Barb produces Barb. Barb × another pure breed produces a Barb cross. A Barb cross bred back to pure Barb remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every gray North African or Iberian-type horse as a Barb: real Barb identity depends on regional lineage and studbook context, not a gray coat or Roman profile alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Population model | One Maghreb Barb pool | Barb populations exist across Morocco, Algeria, Tunisia, and neighboring regions, with national programs and international coordination. The file represents a generalized registered/traditional Barb rather than inventing unsupported country-specific genetic strains.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| Base colors | `E: 0.72 / e: 0.28`; `A: 0.65 / a: 0.35` | Produces bay, black/brown, and chestnut non-gray founders. Bay, black, chestnut, and gray are the documented traditional colors. The numeric values are gameplay approximations, not allele-frequency measurements.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| Gray | `G: 0.45` | Gray is widely described as the characteristic or most common Barb adult color. An allele frequency of 0.45 produces approximately 70% gray phenotype under random pairing, making adult herds visually gray-dominant while retaining a meaningful solid non-gray minority. This is a transparent game calibration.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| True gray | `grey`, not silver or white | A gray Barb should be born bay, black, or chestnut-based and progressively depigment. Do not use silver, cream, or dominant-white loci to make the classic pale adult appearance.  [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141306002836) |
| Dilutions | Cream, pearl, champagne, silver, mushroom, dun, and flaxen forced wild type | The available traditional-breed descriptions restrict the standard palette to bay, black, chestnut, and gray. Do not seed unverified dilution alleles merely because they occur in other North African or Iberian-related breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | Small face and leg markings may occur, but no source supports founding a pure Barb population with named pinto, roan, or body-white pattern alleles. The mod’s named loci would produce stronger, hereditary white patterning than the breed profile calls for.  [insiderhorse](https://insiderhorse.com/barb-horse/) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa/leopard patterning is not part of the supported Barb phenotype. |
| Magical loci | All forced wild type | Barb hardiness and desert reputation are natural traits, not magical coat expression. |
| Disorders | All named loci clear | The only retrieved MENA study detected `PRKDC` SCID in one Arab-Barb horse, not pure Barbs. No pure-Barb carrier frequency was established for SCID, CA, LFS, or the other requested disorders.  [madbarn](https://madbarn.com/research/investigation-of-cerebellar-abiotrophy-ca-lavender-foal-syndrome-lfs-and-severe-combined-immunodeficiency-scid-variants-in-a-cohort-of-three-mena-region-horse-breeds/) |
| Speed | `7/10` | Captures long-distance riding speed, cavalry utility, agility, and quick recovery, while staying below specialist flat-race breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| Jump | `5/10` | Barbs are athletic and capable, but the breed was not primarily selected for modern show-jumping scope. |
| Health | `9/10` | Represents heat tolerance, environmental thrift, stamina, strong feet, and ability to work on limited inputs; it does not assert disease immunity.  [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse) |
| Size | `×0.96–1.07` | Represents a compact 14.1–15.1-hand light riding horse, smaller than tall modern warmbloods but solidly horse-sized.  [chevauxdumonde](https://chevauxdumonde.com/en/horse/barb-horse) |

## Disorder approach

Every requested disorder locus is set to **clear** in the pure Barb founder pool.

This is the correct strict-evidence implementation. One 2021 survey of Arabian, Barb, and Arab-Barb horses in the MENA region identified carriers for CA and SCID overall, but the `PRKDC` SCID deletion was detected in nine Arabian horses and **one Arab-Barb**, not in a reported pure-Barb carrier. The study found no LFS mutant allele in its investigated cohort. That is evidence for testing in regional Arabian-influenced populations, not evidence for assigning any disease frequency to a general pure Barb founder pool. [madbarn](https://madbarn.com/research/investigation-of-cerebellar-abiotrophy-ca-lavender-foal-syndrome-lfs-and-severe-combined-immunodeficiency-scid-variants-in-a-cohort-of-three-mena-region-horse-breeds/)

Do not import Arabian-derived carrier frequencies for SCID, CA, or LFS into pure Barbs:

- Arabian SCID prevalence varies by country and sampled population. [vgl.ucdavis](https://vgl.ucdavis.edu/test/scid)
- A population rate for a pure Arabian breed cannot be assumed for the historically and genetically distinct Barb.
- Arab-Barb is a recognized composite type, but it is not synonymous with the pure-Barb pool this file represents.

If later data identifies a pure-Barb-specific mutation rate, add only that locus and label the sampling population, sample size, date, and whether the figure is **allele frequency** or **carrier prevalence**.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `barb` as a natural Maghreb riding breed with desert/Atlas biome mapping, gray-dominant solid color pool, high heartiness, and cavalry/endurance flavour. |
| `common/breed/Breeds` | Register `barb` for wild packs, cowboy and stable access, spawn eggs, breed books, H-menu display, commands, saved genomes, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. The real breed is managed, but wild availability is a reasonable gameplay representation of free-ranging or traditional North African horse landscapes. |
| `common/breed/BreedBands` | Support the empty epigenetic-band object. Do not use a shade band to imitate gray: gray must derive from the actual progressive-gray locus. |
| `common/breed/spec/` | Reconcile all illustrative key names, allele symbols, wild-type defaults, source serialization, and direct probability parsing with the actual breed-file specification. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the intended ladder and that `spawn_weight: 3` is valid in the engine’s weighted-spawn model. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 5, health 9, and size ×0.96–1.07 into legal `TargetBand` records. |
| `common/breed/BreedFounder` | Roll `E/e`, `A/a`, and gray from the declared pools; force all dilution, white-pattern, leopard, magical, and named-disease loci clear; then apply compact endurance-horse stat targets. |
| `common/breed/BreedLineage` | Apply standard pure/cross/Mixed behavior. A gray horse with a Roman profile must not gain Barb lineage from appearance, biome, or stat values. |
| `common/genetics/SpliceOutcome` | No Barb-specific exception. A spliced allele follows normal inheritance and may create an atypical descendant with dilution, pinto, leopard, or disease genetics excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize compact riding-horse size, above-baseline speed, moderate jump, and high heartiness bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual serializer requires a default field. |

## Verification

1. Confirm **Barb** appears in the H-menu’s Breeds tab and breed book with its Maghreb/Amazigh origin, compact cavalry-and-distance role, uncommon tier, broad dry-biome distribution, gray-dominant coat policy, and clear-disease note.

2. Spawn repeated packs in deserts, savannas, badlands, dry plains, windswept hills, and river-valley margins. Every horse in a selected generated pack must show **Barb** as its breed label.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is gray, compact, Roman-nosed, or generated in a desert biome.

4. Generate at least 1,000 pure Barb founders. Adult outcomes should be strongly gray-dominant, with a meaningful minority of bay, brown/black, black, and chestnut. Gray foals should be born on an underlying pigment base, then lighten normally with age.

5. Confirm that pure founders cannot produce cream, buckskin, palomino, pearl, champagne, silver dapple, mushroom, dun, flaxen chestnut, roan, tobiano, sabino, frame overo, splash, W-series white, rabicano, leopard complex, brindle, or magical phenotypes.

6. Inspect genomes:
- Only Extension, Agouti, and gray should vary.
- `G` should be common enough to make adult populations visibly gray-heavy.
- All other coat loci should remain wild type.
- All listed disorder loci must remain clear.

7. Confirm stat behavior. Mature Barbs should be compact rather than tall, quick over distance, highly durable, and moderately athletic at jumps. They must not surpass specialist Thoroughbreds in maximum speed, elite warmbloods in jumping, or giant draft horses in mass.

8. Test default lineage:
- Barb × Barb → Barb.
- Barb × Arabian → Barb cross.
- Barb × Andalusian/Lusitano → Barb cross.
- Barb cross × pure Barb → the existing Barb cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test gray inheritance:
- `G/N × N/N` should produce roughly half gray offspring.
- `G/N × G/N` should yield approximately 25% `G/G`, 50% `G/N`, and 25% `N/N` offspring over a large sample.
- Both `G/G` and `G/N` horses should gray progressively; neither should be rendered as genetically white from birth.

10. Test controlled outcrossing or splice inheritance. Add cream, dun, tobiano, leopard, roan, or a named disease allele through another breed or splice mechanics. The altered offspring should inherit normally but must not redefine the conservative pure-Barb founder population.

## Sources

- [Barb horse overview](https://en.wikipedia.org/wiki/Barb_horse): supplementary synthesis of Maghreb range, Amazigh association, hardiness, stamina, historical use, and recognized bay/black/chestnut/gray palette. [en.wikipedia](https://en.wikipedia.org/wiki/Barb_horse)

- [World Organisation / studbook context for the Barb](https://chevauxdumonde.com/en/horse/barb-horse): Maghreb breeding regions, 145–155 cm range, working and cavalry history, and modern international Barb organization/studbook context. [chevauxdumonde](https://chevauxdumonde.com/en/horse/barb-horse)

- [Barb Horse breed profile](https://insiderhorse.com/barb-horse/): supplementary overview of 14–15-hand height, gray frequency, solid-color restriction, modest face/leg white, stamina, cavalry role, and historical influence. [insiderhorse](https://insiderhorse.com/barb-horse/)

- [Investigation of CA, LFS, and SCID variants in MENA Arabian, Barb, and Arab-Barb horses](https://madbarn.com/research/investigation-of-cerebellar-abiotrophy-ca-lavender-foal-syndrome-lfs-and-severe-combined-immunodeficiency-scid-variants-in-a-cohort-of-three-mena-region-horse-breeds/): disease-screening context showing detected SCID was reported in Arabian and Arab-Barb horses, not establishing a pure-Barb prevalence. [madbarn](https://madbarn.com/research/investigation-of-cerebellar-abiotrophy-ca-lavender-foal-syndrome-lfs-and-severe-combined-immunodeficiency-scid-variants-in-a-cohort-of-three-mena-region-horse-breeds/)