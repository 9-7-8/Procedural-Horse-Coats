The **Anadolu**, more accurately **Anadolu Yerli** or **Anatolian Native Horse**, should be implemented as a compact, broad Turkish landrace rather than a narrowly fixed “Anatolian Pony.” It is a small, durable riding, pack, and light-draught horse from eastern and southeastern Anatolia, shaped by mountainous terrain, dry continental summers, snowy winters, village agriculture, and long-distance local travel. Pure founders should be mostly bay, black/brown, and chestnut, with a conservative allowance for gray and dun; pinto, leopard, champagne, silver, cream, and other visually dramatic genes should be absent unless introduced by outcrossing. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

## Identity & flavour

The **Anadolu**, also called the **Anatolian**, **Anadolu Yerli**, or Anatolian Native Horse, is a traditional Turkish horse population from Anatolia—the Asian part of Türkiye—with its strongest association in eastern and southeastern regions. The name means, simply, “Anatolian.” It belongs to the broader family of Turkish native horses, a collection of local populations shaped by geography, use, and historical movement between Europe, the Caucasus, Central Asia, and the Middle East. Unlike a modern closed registry bred to one narrow look, Anadolu horses are best understood as a practical indigenous landrace: many local horses sharing a recognizable small, tough, adaptable type. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

Anadolu horses were made for work close to home and travel far from it. They carried riders across rough tracks, packed supplies through hill country, pulled light loads, worked small farms, and supported village life where keeping a large, specialized horse was impractical. Their homeland includes harsh continental landscapes: dry plains, scrub, rocky uplands, cold winters, hot summers, uneven mountain paths, and irregular forage. The important qualities are thrift, sturdy feet, reliable movement, strength relative to size, and an ability to keep working without the feed demands of a large imported sport horse. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

Most Anadolu horses stand about **124–142 cm**, approximately **12.1–14 hands**. They are small true horses or large ponies by western height convention, but they should not feel toy-like. The head profile varies—from a slight dish through straight to convex—while the body is compact, deep enough for practical work, and carried on short, strong limbs. The neck may be short or medium, the withers relatively low, the croup somewhat sloping, and the hooves durable for stony ground. Mane and tail are ordinary but often thick enough for an outdoor mountain-and-steppe horse; there is no true draft feathering. In the mod, an Anadolu should read as a sensible Turkish village horse: broad enough to carry, short enough to conserve, and visibly built for terrain rather than show-ring exaggeration. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

The modern Anatolian Native population is not as color-restricted as Sorraia or Konik, but neither is it a rainbow color breed. Historical and modern Anatolian horses are commonly described as bay, black, chestnut, brown, gray, and dun, with white markings generally modest. Ancient DNA from Anatolia demonstrates that domestic horses in the region acquired diverse coat variants—including bay, black, chestnut, silver, leopard, tobiano, and sabino—over thousands of years; however, that archaeological diversity must not be confused with evidence that every modern Anadolu founder should carry every one of those alleles. For a conservative contemporary Anadolu file, ordinary base colors form the core, gray and true dun are minor but legitimate, and loud modern pinto/leopard patterns remain wild type. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

Anadolu horses should feel alert, willing, tough, and self-reliant. In Procedural Horse Genetics, players should breed toward a compact all-purpose Turkish work horse: a bay, brown, black, chestnut, gray, or dun mount that survives difficult terrain and carries a rider efficiently without becoming a specialist racer, jumper, or heavy draft animal. The mod does not model local village breeding practices, regional subpopulations, exact head profile, hoof hardness, pack training, traction, cold or drought resilience, sure-footed terrain algorithms, rider weight, Turkish saddle traditions, or the enormous historical complexity of Anatolia as a horse-movement corridor. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This JSON follows the readable convention in the example you supplied. Before compiling, reconcile exact source enums, genetic locus IDs, allele symbols, commonness enum labels, price units, `D/nd1/nd2` syntax, and stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** There is good evidence that Turkish native horses are genetically diverse and that Anadolu is a broad indigenous population, but I found no representative modern Anadolu-only genotype dataset with allele frequencies for Extension, Agouti, gray, dun, roan, white patterns, or dilution loci. The rates below are therefore transparent **gameplay approximations** constrained by documented modern phenotype descriptions; they are not Turkish registry statistics. The retrieved Turkish genetic screening data found no LFS or PSSM1 variants in 239 **Arabian** horses, not in Anadolu horses, so it cannot justify an Anadolu disease rate. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

