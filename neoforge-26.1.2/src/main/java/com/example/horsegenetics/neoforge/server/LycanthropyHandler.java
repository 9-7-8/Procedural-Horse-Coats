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
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>The werewolf gene, as behaviour.</b> A horse homozygous for one of the
 * LYCAN locus's shapes stops being a horse at nightfall: the {@code Horse}
 * entity is discarded and a <b>real</b> wolf, cat, chicken or panda is put in
 * its place, carrying the whole horse in an attachment ({@link LycanShift})
 * until dawn puts it back.
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
 * {@code wiki/gene-lycan.html#verification}.
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
     * way means the Nether and the End are permanent <i>day</i> for this gene.
     * That is the right answer - a place with no nightfall should not hold a
     * horse in wolf shape forever - and it is why the negation is not the same
     * thing. The horse dimension was on that list until it was given real nights
     * ({@code debug_pens.json}'s timelines); its shifters change like anyone's.
     *
     * <p>A shifted animal standing on the ground has its spot kept in
     * {@link #LAST_GROUND}, which is where {@link #revert} sets the horse down
     * when dawn finds the animal over nothing (gap 243).
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
        if (animal.onGround()) {
            LAST_GROUND.put(animal, animal.position());
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

        if (!isWearable(type, animal)) {
            // The second half of the lycan blacklist. The allele set is already
            // MobRoster.ground() and cannot name a fish, a bat or a monster -
            // but that list is hand-tagged today and derived from the live
            // registry tomorrow, so the body itself is checked here, where the
            // game will actually answer the question. A horse is never shut
            // inside something that drowns in air, flies out of its owner's
            // reach, or is hunted on sight.
            //
            // This supersedes the owner's 2026-09-13 call that a were-fish
            // "should absolutely still shift and then just die" - the fish
            // alleles do not exist any more (owner, 2026-09-24).
            if (WARNED.add(form.mob())) {
                HorseGenetics.LOGGER.warn("[lycan] '{}' swims, flies or is hostile in this build - "
                        + "{} will not shift", form.mob(), record.displayName());
            }
            animal.discard();
            return;
        }

        // IT SHIFTS WHEREVER IT IS STANDING. What is not guarded is the rest of
        // the night: a were-animal that dies takes its horse with it, properly,
        // with a death, drops and a line in the log - see onWereAnimalDeath.
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
        if (ServerConfig.debugTools()) {
            String print = roundTripPrint(horse);
            DUSK_PRINTS.put(horse.getUUID(), print);
            ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " shifted into a " + form.mob() + " at dusk | "
                    + print + String.format(", hp %.1f/%.1f", horse.getHealth(), horse.getMaxHealth()));
        }
    }

    /**
     * <b>Is this a body a horse can be found in again?</b> The runtime half of
     * the lycan blacklist, asked of the animal the registry actually handed us
     * rather than of the id we asked for.
     *
     * <p>Three things disqualify a body, and all three are the same complaint -
     * the owner cannot get their horse back. It <b>swims</b>, so the horse
     * suffocates the moment it shifts in a field; it <b>flies</b>, so it is over
     * the treeline before dawn and its owner never sees it again; or it is
     * <b>hostile</b>, so everything in the world attacks it and it attacks its
     * owner. Each is asked the broadest way the game offers, exactly as
     * {@link MobGroups} does, so that a modded mob answers honestly:
     * {@link MobCategory} for the spawn-time classification, {@link Enemy} and
     * {@link FlyingAnimal} for the interfaces mods implement, and the navigation
     * and move control for the ones that do neither but still take off.
     *
     * <p>API note, unverified in game: {@code AMBIENT} is here because the bat -
     * vanilla's one ambient mob - flies without a {@link FlyingPathNavigation}
     * or a {@link FlyingAnimal} to show for it, so the category is the only
     * thing that catches it. A modded ground-dwelling ambient mob would be
     * excluded with it, which is the safe direction to be wrong in.
     */
    private static boolean isWearable(EntityType<?> type, Mob animal) {
        MobCategory category = type.getCategory();
        boolean swims = category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AXOLOTLS;
        boolean flies = category == MobCategory.AMBIENT
                || animal instanceof FlyingAnimal
                || animal.getNavigation() instanceof FlyingPathNavigation
                || animal.getMoveControl() instanceof FlyingMoveControl;
        return !swims && !flies && !MobGroups.isHostile(animal);
    }

    /** Debug only: each shifted horse's {@link #roundTripPrint} at dusk, for the dawn and death lines to compare. */
    private static final java.util.Map<java.util.UUID, String> DUSK_PRINTS = new java.util.HashMap<>();

    /**
     * What must come through a night as an animal unchanged - identity, record, genome, epigenome, bond,
     * age class, armour - and deliberately not health, which the night is allowed to take.
     */
    private static String roundTripPrint(Horse horse) {
        HorseRecord r = HorseRecords.of(horse);
        return "uuid " + horse.getUUID() + ", " + r.displayName() + ", gen " + r.generation()
                + ", dam " + r.motherId().map(u -> u.toString().substring(0, 8)).orElse("-")
                + ", sire " + r.fatherId().map(u -> u.toString().substring(0, 8)).orElse("-")
                + ", record #" + Integer.toHexString(r.hashCode())
                + ", genome #" + Integer.toHexString(r.geneticCode().hashCode())
                + ", epigenome #" + Integer.toHexString(r.epigenomeCode().hashCode())
                + ", bond " + horse.getData(ModAttachments.HORSE_CARE.get()).bond()
                + ", baby " + horse.isBaby() + ", tamed " + horse.isTamed()
                + ", armour " + horse.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY).getItem();
    }

    /** " | round trip SAME", or what it was at dusk when it is not. */
    private static String roundTrip(Horse horse) {
        String dusk = DUSK_PRINTS.remove(horse.getUUID());
        String now = roundTripPrint(horse);
        return dusk == null ? " | round trip: no dusk print (loaded since)"
                : dusk.equals(now) ? " | round trip SAME" : " | round trip CHANGED - at dusk it was: " + dusk;
    }

    // ------------------------------------------------------------------
    // Dawn
    // ------------------------------------------------------------------

    /**
     * Put the horse back where the animal is standing, with the night's damage carried over - or,
     * when there is nothing under the animal, where it last stood on solid ground.
     *
     * <p>LAND ON SOLID GROUND (owner, 2026-09-15). This was written for the flying forms - two bat
     * lycans came back 49 blocks below the horse dimension's floor and outside its wall, a parrot
     * lycan the same way, and all three fell out of the world (gap 243). Those forms were taken off
     * the locus on 2026-09-24 and nothing on it flies any more, but the guard stays: a walking
     * animal still reaches ledges, boats, minecarts and a chunk whose floor was mined out under it
     * overnight. A revert with no solid block or water within {@link #SAFE_DROP} blocks below the
     * animal sets the horse down at the last spot the animal stood on the ground
     * ({@link #LAST_GROUND}), or, when that is not known because the animal was loaded since, where
     * the horse changed at dusk. The night still happens; only the drop into nothing does not. The
     * death path is untouched: an animal that dies, dies where it is.
     */
    private static void revert(Mob animal, ServerLevel level, LycanShift shift) {
        Horse horse = restore(animal, level, shift, true);
        LAST_GROUND.remove(animal);
        if (horse == null) {
            return;
        }
        double healthLeft = animal.getHealth() / Math.max(1.0F, animal.getMaxHealth());
        horse.setHealth((float) Math.max(1.0, healthLeft * horse.getMaxHealth()));
        animal.setData(ModAttachments.LYCAN_SHIFT.get(), LycanShift.NONE);
        animal.ejectPassengers();
        animal.discard();
        level.addFreshEntity(horse);
        puff(level, horse);
        if (ServerConfig.debugTools()) {
            ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " back from a " + shift.mob() + " at dawn | "
                    + roundTripPrint(horse) + String.format(", hp %.1f/%.1f", horse.getHealth(), horse.getMaxHealth())
                    + roundTrip(horse));
        }
    }

    /**
     * Build the horse back out of the tag the animal is carrying, standing
     * where the animal is standing. Health is <b>not</b> set here - dawn carries
     * the night's damage across proportionally, and a death does not care.
     */
    @Nullable
    private static Horse restore(Mob animal, ServerLevel level, LycanShift shift) {
        return restore(animal, level, shift, false);
    }

    /** How far a horse may drop onto ground without harm: vanilla fall damage starts past three. */
    private static final int SAFE_DROP = 3;

    /**
     * Where each shifted animal last stood on solid ground, for {@link #revert}. Held in memory,
     * weakly, so a discarded animal leaves nothing behind; after a reload the fallback is the
     * position saved in the horse's own tag, which is where it changed at dusk.
     */
    private static final java.util.Map<Mob, net.minecraft.world.phys.Vec3> LAST_GROUND = new java.util.WeakHashMap<>();

    /** Solid ground or water within {@link #SAFE_DROP} blocks under the animal, above the world's floor. */
    private static boolean groundBelow(ServerLevel level, Mob animal) {
        net.minecraft.core.BlockPos at = animal.blockPosition();
        for (int dy = 0; dy <= SAFE_DROP; dy++) {
            net.minecraft.core.BlockPos p = at.below(dy);
            if (p.getY() < level.getMinY()) {
                return false;
            }
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p);
            if (!st.getCollisionShape(level, p).isEmpty() || !st.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Horse restore(Mob animal, ServerLevel level, LycanShift shift, boolean landSafely) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.LOAD);
        if (horse == null) {
            return null;
        }
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(animal.problemPath(), HorseGenetics.LOGGER)) {
            horse.load(TagValueInput.create(reporter, level.registryAccess(), shift.horse()));
        } catch (RuntimeException bad) {
            HorseGenetics.LOGGER.error("[lycan] could not restore the horse inside a {} - it stays an animal",
                    shift.mob(), bad);
            return null;
        }
        // The tag's position is where it stood at dusk; the animal has moved
        // since, and where it is now is where the horse wakes up - unless that
        // is over nothing, which is revert's case (gap 243).
        net.minecraft.world.phys.Vec3 dusk = horse.position();
        if (landSafely && !groundBelow(level, animal)) {
            net.minecraft.world.phys.Vec3 ground = LAST_GROUND.get(animal);
            net.minecraft.world.phys.Vec3 to = ground != null ? ground : dusk;
            horse.snapTo(to.x, to.y, to.z, animal.getYRot(), animal.getXRot());
            ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " came back at dawn with nothing under its "
                    + shift.mob() + " at " + animal.blockPosition().toShortString() + " - set down at "
                    + net.minecraft.core.BlockPos.containing(to).toShortString()
                    + (ground != null ? " (where it last stood on the ground)" : " (where it changed at dusk)"));
            clearOfBlocks(horse, level, shift);
            return horse;
        }
        horse.snapTo(animal.getX(), animal.getY(), animal.getZ(), animal.getYRot(), animal.getXRot());
        clearOfBlocks(horse, level, shift);
        return horse;
    }

    /** How far, in blocks, a restored horse may be moved to get it out of a wall. */
    private static final int CLEAR_REACH = 3;

    /**
     * <b>A horse is bigger than most of its animal forms.</b> A wolf, a fox or a cat fits against a fence or under a
     * roof where a horse's box does not, and restoring the horse where the animal stood put it inside the blocks: the
     * yard's LYCAN WOLF foals suffocated ten seconds after dawn, and their sire took 23 wall hits (2026-09-15 run). So
     * a horse that would collide is moved to the nearest spot within {@link #CLEAR_REACH} blocks where its whole box is
     * clear, searching outward in shells and upward before down. If none is found it is left where it is, as before.
     */
    private static void clearOfBlocks(Horse horse, ServerLevel level, LycanShift shift) {
        net.minecraft.world.phys.AABB box = horse.getBoundingBox();
        if (level.noCollision(horse, box)) {
            return;
        }
        for (int r = 1; r <= CLEAR_REACH; r++) {
            for (int dy = 0; dy <= r; dy = dy <= 0 ? 1 - dy : -dy) {
                if (dy < -r) {
                    break;
                }
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dz)), Math.abs(dy)) != r) {
                            continue;   // only the shell at this distance; the inside was tried already
                        }
                        if (level.noCollision(horse, box.move(dx, dy, dz))) {
                            horse.snapTo(horse.getX() + dx, horse.getY() + dy, horse.getZ() + dz,
                                    horse.getYRot(), horse.getXRot());
                            ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " came back from a "
                                    + shift.mob() + " inside blocks - moved " + dx + ", " + dy + ", " + dz
                                    + " to " + horse.blockPosition().toShortString());
                            return;
                        }
                    }
                }
            }
        }
        ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " came back from a " + shift.mob()
                + " inside blocks and no clear spot within " + CLEAR_REACH + " - left where it was");
    }

    /**
     * <b>A were-animal dying IS the horse dying.</b>
     *
     * <p>Owner, 2026-09-13: <i>"We need a horse dying of lycanthropy to be
     * handled just like a horse dying any other way &hellip; A were-animal
     * dying in its animal form is a death, same as any other."</i>
     *
     * <p>Before this there was no death handling on the animal <em>at all</em>,
     * and the consequence was not a harsh gene, it was a disappearance: {@link
     * #shift} calls {@code horse.discard()} and hangs the horse's whole saved
     * state on the animal, so when the animal died the record, the pedigree and
     * the epigenome went with it - no body, no drops, no line in any log, and
     * nothing to tell a player their horse had died rather than vanished. Two
     * horses went that way as were-salmon on dry land the day this was written.
     *
     * <h2>Restore, then kill, rather than a second death path</h2>
     * The horse is rebuilt out of the tag where the animal fell and then killed
     * with <b>the same damage source</b>. That is deliberate and is the whole
     * point: it means the death runs through every hook a horse death already
     * has - {@code GeneDeathHandler}'s {@code OnDeath} effects and item drops,
     * {@code ActionTrace}'s <i>horse died</i> line with the real cause, the
     * ancestry database, vanilla's own drops - rather than through a parallel
     * implementation that would drift out of step with them. "Handled just like
     * a horse dying any other way" is most cheaply achieved by <em>actually
     * being</em> a horse dying.
     *
     * <p>The fallback kill exists because the restored horse may be immune to
     * what killed the animal (a fire-immune horse inside a burning were-cow),
     * and a horse that shrugs off the blow that killed its own body would be a
     * resurrection rather than a death.
     */
    @SubscribeEvent
    static void onWereAnimalDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob animal) || animal instanceof Horse) {
            return;
        }
        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }
        LycanShift shift = animal.getData(ModAttachments.LYCAN_SHIFT.get());
        if (!shift.active()) {
            return;
        }
        Horse horse = restore(animal, level, shift);
        if (horse == null) {
            return;     // the tag would not load; the animal dies as an animal
        }
        animal.setData(ModAttachments.LYCAN_SHIFT.get(), LycanShift.NONE);
        level.addFreshEntity(horse);
        ActionTrace.log("lycan", ActionTrace.describeShort(horse) + " died as a " + shift.mob()
                + " (" + event.getSource().getMsgId() + ") - the horse dies with it"
                + (ServerConfig.debugTools() ? roundTrip(horse) : ""));
        horse.hurtServer(level, event.getSource(), Float.MAX_VALUE);
        if (horse.isAlive()) {
            horse.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        }
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
     * Rideable forms are not rideable. Several of the forms are
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
