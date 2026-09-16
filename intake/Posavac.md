The **Posavac**—also called the **Croatian Posavac**, **Posavina Horse**, *Posavski konj*, or *Posavski bušak*—should be a rare Croatian wetland draught horse: compact, chestnut-led, thick-maned, calm, strong, and exceptionally suited to the floodplains of the Sava River. It should feel unlike a giant draught breed: short, broad, powerful, and low-maintenance, with high heartiness and low specialist speed/jump scores. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

## Identity & flavour

The **Posavac**, formally the **Croatian Posavac** or Posavina Horse, is a traditional draught breed from **Posavina**, the low Sava River floodplain of central Croatia. Its local names include *Posavski konj*, *Posavski bušak*, and Turopolje horse. This is a wetland work horse: shaped by river valleys, seasonally flooded pasture, marshland, muddy farm tracks, woodland margins, and smallholder agriculture rather than by elite riding-sport selection. Its roots are old and regional, built from the local Posavina horse population and later influenced by imported European heavy-horse stock. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

The Posavac was made for steady rural labor. It hauled wood and farm produce, pulled ploughs and carts, transported families, worked in wet fields, and carried out forestry and agricultural tasks that demanded traction more than speed. Its flooded homeland rewarded a horse that could remain calm in mud, recover on coarse pasture, keep working through wet and cold weather, and move a load without needing the feed or size of a very large Belgian or Shire. In a Minecraft survival world, the Posavac should feel at home beside rivers, marshes, forests, and low meadows: not elegant, not quick, but quietly unbreakable. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

Most Posavacs stand about **140–150 cm**, roughly **13.3–14.3 hands**. They are compact cold-blooded draught horses with a broad, heavy but kind head; short, thick muscular neck; deep chest; wide back; broad loins; rounded, powerful croup; low-to-medium withers; and short, strong limbs. The build should look substantial relative to height, with enough bone and body for work but without the towering height of a modern giant draught. Mane and tail are dense, and a chestnut horse often shows an attractive **flaxen or light mane and tail** contrast. Feathering is light to moderate, useful but never Shire-like. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

The familiar Posavac palette is **chestnut**, bay, gray, and rich brown. Chestnut is the breed’s strongest visual signature, especially deep liver chestnut or bright chestnut with a pale mane and tail. Bay and brown provide the main dark alternatives, while gray occurs in some descriptions as a minority. Because no dependable population-wide coat-genotype study was found, the pure founder pool should remain conservative: a chestnut-heavy Extension pool, enough Agouti to create bay/brown, a small gray chance, and low flaxen to enhance chestnut mane contrast. Pure founders should not generate cream, champagne, silver, dun, roan, pinto, leopard complex, or magical effects. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

Posavacs are valued for docility, friendliness, tractability, endurance, and resilience. They belong in Procedural Horse Genetics because they offer a distinct gameplay niche: a compact floodplain draft whose breeding goal is not a spectacular color or extreme athletic stat, but a stout chestnut worker that remains healthy in difficult country. The mod does not model traction force, pulling efficiency, wetland footing, seasonal flooding, feathering, thick mane, hoof quality, forage conversion, working temperament, forestry training, traditional Croatian harness, conservation population management, or the difference between a registered Posavac and a visually similar Balkan crossbred draught horse. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file follows the readable JSON convention used in the provided example. Before compiling, reconcile exact field names, source enums, locus IDs, allele symbols, `Commonness` values, pricing units, and `TargetBand` serialization against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The available material clearly supports Posavina origin, compact draught function, 140–150 cm height, chestnut/bay/brown/gray phenotype range, and flaxen-like mane/tail contrast. I did not locate a population-wide Posavac `MC1R`, `ASIP`, `STX17`, or flaxen genotype study. The rates below are transparent **gameplay approximations**, not Croatian studbook allele frequencies. No Posavac-specific carrier-rate study was located for the requested disorder panel. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

