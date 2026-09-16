The **Debao Pony** is a distinct, registered Guangxi landrace descended from the older *Guoxia* tradition—not merely another name for it. It should be implemented as a rare, exceptionally small but muscular Chinese pack, draught, riding, and companion pony whose coat pool is strongly bay-centered, with substantial gray and chestnut representation, some black, very rare buckskin-like yellow, and only exceptional piebald founders. Its defining genetic identity is extreme **non-pathological small stature**, with selection signals near *TBX3* and *HMGA2*, rather than dwarfism. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/)

> **Schema caveat:** The available reader could not retrieve the provided Procedural Horse Genetics breed-wiki page. This uses the readable JSON convention from the supplied example; translate field names, enum capitalization, locus identifiers, and exact file placement to the mod’s active `common/breed/spec/` schema before compiling.

## Identity & flavour

The **Debao Pony**—also called the **Debao dwarf horse**, **Baiseshishan Pony** (“Baise rocky-mountain pony”), and locally connected with the older **Guoxia** or “under-fruit-tree horse” tradition—is a native Chinese pony from Debao County in Baise, western Guangxi Zhuang Autonomous Region. Its core distribution includes villages and townships in Debao, with related populations extending into neighboring Jingxi, Tianyang, and Napo. The modern breed was rediscovered and scientifically investigated in the early 1980s, but the regional miniature-horse tradition reaches much farther back: Chinese accounts and archaeological material associate Guangxi “under-fruit-tree horses” with the Han era, more than 2,000 years ago. [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473)

The Debao was made for work where a large horse would be awkward: steep limestone country, small farms, narrow tracks, local cargo, and village life. It is a true utility pony, historically used for **pack, light draught, riding, and ornament**, rather than a decorative miniature selected only for novelty. Its short frame and strength made it useful for hauling fruit and other goods, while modern Debao programs have also used the breed for youth riding education and pony polo. Contemporary reports describe it as muscular and capable of carrying substantial loads despite its extraordinary smallness. [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473)

A typical Debao Pony stands about **0.94–1.03 m (9.1–10.0 hands)** at the withers, while the broader breed description identifies the population as generally below **106 cm (10.2 hands)**. It weighs roughly 106–111 kg. The head is fine but can be slightly substantial, with a straight nasal profile, broad enough forehead, large round eyes, alert upright ears, and flexible nostrils. The neck is moderate in length, the withers low and level, the chest broad and deep, the belly rounded, the back and loin straight, and the croup short and gently sloped. The legs are strong and correctly set, joints are robust, hoof horn is hard, and the mane, forelock, tail, and fetlock hair are dense. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/)

Bay is the breed’s unmistakable dominant color. A survey of 856 Debao ponies reported 54.91% bay, 15.77% gray, 14.95% chestnut, 6.78% black, 3.27% rabbit-brown, 2.45% yellow, and 1.87% piebald; limited white markings on the face and lower legs also occur. That gives the mod a far stronger basis than generic “Chinese pony” guesswork: a founder herd should immediately read as mostly bay, with a significant gray-and-chestnut minority and occasional black horses, rather than as a blanket gray, dun, pinto, or diluted-color breed. The rare yellow category is conservatively represented with a small cream pool, while piebald is represented by a very low tobiano rate. [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473)

The Debao’s cultural footprint is inseparable from conservation. A 1981 field survey rediscovered a seven-year-old mare only 92.5 cm tall near the Debao–Jingxi border; research from 1986–1990 established that its small stature was stable and heritable, distinguishing Debao from the taller Baise horse. The breed later faced endangered-maintained status, with conservation programs underway. Genomic research has since found that its miniature size reflects selection at ordinary size-associated regions, especially near *TBX3* and *HMGA2*, and evolved independently from the small stature of other pony breeds. In the mod, players should breed toward a rare, sturdy, bay-forward pocket workhorse: tiny, but never to be mistaken for an ACAN dwarf or a fragile toy. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/)

## Breed JSON

