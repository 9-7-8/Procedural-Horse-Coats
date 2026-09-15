The **Azteca** should feel like a compact, powerful Mexican working-and-performance horse: Iberian elegance and collection joined to Quarter Horse cow-horse agility, with some Criollo/Mexican horse influence in the Mexican registry. Its most recognizable pure-population look is solid bay, brown, black, chestnut, or gray, while the wider American Azteca registry permits selected Paint-derived tobiano, overo, and sabino patterns.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses)

## Identity & flavour

The **Azteca** is Mexico’s national horse breed in spirit and one of the country’s modern purpose-bred riding horses. It was developed beginning in 1972 at Rancho San Antonio near Texcoco, Mexico, when breeders crossed Iberian horses—principally Andalusian/Pura Raza Española and Lusitano-type stock—with American Quarter Horses and, in Mexican programs, Mexican Criollo or *Criollo militar* mares. The first officially recognized Azteca foal, Casarejo, was born in 1972 from the Andalusian stallion Ocultado and the Quarter Horse mare Americana. Mexico’s Department of Agriculture formally recognized the breed in 1982.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/)

The breed was designed for the kind of horse Mexican riders needed: athletic enough for cattle work and ranch tasks, compact and quick enough for western performance, but elegant, collected, brave, and trainable enough for classical riding, charro traditions, *escuela* work, dressage, working equitation, and exhibition. The Azteca is a deliberate blend rather than a random cross: Mexican standards control the proportions of foundation blood, generally allowing 3/8–5/8 Andalusian or Quarter Horse influence, while Criollo contribution is limited. American registry rules have their own categories and may also admit qualified Paint Horse ancestry.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses)

Aztecas are medium-sized, usually around 14.1–16 hands, with Mexican standards tending toward roughly 14.1–15.3 hands and American registry horses sometimes reaching 16 hands. They should appear harmonious, compact, and strongly muscled rather than tall and narrow: a refined, expressive head with a straight or slightly convex profile; a substantial arched neck; pronounced withers; long sloping shoulder; broad, deep chest; short, strong back; rounded, powerful hindquarters; straight limbs; and strong hooves. The mane and tail should look full and handsome but not genetically extreme, and legs should be clean with little or no feather.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/)

The Mexican Azteca is traditionally a **solid-color** horse. All solid colors are permitted, and gray is frequently seen; bay, dark bay/brown, black, and chestnut belong naturally in a founder herd. White facial and lower-leg markings are allowed, but Mexican rules do not accept Appaloosa patterning, albino/white classifications, or excessive white. The American Azteca registry is broader: eligible horses may carry Paint-derived tobiano, overo, and sabino, provided their pedigree and pattern fall within the registry’s accepted foundation rules. In mod terms, a standard Azteca pack should mostly look solid, polished, and dark, while an uncommon American-registry strain can yield a visibly patterned individual.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses)

Aztecas are valued for intelligence, courage, willing temperament, agility, collection, athletic versatility, and a balance between responsive sensitivity and practical tractability. In this mod, they should reward players breeding for a sturdy, compact warmblood-type athlete: high jump and agility, good speed, excellent general usefulness, a solid health score, and an unmistakably powerful neck-and-hindquarter silhouette. The mod cannot reproduce breed inspection, foundation-blood percentages, charro training, cow sense, working equitation skill, piaffe/passage collection, head profile, mane fullness, or the subtle difference between Mexican and American registry eligibility.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/)

## Breed JSON

> **Schema note:** The Procedural Horse Genetics wiki page supplied in the original request could not be fetched, so field names and allele IDs below remain implementation-oriented placeholders. Reconcile them with the actual data schema in `common/breed/spec/` and the mod’s registered locus IDs before adding the file.
>
> **Registry choice:** This definition treats the primary breed as a Mexican-style solid Azteca population and uses a low-weight `american_patterned` strain for the wider U.S. registry’s permitted Paint-derived patterns. That prevents every Azteca herd from becoming pinto while still representing the real registry distinction.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses)

