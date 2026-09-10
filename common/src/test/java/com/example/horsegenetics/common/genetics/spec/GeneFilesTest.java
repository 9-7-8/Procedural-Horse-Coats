package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tripwire under {@code horsegenetics/genes/}: every shipped gene file
 * parses, registers, and lands somewhere sane.
 *
 * <p>It matters more than it looks. {@link Genes} loads these from its class
 * initialiser and a file that will not parse is <b>logged and skipped</b> - the
 * right behaviour in game, where one bad drop-in should not cost the player the
 * other eighty, and exactly the wrong behaviour in a build, where it means a
 * typo removes a gene from the world and nothing goes red.
 */
class GeneFilesTest {

    @Test
    void everyShippedGeneFileLoads() {
        GeneSpecLoader.Result result = GeneSpecLoader.fromClasspath();
        assertEquals(List.of(), result.errors(), "gene files that would not parse");
        assertTrue(result.specs().size() > 0, "the shipped gene index found nothing");
    }

    @Test
    void everyShippedGeneIsRegistered() {
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            assertTrue(Genes.byKeyOrNull(spec.key()) != null,
                    spec.key() + " parses but is not in the registry - did the class initialiser run?");
        }
    }

    /**
     * No two shipped genes share an allele <b>key</b>. Tokens repeat freely
     * across genes (half of them have an {@code n}); what must not repeat is the
     * {@code gene.token} pair {@link Genes#allele} resolves, and a bulk import
     * that copy-pasted a key is exactly how that would happen.
     */
    @Test
    void alleleKeysAreUnique() {
        Set<String> seen = new HashSet<>();
        List<String> clashes = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            for (com.example.horsegenetics.common.genetics.Allele allele : gene.alleles()) {
                if (!seen.add(allele.key())) {
                    clashes.add(allele.key());
                }
            }
        }
        assertEquals(List.of(), clashes, "duplicate allele keys");
    }

    /** A magical gene sits at 100 or above, a natural one below it. {@link Genes} only warns. */
    @Test
    void everyShippedGeneIsInItsPhaseBand() {
        List<String> wrong = new ArrayList<>();
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            boolean magicalBand = spec.priority() >= Genes.MAGICAL_BAND_START;
            if (magicalBand == spec.natural()) {
                wrong.add(spec.key() + " is " + (spec.natural() ? "natural" : "magical")
                        + " at priority " + spec.priority());
            }
        }
        assertEquals(List.of(), wrong, "genes outside their phase's priority band");
    }

    /**
     * No two <b>shipped gene files</b> share a priority.
     *
     * <p>Sharing one is legal - the registry breaks the tie on the key - but for
     * a data-driven gene it is never deliberate, because the tie-break is
     * alphabetical and so renaming the gene silently moves it in the paint
     * order. The hand-written genes do share a few (brindle and sooty both sit
     * at 36, and mean to); this deliberately does not police them.
     */
    @Test
    void shippedGenePrioritiesAreDistinct() {
        Set<Integer> seen = new HashSet<>();
        List<String> clashes = new ArrayList<>();
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            if (!seen.add(spec.priority())) {
                clashes.add(spec.key() + " @ " + spec.priority());
            }
        }
        assertEquals(List.of(), clashes, "two gene files share a priority - the paint order is then alphabetical");
    }

    /**
     * <b>A {@code WAVES} amplitude has to fit the space it is measured in.</b>
     *
     * <p>{@code from}, {@code to} and {@code amplitude} are in whatever
     * {@code space} says, while {@code wavelength} is <i>always</i> in body
     * units - and that asymmetry is a trap that has already been walked into
     * eleven times. A band along the neck's crest used to be faked with a
     * sawtooth longer than the horse ({@code wavelength} 90) at an
     * {@code amplitude} of 77.94, which is a tilted plane and is correct in
     * {@code units}. Six genes then copied the pair into {@code part} space,
     * where an amplitude of 77.94 displaces the band twenty to forty
     * <b>normalised spans</b> off the horse: all six selected exactly zero
     * texels, on every horse and at every seed, and nothing said so. They
     * loaded, they registered, they had icons, and their layer was inert.
     *
     * <p>So: in a normalised space, an amplitude much over 1 cannot be
     * deliberate, because 1 already sweeps the band across the entire part. The
     * bound is generous - it is looking for the 77.94 class of mistake, not
     * policing taste. {@code units} is exempt: there an amplitude of 77.94 is
     * the tilted plane and is fine.
     *
     * @see com.example.horsegenetics.common.coat.skin.HorseSkinGeometry#local
     */
    @Test
    void noWaveIsDisplacedRightOffTheHorse() {
        List<String> absurd = new ArrayList<>();
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            for (GeneSpec.ExpressionSpec expression : spec.expressions()) {
                for (GeneSpec.Layer layer : expression.layers()) {
                    for (GeneSpec.Mask mask : layer.masks()) {
                        if (mask.type() != GeneSpec.MaskType.WAVES) {
                            continue;
                        }
                        String space = mask.params().text("space", "part");
                        if (space.equals("units")) {
                            continue;   // raw body units - a big amplitude is a tilt, not a bug
                        }
                        GeneSpec.Value amplitude = mask.params().value("amplitude", 0.5);
                        if (!(amplitude instanceof GeneSpec.Value.Const constant)) {
                            continue;   // a knob; its range is the author's problem
                        }
                        if (Math.abs(constant.v()) > 3.0) {
                            absurd.add(spec.key() + " / " + expression.id() + " / " + layer.name()
                                    + ": amplitude " + constant.v() + " in '" + space + "' space");
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), absurd,
                "a WAVES amplitude far above 1 in a normalised space displaces the band clean off "
                        + "the part, so the mask selects nothing at all and does it silently. If "
                        + "you want a band that follows a pitched part, use space 'local' - see "
                        + "wiki/making-a-gene.html#local-space.");
    }
}
