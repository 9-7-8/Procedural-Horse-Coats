package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseRecordCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: the {@link HorseRecord} for one horse, so the client can
 * show it on the inventory screen and open the family tree. Sent on
 * start-tracking and whenever the record changes. Cached in
 * {@code ClientHorseRecordCache} keyed by entity id.
 */
public record HorseRecordSyncPayload(int entityId, HorseRecord record) implements CustomPacketPayload {

    public static final Type<HorseRecordSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_record_sync"));

    public static final StreamCodec<ByteBuf, HorseRecordSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HorseRecordSyncPayload::entityId,
            HorseRecordCodecs.STREAM_CODEC, HorseRecordSyncPayload::record,
            HorseRecordSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
