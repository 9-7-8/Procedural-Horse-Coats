package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.name.NamingPolicy;
import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Client &rarr; server on login: how this player would like foals born to
 * <i>their</i> horses named. The server files it under their UUID
 * ({@code HorseNamingData}) and is the only thing that ever names a foal, so
 * two players on one server keep different policies and neither reaches the
 * other's horses.
 *
 * <p>Enum <b>names</b>, not ordinals - the same reason
 * {@link ProgressSyncPayload} sends task ids. Reordering an enum must not
 * silently turn somebody's setting into a different one, and an unreadable
 * value is not worth dropping a player over: {@link #policy()} falls back to
 * {@link NamingPolicy#DEFAULT} per half rather than throwing.
 */
public record NamingPolicyPayload(String half, String source) implements CustomPacketPayload {

    /** Longer than any constant name; a stray long string is not worth parsing. */
    private static final int MAX_NAME = 32;

    public static final Type<NamingPolicyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "naming_policy"));

    public static final StreamCodec<ByteBuf, NamingPolicyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME), NamingPolicyPayload::half,
            ByteBufCodecs.stringUtf8(MAX_NAME), NamingPolicyPayload::source,
            NamingPolicyPayload::new);

    public static NamingPolicyPayload of(NamingPolicy policy) {
        return new NamingPolicyPayload(policy.half().name(), policy.source().name());
    }

    /** The policy this payload asks for, with anything unreadable left at the default. */
    public NamingPolicy policy() {
        NamingPolicy.InheritedHalf h = NamingPolicy.DEFAULT.half();
        NamingPolicy.ParentSource s = NamingPolicy.DEFAULT.source();
        try {
            h = NamingPolicy.InheritedHalf.valueOf(half.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            // a client from a build that knows a half this one doesn't
        }
        try {
            s = NamingPolicy.ParentSource.valueOf(source.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            // likewise
        }
        return new NamingPolicy(h, s);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
