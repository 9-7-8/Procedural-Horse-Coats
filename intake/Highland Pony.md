The **Highland Pony** should be a strong, compact Scottish mountain-and-moorland pony: predominantly dark or dun, dense-coated, sure-footed, and more substantial than most ponies. In gameplay, it should feel like a calm, durable pack-and-forestry pony with a flowing mane, primitive dun markings, excellent hardiness, and an unmistakable Highland silhouette—not a sleek sport pony or a gaited horse. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

## Identity & flavour

The **Highland Pony** is one of Scotland’s three native pony breeds, alongside the Shetland and Eriskay. It comes from the Scottish Highlands and Islands, where ponies of its general type have existed for centuries; ancient equine presence in Scotland may date back to at least the eighth century BCE, though the exact ancestry of the modern breed is not fully documented. The Highland as a recognizable, recorded breed took shape through the eighteenth and nineteenth centuries, with a registry beginning in the 1830s and the **Highland Pony Society** formally established in 1923 to preserve the type and maintain the stud book. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

This was a pony made for hard country and real labor. Highland ponies carried deer and game off the hills, hauled timber through forestry, pulled carts, ploughed crofts, packed goods over roadless ground, and served Highland communities where a large horse was impractical. They were also used for military work and as dependable transport animals in severe terrain. Their modern jobs include trekking, driving, showing, conservation grazing, hunting, family riding, and still, in some places, carrying deer down from the Scottish hills. A Highland Pony should feel powerful for its height: not fast in the racehorse sense, but able to keep going through rain, bog, heather, loose stone, and thin grazing. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

A Highland usually stands **13–14.2 hands**, about 132–147 cm, and is one of the largest British native ponies. It is compact, broad, and muscular, with a deep chest, well-sprung ribs, strong quarters, short cannon bones, well-developed forearms and thighs, broad knees and hocks, and tough round feet. Its mane and tail are long, flowing, and normally left natural rather than clipped into a show-horse shape. It has silky feather around the lower limbs and a dense double winter coat that sheds to a cleaner summer coat. In mod terms, it should look low, broad, sturdy, dark-legged, and weatherproof: a pony that could carry an adult across a wet Scottish glen. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

The signature Highland palette is **dun**. “Mouse” dun, yellow dun, gray dun, cream dun, red/f​ox dun, oatmeal dun, and biscuit dun are all terms encountered around the breed, with dorsal stripes, leg barring, and occasional shoulder stripes supplying the primitive look players should immediately recognize. The registry also accepts gray, brown, black, bay, and occasional liver chestnut with a light or silvery mane and tail. Broken colors such as pinto are not permitted, and white markings are highly restricted: a small star is acceptable, but white legs, extensive facial white, white hooves, and stallions with white beyond a small star are unacceptable in the traditional standard. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

Highlands are quiet, friendly, patient, intelligent, sensible, and willing—but they are not passive ornaments. Their intelligence and food-motivated native-pony thrift can turn into pushiness or stubbornness without consistent manners and useful work. Their easy-keeper constitution is a real-world strength in sparse country, but it comes with a modern management warning: rich pasture and excess feed can contribute to obesity, laminitis, and metabolic problems. In this mod, that translates best to high health and hardy founder statistics, not a fictional disease allele or invulnerability mechanic. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

The breed also carries a strong cultural identity. Queen Victoria favored Highland Ponies, and Queen Elizabeth II maintained a substantial working Highland stud at Balmoral; Highland ponies continue to work there in deer-management country. Although they have spread to North America, continental Europe, Australia, New Zealand, and elsewhere, they remain a conservation breed with a limited global population. The Highland Pony Society now actively uses its SPARKS kinship system to help breeders avoid high-coancestry matings and retain diversity in the purebred population. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

For *Procedural Horse Genetics*, the breeding goal should be a solid-color, dark-pointed or dun Highland with a broad pony frame, exceptional heartiness, modest all-round jumping ability, and a powerful working build. The best pure founders should look like they belong in a misty birch-and-spruce valley: mouse dun, yellow dun, bay, brown, black, or gray; compact; strong; and very difficult to mistake for a delicate riding pony. The mod does **not** model dense double coats, summer/winter coat changes, mane length, feathering, hoof hardness, body breadth, deer-packing ability, sure-footedness, easy-keeper metabolism, grazing behavior, discipline training, proper manners, white-marking inspection, or real registry eligibility.

## Breed JSON

> **Schema note:** The supplied PHC wiki URL could not be retrieved. This uses the JSON convention shown in your Anglo-Arabian example and should be reconciled against the actual serializers, allele IDs, enums, and defaulting logic in `common/breed/spec/` before use. In particular, verify whether the implementation calls the cream locus `cream` or `MATP`, whether it distinguishes `nd1` and `nd2` at dun, and whether its “gray dun” terminology is represented by independent `grey` plus `dun` genes.

