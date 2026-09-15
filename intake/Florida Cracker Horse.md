The **Florida Cracker Horse** is a rare American colonial-Spanish stock horse shaped by Florida’s heat, wetlands, pine flatwoods, scrub, and open cattle range. In the mod it should feel like a compact, lean, fast-walking, deeply hardy cow horse: dark bay and black most often, but with a genuinely Spanish-derived palette that includes grey, chestnut, dun, grullo, roan, and occasional pinto rather than the near-uniform darkness of a Fell Pony. [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/)

## Identity & flavour

The **Florida Cracker Horse**, also called the **Cracker Horse**, **Florida Horse**, or occasionally the **Chickasaw Pony** in older historical discussion, is Florida’s native heritage saddle horse. It descends from horses brought to the southeastern North American mainland by Spanish expeditions during the early 1500s, including expeditions associated with Ponce de León and later Spanish colonists. Over centuries of geographic isolation, feral life, ranch work, and selective breeding in Florida, those Iberian-rooted horses became a distinct regional type: small, tough, quick, and perfectly adapted to the subtropical Southeast. The modern Florida Cracker Horse Association was organized in 1989 to register and protect the breed. [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/)

The name comes from Florida’s cattle culture. Early cowhunters and ranchers were called “crackers” for the crack of their long rawhide whips, and their small, agile cattle horses inherited the name. Long before the modern Western ranch horse became widespread, Florida Crackers gathered semi-feral cattle through palmetto scrub, pine woods, swamp margins, wet prairie, and brushy hammock. The work rewarded a horse that could travel all day, pick a safe line through bad footing, react quickly to cattle, tolerate heat and insects, and remain useful on relatively little feed. [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse)

A Florida Cracker Horse is a small, light riding horse rather than a heavy Quarter Horse-type animal. The breed association describes a height range of about 13.2 to 15.2 hands and a weight of roughly 700 to 1,000 pounds. The type is lean and functional: a broad forehead, refined face, straight or slightly Roman profile, alert eyes that may show white sclera, a well-defined narrow-to-medium neck, a long sloping shoulder, pronounced but not high withers, a short strong back, a medium-to-narrow chest, a short sloping croup, and a tail carried medium-low. Fine but strong legs and hard feet matter more than mass; it should look ready to slip through trees, cover ground, and turn after a calf rather than pull a plough. [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/)

The palette is notably broad for a heritage stock breed. Bay, dark bay, black, brown, grey, chestnut, dun, and grullo are all associated with the breed; roan and pinto strains persist, and dun individuals may show primitive markings such as dorsal stripes, leg barring, and shoulder crosses. The ordinary herd should still skew dark and practical—black and dark bay are particularly common in modern descriptions—but the occasional grullo, dun, grey, roan, chestnut, or tobiano should feel authentic, not like an outcrossing mistake. This is a compact Spanish-descended horse that should make players stop and inspect the coat rather than assume every founder looks alike. [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse)

Florida Crackers are prized for endurance, agility, sure-footedness, cow sense, quick acceleration, and their smooth, ground-covering way of going. Older accounts and breed advocates emphasize that the best way to recognize one is to ride it: the breed’s usefulness is in efficient movement and all-day practicality, not simply its silhouette. Its population declined sharply in the 1930s when Florida’s cattle industry shifted toward larger cattle and ranchers adopted bigger American Quarter Horses; conservation-minded families, cattlemen, public herds, and the FCHA helped preserve it. Florida formally designated the Florida Cracker Horse its official state horse in 2008, making it a particularly fitting rare heritage find in a subtropical Minecraft world. [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/)

What the mod does **not** model is the true smoothness of the breed’s ground-covering gait, cow sense, swamp judgment, heat and insect tolerance, historical cattle-work training, exact Spanish head type, white-sclera expression, hoof hardness, palmetto-scrub maneuverability, or the cultural meaning of a living Florida heritage breed. The genetics should represent its compact, tough, Spanish-stock character—not imply that coat colour alone makes a finished Florida cow horse.

## Breed JSON

