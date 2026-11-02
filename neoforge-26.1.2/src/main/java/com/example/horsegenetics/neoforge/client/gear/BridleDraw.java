package com.example.horsegenetics.neoforge.client.gear;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * <b>How a bridle in the Headstall is drawn</b> (#73).
 *
 * <p>The bridle is the vanilla saddle item, and vanilla's saddle asset draws the
 * whole saddle - seat, stirrups and all. A horse in a bridle alone wears none of
 * that, so the stack handed to the saddle-shaped layer is a <b>render-only copy</b>
 * pointed at {@code horsegenetics:bridle}, an asset with the same
 * {@code horse_saddle} layer on {@code EquineSaddleModel} but only two textures:
 * the bridle leather, and the hardware that is not on the seat (the bit and its
 * rings). Both are baked by {@code tools/tack/bake-saddle-textures.ps1} from the
 * same split the saddle's three zones come from, so the bridle drawn on its own
 * is pixel for pixel the bridle half of a vanilla saddle.
 *
 * <p>The copy never leaves the client and is never saved.
 */
public final class BridleDraw {

    /** The bridle-only equipment asset: two layers, bridle leather then hardware. */
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "bridle"));

    private static final Equippable DRAWN = Equippable.builder(EquipmentSlot.SADDLE).setAsset(ASSET).build();

    /** The stack to draw for what a horse wears in its Headstall, or empty for anything but a bridle. */
    public static ItemStack of(ItemStack headstall) {
        if (!HorseTackSlot.isBridle(headstall)) {
            return ItemStack.EMPTY;
        }
        ItemStack drawn = headstall.copyWithCount(1);
        drawn.set(DataComponents.EQUIPPABLE, DRAWN);
        return drawn;
    }

    /**
     * Whether {@code stack} is one of {@link #of}'s copies - which tells the tint
     * hook that layer 0 is the bridle zone and layer 1 the fittings, not the seat.
     */
    public static boolean isDrawn(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.assetId().filter(ASSET::equals).isPresent();
    }

    private BridleDraw() {
    }
}
