The earlier answer was not in the requested project-style format and it materially under-modeled the Fell Pony’s defining genetic issue: **Foal Immunodeficiency Syndrome (FIS)**. Below is a revised, schema-caveated Fell Pony definition in the format you provided, built around the official Fell Pony Society standard and the published FIS evidence.

The **Fell Pony** is a native British mountain-and-moorland pony from Cumbria, the Lake District, and the northern English fells. It should feel like a compact, deeply dark, weatherproof all-purpose working pony: overwhelmingly black, but not genetically monocultural; sturdy, sure-footed, and intensely hardy rather than fast or flashy. Its distinct gameplay identity is a black-dominant herd with occasional brown, bay, and grey founders, substantial health/heartiness, pony scale, and a historically important FIS carrier-management challenge. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)

## Identity & flavour

The **Fell Pony**—sometimes called the **Fell**, or historically the **Fell Pony of the northern fells**—is one of Britain’s traditional native pony breeds. It developed in Cumbria and the upland country around the Lake District, where closely managed but free-ranging pony populations had occupied the northern fells for centuries. The Fell Pony Society was formed in 1922 to preserve the old breed, though the pony’s roots reach much farther back: the Royal Society for the Protection of Rare Breeds describes its presence in the Lake District and surrounding fells as extending for more than 2,000 years. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)

This is a pony made for hard country rather than a show ring. Fell Ponies carried packs across steep hills, transported farm goods, and hauled lead from northern mines; they also served farmers and shepherds in country too wet, rocky, cold, narrow, or uneven for larger horses. Their traditional life required economical movement, sound feet, resilience on sparse grazing, and enough strength to work all day without becoming cumbersome. In modern hands, that base supports riding, driving, trekking, endurance, and practical family-pony work—but the underlying feel remains the same: a small black workhorse built for weather and distance. [rbst.org](https://www.rbst.org.uk/watchlist-breed/fell-pony/)

A true Fell is a substantial pony, usually about 13 to 14 hands and never exceeding 14 hands under the Fell Pony Society standard. The desired type has a neat, pony-like head with a broad forehead; a good neck and sloping shoulder; a deep middle; muscular quarters; strong legs with plenty of flat bone; moderately sloped pasterns; and hard, open feet with characteristic blue horn. Its mane and tail are long, thick, and dense, while straight silky feather extends toward the knees. Those details make the visual silhouette unmistakable, although the mod’s body and coat systems cannot literally render every conformational and hair-quality trait. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)

The registered palette is narrow and purposeful. The core standard lists **black, brown, bay, and grey**; black became dominant only in the latter half of the 20th century, following an earlier era in which bay and brown were especially common. Chestnuts, piebalds, and skewbalds may be recorded in Section X rather than the main colour standard. Small white markings may occur—a star, restricted white around the muzzle, or white on a hind leg or hoof—but extensive facial white, white forelegs, and conspicuous flesh marks are discouraged. In mod terms, a Fell herd should instantly read as dark: black first, dark brown second, then rare bay and grey, with essentially no ordinary pinto, leopard, cream, dun, silver, champagne, or roan surprise in pure mainstream founders. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)

The Fell’s most important real-world genetic consideration is **Foal Immunodeficiency Syndrome (FIS)**, formerly called Fell Pony Syndrome. It is a fatal autosomal-recessive condition: carriers appear healthy, but an affected foal inherits the causative allele from both parents and develops severe immune dysfunction and fatal anemia. A foundational screen of breed samples found 82 carriers among 214 Fell Ponies—about 38%—and UC Davis reports early carrier rates near 40% in Fell Ponies. Responsible breeding now relies on DNA testing and avoiding carrier-to-carrier matings. For the mod, that makes the Fell a wonderful breed for deliberate pedigree stewardship: breed toward the iconic dark, hardy native type while preserving useful genetic diversity and managing a meaningful recessive-risk locus. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/)

What the mod does **not** model is the Fell’s dense winter coat, long mane and tail, straight silky feather, blue-hoof horn, exact head profile, true hill sense, feeding thriftiness, metabolic-management needs, working training, harness aptitude, or the experience of crossing wet stone, bog, and steep Cumbrian ground. It also cannot capture the cultural importance of a protected native breed merely by giving a horse a dark coat and a compact stat band.

## Breed JSON

