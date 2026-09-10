```json
{
  "id": "gill_bloom",
  "symbol": "Gb",
  "category": "magical_marking",
  "rarity": "EPIC",
  "blurb": "A fungal-touched marking: one or two irregular fan-shaped patches on the barrel, each rimmed in a band of dull gold and hollowed out to a deep maroon interior veined like the underside of a gilled mushroom cap.",
  "alleles": ["N", "gb"],
  "knobs": [
    { "name": "patchSeed", "type": "seed" },
    { "name": "patchScale", "type": "number", "default": 6.5, "min": 4.5, "max": 9 },
    { "name": "patchWarp", "type": "number", "default": 0.45, "min": 0.2, "max": 0.7 },
    { "name": "veinSeed", "type": "seed" },
    { "name": "veinWarp", "type": "number", "default": 0.5, "min": 0.3, "max": 0.75 }
  ],
  "expressions": [
    {
      "description": "No gill-bloom marking present.",
      "layers": []
    },
    {
      "when": { "gb": 2 },
      "description": "One to a few irregular fan-shaped patches spread across the barrel, each with a matte gold rim opening onto a deep maroon center threaded with dark, faintly branching veins radiating out from within.",
      "notes": [
        "Rim and center reuse the exact same FRACTAL field (same seed/scale/warp) at two threshold values. Because threshold cuts the same noise field, the higher-threshold (center) region is a strict subset of the lower-threshold (rim) region, which is what keeps the rim a consistent visible band rather than something that has to be hand-aligned to the center shape.",
        "Vein layer is a ridged FRACTAL confined to the center mask. This is an approximation of a radial gill pattern converging on one point per patch; see TOOL GAP below for what it does not achieve and why.",
        "No hue knob on rim or center: the gold/maroon pairing is the concept, not a per-horse variable. A player wanting a different palette should look at a different marking gene rather than this one growing a hue slider.",
        "patchScale is deliberately several body units (4.5-9) per the sizing rule; at barrel scale (22 units long) this yields one to three lobes, matching the reference's two overlapping caps without hardcoding a lobe count noise can't guarantee anyway."
      ],
      "layers": [
        {
          "name": "rim",
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "FRACTAL",
              "space": "body",
              "octaves": 1,
              "scale": "$patchScale",
              "warp": "$patchWarp",
              "seed": "$patchSeed",
              "threshold": 0.55,
              "softness": 0.06,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "COLOR", "color": "#E7A83A" }
        },
        {
          "name": "center",
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "FRACTAL",
              "space": "body",
              "octaves": 1,
              "scale": "$patchScale",
              "warp": "$patchWarp",
              "seed": "$patchSeed",
              "threshold": 0.7,
              "softness": 0.05,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "COLOR", "color": "#5A1420" }
        },
        {
          "name": "veins",
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "FRACTAL",
              "space": "body",
              "octaves": 1,
              "scale": "$patchScale",
              "warp": "$patchWarp",
              "seed": "$patchSeed",
              "threshold": 0.7,
              "softness": 0.05,
              "combine": "MULTIPLY"
            },
            {
              "type": "FRACTAL",
              "space": "body",
              "shape": "ridged",
              "octaves": 3,
              "scale": 3.0,
              "warp": "$veinWarp",
              "seed": "$veinSeed",
              "threshold": 0.7,
              "softness": 0.1,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "COLOR", "color": "#230810" }
        }
      ]
    }
  ]
}
```

ELEMENT the dark grooves inside each cap, which in the reference converge on one off-center point per lobe like spokes of a fan.
SHIPPED a ridged FRACTAL confined to the center mask, giving dark branching, tapering lines through the maroon field.
WHY NOT ridged FRACTAL measures noise value at a position and thresholds it, same as every other mask here; nothing in the table measures angle-around-a-point. A viewer sees veins that fork and drift like cracks or roots, never lines that all meet at one shared origin the way real gills do — the shape is qualitatively different, not just under-tuned, because no available mask can ask "what angle is this texel from point P."

PROPOSAL
WAVES.space: enum {"cartesian","polar"}, default "cartesian". When "polar", WAVES reads angle-from-origin in place of position-along-axis, and wavelength is degrees instead of body units; companion params WAVES.originX, WAVES.originY: number, body units, default 0, the point angles are measured around. At a texel, coverage 0/1 marks trough/crest of that angular wave exactly as today's linear wave does along an axis. Closest existing mask: WAVES itself — this adds the one thing it can't do now, repeating evenly around a point instead of along a line, which is what a spoke, sunburst, or gill pattern needs.