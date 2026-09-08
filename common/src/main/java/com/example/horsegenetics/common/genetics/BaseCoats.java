package com.example.horsegenetics.common.genetics;

import java.util.List;

/**
 * The coats every other coat gene is <b>read against</b>: black, bay and
 * chestnut, then the same bay wearing each of the three white patterns.
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
     * The three, in the order a UI should offer them - <b>bay first</b>, then
     * the two it sits between.
     *
     * <p>It used to be darkest first, on the argument that a white marking goes
     * from most obvious to least in that order. That is true of white markings
     * and wrong for everything else, and everything else is most of the
     * registry: <b>a black horse hides every dark marking on it</b>, and a great
     * many of these genes paint dark. Opening on black meant a reader's first
     * look at a gene was routinely a horse with nothing visible on it, which
     * reads as a broken page rather than as a badly chosen background.
     *
     * <p>Bay is the only common base that shows a dark marking and a pale one at
     * once - black points, red body - so it is the one honest default, and it is
     * why {@code GeneIconTool} already bakes every gene icon on one. Whatever
     * offers these, offers them in this order, and whatever defaults to the
     * first one now defaults to bay.
     *
     * <p><b>Then three bays with white on them</b>, heterozygous so the pattern
     * is the ordinary one rather than the lethal or near-white homozygote.
     * They are here because a solid horse is not a background at all for the
     * genes that <i>modify somebody else's white</i> - Voided and Opalized
     * recolour it, Fielded draws tendrils out of its edges and now draws
     * nothing whatever without it. A preview of one of those on three solid
     * horses is three pictures of a plain bay, which reads as a broken page
     * rather than as a badly chosen background. The three cover the three
     * shapes white comes in: tobiano's hard-edged patches down from the
     * topline, splash's dipped-in-paint underside, and sabino's roan-edged
     * legs and face.
     */
    public static List<BaseCoat> all() {
        Genotype bay = of(Genes.EXTENSION.E, Genes.AGOUTI.A);
        return List.of(
                new BaseCoat("bay", "Bay", bay),
                new BaseCoat("black", "Black", of(Genes.EXTENSION.E, Genes.AGOUTI.a)),
                new BaseCoat("chestnut", "Chestnut", of(Genes.EXTENSION.e, Genes.AGOUTI.a)),
                new BaseCoat("tobiano", "Tobiano",
                        bay.with(new AllelePair(Genes.TOBIANO.To, Genes.TOBIANO.to))),
                new BaseCoat("splash", "Splash",
                        bay.with(new AllelePair(Genes.MITF.SW1, Genes.MITF.N))),
                new BaseCoat("sabino", "Sabino",
                        bay.with(new AllelePair(Genes.KIT.SB1, Genes.KIT.N))));
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
