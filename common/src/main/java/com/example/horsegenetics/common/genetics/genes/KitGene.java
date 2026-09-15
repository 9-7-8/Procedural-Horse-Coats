package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeRequest;
import com.example.horsegenetics.common.genetics.eye.EyeRequestContribution;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.EyeSpread;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>{@code KIT}</b> ({@code horsegenetics.kit}) - the white-patterning
 * neighbourhood on equine chromosome 3, and the mod's <b>widest</b> locus.
 * It replaces the old {@code horsegenetics.white} (dominant white) and
 * {@code horsegenetics.sabino} ({@code SB1}) genes, which modelled two alleles
 * of <i>one</i> gene as two independent genes and so let a horse be
 * homozygous-sabino and dominant-white at the same time - a genotype that
 * cannot exist, because a horse has two copies of chromosome 3 and no more.
 *
 * <h2>What is and is not at this locus</h2>
 * {@code SB1} and the numbered {@code W} series are all <b>defined {@code KIT}
 * variants</b>, so they are alleles of this one gene. Two patterns that are
 * often filed next to them are <b>not</b>:
 * <ul>
 *   <li><b>Tobiano</b> is a large inversion <i>near / downstream of</i>
 *       {@code KIT}, not a {@code KIT} variant - {@link TobianoGene}.</li>
 *   <li><b>Roan</b> maps to the region but its causal change is unresolved and
 *       breed-dependent - {@link RoanGene}.</li>
 * </ul>
 * Both can therefore co-occur with anything here, which is exactly what real
 * horses do: a tobiano can also carry {@code W20}.
 *
 * <h2>The alleles</h2>
 * <b>Every published one</b> - {@code W1} to {@code W39}, less {@code W29} and {@code W36}, which were never
 * assigned (researched 2026-09-15 against the 2024 McFadden review, OMIA and the per-allele papers). The {@code W}
 * number is a <b>discovery order</b>, not a severity ranking, so nothing here can be inferred from the numbers. The
 * anchors below paint as the table says; every other allele takes the outcome of the anchor it matches in both extent
 * and homozygote viability (the groups are on the fields):
 * <table>
 *   <tr><th>allele</th><th>one copy</th><th>two copies</th></tr>
 *   <tr><td>{@code N}</td><td>-</td><td>wild type</td></tr>
 *   <tr><td>{@code W35}</td><td>nothing visible, to sabino-like</td><td><b>viable</b>, modestly more</td></tr>
 *   <tr><td>{@code W32}</td><td>nothing visible, to mild sabino-like</td><td><b>viable</b>, modestly more</td></tr>
 *   <tr><td>{@code W34}</td><td>nothing visible, to sabino-like</td><td><b>viable</b>, modestly more</td></tr>
 *   <tr><td>{@code W20}</td><td>subtle: ordinary face / leg white</td><td><b>viable</b>, modestly more</td></tr>
 *   <tr><td>{@code SB1}</td><td>classic sabino-1</td><td><b>viable</b>, sabino-white (90%+)</td></tr>
 *   <tr><td>{@code W15}</td><td>sabino-like through very white</td><td><b>viable</b> - an all-white homozygote is on record</td></tr>
 *   <tr><td>{@code W5}</td><td>sabino-like, broad</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W10}</td><td>sabino-like, broad</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W13}</td><td>extensive white spotting</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W23}</td><td>near-white to all white</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W22}</td><td>sabino-like, broad; <b>all white and masking beside a booster</b></td><td><b>nonviable</b></td></tr>
 * </table>
 * Most of the long tail is one founder, one family or one de novo foal, so it has no wild frequency and reaches the
 * game through the breeds it was found in, or a splice. Only {@code W19}, {@code W31} and {@code W3} turned up in the
 * one large tested population, and only those three are in the wild table.
 *
 * <h2>The boosters are the common part of this locus</h2>
 * <b>{@code W35}, {@code W32}, {@code W34} and {@code W20} are a group</b>, and
 * between them they are most of what this locus actually does to a population.
 * All four are mild, all four have viable homozygotes, all four range from "no
 * visible white beyond ordinary markings" to frank sabino-like spotting, and
 * all four add white beside any other variant here. {@code W20} is the famous
 * one - detected in twenty-five of twenty-eight surveyed breeds - but
 * {@code W35} is <i>commoner still</i> in the one large commercial dataset that
 * measured it, and {@code W32} is not far behind.
 *
 * <p>That matters because the mod used to carry {@code W20} alone and treat the
 * rest of the locus as a museum of rare founder alleles. The rare alleles are
 * real and they are here, but a horse's chance of carrying <i>something</i> at
 * {@code KIT} is set by the boosters, and one booster copy is an ordinary horse
 * with a star and a sock.
 *
 * <h2>What is deliberately not modelled</h2>
 * <ul>
 *   <li><b>Haplotype phase.</b> Several {@code KIT} changes can sit on the same
 *       physical chromosome - {@code W22} is usually reported linked with
 *       {@code W20}, and one 2024 dataset found horses carrying six variants
 *       across their two copies. A horse here has two alleles at this locus and
 *       no more, because an allele pair is the model. Linkage is a system, not a
 *       number.</li>
 *   <li><b>{@code W34}'s interaction with {@code MC1R}.</b> Its effect is
 *       reported as possibly stronger on a chestnut background. "Possibly" is
 *       doing a lot of work in that sentence, and buying it costs {@code W34} an
 *       outcome of its own.</li>
 *   <li><b>Pigment lost with age</b> ({@code W1}, and reportedly {@code W3}). A coat has no age axis, so these
 *       render as the adult.</li>
 * </ul>
 *
 * <h2>Viability, and what {@code canOccur} means here</h2>
 * The four {@code W} alleles UC Davis lists as "homozygosity thought nonviable"
 * get {@link #canOccur} {@code false}: those are <i>embryonic</i> lethals, so
 * there is no such horse to give a pen to and none in the founder population.
 * That is the same statement the sex locus makes about {@code Y/Y}.
 *
 * <p><b>Compound heterozygotes are allowed</b> - {@code W5/W13}, {@code W22/W5}
 * and the rest. The evidence is about an allele paired <i>with itself</i>; the
 * breeding advice is to avoid crossing carriers of <i>the same exact</i>
 * variant, and horses carrying two different strong {@code W}s are recorded. So
 * the rule is one allele twice, not two strong alleles.
 *
 * <p>Where the evidence runs the other way it is taken the other way:
 * {@code W15}'s homozygote is <b>on record and all white</b>, so it is allowed
 * and lands on the near-white outcome. "No homozygote has been found" and "a
 * homozygote has been found" are different claims and the table says which is
 * which.
 *
 * <h2>How one copy and two relate</h2>
 * There is no dose arithmetic here and deliberately so. Some of these alleles
 * have a real viable dose series ({@code SB1}), one is a <b>booster</b> that is
 * subtle alone and adds white beside another variant ({@code W20}), and most
 * are "dominant with variable expression" - one copy is already effective and
 * the phenotype is not neatly intermediate. Only a combination table can say
 * all three at once, so {@link #expressionOf} is a table.
 *
 * <p>Every painted outcome is the same {@link WhitePattern#sabino} shape at a
 * different strength - ragged margins growing inward from legs, belly and face -
 * because that is how the source describes the whole series: "sabino-like
 * through nearly all-white". Only {@code W22}'s dominant white is a different
 * painter, because all-white is not a lot of sabino, it is the absence of
 * pigment cells altogether.
 *
 * <p>Natural. Every outcome but the wild type and dominant white is
 * <b>non-deterministic</b>. See {@code wiki/gene-kit.html}.
 */
public final class KitGene implements Gene, EyeRequestContribution,
        WhitePatternEyes.WhiteExtent {

    public static final String KEY = "horsegenetics.kit";

    /**
     * Where each outcome sits on {@link WhitePattern#sabino}'s ramp. Tuned so
     * the ladder reads as distinct steps in-game rather than as a smooth blur:
     * a {@code W20} horse should look like an ordinary horse with a star, and a
     * sabino-white should be unmistakably not just a bold sabino.
     */
    private static final double S_MINIMAL = 0.12;
    private static final double S_MODEST = 0.24;
    private static final double S_SABINO = 0.42;
    private static final double S_BROAD = 0.58;
    private static final double S_EXTENSIVE = 0.74;
    private static final double S_NEAR_WHITE = 0.93;
    /** The speckled outcome's density and face strength - see {@link WhitePattern#speckled}. */
    private static final double S_SPECKLED = 0.55;

    // Declaration order is AllelePair's canonical slot order and nothing else -
    // it is not a dominance ranking. Strongest first reads best in a code string.
    // The anchors keep their relative order; each long-tail allele sits beside the
    // anchor whose outcome it takes.
    public final Allele W22 = new Allele(KEY, 0, "W22", "Dominant white (W22)");
    /**
     * <b>Camarillo white.</b> The second all-white allele at this locus, and a
     * different mutation rather than another name for the first: it traces to
     * one stallion, Sultan, born about 1912 and bought by Adolfo Camarillo in
     * 1921, and the line he founded is still a named one. A {@code W4} horse is
     * white from birth with pink skin and stays that way - which is what
     * separates it from a grey, and is the whole point of the allele existing
     * in a mod that also has grey.
     */
    public final Allele W4 = new Allele(KEY, 1, "W4", "Camarillo white (W4)");

    // All white on one copy, homozygote not seen: W22's outcome. W2 Thoroughbred (1946 founder); W9 Holsteiner
    // (2006); W11 South German Draft (1997); W14 Japanese Thoroughbred (1996 mare); W17 Japanese Draft, one horse,
    // with one blue eye; W24 Italian Trotter, a de novo mare (2014).
    public final Allele W2 = new Allele(KEY, 2, "W2", "Dominant white (W2)");
    public final Allele W9 = new Allele(KEY, 3, "W9", "Dominant white (W9)");
    public final Allele W11 = new Allele(KEY, 4, "W11", "Dominant white (W11)");
    public final Allele W14 = new Allele(KEY, 5, "W14", "Dominant white (W14)");
    public final Allele W17 = new Allele(KEY, 6, "W17", "Dominant white (W17)");
    public final Allele W24 = new Allele(KEY, 7, "W24", "Dominant white (W24)");

    public final Allele W13 = new Allele(KEY, 8, "W13", "White spotting (W13)");
    public final Allele W23 = new Allele(KEY, 9, "W23", "White spotting (W23)");
    // Near-white with some pigment left, homozygote not seen: W13 / W23's outcome. W1 Franches-Montagnes, the mare
    // Cigale (1957), losing pigment with age; W3 Arabian, R Khasper (1996), measured at 1.8e-4; W7 Thoroughbred,
    // one colt (2005); W16 Oldenburg, the mare Celene (2003); W25 Australian Thoroughbred, Laughyoumay (2001),
    // sometimes blue-eyed; W27 Australian Thoroughbred, Milady Fair (1960), into jumpers and Paints; W30 Berber,
    // de novo in Aghilasse; W39 a stock-type mare (2025).
    public final Allele W1 = new Allele(KEY, 10, "W1", "White spotting (W1)");
    public final Allele W3 = new Allele(KEY, 11, "W3", "White spotting (W3)");
    public final Allele W7 = new Allele(KEY, 12, "W7", "White spotting (W7)");
    public final Allele W16 = new Allele(KEY, 13, "W16", "White spotting (W16)");
    public final Allele W25 = new Allele(KEY, 14, "W25", "White spotting (W25)");
    public final Allele W27 = new Allele(KEY, 15, "W27", "White spotting (W27)");
    public final Allele W30 = new Allele(KEY, 16, "W30", "White spotting (W30)");
    public final Allele W39 = new Allele(KEY, 17, "W39", "White spotting (W39)");

    public final Allele W10 = new Allele(KEY, 18, "W10", "White spotting (W10)");
    public final Allele W5 = new Allele(KEY, 19, "W5", "White spotting (W5)");
    // Sabino-like to all white, homozygote not seen: W5 / W10's outcome. W6 Thoroughbred, one colt (2004); W8
    // Icelandic, mottled; W12 Thoroughbred, a de novo colt that died in 2010; W18 Swiss Warmblood, one speckled mare;
    // W21 Icelandic, speckled; W26 Australian Thoroughbred, always on a W20 chromosome; W28 German Riding Pony, one
    // horse; W31 Quarter Horse, Cookin Merada, measured at 6.0e-4; W33 Standardbred, a de novo filly; W37 Anglo-Arabian
    // and Zangersheide, always beside W35; W38 Polish Sporthorse, sometimes blue-eyed. (W8, W18 and W21 speckle rather
    // than spot, which is not painted - see the class note.)
    public final Allele W6 = new Allele(KEY, 20, "W6", "White spotting (W6)");
    public final Allele W8 = new Allele(KEY, 21, "W8", "White spotting (W8)");
    public final Allele W12 = new Allele(KEY, 22, "W12", "White spotting (W12)");
    public final Allele W18 = new Allele(KEY, 23, "W18", "White spotting (W18)");
    public final Allele W21 = new Allele(KEY, 24, "W21", "White spotting (W21)");
    public final Allele W26 = new Allele(KEY, 25, "W26", "White spotting (W26)");
    public final Allele W28 = new Allele(KEY, 26, "W28", "White spotting (W28)");
    public final Allele W31 = new Allele(KEY, 27, "W31", "White spotting (W31)");
    public final Allele W33 = new Allele(KEY, 28, "W33", "White spotting (W33)");
    public final Allele W37 = new Allele(KEY, 29, "W37", "White spotting (W37)");
    public final Allele W38 = new Allele(KEY, 30, "W38", "White spotting (W38)");
    /**
     * The one strong {@code W} with a <b>documented viable homozygote</b>: a
     * {@code W15/W15} horse is on record and it is all white. It is here to keep
     * the locus honest about the difference between "no homozygote has been
     * found" and "a homozygote has been found", which every other strong allele
     * on this page is on the first side of.
     */
    public final Allele W15 = new Allele(KEY, 31, "W15", "White spotting (W15)");
    /**
     * W15's twin: sabino-like on one copy (bald face, white past the knees, belly spots), <b>a homozygote on
     * record</b>, and the best-measured allele of the long tail - 1.6e-3 in the tested population, in Arabians and
     * part-bred Arabians from the mare Fantasia Vu (1990). Most W19 chromosomes also carry W34 and W35, which a
     * one-allele-per-chromosome model cannot hold, so a W19 horse here is a little milder than in life.
     */
    public final Allele W19 = new Allele(KEY, 32, "W19", "White spotting (W19)");
    public final Allele SB1 = new Allele(KEY, 33, "SB1", "Sabino 1 (SB1)");
    public final Allele W20 = new Allele(KEY, 34, "W20", "White booster (W20)");
    /** The commonest {@code W} allele in the one large dataset that measured it. */
    public final Allele W35 = new Allele(KEY, 35, "W35", "White booster (W35)");
    public final Allele W32 = new Allele(KEY, 36, "W32", "White booster (W32)");
    public final Allele W34 = new Allele(KEY, 37, "W34", "White booster (W34)");
    public final Allele N = new Allele(KEY, 38, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(W22, W4, W2, W9, W11, W14, W17, W24,
            W13, W23, W1, W3, W7, W16, W25, W27, W30, W39,
            W10, W5, W6, W8, W12, W18, W21, W26, W28, W31, W33, W37, W38,
            W15, W19, SB1, W20, W35, W32, W34, N);

    /**
     * One copy removes every pigment and masks the coat. <b>Not {@code W22}</b> (owner, 2026-09-15, following the 2024
     * McFadden review and haplotype data): one {@code W22} copy is sabino-like, and it is all white only beside a
     * booster, which is where every {@code W22} chromosome measured was found. See {@link #expressionOf}.
     */
    private final List<Allele> allWhite = List.of(W2, W9, W11, W14, W17, W24);

    /** One copy is near-white with some colour left: the extensive outcome alone. */
    private final List<Allele> extensiveAlone = List.of(W13, W23, W1, W3, W7, W16, W25, W27, W30, W39);

    /** One copy is sabino-like and broad: the broad outcome alone. */
    private final List<Allele> broadAlone = List.of(W22, W10, W5, W15, W6, W12, W26, W28, W31, W33, W37, W38, W19);

    /**
     * One copy speckles or mottles white through the coat rather than spotting it (owner, 2026-09-15): {@code W8} and
     * {@code W21} in Icelandics (<i>yruskjottur</i>) and {@code W18} in a Swiss Warmblood. Nonviable doubled, strong
     * beside another strong allele, like the rest of their group.
     */
    private final List<Allele> speckledAlone = List.of(W8, W18, W21);

    /**
     * The alleles whose homozygote is thought nonviable: the four UC Davis lists, and every long-tail allele the 2024
     * review predicts lethal because no homozygote has been seen. {@code W15} and {@code W19} have homozygotes on
     * record, and {@code W4}'s is not known to be lethal.
     */
    private final List<Allele> lethalWhenDoubled = List.of(W22, W2, W9, W11, W14, W17, W24,
            W13, W23, W1, W3, W7, W16, W25, W27, W30, W39,
            W10, W5, W6, W8, W12, W18, W21, W26, W28, W31, W33, W37, W38);

    /**
     * The alleles that already produce broad-to-extensive white on their own;
     * two of them together produce more, whichever two they are.
     */
    private final List<Allele> strong = List.of(W22, W13, W23, W1, W3, W7, W16, W25, W27, W30, W39,
            W10, W5, W15, W6, W8, W12, W18, W21, W26, W28, W31, W33, W37, W38, W19);

    /**
     * The <b>boosters</b>: mild, viable, additive, and between them most of what
     * this locus does to a population. One copy is ordinary markings; two is a
     * little more; beside any other variant here they add a step.
     */
    private final List<Allele> boosters = List.of(W20, W35, W32, W34);

    /**
     * Alleles that sit on the same chromosome as a booster in every horse measured - {@code W26} on {@code W20},
     * {@code W37} on {@code W35}, {@code W19} on {@code W34} and {@code W35} - so each counts as carrying one (owner,
     * 2026-09-15). The model holds one allele per chromosome, and without this those horses lost the booster they
     * really carry and painted a step milder than in life. Inheritance is unchanged: the booster travels with the
     * allele, which is what linkage means.
     */
    private final List<Allele> linkedBooster = List.of(W26, W37, W19);

    private static boolean hasAny(AllelePair pair, List<Allele> group) {
        return group.contains(pair.first()) || group.contains(pair.second());
    }

    private boolean hasBooster(AllelePair pair) {
        return boosterCount(pair) > 0;
    }

    private int boosterCount(AllelePair pair) {
        int n = 0;
        if (boosters.contains(pair.first()) || linkedBooster.contains(pair.first())) {
            n++;
        }
        if (boosters.contains(pair.second()) || linkedBooster.contains(pair.second())) {
            n++;
        }
        return n;
    }

    private final Expression WILD = Expression.wildType("No congenital white markings.");

    private final Expression MINIMAL = Expression.of("minimal-white", "Minimal white")
            .describe("Ordinary-looking white: a star or snip and a low sock or two, the sort of "
                    + "marking nobody would call a pattern. This is what a single booster copy - "
                    + "W20, W35, W32 or W34 - usually does on its own. They are boosters, and they "
                    + "show properly beside another variant.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_MINIMAL));

    private final Expression MODEST = Expression.of("modest-white", "Modest white")
            .describe("A little more than ordinary: a narrow blaze, socks climbing past the fetlock, "
                    + "maybe a fleck under the belly. Two booster copies are viable - the same one "
                    + "twice or two different ones - and do add white, but not reliably twice as "
                    + "much as one.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_MODEST));

    private final Expression SABINO = Expression.of("sabino", "Sabino")
            .describe("Classic sabino: tall jagged stockings, white up the belly, a broad blaze, and "
                    + "roaned ragged margins rather than the clean edge a splash marking leaves.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_SABINO));

    private final Expression BROAD = Expression.of("broad-white", "Broad white spotting")
            .describe("Sabino carried much further: irregular white well up the legs and flanks, a "
                    + "wide blaze or bald face, and the first real patches on the barrel.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_BROAD));

    private final Expression EXTENSIVE = Expression.of("extensive-white", "Extensive white")
            .describe("Most of the horse is white, with colour holding out over the topline, the "
                    + "crest and the quarters. The margins are still ragged - this is a very white "
                    + "spotted horse, not a white one.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_EXTENSIVE));

    private final Expression NEAR_WHITE = Expression.of("near-white", "Near-white")
            .describe("Ninety per cent white or more, with a few coloured flecks left on the ears and "
                    + "the crest. Sabino-white, and where the strong W alleles land when they meet "
                    + "each other.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_NEAR_WHITE));

    private final Expression DOMINANT_WHITE = Expression.of("dominant-white", "Dominant white")
            .describe("Every pigment gone over the whole body and pink skin underneath, so the horse "
                    + "renders pure white and no other coat gene it carries can be seen. This is an "
                    + "absence of pigment cells, not a great deal of sabino.")
            .masking()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
                    field.whiten(px, py, 1f);
                });
                return f;
            });

    /**
     * The same horse as {@link #DOMINANT_WHITE} to look at, and a separate
     * outcome anyway.
     *
     * <p>It could have shared {@code W22}'s: they paint identically, because
     * "no pigment cells reached the coat" has one appearance. It does not,
     * because the two differ in the thing a breeder actually acts on -
     * {@code W22}'s homozygote is thought nonviable and {@code W4}'s is not
     * known to be - and because a Camarillo white is a <i>named line</i> rather
     * than a generic white horse. A gene dictionary that answered "dominant
     * white" for it would be throwing away the only fact about it worth having.
     */
    private final Expression CAMARILLO_WHITE = Expression.of("camarillo-white", "Camarillo white")
            .describe("White from birth over the whole body, with pink skin underneath - not a grey "
                    + "that turned white, and not a great deal of sabino. Every other coat gene it "
                    + "carries is hidden and still inherited. Unlike the other all-white variant at "
                    + "this locus, two copies are not known to be nonviable.")
            .masking()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
                    field.whiten(px, py, 1f);
                });
                return f;
            });

    private final Expression SPECKLED = Expression.of("speckled-white", "Speckled white")
            .describe("White flecks and mottling scattered through the coat, thickest low on the body and thinning toward "
                    + "the topline, with a white face - not the torn-edged patches of sabino. The Icelandic word for it "
                    + "is yruskjottur.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.speckled(ctx, coat, KEY, S_SPECKLED));

    private final List<Expression> expressions =
            List.of(WILD, MINIMAL, MODEST, SABINO, BROAD, EXTENSIVE, NEAR_WHITE,
                    DOMINANT_WHITE, CAMARILLO_WHITE, SPECKLED);

    /**
     * Founder allele frequencies. {@code W20} is genuinely common in some
     * breeds, {@code SB1} uncommon, and every strong {@code W} is rare -
     * these are all founder-effect alleles traced to individual horses. The
     * four lethal homozygotes are dropped and the rest rescaled, which is what
     * a real adult population <i>is</i>.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), this::canOccur);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(W22, 0.001);
        // Rarer than any of them: one Californian line, from one 1912 stallion,
        // dispersed at auction in 1987. It is in the wild table at all rather
        // than being breed-only because every other allele here is a
        // founder-effect allele traced to an individual horse too.
        p.put(W4, 0.0004);
        p.put(W13, 0.002);
        p.put(W23, 0.003);
        p.put(W10, 0.004);
        p.put(W5, 0.005);
        p.put(W15, 0.001);
        // The only three of the long tail found in the 8,334-horse tested population (McFadden 2024): W19 1.6e-3,
        // W31 6.0e-4, W3 1.8e-4. Scaled the way W15 and W22 already are. The rest have no wild frequency.
        p.put(W19, 0.001);
        p.put(W31, 0.0005);
        p.put(W3, 0.0002);
        p.put(SB1, 0.022);
        // The boosters. The one large commercial dataset puts W35 at 0.168 and
        // W32 at 0.061, and W20 reaches 0.34 in sampled Thoroughbreds - but that
        // dataset is horses whose owners paid for a colour test, which is a
        // population selected for having something to find. These are that
        // ordering at roughly half the magnitude, with the breeds that really do
        // carry them saying so in their own tables.
        p.put(W35, 0.090);
        p.put(W32, 0.035);
        p.put(W20, 0.085);
        p.put(W34, 0.010);
        p.put(N, 0.7399);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "KIT (white spotting)"; }
    @Override public int priority() { return 76; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * <b>The table.</b> Every combination, nine outcomes, written as a
     * cascade because that is how the source reads: strongest allele first,
     * then what the second copy adds.
     *
     * <p>It still answers for the four combinations {@link #canOccur} rules
     * out - parsing is tolerant, so a hand-written code can name one, and the
     * nearest sensible outcome beats throwing.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        // W22 is the strong dominant white: one copy already removes essentially
        // all pigment, and nothing another allele does can be seen under it.
        if (hasAny(pair, allWhite)) {
            return DOMINANT_WHITE;
        }
        // W22 is all white only beside a booster; alone it is a broad sabino-like white (2024 review, AWS 19.5).
        if (pair.has(W22) && hasBooster(pair)) {
            return DOMINANT_WHITE;
        }
        // W4 removes the pigment just as completely. It sits below W22 only so
        // that the one combination carrying both reads as the older name; the
        // horse is the same white either way.
        if (pair.has(W4)) {
            return CAMARILLO_WHITE;
        }
        // Two strong alleles - the same one twice, or two different ones - and
        // the horse is near-white however they got there.
        if (strong.contains(pair.first()) && strong.contains(pair.second())) {
            return NEAR_WHITE;
        }
        // W13 and W23 are the two the source calls all-white or near-all-white
        // on a single copy, so they start a step above W5 / W10.
        if (hasAny(pair, extensiveAlone)) {
            return pair.has(SB1) || hasBooster(pair) ? NEAR_WHITE : EXTENSIVE;
        }
        if (hasAny(pair, speckledAlone)) {
            return SPECKLED;
        }
        if (hasAny(pair, broadAlone)) {
            if (pair.has(SB1)) {
                return NEAR_WHITE;
            }
            return hasBooster(pair) ? EXTENSIVE : BROAD;
        }
        if (pair.has(SB1)) {
            // The clearest viable dose series in the whole locus, and the reason
            // the boosters are worth having: SB1 + W20 is visibly more than SB1
            // alone, and that is measured rather than assumed.
            if (pair.homozygousFor(SB1)) {
                return NEAR_WHITE;
            }
            return hasBooster(pair) ? BROAD : SABINO;
        }
        if (hasBooster(pair)) {
            // Two booster copies - the same one twice or two different ones.
            return boosterCount(pair) >= 2 ? MODEST : MINIMAL;
        }
        return WILD;
    }

    /**
     * The {@code W} alleles whose homozygote is thought nonviable have no
     * horse to describe. A <b>compound</b> heterozygote of two of them is a
     * different question and is allowed - see the class javadoc.
     */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !(pair.homozygous() && lethalWhenDoubled.contains(pair.first()));
    }

    /**
     * How many combinations {@link #canOccur} rules out - one per allele whose
     * homozygote is thought nonviable. An accessor rather than a number,
     * because the number moves every time the locus grows and a test that
     * quotes it goes red for the wrong reason.
     */
    public int nonviableHomozygotes() {
        return lethalWhenDoubled.size();
    }

    /**
     * Does this combination remove every pigment everywhere? Either all-white
     * allele does - {@code W22} or {@code W4} - and callers care about the
     * <i>absence</i>, not about which mutation caused it.
     */
    public boolean isDominantWhite(AllelePair pair) {
        return hasAny(pair, allWhite) || pair.has(W4) || (pair.has(W22) && hasBooster(pair));
    }

    /**
     * {@code KIT} qualifies for a blue eye only from {@code broad-white} upward.
     * A sabino with four socks and a blaze has ordinary dark eyes - the white on
     * it never reached the head - which is the difference between this locus and
     * splash, where even a modest marking comes with blue. See
     * {@link WhitePatternEyes}.
     */
    @Override
    public EyeRequest requestEyes(AllelePair pair, Genotype genotype,
            com.example.horsegenetics.common.genetics.Epigenome epigenome) {
        Expression e = expressionOf(pair);
        boolean broad = e == BROAD || e == SPECKLED || e == EXTENSIVE || e == NEAR_WHITE
                || e == DOMINANT_WHITE || e == CAMARILLO_WHITE;
        return WhitePatternEyes.blueIf(broad, this, pair, genotype, epigenome);
    }

    /**
     * Roughly how much of the horse this combination leaves unpigmented - the
     * painter's own strength constants, which is the honest answer since they
     * are literally what the sabino field is scaled by. Dominant and Camarillo
     * white are 1: there is nothing left.
     */
    @Override
    public double whiteness(AllelePair pair) {
        Expression e = expressionOf(pair);
        if (e == MINIMAL) {
            return S_MINIMAL;
        }
        if (e == MODEST) {
            return S_MODEST;
        }
        if (e == SABINO) {
            return S_SABINO;
        }
        if (e == BROAD) {
            return S_BROAD;
        }
        if (e == SPECKLED) {
            return S_SPECKLED;
        }
        if (e == EXTENSIVE) {
            return S_EXTENSIVE;
        }
        if (e == NEAR_WHITE) {
            return S_NEAR_WHITE;
        }
        if (e == DOMINANT_WHITE || e == CAMARILLO_WHITE) {
            return 1.0;
        }
        return 0.0;
    }

    /**
     * The sabino shape - the noise field, one sock height per leg, the belly and
     * the shared face marking - plus the eye spread, since a loud {@code KIT}
     * horse can claim a blue eye and the spread is read off whichever gene wins.
     */
    @Override
    public EpiSchema epiSchema() {
        return WhitePattern.sabinoSchema().and(EyeSpread.schema().values().toArray(new EpiValue[0]));
    }

}
