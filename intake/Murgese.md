The **Murgese** should be one of the mod’s most visually unified natural breeds: a substantial southern Italian baroque riding and work horse that is almost always solid black, with a very small, authentic **blue roan / iron-gray** minority. Pure founders should be fixed for black pigment and recessive agouti—producing true black rather than bay—and should carry a low `Rn` frequency only if the mod’s roan locus correctly produces a viable black roan. The breed’s real history, small effective population, and narrow approved palette make it a strong candidate for a tight founder gene pool. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

## Identity & flavour

The **Murgese**, Italian *Cavallo Murgese*, is the native black horse of the **Murge plateau** in Apulia/Puglia, southern Italy. Its ancestors developed under Spanish rule in the fifteenth and sixteenth centuries, when local horses were crossed with imported Oriental, Barb, and Iberian/Andalusian-type stock. The Counts of Conversano played an important role in early selective breeding, but the modern breed was formally rebuilt and registered in 1926 at the Foggia stallion depot, later the Istituto di Incremento Ippico for Puglia. The current Italian breed organization is **ANAMF**, the National Association of Breeders of the Martina Franca Donkey and Murgese Horse. [masseriacapoiazzo](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/)

The Murgese was bred as a versatile horse for the limestone country of Apulia: farm work, military use, light draught, carriage work, ranch and cattle work, forestry, trekking, and riding. It needed to tolerate hot, dry summers and comparatively harsh winters on the Murge, travel over stony ground, and remain strong and tractable with practical management. In the twentieth century, selection moved gradually toward a lighter, more rideable type, but a Murgese should still feel more substantial and grounded than a refined modern warmblood. It is a baroque utility horse: muscular, compact, and calm, with enough presence for ceremonial carriage work and enough sense for a long trail. [masseriacapoiazzo](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/)

Murgese stallions usually stand about **155–168 cm**—roughly **15.1–16.2 hands**—while mares stand about **150–162 cm**, about **14.3–16 hands**. The horse has a medium-length head, broad forehead, large expressive eyes, wide mobile nostrils, and a flat to slightly ram-like profile. Its neck is muscular and well carried, the body broad and deep, the back short-to-medium and strong, the croup powerful, the limbs robust, and the hooves hard. The mane is characteristically abundant, and the tail is thick and full; there is no heavy draft feathering. A mature Murgese should read as a large, dark, strong Italian horse with a traditional baroque outline—not a modern narrow Thoroughbred cross. [masseriacapoiazzo](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/)

The color rule is unusually strict. The official palette is **black** and **blue roan**, also described as dark iron gray in older or non-genetic language. The vast majority are solid raven black with no particular markings; one standard description places blue roan/iron-gray at about **2%** of the population. In genetics terms, a true black Murgese is normally `E_ aa`, while a blue roan Murgese is a black horse with the dominant `Rn` roan allele. Blue roan is not progressive gray: it retains a dark head and dark lower legs while white hairs mix through the body coat. The breed should not generate chestnut, bay, gray, dun, silver, cream, champagne, pinto, leopard, or flashy white patterns from pure founders. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

