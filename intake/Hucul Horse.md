The **Hucul Horse**—also written **Hutsul**, **Hucul Pony**, or **Carpathian Pony**—should be a rare, highly recognizable Eastern Carpathian mountain breed centered on bay and dun genetics. Unlike many breed files that must approximate base-color frequencies, the Polish Hucul population has published allele-frequency data for agouti, extension, dun, tobiano-region KIT, and gray, making this one of the strongest evidence-based primitive-coat implementations in the mod. [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf)

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied earlier could not be retrieved through the available reader. This file follows the readable proposed convention from the example. Before compiling, replace any proposed field, allele, source, and enum names with their exact equivalents in `common/breed/spec/`.

## Identity & flavour

The **Hucul Horse**, also called the **Hutsul**, **Hucul Pony**, or **Carpathian Pony**, is an indigenous mountain horse of the Eastern Carpathians. Its historical homeland is the Hutsul region around the Carpathian arc—especially present-day Romania, Ukraine, Poland, and Slovakia—and breeding populations now extend into Hungary and the Czech Republic. The breed developed through long adaptation to the mountains and may reflect old local Carpathian horses alongside oriental, Tatar, Turkish, Arabian, Przewalski-like, and Noriker influences. It is not a recently invented “primitive look” pony: it is a long-lived regional working type shaped by terrain, sparse forage, weather, and people who needed a horse to get safely across the mountains. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11675560/)

The Hucul was made for mountain travel, pack work, light draught, forestry, farm work, riding, and long-distance trekking. In modern use it remains well suited to recreational riding, endurance-style trail work, driving, hippotherapy, sustainable tourism, and the Hucul Path—an obstacle-and-trail competition designed to test the exact qualities that mattered historically: balance, courage, tractability, obedience, and calm movement over uneven country. It should feel like a horse that will pick its way across a rocky stream bed, tolerate poor weather, carry an adult without complaint, and keep enough sense not to throw itself into danger. [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses)

Hucul mares typically stand **13.4–13.9 hands**, while stallions stand about **13.6–14.1 hands**. The body is compact, broad, and strong, with a short practical head, short muscular neck, low sturdy frame, short legs, and famously sound feet. It has the depth and bone of a mountain workhorse compressed into a pony-sized package; its thick, often dark mane and tail enhance the primitive impression, but the key silhouette is a dense, low, sure-footed Carpathian horse rather than a refined riding pony. [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses)

The Hucul’s famous coat is **dun**, often accompanied by a dorsal stripe and zebra bars on the legs. Bay, black, chestnut, and gray also occur, but the dun family—bay dun, red dun, and especially mouse/grullo dun—is the visual anchor. Polish allele-frequency research estimated wild-type recessive frequencies of 0.521 for agouti, 0.115 for extension, and 0.878 for the dun locus; converted to dominant allele pools, that supports `A` 0.479, `E` 0.885, and `D` 0.122. The same study found a small tobiano-region KIT rate and gray essentially absent from that Polish sample, while later Hucul research warned that recorded colors may be misclassified without testing TBX3 and ASIP. In other words: the Hucul belongs in the mod precisely because its primitive markings should emerge from real inheritance, not from manually painted textures. [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf)

The Hucul has repeatedly faced conservation pressure as agriculture mechanized and small mountain-horse populations became isolated. It entered the FAO protected gene fund for original and primitive animal breeds in 1979; the Hucul International Federation was formed in 1994, and Poland’s Hucul studbook is recognized as the Studbook of Origin. Poland’s current conservation program preserves 14 maternal families and 7 male lines, while other national programs maintain closely related populations across the Carpathian region. In Procedural Horse Genetics, players should recognize a Hucul instantly: compact, deeply built, mountain-ready, and often striped by dun. The mod does not model dorsal-stripe placement, leg-bar count, countershading, mane thickness, foot quality, mountain trail judgement, endurance conditioning, the Hucul Path, lineage-family eligibility, or national studbook rules. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11675560/)

## Breed JSON

