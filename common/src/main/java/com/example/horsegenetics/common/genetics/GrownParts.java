package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.HornColourGene;
import com.example.horsegenetics.common.parts.AttachedPart;

import java.util.List;

/**
 * <b>Every piece of geometry this horse's genes grow</b>, in one call.
 *
 * <p>The client asks here and nowhere else. That is the point of it: the render
 * layer never learns <i>which</i> loci grant parts, so the second part - antlers,
 * dorsal spines, crystals - is a line in this file and no change at all on the
 * Minecraft side. It is the same reason {@code CutieMarkGene.markFor} is the one
 * door to the emblem rather than the layer folding over the registry itself.
 *
 * <h2>Grown, not worn</h2>
 * Everything here comes from the <b>genotype</b>. A part a player buckles on is a
 * different source with the same destination - it resolves to an
 * {@link AttachedPart} too, off the gear attachment on the client, and reaches the
 * same layer through the same list. Keeping the two sources apart and the renderer
 * single is what stops horse shoes, a saddle pad and a horn becoming three layers
 * with three different opinions about draw order.
 *
 * <h2>Call this in extract, never in a render layer</h2>
 * {@code GeneticHorseRenderer.extractRenderState} calls it <b>once per frame per
 * horse</b> and hands the layer a ready list. Not from the layer itself:
 * {@code CutieMarkLayer} asks its gene afresh on every submit and folds over the
 * whole registry each time, which is affordable for one emblem on a handful of
 * horses and wrong to copy for parts on a herd.
 *
 * <p>Once per frame, not once per horse - and the difference is deliberate rather
 * than overlooked. A horse with no parts returns a shared empty list and allocates
 * nothing, which is almost every horse; one with a horn allocates a small record.
 * Caching on the entity and recomputing only when the genotype changes is the next
 * step if a measurement asks for it, and no measurement has yet: the honest reason
 * it is not already here is that per-entity client caches have to be invalidated,
 * and inventing that before there is a number to point at buys nothing.
 */
public final class GrownParts {

    private GrownParts() {
    }

    /**
     * The parts {@code genotype} grows, or an empty list - which is the answer for
     * all but a few horses in a thousand, and is a shared immutable list so the
     * common case allocates nothing.
     */
    public static List<AttachedPart> of(Genotype genotype, Epigenome epigenome) {
        if (genotype == null) {
            return List.of();
        }
        // One granting locus today. The second one turns this into a collect into
        // an ArrayList; until then, returning the Optional's own list keeps the
        // common "no parts" answer free of an allocation.
        return Genes.UNICORN_HORN.hornFor(genotype, epigenome)
                .<List<AttachedPart>>map(horn -> List.of(dressHorn(horn, genotype, epigenome)))
                .orElseGet(List::of);
    }

    /**
     * The horn is polygenic: the unicorn locus grows its shape, and two more loci
     * give it its colours and its glow. (A fourth, {@code HornDustGene}, makes it
     * shed, and asks this method's answer for the dust's colours rather than
     * working them out again.)
     */
    private static AttachedPart dressHorn(AttachedPart horn, Genotype genotype, Epigenome epigenome) {
        HornColourGene.Tints tints = Genes.HORN_COLOUR.tintsFor(genotype, epigenome);
        return horn.dressed(tints.base(), tints.tip(),
                Genes.HORN_GLOW.glows(genotype.pair(Genes.HORN_GLOW)));
    }

    /**
     * Does {@code gene} grow a part, or change one - does <i>any</i> of its
     * combinations put geometry on an otherwise wild horse, or alter the geometry
     * already on a horse that has every part?
     *
     * <p>The second half is what counts the loci that only dress a part: horn
     * colour and horn glow do nothing to a hornless horse, so asked only of a wild
     * one they would look invisible. The baseline with "every part" is a horse
     * with a horn today; a second granting locus adds itself to it.
     *
     * <p>Asked of the model rather than kept as a list, the same way
     * {@code DesignerApi.showsAs} asks the cutie mark. It is the other half of "does
     * this gene change how the horse looks" beside {@link Genes#influencesCoat}: a
     * horn paints nothing, so that question alone filed it with the invisible
     * health genes, and the editors' randomize never moved it. See
     * {@link EditorRules#changesLooks}.
     */
    public static boolean shapes(Gene gene) {
        Epigenome epi = Epigenome.fromSeed(0x5EEDL);
        Genotype wild = Genotype.wildType();
        Genotype everyPart = wild.with(
                new AllelePair(Genes.UNICORN_HORN.Horn, Genes.UNICORN_HORN.Horn));
        List<AttachedPart> dressed = of(everyPart, epi);
        for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
            if (!of(wild.with(pair), epi).isEmpty()
                    || !of(everyPart.with(pair), epi).equals(dressed)) {
                return true;
            }
        }
        return false;
    }
}
