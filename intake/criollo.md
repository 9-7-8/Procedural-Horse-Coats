The **Criollo**—more specifically the **Argentine Criollo** for a single mod breed definition—should be a compact, exceptionally hardy South American cattle and distance horse with an unmistakably high rate of dun and primitive markings. It is a broad-color breed, but its core visual message should be “tough gaucho horse”: medium-small, muscular, long-maned, sure-footed, and often line-backed or grullo rather than a generic western pony.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)

## Identity & flavour

The **Criollo** is the foundational horse type of southern South America, especially Argentina, Uruguay, Brazil, and Chile. “Criollo” broadly means a locally developed New World animal of Iberian ancestry, so several regional Criollo populations exist; this definition represents the **Argentine Criollo** or Río de la Plata-style Criollo rather than treating every horse called Criollo across the Americas as genetically interchangeable. Its ancestors descended from Spanish horses brought into the Río de la Plata region during the colonial period, including a shipment of around 100 horses recorded in 1535.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)

Over centuries, horses escaped, multiplied, and adapted to the Pampas, producing a feral-derived population shaped by heat, drought, sparse forage, long travel, and livestock work. Criollos became inseparable from the *gaucho* culture of Argentina, Uruguay, and neighboring regions: they carried riders across open grassland, worked cattle, performed ranch tasks, and survived on conditions that would defeat a more delicate imported riding horse. In the early twentieth century, extensive European and North American crossbreeding threatened the traditional type, leading Argentine breeders to organize preservation efforts; the Argentine Rural Society accepted the breed into its studbook in 1918, and Dr. Emilio Solanet later led a restoration program and formalized a type-focused standard.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)

A Criollo is compact, strongly built, and generally around 14.3 hands, with a typical range roughly 13.3–15.1 hands. It should have a broad, short head with a straight or gently convex profile, small pointed ears, a thick but well-shaped neck, prominent withers, a short strong back, deep barrel, broad chest, rounded muscular croup, and strong legs with dense bone. The mane and tail are generally full. Its hooves should look unusually practical and durable, built for long miles on rough country—not large draft hooves, but tough, balanced ones.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)

The breed accepts a broad palette of colors, but **dun is the iconic Criollo color**, commonly paired with a dark dorsal stripe and zebra barring on the legs. Grullo, bay dun, red dun, brown, black, bay, chestnut, buckskin, palomino, roan, gray, and selected overo-type patterns occur. The exact acceptance of pinto/tobiano varies among regional standards: a shared southern-Criollo description accepts many colors but excludes “paint” and tobiano, while general Argentine summaries often list pinto or overo among the population. For a single gameplay breed, the pure founder pool should strongly favor dun and primitive markings, allow a broad range of solid and limited overo/sabino expression, and keep tobiano absent.  [en.wikipedia](https://en.wikipedia.org/wiki/Criollo_horse)

Criollos are celebrated for endurance, thrift, courage, soundness, calm intelligence, resistance to weather, and ability to work all day. They are not designed to be the fastest racehorse or tallest jumper; their advantage is that they keep going, stay useful, and cope with poor conditions. In Procedural Horse Genetics, players should breed Criollos for extraordinary heartiness, compact size, good practical speed, and usable agility. The mod cannot simulate real endurance trials, heat and drought tolerance, metabolic thrift, grazing efficiency, cattle sense, gaucho training, full mane and tail, primitive marking placement, or registry differences among Argentine, Uruguayan, Brazilian, Chilean, and other Criollo populations.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)

## Breed JSON

> **Scope choice:** This file models the Argentine/Río de la Plata Criollo type, not every Latin American horse called “Criollo.” That keeps its size, gait, cultural identity, and coat pool coherent.
>
> **Schema note:** The Procedural Horse Genetics wiki URL supplied in the original request could not be fetched, so the exact JSON field names, commonness enum, allele spelling, and strain syntax must be reconciled against `common/breed/spec/` before implementation. The file below follows the same implementation-oriented convention as the preceding breed definitions.