```json
{
  "id": "hucul_horse",
  "name": "Hucul Horse",
  "type": "natural",
  "notes": "The Hucul Horse is defined by compact Carpathian mountain conformation, sound feet, thick mane and tail, primitive dun markings, mountain trail sense, pack and light-draught usefulness, long-distance stamina, and the regional studbook system preserving distinct founder families and stallion lines. Procedural Horse Genetics does not model dorsal-stripe width or placement, zebra-bar number, countershading, mealy pattern, mane/tail density, hoof hardness, exact mountain sure-footedness, Hucul Path performance, pack capacity, endurance training, hippotherapy suitability, maternal-family identity, stallion-line eligibility, or registry-specific breeding rules.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:meadow",
    "minecraft:grove",
    "minecraft:old_growth_pine_taiga",
    "minecraft:taiga",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
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
  "price": 820,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.885,
      "e": 0.115
    },
    "agouti": {
      "A": 0.479,
      "a": 0.521
    },

    "dun": {
      "N": 0.878,
      "D": 0.122
    },
    "tobiano": {
      "N": 0.929,
      "To": 0.071
    },

    "grey": {
      "N": 0.997,
      "G": 0.003
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
    "jump": 5,
    "health": 10,
    "size": [
      0.91,
      1.01
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Extension | `E` 0.885 / `e` 0.115 | The Polish Hucul study estimated the recessive `e` frequency at 0.115. This directly supports a predominantly black-pigment-capable population with uncommon chestnut/red-dun founders.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf) |
| Agouti | `A` 0.479 / `a` 0.521 | The same study estimated recessive `a` at 0.521. This makes black and mouse-dun backgrounds genetically important, even where recorded phenotype labels call many horses bay or brown.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf) |
| Dun | `D` 0.122 | The source gives recessive wild-type `d` at 0.878, so the dominant `D` estimate is 0.122. This generates a meaningful but not overwhelming dun population and allows bay dun, red dun, and mouse/grullo dun with the expected primitive striping.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf) |
| Tobiano | `To` 0.071 | The study’s recessive wild-type frequency at the KIT tobiano region was 0.929, yielding `To` 0.071. Tobiano is documented as rare in broader Hucul descriptions, so this rate provides unusual but real tobiano founders rather than treating pinto as standard.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf) |
| Gray | `G` 0.003 | The Polish allele study estimated wild-type `g` at 0.997, leaving `G` at 0.003. Gray is therefore possible but truly exceptional in this particular Polish-conservation-style founder pool, even though some international Hucul sources list gray among possible colors.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf) |
| Base color labels | No shade band | A 2019 study showed that recorded Hucul coat colors may disagree with ASIP and TBX3 genotypes. Do not force “dark bay” or use appearance-only strain labels; let molecular loci decide the base coat and dun expression.  [sciencedirect](https://www.sciencedirect.com/science/article/pii/S1751731118003506) |
| Other dilutions/patterns | Forced wild type | No defensible Hucul-specific evidence was found for cream, pearl, champagne, silver, mushroom, flaxen, frame, splash, W-series white, roan, rabicano, leopard complex, brindle, or magical loci. |
| Disorders | All clear | No credible Hucul-specific carrier-rate data was located for the listed inherited disorders. The breed’s hardiness is real but does not justify inventing either disease risk or disease immunity. |
| Speed | 5/10 | A Hucul is not a flat-racing breed, but it should cover difficult terrain efficiently, carry adults, and retain enough all-round athleticism for trekking and the Hucul Path.  [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses) |
| Jump | 5/10 | Mountain agility and obstacle-based Hucul Path work justify a middle score, without claiming dedicated show-jumping selection.  [prestigemedia](https://prestigemedia.cz/en/2025/12/10/the-hucul-a-horse-with-the-soul-of-the-mountains/) |
| Health | 10/10 | Represents outstanding ecological hardiness, sound feet, mountain adaptation, and low-input longevity—not immunity from illness or a substitute for genetic testing.  [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses) |
| Size | ×0.91–1.01 | Produces a compact but adult-ridable mountain horse, consistent with documented mare and stallion height bands while keeping literal height outside the JSON.  [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses) |

## Code map

| Location | Hucul Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `hucul_horse` natural breed record, its metadata, notes, sources, biomes, commonness, price, coat pools, clear disease panel, and body targets. |
| `common/breed/Breeds` | Register `hucul_horse` so the ID resolves for spawning, founder generation, menus, books, stable systems, and persisted horse data. |
| `common/breed/BreedSource` | Use all four ordinary source flags: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Preserve an empty epigenetic band map. Primitive markings should derive from the dun locus and the mod’s normal renderer, rather than an arbitrary color-shade band. |
| `common/breed/spec/` | Implement both JSON read and write mappings, replacing the proposed names with the exact current schema vocabulary where necessary. |
| `common/breed/Commonness` | Confirm that `RARE` maps to the intended 1.5 numerical spawn weight. |
| `common/breed/BreedStatCurve` | Map speed 5, jump 5, health 10, and compact size ×0.91–1.01 to legal `TargetBand` objects. |
| `common/breed/BreedFounder` | Roll extension, agouti, dun, tobiano, and gray from the stated pools; force all unnamed coat loci wild type and all disease loci clear. |
| `common/breed/BreedLineage` | No Hucul-specific lineage exception is necessary. |
| `common/genetics/SpliceOutcome` | No changes: the splice carrot should use normal allele-transfer and foal-inheritance behavior. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use the ordinary axes and band objects to encode a compact, versatile, extremely hardy mountain horse. |
| `common/breed/BandType` | No custom `TRADITIONAL` or `BACHELOR` handling is needed unless required by the project’s current serialization format. |

## Verification

1. Confirm **Hucul Horse** appears in the H-menu’s Breeds tab and in the breed book with its natural designation, rare status, Carpathian mountain biome list, source checklist, price, and conservation-oriented notes.

2. Trigger breed-selected wild herd generation in an eligible peak, grove, taiga, meadow, or windswept-forest biome. Every horse in the generated pack should display **Hucul Horse** as its breed, regardless of whether it is bay, black, chestnut, dun, tobiano, or the exceedingly rare gray.

3. Verify that an ordinary lone wild horse that did not come from the breed-pack generator reads **Feral Mixed**, not Hucul Horse.

4. Generate at least 2,000 founders and inspect loci. The aggregate pool should approximate `E` 0.885 / `e` 0.115, `A` 0.479 / `a` 0.521, `D` 0.122, `To` 0.071, and `G` 0.003. Large samples are important because gray is expected to be extremely uncommon.

5. Confirm normal locus interactions:
   - `A_ E_ D_` should produce bay dun.
   - `aa E_ D_` should produce mouse/grullo dun.
   - `ee D_` should produce red dun.
   - `aa E_ dd` should produce black.
   - `ee dd` should produce chestnut.
   - `To` should produce rare tobiano descendants.
   - `G` should eventually gray any underlying base color.

6. Confirm that pure Hucul founders never generate cream, pearl, champagne, silver, mushroom, flaxen, frame overo, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical traits.

7. Check the disorder panel in a large founder sample. All named disorder loci must remain clear. Any disease allele in a later Hucul-descended line should be traceable to an outcross or a genetics-tool intervention.

8. Compare it against a racing breed, a sport jumper, and a heavy draught horse. The Hucul should feel compact, durable, steady, and terrain-capable, with moderate all-round speed and jump—not dominant in any specialist performance category except heartiness.

## Sources

- [Mackowski et al., *Czech Journal of Animal Science* — “Allele Frequency in Loci Which Control Coat Colours in Hucul Horse Population in Poland”](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf): principal source for the exact Polish Hucul allele estimates used here—recessive frequencies of 0.521 at ASIP, 0.115 at MC1R, 0.878 at DUN, 0.929 at the KIT tobiano region, and 0.997 at STX17 gray. [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2012/04/04.pdf)

- [Mackowski et al., *Animal* — “TBX3 and ASIP Genotypes Reveal Discrepancies in Officially Recorded Coat Colors of Hucul Horses”](https://www.sciencedirect.com/science/article/pii/S1751731118003506): supports the decision to trust genotype logic rather than force traditional color labels; the study found that officially recorded Hucul colors may not agree with TBX3 and ASIP genotype predictions. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S1751731118003506)

- [Oklahoma State University Breeds of Livestock — Hucul Horses](https://breeds.okstate.edu/horses/hucul-horses): source for the historical Carpathian distribution, mare and stallion height ranges, mountain-horse type, major uses, typical colors and primitive markings, FAO protected-gene-fund status, and Hucul International Federation history. [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses)

- [Smołucha et al., *Animals* — “Genetic Composition of Polish Hucul Mare Families: mtDNA Diversity”](https://pmc.ncbi.nlm.nih.gov/articles/PMC11675560/): peer-reviewed conservation source for Hucul origins, current Polish genetic-resource protection, the Hucul Horse Studbook, 14 female families, 7 male lines, and continuing conservation need. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11675560/)