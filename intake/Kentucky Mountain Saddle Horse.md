The **Kentucky Mountain Saddle Horse** should be a compact-to-medium Appalachian gaited riding horse: solid-colored, smooth-moving, sure-footed, people-oriented, and suited to wooded hills rather than race tracks or formal sport-horse arenas. Its defining PHC identity is the natural four-beat **single-foot** gait—best represented through a high DMRT3 gait-keeper frequency if the mod supports it—paired with a broad but strictly solid-color founder palette and practical mountain-horse hardiness. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

## Identity & flavour

The **Kentucky Mountain Saddle Horse**, usually shortened to **KMSH**, is an American gaited saddle horse from the Appalachian region of eastern Kentucky. It arose from the everyday horses of isolated mountain farms: animals expected to carry a rider comfortably over steep, wooded, rocky ground; pull or assist with farm work; travel long distances; and remain useful to families with limited means and limited access to specialist breeding stock. The breed’s modern organization is relatively young—the **Kentucky Mountain Saddle Horse Association** was formed in 1989—but the horse itself descends from an older Appalachian tradition of naturally gaited mountain horses. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

The KMSH was made for useful, comfortable transportation. Appalachian riders needed a horse that could travel narrow trails and uneven hills without the jarring bounce of a trot, while still being calm enough for farm families and capable enough for light work. Its famous **single-foot** is a smooth, even, four-beat lateral ambling gait performed in place of the ordinary trot. A rider should feel the horse glide rather than bounce: it is a trail and distance comfort trait, not a manufactured show action. The gait occurs naturally and is associated with the equine DMRT3 “gait-keeper” mutation, which is documented in Kentucky Mountain Saddle Horses. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

The breed’s permitted height begins above **11 hands**, with two registration size classes: **Class B** is 11.0–14.1 hands, while **Class A** exceeds 14.2 hands. In practice, it ranges from small pony-sized mountain mounts to substantial riding horses near 16 hands. A typical KMSH is compact and muscular, with a deep chest, a short-to-medium back, strong hindquarters, a well-arched neck, an alert head with a straight or slightly dished profile, and clean, serviceable legs. The mane and tail are often full and flowing but are not genetically distinctive; the feet should feel sound and practical for rocky trail work. The breed has little emphasis on heavy feather. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

Unlike the chocolate-and-flaxen public image of its close relative, the Rocky Mountain Horse, the KMSH accepts a remarkably broad range of **solid** colors. Bay, black, chestnut, brown, palomino, buckskin, dun, gray, roan, and silver-associated chocolate shades can all occur in the wider KMSH population. Face, leg, and small belly white markings are allowed, but loud or excessive white—such as a bald face, high white beyond knees or hocks, or pinto-pattern expression—is not accepted into the KMSH main registry. Spotted horses belong instead with the affiliated **Spotted Mountain Horse Association**, so a pure KMSH founder pool should have no tobiano, frame, splash, sabino, or W-series loud-white pattern alleles. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

The Kentucky Mountain Saddle Horse is renowned for being willing, calm, gentle, personable, intelligent, and easy to handle. It is commonly valued as a family horse, trail horse, pleasure mount, and an excellent choice for riders who prioritize comfort and confidence over speed. That quiet temperament should not turn it into a sluggish breed: a good KMSH has a purposeful gait, plenty of trail stamina, and the sensible self-preservation expected of a horse developed on Appalachian slopes. Its athletic identity is steady forward travel, not explosive jumping or racetrack sprinting. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

In *Procedural Horse Genetics*, the breed should offer a distinctly American mountain-horse palette and a real reason to seek it out: smooth-gait genetics, solid coats in nearly every normal base-color and dilution family, useful size variation, and forgiving trail-horse durability. Players should instantly recognize an ideal founder as a solid, clean-legged, compact horse moving comfortably through a forested ridge biome—perhaps a chocolate silver-dapple-like horse with flaxen mane and tail, a buckskin, a dun, a bay roan, a gray, or a black single-footer. The mod does **not** model the single-foot animation or ride smoothness unless it has a dedicated gait system; it also does not model trail judgment, rider comfort, head profile, mane fullness, hoof quality, temperament, registration size classes, eligibility inspections, or the registry’s separate handling of spotted horses.

