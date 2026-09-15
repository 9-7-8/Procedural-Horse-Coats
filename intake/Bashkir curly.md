The **American Bashkir Curly**—often shortened to **Bashkir Curly**, **Curly Horse**, or **American Curly**—belongs in the mod as a rare, cold-weather North American saddle horse whose identity is written in coat structure rather than coat color. Its founder pool should remain intentionally broad in ordinary color genes, but every true breed founder should carry at least one curly-associated allele; the major gameplay decision is whether the mod can represent the separate dominant **KRT25** and **SP6** curly variants and their hypotrichosis interaction.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

## Identity & flavour

The **American Bashkir Curly Horse**, commonly called the Bashkir Curly, Curly Horse, or American Curly, is a rare North American breed recognized for its striking curly coat, curled mane and tail, and often curly inner-ear hair. Despite the “Bashkir” name, its precise origin remains unresolved; the best-documented North American foundation story centers on the Damele family, who found and bred curly-coated horses in Nevada’s mountain ranges from 1898 onward. The American Bashkir Curly Registry, founded in 1971, is the oldest registry for Curly horses in North America.  [horseillustrated](https://www.horseillustrated.com/horse-breeds-horse-breed-articles-curly-horse/)

Curlies developed as hardy, versatile riding and working horses rather than as a single-discipline specialist. Their rugged Nevada and western-U.S. background favors a horse that can cope with winter conditions, variable forage, rough ground, and long practical days. Today they appear in trail riding, ranch work, driving, dressage, jumping, endurance, western events, and family riding. Their famously quiet, people-oriented disposition and frequently reported suitability for some riders with horse allergies have helped create a devoted following, though an allergy-friendly reputation should never be treated as a medical guarantee.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse)

Most saddle-type Curlies stand about 14.1–15.1 hands, while the breed overall averages around 15 hands. They are usually sturdy rather than massive: broad through the chest and heartgirth, with good bone, strong legs, practical hooves, sloping shoulders, a muscular hindquarter, and a sensible working-horse frame. Some Curly populations show more feather near the feet than a typical stock horse, but not the great draft-horse feather of a Shire or Clydesdale. The breed’s head and conformation can vary because Curly breeding has drawn from several riding-horse and western lines; the curls are the unmistakable signature.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse)

The coat may range from loose waves to dense ringlets. The mane and tail can be wavy or corkscrew-curled; seasonal shedding can make the coat look dramatically different between winter and summer. Curlies occur in essentially every ordinary horse color and pattern: chestnut is often cited as common, but bay, black, gray, buckskin, grulla, cremello, roan, pinto, and Appaloosa-type leopard patterning all occur. A Curly herd should therefore be visually eclectic in pigment and markings, yet instantly recognizable because nearly every horse wears curls.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse)

Genetically, this is much richer than a simple “curly gene.” Two dominant variants have been identified: a **KRT25** variant associated with curly hair plus hypotrichosis/hair reduction, and an **SP6** variant associated with curly hair without hypotrichosis. KRT25 has an epistatic effect over SP6 for the hair-loss expression. In mod terms, players should recognize the breed at a glance by curly coats, but should be able to breed toward the more desirable plush, full-haired SP6-style curly phenotype rather than severe KRT25-associated hair reduction. The mod does not currently model hair texture, ringlet tightness, seasonal coat shedding, hypoallergenic response, mane and tail curl, ear-hair curl, skin irritation, hoof quality, or the degree of hypotrichosis unless those traits are explicitly added.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

## Breed JSON

> **Schema and locus note:** The provided Procedural Horse Genetics wiki URL was not retrievable, so the exact field names below are implementation-oriented. The breed requires **two non-coat-color loci** not named in the original coat list: `KRT25_CURLY` and `SP6_CURLY`. If PHG currently supports only pigment, dilution, white-pattern, magical, and disorder loci, add a `coat_structure` or equivalent locus group rather than misusing a color gene or treating curly hair as magical.
>
> **Founder policy:** Every Bashkir Curly founder below receives at least one curly-associated allele through weighted strains. The 7:3 split favors a full-coated SP6 curly phenotype; the 3:10 KRT25-including portion preserves real genetic diversity and makes hypotrichosis a meaningful, uncommon breeding-management trait. The exact KRT25-versus-SP6 population frequencies are not established by a representative public breed-wide survey, so these are transparent gameplay weights, not measured allele frequencies.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

