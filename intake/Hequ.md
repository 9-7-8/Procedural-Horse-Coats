The **Hequ** should be represented as a compact, high-altitude Chinese horse with three real regional varieties—**Jiaode**, **Suoke**, and **Kesheng**—rather than as three separate breeds. It is a Qinghai–Tibetan Plateau riding, pack, light-draught, and local-racing horse: sturdy, broad-chested, cold- and hypoxia-adapted, generally solid black, bay/brown, or gray, and substantially more robust than its 12.3–14.3-hand height suggests. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1365-2052.2010.02067.x)

## Identity & flavour

The **Hequ horse**, formerly known as the **Nanfan**, is an indigenous Chinese horse of the northeastern Qinghai–Tibetan Plateau. Its traditional center lies around the great bend of the Yellow River, spanning parts of Qinghai, southern Gansu, and western Sichuan, in a cultural landscape shared by Tibetan, Mongol, and other plateau pastoral communities. The breed has been important for centuries; some sources trace its recognizable regional identity to the Tang-era horse culture, though it is more accurate to say that the modern Hequ is a long-developed highland landrace, not a frozen relic from one exact dynasty. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

The Hequ was made for altitude. Its homeland is roughly **11,000 feet** above sea level, where the air is thin, winters are cold, summers can be cool and wet, forage is seasonal, and travel means long rides across grassland, river valleys, and mountain country. Hequs are used for riding, packing, light draught, local racing, and livestock work. They must carry a rider and gear, pull modest loads, recover after exertion, and remain useful where a larger lowland horse would struggle. In the mod, it should be a dependable expedition horse: a compact mount that feels unhurried but keeps going, handles hills, and stays healthy in high, cold country. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

A Hequ is a small horse, generally **130–150 cm** at the withers—approximately **12.3–14.3 hands**—and weighs about **330–400 kg**. It is solid rather than delicate: a medium-length to somewhat coarse head with a straight or slightly convex profile, broad forehead, large mobile eyes, long open ears, broad deep chest, medium-to-short thick neck, low-to-moderate withers, strong back, slightly sloping croup, medium legs, and broad hooves. The breed does not carry heavy feathering. Its mane and tail are full but ordinary, although the **Suoke** is known for high tail carriage reminiscent of horses in Tang-era art. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

The Hequ’s usual coat families are **black, bay/brown, and gray**. Gray is especially characteristic of the heaviest **Jiaode** population in southern Gansu; the Suoke and Kesheng populations retain a broader dark bay, brown, black, and gray mix. This is true progressive gray, not silver dapple and not mouse dun: a gray Hequ should be born on a dark underlying base and lighten progressively with age. Pure founders should not casually produce cream, champagne, silver, pearl, mushroom, true dun, pinto, leopard spotting, or magical colors. Small ordinary face or leg markings can occur in real domestic horses, but the mod’s named white-pattern loci would create a showier population than the documented solid-color Hequ type supports. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

The three varieties matter. **Jiaode** is the largest and heaviest, from southern Gansu, often gray, with a rougher constitution, broader coarser head, and comparatively weaker hooves. **Suoke**, from western Sichuan, is more compact and well coupled, with a relatively large head and ears, short loin, and high tail carriage. **Kesheng**, from Henan Mongol Autonomous County in Qinghai, has historical Mongolian-horse influence and forms the most practical all-round plateau type. In Procedural Horse Genetics, players should recognize the Hequ at a glance as a broad-chested, solid-colored Tibetan Plateau horse—mostly dark or gray—with compact mountain strength, modest size, and exceptional heartiness. The mod does not model hypoxia physiology, hemoglobin response, high-altitude metabolism, tail carriage, hoof quality differences, local saddle traditions, packing skill, local racing, Tibetan/Mongol pastoral culture, regional management, or the real boundary between the three varieties. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the provided example. Before compiling, reconcile exact gene IDs, `grey` locus syntax, strain format, source enum values, commonness labels, price units, and stat-band serialization against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The sources support the Hequ’s three varieties, range, high-altitude adaptation, and black/bay/brown/gray palette, but no representative genotype study was found giving Extension, Agouti, or `STX17` gray allele frequencies by variety. The coat values below are transparent **gameplay approximations**, not Chinese studbook percentages. The varieties are grounded in actual regional types; their weights and locus distributions are implementation choices. No Hequ-specific carrier-rate study was located for the requested disorder panel. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1365-2052.2010.02067.x)

