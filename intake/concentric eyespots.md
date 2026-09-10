```json
{
  "id": "concentric_eyespots",
  "name": "Concentric Eyespots",
  "phase": "magical",
  "category": "marking",
  "blurb": "A rare marking that traces butterfly-wing eyespots — nested rings of colour around a pale glinting core — down the neck and along all four legs.",
  "parts": ["NECK", "LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"],
  "knobs": [
    { "name": "legSeed", "type": "seed" },
    { "name": "neckSeed", "type": "seed" },
    { "name": "legSpacing", "type": "number", "default": 3.2, "min": 2.6, "max": 4.2 },
    { "name": "neckSpacing", "type": "number", "default": 6.2, "min": 5.2, "max": 7.6 },
    { "name": "stretch", "type": "number", "default": 1.3, "min": 1.0, "max": 1.8 },
    { "name": "vary", "type": "number", "default": 0.3, "min": 0.1, "max": 0.5 },
    { "name": "hue", "type": "number", "default": 26, "min": 0, "max": 360 }
  ],
  "alleles": [
    { "id": "W", "dominant": true },
    { "id": "c", "dominant": false }
  ],
  "expressions": [
    {
      "when": ["W/W", "W/c"],
      "description": "A faint, pale echo of the ring pattern ghosts along the neck and legs — visible mostly as a sheen, with no true colour.",
      "notes": [
        "Single-copy (or double-dominant) carriers draw the identical ring geometry as the colored form, just repainted in flat ivory tones per band, per the white/coloured pairing convention."
      ],
      "layers": [
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.0, "softness": 0.05 },
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.72, "softness": 0.05, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#d9d4c9" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.72, "softness": 0.05 },
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.46, "softness": 0.05, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#efe9df" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.46, "softness": 0.06 }
          ],
          "op": { "type": "PAINT", "color": "#f8f5ee" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 2.4, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.9, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#d9d4c9" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.9, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.3, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#e3ddd0" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.3, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.85, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#efe9df" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.85, "softness": 0.09 }
          ],
          "op": { "type": "PAINT", "color": "#f8f5ee" }
        }
      ]
    },
    {
      "when": ["c/c"],
      "description": "Vivid concentric eyespots — a near-black rim, a warm coloured band, and a pale glinting core — mark the neck and all four legs, like scattered butterfly wing-eyes.",
      "notes": [
        "This is the full four-band version on the neck and the compressed three-band version on the legs; see the gene-level notes for why the leg band count differs."
      ],
      "layers": [
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.0, "softness": 0.05 },
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.72, "softness": 0.05, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#0d0f1a" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.72, "softness": 0.05 },
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.46, "softness": 0.05, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "sat": 0.78, "light": 0.42 }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"], "space": "part", "seed": "$legSeed", "spacing": "$legSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.46, "softness": 0.06 }
          ],
          "op": { "type": "PAINT", "color": "#bfe9f2" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 2.4, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.9, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "color": "#0d0f1a" }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.9, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.3, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "sat": 0.8, "light": 0.4 }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 1.3, "softness": 0.08 },
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.85, "softness": 0.08, "invert": true, "combine": "MULTIPLY" }
          ],
          "op": { "type": "PAINT", "hue": "$hue", "sat": 0.4, "light": 0.68 }
        },
        { "masks": [
            { "type": "SPOTS", "parts": ["NECK"], "space": "local", "seed": "$neckSeed", "spacing": "$neckSpacing", "stretch": "$stretch", "stretchAxis": "y", "vary": "$vary", "radius": 0.85, "softness": 0.09 }
          ],
          "op": { "type": "PAINT", "color": "#bfe9f2" }
        }
      ]
    }
  ],
  "founders": { "rarity": "RARE" },
  "notes": [
    "Each ring band is a pair of SPOTS masks sharing one seed+spacing+stretch+vary and differing only in radius, folded with the smaller one inverted and MULTIPLY-combined, per the standard concentric-zone recipe — this is what keeps every band centred on the same point and scaled together by 'vary' rather than drifting independently.",
    "Legs get three bands (rim/ring/core) while the neck gets four (rim/ring/ring/core). The legs are only 4 body units wide; a fourth band there would be under a texel wide at 2 texels/unit and read as mush rather than a ring, so the leg version was deliberately simplified rather than shrinking every band to fit. The neck's ~12-unit width has room for the full reference detail.",
    "The rim (near-black) and core (pale icy highlight) are fixed colors rather than hue-linked, since they read as the eyespot's structural dark edge and glinting centre regardless of accent color — only the mid ring(s) carry '$hue' so the accent color varies per horse while the silhouette stays recognizable.",
    "legSpacing and neckSpacing are separate knobs because the two eyespot sizes differ by more than 2x; stretch and vary are shared since they describe proportional behavior (elongation, per-cell size jitter) rather than an absolute scale.",
    "W (pale ring echo) is dominant over c (fully coloured rings): both alleles paint the identical band geometry, so a single copy of c is invisible except as a faint ivory sheen, and only c/c shows the full vivid butterfly-eyespot coloring."
  ]
}
```