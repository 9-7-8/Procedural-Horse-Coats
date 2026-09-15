The **Colorado Ranger**—also called the **Colorado Rangerbred** or simply **Rangerbred**—should be a rare, agile American range-and-performance horse whose breed identity comes from pedigree and athletic type, not from mandatory spotting. Its mod population should be split between solid horses and Leopard Complex horses: a player should recognize one by its compact, refined, powerful western-sport silhouette and be delighted, rather than surprised, when a blanket or leopard-patterned individual appears.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734)

## Identity & flavour

The **Colorado Ranger Horse** originated on the western ranges of the United States, especially Colorado, but its foundation story begins far from the Rocky Mountains. In 1878, Sultan Abdul Hamid II of Turkey gave former U.S. President Ulysses S. Grant two stallions: the gray Siglavy-Gidran Arabian **Leopard** and the blue-gray Barb **Linden Tree**. Descendants of these horses entered American breeding programs, and two later stallions, **Patches** and **Max**, became the recognized foundation sires of the Colorado Rangerbred. Mike Ruby established the Colorado Ranger Horse Association in 1935; the registry received its corporate charter in 1938.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734)

Rangerbreds were made for the broad practical demands of western country. They needed enough stamina and refinement for long miles over open range, enough speed and agility for cattle work, and enough athleticism for ranch riding, trail work, endurance, western performance, roping, gaming, jumping, and general riding. Their Arabian and Barb heritage contributes refinement, endurance, and a often discernible dished profile; later approved outcrosses with Arabian, Thoroughbred, Appaloosa, AraAppaloosa, and American Quarter Horse blood support a useful, balanced performance horse.  [appaloosamuseum](https://www.appaloosamuseum.org/colorado-ranger-horse/)

Colorado Rangers stand about 14.2–16 hands, with 15.2 hands often given as an average. They should look medium-sized, compact, balanced, and refined rather than tall, massive, or pony-like. Typical features include a clean, sometimes slightly dished Arabian-style head; well-set neck; sloping shoulder; prominent withers; strong back; deep heartgirth; powerful rounded hindquarters; sturdy clean legs; and sound hooves. There is no draft feather, no need for a huge mane or tail, and no distinctive gait requirement: the key impression is a capable western athlete with enough elegance to show its oriental ancestry.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734)

The Colorado Ranger Horse Association is explicitly **not a color registry**. A qualified Rangerbred can be any solid color or carry Leopard Complex/Appaloosa-style patterns, including blanket, snowflake, varnish, and leopard expression. Solid bay, chestnut, black, gray, roan, dun, grulla, palomino, and buckskin all occur. However, pinto coloration and Paint Horse ancestry within five generations are not permitted under the registry rules, and draft or pony ancestry is also excluded. That makes this a wonderful mod breed for a controlled kind of visual variety: broad ordinary pigment and dilution genes, a meaningful Leopard Complex presence, and a hard prohibition on tobiano, frame, splash, W-series dominant-white, and other pinto-founder expression.  [en.wikipedia](https://en.wikipedia.org/wiki/Colorado_Ranger)

Temperament should feel intelligent, responsive, sensible, athletic, and willing. The breed has the agility to work cattle, the stamina for distance, and the trainability for a broad program of western and English uses. In Procedural Horse Genetics, players should breed toward high maneuverability, a sturdy medium riding-horse frame, good speed, good jump, and strong general health. The mod cannot model documented descent from Max or Patches, registry inspection, an Arabian dished profile, cattle sense, range experience, endurance conditioning, white sclera, mottled skin, striped hooves, or the eye-care needs associated with Leopard Complex horses.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734)

## Breed JSON

