```json
{
  "id": "opal_crackle",
  "name": "Opal Crackle",
  "phase": "MAGICAL",
  "locus": "OPAL_CRACKLE",
  "alleles": [
    { "symbol": "OC", "name": "Opal (iridescent)", "dominance": "dominant" },
    { "symbol": "oc", "name": "Opal (pigmented)", "dominance": "recessive" }
  ],
  "knobs": [
    { "name": "seed", "type": "seed", "default": 0 },
    { "name": "cellSize", "type": "number", "min": 2.2, "max": 4.5, "default": 3.2 },
    { "name": "veinWidth", "type": "number", "min": 0.3, "max": 0.9, "default": 0.6 },
    { "name": "goldSeed", "type": "seed", "default": 0 },
    { "name": "flecksSpacing", "type": "number", "min": 1.6, "max": 3.2, "default": 2.4 },
    { "name": "flecksRadius", "type": "number", "min": 0.2, "max": 0.5, "default": 0.32 },
    { "name": "hue", "type": "number", "min": 0, "max": 360, "default": 210 }
  ],
  "expressions": [
    {
      "when": ["OC/OC", "OC/oc"],
      "description": "The coat is broken by a network of dark, thin-walled cracks running nose to tail, each seam lit from within — teal and blue near the withers shifting to violet and hot pink toward the hindquarters — with scattered flecks of gold caught in the matrix between them.",
      "layers": [
        {
          "masks": [
            { "type": "group", "group": "ALL" },
            {
              "type": "CRACKLE",
              "seed": "$seed",
              "cellSize": "$cellSize",
              "edgeWidth": "$veinWidth",
              "jitter": 0.55,
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "RAMP",
            "axis": "x",
            "space": "body",
            "from": 0,
            "to": 41,
            "stops": [
              { "at": 0.0, "color": "#00E5FF" },
              { "at": 0.35, "color": "#2244FF" },
              { "at": 0.6, "color": "#8A2BE2" },
              { "at": 0.85, "color": "#E619B0" },
              { "at": 1.0, "color": "#FF3D9A" }
            ]
          }
        },
        {
          "masks": [
            { "type": "group", "group": "ALL" },
            {
              "type": "SPOTS",
              "seed": "$goldSeed",
              "spacing": "$flecksSpacing",
              "radius": "$flecksRadius",
              "vary": 0.5,
              "combine": "MULTIPLY"
            },
            {
              "type": "CRACKLE",
              "seed": "$seed",
              "cellSize": "$cellSize",
              "edgeWidth": "$veinWidth",
              "invert": true,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "SOLID", "color": "#D9A441" }
        }
      ],
      "notes": [
        "veinWidth is held well under cellSize (about a fifth) so the network reads as seams, not a wash — same headroom STROKES needs against its spacing.",
        "The RAMP runs along whole-horse X because the reference shows the seam colour drifting from cool to warm across the body's length, not cycling per-cell; this is the honest limit noted below.",
        "The gold-fleck layer reuses the exact seed/cellSize/edgeWidth of the vein layer, inverted, so flecks land in the flats and never sit on top of a seam — this is the 'ground minus the throat' pattern (Rule 2), not a second independent draw."
      ]
    },
    {
      "description": "A quieter, single-toned version of the same cracked pattern — the seams still run the length of the body, but glow one steady colour instead of shifting through the spectrum.",
      "layers": [
        {
          "masks": [
            { "type": "group", "group": "ALL" },
            {
              "type": "CRACKLE",
              "seed": "$seed",
              "cellSize": "$cellSize",
              "edgeWidth": "$veinWidth",
              "jitter": 0.55,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "SOLID", "hue": "$hue" }
        },
        {
          "masks": [
            { "type": "group", "group": "ALL" },
            {
              "type": "SPOTS",
              "seed": "$goldSeed",
              "spacing": "$flecksSpacing",
              "radius": "$flecksRadius",
              "vary": 0.5,
              "combine": "MULTIPLY"
            },
            {
              "type": "CRACKLE",
              "seed": "$seed",
              "cellSize": "$cellSize",
              "edgeWidth": "$veinWidth",
              "invert": true,
              "combine": "MULTIPLY"
            }
          ],
          "op": { "type": "SOLID", "color": "#D9A441" }
        }
      ],
      "notes": [
        "Same two layers as the dominant form, same knobs, only the vein layer's op changes from RAMP to SOLID hue — this is the standard white-form/coloured-form pair, not a redesign."
      ]
    }
  ],
  "founders": { "rarity": "LEGENDARY" },
  "blurb": "A coat shot through with a network of fine dark seams, each one catching light in its own colour — the same fire you'd find cracked out of a boulder opal.",
  "notes": [
    "The reference's per-seam colour variation (adjacent cracks showing unrelated hues) cannot be produced by an axis RAMP or a lattice PALETTE; both are documented below as a genuine tool gap, and this file ships the closest honest approximation (a whole-body positional gradient) instead.",
    "cellSize and veinWidth are shared verbatim between the vein layer and the fleck layer's subtractive mask in both expressions — if either knob changes, re-check that flecks still avoid the seams."
  ]
}
```

ELEMENT: the per-seam colour shift in the crackle network (adjacent cracks showing unrelated hues — teal next to blue next to violet next to pink, not a smooth sweep).
SHIPPED: CRACKLE for the seam shape, coloured by a single whole-horse RAMP along X. A viewer sees the colour drift smoothly nose-to-tail instead of jumping between neighboring seams; two adjacent cracks a hand's-width apart will always be nearly the same hue, where the reference shows them sharply different.
WHY NOT: RAMP measures position along a spatial axis; PALETTE measures which lattice cell a point falls inside. Neither measures which *edge* of the CRACKLE diagram a point belongs to — CRACKLE collapses that identity into a single coverage scalar before any op sees it, so there is no per-seam key left for a colour source to read. This is the same gap DAPPLES/SPOTS have against CRACKLE's own walls: a different measurement, not different tuning.

PROPOSAL: RAMP.keyBy (string enum ["position","crackleEdge"], default "position") — when "crackleEdge", RAMP samples its stop position not from spatial coordinate but from a stable per-edge value (0-1) derived from the same seed/cellSize/jitter as the CRACKLE mask feeding the layer. At a texel, it measures which cell-boundary segment that texel sits on, not where that texel is in the body box. A value near 0 keys the coolest stop, a value near 1 keys the warmest, and every texel along one continuous seam gets the same key while neighboring seams draw independent keys. Closest existing thing: RAMP itself (same stop list, same interpolation) — the one thing this adds is a coordinate source keyed to CRACKLE's edge identity instead of the model's spatial axes.