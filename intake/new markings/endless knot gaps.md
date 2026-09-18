Good — the tool only needs the creator's `svg-path.js`. I'll set up that layout and run the real intake tool.TOOL GAP

**ELEMENT:** The knot "stretching a bit down on the sides." A marking that lies over the back and continues down both flanks as one unbroken drawing.

**SHIPPED:** An SVG mask on `plane: "top"` (z −6..6, x 5–21), multiplied by an `AXIS` Y band on BODY from `$sideDrop` (0.6–0.8) up to the top. The top of the back shows the knot correctly. On each flank, the side face reads only the single slice of the knot at z = ±5, repeated straight down, so a viewer sees vertical white bars under whichever arms cross the barrel's edge, fading out after a couple of units. The knot's outer ~1 unit on each side never appears at all.

**WHY NOT:** Every plane (`side`, `top`, `front`) is a straight projection, so a face parallel to the projection axis always gets one line of the drawing smeared across it. What's missing is a different measurement: distance travelled *around* the part's cross-section from the spine. Swapping planes or tuning the numbers can't produce it, and `NORMAL` and `EDGE` report facing and rims, not arc length.

**PROPOSAL:** A new `plane` value on `SVG` and `PATH`, rather than a new mask.
- `plane: "wrap"` — `u` is the body X (as `top`); `v` is signed arc length, in body units, along the part's box perimeter in the YZ cross-section, measured from the top-face centreline (0 on the spine, +z side positive, down the right flank continuing past the edge).
- Parameters: `wrapAxis` (X, default), and `from` = the part whose cross-section is walked (defaults to the mask's first part).
- At a texel it measures the texel's perimeter distance from the spine on its own part's box. Coverage is then the drawing's inside test at (x, v): 1 inside the drawn shape, 0 outside, exactly as today.
- The closest existing option is `plane: "top"`. The one thing this adds is that a drawing continues over the top edge onto the side faces at its true proportions, instead of being cut off at z = ±5 and smeared vertically.