```json
{
  "id": "bashkir_curly",
  "name": "American Bashkir Curly",
  "type": "natural",
  "notes": "American Bashkir Curlies are recognized primarily by their curly coat, curly mane and tail, seasonal shedding pattern, and sometimes curly inner-ear hair. Procedural Horse Genetics does not model curl tightness, seasonal winter-coat growth and shedding, mane or tail curl, ear-hair curl, coat texture, skin sensitivity, hypoallergenic response in individual riders, hoof quality, body conformation variation, or the full severity range of KRT25-associated hypotrichosis.",

  "biomes": [
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:taiga",
    "minecraft:snowy_taiga",
    "minecraft:grove",
    "minecraft:plains"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 780,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.62,
      "e": 0.38
    },
    "agouti": {
      "A": 0.62,
      "a": 0.38
    },

    "cream": {
      "N": 0.87,
      "Cr": 0.13
    },
    "dun": {
      "N": 0.90,
      "D": 0.10
    },
    "grey": {
      "N": 0.90,
      "G": 0.10
    },
    "roan": {
      "N": 0.90,
      "Rn": 0.10
    },
    "silver": {
      "N": 0.95,
      "Z": 0.05
    },
    "champagne": {
      "N": 0.98,
      "Ch": 0.02
    },
    "flaxen": {
      "N": 0.78,
      "f": 0.22
    },

    "tobiano": {
      "N": 0.88,
      "To": 0.12
    },
    "sabino_1": {
      "N": 0.88,
      "SB1": 0.12
    },
    "frame_overo": {
      "N": 0.97,
      "O": 0.03
    },
    "splash_white_1": {
      "N": 0.94,
      "SW1": 0.06
    },
    "kit_white_spotting": {
      "N": 0.94,
      "W": 0.06
    },
    "rabicano": {
      "N": 0.91,
      "Rb": 0.09
    },

    "leopard_complex": {
      "N": 0.90,
      "LP": 0.10
    },
    "patn1": {
      "N": 0.88,
      "PATN1": 0.12
    },
    "patn2": {
      "N": 0.94,
      "PATN2": 0.06
    },

    "pearl": {
      "N": 1.0
    },
    "mushroom": {
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

  "coat_structure_genes": {
    "KRT25_CURLY": {
      "n": 0.94,
      "Crd": 0.06
    },
    "SP6_CURLY": {
      "n": 0.86,
      "Csp6": 0.14
    }
  },

  "disorder_genes": {
    "TOE1_CA": {
      "N": 0.972,
      "ca": 0.028
    },
    "SCN4A_HYPP": {
      "N": 0.995,
      "hypp": 0.005
    },
    "GYS1_PSSM1": {
      "N": 0.985,
      "pssm1": 0.015
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
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 6,
    "jump": 6,
    "health": 9,
    "size": [
      0.93,
      1.04
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "sp6_full_coated_curly",
      "name": "Full-Coated Curly",
      "weight": 7,
      "notes": "Default Curly phenotype. This strain uses the SP6 curly variant without KRT25, producing a curly coat without KRT25-associated hypotrichosis.",
      "coat_structure_genes": {
        "KRT25_CURLY": {
          "n": 1.0
        },
        "SP6_CURLY": {
          "n": 0.30,
          "Csp6": 0.70
        }
      }
    },
    {
      "id": "krt25_hypotrichotic_curly",
      "name": "Hypotrichotic Curly",
      "weight": 3,
      "notes": "Less common KRT25-associated Curly strain. It produces curly hair with variable hair reduction; KRT25 is epistatic to SP6 for the hypotrichosis expression.",
      "coat_structure_genes": {
        "KRT25_CURLY": {
          "n": 0.15,
          "Crd": 0.85
        },
        "SP6_CURLY": {
          "n": 0.80,
          "Csp6": 0.20
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Bashkir Curly × Bashkir Curly produces Bashkir Curly. Bashkir Curly × another pure breed produces a cross. Curly expression itself follows its own loci and can pass into crosses; a visibly curly cross must not automatically receive the Bashkir Curly breed label. Cross × pure remains a cross, different crosses become Mixed, and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Curly genetics

The important implementation is the interaction between the two genes, not simply “curly versus straight.”

| `KRT25_CURLY` | `SP6_CURLY` | Expected phenotype | Mod implementation |
|---|---|---|---|
| `n/n` | `n/n` | Straight coat with ordinary hair density | Not valid for a pure Curly founder, but possible in crosses if neither curly allele is inherited. |
| `n/n` | At least one `Csp6` | Curly coat without KRT25-associated hypotrichosis | The intended default full-coated Curly phenotype. |
| At least one `Crd` | `n/n` | Curly coat with variable hypotrichosis | Use a curly phenotype plus a reduced-hair visual/health flag if the renderer supports it. |
| At least one `Crd` | At least one `Csp6` | Curly coat with KRT25-associated hypotrichosis | KRT25 masks SP6’s full-haired distinction for the hair-loss trait. |

This model reflects research showing that KRT25 and SP6 variants are each associated with dominant curly-coat expression, while KRT25 is associated with hypotrichosis and exerts an epistatic effect over SP6. Registry breeding records support an autosomal-dominant curly inheritance pattern in the American Bashkir Curly population.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

## Genetics rationale

| Feature | Proposed treatment | Why |
|---|---:|---|
| Curly coat | Two-locus `KRT25_CURLY` + `SP6_CURLY` system | This is the central breed trait and is supported by identified variants rather than folklore.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/) |
| Full-haired curly strain | Weight 7 | Makes the plush, visibly curly phenotype the common player experience. The weight is a gameplay choice, not a surveyed breed frequency. |
| KRT25/hypotrichotic strain | Weight 3 | Keeps a real and genetically meaningful curly subtype present without making reduced hair the norm.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/) |
| Broad pigment pool | Bay, chestnut, black, gray, dilutions, roan, pinto, and leopard all enabled | The breed is documented in all colors, including Appaloosa and pinto patterns; chestnut is often described as common.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Leopard complex | `LP` 0.10; `PATN1` 0.12 | Appaloosa-patterned Curlies are real enough to merit a low-to-moderate founder rate, but should remain a minority.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Tobiano/sabino | 0.12 each | Supports a visibly varied Curly population without allowing patterned horses to crowd out solid founders.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Pearl, mushroom, brindle | Forced wild type | No clear evidence makes these needed for the recognizable Bashkir Curly population; players can introduce them through crossing. |
| Speed | 6/10 | A versatile saddle horse, not a specialist racing type.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Jump | 6/10 | Competent athletic generalist, but not principally selected as a show-jumping breed.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Health | 9/10 | Represents practical hardiness, ruggedness, and ability to work in variable conditions—not a claim that every genetic disorder is absent.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |
| Size | ×0.93–1.04 | Fits saddle-type Curly heights around 14.1–15.1 hands and an overall average near 15 hands.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse) |

## Disorder policy

A breed-profile source reports that **2.8% of Bashkir Curlies carried the cerebellar abiotrophy mutation**, so the JSON uses `ca: 0.028`. Cerebellar abiotrophy is recessive; a carrier should remain outwardly normal but can pass the allele to offspring.  [madbarn](https://madbarn.com/american-bashkir-curly-horse-breed-profile/)

The same source flags lineage-dependent CA, HYPP, and PSSM risk. No representative public Bashkir Curly-wide allele-frequency figures were located for HYPP or PSSM1, so the `0.005` HYPP and `0.015` PSSM1 values are intentionally low gameplay approximations rather than reported population estimates. HYPP is dominantly inherited and should be handled by the mod’s actual `SCN4A` behavior, not tested with a recessive 25/50/25 carrier-cross expectation.  [madbarn](https://madbarn.com/american-bashkir-curly-horse-breed-profile/)

If this project requires **only measured breed-specific rates**, retain CA at `0.028` and set HYPP/PSSM1 clear pending source-quality screening data:

```json
"SCN4A_HYPP": {
  "N": 1.0
},
"GYS1_PSSM1": {
  "N": 1.0
}
```

Hypotrichosis should not be stored under `disorder_genes` unless PHG treats it as a health condition. It is more accurately a consequence/phenotype modifier of the `KRT25_CURLY` coat-structure allele combination.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

## Code map

| Location | Required work |
|---|---|
| `common/breed/Breed` | Add the `bashkir_curly` natural-breed record, metadata, spawn data, notes, stat targets, coat pools, health pools, and strains. |
| `common/breed/Breeds` | Register the breed ID for data loading, saved-genome resolution, spawning, books, menus, and lineage display. |
| `common/breed/BreedSource` | Validate the four enabled sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Accept an empty epigenetic-band map. Curl is a locus-driven structural phenotype, not a color-shade epigenetic band. |
| `common/breed/spec/` | Add support for `coat_structure_genes` and strain-specific overrides if the format does not already provide it; ensure bidirectional serialization. |
| `common/breed/Commonness` | Verify `RARE` maps to the intended ladder weight. If commonness is stored numerically, retain `1.5`. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 6, health 9, and size ×0.93–1.04 into valid target bands. |
| `common/breed/BreedFounder` | Roll the base Curly genome, select one strain by 7:3 weight, apply the strain’s structure-locus pool, then assert that each pure founder inherits `Crd` or `Csp6`. |
| `common/breed/BreedLineage` | Keep default pure/cross/Mixed/Feral Mixed labels; curly phenotype inheritance must be separate from breed-label inheritance. |
| `common/genetics/SpliceOutcome` | Add `KRT25_CURLY` and `SP6_CURLY` to transmissible splice outcomes if splice carrots may modify coat-structure alleles. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Verify speed, jump, health, and size targets use valid curve and range types. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` band behavior is needed. |
| Genome/phenotype renderer | **Required:** derive curly/straight and hypotrichosis state from KRT25 × SP6 genotype, then render coat, mane, tail, and optionally hair-density differences. |

