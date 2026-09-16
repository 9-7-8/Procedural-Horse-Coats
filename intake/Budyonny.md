The **Budyonny** should be represented as a rare Russian cavalry-to-sport riding horse: tall, powerful, resilient, and overwhelmingly **chestnut**, often with the prized golden or metallic sheen inherited through Don-horse influence. Its pure founder pool should allow chestnut as the strong core, bay/brown as real minorities, and black only if the mod favors a broader historical reading; gray, pinto, leopard complex, cream, dun, silver, champagne, and magical coats should remain absent. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

## Identity & flavour

The **Budyonny**, also rendered **Budenny**, *Budennovskaya loshad* (Будённовская лошадь), or Budyonny Horse, is a Russian light riding breed developed in the Rostov region after the First World War and Russian Civil War. It was named for Marshal **Semyon Budyonny**, commander of the First Cavalry Army and a central figure in Soviet remount breeding. Beginning in the early 1920s, state studs rebuilt cavalry horse numbers using local Don mares crossed primarily with English Thoroughbred stallions, then selected the resulting population for size, courage, gallop, endurance, and military usefulness. The breed was officially recognized in **1949**. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

The Budyonny was made as a cavalry horse first. It had to carry a soldier across long distances, remain sound through poor weather and limited feed, recover after hard work, gallop with purpose, and keep enough courage and mind for military conditions. In later decades, as cavalry disappeared, the same qualities translated into sport: racing, eventing, show jumping, dressage, endurance, pleasure riding, and general athletic work. A Budyonny should feel like a tougher, more substantial Russian riding horse than a pure Thoroughbred—fast enough to be exciting, strong enough to be dependable, and hardy enough for rough country. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

Budyonny stallions average about **165 cm**, roughly **16.1 hands**, while mares average around **163 cm**, roughly **16 hands**. They are tall, long-lined, muscular horses with a well-proportioned straight-profiled head, long neck, pronounced withers, long sloping shoulder, wide deep chest, long straight back, broad powerful loin, muscular slightly sloping croup, and clean strong limbs. The build is athletic rather than drafty: it should carry more bone and substance than a fine racehorse, but remain unmistakably a riding horse built for a forward gallop. Mane and tail are full but ordinary, with no feathering and no special hair gene. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

The signature color is **chestnut**, often deep red-gold or golden chestnut with an admired metallic sheen. More than 80% of the breed is reported as chestnut in breed-conservancy material. Bay and brown also occur, while the historical broad summary sometimes lists gray and black; however, a strict modern breed-style implementation should follow the chestnut-led Don/Thoroughbred population and exclude gray, black, splash, and leopard-patterned founders unless direct studbook data supports them. The golden shine is not cream dilution, champagne, silver, pearl, or a magical metallic gene—it is a subtle coat-quality effect beyond ordinary Mendelian base-color rendering. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

Early Budyonny breeding recognized three types: the larger, more muscular **Massive** type; the refined, Arab- or Don-like **Eastern** type; and the balanced, athletic **Middle** type. In game, these work well as internal founder strains: Massive for a larger, stronger cavalry body; Eastern for a slightly lighter and more elegant type; Middle as the common all-purpose standard. Players should breed toward the unmistakable Budyonny combination of chestnut-gold coat, tall soldier-horse outline, useful speed, high health, and athletic versatility. The mod does not model metallic sheen, Don-horse gold tone, cavalry training, courage, long gallop mechanics, historical Soviet breeding, exact body type, gait quality, race conditioning, sport training, or the difference between the three real conformation types beyond broad size/stat targets. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the provided example. Before compiling, reconcile exact source enums, locus IDs, `E/e` and `A/a` allele symbols, strain syntax, rarity labels, price units, and stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The chestnut dominance is source-supported—one breed source says more than 80% are chestnut—but no representative peer-reviewed Budyonny `MC1R`/Extension genotype survey was located. The Extension rate below is therefore a transparent Hardy–Weinberg gameplay estimate: setting `e` around 0.90 yields \(e/e \approx 81\%\) chestnut. Agouti affects only the minority black-pigment founders and is correspondingly an approximate design value. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

