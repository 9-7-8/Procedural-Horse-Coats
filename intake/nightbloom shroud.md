```json
{
  "id": "nightbloom_shroud",
  "name": "Nightbloom Shroud",
  "type": "magical",
  "alleles": ["Nb", "nb"],
  "knobs": [
    { "name": "seed", "type": "seed" },
    { "name": "gillSpacing", "type": "float", "min": 1.3, "max": 1.9, "default": 1.6 }
  ],
  "expressions": [
    {
      "when": ["Nb/Nb", "Nb/nb"],
      "description": "The crest, poll and whole skull glow deep abyssal blue, and ribbons of gold-to-violet gill striping fan down the neck and barrel, dimming to the ordinary coat above the knees.",
      "notes": [
        "intensity.perDose is keyed on copies of Nb: index 1 = Nb/nb, index 2 = Nb/Nb. Index 0 is never read in this expression (0 copies is the catch-all's territory) and is included only so the array has one slot per possible dose.",
        "Only the gill layer scales with dose. The blue mantle is treated as an all-or-nothing signal - a single copy commits fully to it - so it carries no perDose and looks identical in Nb/Nb and Nb/nb."
      ],
      "layers": [
        {
          "id": "gill_bloom",
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK"] },
            {
              "type": "PATH",
              "combine": "MULTIPLY",
              "invert": true,
              "space": "body",
              "points": [
                [0.1951, 0.3264],
                [0.4390, 0.3264],
                [0.6585, 0.3858],
                [0.7317, 0.5045],
                [0.8049, 0.6528]
              ],
              "width": 0.5,
              "softness": 10
            },
            {
              "type": "STROKES",
              "combine": "MULTIPLY",
              "space": "body",
              "axis": "X",
              "across": "Y",
              "spacing": "$gillSpacing",
              "width": 0.35,
              "softness": 0.15,
              "seed": "$seed"
            }
          ],
          "op": {
            "type": "RAMP",
            "space": "body",
            "axis": "Y",
            "from": 11,
            "to": 27,
            "hueFrom": 285,
            "hueTo": 48,
            "saturation": 88,
            "lightness": 62,
            "emissive": true
          },
          "intensity": { "perDose": [0, 0.55, 1.0] }
        },
        {
          "id": "dorsal_mantle",
          "masks": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD"] },
            {
              "type": "PATH",
              "combine": "MULTIPLY",
              "invert": true,
              "parts": ["BODY", "NECK"],
              "space": "body",
              "points": [
                [0.1951, 0.3264],
                [0.4390, 0.3264],
                [0.6585, 0.3858],
                [0.7317, 0.5045],
                [0.8049, 0.6528]
              ],
              "width": 0.5,
              "softness": 5
            }
          ],
          "op": {
            "type": "COLOR",
            "color": "#141E6E",
            "emissive": true
          }
        }
      ]
    },
    {
      "description": "No bioluminescent marking is present; the horse's coat and coloring are entirely ordinary.",
      "layers": []
    }
  ],
  "founders": { "rarity": "LEGENDARY" },
  "blurb": "Nightbloom Shroud is a vanishingly rare marking said to mark horses touched by fungal fae magic deep in old-growth forests. Carriers glow faintly blue along the crest and skull, while fanned gill-like striations of gold and violet ripple down the neck and body before fading into the ordinary coat near the legs.",
  "notes": [
    "The dorsal_mantle layer paints HEAD, BODY and NECK together in one PARTS mask, then multiplies in a PATH mask whose own 'parts' list only names BODY and NECK - per rule 2 that PATH is skipped over HEAD rather than zeroing it, so the whole head stays at full coverage from the first mask while the body/neck get shaped. This is the 'ground minus throat' technique applied in reverse (a shape minus the head, rather than a ground minus the throat).",
    "Both layers reuse the same five-point underline-to-throat polyline in body space (not local/pitched space), inverted so coverage is 0 on the line and grows moving away from it. The only difference between the two layers is softness: 5 units for the mantle (saturates around the upper third of the body/neck - 'the back' and 'top of the neck') and 10 units for the gills (saturates near the spine, giving a full-height field down to a hard 0 right at the underline). Body space was used instead of NECK's local space so the fade reads as one continuous gradient across the BODY/NECK seam rather than two differently-anchored fields.",
    "Legs receive no coverage from either layer because LEGS is never named in either PARTS mask - the 'fading out over the legs' in the brief is delivered by the PATH softness reaching ~0 before y=11 (the BODY/leg boundary), not by any leg-side masking.",
    "gill_bloom is painted before dorsal_mantle in the layer list so the solid blue mantle is the one visible in the overlap band near the spine/crest, per rule 8's note that a layer can't see what the layer above it painted - the visual layering here comes from list order, not from one mask reading the other's output.",
    "No hue knob was added; the blue/gold/violet palette is fixed by concept (this is a specific bioluminescent fungal identity, not a recolorable pattern). seed and gillSpacing exist purely to vary gill placement and density horse to horse.",
    "PIGMENT and LUMA were deliberately not used: this marking doesn't key off the horse's existing coat color, so there was nothing on the resolved coat worth reading.",
    "Rarity was declared as LEGENDARY rather than budgeted by hand, per the founders convention; intake/tools/founders.py should derive the actual table."
  ]
}
```