```json
{
  "id": "highland_pony",
  "name": "Highland Pony",
  "type": "natural",
  "notes": "Procedural Horse Genetics models a Highland Pony's compact size, hardy constitution, working athleticism, and traditional solid-color coat pool. It does not model the broad native-pony build, head type, long flowing mane and tail, silky leg feathering, dense double winter coat, seasonal coat changes, hard round hooves, easy-keeper metabolism, sure-footedness, deer-packing ability, manners and stubbornness, white-marking inspection, or Highland Pony Society registration eligibility.",

  "biomes": [
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:birch_forest"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 520,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.86,
      "e": 0.14
    },
    "agouti": {
      "A": 0.76,
      "a": 0.24
    },

    "dun": {
      "D": 0.56,
      "nd1": 0.14,
      "nd2": 0.30
    },

    "grey": {
      "N": 0.78,
      "G": 0.22
    },

    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "flaxen": {
      "N": 0.84,
      "f": 0.16
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
    "pearl": {
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
    "jump": 6,
    "health": 9,
    "size": [
      0.82,
      0.93
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Highland Pony × Highland Pony produces Highland Pony. Highland Pony × another pure breed produces a Highland Pony cross. A Highland Pony cross bred to a pure Highland Pony remains that named cross; different crosses resolve to Mixed, and any lineage combined with Feral Mixed resolves to Mixed. Do not grant a Highland label solely because a cross is dun, dark, hardy, or pony-sized."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Reasoning |
|---|---:|---|
| Base-color pool | `E` 0.86 / `e` 0.14; `A` 0.76 / `a` 0.24 | The breed is mostly dark: black, brown, bay, gray, and dun forms are widely described, while chestnut/liver chestnut is uncommon. This pool favors bay/brown and black foundations but retains rare red-based founders. These are intentional gameplay estimates, not surveyed Highland allele frequencies.  [madbarn](https://madbarn.com/highland-pony-breed-profile/) |
| Dun | `D` 0.56 | Dun is the Highland’s most iconic coat family. A high dominant-dun frequency makes mouse dun, yellow/bay dun, gray dun, cream dun, and occasional red dun familiar sights without removing dark non-dun Highland founders. Primitive dorsal stripes and leg bars are a major visual cue.  [en.wikipedia](https://en.wikipedia.org/wiki/Highland_pony) |
| `nd1` | `nd1` 0.14 | Retaining a modest non-dun-1 fraction is appropriate if PHC models primitive markings separately from full dun dilution. It lets some non-dun dark Highland founders retain subdued primitive-striping potential. If the implementation only has `D` and ordinary non-dun, merge `nd1` into the `N` or non-dun state.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse) |
| Gray | `G` 0.22 | Gray is a recognized Highland color, and descriptions separately identify “grey dun” and ordinary gray. A moderate `G` pool allows gray Highlands without making the population gray-dominant. The mod should independently combine gray with dun, since “gray dun” in breed terminology may refer to a dun shade as well as a horse carrying the true gray gene.  [en.wikipedia](https://en.wikipedia.org/wiki/Highland_pony) |
| Cream | `Cr` 0.10 | “Cream dun,” “biscuit dun,” and related dilution terms are recognized in traditional Highland color vocabulary. A modest cream frequency allows cream-dun appearances and rare cream-derived colors without turning the breed into a palomino/buckskin population. This is an approximation because the source material describes phenotype categories, not MATP allele counts.  [en.wikipedia](https://en.wikipedia.org/wiki/Highland_pony) |
| Flaxen | `f` 0.16 | Liver chestnut with a light or silver mane and tail is an occasional recognized Highland color. The mod’s flaxen locus is the closest available tool for that traditional look; it is not a claim that all light-maned Highland chestnuts carry a confirmed flaxen genotype.  [livestockconservancy](https://livestockconservancy.org/highland-pony/) |
| Pinto and white-pattern genes | Forced wild type | The Highland standard excludes broken/pinto colors, and white markings beyond a small star are strongly discouraged or unacceptable. Setting tobiano, frame, splash, sabino, W-series white, leopard complex, roan, and rabicano to wild type preserves the recognizably solid Highland phenotype. Small non-genetic or baseline face markings, if PHC renders them separately, can still handle the permitted small star.  [en.wikipedia](https://en.wikipedia.org/wiki/Highland_pony) |
| Other dilutions | Forced wild type | No defensible breed-specific basis was found for champagne, silver, pearl, mushroom, leopard complex, true roan, brindle, or magical traits. The Highland should gain these through outcrosses, not generate them as pure founders. |
| Disorders | All named loci clear | No trustworthy Highland-Pony-specific carrier study was found for the listed monogenic disorders. It would be misleading to import frequencies from breeds in which those diseases are established. High metabolic risk in easy-keeper native ponies is a management phenotype, not proof of a listed single-locus disease allele.  [madbarn](https://madbarn.com/highland-pony-breed-profile/) |
| Speed | 5/10 | A Highland is a powerful, free-moving work pony, but its historic purpose is carrying, hauling, and traversing difficult ground rather than flat-out racing. Baseline speed keeps it useful without making it a trot-racing or Thoroughbred analogue.  [livestockconservancy](https://livestockconservancy.org/highland-pony/) |
| Jump | 6/10 | The breed’s compact strength, soundness, and broad modern riding versatility support a modestly above-baseline jump statistic. It should excel at practical terrain and small-to-medium obstacles rather than becoming an elite specialist show jumper.  [madbarn](https://madbarn.com/highland-pony-breed-profile/) |
| Health | 9/10 | This expresses native hardiness, durability, resistance to harsh weather, ability to use sparse grazing, and strong feet. It does not erase obesity, laminitis, metabolic syndrome, PPID, injury, or the consequences of poor management.  [madbarn](https://madbarn.com/highland-pony-breed-profile/) |
| Size | ×0.82–0.93 | A Highland is a large pony—13–14.2 hands—but still visibly below ordinary riding-horse height. This range should place it larger and heavier-feeling than many small native ponies while remaining a pony. Calibrate to PHC’s actual hands curve before finalizing.  [livestockconservancy](https://livestockconservancy.org/highland-pony/) |

## Spawning & lineage

The selected biome pool favors cold, wet, wooded, upland Minecraft terrain: groves, taigas, old-growth conifers, forests, meadows, and windswept hills. These are closer in feel to Highlands glens, heather moor edges, birch woods, mountain pasture, and forestry ground than plains, deserts, or warm savannas. The breed historically worked on crofts and in forestry and remains associated with rugged Highland weather and sparse grazing. [livestockconservancy](https://livestockconservancy.org/highland-pony/)

`UNCOMMON` with `spawn_weight: 3` gives the Highland Pony a meaningful discovery value without treating a real, internationally maintained conservation breed as fantasy-rare. Its limited population and active conservation efforts justify keeping it below generic breeds in a shared biome pool. The Highland Pony Society’s SPARKS project makes the preservation concern concrete: it monitors mean kinship, warns against pairings expected to yield 10% or greater co-ancestry, and aims to retain genetic diversity in the breed. [highlandponysociety](https://www.highlandponysociety.com/sparks-project/)

No special lineage rule is recommended. A pure Highland Pony is a registry population with a specific history, not merely any compact dark dun pony. A Highland × Fell, Highland × Dales, Highland × Fjord, or Highland × generic horse should remain a cross under the normal PHC lineage table even if the foal visually resembles its Highland parent.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Adds the `highland_pony` natural breed record, its display metadata, sources, biome list, price, commonness, and player-facing notes. |
| `common/breed/Breeds` | Registers `highland_pony` for generation, lineage display, menus, books, entity persistence, and command lookups. |
| `common/breed/BreedSource` | Validates the selected `wild`, `cowboy`, `spawn_egg`, and `stable` sources. |
| `common/breed/BreedBands` | Serializes an empty epigenetic-band object. Do not force a dark shade band; genetic variety between dun, gray, brown, bay, black, and chestnut is important to the breed. |
| `common/breed/spec/` | Confirms JSON field names, natural-type enum, coat/disorder locus IDs, allele spelling, source encoding, and bidirectional serializer/parser behavior. |
| `common/breed/Commonness` | Confirms that `UNCOMMON` maps to the project’s rarity ladder and that `spawn_weight: 3` matches the intended practical spawn pull. |
| `common/breed/BreedStatCurve` | Converts speed 5, jump 6, health 9, and size ×0.82–0.93 into legal founder `TargetBand` values. |
| `common/breed/BreedFounder` | Builds a Highland founder from only the listed allele pools and forces every omitted coat gene to wild type and disorder locus to clear. |
| `common/breed/BreedLineage` | Applies ordinary pure/cross/mixed behavior with no phenotype-based Highland exception. |
| `common/genetics/SpliceOutcome` | No custom exception. A splice-carrot allele must follow normal inheritance once introduced, even if it is outside the pure Highland founder pool. |
| `common/trait/StatAxis` | Uses speed, jump, health, and size for the breed target definition. |
| `common/trait/TargetBand` | Stores valid founder outcome ranges for the four stats. |
| `common/trait/BreedStatTargets` | Associates the Highland Pony’s stat targets with the breed record. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` band behavior is proposed for this breed. |

