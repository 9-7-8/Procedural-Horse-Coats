The **Konik** is best implemented as the tightly conserved Polish breed **Konik polski**—a compact, exceptionally hardy blue-dun pony whose official pure-bred standard is deliberately much narrower than the coat variation seen in its historical foundation stock. Pure founders should overwhelmingly, and ideally exclusively, produce black-based dun horses: mouse-colored or blue-dun/grullo, with primitive markings, dark points, no conspicuous white, and strong survival-oriented body stats. The Polish Horse Breeders Association permits only mouse-colored horses with a dorsal stripe and no white markings in pure-bred Konik breeding, with only tiny temporary facial markings permitted in mares. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

## Identity & flavour

The **Konik polski**—commonly called the **Polish Konik**, **Konik**, or Polish primitive horse—is Poland’s small native horse breed. *Konik* means “little horse” in Polish. The modern breed emerged from early-20th-century work with local primitive horses from the Biłgoraj region, particularly the small working horses then called Panje horses; the name “Konik” was adopted in the 1920s, and organized breeding expanded through state studs and conservation programs. The Polish Horse Breeders Association maintains the official Stud-book of Origin for the breed. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

Koniks were never shaped into a high-speed racehorse or a polished riding pony. They were shaped by poor forage, wet ground, woodland margins, cold Central European winters, and practical use by rural people. In modern life they are especially valued for reserve grazing and habitat management: a herd can live outdoors, browse and graze rough vegetation, move safely across damp ground, and help maintain open wetland and grassland mosaics. Their reputation for hardiness, ability to tolerate wet conditions, and willingness to cope with rushes and coarse vegetation has made them common conservation-grazing animals well beyond Poland. [mdpi](https://www.mdpi.com/2077-0472/13/2/325)

A registered Konik should stand about **130–140 cm**, or roughly **12.3–13.3 hands**. It is a compact, broad-chested, strong-boned pony of about 350–400 kg rather than a miniature riding horse. The breed has a relatively small, plain head with a straight profile; a short, substantial neck; a deep body; sturdy legs; little to no true feather; and notably tough, dark hooves. Its mane and tail are abundant and often mixed in color, with darker central hairs; some animals show an upright or semi-upright primitive-looking mane, though that is a physical trait rather than a simple coat gene. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

The signature coat is **blue dun**, often described casually as mouse-gray, but genetically it is not gray: it is a black-based horse carrying dun, conventionally expressed as \(E\_\,aa\,D\_\). The coat may range from light mouse through ordinary or dark mouse to gray-dun, with a dorsal stripe mandatory in the pure-bred standard. Zebra striping on the legs is desirable, and shoulder shadowing or barring may occur. The trunk should look smoky gray-brown, the lower legs should be dark, and mane and tail should remain visibly dark or dark-mixed. Crucially, gray is not the breed’s defining gene: a true gray Konik would whiten progressively with age, while a blue-dun Konik retains its mouse-dun character. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

Koniks tend to feel sensible, self-sufficient, fertile, and economical rather than flashy. They should not outpace a Thoroughbred, outjump a specialist sport horse, or match a mountain pony’s extreme climbing niche—but they should be dependable survival mounts with strong health and a small, sturdy frame. Their cultural history includes Tadeusz Vetulani’s 1930s Białowieża reserve project, disruption during World War II, and postwar continuation at Popielno, where reserve breeding has continued since 1955. Modern genetic research supports treating the Konik as a conserved domestic Polish population with complex origins and historical bottlenecks, not as an untouched surviving wild Tarpan. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11193968/)

For Procedural Horse Genetics, a player should recognize a Konik instantly: a compact, dark-legged, blue-dun pony with a dorsal stripe and little or no white. Breeding toward Konik type should mean preserving `E`, `a`, and `D` together—not merely collecting horses that look gray or vaguely primitive. The mod does not model the breed’s precise straight-profile head, compact body proportions, mane posture, hoof growth rate, seasonal coat texture, social behavior in a semi-feral herd, forage efficiency, conservation-grazing impact, registry inspection, or historical founder-line eligibility.

