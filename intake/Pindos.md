The **Pindos** should be modeled as a rare Greek mountain horse—also called the **Pindos pony**—with compact build, extremely hard dark hooves, high heartiness, and a traditional coat pool centered on bay, chestnut, and gray. It should be a highland pack, farm, and trail mount: small and modest in raw speed or jumping, but outstandingly reliable over rocky terrain and on sparse forage. The most credible sources support a living but minority Greek native population, not an extinct relic or a visually fixed one-color breed. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

## Identity & flavour

The **Pindos**, often called the **Pindos Horse** or **Pindos Pony**, is Greece’s classic mountain horse from the Pindus mountain range. Its homeland runs through **Epirus, Thessaly, and Macedonia**, where steep slopes, limestone paths, forest tracks, cold winters, hot dry summers, and small mountain farms shaped a compact working animal rather than a show-ring specialist. The breed has developed in the Greek mountains over a long period of local use; its exact deep origins are not documented as a single founding event, but its identity as a distinct mountain type is ancient and culturally durable. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

The Pindos was made to go where carts, tractors, and large horses could not. It carried packs, wood, crops, supplies, and people; supported nomadic livestock systems; performed light agricultural work; and served rural communities in isolated terrain. During World War II, Pindos horses and ponies carried ammunition and supplies through the mountains during the Greco-Italian War and Greek resistance campaigns. Today, they still have a role in mountain access, light work, riding, and agrotourism. In Minecraft, the Pindos should feel like the horse you choose when the road ends: compact, frugal, calm, and capable over hostile hills. [amalthia](https://www.amalthia.org/en/breeds/horses/162-pindos)

Pindos horses usually stand about **120–140 cm**, roughly **11.3–13.3 hands**, though older descriptions sometimes cite a wider 110–135 cm population range. They are small but not delicate. A correct Pindos has a slender, expressive head with a straight profile; a long but strong neck; prominent withers; a narrow, compact body; a straight topline; strong hindquarters; thin but durable legs; and a tendency toward cow hocks that helps a mountain animal find purchase on uneven ground. The hooves are narrow, dark, and exceptionally hard. The mane is thick, the tail long, and there is no meaningful draft-style feathering. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

The real coat palette is restrained but varied. Bay is common, while chestnut, gray, black, brown, and occasional roan are reported; many descriptions emphasize bay, gray, and chestnut as the principal colors. The pure founder pool should therefore make bay the most frequent non-gray outcome, retain a meaningful chestnut population, permit black/dark bay, and include gray at a noticeable but subordinate rate. A low true-roan rate is defensible as a gameplay approximation because roan is reported in supplemental breed material, but pinto, leopard complex, cream, champagne, silver, pearl, mushroom, and bold white-pattern genetics should be absent from pure founders. [amalthia](https://www.amalthia.org/en/breeds/horses/162-pindos)

Pindos horses are described as calm, reliable, easy to train, frugal, steady-footed, and highly enduring. Their main value is not raw sprinting or tall sport-horse scope, but getting a rider or load safely through ground that defeats more elegant horses. In Procedural Horse Genetics, a player should recognize a Pindos as a short, dark-hoofed Greek mountain horse—usually bay, chestnut, or gray, with a thick mane and long tail—whose high health score hints at its real strength. The mod does not model hoof hardness, cow hocks, mountain grip, pack capacity, light draught strength, winter coat, thick mane, tail length, forage thrift, wartime logistics, temperament, Greek studbook inspection, or the real-world difference between a carefully registered Pindos and a local crossbred mountain pony. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention from the provided example. Before compiling, reconcile the actual locus IDs, allele names, source enum values, commonness ladder, price units, and body-stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** Available Greek breed and academic sources establish Pindos geography, height, roles, hardiness, and its bay/gray/chestnut-oriented palette. I did not locate a representative Pindos genotype survey providing exact `MC1R`, `ASIP`, `STX17`, or roan allele frequencies. The values below are transparent **gameplay approximations**, not official Greek studbook frequencies. The genetic-diversity literature supports treating Pindos as a distinct living population, but does not justify fabricating frequencies for the requested disease panel. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

```json
{
  "id": "pindos",
  "name": "Pindos",
  "type": "natural",
  "notes": "The Pindos, also called the Pindos Horse or Pindos Pony, is a Greek mountain breed from Epirus, Thessaly, and Macedonia. Its real identity includes dark narrow hard hooves, cow-hocked mountain conformation, thick mane, long tail, sparse-forage thrift, pack ability, light agricultural work, trail grip, wartime mountain transport, calm temperament, and Greek native-breed studbook status. Procedural Horse Genetics does not model hoof hardness, cow hocks, mountain traction, load carrying, draught force, mane thickness, tail length, forage efficiency, weather tolerance, wartime use, agrotourism training, temperament, or registry eligibility.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:meadow",
    "minecraft:grove",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 680,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "roan": {
      "N": 0.95,
      "Rn": 0.05
    },
    "flaxen": {
      "N": 0.94,
      "f": 0.06
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
    "jump": 4,
    "health": 9,
    "size": [
      0.78,
      0.91
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Pindos × Pindos produces Pindos. Pindos × another pure breed produces a Pindos cross. A Pindos cross bred back to a pure Pindos remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every short, hardy Greek-looking mountain horse as Pindos: real identity depends on native-breed lineage and registration, not size, bay color, dark hooves, or mountain biome alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Population model | Rare living Greek mountain breed | Pindos is one of Greece’s recognized indigenous breeds, with a reported historic census near 500 in one academic source and a larger 2019 registered-studbook figure cited by the Greek breed organization. The difference reflects time and data source, so `RARE` is more defensible than treating it as globally common.  [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.68 / a: 0.32` | Produces a population centered on bay/dark bay, chestnut, brown, and black. Bay, gray, and chestnut are named as primary colors; black and brown are also reported. Exact allele values are gameplay estimates.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| Gray | `G: 0.18` | Gray is a traditional documented Pindos color. Keeping it a minority preserves a strong bay/chestnut mountain-pony identity. This is a gameplay frequency, not a genotype survey result.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| Roan | `Rn: 0.05` | Roan is reported in supplemental Pindos material. It is retained at low frequency so it appears occasionally rather than becoming a defining trait.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| Flaxen | `f: 0.06` | Low flaxen permits occasional light-maned chestnut without making it a principal breed signature. This is a restrained gameplay approximation.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| True gray | `G`, not silver/cream/white | Gray Pindos foals should be born with pigmented base coats and progressively lighten. They must not render as silver dapple, cream, or genetically white. |
| Excluded dilutions | Cream, pearl, champagne, silver, mushroom, and dun forced wild type | The strong source-supported palette does not justify inserting these loci in a conservative pure Pindos pool. |
| Excluded white patterns | Tobiano, frame, Sabino 1, splash, KIT white, and rabicano forced wild type | The breed is not described as a pinto or conspicuously body-white population. Ordinary small incidental markings are not adequate grounds to seed strong named white-pattern loci. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type patterning is not part of the source-supported Pindos phenotype. |
| Magical loci | All forced wild type | High mountain endurance, hoof hardness, and sure-footedness are natural physical traits. |
| Disorders | All requested loci clear | No Pindos-specific carrier-frequency study was found for ACAN dwarfism, WFFS, frame lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `4/10` | Pindos is a practical mountain pack/farm horse, not a purpose-bred racing population. Its ability is endurance and footing, which the mod only partly captures through health.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| Jump | `4/10` | Mountain agility helps clear terrain, but does not justify a sport-jumping specialization. |
| Health | `9/10` | Represents frugality, tough bones and joints, strong hooves, mountain endurance, and harsh-environment adaptability—not immunity from injury or inherited disease.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php) |
| Size | `×0.78–0.91` | Covers the documented 120–140 cm contemporary breed range, while remaining compatible with older reports of a somewhat smaller population.  [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf) |

## Disorder approach

Every named disorder locus is intentionally **clear** in the pure Pindos founder pool.

This is a strict data-quality choice. Current research supports Pindos as a genetically distinct Greek native population with high observed heterozygosity in the cited comparison, but that research does not report carrier frequencies for the mod’s disease panel. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

Do not assign disease frequencies by analogy from other Mediterranean breeds, mountain ponies, Arabians, Quarter Horses, warmbloods, or neighboring Balkan populations. The absence of a rate does not prove that a mutation is absent in real Pindos horses; it means that adding one to the founder generator would be speculative.

The file therefore leaves clear:

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

A later Pindos-specific screening study can replace the all-clear policy for any directly tested mutation.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `pindos` as a natural Greek mountain-horse record with native-breed flavour, highland biome list, rare commonness, base-color/gray pool, clear disease policy, and compact hardy stats. |
| `common/breed/Breeds` | Register `pindos` for wild packs, stable access, spawn eggs, the H-menu, breed books, commands, save loading, founder generation, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Pindos is a regional Greek native/conservation breed, not an ordinary commercial sale-yard horse. |
| `common/breed/BreedBands` | Support the empty epigenetic-band object. Do not create fake bands for hoof hardness, thick mane, cow hocks, mountain grip, or forage thrift. |
| `common/breed/spec/` | Reconcile the illustrative JSON with the real format, including actual Extension, Agouti, gray, roan, and flaxen IDs; direct probability syntax; source enums; and defaults for omitted loci. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder and that `spawn_weight: 1.5` is valid if direct weighted spawning is supported. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 9, and size ×0.78–0.91 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll Extension, Agouti, gray, low roan, and low flaxen; force all dilution, pinto, leopard, magical, and named disorder loci wild type or clear; then apply compact mountain-work targets. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A short bay/gray horse on a mountain must not become Pindos based on phenotype or biome. |
| `common/genetics/SpliceOutcome` | No Pindos-specific exception. A spliced allele transmits normally and can produce a nonstandard descendant with cream, pinto, leopard, disease, or large-size genetics excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the small, high-heartiness, modest-speed, modest-jump mountain-horse target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is necessary unless the actual serializer requires a default field. |

## Verification

1. Confirm **Pindos** appears in the H-menu’s Breeds tab and breed book with Greek Pindus origin, rare commonness, compact mountain-work identity, dark-hoof/forage-thrift caveat, and correct source checklist.

2. Spawn repeated packs in windswept hills, gravelly hills, meadow, grove, forest edge, stony peaks, jagged peaks, and river valleys. Every horse in one selected Pindos pack must display the **Pindos** label.

3. Confirm that a lone unassigned wild horse reads **Feral Mixed**, even if it is short, bay, gray, sure-footed, or generated in mountain terrain.

4. Generate at least 1,000 pure founders. The cohort should be dominated by bay/dark bay and chestnut, with meaningful gray and smaller black/brown populations; roan and flaxen-chestnut expression should be uncommon.

5. Confirm that pure Pindos founders do **not** generate cream dilution, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, pinto, frame overo, splash, dominant white, leopard complex, brindle, or magical effects.

6. Inspect genome panels:
- `E/e` and `A/a` should create the core bay/chestnut/black/brown palette.
- `G` should produce a visible but minority progressive-gray population.
- `Rn` and `f` should remain rare.
- All other named coat genes and all disease loci must be wild type or clear.

7. Test true gray:
- `G/N` foals should be born pigmented and then progressively lighten.
- `G/G` and `G/N` adults should gray normally.
- A blue roan must not be mistaken for gray, and a gray horse must not be rendered as silver dapple.

8. Confirm mature stats. Pindos horses should remain clearly pony-to-small-horse sized, below average in raw sprint and jumping specialization, but distinctly high in health/heartiness. They should feel useful on difficult terrain without becoming mechanically superior to purpose-bred endurance, race, or jumping horses.

9. Test default lineage:
- Pindos × Pindos → Pindos.
- Pindos × Thessaly Horse → Pindos cross.
- Pindos × Skyros Pony → Pindos cross.
- Pindos cross × pure Pindos → the existing Pindos cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test controlled introduction of an excluded locus. Cross in cream, tobiano, leopard complex, draft size, or a named disease allele. Descendants should inherit normally but must not acquire a pure Pindos label merely because they remain small or live in the mountains.

## Sources

- [Amalthia — Pindos](https://www.amalthia.org/en/breeds/horses/162-pindos): Greek animal-genetic-resource source for Pindos homeland, history, wartime use, 120–140 cm size, conformation, thick mane, long tail, hard dark hooves, colors, forage thrift, sure-footedness, present work/riding roles, and 2019 registered-population figure. [amalthia](https://www.amalthia.org/en/breeds/horses/162-pindos)

- [Greek native horse genetic diversity study](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf): academic source for Pindos geographic range, 110–135 cm historical height range, approximately 500-animal census context, and high genetic-diversity estimates. [brill](https://brill.com/downloadpdf/edcollchap-oa/book/9789086869404/BP000043.pdf)

- [Indigenous Greek Horse Breeds: Genetic Structure and the Importance of Conservation](https://www.mdpi.com/2077-0472/15/5/540): current genetic-structure and conservation context for Pindos among Greece’s indigenous horse breeds. [mdpi](https://www.mdpi.com/2077-0472/15/5/540)

- [Pindos Pony reference](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php): supplementary structured description of Pindos mountain uses, conformation, bay/black/gray colors, occasional roan/flaxen modifiers, and pony height. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Pindos_Pony.php)