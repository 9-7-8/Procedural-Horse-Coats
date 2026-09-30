package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: <b>the player pressed the blow-whistles key.</b> That is
 * the whole message.
 *
 * <p><b>It carries no item and no slot on purpose.</b> The obvious design has
 * the client work out which whistles to blow and name them, and that is the
 * {@link TackSlotPayload} lesson written out again: an index into a container
 * is a claim about that container, and a client does not get to be believed
 * about what is in one. So the client says only <em>that</em> the key was
 * pressed, and {@code server/WhistleBlowing} scans the sender's own inventory
 * for itself. A client that lied could at most press its own key.
 *
 * <p>Field-less, so {@link StreamCodec#unit} - the same shape as
 * {@link DismountPayload}, which is where to look if this ever needs changing.
 */
public record BlowWhistlesPayload() implements CustomPacketPayload {

    public static final Type<BlowWhistlesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "blow_whistles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlowWhistlesPayload> STREAM_CODEC =
            StreamCodec.unit(new BlowWhistlesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
