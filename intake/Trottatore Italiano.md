The **Trottatore Italiano** should be treated as a performance-defined, open-color Italian harness-racing breed rather than a visually constrained landrace. Pure founders should emphasize bay, chestnut, and black while retaining low-frequency access to the wider coat gene pool, because the Italian Trotter has no morphological breed standard or color restriction: racing aptitude, especially at the trot, is the defining selection criterion. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied earlier could not be retrieved through the available reader. This JSON follows the readable convention from your example. Match the exact current field names, gene IDs, source enum values, commonness enum, and any required schema wrappers to `common/breed/spec/` before compiling.

## Identity & flavour

The **Trottatore Italiano**, usually rendered in English as the **Italian Trotter**, is Italy’s national harness-racing horse. It emerged during the nineteenth century from the deliberate development of fast trotting stock, drawing especially on the American Standardbred, French/Norman Trotter, and Russian Orlov Trotter, with other European light-horse influences in its deeper ancestry. The first Italian Trotter herdbook was established in **1896**; the modern breed is pedigree-defined and selectively bred for racing ability, not for a rigid exterior type or a narrowly controlled coat color. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

It was made to race at the **trot**, pulling a sulky at high speed without breaking into a gallop. Italian trotting races have historically centered on short distances around 1,600–1,660 metres and medium distances around 2,060–2,100 metres, and the breed has become internationally significant through Italian harness-racing culture, breeding operations, and export bloodlines. A Trottatore Italiano should feel like a focused athlete: eager to go forward, economically built, quick to accelerate, and more at home on an oval track or in a harness than under a heavy pack saddle. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

Italian Trotters generally stand **145–160 cm (about 14.1–15.3 hands)**, though individual racing-bred horses can stand outside that practical range. There is intentionally no fixed morphology requirement: conformation is judged by whether it supports a fast, efficient trot, not whether it reproduces a single breed silhouette. In broad terms, expect a medium-sized, dry, athletic harness horse with a straight, well-made head, sloping shoulder, strong hindquarters, sturdy but relatively light legs, and small mobile ears. It should look more compact and muscular than a Thoroughbred, but lighter, faster, and more track-focused than a farm horse. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

The breed accepts **any color**, though bay, chestnut, and black are the most common. That does not mean every color should be equally likely in a founder pool. For a coherent Italian Trotter population, bay should lead, chestnut should be common, black should be present, and gray should occur at a modest level. Cream, dun, silver, champagne, tobiano, roan, and other patterns should be possible at low background rates rather than excluded outright: the breed has no color-based registration standard, and its historical composite ancestry makes a tightly “one-color-only” definition inappropriate. The player-facing signature is speed and harness-athlete shape, not a special coat. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

The Trottatore Italiano belongs in Procedural Horse Genetics because it provides a different breeding goal from both Thoroughbreds and warmbloods: a fast, compact, high-heartiness track horse whose genetic value is its repeated ability to produce a powerful, efficient trotting athlete. The mod does not model gait mechanics, trotting action, break-to-gallop penalties, sulky handling, race strategy, track surface, pacing, training, race records, pedigree-performance indexes, or the distinction between a fast harness horse and an elite champion. A player should recognize the breed from its race-ready outline and performance trend, then discover that its coat genetics remain refreshingly open. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

## Breed JSON

