package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Pink hair</b> ({@code horsegenetics.pink_hair}) - a <b>magical</b> gene:
 * {@code Pihr} is <b>recessive</b>, so only {@code Pihr/Pihr} shows it and a
 * single copy is a carrier you can't see. {@code n} is wild-type. {@code 1 in}
 * {@value #WILD_PIHR_ALLELE_ODDS} per allele, so roughly 1 wild horse in
 * {@value #WILD_PIHR_HORSE_ODDS} is born with it and a good many more carry it -
 * which is the point: a recessive is something you <i>breed for</i>.
 *
 * <p>Turns the <b>mane and tail</b> pink. It is <b>not</b> flat paint - that
 * would throw away the shading the natural phase gave those strands and leave a
 * dead pink patch. Instead the gene <i>reads</i> what each hair texel currently
 * looks like ({@code ColorView.visible}) and returns the delta that walks it
 * {@value #STRENGTH_PERCENT}% of the way to hot pink, so the mane keeps its own
 * light and dark while ending up unmistakably pink on a black, a chestnut or a
 * cremello alike. It raises opacity too, so a dominant-white horse gets pink
 * hair rather than nothing.
 *
 * <p>A blind {@code add} was tried first and can't do this: to reach pink on a
 * black mane it has to push so hard that a pale mane saturates to white. Reading
 * first is the point of the phase-3 read access - the cost is that this gene is
 * <b>order-dependent</b>, so it runs before magic zebra (whose stripes should
 * black out pink hair, not the other way round). See {@code Genes.magicalOrder}.
 *
 * <p>Deterministic - one intensity, no per-horse variation yet. Alleles for a
 * couple of intensities are the obvious extension.
 *
 * <p><b>Foals get a pink tail only.</b> The foal mesh has no {@code MANE} part
 * (see {@code HorseSkinGeometry}), so the mane comes in with adulthood.
 */
public final class PinkHairGene implements Gene {

    public static final String KEY = "horsegenetics.pink_hair";
    public static final int WILD_PIHR_ALLELE_ODDS = 12;
    /** Both copies, so the square of the per-allele odds - for the Javadoc above. */
    public static final int WILD_PIHR_HORSE_ODDS = WILD_PIHR_ALLELE_ODDS * WILD_PIHR_ALLELE_ODDS;

    /** Hot pink, the colour the hair is walked toward. */
    private static final int PINK_R = 255;
    private static final int PINK_G = 105;
    private static final int PINK_B = 180;
    /** How far of the way there. Short of 100 so the strands keep their shading. */
    public static final int STRENGTH_PERCENT = 82;

    private static final List<Part> HAIR = List.of(Part.MANE, Part.TAIL);

    public final Allele n = new Allele(KEY, "n", "Wild-type (n)", false, true);
    public final Allele Pihr = new Allele(KEY, "Pihr", "Pink hair (Pihr)", true, true);
    /** Most-dominant first: the wild type is the dominant one here. */
    private final List<Allele> alleles = List.of(n, Pihr);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return n; }

    /** Recessive: {@code n/Pihr} is an invisible carrier; only {@code Pihr/Pihr} shows. */
    @Override public DominancePattern dominance() { return DominancePattern.RECESSIVE; }

    @Override
    public boolean isNatural() {
        return false;
    }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_PIHR_ALLELE_ODDS) == 0 ? Pihr : n,
                rng.nextInt(WILD_PIHR_ALLELE_ODDS) == 0 ? Pihr : n);
    }

    public boolean isPinkHaired(AllelePair pair) {
        return pair.first().equals(Pihr) && pair.second().equals(Pihr);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isPinkHaired(pair);
    }

    @Override
    public ColorField tint(AllelePair pair, CoatBuildContext ctx, PigmentView coat, ColorView colour) {
        if (!isPinkHaired(pair)) {
            return null;
        }
        Skin skin = ctx.skin();
        ColorField delta = ColorField.deltaLike(colour);
        for (Part part : HAIR) {
            if (!HorseSkinGeometry.hasPart(skin, part)) {
                continue; // a foal has no mane
            }
            HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> {
                // The hair ends up fully opaque, so what it will look like is
                // just the accumulated colour - hence the delta that lands there.
                delta.add(px, py,
                        toward(colour, px, py, 0, PINK_R),
                        toward(colour, px, py, 1, PINK_G),
                        toward(colour, px, py, 2, PINK_B));
                delta.addOpacity(px, py, 255 - colour.opacity(px, py));
            });
        }
        return delta;
    }

    /** The signed step from this texel's accumulated channel to its pink one. */
    private static int toward(ColorView colour, int px, int py, int channel, int target) {
        int seen = colour.visible(px, py, channel);
        int wanted = (int) Math.round(seen + (target - seen) * (STRENGTH_PERCENT / 100.0));
        return wanted - switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
    }
}
