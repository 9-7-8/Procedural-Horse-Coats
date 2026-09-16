The **Sanhe** should be represented as a rare Chinese improved riding, harness, and light-draught horse from Hulunbuir, Inner Mongolia: taller and more powerful than a Mongolian Horse, but still adapted to open northern grassland and severe cold. It is most reliably described as **chestnut- and bay-dominant**, with other colors rare; pure founders should therefore be solid bay, chestnut, brown, and a small black minority, while gray, dun, cream, pinto, leopard, and magical coat genetics remain absent. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/)

## Identity & flavour

The **Sanhe Horse**, also called **Sanhe**, **Sanho**, **Sanpeitze**, **Hailar**, or **Hai-La-Erh**, is an improved Chinese horse breed from the Sanhe district of Hulunbuir in northeastern Inner Mongolia. Its name means “three rivers,” reflecting the grassland basin where it developed. The modern breed formed during the twentieth century when local Mongolian horses were crossed with imported Russian breeds—including Zabaikal-type horses—and later with Thoroughbred and other selected stock. It is therefore neither a pure Mongolian steppe horse nor a European warmblood transplanted into China: it is a northern Chinese working and riding horse deliberately built from both. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/)

The Sanhe was made for the grasslands. It needed more height, pulling power, and speed than the small local Mongolian horse while preserving the ability to endure wind, snow, sparse winter pasture, and enormous travel distances. Sanhes serve as riding horses, local race horses, harness horses, light draught horses, and practical livestock-country mounts. In the modern Hulunbuir image, they are renowned for good speed, significant pulling strength, and stamina, even through winters that can reach around −40 °C. In Minecraft, this should translate to a medium-large, cold-country all-rounder: quicker than a compact native pony, stronger than a fine saddle horse, and more versatile than either. [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses)

A Sanhe normally stands about **150–155 cm**, roughly **14.3–15 hands**, with some modern sources placing the upper range nearer 15.2 hands. It has a stronger, taller silhouette than a Mongolian horse: a medium-to-large straight-profiled head, slightly heavy but muscular neck, well-sloped shoulder, broad deep chest, long and strong body, rounded muscular croup, sturdy legs with sound joints, and hard hooves. The mane and tail are full and practical, and there should be little to no feathering. At a glance, a Sanhe should read as an improved northern grassland horse—solid, big-bodied, and athletic—not as a tiny steppe pony, a heavy Belgian-style draught, or a refined Olympic warmblood. [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses)

The coat palette should be deliberately conservative. The strongest accessible breed reference says Sanhes are **usually chestnut or bay**, with other colors rare. That is the appropriate pure-founder rule: chestnut should be common; bay should be common; brown and black should occur in small numbers; and gray may be omitted unless a direct Chinese breed-standard source supports it. Cream, dun, champagne, silver, pearl, mushroom, roan, pinto, frame, splash, leopard complex, and broad white patterns should be forced wild type. A Sanhe should look like a handsome solid working horse in red or bay tones, not like a color-bred pinto or a gray-dominant Russian trotter. [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses)

Sanhes are valued for endurance, strength, tractability, hardiness, and useful speed. Their character should feel practical rather than dramatic: confident enough for open steppe travel, strong enough for light traction, and athletic enough for local racing. In Procedural Horse Genetics, players should breed toward a chestnut or bay northern Chinese harness-and-riding horse with a larger body and more forward speed than the Mongolian Horse, but without losing cold-country durability. The mod does not model true −40 °C survival, winter forage excavation, pulling force, harness action, local racing style, livestock work, exact Russian/Zabaikal/Thoroughbred ancestry proportions, hoof quality, mane density, or the cultural landscape of Hulunbuir pastoralism. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the user-provided example. Before compiling, reconcile actual gene IDs, allele labels, source enums, rarity values, price units, and body-stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The sources support the Sanhe’s twentieth-century origin, Mongolian-plus-imported ancestry, height, practical uses, climate adaptation, and chestnut/bay-dominant color description. No representative Sanhe genotype study was found supplying `MC1R`, `ASIP`, gray, dilution, or disease allele frequencies. The numbers below are transparent **gameplay approximations**, not official Chinese breed registry statistics. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/)

