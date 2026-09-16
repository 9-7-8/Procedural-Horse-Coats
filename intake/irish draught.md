The **Irish Draught** should be a rare, substantial but athletic Irish foundation breed: a solid-colored, sound, sensible horse built for farm work, hunting, riding, driving, and producing Irish Sport Horses. Its founder pool should be broad but traditional—bay, brown, gray, chestnut, black, and dun—with loud white patterns and exotic dilutions excluded; it should include **PSSM1** at a cautious low proxy rate because the GYS1 mutation is documented in Irish Draughts, but no published Irish-Draught-specific population frequency was located. [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/)

> **Schema caveat:** The Procedural Horse Genetics wiki URL supplied earlier could not be retrieved by the available reader. The JSON follows the readable proposed convention from your examples. Align the precise field names, locus IDs, commonness/source enums, and required serialization structure with the current `common/breed/spec/` implementation before compiling.

## Identity & flavour

The **Irish Draught**, Irish **Dréacht Éireannach**, is Ireland’s indigenous all-purpose working horse and the foundation breed behind much of the modern **Irish Sport Horse** population. It developed over centuries from native Irish horses shaped by farm work, hunting, transport, and military demand, with later influence from Thoroughbred, Connemara, Clydesdale, and other British and European stock. The modern breed registry and inspection system emerged during the twentieth century as agricultural mechanization threatened the old farm-horse population. It is not a “draught” horse in the giant continental sense: it is a powerful, athletic, medium-heavy horse designed to work and still move freely. [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/)

Irish Draughts were made for the versatile rural life of Ireland. They ploughed and hauled, carried riders to hounds, worked in harness, and traveled over uneven ground in weather that demanded an honest constitution. Their most influential modern job may be as a crossbreeding foundation: pairing an Irish Draught with a Thoroughbred or other sport horse can produce an Irish Sport Horse with bone, courage, good feet, tractability, and jumping scope. A pure Irish Draught in the mod should therefore feel useful in almost every ordinary activity—strong enough for practical work, athletic enough for riding, and sensible enough to make a reliable long-term mount. [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/)

Irish Draughts should stand **158–170 cm (15.2–16.3 hands)** at maturity. The build is substantial but not coarse: a pleasant, broad-foreheaded head with kind eyes; a long well-set neck; defined withers; a deep heartgirth; strong back and loins; a long, gently sloping croup; straight limbs with short, flat, clean cannons; and around nine inches of quality bone. The hooves should be hard, sound, and well formed—neither boxy, overlarge, nor flat. Their mane and tail are full but manageable, while feathering remains modest compared with Clydesdale or Shire types. At a glance, it should be a big, solid, workmanlike hunter: powerful through the body, clean through the limbs, and capable of active movement rather than ponderous draft-horse action. [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/)

The official color rule is admirably broad but not uncontrolled: **any strong whole color**, including bay, brown, gray, chestnut, black, and dun, is accepted; excessive white markings are undesirable. That means this should not be a visually uniform breed file. Bay and brown should dominate, with chestnut, gray, and black all visibly possible, and a modest dun pool reflecting explicit studbook acceptance. Cream, pearl, champagne, silver, mushroom, tobiano, frame, splash, leopard complex, roan, and other loud patterns should remain absent from pure founders. This preserves the real “solid, practical Irish horse” look while giving players a diverse, believable herd. [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/)

The Irish Draught is designated an endangered breed by Ireland’s Department of Agriculture, and current breeding policy emphasizes genetic diversity alongside conformation, soundness, and performance. It carries a cultural weight far beyond its census: it is one of the roots of Irish hunting and sport-horse identity, and careful purebred breeding is necessary so the Irish Sport Horse does not eclipse the foundation breed that made it possible. In Procedural Horse Genetics, a player should recognize the Irish Draught as a tall, solid-colored Irish all-rounder: calm and common-sense, deep-chested, powerful, athletic over practical obstacles, and distinctly more versatile than a pure heavy draught. The mod does not model inspection scores, hunting courage, individual manners, hoof quality, feathering, pull strength, Irish Sport Horse eligibility, Irish Draught percentage, saddle fit, or training for hunting, eventing, driving, or farm work. [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/)

## Breed JSON

