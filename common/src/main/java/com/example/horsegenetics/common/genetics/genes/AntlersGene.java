package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AntlerGenerator;
import com.example.horsegenetics.common.parts.AntlerSize;
import com.example.horsegenetics.common.parts.AttachedPart;

import java.util.List;
import java.util.Optional;

/**
 * <b>Antlers</b> ({@code horsegenetics.antlers}) - a <b>magical, dominant</b> gene
 * that grows a rack of antlers, and the second gene in the mod that makes geometry
 * rather than texels. Three alleles, in a dominance series:
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>any {@code Ant}</td><td>{@code antlers} - antlers on mares and stallions alike (a reindeer)</td></tr>
 *   <tr><td>{@code Antm} without {@code Ant}</td><td>{@code antlers-stallion} - antlers on a stallion;
 *       a mare carries it and shows nothing (a red deer)</td></tr>
 * </table>
 *
 * <h2>Sex-limitation is a render gate, not a genotype rule</h2>
 * {@code Antm} is "stallions only", and that is <b>not</b> decided in
 * {@link #expressionOf}: making a gene's expression read another locus is a
 * cross-locus read the coat pipeline avoids. It is safe for a part for the reason
 * the cutie mark's emblem is - a part is drawn after the coat, nothing reads it
 * back, and it is not in the coat's texture key - so {@link #racksFor} asks
 * {@link Genotype#sex()} and the gate lives there. The consequence, said on the
 * gene page and in the expression: the genotype alone does not show the rule, and
 * a mare's {@code Antm/n} reads the same as a stallion's.
 *
 * <h2>The numbers are on the copy</h2>
 * Every rack is its own, from seven epigenetic values on the expressing copy (the
 * dominant one - {@code Ant} over {@code Antm}). Sorted by what they cost to change,
 * as the horn's are:
 * <ul>
 *   <li>{@code rack} (a seed) picks one of {@link AntlerGenerator#VARIANTS} racks,
 *       and {@code size}'s class picks the mesh - the topology;</li>
 *   <li>{@code tines} is how many of the mesh's tines show, fractional - a
 *       visibility and one scale at draw time;</li>
 *   <li>{@code size}'s remainder, {@code reach}, {@code girth} and {@code tint} are
 *       a transform and a colour - free.</li>
 * </ul>
 * {@code asymmetry} is the odd one: <b>every founder rolls exactly 0 and drift never
 * moves it</b> (D3, owner) - its design range is zero wide, which is what makes both
 * true without any special case in the epigenome. Only a breed's band can put it
 * above zero, and then it does two things to the left antler: past
 * {@link #ASYMMETRIC} it grows a different rack instead of mirroring the right, and
 * throughout it carries fewer tines and a smaller beam in proportion.
 *
 * <h2>Antlers are permanent</h2>
 * No casting, no velvet, no year clock (D4, owner). A horse grows its rack once,
 * at maturity: a foal shows none ({@code PartKind.showsOnFoal}).
 *
 * <p>The habit - forked, brow-tined, palmate, crowned or spike - is another locus,
 * {@link AntlerFormGene}, and glow, crystal and bloom are three more; this one decides
 * <i>whether</i> and <i>how big</i>. {@code GrownParts.of} puts them together.
 */
public final class AntlersGene implements Gene {

    public static final String KEY = "horsegenetics.antlers";

    /** Magical band, the first of a free run of five for the antler loci. Only a code-order slot. */
    public static final int PRIORITY = 230;

    /**
     * Founders, in percent, as pairs - written out by hand rather than from an
     * allele frequency, the flying gene's precedent for a rare gene that should not
     * be flattened by a bulk tool. About four horses in a thousand carry an antler
     * allele; {@code Antm} is the commoner of the two, and every homozygote is too
     * rare to be worth a row. Most stallion-only carriers that are mares show
     * nothing, which is the one invisible carrier this gene has, and it is the
     * biology rather than an oversight.
     */
    public static final double WILD_ANT_PERCENT = 0.14;
    /** @see #WILD_ANT_PERCENT */
    public static final double WILD_ANTM_PERCENT = 0.26;

    /** Past this much asymmetry the left antler grows a rack of its own rather than the mirror. */
    public static final double ASYMMETRIC = 0.1;

    public final Allele Ant = new Allele(KEY, 0, "Ant", "Antlers (Ant)");
    public final Allele Antm = new Allele(KEY, 1, "Antm", "Stallion antlers (Antm)");
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Ant, Antm, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression ANTLERS = Expression.wildType("antlers", "Antlers",
            "A rack of antlers growing from the top of the skull, on a mare or a stallion alike. "
                    + "Its size, how many tines it carries, its reach, its thickness and its "
                    + "colour are epigenetic and inherited with the allele, so a rack breeds "
                    + "true and a line selected for size keeps growing. Grown at maturity - a "
                    + "foal has none.");

    private final Expression STALLION = Expression.wildType("antlers-stallion", "Stallion antlers",
            "Antlers on a stallion only. A mare with this shows nothing and passes it on - "
                    + "the genotype alone will not tell you, so breed her to a stallion and look at "
                    + "the colts. Everything about the rack is epigenetic, as with the other "
                    + "allele.");

    private final List<Expression> expressions = List.of(WILD, ANTLERS, STALLION);

    private final FounderTable founders = FounderTable.builder()
            .weight(Ant, n, WILD_ANT_PERCENT)
            .weight(Antm, n, WILD_ANTM_PERCENT)
            .weight(n, 100.0 - WILD_ANT_PERCENT - WILD_ANTM_PERCENT)
            .build();