## Verification

1. **Data registration:** Confirm **American Bashkir Curly** appears in the H-menu’s Breeds tab and the breed book, showing natural status, rare commonness, sources, winter-capable biome choices, and curly-coat flavour text.

2. **Pack identity:** Spawn a wild pack in a valid biome. Every pack member must display the **American Bashkir Curly** breed label, though pigment colors and markings should vary widely.

3. **Curly founder guarantee:** Generate at least 100 pure founders. Every one must carry at least one `Crd` or `Csp6` allele and should render curly. A straight-coated pure founder indicates the strain override or founder assertion failed.

4. **Strain frequency:** In a large founder sample, approximately 70% should use the full-coated SP6-style strain and roughly 30% the KRT25-including hypotrichotic strain. Measure selected strain metadata, not merely visible phenotype, because both strains can be curly.

5. **Epistasis check:** Create each of the four genotype categories in the curly-genetics table. Verify that `Csp6` alone gives curly/full coat; `Crd` yields curly with hypotrichosis; and a horse carrying both remains hypotrichotic under the KRT25-over-SP6 rule.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5913262/)

6. **Coat diversity:** Generate 200 founders. Confirm the breed supports solid colors, gray, cream dilution, dun, roan, pinto loci, and leopard complex/PATN combinations. Chestnut should be readily visible, but no particular pigment color should define every Curly.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse)

