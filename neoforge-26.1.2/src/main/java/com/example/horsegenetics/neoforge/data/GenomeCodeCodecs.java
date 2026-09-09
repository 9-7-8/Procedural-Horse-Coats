package com.example.horsegenetics.neoforge.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * <b>The one place a genome code's wire length is declared.</b>
 *
 * <p>A genotype code grows with the registered gene count and an epigenome code
 * grows far faster - it stores literal values for every varying gene, so
 * registering one wide gene moves it by hundreds of characters (known-gaps
 * &sect;66). Neither is anywhere near the 32&nbsp;767 characters that
 * {@code FriendlyByteBuf.writeUtf(String)} and {@code ByteBufCodecs.STRING_UTF8}
 * quietly default to, and NeoForge <b>throws</b> rather than truncating.
 *
 * <p>That is not hypothetical: 0.3.1 shipped with {@link
 * com.example.horsegenetics.neoforge.network.CoatSyncPayload} on the default
 * cap and a full code at 35&nbsp;568 characters, so every client was kicked
 * with an {@code EncoderException} the moment the first horse came into view -
 * i.e. on entering a new world. {@code EpigenomeSizeTest} was watching the
 * spawn payload's explicit cap and never saw the implicit one next to it.
 *
 * <p>So: <b>no genome code goes over the wire on a default-length string
 * codec.</b> Use these two, and when the test says to raise a cap, raise it
 * here - it is the only number to change.
 */
public final class GenomeCodeCodecs {

    /** Characters. Mirrored by {@code EpigenomeSizeTest.NETWORK_CAP} in {@code common/}. */
    public static final int MAX_EPIGENOME_CHARS = 131072;

    /**
     * Characters. Mirrored by {@code EpigenomeSizeTest.GENOTYPE_NETWORK_CAP}.
     * This was 8192, which sounds roomy and was not: a full genotype is already
     * 5612 characters and it grows with every registered gene, so it was two
     * thirds of the way to the same crash the epigenome had just caused.
     */
    public static final int MAX_GENOTYPE_CHARS = 32768;

    public static final StreamCodec<ByteBuf, String> EPIGENOME_CODE =
            ByteBufCodecs.stringUtf8(MAX_EPIGENOME_CHARS);

    public static final StreamCodec<ByteBuf, String> GENOTYPE_CODE =
            ByteBufCodecs.stringUtf8(MAX_GENOTYPE_CHARS);

    private GenomeCodeCodecs() {
    }
}