## Breed JSON

> **Schema caveat:** The supplied wiki page could not be retrieved by the documentation fetcher, so the object below follows the readable JSON convention in your Anglo-Arabian example. Before merging, reconcile exact key names, locus IDs, allele symbols, `Commonness` enum names, pricing units, source enum values, and stat-band serialization against `common/breed/spec/`.  
>
> **Genetic policy:** This intentionally models the modern registered **Konik polski**, not the looser historic “primitive Polish horse” source population. The official pure-bred Polish standard requires a mouse-colored, dorsally striped horse without white markings; therefore the founder pool is fixed to black-based dun and all other cosmetic loci are forced wild type. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

```json
{
  "id": "konik",
  "name": "Konik",
  "type": "natural",
  "notes": "Konik polski is a Polish conservation breed defined by registry pedigree, a highly restricted blue-dun coat standard, compact primitive-pony conformation, tough hooves, abundant mane and tail, thrift on rough forage, and suitability for semi-feral reserve life. Procedural Horse Genetics does not model registry inspection, founder-line pedigree, mane posture, exact head profile, hoof quality, seasonal coat change, winter insulation, forage efficiency, wetland grazing behavior, herd social structure, fertility, or conservation-management aptitude.",

  "biomes": [
    "minecraft:meadow",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:taiga",
    "minecraft:old_growth_birch_forest"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 420,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 1.0
    },
    "agouti": {
      "a": 1.0
    },
    "dun": {
      "D": 1.0
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
      0.83,
      0.91
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Konik × Konik produces Konik. Konik × another pure breed produces a Konik cross. A Konik cross bred back to pure Konik remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not automatically assign the Konik label to a visually blue-dun pony without two Konik lineage labels; the real breed is pedigree- and studbook-defined."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Registered identity | `id: "konik"` / display name `Konik` | The public-facing name is compact, while the flavour and notes identify the stricter registered breed as *Konik polski*. The Polish Horse Breeders Association maintains its Stud-book of Origin.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| Extension | `E: 1.0` | A modern pure-bred Konik founder should be capable of black pigment because its defining blue-dun coat is black-based. The published genotype for blue dun is \(E\_\,aa\,D\_\).  [ejpau.media](http://www.ejpau.media.pl/volume6/issue1/animal/art-04.html) |
| Agouti | `a: 1.0` | Fixing recessive agouti prevents bay dun and produces black-based dun—mouse-dun, grullo, or blue dun—rather than yellow-brown bay dun.  [ejpau.media](http://www.ejpau.media.pl/volume6/issue1/animal/art-04.html) |
| Dun | `D: 1.0` | The Polish standard requires mouse-colored horses with a dorsal stripe. Dun creates the primitive dorsal stripe, dark limbs, and smoky diluted body color on the black base.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| Gray | `N: 1.0` | “Mouse-gray” is a conventional visual description, not a reason to add the true gray allele. A `G` Konik would progressively depigment with age and no longer preserve the signature blue-dun phenotype.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| White patterns | All forced `N` | Pure-bred Konik breeding allows no white spots or markings, apart from a narrow temporary allowance for tiny facial markings in mares. Since the mod’s named white-pattern loci tend to produce conspicuous hereditary white, they should remain absent from pure founders.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| Other dilutions | Cream, pearl, champagne, silver, mushroom, and flaxen forced `N` | The formal preservation standard seeks the mouse-colored Konik phenotype rather than a broad palette of dilutions. This keeps founders visually coherent and makes outcross-derived colors meaningful.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| Leopard complex and roan | `LP`, `PATN1`, `PATN2`, and roan forced `N` | Neither leopard spotting nor classic roan is part of the official blue-dun, no-white Konik population target.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |
| Magical loci | All forced `N` | Konik is an ordinary natural breed; its “primitive” look comes from real dun genetics rather than magical effects. |
| Disorders | All named disorders clear | I found no breed-specific, defensible carrier-frequency survey for the listed Mendelian disorders in registered Konik polski. In a genetics-focused mod, invented disease frequencies are worse than a clear founder pool. This does not claim real Koniks are genetically disease-free; it means no verified rate was suitable for this file. |
| Speed | `4/10` | Koniks are compact working/conservation ponies, selected for practical movement and survival rather than sprinting.  [mdpi](https://www.mdpi.com/2077-0472/13/2/325) |
| Jump | `4/10` | They are capable, sound ponies but not historically selected as a purpose-bred jumping population. This is a conservative gameplay score. |
| Health | `9/10` | High heartiness represents hardiness, outdoor living, fertility, uncomplicated foaling, tolerance of wet conditions, and ability to use rough forage—not immunity from disease.  [mdpi](https://www.mdpi.com/2077-0472/13/2/325) |
| Size | `×0.83–0.91` | This creates a consistently pony-sized population, matching the adult 130–140 cm standard, approximately 12.3–13.3 hands.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/) |

## Disorder approach

The correct default for this breed file is **no modeled disorder alleles**: every listed disorder locus is forced clear.

That is not a biological claim that every real Konik is free of inherited disease. It is a data-quality decision. The requested loci are breed-associated or mutation-specific conditions, and no credible Konik-specific carrier rate was located for ACAN dwarfism, PLOD1 dwarfism, MET-associated lethal white syndrome, Arabian-line SCID/CA/LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Assigning even “small” frequencies would be fabricated.

This also creates useful gameplay contrast. Konik founders are genetically narrow in **color**, not artificially burdened with unrelated breed-associated disorders. A player can still introduce any modeled disease allele via an outcross, mutation system, or splice mechanic if the mod allows it; it simply should not originate from an evidence-free pure Konik pool.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the natural `konik` breed record with its display name, source list, biome list, price, commonness, notes, genes, and stat targets. |
| `common/breed/Breeds` | Register `konik` so it resolves in spawning, serialization, breed books, the H-menu, lineage labels, and commands. |
| `common/breed/BreedSource` | Confirm the legal source enum values for `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Accept an empty epigenetic-band object for Konik; no non-stat epigenetic expression needs forcing. |
| `common/breed/spec/` | Reconcile this illustrative JSON’s key names and allele representations with the actual read/write specification. This includes whether the project uses `type`, `natural`, nested gene pools, registry IDs, or different gene symbols. |
| `common/breed/Commonness` | Verify the enum name corresponding to the requested uncommon rarity ladder and ensure it maps to the intended spawn pull. If the implementation stores only numbers, preserve `spawn_weight: 3`. |
| `common/breed/BreedStatCurve` | Map scores `4`, `4`, and `9`, plus size `0.83–0.91`, to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll every Konik founder with `E`, `a`, and `D`; force all omitted or explicitly clear coat/disorder loci to wild type or clear. |
| `common/breed/BreedLineage` | Use normal pure-breed and cross-label behavior; no pedigree-aware exception should identify arbitrary blue-dun horses as Koniks. |
| `common/genetics/SpliceOutcome` | No special-case behavior. A splice-carrot allele follows ordinary transmission rules and can intentionally break the strict pure-Konik phenotype in offspring. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Ensure speed, jump, health, and size targets can serialize for the Konik record. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the project’s schema requires an explicit empty/default band type. |

