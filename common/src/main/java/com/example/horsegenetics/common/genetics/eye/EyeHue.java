package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * <b>The colour vocabulary the eye loci are written in</b> - one closed list,
 * shared by the iris loci, the heterochromia-colour loci and the sclera loci,
 * so that "gold" means one thing on a horse however it got there.
 *
 * <h2>Why a closed list and not a colour per gene</h2>
 * Every eye colour in the mod used to be declared by whichever gene claimed the
 * iris - champagne owned four shades, tiger eye owned two, the four white loci
 * shared one constant. That made "what colour are its eyes" a question you
 * answered by finding out which gene won, and it made an eye colour something
 * a gene could invent rather than something a horse could inherit.
 *
 * <p>The eyes are their own loci now, so the colours have to be
 * <b>alleles</b> - a fixed, nameable set a breeder can aim at. A gene that used
 * to paint an iris {@linkplain EyeRequest requests} one of these instead.
 *
 * <h2>Two of them are not colours</h2>
 * {@link #INVISIBLE} and {@link #CHAOS} are in the same list because they sit in
 * the same allele slot, not because they behave alike:
 * <ul>
 *   <li>{@link #INVISIBLE} paints <b>nothing</b>. Its texels are left showing
 *       whatever the coat underneath them is, which is why it has to be handled
 *       before the template's eyes are copied back - see
 *       {@code CoatRegions.redrawEyes}. It is the one hue that overrides
 *       everything further down the eye's paint stack.</li>
 *   <li>{@link #CHAOS} has no rgb of its own at all: it reads one off the
 *       {@linkplain EpiValues epigenetic values} of the allele copy carrying
 *       it, so two chaos-eyed horses are not the same colour and a line of them
 *       drifts. {@link #schema} is what a gene offering it must compose in.</li>
 * </ul>
 */
public enum EyeHue {

    /**
     * The wild type of both iris loci and the eye every ordinary horse has. A
     * warm mid brown rather than the template's flat black: the template's eye
     * is black because there is no room for an iris <i>and</i> a pupil at 128px,
     * and a horse that is genotypically brown-eyed should read as brown.
     */
    BROWN("Brn", "Brown", 0x53361B),

    GREEN("Grn", "Green", 0x4E7A45),

    /** The tiger-eye / champagne-amber end of the range. */
    GOLD("Gld", "Gold", 0xC8811E),

    DARK_BLUE("DBl", "Dark blue", 0x2F5A8C),

    /**
     * The blue of a splashed white or dominant white horse, and the wild type of
     * both heterochromia-colour loci. Pale and cool, and deliberately not
     * saturated: at two texels a bright cyan stops reading as an eye. This is
     * the value the four white loci request, and it is the one eye colour that
     * was a shared constant before the eye loci existed.
     */
    MID_BLUE("MBl", "Mid blue", 0x6FA8D8),

    LIGHT_BLUE("LBl", "Light blue", 0xA8CDE8),

    /** The wild type of both sclera loci, and a rare iris. */
    WHITE("Wht", "White", 0xE8E6E0),

    /** Sclera only - the iris loci do not offer it, the template already is it. */
    BLACK("Blk", "Black", 0x14100E),

    /** A colour off the allele copy. See the class note; {@link #schema} goes with it. */
    CHAOS("Cha", "Chaos", 0x000000),

    /** Paints nothing. See the class note. */
    INVISIBLE("Inv", "Invisible", 0x000000);

    private final String token;
    private final String label;
    private final int rgb;

    EyeHue(String token, String label, int rgb) {
        this.token = token;
        this.label = label;
        this.rgb = rgb;
    }

    /** The allele token a gene offering this hue declares. */
    public String token() {
        return token;
    }

    public String label() {
        return label;
    }

    /**
     * The fixed colour, {@code 0xRRGGBB}. Meaningless for {@link #CHAOS} and
     * {@link #INVISIBLE} - go through {@link #rgb(EpiValues)} instead, which is
     * the only call a painter should be making.
     */
    public int fixedRgb() {
        return rgb;
    }

    /** Does this hue paint at all? False for {@link #INVISIBLE} alone. */
    public boolean paints() {
        return this != INVISIBLE;
    }

    /** Does this hue differ from horse to horse? True for {@link #CHAOS} alone. */
    public boolean varies() {
        return this == CHAOS;
    }

    /**
     * What to paint with, on <b>this</b> horse. {@code epi} is the values of the
     * gene offering the hue; it may be {@link EpiValues#EMPTY} or carry no
     * {@link #CHAOS_PREFIX} channels, in which case chaos falls back to its
     * drab fixed value rather than throwing - a question asked about a genotype
     * rather than about a horse still deserves an answer.
     */
    public int rgb(EpiValues epi) {
        if (this != CHAOS) {
            return rgb;
        }
        if (epi == null || epi.isEmpty() || epi.schema().indexOf(CHAOS_PREFIX + "_r") < 0) {
            return CHAOS_FALLBACK;
        }
        return epi.rgb(CHAOS_PREFIX);
    }

    /** What a chaos hue comes out as when nobody has rolled one - a mid grey-violet. */
    public static final int CHAOS_FALLBACK = 0x6B5C7A;

    /** The {@link EpiValue#colour} prefix a gene offering {@link #CHAOS} stores under. */
    public static final String CHAOS_PREFIX = "eye_chaos";

    /**
     * The three channels {@link #CHAOS} reads. Composed into the schema of every
     * gene that offers the hue, whether or not this particular horse carries it
     * - a schema is a property of the gene, not of the horse.
     *
     * <p>The saturation and value floors are what stop a chaos eye coming out
     * black or grey: an iris is two texels, and an unconstrained random rgb is
     * a muddy one about half the time.
     */
    public static EpiValue[] schema() {
        return EpiValue.colour(CHAOS_PREFIX, 0.45, 1.0, 0.40, 1.0);
    }

    /**
     * <b>The hues an iris locus offers</b>, wild type first. The single source
     * of truth for the third eye's {@code defined} allele, which invents an
     * iris colour from nothing and must invent one a horse could also have
     * inherited; {@code EyeColourGene} declares the same list with a founder
     * frequency and a sentence against each, and {@code EyeGeneTest} asserts the
     * two agree.
     */
    public static final EyeHue[] IRIS_PALETTE = {
            BROWN, GREEN, GOLD, DARK_BLUE, MID_BLUE, LIGHT_BLUE, WHITE, CHAOS, INVISIBLE};

    /**
     * The hues a sector-colour locus offers. {@link #MID_BLUE} first, because
     * it is that locus's wild type; no {@link #INVISIBLE}, because a hole in an
     * iris is not something the eye model has a meaning for.
     */
    public static final EyeHue[] SECTOR_PALETTE = {
            MID_BLUE, GREEN, GOLD, DARK_BLUE, BROWN, LIGHT_BLUE, WHITE, CHAOS};

    /** The hues a sclera locus offers, wild type first. */
    public static final EyeHue[] SCLERA_PALETTE = {WHITE, BLACK, INVISIBLE, CHAOS};

    /** The hue under {@code token}, or {@code null}. */
    public static EyeHue byToken(String token) {
        for (EyeHue h : values()) {
            if (h.token.equals(token)) {
                return h;
            }
        }
        return null;
    }
}
