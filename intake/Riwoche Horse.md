The **Riwoche Horse** should be a very rare, visually unified Tibetan highland pony: compact, primitive-looking, almost entirely dun, and defined by a short upright mane, dorsal stripe, leg barring, and a stark mountain-valley origin. It must not be framed as a surviving wild horse or “missing link”—DNA testing found it genetically indistinguishable from ordinary modern domestic horses—but its isolated population and extraordinary appearance make it an excellent breed for a Mendelian genetics mod. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL was not retrievable through the available reader. The following is written in the readable JSON convention used in your example. Map exact key names, locus names, enum values, and file registration to the real `common/breed/spec/` schema before compiling.

## Identity & flavour

The **Riwoche Horse**, also called the **Riwoche Pony**, is a small Tibetan horse from the highland region around **Riwoche County** in eastern/northeastern Tibet, within the wider Kham plateau horse culture. It came to international attention in 1995, when French ethnologist **Michel Peissel** and his expedition encountered a herd in an isolated valley reached by crossing a mountain pass around 5,000 metres above sea level. Local Tibetan Bon-po people had used these horses for generations as domestic riding and pack animals; the “discovery” was a discovery by outside researchers, not the beginning of the breed or its relationship with local people. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

The Riwoche was made for movement in a severe highland environment: transport, pack work, riding, and daily travel along paths where altitude, cold, rock, wind, and distance matter more than speed on level ground. It should feel spirited, intelligent, bold, and self-reliant—a little Tibetan mountain horse that carries itself efficiently, chooses footing carefully, and keeps moving through a landscape where large modern sport horses would be impractical. It is not a heavy draught animal and not a racehorse; it is an economical working pony from a remote high plateau. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

Riwoche horses stand approximately **12 hands (122 cm)** at the withers. Their shape is remarkably distinctive: angular and compact, with a heavy or wedge-like head, small ears, a straight flat forehead, small jaws, narrow “duck-bill” nostrils, a rough coat, and a **short upright mane** rather than a long hanging forelock. The body recalls the stylized dun horses of prehistoric cave art—not because it is literally a prehistoric survivor, but because the breed preserves a familiar primitive-type silhouette: pale body, dark points, dark dorsal stripe, barred lower legs, and alert upright mane. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

The real Riwoche coat is overwhelmingly **dun**. It is described as beige/tan or dun with a black dorsal stripe and black leg striping, the primitive markings that make it immediately recognizable. This breed should therefore be intentionally visually unified: every pure founder carries dun, while extension and agouti are constrained toward a bay-dun background. The resulting population should be mostly classic bay dun, with a small mouse-dun/grullo possibility; chestnut/red dun should be absent or vanishingly rare, because the available breed descriptions repeatedly present a dark-maned beige dun rather than a broad spectrum of red, gray, pinto, or diluted colors. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

The Riwoche’s cultural footprint rests on a cautionary tale in horse history. Its 1995 discovery prompted claims that it might represent a living relic between prehistoric wild horses and modern domesticated horses. Blood samples taken from the herd were later tested, and the results did not show the genetic divergence expected of a remnant wild population: Riwoche horses are domestic horses. That correction makes them more interesting, not less. They are a living example of how isolation, local selection, and a tightly maintained traditional type can make an ordinary domestic horse population look astonishingly ancient. In Procedural Horse Genetics, players should recognize one at a glance as a tiny, dun, upright-maned Tibetan highlander. The mod does not model altitude acclimation, short upright mane structure, exact dorsal/leg-bar patterning, nostril shape, rough seasonal coat, primitive head profile, high-altitude endurance, local cultural use, or whether a horse has been recognized by a community as Riwoche. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

## Breed JSON