> **Schema note:** The Procedural Horse Genetics wiki URL from the original prompt was not retrievable through the browser tool. The keys below are implementation-oriented and must be reconciled with the current `common/breed/spec/` schema, actual enums, locus names, and gene-value syntax.
>
> **Breed-design choice:** This file uses an 11:9 solid-to-Leopard Complex strain split. That makes leopard-patterned Rangerbreds frequent enough to communicate their Appaloosa relationship, while preserving the CRHA’s central point: a Rangerbred need not be spotted, and color does not determine registration.  [breeds.okstate](https://breeds.okstate.edu/horses/colorado-ranger-horses)

```json
{
  "id": "colorado_ranger",
  "name": "Colorado Ranger",
  "type": "natural",
  "notes": "Colorado Rangerbreds are defined by documented descent from the foundation sires Max or Patches, registered-pedigree eligibility, western athleticism, Arabian and Barb refinement, and practical range-horse conformation. Procedural Horse Genetics does not model pedigree tracing, registry inspection, the required absence of disallowed Paint, Pinto, draft, or pony ancestry, Arabian head profile, cattle sense, endurance conditioning, mottled skin, white sclera, striped hooves, sun sensitivity, recurrent uveitis risk, or night-blindness behavior.",

  "biomes": [
    "minecraft:plains",
    "minecraft:windswept_hills",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:badlands"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 710,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.68,
      "e": 0.32
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "cream": {
      "N": 0.87,
      "Cr": 0.13
    },
    "dun": {
      "N": 0.88,
      "D": 0.12
    },
    "grey": {
      "N": 0.90,
      "G": 0.10
    },
    "roan": {
      "N": 0.90,
      "Rn": 0.10
    },
    "silver": {
      "N": 0.97,
      "Z": 0.03
    },
    "champagne": {
      "N": 0.99,
      "Ch": 0.01
    },
    "flaxen": {
      "N": 0.80,
      "f": 0.20
    },
    "rabicano": {
      "N": 0.92,
      "Rb": 0.08
    },

    "sabino_1": {
      "N": 0.98,
      "SB1": 0.02
    },
    "tobiano": {
      "N": 1.0
    },
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_1": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 1.0
    },

    "leopard_complex": {
      "N": 0.76,
      "LP": 0.24
    },
    "patn1": {
      "N": 0.72,
      "PATN1": 0.28
    },
    "patn2": {
      "N": 0.89,
      "PATN2": 0.11
    },

    "pearl": {
      "N": 1.0
    },
    "mushroom": {
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
    "GYS1_PSSM1": {
      "N": 0.985,
      "pssm1": 0.015
    },

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
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 7,
    "jump": 7,
    "health": 8,
    "size": [
      0.99,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "solid_rangerbred",
      "name": "Solid Rangerbred",
      "weight": 11,
      "notes": "The default Colorado Rangerbred strain. It represents registered solid-color horses, because the CRHA is a pedigree registry rather than a color registry.",
      "coat_genes": {
        "leopard_complex": {
          "N": 1.0
        },
        "patn1": {
          "N": 1.0
        },
        "patn2": {
          "N": 1.0
        }
      }
    },
    {
      "id": "leopard_rangerbred",
      "name": "Leopard Rangerbred",
      "weight": 9,
      "notes": "An Appaloosa-characteristic Rangerbred strain. It carries Leopard Complex and pattern modifiers, allowing varnish, blanket, snowflake, leopard, and fewspot-style outcomes under the mod’s LP/PATN renderer.",
      "coat_genes": {
        "leopard_complex": {
          "N": 0.35,
          "LP": 0.65
        },
        "patn1": {
          "N": 0.35,
          "PATN1": 0.65
        },
        "patn2": {
          "N": 0.72,
          "PATN2": 0.28
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Colorado Ranger × Colorado Ranger produces Colorado Ranger. Colorado Ranger × another pure breed produces a cross. A pure Colorado Ranger crossed with an Appaloosa, Arabian, Thoroughbred, AraAppaloosa, or American Quarter Horse remains a cross in the mod, even though the real CRHA can accept qualifying registered outcross offspring after pedigree review. Do not use appearance alone to label a foal Colorado Ranger."
  }
}
```

## Genetics rationale

| Feature | Proposed treatment | Reason |
|---|---:|---|
| Solid versus Leopard Complex strains | 11:9 | Both solid and leopard-patterned horses are real Rangerbreds. The registry explicitly has no color preference, though Appaloosa-type characteristics are common enough to be culturally recognizable.  [breeds.okstate](https://breeds.okstate.edu/horses/colorado-ranger-horses) |
| Leopard Complex | `LP` 0.24 base; strongly enriched in leopard strain | Represents blanket, snowflake, varnish, leopard, and fewspot outcomes through the existing LP/PATN system.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/) |
| `PATN1` and `PATN2` | Enabled only with the Leopard Complex strain | Pattern modifiers should enrich spotted Rangerbreds rather than make solid founders accidentally leopard-patterned. |
| Pinto loci | Forced wild type | CRHA rules exclude Pinto and American Paint Horse ancestry within five generations. Pure Ranger founders should not roll tobiano, frame, splash, or W-series pinto patterns.  [en.wikipedia](https://en.wikipedia.org/wiki/Colorado_Ranger) |
| Base colors and dilutions | Broad bay/chestnut/black pool plus cream, dun, gray, roan, silver, and rare champagne | Registry sources describe solid Rangerbreds across bay, chestnut, black, gray, roan, dun, grulla, palomino, and buckskin.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/) |
| Roan versus LP varnish | Both enabled | True roan and LP-associated varnish roaning should remain genetically distinct, even if the renderer makes them visually similar at some ages.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/) |
| PSSM1 | `pssm1` 0.015 | No publicly located breed-wide Colorado Ranger frequency supports a precise value. The small rate is a transparent gameplay approximation for an approved-outcross population that includes Quarter Horse blood, not a claim of measured CRHA prevalence.  [coloradoranger](http://www.coloradoranger.com/registration.html) |
| Speed | 7/10 | Athletic enough for range work, games, and western performance; not a dedicated racehorse.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734) |
| Jump | 7/10 | A versatile riding horse with usable jumping and general sport ability.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/) |
| Health | 8/10 | Reflects practical stamina and robust range-horse type, while not erasing LP-associated eye concerns.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/) |
| Size | ×0.99–1.10 | Fits the documented 14.2–16-hand range and 15.2-hand average.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734) |