```json
{
  "id": "posavac",
  "name": "Posavac",
  "type": "natural",
  "notes": "The Posavac, also called the Croatian Posavac, Posavina Horse, Posavski konj, or Posavski bušak, is a Croatian compact draught breed defined by Sava floodplain origin, wetland and forest-farm work, strong traction, broad cold-blooded conformation, thick mane and tail, light-to-moderate feathering, calm temperament, coarse-forage thrift, and muddy-ground endurance. Procedural Horse Genetics does not model pulling force, harness work, ploughing, forestry, wetland traction, seasonal flooding, feathering, mane density, hoof quality, feed conversion, local Croatian management, conservation status, or working temperament.",

  "biomes": [
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:river",
    "minecraft:meadow",
    "minecraft:plains",
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest",
    "minecraft:grove",
    "minecraft:taiga"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 820,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.55,
      "e": 0.45
    },
    "agouti": {
      "A": 0.70,
      "a": 0.30
    },

    "grey": {
      "N": 0.94,
      "G": 0.06
    },
    "flaxen": {
      "N": 0.80,
      "f": 0.20
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
    "speed": 3,
    "jump": 3,
    "health": 9,
    "size": [
      0.95,
      1.07
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Posavac × Posavac produces Posavac. Posavac × another pure breed produces a Posavac cross. A Posavac cross bred back to pure Posavac remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Posavac lineage merely because a horse is broad, chestnut, flaxen-maned, and found in a swamp biome: real identity depends on Posavina ancestry and breed registration."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed type | Croatian compact draught horse | Posavac is a traditional Croatian Posavina draft, selected for rural farm, forestry, transport, and wetland work rather than speed or sport.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Chestnut core | `E: 0.55 / e: 0.45` | The elevated `e` frequency makes chestnut the most common outcome, reflecting the breed’s familiar deep chestnut profile. It still permits non-chestnut black-pigment horses. The value is a gameplay approximation, not a genotyping result.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Bay and brown | `A: 0.70 / a: 0.30` | Among black-pigment horses, a high `A` rate favors bay and brown rather than making black common. That matches the documented chestnut/bay/brown emphasis.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Gray | `G: 0.06` | Gray is listed among the breed’s known colors but should be rare compared with chestnut and bay/brown. This rate is a conservative implementation estimate.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Flaxen | `f: 0.20` | Chestnut Posavacs are often described with flaxen or light mane/tail contrast. The locus is useful for the visual identity, but the rate is not presented as a Croatian population frequency.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Gray versus flaxen | Separate loci | A gray horse must progressively lighten over time, while flaxen affects mane/tail contrast on chestnut. The renderer should not use gray to create pale hair on chestnuts. |
| Dilutions | Cream, pearl, champagne, silver, mushroom, and dun forced wild type | No strong source located supports these in the conservative Posavac founder pool. The chestnut/flaxen identity already provides visible variety without creating a diluted-color breed. |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | The breed is represented as solid-coated. Minor incidental markings do not justify strong inherited body-white or roan loci. |
| Leopard complex | `LP`, `PATN1`, and `PATN2` forced wild type | Appaloosa-type patterning is not supported in a pure Posavac founder population. |
| Magical loci | All forced wild type | Wetland hardiness and sturdy work type are natural traits, not magical color effects. |
| Disorders | All requested loci clear | No Posavac-specific carrier frequencies were found for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `3/10` | The Posavac is designed for pulling and steady work, not speed. It should be usable but distinctly slower than riding, trotting, racing, or gaited-trail specialists. |
| Jump | `3/10` | Compact strength and working legs do not equal show-jumping ability. |
| Health | `9/10` | Represents resilience in wetland conditions, coarse-forage usefulness, sound working temperament, and all-day durability—not immunity from disease or injury.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |
| Size | `×0.95–1.07` | Captures a 13.3–14.3-hand compact draught horse: broad and powerful, but not tall.  [thepixelnomad](https://thepixelnomad.com/posavac-horse/) |

## Disorder approach

All named disorder loci remain **clear** in pure Posavac founders.

This is a deliberate evidence standard. The available result set supports the breed’s Croatian floodplain origin, type, colors, and working role, but it provides no defensible Posavac carrier frequency for ACAN dwarfism, `PLOD1` WFFS, `EDNRB` lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA.

The WFFS population survey is not a justification to invent a Posavac rate: it reports zero WFFS carriers in the sampled **Polish Heavy Draft** group, which is a separate breed with a distinct history and does not establish anything about Croatian Posavacs. [mdpi](https://www.mdpi.com/2073-4425/11/12/1518)

Likewise, PSSM1 is a dominant `GYS1` condition with highly variable prevalence among breeds. The fact that it is testable in many drafts does not establish Posavac frequency. Keep it clear unless a direct population screen becomes available. [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Genetic_Disease/PSSM.asp)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `posavac` as a natural Croatian floodplain-draught record with working notes, wetland biome mapping, chestnut/flaxen pool, clear diseases, rare commonness, and compact heavy-horse stat targets. |
| `common/breed/Breeds` | Register `posavac` for wild pack spawning, stable access, spawn eggs, H-menu display, breed books, commands, saved genomes, and lineage labels. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Posavac is a regional Croatian working/conservation breed rather than an ordinary sale-yard horse. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map. Do not use bands to fake wetland traction, feathering, hoof quality, thick mane, or draft strength. |
| `common/breed/spec/` | Reconcile the illustrative object with the real parser/writer. Confirm exact loci for Extension, Agouti, gray, and flaxen; direct probability syntax; wild-type defaults; and all source/commonness values. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity ladder and that `spawn_weight: 1.5` is valid where direct spawn weights are supported. |
| `common/breed/BreedStatCurve` | Convert speed 3, jump 3, health 9, and size ×0.95–1.07 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only Extension, Agouti, low gray, and flaxen. Force all dilution, pinto, leopard, magical, and requested disorder loci wild type or clear, then apply compact-draught stat bands. |
| `common/breed/BreedLineage` | Use the default pure/cross/Mixed table. A chestnut, flaxen-maned, broad horse in a swamp must not receive Posavac lineage from phenotype or biome alone. |
| `common/genetics/SpliceOutcome` | No Posavac-specific exception. A splice-carrot allele passes normally and can create a nonstandard descendant with cream, pinto, leopard, disease, or other excluded traits. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize the compact, slow, low-jump, high-heartiness working-horse targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual serializer requires an explicit default. |

## Verification

1. Confirm **Posavac** appears in the H-menu’s Breeds tab and the breed book with Croatian Posavina identity, floodplain working role, rare commonness, chestnut/flaxen palette, compact draught frame, and high-health score.

2. Spawn repeated packs in swamp, river, meadow, plains, forest-edge, grove, taiga, and wetland biomes. Every horse in one selected Posavac pack must display the **Posavac** lineage label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is chestnut, broad-bodied, flaxen-maned, or standing in a swamp.

4. Generate at least 1,000 pure founders. The population should be chestnut-led, with bay and brown minorities; gray should be rare. A substantial subset of chestnuts should show flaxen or lighter mane/tail expression if the mod’s flaxen locus handles it correctly.

5. Confirm that no pure Posavac founder produces cream dilution, palomino, buckskin, cremello, pearl, champagne, silver dapple, mushroom, true dun, pinto, frame overo, splash, strong sabino body white, dominant white, roan, rabicano, leopard complex, brindle, or magical phenotypes.

6. Inspect genomes:
- `e` should be frequent enough to make chestnut common.
- `E` plus `A` should make bay/brown secondary outcomes.
- `G` must stay uncommon.
- `f` should occur only in the stated moderate range and visibly matter only on chestnut.
- All other coat and disorder loci must be wild type or clear.

7. Test genetic distinctions:
- `G/N` foals should be born pigmented and progressively gray, not white from birth.
- Flaxen chestnuts should remain chestnut-based and must not be confused with palominos.
- Bay should be `E_ A_`, black should require `E_ a/a`, and chestnut should be `e/e`.

8. Confirm mature stats. Posavacs should feel compact and powerful but slower than ordinary saddle horses, poor-to-modest jumpers, and especially hearty. They should not become giant-draught sized solely because their build is broad.

9. Test default lineage:
- Posavac × Posavac → Posavac.
- Posavac × Croatian Coldblood → Posavac cross.
- Posavac × Nonius → Posavac cross.
- Posavac cross × pure Posavac → the existing Posavac cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test outcrossing or splice inheritance. Add cream, tobiano, leopard complex, draft height, sprint speed, or a named disease allele via normal mechanics. Descendants should inherit normally but must never regain pure Posavac status from chestnut color, flaxen hair, or a wetland biome alone.

## Sources

- [Posavac breed profile](https://thepixelnomad.com/posavac-horse/): available English-language reference for Croatian Posavina origin, aliases, wetland/floodplain adaptation, rare/vulnerable conservation context, 140–150 cm size, chestnut/bay/gray palette, light mane/tail, heavy working conformation, calm disposition, and farm/forestry roles. [thepixelnomad](https://thepixelnomad.com/posavac-horse/)

- [Distribution of Warmblood Fragile Foal Syndrome Type 1 in multiple breeds](https://www.mdpi.com/2073-4425/11/12/1518): peer-reviewed WFFS population study, used only to establish why a separate Polish Heavy Draft result cannot be treated as a Posavac disease estimate. [mdpi](https://www.mdpi.com/2073-4425/11/12/1518)

- [PSSM genetic-test reference](https://avian2.animalgenetics.com/Equine/Genetic_Disease/PSSM.asp): general explanation that `GYS1` PSSM1 is dominant and why disease frequency cannot be inferred merely from a breed being a draught type. [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Genetic_Disease/PSSM.asp)