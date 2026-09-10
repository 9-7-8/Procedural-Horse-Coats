```json
{
  "id": "opaline",
  "name": "Opaline",
  "locus": "OPALINE",
  "blurb": "Opaline traces a network of raised, glassy veins through the coat, dividing it into a mosaic of small irregular cells. In its common dominant form the mosaic is a faint webbing of pale-on-pale cells; in its rare fully recessive form the cells blaze blue and violet, each rimmed near-black and starred with tiny pale bubbles where the veins meet.",
  "notes": [
    "Colour is fixed by concept — this is specifically a blue/violet/white 'opal' effect — so there is no $hue knob and the usual white-form/hue-recolour CONVENTION is adapted into a three-allele series instead: Op (dominant, plain pale form) > N (no effect) > op (fully recessive, full colour form).",
    "Every position-based mask below uses space: body so the cell lattice runs continuously across the BODY/NECK/HEAD/MUZZLE/leg seams instead of restarting at each part boundary.",
    "CRACKLE, SPOTS and CHOICE key off $seed / $tintSeed / $bubbleSeed together with matching $cellSize or $bubbleSpacing and jitter wherever they must agree on the same cell or spot centres. In particular the three FILL layers share $tintSeed + $cellSize with the CHOICE mask so 'which third of the cells is blue vs violet vs pale' is one consistent partition, not three independent draws.",
    "RIM is built as an annulus: the wide $rimWidth reading of the wall-distance field minus the narrower $veinWidth reading of the same field, both taken with invert: true per rule 6 — direction is flipped with invert, from/to are never swapped.",
    "Opaline paints BODY, NECK, HEAD, MUZZLE and the four legs only. MANE, TAIL and EARS are excluded — this is a skin/coat effect, not a hair pigment."
  ],
  "knobs": {
    "seed": {"type": "seed"},
    "tintSeed": {"type": "seed"},
    "bubbleSeed": {"type": "seed"},
    "cellSize": {"type": "number", "default": 3.1},
    "veinWidth": {"type": "number", "default": 0.3},
    "rimWidth": {"type": "number", "default": 0.55},
    "bubbleSpacing": {"type": "number", "default": 1.15},
    "bubbleOuterRadius": {"type": "number", "default": 0.16},
    "bubbleInnerRadius": {"type": "number", "default": 0.09},
    "bubbleCoreRadius": {"type": "number", "default": 0.05}
  },
  "alleles": [
    {"symbol": "Op", "name": "White Opaline", "founders": {"rarity": "RARE"}},
    {"symbol": "N", "name": "none", "founders": {"rarity": "COMMON"}},
    {"symbol": "op", "name": "Opaline", "founders": {"rarity": "UNCOMMON"}}
  ],
  "expressions": [
    {
      "when": ["Op/Op", "Op/N", "Op/op"],
      "description": "A faint webbing of pale, glassy veins divides the coat into soft cream-on-cream cells, like frost on a window.",
      "notes": [
        "Op is dominant over both N and op, so any horse carrying a single Op copy shows this quiet pale form regardless of what the other allele is — including over a hidden op, which the horse can still pass to its foals.",
        "Reuses the exact vein/rim/cell/bubble geometry (same $seed, $tintSeed, $bubbleSeed) as the op/op expression below, just recoloured into off-white and pale tones, so a carrier previews the shape (not the colour) its future op/op foals could show in full."
      ],
      "layers": [
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 0, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#F1ECE0"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 1, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#E3DEEA"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 2, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#E6ECEF"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": true, "combine": "MULTIPLY"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$veinWidth", "invert": true, "combine": "SUBTRACT"}
          ],
          "op": {"color": "#C9C2B3"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$veinWidth", "invert": true, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#FBF9F5"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleOuterRadius", "softness": 0.03, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "MULTIPLY"},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleInnerRadius", "softness": 0.02, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "SUBTRACT"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#D8D2C2"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleCoreRadius", "softness": 0.02, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "MULTIPLY"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#FFFFFF"}
        }
      ]
    },
    {
      "when": ["op/op"],
      "description": "The coat breaks into a stained-glass mosaic of blue and violet cells, each ringed in near-black and lit with tiny pale bubbles where the veins meet.",
      "notes": [
        "Only op/op shows colour. A single op next to N is a silent carrier — N/op is the carrier-only pair the founders table omits — and any Op copy masks the colour entirely, per the Op expression above.",
        "FILL layers are listed before RIM and VEIN even though their coverage regions do not actually overlap (FILL only starts past $rimWidth from the nearest wall), so the listed order already matches the geometry independent of engine compositing order.",
        "Approximation: each cell fill is a single flat colour. The reference painting shows every cell brightening toward its own centre and darkening toward its own rim — a continuous radial gradient local to each irregular cell. No available mask exposes a continuous 'distance to my own cell's centre' value for a colour op to ramp across: RAMP only varies colour along one global (or PATH) axis, and DAPPLES/SPOTS/CRACKLE only ever resolve to a coverage number, never a value handed to RAMP. The mosaic here therefore reads flatter and more uniformly saturated within each cell than the source image, which has real internal light and shadow in almost every cell. See TOOL GAP below."
      ],
      "layers": [
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 0, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#2569A1"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 1, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#584A82"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"},
            {"type": "CHOICE", "seed": "$tintSeed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "weights": [0.5, 0.32, 0.18], "index": 2, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#DCE4E8"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": true, "combine": "MULTIPLY"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$veinWidth", "invert": true, "combine": "SUBTRACT"}
          ],
          "op": {"color": "#20233A"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$veinWidth", "invert": true, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#F5F1E8"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleOuterRadius", "softness": 0.03, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "MULTIPLY"},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleInnerRadius", "softness": 0.02, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "SUBTRACT"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#181B2C"}
        },
        {
          "masks": [
            {"type": "PARTS", "parts": ["BODY", "NECK", "HEAD", "MUZZLE", "LEGS"]},
            {"type": "SPOTS", "seed": "$bubbleSeed", "spacing": "$bubbleSpacing", "radius": "$bubbleCoreRadius", "softness": 0.02, "jitter": 0.85, "vary": 0.3, "space": "body", "combine": "MULTIPLY"},
            {"type": "CRACKLE", "seed": "$seed", "spacing": "$cellSize", "jitter": 0.9, "space": "body", "from": 0, "to": "$rimWidth", "invert": false, "combine": "MULTIPLY"}
          ],
          "op": {"color": "#FBF8F2"}
        }
      ]
    },
    {
      "description": "No Opaline marking is visible.",
      "layers": []
    }
  ]
}
```

