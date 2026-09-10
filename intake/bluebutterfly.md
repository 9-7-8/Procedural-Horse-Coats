```json
{
  "id": "morpho_wing_mark",
  "name": "Morpho Wing",
  "blurb": "A patch of iridescent light gathers low on the haunch, like a wing folded against the flank, cut through with dark bars that rake toward the tail.",
  "alleles": ["W", "w"],
  "knobs": [
    { "name": "hue", "type": "number", "min": 0, "max": 360, "default": 212 }
  ],
  "expressions": [
    {
      "when": ["W/W", "W/w"],
      "description": "A soft silver-white sheen pools low on the haunch, banded with a few dark diagonal bars.",
      "layers": [
        {
          "name": "haunch_glow",
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            {
              "type": "PATH", "combine": "MULTIPLY",
              "space": "body",
              "from": { "u": 0.20, "v": 0.40 },
              "to":   { "u": 0.32, "v": 0.60 },
              "width": 8, "softness": 4
            }
          ],
          "op": { "type": "PAINT", "color": "#E9EAF2", "emissive": true }
        },
        {
          "name": "haunch_slashes",
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            {
              "type": "PATH", "combine": "MULTIPLY",
              "space": "body",
              "from": { "u": 0.20, "v": 0.40 },
              "to":   { "u": 0.32, "v": 0.60 },
              "width": 8, "softness": 4
            },
            {
              "type": "WAVES", "combine": "MULTIPLY",
              "axis": "X", "space": "part", "shape": "saw",
              "wavelength": 2.5, "amplitude": 3.5,
              "from": 0.78, "to": 0.92
            }
          ],
          "op": { "type": "PAINT", "color": "#0A0A0A", "emissive": false }
        }
      ]
    },
    {
      "description": "A patch of deep, glowing blue pools low on the haunch, cut with a few black bars raking toward the tail — a wing folded against the flank.",
      "layers": [
        {
          "name": "haunch_glow",
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            {
              "type": "PATH", "combine": "MULTIPLY",
              "space": "body",
              "from": { "u": 0.20, "v": 0.40 },
              "to":   { "u": 0.32, "v": 0.60 },
              "width": 8, "softness": 4
            }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "sat": 0.85, "val": 0.92, "emissive": true }
        },
        {
          "name": "haunch_slashes",
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            {
              "type": "PATH", "combine": "MULTIPLY",
              "space": "body",
              "from": { "u": 0.20, "v": 0.40 },
              "to":   { "u": 0.32, "v": 0.60 },
              "width": 8, "softness": 4
            },
            {
              "type": "WAVES", "combine": "MULTIPLY",
              "axis": "X", "space": "part", "shape": "saw",
              "wavelength": 2.5, "amplitude": 3.5,
              "from": 0.78, "to": 0.92
            }
          ],
          "op": { "type": "PAINT", "color": "#0A0A0A", "emissive": false }
        }
      ]
    }
  ],
  "founders": { "rarity": "EPIC" },
  "notes": [
    "The glow blob is placed with a single PATH run (u 0.20-0.32, v 0.40-0.60 in body space), which lands entirely inside BODY at x 8.2-13.1 - deliberately the tail-most third of the 22-unit barrel, i.e. the haunch, not the shoulder.",
    "Both the glow layer and the slash layer re-declare the identical PARTS+PATH region rather than one referencing the other's result, per the rule that a layer cannot see what the layer above painted.",
    "The WAVES stripe uses axis X with a Y-scaled amplitude (wavelength 2.5 x tan(54.5 deg) = 3.5) to rake the bars diagonally rather than running them vertically, then narrows each cycle to a thin bar with from 0.78 / to 0.92 rather than letting the full sawtooth ramp show. This reproduces the diagonal, evenly-spaced, straight-edged look of the reference's bars, but see the TOOL GAP below for what it does not reproduce.",
    "Black is fixed rather than hue-linked in both expressions - the reference's bars stay black regardless of the wing's ground color, so the marking's own hue knob only recolors the glow."
  ]
}
```
ELEMENT the black bars, whose true shape fans outward from a point near the body and widens as it goes, rather than running as parallel stripes.
SHIPPED WAVES (axis X, Y-scaled amplitude, saw, narrowed to a band with from/to) over the haunch region — a set of parallel diagonal bars of constant width and constant spacing.
WHY NOT WAVES derives its phase from a linear coordinate (X plus a fixed multiple of Y), so every bar has the same direction and the same spacing everywhere in the region - that is the measurement it takes. The reference's bars share a common origin and their angle and gap both change with distance from it, which needs the phase taken from the angle around a pivot point (atan2 of position relative to that point), a different measurement WAVES has no parameter for; no wavelength, amplitude, or space setting recovers it, since all of those still feed a linear coordinate into the same sawtooth.
PROPOSAL FAN — parameters: origin (point, body-space, no default), spacing (number, body units, default 2.0, angular period measured as arc length at unit distance from origin), twist (number, radians per unit distance, default 0.0, how much the phase's zero-angle rotates per unit distance from origin), from (number, 0-1, default 0.8), to (number, 0-1, default 0.95, must exceed from). At a texel it measures the angle from origin to that texel, divided into cycles of width spacing/distance-from-origin (so bars widen with distance) with a per-distance rotation added by twist; coverage 0 is a texel that falls in a cycle's gap, coverage 1 is a texel at a cycle's peak. Closest existing mask: WAVES, whose linear phase this replaces with an angular one; WAVES cannot widen or converge its bars because its coordinate never depends on distance from a point.