Murgese horses are known for calm courage, intelligence, reliability, strength, and rusticity. Their heritage includes the influence of three famous foundation stallions—**Granduca di Martina**, **Araldo delle Murge**, and **Nerone**—whose lines remain part of the breed’s modern history. Pedigree research shows that the breed has suffered meaningful genetic narrowing: a relatively small number of ancestors explain a large share of present diversity, so a player’s Murgese breeding project should feel precious and concentrated rather than endlessly variable. In Procedural Horse Genetics, the goal is instantly recognizable: a nearly black, abundant-maned, powerful Apulian trail and carriage horse, with the rare blue roan foal as a special but authentic surprise. The mod does not model mane abundance, baroque conformation, hard hooves, climate tolerance, pulling force, cattle work, regional Italian management, founder-line pedigree, inbreeding coefficients, or the specific influence of the three foundation stallions. [mdpi](https://www.mdpi.com/1424-2818/14/6/422)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This JSON follows the readable format used in your example. Before compiling, reconcile exact field names, actual `MC1R`/Extension and `ASIP`/Agouti locus symbols, the roan allele name, source enum values, rarity enum values, price units, and stat-band syntax with `common/breed/spec/`.
>
> **Scientific-frequency note:** The modern Murgese approved-color requirement—black or blue roan—is directly supported by Italian and ANAMF-derived standards. A source describing the standard estimates blue roan/iron-gray at approximately **2%**. Under a Hardy–Weinberg approximation, a 2% roan phenotype frequency corresponds to an `Rn` allele frequency near **1.0%** if roan is fully penetrant and almost all carriers are heterozygous. Because actual studbook breeding is nonrandom, use `Rn: 0.01` as a restrained founder-pool approximation, not a claimed population-genetic census. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

```json
{
  "id": "murgese",
  "name": "Murgese",
  "type": "natural",
  "notes": "The Murgese is a southern Italian Apulian baroque riding, light-draught, carriage, farm, trekking, and working horse defined by ANAMF pedigree, a black or blue-roan coat standard, abundant mane and tail, a muscular compact frame, hard hooves, rusticity, calm temperament, and the historic Granduca di Martina, Araldo delle Murge, and Nerone foundation lines. Procedural Horse Genetics does not model mane abundance, exact ram-profile head, broad forehead, heavy muscle, hoof hardness, limestone-terrain sure-footedness, carriage action, traction, cattle work, regional Italian management, pedigree bottlenecks, inbreeding, foundation-line identity, registry inspection, or individual tractability.",

  "biomes": [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:windswept_hills",
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
  "price": 920,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 1.0
    },
    "agouti": {
      "a": 1.0
    },

    "roan": {
      "N": 0.99,
      "Rn": 0.01
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
    "jump": 5,
    "health": 9,
    "size": [
      1.03,
      1.15
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Murgese × Murgese produces Murgese. Murgese × another pure breed produces a Murgese cross. A Murgese cross bred back to pure Murgese remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not assign the Murgese label to every black or blue-roan horse: real Murgese identity depends on Italian breed lineage and ANAMF registration, not its extremely recognizable coat alone."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Visual identity | Nearly fixed black with rare blue roan | Murgese is a deliberately color-restricted Italian breed. ANAMF-derived standards permit black and blue roan/iron gray; source descriptions identify most horses as black and put blue roan around 2%.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/) |
| Extension | `E: 1.0` | All valid black and blue-roan Murgese phenotypes require black pigment. Fixing `E` prevents chestnut/sorrel founders.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/) |
| Agouti | `a: 1.0` | Recessive agouti allows black pigment across the body, producing black rather than bay. Fixing `a` prevents bay/brown founders and keeps the population faithful to the black/blue-roan standard.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11200706/) |
| Roan | `Rn: 0.01` | Blue roan is a black base plus roan. A 1% allele frequency gives roughly 2% roan phenotype under random mating, matching a source’s “about 2%” blue-roan/iron-gray statement. This is an intentionally simplified Hardy–Weinberg conversion.  [hilarispublisher](https://www.hilarispublisher.com/open-access/equine-color-genetics-and-deoxyribonucleic-acid-testing-2157-7579.1000134.pdf) |
| Blue roan versus gray | Roan allowed; `G` forced wild type | Blue roan retains a dark head and legs while the body mixes colored and white hairs. It does not progressively whiten like genetic gray. “Iron gray” in historical breed language must not be implemented with the `STX17` gray allele.  [hilarispublisher](https://www.hilarispublisher.com/open-access/equine-color-genetics-and-deoxyribonucleic-acid-testing-2157-7579.1000134.pdf) |
| Other dilution loci | Cream, pearl, champagne, silver, mushroom, dun, flaxen forced wild type | None belongs to the permitted black/blue-roan founder standard. The abundant mane is a structural trait, not flaxen or silver dilution.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/) |
| White patterns | Tobiano, sabino, frame, splash, KIT white, rabicano forced wild type | The principal standard calls for raven black without special markings; no pinto or major white-pattern genetics belongs in a pure Murgese founder pool.  [masseriacapoiazzo](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Leopard spotting is incompatible with the tightly black/blue-roan breed presentation. |
| Magical loci | All forced wild type | The Murgese’s dark dramatic appearance is natural pigment genetics and morphology. |
| Disorders | All listed loci clear | No credible Murgese-specific carrier-frequency data was found for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Do not invent frequencies from Italian, Arabian, draft, or warmblood ancestry. |
| Speed | `5/10` | A versatile riding and working horse: capable across country but not selected for race-speed specialization.  [en.wikipedia](https://en.wikipedia.org/wiki/Murgese) |
| Jump | `5/10` | Modern riding selection and baroque athleticism support a practical baseline-to-good score, but not elite specialist-jumper status. |
| Health | `9/10` | Represents strong constitution, rusticity, hard hooves, climate resilience, and general working soundness—not immunity from disease or the consequences of a managed small population.  [mdpi](https://www.mdpi.com/1424-2818/14/6/422) |
| Size | `×1.03–1.15` | Captures mares roughly 150–162 cm and stallions around 155–168 cm, making the breed medium-large without entering giant-draft territory.  [en.wikipedia](https://en.wikipedia.org/wiki/Murgese) |

## Disorder approach

Every named disorder locus remains **clear** in the pure Murgese founder pool.

This is the scientifically conservative choice. The Murgese has valuable published pedigree and demographic research showing a meaningful bottleneck and limited effective population size, but that is not the same thing as a demonstrated frequency for any one of the mod’s named Mendelian disorders. The population study reported an average pedigree inbreeding coefficient of about **5.22%**, effective population size around **47.46**, and substantial founder/ancestor concentration—important reasons for real-world genetic monitoring, but not evidence for adding a particular disease mutation. [mdpi](https://www.mdpi.com/1424-2818/14/6/422)

Therefore, the file should not seed:

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

The rare `Rn` allele is **not** a disease locus. Blue roan Murgese horses are viable, and ordinary roan should not be modeled with the obsolete “lethal roan” myth. [hilarispublisher](https://www.hilarispublisher.com/open-access/equine-color-genetics-and-deoxyribonucleic-acid-testing-2157-7579.1000134.pdf)

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `murgese` as a natural Italian breed record with Puglia/Murge flavour, rare commonness, managed sources, nearly uniform black founder pool, rare blue-roan genetics, clear disease pool, and robust medium-large stats. |
| `common/breed/Breeds` | Register `murgese` for stable/cowboy acquisition, spawn eggs, H-menu display, breed books, commands, genome serialization, and lineage labels. |
| `common/breed/BreedSource` | Validate `cowboy`, `spawn_egg`, and `stable`. `wild` is intentionally omitted because Murgese is a managed Italian registry breed, not a natural feral population. |
| `common/breed/BreedBands` | Support the empty epigenetic-band object. The breed already has genetically fixed black identity; do not fake blackness through an unsupported shade band. |
| `common/breed/spec/` | Confirm the actual `extension`, `agouti`, and `roan` locus IDs and allele labels. Ensure `E` and `a` can be fixed, and ensure the engine’s roan allele represents normal viable roan, not a deprecated lethal model. |
| `common/breed/Commonness` | Confirm `RARE` maps to the intended rarity tier and that `spawn_weight: 1.5` is valid if the game uses direct numerical weights. |
| `common/breed/BreedStatCurve` | Convert speed 5, jump 5, health 9, and size ×1.03–1.15 into valid `TargetBand` targets. |
| `common/breed/BreedFounder` | Roll fixed `E` and `a`, roll low-frequency `Rn`, force every other coat and disease locus to wild type/clear, then apply the robust Apulian baroque work-horse stat targets. |
| `common/breed/BreedLineage` | Apply the default pure/cross/Mixed behavior. A black or blue-roan horse from another breed must not gain Murgese lineage from visual phenotype alone. |
| `common/genetics/SpliceOutcome` | No Murgese-specific exception. A splice-carrot allele can pass normally and create a nonstandard descendant with bay, chestnut, dilution, pinto, leopard, or disease genetics excluded from pure founders. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Define and serialize medium-large size, practical riding speed/jump, and high-heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` epigenetic band is needed unless the concrete serializer requires a default. |

## Verification

1. Confirm **Murgese** appears in the H-menu’s Breeds tab and breed book with the correct Puglia/Murge origin, Italian registry context, rare commonness, managed-only sources, black/blue-roan coat rule, and baroque utility-horse flavour.

2. Confirm Murgese does **not** spawn as an ordinary wild pack because `wild` is omitted. It should be available from cowboy, stable, and spawn-egg sources only.

3. Confirm a lone ordinary wild horse reads **Feral Mixed**, even if it is black, blue roan, abundant-maned, or in a warm dry biome.

4. Generate at least 2,000 pure Murgese founders. The population should be almost entirely solid black, with approximately 1–3% blue-roan outcomes over a sufficiently large sample. Small samples may easily produce no roans because the allele is intentionally rare.

5. Inspect genomes:
- Every founder must carry black-capable Extension `E`.
- Every founder must carry recessive Agouti `a`.
- Only `Rn` may vary within the coat pool.
- `G`, cream, pearl, champagne, silver, mushroom, dun, flaxen, every pinto/white pattern, leopard complex, PATN modifiers, brindle, magical loci, and all disorder loci must remain wild type or clear.

6. Confirm correct phenotype biology:
- `E_ aa N/N` founders render solid black.
- `E_ aa Rn/N` founders render blue roan, with white hairs through the body but dark head and lower legs.
- A blue-roan founder must not progressively turn white with age; if it does, the renderer has incorrectly used gray rather than roan.
- No chestnut or bay pure founder should appear.

7. Test roan inheritance:
- `Rn/N × N/N` should transmit `Rn` to approximately half the foals.
- `Rn/N × Rn/N` should create the normal Mendelian range, including `Rn/Rn` offspring if the mod permits it.
- `Rn/Rn` must remain viable unless the mod intentionally implements a modern, evidence-supported alternative model.

8. Confirm every named disorder locus remains clear across a large founder sample. The Murgese’s documented pedigree bottleneck must not accidentally cause unrelated disease loci to roll.

9. Verify mature stats. Murgese horses should trend medium-large, powerful, durable, and versatile, with ordinary-to-good riding speed and jump. They should not surpass dedicated sprint breeds in speed, elite warmbloods in jumping, or giant draught horses in body size.

10. Test default lineage:
- Murgese × Murgese → Murgese.
- Murgese × Lipizzaner → Murgese cross.
- Murgese × Friesian → Murgese cross.
- Murgese cross × pure Murgese → the existing Murgese cross label.
- Two different cross labels → Mixed.
- Any pairing involving Feral Mixed → Mixed.

## Sources

- [Exploring genetic diversity in five native horse breeds](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/): peer-reviewed Italian native-breed study; states that only black and gray/blue-roan colors are allowed in Murgese breeding and provides broader conservation context. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC12082900/)