## Verification

1. Confirm that **Konik** appears in the H-menu’s Breeds tab and the breed book with the display name, Polish conservation-breed description, source checklist, price, biome distribution, and uncommon rarity expected by the record.

2. Spawn multiple eligible wild packs in meadow, wetland, forest-edge, river, and cool-temperate woodland biomes. Every horse within a single generated Konik pack should have the **Konik** breed label, even if individual stat rolls vary.

3. Confirm that a lone ordinary wild horse generated outside the breed-pool mechanism is labeled **Feral Mixed**, not Konik.

4. Generate at least 100 Konik founders. Every founder should genetically resolve as black-based dun: an `E_ aa D_` blue-dun/grullo phenotype, usually with primitive dun markings such as a dorsal stripe and dark legs. It should not produce chestnut, bay, true black, palomino, buckskin, dunskin, champagne, silver dapple, mushroom, pearl, gray, roan, tobiano, frame, splash, leopard spotting, or magical coats.

5. Inspect founder genomes directly. Confirm `extension` contains only `E`, `agouti` contains only `a`, and `dun` contains only `D`. Confirm every named white-pattern, dilution, gray, leopard, and magical locus is wild type.

6. Inspect the disorder panel across a large founder sample. Every specified disorder locus should be clear. This validates the intentional “no evidence-supported Konik disorder frequency” implementation rather than accidentally omitting a forced-clear fallback.

