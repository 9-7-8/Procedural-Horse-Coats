The **Hucul Pony**—often called the **Hucul Horse**, **Hutsul**, **Huzul**, or Carpathian Hucul—should be a compact, immensely hardy Carpathian mountain horse with a powerful bay-and-dun identity, primitive dorsal markings, hard hooves, and calm working intelligence. It belongs in PHC as a conservation-minded landrace: a small, deep-bodied, all-weather trail, pack, farm, and driving horse that rewards breeding for bay dun, mouse dun, strong health, and compact mountain strength. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

## Identity & flavour

The **Hucul Horse**, commonly called the **Hucul Pony**, is an indigenous Carpathian mountain breed associated with the Hutsul/Hucul region of the eastern Carpathians. Its historic homeland spans territory that has belonged at different times to Romania, Poland, Slovakia, Ukraine, the Czech lands, Hungary, and the former Austro-Hungarian Empire. The names *Hucul*, *Hutsul*, and *Huzul* all occur in English and European breed writing; “Carpathian Pony” is a useful descriptive alias, though the official international studbook language commonly uses **Hucul horse**. Professional accounts mention the breed as early as 1613, while systematic recording began after the Imperial stud was established at Łuczyn in Bukovina in 1856. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

This is a true mountain utility horse. Huculs carried people and supplies over steep forest tracks, worked farms, pulled carts and heavy vehicles, packed loads, and served as reliable saddle and draught horses for Carpathian communities. Armies also valued them as saddle, draught, and cart horses. Their survival depended on calmness, firm footing, thrift, and the ability to live much of the year outdoors on grass and hay rather than constant stable care. In modern use, they remain excellent trail and trekking horses, carriage horses, riding-school mounts, farm helpers, and family horses; the breed’s official goals still emphasize versatility, endurance, intelligence, gentleness, and resilience. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

Most Huculs stand about **132–148 cm**, roughly **13.0–14.2 hands**, with an average stick measurement around 140 cm. They are not fine little ponies: a proper Hucul is dense, rectangular, deep-chested, and strongly made, with well-sprung ribs, substantial limbs, good joints and tendons, a short strong neck, muscular croup, thick mane, and notably hard, healthy, well-formed hooves that often need little shoeing. Their tails and manes should look thick and practical, while their legs should be clean and strong rather than heavily feathered. Players should see a Hucul as a compact mountain workhorse with the mass and confidence to take a rider, panniers, or a cart through rough spruce woods. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

The classic Hucul look is **bay with primitive markings**: a dark dorsal stripe, sometimes zebra barring across the legs, and frequently a darker shoulder or croup stripe. Bay is widespread; black occurs; chestnut is rare; and dun forms include bay dun, mouse/gray dun, ginger/red dun, and historically dun-skewbald. The Polish/Hucul international studbook explicitly describes dorsal bands and zebra stripes as inherited wild-horse-type features and recognizes bay, black, dun, chestnut, and skewbald. It excludes gray and isabella/cream from its desired registered coat pool. That makes the pure PHC Hucul visually much tighter than a generic “all colors permitted” mountain pony: bays and dun variants should dominate, with a small black contingent and very rare chestnuts. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

A Hucul should feel kind, sensible, hard-working, and steady rather than hot or reactive. Official breeding aims name gentleness, intelligence, endurance, “firm paces,” toughness, and lack of fastidiousness—meaning a low-maintenance willingness to live and work in modest conditions. This is not a dedicated lateral-gait breed, a high-speed racehorse, or an elite specialist jumper. Its power is practical athleticism: reliable travel over bad ground, strength under load, durable joints and hooves, and a rideable mind. In PHC terms, favor high health, moderate jumping, baseline-to-slightly-above baseline speed, and a pony/small-horse body scale. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

The breed’s history is also a survival story. Huculs were fragmented when the Carpathian homeland was divided after the First World War, and the Second World War reduced some national populations to only a few surviving animals. Modern national populations in Poland, Romania, Slovakia, Hungary, the Czech Republic, and Austria are coordinated across borders through the **Hucul International Federation**. The closed main studbook requires documented descent—normally five generations on both sides—and DNA/blood verification has been required for origin protection since 2002. Research characterizes the Hucul as a vanishing breed requiring conservation breeding; preservation programs have maintained genetic diversity, but small populations still require careful management of stallion lines and mare families. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