```json
{
  "id": "sanhe",
  "name": "Sanhe",
  "type": "natural",
  "notes": "The Sanhe Horse, also called Sanho, Sanpeitze, Hailar, or Hai-La-Erh, is a northern Chinese Inner Mongolian riding, harness, light-draught, livestock, and local-racing horse developed from Mongolian horses crossed with Russian Zabaikal-type, Thoroughbred, and other imported stock. Its real identity includes severe-cold adaptation, steppe travel, pulling power, harness action, hard hooves, local pastoral management, exact foundation-breed proportions, regional racing culture, and working temperament. Procedural Horse Genetics does not model those traits directly.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:snowy_plains",
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:river",
    "minecraft:frozen_river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 820,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.62,
      "e": 0.38
    },
    "agouti": {
      "A": 0.74,
      "a": 0.26
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
      "N": 0.96,
      "f": 0.04
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.99,
      "SB1": 0.01
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
      "N": 0.99,
      "Rb": 0.01
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

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Sanhe × Sanhe produces Sanhe. Sanhe × another pure breed produces a Sanhe cross. A Sanhe cross bred back to pure Sanhe remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every tall chestnut or bay Mongolian-type horse as Sanhe: real Sanhe identity depends on its Inner Mongolian improved-breed lineage and breeding history, not coat, size, or cold biome alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | Improved Mongolian-derived Chinese working horse | Sanhe was developed in Inner Mongolia from local Mongolian stock with imported Russian Zabaikal, Thoroughbred, and other breed influence. It should be larger and more athletic than a Mongolian Horse while retaining northern grassland resilience.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/) |
| Base-color pool | `E: 0.62 / e: 0.38`; `A: 0.74 / a: 0.26` | These values yield a population with substantial chestnut plus predominantly bay among black-pigment horses, with relatively little black. That reconstructs the source description: chestnut and bay usual, other colors rare. The values are gameplay approximations.  [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses) |
| Chestnut | Common `e/e` outcome | With `e: 0.38`, approximate chestnut frequency under random pairing is \(0.38^2 \approx 14\%\). This is lower than bay but common enough for a genuine chestnut-and-bay population; increase `e` toward 0.45 if playtesting yields too few chestnuts. |
| Bay | Common `E_ A_` outcome | A high `A` rate means black pigment is usually restricted to points, producing bay. This makes bay the likely largest single phenotype group. |
| Black | Minor `E_ a/a` outcome | Low `a` frequency permits a small number of black horses without treating black as a central Sanhe color. |
| Gray | Forced wild type | The stronger accessible sources characterize Sanhe as chestnut/bay dominant, with other colors rare. No reliable Sanhe-specific evidence supports seeding progressive gray in the default founder pool.  [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses) |
| Dilutions | Cream, pearl, champagne, silver, mushroom, dun forced wild type | No reliable source supports adding these to a conservative Sanhe founder population. The historical crossbred origin is not evidence for every possible coat allele. |
| Flaxen | `f: 0.04` | A tiny optional modifier permits occasional light-maned chestnut texture, but it should not become a defining trait. This is a gameplay approximation. |
| White patterns | Tobiano, frame, splash, KIT white, roan, leopard complex, PATN, brindle forced wild type | The intended pure Sanhe phenotype is a solid northern Chinese riding/harness horse. Rare small markings do not justify major inherited pinto or leopard loci. |
| Minor marking texture | `SB1: 0.01`, `Rb: 0.01` | These are near-absent optional modifiers for ordinary small white or hair variation only. Remove them if the mod renders either as conspicuous pinto-like body patterning. |
| Magical loci | All forced wild type | Grassland hardiness, pulling power, and cold adaptation are natural traits. |
| Disorders | All named loci clear | No defensible Sanhe-specific carrier-frequency source was located for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `7/10` | The breed is reported to combine good speed with endurance and practical riding/racing use, while remaining below a specialized flat-racing breed.  [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses) |
| Jump | `5/10` | Sanhes are versatile, strong saddle horses, but no source supports specialist jumping selection. |
| Health | `9/10` | Represents cold tolerance, resilience, grassland adaptation, hard hooves, stamina, and working versatility—not immunity from disease or injury.  [theequinest](https://theequinest.com/breeds/sanhe) |
| Size | `×1.00–1.11` | Models the 150–155 cm, approximately 14.3–15-hand, medium-large improved grassland horse.  [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses) |

## Disorder approach

All named disorder loci are **clear** in the pure Sanhe founder pool.

That is a scientific evidence decision. The retrieved sources document Sanhe’s origin, function, climate adaptation, body type, and color tendencies, but none provides a population-level carrier or allele frequency for ACAN dwarfism, WFFS, frame-associated lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

The breed’s imported foundation ancestry does not justify copying disease rates:

- Thoroughbred ancestry does not justify a generic WFFS rate.
- Russian/Zabaikal ancestry does not justify importing a trotter or draft disease frequency.
- Mongolian ancestry does not justify a Mongolian Horse health-genetics assumption.
- Cold adaptation does not demonstrate absence or presence of any Mendelian disorder.

Keep the founder disease pool clear until a direct Sanhe testing dataset becomes available.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `sanhe` as a natural Chinese Inner Mongolian improved horse record with Hulunbuir grassland flavour, compact strong body targets, chestnut/bay-focused coat pool, and clear disorders. |
| `common/breed/Breeds` | Register `sanhe` for wild packs, stable access, spawn eggs, H-menu display, breed books, command lookup, saved genomes, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Sanhe is a regional Chinese working/riding breed, not a standard sale-yard breed. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band object. Do not use a band to fake cold adaptation, muscle, pulling strength, hoof quality, or harness action. |
| `common/breed/spec/` | Match every illustrative field to the actual parser/writer. Confirm exact Extension, Agouti, flaxen, Sabino 1, rabicano, source, and numeric-probability syntax. |
| `common/breed/Commonness` | Confirm `RARE` maps to the desired rarity ladder and preserve `spawn_weight: 1.5` only if direct numerical weights are valid. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 5, health 9, and size ×1.00–1.11 into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Roll only the declared base-color and near-absent minor marking loci; force all dilution, pinto, leopard, magical, and disease loci wild type or clear; apply medium-large northern working-horse stats. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed behavior. A larger chestnut or bay horse in a cold grassland biome must not acquire Sanhe lineage purely from appearance or stats. |
| `common/genetics/SpliceOutcome` | No special rule. A spliced allele should inherit normally and can create a nonstandard Sanhe descendant with pinto, leopard, gray, dilution, disease, or other excluded traits. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize medium-large size, good practical speed, moderate jump, and high heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the actual serializer mandates an explicit default. |

## Verification

1. Confirm **Sanhe** appears in the H-menu’s Breeds tab and breed book with its Hulunbuir/Inner Mongolia origin, Mongolian-plus-imported development history, riding/harness/light-draught role, rare commonness, and cold-grassland biome mapping.

2. Spawn repeated packs in plains, meadows, windswept hills, snowy plains, groves, taiga margins, and frozen river valleys. Every horse in a selected Sanhe pack must display the **Sanhe** breed label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is bay, chestnut, tall, powerful, or generated in a northern grassland biome.

4. Generate at least 1,000 pure founders. The population should be made up primarily of chestnut and bay horses, with a smaller brown/dark-bay group and a very small black group. Every founder should appear solid-bodied.

5. Confirm that pure founders do not generate gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, pinto, frame overo, splash, strong Sabino body white, dominant white, roan, rabicano body effects, leopard complex, brindle, or magical coats.

6. Inspect genomes:
- `E/e` must allow a genuine chestnut population.
- `A` must be common enough for bay to be frequent among black-pigment horses.
- `a` must remain low enough that black is uncommon.
- Only very rare `f`, `SB1`, and `Rb` modifiers may occur if retained.
- All major dilution, pinto, leopard, magical, and named disease loci must be wild type or clear.

7. Confirm mature stat behavior. Sanhes should be taller and stronger than a Mongolian Horse, with better practical speed and light-draught versatility, but still high in heartiness and far below high-end European sport-horse jumping capacity.

8. Test default lineage:
- Sanhe × Sanhe → Sanhe.
- Sanhe × Mongolian Horse → Sanhe cross.
- Sanhe × Hequ → Sanhe cross.
- Sanhe cross × pure Sanhe → the existing Sanhe cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test controlled genetic introduction. Add gray, dun, cream, tobiano, leopard complex, heavy-draft size, WFFS, PSSM1, or another excluded trait via normal crossing or splice mechanics. Descendants should inherit normally but must not acquire pure Sanhe status from chestnut/bay color, large size, or northern-biome placement alone.

## Sources

- [Selection signatures for local and regional adaptation in Chinese Mongolian horse populations](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/): peer-reviewed genomic source distinguishing Sanhe from indigenous Chinese Mongolian types and describing its development from local Mongolian stock crossed with imported European and other horses. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9854188/)

- [Oklahoma State University — Sanhe Horses](https://breeds.okstate.edu/horses/sanhe-horses): concise university breed reference for northeastern Inner Mongolia origin, chestnut/bay-dominant colors, riding/racing/harness/light-draught uses, and cold-country adaptation. [breeds.okstate](https://breeds.okstate.edu/horses/sanhe-horses)

- [Sanhe horse profile](https://theequinest.com/breeds/sanhe): supplementary source for Hailar/Sanho/Sanpeitze aliases, three-rivers geography, early twentieth-century Russian and Thoroughbred crosses, approximately 14.3–15-hand size, conformation, hard hooves, and broad working uses. [theequinest](https://theequinest.com/breeds/sanhe)

- [The Sanhe horse: an iconic breed of Hulun Buir Grasslands](https://www.globalpeople.com.cn/waphtml/channel/147/115284.html): regional source for Sanhe location, imported Zabaikal/Mongolian/Thoroughbred ancestry, speed, pulling power, endurance, and Chinese national-breed cultural importance. [globalpeople.com](https://www.globalpeople.com.cn/waphtml/channel/147/115284.html)

- [CGTN — Sanhe horse thriving at −40 °C](https://news.cgtn.com/news/2026-01-25/Meet-China-s-famous-horses-The-Sanhe-horse-that-thrives-at-40--1Kclzj4NZYI/p.html): supplementary cold-adaptation context for the modern twentieth-century Sanhe breed. [news.cgtn](https://news.cgtn.com/news/2026-01-25/Meet-China-s-famous-horses-The-Sanhe-horse-that-thrives-at-40--1Kclzj4NZYI/p.html)