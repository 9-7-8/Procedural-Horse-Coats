The **Kiso Horse** (*Kisouma*, 木曽馬) should be one of PHC’s most visually unified natural breeds: a small, deep-bodied Japanese mountain horse overwhelmingly built on bay genetics, frequently marked with a dorsal stripe, and shaped by a severe twentieth-century population bottleneck. The real survey data are unusually useful here: in a 2008–09 survey covering 125 horses—86.2% of the registered breed—92.8% were bay or dark bay, 66.4% had a dorsal stripe, and only four coat categories occurred: bay, dark bay, buckskin dun, and chestnut. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

## Identity & flavour

The **Kiso Horse**, Japanese **Kisouma** (木曽馬), is one of Japan’s native horse breeds and the only indigenous horse breed of Honshu, Japan’s main island. Its homeland is the mountainous Kiso district spanning southern Nagano and eastern Gifu prefectures in central Japan. The breed has lived in and around the Kiso Valley for many centuries, with historical accounts describing horses in the region from at least the early medieval era. Rather than being a newly assembled modern breed, the Kiso is an old Japanese mountain landrace whose present form was rebuilt from a remarkably narrow twentieth-century remnant. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

Kiso horses were the working horses of a poor, steep highland region. They cultivated small mountain farms, carried people and goods along rough tracks, hauled supplies, and served military purposes from the Heian period onward. In country where large cavalry or draft horses could be expensive to keep and awkward on narrow trails, the Kiso’s compact body, calm mind, strength, and hard feet made it invaluable. A good Kiso should feel like a small horse that has no interest in showing off, but quietly gets a person, pack, or cart through difficult country. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

A Kiso is small in absolute terms but solidly horse-like in build: the scientific population survey found an average height of **131.9 ± 4.4 cm**, roughly **13.0 hands**, with most adults falling approximately 123–141 cm. Its deep chest is notable—an average girth of 167.1 cm—and its cannon circumference averaged 18.3 cm, giving it a dense, substantial mountain build rather than a delicate pony outline. Traditional descriptions emphasize a heavy or plain head, strong deep jaw, short thick neck, straight back, wide chest, round barrel, short legs, tough feet, thick mane and tail, and sometimes light feathering. In PHC, it should read as low, broad, practical, and impressively weighty for its height. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

The Kiso’s modern color story is unusually dramatic. In 1953 the breed still included bay, dark bay, chestnut, dark chestnut, black, buckskin dun, palomino, and gray; after military crossbreeding, mechanization, and a severe bottleneck, the modern registered population became almost monochrome. In the 2008–09 survey, 105 horses were bay, 11 dark bay, five buckskin dun, and four chestnut—meaning 92.8% were bay or dark bay. The dorsal stripe remains a cherished traditional trait, recorded in 66.4% of the surveyed horses. A pure Kiso herd should therefore look richly bay: brown-bay, dark bay, and occasional buckskin-dun, with rare chestnuts and frequent subtle or obvious dorsal striping. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

Kisos are known as calm, gentle, hardy, strong, tractable horses capable of thriving in harsh mountain conditions. Their aptitude is practical: steady trail riding, packing, light farm work, trekking, therapeutic riding, and cultural demonstration rather than high-level speed, large jumping, or specialized dressage. They should have good health and enduring strength, but their compact body and limited genetic base should not be mistaken for magical superiority. In-game, their personality is best expressed by a hardy, mid-small body with reliable baseline movement and a useful, modest jump score—not a racehorse engine. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

The breed survived an extraordinarily close call. In 1899, historical records listed 6,823 Kiso horses; wartime policies then forced crossbreeding with foreign horses to produce larger military mounts and ordered stallion castration. After World War II, a single intact shrine stallion, **Shinmei**, was found; his son **Daisan-haruyama**, born in 1951, became the ancestor of the modern breed. Mechanization then drove numbers down again to only **32 horses in 1976**. The Kiso Horse Conservation Association, established in 1969, organized recovery; the 2012 study reported 149 registered horses, but also found a mean pedigree inbreeding coefficient of 0.11 and an effective population size around 45.8—important context for why the coat pool is so tightly fixed. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

For *Procedural Horse Genetics*, this is a rare chance to make a real breed feel deliberately narrow without becoming boring. A Kiso player should learn to recognize the dark mane, black points, brown-to-dark-bay body, deep barrel, compact height, and often-present dorsal stripe immediately. The ideal breeding objective is not “find the rarest color”; it is to preserve the old Kiso look: sturdy bay, strong agouti and extension genetics, a chance of dun-derived dorsal striping, tiny chestnut leakage, and no gray, silver, champagne, pinto, leopard, roan, or magical effects in pure founders. The mod does **not** model Kiso head type, jaw depth, thick mane and tail, occasional light feathering, hard hoof quality, deep barrel, load-carrying capacity, mountain-footedness, Japanese native-horse cultural status, pedigree bottleneck, actual inbreeding coefficient, or registration eligibility.

