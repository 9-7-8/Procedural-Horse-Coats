The **Walkaloosa** should be implemented as a rare American **gaited Appaloosa-patterned registry breed**: medium-sized, smooth-traveling, generally bay-based, and built around leopard-complex inheritance rather than tobiano/overo pinto genetics. Every fully registered Walkaloosa founder should carry `LP` and visibly show Appaloosa characteristics or patterning; a small solid “ID/breeding purposes” strain can exist for real registry context, but should not be the normal player-facing phenotype. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

## Identity & flavour

The **Walkaloosa**, sometimes described as a gaited Appaloosa or gaited leopard horse, is an American registry breed built around two inherited traits: an **intermediate ambling gait** and recognizable **Appaloosa/leopard-complex coloration**. The Walkaloosa Horse Registry was established in **1983** to preserve and register horses that combined verified Appaloosa ancestry with blood from naturally gaited breeds. Its earliest practical crosses often involved Appaloosas and Paso Finos, but eligible horses may descend from a wider range of gaited stock so long as they meet the registry’s color and gait requirements. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

The breed exists because an ordinary Appaloosa can be athletic and versatile without necessarily being naturally gaited, while an ordinary gaited horse can be comfortable without carrying leopard-complex patterning. A Walkaloosa joins both. It should perform an intermediate gait other than a trot—such as a running walk, fox trot, rack, single-foot, stepping pace, or pace—giving a rider smooth, efficient travel across trails and open country. Registry eligibility requires gait certification by a veterinarian, professional trainer, or accepted video evidence; this is a functional breed identity, not just a visual one. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

