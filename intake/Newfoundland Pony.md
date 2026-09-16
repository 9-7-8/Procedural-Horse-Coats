The **Newfoundland Pony** should be implemented as a rare Canadian heritage pony: compact but variable in build, exceptionally winter-hardy and sure-footed, with heavy hair, feathered fetlocks, and famously hard hooves. Its real registry accepts a broad but non-pinto palette—bay, black, brown, chestnut, dun, gray, roan, and true white—so pure founders should be colorful without ever producing tobiano, frame, splash, leopard complex, or other spotted/pinto patterns. [livestockconservancy](https://livestockconservancy.org/newfoundland/)

## Identity & flavour

The **Newfoundland Pony** is the native heritage pony of Newfoundland and Labrador, Canada. It developed from British and Irish working ponies brought to the island by settlers from the seventeenth century onward, with likely contributions from several pony and small-horse types rather than one imported foundation breed. Over generations of isolated island breeding, practical work, rough pasture, cold maritime weather, and limited feed, those horses became a distinct Newfoundland pony type. The breed is protected under Newfoundland and Labrador’s Heritage Animals Act, and the Newfoundland Pony Society is the government-designated organization responsible for preserving and protecting it. [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/)

For much of Newfoundland’s settlement history, these ponies were indispensable small farm horses. They hauled wood, nets, fish, peat, supplies, carts, and families; worked gardens and small fields; carried riders over uneven roads; and helped households survive in communities where a full-size draft horse was too expensive to keep. Mechanization, outmigration, and restrictions on open pasturing sharply reduced the population during the twentieth century, pushing the breed near extinction. The Newfoundland Pony is now a conservation breed rather than merely a regional type, recognized internationally as at-risk and listed by The Livestock Conservancy as a conservation priority. [livestockconservancy](https://livestockconservancy.org/newfoundland/)

A Newfoundland Pony stands roughly **11–14.2 hands** and weighs about **400–800 lb**. This is deliberately a variable breed: some animals are fine-boned and light, while others are broad, muscular, and stocky enough to look like miniature workhorses. Typical ponies have a plain, kind head, sturdy neck, deep body, strong short legs, low-set tail, thick mane and tail, and genuine fetlock feather extending below the fetlock points. Their dense winter coats can alter the apparent color and texture dramatically between seasons, while the hooves are famously “flint hard”—one of the breed’s clearest practical signatures. [livestockconservancy](https://livestockconservancy.org/newfoundland/)

The Newfoundland Pony Society recognizes **bay, black, brown, chestnut, dun, gray, roan, and white**. Primitive markings may accompany dun; gray should progress through life in the normal genetic sense; true white is possible and is distinguished from gray by pink skin and a white coat from birth. Modest white leg markings are allowed, but piebald and skewbald pinto patterns are explicitly not acceptable for registration, nor are Appaloosa-type leopard, blanket, or snowflake patterns. In game, this produces an attractive cold-climate pony population: dark bays, browns, black, chestnut, blue-dun or yellow-dun ponies, iron-gray and later pale-gray horses, and a few roans or true whites—never a loud Paint or Appaloosa herd. [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/)

Newfoundland Ponies are meant to be docile, easy to work with, all-around hardy, and sure-footed. They should be reliable survival mounts rather than high-performance sport ponies: small enough to conserve resources, tough enough for wet forests and bitter coastal weather, and strong enough to surprise players with their utility. In Procedural Horse Genetics, players should breed toward a furry, feathered, compact island work pony with a thick mane, dark points, modest white, and excellent heartiness. The mod does not model heavy winter coat, seasonal coat-color shifts, true feathering, hoof hardness, low tail set, exact head type, cold tolerance, hauling power, docility, working training, hereditary status, or the many centuries of local island selection that made the breed what it is. [livestockconservancy](https://livestockconservancy.org/newfoundland/)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This proposed file follows the readable JSON convention in the supplied example. Before merging, reconcile exact key names, allele IDs, source enum values, `Commonness` names, pricing units, and body-target serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The Newfoundland Pony Society documents which phenotypes are accepted, but I did not locate a population-wide genotyping study reporting allele frequencies for Extension, Agouti, dun, gray, roan, cream, or white in Newfoundland Ponies. The rates below are transparent, restrained **gameplay approximations**, selected to recreate the accepted color range while keeping bay/black/brown prominent and patterned coats absent. No requested disorder locus has a defensible Newfoundland Pony-specific carrier rate, so all are forced clear. [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/)

```json
{
  "id": "newfoundland_pony",
  "name": "Newfoundland Pony",
  "type": "natural",
  "notes": "The Newfoundland Pony is a Canadian heritage pony defined by documented Newfoundland ancestry, heavy seasonal coat, thick mane and tail, fetlock feather, flint-hard hooves, low-set tail, cold-weather hardiness, docility, sure-footedness, and long use as an island work pony. Procedural Horse Genetics does not model winter-coat density, seasonal color change, feathering, hoof quality, tail set, exact work type, hauling strength, training, historical open pasturing, registry inspection, heritage-breed status, or the degree of human management needed to preserve this critically threatened population.",

  "biomes": [
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:grove",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:stony_shore",
    "minecraft:beach",
    "minecraft:river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 760,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.67,
      "e": 0.33
    },
    "agouti": {
      "A": 0.62,
      "a": 0.38
    },

    "dun": {
      "N": 0.85,
      "D": 0.15
    },
    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "roan": {
      "N": 0.86,
      "Rn": 0.14
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
      "N": 0.94,
      "f": 0.06
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
      "N": 0.99,
      "W": 0.01
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
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
    "jump": 4,
    "health": 9,
    "size": [
      0.76,
      0.94
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Newfoundland Pony × Newfoundland Pony produces Newfoundland Pony. Newfoundland Pony × another pure breed produces a Newfoundland Pony cross. A Newfoundland Pony cross bred back to a pure Newfoundland Pony remains that cross under the default system. Different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign the Newfoundland Pony label solely from a short, shaggy, cold-biome horse: actual breed identity depends on ancestry and registry eligibility, not merely a winter coat or a dark bay phenotype."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Broad heritage-pony pool, not a one-color breed | The Newfoundland Pony Society accepts multiple solid color groups and allows variation from fine-boned to stocky individuals. It is a conservation breed with a real working type, not a clone-like show color population.  [livestockconservancy](https://livestockconservancy.org/newfoundland/) |
| Base colors | `E: 0.67 / e: 0.33`; `A: 0.62 / a: 0.38` | Generates bay, brown, black, and chestnut in substantial numbers, matching the Society’s accepted color classes. The values are gameplay approximations, not population-genetics measurements.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Dun | `D: 0.15` | Dun is explicitly accepted and the Society describes both blue/mouse dun and yellow dun, often with dorsal striping and zebra marks. It should be present but not dominant.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Gray | `G: 0.18` | Gray is an accepted and described Newfoundland Pony color. The Society correctly distinguishes progressive gray from true white.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Roan | `Rn: 0.14` | Roan is a recognized breed color, including strawberry, red, and blue roan descriptions. A moderate-low rate makes it a recurring but minority feature.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| True white | Low `KIT`-family white entry | The breed standard explicitly permits white ponies with pink skin. However, the exact causal variant(s) and Newfoundland-specific rate were not found; use the actual mod’s safest viable `KIT` white allele at a minimal rate, or omit this entry if its W locus causes an inappropriately extensive phenotype.  [livestockconservancy](https://livestockconservancy.org/newfoundland/) |
| Sabino 1 | `SB1: 0.01` | The Society permits light/white limb color but values restrained markings. This low rate is a gameplay stand-in for modest inherited white, not a claim that SB1 is documented in the breed. Remove it if the mod treats SB1 as too conspicuous.  [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/) |
| Flaxen | `f: 0.06` | The Society’s color guide explicitly discusses flaxen mane and tail on chestnuts. The rate is intentionally low and illustrative, not a measured breed frequency.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Cream, champagne, silver, pearl, mushroom | Forced wild type | They are not named among the Newfoundland Pony Society’s accepted standard color groups. Pure founders should not introduce unsupported dilute families.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Pinto patterns | Tobiano, frame, splash forced wild type | The Society explicitly excludes piebald and skewbald pinto patterns from registration. Frame is also excluded to avoid unsupported lethal-white genetics.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | The Society explicitly excludes leopard, blanket, and snowflake Appaloosa-type spotting.  [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/) |
| Rabicano | `Rb: 0.03` | A low modifier rate allows subtle white hairs without creating a pinto phenotype. This is gameplay texture, not a documented breed allele estimate. |
| Disorders | All named loci clear | No reliable Newfoundland Pony-specific carrier frequencies were found for the requested Mendelian disease panel. It is more scientifically accurate to keep the pool clear than import rates from unrelated pony, draft, Quarter Horse, Arabian, Friesian, or Warmblood populations. |
| Speed | `4/10` | The breed was selected for practical all-day work and travel, not racing. |
| Jump | `4/10` | Sure-footedness supports useful terrain movement, but there is no specialist show-jumping selection.  [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/) |
| Health | `9/10` | Represents all-around hardiness, winter suitability, sturdy working ability, and hard hooves—not immunity to disease or injury.  [livestockconservancy](https://livestockconservancy.org/newfoundland/) |
| Size | `×0.76–0.94` | Covers the broad registered 11–14.2-hand range and allows both fine small ponies and heavier, taller work-pony individuals.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-newfoundland-pony) |

## Disorder approach

All named disorder loci are intentionally **clear** in the pure Newfoundland Pony founder pool.

That decision does not imply that every Newfoundland Pony is free from all inherited conditions. It reflects the available evidence: I found no defensible Newfoundland Pony-specific carrier prevalence for ACAN dwarfism, PLOD1 Friesian dwarfism, MET/EDNRB lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

The pure population should therefore not spawn these disorders by default. A player may still introduce them through breeding with another affected/carrier lineage or through any normal mutation/splice mechanics in the mod. That preserves Mendelian behavior without attaching unrelated breed-associated disease rates to a rare heritage pony.

### White-coat implementation note

The real Society recognizes genuine white ponies with pink skin, while it also recognizes normal progressive gray and prohibits pinto patterns. This is genetically nuanced:

- **Gray** should use the regular `G` locus and lighten with age.
- **True white** should use only the actual `KIT`-family or dominant-white implementation used by the mod, if it can render a viable mostly/all-white horse with pink skin.
- **Pinto loci** such as tobiano, frame, and splash should remain absent.
- If the mod’s generic `W` allele always produces a phenotype that the registry would treat as pinto or an unsafe genotype, remove the low `W` entry and document true white as an unmodeled registry color rather than faking it with gray.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the natural `newfoundland_pony` record with Canadian heritage notes, source list, cold/wetland biome mapping, rarity, price, coat pools, disorder policy, and stat targets. |
| `common/breed/Breeds` | Register `newfoundland_pony` for wild packs, stable/cowboy systems if enabled later, menus, books, lineage labels, commands, and genome deserialization. |
| `common/breed/BreedSource` | Validate the selected `wild`, `spawn_egg`, and `stable` sources. `cowboy` is intentionally omitted because this is a rare conservation population, not an ordinary common sale-yard breed. |
| `common/breed/BreedBands` | Support the empty epigenetic-band object. Do not fake winter coat density or seasonal color change through pigment epigenetics. |
| `common/breed/spec/` | Reconcile all illustrative keys and symbols with the real parser/writer. Confirm the exact `KIT` white-spotting/dominant-white data format before enabling the low true-white entry. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder and preserve numerical `spawn_weight: 1.5` where direct weights are supported. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 4, health 9, and size ×0.76–0.94 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only the declared accepted-color pool, force pinto/leopard/unsupported dilutions and disease loci clear, and apply the compact hardy pony stat bands. |
| `common/breed/BreedLineage` | Use default pure/cross/Mixed behavior. A shaggy, feathered, cold-biome horse does not become a Newfoundland Pony without the appropriate lineage label. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. A spliced allele transmits normally and may create a nonstandard descendant outside pure-breed registration color rules. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define the compact body-size range and practical work-pony performance targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` coat-expression band is needed unless the final spec requires an explicit default value. |

## Verification

1. Confirm **Newfoundland Pony** appears in the H-menu’s Breeds tab and breed book with its heritage status, Newfoundland/Labrador identity, rare rarity tier, cold/coastal biome mapping, small size, and winter-hardy flavour text.

2. Spawn packs in taiga, grove, meadow, forest, rocky shore, beach, and river-edge environments. Every horse selected as a Newfoundland Pony pack member must display **Newfoundland Pony** as its breed label.

3. Confirm that a lone ordinary wild horse—even one that is short, dark, or standing in a cold biome—reads **Feral Mixed**, not Newfoundland Pony.

4. Generate at least 500 pure founders. The population should include:
- Bay, black, brown, and chestnut as the core colors.
- Minority dun, gray, and roan horses.
- Rare legitimate true-white horses only if the mod’s actual `KIT`-family implementation supports the correct viable pink-skinned phenotype.
- Occasional subtle flaxen-chestnut and minimal white-leg effects if those loci render conservatively.

5. Confirm that pure founders never generate tobiano, frame overo, splash white, piebald/skewbald pinto patterns, leopard, blanket, snowflake, PATN patterning, champagne, cream dilution, silver dapple, pearl, mushroom, brindle, or magical coats.

6. Confirm gray works biologically: a `G` foal should be born pigmented and lighten progressively. Do not accept a white-at-birth phenotype as evidence of gray.

7. Inspect founder disorder genomes across a large sample. Every named disorder locus must be clear. This validates the intentionally evidence-limited health model.

8. Confirm mature stat behavior: the population should be clearly pony-sized, below average in raw speed and jumping, but notably high in health/heartiness. It should feel capable for travel and survival without outperforming dedicated sport, racing, or heavy-draft breeds.

9. Test lineage:
- Newfoundland Pony × Newfoundland Pony → Newfoundland Pony.
- Newfoundland Pony × Connemara or another pure pony breed → Newfoundland Pony cross.
- Newfoundland Pony cross × pure Newfoundland Pony → the same Newfoundland Pony cross label.
- Two distinct crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice insertion. Adding tobiano, leopard complex, cream, draft size, race speed, or a known disorder allele must yield an appropriately nonstandard descendant and should never leave the animal labeled as a pure Newfoundland Pony.

## Sources

- [Newfoundland Pony Society — About the Newfoundland Pony Society](https://newfoundlandpony.com/about-the-pony-society/): government designation, heritage-breed standards, accepted colors, 11–14.2-hand size range, temperament, hardiness, feathered fetlocks, thick mane/tail, low tail set, dark limb points, and flint-hard hooves. [newfoundlandpony](https://newfoundlandpony.com/about-the-pony-society/)

- [Newfoundland Pony Society — Colors and Markings](https://newfoundlandpony.com/colors-and-markings/): detailed registry definitions for black, brown, chestnut, bay, dun, gray, roan, flaxen, pinto exclusion, Appaloosa-pattern exclusion, and palomino terminology. [newfoundlandpony](https://newfoundlandpony.com/colors-and-markings/)

- [The Livestock Conservancy — Newfoundland Pony](https://livestockconservancy.org/newfoundland/): height, weight, conservation history, Heritage Animals Act context, accepted coat groups, feathering, seasonal coat, and hoof description. [livestockconservancy](https://livestockconservancy.org/newfoundland/)

- [Fédération Equestre Internationale — Breed Profile: The Newfoundland Pony](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-newfoundland-pony): conservation timeline, 1997 preservation law, critical-status context, typical build, winter coat, height/weight, and accepted coat range. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-newfoundland-pony)

- [Government of Newfoundland and Labrador — Evolution](https://www.gov.nl.ca/fisheries/livestock/pony-evo/): provincial background on Newfoundland Pony development and historical agricultural use. [gov.nl](https://www.gov.nl.ca/fisheries/livestock/pony-evo/)

- [MadBarn — Newfoundland Pony Breed Guide](https://madbarn.com/newfoundland-pony-breed-profile/): supplementary overview of history, mechanization-driven decline, conservation, height, work roles, and non-pinto coat policy. [madbarn](https://madbarn.com/newfoundland-pony-breed-profile/)