```json
{
  "id": "riwoche_horse",
  "name": "Riwoche Horse",
  "type": "natural",
  "notes": "The Riwoche Horse is defined by a compact Tibetan highland body, primitive dun appearance, dorsal stripe, leg barring, short upright mane, rough seasonal coat, narrow nostrils, and adaptation to remote high-altitude travel as a riding and pack horse. Procedural Horse Genetics does not model mane posture or bristle texture, dorsal-stripe width, leg-bar count, countershading, mealy muzzle, nostril shape, head profile, rough winter coat, altitude acclimation, highland terrain judgement, local Bon-po cultural practice, pack training, or community-based breed recognition.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:frozen_peaks",
    "minecraft:snowy_slopes",
    "minecraft:grove",
    "minecraft:meadow",
    "terralith:alpine_highlands",
    "terralith:alpine_grove",
    "terralith:rocky_mountains",
    "terralith:highlands"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1080,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.99,
      "e": 0.01
    },
    "agouti": {
      "A": 0.95,
      "a": 0.05
    },

    "dun": {
      "D": 1.0
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
    "jump": 5,
    "health": 10,
    "size": [
      0.82,
      0.90
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Dun | `D` 1.0 | Every pure founder receives the dominant dun allele. The Riwoche is consistently described as a dun/beige pony with a dark dorsal stripe and dark leg striping; a visually unified dun pool is therefore more faithful than allowing random non-dun founders. This is phenotype-based breed design, not a published TBX3 frequency survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse) |
| Extension | `E` 0.99 / `e` 0.01 | The cited descriptions consistently present a black-maned, dark-pointed dun horse, which is most naturally modeled as a bay-dun base. A near-fixed `E` keeps red dun effectively absent while allowing a negligible amount of background genetic variation.  [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse) |
| Agouti | `A` 0.95 / `a` 0.05 | A high agouti frequency yields classic bay dun as the dominant result. The low `a` rate permits rare mouse/grullo dun horses without changing the breed’s recognizable beige, dark-pointed majority. This is an explicit game approximation because no Riwoche ASIP study was located. |
| Other dilutions | Forced wild type | Gray, cream, pearl, champagne, silver, mushroom, and flaxen are unsupported by the available Riwoche source record and would dilute the breed’s deliberately narrow primitive-dun visual identity. |
| White patterns | Forced wild type | No evidence was found for tobiano, sabino, frame, splash, W-series white spotting, roan, rabicano, leopard complex, or brindle in this isolated dun population. |
| Disorders | All clear | No defensible Riwoche-specific carrier frequencies were located for the requested disease panel. The population’s small size and isolation are not enough to justify inventing disease risk. |
| Speed | 4/10 | Riwoche horses are practical highland pack and riding animals, not specialist racers.  [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse) |
| Jump | 5/10 | Mountain travel implies balance and basic obstacle competence, but no source supports modern show-jumping selection. |
| Health | 10/10 | Represents high-altitude hardiness, rough-coat resilience, and survival in remote mountain conditions. It does not claim immunity to infectious, nutritional, or inherited disease.  [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse) |
| Size | ×0.82–0.90 | Produces a compact, pony-sized highland horse while keeping the literal real-world height statement confined to flavor text.  [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse) |

## Code map

| Location | Riwoche Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `riwoche_horse` natural record with its display name, notes, highland biomes, very-rare weight, sources, price, dun-focused coat pool, disorder policy, and body-stat targets. |
| `common/breed/Breeds` | Register `riwoche_horse` for founder selection, world spawning, H-menu display, breed-book display, stable systems, and saved-genome lookup. |
| `common/breed/BreedSource` | Enable `wild`, `cowboy`, `spawn_egg`, and `stable`; wild spawning should remain rare and restricted to severe highland biomes. |
| `common/breed/BreedBands` | Retain an empty epigenetic-band map. The primitive visual effect comes from fixed dun plus constrained base loci, not from an arbitrary dark/light shade band. |
| `common/breed/spec/` | Add bidirectional parsing and serialization for the JSON file, replacing proposed key names and allele symbols with the current source-code schema if needed. |
| `common/breed/Commonness` | Ensure `VERY_RARE` corresponds to the requested rarity-ladder weight of 0.75. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 5, health 10, and the compact size range into legal `TargetBand` entries. |
| `common/breed/BreedFounder` | Force the dun allele in every pure founder; roll only extension and agouti from their constrained pools; set unnamed coat loci wild type and all listed disease loci clear. |
| `common/breed/BreedLineage` | No Riwoche-specific behavior is required. |
| `common/genetics/SpliceOutcome` | Use normal splice-carrot allele-transfer rules. There is no Riwoche exception. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use standard body-stat target bands for a compact, low-speed, terrain-capable, exceptionally hardy highland pony. |
| `common/breed/BandType` | No custom `TRADITIONAL` or `BACHELOR` band use is needed unless the active schema requires an explicit default. |

## Verification

1. Confirm **Riwoche Horse** appears in the H-menu’s Breeds tab and the breed book with its natural classification, very-rare status, Tibetan-highland biome selection, four enabled sources, price, and notes.

2. Trigger wild pack generation in an eligible windswept hill, stony peak, jagged peak, frozen peak, snowy slope, or modded alpine biome. Every horse generated in the chosen pack should identify as **Riwoche Horse**.

3. Confirm a lone wild horse produced outside the selected breed-pack mechanism reads **Feral Mixed**, not Riwoche Horse.

4. Generate at least 500 pure founders. Every founder must carry the dun allele, and the overwhelming visible result should be bay dun: beige/tan body, dark points, dorsal stripe, and leg striping. A small number of mouse/grullo dun founders may occur.

5. Verify that chestnut and red dun do not arise from pure founders in ordinary sample sizes. With `e` at 0.01, red-dun probability should be extremely small; if the code’s founder roller creates a large chestnut group, verify that it is honoring allele rates rather than interpreting `e` as a phenotype rate.

6. Confirm that pure founders never roll gray, cream, pearl, champagne, silver, mushroom, flaxen, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical traits.

7. Inspect the disease panel across a large founder sample. All listed disorder loci must remain clear. Any later disease allele must be attributable to intentional outcrossing or direct genetic intervention.

8. Compare Riwoche founders with Huculs, Koniks, and generic feral horses. The Riwoche should be smaller and more visually uniform than the Hucul, more strongly and consistently dun than a generic feral population, and characterized by rare highland heritage rather than wild-horse ancestry.

## Sources

- [Riwoche Horse overview](https://en.wikipedia.org/wiki/Riwoche_horse): secondary synthesis for the 1995 Riwoche expedition, isolated-valley location, 12-hand stature, dun coat with dorsal stripe and leg barring, primitive appearance, later DNA conclusion that the population is not genetically divergent from modern domestic horses, and local riding/pack use. [en.wikipedia](https://en.wikipedia.org/wiki/Riwoche_horse)

- [The Spokesman-Review — “Expedition Finds Unusual Horses in High Valley of Tibet”](https://www.spokesman.com/stories/1995/nov/12/expedition-finds-unusual-horses-in-high-valley-of/): contemporaneous reporting on the 1995 expedition, approximate four-foot height, triangular/wedge-like head, beige coat, bristly mane, black dorsal stripe, leg markings, and early “relic population” hypothesis. [spokesman](https://www.spokesman.com/stories/1995/nov/12/expedition-finds-unusual-horses-in-high-valley-of/)

- [The Equinest — Riwoche Pony](https://theequinest.com/breeds/riwoche-pony): secondary breed profile used for its Riwoche regional origin, long local Bon-po use, average 12-hand height, heavy straight-profiled head, small ears, upright mane, traditional dun-with-primitive-markings description, spirited/intelligent/bold temperament, and transport/pack use. [theequinest](https://theequinest.com/breeds/riwoche-pony)