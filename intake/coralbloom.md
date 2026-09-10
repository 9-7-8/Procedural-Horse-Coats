```json
{
  "id": "coral_bloom",
  "name": "Coral Bloom",
  "category": "magical",
  "locus": "CRB",
  "alleles": ["CRB", "crb"],
  "knobs": [
    { "name": "cellSeed", "type": "seed" },
    { "name": "speckleSeed", "type": "seed" },
    { "name": "cellSpacing", "type": "float", "default": 7.0, "min": 5.5, "max": 9.5 },
    { "name": "cellJitter", "type": "float", "default": 0.42, "min": 0.3, "max": 0.55 },
    { "name": "cellStretch", "type": "float", "default": 1.3, "min": 1.1, "max": 1.6 },
    { "name": "cellWarp", "type": "float", "default": 0.55, "min": 0.35, "max": 0.75 }
  ],
  "expressions": [
    {
      "when": ["CRB/CRB", "CRB/crb"],
      "description": "The coat over the body, neck, head and muzzle breaks into overlapping ruffled lobes, each rimmed in bright orange, shading from deep sea-blue near the rim to a pale blue crown at its center, and flecked all over with tiny gold specks.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$cellSeed",
              "spacing": "$cellSpacing", "jitter": "$cellJitter",
              "stretch": "$cellStretch", "warp": "$cellWarp",
              "field": "wallDistance", "from": 0.0, "to": 0.6, "softness": 0.12
            }
          ],
          "op": { "type": "COLOR", "color": "#E8791E" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$cellSeed",
              "spacing": "$cellSpacing", "jitter": "$cellJitter",
              "stretch": "$cellStretch", "warp": "$cellWarp",
              "field": "wallDistance", "from": 0.6, "to": 1.8, "softness": 0.12
            }
          ],
          "op": { "type": "COLOR", "color": "#1E3E9E" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$cellSeed",
              "spacing": "$cellSpacing", "jitter": "$cellJitter",
              "stretch": "$cellStretch", "warp": "$cellWarp",
              "field": "wallDistance", "from": 1.8, "to": 4.0, "softness": 0.15
            }
          ],
          "op": { "type": "COLOR", "color": "#8FC3EF" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$cellSeed",
              "spacing": "$cellSpacing", "jitter": "$cellJitter",
              "stretch": "$cellStretch", "warp": "$cellWarp",
              "field": "wallDistance", "from": 0.6, "to": 4.0, "softness": 0.12
            },
            {
              "type": "SPOTS", "combine": "MULTIPLY",
              "space": "body", "seed": "$speckleSeed",
              "spacing": 1.3, "radius": 0.28, "vary": 0.5, "softness": 0.15
            }
          ],
          "op": { "type": "COLOR", "color": "#F7B23B" }
        }
      ]
    },
    {
      "description": "No coral marking; the coat expresses the horse's base colour and pattern unmodified.",
      "layers": []
    }
  ],
  "founders": { "rarity": "RARE" },
  "blurb": "A rare magical marking that ruffles the coat into overlapping coral-like lobes, each edged in bright orange and shading from deep sea-blue to a pale crown, scattered with fine gold flecks.",
  "notes": [
    "Scoped to BODY, NECK, HEAD and MUZZLE - a skin marking, not a hair one - so MANE, TAIL, EARS and the legs stay bare and keep reading as hair and hooves rather than coral.",
    "All four layers reuse the identical cellSeed/cellSpacing/cellJitter/cellStretch/cellWarp values on the same wallDistance field, only sliding the from/to band, exactly as required for the walls to line up rather than open seams or double-paint.",
    "cellWarp bends CRACKLE's naturally straight polygon facets into the rounded, ruffled lobes the reference shows - a tuning composition, not a literal frill silhouette; the underlying tessellation is still a multi-sided Voronoi cell under close inspection, but at coat scale it reads as overlapping coral folds.",
    "The rim/fill boundary (0.6) and fill/highlight boundary (1.8) share matched or near-matched softness on both sides of each seam so the orange-to-blue and blue-to-pale transitions blend at a consistent rate rather than one side falling off faster.",
    "No hue knob: the orange-rim/blue-fill/gold-speck palette is the coral concept itself, not a per-horse variable - a player wanting a different palette should look at a different marking gene.",
    "Speckle spacing and radius are fixed structural constants rather than knobs; only speckleSeed is heritable, keeping the fleck density from drifting into a wash per the strokes/spacing rule."
  ]
}
```