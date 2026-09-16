The **Montenegrin Domestic Hilly Horse**—**Domaći brdski konj**, often rendered in English as the **Montenegro Mountain Horse** or **Montenegrin Mountain Pony**—should be a rare, compact Balkan pack-and-mountain horse. Its pure-founder palette should center on traditional solid bay/brown, gray, black, and chestnut horses, with no defensible basis for introducing modern dilute, pinto, leopard, or magical color genes; its defining gameplay strength is exceptional heartiness and mountain utility. [en.wikipedia](https://en.wikipedia.org/wiki/List_of_horse_breeds_in_DAD-IS)

> **Schema caveat:** The provided Procedural Horse Genetics wiki could not be retrieved with the available reader. This file follows the readable schema convention from your example. Match field names, allele labels, source/commonness enums, and exact registration requirements to the live `common/breed/spec/` code before compiling.

## Identity & flavour

The **Montenegrin Domestic Hilly Horse**, properly **Domaći brdski konj** in Montenegrin, is Montenegro’s native mountain horse: a member of the wider Balkan mountain-horse family that includes related local populations in Bosnia and Herzegovina, North Macedonia, Bulgaria, Romania, and elsewhere in the region. English references may call it the **Montenegro Mountain Horse**, **Montenegrin Mountain Pony**, or simply the **Domestic Mountain Horse**. Its identity is older than a modern sport-breed registry: it coalesced across centuries of mountain pastoralism, village transport, and subsistence farming in the steep karst landscapes of Montenegro. The FAO DAD-IS breed list records *Domaći brdski konj* as Montenegro’s mountain-pony population. [en.wikipedia](https://en.wikipedia.org/wiki/List_of_horse_breeds_in_DAD-IS)

This is a horse made for places without easy roads. The Domestic Hilly Horse traditionally carried people, firewood, farm goods, and supplies across narrow, rocky mountain trails; it also worked in light draught and served as a practical riding horse. It is described as a pack animal capable of carrying roughly **100–120 kg** in a saddle, and its value is grounded in reaching terrain that is inaccessible to vehicles or to taller, less balanced horses. A Montenegrin mountain pony should feel like an all-day climbing partner: slow to tire, economical on poor forage, willing to pick a path through broken limestone, and sturdy enough to keep working through weather that would leave a refined saddle horse miserable. [theequinest](https://theequinest.com/breeds/montenegro-mountain)

Adults average approximately **13.1–13.2 hands** and are usually reported around 300–400 kg. Their body is compact and strongly made rather than miniature: a broad, deep practical frame, sound legs, solid feet, and a balance suited to steep slopes. The head tends toward the plain, alert, durable type expected in a working mountain horse rather than a refined show-horse profile. The breed is not defined by extravagant mane or feathering; its visual signature is functional density—short-coupled, tough-legged, and stable under a load. [theequinest](https://theequinest.com/breeds/montenegro-mountain)

Traditional descriptions name **dorata** as the most common color, followed by gray/white, black, and **alata**—terms generally rendered in breed summaries as bay or brown, gray, black, and chestnut/red. The conservative mod interpretation is therefore a solid-color founder population based on extension and agouti, with a meaningful gray component, but without cream, silver, champagne, pearl, dun, roan, tobiano, frame, splash, leopard spotting, or conspicuous white-pattern alleles. That produces a herd that looks like a coherent local mountain breed rather than a generalized fantasy pony collection. [theequinest](https://theequinest.com/breeds/montenegro-mountain)

Montenegrin Domestic Hilly Horses are typically described as obedient and hardy, though capable of spirit when pushed; they are selected for endurance, soundness, and self-preservation rather than top speed or show-ring scope. In Procedural Horse Genetics, players should recognize the breed instantly as a compact, dark-and-gray Balkan mountain worker whose rare status makes it worth preserving. The mod does not model trail sense, load capacity, hoof wear on limestone, forage thrift, regional tack and husbandry, exact coat terminology, individual temperament, or the real conservation and breed-management decisions that determine whether a horse qualifies as *Domaći brdski konj*. [theequinest](https://theequinest.com/breeds/montenegro-mountain)

## Breed JSON

```json
{
  "id": "montenegrin_domestic_hilly_horse",
  "name": "Montenegrin Domestic Hilly Horse",
  "type": "natural",
  "notes": "The Montenegrin Domestic Hilly Horse is defined by centuries of mountain pack work, light draught, riding, rugged karst-country sure-footedness, a compact load-carrying body, practical feet, endurance on poor forage, and the local management traditions of Montenegro. Procedural Horse Genetics does not model carrying capacity, pathfinding on steep limestone, hoof wear, true forage efficiency, regional tack, exact Balkan color terminology, working training, individual tractability, tourism use, or country-level breed-conservation and registration decisions.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:meadow",
    "minecraft:grove",
    "minecraft:stony_peaks",
    "minecraft:plains",
    "minecraft:forest",
    "terralith:alpine_highlands",
    "terralith:alpine_grove",
    "terralith:rocky_mountains",
    "terralith:highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 740,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.78,
      "e": 0.22
    },
    "agouti": {
      "A": 0.75,
      "a": 0.25
    },

    "grey": {
      "N": 0.72,
      "G": 0.28
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
    "health": 9,
    "size": [
      0.88,
      0.99
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Bay/brown foundation | `E` 0.78 / `e` 0.22; `A` 0.75 / `a` 0.25 | Traditional descriptions identify dorata—commonly rendered as bay or brown—as the most common Montenegrin mountain-horse color. A high `E` and `A` pool makes bay the normal founder result while retaining black and chestnut minorities. These are phenotype-informed gameplay estimates, not published breed-wide MC1R/ASIP data.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Gray | `G` 0.28 | Gray/white is named among the principal traditional colors. This makes gray a conspicuous minority in a founder herd rather than the population’s dominant phenotype. The figure is a transparent approximation because no published Montenegrin STX17 survey was located.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Black and chestnut | `a` 0.25; `e` 0.22 | Black/crow and chestnut/red/alata are both reported traditional solid colors. The moderate recessive allele pools preserve them as familiar but less common founder outcomes.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Dilutions and patterns | Forced wild type | No reliable source was located supporting cream, pearl, champagne, silver, mushroom, dun, flaxen, roan, tobiano, frame, splash, W-series dominant white, rabicano, leopard complex, brindle, or magical traits as components of a pure Montenegrin Domestic Hilly Horse population. |
| Disorders | All clear | No defensible population carrier rates were found for any disorder in the requested panel. It is preferable to leave these loci clear than borrow rates from a different Balkan breed or from unrelated commercial breed populations. |
| Speed | 4/10 | The breed’s role is steady pack transport, light work, and rough-country riding, not flat racing.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Jump | 5/10 | Its steep-country balance and sure-footedness justify a modestly better-than-basic terrain aptitude, without claiming a sport-jumping selection history.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Health | 9/10 | Reflects its strong constitution, endurance, ability to survive on poor forage, and historic use in difficult mountain terrain. It is a gameplay heartiness score, not a statement of disease immunity.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |
| Size | ×0.88–0.99 | Gives the mod a compact but full-working-horse silhouette consistent with the documented 13.1–13.2-hand average, while keeping direct height information out of the JSON.  [theequinest](https://theequinest.com/breeds/montenegro-mountain) |

## Code map

| Location | Montenegrin Domestic Hilly Horse implementation |
|---|---|
| `common/breed/Breed` | Add the natural `montenegrin_domestic_hilly_horse` record, display name, notes, sources, biomes, commonness, price, coat pools, disease-clear policy, and stat targets. |
| `common/breed/Breeds` | Register the ID for spawning, H-menu display, breed-book display, serialization, founder selection, and saved genome lookup. |
| `common/breed/BreedSource` | Enable the four existing source flags: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Preserve an empty epigenetic-band object; the breed’s dark/bay-and-gray identity comes from ordinary coat loci rather than forced expression bands. |
| `common/breed/spec/` | Add schema support in both loading and writing directions. Replace proposed JSON keys and allele names with the live project names as required. |
| `common/breed/Commonness` | Ensure `RARE` resolves to the intended rarity ladder and retain `spawn_weight` 1.5 as the numerical selection weight. |
| `common/breed/BreedStatCurve` | Translate speed 4, jump 5, health 9, and size ×0.88–0.99 into legal target bands. |
| `common/breed/BreedFounder` | Roll only extension, agouti, and gray from this pool; force all unnamed color loci wild type and all unnamed disorder loci clear. |
| `common/breed/BreedLineage` | No breed-specific change is required. |
| `common/genetics/SpliceOutcome` | No exception: splice-carrot alleles should be transmitted using the usual outcome logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use standard stat-axis and target-band serialization to represent compact size and durable mountain-utility scores. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` use is needed unless the actual schema requires an explicit default selection. |

## Verification

1. Verify that **Montenegrin Domestic Hilly Horse** appears in the H-menu’s Breeds tab and the breed book with its natural designation, rare commonness, source availability, mountain biome selection, price, and notes.

2. Trigger wild pack spawning in a listed mountain, meadow, windswept-hill, or grove biome. All horses created in a single breed-selected pack should show **Montenegrin Domestic Hilly Horse** as their breed label, regardless of individual gray, bay, black, or chestnut phenotype.

3. Verify that a lone unassigned wild horse is labelled **Feral Mixed**, not Montenegrin Domestic Hilly Horse.

4. Generate at least 1,000 founders and inspect visible distribution and loci. The herd should be mostly bay/brown, with visible gray, black, and chestnut minorities. Founder allele totals should trend toward `E` 0.78, `e` 0.22, `A` 0.75, `a` 0.25, and `G` 0.28.

5. Confirm that pure founders cannot generate dun, cream, pearl, champagne, silver, mushroom, flaxen, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical traits.

6. Confirm that every listed disorder locus remains clear over a large founder sample. Disorder alleles should reach descendants only through deliberate outcrossing or genetics-tool intervention.

7. Compare gameplay behavior against a racehorse, a specialist jumper, and a heavy draft breed. The Montenegrin horse should remain compact and moderately paced, with credible basic agility and exceptional heartiness, without exceeding specialists in speed, jumping, or absolute pulling mass.

## Sources

- [FAO DAD-IS breed-list reference — Montenegro: *Domaći brdski konj*](https://en.wikipedia.org/wiki/List_of_horse_breeds_in_DAD-IS): identifies *Domaći brdski konj* as Montenegro’s mountain-pony breed in the FAO Domestic Animal Diversity Information System list. Treat the live DAD-IS national breed record as the preferred production reference if accessible. [en.wikipedia](https://en.wikipedia.org/wiki/List_of_horse_breeds_in_DAD-IS)

- [FAO AGRIS — “Defining the Breed Standards and Breeding Goals for Domestic Mountain Horse”](https://agris.fao.org/search/en/providers/122550/records/68b6dbf868d9e6806700a83f): confirms that the Domestic Mountain Horse is a transboundary Balkan breed type present across multiple countries and provides regional breed-standard context. [agris.fao](https://agris.fao.org/search/en/providers/122550/records/68b6dbf868d9e6806700a83f)

- [The Equinest — Montenegro Mountain Horse](https://theequinest.com/breeds/montenegro-mountain): source for aliases, Balkan mountain-horse context, reported 13.1–13.2-hand average, 300–400 kg mass, traditional solid-color list, endurance and poor-fodder hardiness, sure-footed pack use, reported 100–120 kg carrying capacity, draft/riding/tourism roles, and temperament description. [theequinest](https://theequinest.com/breeds/montenegro-mountain)

- [FAO DAD-IS information-system overview](https://data.fao.org/catalog/dataset/domestic-animal-diversity-information-system-dad-is): official context for DAD-IS as the global system containing country-recorded livestock breed-population, characteristic, and risk-status information. [data.fao](https://data.fao.org/catalog/dataset/domestic-animal-diversity-information-system-dad-is)