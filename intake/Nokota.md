The **Nokota** is best modeled as a rare North Dakota conservation breed from the Little Missouri Badlands: angular, strong, sure-footed, and unusually rich in blue roan, black, gray, frame overo, sabino, and occasional dun/grullo genetics. It should have two explicit strains—smaller, refined **National Park Traditional** horses and larger, ranch-influenced **National Park Ranch** horses—because the Nokota Horse Conservancy recognizes both as meaningful types within the breed. [nokotahorse](https://www.nokotahorse.org/projecto-3)

## Identity & flavour

The **Nokota horse**, usually called the **Nokota**, is an American conservation breed rooted in the Little Missouri River Badlands of western North Dakota. Its name combines “North Dakota,” and it refers to the descendants of feral horses preserved in and around what became Theodore Roosevelt National Park. The modern registered breed emerged in the late twentieth century, especially after the Kuntz family acquired horses removed from the park in the 1980s and the **Nokota Horse Conservancy** was organized in 1999 to preserve and register them. [nokotahorse](https://www.nokotahorse.org/projecto-3)

Nokotas were shaped by badlands country rather than by a formal show-ring purpose: dry grassland, steep draws, broken hills, wind, harsh winters, sparse cover, and long distances between water and forage. Their ancestry is complex and debated, encompassing horses associated with Indigenous peoples of the northern plains, nineteenth-century ranch herds, and horses that became isolated in the park. They should not be treated as untouched “pure Spanish mustangs” or as direct replicas of any single Lakota herd; they are a historically distinct feral-derived population with conservation value and a managed modern registry. [nokotahorse](https://www.nokotahorse.org/projecto-3)

The breed’s silhouette is leaner and more angular than a stock horse. A Nokota commonly has prominent withers, a long or slightly arched neck, a narrow but expressive head, sloping shoulder, strong though often somewhat sloping croup, low-set tail, and clean, durable legs. The **Traditional** type is smaller and more refined, typically about **14–15 hands**, with concentrated Colonial Spanish-type traits. The **Ranch** type reflects early foundation-style ranch/Quarter Horse influence and can be more substantial, approximately **14.2–17 hands**. In either type, the breed should read as a tough plains saddle horse—not a bulky modern halter Quarter Horse and not a tiny primitive pony. [nokotahorse](https://www.nokotahorse.org/projecto-3)

The Nokota’s visual hallmark is **blue roan**: black base coat mixed with the dominant roan allele, giving a smoky blue-gray body while preserving darker head and points. Black and gray are also common; red roan, bay, brown, and chestnut occur; and a few lines produce dun or grullo, sometimes with pronounced primitive leg and wither striping. The Nokota Horse Conservancy explicitly identifies frame overo and sabino—including blue-eyed individuals—as characteristic; tobiano, however, is described as occurring only through crossbreeding and must be excluded from pure Nokota founders. This is therefore a breed where roan, frame, and sabino are not accidental decoration: they are part of the recognizable breed picture. [nokotahorse](https://www.nokotahorse.org/projecto-3)

Nokotas are intelligent, alert, tough, and athletic, with the stamina and sure-footedness to cover badlands terrain. Some show an ambling gait often called the **Indian shuffle**, but that gait is not universal and should remain flavour rather than a forced gameplay gene. In Procedural Horse Genetics, players should breed toward an angular, dark-headed blue roan horse with a low tail, clean legs, and a little wild-country attitude—or pursue a black, gray, frame-overo, sabino, or rare grullo family without leaving real Nokota color space. The mod does not model the Indian shuffle, precise head and croup shape, low tail set, blue eyes, hoof toughness, feral behavior, badlands navigation, actual park-removal pedigree, registry inspection, historical ancestry disputes, or the difference between the Conservancy’s Traditional and Ranch types beyond the supplied founder strains. [nokotahorse](https://www.nokotahorse.org/projecto-3)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied in the prompt could not be retrieved by the documentation fetcher. This uses the readable JSON convention in the provided Anglo-Arabian example. Before merging, align actual field names, source enums, commonness values, locus IDs, allele symbols, strain representation, and stat-target syntax with `common/breed/spec/`.
>
> **Scientific-rate caveat:** The Nokota Horse Conservancy provides strong qualitative support for the included loci—especially roan, black, gray, frame overo, sabino, and occasional dun/grullo—but I did not locate a representative peer-reviewed genotype-frequency study that justifies precise population allele frequencies. The numeric rates below are clearly marked **gameplay approximations**, calibrated to yield blue roan as the breed’s visible hallmark without falsely claiming registry-measured allele statistics. `frame_overo` must remain low because the homozygous `EDNRB` genotype is associated with lethal white syndrome. [nokotahorse](https://www.nokotahorse.org/projecto-3)

```json
{
  "id": "nokota",
  "name": "Nokota",
  "type": "natural",
  "notes": "The Nokota is a North Dakota conservation breed defined by descent from preserved feral Badlands horses, its two registry-recognized Traditional and Ranch types, angular Colonial-Spanish-influenced conformation, low tail set, tough feet, alert character, and occasional Indian-shuffle gait. Procedural Horse Genetics does not model park-removal pedigree, registry eligibility, disputed historical ancestry, exact head profile, wither prominence, croup angle, low tail set, blue eyes, hoof quality, feral social behavior, badlands navigation, cattle sense, gait mechanics, or show-ring inspection.",

  "biomes": [
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:eroded_badlands",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 840,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.82,
      "e": 0.18
    },
    "agouti": {
      "A": 0.42,
      "a": 0.58
    },

    "roan": {
      "N": 0.55,
      "Rn": 0.45
    },
    "grey": {
      "N": 0.78,
      "G": 0.22
    },
    "dun": {
      "N": 0.90,
      "D": 0.10
    },
    "cream": {
      "N": 0.97,
      "Cr": 0.03
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
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.95,
      "f": 0.05
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.88,
      "SB1": 0.12
    },
    "frame_overo": {
      "N": 0.94,
      "O": 0.06
    },
    "splash_white_1": {
      "N": 0.98,
      "SW1": 0.02
    },
    "splash_white_2": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 1.0
    },
    "rabicano": {
      "N": 0.95,
      "Rb": 0.05
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
    "jump": 5,
    "health": 8,
    "size": [
      0.96,
      1.13
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "traditional",
      "name": "National Park Traditional",
      "weight": 0.70,
      "notes": "Smaller, more refined Traditional Nokota type with concentrated Colonial-Spanish-influenced conformation. It should remain within the same core color pool but lean slightly more toward blue-roan, black, gray, and dun/grullo than the Ranch strain.",
      "coat_genes": {
        "extension": {
          "E": 0.86,
          "e": 0.14
        },
        "agouti": {
          "A": 0.36,
          "a": 0.64
        },
        "roan": {
          "N": 0.48,
          "Rn": 0.52
        },
        "grey": {
          "N": 0.76,
          "G": 0.24
        },
        "dun": {
          "N": 0.86,
          "D": 0.14
        },
        "sabino_1": {
          "N": 0.90,
          "SB1": 0.10
        },
        "frame_overo": {
          "N": 0.95,
          "O": 0.05
        }
      },
      "stat_scores": {
        "speed": 6,
        "jump": 5,
        "health": 8,
        "size": [
          0.96,
          1.04
        ]
      }
    },
    {
      "id": "ranch",
      "name": "National Park Ranch",
      "weight": 0.30,
      "notes": "Larger, more substantial Ranch Nokota type, historically closer in outline to early foundation-type ranch and Quarter Horse stock. It retains the core Nokota color pool while allowing modestly more bay, chestnut, and body-size variation.",
      "coat_genes": {
        "extension": {
          "E": 0.75,
          "e": 0.25
        },
        "agouti": {
          "A": 0.56,
          "a": 0.44
        },
        "roan": {
          "N": 0.66,
          "Rn": 0.34
        },
        "grey": {
          "N": 0.84,
          "G": 0.16
        },
        "dun": {
          "N": 0.94,
          "D": 0.06
        },
        "sabino_1": {
          "N": 0.84,
          "SB1": 0.16
        },
        "frame_overo": {
          "N": 0.92,
          "O": 0.08
        }
      },
      "stat_scores": {
        "speed": 6,
        "jump": 5,
        "health": 8,
        "size": [
          1.01,
          1.13
        ]
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Nokota × Nokota produces Nokota regardless of Traditional or Ranch founder strain. Nokota × another pure breed produces a Nokota cross. A Nokota cross bred back to a pure Nokota remains that cross under the default system. Two different crosses produce Mixed, and any pairing with Feral Mixed produces Mixed. Strain identity is founder flavor only and should not replace the breed lineage label."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed structure | Two founder strains | The Nokota Horse Conservancy distinguishes a smaller refined Traditional type from a larger Ranch type. The strain split affects size and subtle founder-pool weighting while preserving one Nokota lineage label.  [en.wikipedia](https://en.wikipedia.org/wiki/Nokota_horse) |
| Base colors | `E: 0.82 / e: 0.18`; `A: 0.42 / a: 0.58` | Black is identified as common and is the base for the hallmark blue roan; bay, brown, and chestnut also occur. The bias toward `E` and `a` increases black-based blue roans without eliminating legitimate bay/red families. These rates are gameplay estimates.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Roan | `Rn: 0.45` overall | Blue roan is so frequent that the Conservancy calls it a breed hallmark. Roan must therefore be markedly more common than in most breed definitions. The allele remains below fixation so black, gray, bay, and non-roan members remain possible.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Gray | `G: 0.22` | The Conservancy identifies gray as another common Nokota color, so it belongs in the pure-founder pool at a substantial minority rate.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Dun | `D: 0.10` | A few lines produce dun and grullo horses, sometimes with pronounced leg and wither striping. It should be uncommon but real, rather than excluded.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Cream | `Cr: 0.03` | Palomino is reported as less common in broader Nokota descriptions. A low rate permits it without turning the breed into a cream-dilution population.  [en.wikipedia](https://en.wikipedia.org/wiki/Nokota_horse) |
| Tobiano | Forced wild type | The Conservancy states that tobiano patterns occur only with crossbreeding. A pure Nokota founder must never introduce `TO`.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Frame overo | `O: 0.06` | Frame overo is directly described as characteristic, including irregular body patches, bold face white, blue eyes, and occasional medicine-hat patterns. The low rate preserves it while limiting `O/O` lethal-white pairings.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Sabino | `SB1: 0.12` | Sabino is directly recognized as characteristic of Nokotas. The rate permits ordinary white-leg/facial sabino through higher expression while leaving most horses solid or roan.  [nokotahorse](https://www.nokotahorse.org/projecto-3) |
| Splash | `SW1: 0.02` | Blue eyes occur in the breed, but the Conservancy specifically emphasizes frame and sabino rather than a proven splash genotype. Retain only a minimal optional rate—or remove it if the mod’s blue-eye implementation is unsupported by actual locus data. |
| Rabicano | `Rb: 0.05` | A small rate supplies the occasional flank/tail white-hair texture described in supplementary summaries, without confusing it with blue roan. This is an approximate gameplay modifier rather than a measured Nokota frequency.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Nokota.php) |
| Excluded loci | Silver, champagne, pearl, mushroom, leopard complex, PATN, brindle, W-series white, magical loci forced wild type | The Conservancy-supported phenotype range does not justify seeding these loci in pure founders. |
| Disorders | All named standalone disorder loci clear | No defensible Nokota-specific carrier rate was found for ACAN, PLOD1, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Do not import Quarter Horse disease rates merely because the Ranch type has historical foundation-ranch influence. |
| Frame-linked lethal white | Use the coat-locus engine | `EDNRB`/frame is medically relevant because `O/O` causes lethal white syndrome. If the mod already derives that outcome from the `frame_overo` locus, do not duplicate it as a separate `MET` disorder entry. |
| Speed | `6/10` | Nokotas should cover ground capably over uneven terrain, but they are not a specialist race breed. |
| Jump | `5/10` | Their athleticism and badlands agility support a baseline-to-good jump score, without pretending they are purpose-bred jumpers. |
| Health | `8/10` | Represents feral survival, tough feet, climate resilience, and general soundness—not immunity from injury, low genetic diversity, or any untested disorder.  [ndtourism](https://www.ndtourism.com/linton/historic-sites-forts/western-culture/nokota-horse-conservancyr) |
| Size | `×0.96–1.13` | Captures Traditional founders around 14–15 hands and allows the larger Ranch type to extend higher.  [en.wikipedia](https://en.wikipedia.org/wiki/Nokota_horse) |

## Disorder approach

The pure Nokota file should carry **no separately seeded disorder alleles** from the requested panel. No credible Nokota-specific carrier prevalence was found for ACAN dwarfism, PLOD1 Friesian dwarfism, Arabian-line SCID/CA/LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

Two coat-linked medical rules still matter:

- **Frame overo** should remain a real Nokota coat allele because the Conservancy identifies it as characteristic. If the mod maps the frame allele to the appropriate `EDNRB`/MET lethal-white outcome, two heterozygous frame parents can produce the expected lethal homozygous foal outcome. Do not create a duplicate, independent `MET` disease roll.
- **Roan** should not be treated as lethal or embryonic because ordinary equine roan is a viable dominant coat-pattern locus. The old “roan homozygote is lethal” claim is not an appropriate rule for this breed file.

If the codebase does not connect `frame_overo` to its homozygous lethal consequence, document that as an engine limitation rather than fabricating a duplicate disease entry in the breed JSON.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `nokota` natural-breed record with conservation notes, Badlands biome mapping, rarity, sources, broad but characteristic coat pool, founder strains, and stat targets. |
| `common/breed/Breeds` | Register `nokota` so it loads in wild spawning, breeding, commands, lineage displays, books, menus, and saved genomes. |
| `common/breed/BreedSource` | Validate all four desired sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Support the empty epigenetic band object. Do not attempt to represent roan seasonal contrast, body conformation, blue eyes, or gait with unrelated epigenetic ranges. |
| `common/breed/spec/` | Reconcile this illustrative JSON with the actual parser/writer—especially strain syntax, inheritance pool override rules, `Rn`, `O`, `SB1`, and `D` allele labels, and default behavior for omitted loci. |
| `common/breed/Commonness` | Confirm `RARE` maps to the appropriate requested weight ladder; retain `spawn_weight: 1.5` if direct weighted spawning is supported. |
| `common/breed/BreedStatCurve` | Map speed 6, jump 5, health 8, and both strain size bands into valid target bands. |
| `common/breed/BreedFounder` | First choose Traditional or Ranch using the 70:30 strain weights; then roll only from that strain’s gene pool and body-target bands while inheriting all unspecified breed-wide wild-type settings. |
| `common/breed/BreedLineage` | Keep the default pure/cross/Mixed table. A Traditional × Ranch Nokota foal remains Nokota, because the strains are internal population types rather than distinct breeds. |
| `common/genetics/SpliceOutcome` | No Nokota exception. A spliced allele should transmit under normal mechanics and may create a nonstandard descendant such as tobiano, leopard spotting, champagne, or an unsupported disease allele. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Ensure the moderate athletic and two-band size targets serialize and generate correctly. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed; do not confuse the Conservancy’s “Traditional Nokota” population designation with the code’s `BandType.TRADITIONAL` enum. |

## Verification

1. Confirm **Nokota** appears in the H-menu’s Breeds tab and breed book with the North Dakota Badlands conservation history, rare commonness, complete source checklist, two strain descriptions, and blue-roan-focused genetics.

2. Spawn wild packs in badlands, wooded badlands, eroded badlands, windswept hills, dry plains, and river-edge environments. Every member of one generated breed pack must show the **Nokota** label, even when a Traditional and Ranch founder render differently.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is a blue roan, grullo, medicine-hat overo, or standing in a badlands biome.

4. Generate at least 500 pure Nokota founders. The breed should visibly center on blue roan and black, with substantial gray; lesser bay, brown, chestnut, red roan, dun, and grullo; and occasional frame overo/sabino horses. The sample should contain no tobiano.

5. Inspect genomes:
- `Rn` must be visibly much more frequent than in ordinary breeds.
- `E` and `a` must combine often enough to make black-based blue roan common.
- `G`, `D`, `SB1`, and `O` may vary only at the listed minority rates.
- `TO`, `LP`, `PATN1`, `PATN2`, silver, champagne, pearl, mushroom, brindle, W-series white, and magical loci must be wild type.

6. Test strain behavior:
- About 70% of generated founders should be Traditional and fall in the smaller `×0.96–1.04` size band.
- About 30% should be Ranch and fall in the larger `×1.01–1.13` band.
- Both strains retain the Nokota lineage label.
- Traditional × Ranch offspring must remain Nokota, not a cross or Mixed horse.

7. Test frame inheritance only if the engine links the `frame_overo` locus to lethal white:
- `O/N × N/N` should transmit frame to roughly half of foals.
- `O/N × O/N` should yield roughly 25% `O/O` conceptions/foals according to the mod’s lethal-white implementation.
- Do not add an independent `MET` roll on top of that result.

8. Confirm all standalone requested disorder genes remain clear across a large founder sample. No unverified disease allele should originate in pure Nokota founders.

9. Test default lineage:
- Nokota × Nokota → Nokota.
- Nokota × American Quarter Horse → Nokota cross.
- Nokota × Mustang → Nokota cross.
- Nokota cross × pure Nokota → existing Nokota cross.
- Two different crosses → Mixed.
- Any pairing with Feral Mixed → Mixed.

10. Check play feel. Traditional horses should feel slightly smaller and more refined; Ranch horses somewhat larger and more substantial; both should be tough, useful, medium-speed trail mounts. Neither type should mechanically eclipse dedicated racers, jumpers, draft horses, or specialist endurance breeds.

## Sources

- [Nokota Horse Conservancy — Nokota Type](https://www.nokotahorse.org/projecto-3): primary breed-conservancy source for Traditional versus Ranch types, 14–15-hand Traditional size, coat hallmarks, blue roan frequency, common black/gray, occasional dun/grullo, frame overo and sabino, and the explicit statement that tobiano arises only through crossbreeding. [nokotahorse](https://www.nokotahorse.org/projecto-3)

- [North Dakota Tourism — The Nokota Horse Conservancy](https://www.ndtourism.com/linton/historic-sites-forts/western-culture/nokota-horse-conservancyr): regional history of feral North Dakota plains horses and their links to nineteenth-century Lakota and ranch herds. [ndtourism](https://www.ndtourism.com/linton/historic-sites-forts/western-culture/nokota-horse-conservancyr)

- [MadBarn — Nokota Horse Breed Guide](https://madbarn.ca/nokota-horse-breed-profile/): supplementary current overview of Badlands origin, the 1986 park-sale event, Nokota Horse Conservancy formation, registration policy, conservation status, size, colors, and temperament. [madbarn](https://madbarn.com/nokota-horse-breed-profile/)

- [Nokota Horse reference overview](https://en.wikipedia.org/wiki/Nokota_horse): supplementary synthesis for angular conformation, height distinctions between Traditional and Ranch types, blue-roan hallmark, color range, low tail set, and Indian-shuffle reports. [en.wikipedia](https://en.wikipedia.org/wiki/Nokota_horse)