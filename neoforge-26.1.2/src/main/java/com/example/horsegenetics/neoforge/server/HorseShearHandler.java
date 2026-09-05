package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Shearing</b> (roadmap wiki &sect;12.1). Right-click an <b>adult</b> horse
 * with shears &rarr; 1&ndash;3 {@link ModItems#HORSE_HAIR}, regrowing once per
 * Minecraft day.
 *
 * <ul>
 *   <li>The once-a-day gate is {@link HorseCooldownsAttachment} keyed
 *       {@code "shear"} - a stored game time, so it survives a restart and the
 *       cooldown is inspectable, unlike the {@code yield} verb's old static
 *       map.</li>
 *   <li>Same trap as every other horse interaction: {@code EntityInteract}
 *       fires on both sides and vanilla turns any item used on a tamed horse
 *       into a mount, so the event is cancelled on <b>both</b> sides and the
 *       drop happens server-only (see {@link HorseInteractionHandler}).</li>
 *   <li>Bond {@code +5} per shear (&sect;13) via
 *       {@link HorseCareHandler#awardBondFor} - a no-op for an untamed horse,
 *       which can still be sheared for hair.</li>
 *   <li>The sheared <i>look</i> (a client render-layer overlay driven off this
 *       same cooldown stamp, settled &sect;21 as low-fidelity - no coat bake) is
 *       not yet built; see {@code wiki/verification.html}.</li>
 * </ul>
 */
@EventBusSubscriber
public final class HorseShearHandler {

    private static final int SHEAR_BOND = 5;

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.SHEARS)) {
            return;
        }
        Player player = event.getEntity();
        boolean client = event.getLevel().isClientSide();

        if (horse.isBaby()) {
            if (!client) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("message.horsegenetics.shear.foal"));
            }
            consume(event);
            return;
        }

        if (!client && horse.level() instanceof ServerLevel level) {
            shear(level, horse, player, stack, event.getHand());
        }
        consume(event);
    }

    private static void shear(ServerLevel level, Horse horse, Player player, ItemStack shears, InteractionHand hand) {
        long now = level.getGameTime();
        HorseCooldownsAttachment cooldowns = horse.getData(ModAttachments.HORSE_COOLDOWNS.get());
        if (!cooldowns.ready("shear", now)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("message.horsegenetics.shear.regrowing"));
            return;
        }

        int count = 1 + horse.getRandom().nextInt(3);
        ItemStack hair = new ItemStack(ModItems.HORSE_HAIR.get(), count);
        ItemEntity drop = new ItemEntity(level,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.5, horse.getZ(), hair);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);

        horse.setData(ModAttachments.HORSE_COOLDOWNS.get(), cooldowns.stamp("shear", now));

        if (!player.getAbilities().instabuild) {
            shears.hurtAndBreak(1, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.HORSE_HAIR.get()),
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.6, horse.getZ(),
                12, 0.3, 0.3, 0.3, 0.02);

        HorseCareHandler.awardBondFor(horse, SHEAR_BOND);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private HorseShearHandler() {
    }
}
