package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.CommonLog;
import com.example.horsegenetics.common.horse.Sex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>The right-hand column of the horse dimension: every visually affecting
 * allele combination of every gene nobody has confirmed by eye yet</b>, on four
 * base coats, worst-first.
 *
 * <h2>Which genes</h2>
 * A gene is in this plan when its wiki page carries <b>no {@code Verified}
 * block</b> - the convention hard rule 9 sets up, where a confirmed thing is
 * deleted from its own page's <b>Verification tab</b> and written up on that
 * same page's coding tab. So "has somebody looked at this in the game?" is
 * already recorded, once, in the place the answer belongs, and this reads it
 * rather than keeping a second list that would drift. The list is baked to a
 * classpath resource by {@code UnverifiedGeneTool} because the wiki is not in
 * the jar.
 *
 * <p>Two filters are applied <b>here</b> rather than at bake time, so the
 * resource stays stable while the build it runs in decides what it means:
 * <ul>
 *   <li>a gene that is not registered in this build is skipped - an absence from
 *       {@code genes/index.json} is a killswitch (hard rule 6), and a parked
 *       gene must not come back as a pen;</li>
 *   <li>a gene that {@link Gene#affectsCoat() paints nothing} is skipped. The
 *       newest unverified genes are mostly health and behaviour loci - they
 *       change no pixel, so a pen of them would be four identical horses.</li>
 * </ul>
 *
 * <h2>What order</h2>
 * Newest first, by the gene timeline, which is derived from git: the commit that
 * first wrote the gene's key. A painter written last week is where a drawing bug
 * actually is, and one that has been on a horse for a month has been looked at
 * by accident if not on purpose. Genes the timeline cannot date (their key never
 * appears as a literal) go last, in registry order, rather than being dropped.
 *
 * <h2>What is in one pen</h2>
 * One combination, four horses: {@link Base#BLACK}, {@link Base#BAY},
 * {@link Base#CHESTNUT} and {@link Base#WHITE} side by side. A marking that is
 * invisible on black is usually obvious on chestnut, and the white horse is the
 * masking check - {@code KIT}'s dominant white removes every pigment, so
 * <b>anything still painted on that horse is a gene ignoring a mask</b>.
 *
 * <p>Pure, and free of the game: the NeoForge module turns a {@link Pen} into
 * blocks and horses.
 */
public final class CoatCheckPlan {

    /** Where {@code UnverifiedGeneTool} writes the gene keys, one per line. */
    private static final String RESOURCE = "/horsegenetics/unverified-genes.txt";

    private CoatCheckPlan() {
    }

    /**
     * The four base coats a combination is shown on. Each is a whole genotype in
     * its own right - extension and agouti decide the first three, and the
     * fourth is a black horse carrying one copy of a {@code KIT} all-white
     * allele.
     */
    public enum Base {
        BLACK("black"),
        BAY("bay"),
        CHESTNUT("chestnut"),
        WHITE("white");

        private final String label;

        Base(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /**
         * This base coat and nothing else - every other locus at its wild type.
         *
         * <p>{@link Genotype#wildType()} is already a black horse (extension
         * defaults to {@code E}, agouti to {@code a}), so black states what it
         * is anyway rather than relying on that staying true.
         */
        public Genotype genotype() {
            Genotype g = Genotype.wildType()
                    .with(new AllelePair(Genes.EXTENSION.E, Genes.EXTENSION.E))
                    .with(new AllelePair(Genes.AGOUTI.a, Genes.AGOUTI.a));
            switch (this) {
                case BAY:
                    return g.with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A));
                case CHESTNUT:
                    return g.with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e));
                case WHITE:
                    // One copy is enough: W2 is in KitGene's allWhite list, where a
                    // single copy removes every pigment and masks the coat. NOT W22,
                    // which is sabino-like alone and all-white only beside a booster.
                    return g.with(new AllelePair(Genes.KIT.W2, Genes.KIT.N));
                default:
                    return g;
            }
        }
    }

    /**
     * One pen: a gene and one combination of it, shown <b>four times over</b> -
     * once per {@link Base}. {@link #genotype(Base)} is what each of the four
     * horses in that pen carries.
     *
     * <p>Four in one pen rather than four pens in a row, because the question
     * these pens exist to answer is "does this marking work on every base?" and
     * that is a question you answer by <i>looking at them together</i>. Four
     * consecutive pens would make it a memory test and a corridor four times
     * longer.
     */
    public record Pen(Gene gene, AllelePair pair) {

        /** The base coat with this combination written over it. */
        public Genotype genotype(Base base) {
            return base.genotype().with(pair).withSex(sex());
        }

        /**
         * A mare, unless this combination is one a mare cannot carry. A
         * sex-linked gene's hemizygous form holds the reserved placeholder in
         * its spare slot, and that is a stallion - showing it on a mare would be
         * a horse whose genotype does not exist.
         */
        public Sex sex() {
            // ASK WHETHER THE GENE IS SEX-LINKED FIRST. hemizygousPlaceholder()
            // THROWS on an autosomal gene - it does not return null - so the
            // null check below it never ran, and every pen in the plan is
            // autosomal. That made the corridor's first pen throw while the
            // player was still being teleported in: a crash on entering the
            // horse dimension, reported by testers against 0.5.004 and
            // reproduced by CoatCheckPlanSanityTest on all 438 pens.
            if (!gene.inheritance().sexLinked()) {
                return Sex.FEMALE;
            }
            return pair.has(gene.hemizygousPlaceholder()) ? Sex.MALE : Sex.FEMALE;
        }

        /** What this combination is called - the expression's own name. */
        public Expression expression() {
            return gene.expressionOf(pair);
        }
    }

    private static List<Pen> pens;

    /**
     * Every pen in the plan, in the order they are built down the corridor.
     * Computed once - it walks every registered gene's combination table, which
     * is cheap but not free, and the corridor asks for it per segment.
     */
    public static synchronized List<Pen> pens() {
        if (pens == null) {
            pens = build();
        }
        return pens;
    }

    /**
     * How many pens the right-hand column needs - one per combination, each
     * holding {@link Base#values()} horses. The corridor's length comes from
     * this, so it falls every time a gene's {@code Verified} block is written.
     */
    public static int size() {
        return pens().size();
    }

    /** The {@code index}-th pen, or {@code null} once the plan is exhausted. */
    public static Pen at(int index) {
        List<Pen> all = pens();
        return index < 0 || index >= all.size() ? null : all.get(index);
    }

    private static List<Pen> build() {
        List<Pen> out = new ArrayList<>();
        for (Gene gene : unverifiedGenes()) {
            for (AllelePair pair : GenotypeCatalog.distinctPairsOf(gene)) {
                if (gene.expressionOf(pair).wildType()) {
                    continue;   // the plain horse - not a thing to look at
                }
                if (!gene.canOccur(pair)) {
                    continue;
                }
                out.add(new Pen(gene, pair));
            }
        }
        return List.copyOf(out);
    }

    /**
     * The registered, painting genes whose page carries no {@code Verified}
     * block, newest first. Undated genes are appended in registry order.
     */
    public static List<Gene> unverifiedGenes() {
        List<Gene> out = new ArrayList<>();
        List<String> keys = bakedKeys();
        for (String key : keys) {
            Gene gene = Genes.byKeyOrNull(key);
            if (gene == null || !gene.affectsCoat() || out.contains(gene)) {
                continue;
            }
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI) {
                // The base coat itself, not a marking on one. Every pen already
                // shows four of these, so a pen "of extension" would be the four
                // bases again with a different label - the same exclusion, and
                // for the same reason, that ShowcaseGenotypes makes.
                continue;
            }
            out.add(gene);
        }
        return List.copyOf(out);
    }

    /**
     * The baked list, or an empty list if it is missing. Empty rather than a
     * throw on purpose: a missing resource must cost the right-hand column, not
     * the dimension - the left column and the yard are still worth walking into.
     */
    private static List<String> bakedKeys() {
        List<String> keys = new ArrayList<>();
        try (InputStream in = CoatCheckPlan.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                CommonLog.warn("No " + RESOURCE + " on the classpath - "
                        + "run :common:bakeUnverifiedGenes. The coat-check column will be empty.");
                return keys;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.trim();
                if (!key.isEmpty() && !key.startsWith("#")) {
                    keys.add(key);
                }
            }
        } catch (IOException e) {
            CommonLog.warn("Could not read " + RESOURCE + ": " + e);
        }
        return keys;
    }
}
