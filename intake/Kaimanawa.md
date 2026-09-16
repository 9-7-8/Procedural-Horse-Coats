The **Kaimanawa Horse** should be a rare New Zealand feral population rather than a closed pure breed: a compact, highly hardy, mostly bay/chestnut/black horse from the central North Island’s Kaimanawa ranges. Its best implementation is a two-strain founder pool—**southern gray** and **northern bay/chestnut**—reflecting evidence that the living Kaimanawa population contains geographically distinct subpopulations with different maternal ancestry and coat-color tendencies. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/)

> **Design note:** Kaimanawas are not genetically unique in the sense of being separate from domestic horses. New Zealand’s Department of Conservation describes them as mixed-breed feral horses with strong Thoroughbred and Station Hack similarity; genomic work identifies Welsh pony, Thoroughbred, and Arabian contributions, with later input from other domestic stock. They are still appropriate as a named mod population because the founder pool has lived ferally in a particular landscape for generations and is managed as the Kaimanawa herd. [doc.govt](https://www.doc.govt.nz/nature/pests-and-threats/animal-pests-and-threats/kaimanawa-horses/)

> **Schema caveat:** The Procedural Horse Genetics wiki could not be retrieved by the available reader. This JSON follows the readable proposed convention in your examples. Before compiling, match actual field names, probability semantics, strain syntax, gene IDs, biome IDs, source/commonness enums, and target-band fields to the live `common/breed/spec/` schema.

## Identity & flavour

The **Kaimanawa Horse**, also called the **Kaimanawa wild horse** or simply **Kaimanawa**, is a feral horse population from the Kaimanawa Ranges and surrounding central North Island high country of Aotearoa New Zealand. Its ancestors were domestic horses released or escaped during the nineteenth and twentieth centuries: early Welsh and Exmoor-type ponies, local farm and cavalry horses, and later contributions from Thoroughbreds, Arabians, Standardbreds, and Clydesdales. The core herd is usually traced to releases in the late nineteenth century, so the Kaimanawa identity formed over roughly a century and a half of unmanaged breeding under New Zealand’s volcanic high-country conditions. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/)

Kaimanawas were not deliberately made for one human job; they were shaped by feral survival. Their ancestry includes working, cavalry, pony, riding, and farm stock, but life on the **Desert Road**, volcanic plains, tussock country, scrub, and highland slopes selected for practical feet, thrift, herd awareness, tough constitutions, and a balanced all-purpose body. The modern horses are routinely mustered by helicopter because the population is managed to reduce grazing pressure on threatened native plants. That makes them unusual in the mod: they are a horse to find in the wild, tame patiently, and keep as a hardy explorer—not a finished breed-product bought from a stud farm. [doc.govt](https://www.doc.govt.nz/nature/pests-and-threats/animal-pests-and-threats/kaimanawa-horses/)

Kaimanawa adults stand approximately **133–151 cm (13.1–14.3 hands)** at the withers, with a historical measured mean of about **144.8 cm (14.1 hands)**. They are generally small, solidly built horses with sturdy pony influence, but their appearance varies by geographic band and ancestry. Many are compact and well muscled, with strong legs, durable feet, alert expressions, and a plain sensible head. Their mane and tail are ordinary feral-horse hair rather than a breed-specific genetic feature; their real visual signature is variation within a coherent wild high-country type—tough, compact, and ready to move. [newzealandecology](https://newzealandecology.org/system/files/articles/NZJEcol24_2_139.pdf)

Most Kaimanawas are bay, chestnut-brown, or black. Ecological-genetic summaries describe approximately **56% bay**, **26% chestnut-brown**, and **7.7% black**, with gray very rare and major white-pattern alleles apparently absent. Yet modern genetic research adds an important twist: all gray horses sampled in the Kaimanawa range belonged to one maternal haplogroup and occurred in southern zones, while chestnuts were concentrated in the northern population, suggesting two partially isolated subpopulations. This file models that evidence as a **1-in-4 Southern Gray strain** and a **3-in-4 Northern Plains strain**. The first gives you gray-masked but mostly dark-base feral horses; the second makes the familiar bay/chestnut high-country herd. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/)

The Kaimanawa’s history is inseparable from management conflict and conservation. The herd was hunted and reduced dramatically in earlier decades, then grew to numbers that threatened unique native flora; New Zealand’s Department of Conservation now manages the population around **300 horses** through periodic mustering. Genetic analysis of 96 horses found at least six maternal and six paternal lineages, but contemporary ancestry is unevenly distributed and mitochondrial diversity is lower than in several other feral populations. In Procedural Horse Genetics, Kaimanawas should feel like a living feral population worth preserving thoughtfully: not a magical “wild-horse relic,” not a standardized breed-book horse, but a tough, calm, inquisitive mixed-origin survivor whose rich ancestry becomes visible only when players breed and inspect it. The mod does not model social bands, helicopter musters, contraception, native-plant grazing impact, herd-range geography, taming history, exact ancestry percentages, altitude adaptation, or the legal and ethical questions around feral-horse management. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/)

