package com.example.horsegenetics.neoforge.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * One horse's assigned stall: the bound horse ({@code horseId} + a display
 * {@code horseName}), the {@code dimension} and {@code signPos} of the stall
 * sign, and the block span the flood-fill found ({@code min}..{@code max}
 * inclusive, {@code blockCount} cells).
 *
 * <p>Persisted server-global in {@link StallData}. A later stall-teleport /
 * ticket feature reads {@link #center()} for the drop point.
 */
public record StallRecord(UUID horseId, String horseName, ResourceKey<Level> dimension,
                          BlockPos signPos, BlockPos min, BlockPos max, int blockCount) {

    public static final Codec<StallRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("horse_id").forGetter(StallRecord::horseId),
            Codec.STRING.optionalFieldOf("horse_name", "").forGetter(StallRecord::horseName),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(StallRecord::dimension),
            BlockPos.CODEC.fieldOf("sign").forGetter(StallRecord::signPos),
            BlockPos.CODEC.fieldOf("min").forGetter(StallRecord::min),
            BlockPos.CODEC.fieldOf("max").forGetter(StallRecord::max),
            Codec.INT.fieldOf("blocks").forGetter(StallRecord::blockCount)
    ).apply(i, StallRecord::new));

    /** The stall volume in world coordinates (min .. max+1). */
    public AABB bounds() {
        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
    }

    public net.minecraft.world.phys.Vec3 center() {
        return bounds().getCenter();
    }
}
