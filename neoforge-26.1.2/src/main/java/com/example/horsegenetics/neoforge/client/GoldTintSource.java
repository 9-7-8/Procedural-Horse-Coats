package com.example.horsegenetics.neoforge.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One colour, and always the same one: the gold the golden carrot crop is
 * tinted with.
 *
 * <h2>Why a whole class for a constant</h2>
 * Because the alternative is a texture. The crop wears <b>vanilla's own carrot
 * sheets</b> and the seed icon wears vanilla's wheat seeds, so no art is
 * shipped or redistributed for either - the gold comes entirely from this
 * multiply. That is the same trade {@link MetalTintSource} makes for generated
 * horse armour, and the same reason: one tinted texture beats a folder of
 * near-identical PNGs nobody wants to maintain.
 *
 * <p>{@link #GOLD} is shared with {@code ClientSetup}'s block colour handler,
 * which colours the planted crop. <b>Two routes to one number</b>, because an
 * item tint cannot reach a block in the world and a block colour cannot reach
 * an item in a slot - so the constant lives here and both read it, rather than
 * the value being written down twice and drifting.
 */
public final class GoldTintSource implements ItemTintSource {

    /**
     * Warm gold, deliberately not the yellow of a gold ingot: multiplied over
     * carrot orange it needs to read as "golden carrot", and the ingot colour
     * over orange comes out muddy.
     */
    public static final int GOLD = 0xFFE9A5;

    public static final MapCodec<GoldTintSource> MAP_CODEC = MapCodec.unit(new GoldTintSource());

    @Override
    public int calculate(final ItemStack stack, final @Nullable ClientLevel level,
                         final @Nullable LivingEntity owner) {
        return ARGB.opaque(GOLD);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
