package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.gear.Saddles;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.item.HorseSaddleItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * <b>What the mod's saddle does to the horse</b> (#75): its tier's share of the
 * horse's own speed and jump, <b>only while a player rides</b>.
 *
 * <p>The rule is {@code common/gear/Saddles}; this reads the saddle slot and the
 * passengers, asks it, and holds the answer up as one saddle-owned modifier on
 * each attribute, the way {@link HorseShoeEffects} holds the shoes'. Nothing
 * ticks: the answer changes only when the saddle slot is written
 * ({@link LivingEquipmentChangeEvent}) or a player gets on or off
 * ({@link EntityMountEvent}), so those are when it runs. The modifiers are
 * transient, never in the save; a horse that loads with a player already aboard
 * is caught by its join.
 *
 * <p><b>The horse's own screen does not show it</b> (treatment #72): the stat is
 * the rider's, not the horse's. {@link #withoutSaddle} is what the screen reads.
 *
 * <p><b>UNVERIFIED in a played game:</b> that {@code EntityMountEvent} fires for
 * every way onto and off a horse (a dismount by death, a teleport, a boat). The
 * gametest {@code a_saddle_adds_only_while_ridden} drives mount and dismount.
 */
@EventBusSubscriber
public final class SaddleEffects {

    public static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "saddle/speed");
    public static final Identifier JUMP_ID = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "saddle/jump");

    private SaddleEffects() {
    }

    /** Whether a player is aboard, the one this answer is for. */
    private static boolean playerAboard(AbstractHorse horse, Entity leaving) {
        for (Entity passenger : horse.getPassengers()) {
            if (passenger != leaving && passenger instanceof Player) {
                return true;
            }
        }
        return false;
    }

    /** Re-read the saddle and the riders, and set the two modifiers to match. Server side. */
    public static void refresh(AbstractHorse horse) {
        refresh(horse, playerAboard(horse, null));
    }

    private static void refresh(AbstractHorse horse, boolean ridden) {
        if (horse.level().isClientSide()) {
            return;
        }
        Saddles.Tier tier = HorseSaddleItem.tierOf(horse.getItemBySlot(EquipmentSlot.SADDLE));
        Saddles.Bonus bonus = Saddles.bonus(ServerConfig.saddleRates(), tier, ridden);
        hold(horse.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, bonus.speed());
        hold(horse.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, bonus.jump());
    }

    private static void hold(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier on = attribute.getModifier(id);
        if (amount == 0.0) {
            if (on != null) {
                attribute.removeModifier(id);
            }
        } else if (on == null || on.amount() != amount) {
            // A share of the horse's own base: a saddle is worth the same
            // proportion to a slow horse as to a fast one.
            attribute.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /**
     * The attribute's value with the saddle's share taken back out, for the
     * horse's own screen: the saddle is the rider's, not the horse's. Both sides;
     * the modifiers reach the client with the attribute.
     */
    public static double withoutSaddle(AbstractHorse horse, Holder<Attribute> attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance == null) {
            return horse.getAttributeValue(attribute);
        }
        Identifier id = attribute == Attributes.MOVEMENT_SPEED ? SPEED_ID : JUMP_ID;
        if (instance.getModifier(id) == null) {
            return instance.getValue();
        }
        // AttributeInstance.calculateValue's order with the saddle's modifier
        // left out. Subtracting base x share was off whenever an ADD_VALUE sat
        // beside it (shoes): the share multiplies base plus the adds, not the base.
        double added = instance.getBaseValue();
        for (AttributeModifier m : instance.getModifiers()) {
            if (m.operation() == AttributeModifier.Operation.ADD_VALUE) {
                added += m.amount();
            }
        }
        double value = added;
        for (AttributeModifier m : instance.getModifiers()) {
            if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE && !m.id().equals(id)) {
                value += added * m.amount();
            }
        }
        for (AttributeModifier m : instance.getModifiers()) {
            if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0 + m.amount();
            }
        }
        return attribute.value().sanitizeValue(value);
    }

    @SubscribeEvent
    static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.SADDLE && event.getEntity() instanceof AbstractHorse horse) {
            refresh(horse);
        }
    }

    // LOWEST, and never a cancelled one: the event is cancellable, and a mount
    // another handler refused must not leave the bonus on.
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onMount(EntityMountEvent event) {
        // Fired before the passenger list changes, so the rider in question is
        // counted by hand: in when mounting, out when getting off.
        if (event.getEntityBeingMounted() instanceof AbstractHorse horse
                && event.getEntityMounting() instanceof Player) {
            boolean ridden = event.isMounting() || playerAboard(horse, event.getEntityMounting());
            refresh(horse, ridden);
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractHorse horse
                && HorseSaddleItem.tierOf(horse.getItemBySlot(EquipmentSlot.SADDLE)) != null) {
            refresh(horse);
        }
    }
}
