package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * <b>Three colours on one saddle.</b>
 *
 * <p>Vanilla reads a stack's dye <i>once</i>, outside its layer loop, so every
 * layer of an equipment asset takes the same colour however many there are.
 * NeoForge provides this hook for exactly that limitation - it is handed the
 * layer index, so layer 0 can answer with the seat's colour, 1 with the
 * bridle's and 2 with the metal's.
 *
 * <p><b>Falls through to vanilla when there is no tint.</b> That is the whole
 * reason a plain saddle still looks like a plain saddle: with no component, each
 * layer resolves its own {@code color_when_undyed}, and those constants were
 * chosen so the three layers recompose to vanilla's texture exactly. A donkey,
 * a mule, a skeleton horse and a zombie horse all go through this path too.
 *
 * <p><b>Never returns 0.</b> Zero tells the renderer to skip a layer entirely,
 * so an unrecognised layer index answers {@code -1} (white, i.e. untinted)
 * rather than disappearing. {@link ARGB#opaque} guarantees the alpha byte is
 * set, so a legitimately black dye cannot come out as zero either - which it
 * otherwise would, and a black-dyed seat would vanish instead of being black.
 */
public final class TackClientExtensions implements IClientItemExtensions {

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer,
                                      int layerIdx, int fallbackColor) {
        SaddleTint tint = stack.get(ModDataComponents.TACK_TINT.get());
        if (tint == null) {
            return IClientItemExtensions.super.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
        }
        int colour = tint.forLayer(layerIdx);
        if (colour == -1) {
            return IClientItemExtensions.super.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
        }
        return ARGB.opaque(colour);
    }
}
