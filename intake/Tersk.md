The **Tersk** should be implemented as a rare, gray-dominant Russian light riding horse of Arabian type: elegant, tough, fast, and suited to extensive North Caucasus management, but larger and more substantial than a pure Arabian. Its founder pool should strongly favor the true progressive-gray allele while retaining bay and chestnut as genuine minority colors; loud white patterns, cream, dun, silver, leopard complex, and magical colors should be absent from pure founders. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

## Identity & flavour

The **Tersk horse**, also called the **Tersky**, is a Russian light riding breed of Arabian type developed at the **Tersk Stud** in Stavropol Krai in the North Caucasus. The stud was founded in 1921 under Semyon Budyonny during the rebuilding of Russia’s depleted horse population after the Revolution and civil-war era. Between about 1925 and 1940, breeders used surviving Strelets horses—an extinct Russian part-Arab breed with Orlov Trotter, Don, and Kabardin influence—alongside Arabian, Don, Kabardin, and later Thoroughbred-related stock. The new breed was officially recognized in **1948**. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

Tersks were made as elegant, practical military and riding horses: refined enough to resemble Arabians, but larger, stronger, and better able to thrive in the **taboon** system of extensive herd management in the North Caucasus. They were used for cavalry remount work, endurance travel, riding, racing, driving, circus performance, and general sport. Their heritage is therefore a blend of Arabian refinement and responsiveness with the durability, climate tolerance, and all-day usefulness expected from Russian saddle horses raised under comparatively open herd conditions. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

A Tersk typically stands around **157 cm for mares** and **160 cm for stallions**, about **15.2–15.3 hands**. It has a light, dry, expressive head with a straight profile, a long well-shaped neck, sloping shoulder, broad chest, straight back, rounded croup, high-set tail, and slender but strong legs. Its skin and hair are characteristically fine, as are the mane and tail. It should look like a larger, more linear Russian Arabian-type riding horse: refined but not fragile, with enough height and substance for a serious saddle mount. There should be no feathering, no cob-like mane mass, and no draft build. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

The breed is best known for **silvery gray** coats. This is true genetic gray, not silver dapple and not a pale cream dilution: gray horses are born on an underlying pigment base and progressively lose colored hairs as they age. Tersks may also be bay or chestnut, and dark colors occur, but a herd of pure founders should read overwhelmingly gray by adulthood. In this file, gray remains frequent but not fixed so the player can encounter the correct underlying bay, chestnut, and occasional black/dark base coats beneath it. Tobiano, frame, splash, sabino, dominant-white, roan, leopard, dun, champagne, silver, pearl, mushroom, and magical coats should not arise from pure Tersk founders. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

Tersks are intelligent, alert, willing, hardy, and athletic. Their profile in Procedural Horse Genetics should be a refined but durable distance and all-purpose riding horse: brisker and lighter than a stock horse, sturdier and taller than a typical Arabian, and much less specialized for jumping than an Oldenburg or Selle Français. Players should breed toward a high-tailed, silvery gray North Caucasus saddle horse that remains capable of producing bay and chestnut descendants when gray is absent. The mod does not model Arabian-type head refinement, high tail carriage, fine skin, breed-specific gait quality, taboon management, endurance conditioning, circus aptitude, Russian cavalry training, studbook inspection, pedigree composition, or the difference between a historic Strelets influence and a modern registered Tersk. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention from your provided example. Before compiling, reconcile exact field names, locus IDs, allele symbols, source enum values, rarity names, prices, and stat-band serialization with `common/breed/spec/`.
>
> **Scientific-rate caveat:** I found sound historical and phenotype evidence for a gray-dominant Tersk population, but no representative modern Tersk genotype survey providing precise `STX17` gray, Extension, or Agouti allele frequencies. The coat probabilities below are transparent **gameplay approximations**, not published breed frequencies. No Tersk-specific carrier-frequency study was located for the requested disorder panel. Arabian SCID, CA, and LFS rates must not be copied into Tersks without direct evidence, despite the breed’s Arabian foundation. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

