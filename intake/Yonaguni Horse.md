The **Yonaguni Horse** should be a very rare, compact Japanese island horse with an unusually tight and scientifically defensible coat pool: almost entirely solid **bay**, with a minority of dark bay/brown and chestnut, no white markings, and a genuine dark dorsal stripe that should be modeled as a **non-dun primitive marking** if the mod distinguishes `nd1` from true dun. Its defining gameplay strengths are tiny size, strong hooves, island hardiness, quiet tractability, and sure-footed carrying ability—not high speed, jumping scope, or dramatic color. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

## Identity & flavour

The **Yonaguni Horse**, Japanese *Yonaguni-uma* (与那国馬) and locally called **Chimanma**, is an endangered native horse from Yonaguni Island in the Yaeyama Islands of Okinawa Prefecture, Japan. The island lies at Japan’s far western edge, close to Taiwan, between the East China Sea and the Philippine Sea. Yonaguni is one of Japan’s eight native horse breeds, but it is genetically and culturally distinct as an isolated island population. A 2016 study estimated about 130 horses on and off the island and found that, despite its small size, the population retained genetic diversity comparable to several other endangered breeds. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

The breed has existed on Yonaguni for centuries, though the exact arrival route and original foundation remain uncertain. It likely descends from the small horses historically brought to Japan through routes involving the Korean Peninsula, then adapted to island life. In the Ryukyu Kingdom era, Ryukyuan horses were used in regional trade and tribute relationships; later, Yonaguni horses became indispensable local farm, pack, and riding animals. They carried crops and supplies, worked fields, and moved people across steep, wet, rocky island country. Yonaguni Island’s remoteness helped the horses survive nineteenth-century Japanese policies that favored larger cavalry horses, but twentieth-century agricultural and transport mechanization later drove their numbers dangerously low. [oki-park](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855)

A Yonaguni is a small true horse, usually **110–120 cm** at the withers—roughly **10.3–11.3 hands**—though reported ranges extend slightly above or below that depending on the source and population. It has a large, plain head with a straight profile, small ears, a short thick or slender neck, low withers, a straight back, a sloping croup, narrow body, short sturdy limbs, and notably strong, hard hooves. The mane may be coarse, bushy, or short and upright; the tail can grow long but is often finer than it first appears. It should look like an old island utility horse: light, tough, compact, and entirely capable of carrying a rider or panniers despite its small frame. [oki-park](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855)

The real Yonaguni palette is narrow. Research based on the modern population says the horses are **mostly bay**, while local natural-monument material describes the common coat as reddish-brown bay with a dark stripe running from mane to tail. Supplementary breed descriptions list bay as dominant, dark bay/brown and chestnut as less common, and emphasize that the coat is solid with **no white markings**. That dorsal stripe needs a careful genetics distinction: a bay Yonaguni with dark mane, tail, lower legs, and a back stripe is not automatically a genetically true-dun horse. If the mod supports the `TBX3` non-dun-1 (`nd1`) allele, use a low-to-moderate `nd1` rate to permit primitive marking without the pale body dilution of `D`; otherwise, omit the stripe rather than falsely filling the pure population with yellow dun. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

Yonaguni horses are gentle, willing, intelligent, hardy, and unusually sure-footed. Today they are preserved as a town-protected natural monument and used in conservation, tourism, education, therapy, and carefully managed riding or swimming experiences. In Procedural Horse Genetics, a Yonaguni should be immediately recognizable: a very small solid bay island horse with dark points, a possible dark stripe, clean legs, no white, and an outsized health score. The mod does not model island swimming, hard-hoof quality, narrow-body conformation, mane texture, tourism training, rice or sugarcane hauling, regional Ryukyuan history, local preservation law, social behavior, exact primitive markings, or the population-management work needed to conserve an endangered island breed. [oki-park](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855)

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed wiki could not be retrieved by the documentation fetcher. This follows the readable JSON convention from the supplied example. Before compiling, reconcile key names, allele symbols, `D/nd1/nd2` support, source enums, commonness labels, price units, and stat-band serialization against `common/breed/spec/`.
>
> **Scientific-rate caveat:** The 2016 Yonaguni genetics paper establishes that the modern population is mostly bay, but it does not provide a published Extension/Agouti allele-frequency table. The `E/e` and `A/a` values below are therefore transparent gameplay approximations selected to make bay overwhelmingly common while leaving smaller dark-bay/brown, black, and chestnut minorities. All named disease loci are clear because no Yonaguni-specific carrier-rate evidence was located. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

