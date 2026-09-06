package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.LutContribution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>LUT</b> ({@code horsegenetics.lut}) - a <b>magical</b> gene that swaps the
 * colour lookup the natural genes resolve through. It touches no pigment in
 * phase 1; instead, for a horse carrying two identical copies of a variant
 * allele, {@link com.example.horsegenetics.common.coat.pattern.CoatTextureComposer}
 * resolves the surviving red/black pigment against an <i>unnatural</i> gradient
 * in phase 2, so the whole coat lands on a different palette while every melanin
 * gene still does exactly what it did.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - the natural red/black gradient</td></tr>
 *   <tr><td>{@code Blupnk/n}</td><td>{@code bluepink-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Blupnk/Blupnk}</td><td>{@code bluepink} - resolves through {@code lutbluepink.png}</td></tr>
 * </table>
 *
 * <h2>Two of the same, or nothing</h2>
 * A variant allele only does anything <b>homozygous with itself</b>. One copy is
 * a silent carrier; two <i>different</i> variant alleles (once there is more
 * than one) are also silent - a mixed pair is not "half of each palette", it is
 * the natural gradient. That is the whole shape of the locus, and it is what
 * makes an unnatural coat a breeding project rather than a lucky catch.
 *
 * <h2>Currently one variant, built to grow</h2>
 * {@code Blupnk} swaps the warm red/orange/brown gradient for a blue-and-pink
 * one - dreamier horse colours. More palettes are planned; each is one more
 * {@link Variant} in {@link #VARIANTS} (an allele, a LUT key and a texture
 * path) and the expression table, carrier wording and
 * {@link LutContribution} wiring all follow from that list.
 *
 * <p><b>This is the only locus that can change the LUT, and it always will
 * be.</b> There is no per-gene "use this gradient" hook and there is not going
 * to be one: a horse has two copies of this chromosome and no more, so it shows
 * at most one alternate palette, ever. Anything that should affect which LUT a
 * coat resolves against is written as <b>another allele on this gene</b>, never
 * as a new gene - the same rule {@code KIT} follows for white spotting.
 *
 * <p>It <b>paints nothing itself</b>, so its non-wild outcome carries no painter
 * ({@link Expression.Builder#marker()}); the composer applies the swap out of
 * band. It is deterministic - every {@code Blupnk/Blupnk} horse resolves against
 * the same LUT - so it stays out of the per-horse texture fingerprint.
 *
 * <p>About one wild horse in {@value #WILD_VARIANT_ONE_IN} carries a variant
 * copy; a homozygote never turns up in the wild at all - like the other
 * "breed it, don't catch it" loci, every dreamy-coated horse is one somebody
 * paired up on purpose.
 */
public final class LutGene implements Gene, LutContribution {

    public static final String KEY = "horsegenetics.lut";
    public static final int PRIORITY = 190; // magical band, after verdant (180)

    /** One in this many wild horses carries a given variant allele. */
    public static final int WILD_VARIANT_ONE_IN = 60;

    /** One alternate palette: its allele, the LUT key the composer looks up, and the texture. */
    public record Variant(Allele allele, Expression carrier, Expression outcome,
                          String lutKey, String texturePath) {
    }

    public final Allele n = new Allele(KEY, 0, "n", "Wild-type (n)");
    public final Allele Blupnk = new Allele(KEY, 1, "Blupnk", "Blue-pink LUT (Blupnk)");

    private final Expression WILD = Expression.wildType(
            "The natural red/black gradient - ordinary horse colours.");

    /** Every variant palette, in allele order. Add a row here to add a palette. */
    public final List<Variant> VARIANTS = List.of(
            new Variant(Blupnk,
                    Expression.wildType("bluepink-carrier", "Blue-pink LUT carrier",
                            "One copy of the blue-pink allele. Nothing shows - a horse needs two "
                                    + "copies of the same LUT allele to shift its palette."),
                    Expression.of("bluepink", "Blue-pink palette")
                            .describe("The warm red/orange/brown gradient is replaced with a blue "
                                    + "and pink one, so every melanin gene resolves to a dreamier, "
                                    + "cooler colour. Requires two Blupnk copies.")
                            .marker(),
                    "bluepink", "textures/coat/lutbluepink.png"));

    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final FounderTable founders;

    public LutGene() {
        List<Allele> a = new ArrayList<>();
        a.add(n);
        VARIANTS.forEach(v -> a.add(v.allele()));
        this.alleles = List.copyOf(a);

        List<Expression> e = new ArrayList<>();
        e.add(WILD);
        VARIANTS.forEach(v -> {
            e.add(v.carrier());
            e.add(v.outcome());
        });
        this.expressions = List.copyOf(e);

        // Carriers only in the wild - a variant homozygote is never a founder,
        // the same rule Healer and the magic body-stat loci use. One carrier
        // row per variant (percentage points), the remainder plain n/n.
        FounderTable.Builder fb = FounderTable.builder();
        double carrierPercent = 100.0 / WILD_VARIANT_ONE_IN;
        for (Variant v : VARIANTS) {
            fb.weight(v.allele(), n, carrierPercent);
        }
        fb.weight(n, n, 100.0 - carrierPercent * VARIANTS.size());
        this.founders = fb.build();
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "LUT"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** No carrot for it (yet): it is a curiosity, and a "make my horse dreamy" carrot needs its own thought. */
    @Override public boolean hasGeneCarrot() { return false; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        for (Variant v : VARIANTS) {
            if (pair.count(v.allele()) == 2) {
                return v.outcome();
            }
        }
        for (Variant v : VARIANTS) {
            if (pair.count(v.allele()) == 1) {
                return v.carrier();
            }
        }
        return WILD;
    }

    // --- LutContribution ---------------------------------------------------

    @Override
    public Optional<String> alternateLut(AllelePair pair, Genotype genotype) {
        for (Variant v : VARIANTS) {
            if (pair.count(v.allele()) == 2) {
                return Optional.of(v.lutKey());
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> lutResources() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Variant v : VARIANTS) {
            out.put(v.lutKey(), v.texturePath());
        }
        return Map.copyOf(out);
    }
}
