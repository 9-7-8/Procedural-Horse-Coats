The **Nonius** should be a rare Hungarian carriage-and-light-draught horse defined by dark, nearly uniform colors, large frame, calm strength, and historic Mezőhegyes stud breeding. Pure founders should overwhelmingly be black, dark bay, and brown, with no gray, chestnut, dilute, pinto, roan, leopard, or magical-color genetics. It should feel powerful and durable rather than fast: a broad, substantial all-purpose work horse that can pull, carry, and endure, but is not a modern jumping warmblood or a sprinting cavalry horse. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

## Identity & flavour

The **Nonius** is a Hungarian carriage, farm, cavalry, and light-draught horse developed at the imperial state stud of **Mezőhegyes** in southeastern Hungary. It takes its name from **Nonius Senior**, a dark Anglo-Norman stallion captured in France during the Napoleonic Wars and brought to Mezőhegyes in 1816. His descendants were bred with local mares and selected Anglo-Norman, Norfolk Roadster, Arabian, Lipizzan, and other European stock to establish a strong, practical Hungarian horse. The Nonius type coalesced through the nineteenth century and was internationally recognized as a distinct breed in the twentieth. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

The Nonius was made to work. In the age before tractors and motor lorries, it pulled farm equipment, artillery, wagons, and carriages; carried cavalry riders; and served as a versatile state-stud horse for Hungary’s plains and agricultural districts. It needed strength, stamina, a calm mind, and enough height to be useful in harness and under saddle. A Nonius in this mod should feel like a dependable heavy riding horse and light draught animal: not the colossal mass of a Shire, but much more substantial than a typical saddle horse. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

The breed is large and powerful, generally around **15.3–16.3 hands**, with two historical types: a more athletic, lighter saddle-and-carriage type and a heavier agricultural type. It has a long, heavy head with a straight to slightly convex profile, broad forehead, medium to long neck, broad chest, relatively long back, muscular loins, strong sloping croup, powerful hindquarters, robust joints, and substantial bone. The mane and tail are ordinary, often thick, and there is little to no true feather. The overall look is dark, plain, and forceful: a serious harness horse with more presence than refinement. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

Modern Nonius horses are characteristically **black, dark bay, or brown**. A 2021 genetic paper discussing Hungarian and Serbian Nonius populations notes that most representatives are black, dark bay, or brown, while bay horses are relatively more common in the Hortobágy stud population. That makes the breed a particularly good candidate for a visually unified founder pool: strong Extension pigment, a low chestnut rate, and a mix of dominant agouti and recessive agouti sufficient to yield dark bay/brown and black. Gray, cream, dun, champagne, silver, roan, pinto, leopard complex, and broad white patterns should not arise from pure Nonius founders. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

Nonius horses are known for calm temperament, willingness, endurance, strength, and tractable workability. They are not intended to be flashy or delicate; their appeal is the sense that they could pull a loaded carriage through mud, then still carry a rider home. In Procedural Horse Genetics, players should breed toward a tall, dark, heavy-bodied Hungarian utility horse with excellent health and carrying power, modest speed, and practical—but not elite—jumping ability. The mod does not model pulling force, harness training, artillery or farm work, exact head profile, broad chest, bone circumference, carriage action, temperament, Mezőhegyes or Hortobágy studbook inspection, regional Hungarian and Serbian subpopulations, or the distinction between the historic lighter and heavier Nonius types. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This JSON follows the readable format in your provided Anglo-Arabian example. Before compiling, reconcile exact field names, locus IDs, allele symbols, source values, price units, `Commonness` names, and body-stat target serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The 2021 Hungarian genetic work directly supports the qualitative statement that most Nonius horses are black, dark bay, or brown, but I found no representative Nonius-wide Extension/Agouti allele-frequency dataset. The numerical `E/e` and `A/a` rates below are transparent **gameplay approximations**, chosen to create a dark bay/brown/black founder population. No Nonius-specific carrier-frequency dataset was found for the requested disease loci, so all are clear. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

