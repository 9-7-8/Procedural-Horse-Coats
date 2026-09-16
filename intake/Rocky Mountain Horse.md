The **Rocky Mountain Horse** should be a medium-sized Kentucky Appalachian gaited breed with a strict solid-color founder pool, high trail durability, and a notably common **silver** (`PMEL`) allele that creates the breed’s famous chocolate coat with flaxen mane and tail. That same allele is scientifically linked to **multiple congenital ocular anomalies** (MCOA), so this breed definition should treat silver as medically consequential—not as a harmless cosmetic color—and must exclude pinto, Appaloosa/leopard, and all-white founders under RMHA registration rules. [vgl.ucdavis](https://vgl.ucdavis.edu/breed/rocky-mountain-horse)

## Identity & flavour

The **Rocky Mountain Horse** is an American gaited saddle breed developed in the Appalachian foothills of eastern Kentucky, especially around the Rocky Mountain community in the late nineteenth and twentieth centuries. Despite its name, it is not a western Rocky Mountains breed. Its foundation story centers on a small, chocolate-colored gaited colt brought from the Rocky Mountain region of the American West to Kentucky around the turn of the twentieth century; descendants of that horse, later reinforced by local Kentucky mountain horses, became the versatile family and farm saddle horse now registered by the **Rocky Mountain Horse Association** (**RMHA**). [rmhorse](https://www.rmhorse.com/faqs/)

The breed was made for practical Appalachian life: carrying riders over steep forest trails, traveling between isolated farms and towns, working around livestock, pulling light vehicles, and remaining useful to several generations of one family. It is celebrated as “One Horse for All Occasions” because it combines an easy, tractable nature with sure-footedness, stamina, smooth movement, and enough substance for trail and pleasure work. A Rocky Mountain Horse should feel like a comfortable all-day trail partner rather than a high-speed racehorse, high-level jumper, heavy draft horse, or tightly collected show-gait specialist. [rmhorse](https://www.rmhorse.com/faqs/)

Rocky Mountain Horses must stand **14–16 hands** for RMHA registration and certification. They are medium-sized, balanced, and naturally substantial without bulk: a broad forehead, gently curved facial profile, well-shaped neck, sloping shoulder, short-to-medium strong back, deep heartgirth, rounded hindquarters, clean limbs, and hard, well-shaped feet. They carry a full mane and tail, often dark or flaxen depending on coat color, but have no true feathering. The breed should look like a sensible, compact American mountain saddle horse with enough bone for hills and enough refinement for pleasure riding. [rmhorse](https://www.rmhorse.com/faqs/)

The signature phenotype is **chocolate**: genetically a black-based silver horse, commonly called silver dapple or `Z`-diluted black, with a dark brown-to-chocolate body and a conspicuously pale flaxen or silver-white mane and tail. Rocky Mountain Horses can also be black, bay/brown, chestnut/sorrel, champagne, buckskin, palomino, dun, grullo/grulla, red dun, roan, gray, and several DNA-confirmed cream/silver combinations. Yet the registry admits only solid body colors. There are no registered Paint, pinto, Appaloosa, spotted, or all-white Rocky Mountain Horses, and white must remain limited to modest face markings and below-knee/below-hock leg markings. [rmhorse](https://www.rmhorse.com/ufaqs/what-colors-are-rocky-mountain-horses/)

The chocolate coat is beautiful but biologically important. The same `PMEL`—historically `PMEL17`—variant responsible for silver dilution is causally associated with **MCOA**, an inherited congenital eye syndrome. Heterozygous silver horses can show the milder cyst phenotype; homozygous `Z/Z` horses are at substantially greater risk of severe abnormalities such as enlarged or malformed corneas, iris defects, cataracts, retinal changes, and impaired vision. In this mod, the player should be able to recognize a Rocky Mountain Horse instantly: a 14–16-hand, sure-footed gaited trail horse, usually chocolate with flaxen hair or otherwise solid-colored, never pinto or leopard. The mod does not model the smooth single-foot gait, Appalachian trail sense, head nod, rider comfort, hoof quality, exact conformation, registry inspection, white-marking measurement, gait training, or MCOA eye anatomy unless the engine has a dedicated `PMEL` medical system. [rmhorse](https://www.rmhorse.com/multiple-congenital-ocular-anomalies-mcoa/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file follows the readable JSON format used in the provided example. Before compiling, reconcile exact record keys, locus identifiers, `PMEL`/silver medical linkage, allele symbols, source enums, pricing units, commonness values, and stat-band serialization with `common/breed/spec/`.
>
> **Scientific-rate caveat:** RMHA sources establish the allowed colors and strict solid-color policy, and peer-reviewed work establishes the silver–MCOA relationship. I did **not** locate a representative population-wide Rocky Mountain Horse genotype survey with reliable allele frequencies for Extension, Agouti, cream, dun, gray, roan, champagne, or silver. The founder rates below are therefore transparent **gameplay approximations**. `Z` is intentionally frequent because chocolate/silver is iconic, but the number is not claimed to be an RMHA census frequency. [rmhorse](https://www.rmhorse.com/ufaqs/what-colors-are-rocky-mountain-horses/)

```json
{
  "id": "rocky_mountain_horse",
  "name": "Rocky Mountain Horse",
  "type": "natural",
  "notes": "The Rocky Mountain Horse is defined by RMHA pedigree and certification, a natural smooth four-beat gait, Appalachian Kentucky trail and farm utility, 14-16 hand height, solid-color rules, modest marking limits, and practical family-horse temperament. Procedural Horse Genetics does not model the single-foot gait, gait timing, rider comfort, Appalachian trail judgment, head nod, hoof quality, exact head and body conformation, white-marking measurement, registry inspection, pedigree certification, training, or the ocular anatomy and clinical severity of MCOA unless the mod has dedicated PMEL-linked disease logic.",

  "biomes": [
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest",
    "minecraft:windswept_forest",
    "minecraft:windswept_hills",
    "minecraft:meadow",
    "minecraft:plains",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 880,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.54,
      "a": 0.46
    },

    "silver": {
      "N": 0.65,
      "Z": 0.35
    },
    "flaxen": {
      "N": 0.82,
      "f": 0.18
    },
    "grey": {
      "N": 0.92,
      "G": 0.08
    },
    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "champagne": {
      "N": 0.95,
      "Ch": 0.05
    },
    "dun": {
      "N": 0.94,
      "D": 0.06
    },
    "roan": {
      "N": 0.94,
      "Rn": 0.06
    },

    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },

    "tobiano": {
      "N": 1.0
    },
    "frame_overo": {
      "N": 1.0
    },
    "sabino_1": {
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
    "speed": 6,
    "jump": 4,
    "health": 8,
    "size": [
      0.98,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Rocky Mountain Horse × Rocky Mountain Horse produces Rocky Mountain Horse. Rocky Mountain Horse × another pure breed produces a Rocky Mountain Horse cross. A Rocky Mountain Horse cross bred back to pure Rocky Mountain Horse remains that cross under the default system. Different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label any chocolate silver gaited horse as a pure Rocky Mountain Horse from phenotype alone; real breed status requires RMHA-eligible lineage, registration, and certification."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed identity | Solid-color American gaited mountain horse | RMHA rules allow only solid body colors and explicitly exclude Paint, pinto, Appaloosa, spotted, and all-white horses. A chocolate coat alone does not create breed identity.  [rmhorse](https://www.rmhorse.com/ufaqs/what-colors-are-rocky-mountain-horses/) |
| Base colors | `E: 0.72 / e: 0.28`; `A: 0.54 / a: 0.46` | Supports black, bay/brown, and chestnut/sorrel foundations. A moderate `a` frequency is needed so the abundant silver allele often acts on black pigment and produces recognizable chocolate/silver-black horses. Exact values are gameplay approximations.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Silver | `Z: 0.35` | Silver-black/chocolate is the breed’s signature and must recur often in founders. `Z` dilutes black pigment, especially mane and tail, creating chocolate/silver-black and silver-bay outcomes. The value is deliberately gameplay-calibrated, not a published RMHA allele frequency.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/mcoa) |
| Flaxen | `f: 0.18` | Flaxen chestnut is accepted in the registry, and the iconic chocolate-flaxen look makes a modest rate visually useful. Genetically, cream/light mane on a silver black is caused primarily by `Z`, so `f` must not be used as a substitute for silver.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Gray | `G: 0.08` | Gray and gray-black/gray-red registry categories exist. Gray should be a minority because progressive graying conceals the famous chocolate phenotype over time.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Cream | `Cr: 0.10` | Buckskin, palomino, cremello, perlino, smoky cream, and silver buckskin categories are accepted, including some requiring DNA proof.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Champagne | `Ch: 0.05` | Champagne is explicitly listed among RMHA-accepted colors. It should remain uncommon in a general founder population.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Dun | `D: 0.06` | Dun, grullo/grulla, and red dun are accepted by the registry, making a minority dun pool appropriate.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Roan | `Rn: 0.06` | Black, bay, and red roan are specifically listed registration color categories.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Pinto/white-pattern loci | Tobiano, frame, sabino, splash, KIT white, rabicano all forced `N` | The registry’s solid-body-color rule excludes Paint, pinto, spotted, and all-white horses; its marking rules allow only restrained white on face and legs. The named loci would produce inappropriate body white or pinto outcomes in pure founders.  [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf) |
| Leopard complex | `LP`, `PATN1`, and `PATN2` forced `N` | Appaloosa, leopard, blanket, and other spotted horses are explicitly excluded from RMHA registration.  [rmhorse](https://www.rmhorse.com/ufaqs/what-colors-are-rocky-mountain-horses/) |
| Pearl and mushroom | Forced `N` | No registry evidence was found to justify including either in a baseline RMH founder pool. |
| Magical loci | All forced `N` | The chocolate phenotype is a real `PMEL` silver effect, not magical coloration. |
| MCOA | Derived from the silver/`PMEL` locus | The scientific literature and RMHA state that the same `PMEL` mutation causes silver/chocolate coat color and MCOA. MCOA must be linked to `Z` rather than generated as an independent disease locus.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2653074/) |
| Other named disorders | Forced clear | No defensible Rocky Mountain Horse-specific carrier frequency was found for ACAN dwarfism, PLOD1 dwarfism, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. UC Davis offers PSSM1 testing for RMHs, but test availability alone does not establish a population rate.  [vgl.ucdavis](https://vgl.ucdavis.edu/breed/rocky-mountain-horse) |
| Speed | `6/10` | Represents a practical, efficient trail gait and all-day utility, not elite sprint-racing speed. |
| Jump | `4/10` | Sure-footed trails and general athleticism are not the same as specialist jumping selection. |
| Health | `8/10` | Represents hardiness, sensible mountain-trail utility, and robust general use; it is not an assertion that the breed is free of MCOA, PSSM1, orthopedic issues, or ordinary injury. |
| Size | `×0.98–1.10` | Corresponds to RMHA’s strict 14–16-hand registration range.  [rmhorse](https://www.rmhorse.com/faqs/) |

## Disorder approach

The Rocky Mountain Horse’s crucial disease rule is **not** a separately randomized disease allele. It is a direct result of the same `PMEL` mutation used for the silver/chocolate coat.

### Silver and MCOA

The `Z` silver allele should drive both color and disease logic:

| `PMEL` / silver genotype | Coat implication | MCOA implication |
|---|---|---|
| `N/N` | No silver dilution | No silver-associated MCOA genotype |
| `N/Z` | Silver black/chocolate or silver bay, depending on base color | Often the milder cyst phenotype; affected horses can have ocular cysts and may otherwise appear normal |
| `Z/Z` | More strongly silver-diluted black pigment | Significantly higher risk of the severe MCOA syndrome, including multiple congenital eye abnormalities and possible visual impairment |

The RMHA describes MCOA as 100% caused by the `PMEL17` gene that also causes silver/chocolate coloration, with abnormalities more common and severe in `Z/Z` horses. Peer-reviewed Rocky Mountain Horse family data likewise showed heterozygotes associated with a cyst phenotype and homozygotes with the broader MCOA phenotype. [rmhorse](https://www.rmhorse.com/multiple-congenital-ocular-anomalies-mcoa/)

Therefore:

- Do **not** put MCOA under `PPIB`; `PPIB` is HERDA, not MCOA.
- Do **not** add a separate random MCOA rate if `silver` already maps to `PMEL`.
- Do **not** call silver merely cosmetic.
- If the mod has no MCOA engine, retain `Z` for coat realism but note that the health consequence is an engine limitation rather than inventing an unrelated disorder allele.

All requested standalone disorder genes remain clear because no breed-specific rates were available. `GYS1`/PSSM1 deserves monitoring if an actual RMH survey becomes available, but it should not be added merely because UC Davis sells a diagnostic test for the breed. [vgl.ucdavis](https://vgl.ucdavis.edu/breed/rocky-mountain-horse)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `rocky_mountain_horse` as a natural breed with Appalachian flavour, solid-color restrictions, gait notes, sources, coat pool, and medium trail-horse stat targets. |
| `common/breed/Breeds` | Register the ID for spawning, commands, breed books, the H-menu, saved-genome loading, and lineage display. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. For strict real-world realism, `wild` could be removed because RMHs are a registry-managed breed; it remains here for biome-based gameplay access. |
| `common/breed/BreedBands` | Support the empty epigenetic-band map. Do not use a band to fake a chocolate coat; chocolate must arise from black pigment plus `PMEL` silver. |
| `common/breed/spec/` | Verify the exact `PMEL`/silver locus name, `Z` allele notation, and whether the serializer supports coat-linked health effects. Also verify all forced-wild-type pattern keys. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the intended rarity ladder and preserve `spawn_weight: 3` if the implementation uses direct numeric weights. |
| `common/breed/BreedStatCurve` | Map speed 6, jump 4, health 8, and size ×0.98–1.10 to valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Roll only the allowed solid-color alleles; ensure all pinto, leopard, and body-white loci are wild type; then apply the medium-size, durable trail-horse stat targets. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A chocolate `Z` horse does not gain RMH lineage just by phenotype. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. A spliced pattern allele can produce a visually nonstandard descendant under normal inheritance, even though it would not meet RMHA color rules. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the moderate-speed, low-jump-specialization, high-heartiness, 14–16-hand target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual schema requires an explicit default. |

## Verification

1. Confirm **Rocky Mountain Horse** appears in the H-menu’s Breeds tab and the breed book with its Kentucky/Appalachian origin, solid-color rules, 14–16-hand size, gait explanation, and silver/MCOA warning.

2. Spawn repeated packs in Appalachian-like forest, woodland, river-valley, meadow, and hill biomes. Every horse in an individual selected pack must display the **Rocky Mountain Horse** label.

3. Confirm a lone wild horse still reads **Feral Mixed**, even if it happens to be chocolate-colored, gaited, compact, or standing in a forested hill biome.

4. Generate at least 500 pure founders. The sample should include many solid black, bay/brown, chestnut, chocolate/silver-black, and silver-bay outcomes; smaller numbers of gray, palomino, buckskin, champagne, dun, grullo, red dun, and roan founders. It must never produce pinto, tobiano, frame, splash, sabino body spotting, Appaloosa/leopard, all-white, brindle, or magical phenotypes.

5. Confirm the genetics behind chocolate:
- A black-based `E_ aa Z_` horse should render as silver black/chocolate, generally with a pale or flaxen-looking mane and tail.
- A bay-based `E_ A_ Z_` horse should render as silver bay/red chocolate rather than generic chocolate.
- A chestnut `e/e` horse may carry `Z` genetically but should show little or no visible silver dilution because silver acts primarily on black pigment.
- Flaxen chestnut must not be mistaken for silver black/chocolate.

6. Test `PMEL`/MCOA health linkage if supported:
- `N/Z × N/N` should transmit `Z` to about half of offspring.
- `N/Z × N/Z` should yield approximately 25% `Z/Z`, 50% `N/Z`, and 25% `N/N` over a large sample.
- Confirm `Z/Z` foals receive the mod’s stronger MCOA-risk/affected state and `N/Z` foals receive the milder cyst-associated state, without an independent duplicate disease roll.

7. Confirm all unrelated disorder loci stay clear in a large pure-founder sample. No ACAN dwarfism, PLOD1 dwarfism, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA allele should originate in this definition without later evidence.

8. Check stats on mature horses. They should trend toward medium riding-horse size, good practical travel speed, high durability, and modest jumping. They should not become racehorse-fast, specialist-jumper-capable, or draft-sized.

9. Test default lineage:
- Rocky Mountain Horse × Rocky Mountain Horse → Rocky Mountain Horse.
- Rocky Mountain Horse × Kentucky Mountain Saddle Horse → Rocky Mountain Horse cross.
- Rocky Mountain Horse × American Saddlebred → Rocky Mountain Horse cross.
- Rocky Mountain Horse cross × pure Rocky Mountain Horse → the existing Rocky Mountain Horse cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

## Sources

- [Rocky Mountain Horse Association — FAQ](https://www.rmhorse.com/faqs/): official 14–16-hand registration range, solid-body-color requirement, and explicit exclusion of Paint, pinto, Appaloosa, spotted, and all-white horses. [rmhorse](https://www.rmhorse.com/faqs/)

- [Rocky Mountain Horse Association — Rules for Examiners](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf): official accepted color categories, modest facial-marking rules, leg-white limits, DNA-proof requirements for certain dilute colors, and solid-body restrictions. [rmhorse](https://www.rmhorse.com/wp-content/uploads/2024/03/EXAMINERS-MANUAL-FINAL-3.18.24-corrected.pdf)

- [Rocky Mountain Horse Association — Rules of Registry](https://www.rmhorse.com/wp-content/uploads/2021/06/Rules-of-Registry-Revised-June-2021.pdf): registration color list, certification requirements, and white-marking exclusions. [rmhorse](https://www.rmhorse.com/wp-content/uploads/2021/06/Rules-of-Registry-Revised-June-2021.pdf)

- [Rocky Mountain Horse Association — MCOA](https://www.rmhorse.com/multiple-congenital-ocular-anomalies-mcoa/): official breed-association explanation that `PMEL17` causes both silver/chocolate coloration and MCOA, with more severe effects in `Z/Z` horses. [rmhorse](https://www.rmhorse.com/multiple-congenital-ocular-anomalies-mcoa/)

- [UC Davis Veterinary Genetics Laboratory — MCOA](https://vgl.ucdavis.edu/test/mcoa): `PMEL` silver/MCOA testing, phenotypic description, allele interpretation, and breeding rationale to avoid homozygous silver horses. [vgl.ucdavis](https://vgl.ucdavis.edu/test/mcoa)

- [Equine Multiple Congenital Ocular Anomalies maps to a 4.9 megabase interval on horse chromosome 6](https://pmc.ncbi.nlm.nih.gov/articles/PMC2653074/): peer-reviewed Rocky Mountain Horse family study showing association between silver `PMEL17`, heterozygous cyst phenotype, and more severe homozygous MCOA. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC2653074/)