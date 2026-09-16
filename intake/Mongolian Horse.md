The **Mongolian Horse** should be treated as a broad, regional landrace rather than a closed, visually uniform studbook breed: small and stocky, extraordinarily hardy, fast enough for long steppe travel, and genetically varied in coat color. Its defining gameplay trait should be survival under severe seasonal conditions—high heartiness, pony-to-small-horse size, sturdy feet, and endurance—while its wide coat pool reflects semi-feral herd breeding and strong regional/color preferences rather than a single registry-enforced phenotype. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

## Identity & flavour

The **Mongolian Horse**, also called the **Mongol horse**, is Mongolia’s native horse population: a family of related local steppe horse types rather than one modern, narrowly standardized international breed. It has developed for centuries alongside pastoral life on the Mongolian Plateau, where horses live outdoors year-round, find much of their own forage, and must withstand fierce wind, limited winter feed, summer heat, snow, drought, and long travel across open grassland. There is no single formal breed registry governing all Mongolian horses; natural selection, regional practices, and pastoral utility have shaped them more strongly than a show-ring standard. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

Mongol horses are inseparable from Mongolia’s history. They carried herders, transported camps and goods, helped manage livestock, and supplied mares’ milk for *airag*. They were central to the thirteenth-century Mongol Empire’s mobility: mounted forces depended on hardy remounts capable of traveling, grazing, and recovering in environments where larger imported horses would struggle. Today, horse racing remains one of the “Three Manly Games” of **Naadam**, with children traditionally riding young horses across long open-country courses. A Mongolian horse in the mod should therefore feel like an all-purpose living partner for survival, herding, travel, and distance—not a modern specialist optimized for one arena discipline. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

Most Mongolian horses stand about **12–14 hands**—approximately 122–142 cm—and weigh around **500–600 lb**. They are stocky for their height, with a large, broad-headed, often Roman-nosed profile; a short, powerful neck; a deep barrel; short but strong legs; dense bone; and exceptionally hard, firm hooves. They grow a thick winter coat and carry a heavy mane and tail, practical traits for a horse expected to remain outdoors through hard weather. Regional types differ: desert horses tend to have relatively large feet, mountain horses are shorter and stronger, and steppe horses are generally taller and faster. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

There is no single “correct” Mongolian coat. Bay, black, dun, chestnut, gray, roan, palomino, and white horses all occur, with regional and cultural preferences influencing which colors are retained. Dun, bay, and black are commonly favored across much of Mongolia, while white horses have special importance among the Darkhad people. That means a pure Mongolian founder pool should be visibly diverse, but still earth-toned and steppe-plausible: ordinary bay, black, chestnut, and dun should dominate, with gray, cream, roan, and occasional white-pattern genetics present at lower rates. It should not behave like a leopard-spotted Appaloosa population or a champagne-heavy American gaited breed. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

Mongol horses are famously hardy, independent, sure-footed, intelligent, and enduring. They can gallop long distances despite their modest size, and traditional management assumes they will forage, socialize, and cope without constant intensive care. For Procedural Horse Genetics, players should breed toward a compact, resilient steppe horse with a wide but restrained natural palette, strong health, good practical speed, and neither extreme jumping nor modern sport-horse height. The mod does not model mare-milking suitability, airag production, winter coat density, parasite resistance, regional tribe-specific selection, survival under *dzud* winters, large-foot desert adaptation, Roman nose, hoof hardness, rider skill, endurance conditioning, herd social structure, Naadam race training, or a horse’s ability to locate forage under snow.

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. The following uses the readable JSON convention in your Anglo-Arabian example; reconcile exact key names, allele symbols, source IDs, rarity enum names, price units, and body-target serialization with `common/breed/spec/` before compiling.
>
> **Scientific-rate caveat:** No population-wide Mongolian Horse genomic survey was located that gives defensible allele frequencies for all coat loci in the requested system. The sources support a genuinely broad coat range and cultural preference for dun, bay, and black, but they do not justify invented precision. The listed coat rates are therefore transparent **gameplay approximations**, not claimed national allele frequencies. All listed disorder loci are clear because no Mongolian-Horse-specific carrier rates were located.

