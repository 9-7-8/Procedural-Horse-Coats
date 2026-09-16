The **Guangxi Horse** should be filed as the **Baise Horse**—also called the **Guangxi** or Baise–Guangxi horse—not as a generic large “Guangxi horse.” It is a very small, tough subtropical Chinese village pony from the Baise region of the Guangxi Zhuang Autonomous Region: a 10.3–11.2-hand pack, farm, harness, and riding horse with hard hooves, high heat-and-humidity tolerance, and a solid coat pool of bay, black, chestnut, and gray. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

## Identity & flavour

The **Baise Horse**, also known as the **Guangxi Horse** or Baise–Guangxi pony, is an indigenous Chinese horse from the Baise region of the Guangxi Zhuang Autonomous Region in southwestern China. It is not one of China’s large northern steppe horses; it is a compact southern mountain-and-village horse shaped by hot, humid subtropical climate, narrow tracks, rocky soils, wet growing seasons, and long-standing local agricultural use. Bronze horses from the third to first centuries BCE found in the region resemble the modern Baise type, suggesting a long local history, though that does not prove that today’s population is genetically unchanged since antiquity. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

The Guangxi/Baise horse was made for the work that mattered in remote villages: carrying people, moving supplies over narrow paths, pulling light carts, working on small farms, supporting local tourism, and serving as a practical harness or pack animal. It must remain willing under a load, cope with humidity and rough footing, make use of modest forage, and continue working where a tall, long-legged sport horse would be inconvenient. In Minecraft, it should be the player’s little subtropical workhorse: short, economical, exceptionally useful in broken terrain, and much tougher than its size first suggests. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

A mature Baise is usually only **112–117 cm**, about **10.3–11.2 hands**, and weighs roughly **200–300 kg**. It has a heavy, broad-jawed head with a straight profile, a medium-length neck, relatively straight shoulders, low or modest withers, a short strong back, broad powerful loins, short straight legs, dense bone, clean joints, and small-to-medium hard hooves. It is pony-sized but not refined like a show pony: the silhouette should be compact, muscular, and practical, with no feathering and a normal, sturdy mane and tail. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

The supported coat palette is restrained but not uniform: **bay, black, chestnut, and gray**. The available source material does not supply a population genotype survey for their exact frequencies, so the file should produce a broadly bay-biased population with chestnut, black, and gray minorities rather than inventing a dramatic dilution or spotted population. Cream, dun, champagne, silver, pearl, mushroom, roan, tobiano, frame overo, splash, leopard complex, and magical genes should be absent from pure founders. Small ordinary facial or lower-leg markings may occur in individual domestic horses, but the mod’s named white-pattern loci are too strong to include without evidence. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

Baise horses are described as strong, quick, willing, frugal, sure-footed, and well adapted to local heat, humidity, rock, and poor-to-medium forage. Their deeper significance is conservation: genomic work identifies the Baise as a distinctive Chinese indigenous population, while regional conservation programs protect it as an animal genetic resource. In Procedural Horse Genetics, players should breed toward a tiny, solid, broad-headed Guangxi pack pony with powerful health and useful ordinary speed—not a miniature racehorse, not a spotted Appaloosa, and not a generic “small horse.” The mod does not model heat tolerance, humidity resilience, hoof wear, feed efficiency, load carrying, local disease resistance, village work, ethnic/cultural traditions, tourism handling, meat production, exact head shape, or the conservation status of the real population. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention used in your example. Before compilation, reconcile the exact field names, gene IDs, allele symbols, `Commonness` enum, source values, price units, and body-stat target format with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The real breed’s reported color set is bay, black, chestnut, and gray, but I found no representative Baise/Guangxi-wide genotype survey providing Extension, Agouti, or gray allele frequencies. The probabilities below are transparent **gameplay approximations**, designed only to produce the documented phenotype range. No credible Baise-specific carrier-frequency study was located for the requested disorder panel, so every named disorder locus is clear. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