## Breed JSON

```json
{
  "id": "kaimanawa",
  "name": "Kaimanawa",
  "type": "natural",
  "notes": "The Kaimanawa is a mixed-origin New Zealand feral horse population defined by free-ranging life in the central North Island high country, strong herd bonds, sure-footedness, thrift, calm inquisitiveness, and regional variation descended from pony, cavalry, farm, Thoroughbred, Arabian, Standardbred, and draught influences. Procedural Horse Genetics does not model social-band membership, helicopter mustering, fertility control, native-plant grazing impacts, Department of Conservation population limits, precise maternal/paternal haplogroups, geographic range bands, taming history, exact ancestry proportions, feral learned behavior, or modern adoption and welfare programs.",

  "biomes": [
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:stony_peaks",
    "minecraft:grove",
    "minecraft:taiga",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "terralith:highlands",
    "terralith:rocky_mountains",
    "terralith:shrubland"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "any",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 680,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.78,
      "a": 0.22
    },

    "grey": {
      "N": 0.97,
      "G": 0.03
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
    "speed": 6,
    "jump": 5,
    "health": 10,
    "size": [
      0.91,
      1.04
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "northern_bay_chestnut",
      "name": "Northern Bay-Chestnut",
      "weight": 3,
      "coat_genes": {
        "extension": {
          "E": 0.66,
          "e": 0.34
        },
        "agouti": {
          "A": 0.82,
          "a": 0.18
        },
        "grey": {
          "N": 1.0
        }
      }
    },
    {
      "id": "southern_gray",
      "name": "Southern Gray",
      "weight": 1,
      "coat_genes": {
        "extension": {
          "E": 0.85,
          "e": 0.15
        },
        "agouti": {
          "A": 0.67,
          "a": 0.33
        },
        "grey": {
          "N": 0.08,
          "G": 0.92
        }
      }
    }
  ]
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Two strains | 3 northern : 1 southern | The 2022 genetic study found evidence consistent with two geographically separated Kaimanawa subpopulations: a northern population dominated by bay and chestnut horses and a southern population in which gray horses were found. A 75%/25% split makes the northern type clearly dominant while preserving a meaningful southern-gray source line.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/) |
| Northern base colors | `E` 0.66 / `e` 0.34; `A` 0.82 / `a` 0.18 | Kaimanawas are predominantly bay and chestnut-brown, with chestnuts concentrated in the northern maternal group. The pool makes bay lead and chestnut a major minority; black remains possible but uncommon. These are phenotype-informed estimates rather than published MC1R/ASIP allele frequencies.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/) |
| Southern base colors | `E` 0.85 / `e` 0.15; `A` 0.67 / `a` 0.33 | Gray masks adult bases, so the southern pool retains a richer dark-base mix beneath a near-fixed gray allele. This allows gray horses to reveal bay or black ancestry through foal color and genotype screens. It is a gameplay interpretation, not a directly measured allele table.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/) |
| Gray | Southern `G` 0.92; northern `G` 0.0 | All seven sampled gray Kaimanawa horses carried maternal haplogroup P, and research describes gray horses as exclusively found in the southern zones. Keeping gray exclusive to a rare southern strain mirrors the reported geographic structure much better than a uniform small gray rate across all founders.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/) |
| Plain coat genome | Other coat loci forced wild type | Kaimanawa summaries describe a genome dominated by the black- and chestnut-pigment alleles, with gray rare, dominant white absent, tobiano/spotting likely absent, roan uncommon, and no prominent hue/dilution alleles. The conservative founder file therefore excludes cream, dun, champagne, silver, leopard complex, and large white-pattern systems.  [perissodactyla.wordpress](https://perissodactyla.wordpress.com/) |
| White markings | Major white loci clear | Facial and lower-leg white markings are common in field descriptions, but this does not establish tobiano, frame, splash, or W-series alleles. The mod should use ordinary marking rendering if available, rather than inventing major pinto loci.  [newzealandecology](https://newzealandecology.org/system/files/articles/NZJEcol24_2_139.pdf) |
| Disorders | All clear | No defensible Kaimanawa-specific carrier frequencies were located for the listed disorder panel. Despite their mixed domestic ancestry, no disease locus should be assigned merely from an ancestor breed. |
| Speed | 6/10 | Thoroughbred and Station Hack affinity, plus farm/cavalry ancestry, support useful speed. It remains below specialized racing stock because the population’s modern selection pressure is feral survival, not track performance.  [doc.govt](https://www.doc.govt.nz/nature/pests-and-threats/animal-pests-and-threats/kaimanawa-horses/) |
| Jump | 5/10 | High-country terrain and practical athletic ancestry justify a mid-range obstacle score without claiming organized jumping selection. |
| Health | 10/10 | Represents proven feral survival in harsh volcanic high country, not immunity from disease or a substitute for genetic testing.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/) |
| Size | ×0.91–1.04 | Captures a population that is generally pony-to-small-horse sized but variable because its ancestry includes both Welsh/Exmoor ponies and later farm/cavalry horses. Literal height remains in flavor only.  [newzealandecology](https://newzealandecology.org/system/files/articles/NZJEcol24_2_139.pdf) |

## Verification

1. Confirm **Kaimanawa** appears in the H-menu’s Breeds tab and the breed book with natural classification, rare commonness, wild availability, central-North-Island-style highland biome placement, price, and feral-population notes.

2. Spawn wild packs in an eligible windswept hill, gravelly hill, highland, rocky-mountain, plains, or shrubland biome. Every horse in a selected Kaimanawa pack should display **Kaimanawa** as its breed label.

3. Confirm a lone naturally generated horse outside a breed-selected pack reads **Feral Mixed**, not Kaimanawa.

4. Roll at least 2,000 founders and record their strain assignment. About 75% should use the **Northern Bay-Chestnut** pool and about 25% the **Southern Gray** pool, subject to normal random variation.

5. Verify strain effects:
   - Northern founders must be `g/g`; their visible population should be mainly bay and chestnut, with a small black minority.
   - Southern founders should be overwhelmingly gray, though their foals should reveal the underlying bay or black base before progressive graying takes over.
   - No northern founder should carry `G`.
   - No pure founder in either strain should carry dilution, major-white, leopard, roan, or magical alleles.

6. Inspect the disease panel across a large founder sample. All listed disorders must remain clear in both Kaimanawa strains.

7. Compare Kaimanawa stat tendencies with Welsh/Exmoor-like ponies, Thoroughbred-like saddle horses, and generic Feral Mixed horses. Kaimanawas should be compact, notably hardy, moderately fast, and versatile, but should not outclass dedicated racers, jumpers, or draught horses.

## Sources

- [Cappellini et al., *Animals* — “Reconstruction of the Major Maternal and Paternal Lineages in the Feral New Zealand Kaimanawa Horses”](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/): principal peer-reviewed source for nineteenth-/twentieth-century domestic origin, 96-horse sampling, six maternal and six paternal lineages, Welsh/Thoroughbred/Arabian contributions, low mitochondrial diversity, evidence for northern and southern subpopulations, and the specific gray/chestnut maternal-line pattern. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9774138/)

- [New Zealand Department of Conservation — Kaimanawa horses](https://www.doc.govt.nz/nature/pests-and-threats/animal-pests-and-threats/kaimanawa-horses/): official source stating that Kaimanawas are mixed-origin feral domestic horses rather than a genetically unique wild population, with strongest similarity to Thoroughbred and Station Hack horses; also provides official management context. [doc.govt](https://www.doc.govt.nz/nature/pests-and-threats/animal-pests-and-threats/kaimanawa-horses/)

- [Linklater et al., *New Zealand Journal of Ecology* — social and spatial structure/range use](https://newzealandecology.org/system/files/articles/NZJEcol24_2_139.pdf): peer-reviewed field source for 133–151 cm height range, predominantly bay phenotype with ordinary face/lower-leg white, late-nineteenth-century release history, Welsh/Exmoor and farm/cavalry ancestry, and stable social groups. [newzealandecology](https://newzealandecology.org/system/files/articles/NZJEcol24_2_139.pdf)

- [Kaimanawa Heritage Horses Welfare Society — history](https://kaimanawaheritagehorses.org/history/): population-management and heritage source for Exmoor/Welsh foundations, later Thoroughbred/Arabian/Standardbred/Clydesdale contributions, conservation-management context, approximate 300-horse target, geographical bands, general calm/inquisitive temperament, and adoption/muster history. [kaimanawaheritagehorses](https://kaimanawaheritagehorses.org/history/)