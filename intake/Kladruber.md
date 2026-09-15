The **Kladruber**, more precisely the **Old Kladruber Horse** (*Starokladrubský kůň*), should be PHC’s grand ceremonial carriage breed: tall, baroque, Roman-nosed, powerful in harness, and genetically divided into a dominant-gray imperial strain and a solid-black ecclesiastical strain. It is not just “a gray horse from Czechia”—the two colors are conserved as historically distinct closed subpopulations, making them ideal for explicit strains in the breed definition. [nhkladruby](https://www.nhkladruby.cz/en/about-us)

## Identity & flavour

The **Old Kladruber Horse**, commonly called the **Kladruber**, is the oldest indigenous Czech horse breed and one of Europe’s oldest continuously maintained court-horse populations. Its home is the National Stud at **Kladruby nad Labem** in eastern Bohemia, Czech Republic, where horse breeding reaches back at least to the fourteenth century. Emperor Maximilian II established the imperial stud in 1563, and Emperor Rudolf II granted it formal Imperial Court Stud status on 6 March 1579. The modern Kladruber emerged in the late sixteenth and seventeenth centuries from Czech stock crossed with Iberian, Italian/Neapolitan, Danish, Holstein, Oldenburg, Irish, and related Baroque carriage-horse blood. [nhkladruby](https://www.nhkladruby.cz/en/about-us)

It was made for spectacle, precision, and serious ceremonial work. Kladrubers pulled the ornate state coaches of the Habsburg court in Prague and Vienna, moving in matched teams through processions, royal arrivals, funerals, and public ceremonies where a horse had to look imposing, stay level-headed, and work in exact harness formation. The gray—or historically “white,” once fully grayed—Kladrubers were the preferred horses of the royal and imperial court. The black strain became associated with church and clerical coaches. Today the gray strain still works ceremonially for the Swedish and Danish royal courts, while the breed remains a prestige carriage and driving horse in its homeland. [nhkladruby](https://www.nhkladruby.cz/en/about-us)

A Kladruber is a large, substantial **Baroque horse**, usually about **16.0–17.0 hands**, though some sources place the breed from around 15.3 up to 17.3 hands. It carries a long, strong body; broad, deep chest; powerful rounded quarters; substantial bone; an arched neck set high on a strong shoulder; and a broad, expressive head with the famous convex or **Roman nose**. The mane and tail are long and full, though not a special genetic “hair breed” feature. It should have little dramatic feather, sound large hooves, and an elevated, expressive trot that belongs in a carriage team rather than a racecourse. [en.wikipedia](https://en.wikipedia.org/wiki/Kladruber)

Modern pure Kladrubers are deliberately constrained to **gray and black**. Gray horses are born dark and become progressively pale with age because gray is a dominant, epistatic coat-color process; that means a genetically black, bay, or chestnut foundation can disappear beneath the mature gray phenotype. The black variety remains solid black and has historically been managed separately from the gray population. The National Stud maintains about 500 Kladrubers, with roughly 250 gray horses at Kladruby nad Labem and the black strain associated with Slatiňany. A player should identify a Kladruber herd either as an elegant black carriage team or as a group of tall gray horses whose foals begin dark before whitening. [nhkladruby](https://www.nhkladruby.cz/en/about-us)

The Kladruber temperament is expected to be steady, tractable, proud, willing, and dependable under noise, crowds, harness, and ceremonial pressure. Its notable athletic quality is not raw speed or maximal jumping power, but collected carriage movement: a strong, high, expressive trot; endurance in harness; and the mind to remain composed while pulling a vehicle in close company. The breed should feel more imposing and deliberate than an Anglo-Arabian, more refined than a heavy draft, and more ceremonially stately than a generic warmblood. [visitczechia](https://www.visitczechia.com/en-us/things-to-do/places/landmarks/unesco/c-kladruby-stud-farm-castle-and-landscape-unesco)

The breed has survived political collapse and a dangerously narrow population base. After the fall of the Habsburg Empire in 1918, demand for imperial coach horses fell sharply. The gray herd was retained as a Czech cultural treasure; the black herd was formally dissolved during the 1930s and later reconstructed from surviving mares and a few stallions, using carefully controlled restoration crosses before closure of the rebuilding program in 1973. Genetic work still identifies gray and black Kladrubers as distinct subpopulations, while finding that conservation management preserved molecular diversity despite bottlenecks and small numbers. In 2019, the landscape for breeding and training ceremonial carriage horses at Kladruby nad Labem became a UNESCO World Heritage Site. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/23649723/)

For *Procedural Horse Genetics*, this breed should feel like a prize discovery: not a broad all-purpose color pool, but a rare, towering, formal harness horse whose genome is either “born dark, goes gray” or “glossy black forever.” Players should breed toward a tall, healthy, powerful-driving individual with grand carriage and an unmistakable Roman-nosed silhouette—while remembering that PHC cannot simulate the silhouette itself. The mod does **not** model head profile, Roman nose, neck arch, mane fullness, Baroque body shape, high knee action, carriage training, paired/team driving, ceremonial nerve, sound of hoofbeats on cobbles, royal or church service, gray progression speed, or the real black-versus-gray stud-line structure beyond the strains below.

## Breed JSON

> **Schema note:** The PHC breed-format documentation supplied earlier could not be fetched by the browser. This is written in the same readable JSON convention as the examples in the conversation. Before compiling, reconcile exact locus IDs, colors, strains, enum values, and `grey` spelling with `common/breed/spec/`, `common/breed/Breeds`, and the live gene registry. In particular, do not force the base coat itself to black inside the gray strain: real gray is epistatic and can cover other base colors.

```json
{
  "id": "kladruber",
  "name": "Old Kladruber Horse",
  "type": "natural",
  "notes": "Procedural Horse Genetics models the Old Kladruber Horse's unusually tall carriage-horse frame, dominant gray and solid-black color strains, and stately harness-oriented athletic profile. It does not model the convex Roman nose, Baroque outline, arched neck, mane and tail fullness, high expressive knee action, collection, carriage-team coordination, ceremonial training, steady behavior in crowds, precise gray-whitening timeline, royal or ecclesiastical use, historic sire lines, or real studbook admission rules.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:flower_forest",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:windswept_hills"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1180,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 1.0
    },
    "agouti": {
      "a": 1.0
    },

    "grey": {
      "N": 0.52,
      "G": 0.48
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
    "speed": 5,
    "jump": 5,
    "health": 8,
    "size": [
      1.12,
      1.26
    ]
  },

  "epigenetic_bands": {
    "extension_black_intensity": [
      0.68,
      0.92
    ]
  },

  "strains": [
    {
      "id": "grey_kladruber",
      "name": "Grey Kladruber",
      "weight": 0.50,
      "coat_genes": {
        "extension": {
          "E": 1.0
        },
        "agouti": {
          "a": 1.0
        },
        "grey": {
          "G": 1.0
        }
      }
    },
    {
      "id": "black_kladruber",
      "name": "Black Kladruber",
      "weight": 0.50,
      "coat_genes": {
        "extension": {
          "E": 1.0
        },
        "agouti": {
          "a": 1.0
        },
        "grey": {
          "N": 1.0
        }
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Old Kladruber Horse × Old Kladruber Horse produces Old Kladruber Horse regardless of grey or black strain. Old Kladruber Horse × any other pure breed produces an Old Kladruber cross. A Kladruber cross bred back to a pure Kladruber remains that named cross; distinct crosses resolve to Mixed, and any pairing with Feral Mixed resolves to Mixed. Do not label a tall black or grey horse as Kladruber solely by appearance."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Rationale |
|---|---:|---|
| Breed type | One breed with two founder strains | The National Stud and genetic literature explicitly describe gray and black Kladrubers as two color variants/subpopulations with distinct recent breeding histories. They should remain one **Old Kladruber Horse** breed label, but use strains to preserve their unmistakable founder identities.  [nhkladruby](https://www.nhkladruby.cz/en/about-us) |
| Gray strain | `G: 1.0` | Gray is dominant and epistatic: a gray Kladruber foal begins dark and grays progressively. Treating the gray pool as guaranteed `G` gives every founder in the gray strain the historical “white Kladruber” adult outcome without falsely spawning white-bodied newborns if PHC renders gray aging.  [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2015/10/03.pdf) |
| Black strain | `E: 1.0`, `a: 1.0`, `G: 0.0` | The black variety should be a true, solid black strain. Fixing functional extension and recessive agouti gives black pigmentation, while forcing gray absent prevents later whitening. This is the cleanest genetic expression of the separate black breeding population.  [nhkladruby](https://www.nhkladruby.cz/en/about-us) |
| Overall gray frequency | `G: 0.48`, plus 50/50 strains | The root pool is intentionally close to balanced, while the strain system makes each individual founder genetically unambiguous. The National Stud describes about 250 gray horses among approximately 500 Kladrubers, but that figure describes its own managed population, not a universal global allele frequency; equal strain weights are therefore a transparent gameplay approximation.  [nhkladruby](https://www.nhkladruby.cz/en/about-us) |
| Extension and agouti | `E: 1.0`, `a: 1.0` | Modern black Kladrubers must be non-gray black. Keeping the shared base fixed black also makes gray foals visibly dark at birth before the gray locus acts. It is a breed-style decision, not a claim that every historical gray Kladruber carried only one concealed base color. |
| All other coat loci | Forced wild type | Modern Old Kladrubers are selectively restricted to black and gray. Cream, champagne, dun, silver, pearl, mushroom, tobiano, frame, splash, W-series white, leopard spotting, roan, rabicano, brindle, and magical traits should not originate in pure founders.  [en.wikipedia](https://en.wikipedia.org/wiki/Kladruber) |
| Black intensity band | `0.68–0.92` | This optional epigenetic band emphasizes the rich, glossy, near-ink black expected in the black variety and the dark foal coat of gray-strain horses. It is a pigment-depth band rather than a change to base-coat genotype or stat axes. |
| Disorders | All listed loci clear | No defensible Kladruber-specific carrier rates were identified for the prompt’s named disease panel. Research discusses markers relating to melanoma and insect-bite hypersensitivity in the breed, but does not establish rates for ACAN, PLOD1, MET, PRKDC, TOE1, MYO5A, GBE1, CVM, HYPP, GYS1, or PPIB. Do not invent them.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/23649723/) |
| Speed | 5/10 | The breed’s movement is expressive and powerful, but selection centered on ceremonial driving and harness reliability rather than racing. Keep raw speed baseline.  [visitczechia](https://www.visitczechia.com/en-us/things-to-do/places/landmarks/unesco/c-kladruby-stud-farm-castle-and-landscape-unesco) |
| Jump | 5/10 | Kladrubers can be versatile riding horses, but jumping is not the central historical or genetic selection target. Baseline jumping avoids making a carriage specialist into an unearned sport-horse specialist. |
| Health | 8/10 | This represents a managed, strong working horse capable of demanding harness work. It deliberately stops short of extreme hardiness because the breed is small, isolated, historically bottlenecked, and conservation-managed.  [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/23649723/) |
| Size | ×1.12–1.26 | This targets the usual 16–17 hand range and allows a few large individuals without reaching giant draft-horse proportions.  [en.wikipedia](https://en.wikipedia.org/wiki/Kladruber) |

### Important gray-strain note

The root-level `grey` pool exists to document the breed’s overall genetics, but the actual founder generator should select a strain first and then use that strain’s fixed value. In pseudocode:

```text
strain = weightedChoice(grey_kladruber: 0.50, black_kladruber: 0.50)
founderGenome = rollBaseKladruberGenome()
founderGenome.apply(strain.coat_genes)
```

If PHC instead merges the root pool and strain pool indiscriminately, remove root-level `grey` entirely and let the strains own it. The important behavior is that a founder is either genuinely **gray-strain** (`G/_`) or genuinely **black-strain** (`N/N`), not a probabilistic mixture after a named strain has already been chosen.

## Spawning & lineage

Kladrubers should appear in temperate lowland pasture and managed-park landscapes rather than wild alpine, desert, or tundra biomes. The selected plains, meadow, flower forest, broadleaf forest, birch forest, and modest windswept-hill pool evokes eastern Bohemia’s cultivated floodplain, ceremonial stud landscape, and carriage-horse training country. The actual Kladruby landscape is a designed agricultural and training landscape rather than a wilderness breed range, which is why `wild` is deliberately **not** on the source checklist. [whc.unesco](https://whc.unesco.org/en/list/1589/)

`VERY_RARE` at `spawn_weight: 0.75` fits a living cultural-heritage breed maintained through a specialist National Stud and conservation breeding program. In survival gameplay, a player should most often acquire a Kladruber through a stable, cowboy trader, or spawn egg; finding one naturally should be an exceptional event rather than a routine plains-horse roll. The National Stud itself maintains roughly 500 horses across the two color varieties. [nhkladruby](https://www.nhkladruby.cz/en/about-us)

The default lineage table is correct. A black Kladruber × gray Kladruber remains **Old Kladruber Horse** because the two are color strains within one breed. A Kladruber crossed with any other pure breed becomes a **Kladruber cross**, even if the foal is tall, convex-faced in a custom model, black, or gray. PHC should not use phenotype to recreate the real studbook’s pedigree requirements.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Adds the `kladruber` natural breed record with its notes, selected biomes, no-wild source policy, price, very-rare status, gene pool, stats, and strain list. |
| `common/breed/Breeds` | Registers `kladruber` so the record resolves for stable acquisition, cowboy sales, spawn eggs, lineage display, menus, books, entity saves, and commands. |
| `common/breed/BreedSource` | Validates the intentionally restricted source checklist: `cowboy`, `spawn_egg`, and `stable`; it must reject ordinary wild-pack generation for this breed. |
| `common/breed/BreedBands` | Stores and applies the optional `extension_black_intensity` range. Verify that this exact epigenetic target exists; otherwise omit it rather than inventing a key. |
| `common/breed/spec/` | Parses and writes coat genes, disease genes, stats, bands, and nested strains in both directions. Confirm exact vocabulary for `grey`, `G`, `N`, `weight`, and nested overrides. |
| `common/breed/Commonness` | Maps `VERY_RARE` to the rarity ladder’s 0.75 level and confirms that `spawn_weight: 0.75` is compatible with the code’s intended data model. |
| `common/breed/BreedStatCurve` | Converts speed 5, jump 5, health 8, and size ×1.12–1.26 into legal target bands. |
| `common/breed/BreedFounder` | Rolls the Kladruber root genome, selects either gray or black strain, applies the strain-specific gray state, and forces all excluded coat loci and disorders to wild type/clear. |
| `common/breed/BreedLineage` | Applies standard pure/cross/Mixed behavior, treating gray and black as strains under the single Kladruber lineage rather than separate breeds. |
| `common/genetics/SpliceOutcome` | Requires no Kladruber exception. A user-spliced allele transmits normally to descendants, even when it is excluded from pure Kladruber founders. |
| `common/trait/StatAxis` | Uses the existing speed, jump, health, and size axes. |
| `common/trait/TargetBand` | Stores generated numeric bands for the four body-stat targets. |
| `common/trait/BreedStatTargets` | Associates the Old Kladruber Horse’s size and performance profile with founder generation. |
| `common/breed/BandType` | Uses a normal band only if `extension_black_intensity` is supported. No special `TRADITIONAL` or `BACHELOR` behavior is required by the breed concept. |

## Verification

1. **Registry and acquisition**
   - Confirm **Old Kladruber Horse** appears in the H menu’s Breeds tab and the breed book with the correct display name, `VERY_RARE` status, 1,180 cowboy price, temperate biome list, and three allowed sources.
   - Verify it is obtainable through cowboy sales, stable generation, and spawn eggs.
   - Verify it does **not** enter normal wild-pack selection because `wild` is absent from the source checklist.

2. **Strain selection**
   - Generate at least 200 founders from a stable or spawn egg.
   - Confirm founder selection resolves into two visible populations: approximately half **Grey Kladruber** strain and half **Black Kladruber** strain.
   - Confirm strain metadata is inspectable or logged for testing, while the displayed breed label remains **Old Kladruber Horse** for both.
   - Confirm a gray-strain founder always carries at least one `G` allele and a black-strain founder is always `N/N` at gray.

3. **Coat behavior**
   - Confirm every black-strain founder is `E/E a/a N/N`, visually solid black, and stays black with age.
   - Confirm gray-strain founders begin from a dark black-based foal coat and follow PHC’s normal progressive gray system toward adult gray/white.
   - Confirm pure founders never produce bay, chestnut, palomino, buckskin, dun, champagne, silver dapple, mushroom, pearl, tobiano, frame, splash, W-series dominant white, roan, leopard spotting, rabicano, brindle, or magical effects.
   - Confirm the optional black-intensity band makes the black variety and gray foal coats read richly dark without eliminating normal shader or age variation.

4. **Within-breed crosses**
   - Black Kladruber × Black Kladruber should yield black Kladrubers only.
   - Gray Kladruber × black Kladruber should yield a normal dominant-gray inheritance outcome: approximately half gray offspring when the gray parent is heterozygous, or all gray offspring if it is homozygous.
   - Gray Kladruber × gray Kladruber should follow ordinary gray Mendelian inheritance, while all foals remain labeled **Old Kladruber Horse**.
   - Confirm breed strains never create cross or Mixed lineage labels merely because their parents are different colors.

5. **Disorders and stats**
   - Inspect founders and verify that every named disorder locus is clear.
   - Compare generated animals with baseline riding horses: Kladrubers should be tall, healthy, and powerful-looking, but ordinary in raw speed and specialized jumping.
   - Confirm body size clusters around tall riding-horse scale without reaching Shire-like giant scale.

6. **Lineage table**
   - Old Kladruber Horse × Old Kladruber Horse → **Old Kladruber Horse**.
   - Old Kladruber Horse × another pure breed → **Old Kladruber cross**.
   - Old Kladruber cross × pure Old Kladruber Horse → the same named cross.
   - Old Kladruber cross × a different cross → **Mixed**.
   - Any Old Kladruber lineage × Feral Mixed → **Mixed**.
   - Verify label behavior in the H menu, breed book, genome inspection, foal result UI, stable records, and across save/reload.

## Sources

- [Národní hřebčín Kladruby nad Labem / National Stud at Kladruby nad Labem — About us](https://www.nhkladruby.cz/en/about-us): official National Stud source for the Kladruber’s position as the oldest indigenous Czech horse breed, conservation of gray and black variants, approximately 500 horses, gray population at Kladruby nad Labem, Habsburg service, and cultural-heritage role. [nhkladruby](https://www.nhkladruby.cz/en/about-us)
- [UNESCO — Landscape for Breeding and Training of Ceremonial Carriage Horses at Kladruby nad Labem](https://whc.unesco.org/en/list/1589/): official history of the imperial stud, Maximilian II’s 1563 foundation, Rudolf II’s 1579 charter, and the cultural landscape’s global heritage status. [whc.unesco](https://whc.unesco.org/en/list/1589/)
- [The National Stud / VisitCzechia — Kladruby ceremonial carriage landscape](https://www.visitczechia.com/en-us/things-to-do/places/landmarks/unesco/c-kladruby-stud-farm-castle-and-landscape-unesco): modern ceremonial function, gray and black strain roles, gray carriage use by Scandinavian royal courts, and the separate Kladruby/Slatiňany associations. [visitczechia](https://www.visitczechia.com/en-us/things-to-do/places/landmarks/unesco/c-kladruby-stud-farm-castle-and-landscape-unesco)
- [Petersen et al., 2013 — “Genetic diversity and conservation in a small endangered horse population”](https://pubmed.ncbi.nlm.nih.gov/23649723/): peer-reviewed analysis of gray and black subpopulations, post-bottleneck diversity, selective breeding effects, and conservation management in Old Kladrubers. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/23649723/)
- [“Characterization of greying, melanoma, and vitiligo quantitative traits in Old Kladruber horses”](https://cjas.agriculturejournals.cz/pdfs/cjs/2015/10/03.pdf): scientific support for dominant, epistatic gray and the current two-color population structure of six gray and four black sire lines. [cjas.agriculturejournals](https://cjas.agriculturejournals.cz/pdfs/cjs/2015/10/03.pdf)
- [Czech Radio — Kladruby National Stud](https://english.radio.cz/kladruby-national-stud-breeding-unique-old-kladruber-horses-over-400-years-8617906): supplementary historical account of Habsburg ceremonial breeding and Spanish-blood roots. [english.radio](https://english.radio.cz/kladruby-national-stud-breeding-unique-old-kladruber-horses-over-400-years-8617906)