For *Procedural Horse Genetics*, a pure Hucul herd should look at home where dark conifers meet high meadow: solid bay, black, or dun ponies with sturdy mountain proportions and strong primitive markings. Players should chase the satisfying classic genotype—black pigment, agouti, dominant dun, dark mane and legs—and should know immediately that gray, palomino, buckskin, champagne, leopard patterns, loud Paint patterns, and magical coats do not belong in a pure founder pool. The mod does **not** model the exact rectangular body, thick mane, hard hoof quality, winter coat, sure-footedness, pack ability, outdoor-husbandry thrift, discipline training, local national subtypes, seven paternal foundation lines, pedigree depth, or the strict real-world studbook inspection system.

## Breed JSON

> **Schema note:** The PHC wiki URL supplied in the original request could not be retrieved through the browser. This definition follows the example convention provided in the request. Confirm the actual field names and locus/allele identifiers against `common/breed/spec/`, the genetics registry, and the serializer before compilation—especially whether dun is modeled as `dun`, `TBX3`, or `D/nd1/nd2`, and whether pinto/skewbald maps to a specific supported spotting locus.

```json
{
  "id": "hucul_pony",
  "name": "Hucul Pony",
  "type": "natural",
  "notes": "Procedural Horse Genetics models the Hucul Pony's compact mountain-horse size, hardy constitution, practical athleticism, and bay-and-dun-centered coat pool. It does not model its exact rectangular body, dense mane and forelock, seasonal coat, hoof hardness, sure-footedness, ability to carry heavy loads, outdoor-living thrift, temperament, firm paces, national Hucul subpopulations, seven sire lines, five-generation pedigree requirements, DNA parentage verification, or real-world studbook selection.",

  "biomes": [
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:stony_peaks"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 500,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.89,
      "e": 0.11
    },
    "agouti": {
      "A": 0.85,
      "a": 0.15
    },

    "dun": {
      "D": 0.60,
      "nd1": 0.15,
      "nd2": 0.25
    },

    "cream": {
      "N": 1.0
    },
    "grey": {
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
    "pearl": {
      "N": 1.0
    },
    "flaxen": {
      "N": 1.0
    },

    "tobiano": {
      "N": 0.99,
      "TO": 0.01
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
    "speed": 5,
    "jump": 6,
    "health": 9,
    "size": [
      0.82,
      0.94
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Hucul Pony × Hucul Pony produces Hucul Pony. Hucul Pony × any other pure breed produces a Hucul Pony cross. A Hucul Pony cross bred back to a pure Hucul Pony remains that named cross; two different crosses become Mixed, and any lineage crossed with Feral Mixed becomes Mixed. A compact bay dun phenotype alone must not confer a Hucul label."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Reasoning |
|---|---:|---|
| Base-color pool | `E` 0.89 / `e` 0.11; `A` 0.85 / `a` 0.15 | Bay is the widespread standard Hucul color; black occurs; chestnut is explicitly described as rare. A high `E` and high `A` pool produces predominantly bay and bay-dun animals, some black or grullo/mouse-dun animals, and a small red/chestnut fraction. These are game-balance allele estimates, not a published breed-wide SNP survey.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Dun | `D` 0.60 | Primitive markings—dorsal stripe, leg barring, and sometimes croup/shoulder shading—are central to the Hucul identity, and bay dun and mouse/gray dun are specifically recognized. A high dominant-dun rate makes these markings a familiar sight in a pure herd without making every founder dun.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Non-dun 1 | `nd1` 0.15 | If PHC distinguishes `nd1` from `nd2`, this gives a subset of non-dun Huculs a chance at residual primitive markings. If PHC has only one non-dun state, combine the `nd1` and `nd2` probabilities into the ordinary wild-type/non-dun allele. |
| Gray | Forced wild type | The international Polish/HIF studbook description says gray was eliminated from the breed’s accepted pool because of Arabian-origin influence. Pure founders should never generate gray.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Cream / isabella | Forced wild type | The same studbook description excludes isabella, the traditional term generally used for cream-diluted yellow coats. Pure founders therefore should not create palomino, buckskin, smoky black, or double-cream outcomes.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Tobiano / skewbald | `TO` 0.01 only | The official text recognizes rare **skewbald** and dun-skewbald Huculs, unlike many conservative pony standards. In PHC, a 1% tobiano allele frequency is a restrained proxy for that historically admitted patterned minority. Remove this small pool and use `N: 1.0` if your project interprets the relevant Hucul studbook population as solid-only or lacks an appropriate skewbald model.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Other white loci | Forced wild type | The available primary material identifies skewbald but does not justify frame, splash, sabino, W-series dominant white, roan, rabicano, or leopard-complex alleles. Do not use unrelated spotting genes merely to create “some pinto.” |
| Other dilutions | Forced wild type | No source supports pure founder populations carrying champagne, silver, pearl, mushroom, or magical loci. Their absence keeps the Hucul’s visual focus on base colors, dun dilution, and primitive markings. |
| Disorders | All named disorders clear | No robust Hucul-specific carrier-frequency study was located for the listed Mendelian disease panel. Conservation and pedigree studies establish bottleneck/inbreeding management needs, but they do not justify inventing frequencies for SCID, CA, LFS, GBED, HYPP, HERDA, PSSM1, or other named loci.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7401648/) |
| Speed | 5/10 | Huculs possess firm paces and real endurance but were selected for mountain labor, transport, and loaded travel—not flat-out racing. Baseline speed is more faithful than a trotter or racehorse score.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Jump | 6/10 | Strong hindquarters, compact athleticism, mountain footing, and modern saddle use support a modestly above-baseline jump score. It should represent practical obstacles and terrain rather than elite sport specialization.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Health | 9/10 | The official breeding objectives specifically preserve immunity, thrift, gentleness, intelligence, endurance, and outdoor hardiness; its mountain constitution, joints, tendons, and hooves are repeatedly emphasized. This score represents broad durability, not literal disease immunity.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |
| Size | ×0.82–0.94 | The standard target is 132–148 cm, around 13.0–14.2 hands. This yields a substantial small horse or large pony, bigger-feeling than a small native pony but below ordinary riding-horse height.  [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/) |

### Coat-policy caveat

The only intentionally nonzero white-pattern pool is a very rare tobiano proxy. The international studbook describes **skewbald** as admitted and explains it as a historical feature likely introduced through oriental blood, while most of the modern desired palette remains bay, black, and dun. Because “skewbald” is a phenotype term—not a molecular diagnosis—the `TO: 0.01` setting is an implementation convenience, not evidence that all patterned Huculs are genetically tobiano. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

If your PHC implementation favors strict visual uniformity over preserving that historically accepted minority, replace the tobiano section with:

```json
"tobiano": {
  "N": 1.0
}
```

That produces an entirely solid pure-founder Hucul pool, still fully compatible with the central bay-and-dun breed identity.

## Spawning & lineage

Use cold forest, grove, taiga, meadow, windswept forest, foothill, and stony-peak biomes. Huculs belong in the transition between timber and high pasture: dark conifers, birch, mountain tracks, high grass, valleys, and steep stony slopes—not desert, jungle, or flat warm prairie. Their real homeland is the forested eastern Carpathians, and their historic work involved mountainous farms, transport, and outdoor herd life. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

`UNCOMMON` with `spawn_weight: 3` is appropriate. It allows a player to discover a small Hucul herd often enough for meaningful breeding while respecting the breed’s conservation status and limited international population. The Hucul is described in peer-reviewed conservation work as a vanishing breed; its surviving national populations require coordinated international genetic management. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7401648/)

No lineage exception is needed. The strict real-world main studbook requires documented pure descent for at least five generations, but PHC’s normal pure/cross/Mixed naming is the correct game abstraction: a Hucul cross remains a cross even if it inherits dun, a dorsal stripe, hardiness, and the compact body of its Hucul parent. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Adds the `hucul_pony` natural breed record, display name, biome pool, spawn behavior, price, rarity, source checklist, coat/disorder constraints, and player-facing notes. |
| `common/breed/Breeds` | Registers `hucul_pony` for founder generation, breed books, menus, commands, saved entities, and lineage-name resolution. |
| `common/breed/BreedSource` | Validates `wild`, `cowboy`, `spawn_egg`, and `stable` as supported acquisition methods. |
| `common/breed/BreedBands` | Serializes the empty band object. Do not force universal dark shade or stripe intensity; dun genetics should create natural founder variation. |
| `common/breed/spec/` | Matches all JSON names to the actual schema in both directions: natural type enum, commonness enum, source values, gene IDs, allele names, stat-target format, and empty-map handling. |
| `common/breed/Commonness` | Maps `UNCOMMON` and/or the numerical weight of `3` to the project’s intended rarity ladder. |
| `common/breed/BreedStatCurve` | Resolves speed 5, jump 6, health 9, and size ×0.82–0.94 into legal target bands. |
| `common/breed/BreedFounder` | Rolls a founder only from the listed Hucul allele pools and defaults every omitted pattern/dilution to wild type and every omitted disorder to clear. |
| `common/breed/BreedLineage` | Implements ordinary pure Hucul, Hucul-cross, and Mixed outcomes; no automatic “Hucul” recognition by phenotype should be added. |
| `common/genetics/SpliceOutcome` | Requires no breed-specific logic. An allele introduced through a splice carrot should follow normal transmission even if it is not allowed in a pure Hucul founder pool. |
| `common/trait/StatAxis` | Uses existing speed, jump, health, and size axes. |
| `common/trait/TargetBand` | Stores legal output intervals for each targeted axis. |
| `common/trait/BreedStatTargets` | Associates the Hucul Pony’s four stat targets with its founder-generation record. |
| `common/breed/BandType` | Does not require `TRADITIONAL` or `BACHELOR`; use the normal empty-band/default behavior. |

## Verification

1. **Menus and records**
   - Open the H menu’s Breeds tab and confirm **Hucul Pony** is present as a natural breed with the correct sources, mountain-biome list, price, `UNCOMMON` status, and notes.
   - Confirm the breed book shows the same display name, and ensure saved/reloaded founders retain the `hucul_pony` ID.

2. **Wild herd consistency**
   - Generate packs in grove, taiga, old-growth taiga, meadow, windswept forest, or stony-peak terrain.
   - Confirm every horse created in an individual natural pack reads **Hucul Pony**, even if the pack contains bay, black, dun, and rare chestnut founders.
   - Confirm a lone non-breed wild horse still displays **Feral Mixed**.

3. **Coat distribution**
   - Roll at least 200 founders and log genotype plus phenotype.
   - Confirm bay and bay dun are the dominant impressions, with a large minority of mouse/grullo-like or other dark dun founders.
   - Confirm some solid black and rare chestnut/red-dun founders occur.
   - Confirm dun founders render a dorsal stripe and, where PHC supports it, leg barring or primitive markings.
   - Confirm gray, cream, champagne, silver, pearl, mushroom, roan, frame, splash, sabino, W-series white, leopard complex, rabicano, brindle, and magical coats never appear among pure founders.
   - Confirm the rare pinto/skewbald-like outcome appears only if retaining `TO: 0.01`, and that it stays genuinely uncommon.

4. **Disorder audit**
   - Inspect a statistically useful founder sample and verify every listed disorder locus is clear.
   - Breed pure Huculs through multiple generations and confirm no named disorder mutation arises spontaneously.
   - Introduce a test allele by outcrossing or genetics tools and verify it transmits normally, proving that founder restrictions do not override ordinary Mendelian inheritance.

5. **Stat behavior**
   - Compare Hucul founders with an ordinary saddle horse, a small pony, and a specialist sport breed.
   - Huculs should remain below ordinary riding-horse size but feel substantial for their height.
   - They should show very high health, practical jumping ability, and ordinary riding-horse speed—not be mechanically transformed into racers or elite show jumpers.

6. **Lineage behavior**
   - Hucul Pony × Hucul Pony → **Hucul Pony**.
   - Hucul Pony × any other pure breed → **Hucul Pony cross**.
   - Hucul Pony cross × pure Hucul Pony → the same named cross under the default table.
   - Hucul Pony cross × a different named cross → **Mixed**.
   - Hucul Pony or Hucul Pony cross × Feral Mixed → **Mixed**.
   - Verify these labels in the H menu, breed book, foal screen, genome inspector, spawned entities, and after save/reload.

## Sources

- [Polish Horse Breeders Association / Hucul International Federation — Stud-Book of Origin of Hucul Horses](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/): primary breeding and registry source for history, 1613 first mention, 1856 formal recording, Carpathian origin, use, build, height, hoof quality, accepted colors, primitive markings, studbook rules, pedigree requirements, and closed-book policy. [pzhk](https://www.pzhk.pl/en/breeding/stud-book-of-origin-of-hucul-horses/)
- [Posta, Somogyvári & Mihók, 2020 — Historical Changes and Description of the Current Hungarian Hucul Horse Population](https://pmc.ncbi.nlm.nih.gov/articles/PMC7401648/): peer-reviewed conservation history, post-war bottleneck, population recovery, international coordination, inbreeding/effective-population issues, and the breed’s FAO “vanishing” classification. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7401648/)
- [Mackowski et al., 2015 — Genetic Diversity in Hucul and Polish Primitive Horse Breeds](https://aab.copernicus.org/articles/58/23/2015/): molecular and pedigree evidence for genetic diversity, conservation-management context, and approximate Hucul inbreeding trends in Poland. [aab.copernicus](https://aab.copernicus.org/articles/58/23/2015/)
- [Oklahoma State University — Hucul Horses](https://breeds.okstate.edu/horses/hucul-horses): supplementary breed summary for Carpathian origin, history, mountain utility, size, color, temperament, and conservation context. [breeds.okstate](https://breeds.okstate.edu/horses/hucul-horses)
- [Mackowski et al., 2019 — TBX3 and ASIP genotypes reveal discrepancies in officially recorded coat colors of Hucul horses](https://www.sciencedirect.com/science/article/pii/S1751731118003506): supports treating Hucul color terminology carefully and modeling dun/agouti through actual genetic loci rather than relying only on registry phenotype labels. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S1751731118003506)