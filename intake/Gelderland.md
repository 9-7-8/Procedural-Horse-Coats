The **Gelderlander**—also called the **Gelderland**, **Gelders Paard**, or historically the **Basispaard**—is a Dutch warmblood from Gelderland in the Netherlands. In the mod it should feel like a tall, substantial, high-action carriage-and-farm horse: most often chestnut with generous white, but also bay, black, grey, and rare pinto; powerful enough to work, elegant enough to drive, and athletic enough to ride. It is not a narrowly specialized dressage or jumping warmblood, but a traditional versatile utility horse whose hallmark is presence, active trot, willing temperament, and useful strength. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)

## Identity & flavour

The **Gelderlander**, known in Dutch as the **Gelders Paard**, comes from the province of Gelderland in the eastern Netherlands. It formed as a distinct Dutch warmblood during the late 19th century, when breeders crossed local Gelderland mares with selected European carriage, farm, and riding-horse blood. Historical influences included Dutch Friesian and East Friesian horses, plus Anglo-Norman, Oldenburg, Holsteiner, Thoroughbred, Hackney, Andalusian, and related European warmblood lines. The Geldersch Paarden Stamboek was founded in 1890, organizing breeders around two related goals: an agricultural carriage horse and a more luxurious carriage horse. [eurodressage](https://eurodressage.com/2021/03/29/gelderlander-horse-breeding-kwpn-goes-type-breeding-instead-line-breeding)

The Gelderlander was bred to earn its keep in several ways. It pulled carriages with an animated, high-stepping trot, worked agricultural land before mechanization, carried riders, and served as a dependable general-purpose horse for Dutch rural life. That balance is the breed’s core: it needed more size and pull than a light saddle horse, but more activity and refinement than a heavy draft horse. When tractors and automobiles displaced much of its traditional work during the 20th century, its carriage ability, versatility, and classic type became increasingly valuable as cultural as well as practical qualities. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)

A typical Gelderlander is a substantial, rectangular warmblood, generally about 15.2 to 17 hands. Older descriptions often place stallions and geldings around 16.2 hands, with mares a little smaller, and a typical body mass near 600 kg. It should have a long, relatively plain or straight-profiled head; expressive eyes; a strong, often arched and upright-set neck; a long sloping shoulder; pronounced withers; a deep chest; a long, strong back; rounded muscular hindquarters; clean strong legs with broad joints; and broad, durable hooves. The type is more solid and old-fashioned than a modern refined sporthorse, yet never draft-heavy. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)

Colour is part of the Gelderlander’s visual charm. **Chestnut is the traditional and most frequent colour**, often paired with broad blazes and substantial white socks. Bay, black, and grey also occur, while skewbald or pinto colouring is rare but historically present. In a mod herd, chestnut should dominate strongly; chestnuts may range from clear red to dark liver chestnut, and bold but ordinary face-and-leg markings should appear much more often than in a conservative native-pony breed. Bay and black should provide real variety, grey should be uncommon, and a pinto Gelderlander should be a memorable find rather than the default. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)

The modern Gelderlander is both a conservation breed and a living genetic contributor to Dutch sport-horse culture. In 1965 it was one of the foundation breeds of the Dutch Warmblood, alongside the heavier Groninger; later studbook consolidation placed the Gelderlander, Dutch Warmblood, and Dutch Harness Horse under the KWPN umbrella. The breed remains relatively scarce: a 2017 estimate cited about 600 breeding mares and 35 stallions. A Classic Gelder Horse studbook received Dutch recognition in 2019, supporting the preservation of the historical type rather than allowing it to disappear inside modern sport-horse breeding. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)