```json
{
  "id": "irish_draught",
  "name": "Irish Draught",
  "type": "natural",
  "notes": "The Irish Draught is defined by inspection-led breeding for substance with quality, hard sound hooves, clean flat bone, deep body, powerful active movement, common-sense temperament, farm and hunting versatility, and its importance as a foundation of the Irish Sport Horse. Procedural Horse Genetics does not model studbook inspection, Irish Draught pedigree percentage, hoof quality, exact feathering, individual temperament, hunting bravery, true pulling strength, jump technique, farm training, driving training, Irish Sport Horse eligibility, or the conservation management needed to maintain genetic diversity in the real breed.",

  "biomes": [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:river",
    "minecraft:swamp",
    "minecraft:grove",
    "terralith:temperate_highlands",
    "terralith:wetland",
    "terralith:lowlands",
    "terralith:shrubland"
  ],
  "spawn_weight": 1.5,
  "spawn_time": "day",
  "sources": [
    "cowboy",
    "spawn_egg",
    "stable"
  ],
  "price": 1160,
  "commonness": "RARE",

  "coat_genes": {
    "extension": {
      "E": 0.76,
      "e": 0.24
    },
    "agouti": {
      "A": 0.76,
      "a": 0.24
    },

    "grey": {
      "N": 0.82,
      "G": 0.18
    },
    "dun": {
      "N": 0.94,
      "D": 0.06
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
    "GYS1_PSSM1": {
      "N": 0.98,
      "pssm1": 0.02
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
    "PPIB_HERDA": {
      "N": 1.0
    }
  },

  "stat_scores": {
    "speed": 6,
    "jump": 7,
    "health": 9,
    "size": [
      1.07,
      1.18
    ]
  },

  "epigenetic_bands": {},

  "strains": []
}
```

## Genetics rationale

| Feature | JSON choice | Rationale |
|---|---:|---|
| Base colors | `E` 0.76 / `e` 0.24; `A` 0.76 / `a` 0.24 | Bay and brown are used as the visual center of a broad but solid Irish Draught founder population. Chestnut and black remain established acceptable colors. These are explicit gameplay estimates because no Irish-Draught-specific MC1R/ASIP allele-frequency study was located.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |
| Gray | `G` 0.18 | Gray is directly accepted in the official standard and is common enough to belong in ordinary founder herds, but it should not erase the dominant bay/brown identity. The 0.18 figure is phenotype-informed rather than a published STX17 allele survey.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |
| Dun | `D` 0.06 | The Horse Sport Ireland standard explicitly accepts dun. A low rate makes dun a genuine but uncommon breed-consistent result rather than a pervasive feature.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |
| Other dilutions | Forced wild type | Cream, pearl, champagne, silver, mushroom, and flaxen are not named in the formal “strong whole color” list used in the accessible Irish breed standard. Excluding them keeps pure founders conventionally Irish Draught.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |
| White patterns | Forced wild type | The standard discourages excessive white. The mod’s major white-pattern loci would often render far more white than ordinary facial and leg markings, so all are forced wild type. Small routine markings remain a renderer/model concern rather than evidence for a major white allele.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |
| PSSM1 | `pssm1` 0.02 | PSSM1 has been identified in Irish Draughts, but no public breed-wide allele estimate was found. This 2% allele rate is a deliberately conservative gameplay proxy, **not** a published Irish Draught carrier rate. PSSM1 is autosomal dominant, so implementation should respect the mod’s dominant-disorder behavior.  [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf) |
| Other disorders | Clear | No defensible Irish-Draught-specific carrier frequencies were located for the other listed loci. |
| Speed | 6/10 | An Irish Draught is active and athletic, but it is not a pure racehorse. This score captures capable hunting, riding, and sport-cross utility.  [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/) |
| Jump | 7/10 | The breed’s powerful hindquarters, active movement, and historic role as a principal ingredient of Irish Sport Horses support a clear jumping strength without claiming it is a dedicated modern showjumper.  [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/) |
| Health | 9/10 | Represents the standard’s emphasis on robust constitution, soundness, good bone, and hard feet; it does not imply immunity from inherited disease, especially PSSM1.  [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/) |
| Size | ×1.07–1.18 | Models a substantial mature Irish working-hunter type, with real height kept in the flavor text rather than the JSON.  [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/) |

