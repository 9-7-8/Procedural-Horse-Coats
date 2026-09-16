The **Finnhorse**—Finnish **Suomenhevonen**, literally “Finnish horse”—should be a rare Finnish national breed with a distinctly chestnut-and-flaxen core but enough conserved genetic breadth to produce bay, black, gray, cream-derived, roan, rabicano, splash-white, sabino-like, and very rare pinto offspring. Its most important design feature is not one body type: the Finnhorse is one shared breed gene pool with four performance-based breeding sections—trotting, riding, pony-sized, and working/utility—so the mod should preserve broad all-round ability rather than treat it as only a draught horse. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

> **Schema caveat:** The Procedural Horse Genetics wiki could not be retrieved through the available reader. The file below follows the readable JSON convention from your examples. Before compiling, align the exact object names, allele naming, source and commonness enums, and strain format with the current `common/breed/spec/` implementation.

## Identity & flavour

The **Finnhorse**, Finnish **Suomenhevonen**, is Finland’s only native horse breed and one of the country’s most important living cultural animals. It formed from local Finnish landrace horses and was organized into a closed studbook in **1907**, after which breeders strongly selected for the hardy, useful, chestnut-colored national horse. The breed’s roots are older than the registry: small northern horses worked Finnish farms, crossed bogs and forests, moved people and supplies, and served in military and transport roles long before the twentieth-century studbook fixed the modern name and type. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7894145/)

The Finnhorse was made to be a generalist. It hauled timber from forests, worked farms, pulled sleighs and wagons, carried cavalry riders, and later became a major harness-trotting horse. Since 1971, the studbook has separated performance evaluation into four sections: **trotter**, **riding horse**, **pony-sized horse**, and **working/utility horse**. Those sections do not form separate breeds or closed genetic populations: a registered Finnhorse remains part of one shared gene pool, and eligibility is determined by its own inspection and performance, not automatically by its parents’ section. In mod terms, this is a versatile, durable, practical horse whose breeding value can lead toward harness speed, comfortable riding, compact pony size, or useful pulling ability. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

Most adult Finnhorses stand near **155–156 cm (about 15.1 hands)**, though the pony-sized section is limited to **148 cm or less** at both withers and croup, while the other categories can extend substantially taller. The traditional Finnhorse is compact, broad, strong, and clean-limbed, with a straight or slightly convex profile, active ears, powerful shoulder and hindquarters, a deep body, strong feet, thick mane and tail, and modest feathering. It should look like a northern farm-and-harness horse rather than a polished continental warmblood: substantial enough for timber work, athletic enough for a sulky, and steady enough for a family rider. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

The breed’s great visual signature is **chestnut**, usually with a pale flaxen mane and tail and ordinary face or leg markings. More than 90% of modern Finnhorses are chestnut because twentieth-century studbook policy strongly favored chestnut and refused many “foreign” colors, including gray, palomino, piebald, and white. The old color diversity was never wholly erased: bay, black, gray, cream-derived colors, roan, rabicano, silver, splash white 1, and a distinctive minimally expressed sabino-like pattern remain in the gene pool at low frequency. A strict pure-founder population should therefore be overwhelmingly chestnut and often flaxen, but not genetically sterile—rare heritage colors should feel like exciting discoveries rather than impossible mutations. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7894145/)

The Finnhorse survived industrialization because it reinvented itself: when agricultural and forestry work declined, trotting, riding, and national heritage programs sustained it. Genetic studies show a breed with high mitochondrial variation but small nuclear effective population sizes—about 50 per breeding section in one analysis—so it is neither a generic common farm horse nor a simple closed-color novelty. In Procedural Horse Genetics, players should recognize a Finnhorse immediately as a chestnut, frequently flaxen-maned Finnish all-rounder, then discover through breeding that old colors and four different kinds of talent still lie inside the same national breed. The mod does not model the trot gait, pulling tests, forestry work, individual section qualification, exact mane thickness, white-marking extent, pace records, Finnish climate adaptation, or the real studbook’s performance and DNA-parentage rules. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

## Breed JSON

