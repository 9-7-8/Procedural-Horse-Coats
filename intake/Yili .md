The **Yili**—also spelled **Ili**—should be modeled as a broad Chinese **riding, harness, light-draught, milk, meat, and racing horse** from the Ili River valley in Xinjiang. It is not one uniform phenotype: the breed includes Kazakh-rooted and improved Yili populations, while the racehorse group includes galloping, trotting, and pacing types. A well-designed file should therefore use strains, remain centered on bay and chestnut with black and gray minorities, and make it medium-small, hardy, fast, and versatile rather than an extreme specialist. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

## Identity & flavour

The **Yili Horse**, also known as the **Ili Horse**, is a modern Chinese breed from the Ili Kazakh Autonomous Prefecture in the Xinjiang Uyghur Autonomous Region of northwestern China. It developed in the twentieth century from local Kazakh mares and other regional stock improved with imported Russian and Soviet-derived stallions, including **Don**, **Budyonny**, and **Orlov Trotter** lines. Historical descriptions also record Mongolian influence in the wider foundation. The aim was not to replace the local horse with a European type, but to create a more capable regional animal that retained Kazakh hardiness and forage efficiency while gaining size, speed, harness quality, and working power. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

The Yili was bred for versatility. It carries riders across grasslands and mountain basins, works in light harness, pulls agricultural loads, serves as a local racehorse, and contributes mare’s milk and meat to regional pastoral economies. In 1963, breeders deliberately shifted selection toward a larger riding-and-draught type. Later selection created several performance-oriented groups, including galloping, trotting, and pacing Yili racehorse types. This makes Yili a particularly interesting mod breed: it should feel less like a narrowly fixed studbook horse and more like a productive, adaptable Xinjiang animal with multiple legitimate directions a player can breed toward. [nature](https://www.nature.com/articles/s41598-024-79014-w)

A typical Yili stands around **144–153 cm**, about **14.1–15.1 hands**, with many general references centering close to 14 hands. It is compact but well-made: light straight-profiled head, average-length neck, pronounced withers, short strong back, longer loin, deep sprung ribs, sloping shoulder, well-defined tendons, and clean legs. Some horses toe out in front or are cow-hocked or sickle-hocked behind—practical population variation, not traits the mod should imitate deliberately. The horse has a fine, light coat and may show a subtle metallic gloss, but that sheen is a coat-quality effect, not silver, champagne, cream, or a magical gene. There is no meaningful feathering, and mane/tail are ordinary full horse hair. [en.wikipedia](https://en.wikipedia.org/wiki/Yili_horse)

Bay is the most common documented color, followed by chestnut, black, and gray. The proper pure founder pool is therefore broad but conventional: bay should be the largest group, chestnut common, black/dark bay possible, and true progressive gray an established minority. The strong sources do not support a normal Yili founder population of cream, dun, champagne, silver dapple, pearl, mushroom, roan, pinto, leopard complex, or loud white patterns. Keep those loci clear in pure founders; the breed’s visual character is a clean, solid northern Xinjiang horse with practical variety, not a color project. [en.wikipedia](https://en.wikipedia.org/wiki/Yili_horse)

Yili horses are known for strength, endurance, speed, willingness, and resilience in continental grassland and mountain-basin environments. Their cultural footprint is broader than sport: they remain part of regional livestock systems and embody the connection between riding, farming, transport, racing, dairy production, and food security in Xinjiang. In Procedural Horse Genetics, players should recognize a Yili as a medium-small bay or chestnut utility horse with a compact powerful back, high heartiness, good travel speed, and a meaningful chance of gray. The mod does not model milk yield, meat production, local racing gait types, traction, metallic coat gloss, mountain-basin climate, Kazakh cultural management, exact limb angles, performance testing, pedigree classification, or the real distinction between Kazakh indigenous and improved Yili populations. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention from the supplied example. Before compiling, reconcile actual field names, locus IDs, source enum values, allele notation, `Commonness` names, strain syntax, and stat-target serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The 2024 Yili GWAS and 2025 genomic work provide strong support for Yili origin, regional diversity, performance subgroups, and the Kazakh-mare/introduced-stallion breeding history. The sources state that bay is most common and chestnut/black/gray occur, but I found no representative genotype-frequency table for `MC1R`, `ASIP`, or `STX17`. All coat values below are transparent **gameplay approximations**, not Chinese registry allele frequencies. No Yili-specific carrier rates were located for the requested disorder panel. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

```json
{
  "id": "yili",
  "name": "Yili",
  "type": "natural",
  "notes": "The Yili, also called the Ili Horse, is a Xinjiang Chinese riding, harness, light-draught, milk, meat, transport, and local-racing breed from the Ili Kazakh Autonomous Prefecture. Its real identity includes local Kazakh and Mongolian foundations, Don, Budyonny, Orlov Trotter, and Thoroughbred-influenced improvement, mountain-basin climate adaptation, mare-milk production, meat use, harness action, traction, galloping/trotting/pacing race types, metallic coat gloss, individual limb-angle variation, and local pastoral management. Procedural Horse Genetics does not model those traits directly.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:snowy_taiga",
    "minecraft:river",
    "minecraft:frozen_river",
    "minecraft:stony_peaks"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 780,
  "commonness": "UNCOMMON",

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
      "N": 0.84,
      "G": 0.16
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
      "N": 0.97,
      "f": 0.03
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.99,
      "SB1": 0.01
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
      "N": 0.99,
      "Rb": 0.01
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
      0.98,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "improved_yili",
      "name": "Improved Yili",
      "weight": 0.60,
      "notes": "The modern improved Yili population: a larger, balanced riding, harness, and light-draught horse selected from Kazakh mares with imported Don, Budyonny, Orlov Trotter, and related improvement lines. It should favor bay, chestnut, black, and gray without becoming a tall European warmblood.",
      "coat_genes": {
        "extension": {
          "E": 0.72,
          "e": 0.28
        },
        "agouti": {
          "A": 0.74,
          "a": 0.26
        },
        "grey": {
          "N": 0.84,
          "G": 0.16
        }
      },
      "stat_scores": {
        "speed": 7,
        "jump": 5,
        "health": 9,
        "size": [
          1.00,
          1.10
        ]
      }
    },
    {
      "id": "kazakh_root",
      "name": "Kazakh-Rooted Yili",
      "weight": 0.25,
      "notes": "The smaller, hardier Kazakh-rooted Yili population. It retains stronger local mountain-basin and pastoral character, with good forage economy and endurance, a modestly smaller frame, and a conservative bay/chestnut/black/gray pool.",
      "coat_genes": {
        "extension": {
          "E": 0.68,
          "e": 0.32
        },
        "agouti": {
          "A": 0.70,
          "a": 0.30
        },
        "grey": {
          "N": 0.87,
          "G": 0.13
        }
      },
      "stat_scores": {
        "speed": 6,
        "jump": 4,
        "health": 10,
        "size": [
          0.94,
          1.02
        ]
      }
    },
    {
      "id": "race_yili",
      "name": "Race Yili",
      "weight": 0.15,
      "notes": "A performance-selected Yili strain representing the breed's galloping, trotting, and pacing racehorse groups. It remains a Yili—not a pure Thoroughbred or Standardbred—but trends quicker, lighter, and slightly less rugged than the broader utility population.",
      "coat_genes": {
        "extension": {
          "E": 0.72,
          "e": 0.28
        },
        "agouti": {
          "A": 0.70,
          "a": 0.30
        },
        "grey": {
          "N": 0.82,
          "G": 0.18
        }
      },
      "stat_scores": {
        "speed": 8,
        "jump": 5,
        "health": 8,
        "size": [
          0.99,
          1.08
        ]
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Yili × Yili produces Yili regardless of Improved, Kazakh-Rooted, or Race Yili founder strain. Yili × another pure breed produces a Yili cross. A Yili cross bred back to pure Yili remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. The strains represent real production and performance groups inside the Yili population, not separate breed labels."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed structure | Three internal strains | Published Yili research identifies phenotypically and genetically distinct subgroups for meat, milk, and racing. Race Yili includes galloping, trotting, and pacing types; a Kazakh-rooted strain captures the local mare base, and Improved Yili represents the principal developed type.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.72 / a: 0.28` | Bay is documented as the most common Yili coat; chestnut, black, and gray also occur. High `A` makes bay dominant among black-pigment horses, while `e` retains a meaningful chestnut group. Values are gameplay approximations.  [en.wikipedia](https://en.wikipedia.org/wiki/Yili_horse) |
| Gray | `G: 0.16` | Gray is consistently reported among normal Yili colors. A moderate-low rate yields a visible but minority progressive-gray adult population. This is a gameplay estimate, not a genomic survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Yili_horse) |
| Flaxen | `f: 0.03` | A tiny optional modifier allows occasional light-maned chestnut without treating flaxen as a breed hallmark. It is an implementation approximation. |
| Metallic gloss | Not a pigment locus | Sources describe a fine coat with possible metallic gloss. That feature should not be represented through silver, champagne, pearl, cream, or a magical light gene; omit it unless the mod has a cosmetic non-Mendelian coat-quality layer.  [horsebreedspictures](https://www.horsebreedspictures.com/yili-horse.asp) |
| Excluded dilutions | Cream, pearl, champagne, silver, mushroom, and dun forced wild type | The source-supported baseline colors are bay, chestnut, black, and gray. No robust evidence supports seeding these other dilution genes in pure founders. |
| White patterns | Tobiano, frame, splash, KIT white, roan, leopard complex, PATN, brindle forced wild type | The desired default population is solid-bodied. Very low SB1/rabicano values allow only minimal ordinary marking texture if the mod renders them conservatively. |
| Disorders | All named loci clear | No Yili-specific carrier frequency was located for ACAN dwarfism, WFFS, frame-linked lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `7/10` overall; `8/10` Race Yili | Yili combines practical travel with local racing. The Race strain represents the selected galloping/trotting/pacing performance group, without confusing it with a pure race breed.  [nature](https://www.nature.com/articles/s41598-024-79014-w) |
| Jump | `5/10` | The breed is versatile, but no source supports specialist jumping selection. |
| Health | `9/10` overall | Represents disease resistance/adaptability of Kazakh-rooted stock and the breed’s practical endurance—not immunity from any particular genetic disease.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/) |
| Size | `×0.98–1.10` | Represents the usual 144–153 cm range, approximately 14.1–15.1 hands.  [equihorn](https://equihorn.com/breeds/yili-horse) |

## Disorder approach

Every named disorder locus is **clear** in the pure Yili founder pool.

That is an evidence-based restraint, not an assertion that Yili horses never carry inherited disease. The genetic literature retrieved for Yili focuses on population structure, immune regulation, racing-performance markers, and adaptation to Xinjiang conditions; it does not provide defensible carrier rates for the mod’s requested disorder set. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

Do not copy frequencies from constituent or neighboring populations:

- Don, Budyonny, Orlov Trotter, and Thoroughbred ancestry does not establish a present-day Yili WFFS, PSSM1, or other disease frequency.
- Kazakh-rooted ancestry does not establish a unique “cold-climate disease-free” pool.
- `GYS1` PSSM1 is dominant, so it should never be seeded as a harmless carrier gene without a direct frequency study.
- Arabian-line SCID, CA, and LFS should not be imported into an Xinjiang Chinese working population from historical general horse ancestry.

A future study reporting a direct Yili mutation rate may justify adding that exact locus. Until then, `N: 1.0` is the scientifically defensible implementation for the requested disease panel.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `yili` as a natural Xinjiang breed with three internal strains, plateau/grassland biome mapping, solid base-color pool, clear disorder policy, and versatile medium-horse stat targets. |
| `common/breed/Breeds` | Register `yili` for wild packs, stable access, spawn eggs, breed-book pages, H-menu display, commands, genome serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Yili is a regional Chinese working and pastoral breed, not a standard commercial sale-yard population. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band map. Do not fake metallic coat gloss, high-altitude adaptation, milk output, muscle, or gait type through coat-expression bands. |
| `common/breed/spec/` | Reconcile the illustrative schema with the real parser and writer. Confirm how strains override base gene pools and stat targets, plus actual `E/e`, `A/a`, gray, flaxen, source, and commonness identifiers. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the requested rarity ladder and that `spawn_weight: 3` is valid for weighted founder generation. |
| `common/breed/BreedStatCurve` | Convert overall and strain-specific speed, jump, health, and size values into legal `TargetBand` targets. |
| `common/breed/BreedFounder` | Choose Improved, Kazakh-Rooted, or Race Yili by weight; roll only its stated base-color/gray pool; force excluded coat and disease loci clear; then apply strain stats. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed logic. Crosses among Yili strains remain Yili, because the strains are internal production and performance groups. |
| `common/genetics/SpliceOutcome` | No special Yili exception. A splice-carrot allele follows ordinary inheritance and can create a nonstandard descendant with cream, pinto, leopard, disease, or other genes excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize medium-small size, good speed, moderate jumping, and high heartiness, with Race Yili’s speed bias and Kazakh-rooted health bias. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the concrete file format requires a default entry. |

## Verification

1. Confirm **Yili** appears in the H-menu’s Breeds tab and the breed book with Xinjiang/Ili Valley identity, Chinese riding/harness/light-draught role, three internal strain descriptions, bay-led palette, and high-hardiness profile.

2. Spawn packs in plains, meadow, windswept hill, grove, taiga margin, frozen-river, and mountain-basin-like biomes. Every horse in a selected Yili pack should display the **Yili** lineage label.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is bay, chestnut, medium-small, fast, or generated in a northern grassland biome.

4. Generate at least 1,000 pure founders. The population should be dominated by bay and chestnut, with dark bay/brown and black minorities and a visible but smaller progressive-gray population. No pure founder should be cream-dilute, dun, champagne, silver dapple, pearl, mushroom, pinto, leopard-spotted, roan, or magical.

5. Test strain behavior:
- About 60% should be **Improved Yili**, with the main medium-sized all-purpose profile.
- About 25% should be **Kazakh-Rooted Yili**, smaller and highest in heartiness.
- About 15% should be **Race Yili**, quicker and slightly lighter/less hardy.
- Every strain should retain exactly the **Yili** breed label.
- Crosses among internal strains must remain Yili.

6. Inspect coat genetics:
- `E/e` and `A/a` should make bay the most common phenotype.
- `e/e` should create a significant chestnut population.
- `E_ a/a` should create a smaller black population.
- `G` should produce normal progressive graying from a pigmented foal coat.
- Cream, dun, champagne, silver, pearl, mushroom, pinto loci, leopard complex, and true roan must remain wild type.

7. Confirm every named disorder locus is clear across a large pure-founder sample. No WFFS, HYPP, PSSM1, GBED, HERDA, SCID, CA, LFS, or other listed allele should originate in pure Yili founders.

8. Confirm adult stat behavior:
- Kazakh-Rooted Yili should be compact and exceptionally durable.
- Improved Yili should be the balanced riding/harness/light-draft horse.
- Race Yili should be the quickest.
- None should exceed specialized Thoroughbred speed, elite warmblood jumping, or giant-draught mass.

9. Test default lineage:
- Yili × Yili → Yili.
- Yili × Kazakh Horse → Yili cross.
- Yili × Don Horse → Yili cross.
- Yili cross × pure Yili → the existing Yili cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add cream, dun, tobiano, leopard complex, giant draught size, WFFS, PSSM1, or other excluded traits. The descendant should inherit normally but must not gain a pure Yili label from bay color, size, hardiness, or a northern biome.

## Sources

- [Genome-wide association study of racing performance in Yili horses](https://www.nature.com/articles/s41598-024-79014-w): peer-reviewed primary source for Ili/Kazakh Autonomous Prefecture origin; Kazakh mare × Orlov, Budyonny, and Don development; distinct meat/milk/racing subgroups; and gallop, trot, and pace racehorse types. [nature](https://www.nature.com/articles/s41598-024-79014-w)

- [Transcriptomic insights into immune traits and adaptation in Yili horses](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/): peer-reviewed source describing the breed’s combination of Kazakh disease resistance/adaptability with athletic contributions from Thoroughbred and Orlov Trotter sires. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12565139/)

- [Genetic diversity and population structure of Xinjiang native horse breeds](https://www.frontiersin.org/journals/genetics/articles/10.3389/fgene.2025.1439312/full): peer-reviewed genomic context for high diversity and frequent historical gene flow among Xinjiang populations, including Yili. [frontiersin](https://www.frontiersin.org/journals/genetics/articles/10.3389/fgene.2025.1439312/full)

- [Yili horse overview](https://en.wikipedia.org/wiki/Yili_horse): supplementary structured reference for approximately 14-hand height, compact conformation, 1900s origin, 1936-onward Don/Thoroughbred/Orlov improvement, and 1963 draft-type selection direction. [en.wikipedia](https://en.wikipedia.org/wiki/Yili_horse)

- [Yili breed profile](https://equihorn.com/breeds/yili-horse): supplementary source for 144–153 cm range, Ili Valley origin, functional roles, practical temperament, and broad profile. [equihorn](https://equihorn.com/breeds/yili-horse)

- [Yili Horse information](https://www.horsebreedspictures.com/yili-horse.asp): supplementary detailed source for two broad population groupings, height, bay/chestnut/black/gray color range, metallic gloss, conformation, uses, and historical development. [horsebreedspictures](https://www.horsebreedspictures.com/yili-horse.asp)