> **Schema caveat:** The supplied Procedural Horse Genetics breed-wiki URL could not be retrieved by the browser tool. This follows the naming and object layout in your Australian Stock Horse example, but it must be reconciled with the actual parser contract in `common/breed/spec/`, the real project locus IDs, available biomes, enum spelling, and source names before it is merged.
>
> **Disease caveat:** I found no credible Florida Cracker Horse Association population study establishing nonzero carrier frequencies for the disorder panel named in the prompt. All listed disorders are therefore forced clear. This is preferable to importing speculative Quarter Horse disease rates into a distinct, conservation-managed colonial-Spanish breed.

```json
{
  "id": "florida_cracker_horse",
  "name": "Florida Cracker Horse",
  "type": "natural",
  "notes": "Florida Cracker Horses are defined by compact colonial-Spanish saddle-horse conformation, a smooth and economical ground-covering way of going, agility, cow sense, heat tolerance, hard feet, stamina, and working ability in Florida scrub, wet prairie, pine flatwoods, and swamp-edge cattle country. Procedural Horse Genetics does not simulate cattle instinct, whip-horse training, swamp judgement, heat and insect tolerance, hoof hardness, white sclera, exact Spanish head profile, primitive-marking sharpness, gait smoothness, or the skill and experience of a finished Florida cow horse.",

  "biomes": [
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:plains",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:sparse_jungle",
    "minecraft:meadow"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "wild",
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 620,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.79,
      "e": 0.21
    },
    "agouti": {
      "A": 0.67,
      "a": 0.33
    },

    "dun": {
      "N": 0.79,
      "D": 0.21
    },
    "grey": {
      "N": 0.87,
      "G": 0.13
    },
    "roan": {
      "N": 0.91,
      "Rn": 0.09
    },
    "flaxen": {
      "N": 0.88,
      "f": 0.12
    },

    "tobiano": {
      "N": 0.95,
      "To": 0.05
    },
    "sabino_1": {
      "N": 0.95,
      "SB1": 0.05
    },
    "splash_white_1": {
      "N": 0.98,
      "SW1": 0.02
    },
    "kit_white_spotting": {
      "N": 0.97,
      "W": 0.03
    },
    "rabicano": {
      "N": 0.95,
      "Rb": 0.05
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

    "frame_overo": {
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
    "MET_lethal_at_conception": {
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
    "health": 9,
    "size": [
      0.84,
      0.97
    ]
  },

  "epigenetic_bands": {},

  "strains": [],

  "lineage": {
    "behavior": "default",
    "notes": "Use the default lineage table. Florida Cracker Horse × Florida Cracker Horse produces Florida Cracker Horse. Florida Cracker Horse × another pure breed produces a named cross. A Florida Cracker cross bred back to a pure Florida Cracker Horse remains a cross; two different crosses become Mixed; and any lineage crossed with Feral Mixed becomes Mixed."
  }
}
```

## Genetics rationale

