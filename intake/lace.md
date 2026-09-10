```json
{
  "id": "lace_vein",
  "phase": "magical",
  "name": "Lace",
  "alleles": [
    { "symbol": "V", "name": "Lace" },
    { "symbol": "v", "name": "Plain" }
  ],
  "blurb": "A rare magical marking that etches fine branching lace across the coat, the veins darkening from jewel-toned blooms rooted at the withers and poll.",
  "founders": {
    "rarity": "RARE"
  },
  "knobs": [
    { "name": "seed", "type": "seed" },
    { "name": "hue", "type": "float", "range": [0, 360], "default": 275 },
    { "name": "density", "type": "float", "range": [0.62, 0.78], "default": 0.70 }
  ],
  "expressions": [
    {
      "when": ["V/V"],
      "description": "Dense black lace forks and tapers across the body, neck, head and muzzle, rooted in two deep jewel-toned blooms at the withers and poll.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            { "type": "FRACTAL", "shape": "ridged", "space": "body", "seed": "$seed", "octaves": 3, "warp": 0.12, "threshold": "$density", "softness": 0.05, "combine": "MULTIPLY" }
          ],
          "op": { "op": "paint", "color": "#151016" }
        },
        {
          "masks": [
            { "type": "PATH", "space": "body", "path": [ { "u": 0.55, "v": 0.60 }, { "u": 0.58, "v": 0.68 } ], "width": 2.5, "softness": 1.2 }
          ],
          "op": { "op": "paint", "hue": "$hue", "saturation": 0.55, "lightness": 0.14 }
        },
        {
          "masks": [
            { "type": "PATH", "space": "body", "path": [ { "u": 0.66, "v": 0.85 }, { "u": 0.70, "v": 0.92 } ], "width": 2.0, "softness": 1.0 }
          ],
          "op": { "op": "paint", "hue": "$hue", "saturation": 0.55, "lightness": 0.14 }
        }
      ]
    },
    {
      "when": ["V/v"],
      "description": "A sparser web of black lace crosses the body and neck, fading around a single faint bloom at the withers.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK"] },
            { "type": "FRACTAL", "shape": "ridged", "space": "body", "seed": "$seed", "octaves": 2, "warp": 0.08, "threshold": 0.82, "softness": 0.05, "combine": "MULTIPLY" }
          ],
          "op": { "op": "paint", "color": "#151016" }
        },
        {
          "masks": [
            { "type": "PATH", "space": "body", "path": [ { "u": 0.55, "v": 0.60 }, { "u": 0.58, "v": 0.68 } ], "width": 1.8, "softness": 1.5 }
          ],
          "op": { "op": "paint", "hue": "$hue", "saturation": 0.35, "lightness": 0.18 }
        }
      ]
    },
    {
      "description": "Coat shows no lacing.",
      "layers": []
    }
  ],
  "notes": [
    "Veins are painted before the deep-center layers within each expression's layer list so the centers render over the lace rather than under it; masking still can't see prior paint (rule 8), which is fine here since neither layer masks off the other's coverage.",
    "PARTS region is BODY+NECK+HEAD+MUZZLE for V/V and BODY+NECK only for V/v, so the heterozygote reads as a smaller, less-spread expression rather than just a thinner version of the full pattern.",
    "Vein color is fixed (#151016) because the marking is conceptually black lace; only the deep centers take the hue knob, since their color is the part described as varying ('colored deep centers').",
    "FRACTAL octaves capped at 3 per rule 12 (finer detail goes sub-texel); the heterozygote uses 2 octaves and a higher threshold for a visibly simpler, sparser network rather than perDose-scaling a single layer.",
    "PATH was used for both deep centers instead of SPOTS because the reference wants two specific rooted locations (withers, poll), not a repeating lattice; coordinates derived from u=x/41, v=y/33.7 against the given part extents.",
    "Approximation: the vein fractal and the two PATH centers are independent, co-located shapes, not a single connected branching structure rooted at those points — see TOOL GAP below."
  ]
}
```

ELEMENT: the vein network's visible connection to its two deep-color roots (veins branching outward continuously from the blooms, per the reference image).
SHIPPED: a FRACTAL ridged mask covering the region, independently overlaid by two PATH-shaped solid patches at the withers and poll.
WHY NOT: FRACTAL ridged is unrooted global noise — it has no notion of "originates at this point" — and PATH only carries a single prescribed curve/patch, not a branching tree. Neither measures graph-connectivity back to a root, so a viewer will sometimes see a vein pass near a bloom without visibly joining it, or a bloom edge with no vein touching it at all; no threshold, octave, or warp tuning changes that, since the two masks are answering different questions (density-above-threshold vs. distance-to-path) rather than "is this pixel connected by an unbroken branch to that root."