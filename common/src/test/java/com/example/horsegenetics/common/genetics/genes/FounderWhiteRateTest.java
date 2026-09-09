package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>How much of the wild population wears ordinary white markings, and how
 * loudly.</b>
 *
 * <p>This is the measurement known-gaps gap 53 asked for. Two independent loci
 * hand out everyday stars, socks and blazes - {@code MITF}'s {@code SW1}, which
 * a majority of founders carry, and the {@code KIT} boosters, carried by
 * roughly two in five. They are different genes, so they stack, and "white finds
 * white" means the second one to paint runs hotter for the first one's work.
 * Both loci were individually reasonable and <b>nothing had ever measured them
 * together</b>: the per-genotype tests pin one genotype at a time and the golden
 * files pin specific horses, so neither can answer "what does a paddock look
 * like". The worry was a wild population that reads as a pinto herd.
 *
 * <p><b>It does not.</b> Roughly two founders in three carry some white from
 * these two loci, nearly all of it in the star-and-socks range, and only a small
 * minority are loudly marked. That is a defensible naturalistic population -
 * real horse populations are mostly marked - so this test exists to <i>hold</i>
 * the number rather than to condemn it. It is also the instrument those two
 * frequencies should be tuned against, which is the thing gap 53 actually
 * wanted: {@code MitfGene.WILD_SW1_PERCENT} and {@code KitGene.frequencies()}
 * were previously only ever tuned against each other.
 *
 * <h2>Why it differences two renders instead of just counting pale texels</h2>
 *
 * <p><b>Counting white on a founder directly gives the wrong answer, and gives
 * it confidently.</b> Measured that way the population comes out 91% "white",
 * which looks like a catastrophe and is an artefact: on the flat greyscale chart
 * these tests use, a cremello, a grey and a palomino are all pale <i>coats</i>
 * with no marking anywhere on them, and the threshold cannot tell a white
 * marking from a white horse. The giveaway was that 91% wore white while only
 * 78% carried either locus at all.
 *
 * <p>So each founder is composed <b>twice</b> - as rolled, and again with these
 * loci forced to wild type - and the white that appears between the two is the
 * white these loci are responsible for. Both renders share a base coat, a
 * gradient and an epigenome, so everything else cancels.
 */
class FounderWhiteRateTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** The loci that hand out ordinary markings, i.e. the ones being measured. */
    private static final String[] MARKING_LOCI = {"mitf", "pax3", "kit"};

    /**
     * Founders per sample. 200 is two composes each, and it is enough to place
     * the marked share to about +/-3 points - far finer than the band asserted.
     */
    private static final int FOUNDERS = 200;

    /** Below this share of the sheet, "white" is noise rather than a marking. */
    private static final double PRESENT = 0.005;

    /** Above this share, a horse is not "marked" any more - it is a pinto. */
    private static final double LOUD = 0.25;

    @Test
    void mostFoundersAreMarkedAndFewAreLoud() {
        int marked = 0;
        int loud = 0;
        double total = 0;
        for (int i = 0; i < FOUNDERS; i++) {
            double w = markingWhite(i);
            total += w;
            if (w > PRESENT) {
                marked++;
            }
            if (w > LOUD) {
                loud++;
            }
        }
        double markedShare = marked / (double) FOUNDERS;
        double loudShare = loud / (double) FOUNDERS;
        double mean = total / FOUNDERS;

        String report = String.format(
                "founder white from %s: marked %.1f%%, loud %.1f%%, mean coverage %.3f",
                String.join("+", MARKING_LOCI), markedShare * 100, loudShare * 100, mean);

        // A WIDE band on purpose. The point is not to pin today's number - that
        // would go red every time a founder frequency is touched, which is the
        // opposite of an instrument you can tune against. The point is to catch
        // the two outcomes that are actually wrong: a population with no white
        // in it, and a population that reads as a pinto herd.
        assertTrue(markedShare > 0.45 && markedShare < 0.85,
                "the share of founders wearing ordinary white markings left its band - "
                        + "that is not automatically wrong, but it is a population-level "
                        + "change and wants looking at. " + report);
        assertTrue(loudShare < 0.20,
                "too many wild founders are loudly marked - the paddock is reading as a "
                        + "pinto herd, which is what gap 53 was opened to watch for. " + report);
        assertTrue(mean > 0.02 && mean < 0.25,
                "mean white coverage from the marking loci left its band. " + report);
    }

    /**
     * White on founder {@code i} that the marking loci put there - the
     * difference between the horse as rolled and the same horse with those loci
     * wild. See this class's javadoc for why the difference is necessary.
     */
    private static double markingWhite(int i) {
        Genome g = Genome.random(new SeededRng(i, "founder"));
        double asRolled = whiteFraction(g.genotype(), g.epigenome());
        double withoutMarkings = whiteFraction(wildAt(g.genotype(), MARKING_LOCI), g.epigenome());
        return Math.max(0, asRolled - withoutMarkings);
    }

    /** The same genotype with the named genes put back to their wild-type pair. */
    private static Genotype wildAt(Genotype genotype, String... genes) {
        String[] segments = genotype.toCode().split("-");
        String[] wild = Genotype.wildType().toCode().split("-");
        for (int i = 0; i < segments.length; i++) {
            for (String gene : genes) {
                if (segments[i].contains("." + gene + "=")) {
                    segments[i] = wild[i];
                }
            }
        }
        return Genotype.parse(String.join("-", segments));
    }

    private static double whiteFraction(Genotype genotype, Epigenome epigenome) {
        int[] image = CoatTextureComposer.compose(
                genotype, epigenome, Skin.ADULT, true, template(), greyscale());
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            tally[1]++;
            if ((image[py * N + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    private static int[] template() {
        int[] template = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                template[py * N + px] = 0xFFFFFFFF);
        return template;
    }

    /** A flat white-to-black chart, so "pale" means pigment and nothing else. */
    private static GradientLut greyscale() {
        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        return new GradientLut(lut, 16, 16);
    }
}
