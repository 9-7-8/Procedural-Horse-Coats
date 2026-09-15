The **Mangalarga Marchador** should be represented as a Brazilian Iberian-rooted saddle breed with a broad, genuinely variable coat pool and unusually strong “comfort over distance” gameplay identity. Its defining biological and cultural feature is not a particular color but the naturally smooth, four-beat **marcha** gait—especially *marcha batida* and *marcha picada*—which the mod cannot directly encode, so the breed should receive strong health, good speed, moderate jumping, and a medium riding-horse size band rather than an artificially narrow phenotype. [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador)

## Identity & flavour

The **Mangalarga Marchador**, sometimes abbreviated **MM** and historically called **Mangalarga Mineiro**, is Brazil’s best-known native saddle-horse breed. It developed in Minas Gerais in southeastern Brazil from the eighteenth century onward, rooted in Portuguese Iberian horses—especially Alter Real/Lusitano-type stock—crossed with selected local horses that included gaited Iberian-descended animals. The association that now governs the breed, the **Associação Brasileira dos Criadores do Cavalo Mangalarga Marchador** (**ABCCMM**), was established in Belo Horizonte in 1949 to formalize the breed’s identity, standards, and especially its distinctive gait. [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador)

The Marchador was made for long, comfortable days in the saddle across Brazilian farms, hills, roads, and varied country. It needed to cover ground economically, carry a rider for hours, remain tractable around livestock and people, and do so without the jarring trot of an ordinary riding horse. Its hallmark is the **marcha**, a smooth, ambling four-beat family of gaits with moments of triple support. *Marcha batida* trends diagonally coordinated, while *marcha picada* trends more laterally coordinated and is often considered the smoother of the two. The breed can still walk, canter, and perform ordinary horse gaits; it is the quality, rhythm, comfort, and natural maintenance of the marcha that makes it a Marchador. [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador)