## Verification

1. **Breed registration**
   - Open the H menu’s Breeds tab and confirm **Highland Pony** appears with the correct name, natural classification, price, biome list, source availability, `UNCOMMON` commonness, and breed notes.
   - Confirm the breed book displays the same record and the saved entity ID remains `highland_pony` after a world reload.

2. **Pack generation**
   - Spawn packs in an eligible grove, taiga, old-growth conifer biome, meadow, forest, or windswept hill.
   - Verify every member created in one natural pack has the **Highland Pony** breed label.
   - Verify a lone ordinary wild horse outside a selected breed pack reads **Feral Mixed**, not Highland Pony.

3. **Coat audit**
   - Generate at least 200 Highland founders.
   - Confirm a strong visible dun presence, including dark mouse/grullo-like and yellow/bay-dun-like animals, with dorsal stripes and leg bars whenever the mod renders dun primitive markings.
   - Confirm black, brown, bay, gray, and rare chestnut/liver-chestnut founders occur.
   - Confirm tobiano, frame, splash, sabino, W-series white, leopard spotting, roan, rabicano, champagne, silver, pearl, mushroom, brindle, and magical coat types do not occur in pure founders.
   - Check that `G` produces true gray independently of the dun locus and that gray plus dun can coexist genetically.
   - Check that cream-derived founders remain uncommon and that `Cr/Cr` outcomes appear only after carrier-by-carrier matings.

