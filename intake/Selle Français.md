The **Selle Français** should be implemented as a tall French performance warmblood: a deliberately open, sport-selected studbook with a founder pool dominated by bay and chestnut, plus black and gray, ordinary white markings, and a modest allowance for rare colors introduced through approved performance ancestry. Its defining gameplay strengths are jumping scope, athletic movement, rideability, and durable sport-horse power—not a fixed coat, regional feral adaptation, or a special gait. The strongest scientifically defensible disorder entry is **WFFS**: a Selle Français study found **6% heterozygous carriers** of the recessive `PLOD1` allele. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S0737080622001976)

## Identity & flavour

The **Selle Français**, literally the **French Saddle Horse**, is France’s national sport-horse breed. It was formally created in **1958**, when several regional French half-bred riding-horse populations—including the Anglo-Norman, Charolais, Vendéen, and other local sport types—were merged into a single national studbook. The modern breed is administered by the **Association Nationale du Selle Français** (**ANSF**), which maintains the studbook, approves breeding stallions, and allows carefully selected outside bloodlines to improve international sport performance. [madbarn](https://madbarn.com/selle-francais-breed-profile/)

The breed grew out of France’s long transition from cavalry, carriage, agricultural, and trotting horses toward modern competitive riding horses. Anglo-Norman mares and stallions provided much of the historic foundation, while Thoroughbred, Anglo-Arabian, French Trotter, Arabian, and later international warmblood bloodlines supplied refinement, gallop, scope, movement, and sport specialization. The result is not a closed historical phenotype: the Selle Français is a performance population shaped by the question, “Can this horse excel under saddle?” rather than “Does this horse look like one old local type?” [madbarn](https://madbarn.com/selle-francais-breed-profile/)

Most Selle Français stand about **16.1–16.3 hands**, though the accepted practical range is broad—approximately **15.1–17.3 hands**. A typical horse is tall, athletic, balanced, and substantial without becoming drafty: broad forehead, generally straight facial profile, long muscular neck, deep chest, defined withers, sloping shoulder, straight back, strong loins, powerful broad hindquarters, and clean, muscular legs. It should look purpose-built to leave the ground: not merely tall, but uphill, elastic, scopey, and strong enough to organize its body for fences, cross-country terrain, and collected dressage work. [breeds.okstate](https://breeds.okstate.edu/horses/selle-francais-horses)

Bay and chestnut are the most typical colors, with black and gray also established in the breed. White facial and lower-leg markings are common, inherited in part through Norman ancestry, but a Selle Français is not chiefly a pinto breed. The studbook’s open performance approach means uncommon cream dilutions, roan, tobiano, sabino, frame-derived markings, and dominant-white phenotypes can occur through qualifying ancestry, but they should remain rare in a general founder population. For this file, players should see mostly bay and chestnut sport horses, some black and gray athletes, common ordinary white markings, and the occasional unexpected color—never a herd dominated by flashy spotting. [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile)

Selle Français horses are valued for intelligence, tractability, courage, carefulness, strength, and athletic ability. Their international cultural footprint is immense: French-bred Selle Français horses have been central to show jumping, eventing, dressage, and Olympic-level sport, with the studbook still selecting for movement, conformation, and jumping ability. In Procedural Horse Genetics, a player should breed toward a tall, powerful, high-jumping French sport horse whose bay or chestnut coat is merely the frame for its real identity: scope, balance, and performance. The mod does not model studbook approval, stallion testing, mare inspection, pedigree admissibility, gait quality, jumping technique, carefulness, courage, rideability, dressage collection, rider skill, training, or international competition results. [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This uses the readable JSON convention from the provided Anglo-Arabian example. Before compiling, reconcile exact field names, source values, locus IDs, allele labels, `Commonness` values, price units, and stat-band serialization with `common/breed/spec/`.
>
> **Scientific-frequency caveat:** The Selle Français is an open performance studbook, and no representative modern breed-wide genotype frequency dataset was located for the requested coat loci. The coat values below are restrained **gameplay approximations**, not official ANSF allele frequencies. The WFFS number is stronger: a study of Selle Français horses reported **6% heterozygous `PLOD1` carriers**. To generate that carrier frequency under Hardy–Weinberg assumptions, the founder allele pool uses \(q \approx 0.031\), which yields \(2q(1-q) \approx 6.0\%\) carriers and \(q^2 \approx 0.10\%\) homozygous affected conceptions. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S0737080622001976)

```json
{
  "id": "selle_francais",
  "name": "Selle Français",
  "type": "natural",
  "notes": "The Selle Français is a French performance warmblood defined by ANSF studbook eligibility, approved pedigree, athletic conformation, jumping scope, movement, rideability, training, and competition performance rather than a fixed old local phenotype or narrow coat palette. Procedural Horse Genetics does not model pedigree approval, stallion licensing, mare inspection, performance testing, jump technique, carefulness, courage, dressage collection, elasticity, gait quality, rider compatibility, training, competition records, or the distinction between an elite international horse and an ordinary sport prospect.",

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
  "price": 1380,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.66,
      "e": 0.34
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "grey": {
      "N": 0.88,
      "G": 0.12
    },
    "cream": {
      "N": 0.96,
      "Cr": 0.04
    },
    "dun": {
      "N": 0.98,
      "D": 0.02
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
      "N": 0.97,
      "f": 0.03
    },

    "tobiano": {
      "N": 0.98,
      "TO": 0.02
    },
    "sabino_1": {
      "N": 0.94,
      "SB1": 0.06
    },
    "frame_overo": {
      "N": 0.995,
      "O": 0.005
    },
    "splash_white_1": {
      "N": 0.99,
      "SW1": 0.01
    },
    "splash_white_2": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 0.995,
      "W": 0.005
    },
    "roan": {
      "N": 0.985,
      "Rn": 0.015
    },
    "rabicano": {
      "N": 0.97,
      "Rb": 0.03
    },
    "leopard_complex": {
      "N": 0.995,
      "LP": 0.005
    },
    "patn1": {
      "N": 0.995,
      "PATN1": 0.005
    },
    "patn2": {
      "N": 0.995,
      "PATN2": 0.005
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
      "N": 0.969,
      "WFFS": 0.031
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
      1.08,
      1.23
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Selle Français × Selle Français produces Selle Français. Selle Français × another pure breed produces a Selle Français cross. A Selle Français cross bred back to pure Selle Français remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not infer Selle Français lineage solely from a tall bay/chestnut sport-horse phenotype: real status requires ANSF-recognized pedigree and breeding-program eligibility."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | Open performance-warmblood pool | The Selle Français was formed from several French regional half-bred populations and later admitted selected outside bloodlines. It is performance-led rather than a genetically closed color breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Selle_Fran%C3%A7ais) |
| Base colors | `E: 0.66 / e: 0.34`; `A: 0.68 / a: 0.32` | Produces a founder population dominated by bay and chestnut, with some black/brown. Bay and chestnut are explicitly the most common colors in breed descriptions. These numeric allele values are gameplay approximations.  [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile) |
| Gray | `G: 0.12` | Gray is established but noticeably less common than bay and chestnut, partly reflecting Thoroughbred and Anglo-Arabian influences.  [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile) |
| Cream | `Cr: 0.04` | Cream dilutions occur within the open studbook context, but should remain uncommon relative to classic base colors. This is a gameplay approximation rather than an ANSF allele survey.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Selle_Francais.php) |
| Dun | `D: 0.02` | Dun is a rare but plausible imported-performance-line outcome in an open modern studbook; it should not be common.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Selle_Francais.php) |
| Flaxen | `f: 0.03` | A low-level modifier allows occasional flaxen chestnut, but should not compete visually with standard chestnut/sorrel. This is a conservative implementation choice. |
| Common ordinary markings | `SB1: 0.06`, `Rb: 0.03` | White legs and facial markings are fairly common in the breed. Low Sabino 1 and rabicano allow modest inherited white/hair effects without making pure founders mainly pinto. Exact locus frequencies are not documented.  [en.wikipedia](https://en.wikipedia.org/wiki/Selle_Fran%C3%A7ais) |
| Rare pinto/white loci | Low `TO`, `O`, `SW1`, and `W` | The modern open studbook can include rare tobiano, frame, splash, and dominant-white phenotypes through approved ancestry. The deliberately tiny values keep them extraordinary rather than typical.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Selle_Francais.php) |
| Frame / lethal white | `O: 0.005`, with engine-linked medical effect | Frame is rare and should only be retained if the mod couples `EDNRB` homozygosity to lethal white syndrome. Do not duplicate frame as an independent `MET` disease roll. |
| Leopard complex | `LP`, `PATN1`, `PATN2` at 0.005 | Rare leopard-patterned Selle Français outcomes are described in supplemental breed material. The extremely low implementation rate prevents Appaloosa-like spotting from becoming normal in a French warmblood herd.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Selle_Francais.php) |
| Excluded loci | Champagne, silver, pearl, mushroom, brindle, magical loci forced wild type | No strong source located justified putting these into an average Selle Français founder pool. Open studbook logic is not evidence that all possible color genes belong at baseline. |
| WFFS | `PLOD1_WFFS: 0.031` | A published Selle Français sample reported 6% heterozygous carriers. An allele frequency of 3.1% generates about 6.0% heterozygotes under Hardy–Weinberg equilibrium. This is directly anchored to Selle Français data.  [sciencedirect](https://www.sciencedirect.com/science/article/pii/S0737080622001976) |
| Other disorders | Forced clear | No defensible Selle Français-specific carrier rates were located for the remaining requested diseases. Do not import Quarter Horse, Arabian, Friesian, or unrelated warmblood rates. |
| Speed | `7/10` | Represents athletic gallop and sport-horse movement, but not the raw specialized sprint of a Thoroughbred. |
| Jump | `9/10` | Show jumping and eventing scope are central to breed selection and international reputation.  [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile) |
| Health | `7/10` | Represents strength and soundness appropriate to a performance horse while retaining meaningful WFFS management and avoiding an unrealistic “hardiness” claim. |
| Size | `×1.08–1.23` | Produces a tall sport-horse population spanning the common 16.1–16.3-hand center and broad documented 15.1–17.3-hand range.  [en.wikipedia](https://en.wikipedia.org/wiki/Selle_Fran%C3%A7ais) |

## Disorder approach

The pure Selle Français pool should carry exactly one independently seeded disorder allele: **Warmblood Fragile Foal Syndrome** (**WFFS**).

WFFS is a recessive connective-tissue disorder associated with a `PLOD1` variant. Affected foals inherit two mutant copies and are generally severely compromised or nonviable; heterozygotes are clinically normal carriers but can transmit the allele. [madbarn](https://madbarn.com/selle-francais-breed-profile/)

The direct Selle Français evidence is unusually useful:

- A 2022 genetic-marker study reported that **6% of tested Selle Français horses were heterozygous WFFS carriers**. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S0737080622001976)
- For a random-mating founder generator, carrier prevalence is not the same as allele frequency. Let \(q\) be the mutant allele frequency. Under Hardy–Weinberg assumptions, heterozygote prevalence is \(2q(1-q)\).
- Setting `WFFS: 0.031` produces:
  - \(2(0.031)(0.969) \approx 6.0\%\) heterozygous carriers.
  - \((0.031)^2 \approx 0.10\%\) homozygous affected outcomes in random pairings.
  - About 93.9% clear `N/N` founders.

Do **not** add a separate generic PLOD1/“Friesian dwarfism” entry. PLOD1 is the WFFS locus in this context; the record’s name must reflect the actual disorder and keep the founder pool from rolling two incompatible conditions at one site.

All other requested disorder loci are clear because no defensible Selle Français-specific rate was found. In particular, the breed’s open international sport-horse ancestry does not justify copying HYPP, HERDA, GBED, SCID, CA, LFS, PSSM1, or Friesian-specific disease rates into pure founders.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `selle_francais` natural-breed record with French sport-horse metadata, source availability, rarity, price, gene pools, WFFS data, and target stats. |
| `common/breed/Breeds` | Register `selle_francais` for stable/cowboy acquisition, books, H-menu display, commands, save loading, founder generation, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because Selle Français is a managed sport-horse studbook, not a natural feral population. |
| `common/breed/BreedBands` | Allow the empty epigenetic-band object. Do not use coat-depth bands to fake athletic movement, jumping style, or subjective type. |
| `common/breed/spec/` | Match every example field to the real read/write schema, especially `PLOD1` WFFS representation, rare-pattern loci, direct frequency parsing, and omitted-locus wild-type behavior. |
| `common/breed/Commonness` | Confirm `UNCOMMON` maps to the intended rarity ladder and retain numeric `spawn_weight: 3` only if that is the project’s direct weighted-spawn field. |
| `common/breed/BreedStatCurve` | Translate speed 7, jump 9, health 7, and size ×1.08–1.23 into valid `TargetBand` targets. |
| `common/breed/BreedFounder` | Roll founders from the broad sport-horse coat pool; force unsupported loci clear; use `PLOD1` allele frequency 0.031; and apply tall jump-specialist stat bands. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed logic. A tall chestnut jumper must not become Selle Français simply from phenotype or performance stats. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. Spliced alleles follow normal inheritance, allowing intentional creation of nonstandard descendants outside the baseline French Saddle Horse pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define the tall, athletic, high-jump body-stat targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is required unless the actual serialization format mandates a default. |

## Verification

1. Confirm **Selle Français** appears in the H-menu’s Breeds tab and breed book with correct French identity, tall sport-horse size, high jumping score, WFFS note, and managed-only source checklist.

2. Confirm Selle Français does **not** occur in natural wild packs when `wild` is omitted. It should be available through the stable, cowboy, and spawn-egg systems only.

3. Confirm an ordinary lone wild horse reads **Feral Mixed**, never Selle Français, even if it is tall, chestnut, bay, gray, or mechanically excellent at jumping.

4. Generate at least 1,000 pure founders. The sample should be dominated by bay and chestnut, with lesser black/brown and gray. It may produce rare cream dilution, dun, tobiano, sabino, frame-derived, splash-derived, dominant-white, roan, rabicano, and leopard outcomes only at the very low supplied rates.

5. Confirm that pure founders do not generate champagne, silver dapple, pearl, mushroom, brindle, magical coats, or other omitted loci.

6. Inspect genome panels to confirm:
- `E/e` and `A/a` support bay/chestnut-dominant base colors.
- `G` occurs as a visible minority.
- `Cr`, `D`, `TO`, `SB1`, `O`, `SW1`, `W`, `Rn`, `Rb`, `LP`, `PATN1`, and `PATN2` occur only at their low listed frequencies.
- All explicitly excluded loci are wild type.

7. Test WFFS:
- Generate a large random founder sample. Carrier frequency should approach 6%, allowing sampling variation.
- `N/WFFS × N/N` should produce about 50% carriers and 50% clear foals.
- `N/WFFS × N/WFFS` should produce approximately 25% `WFFS/WFFS`, 50% carriers, and 25% clear offspring or conceptions.
- Confirm the engine handles homozygous WFFS according to its severe/usually lethal foal model and does not double-count the locus under another PLOD1 disease name.

8. Test rare frame logic only if the mod links frame overo to lethal white:
- `O/N × O/N` should yield approximately 25% `O/O`.
- The engine should apply its usual lethal-white outcome once, through the frame/`EDNRB` system.
- Do not add a second independent `MET` disease roll.

9. Test default lineage:
- Selle Français × Selle Français → Selle Français.
- Selle Français × Anglo-Arabian → Selle Français cross.
- Selle Français × Oldenburg → Selle Français cross.
- Selle Français cross × pure Selle Français → the existing Selle Français cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test mature body stats. Pure Selle Français horses should trend tall, fast enough for sport, and very strong jumpers. They should not become raw-sprint specialists, giant draft horses, exceptionally low-maintenance primitive ponies, or automatically superior to all other warmbloods in every discipline.

## Sources

- [Association Nationale du Selle Français / official studbook context](https://www.sellefrancais.fr/): the current French national organization responsible for the Selle Français studbook, breeding selection, stallion approvals, and sport-horse program. The search index identifies ANSF as the official breed organization. [madbarn](https://madbarn.com/selle-francais-breed-profile/)

- [FEI — Celebrating the Elegant Selle Français](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile): international sport-horse profile supporting French origin, sport role, athletic temperament, and the predominance of bay and chestnut with black and gray also occurring. [fei](https://www.fei.org/stories/lifestyle/horse-human/selle-francais-horse-profile)

- [Profiling of genetic markers useful for breeding decision in Selle Français](https://www.sciencedirect.com/science/article/pii/S0737080622001976): primary study reporting that 6% of the examined Selle Français horses were WFFS carriers. [sciencedirect](https://www.sciencedirect.com/science/article/pii/S0737080622001976)

- [Full PDF — Profiling of genetic markers useful for breeding decision in Selle Français](https://www.repo.ur.krakow.pl/docstore/download/UR4b469f50e6384c0db1c9ad2a61ec2f82/Profiling+of+genetic+markers+useful+for+breeding+decision+in+Selle.pdf?entityType=article): full-text access to the 2022 Selle Français genetic-marker results. [repo.ur.krakow](https://www.repo.ur.krakow.pl/docstore/download/UR4b469f50e6384c0db1c9ad2a61ec2f82/Profiling+of+genetic+markers+useful+for+breeding+decision+in+Selle.pdf?entityType=article)

- [Distribution of the WFFS `PLOD1` mutation in horse breeds](https://pmc.ncbi.nlm.nih.gov/articles/PMC7766603/): peer-reviewed WFFS distribution, inheritance, and warmblood frequency context. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7766603/)

- [Oklahoma State University — Selle Français Horses](https://breeds.okstate.edu/horses/selle-francais-horses): breed description of athletic build, strength, bone, intelligence, and tractable disposition. [breeds.okstate](https://breeds.okstate.edu/horses/selle-francais-horses)

- [MadBarn — Selle Français Breed Guide](https://madbarn.com/selle-francais-breed-profile/): supplementary overview of ANSF governance, height, conformation, performance disciplines, common colors, and the 6% WFFS-carrier finding. [madbarn](https://madbarn.com/selle-francais-breed-profile/)