## Disorder and LP policy

No credible, breed-specific carrier-frequency study was located for the prompt’s listed simple Mendelian disorder loci in Colorado Rangers. The `GYS1_PSSM1` value is thus optional and conservative. If your data policy allows **only directly measured breed-population frequencies**, use this stricter replacement and keep all listed disorders clear:

```json
"disorder_genes": {
  "ACAN_dwarfism": { "N": 1.0 },
  "PLOD1_friesian_dwarfism": { "N": 1.0 },
  "MET_lethal_white": { "N": 1.0 },
  "PRKDC_SCID": { "N": 1.0 },
  "TOE1_CA": { "N": 1.0 },
  "MYO5A_LFS": { "N": 1.0 },
  "GBE1_GBED": { "N": 1.0 },
  "CVM": { "N": 1.0 },
  "megaesophagus": { "N": 1.0 },
  "SCN4A_HYPP": { "N": 1.0 },
  "GYS1_PSSM1": { "N": 1.0 },
  "PPIB_HERDA": { "N": 1.0 }
}
```

The crucial health behavior for this breed is actually **not** represented by the disorder list. Leopard Complex is linked to congenital stationary night blindness, particularly in `LP/LP` horses, and Appaloosa-patterned horses have elevated risk for equine recurrent uveitis. CSNB is a recessive LP-linked phenotype, while ERU is a multifactorial risk association—not a simple recessive `disorder_genes` entry. Represent both in the LP phenotype/health system if PHG has one; otherwise document them in `notes()` and avoid inventing a carrier rate.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/)

## Code map

