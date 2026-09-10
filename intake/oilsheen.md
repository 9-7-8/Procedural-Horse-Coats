```json
{
  "id": "oil_sheen",
  "name": "Oil Sheen",
  "blurb": "A rare enchantment leaves the coat looking like a soap film caught in light: a cracked, iridescent web of veins runs the length of the horse, pooling over the barrel into a dense, bubble-pitted slick that shifts color from tail to poll.",
  "phase": "magical",
  "rarity": "LEGENDARY",
  "knobs": {
    "hueShift": { "type": "number", "min": 0, "max": 360 },
    "satBase": { "type": "number", "min": 0.16, "max": 0.34 },
    "veinSeed": { "type": "seed" },
    "veinScale": { "type": "number", "min": 3.2, "max": 6.5 },
    "veinJitter": { "type": "number", "min": 0.4, "max": 0.6 },
    "blobSeed": { "type": "seed" },
    "blobRadius": { "type": "number", "min": 6.5, "max": 9.5 },
    "bubbleSeed": { "type": "seed" },
    "bubbleSpacing": { "type": "number", "min": 1.6, "max": 2.4 }
  },
  "alleles": [
    { "code": "S", "description": "Oil Sheen — dominant magical marking." },
    { "code": "n", "description": "Normal coat, no oil sheen." }
  ],
  "expressions": [
    {
      "when": ["S/S", "S/n"],
      "description": "The coat is laced with a cracked, glassy web of colour running nose to tail, densest and almost solid over the barrel, where it is pitted with bubbles of every size — some no bigger than a pinhead, a few as wide as a coin, each rimmed with a thread of brighter colour.",
      "layers": [
        {
          "name": "veins",
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            {
              "type": "CRACKLE",
              "space": "body",
              "seed": "$veinSeed",
              "cellSize": "$veinScale",
              "lineWidth": 1.1,
              "softness": 0.35,
              "jitter": "$veinJitter",
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "RAMP",
            "space": "body",
            "axis": "x",
            "hueRotate": "$hueShift",
            "from": { "hue": 210, "sat": "$satBase", "light": 0.34 },
            "to": { "hue": 55, "sat": "$satBase", "light": 0.58 }
          }
        },
        {
          "name": "barrel_slick_with_bubbles",
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "SPOTS",
              "space": "body",
              "seed": "$blobSeed",
              "spacing": 40,
              "radius": "$blobRadius",
              "softness": 3.0,
              "combine": "MULTIPLY"
            },
            {
              "type": "SPOTS",
              "space": "body",
              "seed": "$bubbleSeed",
              "spacing": "$bubbleSpacing",
              "radius": 0.5,
              "vary": 0.85,
              "softness": 0.12,
              "combine": "SUBTRACT"
            }
          ],
          "op": {
            "type": "RAMP",
            "space": "body",
            "axis": "y",
            "hueRotate": "$hueShift",
            "from": { "hue": 275, "sat": "$satBase", "light": 0.30 },
            "to": { "hue": 150, "sat": "$satBase", "light": 0.50 }
          }
        },
        {
          "name": "bubble_rims",
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "RINGS",
              "space": "body",
              "seed": "$bubbleSeed",
              "spacing": "$bubbleSpacing",
              "radius": 0.5,
              "vary": 0.85,
              "bandWidth": 0.18,
              "softness": 0.08,
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "RAMP",
            "space": "body",
            "axis": "y",
            "hueRotate": "$hueShift",
            "from": { "hue": 190, "sat": 0.40, "light": 0.62 },
            "to": { "hue": 40, "sat": 0.42, "light": 0.70 }
          }
        }
      ],
      "notes": [
        "S/S and S/n are given one expression on purpose: a dose-thinned version of this pattern (half the veins, or veins with no barrel slick) reads as a rendering error rather than a fainter magical coat, so the marking is written fully dominant rather than perDose.",
        "The three layers share $bubbleSeed, $bubbleSpacing and the same vary=0.85 across the SPOTS (holes) and RINGS (rims) masks so that every rim sits exactly on the hole it belongs to and scales with it — the same one-seed-many-masks trick used for concentric zones, applied here to keep a hole and its highlight locked together instead of concentric."
      ]
    },
    {
      "description": "No oil sheen; the coat is unaffected.",
      "layers": []
    }
  ],
  "notes": [
    "Anatomy does most of the 'spikey' work for free: BARREL is the dense hub, and NECK/HEAD/MUZZLE/MANE/TAIL/legs are already the radiating limbs, so the vein layer only needs an isotropic cellular field over PARTS:ALL rather than any hand-built branch shape.",
    "CRACKLE is used for the veins because the reference is a foam/soap-film membrane — a planar network of walls enclosing empty cells — which is exactly the wall-between-cells shape CRACKLE exists for; cellSize is kept to several units (3.2-6.5, against an 11-22 unit barrel/leg run) so each part shows a handful of cells rather than a wash, and lineWidth (1.1) is kept well under cellSize per the stroke-width rule.",
    "The barrel's dense solid-looking mass is one oversized SPOTS instance (spacing 40, bigger than the group) rather than a new 'blob' primitive — same tool as the bubbles, just tuned to produce a single cell instead of a field of them.",
    "RAMP is only ever driven off a literal body-space axis (x for the veins, y for the barrel and rims) here, which gives a one-directional colour sweep rather than the swirling, non-monotonic iridescence in the reference; see TOOL GAP below.",
    "hueRotate is the one knob shared by all three RAMP ops so a horse's whole coat rotates through the spectrum together instead of each layer picking an independent, mismatched rainbow."
  ]
}
```

ELEMENT: the swirling, non-monotonic colour shift of the oil-slick surface (the reference's hue wanders back and forth across the membrane rather than sweeping once from one end to the other).
SHIPPED: three RAMP ops, each keyed to a single literal body-space axis (x for the veins, y for the barrel/rims) — a coat that reddens or blues steadily from tail to nose and bottom to top, with no local wandering; a viewer would see one clean gradient sweep instead of the reference's patchy, doubling-back iridescence.
WHY NOT: RAMP measures position along a chosen literal axis (monotonic by construction); PALETTE measures which lattice cell a point falls in (discrete, hard-edged by construction). Neither measures a continuous, non-monotonic scalar field — there is no coverage-shaped mask output being read as a colour input, only an axis coordinate or a cell index. Getting the reference's wander requires driving colour off a noise value the way SPOTS/CRACKLE/FRACTAL drive coverage off one, which is a different measurement than either existing colour op takes.
PROPOSAL: RAMP gets a new axis option, "axis": "noise", with two added parameters: "seed" (seed, no default — required with this axis) and "scale" (number, default 4.0, body units — the noise's characteristic wavelength). At a texel it measures a smooth pseudo-random scalar in 0..1 built from body-space position (no cell walls, no cell identity, just a continuous field). 0 selects the "from" colour, 1 selects the "to" colour, with every value between continuously blended. Closest existing feature: RAMP itself — this is a new value source for its existing axis parameter, not a new op; the one thing it does that RAMP's current axes (x/y/z/u/v) cannot is vary colour non-monotonically without ever introducing PALETTE's hard lattice edges.