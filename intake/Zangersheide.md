The **Zangersheide** should be represented as a Belgian **open show-jumping studbook**, not as a genetically closed traditional breed. Its pure-founder population should be tall, highly athletic, and dominated by the normal European sport-horse colors—bay, chestnut, black/brown, and gray—while rare dilution and pinto genes remain possible because Zangersheide registers selected performance horses from multiple approved sport-horse backgrounds. The only scientifically defensible named disease locus to seed is WFFS at `PLOD1`, but no Zangersheide-specific carrier frequency was located, so a conservative warmblood proxy must be explicitly labeled as such. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

## Identity & flavour

The **Zangersheide**, often shortened to **Z**, is a Belgian warmblood show-jumping studbook based at Lanaken in Limburg, near the Dutch border. It began with the Zangersheide Stud, founded by **Léon Melchior** in the 1970s, and became the independent **Studbook Zangersheide vzw** in 1992. A registered Zangersheide horse traditionally carries the suffix **“Z”** after its name. It is better understood as a global performance registry than as a closed historical breed: a Z horse is selected for its ability to jump, not for descent from one isolated local population. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

Zangersheide was created for one purpose above all others: breeding **international show jumpers**. The studbook evaluates and selects horses for power, scope, technique, carefulness, rideability, gallop, soundness, and proven or predicted performance over fences. Its open policy allows sport-horse blood from such populations as Holsteiner, Hanoverian, Selle Français, Dutch Warmblood, Belgian Warmblood, and Thoroughbred lines. That produces a remarkably modern breed identity: not “Belgian-looking” horses in one color, but athletes built to leave the ground, think quickly, and remain rideable enough to make their ability usable. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

Most Zangersheide horses stand around **16–17.2 hands**, though the studbook has no strict universal height requirement and individuals can fall outside that range. They should be tall, long-legged, and uphill, with a noble head, long neck, defined withers, sloping shoulder, deep chest, powerful loin, broad muscled hindquarters, and clean limbs with enough bone for high-impact sport. A good Z should look more rangy and jumping-oriented than a stock horse, more modern and reactive than a heavy carriage warmblood, and more powerful through the hind end than a pure flat-racing Thoroughbred. There should be no feathering and no breed-specific mane or tail texture. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

Color is not a Zangersheide selection criterion. Bay, brown, black, chestnut, and gray are the normal foundation palette, but Z horses also occur in pinto and other colors because the studbook accepts outside sport-horse ancestry on performance grounds. This means the mod should allow a small but real tobiano/sabino/splash contribution, rather than forcing every founder into a solid bay-or-chestnut pattern. However, rare color does not define a Zangersheide: a pinto Z should still look like a serious tall jumper, and the average population should remain dominated by conventional dark sport-horse coats. Leopard complex, champagne, pearl, mushroom, and magical colors should stay absent unless introduced through a cross or deliberate genetic intervention. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

