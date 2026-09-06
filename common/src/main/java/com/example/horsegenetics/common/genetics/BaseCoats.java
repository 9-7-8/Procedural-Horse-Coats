package com.example.horsegenetics.common.genetics;

import java.util.List;

/**
 * The three coats every other coat gene is <b>read against</b>: black, bay and
 * chestnut.
 *
 * <p>Extension and agouti are not markings - they are the pigment a marking
 * gene then has to work on, which is why {@link ShowcaseGenotypes} excludes
 * them from the pool it forces and why the coat sample tool renders most of its
 * samples three times. Tobiano over a black is a black-and-white horse and
 * tobiano over a chestnut is a skewbald; roan reads strongly on a black and is
 * nearly invisible on a light chestnut. So "what does this gene look like" has
 * three answers, and the wiki's gene previews ask for all three.
 *
 * <p>Built from the {@link Allele} objects, not from a code string: the tokens
 * are the gene's business, and a renamed allele must break the compile here
 * rather than parse into something wrong.
 */
public final class BaseCoats {

    /** One base coat: a stable key for a UI, a label for a person, a genotype. */
    public record BaseCoat(String key, String name, Genotype genotype) {
    }

    private BaseCoats() {
    }

    /**
     * The three, in the order a UI should offer them - darkest first, which is
     * also the order in which a white-marking gene goes from most obvious to
     * least.
     */
    public static List<BaseCoat> all() {
        return List.of(
                new BaseCoat("black", "Black", of(Genes.EXTENSION.E, Genes.AGOUTI.a)),
                new BaseCoat("bay", "Bay", of(Genes.EXTENSION.E, Genes.AGOUTI.A)),
                new BaseCoat("chestnut", "Chestnut", of(Genes.EXTENSION.e, Genes.AGOUTI.a)));
    }

    /** @return null if no base coat has that key - the caller decides what that means. */
    public static BaseCoat byKey(String key) {
        for (BaseCoat b : all()) {
            if (b.key().equals(key)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Homozygous at both loci, wild type everywhere else. Homozygous so that the
     * base is what it says it is under any later edit: a bay written {@code A/a}
     * would still be a bay, but it also carries a black the preview never shows,
     * and a reader comparing the genotype line against the picture would have to
     * work out why.
     */
    private static Genotype of(Allele extension, Allele agouti) {
        return Genotype.wildType()
                .with(new AllelePair(extension, extension))
                .with(new AllelePair(agouti, agouti));
    }
}