4. **Disorder audit**
   - Inspect founder genomes and verify every named disorder locus is clear.
   - Confirm no listed disease allele appears in pure Highland founder generation or in pure-bred descendant lines unless introduced by an outcross, a test command, or a genetics item.
   - If an allele is introduced experimentally, verify Mendelian inheritance works normally rather than being erased by the breed label.

5. **Stat behavior**
   - Compare Highlands with baseline horses and smaller native ponies.
   - They should read as larger and more substantial than tiny ponies, but clearly smaller than an ordinary riding horse.
   - They should feel exceptionally robust, average in outright speed, and moderately capable over practical jumps.
   - They should not eclipse dedicated racing, jumping, or large draft breeds in their specialist axis.

6. **Lineage behavior**
   - Highland Pony × Highland Pony → **Highland Pony**.
   - Highland Pony × another pure breed → **Highland Pony cross**.
   - Highland Pony cross × pure Highland Pony → the same named cross, under the normal default table.
   - Highland Pony cross × a different cross → **Mixed**.
   - Highland Pony or Highland Pony cross × Feral Mixed → **Mixed**.
   - Verify labels consistently in foal screens, H-menu inspection, breed-book entries, saved NBT/entity data, and any breeder or stable UI.

## Sources

- [Highland Pony Society — SPARKS genetic-diversity and kinship guidance](https://www.highlandponysociety.com/sparks-project/): confirms the official Society’s active population-management approach, its pedigree/kinship system, the rationale for reducing inbreeding, and the 10% co-ancestry “red” threshold. [highlandponysociety](https://www.highlandponysociety.com/sparks-project/)
- [Rare Breeds Survival Trust — Highland Pony](https://www.rbst.org.uk/watchlist-breed/highland-pony/): Scottish origin, traditional work, military and pack uses, 13–14.2-hand stature, strong compact build, winter coat, and at-risk conservation context. [rbst.org](https://www.rbst.org.uk/watchlist-breed/highland-pony/)
- [The Livestock Conservancy — Highland Pony](https://livestockconservancy.org/highland-pony/): historical development, registry history, height and mass, conformation, hard feet, long mane/tail, coat colors and marking restrictions, temperament, working roles, and Balmoral association. [livestockconservancy](https://livestockconservancy.org/highland-pony/)
- [MadBarn — Highland Pony breed profile](https://madbarn.com/highland-pony-breed-profile/): Highland Pony Society as registry, solid-color palette, white-marking restrictions, conformation, feathering, temperament, and easy-keeper/metabolic management context. [madbarn](https://madbarn.com/highland-pony-breed-profile/)
- [Lochnagar Highland Ponies — History of Highland Ponies](https://www.lochnagar.co.nz/history-of-highland-ponies/): practical Highland work, deer stalking, forestry, crofting, royal association, international spread, and traditional color vocabulary. [lochnagar.co](https://www.lochnagar.co.nz/history-of-highland-ponies/)
- [UC Davis Veterinary Genetics Laboratory — Equine coat-color genetics](https://vgl.ucdavis.edu/resources/horse-coat-color): base-color genetics context for extension and agouti. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)