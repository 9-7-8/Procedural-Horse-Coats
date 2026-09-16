The **Domestic Mountain Pony**, locally **Domaći brdski konj**, should be filed as the **Bosnian Mountain Horse**: Bosnia and Herzegovina’s indigenous small mountain horse, selected for carrying people and packs through the Dinaric highlands. It has unusually valuable coat data: modern genotyping found dark bay at 44%, black at 36%, bay at 11%, and dun at 8%, with the true-dun `D` allele at 0.09; those figures allow a more scientifically grounded founder pool than most rare mountain breeds. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

## Identity & flavour

The **Domestic Mountain Pony**, Bosnian *Domaći brdski konj*—more commonly rendered in English as the **Bosnian Mountain Horse**—is the sole indigenous domestic horse breed of Bosnia and Herzegovina. It is also called the Bosnian Pony, Bosnian Mountain Pony, *Bosanski brdski konj*, or Bosnian Mountain Horse. The breed belongs to the rugged Dinaric mountain landscape of Bosnia: steep limestone ridges, forest tracks, mountain pastures, cold winters, hot dry summers, and villages long connected by narrow routes rather than wide roads. It is not a decorative pony created for show; it is a small, serious work horse shaped by survival and usefulness. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

The breed was made for packs, riding, farm work, and mountain transport. A Bosnian Mountain Horse could carry wood, crops, supplies, people, and military loads across ground unsuitable for wheeled vehicles. Selective breeding began at Goražde in 1908, later concentrated at the Borike and Han Pijesak studs. Breeders established stallion lines named **Agan**, **Barut**, and **Miško**, plus nine mare lines; Arab stallions were introduced at Borike to improve the horse, while retaining compact size and mountain durability. The breed has both lighter riding-oriented and heavier pack-oriented types, but both should remain recognizably short, sturdy, and sure-footed. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

A Domestic Mountain Pony generally stands about **130–145 cm**, or roughly **12.3–14.1 hands**, and weighs about **300–380 kg**. It has a dry, often straight-profiled head; alert eyes; small mobile ears; a short, strong neck; low to moderate withers; deep chest; compact, powerful body; short back; muscular sloping croup; short durable legs; and small, dark, exceptionally hard hooves. It should have little to no true feathering. Mane and tail are thick and practical rather than refined or unusually long. In game, it should look like a compact Balkan mount with enough body for a pack saddle, not a tiny show pony or a long-legged sport horse. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

The current breeding picture is much darker and more genetically specific than generic “mountain pony” artwork suggests. In a modern study of 313 Bosnian Mountain Horses, **dark bay** accounted for 44%, **black** 36%, ordinary bay 11%, and dun 8%. The study found the true-dun `D` allele at frequency **0.09**, higher than older register-based prediction. This means the correct pure pool is dark-pigment dominated: black and dark bay first, ordinary bay second, genuine dun uncommon but unmistakable. Chestnut, progressive gray, pinto, and spotted horses are excluded by the cited breeding program; cream dilution should likewise remain absent in the strict current founder pool. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

The Domestic Mountain Pony should feel alert, reliable, tough, frugal, fertile, and almost absurdly capable for its height. In Procedural Horse Genetics, it gives players a highland survival breed whose reward is not huge speed or jumping scope but a small, dark, hard-hoofed pack horse with exceptionally high health. Players should recognize it by its dark bay/black core, compact build, and occasional primitive-marked dun. The mod does not model hoof hardness, mountain traction, carrying capacity, Arab-improvement history, Barut/Miško lineages, exact wither height, winter coat density, fertility, nutrition efficiency, regional conservation work, or the difference between riding and heavier pack types. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This follows the readable JSON convention from the example in the request. Before compiling, align field names, source enum values, `E/e`, `A/a`, `D/nd1/nd2`, and stat-band syntax with `common/breed/spec/`.
>
> **Genetic-method note:** The modern study reports phenotype frequencies and `D` allele frequency, but not a full directly published `MC1R` Extension allele table. The Extension and Agouti values below were solved as an approximate Hardy–Weinberg model that reconstructs the reported 44% dark bay, 36% black, 11% bay, and 8% dun distribution as closely as the simplified mod locus set permits. `D: 0.09` is directly grounded in the study; `E: 0.99` and the multi-allelic Agouti pool are gameplay-compatible estimates. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