For a player, a Gelderlander should be immediately recognizable as the **big chestnut carriage warmblood with white socks and an elevated trot**. It should make an excellent stable-builder’s horse: markedly taller and stronger than a pony, healthier and more versatile than a single-discipline specialist, moderately quick, and notably capable in a harness-oriented world. What the mod cannot model is true trot action, carriage presence, neck carriage, formal driving training, subjective old-type conformation, exact white-marking placement, or the practical difference between an obedient farm-and-carriage horse and a modern competition horse.

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the browser tool. This is written in the same proposed format as the Australian Stock Horse example, but the field names, loci, allele identifiers, biome IDs, commonness enum, and stat-band representation must be mapped to the actual parser in `common/breed/spec/` before integration.
>
> **Health caveat:** I found no defensible Gelderlander-specific carrier-frequency study for the named disorder panel. The breed is within the broader Dutch warmblood/KWPN population, where **Warmblood Fragile Foal Syndrome** is relevant, but WFFS is not one of the requested loci and no Gelderlander-specific frequency was found. Accordingly, every listed disorder is forced clear; add WFFS only if the mod implements its locus and a registry-supported rate is available. [madbarn](https://madbarn.com/gelderlander-horse-breed-profile/)

```json
{
  "id": "gelderlander",
  "name": "Gelderlander",
  "type": "natural",
  "notes": "Gelderlanders are defined by a traditional Dutch warmblood type: a powerful but elegant rectangular frame, a strong arched upright neck, active high-stepping trot, carriage presence, broad durable hooves, practical pulling strength, and a willing all-purpose temperament. Procedural Horse Genetics does not simulate true elevated trot mechanics, knee action, neck carriage, harness training, carriage manners, agricultural pulling skill, subjective old-type warmblood conformation, exact white-marking placement, or the difference between traditional Gelderlander and modern Dutch sport-horse training.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:birch_forest",
    "minecraft:flower_forest",
    "minecraft:windswept_forest"
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
      "E": 0.41,
      "e": 0.59
    },
    "agouti": {
      "A": 0.73,
      "a": 0.27
    },

    "grey": {
      "N": 0.92,
      "G": 0.08
    },

    "flaxen": {
      "N": 0.81,
      "f": 0.19
    },
    "tobiano": {
      "N": 0.975,
      "To": 0.025
    },
    "sabino_1": {
      "N": 0.78,
      "SB1": 0.22
    },
    "splash_white_1": {
      "N": 0.94,
      "SW1": 0.06
    },
    "kit_white_spotting": {
      "N": 0.88,
      "W": 0.12
    },
    "rabicano": {
      "N": 0.94,
      "Rb": 0.06
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
    "roan": {
      "N": 1.0
    },
    "frame_overo": {
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
    "MET_lethal_at_conception": {
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
    "health": 8,
    "size": [
      1.04,
      1.17
    ]
  },

  "epigenetic_bands": {
    "chestnut_darkness": {
      "min": 0.42,
      "max": 0.90
    }
  },

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Gelderlander × Gelderlander produces Gelderlander. Gelderlander × another pure breed produces a named cross. A Gelderlander cross bred back to a pure Gelderlander remains a cross; two different crosses become Mixed; and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Suggested implementation | Rationale |
|---|---:|---|
| Base colour | `E` 0.41 / `e` 0.59; `A` 0.73 / `a` 0.27 | A high chestnut allele frequency produces the breed’s defining chestnut majority, while `E` and `A` preserve a meaningful bay population and `E` with `a` permits black. These are design frequencies for the documented phenotype distribution, not a published Gelderlander population allele survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Chestnut expression | `chestnut_darkness` 0.42–0.90 | Optional tuning only. It produces red through dark/liver chestnut without requiring the entire breed to be a single bright-orange chestnut shade. Omit if the epigenetic variable does not specifically and safely control chestnut shade. |
| Grey | `G` 0.08 | Grey is accepted and observed, but chestnut is the historically dominant phenotype; grey should be uncommon rather than a herd-defining colour.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Flaxen | `f` 0.19 | Allows a minority of chestnut horses to have lighter mane-and-tail contrast, broadening the chestnut herd visually without converting the breed into a flaxen specialty. This is a gameplay approximation, not a published prevalence. |
| White markings | Moderate `SB1`, KIT, and low splash pools | Gelderlanders are frequently chestnut with extensive blaze and leg white. These loci give the mod mechanisms for common socks and facial markings, while low splash prevents most horses from reading as loud splashes.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Tobiano | `To` 0.025 | Skewbald/pinto colouring occurs rarely. This should yield a memorable but scarce traditional outlier, not a pinto population.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Rabicano | `Rb` 0.06 | An optional low-level modifier that can add tail-head or flank roaning to some chestnuts without displacing the solid-colour identity. Use only if your project’s rabicano rendering remains subtle at this rate. |
| Cream / champagne / silver / dun | Forced clear | These are not central to the reviewed Gelderlander standard or typical colour descriptions. They may enter through crosses or mutation, but pure founders should not casually produce diluted-colour outliers.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Roan / leopard / frame | Forced clear | No credible breed-standard reason supports a founder pool for these traits; frame is additionally excluded because no evidence justifies introducing its associated lethal-white risk. |
| Magical loci | Forced clear | Gelderlanders are ordinary Dutch warmbloods. |
| Speed | 5/10 | A middle score captures active, useful carriage movement without treating the breed as a racing specialist.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Jump | 6/10 | The breed’s warmblood versatility and historical riding utility justify above-baseline athletic potential, but not the intense specialization of purpose-bred modern jumping lines.  [madbarn](https://madbarn.com/gelderlander-horse-breed-profile/) |
| Health | 8/10 | A robust score reflects practical farm-and-carriage origins, broad feet, and durable utility type, while avoiding a claim of exceptional disease immunity.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |
| Size | ×1.04–1.17 | The 15.2–17-hand documented range places the Gelderlander firmly in large-riding-horse territory, above a baseline average horse but below a true heavy draft.  [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander) |

## Disorder policy

The named disorder panel is set fully clear because the research did not identify a published **Gelderlander-specific** carrier-frequency study supporting any listed mutation. It would be inappropriate to assign alleles from Friesians merely because Friesian ancestry contributed historically to the breed, or to import generic warmblood assumptions as if they were verified Gelderlander prevalence.

The principal modern genetic concern cited for Gelderlanders and broader warmblood populations is **Warmblood Fragile Foal Syndrome (WFFS)**, a recessive connective-tissue disorder. The reviewed profile states that it occurs in many warmblood breeds and that the KWPN requires DNA testing for licensed breeding stallions to avoid carrier-to-carrier pairings. WFFS is not among the loci in the user-provided required panel, however, so it should be added only if PHG has an actual `PLOD1_WFFS` or equivalent locus implementation and the developer decides to use a properly sourced KWPN/Gelderlander allele frequency. [madbarn](https://madbarn.com/gelderlander-horse-breed-profile/)

Do not substitute **PLOD1 Friesian dwarfism** for WFFS. They are distinct disorders despite the gene-name overlap often encountered in equine genetic-health discussions. For this definition, `PLOD1_friesian_dwarfism` remains clear.

## Code map

| Location | Change or verification |
|---|---|
| `common/breed/Breed` | Add the natural `gelderlander` record with Dutch warmblood presentation metadata, sources, price, rarity, spawn settings, genes, and body-stat targets. |
| `common/breed/Breeds` | Register `gelderlander` so it appears in UI lists, data loading, spawners, breeding, saved genomes, and the breed book. |
| `common/breed/BreedSource` | Verify that `wild`, `cowboy`, `spawn_egg`, and `stable` are valid enum values for all intended acquisition paths. |
| `common/breed/BreedBands` | Encode the optional chestnut-shade band only if `chestnut_darkness` is a real project band ID and affects only pigment shade. Otherwise use an empty object. |
| `common/breed/spec/` | Map proposed JSON keys, loci, and allele names to the exact serializer/deserializer contract. |
| `common/breed/Commonness` | Confirm that `RARE` corresponds to the chosen weight of 1.5 under the project’s rarity ladder. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 6, health 8, and size ×1.04–1.17 into valid `TargetBand` records. |
| `common/breed/BreedFounder` | Ensure founders only roll the listed chestnut-biased base pool, grey, permitted white-marking loci, rare tobiano, and subtle rabicano; all omitted genes must resolve wild type or disorder-clear. |
| `common/breed/BreedLineage` | Use standard pure, named-cross, Mixed, and Feral Mixed outcomes. |
| `common/genetics/SpliceOutcome` | No special handling; confirm all permitted colour alleles pass through normal inheritance and splice-carrot logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate each stat axis, size target, and height conversion against the project’s normal riding-horse baseline. |
| `common/breed/BandType` | Use only the normal `TRADITIONAL` or `BACHELOR` handling required by the project. No special Gelderlander lineage rule is necessary. |
| `common/genetics/` WFFS locus, if present | If the codebase already implements WFFS, determine its exact locus name, phenotype behavior, and sourceable population rate before adding it. Do not alias it to Friesian dwarfism. |

## Verification

1. Open the H menu’s Breeds tab and the breed book. Confirm **Gelderlander** appears as a natural rare breed with the Dutch lowland biome list, four source methods, appropriate large-warmblood scale, chestnut-forward flavour, and traditional carriage-and-farm description.

2. Generate eligible wild packs in plains, meadows, river-adjacent terrain, birch forest, flower forest, and windswept forest. Every horse in a single generated pack must read **Gelderlander**, even where its individual colour differs.

3. Generate or find a lone ordinary wild horse outside the managed breed-spawn path. It must display **Feral Mixed**, not Gelderlander, even if it happens to be a chestnut with a blaze and four white socks.

4. Generate at least 100–200 pure founders. Expect chestnut to be visibly dominant, including ordinary red and darker liver chestnuts. Bay should be the main secondary colour; black and grey should occur occasionally; a pinto should be rare but possible.

5. Examine white markings in a large sample. Facial white and socks should be common enough to create the recognizable carriage-horse look. If the combined `SB1`, KIT, and splash settings make too many horses loud pinto patterns, lower the splash and KIT pools first rather than eliminating all white-marking inheritance.

6. Confirm that pure Gelderlander founders never generate cream, pearl, champagne, silver, mushroom, dun, roan, frame overo, leopard complex, PATN patterns, brindle, or magical loci. Such traits should require a cross, mutation, or intentional genetic intervention.

7. Inspect founders’ health panels. Every named disorder in the requested panel must read clear. If WFFS is later added to the project, test it separately as its own correctly named locus and verify that it follows the intended recessive inheritance system.

8. Confirm lineage behavior:
   - Gelderlander × Gelderlander → **Gelderlander**
   - Gelderlander × a different pure breed → named **cross**
   - Gelderlander cross × pure Gelderlander → **cross**
   - Two different cross labels → **Mixed**
   - Any lineage × **Feral Mixed** → **Mixed**

## Sources

- [Royal Dutch Sport Horse Studbook / KWPN](https://www.kwpn.org/): current umbrella registry context for Gelderlander, Dutch Warmblood, and Dutch Harness Horse breeding divisions. The search material identifies KWPN as the Gelderlander’s registry context. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)
- [Classic Gelder Horse Studbook recognition context](https://eurodressage.com/2021/03/29/gelderlander-horse-breeding-kwpn-goes-type-breeding-instead-line-breeding): detailed historical account of the 1890 Geldersch Paarden Stamboek, agricultural and luxury carriage types, mechanization-driven decline, KWPN management, and 2019 Classic Gelder Horse Studbook recognition. [eurodressage](https://eurodressage.com/2021/03/29/gelderlander-horse-breeding-kwpn-goes-type-breeding-instead-line-breeding)
- [Oklahoma State University, Breeds of Livestock — Gelderlander](https://breeds.okstate.edu/horses/gelderlander-horses): breed origin and foundational crossbred development. [breeds.okstate](https://breeds.okstate.edu/horses/gelderlander-horses)
- [Gelderlander overview](https://en.wikipedia.org/wiki/Gelderlander): concise compiled history of Dutch warmblood status, Gelderland origin, carriage/farm use, KWPN incorporation, 2017 breeding-population estimate, size, chestnut predominance, white markings, and rare skewbald colour. [en.wikipedia](https://en.wikipedia.org/wiki/Gelderlander)
- [MadBarn — Gelderlander Horse Breed Profile](https://madbarn.com/gelderlander-horse-breed-profile/): current overview of conformation, height, common colours, temperament, traditional and sport uses, and WFFS/KWPN testing context. [madbarn](https://madbarn.com/gelderlander-horse-breed-profile/)