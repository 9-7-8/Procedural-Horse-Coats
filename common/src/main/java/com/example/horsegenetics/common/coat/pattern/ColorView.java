package com.example.horsegenetics.common.coat.pattern;

/**
 * A <b>read-only</b> look at a {@link ColorField} - the phase-3 colour
 * accumulator as the magical genes have left it so far, or one gene's delta.
 *
 * <p>Channels are the raw signed accumulators, <b>not</b> capped to 0-255: a
 * gene that wants to know "is this texel already blue past saturation" can see
 * that. {@link #argb} is the capped conversion.
 */
public interface ColorView {

    /** Sheet edge length in texels; the field is {@code size * size}. */
    int size();

    /** Accumulated red, signed and uncapped (0-255 is the nominal range). */
    int red(int px, int py);

    /** Accumulated green, signed and uncapped. */
    int green(int px, int py);

    /** Accumulated blue, signed and uncapped. */
    int blue(int px, int py);

    /**
     * Accumulated opacity, signed and uncapped. 0 = the white template shows
     * through untouched; 255 = this colour fully replaces it.
     */
    int opacity(int px, int py);

    /**
     * Only meaningful on a <b>delta</b>: this texel is flat paint that replaces
     * the accumulator rather than adding to it. See
     * {@link ColorField#set(int, int, int, int, int, int)}.
     */
    boolean isAbsolute(int px, int py);

    /** This texel capped into a real ARGB pixel. */
    int argb(int px, int py);

    /**
     * What this texel will actually <b>look like</b>, per channel, 0-255:
     * {@code colour * opacity + white * (1 - opacity)}, which is the overlay's
     * own contribution before {@code CoatTextureComposer} multiplies the white
     * template's shading into it.
     *
     * <p>It is not the same as {@link #argb}, and the difference matters to any
     * gene that wants to reason about the coat it is painting over. A texel the
     * natural phase left <b>transparent</b> reads as <b>white</b> here - because
     * that is what a viewer sees, the bald template - not as the black that
     * {@code argb} reports. A texel that resolved to pure black reads as 20%
     * grey, for the same reason.
     *
     * <p>{@code channel} is 0 red, 1 green, 2 blue.
     */
    default int visible(int px, int py, int channel) {
        int c = switch (channel) {
            case 0 -> red(px, py);
            case 1 -> green(px, py);
            case 2 -> blue(px, py);
            default -> throw new IllegalArgumentException("channel must be 0, 1 or 2, was " + channel);
        };
        int capped = c < 0 ? 0 : (c > 255 ? 255 : c);
        int op = opacity(px, py);
        double a = (op < 0 ? 0 : (op > 255 ? 255 : op)) / 255.0;
        return (int) Math.round(capped * a + 255 * (1 - a));
    }

    /** A private, writable copy of this state. */
    ColorField mutableCopy();
}