- [Genetic Variability within the Murgese Horse Breed Inferred from Genealogical Data and Morphometric Measurements](https://www.mdpi.com/1424-2818/14/6/422): peer-reviewed pedigree, bottleneck, inbreeding, founder/ancestor, effective-population-size, and baroque black/blue-roan context. [mdpi](https://www.mdpi.com/1424-2818/14/6/422)

- [Italian breeds of horses: The Murgese](https://eng.agraria.org/horse/murgese.htm): Italian breed history, Puglia/Murge origin, Spanish-rule and imported-stock background, 1926 registration, height, weight, and use context. [eng.agraria](https://eng.agraria.org/horse/murgese.htm)

- [Murgese Horse Breed Standard](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/): ANAMF-derived practical standard for raven-black majority, approximately 2% blue roan/iron gray, foundation stallions, conformation, heat/cold rusticity, and height range. [masseriacapoiazzo](https://masseriacapoiazzo.it/en/murgese-horse-breed-standard/)

- [North American Murgese Association — Breed Standard](https://www.namamurgese.com/breed-standard): ANAMF-derived North American presentation of the black/blue-roan permitted coat standard, head profile, muscular neck, abundant mane, and desired type. [namamurgese](https://www.namamurgese.com/breed-standard)

- [Equine coat-color genetics overview](https://pmc.ncbi.nlm.nih.gov/articles/PMC11200706/): peer-reviewed source on `MC1R`/Extension, `ASIP`/Agouti, and roan/KIT-associated coat genetics, used to distinguish black from black roan. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC11200706/)