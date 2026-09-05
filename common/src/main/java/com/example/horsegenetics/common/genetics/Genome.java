package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.Sex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a horse inherits: its {@link Genotype} (which alleles) plus its
 * {@link Epigenome} (the priority + epigenetic seed riding on each of those
 * allele copies). The two are one unit because they have to stay
 * <b>aligned</b> - slot {@code first}/{@code second} of a gene's
 * {@link Epigenome.Copies} belongs to the matching slot of its
 * {@link AllelePair} - and only a breeding pass that draws both at once can
 * keep that true.
 *
 * <p><b>Breeding</b> ({@link #breedWith}) is Mendelian on the genotype and
 * <i>carrier-faithful</i> on the epigenetics: the child takes one copy from
 * each parent, and each inherited allele brings that parent copy's priority and
 * epigenetic seed along <b>unchanged</b> - no re-roll, no jitter. The one
 * exception is the priority tie-break; see {@link AlleleEpigenetics#deconflict}.
 */
public record Genome(Genotype genotype, Epigenome epigenome) {

    public Genome {
        Objects.requireNonNull(genotype, "genotype");
        Objects.requireNonNull(epigenome, "epigenome");
    }

    /** A founder / wild horse: random alleles, random epigenetics on each copy. */
    public static Genome random(Rng rng) {
        return new Genome(Genotype.random(rng), Epigenome.random(rng));
    }

    /** A known genotype with fresh random epigenetics (debug-pen horses, imports). */
    public static Genome of(Genotype genotype, Rng rng) {
        return new Genome(genotype, Epigenome.random(rng));
    }

    public static Genome parse(String genotypeCode, String epigenomeCode) {
        return new Genome(Genotype.parse(genotypeCode), Epigenome.parse(epigenomeCode));
    }

    /**
     * One foal. Per gene, in {@link Genes#codeOrder()}:
     * <ol>
     *   <li>one {@link Rng#nextBoolean()} picks which of <i>this</i> parent's
     *       two copies is passed on, one more picks {@code other}'s - the same
     *       draws {@link Genotype#breedWith} makes, so the genotype half is
     *       unchanged;</li>
     *   <li>each chosen allele arrives carrying its parent copy's
     *       {@link AlleleEpigenetics} verbatim;</li>
     *   <li>the two copies are re-aligned to the canonical dominant-first
     *       {@link AllelePair} order;</li>
     *   <li>if both arrived with the <b>same priority</b>, one extra
     *       {@code nextBoolean()} bumps the second copy one step up or down
     *       ({@link AlleleEpigenetics#deconflict}).</li>
     * </ol>
     * So: 2 {@code nextBoolean()} per gene, plus 1 per gene that ties.
     */
    public Genome breedWith(Genome other, Rng rng) {
        return breedWith(other, rng, GameteBias.NONE, GameteBias.NONE);
    }

    /**
     * One foal, with a <b>breeding-carrot {@link GameteBias}</b> on each parent
     * (roadmap wiki &sect;14). With {@link GameteBias#NONE} on both sides this is
     * bit-for-bit {@link #breedWith(Genome, Rng)} - the same {@code nextBoolean()}
     * per parent per gene, in the same order, with no extra draws - so a carrot
     * that is not fed changes nothing. A bias may then:
     * <ul>
     *   <li>replace the 50/50 copy pick with "always the dominant / always the
     *       recessive copy" (the {@code nextBoolean()} is still <i>consumed</i>,
     *       so the other parent's stream stays aligned);</li>
     *   <li>draw that parent's gamete for a named gene from a substitute pair
     *       (magic gene carrot, gene-splice carrot);</li>
     *   <li>hand the contributed copy a fresh epigenetic seed rather than the
     *       parent copy's own (epigenetic-splice carrot, and always for a substituted
     *       copy) - which consumes one {@code int} + one {@code long} extra.</li>
     * </ul>
     */
    public Genome breedWith(Genome other, Rng rng, GameteBias mineBias, GameteBias theirsBias) {
        Map<String, AllelePair> pairs = new LinkedHashMap<>();
        Map<String, Epigenome.Copies> copies = new LinkedHashMap<>();
        // Which child slot carries the copy this parent contributed - needed by
        // the second pass, since AllelePair may have swapped the two.
        Map<String, Boolean> damIsFirst = new LinkedHashMap<>();

        // --- Pass 1: the allele draw. This consumes EXACTLY what the plain
        // breed consumes - two nextBoolean() per gene plus a deconflict tie -
        // so an unfed carrot (NONE/NONE) is bit-for-bit identical, and a bias
        // that only re-rolls epigenetics does not shift a single allele.
        for (Gene g : Genes.codeOrder()) {
            AllelePair mine = mineBias.pairFor(g.key(), genotype.pair(g));
            AllelePair theirs = theirsBias.pairFor(g.key(), other.genotype().pair(g));
            Epigenome.Copies myEpi = epigenome.copies(g);
            Epigenome.Copies theirEpi = other.epigenome().copies(g);

            boolean fromMyFirst = rng.nextBoolean();
            Allele a = fromMyFirst ? mine.first() : mine.second();
            AlleleEpigenetics aEpi = fromMyFirst ? myEpi.first() : myEpi.second();
            if (mineBias.preferLowerOrder().isPresent() && !mine.homozygous()) {
                boolean lower = mineBias.preferLowerOrder().get();
                a = lower ? mine.first() : mine.second();
                aEpi = lower ? myEpi.first() : myEpi.second();
            }

            boolean fromTheirFirst = rng.nextBoolean();
            Allele b = fromTheirFirst ? theirs.first() : theirs.second();
            AlleleEpigenetics bEpi = fromTheirFirst ? theirEpi.first() : theirEpi.second();
            if (theirsBias.preferLowerOrder().isPresent() && !theirs.homozygous()) {
                boolean lower = theirsBias.preferLowerOrder().get();
                b = lower ? theirs.first() : theirs.second();
                bEpi = lower ? theirEpi.first() : theirEpi.second();
            }

            AllelePair pair = new AllelePair(a, b);
            boolean aFirst = pair.first().equals(a);
            Epigenome.Copies childCopies = aFirst
                    ? new Epigenome.Copies(aEpi, bEpi)
                    : new Epigenome.Copies(bEpi, aEpi);
            childCopies = new Epigenome.Copies(
                    childCopies.first(),
                    AlleleEpigenetics.deconflict(childCopies.first(), childCopies.second(), rng));

            pairs.put(g.key(), pair);
            copies.put(g.key(), childCopies);
            damIsFirst.put(g.key(), aFirst);
        }

        // --- Pass 2: epigenetic re-rolls (epigenetic splice, and every substituted
        // copy - a gene-splice / gene-carrot gamete has no real parent copy behind
        // it). Runs AFTER every allele is locked, so its extra draws never move
        // a foal's genotype - only its seeds.
        boolean anyReroll = mineBias.rerollEpigenetics() || theirsBias.rerollEpigenetics()
                || !mineBias.substitutePairs().isEmpty() || !theirsBias.substitutePairs().isEmpty();
        if (anyReroll) {
            for (Gene g : Genes.codeOrder()) {
                boolean rerollMine = mineBias.rerollEpigenetics() || mineBias.substitutes(g.key());
                boolean rerollTheirs = theirsBias.rerollEpigenetics() || theirsBias.substitutes(g.key());
                if (!rerollMine && !rerollTheirs) {
                    continue;
                }
                Epigenome.Copies c = copies.get(g.key());
                boolean aFirst = damIsFirst.get(g.key());
                AlleleEpigenetics first = (aFirst ? rerollMine : rerollTheirs)
                        ? AlleleEpigenetics.random(rng) : c.first();
                AlleleEpigenetics second = (aFirst ? rerollTheirs : rerollMine)
                        ? AlleleEpigenetics.random(rng) : c.second();
                second = AlleleEpigenetics.deconflict(first, second, rng);
                copies.put(g.key(), new Epigenome.Copies(first, second));
            }
        }

        return new Genome(Genotype.of(List.copyOf(pairs.values())), Epigenome.of(copies));
    }

    /**
     * This genome with its sex locus forced to {@code sex} - see
     * {@link Genotype#withSex}. The epigenome is untouched and stays aligned:
     * the sex locus has two copies either way, and nothing reads their seeds.
     */
    public Genome withSex(Sex sex) {
        return new Genome(genotype.withSex(sex), epigenome);
    }

    /** This horse's {@link Sex}, read off the sex locus. */
    public Sex sex() {
        return genotype.sex();
    }

    public String genotypeCode() {
        return genotype.toCode();
    }

    public String epigenomeCode() {
        return epigenome.toCode();
    }

    /** The epigenetic seed {@code gene}'s per-horse randomness runs on for this horse. */
    public long expressedSeed(Gene gene) {
        return epigenome.expressedSeed(gene, genotype);
    }
}
