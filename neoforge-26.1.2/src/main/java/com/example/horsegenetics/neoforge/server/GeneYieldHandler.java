package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Map;

/**
 * The translator for a data-driven gene's {@code yield} effect - "the horse
 * produces something when you interact with it", and its <b>else branch</b>:
 * milking a stallion earns a kick, milking a foal earns a message
 * ({@code deniedDamage} / {@code deniedMessage}).
 *
 * <p>Fires on {@link PlayerInteractEvent.EntityInteract}: the first yield whose
 * trigger item and {@code when} both match wins. If it {@code produces}
 * something, the held item is swapped and a per-horse, per-gene cooldown is
 * stamped; if it produces nothing it is a denial - the punishment / message is
 * applied. Either way the interaction is cancelled on both sides so vanilla does
 * not also read it as a mount.
 *
 * <p>The cooldown now lives in {@link HorseCooldownsAttachment} keyed
 * {@code "yield:<geneKey>"} - a stored game time that survives a restart and is
 * inspectable, replacing the old {@code static Map} (roadmap &sect;7).
 */
@EventBusSubscriber
public final class GeneYieldHandler {

    private GeneYieldHandler() {}

    /** The output items a {@code yield} may name. Small and explicit on purpose. */
    private static final Map<String, Item> OUTPUTS = Map.of(
            "minecraft:water_bucket", Items.WATER_BUCKET,
            "minecraft:lava_bucket", Items.LAVA_BUCKET,
            "minecraft:milk_bucket", Items.MILK_BUCKET,
            "minecraft:bucket", Items.BUCKET,
            "minecraft:honey_bottle", Items.HONEY_BOTTLE,
            "minecraft:glass_bottle", Items.GLASS_BOTTLE,
            "minecraft:egg", Items.EGG,
            "minecraft:slime_ball", Items.SLIME_BALL);

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        if (!HorseAbilities.anyLoaded()) {
            return;
        }
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return;
        }

        List<HorseAbilities.Active> abilities;
        try {
            abilities = HorseAbilities.activeFor(Genotype.parse(record.geneticCode()),
                    Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException e) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        String heldId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        boolean client = event.getLevel().isClientSide();

        for (HorseAbilities.Active active : abilities) {
            if (!(active.ability() instanceof GeneAbility.Yield yield)) {
                continue;
            }
            String want = yield.trigger().item();
            if (!want.isEmpty() && !want.equals(heldId)) {
                continue;
            }
            if (!GeneAbilityHandler.conditionHolds(yield.when(), horse, record)) {
                continue;
            }

            if (!client) {
                if (yield.produces().isEmpty()) {
                    applyDenial(horse, player, yield);
                } else {
                    fulfil(horse, player, held, yield, active.geneKey());
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    private static void fulfil(Horse horse, Player player, ItemStack held, GeneAbility.Yield yield, String geneKey) {
        long now = horse.level().getGameTime();
        String key = "yield:" + geneKey;
        HorseCooldownsAttachment cooldowns = horse.getData(ModAttachments.HORSE_COOLDOWNS.get());
        long cd = Math.max(1, yield.cooldownTicks());
        if (!cooldowns.ready(key, now, cd)) {
            player.sendSystemMessage(
                    Component.translatable("message.horsegenetics.yield.recharging"));
            return;
        }

        Item output = OUTPUTS.get(yield.produces());
        if (output == null) {
            com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.info(
                    "[genes] yield output '{}' is not a recognised item - nothing produced", yield.produces());
            return;
        }

        if (!yield.consumes().isEmpty() && !player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack produced = new ItemStack(output);
        if (!player.addItem(produced)) {
            player.drop(produced, false);
        }
        horse.setData(ModAttachments.HORSE_COOLDOWNS.get(), cooldowns.stamp(key, now));
    }

    /** The else branch: a stallion kick, a foal's "nothing to give". */
    private static void applyDenial(Horse horse, Player player, GeneAbility.Yield yield) {
        if (yield.deniedDamage() > 0 && player.level() instanceof ServerLevel level) {
            player.hurtServer(level, horse.damageSources().mobAttack(horse), (float) yield.deniedDamage());
            horse.level().playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                    SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (!yield.deniedMessage().isEmpty()) {
            player.sendSystemMessage(Component.translatable(yield.deniedMessage()));
        }
    }
}
