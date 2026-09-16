The **Croatian Coldblood**—Croatian *Hrvatski hladnokrvnjak*—should be a rare, substantial Croatian farm and forest draught horse: large, broad, calm, highly fertile, and selected for traction and low-input rural work rather than speed or sport. Its founder pool should be chestnut-led but visibly broad enough to include bay, brown, black, gray, roan, and occasional dun, while excluding pinto, leopard, champagne, silver, pearl, mushroom, and magical colors. The breed is distinct from the smaller Posavac: it should stand taller, carry more mass, and feel more like a true heavy farm horse. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

## Identity & flavour

The **Croatian Coldblood**, Croatian *Hrvatski hladnokrvnjak*, is Croatia’s principal traditional heavy draught horse. It developed in the lowland agricultural regions of continental Croatia—especially Slavonia, Podravina, Posavina, Međimurje, and the Sava and Drava river basins—from local mares crossed over time with imported European heavy horses. Belgian/Brabant, Noriker, Ardennes, Percheron, and other Central European draught influences contributed size, mass, pulling strength, and calm working temperament, while local horses contributed adaptation to Croatian floodplains, forests, small farms, and seasonal pasture. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

The breed was made for the work that kept rural households moving: ploughing, logging, hauling hay and timber, pulling carts and wagons, carrying agricultural loads, transporting people, and working in harness where machines could not safely or economically go. Croatian Coldbloods should be powerful but not rushed. They belong in fields, wet lowlands, forest tracks, and village roads, where a short explosive gallop matters far less than the ability to lean into a collar, stand patiently, and keep walking with a load. In Minecraft, it should be a reliable heavy utility horse: slow compared with riding breeds, but sturdy, calm, and immensely durable. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

A typical Croatian Coldblood stands roughly **150–165 cm**, around **14.3–16.1 hands**, with stallions generally larger than mares. It has a broad, plain head; strong jaw; thick muscular neck; broad deep chest; pronounced body mass; short-to-medium strong back; broad loins; a wide, rounded, powerful croup; and short, heavily boned legs. Fetlock hair is usually present but moderate, not the extravagant feathering of a Shire. The mane and tail are dense and practical, and the hooves should read as broad, dark, and sound. At a glance, it must look like a real working horse: wide through the body and hindquarters, strong over the loin, and heavy enough to pull without becoming a fantasy giant. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

Croatian Coldbloods occur in several normal solid colors. Chestnut is common and should form the visual core; bay, brown, black, gray, roan, and dun appear in broader Croatian heavy-horse descriptions. This makes the breed less visually restricted than Murgese or Nonius, but it should still look like a traditional solid-colored farm horse rather than a modern color-bred pinto. A chestnut with flaxen mane and tail, a bay, a black, a gray, a blue roan, or an occasional dun should all feel plausible. Pure founders should not create tobiano, frame overo, splash white, dominant-white, leopard complex, champagne, pearl, mushroom, or magical coats. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

Croatian Coldbloods are valued for docility, strength, fertility, longevity, frugality, and calm work temperament. Their continued existence matters because they preserve agricultural knowledge and genetic diversity in a world where mechanization made heavy horses economically rare. In Procedural Horse Genetics, the player should breed toward a big, patient Croatian field horse—usually chestnut or bay, sometimes gray, roan, black, or dun—with extremely good health and a low, steady speed profile. The mod does not model traction force, harness discipline, ploughing, logging, fertility, milk production, feed conversion, feathering, hoof width, muscle mass, calmness, forest work, floodplain footing, Croatian rural history, or breed-registry status. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention used in the provided example. Before compiling, reconcile exact locus names, `D/nd1/nd2` representation, source enums, rarity labels, price units, and body-stat serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The available peer-reviewed material establishes that Croatian Coldblood and Posavina horses are distinct but genetically related Croatian draught populations. It does not provide a representative Croatian Coldblood coat-locus survey for Extension, Agouti, gray, dun, roan, cream, or flaxen. The allele probabilities below are transparent **gameplay approximations** made to produce the documented broad solid-color working-horse type. No Croatian Coldblood-specific carrier-frequency data was found for the requested disease panel. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

