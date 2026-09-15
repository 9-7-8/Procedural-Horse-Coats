The **Missouri Fox Trotter** should be implemented as a broad-colored, medium-sized Ozarks gaited riding horse: sound, sure-footed, kind, and optimized for comfortable distance travel over broken country rather than for elite gallop or jumping. Unlike a color-restricted landrace, it is expressly identified by its gait and type—not its color—so pure founders should carry a wide but carefully evidence-bounded palette including ordinary base colors, cream, champagne, dun, silver, gray, roan, tobiano, sabino, and some overo-pattern genetics. [sos.mo](https://www.sos.mo.gov/symbol/horse)

## Identity & flavour

The **Missouri Fox Trotting Horse**, normally called the **Missouri Fox Trotter** or simply **MFT**, is an American gaited saddle breed from the rugged Ozark country of southern Missouri and northern Arkansas. It emerged in the early nineteenth century from the practical horses of settlers who needed mounts able to work cattle, cover long distances, hunt, carry families, and move confidently across steep, stony, wooded terrain. The Missouri Fox Trotting Horse Breed Association was organized in Ava, Missouri, in 1948; the horse became Missouri’s official state horse in 2002. [sos.mo](https://www.sos.mo.gov/symbol/horse)

The defining feature is the **fox trot**, a smooth four-beat broken-diagonal gait that replaces the ordinary trot in a properly gaited horse. In a fox trot, the front foot of a diagonal pair lands just before the opposite hind foot, producing a rhythmic “walk in front, trot behind” effect. At least one foot remains in contact with the ground, which reduces concussion and gives the rider a smooth, secure trip over trails and uneven ground. A correct Fox Trotter also performs a flat-footed walk and a canter; it is not defined by one coat color, a show style, or a particular bloodline alone. [sos.mo](https://www.sos.mo.gov/symbol/horse)

Most Missouri Fox Trotters stand **14–16 hands** and weigh roughly **900–1,200 lb**. They are strong but graceful saddle horses: a proud, well-proportioned head, straight facial profile, medium neck, defined withers, sloping shoulder, short strong back, substantial hindquarters, and sturdy, clean legs. They should have no heavy draft feather. The mane and tail are normally full, silky, and practical for a trail horse; curly-coated individuals occur, but curl is not required for breed identity and should not be seeded unless the mod has a specific, documented curly locus. [horseillustrated](https://www.horseillustrated.com/horse-breed-missouri-fox-trotter/)

The breed’s coat policy is unusually open. Missouri Fox Trotters may be registered in solid colors and pinto patterns; the official rulebook specifically recognizes champagne and roan categories, while broader breed material lists bay, brown, black, chestnut, palomino, buckskin, cremello, dun, gray, champagne, roan, tobiano, sabino, and overo-patterned horses. Silver also occurs and is popular, but it is genetically important: the `PMEL` silver variant is associated with **multiple congenital ocular anomalies** (MCOA), a congenital eye disorder. A pure MFT population can therefore legitimately create silver dapple, champagne, cream, dun, roan, gray, and pinto horses—but it should not casually produce leopard complex or other unsupported patterns. [miller-ranch](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf)

In Procedural Horse Genetics, a Missouri Fox Trotter should feel like the player’s dependable long-range trail partner: quicker and smoother-feeling than a basic work horse, durable enough for daily exploration, and dramatically more colorful than a restricted studbook breed. The visual tell is a compact, strong, medium-height American trail horse in nearly any plausible coat, moving with a calm head nod and an even, ground-hugging rhythm. The mod does not model the fox trot, flat-foot walk, headshake, sliding hind action, rider comfort, gait registration standards, gait training, stock sense, trail judgment, curly hair, subjective disposition, show-ring performance, or the breed’s separate 11–14-hand pony registry. [mfthba](https://mfthba.com/the-breed/gaits/)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file follows the readable convention in the supplied Anglo-Arabian example. Before merging, match all keys, source values, locus names, allele notation, `Commonness` values, and stat-band syntax to the actual `common/breed/spec/` serializer and parser.
>
> **Frequency caveat:** The Missouri Fox Trotter registry recognizes a broad range of colors, but I did not find a population-wide, peer-reviewed genotype survey that provides breed-level allele frequencies for every relevant locus. The rates below are deliberately labeled **gameplay approximations**, constrained by documented colors and patterns; they are not claimed to be official breed frequencies. `silver` is included because it is documented in the breed and scientifically associated with MCOA, but its low rate reflects the lack of a reliable population estimate. [miller-ranch](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf)

```json
{
  "id": "missouri_fox_trotter",
  "name": "Missouri Fox Trotter",
  "type": "natural",
  "notes": "The Missouri Fox Trotter is defined by its naturally smooth fox trot, flat-foot walk, canter, practical Ozarks trail-horse type, and registry inspection rather than a narrow coat palette. Procedural Horse Genetics does not model the broken-diagonal timing of the fox trot, head nod, sliding hind action, rider comfort, gait quality, gait training, show-ring performance, stock sense, trail judgment, curly coat, separate Missouri Fox Trotter Pony registry, or subjective temperament.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest",
    "minecraft:windswept_hills",
    "minecraft:river"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 760,
  "commonness": "COMMON",

  "coat_genes": {
    "extension": {
      "E": 0.68,
      "e": 0.32
    },
    "agouti": {
      "A": 0.64,
      "a": 0.36
    },

    "grey": {
      "N": 0.91,
      "G": 0.09
    },
    "cream": {
      "N": 0.76,
      "Cr": 0.24
    },
    "champagne": {
      "N": 0.94,
      "Ch": 0.06
    },
    "dun": {
      "N": 0.94,
      "D": 0.06
    },
    "silver": {
      "N": 0.94,
      "Z": 0.06
    },
    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.90,
      "f": 0.10
    },

    "tobiano": {
      "N": 0.84,
      "TO": 0.16
    },
    "sabino_1": {
      "N": 0.86,
      "SB1": 0.14
    },
    "frame_overo": {
      "N": 0.97,
      "O": 0.03
    },
    "splash_white_1": {
      "N": 0.96,
      "SW1": 0.04
    },
    "splash_white_2": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 1.0
    },
    "roan": {
      "N": 0.88,
      "Rn": 0.12
    },
    "rabicano": {
      "N": 0.96,
      "Rb": 0.04
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
    "PPIB_MCOA_silver": {
      "N": 0.94,
      "Z_MCOA": 0.06
    },

    "ACAN_dwarfism": {
      "N": 1.0
    },
    "PLOD1_friesian_dwarfism": {
      "N": 1.0
    },
    "MET_lethal_white": {
      "N": 0.97,
      "O": 0.03
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
      0.99,
      1.11
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Missouri Fox Trotter × Missouri Fox Trotter produces Missouri Fox Trotter. Missouri Fox Trotter × another pure breed produces a Missouri Fox Trotter cross. A Missouri Fox Trotter cross bred back to a pure Missouri Fox Trotter remains that cross under the default system. Different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not infer breed identity from a fox-trotting animation, a pinto coat, or an Ozarks biome alone; real MFT registration is based on pedigree, type, and gait."
  }
}
```

## Important correction

The `disorder_genes` block above must **not** be used literally if the mod keeps MCOA, silver, frame overo, and HERDA as separate locus systems.

There are two errors in a naïve schema:

- **MCOA is not caused by `PPIB`.** `PPIB` is the gene associated with HERDA, a different disorder chiefly documented in specific Quarter Horse cutting-horse lines.
- **Silver/MCOA is associated with `PMEL`**, historically called `PMEL17`. The same missense variant produces silver dilution and is associated with MCOA. It should be represented once—ideally through the existing `silver` locus plus a phenotype/disorder consequence—not duplicated as an independent `PPIB_MCOA_silver` disease locus. [journals.plos](https://journals.plos.org/plosone/article/file?id=10.1371/journal.pone.0075639&type=printable)

Likewise, `MET` is the locus linked to frame overo and lethal white syndrome. If the mod’s `frame_overo` coat locus already drives the lethal homozygous outcome, do **not** repeat it in `disorder_genes`. The correct integrated implementation is:

```json
"silver": {
  "N": 0.94,
  "Z": 0.06
},
"frame_overo": {
  "N": 0.97,
  "O": 0.03
}
```

…and then allow the mod’s real `PMEL`/MCOA and `EDNRB`/lethal-white logic to operate from those loci. No standalone MCOA or MET disorder entry should be added unless the actual schema explicitly requires one.

## Corrected disorder block

Because the requested disorder list contains no `PMEL/MCOA` entry, and because the `silver` and `frame_overo` coat loci should own those effects, the scientifically clean standalone disorder pool is:

```json
{
  "ACAN_dwarfism": {
    "N": 1.0
  },
  "PLOD1_friesian_dwarfism": {
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
}
```

However, that means the top-level full JSON should be amended by replacing its entire `disorder_genes` object with the corrected block above. The MFT’s silver allele then remains medically meaningful through the mod’s `PMEL` implementation, while the specific MCOA study’s authors caution that breed-wide silver allele frequency still needs dedicated screening. [thieme-connect](https://www.thieme-connect.com/products/ejournals/abstract/10.1055/a-1581-4810)

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed identity | Broad, gait-first founder pool | The MFT has historically been identified by a required gait suite and conformational type, not by a restricted color class. Its registry accepts a wide coat range.  [sos.mo](https://www.sos.mo.gov/symbol/horse) |
| Base colors | `E: 0.68 / e: 0.32`; `A: 0.64 / a: 0.36` | Produces common bay, brown, black, chestnut, and sorrel outcomes. These values are gameplay estimates because no robust all-locus MFT population survey was located.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Cream | `Cr: 0.24` | Buckskin, palomino, cremello, and perlino-type outcomes are widely recognized in MFTs, making cream a relatively common dilution in the game pool.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Champagne | `Ch: 0.06` | The official rulebook contains detailed recognized champagne categories, including classic, amber, gold, and ivory champagne. Keep it present but uncommon.  [miller-ranch](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf) |
| Dun | `D: 0.06` | Dun is documented among the breed’s recognized colors but should be much less frequent than ordinary bases and cream dilutions.  [youngrider](https://www.youngrider.com/meet-the-missouri-fox-trotter/) |
| Silver | `Z: 0.06` | Silver occurs and is popular enough for a documented MCOA case report. It should remain low pending a true MFT population-frequency survey. Its medical association belongs to `PMEL`, not `PPIB`.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Gray | `G: 0.09` | Gray occurs in breed descriptions and is an appropriate minority allele.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Tobiano | `TO: 0.16` | Tobiano is documented and accepted. It receives the largest pinto-pattern rate because it is an ordinary, recognized MFT phenotype.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Sabino 1 | `SB1: 0.14` | Sabino is documented in MFT color descriptions; a moderate rate creates common white facial/leg markings and variable sabino expression.  [youngrider](https://www.youngrider.com/meet-the-missouri-fox-trotter/) |
| Frame overo | `O: 0.03` | Overo is reported in broad breed-color references, but a low rate is safer because the `EDNRB` homozygous state causes lethal white syndrome. Treat this as an explicit gameplay approximation, not a published MFT frequency.  [youngrider](https://www.youngrider.com/meet-the-missouri-fox-trotter/) |
| Splash white | `SW1: 0.04` | Kept at low frequency as a cautious component of an open pinto population; it should be removed if the actual registry/rulebook does not recognize splash-derived phenotypes in its color categories. |
| Roan | `Rn: 0.12` | The official rulebook gives specific roan standards and categories, supporting a visible but minority roan population.  [miller-ranch](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf) |
| Rabicano | `Rb: 0.04` | A low modifier rate adds modest tail and flank white without overwhelming the better documented pinto and roan mechanisms. This is gameplay texture, not a measured breed frequency. |
| Excluded loci | Pearl, mushroom, W-series white, leopard complex, PATN, brindle, magical loci forced wild type | No evidence found supports seeding them in a pure MFT founder pool. Leopard complex in particular is not part of the documented MFT color range. |
| PSSM1 | Do not seed without a rate | UC Davis offers a PSSM1 test for MFTs and notes a variant specific to MFT/Tennessee Walking Horse/Standardbred context, but this is not a carrier-prevalence estimate. The breed-specific data found supports testing, not a numerical founder rate.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/pssm1) |
| MCOA | Derived from `silver` / PMEL | A Missouri Fox Trotter stallion heterozygous for the silver `PMEL` variant was reported with MCOA; PMEL silver is scientifically linked to the syndrome. This supports gene-linked disease logic but not a population prevalence claim.  [journals.plos](https://journals.plos.org/plosone/article/file?id=10.1371/journal.pone.0075639&type=printable) |
| Speed | `7/10` | Captures an efficient, trail-ready gait that can travel about 10 mph, without representing racehorse sprint capacity.  [horseillustrated](https://www.horseillustrated.com/horse-breed-missouri-fox-trotter/) |
| Jump | `5/10` | The breed is balanced, athletic, and practical but not selected principally for show jumping. |
| Health | `8/10` | Represents soundness, sure-footedness, and sustained usefulness over rugged country—not immunity to PSSM1, MCOA, metabolic disease, orthopedic problems, or ordinary injury.  [madbarn](https://madbarn.com/missouri-fox-trotter-breed-profile/) |
| Size | `×0.99–1.11` | Centers founders around the documented 14–16-hand saddle-horse range.  [en.wikipedia](https://en.wikipedia.org/wiki/Missouri_Fox_Trotter) |

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `missouri_fox_trotter` with its natural classification, Ozarks-focused biome pool, sources, commonness, price, notes, gene pools, and stat targets. |
| `common/breed/Breeds` | Register the breed ID for commands, books, menus, pack spawning, genome persistence, and lineage naming. |
| `common/breed/BreedSource` | Validate all four source modes: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Support an empty epigenetic-band object. Do not attempt to fake a gait with a coat-gene epigenetic band. |
| `common/breed/spec/` | Reconcile this illustrative object with the actual schema, especially `PMEL`/silver/MCOA linkage, `EDNRB`/frame/lethal-white linkage, all allele IDs, and whether omitted genes default correctly to wild type. |
| `common/breed/Commonness` | Confirm the intended `COMMON` ladder entry and retain direct spawn weight `6` only if the implementation supports it. |
| `common/breed/BreedStatCurve` | Translate speed 7, jump 5, health 8, and size ×0.99–1.11 into valid target bands. |
| `common/breed/BreedFounder` | Draw founder alleles only from the listed broad coat pool; force unsupported cosmetic loci and unsubstantiated standalone disease loci clear. |
| `common/breed/BreedLineage` | Use the normal pure/cross/Mixed table. A horse should not acquire an MFT label simply by carrying a gait gene or a champagne-pinto phenotype. |
| `common/genetics/SpliceOutcome` | No exception. Normal splice-carrot inheritance can create atypical descendants, including genes excluded from pure MFT founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define medium-frame trail-horse targets for speed, jumping, health, and size. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` epigenetic band is necessary unless the actual schema demands an explicit default. |

## Verification

1. Confirm **Missouri Fox Trotter** appears in the H-menu’s Breeds tab and breed book with its Ozarks origin, commonness, full source checklist, medium size, endurance-oriented stats, and fox-trot caveat.

2. Spawn repeated packs in eligible Ozarks-like terrain: meadow, plains, forest, dark forest, birch woodland, river edge, and windswept hills. Every member of a given selected breed pack should display **Missouri Fox Trotter**.

3. Confirm a lone ordinary wild horse still reads **Feral Mixed**, even when it happens to be palomino, champagne, pinto, or roan.

4. Generate at least 500 pure founders. The population should produce:
- Bay, black/brown, chestnut, and sorrel foundations.
- Regular palomino, buckskin, cremello/perlino, champagne, dun, gray, silver, and roan minority phenotypes.
- Tobiano and sabino commonly enough to make white-marked and pinto horses recognizable.
- Occasional frame-derived and splash-derived patterns only if those exact loci are accepted by the actual mod schema and intended registry interpretation.

5. Confirm no founder produces pearl, mushroom, leopard-complex spotting, PATN patterning, brindle, W-series dominant white, magical coats, or other genes not in the file.

6. Verify medical linkage rather than duplicate genes:
- `Z/N` silver horses should invoke the mod’s intended heterozygous `PMEL`/MCOA behavior, if modeled.
- `Z/Z` offspring should invoke the more severe MCOA-risk behavior expected from the mod’s medical design, if modeled.
- `O/O` frame-overo offspring should use the mod’s normal lethal-white logic, if frame and `MET` are integrated.
- No independent `PPIB_MCOA_silver` locus should exist.

7. Check the standalone disorder panel across a large founder sample. Only disease loci that have direct MFT evidence and a correctly implemented underlying genetic cause should vary; all others remain clear. If the mod cannot link silver to MCOA or frame to lethal white internally, document that as an engine limitation rather than inventing duplicate loci.

8. Test default lineage:
- Missouri Fox Trotter × Missouri Fox Trotter → Missouri Fox Trotter.
- Missouri Fox Trotter × Tennessee Walking Horse → Missouri Fox Trotter cross.
- Missouri Fox Trotter × American Paint Horse → Missouri Fox Trotter cross.
- MFT cross × pure MFT → the same MFT cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test gameplay performance. Mature MFTs should feel medium-sized, quick, durable, and dependable for long overland rides, but they should not surpass specialist Thoroughbreds in raw speed, dedicated jumpers in jumping, or draft horses in size/traction. Their real defining advantage—the fox trot’s rider comfort—belongs in flavour, breed-book text, or future gait mechanics rather than an invented coat or disease mechanic.

## Sources

- [Missouri Secretary of State — State Horse](https://www.sos.mo.gov/symbol/horse): early nineteenth-century Ozarks origin, trail and work role, 1948 association history, 2002 state-horse designation, and required gait suite. [sos.mo](https://www.sos.mo.gov/symbol/horse)

- [Missouri Fox Trotting Horse Breed Association — Gaits](https://mfthba.com/the-breed/gaits/): official description of the fox trot’s broken-diagonal rhythm, continuous ground contact, headshake, and movement mechanics. [mfthba](https://mfthba.com/the-breed/gaits/)

- [Missouri Fox Trotting Horse Breed Association — The Breed](https://mfthba.com/the-breed/): official association’s central breed-information page. [mfthba](https://mfthba.com/the-breed/)

- [Horse Illustrated — Breed Portrait: Missouri Fox Trotter](https://www.horseillustrated.com/horse-breed-missouri-fox-trotter/): height, overall conformation, gaits, approximately 10-mph fox-trot speed, pinto acceptance, and 2004 pony registry. [horseillustrated](https://www.horseillustrated.com/horse-breed-missouri-fox-trotter/)

- [Missouri Fox Trotting Horse Breed Association Rule Book, color standards](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf): recognized champagne and roan descriptions, including classic/amber/gold/ivory champagne and multiple roan classes. [miller-ranch](https://www.miller-ranch.com/wp-content/uploads/2020/10/2020-MFTHBA-Rule-Book-1.pdf)

- [UC Davis Veterinary Genetics Laboratory — Missouri Fox Trotter](https://vgl.ucdavis.edu/breed/missouri-fox-trotter): availability of breed-relevant genetic testing, including PSSM1 context. [vgl.ucdavis](https://vgl.ucdavis.edu/breed/missouri-fox-trotter)

- [Multiple Congenital Ocular Anomalies in a silver coat Missouri Fox Trotter stallion](https://www.thieme-connect.com/products/ejournals/abstract/10.1055/a-1581-4810): first published MCOA report in a silver MFT; the stallion was heterozygous for the `PMEL` silver-associated mutation and the authors recommend breed-frequency screening. [thieme-connect](https://www.thieme-connect.com/products/ejournals/abstract/10.1055/a-1581-4810)

- [PLOS ONE — Equine MCOA and silver coat color](https://journals.plos.org/plosone/article/file?id=10.1371/journal.pone.0075639&type=printable): scientific basis linking the `PMEL` missense mutation to both silver coat dilution and MCOA syndrome. [journals.plos](https://journals.plos.org/plosone/article/file?id=10.1371/journal.pone.0075639&type=printable)

- [Online Mendelian Inheritance in Animals — PMEL/MCOA](https://omia.org/OMIA000733/9796/): current genomic and clinical catalog entry documenting the `PMEL` variant, silver phenotype, MCOA association, and Missouri Fox Trotter inclusion among affected breeds. [omia](https://omia.org/OMIA000733/9796/)