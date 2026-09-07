package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The translator for the <b>diet</b> locus: it decides what a special-diet
 * horse will accept from a player's hand and what that does for it.
 *
 * <p><b>It only ever runs for a horse whose diet is not
 * {@link Diet#NORMAL}.</b> The overwhelming majority of horses are ordinary,
 * and for them this handler returns immediately and vanilla feeding is
 * untouched - which is also why the mod's existing gated healing, bond and herd
 * behaviour needed no changes.
 *
 * <h4>What it does with an interaction</h4>
 * <ol>
 *   <li><b>Not a feed attempt</b> (a saddle, a lead, an empty hand) - left
 *       alone entirely. A special diet must not stop a horse being ridden.</li>
 *   <li><b>A vanilla love item on a tamed adult</b> - left alone. The locus
 *       governs healing, not breeding; a horse that could never be bred would
 *       be a dead end rather than a curiosity. This is the one deliberate leak
 *       (a golden carrot heals a little on the way past).</li>
 *   <li><b>Something the horse eats</b> - it heals by
 *       {@link Diet#healPoints()}, or completely if {@link Diet#healsFully()},
 *       the item is consumed (leaving a bucket or a bottle where there was
 *       one), the horse gets its bond, and the interaction is cancelled so
 *       vanilla does not also feed it.</li>
 *   <li><b>Anything else edible</b> - refused: a puff of smoke, no healing, and
 *       cancelled, because the whole point is that an apple does nothing for a
 *       horse that eats only lava.</li>
 * </ol>
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources; see
 * {@code wiki/verification.html}.
 */
@EventBusSubscriber
public final class HorseDietHandler {

    private HorseDietHandler() {
    }

    /** Bond for feeding a horse the one thing it wants. Twice the ordinary feed. */
    private static final int FEED_BOND = 4;

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !DietFoods.isFeedAttempt(held)) {
            return;     // a saddle, a lead, a hand - none of our business
        }

        HorseDiet diet = dietOf(horse);
        if (!diet.isSpecial()) {
            return;     // an ordinary horse: vanilla decides, as it always has
        }
        if (DietFoods.isVanillaLoveItem(held) && horse.isTamed() && !horse.isBaby()) {
            return;     // breeding is not this locus's business - see the class note
        }

        boolean accepted = DietFoods.accepts(diet, held);
        if (!(horse.level() instanceof ServerLevel level)) {
            // Client side still has to cancel, or the hand swings and the
            // player is told a different story from the server's.
            consume(event);
            return;
        }

        if (!accepted) {
            refuse(level, horse, event.getEntity(), diet);
            consume(event);
            return;
        }

        feed(level, horse, event.getEntity(), held, diet);
        consume(event);
    }

    // ------------------------------------------------------------------

    private static void feed(ServerLevel level, Horse horse, Player player,
                             ItemStack held, HorseDiet diet) {
        if (diet.diet().healsFully()) {
            horse.setHealth(horse.getMaxHealth());
        } else {
            horse.heal((float) diet.diet().healPoints());
        }

        Item remainder = DietFoods.remainderOf(held);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (remainder != null) {
                giveOrDrop(player, new ItemStack(remainder));
            }
        }

        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HEART,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.8, horse.getZ(),
                6, 0.4, 0.3, 0.4, 0.0);

        if (horse.isTamed() && horse.getOwner() == player) {
            HorseCareHandler.awardBondFor(horse, FEED_BOND);
        }
    }

    /**
     * The horse turns its head away. The message names what it actually wants -
     * a diet nobody can see is a guessing game, and the locus is meant to be a
     * puzzle with a solution, not a mystery.
     */
    private static void refuse(ServerLevel level, Horse horse, Player player, HorseDiet diet) {
        level.sendParticles(ParticleTypes.SMOKE,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.8, horse.getZ(),
                6, 0.3, 0.2, 0.3, 0.01);
        Item wanted = DietFoods.wantedBy(diet);
        Component what = wanted != null
                ? Component.translatable(wanted.getDescriptionId())
                : Component.literal(diet.diet().label().toLowerCase());
        player.sendSystemMessage(
                Component.literal("The horse turns its head away. It only eats ")
                        .append(what).append(Component.literal(".")));
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /**
     * <b>Can this horse regenerate at all?</b> False for a
     * {@link Diet#NOTHING} horse - a dhampir - and that is the whole reason the
     * value exists: "cannot be fed" has to mean the gated regen in
     * {@link HorseCareHandler} too, or a horse that nothing in the world can
     * feed would still quietly heal itself standing beside a hay bale.
     *
     * <p>Stated once, here, in terms of the diet rather than in terms of the
     * gene: any future gene that claims {@code NOTHING} inherits it.
     */
    public static boolean canRegenerate(Horse horse) {
        return dietOf(horse).diet() != Diet.NOTHING;
    }

    /**
     * This horse's diet, or {@link HorseDiet#NORMAL} for anything without a
     * real record - an unconverted vanilla horse, or one whose codes do not
     * parse. Same defensive shape as {@link GeneYieldHandler}.
     */
    public static HorseDiet dietOf(Horse horse) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return HorseDiet.NORMAL;
        }
        HorseRecord record = HorseRecords.of(horse);
        try {
            return HorseDiet.resolve(Genotype.parse(record.geneticCode()),
                    Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException bad) {
            return HorseDiet.NORMAL;
        }
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
