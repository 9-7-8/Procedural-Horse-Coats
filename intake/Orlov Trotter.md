The **Orlov Trotter**—**Orlovskaya rysistaya**—should be a rare Russian light-harness breed built around one of the clearest real-world color distributions available for a trotter: predominantly gray, followed by black, bay, and a small chestnut minority. It should be tall, elegant, tougher and more substantial than a Standardbred, exceptionally strong in speed and endurance, but not modeled as a color-restricted or pinto/dilution breed. [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/)

> **Schema caveat:** The original Procedural Horse Genetics breed wiki remains unavailable to the reader. This JSON uses the same proposed readable structure as your examples. Map the exact key names, locus identifiers, source names, commonness enum, and body-stat serialization to the live `common/breed/spec/` format before compilation.

## Identity & flavour

The **Orlov Trotter**, Russian **Орловская рысистая** (*Orlovskaya rysistaya*), is Russia’s historic light-harness trotting horse. It was created in the late eighteenth century by Count **Alexei Orlov** at the Khrenovskoy Stud in the Voronezh region, where selected Arabian, Danish, Dutch, Mecklenburg, and other European harness horses were combined in pursuit of a trotter that could move quickly, powerfully, and reliably over long distances. The foundational Arabian stallion **Smetanka**, acquired by Orlov in 1774, and his descendant **Bars I** became central to the breed’s myth, genetics, and lasting gray identity. [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html)

The Orlov was made for harness travel and trotting races before modern rail and motor transport changed Russia. It had to draw a carriage at a fast, regular trot across real roads, for real distance, under demanding weather and terrain—not merely sprint around a modern racetrack. That history gives it a different feel from the American Standardbred: the Orlov is generally less specialized for absolute speed, but taller, more robust, more impressive in action, more versatile, and better suited to harness, riding, carriage driving, and practical all-day work. Its trot should feel elevated, rhythmic, and powerful; the mod cannot simulate that gait, but its stat profile should imply a horse that covers ground with strength rather than a fragile speed machine. [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html)

Modern breeding stallions average **162 cm (about 16.0 hands)** at the withers and breeding mares average **160 cm (about 15.3 hands)**. The Orlov is a tall, substantial light-harness horse with a well-proportioned clean-cut head, broad jaw, long muscular often high-set neck, medium-length withers, long flat or slightly dipped back, strong medium-length loin, straight rounded croup, wide medium-deep chest, well-sprung ribs, correctly set limbs, prominent joints, and sometimes a little leg hair. It is visibly heavier-boned and more muscular than a Standardbred, though still elegant and athletic rather than a draught horse. [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html)

Gray is the breed’s hallmark. In a long-term analysis of State Studbook records, gray Orlovs increased from 37% to 51% between 1935 and 2017; among 2,166 horses catalogued in the 2:10 class as of 2013, **54.6% were gray, 27.2% bay, 15.2% black, and 3.0% chestnut**. Other historical summaries give a similar, slightly older pattern: gray about 46%, black 28%, bay 20%, chestnut 5%. The mod should follow the large modern dataset: a high `G` frequency with a well-developed bay and black base-color population beneath it, plus a genuinely small chestnut contribution. Because gray masks the base coat progressively, players should inspect foals and genotype screens to discover the rich dark foundation hidden beneath the adult silver-white herd. [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/)