ELEMENT the soft light-to-dark gradient inside each cell of the reference (brighter near each cell's own centre, darkening toward its own rim).
SHIPPED three flat-colour FILL layers gated by CRACKLE-interior coverage and a CHOICE index; a viewer sees uniform blue, violet or pale patches per cell, with all internal modelling coming only from the separate hard RIM band, not a continuous fade.
WHY NOT every mask resolves to a single coverage number for an op to scale; none exposes a continuous per-cell radial value (distance to *that cell's own* centre) that a colour op could ramp across. RAMP varies colour along one global or PATH axis, not a per-Voronoi-cell-local one, so it can't gradient each irregular cell independently the way DAPPLES/SPOTS/CRACKLE can space coverage per-cell — this is a different measurement, not a tuning gap.

PROPOSAL (parameter) RAMP.axis: "cell" (kind: enum, default: "linear") — paired with new fields RAMP.cellSource (kind: mask-reference, default: none, pointing at a CRACKLE or SPOTS mask's seed+spacing+jitter). At a texel it measures normalised distance from that texel to the centre of its own enclosing cell/spot (0 = centre, 1 = the shared boundary with the nearest neighbour), and RAMP interpolates colour across 0-1 along that value instead of along a global line. Closest to existing RAMP with axis: "linear"/"radial"; the one thing it adds is a per-cell-local coordinate so every irregular CRACKLE or SPOTS cell gets its own independent radial fade while all cells stay reproducible from one seed, instead of one shared axis running through the whole mesh.