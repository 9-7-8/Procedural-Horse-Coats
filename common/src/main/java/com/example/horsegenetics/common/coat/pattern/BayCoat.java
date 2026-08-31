package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

/**
 * Builds a <b>bay</b> coat into a {@link CoatBuildContext}'s pigment field:
 * red-brown body, black points (mane, tail, ear tips, hooves), and black that
 * climbs a <b>random</b> amount up each leg and the face. The random heights
 * are the horse's epigenetic value - rolled once at birth, replayed here.
 *
 * <p>Reused by {@code AgoutiGene} for {@code A_} horses and callable directly.
 */
public final class BayCoat {

    /** How much black pheomelanin the body keeps - lower = redder body. */
    public static final float BODY_BLACK = 0.32f;
    /** Hooves are always black for at least this fraction of leg height. */
    public static final double HOOF_FRACTION = 0.12;

    private BayCoat() {}

    /** Roll the leg / face heights from {@code epi} and paint. Consumes 9 {@code nextFloat()}s. */
    public static void apply(CoatBuildContext ctx, Rng epi) {
        double[] legs = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < legs.length; i++) {
            // skew toward low socks: product of two uniforms
            legs[i] = 0.15 + epi.nextFloat() * epi.nextFloat() * 0.45;
        }
        double face = 0.05 + epi.nextFloat() * 0.30;
        apply(ctx, legs, face);
    }

    /** Paint with explicit heights (fractions of leg height / head length). */
    public static void apply(CoatBuildContext ctx, double[] legFractions, double faceFraction) {
        PigmentField f = ctx.pigment();

        // 1. bay body: keep the red, knock the black down everywhere
        CoatRegions.restrictAll(f, (field, px, py, p) -> field.setBlack(px, py, BODY_BLACK));

        // 2. hard black points
        CoatRegions.blackenPart(f, Part.MANE);
        CoatRegions.blackenPart(f, Part.TAIL);
        CoatRegions.blackenPart(f, Part.LEFT_EAR);
        CoatRegions.blackenPart(f, Part.RIGHT_EAR);

        // 3. black up the legs a random amount, hooves always black
        for (int i = 0; i < CoatRegions.LEGS.size(); i++) {
            Part leg = CoatRegions.LEGS.get(i);
            CoatRegions.blackenLowerLeg(f, leg, Math.max(HOOF_FRACTION, legFractions[i]));
        }

        // 4. black up the face a random amount (muzzle always)
        CoatRegions.blackenFace(f, faceFraction);
    }
}
