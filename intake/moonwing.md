```json
{
  "id": "moonwing_marking",
  "name": "Moonwing",
  "blurb": "A pearl-white pattern that sweeps across the barrel like a pair of folded wings, dusted with a faint shimmer of blue, violet and pink and rimmed in a thick band of gold. Rare foals are born with the pattern reversed - the wing itself carrying colour instead of white, still trimmed in the same gold.",
  "notes": [
    "Wing silhouette is one large soft-edged SPOTS blob per side, elongated along X and mirrored - not a hand-authored PATH outline. This is a deliberate approximation of the reference's pointed wing shape; it reads as a soft asymmetric wing rather than the crisp scalloped moth silhouette, which is an acceptable trade for staying inside the general-purpose mask set.",
    "Border layer reuses the wing SPOTS mask twice in one fold: inverted+spread (region = grown outside band) times the normal wing mask (MULTIPLY), per the spread-after-invert rule. This carves exactly the ring of `borderWidth` just inside the wing boundary without needing to read anything the base/dust layers painted.",
    "Dust layer's PALETTE is keyed to the DAPPLES lattice from the same fold, so each tiny dot gets one hard-edged hue from the 3-colour set rather than a blended wash - this is what gives the 'very dithered' look instead of a smooth gradient.",
    "The reference's colour-shifting iridescence is a viewing-angle effect; there is no per-angle shader hook in this system, so it is approximated with a static three-hue stipple instead. That is a rendering-pipeline gap, not a mask/op gap, so it is not raised as a TOOL GAP below.",
    "Dust and gold trim stay fixed-colour on both alleles (they are the marking's identity); only the base fill switches to `$hue` on the recessive coloured form, per the white/coloured pairing convention."
  ],
  "alleles": [
    {
      "id": "W",
      "label": "Moonwing (white)",
      "founders": { "rarity": "LEGENDARY" },
      "knobs": [
        { "name": "dustSeed", "type": "seed" }
      ]
    },
    {
      "id": "C",
      "label": "Moonwing (coloured)",
      "founders": { "rarity": "EPIC" },
      "knobs": [
        { "name": "dustSeed", "type": "seed" },
        { "name": "hue", "type": "number", "min": 0, "max": 360 }
      ]
    },
    {
      "id": "n",
      "label": "No moonwing",
      "founders": { "rarity": "COMMON" }
    }
  ],
  "expressions": [
    {
      "when": ["W/W", "W/C", "W/n"],
      "description": "Twin pearl-white patches sweep back across the ribs like folded wings, glimmering with a faint blue-violet-pink dust and bound at the edge by a thick band of gold.",
      "notes": [
        "perDose keys on copies of W (doses 2/1/1 across the three combos, so the table is not a constant): homozygotes get the full-size, full-density wing; heterozygotes a slightly smaller, sparser one."
      ],
      "perDose": {
        "1": { "patchScale": 0.85, "dustDensity": 0.75 },
        "2": { "patchScale": 1.0, "dustDensity": 1.0 }
      },
      "layers": [
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "scale": "$patchScale",
              "mirror": true
            }
          ],
          "op": {
            "type": "PALETTE",
            "colors": ["#F4F0E8"]
          }
        },
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "scale": "$patchScale",
              "mirror": true
            },
            {
              "type": "DAPPLES",
              "radius": 0.35,
              "spacing": 1.1,
              "vary": 0.5,
              "density": "$dustDensity",
              "seed": "$dustSeed",
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "PALETTE",
            "keyedTo": "DAPPLES",
            "colors": ["#BFE0FF", "#D6C2FF", "#FFD1E8"]
          }
        },
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "scale": "$patchScale",
              "invert": true,
              "spread": 1.1
            },
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "scale": "$patchScale",
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "PALETTE",
            "colors": ["#C9932E"]
          }
        }
      ]
    },
    {
      "when": ["C/C"],
      "description": "The same wing pattern, but the field itself carries deep colour instead of white - still dusted with the same faint iridescent shimmer and bound in the same thick gold.",
      "notes": [
        "Recessive to both W and n - only expressed homozygous, so C/n shows no marking at all. Shares the exact wing geometry and border/dust layers with the dominant form; only the base-fill op differs (hue instead of fixed colour)."
      ],
      "layers": [
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "mirror": true
            }
          ],
          "op": {
            "type": "PALETTE",
            "colors": [
              { "hue": "$hue", "saturation": 0.55, "lightness": 0.4 }
            ]
          }
        },
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "mirror": true
            },
            {
              "type": "DAPPLES",
              "radius": 0.35,
              "spacing": 1.1,
              "vary": 0.5,
              "density": 1.0,
              "seed": "$dustSeed",
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "PALETTE",
            "keyedTo": "DAPPLES",
            "colors": ["#BFE0FF", "#D6C2FF", "#FFD1E8"]
          }
        },
        {
          "target": { "group": "BARREL" },
          "masks": [
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "invert": true,
              "spread": 1.1
            },
            {
              "type": "SPOTS",
              "count": 1,
              "center": { "x": 18.5, "y": 17, "z": 2.8 },
              "radius": 7,
              "stretch": { "x": 1.9, "y": 0.8 },
              "softness": 1.4,
              "warp": { "amplitude": 0.6, "scale": 3, "seed": 4177 },
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "PALETTE",
            "colors": ["#C9932E"]
          }
        }
      ]
    },
    {
      "description": "No moonwing marking.",
      "layers": []
    }
  ]
}
```