| Feature | Suggested implementation | Rationale |
|---|---:|---|
| Base colour | `E` 0.79 / `e` 0.21 and `A` 0.67 / `a` 0.33 | Produces a practical mixture led by bay/dark bay and black, while retaining a real chestnut population. Modern sources consistently list bay, black, grey, dun, grullo, and chestnut; dark bay and black are often described as especially common.  [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse) |
| Dun | `D` 0.21 | Dun and grullo are characteristic enough to deserve a visibly recurring founder presence. Dun also enables the breed’s Spanish-stock primitive-marking identity: dorsal stripe, leg bars, and shoulder cross.  [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse) |
| Grey | `G` 0.13 | Grey is a recognized, regularly encountered Florida Cracker colour but should remain distinctly less common than the dark base colours.  [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/) |
| Roan | `Rn` 0.09 | Roan strains persist historically and are repeatedly noted as occasional; nine percent makes roan findable without turning the herd into a roan breed.  [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse) |
| Flaxen | `f` 0.12 | A modest recessive flaxen pool gives chestnut founders occasional lighter manes and tails, consistent with Spanish-type colour diversity, without making flaxen a breed hallmark. |
| Tobiano | `To` 0.05 | Pinto exists as a rare but legitimate historical strain. A five-percent allele frequency makes tobiano unusual but exciting rather than impossible in pure stock.  [en.wikipedia](https://en.wikipedia.org/wiki/Florida_Cracker_Horse) |
| Sabino / splash / KIT | Low frequencies | These loci provide a mechanism for common ordinary white markings and occasional low-to-moderate pinto expression, while keeping solid horses dominant. The exact allele frequencies are deliberate gameplay approximations, not published breed-population counts.  [hi3.horseisle](https://hi3.horseisle.com/www/bbb/Florida_Cracker.php) |
| Cream / pearl / champagne / silver | Forced clear | These are not documented as central colours in the reviewed breed-association and public heritage sources. They can enter through outcrossing, mutation, or project items, but should not characterize pure Florida Cracker founders.  [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/) |
| Frame overo | Forced clear | There is no breed-specific evidence here to justify a frame pool, and avoiding it prevents introducing an unsupported lethal-white risk into a small conservation breed. |
| Leopard complex | Forced clear | Appaloosa-style leopard spotting is not part of the documented core Florida Cracker palette. |
| Magical loci | Forced clear | This is an ordinary natural breed. |
| Speed | 6/10 | The breed is agile and quick enough for historical cattle work, with a ground-covering gait, but it is not a specialist racehorse.  [madbarn](https://madbarn.com/florida-cracker-horse-breed-profile/) |
| Jump | 5/10 | A neutral-to-useful jump score suits a compact all-purpose saddle horse without inventing a focused sport-jumping history. |
| Health | 9/10 | This represents strong practical hardiness, stamina, hard feet, and adaptation to heat, wet ground, scrub, and sparse working conditions—not immunity from all disease.  [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/) |
| Size | ×0.84–0.97 | The documented 13.2–15.2-hand range is small-to-medium, sitting above most native ponies but below a large modern stock horse.  [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/) |

## Disorder policy

The file forces every named disorder locus clear because no credible, breed-specific carrier-rate survey was found in the Florida Cracker Horse Association material, Florida state heritage information, or the extension publication describing the breed’s history and conservation.

That choice is intentionally conservative. The Florida Cracker Horse is genetically connected to colonial Iberian horses and is historically distinct from the later Quarter Horse population that displaced it in much Florida cattle work during the 1930s. It would be misleading to assign Quarter Horse-panel disorder frequencies merely because both breeds have cattle-horse histories. [floridacrackerhorseassociation](https://floridacrackerhorseassociation.com/about-us/)

If the project later obtains a Florida Cracker-specific population test report, add only the supported locus or loci and cite the actual screened population, sample size, carrier count, testing lab, and study year. Until then, all panel disorders should remain clear in pure founders.

## Code map

| Location | Change or verification |
|---|---|
| `common/breed/Breed` | Add the natural `florida_cracker_horse` record, presentation data, biomes, price, commonness, source list, genes, and stat targets. |
| `common/breed/Breeds` | Register the ID so the breed loads, displays in the UI, spawns, breeds, and survives saved-genome serialization. |
| `common/breed/BreedSource` | Confirm `wild`, `cowboy`, `spawn_egg`, and `stable` map to the project’s real source enum values. |
| `common/breed/BreedBands` | Confirm the empty epigenetic-band map is valid. This breed should rely on allele variety, especially dun and grey, rather than a pinned pigment-shade band. |
| `common/breed/spec/` | Translate the proposed JSON keys and allele names into the actual bidirectional data format. |
| `common/breed/Commonness` | Verify `RARE` maps to the intended weight. Under the user-provided doubling ladder, moderate is 6 and rare is plausibly 1.5. |
| `common/breed/BreedStatCurve` | Convert speed 6, jump 5, health 9, and size ×0.84–0.97 into valid `TargetBand` values. |
| `common/breed/BreedFounder` | Ensure pure founders roll only the stated coat pools; all omitted genes must resolve to wild type or disorder clear. |
| `common/breed/BreedLineage` | Retain normal pure, cross, Mixed, and Feral Mixed lineage behavior. |
| `common/genetics/SpliceOutcome` | No special logic; validate ordinary inheritance for dun, grey, roan, tobiano, white-marking, and all other permitted loci. |
| `common/trait/{StatAxis,TargetBand,BreedStatTargets}` | Validate the four trait targets and their size conversion against the project’s horse-height curve. |
| `common/breed/BandType` | No special `TRADITIONAL` or `BACHELOR` behaviour is required unless the implementation mandates a default band type. |

## Verification

1. Open the H menu’s Breeds tab and the breed book. Confirm that **Florida Cracker Horse** displays as a natural breed with rare commonness, its Florida-like biome list, four acquisition methods, correct flavour text, and a compact stock-horse stat profile.

2. Generate wild breed packs in eligible warm open-country biomes. Every individual in one pack must read **Florida Cracker Horse**, even when individual founders differ in base colour, grey status, dun, roan, or white markings.

3. Generate a lone wild horse outside the managed breed-pack path. It must display **Feral Mixed**, not Florida Cracker Horse, even if it is a dark bay or dun horse generated in a Florida-like biome.

4. Generate at least 100–200 Florida Cracker founders. Expect dark bay/bay and black to form the largest groups; chestnut, grey, dun, and grullo should be clearly present; roan should be occasional; tobiano or other notable pinto expression should be rare but possible.

5. Confirm that pure founders never generate cream, pearl, champagne, silver, mushroom, frame overo, leopard complex, PATN expression, brindle, or magical coats. Those phenotypes must require outcrossing, mutation, or an intentional gene-editing mechanic.

6. Check primitive-marking rendering on dun founders. If the mod separates primitive markings from the dun locus, confirm the Florida Cracker definition references the appropriate dun/primitive allele or epigenetic system. If it does not support primitive markings, keep dun for the base dilution but state the rendering limitation in the breed book.

7. Inspect health panels across a substantial founder sample. ACAN, PLOD1, MET, PRKDC, TOE1, MYO5A, GBE1, CVM, megaesophagus, SCN4A, GYS1, and PPIB must all be clear in pure Florida Cracker founders under this evidence-only configuration.

8. Confirm the default lineage table:
   - Florida Cracker Horse × Florida Cracker Horse → **Florida Cracker Horse**.
   - Florida Cracker Horse × another pure breed → named **cross**.
   - Florida Cracker cross × pure Florida Cracker Horse → **cross**.
   - Different cross × different cross → **Mixed**.
   - Any lineage × **Feral Mixed** → **Mixed**.

## Sources

- [Florida Cracker Horse Association — About Us](https://floridacrackerhorseassociation.com/about-us/): official breed-association source for Iberian/North African heritage framing, height and weight, head and body type, shoulder, chest, back, croup, and tail-set standard. [floridacrackerhorseassociation](https://floridacrackerhorseassociation.com/about-us/)
- [Florida Department of State — State Horse](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/): state source for Spanish ancestry, 13.2–15-hand type, physical description, heritage-breed status, and official state-horse context. [dos.fl](https://dos.fl.gov/florida-facts/florida-state-symbols/state-horse/)
- [Florida Department of Agriculture and Consumer Services — Cracker Cattle and Cracker Horse Program](https://www.fdacs.gov/Agriculture-Industry/Horses-Equine/Cracker-Cattle-and-Cracker-Horse-Program): official Florida heritage-breed conservation context. [fdacs](https://www.fdacs.gov/Agriculture-Industry/Horses-Equine/Cracker-Cattle-and-Cracker-Horse-Program)
- [University of Florida EDIS — The Florida Cracker Horse](https://journals.flvc.org/edis/article/view/117904/115906): extension source covering Spanish colonial ancestry, the breed’s distinct development, 1930s decline, and FCHA preservation history. [journals.flvc](https://journals.flvc.org/edis/article/view/117904/115906)
- [MadBarn — Florida Cracker Horse Breed Profile](https://madbarn.com/florida-cracker-horse-breed-profile/): consolidated modern description of height, conformation, commonly reported colours, primitive dun markings, and historic cattle-horse use. [madbarn](https://madbarn.com/florida-cracker-horse-breed-profile/)
- [Horse Illustrated — Florida Cracker Horse](https://www.horseillustrated.com/horse-news-florida-cracker-state-horse/): historical and cultural context for Florida “cracker” cowhunters, colour diversity, 1930s population decline, and the 1989 association formation. [horseillustrated](https://www.horseillustrated.com/horse-news-florida-cracker-state-horse/)