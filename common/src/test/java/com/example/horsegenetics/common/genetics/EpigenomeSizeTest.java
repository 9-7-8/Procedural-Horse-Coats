package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>An epigenome code has to fit down the wire.</b>
 *
 * <p>{@code GenomeCodeCodecs} declares the one cap every genome-code field on
 * the wire is written with, and NeoForge <b>throws</b> when an encoded string
 * exceeds it - so a code that outgrows the cap does not degrade, it breaks at
 * the moment of use, with a stack trace nowhere near the gene that pushed it
 * over.
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
 *
 * <p><b>And it is only worth as much as the caps it is watching.</b> The same
 * crossing of 32&nbsp;768 that fired this test also broke 0.3.0 and 0.3.1
 * outright: {@code CoatSyncPayload} was writing the epigenome on a plain
 * {@code buf.writeUtf(v)}, whose <i>implicit</i> cap is 32&nbsp;767, so every
 * client was kicked with an {@code EncoderException} the moment a horse came
 * into view. The test was watching the one cap that had a number written next
 * to it and could not see the one that did not. Hence {@code GenomeCodeCodecs}:
 * the caps are in a single place so this test guards all of them at once, and
 * a genome code on a default-length string codec is now a bug on sight.
 */
class EpigenomeSizeTest {

    /**
     * The cap {@code GenomeCodeCodecs.MAX_EPIGENOME_CHARS} declares. Kept in
     * sync by hand - {@code common/} cannot see the NeoForge module, which is
     * the whole point of the split.
     */
    private static final int NETWORK_CAP = 131072;

    /**
     * The cap {@code GenomeCodeCodecs.MAX_GENOTYPE_CHARS} declares. A genotype
     * code is the smaller half and grows only with the gene count, but it
     * travels on the same payloads, so it is guarded on the same terms - and
     * this check earned its place on the first run, failing at 5612 characters
     * against the 8192 the payloads had been declaring.
     */
    private static final int GENOTYPE_NETWORK_CAP = 32768;

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
                        + " cap; raise the cap in GenomeCodeCodecs before this gets tight");
    }

    @Test
    void aFullGenotypeFitsWellInsideTheNetworkCap() {
        int worst = 0;
        for (long seed = 0; seed < 40; seed++) {
            worst = Math.max(worst, Genotype.random(new SeededRng(seed)).toCode().length());
        }
        assertTrue(worst < GENOTYPE_NETWORK_CAP * HEADROOM,
                "a genotype code is " + worst + " chars against a " + GENOTYPE_NETWORK_CAP
                        + " cap; raise the cap in GenomeCodeCodecs before this gets tight");
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