7. **Disease loci:** Verify that only CA, HYPP, and PSSM1 may roll non-clear in the supplied version, and that all unnamed disorders are forced clear. For CA, carrier × carrier matings should yield approximately 25% affected, 50% carrier, and 25% clear foals across a large test group. Treat HYPP with its normal dominant inheritance model.

8. **Lineage:** Verify:
   - Bashkir Curly × Bashkir Curly → Bashkir Curly.
   - Bashkir Curly × another pure breed → named cross.
   - Curly cross × pure Bashkir Curly → cross.
   - Straight-coated offspring from a Curly cross remains a cross, not Feral Mixed.
   - Curly-coated offspring of a non-Curly cross remains a cross, not Bashkir Curly.
   - Any lineage × Feral Mixed → Mixed.

9. **Feral distinction:** Confirm an unassigned lone wild horse reads **Feral Mixed**, including if a mutation or genetic item later gives it a curly coat. Appearance alone must never overwrite lineage.

## Sources

- [American Bashkir Curly Horse Registry](https://abcregistry.org/): official registry identity, 1971 founding, and preservation mission.  [abcregistry](https://abcregistry.org/)
- [FEI — Breed Profile: The American Bashkir Curly Horse](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse): registry history, average height, conformation, broad color/pattern range, and modern international association context.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-american-bashkir-curly-horse)
- [Scientific Reports — *An epistatic effect of KRT25 on SP6 is involved in curly coat in horses*](https://www.nature.com/articles/s41598-018-24865-3): KRT25 and SP6 variants, dominant curly expression, hypotrichosis, and KRT25-over-SP6 epistasis.  [nature](https://www.nature.com/articles/s41598-018-24865-3)
- [OMIA — KRT25-related Curly coat](https://omia.org/OMIA003039/9796/): causal KRT25 variant, autosomal-dominant inheritance, and breed association.  [omia](https://omia.org/OMIA003039/9796/)
- [OMIA — SP6-related Curly coat](https://omia.org/OMIA002175/9796/): SP6 variant and the two-locus curly/hypotrichosis interpretation.  [omia](https://omia.org/OMIA002175/9796/)
- [Horse Illustrated — Curly Horse](https://www.horseillustrated.com/horse-breeds-horse-breed-articles-curly-horse/): Nevada Damele foundation account and saddle-type height range.  [horseillustrated](https://www.horseillustrated.com/horse-breeds-horse-breed-articles-curly-horse/)
- [MadBarn — American Bashkir Curly Horse Breed Profile](https://madbarn.com/american-bashkir-curly-horse-breed-profile/): color range, height, genetic-trait summary, and reported 2.8% CA carrier figure.  [madbarn](https://madbarn.com/american-bashkir-curly-horse-breed-profile/)