## Breed JSON

> **Schema note:** The supplied PHC wiki URL could not be retrieved. This definition follows the readable structure in the provided Anglo-Arabian example. The list of coat loci must be reconciled against the real project schema in `common/breed/spec/`. In particular, determine whether PHC already has a `DMRT3`/gait locus, the exact spelling of `silver`, `cream`, `roan`, and `grey`, and whether KMSH color implementation distinguishes ordinary white markings from true KIT, MITF, PAX3, EDNRB, and tobiano patterns.

```json
{
  "id": "kentucky_mountain_saddle_horse",
  "name": "Kentucky Mountain Saddle Horse",
  "type": "natural",
  "notes": "Procedural Horse Genetics models the Kentucky Mountain Saddle Horse's solid-color coat pool, variable mountain-horse size, hardy trail aptitude, and—if implemented—the DMRT3-associated tendency toward a natural four-beat single-foot gait. It does not model actual gait animation, ride smoothness, gait quality, trail sense, sure-footedness, Appalachian training, head profile, mane and tail fullness, hoof quality, temperament, KMSHA Class A and Class B registration categories, white-marking inspection, or the separate Spotted Mountain Horse registry.",

  "biomes": [
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:dark_forest",
    "minecraft:meadow",
    "minecraft:windswept_forest",
    "minecraft:windswept_hills",
    "minecraft:grove",
    "minecraft:plains"
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
      "E": 0.72,
      "e": 0.28
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "cream": {
      "N": 0.82,
      "Cr": 0.18
    },
    "silver": {
      "N": 0.82,
      "Z": 0.18
    },
    "dun": {
      "D": 0.13,
      "nd1": 0.12,
      "nd2": 0.75
    },
    "champagne": {
      "N": 0.95,
      "Ch": 0.05
    },
    "grey": {
      "N": 0.85,
      "G": 0.15
    },
    "roan": {
      "N": 0.86,
      "Rn": 0.14
    },
    "flaxen": {
      "N": 0.72,
      "f": 0.28
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

    "dmrt3_gait_keeper": {
      "C": 0.05,
      "A": 0.95
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
    "speed": 6,
    "jump": 5,
    "health": 8,
    "size": [
      0.86,
      1.08
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Kentucky Mountain Saddle Horse × Kentucky Mountain Saddle Horse produces Kentucky Mountain Saddle Horse. Kentucky Mountain Saddle Horse × another pure breed produces a Kentucky Mountain Saddle Horse cross. A Kentucky Mountain Saddle Horse cross bred back to a pure Kentucky Mountain Saddle Horse remains that named cross; different crosses resolve to Mixed, and any cross involving Feral Mixed resolves to Mixed. A foal with a smooth gait or solid chocolate coat must not receive a KMSH label solely from phenotype."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Reasoning |
|---|---:|---|
| Base colors | `E` 0.72 / `e` 0.28; `A` 0.68 / `a` 0.32 | The KMSH accepts solid horses in a broad conventional color range. A relatively open base-color pool produces bay, black, brown, and chestnut foundations rather than locking the breed into the chocolate phenotype strongly associated with Rocky Mountain Horses.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Cream | `Cr` 0.18 | Palomino and buckskin are repeatedly listed among permitted KMSH colors. A moderate cream pool makes both recognizable but not dominant; double cream may arise normally from carrier matings because the real registry’s main restriction is excessive white patterning, not cream dilution.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Silver | `Z` 0.18 | Chocolate shades with flaxen mane and tail are culturally linked to the closely related Appalachian Mountain Horse family, especially the Rocky Mountain Horse. KMSH color sources also include silver-associated/chocolate-type colors. A moderate silver pool gives the breed a meaningful chance of the dark-body, light-mane look without confusing it with a Rocky Mountain Horse clone.  [livestockconservancy](https://livestockconservancy.org/rocky-mountain-horse-breed/) |
| Dun | `D` 0.13 | Dun is an accepted but less signature KMSH color. This low-to-moderate frequency gives players occasional dun, red dun, and grullo founders while keeping the breed’s identity broader than a dun landrace.  [oasisvets](https://www.oasisvets.com/services/equine/breeds/kentucky-mountain-saddle-horse) |
| Champagne | `Ch` 0.05 | Champagne is plausibly present in the broad solid-color American mountain-horse gene pool, but direct breed-specific prevalence evidence is limited. This deliberately low value is a gameplay approximation; set it to `N: 1.0` if PHC demands exclusively documented breed-specific allele frequencies. |
| Gray | `G` 0.15 | Gray is listed as a KMSH color. Its moderate frequency ensures a visible but minority gray line without turning the majority of a founder herd gray over time.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Roan | `Rn` 0.14 | Roan is listed among KMSH colors. The proposed level allows occasional bay roans, red roans, and black roans as a normal part of a broad solid-color saddle-horse population.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Flaxen | `f` 0.28 | A substantial flaxen pool supports light mane and tail on chestnut-based horses and reinforces the Appalachian “chocolate/flaxen” aesthetic when visually appropriate, while silver remains responsible for the classic dark-base silver-dapple effect. |
| White pattern loci | Forced wild type | The KMSHA registers solid horses. Excessive white, bald faces, high leg white, and pinto patterns—including tobiano, overo, and sabino—belong in the affiliated Spotted Mountain Horse Association rather than the KMSH main registry. Therefore pure KMSH founders must not carry tobiano, frame, splash, sabino, W-series dominant white, leopard, rabicano, or brindle loci.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| DMRT3 gait locus | `A` 0.95 | The DMRT3 stop-gain “gait-keeper” variant is documented in Kentucky Mountain Saddle Horses and enables alternate gaits. A 95% allele pool makes naturally gaited founders standard while allowing a very small residual non-gaited outcome if PHC models the locus probabilistically. If the mod categorically treats a recognized gaited breed as fixed, use `A: 1.0`.  [omia](https://omia.org/OMIA001715/9796/) |
| Disorders | All listed loci clear | No trustworthy KMSH-specific carrier-rate data were located for the listed disorder panel. PSSM1 occurs across several horse groups and is a dominant GYS1 condition, but general occurrence in horses is not evidence for assigning it a Kentucky Mountain Saddle Horse founder frequency. Importing Quarter Horse, Arabian, Friesian, or Warmblood rates would be speculative.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3297474/) |
| Speed | 6/10 | The single-foot provides efficient, purposeful trail travel, but this is not a flat-racing breed. Slightly above baseline captures useful forward movement without treating gait comfort as sprint performance.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Jump | 5/10 | KMSHs can navigate trail obstacles and uneven Appalachian country, but jumping is not the central historic selection target. Baseline jumping is appropriate.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |
| Health | 8/10 | This represents practical mountain durability, sure-footedness, soundness, and outdoor working versatility. It is intentionally below the extreme 9/10 assigned to heavily primitive mountain landraces because the KMSH is a modern, purpose-bred saddle population with broad ancestry and major size variation.  [oasisvets](https://www.oasisvets.com/services/equine/breeds/kentucky-mountain-saddle-horse) |
| Size | ×0.86–1.08 | The official registration system starts above 11 hands, with Class B extending to 14.1 hands and Class A above 14.2 hands; reported individuals reach around 16 hands. This wide band preserves the defining compact mountain-mount origin while permitting the larger registered saddle-horse type.  [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse) |

### Gait implementation note

`dmrt3_gait_keeper` appears in `coat_genes` only because the prompt’s sample structure does not supply a separate performance-gene section. If PHC has a dedicated trait or locomotion namespace, move it there—for example, `performance_genes`, `gait_genes`, or the actual DMRT3 locus record—rather than treating gait as pigmentation.

The causal variant is the DMRT3 nonsense mutation, commonly described as **Ser301STOP**, whose alternate allele is usually written as `A` in commercial testing. It is permissive for alternate gaits, but genotype alone does not guarantee identical gait quality; conformation, training, management, and other genetics also matter. A PHC implementation should treat it as enabling the KMSH’s smooth-gait tendency, not as an unconditional animation override. [omia](https://omia.org/OMIA001715/9796/)

## Spawning & lineage

The habitat choice favors Appalachian-feeling ground: deciduous forest, birch forest, dark forest, meadow, grove, forested slopes, and windswept hills. These biomes evoke eastern Kentucky’s wooded ridges, hollows, farms, and trail country much more accurately than tundra, desert, jungle, or open savanna. The breed should be found in modest forested mountain terrain, where its single-foot and steady mind feel naturally valuable. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)

`UNCOMMON` with `spawn_weight: 3` is a good balance. KMSHs are a living American breed with an active registry, but not a generic global horse population. Finding one should reward a player looking in the right terrain without making smooth-gait genetics so rare that ordinary survival breeding cannot access it.

No lineage exception is needed. The official registry can admit certain horses associated with related Mountain Horse registries, but PHC should preserve its ordinary breed-label logic. A KMSH × Rocky Mountain Horse foal may inherit a solid chocolate coat, flaxen mane, and the DMRT3 gait allele, yet it should still read as a named cross unless the mod someday implements studbook-specific pedigree admission rules.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Adds `kentucky_mountain_saddle_horse`, natural classification, display metadata, notes, biome list, sources, price, and commonness. |
| `common/breed/Breeds` | Registers the breed ID for spawn selection, entities, genome records, books, menus, commands, and lineage display. |
| `common/breed/BreedSource` | Validates `wild`, `cowboy`, `spawn_egg`, and `stable` as allowed KMSH acquisition sources. |
| `common/breed/BreedBands` | Serializes the intentionally empty epigenetic-band map. No uniform dark “chocolate” band is used because KMSHs are legitimately broad in solid color. |
| `common/breed/spec/` | Confirms JSON key names and values, including source enums, commonness spelling, stat targets, and the real locus ID for DMRT3. Add a dedicated gait-gene serialization field if one does not already exist. |
| `common/breed/Commonness` | Confirms that `UNCOMMON` and numerical `spawn_weight: 3` map to the intended rarity/commonness ladder. |
| `common/breed/BreedStatCurve` | Maps 6 speed, 5 jump, 8 health, and size ×0.86–1.08 to legal `TargetBand` outputs. |
| `common/breed/BreedFounder` | Rolls founders from only the listed allele pools, forces omitted coat and disorder loci to wild type/clear, and delegates DMRT3 to the appropriate trait-generation pathway. |
| `common/breed/BreedLineage` | Retains default pure/cross/Mixed outcomes; do not identify phenotype-matching Mountain Horse crosses as pure KMSH. |
| `common/genetics/SpliceOutcome` | Uses normal splice-carrot inheritance. A user-introduced pinto, magical trait, or disease allele should follow normal transmission rather than being silently removed. |
| `common/trait/StatAxis` | Uses speed, jump, health, and size. Add gait only if PHC’s trait system exposes it as an actual axis. |
| `common/trait/TargetBand` | Stores size and body-performance result bands used for KMSH founder generation. |
| `common/trait/BreedStatTargets` | Groups the KMSH body-stat targets under the breed record. |
| `common/breed/BandType` | Requires no `TRADITIONAL` or `BACHELOR` epigenetic band. The breed’s color variety comes from locus pooling, not shade forcing. |

## Verification

1. **Breed availability**
   - Confirm **Kentucky Mountain Saddle Horse** appears in the H menu’s Breeds tab and the breed book.
   - Verify the natural type, source checklist, `UNCOMMON` classification, 620 cowboy price, Appalachian-style biomes, display name, notes, and `kentucky_mountain_saddle_horse` save ID.

2. **Wild herd labels**
   - Spawn packs in eligible forest, birch forest, dark forest, meadow, grove, windswept forest, or windswept-hills biomes.
   - Verify that every horse in one generated pack identifies as **Kentucky Mountain Saddle Horse**.
   - Generate a lone ordinary wild horse outside a breed pack and verify it reads **Feral Mixed**, not KMSH.

3. **Coat pool**
   - Generate at least 200 founders and inspect phenotypes and genomes.
   - Confirm a wide **solid-color** palette: bay, black, chestnut, brown, palomino, buckskin, dun, gray, roan, and occasional silver/flaxen combinations should occur.
   - Confirm that most founders retain modest or absent white markings, according to how PHC separates ordinary marks from white-pattern loci.
   - Confirm tobiano, frame, sabino, splash, W-series dominant white, leopard complex, PATN patterns, rabicano, brindle, pearl, mushroom, and magical traits never occur in a pure founder.
   - Confirm any pinto foal appears only after outcrossing or deliberate gene insertion and is labeled as a cross or Mixed, never a pure KMSH.

4. **Gait genetics**
   - If PHC supports DMRT3, inspect a large founder sample and verify the `A` gait-keeper allele appears at approximately 95% frequency.
   - Confirm most founders are `A/A` or `C/A`, with rare `C/C` non-gaited founders if retaining the proposed probabilistic pool.
   - If the implementation models alternate gait as a phenotype, test that DMRT3-positive horses receive the available smooth-gait/single-foot behavior without being forced into pace or an unrelated breed-specific gait.
   - If PHC has no gait system, confirm the DMRT3 field is omitted or stored safely without being misinterpreted as a coat locus.

5. **Disorders and outcross inheritance**
   - Verify each named disorder locus is clear in generated pure founders.
   - Breed pure KMSHs for multiple generations and confirm no disease allele emerges spontaneously.
   - Introduce a test disease, pinto, or magical allele through an outcross or splice mechanic and verify ordinary Mendelian transmission continues in descendants.

6. **Body statistics**
   - Compare a KMSH sample with a baseline riding horse, Rocky Mountain Horse, small pony breed, and specialist sport horse.
   - Confirm KMSHs range from compact pony-scale mounts to ordinary small saddle horses, show good hardiness, have useful forward movement, and retain baseline jump specialization.
   - Confirm that smooth gait does not incorrectly raise their raw speed beyond specialist racing breeds.

7. **Lineage**
   - Kentucky Mountain Saddle Horse × Kentucky Mountain Saddle Horse → **Kentucky Mountain Saddle Horse**.
   - Kentucky Mountain Saddle Horse × another pure breed → **Kentucky Mountain Saddle Horse cross**.
   - Kentucky Mountain Saddle Horse cross × pure Kentucky Mountain Saddle Horse → the same named cross.
   - Kentucky Mountain Saddle Horse cross × a different named cross → **Mixed**.
   - Kentucky Mountain Saddle Horse or its cross × Feral Mixed → **Mixed**.
   - Confirm the label appears consistently on foal creation, H-menu inspection, breed-book lookups, entity save/load, and any stable or breeder interface.

## Sources

- [OMIA — DMRT3-related gaitedness in domestic horses](https://omia.org/OMIA001715/9796/): authoritative gene/variant resource identifying the DMRT3 Ser301STOP gait-keeper variant and listing the Kentucky Mountain Saddle Horse among affected gaited breeds. [omia](https://omia.org/OMIA001715/9796/)
- [Kentucky Mountain Saddle Horse overview](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse): accessible summary of registration height classes, solid-color rules, white-marking restrictions, Spotted Mountain Horse Association distinction, Appalachian origin, and natural single-foot gait. This should be cross-checked against live KMSHA rulebook material before release because it is a secondary summary. [en.wikipedia](https://en.wikipedia.org/wiki/Kentucky_Mountain_Saddle_Horse)
- [University of Florida IFAS — Genetic Selection for Gaits in the Horse](https://ask.ifas.ufl.edu/publication/AN332): current academic extension explanation of the DMRT3 gait mutation and the availability of genetic testing. [ask.ifas.ufl](https://ask.ifas.ufl.edu/publication/AN332)
- [Oasis Veterinary Hospital — Kentucky Mountain Saddle Horse profile](https://www.oasisvets.com/services/equine/breeds/kentucky-mountain-saddle-horse): supplementary description of Appalachian origin, typical build, practical uses, height range, colors, and smooth four-beat gait. [oasisvets](https://www.oasisvets.com/services/equine/breeds/kentucky-mountain-saddle-horse)
- [Bonnie View Farms — Mountain Horse gait explanation](http://www.bonnieviewfarms.ca/aboutmountainhorses.html): descriptive source for the sequence and naturally occurring four-beat lateral Mountain Horse gait; use as supplementary rather than registry authority. [bonnieviewfarms](http://www.bonnieviewfarms.ca/aboutmountainhorses.html)
- [Equine Clinical Genomics: A Clinician’s Primer](https://pmc.ncbi.nlm.nih.gov/articles/PMC3297474/): source for the general GYS1/PSSM1 disease context and the rationale for not assigning an unsupported KMSH-specific disorder rate. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3297474/)