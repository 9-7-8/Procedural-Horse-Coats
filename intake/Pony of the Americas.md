The **Pony of the Americas** should be modeled as a deliberately loud, Appaloosa-patterned American youth pony—not as a generic small Appaloosa and never as a pinto. A pure POA founder should always carry `LP`, usually carry at least one pattern modifier so it reads as visibly spotted from a distance, remain within a strict 11.2–14-hand range, and combine practical Quarter Horse-like versatility with pony size. The Pony of the Americas Club requires an approved Appaloosa-type pattern and specific Appaloosa characteristics for full registration while explicitly excluding pinto coloring and Paint/Pinto parentage. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

## Identity & flavour

The **Pony of the Americas**, usually abbreviated **POA**, is an American breed of riding pony developed for children and young riders. The breed began in Iowa in 1954 when Leslie Boomhower bought a young Appaloosa-cross foal named **Black Hand**. The foal’s distinctive spotted coat and compact, usable riding-pony build inspired a new breed that blended Appaloosa color and characteristics with the size and practicality needed for youth mounts. The Pony of the Americas Club (**POAC**) was established in 1954 and remains the breed’s national registry organization. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

The POA was made to do nearly everything a child or family could ask of a pony. It should be sturdy enough for western riding, trail riding, games, ranch-style work, pleasure riding, jumping, driving, and youth show classes, but small enough for a young rider to mount and manage. Its conformation is often described as a “small horse” rather than a tiny pony: the breed carries noticeable Quarter Horse and Appaloosa influence, with a refined head, broad chest, short back, strong hip, muscular hindquarters, clean legs, and a balanced, athletic outline. A POA should feel more substantial and capable than a Shetland-type pony, yet clearly smaller than an ordinary stock horse. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

A permanently registered POA must mature between **46 and 56 inches** at the withers—about **11.2–14 hands**. That narrow size rule is fundamental. A horse that outgrows 56 inches may be valuable, but it cannot receive normal permanent POA registration; a pony that lacks the required visible Appaloosa traits may receive another registration category but is not a fully regular spotted POA. This mod should therefore make pure POA founders reliably pony-sized and should not let a giant leopard-spotted warmblood appear as a normal breed founder. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

POAs are genetically and visually Appaloosa-patterned. Their allowed patterns include leopard, few-spot leopard, blanket, blanket with spots, snowcap, snowflake, frost, varnish or marbleized roan, white with dark hindquarter spots, and solid-bodied horses with dark spots. The breed’s registration rules treat **mottled skin**, **visible white sclera**, and **striped hooves** as crucial Appaloosa characteristics, especially where a gray, roan, frost, snowflake, or marbleized pattern might otherwise be ambiguous. In genetic terms, this means the `LP` locus is non-negotiable in pure founders; `PATN1` and the mod’s `PATN2` should be common enough to create real blankets and leopards, not merely subtle varnish effects. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

