package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * <b>Horses that breed without being asked.</b> Two of them, both expressing
 * {@code SpontaneousBreedingGene}, within a few blocks of one another, and every
 * so often there is a foal.
 *
 * <h2>This is the most dangerous gene in the batch, and the caps come first</h2>
 * Uncapped automatic animal breeding is the classic way to kill a server, and
 * unlike every other behaviour gene it runs <b>while the player is asleep and
 * not watching</b>. So three limits are load-bearing rather than tuning, and
 * none of them may be relaxed without thinking about a pasture left running
 * overnight:
 *
 * <ul>
 *   <li>{@link #LOCAL_CAP} - if there are already this many horses nearby, the
 *       pair simply do not. This is the one that actually bounds the population,
 *       because it is a limit on the <i>outcome</i> rather than on the rate.</li>
 *   <li>{@link #CHECK_TICKS} - the pair are only even considered occasionally,
 *       and the scan is what costs anything here.</li>
 *   <li><b>Heat and pregnancy.</b> Only a mare in heat pairs, and a pregnant
 *       mare is out of heat until after she foals. The pair conceives through
 *       {@link ReproHandler#tryConceive}, the one path the seed jar and the
 *       breeding carrots use too - so it is still never a second breeding
 *       implementation, just no longer vanilla's (owner, 2026-09-13).</li>
 * </ul>
 *
 * <h2>It takes two</h2>
 * Both horses must express. One expressing horse that bred with anything would
 * spread itself through a herd in a few generations and stop being rare;
 * requiring a pair makes the trait self-limiting genetically as well as
 * mechanically - the population can only grow where somebody has already
 * gathered two.
 */
@EventBusSubscriber
public final class SpontaneousBreedingHandler {

    private SpontaneousBreedingHandler() {
    }

    /** How far apart the pair may be. Small: they have to actually be together. */
    private static final double RANGE = 8.0;

    /**
     * Horses within {@link #RANGE} above which nothing happens at all.
     *
     * <p>The population limit, and the only one of the three that bounds the
     * outcome rather than the rate. A pasture that has reached it stays there
     * for ever rather than growing slowly.
     */
    private static final int LOCAL_CAP = 6;

    /** Ticks between considering it. Long - this is a scan, and it need never be prompt. */
    private static final int CHECK_TICKS = 600;

    /**
     * <b>The same scan, thirty seconds sooner, when the testing tools are on.</b>
     *
     * <p>Thirty-second beats are right for a pasture nobody is watching and
     * wrong for a person standing in front of it waiting to find out whether
     * the gene works at all. With {@code debug.tools} on it runs every four
     * seconds and says what it decided; in a real game neither happens.
     *
     * <p>It changes the <i>rate of checking</i>, not the outcome: vanilla's own
     * breeding cooldown still governs how often a pair can actually produce,
     * which is the limit that matters and is deliberately untouched.
     */
    private static final int DEBUG_CHECK_TICKS = 80;

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        // Offset off the horse's own tick count so a field of them does not all
        // scan on the same tick - the discipline every radius effect here follows.
        int beat = ServerConfig.debugTools() ? DEBUG_CHECK_TICKS : CHECK_TICKS;
        if ((horse.tickCount + horse.getId()) % beat != 0) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!expresses(horse)) {
            return;
        }
        // Every refusal below says which one it was. Hearts over a horse mean
        // setInLove landed, and that is the ONE outcome that shows - so from
        // outside, "it is working", "it is on cooldown" and "the field is full"
        // all look identical, which is how a working gene reads as a broken one
        // (owner, 2026-09-12: "I'm seeing heart particles, but I don't know
        // what that means").
        if (horse.isBaby() || horse.isVehicle()) {
            return;     // not a refusal worth a line: it is not a candidate at all
        }
        // TAMED AND AT FULL HEALTH, still. That was vanilla's rule
        // (AbstractHorse.canParent) when this gene only put a pair in love, and
        // it stays the gene's rule now that it conceives directly: a wild pasture
        // breeding itself is band breeding, which is its own slice.
        if (!canEverBreed(horse)) {
            trace(horse, describeBlock(horse));
            return;
        }
        // A MARE OUT OF HEAT HAS NOTHING TO PAIR FOR - and a pregnant one is out
        // of heat until after she foals, which is now the limit on the rate.
        if (isMare(horse) && !ReproHandler.receptive(horse)) {
            trace(horse, "a mare who is " + ReproHandler.stateOf(horse).label().toLowerCase()
                    + " - nothing to pair for");
            return;
        }

        AABB box = horse.getBoundingBox().inflate(RANGE);
        List<Horse> nearby = level.getEntitiesOfClass(Horse.class, box, Horse::isAlive);
        if (nearby.size() > LOCAL_CAP) {
            trace(horse, "the local cap stopped it: " + nearby.size() + " horses within "
                    + (int) RANGE + " blocks, cap is " + LOCAL_CAP);
            return;
        }

        int carriers = 0;
        for (Horse other : nearby) {
            if (other == horse || other.isBaby()) {
                continue;
            }
            if (!expresses(other)) {
                continue;
            }
            carriers++;
            if (!canEverBreed(other)) {
                continue;
            }
            // A MARE AND A STALLION. Two mares used to go into love together and
            // produce hearts and nothing else (owner, 2026-09-12).
            if (HorseRecords.of(horse).sex() == HorseRecords.of(other).sex()) {
                continue;
            }
            Horse mare = isMare(horse) ? horse : other;
            Horse stallion = mare == horse ? other : horse;
            if (!ReproHandler.receptive(mare)) {
                continue;
            }
            // CONCEIVED DIRECTLY, through the same path as the seed jar and the
            // breeding carrots (owner, 2026-09-13) - not vanilla love any more,
            // so there are no hearts that do not mean anything.
            HorseRecord mareRecord = HorseBreedingHandler.ensureParentRecord(mare);
            HorseRecord stallionRecord = HorseBreedingHandler.ensureParentRecord(stallion);
            Rng rng = HorseRecords.rng(mare);
            Conception.Result result = ReproHandler.breed(mare, mareRecord,
                    HorseBreedingHandler.genomeOf(mare, mareRecord, rng),
                    HorseBreedingHandler.genomeOf(stallion, stallionRecord, rng),
                    stallionRecord, stallion, List.of(), null);
            level.broadcastEntityEvent(mare, (byte) 18);
            trace(horse, "PAIRED with " + other.getUUID().toString().substring(0, 8) + " - "
                    + result.outcome() + String.format(" (chance %.2f)", result.chance()));
            return;
        }
        trace(horse, "no partner: " + carriers + " other carrier(s) in range, none of them "
                + "available (same sex, untamed or hurt, or no mare in heat)");
    }

    private static boolean isMare(Horse horse) {
        return HorseRecords.of(horse).sex() == com.example.horsegenetics.common.horse.Sex.FEMALE;
    }

    /**
     * The two things vanilla insists on that this gene cannot supply: a tamed
     * horse at full health. Checked here so the gene never puts a horse in love
     * that cannot act on it.
     */
    private static boolean canEverBreed(Horse horse) {
        return horse.isTamed() && horse.getHealth() >= horse.getMaxHealth();
    }

    private static String describeBlock(Horse horse) {
        if (!horse.isTamed()) {
            return "UNTAMED - vanilla refuses to breed untamed horses (AbstractHorse.canParent), "
                    + "so this gene cannot do anything with it. Tame it and it will pair.";
        }
        return "hurt (" + Math.round(horse.getHealth()) + "/" + Math.round(horse.getMaxHealth())
                + ") - vanilla wants full health before breeding";
    }

    /** One line per scan, only when the testing tools are on. */
    private static void trace(Horse horse, String what) {
        ActionTrace.log("spontaneous breeding",
                horse.getUUID().toString().substring(0, 8) + " at "
                        + horse.blockPosition().toShortString() + ": " + what);
    }

    /** Does this horse carry two copies? Both parents must, which is the whole gene. */
    private static boolean expresses(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return false;
        }
        try {
            Genotype genotype = Genotype.parse(record.geneticCode());
            return Genes.SPONTANEOUS_BREEDING.expresses(
                    genotype.pair(Genes.SPONTANEOUS_BREEDING));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
