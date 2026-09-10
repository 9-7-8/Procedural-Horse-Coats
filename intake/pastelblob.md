```json
{
  "id": "nacre",
  "name": "Nacre",
  "category": "magical_marking",
  "blurb": "Nacre breaks the coat into hard-edged plates like a cracked shell, each one paler near its gold seams and richer toward its center. One copy of the pale form is enough to show it; without one, the same cracked plating turns up in a single saturated color instead of pastel-on-pastel.",
  "knobs": [
    { "name": "cellSeed", "type": "seed" },
    { "name": "cellSpacing", "type": "number", "min": 6, "max": 12, "default": 9 },
    { "name": "cellJitter", "type": "number", "min": 0.4, "max": 0.75, "default": 0.6 },
    { "name": "crackWidth", "type": "number", "min": 0.4, "max": 0.8, "default": 0.6 },
    { "name": "hue", "type": "number", "min": 0, "max": 360, "default": 275 }
  ],
  "alleles": ["opal", "glaze"],
  "expressions": [
    {
      "id": "opal_nacre",
      "when": ["opal/opal", "opal/glaze"],
      "description": "The coat breaks into pale, irregular plates of blush pink and sky blue, each deepening toward a flamingo-pink heart, all of it seamed with fine cracks of pale gold — a horse that looks carved from mother-of-pearl.",
      "rarity": "EPIC",
      "notes": [
        "DAPPLES reads distance-to-cell-center on the same lattice as the PALETTE plates, so its coverage is highest mid-plate and falls off toward the crack — that is the 'darkening to a flamingo pink ... inside' read on the reference: pale near the seam, richer toward the middle of each plate."
      ],
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] }
          ],
          "op": {
            "type": "PALETTE",
            "space": "body",
            "seed": "$cellSeed",
            "spacing": "$cellSpacing",
            "jitter": "$cellJitter",
            "colors": ["#F7D6E3", "#DCE7F7"]
          }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            {
              "type": "DAPPLES",
              "space": "body",
              "seed": "$cellSeed",
              "spacing": "$cellSpacing",
              "jitter": "$cellJitter",
              "softness": 0.35,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "color", "color": "#EE6FA0" },
          "perDose": { "1": 0.55, "2": 1.0 }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            {
              "type": "CRACKLE",
              "space": "body",
              "seed": "$cellSeed",
              "spacing": "$cellSpacing",
              "jitter": "$cellJitter",
              "mode": "edge",
              "width": "$crackWidth",
              "softness": 0.15,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "color", "color": "#F4E48A" }
        }
      ]
    },
    {
      "id": "glazed_nacre",
      "description": "The same cracked, plated coat, but every plate and its darkened core run in a single rich color instead of pastel plate on pastel plate — a horse that looks poured from stained glass rather than shell.",
      "rarity": "LEGENDARY",
      "notes": [
        "Same three layers, same lattice knobs, as opal_nacre; only the PALETTE colors and the core color op swap explicit hex for hue:$hue, per convention — the plating and the seams are unchanged, only the pigment source is."
      ],
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] }
          ],
          "op": {
            "type": "PALETTE",
            "space": "body",
            "seed": "$cellSeed",
            "spacing": "$cellSpacing",
            "jitter": "$cellJitter",
            "colors": [
              { "hue": "$hue", "lightness": 0.86 },
              { "hue": "$hue", "lightness": 0.76 }
            ]
          }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            {
              "type": "DAPPLES",
              "space": "body",
              "seed": "$cellSeed",
              "spacing": "$cellSpacing",
              "jitter": "$cellJitter",
              "softness": 0.35,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "hue", "hue": "$hue", "lightness": 0.45 }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            {
              "type": "CRACKLE",
              "space": "body",
              "seed": "$cellSeed",
              "spacing": "$cellSpacing",
              "jitter": "$cellJitter",
              "mode": "edge",
              "width": "$crackWidth",
              "softness": 0.15,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "color", "color": "#F4E48A" }
        }
      ]
    }
  ],
  "notes": [
    "All three layers in each expression open on PARTS parts:['ALL'] so each is independently valid at full coverage across the whole model; the crack seam, the color plates, and the core-darkening all key off the same $cellSeed/$cellSpacing/$cellJitter triple, which is what keeps the gold seams sitting exactly on the plate boundaries and the flamingo (or hue) core exactly centered in each plate. No cell-tracking mask exists in this format, so alignment is achieved by every layer independently deriving the same tessellation from identical knobs, not by a shared field between layers — layer 2 cannot see what layer 1 painted (Rule: a layer cannot see what the layer above it painted), only what layer 1's knobs would also produce.",
    "PALETTE's hard walls are usually a hazard, but here they are the point: the plate edges are supposed to be sharp, since that is exactly where the CRACKLE seam is drawn on top.",
    "space: body is used on DAPPLES and CRACKLE (and implicitly by PALETTE's own lattice) so the tessellation is computed in one continuous whole-horse frame; the neck, head, mane, and tail are all pitched parts and would otherwise re-derive the lattice in their own local frames and produce a seam at every part boundary.",
    "perDose on opal_nacre's core layer is keyed by copy-count string ('1'/'2') rather than array index, since an index-0 read on a two-count expression is exactly the silent-zero bug this format warns against; the heterozygote shows the same plates at the same spacing, only a fainter core."
  ]
}
```