package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.Objects;

/**
 * A <b>detached, transferable copy of one horse's heritable material</b> -
 * its {@link Genotype} and {@link Epigenome}, frozen as code strings so it can
 * live somewhere a live entity cannot: an item component, a saved-data blob, an
 * export.
 *
 * <p>A {@link Genome} is what a <i>loaded horse</i> carries; a
 * {@code GenomeSample} is that same information taken off the horse. It exists
 * so breeding no longer requires both parents to be in the world at once - the
 * <b>stallion seed jar</b> is a {@code GenomeSample} of the sire, and
 * {@link #breedInto} runs the ordinary Mendelian draw
 * ({@link Genome#breedWith}) with the mare's live genome on the other side.
 * "Draw at impregnation, from the stored genotype" - the sample never freezes a
 * gamete, so the mare route is deterministic in exactly the way normal breeding
 * is.
 *
 * <p>Later this is also what an embryo, a clone source and a research/export
 * read. Carrot / gamete-bias effects are <b>not</b> modelled here yet - when
 * they are, they attach to the sample (the jar "carries the carrot effects"),
 * and {@link #breedInto} grows a bias parameter. For now it is genotype +
 * epigenome only.
 *
 * <p>Validated on construction: both codes must parse under the current
 * {@link Genes} registry. Dev only - no legacy-format handling.
 */
public record GenomeSample(String genotypeCode, String epigenomeCode) {

    public GenomeSample {
        Objects.requireNonNull(genotypeCode, "genotypeCode");
        Objects.requireNonNull(epigenomeCode, "epigenomeCode");
        // Parse-check now so a malformed sample fails where it is built, not
        // deep inside a breeding pass.
        Genome.parse(genotypeCode, epigenomeCode);
    }

    public static GenomeSample of(Genome genome) {
        return new GenomeSample(genome.genotypeCode(), genome.epigenomeCode());
    }

    public static GenomeSample of(Genotype genotype, Epigenome epigenome) {
        return new GenomeSample(genotype.toCode(), epigenome.toCode());
    }

    /** The live {@link Genome} this sample was taken from. */
    public Genome genome() {
        return Genome.parse(genotypeCode, epigenomeCode);
    }

    /**
     * One foal from this sample (the sire) and a live {@code mare} genome, using
     * the same seeded Mendelian + carrier-faithful draw as an in-world pairing.
     * Argument order into {@link Genome#breedWith} is mare-then-sample, matching
     * how {@code GeneticCodeCombiner.combine(mother, father, rng)} is called.
     */
    public Genome breedInto(Genome mare, Rng rng) {
        return mare.breedWith(genome(), rng);
    }
}