```json
{
  "id": "yonaguni_horse",
  "name": "Yonaguni Horse",
  "type": "natural",
  "notes": "The Yonaguni Horse is an endangered Japanese island breed defined by its isolated Yonaguni Island population, tiny 110-120 cm size, compact island-work conformation, strong unshod hooves, solid mostly bay coat, absence of white markings, possible dark dorsal stripe, hardiness, gentle temperament, and historical use as a farm, pack, and riding horse. Procedural Horse Genetics does not model island swimming, hoof hardness, mane texture, narrow body, exact head profile, tourism or therapy training, crop hauling, Ryukyuan history, local natural-monument protection, population management, or the precise distinction between true dun dilution and non-dun primitive dorsal marking unless the engine exposes TBX3 nd1.",

  "biomes": [
    "minecraft:beach",
    "minecraft:stony_shore",
    "minecraft:warm_ocean",
    "minecraft:river",
    "minecraft:sparse_jungle",
    "minecraft:jungle",
    "minecraft:forest",
    "minecraft:meadow",
    "minecraft:windswept_hills"
  ],
  "spawn_weight": 0.75,
  "spawn_time": "day",
  "sources": [
    "wild",
    "spawn_egg",
    "stable"
  ],
  "price": 1080,
  "commonness": "VERY_RARE",

  "coat_genes": {
    "extension": {
      "E": 0.90,
      "e": 0.10
    },
    "agouti": {
      "A": 0.90,
      "a": 0.10
    },

    "dun": {
      "D": 0.0,
      "nd1": 0.35,
      "nd2": 0.65
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
    "grey": {
      "N": 1.0
    },
    "roan": {
      "N": 1.0
    },
    "rabicano": {
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
    "speed": 4,
    "jump": 3,
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
    "notes": "Use the default lineage table. Yonaguni Horse × Yonaguni Horse produces Yonaguni Horse. Yonaguni Horse × another pure breed produces a Yonaguni Horse cross. A Yonaguni Horse cross bred back to pure Yonaguni Horse remains that cross under the default system. Two different cross labels produce Mixed, and any pairing involving Feral Mixed produces Mixed. Do not identify a tiny bay island horse as Yonaguni from appearance alone; the real breed is a specific endangered Yonaguni Island population, not a generic Japanese pony phenotype."
  }
}
```

## Genetics rationale

| Feature | Implementation | Rationale |
|---|---:|---|
| Population identity | Very rare isolated Japanese island horse | Yonaguni is one of Japan’s native breeds and an endangered population restricted to Yonaguni Island and managed off-island conservation stock. About 130 were estimated in the cited genetic-diversity research.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Base-color focus | Strong bay bias | The primary genetics paper says Yonaguni horses are mostly bay; official local material likewise describes a reddish-brown bay body with a dark dorsal stripe.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Extension | `E: 0.90 / e: 0.10` | A high `E` frequency preserves black pigment for dark points and bay. `e` remains low but nonzero to allow the reported chestnut minority. This is a gameplay estimate, not direct genotyping.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Agouti | `A: 0.90 / a: 0.10` | A strong `A` bias makes black pigment largely point-restricted, generating the reported mostly bay population. Rare `a/a` horses can produce black rather than forcing every horse to bay. This is a gameplay estimate.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Dorsal stripe | `D: 0.0`, `nd1: 0.35`, `nd2: 0.65` | Local documentation reports a dark mane-to-tail stripe on otherwise bay horses. True `D` would lighten the body into bay dun; `nd1` can retain primitive markings without full dun dilution. Use this only if the mod models `D/nd1/nd2`; otherwise omit the stripe rather than introducing false dun.  [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse) |
| Gray | Forced wild type | No reliable Yonaguni source supports true progressive gray as a normal population phenotype.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Cream, champagne, silver, pearl, mushroom, flaxen | Forced wild type | These dilution/modifier families are not needed to model the narrow documented bay/dark-bay/chestnut pool. Reports that list unusual colors without population context are weaker than the population paper and local natural-monument source.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |
| Roan and rabicano | Forced wild type | The primary population description emphasizes a mostly bay herd; no reliable allele evidence supports adding roan-family modifiers to pure founders. |
| White pattern loci | All forced wild type | The breed is described as solid with no white markings. Tobiano, frame, Sabino 1, splash, KIT white, and leopard complex would break the immediate visual identity.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Yonaguni.php) |
| Leopard complex | `LP`, `PATN1`, `PATN2` forced wild type | Appaloosa-type spotting is not part of the supported Yonaguni phenotype. |
| Magical loci | All forced wild type | The dorsal line is a real primitive-marking issue, not magical zebra coloration. |
| Disorders | All requested loci clear | No credible Yonaguni-specific carrier frequency was located for ACAN dwarfism, WFFS, lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. Rare-population conservation status does not justify inventing disease rates. |
| Speed | `4/10` | The tiny island work horse was selected for useful transport and field work, not racing. |
| Jump | `3/10` | Sure-footedness and toughness do not support a specialist jumping score. |
| Health | `9/10` | Represents hard hooves, island hardiness, compact body, and historical work utility—not immunity from all disease, injury, or inbreeding pressure.  [oki-park](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855) |
| Size | `×0.70–0.80` | Represents the documented 110–120 cm, about 10.3–11.3 hand, miniature-horse-to-small-pony scale.  [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/) |