7. Measure founder physical performance over a modest sample. Koniks should trend smaller than ordinary saddle horses, somewhat below baseline in sprint and jump, and conspicuously high in health/heartiness. They should feel sturdy and usable, not slow to the point of uselessness.

8. Test lineage behavior:
- Konik × Konik → Konik.
- Konik × another pure breed → Konik cross.
- Konik cross × pure Konik → the same Konik cross label under the default table.
- Two different cross labels → Mixed.
- Any pairing with Feral Mixed → Mixed.

9. Use the mod’s normal allele-editing or splice system, if enabled, to introduce a non-Konik allele such as `e`, `A`, `d`, `G`, cream, tobiano, or roan. Confirm that the altered horse and its descendants can show the expected nonstandard phenotype, while the original pure-Koniks remain visually unified.

## Sources

- [Polish Horse Breeders Association — Stud-book of Origin of Konik polski breed](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/): official breeding organization, 130–140 cm height standard, blue-/mouse-dun color requirements, dorsal stripe, permitted shades, white-marking restrictions, and desirable zebra striping. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-konik-polski-breed/)

- [PubMed — Genetic Background of the Polish Primitive Horse (Konik) Coat Color](https://pubmed.ncbi.nlm.nih.gov/34432873/): peer-reviewed confirmation that blue dun is the officially permitted Polish primitive horse coat, while documenting variation in its visible shade. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/34432873/)

- [Selection of Polish Konik horses for coat colour and the quality of coat colour](http://www.ejpau.media.pl/volume6/issue1/animal/art-04.html): genetic interpretation of blue dun as black-based dun, \(E\_ aa D\_\), plus discussion of the breed-preservation coat standard. [ejpau.media](http://www.ejpau.media.pl/volume6/issue1/animal/art-04.html)

- [Mitochondrial DNA and Y chromosome reveal the genetic background of the Polish Konik horse](https://pmc.ncbi.nlm.nih.gov/articles/PMC11193968/): peer-reviewed history of early research, development through the Janów Podlaski stud from 1923, conservation status, and the modern Polish population. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11193968/)

- [Polish Academy of Sciences, Olsztyn — 70 years of reserve breeding in Popielno](https://pan.olsztyn.pl/2025/08/polish-konik-freedom-in-the-woods-celebrating-70-years-of-reserve-breeding-in-popielno/): Białowieża work beginning in 1936, WWII interruption, Popielno’s postwar reserve herd, and continuity of the 1955-onward program. [pan.olsztyn](https://pan.olsztyn.pl/2025/08/polish-konik-freedom-in-the-woods-celebrating-70-years-of-reserve-breeding-in-popielno/)

- [The Influence of Konik Horses Grazing and Meteorological Conditions](https://www.mdpi.com/2077-0472/13/2/325): peer-reviewed discussion of Konik fertility, gentle character, food-seeking resourcefulness, and uncomplicated foaling in conservation-grazing contexts. [mdpi](https://www.mdpi.com/2077-0472/13/2/325)

- [ECOS — Horses for Nature: Equids and extensive grazing in Britain](https://www.ecos.org.uk/horses-for-nature-equids-and-extensive-grazing-in-britain/): practical conservation-grazing rationale for Koniks’ wet-ground hardiness, coarse-vegetation use, and generally reliable temperament. [ecos.org](https://www.ecos.org.uk/horses-for-nature-equids-and-extensive-grazing-in-britain/)