## Breed JSON

> **Schema note:** The PHC wiki URL supplied earlier could not be fetched in this environment. This JSON follows the style supplied in your Anglo-Arabian example. Before implementation, verify exact JSON shape, locus IDs, enum capitalization, allele labels, and whether the project treats `nd1`/`nd2` as part of the same dun locus in `common/breed/spec/`. The exceptionally strong `E`/`A` and constrained-color implementation is directly informed by the Kiso population survey rather than generalized breed-guide estimates.

```json
{
  "id": "kiso_horse",
  "name": "Kiso Horse",
  "type": "natural",
  "notes": "Procedural Horse Genetics models the Kiso Horse's compact Japanese mountain-horse size, hardy constitution, extremely bay-centered modern coat pool, and frequent dorsal-stripe tendency. It does not model the Kiso head and jaw shape, deep barrel, thick mane and tail, occasional light feathering, hoof hardness, mountain-footedness, pack and farm-work skill, historical military use, Japanese cultural importance, the Daisan-haruyama bottleneck, actual pedigree inbreeding, effective population size, or eligibility in the Kiso Horse Conservation Association registry.",

  "biomes": [
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:meadow",
    "minecraft:birch_forest",
    "minecraft:forest",
    "minecraft:windswept_forest",
    "minecraft:windswept_hills",
    "minecraft:stony_peaks"
  ],
  "spawn_weight": 1,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 760,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.98,
      "e": 0.02
    },
    "agouti": {
      "A": 0.98,
      "a": 0.02
    },

    "dun": {
      "D": 0.08,
      "nd1": 0.60,
      "nd2": 0.32
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
    "speed": 5,
    "jump": 5,
    "health": 8,
    "size": [
      0.80,
      0.90
    ]
  },

  "epigenetic_bands": {
    "extension_black_intensity": [
      0.58,
      0.82
    ]
  },

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Kiso Horse × Kiso Horse produces Kiso Horse. Kiso Horse × another pure breed produces a Kiso Horse cross. A Kiso Horse cross bred back to a pure Kiso Horse remains that named cross; different crosses resolve to Mixed, and any cross involving Feral Mixed resolves to Mixed. A compact bay horse with a dorsal stripe must never acquire a Kiso label based only on phenotype."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Rationale |
|---|---:|---|
| Extension | `E: 0.98`, `e: 0.02` | The 2008–09 survey found only 3.2% chestnuts and no black horses among 125 studied Kisos. The paper specifically concludes that the high bay proportion suggests near fixation of `E` and `A`, potentially `E/E` and `A/A` in much of the population. A 2% `e` pool preserves a very rare chestnut founder without making chestnut impossible.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Agouti | `A: 0.98`, `a: 0.02` | Bay plus dark bay made up 92.8% of the sampled registered population. A near-fixed agouti pool is the most direct Mendelian translation of that documented phenotype distribution. The tiny `a` value allows a rare black-based genetic outcome without falsely claiming it is a routine modern Kiso color.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Dun | `D: 0.08`, `nd1: 0.60`, `nd2: 0.32` | The survey found 4.0% buckskin dun and a dorsal stripe in 66.4% of adults. A low dominant-dun frequency produces a small buckskin-dun class, while a high `nd1` frequency is a useful PHC approximation for widespread dorsal-striping potential outside obvious dun. This depends on PHC actually differentiating `nd1` from `nd2`; otherwise, keep `D` low and handle dorsal-striping through a separate trait or cosmetic system.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Cream | Forced wild type | Modern surveyed Kisos included only bay, dark bay, buckskin dun, and chestnut. Although 1953 records included a small palomino fraction, the current bottlenecked population did not. The visible “buckskin dun” category is therefore best treated as a dun-color classification, not proof that cream remains common in the breed.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Gray | Forced wild type | Gray was reported at just 0.2% in the 1953 historical source and was absent from the 2008–09 registered survey. Do not generate gray pure founders.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Silver, champagne, pearl, mushroom | Forced wild type | No population evidence supports these dilutions in the modern Kiso. Excluding them is essential to preserve the unusually narrow real-world founder palette.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Pinto and white patterns | Forced wild type | The modern Kiso phenotype is solid and strongly fixed toward bay/dark bay. There is no source basis for putting tobiano, frame, splash, sabino, W-series dominant white, leopard complex, roan, rabicano, or brindle into a pure founder pool.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Black intensity band | `0.58–0.82` | This optional narrow epigenetic range gives black mane, tail, legs, and dorsal stripe enough depth for the iconic dark-pointed bay/dark-bay Kiso look without forcing every founder into near-black seal brown. It deliberately bands pigmentation shade, not the base extension genotype or body-stat axes. |
| Disorders | All named loci clear | No trustworthy Kiso-specific carrier frequencies were found for the listed monogenic disease panel. The population is demonstrably bottlenecked and inbred, but inbreeding coefficient is not evidence for inventing rates for ACAN dwarfism, SCID, CA, LFS, GBED, HYPP, PSSM1, HERDA, or other named mutations.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Speed | 5/10 | The Kiso was selected for mountain transportation, farming, and carrying, not racing. Baseline speed captures practical travel without turning it into a fast specialist.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Jump | 5/10 | Its practical, compact mountain build may handle trail obstacles, but there is no basis for a high sport-jumping score. Keep jumping at baseline. |
| Health | 8/10 | This represents a tough local mountain constitution, strong feet, and centuries of selection for survival and work in difficult highlands. It is intentionally not 9–10 because the modern breed’s small, inbred population is a conservation concern, not proof of exceptional genetic robustness.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |
| Size | ×0.80–0.90 | The mean height was 131.9 cm, near 13.0 hands, with the observed adult range about 123–143 cm. This should produce a compact, substantial small horse/large pony.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/) |

### Important dun caveat

The published Kiso study documents **dorsal stripe phenotype**, not a genotype survey of `D`, `nd1`, and `nd2`. The `D: 0.08` / `nd1: 0.60` design is therefore an informed gameplay translation rather than a claim that 60% of modern Kisos are genetically `nd1`. It serves two goals at once:

- It preserves the observed 4% buckskin-dun minority by keeping full dun uncommon.
- It lets the mod express the observed 66.4% dorsal-stripe frequency if `nd1` permits residual primitive markings.

If PHC does **not** make `nd1` visibly striped, use a dedicated dorsal-stripe cosmetic trait at approximately 0.66, or leave the stripe unmodeled rather than raising `D` so high that one-third to one-half of all Kisos become visibly dun. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

## Spawning & lineage

The Kiso belongs in cool, mountainous central-Japan-feeling terrain: groves, taiga variants, old-growth conifer forests, birch forest, mountain meadow, windy forest edges, and stony peaks. These biomes evoke the Kiso Valley’s highland farms, mountain woods, steep tracks, and cold upland pasture better than desert, tropical jungle, plains, or swamp. The historical Kiso was a farm, transport, and mountain trail horse of the southern Nagano/eastern Gifu uplands. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

`RARE` with `spawn_weight: 1` is justified. The breed was classified as critical in the FAO’s World Watch List context, reached only 32 animals in 1976, and was reported at 149 registered horses in the population study. This should be a meaningful discovery in a survival world, while `stable` and `spawn_egg` access ensure a player can intentionally begin a conservation breeding program. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)

No special lineage override is appropriate. The real Kiso Horse Conservation Association uses pedigree and conservation criteria, not coat appearance. Therefore a bay, dorsal-striped cross must remain a **Kiso Horse cross** or **Mixed** under PHC’s normal rules, even if it looks almost indistinguishable from a pure Kiso.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Adds the natural `kiso_horse` record with name, notes, biome pool, sources, rarity, price, genes, stat targets, and optional shade band. |
| `common/breed/Breeds` | Registers `kiso_horse` so it resolves in world generation, saved horse data, breeding output, H-menu displays, books, and commands. |
| `common/breed/BreedSource` | Validates `wild`, `cowboy`, `spawn_egg`, and `stable` as available Kiso acquisition routes. |
| `common/breed/BreedBands` | Serializes `extension_black_intensity: [0.58, 0.82]`; verify the actual epigenetic key for black-point depth and remove the band if the project has no matching numeric gene. |
| `common/breed/spec/` | Defines/parses the record in both directions and verifies field names, source/commonness enums, locus identifiers, exact allele labels, stat object format, and empty strain list handling. |
| `common/breed/Commonness` | Maps `RARE` to the intended rarity ladder and confirms numerical `spawn_weight: 1` is legal or correctly derived. |
| `common/breed/BreedStatCurve` | Converts speed 5, jump 5, health 8, and size ×0.80–0.90 into valid `TargetBand` ranges. |
| `common/breed/BreedFounder` | Creates founders from the near-fixed `E`/`A` population, low dun pool, optional dorsal-stripe genotype pathway, clear disease loci, and forced-wild-type excluded colors. |
| `common/breed/BreedLineage` | Preserves the default pure/cross/Mixed table and prevents phenotype-only Kiso classification. |
| `common/genetics/SpliceOutcome` | Needs no Kiso exception. Spliced alleles should inherit normally in later foals even when those alleles cannot originate in a pure Kiso founder. |
| `common/trait/StatAxis` | Supplies speed, jump, health, and size as the four Kiso axes. |
| `common/trait/TargetBand` | Represents the result ranges generated from Kiso score and size choices. |
| `common/trait/BreedStatTargets` | Stores and applies the Kiso Horse’s target configuration during founder generation. |
| `common/breed/BandType` | Use the normal epigenetic band mechanism only if `extension_black_intensity` is a valid numeric band target; no `TRADITIONAL` or `BACHELOR` special behavior is otherwise required. |

## Verification

1. **Record and UI**
   - Open the H menu’s Breeds tab and verify **Kiso Horse** appears as a natural breed with the `RARE` commonness, price 760, source list, Japanese-mountain biome pool, and correct notes.
   - Confirm the breed book presents the same display name and the serialized ID remains `kiso_horse` through entity save/load.

2. **Pack identity**
   - Spawn natural packs in groves, taigas, old-growth conifer forests, birch forests, meadows, windswept forests, or stony peaks.
   - Verify every horse in one generated herd has the **Kiso Horse** label.
   - Generate a lone, non-breed wild horse and confirm it reads **Feral Mixed**.

3. **Bay fixation**
   - Generate at least 500 Kiso founders; this breed needs a larger sample because its predicted off-types are intentionally very rare.
   - Confirm that nearly every founder is genetically `E_ A_` and visually bay or dark bay.
   - In a sample of 500, expect almost no chestnuts and at most a handful of black-based outcomes; if chestnuts or blacks become common, reduce `e` and `a` further or use `1.0` fixation.
   - Confirm bay/dark bay visually dominates to an extent that a herd appears nearly uniform without every individual being identical.

4. **Dorsal stripe and dun**
   - Confirm that around two-thirds of founders show a dorsal stripe if PHC’s `nd1` expression supports that trait.
   - Confirm that fully obvious dun-derived founders stay uncommon, roughly mirroring the small buckskin-dun minority in the observed study.
   - If the mod’s `nd1` produces no stripe, do not accept a 66% stripe result merely by increasing `D`; instead add an appropriate primitive-marking system or document dorsal stripe as an unmodeled feature.

5. **Excluded colors**
   - Confirm pure founders never generate gray, palomino, cremello, perlino, smoky cream, champagne, silver, mushroom, pearl, tobiano, frame, splash, sabino, W-series white, roan, leopard complex, rabicano, brindle, or magical phenotypes.
   - Confirm any excluded trait can appear only after deliberate genetic alteration or outcrossing and that it changes the offspring’s lineage label normally.

6. **Disorders and health**
   - Inspect at least 200 founders and confirm every named disorder locus is clear.
   - Breed pure Kisos for several generations and confirm no listed disease allele originates spontaneously.
   - Compare stats with an ordinary saddle horse: Kisos should remain clearly smaller, hardy, and useful over practical terrain, but baseline in raw speed and specialized jumping.

7. **Lineage behavior**
   - Kiso Horse × Kiso Horse → **Kiso Horse**.
   - Kiso Horse × another pure breed → **Kiso Horse cross**.
   - Kiso Horse cross × pure Kiso Horse → the same named cross.
   - Kiso Horse cross × another distinct cross → **Mixed**.
   - Kiso Horse or Kiso Horse cross × Feral Mixed → **Mixed**.
   - Confirm all labels match in breeding outcomes, H-menu inspection, breed book, saved entities, and stable UI.

## Sources

- [Takasu et al., 2012, *Journal of Equine Science* — “Population Statistics and Biological Traits of Endangered Kiso Horse”](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/): primary peer-reviewed source covering 125 horses, or 86.2% of the registered population; modern coat counts, dorsal stripe prevalence, average height and body measurements, population history, bottleneck, conservation association, inbreeding, effective population size, and historical color comparison. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4013974/)
- [Kiso Uma no Sato / Kiso Horse Village](https://www.kisoumanosato.or.jp/): living Kiso-region cultural and visitor center for direct contact with the breed in its highland homeland. [kisoumanosato.or](https://www.kisoumanosato.or.jp/)
- [World Unite — Native horse breeds of Japan](https://www.world-unite.de/en/magazine/native-horse-breeds-japan): supplementary overview of the Kiso’s central Japanese mountain origin, Heian-era military and farm use, compact stature, hardiness, and conservation context. [world-unite](https://www.world-unite.de/en/magazine/native-horse-breeds-japan)
- [More Than Tokyo — Kiso horses and Meiji-era consequences](https://www.morethantokyo.com/kiso-horses/): supplementary public-history account of Kiso Valley use, Meiji and twentieth-century decline, the 1976 low point, conservation recovery, and narrowed modern color pool. [morethantokyo](https://www.morethantokyo.com/kiso-horses/)
- [Discover the Horse — Kiso Horse](https://www.discoverthehorse.com/breeds/kisohorse): supplementary modern profile linking the breed with Kiso Uma no Sato and summarizing Japanese origin, working history, hardiness, and average height. [discoverthehorse](https://www.discoverthehorse.com/breeds/kisohorse)