## Disorder approach

Every disorder locus listed in the prompt is intentionally **clear** in the pure Yonaguni founder pool.

The relevant research supports Yonaguni population history and microsatellite/mitochondrial diversity, not carrier prevalence for ACAN dwarfism, PLOD1/WFFS, `EDNRB`/frame lethal white, SCID, CA, LFS, GBED, CVM, megaesophagus, HYPP, PSSM1, or HERDA. It would be scientifically misleading to transfer disease frequencies from Japanese mainland breeds, Arabians, Quarter Horses, Friesians, European warmbloods, or other small ponies into this isolated island population. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

This does **not** mean that real Yonaguni horses are genetically invulnerable. It means that no named mutation is seeded by default without evidence. Players can still introduce such alleles through outcrossing or normal mod mechanics.

### Primitive-marking caution

Do not encode the Yonaguni dorsal stripe as:

- A true-dun `D` allele, unless an actual Yonaguni `TBX3` study supports that conclusion.
- The magical `zebra` locus.
- A gray modifier.
- A random coat-shade band.

If the engine lacks the `nd1` allele, the correct scientifically conservative choice is a solid bay founder pool with no forced dorsal stripe. A small missing visual detail is better than systematically generating genetically incorrect yellow-dun horses.

## Code map

| Location | Implementation responsibility |
|---|---|
| `common/breed/Breed` | Add `yonaguni_horse` with Japanese island conservation flavour, very-rare commonness, coastal/subtropical biome mapping, narrow solid coat pool, clear disorders, and very small hardy stats. |
| `common/breed/Breeds` | Register the breed for wild packs, stable access, spawn eggs, H-menu display, breed books, commands, saved-genome loading, and lineage naming. |
| `common/breed/BreedSource` | Validate `wild`, `spawn_egg`, and `stable`. `cowboy` is omitted because Yonaguni is an endangered island conservation breed rather than a normal commercial sale-yard type. |
| `common/breed/BreedBands` | Permit an empty epigenetic-band map. Do not use an epigenetic shade band to fake the reported dorsal stripe. |
| `common/breed/spec/` | Verify the exact JSON format, especially whether `dun` supports `D`, `nd1`, and `nd2`; if it does not, remove the illustrative `nd1`/`nd2` frequencies and leave the locus omitted or forced to the schema’s normal non-dun wild type. |
| `common/breed/Commonness` | Confirm `VERY_RARE` maps to the intended `0.75` rarity weight. |
| `common/breed/BreedStatCurve` | Convert speed 4, jump 3, health 9, and size ×0.70–0.80 into legal `TargetBand` values. |
| `common/breed/BreedFounder` | Roll the bay-heavy `E/e` and `A/a` pools; use `nd1` only if supported; force all dilution, white-pattern, leopard, magical, and named disorder loci clear. |
| `common/breed/BreedLineage` | Use default pure/cross/Mixed behavior. A short bay horse with a dorsal stripe must not receive Yonaguni lineage from appearance or biome alone. |
| `common/genetics/SpliceOutcome` | No breed-specific exception. A successfully spliced allele transmits normally and can create a nonstandard descendant, even though it would not represent a typical pure Yonaguni horse. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Serialize very small size, low specialist speed/jump, and high heartiness targets. |
| `common/breed/BandType` | No `TRADITIONAL` or `BACHELOR` band is required unless the actual serializer requires an explicit default value. |

## Verification