```json
{
  "id": "finnhorse",
  "name": "Finnhorse",
  "type": "natural",
  "notes": "The Finnhorse, or Suomenhevonen, is defined by a shared Finnish national studbook, a chestnut-and-flaxen visual tradition, strong compact northern conformation, thick mane and tail, ordinary white facial and leg markings, and four performance-based breeding sections for trotters, riding horses, pony-sized horses, and working and utility horses. Procedural Horse Genetics does not model trotting gait mechanics, pulling-test scores, forestry work, sleigh work, exact section qualification, horse-specific performance testing, Finnish climate adaptation, mane thickness, ordinary sock and facial-marking extent, pedigree inspection, DNA parentage procedures, or modern Finnish studbook management.",

  "biomes": [
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:snowy_taiga",
    "minecraft:grove",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:windswept_forest",
    "minecraft:plains",
    "minecraft:river",
    "terralith:alpine_grove",
    "terralith:temperate_highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1080,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.06,
      "e": 0.94
    },
    "agouti": {
      "A": 0.62,
      "a": 0.38
    },

    "flaxen": {
      "N": 0.18,
      "F": 0.82
    },

    "grey": {
      "N": 0.985,
      "G": 0.015
    },
    "cream": {
      "N": 0.985,
      "Cr": 0.015
    },
    "silver": {
      "N": 0.99,
      "Z": 0.01
    },
    "dun": {
      "N": 0.995,
      "D": 0.005
    },

    "roan": {
      "N": 0.99,
      "Rn": 0.01
    },
    "rabicano": {
      "N": 0.98,
      "Rb": 0.02
    },
    "splash_white_1": {
      "N": 0.985,
      "SW1": 0.015
    },
    "sabino_1": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 0.995,
      "W": 0.005
    },

    "pearl": {
      "N": 1.0
    },
    "champagne": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },

    "tobiano": {
      "N": 0.998,
      "To": 0.002
    },
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_2": {
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
    "speed": 7,
    "jump": 6,
    "health": 9,
    "size": [
      0.98,
      1.11
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Chestnut base | `e` 0.94 | More than 90% of Finnhorses are chestnut. A 0.94 `e` allele frequency produces about 88% `ee` chestnut founders before gray and rare dilutions mask the phenotype, closely matching the breed’s chestnut-dominant modern appearance.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Bay and black remnants | `E` 0.06; `A` 0.62 / `a` 0.38 | Historical 2007 figures cited for modern Finnhorses give about 6% bay and 1.2% black. Low `E` preserves that small black-pigment base pool; agouti is mostly hidden in chestnut horses but makes the rare dark-base founder more often bay than black.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Flaxen | `F` 0.82 | Flaxen mane and tail are extremely common on chestnut Finnhorses and form a principal visual hallmark. The locus is set high so most chestnut founders show the traditional pale mane/tail. The biological architecture of flaxen remains incompletely resolved; map this modifier to the mod’s own flaxen inheritance model rather than treating 0.82 as a literal molecular frequency.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Gray, cream, silver, and dun | Low `G`, `Cr`, `Z`, and `D` pools | Gray, palomino/cream, silver dapple, and dun are documented as rare survivors in the Finnhorse gene pool after a long period of chestnut-only selection. Low rates preserve those heritage outcomes while maintaining the overwhelmingly chestnut visual type.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Roan and rabicano | Very low `Rn` and low `Rb` pools | Roan and rabicano are documented in Finnhorse color summaries but are rare. The low pools allow unusual founders without turning a Finnish chestnut breed into a roan breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Splash and sabino-like white | `SW1` 0.015; `SB1` wild type | Research specifically examined splash white 1 and sabino in Finnhorses. The breed has a moderately common **non-SB1** minimally expressed sabino-like pattern, so it should not be modeled as literal `SB1`. `SW1` is retained at a low rate; ordinary small socks and facial marks remain outside these major pattern loci.  [wildlifegenomics.wordpress](https://wildlifegenomics.wordpress.com/2021/02/08/genetic-variation-selection-and-history-of-the-finnhorse/) |
| Rare white/pinto history | `To` 0.002; low KIT-W placeholder | Historical modern records include a single pinto/sabino-white horse, and piebalds were actively selected against. A near-zero tobiano rate preserves an extreme outlier without creating a normal pinto founder population. If the mod’s KIT W locus produces too much white at 0.005, set it to wild type and leave rare white history as unmodeled.  [en.wikipedia](https://en.wikipedia.org/wiki/Finnhorse) |
| Disorders | All clear | No defensible Finnhorse-specific carrier rates were located for the requested disorder panel. The breed has distinct population-genetic concerns, but bottleneck and effective-population data do not justify invented disease-locus frequencies.  [gsejournal.biomedcentral](https://gsejournal.biomedcentral.com/articles/10.1186/s12711-019-0480-8) |
| Speed | 7/10 | Finnhorse trotting is a major modern breeding direction; the breed is a functional harness athlete, though not as specialized as an international Standardbred.  [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/) |
| Jump | 6/10 | Riding Finnhorses are selected for suitability under saddle and all-round performance. They should be capable without outperforming specialist sport warmbloods.  [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/) |
| Health | 9/10 | Represents strong northern hardiness, forestry/farm durability, and an all-round constitution. It deliberately is not a claim of immunity to disease or a substitute for the breed’s real genetic-diversity management.  [gsejournal.biomedcentral](https://gsejournal.biomedcentral.com/articles/10.1186/s12711-019-0480-8) |
| Size | ×0.98–1.11 | Captures the ordinary full-sized Finnhorse while allowing the population’s practical size variation. The dedicated pony-size section is not forced in this file because breed sections are individual performance categories, not genetically separate closed strains.  [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/) |

## Section choice

Do **not** encode the Finnhorse’s four official breeding sections as hard genetic strains in the base file. Suomen Hippos states that all registered Finnhorses share a common gene pool, an individual can qualify for multiple sections, and foals do not automatically inherit their parents’ section labels. Hard strains would falsely imply four closed sub-breeds. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

If the mod later supports performance labels independent of breed genetics, add a non-genetic registry or achievement system for:

- **J** — harness trotter.
- **R** — riding horse.
- **P** — pony-sized horse.
- **T** — working and utility horse.

That system could evaluate adult measurements, stat outcomes, and discipline tests without falsely assigning a Mendelian “Finnhorse section” allele. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

## Verification

1. Confirm **Finnhorse** appears in the H-menu’s Breeds tab and the breed book with the natural classification, rare commonness, managed source list, Nordic forest-and-farm biome profile, price, and Finnish four-section flavor text.

2. Confirm no Finnhorse wild pack generates, because `wild` is intentionally omitted for a registry-managed national breed. Verify access through cowboy stock, stable stock, and the spawn egg.

3. Confirm a lone normally generated wild horse reads **Feral Mixed**, not Finnhorse.

4. Generate at least 2,000 founders. The population should be overwhelmingly chestnut, usually with a pale flaxen mane and tail. Bay should be uncommon, black rare, and gray, cream-derived, silver, dun, roan, rabicano, splash, or white-pattern outcomes exceptional.

5. Verify base-color behavior:
   - Most founders should be `ee`, producing chestnut.
   - High flaxen expression should affect chestnut founders but not visibly alter bay or black horses.
   - The rare `E_ A_` group should appear bay.
   - The rare `E_ aa` group should appear black.
   - Gray should progressively cover any underlying base under normal mod aging behavior.

6. Confirm that `SB1`, frame overo, splash white 2, pearl, champagne, mushroom, leopard complex, PATN1, PATN2, brindle, and all magical loci remain wild type in every pure founder.

7. Inspect founder health panels across a large sample. Every listed disorder must remain clear unless introduced through a later outcross or a genetics-tool action.

8. Compare generated Finnhorses with a Dølahest, Fjord, Standardbred-like trotter, and compact pony. Finnhorses should be all-rounders: faster than a farm-draught type, more robust than a specialist racer, larger than a Fjord, and capable under saddle without becoming an elite warmblood jumper.

## Sources

- [Suomen Hippos — Finnhorse Breeding Programme](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/): official Finnish breed-program source for the four breeding sections, their performance logic, pony-size ceiling, and section codes; crucially confirms that registered Finnhorses share a common gene pool and sections are not automatically inherited. [hippos](https://www.hippos.fi/tiedostot/jalostusohjelma-sh-eng/)

- [Kvist et al., *Ecology and Evolution* — “Selection in the Finnhorse, a Native All-Around Horse Breed”](https://pmc.ncbi.nlm.nih.gov/articles/PMC7894145/): peer-reviewed source for the 1907 studbook, twentieth-century chestnut selection, former rejection of gray/palomino/piebald/white, 1924 split, 1971 four-section system, common gene pool, and selection at coat-color, performance, locomotion, and stature-associated genes. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7894145/)

- [Kvist et al., *Genetics Selection Evolution* — “Genetic Variability and History of a Native Finnish Horse Breed”](https://gsejournal.biomedcentral.com/articles/10.1186/s12711-019-0480-8): peer-reviewed population-genetics source for local-landrace origin, strong historical selection for chestnut and minimum height, high mitochondrial diversity, small nuclear effective population size, comparatively low inbreeding, and genetic differentiation among current breeding sections. [gsejournal.biomedcentral](https://gsejournal.biomedcentral.com/articles/10.1186/s12711-019-0480-8)

- [NordGen — Finnhorse](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/finnhorse/): Nordic genetic-resource profile supporting the breed’s national status, average weight, practical height bands, chestnut-majority coat distribution, and four breeding directions. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/finnhorse/)