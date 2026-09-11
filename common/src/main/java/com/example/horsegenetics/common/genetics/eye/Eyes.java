package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.EyeColourGene;
import com.example.horsegenetics.common.genetics.genes.EyeGlowGene;
import com.example.horsegenetics.common.genetics.genes.EyeScleraGene;
import com.example.horsegenetics.common.genetics.genes.EyeSectorColourGene;
import com.example.horsegenetics.common.genetics.genes.EyeSectorGene;
import com.example.horsegenetics.common.genetics.genes.ThirdEyeGene;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <b>The two things anything does with a horse's eyes</b>: {@link #force} them
 * when the horse is made, and {@link #resolve} them when something needs to know
 * what they look like.
 *
 * <h2>force - at birth, once</h2>
 * A gene with something to say about an eye says it as an
 * {@link EyeRequest}, and this writes the requested alleles <b>onto the
 * horse</b>. It runs where a horse is created - {@code Genome.random},
 * {@code Genome.of}, {@code Genome.breedWith}, the breed founder, and the two
 * editors - and nowhere else.
 *
 * <p>It is deliberately <b>not</b> in {@code Genotype.random} or
 * {@code Genotype.breedWith}. Those two build a genotype without an epigenome,
 * and several requests depend on one (champagne picks which of three hues to
 * ask for off its own allele copy; the white loci roll how far the blue got).
 * Forcing with midpoints there and again with the real numbers here would
 * destroy the horse's own eye alleles on the first pass and then be unable to
 * tell, on the second, that it had - which comes out as a splashed horse that
 * should have had one blue eye having two. So the genotype-only paths (the
 * combination catalogue, a breeding preview) see unforced eye loci, which is
 * the honest answer to a question asked about a genotype rather than a horse.
 *
 * <h2>resolve - whenever, freely</h2>
 * Pure and cheap: thirteen pair reads and no coat. The coat composer paints from
 * it, and so can anything that merely wants to <i>say</i> what colour a horse's
 * eyes are.
 */
public final class Eyes {

    private Eyes() {}

    // ------------------------------------------------------------------
    // Resolve
    // ------------------------------------------------------------------

    /**
     * What this horse's eyes look like.
     *
     * @param epigenome may be {@code null} - a question about a genotype rather
     *                  than about a horse, answered with schema midpoints
     */
    public static EyePhenotype resolve(Genotype genotype, Epigenome epigenome) {
        EyeRender right = eye(genotype, epigenome, EyeLocus.EyeSideRef.RIGHT);
        EyeRender left = eye(genotype, epigenome, EyeLocus.EyeSideRef.LEFT);
        ThirdEyeGene third = Genes.THIRD_EYE;
        EyeRender forehead = third.renderOf(genotype.pair(third), right, left,
                values(third, genotype, epigenome));
        return new EyePhenotype(right, left, forehead);
    }

    private static EyeRender eye(Genotype genotype, Epigenome epigenome, int side) {
        EyeColourGene iris = (EyeColourGene) gene(EyeLocus.iris(side));
        EyeSectorGene sector = (EyeSectorGene) gene(EyeLocus.sector(side));
        EyeSectorColourGene sectorColour = (EyeSectorColourGene) gene(EyeLocus.sectorColour(side));
        EyeScleraGene sclera = (EyeScleraGene) gene(EyeLocus.sclera(side));
        EyeGlowGene glowIris = (EyeGlowGene) gene(EyeLocus.glowIris(side));
        EyeGlowGene glowSclera = (EyeGlowGene) gene(EyeLocus.glowSclera(side));

        return new EyeRender(
                EyeInk.of(iris.hueOf(genotype.pair(iris)), values(iris, genotype, epigenome)),
                sector.sectorOf(genotype.pair(sector)),
                EyeInk.of(sectorColour.hueOf(genotype.pair(sectorColour)),
                        values(sectorColour, genotype, epigenome)),
                glowIris.glows(genotype.pair(glowIris)),
                EyeInk.of(sclera.hueOf(genotype.pair(sclera)), values(sclera, genotype, epigenome)),
                glowSclera.glows(genotype.pair(glowSclera)));
    }

    private static Gene gene(EyeLocus locus) {
        return Genes.byKey(locus.key());
    }

    private static EpiValues values(Gene gene, Genotype genotype, Epigenome epigenome) {
        return GeneEpigenetics.forGene(gene, genotype, epigenome).expressed();
    }

    // ------------------------------------------------------------------
    // Force
    // ------------------------------------------------------------------

    /**
     * This genotype with every {@link EyeRequest} on it applied - the eye loci
     * rewritten to the alleles the rest of the horse asked for.
     *
     * <p>A forced locus is written <b>homozygous</b>. It has to be: the request
     * is "this horse's eyes are blue", and a heterozygote at a locus where every
     * variant is recessive would not be blue-eyed at all.
     *
     * <p>Returns the same genotype when nothing asked, which is most horses.
     */
    public static Genotype force(Genotype genotype, Epigenome epigenome) {
        List<EyeRequest> asked = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (!(g instanceof EyeRequestContribution contribution)) {
                continue;
            }
            EyeRequest request = contribution.requestEyes(genotype.pair(g), genotype, epigenome);
            if (request != null && !request.isEmpty()) {
                asked.add(request);
            }
        }
        if (asked.isEmpty()) {
            return genotype;
        }
        Genotype out = genotype;
        for (Map.Entry<EyeLocus, String> e : EyeRequest.merge(asked).forced().entrySet()) {
            Gene target = Genes.byKeyOrNull(e.getKey().key());
            if (target == null) {
                continue;   // the locus is not registered in this build - nothing to force
            }
            Allele allele = target.fromToken(e.getValue());
            out = out.with(new AllelePair(allele, allele));
        }
        return out;
    }
}
