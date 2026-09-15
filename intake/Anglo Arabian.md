The **Anglo-Arabian** is best represented as a French-rooted, Arabian–Thoroughbred sport breed: refined and spirited, genuinely endurance-oriented, taller and more substantial than a typical Arabian, and genetically dominated by bay, chestnut, black, and gray. Because its registry definition is fundamentally pedigree-based rather than a closed visual type, the file should preserve a comparatively tight Arab/Thoroughbred-compatible coat pool while retaining Arabian-line disease loci at cautious, defensible rates. [wbfsh](https://wbfsh.com/studbooks/AA)

## Identity & flavour

The **Anglo-Arabian**, also called the **Anglo-Arab** or simply **Anglo**, is a recognized sport-horse breed created from the deliberate blending of the purebred Arabian and English Thoroughbred. It emerged principally in France during the nineteenth century, with the term appearing in French writing by 1848 and formal breed criteria established in 1880. The French stud-book recorded the type in the mid-nineteenth century, and international studbooks later organized through the International Confederation of Anglo-Arab Studbooks. [wbfsh](https://wbfsh.com/studbooks/AA)

It was made to unite Arabian endurance, thrift, soundness, courage, and sensitivity with Thoroughbred size, gallop, reach, and jumping power. The result is an athletic riding horse built for long distances and demanding sport: eventing, endurance, jumping, dressage, hunting, racing, and general performance riding all suit it. An Anglo should feel less specialized than a pure racehorse and less compact than a pure Arabian—an elegant all-round athlete capable of covering ground for a long time and still answering its rider. [wbfsh](https://wbfsh.com/studbooks/AA)

Anglo-Arabians commonly stand about 15.2–16.3 hands, though some sport-bred individuals reach 17 hands. They tend to have a fine, expressive head that may retain a slight Arabian dish, a long elegant neck, pronounced withers, deep chest, strong compact body, good bone, rounded and powerful hindquarters, and long athletic legs. The breed is not a draft horse and should carry no feather. Its mane and tail are usually ordinary, flowing riding-horse hair rather than a special genetic hallmark, and its hooves should read as solid working sport-horse feet rather than unusually large or heavy ones. [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/)

Bay, chestnut, and gray are the classic and most recognizable Anglo-Arabian colors. Black occurs but is less common. Registries may have broad color acceptance, but the two source breeds strongly constrain what belongs in a historically typical population: cream, champagne, dun, silver, tobiano, true roan, leopard complex, and loud pinto-pattern alleles should not arise from pure Anglo-Arab founders. Small ordinary white markings—and potentially subtle sabino-like expression through the wider Arabian-derived pool—are more appropriate than Paint-like patterning. [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/)

Anglos are intelligent, willing, spirited, elegant, and sometimes sensitive: the kind of horse that rewards tactful riding rather than blunt handling. Their visual signature is not a rare dilution or a heavy mane—it is a clean, rangy, fine-headed performance silhouette, a ground-covering stride, and the unmistakable combination of Arabian refinement with Thoroughbred scope. In this mod, players should breed toward a tallish, fast, exceptionally enduring sport horse with a strong chance of gray and a classic bay-or-chestnut palette. The mod does not simulate pedigree percentage, Arabian vertebral traits, head dish, subjective refinement, stride length, endurance metabolism, rider sensitivity, or the training that separates an excellent eventer from a merely athletic cross. [wbfsh](https://wbfsh.com/studbooks/AA)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki URL could not be retrieved, so exact current field names, enum values, and locus IDs need to be matched to `common/breed/spec/` before compilation. This uses the same proposed, readable JSON convention as the prior Saddlebred definition. It intentionally models the Anglo-Arabian as a distinct recognized breed, not as an automatically generated `arabian × thoroughbred` cross.

```json
{
  "id": "anglo_arabian",
  "name": "Anglo-Arabian",
  "type": "natural",
  "notes": "The Anglo-Arabian is defined partly by documented Arabian and Thoroughbred pedigree percentages, plus refinement, sensitivity, gallop, stride, stamina, and sport training. Procedural Horse Genetics does not model required pedigree fractions, studbook eligibility, an Arabian head profile or vertebral count, subjective elegance, stride mechanics, endurance conditioning, rider sensitivity, or discipline-specific training.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:windswept_hills"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 680,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.64,
      "e": 0.36
    },
    "agouti": {
      "A": 0.70,
      "a": 0.30
    },

    "grey": {
      "N": 0.68,
      "G": 0.32
    },
    "sabino_1": {
      "N": 0.975,
      "SB1": 0.025
    },
    "rabicano": {
      "N": 0.985,
      "Rb": 0.015
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
    "PRKDC_SCID": {
      "N": 0.966,
      "scid": 0.034
    },
    "TOE1_CA": {
      "N": 0.949,
      "ca": 0.051
    },
    "MYO5A_LFS": {
      "N": 0.883,
      "lfs": 0.117
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
    "speed": 8,
    "jump": 8,
    "health": 8,
    "size": [
      1.02,
      1.13
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Anglo-Arabian × Anglo-Arabian produces Anglo-Arabian. Anglo-Arabian × another pure breed produces a cross. Do not automatically label every Arabian × Thoroughbred foal Anglo-Arabian unless the mod adds a pedigree-aware registry rule; default lineage behavior should call that foal a cross."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Base colors | `E` 0.64 / `e` 0.36; `A` 0.70 / `a` 0.30 | Produces a population centered on bay, chestnut, and some black, matching common descriptions of the breed.  [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/) |
| Gray | `G` 0.32 | Gray is one of the three repeatedly cited common colors; this is a game-balance approximation, not a measured Anglo-Arab allele survey.  [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/) |
| White marking loci | Low sabino/rabicano only | Keeps ordinary markings possible without treating a historically Arab–Thoroughbred-derived breed as a pinto or leopard population. Pure Arabian color guidance recognizes base colors, gray, and limited sabino-like white expression—not cream, dun, tobiano, leopard complex, or similar dilutions/patterns.  [victorygenomics](https://victorygenomics.com/arabian-horse-colors-explained/) |
| Dilutions and loud patterns | Forced wild type | Pure founders should not generate cream, champagne, silver, dun, pearl, tobiano, frame, splash, W-series dominant white, true roan, or leopard spotting. Players can introduce them through outcrossing.  [victorygenomics](https://victorygenomics.com/arabian-horse-colors-explained/) |
| SCID | `scid` 0.034 | The 2009/10 Arabian foal-crop study reported a 3.4% carrier prevalence; this is used as a cautious proxy allele rate for an Anglo-Arabian pool rather than claiming a direct breed-specific survey.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/) |
| CA | `ca` 0.051 | The same Arabian foal-crop study reported a 5.1% carrier prevalence. Use as a deliberately conservative Arabian-line approximation.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/) |
| LFS | `lfs` 0.117 | This reflects the cited 11.7% carrier estimate in an Arabian foal-crop study. It is an upper-bound proxy for an Anglo-Arabian founder pool; reduce it if your mod treats Anglo-Arabs as heavily Thoroughbred-weighted.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/) |
| Speed | 8/10 | Captures Thoroughbred-influenced gallop and a breed regularly used for endurance and sport.  [wbfsh](https://wbfsh.com/studbooks/AA) |
| Jump | 8/10 | Eventing and jumping are central named aptitudes; this is a meaningful breed strength.  [wbfsh](https://wbfsh.com/studbooks/AA) |
| Health | 8/10 | Represents durability and stamina—not immunity from inherited recessives.  [wbfsh](https://wbfsh.com/studbooks/AA) |
| Size | ×1.02–1.13 | Puts ordinary founders slightly above a baseline riding horse, consistent with the common 15.2–16.3-hand range.  [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/) |

## Disorder caution

The rates above should be understood as **Arabian-derived proxy rates**, not demonstrated population rates for every international Anglo-Arabian registry. The available cited carrier-prevalence study concerns Arabian foals, and Anglo-Arabian populations differ by country, registration rules, and Arabian percentage. The French and international Anglo-Arabian population may not have the same allele frequencies as U.S.-registered Anglo-Arabs.

If you prefer a stricter “only directly evidenced in this breed” approach, use only `PRKDC_SCID` with a much lower frequency—such as `0.01`—because one Morocco dataset found 1 carrier among 30 Anglo-Arabian/crossbred Arabian horses, or 3.3%. That is a tiny sample, so it is not robust enough to be treated as a definitive breed-wide number. [academia](https://www.academia.edu/67374033/Frequency_of_the_severe_combined_immunodeficiency_disease_gene_among_horses_in_Morocco)

For gameplay that makes health testing relevant while avoiding needless disease saturation, the above trio is the more interesting implementation. All three are recessive disorders associated with Arabian ancestry: SCID compromises immune function, CA is a progressive neurologic condition, and LFS is a severe neonatal syndrome. [en.wikipedia](https://en.wikipedia.org/wiki/Anglo-Arabian)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `anglo_arabian` natural breed record, its metadata, source availability, biome list, commonness, price, and notes. |
| `common/breed/Breeds` | Register `anglo_arabian` so IDs resolve in spawning, books, menus, saved genomes, and lineage display. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable` as legal sources. |
| `common/breed/BreedBands` | Allow this breed to use an empty epigenetic-band object. No shade or body-stat range should be forced beyond the breed-stat targets. |
| `common/breed/spec/` | Match all key names, array/object conventions, source enums, and gene IDs to the mod’s actual serialization and parsing schema. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the intended rarity ladder weight. If the project uses numerical weights directly, retain `3`. |
| `common/breed/BreedStatCurve` | Map scores 8/8/8 and size ×1.02–1.13 to legal target bands. |
| `common/breed/BreedFounder` | Roll founders from only the supplied pools and force omitted coat and disorder loci to wild type/clear. |
| `common/breed/BreedLineage` | Keep default lineage behavior, while preserving the distinction between a registered Anglo-Arabian founder and a generic Arabian × Thoroughbred cross. |
| `common/genetics/SpliceOutcome` | No breed-specific exception: inserted alleles should transmit according to normal splice-carrot behavior. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Ensure the four stat axes and their Anglo-Arabian target ranges can serialize and generate. |
| `common/breed/BandType` | No special use expected. Do not use `TRADITIONAL` or `BACHELOR` unless required by the existing spec. |

## Verification

1. Confirm that **Anglo-Arabian** appears in the H-menu’s Breeds tab and in the breed book, with the right display name, natural classification, sources, biomes, rarity, and description.

2. Spawn wild packs in an eligible biome. Every horse created as part of the same pack should identify as **Anglo-Arabian**, even when founders differ in base color or gray status.

3. Check a lone unassigned wild horse. It should read **Feral Mixed**, not Anglo-Arabian.

4. Generate at least 100–200 Anglo-Arabian founders. Most should be bay, chestnut, black-based, or gray; a horse with gray should progressively show the mod’s normal gray expression. Pure founders should not produce palomino, buckskin, dun, champagne, silver dapple, tobiano, frame overo, splash, true roan, leopard complex, or magical coats.

5. Inspect disorder genomes. Only `PRKDC_SCID`, `TOE1_CA`, and `MYO5A_LFS` should be capable of rolling as non-clear. All other listed disorder loci should be clear in a pure founder.

6. Test recessive inheritance with deliberately selected carrier pairs. Over a large foal sample from two heterozygous carriers at any one locus, expect approximately 25% affected, 50% carriers, and 25% clear offspring.

7. Test the lineage table:
   - Anglo-Arabian × Anglo-Arabian → Anglo-Arabian.
   - Anglo-Arabian × Arabian → Anglo-Arabian cross, under the default table.
   - Anglo-Arabian × Thoroughbred → Anglo-Arabian cross, under the default table.
   - Anglo-Arabian cross × pure Anglo-Arabian → the same cross classification.
   - Two different crosses → Mixed.
   - Any cross with Feral Mixed → Mixed.

8. Verify that stats feel coherent in play: the breed should trend fast, athletic, and durable, with a slightly taller saddle-horse frame, but it should not mechanically eclipse specialist Thoroughbred racers, purpose-bred jumpers, or unusually hardy mountain breeds.

## Sources

- [World Breeding Federation for Sport Horses — Anglo-Arabian studbook profile](https://wbfsh.com/studbooks/AA): French nineteenth-century stud-book history, Arabian–Thoroughbred origin, international development, and aptitude for eventing, endurance, jumping, and dressage. [wbfsh](https://wbfsh.com/studbooks/AA)
- [Anglo-Arabians — “About Anglos”](https://anglo-arabians.com/about-anglos/): Arabian percentage framework of 25%–75% for Anglo-Arabian pedigree. [anglo-arabians](https://anglo-arabians.com/about-anglos/)
- [MadBarn — Anglo-Arabian Horse Breed Profile](https://madbarn.ca/anglo-arabian-horse-breed-profile/): origin, formal 1880 criteria, height, conformation, colors, sport roles, and registry context. [madbarn](https://madbarn.ca/anglo-arabian-horse-breed-profile/)
- [Horse Illustrated — Half-Arabian Horse Breeds: Anglo-Arabian](https://www.horseillustrated.com/horse-breeds-half-arabian-anglo-arabian/): U.S. registration context, twentieth-century pedigree practice, height, and sport disciplines. [horseillustrated](https://www.horseillustrated.com/horse-breeds-half-arabian-anglo-arabian/)
- [UC Davis Veterinary Genetics Laboratory — SCID](https://vgl.ucdavis.edu/test/scid): SCID mutation prevalence context in Arabian horses. [vgl.ucdavis](https://vgl.ucdavis.edu/test/scid)
- [PubMed — Carrier prevalence of SCID, LFS, and CA in Arabian foals](https://pubmed.ncbi.nlm.nih.gov/24033554/): 2009/10 carrier estimates of 3.4% for SCID, 5.1% for CA, and 11.7% for LFS, used here only as transparent Arabian-line proxy data. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/)
- [World Arabian Horse Organization — Genetic Disorders in Arabian Horses](http://www.waho.org/genetic-disorders-in-arabian-horses-current-research-projects/): SCID history and the importance of genetic testing in Arabian-line breeding. [waho](http://www.waho.org/genetic-disorders-in-arabian-horses-current-research-projects/)
