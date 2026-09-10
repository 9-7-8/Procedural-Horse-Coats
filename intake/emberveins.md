```json
{
  "id": "emberveins",
  "name": "Emberveins",
  "blurb": "Fine seams trace the coat like cracked stone about to give way, glowing molten orange in the dominant form and settling to a quiet rust vein-work in the recessive one, with drifting embers and a single wandering thread of light caught along the flank.",
  "notes": [
    "The reference image's three broad tones (near-black, cream, blue-grey) are treated as the horse's base coat showing through the cracks, per project convention, not as part of this marking — this gene draws only the vein network, embers and filament layered above whatever base coat is already there.",
    "Vein network uses the CRACKLE + SMOOTHSTEP combo established for webbed_marking (wing venation): CRACKLE gives distance-to-wall, SMOOTHSTEP(from=$veinWidth, to=0.4) keeps only the band nearest the wall as a thin line. cellSize and veinWidth are both knobs so the network scales and thins independently per horse.",
    "Both alleles draw the identical CRACKLE/SPOTS/FRACTAL geometry from the same $seed/$emberSeed/$filamentSeed so littermates of either genotype share a recognizable vein layout; only the op (COLOR-emissive vs PIGMENT-hue) differs, per the white/coloured pairing convention.",
    "The recessive filament layer is intentionally fainter (higher threshold, no emissive) rather than omitted, so the 'coloured' form still reads as the same creature with the light turned down, not a different marking.",
    "See TOOL GAP below: the reference's veins visibly pool and thicken where cracks meet and thin along the mid-span between junctions. CRACKLE's wall-distance field is uniform along its length, so this file ships a constant-width vein instead — a viewer will see even, wire-like lines with no swelling at the branch points."
  ],
  "knobs": [
    { "name": "seed", "type": "seed" },
    { "name": "emberSeed", "type": "seed" },
    { "name": "filamentSeed", "type": "seed" },
    { "name": "hue", "type": "float", "min": 0, "max": 360, "default": 8 },
    { "name": "cellSize", "type": "float", "min": 3.5, "max": 8.0, "default": 5.5 },
    { "name": "veinWidth", "type": "float", "min": 0.08, "max": 0.3, "default": 0.16 },
    { "name": "emberSpacing", "type": "float", "min": 2.0, "max": 5.0, "default": 3.2 },
    { "name": "emberSize", "type": "float", "min": 0.15, "max": 0.5, "default": 0.28 },
    { "name": "spread", "type": "float", "min": 0.0, "max": 0.4, "default": 0.1 },
    { "name": "filamentWarp", "type": "float", "min": 0.2, "max": 0.8, "default": 0.45 }
  ],
  "alleles": [
    { "id": "Em", "label": "Emberveins (glowing)" },
    { "id": "em", "label": "Emberveins (ember-marked)" }
  ],
  "expressions": [
    {
      "when": ["Em/Em", "Em/em"],
      "description": "Molten seams glow along the coat wherever the hide would naturally crease, brightest at a scatter of ember points and one thin drifting thread of light across the flank.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            { "type": "CRACKLE", "seed": "$seed", "scale": "$cellSize", "stretch": 1.0, "combine": "MULTIPLY" },
            { "type": "SMOOTHSTEP", "from": "$veinWidth", "to": 0.4, "combine": "MULTIPLY" }
          ],
          "op": { "type": "COLOR", "color": "#ff5a1f", "emissive": true }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            { "type": "SPOTS", "seed": "$emberSeed", "spacing": "$emberSpacing", "radius": "$emberSize", "jitter": 0.6, "vary": 0.3, "combine": "MULTIPLY" }
          ],
          "op": { "type": "COLOR", "color": "#ff8a3d", "emissive": true }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "FRACTAL", "seed": "$filamentSeed", "shape": "ridged", "octaves": 3, "warp": "$filamentWarp", "threshold": 0.85, "softness": 0.04, "combine": "MULTIPLY" }
          ],
          "op": { "type": "COLOR", "color": "#ffd9c2", "emissive": true }
        }
      ]
    },
    {
      "description": "The same cracked seams show as quiet rust-dark vein-work with a scatter of ash-dark freckling and a single faint hairline thread on the flank, no glow to any of it.",
      "layers": [
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            { "type": "CRACKLE", "seed": "$seed", "scale": "$cellSize", "stretch": 1.0, "combine": "MULTIPLY" },
            { "type": "SMOOTHSTEP", "from": "$veinWidth", "to": 0.4, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PIGMENT", "channel": "hue", "hue": "$hue", "spread": "$spread" }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["ALL"] },
            { "type": "SPOTS", "seed": "$emberSeed", "spacing": "$emberSpacing", "radius": "$emberSize", "jitter": 0.6, "vary": 0.3, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PIGMENT", "channel": "darkness", "target": 0.8, "spread": 0.05 }
        },
        {
          "masks": [
            { "type": "PARTS", "parts": ["BODY"] },
            { "type": "FRACTAL", "seed": "$filamentSeed", "shape": "ridged", "octaves": 3, "warp": "$filamentWarp", "threshold": 0.92, "softness": 0.04, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PIGMENT", "channel": "hue", "hue": "$hue", "spread": 0.05 }
        }
      ]
    }
  ],
  "rarity": "LEGENDARY"
}
```

ELEMENT: the vein network's variable thickness — thick pooled nodes where cracks meet, thinning to hairlines between junctions.
SHIPPED: CRACKLE's wall-distance field thresholded once by SMOOTHSTEP(from=$veinWidth, to=0.4), giving a single constant-width line along the entire network. A viewer will see even, wire-like seams with no swelling at branch points, unlike the reference's pooled junctions.
WHY NOT: this needs a distance-to-nearest-VERTEX measurement (junctions swell) blended against the distance-to-nearest-EDGE measurement CRACKLE already provides (mid-span stays thin). CRACKLE only exposes the latter; no combination of existing masks recovers vertex proximity, since SPOTS/DAPPLES measure distance to a cell center, not to a cell-wall corner.
PROPOSAL: parameter "vertexWeight" (float, 0-1, default 0) on CRACKLE. At a texel, blends the existing wall-distance value with a new distance-to-nearest-lattice-vertex value by that weight; 0 keeps current edge-only behavior, 1 replaces it with pure vertex-distance. Coverage 0 means far from any vertex (or wall, at weight 0), coverage 1 means at a vertex (or wall). Closest existing mask: CRACKLE — the one thing this adds is letting a downstream SMOOTHSTEP threshold thicken at junctions while staying thin along edges, which pure wall-distance cannot do.