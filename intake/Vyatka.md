The **Vyatka** should be modeled as a rare Russian northern forest-and-meadow horse with a strong **true-dun** identity, common mouse-dun/grullo and bay-dun phenotypes, compact size, enormous winter hardiness, and a real, breed-specific `GYS1` PSSM1 allele frequency of **0.189**. It is not merely a “dun pony”: it is a historically important Vyatka/Kirov harness, farm, and travel horse whose modern conservation program must balance prized primitive color with genetic diversity and disease management. [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect)

## Identity & flavour

The **Vyatka horse**, Russian *Vyatskaya loshad* (Вятская лошадь), is a small northern Russian native horse from the Vyatka River basin—today centered around the Kirov region and neighboring parts of Udmurtia, Perm Krai, and the northern Volga–Ural country. It is one of Russia’s oldest native horse populations, shaped over centuries by forest, meadow, marshy river valleys, snowbound winters, poor forage, deep mud, and the demands of isolated rural travel. The breed’s name comes from the historical Vyatka region, not from a single modern state stud; it emerged as a regional landrace long before formal twentieth-century conservation programs. [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867)

Vyatkas were made for the hard practical work of the Russian north. They carried riders and loads, pulled sledges, carts, and farm implements, worked in harness, traveled long distances over snow and forest tracks, and served as dependable village horses where a large imported draft horse would cost too much to feed. Historical accounts praised their endurance, thrift, speed at the trot, safe footing, and ability to work in winter. A Vyatka should therefore feel like a small all-weather utility horse: not tall, not a show jumper, but unusually useful through snow, mud, woodland, and long overland routes. [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867)

A typical Vyatka stands about **140–150 cm**, roughly **13.3–14.3 hands**. It is compact, broad, and strong for its height, with a plain dry head, broad forehead, straight profile, short muscular neck, deep chest, wide body, short strong back, powerful sloping croup, low or moderate withers, short stout legs, and exceptionally sound dark hooves. The mane and tail are thick, as is the winter coat, but it does not have dramatic draft feathering. Its build should look like a northern harness horse: close-coupled, low to the ground, solid in the barrel, and capable of working all day without looking like a miniaturized draft giant. [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867)