```json
{
  "id": "hequ",
  "name": "Hequ",
  "type": "natural",
  "notes": "The Hequ, formerly called Nanfan, is a Chinese Qinghai-Tibetan Plateau riding, pack, light-draught, livestock, and local-racing horse defined by long regional adaptation to high altitude, cold weather, hypoxia, broad chest, compact mountain conformation, pastoral management, and the Jiaode, Suoke, and Kesheng varieties. Procedural Horse Genetics does not model oxygen physiology, altitude acclimatization, hemoglobin response, cold tolerance, seasonal forage, load carrying, local racing, pack training, hoof quality, high tail carriage, regional saddle traditions, Tibetan and Mongol pastoral culture, or real Chinese breeding-registration criteria.",

  "biomes": [
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:grove",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:snowy_slopes",
    "minecraft:plains",
    "minecraft:river",
    "minecraft:frozen_river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 720,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.74,
      "e": 0.26
    },
    "agouti": {
      "A": 0.64,
      "a": 0.36
    },

    "grey": {
      "N": 0.72,
      "G": 0.28
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
    "speed": 5,
    "jump": 4,
    "health": 10,
    "size": [
      0.88,
      1.00
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "jiaode",
      "name": "Jiaode",
      "weight": 0.35,
      "notes": "The Jiaode, also called Jiaoke, is the heaviest Hequ variety from southern Gansu. It has a rougher, broader, coarser head, a more substantial body, and comparatively weaker hoof quality. Gray is its predominant reported color.",
      "coat_genes": {
        "extension": {
          "E": 0.76,
          "e": 0.24
        },
        "agouti": {
          "A": 0.68,
          "a": 0.32
        },
        "grey": {
          "N": 0.48,
          "G": 0.52
        }
      },
      "stat_scores": {
        "speed": 4,
        "jump": 3,
        "health": 9,
        "size": [
          0.94,
          1.00
        ]
      }
    },
    {
      "id": "suoke",
      "name": "Suoke",
      "weight": 0.30,
      "notes": "The Suoke is the western Sichuan Hequ variety. It is compact and well coupled, with a comparatively large head and ears, short loin, and high tail carriage. It should remain a dark solid or gray mountain horse rather than a color strain.",
      "coat_genes": {
        "extension": {
          "E": 0.78,
          "e": 0.22
        },
        "agouti": {
          "A": 0.60,
          "a": 0.40
        },
        "grey": {
          "N": 0.80,
          "G": 0.20
        }
      },
      "stat_scores": {
        "speed": 5,
        "jump": 4,
        "health": 10,
        "size": [
          0.88,
          0.95
        ]
      }
    },
    {
      "id": "kesheng",
      "name": "Kesheng",
      "weight": 0.35,
      "notes": "The Kesheng is the Qinghai/Henan Mongol Autonomous County variety, historically influenced by Mongolian horses. It is the balanced all-round plateau type: usable for riding, packing, light draught, and local travel, with a broadly dark bay, brown, black, and gray founder pool.",
      "coat_genes": {
        "extension": {
          "E": 0.70,
          "e": 0.30
        },
        "agouti": {
          "A": 0.64,
          "a": 0.36
        },
        "grey": {
          "N": 0.78,
          "G": 0.22
        }
      },
      "stat_scores": {
        "speed": 5,
        "jump": 4,
        "health": 10,
        "size": [
          0.90,
          0.98
        ]
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Hequ × Hequ produces Hequ regardless of Jiaode, Suoke, or Kesheng founder variety. Hequ × another pure breed produces a Hequ cross. A Hequ cross bred back to pure Hequ remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Jiaode, Suoke, and Kesheng are real regional varieties within the Hequ breed, not separately labeled cross breeds."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | One Hequ lineage with three strains | Jiaode, Suoke, and Kesheng are established regional varieties of one Hequ breed. They should affect founder appearance and physical targets, not create three separate breed labels.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |
| Base colors | `E: 0.74 / e: 0.26`; `A: 0.64 / a: 0.36` | Produces a mostly black-pigment population—bay/brown and black—with a legitimate chestnut minority. The sources support black, bay/brown, and gray as major categories; the exact allele values are gameplay estimates.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |
| Gray | `G: 0.28` overall | Gray is a recognized common color and is particularly dominant in Jiaode. The pooled rate makes gray frequent while retaining substantial dark and bay non-gray founders. Rates are strain-calibrated gameplay estimates.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |
| True gray | `grey`, not silver | A gray Hequ should progressively lighten with age. Silver dapple acts on black pigment and is not the correct explanation for the breed’s ordinary gray population. |
| Jiaode color bias | `G: 0.52` | Jiaode is reported as predominantly gray. A 52% gray allele rate creates a strongly gray adult variety without forcing every individual gray.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |
| Suoke and Kesheng | Lower `G` values | Sources describe the breed-wide palette rather than a strict gray requirement for these varieties. Their lower rates preserve more black, brown, and bay mountain horses. |
| Dilutions | Cream, pearl, champagne, silver, mushroom, dun, flaxen forced wild type | No suitable source was found to treat those loci as normal pure-Hequ founder traits. The desired horse is solid black/bay/brown/gray, not a palette-driven breed. |
| White-pattern loci | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | No evidence supports a pinto, roan, or leopard-complex Hequ founder population. Ordinary incidental white markings are not grounds to seed major named pattern genes. |
| Leopard complex | `LP`, `PATN1`, and `PATN2` forced wild type | Appaloosa-type spotting does not belong in the supported solid Hequ phenotype. |
| Magical loci | All forced wild type | The breed’s high-altitude adaptation and high tail carriage are natural physiological and anatomical traits. |
| Disorders | All named loci clear | No Hequ-specific carrier-frequency data was located for the requested disease panel. High-altitude adaptation does not imply a known frequency for any modern diagnostic disease mutation. |
| Speed | `5/10` | Hequs are capable riding and local-racing horses, but their central selection is practical plateau travel, not specialist sprinting.  [breeds.okstate](https://breeds.okstate.edu/horses/hequ-horses) |
| Jump | `4/10` | Rough-terrain competence supports practical agility but not dedicated jumping selection. |
| Health | `10/10` overall | This represents exceptionally strong high-altitude, cold-country, and low-input hardiness—the mod’s closest proxy for hypoxia adaptation. It does not mean the breed is immune to injury, disease, or poor husbandry.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |
| Size | `×0.88–1.00` | Represents the 130–150 cm, roughly 12.3–14.3-hand range; strains retain meaningful size differences.  [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse) |

## Disorder approach

Every requested disorder locus is **clear** in the pure Hequ founder pool.

This is a strict evidence decision. Genetic studies of Hequ horses document population structure, genetic diversity, mitochondrial sequence, and adaptation to the Qinghai–Tibetan Plateau. They do not provide defensible carrier frequencies for ACAN dwarfism, `PLOD1` WFFS, `EDNRB`/frame lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1365-2052.2010.02067.x)

The absence of seeded disease alleles does **not** claim that every Hequ horse is free of every inherited disorder. It means no mutation is assigned a fake frequency merely because the breed is hardy, high-altitude adapted, or historically influenced by Mongolian horses.

Do not infer disorders from neighboring populations:

- `GYS1` PSSM1 is dominant and should not be inserted as a casual “carrier” allele.
- Arabian-associated SCID, CA, and LFS should not be copied into a Tibetan Plateau horse population.
- Quarter Horse-associated HYPP, GBED, and HERDA do not belong in this founder pool without direct evidence.
- Warmblood WFFS is not a default mountain-horse mutation.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `hequ` as a natural Chinese highland breed with three regional founder strains, plateau biome distribution, solid dark/gray coat pool, clear disease policy, and compact high-heartiness stats. |
| `common/breed/Breeds` | Register `hequ` for wild packs, stable access, spawn eggs, H-menu display, breed books, commands, saved genomes, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Hequ is a regional highland landrace rather than an ordinary commercial sale-yard breed. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band object. Do not use bands to imitate altitude adaptation, gray progression, tail carriage, or hoof quality. |
| `common/breed/spec/` | Reconcile this illustrative file with the actual parser/writer, especially strain overrides, `grey` allele names, omitted-locus defaults, and direct numeric probability values. |
| `common/breed/Commonness` | Confirm that `UNCOMMON` maps to the intended rarity ladder and direct `spawn_weight: 3` value. |
| `common/breed/BreedStatCurve` | Convert overall and strain-specific speed, jump, health, and size targets into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Select Jiaode, Suoke, or Kesheng according to the stated weights; roll that strain’s Extension, Agouti, and gray pool; force all other coat and disease loci wild type/clear; then apply its body-stat bands. |
| `common/breed/BreedLineage` | Apply standard pure/cross/Mixed behavior. Jiaode × Suoke, Suoke × Kesheng, and Jiaode × Kesheng all remain Hequ because they are varieties inside one breed. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. A splice-carrot allele transmits normally and can create nonstandard Hequ descendants outside this tightly solid-color population. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the small-to-medium highland frame, practical speed, modest jumping, and exceptional health targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless required by the actual schema. Do not confuse the real regional varieties with code-level `BandType` names. |

## Verification

1. Confirm **Hequ** appears in the H-menu’s Breeds tab and the breed book with Chinese Qinghai–Tibetan Plateau origin, high-altitude adaptation flavour, its three variety descriptions, compact size, rare/uncommon accessibility, and high heartiness.

2. Spawn repeated packs in plateau-like terrain: meadow, windswept hills, gravelly hills, grove, snowy slopes, stony peaks, river valleys, and cold uplands. Every member of one generated pack should display the **Hequ** breed label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is small, broad-chested, gray, black, or generated in mountainous terrain.

4. Generate at least 1,000 founders. The overall population should be solid black, dark bay/brown, bay, chestnut, and gray. Gray should be especially noticeable in Jiaode, while Suoke and Kesheng should retain more dark non-gray individuals.

5. Confirm no pure founder produces cream, palomino, buckskin, dun, champagne, silver dapple, pearl, mushroom, flaxen chestnut, roan, tobiano, frame, splash, sabino, dominant white, rabicano, leopard complex, brindle, or magical coats.

6. Check strain behavior:
- Approximately 35% of founders should be **Jiaode**, larger/heavier and gray-biased.
- Approximately 30% should be **Suoke**, smallest/most compact with lower gray frequency.
- Approximately 35% should be **Kesheng**, balanced in size and color, with more Mongolian-influenced general-purpose flavour.
- All three should retain the exact same **Hequ** lineage label.

7. Confirm true-gray mechanics:
- A `G` founder should be born with a normal underlying bay, black, or chestnut coat and become progressively lighter.
- Gray horses must not render as silver dapple, pearl, or white-at-birth.
- A non-gray `N/N` founder should remain bay, brown, black, or chestnut.

8. Confirm all requested disease loci remain clear in a large founder sample. No unverified disorder allele should originate in a pure Hequ founder.

9. Test default lineage:
- Hequ × Hequ → Hequ, including crosses among Jiaode, Suoke, and Kesheng founders.
- Hequ × Mongolian Horse → Hequ cross.
- Hequ × Yakutian Horse → Hequ cross.
- Hequ cross × pure Hequ → the existing Hequ cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Check gameplay feel. Mature Hequs should remain compact, exceptionally durable, and suitable for long highland exploration, with practical but not elite speed and modest jumping. They should not surpass specialized endurance breeds in speed, top sport horses in jumping, or giant draught breeds in mass.

## Sources

- [Evaluation of the genetic diversity and population structure of Chinese indigenous horse breeds using 27 microsatellite markers](https://onlinelibrary.wiley.com/doi/10.1111/j.1365-2052.2010.02067.x): primary peer-reviewed Chinese-breed analysis placing Hequ in the Qinghai–Tibetan Plateau group and documenting its genetic-diversity context. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1365-2052.2010.02067.x)

- [Oklahoma State University — Hequ Horses](https://breeds.okstate.edu/horses/hequ-horses): detailed breed reference for Qinghai origin, 11,000-foot environment, riding/packing/draught/racing roles, black/brown/gray coats, 130–150 cm size, and the Jiaode, Suoke, and Kesheng variety descriptions. [breeds.okstate](https://breeds.okstate.edu/horses/hequ-horses)

- [Hequ horse overview](https://en.wikipedia.org/wiki/Hequ_horse): supplementary synthesis of Nanfan alias, Tibetan Plateau range, altitude adaptation, 12.3–14.3-hand size, overall conformation, color palette, and the three varieties. [en.wikipedia](https://en.wikipedia.org/wiki/Hequ_horse)

- [Assessment of SNP-based genomic diversity in Eastern Asian landrace horse populations](https://www.biodiversity-science.net/EN/10.17520/biods.2021031): genomic-diversity context for a sampled Hequ population in Henan Mongol Autonomous County, Qinghai. [biodiversity-science](https://www.biodiversity-science.net/EN/10.17520/biods.2021031)