The POA must not be implemented as a Paint Horse in miniature. **Pinto coloration is prohibited**, Paint/Pinto ancestry is ineligible, and excessive white on the legs or face with light underlying skin is also barred from POA registration. Thus, tobiano, frame overo, splash white, Sabino 1, and generic KIT dominant-white alleles should all be forced wild type in pure POA founders. A horse may have a huge white leopard blanket, be nearly white as a few-spot, or show varnish roaning because of `LP` plus pattern modifiers; that is genetically and visually distinct from a tobiano or overo pinto. [poac](https://www.poac.org/club/registration-types)

In Procedural Horse Genetics, players should breed toward a compact, cheerful, all-around pony with unmistakable leopard-complex inheritance. Every pure POA should be recognizably “Appaloosa pony” at a glance: mottled skin, striped hooves, white sclera, and a visible blanket, leopard, snowflake, frost, or varnish-style pattern. The mod does not model POAC registration classes, the 40-foot visual-color rule, exact Appaloosa skin mottling, striped hoof keratin, sclera visibility, youth-rider suitability, western event training, driving training, subjective temperament, inspection photography, or the detailed distinction between blue-paper, pink-paper, tentative, hardship, and permanent registration. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied in the prompt could not be retrieved by the documentation fetcher. This follows the readable JSON convention in the provided Anglo-Arabian example. Before compilation, match every key, allele label, white-pattern locus identifier, commonness enum, source value, price unit, strain syntax, and body-stat target syntax to `common/breed/spec/`.
>
> **Scientific-policy caveat:** The POAC’s registration rules are phenotype- and characteristic-based, not a published population-genomic allele-frequency survey. `LP` is correctly fixed in the pure founder pool because the breed requires Appaloosa-type coat characteristics. `PATN1` and `PATN2` rates below are transparent gameplay calibrations intended to ensure a strong visible-pattern rate; they are not claimed POAC frequency estimates. Pinto loci are explicitly excluded by registry policy. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

```json
{
  "id": "pony_of_the_americas",
  "name": "Pony of the Americas",
  "type": "natural",
  "notes": "The Pony of the Americas is defined by POAC pedigree and registration rules, permanent 46-56 inch height, a visibly recognizable Appaloosa-patterned coat, mottled skin, striped hooves, visible white sclera, and youth-oriented all-around riding ability. Procedural Horse Genetics does not model the 40-foot color requirement, POAC paperwork, tentative versus permanent registration, Blue or Pink registration papers, inspection photographs, precise mottled-skin distribution, hoof striping, sclera visibility, youth suitability, rider skill, western-event training, driving, jumping training, or subjective pony temperament.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:windswept_hills",
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
  "price": 620,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.66,
      "e": 0.34
    },
    "agouti": {
      "A": 0.62,
      "a": 0.38
    },

    "leopard_complex": {
      "N": 0.0,
      "LP": 1.0
    },
    "patn1": {
      "N": 0.48,
      "PATN1": 0.52
    },
    "patn2": {
      "N": 0.55,
      "PATN2": 0.45
    },

    "grey": {
      "N": 0.94,
      "G": 0.06
    },
    "cream": {
      "N": 0.90,
      "Cr": 0.10
    },
    "dun": {
      "N": 0.94,
      "D": 0.06
    },
    "roan": {
      "N": 0.92,
      "Rn": 0.08
    },
    "flaxen": {
      "N": 0.94,
      "f": 0.06
    },

    "champagne": {
      "N": 1.0
    },
    "silver": {
      "N": 1.0
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
      0.77,
      0.94
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "blanket_leopard",
      "name": "Blanket and Leopard",
      "weight": 0.55,
      "notes": "The bold-pattern POA population: strong PATN expression produces blankets, blankets with spots, leopard, few-spot leopard, snowcap, and white-with-dark-hindquarter-spot outcomes. Every horse still carries LP.",
      "coat_genes": {
        "patn1": {
          "N": 0.25,
          "PATN1": 0.75
        },
        "patn2": {
          "N": 0.35,
          "PATN2": 0.65
        }
      }
    },
    {
      "id": "varnish_snowflake",
      "name": "Varnish and Snowflake",
      "weight": 0.45,
      "notes": "The subtler but still visibly Appaloosa-patterned POA population: LP remains fixed while lower PATN frequency favors snowflake, frost, varnish or marbleized roan, dark-spotted solids, and small blankets.",
      "coat_genes": {
        "patn1": {
          "N": 0.72,
          "PATN1": 0.28
        },
        "patn2": {
          "N": 0.74,
          "PATN2": 0.26
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Pony of the Americas × Pony of the Americas produces Pony of the Americas. Pony of the Americas × another pure breed produces a Pony of the Americas cross. A Pony of the Americas cross bred back to pure Pony of the Americas remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not label a small leopard-complex horse as pure POA solely from its phenotype: real POA registration depends on height, pedigree, Appaloosa characteristics, and exclusion of Paint/Pinto ancestry."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Core breed rule | `LP: 1.0` | A POA must display or genetically substantiate Appaloosa-type color and characteristics. The Leopard Complex locus is the correct genetic foundation for mottled skin, white sclera, striped hooves, varnish effects, and Appaloosa spotting. A pure POA pool without `LP` would violate the breed’s central identity.  [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas) |
| Pattern modifiers | `PATN1: 0.52`; `PATN2: 0.45`, with bold and subtle strains | `LP` alone can create varnish-type effects, while `PATN1` drives broad white pattern distribution and larger blankets/leopard outcomes. The POAC requires a visible pattern, so PATN genes need to be common enough to avoid an overwhelming number of merely varnish/dark founders. Exact values are gameplay calibrations.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Two strains | 55% Blanket/Leopard; 45% Varnish/Snowflake | This is not a formal POAC population division. It is a practical founder-generation design that preserves the full approved pattern spectrum while preventing every POA from rendering as a full leopard. |
| Base colors | `E: 0.66 / e: 0.34`; `A: 0.62 / a: 0.38` | POAs can carry a wide Appaloosa-compatible base-color range. These rates produce useful bay, black, brown, chestnut, and sorrel foundations beneath `LP`; they are gameplay estimates, not POAC allele data.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Gray | `G: 0.06` | POAC regulations specifically discuss gray ponies, provided they show mottled skin and white sclera and/or striped hooves. Gray is appropriate but should remain uncommon because it can visually obscure an Appaloosa pattern as the horse ages.  [poac](https://www.poac.org/club/registration-types) |
| Cream | `Cr: 0.10` | Palomino, buckskin, cremello, and perlino-compatible Appaloosa patterns are valid under the broad base-color approach; cream is allowed at a modest level.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Dun | `D: 0.06` | Dun and grulla-type base colors can support Appaloosa patterns but should be minor relative to ordinary bases.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Roan | `Rn: 0.08` | POAC registration language recognizes roan and marbleized-roan-like appearances, but `LP` varnish is biologically distinct from true roan. Keep `Rn` low so LP varnish remains the dominant source of Appaloosa-style progressive roaning.  [poac](https://www.poac.org/club/registration-types) |
| Flaxen | `f: 0.06` | Low gameplay modifier for occasional flaxen chestnut Appaloosa ponies; not a published POA frequency. |
| Pinto loci | Tobiano, frame, Sabino 1, splash, KIT white all forced `N` | The POAC explicitly excludes pinto-colored ponies, Paint/Pinto parents, and excessive white from non-Appaloosa white-pattern mechanisms. This is one of the most important breed-definition constraints.  [poac](https://www.poac.org/club/registration-types) |
| Leopard complex versus pinto | `LP/PATN`, not tobiano/overo | A nearly white few-spot leopard is still genetically Appaloosa, not a pinto. The renderer must preserve that distinction.  [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas) |
| Excluded loci | Champagne, silver, pearl, mushroom, rabicano, brindle, magical loci forced wild type | No source found supports seeding these loci as normal POA founder genes. The population already gains sufficient visual diversity from base color, cream/dun/gray, `LP`, and pattern modifiers. |
| HYPP | Forced clear | The POAC does not permit HYPP-positive ponies for registration/breeding approval, so a pure registered-founder pool should not seed `SCN4A` HYPP.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Other disorders | Forced clear | No credible POA-specific frequencies were found for ACAN, PLOD1, MET, SCID, CA, LFS, GBED, CVM, megaesophagus, PSSM1, or HERDA. Do not import Quarter Horse disease frequencies just because stock-horse influence exists in the breed. |
| Speed | `5/10` | Represents a useful, versatile riding pony—not a racehorse and not an exceptionally slow miniature. |
| Jump | `5/10` | POAs participate in youth all-around activities and can jump, but they are not a specialized show-jumping population.  [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/) |
| Health | `8/10` | Represents practical pony hardiness and all-around usability, moderated by the fact that `LP`-associated traits and individual management issues are not the same as blanket health immunity. |
| Size | `×0.77–0.94` | Matches the strict 46–56-inch permanent-registration height range, roughly 11.2–14 hands.  [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas) |

## Disorder approach

The pure POA founder pool should carry **no named disease alleles** from the requested list.

This is particularly important for **HYPP**. The Pony of the Americas Club states that ponies testing positive for HYPP are not permitted for registration, and animals descended from known HYPP bloodlines must test negative before breeding approval. The correct pure-POA implementation is therefore `SCN4A_HYPP: { "N": 1.0 }`, not a Quarter Horse-derived proxy frequency. [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/)

All other listed loci remain clear because no defensible POA-specific carrier-rate dataset was identified. In particular:

- Do not seed `PPIB`/HERDA, `GBE1`/GBED, or `GYS1`/PSSM1 from generic Quarter Horse ancestry alone.
- Do not seed `MET`/frame because frame overo is prohibited by POAC’s pinto/Paint exclusion policy.
- Do not confuse `LP` with a disease allele. Leopard Complex creates the breed’s required coat-and-characteristic package, though real `LP/LP` horses may have congenital stationary night blindness; that condition is not one of the disorder loci specified in the prompt and should only be modeled if the mod has a scientifically correct `TRPM1`/CSNB system.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `pony_of_the_americas` as a natural breed with POAC-focused flavour, sources, commonness, founder pools, `LP`-driven pattern structure, and strict pony-size target. |
| `common/breed/Breeds` | Register the unique ID for wild spawning, commands, menus, breed books, saves, and lineage display. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. If you want stricter realism, remove `wild` because POAs are a managed American registry breed; it is included here for gameplay access. |
| `common/breed/BreedBands` | Support an empty epigenetic band map. Do not attempt to encode mottled skin, sclera, striped hooves, or pattern visibility with an unrelated epigenetic band. |
| `common/breed/spec/` | Verify the actual names and inheritance implementation for `LP`, `PATN1`, `PATN2`, and any marker the mod uses for LP-associated mottling, sclera, hoof stripes, varnish, and few-spot phenotypes. |
| `common/breed/Commonness` | Confirm the `UNCOMMON` rarity enum and direct `spawn_weight: 3` mapping against the real ladder. |
| `common/breed/BreedStatCurve` | Map speed 5, jump 5, health 8, and size ×0.77–0.94 into valid `TargetBand` targets. |
| `common/breed/BreedFounder` | Choose the bold or subtle Appaloosa-pattern strain; then enforce `LP` in every founder and use strain-specific pattern-modifier pools. Force all pinto, Paint-derived, non-Appaloosa white-pattern loci and all named disorder loci clear. |
| `common/breed/BreedLineage` | Use the default pure/cross/Mixed system. A short horse carrying `LP` must not automatically gain POA lineage. |
| `common/genetics/SpliceOutcome` | No special exception. A splice-carrot allele follows standard Mendelian inheritance and can intentionally create a nonstandard descendant, including pinto patterns excluded from the pure POA pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize and generate the strict small-pony size range and balanced all-around physical targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the actual schema requires a default type. Do not use `TRADITIONAL` to represent a classic POA pattern. |

## Verification

1. Confirm **Pony of the Americas** appears in the H-menu’s Breeds tab and breed book with its American youth-pony identity, strict small size, Appaloosa-pattern description, pinto exclusion, and `LP`-based genetic explanation.

2. Spawn repeated eligible packs. Every horse generated in a selected POA pack should display **Pony of the Americas**, including horses from both the blanket/leopard and varnish/snowflake strains.

3. Confirm a lone ordinary wild horse remains **Feral Mixed**, even if it is small and leopard-spotted.

4. Generate at least 500 pure POA founders. Every founder must carry at least one `LP` allele. The population should visibly include blankets, spotted blankets, snowcaps, leopards, few-spots, snowflakes, frost, varnish/marbleized roan effects, and dark-bodied horses with visible dark spots; a full-leopard phenotype should be common enough to recognize but not obligatory.

5. Confirm the narrow body range. Mature pure POAs should stay inside the intended roughly 11.2–14-hand equivalent size band. Any animal substantially larger than that should result from an outcross, stat-gene edit, or a deliberately relaxed registry gameplay mode—not ordinary pure-founder generation.

6. Verify all pure founder genomes are free of `TO`, frame `O`, `SB1`, splash alleles, generic `KIT` dominant-white alleles, and other pinto-pattern loci. The ponies may be visually very white because of `LP + PATN`; this must not be mistakenly counted as pinto inheritance.

7. Verify `LP` and pattern inheritance:
- `LP/N × N/N` should transmit `LP` to approximately half of foals.
- `LP/N × LP/N` should produce the normal segregation, including approximately 25% `LP/LP`.
- `PATN1` or `PATN2` heterozygotes crossed to wild type should transmit their modifier alleles to approximately half of foals.
- `LP` with more pattern modifiers should visibly bias toward blankets/leopards; `LP` with few/no modifiers should favor varnish, frost, snowflake, or more limited spotting according to the renderer.

8. Confirm every requested named disease locus is clear in the pure founder sample, including HYPP. No `SCN4A` variant should originate from a pure POA founder.

9. Test default lineage:
- Pony of the Americas × Pony of the Americas → Pony of the Americas.
- Pony of the Americas × Appaloosa → Pony of the Americas cross.
- Pony of the Americas × Quarter Horse → Pony of the Americas cross.
- POA cross × pure POA → the same existing POA cross label.
- Two different cross labels → Mixed.
- Any cross involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or gene editing. Add tobiano, frame, splash, Sabino 1, or a generic KIT white allele to a POA line. The altered animal may remain visually striking but should be a nonstandard descendant and, under real POAC rules, would be ineligible for normal registration. Conversely, an `LP`-negative solid descendant should not read as a fully regular spotted POA, even if the mod preserves the lineage label for gameplay.

## Sources

- [Pony of the Americas Club — Official breed website](https://www.poac.org/): national breed association and registry. [poac](https://www.poac.org/)

- [Pony of the Americas Club — Registration types](https://www.poac.org/club/registration-types): official size limits, registration pathways, pattern-characteristic requirements, and exclusion of Pinto/Paint color and parentage. [poac](https://www.poac.org/club/registration-types)

- [Pony of the Americas Club — POA characteristics](https://www.poac.org/breed/characteristics): official descriptions of leopard, few-spot, blanket, snowcap, spotted blanket, frost, snowflake, marbleized roan, and related accepted POA pattern types. [poac](https://www.poac.org/breed/characteristics)

- [Pony of the Americas Breed Profile](https://madbarn.ca/pony-of-the-americas-breed-profile/): accessible summary of 1954 Iowa development, size range, permitted pattern types, conformation, registration process, and HYPP-negative requirement. [madbarn](https://madbarn.ca/pony-of-the-americas-breed-profile/)

- [Pony of the Americas reference overview](https://en.wikipedia.org/wiki/Pony_of_the_Americas): supplementary source for Black Hand, breed history, 40-foot pattern criterion, Appaloosa characteristics, 11.2–14-hand height, and Paint/Pinto exclusion. [en.wikipedia](https://en.wikipedia.org/wiki/Pony_of_the_Americas)

- [PonyZine — Pony of the Americas breed portrait](http://www.ponyzine.info/pony-of-the-americas.html): supplementary account of ancestry, approved crossbreeding registries, performance uses, accepted pattern list, and pinto exclusion. [ponyzine](http://www.ponyzine.info/pony-of-the-americas.html)