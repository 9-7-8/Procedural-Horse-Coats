package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genes;
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
 *   <li>Vanilla's own breeding cooldown, which is left entirely alone: this
 *       handler puts a horse <i>in love</i> and lets the ordinary breeding path
 *       do the rest, so age, cooldown, pedigree and stat inheritance all still
 *       run. It must never become a second breeding implementation.</li>
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
        if (horse.isInLove()) {
            trace(horse, "already in love - vanilla's breeding takes it from here");
            return;
        }
        // VANILLA WILL NOT BREED AN UNTAMED OR HURT HORSE. AbstractHorse
        // .canParent() requires isTamed() and full health, and this handler
        // deliberately does not implement breeding itself - it puts a pair in
        // love and lets the ordinary path run. So on an untamed pair setInLove
        // succeeds, hearts appear over both, the love timer runs out and
        // nothing happens, for ever. That is what "the heart particle is not
        // followed by any babies appearing" was (owner, 2026-09-12) once the
        // same-sex bug behind it was fixed and it still did not breed.
        //
        // Refusing here rather than putting them in love is the point: hearts
        // that cannot lead to a foal are a lie the gene tells about itself.
        if (!canEverBreed(horse)) {
            trace(horse, describeBlock(horse));
            return;
        }
        if (!horse.canFallInLove()) {
            trace(horse, "on vanilla's breeding cooldown (this is the limit that governs the rate)");
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
            if (other.isInLove() || !other.canFallInLove() || !canEverBreed(other)) {
                continue;
            }
            // A MARE AND A STALLION, because HorseBreedingHandler cancels
            // same-sex pairings - so putting two mares in love produced hearts
            // over both of them and then nothing at all, for ever. Reported as
            // "the heart particle is not followed by any babies appearing"
            // (owner, 2026-09-12), and it had been true of every same-sex pair
            // since the gene was written: the one visible sign of this gene
            // working is also exactly what it looks like when it cannot.
            if (HorseRecords.of(horse).sex() == HorseRecords.of(other).sex()) {
                continue;
            }
            // In love, not bred: the ordinary breeding path takes it from here,
            // so cooldowns, pedigree and stat inheritance all still apply.
            horse.setInLove(null);
            other.setInLove(null);
            trace(horse, "PAIRED with " + other.getUUID().toString().substring(0, 8)
                    + " - both in love; vanilla breeds them and a foal follows");
            return;
        }
        trace(horse, "no partner: " + carriers + " other carrier(s) in range, none of them "
                + "available (wrong sex, a cooldown, or already in love)");
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
