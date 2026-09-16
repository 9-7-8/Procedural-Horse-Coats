The **Boerperd** should be represented as the **SA Boerperd**, a South African Cape-derived, multi-purpose gaited saddle horse: muscular, sure-footed, drought-hardy, and broad in ordinary solid coat colors. Its defining gameplay identity is a calm but responsive “farmer’s horse” that can travel comfortably and work hard over rough ground, with a frequent natural ambling/racking gait that the mod should describe but not fake as a coat or stat gene. Pure founders should be solid bay, brown, black, chestnut, gray, buckskin, palomino, smoky black, roan, or rare dun—never pinto, leopard-spotted, or magically colored. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

## Identity & flavour

The **SA Boerperd**, commonly called the **Boerperd**, is an indigenous South African saddle horse descended from the historic Cape Horse. *Boerperd* means “farmer’s horse” in Afrikaans, which describes its purpose perfectly: it was shaped by settlers and farmers who needed one durable animal for riding, light farm work, transport, harness, herding, hunting, and long travel across the Cape and interior veld. Its ancestry begins with horses brought to the Cape of Good Hope from Java and elsewhere in the mid-seventeenth century, including Barb-Arab and Persian-Arab type horses, later joined by Andalusian, Flemish, Hackney, Norfolk Trotter, Cleveland Bay, Thoroughbred, and Criollo influence. The recognizable Boerperd formed through the eighteenth and nineteenth centuries; the modern organized breed societies came later. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

The Boerperd’s defining environment is the South African landscape: hot dry summers, cold winters, rough veld, long distances, uneven terrain, and variable forage. It was valued as a cavalry and remount horse during the Anglo-Boer Wars, when the Cape-derived horse’s endurance, intelligence, sure footing, and ability to maintain condition on modest feed became legendary. After the war, the population faced severe loss and crossbreeding pressure, prompting preservation efforts. The Kaapse Boerperd Breeders’ Society formed in 1948; a separate Boerperd society formed in 1973, became the Historiese Boerperd Breeders Society in 1977, and was officially recognized in South Africa in 1996 before becoming SA Boerperd in 1998. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

A Boerperd is a medium-sized but strong horse, commonly **14.2–16 hands**, with a minimum registration height around 13.3 hands for mares and 14.2 hands for stallions. It is robust without clumsiness: broad, flat forehead; large prominent eyes; straight to lightly concave profile; medium to well-formed neck; sloping shoulder; strong, muscular body; deep chest; rounded hindquarters; sturdy legs; and large, hard, well-shaped hooves. Its mane and tail can be long and sometimes wavy, but there is no heavy feathering. The overall silhouette should be a handsome, sturdy southern African saddle horse—more muscular and substantial than the refined Cape Boerperd, but still active and elegant enough for riding or harness. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

The SA Boerperd is not color-fixed, but it is not a pinto breed. Documented solid coat colors include bay, brown, black, chestnut, gray, buckskin, palomino, smoky black, roan, and rare dun, with flaxen, sooty, rabicano, and dark-mane or gray-mane modifiers also noted. The skin must remain darkly pigmented, and large blazes, stockings above the knee or hock, and pinto-like markings are undesirable or barred by breed standards. This makes the founder pool broad but tidy: base color, cream, gray, dun, roan, flaxen, and rabicano are all useful; tobiano, frame, splash, Sabino 1, dominant white, and leopard complex should remain wild type. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

Boerperds are known for intelligence, courage, calmness, willingness, endurance, hardiness, and impressive sure-footedness. Many are naturally **five-gaited**, with a comfortable four-beat rack or amble in addition to the normal walk, trot, canter, and gallop; some individuals are not gaited. In Procedural Horse Genetics, a Boerperd should be a player’s dependable veld mount—medium-sized, high-hearted, responsive, practical, and varied in solid color without being flashy-pinto. The mod does not model the rack, amble, fifth gait, gait comfort, head carriage, wavy mane, hoof hardness, heat tolerance, feed thrift, Boer War remount ability, cattle work, harness training, breed-society registration, or the distinction between SA Boerperd and Cape Boerperd. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics wiki page supplied in the original request could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the supplied example. Before compiling, reconcile exact gene IDs, allele labels, source enums, price units, `Commonness` names, `D/nd1/nd2` syntax, and body-stat band serialization against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** SA Boerperd material documents which solid phenotype families occur, but I did not locate a representative breed-wide genotype-frequency survey for Extension, Agouti, cream, gray, dun, roan, or other coat loci. The numeric founder rates below are transparent **gameplay approximations**, not official SA Boerperd population frequencies. A 2020 study investigated performance-related mutations in the breed, but no retrieved result established defensible rates for the exact disease panel requested here; all named disorder loci remain clear. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php)