In Procedural Horse Genetics, players should breed Zangersheides toward one clear goal: **scope**. A Z should be tall, quick, strong, and among the mod’s best jumpers, but not necessarily the fastest flat racer, toughest primitive survivor, or most specialized dressage horse. Its most recognizable in-game feature is the contrast between a broad international color palette and extremely consistent performance intent: whether bay, gray, black, chestnut, or an uncommon pinto, it should look and feel like a high-end jumping prospect. The mod does not model studbook approval, “Z” name suffixes, free-jumping tests, competition records, international rankings, breeding-value estimation, jumping technique, carefulness, rideability, professional training, or selection of foals from approved parents. [zangersheide](https://www.zangersheide.com/en/studbook)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in the user-provided example. Before compiling, reconcile every key, source enum, allele symbol, disease-locus ID, commonness value, pricing convention, and stat-target representation against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** Zangersheide’s open studbook policy supports a broad sport-horse phenotype, but I found no representative Zangersheide-wide genetic frequency study for Extension, Agouti, gray, cream, pinto loci, or other coat genes. The coat values below are transparent **gameplay approximations**, not official Studbook Zangersheide frequencies. WFFS is well documented in warmblood populations, but the `PLOD1` rate below is a cautious warmblood proxy—not a Zangersheide-specific estimate. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

```json
{
  "id": "zangersheide",
  "name": "Zangersheide",
  "type": "natural",
  "notes": "Zangersheide is a Belgian open show-jumping studbook, defined by Studbook Zangersheide registration, approved ancestry, jumping scope, carefulness, technique, rideability, gallop, soundness, and sport performance rather than a closed genetic population or fixed coat color. Procedural Horse Genetics does not model name suffixes, studbook approval, foal registration, free-jumping evaluation, stallion licensing, mare inspection, breeding values, jumping technique, carefulness, rider compatibility, professional training, competition results, rankings, or the difference between a promising young jumper and an elite international horse.",

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
  "price": 1550,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.65,
      "a": 0.35
    },

    "grey": {
      "N": 0.84,
      "G": 0.16
    },
    "cream": {
      "N": 0.96,
      "Cr": 0.04
    },
    "dun": {
      "N": 0.99,
      "D": 0.01
    },
    "silver": {
      "N": 0.99,
      "Z": 0.01
    },
    "flaxen": {
      "N": 0.97,
      "f": 0.03
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

    "tobiano": {
      "N": 0.96,
      "TO": 0.04
    },
    "sabino_1": {
      "N": 0.96,
      "SB1": 0.04
    },
    "frame_overo": {
      "N": 0.995,
      "O": 0.005
    },
    "splash_white_1": {
      "N": 0.985,
      "SW1": 0.015
    },
    "splash_white_2": {
      "N": 1.0
    },
    "kit_white_spotting": {
      "N": 0.995,
      "W": 0.005
    },
    "roan": {
      "N": 0.99,
      "Rn": 0.01
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
      "N": 0.95,
      "WFFS": 0.05
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
    "speed": 8,
    "jump": 10,
    "health": 7,
    "size": [
      1.10,
      1.24
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Zangersheide × Zangersheide produces Zangersheide. Zangersheide × another pure breed produces a Zangersheide cross. A Zangersheide cross bred back to pure Zangersheide remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign Zangersheide lineage solely because a horse is tall, athletic, pinto, or highly ranked in jumping stats: real Z status requires registration in the open Belgian sport-horse studbook."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Breed architecture | Open Belgian jumping studbook | Zangersheide is a performance-selected studbook founded in 1992, open to qualifying sport-horse breeds and pedigrees. It is not a closed landrace or color breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Base colors | `E: 0.70 / e: 0.30`; `A: 0.65 / a: 0.35` | Produces a founder population centered on bay, brown/black, and chestnut, matching the normal European warmblood palette. Exact values are gameplay approximations.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Gray | `G: 0.16` | Gray is regularly represented in Zangersheide horses alongside the common dark base colors. A meaningful minority rate preserves it without making every mature jumper gray.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Cream | `Cr: 0.04` | An open performance studbook can admit rare cream-dilute horses, but cream should remain a minority feature relative to conventional jumping-horse colors. This is a gameplay approximation. |
| Dun and silver | `D: 0.01`, `Z: 0.01` | Extremely rare optional alleles reflect the broad background of an open sport-horse registry. Remove either if the mod’s phenotype or health linkage is not robust; silver in particular should use `PMEL`/MCOA logic if it exists. |
| Tobiano | `TO: 0.04` | Pinto Zangersheides exist through open sport-horse ancestry. A low rate permits recognizable pinto jumpers without turning the studbook into a paint-color population.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Sabino and splash | `SB1: 0.04`, `SW1: 0.015` | Low rates allow occasional white-marked/pinto outcomes through diverse warmblood ancestry, while classic solid coats remain dominant. These are gameplay approximations. |
| Frame overo | `O: 0.005` | Included only as a very rare open-studbook possibility. If the engine models `EDNRB` lethal white, it must derive that risk from `O/O`; do not duplicate it under `MET`. Remove `O` entirely if the real project reserves frame for paint-derived populations. |
| Roan and rabicano | `Rn: 0.01`, `Rb: 0.02` | Very low optional modifiers retain visual diversity consistent with a global open sport-horse studbook. These are not published Z allele frequencies. |
| Excluded loci | Champagne, pearl, mushroom, leopard complex, PATN, brindle, magical genes forced wild type | No source found supports seeding these as routine Zangersheide founder traits. An open registry does not imply every possible horse color belongs in its baseline population. |
| WFFS | `PLOD1_WFFS: 0.05` | WFFS is established across many warmblood breeds. A 5% mutant-allele proxy yields about \(2(0.05)(0.95)=9.5\%\) heterozygous carriers and 0.25% homozygous affected conceptions under Hardy–Weinberg assumptions. This is intentionally cautious and not Zangersheide-specific.  [madbarn](https://madbarn.com/research/distribution-of-the-warmblood-fragile-foal-syndrome-type-1-mutation-plod1-c-2032ga-in-different-horse-breeds-from-europe-and-the-united-states/) |
| Other disorders | Forced clear | No defensible Zangersheide-specific frequency was found for the remaining requested loci. Its open pedigree does not justify importing Quarter Horse, Arabian, Friesian, or other breed-specific disease rates. |
| Speed | `8/10` | Zangersheides require a powerful, adjustable gallop and athletic responsiveness for show jumping, though they remain below flat-racing specialists in pure speed.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Jump | `10/10` | The entire studbook exists to breed high-quality show jumpers. It is appropriate as one of the mod’s maximum jumping-score breeds.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |
| Health | `7/10` | Represents selected soundness and high athletic capacity, tempered by intense sport demands and the use of a cautious WFFS allele proxy. |
| Size | `×1.10–1.24` | Represents the common 16–17.2-hand tall sport-horse frame while allowing individual variation, as the studbook does not enforce a universal height standard.  [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide) |

## Disorder approach

The proposed pure Zangersheide pool seeds one disorder locus: **Warmblood Fragile Foal Syndrome** (**WFFS**) at `PLOD1`.

This is an evidence-limited warmblood implementation, not a claim of a published Zangersheide-specific prevalence:

- WFFS is caused by a recessive `PLOD1` variant and is documented across European and North American warmblood populations. [madbarn](https://madbarn.com/research/distribution-of-the-warmblood-fragile-foal-syndrome-type-1-mutation-plod1-c-2032ga-in-different-horse-breeds-from-europe-and-the-united-states/)
- A 2020 multi-breed dataset reported WFFS carriers across 21 breeds, with the allele especially widespread among warmbloods. [madbarn](https://madbarn.com/research/distribution-of-the-warmblood-fragile-foal-syndrome-type-1-mutation-plod1-c-2032ga-in-different-horse-breeds-from-europe-and-the-united-states/)
- No retrieved source gave a defensible Zangersheide-only carrier frequency.
- The file uses a **5% allele-frequency proxy**. Under random mating, that yields approximately:
  - 9.5% `N/WFFS` heterozygous carriers,
  - 0.25% `WFFS/WFFS` affected conceptions,
  - 90.25% clear `N/N` founders.

If your implementation standard requires only **breed-specific numerical data**, replace the `PLOD1_WFFS` entry with `N: 1.0` until a Zangersheide-specific tested-population dataset is available.

All other named disorders are forced clear. Do not transfer PSSM1, HYPP, GBED, HERDA, SCID, CA, LFS, or Friesian-specific dwarfism rates into this record simply because Zangersheide accepts broad sport-horse ancestry.

### Frame-overo handling

The low `O` frequency belongs under the **coat** locus, not a duplicate independent disease locus:

- `N/O` can yield a viable frame-overo patterned horse.
- `O/O` is the lethal-white genotype if the mod models the `EDNRB` effect.
- Do not add a second `MET` random roll for the same genetic event.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `zangersheide` as a natural Belgian open-studbook record with jumping-first notes, coat pools, WFFS policy, stats, price, and controlled sources. |
| `common/breed/Breeds` | Register the ID for stable/cowboy generation, commands, breed books, H-menu display, genome serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally excluded: Zangersheide is a managed sport-horse studbook, not a feral population. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band map. Do not simulate jumping technique, carefulness, or professional training through arbitrary color-expression bands. |
| `common/breed/spec/` | Reconcile all illustrative keys with the actual serialization format, especially `PLOD1` WFFS allele naming, `EDNRB`/frame behavior, `PMEL`/silver behavior, omitted-gene defaults, and any array/object convention. |
| `common/breed/Commonness` | Confirm `UNCOMMON` matches the requested commonness ladder and keep direct `spawn_weight: 3` only if the codebase uses direct numerical weights. |
| `common/breed/BreedStatCurve` | Convert speed 8, jump 10, health 7, and size ×1.10–1.24 to valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll founders only from the declared pools, enforce tall jumping-horse stat bands, force unsupported loci clear, and avoid wild-pack generation. |
| `common/breed/BreedLineage` | Use default pure/cross/Mixed rules. A high-jump horse does not gain a Zangersheide label from its stats, conformation, or a “Z” suffix alone. |
| `common/genetics/SpliceOutcome` | No breed exception. Splice-carrot alleles follow normal transmission and may create nonstandard descendants outside the default Z founder pool. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize tall-frame, fast, high-jump, and moderately robust sport-horse targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the final format requires an explicit default. |

## Verification

1. Confirm **Zangersheide** appears in the H-menu’s Breeds tab and breed book with Belgian Lanaken origins, Léon Melchior/1992 studbook history, high-jump identity, tall frame, open-studbook context, and managed-only sources.

2. Confirm Zangersheide does **not** spawn as an ordinary wild herd because `wild` is omitted. It should appear through stable, cowboy, and spawn-egg systems only.

3. Confirm any ordinary lone wild horse reads **Feral Mixed**, even if it is tall, bay, pinto, gray, or mechanically excellent at jumping.

4. Generate at least 1,000 pure founders. The population should be mainly bay, brown/black, chestnut, and gray; rare cream, dun, silver, tobiano, sabino, splash, roan, rabicano, and very rare frame outcomes may occur. It must not generate champagne, pearl, mushroom, leopard spotting, PATN patterns, brindle, or magical coats.

5. Inspect founder genomes. Confirm:
- `E/e`, `A/a`, and `G` produce the ordinary European sport-horse palette.
- `TO`, `SB1`, `SW1`, `O`, and `W` remain rare.
- `LP`, `PATN1`, `PATN2`, `Ch`, `Prl`, `Mushroom`, and magical loci are always wild type.
- The system does not infer performance or breed identity from coat genes.

6. Test WFFS if retaining the proxy:
- Large random founder samples should approach 9–10% heterozygous carriers.
- `N/WFFS × N/N` should yield roughly 50% carrier foals.
- `N/WFFS × N/WFFS` should yield approximately 25% `WFFS/WFFS`, 50% carrier, and 25% clear conceptions/foals.
- Confirm the affected homozygous state follows the mod’s severe fragile-foal/conception-loss behavior once only.

7. Test frame only if the engine implements `EDNRB` correctly:
- `O/N × O/N` yields about 25% `O/O` outcomes in a large sample.
- The lethal-white consequence comes from the coat-locus genotype, not a duplicated disorder roll.

8. Test default lineage:
- Zangersheide × Zangersheide → Zangersheide.
- Zangersheide × Holsteiner → Zangersheide cross.
- Zangersheide × Selle Français → Zangersheide cross.
- Zangersheide cross × pure Zangersheide → the existing Zangersheide cross label.
- Two different cross labels → Mixed.
- Any pairing with Feral Mixed → Mixed.

9. Check mature stats. Zangersheides should consistently rank among the highest jump-potential horses, with a tall, fast, powerful sport frame. They should not surpass specialist racehorses in flat speed, giant drafts in mass, or native survival breeds in low-input heartiness.

## Sources

- [Studbook Zangersheide — official studbook page](https://www.zangersheide.com/en/studbook): official confirmation that Studbook Zangersheide was founded by Léon Melchior in 1992 to breed showjumpers of the highest quality. [zangersheide](https://www.zangersheide.com/en/studbook)

- [Studbook Zangersheide — official site](https://www.zangersheide.com/en): Lanaken location, international scale, stallion-station context, and current annual registration scale. [zangersheide](https://www.zangersheide.com/en)

- [Zangersheide overview](https://en.wikipedia.org/wiki/Zangersheide): supplementary synthesis of 1970s stud origins, 1992/93 studbook formation, “Z” suffix, open registry, approved performance-line backgrounds, tall warmblood type, and show-jumping focus. [en.wikipedia](https://en.wikipedia.org/wiki/Zangersheide)

- [Zangersheide breed reference](https://hi3.horseisle.com/www/bbb/Zangersheide.php): supplementary confirmation of show-jumping specialization, open-studbook practice, conventional coat palette, and 16–17.2-hand range. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Zangersheide.php)

- [Distribution of the WFFS `PLOD1` mutation in European and U.S. horse breeds](https://madbarn.com/research/distribution-of-the-warmblood-fragile-foal-syndrome-type-1-mutation-plod1-c-2032ga-in-different-horse-breeds-from-europe-and-the-united-states/): multi-breed WFFS context used only for the stated cautious warmblood proxy, not as a direct Zangersheide frequency. [madbarn](https://madbarn.com/research/distribution-of-the-warmblood-fragile-foal-syndrome-type-1-mutation-plod1-c-2032ga-in-different-horse-breeds-from-europe-and-the-united-states/)