    // ------------------------------------------------------------------
    // The epigenetic numbers. Every one of these is read by racksFor and
    // honoured by AttachedPartLayer - AntlersGeneTest asserts it.
    // ------------------------------------------------------------------

    /** Position on the {@link AntlerSize} ladder: the class is the mesh, the rest a stretch. */
    public static final String SIZE = "size";
    /** Which rack - the seed the variant is cut from. */
    public static final String RACK = "rack";
    /** How many tines show; fractional grows the last one in. */
    public static final String TINES = "tines";
    /** Beam length on top of the size. */
    public static final String REACH = "reach";
    /** Thickness on top of the size. */
    public static final String GIRTH = "girth";
    /** How lopsided the rack is. Zero on every founder - see the class note. */
    public static final String ASYMMETRY = "asymmetry";
    /** Pale bone at 0 to dark umber at 1. */
    public static final String TINT = "tint";

    /** The two ends of {@link #TINT}. */
    static final int PALE_BONE = 0xFFEFE6D2;
    /** @see #PALE_BONE */
    static final int DARK_UMBER = 0xFF4B3524;

    @Override public String key() { return KEY; }
    @Override public String name() { return "Antlers"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.count(Ant) > 0) {
            return ANTLERS;
        }
        return pair.count(Antm) > 0 ? STALLION : WILD;
    }

    /**
     * Would this horse grow antlers - the genotype <i>and</i> the sex gate. The one
     * place {@code Antm}'s "stallions only" is applied.
     */
    public boolean grows(Genotype genotype) {
        AllelePair pair = genotype.pair(this);
        if (pair.count(Ant) > 0) {
            return true;
        }
        return pair.count(Antm) > 0 && genotype.sex() == Sex.MALE;
    }

    /**
     * <b>The design ranges.</b> Size is wild in the lower part of the ladder - most
     * antlered horses carry a small to standard rack - and clamped to the whole of
     * it, so a line bred for size climbs to a massive rack and no further. Tines are
     * wild from one to six and may be bred to the eight the largest class carries.
     * Asymmetry's design range is zero wide on purpose.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(SIZE, 0.0, 0.6).clampedTo(0.0, 1.0),
                EpiValue.seed(RACK),
                EpiValue.uniform(TINES, 1.0, 6.0).clampedTo(0.0, AntlerGenerator.MAX_TINES),
                EpiValue.uniform(REACH, 0.85, 1.15).clampedTo(0.6, 1.5),
                EpiValue.uniform(GIRTH, 0.85, 1.15).clampedTo(0.6, 1.5),
                EpiValue.uniform(ASYMMETRY, 0.0, 0.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(TINT, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * The pair of antlers this horse grows, right then left, in the forked habit and
     * bare - or empty for a horse without them. The habit is {@link AntlerFormGene}'s;
     * ask {@code GrownParts.of} for the finished rack.
     */
    public Optional<List<AttachedPart>> racksFor(Genotype genotype, Epigenome epigenome) {
        return racksFor(genotype, epigenome, AntlerGenerator.FORK);
    }

    /** As {@link #racksFor(Genotype, Epigenome)}, in the habit {@code form}. */
    public Optional<List<AttachedPart>> racksFor(Genotype genotype, Epigenome epigenome, int form) {
        if (!grows(genotype)) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        double size = epi.get(SIZE);
        double tines = epi.get(TINES);
        double reach = epi.get(REACH);
        double girth = epi.get(GIRTH);
        double asymmetry = clamp01(epi.get(ASYMMETRY));
        int tint = tintOf(epi.get(TINT));
        long rack = epi.seed(RACK);

        int right = variantOf(rack);
        int left = asymmetry > ASYMMETRIC ? secondVariantOf(rack, right) : right;
        // Continuous as well as stepped, so every value of the number shows: the
        // lopsided side is the left, and it loses up to half its tines and a
        // fifth of its beam at full asymmetry.
        double leftTines = tines * (1.0 - 0.5 * asymmetry);
        double leftSize = size - 0.2 * asymmetry;
        return Optional.of(List.of(
                AttachedPart.antler(false, form, right, size, tines, reach, girth, tint),
                AttachedPart.antler(true, form, left, leftSize, leftTines, reach, girth, tint)));
    }

    /** What to call this horse's rack - "a standard rack". Empty for a horse that has none. */
    public Optional<AntlerSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!grows(genotype)) {
            return Optional.empty();
        }
        return Optional.of(AntlerSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SIZE)));
    }

    /** The rack seed's render variant. A {@code long} genome, sixteen cached shapes. */
    static int variantOf(long rack) {
        return (int) Math.floorMod(mix(rack), (long) AntlerGenerator.VARIANTS);
    }

    /** The left antler's rack on an asymmetric horse: a second variant off the same seed, never the first. */
    static int secondVariantOf(long rack, int right) {
        int v = (int) Math.floorMod(mix(rack ^ 0x5DEECE66DL), (long) AntlerGenerator.VARIANTS);
        return v == right ? (v + 1) % AntlerGenerator.VARIANTS : v;
    }

    /** {@code t} on the bone-to-umber line, as opaque ARGB. */
    static int tintOf(double t) {
        double c = clamp01(t);
        int r = lerp(PALE_BONE >> 16, DARK_UMBER >> 16, c);
        int g = lerp(PALE_BONE >> 8, DARK_UMBER >> 8, c);
        int b = lerp(PALE_BONE, DARK_UMBER, c);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, double t) {
        a &= 0xFF;
        b &= 0xFF;
        return (int) Math.round(a + (b - a) * t);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    /** A 64-bit finaliser, so neighbouring seeds land on unrelated variants. */
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
        z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return z ^ (z >>> 33);
    }

}
