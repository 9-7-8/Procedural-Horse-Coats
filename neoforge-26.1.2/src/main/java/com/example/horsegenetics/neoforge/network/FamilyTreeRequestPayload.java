package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Client -> server: "send me the family tree rooted at this horse id". The
 * server replies with a {@link FamilyTreeDataPayload}. Fired when the family
 * tree screen opens and each time the player clicks an ancestor to re-centre.
 */
public record FamilyTreeRequestPayload(UUID rootId) implements CustomPacketPayload {

    public static final Type<FamilyTreeRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "family_tree_request"));

    public static final StreamCodec<ByteBuf, FamilyTreeRequestPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FamilyTreeRequestPayload::rootId,
            FamilyTreeRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