```json
{
  "id": "tersk",
  "name": "Tersk",
  "type": "natural",
  "notes": "The Tersk is a Russian North Caucasus light riding horse of Arabian type, defined by Tersk Stud pedigree, Russian breeding history, silvery-gray presentation, fine skin and hair, high tail carriage, refined conformation, broad chest, extensive taboon management, endurance, and riding aptitude. Procedural Horse Genetics does not model Arabian refinement, exact head profile, fine skin, high tail set, breed-specific movement, taboon herd management, Russian cavalry use, racing or circus training, endurance conditioning, pedigree composition, studbook inspection, or individual rideability.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:river",
    "minecraft:grove"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 920,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.70,
      "a": 0.30
    },

    "grey": {
      "N": 0.35,
      "G": 0.65
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
    "speed": 7,
    "jump": 5,
    "health": 8,
    "size": [
      1.01,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Tersk × Tersk produces Tersk. Tersk × another pure breed produces a Tersk cross. A Tersk cross bred back to pure Tersk remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label an arbitrary silvery-gray Arabian-type horse as Tersk solely from phenotype: real Tersk identity is defined by its Russian studbook and breeding history."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed type | Gray-dominant Russian Arabian-type saddle horse | Tersks were bred at Tersk Stud from Strelets, Arabian, Don, Kabardin, and related material to create a larger Arab-type riding horse suited to extensive management.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| Extension | `E: 0.72 / e: 0.28` | Supports a majority black-pigment population with legitimate chestnut founders, consistent with gray, bay, chestnut, and dark colors being reported. Exact numbers are gameplay estimates.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| Agouti | `A: 0.70 / a: 0.30` | Produces primarily bay-based underlying colors, with a minority black base. This makes non-gray Tersks mostly bay with some chestnut and black/dark animals. Exact values are approximations.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| Gray | `G: 0.65` | Silvery gray is the iconic and common adult Tersk presentation. A 65% allele frequency yields approximately 87.8% gray phenotype under random pairing, leaving a real minority of non-gray bay, chestnut, and black horses. This is calibrated gameplay, not a published genetic survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| True gray, not silver | `grey` allowed; `silver: N 1.0` | Gray is progressive depigmentation associated with `STX17`, while silver is `PMEL` dilution of black pigment and is linked with MCOA. Tersk “silvery gray” should use true gray, not silver dapple.  [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141306002836) |
| Base-color modifiers | Cream, pearl, champagne, silver, mushroom, dun, flaxen forced wild type | These are not part of the standard documented Tersk palette. Their absence keeps pure founders centered on gray, bay, chestnut, and black/dark colors.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | No source located supports a pinto, roan, or conspicuously white-patterned pure Tersk population. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type spotting is not part of the documented breed phenotype. |
| Magical loci | All forced wild type | The breed’s “silvery” appearance derives from standard progressive gray, not magical pigment mechanics. |
| Disorders | All named loci clear | No credible Tersk-specific carrier-frequency survey was located for the requested disorders. Arabian disease frequencies cannot be treated as Tersk frequencies merely because Arabians were used in the breed’s formation.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/) |
| Speed | `7/10` | Captures a light riding horse selected for athletic travel, riding, racing, and endurance rather than raw sprint specialization.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| Jump | `5/10` | Tersks are versatile sport horses but not selected principally as elite jumpers. |
| Health | `8/10` | Represents adaptation to extensive taboon management, hardiness, and robust riding utility—not immunity from disease, injury, or genetic bottleneck effects.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |
| Size | `×1.01–1.10` | Centers the population around 157–160 cm, roughly 15.2–15.3 hands: modestly larger than many Arabians but not a tall warmblood.  [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse) |

## Disorder approach

All requested standalone disorder loci are deliberately **clear** in the pure Tersk founder pool.

This is not a claim that living Tersks cannot carry inherited disease. It is an evidence rule: no defensible Tersk-specific carrier rate was found for ACAN dwarfism, PLOD1/WFFS, `EDNRB`/frame-related lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

Arabian ancestry is insufficient to seed Arabian disease rates:

- The South African Arabian foal study found carrier prevalence of **3.4% SCID**, **5.1% CA**, and **11.7% LFS** in a particular Arabian sample—not in Tersks. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/)
- UC Davis notes that SCID frequency varies among Arabian populations and countries, estimated broadly at 1–8% for the mutation. [vgl.ucdavis](https://vgl.ucdavis.edu/test/scid)
- A Tersk is not genetically interchangeable with a pure Arabian. It has Strelets, Don, Kabardin, and other historical influence, and its modern population structure is distinct.

Therefore, `PRKDC_SCID`, `TOE1_CA`, and `MYO5A_LFS` remain clear unless a future direct Tersk-screening study supplies defensible rates.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `tersk` as a natural Russian breed record with North Caucasus flavour, gray-dominant coat pool, clear disorders, rare commonness, and medium light-riding-horse stat targets. |
| `common/breed/Breeds` | Register `tersk` so it resolves in spawning, cowboy/stable access, breed books, H-menu entries, saved genomes, commands, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. For strict real-world realism, `wild` could be omitted because Tersks are managed studbook horses; it remains for gameplay access in North Caucasus-like terrain. |
| `common/breed/BreedBands` | Allow the empty epigenetic-band object. Do not use a shade band to force silver-gray; gray must arise from the real `G` locus and progress normally with age. |
| `common/breed/spec/` | Verify actual key names, `STX17` gray representation, allele values, wild-type defaults, and correct schema formatting for all clear disorder loci. |
| `common/breed/Commonness` | Confirm that `RARE` maps to the intended rarity ladder and that `spawn_weight: 1.5` is legal if the code supports a direct numerical weight. |
| `common/breed/BreedStatCurve` | Translate speed 7, jump 5, health 8, and size ×1.01–1.10 into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Roll `E/e`, `A/a`, and especially `G` from the supplied pools; force all excluded coat loci and all named disorder loci wild type/clear; then assign the riding-horse stat targets. |
| `common/breed/BreedLineage` | Apply the default pure/cross/Mixed table. The engine must not infer Tersk status from a gray phenotype or Arabian-like model alone. |
| `common/genetics/SpliceOutcome` | No Tersk-specific exception. A splice-carrot allele inherits normally and can create a nonstandard descendant such as a cream, pinto, leopard, or disease-carrying Tersk cross. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the fast, durable, medium-size riding-horse target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the real spec requires a default entry. |

## Verification

1. Confirm **Tersk** appears in the H-menu’s Breeds tab and the breed book with the Russian/North Caucasus history, rare commonness, gray-dominant visual identity, Arabian-type conformation notes, and correct source checklist.

2. Spawn repeated eligible packs in plains, meadow, steppe-like hill, grove, river-valley, and dry grassland biomes. Every member of a selected Tersk pack should show the **Tersk** label.

3. Confirm that an ordinary lone wild horse remains **Feral Mixed**, even if it is gray, bay, chestnut, refined, or generated in a North Caucasus-like biome.

4. Generate at least 1,000 pure Tersk founders. About 85–90% should carry at least one true-gray allele and progressively gray with age; the remainder should be primarily bay and chestnut with a smaller black/dark component.

5. Confirm correct gray behavior:
- Gray foals must be born with an underlying bay, black, or chestnut coat and become progressively lighter as they age.
- A `G` horse must not be rendered as a silver dapple or chocolate horse.
- A non-gray `N/N` Tersk should retain its bay, chestnut, or black/dark adult base color.

6. Inspect founder genomes. Only Extension, Agouti, and gray should vary. Cream, pearl, champagne, silver, mushroom, dun, flaxen, every named white-pattern locus, roan, rabicano, leopard complex, PATN modifiers, brindle, magical loci, and every requested disorder locus must remain wild type or clear.

7. Confirm mature body performance: Tersks should trend medium-sized, fast, athletic, and hardy, with baseline-to-good jumping. They should not be as tall or jump-specialized as major European warmbloods, nor as small as conservation ponies.

8. Test default lineage:
- Tersk × Tersk → Tersk.
- Tersk × Arabian → Tersk cross.
- Tersk × Kabardin → Tersk cross.
- Tersk cross × pure Tersk → the existing Tersk cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test deliberate outcrossing or splice insertion. Introduce cream, silver, dun, tobiano, leopard complex, or any disease allele through normal mechanics. Offspring should inherit the new locus correctly but should no longer be presented as typical pure-founder Tersks in the breed-book description.

## Sources

- [Tersk horse overview](https://en.wikipedia.org/wiki/Tersk_horse): concise historical and phenotype synthesis covering 1921 Tersk Stud founding, Strelets foundation stock, 1925–1940 development, 1948 official recognition, North Caucasus distribution, taboon-management purpose, 157–160 cm height, conformation, and gray/bay/chestnut colors. [en.wikipedia](https://en.wikipedia.org/wiki/Tersk_horse)

- [Carrier prevalence of SCID, LFS, and CA in Arabian horses](https://pubmed.ncbi.nlm.nih.gov/24033554/): Arabian-only carrier estimates, used here specifically to explain why Arabian frequencies must not be transferred uncritically into the distinct Tersk population. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/24033554/)

- [UC Davis Veterinary Genetics Laboratory — SCID](https://vgl.ucdavis.edu/test/scid): disease explanation and population-variation context for Arabian SCID; supports clear founder policy absent direct Tersk data. [vgl.ucdavis](https://vgl.ucdavis.edu/test/scid)

- [Gray pattern in horses](https://www.sciencedirect.com/science/article/abs/pii/S1871141306002836): scientific genetics context distinguishing the dominant progressive gray pattern from base coat color. [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141306002836)