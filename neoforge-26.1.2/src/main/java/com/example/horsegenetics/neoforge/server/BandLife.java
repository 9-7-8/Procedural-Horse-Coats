package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.BandType;
import com.example.horsegenetics.common.herd.HerdRules;
import com.example.horsegenetics.common.herd.Relationship;
import com.example.horsegenetics.common.herd.SocialLedger;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseSocialAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>The events that change who is in which band</b> - wild horses only, decided
 * on the social tick ({@link HerdSocialHandler}) at the per-day rates in
 * {@link HerdRules}.
 *
 * <ul>
 *   <li><b>Dispersal.</b> A grown colt leaves the band he was born into for a
 *       bachelor band, or strikes out alone; a grown filly joins another family
 *       band, or leaves with a bachelor and makes a new one.</li>
 *   <li><b>Takeovers.</b> A bachelor challenges a band stallion. The fight is real
 *       but stops short: whoever would drop below
 *       {@link HerdRules#FIGHT_YIELD_HEALTH} of his health breaks off instead, so no
 *       horse dies of one. A winning challenger takes the band; the old stallion
 *       becomes a bachelor. A family band whose stallion is simply <i>gone</i> is
 *       claimed by the first bachelor to reach it, without a fight.</li>
 *   <li><b>Mare transfers.</b> A mare drifts to a nearby band where she already
 *       knows the mares.</li>
 * </ul>
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class BandLife {

    private BandLife() {
    }

    private static final double DISPERSE_REACH = 64.0;
    private static final double CHALLENGE_REACH = 24.0;
    private static final double TRANSFER_REACH = 32.0;
    /** A fight nobody is winning ends after this long, in the defender's favour. */
    private static final long FIGHT_TIMEOUT = 600L;

    private record Fight(UUID challenger, UUID defender, long started) {
        UUID opponentOf(UUID id) {
            return id.equals(challenger) ? defender : challenger;
        }
    }

    /** Both fighters map to the one fight. Transient - a restart ends every fight. */
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    public static boolean inFight(Horse horse) {
        return FIGHTS.containsKey(horse.getUUID());
    }

    // ------------------------------------------------------------------
    // The decision, once per scan per wild horse
    // ------------------------------------------------------------------

    static void consider(Horse horse, ServerLevel level, long now) {
        Fight fight = FIGHTS.get(horse.getUUID());
        if (fight != null) {
            checkFight(fight, level, now);
            return;
        }
        if (horse.isBaby()) {
            return;
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
        Sex sex = HorseRecords.of(horse).sex();

        if (social.natalHerd().isPresent() && social.natalHerd().equals(care.herd())
                && social.bornKnown() && HerdRules.dueToDisperse(social.bornTick(), now, social.disperseAfterDays())) {
            disperse(horse, level, care, social, sex);
            return;
        }

        if (HorseRecords.of(horse).entire() && HerdSocialHandler.isBachelor(care)
                && horse.getRandom().nextDouble() < HerdRules.perScan(
                        HerdRules.CHALLENGES_PER_BAND_PER_DAY, HerdSocialHandler.SCAN)) {
            challenge(horse, level, care, now);
            return;
        }

        boolean mare = sex == Sex.FEMALE && !HerdSocialHandler.isBachelor(care)
                && !care.herd().map(horse.getUUID()::equals).orElse(false);
        if (mare && social.natalHerd().isEmpty()
                && horse.getRandom().nextDouble() < HerdRules.perScan(
                        HerdRules.TRANSFERS_PER_MARE_PER_DAY, HerdSocialHandler.SCAN)) {
            transfer(horse, level, care, social);
        }
    }

    // ------------------------------------------------------------------
    // Dispersal
    // ------------------------------------------------------------------

    private static void disperse(Horse horse, ServerLevel level, HorseCareAttachment care,
                                 HorseSocialAttachment social, Sex sex) {
        String breed = care.herdBreed().orElse("");
        UUID natal = care.herd().orElseThrow();
        if (sex == Sex.MALE) {
            Horse bachelor = nearest(level, horse, DISPERSE_REACH, h -> {
                HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
                return c.inWildHerd() && HerdSocialHandler.isBachelor(c);
            });
            if (bachelor != null) {
                HorseCareAttachment theirs = bachelor.getData(ModAttachments.HORSE_CARE.get());
                join(horse, theirs.herd().orElseThrow(), theirs.herdBreed().orElse(breed), BandType.BACHELOR);
            } else {
                join(horse, horse.getUUID(), breed, BandType.BACHELOR);   // on his own, for now
            }
        } else {
            Horse band = nearest(level, horse, DISPERSE_REACH, h -> {
                HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
                return c.inWildHerd() && !HerdSocialHandler.isBachelor(c)
                        && !c.herd().map(natal::equals).orElse(false);
            });
            Horse suitor = nearest(level, horse, DISPERSE_REACH, h -> {
                HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
                return !h.isBaby() && HorseRecords.of(h).entire() && c.inWildHerd()
                        && HerdSocialHandler.isBachelor(c);
            });
            if (suitor != null && (band == null || suitor.distanceToSqr(horse) < band.distanceToSqr(horse))) {
                // Recruited by a bachelor: the most common way a stallion gets his first mare.
                String theirBreed = suitor.getData(ModAttachments.HORSE_CARE.get()).herdBreed().orElse(breed);
                join(suitor, suitor.getUUID(), theirBreed, BandType.TRADITIONAL);
                join(horse, suitor.getUUID(), theirBreed, BandType.TRADITIONAL);
            } else if (band != null) {
                HorseCareAttachment theirs = band.getData(ModAttachments.HORSE_CARE.get());
                join(horse, theirs.herd().orElseThrow(), theirs.herdBreed().orElse(breed), BandType.TRADITIONAL);
            } else {
                return;     // nowhere to go yet - she stays, and asks again next scan
            }
        }
        horse.setData(ModAttachments.HORSE_SOCIAL.get(), social.dispersed());
        ActionTrace.log("herd", ActionTrace.describeShort(horse) + " left the band it was born into ("
                + HerdSocialHandler.short8(natal) + ")");
    }

    // ------------------------------------------------------------------
    // Takeovers
    // ------------------------------------------------------------------

    private static void challenge(Horse bachelor, ServerLevel level, HorseCareAttachment care, long now) {
        Horse mare = nearest(level, bachelor, CHALLENGE_REACH, h -> {
            HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
            return !h.isBaby() && HorseRecords.of(h).sex() == Sex.FEMALE && c.inWildHerd()
                    && !HerdSocialHandler.isBachelor(c);
        });
        if (mare == null) {
            return;
        }
        HorseCareAttachment band = mare.getData(ModAttachments.HORSE_CARE.get());
        UUID herd = band.herd().orElseThrow();
        Horse stallion = HerdSocialHandler.leadOf(level, herd);
        if (stallion == null) {
            // A vacant band: no fight to have. The bachelor simply takes it.
            takeOver(level, bachelor, herd, band.herdBreed().orElse(""), null);
            return;
        }
        if (stallion == bachelor || inFight(stallion) || stallion.distanceToSqr(bachelor) > CHALLENGE_REACH * CHALLENGE_REACH * 4) {
            return;
        }
        Fight fight = new Fight(bachelor.getUUID(), stallion.getUUID(), now);
        FIGHTS.put(bachelor.getUUID(), fight);
        FIGHTS.put(stallion.getUUID(), fight);
        bachelor.setTarget(stallion);
        stallion.setTarget(bachelor);
        ActionTrace.log("herd", ActionTrace.describeShort(bachelor) + " challenged "
                + ActionTrace.describeShort(stallion) + " for band " + HerdSocialHandler.short8(herd));
    }

    /**
     * <b>Nobody dies of a herd fight.</b> The blow that would take a fighter below
     * the yield line is cancelled and the fight ends there, with that horse the
     * loser. Runs early, so the herd-aggro handler never sees it either.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onFightDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Horse victim) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Fight fight = FIGHTS.get(victim.getUUID());
        if (fight == null || !(event.getSource().getEntity() instanceof Horse attacker)
                || !attacker.getUUID().equals(fight.opponentOf(victim.getUUID()))) {
            return;
        }
        if (victim.getHealth() - event.getAmount() < victim.getMaxHealth() * HerdRules.FIGHT_YIELD_HEALTH) {
            event.setCanceled(true);
            end(level, fight, attacker, victim);
        }
    }

    private static void checkFight(Fight fight, ServerLevel level, long now) {
        Horse challenger = level.getEntity(fight.challenger()) instanceof Horse h && h.isAlive() ? h : null;
        Horse defender = level.getEntity(fight.defender()) instanceof Horse h && h.isAlive() ? h : null;
        if (challenger == null || defender == null) {
            FIGHTS.remove(fight.challenger());
            FIGHTS.remove(fight.defender());
            return;
        }
        if (now - fight.started() > FIGHT_TIMEOUT) {
            end(level, fight, defender, challenger);    // a stalemate goes to the horse holding the mares
        }
    }

    private static void end(ServerLevel level, Fight fight, Horse winner, Horse loser) {
        FIGHTS.remove(fight.challenger());
        FIGHTS.remove(fight.defender());
        winner.setTarget(null);
        loser.setTarget(null);
        settle(winner, loser, HerdRules.STAKES_FIGHT, level.getGameTime());
        retreat(loser, winner, 16.0);

        HorseCareAttachment defenderCare = level.getEntity(fight.defender()) instanceof Horse d
                ? d.getData(ModAttachments.HORSE_CARE.get()) : null;
        if (winner.getUUID().equals(fight.challenger()) && defenderCare != null && defenderCare.herd().isPresent()) {
            takeOver(level, winner, defenderCare.herd().get(), defenderCare.herdBreed().orElse(""), loser);
        } else {
            ActionTrace.log("herd", ActionTrace.describeShort(winner) + " saw off "
                    + ActionTrace.describeShort(loser));
        }
    }

    /** {@code newStallion} takes band {@code herd}; the deposed stallion, if any, turns bachelor. */
    private static void takeOver(ServerLevel level, Horse newStallion, UUID herd, String breed, Horse deposed) {
        List<Horse> members = HerdSocialHandler.bandMembers(level, herd, deposed != null ? deposed : newStallion);
        members.remove(newStallion);
        if (deposed != null) {
            members.remove(deposed);
        }
        // THE BACHELORS HE LED STAY BACHELORS. Their herd id is his UUID, which is
        // about to become a family band's id - so without this they were counted
        // as his mares' band-mates: never intruders to him, and following him
        // about as a harem of stallions. Found building the yard's herd rows,
        // before anyone saw it; they re-form round the best of those left.
        HorseCareAttachment own = newStallion.getData(ModAttachments.HORSE_CARE.get());
        List<Horse> leftBehind = HerdSocialHandler.isBachelor(own)
                && own.herd().map(newStallion.getUUID()::equals).orElse(false)
                ? HerdSocialHandler.bandMembers(level, newStallion.getUUID(), newStallion) : new java.util.ArrayList<>();
        leftBehind.remove(newStallion);
        join(newStallion, newStallion.getUUID(), breed, BandType.TRADITIONAL);
        repoint(members, newStallion.getUUID(), breed, BandType.TRADITIONAL);
        if (!leftBehind.isEmpty()) {
            Horse top = HerdSocialHandler.topRanked(leftBehind);
            repoint(leftBehind, top.getUUID(), own.herdBreed().orElse(breed), BandType.BACHELOR);
            ActionTrace.log("herd", "the bachelors " + ActionTrace.describeShort(newStallion) + " left behind re-formed round "
                    + ActionTrace.describeShort(top));
        }
        if (deposed != null) {
            Horse bachelors = nearest(level, deposed, DISPERSE_REACH, h -> {
                HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
                return h != newStallion && c.inWildHerd() && HerdSocialHandler.isBachelor(c);
            });
            if (bachelors != null) {
                HorseCareAttachment theirs = bachelors.getData(ModAttachments.HORSE_CARE.get());
                join(deposed, theirs.herd().orElseThrow(), theirs.herdBreed().orElse(breed), BandType.BACHELOR);
            } else {
                join(deposed, deposed.getUUID(), breed, BandType.BACHELOR);
            }
        }
        ActionTrace.log("herd", ActionTrace.describeShort(newStallion) + " took over band "
                + HerdSocialHandler.short8(herd) + " (" + members.size() + " horses)");
    }

    // ------------------------------------------------------------------
    // Transfers
    // ------------------------------------------------------------------

    private static void transfer(Horse mare, ServerLevel level, HorseCareAttachment care, HorseSocialAttachment social) {
        UUID own = care.herd().orElseThrow();
        SocialLedger ledger = social.ledger();
        Horse best = null;
        double bestPull = 0.0;
        for (Horse other : level.getEntitiesOfClass(Horse.class, mare.getBoundingBox().inflate(TRANSFER_REACH),
                h -> h != mare && h.isAlive() && !h.isTamed() && YardPens.together(mare, h))) {
            HorseCareAttachment c = other.getData(ModAttachments.HORSE_CARE.get());
            if (!c.inWildHerd() || HerdSocialHandler.isBachelor(c) || c.herd().map(own::equals).orElse(true)) {
                continue;
            }
            double pull = ledger.with(other.getUUID()).map(r -> r.familiarity() + r.grooming() * 2).orElse(0.0);
            if (pull > bestPull) {
                bestPull = pull;
                best = other;
            }
        }
        if (best == null || bestPull < 0.6) {
            return;
        }
        HorseCareAttachment theirs = best.getData(ModAttachments.HORSE_CARE.get());
        join(mare, theirs.herd().orElseThrow(), theirs.herdBreed().orElse(""), BandType.TRADITIONAL);
        ActionTrace.log("herd", ActionTrace.describeShort(mare) + " moved from band " + HerdSocialHandler.short8(own)
                + " to " + ActionTrace.describeShort(best) + "'s");
    }

    // ------------------------------------------------------------------
    // Shared helpers - the behaviour goals use settle and retreat too
    // ------------------------------------------------------------------

    static void repoint(List<Horse> horses, UUID lead, String breed, BandType band) {
        for (Horse h : horses) {
            join(h, lead, breed, band);
        }
    }

    private static void join(Horse horse, UUID lead, String breed, BandType band) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        horse.setData(ModAttachments.HORSE_CARE.get(), care.withWildHerd(lead, breed, band.name()));
    }

    /** Write a contest's outcome onto both horses' ledgers. */
    static void settle(Horse winner, Horse loser, double stakes, long now) {
        HorseSocialAttachment w = winner.getData(ModAttachments.HORSE_SOCIAL.get());
        HorseSocialAttachment l = loser.getData(ModAttachments.HORSE_SOCIAL.get());
        SocialLedger[] out = HerdRules.settle(w.ledger(), winner.getUUID(), l.ledger(), loser.getUUID(), stakes, now);
        winner.setData(ModAttachments.HORSE_SOCIAL.get(), w.withLedger(out[0], w.lastDecay()));
        loser.setData(ModAttachments.HORSE_SOCIAL.get(), l.withLedger(out[1], l.lastDecay()));
    }

    /** Move {@code horse} directly away from {@code from}. */
    static void retreat(Horse horse, Horse from, double distance) {
        Vec3 away = horse.position().subtract(from.position());
        if (away.lengthSqr() < 1.0e-4) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 to = horse.position().add(new Vec3(away.x, 0, away.z).normalize().scale(distance));
        horse.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
    }

    /** What {@code horse} brings to a contest against {@code other}. */
    static HerdRules.Contestant contestant(Horse horse, Horse other, long now) {
        HorseSocialAttachment s = horse.getData(ModAttachments.HORSE_SOCIAL.get());
        return new HerdRules.Contestant(horse.getHealth(), horse.getMaxHealth(),
                horse.getAttributeValue(Attributes.SCALE),
                s.bornKnown() ? HerdRules.ageDays(s.bornTick(), now) : 5.0,
                s.ledger().rankOver(other.getUUID()));
    }

    private static Horse nearest(ServerLevel level, Horse from, double reach,
                                 java.util.function.Predicate<Horse> test) {
        Horse best = null;
        double bestD = Double.MAX_VALUE;
        for (Horse h : level.getEntitiesOfClass(Horse.class, from.getBoundingBox().inflate(reach),
                h -> h != from && h.isAlive() && !h.isTamed() && HorseRecords.hasRealRecord(h)
                        && YardPens.together(from, h))) {
            if (!test.test(h)) {
                continue;
            }
            double d = h.distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = h;
            }
        }
        return best;
    }

    /** Puff of particles over a horse's head - the ears-back, snake-neck moment a player can see. */
    static void threat(ServerLevel level, Horse horse) {
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, horse.getX(), horse.getEyeY() + 0.4, horse.getZ(),
                1, 0.2, 0.1, 0.2, 0.0);
    }

    static Optional<Relationship> relationship(Horse horse, Horse other) {
        return horse.getData(ModAttachments.HORSE_SOCIAL.get()).ledger().with(other.getUUID());
    }
}
