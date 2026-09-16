The **Yakutian Horse**—more precisely the **Yakut horse** or **Sakha horse** (*Sakha ata*)—should be a compact, intensely cold-adapted native breed whose founder pool is recognizably gray, dun, bay, black, and chestnut rather than a visually uniform solid-color breed. Its most defensible coat implementation uses measured Yakutian MC1R/ASIP frequencies, a high gray frequency, and a substantial dun frequency; it should carry no known breed-specific disorder locus in the mod’s listed panel. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

> **Schema caveat:** The breed wiki URL could not be retrieved by the available reader, so the JSON below follows the readable convention in your Anglo-Arabian example. Map the exact serialization key names, locus IDs, enum casing, and commonness name to the current `common/breed/spec/` implementation before compiling. In particular, this file intentionally uses genetic allele pools as probabilities for founder alleles, not phenotype percentages.

## Identity & flavour

The **Yakut horse**, also called the **Yakutian horse** and known in Sakha as **Sakha ata**, is the indigenous horse of the Sakha Republic (Yakutia) in northeastern Siberia, Russia. It is one of the world’s great northern landrace horses: a population formed and maintained by local people under open-range conditions rather than sculpted for a modern show ring. Yakut horses have lived in this region for centuries, and the breed was formally recognized as an independent Russian breed in 1987; the Russian State Register later recognized the Megezhek and Prilensk breeds and the Kolyma and Yana intrabreed types within the broader Yakut horse population. [lrgaf](http://www.lrgaf.org/yakut-study.htm)

This is a horse built for a place where winter is not a season but an ecological test. Yakut horses are kept outdoors year-round and use **tebenevka**—pawing or digging through snow for forage—to live through long subarctic winters. They served Sakha communities as transport animals and as productive herd horses for meat and mare’s milk, including the regional kumis tradition. Their job was not to be the fastest horse in a straight line, but to stay alive, stay useful, digest rough forage, and keep moving across snow, forest, swamp, and frozen ground where a specialized stable-bred horse would struggle. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

A typical indigenous Yakut is a compact, broad-bodied, deep-chested horse with a short, thick neck; low, wide withers; a straight-profiled medium head; a strong short-legged frame; and exceptionally sound, crack-resistant hoof horn. Stallions of the indigenous type are commonly about **135 cm (13.1 hands)** at the withers and mares about **132 cm (13.0 hands)**, although the recognized intrabreed types include larger, selectively improved populations. Their signature is seasonal rather than merely structural: an extraordinarily thick winter coat, heavy long mane, and long tail make a Yakut look as though it has put on a full arctic survival suit. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

Gray and dun—especially the primitive **savras** shades described in Russian breed material—are the visual center of the breed, with mousy dun, bay, black, chestnut/red, roan, piebald, and occasional leopard-patterned horses also reported across its wider population. A modern sample of 45 purebred indigenous- and Yana-type Yakut horses found a high frequency of the black-pigment-producing `E` allele at MC1R and an unusually common recessive `a` allele at ASIP, while breed researchers explicitly describe gray and dun as the prevailing camouflage colors. This makes the Yakut a wonderful mod breed for players who want a genuinely varied but coherent northern gene pool: pale grays and primitive duns against snow, interspersed with hardy bay, black, and chestnut horses rather than a parade of imported show-breed colors. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

Yakuts should feel tenacious, economical, and exceptionally hardy rather than highly specialized for racing or jumping. The sampled Yakut population had very low frequencies of the performance-associated `DMRT3` gait-keeper and `MSTN` sprint-distance variants, consistent with the breed’s historical selection for walking transport through forest and marshy terrain rather than artificial gaits or flat-racing speed. In Procedural Horse Genetics, a player should recognize one at a glance as a small, dense, winter-ready horse from a gray-and-dun-heavy population: a reliable survivor for frozen taiga exploration, not a miniature draft horse and not a modern sport pony. The mod does not model seasonal hair length, mane and tail mass, hoof-horn quality, snow-cratering behavior, cold metabolism, mosquito avoidance, mare-milk production, meat production, or the breed’s exceptional ability to subsist on winter pasture. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

## Breed JSON

```json
{
  "id": "yakut",
  "name": "Yakut",
  "type": "natural",
  "notes": "The Yakut horse is defined as much by severe northern ecology as by a static conformation: a huge winter coat, long mane and tail, dense undercoat, cold metabolism, powerful rough-forage digestion, snow-cratering behavior, strong hoof horn, and year-round herd management are central to the real breed. Procedural Horse Genetics does not model seasonal coat growth, feathering and mane volume, hoof quality, cold resistance, pasture-foraging skill, mosquito pressure, meat or milk production, or the Kolyma, Yana, Megezhek, and Prilensk management and body-type distinctions.",

  "biomes": [
    "minecraft:snowy_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:taiga",
    "minecraft:grove",
    "minecraft:snowy_plains",
    "minecraft:ice_spikes",
    "minecraft:windswept_hills",
    "terralith:alpine_highlands",
    "terralith:alpine_grove",
    "terralith:siberian_grove"
  ],
  "spawn_weight": 6,
  "spawn_time": "any",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 520,
  "commonness": "COMMON",

  "coat_genes": {
    "extension": {
      "E": 0.711,
      "e": 0.289
    },
    "agouti": {
      "A": 0.400,
      "a": 0.600
    },

    "grey": {
      "N": 0.440,
      "G": 0.560
    },
    "dun": {
      "N": 0.400,
      "D": 0.600
    },

    "roan": {
      "N": 0.940,
      "Rn": 0.060
    },
    "tobiano": {
      "N": 0.970,
      "To": 0.030
    },
    "leopard_complex": {
      "N": 0.990,
      "LP": 0.010
    },
    "patn1": {
      "N": 1.0
    },
    "patn2": {
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
    "health": 10,
    "size": [
      0.86,
      0.96
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Basis and intended result |
|---|---:|---|
| Extension | `E` 0.711; `e` 0.289 | A study of 45 purebred Yakutian horses found the dominant black-pigment-producing `E` allele at 0.711. This preserves bay, black, dun, gray, and chestnut possibilities while avoiding an implausibly chestnut-dominant population.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Agouti | `A` 0.400; `a` 0.600 | The same study found `A` at 0.400; equivalently, `a` occurred at 0.600. The high `a` frequency is notable for the breed and allows a meaningful black/mouse-dun component beneath gray and dun.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Gray | `G` 0.560 | Gray is repeatedly described as a dominant and characteristic Yakut coat color, particularly in the northern Kolyma and Yana populations. `G` 0.56 is a transparent gameplay-oriented population estimate, not a claim that the 45-horse MC1R/ASIP study measured STX17 directly.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Dun | `D` 0.600 | The research paper identifies gray and dun as dominant Yakutian camouflage colors. A high dun rate makes primitive bay dun, red dun, and mouse dun a clear visual hallmark. This is a phenotype-informed game approximation rather than a published TBX3 allele survey.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Roan and tobiano | Low rates: `Rn` 0.06; `To` 0.03 | Roan and piebald horses are reported in the indigenous population, but neither is described as predominant. Low allele rates retain those historically documented exceptions without turning every Yakut herd into a pinto or roan breed.  [lrgaf](http://www.lrgaf.org/yakut-study.htm) |
| Leopard complex | `LP` 0.01; no PATN alleles | “Chubara,” a traditional term often used for leopard/spotted coats, is reported as uncommon. A very low LP rate permits rare spotted founders; no PATN pool prevents a modern Appaloosa-like blanket-spotted population.  [lrgaf](http://www.lrgaf.org/yakut-study.htm) |
| Other colors | Forced wild type | No defensible evidence was found for cream, pearl, champagne, silver, mushroom, frame, splash, W-series dominant white, rabicano, brindle, or magical genes as part of a Yakut founder pool. Omission should force them wild type. |
| Disorders | All clear | The sources located provide coat-color, locomotion, body-type, and adaptive information, but no defensible Yakut-specific carrier frequencies for the listed disorder panel. It is better to set these clear than import unrelated disease rates from Arabian, Quarter Horse, Warmblood, or Friesian populations. |
| Speed | 4/10 | Yakuts are transport and utility horses selected for reliable movement in forest, swamp, snow, and pasture conditions. The sampled population’s low `MSTN` racing-distance mutation frequency also argues against a race-oriented score.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Jump | 4/10 | The breed is sturdy and sure-footed but not historically selected as a jumping specialist. |
| Health | 10/10 | This represents extreme ecological hardiness, year-round outdoor survival, rough-forage efficiency, and cold adaptation—not immunity to every disease and not a substitute for the disorder loci.  [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html) |
| Size | ×0.86–0.96 | Keeps founders distinctly compact without encoding a hand-height statement in JSON. The real breed has indigenous and enlarged intrabreed types, so the range deliberately represents the native compact core rather than every registry-recognized type.  [lrgaf](http://www.lrgaf.org/yakut-study.htm) |

## Code map

| Location | Yakut implementation |
|---|---|
| `common/breed/Breed` | Add the natural `yakut` record, display name, notes, biome/source metadata, price, commonness, gene pools, and stat targets. |
| `common/breed/Breeds` | Register `yakut` so it resolves for spawning, books, menus, serialization, and breed lookup. |
| `common/breed/BreedSource` | Use `wild`, `cowboy`, `spawn_egg`, and `stable`; no new source category is required. |
| `common/breed/BreedBands` | Leave the breed’s epigenetic bands empty. The distinctive Yakut appearance is represented by loci and body stats, not a fixed shade band. |
| `common/breed/spec/` | Add read/write support for the Yakut JSON file and adjust field names or locus identifiers to the real current schema if they differ from this proposed convention. |
| `common/breed/Commonness` | Confirm that `COMMON` maps to the desired spawn-weight ladder. The supplied numerical weight is 6, the requested moderate baseline. |
| `common/breed/BreedStatCurve` | Map speed 4, jump 4, health 10, and size ×0.86–0.96 to valid target bands. |
| `common/breed/BreedFounder` | Roll only the named loci from the provided pools, force unnamed coat loci wild type and unnamed disorders clear, and use the normal founder mechanism. |
| `common/breed/BreedLineage` | No Yakut-specific behavior is required; use the mod’s ordinary breed-label rules. |
| `common/genetics/SpliceOutcome` | No exception is needed. A splice-carrot allele reaches a Yakut foal only through the normal splice outcome and inheritance logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the compact size band and the four Yakut target scores through the ordinary stat-band system. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` handling is needed unless the actual schema requires a default band type for every natural breed. |

## Verification

1. Confirm **Yakut** appears in the H-menu’s Breeds tab and in the breed book with its correct natural classification, snow-and-taiga biome list, four enabled sources, commonness, price, and notes.

2. Force a wild-pack spawn in each enabled biome. Every horse generated as part of one selected Yakut pack should display **Yakut** as its breed label, including gray, dun, mouse-dun, bay, black, and chestnut-based individuals.

3. Spawn or locate a lone unassigned wild horse outside the pack-generation path. Its breed label should read **Feral Mixed**, not Yakut.

4. Roll at least 500 Yakut founders and inspect loci rather than judging only visible coats. The aggregate founder pool should approach `E` 0.711 / `e` 0.289 and `A` 0.400 / `a` 0.600; gray and dun should be frequent enough that snowy-biome herds visibly feel Yakutian.

5. Confirm that pure Yakut founders cannot naturally roll cream, pearl, champagne, silver, mushroom, flaxen, frame, splash, W-series white, rabicano, brindle, PATN1, PATN2, or magical coat alleles.

6. Confirm that low-frequency roan, tobiano, and leopard-complex founders can occur, but are genuinely uncommon. A rare `LP` founder should not produce blanket or fewspot patterns without an introduced PATN allele.

7. Inspect the disorder genome for a substantial founder sample. Every locus in the listed disorder panel should be clear in pure Yakut founders.

8. Compare generated body stats with a baseline breed. Yakuts should trend compact, slower than dedicated racing breeds, not optimized for jumping, and unusually hearty. Their survival niche should be apparent in exploration and snow-biome play without making them mechanically superior at every task.

## Sources

- [Agricultural Biology — “Genetic Structure of the Local Yakutian Horse Population for Genes MC1R, ASIP, DMRT3, and MSTN”](http://www.agrobiology.ru/2-2022kalinkova-eng.html): peer-reviewed Yakutian population study of 45 purebred horses; supplies the measured MC1R `E` frequency of 0.711, ASIP `A` frequency of 0.400, the common `E/E-A/a` and `E/E-a/a` genotypes, and the low DMRT3 and MSTN variant frequencies. It also describes the breed’s compact form, exceptionally thick winter coat, long mane and tail, gray/dun prevalence, and transport/milk/meat roles. [agrobiology](http://www.agrobiology.ru/2-2022kalinkova-eng.html)

- [Long Riders’ Guild Academic Foundation — “A Study of the Yakut Breed of Horses”](http://www.lrgaf.org/yakut-study.htm): detailed English-language synthesis of Yakut breed history, formal 1987 independent-breed recognition, Russian protected-breeding registrations, indigenous/Kolyma/Yana/Megezhek/Prilenskaya types, conformation, color descriptions, year-round range management, hoof quality, winter grazing, and breed use. [lrgaf](http://www.lrgaf.org/yakut-study.htm)

- [FAO Domestic Animal Diversity Information System (DAD-IS)](https://www.fao.org/dad-is/en): the FAO global breed-information system for country-level breed profiles, diversity, use, and conservation context. It is appropriate for confirming country-level breed-record context when integrating a final production file against the current DAD-IS entry. [fao](https://www.fao.org/dad-is/en)