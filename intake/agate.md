```json
{
  "id": "agate",
  "name": "Agate",
  "phase": "magical",
  "locus": "AGATE",
  "blurb": "A rare mineral-sheen marking that shatters the coat into cracked, jewel-toned cells — each blob softening to a pale rim and deepening to its opposite hue at the core, like agate split open in the light.",
  "knobs": [
    { "name": "cellSeed", "type": "seed" },
    { "name": "cellSize", "type": "float", "min": 3.5, "max": 6.0, "default": 4.5 },
    { "name": "cellVary", "type": "float", "min": 0.15, "max": 0.4, "default": 0.25 },
    { "name": "wallWidth", "type": "float", "min": 0.4, "max": 0.7, "default": 0.5 },
    { "name": "wallSoftness", "type": "float", "min": 0.1, "max": 0.25, "default": 0.15 },
    { "name": "coreRadius", "type": "float", "min": 1.0, "max": 1.8, "default": 1.4 },
    { "name": "coreSoftness", "type": "float", "min": 0.6, "max": 1.0, "default": 0.8 },
    { "name": "hue", "type": "float", "min": 0, "max": 360, "default": 190 }
  ],
  "alleles": [
    { "symbol": "Ao", "name": "Agate Opal", "rarity": "RARE" },
    { "symbol": "Ac", "name": "Agate Chroma", "rarity": "EPIC" }
  ],
  "expressions": [
    {
      "when": ["Ao/Ao", "Ao/Ac"],
      "description": "The coat takes on a pearly, cracked-opal sheen — pale, pearlescent cells rimmed faintly in lilac-white, brightening to a milky glow at each cell's heart.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["HAIR"], "invert": true },
            { "type": "CRACKLE", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "wallWidth": "$wallWidth", "softness": "$wallSoftness", "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#EDE7F6" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["HAIR"], "invert": true },
            { "type": "CRACKLE", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "wallWidth": "$wallWidth", "softness": "$wallSoftness", "combine": "MULTIPLY" },
            { "type": "DAPPLES", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "radius": "$coreRadius", "softness": "$coreSoftness", "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#FFFFFF" }
        }
      ],
      "notes": [
        "This is the dominant, single-tone reading of the pattern: the same crackle/dapple shape as the chromatic form below, painted in flat opal white/lilac rather than PALETTE-driven hue, per the white-form convention."
      ]
    },
    {
      "when": ["Ac/Ac"],
      "description": "The coat shatters into a mosaic of jewel-bright cells — each pastel blob deepening into its opposite hue at the core, veined through with dark cracks like a cut agate.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["HAIR"], "invert": true },
            { "type": "CRACKLE", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "wallWidth": "$wallWidth", "softness": "$wallSoftness", "combine": "MULTIPLY" }
          ],
          "op": {
            "type": "PALETTE",
            "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x",
            "colors": [
              { "hue": "$hue", "rotate": 0, "sat": 0.34, "light": 0.82 },
              { "hue": "$hue", "rotate": 125, "sat": 0.30, "light": 0.85 },
              { "hue": "$hue", "rotate": 245, "sat": 0.38, "light": 0.80 }
            ]
          }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["HAIR"], "invert": true },
            { "type": "CRACKLE", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "wallWidth": "$wallWidth", "softness": "$wallSoftness", "combine": "MULTIPLY" },
            { "type": "DAPPLES", "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x", "radius": "$coreRadius", "softness": "$coreSoftness", "combine": "MULTIPLY" }
          ],
          "op": {
            "type": "PALETTE",
            "seed": "$cellSeed", "spacing": "$cellSize", "vary": "$cellVary", "stretch": 1.15, "stretchAxis": "x",
            "colors": [
              { "hue": "$hue", "rotate": 180, "sat": 0.60, "light": 0.58 },
              { "hue": "$hue", "rotate": 305, "sat": 0.55, "light": 0.62 },
              { "hue": "$hue", "rotate": 65, "sat": 0.62, "light": 0.55 }
            ]
          }
        }
      ],
      "notes": [
        "The two PALETTE ops must keep identical seed, spacing, vary, stretch and stretchAxis, and matching list lengths (3), so each cell's index resolves to the same slot in both lists; the second list's rotate values are the first list's rotate values +180 (mod 360), which is what makes the core read as the edge colour's complement rather than an unrelated third colour.",
        "coreRadius/coreSoftness were tuned against the default cellSize of 4.5 units so the DAPPLES falloff stays inside a cell's interior (~0.31x cellSize at default); raising cellSize's max without a matching rise in coreRadius's range will make the core look small and lost within larger cells, and lowering cellSize without shrinking coreRadius will let the core spill past the crackle wall and clip against the black veins.",
        "wallWidth is kept well under cellSize (~1/9 at defaults) so the network reads as veins, not a wash.",
        "The pattern is masked off HAIR (mane and tail) on both layers, since this is a skin marking, not a hair-pigment one; ears and legs are left in since they carry skin same as the barrel."
      ]
    },
    {
      "description": "No visible agate pattern; the coat is unaffected by this locus.",
      "layers": []
    }
  ],
  "notes": [
    "Stretch is fixed (not a knob) at 1.15 along body-space X on every CRACKLE/DAPPLES/PALETTE call in this gene, biasing cells slightly long along the horse's length rather than perfectly round; all three mask/op families must keep this value identical or their lattices will drift apart and the core will land off-center in its cell.",
    "PIGMENT/LUMA were not needed here: this marking doesn't key off the horse's existing coat colour, it paints over whatever base coat shows through the crackle walls (black in the reference image, but any base coat on an actual horse)."
  ]
}
```