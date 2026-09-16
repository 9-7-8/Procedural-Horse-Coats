The **Guoxia** is best represented as a very small, rare southern Chinese mountain pony with a compact bay–gray–roan palette, abundant mane/tail/leg hair, strong feet, and an unusually clear recovery-and-conservation story. Its founder genetics should feel visually restrained but not uniform: mostly bay, with gray and roan present, while dilutions, loud white patterns, leopard spotting, and all magical genes remain absent from pure founders. [theequinest](https://theequinest.com/breeds/guoxia-pony)

> **Schema caveat:** As with the Yakut file, the supplied mod wiki could not be retrieved by the available reader. The JSON therefore follows the readable, proposed convention from your example; align actual key names, enum values, locus IDs, and file location with `common/breed/spec/` before compiling.

## Identity & flavour

The **Guoxia**—also written **Guoxia pony**, **Chinese Guoxia**, or occasionally **Gouxia**—is a tiny native horse breed from the counties of Debao, Jingxi, and Tianyang in the Baise region of western Guangxi Zhuang Autonomous Region, southern China. Its Chinese name is commonly translated as **“under-fruit-tree horse,”** a wonderfully literal description of an equine small enough to pass beneath orchard branches that would brush the backs of ordinary horses. The breed is generally described as ancient and is often said to have Mongolian-horse ancestry, although its documented modern history is especially defined by its rediscovery in the late twentieth century rather than a continuously maintained international studbook. [theequinest](https://theequinest.com/breeds/guoxia-pony)

The Guoxia was shaped by southern China’s rugged, subtropical hill country: a practical little working pony for narrow paths, small farms, pack work, light draught, and local riding. It is not a purpose-bred miniature pet, despite its extreme height. It is a functional mountain pony with straight legs, good joints, strong feet, and the compact, level-backed body needed to negotiate uneven land. A Guoxia should feel economical and sure-footed rather than delicate—small enough for a village orchard or steep track, but made to carry out real local work. [theequinest](https://theequinest.com/breeds/guoxia-pony)

A Guoxia stands approximately **86 cm to 100 cm at the withers—about 8.2 to 9.3 hands—with an average near 97 cm (9.2 hands)**. The head is short and can be somewhat substantial, usually with a straight profile; the neck is short, thick, and level with the back; the legs are straight; and the feet are notably strong. Its hair is unusually generous for such a small southern pony: the mane and tail are abundant and thick, and the leg hair is also described as growing thickly. Bay, gray, and roan are the traditional named colors, while primitive markings are also reported. [theequinest](https://theequinest.com/breeds/guoxia-pony)

The modern Guoxia story is one of fortunate survival. A remote herd of roughly 1,000 very small ponies was reported in 1981, after which a registry was established and 390 animals were registered. That makes conservation part of the breed’s identity: a player should not encounter Guoxias as the dominant horse in every warm biome, but as an uncommon, memorable mountain-orchard pony worth protecting and selectively breeding. In the mod, players should recognize it at once as a genuinely miniature, sturdy Chinese pony with heavy hair and a simple natural palette—not as a shrunken Arabian, not as a colorful American mini, and not as a magical creature. [theequinest](https://theequinest.com/breeds/guoxia-pony)

The Guoxia belongs in Procedural Horse Genetics because its size gives it a distinctive mechanical and visual niche. Breed toward an exceptionally compact, hardy, practical pony with mostly bay, gray, and occasional roan coats; the reward is a believable survivor of a rare regional landrace rather than an all-purpose performance horse. The mod does not model its abundant mane, tail, or leg hair; primitive dorsal or leg markings; orchard-scale utility; exact mountain sure-footedness; Chinese regional husbandry; tack fit; carrying capacity; or the registry’s conservation decisions. [theequinest](https://theequinest.com/breeds/guoxia-pony)

## Breed JSON

```json
{
  "id": "guoxia",
  "name": "Guoxia",
  "type": "natural",
  "notes": "The Guoxia is defined by its extremely small but functional pony build, abundant mane and tail, thick leg hair, strong feet, primitive markings, and adaptation to the rugged subtropical hill country of western Guangxi. Procedural Horse Genetics does not model mane/tail density, feathering, dorsal stripes and leg barring, hoof strength, exact carrying capacity, orchard and pack utility, mountain sure-footedness, regional husbandry, or the conservation history of the modern Guoxia registry.",

  "biomes": [
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:windswept_hills",
    "minecraft:stony_peaks",
    "terralith:tropical_jungle",
    "terralith:orchid_swamp",
    "terralith:rocky_mountains"
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
      "E": 0.82,
      "e": 0.18
    },
    "agouti": {
      "A": 0.80,
      "a": 0.20
    },

    "grey": {
      "N": 0.72,
      "G": 0.28
    },
    "roan": {
      "N": 0.92,
      "Rn": 0.08
    },

    "dun": {
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
    "jump": 4,
    "health": 7,
    "size": [
      0.42,
      0.51
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base colors | `E` 0.82 / `e` 0.18; `A` 0.80 / `a` 0.20 | Bay is one of the three traditional named Guoxia colors. A high `E` and `A` pool makes bay the expected founder phenotype while retaining enough chestnut and black alleles for natural background variation. These are conservative gameplay estimates, because no breed-specific MC1R/ASIP frequency study was located.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |
| Gray | `G` 0.28 | Gray is explicitly listed among the Guoxia’s traditional colors. The rate is set high enough for gray to be recognizable in a small founder sample but below the level that would make the breed visually gray-dominant. This is a phenotype-based approximation, not a published STX17 frequency.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |
| Roan | `Rn` 0.08 | Roan is traditionally reported but should remain less common than bay and gray. A low `Rn` rate retains the documented color without turning a rare conservation pony into a roan-specialist population.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |
| Primitive markings | No dun allele | Sources describe primitive markings, but that does not establish that modern Guoxia markings are caused by the tested `D`/TBX3 dun allele. Keeping `dun` wild type avoids claiming a molecular result that has not been demonstrated. The markings belong in flavor and model art, not an unsupported genetic pool.  [webequitation](https://www.webequitation.com/en/g/guoxia-horse-3171/) |
| Dilutions and loud patterns | Forced wild type | No reliable Guoxia-specific evidence was found for cream, pearl, champagne, silver, mushroom, tobiano, frame, splash, W-series spotting, leopard complex, brindle, or magical loci. Pure founders should not spontaneously produce them. |
| Disorders | All clear | No defensible Guoxia-specific carrier frequencies were located for ACAN dwarfism, PLOD1, MET, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Do not import disease rates from superficially similar small breeds. |
| Speed | 4/10 | The Guoxia was a practical mountain and farm pony, not a racehorse. It should move reliably but remain slower than saddle and racing specialists.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |
| Jump | 4/10 | Strong legs and practical hill-country utility justify competent mobility, but there is no historical basis for a sport-jumping specialization. |
| Health | 7/10 | A compact, regionally adapted working pony should feel hardy and low-maintenance. This score represents ordinary landrace toughness, not an unproven disease-resistance claim.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |
| Size | ×0.42–0.51 | Produces a genuinely tiny but usable horse-scale pony. The actual height statement remains in flavor text only, per the breed-file requirement.  [theequinest](https://theequinest.com/breeds/guoxia-pony) |

## Code map

| Location | Guoxia implementation |
|---|---|
| `common/breed/Breed` | Add the natural `guoxia` record: name, notes, biome placement, sources, price, commonness, gene pools, disease-clear policy, and stat targets. |
| `common/breed/Breeds` | Register `guoxia` for H-menu display, breed-book lookup, spawning, serialization, and founder generation. |
| `common/breed/BreedSource` | Validate the selected `wild`, `cowboy`, `spawn_egg`, and `stable` source flags. |
| `common/breed/BreedBands` | Use an empty epigenetic-band map; no coat shade or non-stat gene requires fixed epigenetic expression. |
| `common/breed/spec/` | Add the JSON file to the exact bidirectional read/write schema, replacing proposed names such as `coat_genes`, `stat_scores`, and allele labels with project-defined ones where necessary. |
| `common/breed/Commonness` | Ensure `RARE` resolves to the mod’s appropriate rarity ladder; `spawn_weight` 1.5 is the intended numerical pull. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 7, and the compact size target into legal `TargetBand` ranges. |
| `common/breed/BreedFounder` | Roll founders from Guoxia’s named `E/e`, `A/a`, gray, and roan pools; force every omitted coat locus wild type and each disorder locus clear. |
| `common/breed/BreedLineage` | No Guoxia-specific lineage rule is required; retain normal project behavior. |
| `common/genetics/SpliceOutcome` | No special handling. Spliced alleles use ordinary inheritance and splice-outcome logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use standard body-stat axes and target-band serialization for the miniature size range and working-pony scores. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` specialization is necessary unless the current implementation requires one as an explicit default. |

## Verification

1. Open the H-menu and the breed book. **Guoxia** should appear with the exact display name, natural classification, rare commonness, source list, warm southern mountain biome list, compact-stat presentation, and explanatory notes.

2. Force wild herd generation in one of the listed jungle, forest, or windswept-hill biomes. Every horse belonging to one generated Guoxia pack should identify as **Guoxia**, even where individual founders differ in gray or roan status.

3. Confirm a lone unassigned wild horse reads **Feral Mixed**, rather than Guoxia.

4. Roll at least 500 founders and check genotype distribution. Most founders should have `E` and `A` alleles consistent with a bay-centered population; gray should occur regularly, roan less often, and chestnut or black backgrounds should remain possible but not dominate.

5. Verify that pure founders do not produce cream, pearl, champagne, silver, mushroom, dun, tobiano, frame overo, splash, W-series white, rabicano, leopard complex, PATN1, PATN2, brindle, or magical phenotypes.

6. Verify that every listed disorder locus remains clear after large founder sampling. Disease alleles should enter Guoxia lines only through deliberate outcrossing or mod genetics tools.

7. Compare the breed against an ordinary saddle horse and a Falabella-like miniature. A Guoxia should be visibly tiny, practical, and sturdier-feeling than a decorative miniature, but it should not outpace, outjump, or out-muscle specialized full-sized breeds.

## Sources

- [FAO Domestic Animal Diversity Information System (DAD-IS)](https://www.fao.org/dad-is/data/en/): FAO’s global domestic-animal genetic-resource database and the appropriate official international system for validating country-reported conservation and breed-record data. [fao](https://www.fao.org/dad-is/data/en/)

- [The Equinest — Guoxia Pony](https://theequinest.com/breeds/guoxia-pony): reports the Guangxi origin, “under fruit tree horse” name translation, rediscovery of approximately 1,000 ponies in 1981, initial registry activity, average 9–10 hand height description, thick mane/tail/leg hair, strong feet, traditional bay/gray/roan palette, and rare conservation context. [theequinest](https://theequinest.com/breeds/guoxia-pony)

- [WebEquitation — Guoxia Horse](https://www.webequitation.com/en/g/guoxia-horse-3171/): supplies the reported 86–100 cm height range and approximately 97 cm average, plus bay/roan/gray colors, abundant mane and tail, and reported primitive markings. [webequitation](https://www.webequitation.com/en/g/guoxia-horse-3171/)

- [Guoxia overview](https://en.wikipedia.org/wiki/Guoxia): identifies the breed’s origin in Debao, Jingxi, and Tianyang counties in Baise, western Guangxi Zhuang Autonomous Region, China. This is useful geographic context but should be superseded by an accessible Chinese national studbook or DAD-IS record if you obtain one for production documentation. [en.wikipedia](https://en.wikipedia.org/wiki/Guoxia)