```json
{
  "id": "budyonny",
  "name": "Budyonny",
  "type": "natural",
  "notes": "The Budyonny, also called Budenny or Budennovskaya Horse, is a Russian cavalry-to-sport riding breed developed in the Rostov region from Don mares and English Thoroughbred stallions. Its real identity includes chestnut-gold metallic sheen, Don-horse influence, Soviet cavalry remount history, courage, endurance, gallop quality, the Massive, Eastern, and Middle body types, sport training, conformation inspection, and individual rideability. Procedural Horse Genetics does not model metallic sheen, precise golden chestnut tone, cavalry training, bravery, long-gallop mechanics, race conditioning, sport training, gait quality, detailed conformation, or the true difference between the three historical types.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:forest",
    "minecraft:river"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1080,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.10,
      "e": 0.90
    },
    "agouti": {
      "A": 0.72,
      "a": 0.28
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
    "speed": 8,
    "jump": 7,
    "health": 8,
    "size": [
      1.08,
      1.20
    ]
  },

  "epigenetic_bands": {},

  "strains": [
    {
      "id": "middle",
      "name": "Middle",
      "weight": 0.55,
      "notes": "The balanced modern Budyonny type: the common athletic cavalry-and-sport model, with substantial strength, useful speed, and a tall but not heavy frame.",
      "stat_scores": {
        "speed": 8,
        "jump": 7,
        "health": 8,
        "size": [
          1.08,
          1.16
        ]
      }
    },
    {
      "id": "massive",
      "name": "Massive",
      "weight": 0.25,
      "notes": "The larger, more muscular, more substantial historical Budyonny type. It remains a riding horse but should feel more cavalry-strong and less refined than the Middle or Eastern type.",
      "stat_scores": {
        "speed": 7,
        "jump": 6,
        "health": 8,
        "size": [
          1.15,
          1.20
        ]
      }
    },
    {
      "id": "eastern",
      "name": "Eastern",
      "weight": 0.20,
      "notes": "The lighter, more refined historical Budyonny type, retaining stronger Don and Arabian-type influence in appearance. It should be elegant and forward without becoming a pure Arabian substitute.",
      "stat_scores": {
        "speed": 8,
        "jump": 7,
        "health": 8,
        "size": [
          1.05,
          1.12
        ]
      }
    }
  ],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Budyonny × Budyonny produces Budyonny regardless of Massive, Eastern, or Middle founder type. Budyonny × another pure breed produces a Budyonny cross. A Budyonny cross bred back to pure Budyonny remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Massive, Eastern, and Middle are historical conformation types within one breed, not separate breed-lineage labels."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | One Budyonny lineage with three body-type strains | Early Budyonny development recognized Massive, Eastern, and Middle types. They are conformation and performance tendencies within the breed, not independent breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |
| Chestnut core | `e: 0.90` | More than 80% chestnut suggests a high chestnut allele frequency. With random founder pairing, \(e = 0.90\) yields \(e/e = 81\%\), closely reproducing the stated population feature. This is a model-derived approximation, not a direct DNA survey.  [budennyhorse](https://www.budennyhorse.com/breed-history) |
| Black pigment minority | `E: 0.10` | The remaining black-pigment horses can become bay/brown under Agouti. This produces a genuinely chestnut-dominant founder population rather than a generic Russian bay warmblood. |
| Agouti | `A: 0.72 / a: 0.28` | Among the roughly 19% non-chestnut founders, Agouti favors bay/brown outcomes with a much smaller black possibility. This remains a gameplay estimate.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |
| Golden sheen | Not encoded as cream, champagne, silver, pearl, or magic | The prized chestnut-gold metallic appearance is a coat-quality and shade effect associated with Don/Turkoman-influenced breeding, not evidence for cream, champagne, silver, pearl, or an invented metallic gene.  [budennyhorse](https://www.budennyhorse.com/breed-history) |
| Gray and black policy | Gray excluded; black emergent and rare | A broad summary lists gray and black, but modern breed-oriented material describes the strict chestnut/bay/brown pool and says gray/black are not part of the intended population. This file favors the tighter modern founder definition.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |
| All dilutions | Cream, pearl, champagne, silver, mushroom, dun, and flaxen forced wild type | The breed’s golden chestnut is not a dilution. Excluding these loci prevents palomino, buckskin, champagne, silver dapple, dun, and flaxen chestnut from being mistaken for the traditional Budyonny sheen. |
| White patterns | Tobiano, sabino, frame, splash, KIT white, roan, rabicano forced wild type | The desired pure breed is solid-colored. No source supports an ordinary pinto, roan, or loud body-white Budyonny founder pool.  [budennyhorse](https://www.budennyhorse.com/breed-history) |
| Leopard complex | `LP`, `PATN1`, and `PATN2` forced wild type | Appaloosa-type spotting is explicitly outside the intended breed phenotype.  [budennyhorse](https://www.budennyhorse.com/breed-history) |
| Magical loci | All forced wild type | Budyonny gold is natural chestnut coat quality, not magical illumination or particle effects. |
| Disorders | All requested loci clear | No defensible Budyonny-specific carrier-frequency study was found for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. |
| Speed | `8/10` | The breed was created for cavalry gallop and later proved versatile in riding sport. It should be quicker than an ordinary work horse without reaching the narrow sprint specialization of a pure racehorse.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |
| Jump | `7/10` | Modern sport use includes show jumping and eventing; the score should be meaningfully athletic but below dedicated elite jumping studbooks.  [equio](https://equio.fr/en/horse-breeds/budyonny/) |
| Health | `8/10` | Represents courage, stamina, Don-horse hardiness, cavalry utility, and broad riding durability—not immunity from injury or inherited disease.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |
| Size | `×1.08–1.20` | Captures the roughly 16-hand average, tall cavalry frame, and variation among Massive/Eastern/Middle types.  [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse) |

## Disorder approach

Every requested disorder locus is **clear** in pure Budyonny founders.

The sources support the breed’s history, height, sport use, chestnut predominance, and distinct body types, but no Budyonny-specific carrier-frequency dataset was located for:

- `ACAN` dwarfism.
- `PLOD1` / WFFS.
- `MET` / frame-associated lethal white syndrome.
- `PRKDC` / SCID.
- `TOE1` / cerebellar abiotrophy.
- `MYO5A` / lavender foal syndrome.
- `GBE1` / GBED.
- CVM.
- Megaesophagus.
- `SCN4A` / HYPP.
- `GYS1` / PSSM1.
- `PPIB` / HERDA.

The breed’s Don, Thoroughbred, and light Arabian-type influences do **not** justify importing disorder rates from Don horses, Thoroughbreds, Arabians, Quarter Horses, or European warmbloods. A future direct Budyonny health-screening study may warrant a revision, but it must identify the actual mutation and whether its reported value is allele frequency, carrier prevalence, or affected-horse prevalence.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `budyonny` as a natural Russian riding breed with chestnut-dominant genetics, three internal body-type strains, clear disease policy, high athletic scores, and managed source options. |
| `common/breed/Breeds` | Register `budyonny` for stable and cowboy generation, spawn eggs, the H-menu, breed books, commands, saved genomes, founder rolls, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is omitted because Budyonny is a managed state-stud and sport-horse breed, not a natural feral population. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band map. Do not fabricate a “metallic chestnut” gene band unless the engine already has a coat-quality/shade trait suitable for the subtle real-world sheen. |
| `common/breed/spec/` | Reconcile exact `E/e` and `A/a` allele syntax, strain override format, source enums, default wild-type behavior, and direct numeric probability parsing against the live serializer and parser. |
| `common/breed/Commonness` | Confirm that `RARE` maps to the intended rarity ladder and that `spawn_weight: 1.5` is valid for direct numerical weighted selection. |
| `common/breed/BreedStatCurve` | Convert the overall and strain-specific speed, jump, health, and size settings into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Choose Middle, Massive, or Eastern first; roll the chestnut-heavy base-color pool; force all dilution, white-pattern, leopard, magical, and disease loci clear; then apply the strain body-stat targets. |
| `common/breed/BreedLineage` | Use the normal pure/cross/Mixed table. A chestnut sport horse must not gain Budyonny lineage merely from its color, size, or speed. |
| `common/genetics/SpliceOutcome` | No breed exception. A splice-carrot allele transmits normally and can create a nonstandard descendant with gray, cream, pinto, leopard, or disease genetics absent from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize tall cavalry-horse targets, including Massive size/strength bias and Eastern refinement bias. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the concrete schema requires an explicit default. Do not confuse the historical breed types with `BandType` values. |

## Verification

1. Confirm **Budyonny** appears in the H-menu’s Breeds tab and breed book with its Russian/Rostov origin, Semyon Budyonny history, cavalry-to-sport role, chestnut-gold identity, three body-type strains, and managed-only source checklist.

2. Confirm Budyonny does **not** spawn in ordinary wild packs because `wild` is absent. It should appear only through cowboy, stable, and spawn-egg generation.

3. Confirm that a lone wild horse reads **Feral Mixed**, even when it is tall, chestnut, golden-looking, athletic, or found in a plains biome.

4. Generate at least 1,000 pure founders. About 80% should be chestnut. The non-chestnut minority should mostly be bay/brown, with an exceptionally small black component. The sample must not produce gray, palomino, buckskin, cremello, dun, champagne, silver dapple, pearl, mushroom, flaxen chestnut, pinto, roan, leopard complex, brindle, or magical coats.

5. Inspect base-color loci:
- `e` should occur at approximately 0.90 across founder alleles.
- Chestnut `e/e` phenotype frequency should approach 81% in a large random sample.
- Black-pigment `E_` founders should be mostly `A_` bay/brown.
- `E_ a/a` black founders should remain rare.
- The renderer must not treat the desired golden chestnut sheen as cream or champagne.

6. Check strain behavior:
- About 55% should be **Middle**: tall, balanced, athletic.
- About 25% should be **Massive**: larger, stronger, slightly less fast/jump-specialized.
- About 20% should be **Eastern**: somewhat smaller, lighter, and forward.
- All three should retain the single **Budyonny** lineage label.
- Middle × Massive, Massive × Eastern, and Eastern × Middle offspring remain Budyonny.

7. Confirm all requested disorder loci remain clear over a large founder sample. No WFFS, PSSM1, HYPP, SCID, CA, LFS, GBED, HERDA, frame-related lethal white, or other named disorder should originate in pure Budyonny founders.

8. Confirm mature physical behavior. Budyonnys should be tall, fast, athletic, and durable, with good jumping. They should not outrun specialist Thoroughbreds, outjump Zangersheides, or become giant heavy horses.

9. Test default lineage:
- Budyonny × Budyonny → Budyonny.
- Budyonny × Don → Budyonny cross.
- Budyonny × Thoroughbred → Budyonny cross.
- Budyonny cross × pure Budyonny → the existing Budyonny cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice inheritance. Add gray, cream, tobiano, leopard complex, a larger draught body band, or a named disease allele. Offspring should inherit normally but must remain nonstandard descendants and never receive a pure Budyonny identity from chestnut color alone.

## Sources

- [Budyonny horse overview](https://en.wikipedia.org/wiki/Budyonny_horse): structured historical source for early-1920s Rostov development, Semyon Budyonny association, Don × Thoroughbred foundation, 1949 recognition, three historic types, average heights, conformation, cavalry role, and broad color summary. [en.wikipedia](https://en.wikipedia.org/wiki/Budyonny_horse)

- [Budenny Horse—Breed History](https://www.budennyhorse.com/breed-history): breed-conservancy source for endangered status, cavalry purpose, Don relationship, 16–16.3-hand range, bravery/endurance, chestnut frequency above 80%, prized gold sheen, and strict modern color interpretation. [budennyhorse](https://www.budennyhorse.com/breed-history)

- [Budyonny breed profile](https://equio.fr/en/horse-breeds/budyonny/): supplementary source for Russian sport-horse role, cavalry history, athletic performance, endurance, riding disciplines, and modern practical profile. [equio](https://equio.fr/en/horse-breeds/budyonny/)