```json
{
  "id": "croatian_coldblood",
  "name": "Croatian Coldblood",
  "type": "natural",
  "notes": "The Croatian Coldblood, Hrvatski hladnokrvnjak, is a Croatian heavy agricultural and forestry horse defined by regional Croatian lineage, broad cold-blooded build, traction, harness work, ploughing, logging, cart work, calm temperament, fertility, long working life, coarse-forage thrift, moderate fetlock feathering, dense mane and tail, broad hooves, and floodplain/forest adaptation. Procedural Horse Genetics does not model pulling force, harness action, logging, ploughing, cart weight, fertility, milk production, feed conversion, hoof size, feathering, mane density, muscle mass, temperament, forest footing, breed registration, or Croatian rural cultural history.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest",
    "minecraft:grove",
    "minecraft:taiga"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.58,
      "e": 0.42
    },
    "agouti": {
      "A": 0.66,
      "a": 0.34
    },

    "grey": {
      "N": 0.91,
      "G": 0.09
    },
    "dun": {
      "N": 0.92,
      "D": 0.08
    },
    "roan": {
      "N": 0.90,
      "Rn": 0.10
    },
    "flaxen": {
      "N": 0.84,
      "f": 0.16
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
    "rabicano": {
      "N": 0.98,
      "Rb": 0.02
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
      1.08,
      1.20
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Croatian Coldblood × Croatian Coldblood produces Croatian Coldblood. Croatian Coldblood × another pure breed produces a Croatian Coldblood cross. A Croatian Coldblood cross bred back to pure Croatian Coldblood remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Croatian Coldblood lineage simply because a horse is large, chestnut, calm, or found in wet farmland: real identity depends on Croatian breed ancestry and studbook status."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed distinction | Larger, heavier counterpart to Posavac | The Croatian Coldblood and Posavina horse are genetically related but distinct populations. This file uses a taller size band and lower speed/jump profile than the compact Posavac.  [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf) |
| Base colors | `E: 0.58 / e: 0.42`; `A: 0.66 / a: 0.34` | Produces a chestnut-led but still broad working-horse palette: chestnut, bay, brown, and smaller black contribution. Exact locus frequencies are gameplay approximations, not Croatian studbook data. |
| Gray | `G: 0.09` | Gray is plausible among broad Croatian heavy-horse color classes but should remain a minority against the chestnut/bay core. This is a cautious implementation value. |
| Dun | `D: 0.08` | A low true-dun rate permits occasional primitive-marked bay dun, grullo, and red dun horses without making dun a central Croatian Coldblood identity. |
| Roan | `Rn: 0.10` | Roan is a plausible and visually appropriate minority in a broad Central European draught population. This is a gameplay approximation, not a published Croatian Coldblood allele frequency. |
| Flaxen | `f: 0.16` | Supports attractive chestnut-to-flaxen variation without confusing flaxen chestnuts with palominos. It is a visual approximation rather than a documented prevalence. |
| Minor markings | `SB1: 0.01`; `Rb: 0.02` | Extremely low values permit occasional conventional face/leg markings or tail/flank white-hair texture. Remove these entries if the mod’s loci create body-white pinto phenotypes too easily. |
| Excluded dilutions | Cream, pearl, champagne, silver, mushroom forced wild type | No retrieved source supports putting these in a conservative pure Croatian Coldblood founder pool. |
| Excluded patterns | Tobiano, frame, splash, KIT white, leopard complex, PATN, brindle forced wild type | The desired breed is a traditional solid-colored Croatian farm horse, not a pinto or Appaloosa-style color population. |
| Magical genes | All forced wild type | Working strength, fertility, and wetland/forest usefulness are natural traits. |
| Disorders | All named loci clear | No Croatian Coldblood-specific carrier-frequency study was found for the requested disorder panel. |
| PSSM1 caution | Do not infer from draft status | `GYS1` PSSM1 occurs in multiple breeds and can be common in some draught populations, but it is dominant and breed-specific frequency varies. “Draft horse” is not sufficient evidence to seed it in Croatian Coldblood founders.  [avian2.animalgenetics](https://avian2.animalgenetics.com/Equine/Genetic_Disease/PSSM.asp) |
| Speed | `3/10` | A traction breed should be slower than ordinary saddle horses but not stationary; this score represents steady working movement rather than racing speed. |
| Jump | `3/10` | Broad draught conformation and heavy mass do not support specialist jumping. |
| Health | `9/10` | Represents practical farm durability, fertility, robust constitution, and low-input working ability—not immunity from injury or inherited disease. |
| Size | `×1.08–1.20` | Models a substantial 14.3–16.1-hand work horse, clearly larger than Posavac but below giant draught scale. |

## Disorder approach

All requested disorder loci remain **clear** in the pure Croatian Coldblood founder pool.

This is the scientifically conservative choice. The retrieved genetic study supports treating Croatian Coldblood and Posavina as related but distinct Croatian populations; it does not provide frequencies for ACAN dwarfism, `PLOD1` WFFS, `EDNRB` lethal white syndrome, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

The most likely incorrect shortcut would be to give the breed PSSM1 simply because it is a draught breed. That is not evidence-based:

- `GYS1` PSSM1 is an autosomal dominant mutation. A heterozygous horse is genetically affected or at increased risk, not merely a harmless recessive carrier. [labogen](https://labogen.com/en/2019/08/14/spread-of-pssm-in-different-breeds/)
- Prevalence differs sharply across breeds and bloodlines.
- No direct Croatian Coldblood test result was located.

The file therefore leaves every disease locus clear. If a future Croatian breed-association or peer-reviewed test survey supplies a specific mutant allele or carrier frequency, replace only the directly supported locus and document the study population.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `croatian_coldblood` as a natural Croatian heavy-horse record with farm/forestry flavour, managed sources, wetland/forest biome association, broad solid coat pool, clear disorders, and large working stat targets. |
| `common/breed/Breeds` | Register `croatian_coldblood` for stable/cowboy acquisition, spawn eggs, breed-book pages, H-menu display, commands, saved genomes, breeding, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is omitted because this is a managed Croatian agricultural breed, not a free-ranging wild population. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band object. Do not use bands to fake heavy muscle, feathering, hoof breadth, farm temperament, or pulling strength. |
| `common/breed/spec/` | Reconcile the illustrative JSON with the actual parser/writer. Verify the `E/e`, `A/a`, `G`, `D`, `Rn`, flaxen, and minor white-modifier locus syntax; ensure unlisted genes default correctly. |
| `common/breed/Commonness` | Confirm `RARE` maps to the desired rarity ladder and keep `spawn_weight: 1.5` only if direct numerical weights are supported. |
| `common/breed/BreedStatCurve` | Convert speed 3, jump 3, health 9, and size ×1.08–1.20 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll the chestnut-biased broad solid coat pool, force pinto/leopard/unsupported dilution/magical/disease loci clear, and apply the heavy work-horse stat bands. |
| `common/breed/BreedLineage` | Apply default pure/cross/Mixed behavior. A large chestnut draught horse must not become Croatian Coldblood simply from phenotype, biome, or body stats. |
| `common/genetics/SpliceOutcome` | No special breed exception. A splice-carrot allele follows ordinary transmission and can produce a nonstandard Croatian Coldblood descendant. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the large body range, low speed/jump, and high heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is required unless the concrete schema mandates an explicit default. |

## Verification

1. Confirm **Croatian Coldblood** appears in the H-menu’s Breeds tab and breed book with its Croatian farm-and-forestry identity, rare commonness, large draught body range, chestnut-led palette, and managed-only source checklist.

2. Confirm the breed does **not** generate as an ordinary wild pack because `wild` is omitted. It should appear through cowboy, stable, and spawn-egg sources.

3. Confirm an ordinary lone wild horse reads **Feral Mixed**, even if it is large, chestnut, broad, slow, or standing in a wetland/farm-like biome.

4. Generate at least 1,000 pure founders. The cohort should center on chestnut, bay, brown, and occasional black. Gray, dun, roan, and flaxen-chestnut expression should appear as minority variation. All bodies should remain solid-coloured rather than pinto or leopard-spotted.

5. Confirm no pure founder produces cream dilution, palomino, buckskin, cremello, pearl, champagne, silver dapple, mushroom, tobiano, frame overo, splash, dominant white, leopard complex, PATN spotting, brindle, or magical effects.

6. Inspect genomes:
- `e` should be common enough to make chestnut prominent.
- `E` plus `A` should make bay/brown common secondary outcomes.
- `G`, `D`, `Rn`, and flaxen should remain minority loci.
- `SB1` and rabicano should be barely detectable, if retained.
- Every excluded coat locus and every named disorder locus must be wild type or clear.

7. Confirm correct phenotype mechanisms:
- Gray must progress with age and must not be rendered as a white-at-birth phenotype.
- A roan must retain a darker head/points and must not gray progressively.
- Dun must show true body dilution plus primitive markings.
- Flaxen must act only on chestnut mane/tail and must not make palominos.
- Minor white loci must not accidentally create a tobiano-like horse.

8. Confirm mature body stats. Croatian Coldbloods should trend large, slow but usable, low in jump specialization, and very high in health/heartiness. They should feel distinctly heavier than Posavacs and ordinary riding horses but smaller than the largest Shire-type giants.

9. Test default lineage:
- Croatian Coldblood × Croatian Coldblood → Croatian Coldblood.
- Croatian Coldblood × Posavac → Croatian Coldblood cross.
- Croatian Coldblood × Nonius → Croatian Coldblood cross.
- Croatian Coldblood cross × pure Croatian Coldblood → the existing Croatian Coldblood cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add cream, tobiano, leopard complex, silver, race-speed targets, WFFS, or PSSM1 through another breed or splice mechanics. Descendants should inherit normally but must not regain pure Croatian Coldblood status merely because they remain broad, chestnut, or found near a river.

## Sources

- [Genetic structure and admixture between the Posavina and Croatian Coldblood horse breeds](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf): peer-reviewed Croatian study establishing that Posavina and Croatian Coldblood horses are distinct but genetically related breeds, based on microsatellite analysis. [cjas.agriculturejournals](http://cjas.agriculturejournals.cz/pdfs/cjs/2013/02/04.pdf)

- [PSSM1—University of Minnesota Equine Genetics and Genomics Laboratory](https://equine-genetics.umn.edu/projects/genetic-muscle-disease-horses/polysaccharide-storage-myopathy-type-1): authoritative description of dominant `GYS1` PSSM1 inheritance and why draft-breed status alone cannot support a founder frequency. [equine-genetics.umn](https://equine-genetics.umn.edu/projects/genetic-muscle-disease-horses/polysaccharide-storage-myopathy-type-1)

- [LABOGEN—Spread of PSSM in different breeds](https://labogen.com/en/2019/08/14/spread-of-pssm-in-different-breeds/): supplementary explanation that PSSM1 is inherited dominantly and requires careful breed-specific interpretation. [labogen](https://labogen.com/en/2019/08/14/spread-of-pssm-in-different-breeds/)