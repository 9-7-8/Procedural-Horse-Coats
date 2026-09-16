The **Tarpan** should be a **magical/extinct reconstruction breed**, not a natural modern breed: no living studbook can supply a true contemporary founder pool, and the historical “tarpan” itself was a poorly documented western Eurasian free-ranging horse population whose exact taxonomic and genetic status remains debated. The scientifically strongest mod treatment is a compact, extremely hardy Eurasian steppe horse centered on bay, black, bay dun, and black dun, with a low but nonzero ancient `LP` component—rather than the old pop-culture rule that every Tarpan was a uniform gray grullo. [gbif](https://www.gbif.org/species/100373356)

## Identity & flavour

The **Tarpan**, often called the **Eurasian wild horse**, is the traditional name for the extinct free-ranging horse population of western Eurasia, conventionally written *Equus ferus ferus*. It was documented in eastern and central Europe, the Pontic–Caspian steppe, and parts of western Asia during the eighteenth and nineteenth centuries. “Tarpan” was not a modern breed name, and there was no reliable closed studbook, registry, or coherent domestic breeding standard. The animal disappeared through habitat loss, hunting, capture, and hybridization with domestic horses; the last widely accepted captive individual died in the Russian Empire in **1909**, though the identity of some last animals and the final extinction date remain disputed. [gbif](https://www.gbif.org/species/100373356)

For centuries, the Tarpan was imagined as Europe’s last true wild horse, a small, shaggy gray-dun animal haunting forest and steppe. That image inspired twentieth-century “breeding-back” projects, including the Heck horse and later Tarpan-like reconstructions. Those horses can resemble historical descriptions, but they are domestic descendants selected for appearance; they are **not resurrected Tarpans**. Modern ancient-DNA research also complicates the old story: some historical “tarpan” specimens show domestic-horse admixture, and archaeological evidence shows that prehistoric western Eurasian horses carried more than one coat phenotype. A mod should preserve the mystery without presenting disputed folklore as settled biology. [theextinctions](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj)

Historical descriptions portray a compact horse about **13.3–14.1 hands**: narrow-headed, deep-bodied, short-backed, strong-legged, and built for movement across open country. The mane was thick, dark, and falling rather than the fully upright mane of Przewalski’s horse; the tail was coarse and dark. A dark muzzle, dark lower legs, dorsal stripe, shoulder stripes, and other primitive markings were frequently reported. The overall silhouette should be a small, hardy, dry-built steppe horse—less polished than an Iberian saddle horse, less bulky than a modern cob, and less pony-like than a Shetland. [en.wikipedia](https://en.wikipedia.org/wiki/Eurasian_wild_horse)

The old textbook Tarpan color was **grullo** or mouse dun: a smoky gray-black body, dark mane and tail, dark legs, dorsal stripe, and shoulder stripes. True dun remains appropriate as a major signature because `TBX3` dun produces exactly that pale-body/dark-points/primitive-marking phenotype. But it should not be made the sole possible founder phenotype. Ancient wild-horse data indicate bay, bay dun, black, black dun, and leopard-spotted horses in prehistoric western Eurasian populations; chestnut is not supported as a dominant reconstructed Tarpan color, while gray, cream, champagne, silver, roan, and modern pinto patterns have no good basis for a strict extinct-population founder pool. [sciencenordic](https://www.sciencenordic.com/ancient-dna-animals-dna/wild-horses-lost-their-camouflage-because-of-humans/1428075)

In Procedural Horse Genetics, Tarpan should feel like an artifact recovered from prehistory: rare, wary, small, deeply hardy, mostly dun-marked, and genetically unlike a conventional domestic breed. Players should see bay dun and black dun most often, with occasional undiluted bay or black and exceptionally rare leopard-complex animals. It belongs in the mod as an extinct, conservation-mythic counterpart to modern primitive breeds—but it must remain clearly distinct from Konik, Sorraia, Heck horse, and Przewalski’s horse. The mod does not model extinct-population uncertainty, ancient DNA sampling bias, domestic admixture, exact mane form, seasonally shaggy coats, wild behavior, predator avoidance, steppe migration, fertility, archaeological chronology, taxonomic debate, or the real genetic distance between historical Tarpans and visual “breeding-back” recreations. [theextinctions](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This JSON follows the readable convention used in your supplied example. Before compilation, reconcile the exact `type` enum (`magical`, `extinct`, or any equivalent), all locus IDs, allele names, source values, price units, commonness ladder, and target-band syntax with `common/breed/spec/`.
>
> **Scientific-status caveat:** This is an intentionally **reconstructed genotype pool**, not a claim that we know the exact genome of historical Tarpans. The rules use ancient wild-horse coat evidence and documented nineteenth-century phenotype descriptions as constraints. Exact founder rates are transparent gameplay approximations, not measured Tarpan allele frequencies. If the mod has an `extinct` category separate from `magical`, use that category instead; under the prompt’s two-category choice, use `"type": "magical"` only as the mod’s vehicle for non-natural/extinct content. [en.wikipedia](https://en.wikipedia.org/wiki/Eurasian_wild_horse)

```json
{
  "id": "tarpan",
  "name": "Tarpan",
  "type": "magical",
  "notes": "The Tarpan is an extinct and scientifically uncertain western Eurasian wild-horse reconstruction, not a living registered breed. The mod models a constrained ancient-style coat pool, small steppe-horse size, and exceptional hardiness, but does not model extinct-population uncertainty, ancient-DNA sampling bias, domestic admixture, taxonomic debate, exact geographic range, shaggy seasonal coat, falling mane, coarse tail, wild social behavior, predator avoidance, migration, forage ecology, historical human persecution, or the distinction between an actual historical Tarpan and a modern Heck horse, Konik, or other breeding-back reconstruction.",

  "biomes": [
    "minecraft:plains",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:grove",
    "minecraft:stony_peaks"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "spawn_egg",
    "stable"
  ],
  "price": 1800,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.91,
      "e": 0.09
    },
    "agouti": {
      "A": 0.58,
      "a": 0.42
    },

    "dun": {
      "N": 0.20,
      "D": 0.80
    },
    "leopard_complex": {
      "N": 0.96,
      "LP": 0.04
    },
    "patn1": {
      "N": 0.985,
      "PATN1": 0.015
    },
    "patn2": {
      "N": 0.99,
      "PATN2": 0.01
    },

    "grey": {
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
    "speed": 6,
    "jump": 4,
    "health": 10,
    "size": [
      0.88,
      0.98
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "steppe_dun",
      "name": "Steppe Dun",
      "weight": 0.85,
      "notes": "The primary reconstruction strain: mostly bay dun and black dun, with dorsal stripe, dark legs, and other true-dun primitive markings. This is the historical grullo/mouse-dun visual that players expect, without falsely making it the only possible ancient wild-horse phenotype.",
      "coat_genes": {
        "extension": {
          "E": 0.94,
          "e": 0.06
        },
        "agouti": {
          "A": 0.54,
          "a": 0.46
        },
        "dun": {
          "N": 0.08,
          "D": 0.92
        },
        "leopard_complex": {
          "N": 1.0
        },
        "patn1": {
          "N": 1.0
        },
        "patn2": {
          "N": 1.0
        }
      }
    },
    {
      "id": "ancient_leopard",
      "name": "Ancient Leopard",
      "weight": 0.05,
      "notes": "An intentionally rare ancient-DNA-inspired reconstruction. It carries leopard complex and an elevated pattern-modifier chance, representing evidence that leopard-spotted phenotypes occurred in prehistoric western Eurasian wild-horse populations. It is not evidence that nineteenth-century Tarpans were commonly Appaloosa-patterned.",
      "coat_genes": {
        "extension": {
          "E": 0.92,
          "e": 0.08
        },
        "agouti": {
          "A": 0.60,
          "a": 0.40
        },
        "dun": {
          "N": 0.65,
          "D": 0.35
        },
        "leopard_complex": {
          "N": 0.05,
          "LP": 0.95
        },
        "patn1": {
          "N": 0.30,
          "PATN1": 0.70
        },
        "patn2": {
          "N": 0.45,
          "PATN2": 0.55
        }
      }
    },
    {
      "id": "undiluted_wildtype",
      "name": "Undiluted Wild-Type",
      "weight": 0.10,
      "notes": "A minority ancient-style bay or black strain without full dun dilution. It acknowledges that wild western Eurasian horses were not genetically or visually identical, while keeping the population strongly primitive and non-domestic in overall character.",
      "coat_genes": {
        "extension": {
          "E": 0.90,
          "e": 0.10
        },
        "agouti": {
          "A": 0.66,
          "a": 0.34
        },
        "dun": {
          "N": 0.92,
          "D": 0.08
        },
        "leopard_complex": {
          "N": 1.0
        },
        "patn1": {
          "N": 1.0
        },
        "patn2": {
          "N": 1.0
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Tarpan × Tarpan produces Tarpan within the mod's extinct-reconstruction category. Tarpan × any living pure breed produces a Tarpan cross. A Tarpan cross bred back to pure Tarpan remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not identify Koniks, Heck horses, Sorraias, Przewalski's horses, or any dun primitive pony as Tarpan merely from phenotype; all modern look-alikes descend from living domestic or surviving wild lineages, not from a restored extinct Tarpan genome."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Classification | `type: "magical"` as extinct-content proxy | The prompt permits only `natural` or `magical`. A Tarpan cannot honestly be a present-day natural breed; use a dedicated extinct category instead if the code supports one. |
| No wild spawning | `spawn_egg`, `stable` only | Tarpans are extinct. They should not occur as ordinary wild herds in any natural Minecraft biome. A stable source can represent a conservation archive, reconstructed breeding program, archaeological revival, or configured world feature. |
| Rarity | `VERY_RARE`, weight `0.75` | Appropriate for a deliberately recreated extinct population. |
| Base pigment | `E: 0.91 / e: 0.09`; `A: 0.58 / a: 0.42` | Ancient western Eurasian horse evidence supports bay and black phenotypes; the very low `e` setting avoids creating an unsupported chestnut-heavy population. Exact rates are reconstruction assumptions.  [theextinctions](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj) |
| Dun | `D: 0.80` overall; strongly enriched in Steppe Dun strain | Dun is historically associated with classic Tarpan reconstructions and produces the dorsal stripe, dark points, and primitive markings documented in accounts. It is appropriate as the main phenotype, but ancient evidence does not justify universal fixation.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/) |
| `nd1` and `nd2` | Do not seed unless schema distinguishes them | If `TBX3` uses `D/nd1/nd2`, `D` should dominate; `nd1` may explain primitive markings without body dilution but should be uncommon; `nd2` belongs in the undiluted strain. Do not represent all non-dun horses as identical.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/) |
| Grullo/mouse dun | Emergent \(E\_\,aa\,D\_\) phenotype | This is the correct genotype for a black-based dun reconstruction. It must not be implemented through true gray.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse) |
| Bay dun | Emergent \(E\_\,A\_\,D\_\) phenotype | Ancient and historical wild-horse reconstructions support brown/bay-dun possibilities alongside black dun.  [theextinctions](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj) |
| Leopard complex | `LP: 0.04` overall, concentrated in 5% Ancient Leopard strain | Ancient-DNA-oriented reconstructions indicate leopard spotting occurred among prehistoric western Eurasian wild horses. This supports an exceptionally rare strain, not ordinary nineteenth-century Tarpan leopards.  [en.wikipedia](https://en.wikipedia.org/wiki/Dun_gene) |
| Gray | Forced wild type | The historical “gray tarpan” label commonly describes mouse dun/grullo. There is no basis to seed the progressive gray allele in this strict reconstruction.  [en.wikipedia](https://en.wikipedia.org/wiki/Eurasian_wild_horse) |
| Cream, champagne, silver, pearl, mushroom | Forced wild type | These modern or domestically selected dilution families have no adequate evidence for the reconstructed population. |
| Modern white patterns | Tobiano, sabino, frame, splash, KIT W-series, roan, rabicano forced wild type | No reliable historical/ancient evidence justifies building a modern pinto/roan founder population. The rare `LP` strain is treated separately because ancient evidence directly supports leopard spotting. |
| Disorders | All listed loci clear | No living Tarpan population exists from which disease carrier frequencies could be responsibly measured. Seed no modern breed-associated disease allele by speculation. |
| Speed | `6/10` | Small size does not imply slow movement; steppe survival and historical accounts support practical travel speed, not specialist racehorse sprinting. |
| Jump | `4/10` | A rugged wild horse may be agile, but there is no evidence of selection for modern jumping scope. |
| Health | `10/10` | Represents ecological hardiness and a low-input survival phenotype—not a claim that extinct animals were genetically disease-free, nor a substitute for separate inbreeding/mortality systems. |
| Size | `×0.88–0.98` | Matches the commonly cited 140–145 cm, approximately 13.3–14.1-hand, historical shoulder-height range.  [en.wikipedia](https://en.wikipedia.org/wiki/Eurasian_wild_horse) |

## Disorder approach

Every named disorder locus is forced **clear**.

This is not because Tarpans were necessarily healthier than domestic horses. It is because a real Tarpan population no longer exists and no valid carrier-frequency survey can be performed for the mod’s modern disease loci. Inventing rates for ACAN dwarfism, PLOD1/WFFS, MET/EDNRB lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA would be scientifically indefensible.

The `LP` allele deserves a separate implementation note:

- Leopard complex is a coat locus, not one of the listed disorder genes.
- In real horses, `LP/LP` is associated with congenital stationary night blindness through `TRPM1` biology.
- The mod should only add that health/vision effect if it already has a scientifically accurate `LP`/CSNB system.
- Do not inaccurately map it onto `PRKDC`, `TOE1`, `MYO5A`, `GBE1`, `PPIB`, or another unrelated disease locus.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `tarpan` as an extinct-reconstruction record with an appropriate category, non-wild sources, very-rare rarity, reconstruction notes, gene pools, strains, and steppe-focused stat targets. |
| `common/breed/Breeds` | Register the breed for commands, stable/archive sources, spawn eggs, breed books, H-menu display, lineage labels, and save serialization. |
| `common/breed/BreedSource` | Validate `spawn_egg` and `stable`. Do not add `wild` unless the world deliberately includes an alternate-history or magical resurrection biome. |
| `common/breed/BreedBands` | Keep bands empty. Do not model shaggy seasonal coats, dark muzzle intensity, primitive-striping strength, or wild behavior through unrelated epigenetic bands. |
| `common/breed/spec/` | Add support for the correct extinct/magical category and verify the three-strain structure. Confirm how the actual schema represents `D`, `nd1`, `nd2`, `LP`, `PATN1`, and `PATN2`. |
| `common/breed/Commonness` | Confirm `VERY_RARE` corresponds to `0.75`, the requested lowest commonness rung. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 4, health 10, and size ×0.88–0.98 into valid `TargetBand` targets. |
| `common/breed/BreedFounder` | Choose a reconstruction strain first, then generate only from its constrained gene pool. For Ancient Leopard, require `LP`; for Steppe Dun, strongly require `D`; force every modern unsupported color and named disease locus clear. |
| `common/breed/BreedLineage` | Keep default pure/cross/Mixed behavior, but retain `Tarpan` as an extinct reconstruction label, never an auto-label for ordinary dun horses. |
| `common/genetics/SpliceOutcome` | No special transmission exception. A spliced allele can reach an offspring normally, but the resulting nonstandard animal remains a Tarpan cross or an atypical pure-lineage descendant depending on the mod’s lineage rule. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the compact size, strong survival health, practical speed, and modest jump targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed. Do not confuse the Steppe Dun reconstruction strain with a code-level `TRADITIONAL` band type. |

## Verification

1. Confirm **Tarpan** appears in the H-menu’s Breeds tab and breed book as an extinct reconstruction, with very-rare rarity, non-wild sources, western Eurasian steppe flavour, scientific uncertainty warning, and explicit distinction from Konik, Sorraia, Heck horse, and Przewalski’s horse.

2. Confirm that Tarpan does **not** spawn in ordinary wild packs because it is extinct and its source checklist omits `wild`.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is small, grullo, bay dun, has a dorsal stripe, or occupies a windswept steppe biome.

4. Generate at least 1,000 founders. The population should be:
- Mostly Steppe Dun: bay dun and black dun/grullo, with dorsal stripe and primitive markings.
- A minority undiluted bay or black population.
- A very rare leopard-complex ancient-reconstruction outcome.
- Not dominated by chestnut, gray, palomino, buckskin, champagne, silver, modern pinto patterns, or roan.

5. Inspect genes:
- `D` should be very common overall and nearly fixed in the Steppe Dun strain.
- `LP` should occur almost exclusively in the 5% Ancient Leopard strain.
- `PATN1` and `PATN2` should remain scarce overall but enriched in the Leopard strain.
- `G`, cream, pearl, champagne, silver, mushroom, flaxen, pinto loci, roan, rabicano, brindle, magical loci, and every requested disorder locus must remain wild type or clear.

6. If the mod distinguishes `D`, `nd1`, and `nd2`, verify:
- Steppe Dun founders carry `D`.
- The undiluted strain may carry the non-dun form selected by the actual schema.
- No founder mistakenly gets a dorsal stripe through a fake `magical_zebra` locus.

7. Confirm all listed disorder loci remain clear in a large founder sample. The extinct reconstruction must not accidentally inherit Quarter Horse, Arabian, Friesian, or warmblood disease rates from unrelated breed templates.

8. Test default lineage:
- Tarpan × Tarpan → Tarpan.
- Tarpan × Konik → Tarpan cross.
- Tarpan × Sorraia → Tarpan cross.
- Tarpan cross × pure Tarpan → existing Tarpan cross.
- Two different crosses → Mixed.
- Any cross with Feral Mixed → Mixed.

9. Check mature physical behavior. Tarpans should be smaller than ordinary warmbloods, extremely hardy, practically quick across open terrain, and modest at jumping. They should not eclipse specialist racers, sport jumpers, or heavy work horses.

10. Test deliberate alteration. Introduce gray, tobiano, silver, champagne, cream, `nd2`, PSSM1, or other modern alleles through outcrossing or splice mechanics. The altered animal must inherit normally but should never be presented in breed-book flavour as a historically standard Tarpan.

## Sources

- [GBIF — *Equus ferus* Boddaert, 1784](https://www.gbif.org/species/100373356): biodiversity-database context for the historical Eurasian wild-horse population and loss of free-ranging forest/steppe forms. [gbif](https://www.gbif.org/species/100373356)

- [Tarpan / Eurasian wild horse overview](https://en.wikipedia.org/wiki/Eurasian_wild_horse): supplementary historical description of the last captive animal’s 140–145 cm height, grullo coat, dark legs, falling mane, dorsal stripe, shoulder stripes, and reported 1909 death. [en.wikipedia](https://en.wikipedia.org/wiki/Eurasian_wild_horse)

- [Tarpan overview](https://en.wikipedia.org/wiki/Tarpan): supplementary synthesis of extinction chronology uncertainty, historical specimen ambiguity, archaeological genetics, and the distinction between historical Tarpans and reconstructed look-alikes. [en.wikipedia](https://en.wikipedia.org/wiki/Tarpan)

- [Regulatory mutations in `TBX3` disrupt asymmetric hair pigmentation in dun horses](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/): peer-reviewed molecular evidence for true dun dilution and primitive markings. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/)

- [UC Davis Veterinary Genetics Laboratory — Dun Dilution](https://vgl.ucdavis.edu/test/dun-horse): authoritative explanation of dun phenotype and `D`/`nd1`/`nd2` biology. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)

- [Wild horses lost their camouflage because of humans](https://www.sciencenordic.com/ancient-dna-animals-dna/wild-horses-lost-their-camouflage-because-of-humans/1428075): accessible account of ancient-DNA work on dun and non-dun alleles in prehistoric horses. [sciencenordic](https://www.sciencenordic.com/ancient-dna-animals-dna/wild-horses-lost-their-camouflage-because-of-humans/1428075)

- [European wild horse extinction and phenotype reconstruction](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj): synthesis of uncertainty around late historical “tarpan” animals and ancient evidence for bay, bay-dun, black, black-dun, and leopard phenotypes. Use as supplementary interpretation, not as the sole genetic authority. [theextinctions](https://www.theextinctions.com/articles-1/5wa1nzaq7zveeuawomiqu2n3c6cfdj)