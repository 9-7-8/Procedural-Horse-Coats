package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.world.entity.animal.equine.Horse;
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
 * The translator for the two <b>watch</b> loci - {@code magic_night_watch} and
 * {@code magic_day_watch}, which decide what the horse does <i>about you</i> as
 * opposed to what it does <i>to</i> you. One handler for both, because the
 * behaviour is the same whichever half of the day it is; the class keeps its
 * name because the night pair came first.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources.
 *
 * <h2>It used to run the temper loci too</h2>
 * {@code night_temper} and {@code day_temper} are gone: aggression is one locus
 * now ({@code horsegenetics.aggression}, and {@code horsegenetics.skittish} for
 * what a horse runs from), both of which use the general {@code temper} verb
 * with an ordinary {@code when} condition. So hunting and fleeing are
 * {@link GeneAbilityHandler}'s business, on the ordinary ability tick, and this
 * class is left with the half it was always really about.
 *
 * <h2>The supersede rule survived the move, and had to</h2>
 * The temper loci beat the watch loci <b>whenever they actually have something
 * to act on</b>. A horse that both stalks you and flees from you is not two
 * behaviours, it is a bug. That is a question about which mobs are standing
 * nearby, so {@code common/} cannot answer it - and now that the two halves live
 * in different handlers it cannot be answered by reading a gene either.
 *
 * <p>So {@link GeneAbilityHandler} <b>reports</b>: every time a {@code temper}
 * ability picks a target or runs from one it calls {@link #noteActing}, and this
 * class withholds the watch ability for as long as that says so. The alternative
 * - having the watch goal re-scan for targets itself - would run the same radius
 * query twice a second on every horse in the world to answer a question the
 * other handler had just answered.
 *
 * <h2>A day behaviour costs a cached lookup, not a parse</h2>
 * {@link #TIMED} keeps each horse's two resolved watch abilities against the
 * genetic code they came from, so a horse with neither costs a map lookup and a
 * string comparison.
 */
@EventBusSubscriber
public final class NightBehaviourHandler {

    private NightBehaviourHandler() {}

    /** Goal priority for the watch. Below the aggro melee, above wandering. */
    private static final int WATCH_GOAL_PRIORITY = 4;

    /** How often the watch loci re-decide, in ticks. */
    private static final int SCAN_INTERVAL = 20;

    /**
     * How long a report from {@link #noteActing} stands before it goes stale, in
     * ticks. Comfortably more than one {@link #SCAN_INTERVAL} and more than the
     * default temper beat, so an ability that is acting on every one of its own
     * beats never flickers back to "idle" between them - and short enough that a
     * horse whose temper stopped firing (its target died, the sun came up, the
     * gene stopped expressing) resumes watching within a second or so.
     */
    private static final int ACTING_TTL_TICKS = 60;

    /** The watch ability each horse should currently obey, if any. */
    private static final Map<UUID, GeneAbility.NightWatch> WATCHING = new ConcurrentHashMap<>();

    /** Horses whose footfalls are currently suppressed - read by the sound hook. */
    private static final Map<UUID, Boolean> SILENT = new ConcurrentHashMap<>();

    /** Horse UUID to the tick a temper ability last reported acting on something. */
    private static final Map<UUID, Integer> TEMPER_ACTING = new ConcurrentHashMap<>();

    /**
     * <b>The supersede rule's input.</b> Called by {@link GeneAbilityHandler}
     * whenever a {@code temper} ability has actually done something this beat -
     * acquired a target, or pathed away from one.
     *
     * <p>Only the <i>positive</i> case is reported. A temper that found nothing
     * says nothing rather than reporting false, because a horse may express more
     * than one temper ability (hunting monsters and fleeing people is two), they
     * are applied in sequence within a tick, and a later one reporting "I found
     * nothing" must not erase an earlier one's "I am hunting". The record simply
     * ages out - see {@link #ACTING_TTL_TICKS}.
     */
    static void noteActing(Horse horse) {
        TEMPER_ACTING.put(horse.getUUID(), horse.tickCount);
    }

    /** Is a temper ability currently talking over the watch loci? */
    private static boolean temperActing(Horse horse) {
        Integer at = TEMPER_ACTING.get(horse.getUUID());
        if (at == null) {
            return false;
        }
        if (horse.tickCount - at > ACTING_TTL_TICKS) {
            TEMPER_ACTING.remove(horse.getUUID());
            return false;
        }
        return true;
    }

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
     * One scan: work out what this horse is watching, and record it.
     *
     * <p>The temper loci get first refusal - if one of them reported acting
     * within the last {@link #ACTING_TTL_TICKS}, the watch ability is withheld,
     * the goal reports {@code canUse() == false} and stands down cleanly rather
     * than fighting the temper for the navigation.
     */
    private static void scan(Horse horse, Level level) {
        UUID id = horse.getUUID();
        Timed timed = timedOf(horse);
        boolean day = level.isBrightOutside();
        GeneAbility.NightWatch watch = day ? timed.dayWatch() : timed.nightWatch();
        if (watch == null) {
            // Nothing for this half of the day. Clearing rather than leaving a
            // stale entry is what makes dawn (or dusk) actually end the
            // behaviour.
            WATCHING.remove(id);
            SILENT.remove(id);
            return;
        }

        // THE SUPERSEDE RULE.
        if (temperActing(horse)) {
            WATCHING.remove(id);
        } else {
            WATCHING.put(id, watch);
        }
        // The quiet is the WATCH locus's, and it holds even while a temper is
        // talking over it - the horse still has the allele, and a stalker that
        // became audible the moment a cow wandered past would be a tell.
        if (watch.silentSteps()) {
            SILENT.put(id, Boolean.TRUE);
        } else {
            SILENT.remove(id);
        }
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
            TIMED.remove(horse.getUUID());
            TEMPER_ACTING.remove(horse.getUUID());
        }
    }

    /**
     * One horse's two watch behaviours, resolved, and the genetic code they were
     * resolved from. The day one is stored as a night record - they have the
     * same fields - so the rest of this class and {@link NightWatchGoal} never
     * need to know which half of the day it is.
     */
    private record Timed(String code, GeneAbility.NightWatch nightWatch,
                         GeneAbility.NightWatch dayWatch) {
        static final Timed NONE = new Timed("", null, null);
    }

    private static final Map<UUID, Timed> TIMED = new ConcurrentHashMap<>();

    private static Timed timedOf(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return Timed.NONE;
        }
        String code = record.geneticCode();
        Timed cached = TIMED.get(horse.getUUID());
        if (cached != null && cached.code().equals(code)) {
            return cached;
        }
        GeneAbility.NightWatch nw = null;
        GeneAbility.NightWatch dw = null;
        for (HorseAbilities.Active active : abilitiesOf(horse)) {
            switch (active.ability()) {
                case GeneAbility.NightWatch w -> nw = w;
                case GeneAbility.DayWatch w -> dw = new GeneAbility.NightWatch(w.mode(), w.radius(),
                        w.silentSteps(), w.when(), w.minDose());
                default -> { }
            }
        }
        Timed fresh = new Timed(code, nw, dw);
        TIMED.put(horse.getUUID(), fresh);
        return fresh;
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
