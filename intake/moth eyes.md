```json
{
  "id": "moth_eyes",
  "name": "Moth Eyes",
  "category": "magical_marking",
  "blurb": "A rare, faintly eerie coat pattern: fine black vein-work threads through the coat like an insect's wing membrane, broken by a row of dark, gold-ringed eyespots along the flank — but only where the coat itself is already black or grey.",
  "alleles": ["N", "ME"],
  "knobs": [
    { "name": "hue", "type": "number", "min": 40, "max": 65, "default": 52 },
    { "name": "patternSeed", "type": "seed" },
    { "name": "spotSpacing", "type": "number", "min": 5.0, "max": 8.5, "default": 6.5 },
    { "name": "spotRadius", "type": "number", "min": 1.8, "max": 2.6, "default": 2.2 },
    { "name": "spotVary", "type": "number", "min": 0.0, "max": 0.4, "default": 0.25 },
    { "name": "veinScale", "type": "number", "min": 2.0, "max": 4.5, "default": 3.0 }
  ],
  "expressions": [
    {
      "when": ["ME/ME"],
      "description": "Fine black vein-lines web the neck and body coat, and a scattering of dark, gold-ringed eyespots appears along the flank. Both only show up where the coat underneath is already black or grey — a bay's tan barrel or a chestnut's red stays plain.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "FRACTAL", "shape": "ridged", "threshold": 0.78, "softness": 0.05, "octaves": 3, "warp": 0.35, "scale": "$veinScale", "seed": "$patternSeed", "combine": "MULTIPLY" }
          ],
          "op": { "color": "#050505" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["NECK"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "FRACTAL", "shape": "ridged", "threshold": 0.78, "softness": 0.05, "octaves": 3, "warp": 0.35, "scale": "$veinScale", "seed": "$patternSeed", "space": "local", "combine": "MULTIPLY" }
          ],
          "op": { "color": "#050505" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "RINGS", "seed": "$patternSeed", "spacing": "$spotSpacing", "vary": "$spotVary", "stretch": 1.0, "radius": "$spotRadius", "width": 0.4, "arc": 1.0, "combine": "MULTIPLY" }
          ],
          "op": { "color": "#000000" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "RINGS", "seed": "$patternSeed", "spacing": "$spotSpacing", "vary": "$spotVary", "stretch": 1.0, "radius": 1.8, "width": 1.0, "arc": 1.0, "combine": "MULTIPLY" }
          ],
          "op": { "hue": "$hue" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "SPOTS", "seed": "$patternSeed", "spacing": "$spotSpacing", "vary": "$spotVary", "stretch": 1.0, "radius": 0.8, "softness": 0.05, "combine": "MULTIPLY" }
          ],
          "op": { "color": "#000000" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "LUMA", "channel": "dark", "from": 0.0, "to": 0.55, "combine": "MULTIPLY" },
            { "type": "SPOTS", "seed": "$patternSeed", "spacing": "$spotSpacing", "vary": "$spotVary", "stretch": 1.0, "radius": 0.25, "softness": 0.02, "combine": "MULTIPLY" }
          ],
          "op": { "color": "#f5f0e6" }
        }
      ],
      "notes": [
        "Paint order (veins, then rim, then yellow, then core, then highlight) is what makes the eyespots read as sitting on top of the vein-work in the final image, even though every layer's mask is computed against the pre-gene coat per the no-layer-sees-the-layer-above rule — the masks don't know about each other, only the render order does."
      ]
    },
    {
      "description": "No visible marking.",
      "layers": []
    }
  ],
  "founders": { "rarity": "EPIC" },
  "notes": [
    "Masking on 'black or grey coat only' reads LUMA, not PIGMENT: the instruction is about what the coat LOOKS like after every earlier gene has resolved (a chart could turn a black pigment level into something else entirely), so LUMA 'dark' with a from/to smoothstep of 0.0-0.55 is used, not a PIGMENT channel.",
    "Veins use FRACTAL shape=ridged rather than STROKES because the reference shows a branching, tapering vein network, not parallel hatching — STROKES cannot fork, only ridged FRACTAL can. threshold 0.78 / softness 0.05 sit between the filament (0.85/0.04) and ribbon (0.7/0.1) presets in the reference table, giving a vein slightly thicker than a hairline; 3 octaves for texel-scale detail without stippling; warp 0.35 so the lines wander instead of running dead straight.",
    "The NECK copy of the vein layer sets space:'local' because the neck is pitched per the geometry note — without it the fractal's coordinate frame would skew diagonally across the part instead of following its surface.",
    "The four eyespot bands (black rim, yellow ring, dark core, highlight) all share the same seed, spacing, vary and stretch on their RINGS/SPOTS masks so every instance is concentric and scales together as one unit; only radius/width differ per band. Because arithmetic on knob references isn't assumed to be available, only the outermost rim's radius is tied to the $spotRadius knob — the inner band radii (1.8 / 0.8 / 0.25) are fixed literals tuned to that knob's narrow range (1.8-2.6), not derived from it. This is a numeric coupling choice, not a missing capability.",
    "Eyespots are restricted to BODY only (not NECK/HEAD) to match the reference, where the wing membrane carries the eyespots and the thorax is plain fuzzed vein-texture. Spot placement uses a generic scattered SPOTS/RINGS field seeded per-horse rather than an authored PATH row, so that the marking varies horse to horse instead of stamping an identical row on every carrier — a deliberate genetics choice, not a tool gap.",
    "Rarity EPIC: single-locus fully-recessive cosmetic marking with a strong, specific visual identity; left for intake/tools/founders.py to derive an actual rate."
  ]
}
```