```json
{
  "id": "boerperd",
  "name": "Boerperd",
  "type": "natural",
  "notes": "The Boerperd, specifically the SA Boerperd, is a South African Cape-derived farmer's horse defined by historical Cape Horse ancestry, hard veld use, broad multi-purpose saddle and harness ability, strong hooves, drought and heat resilience, feed thrift, calm but responsive temperament, long-distance stamina, and a frequent natural rack or amble. Procedural Horse Genetics does not model the rack, amble, five-gaited status, gait comfort, head carriage, wavy mane, hoof hardness, drought adaptation, heat tolerance, feed efficiency, Boer War remount service, farm work, harness training, registry inspection, breed-paper eligibility, or the distinction between SA Boerperd and Cape Boerperd.",

  "biomes": [
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:plains",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:windswept_hills",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:forest"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 760,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.68,
      "e": 0.32
    },
    "agouti": {
      "A": 0.64,
      "a": 0.36
    },

    "cream": {
      "N": 0.88,
      "Cr": 0.12
    },
    "grey": {
      "N": 0.88,
      "G": 0.12
    },
    "dun": {
      "N": 0.94,
      "D": 0.06
    },
    "roan": {
      "N": 0.91,
      "Rn": 0.09
    },
    "flaxen": {
      "N": 0.88,
      "f": 0.12
    },
    "rabicano": {
      "N": 0.96,
      "Rb": 0.04
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
    "jump": 5,
    "health": 9,
    "size": [
      0.98,
      1.11
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Boerperd × Boerperd produces Boerperd. Boerperd × another pure breed produces a Boerperd cross. A Boerperd cross bred back to a pure Boerperd remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Boerperd lineage merely because a horse is bay, gaited, hardy, and found in savanna terrain; real SA Boerperd identity depends on South African breed lineage and registration."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Broad solid-color South African saddle horse | SA Boerperd standards permit multiple normal coat families, but require pigmented skin and discourage or reject large blaze, high stockings, and pinto-like markings.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |
| Base colors | `E: 0.68 / e: 0.32`; `A: 0.64 / a: 0.36` | Produces bay, brown/dark bay, black, chestnut, and sorrel as a broad natural foundation. Exact values are gameplay approximations because no SA Boerperd genotype frequency table was located.  [horsebreedspictures](https://www.horsebreedspictures.com/boerperd-horse.asp) |
| Cream | `Cr: 0.12` | Buckskin, palomino, smoky black, cremello, and perlino-type outcomes are consistent with a documented Boerperd cream-dilution population. The rate is deliberately moderate-low rather than a formal breed estimate.  [equus-journeys](https://www.equus-journeys.com/info/the-boerperd-23.html) |
| Gray | `G: 0.12` | Gray is a documented Boerperd color and should appear occasionally without overwhelming bay, chestnut, and dark solid horses.  [horsebreedspictures](https://www.horsebreedspictures.com/boerperd-horse.asp) |
| Dun | `D: 0.06` | Dun is specifically described as rare. A 6% allele rate makes true dun a recurring but distinctly minority feature; reduce to 2–3% if rendered adults prove too dun-heavy.  [equus-journeys](https://www.equus-journeys.com/info/the-boerperd-23.html) |
| Roan | `Rn: 0.09` | Roan is documented among ordinary solid Boerperd colors. It should be visible but must not be confused with pinto, sabino, or leopard complex.  [equus-journeys](https://www.equus-journeys.com/info/the-boerperd-23.html) |
| Flaxen and rabicano | `f: 0.12`, `Rb: 0.04` | Breed references list flaxen and rabicano among observed modifiers. These values create moderate chestnut mane/tail variation and rare tail/flank white texture without creating pinto bodies.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php) |
| Excluded dilutions | Champagne, silver, pearl, mushroom forced wild type | These are not supported by the retrieved SA Boerperd sources as normal founder-pool colors. |
| Excluded pinto loci | Tobiano, Sabino 1, frame, splash, KIT white forced wild type | Breed standards require pigmented skin and discourage pinto-like facial/leg markings; the source explicitly describes the coat as always solid. Excluding these loci preserves the correct non-pinto identity.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type spotting does not belong to the documented Boerperd founder palette. |
| Magical loci | All forced wild type | The Boerperd’s visual richness and environmental adaptation come from ordinary equine genetics and management history. |
| Disorders | All named loci clear | A Boerperd performance-mutation study exists, but no defensible carrier rates for the requested disorder panel were found in the retrieved results. Do not invent PSSM1, HYPP, WFFS, SCID, GBED, HERDA, or other rates from ancestry.  [ajol](https://www.ajol.info/index.php/sajas/article/view/194911/184097) |
| Speed | `6/10` | Captures practical pace, sure-footedness, and a comfortable ambling/racking travel style, not specialist racehorse speed.  [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588) |
| Jump | `5/10` | The Boerperd is a versatile saddle horse capable of general riding and show work, but is not a purpose-bred jumper.  [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588) |
| Health | `9/10` | Represents field hardiness, low-input condition, long working days, hoof strength, heat/cold tolerance, and recovery—not genetic invulnerability.  [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588) |
| Size | `×0.98–1.11` | Models a medium 14.2–16-hand population, including smaller mares and taller mature riding horses.  [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588) |

## Disorder approach

Every named disorder locus is set to **clear** in the pure Boerperd founder pool.

This is a data-standard decision, not a claim that every individual SA Boerperd is free from hereditary disease. The retrieved literature includes a 2020 study examining performance-trait mutations and genetic diversity in SA Boerperds, but the available search result does not provide validated frequencies for the mod’s requested disease panel. [ajol](https://www.ajol.info/index.php/sajas/article/view/194911/184097)

Therefore, do not seed:

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

This matters especially for `GYS1` PSSM1. It is a **dominant** mutation: a heterozygous horse is genetically affected, not a harmless recessive carrier. A future Boerperd-specific study could justify a nonzero founder rate, but only if it reports the precise variant and a clearly sampled SA Boerperd population. [equinegeneticsresearchcentre](https://equinegeneticsresearchcentre.horse/genetic-diagnostic-testing/pssm1/)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `boerperd` as a natural South African breed with Cape-derived history, veld biome mapping, solid-color gene pools, clear disease policy, and hardy all-purpose saddle stats. |
| `common/breed/Breeds` | Register `boerperd` for wild packs, cowboy/stable acquisition, spawn eggs, H-menu display, breed books, commands, saves, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. `wild` is a gameplay representation of free-ranging veld populations, not a claim that the current registry breed is unmanaged. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band object. Do not use bands to fake racking gait, wavy mane, hard hooves, heat resistance, or body-muscle quality. |
| `common/breed/spec/` | Verify the exact `E/e`, `A/a`, cream, gray, dun, roan, flaxen, and rabicano locus IDs; confirm that omitted loci become wild type and that all probability objects follow the actual parser’s syntax. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the requested rarity ladder and that `spawn_weight: 3` is a valid moderate weighted-spawn value. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 5, health 9, and size ×0.98–1.11 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only the listed solid-color loci. Force pinto, leopard, unsupported dilutions, magical loci, and disease loci clear; then apply medium-size, high-heartiness body targets. |
| `common/breed/BreedLineage` | Use standard pure/cross/Mixed behavior. A hardy gaited bay horse does not become Boerperd merely because it lives in a savanna biome. |
| `common/genetics/SpliceOutcome` | No Boerperd-specific exception. A spliced allele transmits normally and can make an atypical descendant, including pinto, leopard, or disease alleles excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize medium-size, practical-speed, general-athletic, high-heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is needed unless the actual schema requires an explicit default. |

## Verification

1. Confirm **Boerperd** appears in the H-menu’s Breeds tab and breed book with South African Cape origins, farmer’s-horse purpose, historical Boer War context, broad solid-color palette, gait caveat, and high heartiness.

2. Spawn repeated packs in savanna, plateau, plains, dry meadow, badlands-margin, forest-edge, river, and windswept-hill biomes. Every horse in a selected Boerperd pack must display the **Boerperd** label.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is bay, dun, gray, hardy, compact, or standing in a savanna biome.

4. Generate at least 1,000 pure founders. The cohort should include bay, brown, black, chestnut, gray, buckskin, palomino, smoky black, roan, rare dun, and occasional flaxen/rabicano expression. It must remain solid-bodied.

5. Confirm pure founders cannot generate tobiano, frame overo, Sabino 1, splash white, KIT dominant white, leopard complex, PATN spotting, champagne, silver dapple, pearl, mushroom, brindle, or magical coat effects.

6. Inspect genome pools:
- `E/e` and `A/a` produce base-color variety.
- `Cr`, `G`, `D`, `Rn`, flaxen, and rabicano vary at the supplied modest rates.
- Every pinto/white-pattern and leopard locus is wild type.
- All named disorder loci are clear.

7. Confirm phenotype distinctions:
- A palomino should be `e/e Cr/_`, not a chestnut with a magical blond mane.
- A buckskin should be `E_ A_ Cr/_`.
- A smoky black should be `E_ a/a Cr/_`.
- A roan should retain a dark head and points and must not progressively gray.
- A gray should be born pigmented and progressively lighten.
- A dun should show real dun dilution and primitive markings, not a random dorsal stripe.

8. Confirm mature body stats. Boerperds should be medium-sized, durable, good at ordinary overland travel, and capable of a useful all-around jump. They should not outpace specialist racehorses, outjump elite warmbloods, or become heavy-draught sized.

9. Test default lineage:
- Boerperd × Boerperd → Boerperd.
- Boerperd × Cape Boerperd → Boerperd cross.
- Boerperd × American Saddlebred → Boerperd cross.
- Boerperd cross × pure Boerperd → the existing Boerperd cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate genetic introduction. Add tobiano, frame, splash, leopard complex, champagne, `PLOD1` WFFS, or `GYS1` PSSM1 through outcrossing or splice mechanics. The offspring should inherit normally, but it must be visually or genetically nonstandard for the pure SA Boerperd founder pool and must never receive pure Boerperd status merely from terrain, size, or color.

## Sources

- [Cape Boerperd: Peacock from the South](https://journals.co.za/doi/pdf/10.10520/EJC14588): detailed South African breed article describing Cape Horse origins, 14.2–16-hand size, hard farm work, rough-terrain sure-footedness, stamina, condition on minimal feed, temperament, action, and multi-purpose conservation goal. [journals.co](https://journals.co.za/doi/pdf/10.10520/EJC14588)

- [SA Boerperd — Breed History](https://saboerperd.com/sa-boerperd-breed-history/): official SA Boerperd historical context for Cape Horse ancestry, Boer War legacy, temperament, bravery, intelligence, endurance, sure-footedness, and hardiness. [saboerperd](https://saboerperd.com/sa-boerperd-breed-history/)

- [Boerperd history and standards](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/): detailed historical account of Dutch/Java/Arabian, Andalusian, Flemish, Hackney, Norfolk Trotter, and Cleveland Bay influence; 13.3–16+ hand range; pigmented-skin and marking standards; and climate adaptation. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

- [SA Boerperd breed reference](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php): supplementary documentation of common colors, cream and dun behavior, roan/flaxen/rabicano modifiers, solid-body policy, gaited status, build, and all-around performance context. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php)

- [Performance trait analysis and genetic diversity of the SA Boerperd](https://www.ajol.info/index.php/sajas/article/view/194911/184097): peer-reviewed research reference for SA Boerperd performance-trait mutation and genetic-diversity investigation; used here to justify avoiding unsupported disease-frequency claims pending a direct locus-specific result. [ajol](https://www.ajol.info/index.php/sajas/article/view/194911/184097)