```json
{
  "id": "domaci_brdski_konj",
  "name": "Domestic Mountain Pony",
  "type": "natural",
  "notes": "The Domestic Mountain Pony, Domaći brdski konj or Bosnian Mountain Horse, is Bosnia and Herzegovina's indigenous mountain pack, riding, and farm horse. Its real identity includes hard dark hooves, compact Dinaric mountain conformation, short powerful legs, carrying ability, mountain traction, thick mane and tail, sparse-forage thrift, high fertility, Arab-improvement history, the historic Agan, Barut, and Miško stallion lines, and distinct light-riding versus heavier pack types. Procedural Horse Genetics does not model these traits directly.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:meadow",
    "minecraft:grove",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:dark_forest",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 700,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.99,
      "e": 0.01
    },
    "agouti": {
      "At": 0.63,
      "A": 0.19,
      "a": 0.18
    },

    "dun": {
      "D": 0.09,
      "nd1": 0.15,
      "nd2": 0.76
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
    "health": 10,
    "size": [
      0.84,
      0.97
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Domestic Mountain Pony × Domestic Mountain Pony produces Domestic Mountain Pony. Domestic Mountain Pony × another pure breed produces a Domestic Mountain Pony cross. A Domestic Mountain Pony cross bred back to pure Domestic Mountain Pony remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign the breed merely because a horse is short, dark, hard-hoofed, or found in a mountain biome: real Bosnian Mountain Horse identity depends on Bosnian breed lineage and breeding-program criteria."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Name and ID | `domaci_brdski_konj` / “Domestic Mountain Pony” | Uses the requested local Bosnian name as the implementation ID while retaining an accessible display name. “Bosnian Mountain Horse” should be included as the primary alias in the breed book.  [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse) |
| Population model | Compact Bosnian highland pack horse | Bosnia and Herzegovina’s only indigenous domestic horse breed, used for riding and packs; it comprises both lighter and heavier practical types.  [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse) |
| Dark pigment | `E: 0.99` | The current study found dark bay and black together in 80% of the sample. A near-fixed black-pigment extension allele reflects that observed population, while `e` remains only as a tiny fallback due to modeling uncertainty.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Agouti complexity | `At: 0.63`, `A: 0.19`, `a: 0.18` | The study separates **dark bay** from ordinary bay and black. If the mod supports `At`/seal-brown-style Agouti, this pool targets dark bay as the dominant phenotype, ordinary bay as a smaller class, and black through `a/a`. If the mod supports only `A/a`, see the compatibility fallback below.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| True dun | `D: 0.09` | This is directly supported: the study reports the dominant true-dun allele at 0.09, higher than the older registry expectation.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| `nd1` / `nd2` | `nd1: 0.15`, `nd2: 0.76` | `nd1` permits some primitive marking without full dun dilution, useful for a Balkan mountain breed in which visual classification proved inconsistent. Only `D` should create full dun body dilution. The `nd1` estimate is gameplay texture, not directly reported.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Excluded colors | Gray, cream, champagne, silver, pearl, mushroom, flaxen all forced wild type | The cited breeding program allows all colors **except gray, pinto, chestnut, and spotted**, without white markings. Fixing these loci preserves the dark bay/black/bay/dun population.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| White and spotting | All white-pattern loci forced wild type | The cited program excludes pinto and spotted horses and requires no white markings. Do not seed tobiano, frame, splash, Sabino 1, dominant-white, roan, rabicano, or leopard-complex loci.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Chestnut | Functionally absent | Chestnut is excluded by the breeding program. `e: 0.01` makes chestnut exceedingly rare in random founders; set `e: 0.0` if the mod requires strict phenotype compliance instead of preserving tiny genetic uncertainty.  [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405) |
| Disorders | All named loci clear | No Bosnian Mountain Horse-specific carrier-frequency data was found for the requested disease panel. Do not infer disease loci from mountain hardiness, Arabian improvement crosses, or Balkan geography. |
| Speed | `4/10` | Practical mountain travel and pack work, not speed selection. |
| Jump | `4/10` | Sure-footedness helps terrain travel but does not support an elite jumping score. |
| Health | `10/10` | Represents exceptional hardiness, fertility, rough-country resilience, and low-input survival—the closest available stat proxy for mountain utility.  [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse) |
| Size | `×0.84–0.97` | Represents the 130–145 cm, 12.3–14.1-hand small-horse range without making it a miniature.  [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse) |

### Two-allele Agouti fallback

If the mod implements only `A/a` at Agouti and has no `At` or dark-bay/seal-brown state, use this simpler, less exact substitute:

```json
"extension": {
  "E": 0.99,
  "e": 0.01
},
"agouti": {
  "A": 0.74,
  "a": 0.26
}
```

That fallback will yield mostly ordinary bay plus black and will **not** fully recreate the study’s dark-bay distinction. The better scientific choice is to use the mod’s existing dark-bay/sooty/shade machinery, if available, rather than pretending that dark bay is simply ordinary `A_` bay.

## Disorder approach

Every requested disorder locus is **clear** in pure Domestic Mountain Pony founders.

The sources include unusually good coat-color data and historical breed information, but no defensible carrier-frequency screen for:

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

The breed’s Arab improvement history does **not** justify importing Arabian SCID, CA, or LFS frequencies. Nor does its sturdy mountain type justify assigning it a draft-breed PSSM1 rate. Clear founders are more scientifically defensible than invented ones. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `domaci_brdski_konj` as a natural Bosnian mountain breed with local aliases, dark coat pool, fixed clear disorders, highland biomes, and compact survival-focused stat bands. |
| `common/breed/Breeds` | Register the ID for wild packs, stable generation, spawn eggs, H-menu listing, breed books, commands, saved genomes, and lineage output. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because it is a regional mountain breed, not an ordinary commercial sale-yard horse. |
| `common/breed/BreedBands` | Keep the object empty. Do not use an epigenetic band to fake dark bay, hoof hardness, mountain grip, winter coat, or pack strength unless the codebase has a specific scientifically appropriate trait. |
| `common/breed/spec/` | Verify whether Agouti supports `At`; verify exact `D/nd1/nd2` syntax; ensure forced-clear disease and wild-type coat values match the true schema. |
| `common/breed/Commonness` | Confirm `UNCOMMON` corresponds to the intended rarity ladder and retains direct `spawn_weight: 3` if that field is valid. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 10, and size ×0.84–0.97 into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Roll from the dark-pigment Extension/Agouti pool, use direct `D: 0.09`, keep all forbidden color and disease loci clear, and apply compact highland body-stat targets. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed behavior. A dark bay or black mountain horse should not receive the local Bosnian lineage label just from phenotype. |
| `common/genetics/SpliceOutcome` | No exception. A spliced allele can transmit normally, allowing nonstandard gray, chestnut, pinto, leopard, or disease-carrying descendants outside the strict founder pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize small body size, low specialist speed/jump, and maximum heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual serializer requires an explicit default. |

## Verification

1. Confirm **Domestic Mountain Pony** appears in the H-menu’s Breeds tab and the breed book under `domaci_brdski_konj`, with **Bosnian Mountain Horse** and *Domaći brdski konj* visible as aliases.

2. Spawn repeated packs in meadow, forest, grove, windswept hills, rocky hills, stony peaks, and river-valley biomes. Every horse generated in an individual selected pack must display the **Domestic Mountain Pony** lineage label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is black, dark bay, compact, or standing on a mountain.

4. Generate at least 1,000 pure founders. If the renderer supports dark bay correctly, the cohort should approximate:
- About 44% dark bay.
- About 36% black.
- About 11% ordinary bay.
- About 8% true dun across the base-color groups.
- Essentially no chestnut, gray, pinto, or spotted horses.

5. Confirm full-dun inheritance:
- Founder `D` frequency should be approximately 0.09.
- A `D` horse should display true body dilution plus primitive markings.
- `nd1` should not dilute the body like `D`.
- `nd2` should not create full primitive-dun expression.
- Do not substitute the magical zebra locus for genuine `TBX3` behavior.

6. Confirm all pure founders are clear of gray, cream, pearl, champagne, silver, mushroom, flaxen, tobiano, Sabino 1, frame, splash, KIT white, roan, rabicano, leopard complex, `PATN1`, `PATN2`, brindle, magical loci, and every named disorder locus.

7. Confirm mature stats. Horses should remain compact and short-legged, moderately slow, modest at jumping, and among the mod’s highest-health mounts. They should feel ideal for harsh hills, not as elite racers or sport jumpers.

8. Test default lineage:
- Domestic Mountain Pony × Domestic Mountain Pony → Domestic Mountain Pony.
- Domestic Mountain Pony × Bosnian-related or Balkan breed → Domestic Mountain Pony cross.
- Domestic Mountain Pony cross × pure Domestic Mountain Pony → the existing cross label.
- Two distinct crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test deliberate outcrossing or splice changes. Introduce chestnut, gray, pinto, leopard complex, cream, or a disease allele. Descendants should inherit the gene normally but remain nonstandard for strict real-world breeding-program flavour and must not gain pure Domestic Mountain Pony status from their body size or mountain spawn biome.

## Sources

- [Bosnian Mountain Horse overview](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse): supplementary structured source for local names, Bosnia and Herzegovina status, 1908 selective breeding, Borike/Han Pijesak studs, Arab improvement, historic stallion and mare lines, two practical types, uses, and historical coat survey. [en.wikipedia](https://en.wikipedia.org/wiki/Bosnian_Mountain_Horse)

- [Variation in the `ASIP` and `DUN` genes responsible for coat colour in Bosnian Mountain Horse](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405): primary study of 313 Bosnian Mountain Horses reporting dark bay 44%, black 36%, bay 11%, dun 8%, direct `D` allele frequency 0.09, phenotype/genotype discrepancies, and the breeding-program exclusion of gray, pinto, chestnut, spotted, and white-marked horses. [slovetres](https://www.slovetres.si/index.php/SVR/article/download/1810/682/7405)

- [Rewilding Europe—Bosnian mountain horses](https://rewildingeurope.com/news/five-more-hardy-bosnian-mountain-horses-now-reinforce-the-wild-living-herd-in-velebit/): supplementary conservation source for the breed’s hardiness, fertility, low nutritional demands, outdoor management, and use in semi-wild landscape grazing. Treat its Tarpan/Przewalski ancestry language as historical claim rather than settled genomics. [rewildingeurope](https://rewildingeurope.com/news/five-more-hardy-bosnian-mountain-horses-now-reinforce-the-wild-living-herd-in-velebit/)