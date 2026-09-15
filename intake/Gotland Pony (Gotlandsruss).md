The **Gotland Pony**, properly **Gotlandsruss** and often simply *Russ*, should play as a compact Swedish landrace pony: dun-heavy, hardy, quick-minded, strong-footed, and made for Baltic woodland rather than polished show-ring uniformity. Its defining gameplay silhouette is a small, substantial, mostly solid-colored pony with an unusually strong pull toward dun—especially bay dun—plus genuine usefulness in driving, riding, jumping, and pony trotting. The proposed gene frequencies below are deliberately transparent *gameplay estimates*: the registry describes permitted colors, not a population-wide allele survey. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

## Identity & flavour

The **Gotland Pony**—Swedish **Gotlandsruss**, usually shortened to **Russ**—is Sweden’s only native pony breed, born on the Baltic island of Gotland. Older names include *Skogruss*, “little horse of the woods,” and *Skogsbaggar*, “forest rams.” It is not a recent designer pony with a single founding decade: it is an old island landrace shaped over centuries by semi-feral life, practical work, and later controlled conservation breeding. Ponies of the type are associated with Gotland’s long human history, while the recognizable modern registry population coalesced through rescue breeding from the late nineteenth century onward. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

Gotlands lived loose in the island’s woods and rough grazing when they were not needed, then worked as small draft, transport, agricultural, and driving horses. Their home is wind-exposed Baltic country: mixed forest, heath, thin soils, seasonal forage, and cold wet winters rather than lush permanent pasture. That background made a pony that wastes little, keeps moving, and puts much of its strength into sturdy limbs and hard feet. Today it is a family riding pony, driving pony, jumper, and—most famously in Sweden—an exceptionally capable pony trotter. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

A typical Russ stands about **115–130 cm**, roughly **11.1–12.3 hands**, with some breed references expressing the practical range as about 11.2–13 hands. It is light and active rather than cob-heavy: a deep chest, long sloping shoulder, marked withers, comparatively long back, sloping croup, good joints, strong legs, and famously sound, hard hooves. The mane and tail are ordinary, practical pony hair rather than the great curtain of a Friesian or the upright crest of a Fjord. It should have little to no dramatic feathering. In mod terms, a Gotland should read at a glance as a low, clean-legged, well-made primitive riding pony—not as a miniature draft animal. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

The palette is broad by native-pony standards, but the strongest visual impression should be **dun**, bay, chestnut, or black. Dun and bay are repeatedly identified as predominant. Sweden’s conservation description excludes gray and tobiano, and excludes **homozygous cream**; this is an important distinction from banning cream entirely. A cream allele can occur historically in the population, but breeding policies prevent the double-cream “yellow” result from being accepted. For a Mendelian mod that does not model registry inspection, low-frequency cream is more faithful than forcing cream to zero, while gray and tobiano should be absent from pure founders. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

