package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * One player's <b>holding pen</b>: where a holding pen ticket sends any horse
 * they own. Just the sign - the room behind it is measured afresh every time a
 * ticket is spent, the same as a stall (see {@code TicketHandler}), so a pen
 * that has been rebuilt is the pen the horse arrives in.
 *
 * <p>One per player: hanging a second holding pen sign moves the pen. Kept in
 * {@link StallData} beside the stalls.
 */
public record PenRecord(UUID owner, ResourceKey<Level> dimension, BlockPos signPos) {

    public static final Codec<PenRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(PenRecord::owner),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(PenRecord::dimension),
            BlockPos.CODEC.fieldOf("sign").forGetter(PenRecord::signPos)
    ).apply(i, PenRecord::new));
}
