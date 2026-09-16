The **SA Boerperd** should be a rare, athletic South African all-purpose saddle horse: compact and muscular, heat- and terrain-hardy, fast enough for long travel, and historically tied to the Cape Horse, farming, transport, and Boer-war endurance. Its pure founder pool should be broad but **solid-colored**, centered on bay, brown, chestnut, and black, with gray, cream-derived colors, roan, rabicano, and rare dun permitted at restrained rates; pinto-like patterns and excessive white should be excluded. [saboerperd](https://saboerperd.com/)

> **Schema caveat:** The Procedural Horse Genetics wiki could not be retrieved by the available reader. This JSON follows the readable convention from your examples. Before compiling, replace proposed JSON keys, locus IDs, allele symbols, source/commonness enums, and body-target serialization with the project’s actual `common/breed/spec/` vocabulary.

## Identity & flavour

The **SA Boerperd**, also called the **South African Boerperd**, **Historiese Boerperd**, **Kaapse Boerperd**, **Cape Boerperd**, or historically the **Boer horse**, is one of South Africa’s indigenous horse breeds. Its name means roughly “farmer’s horse” in Afrikaans, and that is its whole purpose in miniature: a hardy, versatile horse able to ride, travel, work lightly, and remain useful in a country of long distances and demanding climate. The breed traces back to the Cape settlement after Jan van Riebeeck’s arrival in 1652, when Barb–Arabian-type horses imported through Java became the foundation of the Cape Horse. Later Arabian, Persian Arabian, Andalusian, Thoroughbred, Hackney, Norfolk Trotter, Cleveland Bay, Flemish, and local influences built the more substantial, fast, enduring Boerperd type. [saboerperd](https://saboerperd.com/)

The Boerperd was made for **riding, transport, farm work, distance travel, and practical cavalry use**. It was never supposed to be a delicate show horse or a heavy plough specialist: it needed enough bone and muscle to work, enough speed to cover ground, enough comfort and natural gait to ride for hours, and enough thrift to stay useful on sparse grazing. During the Anglo-Boer Wars, Boerperds became closely associated with mounted mobility; thousands were lost in war and deliberate destruction, but remote survivors formed the basis of later restoration efforts. In gameplay terms, this should be a strong, quick, practical saddle horse that feels at home crossing savanna, shrubland, dry plains, and rough hills. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

SA Boerperd mares must stand at least **13.3 hands**, and stallions at least **14.2 hands**; modern practical descriptions commonly place the breed around **14.2–15.3 hands**, with larger individuals not unusual. The ideal is a compact, muscular, balanced saddle horse, usually 400–500 kg: a straight-profiled, well-proportioned head with large expressive eyes; an arched, muscular neck; a deep chest; a short strong back; a powerful croup; and strong, correctly angled legs. It should look stronger and more compact than a Thoroughbred but more athletic and longer-legged than a small farm pony. The mane and tail are ordinary riding-horse hair, and the breed should not have the heavy feathering of a draught horse. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

The breed’s color rule is broad, but the visual boundary matters. Bay, brown, chestnut, black, gray, buckskin, palomino, smoky black, and roan are all described in SA Boerperd material; dun is reported only rarely. The coat should remain **solid**, with black pigmented skin required, while large blaze-and-stocking combinations that create a pinto-like impression are penalized or not accepted. For the mod, this means a core of bay/brown, chestnut, and black, plus low gray and cream rates. Cream must be handled carefully: the breed material warns against breeding two cream-diluted parents because double-cream offspring are barred from registration, so pure founder design should allow buckskins, palominos, and smoky blacks while keeping the allele rate modest. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

The SA Boerperd is a heritage breed shaped by survival, not an imported copy of an overseas saddle horse. Formal breed organizations emerged in the twentieth century: the Kaapse Boerperd Breeders’ Society in 1948, the Boerperd Society of South Africa in 1973, later historic-breed organization work, and official recognition of the Historiese Boerperd by South Africa’s Department of Agriculture in 1996. In Procedural Horse Genetics, players should recognize an SA Boerperd as a sturdy, solid-colored, fast-moving South African riding horse—sensible enough for a working farm, athletic enough for distance, and visually open enough that careful breeding still matters. The mod does not model the breed’s ambling/gaited tendencies, heat tolerance, dryland grazing thrift, endurance conditioning, Boer-war history, African climate adaptation, exact pigment rules, double-cream registration restrictions, registration inspection, or pedigree qualification. [saboerperd](https://saboerperd.com/)

## Breed JSON

```json
{
  "id": "sa_boerperd",
  "name": "SA Boerperd",
  "type": "natural",
  "notes": "The SA Boerperd is defined by compact South African saddle-horse conformation, efficient natural movement, practical farm and transport utility, heat and dryland hardiness, long-distance stamina, and a solid colored coat with pigmented skin. Procedural Horse Genetics does not model natural ambling or gait quality, heat tolerance, drought and sparse-grazing efficiency, exact skin pigment, extreme white-marking evaluation, double-cream registration restrictions, Boer-war service, endurance conditioning, stock work, individual ride comfort, studbook inspection, or historical Cape Horse pedigree qualification.",

  "biomes": [
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_savanna",
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:eroded_badlands",
    "minecraft:forest",
    "minecraft:windswept_hills",
    "terralith:shrubland",
    "terralith:temperate_highlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.72,
      "a": 0.28
    },

    "grey": {
      "N": 0.92,
      "G": 0.08
    },
    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "dun": {
      "N": 0.99,
      "D": 0.01
    },
    "roan": {
      "N": 0.97,
      "Rn": 0.03
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
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
    "jump": 5,
    "health": 9,
    "size": [
      0.98,
      1.09
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base colors | `E` 0.70 / `e` 0.30; `A` 0.72 / `a` 0.28 | Bay, brown, chestnut, and black are consistently described as ordinary SA Boerperd colors. The pool makes bay/brown the leading outcome while preserving a substantial chestnut group and a meaningful black minority. These are transparent phenotype-based game estimates; no public breed-wide MC1R/ASIP frequency table was located.  [hqmagazine.co](https://hqmagazine.co.za/breeds-of-hoy-south-african-boerperd/) |
| Gray | `G` 0.08 | Gray is accepted and reported but should remain a minority within a mainly solid dark-base herd. This is a visual approximation, not a published STX17 frequency.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php) |
| Cream | `Cr` 0.10 | Buckskin, palomino, and smoky black are documented. A modest cream rate allows all three while making double-cream genotypes uncommon, reflecting the breed’s concern over double-cream registration rather than pretending cream is absent.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php) |
| Dun | `D` 0.01 | Dun is specifically described as rare. A 1% allele rate lets it occur as an exceptional heritage-like founder outcome without turning the breed into a dun population.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php) |
| Roan and rabicano | `Rn` 0.03; `Rb` 0.03 | Roan and rabicano are reported among additional coat traits. Keep both uncommon so the breed reads as solid-colored in ordinary play. These are phenotype-based implementation rates, not genotype-survey figures.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/South_African_Boerperd.php) |
| Major white patterns | Forced wild type | The SA Boerperd should be solid colored with pigmented skin, and pinto-like face-and-leg white is penalized. Excluding tobiano, sabino, frame, splash, and W-series alleles is more faithful than allowing a common pinto population.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |
| Other dilutions | Forced wild type | No strong source supports pearl, champagne, silver, mushroom, or flaxen as expected pure SA Boerperd founder genes. |
| Disorders | All clear | The identified SA Boerperd diversity thesis studies performance-associated mutations and microsatellite diversity, but accessible source material does not supply defensible carrier rates for the requested inherited-disorder panel. All are left clear rather than assigning rates by ancestry.  [col-westernsem.primo.exlibrisgroup](https://col-westernsem.primo.exlibrisgroup.com/nde/fulldisplay/alma991001450411004770/01COL_WTS:WTS_2026) |
| Speed | 7/10 | The breed’s historic selection for mobility, riding, fast travel, and endurance justifies a clear speed strength, below specialist racehorses.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |
| Jump | 5/10 | Athletic and balanced enough for ordinary obstacles, but not developed primarily as a jumping breed. |
| Health | 9/10 | Encodes remarkable environmental hardiness and historical survival through hot summers, cold winters, sparse grazing, long travel, and war conditions—not immunity to disease.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |
| Size | ×0.98–1.09 | Produces a medium, compact, usable saddle horse; literal breed height remains in flavor text rather than the JSON.  [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/) |

## Verification

1. Confirm **SA Boerperd** appears in the H-menu’s Breeds tab and breed book with the natural classification, rare commonness, managed source list, South African savanna-and-shrubland biome profile, price, and breed notes.

2. Verify that it does not occur in wild packs because `wild` is omitted. Confirm availability through cowboy sales, stable selection, and spawn eggs.

3. Confirm a lone unassigned wild horse is labelled **Feral Mixed**, not SA Boerperd.

4. Generate at least 1,000 founders. The herd should center on bay/brown, chestnut, and black; gray, buckskin, palomino, smoky black, roan, and rabicano should appear occasionally; dun should be exceptional.

5. Inspect allele behavior:
   - `E_ A_` yields bay/brown.
   - `E_ aa` yields black.
   - `ee` yields chestnut.
   - `Cr` produces buckskin, palomino, or smoky black depending on the base color.
   - `G` progressively grays any base.
   - `Rn` and `Rb` remain uncommon.
   - `D` occurs only in rare founders.

6. Confirm pure founders cannot produce tobiano, sabino, frame overo, splash, W-series white, pearl, champagne, silver, mushroom, leopard complex, PATN patterns, brindle, or magical traits.

7. Verify all listed disorder loci remain clear across a large founder sample.

8. Compare the breed with an Arabian, a stock horse, and a warmblood. SA Boerperds should be sturdy, compact, notably fast and hardy, but not highly specialized in racing, jumping, or heavy pulling; their special niche is reliable distance and general-purpose work across dry country.

## Sources

- [SA Boerperd official site](https://saboerperd.com/): breed-association source for the breed’s historical roots after the 1652 Cape settlement, Java-derived Barb–Arabian foundations, Cape Horse development, Great Trek history, and modern SA Boerperd organization. [saboerperd](https://saboerperd.com/)

- [SA Boerperd Breed History](https://saboerperd.com/sa-boerperd-breed-history/): official association history for the 1973 Boerperd Society of South Africa, early breed standard development, and preservation context. [saboerperd](https://saboerperd.com/sa-boerperd-breed-history/)

- [South African Boerperd profile](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/): secondary detailed source for ancestry, minimum height rules, high-temperature and cold-winter hardiness, solid black-pigmented-skin requirement, restrictions on pinto-like markings, Boer-war history, registration development, and major uses. [inthesaddle](https://www.inthesaddle.com/breeds-from-around-the-world-part-one-boerperd/)

- [HQ Magazine — “Breeds of Hoy: South African Boerperd”](https://hqmagazine.co.za/breeds-of-hoy-south-african-boerperd/): supporting South African breed profile for 14.2–15.3-hand typical size, 400–500 kg mass, conformation, core bay/brown/chestnut/black colors, calm temperament, historic development, and Boer-war survival history. [hqmagazine.co](https://hqmagazine.co.za/breeds-of-hoy-south-african-boerperd/)

- [SA Boerperd genetic-diversity and performance-trait study record](https://col-westernsem.primo.exlibrisgroup.com/nde/fulldisplay/alma991001450411004770/01COL_WTS:WTS_2026): academic thesis record for the SA Boerperd study of performance-associated mutations, microsatellite diversity, within-breed structure, and relationships to other breeds. [col-westernsem.primo.exlibrisgroup](https://col-westernsem.primo.exlibrisgroup.com/nde/fulldisplay/alma991001450411004770/01COL_WTS:WTS_2026)