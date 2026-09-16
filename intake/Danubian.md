The **Danubian Horse** should be implemented as a rare Bulgarian **light-draught and working carriage breed**, descended chiefly from Nonius horses but deliberately diversified through six paternal lines. It should look sturdier and darker than a sport warmblood, but lighter and more mobile than a giant draught: a medium-large, mostly bay, brown, black, and chestnut work horse with high health, modest speed, moderate jumping, and no flashy pattern or dilution genes in pure founders. The breed was created at the Klementina Stud near Pleven for the Bulgarian Army and to improve local working horses; it was officially recognized in **1951**. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

## Identity & flavour

The **Danubian Horse**, Bulgarian *Dunavski kon* (Дунавски кон), is one of Bulgaria’s modern national horse breeds, alongside the Pleven and Eastern Bulgarian horses. It was developed in northern Bulgaria around the former **Klementina Stud** in the village of Pobeda, near Pleven, to provide a reliable horse for the Bulgarian Army and to improve the working capacity of local mares. The breed was officially recognized in **1951**, making it a twentieth-century national work-horse program rather than a medieval landrace preserved unchanged through time. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

The Danubian was bred for **light draught**, carriage, agricultural work, riding, and practical transport. Its core foundation was Hungarian Nonius stock imported from Mezőhegyes, later supplemented by Nonius from Yugoslavia and Czechoslovakia. The result was a horse with a strong working body, steady temperament, and enough forward motion for farm roads and carriage work. It should feel like the middle ground between the Hungarian Nonius and a modern sport horse: strong, durable, and harness-minded, but not an enormous heavy draught animal and not a specialist harness-racing trotter. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

A Danubian is generally a medium-large horse, approximately **155–165 cm** at the withers, or about **15.1–16.1 hands**. It should have a plain but expressive head, straight to slightly convex profile, a medium-to-long muscular neck, developed withers, broad deep chest, long back, firm loins, muscular sloping croup, strong hindquarters, and clean but substantial legs. Its hooves should feel practical and durable for roads, farms, and harness work. Mane and tail are normally full but ordinary, with no special genetic feathering, curl, or mane-color trait. At a glance, the Danubian should look like a broad, capable north-Bulgarian work and driving horse rather than a small mountain pony or refined Olympic warmblood. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

The Danubian is not a color breed, but a conservative pure population should stay mostly solid and dark. Its Nonius foundation supports bay, dark bay/brown, black, and chestnut, with bay/brown likely the most useful general population center. No retrieved Danubian source established a modern coat-locus frequency table, so this file deliberately avoids pretending to know precise national percentages. Gray, cream, dun, champagne, silver, pearl, mushroom, roan, tobiano, frame overo, splash, leopard complex, and dramatic body-white patterns are therefore excluded from pure founders. The visual signature is utility: dark solid coat, strong frame, and calm carriage-horse presence. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

The Danubian horse’s deeper genetic story is unusually relevant to the mod. A 2022 study of **166 horses** across six paternal lines—**Zdravko, Nonius XVII-30, Torpedo, Lider, Kalifa, and Hrabar**—found high genetic diversity overall, with mean expected heterozygosity around **0.84** and low average inbreeding. That means the breed should not be modeled as a single clone-like black strain despite its Nonius ancestry. In Procedural Horse Genetics, players should breed toward a powerful, solid-colored Bulgarian work horse whose type is steady and recognizable while individuals retain honest variation in base color and size. The mod does not model line-specific pedigree, harness action, pulling force, army-remount suitability, agricultural efficiency, leg bone circumference, hoof strength, population genetic diversity, or the distinction between the six paternal lines beyond optional flavour. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the example you provided. Before compilation, reconcile exact field names, source enums, Extension/Agouti allele labels, rarity values, price units, and target-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The primary Danubian genetic study supplies population-structure data, six sire-line identities, and evidence of high diversity—not `MC1R`, `ASIP`, gray, or dilution allele frequencies. The base-color values below are therefore transparent **gameplay approximations** selected to create a broad but dark-solid Nonius-derived work-horse population. No Danubian-specific carrier frequency was found for the requested disorders, so all named disorder loci are clear. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

