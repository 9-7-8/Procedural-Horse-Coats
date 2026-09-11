package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.progress.ProgressTask;
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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
            "minecraft:slime_ball", Items.SLIME_BALL,
            "minecraft:potion", Items.POTION);

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
                    fulfil(horse, player, held, yield, active.geneKey(),
                            chargesFor(yield.kind(), abilities, horse, record),
                            sameKindEffects(yield, abilities, horse, record));
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    /**
     * How many uses of this {@code kind} of yield the horse's other genes have
     * granted - the {@code charges} verb, summed.
     *
     * <p>It is looked up by <b>kind</b> rather than by gene, which is the whole
     * point of that verb: one volume locus governs every gene that produces the
     * kind, including ones written after it. A yield with no kind (the denial
     * branches, and anything that opted out) can never be boosted, because an
     * empty kind matches nothing.
     */
    private static int chargesFor(String kind, List<HorseAbilities.Active> abilities,
                                  Horse horse, HorseRecord record) {
        if (kind.isEmpty()) {
            return 1;
        }
        int total = 1;
        for (HorseAbilities.Active active : abilities) {
            if (active.ability() instanceof GeneAbility.YieldCharges charges
                    && charges.kind().equals(kind)
                    && GeneAbilityHandler.conditionHolds(charges.when(), horse, record)) {
                total += charges.extra();
            }
        }
        return total;
    }

    /** A filled bucket off a mare is the milk task - the other yields are their own thing. */
    private static void tickYieldTask(Player player, ItemStack held) {
        if (held.is(net.minecraft.world.item.Items.BUCKET)) {
            HorseProgress.complete(player, ProgressTask.MILK_MARE);
        }
    }

    /**
     * <b>Every potion effect this horse's yields of one kind ask for.</b>
     *
     * <p>This is the merge, and it is the one part of the potion-milk locus that
     * is not free. A compound heterozygote carries <i>two</i> yields triggered by
     * the same bottle, each naming its own effect - and without this the first
     * one would fire and the second would be silently dropped, handing back a
     * single-effect potion while the gene's page promises two.
     *
     * <p>It is done here rather than in {@code common/} on purpose: what a potion
     * item can carry is a Minecraft question, and the genetics side does not get
     * to know the answer. It only declares two yields and lets the translator
     * decide they are one bottle.
     *
     * <p>An unrecognised effect id is <b>skipped</b> rather than producing an
     * empty potion, because a bottle that comes back with nothing in it is worse
     * than no bottle at all - it has consumed the glass.
     */
    private static List<MobEffectInstance> sameKindEffects(GeneAbility.Yield yield,
                                                           List<HorseAbilities.Active> abilities,
                                                           Horse horse, HorseRecord record) {
        if (yield.potionEffect().isEmpty()) {
            return List.of();
        }
        List<MobEffectInstance> out = new ArrayList<>(2);
        Set<String> seen = new HashSet<>();
        for (HorseAbilities.Active active : abilities) {
            if (!(active.ability() instanceof GeneAbility.Yield other)) {
                continue;
            }
            if (other.potionEffect().isEmpty() || !other.kind().equals(yield.kind())) {
                continue;
            }
            if (!other.trigger().item().equals(yield.trigger().item())) {
                continue;
            }
            if (!GeneAbilityHandler.conditionHolds(other.when(), horse, record)) {
                continue;
            }
            if (!seen.add(other.potionEffect())) {
                continue;
            }
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT
                    .get(Identifier.parse(other.potionEffect())).orElse(null);
            if (effect == null) {
                com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.info(
                        "[genes] potion effect '{}' is not recognised - left out of the bottle",
                        other.potionEffect());
                continue;
            }
            out.add(new MobEffectInstance(effect, other.potionDurationTicks(),
                    other.potionAmplifier()));
        }
        return List.copyOf(out);
    }

    private static void fulfil(Horse horse, Player player, ItemStack held, GeneAbility.Yield yield,
                               String geneKey, int charges,
                               List<MobEffectInstance> effects) {
        tickYieldTask(player, held);
        long now = horse.level().getGameTime();
        String key = "yield:" + geneKey;
        HorseCooldownsAttachment cooldowns = horse.getData(ModAttachments.HORSE_COOLDOWNS.get());
        // Charges DIVIDE the cooldown rather than banking uses. "Three times a
        // day" then means three fillings spread across the day rather than
        // three at dawn and nothing after - which is what a dairy animal does,
        // and which needs no counter of its own: the stamp already on the
        // attachment is enough, so the whole feature survives a restart for
        // free. A burstable version would need a second stored number and would
        // let a player empty the horse and walk away.
        long cd = Math.max(1, yield.cooldownTicks() / Math.max(1, charges));
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
        if (!effects.isEmpty()) {
            // One bottle, every effect the horse's matching yields asked for.
            produced.set(DataComponents.POTION_CONTENTS,
                    new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty()));
        }
        if (!player.addItem(produced)) {
            player.drop(produced, false);
        }
        horse.setData(ModAttachments.HORSE_COOLDOWNS.get(), cooldowns.stamp(key, now));
    }

    /**
     * The else branch: a stallion kick, a foal's "nothing to give". A kick also
     * rears the horse ({@code makeMad}, which plays the angry sound itself), so
     * the reaction is something you see and not only a line in chat.
     */
    private static void applyDenial(Horse horse, Player player, GeneAbility.Yield yield) {
        if (yield.deniedDamage() > 0 && player.level() instanceof ServerLevel level) {
            player.hurtServer(level, horse.damageSources().mobAttack(horse), (float) yield.deniedDamage());
            if (horse.isStanding()) {
                horse.level().playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                        SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0F, 1.0F);
            } else {
                horse.makeMad();
            }
        }
        if (!yield.deniedMessage().isEmpty()) {
            player.sendSystemMessage(Component.translatable(yield.deniedMessage()));
        }
    }
}