The Russ is generally gentle, intelligent, co-operative, hardy, and lively rather than dull. It has enough athleticism to make an honest child’s mount, driving pony, or compact jumper, and enough endurance to feel at home in long woodland travel. Its special sporting quirk is trotting: the breed society calls out exceptional pony-trotting talent. However, it is **not** a gaited breed in the tölt/pace sense: a published sample of 28 Gotland ponies had a zero frequency of the DMRT3 gait-keeper mutation. Players should therefore recognize the breed through compact stamina, responsive trot, tough feet, and dominant-looking dun markings—not through a magical special gait. [gotlandsruss](https://www.gotlandsruss.se/svraf/membership-application/)

The breed nearly vanished when nineteenth-century development reduced free range and demand shifted toward larger working horses. The 1859 legislation is identified as a major population blow; organized stud work began in the 1880s, and by 1922 only seven mares, one young stallion, and six young horses remained in one core remnant. Welsh Pony and Welsh Mountain stallions were introduced in 1951 and 1957 to address inbreeding, and their lines occur in much of the modern population—but no further outside introductions have occurred since. A managed semi-feral herd remains at Lojsta Hed on Gotland. The breed has also reached North America, though its U.S. population underwent its own near-collapse in the 1980s. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

For *Procedural Horse Genetics*, the goal is a pony that rewards selecting for classic **dun primitive markings**, especially bay dun, alongside strong health, modest size, and nimble all-round ability. Pure Gotland founders should never spontaneously throw gray, tobiano, roan, leopard spotting, champagne, silver, or magical effects; players can introduce those only through crosses or genetics tools. The mod does **not** model the specific head profile, mane texture, hard-hoof quality, feather amount, winter coat, easy-keeper metabolism, pasture thrift, trainability, trotting technique, behavior in a semi-feral herd, Swedish registration inspection, or the rule disqualifying homozygous cream animals.

## Breed JSON

> **Schema note:** The supplied PHC wiki page could not be fetched by the browser, so the JSON below uses the field structure and readable locus naming from your provided Anglo-Arabian example. Before compiling, map the locus IDs and enum capitalization exactly to the project’s live definitions in `common/breed/spec/`, `Breeds`, and the gene registry. In particular, verify whether the mod uses `cream`, `MATP`, `dun`, `TBX3`, or allele-level IDs such as `D`, `nd1`, and `nd2`.

```json
{
  "id": "gotland_pony",
  "name": "Gotland Pony (Gotlandsruss)",
  "type": "natural",
  "notes": "Procedural Horse Genetics models this breed's inherited coat pool, small size, athletic tendency, and hardiness, but not the Gotlandsruss head type, body proportions, mane and tail texture, scant feathering, unusually tough hooves, seasonal coat, easy-keeper metabolism, behavior in semi-feral woodland herds, trotting technique, Swedish studbook inspection, or the real-world registration restriction against homozygous cream animals.",

  "biomes": [
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:old_growth_birch_forest",
    "minecraft:meadow",
    "minecraft:windswept_hills",
    "minecraft:taiga",
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
  "price": 460,
  "commonness": "UNCOMMON",

  "coat_genes": {
    "extension": {
      "E": 0.80,
      "e": 0.20
    },
    "agouti": {
      "A": 0.82,
      "a": 0.18
    },

    "dun": {
      "D": 0.58,
      "nd1": 0.12,
      "nd2": 0.30
    },

    "cream": {
      "N": 0.94,
      "Cr": 0.06
    },
    "flaxen": {
      "N": 0.82,
      "f": 0.18
    },

    "grey": {
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
    "pearl": {
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
    "speed": 6,
    "jump": 6,
    "health": 9,
    "size": [
      0.70,
      0.80
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Gotland Pony × Gotland Pony produces Gotland Pony. Gotland Pony × any other pure breed produces a Gotland Pony cross. A Gotland Pony cross bred back to a pure Gotland Pony remains that cross under the default system; two different crosses and any cross involving Feral Mixed resolve to Mixed. No pedigree-percentage exception is required."
  }
}
```

## Genetics rationale

| Feature | Proposed implementation | Reasoning |
|---|---:|---|
| Base colors | `E` 0.80 / `e` 0.20; `A` 0.82 / `a` 0.18 | This produces a population centered on bay and bay dun, with black, chestnut, red dun, and grullo still possible. The real sources consistently identify dun, bay, chestnut, and black among typical Gotland colors, with dun and bay predominant. These are gameplay probabilities rather than measured allele frequencies.  [en.wikipedia](https://en.wikipedia.org/wiki/Gotland_Russ) |
| Dun | `D` 0.58 | Dun is the signature color family for the breed, so it should be frequent enough that a newly found herd visibly reads as Gotlandsruss. Because dun is dominant, this allele frequency yields a visibly dun-majority founder population without making every pony identical. Dun causes dilution plus primitive markings when present.  [livestockconservancy](https://livestockconservancy.org/gotland-horse/) |
| Non-dun primitive marking | `nd1` 0.12 | Retaining a small `nd1` pool gives some non-dun founders primitive-marking potential, a useful landrace texture if PHC distinguishes `nd1` from fully non-dun `nd2`. If the mod has only a two-allele dun locus, fold `nd1` into normal non-dun.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse) |
| Cream | `Cr` 0.06 | NordGen explicitly excludes *homozygous* cream, not every cream carrier. A low `Cr` rate preserves rare palomino/buckskin/smoky black possibilities while making double-cream offspring very unusual. Because the mod does not enforce registry rejection, this is the least misleading implementation.  [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/) |
| Flaxen | `f` 0.18 | This is a cautious aesthetic possibility for chestnut founders, not a documented population estimate. It adds occasional traditional-looking lighter mane/tail chestnuts while keeping the population visually anchored in dun and bay. If PHC treats flaxen as unvalidated or does not express it on this breed’s models, set this locus to `N: 1.0`. |
| Gray and tobiano | Forced wild type | Both NordGen’s conservation profile and breed summaries exclude gray and tobiano/piebald animals from the accepted breeding population. Gray and tobiano should therefore not arise among pure generated founders.  [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/) |
| Other dilutions/patterns | Forced wild type | There is no strong breed-specific evidence to populate champagne, silver, pearl, mushroom, leopard complex, roan, frame, splash, W-series white, rabicano, or brindle. Forcing them clear gives Gotland a coherent native-landrace palette and lets players create unusual colors through outcrossing. |
| Disorders | All named loci clear | No reliable, breed-specific carrier-frequency dataset was located for Gotlandsruss for the disorders listed in the prompt. It would be inappropriate to import Arabian, Quarter Horse, Friesian, or Warmblood rates into this small Swedish pony population. In particular, PSSM1 is a dominant GYS1 disorder most routinely targeted in Quarter Horse-related populations, not an evidenced Gotlandsruss founder frequency.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/pssm1) |
| Speed | 6/10 | The Russ is athletic and notably good at pony trotting, but it is not a purpose-bred galloping racehorse. A modestly above-baseline speed score suits a quick, compact harness and riding pony.  [gotlandsruss](https://www.gotlandsruss.se/svraf/membership-application/) |
| Jump | 6/10 | Breed references describe Gotlands as athletic and capable jumpers. Score 6 makes this an identifiable strength without competing mechanically with specialist sport-horse breeds.  [livestockconservancy](https://livestockconservancy.org/gotland-horse/) |
| Health | 9/10 | This represents hardiness, endurance, efficient use of rough forage, and sound feet—not a claim that individual ponies are free of ordinary veterinary needs or all genetic risks. NordGen specifically highlights endurance and robustness as valuable Gotlandsruss characteristics.  [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/) |
| Size | ×0.70–0.80 | This targets a true pony stature consistent with the usual 115–130 cm / roughly 11.1–12.3 hh range. Calibrate against PHC’s baseline-to-hands curve; if its ordinary horse median is about 15 hh, ×0.76 corresponds to roughly 11.4 hh.  [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/) |
| Gait | No alternate-gait locus | A published Gotland Pony sample had zero copies of the DMRT3 gait-keeper mutation. The breed’s trotting reputation should therefore be expressed through speed, health, behavior, or a future harness aptitude trait—not a gaited-horse allele.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3523687/) |

### Cream-policy caveat

The one unusual implementation choice is cream. The conservation description says the breed accepts all colors **except homozygous yellow/homozygous cream, gray, and tobiano**. That is not identical to “cream is absent.” The JSON therefore assigns `Cr` a low but nonzero frequency, which permits rare single-cream Gotlands while preventing a major cream population. Pure-breed `Cr/Cr` foals can occur in-game, but should be understood as genetically possible offspring that would fail the real Swedish breeding color rule rather than as a standard registered Gotlandsruss. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

If you want the strictest possible **studbook-phenotype** implementation instead of a biologically permissive Mendelian population, change this block:

```json
"cream": {
  "N": 0.94,
  "Cr": 0.06
}
```

to:

```json
"cream": {
  "N": 1.0
}
```

That eliminates all cream-derived colors in pure founders and pure-bred foals, but it does flatten a nuance of the published breed rule.

## Spawning and lineage

The recommended spawning choice is forest-forward rather than plains-forward: the living cultural image of the breed is the Lojsta Hed woodland herd on Gotland, and the breed’s long semi-feral history is tied to wooded range and rough Baltic grazing. Meadows, windswept hills, taiga, groves, and some plains retain practical Minecraft variety without making Gotlands feel like a desert or tropical pony. The official breed society reports that a herd of approximately 50 mares and foals is still maintained at Lojsta Hed. [gotlandsruss](https://www.gotlandsruss.se/svraf/membership-application/)

`UNCOMMON` with a numerical spawn weight of `3` makes the breed a rewarding but not vanishingly rare find. That feels more appropriate than “rare” because this is a genuine surviving native breed with thousands globally, while still preserving the specialness of finding a real Gotland-like woodland herd instead of another generic plains horse. NordGen recorded about 2,500 mares in Sweden in 2020, while other conservation sources estimate roughly 6,000–8,000 animals worldwide. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)

There should be **no special lineage override**. The default breed table communicates a crucial distinction: an official Gotlandsruss is a documented population, whereas its foals from an outcross are a Gotland cross, not automatically a Gotland Pony simply because they look dun and compact.

## Code map

| Location | What this breed uses or changes |
|---|---|
| `common/breed/Breed` | Adds the natural `gotland_pony` record: display name, notes, biome pool, spawn weight, sources, price, and commonness. |
| `common/breed/Breeds` | Registers `gotland_pony` so the ID resolves in spawning, saved genomes, commands, the breed book, menus, and lineage display. |
| `common/breed/BreedSource` | Validates the four selected sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Serializes the empty epigenetic band set. No color-shade bands are needed; the strong visual identity comes from allele pooling rather than forced darkness/lightness. |
| `common/breed/spec/` | Defines and parses the JSON format. Confirm field names, source enum serialization, allele/locus names, `type` values, arrays versus objects, and defaulting behavior in both serialization directions. |
| `common/breed/Commonness` | Confirms that `UNCOMMON` corresponds to the desired rarity ladder and that `spawn_weight: 3` is either legal metadata or correctly derived from the enum. |
| `common/breed/BreedStatCurve` | Converts scores 6 speed, 6 jump, 9 health, and size ×0.70–0.80 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Rolls each founder genome from this Gotland-only allele pool; every omitted coat locus must default to wild type and every omitted disorder locus to clear. |
| `common/breed/BreedLineage` | Applies standard pure/cross/mixed naming. No Gotland-specific exception is needed. |
| `common/genetics/SpliceOutcome` | Requires no custom behavior. A splice-carrot allele should reach the foal only under ordinary splice-transmission rules. |
| `common/trait/StatAxis` | Uses the existing speed, jump, health, and size axes. |
| `common/trait/TargetBand` | Holds the allowed founder result bands derived from scores and size. |
| `common/trait/BreedStatTargets` | Groups the four Gotland targets as the breed’s physical-performance profile. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is proposed. Use the project’s normal/default handling for an empty epigenetic-band map. |

## Verification

1. **Registry and UI**
   - Open the H menu’s Breeds tab and confirm **Gotland Pony (Gotlandsruss)** appears with its natural type, price, `UNCOMMON` status, sources, biome list, and notes.
   - Confirm the same name and description display correctly in the breed book.
   - Confirm the serialized ID remains `gotland_pony` after saving, reloading, and respawning a test world.

2. **Wild pack identity**
   - Spawn or locate a herd in an eligible forest, meadow, grove, taiga, or windswept-hills biome.
   - Verify every horse created in one pack identifies as **Gotland Pony (Gotlandsruss)**, even when their base colors differ.
   - Verify a lone, unassigned wild horse generated outside a breed pack identifies as **Feral Mixed**, not Gotland Pony.

3. **Coat distribution**
   - Generate at least 200 founders and record phenotype and genotype.
   - Most should be dun or visibly from the bay/chestnut/black family; bay dun should be a frequent sight, with red dun and grullo occurring in smaller numbers.
   - Confirm gray, tobiano, champagne, silver, pearl, mushroom, leopard complex, roan, frame overo, splash, W-series dominant white, rabicano, brindle, and magical effects never appear in a pure founder.
   - Confirm that a rare `Cr` carrier can arise only if the biologically permissive cream option is retained.
   - If two `Cr` carriers are bred, confirm the game can produce a `Cr/Cr` foal at the normal Mendelian 25% expectation, and document that this phenotype is intentionally outside real registry acceptance.

4. **Disorder audit**
   - Inspect a large founder sample and confirm every listed disorder locus is `N/N`.
   - Cross pure Gotlands for several generations and verify that no named disease allele arises spontaneously.
   - Introduce a disorder allele through an outcross or genetics mechanic and verify it follows the mod’s normal inheritance behavior; purity should constrain founder generation, not magically suppress a deliberately introduced allele forever.

5. **Stats**
   - Compare a founder sample with a baseline horse breed.
   - Gotlands should cluster well below average saddle-horse size, trend sturdy and healthy, and feel a little quicker and more capable over small jumps than a generic baseline pony.
   - They should not beat specialist Thoroughbreds in flat speed or high-end sport horses in jump score solely because of the health statistic.

6. **Lineage table**
   - Gotland Pony × Gotland Pony → **Gotland Pony**.
   - Gotland Pony × any other pure breed → **Gotland Pony cross**.
   - Gotland Pony cross × pure Gotland Pony → the same **Gotland Pony cross**, under the default cross-back rule.
   - Gotland Pony cross × a different named cross → **Mixed**.
   - Any Gotland Pony or Gotland cross × **Feral Mixed** → **Mixed**.
   - Verify these labels appear consistently in the H menu, breed book, genome inspection, saved entities, and foal creation events.

## Sources

- [NordGen — Gotland Pony / Gotlandsruss](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/): Swedish native name, 115–130 cm height, accepted-color rule, conservation governance, nineteenth- and twentieth-century rescue history, Welsh introductions, present conservation context, and robustness/endurance characterization. [nordgen](https://www.nordgen.org/our-work/farm-animals/nordic-native-breeds/gotland-pony/)
- [Svenska Russavelsföreningen / SvRaF — Official Gotland Pony breed society and studbook holder](https://www.gotlandsruss.se/svraf/membership-application/): official-society status, Lojsta Hed herd, global population statement, family/competition versatility, and pony-trotting aptitude. [gotlandsruss](https://www.gotlandsruss.se/svraf/membership-application/)
- [The Livestock Conservancy — Gotland Horse](https://livestockconservancy.org/gotland-horse/): North American conservation context, historical use, late-nineteenth-century decline, height, conformation, hard feet, temperament, jumping/trotting aptitude, and U.S. history. [livestockconservancy](https://livestockconservancy.org/gotland-horse/)
- [Andersson et al., “Mutations in DMRT3 Affect Locomotion in Horses and Spinal Circuit Function in Mice”](https://pmc.ncbi.nlm.nih.gov/articles/PMC3523687/): the Gotland Pony sample of 28 had DMRT3 gait-keeper mutant allele frequency 0.00; supports leaving alternate-gait genetics out of the pure breed definition. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC3523687/)
- [UC Davis Veterinary Genetics Laboratory — Dun dilution](https://vgl.ucdavis.edu/test/dun-horse): dominance and basic inheritance of dun and non-dun alleles, supporting the `D`-weighted coat design. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)
- [UC Davis Veterinary Genetics Laboratory — PSSM1](https://vgl.ucdavis.edu/test/pssm1): inheritance and breed-testing context for PSSM1, supporting the decision not to fabricate a Gotlandsruss disease rate without population evidence. [vgl.ucdavis](https://vgl.ucdavis.edu/test/pssm1)