```json
{
  "id": "criollo",
  "name": "Criollo",
  "type": "natural",
  "notes": "This definition represents the Argentine or Río de la Plata Criollo, not every regional breed called Criollo. Criollos are defined by endurance, heat and drought tolerance, forage thrift, sound feet, cattle-work ability, gaucho horsemanship, a compact mesomorphic body, full mane and tail, and primitive dorsal and leg markings. Procedural Horse Genetics does not model endurance conditioning, grazing efficiency, desert or Pampas adaptation, hoof density, cow sense, regional registry differences, gaited Colombian or Peruvian Criollo types, mane fullness, or exact primitive-marking placement.",

  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "minecraft:windswept_hills"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 540,
  "commonness": "MODERATE",

  "coat_genes": {
    "extension": {
      "E": 0.67,
      "e": 0.33
    },
    "agouti": {
      "A": 0.67,
      "a": 0.33
    },

    "dun": {
      "N": 0.52,
      "D": 0.48
    },
    "cream": {
      "N": 0.88,
      "Cr": 0.12
    },
    "grey": {
      "N": 0.90,
      "G": 0.10
    },
    "roan": {
      "N": 0.88,
      "Rn": 0.12
    },
    "flaxen": {
      "N": 0.76,
      "f": 0.24
    },
    "silver": {
      "N": 0.98,
      "Z": 0.02
    },
    "champagne": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },

    "sabino_1": {
      "N": 0.91,
      "SB1": 0.09
    },
    "frame_overo": {
      "N": 0.97,
      "O": 0.03
    },
    "splash_white_1": {
      "N": 0.98,
      "SW1": 0.02
    },
    "kit_white_spotting": {
      "N": 0.97,
      "W": 0.03
    },
    "rabicano": {
      "N": 0.90,
      "Rb": 0.10
    },

    "tobiano": {
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
    "speed": 6,
    "jump": 6,
    "health": 10,
    "size": [
      0.92,
      1.03
    ]
  },

  "epigenetic_bands": {
    "dun_expression": {
      "min": 0.70,
      "max": 1.00
    }
  },

  "strains": [
    {
      "id": "traditional_dun_criollo",
      "name": "Traditional Dun Criollo",
      "weight": 7,
      "notes": "The dominant Argentine-style strain. It strongly favors dun-derived coats—bay dun, red dun, and grullo—with clear primitive character.",
      "coat_genes": {
        "dun": {
          "N": 0.27,
          "D": 0.73
        },
        "cream": {
          "N": 0.91,
          "Cr": 0.09
        },
        "tobiano": {
          "N": 1.0
        }
      }
    },
    {
      "id": "broad_colour_criollo",
      "name": "Broad-Colour Criollo",
      "weight": 3,
      "notes": "A lower-frequency general Criollo strain representing the documented broad solid, roan, gray, cream-dilute, and modest overo/sabino palette without making tobiano a pure-breed founder trait.",
      "coat_genes": {
        "dun": {
          "N": 0.72,
          "D": 0.28
        },
        "cream": {
          "N": 0.80,
          "Cr": 0.20
        },
        "grey": {
          "N": 0.86,
          "G": 0.14
        },
        "roan": {
          "N": 0.84,
          "Rn": 0.16
        },
        "tobiano": {
          "N": 1.0
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Criollo × Criollo produces Criollo. Criollo × another pure breed produces a cross. Do not automatically label any South American horse a Criollo merely because it is dun, compact, hardy, or descended from Spanish-type horses; visible traits and regional origin are not a substitute for breed identity. Cross × pure remains a cross, different crosses become Mixed, and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Reason |
|---|---:|---|
| Regional scope | Argentine/Río de la Plata Criollo only | “Criollo” covers several regional populations with different sizes, uses, and gaits. A single shared founder pool would erase meaningful distinctions.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/) |
| Dun | `D` 0.48 base; 0.73 in traditional strain | Dun, commonly with dorsal stripe and zebra leg barring, is the most iconic and frequently cited Criollo coat color.  [en.wikipedia](https://en.wikipedia.org/wiki/Criollo_horse) |
| Dun-expression band | 0.70–1.00 | Optional visual tuning to make a dun founder look strongly primitive rather than merely carrying a minimally visible dun allele. This should apply only to an existing compatible PHG expression parameter. |
| Base colors | Broad `E/e` and `A/a` pool | Allows bay dun, grullo, red dun, bay, brown, black, chestnut, and other documented colors.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/) |
| Cream | `Cr` 0.12 base | Allows buckskin, palomino, smoky black, cremello, and perlino-like outcomes, which occur in broad Criollo color descriptions.  [en.wikipedia](https://en.wikipedia.org/wiki/Criollo_horse) |
| Gray and roan | Low-to-moderate frequencies | Both gray and blue/strawberry roan are documented in population summaries.  [en.wikipedia](https://en.wikipedia.org/wiki/Criollo_horse) |
| Sabino / frame / W | Low frequencies | Permits the reported limited overo-style or conspicuous white expression without making the breed pinto-dominated.  [en.wikipedia](https://en.wikipedia.org/wiki/Criollo_horse) |
| Tobiano | Forced wild type | The FICCC-style description cited excludes “paint” and tobiano; omitting tobiano preserves the core Argentine-style breed identity despite conflicting broad summaries that use “pinto” loosely.  [criollo-horse](https://www.criollo-horse.com/en/the-criollo-horse-what-does-it-look-like.html) |
| Leopard complex | Forced wild type | No source found establishing Appaloosa-type leopard spotting as a characteristic Criollo founder trait. |
| Disorders | All listed loci clear | No reliable, breed-specific prevalence data was found for the supplied simple Mendelian disease panel. It is more accurate to leave them clear than import rates from unrelated Quarter Horse, Arabian, or warmblood populations. |
| Speed | 6/10 | Useful working speed and long-distance travel ability, not a specialist sprint-horse profile.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/) |
| Jump | 6/10 | Athletic and agile enough for ranch work, trail obstacles, and general sport, but not selectively specialized for high-level jumping. |
| Health | 10/10 | The defining gameplay trait: extreme resilience, soundness, stamina, and survival under sparse forage and difficult weather. This is a stat tendency, not a claim that real individuals cannot become ill.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/) |
| Size | ×0.92–1.03 | Anchors founders around approximately 13.3–15.1 hands, near the commonly cited 14.3-hand average.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/) |

## Implementation cautions

The `dun_expression` block is **optional**. Keep it only if `BreedBands` already has a phenotype-expression target that controls primitive-mark visibility or dilution intensity. If the mod has no such parameter, remove the block entirely; do not create a generic band that accidentally affects unrelated traits.

The file intentionally excludes PSSM1 despite broad veterinary sources discussing PSSM1 across multiple breeds. No reliable Criollo-specific frequency appeared in the available search results, and PSSM1 is dominant, so inventing a carrier rate would have an outsized gameplay effect. If a future Argentine Criollo registry survey or peer-reviewed population study supplies a rate, add the `GYS1` locus then. PSSM1 is autosomal dominant and can be passed by an affected heterozygote to about half of offspring.  [uu](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts)

Likewise, the `frame_overo` locus requires special attention. The prompt labels “frame overo” as `EDNRB`, while its disorder list includes `MET` as lethal at conception; standard equine genetics normally treats frame overo and overo lethal white syndrome as alleles at the same `EDNRB` locus, with lethal homozygosity after birth rather than “at conception.” Use PHG’s existing locus naming and lethality behavior rather than retaining both as unrelated genes. This matters whenever frame is enabled, even at 3%.  [thehorse](https://thehorse.com/16446/breeding-horses-with-genetic-disorders/)

## Code map

| Location | Required work |
|---|---|
| `common/breed/Breed` | Add the natural `criollo` record, using Argentine/Río de la Plata scope, flavor text, spawn data, coat pools, strains, stats, and notes. |
| `common/breed/Breeds` | Register `criollo` for spawning, books, menus, saved-genome deserialization, and lineage labels. |
| `common/breed/BreedSource` | Confirm the four source entries—`wild`, `cowboy`, `spawn_egg`, and `stable`—are valid. |
| `common/breed/BreedBands` | Validate the optional `dun_expression` range only if an actual corresponding phenotype parameter exists. Otherwise leave the band map empty. |
| `common/breed/spec/` | Ensure parser and writer support strain weights, per-strain gene overrides, stat targets, and optional bands in both serialization directions. |
| `common/breed/Commonness` | Confirm `MODERATE` maps to weight 6, or use the mod’s exact rarity enum and ladder value. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 6, health 10, and ×0.92–1.03 size into valid `TargetBand` targets. |
| `common/breed/BreedFounder` | Roll base genes, select `traditional_dun_criollo` or `broad_colour_criollo` at 7:3, apply the selected overrides, and force unlisted loci to wild type/clear. |
| `common/breed/BreedLineage` | Apply standard pure/cross/Mixed/Feral Mixed lineage behavior; strains must not become breed labels. |
| `common/genetics/SpliceOutcome` | No special handling; ensure inserted `D` and other compatible alleles can pass normally to foals. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Verify speed, jump, health, and size axis targets. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` behavior is required unless the actual data spec demands it. |
| EDNRB / frame implementation | If frame is permitted, use the project’s real EDNRB locus and existing homozygous-lethal handling; do not duplicate it as unrelated `MET`. |