```json
{
  "id": "azteca",
  "name": "Azteca",
  "type": "natural",
  "notes": "The Azteca is a pedigree-defined Mexican breed shaped by Andalusian or Lusitano, Quarter Horse, and in Mexican programs Criollo ancestry. The mod does not model foundation-blood percentages, Mexican registration inspection, American versus Mexican registry eligibility, charro horsemanship, cattle sense, working-equitation training, dressage collection, the arched neck and profile required by conformation standards, or mane and tail fullness.",

  "biomes": [
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:plains",
    "minecraft:windswept_hills",
    "minecraft:meadow"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 720,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.73,
      "e": 0.27
    },
    "agouti": {
      "A": 0.74,
      "a": 0.26
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "sabino_1": {
      "N": 0.96,
      "SB1": 0.04
    },
    "rabicano": {
      "N": 0.975,
      "Rb": 0.025
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
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_1": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 1.0
    },
    "roan": {
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
    "GBE1_GBED": {
      "N": 0.99,
      "gbed": 0.01
    },
    "GYS1_PSSM1": {
      "N": 0.98,
      "pssm1": 0.02
    },
    "PPIB_HERDA": {
      "N": 0.995,
      "herda": 0.005
    },
    "SCN4A_HYPP": {
      "N": 0.998,
      "hypp": 0.002
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
    "CVM": {
      "N": 1.0
    },
    "megaesophagus": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 7,
    "jump": 8,
    "health": 8,
    "size": [
      0.96,
      1.08
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "mexican_solid",
      "name": "Mexican Solid",
      "weight": 9,
      "notes": "Default Mexican-style Azteca strain. It retains the base breed pool, with solid coats and ordinary white facial or leg markings only.",
      "coat_genes": {
        "tobiano": {
          "N": 1.0
        },
        "frame_overo": {
          "N": 1.0
        },
        "splash_white_1": {
          "N": 1.0
        },
        "kit_white_spotting": {
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
        }
      }
    },
    {
      "id": "american_patterned",
      "name": "American Patterned",
      "weight": 1,
      "notes": "Rare American-registry-inspired strain, representing permitted Paint-derived pattern ancestry. Patterned founders remain uncommon and should never carry leopard-complex traits.",
      "coat_genes": {
        "tobiano": {
          "N": 0.65,
          "To": 0.35
        },
        "sabino_1": {
          "N": 0.78,
          "SB1": 0.22
        },
        "frame_overo": {
          "N": 0.88,
          "O": 0.12
        },
        "splash_white_1": {
          "N": 0.93,
          "SW1": 0.07
        },
        "kit_white_spotting": {
          "N": 0.95,
          "W": 0.05
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
    "notes": "Use the default lineage table. Azteca × Azteca produces Azteca. Azteca × another pure breed produces a cross. Do not automatically label a Quarter Horse × Andalusian, Lusitano, or Criollo foal Azteca: real Azteca registration depends on documented proportions, pedigree, and conformation approval. Under normal mod rules, such offspring should remain a cross unless generated directly as an Azteca founder."
  }
}
```

## Genetics rationale

| Feature | Proposed rule | Reason |
|---|---:|---|
| Base color pool | `E` 0.73 / `e` 0.27; `A` 0.74 / `a` 0.26 | Creates a breed centered on bay and dark bay/brown while retaining black and chestnut founders. Dark brown/bay is often described as frequent, and all solid colors are permissible.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses) |
| Gray | `G` 0.18 | Gray is repeatedly noted as common or especially frequent in Aztecas, reflecting Iberian ancestry. This is a gameplay estimate, not a published Azteca allele survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Azteca_horse) |
| Sabino and rabicano | Low rates | Allows modest white markings and subtle white expression without turning ordinary Mexican-registry founders into pinto horses. Facial and lower-leg markings are permitted.  [en.wikipedia](https://en.wikipedia.org/wiki/Azteca_horse) |
| Cream, champagne, silver, dun, pearl | Forced wild type | The Mexican breed’s intended founder identity is solid and Iberian/Quarter Horse based, but these dilutions are not necessary to represent its recognizable look. This also helps distinguish it from broad “all colors” stock-horse breeds.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses) |
| Mexican solid strain | Weight 9 | Most generated Aztecas should match the traditional Mexican standard: solid-colored and elegant.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses) |
| American patterned strain | Weight 1 | Represents the American registry’s acceptance of qualifying Paint-derived tobiano, overo, and sabino without making pinto Aztecas routine.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/) |
| Leopard complex | Forced wild type | Appaloosa patterns are explicitly not accepted in the Mexican standard and are inappropriate for an Azteca founder pool.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses) |
| Speed | 7/10 | Good short-distance athleticism and responsiveness for cattle work and performance, without the profile of a dedicated racehorse.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/) |
| Jump | 8/10 | Supports sport versatility, collected athleticism, and the breed’s suitability for jumping, dressage, and working equitation.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/) |
| Health | 8/10 | A strong practical-riding score, balanced against inherited-disease considerations from Quarter Horse-derived lines.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/) |
| Size | ×0.96–1.08 | Represents a compact-to-medium, powerfully muscled horse in the documented 14.1–16-hand range.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses) |

