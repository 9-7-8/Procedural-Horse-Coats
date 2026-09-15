The **Chincoteague Pony** should be one of the mod’s most distinctive natural breeds: a small, salt-marsh-adapted American landrace with strong legs and hooves, a shaggy seasonal coat, and a notably pinto-rich color pool. It should spawn rarely in coastal wetlands and beaches, look compact and weatherproof at a glance, and reward breeding for hardy pony scale rather than raw speed or show-ring refinement.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/)

## Identity & flavour

The **Chincoteague Pony** is a feral and domesticated pony breed associated with Assateague Island, the barrier island shared by Virginia and Maryland on the eastern coast of the United States. It is often called the **Assateague Pony** in a geographic sense, although “Chincoteague Pony” is the name most strongly associated with the Virginia herd, the annual Pony Swim, and the auction held by the Chincoteague Volunteer Fire Company. The breed is a coastal landrace rather than a polished, single-origin imported breed: its exact early ancestry is debated, with Spanish-shipwreck stories forming part of its folklore, while genetic and historical evidence points to a complex mix of introduced horses and ponies that adapted over generations to island life.  [virginia](https://www.virginia.org/blog/post/chincoteague-ponies/)

These ponies were shaped by salt marsh, sandy dunes, wind, heat, biting insects, sparse forage, brackish water, and the need to travel over uneven island ground. Their small stature is partly an adaptation to limited nutrition; feral ponies are commonly around 12–13 hands, while domesticated animals given richer feed may grow noticeably larger. Their work today is cultural as much as practical: they are protected feral herds, beloved children’s and family mounts, trail ponies, driving ponies, and living symbols of Virginia’s Eastern Shore. The annual pony penning and swim finance local fire-company operations and herd management, while the wild herds themselves are managed for health and ecological balance.  [en.wikipedia](https://en.wikipedia.org/wiki/Chincoteague_pony)

A traditional Chincoteague should have a close-coupled, stocky pony body, a short sturdy neck, prominent withers, sloping croup, medium-to-low tail set, short thick legs, and strong practical feet. The head is typically pony-like and blocky with a straight profile, broad forehead, and large, well-spaced eyes. Their seasonal winter coat can be dense and shaggy, while mane and tail are usually bushy rather than fine or flowing. There should be no feathering worthy of a draft breed: this is a marsh pony with weatherproof hair, broad hooves, and a body made to hold condition on poor grazing.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/)

Chincoteagues come in many colors, but **pinto is a major visual signature**, particularly tobiano. Bay, chestnut, black, palomino, buckskin, smoky black, cremello, perlino, flaxen chestnut, mealy, sooty, and gray also occur. Splash White 1 is particularly important because blue, partial blue, or two blue eyes are frequently found in the population. A player should be able to spot a Chincoteague pack from far away: a group of small, sturdy ponies with a high chance of tobiano or other bright white markings, moving through dunes, beach grass, or wetland plains.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/)

The temperament should feel practical, alert, independent, and intelligent—not automatically quiet just because the animals are ponies. Feral ancestry produces strong survival instincts, while domestic-bred Chincoteagues can become capable partners for children and adults. In Procedural Horse Genetics, breed them for resilience, compact size, strong general health, and a bright patterned-coat population. The mod does not model salt tolerance, marsh grazing, dense seasonal coat growth, obesity from rich domestic feed, laminitis tendency, local herd management, swimming ability, beach footing, pony temperament, Pony Swim training, or the cultural importance of Misty of Chincoteague.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/)

## Breed JSON

> **Schema note:** The original Procedural Horse Genetics wiki page could not be fetched by the browser tool, so this is an implementation-focused proposed file. Rename fields, source enums, commonness values, gene keys, and strain syntax to exactly match `common/breed/spec/` before committing.
>
> **Population note:** The high tobiano and Splash White 1 frequencies are deliberate design choices based on breed-standard descriptions that call tobiano the prevailing pinto pattern and identify blue/partly blue eyes as frequently associated with Splash White 1. They are not presented as a formal population-wide allele-frequency survey.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html)

