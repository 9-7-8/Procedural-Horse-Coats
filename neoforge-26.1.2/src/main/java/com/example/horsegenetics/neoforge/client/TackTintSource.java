package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Tints the saddle's <b>inventory icon</b> from our own {@code tack_tint}.
 *
 * <p>Without this the icon reads vanilla's {@code dyed_color}, which a dyed
 * saddle does not carry - so a saddle that renders in three colours on the horse
 * sat in the hotbar looking plain, and looked for all the world like the dye had
 * not been stored at all.
 *
 * <p><b>An icon can only show one colour</b>, and it shows the <b>seat</b>: it is
 * the largest area on the real saddle and the one a player means when they say
 * what colour a saddle is. The bridle and the fittings are in the tooltip
 * instead, which is the only place all three can be read.
 *
 * <p>Undyed falls back to the icon's own undyed constant rather than to white.
 * The icon's base was divided by that number, so returning it reproduces
 * vanilla's saddle exactly - the same guarantee the worn saddle has.
 */
public final class TackTintSource implements ItemTintSource {

    /** Matches {@code items/saddle.json}'s old default and the icon bake's divisor. */
    public static final int UNDYED_ICON = 0xF19988;

    /**
     * Leather horse armour's icon divisor - vanilla's own {@code -6265536}. Its
     * icon base is greyscale already, so this is simply the colour vanilla
     * multiplies it by to get the armour everyone recognises.
     */
    public static final int UNDYED_ARMOUR_ICON = 0xA06540;

    public static final MapCodec<TackTintSource> MAP_CODEC =
            MapCodec.unit(new TackTintSource());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        SaddleTint tint = stack.get(ModDataComponents.TACK_TINT.get());
        if (tint != null) {
            return ARGB.opaque(tint.seat());
        }
        if (stack.is(net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR)) {
            // Replacing vanilla's tint source must not cost vanilla its own
            // behaviour. Leather horse armour can still be dyed in a crafting
            // grid, which sets dyed_color and nothing else - so with no
            // tack_tint we answer exactly what vanilla's dye source would have,
            // and a grid-dyed armour keeps a correctly coloured icon.
            return ARGB.opaque(net.minecraft.world.item.component.DyedItemColor
                    .getOrDefault(stack, UNDYED_ARMOUR_ICON));
        }
        return ARGB.opaque(UNDYED_ICON);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
