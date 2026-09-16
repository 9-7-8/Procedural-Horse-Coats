The **Međimurje Horse**—also known as the **Murinsulaner**, **Muraközi**, **Murakoz**, **Međimurec**, and **Međimurski konj**—should be a critically endangered, tall medium-heavy draught horse from the Mura–Drava region spanning northern Croatia, southwestern Hungary, and adjacent Slovenia. It is visually restrained and genetically distinctive: modern registry-oriented modeling should make every pure founder **black-based**, with the recessive black agouti state fixed, allowing only black and rare gray horses rather than the broader bay/brown/chestnut descriptions found in older or mixed historical accounts. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the available reader. The JSON below follows the readable convention in your example. Before implementation, map the property names, locus IDs, allele symbols, source/commonness enums, and file location to the live `common/breed/spec/` schema.

## Identity & flavour

The **Međimurje Horse**, Croatian **Međimurski konj** or **Međimurec**, is an indigenous medium-heavy draught horse of Međimurje County in northern Croatia, the lowland region between the Mura and Drava rivers. Across the border in Hungary it is known as the **Muraközi** or **Muraköz horse**, while the German historical name **Murinsulaner** refers to the same Mura-island regional horse tradition. The breed emerged across the nineteenth and early twentieth centuries as local mares were crossed with heavy western-European coldblood stallions, producing a bigger, more powerful farm horse for the agricultural country of the former Habsburg borderlands. Its character is transboundary: Croatia, Hungary, Slovenia, and Austria all belong to its historical landscape, though the Croatian population is now extraordinarily small. [hrcak.srce](https://hrcak.srce.hr/file/321644)

Murinsulaners were made for the heavy practical work of a fertile but muddy river plain: ploughing, hauling, cart transport, logging, and farm draught. They were strong enough to pull hard and calm enough to live beside ordinary families, a large-bodied coldblood that could be worked daily rather than a ceremonial carriage horse. As mechanization displaced working horses in the twentieth century, the same physical qualities that once made them indispensable became rare. Their modern value lies in heritage farming, low-impact forestry, driving, conservation grazing, tourism, and the living preservation of a regional agricultural horse. [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse)

A typical Međimurje Horse stands **155–165 cm (about 15.1–16.1 hands)** at the withers; stallions can reach roughly **900 kg**. It is taller and heavier than the Croatian Posavac and resembles a serious medium-heavy draught horse rather than a pony or compact coldblood. The head is relatively small with small ears, the neck is short and strong, the withers are pronounced, the shoulders powerful, the chest deep and broad, and the legs stout enough to carry great mass over soft ground. Its mane and tail should be plain, thick working-horse hair rather than a special feather or long-mane breed hallmark; its signature is scale, strength, and a controlled, unhurried power. [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse)

The coat story requires a deliberate modern choice. Older descriptions most often call the breed bay or seal brown, followed by black, and some surviving Croatian examples clearly show dark chestnut or brown horses. However, an important recent genomic study of the **Italian Murgese**—not the Croatian Međimurje—uses the code “MUR” for *Murgese* and reports that only black and gray are accepted in that separate Italian breed. That evidence does **not** apply to Murinsulaner or Muraközi. The real Međimurje/Murinsulaner should therefore retain the historical bay/brown core, with black, chestnut, and rare gray founders possible; this is a direct correction against conflating the Croatian breed with the unrelated Murgese. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

The Međimurje is critically endangered: a 2018 breeding-systematization paper estimated an effective population size of only **14.17**, and the Croatian source population has been reported at around 40 animals, while larger related populations survived north of the Mura in Hungary and in eastern Slovenia. Genetic work on Croatian native breeds found moderate diversity and low, nonsignificant inbreeding in sampled Murinsulaner horses, but the tiny remaining core still makes preservation a serious player goal. In Procedural Horse Genetics, a Murinsulaner should feel like a rare, tall, deep-bodied, dark bay-or-brown draught survivor: slow, durable, immensely strong-looking, and worth a careful breeding program. The mod does not model pulling strength, logging skill, crop work, body mass distribution, exact feathering, handling temperament, historical regional bloodlines, or formal conservation eligibility. [hrcak.srce](https://hrcak.srce.hr/file/321644)

## Breed JSON

```json
{
  "id": "medimurje_horse",
  "name": "Međimurje Horse",
  "type": "natural",
  "notes": "The Međimurje Horse, also called Murinsulaner or Muraközi, is defined by its tall medium-heavy draught build, powerful shoulders, deep chest, strong short neck, working-horse temperament, and history in the Mura–Drava agricultural region. Procedural Horse Genetics does not model true draught traction, pulling mechanics, logging and plough skill, carriage training, body-mass distribution, exact mane density, feathering, individual tractability, regional bloodline eligibility, or the critically important real-world conservation registry and breeding-management program.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:swamp",
    "terralith:temperate_highlands",
    "terralith:wetland",
    "terralith:marsh",
    "terralith:lowlands"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1280,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.82,
      "e": 0.18
    },
    "agouti": {
      "A": 0.82,
      "a": 0.18
    },

    "grey": {
      "N": 0.95,
      "G": 0.05
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
    "speed": 3,
    "jump": 3,
    "health": 8,
    "size": [
      1.16,
      1.31
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base-color pool | `E` 0.82 / `e` 0.18; `A` 0.82 / `a` 0.18 | Bay and seal brown are repeatedly identified as the main Međimurje colors, with black and chestnut less common. High `E` and `A` produces the dominant bay/brown population; lower `a` and `e` preserve legitimate black and chestnut founders. These are transparent phenotype-derived game rates, not an MC1R/ASIP frequency claim.  [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse) |
| Gray | `G` 0.05 | Gray is reported as rare. A 5% allele pool makes gray present but exceptional and avoids obscuring the breed’s characteristic dark base colors.  [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse) |
| Other dilutions | Forced wild type | No defensible breed-specific evidence supports cream, pearl, champagne, silver, mushroom, dun, or flaxen as a regular Murinsulaner founder feature. |
| White patterns | Forced wild type | No reliable source was found to support tobiano, sabino, frame, splash, W-series dominant white, roan, rabicano, leopard complex, brindle, or other loud patterns as part of a pure breed founder pool. |
| Disorder genes | All clear | No source located supplies defensible Međimurje/Murinsulaner-specific carrier frequencies for ACAN dwarfism, PLOD1, MET, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. The very small real population makes invented disease frequencies particularly inappropriate. |
| Speed | 3/10 | A large, medium-heavy working draught horse should not compete mechanically with light riding and racing breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse) |
| Jump | 3/10 | Powerful conformation aids ordinary mobility but does not imply selection for show jumping. |
| Health | 8/10 | Represents a sturdy regional draught breed with working-horse durability, but deliberately stops below a maximum score because the current conservation population is critically small and health score is not a claim of genetic immunity.  [hrcak.srce](https://hrcak.srce.hr/file/321644) |
| Size | ×1.16–1.31 | Produces a visibly tall, heavy medium-draught horse that is larger than Posavac and comparable to a serious regional farm coldblood. Direct height stays in flavor rather than JSON.  [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse) |

## Code map

| Location | Međimurje Horse implementation |
|---|---|
| `common/breed/Breed` | Add the `medimurje_horse` natural-breed record, metadata, coat/disorder pools, stat targets, source set, biome set, price, and rarity. |
| `common/breed/Breeds` | Register `medimurje_horse` so it resolves in the H-menu, breed book, stable inventory, spawned founder data, and saved genetic records. |
| `common/breed/BreedSource` | Enable `cowboy`, `spawn_egg`, and `stable`; omit `wild` to reflect an exceptionally rare heritage breed better encountered through managed sources. |
| `common/breed/BreedBands` | Retain an empty epigenetic-band object. The breed should be recognizable through body size and its dark bay/brown genetic pool rather than enforced shade bands. |
| `common/breed/spec/` | Implement bidirectional JSON parsing/serialization for the file and translate proposed keys/alleles to active source-code naming. |
| `common/breed/Commonness` | Confirm `VERY_RARE` corresponds to the requested 0.75 weight in the current rarity ladder. |
| `common/breed/BreedStatCurve` | Convert 3/3/8 performance values and the large body-size target into legal `TargetBand` ranges. |
| `common/breed/BreedFounder` | Roll only extension, agouti, and gray alleles from the defined pool, while forcing all omitted coat loci wild type and all disorders clear. |
| `common/breed/BreedLineage` | No special breed-label rule is required. |
| `common/genetics/SpliceOutcome` | Use ordinary splice-carrot outcome logic without a breed-specific exception. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Store and apply standard target-band definitions for the medium-heavy body and low-speed/low-jump/high-heartiness profile. |
| `common/breed/BandType` | No custom use of `TRADITIONAL` or `BACHELOR` should be required unless the actual implementation mandates a band type. |

## Verification

1. Check the H-menu’s Breeds tab and breed book. **Međimurje Horse** must appear with the exact diacritic-bearing display name, its natural classification, very-rare status, managed-only source availability, price, and Mura–Drava wetland biome description.

2. Attempt a wild-herd spawn in an eligible biome. The breed should **not** appear because `wild` is deliberately absent from its source checklist. Verify it instead through cowboy stock, stables, and the spawn egg.

3. Spawn or purchase multiple pure founders. Every founder should identify as **Međimurje Horse**, while a lone naturally generated horse without a breed-source assignment should still read **Feral Mixed**.

4. Generate at least 1,000 founders. Most should be bay or seal brown, with smaller black and chestnut groups and a very small gray group. The breed must never emerge as a black-only population; that would incorrectly import the unrelated Murgese registry rule.

5. Inspect genotype results. Only extension, agouti, and gray should have non-wild alleles. Pure founders must not generate dun, cream, pearl, champagne, silver, mushroom, flaxen, tobiano, frame, splash, W-series white, roan, rabicano, leopard complex, PATN patterns, brindle, or magical coats.

6. Inspect the disorder panel after a large founder sample. All listed disorders should remain clear. Any disease allele in a later Murinsulaner-descended horse must be traceable to an outcross or direct mod-gene intervention.

7. Compare it with Croatian Posavina and Croatian Coldblood founders. The Međimurje should consistently read as the taller, heavier, rarer medium-draught heritage horse; it should be low-speed and low-jump, but highly durable without mechanically eclipsing every working breed.

## Sources

- [Čačić et al. — “Breeding Systematization of Indigenous Breed Međimurje Horse”](https://hrcak.srce.hr/file/321644): Croatian academic source identifying the breed as one of Croatia’s three indigenous horse breeds, reporting critically endangered status and an effective population size of 14.17, and discussing breed-line systematization and admixture concerns. [hrcak.srce](https://hrcak.srce.hr/file/321644)

- [Međimurje / Murinsulaner population-genetics summary](https://www.academia.edu/90459601/Breeding_systematization_of_indigenous_breed_Medjimurje_horse): reports microsatellite diversity results for Croatian native horses, including moderate genetic variability, low nonsignificant inbreeding, and genetic differentiation of the Murinsulaner population from Posavac and Croatian Coldblood samples. [academia](https://www.academia.edu/90459601/Breeding_systematization_of_indigenous_breed_Medjimurje_horse)

- [Međimurje Horse overview](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse): convenient secondary summary for the 155–165 cm range, stallion mass up to 900 kg, characteristic conformation, historical distribution, the approximate Croatian remnant population, and classic bay/seal-brown/black palette. Use Croatian registry material for definitive current breed-standard wording. [en.wikipedia](https://en.wikipedia.org/wiki/Me%C4%91imurje_horse)

- [Forgotten Horses — Međimurje / Muraközi](https://thepixelnomad.com/medimurje-murakoezi-horse/): secondary conservation context for aliases, Croatian–Hungarian geography, agricultural uses, rarity, and current preservation framing. [thepixelnomad](https://thepixelnomad.com/medimurje-murakoezi-horse/)