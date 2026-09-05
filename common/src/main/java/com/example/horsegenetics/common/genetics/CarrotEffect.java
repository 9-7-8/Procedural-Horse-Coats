package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One thing a fed <b>breeding carrot</b> (roadmap wiki &sect;14) does to the
 * gamete a parent contributes. A carrot item / a live carrot window carries a
 * <i>list</i> of these (multiple carrots crafted together, &sect;14.3);
 * {@link #fold} turns the list into the single {@link GameteBias} the breeding
 * draw takes.
 *
 * <p>Pure {@code common/} - the translator only builds these and serialises
 * them; nothing here touches a game.
 */
public sealed interface CarrotEffect {

    /** A stable token for serialisation and tooltips. */
    String id();

    /** <i>Mutinogenic</i> - re-roll the epigenetic seed of every copy this parent passes on. */
    record Mutinogenic() implements CarrotEffect {
        @Override public String id() { return "mutinogenic"; }
    }

    /** <i>Stabilizer</i> - contribute the dominant (earlier-declared) copy at every heterozygous locus. */
    record Stabilizer() implements CarrotEffect {
        @Override public String id() { return "stabilizer"; }
    }

    /** <i>Magnifier</i> - contribute the recessive (later-declared) copy at every heterozygous locus. */
    record Magnifier() implements CarrotEffect {
        @Override public String id() { return "magnifier"; }
    }

    /**
     * <i>Chaos</i> - pick one random gene and draw this parent's gamete for it
     * from that gene's chaos distribution ({@link Gene#chaosTable()}, or a
     * uniform draw over its viable pairs if it declares none). The gene and the
     * pair are rolled at breeding time off the foal's own deterministic RNG.
     */
    record Chaos() implements CarrotEffect {
        @Override public String id() { return "chaos"; }
    }

    /**
     * A <i>magic gene carrot</i> - treat this parent as {@code n<gene>}
     * ({@code homozygous=false}, the default) or {@code <gene><gene>}
     * ({@code homozygous=true}) for that one gene's gamete. Normal Mendelian
     * rules apply from there.
     */
    record MagicGene(String geneKey, boolean homozygous) implements CarrotEffect {
        @Override public String id() { return "magic:" + geneKey + (homozygous ? ":hom" : ":het"); }
    }

    // ------------------------------------------------------------------
    // Serialisation - a flat string token, so the item component and the live
    // carrot window are just a List<String>. No polymorphic codec needed.
    // ------------------------------------------------------------------

    static java.util.Optional<CarrotEffect> parse(String token) {
        if (token == null) {
            return java.util.Optional.empty();
        }
        switch (token) {
            case "mutinogenic": return java.util.Optional.of(new Mutinogenic());
            case "stabilizer": return java.util.Optional.of(new Stabilizer());
            case "magnifier": return java.util.Optional.of(new Magnifier());
            case "chaos": return java.util.Optional.of(new Chaos());
            default:
                if (token.startsWith("magic:")) {
                    String[] p = token.split(":", 3);
                    if (p.length == 3 && (p[2].equals("hom") || p[2].equals("het"))) {
                        return java.util.Optional.of(new MagicGene(p[1], p[2].equals("hom")));
                    }
                }
                return java.util.Optional.empty();
        }
    }

    /** Parse a token list, silently dropping anything unrecognised (a retired gene, a bad edit). */
    static List<CarrotEffect> parseList(List<String> tokens) {
        List<CarrotEffect> out = new ArrayList<>();
        for (String t : tokens) {
            parse(t).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    static List<String> tokens(List<CarrotEffect> effects) {
        List<String> out = new ArrayList<>(effects.size());
        for (CarrotEffect e : effects) {
            out.add(e.id());
        }
        return List.copyOf(out);
    }

    /** True if this list contains both a stabilizer and a magnifier, or two magic carrots that disagree. */
    static boolean isContradictory(List<CarrotEffect> effects) {
        boolean stab = false;
        boolean magn = false;
        Map<String, Boolean> magic = new LinkedHashMap<>();
        for (CarrotEffect e : effects) {
            if (e instanceof Stabilizer) {
                stab = true;
            } else if (e instanceof Magnifier) {
                magn = true;
            } else if (e instanceof MagicGene mg) {
                Boolean prev = magic.putIfAbsent(mg.geneKey(), mg.homozygous());
                if (prev != null && prev != mg.homozygous()) {
                    return true;
                }
            }
        }
        return stab && magn;
    }

    // ------------------------------------------------------------------

    /**
     * Collapse a carrot's effect list into one {@link GameteBias} for this
     * parent. {@code parentGenotype} is read only to size a uniform chaos draw;
     * {@code rng} is the foal's breeding RNG, so the result is deterministic per
     * foal. An empty list yields {@link GameteBias#NONE}.
     */
    static GameteBias fold(List<CarrotEffect> effects, Genotype parentGenotype, Rng rng) {
        if (effects.isEmpty()) {
            return GameteBias.NONE;
        }
        boolean reroll = false;
        Boolean prefer = null; // true = stabilizer, false = magnifier
        Map<String, AllelePair> subs = new LinkedHashMap<>();

        for (CarrotEffect e : effects) {
            if (e instanceof Mutinogenic) {
                reroll = true;
            } else if (e instanceof Stabilizer) {
                prefer = Boolean.TRUE;
            } else if (e instanceof Magnifier) {
                prefer = Boolean.FALSE;
            } else if (e instanceof Chaos) {
                Gene g = randomChaosGene(rng);
                subs.put(g.key(), chaosPair(g, rng));
            } else if (e instanceof MagicGene mg) {
                Gene g = Genes.byKeyOrNull(mg.geneKey());
                if (g != null && g.hasMagicCarrot()) {
                    subs.put(g.key(), magicPair(g, mg.homozygous()));
                }
            }
        }

        return new GameteBias(reroll,
                prefer == null ? java.util.Optional.empty() : java.util.Optional.of(prefer),
                subs);
    }

    /** Every gene except the sex locus - a carrot must not flip a foal's sex (that is roadmap §5.3). */
    private static Gene randomChaosGene(Rng rng) {
        List<Gene> pool = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (!g.key().equals(Genes.SEX.key())) {
                pool.add(g);
            }
        }
        return pool.get(rng.nextInt(pool.size()));
    }

    private static AllelePair chaosPair(Gene gene, Rng rng) {
        return gene.chaosTable()
                .map(t -> t.draw(rng))
                .orElseGet(() -> {
                    List<AllelePair> pairs = GenotypeCatalog.allPairsOf(gene);
                    return pairs.get(rng.nextInt(pairs.size()));
                });
    }

    /** {@code n<gene>} or {@code <gene><gene>}, falling back to het if the homozygote cannot occur. */
    private static AllelePair magicPair(Gene gene, boolean homozygous) {
        Allele variant = gene.alleles().get(0);          // first-declared = the variant, by convention
        Allele baseline = gene.defaultAllele();
        if (homozygous) {
            AllelePair hom = new AllelePair(variant, variant);
            if (gene.canOccur(hom)) {
                return hom;
            }
        }
        return new AllelePair(variant, baseline);
    }
}
