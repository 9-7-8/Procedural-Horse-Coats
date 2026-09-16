The **Yugoslavian Trotter**—Serbian **Jugoslovenski kasač**—should be modeled as a rare Balkan harness-racing horse: compact, strong, durable, and noticeably faster than a farm horse, but not as tall or refined as an American Standardbred. It is a Serbian/Yugoslav trotting population derived primarily from Orlov Trotter, Anglo-Arabian, and Standardbred ancestry, so pure founders should be solid bay, bay-brown, chestnut, brown, or black, with gray rare and all pinto/leopard/dilution loci absent. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

## Identity & flavour

The **Yugoslavian Trotter**, Serbian *Jugoslovenski kasač* (Југословенски касач), is a Balkan harness horse developed in the former Yugoslavia and maintained principally in Serbia. It is a modern trotting breed rather than an ancient local landrace. Breeders developed it through crosses involving **Orlov Trotters**, **Anglo-Arabians**, and later **American Standardbred** blood, seeking a horse that could combine durable regional adaptation and practical body substance with a clean, fast, sustained harness trot. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

The breed was made for **harness racing** and practical driving. It should travel at a disciplined, efficient trot with speed and endurance, pull a sulky or light carriage, stay sound through repetitive work, and retain enough strength for ordinary saddle or farm use. Unlike a pure Standardbred, the Yugoslavian Trotter historically remained a more general-purpose Balkan horse, shaped by local management as well as performance racing. In game terms, it should feel fast for long overland travel and exceptionally useful in harness-themed play, without becoming a top-level sport jumper or a giant heavy draught. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

Yugoslavian Trotters are generally **145–152 cm**, about **14.1–15 hands**, with stallions averaging around 153 cm. They have a medium-sized, practical warmblood frame: a dry straight-profiled head, long neck, pronounced withers, deep chest, long shoulder, strong back, muscular loin, long sloping croup, sound legs, and hard hooves. They should be leaner and more mobile than a Nonius or Croatian Coldblood, but more solid and compact than some long-lined Standardbreds. Mane and tail are normal and full; feathering is absent or minimal. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

The core colors are **bay, bay-brown, light chestnut, brown, and black**, while gray is explicitly rare. That supports a focused solid founder pool: bay and dark bay/brown should be common, chestnut common enough to matter, black a smaller outcome, and true progressive gray a very low-frequency surprise. It is not a color breed. There is no source basis for inserting cream, dun, champagne, silver, pearl, mushroom, roan, tobiano, frame, splash, dominant white, leopard complex, or magical colors into baseline pure founders. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

Yugoslavian Trotters should feel energetic, willing, tough, and tractable under sustained driving work. The breed belongs in Procedural Horse Genetics because it offers a clean mechanical niche: a compact-to-medium trotting specialist with strong speed, solid health, modest jumping, and a practical dark European harness-horse appearance. The mod does not model trotting action, stride length, sulky pulling, race time, gait faults, harness training, Standardbred/Orlov pedigree percentage, track conditioning, driver skill, or the difference between a quick local trotter and an elite international racehorse. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied earlier could not be retrieved by the documentation fetcher. This JSON follows the readable convention in your example. Before compiling, reconcile exact field names, source enums, locus identifiers, allele names, commonness values, price units, and body-stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The FAO breed record provides height and a clear phenotype list—bay, bay-brown, light chestnut, brown, black, with gray rare—but no published `MC1R`, `ASIP`, or `STX17` frequency table was found. The numeric pools below are transparent **gameplay approximations**, not Serbian studbook allele frequencies. No Yugoslavian Trotter-specific carrier-frequency survey was located for the requested disease panel. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

