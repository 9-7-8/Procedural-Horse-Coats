package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Client -&gt; server: <b>the command whistle's pick</b>. {@code entityIds} is the aimed
 * horse (one id), or empty with {@code all} set for a sneak-hold over every horse of the
 * player's in reach. The order travels by NAME, as it is saved.
 *
 * <p>Nothing in it is trusted. {@code HorseOrdering.handle} re-finds the horses and
 * re-checks ownership, reach, the lead, the cart and the bond on the server.
 */
public record CommandHorsePayload(List<Integer> entityIds, boolean all, String order)
        implements CustomPacketPayload {

    public static final Type<CommandHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "command_horse"));

    public static final StreamCodec<ByteBuf, CommandHorsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), CommandHorsePayload::entityIds,
            ByteBufCodecs.BOOL, CommandHorsePayload::all,
            ByteBufCodecs.STRING_UTF8, CommandHorsePayload::order,
            CommandHorsePayload::new);

    public HorseOrder parsedOrder() {
        return HorseOrder.byName(order);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
