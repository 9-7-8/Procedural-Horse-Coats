```json
{
  "id": "petaled",
  "name": "Petaled",
  "category": "magical",
  "locus": "PTL",
  "alleles": ["PTL", "ptl"],
  "knobs": [
    { "name": "petalSeed", "type": "seed" },
    { "name": "veinSeed", "type": "seed" },
    { "name": "petalSpacing", "type": "float", "default": 7.5, "min": 5.5, "max": 9.5 },
    { "name": "petalJitter", "type": "float", "default": 0.4, "min": 0.2, "max": 0.55 },
    { "name": "petalStretch", "type": "float", "default": 1.3, "min": 1.1, "max": 1.6 },
    { "name": "petalWarp", "type": "float", "default": 0.55, "min": 0.35, "max": 0.75 }
  ],
  "expressions": [
    {
      "when": ["PTL/PTL", "PTL/ptl"],
      "description": "The coat over the body, neck, head and muzzle breaks into overlapping petal-shaped blobs. Each blob is rimmed with a thin dark rose edge, then a thick black line just inside it, and filled with pale blush laced with fine dark rose veins.",
      "layers": [
        {
          "masks": [
            { "mask": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            { "mask": "CRACKLE", "combine": "MULTIPLY", "space": "body", "seed": "$petalSeed", "spacing": "$petalSpacing", "jitter": "$petalJitter", "stretch": "$petalStretch", "warp": "$petalWarp", "field": "wallDistance", "from": 1.6, "to": 4.0, "softness": 0.12 }
          ],
          "op": { "color": "#F7E3E0" }
        },
        {
          "masks": [
            { "mask": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            { "mask": "CRACKLE", "combine": "MULTIPLY", "space": "body", "seed": "$petalSeed", "spacing": "$petalSpacing", "jitter": "$petalJitter", "stretch": "$petalStretch", "warp": "$petalWarp", "field": "wallDistance", "from": 1.6, "to": 4.0, "softness": 0.12 },
            { "mask": "FRACTAL", "combine": "MULTIPLY", "shape": "ridged", "space": "body", "seed": "$veinSeed", "scale": 1.1, "octaves": 3, "warp": 0.3, "threshold": 0.8, "softness": 0.06 }
          ],
          "op": { "color": "#C2416B" }
        },
        {
          "masks": [
            { "mask": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            { "mask": "CRACKLE", "combine": "MULTIPLY", "space": "body", "seed": "$petalSeed", "spacing": "$petalSpacing", "jitter": "$petalJitter", "stretch": "$petalStretch", "warp": "$petalWarp", "field": "wallDistance", "from": 0.55, "to": 1.6, "softness": 0.12 }
          ],
          "op": { "color": "#141414" }
        },
        {
          "masks": [
            { "mask": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            { "mask": "CRACKLE", "combine": "MULTIPLY", "space": "body", "seed": "$petalSeed", "spacing": "$petalSpacing", "jitter": "$petalJitter", "stretch": "$petalStretch", "warp": "$petalWarp", "field": "wallDistance", "from": 0.0, "to": 0.55, "softness": 0.12 }
          ],
          "op": { "color": "#A52F52" }
        }
      ],
      "notes": [
        "All four layers reuse the identical petalSeed/petalSpacing/petalJitter/petalStretch/petalWarp values on the same wallDistance field, only sliding the from/to band. This is required, not stylistic: any drift between layers would open seams or double-paint at the cell walls.",
        "The from=0.55 and from=1.6 boundaries carry the same softness (0.12) on both sides of the seam they share, so the rose-to-black and black-to-fill transitions blend at a single matched rate instead of one side falling off faster than the other."
      ]
    },
    {
      "description": "No petal marking; the coat expresses the horse's base colour and pattern unmodified.",
      "layers": []
    }
  ],
  "founders": { "rarity": "RARE" },
  "blurb": "A rare magical marking that breaks the coat into rose- and black-rimmed petal shapes, like an overlapping flower caught mid-bloom, veined in dark rose across a pale blush field.",
  "notes": [
    "Scoped to BODY, NECK, HEAD and MUZZLE only - a 'flower blanket' rather than full coverage. MANE, TAIL, EARS and the legs were left bare so the hair and hooves keep reading as hair and hooves against the petaled skin.",
    "CRACKLE is the wall-based tessellation the description calls for (adjoining blobs with a defined boundary between them, not distance-to-centre blobs), but its natural walls are straight polygon facets. The petalWarp knob is used here exactly as warp is used elsewhere to make a wandering edge: it bends those facets into rounded, organic lobes. This is a tuning composition, not a literal petal silhouette - the cells still carry a multi-sided Voronoi character under close inspection - but at coat scale it reads as overlapping petals, which is what the reference calls for.",
    "Vein scale and warp are fixed structural constants rather than knobs; only veinSeed is heritable, so vein placement varies horse to horse without letting vein density or thickness drift into a wash (rule against strokes/lace washing out)."
  ]
}
```