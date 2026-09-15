The **Misaki horse** is best modeled as a tightly localized Japanese feral native breed from Cape Toi, Miyazaki Prefecture: a small, dark, solid-coated, bay-dominant horse with unusually well documented base-coat frequencies. Unlike the Konik, it should not be genetically fixed to one color; a credible founder pool is bay-heavy with black and chestnut minorities, while cream, gray, dun, pinto, leopard complex, and conspicuous white are excluded. A Misaki should feel like a compact, hardy coastal-hill pony rather than a polished riding breed. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x)

## Identity & flavour

The **Misaki horse**—Japanese *Misaki-uma* (御崎馬 or 岬馬)—is one of Japan’s eight native horse breeds and the country’s only native breed still living as a self-sustaining feral population. Its home is **Cape Toi** (*Toimisaki*) in Kushima, at the southern end of Miyazaki Prefecture on Kyūshū. The breed is closely bound to that landscape: grassy coastal headlands, wooded slopes, wind, rain, and the Pacific at the edge of the pasture. The Misaki and its Cape Toi habitat were designated a Japanese Natural Monument in 1953, and the population remains a conservation priority. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6541847/)

The horses’ local history reaches back to the Edo period. In 1697, the Akizuki family gathered local horses for agricultural and riding use, turning them out on pasture when they were not required; over generations, the Cape Toi population continued breeding with comparatively little directed selection. Their numbers fell sharply after World War II, reaching a recorded low of 53 animals in 1973. Recovery and careful conservation brought the herd back to approximately one hundred or more animals, but it remains a very small, vulnerable breed. Modern genetic work is therefore aimed at preserving diversity and supplying data for a studbook and conservation management. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6541847/)

A Misaki is a small horse, typically about **130–135 cm** at the withers—roughly **12.3–13.1 hands**, though reported ranges vary somewhat by source. It is a compact, practical native horse with a large, plain, straight-profile head; short, horizontally carried neck; low or inconspicuous withers; straight back; relatively flat croup; slender limbs; small hooves; and sometimes light hair behind the fetlocks rather than heavy feather. Its mane can be short and upright or medium and bushy, while the tail is usually short to medium but thick. These details help it read as a weathered, locally adapted feral pony rather than a miniature warmblood. [en.wikipedia](https://en.wikipedia.org/wiki/Misaki_horse)

The breed’s coat is generally dark and solid. The best available Misaki-specific coat study sampled 99 horses and recorded **79.2% bay, 12.9% black, and 8.0% chestnut**. That is an unusually useful basis for a breed-genetics file: the breed is mostly bay because functional Extension pigment and dominant Agouti are common, but it still legitimately produces black and chestnut animals. White face and leg markings are very rare in Japanese native horses, including Misakis, and the documented breed palette does not support seeding gray, cream, dun, champagne, silver, tobiano, roan, leopard complex, or other dramatic pattern alleles in pure founders. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x)

Misakis are intelligent, self-reliant, sure-footed, and hardy enough for semi-feral life, but they are not a high-performance sport population. In Procedural Horse Genetics, they should be a rewarding conservation breed: short, resilient, generally bay, quiet-looking, and unmistakably solid-coated. Players should recognize a Misaki at a glance as a small dark Japanese coastal pony—most often bay, sometimes black, occasionally chestnut—with clean legs and no decorative color genes. The mod does not model the real herd’s social structure, seasonally variable forage, coastal grazing, natural selection, exact head and croup profile, mane form, hoof shape, Japanese legal protection, population management, or the difference between a feral horse and an untamed domestic horse.

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. The JSON uses the readable convention in your Anglo-Arabian example; confirm exact field names, gene IDs, allele symbols, `Commonness` enum spelling, source values, and band serialization against `common/breed/spec/` before compiling.
>
> **Scientific-genetics note:** The `extension` and `agouti` pools below are calculated from the Misaki-specific reported phenotype frequencies—79.2% bay, 12.9% black, and 8.0% chestnut—under Hardy–Weinberg equilibrium and the standard base-color model. That gives approximate allele frequencies of `E ≈ 0.958`, `e ≈ 0.042`, `A ≈ 0.937`, and `a ≈ 0.063`. This is a defensible model-derived estimate, not directly genotyped Misaki allele-frequency data. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x)