1. Confirm **Yonaguni Horse** appears in the H-menu’s Breeds tab and breed book with its Yonaguni Island origin, very-rare commonness, tiny size, bay-dominant solid coat policy, no-white rule, island-work history, and dorsal-stripe caveat.

2. Spawn repeated packs only in eligible island-like coast, warm forest, sparse jungle, meadow, and wind-exposed hill biomes. Every member of an individual selected pack must display the **Yonaguni Horse** breed label.

3. Confirm that a lone ordinary wild horse reads **Feral Mixed**, even if it is tiny, bay, solid, on a beach, or in a warm island biome.

4. Generate at least 1,000 pure founders. The output should be overwhelmingly bay, with smaller dark bay/brown, black, and chestnut minorities. No pure founder should be gray, palomino, buckskin, cremello, dun, champagne, silver, pearl, mushroom, roan, pinto, leopard-spotted, brindle, or magical.

5. Confirm all pure founders have no ordinary white pattern:
- `TO`, `O`, `SB1`, splash alleles, KIT white, `LP`, `PATN1`, and `PATN2` must be wild type.
- The renderer must not add conspicuous inherited face or leg white as a hidden founder feature.

6. If `TBX3` supports `D/nd1/nd2`, inspect dorsal-striping logic:
- No founder may carry `D`; pure founders must not become body-diluted yellow dun.
- Some founders may carry `nd1` and show modest primitive dorsal marking while keeping a bay body.
- If `nd1` does not visibly produce correct subtle markings, set `nd1` to wild type instead of forcing an inaccurate stripe.

7. Confirm all named disorder loci are clear in a large founder sample. No disease allele should originate in the pure Yonaguni file.

8. Confirm body-stat behavior. Adult Yonaguni horses should be among the mod’s smallest ridable equines, with very high heartiness but modest speed and low jumping specialization. They should feel practical and agile, not mechanically weak or unusable.

9. Test default lineage:
- Yonaguni Horse × Yonaguni Horse → Yonaguni Horse.
- Yonaguni Horse × Misaki → Yonaguni Horse cross.
- Yonaguni Horse × Hokkaido Horse → Yonaguni Horse cross.
- Yonaguni cross × pure Yonaguni Horse → the existing Yonaguni cross label.
- Two different crosses → Mixed.
- Any pairing involving Feral Mixed → Mixed.

10. Test deliberate outcrossing or splice insertion. Introducing true dun, gray, cream, tobiano, leopard complex, or any disease allele should produce Mendelian descendants, but they should be visually nonstandard for pure Yonaguni flavour and should never acquire a pure Yonaguni label solely because they remain small or live in an island biome.

## Sources

- [Genetic diversity of the Yonaguni horse based on polymorphisms in microsatellites and mitochondrial DNA](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/): peer-reviewed primary source for Yonaguni Island origin, estimated population size, 110–120 cm height, mostly bay coat, and conservation-genetics context. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC5326952/)

- [Yonaguni Town Natural Monument — Yonaguni Horse](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855): local official natural-monument material for protected status, 110–120 cm size, historical farm/pack use, strong hooves, bay coloration, and the reported dark back stripe. [oki-park](https://oki-park.jp/sp/kaiyohaku/en/inst/79/6855)

- [Yonaguni horse overview](https://en.wikipedia.org/wiki/Yonaguni_horse): supplementary synthesis of Japanese native-breed status, endangered condition, island location, historic size evidence, and broad population context. [en.wikipedia](https://en.wikipedia.org/wiki/Yonaguni_horse)

- [Oklahoma State University — Yonaguni Horses](https://breeds.okstate.edu/horses/yonaguni-horses): supplementary breed reference for head and body type, small ears, short neck, island work role, and chestnut occurrence. [breeds.okstate](https://breeds.okstate.edu/horses/yonaguni-horses)

- [Yonaguni breed reference](https://hi3.horseisle.com/www/bbb/Yonaguni.php): supplementary detailed account of local history, remoteness during Meiji horse policy, twentieth-century decline, mane/tail traits, mostly bay/dark bay/chestnut palette, and solid no-marking standard. [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Yonaguni.php)

- [UC Davis Veterinary Genetics Laboratory — Dun Dilution](https://vgl.ucdavis.edu/test/dun-horse): authoritative explanation of true `D` versus `nd1`/`nd2` and why a dorsal stripe does not automatically prove body-diluting dun genetics. [vgl.ucdavis](https://vgl.ucdavis.edu/test/dun-horse)