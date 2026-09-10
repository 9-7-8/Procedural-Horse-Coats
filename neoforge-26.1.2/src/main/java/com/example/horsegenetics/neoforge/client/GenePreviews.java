package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>What each of a gene's outcomes actually looks like</b> - the row of little
 * horses at the top of a gene's entry in the database.
 *
 * <h2>Why fixed rather than rolled</h2>
 * A gene entry is a reference page. Two players comparing notes, or one player
 * coming back to the same gene an hour later, have to be looking at the same
 * picture, or the page teaches nothing you can rely on - and a coat that
 * re-rolls every time the panel opens reads as a bug long before it reads as
 * variety. So every shot in here is built from one baseline genotype and one
 * fixed epigenome seed, and is cached for the session.
 *
 * <p>This is <b>not</b> a pre-generated set of coats being substituted for the
 * real thing (which the mod does not do, and must not). Nothing is baked to
 * disk and no image is shipped: the genotype is assembled here, run through the
 * real {@link CoatData} pipeline, and drawn on the real 3D model. Fixing the
 * <i>inputs</i> is what makes it a reference; the painting is as live as any
 * horse in the world.
 *
 * <h2>The baseline, and why it is the wild type</h2>
 * Every shot is the mod's own wild-type horse with one locus changed. That is
 * the honest picture of what the gene <i>does</i> - the difference between the
 * shots is the gene and nothing else - and it is the same baseline the gene's
 * wiki page describes it against. Picking a prettier base would mean every
 * entry showed a coat the gene had only half a hand in.
 *
 * <p>One shot per allele, homozygous. A recessive needs two copies to show at
 * all, and for everything else the homozygote is the loudest the gene gets;
 * showing the heterozygote too would double a row that is mostly duplicates.
 *
 * <p><b>The wild type gets a square only where it is a coat rather than an
 * absence</b> - which is extension, agouti and shade, the three loci every
 * horse has a real colour at ({@link EditorRules#alwaysCarried}). Everywhere
 * else the baseline draws precisely the same horse as the row's own reference
 * point, so it was a duplicate of the plain animal parked at the front of every
 * entry, teaching nothing and taking the eye first. (Owner's call.)
 */
public final class GenePreviews {

    /**
     * The one epigenome every preview is drawn with.
     *
     * <p>Arbitrary, and that is fine - what matters is that it never changes,
     * because it is the difference between a reference and a slot machine.
     */
    private static final long SEED = 0x486F727365L;

    /** No row is worth more than this many horses; past it the panel is a stable, not a page. */
    private static final int MAX_SHOTS = 8;

    /** One outcome: what to call it, and the coat to draw. */
    public record Shot(String label, CoatData coat) {
    }

    private static final Map<String, List<Shot>> CACHE = new HashMap<>();

    private GenePreviews() {
    }

    /** Dropped with the rest of the per-world client state. */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * The shots for {@code gene}, or an empty list for a gene with nothing to
     * show - which is most of the health and behaviour loci, and is why
     * {@link Gene#affectsCoat()} is the gate rather than a hand-kept list.
     */
    public static List<Shot> forGene(Gene gene) {
        if (!gene.affectsCoat()) {
            return List.of();
        }
        List<Shot> cached = CACHE.get(gene.key());
        if (cached != null) {
            return cached;
        }
        List<Shot> shots = new ArrayList<>();
        Epigenome epigenome = Epigenome.fromSeed(SEED);
        for (Allele allele : gene.alleles()) {
            if (shots.size() >= MAX_SHOTS) {
                break;
            }
            if (gene.isPlaceholder(allele)) {
                continue; // a slot reserved for an allele nobody has written yet
            }
            if (allele.equals(gene.defaultAllele()) && !EditorRules.alwaysCarried(gene)) {
                continue; // "no effect" needs no picture
            }
            AllelePair pair = new AllelePair(allele, allele);
            if (!gene.canOccur(pair)) {
                continue; // a sex-linked locus has combinations no horse can be
            }
            try {
                Genotype genotype = Genotype.wildType().with(pair);
                shots.add(new Shot(allele.label(), new CoatData(genotype, epigenome)));
            } catch (RuntimeException notPaintable) {
                // A gene whose painter will not run against a bare wild type is
                // not going to draw here either. Skipping the shot loses one
                // picture; letting it through loses the panel.
            }
        }
        List<Shot> out = List.copyOf(shots);
        CACHE.put(gene.key(), out);
        return out;
    }
}
