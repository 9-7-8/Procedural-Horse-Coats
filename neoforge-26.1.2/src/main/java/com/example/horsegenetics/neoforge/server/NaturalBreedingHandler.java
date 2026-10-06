package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.example.horsegenetics.common.repro.NaturalCover;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Leave a stallion with mares and there will be foals.</b> Owner, 2026-09-13:
 * the Spontaneous Breeding gene was "now basically just base game behavior",
 * so it is gone and this is what every horse does.
 *
 * <p>This class only gathers facts. Every rule - once per heat, healthy enough
 * ({@link ReproRules#COVER_HEALTH}), not ridden or leashed, no geldings, the
 * local cap, cowboy stock exempt - is {@link NaturalCover#decide}, which is
 * unit-tested. A stallion's day is not capped: past
 * {@code fertility.free_covers_per_day} his odds halve and he keeps covering. The cover goes
 * through {@link ReproHandler#breed}, so carrot effects armed on either horse act
 * on it, and it is credited to the mare's owner. It runs off the entity tick, so it
 * only ever happens in loaded chunks. The heat-attraction goal
 * ({@code HerdGoals.HeatAttraction}) is what brings the two together.
 *
 * <h4>The owner hears why</h4>
 * Owner's ask: <i>"if a breeding attempt does not go through for any reason,
 * print the failure to the horse's owner in chat"</i>. {@link #tell} does that,
 * in {@link CoverNotice}'s words, for the refusals an owner can act on and for
 * how a cover itself went. Which refusals those are, and why the other two say
 * nothing at all, is {@link CoverNotice}'s own documentation. The switch is
 * {@code notices.owned_horse_breeding}. The {@link ActionTrace} lines below are
 * unchanged: they are the debug record, they are not owner-facing, and they
 * throttle on their own schedule.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class NaturalBreedingHandler {

    private NaturalBreedingHandler() {
    }

    /** Two seconds; a heat is at least a Minecraft day, so this is plenty. */
    static final int SCAN = 40;

    /**
     * Each mare's courtship in progress. Transient on purpose, like vanilla's
     * {@code BreedGoal.loveTime}: a restart mid-courtship costs three seconds.
     */
    private static final java.util.Map<java.util.UUID, NaturalCover.Courtship> COURTSHIPS = new java.util.HashMap<>();

    /** When each capped mare last said so. Transient; a restart just says it again. */
    private static final java.util.Map<java.util.UUID, Long> CROWDED_LOGGED = new java.util.HashMap<>();

    /** When each too-hurt mare last said so. Transient, like {@link #CROWDED_LOGGED}. */
    private static final java.util.Map<java.util.UUID, Long> UNFIT_LOGGED = new java.util.HashMap<>();
    /** Yard mares told "no walkable path" in the last minute - see the refusal after {@code canMeet}. */
    private static final java.util.Map<java.util.UUID, Long> UNREACHABLE_LOGGED = new java.util.HashMap<>();

    /**
     * What each mare's owner was last told, and when. Transient like the rest of
     * these: a restart costs one repeated line at most.
     */
    private static final java.util.Map<java.util.UUID, Told> NOTICES = new java.util.HashMap<>();

    /** Above this many remembered mares, the long-quiet ones are dropped on the next line. */
    private static final int SWEEP_ABOVE = 512;

    /** One line to an owner, for the throttle in {@link CoverNotice#dueAgain}. */
    private record Told(CoverNotice.Reason reason, long at) {}

    /** Wild mares the cap held back since {@link #wildCappedSince}, summarised every ten minutes. */
    private static final java.util.Set<java.util.UUID> WILD_CAPPED = new java.util.HashSet<>();
    private static long wildCappedSince = -1L;
    private static final long WILD_CAP_SUMMARY_TICKS = 12_000L;

    /**
     * <b>Can these two actually get to each other?</b> The real question behind
     * "is there a wall in the way", and the reason it is a path and not a line
     * of sight: two horses at opposite ends of one L-shaped pen cannot see each
     * other and should breed, and two horses either side of a glass pane can see
     * each other perfectly and should not.
     *
     * <p>Affordable because of where it is called. It costs one
     * {@code createPath} per stallion already within
     * {@link ReproRules#NATURAL_REACH} of a mare who has already passed
     * {@code mayTryNaturally} - so: only mares in heat, only on the
     * {@value #SCAN}-tick scan, and only over the handful of horses standing
     * essentially next to her. Nothing pays for this except a pairing that was
     * about to happen anyway.
     */
    private static boolean canMeet(Horse mare, Horse stallion) {
        // TO WITHIN ONE BLOCK OF HIM, NOT TO HIS BLOCK (2026-09-30). With accuracy 0 the path had to end on
        // the block the stallion stands in, and a horse-sized path very often cannot: the yard's run 11
        // logged mares refused "no walkable path" to a stallion 1.5 to 4 blocks away in the same open pen,
        // wild bands included, and HURT MARE went whole runs uncovered for it. One block of slack is
        // "standing beside him". It does not open a fence: two horses either side of a one-block fence
        // stand two blocks apart at the closest, and the owner's call the same day is that a fence blocks
        // a cover like a wall (REACH FENCE now expects none). UNVERIFIED beyond the yard's pens.
        // Touching. There is no path to compute between two horses whose
        // hitboxes already overlap, and there is no room for a wall between them
        // either - so the absent path means "no distance", not "no way through".
        if (mare.getBoundingBox().inflate(0.1).intersects(stallion.getBoundingBox())) {
            return true;
        }
        // The cheap answers first (2026-10-02): one ray is far cheaper than an A* search, and the pathfinder was
        // ~6% of a breeding thousand-horse server's time. Only a pair further apart, or with something between
        // them, pays for the path.
        if (besideWithNothingBetween(mare, stallion)) {
            return true;
        }
        return pathBetween(mare, stallion);
    }

    /**
     * The path answer for one pair, kept while neither horse has moved a block (#201). A pair the path refuses -
     * a stallion over a fence - was asked again every {@value #SCAN} ticks for the whole of her heat, an A* each
     * time for an answer that could not have changed. Kept {@link #REACH_TTL} ticks at most, so a gate opened
     * between two horses standing still is noticed within that.
     */
    private record PairKey(java.util.UUID mare, java.util.UUID stallion) {}

    private record Reach(long mareAt, long stallionAt, boolean ok, long at) {}

    private static final java.util.Map<PairKey, Reach> REACH = new java.util.HashMap<>();
    private static final long REACH_TTL = 400L;

    private static boolean pathBetween(Horse mare, Horse stallion) {
        long now = mare.level().getGameTime();
        long mareAt = mare.blockPosition().asLong();
        long stallionAt = stallion.blockPosition().asLong();
        PairKey key = new PairKey(mare.getUUID(), stallion.getUUID());
        Reach known = REACH.get(key);
        if (known != null && known.mareAt() == mareAt && known.stallionAt() == stallionAt
                && now - known.at() < REACH_TTL) {
            return known.ok();
        }
        Path path = mare.getNavigation().createPath(stallion, 1);
        boolean ok = path != null && path.canReach();
        if (REACH.size() > SWEEP_ABOVE) {
            REACH.values().removeIf(r -> now - r.at() >= REACH_TTL);
        }
        REACH.put(key, new Reach(mareAt, stallionAt, ok, now));
        return ok;
    }

    /**
     * <b>Side by side, with nothing solid between them</b> (2026-10-02). The path test fails for a horse standing
     * against a wall: the pathfinder plans a 1.4-wide horse as a 2x2 mob, so beside a wall its own start node does not
     * fit and no path is ever found, however near the stallion. The yard's RATIO BRINDLE, STARBURST and MITF pens bred
     * for an hour and then stood refused - "no walkable path to ... (1.6 blocks)" - for the next two and a half, the
     * pair grazing against the pen's east wall. Every small stable is that pen.
     *
     * <p>So, when the path says no: two horses whose hitboxes are within a block of each other, and a line between their
     * bodies that crosses no block's collision shape. A fence, a pane, a wall and a closed gate all have one and still
     * part them - the owner's 2026-09-30 call that a fence blocks a cover stands; an open gate has none.
     */
    private static boolean besideWithNothingBetween(Horse mare, Horse stallion) {
        if (!mare.getBoundingBox().inflate(1.0).intersects(stallion.getBoundingBox())) {
            return false;
        }
        double y = 0.5;
        net.minecraft.world.phys.Vec3 from = mare.position().add(0.0, y, 0.0);
        net.minecraft.world.phys.Vec3 to = stallion.position().add(0.0, y, 0.0);
        net.minecraft.world.phys.BlockHitResult hit = mare.level().clip(new net.minecraft.world.level.ClipContext(
                from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, mare));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    /** All of it is transient; none of it should reach the next singleplayer world (#200). */
    @SubscribeEvent
    static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        COURTSHIPS.clear();
        CROWDED_LOGGED.clear();
        UNFIT_LOGGED.clear();
        UNREACHABLE_LOGGED.clear();
        NOTICES.clear();
        WILD_CAPPED.clear();
        wildCappedSince = -1L;
        REACH.clear();
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse mare) || !mare.isAlive() || mare.isBaby()) {
            return;
        }
        if (!(mare.level() instanceof ServerLevel level)) {
            return;
        }
        if ((mare.tickCount + mare.getId()) % SCAN != 0 || !HorseRecords.hasRealRecord(mare)) {
            return;
        }
        HorseRecord mareRecord = HorseRecords.of(mare);
        if (mareRecord.sex() != Sex.FEMALE) {
            return;
        }
        if (HorseRealmRepro.reproductionPaused(mare)) {
            // realm.breeding_rate_percent = 0. Her clock is frozen, so heat and
            // gestation have already stopped on their own - but a mare who is
            // standing IN heat with her one try unspent could still be covered
            // on the tick the pause began. A paused realm is a still one.
            return;
        }
        // Her own reproductive clock, not the world's - see HorseRealmRepro.
        long now = HorseRealmRepro.reproTime(mare);
        long realNow = level.getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = ReproHandler.of(mare);
        if (!ReproRules.mayTryNaturally(r, now, t)) {
            return;     // the cheap refusal first: most mares, most of the time
        }

        // Entire stallions only (#201): NaturalCover.decide drops anything else, so a gelding, a colt or a
        // ridden stallion beside her was paid a path for and then thrown away.
        List<Horse> near = level.getEntitiesOfClass(Horse.class,
                mare.getBoundingBox().inflate(ReproRules.NATURAL_REACH),
                h -> h != mare && h.isAlive() && HorseRecords.hasRealRecord(h)
                        && HorseRecords.of(h).sex() == Sex.MALE && YardPens.together(mare, h)
                        && party(h).entireStallion());
        if (near.isEmpty()) {
            return;
        }
        // A WALL IS NOT A SUGGESTION (owner, 2026-09-25: "covers are now
        // happening through walls"). Everything above this line is straight-line
        // distance: an inflated AABB, and YardPens.together, which returns true
        // outside the test yard and says so in its own javadoc. So a mare and a
        // stallion in adjacent stalls were three blocks apart and therefore
        // breeding.
        //
        // This was always true; what changed is that it became visible. The
        // crowding cap used to be a hard-coded eight within sixteen blocks and
        // was accidentally doing this job - in a barn it fired CROWDED and
        // suppressed nearly every cover. Raising it to the configured default of
        // fifty took the accident away and left the unguarded three-block box as
        // the only gate.
        //
        // Filtered here rather than at the COVER verdict so that an unreachable
        // stallion does not merely block the cover: he is not a candidate, and
        // NaturalCover.decide picks the best of the ones she can actually get
        // to. Order is preserved, which matters - decision.stallion() indexes
        // back into this list.
        List<Horse> inReach = List.copyOf(near);
        near.removeIf(stallion -> !canMeet(mare, stallion));
        if (near.isEmpty()) {
            // SAY SO IN THE YARD (2026-09-30). This refusal was silent everywhere, and the yard's HURT
            // MARE - healed, in heat, her stud within a block of her - went two whole runs without a
            // single cover or a line saying why, while three other runs covered her in seconds. The
            // path check is the one silent gate a penned pair can fail, so inside a test pen it now
            // writes a line, once a minute a mare. Outside the yard it stays quiet: a wild mare with
            // a stallion over a fence is ordinary and would bury the log.
            if (YardPens.inPen(mare)) {
                Long saidAt = UNREACHABLE_LOGGED.get(mare.getUUID());
                if (saidAt == null || realNow - saidAt >= 1_200L) {
                    UNREACHABLE_LOGGED.put(mare.getUUID(), realNow);
                    Horse first = inReach.get(0);
                    ActionTrace.log("fertility", ActionTrace.describeShort(mare) + String.format(
                            " not covered: no walkable path to %s (%.1f blocks) or any of %d stallion(s) in reach",
                            ActionTrace.describeShort(first), Math.sqrt(first.distanceToSqr(mare)), inReach.size()));
                }
            }
            return;
        }
        List<NaturalCover.Stallion> candidates = new ArrayList<>(near.size());
        for (Horse h : near) {
            candidates.add(new NaturalCover.Stallion(party(h), h.distanceToSqr(mare),
                    ServerConfig.stallionDay(ReproHandler.of(h).coversOn(HorseRealmRepro.reproTime(h), t.dayTicks()))));
        }
        NaturalCover.Crowd crowd = new NaturalCover.Crowd(level.getEntitiesOfClass(Horse.class,
                mare.getBoundingBox().inflate(ReproRules.NATURAL_CAP_RADIUS),
                h -> h != mare && h.isAlive() && YardPens.together(mare, h)).size(),
                ServerConfig.nearbyHorseCap());

        NaturalCover.Party mareParty = party(mare);
        NaturalCover.Decision decision = NaturalCover.decide(mareParty, r, now, t, candidates, crowd);
        switch (decision.verdict()) {
            case COVER -> {
                // VANILLA'S COURTSHIP FIRST (gap 230): the same stallion in reach for three
                // seconds, as BreedGoal waits out loveTime >= 60 before it breeds.
                Horse stallion = near.get(decision.stallion());
                NaturalCover.Courtship before = COURTSHIPS.get(mare.getUUID());
                // WALL-CLOCK, not her repro clock: the courtship is a three-second
                // approach, a thing that happens in front of you. It is not part of
                // the reproductive calendar and must not be slowed with it, or a
                // quarter-rate realm would want a twelve-second stand-still.
                NaturalCover.Courtship courtship = before == null
                        ? NaturalCover.Courtship.start(stallion.getUUID(), realNow)
                        : before.seen(stallion.getUUID(), realNow);
                if (courtship.complete(realNow)) {
                    COURTSHIPS.remove(mare.getUUID());
                    cover(level, mare, mareRecord, r, stallion, crowd, now, realNow);
                } else {
                    COURTSHIPS.put(mare.getUUID(), courtship);
                }
            }
            case CROWDED -> {
                // The owner hears it wherever she is standing; the log below is the
                // debug record, and splits penned from wild for its own reasons.
                tell(mare, CoverNotice.Reason.CROWDED, crowd, realNow);
                if (!YardPens.inPen(mare)) {
                    // A WILD MARE GETS ONE SUMMARY, NOT A LINE (2026-09-14). Wild bands live
                    // eight to sixteen blocks, so nearly every wild mare is capped every heat,
                    // and a line each a minute was 2,900 of 3,400 fertility lines in a
                    // morning - burying the pen that tests the cap. They are counted instead.
                    if (wildCappedSince < 0L) {
                        wildCappedSince = realNow;
                    } else if (realNow - wildCappedSince >= WILD_CAP_SUMMARY_TICKS) {
                        ActionTrace.log("fertility", "crowding cap held back " + WILD_CAPPED.size()
                                + " wild mares in the last 10 minutes (more than " + crowd.cap()
                                + " horses within " + (int) ReproRules.NATURAL_CAP_RADIUS + " blocks)");
                        WILD_CAPPED.clear();
                        wildCappedSince = realNow;
                    }
                    WILD_CAPPED.add(mare.getUUID());
                    break;
                }
                // Once a minute per mare: a capped paddock asks every two seconds, and
                // eight mares saying so each time buries the log the cap is read from.
                Long last = CROWDED_LOGGED.get(mare.getUUID());
                if (last == null || realNow - last >= 1_200L) {
                    CROWDED_LOGGED.put(mare.getUUID(), realNow);
                    ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " not covered: "
                            + crowd.nearby() + " other horses within " + (int) ReproRules.NATURAL_CAP_RADIUS
                            + " blocks, the cap is " + crowd.cap());
                }
            }
            case NOT_A_BREEDING_MARE -> {
                // HER OWNER HEARS ALL FOUR, unlike the log below: ridden and leashed are
                // the player's own doing, but a mare left saddled in a pen overnight is
                // not, and "why is she the only one not in foal" is the question this
                // whole notice exists to answer.
                CoverNotice.Reason why = null;
                if (!mareParty.healthyEnough()) {
                    why = CoverNotice.Reason.HURT;
                } else if (mareParty.ridden()) {
                    why = CoverNotice.Reason.RIDDEN;
                } else if (mareParty.leashed()) {
                    why = CoverNotice.Reason.LEASHED;
                } else if (mareParty.cowboyStock()) {
                    why = CoverNotice.Reason.COWBOY_STOCK;
                }
                if (why != null) {
                    tell(mare, why, crowd, realNow);
                }
                // ONLY HER HEALTH IS WORTH A LINE (gap 258). To get here she is already an
                // adult mare in heat, with a try left and a stallion in reach, so the rest
                // of the reasons - ridden, leashed, cowboy stock - are the player's own
                // doing and obvious. Being a point short of full health was neither: it
                // refused her silently, and a miscarriage costs half a heart.
                if (!mareParty.healthyEnough()) {
                    Long saidAt = UNFIT_LOGGED.get(mare.getUUID());
                    if (saidAt == null || realNow - saidAt >= 1_200L) {
                        UNFIT_LOGGED.put(mare.getUUID(), realNow);
                        ActionTrace.log("fertility", ActionTrace.describeShort(mare) + String.format(
                                " not covered: hurt, %.1f/%.1f health - a natural cover needs %.0f%%",
                                mare.getHealth(), mare.getMaxHealth(), ReproRules.COVER_HEALTH * 100.0));
                    }
                }
            }
            default -> {
                // no able stallion in reach, or not her heat - nothing worth a line
            }
        }
    }

    /**
     * {@code now} is the mare's own reproductive clock and {@code realNow} the
     * world's - see {@link HorseRealmRepro}. The first spends her try this heat;
     * the second is what every chat throttle and log stamp below wants, because a
     * message is a thing a player reads in real time whatever the realm's pacing.
     */
    private static void cover(ServerLevel level, Horse mare, HorseRecord mareRecord, Reproduction r, Horse stallion,
                              NaturalCover.Crowd crowd, long now, long realNow) {
        // Her one try this heat is spent whatever the roll says.
        ReproHandler.set(mare, r.withNaturalTry(now));
        HorseRecord stallionRecord = HorseRecords.of(stallion);
        Rng rng = HorseRecords.rng(mare);
        Conception.Result result = ReproHandler.breed(mare, mareRecord,
                HorseBreedingHandler.genomeOf(mare, mareRecord, rng),
                HorseBreedingHandler.genomeOf(stallion, stallionRecord, rng),
                stallionRecord, stallion, List.of(), bredBy(mare, mareRecord), ownerPlayer(mare));
        level.broadcastEntityEvent(mare, (byte) 18);
        // A cover is once a heat, so this one is not throttled in practice - and the
        // owner who is told why a cover did not happen has to be told when one did,
        // or the silence reads as another failure.
        tell(mare, switch (result.outcome()) {
            case CONCEIVED -> CoverNotice.Reason.CONCEIVED;
            case DID_NOT_TAKE -> CoverNotice.Reason.DID_NOT_TAKE;
            case NOT_RECEPTIVE -> CoverNotice.Reason.NOT_RECEPTIVE;
        }, crowd, realNow);
        // Null for a wild mare, which HorseProgress swallows - a cover in a wild
        // band is nobody's achievement.
        HorseProgress.complete(ownerPlayer(mare), ProgressTask.NATURAL_COVER);
        ActionTrace.log("fertility", "natural cover: " + ActionTrace.describeShort(mare) + " by "
                + ActionTrace.describeShort(stallion) + " - " + result.outcome()
                + String.format(" (chance %.2f)", result.chance()));
    }

    /**
     * <b>Tell the mare's owner what happened, if it is worth saying again.</b>
     * Silent for a wild mare, for an owner who is not in this world to read it,
     * and while {@code notices.owned_horse_breeding} is off. The words and the
     * quiet period are {@link CoverNotice}; this is the translation.
     */
    private static void tell(Horse mare, CoverNotice.Reason reason, NaturalCover.Crowd crowd, long now) {
        // THE LOG FIRST, AND BY UUID. Everything below needs a player standing in
        // this level with the notices config on; the browser's Log tab needs
        // neither, because the owner who was not here to read the line is exactly
        // who it is for. Its deduplication is in common/ - so a refusal that is
        // still true two seconds from now costs nothing, and this needs no
        // throttle beside the one underneath it.
        java.util.UUID mareOwner = HorseOwnership.ownerId(mare);
        if (mareOwner != null) {
            HorseLog.covered(mare, mareOwner, reason, crowd, now);
        }
        if (!ServerConfig.breedingNotices() || !(ownerPlayer(mare) instanceof ServerPlayer player)) {
            return;
        }
        Told last = NOTICES.get(mare.getUUID());
        if (!CoverNotice.dueAgain(last == null ? null : last.reason(), last == null ? 0L : last.at(), reason, now)) {
            return;
        }
        if (NOTICES.size() > SWEEP_ABOVE) {
            NOTICES.entrySet().removeIf(e -> now - e.getValue().at() > CoverNotice.QUIET_TICKS);
        }
        NOTICES.put(mare.getUUID(), new Told(reason, now));
        player.sendSystemMessage(Component.literal(CoverNotice.line(HorseNotices.name(mare), reason, crowd))
                .withStyle(reason.good() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    /** The facts {@link NaturalCover} asks about one horse. */
    static NaturalCover.Party party(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        // hasData first: getData would attach an empty brand to every horse it asked.
        boolean cowboy = horse.hasData(ModAttachments.COWBOY_BRAND.get())
                && horse.getData(ModAttachments.COWBOY_BRAND.get()).isBranded();
        return new NaturalCover.Party(!horse.isBaby(), record.sex() == Sex.FEMALE, record.gelded(),
                ReproRules.healthyEnoughToBreed(horse.getHealth(), horse.getMaxHealth()),
                horse.isVehicle(), horse.isLeashed(), cowboy);
    }

    /**
     * The mare's owner, for {@code bredBy} - blank for a wild mare. An offline
     * owner is resolved through {@link HorseOwnership#ownerName}.
     */
    private static String bredBy(Horse mare, HorseRecord record) {
        if (!mare.isTamed()) {
            return "";
        }
        return HorseOwnership.ownerName(mare).orElse(record.tamedBy().orElse(""));
    }

    private static @Nullable Player ownerPlayer(Horse horse) {
        return horse.getOwner() instanceof Player p ? p : null;
    }
}
