The **Spotted Saddle Horse** should be a large, colorful American gaited trail horse with **mandatory pinto spotting** in ordinary registered founders and a genetically meaningful solid breeding-stock exception. Its identity is the combination of a smooth, non-trotting four-beat gait and conspicuous white body patches—usually tobiano, frame-derived overo, sabino, or combinations—not simply a stock-horse pedigree or any horse with white socks. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

## Identity & flavour

The **Spotted Saddle Horse** is an American gaited riding breed that developed in the southeastern United States during the second half of the twentieth century. It draws on the same practical riding-horse culture that produced Tennessee Walking Horses and related southern gaited breeds, while adding the colorful pinto inheritance prized by breeders and trail riders. The National Spotted Saddle Horse Association (**NSSHA**) began in 1979; the Spotted Saddle Horse Breeders’ and Exhibitors’ Association (**SSHBEA**) is another registry serving the breed. Unlike a tightly pedigree-closed breed, registration historically emphasizes what the horse **does** and what it **looks like**: it must be gaited and must show qualifying pinto spotting. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

The breed was made for comfort. A Spotted Saddle Horse should carry a rider for long trail days, stay willing and sure-footed over uneven country, travel at a smooth running walk, single-foot, rack, stepping pace, or another acceptable four-beat ambling gait, and do so without the hard bounce of a conventional trot. Under NSSHA rules, a fully registered horse cannot trot as its qualifying gait; the natural gait requirement is as central to the breed as the color requirement. In mod terms, its speed should be good for exploration and trail travel, its health should be high, and its jumping should remain moderate: this is a comfort-and-distance saddle horse, not a racehorse or purpose-bred jumper. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

Spotted Saddle Horses generally stand **14.3–16 hands** and weigh about **900–1,100 lb**, though the NSSHA allows horses as short as **13.3 hands**. Their build is medium-sized, smooth, and athletic rather than heavily muscled like a halter Quarter Horse or high-set like a Saddlebred. They usually have a refined, sensible head; long, clean neck; sloping shoulder; moderate withers; strong back; rounded hindquarters; clean legs; and no heavy feather. Mane and tail are ordinarily full and practical. They should look like generous, easy-moving trail horses—substantial enough for adult riders but built for comfort rather than mass. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

A pure registered Spotted Saddle Horse must be visibly **spotted**: white plus another recognized horse color, with qualifying white above the hock or knee and at least one sufficiently large qualifying patch. Facial markings and tall stockings alone do not count. Tobiano and overo are the two broad pattern groups most often named, while “overo” in practical registry language can encompass genetically distinct frame, splash, sabino, and similar non-tobiano white-pattern phenotypes. A horse may be lightly marked or nearly all white, but it must have pinto body spotting; a solid-colored foal from registered parents may enter a separate solid-color breeding-stock registry rather than the fully spotted register. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

In Procedural Horse Genetics, the breed should feel visually joyful and mechanically comfortable: broad patches, loud tobiano, frame-overo, sabino, splash, and tovero combinations over any normal base color, carried by a sturdy medium saddle horse. Every ordinary pure founder should be patterned, while a small optional **Solid Registry strain** can supply genetically appropriate solid breeding stock without pretending it is a fully registered Spotted Saddle Horse. The mod does not model the running walk, single-foot, rack, stepping pace, gait quality, head nod, rider comfort, the registry’s exact spot-measurement line, underlying pink skin, registration paperwork, show divisions, trail training, or the distinction between a solid registry animal and an NSSHA/SSHBEA fully spotted animal beyond flavour and founder-strain tagging. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the provided Anglo-Arabian example. Before compiling, reconcile exact field names, locus IDs, allele labels, strain format, source values, `Commonness` enum values, price units, and disease linkage behavior with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The NSSHA and SSHBEA establish that spotted horses must be gaited and show qualifying body white, but they do not publish a representative breed-wide genotype survey for tobiano, frame, Sabino 1, splash, cream, gray, or other coat loci. The numeric coat rates below are therefore transparent **gameplay approximations**, chosen to make ordinary founders reliably pinto while retaining varied genetic routes to the phenotype. `frame_overo` is deliberately lower than tobiano because `EDNRB` homozygotes are associated with lethal white syndrome. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

