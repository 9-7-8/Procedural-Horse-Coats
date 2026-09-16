The **Swedish Warmblood** should be implemented as a tall, athletic Swedish sport-horse studbook: bay, chestnut, gray, and black should dominate; ordinary white markings can occur; and rare cream dilution or rare tobiano is plausible only as a minor by-product of an open performance-breeding population. Its signature is not coat color but the balance of rideability, elastic movement, jumping capacity, and dressage aptitude developed through Sweden’s royal studs and modern Swedish Warmblood Association selection. The most defensible disease locus is recessive **WFFS** at `PLOD1`, for which a random 2017 Swedish Warmblood sample reported **7.4% carriers**. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

## Identity & flavour

The **Swedish Warmblood**, traditionally abbreviated **SWB** and formerly organized as the Swedish Warmblood Association or *Avelsföreningen för Svenska Varmblodiga Hästen*, is Sweden’s principal modern sport-horse population. Its foundations reach into the seventeenth century, when Sweden’s state and royal studs began combining local mares with imported European stallions to create cavalry and carriage horses. The most influential historic institutions were Strömsholm, founded in 1621; Ottenby on Öland; and Flyinge in Skåne, established in the seventeenth century. A formal studbook began in 1874, while the national breeders’ association formed in 1928. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

The early Swedish Warmblood was meant to serve military and practical saddle purposes: it needed courage, soundness, carrying ability, obedience, and a useful ride across Sweden’s varied terrain. During the nineteenth and twentieth centuries, military need gave way to sport. Thoroughbred, Trakehner, Hanoverian, Anglo-Norman, Arabian, and other European performance lines added height, refinement, movement, scope, and gallop. The modern SWB is therefore a performance-led warmblood, selected for dressage, show jumping, eventing, and high-level riding rather than for a preserved historical farm-horse type. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

A Swedish Warmblood commonly stands **16–17 hands**, roughly **164–170 cm** at the withers. Breeding goals cited for stallions sit around 165–172 cm and for mares around 164–170 cm. The horse should be large but athletic: a noble, expressive head; long, well-set neck; sloping shoulder; pronounced withers; deep body; strong back and loins; powerful hindquarters; clean limbs; and an uphill, balanced outline. It should have no feathering and no unusual mane or tail gene. At a glance, it ought to read as an elegant Scandinavian sport horse: enough substance to jump and carry, enough elasticity to collect, and enough refinement to look convincing in a dressage ring. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

The Swedish Warmblood may be **any solid coat color**, but bay, chestnut, gray, and black are the most familiar and common. White face and leg markings are ordinary; the studbook’s solid-color language means that modest marking expression is appropriate, but pure founders should not create a population dominated by loud pinto or leopard spotting. Cream dilution can appear at low frequency, allowing occasional palomino, buckskin, cremello, or perlino horses. Tobiano and silver dapple are reported as rare in supplementary breed records, but should remain exceptional in a general Swedish founder pool. Roan should be excluded because it is not ordinarily part of the traditional SWB color population. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

