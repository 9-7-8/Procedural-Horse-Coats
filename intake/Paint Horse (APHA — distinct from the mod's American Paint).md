The **Paint Horse (APHA)** should be a genetically broad American stock-horse breed whose defining feature is its eligible **Paint-pattern genetics and APHA pedigree**, not merely any colored horse. It must remain distinct from the mod’s generic “American Paint”: this file represents the registered **American Paint Horse Association** population, whose eligible white-pattern loci include tobiano, frame overo, Sabino 1, multiple splash-white alleles, and several dominant-white variants, with substantial solid Paint-bred horses also possible. [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel)

## Identity & flavour

The **American Paint Horse**, commonly called the **Paint Horse** or simply a **Paint**, is an American stock-horse breed administered by the **American Paint Horse Association** (**APHA**). It arose from the same broad western ranch-horse foundation that produced the American Quarter Horse, with additional Thoroughbred influence allowed through qualifying pedigrees. The modern APHA was created in 1965 through the merger of the American Paint Stock Horse Association and the American Paint Quarter Horse Association, both formed to preserve and register the stock-type horses with pinto patterns that earlier registry rules had often excluded. The first APHA registration belonged to Bandit’s Pinto, a black tobiano foaled in 1959. [en.wikipedia](https://en.wikipedia.org/wiki/American_Paint_Horse)

A registered APHA Paint is not simply “a horse with white spots.” It is a pedigree breed. Its sire and dam must be APHA-, AQHA-, or Jockey Club-registered, with at least one APHA-registered parent; regular-registration eligibility also involves a qualifying natural Paint marking or an approved white-pattern allele. APHA also registers **Solid Paint-Bred** horses that have the right pedigree but do not display enough qualifying white. That distinction matters in the mod: pure APHA founders should sometimes be visually solid, while an ordinary tobiano horse with no Paint lineage should remain a different breed or cross. [apha](https://apha.com/registration/the-breed/)

Paints generally stand about **14–16 hands**, with compact, muscular American stock-horse conformation: a refined but broad-headed profile, strong arched neck, sloping shoulder, short-coupled back, deep heartgirth, rounded hip, powerful hindquarters, and sturdy clean legs. They were shaped for ranch work, cattle handling, rodeo, trail riding, western pleasure, reining, cutting, racing, and all-around family use. A good APHA Paint should read as athletic and quick over a short distance, with powerful acceleration and a level-headed working disposition—not as a tall warmblood, feathered cob, delicate racehorse, or gaited trail specialist. [apha](https://apha.com/about_apha/)

The breed’s defining visual vocabulary is genetic. **Tobiano** creates smooth, rounded white patches that cross the topline between withers and tail, usually with white legs and a relatively dark head. **Overo** is APHA’s broad descriptive category for non-tobiano white patterns, including frame overo, splash white, sabino, and other forms: frame commonly produces sharp horizontal body patches and a bold face while leaving the topline colored; splash often looks as if the horse has been dipped in white paint from below; sabino can range from high white and belly spots to roaning at marking edges. **Tovero** describes a horse combining tobiano and overo-pattern genetics or features. [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel)

APHA Paints may be bay, black, brown, chestnut, sorrel, gray, palomino, buckskin, cremello, perlino, smoky cream, dun, champagne, silver, roan, pearl, and more. The correct mod experience is therefore not “every Paint is a tobiano.” It is a colorful stock-horse population in which many founders visibly carry a qualifying pattern, some are solid Paint-Bred, and combinations create huge variety. Players should breed toward strong, compact western performance horses with bold, inheritable white patterning—and should learn that pairing two frame carriers can produce lethal white syndrome. The mod does not model APHA pedigree eligibility, marking measurements, DNA parentage, registry divisions, blue-eye deafness risk, cattle sense, reining/cutting training, cow work, western events, gait quality, subjective stock-horse “type,” or show-ring points. [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics wiki URL could not be retrieved by the documentation fetcher. This file uses the readable JSON convention established in your example. Before merging, reconcile locus IDs, allele names, pattern names, disease mapping, price units, rarity enum values, and source-list syntax against `common/breed/spec/`.
>
> **Scientific-frequency caveat:** APHA and UC Davis provide exceptionally strong evidence for which pattern genes are relevant to the breed, but I did not find a current, representative APHA-wide allele-frequency study for every coat locus. Therefore the numeric coat probabilities below are **explicit gameplay approximations**—designed to produce a majority visually patterned population plus a meaningful Solid Paint-Bred minority—not claimed APHA registry frequencies. For disorders, the closest breed-specific study found a control American Paint Horse group with `EDNRB`/LWFS allele frequency 0.107 and `SCN4A`/HYPP allele frequency 0.025; disease pools below preserve those reported values where the mod has matching loci. [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel)

```json
{
  "id": "apha_paint_horse",
  "name": "Paint Horse",
  "type": "natural",
  "notes": "The APHA Paint Horse is defined by qualifying APHA/AQHA/Jockey Club pedigree, APHA registration rules, stock-horse conformation, and one or more eligible white-pattern alleles or markings. Procedural Horse Genetics does not model APHA pedigree qualification, parentage testing, Regular Registry versus Solid Paint-Bred registration status, marking measurements, underlying skin pigmentation, blue-eye deafness risk, cattle sense, reining, cutting, barrel racing, western pleasure, stock-horse show type, or performance records.",

  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
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
  "price": 820,
  "commonness": "COMMON",

  "coat_genes": {
    "extension": {
      "E": 0.64,
      "e": 0.36
    },
    "agouti": {
      "A": 0.60,
      "a": 0.40
    },

    "cream": {
      "N": 0.80,
      "Cr": 0.20
    },
    "champagne": {
      "N": 0.94,
      "Ch": 0.06
    },
    "dun": {
      "N": 0.90,
      "D": 0.10
    },
    "silver": {
      "N": 0.96,
      "Z": 0.04
    },
    "pearl": {
      "N": 0.98,
      "Prl": 0.02
    },
    "mushroom": {
      "N": 1.0
    },
    "flaxen": {
      "N": 0.91,
      "f": 0.09
    },
    "grey": {
      "N": 0.91,
      "G": 0.09
    },

    "tobiano": {
      "N": 0.60,
      "TO": 0.40
    },
    "frame_overo": {
      "N": 0.893,
      "O": 0.107
    },
    "sabino_1": {
      "N": 0.82,
      "SB1": 0.18
    },
    "splash_white_1": {
      "N": 0.86,
      "SW1": 0.14
    },
    "splash_white_2": {
      "N": 0.97,
      "SW2": 0.03
    },
    "kit_white_spotting": {
      "N": 0.98,
      "W": 0.02
    },
    "roan": {
      "N": 0.88,
      "Rn": 0.12
    },
    "rabicano": {
      "N": 0.94,
      "Rb": 0.06
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
      "N": 0.955,
      "gbed": 0.045
    },
    "CVM": {
      "N": 1.0
    },
    "megaesophagus": {
      "N": 1.0
    },
    "SCN4A_HYPP": {
      "N": 0.975,
      "HYPP": 0.025
    },
    "GYS1_PSSM1": {
      "N": 0.965,
      "PSSM1": 0.035
    },
    "PPIB_HERDA": {
      "N": 0.9825,
      "HERDA": 0.0175
    }
  },

  "stat_scores": {
    "speed": 7,
    "jump": 5,
    "health": 7,
    "size": [
      0.98,
      1.10
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. APHA Paint Horse × APHA Paint Horse produces Paint Horse. Paint Horse × another pure breed produces a Paint Horse cross. A Paint Horse cross bred back to pure Paint Horse remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. The mod must not label a horse as APHA Paint Horse merely because it has a tobiano, splash, sabino, frame, or tovero appearance; real APHA identity is pedigree- and registration-based."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Identity | Separate `apha_paint_horse` ID | This prevents collision with the mod’s existing generic “American Paint.” The APHA breed is a specific pedigree-and-registration population with defined genetic eligibility logic, not a synonym for any pinto stock horse.  [en.wikipedia](https://en.wikipedia.org/wiki/American_Paint_Horse) |
| Base colors | `E: 0.64 / e: 0.36`; `A: 0.60 / a: 0.40` | Produces the expected bay, brown/black, chestnut, and sorrel stock-horse foundations. Exact rates are gameplay approximations because APHA does not publish a universal current Extension/Agouti frequency table.  [apha](https://apha.com/registration/genetics-101/) |
| Tobiano | `TO: 0.40` | Tobiano is a central APHA pattern and a dominant allele; it should be the most frequent single white-pattern locus in a founder pool built to visibly read as Paint.  [en.wikipedia](https://en.wikipedia.org/wiki/American_Paint_Horse) |
| Frame overo | `O: 0.107` | The 2009 inherited-disease allele-frequency study reported `LWFS`/frame allele frequency 0.107 in its control American Paint Horse group. Frame is a key APHA-eligible pattern but must be modeled with its homozygous lethal consequence via the existing `EDNRB`/MET system.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel) |
| Sabino 1 | `SB1: 0.18` | APHA expressly recognizes Sabino 1 for Regular Registry consideration. A moderate rate enables the breed’s characteristic high-leg white, facial white, belly spots, and roaned marking edges. Rate is gameplay-calibrated, not an APHA population survey.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel) |
| Splash white | `SW1: 0.14`, `SW2: 0.03` | APHA recognizes multiple splash-white alleles; `SW1` is appropriate as the primary model-supported splash allele. It enables crisp bottom-up white, white/tipped tails, blue eyes, and bold face patterns.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel) |
| KIT dominant white | `W: 0.02` | APHA recognizes several dominant-white alleles for registration eligibility. A low generic `KIT` white rate is appropriate only if the mod can distinguish a viable dominant-white allele from lethal or implausibly extreme variants.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel) |
| Tovero | Emergent, not its own locus | A tovero phenotype should arise when tobiano coexists with one or more overo-class genetics; it must not be represented as a separate allele.  [en.wikipedia](https://en.wikipedia.org/wiki/American_Paint_Horse) |
| Cream, champagne, dun, silver, pearl, gray, roan | Present at low-to-moderate rates | APHA’s own color/pattern panel includes these loci, and the breed accepts their corresponding colors. Rates are controlled gameplay approximations.  [apha](https://apha.com/registration/genetics-101/) |
| Rabicano | `Rb: 0.06` | A small modifier rate adds plausible tail/flank white texture in a breed already rich in white-pattern variation. This is an implementation approximation, not a published APHA rate. |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type leopard inheritance is not a core APHA Paint pattern system and should not appear in pure founders. |
| Magical loci | All forced wild type | Paint Horses are natural horses; their dramatic color variation comes from real equine coat genetics. |
| HYPP | `SCN4A: 0.025` | The same American Paint Horse subgroup study reported an HYPP allele frequency of 0.025. HYPP is dominant: one mutant allele is medically relevant, so the implementation must not call heterozygotes harmless carriers.  [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf) |
| GBED, PSSM1, HERDA | Conservative proxy rates | These variants occur in Quarter Horse-related populations, including APHA-relevant bloodlines, but the exact APHA-wide figures were not found in the retrieved sources. `GBED 0.045`, `PSSM1 0.035`, and `HERDA 0.0175` are transparent conservative gameplay proxies, not direct APHA prevalence estimates. Replace them with an APHA-specific genetic-health survey if one is available.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |
| Size | `×0.98–1.10` | Keeps the breed within the common 14–16-hand medium stock-horse range.  [breedbreeze](https://breedbreeze.com/american-paint-horse/) |
| Speed | `7/10` | Represents compact stock-horse acceleration, ranch athleticism, barrel-racing potential, and general western performance, not Thoroughbred-level flat-racing speed.  [apha](https://apha.com/about_apha/) |
| Jump | `5/10` | Balanced athletic ability without making a western stock-horse breed a purpose-bred jumper. |
| Health | `7/10` | Represents robust utility and versatility, moderated because inherited Quarter Horse-line disease alleles are deliberately present in the pool. |

## Disorder approach

The APHA Paint Horse is one of the few requested breeds where a disease-aware founder pool is defensible, but the mechanism must be modeled correctly.

| Disorder / locus | Founder treatment | Important implementation rule |
|---|---|---|
| Lethal white syndrome | Do **not** duplicate in `disorder_genes`; use `frame_overo` / `EDNRB` | `O/N` creates a viable frame-overo horse. `O/O` is associated with lethal white syndrome. The control APHA group in one study had allele frequency 0.107.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel) |
| HYPP | `SCN4A_HYPP: 0.025` | HYPP is dominant. Both `N/HYPP` and `HYPP/HYPP` horses carry the mutation and may be clinically affected; founder generation should not describe `N/HYPP` as merely an unaffected carrier.  [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf) |
| PSSM1 | `GYS1_PSSM1: 0.035` | PSSM1 is also dominant. This is a cautious Quarter Horse-related proxy, not a direct APHA frequency. Replace if a breed-specific estimate is available.  [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf) |
| GBED | `GBE1_GBED: 0.045` | Recessive, Quarter Horse-related proxy rate only. The mod should render homozygous affected foals according to its actual disease model.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |
| HERDA | `PPIB_HERDA: 0.0175` | Recessive, conservative Quarter Horse-line proxy. It should be omitted if the project requires direct breed-specific prevalence rather than ancestry-informed estimates.  [aqha](https://www.aqha.com/widget/-/genetic-test-roundup) |
| All remaining listed diseases | Forced clear | No defensible APHA-specific rate was found for them in the retrieved sources. |

If you want the strictest possible standard—**direct APHA evidence only**—retain only the frame/`EDNRB` allele at 0.107 and HYPP at 0.025, because those values are directly reported for an American Paint Horse control group. Set GBED, PSSM1, and HERDA to clear until a direct APHA population report is supplied. [experts.umn](https://experts.umn.edu/en/publications/evaluation-of-allele-frequencies-of-inherited-disease-genes-in-su)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `apha_paint_horse` as a natural breed record, separate from generic `american_paint`, with APHA-focused notes, sources, gene pools, stats, and western-biome distribution. |
| `common/breed/Breeds` | Register the unique ID so commands, books, H-menu listing, spawning, lineage serialization, and saved-genome loading distinguish it from the pre-existing American Paint record. |
| `common/breed/BreedSource` | Validate `wild`, `cowboy`, `spawn_egg`, and `stable`. If the mod treats APHA horses as managed pedigree stock only, remove `wild`; the requested default here leaves it available in western-style herd generation for gameplay. |
| `common/breed/BreedBands` | Accept an empty epigenetic-band object. Do not use epigenetics to fake tovero, which must arise from combined inherited pattern loci. |
| `common/breed/spec/` | Verify the actual names for `EDNRB`/frame, `KIT`/tobiano/Sabino/W alleles, `MITF`/`PAX3` splash alleles, `PMEL`/silver, and disease allele serialization. Confirm whether the format supports multiple KIT-family variants separately. |
| `common/breed/Commonness` | Confirm `COMMON` maps to the requested moderate weight ladder and retain direct `spawn_weight: 6` only if the actual system uses numeric weights. |
| `common/breed/BreedStatCurve` | Map speed 7, jump 5, health 7, and size ×0.98–1.10 to valid stock-horse `TargetBand` settings. |
| `common/breed/BreedFounder` | Roll core and pattern loci independently so tobiano + overo-class alleles can create tovero. Force `LP`, `PATN`, mushroom, brindle, magical genes, and unsupported disorders clear. |
| `common/breed/BreedLineage` | Apply normal pure/cross/Mixed behavior. A pattern gene alone must never confer the `apha_paint_horse` lineage label. |
| `common/genetics/SpliceOutcome` | No exception: a spliced allele may reach a foal normally, creating a nonstandard but genetically valid descendant. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize medium stock-horse size, good acceleration-oriented speed, moderate jumping, and health targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is required unless the concrete schema mandates a default. |

## Verification

1. Confirm **Paint Horse** appears in the H-menu’s Breeds tab and the breed book under the unique internal ID `apha_paint_horse`, not as the existing generic `american_paint` record. The display description should mention APHA pedigree identity and the distinction between patterned and Solid Paint-Bred horses.

2. Spawn repeated packs in plains, western grassland, badlands, savanna-edge, meadow, and river environments. Every horse in one selected APHA pack must display **Paint Horse** as its breed label, even when some founders are solid or exhibit different pattern combinations.

3. Confirm that a lone unassigned wild horse reads **Feral Mixed**, not Paint Horse, even if it happens to be tobiano, splash, or frame overo.

4. Generate at least 1,000 pure founders. Expect a broad American stock-horse color population with:
- Bay, black/brown, chestnut, and sorrel foundations.
- A substantial tobiano population.
- Meaningful overo-class variation from frame, Sabino 1, and splash.
- Emergent toveros where tobiano combines with frame, splash, or sabino.
- A noticeable Solid Paint-Bred-like minority with no visibly qualifying white pattern.
- Minor cream, gray, dun, champagne, silver, pearl, and roan variation.

5. Confirm that pure founders cannot generate leopard complex, blanket spotting, snowflake, PATN patterns, mushroom, brindle, or magical coats.

6. Test pattern inheritance:
- `TO/N × N/N` should transmit tobiano to roughly half of foals.
- `O/N × N/N` should transmit frame to roughly half of foals.
- `SB1/N × N/N` and `SW1/N × N/N` should each transmit their allele to roughly half of foals.
- `TO/N × O/N` combinations should generate some tovero-phenotype offspring according to the renderer.
- Confirm that “overo” is a phenotype category, not a single genetic locus.

7. Test frame-related lethal-white logic:
- `O/N × O/N` should yield approximately 25% `O/O`, 50% `O/N`, and 25% `N/N` conceptions/foals over a large sample.
- The `O/O` outcome must follow the mod’s lethal-white model.
- Do **not** also roll a separate duplicate `MET` disorder gene for the same foal.

8. Test inherited disorders:
- `N/HYPP × N/N` should pass HYPP to approximately half of foals; affected heterozygotes must be medically flagged under the mod’s dominant-disease logic.
- `N/GBED × N/GBED` and `N/HERDA × N/HERDA` should yield approximately 25% homozygous affected outcomes if those proxy loci are retained.
- `N/PSSM1 × N/N` should transmit the dominant allele to approximately half of foals.
- If using the strict evidence-only variant, verify only frame and HYPP can vary in pure founders.

9. Test lineage:
- Paint Horse × Paint Horse → Paint Horse.
- Paint Horse × American Quarter Horse → Paint Horse cross.
- Paint Horse × Thoroughbred → Paint Horse cross.
- Paint Horse cross × pure Paint Horse → that existing Paint Horse cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Validate play feel. Mature APHA Paints should be medium-sized, quick, compact, capable western all-rounders with moderate jump ability and strong versatility. They should not become giant warmblood jumpers, Thoroughbred-speed racers, gaited-distance specialists, or automatically superior to every other western breed merely because their coat genetics are more varied.

## Sources

- [American Paint Horse Association — The History of APHA](https://apha.com/about_apha/): official formation history, 1962 founding organizations, 1965 merger, Bandit’s Pinto as first registration, and modern registry context. [apha](https://apha.com/about_apha/)

- [American Paint Horse Association — Breed characteristics](https://apha.com/registration/the-breed/): APHA descriptions of tobiano, overo, tovero, pattern orientation, and qualifying white-pattern genes. [apha](https://apha.com/registration/the-breed/)

- [American Paint Horse Association — Paint Horse Genetics 101](https://apha.com/registration/genetics-101/): APHA’s own color/pattern panel and list of recognized loci, including Extension, Agouti, cream, champagne, dun, gray, pearl, silver, frame, tobiano, Sabino 1, splash variants, and dominant-white variants. [apha](https://apha.com/registration/genetics-101/)

- [UC Davis Veterinary Genetics Laboratory — APHA White Pattern Registration Eligibility Panel](https://vgl.ucdavis.edu/test/apha-white-pattern-panel): diagnostic and registration relevance of frame, tobiano, Sabino 1, splash-white variants, and specified dominant-white alleles. [vgl.ucdavis](https://vgl.ucdavis.edu/test/apha-white-pattern-panel)

- [Evaluation of allele frequencies of inherited disease genes in subgroups of American Quarter Horses](https://experts.umn.edu/en/publications/evaluation-of-allele-frequencies-of-inherited-disease-genes-in-su): reports `LWFS`/frame allele frequency 0.107 and HYPP allele frequency 0.025 in a control American Paint Horse group. [experts.umn](https://experts.umn.edu/en/publications/evaluation-of-allele-frequencies-of-inherited-disease-genes-in-su)

- [Genetics of Muscle Disease](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf): APHA/Quarter Horse-related prevalence context for HYPP and PSSM1; used cautiously and not as a substitute for a modern APHA-wide direct dataset. [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf)