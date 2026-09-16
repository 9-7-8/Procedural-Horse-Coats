The **Sorraia** should be one of the mod’s most genetically unified natural breeds: a rare Portuguese Iberian conservation horse, consistently dun or grullo/grulla, with strong primitive markings and no ordinary white-pattern, gray, cream, leopard, roan, or magical variation in pure founders. Its defining genotype is `D_` at the dominant dun locus, usually on a black-based `E_ aa` background for mouse-dun/grullo, with a minority bay-dun-compatible genotype if the mod needs the wider historical Sorraia palette. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

## Identity & flavour

The **Sorraia horse**, also called the **Sorraia**, *Sorraia do Vale do Sorraia*, or occasionally the Sorraia Portuguese primitive horse, is a rare Iberian breed from the low river valleys of the Sor and Raia in southern Portugal. It is associated with the Tagus basin and the broad wet, open, seasonally harsh landscape between Lisbon and the Spanish border. Although the breed is sometimes described romantically as a surviving wild horse or a direct ancestral stock of Iberian breeds, the careful scientific position is more modest: Sorraias are a distinct, endangered domestic/feral-derived Iberian population with unusual primitive traits and a severe historical genetic bottleneck. Their early ancestry remains an active research subject. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

The breed came to wider attention in 1920 when Portuguese zoologist and palaeontologist **Dr. Ruy d’Andrade** encountered a small wild-living population in the lowlands. He gathered animals and began a preservation program, but the breed remained numerically tiny. That bottleneck—and later conservation breeding from a very small foundation—makes the Sorraia a living rarity rather than a widespread riding breed. It has never been selected for modern racing, high jumping, carriage glamour, or specialized arena work; its historic survival value lies in hardiness, low-input living, and its unusually primitive visual type. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

Sorraias are true horses rather than formal ponies, but they are small: about **13.3–14.3 hands**, with 14.3 hands often treated as an upper limit. They have a long, narrow, convex or subconvex “Roman” profile; narrow head; long ears; deep but relatively narrow body; sloping shoulder; straight or slightly hollow back; low-set tail; and hard, clean legs with little to no feather. The mane and tail are characteristic: dark central hairs are often fringed or mixed with paler, sometimes nearly white hairs, producing a subtle two-toned look rather than a true flaxen chestnut mane. They should look light, dry, and tough—more marsh-and-scrub survivor than polished show horse. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

The Sorraia’s coat is its instant signature. Pure animals are described as **dun** or **grullo/grulla**, usually a yellow-dun or mouse-dun body with dark points and an emphatic black dorsal stripe. Black-tipped ears, a dark muzzle, zebra bars on the lower legs, shoulder stripes, neck striping, and spiderweb/cobweb markings on the forehead are common. Sorraia foals may be born with particularly conspicuous zebra-like striping. This is genuine **dun dilution**, not gray: `D` at the `TBX3` locus dilutes body pigment while leaving mane, tail, head, and lower legs relatively dark, and it produces primitive markings. A blue-dun/grullo Sorraia should therefore remain mouse-colored through life rather than turning pale with age as a true gray would. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

Sorraias are alert, intelligent, independent, frugal, hardy, and sure-footed. Their visual and genetic gameplay role is clear: breed toward a small, low-maintenance, primitive-marked Iberian horse with dark face, dorsal stripe, leg barring, dark-centered mane and tail, and virtually no decorative white. A player should recognize one across a field even before opening its genome. The mod does not model Sorraia head profile, ear length, mane bi-coloration, seasonal coat, striped newborn coat, hoof quality, wetland survival, low-input forage use, historical founder bottleneck, sex-chromosome mosaicism, social behavior, or the debated relationship between Sorraias, ancient Iberian horses, and later Iberian domestic breeds. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

## Breed JSON

> **Schema caveat:** The provided Procedural Horse Genetics wiki page could not be retrieved by the documentation fetcher. This file follows the readable JSON convention in your supplied Anglo-Arabian example. Before compiling, match all keys, allele names, possible `nd1` versus `nd2` representations, source enum values, commonness names, price units, and stat-target serialization with `common/breed/spec/`.
>
> **Scientific-genetics policy:** Pure Sorraia founders are intentionally almost fixed to the wild-type primitive phenotype. The core `D` allele is fixed because dun/grullo coat color and primitive markings are defining breed features; grey and all white-pattern loci are excluded because pure Sorraias are described as having no white markings. Since no reliable breed-wide genotype count was located separating black-dun from bay-dun Sorraias, `E` and `a` are weighted toward black-based grullo without claiming measured allele frequency. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

```json
{
  "id": "sorraia",
  "name": "Sorraia",
  "type": "natural",
  "notes": "The Sorraia is a rare Portuguese Iberian conservation breed defined by an endangered lowland population, primitive dun or grullo coloration, strong dorsal and zebra striping, dark muzzle, black-tipped ears, a convex head profile, pale-fringed dark mane and tail, low-input hardiness, and a severe historical bottleneck. Procedural Horse Genetics does not model the Roman profile, ear length, mane bi-coloration, seasonal coat, zebra-striped foal coat, hoof hardness, fertility, wetland or scrubland adaptation, feral herd behavior, historic bottleneck, sex-chromosome mosaicism, conservation pedigree, or unresolved ancient-Iberian ancestry.",

  "biomes": [
    "minecraft:meadow",
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:swamp",
    "minecraft:river",
    "minecraft:forest",
    "minecraft:sparse_jungle",
    "minecraft:stony_shore"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 980,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.92,
      "e": 0.08
    },
    "agouti": {
      "A": 0.18,
      "a": 0.82
    },
    "dun": {
      "D": 1.0
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
    "jump": 4,
    "health": 9,
    "size": [
      0.88,
      0.98
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Sorraia × Sorraia produces Sorraia. Sorraia × another pure breed produces a Sorraia cross. A Sorraia cross bred back to pure Sorraia remains that cross under the default system. Two different crosses produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not grant Sorraia lineage to every dun or grullo horse: real Sorraia identity depends on the tiny managed Portuguese conservation population and pedigree, not coat color alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Core phenotype | `D: 1.0` | Sorraias are consistently described as dun or grullo/grulla with a black dorsal stripe and other primitive markings. Fixing the dominant dun allele makes every pure founder generate the defining phenotype.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse) |
| Dun biology | `TBX3` dominant `D` | Dun lightens body color while leaving the mane, tail, head, and lower legs comparatively undiluted; it is associated with dorsal striping, leg bars, shoulder stripes, and forehead cobwebbing.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/) |
| Black-based bias | `E: 0.92`; `a: 0.82` | Blue dun/grullo or mouse-dun is especially characteristic in many modern descriptions. Strong `E` and `a` bias produces mainly \(E\_\,aa\,D\_\) founders while leaving a minority \(E\_\,A\_\,D\_\) yellow/bay-dun population. These values are explicit gameplay approximations, not measured Sorraia allele frequencies.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse) |
| Chestnut/red dun | `e: 0.08` | Red dun is not a standard headline Sorraia color. Keeping `e` low permits extremely rare red-dun-like variation only if the mod intends to represent historical/population variation; set `e: 0.0` for a stricter visual preservation program. |
| Bay dun | `A: 0.18` | Yellow/bay dun is described alongside mouse dun. A low-but-real `A` rate allows it without making ordinary yellow dun more common than the hallmark grullo.  [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141312004386) |
| Gray | Forced wild type | “Mouse gray” or *rato* is a color description for grullo; it is not evidence for the progressive gray `G` allele. A true gray would whiten with age and disrupt the stable primitive-dun phenotype.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse) |
| Cream, champagne, silver, pearl, mushroom, flaxen | Forced wild type | The pure Sorraia phenotype is dun/grullo with dark points and pale-fringed mane hair, not a population of cream, champagne, silver, pearl, mushroom, or true flaxen dilution.  [breeds.okstate](https://breeds.okstate.edu/horses/sorraia-horses) |
| White-pattern loci | All forced wild type | Pure Sorraias are described as having no white markings. Therefore tobiano, sabino, frame, splash, KIT white, roan, rabicano, leopard complex, PATN, and brindle must not originate in pure founders.  [breeds.okstate](https://breeds.okstate.edu/horses/sorraia-horses) |
| Magical loci | All forced wild type | Sorraia primitive striping comes from real dun genetics, not magical zebra or mane-tail coloration. |
| Disorders | All named loci clear | No credible Sorraia-specific carrier-frequency study was located for the requested disease panel. A rare bottlenecked breed may have genetic-management concerns, but inventing disease alleles from unrelated breeds is not scientifically defensible. |
| Speed | `5/10` | A practical, agile, medium-paced small horse; not a specialist race breed. |
| Jump | `4/10` | Tough legs and rough-ground ability do not equal purposeful jump selection. |
| Health | `9/10` | Represents unusual survival ability under harsh, low-input conditions. It does not mean the population is free from inbreeding effects or all hereditary disease.  [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141312004386) |
| Size | `×0.88–0.98` | Models a small true horse centered near 13.3–14.3 hands, with lower individuals possible in the population.  [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse) |

## Disorder approach

All named disorder loci are **clear** in pure Sorraia founders.

This is a deliberate evidence standard. The Sorraia is rare and bottlenecked, and research has highlighted its low diversity and value as a model for studying inbreeding-related biological effects. That makes health monitoring important—but it does **not** establish carrier frequencies for ACAN dwarfism, PLOD1-associated WFFS, MET/EDNRB lethal white syndrome, Arabian SCID/CA/LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141312004386)

Pure-founder `N: 1.0` entries avoid making unsupported medical claims. It does not make a Sorraia genetically invulnerable. It simply means no requested mutation is seeded without evidence. Those alleles may still enter Sorraia-cross populations through outcrossing, mutation, or the mod’s splice mechanics.

### Important dun distinction

If the mod models three `TBX3` alleles—`D`, `nd1`, and `nd2`—the strict version should make Sorraia founders **`D` only**, as in this file.

- `D` produces true dun dilution plus primitive markings.
- `nd1` does not dilute the coat but can retain variable primitive markings.
- `nd2` produces neither dun dilution nor primitive markings.

Do not substitute `nd1` simply to generate a dorsal stripe. A `nd1` horse can carry primitive marks while remaining undiluted, whereas the breed’s signature is the full pale-body, dark-points dun phenotype. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add the `sorraia` natural-breed record with Portuguese conservation flavour, rare commonness, wetland/scrub biome mapping, fixed dun pool, clear disorders, and small hardy stats. |
| `common/breed/Breeds` | Register `sorraia` so it resolves in commands, breed books, H-menu listings, wild packs, saved genomes, and lineage displays. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is intentionally omitted because this is an exceptionally rare conservation breed rather than a normal commercial sale-yard horse. |
| `common/breed/BreedBands` | Permit the empty epigenetic-band object. Do not use a band to fake primitive striping: the coat must derive from `TBX3` dun. |
| `common/breed/spec/` | Match this illustrative structure to the real serializer and parser. In particular, verify whether dun is a binary `D/N` locus or the biologically more detailed `D/nd1/nd2` locus. |
| `common/breed/Commonness` | Confirm `VERY_RARE` maps to the requested 0.75 spawn-weight rung. |
| `common/breed/BreedStatCurve` | Translate speed 5, jump 4, health 9, and size ×0.88–0.98 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Roll `E/e` and `A/a` from the constrained pools, force `D`, and force every excluded coat/disorder locus to wild type or clear. |
| `common/breed/BreedLineage` | Apply the normal pure/cross/Mixed table. A horse cannot acquire a Sorraia label just because it is small, dun, and striped. |
| `common/genetics/SpliceOutcome` | No exception. A splice-carrot allele may transmit as usual and can create a nonstandard Sorraia descendant, including a pinto, gray, dilution, or disease allele. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize the compact, hardy, modest-performance stat targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the actual format mandates an explicit default. |

## Verification

1. Confirm **Sorraia** appears in the H-menu’s Breeds tab and breed book with Portuguese/Iberian conservation identity, very-rare commonness, small size, primitive-marking description, and solid-color restrictions.

2. Spawn packs in meadow, river-lowland, swamp-margin, plains, scrub/savanna, and sparse-forest environments. Every member of a selected Sorraia pack must display the **Sorraia** breed label.

3. Confirm a lone wild horse reads **Feral Mixed**, even if it is grullo, mouse dun, has a dorsal stripe, or stands in a Portuguese-looking wetland biome.

4. Generate at least 500 pure Sorraia founders. Every founder must carry `D` and show a true dun phenotype:
- Most should be black-based mouse-dun/grullo.
- A smaller minority should be bay/yellow dun.
- Red dun should be extremely rare under the `e: 0.08` pool; eliminate it by fixing `E` if a stricter preservation phenotype is desired.
- All should carry a dorsal stripe, dark points, and the mod’s normal primitive-marking expression.

5. Confirm that no pure founder produces ordinary non-dun black, bay, chestnut, gray, palomino, buckskin, champagne, silver dapple, pearl, mushroom, roan, tobiano, frame, splash, sabino, dominant white, rabicano, leopard complex, brindle, or magical effects.

6. Inspect genome panels. Verify:
- `D` is fixed.
- `E` is overwhelmingly common.
- `a` is strongly favored.
- `G`, cream, pearl, champagne, silver, mushroom, flaxen, pinto/white loci, roan, rabicano, leopard genes, brindle, magical genes, and every named disorder locus are all wild type or clear.

7. If the mod uses `D/nd1/nd2`, verify that founders inherit only `D`, never `nd1` or `nd2`. A pure founder must not rely on non-dun primitive markings to resemble a Sorraia.

8. Check mature stats: Sorraias should be small, very hardy, of baseline practical speed, and modest jumping ability. They should not rival racehorses in speed, modern sport horses in jumping, or large riding horses in height.

9. Test default lineage:
- Sorraia × Sorraia → Sorraia.
- Sorraia × Lusitano → Sorraia cross.
- Sorraia × Konik → Sorraia cross.
- Sorraia cross × pure Sorraia → the existing Sorraia cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate genetic introduction. Cross in gray, cream, tobiano, leopard complex, `nd2`, or any disease allele. Descendants should render and inherit normally but must be a cross or Mixed lineage, never a visually-based pure Sorraia.

## Sources

- [FEI — Breed Profile: The Sorraia Horse](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse): Portuguese/Iberian origin, Ruy d’Andrade’s 1920 rediscovery, height ceiling, primitive-marking description, conservation context, and uncertainty around ancient ancestry. [fei](https://www.fei.org/stories/lifestyle/health-fitness/breed-profile-sorraia-horse)

- [Oklahoma State University — Sorraia Horses](https://breeds.okstate.edu/horses/sorraia-horses): detailed standard description of dun/grullo-only color, dorsal stripe, dark muzzle, black ear tips, zebra striping, white-marking absence, pale-fringed dark mane/tail, Roman profile, and small-horse height. [breeds.okstate](https://breeds.okstate.edu/horses/sorraia-horses)

- [UC Davis Veterinary Genetics Laboratory — Dun Dilution](https://vgl.ucdavis.edu/test/dun-horse): authoritative `TBX3` dun biology, `D`/`nd1`/`nd2` allele distinctions, dominant inheritance, and primitive-marking mechanisms. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)

- [Regulatory mutations in TBX3 disrupt asymmetric hair pigmentation in dun horses](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/): peer-reviewed molecular evidence that `TBX3` regulation underlies dun dilution and primitive markings. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC4731265/)

- [Genetic diversity and demographic structure of the endangered Sorraia horse breed](https://www.sciencedirect.com/science/article/abs/pii/S1871141312004386): peer-reviewed context for endangered status, severe demographic constraints, hardiness, and primitive dun markings. [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141312004386)

- [First evidence of sex chromosome mosaicism in the endangered Sorraia Horse breed](https://www.sciencedirect.com/science/article/abs/pii/S1871141310004221): peer-reviewed evidence supporting caution around genetic bottleneck and inbreeding claims. [sciencedirect](https://www.sciencedirect.com/science/article/abs/pii/S1871141310004221)