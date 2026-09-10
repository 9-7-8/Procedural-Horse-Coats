```json
{
  "id": "iridescent_rosette",
  "name": "Iridescent Rosette",
  "type": "magical",
  "locus": "IRR",
  "blurb": "Dark rosettes speckle the coat, each one rimmed in bands of colour that shift like the shell of a jewel beetle.",
  "parts": ["BODY", "NECK", "HEAD"],
  "alleles": ["W", "I"],
  "knobs": [
    { "id": "seed", "type": "seed" },
    { "id": "spacing", "type": "number", "default": 4.5, "min": 3.5, "max": 6.0 },
    { "id": "stretch", "type": "number", "default": 1.05, "min": 0.9, "max": 1.25 },
    { "id": "vary", "type": "number", "default": 0.35, "min": 0.15, "max": 0.5 },
    { "id": "rHole", "type": "number", "default": 0.9, "min": 0.6, "max": 1.2 },
    { "id": "holeSoft", "type": "number", "default": 0.25, "min": 0.1, "max": 0.35 },
    { "id": "rOuter", "type": "number", "default": 3.2, "min": 2.4, "max": 4.0 },
    { "id": "outerSoft", "type": "number", "default": 0.3, "min": 0.15, "max": 0.4 },
    { "id": "hueInner", "type": "number", "default": 150, "min": 0, "max": 360 },
    { "id": "hueMid", "type": "number", "default": 45, "min": 0, "max": 360 },
    { "id": "hueOuter", "type": "number", "default": 330, "min": 0, "max": 360 }
  ],
  "expressions": [
    {
      "id": "pale",
      "when": ["W/W", "W/I"],
      "description": "Faint ivory ring-spots ghost the coat, the same size and spacing as the true rosettes but without any colour in the band.",
      "layers": [
        {
          "mask": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD"] },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$spacing",
              "stretch": "$stretch",
              "vary": "$vary",
              "mirror": true,
              "radius": "$rOuter",
              "softness": "$outerSoft",
              "combine": "MULTIPLY"
            },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$spacing",
              "stretch": "$stretch",
              "vary": "$vary",
              "mirror": true,
              "radius": "$rHole",
              "softness": "$holeSoft",
              "combine": "SUBTRACT"
            }
          ],
          "op": { "type": "SOLID", "color": "#EDE6D6" }
        }
      ]
    },
    {
      "id": "iridescent",
      "when": ["I/I"],
      "description": "Each rosette opens into a dark hollow ringed by green, then gold, then a wide band of magenta before the coat returns to its base colour - a beetle-shell shimmer across the whole body, neck, and face.",
      "layers": [
        {
          "mask": [
            { "type": "PARTS", "parts": ["BODY", "NECK", "HEAD"] },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$spacing",
              "stretch": "$stretch",
              "vary": "$vary",
              "mirror": true,
              "radius": "$rOuter",
              "softness": "$outerSoft",
              "combine": "MULTIPLY"
            },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": "$spacing",
              "stretch": "$stretch",
              "vary": "$vary",
              "mirror": true,
              "radius": "$rHole",
              "softness": "$holeSoft",
              "combine": "SUBTRACT"
            }
          ],
          "op": {
            "type": "RAMP",
            "axis": "radial",
            "from": "$rHole",
            "to": "$rOuter",
            "stops": [
              { "at": 0.0, "hue": "$hueInner", "sat": 0.55, "light": 0.42 },
              { "at": 0.28, "hue": "$hueInner", "sat": 0.55, "light": 0.42 },
              { "at": 0.40, "hue": "$hueMid", "sat": 0.72, "light": 0.55 },
              { "at": 0.58, "hue": "$hueMid", "sat": 0.72, "light": 0.55 },
              { "at": 0.68, "hue": "$hueOuter", "sat": 0.55, "light": 0.62 },
              { "at": 1.0, "hue": "$hueOuter", "sat": 0.55, "light": 0.62 }
            ]
          }
        }
      ]
    }
  ],
  "founders": {
    "W": { "rarity": "RARE" },
    "I": { "rarity": "LEGENDARY" }
  },
  "notes": [
    "Both alleles draw from the identical mask stack - a footprint SPOTS multiplied in, then a smaller SPOTS of the same seed/spacing/stretch/vary subtracted out to leave the dark hollow - so W and I only ever differ in the op, per the pair convention. This is the deliberate reading of 'compose before inventing': a concentric ring-and-hollow reads as two nested SPOTS sharing one lattice, not a dedicated ring primitive.",
    "The recessive form needed three hues in a fixed order (green inner, gold mid, magenta outer) to read as structural iridescence rather than a single dyed ring, so it carries hueInner/hueMid/hueOuter instead of the usual single hue knob. The shape knobs (seed, spacing, stretch, vary, rHole, rOuter, and both softness values) are still shared with W exactly as the convention intends.",
    "RAMP's from/to are pinned to the same $rHole/$rOuter knobs that shape the mask, not independent literals, so the colour bands can never drift out of registration with the hollow or the rim as those knobs are bred toward their range extremes. Their declared ranges (rHole max 1.2, rOuter min 2.4) keep from < to at every legal knob value.",
    "SPOTS radius/softness already gives full coverage inside radius fading to zero over the softness band outward, which is the correct direction for both the footprint and the hollow, so no invert is needed on either mask - only the SUBTRACT combine on the hollow.",
    "The reference also carries small ring-marks at the leg joints; those are left off this gene to keep the pattern legible at coat scale and because the legs are visually secondary to the torso/neck/head reading. A future joint-accent variant could reuse this same mask stack scoped to LEGS."
  ]
}
```