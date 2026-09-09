package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The translator for the two <b>night</b> loci - {@code magic_night_temper},
 * which decides what the horse becomes after dark, and
 * {@code magic_night_watch}, which decides what it does about you.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources.
 *
 * <h2>The supersede rule lives here, and could not live anywhere else</h2>
 * The temper locus beats the watch locus <b>whenever it actually has something
 * to act on</b>. That is a question about which mobs are standing nearby, so
 * {@code common/} cannot answer it - the genes describe two behaviours and this
 * class decides, every scan, which of them is the horse's business this second.
 * When the temper gene has a target the watch ability is withheld from
 * {@link NightWatchGoal}, which then reports {@code canUse() == false} and
 * stands down cleanly rather than fighting it for the navigation.
 *
 * <h2>Night is checked here too</h2>
 * Both verbs are night-only by definition rather than by condition, so the
 * check is one {@code isBrightOutside} in {@link #scan} instead of a
 * {@code when} clause repeated across fourteen alleles.
 */
@EventBusSubscriber
public final class NightBehaviourHandler {

    private NightBehaviourHandler() {}

    /** Goal priority for the watch. Below the aggro melee, above wandering. */
    private static final int WATCH_GOAL_PRIORITY = 4;

    /** How often the temper locus looks around, in ticks. */
    private static final int SCAN_INTERVAL = 20;

    /** How fast a fleeing horse goes. Faster than a walk - it means it. */
    private static final double FLEE_SPEED = 1.5;

    /** How far a flee path runs before it is re-picked. */
    private static final double FLEE_DISTANCE = 12.0;

    /** The watch ability each horse should currently obey, if any. */
    private static final Map<UUID, GeneAbility.NightWatch> WATCHING = new ConcurrentHashMap<>();

    /** Horses whose footfalls are currently suppressed - read by the sound hook. */
    private static final Map<UUID, Boolean> SILENT = new ConcurrentHashMap<>();

    /**
     * The watch ability {@code horse} should be obeying right now, or
     * {@code null}. Read by {@link NightWatchGoal}; written by {@link #scan}.
     */
    static GeneAbility.NightWatch activeWatch(Horse horse) {
        return WATCHING.get(horse.getUUID());
    }

    /**
     * Is this horse currently walking silently? Read by the client-facing sound
     * suppression; a horse that is watching you is not announcing itself.
     */
    public static boolean isSilent(Horse horse) {
        return Boolean.TRUE.equals(SILENT.get(horse.getUUID()));
    }

    @SubscribeEvent
    static void addWatchGoal(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (var w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof NightWatchGoal) {
                return; // already wired
            }
        }
        // Added to every horse. The goal itself is inert unless the handler has
        // put an ability in WATCHING for it, which is cheaper than adding and
        // removing a goal as the sun moves.
        horse.goalSelector.addGoal(WATCH_GOAL_PRIORITY, new NightWatchGoal(horse));
    }

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        Level level = horse.level();
        if (level.isClientSide() || !horse.isAlive()) {
            return;
        }
        if (horse.tickCount % SCAN_INTERVAL != 0) {
            return;
        }
        scan(horse, level);
    }

    /**
     * One scan: work out what this horse is doing tonight, and record it.
     *
     * <p>Order matters and is the supersede rule. The temper locus is resolved
     * first and, if it finds a target, the watch ability is withheld for the
     * next interval - so the goal stands down and the horse hunts or runs
     * instead of trying to do both.
     */
    private static void scan(Horse horse, Level level) {
        UUID id = horse.getUUID();
        if (level.isBrightOutside()) {
            // Daytime: both loci are silent, and so is the horse's own gate on
            // being quiet. Clearing rather than leaving stale entries is what
            // makes dawn actually end the behaviour.
            WATCHING.remove(id);
            SILENT.remove(id);
            return;
        }

        List<HorseAbilities.Active> abilities = abilitiesOf(horse);
        GeneAbility.NightTemper temper = null;
        GeneAbility.NightWatch watch = null;
        for (HorseAbilities.Active active : abilities) {
            if (active.ability() instanceof GeneAbility.NightTemper t) {
                temper = t;
            } else if (active.ability() instanceof GeneAbility.NightWatch w) {
                watch = w;
            }
        }

        boolean temperActing = temper != null && applyTemper(temper, horse, level);
        // THE SUPERSEDE RULE: the watch only runs when the temper found nothing.
        if (temperActing || watch == null) {
            WATCHING.remove(id);
        } else {
            WATCHING.put(id, watch);
        }
        // The quiet is the WATCH locus's, and it holds even while the temper is
        // talking over it - the horse still has the allele, and a stalker that
        // became audible the moment a cow wandered past would be a tell.
        if (watch != null && watch.silentSteps()) {
            SILENT.put(id, Boolean.TRUE);
        } else {
            SILENT.remove(id);
        }
    }

    /**
     * Hunt or run. Returns whether it found anything - which is what decides
     * whether the watch locus gets a turn.
     */
    private static boolean applyTemper(GeneAbility.NightTemper t, Horse horse, Level level) {
        AABB box = horse.getBoundingBox().inflate(t.radius());
        double reachSqr = t.radius() * t.radius();
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        int considered = 0;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
                LivingEntity::isAlive)) {
            if (considered >= t.maxTargets()) {
                break;  // every radius effect here states a cap
            }
            if (candidate == horse || !matches(t.towards(), candidate)) {
                continue;
            }
            // The scan box is a box and the reach is a sphere.
            double d = candidate.distanceToSqr(horse);
            if (d > reachSqr) {
                continue;
            }
            considered++;
            if (d < best) {
                best = d;
                nearest = candidate;
            }
        }
        if (nearest == null) {
            return false;
        }

        if ("aggressive".equals(t.mood())) {
            // The melee goal and the attack damage attribute are already on
            // every horse - HorseAggroHandler put them there so a wild one
            // could kick back - so setting a target is the whole of it.
            if (horse.getTarget() != nearest) {
                horse.setTarget(nearest);
            }
            return true;
        }

        // Flee: path to a point directly away, and drop any target it had.
        horse.setTarget(null);
        Vec3 away = horse.position().subtract(nearest.position());
        if (away.lengthSqr() < 1.0e-4) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 to = horse.position().add(away.normalize().scale(FLEE_DISTANCE));
        horse.getNavigation().moveTo(to.x, to.y, to.z, FLEE_SPEED);
        return true;
    }

    /** Does this creature fall into the group the allele names? */
    private static boolean matches(String towards, LivingEntity candidate) {
        return switch (towards) {
            case "players" -> candidate instanceof Player;
            // A horse is an Animal, so "passive" would otherwise include every
            // other horse in the herd AND the hostile mobs that extend Animal
            // (there are none, but Enemy is the honest test either way).
            case "passive" -> candidate instanceof Animal && !(candidate instanceof Enemy);
            case "hostile" -> candidate instanceof Enemy;
            case "all" -> candidate instanceof Player
                    || (candidate instanceof Mob && !(candidate instanceof Horse));
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // Going quiet
    // ------------------------------------------------------------------

    /**
     * How close a positional sound must be to a silent horse to be taken as
     * that horse's footfall, in blocks. Squared at the call site.
     *
     * <p>It exists because a step sound may arrive as a <i>position</i> rather
     * than as an entity, and a position carries no clue whose it is. Small
     * enough that two horses standing together cannot silence each other's
     * hooves by accident.
     */
    private static final double STEP_MATCH_RADIUS = 0.75;

    /**
     * Cancel a silent horse's footfall.
     *
     * <p><b>Both event shapes are handled deliberately.</b> A step sound is
     * played through {@code Entity.playStepSound}, and which of the two
     * {@link PlayLevelSoundEvent} forms that ends up firing is a detail of how
     * the entity happens to call {@code playSound} - which is exactly the kind
     * of thing that is true in one version and not the next. Listening for both
     * costs one extra method and cannot be wrong; listening for the wrong one
     * costs a gene that silently does nothing, which is the failure this
     * codebase keeps paying for.
     *
     * <p><b>Not verified in-game.</b>
     */
    @SubscribeEvent
    static void silenceStepAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getEntity() instanceof Horse horse && isSilent(horse) && isStep(event)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void silenceStepAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (!isStep(event) || SILENT.isEmpty()) {
            return;
        }
        Vec3 at = event.getPosition();
        AABB near = new AABB(at, at).inflate(STEP_MATCH_RADIUS);
        for (Horse horse : event.getLevel().getEntitiesOfClass(Horse.class, near)) {
            if (isSilent(horse)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * Is this a footfall rather than a whinny?
     *
     * <p>Matched on the sound's own id containing {@code "step"}, which covers
     * every block's step sound without naming any of them. A watcher is meant to
     * be silent on its feet, not mute - it can still be heard breathing, and
     * that is the point.
     */
    private static boolean isStep(PlayLevelSoundEvent event) {
        var sound = event.getSound();
        return sound != null && sound.unwrapKey()
                .map(key -> key.identifier().getPath().contains("step"))
                .orElse(false);
    }

    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            WATCHING.remove(horse.getUUID());
            SILENT.remove(horse.getUUID());
        }
    }

    private static List<HorseAbilities.Active> abilitiesOf(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return List.of();
        }
        try {
            return HorseAbilities.activeFor(Genotype.parse(record.geneticCode()),
                    Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
