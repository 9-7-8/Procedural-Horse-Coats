package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The numbers a gene has written on a horse's two allele copies, formatted
 * for a person.</b> Pulled out of the gene inspector so every screen that shows
 * epigenetics shows it identically - the inspector, the horse-information
 * screen's Health / Coat / Other tabs, and anything the browser build grows
 * later.
 *
 * <p>One line per value, both copies side by side, with a {@code &rsaquo;} on
 * whichever copy the horse actually expresses:
 *
 * <pre>
 *   cover      &gt; 41%      9%
 *   coronet      .31 .80 .12 .60
 * </pre>
 *
 * <p>Empty for the majority of genes, which declare no schema because their
 * behaviour is fixed by their alleles - two {@code Hlr/Hlr} horses heal
 * identically and there is nothing per-horse to show.
 *
 * <p>Three presentation decisions, all deliberate: a declared {@code p_r} /
 * {@code p_g} / {@code p_b} triple collapses to one {@code #rrggbb}, because
 * three numbers are not a colour to a reader; a seed is truncated to six hex
 * digits, because the point of showing one is "these two horses match" and the
 * other ten digits say nothing a person can act on; and a number is three
 * decimals with trailing zeros trimmed, which is enough to tell two horses
 * apart and short enough to fit a row.
 */
public final class EpigenomeReadout {

    /** The mark on the copy the horse expresses. */
    public static final String EXPRESSED = "›";

    private EpigenomeReadout() {
    }

    /**
     * One line per value {@code gene} writes, or an empty list when the gene
     * declares no schema or this horse has no stored genome.
     */
    public static List<String> lines(Gene gene, Genotype genotype, Epigenome epigenome) {
        EpiSchema schema = gene.epiSchema();
        if (epigenome == null || schema.isEmpty()) {
            return List.of();
        }
        Epigenome.Copies copies = epigenome.copies(gene);
        boolean firstExpressed = epigenome.expressed(gene, genotype) == copies.first();
        EpiValues a = copies.first().values();
        EpiValues b = copies.second().values();

        List<String> out = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (EpiValue v : schema.values()) {
            if (done.contains(v.name())) {
                continue;
            }
            String colour = colourPrefix(schema, v.name());
            if (colour != null) {
                done.add(colour + "_r");
                done.add(colour + "_g");
                done.add(colour + "_b");
                out.add(row(colour, firstExpressed, hex(a.rgb(colour)), hex(b.rgb(colour))));
                continue;
            }
            done.add(v.name());
            out.add(row(v.name(), firstExpressed, show(v, a), show(v, b)));
        }
        return List.copyOf(out);
    }

    /** {@code <name>  <A>  <B>}, with a caret on whichever copy the horse shows. */
    private static String row(String name, boolean firstExpressed, String a, String b) {
        String clipped = name.length() <= 18 ? name : name.substring(0, 17) + "…";
        return clipped + "   "
                + (firstExpressed ? EXPRESSED : " ") + a + "   "
                + (firstExpressed ? " " : EXPRESSED) + b;
    }

    /**
     * {@code p} if {@code p_r}, {@code p_g} and {@code p_b} are all declared -
     * a colour, which reads far better as one hex value than as three numbers.
     */
    private static String colourPrefix(EpiSchema schema, String name) {
        if (!name.endsWith("_r")) {
            return null;
        }
        String prefix = name.substring(0, name.length() - 2);
        return schema.indexOf(prefix + "_g") >= 0 && schema.indexOf(prefix + "_b") >= 0
                ? prefix : null;
    }

    private static String hex(int rgb) {
        StringBuilder sb = new StringBuilder(Integer.toHexString(rgb & 0xFFFFFF));
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return "#" + sb;
    }

    /** One value, formatted for a person rather than for a codec. */
    private static String show(EpiValue v, EpiValues values) {
        if (v.kind() == EpiValue.Kind.SEED) {
            String h = Long.toHexString(values.seed(v.name()));
            return "#" + (h.length() > 6 ? h.substring(0, 6) : h);
        }
        if (v.kind() == EpiValue.Kind.CATEGORY) {
            return Integer.toString(values.category(v.name()));
        }
        if (v.arity() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int leg = 0; leg < v.arity(); leg++) {
                if (leg > 0) {
                    sb.append(' ');
                }
                sb.append(num(values.get(v.name(), leg)));
            }
            return sb.toString();
        }
        return num(values.get(v.name()));
    }

    /** Three decimals, trailing zeros trimmed - enough to tell two horses apart. */
    public static String num(double d) {
        long scaled = Math.round(d * 1000);
        StringBuilder sb = new StringBuilder();
        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }
        sb.append(scaled / 1000);
        long frac = scaled % 1000;
        if (frac != 0) {
            String f = Long.toString(frac);
            int end = f.length();
            while (end > 0 && f.charAt(end - 1) == '0') {
                end--;
            }
            sb.append('.');
            for (int pad = f.length(); pad < 3; pad++) {
                sb.append('0');
            }
            sb.append(f, 0, end);
        }
        return sb.toString();
    }
}
