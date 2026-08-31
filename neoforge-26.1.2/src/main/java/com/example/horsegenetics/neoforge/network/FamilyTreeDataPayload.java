package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseRecordCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server -> client: the records making up a family tree - the requested root
 * plus its ancestors, {@code ancestorsOf(root, depth)}. The client merges
 * these into its record cache and (re)builds the family-tree screen.
 */
public record FamilyTreeDataPayload(List<HorseRecord> records) implements CustomPacketPayload {

    public static final Type<FamilyTreeDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "family_tree_data"));

    public static final StreamCodec<ByteBuf, FamilyTreeDataPayload> STREAM_CODEC = StreamCodec.composite(
            HorseRecordCodecs.LIST_STREAM_CODEC, FamilyTreeDataPayload::records,
            FamilyTreeDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
