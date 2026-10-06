package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>The bridge between a live animal and the draught model.</b>
 *
 * <p>{@link CartDraft} is pure and lives in {@code common/}: it takes two
 * doubles and a load and knows nothing about entities. This class is the other
 * half - it finds those two doubles on a real animal, keeps the per-horse
 * haulage tally, and is the <b>public surface other mods integrate against</b>.
 *
 * <h2>Animals that are not our horses</h2>
 * Every method here takes any {@link LivingEntity}, because a cart does not
 * care what is hitched to it and a pack of other mods' mounts may be. Anything
 * without one of this mod's genomes is treated as an ordinary horse -
 * {@link HorseTraits#BASE_PULL} - rather than as a weakling. A donkey is not
 * feeble for want of a genotype, and a mod that adds a mount should not have it
 * silently crippled by ours.
 *
 * @see CartDraft the model itself, and the reasoning behind its shape
 */
@net.neoforged.fml.common.EventBusSubscriber
public final class HorseDraft {

    private HorseDraft() {
    }

    /**
     * This animal's pulling ability on the 1-10 score, or
     * {@link HorseTraits#BASE_PULL} if it has no genome of ours.
     */
    public static double pullOf(final LivingEntity entity) {
        if (entity instanceof Horse horse) {
            final var record = HorseRecords.of(horse);
            if (record != null && record.hasGenome()) {
                return HorseRecords.traitsOf(horse).pull();
            }
        }
        return HorseTraits.BASE_PULL;
    }

    /**
     * This animal's <b>unhitched</b> movement speed - the attribute's base
     * value, deliberately not its current one.
     *
     * <p>A hitched horse already carries the draught modifier, so reading the
     * current value and feeding it back into the model would compound the
     * penalty every time anything asked.
     */
    public static double speedOf(final LivingEntity entity) {
        final AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return attr == null ? HorseTraits.BASE_SPEED : attr.getBaseValue();
    }

    /**
     * The fraction of its own speed this animal keeps while pulling that
     * vehicle <b>empty</b>: {@code 1.0} unhitched, and never zero.
     *
     * <p>Empty because a {@link CartKind} is a kind, not a cart - there is
     * nothing here to ask how full it is. A caller holding a real vehicle should
     * pass {@code AbstractDrawnEntity.currentLoad()} to
     * {@link CartDraft#retention} instead; see {@link CartDraft#loaded}.
     */
    public static double retention(final LivingEntity entity, final CartKind kind) {
        return CartDraft.retention(pullOf(entity), speedOf(entity), kind.load());
    }

    /**
     * <b>How hard this animal drives a machine</b>, as a multiple of an ordinary
     * horse: {@code 1.0} is the baseline, above it is better.
     *
     * <p>This is the number a mod that makes a horse turn something should
     * multiply its work rate by. It blends pulling ability with speed rather
     * than reading pull alone, so that breeding a draft horse does not mean
     * abandoning every other stat - see {@link CartDraft#workRate}.
     *
     * <p>Not clamped, and deliberately so: a caller knows its own machine's
     * sensible bounds better than this does.
     */
    public static double workRate(final LivingEntity entity) {
        return CartDraft.workRate(pullOf(entity), speedOf(entity));
    }

    /** Whether this animal's back takes a second rider. */
    public static boolean carriesTwoRiders(final LivingEntity entity) {
        return CartDraft.carriesTwoRiders(pullOf(entity));
    }

    // ------------------------------------------------------------------
    // The haulage tally
    // ------------------------------------------------------------------

    /**
     * Add to this animal's lifetime haulage, in metres. Server side; a no-op for
     * anything that is not one of this mod's horses.
     *
     * <p>Called once per cart tick with a fraction of a metre. <b>Batched</b>
     * since #202: the attachment is synced, and {@code setData} sends it to every
     * player tracking the horse, so writing it back on every call was a packet per
     * tick per hauling horse - for a number the screen shows in whole metres.
     * The fraction collects here and is written once it reaches
     * {@link #FLUSH_METRES}. A horse that unloads mid-haul can lose up to that much:
     * UNVERIFIED that the leave event below runs before the chunk's entities are saved -
     * read as after, so it is a best effort, and four metres is the bound either way.
     */
    public static void addHauled(final Entity entity, final double metres) {
        if (metres <= 0.0 || !(entity instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        double pending = PENDING.merge(horse.getUUID(), metres, Double::sum);
        if (pending >= FLUSH_METRES) {
            flush(horse);
        }
    }

    /** Metres collected before the tally is written - about a second of a trotting team. */
    private static final double FLUSH_METRES = 4.0;

    /** Hauled metres not yet written to the attachment, by horse. Server side only. */
    private static final java.util.Map<java.util.UUID, Double> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private static void flush(final Horse horse) {
        Double pending = PENDING.remove(horse.getUUID());
        if (pending != null && pending > 0.0) {
            horse.setData(ModAttachments.CART_METRES.get(), horse.getData(ModAttachments.CART_METRES.get()) + pending);
        }
    }

    /** Whatever is still collecting goes onto the horse as it leaves (#202) - see {@link #addHauled}. */
    @net.neoforged.bus.api.SubscribeEvent
    static void onEntityLeave(final net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Horse horse) {
            flush(horse);
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    static void onServerStopped(final net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        PENDING.clear();
    }

    /**
     * How far this horse has hauled a cart in its life, in metres. Zero for
     * anything that has never been hitched, and for anything that is not a
     * horse.
     */
    public static double hauled(final Entity entity) {
        if (!(entity instanceof Horse horse)) {
            return 0.0;
        }
        double written = horse.getData(ModAttachments.CART_METRES.get());
        Double pending = horse.level().isClientSide() ? null : PENDING.get(horse.getUUID());
        return pending == null ? written : written + pending;
    }
}