A typical Mangalarga Marchador is a medium, elegant but useful saddle horse, generally about **14.2–16 hands** and roughly **850–1,100 lb**. It should have a dry, expressive Iberian-type head with a broad forehead, straight or slightly subconvex profile, alert eyes, a well-set neck, defined withers, a straight and muscular medium-length back, rounded hindquarters, and clean, durable limbs. The overall impression is neither a tiny gaited pony nor a heavy stock horse: it is a versatile, balanced riding horse with enough substance for ranch work and enough refinement for presentation. Mane and tail are normally full and practical; Brazilian tradition often calls for mares’ manes to be roached, but that is grooming rather than a separately inherited mane trait. [namarchador](https://namarchador.org/breed/)

Unlike the Konik, the Mangalarga Marchador is **not** visually unified. Gray is conspicuous and often common, but bay, brown, black, chestnut, buckskin, palomino, dun, roan, and pinto-patterned horses occur in the breed. Sources describing the North American and Brazilian population explicitly recognize solid horses as well as sabino and tobiano patterned individuals. A good in-game MM herd should therefore feel richer and more colorful than a gray-only Iberian breed: grays should be frequent, ordinary bay and chestnut should be well represented, black should occur, and cream or pinto should be possible without making every founder a loud-color horse. [namarchador](https://namarchador.org/breed/)

Marchadors are prized for docility, endurance, willingness, and their comfortable travel gait. The ABCCMM’s permanent registration system historically requires inspection of eligible horses at three years or older for conformation, gait, and temperament, which means a real Marchador is more than a pedigree label or a pretty color. In Procedural Horse Genetics, players should breed toward a durable, smoothly traveling, versatile Brazilian riding horse: a medium frame, good all-day speed, strong heartiness, and a broad but plausibly Iberian-Brazilian coat palette. The mod does not model *marcha batida*, *marcha picada*, triple-support timing, stride comfort, rider fatigue, gait inspection, laterality, training, mane roaching, regional Brazilian bloodlines, or registry approval. [campeasdagameleira.com](https://www.campeasdagameleira.com.br/ingles/brazilian_breed.php)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed-wiki URL could not be retrieved by the documentation fetcher. This uses the readable JSON structure from your provided Anglo-Arabian example; reconcile exact field names, locus IDs, allele labels, source enum values, and `Commonness` enum spelling against `common/breed/spec/` before compiling.
>
> **Scientific-rate caveat:** Breed descriptions support the presence of gray, cream-derived colors, dun, roan, tobiano, and sabino in the Mangalarga Marchador, but I did not locate a population-wide, peer-reviewed allele-frequency survey for all of those loci. The color rates below are therefore explicitly **gameplay population approximations**, selected to yield a recognizable, broad MM coat population without falsely presenting unmeasured values as registry statistics. Disorder loci are all clear because no defensible Mangalarga Marchador-specific carrier frequencies were identified for the requested diseases.

```json
{
  "id": "mangalarga_marchador",
  "name": "Mangalarga Marchador",
  "type": "natural",
  "notes": "The Mangalarga Marchador is defined by Brazilian pedigree, inspection, conformation, temperament, and above all the naturally smooth marcha gait. Procedural Horse Genetics does not model marcha batida, marcha picada, gait timing, triple-support moments, rider comfort, gait quality, gait training, ABCCMM inspection, permanent-registration eligibility, mane-roaching tradition, regional bloodlines, working-cattle skill, or the subjective elegance and docility valued in the real breed.",

  "biomes": [
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:wooded_badlands",
    "minecraft:sparse_jungle"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 690,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.67,
      "a": 0.33
    },

    "grey": {
      "N": 0.62,
      "G": 0.38
    },
    "cream": {
      "N": 0.86,
      "Cr": 0.14
    },
    "dun": {
      "N": 0.93,
      "D": 0.07
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
      "N": 0.92,
      "f": 0.08
    },

    "tobiano": {
      "N": 0.90,
      "TO": 0.10
    },
    "sabino_1": {
      "N": 0.88,
      "SB1": 0.12
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
      "N": 0.94,
      "Rn": 0.06
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
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
    "jump": 5,
    "health": 8,
    "size": [
      0.98,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Mangalarga Marchador × Mangalarga Marchador produces Mangalarga Marchador. Mangalarga Marchador × another pure breed produces a Mangalarga Marchador cross. A Mangalarga Marchador cross bred back to pure Mangalarga Marchador remains that cross under the default system. Different crosses produce Mixed, and any pairing with Feral Mixed produces Mixed. Do not label an arbitrary gaited or gray Brazilian-style horse as Mangalarga Marchador without two Mangalarga Marchador lineage labels; the real breed is registry-defined and requires inspection."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Base-color pool | `E: 0.70 / e: 0.30`; `A: 0.67 / a: 0.33` | Supports bay, black/brown, and chestnut foundations. These colors are all repeatedly described in the breed, although the exact numbers are game-balancing estimates rather than a measured MM allele survey.  [namarchador](https://namarchador.org/breed/) |
| Gray | `G: 0.38` | Gray is repeatedly described as predominant or prominent. A 38% founder allele rate makes gray common without turning most newborns and all adult population samples white. It is an implementation approximation, not an ABCCMM population statistic.  [namarchador](https://namarchador.org/breed/) |
| Cream | `Cr: 0.14` | Buckskin and palomino are documented breed colors; cream is therefore appropriate at a moderate-low rate. The rate preserves the colors as recognizable possibilities rather than a dominant herd phenotype.  [namarchador](https://namarchador.org/breed/) |
| Dun | `D: 0.07` | Dun is documented among accepted/observed colors. Keeping it uncommon avoids accidentally producing a Konik-like primitive-dun population.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Mangalarga_Marchador.php) |
| Flaxen | `f: 0.08` | A cautious low rate allows occasional flaxen chestnut expression if the mod’s locus supports it. This is not a claim of a documented MM flaxen allele survey. |
| Tobiano | `TO: 0.10` | Pinto horses are documented, and one breed source explicitly identifies tobiano among known MM coat patterns. This rate keeps tobiano visible but distinctly less common than solid horses.  [namarchador](https://namarchador.org/breed/) |
| Sabino | `SB1: 0.12` | Sabino patterning is documented in descriptions of MM coat variation. The modest rate permits stockings, facial white, and variable sabino expression without making the breed predominantly patterned.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Mangalarga_Marchador.php) |
| Roan | `Rn: 0.06` | Roan is reported among accepted/observed colors, so it belongs at a low rate.  [namarchador](https://namarchador.org/breed/) |
| Rabicano | `Rb: 0.03` | Kept low as a conservative modifier-like source of subtle tail and flank white; it is gameplay texture rather than a breed-specific frequency assertion. |
| Excluded dilutions | Silver, champagne, pearl, and mushroom forced wild type | The source set supports the implemented colors but did not provide credible evidence to seed these alleles in a pure MM population. Players can introduce them through crosses or genetics mechanics. |
| Excluded white loci | Frame, splash, W-series spotting, leopard complex, and brindle forced wild type | Tobiano and sabino already produce a breed-supported pinto/white-marking spectrum. No evidence found justified seeding these other named loci. Avoiding frame also avoids inventing an associated lethal-white risk. |
| Disorders | All listed disorder loci clear | No breed-specific carrier-rate evidence was found for this disease list. Keeping founders clear is scientifically more defensible than importing disease frequencies from unrelated gaited, stock, Arabian, Quarter Horse, Friesian, or Warmblood populations. |
| Speed | `7/10` | Represents a naturally smooth, fast-feeling all-day saddle gait and a versatile working-riding role, not elite galloping-race specialization.  [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador) |
| Jump | `5/10` | Baseline athletic jumping: useful, balanced, and capable, but not the breed’s principal historic selection goal. |
| Health | `8/10` | Represents hardiness, utility, soundness, stamina, and field endurance. It does not imply immunity to all disease or a documented lower disease prevalence.  [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador) |
| Size | `×0.98–1.10` | Centers ordinary founders around a 14.2–16-hand medium saddle-horse population, with enough sex and individual variation to avoid a uniform clone-like herd.  [namarchador](https://namarchador.org/breed/) |

## Disorder approach

All requested disorder loci are **forced clear** in the pure Mangalarga Marchador founder pool.

This is a scientific-evidence decision, not a claim that Mangalarga Marchadors cannot inherit genetic disease. The breed has a large, geographically diverse population, and genetic disease testing may be relevant to individual breeding programs. However, no reliable Mangalarga Marchador-specific carrier frequencies were located for ACAN dwarfism, PLOD1 dwarfism, MET-related lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

In particular:

- **HYPP** is strongly associated with Quarter Horse-derived *Impressive* ancestry, not a reason to seed a Brazilian Iberian-rooted gaited breed by default.
- **SCID, CA, and LFS** are best treated as Arabian-line disorders unless credible evidence supports rates in the target breed.
- **PLOD1 dwarfism** is a Friesian-associated disorder.
- **HERDA** is chiefly associated with particular Quarter Horse cutting-horse lineages.
- **Frame overo / MET** is not seeded, so the corresponding lethal-white syndrome should not arise from two pure MM founders under this definition.

If the mod later gains a peer-reviewed Mangalarga Marchador disease-screening dataset, replace the all-clear policy only for the directly supported locus and clearly distinguish **allele frequency** from **carrier prevalence** in the schema.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the natural `mangalarga_marchador` record with metadata, biomes, sources, price, rarity, genes, stat targets, and player-facing notes. |
| `common/breed/Breeds` | Register the breed ID for commands, wild spawning, breed books, the H-menu, genome loading, lineage display, and saved data migration. |
| `common/breed/BreedSource` | Validate the four requested sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Allow the explicitly empty epigenetic-band object. No shade, gait, or body-stat epigenetic band is required. |
| `common/breed/spec/` | Map this proposed JSON to the actual project schema in both reading and writing directions. Verify all locus keys, allele symbols, field names, source enums, and numeric-rate formats. |
| `common/breed/Commonness` | Confirm that `UNCOMMON` corresponds to the requested ladder rung and that `spawn_weight: 3` is consistent with its weighted biome-spawn implementation. |
| `common/breed/BreedStatCurve` | Translate speed 7, jump 5, health 8, and size ×0.98–1.10 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll each founder from the listed allele pools, force unlisted genes to wild type or clear, and apply the four stat target bands. |
| `common/breed/BreedLineage` | Use default pure/cross/Mixed behavior. The system must not infer “Mangalarga Marchador” solely from gait, color, or Brazilian biome origin. |
| `common/genetics/SpliceOutcome` | No MM exception. A successfully inherited splice-carrot allele can create a nonstandard descendant, as normal Mendelian gameplay intends. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize and generate the breed’s four target axes, including the medium riding-horse size range. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` use is needed unless an explicit empty/default band type is mandatory in the implementation. |

## Verification

1. Confirm **Mangalarga Marchador** appears in the H-menu’s Breeds tab and the breed book with the correct Brazilian identity, source checklist, uncommon rarity, biome list, medium riding-horse size, and marcha-focused notes.

2. Spawn repeated packs in eligible plains, savanna-edge, meadow, hill, and sparse-woodland biomes. All horses generated within the same selected pack must display the **Mangalarga Marchador** breed label.

3. Confirm an ordinary lone wild horse not generated through a breed pack displays **Feral Mixed**, not Mangalarga Marchador.

4. Generate at least 250 pure MM founders and inspect coat outcomes. The sample should contain many bays, chestnuts, blacks/browns, and grays; smaller numbers of palominos, buckskins, duns, roans, tobiano pintos, and sabino-marked horses; and no silver dapple, champagne, pearl, mushroom, leopard complex, frame overo, splash, W-series dominant white, brindle, or magical phenotypes.

5. Inspect genomes, not only rendered coats. Confirm that only the named loci can vary in a pure founder: extension, agouti, gray, cream, dun, flaxen, tobiano, sabino, roan, and rabicano. Every forced-wild-type locus should be genetically clear.

6. Confirm founder disorder panels are clear across a large sample. No listed inherited disorder should arise from two unmodified pure Mangalarga Marchador parents.

7. Test Mendelian segregation with intentionally selected pure-breed parents:
- `Cr/N × N/N` should yield approximately half cream carriers over a large foal set.
- `G/N × N/N` should yield approximately half gray foals.
- `TO/N × N/N` should yield approximately half tobiano inheritors.
- `SB1/N × N/N` should yield approximately half Sabino 1 inheritors, subject to the mod’s white-expression system.
- `Rn/N × N/N` should yield approximately half roan inheritors.

8. Test lineage behavior:
- Mangalarga Marchador × Mangalarga Marchador → Mangalarga Marchador.
- Mangalarga Marchador × Lusitano → Mangalarga Marchador cross.
- Mangalarga Marchador × Paso Fino or Tennessee Walking Horse → Mangalarga Marchador cross.
- Mangalarga Marchador cross × pure Mangalarga Marchador → that same cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Validate play feel. A mature MM should feel noticeably more capable at long, ordinary riding than a low-stamina pony, more durable than a fragile race specialist, and somewhat faster than a baseline horse—but it should not outclass specialist racers, jumpers, or heavy draft horses on their own axes. The unique *marcha* remains a breed-book/flavour identity unless the mod later adds gait mechanics.

## Sources

- [Fédération Equestre Internationale — Breed Profile: The Mangalarga Marchador](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador): eighteenth-century Brazilian development, Iberian and local ancestry, purpose, and the distinction between *marcha batida* and *marcha picada*. [fei](https://www.fei.org/stories/lifestyle/my-equestrian-life/breed-profile-mangalarga-marchador)

- [North American Mangalarga Marchador Association — MM Breed](https://namarchador.org/breed/): North American breed-association overview, 14.2–16-hand height range, common colors, broad history, and description of marcha mechanics. [namarchador](https://namarchador.org/breed/)

- [Horse Illustrated — The Treasure of Brazil: The Mangalarga Marchador](https://www.horseillustrated.com/horse-breeds-horse-breed-articles-the-treasure-of-brazil): Minas Gerais origin, ABCCMM registry context, conformation, traditional mane treatment, broad coat acceptance, and practical breed description. [horseillustrated](https://www.horseillustrated.com/horse-breeds-horse-breed-articles-the-treasure-of-brazil)

- [Spanish Horse Tack — The Mangalarga Marchador Horse](https://spanishhorsetack.com/spanish-horse-breeds/mangalarga-marchador-horse/): Brazilian development, average size, color range, inspection and permanent-registration process, and the two marcha types. [spanishhorsetack](https://spanishhorsetack.com/spanish-horse-breeds/mangalarga-marchador-horse/)

- [Campeãs da Gameleira — Brazilian breed history and ABCCMM registration practice](https://www.campeasdagameleira.com.br/ingles/brazilian_breed.php): establishment of the 1949 association, historical purpose of the breed standard, and inspection of horses aged at least three for conformation, gait, and temperament. [campeasdagameleira.com](https://www.campeasdagameleira.com.br/ingles/brazilian_breed.php)

- [Horse Isle 3 — Mangalarga Marchador breed reference](https://hi3.horseisle.com/www/bbb/Mangalarga_Marchador.php): supplementary confirmation of solid, sabino, and tobiano MM coat patterns, plus documented gray, dun, roan, cream-derived, and base colors. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Mangalarga_Marchador.php)