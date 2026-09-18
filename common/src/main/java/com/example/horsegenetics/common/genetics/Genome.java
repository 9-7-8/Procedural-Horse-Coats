package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.genetics.eye.Eyes;
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
        // Every horse passes through here, so this is where a wild-type copy loses its
        // numbers and a variant copy that arrived with none gets some - see
        // Epigenome.alignedTo.
        epigenome = epigenome.alignedTo(genotype);
    }

    /** A founder / wild horse: random alleles, random epigenetics on each copy. */
    public static Genome random(Rng rng) {
        return withForcedEyes(Genotype.random(rng), Epigenome.random(rng));
    }

    /** A known genotype with fresh rolled epigenetics (debug-pen horses, imports). */
    public static Genome of(Genotype genotype, Rng rng) {
        return withForcedEyes(genotype, Epigenome.random(rng));
    }

    /**
     * <b>Where the eye loci are settled</b>, and the only place - see
     * {@link Eyes#force}. A gene with something to say about an eye says it as a
     * request, and the requested allele is written onto the horse here, once,
     * when the horse is made.
     *
     * <p>It has to be at the {@link Genome} level rather than inside
     * {@link Genotype#random} because several requests read the epigenome:
     * champagne picks which of three hues to ask for off its own allele copy,
     * and the white loci roll how far the blue got. Forcing twice - once with
     * midpoints and once with the real numbers - would destroy the horse's own
     * eye alleles on the first pass and leave the second unable to tell that it
     * had.
     *
     * <p>The epigenome is handed back untouched and stays aligned: a forced
     * locus keeps whatever numbers its two copies were rolled or inherited
     * with, and {@link Epigenome.Copies} is addressed by slot rather than by
     * allele.
     */
    private static Genome withForcedEyes(Genotype genotype, Epigenome epigenome) {
        return new Genome(Eyes.force(genotype, epigenome), epigenome);
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
        com.example.horsegenetics.common.horse.Sex childSex = null;
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

            // SEX-LINKED LOCI (2026-09-14). The foal's sex - drawn at the sex locus, which is
            // first in codeOrder - decides what the sire can give: his X-borne allele to a
            // filly and his Y, which is nothing, to a colt (the mirror for a Y-linked locus).
            // Genotype.breedWith always did this. This draw, which every real foal comes
            // through, did not, and the brindle ratio pen threw brindle colts. Both coins
            // above are still spent, so no other gene's draw moves.
            if (childSex != null && g.inheritance().sexLinked()) {
                boolean mineIsDam = genotype.sex() == com.example.horsegenetics.common.horse.Sex.FEMALE
                        || other.genotype().sex() != com.example.horsegenetics.common.horse.Sex.FEMALE;
                AllelePair sirePair = mineIsDam ? theirs : mine;
                Epigenome.Copies sireEpi = mineIsDam ? theirEpi : myEpi;
                Allele placeholder = g.hemizygousPlaceholder();
                List<Allele> sireReal = g.realAlleles(sirePair);
                boolean filly = childSex == com.example.horsegenetics.common.horse.Sex.FEMALE;
                boolean xLinked = g.inheritance() == Inheritance.X_LINKED;
                boolean sireGives = xLinked == filly && !sireReal.isEmpty();
                Allele fromSire = sireGives ? sireReal.get(0) : placeholder;
                AlleleEpigenetics fromSireEpi = !sireGives ? AlleleEpigenetics.NONE
                        : sirePair.first().equals(fromSire) ? sireEpi.first() : sireEpi.second();
                // X-linked: the dam's ordinary pick stands. Y-linked: the dam gives nothing.
                Allele damSide = xLinked ? (mineIsDam ? a : b) : placeholder;
                AlleleEpigenetics damSideEpi = xLinked ? (mineIsDam ? aEpi : bEpi) : AlleleEpigenetics.NONE;
                a = mineIsDam ? damSide : fromSire;
                aEpi = mineIsDam ? damSideEpi : fromSireEpi;
                b = mineIsDam ? fromSire : damSide;
                bEpi = mineIsDam ? fromSireEpi : damSideEpi;
            }

            AllelePair pair = new AllelePair(a, b);
            boolean aFirst = pair.first().equals(a);
            pairs.put(g.key(), pair);
            if (g == Genes.SEX) {
                childSex = Genes.SEX.sexOf(pair);
            }
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
            // A wild-type copy carries nothing, re-rolled or not (2026-09-14). The draws above
            // are still spent so the stream is unchanged; only the result is dropped. Without
            // this an epigenetic splice handed a wild white locus numbers before the eyes were
            // forced, and a spliced foal's eye alleles differed from its unspliced twin's.
            AllelePair childPair = pairs.get(g.key());
            if (Epigenome.silent(g, childPair.first())) {
                first = AlleleEpigenetics.NONE;
            }
            if (Epigenome.silent(g, childPair.second())) {
                second = AlleleEpigenetics.NONE;
            }
            copies.put(g.key(), new Epigenome.Copies(
                    first, AlleleEpigenetics.deconflict(first, second, rng)));
        }

        // The mutation roll goes here, after every allele is locked, so it cannot
        // shift another gene's inheritance - and BEFORE the eyes are forced, so a
        // mutated gene that asks for an eye colour is honoured like any other.
        Genome drawn = Mutation.mutate(
                new Genome(Genotype.of(List.copyOf(pairs.values())), Epigenome.of(copies)), rng);
        return withForcedEyes(drawn.genotype(), drawn.epigenome());
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