Swedish Warmbloods are expected to be intelligent, willing, trainable, energetic, and balanced enough for serious amateurs as well as elite riders. In Procedural Horse Genetics, players should breed toward a tall, refined, all-purpose European sport horse with excellent jumping, strong speed, respectable health, and colors that remain mostly bay, chestnut, black, or gray. The mod does not model SWB stallion licensing, mare quality inspections, young-horse tests, dressage collection, jumping style, elasticity, rideability, courage, gaits, professional training, pedigree approval, sport results, or the difference between a pleasant riding horse and an Olympic prospect. [madbarn](https://madbarn.com/swedish-warmblood-breed-profile/)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file follows the readable JSON convention from the supplied Anglo-Arabian example. Before compiling, reconcile exact field names, source enum values, locus identifiers, allele notation, commonness enum names, price units, and `TargetBand` serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The Swedish Warmblood Association and breed sources establish height, breeding history, sport focus, and the accepted solid color range, but I did not locate a modern representative SWB-wide coat-genotype survey for Extension, Agouti, gray, cream, or white-pattern loci. The coat pools below are therefore transparent **gameplay approximations**, not Swedish Warmblood Association allele-frequency statistics. The WFFS number is evidence-based: 38 of 511 randomly selected Swedish Warmbloods born in 2017 were carriers, or **7.4%**. Converting carrier prevalence to a random founder allele frequency gives \(q \approx 0.0385\). [huveta](https://huveta.hu/bitstreams/32afdd4e-89c5-4a7b-a5aa-230d56acf06a/download)

```json
{
  "id": "swedish_warmblood",
  "name": "Swedish Warmblood",
  "type": "natural",
  "notes": "The Swedish Warmblood is a Swedish performance warmblood defined by approved pedigree, stallion licensing, mare assessment, performance testing, correct conformation, rideability, movement, jumping scope, dressage aptitude, training, and sport results rather than a fixed coat color or closed ancestral population. Procedural Horse Genetics does not model breeding-value indices, licensing, mare inspections, young-horse tests, dressage collection, jump technique, carefulness, elasticity, gait quality, rider compatibility, professional training, competition records, or the distinction between a talented sport prospect and an elite international horse.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:flower_forest",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:taiga",
    "minecraft:grove",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1380,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.66,
      "a": 0.34
    },

    "grey": {
      "N": 0.84,
      "G": 0.16
    },
    "cream": {
      "N": 0.96,
      "Cr": 0.04
    },
    "silver": {
      "N": 0.99,
      "Z": 0.01
    },
    "dun": {
      "N": 1.0
    },
    "champagne": {
      "N": 1.0
    },
    "pearl": {
      "N": 1.0
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.96,
      "f": 0.04
    },

    "tobiano": {
      "N": 0.99,
      "TO": 0.01
    },
    "sabino_1": {
      "N": 0.95,
      "SB1": 0.05
    },
    "frame_overo": {
      "N": 1.0
    },
    "splash_white_1": {
      "N": 0.99,
      "SW1": 0.01
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
    "PLOD1_WFFS": {
      "N": 0.9615,
      "WFFS": 0.0385
    },

    "ACAN_dwarfism": {
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
    "speed": 7,
    "jump": 8,
    "health": 7,
    "size": [
      1.10,
      1.22
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Swedish Warmblood × Swedish Warmblood produces Swedish Warmblood. Swedish Warmblood × another pure breed produces a Swedish Warmblood cross. A Swedish Warmblood cross bred back to pure Swedish Warmblood remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not infer Swedish Warmblood status from a tall bay, chestnut, gray, or black sport-horse appearance: real SWB identity requires approved ancestry and association-controlled breeding status."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed model | Managed sport-horse studbook | The modern SWB was created through state-stud breeding, organized inspection, selective imports, and performance selection. It is a sport population, not a native feral herd.  [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.66 / a: 0.34` | Produces a founder population centered on bay, chestnut, black, and brown. These are the classic Swedish Warmblood colors, but the values are gameplay approximations rather than a published Swedish genotype survey.  [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood) |
| Gray | `G: 0.16` | Gray is one of the four repeatedly named common SWB colors and should appear regularly, but not replace the bay/chestnut core.  [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood) |
| Cream | `Cr: 0.04` | Rare cream-dilute Swedish Warmbloods are documented in supplementary breed records. This permits occasional palomino, buckskin, cremello, perlino, or smoky cream without making them typical.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Swedish_Warmblood.php) |
| Silver | `Z: 0.01` | Silver dapple is reported as rare. The low rate is optional and should be removed if the engine cannot correctly couple `PMEL` silver to its MCOA consequences.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Swedish_Warmblood.php) |
| Flaxen | `f: 0.04` | Allows rare flaxen chestnut expression. This is a modest gameplay modifier, not a documented breed frequency.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Swedish_Warmblood.php) |
| Tobiano | `TO: 0.01` | Supplementary references describe tobiano as rare rather than impossible. A 1% allele rate makes it exceptional and preserves the solid-color overall identity.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Swedish_Warmblood.php) |
| Sabino 1 | `SB1: 0.05` | Low Sabino 1 supports common white facial and leg markings without making the overall founder population broadly pinto. Exact allele prevalence is not known from the retrieved sources.  [madbarn](https://madbarn.com/swedish-warmblood-breed-profile/) |
| Splash white | `SW1: 0.01` | Very rare optional white-pattern contribution in a broad sport-horse population. Remove it if the real mod’s splash expression is too visibly pinto for the SWB registry interpretation. |
| Roan | Forced wild type | A supplementary Swedish Warmblood breed reference specifically notes that roan does not naturally occur in the breed and must be introduced through breeding. Pure founders should therefore carry no `Rn`.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Swedish_Warmblood.php) |
| Excluded loci | Dun, champagne, pearl, mushroom, frame, KIT W-series white, leopard complex, PATN, brindle, magical loci forced wild type | No source found justifies seeding these loci in a baseline pure Swedish Warmblood population. The open warmblood pedigree structure is not a reason to give every possible gene a positive frequency. |
| WFFS | `PLOD1_WFFS: 0.0385` | A random cohort of 511 Swedish Warmbloods born in 2017 had 7.4% carriers. Under Hardy–Weinberg assumptions, \(q \approx 0.0385\) produces \(2q(1-q) \approx 7.4\%\) heterozygous carriers and \(q^2 \approx 0.15\%\) homozygous affected conceptions.  [escholarship](https://escholarship.org/content/qt4n2326rr/qt4n2326rr.pdf) |
| Other disorders | Forced clear | No defensible Swedish Warmblood-specific carrier frequency was located for the rest of the requested disease panel. |
| Speed | `7/10` | Represents athletic movement, useful gallop, and eventing/dressage/jumping versatility without becoming a specialized Thoroughbred sprint score.  [madbarn](https://madbarn.com/swedish-warmblood-breed-profile/) |
| Jump | `8/10` | Jumping is one of the core intended sport disciplines of the modern SWB.  [madbarn](https://madbarn.com/swedish-warmblood-breed-profile/) |
| Health | `7/10` | Represents strength and soundness expected of a selected sport horse, moderated by the realities of large-athlete management and the intentionally seeded WFFS allele. |
| Size | `×1.10–1.22` | Models the typical 164–170 cm, 16–17-hand population and the stated breeding goals for mares and stallions.  [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood) |

## Disorder approach

The Swedish Warmblood file should seed exactly one independently modeled disorder locus: **Warmblood Fragile Foal Syndrome** (**WFFS**) at `PLOD1`.

A Swedish research sample provides a better basis than generic warmblood estimates:

- Of **511 randomly selected Swedish Warmbloods born in 2017**, **7.4%** were WFFS carriers. [huveta](https://huveta.hu/bitstreams/32afdd4e-89c5-4a7b-a5aa-230d56acf06a/download)
- `PLOD1` WFFS is recessive. Heterozygotes are clinically normal carriers; a foal inheriting two mutant alleles is severely affected, and the condition is generally lethal or leads to euthanasia shortly after birth.
- Carrier prevalence is not mutant allele frequency. With \(2q(1-q) = 0.074\), the practical solution is \(q \approx 0.0385\).
- The JSON uses `WFFS: 0.0385`, giving approximately:
  - **7.4%** `N/WFFS` carrier founders,
  - **0.15%** `WFFS/WFFS` affected genotypes under random pairing,
  - **92.45%** clear `N/N` founders.

The remaining listed disease loci are clear. Do not use a second `PLOD1_friesian_dwarfism` record: this is the same `PLOD1` locus relevant to WFFS in the mod’s disease system. Do not import HYPP, GBED, HERDA, SCID, CA, LFS, or PSSM1 frequencies from Quarter Horses, Arabians, Friesians, or other warmblood populations without Swedish-specific evidence.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `swedish_warmblood` with Swedish royal-stud history, sport-horse notes, managed acquisition sources, coat pools, WFFS pool, and stat bands. |
| `common/breed/Breeds` | Register the breed for stable/cowboy generation, H-menu display, breed books, commands, breeding, serialization, and saved-game loading. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally excluded because SWB is a registered, managed sport-horse population, not a natural feral herd. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map. Do not create a cosmetic epigenetic band to simulate elite movement, because dressage/jumping quality is not coat expression. |
| `common/breed/spec/` | Reconcile the sample keys with the actual bidirectional schema, especially the exact `PLOD1` WFFS identifier, `PMEL` silver behavior, and defaults for omitted loci. |
| `common/breed/Commonness` | Confirm that `UNCOMMON` matches the requested rarity ladder. Keep `spawn_weight: 3` only if this is a valid direct numerical spawn-weight field. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 8, health 7, and size ×1.10–1.22 to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only the declared coat and WFFS pools, force all excluded coat loci and unrelated disorders wild type or clear, and apply the tall sport-horse target bands. |
| `common/breed/BreedLineage` | Use normal pure/cross/Mixed behavior. A tall gray or bay sport horse must not receive Swedish Warmblood lineage solely from its phenotype. |
| `common/genetics/SpliceOutcome` | No exception. A successful splice allele passes normally and may create nonstandard descendants outside the baseline pure SWB population. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the tall, athletic, jumping-capable sport-horse target bands. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the final serializer requires an explicit default value. |

## Verification

1. Confirm **Swedish Warmblood** appears in the H-menu’s Breeds tab and breed book with its Swedish state-stud/modern sport-horse identity, tall frame, jumping emphasis, solid-color policy, WFFS note, and managed-only source checklist.

2. Confirm Swedish Warmblood does **not** spawn as a natural wild pack, because `wild` is absent. It should appear only through cowboy, stable, or spawn-egg sources.

3. Confirm an ordinary lone wild horse always reads **Feral Mixed**, even when it is large, bay, chestnut, black, gray, or mechanically athletic.

4. Generate at least 1,000 pure founders. The population should be dominated by bay, chestnut, black/brown, and gray. Rare cream dilution, occasional flaxen chestnut, and exceptionally rare silver, tobiano, splash, or sabino-derived expression may occur according to the supplied pools. No pure founder should generate roan.

5. Confirm no pure founder produces dun, champagne, pearl, mushroom, frame overo, W-series dominant white, leopard complex, PATN patterning, brindle, or magical effects.

6. Inspect founder genomes:
- `E/e`, `A/a`, and `G` should create the four principal colors.
- `Cr`, `Z`, `f`, `TO`, `SB1`, `SW1`, and `Rb` should be limited to their low listed frequencies.
- `Rn` must always be absent.
- All excluded pattern, dilution, magical, and disease loci must be wild type or clear.

7. Test WFFS:
- In a large randomly generated founder group, `N/WFFS` carriers should approach **7.4%**, allowing ordinary sampling variation.
- `N/WFFS × N/N` should produce approximately half carriers and half clear offspring.
- `N/WFFS × N/WFFS` should produce approximately 25% `WFFS/WFFS`, 50% carriers, and 25% clear outcomes.
- Verify that homozygous WFFS outcomes follow the mod’s fragile-foal/conception-loss logic once only, without duplicating the locus under a separate “Friesian dwarfism” label.

8. Test default lineage:
- Swedish Warmblood × Swedish Warmblood → Swedish Warmblood.
- Swedish Warmblood × Oldenburg → Swedish Warmblood cross.
- Swedish Warmblood × Selle Français → Swedish Warmblood cross.
- Swedish Warmblood cross × pure Swedish Warmblood → the existing Swedish Warmblood cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test mature stats. Swedish Warmbloods should be tall, athletic, fast enough for modern sport, and very good jumpers. They should not exceed specialist racehorses in raw speed, giant draft breeds in size, or dedicated low-input native horses in survival hardiness.

## Sources

- [Swedish Warmblood breed history and standards overview](https://en.wikipedia.org/wiki/Swedish_Warmblood): state-stud history, 1621 Strömsholm foundation, Flyinge and Ottenby influence, 1874 studbook, 1928 association formation, 164–170 cm height, solid-color statement, and performance selection. [en.wikipedia](https://en.wikipedia.org/wiki/Swedish_Warmblood)

- [Breed evolution and development of the Swedish Warmblood Horse](https://huveta.hu/bitstreams/32afdd4e-89c5-4a7b-a5aa-230d56acf06a/download): breeding-goal height bands for stallions and mares, plus Swedish Warmblood breeding-program context. [huveta](https://huveta.hu/bitstreams/32afdd4e-89c5-4a7b-a5aa-230d56acf06a/download)

- [Oklahoma State University — Swedish Warmblood Horses](https://breeds.okstate.edu/horses/swedish-warmblood-horses): Swedish Warmblood Association formation, Swedish army encouragement, and history of quality/inspection goals. [breeds.okstate](https://breeds.okstate.edu/horses/swedish-warmblood-horses)

- [MadBarn — Swedish Warmblood Breed Guide](https://madbarn.com/swedish-warmblood-breed-profile/): practical breed overview of royal-stud history, 16–17-hand range, common bay/chestnut/gray/black colors, solid-color rule, modern sport disciplines, and breeding selection. [madbarn](https://madbarn.com/swedish-warmblood-breed-profile/)

- [Distribution of the Warmblood Fragile Foal Syndrome Type 1 mutation in various horse breeds](https://escholarship.org/content/qt4n2326rr/qt4n2326rr.pdf): WFFS mutation distribution study and Swedish Warmblood carrier-frequency context. [escholarship](https://escholarship.org/content/qt4n2326rr/qt4n2326rr.pdf)