```json
{
  "id": "guangxi",
  "name": "Guangxi",
  "type": "natural",
  "notes": "The Guangxi Horse, more precisely the Baise Horse or Baise-Guangxi pony, is a Chinese indigenous village, pack, light-draught, harness, riding, and farm horse from Baise in the Guangxi Zhuang Autonomous Region. Its real identity includes exceptional heat and humidity tolerance, feed thrift, hard hooves, dense bone, compact broad-headed conformation, sure-footed mountain-path travel, load carrying, local management, cultural use, and conservation status. Procedural Horse Genetics does not model those traits directly, including hoof wear, traction, pack capacity, disease resistance, local climate adaptation, tourism training, farm work, or regional pedigree.",

  "biomes": [
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:mangrove_swamp"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 560,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.66,
      "a": 0.34
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
    "jump": 3,
    "health": 9,
    "size": [
      0.71,
      0.79
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Guangxi × Guangxi produces Guangxi. Guangxi × another pure breed produces a Guangxi cross. A Guangxi cross bred back to pure Guangxi remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Guangxi lineage merely because a horse is tiny, bay, sure-footed, or found in a jungle biome: the real Baise-Guangxi breed is a geographically specific indigenous Chinese population."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed identity | `id: "guangxi"` / display name `Guangxi` | “Guangxi” is an alternate name for the Baise horse. The flavour text preserves the precise Baise–Guangxi identity while retaining the requested short breed name.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.66 / a: 0.34` | Produces a bay-led population with chestnut, black, and brown/dark-bay minorities. The real source identifies bay, black, chestnut, and gray but does not provide locus frequencies, so these values are gameplay estimates.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |
| Gray | `G: 0.16` | Gray is one of the documented coat colors. A minority rate retains gray adults without erasing the bay/black/chestnut core. This is an implementation estimate, not a survey result.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |
| Progressive gray | Use `G`, not white or silver | A true gray foal should be born on a pigmented base and lighten with age. The Guangxi palette’s “gray” class should never be represented by cream, dominant white, or silver dapple. |
| Dilutions | Cream, pearl, champagne, silver, mushroom, dun, and flaxen forced wild type | No reliable Baise/Guangxi population evidence was found to put these genes in ordinary pure founders. The source-supported four-color pool is enough to make a recognizable breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, and rabicano forced wild type | The breed is not characterized as a pinto, roan, or body-white population. Minor incidental markings are not enough evidence to seed strong named white-pattern loci. |
| Leopard complex | `LP`, `PATN1`, and `PATN2` forced wild type | Appaloosa-type leopard patterning is not part of the supported Guangxi/Baise color range. |
| Magical loci | All forced wild type | Heat tolerance, strong hooves, and feed efficiency are natural adaptations, not magical coat effects. |
| Disorders | All named loci clear | No defensible Guangxi/Baise-specific carrier frequency was located for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `5/10` | The breed is described as quick and useful under saddle, but is selected for local work and pack travel rather than elite racing.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |
| Jump | `3/10` | Tough mountain footing does not equate to sport-jumping selection. |
| Health | `9/10` | Represents heat/humidity resilience, forage thrift, hard hooves, functional longevity, and day-to-day working soundness—not invulnerability to disease or injury.  [chevauxdumonde](https://chevauxdumonde.com/en/horse/baise-guangxi) |
| Size | `×0.71–0.79` | Reflects the documented 112–117 cm, roughly 10.3–11.2-hand, small-horse/pony-sized population.  [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse) |

## Disorder approach

Every named disorder locus is **clear** in the pure Guangxi founder pool.

That is a strict evidence decision. Research supports the Baise horse’s indigenous genetic identity and its relevance to Chinese animal-genetic-resource conservation, but the retrieved material does not supply population carrier rates for the disease panel requested by the mod. [madbarn](https://madbarn.com/research-topics/genome/)

The file therefore does not invent risk for:

- `ACAN` dwarfism.
- `PLOD1` / Warmblood Fragile Foal Syndrome.
- `MET` / `EDNRB` lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

High real-world resilience to tropical climate, poor forage, and rough ground does not demonstrate absence of any particular Mendelian disease allele. It only supports a high **health/heartiness** gameplay score. A future Guangxi-specific genetic-screening study could justify adding a locus with transparent rate and sample context.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `guangxi` as a natural Chinese indigenous pony record with Baise alias context, subtropical biome mapping, narrow solid coat pool, clear disorders, and tiny high-heartiness stats. |
| `common/breed/Breeds` | Register `guangxi` for wild spawning, stable access, spawn eggs, breed books, H-menu display, commands, save loading, genome generation, and lineage output. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because this is a regional Chinese conservation/working population, not a typical sale-yard breed. |
| `common/breed/BreedBands` | Accept an empty epigenetic-band object. Do not use shade bands to imitate tropical heat tolerance, hoof hardness, or feed efficiency. |
| `common/breed/spec/` | Reconcile all illustrative field names, source enums, allele symbols, direct probability values, and defaults for omitted loci with the actual bidirectional JSON format. |
| `common/breed/Commonness` | Confirm `UNCOMMON` corresponds to the intended rarity ladder and that `spawn_weight: 3` is the correct moderate uncommon spawn pull. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 3, health 9, and size ×0.71–0.79 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only Extension, Agouti, and gray; force all other coat loci and every named disorder locus wild type or clear; then apply very small, tough subtropical work-pony stat targets. |
| `common/breed/BreedLineage` | Use the default pure/cross/Mixed table. A tiny bay jungle horse must not receive Guangxi lineage merely from size, color, terrain, or health stats. |
| `common/genetics/SpliceOutcome` | No Guangxi exception. A splice-carrot allele passes normally and can create a nonstandard descendant with dilution, pinto, leopard, magical, or disease genetics excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the breed’s tiny size, moderate utility speed, low jump specialization, and high heartiness target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the actual breed-file schema demands an explicit default. |

## Verification

1. Confirm **Guangxi** appears in the H-menu’s Breeds tab and the breed book with Baise–Guangxi identity, southwestern Chinese origin, subtropical village-work flavour, small size, high heartiness, uncommon rarity, and the stated source checklist.

2. Spawn repeated packs in eligible warm, wet, wooded, riverine, jungle-edge, and hilly biomes. Every horse within an individual selected pack must display **Guangxi** as its breed label.

3. Confirm that a lone ordinary wild horse remains **Feral Mixed**, even when it is tiny, bay, standing near a jungle river, or mechanically hardy.

4. Generate at least 1,000 pure Guangxi founders. The population should consist only of bay, brown/dark bay, black, chestnut, and gray. Bay should be the dominant visible non-gray outcome; gray should remain a minority.

5. Confirm correct gray behavior:
- `G` foals are born pigmented on bay, black, or chestnut bases.
- Gray horses lighten progressively as they mature.
- A gray horse is not rendered as white from birth, silver dapple, palomino, or cremello.

6. Inspect genome panels. Only Extension, Agouti, and gray should vary. Cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, sabino, frame, splash, KIT white, roan, rabicano, leopard complex, PATN genes, brindle, magical loci, and every named disorder locus must be wild type or clear.

7. Verify mature statistics. Guangxi founders should be among the smallest rideable horses, moderately quick and responsive for utility work, poor-to-modest jumpers, and notably high in health/heartiness. They should not rival racing horses in speed, sport ponies in jumping, or heavy drafts in carrying-force proxies.

8. Test default lineage:
- Guangxi × Guangxi → Guangxi.
- Guangxi × Hequ → Guangxi cross.
- Guangxi × Mongolian Horse → Guangxi cross.
- Guangxi cross × pure Guangxi → the existing Guangxi cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test deliberate genetic introduction. Add cream, dun, tobiano, leopard complex, large warmblood size, or any named disease allele through normal crossing or splice mechanics. Offspring should inherit the gene normally, but must remain nonstandard descendants and should never become pure Guangxi from phenotype alone.

## Sources

- [Baise horse overview](https://en.wikipedia.org/wiki/Baise_horse): structured reference identifying Baise as the Guangxi horse, plus Guangxi origin, 112–117 cm size, broad head, strong legs and hooves, bay/black/chestnut/gray palette, village roles, and traditional wedding use. [en.wikipedia](https://en.wikipedia.org/wiki/Baise_horse)

- [Baise–Guangxi horse profile](https://chevauxdumonde.com/en/horse/baise-guangxi): supplementary detailed breed account of Baise-region geography, hot humid/rocky habitat, small stocky type, hard hooves, work/pack adaptation, 200–300 kg weight, and conservation status. [chevauxdumonde](https://chevauxdumonde.com/en/horse/baise-guangxi)

- [Current genetic conservation of Chinese indigenous horses](https://academic.oup.com/g3journal/article/11/2/jkab008/6144767): peer-reviewed context for Chinese indigenous-horse conservation genetics and maintenance of diverse maternal lineages. [academic.oup](https://academic.oup.com/g3journal/article/11/2/jkab008/6144767)

- [Baise whole-genome research context](https://madbarn.com/research-topics/genome/): research-index entry describing a whole-genome comparison of Baise and other indigenous Chinese horses, used here to support treatment as a distinct local genetic resource rather than an arbitrary generic pony. [madbarn](https://madbarn.com/research-topics/genome/)