Walkaloosas generally stand **13–16 hands**, with **14–15.2 hands** often cited as the preferred range. They should be a practical saddle-horse type: compact enough for easy handling, substantial enough for adult trail riding, and less stocky than a halter Quarter Horse. The head is generally neat and expressive, the neck medium to long, the shoulder sloping, the back strong, the hip capable, and the legs clean with no heavy feather. Mane and tail are full but ordinary; there is no special mane-shape gene. A good Walkaloosa looks like a useful trail horse first, then surprises players with a smooth, ground-covering gait and bright Appaloosa pattern. [horsebreedspictures](https://www.horsebreedspictures.com/walkaloosa.asp)

The coat is fundamentally **leopard complex**, not pinto. Bay is widely described as common, but any ordinary base color can appear beneath the Appaloosa pattern except colors excluded by registry convention such as gray, white, and mushroom. Registered Walkaloosas should visibly show Appaloosa characteristics from at least ten feet: mottled skin, white sclera, striped hooves, varnish effects, snowflake, frost, blanket, leopard, few-spot, or similar `LP`-driven outcomes. `PATN1` and the mod’s `PATN2` should create the wide variety of blanket and leopard expression. A nearly white few-spot Walkaloosa is still genetically Appaloosa-patterned; it is not a tobiano or overo pinto. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

Walkaloosas are intended to be sensible, athletic, sure-footed, and comfortable for distance riding. In Procedural Horse Genetics, they should be a compelling trail-breeding project: a bay, black, chestnut, palomino, buckskin, dun, roan, or flaxen horse with `LP` markings and a gaited identity, but never a generic Paint horse with spots. The mod does not model the gait itself, rider comfort, gait certification, striped hoof keratin, mottled skin, sclera, ultraviolet sensitivity, Appaloosa uveitis risk, rider skill, trail training, breed-paper categories, or the distinction between full registry eligibility and the registry’s identification-only breeding papers. It should, however, respect the real `LP/LP` association with congenital stationary night blindness when the engine has appropriate support. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in your provided Anglo-Arabian example. Before compilation, reconcile exact key names, source enums, `LP` and `PATN` locus syntax, allele symbols, rare-color definitions, commonness values, pricing units, strain support, and disease-effect handling with `common/breed/spec/`.
>
> **Scientific-policy caveat:** The Walkaloosa Registry provides clear phenotype and ancestry requirements—visible Appaloosa color plus an intermediate non-trotting gait—but not a published population-wide allele-frequency survey. `LP` is therefore fixed in full-registry founders because it is a direct genetic representation of the required Appaloosa phenotype; the numeric rates for `PATN1`, `PATN2`, base colors, cream, dun, roan, and flaxen are transparent **gameplay approximations**, not registry statistics. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

```json
{
  "id": "walkaloosa",
  "name": "Walkaloosa",
  "type": "natural",
  "notes": "The Walkaloosa is defined by Walkaloosa Registry pedigree rules, verified Appaloosa ancestry, a visibly Appaloosa-patterned leopard-complex coat, and a certified intermediate gait other than an ordinary trot. Procedural Horse Genetics does not model the running walk, fox trot, rack, single-foot, stepping pace, pace, gait timing, rider comfort, gait certification, striped hoof keratin, mottled skin, visible white sclera, trail training, registry paperwork, identification-only papers, ultraviolet sensitivity, or Appaloosa-associated uveitis risk. If supported elsewhere in the engine, LP homozygosity should retain its real association with congenital stationary night blindness.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:windswept_hills",
    "minecraft:river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "leopard_complex": {
      "N": 0.0,
      "LP": 1.0
    },
    "patn1": {
      "N": 0.42,
      "PATN1": 0.58
    },
    "patn2": {
      "N": 0.52,
      "PATN2": 0.48
    },

    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "dun": {
      "N": 0.93,
      "D": 0.07
    },
    "roan": {
      "N": 0.92,
      "Rn": 0.08
    },
    "flaxen": {
      "N": 0.93,
      "f": 0.07
    },

    "grey": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },
    "silver": {
      "N": 1.0
    },
    "champagne": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },

    "tobiano": {
      "N": 1.0
    },
    "frame_overo": {
      "N": 1.0
    },
    "sabino_1": {
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
    "health": 8,
    "size": [
      0.94,
      1.09
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "regular_registry",
      "name": "Regular Walkaloosa",
      "weight": 0.90,
      "notes": "The ordinary fully eligible registry population: every founder carries LP and should visibly show Appaloosa patterning or characteristics from a distance. Stronger PATN selection creates blankets, spotted blankets, snowcaps, leopards, and few-spots alongside frost and varnish outcomes.",
      "required_all_of": [
        "leopard_complex:LP"
      ],
      "coat_genes": {
        "leopard_complex": {
          "N": 0.0,
          "LP": 1.0
        },
        "patn1": {
          "N": 0.28,
          "PATN1": 0.72
        },
        "patn2": {
          "N": 0.38,
          "PATN2": 0.62
        }
      }
    },
    {
      "id": "id_breeding_stock",
      "name": "ID / Breeding Stock",
      "weight": 0.10,
      "notes": "A small identification-only breeding-purpose pool reflecting registry horses that lack visible Appaloosa coloration at the required distance or do not present a qualifying gait. This strain should not be treated as a fully regular Walkaloosa in real registry terms.",
      "coat_genes": {
        "leopard_complex": {
          "N": 0.88,
          "LP": 0.12
        },
        "patn1": {
          "N": 0.90,
          "PATN1": 0.10
        },
        "patn2": {
          "N": 0.92,
          "PATN2": 0.08
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Walkaloosa × Walkaloosa produces Walkaloosa, including a genetically solid or visually insufficient offspring from two pure lineage labels. Walkaloosa × another pure breed produces a Walkaloosa cross. A Walkaloosa cross bred back to pure Walkaloosa remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Registry appearance is not identical to lineage: a pure-lineage but LP-negative or non-gaited foal could receive only ID papers in real registry practice, while retaining Walkaloosa lineage in the mod."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | `LP`-centered gaited Appaloosa registry | Registry eligibility requires Appaloosa ancestry, visible Appaloosa coloration, and an intermediate gait. This makes `LP`, not pinto, the genetic core.  [walkaloosaregistry](https://walkaloosaregistry.com/history/) |
| Leopard complex | `LP: 1.0` in regular founders | A true regular Walkaloosa must visibly express Appaloosa color characteristics. Fixing `LP` makes mottled skin, white sclera, striped-hoof traits, varnish, and Appaloosa-pattern inheritance available in every regular founder.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/leopard-complex) |
| Pattern modifiers | `PATN1: 0.58`; `PATN2: 0.48`, elevated in regular strain | `LP` is an incompletely dominant base for the Appaloosa complex, while pattern modifiers increase large blankets, snowcaps, leopards, and few-spot outcomes. Exact Walkaloosa frequencies are not published, so these are gameplay calibrations intended to maintain visible patterning.  [journals.plos](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0078280) |
| Base colors | `E: 0.72 / e: 0.28`; `A: 0.68 / a: 0.32` | Bay is often reported as common. These settings make bay the dominant underlying base while preserving black and chestnut Appaloosa-patterned founders. Values are estimates, not registry data.  [horsebreedspictures](https://www.horsebreedspictures.com/walkaloosa.asp) |
| Cream | `Cr: 0.10` | Cream-compatible Appaloosa patterns can occur in the broad Walkaloosa pool. This low-moderate rate permits palomino/buckskin outcomes without making them typical. |
| Dun | `D: 0.07` | Dun and grullo-compatible leopard patterns are plausible in broad Appaloosa-derived ancestry. Retain as a visible minority. |
| Roan | `Rn: 0.08` | Roan is treated as a rare additional Walkaloosa color in breed summaries, but must remain genetically distinct from `LP` varnish roaning.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Walkaloosa.php) |
| Flaxen | `f: 0.07` | A low rate allows occasional flaxen chestnut Appaloosa-patterned horses. This is a gameplay choice rather than a measured frequency.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Walkaloosa.php) |
| Gray, white, mushroom | Forced wild type | Walkaloosa registry-oriented references exclude gray, white, and mushroom from the normal color range. Gray would progressively obscure Appaloosa patterning; mushroom is not supported for the founder population.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Walkaloosa.php) |
| Silver, champagne, pearl | Forced wild type | No adequate registry or genetic evidence was located to seed them as ordinary pure Walkaloosa founder loci. |
| Pinto loci | Tobiano, frame, sabino, splash, and KIT white forced wild type | Walkaloosa is an Appaloosa/leopard breed, not a Paint/Pinto registry. Its bright white patterns should derive from `LP` plus modifiers, not modern pinto loci.  [walkaloosaregistry](https://walkaloosaregistry.com/history/) |
| Leopard versus pinto | `LP/PATN`, not `TO` or `O` | A leopard, few-spot, blanket, snowcap, varnish, frost, or snowflake horse is genetically different from a tobiano, frame, splash, or sabino pinto. The renderer must preserve that distinction.  [journals.plos](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0078280) |
| Named disorders | All requested panel loci clear | No defensible Walkaloosa-specific carrier frequencies were located for ACAN, PLOD1/WFFS, MET/frame, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Do not import values from Appaloosas or gaited foundation breeds without data. |
| LP/CSNB | Derived from `LP/LP` genotype | Horses with two `LP` alleles have congenital stationary night blindness, caused by a `TRPM1` insertion. It is congenital and nonprogressive; this is a real medical consequence that should be engine-linked to LP rather than duplicated as an unrelated disease gene.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2516064/) |
| Speed | `6/10` | Represents smooth, practical trail travel—not sprint-race specialization. |
| Jump | `4/10` | Walkaloosas are versatile saddle horses, but no evidence supports specialist jumper selection. |
| Health | `8/10` | Represents generally hardy gaited-trail-horse utility, explicitly tempered by LP-linked night blindness in `LP/LP` individuals. |
| Size | `×0.94–1.09` | Covers the reported 13–16-hand breed range while centering near the desired 14–15.2-hand riding-horse height.  [horsebreedspictures](https://www.horsebreedspictures.com/walkaloosa.asp) |

## Disorder approach

Every disorder locus listed in the prompt is **clear** in the pure Walkaloosa founder pool. No direct Walkaloosa carrier-rate evidence was found for ACAN dwarfism, PLOD1/WFFS, `EDNRB`/frame lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

The important genetic health rule is instead linked directly to `LP`:

| Leopard-complex genotype | Coat implication | Health implication |
|---|---|---|
| `N/N` | No leopard-complex phenotype | No LP-associated CSNB genotype |
| `N/LP` | Appaloosa characteristics and variable leopard-complex pattern expression | Not genetically designated as LP-homozygous CSNB |
| `LP/LP` | Often more extensively white, including few-spot/snowcap tendencies depending on modifiers | Congenital stationary night blindness; reduced low-light vision from birth, nonprogressive |

`LP/LP` congenital stationary night blindness is linked to a `TRPM1` insertion and is directly supported by peer-reviewed genetic evidence. It must **not** be misfiled under `PRKDC`, `TOE1`, `MYO5A`, `GBE1`, `PPIB`, or one of the prompt’s unrelated disorder loci. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2516064/)

If the mod currently lacks a CSNB/vision system, retain `LP` for correct coat genetics and record the night-blindness effect as an engine limitation. Do not solve that limitation by inventing a fake disease-frequency entry.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `walkaloosa` natural-breed record with gaited-Appaloosa flavour, restricted source set, rare commonness, `LP/PATN` founder pools, strains, and medium trail-horse stats. |
| `common/breed/Breeds` | Register the breed ID for stable and cowboy access, spawn eggs, books, H-menu display, commands, genome serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is omitted because Walkaloosas are a managed registry population rather than a natural feral herd. |
| `common/breed/BreedBands` | Support an empty epigenetic-band map. Do not imitate Appaloosa visibility or gait with coat-depth epigenetics; both must derive from the relevant renderer/genotype and future gait system. |
| `common/breed/spec/` | Verify exact locus names for `LP`, `PATN1`, `PATN2`, and any `TRPM1`/CSNB linkage. Confirm whether `required_all_of` and strain-specific pool overrides are supported. |
| `common/breed/Commonness` | Confirm that `RARE` maps to the requested rarity ladder and retain `spawn_weight: 1.5` if direct numeric weights are valid. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 4, health 8, and size ×0.94–1.09 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Choose the Regular or ID strain first. Regular founders must carry `LP` and be rerolled if the renderer does not produce a visible Appaloosa phenotype; ID founders can be `LP`-negative or minimally expressed. Force pinto and excluded-color loci wild type. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed behavior. A leopard-patterned horse must not gain Walkaloosa lineage merely from carrying `LP`; actual identity also depends on gaited ancestry and registry status. |
| `common/genetics/SpliceOutcome` | No special rule. A spliced allele transmits normally and can produce pinto, gray, mushroom, or other nonstandard descendants outside normal Walkaloosa registration phenotype. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize medium-size, good practical travel speed, modest jumping, and high-heartiness target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the real schema mandates one. Do not confuse an “ID/breeding stock” strain with a `BandType` enum. |

## Verification

1. Confirm **Walkaloosa** appears in the H-menu’s Breeds tab and breed book with the correct gaited-Appaloosa identity, rare commonness, managed acquisition sources, `LP`/CSNB warning, and explicit distinction from Paint/Pinto horses.

2. Confirm Walkaloosa does **not** spawn as a generic natural wild pack because `wild` is omitted. It should enter the world through cowboy, stable, or spawn-egg sources.

3. Confirm an ordinary lone wild horse reads **Feral Mixed**, even if it is a small bay leopard, has a blanket, or happens to carry an ambling-gait trait in a future gait system.

4. Generate at least 500 pure founders:
- About 90% should be Regular Walkaloosas: visibly Appaloosa-patterned or visibly LP-characteristic.
- About 10% may be ID/breeding-stock horses with visually insufficient or absent leopard patterning.
- Regular founders should display blankets, spotted blankets, snowcaps, leopards, few-spots, frost, snowflake, varnish, or dark-spotted outcomes.
- Bay should remain the most common underlying base; black and chestnut should occur as minority bases.

5. Confirm that pure founders never generate gray, mushroom, silver, champagne, pearl, tobiano, frame overo, Sabino 1, splash, KIT dominant white, rabicano, brindle, or magical coats.

6. Inspect genome panels:
- Every Regular founder must carry at least one `LP`.
- `PATN1` and `PATN2` must occur often enough in Regular founders to make large blankets/leopards visually common.
- The ID strain may contain `N/N` at `LP` or low-pattern `LP` horses, but must remain a clearly labeled minority.
- All named pinto loci must remain wild type.

7. Test `LP` inheritance and CSNB:
- `N/LP × N/N` should pass `LP` to about half of foals.
- `N/LP × N/LP` should yield approximately 25% `LP/LP`, 50% `N/LP`, and 25% `N/N`.
- `LP/LP` horses should display the mod’s appropriate few-spot/snowcap tendency, subject to `PATN` modifiers.
- If a vision system exists, `LP/LP` horses should be unable or impaired in low-light travel while retaining normal daylight behavior.
- Do not apply CSNB to `N/LP` horses merely because they are patterned.

8. Confirm every named disorder-panel locus remains clear in pure founders. No WFFS, HYPP, GBED, HERDA, PSSM1, SCID, CA, LFS, frame lethal-white risk, or unrelated disease allele should originate in the default pool.

9. Test default lineage:
- Walkaloosa × Walkaloosa → Walkaloosa.
- Walkaloosa × Appaloosa → Walkaloosa cross.
- Walkaloosa × Paso Fino → Walkaloosa cross.
- Walkaloosa cross × pure Walkaloosa → existing Walkaloosa cross.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice insertion. Adding tobiano, frame, splash, gray, mushroom, or a large warmblood body-size target must create genetically normal offspring but visually nonstandard registry candidates. They should retain cross/Mixed lineage behavior instead of being auto-certified as pure Walkaloosas.

## Sources

- [Walkaloosa Horse Registry — History and registration requirements](https://walkaloosaregistry.com/history/): primary registry source for 1983 formation, Appaloosa-plus-gaited ancestry, visible Appaloosa-color requirement, required intermediate gait, gait-certification process, and identification-only registration for visually insufficient or non-gaited horses. [walkaloosaregistry](https://walkaloosaregistry.com/history/)

- [Walkaloosa horse history and description](https://www.horsebreedspictures.com/walkaloosa.asp): supplementary account of breed formation, Paso Fino × Appaloosa origins, 13–16-hand range, desired 14–15.2-hand size, gaited Appaloosa phenotype, and registration rules. [horsebreedspictures](https://www.horsebreedspictures.com/walkaloosa.asp)

- [Walkaloosa breed reference](https://hi3.horseisle.com/www/bbb/Walkaloosa.php): supplementary reference for 1983 registry goal, gaited-Appaloosa cross structure, required gait and leopard phenotype, breed color exclusions, rare modifiers, and common height range. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Walkaloosa.php)

- [UC Davis Veterinary Genetics Laboratory — Leopard Complex](https://vgl.ucdavis.edu/test/leopard-complex): authoritative `LP` phenotype, inheritance, Appaloosa characteristics, pattern variability, and practical genetic-testing interpretation. [vgl.ucdavis](https://vgl.ucdavis.edu/test/leopard-complex)

- [Evidence for a retroviral insertion in `TRPM1` as the cause of congenital stationary night blindness](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0078280): peer-reviewed evidence that `LP/LP` horses have CSNB and that the associated causal insertion is in `TRPM1`. [journals.plos](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0078280)

- [UC Davis Center for Equine Health — Congenital Stationary Night Blindness](https://ceh.vetmed.ucdavis.edu/health-topics/congenital-stationary-night-blindness-csnb): veterinary explanation of `LP/LP` CSNB, congenital nonprogressive night-vision loss, and management implications. [ceh.vetmed.ucdavis](https://ceh.vetmed.ucdavis.edu/health-topics/congenital-stationary-night-blindness-csnb)