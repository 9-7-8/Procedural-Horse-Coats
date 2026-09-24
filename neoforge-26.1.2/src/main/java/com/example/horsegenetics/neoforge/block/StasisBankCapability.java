package com.example.horsegenetics.neoforge.block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * <b>The one part of the Horse Stasis Bank other machines can reach.</b>
 *
 * <p>The bank deliberately is not a {@link net.minecraft.world.Container} - it
 * <i>holds</i> one, the way the research shelf does - so nothing outside it can
 * file or fetch a chamber. That was the right call and it stays: automating the
 * shelving of a live animal is not a thing anybody asked for, and the cheapest
 * way not to have it is not to expose the grid.
 *
 * <p>The <b>supply slots</b> are the opposite case (owner, 2026-09-24). Feed and
 * water are bulk goods that run out, and keeping a stud farm's bank topped up by
 * hand is exactly the chore automation exists for - so this registers an item
 * handler over {@link HorseStasisBankBlockEntity#supplies()} and nothing else.
 * A hopper, a dropper or another mod's pipework can push hay and water buckets
 * in and pull the empty buckets out; none of it can see a chamber, because the
 * chamber grid is not part of the handler at all.
 *
 * <h2>What the slots refuse is enforced once</h2>
 * NeoForge's {@link VanillaContainerWrapper} asks
 * {@code Container.canPlaceItem} before it inserts, so the per-slot rules the
 * block entity already writes for the menu are the same rules a pipe obeys -
 * feed in the feed slot, water in the water slot, and nothing at all into the
 * empties, which is an output. There is no second copy of that list to drift.
 *
 * <h2>API note - 26.1.2 has no {@code IItemHandler}</h2>
 * <b>Not verified in a running game.</b> Written against the 26.1.2 sources: the
 * item capability is {@code Capabilities.Item.BLOCK} and its type is
 * {@code ResourceHandler<ItemResource>}, NeoForge's newer transfer API, not the
 * {@code IItemHandler} every tutorial still shows. The patched
 * {@code HopperBlockEntity} falls back to that same capability when the target
 * is not a {@code Container}, which is what makes a vanilla hopper work here.
 * See {@code wiki/api-notes.html}.
 */
public final class StasisBankCapability {

    private StasisBankCapability() {
    }

    /** Called from the mod constructor - the event is a mod-bus one. */
    public static void listen(IEventBus modEventBus) {
        modEventBus.addListener(StasisBankCapability::register);
    }

    private static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.HORSE_STASIS_BANK.get(),
                (bank, side) -> VanillaContainerWrapper.of(bank.supplies()));
    }
}
