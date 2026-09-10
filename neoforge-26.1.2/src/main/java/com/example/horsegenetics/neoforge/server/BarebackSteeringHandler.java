package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>A horse that trusts you enough to be steered without a saddle.</b>
 *
 * <p>At the top bond tier ({@link HorseCareAttachment#behaviourTier()} 3, where
 * the horse already follows you around) its owner can climb on bare and ride
 * it properly. Below that tier, and for anyone who is not the owner, a bareback
 * rider is a passenger and the horse goes where it likes - which is vanilla's
 * behaviour and stays the default.
 *
 * <h2>How, given vanilla will not be asked</h2>
 * Vanilla decides who may steer inside
 * {@code AbstractHorse.getControllingPassenger()}, which hands control to a
 * player passenger <b>only if {@code isSaddled()}</b>. There is no event on
 * that call and this module has no mixins.
 *
 * <p>The first version of this drove the horse from outside on the entity tick
 * - read the rider's {@code zza}/{@code xxa}, point the animal, push it. It
 * worked and it felt wrong: server-driven, so a beat behind the input, with no
 * jump and none of vanilla's acceleration curve.
 *
 * <p>So it does the blunt thing instead and <b>puts a saddle on</b>. A real one,
 * in the real slot, so every part of vanilla's mounted path runs exactly as it
 * does for a saddled horse: client prediction, jumping, the lot. The saddle
 * carries {@link ModDataComponents#PHANTOM_SADDLE}, which is what stops it
 * being a lie:
 *
 * <ul>
 *   <li><b>It is never drawn.</b> {@code client/GeneticHorseRenderer} feeds the
 *       saddle layer an empty stack when the component is present. The mod owns
 *       the horse renderer, so this costs nothing.</li>
 *   <li><b>It is taken back off</b> the moment the rider is gone, the bond
 *       drops, or the rider turns out not to be the owner - checked every tick,
 *       so the window in which a phantom saddle exists without an eligible
 *       rider on it is one tick.</li>
 *   <li><b>It cannot be kept.</b> A player <i>can</i> open a horse's inventory
 *       while riding it, so {@link #reclaim} sweeps the rider's inventory for
 *       phantom saddles and destroys them. Without that, "ride your best horse
 *       bareback" would be a saddle duplicator.</li>
 * </ul>
 *
 * <p><b>A horse the player has saddled themselves is never touched</b> - the
 * component is the only thing that marks a saddle as ours, and it is set on
 * exactly the stacks this class creates.
 */
@EventBusSubscriber
public final class BarebackSteeringHandler {

    /** The bond tier at which a horse will take direction bare - the tier where it already follows you. */
    private static final int TIER = 3;

    private BarebackSteeringHandler() {
    }

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide()) {
            return;
        }
        boolean phantom = isPhantom(horse.getItemBySlot(EquipmentSlot.SADDLE));
        Player rider = eligibleRider(horse);

        if (rider != null && !horse.isSaddled()) {
            ItemStack saddle = new ItemStack(Items.SADDLE);
            saddle.set(ModDataComponents.PHANTOM_SADDLE.get(), true);
            horse.setItemSlot(EquipmentSlot.SADDLE, saddle);
            return;
        }
        if (phantom && rider == null) {
            horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
        }
        if (phantom && rider != null) {
            reclaim(rider);
        }
    }

    /**
     * The owner, riding, with the bond for it - or {@code null}.
     *
     * <p>{@code getFirstPassenger} rather than {@code getControllingPassenger},
     * because on the tick the saddle goes on there is by definition no
     * controlling passenger yet.
     */
    private static Player eligibleRider(AbstractHorse horse) {
        if (!horse.isAlive() || !horse.isVehicle() || !horse.isTamed()) {
            return null;
        }
        if (!(horse.getFirstPassenger() instanceof Player rider)) {
            return null;
        }
        LivingEntity owner = horse.getOwner();
        if (owner == null || !owner.getUUID().equals(rider.getUUID())) {
            return null;
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        return care.behaviourTier() >= TIER ? rider : null;
    }

    /**
     * Destroy any phantom saddle that has found its way into a player's hands.
     *
     * <p>The only route in is opening the horse's inventory while mounted and
     * dragging it out, which is a real thing a player can do and would otherwise
     * mint a free saddle every time. Destroying rather than returning it to the
     * horse keeps this from fighting the player over a slot: the tick after,
     * they are still eligible, and the horse gets a fresh one.
     */
    private static void reclaim(Player rider) {
        for (int i = 0; i < rider.getInventory().getContainerSize(); i++) {
            ItemStack stack = rider.getInventory().getItem(i);
            if (isPhantom(stack)) {
                rider.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (isPhantom(rider.containerMenu.getCarried())) {
            rider.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static boolean isPhantom(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModDataComponents.PHANTOM_SADDLE.get());
    }
}