```json
{
  "id": "mongolian_horse",
  "name": "Mongolian Horse",
  "type": "natural",
  "notes": "The Mongolian Horse is a broad native landrace shaped by pastoral use, regional populations, year-round outdoor life, natural selection, and Mongolia’s severe steppe climate. Procedural Horse Genetics does not model regional Mongolian types, winter coat depth, dzud survival, forage-finding under snow, airag production, tribal color preferences, Roman nose, hoof hardness, large-foot desert adaptation, Naadam race training, endurance conditioning, herd behavior, livestock work, or rider skill.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:meadow",
    "minecraft:stony_peaks",
    "minecraft:grove",
    "minecraft:badlands",
    "minecraft:wooded_badlands"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 380,
  "commonness": "COMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.64,
      "a": 0.36
    },

    "dun": {
      "N": 0.68,
      "D": 0.32
    },
    "grey": {
      "N": 0.87,
      "G": 0.13
    },
    "cream": {
      "N": 0.91,
      "Cr": 0.09
    },
    "silver": {
      "N": 0.98,
      "Z": 0.02
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
      "N": 0.96,
      "TO": 0.04
    },
    "sabino_1": {
      "N": 0.97,
      "SB1": 0.03
    },
    "frame_overo": {
      "N": 1.0
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
    "roan": {
      "N": 0.90,
      "Rn": 0.10
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
    "speed": 6,
    "jump": 4,
    "health": 10,
    "size": [
      0.82,
      0.94
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Mongolian Horse × Mongolian Horse produces Mongolian Horse. Mongolian Horse × another pure breed produces a Mongolian Horse cross. A Mongolian Horse cross bred back to pure Mongolian Horse remains that cross under the default system. Different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Mongolian Horse solely from an unowned, hardy, short horse in a steppe biome: real Mongolian horses are a cultural and regional landrace, not simply generic feral ponies."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Broad landrace rather than a fixed visual type | Mongolian horses are managed as regional native populations, with no single formal registry and substantial environmental and cultural variation.  [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.64 / a: 0.36` | Generates a foundation of bay, black, brown, and chestnut. Bay, black, and dun are repeatedly reported as common or culturally favored. Exact rates are gameplay approximations.  [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse) |
| Dun | `D: 0.32` | Dun is one of the repeatedly favored and recognizable Mongolian color families. It should be common enough to make primitive markings familiar without turning the breed into a Konik clone.  [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse) |
| Gray | `G: 0.13` | Gray occurs within the breed’s broad palette but should be a minority relative to bay, black, and dun in a general national founder pool.  [madbarn](https://madbarn.ca/mongolian-horse-breed-guide/) |
| Cream | `Cr: 0.09` | Palomino and buckskin-type horses occur among Mongolian colors. A modest cream rate provides them without falsely making diluted gold coats dominant.  [madbarn](https://madbarn.ca/mongolian-horse-breed-guide/) |
| Silver | `Z: 0.02` | Retained only as a very low-frequency visual possibility in a broad, unconstrained landrace. This is not a documented Mongolian allele frequency; remove it if the mod treats `Z` as too medically consequential without linked MCOA logic. |
| Flaxen | `f: 0.08` | Allows occasional flaxen chestnut variation. This is a low gameplay approximation, not a measured Mongolian frequency. |
| Tobiano, sabino, splash | Low rates | Occasional white/pinto variation is plausible in an open landrace with white horses valued in at least some regional populations, but loud white should remain rare in an average steppe herd. These are gameplay approximations rather than published locus frequencies.  [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse) |
| Roan | `Rn: 0.10` | Roan is listed among the broad color range; a visible minority gives the population realistic variety without overriding the earth-toned core.  [madbarn](https://madbarn.ca/mongolian-horse-breed-guide/) |
| Excluded loci | Champagne, pearl, mushroom, frame, W-series white, leopard complex, PATN, brindle, magical loci forced wild type | No source found supports treating those loci as intrinsic to a generalized Mongolian Horse founder pool. |
| Disorders | All listed loci clear | No credible Mongolian-Horse-specific carrier prevalence was located for the requested disease loci. Importing rates from unrelated breeds would not be scientifically accurate. |
| Speed | `6/10` | Mongolian horses are not tall racers, but their practical steppe speed and ability to gallop repeatedly over distance justify above-baseline travel speed.  [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse) |
| Jump | `4/10` | Sure-footedness and rough-country agility do not equal specialist jumping selection. |
| Health | `10/10` | Represents the breed’s exceptional climate tolerance, self-sufficient forage behavior, strong hooves, and year-round survival. It does not imply that every real horse is disease-free or that all injuries are prevented.  [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses) |
| Size | `×0.82–0.94` | Represents a broad 12–14-hand native range, including smaller mountain/desert types and taller steppe animals.  [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse) |

## Disorder approach

All named disease loci are intentionally **clear** in the pure Mongolian Horse pool. This is the scientifically responsible choice with the available evidence.

The absence of a seeded allele does not mean Mongolian horses are immune to inherited disease. It means no source located supplied a defensible population frequency for the specific mutations requested: ACAN dwarfism, PLOD1 dwarfism, MET/EDNRB lethal white syndrome, Arabian-line SCID/CA/LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

In particular, `GYS1` PSSM1 is a dominant disease-associated mutation—not a harmless recessive “carrier” state—and should not be inserted into a founder pool without breed-specific positive data. General references note that PSSM1 prevalence varies sharply across breeds and that affected heterozygotes can transmit the variant to about half their offspring. [uu](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the natural `mongolian_horse` record, including its broad landrace notes, sources, biomes, commonness, price, genetics, and stat targets. |
| `common/breed/Breeds` | Register the ID for spawning, menus, books, save loading, breeding, and lineage display. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable` as the permitted source checklist. |
| `common/breed/BreedBands` | Support an empty epigenetic band object. Do not use a coat-shade band to imitate thick winter coat, which is not modeled by pigment genes. |
| `common/breed/spec/` | Match the readable sample JSON to the actual schema, particularly allele notation for cream, gray, dun, white patterns, magical loci, and direct numeric probabilities. |
| `common/breed/Commonness` | Confirm that `COMMON` maps to the requested rarity ladder and that `spawn_weight: 6` is the intended moderate biome weight. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 4, health 10, and size ×0.82–0.94 to legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll founders from the broad stated coat pool; force all unlisted genes wild type/clear; guarantee body-stat pushing alleles for the four specified targets. |
| `common/breed/BreedLineage` | Apply the default pure/cross/Mixed table. Do not equate “wild” with Mongolian Horse; unassigned wild horses must remain Feral Mixed. |
| `common/genetics/SpliceOutcome` | No special breed rule. A spliced allele can produce a nonstandard descendant, consistent with normal Mendelian inheritance. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the small-size and survival-focused body-stat targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is required unless the concrete spec mandates a default value. |

## Verification

1. Confirm **Mongolian Horse** appears in the H-menu’s Breeds tab and the breed book with the Mongolia/steppe description, broad color palette, full source checklist, commonness, small size, and very high health target.

2. Spawn repeated packs in windswept hills, plains, meadow, dry savanna, plateau, and stony upland biomes. Every horse in a selected pack must have the **Mongolian Horse** label.

3. Confirm a lone wild horse outside the breed-pack generator still reads **Feral Mixed**, regardless of whether it happens to be dun, bay, small, or hardy.

4. Generate at least 500 pure Mongolian Horse founders. The resulting population should be dominated by bay, black/brown, chestnut, and dun-based horses, with minority gray, palomino/buckskin, roan, and restrained pinto/white-marked horses.

5. Confirm that no pure founder generates champagne, pearl, mushroom, frame-overo, W-series dominant white, leopard complex, PATN spotting, brindle, or magical phenotypes.

6. Check the genome panels. Only the explicitly listed coat loci may vary; all requested disorder loci must remain clear across a large founder sample.

7. Measure mature body stats. The population should remain clearly smaller than ordinary riding horses, noticeably tough and durable, and practically fast for overland travel, without becoming a high-jump sport breed or a Thoroughbred-equivalent racehorse.

8. Test default lineage:
- Mongolian Horse × Mongolian Horse → Mongolian Horse.
- Mongolian Horse × Przewalski-style feral or another pure horse → Mongolian Horse cross.
- Mongolian Horse cross × pure Mongolian Horse → the same existing cross label.
- Two different cross labels → Mixed.
- Any cross with Feral Mixed → Mixed.

9. Test intentional outcrossing. Introduce gray, leopard complex, champagne, draft size, or a sport-horse jump target through another breed. The offspring should become a cross rather than remain “pure Mongolian,” preserving the distinction between native landrace lineage and a generic hardy steppe phenotype.

## Sources

- [Oklahoma State University — Mongolian Horses](https://breeds.okstate.edu/horses/mongolian-horses): native status, year-round outdoor management, Mongol Empire role, average male/female height, stocky conformation, robust hooves, regional color preferences, and endurance. [breeds.okstate](https://breeds.okstate.edu/horses/mongolian-horses)

- [Mongolian Horse breed overview](https://en.wikipedia.org/wiki/Mongolian_horse): supplementary compilation of 12–14-hand height, regional variation, rugged conformation, steppe endurance, historic transport capacity, winter-adaptation preferences, and cultural color selection. [en.wikipedia](https://en.wikipedia.org/wiki/Mongolian_horse)

- [MadBarn — Mongolian Horse Breed Guide](https://madbarn.ca/mongolian-horse-breed-guide/): no single official registry, semi-feral herd management, broad coat range, conformation, practical roles, and endurance context. [madbarn](https://madbarn.ca/mongolian-horse-breed-guide/)

- [Utrecht University — PSSM in horses: scientific facts](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts): scientific explanation of `GYS1` PSSM1 inheritance, dominant expression, and the need to distinguish PSSM1 from poorly defined PSSM2 genetic claims. [uu](https://www.uu.nl/en/news/pssm-in-horses-a-summary-of-the-scientific-facts)

- [Kentucky Equine Research — PSSM1](https://ker.com/nmdl/resources/pssm-1/): general PSSM1 inheritance and prevalence context, used only to justify not inventing a Mongolian founder frequency. [ker](https://ker.com/nmdl/resources/pssm-1/)