```json
{
  "id": "trottatore_italiano",
  "name": "Trottatore Italiano",
  "type": "natural",
  "notes": "The Trottatore Italiano is defined by pedigree, harness-racing performance, fast and regular trotting action, sulky aptitude, trainability, race conditioning, and performance records rather than a fixed conformation or color standard. Procedural Horse Genetics does not model the trot gait, pacing or breaking, sulky pull, driver skill, track surface, race distance preference, training response, race times, winnings, performance indexes, official Italian studbook eligibility, or whether a horse can succeed as a competitive harness racer.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "terralith:temperate_highlands",
    "terralith:lowlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.68,
      "e": 0.32
    },
    "agouti": {
      "A": 0.70,
      "a": 0.30
    },

    "grey": {
      "N": 0.92,
      "G": 0.08
    },
    "cream": {
      "N": 0.96,
      "Cr": 0.04
    },
    "dun": {
      "N": 0.97,
      "D": 0.03
    },
    "champagne": {
      "N": 0.99,
      "Ch": 0.01
    },
    "silver": {
      "N": 0.985,
      "Z": 0.015
    },
    "pearl": {
      "N": 0.99,
      "Prl": 0.01
    },
    "mushroom": {
      "N": 0.995,
      "mu": 0.005
    },
    "flaxen": {
      "N": 0.95,
      "F": 0.05
    },

    "tobiano": {
      "N": 0.97,
      "To": 0.03
    },
    "sabino_1": {
      "N": 0.97,
      "SB1": 0.03
    },
    "frame_overo": {
      "N": 0.995,
      "O": 0.005
    },
    "splash_white_1": {
      "N": 0.98,
      "SW1": 0.02
    },
    "splash_white_2": {
      "N": 0.995,
      "SW2": 0.005
    },
    "kit_white_spotting": {
      "N": 0.99,
      "W": 0.01
    },
    "roan": {
      "N": 0.98,
      "Rn": 0.02
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
    },
    "leopard_complex": {
      "N": 0.99,
      "LP": 0.01
    },
    "patn1": {
      "N": 0.995,
      "PATN1": 0.005
    },
    "patn2": {
      "N": 0.995,
      "PATN2": 0.005
    },
    "brindle": {
      "N": 0.995,
      "Br": 0.005
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
      "N": 0.995,
      "O": 0.005
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
    "speed": 9,
    "jump": 4,
    "health": 7,
    "size": [
      0.97,
      1.08
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base colors | `E` 0.68 / `e` 0.32; `A` 0.70 / `a` 0.30 | Bay, chestnut, and black are repeatedly identified as the most common Italian Trotter colors. This founder pool makes bay the leading result while keeping chestnut and black common enough to look like a genuine broad-color harness-racing population. These are gameplay estimates, because no breed-wide published MC1R/ASIP frequency dataset was located.  [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter) |
| Gray | `G` 0.08 | Gray is permitted under the no-color-restriction breed definition but is not named among the principal common colors; a small gray pool gives occasional graying runners without obscuring the underlying bay/chestnut/black population.  [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter) |
| Dilutions | Low `Cr`, `D`, `Ch`, `Z`, `Prl`, `mu`, and flaxen pools | The breed accepts any color and lacks a color-specific standard. Low background rates preserve genetic breadth and the possibility of rare unusual founders while preventing dilutions from becoming more recognizable than the breed’s athletic type. This is a permissive gameplay choice, not an assertion of measured Italian Trotter allele frequencies.  [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter) |
| White and patterned loci | Low rates across patterns | The same no-color-restriction logic applies to tobiano, sabino, frame, splash, KIT white, roan, rabicano, leopard complex, and brindle. Keep them uncommon enough that bay/chestnut/black remain visually typical. |
| Frame/MET consistency | `frame_overo` and `MET_lethal_white` must be the same locus | If the live mod represents frame as `EDNRB` and the lethal white disorder through the same `MET` mutation, do **not** roll them independently. The `O` allele must be a single shared locus: `O/O` is lethal, `N/O` is frame overo, and `N/N` is clear. The duplicated representation above is only a schema placeholder and must be collapsed to the real project design. |
| Disorders | Clear except shared frame/MET placeholder | No credible Italian Trotter carrier-rate studies were located for the requested disorder panel. It is not defensible to import PSSM1 or other disease frequencies from American Standardbreds, Belgian draughts, Quarter Horses, or generic warmblood estimates. |
| Speed | 9/10 | The breed has been selectively bred exclusively for trotting-race ability, particularly short and medium harness-race distances. This should place it among the mod’s fastest horses without turning the speed stat into an unrealistically gait-specific simulation.  [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter) |
| Jump | 4/10 | Athletic, but bred for harness-racing efficiency rather than obstacle scope. |
| Health | 7/10 | Represents a fit, functional performance horse while leaving room for the intense management, injury risk, and training demands that the mod does not simulate. |
| Size | ×0.97–1.08 | Produces a medium-sized racing horse around the documented 145–160 cm range, with the direct height statement retained in flavor rather than JSON.  [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter) |

## Verification

1. Confirm **Trottatore Italiano** appears in the H-menu’s Breeds tab and the breed book with the correct natural classification, uncommon status, managed source list, price, Italian-racing flavor text, and performance-focused notes.

2. Attempt wild herd spawning in an eligible biome. No pure Italian Trotter pack should occur because `wild` is intentionally absent. Confirm access through cowboy offers, stable stock, and spawn eggs.

3. Confirm a lone unassigned wild horse reads **Feral Mixed**, not Trottatore Italiano.

4. Generate at least 1,000 founders. Bay should be the largest visible group; chestnut and black should also be common. Gray and dilutions should appear occasionally, while loud white, leopard, roan, or brindle patterns should remain unusual.

5. Inspect the genetics pool directly. Pure founders should have no magical loci. All named coat loci should be able to occur only at the listed low rates, and the underlying base-color distribution should remain much stronger than any dilution or pattern frequency.

6. Confirm that every disorder locus except the model’s single shared frame/MET locus remains clear. If frame is represented in the real mod as an ordinary coat locus plus separate lethal `MET`, revise the file to the project’s canonical shared-locus representation before testing.

7. Compare performance against a Thoroughbred, Standardbred-like breed, warmblood, and draught horse. Trottatore Italiano should be exceptionally fast, medium-sized, and only moderately suited to jumping. It should not receive a special invisible gait bonus unless the mod separately implements harness-gait mechanics.

## Sources

- [Italian Trotter overview](https://en.wikipedia.org/wiki/Italian_Trotter): secondary summary for the official Italian name, 145–160 cm range, lack of a morphology standard, any-color acceptance, most common bay/chestnut/black colors, nineteenth-century development, 1896 herdbook, ancestry, and racing emphasis at short and medium harness distances. [en.wikipedia](https://en.wikipedia.org/wiki/Italian_Trotter)

- [Italian Trotter breed profile](https://www.horsebreedspictures.com/italian-trotter.asp): secondary supporting source for the Italian name, nineteenth-century origin, Standardbred/Norman/Orlov ancestry, broad color acceptance, predominant bay/black/chestnut colors, straight profile, sloping shoulder, sturdy legs, and closed registered-parentage herdbook model. [horsebreedspictures](https://www.horsebreedspictures.com/italian-trotter.asp)

- [Capomaccio et al., *Frontiers in Genetics* — Italian equine gene-pool analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC9900106/): peer-reviewed genomic context for Italian horse populations and the distinction between Italian coldblood and warmblood groupings. It supports treating the Trottatore Italiano as a performance-oriented, lighter sport/racing population rather than a regional heavy workhorse. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9900106/)