```json
{
  "id": "danubian",
  "name": "Danubian",
  "type": "natural",
  "notes": "The Danubian Horse, Dunavski kon, is a Bulgarian light-draught, agricultural, carriage, and riding horse developed at Klementina Stud near Pleven from local mares and chiefly Nonius stock. Its real identity includes the Zdravko, Nonius XVII-30, Torpedo, Lider, Kalifa, and Hrabar paternal lines; substantial working conformation; harness action; pulling strength; military-remount history; hoof durability; farm temperament; and population-level genetic diversity. Procedural Horse Genetics does not model those traits directly.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:grove",
    "minecraft:windswept_hills"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 920,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.74,
      "e": 0.26
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "grey": {
      "N": 1.0
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
      "N": 0.97,
      "f": 0.03
    },

    "tobiano": {
      "N": 1.0
    },
    "sabino_1": {
      "N": 0.99,
      "SB1": 0.01
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
      "N": 0.99,
      "Rb": 0.01
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
    "jump": 4,
    "health": 8,
    "size": [
      1.04,
      1.16
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Danubian × Danubian produces Danubian. Danubian × another pure breed produces a Danubian cross. A Danubian cross bred back to a pure Danubian remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Zdravko, Nonius XVII-30, Torpedo, Lider, Kalifa, and Hrabar are paternal-line identities within the Danubian breed, not distinct cross labels."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Bulgarian Nonius-derived light draught | The Danubian was created at Klementina Stud to meet Bulgarian Army needs and improve local working horses, using imported Nonius foundation stock and later additional Nonius imports.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/) |
| Genetic diversity | Broad base-color pool, no artificial one-color fixation | The 2022 survey of 166 Danubians found high diversity across six male lineages, with mean heterozygosity around 0.84 and low inbreeding. This supports a varied dark-solid founder population instead of cloning the black-heavy Nonius phenotype.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/) |
| Base colors | `E: 0.74 / e: 0.26`; `A: 0.68 / a: 0.32` | These gameplay values produce mostly bay/dark bay/brown, plus black and chestnut minorities. They reflect Nonius-derived working-horse ancestry while avoiding the false claim that Danubian allele frequencies are published.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC13353399/) |
| Flaxen | `f: 0.03` | A tiny optional modifier permits occasional light-maned chestnut without making it a breed signature. This is an implementation estimate, not published Danubian genotype data. |
| Minor ordinary markings | `SB1: 0.01`; `Rb: 0.01` | These values are deliberately close to absent. They allow minimal white/hair variation only if the renderer handles both conservatively; remove them if either produces visually obvious pinto expression. |
| Gray and dilutions | Gray, cream, pearl, champagne, silver, mushroom, and dun forced wild type | No retrieved Danubian source supports seeding these genes in the baseline pure founder population. An open or broad working type is not evidence that every European horse color belongs at baseline. |
| Pinto and leopard exclusion | Tobiano, frame, splash, KIT white, roan, leopard complex, PATN, brindle forced wild type | The desired breed phenotype is a solid Bulgarian work/drive horse. No direct breed evidence justified patterned-founder alleles. |
| Disorders | All named loci clear | No Danubian-specific carrier-frequency survey was located for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| PSSM1 caution | Do not infer from Nonius or working type | `GYS1` PSSM1 is dominant and varies greatly by breed. Neither heavy bone, Nonius ancestry, nor agricultural purpose establishes a Danubian allele frequency. |
| Speed | `5/10` | The breed must travel effectively in harness and under saddle, but it is not a purpose-bred trotter or racehorse. |
| Jump | `4/10` | It should manage ordinary terrain and use, but has no documented selection for modern jumping scope. |
| Health | `8/10` | Represents working durability, broad genetic diversity, and low observed inbreeding in the sampled lines—not immunity from injury, disease, or poor management.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/) |
| Size | `×1.04–1.16` | Produces a medium-large light-draught horse, substantially larger than a mountain pony but well below giant-draft scale. |

## Disorder approach

Every named disorder locus is **clear** in the pure Danubian founder pool.

The source-backed genetic research concerns **microsatellite diversity and sire-line population structure**, not clinical mutation carrier frequencies. It cannot be repurposed to claim that Danubians do or do not carry a named disease mutation. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

Therefore, do not seed:

- `ACAN` dwarfism.
- `PLOD1` / Warmblood Fragile Foal Syndrome.
- `MET` / `EDNRB` frame-related lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

The Danubian’s Nonius background does not justify copying Nonius disease assumptions, and its status as a Bulgarian working breed does not justify copying generic draught-horse PSSM1 estimates. If a direct Bulgarian Danubian health-panel survey becomes available, add only the allele measured and convert carrier frequency to founder allele frequency correctly.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `danubian` as a natural Bulgarian light-draught record with Pleven/Klementina history, managed sources, dark solid founder pool, clear disorder policy, and medium-heavy body targets. |
| `common/breed/Breeds` | Register `danubian` for cowboy and stable generation, spawn eggs, breed books, the H-menu, commands, lineage labels, genome serialization, and save loading. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because Danubian is a managed Bulgarian working breed, not a natural feral population. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map. Do not create artificial bands for harness action, traction, calmness, or line-specific quality. |
| `common/breed/spec/` | Reconcile illustrative field names with the live parser/writer. Verify exact `E/e`, `A/a`, flaxen, sabino, and rabicano locus syntax, direct probability parsing, and wild-type defaults. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder and that `spawn_weight: 1.5` is valid if the system supports direct numerical weighting. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 4, health 8, and size ×1.04–1.16 to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only declared base-color and tiny modifier pools; force all dilution, pinto, leopard, magical, and named disease loci wild type or clear; then apply medium-heavy work-horse stat targets. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A dark, broad, Nonius-like horse must not gain Danubian lineage merely from phenotype, body stats, or Bulgarian-looking terrain. |
| `common/genetics/SpliceOutcome` | No exception. Splice-carrot alleles transmit normally and can create nonstandard Danubian descendants with cream, pinto, leopard, disease, or sport-horse traits excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize medium-large size, practical travel speed, modest jumping, and good working health targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is necessary unless the actual schema requires an explicit default field. |

## Verification

1. Confirm **Danubian** appears in the H-menu’s Breeds tab and the breed book with Bulgarian origin, 1951 recognition, Klementina/Pleven history, Nonius foundation, six paternal-line flavour, rare commonness, and managed-only source list.

2. Confirm Danubians do **not** generate in ordinary wild packs because `wild` is absent. They should appear through cowboy, stable, and spawn-egg systems only.

3. Confirm a lone ordinary wild horse remains **Feral Mixed**, even if it is bay, brown, black, medium-large, broad-bodied, or generated in plains terrain.

4. Generate at least 1,000 pure founders. The cohort should be dominated by bay, dark bay/brown, and black, with a clear but smaller chestnut population. It must never generate gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, roan, tobiano, frame, splash, dominant white, leopard complex, brindle, or magical coats.

5. Inspect founder genomes:
- Only Extension, Agouti, low flaxen, and the near-absent marking modifiers can vary.
- `E/e` and `A/a` should generate a diverse but dark solid-color population.
- All major dilution, pinto, leopard, magical, and disorder loci must be wild type or clear.

6. Check that high diversity is represented honestly:
- The breed should not be phenotypically cloned.
- Base color and body-size outcomes should vary within the intended range.
- Every founder should still retain the same `Danubian` breed label.
- Do not try to label Zdravko, Nonius XVII-30, Torpedo, Lider, Kalifa, or Hrabar from coat color; they are real paternal genealogical lines, not coat strains.

7. Confirm mature stats. Danubians should be medium-large, stronger and slower than normal saddle horses, moderately capable of ordinary terrain jumping, and robust enough for sustained farm/carriage use. They should not become a fast trotter, giant draught horse, or high-level show jumper.

8. Confirm all named disorder loci remain clear across a large founder sample. No PSSM1, WFFS, HYPP, SCID, CA, LFS, GBED, HERDA, frame lethal-white, or other listed mutation should originate from pure Danubian founders.

9. Test default lineage:
- Danubian × Danubian → Danubian.
- Danubian × Nonius → Danubian cross.
- Danubian × Bulgarian Warmblood / Pleven-type horse → Danubian cross.
- Danubian cross × pure Danubian → the existing Danubian cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add gray, cream, dun, tobiano, leopard complex, draft size, racing speed, or any named disease allele. Descendants should inherit normally but must not gain pure Danubian status merely because they remain dark, sturdy, or medium-large.

## Sources

- [Genetic Diversity and Structure of the Main Danubian Horse Paternal Genealogical Lineages Based on Microsatellite Genotyping](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/): primary peer-reviewed source for Danubian status as a modern Bulgarian breed; Klementina/Pobeda/Pleven origin; Bulgarian Army and local-work-horse purpose; 1951 recognition; Nonius foundation; six paternal lines; 166-horse sample; high diversity; and low inbreeding. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9322366/)

- [Veterinary Sciences—Danubian Horse genetic diversity study](https://www.mdpi.com/2306-7381/9/7/333): publisher record for the same primary study, including the six paternal lines and genetic-diversity results. [mdpi](https://www.mdpi.com/2306-7381/9/7/333)

- [Danubian population-structure summary](https://ouci.dntb.gov.ua/en/works/l1yvP2Ol/): supplementary summary of the 166-horse, 15-STR study and the high-diversity/low-differentiation conclusion. [ouci.dntb.gov](https://ouci.dntb.gov.ua/en/works/l1yvP2Ol/)