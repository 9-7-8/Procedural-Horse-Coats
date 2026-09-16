The **American Cream Draft** is best implemented as a critically rare U.S. medium-heavy draught breed centered on one unmistakable genetic target: **gold champagne on a chestnut base**. Pure founders should normally be `ee` with at least one champagne allele, producing the rich cream-to-gold body, pink mottled skin, amber or hazel eyes, and white mane and tail required by the breed’s visual standard; the breed should carry the known American Cream Draft health-testing panel of **JEB1** and **PSSM1**, though defensible breed-wide carrier frequencies are not publicly established. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

> **Schema caveat:** The mod wiki URL could not be retrieved through the available reader. This file uses the proposed readable JSON convention from your example. Before compiling, reconcile exact JSON keys, gene IDs—especially the champagne and JEB locus IDs—source enum values, and commonness naming with `common/breed/spec/`.

## Identity & flavour

The **American Cream Draft**, formally represented by the **American Cream Draft Horse Association** (ACDHA), is the only draught-horse breed developed in the United States. It began in Iowa in the early twentieth century, tracing to a cream-colored foundation mare known as **Old Granny**, foaled around 1911. Her distinctive pale offspring attracted breeders who developed a medium-heavy agricultural horse from regional draft stock—especially Belgian, Percheron, Shire, and other farm-draught blood—while preserving the unusual cream-gold color. The American Cream Horse Association formed in 1944, and the breed later endured a severe decline as mechanization replaced farm horses. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

The American Cream was made for the farm: ploughing, hauling, general draught, wagon work, and the steady, cooperative labor required of a family workhorse. It is a medium-heavy draught rather than a giant show draught—strong enough for real agricultural work but more compact and approachable than the largest Belgian or Shire types. Today it is also used for carriage driving, pleasure driving, parades, breed promotion, sustainable small-farm work, and heritage agriculture. It should feel patient, willing, calm, and powerful: not fast, not made for jumping, but remarkably satisfying when a player wants a dependable horse for a wagon, ranch, or working stable. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

American Cream mares stand **15–16 hands**, while stallions and geldings stand **16–16.3 hands**. Mature mares commonly weigh about **1,600–1,800 lb**, and stallions can weigh **1,800–2,000 lb**. The body is broad and well muscled, with a strong neck, sloping shoulder, broad chest, short coupled back, powerful hindquarters, short sturdy legs, solid hooves, and minimal feathering. The head is straight-profiled and practical rather than refined. At a glance, it should read as a broad, substantial American farm horse: heavy enough to pull, but not so massive that it becomes a cartoonish giant. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

The signature color is often misleadingly called “cream,” but genetically it is primarily **gold champagne**: champagne dilution acting on a chestnut base. The ideal adult is light, medium, or dark cream/gold with **pink skin showing dark mottling or freckles**, amber or hazel eyes, and a white mane and tail. Champagne is caused by a dominant variant in **SLC36A1**; unlike the cream-dilution locus, it can produce the characteristic skin and eye changes. The registry allows some variation—especially in mares and foundation stock—but a strict game founder pool should preserve the recognizable core: chestnut base plus champagne, no gray, no pinto, no leopard spotting, and no unrelated fantasy colors. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2535566/)

The breed’s history is one of survival. American Creams were recognized in the mid-twentieth century, nearly disappeared as tractors displaced working horses, and were rebuilt through committed breeders and the ACDHA. The Livestock Conservancy lists the breed as **Critical**, and recent accounts put the registered population below roughly 400 horses, although counts differ by year and reporting method. That makes the American Cream Draft an ideal preservation breed for the mod: a player should recognize one instantly as a big, calm, pale-gold workhorse and should be motivated to establish a careful breeding herd. The mod does not model pink mottled skin, amber-eye shade, white mane/tail texture, exact draft pull, harness skill, gait quality, registrable color nuance, ACDHA pedigree rules, or the real-world conservation work that keeps this breed alive. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

## Breed JSON

