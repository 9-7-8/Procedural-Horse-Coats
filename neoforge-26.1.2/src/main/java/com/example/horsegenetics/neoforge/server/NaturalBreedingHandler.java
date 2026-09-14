package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Leave a stallion with mares and there will be foals.</b> Owner, 2026-09-13:
 * the Spontaneous Breeding gene was "now basically just base game behavior",
 * so it is gone and this is what every horse does.
 *
 * <p>Every {@link #SCAN} ticks, an adult mare - tamed or wild - is covered by
 * the nearest entire stallion within {@link ReproRules#NATURAL_REACH} when all of
 * this holds:
 * <ul>
 *   <li>she is in heat and has not been covered yet this heat
 *       ({@link ReproRules#mayTryNaturally}). A cover that does not take waits for
 *       her next heat;</li>
 *   <li>both are at full health, neither is ridden or leashed, and neither is
 *       cowboy stock;</li>
 *   <li>he is not a gelding, and has made fewer than
 *       {@link ReproRules#FREE_COVERS_PER_DAY} covers today - a hard stop here,
 *       where the carrot and jar paths only halve his odds;</li>
 *   <li>fewer than {@link ReproRules#NATURAL_CAP} other horses, foals included,
 *       stand within {@link ReproRules#NATURAL_CAP_RADIUS} of her.</li>
 * </ul>
 *
 * <p>The cover goes through {@link ReproHandler#breed}, so carrot effects armed on
 * either horse act on it, and it is credited to the mare's owner. It runs off
 * the entity tick, so it only ever happens in loaded chunks. The heat-attraction
 * goal ({@code HerdGoals.HeatAttraction}) is what brings the two together.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class NaturalBreedingHandler {

    private NaturalBreedingHandler() {
    }

    /** Two seconds; a heat is at least a Minecraft day, so this is plenty. */
    static final int SCAN = 40;

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
        if (mareRecord.sex() != Sex.FEMALE || !ableToBreed(mare)) {
            return;
        }
        long now = level.getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = ReproHandler.of(mare);
        if (!ReproRules.mayTryNaturally(r, now, t)) {
            return;
        }

        Horse stallion = nearestStallion(level, mare, now, t);
        if (stallion == null) {
            return;
        }
        int crowd = level.getEntitiesOfClass(Horse.class,
                mare.getBoundingBox().inflate(ReproRules.NATURAL_CAP_RADIUS), h -> h != mare && h.isAlive()).size();
        if (crowd >= ReproRules.NATURAL_CAP) {
            ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " not covered: " + crowd
                    + " other horses within " + (int) ReproRules.NATURAL_CAP_RADIUS + " blocks, the cap is "
                    + ReproRules.NATURAL_CAP);
            return;
        }

        // Her one try this heat is spent whatever the roll says.
        ReproHandler.set(mare, r.withNaturalTry(now));
        HorseRecord stallionRecord = HorseRecords.of(stallion);
        Rng rng = HorseRecords.rng(mare);
        Conception.Result result = ReproHandler.breed(mare, mareRecord,
                HorseBreedingHandler.genomeOf(mare, mareRecord, rng),
                HorseBreedingHandler.genomeOf(stallion, stallionRecord, rng),
                stallionRecord, stallion, List.of(), bredBy(mare, mareRecord), ownerPlayer(mare));
        level.broadcastEntityEvent(mare, (byte) 18);
        ActionTrace.log("fertility", "natural cover: " + ActionTrace.describeShort(mare) + " by "
                + ActionTrace.describeShort(stallion) + " - " + result.outcome()
                + String.format(" (chance %.2f)", result.chance()));
    }

    private static @Nullable Horse nearestStallion(ServerLevel level, Horse mare, long now, ReproTiming t) {
        Horse best = null;
        for (Horse h : level.getEntitiesOfClass(Horse.class, mare.getBoundingBox().inflate(ReproRules.NATURAL_REACH),
                h -> h != mare && h.isAlive() && !h.isBaby() && HorseRecords.hasRealRecord(h))) {
            if (!HorseRecords.of(h).entire() || !ableToBreed(h)
                    || ReproHandler.of(h).coversOn(now, t.dayTicks()) >= ReproRules.FREE_COVERS_PER_DAY) {
                continue;
            }
            if (best == null || h.distanceToSqr(mare) < best.distanceToSqr(mare)) {
                best = h;
            }
        }
        return best;
    }

    /** Full health, not ridden, not leashed, and not a cowboy's stock. */
    private static boolean ableToBreed(Horse horse) {
        if (horse.getHealth() < horse.getMaxHealth() || horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        // hasData first: getData would attach an empty brand to every horse it asked.
        return !horse.hasData(ModAttachments.COWBOY_BRAND.get())
                || !horse.getData(ModAttachments.COWBOY_BRAND.get()).isBranded();
    }

    /**
     * The mare's owner, for {@code bredBy} - blank for a wild mare. <b>An offline
     * owner falls back to whoever first tamed her</b>, usually the same person:
     * this module has no lookup from an absent player's id to a name, and the
     * profile cache on 26.1.2 has not been checked. See known gaps.
     */
    private static String bredBy(Horse mare, HorseRecord record) {
        if (!mare.isTamed()) {
            return "";
        }
        if (mare.getOwner() instanceof Player owner) {
            return owner.getGameProfile().name();
        }
        return record.tamedBy().orElse("");
    }

    private static @Nullable Player ownerPlayer(Horse horse) {
        return horse.getOwner() instanceof Player p ? p : null;
    }
}
