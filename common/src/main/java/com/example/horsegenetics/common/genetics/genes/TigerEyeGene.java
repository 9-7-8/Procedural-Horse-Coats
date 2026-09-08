package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.EyeSpread;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Tiger eye</b> ({@code horsegenetics.tiger_eye}, {@code SLC24A5}) - a
 * <b>natural, recessive</b> gene, and the first in the mod that changes
 * <i>only</i> the eyes.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type - ordinary dark eyes</td></tr>
 *   <tr><td>{@code TE1/N}, {@code TE2/N}</td><td>{@code tiger-eye-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code TE1/TE1}, {@code TE1/TE2}</td><td>{@code tiger-eye-amber} - a bright amber iris</td></tr>
 *   <tr><td>{@code TE2/TE2}</td><td>{@code tiger-eye-yellow} - the paler, more yellow shade</td></tr>
 * </table>
 *
 * <h2>It is not a coat pattern</h2>
 * Worth saying because the breed reference this mod was built from files tiger
 * eye under coat patterns, and it is not one: the coat is completely unaffected
 * and only the iris changes, which is why the gene needed the eye-colour channel
 * ({@link EyeColorContribution}) to exist before it could be built at all.
 *
 * <p>It does, however, <b>affect the baked coat texture</b> - the eyes are drawn
 * into it - so unlike the other "paints nothing" genes its expressing outcomes
 * are real {@link Expression.Builder#marker() markers} rather than wild types.
 * Two horses that differ only here do not share a texture, and should not. They
 * are <b>deterministic</b>: every amber eye is the same amber, so the locus stays
 * out of the per-horse fingerprint and costs the cache one entry per outcome and
 * not one per horse.
 *
 * <h2>Where it comes from</h2>
 * Tiger eye is a Puerto Rican Paso Fino gene, essentially confined to that breed
 * in life, and that is how it is distributed here: about a fifth of Paso Finos
 * carry a copy against roughly one wild horse in {@value #WILD_ONE_IN}. It is the
 * clearest case in the mod of a gene you find by knowing which breed to look in.
 *
 * <p>Natural, deterministic. See {@code wiki/gene-tiger-eye.html}.
 */
public final class TigerEyeGene implements Gene, EyeColorContribution {

    public static final String KEY = "horsegenetics.tiger_eye";
    public static final int PRIORITY = 60; // after the dilutions and grey, before the white loci

    /** Per allele, in the wild population at large. Paso Finos carry it far more often. */
    public static final int WILD_ONE_IN = 70;

    /** A warm amber - the classic tiger eye. */
    public static final int AMBER = 0xC8811E;
    /** {@code TE2}'s paler, greener-yellow shade. */
    public static final int YELLOW = 0xD6B341;

    public final Allele TE1 = new Allele(KEY, 0, "TE1", "Tiger eye 1 (TE1)");
    public final Allele TE2 = new Allele(KEY, 1, "TE2", "Tiger eye 2 (TE2)");
    public final Allele N = new Allele(KEY, 2, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(TE1, TE2, N);

    private final Expression WILD = Expression.wildType("Ordinary dark eyes.");

    private final Expression CARRIER = Expression.wildType("tiger-eye-carrier", "Tiger eye carrier",
            "One copy, which shows nothing at all - not in the eyes and not in the coat. Two "
                    + "carriers bred together is the only way amber eyes appear.");

    private final Expression AMBER_EYES = Expression.of("tiger-eye-amber", "Tiger eye (amber)")
            .describe("A bright amber iris, the colour of a hawk's eye, in a horse whose coat is "
                    + "entirely ordinary. Nothing else about the horse changes.")
            .marker();

    private final Expression YELLOW_EYES = Expression.of("tiger-eye-yellow", "Tiger eye (yellow)")
            .describe("The paler, more yellow shade two TE2 copies produce - the same trait a stop "
                    + "lighter than the amber TE1 gives.")
            .marker();

    private final List<Expression> expressions = List.of(WILD, CARRIER, AMBER_EYES, YELLOW_EYES);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), p -> true);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        double each = 1.0 / WILD_ONE_IN;
        p.put(TE1, each);
        p.put(TE2, each * 0.6); // TE2 is the rarer of the two
        p.put(N, 1.0 - each - each * 0.6);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Tiger eye (SLC24A5)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return true; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** Recessive: one wild-type copy and nothing shows. {@code TE2/TE2} is the paler shade. */
    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(N)) {
            return pair.homozygousFor(N) ? WILD : CARRIER;
        }
        return pair.homozygousFor(TE2) ? YELLOW_EYES : AMBER_EYES;
    }

    public boolean shows(AllelePair pair) {
        return !pair.has(N);
    }

    /**
     * The amber, at {@link EyeColor#RANK_PIGMENT} - so a tiger-eye horse that is
     * <i>also</i> splashed white has blue eyes, not amber ones. There is no
     * pigment left in a depigmented iris for this gene to colour.
     */
    @Override
    public Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                      double whiteCoverage) {
        if (pair.has(N)) {
            return Optional.empty();
        }
        return Optional.of(pair.homozygousFor(TE2)
                ? EyeColor.pigment("tiger-eye-yellow", "Tiger eye (yellow)", YELLOW)
                : EyeColor.pigment("tiger-eye-amber", "Tiger eye (amber)", AMBER));
    }
    /**
     * Only the eye spread; the iris colour itself is fixed by the alleles.
     */
    @Override
    public EpiSchema epiSchema() {
        return EyeSpread.schema();
    }

}
