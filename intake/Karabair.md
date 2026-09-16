The **Karabair**—Uzbek **Qorabayir**, Tajik **Qarabair**, Russian **Karabairskaya**—should be a moderately common Central Asian riding-and-working horse with a broad but coherent bay/chestnut/gray core. It is not a color-defined breed: its identity is the blend of oriental speed, desert-and-mountain hardiness, agility, comfortable utility, and long-distance practicality that made it valuable from Uzbekistan through northern Tajikistan and neighboring Central Asian regions. [en.wikipedia](https://en.wikipedia.org/wiki/Karabair)

> **Schema caveat:** The Procedural Horse Genetics wiki could not be retrieved by the available reader. This JSON follows the readable proposed convention used in your examples. Match all key names, allele identifiers, source/commonness enums, and stat-band serialization to the actual `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Karabair**, known in Uzbek as **Qorabayir**, in Tajik as **Qarabair**, and in Russian as **Karabairskaya**, is a long-established Central Asian horse breed associated especially with Uzbekistan and northern Tajikistan. Its name is often linked to the Uzbek words *qora* and *bayir*, commonly glossed as “black horse” or “dark horse,” but the breed itself is not restricted to black. The Karabair coalesced over many centuries from the meeting of southern desert horses of Arabian and Turkmene type with northern steppe horses, creating a practical regional animal that could combine endurance, agility, toughness, and useful speed. [en.wikipedia](https://en.wikipedia.org/wiki/Karabair)

The Karabair was made for the demanding ordinary work of Central Asia: riding, pack transport, light harness, agricultural work, caravan-style travel, and regional horse games. It had to work across dry steppe, desert margins, foothills, irrigated farmland, and mountain routes without becoming too heavy or too fragile. It is also associated with **kok-boru** and related mounted games, where agility, courage, balance, and fast changes of direction matter as much as sheer pace. In mod gameplay, a Karabair should be a horse that feels useful everywhere: quick enough to cross open land, tough enough for rough mountain paths, and strong enough for a working stable. [madbarn](https://madbarn.ca/karabair-horse-breed-profile/)

Karabairs commonly stand **150–158 cm (about 14.3–15.2 hands)** at the withers and weigh roughly 400–500 kg. Their frame is strong but refined: a medium-sized head with a straight or slightly convex profile, medium-length muscular neck, developed chest, dry strong limbs, durable hooves, and a compact, athletic body. They are neither tall European warmbloods nor small steppe ponies. A Karabair should look like an economical Central Asian saddle horse—substantial enough for harness and farm work, but clean-limbed and light enough for distance, games, and rapid practical travel. [breeds.okstate](https://breeds.okstate.edu/horses/karabair-horses)

Bay, chestnut, gray, and black are the consistently documented colors. Black or dark bay is especially emphasized in recent Uzbek agricultural literature, while older breed references list bay, chestnut, gray, and black without identifying color as a defining registry criterion. The safest founder implementation is therefore a bay/dark-bay-forward population with strong chestnut and gray representation and a meaningful black minority. The coat pool should remain free of cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, frame, splash, roan, leopard complex, and magical loci—not because those colors can never exist in individual horses, but because no reliable Karabair-specific allele data supports presenting them as normal pure-founder traits. [breeds.okstate](https://breeds.okstate.edu/horses/karabair-horses)

The Karabair is part of Uzbekistan’s agricultural and cultural heritage, sometimes described in local writing as a “golden fund” of national horse breeding. It remains regionally important but has limited international registry visibility and relatively little published English-language genetic health data. This makes it ideal for a mod breed that rewards practical breeding rather than a narrow show standard. Players should recognize Karabairs as medium-sized, hardy, bay-or-gray Central Asian all-rounders with endurance and agile working ability—not as a generic Arabian cross and not as a specialist racehorse. The mod does not model traditional gaits, mounted-game skill, desert heat tolerance, drought thrift, mountain pathfinding, local tack, cultural horse-game training, the exact main/riding/denser intrabreed types, or Uzbekistan’s state-studbook lineage rules. [agriculture-oshsu](https://agriculture-oshsu.com/en/journals/tom-3-2-2024/karabairskaya-poroda-loshadey-zolotoy-fond-uzbekistana)

## Breed JSON

```json
{
  "id": "karabair",
  "name": "Karabair",
  "type": "natural",
  "notes": "The Karabair is defined by Central Asian utility: a strong but refined body, dry durable limbs, sound hooves, endurance, agility, comfortable practical riding, light-harness and pack usefulness, and suitability for regional mounted games. Procedural Horse Genetics does not model traditional gaits, precise heat or drought tolerance, desert forage thrift, mountain pathfinding, true pack capacity, harness training, kok-boru skill, local tack, exact main/riding/denser intrabreed types, individual courage, Uzbekistan or Tajikistan studbook eligibility, or historical regional lineage identity.",

  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_savanna",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:desert",
    "minecraft:stony_peaks",
    "minecraft:windswept_hills",
    "minecraft:meadow",
    "terralith:shrubland",
    "terralith:rocky_mountains"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 780,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.74,
      "e": 0.26
    },
    "agouti": {
      "A": 0.73,
      "a": 0.27
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },

    "dun": {
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
    "speed": 7,
    "jump": 5,
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
| Base-color pool | `E` 0.74 / `e` 0.26; `A` 0.73 / `a` 0.27 | Bay, chestnut, gray, and black are all consistently documented. A high `E` and `A` pool makes bay and dark bay the leading visible non-gray results, while retaining a large enough `e` pool for chestnut and `a` pool for black. These are phenotype-based implementation estimates; no Karabair MC1R/ASIP allele-frequency study was located.  [breeds.okstate](https://breeds.okstate.edu/horses/karabair-horses) |
| Gray | `G` 0.18 | Gray is an ordinary, repeatedly listed Karabair color but not the single defining coat. This rate makes it a visible minority under a dominant-gray model, without masking the bay/dark-bay core. It is an explicit gameplay estimate, not a measured STX17 frequency.  [breeds.okstate](https://breeds.okstate.edu/horses/karabair-horses) |
| Black/dark bay emphasis | Base-color structure, not a shade band | Recent Uzbek literature characterizes the notable Karabair coat as predominantly black or dark bay. The file supports that visual tendency through high `E`, bay dominance, and a meaningful `a` pool rather than forcing every horse into an artificial “deeply dark” epigenetic band.  [cyberleninka](https://cyberleninka.ru/article/n/technology-of-intensive-breeding-of-horses-of-the-karabair-breed-in-the-highlands-of-uzbekistan) |
| Dilutions/patterns | Forced wild type | The reliable sources identify base colors but do not provide breed-specific evidence for cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, frame, splash, KIT W, roan, rabicano, leopard complex, brindle, or magical loci. These should enter only through crosses or genetics tools. |
| Disorders | All clear | No defensible Karabair-specific prevalence data was located for the requested disorder panel. The available breed-health material explicitly notes that no breed-specific inherited diseases are currently well documented, while correctly cautioning that absence of evidence is not proof of absence.  [madbarn](https://madbarn.ca/karabair-horse-breed-profile/) |
| Speed | 7/10 | The Karabair combines oriental/light-horse speed with practical working versatility and is used for riding, transport, and mounted games. It should be fast, but below pure racing breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Karabair) |
| Jump | 5/10 | Agility and mountain/working utility support a balanced middle score; no evidence supports a dedicated jumping selection program. |
| Health | 9/10 | Represents long adaptation to Central Asian climate, sparse pasture, rough travel, sound hooves, and practical endurance. It is not a claim of disease immunity.  [madbarn](https://madbarn.ca/karabair-horse-breed-profile/) |
| Size | ×1.00–1.11 | Produces a medium Central Asian saddle-and-working horse; the direct height statement remains confined to flavor text.  [madbarn](https://madbarn.ca/karabair-horse-breed-profile/) |

## Verification

1. Confirm **Karabair** appears in the H-menu’s Breeds tab and the breed book with natural classification, uncommon rarity, all four acquisition sources, Central Asian dry-steppe/mountain biomes, price, and appropriate notes.

2. Spawn selected wild packs in eligible plains, savanna, badlands, desert-edge, meadow, or rocky-mountain biomes. Every horse created in the same selected pack should display **Karabair** as its breed label.

3. Confirm a lone ordinary wild horse that was not generated as part of a breed-selected pack reads **Feral Mixed**, not Karabair.

4. Generate at least 1,000 pure founders. The visible non-gray population should center on bay and dark bay, with a substantial chestnut minority and a smaller black minority. Gray should appear regularly but remain secondary to the total founder pool.

5. Inspect loci directly. Only extension, agouti, and gray should carry non-wild alleles. Pure founders must not contain cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, magical loci, or non-clear disorder alleles.

6. Verify ordinary base-color interactions:
   - `E_ A_` produces bay or dark bay.
   - `E_ aa` produces black.
   - `ee` produces chestnut.
   - `G_` progressively grays any of those bases under the mod’s normal aging system.

7. Compare Karabairs against an Arabian, an Akhal-Teke-type horse, a mountain pony, and a heavier farm horse. They should feel medium-sized, notably fast, very hardy, and broadly useful, without outclassing specialist racers, dedicated jumpers, or true heavy draught breeds.

## Sources

- [Karabair overview](https://en.wikipedia.org/wiki/Karabair): secondary reference for Central Asian origin, Uzbek/Tajik/Kazakh/Russian names, the southern desert-horse plus northern-steppe-horse origin model, all-purpose riding/driving use, and reported 2003 Uzbekistan population count. [en.wikipedia](https://en.wikipedia.org/wiki/Karabair)

- [Oklahoma State University — Karabair Horses](https://breeds.okstate.edu/horses/karabair-horses): academic breed summary for breeding distribution throughout Uzbekistan, common bay/chestnut/gray/black palette, and body-measurement context. [breeds.okstate](https://breeds.okstate.edu/horses/karabair-horses)

- [Nurmatov et al. — “The Karabair Horse Breed Is the Golden Fund of Uzbekistan”](https://agriculture-oshsu.com/en/journals/tom-3-2-2024/karabairskaya-poroda-loshadey-zolotoy-fond-uzbekistana): Uzbek agricultural reference for the breed’s importance, regional breeding context, exterior description, and reported black/dark-bay emphasis. [agriculture-oshsu](https://agriculture-oshsu.com/en/journals/tom-3-2-2024/karabairskaya-poroda-loshadey-zolotoy-fond-uzbekistana)

- [Karabair breed profile](https://madbarn.ca/karabair-horse-breed-profile/): detailed secondary synthesis for current Central Asian distribution, 14.3–15.1-hand range, 400–500 kg mass, conformation, common colors, uses, hardiness, intrabreed variation, and the absence of documented breed-specific inherited disease data. [madbarn](https://madbarn.ca/karabair-horse-breed-profile/)