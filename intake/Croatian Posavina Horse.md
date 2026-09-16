The **Croatian Posavina Horse**—**Hrvatski posavac**, also called the **Posavac** or **Croatian Posavina**—should be a vulnerable, medium-sized Croatian cold-blood from the seasonally flooded Sava River basin. It is compact for a draught horse, immensely muscular and tractable, built for wet pasture, light-to-heavy farm work, and conservation grazing; pure founders should be predominantly bay or seal brown, with smaller black, chestnut, and gray minorities. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

> **Schema caveat:** The supplied Procedural Horse Genetics breed-wiki URL could not be retrieved by the available reader. The JSON uses the proposed readable convention from your example. Align the exact property names, allele IDs, commonness enum, biome validation, and file registration with the current `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Croatian Posavina Horse**, officially **Hrvatski posavac** in Croatian and often shortened to **Posavac** or **Posavina**, is an indigenous Croatian cold-blooded breed from the **Posavina**, the lowland floodplain of the Sava River basin. It is not a giant Belgian-style draught horse, but a smaller, deep-bodied, exceptionally useful coldblood shaped by wet meadows, common pasture, and the practical needs of rural households. Its roots lie in the archaic horses of the Sava basin, later influenced over centuries by the region’s changing position between Habsburg and Ottoman power; Arabian stallions were occasionally used from the seventeenth through twentieth centuries to improve stamina and speed, while later coldblood introductions increased muscularity. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

The Posavac was made for the heavy, ordinary work that keeps a floodplain community going: draught, hauling, farm work, carting, and work under low-input extensive management. Today it remains valuable for pasture-based meat production, rural tradition, agrotourism, recreational riding, carriage driving, hippotherapy, milk production, and—especially importantly—ecosystem service. Free-ranging Posavac herds graze seasonally flooded Sava pastures that are less suitable for other agriculture, helping maintain open grassland habitats and protected landscapes. A Posavac should feel like a patient, broad-backed working partner who can live outdoors, eat sensibly, and still put its chest and hindquarters into a load. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

Licensed Croatian Posavina stallions average **142.79 cm (about 14.0 hands)** at the withers, while mares average **139.71 cm (about 13.3 hands)**; broader breed descriptions commonly give a 140–150 cm, or roughly 13.3–14.3 hand, range. The horse is muscular and broad, with a relatively lighter, more refined head than larger Croatian cold-blood breeds, a short strong neck, deep wide chest, short back, powerful hindquarters, short sturdy legs, and large broad feet suited to soft floodplain ground. It should look dense and substantial rather than tall: a small cold-blood with a low center of gravity, enormous depth through the barrel, and enough bone to turn practical muscle into pulling power. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

Bay and seal brown are the signature Posavac colors; black and chestnut are less common, and gray is also accepted but uncommon. The breed should not be represented as a flashy pinto, leopard, champagne, silver, or cream population. Although some popular accounts mention a flaxen mane and tail, the robust academic and breed-level sources support the conservative approach: build the core coat pool from extension, agouti, and a low gray frequency, then leave rare or molecularly undocumented color traits out of pure founders. The result is a rich herd of bay, dark bay, brown, black, chestnut, and occasional gray working horses rather than a generic “all colors allowed” draught breed. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

The Posavac nearly disappeared with twentieth-century agricultural industrialization; by the 1990s the breeding population had fallen to around **450 breeding animals**. A breeding program, breed-organization work, subsidies, promotion, and economically viable use helped recover the population to **5,854 active horses** in the 2021 conservation assessment, though it remained classified as vulnerable. Genetic analysis found high diversity, no evidence of high inbreeding, and a broad base of active sire and mare lines—good news for a player who wants to build a long-lived heritage herd rather than preserve a breed through a single bottlenecked bloodline. In the mod, players should recognize a Posavac instantly as a compact bay-or-brown floodplain draught horse: calm, heavy, strong, and made for pasture and pull rather than speed or jumping. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

## Breed JSON

```json
{
  "id": "croatian_posavina_horse",
  "name": "Croatian Posavina Horse",
  "type": "natural",
  "notes": "The Croatian Posavina Horse is defined by a compact cold-blood body, deep chest, broad feet, muscular hindquarters, calm working character, adaptation to seasonally flooded Sava River pastures, and its roles in draught, grazing-based conservation, rural work, and pasture-based production. Procedural Horse Genetics does not model pulling force, load distribution, hoof shape or traction in wet ground, pasture-management behavior, flood tolerance, meat and milk production, carriage training, hippotherapy suitability, exact conformation scoring, individual calmness, or the studbook and conservation program that manage Croatian Posavac genetic diversity.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:forest",
    "minecraft:flower_forest",
    "terralith:wetland",
    "terralith:marsh",
    "terralith:temperate_highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 880,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.86,
      "e": 0.14
    },
    "agouti": {
      "A": 0.82,
      "a": 0.18
    },

    "grey": {
      "N": 0.91,
      "G": 0.09
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
    "speed": 3,
    "jump": 3,
    "health": 9,
    "size": [
      1.01,
      1.12
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Bay and seal-brown center | `E` 0.86 / `e` 0.14; `A` 0.82 / `a` 0.18 | Breed references identify bay and seal brown as the standard dominant colors. A high extension and agouti pool makes bay/brown the normal pure-founder result while retaining the documented black and chestnut minorities. These are cautious phenotype-derived game rates, not published MC1R/ASIP allele frequencies.  [en.wikipedia](https://en.wikipedia.org/wiki/Posavac) |
| Black | `a` 0.18 | Black is accepted but less common than bay/brown. A modest recessive agouti pool permits black founders without changing the overall breed visual identity.  [en.wikipedia](https://en.wikipedia.org/wiki/Posavac) |
| Chestnut | `e` 0.14 | Chestnut is less frequent than bay/brown but reported as an ordinary, valid Posavac color. This low `e` rate preserves it without creating a chestnut-forward population.  [en.wikipedia](https://en.wikipedia.org/wiki/Posavac) |
| Gray | `G` 0.09 | Gray is reported among accepted Posavac colors but is not the breed’s defining phenotype. The 0.09 founder-allele rate produces rare-to-uncommon gray descendants and is deliberately stated as a visual approximation rather than a direct STX17 survey result.  [en.wikipedia](https://en.wikipedia.org/wiki/Posavac) |
| Flaxen | Forced wild type | Some popular descriptions call flaxen mane/tail an “additional,” but no breed-specific molecular frequency was located. Flaxen should not be assumed from mane appearance alone; keeping it wild type is the more defensible pure-founder policy.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Dilutions and patterns | Forced wild type | No strong source supports cream, pearl, champagne, silver, mushroom, dun, tobiano, frame, splash, W-series spotting, roan, rabicano, leopard complex, brindle, or magical loci as a normal Croatian Posavina founder pool. |
| Disorders | All clear | The conservation and genetic-structure studies establish valuable population diversity but do not publish defensible breed carrier frequencies for the mod’s listed disease panel. All loci remain clear instead of importing rates from other heavy breeds.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/) |
| Speed | 3/10 | The Posavac is a cold-blooded draught and grazing horse, not a racing breed. The historical Arabian influence should not make the modern breed perform like a light saddle horse.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/) |
| Jump | 3/10 | Strong, broad, functional conformation supports practical movement but not jumping specialization. |
| Health | 9/10 | Encodes adaptability to extensive systems, regular fertility, floodplain grazing, and a robust low-maintenance working constitution. It does not claim immunity from disease.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/) |
| Size | ×1.01–1.12 | Produces a compact but undeniably substantial small cold-blood, slightly larger than an ordinary saddle horse in mass and body depth. Direct height remains in flavor text, not the JSON.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/) |

## Code map

| Location | Croatian Posavina Horse implementation |
|---|---|
| `common/breed/Breed` | Add the natural `croatian_posavina_horse` record, name, notes, biome and source metadata, commonness, price, coat pool, disorder policy, and stat targets. |
| `common/breed/Breeds` | Register the breed ID so it appears in spawning, saved genomes, the H-menu, the breed book, and all breed-lookup paths. |
| `common/breed/BreedSource` | Use the existing `wild`, `cowboy`, `spawn_egg`, and `stable` flags. |
| `common/breed/BreedBands` | Keep `epigenetic_bands` empty. The visual identity comes from base-color distribution and body stats, not a forced shade or special expression band. |
| `common/breed/spec/` | Add read/write support for this JSON file and substitute live schema names for proposed fields and locus labels as necessary. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder; retain `spawn_weight` 1.5 as the desired numerical pull. |
| `common/breed/BreedStatCurve` | Convert speed 3, jump 3, health 9, and the compact-coldblood size range to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only extension, agouti, and gray from the Posavac pool, while forcing every omitted coat locus wild type and every disorder locus clear. |
| `common/breed/BreedLineage` | No Posavac-specific lineage exception is required. |
| `common/genetics/SpliceOutcome` | No modification is needed: splice-carrot allele transfer and foal inheritance use ordinary logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the ordinary stat axes and target bands for a low-speed, low-jump, high-heartiness small cold-blood. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` band type is required unless the real schema mandates an explicit default. |

## Verification

1. Confirm **Croatian Posavina Horse** appears in the H-menu’s Breeds tab and breed book with its natural classification, rare commonness, Sava-floodplain-inspired biome list, source checklist, price, and notes.

2. Spawn a selected wild pack in eligible plains, meadow, swamp, river-margin, or wetland biomes. Every member of that generated pack should identify as **Croatian Posavina Horse**, including bay, brown, black, chestnut, and occasional gray founders.

3. Confirm a solitary unassigned wild horse reads **Feral Mixed**, rather than Croatian Posavina Horse.

4. Generate at least 1,000 founders. The population should be dominated by bay and seal-brown phenotypes, with fewer black and chestnut individuals and a small gray minority. Exact visible outcomes will vary because gray masks the base color.

5. Inspect genotype pools. Only `extension`, `agouti`, and `grey` should contain non-wild alleles. Pure founders must not produce cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical traits.

6. Inspect the disease panel across a large founder sample. Every named disorder should remain clear. If a disease allele appears, it should be traceable to an intentional outcross or genetics-tool action rather than a pure Posavac founder roll.

7. Compare generated Posavacs to a light riding horse and a giant draught breed. They should be notably broad, deep, steady, and hardy, but compact relative to huge draught horses; they should not dominate racing, jumping, or maximum-size roles.

## Sources

- [Ivanković et al., *Animals* — “Evaluation of the Conservation Status of the Croatian Posavina Horse Breed Based on Pedigree and Microsatellite Data”](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/): principal peer-reviewed source for the Posavac’s Sava-basin origin, history, Arabian and coldblood influence, conservation recovery, 2021 population of 5,854 active animals, vulnerable status, morphology, withers heights, high genetic diversity, low inbreeding, herd-line structure, adaptability, calm work character, and ecosystem-service role. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8300408/)

- [Galov et al., *Czech Journal of Animal Science* — “Genetic Structure and Admixture between the Posavina and Croatian Coldblood”](https://www.agriculturejournals.cz/artkey/cjs-201302-0004_genetic-structure-and-admixture-between-the-posavina-and-croatian-coldblood-in-contrast-to-lipizzan-horse-from.php): population-genetic context showing Posavina and Croatian Coldblood as related but distinct Croatian autochthonous-breed populations, with no detected recent bottleneck in the examined Posavina sample. [agriculturejournals](https://www.agriculturejournals.cz/artkey/cjs-201302-0004_genetic-structure-and-admixture-between-the-posavina-and-croatian-coldblood-in-contrast-to-lipizzan-horse-from.php)

- [Ivanković et al., *Animals* — “Meat Production Potential of Local Horse Breeds”](https://pmc.ncbi.nlm.nih.gov/articles/PMC12248997/): recent peer-reviewed source confirming the Croatian Posavina as an indigenous cold-blood traditionally raised on Sava floodplain pasture, its recovery to approximately 5,000 adult breeding animals, free-range grazing practices, conservation value, ecosystem role, and economic use. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12248997/)

- [Posavac breed overview](https://en.wikipedia.org/wiki/Posavac): convenient secondary source for aliases, 140–150 cm breed range, weight range, and traditional bay/seal-brown dominance with less common black and chestnut. For a production document, prefer the current Croatian Posavina breeding program or registry for formal color eligibility. [en.wikipedia](https://en.wikipedia.org/wiki/Posavac)