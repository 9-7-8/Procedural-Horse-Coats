The **Oldenburg** should be implemented as a large, internationally oriented German warmblood studbook—selected primarily for elite dressage and jumping quality, not for one ancestral type or a restricted color. Its pure-founder pool should therefore be centered on bay, black/brown, chestnut, and gray, while allowing modest cream, tobiano, and roan only as occasional outcomes of the Verband’s deliberately open pedigree and color policy. The principal genetically documented health concern appropriate to model is **Warmblood Fragile Foal Syndrome** (WFFS), caused by the recessive `PLOD1` variant; because no Oldenburg-only prevalence estimate was found, a clearly labeled warmblood-wide proxy is safer than an invented Oldenburg statistic. [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger)

## Identity & flavour

The **Oldenburg**, formally the **Oldenburger** or Oldenburg horse, is a German warmblood associated with the former Grand Duchy of Oldenburg in northwestern Germany. The modern breed is administered by the **Oldenburger Pferdezuchtverband e.V.**, commonly called the Oldenburg Verband. Its roots reach into seventeenth- and eighteenth-century regional carriage and farm horses, but the present-day Oldenburg coalesced during the nineteenth and twentieth centuries as breeders shifted from heavy utility animals toward large, elastic, internationally competitive riding horses. The Verband’s modern breeding philosophy is often summarized as: “Quality is the only standard that counts.” [oldenburger-pferde](https://oldenburger-pferde.com/en/)

Oldenburgs began as substantial carriage, agricultural, artillery, and all-purpose horses suited to the flat, fertile country of Lower Saxony. Mechanization made that heavy utility type less useful, and the breeding program transformed through selected Thoroughbred, Anglo-Norman, Hanoverian, Trakehner, Holsteiner, Dutch Warmblood, and other performance-bred influence. The result is not a museum preservation breed. It is a performance studbook: horses are chosen for jumping scope, dressage movement, rideability, conformation, soundness, and modern sport aptitude, with pedigrees accepted from numerous approved sport-horse populations. [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger)

A modern Oldenburg is among the larger riding horses, ideally around **16–17.2 hands**, often strongly built but not drafty. It should have a noble, expressive head; long, well-shaped neck; pronounced withers; deep body; sloping shoulder; powerful loin and hindquarters; strong, clean limbs; and a roomy, uphill outline that supports collection, suspension, and scope. There should be no feathering, no pony-like mane texture, and no extreme Iberian, draft, or stock-horse silhouette. The mane and tail are ordinary full riding-horse hair rather than a genetic hallmark; what players should see is size, power, elegant proportion, and a confident sport-horse frame. [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger)

Black, dark brown, bay, chestnut, and gray remain the classic Oldenburg palette. However, unlike many European registries, the Oldenburg Verband does not make color a central restriction: its open approval philosophy means that an Oldenburg may also be palomino, buckskin, roan, tobiano, or another color if it otherwise meets the studbook’s performance and conformation standards. That does not mean every rare color should be common in a pure founder herd. In this file, classic solid colors dominate, gray is a visible minority, cream and tobiano are uncommon but possible, and leopard complex, champagne, silver, mushroom, and magical coats remain excluded unless introduced through crossing. [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger)

Oldenburgs should feel powerful, trainable, forward, and athletic: less sprint-focused than a Thoroughbred, larger and more expressive than an ordinary riding horse, and particularly capable at jumping and dressage. In Procedural Horse Genetics, players should breed toward a tall, high-performing sport horse with substantial scope, good speed, very good jumping, and robust—but not invulnerable—health. The mod does not model studbook licensing, mare inspection, performance tests, pedigree approval, movement quality, dressage collection, jumping technique, rideability, temperament under professional training, subjective “type,” or the difference between an elite Grand Prix horse and an ordinary but well-bred sport prospect. [oldenburger-pferde](https://oldenburger-pferde.com/en/)

## Breed JSON

> **Schema caveat:** The Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This JSON follows the readable convention in your supplied example. Before integration, reconcile every field, locus identifier, allele symbol, source enum, commonness name, band representation, and disease key with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The Oldenburg studbook is deliberately broad and performance-led. I found no representative peer-reviewed Oldenburg-specific genotype survey for extension, agouti, gray, cream, tobiano, roan, or the other requested coat loci. The coat rates below are restrained **gameplay approximations** based on the documented open-color policy and classic phenotype distribution; they are not claimed to be Verband population frequencies. For WFFS, `PLOD1` is real and relevant across warmblood populations, but there is no cited Oldenburg-only carrier rate here; the chosen 5.5% allele frequency is a conservative warmblood-wide proxy producing roughly 10.7% heterozygous carriers under Hardy–Weinberg equilibrium, near the reported ~11% warmblood average. [vgl.ucdavis](https://vgl.ucdavis.edu/research/wffs-breed-distribution)

```json
{
  "id": "oldenburg",
  "name": "Oldenburg",
  "type": "natural",
  "notes": "The Oldenburg is an open German sport-horse studbook defined by approved pedigree, licensing, mare inspection, conformation, movement, rideability, jumping ability, dressage aptitude, and modern performance selection rather than a closed ancestry or one fixed color. Procedural Horse Genetics does not model studbook approval, stallion licensing, mare inspections, performance testing, dressage collection, jump technique, elasticity, suspension, professional training, rider compatibility, subjective type, pedigree approval, or international sport results.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:flower_forest",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:river"
  ],
  "spawn_weight": 3,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1320,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.66,
      "a": 0.34
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "cream": {
      "N": 0.94,
      "Cr": 0.06
    },
    "dun": {
      "N": 1.0
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
    "flaxen": {
      "N": 0.95,
      "f": 0.05
    },

    "tobiano": {
      "N": 0.95,
      "TO": 0.05
    },
    "sabino_1": {
      "N": 0.98,
      "SB1": 0.02
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
      "N": 0.97,
      "Rn": 0.03
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
      "N": 0.945,
      "WFFS": 0.055
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
    "jump": 9,
    "health": 7,
    "size": [
      1.10,
      1.23
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Oldenburg × Oldenburg produces Oldenburg. Oldenburg × another pure breed produces an Oldenburg cross. An Oldenburg cross bred back to pure Oldenburg remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not automatically grant an Oldenburg label to any tall warmblood cross: real Oldenburg status is controlled by a performance-led studbook and approved pedigree rules, which the mod does not model."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Studbook philosophy | Broad sport-horse pool rather than fixed phenotype | Oldenburg is an open performance studbook whose modern selection emphasizes dressage/jumping quality over local origin or a preferred color.  [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger) |
| Base colors | `E: 0.72 / e: 0.28`; `A: 0.66 / a: 0.34` | Produces a herd centered on bay, brown/black, and chestnut—the traditional Oldenburg colors. These are gameplay estimates, not published Oldenburg genotype frequencies.  [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger) |
| Gray | `G: 0.18` | Gray is repeatedly identified among common Oldenburg colors and should occur regularly without dominating the adult population.  [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger) |
| Cream | `Cr: 0.06` | Palomino and buckskin occur within the color-tolerant studbook environment, but they are minority outcomes compared with bay, black/brown, chestnut, and gray.  [heelsdownmag](https://heelsdownmag.com/wiki/oldenburg/) |
| Tobiano | `TO: 0.05` | Pinto/tobiano horses are compatible with the Verband’s color-open philosophy, but they should remain uncommon in a general Oldenburg founder population. This is a gameplay approximation.  [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger) |
| Roan | `Rn: 0.03` | Roan is a rare but plausible outcome in an open warmblood registry; retain it at low frequency. This is not a measured Oldenburg prevalence.  [heelsdownmag](https://heelsdownmag.com/wiki/oldenburg/) |
| Sabino and splash | Very low `SB1`/`SW1` | These support ordinary white markings and occasional white-patterned outcomes without turning an Oldenburg herd into a color-bred pinto population. They are cautious game-balance estimates. |
| Excluded dilution loci | Dun, champagne, silver, pearl, mushroom forced wild type | Although an open registry could theoretically admit some of these through external approved pedigrees, I found no adequate evidence to seed them in the baseline Oldenburg pool. They remain obtainable through crosses. |
| Excluded patterns | Frame, W-series dominant white, leopard complex, PATN, brindle, magical loci forced wild type | The registry’s broad color policy is not evidence that every known coat locus is common in Oldenburg founders. Frame is also excluded to avoid introducing lethal-white risk without direct breed evidence. |
| WFFS | `PLOD1_WFFS: 0.055` | WFFS is caused by a recessive `PLOD1` mutation and is documented across most tested warmblood breeds. UC Davis reported an average carrier frequency around 11% across tested warmbloods; this file converts that carrier-level figure into an approximate allele frequency, \(q \approx 0.055\), for transparent founder rolling. This is a warmblood proxy, not an Oldenburg-specific survey.  [vgl.ucdavis](https://vgl.ucdavis.edu/research/wffs-breed-distribution) |
| Other disorders | Forced clear | No defensible Oldenburg-specific carrier rates were located for the other requested mutations. The breed’s open pedigree makes gene presence conceivable, but it does not justify inventing founder frequencies. |
| Speed | `7/10` | Oldenburgs are athletic modern riding horses, but their selection is more about power, collection, and sport performance than raw Thoroughbred-style sprinting.  [oldenburger-pferde](https://oldenburger-pferde.com/en/) |
| Jump | `9/10` | Jumping scope is central to Oldenburg breeding and international sport performance; this is one of the breed’s strongest axes.  [oldenburger-pferde](https://oldenburger-pferde.com/en/) |
| Health | `7/10` | Represents a selectively bred, sound sport horse with substantial strength, while recognizing that high performance, large size, and WFFS carrier status do not equal exceptional low-maintenance hardiness. |
| Size | `×1.10–1.23` | Captures the 16–17.2-hand target frame: clearly larger than a baseline riding horse but below giant draft scale.  [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger) |

## Disorder approach

The pure Oldenburg founder pool should carry exactly one named disorder allele: **WFFS**, the recessive `PLOD1` variant associated with Warmblood Fragile Foal Syndrome.

The implementation rate needs careful interpretation:

- UC Davis reported an approximately **11% average WFFS carrier frequency across tested warmblood breeds**, and the allele was found in 17 of 19 warmblood breeds examined. [vgl.ucdavis](https://vgl.ucdavis.edu/research/wffs-breed-distribution)
- A separate World Breeding Federation for Sport Horses summary notes that reported warmblood carrier figures vary, commonly around 6–11%, and emphasizes the need for larger datasets. [wbfsh](https://wbfsh.com/wffs)
- The file therefore uses a **5.5% allele frequency**, not an 11% allele frequency. With Hardy–Weinberg assumptions, `q = 0.055` gives approximately \(2q(1-q) = 10.4\%\) heterozygous carriers and \(q^2 = 0.30\%\) homozygous affected conceptions/foals.
- This is a **warmblood-wide proxy**, not evidence that precisely 10.4% of Oldenburgs carry WFFS. If an Oldenburg Verband-specific dataset becomes available, replace the proxy immediately.

The homozygous `WFFS/WFFS` state should follow the mod’s intended severe/usually lethal fragile-foal behavior. Two heterozygous parents should yield approximately 25% homozygous affected, 50% carriers, and 25% clear offspring over a large sample. [wbfsh](https://wbfsh.com/wffs)

All other requested disorders remain clear. Do not transfer PSSM1, HYPP, GBED, HERDA, Arabian disorders, Friesian dwarfism, or Quarter Horse-associated disease frequencies into this breed merely because the Oldenburg registry accepts internationally sourced performance pedigree.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the natural `oldenburg` record with breed notes, German sport-horse metadata, sources, rarity, price, coat pools, WFFS pool, and stat targets. |
| `common/breed/Breeds` | Register `oldenburg` so it is available in spawn systems, stable/cowboy systems, H-menu breed lists, breed books, commands, saved data, and lineage output. |
| `common/breed/BreedSource` | Validate the intended `cowboy`, `spawn_egg`, and `stable` source values. `wild` is omitted because Oldenburg is a managed international sport-horse studbook, not a natural feral herd population. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band object. Do not use coat-depth bands to imitate subjective “type,” dressage movement, or sport training. |
| `common/breed/spec/` | Map all sample fields to the real bidirectional format. Verify the exact `PLOD1` WFFS locus identifier, recessive allele notation, and whether disorder genes require direct allele pools or a separate disease mapping. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the requested commonness ladder and retains the intended spawn/purchase availability. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 9, health 7, and size ×1.10–1.23 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll only the listed coat and WFFS allele pools for founders, force unlisted coat loci wild type and unlisted disorder loci clear, then apply the large sport-horse stat bands. |
| `common/breed/BreedLineage` | Apply standard pure/cross/Mixed outcomes. The mod should not infer Oldenburg status merely from a tall black warmblood phenotype. |
| `common/genetics/SpliceOutcome` | No Oldenburg-specific exception. A successfully spliced allele should transmit normally and can create a nonstandard descendant outside baseline founder genetics. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the large, athletic, jump-specialist stat targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the actual schema mandates an explicit empty/default value. |

## Verification

1. Confirm **Oldenburg** appears in the H-menu’s Breeds tab and breed book with the correct German sport-horse identity, managed-only source checklist, large size range, jump-focused statistics, WFFS caution, and open-color explanation.

2. Confirm Oldenburg does **not** appear as a natural wild herd unless `wild` is deliberately added later. It should generate from stable, cowboy, and spawn-egg systems only.

3. Verify a lone unassigned wild horse reads **Feral Mixed**, never Oldenburg.

4. Generate at least 500 Oldenburg founders. The cohort should center on bay, brown/black, chestnut, and gray, with occasional palomino/buckskin, rare tobiano, rare roan, and minor ordinary white-marking variation. It must not produce pure-founder dun, champagne, silver dapple, pearl, mushroom, leopard complex, frame overo, W-series dominant white, brindle, or magical coats.

5. Inspect founder genomes. Confirm only Extension, Agouti, gray, cream, flaxen, tobiano, Sabino 1, Splash White 1, roan, rabicano, and `PLOD1_WFFS` vary. Every excluded cosmetic or disorder locus should be wild type or clear.

6. Test WFFS inheritance with selected `N/WFFS` parents:
- `N/WFFS × N/N` should produce roughly 50% carriers and 50% clear foals.
- `N/WFFS × N/WFFS` should produce approximately 25% affected `WFFS/WFFS`, 50% carrier, and 25% clear outcomes across a large sample.
- Confirm the affected outcome matches the mod’s intended fragile-foal/loss behavior and is not doubled by a separate disease mechanism.

7. Check baseline founder prevalence over a large sample. With the `0.055` allele pool, expect approximately 10–11% heterozygous carriers, about 0.3% homozygous affected offspring under random pairing, and the remainder clear. Random founder generation may show sampling noise in small tests.

8. Test default lineage:
- Oldenburg × Oldenburg → Oldenburg.
- Oldenburg × Hanoverian → Oldenburg cross.
- Oldenburg × Holsteiner → Oldenburg cross.
- Oldenburg cross × pure Oldenburg → the existing Oldenburg cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

9. Test mature stats over a sample. Oldenburgs should visibly and mechanically trend tall, powerful, and highly capable jumpers, with good speed and adequate health. They should not exceed specialized racehorses in raw speed, giant draft breeds in mass, or top purpose-bred endurance breeds in survival efficiency.

## Sources

- [Oldenburger Pferdezuchtverband e.V. — official English site](https://oldenburger-pferde.com/en/): official Oldenburg Verband source describing modern Oldenburg horses as partners for classical equestrian sport worldwide. [oldenburger-pferde](https://oldenburger-pferde.com/en/)

- [Oldenburg breed overview](https://en.wikipedia.org/wiki/Oldenburger): supplementary synthesis of the Verband’s color-open, performance-led selection philosophy; traditional colors; ideal 16–17.2-hand height; and historical shift from carriage type to modern sport horse. [en.wikipedia](https://en.wikipedia.org/wiki/Oldenburger)

- [Oldenburg historical overview](https://erenow.org/common/official-horse-breeds-standards-guide-complete-guide-standards/106.php): history of regional breeding organizations, 1923 merger, early registry history, and twentieth-century refinement with Thoroughbred influence. [erenow](https://erenow.org/common/official-horse-breeds-standards-guide-complete-guide-standards/106.php)

- [UC Davis Veterinary Genetics Laboratory — WFFS breed distribution](https://vgl.ucdavis.edu/research/wffs-breed-distribution): warmblood WFFS survey, 4.9% of all tested horses carrying the allele, detection in 17 of 19 warmblood breeds, approximate 11% warmblood average carrier frequency, and variation by breed. [vgl.ucdavis](https://vgl.ucdavis.edu/research/wffs-breed-distribution)

- [World Breeding Federation for Sport Horses — WFFS](https://wbfsh.com/wffs): WFFS inheritance, 25% affected risk from two carriers, and caution that available warmblood prevalence estimates vary. [wbfsh](https://wbfsh.com/wffs)

- [Performance of Swedish Warmblood fragile foal syndrome carriers](https://pmc.ncbi.nlm.nih.gov/articles/PMC8783495/): peer-reviewed warmblood carrier-frequency context and evidence that rates vary among warmblood populations. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC8783495/)