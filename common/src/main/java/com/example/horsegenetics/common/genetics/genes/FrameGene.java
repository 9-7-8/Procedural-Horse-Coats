package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Frame overo</b> ({@code horsegenetics.frame}) - {@code Ov} dominant for the
 * coat, {@code ov} wild-type. Natural, <b>non-deterministic</b>.
 *
 * <p>The inverse constraint of <a href="">tobiano</a>: bold white patches with
 * <b>jagged edges</b> on the <b>sides of the neck and barrel</b> that
 * <b>never reach the topline</b> - a frame horse keeps a solid-coloured spine
 * and (usually) solid legs, and the white is "framed" by colour top and bottom.
 * Plus the broad white face frame is named for.
 *
 * <p>Mechanically: a warped fractal field ({@link PatchNoise#field}) is turned
 * to white where it clears a threshold, and that threshold is <b>lowered on the
 * mid-height flank</b> (a {@code sideWeight} that is zero at the spine and at
 * the belly) and <b>wobbled at a fine scale</b> so the patch margins are ragged
 * rather than smooth. The face is always at least blazed, up to a full bald
 * face.
 *
 * <p><b>Health is out of scope for now.</b> {@code Ov/Ov} is overo lethal white
 * syndrome in real horses; here the homozygote renders as an ordinary frame
 * (the lethal is a to-do on {@code wiki/roadmap.html} §4.2 / §6.4).
 *
 * <p>Three knobs off the expressing {@code Ov} copy: {@code nextLong()} (seed),
 * {@code nextFloat()} for how much body white, {@code nextFloat()} for how much
 * of the face is white.
 */
public final class FrameGene implements Gene {

    public static final String KEY = "horsegenetics.frame";
    public static final int WILD_FRAME_ALLELE_ODDS = 55;

    private static final double SCALE = 0.19;
    /** How much of the flank band is white, before the jagged edge - deliberately high, frame is bold. */
    private static final double COVER_MIN = 0.52;
    private static final double COVER_RANGE = 0.22;
    /** Fine-scale threshold wobble - the source of the ragged edge. */
    private static final double JAG = 0.15;
    private static final double JAG_FREQ = 3.1;
    /** The flank band, as fractions of the body's height: above the belly, below the topline. */
    private static final double BAND_LO = 0.28;
    private static final double BAND_HI = 0.74;
    /** Face-blaze half-width at z==0, body units: a floor (always a blaze) + a roll toward bald face. */
    private static final double FACE_HALF_MIN = 0.9;
    private static final double FACE_HALF_RANGE = 2.6;

    public final Allele Ov = new Allele(KEY, "Ov", "Frame overo (Ov)", true, false);
    public final Allele ov = new Allele(KEY, "ov", "Wild-type (ov)", false, true);
    private final List<Allele> alleles = List.of(Ov, ov);

    @Override public String key() { return KEY; }
    @Override public int priority() { return 74; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return ov; }

    /** Dominant for the coat (see the class note on the homozygote / lethal white). */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_FRAME_ALLELE_ODDS) == 0 ? Ov : ov,
                rng.nextInt(WILD_FRAME_ALLELE_ODDS) == 0 ? Ov : ov);
    }

    public boolean isFrame(AllelePair pair) {
        return pair.has(Ov);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isFrame(pair);
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isFrame(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isFrame(pair)) {
            return null;
        }
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double cover = COVER_MIN + epi.nextFloat() * COVER_RANGE;
        double faceHalf = FACE_HALF_MIN + epi.nextFloat() * FACE_HALF_RANGE;
        double threshold = 1.0 - cover;

        Skin skin = ctx.skin();
        HorseSkinGeometry.Bounds bb = HorseSkinGeometry.bodyBounds(skin);
        double span = bb.span(Axis.Y);
        double bandLo = bb.yMin() + span * BAND_LO;
        double bandHi = bb.yMin() + span * BAND_HI;
        double feather = span * 0.10;

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (Math.abs(point.z()) <= faceHalf) {
                    f.setRed(px, py, 0f);
                    f.setBlack(px, py, 0f);
                }
                return;
            }
            if (part != Part.BODY && part != Part.NECK) {
                return; // legs, crest, tail, ears stay coloured - white never reaches the topline
            }
            // The flank band, in absolute body-space Y so it does not depend on
            // which face a texel is on: zero at the belly and at the topline.
            double side = PatchNoise.smoothstep(bandLo - feather, bandLo, point.y())
                    * (1.0 - PatchNoise.smoothstep(bandHi, bandHi + feather, point.y()));
            if (side <= 0) {
                return;
            }
            double v = PatchNoise.field(seed, point.x(), point.y(), point.z(), SCALE);
            double jag = JAG * (PatchNoise.fbm2(seed ^ 0x5AB0L,
                    point.x() * JAG_FREQ, point.y() * JAG_FREQ, point.z() * JAG_FREQ * 1.4) - 0.5);
            if (v + jag <= threshold + (1.0 - side) * 0.6) {
                return; // outside the band the bar has to clear a much higher threshold
            }
            f.setRed(px, py, 0f);
            f.setBlack(px, py, 0f);
        });
        return f;
    }

}
