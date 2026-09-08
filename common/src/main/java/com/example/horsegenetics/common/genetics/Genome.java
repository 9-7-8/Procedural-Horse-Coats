package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.horse.Sex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a horse inherits: its {@link Genotype} (which alleles) plus its
 * {@link Epigenome} (the priority + the literal values riding on each of those
 * allele copies). The two are one unit because they have to stay
 * <b>aligned</b> - slot {@code first}/{@code second} of a gene's
 * {@link Epigenome.Copies} belongs to the matching slot of its
 * {@link AllelePair} - and only a breeding pass that draws both at once can
 * keep that true.
 *
 * <p><b>Breeding</b> ({@link #breedWith}) is Mendelian on the genotype and
 * <i>carrier-faithful</i> on the epigenetics: the child takes one copy from
 * each parent, and each inherited allele brings that parent copy's priority and
 * literal values along - no re-roll. Those values are then nudged by
 * {@link EpiDrift}, small enough that a pair breeds true to the eye and
 * open-ended enough that a line slowly becomes its own thing.
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

    /** A known genotype with fresh rolled epigenetics (debug-pen horses, imports). */
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
     *       {@link AlleleEpigenetics} - the priority verbatim, the values
     *       nudged by {@link EpiDrift};</li>
     *   <li>the two copies are re-aligned to the canonical dominant-first
     *       {@link AllelePair} order;</li>
     *   <li>if both arrived with the <b>same priority</b>, one extra
     *       {@code nextBoolean()} bumps the second copy one step up or down
     *       ({@link AlleleEpigenetics#deconflict}).</li>
     * </ol>
     *
     * <p><b>The old draw-order contract is gone.</b> It used to be exactly two
     * {@code nextBoolean()} per gene plus a tie, which let an unfed carrot be
     * bit-for-bit identical to a plain breeding. Drift now consumes a couple of
     * draws per stored value on top, so that guarantee cannot hold and is not
     * claimed. What <i>is</i> still guaranteed is the thing it was protecting:
     * pass 1 locks every allele before pass 2 spends a single draw, so no
     * carrot and no amount of drift can move a foal's genotype.
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
     *       (Known Gene Splice carrot, Unknown Gene Splice carrot);</li>
     *   <li>hand the contributed copy freshly rolled values rather than the
     *       parent copy's own (epigenetic-splice carrot, and always for a
     *       substituted copy - a spliced gamete has no real parent copy behind
     *       it for its numbers to have come from).</li>
     * </ul>
     */
    public Genome breedWith(Genome other, Rng rng, GameteBias mineBias, GameteBias theirsBias) {
        Map<String, AllelePair> pairs = new LinkedHashMap<>();
        Map<String, Epigenome.Copies> copies = new LinkedHashMap<>();
        // Which child slot carries the copy this parent contributed - needed by
        // the second pass, since AllelePair may have swapped the two.
        Map<String, Boolean> damIsFirst = new LinkedHashMap<>();

        // --- Pass 1: the allele draw, and nothing else. Two nextBoolean() per
        // gene, in a fixed order, whatever the carrots say - so a bias that only
        // touches epigenetics cannot shift a single allele, and neither can drift.
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
            pairs.put(g.key(), pair);
            copies.put(g.key(), aFirst
                    ? new Epigenome.Copies(aEpi, bEpi)
                    : new Epigenome.Copies(bEpi, aEpi));
            damIsFirst.put(g.key(), aFirst);
        }

        // --- Pass 2: the epigenetics. Every stored copy is either re-rolled
        // (an epigenetic-splice carrot, or any substituted copy - a spliced
        // gamete has no real parent copy for its numbers to have come from) or
        // inherited-with-drift. Runs only over genes that actually store
        // something, and only after every allele above is locked.
        for (Gene g : Genes.codeOrder()) {
            EpiSchema schema = g.epiSchema();
            if (schema.isEmpty()) {
                continue;
            }
            boolean rerollMine = mineBias.rerollEpigenetics() || mineBias.substitutes(g.key());
            boolean rerollTheirs = theirsBias.rerollEpigenetics() || theirsBias.substitutes(g.key());
            Epigenome.Copies c = copies.get(g.key());
            boolean aFirst = damIsFirst.get(g.key());

            AlleleEpigenetics first = inherit(c.first(), aFirst ? rerollMine : rerollTheirs, schema, rng);
            AlleleEpigenetics second = inherit(c.second(), aFirst ? rerollTheirs : rerollMine, schema, rng);
            copies.put(g.key(), new Epigenome.Copies(
                    first, AlleleEpigenetics.deconflict(first, second, rng)));
        }

        return new Genome(Genotype.of(List.copyOf(pairs.values())), Epigenome.of(copies));
    }

    /**
     * One contributed copy's numbers: freshly rolled if a carrot substituted the
     * gamete, otherwise the parent copy's own carried forward and drifted.
     */
    private static AlleleEpigenetics inherit(AlleleEpigenetics parent, boolean reroll,
                                             EpiSchema schema, Rng rng) {
        return reroll ? AlleleEpigenetics.founder(schema, rng) : parent.drifted(rng);
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

    /** The literal numbers {@code gene} paints this horse with. */
    public com.example.horsegenetics.common.genetics.epi.EpiValues expressedValues(Gene gene) {
        return epigenome.expressedValues(gene, genotype);
    }
}
