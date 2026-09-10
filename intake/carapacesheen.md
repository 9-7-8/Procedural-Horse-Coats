```json
{
  "id": "carapace_sheen",
  "name": "Carapace Sheen",
  "blurb": "A shimmering carapace-like sheen washes over the coat, deepest along the spine and fading to a pale glow at the legs, crowned by a bright ridge line and a trio of pale blazes down each flank.",
  "knobs": [
    { "name": "hue", "type": "float", "range": [0, 360], "default": 195 },
    { "name": "seed", "type": "seed" }
  ],
  "alleles": [
    { "id": "S", "name": "Sheen" },
    { "id": "n", "name": "Plain" }
  ],
  "expressions": [
    {
      "when": ["S/S", "S/n"],
      "perDose": {
        "1": { "wash_sat_top": 0.55, "wash_sat_bottom": 0.35, "spot_opacity": 0.6 },
        "2": { "wash_sat_top": 0.85, "wash_sat_bottom": 0.5, "spot_opacity": 1.0 }
      },
      "description": "The coat gleams like a beetle's shell - deep, glassy blue along the back fading to a paler sheen at the legs, with a bright stripe tracing the spine and three pale blazes marking each flank.",
      "layers": [
        {
          "mask": [
            { "type": "PARTS", "parts": ["ALL"] }
          ],
          "op": {
            "type": "RAMP",
            "space": "body",
            "axis": "y",
            "from": 0,
            "to": 33.7,
            "colorFrom": { "hue": "$hue", "sat": "$wash_sat_top", "light": 0.3 },
            "colorTo": { "hue": "$hue", "sat": "$wash_sat_bottom", "light": 0.68 }
          }
        },
        {
          "mask": [
            { "type": "PATH", "space": "body", "path": "spine", "width": 1.2, "softness": 0.4 }
          ],
          "op": {
            "type": "PAINT",
            "color": "white",
            "opacity": 1.0
          }
        },
        {
          "mask": [
            {
              "type": "SPOTS",
              "parts": ["BARREL"],
              "count": 3,
              "spacing": 7,
              "size": 3.5,
              "sizeVariance": 0.25,
              "softness": 0.35,
              "mirror": true,
              "seed": "$seed"
            }
          ],
          "op": {
            "type": "PAINT",
            "color": "white",
            "opacity": "$spot_opacity"
          }
        }
      ],
      "notes": [
        "perDose only touches saturation and spot opacity, never hue, so a single-copy horse reads as a washed-out version of the same colour rather than a different colour.",
        "Spot spacing of 7 units against a 22-unit barrel and a 3.5-unit spot size was chosen so three spots land evenly without crowding the barrel's front or back edge."
      ]
    },
    {
      "description": "A perfectly ordinary coat, with no hint of carapace shimmer.",
      "layers": [],
      "notes": []
    }
  ],
  "founders": {
    "rarity": "RARE"
  },
  "notes": [
    "The vertical RAMP (y 0-33.7, body space) is doing double duty: it is the literal reading of 'deep blue along the top line, light blue along the extremities' from the brief, and it is also this file's stand-in for the reference photo's true iridescence, which is view-angle dependent and out of reach of any position-based mask - see the TOOL GAP note shipped with this file.",
    "Layer order is wash, then spine stripe, then spots, purely for the paint stack - none of the three masks reads position dependent on a prior layer's colour, so this ordering is a presentation choice, not a rule-8 workaround.",
    "SPOTS 'mirror': true with count 3 gives exactly three spots per flank (six total), matching the reference's three splotches per elytron without having to hand-place coordinates on both sides.",
    "Topline and spots are both literal white per the brief, even though the source photo's dorsal stripe reads gold-green - that is a deliberate simplification to the stated description, not an oversight."
  ]
}
```

ELEMENT: the iridescent, angle-shifting sheen across the elytra (green/gold/blue/violet depending on viewing angle).
SHIPPED: a static vertical RAMP (deep saturated blue at the topline, paling toward the legs) standing in for it - a viewer would see a fixed light-to-dark blue gradient that never shifts colour as the camera or horse turns, where the reference shows the hue itself sliding through green-gold-violet with viewing angle.
WHY NOT: every mask in the table (PARTS, PATH, SPOTS, DAPPLES, RINGS, WAVES, CRACKLE, FRACTAL, EDGE, CHOICE, STROKES, PIGMENT, LUMA) resolves coverage from position, model geometry, or already-resolved colour - none reads the surface's facing direction relative to a light or camera, which is the actual measurement true iridescence is a function of. No amount of retuning a position-based RAMP produces a value that changes as the viewer moves; that's a different input, not a different setting.
PROPOSAL: NORMAL - axis: enum{x,y,z} (default "y") - the reference direction to project the surface normal onto; from: float (default -1.0) and to: float (default 1.0) - the normal-dot-axis values mapped to coverage 0 and 1; space: enum{model, body} (default "body") - frame the axis is expressed in. At a texel it takes the surface normal there, dots it with the chosen axis, and smoothsteps that between from/to into coverage - coverage 0 where the surface faces away from the axis, 1 where it faces along it - so a colour run through it reads as a structural sheen tied to how the shell is curved rather than to where a point sits on the body. Closest existing mask is EDGE, which also questions the model rather than position; the one thing EDGE can't do is give continuous coverage across open, non-edge faces from a facing direction, which is exactly what a shell-curvature shimmer needs.