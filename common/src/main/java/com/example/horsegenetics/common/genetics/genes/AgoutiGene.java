package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.BayCoat;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Agouti</b> ({@code horsegenetics.agouti}) - where black pigment is allowed
 * to sit.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code a/a}</td><td>wild type - black is left everywhere, so the horse is whatever extension made it</td></tr>
 *   <tr><td>{@code A/a}, {@code A/A}</td><td>{@code bay} - black restricted to the points</td></tr>
 * </table>
 *
 * <p>Bay is <b>non-deterministic</b>: one uniform per-horse "point extent" off
 * the expressing {@code A} copy sets the leg and face black, with each leg
 * jittered independently. <b>Seal brown is just a high roll of that extent</b> -
 * there is no seal allele and no seal expression.
 *
 * <p>Agouti only moves black pigment, so on a chestnut horse it does nothing at
 * all - {@link #expressionIn} reports the wild type there rather than painting
 * black points onto a horse that has no black.
 *
 * <p>Natural. Founders: 25 / 50 / 25.
 */
public final class AgoutiGene implements Gene {

    public static final String KEY = "horsegenetics.agouti";

    public final Allele A = new Allele(KEY, 0, "A", "Agouti / bay (A)");
    public final Allele a = new Allele(KEY, 1, "a", "Non-agouti (a)");
    private final List<Allele> alleles = List.of(A, a);

    private final Expression WILD = Expression.wildType(
            "Black pigment is not restricted, so it covers the whole horse - a plain black, "
                    + "or a chestnut if extension already removed it.");

    private final Expression BAY = Expression.of("bay", "Bay")
            .describe("Black restricted to the points: a red-brown body with black mane, tail, ears and "
                    + "hooves, and black climbing the legs and face by a per-horse amount. A high roll "
                    + "is what a seal brown is.")
            .varies()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                BayCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, BAY);

    private final FounderTable founders = FounderTable.builder()
            .weight(A, A, 25.0)
            .weight(A, a, 50.0)
            .weight(a, a, 25.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Agouti"; }
    @Override public int priority() { return 20; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return a; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(A) ? BAY : WILD;
    }

    /** Nothing to restrict on a horse that makes no black pigment. */
    @Override
    public Expression expressionIn(AllelePair pair, Genotype genotype) {
        return genotype.hasBlackPigment() ? expressionOf(pair) : WILD;
    }

    public boolean isBay(AllelePair pair) {
        return pair.has(A);
    }
}