```json
{
  "id": "debao_pony",
  "name": "Debao Pony",
  "type": "natural",
  "notes": "The Debao Pony is defined by an extremely small yet functional landrace body, a broad deep chest, strong joints, hard hoof horn, dense mane and tail, substantial fetlock hair, mountain utility, and a long Guoxia-associated cultural history. Procedural Horse Genetics does not model the specific TBX3/HMGA2 selection haplotypes behind Debao stature, mane and tail density, leg feathering, hoof-horn strength, pack and draught capacity, steep-limestone sure-footedness, youth-riding temperament, pony polo ability, Chinese regional husbandry, or conservation-program pedigree management.",

  "biomes": [
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:windswept_hills",
    "minecraft:stony_peaks",
    "terralith:tropical_jungle",
    "terralith:rocky_mountains",
    "terralith:orchid_swamp"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 820,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.78,
      "e": 0.22
    },
    "agouti": {
      "A": 0.82,
      "a": 0.18
    },

    "grey": {
      "N": 0.842,
      "G": 0.158
    },
    "cream": {
      "N": 0.975,
      "Cr": 0.025
    },
    "tobiano": {
      "N": 0.981,
      "To": 0.019
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

    "roan": {
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
    "health": 8,
    "size": [
      0.43,
      0.53
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Basis |
|---|---:|---|
| Bay-centered base pool | `E` 0.78 / `e` 0.22; `A` 0.82 / `a` 0.18 | Bay made up 470 of 856 surveyed Debao ponies—54.91%—and is therefore the breed’s obvious visual center. The high `E` and `A` pool encourages bay without excluding the documented chestnut and black minorities. These are phenotype-derived founder estimates, not published MC1R/ASIP allele frequencies.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Gray | `G` 0.158 | Gray comprised 15.77% of the 856-pony color survey. An allele frequency around 0.158 produces a population-level gray rate close to the reported phenotype rate under Hardy–Weinberg expectations, assuming `G` is dominant and no strong genotype-selection bias.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Chestnut | `e` 0.22 | Chestnut accounted for 14.95% of surveyed horses. The selected `e` rate gives a close game population approximation while preserving bay as the dominant result.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Black | `a` 0.18 | Black occurred in 6.78% of the survey. A modest recessive `a` frequency allows black founders while preventing black from becoming visually common.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Cream | `Cr` 0.025 | “Yellow” comprised 2.45% of reported coats. The source does not molecularly identify the mechanism; a very low cream rate is a cautious visual proxy for rare yellow/buckskin-like horses, not a claim that every yellow Debao is genetically cream.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Tobiano | `To` 0.019 | Piebald horses were 1.87% of the survey. A low tobiano pool is the most straightforward way to preserve rare piebald founders. This is a visual implementation assumption; the survey does not identify the exact white-pattern locus.  [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473) |
| Other coat loci | Forced wild type | No defensible Debao-specific evidence was found for dun, pearl, champagne, silver, mushroom, flaxen, roan, sabino, frame, splash, W-series white, rabicano, leopard complex, brindle, or magical loci. |
| Disorders | All clear | Debao size is a breed-specific, selected stature phenotype, with associated genomic regions near *TBX3* and *HMGA2*—not evidence for ACAN dwarfism. No defensible Debao carrier frequencies were located for the mod’s listed disease loci, so all are forced clear.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/) |
| Speed | 4/10 | Debao ponies were used for transport, pack, draught, riding, and practical work—not selected as racing horses.  [chinadaily.com](https://www.chinadaily.com.cn/a/201604/08/WS5a2b6f8da310eefe3e9a01f4.html) |
| Jump | 4/10 | Strong limbs and mountain utility warrant basic competence, but no source supports a jumping specialization. |
| Health | 8/10 | Captures a compact, sturdy, hard-hoofed working landrace with a history of survival in rugged local conditions. It does not claim disease immunity.  [chinadaily.com](https://www.chinadaily.com.cn/a/201604/08/WS5a2b6f8da310eefe3e9a01f4.html) |
| Size | ×0.43–0.53 | Creates a clearly miniature but functional pony. This models the breed’s major identity while keeping the real height statement out of the JSON itself. The underlying biology is a selected stature architecture near *TBX3* and *HMGA2*, not pathological dwarfism.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/) |

## Code map

| Location | Debao Pony implementation |
|---|---|
| `common/breed/Breed` | Add the `debao_pony` natural-breed record, displayed name, description, sources, price, commonness, biomes, coat/disorder pools, and body-stat targets. |
| `common/breed/Breeds` | Register `debao_pony` so the name resolves in spawning, books, menus, saved horse data, and breed selection. |
| `common/breed/BreedSource` | Enable only `wild`, `cowboy`, `spawn_egg`, and `stable`, using existing four-way source handling. |
| `common/breed/BreedBands` | Accept an empty `epigenetic_bands` object; no fixed shade, category, or non-stat numeric band is needed. |
| `common/breed/spec/` | Add the JSON parsing and serialization entry in both directions. Confirm the actual names for extension, agouti, cream, gray, tobiano, body stats, source list, and commonness. |
| `common/breed/Commonness` | Confirm `RARE` maps to the project’s appropriate ladder value; the desired numerical `spawn_weight` is 1.5. |
| `common/breed/BreedStatCurve` | Resolve scores 4/4/8 and the very small size range into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll founders from only the supplied Debao pools; force omitted coat loci wild type and all unnamed disorder loci clear. |
| `common/breed/BreedLineage` | No Debao-specific exception is required. |
| `common/genetics/SpliceOutcome` | No change: injected alleles should use ordinary splice-carrot transmission logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use the usual body-stat target types to preserve compact size and working-pony performance. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` specialization should be required unless the live schema mandates an explicit default. |