```json
{
  "id": "misaki",
  "name": "Misaki",
  "type": "natural",
  "notes": "The Misaki horse is a small Japanese native breed living ferally at Cape Toi in Miyazaki Prefecture. Its real identity depends on its isolated Cape Toi population, local management, social structure, conservation status, compact native-horse conformation, small hooves, seasonal coat, coastal-hill adaptation, and Japanese Natural Monument protection. Procedural Horse Genetics does not model those features, nor does it distinguish a feral population from an ordinary untamed domestic horse.",

  "biomes": [
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:stony_shore",
    "minecraft:beach"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 720,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.958,
      "e": 0.042
    },
    "agouti": {
      "A": 0.937,
      "a": 0.063
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
    "grey": {
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
    "jump": 4,
    "health": 8,
    "size": [
      0.83,
      0.90
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Misaki × Misaki produces Misaki. Misaki × another pure breed produces a Misaki cross. A Misaki cross bred back to pure Misaki remains that cross under the default system. Two distinct crosses become Mixed, and any pairing involving Feral Mixed becomes Mixed. Do not label a small bay horse as Misaki solely because it resembles one: the real breed is a specific, isolated Cape Toi population rather than a broad visual color type."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Core base-color data | Bay 79.2%; black 12.9%; chestnut 8.0% | These figures come from a Misaki-specific investigation of 99 native horses. They are the appropriate evidence base for the file, rather than generic Japanese-horse or pony color assumptions.  [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x) |
| Extension estimate | `E: 0.958`, `e: 0.042` | With chestnut treated as `e/e`, \(q_e = \sqrt{0.080} = 0.283\), which would not match the proposed figure. Correction: direct phenotype-based allele inference should yield `e ≈ 0.283` and `E ≈ 0.717`, assuming Hardy–Weinberg equilibrium and no other red-suppressing locus. |
| Agouti estimate | `A: 0.875`, `a: 0.125` | Given `E ≈ 0.717`, black phenotype is `E_ a/a`; therefore \(q_a ≈ \sqrt{0.129 / (1 - 0.080)} = 0.375\), yielding `A ≈ 0.625`. This must replace the preliminary draft values to recreate the reported phenotype distribution correctly. |
| Correct implementation values | `E: 0.717 / e: 0.283`; `A: 0.625 / a: 0.375` | These corrected values are the scientifically consistent allele-frequency estimates from the reported bay/black/chestnut phenotype proportions under Hardy–Weinberg assumptions. They yield approximately 79.2% bay, 12.9% black, and 8.0% chestnut.  [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x) |
| Gray and dilutions | All forced wild type | The breed is described as bay, black, brown, and chestnut, with a dark solid palette. No credible source found supports seeding gray, cream, dun, champagne, silver, pearl, mushroom, or flaxen in pure Misaki founders.  [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x) |
| White-pattern loci | All forced wild type | White face or leg markings are reported as very rare, and breed descriptions characterize the coat as solid. The mod’s named pattern loci would create conspicuous hereditary white that does not belong in the default pure population.  [breeds.okstate](https://breeds.okstate.edu/horses/misaki-horses) |
| Leopard, roan, brindle | All forced wild type | These are not part of the documented Misaki phenotype range and should require outcrossing or intentional gene editing. |
| Disorders | All named loci clear | No credible Misaki-specific carrier frequency was found for any disorder in the requested list. Assigning rates from unrelated Japanese, Arabian, Quarter Horse, Friesian, or Warmblood populations would be unsupported. |
| Speed | `4/10` | The Misaki is a compact, semi-feral native horse, not a racing or high-speed saddle population. |
| Jump | `4/10` | Its coastal slopes and natural agility justify usefulness, but the breed has no identified specialist jumping selection. |
| Health | `8/10` | Represents outdoor hardiness, longevity under low-intervention management, and adaptation to Cape Toi—not exemption from disease or inbreeding concerns.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6541847/) |
| Size | `×0.83–0.90` | Creates a 12.3–13.1-hand pony-sized range, aligned with the commonly reported 130–135 cm wither height.  [en.wikipedia](https://en.wikipedia.org/wiki/Misaki_horse) |

## Required JSON correction

The JSON above should use the following corrected base-color section. The `0.958 / 0.042` and `0.937 / 0.063` values shown in the first JSON draft are **not** mathematically consistent with the reported phenotype frequencies and should not be used.

```json
"extension": {
  "E": 0.717,
  "e": 0.283
},
"agouti": {
  "A": 0.625,
  "a": 0.375
}
```

Using those frequencies and random Mendelian founder draws produces approximately:

- Chestnut: \(e/e \approx 8.0\%\).
- Black: \(E\_\,a/a \approx 12.9\%\).
- Bay: \(E\_\,A\_ \approx 79.1\%\).

That closely reconstructs the published Misaki phenotype sample. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `misaki` natural-breed record, its conservation-focused notes, sources, biomes, price, rarity, genetic pools, and stat targets. |
| `common/breed/Breeds` | Register `misaki` for loading, spawning, menus, books, genome serialization, and lineage output. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`; the breed is intentionally omitted from `cowboy` because it is an isolated endangered conservation population rather than an ordinary sale-yard breed. |
| `common/breed/BreedBands` | Support the empty band object. No special shade, coat-depth, or stat epigenetic band is required. |
| `common/breed/spec/` | Replace illustrative keys and allele labels with the project’s actual schema. In particular, make sure the corrected `E/e` and `A/a` rates are what the founder generator consumes. |
| `common/breed/Commonness` | Confirm `RARE` maps to the requested weight ladder. Retain a numerical `spawn_weight` of `1.5` if the implementation supports direct weights. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 8, and size ×0.83–0.90 to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll base colors from the corrected Extension/Agouti pools; force every other coat and disorder locus clear or wild type; apply small hardy body-stat targets. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed rules. Do not infer Misaki from phenotype or from a feral flag. |
| `common/genetics/SpliceOutcome` | No exception: a successfully spliced allele should reach offspring under normal rules and may deliberately make a nonstandard Misaki descendant. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize and generate the small, modest-performance, high-heartiness target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is needed unless the schema requires an explicit default. |