| Location | Required work |
|---|---|
| `common/breed/Breed` | Add `colorado_ranger` with natural status, metadata, coat pools, strain entries, spawn setup, price, notes, and stat targets. |
| `common/breed/Breeds` | Register the breed ID for data loading, saved genomes, menus, books, spawning, and lineage resolution. |
| `common/breed/BreedSource` | Validate all four listed sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Keep the epigenetic-band map empty. This breed’s variation comes from LP/PATN genetics and broad allowed color loci, not a mandated shade. |
| `common/breed/spec/` | Support or validate strain weights, per-strain locus overrides, `type`, commonness, sources, and gene-pool serialization in both directions. |
| `common/breed/Commonness` | Confirm that `RARE` maps to weight `1.5`; otherwise use the project’s nearest valid rarity enum/value. |
| `common/breed/BreedStatCurve` | Resolve speed 7, jump 7, health 8, and size ×0.99–1.10 into valid `TargetBand` definitions. |
| `common/breed/BreedFounder` | Roll base genome, choose solid/leopard strain at 11:9, apply its LP/PATN override, and force pinto loci wild type for every pure founder. |
| `common/breed/BreedLineage` | Keep default pure/cross/Mixed/Feral Mixed rules; do not emulate real-world approved-outcross registration automatically. |
| `common/genetics/SpliceOutcome` | No Ranger-specific exception, but verify LP and PATN alleles added by a splice carrot can transmit to foals normally. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate speed, jump, health, and size band definitions. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` band required unless the actual data format requires an explicit enum. |
| LP phenotype/health renderer | If present, make `LP/LP` capable of CSNB behavior and attach LP-associated ERU-risk metadata without treating ERU as a Mendelian carrier condition.  [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Coat_Color/Appaloosa.asp) |

## Verification

1. **Breed registration:** Confirm **Colorado Ranger** appears in the H-menu’s Breeds tab and the breed book with correct natural-breed status, rare commonness, western/range flavour text, valid biomes, price, and four acquisition sources.

2. **Pack identity:** Generate a pack in a valid plains, meadow, savanna, badlands, or windswept-hills biome. Every member of the same pack must show the **Colorado Ranger** breed label, whether it is solid or Leopard Complex patterned.

3. **Feral separation:** Verify that a lone ordinary wild horse displays **Feral Mixed**, not Colorado Ranger. A naturally occurring leopard-spotted Feral Mixed horse must also remain Feral Mixed; visible LP traits must never rewrite lineage.

4. **Strain frequency:** Generate at least 200 pure founders. Approximately 55% should be `solid_rangerbred` and 45% `leopard_rangerbred`. Inspect the strain metadata rather than visible pattern alone, because `LP` can produce subtle varnish expression.

5. **Pattern range:** Among Leopard Rangerbreds, verify the renderer can generate familiar Appaloosa-type variation from LP/PATN combinations: varnish-like roaning, blankets, snowflakes, spotted blankets, leopards, and fewspot outcomes. Solid-strain founders must have `LP`, `PATN1`, and `PATN2` forced clear.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/)

6. **Pinto exclusion:** Across a large pure-founder sample, verify that tobiano, frame overo, splash, KIT-W, and other pinto-derived phenotypes never generate. Purebred horses may still have modest ordinary markings if the renderer permits them through baseline white-marking mechanics, but they must not display a pinto pattern.  [appaloosamuseum](https://www.appaloosamuseum.org/colorado-ranger-horse/)

7. **LP health hooks:** If implemented, breed `LP/n × LP/n` pairs. Confirm that only `LP/LP` offspring receive the congenital stationary night blindness flag or low-light penalty. Ensure ERU is treated as a risk modifier, not as a carrier/affected Mendelian locus.  [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Coat_Color/Appaloosa.asp)

8. **Disorders:** Under the supplied gameplay-conservative configuration, only PSSM1 may occur as non-clear. Under the strict evidence-only configuration, every listed disorder must remain clear. Test PSSM1 using the mod’s actual `GYS1` inheritance model.

9. **Lineage table:** Verify:
   - Colorado Ranger × Colorado Ranger → Colorado Ranger.
   - Solid Rangerbred × Leopard Rangerbred → Colorado Ranger.
   - Colorado Ranger × Appaloosa, Arabian, Thoroughbred, AraAppaloosa, or Quarter Horse → cross under default mod logic.
   - A Colorado Ranger cross × pure Colorado Ranger → cross.
   - Different crosses → Mixed.
   - Any lineage × Feral Mixed → Mixed.

10. **Stat feel:** Compare same-age, same-conditioning horses. Rangers should be medium-framed, quick, durable, and broadly athletic, without consistently exceeding specialist racehorses in speed or elite purpose-bred jumpers in jumping.

## Sources

- [Colorado Ranger Horse Association — History](http://www.coloradoranger.com/history.html): official registry history and its statement that CRHA is not a color registry, with examples from solid horses to blankets and tri-color leopards.  [coloradoranger](http://www.coloradoranger.com/history.html)
- [Colorado Ranger Horse Association — Registration](http://www.coloradoranger.com/registration.html): approved-outcross/pedigree requirements for CRHA breeding programs.  [coloradoranger](http://www.coloradoranger.com/registration.html)
- [Colorado Ranger Horse Association — Myths and Facts](http://www.coloradoranger.com/mythsandfacts.html): official clarification that Rangerbreds may display Appaloosa-associated patterns but that qualification is pedigree-based; Paints and Pintos are not approved outcrosses.  [coloradoranger](http://www.coloradoranger.com/mythsandfacts.html)
- [Appaloosa Museum — Colorado Ranger Horse](https://www.appaloosamuseum.org/colorado-ranger-horse/): foundation history, height, permitted solid/leopard colors, approved outcrosses, and exclusion of pinto, Paint, draft, and pony ancestry.  [appaloosamuseum](https://www.appaloosamuseum.org/colorado-ranger-horse/)
- [Equus Magazine — Breed Profile: Colorado Ranger Horse](https://equusmagazine.com/horse-care/eqcolorado1734): Grant’s Turkish stallions, Max and Patches, association history, average height, conformation, and pedigree-rule emphasis.  [equusmagazine](https://equusmagazine.com/horse-care/eqcolorado1734)
- [MadBarn — Colorado Ranger Horse Breed Profile](https://madbarn.ca/colorado-ranger-horse-breed-profile/): breed development, colors, approved outcrosses, sport uses, and LP-associated health-context overview.  [madbarn](https://madbarn.ca/colorado-ranger-horse-breed-profile/)
- [Animal Genetics — Appaloosa/Leopard Complex coat pattern](https://avian2.animalgenetics.com/Equine/Coat_Color/Appaloosa.asp): Leopard Complex, CSNB association, and eye-health context.  [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Coat_Color/Appaloosa.asp)
