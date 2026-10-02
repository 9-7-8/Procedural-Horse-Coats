package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import org.junit.jupiter.api.Test;

import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The shared part-colour base, held to the horn colour gene it was cut out of.
 *
 * <p>{@link #hornColourIsUnchangedByTheMove} is a snapshot of everything a world or
 * a breeder can see of {@code horn_colour} - allele tokens and order (saved),
 * outcome ids and text, founder shares, the epigenome schema and the colours it
 * resolves to - taken <b>before</b> the gene moved onto the base. If it moves, the
 * move changed the gene.
 */
class PartColourGeneTest {

    /** CRC32 of {@link #dump} for horn_colour, taken on the pre-refactor class. */
    private static final long HORN_COLOUR_SNAPSHOT = 1097394079L;

    @Test
    void hornColourIsUnchangedByTheMove() {
        assertEquals(HORN_COLOUR_SNAPSHOT, crc(dump(Genes.HORN_COLOUR)),
                "horn_colour's observable state moved:\n" + dump(Genes.HORN_COLOUR));
    }

    /** Everything observable about a part-colour gene, as text. */
    static String dump(Gene gene) {
        StringBuilder out = new StringBuilder();
        out.append(gene.key()).append('|').append(gene.name()).append('|').append(gene.priority())
                .append('|').append(gene.isNatural()).append('|').append(gene.rarity())
                .append('|').append(gene.defaultAllele().token()).append('\n');
        for (Allele a : gene.alleles()) {
            out.append(a.order()).append(' ').append(a.token()).append(' ').append(a.label()).append('\n');
        }
        for (Expression e : gene.expressions()) {
            out.append(e.id()).append('|').append(e.name()).append('|').append(e.description())
                    .append('|').append(e.wildType()).append('\n');
        }
        for (Allele a : gene.alleles()) {
            for (Allele b : gene.alleles()) {
                AllelePair pair = new AllelePair(a, b);
                out.append(a.token()).append('/').append(b.token()).append(' ')
                        .append(gene.expressionOf(pair).id()).append(' ')
                        .append(String.format("%.9f", gene.founderTable(null).share(pair)))
                        .append('\n');
            }
        }
        for (EpiValue v : gene.epiSchema().values()) {
            out.append(v).append('\n');
        }
        return out.toString();
    }

    /** The colours horn_colour resolves to across pairs and seeds. */
    static String tints(Gene gene, java.util.function.BiFunction<Genotype, Epigenome, int[]> tints) {
        StringBuilder out = new StringBuilder();
        for (Allele a : gene.alleles()) {
            for (Allele b : gene.alleles()) {
                Genotype g = Genotype.wildType().with(new AllelePair(a, b));
                for (long seed = 0; seed < 4; seed++) {
                    int[] t = tints.apply(g, Epigenome.fromSeed(seed));
                    out.append(Integer.toHexString(t[0])).append(',').append(Integer.toHexString(t[1])).append(' ');
                }
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static final long HORN_TINTS_SNAPSHOT = 760788342L;

    @Test
    void hornColoursAreUnchangedByTheMove() {
        String t = tints(Genes.HORN_COLOUR, (g, e) -> {
            HornColourGene.Tints x = Genes.HORN_COLOUR.tintsFor(g, e);
            return new int[] {x.base(), x.tip()};
        });
        assertEquals(HORN_TINTS_SNAPSHOT, crc(t), t);
    }

    static long crc(String s) {
        CRC32 c = new CRC32();
        c.update(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return c.getValue();
    }
}