## Verification

1. Confirm **Misaki** appears in the H-menu’s Breeds tab and breed book with the correct Japanese/Cape Toi description, rare commonness, source set, biome mapping, and small hardy-stat profile.

2. Spawn repeated wild packs in meadow, windswept hill, forest-edge, and coast-adjacent biomes. Every member of a selected spawned pack should display **Misaki** as its breed.

3. Check that a lone ordinary wild horse remains **Feral Mixed**, even if it happens to be short and bay.

4. Generate at least 1,000 pure Misaki founders and inspect their visible coat phenotypes. Expected results should approach:
- About 79% bay.
- About 13% black.
- About 8% chestnut.

5. Confirm that no pure-founder sample contains gray, cream dilution, dun, champagne, silver, mushroom, pearl, flaxen chestnut, roan, tobiano, sabino, splash, frame, W-series white, rabicano, leopard spotting, brindle, or magical coat effects.

6. Inspect the genomes of founders and verify the actual seeded base-locus rates are the corrected values:
- `E = 0.717`, `e = 0.283`.
- `A = 0.625`, `a = 0.375`.

7. Confirm every named disorder locus is clear in pure founders. No disease allele should arise from two unmodified Misaki parents.

8. Test default lineage:
- Misaki × Misaki → Misaki.
- Misaki × Kiso → Misaki cross.
- Misaki × Hokkaido → Misaki cross.
- Misaki cross × pure Misaki → the existing Misaki cross label.
- Two different crosses → Mixed.
- Any cross with Feral Mixed → Mixed.

9. Test body stats over a sample of mature horses. Misakis should remain visibly and mechanically pony-sized, less fast and less jump-specialized than mainstream riding horses, but strongly healthy and dependable for rough exploration.

## Sources

- [Development of a method for simultaneously genotyping multiple horse coat-colour loci and genetic investigation of basic colour variation in Thoroughbred and Misaki horses in Japan](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x): primary Misaki color-distribution source; reports bay 0.792, black 0.129, and chestnut 0.080 in the Misaki population. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1439-0388.2009.00841.x)

- [Genetic characteristics of feral Misaki horses based on microsatellite markers](https://pmc.ncbi.nlm.nih.gov/articles/PMC6541847/): peer-reviewed conservation genetics of the Cape Toi feral Misaki population, including its role in conservation and studbook development. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6541847/)

- [Changes in population structure and genetic diversity of Misaki horses](https://europepmc.org/article/pmc/10788177): population-structure and genetic-diversity research relevant to managing a very small conserved breed. [europepmc](https://europepmc.org/article/pmc/10788177)

- [Oklahoma State University — Misaki Horses](https://breeds.okstate.edu/horses/misaki-horses): concise breed reference for population history, postwar decline, typical bay/black/chestnut colors, rarity of white markings, and approximately 13.2-hand height. [breeds.okstate](https://breeds.okstate.edu/horses/misaki-horses)

- [University of Kentucky — Equine Coat Color Genetics 101](https://equine.mgcafe.uky.edu/news-story/equine-coat-color-genetics-101): Extension and Agouti interpretation for bay, black, and chestnut phenotypes. [equine.mgcafe.uky](https://equine.mgcafe.uky.edu/news-story/equine-coat-color-genetics-101)

- [UC Davis Veterinary Genetics Laboratory — Equine Coat Color](https://vgl.ucdavis.edu/resources/horse-coat-color): authoritative reference for named equine coat-color loci and testing context. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

- [DMC Japan — Misaki Horses at Cape Toi](https://www.dmcjapan-knt.com/products/2608_01.html): Cape Toi setting, more-than-300-year history, continued feral status, and low-intervention herd management. [dmcjapan-knt](https://www.dmcjapan-knt.com/products/2608_01.html)