```json
{
  "id": "yugoslavian_trotter",
  "name": "Yugoslavian Trotter",
  "type": "natural",
  "notes": "The Yugoslavian Trotter, Jugoslovenski kasač, is a Serbian and former-Yugoslav harness-racing and driving horse developed with Orlov Trotter, Anglo-Arabian, Standardbred, and regional horse influence. Its real identity includes trotting action, gait regularity, stride length, sulky speed, harness training, racing condition, driver skill, track surface, regional breeding lines, pedigree percentages, hoof quality, and the distinction between a utility trotter and an elite racing performer. Procedural Horse Genetics does not model those traits directly.",

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
  "price": 940,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "At": 0.30,
      "A": 0.48,
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
    "speed": 8,
    "jump": 4,
    "health": 7,
    "size": [
      0.98,
      1.08
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Yugoslavian Trotter × Yugoslavian Trotter produces Yugoslavian Trotter. Yugoslavian Trotter × another pure breed produces a Yugoslavian Trotter cross. A Yugoslavian Trotter cross bred back to pure Yugoslavian Trotter remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label every bay Standardbred-like horse as Yugoslavian Trotter: real identity depends on Balkan trotting-breed lineage and registry context, not speed, color, or build alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Compact Balkan harness trotter | The FAO breed record identifies a 145–152 cm trotting population with a practical medium-horse build rather than a giant cold-blood or a pure racing Standardbred.  [fao](https://www.fao.org/4/ah759e/ah759e13.htm) |
| Base-color pool | `E: 0.70 / e: 0.30`; `At: 0.30 / A: 0.48 / a: 0.22` | Generates bay, bay-brown/dark bay, brown, black, and light chestnut. These categories match the FAO phenotype list. The rates are gameplay estimates, not published Serbian genotype frequencies.  [fao](https://www.fao.org/4/ah759e/ah759e13.htm) |
| Dark bay distinction | `At` optional | The FAO record distinguishes bay from bay-brown and brown. If the mod supports a dark-bay or seal-brown Agouti state, `At` helps express that distinction. If it supports only `A/a`, use the compatibility fallback below. |
| Gray | `G: 0.03` | Gray is explicitly described as rare. A 3% allele rate makes progressive-gray horses unusual but possible.  [fao](https://www.fao.org/4/ah759e/ah759e13.htm) |
| Flaxen | `f: 0.03` | A tiny flaxen rate permits occasional light-maned chestnut but does not make it a breed hallmark. This is a gameplay approximation. |
| Excluded dilutions | Cream, pearl, champagne, silver, mushroom, and dun forced wild type | No direct Yugoslavian Trotter evidence supports adding these to baseline pure founders. |
| White patterns | Tobiano, frame, splash, KIT white, roan, leopard complex, PATN, brindle forced wild type | The breed is modeled as solid. Very low Sabino 1/rabicano values can be removed if the renderer makes them visually conspicuous; they are only placeholders for modest ordinary marking texture. |
| Disorders | All named loci clear | No Yugoslavian Trotter-specific carrier or allele frequency was found for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `8/10` | Harness-racing and trotting selection justifies a strong speed score. It should be fast in sustained travel without matching a Thoroughbred’s specialized flat-race profile. |
| Jump | `4/10` | It is a driving specialist, not a show-jumping breed. |
| Health | `7/10` | Represents a sound, durable performance horse, while avoiding the claim that speed selection or regional hardiness confers exceptional disease resistance. |
| Size | `×0.98–1.08` | Matches the documented 145–152 cm range, roughly 14.1–15 hands.  [fao](https://www.fao.org/4/ah759e/ah759e13.htm) |

### Two-allele Agouti fallback

If the mod has only `A/a`, use:

```json
"agouti": {
  "A": 0.78,
  "a": 0.22
}
```

This will preserve a bay-heavy population with black minorities, but it will not visually separate bay, bay-brown, and brown as well as a model that supports `At` or an independent shade/sooty system.

## Disorder approach

Every named disorder locus is **clear** in pure Yugoslavian Trotter founders.

This is a strict evidence standard. The breed has documented historical contributions from Orlov Trotter, Anglo-Arabian, Standardbred, and local Balkan stock, but breed ancestry cannot be substituted for carrier-frequency data. No direct Yugoslavian Trotter screening estimate was found for:

- `ACAN` dwarfism.
- `PLOD1` / WFFS.
- `MET` / `EDNRB` lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

Do not insert PSSM1 merely because the breed is a trotter. `GYS1` PSSM1 is a dominant mutation, and a fast harness breed needs a direct genetic survey before being assigned a positive founder frequency. [madbarn](https://madbarn.com/type-1-pssm-in-horses/)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `yugoslavian_trotter` as a natural Serbian/Balkan harness-horse record with managed sources, bay/chestnut/dark solid genetics, clear disease policy, and high-speed trotter stats. |
| `common/breed/Breeds` | Register the ID for cowboy and stable generation, spawn eggs, H-menu display, breed books, command lookup, genome serialization, and lineage labeling. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is omitted because this is a managed racing and driving breed, not a natural feral population. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map. Do not use a coat band to fake trotting stride, harness action, muscle, or track conditioning. |
| `common/breed/spec/` | Verify actual syntax for `At`/dark bay, `G`, flaxen, minor marking modifiers, direct probability pools, and all forced-wild-type defaults. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder and retain numerical `spawn_weight: 1.5` only if direct weights are valid. |
| `common/breed/BreedStatCurve` | Convert speed 8, jump 4, health 7, and size ×0.98–1.08 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll the compact trotter base-color pool; force all dilution, pinto, leopard, magical, and disease loci clear; apply high-speed, modest-jump, medium-size targets. |
| `common/breed/BreedLineage` | Use the default pure/cross/Mixed table. A quick bay harness horse does not gain Yugoslavian Trotter lineage from phenotype, speed, or a driving animation. |
| `common/genetics/SpliceOutcome` | No special exception. A splice-carrot allele may transmit normally and create a nonstandard descendant with cream, pinto, leopard, disease, or other excluded traits. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the medium-size, high-speed, modest-jump, moderate-health trotting-horse targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the real schema mandates an explicit default. |

## Verification

1. Confirm **Yugoslavian Trotter** appears in the H-menu’s Breeds tab and breed book with the Serbian *Jugoslovenski kasač* name, Balkans harness-racing identity, 14.1–15-hand range, rare commonness, solid-coat policy, and high-speed profile.

2. Confirm it does **not** spawn in ordinary wild packs because `wild` is absent. It should appear through cowboy, stable, and spawn-egg systems only.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is bay, compact, fast, or shaped like a harness horse.

4. Generate at least 1,000 pure founders. The population should contain bay, bay-brown/dark bay, brown, light chestnut, and black. Gray should remain rare. It must never produce cream dilution, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, roan, pinto, frame overo, splash, Appaloosa/leopard spotting, brindle, or magical effects.

5. Inspect genome panels:
- `E/e` and `At/A/a` should create the intended solid base-color range.
- `G` should be rare.
- Flaxen, Sabino 1, and rabicano should be near absent if retained.
- All excluded dilution, pinto, leopard, magical, and named disease loci must remain wild type or clear.

6. Confirm true-gray behavior:
- `G/N` foals are born with a bay, brown, black, or chestnut base.
- Their coats progressively lighten with age.
- A gray Yugoslavian Trotter must not render as silver dapple, cream, or white from birth.

7. Confirm mature stats. Horses should be compact-to-medium, high in sustained travel speed, modest at jumping, and reasonably hardy. They should not match an elite Thoroughbred in sprint specialization, a Standardbred-specific speed implementation if one exists, or an Oldenburg in jumping scope.

8. Confirm all named disorder loci remain clear across a large pure-founder sample. No WFFS, PSSM1, HYPP, GBED, HERDA, SCID, CA, LFS, or other listed disease allele should originate from a pure Yugoslavian Trotter founder.

9. Test default lineage:
- Yugoslavian Trotter × Yugoslavian Trotter → Yugoslavian Trotter.
- Yugoslavian Trotter × Orlov Trotter → Yugoslavian Trotter cross.
- Yugoslavian Trotter × Standardbred → Yugoslavian Trotter cross.
- Yugoslavian Trotter cross × pure Yugoslavian Trotter → the existing Yugoslavian Trotter cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add gray, cream, dun, tobiano, leopard complex, giant-draft size, WFFS, PSSM1, or another excluded allele. Descendants should inherit normally but must not gain pure Yugoslavian Trotter lineage from a high speed score, harness context, or bay coat alone.

## Sources

- [FAO—Animal Genetic Resources, Serbia: Yugoslavian Trotter breed record](https://www.fao.org/4/ah759e/ah759e13.htm): primary breed-reference material for 145–152 cm average height, 153 cm stallion average, bay/bay-brown/light-chestnut/brown/black colors, rare gray, mature age, and body measurements. [fao](https://www.fao.org/4/ah759e/ah759e13.htm)

- [Animal Genetic Resources of Serbia: Situation and Perspectives](https://researcherslinks.com/current-issues/Animal-Genetic-Resources-Serbia/20/8/3759/html): Serbian animal-genetic-resource context for native and managed Serbian livestock populations. [researcherslinks](https://researcherslinks.com/current-issues/Animal-Genetic-Resources-Serbia/20/8/3759/html)

- [Orlov Trotter breed guide](https://madbarn.ca/orlov-trotter-breed-profile/): supplementary comparison source for the gray-dominant Orlov Trotter foundation population and why the Yugoslavian Trotter should not be modeled as a gray-heavy Orlov clone. [madbarn](https://madbarn.com/orlov-trotter-breed-profile/)

- [Standardbred breed reference](https://hi3.horseisle.com/www/bbb/Standardbred.php): supplementary comparison source for Standardbred body/color variation and why Yugoslavian Trotter should retain a more compact Balkan solid-color profile. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Standardbred.php)