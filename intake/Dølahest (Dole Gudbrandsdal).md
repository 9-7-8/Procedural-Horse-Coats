The **Dølahest**—also called the **Dole Gudbrandsdal**, **Dole horse**, or simply **Dole**—should be a rare Norwegian compact draught-and-harness breed with a broad but traditional solid-color population. Unlike the color-fixed Fjord, the Dole accepts black, brown/bay, chestnut, buckskin, palomino, and all gray variants; its defining inheritance should therefore retain meaningful base-color, gray, and cream variation while excluding loud pinto, leopard, and magical genes. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

> **Schema caveat:** The Procedural Horse Genetics wiki remains unavailable to the reader. This file uses the readable proposed JSON convention from your examples. Before compiling, map field names, locus IDs, source/commonness enums, and target-band format to the active `common/breed/spec/` implementation.

## Identity & flavour

The **Dølahest**, or **Dole Gudbrandsdal**, is Norway’s largest native horse breed: a compact, strong, cold-blooded workhorse from Gudbrandsdalen and eastern Norway. It is also called the **Dole horse** or simply the **Dole**. The breed developed over centuries as a practical rural horse, with local Scandinavian stock shaped by farm work, timber hauling, road travel, and harness use; organized preservation and breed-association work began in the twentieth century, with the present national association formed in 1947. It is one of Norway’s endangered native breeds, managed through the Norwegian Horse Centre and the national Dole-horse organization. [madbarn](https://madbarn.com/dole-gudbrandsdal-breed-profile/)

The Dole was made to do nearly everything a Norwegian farm family needed. It hauled timber, worked fields, pulled carts and sleighs, traveled roads in harness, and served as a dependable riding horse when no separate saddle horse was practical. The breed also has a trotting tradition, including the smaller, lighter Dole Trotter type, but the Dole Gudbrandsdal is first and foremost a powerful all-purpose rural horse. In the mod, it should feel like a trustworthy northern work partner: more nimble and versatile than a giant draught horse, more powerful than an ordinary riding horse, and quietly capable in forests, snow, and rough farmland. [en.wikipedia](https://en.wikipedia.org/wiki/D%C3%B8lehest)

Modern breeding guidance says Dølahest height should generally fall between **148 and 156 cm (about 14.2–15.1 hands)**, though individuals outside that band occur and overall breed type matters more than a single measurement. The horse is broad and compact, with a heavy straight-profiled head, strong jaws, short muscular neck, broad moderately pronounced withers, a wide deep chest, strong sloping shoulder, a long back, broad muscular slightly sloping croup, short sturdy limbs, solid joints, and durable hooves. The mane and tail are practical working-horse hair; feathering is limited rather than the heavy drapery of a Shire or Clydesdale. A Dole should look dense, broad, and solidly grounded—an honest horse built to pull through snow and mud. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

The Dølahest is notably more genetically colorful than its sturdy farm-horse silhouette may suggest. The current Norwegian breeding plan accepts **black, brown, red/chestnut, buckskin, palomino, and every gray variation**, while allowing moderate facial markings and low socks. The mod should preserve that diversity: bay/brown as the center, black and chestnut as ordinary alternatives, a visible gray component, and low-to-moderate cream so buckskin, palomino, smoky black, and occasional double dilutes can arise. The breed plan itself treats color as secondary to type, though very extensive white markings and certain unusual color outcomes are restricted by inspection rules. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

The Dølahest belongs in Procedural Horse Genetics because it is an excellent “working northern generalist”: a substantial but not enormous horse with useful color depth and heritage-breed value. Conservation is active because Norway’s three native non-trotting horse breeds—including the Dole—are endangered. Players should recognize the Dole not through one coat, but through the combination of a sturdy Gudbrandsdal body, broad practical head, strong feet, and a herd of rich northern solids—bay, black, chestnut, gray, buckskin, and palomino—without the loud patterns of a modern color breed. The mod does not model exact body type, timber pulling, sleigh work, trotting action, snowy traction, hoof durability, moderate face and leg markings, registry inspection, DNA parentage documentation, Dole-versus-Dole-Trotter subtyping, or Norwegian conservation breeding management. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

## Breed JSON

```json
{
  "id": "dolahest",
  "name": "Dølahest",
  "type": "natural",
  "notes": "The Dølahest, or Dole Gudbrandsdal, is defined by compact Norwegian cold-blood conformation, a broad deep chest, heavy straight-profiled head, short muscular neck, strong sloping shoulder, broad croup, durable hooves, farm and forestry utility, harness ability, and its place in Norwegian native-breed conservation. Procedural Horse Genetics does not model timber hauling, sleigh work, true draft pull, snow traction, exact Dole-versus-Dole-Trotter type, hoof hardness, moderate white face/leg markings, breed-show evaluation, DNA parentage verification, Norwegian studbook categories, individual temperament, or conservation-program breeding decisions.",

  "biomes": [
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:grove",
    "minecraft:snowy_taiga",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:windswept_forest",
    "minecraft:windswept_hills",
    "minecraft:plains",
    "terralith:alpine_grove",
    "terralith:temperate_highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1020,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.72,
      "a": 0.28
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "cream": {
      "N": 0.84,
      "Cr": 0.16
    },

    "dun": {
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
    "speed": 5,
    "jump": 4,
    "health": 9,
    "size": [
      1.00,
      1.11
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base colors | `E` 0.72 / `e` 0.28; `A` 0.72 / `a` 0.28 | Black, brown/bay, and chestnut/red are all explicitly accepted and ordinary Dole colors. The pool makes bay/brown the visual center while keeping black and chestnut easy to encounter. These are transparent phenotype-based gameplay estimates, not published Dole MC1R/ASIP frequencies.  [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf) |
| Gray | `G` 0.18 | Every gray variation is accepted in the current Norwegian breeding plan. A meaningful but minority gray rate preserves that real diversity without turning the breed into a predominantly gray population.  [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf) |
| Cream | `Cr` 0.16 | Buckskin and palomino are explicitly accepted, and smoky black can occur, demonstrating the cream locus in the breed. This rate makes cream-derived colors visible but not dominant; double dilutes remain possible but uncommon, consistent with the studbook’s historical concern around cremello and perlino.  [en.wikipedia](https://en.wikipedia.org/wiki/D%C3%B8lehest) |
| Dun | Forced wild type | The current breed plan lists black, brown, red, buckskin, palomino, and gray variants. Some secondary summaries list dun, but other sources state the dun gene does not occur. Because the evidence conflicts and no Dole TBX3 survey was found, the conservative implementation is `d/d` only. Do not imply a documented native dun pool without a genetic study or an explicit current registry statement.  [en.wikipedia](https://en.wikipedia.org/wiki/D%C3%B8lehest) |
| Silver | Forced wild type | Silver is a dominant PMEL17 dilution with distinctive effects on black pigment, but no reliable Dole-specific evidence was found. It occurs in some Nordic breeds; that is not a reason to inject it into the Dole.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC1617113/) |
| White patterns | Forced wild type | Moderate facial markings and low socks are allowed, but the mod’s major white-pattern loci represent far more extensive patterns. The current breeding plan restricts extensive white markings, so named pattern loci stay wild type.  [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf) |
| Disorders | All clear | No defensible Dølahest-specific carrier frequencies were located for the requested disorder panel. The breed’s conservation status should not be turned into invented disease prevalence. |
| Speed | 5/10 | The Dole is a working-harness horse with a trotting tradition, but the Dole Gudbrandsdal remains a practical cold-blood generalist rather than a racing specialist.  [en.wikipedia](https://en.wikipedia.org/wiki/D%C3%B8lehest) |
| Jump | 4/10 | Strong, compact conformation gives useful real-world mobility; no evidence supports specialized jumping selection. |
| Health | 9/10 | Represents a rugged, low-input northern farm horse with durable feet and strong survival value. It is not a claim of immunity to disease.  [madbarn](https://madbarn.com/dole-gudbrandsdal-breed-profile/) |
| Size | ×1.00–1.11 | Produces a medium-heavy, compact full-sized working horse, consistent with the documented 148–156 cm core band while keeping literal height out of the JSON.  [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf) |

## Verification

1. Confirm **Dølahest** appears in the H-menu’s Breeds tab and breed book with its natural classification, rare commonness, Norwegian forest-and-farm biome profile, managed source list, price, and breed notes.

2. Confirm the breed does not spawn in wild packs because `wild` is omitted. Verify access through cowboy stock, stable stock, and the spawn egg.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, not Dølahest.

4. Generate at least 1,000 founders. The herd should be centered on bay/brown, with ordinary black and chestnut horses, a visible gray minority, and recurring buckskin, palomino, and smoky-black outcomes from cream inheritance.

5. Confirm that cream inheritance behaves normally:
   - `E_ A_ Cr_` produces buckskin.
   - `ee Cr_` produces palomino.
   - `aa E_ Cr_` produces smoky black.
   - Two `Cr` alleles can create a rare double-dilute outcome, subject to the mod’s normal cream/pearl logic.

6. Inspect founder genomes. Only extension, agouti, gray, and cream should contain non-wild coat alleles. Pure founders should never generate dun, pearl, champagne, silver, mushroom, flaxen, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical traits.

7. Inspect disorder genomes across a large founder sample. Every listed disorder should remain clear unless introduced later by an outcross or an explicit genetics-tool action.

8. Compare performance to a Fjord, a Norwegian Coldblood Trotter, and a large draft horse. The Dølahest should feel sturdier and larger than a Fjord, more broadly useful but less race-specialized than a Coldblood Trotter, and more compact and versatile than a giant draught breed.

## Sources

- [Landslaget for Dølahest / Norwegian Dole Horse breeding plan, 2024–2025](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf): primary breed-plan source for the 148–156 cm general height target, Norwegian Horse Centre DNA parentage documentation and registration role, approved colors—black, brown, red, buckskin, palomino, and all gray variants—and acceptable moderate head/leg markings. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

- [NordGen — Dole Horse](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/dole-horse/): Nordic genetic-resource source for the Dole’s Norwegian native-breed context, 145–155 cm range, historical color summary, and documentation coverage. Note that its public color list conflicts with the newer breed plan on dun; the newer plan is preferred for the JSON. [dolehesten](https://www.dolehesten.no/wp-content/uploads/2025/03/Avlsplan-dol-vedtatt-04.12.24_uten-synlige-endringer.pdf)

- [NordGen — *Equines in the Nordics*](https://www.nordgen.org/media/rvmjyfb1/equines-in-the-nordics-small.pdf): conservation source identifying the Dole as one of Norway’s four native horse breeds and one of its three endangered native non-trotting breeds, managed through the Norwegian Horse Centre and breed associations. [nordgen](https://www.nordgen.org/media/rvmjyfb1/equines-in-the-nordics-small.pdf)

- [Dole Gudbrandsdal profile](https://madbarn.com/dole-gudbrandsdal-breed-profile/): secondary synthesis for aliases, origin, registry context, traditional working roles, approximate size and mass, conformation, accepted colors, cream-derived phenotypes, and the 1947 national breed association. [madbarn](https://madbarn.com/dole-gudbrandsdal-breed-profile/)