```json
{
  "id": "nonius",
  "name": "Nonius",
  "type": "natural",
  "notes": "The Nonius is a Hungarian carriage, cavalry, farm, and light-draught horse developed at the Mezőhegyes state stud from the nineteenth century onward. Its real identity includes historic Nonius Senior lineage, studbook eligibility, lighter and heavier type distinctions, broad chest, heavy head, substantial bone, harness action, pulling strength, calm temperament, carriage training, agricultural work, and regional Hungarian and Serbian breeding populations. Procedural Horse Genetics does not model those traits directly.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:windswept_hills",
    "minecraft:savanna"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 940,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.94,
      "e": 0.06
    },
    "agouti": {
      "A": 0.56,
      "a": 0.44
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
    "flaxen": {
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
    "roan": {
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
    "jump": 4,
    "health": 8,
    "size": [
      1.08,
      1.20
    ]
  },

  "epigenetic_bands": {
    "extension_black_intensity": [
      0.75,
      1.0
    ]
  },

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Nonius × Nonius produces Nonius. Nonius × another pure breed produces a Nonius cross. A Nonius cross bred back to pure Nonius remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Nonius lineage to every dark, heavy Hungarian-type horse: real breed identity depends on Mezőhegyes/Hortobágy or related studbook lineage, not color and body size alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Managed Hungarian state-stud work horse | The Nonius was developed from the Mezőhegyes program for agricultural, carriage, cavalry, and general work. It is a studbook breed, not a feral population. |
| Extension | `E: 0.94 / e: 0.06` | Strongly favors black pigment and prevents a chestnut-heavy population, matching the observation that most Nonius representatives are black, dark bay, or brown. The rate is a gameplay approximation.  [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf) |
| Agouti | `A: 0.56 / a: 0.44` | Produces both dark bay/brown and true black horses, keeping the visual herd dark without making all founders genetically black. Bay is noted as somewhat more common in the Hortobágy population.  [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf) |
| Dark presentation | Narrow base-color pool plus optional black-depth band | The breed’s visual signature is not “any black horse,” but an unusually consistent population of black, dark bay, and brown work horses. The `extension_black_intensity` band is optional and must only be used if the real schema has an equivalent non-stat pigment-intensity axis.  [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf) |
| Gray | Forced wild type | The modern Nonius phenotype references emphasize black, dark bay, brown, and bay—not a gray population.  [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf) |
| Chestnut | Possible but rare through `e: 0.06` | The low `e` rate permits occasional chestnut outcomes from two carriers, preserving ordinary genetic realism without visually diluting the dark-breed identity. If the real Nonius studbook excludes chestnut in the target population, change `e` to `0.0`. |
| Dilutions | Cream, pearl, champagne, silver, mushroom, dun, and flaxen forced wild type | No strong evidence was found to make these routine pure-Nonius founder alleles. Their absence keeps the founder pool dark and historically recognizable. |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | Pure Nonius should be solid and dark. Ordinary small incidental markings are not enough evidence to seed loud inherited white-pattern loci. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type spotting is not part of the recognized Nonius phenotype. |
| Magical loci | All forced wild type | The Nonius’s visual impact comes from dark pigment and work-horse form, not magical effects. |
| Disorders | All named loci clear | No defensible Nonius-specific carrier-rate study was located for ACAN dwarfism, PLOD1/WFFS, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `4/10` | The breed was selected for pull, stamina, and utility—not fast racing. |
| Jump | `4/10` | It can be athletic enough for ordinary terrain and riding, but it is not a specialist jumping warmblood. |
| Health | `8/10` | Represents work-horse durability, soundness, and agricultural usefulness—not genetic invulnerability. |
| Size | `×1.08–1.20` | Produces a large, substantial horse in the 15.3–16.3-hand range, heavier than a normal saddle horse but below giant draft scale. |

## Disorder approach

Every named disorder locus is set to **clear** in the pure Nonius founder pool.

No credible Nonius-specific carrier-frequency survey was located for the listed diseases. The correct evidence-based implementation is therefore not to import rates from related historical contributors such as Arabians, Anglo-Normans, Thoroughbreds, Lipizzans, or modern warmbloods. Breed ancestry alone does not establish a present-day disease frequency.

This applies to:

- `ACAN` dwarfism.
- `PLOD1` / WFFS.
- `MET` / frame-associated lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

A later Nonius-specific screening dataset should replace the all-clear policy only for directly measured variants, with a distinction between **allele frequency**, **carrier frequency**, and **affected prevalence**.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `nonius` as a natural Hungarian work-horse record with managed-only acquisition sources, dark founder pool, clear disease policy, high size, and durability targets. |
| `common/breed/Breeds` | Register `nonius` for stable/cowboy acquisition, spawn eggs, breed books, H-menu display, commands, lineage labels, save loading, and genome serialization. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because Nonius is a managed studbook horse, not a natural feral population. |
| `common/breed/BreedBands` | Support the optional black-depth epigenetic band only if the real code exposes a valid coat-intensity trait. Otherwise use an empty band object and let the constrained `E/e` and `A/a` pool carry the visual identity. |
| `common/breed/spec/` | Confirm exact gene and epigenetic-band keys. If `extension_black_intensity` does not exist, remove it rather than creating a non-compiling custom field. |
| `common/breed/Commonness` | Confirm `RARE` matches the requested rarity ladder and retain `spawn_weight: 1.5` only if direct numeric weights are supported. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 8, and size ×1.08–1.20 into valid `TargetBand` settings. |
| `common/breed/BreedFounder` | Roll from the tightly constrained Extension/Agouti pool, force all unlisted coat and disorder loci wild type/clear, and apply large work-horse stat targets. |
| `common/breed/BreedLineage` | Use standard pure/cross/Mixed behavior. A large dark horse must not acquire Nonius lineage from its appearance or stat profile. |
| `common/genetics/SpliceOutcome` | No special rule. Splice-carrot alleles transmit normally and can create nonstandard descendants outside the pure dark Nonius founder pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the heavy light-draught size range and modest-speed, modest-jump, high-health targets. |
| `common/breed/BandType` | Use no band type unless the real epigenetic-band implementation requires an explicit `TRADITIONAL` or `BACHELOR` field. |

## Verification

1. Confirm **Nonius** appears in the H-menu’s Breeds tab and breed book with Mezőhegyes origin, Hungarian utility/cavalry history, rare commonness, large size, dark-coat identity, and managed-only acquisition sources.

2. Confirm Nonius does **not** appear in naturally generated wild packs because `wild` is omitted. It should appear through cowboy, stable, and spawn-egg sources only.

3. Confirm a lone wild horse reads **Feral Mixed**, even if it is black, dark bay, large, heavily built, or standing on plains.

4. Generate at least 1,000 pure Nonius founders. Nearly all should be black, dark bay, brown, or bay. Chestnut should be very rare. The population must not produce gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, roan, tobiano, frame, splash, sabino, leopard complex, brindle, or magical phenotypes.

5. Inspect genomes:
- `E` should be near-fixed and `e` rare.
- `A` and `a` should both be present so the population makes dark bay/brown and black founders.
- All dilution, white-pattern, leopard, magical, and disorder loci should be wild type or clear.
- If the optional coat-intensity band exists, confirm its values fall inside the stated closed range and only affect allowed pigment-depth presentation.

6. Test Mendelian base-color inheritance:
- Two `E/e` founders should occasionally produce chestnut `e/e` foals at approximately 25% among their offspring.
- `E_ A_` foals should render bay/dark bay/brown.
- `E_ a/a` foals should render black.
- A chestnut outcome must not be mislabeled as an error unless the strict all-`E` version is chosen.

7. Verify body stats over mature horses. Nonius should trend large, slow-to-moderate in speed, modest in jumping, and high in health. It should feel strong and useful without becoming Shire-scale or a top sport horse.

8. Test default lineage:
- Nonius × Nonius → Nonius.
- Nonius × Lipizzaner → Nonius cross.
- Nonius × Hungarian Warmblood → Nonius cross.
- Nonius cross × pure Nonius → the existing Nonius cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test deliberate outcrossing or splice inheritance. Introduce gray, cream, tobiano, leopard complex, warmblood jumping stats, or any named disease allele. The resulting horse should follow normal Mendelian inheritance but remain visibly nonstandard for the pure Nonius breed description.

## Sources

- [Genetic characterization of Hungarian and Serbian Nonius horses](https://real.mtak.hu/144313/1/article-p239.pdf): peer-reviewed population and haplotype study; explicitly notes that most Nonius horses are black, dark bay, or brown and that bay is relatively more common in the Hortobágy population. [real.mtak](https://real.mtak.hu/144313/1/article-p239.pdf)

- [Quantitative genetic aspects of coat color in horses](https://pubmed.ncbi.nlm.nih.gov/16971562/): peer-reviewed study including 294 horses from Nonius and other breeds, supporting the use of Nonius as a color-genetics population while not supplying a direct breed-wide locus-frequency table. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/16971562/)

- [Nonius breed overview](https://en.wikipedia.org/wiki/Nonius_horse): supplementary historical context for Nonius Senior, Mezőhegyes foundation, nineteenth-century development, dual light/heavy types, carriage/cavalry/agricultural uses, height, conformation, and dark colors.