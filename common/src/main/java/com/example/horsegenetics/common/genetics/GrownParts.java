package com.example.horsegenetics.common.genetics;

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
                .<List<AttachedPart>>map(List::of).orElseGet(List::of);
    }

    /**
     * Can {@code gene} grow a part on an otherwise wild horse - does <i>any</i> of
     * its combinations put geometry on it?
     *
     * <p>Asked of the model rather than kept as a list, the same way
     * {@code DesignerApi.showsAs} asks the cutie mark, so the next granting locus
     * answers {@code true} here by being added to {@link #of}. It is the other half
     * of "does this gene change how the horse looks" beside
     * {@link Genes#influencesCoat}: a horn paints nothing, so that question alone
     * filed it with the invisible health genes, and the editors' randomize never
     * moved it. See {@link EditorRules#changesLooks}.
     */
    public static boolean grants(Gene gene) {
        Epigenome epi = Epigenome.fromSeed(0x5EEDL);
        for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
            if (!of(Genotype.wildType().with(pair), epi).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
