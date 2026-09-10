```json
{
  "id": "orchid_frost",
  "name": "Orchid Frost",
  "blurb": "Cold, soft-edged blotches settle across a dark coat, bleaching its black and plum tones toward candy pink wherever they land; on a coat already as pale as that pink, the frost finds nothing left to bite into and leaves no trace.",
  "notes": [
    "Reference: a black-magenta rose with irregular pale ellipses of very mixed size, edges sunk into the surrounding colour rather than cut. Modeled as one SPOTS field with high size variance and generous softness, not several layered masks, so the small droplets and the large patches stay part of one coherent scatter (rule 14).",
    "The 'no effect on a coat lighter than pink' rule is not a region exclusion — it is the LUMA mask's coverage genuinely reaching 0 once resolved luma reaches the chosen pink's own luma (~0.73 for #F7A8C4). SPOTS still draws its shape everywhere in the selected region; on a pale coat the fold just multiplies that shape by zero, which is why a cremello or a pre-existing pink-and-up coat shows nothing even though the blotches are 'there'.",
    "Layer space is body, not part, so the scatter reads continuously across the neck and head instead of compressing oddly at their pitch boundaries (rule 9) — this marking is meant to look like one field poured over the whole animal, the way the rose's blotching ignores petal boundaries.",
    "HAIR (mane + tail) is cut out with an inverted PARTS mask: this is a skin/coat bleaching effect, not a hair-pigment one, so mane and tail colour are left to whatever genes already govern them.",
    "Density (spacing) and blend ceiling (amount) are both perDose on the same two-outcome expression rather than split into separate knobs, because the only per-horse variation this trait needs beyond the dose step is placement/shape (seed, vary, stretch) — giving dose its own knob would just be another way of writing the same constant."
  ],
  "founders": { "rarity": "RARE" },
  "alleles": [
    {
      "name": "Frost",
      "knobs": [
        { "name": "seed", "type": "seed" },
        { "name": "stretch", "type": "float", "min": 1.15, "max": 1.75, "default": 1.4 },
        { "name": "vary", "type": "float", "min": 0.45, "max": 0.75, "default": 0.6 }
      ]
    },
    { "name": "Plain" }
  ],
  "expressions": [
    {
      "when": ["Frost/Frost", "Frost/Plain"],
      "description": "Pale, soft-edged blotches spread over the coat, bleaching its darker patches toward pink and passing over any part of the coat already that light or lighter. A double dose packs the blotches closer together and pushes the bleaching further toward pure pink; a single dose leaves them sparser and only partly faded.",
      "notes": [
        "Single layer; no ordering concerns since nothing else in this gene reads what it painted."
      ],
      "layers": [
        {
          "space": "body",
          "masks": [
            { "type": "PARTS", "parts": ["HAIR"], "invert": true },
            {
              "type": "SPOTS",
              "seed": "$seed",
              "spacing": { "perDose": { "1": 7.5, "2": 4.5 } },
              "radius": 2.2,
              "vary": "$vary",
              "stretch": "$stretch",
              "softness": 1.3,
              "mirror": false,
              "combine": "MULTIPLY"
            },
            {
              "type": "LUMA",
              "quality": "dark",
              "from": 0.0,
              "to": 0.73,
              "combine": "MULTIPLY"
            }
          ],
          "op": {
            "type": "PAINT",
            "color": "#F7A8C4",
            "amount": { "perDose": { "1": 0.55, "2": 1.0 } }
          }
        }
      ]
    },
    {
      "description": "No frosting; the coat keeps its natural colour and shading throughout."
    }
  ]
}
```