```json
{
  "id": "chincoteague_pony",
  "name": "Chincoteague Pony",
  "type": "natural",
  "notes": "Chincoteague Ponies are defined by Assateague Island adaptation, small feral-landrace stature, salt-marsh survival, broad practical hooves, shaggy seasonal winter coats, and cultural management through the Pony Swim and annual auction. Procedural Horse Genetics does not model salt tolerance, brackish-water adaptation, marsh grazing, seasonal coat thickness, swimming, island herd social structure, obesity on rich domestic feed, laminitis management, Pony Swim training, or the blocky traditional pony conformation.",

  "biomes": [
    "minecraft:beach",
    "minecraft:stony_shore",
    "minecraft:mangrove_swamp",
    "minecraft:swamp",
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
  "price": 460,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.64,
      "e": 0.36
    },
    "agouti": {
      "A": 0.67,
      "a": 0.33
    },

    "cream": {
      "N": 0.80,
      "Cr": 0.20
    },
    "grey": {
      "N": 0.93,
      "G": 0.07
    },
    "dun": {
      "N": 0.97,
      "D": 0.03
    },
    "flaxen": {
      "N": 0.70,
      "f": 0.30
    },
    "silver": {
      "N": 0.985,
      "Z": 0.015
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

    "tobiano": {
      "N": 0.54,
      "To": 0.46
    },
    "sabino_1": {
      "N": 0.83,
      "SB1": 0.17
    },
    "splash_white_1": {
      "N": 0.72,
      "SW1": 0.28
    },
    "frame_overo": {
      "N": 0.98,
      "O": 0.02
    },
    "kit_white_spotting": {
      "N": 0.94,
      "W": 0.06
    },
    "rabicano": {
      "N": 0.91,
      "Rb": 0.09
    },
    "roan": {
      "N": 0.97,
      "Rn": 0.03
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
      "N": 0.90,
      "d5": 0.10
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
    "jump": 5,
    "health": 9,
    "size": [
      0.68,
      0.82
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "traditional_assateague",
      "name": "Traditional Assateague",
      "weight": 8,
      "notes": "The default close-coupled, stocky island-pony type. It favors tobiano, Splash White 1, sturdy pony size, and a traditional Assateague appearance.",
      "coat_genes": {
        "tobiano": {
          "N": 0.47,
          "To": 0.53
        },
        "splash_white_1": {
          "N": 0.68,
          "SW1": 0.32
        },
        "cream": {
          "N": 0.82,
          "Cr": 0.18
        }
      },
      "stat_scores": {
        "speed": 4,
        "jump": 5,
        "health": 9,
        "size": [
          0.68,
          0.78
        ]
      }
    },
    {
      "id": "sport_type",
      "name": "Sport Type",
      "weight": 2,
      "notes": "A private-breeding-derived type with documented Arabian, Mustang, Quarter Horse, Paint Horse, and/or Thoroughbred influence. It is taller and more refined than the traditional type while retaining Chincoteague identity.",
      "coat_genes": {
        "tobiano": {
          "N": 0.67,
          "To": 0.33
        },
        "splash_white_1": {
          "N": 0.78,
          "SW1": 0.22
        },
        "cream": {
          "N": 0.74,
          "Cr": 0.26
        }
      },
      "stat_scores": {
        "speed": 5,
        "jump": 6,
        "health": 8,
        "size": [
          0.78,
          0.88
        ]
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Chincoteague Pony × Chincoteague Pony produces Chincoteague Pony. Chincoteague Pony × another pure breed produces a cross. Traditional and Sport Type founders remain the same Chincoteague Pony breed label; strains affect founder phenotype and target traits, not lineage. A cross back to a pure Chincoteague Pony remains a cross, different crosses become Mixed, and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Proposed treatment | Reason |
|---|---:|---|
| Founder size | ×0.68–0.82 | Fits a genuine pony population usually around 12–13 hands, with the upper end allowing better-fed domesticated ponies and sport-type animals.  [en.wikipedia](https://en.wikipedia.org/wiki/Chincoteague_pony) |
| Traditional strain | 80% | Keeps the stocky, close-coupled Assateague island phenotype as the dominant experience.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html) |
| Sport strain | 20% | Represents private-breeding lines described as having Arabian, Mustang, Quarter Horse, Paint Horse, and/or Thoroughbred influence and standing about 13–14 hands, rarely 15.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html) |
| Base colors | Broad black/bay/chestnut pool | Bay, chestnut, and black are named common colors, and the breed permits a broad color range.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/) |
| Cream dilution | `Cr` 0.20 | Buckskin, palomino, smoky black, cremello, and double-cream colors are specifically documented.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/) |
| Flaxen | `f` 0.30 | Flaxen chestnut is specifically reported as common.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html) |
| Tobiano | `To` 0.46 base, 0.53 traditional | Pinto is a hallmark, and the breed standard identifies tobiano as the prevailing pinto pattern.  [en.wikipedia](https://en.wikipedia.org/wiki/Chincoteague_pony) |
| Splash White 1 | `SW1` 0.28 base | Blue, partial-blue, and two-blue-eyed ponies are reported as frequent through Splash White 1.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html) |
| Sabino and KIT | Moderate-low | Supports broad markings and irregular white expression while retaining tobiano as the leading pinto mechanism.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html) |
| Leopard complex | Forced clear | It is not a characteristic population trait in the available breed-standard material; keeping it absent preserves the recognizable pinto-focused palette. |
| Health | 9/10 | Models hardy feral-landrace adaptation and strong legs/hooves, not immunity to metabolic, orthopedic, or environmental health problems.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/) |
| Speed | 4/10 | Compact island pony, not a racing breed. |
| Jump | 5/10 | A practical, versatile middle score; sport-type ponies receive a modest increase. |

## Disorder policy

The strongest directly relevant genetic finding is **ACAN D5 dwarfism**. Chincoteague breeding material reports that D5 has been tested in the population, and an educational genetics source states that **over 10% of ponies are carriers** for skeletal atavism—not ACAN dwarfism—so those conditions must not be merged. The `d5: 0.10` value in this file is therefore a **conservative gameplay placeholder**, not a published, population-wide D5 allele frequency.  [genestogenomes](https://genestogenomes.org/genetic-test-helps-ponies-leave-the-past-behind/)

The Chincoteague Pony Association lists several tested genetic risks or susceptibility markers in the breed: recurrent uveitis, lordosis/swayback, EMS susceptibility, CIAR, D5 dwarfism, EHM risk, ocular squamous-cell-carcinoma risk, impaired acrosomal reaction, laminitis susceptibility, and West Nile virus symptom risk. It also notes observed DSLD. Most are absent from the disorder-locus list in your prompt, and several are risk alleles or multifactorial conditions rather than simple Mendelian “affected/clear” disorders. They should **not** be falsely represented by unrelated loci such as PSSM1, HERDA, CA, or SCID.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html)

The file therefore carries only `ACAN_dwarfism` with a D5 placeholder and forces the supplied unrelated disease loci clear. If PHG supports a dedicated Chincoteague health panel later, add distinct loci or risk modifiers for DSLD, lordosis, EMS/laminitis susceptibility, ERU susceptibility, CIAR, and IAR rather than overloading the ACAN locus. The general equine literature confirms that ACAN has multiple distinct dwarfism variants, so the JSON must use the exact allele key that the mod maps to **D5**, not an arbitrary generic `dwarfism` label.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/30058072/)

## Code map

| Location | Required work |
|---|---|
| `common/breed/Breed` | Add the `chincoteague_pony` natural-breed record, metadata, notes, sources, biomes, price, gene pools, strains, and target stats. |
| `common/breed/Breeds` | Register `chincoteague_pony` so it resolves in spawning, books, menus, saved genomes, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map; coat variation is driven by loci and strains rather than pinned shade values. |
| `common/breed/spec/` | Validate bidirectional JSON support for strain weights, per-strain locus overrides, and optional per-strain stat targets. |
| `common/breed/Commonness` | Confirm `RARE` resolves to weight 1.5. If only the named rarity ladder is accepted, map it to the closest existing rung. |
| `common/breed/BreedStatCurve` | Convert score targets and the pony-size intervals into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll base loci, select `traditional_assateague` or `sport_type` at 8:2 weighting, then apply its locus/stat overrides. |
| `common/breed/BreedLineage` | Retain default pure/cross/Mixed/Feral Mixed behavior; strain must not become a lineage category. |
| `common/genetics/SpliceOutcome` | No special case. A splice-carrot allele should transmit normally, but phenotype must never rewrite breed lineage. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate speed, jump, health, and size target definitions. |
| `common/breed/BandType` | No special `TRADITIONAL`/`BACHELOR` epigenetic band selection is required unless the parser mandates it. |
| Health-locus registry | Confirm that `ACAN_dwarfism` supports a distinct `d5` allele. If not, add D5 explicitly rather than assuming all ACAN variants have identical inheritance and phenotype.  [laboklin.co](https://www.laboklin.co.uk/laboklin/showGeneticTest.jsp?testID=8548) |

## Verification

1. **UI and registry:** Confirm that **Chincoteague Pony** appears in the H-menu Breeds tab and the breed book with its natural classification, rare commonness, coastal habitat description, four sources, pony size, and strain information.

2. **Wild herd identity:** Spawn a pack in a beach, shore, swamp, mangrove-swamp, or coastal-plains equivalent. Every animal in the same generated pack should display **Chincoteague Pony**.

3. **Feral distinction:** Verify that an unassigned lone wild horse reads **Feral Mixed**. It must not gain a Chincoteague label simply because it spawns on a beach or displays tobiano.

4. **Pony scale:** Generate at least 100 founders. Traditional founders should consistently read as small ponies, ordinarily within ×0.68–0.78. Sport-type founders should trend taller at ×0.78–0.88, but should still not resemble ordinary full-size warmbloods.

5. **Strain ratio:** In a large sample, confirm roughly 80% `traditional_assateague` and 20% `sport_type`. Inspect strain metadata directly; visible size alone can overlap.

6. **Color population:** Across 200 founders, expect bays, chestnuts, blacks, and cream-dilute colors; frequent white markings; and a visibly high tobiano rate. Splash-associated blue or partial-blue eyes should appear more often than in a generic feral population.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html)

7. **Exclusions:** Confirm pure founders do not roll leopard complex, PATN1/PATN2, pearl, mushroom, champagne, brindle, or magical coat alleles.

8. **D5 implementation:** If `d5` is a recessive ACAN allele in PHG, breed two known carriers and confirm the model’s intended Mendelian result. Do not treat all ACAN mutations as interchangeable: verify that the genotype-to-phenotype outcome is the one configured for D5 in the codebase.  [laboklin.co](https://www.laboklin.co.uk/laboklin/showGeneticTest.jsp?testID=8548)

9. **Lineage table:** Verify:
   - Chincoteague Pony × Chincoteague Pony → Chincoteague Pony.
   - Traditional strain × Sport Type strain → Chincoteague Pony.
   - Chincoteague Pony × another pure breed → cross.
   - Cross × pure Chincoteague Pony → cross.
   - Different cross × different cross → Mixed.
   - Any lineage × Feral Mixed → Mixed.

10. **Stat feel:** Compare equivalent-age founders. Chincoteagues should be clearly resilient and compact, moderate at jumping, and below ordinary riding-horse speed. They should feel excellent for a coastal survival or practical-pony stable, not like miniature Thoroughbreds.

## Sources

- [Chincoteague Pony Association — Breed Standards](https://www.chincoteagueponyassociation.com/breed-standards.html): traditional and sport-type conformation, height, coat colors, tobiano prevalence, Splash White 1/blue-eye context, and breed-tested health markers.  [chincoteagueponyassociation](https://www.chincoteagueponyassociation.com/breed-standards.html)
- [International Chincoteague Pony Association & Registry / Chincoteague Pony Pedigree Database](https://chincoteaguepedigrees.com/pedigree/pedigree.home.php): official registry-affiliated pedigree database.  [chincoteaguepedigrees](https://chincoteaguepedigrees.com/pedigree/pedigree.home.php)
- [Chincoteague Chamber of Commerce — Chincoteague Ponies](https://www.chincoteague.com/pony-swim/chincoteague-ponies/): official local cultural context, formal breed recognition, and 12–13-hand average.  [chincoteague](https://www.chincoteague.com/pony-swim/chincoteague-ponies/)
- [Virginia Tourism — Chincoteague Ponies](https://www.virginia.org/blog/post/chincoteague-ponies/): Assateague habitat, feral-landrace context, 12–13-hand stature, and 1991 genetic relationship findings.  [virginia](https://www.virginia.org/blog/post/chincoteague-ponies/)
- [MadBarn — Chincoteague Pony Breed Profile](https://madbarn.com/chincoteague-pony-breed-profile/): height, coat-color list, conformation, winter coat, and health-context overview.  [madbarn](https://madbarn.com/chincoteague-pony-breed-profile/)
- [Genes to Genomes — Genetic test helps ponies leave the past behind](https://genestogenomes.org/genetic-test-helps-ponies-leave-the-past-behind/): skeletal atavism carrier context; included to distinguish it from ACAN dwarfism rather than as an ACAN frequency source.  [genestogenomes](https://genestogenomes.org/genetic-test-helps-ponies-leave-the-past-behind/)
- [Laboklin — ACAN chondrodysplasia/dwarfism](https://www.laboklin.co.uk/laboklin/showGeneticTest.jsp?testID=8548): distinct ACAN dwarfism variants and genotype implications.  [laboklin.co](https://www.laboklin.co.uk/laboklin/showGeneticTest.jsp?testID=8548)
