package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.LycanGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.LycanShift;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>The werewolf gene, as behaviour.</b> A horse homozygous for one of the
 * LYCAN locus's thirty-seven shapes stops being a horse at nightfall: the
 * {@code Horse} entity is discarded and a <b>real</b> wolf, cat, chicken or
 * pufferfish is put in its place, carrying the whole horse in an attachment
 * ({@link LycanShift}) until dawn puts it back.
 *
 * <h2>Why a real entity and not a render layer</h2>
 * The roadmap scoped this "render-layer first, entity-swap second", and the
 * requirement is what overruled that: a shifted horse has to be interactable in
 * <b>all</b> the ways its animal is and no others - up to and including
 * <i>breeding with real members of that species</i>. A horse wearing a wolf
 * skin cannot be bred with a wolf, cannot be tamed with a bone, cannot be
 * sheared, cannot be bucketed, and every one of those would have to be
 * hand-written and would still be wrong for the next mob. Swapping in the real
 * entity gets the whole list for free, correctly, including for mobs added by
 * other mods' interactions with vanilla ones.
 *
 * <p>The "and only in ways" half of that requirement falls out of the same
 * decision: while it is a wolf there is no {@code Horse} in the world, so none
 * of this mod's own horse interactions - transfer papers, shearing, gene
 * carrots, the stall reader - can see it. The single exception that has to be
 * enforced by hand is <b>riding</b>: half a dozen of the forms are rideable
 * animals, and {@link #noRidingAShifter} cancels the mount.
 *
 * <h2>The three things a shifted animal does that an ordinary one does not</h2>
 * <ul>
 *   <li><b>It trails a cloud.</b> A faint puff of coloured dust round the body
 *       whenever it moves on the ground, in a colour drawn on the expressing
 *       allele copy - so it is heritable, and a line can be bred toward one.
 *       It is also the only way to tell a were-cow from a cow.</li>
 *   <li><b>It holds a grudge.</b> Hit it and it follows and strikes back until
 *       sunrise or until it loses sight of you - the wolf's temper, given to
 *       every form. {@link LycanTemperGoal} owns that.</li>
 *   <li><b>It cannot be ridden.</b></li>
 * </ul>
 *
 * <h2>Identity survives the night</h2>
 * The stored tag is the horse's complete {@code saveWithoutId}, which on
 * NeoForge includes its data attachments - so the record, name, pedigree, bond,
 * cooldowns and brand all come back, and so does the horse's {@code UUID}. A
 * shifted horse is the same horse, not a copy, and every system that keys on the
 * UUID (the ancestry database, the breeding roster, the stalls) is undisturbed
 * by the whole business.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources; the entity swap in
 * particular is the most invasive thing in the mod. See
 * {@code wiki/verification.html} and {@code wiki/gene-lycan.html}.
 */
@EventBusSubscriber
public final class LycanthropyHandler {

    private LycanthropyHandler() {
    }

    /** How often the sun is checked, in ticks. Staggered by entity id. */
    private static final int SUN_CHECK_INTERVAL = 40;

    /** Probability the walking cloud puffs on any given moving tick. */
    private static final float CLOUD_CHANCE = 0.35F;

    /** Particles per puff, and how far they spread relative to the animal's width. */
    private static final int CLOUD_COUNT = 3;
    private static final double CLOUD_SPREAD = 0.45;

    private static final int TEMPER_GOAL_PRIORITY = 2;

    /** Mob ids this build could not resolve - warned about once each. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    // ------------------------------------------------------------------
    // The clock
    // ------------------------------------------------------------------

    /**
     * Horses shift and animals revert on the same tick event, because they are
     * two halves of one question and the entity that answers it changes identity
     * halfway through.
     *
     * <p>{@link Level#isDarkOutside()} rather than {@code !isBrightOutside()}:
     * both are false in a dimension with a fixed time of day, so asking it this
     * way means the Nether, the End and the horse dimension are permanent
     * <i>day</i> for this gene. That is the right answer - a place with no
     * nightfall should not hold a horse in wolf shape forever - and it is why
     * the negation is not the same thing.
     */
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof Horse horse) {
            if ((horse.tickCount + horse.getId()) % SUN_CHECK_INTERVAL == 0 && level.isDarkOutside()) {
                shift(horse, level);
            }
            return;
        }
        if (!(entity instanceof Mob animal)) {
            return;
        }
        LycanShift shift = animal.getData(ModAttachments.LYCAN_SHIFT.get());
        if (!shift.active()) {
            return;
        }
        if ((animal.tickCount + animal.getId()) % SUN_CHECK_INTERVAL == 0 && !level.isDarkOutside()) {
            revert(animal, level, shift);
            return;
        }
        trailCloud(animal, level, shift.cloudColor());
    }

    // ------------------------------------------------------------------
    // Dusk
    // ------------------------------------------------------------------

    /**
     * Turn one horse into its animal. Everything the animal needs to look like
     * <i>this</i> horse rather than a fresh spawn is copied across by hand -
     * name, age, the fraction of its health it had left - and everything else
     * about the horse goes into the tag.
     *
     * <p>Passengers are thrown off first. A rider whose mount evaporates
     * underneath them is a dismount either way; doing it deliberately means it
     * happens before the entity is gone rather than as a side effect of it.
     */
    private static void shift(Horse horse, ServerLevel level) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return; // record not assigned yet - the spawn handler runs first
        }
        HorseRecord record = HorseRecords.of(horse);
        LycanGene.Form form;
        int cloud;
        try {
            Genotype genotype = Genotype.parse(record.geneticCode());
            form = Genes.LYCAN.formOf(genotype.pair(Genes.LYCAN));
            if (form == null) {
                return;
            }
            cloud = GeneEpigenetics.forGene(Genes.LYCAN, genotype, Epigenome.parse(record.epigenomeCode()))
                    .expressed().rgb("cloud");
        } catch (RuntimeException bad) {
            return;
        }

        EntityType<?> type = EntityType.byString(form.mob()).orElse(null);
        if (type == null || !(type.create(level, EntitySpawnReason.CONVERSION) instanceof Mob animal)) {
            // A mob id this build has never heard of, or one that is not a Mob.
            // The horse simply does not shift - a missing form is a gene that
            // does nothing, not a crash on somebody's world tick.
            if (WARNED.add(form.mob())) {
                HorseGenetics.LOGGER.warn("[lycan] no mob '{}' in this build - {} will not shift",
                        form.mob(), record.displayName());
            }
            return;
        }

        double healthLeft = horse.getHealth() / Math.max(1.0F, horse.getMaxHealth());
        animal.snapTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), horse.getXRot());
        animal.setBaby(horse.isBaby());
        animal.setCustomName(horse.getCustomName());
        animal.setCustomNameVisible(horse.isCustomNameVisible());
        // Persistent, or a were-chicken quietly despawns overnight and takes a
        // pedigreed horse with it.
        animal.setPersistenceRequired();
        animal.setHealth((float) Math.max(1.0, healthLeft * animal.getMaxHealth()));
        animal.setData(ModAttachments.LYCAN_SHIFT.get(), new LycanShift(form.mob(), cloud, save(horse)));
        addTemper(animal);

        horse.ejectPassengers();
        horse.discard();
        level.addFreshEntity(animal);
        puff(level, animal);
    }

    // ------------------------------------------------------------------
    // Dawn
    // ------------------------------------------------------------------

    /** Put the horse back where the animal is standing, with the night's damage carried over. */
    private static void revert(Mob animal, ServerLevel level, LycanShift shift) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.LOAD);
        if (horse == null) {
            return;
        }
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(animal.problemPath(), HorseGenetics.LOGGER)) {
            horse.load(TagValueInput.create(reporter, level.registryAccess(), shift.horse()));
        } catch (RuntimeException bad) {
            HorseGenetics.LOGGER.error("[lycan] could not restore the horse inside a {} - it stays an animal",
                    shift.mob(), bad);
            return;
        }
        // The tag's position is where it stood at dusk; the animal has walked
        // since, and where it walked to is where the horse wakes up.
        horse.snapTo(animal.getX(), animal.getY(), animal.getZ(), animal.getYRot(), animal.getXRot());
        double healthLeft = animal.getHealth() / Math.max(1.0F, animal.getMaxHealth());
        horse.setHealth((float) Math.max(1.0, healthLeft * horse.getMaxHealth()));

        animal.setData(ModAttachments.LYCAN_SHIFT.get(), LycanShift.NONE);
        animal.ejectPassengers();
        animal.discard();
        level.addFreshEntity(horse);
        puff(level, horse);
    }

    // ------------------------------------------------------------------
    // What a shifted animal does that an ordinary one does not
    // ------------------------------------------------------------------

    /**
     * The cloud, round the body rather than at the feet - "surrounded by", not
     * "trailing from". Only while it is actually moving under its own power on
     * the ground, so a sleeping were-cat is just a cat.
     */
    private static void trailCloud(Mob animal, ServerLevel level, int rgb) {
        if (!animal.onGround() || animal.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6) {
            return;
        }
        if (level.getRandom().nextFloat() > CLOUD_CHANCE) {
            return;
        }
        double spread = Math.max(0.2, animal.getBbWidth() * CLOUD_SPREAD);
        level.sendParticles(new DustParticleOptions(rgb, 1.0F),
                animal.getX(), animal.getY() + animal.getBbHeight() * 0.5, animal.getZ(),
                CLOUD_COUNT, spread, animal.getBbHeight() * 0.35, spread, 0.0);
    }

    /** The change itself, at both ends of the night. */
    private static void puff(ServerLevel level, Entity at) {
        level.sendParticles(ParticleTypes.POOF,
                at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(),
                12, 0.35, 0.35, 0.35, 0.02);
    }

    /**
     * Hit a shifted animal and it comes after you. The temper is the gene's, not
     * the animal's, so it is given to every form - a were-chicken really will
     * chase you across a field.
     *
     * <p>Deliberately narrower than {@link HorseAggroHandler}'s: no herd alert.
     * A wild horse's herd-mates come to its defence because they are a herd; a
     * were-sheep standing in a flock of real sheep has no herd, and asking the
     * real ones to help would be asking vanilla sheep to fight.
     */
    @SubscribeEvent
    static void onShifterHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob animal) || animal.level().isClientSide()) {
            return;
        }
        if (!animal.getData(ModAttachments.LYCAN_SHIFT.get()).active()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == animal) {
            return;
        }
        addTemper(animal);
        animal.setTarget(attacker);
        animal.setLastHurtByMob(attacker);
    }

    /**
     * Rideable forms are not rideable. Half a dozen of the thirty-seven are
     * animals a player can sit on, and the one thing a shifted horse must not be
     * is a mount - the point of the gene is the night you cannot ride home.
     *
     * <p>Only mounting is cancelled. Dismounting a shifter somebody was already
     * aboard (a horse that shifted under a rider, if the eject ever misses one)
     * has to stay possible.
     */
    @SubscribeEvent
    static void noRidingAShifter(EntityMountEvent event) {
        if (!event.isMounting()) {
            return;
        }
        if (event.getEntityBeingMounted() instanceof Mob animal
                && animal.getData(ModAttachments.LYCAN_SHIFT.get()).active()) {
            event.setCanceled(true);
        }
    }

    /**
     * Re-wire the temper after a reload. The attachment survives a restart; the
     * goal does not, because goals are built by the entity's constructor and
     * this one was added from outside it.
     */
    @SubscribeEvent
    static void rewireOnJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob animal)) {
            return;
        }
        if (animal.getData(ModAttachments.LYCAN_SHIFT.get()).active()) {
            addTemper(animal);
        }
    }

    private static void addTemper(Mob animal) {
        for (WrappedGoal w : animal.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof LycanTemperGoal) {
                return; // already wired
            }
        }
        animal.goalSelector.addGoal(TEMPER_GOAL_PRIORITY, new LycanTemperGoal(animal));
    }

    // ------------------------------------------------------------------

    /**
     * The whole horse as a tag. {@code saveWithoutId} rather than {@code save}
     * because the entity type is not in question - what comes out of this only
     * ever goes back into a {@code Horse} - and because on NeoForge it is the
     * call that carries the data attachments, which is the half that matters.
     */
    private static CompoundTag save(Horse horse) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(horse.problemPath(), HorseGenetics.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, horse.registryAccess());
            horse.saveWithoutId(out);
            return out.buildResult();
        }
    }
}
