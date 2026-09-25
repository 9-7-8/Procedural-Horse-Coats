package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.SpecGene;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A band has to reach something.</b> Every banded knob on a breed must be one
 * the expression its own pinned alleles produce actually <i>reads</i>.
 *
 * <h2>What went wrong, and why nothing caught it</h2>
 * The Nightmare banded emberveins' {@code hue} to 0-10, with a note explaining
 * that this would read redder and less orange than the gene's full range. But
 * {@code hue} is referenced only by that gene's <b>rust</b> expression; the
 * <b>glowing</b> expression the breed's {@code Emb/Emb} actually produces paints
 * fixed hex and never looks at it. So the band stored a number, drifted it down
 * the generations, and drew nothing, while the file claimed a visible difference.
 *
 * <p>Nothing in the repository could have caught it. The gene key was real, the
 * knob name was real, the range was inside the knob's declared range, and
 * {@link BreedFilesTest} passed - the breed parser only warns about a band whose
 * <i>name</i> it cannot find, which this one had. The missing question was the
 * one below, and it is only answerable because {@code "$hue"} survives parsing as
 * a {@link GeneSpec.Value.FromKnob} index rather than being resolved away into an
 * opaque supplier.
 *
 * <h2>What this can and cannot see</h2>
 * Only data-driven genes. A hand-written Java gene declares its epi values in
 * {@code epiSchema()} and reads them in Java, where there is nothing to walk - so
 * a dead band on {@code MoltenHoovesGene} would still get through. The test
 * counts what it skipped and says so, rather than reporting a coverage it does
 * not have.
 *
 * <p>The rule is deliberately the weak one: a band fails only when <b>no</b> pair
 * in the breed's pool reads it. A mixed pool where some pairs read a knob and
 * others do not is ordinary and correct - the band is for the ones that do.
 */
class BreedBandsReachTest {

    @Test
    void everyBandedKnobIsReadByAnExpressionTheBreedCanActuallyProduce() {
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        int skippedNotSpec = 0;
        int skippedUnpinned = 0;

        for (Breed breed : Breeds.all()) {
            BreedBands bands = breed.bands();
            for (String geneKey : bands.genes()) {
                Gene gene = Genes.byKeyOrNull(geneKey);
                if (!(gene instanceof SpecGene spec)) {
                    // A hand-written gene reads its knobs in Java. Nothing to walk.
                    skippedNotSpec += named(bands, geneKey).size();
                    continue;
                }
                List<AllelePair> pool = pinnedPairs(breed, geneKey);
                if (pool.isEmpty()) {
                    // The breed bands a locus it does not pin, so the founder rolls
                    // the gene's own table and "which expression" has no one answer.
                    skippedUnpinned += named(bands, geneKey).size();
                    continue;
                }

                Set<String> readable = knobsReadBy(spec, pool);
                for (String valueName : named(bands, geneKey)) {
                    checked++;
                    if (!readable.contains(valueName)) {
                        wrong.add(breed.id() + ": " + geneKey + " bands '" + valueName
                                + "', which no expression of " + tokens(pool)
                                + " reads - the band would be stored and inherited "
                                + "and draw nothing. Knobs those pairs do read: "
                                + new TreeSet<>(readable));
                    }
                }
            }
        }

        assertTrue(wrong.isEmpty(), wrong.size() + " band(s) reach nothing:\n" + String.join("\n", wrong));
        // Guard the guard: if a refactor made this walk find nothing, it would go
        // green on every breed at once and say so in the same voice as success.
        assertTrue(checked > 0, "this test checked no bands at all, which cannot be right");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Every value name this breed bands or pins a seed for, at one gene. */
    private static Set<String> named(BreedBands bands, String geneKey) {
        Set<String> all = new LinkedHashSet<>(bands.forGene(geneKey).keySet());
        // A locked seed nobody reads is the identical bug in the other map.
        all.addAll(bands.seedsFor(geneKey).keySet());
        return all;
    }

    /**
     * Every pair a founder of this breed can carry at one gene, the breed's own
     * pool and every strain's together - a band applies to all of them.
     */
    private static List<AllelePair> pinnedPairs(Breed breed, String geneKey) {
        Set<AllelePair> pairs = new LinkedHashSet<>();
        if (breed.constrains(geneKey)) {
            pairs.addAll(breed.founderTable(geneKey).pairs());
        }
        for (Breed.Strain strain : breed.strains()) {
            if (strain.genePools().containsKey(geneKey)) {
                pairs.addAll(breed.founderTable(geneKey, strain).pairs());
            }
        }
        return new ArrayList<>(pairs);
    }

    /** The union of the knobs every one of these pairs' expressions reads. */
    private static Set<String> knobsReadBy(SpecGene gene, List<AllelePair> pairs) {
        GeneSpec spec = gene.spec();
        Set<String> names = new LinkedHashSet<>();
        for (AllelePair pair : pairs) {
            GeneSpec.ExpressionSpec expression = gene.expressionSpecOf(pair);
            if (expression == null) {
                continue;
            }
            Set<Integer> indices = new LinkedHashSet<>();
            for (GeneSpec.Layer layer : expression.layers()) {
                collect(layer.emissive(), indices);
                collect(layer.op(), indices);
                collect(layer.masks(), indices);
            }
            for (int i : indices) {
                if (i >= 0 && i < spec.knobs().size()) {
                    names.add(spec.knobs().get(i).name());
                }
            }
        }
        return names;
    }

    /**
     * Walk anything for knob references. Recursive and type-driven rather than a
     * list of the places a {@code Value} can currently sit, so a params shape
     * added later is walked without anybody remembering to come back here.
     */
    private static void collect(Object o, Set<Integer> out) {
        if (o instanceof GeneSpec.Value.FromKnob knob) {
            out.add(knob.index());
        } else if (o instanceof GeneSpec.Value) {
            // Const and PerDose read no knob.
        } else if (o instanceof GeneSpec.Region region) {
            collect(region.hue(), out);
            collect(region.span(), out);
            collect(region.saturation(), out);
            collect(region.lightness(), out);
        } else if (o instanceof GeneSpec.Op op) {
            collect(op.params(), out);
        } else if (o instanceof GeneSpec.Mask mask) {
            collect(mask.params(), out);
        } else if (o instanceof GeneSpec.Params params) {
            collect(params.raw(), out);
        } else if (o instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                collect(v, out);
            }
        } else if (o instanceof Iterable<?> each) {
            for (Object v : each) {
                collect(v, out);
            }
        }
        // anything else - a colour, a part list, a point array - holds no knob
    }

    private static String tokens(List<AllelePair> pairs) {
        Set<String> out = new TreeSet<>();
        for (AllelePair pair : pairs) {
            out.add(pair.toTokens());
        }
        return out.toString();
    }
}
