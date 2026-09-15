The **Australian Stock Horse** is an Australian all-purpose working horse, formed from colonial, Waler, Thoroughbred, Arabian, pony, and later stock-horse influences for stock work across harsh, variable country. In the mod, it should feel like a compact-to-medium, tough, sure-footed, agile ranch horse with an unusually broad but still believable coat palette—not a narrowly standardized show breed. [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse)

## Identity & flavour

The **Australian Stock Horse**, commonly called the **ASH** or **Stockhorse**, is Australia’s home-grown working stock horse. Its ancestry reaches back to the horses unloaded with the British First Fleet at Port Jackson in 1788, then developed through generations of Australian selection from English Thoroughbred and Spanish stock, later influenced by Arabian, Timor pony, Welsh Mountain pony, Waler, and other useful riding-horse lines. The Australian Stock Horse Society was founded in 1971 in New South Wales to preserve and promote the distinct breed. [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/)

This is a horse made for work first: mustering cattle and sheep, covering long distances, handling rough country, and turning sharply enough for campdrafting, polocrosse, polo, cutting, and modern ranch-style events. Australian conditions demanded a horse that could travel economically across hot, dry plains, uneven bush country, and open pasture, remain level-headed around livestock, and still have the acceleration and cow sense to stop, turn, and sprint when a beast broke away. It is often described as the “breed for every need,” because the same practical foundation supports work, leisure riding, endurance, jumping, eventing, and competition. [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/)

Australian Stock Horses generally stand from 14 to 16.2 hands, with 14–16 hands historically preferred by the breed standard. They should be balanced and well muscled without becoming bulky: an alert, intelligent head with broad forehead and large eyes; a clean throatlatch; a good-length neck; a sloping shoulder; well-defined withers; a deep chest; a strong, medium-length back; and rounded, powerful hindquarters. The legs should have short, practical cannons, clean broad hocks, visible tendons, and feet that are hard, straight, proportional, and wide-heeled. No feather, extravagant mane, or giant draft-like hoof belongs here—this is a practical horse built to stay sound while working. [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse)

The breed accepts **all colors**, reflecting its genetically diverse foundation. Bay, brown, black, chestnut, and gray are common; buckskin, dun, palomino, roan, and many other colors also occur. For the mod, an ASH herd should therefore be more varied than a purposefully color-restricted breed, while retaining a grounded stock-horse look: ordinary solid colors should dominate, with occasional cream dilution, dun, gray, roan, silver, champagne, and restrained pinto expression. A loudly patterned horse can exist, but it should be an exciting outlier rather than the default appearance of every herd. [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse)

The ASH is prized for intelligence, courage, toughness, stamina, soundness, agility, a sure-footed walk, and a calm but responsive mind. In a player’s stable it should be the horse that reliably gets the job done: quicker and more maneuverable than a heavy farm horse, more robust and tractable than a highly specialized racehorse, and compact enough to feel useful in rough terrain. What the mod cannot model is genuine cow sense, stock-work instinct, terrain judgment, campdraft skill, a particular head profile, hoof density, or the training that makes a horse a finished working partner. [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse)

## Breed JSON

> **Schema caveat:** As with the previous definitions, the linked Procedural Horse Genetics wiki page could not be retrieved by the browser tool. The JSON below uses readable proposed names and must be reconciled with the exact parser contract in `common/breed/spec/`, actual locus IDs, enum spelling, and `Commonness` ladder before integration.  
>
> **Disease caveat:** I could not find a credible, breed-specific allele-frequency study for Australian Stock Horses. Because the breed has Quarter Horse and broader stock-horse influence in some modern lines, the file includes very low, explicitly gameplay-conservative rates for Quarter Horse panel disorders. If your project requires directly measured breed-population frequencies only, set all of these disorder loci to clear until ASHS-specific screening data is available.

