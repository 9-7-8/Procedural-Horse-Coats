```json
{
  "id": "kite_bloom",
  "name": "Kite Bloom",
  "phase": "magical",
  "blurb": "A rare bloom of angular, jewel-toned patches spreads across the coat like a stained-glass leaf, each panel scattered with pale flecks.",
  "alleles": [
    {
      "id": "white",
      "name": "Kite Bloom (White)",
      "blurb": "A faint lattice of pale, silvery panels ghosts across the coat, barely visible except at an angle.",
      "knobs": [
        { "name": "seed", "type": "seed" },
        { "name": "patchSize", "type": "number", "min": 5, "max": 7.5, "default": 6 }
      ],
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "combine": "MULTIPLY", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE",
              "combine": "MULTIPLY",
              "seed": "$seed",
              "spacing": "$patchSize",
              "fill": 0.5,
              "edgeWidth": 0.5,
              "warp": 0.2
            }
          ],
          "op": { "type": "COLOR", "color": "#EDEFE8" }
        }
      ]
    },
    {
      "id": "color",
      "name": "Kite Bloom (Colored)",
      "blurb": "Deep blue, violet, and teal panels tile the coat in hard-edged facets, each one salted with small pale blotches.",
      "knobs": [
        { "name": "seed", "type": "seed" },
        { "name": "patchSize", "type": "number", "min": 4, "max": 6, "default": 4.5 },
        { "name": "hueA", "type": "number", "min": 0, "max": 360, "default": 212 },
        { "name": "hueB", "type": "number", "min": 0, "max": 360, "default": 276 },
        { "name": "hueC", "type": "number", "min": 0, "max": 360, "default": 174 },
        { "name": "dotSeed", "type": "seed" },
        { "name": "dotRadius", "type": "number", "min": 0.3, "max": 0.7, "default": 0.5 },
        { "name": "dotSpacing", "type": "number", "min": 1.3, "max": 2.0, "default": 1.6 },
        { "name": "dotVary", "type": "number", "min": 0, "max": 1, "default": 0.6 }
      ],
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "combine": "MULTIPLY", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE",
              "combine": "MULTIPLY",
              "seed": "$seed",
              "spacing": "$patchSize",
              "fill": 0.62,
              "edgeWidth": 0.4,
              "warp": 0.15
            }
          ],
          "op": {
            "type": "PALETTE",
            "seed": "$seed",
            "spacing": "$patchSize",
            "colors": [
              { "hue": "$hueA" },
              { "hue": "$hueB" },
              { "hue": "$hueC" }
            ],
            "saturation": 0.58,
            "lightness": 0.42
          }
        },
        {
          "masks": [
            { "type": "PARTS", "combine": "MULTIPLY", "parts": ["BODY", "NECK", "HEAD", "MUZZLE"] },
            {
              "type": "CRACKLE",
              "combine": "MULTIPLY",
              "seed": "$seed",
              "spacing": "$patchSize",
              "fill": 0.62,
              "edgeWidth": 0.4,
              "warp": 0.15
            },
            {
              "type": "SPOTS",
              "combine": "MULTIPLY",
              "seed": "$dotSeed",
              "radius": "$dotRadius",
              "spacing": "$dotSpacing",
              "vary": "$dotVary"
            }
          ],
          "op": { "type": "COLOR", "color": "#F8F8F2" }
        }
      ]
    }
  ],
  "expressions": [
    {
      "when": { "white": 2 },
      "description": "A faint, silvery lattice of panels ghosts over the coat, catching light without much color.",
      "rarity": "COMMON"
    },
    {
      "when": { "white": 1, "color": 1 },
      "description": "The panel lattice shows through more clearly, tinted with a wash of muted color under the pale overlay.",
      "rarity": "UNCOMMON"
    },
    {
      "when": { "color": 2 },
      "description": "Bold blue, violet, and teal facets tile the coat edge to edge, each one dusted with small pale blotches.",
      "rarity": "LEGENDARY",
      "notes": [
        "The reference photo's color shifts with viewing angle (an iridescent sheen); this gene instead assigns each CRACKLE cell one fixed hue from a 3-entry palette, so the shimmer reads as a jewel-toned patchwork rather than a shot-silk shift. See TOOL GAP below.",
        "dotVary is set high enough that some adjacent spots grow into fused blotches, echoing the larger white patches in the reference rather than uniform dots."
      ]
    }
  ],
  "notes": [
    "patchSize (4-6 units) is chosen against the 22-unit barrel so 4-5 major panels cross the body, matching the reference's facet density.",
    "fill=0.62 with edgeWidth=0.4 leaves a visible base-coat seam between panels, standing in for the leaf's vein network.",
    "Both layers in the 'color' allele redeclare the same PARTS+CRACKLE mask stack independently rather than one reading the other's paint (layers can't see above them); this is why the dot layer repeats the shape mask instead of referencing the color layer.",
    "Masks run in body space rather than local space for NECK/HEAD/MUZZLE; panels crossing those pitched parts may read as slightly stretched relative to BODY. This is a tuning simplification, not a tool gap - local space per rule 9 would fix it and was omitted only because the marking is a whole-body field rather than a part-aligned band."
  ]
}
```

ELEMENT: the leaf's iridescent shift between blue, violet, and teal as the viewing angle changes.
SHIPPED: CRACKLE cells each get one fixed hue from a 3-entry PALETTE, sharing the cell's seed/spacing so color and shape align.
WHY NOT: every mask and PALETTE/RAMP op resolves color from spatial position (or, for LUMA, from the already-resolved coat), never from the angle between the surface normal and the viewer. A fixed per-cell hue can't reproduce a single point changing color as the camera moves; that needs a view-angle measurement no current mask takes.

PROPOSAL
NAME: RAMP, new "axis" value "view" (parameter addition, not a new op)
PARAMETERS:
- axis: enum, adds "view" alongside existing spatial axes. At a texel, measures the dot product of the surface normal and the direction to the active camera, remapped 0-1.
- from / to: color, existing RAMP fields, unchanged.
At axis value 0 (grazing, normal near-perpendicular to view) the texel takes the "from" color; at 1 (facing the viewer square-on) it takes "to". Between them it interpolates continuously, same as RAMP's existing spatial axes, so it varies color only, never coverage - consistent with rule 5.
Closest existing mask: RAMP itself. The one thing this does that today's RAMP cannot: vary color by the model's orientation to the camera rather than by a fixed position in body space, which is what makes the same painted point shift hue as the horse turns.