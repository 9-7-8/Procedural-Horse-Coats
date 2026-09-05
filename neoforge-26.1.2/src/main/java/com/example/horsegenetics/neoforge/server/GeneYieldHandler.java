package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The translator for a data-driven gene's {@code yield} effect - "the horse
 * produces something when you interact with it". Waterborn's mares hand back a
 * water bucket for an empty one.
 *
 * <p>Fires on {@link PlayerInteractEvent.EntityInteract}: if the player is
 * holding the yield's {@code consumes} item and the {@code when} condition holds
 * (mare, tamed, ...), the held item is swapped for {@code produces} and the
 * interaction is cancelled on both sides so vanilla doesn't also read it as a
 * mount. A per-horse cooldown throttles it.
 *
 * <p><b>Not verified in-game</b> - written against 26.1.2 sources. Only a small
 * fixed set of output items is recognised ({@link #OUTPUTS}); an unknown
 * {@code produces} id is logged once and the yield does nothing.
 */
@EventBusSubscriber
public final class GeneYieldHandler {

    private GeneYieldHandler() {}

    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();

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
                fulfil(horse, player, held, yield);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    private static void fulfil(Horse horse, Player player, ItemStack held, GeneAbility.Yield yield) {
        long now = horse.level().getGameTime();
        Long readyAt = COOLDOWN.get(horse.getUUID());
        if (readyAt != null && now < readyAt) {
            return; // still recharging - the cancelled interaction is the only feedback for now
        }

        Item output = yield.produces().isEmpty() ? null : OUTPUTS.get(yield.produces());
        if (output == null) {
            if (!yield.produces().isEmpty()) {
                com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.info(
                        "[genes] yield output '{}' is not a recognised item - nothing produced", yield.produces());
            }
            return;
        }

        if (!yield.consumes().isEmpty() && !player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack produced = new ItemStack(output);
        if (!player.addItem(produced)) {
            player.drop(produced, false);
        }
        if (yield.cooldownTicks() > 0) {
            COOLDOWN.put(horse.getUUID(), now + yield.cooldownTicks());
        }
    }
}