```json
{
  "id": "australian_stock_horse",
  "name": "Australian Stock Horse",
  "type": "natural",
  "notes": "Australian Stock Horses are defined by practical stock-horse conformation, cow sense, stock-work training, sure-footedness, stamina, and a calm but responsive working temperament. Procedural Horse Genetics does not simulate cattle instinct, mustering skill, campdrafting ability, polocrosse training, terrain judgment, hoof hardness, Australian heat tolerance, subjective balance, or the breed-standard head and body profile.",

  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_hills",
    "minecraft:meadow"
  ],
  "spawn_weight": 6,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 560,
  "commonness": "MODERATE",

  "coat_genes": {
    "extension": {
      "E": 0.70,
      "e": 0.30
    },
    "agouti": {
      "A": 0.68,
      "a": 0.32
    },

    "cream": {
      "N": 0.89,
      "Cr": 0.11
    },
    "dun": {
      "N": 0.89,
      "D": 0.11
    },
    "grey": {
      "N": 0.92,
      "G": 0.08
    },
    "roan": {
      "N": 0.93,
      "Rn": 0.07
    },
    "flaxen": {
      "N": 0.84,
      "f": 0.16
    },
    "silver": {
      "N": 0.975,
      "Z": 0.025
    },
    "champagne": {
      "N": 0.985,
      "Ch": 0.015
    },

    "tobiano": {
      "N": 0.96,
      "To": 0.04
    },
    "sabino_1": {
      "N": 0.93,
      "SB1": 0.07
    },
    "frame_overo": {
      "N": 0.985,
      "O": 0.015
    },
    "splash_white_1": {
      "N": 0.975,
      "SW1": 0.025
    },
    "kit_white_spotting": {
      "N": 0.97,
      "W": 0.03
    },
    "rabicano": {
      "N": 0.94,
      "Rb": 0.06
    },

    "pearl": {
      "N": 1.0
    },
    "mushroom": {
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
    "GBE1_GBED": {
      "N": 0.985,
      "gbed": 0.015
    },
    "GYS1_PSSM1": {
      "N": 0.985,
      "pssm1": 0.015
    },
    "PPIB_HERDA": {
      "N": 0.995,
      "herda": 0.005
    },
    "SCN4A_HYPP": {
      "N": 0.998,
      "hypp": 0.002
    },

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
    "CVM": {
      "N": 1.0
    },
    "megaesophagus": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 7,
    "jump": 7,
    "health": 9,
    "size": [
      0.94,
      1.06
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Australian Stock Horse × Australian Stock Horse produces Australian Stock Horse. Australian Stock Horse × another pure breed produces a cross. A cross back to a pure Australian Stock Horse remains a cross; two different crosses become Mixed; and any cross with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Suggested implementation | Reason |
|---|---:|---|
| Base colors | `E` 0.70 / `e` 0.30; `A` 0.68 / `a` 0.32 | Keeps bay/brown and black-pointed horses common while retaining a healthy chestnut/sorrel population. All colors are accepted, and common profiles list bay, brown, black, chestnut, and gray.  [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse) |
| Cream dilution | `Cr` 0.11 | Allows occasional palomino, buckskin, smoky black, cremello, perlino, and smoky cream without turning the breed into a dilute-color breed. Buckskin and palomino are repeatedly listed as seen colors.  [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/) |
| Dun | `D` 0.11 | Duns fit the documented palette and practical Australian stock-horse feel.  [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/) |
| Gray | `G` 0.08 | Gray occurs but is less visually central than bay, chestnut, brown, or black.  [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/) |
| Roan | `Rn` 0.07 | Roan is documented among common or accepted colors; retain it as an occasional founder result.  [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/) |
| White patterns | Low tobiano, sabino, splash, frame, and KIT frequencies | “All colors” supports occasional patterned horses, but stock-type solids should remain the visual majority. The low frame frequency also reduces lethal-white risk while retaining a possible Quarter Horse-derived allele.  [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse) |
| Silver and champagne | Very low frequencies | Both appear in broader stock-horse color descriptions, but neither should characterize an ordinary ASH herd.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Australian_Stock_Horse.php) |
| Leopard complex | Forced clear | Appaloosa influence is not part of the core breed description, so pure ASH founders should not spontaneously generate leopard spotting. |
| Magical loci | Forced clear | This is an ordinary, natural horse breed. |
| Speed | 7/10 | Represents quickness for stock work and campdrafting without approaching a specialist Thoroughbred race profile.  [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse) |
| Jump | 7/10 | The breed’s athletic all-rounder role justifies solid jumping capacity.  [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/) |
| Health | 9/10 | Captures the breed’s explicit reputation for toughness, stamina, soundness, sure-footedness, and suitability for Australian conditions. It does not mean disease alleles are impossible.  [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse) |
| Size | ×0.94–1.06 | Reflects a useful 14–16.2-hand working range: often compact, but fully capable of a medium-sized riding-horse frame.  [en.wikipedia](https://en.wikipedia.org/wiki/Australian_Stock_Horse) |

## Disorder policy

The Australian Stock Horse Society material found in this search establishes the breed’s history, versatility, and performance identity, but does **not** provide a public ASHS-wide genetic-panel carrier survey. Therefore, the disease rates in the JSON are deliberately conservative design values—not claims that they are measured Australian Stock Horse allele frequencies.

The most defensible reason to include the four loci is modern stock-horse ancestry: Australian Stock Horses may include Quarter Horse and related stock-horse influence, and the relevant disorders are established in Quarter Horse populations. A Quarter Horse study reported allele frequencies of 0.055 for PSSM1, 0.054 for GBED, 0.021 for HERDA, and 0.008 for HYPP in a control population; HYPP and related conditions can vary greatly by bloodline, especially in halter-type lines. The ASH values here are much lower than those Quarter Horse estimates to avoid presenting indirect ancestry as direct prevalence evidence. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/19119976/)

If you want a **strict evidence-only breed file**, replace `disorder_genes` with the following until an Australian Stock Horse-specific study or registry testing policy supplies rates:

```json
"disorder_genes": {
  "ACAN_dwarfism": { "N": 1.0 },
  "PLOD1_friesian_dwarfism": { "N": 1.0 },
  "MET_lethal_white": { "N": 1.0 },
  "PRKDC_SCID": { "N": 1.0 },
  "TOE1_CA": { "N": 1.0 },
  "MYO5A_LFS": { "N": 1.0 },
  "GBE1_GBED": { "N": 1.0 },
  "CVM": { "N": 1.0 },
  "megaesophagus": { "N": 1.0 },
  "SCN4A_HYPP": { "N": 1.0 },
  "GYS1_PSSM1": { "N": 1.0 },
  "PPIB_HERDA": { "N": 1.0 }
}
```

## Code map

| Location | Change or verification |
|---|---|
| `common/breed/Breed` | Add the natural `australian_stock_horse` record with its presentation metadata, spawn conditions, price, and flavour notes. |
| `common/breed/Breeds` | Register the breed ID for data loading, UI display, breeding, spawning, and saved genome resolution. |
| `common/breed/BreedSource` | Confirm the file can expose all four sources: `wild`, `cowboy`, `spawn_egg`, and `stable`. |
| `common/breed/BreedBands` | Confirm an empty epigenetic-band map is accepted; this breed needs broad phenotype variation rather than a pinned color shade. |
| `common/breed/spec/` | Translate every proposed JSON field and allele label into the mod’s actual serialization/deserialization schema. |
| `common/breed/Commonness` | Verify that `MODERATE` maps to weight `6`; use the project’s native commonness enum if the spelling differs. |
| `common/breed/BreedStatCurve` | Convert speed 7, jump 7, health 9, and size ×0.94–1.06 into valid `TargetBand` objects. |
| `common/breed/BreedFounder` | Ensure only the listed coat/disorder pools roll for pure founders and every omitted locus is forced wild type or clear. |
| `common/breed/BreedLineage` | Keep default pure/cross/mixed/Feral Mixed behavior. |
| `common/genetics/SpliceOutcome` | No special breed behavior; confirm edited alleles can transmit normally to ASH foals. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate each stat axis and associated target band. |
| `common/breed/BandType` | Do not assign a special `TRADITIONAL` or `BACHELOR` band unless the current implementation requires one. |

## Verification

1. Open the H-menu’s Breeds tab and breed book. Confirm **Australian Stock Horse** appears with the correct display name, natural classification, stock-horse flavour text, selected biome set, moderate rarity, and four acquisition sources.

2. Spawn or find an eligible wild herd in plains, savanna, meadow, or windswept-hills terrain. Every horse generated as part of a single pack should display **Australian Stock Horse**, regardless of individual coat color.

3. Locate or generate a lone wild horse that has not spawned through the breed-pack system. Its lineage should read **Feral Mixed**, not Australian Stock Horse.

4. Generate 100–200 ASH founders. Expect bay/brown, black, and chestnut to dominate. Gray, buckskin, palomino, dun, and roan should appear often enough to establish a varied population but not exceed solid base colors. Silver, champagne, and loud pinto-patterned horses should be uncommon.

5. Confirm that pure founders never generate pearl, mushroom, leopard-complex spotting, PATN expression, brindle, or magical coat loci. Those traits should enter the population only through mutation, a deliberate genetic item, or outcrossing.

6. Inspect a large sample’s health panel. Only GBED, PSSM1, HERDA, and HYPP may roll as non-clear under the gameplay-conservative configuration; all other named disorders must remain clear. If using the strict evidence-only version, every disorder must remain clear.

7. Breed known carrier pairs for each enabled recessive locus and verify normal Mendelian outcomes over a sufficiently large sample: approximately 25% affected, 50% carriers, and 25% clear from carrier × carrier pairings. For HYPP, verify the mod follows its actual inheritance implementation rather than treating it as a recessive disorder. [vgl.ucdavis](https://vgl.ucdavis.edu/test/hypp)

8. Confirm the lineage table:
   - Australian Stock Horse × Australian Stock Horse → Australian Stock Horse.
   - Australian Stock Horse × another pure breed → named cross.
   - Australian Stock Horse cross × a pure Australian Stock Horse → cross.
   - Different cross × different cross → Mixed.
   - Any lineage × Feral Mixed → Mixed.

## Sources

- [Australian Stock Horse Society](https://ashs.com.au/): official registry/society identity; founded in 1971 in Scone, New South Wales, to preserve and promote the breed’s bloodlines and versatile performance. [ashs.com](https://ashs.com.au/)
- [Adina Polocrosse World Cup — Australian Stock Horse](https://polocrosseworldcup.com.au/australian-stock-horse): ASHS-derived historical and Standard of Excellence material covering First Fleet origins, ancestry, preferred height, conformation, hard feet, musculature, and working-horse requirements. [polocrosseworldcup.com](https://polocrosseworldcup.com.au/australian-stock-horse)
- [MadBarn — Australian Stock Horse Breed Profile](https://madbarn.com/australian-stock-horse-breed-profile/): historical overview, ASHS registry context, height range, recognized color variety, and modern uses. [madbarn](https://madbarn.com/australian-stock-horse-breed-profile/)
- [PubMed — Evaluation of allele frequencies of inherited disease genes in American Quarter Horses](https://pubmed.ncbi.nlm.nih.gov/19119976/): Quarter Horse panel-locus frequency context used only as an indirect, deliberately reduced proxy for low-frequency modern stock-horse ancestry. [pubmed.ncbi.nlm.nih](https://pubmed.ncbi.nlm.nih.gov/19119976/)
- [UC Davis Veterinary Genetics Laboratory — HYPP](https://vgl.ucdavis.edu/test/hypp): HYPP inheritance and Quarter Horse concentration context. [vgl.ucdavis](https://vgl.ucdavis.edu/test/hypp)
