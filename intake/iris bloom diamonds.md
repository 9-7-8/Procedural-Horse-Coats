```json
{
  "id": "iris_bloom_diamonds",
  "name": "Iris Bloom Diamonds",
  "rarity": "MYTHIC",
  "blurb": "Dark royal-blue diamonds unfold across the barrel like a fan of iris petals, each rimmed in ink and scattered with fine white dapples that fade toward a core of radiating amber veins.",
  "knobs": [
    { "name": "seed", "type": "seed" },
    { "name": "dotSeed", "type": "seed" },
    { "name": "tearSeed", "type": "seed" },
    { "name": "jitter", "type": "number", "default": 0.55 },
    { "name": "insetGap", "type": "number", "default": 0.9 },
    { "name": "insetOutline", "type": "number", "default": 1.6 },
    { "name": "coreRadius", "type": "number", "default": 1.1 },
    { "name": "vary", "type": "number", "default": 0.3 }
  ],
  "alleles": ["B", "n"],
  "expressions": [
    {
      "description": "The coat carries no iris diamonds.",
      "layers": []
    },
    {
      "when": ["B/B", "B/n"],
      "description": "Dark royal-blue diamonds spread across the barrel, denser and more numerous with a second copy of the allele. Each diamond is rimmed in near-black blue, dappled white through its outer two-thirds, and threaded with radiating amber veins at its core.",
      "notes": [
        "spacing is perDose {1: 5.0, 2: 3.0} and that exact object is repeated verbatim on every CRACKLE and core SPOTS mask below - they must stay numerically identical or the diamonds, their rims, and their cores drift apart.",
        "'top 2/3 white / bottom 1/3 orange' from the brief is drawn here as OUTER 2/3 (by radius) white and INNER 1/3 (by radius) orange, radiating out from each diamond's own centre rather than a literal up/down split - there is no per-instance up-axis to read, but each diamond does have a centre, and the reference's orange concentrates toward the flower's throat while white spreads toward the petal tips, which this preserves.",
        "layer order matters: fill, then outline (paints the rim over the fill's edge), then white dapples, then amber core - each later layer only overwrites the small area its own mask actually covers, so nothing upstream needs to be re-painted."
      ],
      "layers": [
        {
          "op": { "type": "PAINT", "color": "#1c2f82" },
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "jitter": "$jitter", "inset": "$insetGap"
            }
          ]
        },
        {
          "op": { "type": "PAINT", "color": "#0d1247" },
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "jitter": "$jitter", "inset": "$insetGap"
            },
            {
              "type": "CRACKLE", "combine": "SUBTRACT",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "jitter": "$jitter", "inset": "$insetOutline"
            }
          ]
        },
        {
          "op": { "type": "PAINT", "color": "#fbfbf6" },
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "jitter": "$jitter", "inset": "$insetGap"
            },
            {
              "type": "SPOTS", "combine": "SUBTRACT",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "radius": "$coreRadius", "vary": "$vary", "softness": 0.25
            },
            {
              "type": "SPOTS", "combine": "MULTIPLY",
              "space": "body", "seed": "$dotSeed",
              "spacing": 0.85, "radius": 0.3, "vary": 0.4, "softness": 0.12
            }
          ]
        },
        {
          "op": { "type": "PAINT", "color": "#e2891f" },
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "CRACKLE", "combine": "MULTIPLY",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "jitter": "$jitter", "inset": "$insetGap"
            },
            {
              "type": "SPOTS", "combine": "MULTIPLY",
              "space": "body", "seed": "$seed",
              "spacing": { "perDose": { "1": 5.0, "2": 3.0 } },
              "radius": "$coreRadius", "vary": "$vary", "softness": 0.25
            },
            {
              "type": "FRACTAL", "combine": "MULTIPLY",
              "shape": "ridged", "seed": "$tearSeed",
              "octaves": 2, "warp": 0.35, "threshold": 0.72, "softness": 0.09
            }
          ]
        }
      ]
    }
  ],
  "notes": [
    "Diamond outline and fill both come from CRACKLE (inset varied twice, same seed+spacing, subtracted) rather than SPOTS, because only WAVES and CRACKLE produce straight edges - SPOTS/DAPPLES are round-only.",
    "The amber core uses FRACTAL shape=ridged for its taper-and-pinch quality, which is the documented way to get pointed, vein-like forms rather than an invented teardrop primitive.",
    "MYTHIC reflects the marking's four-layer complexity and its narrow, deliberately placed visual niche rather than a computed rate; founders.py derives the actual table."
  ]
}
```

ELEMENT: the shared centre that lets the white outer-band and orange core stay concentric with each diamond's own outline.
SHIPPED: a CRACKLE diamond (fill/outline) intersected with a SPOTS mask given the identical seed+spacing, betting that both mask types derive the same jittered point lattice from those two numbers.
WHY NOT: Rule 14's concentric guarantee is proven only for multiple layers of the *same* mask type sharing seed+spacing+stretch+vary; nothing establishes that CRACKLE's Voronoi seeding and SPOTS's own point placement agree on where a "point" from a given seed+spacing actually falls, so a viewer could see the white/orange split drift off-centre from the diamond's rim on some horses.
PROPOSAL: cellsFrom (mask-reference, default null) - new parameter on SPOTS and DAPPLES. When set to another mask in the same layer stack, that mask draws one instance centred at each of the referenced mask's cell centres instead of deriving its own lattice from seed+spacing. Coverage 0 = outside every referenced cell's radius; coverage 1 = at each referenced centre. Closest existing mask: SPOTS itself; the one thing this adds that plain SPOTS cannot: exact, guaranteed alignment to a *different* mask type's placement, not just to another SPOTS layer.