```json
{
  "id": "american_cream_draft",
  "name": "American Cream Draft",
  "type": "natural",
  "notes": "The American Cream Draft is defined by its medium-heavy American farm-draught conformation, gold-champagne coat on a chestnut base, pink mottled skin, amber or hazel eyes, white mane and tail, calm work temperament, and history as a critically rare heritage breed. Procedural Horse Genetics does not model skin mottling, eye shade, white mane/tail texture, exact gold-champagne shade, harness training, pulling force, ploughing skill, carriage manners, individual tractability, ACDHA registry color rules, pedigree verification, or modern conservation breeding decisions.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:swamp",
    "terralith:temperate_highlands",
    "terralith:lowlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1450,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "e": 1.0
    },

    "agouti": {
      "A": 1.0
    },

    "champagne": {
      "N": 0.20,
      "Ch": 0.80
    },

    "cream": {
      "N": 1.0
    },
    "pearl": {
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
    "flaxen": {
      "N": 1.0
    },
    "grey": {
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
    "JEB1": {
      "N": 0.98,
      "jeb": 0.02
    },
    "GYS1_PSSM1": {
      "N": 0.95,
      "pssm1": 0.05
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
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 3,
    "jump": 3,
    "health": 8,
    "size": [
      1.14,
      1.28
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Chestnut base | `e` 1.0 | The iconic American Cream phenotype is **gold champagne**, produced when champagne acts on chestnut. Fixing `e` makes the founder pool visually coherent and prevents amber, classic, sable, or other champagne variants from replacing the breed’s hallmark cream-gold look.  [en.wikipedia](https://en.wikipedia.org/wiki/American_Cream_Draft) |
| Agouti | `A` 1.0 | Agouti does not alter a chestnut (`ee`) phenotype. It is explicitly set only to prevent a hidden black-base pool from unexpectedly producing classic champagne after outcross-related allele changes; in this strict founder design it has no visible effect. |
| Champagne | `Ch` 0.80; `N` 0.20 | Champagne is dominant, but fixing every allele to `Ch` would make all founders `Ch/Ch` and unrealistically eliminate ordinary segregation. At `Ch` 0.80, about 96% of founders carry at least one champagne allele, while a small non-champagne chestnut fraction represents dark or appendix-eligible animals that can occur around a color-selected registry. This is a game-balance approximation, not a published ACDHA allele survey.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2535566/) |
| Cream/other dilutions | Forced wild type | The breed’s “cream” color is champagne, not necessarily the MATP cream allele. Pure founders should not generate palomino, cremello, pearl, champagne-on-black, silver, mushroom, or dun variants.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2535566/) |
| White patterns | Forced wild type | Minor ordinary markings may be accepted and even desirable, but the listed loci represent major white-pattern systems the mod would render prominently. No strong evidence supports them as a core American Cream founder pool. |
| JEB1 | `jeb` 0.02 | Junctional epidermolysis bullosa is documented in American Cream Draft horses, and the ACDHA-associated disorder panel includes JEB1. A 2% allele rate is a deliberately low, **non-survey-based gameplay proxy**: public sources confirm presence and testing but do not provide a defensible population-wide carrier frequency.  [en.wikipedia](https://en.wikipedia.org/wiki/American_Cream_Draft) |
| PSSM1 | `pssm1` 0.05 | The ACDHA-associated panel includes PSSM1, and PSSM1 is a dominant GYS1 mutation found in draft breeds. The 5% allele rate is a cautious gameplay proxy—not a measured American Cream prevalence—and should be reduced to zero if your project requires direct breed-specific prevalence data.  [animalgenetics](https://animalgenetics.com/horse-tests/equine-disorder-tests/455-panel/) |
| Speed | 3/10 | A medium-heavy farm draught horse should be slow compared with riding and racing breeds.  [breeds.okstate](https://breeds.okstate.edu/horses/american-cream-draft-horses) |
| Jump | 3/10 | Strong, sound conformation supports basic mobility but not specialist jumping. |
| Health | 8/10 | Represents a sturdy working-draught constitution while leaving room for the real conservation concern of a critically small population and the two tested disorder loci.  [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/) |
| Size | ×1.14–1.28 | Produces a substantial but not giant medium-heavy draught horse. Real height is stated only in flavor, not in the JSON.  [breeds.okstate](https://breeds.okstate.edu/horses/american-cream-draft-horses) |

## Disorder caution

The **presence** of JEB1 and PSSM1 in American Cream Draft testing is well supported: Animal Genetics offers an American Cream Draft panel containing those two tests, JEB has been documented in the breed, and PSSM1 is a known dominant draft-breed-associated condition. What is **not** well supported in public sources is a modern, breed-wide allele-frequency survey for either disorder. The JSON’s `jeb: 0.02` and `pssm1: 0.05` are transparent conservative gameplay approximations, not claims of registry statistics. [animalgenetics](https://animalgenetics.com/horse-tests/equine-disorder-tests/455-panel/)

If the mod’s breed-file policy requires only directly measured rates, use the following stricter replacement:

```json
"disorder_genes": {
  "JEB1": {
    "N": 1.0
  },
  "GYS1_PSSM1": {
    "N": 1.0
  }
}
```

That removes the conditions from randomly generated pure founders while leaving the loci available to enter the breed through future outcrossing or genetics tools.

## Code map

| Location | American Cream Draft implementation |
|---|---|
| `common/breed/Breed` | Add the natural `american_cream_draft` record with its heritage notes, medium-heavy draught targets, managed source availability, very-rare commonness, price, and constrained champagne founder pool. |
| `common/breed/Breeds` | Register the ID for the H-menu, breed book, stable and cowboy inventories, spawn-egg selection, founder generation, and saved genome resolution. |
| `common/breed/BreedSource` | Enable `cowboy`, `spawn_egg`, and `stable`; omit `wild`, since this is a critically rare, registry-managed American agricultural breed rather than a free-roaming natural herd. |
| `common/breed/BreedBands` | Keep the epigenetic-band map empty. The breed’s distinctive color should come from `ee` plus champagne inheritance, not an artificial fixed shade. |
| `common/breed/spec/` | Confirm schema support for the champagne locus and JEB1 naming, then add both deserialization and serialization for this record. |
| `common/breed/Commonness` | Confirm `VERY_RARE` maps to the 0.75 rarity weight. |
| `common/breed/BreedStatCurve` | Convert speed 3, jump 3, health 8, and the medium-heavy size range to legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll all founders chestnut-based; roll champagne at the stated rate; force every unnamed coat locus wild type; generate only JEB1 and PSSM1 as possible non-clear disorder loci if using the gameplay-proxy panel. |
| `common/breed/BreedLineage` | No American-Cream-specific lineage behavior is required. |
| `common/genetics/SpliceOutcome` | No special exception: a spliced allele should follow ordinary inheritance and splice-outcome logic. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Use normal trait bands for slow, low-jump, highly sturdy medium-heavy draught performance. |
| `common/breed/BandType` | No custom `TRADITIONAL` or `BACHELOR` choice is needed unless mandatory in the actual serializer. |

## Verification

1. Confirm **American Cream Draft** appears in the H-menu’s Breeds tab and in the breed book with its natural classification, very-rare status, managed-only sources, price, description, and farm-plains biome metadata.

2. Attempt to generate a wild pack in an eligible biome. It should not produce American Cream Draft horses because `wild` is intentionally absent. Verify the breed through a cowboy sale, stable selection, and spawn egg instead.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, not American Cream Draft.

4. Generate at least 1,000 founders. Every founder must be `ee`; approximately 96% should carry at least one `Ch` allele under the stated 0.80 champagne allele pool. Those champagne carriers should display the mod’s gold-champagne phenotype; the small non-champagne group should be ordinary chestnut or the closest available appendix-style fallback.

5. Inspect coat genes directly. Pure founders must not contain cream, pearl, dun, gray, silver, mushroom, flaxen, tobiano, sabino, frame, splash, W-series white, roan, rabicano, leopard complex, PATN, brindle, or magical alleles.

6. Inspect disorder genomes. If the gameplay-proxy configuration is retained, only `JEB1` and `GYS1_PSSM1` may be non-clear. Verify that PSSM1 expresses under the mod’s dominant-disorder logic and that JEB1 follows the model’s intended recessive inheritance and affected-foal logic. If using the strict evidence-only alternative, verify that both loci remain clear.

7. Compare generated adults against an ordinary riding horse and a giant draught breed. American Creams should be broad, tall, powerful, and slow, but they should remain a medium-heavy American workhorse rather than eclipsing Shires or Belgian-type breeds in maximum mass.

## Sources

- [American Cream Draft Horse Association](https://www.acdha.org/): official breed organization, identifying itself as the body dedicated to preserving and promoting the American Cream Draft Horse, the only draft breed originating in the United States. [acdha](https://www.acdha.org/)

- [The Livestock Conservancy — American Cream Draft Horse](https://livestockconservancy.org/american-cream-draft-horse/): conservation authority for the breed’s Critical status, 15–16.3-hand range, 1,600–2,000 lb range, characteristic cream/gold color, pink skin, amber eyes, white mane/tail, and sustainable-farming role. [livestockconservancy](https://livestockconservancy.org/american-cream-draft-horse/)

- [Oklahoma State University — American Cream Draft Horses](https://breeds.okstate.edu/horses/american-cream-draft-horses): academic breed summary for the medium-heavy draught classification, mare/stallion weights, and 15–16.3-hand height range. [breeds.okstate](https://breeds.okstate.edu/horses/american-cream-draft-horses)

- [Cook et al., *PLoS Genetics* — SLC36A1 champagne mutation](https://pmc.ncbi.nlm.nih.gov/articles/PMC2535566/): primary research establishing champagne as an autosomal-dominant SLC36A1 trait and describing the phenotype’s diluted pigment, mottled skin, and light/amber/green eye features. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2535566/)

- [OMIA — Equine champagne, SLC36A1](https://omia.org/OMIA001263/9796/): curated genetic reference identifying the causal SLC36A1 missense variant and listing American Cream Draft among breeds carrying champagne. [omia](https://omia.org/OMIA001263/9796/)

- [Animal Genetics — American Cream Draft Horse Genetic Disorder Panel](https://animalgenetics.com/horse-tests/equine-disorder-tests/455-panel/): confirms that the breed-associated genetic panel includes Junctional Epidermolysis Bullosa 1 and PSSM1. [animalgenetics](https://animalgenetics.com/horse-tests/equine-disorder-tests/455-panel/)

- [UC Davis Veterinary Genetics Laboratory — PSSM1](https://vgl.ucdavis.edu/test/pssm1): authoritative reference for the GYS1 PSSM1 mutation, muscle glycogen disorder, and its particular relevance in draft breeds. [vgl.ucdavis](https://vgl.ucdavis.edu/test/pssm1)