## Verification

1. **Registry and UI:** Start the game with the data file installed. Confirm **Criollo** appears in the H-menu’s Breeds tab and in the breed book with the Argentine/South American working-horse description, correct biome list, moderate commonness, sources, and compact-size tendency.

2. **Pack identity:** Spawn a valid wild pack in plains, savanna, badlands, or windswept-hills terrain. Every horse in the same pack should display the **Criollo** label, even if individuals differ as bay dun, red dun, grullo, bay, chestnut, gray, or roan.

3. **Feral distinction:** Generate or find a lone wild horse outside the breed-pack system. It should read **Feral Mixed**, not Criollo. A dun Feral Mixed horse must remain Feral Mixed: color must never overwrite lineage.

4. **Traditional strain:** Generate at least 200 founders. Approximately 70% should use `traditional_dun_criollo`, and those founders should show an obviously elevated frequency of bay dun, red dun, and grullo. If supported, dorsal stripe and leg barring should be visually clear.

5. **Broad-color strain:** About 30% should use `broad_colour_criollo`, with more plain bay, chestnut, black, gray, roan, cream-dilute, and limited overo/sabino outcomes while retaining the same Criollo breed label.

6. **Exclusions:** Across a large pure-founder sample, verify that tobiano, LP, PATN1, PATN2, pearl, mushroom, brindle, and magical alleles never arise. If tobiano appears, confirm that the `N: 1.0` founder enforcement was not overridden by generic feral gene generation.

