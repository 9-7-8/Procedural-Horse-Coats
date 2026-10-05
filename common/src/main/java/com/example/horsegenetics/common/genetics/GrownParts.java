package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AbstractPartColourGene;
import com.example.horsegenetics.common.genetics.genes.HornColourGene;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // Five granting loci. Each answers empty for nearly every horse, and the
        // list is only built when one of them says otherwise - so the common "no
        // parts" answer still allocates nothing.
        Optional<AttachedPart> horn = Genes.UNICORN_HORN.hornFor(genotype, epigenome);
        Optional<List<AttachedPart>> rack = Genes.ANTLERS.racksFor(genotype, epigenome,
                Genes.ANTLER_FORM.habitOf(genotype.pair(Genes.ANTLER_FORM)));
        Optional<List<AttachedPart>> rams = Genes.RAM_HORNS.hornsFor(genotype, epigenome,
                Genes.RAM_HORN_FORM.shapeOf(genotype.pair(Genes.RAM_HORN_FORM)));
        Optional<List<AttachedPart>> dragon = Genes.DRAGON_HORNS.hornsFor(genotype, epigenome);
        Optional<AttachedPart> spines = Genes.DORSAL_SPINES.spinesFor(genotype, epigenome);
        if (horn.isEmpty() && rack.isEmpty() && rams.isEmpty() && dragon.isEmpty() && spines.isEmpty()) {
            return List.of();
        }
        List<AttachedPart> out = new ArrayList<>(8);
        horn.ifPresent(h -> out.add(dressHorn(h, genotype, epigenome)));
        rack.ifPresent(antlers -> {
            for (AttachedPart antler : antlers) {
                out.add(dressAntler(antler, genotype, epigenome));
            }
        });
        rams.ifPresent(pair -> {
            boolean tipped = Genes.RAM_HORN_TIP.tipped(genotype.pair(Genes.RAM_HORN_TIP));
            int tip = tipped ? Genes.RAM_HORN_TIP.tipColourOf(genotype, epigenome) : 0;
            for (AttachedPart side : pair) {
                // The horn's own shade stays at the base; a coloured tip fades in toward the point.
                out.add(tipped ? side.dressed(side.baseTint(), tip, false) : side);
            }
        });
        dragon.ifPresent(pair -> {
            // Its own colour locus, the horn's rule: one pair of tints for both sides.
            AbstractPartColourGene.Tints tints = Genes.DRAGON_HORN_COLOUR.tintsFor(genotype, epigenome);
            for (AttachedPart side : pair) {
                out.add(side.dressed(tints.base(), tints.tip(), false));
            }
        });
        spines.ifPresent(row -> {
            // Its own colour locus too; a two-tone row is two-tone on every spine, root to point.
            AbstractPartColourGene.Tints tints = Genes.DORSAL_SPINE_COLOUR.tintsFor(genotype, epigenome);
            out.add(row.dressed(tints.base(), tints.tip(), false));
        });
        return List.copyOf(out);
    }

    /**
     * One part, as a list of a horse's traits names it: a pair once, and whether a
     * foal goes without it ({@link PartKind#showsOnFoal}).
     */
    public record Listed(String name, boolean adultOnly) {
    }

    /**
     * The parts {@link #of} grows, named for a person - in the order {@code of}
     * returns them, each pair once. The browser designer prints this because it
     * previews a coat texture and cannot draw geometry (owner's call, 2026-10-01:
     * say so in words rather than build a 3D preview). The sex gate is already in
     * {@code of}, so an {@code Antm} mare lists no antlers, as the game draws none.
     */
    public static List<Listed> listed(Genotype genotype, Epigenome epigenome) {
        List<AttachedPart> parts = of(genotype, epigenome);
        if (parts.isEmpty()) {
            return List.of();
        }
        List<Listed> out = new ArrayList<>(parts.size());
        for (AttachedPart part : parts) {
            PartKind kind = part.kind();
            Listed named = new Listed(kind.label(), !kind.showsOnFoal());
            if (!out.contains(named)) {
                out.add(named);
            }
        }
        return List.copyOf(out);
    }

    /**
     * The antlers are polygenic the same way: the antlers locus grows them in bone,
     * the form locus has already picked their habit, and three more loci finish them
     * - glow at the points, crystal shafts in a gem colour, and leaves.
     */
    private static AttachedPart dressAntler(AttachedPart antler, Genotype genotype, Epigenome epigenome) {
        AttachedPart out = antler;
        if (Genes.ANTLER_CRYSTAL.crystal(genotype.pair(Genes.ANTLER_CRYSTAL))) {
            int gem = Genes.ANTLER_CRYSTAL.gemOf(genotype, epigenome);
            out = out.dressed(gem, gem, false).crystalline(true);
        }
        if (Genes.ANTLER_GLOW.glows(genotype.pair(Genes.ANTLER_GLOW))) {
            out = out.dressed(out.baseTint(), out.tipTint(), true);
        }
        if (Genes.ANTLER_BLOOM.blooms(genotype.pair(Genes.ANTLER_BLOOM))) {
            out = out.blooming(Genes.ANTLER_BLOOM.growthOf(genotype, epigenome).tint());
        }
        return out;
    }

    /**
     * The horn is polygenic: the unicorn locus grows its shape, and two more loci
     * give it its colours and its glow. (A fourth, {@code HornDustGene}, makes it
     * shed, and asks this method's answer for the dust's colours rather than
     * working them out again.)
     *
     * <p>Only horn glow makes it emissive - not the light locus, whatever the horse
     * emits (owner's call, 2026-10-02, reversing the 2026-10-01 one).
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
     * with a horn, a rack of antlers, ram's horns, dragon horns and dorsal spines; a further granting
     * locus adds itself to it.
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
        Genotype everyPart = wild
                .with(new AllelePair(Genes.UNICORN_HORN.Horn, Genes.UNICORN_HORN.Horn))
                .with(new AllelePair(Genes.ANTLERS.Ant, Genes.ANTLERS.Ant))
                .with(new AllelePair(Genes.RAM_HORNS.Rh, Genes.RAM_HORNS.Rh))
                .with(new AllelePair(Genes.DRAGON_HORNS.Drg, Genes.DRAGON_HORNS.Drg))
                .with(new AllelePair(Genes.DORSAL_SPINES.Dsp, Genes.DORSAL_SPINES.Dsp));
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