## Disorder policy

The American Azteca Horse registry requires breeding and registration stock to test `N/N` for **HYPP, PSSM, MH, GBED, and HERDA**. That policy is strong evidence that the registry recognizes Quarter Horse-derived genetic-risk loci as relevant to Azteca breeding decisions; it is **not** evidence that the listed disease rates are measured breed-wide Azteca carrier frequencies.  [americanazteca](https://www.americanazteca.com/registration.html)

The prompt’s available disease list lacks malignant hyperthermia (`RYR1` / MH), despite the Azteca registry naming it. If the mod supports MH elsewhere, add it as a fifth low-frequency Quarter Horse-derived locus; if it does not, record that omission in the breed notes or development issue tracker rather than relabeling another disorder.

The values below are deliberately low gameplay approximations, designed to make testing meaningful without implying an unscreened Azteca population has Quarter Horse-level disease prevalence:

| Locus | File rate | Basis and implementation warning |
|---|---:|---|
| `GBE1_GBED` | 0.01 mutant allele | Quarter Horse-derived risk; registry explicitly requires clear results for breeding stock.  [americanazteca](https://www.americanazteca.com/registration.html) |
| `GYS1_PSSM1` | 0.02 mutant allele | Quarter Horse-derived risk; registry screens PSSM, and AQHA reports PSSM affects approximately 11% of Quarter Horses. Do not treat that Quarter Horse figure as an Azteca rate.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |
| `PPIB_HERDA` | 0.005 mutant allele | Quarter Horse-derived risk; registry requires clear HERDA status. AQHA cites approximately 3.5% carrier frequency in Quarter Horses, concentrated in cutting/cow-horse disciplines.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |
| `SCN4A_HYPP` | 0.002 mutant allele | Very low founder value because registry breeding-stock rules require N/N status and HYPP has a dominant inheritance pattern. AQHA reports approximately 4.4% of Quarter Horses as carriers, especially among halter lines.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |

For a **strict registry-screened Azteca** configuration, set all four enabled disease loci to `N: 1.0`; that arguably better models modern breeding stock accepted by the American Azteca registry.  [americanazteca](https://www.americanazteca.com/registration.html)

## Code map

| Location | Required work |
|---|---|
| `common/breed/Breed` | Add the natural `azteca` breed record, metadata, flavor note, biomes, source checklist, commonness, price, gene pools, strain definitions, and stat targets. |
| `common/breed/Breeds` | Register `azteca` so data loading, saved genomes, menus, books, spawning, and lineage labels resolve correctly. |
| `common/breed/BreedSource` | Validate all four enabled sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Permit an empty epigenetic band map. Color variation should be genetic and strain-based, not locked to a shade band. |
| `common/breed/spec/` | Implement/validate the JSON serializer and parser for base pools, strain overrides, weights, source enum values, and the `type` property in both directions. |
| `common/breed/Commonness` | Ensure `UNCOMMON` maps to the project’s intended rarity rung. If numerical values are stored directly, retain weight `3`. |
| `common/breed/BreedStatCurve` | Translate speed 7, jump 8, health 8, and size ×0.96–1.08 into valid `TargetBand` instances. |
| `common/breed/BreedFounder` | Roll the base Azteca genome, then select one strain by its 9:1 weight and apply only that strain’s override pool. Ensure unlisted loci are wild type/clear. |
| `common/breed/BreedLineage` | Retain default pure/cross/Mixed/Feral Mixed behavior; do not use parent breed names alone to auto-register a crossbred foal as Azteca. |
| `common/genetics/SpliceOutcome` | No Azteca-specific exception; verify allele insertion/transmission behavior operates normally. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate the speed, jump, health, and size axes and target ranges. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` band behavior is necessary unless required by the current data spec. |
| Disease registry, if supported | Add `RYR1_MH` / malignant hyperthermia if the broader mod has not implemented it; the American Azteca registry explicitly screens for it.  [americanazteca](https://www.americanazteca.com/registration.html) |

## Verification

1. **Registry and UI:** Load the mod and confirm that **Azteca** appears in the H-menu’s Breeds tab and the breed book with its name, Mexican working-and-performance flavour, natural classification, `UNCOMMON` commonness, price, sources, and suitable warm-temperate biome list.

2. **Pack identity:** Spawn or locate Azteca packs in valid biomes. Every horse in a single pack should identify as **Azteca**, even where founders differ as bay, chestnut, black, or gray.

3. **Feral identity:** Check a lone ordinary unassigned wild horse. It must read **Feral Mixed**, not Azteca.

4. **Standard coat distribution:** Generate 100–200 founders. Roughly nine tenths should use the Mexican solid strain. Expect bay/dark bay/brown to be common, chestnut and black to occur, and gray to be noticeable. Ordinary white markings may occur, but the default population must not produce palomino, buckskin, champagne, silver dapple, dun, pearl, roan, leopard spots, brindle, magical coats, or loud pinto patterns.

5. **American patterned strain:** In a large sample, about one tenth of founders should use the American-patterned strain. That strain may produce tobiano, overo/frame, sabino, splash, or KIT-patterned horses according to the mod’s actual locus model. It must still never produce leopard complex or PATN patterning.

6. **Frame safety:** Select known `O/n` frame carriers from the patterned strain and breed them. Confirm the mod’s lethal-white mechanism functions only when two frame alleles are inherited, according to the mod’s established `MET`/EDNRB implementation. If the source code calls the locus `MET`, replace `frame_overo` above with that exact key.  [thehorse](https://thehorse.com/138486/why-do-equine-genetic-disorders-and-coat-color-seem-related/)

7. **Disease rolls:** Verify only GBED, PSSM1, HERDA, and HYPP can occur in the gameplay-conservative file. All other listed disorders must remain clear. Test HYPP according to its dominant inheritance behavior; do not validate it as a recessive carrier × carrier 25% disease outcome.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup)

8. **Lineage table:** Verify:
   - Azteca × Azteca → Azteca.
   - Azteca × Andalusian, Lusitano, Quarter Horse, or Criollo → cross under default mod logic.
   - Azteca cross × pure Azteca → cross.
   - Different crosses → Mixed.
   - Any lineage × Feral Mixed → Mixed.

9. **Stat feel:** Compare equivalent-age, equivalent-conditioning horses. Aztecas should trend toward high jump and solid speed/health in a compact-to-medium frame, without consistently outperforming dedicated Thoroughbreds in raw speed or specialist heavy sport breeds in every category.

## Sources

- [Oklahoma State University — Azteca Horses](https://breeds.okstate.edu/horses/azteca-horses): Mexican breed development beginning in 1972; 1982 official recognition; foundation-blood rules; height; solid-color standard; Appaloosa-pattern exclusion.  [breeds.okstate](https://breeds.okstate.edu/horses/azteca-horses)
- [American Azteca Horse International Association — Registration](https://www.americanazteca.com/registration.html): American registration framework, accepted foundation registries, and mandatory `N/N` screening for HYPP, PSSM, MH, GBED, and HERDA.  [americanazteca](https://www.americanazteca.com/registration.html)
- [American Azteca Horse International Association — Coat Color, Patterns and Markings](https://www.americanazteca.com/coat_colors_and_markings.html): American registry color terminology and recognition context.  [americanazteca](https://www.americanazteca.com/coat_colors_and_markings.html)
- [MadBarn — Azteca Horse Breed Profile](https://madbarn.com/azteca-horse-breed-profile/): Casarejo, Ocultado, Americana, historical overview, registry context, conformation, height, colors, and American patterned-coat distinction.  [madbarn](https://madbarn.com/azteca-horse-breed-profile/)
- [American Quarter Horse Association — Six-panel genetic testing](https://www.aqha.com/widget/-/genetic-test-roundup): Quarter Horse disease-context figures for HYPP, HERDA, and PSSM; used only to contextualize low proxy values, not as Azteca prevalence data.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup)