The Vyatka’s coat is its most visible signature. Dun shades predominate, especially **bay-brown**, **savrasaya** bay dun, **mouse dun/grullo**, and darker black-based primitive coats. Modern genetic work on this color system found the dominant `TBX3` true-dun allele at **0.30** in one closely studied native-horse population, while a Vyatka-specific marker study describes dun and grullo as prevailing colors and notes that dun-plus-cream phenotypes are commercially desirable. The important biological point is that true dun produces a pale or smoky body with dark points, dorsal stripe, shoulder markings, leg barring, and sometimes cobwebbing—not merely a randomly painted stripe. The core pure pool should use `D` often, `nd1` moderately, and `nd2` as the non-dun background. [scispace](https://scispace.com/papers/evaluation-of-genetic-markers-in-the-analysis-of-the-colors-2zw119stc1)

Vyatkas are known for quiet intelligence, courage, sure-footedness, cold tolerance, fertile practical stockmanship, and fast useful harness movement. Their hidden complication is **PSSM1**: a Vyatka population-genomics study reports the `GYS1` allele at frequency **0.189**. PSSM1 is autosomal dominant, so heterozygous horses are genetically affected/risk-positive—not harmless recessive carriers—and must be managed carefully in a realistic genetics system. In Procedural Horse Genetics, a player should recognize a Vyatka as a compact, thick-maned, primitive-marked northern horse, usually bay dun or grullo, with top-tier heartiness but an important health-testing reason to avoid careless breed-wide pairings. The mod does not model winter coat length, snow travel, sledge work, fast harness trot, hoof hardness, feed thrift, PSSM1 diet/exercise management, regional line names, seasonal adaptation, or real conservation breeding decisions. [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the supplied example. Before compiling, reconcile actual locus IDs, `D/nd1/nd2` syntax, `GYS1` PSSM1 allele names, disease-effect handling, source enums, commonness labels, and body-stat-band serialization with `common/breed/spec/`.
>
> **Critical evidence note:** The retrieved PSSM1 value of **0.189** is described as a **GYS1 allele frequency**, not carrier prevalence. It should be placed directly in the allele pool as `PSSM1: 0.189`, rather than interpreted as 18.9% heterozygous carriers. Under Hardy–Weinberg assumptions, this produces approximately 30.7% heterozygotes, 3.6% homozygotes, and 34.3% horses carrying at least one mutant allele. [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect)

```json
{
  "id": "vyatka",
  "name": "Vyatka",
  "type": "natural",
  "notes": "The Vyatka is a Russian northern native harness, farm, sledge, pack, and riding horse defined by the Vyatka River basin, compact broad-bodied conformation, thick winter coat, full mane and tail, sound dark hooves, cold adaptation, feed thrift, sure-footedness, fast practical harness movement, and commonly primitive-marked dun coloration. Procedural Horse Genetics does not model winter-hair length, snow travel, sledge pulling, harness action, hoof hardness, forage efficiency, regional lineages, PSSM1 diet and exercise management, seasonal adaptation, or real-world conservation selection.",

  "biomes": [
    "minecraft:taiga",
    "minecraft:snowy_taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:grove",
    "minecraft:meadow",
    "minecraft:plains",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:river",
    "minecraft:frozen_river",
    "minecraft:swamp"
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
      "E": 0.78,
      "e": 0.22
    },
    "agouti": {
      "At": 0.36,
      "A": 0.42,
      "a": 0.22
    },

    "dun": {
      "D": 0.30,
      "nd1": 0.25,
      "nd2": 0.45
    },
    "cream": {
      "N": 0.92,
      "Cr": 0.08
    },

    "grey": {
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
      "N": 0.96,
      "f": 0.04
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
    "GYS1_PSSM1": {
      "N": 0.811,
      "PSSM1": 0.189
    },

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
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 5,
    "jump": 4,
    "health": 9,
    "size": [
      0.91,
      1.02
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Vyatka × Vyatka produces Vyatka. Vyatka × another pure breed produces a Vyatka cross. A Vyatka cross bred back to pure Vyatka remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not identify every dun or grullo northern horse as Vyatka: real breed identity depends on Russian Vyatka lineage and breeding-program context, not primitive markings alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Russian northern native harness horse | Vyatka is an indigenous Russian horse shaped by northern forest, river, snow, and farm/harness work rather than modern sport selection.  [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867) |
| Extension | `E: 0.78 / e: 0.22` | The dark bay, black, mouse-dun, and bay-dun core requires abundant black pigment, while a modest `e` pool retains credible chestnut and red-dun variation. The exact frequency is a gameplay estimate.  [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867) |
| Agouti | `At: 0.36 / A: 0.42 / a: 0.22` | The reported population distinguishes bay-brown/dark bay, ordinary bay, and mouse-dun/grullo. A multi-allelic Agouti model gives the renderer the best chance to keep dark bay common while retaining black-based duns. Values are gameplay approximations.  [scispace](https://scispace.com/papers/evaluation-of-genetic-markers-in-the-analysis-of-the-colors-2zw119stc1) |
| True dun | `D: 0.30` | The retrieved record states that the dominant `D` allele frequency in Vyatka is 0.30. This should be treated as the direct founding allele frequency.  [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect) |
| `nd1` and `nd2` | `nd1: 0.25 / nd2: 0.45` | `nd1` preserves some primitive marking without full body dilution, while `nd2` is the fully non-dun form. These values are gameplay texture, not directly measured Vyatka frequencies. The breed’s actual `D` value remains anchored at 0.30.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/30614426/) |
| Cream | `Cr: 0.08` | Vyatka sources describe dun-plus-cream phenotypes as commercially desirable. A modest cream allele lets players produce dunskin, smoky grullo, and red-dun cream combinations without making cream a core identity. This rate is an implementation approximation.  [scispace](https://scispace.com/papers/evaluation-of-genetic-markers-in-the-analysis-of-the-colors-2zw119stc1) |
| Flaxen | `f: 0.04` | Low flaxen allows occasional lighter-maned chestnuts without confusing it with cream dilution. This is not a documented frequency. |
| Gray and other dilutions | Gray, pearl, champagne, silver, mushroom forced wild type | No suitable source supports treating these as ordinary Vyatka founder loci. |
| White patterns | Tobiano, frame, splash, KIT white, roan, leopard, brindle forced wild type | The breed’s desired phenotype is solid and primitive-marked, not pinto or leopard-spotted. Very low SB1/rabicano values only allow restrained ordinary marking texture if the renderer supports it conservatively. |
| Magical loci | All forced wild type | Primitive striping comes from `TBX3` dun, not a magical zebra locus. |
| PSSM1 | `GYS1_PSSM1: 0.189` | A Vyatka study reports a `GYS1` mutant allele frequency of 0.189. PSSM1 is dominant, so `N/PSSM1` horses are genetically affected/risk-positive. This rate is unusually high and should be preserved exactly unless the original paper/data can be checked and contradicts the search extract.  [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect) |
| Other disorders | Forced clear | No defensible Vyatka-specific carrier-rate evidence was found for the remaining requested loci. |
| Speed | `5/10` | Captures a useful fast harness trot and practical travel, not a dedicated racing or modern sport-horse speed score. |
| Jump | `4/10` | Forest/mountain practicality and soundness do not imply selected jumping scope. |
| Health | `9/10` | Represents cold adaptation, durable feet, forage thrift, work capacity, and rural resilience. It must coexist with meaningful PSSM1 management; high heartiness does not cancel a dominant disease risk.  [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867) |
| Size | `×0.91–1.02` | Models a compact 140–150 cm, roughly 13.3–14.3-hand northern work horse.  [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867) |

## Disorder approach

The pure Vyatka founder pool carries exactly one named disorder locus: **PSSM1** at `GYS1`.

The retrieved Vyatka population-genomic source reports the causal PSSM1 allele at frequency **0.189**. Because this is an **allele frequency**, not the percentage of individual horses testing positive, it belongs directly in the founder allele pool:

\[
q_{\mathrm{PSSM1}} = 0.189,\qquad p_N = 0.811
\]

Under Hardy–Weinberg assumptions, this predicts:

| Genotype | Approximate frequency | Interpretation |
|---|---:|---|
| `N/N` | 65.8% | Clear of the known PSSM1 variant. |
| `N/PSSM1` | 30.7% | Dominant mutation present; potentially affected/risk-positive. |
| `PSSM1/PSSM1` | 3.6% | Two mutant alleles; potentially more severely affected. |
| At least one PSSM1 allele | 34.3% | Genetically positive population fraction. |

PSSM1 should not be modeled as a harmless recessive carrier state. `GYS1` PSSM1 is autosomal dominant, and a heterozygous affected horse has approximately a 50% chance of transmitting the mutation to each foal when crossed with a clear mate. [ceh.vetmed.ucdavis](https://ceh.vetmed.ucdavis.edu/health-topics/polysaccharide-storage-myopathy-pssm)

All remaining listed disease loci are clear. No credible Vyatka-specific rates were found for ACAN dwarfism, PLOD1/WFFS, frame-linked lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, or HERDA.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `vyatka` as a natural Russian northern native breed with taiga biome mapping, true-dun genetics, direct PSSM1 allele frequency, compact high-heartiness targets, and breed-book caveats. |
| `common/breed/Breeds` | Register `vyatka` for wild packs, stable access, spawn eggs, H-menu display, breed books, commands, saved-genome loading, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Vyatka is a regional Russian native/conservation work breed rather than a routine sale-yard horse. |
| `common/breed/BreedBands` | Allow an empty epigenetic-band map. Do not fake primitive markings with a shade band; use actual `TBX3` dun. |
| `common/breed/spec/` | Verify `D/nd1/nd2` syntax, the exact `GYS1` PSSM1 locus and allele identifiers, and whether the disease system stores dominant effects separately from allele-frequency pools. |
| `common/breed/Commonness` | Confirm `RARE` matches the requested rarity ladder and retain `spawn_weight: 1.5` if direct numerical spawn weights are valid. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 4, health 9, and size ×0.91–1.02 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll Extension, Agouti, `TBX3` dun, cream, and `GYS1` PSSM1 from their stated pools; force all excluded loci wild type/clear; apply compact northern work-horse stat targets. |
| `common/breed/BreedLineage` | Use the standard pure/cross/Mixed table. A dun or grullo horse must not acquire a Vyatka label merely from color, small size, or taiga biome. |
| `common/genetics/SpliceOutcome` | No exception. Splice-carrot alleles should transmit normally, including an intentionally introduced color gene or disease allele outside the pure Vyatka pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize compact size, practical travel speed, modest jumping, and high heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is required unless the real schema mandates an explicit default. |

## Verification

1. Confirm **Vyatka** appears in the H-menu’s Breeds tab and breed book with Russian northern origin, true-dun identity, PSSM1 warning, compact harness-horse body profile, rare commonness, and cold-forest biome mapping.

2. Spawn repeated packs in taiga, snowy taiga, old-growth taiga, grove, forest, meadow, river, frozen-river, and marsh-edge biomes. Every horse in one selected Vyatka pack must display the **Vyatka** lineage label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is small, dun, grullo, primitive-marked, or generated in a taiga biome.

4. Generate at least 2,000 pure founders. The cohort should contain many bay dun, dark bay dun, mouse dun/grullo, and non-dun bay/dark-bay/black horses. Cream-dun outcomes should appear occasionally but not dominate. Gray, pinto, leopard, champagne, silver dapple, pearl, mushroom, roan, and magical coats must not appear.

5. Inspect `TBX3` behavior:
- The `D` allele frequency should approach **0.30** over a large allele count.
- `D` should create genuine body dilution plus dorsal stripe and primitive markings.
- `nd1` should not create full dun dilution.
- `nd2` should produce the standard non-dun condition.
- No founder should rely on a magical zebra locus for striping.

6. Inspect PSSM1:
- Founder `GYS1` allele frequency should approach **0.189** over a large sample.
- Individual disease-positive frequency should approach roughly **34%**, not 18.9%, if founder genotypes are assembled by random pairing.
- `N/PSSM1 × N/N` should transmit the mutant allele to roughly half of offspring.
- `N/PSSM1 × N/PSSM1` should yield approximately 25% `PSSM1/PSSM1`, 50% heterozygous, and 25% clear outcomes.
- Confirm heterozygotes are flagged as dominant-risk/affected according to the mod’s health system.

7. Confirm all other named disorder loci remain clear in a large founder sample. No WFFS, HYPP, GBED, HERDA, SCID, CA, LFS, frame-lethal-white, or other listed mutation should originate in pure Vyatka founders.

8. Test default lineage:
- Vyatka × Vyatka → Vyatka.
- Vyatka × Yakutian Horse → Vyatka cross.
- Vyatka × Russian Heavy Draft → Vyatka cross.
- Vyatka cross × pure Vyatka → the existing Vyatka cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Check mature stats. Vyatkas should remain compact, high-hearted, and practically quick for a northern work horse, with modest jumping. They should not become race specialists, tall warmbloods, giant drafts, or disease-free “superhorses.”

## Sources

- [Study of population-genomic structure of Vyatka horses in the context of male lines](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect): source reporting the `GYS1` PSSM1 allele frequency of **0.189** in Vyatka horses. [cyberleninka](https://cyberleninka.ru/article/n/study-of-population-genomic-structure-of-vyatka-horses-in-interline-aspect)

- [State of Vyatka horse population in regions of Russia](https://agrojournal.rudn.ru/agronomy/article/view/19867): peer-reviewed population-status and genetic-diversity context for contemporary Vyatka conservation and line management. [agrojournal.rudn](https://agrojournal.rudn.ru/agronomy/article/view/19867)

- [The Genetic Diversity of Horse Native Breeds in Russia](https://www.mdpi.com/2073-4425/14/12/2148): peer-reviewed Russian native-breed genetic diversity comparison including Vyatka. [mdpi](https://www.mdpi.com/2073-4425/14/12/2148)

- [Evaluation of genetic markers in the analysis of the colors and marks of the Vyatka breed](https://scispace.com/papers/evaluation-of-genetic-markers-in-the-analysis-of-the-colors-2zw119stc1): source summary supporting dun and grullo as prevailing Vyatka colors and noting the commercial value of dun-plus-cream outcomes. [scispace](https://scispace.com/papers/evaluation-of-genetic-markers-in-the-analysis-of-the-colors-2zw119stc1)

- [UC Davis Center for Equine Health — PSSM](https://ceh.vetmed.ucdavis.edu/health-topics/polysaccharide-storage-myopathy-pssm): authoritative reference for `GYS1` PSSM1 dominant inheritance and 50% transmission risk from heterozygous horses. [ceh.vetmed.ucdavis](https://ceh.vetmed.ucdavis.edu/health-topics/polysaccharide-storage-myopathy-pssm)