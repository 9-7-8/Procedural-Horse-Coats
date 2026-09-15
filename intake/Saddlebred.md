The **American Saddlebred** is a broad, color-permissive American show and riding breed rather than a visually uniform population. The implementation below therefore keeps a diverse but weighted North American saddle-horse coat pool, includes a conservative Saddlebred JEB carrier rate, and avoids pretending the mod can simulate the breed’s defining animated carriage and five-gait ability. [saddlebred](https://www.saddlebred.com/aboutthebreed)

## Identity & flavour

The **American Saddlebred**—historically the *Kentucky Saddler* and *American Saddle Horse*—is “the Horse America Made.” It developed in Kentucky and the eastern United States from colonial riding stock during the late eighteenth and nineteenth centuries, with ancestry that includes gaited pacers, Thoroughbreds, Morgans, and other American saddle horses. Its dedicated registry was organized in 1891, helping consolidate the breed recognizable today. [en.wikipedia](https://en.wikipedia.org/wiki/American_Saddlebred)

This was a horse shaped to carry a rider comfortably and stylishly across early America, then to excel in the show ring. Saddlebreds became associated with officers’ mounts during the American Civil War and evolved into a versatile performance horse for saddle seat, fine harness, pleasure driving, trail, western, hunt seat, dressage, jumping, and family riding. Their unusual cultural hallmark is the five-gaited division: in addition to walk, trot, and canter, some individuals perform the slow gait and rack—smooth, animated four-beat gaits. [saddlebred](https://www.saddlebred.com/american-saddlebred)

A Saddlebred usually stands about 15.1–16.3 hands, although the traditional average is often described as 15–16 hands and 1,000–1,200 pounds. The outline is deliberately elegant: a refined straight-profiled head with big, expressive eyes; alert ears; a long, arched neck; prominent withers; a deep, sloping shoulder; a short, strong, level back; a nearly level croup; and a proudly high-set tail. Legs should be clean and straight, with sound, open-heeled hooves. This is not a feathered or heavy-bodied horse; it should look sleek, upright, and ready to step into a spotlight. [saddlebred](https://www.saddlebred.com/aboutthebreed)

The registry accepts **any coat color**, so a Saddlebred herd should not read as a one-color breed. Chestnut/sorrel, bay, brown, and black are especially familiar, but gray, roan, palomino, buckskin, dun, champagne, silver, cremello/perlino, smoky cream, and pinto horses occur in the registered population. The player-facing identity should instead come from a tall, elegant saddle-horse silhouette and a lively, polished palette: plenty of bays and red horses, a meaningful black/brown presence, occasional dilutes, and uncommon but exciting loud-patterned horses. [saddlebred](https://www.saddlebred.com/aboutthebreed)

Saddlebreds are widely described by their association as brave, confident, people-oriented, patient, and forgiving, while their show-ring identity emphasizes brilliance, animation, balance, and cadence. In mod terms, breed them for an athletic, people-friendly American performance horse with above-average speed, fair jumping ability, good general health, and a slightly tall frame. At a glance, a player should think: *high-headed, bright-eyed, elegant, animated—and potentially any color.* The mod does not presently model the rack, slow gait, head/neck carriage, tail set and carriage, mane preparation, show shoeing, breed-ring animation, or the distinction between three- and five-gaited Saddlebreds. [saddlebred](https://www.saddlebred.com/aboutthebreed)

## Implementation choices

- **Natural spawning:** Temperate and plains-like eastern/North American biomes fit the breed’s Kentucky and eastern-U.S. roots better than deserts, taiga, or mountain extremes.
- **Commonness:** `Moderate` / weight 6 makes this a breed players can encounter without overwhelming more locally specialized populations.
- **Coat scope:** The registry’s “any color” policy justifies keeping common base colors broad and admitting established dilutions/patterns at low rates. This is a curated gameplay distribution, not a population-genetic survey. [saddlebred](https://www.saddlebred.com/aboutthebreed)
- **Disorders:** The specifically documented inherited disorder for this breed is American Saddlebred junctional epidermolysis bullosa, caused by a recessive *LAMA3* deletion. A random 2007 foal-crop sample found 9 carriers among 175 foals, reported as mutant-allele frequency 0.026; the file uses that allele frequency directly. The prompt’s listed disorder loci do not include *LAMA3*, so this requires the mod to have a corresponding JEB disorder key before it can be represented faithfully. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/19016681/)
- **Important limitation:** Do **not** substitute the Belgian/European-draft `LAMC2` JEB1 test for American Saddlebred JEB. They are different reported molecular causes in different breed contexts. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4372232/)

## Breed JSON

> **Format note:** The requested wiki page could not be retrieved by the browser tool, so I cannot verify its exact JSON property names, enum spelling, gene IDs, or the mod’s currently registered JEB key. This is a complete, implementation-oriented **proposed definition** using transparent, conventional keys. Before committing it, map `coat_genes`, `disorder_genes`, `stat_scores`, and `epigenetic_bands` to the exact schema described in `common/breed/spec/`. In particular, add or use a `LAMA3_JEB` locus rather than silently mislabeling it as another disease.

```json
{
  "id": "american_saddlebred",
  "name": "American Saddlebred",
  "type": "natural",
  "notes": "American Saddlebreds are defined as much by carriage, refinement, show presentation, and animated movement as by genotype. Procedural Horse Genetics does not model the slow gait, rack, five-gaited versus three-gaited status, upright head-and-neck carriage, naturally high tail set and carriage, fine head, large eye, show shoeing, mane roaching, tail setting, or saddle-seat training.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 520,
  "commonness": "MODERATE",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "champagne": {
      "N": 0.965,
      "Ch": 0.035
    },
    "silver": {
      "N": 0.975,
      "Z": 0.025
    },
    "dun": {
      "N": 0.975,
      "D": 0.025
    },
    "flaxen": {
      "N": 0.82,
      "f": 0.18
    },

    "grey": {
      "N": 0.94,
      "G": 0.06
    },
    "roan": {
      "N": 0.95,
      "Rn": 0.05
    },
    "tobiano": {
      "N": 0.94,
      "To": 0.06
    },
    "sabino_1": {
      "N": 0.91,
      "SB1": 0.09
    },
    "frame_overo": {
      "N": 0.985,
      "O": 0.015
    },
    "splash_white_1": {
      "N": 0.965,
      "SW1": 0.035
    },
    "kit_white_spotting": {
      "N": 0.95,
      "W": 0.05
    },
    "rabicano": {
      "N": 0.95,
      "Rb": 0.05
    },

    "pearl": {
      "N": 1.0
    },
    "mushroom": {
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
    "LAMA3_JEB": {
      "N": 0.974,
      "jeb": 0.026
    },

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
    "jump": 6,
    "health": 7,
    "size": [
      1.00,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. American Saddlebred × American Saddlebred produces American Saddlebred. American Saddlebred × another pure breed produces a cross. An American Saddlebred cross bred back to a pure American Saddlebred remains a cross; crosses between different crosses resolve to Mixed; and any lineage crossed with Feral Mixed resolves to Mixed."
  }
}
```

## Tuning notes

| Design element | Suggested value | Reason |
|---|---:|---|
| Spawn weight | 6 | Moderate availability; useful as a recognizably American performance breed without making it ubiquitous |
| Cowboy price | 520 | Above ordinary riding-horse pricing because of versatility, show-horse identity, and good base stats |
| Speed | 7/10 | Lively, animated, athletic performance horse—not a dedicated sprint racehorse |
| Jump | 6/10 | Versatile and capable, though jumping is not the central selection target |
| Health | 7/10 | General soundness and usefulness, without claiming unusual hardiness or a disease-free population |
| Size | ×1.00–1.10 | Fits the usual 15.1–16.3-hand range as a somewhat tall but not large horse |
| Epigenetic bands | None | “Deeply black” or another fixed shade would contradict the breed’s intentionally wide accepted color range |
| Strains | None | Three- and five-gaited divisions are performance/training distinctions, not a sufficiently clean genetic coat-population strain for this file |

The body-stat scores are game design interpretation rather than measured biological conversion. The size range is anchored to the association’s usual 15.1–16.3-hand description; the score choices reflect the breed’s documented athletic versatility and show-performance emphasis. [saddlebred](https://www.saddlebred.com/aboutthebreed)

## Code map

This breed should touch, or be checked against, the following components:

| Location | Required change or check |
|---|---|
| `common/breed/Breed` | Add/instantiate the `American Saddlebred` breed record and its metadata. |
| `common/breed/Breeds` | Register `american_saddlebred` so it can resolve from JSON, UI, spawning, and lineage references. |
| `common/breed/BreedSource` | Ensure the definition accepts `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Confirm an empty band map is valid; no coat or stat epigenetic band should be added by default. |
| `common/breed/spec/` | Define or validate JSON serialization/deserialization for every field used here, in both directions. |
| `common/breed/Commonness` | Confirm `MODERATE` resolves to the intended weight, here `6`; use the project’s exact enum spelling. |
| `common/breed/BreedStatCurve` | Confirm scores 7/6/7 and the size target map to valid `TargetBand` ranges. |
| `common/breed/BreedFounder` | Make founders draw only from this breed’s defined locus pools and force all omitted loci to wild type. |
| `common/breed/BreedLineage` | Confirm pure, cross, mixed, and Feral Mixed labels use the default cross table. |
| `common/genetics/SpliceOutcome` | No breed-specific change expected; verify a splice-carrot allele can still transmit into a Saddlebred foal normally. |
| `common/trait/StatAxis` | Confirm speed, jump, health, and size are the axes used by this definition. |
| `common/trait/TargetBand` | Validate the ×1.00–1.10 size interval and score-derived target bands. |
| `common/trait/BreedStatTargets` | Register/derive the Saddlebred stat target set if stat targets are enumerated rather than data-driven. |
| `common/breed/BandType` | No nondefault behavior expected; do not assign `TRADITIONAL` or `BACHELOR` unless the project requires an explicit band type. |
| Disorder registry/genome locus definitions | **Required if absent:** add the American Saddlebred *LAMA3* JEB locus and recessive inheritance behavior. Do not reuse an unrelated JEB locus merely because it has a similar phenotype. |

## Verification

1. **Data load:** Launch with the breed JSON installed. Open the H-menu’s Breeds tab and confirm that “American Saddlebred” appears with its description, natural-breed status, source availability, biome list, and intended commonness.

2. **Wild herd integrity:** Spawn or locate an eligible herd in one of the selected biomes. Verify every horse generated in the same wild pack receives the **American Saddlebred** breed label, even though their visible colors vary.

3. **Feral behavior:** Locate or generate a lone ordinary wild horse outside a recognized herd/breed-spawn context. Confirm its lineage reads **Feral Mixed**, not American Saddlebred.

4. **Coat pool:** Generate a statistically useful founder sample—at least 100–200 horses. Expect chestnut/sorrel, bay/brown, and black-based horses to dominate; gray, roan, palomino/buckskin, champagne, silver, dun, and white-patterned horses should appear less often. No leopard-complex, pearl, mushroom, brindle, or magical locus should arise from pure founders unless added through a cross, mutation, or gameplay mechanic.

5. **Disease roll:** In a large sample, inspect the American Saddlebred JEB locus. It should use the defined 0.026 mutant-allele frequency; affected homozygotes should be rare under random founder generation. Breed two known heterozygous carriers and verify ordinary recessive inheritance: approximately 25% affected, 50% carrier, and 25% clear across a sufficiently large foal sample. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/19016681/)

6. **Lineage table:** Verify:
   - American Saddlebred × American Saddlebred → American Saddlebred.
   - American Saddlebred × another pure breed → named cross.
   - American Saddlebred cross × pure American Saddlebred → same cross category.
   - American Saddlebred cross × a different cross → Mixed.
   - Any of those × Feral Mixed → Mixed.

7. **Non-genetic flavour check:** Confirm a player can identify the breed by the breed book and appearance/stat profile, but that the UI does not falsely promise simulated rack, slow gait, high tail carriage, or saddle-seat show animation.

## Sources

- [American Saddlebred Horse & Breeders Association — “The Horse America Made!”](https://www.saddlebred.com/aboutthebreed): official breed description, standard, conformation, height, colors, temperament, movement, and uses. [saddlebred](https://www.saddlebred.com/aboutthebreed)
- [American Saddlebred Horse & Breeders Association — “Meet the American Saddlebred”](https://www.saddlebred.com/american-saddlebred): official breed overview, gait divisions, and modern disciplines. [saddlebred](https://www.saddlebred.com/american-saddlebred)
- [Encyclopaedia Britannica — American Saddlebred horse](https://www.britannica.com/animal/American-Saddlebred-horse): breed development, historical registry context, ancestry, and classic description. [britannica](https://www.britannica.com/animal/American-Saddlebred-horse)
- [University of Kentucky, Animal & Food Sciences — Horse Discovery: Breeds](https://afs.mgcafe.uky.edu/equine/horse-discovery/breeds): origin, use, size, color, and characteristic summary. [afs.mgcafe.uky](https://afs.mgcafe.uky.edu/equine/horse-discovery/breeds)
- Graves, Henney & Ennis, *Animal Genetics* (2009), “Partial deletion of the LAMA3 gene is responsible for hereditary junctional epidermolysis bullosa in the American Saddlebred horse”: *LAMA3* cause and the 0.026 reported allele frequency in a 2007 foal-crop sample. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/19016681/)
- Cappelli et al., *BMC Veterinary Research* (2015): distinguishes the American Saddlebred *LAMA3* JEB mutation from the `LAMC2`-associated JEB variant reported in European draft-horse contexts. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4372232/)