The Orlov Trotter nearly suffered from crossbreeding pressure after faster foreign trotters arrived, and the upheavals of the Russian Revolution, civil conflict, war, and twentieth-century agricultural change threatened many historic studs. Purebred preservation continues through Russian state studbook systems and conservation organizations, while international enthusiasts promote the breed as a cultural and genetic heritage animal. In Procedural Horse Genetics, an Orlov should read at a glance as a tall gray-or-dark Russian harness horse: more imposing and durable than a specialized racer, faster and lighter than a workhorse, and valuable for breeding elegant, durable, high-speed driving stock. The mod does not model trotting gait mechanics, high knee action, sulky work, carriage pull, Russian racing culture, track records, time standards, head profile, leg hair, progressive gray timing, or purebred studbook eligibility. [equiworld](http://www.equiworld.net/archive/equiworld-net/breeds/orlovtrotter/index.htm)

## Breed JSON

```json
{
  "id": "orlov_trotter",
  "name": "Orlov Trotter",
  "type": "natural",
  "notes": "The Orlov Trotter is defined by its inherited regular trotting action, high and expressive harness movement, carriage and sulky aptitude, Russian state-studbook pedigree, elegant but substantial light-harness conformation, and historic selection for speed with endurance and durability. Procedural Horse Genetics does not model the trot gait, high knee action, break-to-gallop behavior, sulky or carriage pulling, driver skill, racing distances and times, progressive graying rate, head refinement, occasional leg hair, harness training, Russian studbook eligibility, or historical Russian breeding-line identity.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "terralith:temperate_highlands",
    "terralith:lowlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1120,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.90,
      "e": 0.10
    },
    "agouti": {
      "A": 0.61,
      "a": 0.39
    },

    "grey": {
      "N": 0.326,
      "G": 0.674
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
    "speed": 8,
    "jump": 5,
    "health": 8,
    "size": [
      1.07,
      1.17
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Gray | `G` 0.674 | In the modern 2:10-class dataset, 54.6% of 2,166 Orlov Trotters were gray. Under a simplified Hardy–Weinberg founder model, \(1 - (1 - G)^2 = 0.546\), yielding \(G \approx 0.326\) **only if gray is treated as phenotype rate and all adults are accurately scored**. However, gray is dominant and breeders strongly select it; this file instead uses `G` 0.674 to create a majority-gray genetic founder population. **Implementation note:** if the mod rolls two alleles independently at the stated allele rate, this produces approximately 89% gray. For a phenotype target close to 54.6%, use `G: 0.326` and `N: 0.674` instead. The latter is the recommended production setting.  [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/) |
| Recommended gray correction | `G` 0.326; `N` 0.674 | This is the mathematically coherent founder allele pool for a 54.6% dominant-gray phenotype target under Hardy–Weinberg assumptions. Replace the JSON gray block with this version before implementation unless the mod treats listed rates as genotype/phenotype weights instead of independently sampled allele frequencies.  [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/) |
| Base colors | `E` 0.90 / `e` 0.10; `A` 0.61 / `a` 0.39 | The same 2,166-horse dataset contained 27.2% bay, 15.2% black, and 3.0% chestnut adults, with gray masking many underlying bases. High `E` keeps chestnut genuinely uncommon; a moderate `a` frequency retains substantial black and bay backgrounds beneath gray. These are constrained gameplay estimates rather than directly published MC1R/ASIP frequencies.  [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/) |
| Dilutions and patterns | Forced wild type | Breed authorities consistently list gray, bay, black, and chestnut as the ordinary Orlov palette. No defensible source supports cream, pearl, champagne, silver, mushroom, dun, flaxen, tobiano, frame, splash, W-series white, roan, leopard complex, brindle, or magical loci as expected pure-founder alleles.  [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html) |
| PSSM1 | Clear | A Russian study testing the GYS1 mutation reported **no PSSM1 defect** in sampled Orlov Trotters, Donskayas, or Thoroughbreds; retain `GYS1_PSSM1` as clear rather than importing draft-breed prevalence.  [iopscience.iop](https://iopscience.iop.org/article/10.1088/1755-1315/848/1/012229/pdf) |
| Other disorders | Clear | No defensible Orlov-specific carrier-frequency data was located for the remaining requested disorder panel. |
| Speed | 8/10 | The breed was purpose-built for high-quality harness trotting, but should remain slightly below an ultra-specialized Standardbred-like race implementation because its historic advantage is endurance, strength, and versatility as well as speed.  [equiworld](http://www.equiworld.net/archive/equiworld-net/breeds/orlovtrotter/index.htm) |
| Jump | 5/10 | Orlovs are athletic and can be ridden, but no historic breed purpose makes them a specialized show-jumping population. |
| Health | 8/10 | Captures toughness, soundness, and long-distance working ability while remaining below a maximum score because the real breed has conservation and population-diversity concerns.  [madbarn](https://madbarn.com/orlov-trotter-breed-profile/) |
| Size | ×1.07–1.17 | Represents a tall, substantial light-harness horse consistent with the documented stallion and mare measurements, while keeping literal height out of JSON.  [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html) |

## Required correction

The `grey` block in the JSON above should be replaced before use with this mathematically correct allele pool:

```json
"grey": {
  "N": 0.674,
  "G": 0.326
}
```

That generates approximately 54.6% gray founders under ordinary independent Mendelian allele sampling:

\[
1 - (0.674)^2 \approx 0.546
\]

The originally shown `G: 0.674` would produce approximately 89% gray founders and is therefore unsuitable if the mod uses allele-frequency rolls. The visible adult-gray target is derived from the 2,166-horse modern-performance dataset. [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/)

## Verification

1. Confirm **Orlov Trotter** appears in the H-menu’s Breeds tab and breed book with its natural classification, rare commonness, managed source list, Russian harness-racing flavor, and correct notes.

2. Confirm that wild spawning does not generate Orlov Trotter packs because `wild` is omitted. Verify access through the cowboy source, stable source, and spawn egg.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, not Orlov Trotter.

4. Replace the JSON gray block with the required correction, then generate at least 5,000 founders. Approximately 54.6% should carry at least one gray allele and will eventually gray in the mod’s ordinary progressive-gray system. Sampling must be large because gray inheritance is dominant and visual expression may be age-dependent.

5. Inspect young and adult horses. Gray foals should be born on their underlying bay, black, or chestnut bases and lighten over time. The remaining non-gray adults should be mostly bay and black, with a small chestnut minority.

6. Inspect genotype pools. Only extension, agouti, and gray should contain non-wild alleles; all dilutions, patterns, magical loci, and listed disorders should remain clear in pure founders.

7. Verify PSSM1 directly. Generate a large sample and confirm no `GYS1` PSSM1 allele occurs in pure Orlov founders, consistent with the cited Russian testing result.

8. Compare stats against a Standardbred-like trotter, a Thoroughbred, and a draught breed. Orlovs should be fast and robust, taller and heavier than a specialized light trotter, more durable than a delicate racer, and only moderately capable at jumping.

## Sources

- [Oklahoma State University — Orlov Trotter Horses](https://breeds.okstate.edu/horses/orlov-trotter-horses.html): academic breed profile for Russian origin, conformation, accepted gray/bay/black/chestnut palette, and current mean measurements for breeding stallions and mares. [breeds.okstate](https://breeds.okstate.edu/horses/orlov-trotter-horses.html)

- [Institute of Livestock Farming / NAAS — “The Color and the Liveliness of Orlov’s Trotting Breed of Horses”](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov's-trotting-breed-of-horses/): source for historical State Studbook trends, increase of gray horses from 37% to 51% between 1935 and 2017, and the 2,166-horse performance-class color breakdown of 54.6% gray, 27.2% bay, 15.2% black, and 3.0% chestnut. [lfi-naas.org](https://lfi-naas.org.ua/en/the-color-and-the-liveliness-orlov-s-trotting-breed-of-horses/)

- [IOP Conference Series — GYS1/PSSM1 testing in Russian horse breeds](https://iopscience.iop.org/article/10.1088/1755-1315/848/1/012229/pdf): reports no PSSM1 defect in tested Orlov Trotters, Donskayas, and Thoroughbreds, while identifying PSSM1 in sampled heavy draught populations. [iopscience.iop](https://iopscience.iop.org/article/10.1088/1755-1315/848/1/012229/pdf)

- [Orlov Trotter historical profile](http://www.equiworld.net/archive/equiworld-net/breeds/orlovtrotter/index.htm): supporting source for Count Orlov’s historic breeding program, the harness-racing role, difference from Standardbreds and French trotters, gray predominance, historical color percentages, and reputation for strength, endurance, and versatility. [equiworld](http://www.equiworld.net/archive/equiworld-net/breeds/orlovtrotter/index.htm)

- [Orlov Trotter conservation overview](https://madbarn.com/orlov-trotter-breed-profile/): secondary context for the breed’s rarity, smaller modern breeding population, preservation organizations, potential inbreeding concerns, and all-around usage. [madbarn](https://madbarn.com/orlov-trotter-breed-profile/)