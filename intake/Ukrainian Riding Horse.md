The **Ukrainian Riding Horse**—also called the **Ukrainian Saddle Horse**—should be a tall, powerful, postwar Ukrainian warmblood sport horse, genetically centered on bay, chestnut, and brown, with black as a minority and no automatic pinto/leopard/dilution palette. It is a managed national sport breed rather than a wild population, so its strengths should be athletic movement, all-around riding speed, good jumping, and a large warmblood frame. The breed began at Dnipropetrovsk after World War II, received a studbook in 1971, and was officially recognized in 1990. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

## Identity & flavour

The **Ukrainian Riding Horse**, Ukrainian *Ukrainskyi Verkhovyi Kin* (український верховий кінь), is also known in English as the **Ukrainian Saddle Horse**. It is a modern Ukrainian warmblood developed under Soviet and later Ukrainian breeding programs to create a nationally competitive sport horse. Breeding began after the Second World War at Dnipropetrovsk Stud in central Ukraine and later expanded to Aleksandriisk, Yagonitsk, Derkul, and other state farms. The studbook began in 1971, and the breed received formal Soviet recognition in 1990, shortly before Ukrainian independence. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

The Ukrainian Riding Horse was made for modern equestrian work rather than for one narrow traditional job. Its founders combined Hungarian mare lines—especially **Nonius**, **Furioso-North Star**, and **Gidran**—with Trakehner, Hanoverian, Thoroughbred, and Russian Saddle Horse influence. That mix was intended to unite strength, bone, steadiness, and a practical Eastern European frame with the movement, gallop, scope, refinement, and athleticism needed for dressage, jumping, eventing, and general riding. A Ukrainian Riding Horse should feel like a substantial all-round sport warmblood: not as jump-specialized as Zangersheide, not as dressage-specialized as some modern German lines, and not as heavy as a Nonius. [breeds.okstate](https://breeds.okstate.edu/horses/ukrainian-saddle-horses)

Adult stallions average about **165 cm**, approximately **16.1–16.2 hands**, while mares average about **160 cm**, approximately **15.3–15.3 hands**. The breed is muscular and solidly built, with a proportionate, straight-profiled head, expressive eyes, long muscular neck, long poll, high prominent withers, deep broad chest, long straight back, broad strong loins, long sloping croup, and well-set, strong limbs. It should read as more substantial than a refined Thoroughbred cross, but more athletic and elevated than an old-style agricultural horse. There is no heavy feathering, no special mane texture, and no small-pony outline. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

Chestnut, bay, and brown are widespread in the breed; black is less common. Some general sources list white, gray, and dilute colors, but the best-supported implementation should stay conservative: ordinary base colors are the core visual identity, while standard white face and lower-leg markings may occur only minimally. The Ukrainian Riding Horse is not a Paint, an Appaloosa, or a color-bred warmblood. Therefore pure founders should not create tobiano, frame, splash, leopard complex, roan, champagne, silver, pearl, mushroom, or magical coats. Gray and cream can be treated as absent unless a verified Ukrainian Riding Horse studbook color-frequency source is added later. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

The breed should feel intelligent, willing, strong, energetic, and trainable, with a good balance of speed, jumping power, and durability. Its cultural story is one of rebuilding: a national warmblood created from state-stud cooperation after wartime loss, then carried forward through Ukraine’s transition from Soviet republic to independent country. In Procedural Horse Genetics, players should breed toward a tall, solid-colored Ukrainian sport horse with a powerful hind end, a bay-or-chestnut core, and versatile athletic stats. The mod does not model state-stud selection, passport/pedigree rules, horse inspections, detailed movement quality, dressage collection, jumping technique, eventing bravery, professional training, war-era breeding history, or individual rideability. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This follows the readable JSON convention from the provided example. Before compiling, reconcile exact field names, source enum values, locus identifiers, allele symbols, commonness labels, price units, and stat-target serialization against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** I found reliable history, foundation-breed, height, conformation, and broad base-color descriptions, but no representative Ukrainian Riding Horse genotype survey with Extension/Agouti, gray, cream, white-pattern, or disease allele frequencies. The supplied coat probabilities are transparent **gameplay approximations**, not Ukrainian registry statistics. Because no Ukrainian Riding Horse-specific carrier rate was identified for WFFS or any disorder in the requested panel, all named disorder loci are clear. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

```json
{
  "id": "ukrainian_riding_horse",
  "name": "Ukrainian Riding Horse",
  "type": "natural",
  "notes": "The Ukrainian Riding Horse, also called the Ukrainian Saddle Horse, is a modern Ukrainian sport warmblood defined by official studbook pedigree, postwar state-stud selection, athletic conformation, high withers, strong back and hindquarters, quality movement, rideability, dressage, jumping, eventing, and professional training. Procedural Horse Genetics does not model studbook eligibility, passport documentation, stallion licensing, mare selection, state-stud history, dressage collection, jumping technique, eventing courage, gait quality, professional training, rider compatibility, competition records, or individual temperament.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:flower_forest",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1240,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.66,
      "e": 0.34
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "grey": {
      "N": 1.0
    },
    "cream": {
      "N": 1.0
    },
    "dun": {
      "N": 1.0
    },
    "champagne": {
      "N": 1.0
    },
    "silver": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.98,
      "f": 0.02
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
      "N": 0.99,
      "Rb": 0.01
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
    "speed": 7,
    "jump": 7,
    "health": 7,
    "size": [
      1.07,
      1.19
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Ukrainian Riding Horse × Ukrainian Riding Horse produces Ukrainian Riding Horse. Ukrainian Riding Horse × another pure breed produces a Ukrainian Riding Horse cross. A Ukrainian Riding Horse cross bred back to pure Ukrainian Riding Horse remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Ukrainian Riding Horse lineage to every tall bay warmblood: real breed identity requires recognized Ukrainian studbook ancestry, not body size, sport stats, or base coat alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Managed national sport warmblood | The breed was intentionally created in Ukrainian state studs after World War II from Hungarian, Trakehner, Hanoverian, Thoroughbred, and Russian Saddle-related material. It is a modern performance population, not a natural or feral local breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse) |
| Base-color pool | `E: 0.66 / e: 0.34`; `A: 0.68 / a: 0.32` | Chestnut, bay, and brown are reported as widespread, while black is less common. These values favor bay and chestnut while retaining a smaller black/brown population. They are gameplay approximations, not a genotype survey.  [horsebreedspictures](https://www.horsebreedspictures.com/ukrainian-riding-horse.asp) |
| Gray | Forced wild type | A few broad breed summaries mention white or gray, but no strong official/studbook source was retrieved to justify seeding true progressive gray in the default founder population. Keep it absent until direct color data is available. |
| Cream, dun, champagne, silver, pearl, mushroom | Forced wild type | No reliable Ukrainian Riding Horse-specific evidence was found to seed these loci. The open warmblood ancestry is not justification for adding every possible performance-horse color. |
| Flaxen | `f: 0.02` | A very low optional modifier permits occasional light-maned chestnuts without making flaxen a recognizable breed trait. Remove it if the real schema or breed policy requires only strictly documented loci. |
| Modest marking texture | `SB1: 0.02`, `Rb: 0.01` | These very low values are a gameplay approximation for ordinary minor white face/leg markings and slight hair variation. They should not create pinto founders. Remove them if `SB1` expression in the mod is too broad. |
| Pinto and leopard exclusion | Tobiano, frame, splash, W-series white, roan, leopard complex, PATN, brindle all fixed wild type | The Ukrainian Riding Horse is not described as a pinto, roan, or Appaloosa-patterned breed. Pure founders should remain solid sport horses with, at most, restrained normal markings.  [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse) |
| Magical loci | All forced wild type | The breed is an ordinary modern warmblood; its identity comes from conformation and athletic selection, not magical pigment. |
| Disorders | All named loci clear | No Ukrainian Riding Horse-specific carrier-rate dataset was found for ACAN dwarfism, WFFS, frame-linked lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `7/10` | The breed was created for modern sport, with Thoroughbred, Trakehner, and Hanoverian influence supporting athletic gallop and useful speed.  [breeds.okstate](https://breeds.okstate.edu/horses/ukrainian-saddle-horses) |
| Jump | `7/10` | Jumping is one intended discipline, but the breed is an all-round national warmblood rather than a Zangersheide-style jumping-only studbook.  [chevauxdumonde](https://chevauxdumonde.com/en/horse/ukrainian-riding-horse) |
| Health | `7/10` | Represents selected soundness and substantial build appropriate to sport use; it does not imply primitive-breed hardiness or genetic disease immunity. |
| Size | `×1.07–1.19` | Captures average 160 cm mares and 165 cm stallions, producing a large warmblood frame without reaching giant-draft size.  [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse) |

## Disorder approach

Every named disorder locus is **clear** in the pure Ukrainian Riding Horse founder pool.

This is the scientifically responsible choice given the available evidence. The breed has warmblood, Thoroughbred, and Hungarian ancestry, which makes some modern sport-horse mutations *possible* in individual real-world lines, but ancestry is not a reliable carrier-rate estimate. No population-wide Ukrainian Riding Horse panel was found for:

- `ACAN` dwarfism.
- `PLOD1` / WFFS.
- `MET` / `EDNRB` lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

The correct implementation is not to assign generic warmblood WFFS rates by analogy. If a direct Ukrainian Riding Horse WFFS survey becomes available, add only `PLOD1` at the frequency supported by that survey and clearly distinguish mutant **allele frequency** from heterozygous **carrier prevalence**.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `ukrainian_riding_horse` as a natural Ukrainian sport-warmblood record with managed sources, solid base-color pool, clear disorder policy, large athletic stat targets, and national-breed flavour. |
| `common/breed/Breeds` | Register the ID for stable/cowboy access, spawn eggs, H-menu display, breed books, commands, genome serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because Ukrainian Riding Horses are studbook-managed sport horses, not a wild or feral population. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band object. Do not create color bands to imitate warmblood quality, movement, muscularity, or sport training. |
| `common/breed/spec/` | Reconcile the sample keys with the actual read/write schema, including correct Extension/Agouti representations, default wild-type handling, source enum syntax, and whether minor white-pattern loci should be specified or omitted. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the requested rarity ladder and that numeric `spawn_weight: 3` is valid for the project’s weighted generation system. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 7, health 7, and size ×1.07–1.19 into legal `TargetBand` entries. |
| `common/breed/BreedFounder` | Roll the constrained solid base-color pool, force all major dilutions, pinto patterns, leopard genes, magical loci, and disease loci wild type or clear, then apply the tall all-round sport-horse targets. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A large bay or chestnut jumper must not receive Ukrainian Riding Horse lineage merely from phenotype or stat scores. |
| `common/genetics/SpliceOutcome` | No special exception. A spliced allele should transmit normally and can create a nonstandard descendant outside the default Ukrainian Riding Horse pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize large sport-horse size plus balanced speed, jump, and health targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual file format requires a default band type. |

## Verification

1. Confirm **Ukrainian Riding Horse** appears in the H-menu’s Breeds tab and breed book with Ukrainian postwar state-stud history, managed acquisition sources, large warmblood size, all-round sport statistics, and bay/chestnut/brown visual identity.

2. Confirm the breed does **not** appear in ordinary wild packs because `wild` is omitted. It should appear through cowboy, stable, and spawn-egg systems only.

3. Confirm an ordinary lone wild horse always reads **Feral Mixed**, even when it is tall, bay, chestnut, brown, black, or has good sport stats.

4. Generate at least 1,000 pure founders. The sample should be dominated by bay, chestnut, dark bay/brown, and a smaller black population. It must not produce gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, roan, tobiano, frame, splash, dominant white, leopard complex, brindle, or magical coats.

5. Inspect genomes:
- `E/e` and `A/a` should be the only important variable coat loci.
- Flaxen, Sabino 1, and rabicano should appear only at their extremely low optional frequencies if retained.
- All major dilution, pinto, leopard, magical, and disease loci must remain wild type or clear.
- If modest Sabino 1 unexpectedly creates obvious pinto body-white in the renderer, remove `SB1` and leave ordinary markings as non-genotyped texture.

6. Test Mendelian base-color outcomes:
- `E/e × E/e` pairings should occasionally yield chestnut `e/e` foals.
- `E_ A_` foals should render bay/dark bay/brown.
- `E_ a/a` foals should render black.
- None of these base-color outcomes should alter the pure Ukrainian Riding Horse lineage label.

7. Confirm adult stat behavior. Ukrainian Riding Horses should trend tall, balanced, athletic, and notably capable in both speed and jumping. They should not outclass Zangersheide or dedicated elite jumpers in jumping, Thoroughbreds in sprinting, or primitive survival breeds in low-input heartiness.

8. Test default lineage:
- Ukrainian Riding Horse × Ukrainian Riding Horse → Ukrainian Riding Horse.
- Ukrainian Riding Horse × Trakehner → Ukrainian Riding Horse cross.
- Ukrainian Riding Horse × Nonius → Ukrainian Riding Horse cross.
- Ukrainian Riding Horse cross × pure Ukrainian Riding Horse → the existing Ukrainian Riding Horse cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test intentional outcrossing or splice inheritance. Add gray, cream, tobiano, leopard complex, heavy-draft size, WFFS, or other excluded loci through another breed or the mod’s gene-editing mechanics. The resulting descendant should inherit normally but must not be auto-classed as a typical pure Ukrainian Riding Horse from appearance alone.

## Sources

- [Ukrainian Riding Horse overview](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse): structured historical source for postwar development at Dnipropetrovsk, 1971 studbook, 1990 recognition, foundation breeds, 2004 registered population, conformation, and sport-breed classification. [en.wikipedia](https://en.wikipedia.org/wiki/Ukrainian_Riding_Horse)

- [Oklahoma State University — Ukrainian Saddle Horses](https://breeds.okstate.edu/horses/ukrainian-saddle-horses): concise university breed reference for Hungarian Nonius, Furioso-North Star, and Gidran mares crossed with Trakehner, Hanoverian, and Thoroughbred stallions, with Russian Saddle influence. [breeds.okstate](https://breeds.okstate.edu/horses/ukrainian-saddle-horses)

- [Ukrainian Riding Horse profile](https://www.horsebreedspictures.com/ukrainian-riding-horse.asp): supplementary conformation, height, weight, common-color, foundation-breed, and state-stud history reference. [horsebreedspictures](https://www.horsebreedspictures.com/ukrainian-riding-horse.asp)

- [Ukrainian Riding Horse profile](https://chevauxdumonde.com/en/horse/ukrainian-riding-horse): supplementary explanation of the postwar national sport-horse program and intended Olympic-discipline performance. [chevauxdumonde](https://chevauxdumonde.com/en/horse/ukrainian-riding-horse)