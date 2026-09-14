package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * What the horse information screen's <b>Social</b> section shows, already in
 * words: the band role and what it means, where the horse stands among its
 * band-mates, its closest companions and its rival, by name. Sent while the screen
 * is open (with the inspect lease), because relationships change slowly and nobody
 * else needs them.
 */
public record HorseSocialSyncPayload(int entityId, String role, String roleDescription, String standing,
                                     List<String> companions, String rival) implements CustomPacketPayload {

    public static final Type<HorseSocialSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "social_sync"));

    public static final StreamCodec<ByteBuf, HorseSocialSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HorseSocialSyncPayload::entityId,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::role,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::roleDescription,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::standing,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), HorseSocialSyncPayload::companions,
            ByteBufCodecs.STRING_UTF8, HorseSocialSyncPayload::rival,
            HorseSocialSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
