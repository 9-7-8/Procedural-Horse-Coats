package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Splash white</b> ({@code horsegenetics.splash}) - random white markings, as
 * if the horse were dipped in white from below.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code spl/spl}</td><td>wild type</td></tr>
 *   <tr><td>{@code Spl/spl}</td><td>{@code splash} - white climbing each leg, plus a centreline blaze</td></tr>
 *   <tr><td>{@code Spl/Spl}</td><td>{@code splash} - <b>the same, for now</b>; see below</td></tr>
 * </table>
 *
 * <p><b>Open issue:</b> the two variant combinations should not land on the same
 * outcome - a homozygous splash wants much taller stockings, a wide blaze or a
 * bald face, and body patches. Under the old model this was recorded as the gene
 * being tagged incomplete dominant while its painter ignored the dose, which
 * meant the tag and the pixels disagreed and nothing could tell. Here the
 * disagreement is impossible to hide: both pairs return this one expression, so
 * the gallery gives splash <b>one</b> pen and the code says plainly that the
 * homozygote is not modelled yet. Splitting it is a matter of adding a second
 * expression. See {@code wiki/verification.html}.
 *
 * <p>Natural (removes both pigments, so the white template shows through),
 * non-deterministic. Founder frequency {@code 1/}{@value #WILD_SPLASH_ONE_IN}
 * per allele.
 */
public final class SplashGene implements Gene {

    public static final String KEY = "horsegenetics.splash";
    public static final int WILD_SPLASH_ONE_IN = 20;

    public final Allele Spl = new Allele(KEY, 0, "Spl", "Splash white (Spl)");
    public final Allele spl = new Allele(KEY, 1, "spl", "Wild-type (spl)");
    private final List<Allele> alleles = List.of(Spl, spl);

    private final Expression WILD = Expression.wildType("No white markings.");

    private final Expression SPLASH = Expression.of("splash", "Splash white")
            .describe("White socks or stockings climbing each leg by an independent random amount, "
                    + "plus a white blaze down the centre of the face. One copy and two currently "
                    + "look the same; the homozygote is meant to be much bolder.")
            .varies()
            .restrict((ctx, coat) -> {
                Rng epi = ctx.epigeneticsFor(KEY);
                Skin skin = ctx.skin();
                PigmentField f = coat.mutableCopy();

                for (var leg : CoatRegions.LEGS) {
                    double h = 0.15 + epi.nextFloat() * epi.nextFloat() * 0.75; // socks .. stockings
                    CoatRegions.whitenLowerLeg(skin, f, leg, h);
                }

                double blazeHalfWidth = 0.4 + epi.nextFloat() * 1.4;  // body units either side of centre
                double blazeLength = 0.2 + epi.nextFloat() * 0.75;    // fraction of the head length
                CoatRegions.whitenBlaze(skin, f, blazeHalfWidth, blazeLength);
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, SPLASH);

    private final FounderTable founders = FounderTable.hardyWeinberg(Spl, spl, 1.0 / WILD_SPLASH_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Splash white"; }
    @Override public int priority() { return 80; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return spl; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(Spl) ? SPLASH : WILD;
    }

    public boolean isSplash(AllelePair pair) {
        return pair.has(Spl);
    }
}
