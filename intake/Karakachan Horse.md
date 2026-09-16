The **Karakachan Horse**—also called the **Karakachan Pony**, **Karakachanska kon**, or **Karakachanski kon**—should be a very rare Bulgarian primitive mountain horse with a visually unified bay-to-dark-brown founder pool. Its core gameplay identity is not a flashy color pattern or racing specialty, but an extraordinarily hardy, compact, strong-hoofed Balkan pack horse made for semi-wild life in the Rhodopes, Rila, Pirin, Stara Planina, and Kraishte mountains. [rewildingeurope](https://rewildingeurope.com/news/free-roaming-karakachan-horses-enrich-natural-diversity-in-the-rhodopes/)

> **Schema caveat:** The provided Procedural Horse Genetics wiki was not retrievable through the available reader. The JSON uses the readable convention from your supplied example. Match the precise property names, genetic-locus IDs, enum values, and file-registration requirements to the current `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Karakachan Horse** is an indigenous Bulgarian mountain horse traditionally associated with the **Karakachans**, Balkan transhumant pastoralists whose seasonal livestock movements shaped the breed for centuries. It is variously called the **Karakachan Pony**, **Karakachanska kon**, or **Karakachanski kon**. Rather than arising from a fashionable stud crossing program, it coalesced as a tough regional landrace under hard use: horses had to travel with flocks, carry people and supplies between winter and summer pasture, survive outside, and continue working on steep ground with limited feed. It is commonly described as one of Europe’s oldest surviving native pony types, retaining a close-to-aboriginal mountain-horse character. [artbycrane](https://www.artbycrane.com/horse_breeds/pony_breeds/karakachan.html)

The Karakachan was made for **mountain transport, pack work, light draught, riding, and pastoral movement**. Its historical task was straightforward and demanding: carry a rider or cargo through terrain where carts, large farm horses, and machinery could not reliably travel. Short, thick legs, a broad deep chest, a compact body, and exceptionally hard hooves let it negotiate rock, mud, narrow trails, alpine pasture, and abrupt weather changes. It should feel sturdy under a load, unhurried but determined, and clever enough to conserve itself over long distances rather than waste energy in a burst of speed. [theequinest](https://theequinest.com/breeds/karakacan)

Modern measured Karakachan horses average **130.22 cm (about 12.3 hands)** at the withers, with a reported body length of 140.57 cm, chest girth of 157.51 cm, and cannon circumference of 17.67 cm. Older descriptions often place the breed closer to 128 cm, while broader modern profiles give approximately 126–138 cm. The build is compact and surprisingly substantial: a broad forehead and generally straight-to-slightly-convex profile, small lively eyes, short muscular neck, deep broad chest, broad sometimes slightly concave back, thick legs, and durable hooves. The breed may carry ordinary working-horse mane and tail, but its signature is not feather or mane length; it is a dense, low, powerful body that looks made to climb. [agrojournal](https://www.agrojournal.org/24/02-16.html)

Traditional descriptions say Karakachans are **almost always bay or brown**. That makes this one of the clearest visually unified populations in the mod: pure founders should be overwhelmingly bay, dark bay, brown, or seal brown, with a small black possibility retained only to avoid falsely claiming a molecularly fixed agouti pool. Chestnut, gray, dun, cream, pearl, champagne, silver, roan, tobiano, frame, splash, leopard spotting, and magical colors should not emerge from pure Karakachan founders. The result should be a herd that feels recognizably ancestral and practical: dark, earth-colored mountain horses against rock, grass, and conifer forest. [theequinest](https://theequinest.com/breeds/karakacan)

The Karakachan’s modern cultural story is conservation through use. It has been listed as critically endangered in Bulgaria’s Red Data List of autochthonous domestic forms, but protected breeding, a studbook initiative, and semi-free herds in mountain landscapes have given it a route back from disappearance. Rewilding projects in the Rhodopes have used Karakachan herds as natural grazers; by 2018, four herd groups in that project area totaled more than 80 horses. In Procedural Horse Genetics, players should see the Karakachan as a rare heritage mount worth finding and protecting: modest in speed and jumping, unusually high in heartiness, and at home in remote hills where a refined sport horse would feel out of place. The mod does not simulate true hoof hardness, snow and rock traction, seasonal transhumance, grazing effects on biodiversity, instinctive mountain navigation, load capacity, body-condition resilience, or the real studbook’s phenotype and pedigree decisions. [rewildingeurope](https://rewildingeurope.com/news/free-roaming-karakachan-horses-enrich-natural-diversity-in-the-rhodopes/)

## Breed JSON

```json
{
  "id": "karakachan_horse",
  "name": "Karakachan Horse",
  "type": "natural",
  "notes": "The Karakachan Horse is defined by semi-wild mountain management, transhumant pastoral work, compact and heavily muscled conformation, very hard hooves, trail sense, low-input survival, and usefulness as a pack, light-draught, and riding horse. Procedural Horse Genetics does not model hoof-horn hardness, exact body-condition resilience, rock and snow traction, transhumant migration, instinctive trail selection, carrying capacity, livestock-herding context, conservation-grazing effects, regional tack, or real Karakachan studbook eligibility and phenotype inspection.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:grove",
    "minecraft:meadow",
    "minecraft:old_growth_pine_taiga",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:taiga",
    "terralith:alpine_highlands",
    "terralith:alpine_grove",
    "terralith:rocky_mountains",
    "terralith:highlands"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "any",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.96,
      "e": 0.04
    },
    "agouti": {
      "A": 0.93,
      "a": 0.07
    },

    "grey": {
      "N": 1.0
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
    "speed": 4,
    "jump": 5,
    "health": 10,
    "size": [
      0.82,
      0.93
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Bay/brown visual unity | `E` 0.96 / `e` 0.04; `A` 0.93 / `a` 0.07 | The available breed sources describe Karakachans as “almost always bay or brown.” High `E` and `A` rates make bay/dark bay/brown the near-universal pure-founder result while retaining only tiny residual chestnut and black possibility. These are conservative phenotype-driven gameplay estimates, not measured MC1R/ASIP population frequencies.  [theequinest](https://theequinest.com/breeds/karakacan) |
| No gray, dilution, or loud pattern pool | All forced wild type | No accessible breed-standard or genetic study supports gray, dun, cream, pearl, champagne, silver, mushroom, roan, pinto, frame, splash, W-series dominant white, leopard complex, rabicano, brindle, or magical genes as expected pure Karakachan founder alleles. |
| Disorders | All clear | No defensible Karakachan-specific carrier-rate data was located for the mod’s requested disorder panel. A primitive, hardy phenotype is not evidence for immunity, but there is also no basis to import disease rates from other pony, draft, Arabian, or Warmblood breeds. |
| Speed | 4/10 | The breed was selected for practical mountain transport and long steady work, not flat racing.  [theequinest](https://theequinest.com/breeds/karakacan) |
| Jump | 5/10 | Strong uphill balance, short powerful limbs, and rough-terrain movement justify moderate agility, but not sport-jumping specialization. |
| Health | 10/10 | Encodes exceptional ecological hardiness: semi-wild management, long-term mountain adaptation, and demonstrated suitability for free-roaming Rhodope rewilding herds. It is a gameplay heartiness score, not a claim that all individual health disorders are impossible.  [rewildingeurope](https://rewildingeurope.com/news/free-roaming-karakachan-horses-enrich-natural-diversity-in-the-rhodopes/) |
| Size | ×0.82–0.93 | Produces a small but substantial mountain horse, aligned with the documented average near 130 cm without placing real-world height in the JSON.  [agrojournal](https://agrojournal.org/24/02-16.pdf) |

## Code map

| Location | Karakachan Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `karakachan_horse` natural breed record, its flavor notes, biome/source metadata, price, rarity, coat pool, clear disorder panel, and stat targets. |
| `common/breed/Breeds` | Register the breed ID for spawning, H-menu and breed-book display, stable stock, serialized genomes, and breed lookup. |
| `common/breed/BreedSource` | Enable `wild`, `cowboy`, `spawn_egg`, and `stable`, allowing rare mountain packs as well as managed conservation-style acquisition. |
| `common/breed/BreedBands` | Leave `epigenetic_bands` empty. The Karakachan’s dark-bay identity comes from its constrained extension/agouti pool rather than fixed shade bands. |
| `common/breed/spec/` | Add bidirectional file parsing and serialization, adapting the proposed JSON’s field names and loci to the actual active schema. |
| `common/breed/Commonness` | Confirm `VERY_RARE` maps to the requested `0.75` rarity weight. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 5, health 10, and compact size ×0.82–0.93 into legal `TargetBand` definitions. |
| `common/breed/BreedFounder` | Roll only extension and agouti from the supplied non-wild pools, while forcing omitted coat loci to wild type and all disorder loci clear. |
| `common/breed/BreedLineage` | No Karakachan-specific lineage change is necessary. |
| `common/genetics/SpliceOutcome` | Apply standard splice-carrot allele transfer rules without a breed exception. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use ordinary trait bands to encode low racing specialization, moderate terrain agility, extreme heartiness, and a compact pony-sized body. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` use is needed unless required as an explicit default by the actual schema. |

## Verification

1. Confirm **Karakachan Horse** appears in the H-menu’s Breeds tab and the breed book with its natural designation, very-rare status, mountain biome range, source checklist, price, and conservation-focused notes.

2. Generate wild packs in an eligible peak, hill, grove, taiga, or mountain biome. Every horse created within a selected Karakachan pack should display **Karakachan Horse** as its breed label.

3. Confirm that an ordinary lone wild horse outside a breed-selected pack reads **Feral Mixed**, not Karakachan Horse.

4. Roll at least 1,000 pure founders. Nearly all should be bay, dark bay, brown, or seal brown. Chestnut and black should be genuinely exceptional; visual gray, dun, cream, pearl, champagne, silver, roan, pinto, leopard, brindle, and magical coats must never appear.

5. Inspect genotype pools rather than visual coats alone. Only `extension` and `agouti` should carry non-wild alleles. Every other named coat locus should be `N/N` in pure founders.

6. Verify the disorder panel over a large founder sample. Every listed disorder locus should remain clear. Any later disorder allele must be traceable to intentional outcrossing or a direct mod-genetics action.

7. Compare stat behavior with a light riding horse, a sport jumper, and a larger draught breed. Karakachans should be compact and relatively slow, competent on uneven terrain, and among the mod’s strongest hardy-exploration mounts without becoming the fastest, tallest, or most powerful hauler.

## Sources

- [BASJA / Agricultural Academy — “Studies of the Exterior of the Karakachan Horse Breed”](https://www.agrojournal.org/24/02-16.html): peer-reviewed exterior study using 404 measurements from 52 Karakachan horses. It reports mean withers height of 130.22 cm, body length of 140.57 cm, chest girth of 157.51 cm, cannon circumference of 17.67 cm, and notes that the modern population is taller and more massive than early twentieth-century horses. [agrojournal](https://www.agrojournal.org/24/02-16.html)

- [Rewilding Europe — Karakachan herds in the Rhodopes](https://rewildingeurope.com/news/free-roaming-karakachan-horses-enrich-natural-diversity-in-the-rhodopes/): conservation-grazing context, 2016 reintroductions, four project herds totaling more than 80 animals by 2018, the breed’s average height just under 130 cm, and its suitability for free-roaming life in the Rhodope Mountains. [rewildingeurope](https://rewildingeurope.com/news/free-roaming-karakachan-horses-enrich-natural-diversity-in-the-rhodopes/)

- [Karakachan pony conservation overview](https://www.artbycrane.com/horse_breeds/pony_breeds/karakachan.html): secondary account of the traditional semi-wild populations in the Rila, Rhodopes, Pirin, Stara Planina, and Kraishte regions; critically endangered classification; compact body, strong legs, hard hooves, and studbook/nucleus-herd preservation efforts. [artbycrane](https://www.artbycrane.com/horse_breeds/pony_breeds/karakachan.html)

- [Karakachan Horse profile](https://theequinest.com/breeds/karakacan): secondary breed description for Karakachan pastoral origin, mountain pack/riding/light-draught roles, conformation, reported 12.6–13-hand range, and the “almost always bay or brown” color description used to constrain the pure-founder coat pool. [theequinest](https://theequinest.com/breeds/karakacan)

- [New Thracian Gold — Karakachan Horse](http://www.newthraciangold.eu/cmspage.php?id=161&lng=en): supporting summary of the approximately 128 cm average, broad forehead, stocky mountain freight-and-riding conformation, short strong legs, and durable hooves. [newthraciangold](http://www.newthraciangold.eu/cmspage.php?id=161&lng=en)