```json
{
  "id": "anadolu",
  "name": "Anadolu",
  "type": "natural",
  "notes": "The Anadolu, also called Anadolu Yerli or Anatolian Native Horse, is a broad Turkish native landrace shaped by village riding, pack work, light draught, mountain and steppe travel, compact body size, sturdy feet, thrift, and adaptation to eastern and southeastern Anatolia. Procedural Horse Genetics does not model local Turkish subpopulations, exact head-profile variation, hoof hardness, pack training, agricultural traction, drought tolerance, snow tolerance, regional bloodlines, Turkish saddle traditions, rider weight, or the historical movement of domestic horse populations through Anatolia.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:grove",
    "minecraft:river"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 460,
  "commonness": "COMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.65,
      "a": 0.35
    },

    "grey": {
      "N": 0.85,
      "G": 0.15
    },
    "dun": {
      "N": 0.85,
      "D": 0.15
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
      "N": 0.95,
      "f": 0.05
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.98,
      "SB1": 0.02
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
      "N": 0.98,
      "Rb": 0.02
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
    "speed": 5,
    "jump": 4,
    "health": 9,
    "size": [
      0.82,
      0.97
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Anadolu × Anadolu produces Anadolu. Anadolu × another pure breed produces an Anadolu cross. An Anadolu cross bred back to a pure Anadolu remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every short, hardy bay horse from a dry biome as Anadolu: the real population is a regional Turkish landrace defined by ancestry, local use, and conservation context rather than a single visible phenotype."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed structure | Broad Anatolian native landrace | Anadolu is part of the broader Anatolian Native group, not a closed show-breed with one approved modern phenotype. The file intentionally avoids invented geographic micro-strains without a reliable source basis.  [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.65 / a: 0.35` | Produces bay, brown, black, and chestnut as the core pool. These values are gameplay approximations designed for a broadly practical Turkish native population, not a published Anatolian genotype survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony) |
| Gray | `G: 0.15` | Gray is a plausible and reported color family in the broader Anatolian native context but should remain a minority in a general Anadolu founder pool. This is an approximation.  [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony) |
| Dun | `D: 0.15` | True dun is a plausible primitive and regionally appropriate color; a minority frequency produces bay dun, grullo, and red dun without turning the breed into a uniform primitive-dun population.  [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony) |
| Flaxen | `f: 0.05` | Allows occasional flaxen chestnut as low-level texture, while the core population remains bay/dark/chestnut. This is a game-balance estimate. |
| Cream and other dilutions | Cream, pearl, champagne, silver, mushroom forced wild type | The ancient Anatolian record shows historical coat diversity, but that is not sufficient evidence to seed these variants in a modern, generalized Anadolu founder pool.  [hal](https://hal.science/hal-03039029/file/Guimaraes%20et%20al_2020_HAL.pdf) |
| White markings | `SB1: 0.02`, `Rb: 0.02` | Very low modifiers can permit occasional ordinary facial/leg white and minimal tail/flank white without creating pinto horses. These values are deliberately conservative and not locus-survey data. |
| Pinto patterns | Tobiano, frame, splash, and W-series white forced wild type | Modern Anadolu should remain a solid utility-horse population. The existence of ancient Anatolian tobiano/sabino horses does not justify making loud modern pinto founders routine.  [hal](https://hal.science/hal-03039029/file/Guimaraes%20et%20al_2020_HAL.pdf) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Ancient leopard-spotted horses in Anatolia are archaeological evidence, not evidence for a typical present-day Anadolu founder pool.  [hal](https://hal.science/hal-03039029/file/Guimaraes%20et%20al_2020_HAL.pdf) |
| Roan and brindle | Forced wild type | No reliable modern Anadolu evidence was found to justify seeding these loci. |
| Disorders | All named loci clear | No defensible Anadolu-specific carrier rate was located. Turkish Arabian testing cannot be used as a proxy for the distinct Anadolu landrace.  [avesis.ankara.edu](https://avesis.ankara.edu.tr/yayin/259ad75c-6e05-445e-9c19-a5d907402974/investigation-of-severe-combined-immunodeficiency-scid-disease-of-arabian-horses-raised-at-the-state-stud-farms-in-turkey/document.pdf) |
| Speed | `5/10` | Represents reliable all-purpose travel, not specialist racing. |
| Jump | `4/10` | Terrain agility and sturdy feet are useful but do not equate to deliberate show-jumping selection. |
| Health | `9/10` | Represents practical hardiness, feed thrift, rough-country utility, and environmental adaptability—not immunity from inherited disease or injury. |
| Size | `×0.82–0.97` | Matches the cited 124–142 cm, roughly 12.1–14-hand range.  [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony) |

## Disorder approach

All requested disorder loci are forced **clear** in the pure Anadolu founder pool.

This is an evidence decision, not a statement that every living Anatolian Native horse is genetically free of disease. I found Turkish genetic work describing native-horse diversity and historical coat variation, but not a representative carrier-frequency panel for the requested mutations in Anadolu horses. [madbarn](https://madbarn.com/research/high-microsatellite-and-mitochondrial-diversity-in-anatolian-native-horse-breeds-shows-anatolia-as-a-genetic-conduit-between-europe-and-asia/)

The most tempting but incorrect shortcut would be to import Arabian results because Turkish Arabians are common and historically relevant in the region. That would not be scientifically valid:

- A Turkish state-stud survey tested **239 Arabian horses** and reported no `PRKDC` SCID carriers. [avesis.ankara.edu](https://avesis.ankara.edu.tr/yayin/259ad75c-6e05-445e-9c19-a5d907402974/investigation-of-severe-combined-immunodeficiency-scid-disease-of-arabian-horses-raised-at-the-state-stud-farms-in-turkey/document.pdf)
- Another Turkish screening report found no LFS or PSSM1 mutation in its studied Arabian sample. [vetdergikafkas](https://vetdergikafkas.org/uploads/pdf/pdf_KVFD_L_2049.pdf)
- Neither study measures the distinct Anadolu landrace, so neither supports setting an Anadolu allele rate to zero or to any nonzero figure.

The pure founder pool therefore remains clear for ACAN dwarfism, `PLOD1` WFFS, `EDNRB`/frame lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, and HERDA. Those alleles may still enter an Anadolu cross through normal breeding, mutation, or splice mechanics.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `anadolu` as a natural Turkish native-landrace record with Anatolian flavour, broad dry/mountain biome placement, conservative color pool, clear disease policy, and compact hardy stats. |
| `common/breed/Breeds` | Register `anadolu` for wild packs, cowboy/stable generation, spawn eggs, H-menu display, breed books, commands, save loading, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. Wild availability is a gameplay representation of free-ranging/local rural horse populations, not a claim of a formal feral breed status. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band object. Do not simulate drought resistance, hoof hardness, or mountain sure-footedness through color-shade epigenetics. |
| `common/breed/spec/` | Reconcile the proposed object with the actual bidirectional parser and writer. Check exact locus keys, allele labels, numerical probability format, forced-wild-type behavior, and source/commonness enum values. |
| `common/breed/Commonness` | Confirm that `COMMON` maps to the requested moderate `spawn_weight: 6` rung. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 4, health 9, and size ×0.82–0.97 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only the supplied base-color, gray, dun, and minor marking-modifier pools. Force excluded dilutions, pinto/leopard loci, magical loci, and all disease loci clear. Apply compact utility-horse stat targets. |
| `common/breed/BreedLineage` | Use the default pure/cross/Mixed table. A small hardy horse from a dry biome must not receive Anadolu lineage based on appearance or health stats. |
| `common/genetics/SpliceOutcome` | No special breed exception. Spliced alleles should transmit normally and can create nonstandard Anadolu descendants, including cream, pinto, leopard, or disease-carrying animals excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize compact size, utility speed, modest jumping, and high heartiness target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the project schema requires an explicit default value. |

## Verification

1. Confirm **Anadolu** appears in the H-menu’s Breeds tab and the breed book with Turkish/Anatolian origin, native-landrace wording, all-purpose riding/pack/light-draught role, moderate commonness, compact body profile, and high-hardiness description.

2. Spawn packs in plains, meadow, dry savanna/plateau, windswept hills, badlands margins, groves, and river-valley terrain. Every member of one selected Anadolu pack should display the **Anadolu** label.

3. Confirm that an unassigned lone wild horse reads **Feral Mixed**, even if it is short, bay, hardy, and generated in dry or hilly terrain.

4. Generate at least 1,000 pure founders. The cohort should be dominated by bay, brown/dark bay, black, and chestnut. A visible minority should be gray or true dun, yielding bay dun, grullo, or red dun according to the base genes. White markings should remain minimal.

5. Confirm that pure founders do not generate cream dilution, palomino, buckskin, champagne, silver dapple, pearl, mushroom, tobiano, frame, splash, strong dominant-white expression, roan, leopard complex, brindle, or magical coats.

6. Inspect genome panels:
- `E/e` and `A/a` should create the broad base-color core.
- `G` and `D` should remain minority alleles.
- `SB1` and rabicano should be rare enough to produce at most restrained marking texture.
- All pinto, leopard, major dilution, magical, and requested disease loci must remain wild type or clear.

7. Confirm adult stat behavior. Anadolu horses should be shorter than ordinary riding horses, above average in health/heartiness, useful at ordinary travel speed, and modest rather than specialized at jumping.

8. Test default lineage:
- Anadolu × Anadolu → Anadolu.
- Anadolu × Arabian → Anadolu cross.
- Anadolu × Karabair or Akhal-Teke → Anadolu cross.
- Anadolu cross × pure Anadolu → the existing Anadolu cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test intentional outcrossing or splice inheritance. Introduce cream, tobiano, leopard complex, warmblood size, sprint speed, WFFS, or other excluded loci. Descendants should inherit normally, but phenotype alone should never reassign a cross back to pure Anadolu.

## Sources

- [Anadolu Pony / Anadolu Yerli overview](https://en.wikipedia.org/wiki/Anadolu_Pony): supplementary structured reference for Turkish origin, aliases, eastern/southeastern Anatolian focus, riding/pack/light-draught uses, 124–142 cm height range, variable head profile, and native-landrace classification. [en.wikipedia](https://en.wikipedia.org/wiki/Anadolu_Pony)

- [High microsatellite and mitochondrial diversity in Anatolian native horse breeds](https://madbarn.com/research/high-microsatellite-and-mitochondrial-diversity-in-anatolian-native-horse-breeds-shows-anatolia-as-a-genetic-conduit-between-europe-and-asia/): summary of the detailed genetic-diversity research supporting Anatolia’s role as a horse-population conduit and the conservation importance of Turkish native breeds. [madbarn](https://madbarn.com/research/high-microsatellite-and-mitochondrial-diversity-in-anatolian-native-horse-breeds-shows-anatolia-as-a-genetic-conduit-between-europe-and-asia/)

- [Nuclear genetic diversity of Turkish native horse breeds](https://digitalarchive.library.bogazici.edu.tr/items/fc40f769-a78d-4adc-8230-3b3f7c4279fe/full): Turkish native-horse conservation and genetic-characterization context. [digitalarchive.library.bogazici.edu](https://digitalarchive.library.bogazici.edu.tr/items/fc40f769-a78d-4adc-8230-3b3f7c4279fe/full)

- [Ancient DNA shows domestic horses were introduced in southern Caucasus and Anatolia during the Bronze Age](https://hal.science/hal-03039029/file/Guimaraes%20et%20al_2020_HAL.pdf): peer-reviewed ancient-DNA evidence for historical Anatolian coat-color diversity and its distinction from a modern-breed founder pool. [hal](https://hal.science/hal-03039029/file/Guimaraes%20et%20al_2020_HAL.pdf)