## Disorder caution

The **PSSM1** entry is intentionally conservative. Published clinical-genetics material identifies Irish Draughts among breeds in which the dominant **GYS1** PSSM1 mutation has been found, but the accessible sources do not provide a population-representative Irish Draught carrier or allele frequency. The JSON’s `pssm1: 0.02` is therefore a gameplay proxy rather than a measured statistic. [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf)

If your breed-file policy requires direct breed-specific frequency evidence only, replace the disorder section with `GYS1_PSSM1: { "N": 1.0 }` and keep every disorder clear. That is more conservative than claiming the mutation is absent, but avoids manufacturing a prevalence estimate.

## Verification

1. Confirm **Irish Draught** appears in the H-menu’s Breeds tab and the breed book with the natural classification, rare commonness, managed-source checklist, Irish grassland/wetland biome profile, price, and the correct breed notes.

2. Confirm the breed does not appear in wild packs because `wild` is omitted. Verify access through cowboy stock, stable stock, and the spawn egg.

3. Confirm a lone naturally generated horse without a selected breed source is labelled **Feral Mixed**, not Irish Draught.

4. Generate at least 1,000 founders. The group should be mostly bay and brown, with clearly visible chestnut, black, and gray minorities; dun should appear occasionally. Exact visible ratios will differ because gray progressively masks underlying base color.

5. Inspect founder genomes. Only extension, agouti, gray, dun, and—if retaining the gameplay-proxy health implementation—PSSM1 should contain non-clear/non-wild alleles. All cream, pearl, champagne, silver, mushroom, flaxen, major white patterns, roan, rabicano, leopard complex, brindle, magical loci, and other disorder loci must remain clear.

6. Test PSSM1 implementation separately. If retaining the 2% proxy rate, sample enough founders to find heterozygotes, cross them to clears, and confirm the mod’s dominant allele transmission. If using direct-evidence-only mode, verify no PSSM1 allele appears in pure founders.

7. Compare the breed with a heavy draught, a Thoroughbred, and an Irish Sport Horse-style cross. Irish Draughts should be large, sturdy, athletic, and notably good at jumping for their substance, but slower than dedicated racers and less massive than the largest farm draughts.

## Sources

- [Horse Sport Ireland — Irish Draught Studbook](https://www.horsesportireland.ie/studbooks/irish-draught/): official Irish studbook source for the ideal 158–170 cm height range, strong clean flat bone, active movement, accepted whole colors—bay, gray, chestnut, black, brown, and dun—and the discouragement of excessive white markings. [horsesportireland](https://www.horsesportireland.ie/studbooks/irish-draught/)

- [Irish Rare Breeds Society — Irish Draught](https://irbs.ie/traditional-irish-breeds/irish-draught/): detailed Irish breed-standard source for the versatile, powerful, athletic type; head, neck, body, limb, hoof, movement, temperament, constitution, and foundation-breed role in leisure and performance horses. [irbs](https://irbs.ie/traditional-irish-breeds/irish-draught/)

- [Irish Draught Horse Society of North America](https://www.irishdraught.com/about): registry context for purebred Irish Draught and partbred Irish Draught Sport Horse studbooks, North American inspection, and registry maintenance. [irishdraught](https://www.irishdraught.com/about)

- [Horse Sport Ireland studbook-breeds brochure](https://ihb.ie/wp-content/uploads/2024/07/Studbook-Breeds-Brochure-updated-2024.pdf): supporting formal color and height wording for the Irish Draught standard. [ihb](https://ihb.ie/wp-content/uploads/2024/07/Studbook-Breeds-Brochure-updated-2024.pdf)

- [McCue et al. — GYS1 mutation in diverse breeds](https://onlinelibrary.wiley.com/doi/10.1111/j.1939-1676.2008.0167.x): primary clinical-genetics research on the GYS1 PSSM1 mutation across breeds. [onlinelibrary.wiley](https://onlinelibrary.wiley.com/doi/10.1111/j.1939-1676.2008.0167.x)

- [Equine muscle-disease genetics review](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf): identifies Irish Draughts among breeds in which the GYS1 PSSM1 mutation has been detected and places its prevalence in broader breed context. [escholarship](https://escholarship.org/content/qt1rq2g2xz/qt1rq2g2xz.pdf)