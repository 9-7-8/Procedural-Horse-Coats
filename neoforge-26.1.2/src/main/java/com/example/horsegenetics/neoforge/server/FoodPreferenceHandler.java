package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>The favourite food, and the one place it beats the diet.</b>
 *
 * <h2>Why this handler exists at all</h2>
 * {@code FoodPreferenceGene} grants no effect verb - what it does happens on an
 * interaction rather than on a tick - so the whole of its behaviour is here.
 *
 * <p>And it must run <b>before</b> {@link HorseDietHandler}, which is the entire
 * point of the locus. The preference is drawn independently of the diet, so a
 * meat-eating horse can have an inexplicable weakness for carrots, and the
 * settled answer is that <b>the preference wins</b>. The alternative - the diet
 * refuses and the preference silently never fires - would let a horse carry a
 * gene that does literally nothing, which is the failure mode this project
 * already tracks by name.
 *
 * <p>So it subscribes at {@link EventPriority#HIGH} and cancels the event when
 * it handles a favourite. Two genes writing one answer is exactly the kind of
 * silent drift the contracts list warns about, and the ordering is the contract.
 *
 * <h2>The bond goes through the daily cap</h2>
 * Like every other bond source, via {@link HorseCareHandler#awardBondFor}. The
 * favourite is worth a lot <i>per feeding</i> and nothing at all once the day's
 * ceiling is reached, which is what stops "find the favourite" from replacing
 * every other way of earning a horse's trust.
 */
@EventBusSubscriber
public final class FoodPreferenceHandler {

    private FoodPreferenceHandler() {
    }

    /** Bond for the favourite. Generous per feeding, and still inside the daily cap. */
    private static final int FAVOURITE_BOND = 4;

    /** The buff, and how long it lasts. Modest - the bond is the real reward. */
    private static final int BUFF_TICKS = 1200;

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || horse.isBaby()) {
            return;
        }
        String favourite = favouriteOf(horse);
        if (favourite == null) {
            return;
        }
        if (!BuiltInRegistries.ITEM.getKey(held.getItem()).toString().equals(favourite)) {
            return;     // not this horse's weakness; the diet locus can have it
        }

        if (horse.level() instanceof ServerLevel level) {
            if (!event.getEntity().getAbilities().instabuild) {
                held.shrink(1);
            }
            // Measured BEFORE healing, or a horse on full health looks like one
            // that was just healed to full.
            boolean needed = horse.getHealth() < horse.getMaxHealth();
            horse.heal(2.0F);
            horse.addEffect(new MobEffectInstance(MobEffects.SPEED, BUFF_TICKS, 0,
                    true, false, false));
            boolean bonded = HorseCareHandler.awardBondFor(horse, FAVOURITE_BOND);
            level.sendParticles(ParticleTypes.HEART,
                    horse.getX(), horse.getY() + horse.getBbHeight(), horse.getZ(),
                    5, 0.4, 0.3, 0.4, 0.0);
            level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                    SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (!needed && !bonded) {
                FeedFeedback.ateButDidNotNeedIt(event.getEntity(), horse);
            }
        }

        // Cancel, so the diet handler never sees it. This is the override.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * The item this horse would rather have than anything, or {@code null}.
     *
     * <p>Reads the genotype off the record rather than an attachment, so it is
     * correct for a horse the moment it is founded and needs no sync.
     */
    static String favouriteOf(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return null;
        }
        try {
            Genotype genotype = Genotype.parse(record.geneticCode());
            AllelePair pair = genotype.pair(Genes.FOOD_PREFERENCE);
            return Genes.FOOD_PREFERENCE.favouriteOf(pair);
        } catch (RuntimeException e) {
            return null;    // an unparseable code is not this handler's problem
        }
    }
}
