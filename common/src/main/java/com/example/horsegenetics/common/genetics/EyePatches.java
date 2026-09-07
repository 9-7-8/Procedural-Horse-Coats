package com.example.horsegenetics.common.genetics;

import java.util.List;

/**
 * <b>One gene's painting of the two irises</b> - a list of {@link EyePatch}es
 * per eye, drawn over whatever the eye-colour channel already settled.
 *
 * <p>This is the second, deliberately blunter half of the eye channel. Where
 * {@link EyeColorContribution} answers "what colour is this horse's iris" and
 * has to be ranked because there is only one answer,
 * {@link EyePatchContribution} answers "and then paint <i>this</i> over part of
 * it", which composes: patches are absolute writes, applied in
 * {@link Genes#codeOrder()}, last writer to a quadrant wins - the same rule the
 * rest of the overlay phase runs on.
 *
 * <p>Patches are painted from the <b>unmodified</b> eye, not from the layer
 * beneath them (see {@code CoatOverlay.blendToward}), so every patch a gene
 * writes should use {@code strength} 1 unless it genuinely wants to blend with
 * the template's own iris rather than with the colour above it.
 *
 * @param right the eye on the head's west face - {@code CoatRegions.eyeRects} index 0
 * @param left  the east face - index 1
 */
public record EyePatches(List<EyePatch> right, List<EyePatch> left) {

    /** Nothing on either eye. */
    public static final EyePatches NONE = new EyePatches(List.of(), List.of());

    public EyePatches {
        right = List.copyOf(right);
        left = List.copyOf(left);
    }

    /** The same list on both eyes - rare; heterochromia is the point of this type. */
    public static EyePatches both(List<EyePatch> patches) {
        return new EyePatches(patches, patches);
    }

    public boolean empty() {
        return right.isEmpty() && left.isEmpty();
    }
}