> **Schema caveat:** The wiki URL supplied in the request could not be retrieved by the browser tool, so the JSON below follows the field and naming style of your Australian Stock Horse example rather than claiming exact parser-valid locus IDs. Before merging, reconcile the object names, allele tokens, `commonness` enum, biome IDs, and any disease-locus implementation with the actual definitions in `common/breed/spec/` and `common/genetics/`.
>
> **FIS caveat:** FIS is specifically important enough to include for this breed. The requested disorder-locus list does not name the FIS gene/locus, so `FIS` below is necessarily a proposed project locus identifier. If PHG has no FIS implementation, omit this field only as a technical limitation—not because the real breed is clear of genetic risk. The documented historical carrier estimate is approximately 38–40%. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/)

```json
{
  "id": "fell_pony",
  "name": "Fell Pony",
  "type": "natural",
  "notes": "Fell Ponies are defined by compact native-pony conformation, dense weatherproof coat, long thick mane and tail, straight silky feather, characteristic blue-horn hooves, sure-footed hill movement, thriftiness, and a calm, willing working temperament. Procedural Horse Genetics does not simulate mane and tail density, feather length or texture, hoof-horn colour and hardness, exact head profile, winter coat, sparse-grazing adaptation, Cumbrian hill sense, harness training, pack work, metabolic management, or the training and judgement that make a trustworthy mountain-and-moorland pony.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:meadow"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 460,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.97,
      "e": 0.03
    },
    "agouti": {
      "A": 0.30,
      "a": 0.70
    },

    "grey": {
      "N": 0.94,
      "G": 0.06
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
    "dun": {
      "N": 1.0
    },
    "flaxen": {
      "N": 1.0
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.995,
      "SB1": 0.005
    },
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_1": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 0.995,
      "W": 0.005
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
    "FIS_foal_immunodeficiency_syndrome": {
      "N": 0.62,
      "fis": 0.38
    },

    "ACAN_dwarfism": {
      "N": 1.0
    },
    "PLOD1_friesian_dwarfism": {
      "N": 1.0
    },
    "MET_lethal_at_conception": {
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
    "jump": 5,
    "health": 9,
    "size": [
      0.82,
      0.91
    ]
  },

  "epigenetic_bands": {
    "coat_darkness": {
      "min": 0.82,
      "max": 1.0
    }
  },

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Fell Pony × Fell Pony produces Fell Pony. Fell Pony × another pure breed produces the appropriate named cross. A Fell Pony cross bred back to a pure Fell Pony remains a cross; two unlike crosses become Mixed; and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Suggested implementation | Rationale |
|---|---:|---|
| Visual identity | Near-solid dark herd | The Society standard recognizes black, brown, bay, and grey, and reports black as the modern predominant colour. This should be one of the mod’s most visually coherent natural pony breeds.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Extension | `E` 0.97 / `e` 0.03 | Chestnut is not one of the four core standard colours; it is recorded in Section X. Keeping `e` extremely uncommon preserves the possibility of historically documented chestnut outliers without letting chestnut dominate pure founders.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Agouti | `A` 0.30 / `a` 0.70 | With extension nearly fixed, a lower agouti rate produces mostly black founders while retaining brown and bay founders as recognized standard colours. This is a deliberate phenotype-weighting choice, not a published population allele-frequency claim.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Grey | `G` 0.06 | Grey is standard-accepted but visually minor compared with modern black Fell Ponies; six percent keeps it recognizably possible but unusual.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| White spotting | `SB1` and `W` only at 0.005 each | The standard permits restricted white but discourages excess white. These tiny rates allow occasional minimal facial or hind-leg white in a genetics system that requires an allele source, without turning pure Fell packs into pinto herds.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Tobiano / frame / splash | Forced clear | Piebald and skewbald ponies are Section X rather than the four-colour core standard; frame and splash are not signature Fell traits. Force clear in mainstream founders.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Roan / rabicano / leopard | Forced clear | These are not part of the breed’s current central visual standard. Outcrossing or deliberate project mechanics can introduce them later.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Cream / pearl / silver / dun / champagne | Forced clear | None is a defining or standard-recognized mainstream Fell colour. Pure founders should not spontaneously become buckskin, palomino, dun, silver dapple, champagne, or pearl.  [fellponysociety](https://www.fellponysociety.org/about_breed.htm) |
| Magical loci | Forced clear | The Fell Pony is an ordinary native British breed with no magical phenotype. |
| Speed | 4/10 | Fell Ponies are capable and enduring, but their defining role is secure, economical travel over difficult ground—not specialist racing speed.  [rbst.org](https://www.rbst.org.uk/watchlist-breed/fell-pony/) |
| Jump | 5/10 | A balanced baseline reflects useful athleticism and practical all-round riding ability without treating the Fell as a specialized jumping pony. |
| Health | 9/10 | This represents rugged soundness, hard feet, dense weather protection, and upland toughness; it does not negate the separately modeled FIS risk.  [rbst.org](https://www.rbst.org.uk/watchlist-breed/fell-pony/) |
| Size | ×0.82–0.91 | A compact 13–14-hand native pony should sit clearly below a typical riding horse while remaining substantial and strong.  [rbst.org](https://www.rbst.org.uk/watchlist-breed/fell-pony/) |
| Darkness band | `coat_darkness` 0.82–1.00 | Optional visual tuning so the black/brown-heavy foundation feels truly Fell-like rather than merely containing the `E` and `a` alleles. Do not use this if the project’s darkness epigenetic value affects unrelated pigment biology. |

## Disorder policy

The Fell Pony has one clearly documented breed-defining genetic disorder: **Foal Immunodeficiency Syndrome**. It is an autosomal recessive disease associated with a mutation identified in Fell and Dales ponies. A published sample of 214 Fell Ponies found 82 heterozygous carriers, giving a 38% carrier rate; UC Davis describes early test-era Fell carrier rates as almost 40%. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/)

The JSON deliberately uses `fis: 0.38` as an **allele-pool design approximation only if the mod’s founder system treats stated numbers as per-allele draws**. Be careful: a 38% *carrier prevalence* is not automatically equivalent to a 38% *allele frequency*. Under Hardy–Weinberg assumptions, a 38% heterozygote prevalence corresponds approximately to allele frequency \(q \approx 0.24\), because \(2q(1-q) \approx 0.38\). If the mod rolls a diploid genotype from independent allele frequencies, use approximately:

```json
"FIS_foal_immunodeficiency_syndrome": {
  "N": 0.76,
  "fis": 0.24
}
```

That alternative will generate roughly 36–37% carrier founders and approximately 5–6% affected homozygotes before any breeding controls, which is close to the historical population implications reported in the original research. If the game does not support affected founder generation or models lethal/affected outcomes differently, use the carrier-prevalence implementation appropriate to its `BreedFounder` logic. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/)

Every requested named disorder other than FIS is set clear because I did not find credible Fell-specific evidence supporting a nonzero population frequency for ACAN dwarfism, PLOD1-related Friesian dwarfism, MET, PRKDC-SCID, TOE1-CA, MYO5A-LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. That is an evidence-based omission, not a claim that no individual Fell or outcross-descended animal could ever carry another mutation.

## Code map

| Location | Change or verification |
|---|---|
| `common/breed/Breed` | Add the natural `fell_pony` record, display name, notes, spawn metadata, price, commonness, source availability, and genetic pools. |
| `common/breed/Breeds` | Register `fell_pony` so data loading, the H-menu, breed book, breeders, spawners, and saved genomes recognize the new breed. |
| `common/breed/BreedSource` | Confirm the four source strings map to the actual enum values for wild packs, cowboy sales, spawn eggs, and stable generation. |
| `common/breed/BreedBands` | Encode the optional dark-coat epigenetic band only if `coat_darkness` is a valid real band target. Otherwise leave this object empty. |
| `common/breed/spec/` | Add or validate serialization and deserialization for all fields; map proposed JSON names and FIS locus naming to actual schema names. |
| `common/breed/Commonness` | Confirm `UNCOMMON` corresponds to the intended spawn weight. Your stated ladder implies moderate is 6 and each step doubles; a defensible uncommon value is 3. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 5, health 9, and size ×0.82–0.91 to valid `TargetBand` data. |
| `common/breed/BreedFounder` | Ensure every omitted allele is forced wild type or clear, then roll only the specified E/e, A/a, grey, minute white-marking pools, and FIS locus for pure Fell founders. |
| `common/breed/BreedLineage` | Use normal pure/cross/mixed/Feral Mixed lineage behavior. No Fell-specific exception is needed. |
| `common/genetics/SpliceOutcome` | No special Fell logic; verify all alleles—including FIS, if implemented—can be transmitted through normal splice-carrot mechanics. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate the speed, jump, health, and size target-band definitions used by the breed. |
| `common/breed/BandType` | Use the project’s standard `TRADITIONAL` or `BACHELOR` handling only if required by the actual band implementation; no breed-specific special band type is inherently required. |
| `common/genetics/` disorder registry | If absent, add FIS as an autosomal recessive locus with carrier, affected, and any foal-health/death behavior the mod supports. The breed data file alone cannot simulate an unimplemented disorder. |

## Verification

1. **H-menu and book:** Open the H menu’s Breeds tab and the breed book. Confirm that **Fell Pony** appears as a natural breed with the stated biome list, uncommon commonness, all four sources, pony-scale stat profile, black-dominant description, and FIS warning.

2. **Pack integrity:** Generate eligible wild packs in windswept hills, windswept forest, groves, taiga, old-growth pine taiga, or meadow terrain. Every horse created in one breed-selected pack must show the same breed label: **Fell Pony**. Coat variation is expected, but breed identity should never split inside a generated pack.

3. **Feral baseline:** Generate or locate a lone wild horse outside the managed breed-pack generation path. It must read **Feral Mixed**, not Fell Pony, even if it happens to resemble a black Fell phenotypically.

4. **Founder phenotype sample:** Generate 100–200 Fell founders. Most should be black; a smaller group should be dark brown or bay; a few should grey with age if the mod represents grey progression. Chestnut should be exceptional, while cream, pearl, champagne, silver, dun, roan, tobiano, frame, splash, leopard complex, brindle, and magical phenotypes should not arise from the intended pure mainstream founder pool.

5. **Restricted-white test:** Examine a large founder sample. White-marked horses should be rare and modestly marked if your `SB1`/KIT system expresses minimal white at low frequency. If those loci routinely produce conspicuous pinto coats, set both to `N: 1.0`; the Fell standard discourages excess white and places piebald/skewbald animals outside the main four-colour standard. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)

6. **FIS test:** Generate a statistically useful founder group and inspect the health panel. Only FIS may be non-clear in a pure Fell founder; all named panel disorders must remain clear. Breed deliberate carrier-to-carrier pairs and confirm expected recessive inheritance—approximately 25% affected, 50% carrier, and 25% clear per foal over a large enough sample—subject to the mod’s specific conception and disease system.

7. **FIS safety test:** Breed a known FIS carrier to a clear Fell Pony repeatedly. No affected foal should occur; offspring should resolve approximately half clear and half carrier over a reasonable sample. This reflects the real conservation-breeding value of testing rather than indiscriminately removing healthy carriers from the population.

8. **Lineage test:** Confirm the default table:
   - Fell Pony × Fell Pony → **Fell Pony**
   - Fell Pony × another pure breed → appropriate named **cross**
   - Fell cross × pure Fell Pony → **cross**
   - Two different crosses → **Mixed**
   - Any breed or cross × **Feral Mixed** → **Mixed**

## Sources

- [Fell Pony Society — About Fell Ponies](https://www.fellponysociety.org/about_breed.htm): official breed-purpose and standard source for height, approved colours, white-marking policy, history of black predominance, and registration context. [fellponysociety](https://www.fellponysociety.org/about_breed.htm)
- [Fell Pony Society — Health](https://www.fellponysociety.org.uk/health.htm): official Society health page documenting availability of FIS carrier testing from 2010. [fellponysociety.org](https://www.fellponysociety.org.uk/health.htm)
- [Royal Society for the Protection of Rare Breeds — Fell Pony](https://www.rbst.org.uk/watchlist-breed/fell-pony/): native-breed history, Cumbrian origin, pack/farm/mining work, size, coat, winter coat, mane and tail, and 20th-century preservation context. [rbst.org](https://www.rbst.org.uk/watchlist-breed/fell-pony/)
- [Foal Immunodeficiency Syndrome mutation study, PLOS Genetics / PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/): primary research identifying the FIS-associated lesion and reporting 82 carriers among 214 Fell Pony samples, about 38%. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3131283/)
- [UC Davis Center for Equine Health — FIS](https://ceh.vetmed.ucdavis.edu/health-topics/foal-immunodeficiency-syndrome-fis): veterinary explanation of FIS and early reported carrier rates near 40% in Fell Ponies. [ceh.vetmed.ucdavis](https://ceh.vetmed.ucdavis.edu/health-topics/foal-immunodeficiency-syndrome-fis)
- [Fell Pony Society and Conservancy of the Americas — About the Fell Pony](https://fellponyconservancy.org/about/): North American conservation and breed-awareness context. [fellponyconservancy](https://fellponyconservancy.org/about/)