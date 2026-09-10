```json
{
  "id": "fish_scales",
  "name": "Fish Scales",
  "locus": "Scl",
  "alleles": ["Sc", "sc"],
  "blurb": "A rare skin trait that raises the coat into overlapping fish-like scales across the barrel, each rimmed in a warm, individually-tinted orange with a darker line right at its outer edge.",
  "knobs": [
    { "name": "seed", "type": "seed" },
    { "name": "hue", "type": "hue", "min": 0, "max": 360, "default": 28 },
    { "name": "scaleSize", "type": "number", "min": 2.6, "max": 4.2, "default": 3.4 }
  ],
  "expressions": [
    {
      "when": ["Sc/Sc", "Sc/sc"],
      "description": "The barrel is patterned in overlapping scales. Each scale's rim glows a warm orange - a bright, saturated band with a thinner, darker orange line traced right along its outer edge for depth - while the scale faces themselves stay the base coat colour.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$scaleSize",
              "radius": 1.7,
              "stretch": { "x": 1.3, "y": 0.85 },
              "vary": 0.15,
              "mirror": true,
              "combine": "MULTIPLY"
            },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$scaleSize",
              "radius": 0.95,
              "stretch": { "x": 1.3, "y": 0.85 },
              "vary": 0.15,
              "mirror": true,
              "combine": "SUBTRACT"
            }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "saturation": 0.72, "lightness": 0.56 }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BARREL"] },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$scaleSize",
              "radius": 1.7,
              "stretch": { "x": 1.3, "y": 0.85 },
              "vary": 0.15,
              "mirror": true,
              "combine": "MULTIPLY"
            },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$scaleSize",
              "radius": 1.4,
              "stretch": { "x": 1.3, "y": 0.85 },
              "vary": 0.15,
              "mirror": true,
              "combine": "SUBTRACT"
            }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "saturation": 0.85, "lightness": 0.28 }
        }
      ],
      "notes": [
        "Two concentric SPOTS pairs share the same seed, spacing, stretch and vary so their rings stay proportional and centred on the same cells, per the 'concentric zones' composition rule - the bright ring (radius 1.7 down to 0.95, a 0.75-unit / ~1.5-texel band) is painted first, then the darker ring (1.7 down to 1.4, a 0.3-unit / ~0.6-texel sliver) is painted on top so only its outermost edge reads as a distinct darker line, giving the two-tone rim depth the reference calls for.",
        "radius is set to exactly half of $scaleSize so same-row scales sit tangent to one another before jitter and vary push them into the overlapping, slightly irregular look of the reference photo rather than a rigid lattice.",
        "stretch widens each scale along X (nose-tail) and compresses it along Y, matching the wider-than-tall scale silhouette in the reference.",
        "Scoped to the BARREL group only - the torso is where the reference reads as scale texture; extending this onto the pitched NECK/HEAD/MUZZLE would need a second local-space layer with re-tuned spacing and is left out of this pass.",
        "See TOOL GAP below: this composition renders each scale as a full/near-full ring rather than the one-sided, tailward-facing crescent real overlapping scales show."
      ]
    },
    {
      "description": "Coat reads as a single smooth colour with no scale texture.",
      "layers": []
    }
  ],
  "founders": { "rarity": "RARE" },
  "notes": [
    "Orange is carried entirely on the $hue knob (default 28, warm orange) rather than a fixed colour, so each individual's scale-edge colour is drawn independently at birth - this is the 'random epigenetic colour' the reference asks for, implemented through the standard per-horse hue knob rather than a second allele.",
    "No perDose split between Sc/Sc and Sc/sc: the trait is presence/absence only, so both genotypes in the expression render identically and no dose table was written.",
    "The requested 'scales run backwards from nose to tail' growth direction is only partially honoured - see TOOL GAP."
  ]
}
```

ELEMENT: the free (visible) edge of each individual scale, which in the reference is a one-sided crescent facing the tail - the nose-facing half of each scale's boundary is hidden under the scale ahead of it and shows no line at all.
SHIPPED: two concentric SPOTS-pair rings tiled across the barrel, giving every scale a full (or near-full) closed ring outline - bright band plus a darker outer sliver - the same on all sides of the cell. A viewer would see complete diamond-shaped outlines around every scale rather than lines only on the tailward rim, so the coat reads as a closed net/lattice instead of shingled, directional scales.
WHY NOT: SPOTS and RINGS both measure a texel's position relative to its OWN cell's centre (radius, or for RINGS, angle-from-centre for its arc<1 crescent) - a purely local, single-cell measurement. Hiding just the noseward half of every cell in a tiled SPOTS field requires knowing where that cell sits relative to its NEIGHBOUR (so the shared measurement is inter-cell relative position/order, not distance-or-angle-from-one's-own-centre), and no listed mask measures that across a whole lattice at once; RINGS' arc<1 gives the right crescent shape but only for one placed instance, not for every cell SPOTS generates.
PROPOSAL: SPOTS.arc (number, 0-1, default 1.0) and SPOTS.angle (number, degrees, default 0) - arc measures, for each texel, its angular position around its own cell's centre relative to angle; coverage 0 outside that angular span (with the SPOTS radius falloff already applied), 1 inside it, tapering at the span's ends the way RINGS' own arc does. Closest to RINGS' existing arc parameter; the one thing it adds is applying that same directional clip per-cell across an entire tiled SPOTS lattice, which RINGS cannot do since it only places a single instance.