package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>An epigenome code has to fit down the wire.</b>
 *
 * <p>{@code SpawnCustomHorsePayload} declares its epigenome field as
 * {@code ByteBufCodecs.stringUtf8(N)}, and NeoForge <b>throws</b> when an
 * encoded string exceeds that - so a code that outgrows the cap does not
 * degrade, it breaks custom-horse spawning outright, at the moment of use, with
 * a stack trace nowhere near the gene that pushed it over.
 *
 * <p>This is not hypothetical. The cap was 4096 while the old seed-based code
 * was already writing a segment for every registered gene - roughly eighty
 * characters each across sixty-odd genes - so it was over the line before this
 * test existed. Storing literal values made each segment far larger, and only
 * writing segments for genes that actually vary made there be far fewer of
 * them; the cap was raised to {@link #NETWORK_CAP} to leave real headroom
 * either way.
 *
 * <p>The margin matters more than the number: registering a gene with a wide
 * schema is a normal thing to do, and it should fail here rather than in the
 * game.
 *
 * <p>It has now fired twice, which is the point of it. The second time was the
 * batch of fourteen genes that added the mechanical loci: a full code went from
 * comfortably under 32&nbsp;768 to 35&nbsp;459 characters, so the cap doubled
 * again. Every gene with an epigenetic schema costs a segment, and the count
 * only goes up - so expect to be back here, and raise the cap rather than
 * trimming a gene to fit a number that was arbitrary in the first place.
 */
class EpigenomeSizeTest {

    /**
     * The cap {@code SpawnCustomHorsePayload} declares. Kept in sync by hand -
     * {@code common/} cannot see the NeoForge module, which is the whole point
     * of the split.
     */
    private static final int NETWORK_CAP = 131072;

    /** Fail while there is still room to add genes, not once it is too late. */
    private static final double HEADROOM = 0.5;

    @Test
    void aFullEpigenomeFitsWellInsideTheNetworkCap() {
        int worst = 0;
        for (long seed = 0; seed < 40; seed++) {
            worst = Math.max(worst, Epigenome.random(new SeededRng(seed)).toCode().length());
        }
        assertTrue(worst < NETWORK_CAP * HEADROOM,
                "an epigenome code is " + worst + " chars against a " + NETWORK_CAP
                        + " cap; raise the cap in SpawnCustomHorsePayload before this gets tight");
    }

    /** What the format actually costs, so a change to it is visible in the diff. */
    @Test
    void reportsTheCodeSize() {
        String code = Epigenome.random(new SeededRng(1L)).toCode();
        int genes = 0;
        for (Gene g : Genes.codeOrder()) {
            if (!g.epiSchema().isEmpty()) {
                genes++;
            }
        }
        assertTrue(genes > 0 && code.length() > 0);
        System.out.println("epigenome: " + code.length() + " chars over " + genes
                + " storing genes of " + Genes.codeOrder().size() + " registered");
    }
}
