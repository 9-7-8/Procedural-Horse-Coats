package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.SaddleTint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Puts a dyed saddle's three colours on its tooltip.
 *
 * <h2>Why a component that describes itself is not enough</h2>
 * {@link SaddleTint} implements vanilla's {@code TooltipProvider}, which looks
 * like it should be sufficient and is not: {@code ItemStack} gathers component
 * tooltips from a <b>hardcoded list</b> of vanilla component types -
 * {@code DYED_COLOR}, {@code TRIM}, {@code LORE} and the rest - so nothing ever
 * asks a modded component for its lines. The interface is the right place for
 * the <i>text</i>; this event is the only thing that will call it.
 *
 * <p>Reported as "it should still fill the tooltip with what's been dyed, and it
 * isn't" (2026-09-16). The lines still live on the component, so there remains
 * one place that decides how a colour is named.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class TackTooltip {

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        SaddleTint tint = event.getItemStack().get(ModDataComponents.TACK_TINT.get());
        if (tint == null) {
            return;
        }
        tint.addToTooltip(
                event.getContext(),
                event.getToolTip()::add,
                event.getFlags(),
                event.getItemStack().getComponents());
    }

    private TackTooltip() {
    }
}