```json
{
  "id": "spotted_saddle_horse",
  "name": "Spotted Saddle Horse",
  "type": "natural",
  "notes": "The Spotted Saddle Horse is defined by a naturally inherited non-trotting four-beat gait plus qualifying pinto body spotting under NSSHA or SSHBEA registry rules. Procedural Horse Genetics does not model gait mechanics, running walk, single-foot, rack, stepping pace, head nod, rider comfort, exact registry spot measurements, the required two-inch qualifying patch, pink skin beneath white, gait inspection, performance divisions, trail training, show-ring quality, or the legal distinction between full spotted registration and solid-colored breeding-stock registration.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest",
    "minecraft:windswept_forest",
    "minecraft:windswept_hills",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 820,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.66,
      "e": 0.34
    },
    "agouti": {
      "A": 0.61,
      "a": 0.39
    },

    "cream": {
      "N": 0.88,
      "Cr": 0.12
    },
    "champagne": {
      "N": 0.97,
      "Ch": 0.03
    },
    "dun": {
      "N": 0.93,
      "D": 0.07
    },
    "silver": {
      "N": 0.97,
      "Z": 0.03
    },
    "grey": {
      "N": 0.94,
      "G": 0.06
    },
    "pearl": {
      "N": 0.99,
      "Prl": 0.01
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.92,
      "f": 0.08
    },

    "tobiano": {
      "N": 0.42,
      "TO": 0.58
    },
    "frame_overo": {
      "N": 0.93,
      "O": 0.07
    },
    "sabino_1": {
      "N": 0.74,
      "SB1": 0.26
    },
    "splash_white_1": {
      "N": 0.88,
      "SW1": 0.12
    },
    "splash_white_2": {
      "N": 0.98,
      "SW2": 0.02
    },
    "kit_white_spotting": {
      "N": 0.98,
      "W": 0.02
    },
    "roan": {
      "N": 0.93,
      "Rn": 0.07
    },
    "rabicano": {
      "N": 0.94,
      "Rb": 0.06
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
      1.00,
      1.12
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "spotted_registry",
      "name": "Spotted Registry",
      "weight": 0.90,
      "notes": "Ordinary fully spotted Spotted Saddle Horse founders. This strain must inherit at least one qualifying pinto-pattern allele and should visibly render a white body patch above the knee or hock rather than only facial white or stockings.",
      "required_any_of": [
        "tobiano",
        "frame_overo",
        "sabino_1",
        "splash_white_1",
        "splash_white_2",
        "kit_white_spotting"
      ],
      "coat_genes": {
        "tobiano": {
          "N": 0.32,
          "TO": 0.68
        },
        "frame_overo": {
          "N": 0.92,
          "O": 0.08
        },
        "sabino_1": {
          "N": 0.68,
          "SB1": 0.32
        },
        "splash_white_1": {
          "N": 0.84,
          "SW1": 0.16
        }
      }
    },
    {
      "id": "solid_breeding_stock",
      "name": "Solid Registry",
      "weight": 0.10,
      "notes": "A small founder pool for the NSSHA-style solid-color breeding-stock registry: gaited, lineage-appropriate, and solid-bodied. These animals should not qualify as fully spotted in a strict registry interpretation, but preserve the realistic possibility of solid foals and breeding stock.",
      "required_all_of": [
        "tobiano:N",
        "frame_overo:N",
        "sabino_1:N",
        "splash_white_1:N",
        "splash_white_2:N",
        "kit_white_spotting:N"
      ],
      "coat_genes": {
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
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Spotted Saddle Horse × Spotted Saddle Horse produces Spotted Saddle Horse, including a solid-colored foal from two pure breed labels. Spotted Saddle Horse × another pure breed produces a Spotted Saddle Horse cross. A Spotted Saddle Horse cross bred back to pure Spotted Saddle Horse remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Registry phenotype may differ from lineage: a genetically solid pure-lineage foal would be solid breeding stock in real registry terms, not a fully spotted horse."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed-defining rule | 90% Spotted Registry strain plus 10% Solid Registry strain | Fully registered Spotted Saddle Horses must be both gaited and visibly pinto; solid-colored, gaited offspring can be registered separately as breeding stock. A mostly spotted founder population with a small solid registry strain represents both realities without pretending solid horses are the ordinary breed phenotype.  [horseillustrated](https://www.horseillustrated.com/breed-spotted-saddle-horse/) |
| Tobiano | `TO: 0.58` overall; `TO: 0.68` in spotted strain | Tobiano is one of the two most frequently named broad pattern groups and is dominant. It should be the principal genetic route to reliably large qualifying body patches.  [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse) |
| Frame overo | `O: 0.07` overall | Frame is a relevant genetic contributor to “overo” phenotype, but it must remain lower-frequency because `O/O` causes lethal white syndrome. If the engine derives lethal white from `EDNRB`, do not duplicate it as a standalone `MET` disease entry.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6001536/) |
| Sabino 1 | `SB1: 0.26` overall | Sabino is named as a qualifying pattern by breed references and can create high leg white, broad face white, belly spots, roaned margins, and more extensive body white. It is genetically distinct from roan.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6001536/) |
| Splash white | `SW1: 0.12`; `SW2: 0.02` | Splash is a genetically plausible source of bold lower-body white, broad facial white, and blue eyes in an overo-category pinto breed. It is kept below tobiano and sabino because registry sources emphasize tobiano/overo broadly rather than a specific splash-heavy population.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6001536/) |
| KIT white alleles | `W: 0.02` | Very low optional white-pattern contribution. Retain only if the mod’s generic `KIT` white representation produces viable pinto-compatible results rather than all-white or otherwise registry-inappropriate outcomes. |
| Tovero | Emergent phenotype | Tovero must arise from combined tobiano and overo-class loci, not from a separate invented “tovero” locus.  [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse) |
| Base colors | `E: 0.66 / e: 0.34`; `A: 0.61 / a: 0.39` | Allows any ordinary equine base color beneath the required pinto pattern—bay, black, brown, chestnut, and sorrel. Values are gameplay approximations because no breed-wide base-color genotyping survey was located.  [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse) |
| Dilutions | Cream, champagne, dun, silver, gray, pearl, flaxen present at low/moderate rates | Registry language accepts white plus “any recognized horse color,” and breed descriptions say the color spectrum can include nearly all horse hues. The rates keep the visual identity in pinto pattern rather than allowing a single dilution to dominate.  [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse) |
| Roan and rabicano | `Rn: 0.07`; `Rb: 0.06` | Low modifier rates add diversity, but must not be confused with sabino or pinto. They are gameplay approximations, not measured breed frequencies. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | The registry identity is pinto, not Appaloosa-type leopard spotting. A horse may be dramatically white through pinto loci without adding leopard genetics. |
| Disorders | All standalone requested disorder loci clear | No defensible Spotted Saddle Horse-specific carrier frequency was found for ACAN, PLOD1, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. The only required medical logic is frame-linked lethal white through the existing `EDNRB` locus. |
| LP/CSNB caution | `LP` omitted | Some related gaited/spotted populations may have Appaloosa ancestry and `LP`-associated congenital stationary night blindness has been discussed for the breed, but the Spotted Saddle Horse registry definition is pinto-based and no scientific basis was found to seed `LP` in pure founders.  [madbarn](https://madbarn.com/spotted-saddle-horse-breed-profile/) |
| Speed | `6/10` | Represents efficient, comfortable all-day trail travel rather than specialist sprint speed. |
| Jump | `4/10` | Practical athleticism, not purpose-bred jumping selection. |
| Health | `8/10` | Represents a generally robust, trail-oriented saddle horse; it does not negate the medical consequence of `O/O` frame offspring or normal equine health risks. |
| Size | `×1.00–1.12` | Produces mature horses centered in the stated 14.3–16-hand ideal while allowing the lower registered 13.3-hand boundary through individual variation.  [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse) |

## Disorder approach

The pure Spotted Saddle Horse pool should have **no independently seeded named disorder alleles** from the requested panel. The evidence supports pattern genetics and registry requirements, not numerical breed-specific disease prevalence.

### Frame-linked lethal white

The critical exception is **frame overo**:

- Frame is a white-pattern allele at `EDNRB`.
- `N/O` horses are viable and may express frame-overo spotting.
- `O/O` foals are affected by lethal white syndrome and are nonviable without intensive intervention.
- This must be handled by the existing frame/`EDNRB`/`MET` logic in the genetics engine rather than as a second, independent disorder roll. [madbarn](https://madbarn.com/spotted-saddle-horse-breed-profile/)

### Why PSSM1, HYPP, and others stay clear

The breed has ancestry connections to southern gaited and stock-horse populations, but ancestry is not a carrier-frequency dataset. Do not seed `GYS1` PSSM1, `SCN4A` HYPP, `GBE1` GBED, `PPIB` HERDA, or any other disease simply because a possible foundation breed carries it. A future peer-reviewed Spotted Saddle Horse screening study could justify a revision; until then, clear founders are the more scientifically honest implementation.

### Solid founders are not a disease state

The **Solid Registry** strain is not a “failed” spotted horse and should not receive a health penalty. It represents gaited, lineage-appropriate breeding stock that lacks a qualifying pinto phenotype, exactly as the NSSHA’s solid-color registry recognizes. [nssha](https://nssha.com/registration/)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `spotted_saddle_horse` as a natural breed record with southeastern U.S. gaited-trail flavour, coat pools, founder strains, stat targets, and registry caveats. |
| `common/breed/Breeds` | Register the unique breed ID for wild spawning, stable/cowboy acquisition, breed books, H-menu display, commands, saved-genome loading, and lineage labels. |
| `common/breed/BreedSource` | Validate the requested `wild`, `cowboy`, `spawn_egg`, and `stable` sources. For strict real-world realism, `wild` can be removed because the breed is registry-managed; it is retained for gameplay accessibility. |
| `common/breed/BreedBands` | Support an empty epigenetic-band map. Do not use bands to fake gait, pinto coverage, or registry eligibility; these depend on genotype and rendering. |
| `common/breed/spec/` | Verify support for `required_any_of`, `required_all_of`, and strain-specific gene overrides. If unavailable, enforce a qualifying-pinto retry rule inside `BreedFounder` rather than storing those illustrative fields. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the appropriate rarity ladder and that direct `spawn_weight: 3` is valid. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 4, health 8, and size ×1.00–1.12 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll the founder strain first. For Spotted Registry, reroll or reject genomes without a qualifying body-white pattern. For Solid Registry, force all named body-white pinto loci wild type. Apply all other breed-wide coat pools normally. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed behavior. A solid offspring of two pure Spotted Saddle Horse labels remains pure lineage, even though real registry treatment would place it in solid breeding stock. |
| `common/genetics/SpliceOutcome` | No special exception. A successfully inherited splice allele can create a nonstandard descendant; it may violate real registry color rules but should follow normal genetic logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize medium-sized, good-travel-speed, high-heartiness, modest-jump targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the concrete breed-file schema requires an explicit default. |

## Verification

1. Confirm **Spotted Saddle Horse** appears in the H-menu’s Breeds tab and breed book with the correct name, southeastern trail identity, gait-and-spotting requirements, 14.3–16-hand center, source checklist, and Solid Registry explanation.

2. Spawn repeated eligible packs in meadow, plains, woodland, hill, and river-valley biomes. Every horse created in a selected pack must display **Spotted Saddle Horse** as its breed label.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is tobiano, overo, sabino, or found in a forested hill biome.

4. Generate at least 500 pure founders:
- Roughly 90% should visibly display pinto body spotting beyond face/legs, with one or more white patches above the knee or hock.
- Tobiano should be the most frequent single route.
- Frame, sabino, splash, and their combinations should create varied overo/tovero-like outcomes.
- About 10% may be solid-bodied, matching the optional Solid Registry strain.
- Base coats and modest dilutions should remain varied beneath the white pattern.

5. Confirm that full-pattern founders do not generate leopard complex, blanket spotting, snowflake, PATN-dependent Appaloosa traits, mushroom, brindle, or magical coats.

6. Inspect genomes:
- Spotted Registry founders must have at least one of `TO`, `O`, `SB1`, `SW1`, `SW2`, or an allowed `KIT` white allele.
- Solid Registry founders must be wild type at all required pinto loci.
- A visually insufficient genotype must be rerolled if the renderer cannot produce the registry-required body patch.
- Base and dilution loci should remain independent of pattern inheritance.

7. Test pinto inheritance:
- `TO/N × N/N` should transmit tobiano to approximately half of foals.
- `O/N × N/N`, `SB1/N × N/N`, and `SW1/N × N/N` should each transmit their respective allele to approximately half of foals.
- Tobiano combined with frame, sabino, or splash should yield tovero/multi-pattern phenotypes according to the renderer.
- Do not model “overo” as one gene; inspect the actual locus combination.

8. Test frame-linked lethal white only if the engine links `EDNRB` correctly:
- `O/N × O/N` should produce approximately 25% `O/O`, 50% `O/N`, and 25% `N/N`.
- `O/O` must follow the mod’s single lethal-white outcome.
- There must be no duplicate independent `MET` disorder roll.

9. Test default lineage:
- Spotted Saddle Horse × Spotted Saddle Horse → Spotted Saddle Horse.
- A solid offspring of two pure labels remains Spotted Saddle Horse lineage, even if registry flavour calls it solid breeding stock.
- Spotted Saddle Horse × Tennessee Walking Horse → Spotted Saddle Horse cross.
- Spotted Saddle Horse × American Paint Horse → Spotted Saddle Horse cross.
- Spotted Saddle Horse cross × pure Spotted Saddle Horse → the existing cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Check gameplay feel. Mature horses should be medium-sized, durable, good long-distance mounts with moderate jumping. Their unique real-world advantage—smooth gait and rider comfort—must remain flavour text or future gait-engine logic rather than an invented coat, health, or speed multiplier.

## Sources

- [National Spotted Saddle Horse Association — Registration](https://nssha.com/registration/): official qualifying requirements for pinto body spotting, white above the hock/knee, a two-inch qualifying patch, required non-trotting gait, and the separate solid-color breeding-stock registry. [nssha](https://nssha.com/registration/)

- [Spotted Saddle Horse breed profile](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse): supplementary synthesis of 1979 NSSHA formation, 13.3–16-hand registered range, 14.3–16-hand ideal, 900–1,100-lb weight, mandatory pinto color, broad overo/tobiano classification, and non-trotting gait requirement. [en.wikipedia](https://en.wikipedia.org/wiki/Spotted_Saddle_Horse)

- [Horse Illustrated — Breed Portrait: Spotted Saddle Horse](https://www.horseillustrated.com/breed-spotted-saddle-horse/): height, medium saddle-horse type, tobiano/overo/sabino qualification, solid breeding-stock status, and four-beat gait requirement. [horseillustrated](https://www.horseillustrated.com/breed-spotted-saddle-horse/)

- [MadBarn — Spotted Saddle Horse Breed Guide](https://madbarn.com/spotted-saddle-horse-breed-profile/): practical summary of size, pattern criteria, foundation influences, likely health considerations, and the distinction between pinto spotting and `LP`/CSNB discussion. [madbarn](https://madbarn.com/spotted-saddle-horse-breed-profile/)

- [Sabino1 and splashed-white coat patterns](https://pmc.ncbi.nlm.nih.gov/articles/PMC6001536/): peer-reviewed explanation of the distinct genetic roles of `KIT`/Sabino 1, `MITF`, `PAX3`, and `EDNRB` in horse white-pattern phenotypes. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC6001536/)