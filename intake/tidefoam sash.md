```json
{
  "id": "tidefoam_sash",
  "name": "Tidefoam Sash",
  "phase": "magical",
  "blurb": "A sinuous sash of deep tidewater teal sweeps diagonally across the barrel, its surface laced with a fine white net of foam-cell lines and the occasional larger bubble, as if the horse waded through sea-foam that never quite dried.",
  "alleles": [
    {
      "id": "n",
      "name": "Clear",
      "description": "No tidefoam marking."
    },
    {
      "id": "W",
      "name": "Tidefoam",
      "knobs": [
        { "name": "seed", "type": "seed" },
        { "name": "cellSize", "type": "float", "default": 3.4, "min": 2.6, "max": 4.4 },
        { "name": "wobble", "type": "float", "default": 2.4, "min": 1.6, "max": 3.2 }
      ]
    }
  ],
  "expressions": [
    {
      "when": ["W/W", "W/n", "n/W"],
      "description": "A diagonal sash of marbled sea-teal crosses the ribcage, its surface broken by a fine white network of foam-cell lines with occasional larger bubble-like voids.",
      "layers": [
        {
          "name": "water_fill",
          "masks": [
            {
              "type": "PATH",
              "space": "body",
              "parts": ["BARREL"],
              "points": [
                { "u": 0.68, "v": 0.60 },
                { "u": 0.50, "v": 0.50 },
                { "u": 0.32, "v": 0.42 },
                { "u": 0.20, "v": 0.34 }
              ],
              "width": 6.5,
              "softness": 1.6,
              "warp": { "seed": "$seed", "amplitude": "$wobble", "scale": 4.0 }
            }
          ],
          "op": {
            "type": "RAMP",
            "driver": {
              "type": "FRACTAL",
              "space": "body",
              "parts": ["BARREL"],
              "seed": "$seed",
              "octaves": 3,
              "scale": 5.0,
              "warp": { "seed": "$seed", "amplitude": 1.5, "scale": 3.0 }
            },
            "stops": [
              { "at": 0.0, "color": "#0d4f4d" },
              { "at": 0.5, "color": "#1f8a86" },
              { "at": 1.0, "color": "#5fd1c9" }
            ]
          }
        },
        {
          "name": "foam_network",
          "masks": [
            {
              "type": "PATH",
              "space": "body",
              "parts": ["BARREL"],
              "points": [
                { "u": 0.68, "v": 0.60 },
                { "u": 0.50, "v": 0.50 },
                { "u": 0.32, "v": 0.42 },
                { "u": 0.20, "v": 0.34 }
              ],
              "width": 6.5,
              "softness": 1.6,
              "warp": { "seed": "$seed", "amplitude": "$wobble", "scale": 4.0 }
            },
            {
              "type": "CRACKLE",
              "space": "body",
              "parts": ["BARREL"],
              "seed": "$seed",
              "spacing": "$cellSize",
              "lineWidth": { "perDose": [0.55, 0.8] },
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "paint",
            "color": "#f4efe2"
          }
        },
        {
          "name": "foam_bubbles",
          "masks": [
            {
              "type": "PATH",
              "space": "body",
              "parts": ["BARREL"],
              "points": [
                { "u": 0.68, "v": 0.60 },
                { "u": 0.50, "v": 0.50 },
                { "u": 0.32, "v": 0.42 },
                { "u": 0.20, "v": 0.34 }
              ],
              "width": 6.5,
              "softness": 1.6,
              "warp": { "seed": "$seed", "amplitude": "$wobble", "scale": 4.0 }
            },
            {
              "type": "SPOTS",
              "space": "body",
              "parts": ["BARREL"],
              "seed": "$seed",
              "spacing": { "perDose": [8.5, 6.0] },
              "radius": 1.8,
              "vary": 0.4,
              "softness": 0.6,
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "paint",
            "color": "#f4efe2"
          }
        }
      ],
      "notes": [
        "lineWidth and bubble spacing carry perDose values ([het, hom]) so a W/W horse reads with a bolder, busier foam network than a W/n horse, without changing the underlying shape.",
        "All three layers recompute the same PATH region as their first mask rather than sharing one region, because a layer cannot see anything an earlier layer of this gene painted (rule 8); recomputing is cheap and keeps each layer self-contained."
      ]
    },
    {
      "description": "The coat carries no tidefoam marking."
    }
  ],
  "founders": {
    "rarity": "RARE"
  },
  "notes": [
    "This marking's white foam network and teal water fill are two components of one inseparable motif, not alternate forms of the same shape, so it ships as a single dominant marking allele (W) over a clear recessive (n) instead of the usual white-form/coloured-form pair.",
    "The water colour is fixed by concept (sea teal-to-cyan) rather than a $hue knob; per-horse variety comes from the FRACTAL driver's seed instead, which keeps the identity of the marking as \"water\" intact across all carriers.",
    "The fill is produced as a single RAMP layer driven directly by a FRACTAL noise field, rather than a base paint layer followed by a darken/lighten layer, because the second layer could not have seen the first layer's paint anyway (rule 8) - driving the ramp off the noise directly gets marbled colour in one paint step.",
    "The foam network uses CRACKLE rather than DAPPLES/SPOTS because the reference is defined by the walls between cells, not by distance to a cell centre.",
    "Individual foam cells in the reference each show their own small radial swirl centred in that cell. This file approximates that with one continuous FRACTAL-driven marbling field across the whole sash instead, since nothing in the current mask set can address a decoration to a specific CRACKLE cell's own centroid. See the tool-gap note below."
  ]
}
```

ELEMENT: the small concentric radial swirl/highlight visible inside individual foam cells in the reference, each one centred on its own cell rather than shared across the sash.

SHIPPED: one shared FRACTAL-driven RAMP producing continuous marbled colour across the whole ribbon (the `water_fill` layer), overlaid afterward by CRACKLE cell walls that are geometrically unrelated to the marbling's own noise field. A viewer will see flowing, continuous marbling that ignores the cell boundaries, instead of each foam cell holding its own distinct, self-centred whirl the way the reference does.

WHY NOT: getting a per-cell radial highlight requires knowing, at a given texel, the centroid of the specific CRACKLE cell that texel sits in, then measuring distance to that centroid. CRACKLE only ever outputs distance-to-boundary; RINGS and SPOTS lay down their own independently seeded centres unrelated to CRACKLE's tessellation. No combination of existing masks can align a second mask's centres to CRACKLE's arbitrary, noise-generated polygon partition - it isn't a tuning gap, it's a different measurement (cell-relative distance vs. wall distance) that nothing exposes.

PROPOSAL: CRACKLE.cellMeasure - kind: enum ["wall","centroid"], default "wall". At a texel, "wall" measures normalized distance to the nearest cell boundary (current behaviour); "centroid" instead measures normalized distance from that texel to its own cell's centroid. Coverage 0 under "centroid" means the texel sits at its cell's centre; coverage 1 means it sits on that cell's boundary. Closest existing mask: CRACKLE itself, using the same tessellation - the one thing this adds is a per-cell radial measurement that no other mask can produce, since SPOTS/DAPPLES centres are never aligned to CRACKLE's polygons.