## Verification

1. Confirm **Debao Pony** appears in the H-menu’s Breeds tab and in the breed book, with its natural classification, rare status, correct sources, southern-China biome placement, and conservation-oriented notes.

2. Force a wild herd spawn in one of its enabled warm forest, jungle, or rocky-hill biomes. Every horse generated in that pack should display **Debao Pony** as its breed, even when one is gray, chestnut, black, cream-derived, or exceptionally tobiano.

3. Confirm that a lone wild horse which did not originate in a breed-selected pack reads **Feral Mixed**, not Debao Pony.

4. Roll at least 1,000 founders. The visible distribution should be strongly bay-forward, with a substantial gray and chestnut minority, fewer black horses, rare yellow/cream-derived founders, and very rare tobiano horses. Exact phenotype totals will vary because base-color loci interact.

5. Inspect founder genomes. Only extension, agouti, gray, cream, and tobiano should be capable of generating non-wild alleles; every other named coat locus should remain wild type.

6. Inspect the disorder panel across a large founder sample. `ACAN_dwarfism` must remain clear: Debao small stature is normal breed phenotype, not the mod’s dwarfism disorder. Every other listed disorder should likewise remain clear.

7. Compare generated adults with baseline horses. Debaos should be exceptionally small, sturdy, and healthy-feeling, but not mechanically optimized for racing, jumping, or high-speed sport.

## Sources

- [Kader et al., *Genome Biology and Evolution* — “Population Variation Reveals Independent Selection toward Small Body Size in Chinese Debao Pony”](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/): peer-reviewed genomic research showing that Debao’s extreme small size is associated with selected regions near *TBX3* and *HMGA2*, that the population is distinct from other Chinese horse groups, that its stature is generally under 106 cm, and that its diminutive stature was selected independently from other pony breeds. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758242/)

- [Xinjiang Agricultural University Horse Industry resource — Debao Pony / 德保矮马](https://horse.xjau.edu.cn/breeds/list/473): Chinese breed description with the primary production area, 0.94–1.03 m height range, 106–111 kg body weight, conformation, dense mane/tail and fetlock hair, hard hoof horn, a survey of 856 coat colors, population figures, endangered-maintained status, conservation efforts, and 1981–1990 research history. [horse.xjau.edu](https://horse.xjau.edu.cn/breeds/list/473)

- [China Daily — “Once-endangered pony makes a comeback”](https://www.chinadaily.com.cn/a/201604/08/WS5a2b6f8da310eefe3e9a01f4.html): report on the 1981 rediscovery, Debao conservation and breeding center, practical riding and pack roles, maximum approximate height, strength, temperament, Guoxia association, and historic regional use hauling fruit and cargo. [chinadaily.com](https://www.chinadaily.com.cn/a/201604/08/WS5a2b6f8da310eefe3e9a01f4.html)