ELEMENT: the off-center bright glint inside each eyespot's dark core.
SHIPPED: a centered SPOTS disc (radius 0.25, white) sharing the same seed/spacing/vary as the other bands.
WHY NOT: SPOTS and RINGS always measure distance to the shared cell centre from seed+spacing; there is no way to author a sub-feature whose own distance field is measured from a deliberately shifted point while the other bands keep using the true centre, so the highlight can only land dead-center, never as the offset comma the reference shows.

ELEMENT: restricting the pattern to "black or grey" coat only, excluding white and every other coat colour.
SHIPPED: LUMA channel "dark" (smoothstep 0.0-0.55) as a brightness gate.
WHY NOT: LUMA as specified only exposes brightness ("dark"/"white"); it has no way to measure saturation. A pale dapple-grey can sit well above 0.55 and get wrongly excluded, while a sufficiently dark bay or liver chestnut can sit below 0.55 and get wrongly included — the actual test needed is achromatic-ness, a different measurement than brightness, not a different threshold on the one already offered.

PROPOSAL centerOffset (parameter on SPOTS and RINGS): "centerOffset": {x: number (body units, default 0), y: number (body units, default 0)}. Measures the same seed+spacing placement field as today, but the falloff for this one mask instance is computed from the cell centre shifted by (x,y) in the part's local frame, while other masks sharing the same seed+spacing keep the unshifted centre. Coverage 0/1 keep their existing SPOTS/RINGS meaning (0 = outside the shifted circle, 1 = at the shifted centre). Closest existing: SPOTS/RINGS placement itself; the one new thing is letting one band of a shared-cell composite sit off-centre from the rest.

PROPOSAL LUMA channel "chroma" (parameter addition to LUMA): adds an enum value "chroma" alongside "dark"/"white", using the existing "from"/"to" smoothstep fields (default from 0.0 to 0.15). Measures the resolved colour's saturation at a texel — how far it sits from grey, independent of how light or dark that grey is. Coverage 1 = fully achromatic (black, white, or any grey), coverage 0 = a fully saturated hue. Closest existing: LUMA's other channels, which read the same post-gradient resolved colour; the one new thing is reading colourfulness instead of brightness, which is the axis "black or grey but not other colours" actually depends on.