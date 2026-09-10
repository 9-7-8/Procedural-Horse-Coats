package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server &rarr; client: which checklist tasks this player has done, so the
 * Getting Started tab can draw the ticks. Sent on login and after every task
 * completed.
 *
 * <p>Task ids rather than ordinals, so adding a task in the middle of the enum
 * cannot silently re-label somebody's finished checklist.
 */
public record ProgressSyncPayload(List<String> done) implements CustomPacketPayload {

    /** Comfortably more tasks than the checklist will ever have, and a real number. */
    private static final int MAX_TASKS = 512;

    public static final Type<ProgressSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "progress_sync"));

    public static final StreamCodec<ByteBuf, ProgressSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(MAX_TASKS)),
            ProgressSyncPayload::done,
            ProgressSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
