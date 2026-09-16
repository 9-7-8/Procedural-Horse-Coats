The **Novooleksandrivsky Heavy Draught**—also rendered **Novoalexandrovsk Heavy Draft**, **Novoolexandrian Draught**, or *Novooleksandrivska Vahovozna*—should be modeled as a compact Ukrainian draught horse: broad, muscular, calm, fertile, long-lived, and surprisingly agile for its size. It is a **light-to-medium heavy horse**, not a harness trotter: selected for agricultural traction, hauling, and reliable everyday work, with chestnut dominant and bay/brown plus occasional roan possible. [en.wikipedia](https://en.wikipedia.org/wiki/Novoolexandrian_Draught)

## Identity & flavour

The **Novooleksandrivsky Heavy Draught**, Ukrainian *Novooleksandrivska Vahovozna* (новоолександрівська ваговозна), is a Ukrainian breed of draught horse named for the **Novo-Oleksandrivka State Stud** in what is now Luhansk Oblast. English sources may call it the **Novoalexandrovsk Heavy Draft**, **Novoolexandrian Draught**, **New Olexandrian Heavy Draft**, or simply the Novooleksandrivsky. It shares early history with the Russian Heavy Draft and the nineteenth-century **Russian Ardennes** population, but later breeding in Ukraine created a distinct national type that was recognized by Soviet agricultural authorities in 1970 and formally recognized by Ukraine’s Ministry of Agrarian Policy in November 1999. [en.wikipedia](https://en.wikipedia.org/wiki/Novoolexandrian_Draught)

The breed was made for practical traction. Local Ukrainian mares were crossed principally with **Ardennes** stock, with lesser Brabant/Belgian Draft and Percheron influence, then selected for farm work, wagon and carriage pulling, transport, forestry, and the steady hauling demands of smallholder agriculture. It was valued not merely for strength, but for being comparatively compact, easy to keep, fertile, long-lived, energetic under harness, and able to work across fields, roads, and village terrain. A Novooleksandrivsky should feel like the survival player’s reliable pulling horse: not enormous and slow like a fantasy giant draft, but short-legged, powerful, broad-backed, and made for steady output. [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/)

This is a compact draught horse, generally about **150–155 cm** at the withers, roughly **14.3–15.1 hands**, with some individuals reaching toward 16 hands. It has a medium, clean head with a straight or slightly convex profile; a short, broad, muscular neck that may be heavily crested in stallions; a deep, broad chest; relatively low or only slightly prominent withers; a long, broad back that may be slightly soft; strong loins; wide, powerful hindquarters; and short, sturdy limbs. Feathering is light to moderate rather than Shire-like, and the hooves are comparatively small but hard. A full mane and tail, often with generous forelock and fetlock hair, complete the practical cob-draught silhouette. [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/)

Chestnut is the defining color, with bay and brown also valid and less common. Some sources also report roan in the wider Russian Heavy Draft / Russian Ardennes-derived population; it is reasonable to retain a low true-roan allele in the Novooleksandrivsky file as a cautious historical-type possibility, but roan should remain distinctly uncommon. Pure founders should not produce gray, black as a major class, palomino, buckskin, dun, champagne, silver dapple, pinto, leopard complex, or magical coats. In game, the typical animal should be a deep chestnut, often with a light mane and tail from chestnut shading or flaxen-like expression, or a sturdy bay/brown work horse. [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/)

Novooleksandrivsky horses are known for calmness, willingness, strength, endurance, good fertility, and longevity. Their value is cultural as well as practical: they preserve a Ukrainian heavy-horse lineage shaped by state studs, rural labor, and regional breeding knowledge during a period when many working breeds were displaced by mechanization. In Procedural Horse Genetics, players should breed toward a compact chestnut power plant—a sturdy horse with short legs, a broad body, strong health, moderate speed, and very little decorative color variation. The mod does not model traction force, harness behavior, fertility, milk production, hoof hardness, feathering, mature muscle, load-pulling efficiency, stocky head/neck proportions, crop work, forestry work, actual regional pedigree, or the distinction between Novooleksandrivsky and the related Russian Heavy Draft. [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the user-provided example. Before compiling, reconcile actual field names, source enums, locus IDs, allele symbols, commonness values, price units, and body-stat-band syntax with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The available sources support the breed’s Ukrainian status, Russian-Ardennes/Ardennes ancestry, 150–155 cm compact draught type, chestnut-dominant palette, bay/brown minority, and possible roan occurrence in the related historical population. They do **not** provide a representative Novooleksandrivsky coat-genotype survey. The numerical `E/e`, `A/a`, roan, and flaxen rates below are transparent **gameplay approximations**, not official Ukrainian studbook allele frequencies. No credible breed-specific carrier rates were found for the requested disease panel, so those loci are all clear. [en.wikipedia](https://en.wikipedia.org/wiki/Novoolexandrian_Draught)

```json
{
  "id": "novooleksandrivsky_heavy_draught",
  "name": "Novooleksandrivsky Heavy Draught",
  "type": "natural",
  "notes": "The Novooleksandrivsky Heavy Draught, also called the Novoalexandrovsk Heavy Draft or Novoolexandrian Draught, is a Ukrainian compact draught horse defined by Ukrainian state-stud lineage, Ardennes and related heavy-horse ancestry, agricultural traction, harness work, short powerful limbs, broad chest, strong loins and hindquarters, hard hooves, light-to-moderate feathering, fertility, longevity, and calm working temperament. Procedural Horse Genetics does not model pulling force, harness action, crop work, forestry work, load efficiency, fertility, milk production, hoof quality, feathering, mane thickness, muscle mass, neck crest, detailed conformation, regional studbook pedigree, or the distinction from the related Russian Heavy Draft.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:taiga",
    "minecraft:grove",
    "minecraft:windswept_hills"
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
      "E": 0.55,
      "e": 0.45
    },
    "agouti": {
      "A": 0.70,
      "a": 0.30
    },

    "roan": {
      "N": 0.96,
      "Rn": 0.04
    },
    "flaxen": {
      "N": 0.82,
      "f": 0.18
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
    "dun": {
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
    "speed": 4,
    "jump": 3,
    "health": 9,
    "size": [
      1.02,
      1.13
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Novooleksandrivsky Heavy Draught × Novooleksandrivsky Heavy Draught produces Novooleksandrivsky Heavy Draught. Novooleksandrivsky Heavy Draught × another pure breed produces a Novooleksandrivsky Heavy Draught cross. A Novooleksandrivsky Heavy Draught cross bred back to pure Novooleksandrivsky Heavy Draught remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not identify a compact chestnut draught horse as Novooleksandrivsky from color or hauling stats alone; real identity depends on Ukrainian breed lineage and the official studbook."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed identity | Ukrainian compact heavy draught | Novooleksandrivsky is a distinct Ukrainian draught breed, named for the Novo-Oleksandrivka stud and descended from the Russian Ardennes/Russian Heavy Draft development with major Ardennes influence.  [en.wikipedia](https://en.wikipedia.org/wiki/Novoolexandrian_Draught) |
| Chestnut core | `E: 0.55 / e: 0.45` | A comparatively high `e` rate makes chestnut the most common visible phenotype, while still retaining bay and brown. Chestnut is the repeatedly identified dominant/common color in the related Russian-Ardennes-derived type. Exact values are gameplay estimates.  [breeds.okstate](https://breeds.okstate.edu/horses/russian-heavy-draft-horses) |
| Bay and brown | `A: 0.70 / a: 0.30` | Where black pigment is present, a high `A` rate makes bay/brown more common than pure black. This fits descriptions that list bay and brown as the principal non-chestnut colors.  [breeds.okstate](https://breeds.okstate.edu/horses/russian-heavy-draft-horses) |
| Black | Emerges rarely from `E_ aa` | Black is not seeded as a target color, but can arise at low frequency from the needed base-color genetics. If the actual official standard excludes black entirely, set `a: 0.0`; the available sources support chestnut/bay/brown but do not provide a sufficiently detailed pure-breed genotype standard to justify full fixation. |
| Roan | `Rn: 0.04` | Roan occurs in the related Russian Heavy Draft/Russian Ardennes-derived population. The low rate preserves it as a rare historical-type possibility instead of making roan a prominent breed hallmark. This is a cautious approximation.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Russian_Heavy_Draft.php) |
| Flaxen | `f: 0.18` | Many chestnut heavy horses can show light mane/tail contrast. A modest flaxen allowance adds believable chestnut variety, but it is not presented as a documented Novooleksandrivsky frequency. |
| Gray and dilutions | Gray, cream, pearl, champagne, silver, mushroom, and dun forced wild type | The documented practical palette centers on chestnut, bay, brown, and occasional roan. No strong evidence supports seeding these other dilution families in pure founders.  [breeds.okstate](https://breeds.okstate.edu/horses/russian-heavy-draft-horses) |
| White patterns | Tobiano, frame, sabino, splash, KIT white, rabicano forced wild type | No evidence supports a routine pinto or body-white founder population. Ordinary incidental markings do not justify named white-pattern loci. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type patterning does not belong in the documented Ukrainian heavy-draught color pool. |
| Magical loci | All forced wild type | The breed’s strength and ruggedness are real working-horse traits, not magical effects. |
| Disorders | All named loci clear | No defensible Novooleksandrivsky-specific carrier rate was located for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `4/10` | The breed is energetic and agile for a draught horse, but not bred for harness racing or high-speed riding.  [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/) |
| Jump | `3/10` | It is a traction horse, not a jump specialist. Short, stout limbs and draft conformation should translate to low jumping specialization. |
| Health | `9/10` | Represents long-lived, fertile, tough, easy-keeping working stock with strong endurance—not immunity from disease, injury, or management failure.  [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/) |
| Size | `×1.02–1.13` | This creates a broad, compact heavy-horse frame around 150–155 cm—larger and stronger than a standard riding horse in mass, without using giant-draft height.  [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/) |

## Disorder approach

Every named disorder locus is **clear** in the pure Novooleksandrivsky Heavy Draught founder pool.

This is not a claim that real Ukrainian draught horses are genetically immune to disease. It is a strict evidence decision. The retrieved sources document the breed’s origin, history, average height, compact light-draught form, endurance, temperament, longevity, and color range, but none supplied a population-level carrier frequency for the requested Mendelian disorders.

Do not import rates merely because the breed shares historical ancestry with Ardennes-, Belgian-, Percheron-, or Russian Heavy Draft-related horses. That would be especially misleading for:

- `SCN4A` HYPP, largely linked to particular Quarter Horse ancestry.
- `PPIB` HERDA and `GBE1` GBED, which are principally relevant in specific Quarter Horse-related populations.
- Arabian-line `PRKDC` SCID, `TOE1` CA, and `MYO5A` LFS.
- `PLOD1` WFFS, which requires a supported warmblood or breed-specific carrier dataset.
- `GYS1` PSSM1, which is dominant and must never be seeded as a casual “carrier” allele without breed data.

A later Ukrainian heavy-draught screening study could justify adding a locus, but the file should then identify the exact mutation, tested population, sample size, and whether the reported number is allele frequency or carrier prevalence.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `novooleksandrivsky_heavy_draught` as a natural Ukrainian compact-draught record with proper aliases in flavour, chestnut-focused coat pool, clear disorders, large working stats, and managed sources. |
| `common/breed/Breeds` | Register the breed ID for stable/cowboy acquisition, spawn eggs, breed books, H-menu display, command lookup, save loading, founder generation, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because this is a managed Ukrainian working breed, not a feral population. |
| `common/breed/BreedBands` | Support the empty epigenetic-band object. Do not use shade bands to imitate muscle, feathering, mane thickness, or draft strength. |
| `common/breed/spec/` | Map all illustrative JSON keys to the actual reader/writer. Confirm the correct `E/e`, `A/a`, `Rn`, and flaxen allele names, as well as defaults for omitted genes. |
| `common/breed/Commonness` | Confirm `RARE` maps to the requested rarity ladder and that `spawn_weight: 1.5` is a valid direct weight if the code supports it. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 3, health 9, and size ×1.02–1.13 into legal `TargetBand` records. |
| `common/breed/BreedFounder` | Roll Extension, Agouti, roan, and flaxen from the supplied pools; force unsupported coat genes and all requested disease loci wild type or clear; apply the compact heavy-work stat bands. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A chestnut compact draft horse must not acquire Novooleksandrivsky lineage from phenotype, biome, or body stats alone. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. Splice-carrot alleles transmit normally and may create nonstandard descendants with gray, pinto, leopard, dilution, or disease genetics excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the modest-speed, low-jump, high-heartiness, medium-heavy size targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the real serializer mandates an explicit default. |

## Verification

1. Confirm **Novooleksandrivsky Heavy Draught** appears in the H-menu’s Breeds tab and the breed book with its Ukrainian identity, Novo-Oleksandrivka history, Ardennes-derived compact draft role, rare commonness, managed-only sources, and chestnut-focused description.

2. Confirm this breed does **not** spawn in ordinary natural wild packs because `wild` is omitted. It should appear through cowboy, stable, and spawn-egg sources only.

3. Confirm an ordinary lone wild horse reads **Feral Mixed**, even if it is chestnut, low-set, broad, short-legged, or standing on plains/meadow terrain.

4. Generate at least 1,000 pure founders. The cohort should be dominated by chestnut, with lesser bay and brown. A small minority may be roan. Black should be uncommon, not a major founder class.

5. Confirm that pure founders never produce gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, tobiano, frame overo, splash, strong sabino body spotting, dominant white, leopard complex, brindle, or magical coats.

6. Inspect founder genomes:
- `e` should be common enough that chestnut is the dominant phenotype.
- `E` plus `A` should produce bay/brown minority horses.
- `E` plus `a/a` should make a rare black outcome.
- `Rn` should remain rare.
- `f` should affect mane/tail expression only where the mod’s flaxen system properly permits it.
- Every named pinto, leopard, major dilution, magical, and disorder locus must remain wild type or clear.

7. Confirm adult physical performance:
- Mature horses should be notably substantial and strong-looking, with a compact draught build.
- They should be slower than dedicated riding or racing horses but not unusably slow.
- Jump ability should be low.
- Health/heartiness should be high.
- They should remain far below giant-draft height even though they are modeled as strong work horses.

8. Confirm all named disorder loci remain clear across a large founder sample. No WFFS, HYPP, PSSM1, GBED, HERDA, SCID, CA, LFS, frame-related lethal white, or unrelated disease allele should originate in pure founders.

9. Test default lineage:
- Novooleksandrivsky Heavy Draught × Novooleksandrivsky Heavy Draught → Novooleksandrivsky Heavy Draught.
- Novooleksandrivsky Heavy Draught × Ardennes → Novooleksandrivsky Heavy Draught cross.
- Novooleksandrivsky Heavy Draught × Russian Heavy Draft → Novooleksandrivsky Heavy Draught cross.
- Novooleksandrivsky Heavy Draught cross × pure Novooleksandrivsky Heavy Draught → the existing cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add gray, cream, tobiano, leopard complex, giant-draft size, sprint speed, or a named disease allele. Descendants should inherit normally but should never be auto-labeled pure Novooleksandrivsky merely because they remain chestnut or heavy-bodied.

## Sources

- [Novoolexandrian Draught overview](https://en.wikipedia.org/wiki/Novoolexandrian_Draught): history of Russian Ardennes origin, Novo-Oleksandrivka association, 1970 Soviet recognition, 1999 Ukrainian recognition, and Ukrainian draught-breed identity. [en.wikipedia](https://en.wikipedia.org/wiki/Novoolexandrian_Draught)

- [Animal Genetic Resources — Ukraine](https://www.animalgeneticresources.net/index.php/country/ukraine/): Ukrainian breeding-history source describing the 1868 local-mare × Ardennes foundation, lesser Brabant and Percheron influence, Novoalexandrovsky stud’s role from 1917–1960, about 150 cm average height, compact draft form, endurance, balanced temperament, and longevity. [animalgeneticresources](https://www.animalgeneticresources.net/index.php/country/ukraine/)

- [SE “Ukraine Horse Breeding”](https://www.spfu.gov.ua/en/news/10296.html): contemporary Ukrainian state-sector context identifying the Novooleksandrivka heavy-duty breed as an important preserved working breed. [spfu.gov](https://www.spfu.gov.ua/en/news/10296.html)

- [Russian Heavy Draft Horses — Oklahoma State University](https://breeds.okstate.edu/horses/russian-heavy-draft-horses): related Russian-Ardennes-derived population context for chestnut predominance and bay/brown minority colors. [breeds.okstate](https://breeds.okstate.edu/horses/russian-heavy-draft-horses)

- [Russian Heavy Draft reference](https://hi3.horseisle.com/www/bbb/Russian_Heavy_Draft.php): supplementary historical and phenotype source for the Novoaleksandrov compact-draft type, 14.1–15-hand range, chestnut/bay/brown/roan colors, short strong limbs, light-to-moderate feather, and hardy work use. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Russian_Heavy_Draft.php)

- [The Russian draft horse](https://www.royal-horse.com/race/the-russian-draft-horse/): supplementary description of Ukrainian origin, Ardennes/Percheron/Orlov influence, calm energetic temperament, longevity, hard hooves, compact cob-like form, chestnut/bay colors, and approximately 155 cm height. [royal-horse](https://www.royal-horse.com/race/the-russian-draft-horse/)