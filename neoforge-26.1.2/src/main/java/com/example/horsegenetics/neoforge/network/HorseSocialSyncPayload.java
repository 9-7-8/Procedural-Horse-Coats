package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the horse information screen's <b>Social</b> section shows, already in
 * words: the band role and what it means, where the horse stands among its
 * band-mates, its closest companions and its rival, by name - plus the one
 * {@code breeding} line the Body section shows for a mare (in heat, pregnant,
 * nursing), which rides along because it has the same audience. Sent while the screen
 * is open (with the inspect lease), because relationships change slowly and nobody
 * else needs them.
 */
public record HorseSocialSyncPayload(int entityId, String role, String roleDescription, String standing,
                                     List<Companion> companions, Optional<Companion> rival, String breeding)
        implements CustomPacketPayload {

    /**
     * One horse named in the Social block - <b>and the id behind the name</b>,
     * so the screen can send the reader to that horse rather than leaving them
     * a name to go and find. Everything about the label stays the server's
     * business: "(grooming partner)" is already in it, and the fallback for a
     * horse nobody has a record of is a phrase rather than a name.
     */
    public record Companion(UUID id, String label) {

        public static final StreamCodec<ByteBuf, Companion> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Companion::id,
                ByteBufCodecs.STRING_UTF8, Companion::label,
                Companion::new);
    }

    public static final Type<HorseSocialSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "social_sync"));

    public static final StreamCodec<ByteBuf, HorseSocialSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HorseSocialSyncPayload::entityId,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::role,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::roleDescription,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::standing,
            Companion.STREAM_CODEC.apply(ByteBufCodecs.list()), HorseSocialSyncPayload::companions,
            ByteBufCodecs.optional(Companion.STREAM_CODEC), HorseSocialSyncPayload::rival,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::breeding,
            HorseSocialSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
