The **Jutland** should be a rare Danish medium draught horse whose founder pool is overwhelmingly **flaxen chestnut**: chestnut body, pale mane and tail, deep compact body, and calm pulling-horse temperament. Bay, black, gray, roan, and silver-related shades are historically reported, but they should be rare background outcomes; the breed’s unmistakable player-facing identity is the broad-chested Danish “national color” chestnut with light mane, tail, and feathering. [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse)

> **Schema caveat:** The Procedural Horse Genetics breed wiki was not retrievable through the available reader. This JSON follows the proposed readable convention from your examples. Match keys, genetic IDs, source/commonness enums, and field containers to the active `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Jutland Horse**, Danish **Jydsk Hest**, is Denmark’s national draught horse: a compact, muscular farm horse originating in the Jutland peninsula. The breed coalesced during the nineteenth century from the heterogeneous local workhorses of Jutland, then became a more organized studbook breed in the late 1800s. Its development was strongly shaped by agricultural labor, with British and continental draught influence—especially Suffolk Punch blood—and it became the everyday power source of Danish farms, towns, and transport before tractors replaced working horses. [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse)

The Jutland was made to pull. It ploughed fields, hauled wagons, carried agricultural goods, and worked in the broad, practical rhythm of a northern European farm horse. It is a medium draught rather than an immense giant: powerful, willing, and able to put steady force into harness without the extreme size of some Shire- or Belgian-type breeds. Its temperament is typically described as calm, dependable, and cooperative. In the mod, a Jutland should feel like the ideal stable-yard workhorse—slow to rush, easy to trust, broad enough to look formidable, and strong enough to make a player want a plough team or a timber-hauling yard. [discoverthehorse](https://www.discoverthehorse.com/breeds/jutland)

Jutlands stand approximately **157–164 cm (about 15.2–16.1 hands)** at the withers, with a common broader range of roughly 15–16.1 hands. They have a medium-sized expressive head with a straight profile, a short thick muscular neck, a broad deep chest, strong shoulders, a deep compact barrel, a broad powerful hindquarter, short stout legs set well apart, and soft but noticeable leg feathering. The mane and tail are usually thick and pale against the red coat. A Jutland should look dense rather than tall: a massive, low-centered body on short strong limbs, with enough feather and hair to feel like a real old-world farm draught rather than a smooth-legged sport horse. [theequinest](https://theequinest.com/breeds/jutland)

Chestnut is the modern Jutland’s “national color,” often with a distinctly flaxen, cream-to-blond mane and tail. Earlier populations included more bay and black horses, but deliberate twentieth-century selection made flaxen chestnut the dominant visual type. Gray, black, bay/brown, and roan are historically recorded, and the breed does not have a strict color requirement or a prohibition on white markings; nevertheless, the mod should keep them peripheral. The correct genetic feeling is a chestnut-dominant population with a very high flaxen modifier frequency, not a palomino breed and not a silver-dapple breed. Flaxen affects the mane and tail of chestnut horses without changing the chestnut body color, whereas cream dilution would create palomino or cremello-like results and should not define the breed. [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse)

The Jutland nearly vanished after farm mechanization. Its population fell below 300 animals in 1998, when it was classed as critically endangered; Danish genetic-resource measures, registration work, and conservation efforts helped bring the 2016 population close to 2,000 individuals, improving its classification to endangered. Its survival matters because the Jutland is genetically distinct from Denmark’s other native horse breeds, and losing it would mean losing a unique piece of Nordic agricultural history. In Procedural Horse Genetics, players should recognize the Jutland instantly: a calm, broad, chestnut draught horse with a pale mane and tail. The mod does not model exact flaxen shade, heavy mane/tail density, feathering quantity, pulling force, harness training, slow agricultural work, white-marking extent, body mass distribution, rural Danish husbandry, or real conservation-registry eligibility. [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse)

## Breed JSON

```json
{
  "id": "jutland",
  "name": "Jutland",
  "type": "natural",
  "notes": "The Jutland Horse is defined by its compact Danish farm-draught conformation, deeply muscled body, short strong legs, broad chest, powerful hindquarters, leg feathering, calm willing temperament, and characteristic flaxen chestnut coat. Procedural Horse Genetics does not model exact flaxen mane/tail shade, mane and feather density, true pulling force, harness and farm training, ploughing or wagon work, body-mass distribution, white-marking extent, rural Danish management, or the real conservation program and studbook eligibility that preserve the Jutland.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:grove",
    "minecraft:taiga",
    "terralith:temperate_highlands",
    "terralith:wetland",
    "terralith:lowlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1180,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.10,
      "e": 0.90
    },
    "agouti": {
      "A": 0.65,
      "a": 0.35
    },

    "flaxen": {
      "N": 0.15,
      "F": 0.85
    },

    "grey": {
      "N": 0.975,
      "G": 0.025
    },
    "roan": {
      "N": 0.98,
      "Rn": 0.02
    },
    "silver": {
      "N": 0.99,
      "Z": 0.01
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
    "mushroom": {
      "N": 1.0
    },
    "dun": {
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
    "health": 9,
    "size": [
      1.12,
      1.25
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Chestnut base | `e` 0.90 | Chestnut is the deliberately selected modern Jutland “national color.” A high `e` frequency makes chestnut the overwhelming founder outcome, while retaining a small bay/black background population consistent with historical colors. This is a phenotype-driven game estimate, not a published Jutland MC1R allele survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse) |
| Flaxen | `F` 0.85 | The defining Jutland image is chestnut with a light flaxen mane and tail. The flaxen modifier is set very high so that most chestnut founders visibly produce the traditional pale mane/tail presentation. The molecular basis remains incompletely resolved and may be recessive/polygenic; therefore map `F` and its dominance behavior to the mod’s actual flaxen implementation rather than treating this as a literal proven allele frequency.  [en.wikipedia](https://en.wikipedia.org/wiki/Flaxen_(color_variant)) |
| Bay and black | `E` 0.10; `A` 0.65 / `a` 0.35 | Bay/brown and black were historically more common and remain permitted but minor. The low `E` pool retains these heritage colors without allowing them to overwhelm modern flaxen chestnut founders. Agouti is mostly hidden in chestnuts but allows the rare non-chestnut result to favor bay over black.  [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse) |
| Gray, roan, silver | Low `G`, `Rn`, and `Z` pools | Gray, roan, and silver-related descriptions are historically reported but rare in current Jutland type. Keeping very low rates allows exceptional heritage-like variants without redefining the population. The `Z` entry is particularly conservative: silver is reported in secondary breed descriptions, but no Jutland-specific PMEL17 frequency was located.  [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse) |
| Cream | Forced wild type | The Jutland’s pale mane and tail are **flaxen**, not palomino. Forcing cream clear prevents a false population of palomino founders.  [en.wikipedia](https://en.wikipedia.org/wiki/Flaxen_(color_variant)) |
| Other dilutions/patterns | Forced wild type | No defensible evidence supports pearl, champagne, mushroom, dun, tobiano, sabino, frame, splash, W-series white, rabicano, leopard complex, brindle, or magical traits as core pure-founder Jutland alleles. |
| Disorders | All clear | No reliable Jutland-specific carrier-rate evidence was located for the requested disorder panel. Do not introduce PSSM1 or other disease alleles merely because the breed is a heavy draught type. |
| Speed | 3/10 | The Jutland is a farm draught horse, selected for steady work and pulling rather than speed.  [discoverthehorse](https://www.discoverthehorse.com/breeds/jutland) |
| Jump | 3/10 | Powerful but not selected for jumping specialization. |
| Health | 9/10 | Encodes a sturdy, long-lived, low-input working constitution and a conservation breed’s durability; it does not claim immunity from inherited disease.  [publication.nordgen](https://publication.nordgen.org/Equines-in-the-Nordics/appendix.html) |
| Size | ×1.12–1.25 | Produces a broad medium-heavy draught horse, clearly larger and denser than ordinary riding horses but below the scale of the largest giant draught breeds. Direct height remains in flavor text only.  [malgretoutmedia](https://www.malgretoutmedia.com/professional/the-jutland-horse-the-grand-ambassador/) |

## Verification

1. Confirm **Jutland** appears in the H-menu’s Breeds tab and breed book with its natural classification, rare status, Danish farm-and-wetland biome profile, managed source list, price, and conservation-focused notes.

2. Confirm no wild pack produces Jutlands because `wild` is intentionally omitted. Verify access via cowboy offers, stable stock, and the spawn egg.

3. Confirm a lone naturally generated wild horse reads **Feral Mixed**, not Jutland.

4. Generate at least 1,000 founders. The overwhelming majority should be chestnut; most chestnut founders should display the mod’s flaxen mane/tail phenotype. Bay and black should be rare; gray, roan, and silver-related outcomes should be exceptional.

5. Verify key visual interactions:
   - `ee` plus the mod’s flaxen expression should create the normal Jutland chestnut-with-pale-mane-and-tail phenotype.
   - The same flaxen allele should not visibly affect the rare bay or black founder, if the mod models flaxen realistically.
   - No pure founder should become palomino, cremello, perlino, buckskin, dun, champagne, mushroom, pinto, leopard-spotted, or magical.

6. Inspect loci directly. Only extension, agouti, flaxen, gray, roan, and silver should be capable of non-wild alleles; all disorder loci must remain clear.

7. Compare performance to a heavy Belgian-type draught, a Suffolk Punch, and an ordinary riding horse. Jutlands should feel slow and broad but not enormous, highly durable, and satisfying for practical work; they should not rival specialized horses in racing or jumping.

## Sources

- [NordGen — *Equines in the Nordics*, appendix: Jutland horse](https://publication.nordgen.org/Equines-in-the-Nordics/appendix.html): Nordic genetic-resource source for the Jutland’s nineteenth-century development from Jutland local stock, its national draught-horse status, 1998 critically endangered low of fewer than 300 horses, recovery effort, and 2016 population close to 2,000 with endangered status. [publication.nordgen](https://publication.nordgen.org/Equines-in-the-Nordics/appendix.html)

- [Jutland breed overview](https://en.wikipedia.org/wiki/Jutland_horse): secondary synthesis for the modern chestnut “national color,” historical bay/black prevalence, accepted chestnut/bay/black/gray/roan range, 15–16.1-hand size, 650–800 kg weight, distinct Danish genetic contribution, and historical population context. [en.wikipedia](https://en.wikipedia.org/wiki/Jutland_horse)

- [Malgretout Media — “The Jutland Horse: The Grand Ambassador”](https://www.malgretoutmedia.com/professional/the-jutland-horse-the-grand-ambassador/): supporting source for 157–164 cm height, head and body conformation, short strong limbs, light feathering, chestnut-with-light-mane majority, rare gray/black, and the lack of strict color or white-marking requirements. [malgretoutmedia](https://www.malgretoutmedia.com/professional/the-jutland-horse-the-grand-ambassador/)

- [Jutland breed profile](https://theequinest.com/breeds/jutland): secondary description for compact draught conformation, heavy feathering, short thick neck, broad chest, traditional flaxen chestnut appearance, and historic bay/black/roan alternatives. [theequinest](https://theequinest.com/breeds/jutland)

- [Holl et al., *Journal of Heredity* — chestnut color-intensity GWAS](https://pmc.ncbi.nlm.nih.gov/articles/PMC8386761/): genetic context for chestnut intensity and the associated *SALL1* region; supports treating pale mane/tail and chestnut shade as nuanced traits rather than reducing every Jutland’s traditional appearance to a single simplistic color label. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8386761/)