7. **Disease panel:** Confirm every disorder listed in the supplied JSON is clear in every pure Criollo founder. Any non-clear disease result indicates a default disorder pool is leaking in from generic founder creation.

8. **Lineage table:** Verify:
   - Criollo × Criollo → Criollo.
   - Traditional Dun Criollo × Broad-Colour Criollo → Criollo.
   - Criollo × another pure breed → cross.
   - Criollo cross × pure Criollo → cross.
   - Different cross × different cross → Mixed.
   - Any lineage × Feral Mixed → Mixed.

9. **Stat feel:** Compare equal-age, equal-condition founders. Criollos should consistently trend smaller and much heartier than baseline, retain useful working speed and agility, and avoid routinely beating racehorses in speed or specialist jumpers in jumping.

## Sources

- [MadBarn — Criollo Horse Breed Profile](https://madbarn.com/criollo-horse-breed-profile/): colonial history, 1535 ancestry account, 1918 registry context, Solanet restoration, height, color palette, endurance, and work traits.  [madbarn](https://madbarn.com/criollo-horse-breed-profile/)
- [Horse Illustrated — Latin American Horse Breeds: Criollo](https://www.horseillustrated.com/horse-breeds-latin-american-horse-breeds-criollo/): early preservation movement, Argentine Rural Society studbook recognition, height range, conformation, colors, and South American cultural context.  [horseillustrated](https://www.horseillustrated.com/horse-breeds-latin-american-horse-breeds-criollo/)
- [Criollo Horse — Morphology and regional standards](https://www.criollo-horse.com/en/the-criollo-horse-what-does-it-look-like.html): FICCC-associated height and conformation targets, all-color policy, tobiano exclusion, and primitive dorsal/leg markings.  [criollo-horse](https://www.criollo-horse.com/en/the-criollo-horse-what-does-it-look-like.html)
- [Chilean Horse — Breed Standard](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82): FICCC height context and regional variation among related South American Criollo populations.  [chileanhorse](https://www.chileanhorse.com/index.php?option=com_content&task=view&id=34&Itemid=82)
- [Utrecht University — PSSM scientific summary](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts): dominant PSSM1 inheritance context; included to justify not guessing a breed frequency where no Criollo-specific value was found.  [uu](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts)