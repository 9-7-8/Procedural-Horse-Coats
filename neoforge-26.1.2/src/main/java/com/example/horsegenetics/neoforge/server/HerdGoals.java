package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.herd.HerdRules;
import com.example.horsegenetics.common.herd.Relationship;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseSocialAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>What band life looks like</b> - the five goals that turn the relationships
 * {@link HerdSocialHandler} keeps into something a player can watch.
 *
 * <table>
 *   <tr><th>goal</th><th>priority</th><th>who</th></tr>
 *   <tr><td>{@link Spar}</td><td>3</td><td>colts and stallions who know each other - tamed too</td></tr>
 *   <tr><td>{@link DamAndFoal}</td><td>4</td><td>a foal and its dam - tamed too</td></tr>
 *   <tr><td>{@link StallionGuard}</td><td>5</td><td>wild band stallions</td></tr>
 *   <tr><td>{@link Displace}</td><td>5</td><td>any horse at food or water - tamed too</td></tr>
 *   <tr><td>{@link GroomAndRest}</td><td>7</td><td>grooming partners - tamed too</td></tr>
 * </table>
 *
 * <p>Nothing here does damage. Sparring is rearing, a push and a short chase; the
 * one real fight is a takeover, and that is {@link BandLife}'s.
 *
 * <p><b>Not verified in-game</b>, and every number below is a first guess at what
 * reads as a horse rather than a measurement.
 */
@EventBusSubscriber
public final class HerdGoals {

    private HerdGoals() {
    }

    @SubscribeEvent
    static void addGoals(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof Spar) {
                return;
            }
        }
        horse.goalSelector.addGoal(3, new Spar(horse));
        horse.goalSelector.addGoal(4, new DamAndFoal(horse));
        horse.goalSelector.addGoal(5, new StallionGuard(horse));
        horse.goalSelector.addGoal(5, new Displace(horse));
        horse.goalSelector.addGoal(7, new GroomAndRest(horse));
    }

    private static boolean free(Horse horse) {
        return horse.isAlive() && !horse.isVehicle() && !horse.isLeashed() && horse.getTarget() == null
                && !BandLife.inFight(horse) && !HorseInspectHold.isHeld(horse);
    }

    private static Horse loaded(Horse horse, UUID id) {
        return horse.level() instanceof ServerLevel level && level.getEntity(id) instanceof Horse h && h.isAlive()
                ? h : null;
    }

    // ------------------------------------------------------------------
    // Sparring
    // ------------------------------------------------------------------

    /** Who is sparring whom, until when. Both horses map to the one bout. */
    private record Bout(UUID a, UUID b, long ends) {
    }

    private static final Map<UUID, Bout> BOUTS = new HashMap<>();

    /**
     * <b>Play that settles something.</b> Two colts or stallions who already know
     * each other square up: face, close, rear in turn, shove. After a few seconds
     * one yields - by {@link HerdRules#winChance} - backs off a few blocks, and both
     * ledgers record who yielded. No damage, ever.
     */
    public static final class Spar extends Goal {
        private final Horse horse;
        private Horse partner;
        private int beat;
        private int checkCooldown;

        Spar(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!free(horse) || horse.isBaby() || HorseRecords.of(horse).sex() != Sex.MALE
                    || !(horse.level() instanceof ServerLevel level)) {
                return false;
            }
            Bout bout = BOUTS.get(horse.getUUID());
            if (bout != null) {
                partner = loaded(horse, bout.a().equals(horse.getUUID()) ? bout.b() : bout.a());
                return partner != null && level.getGameTime() < bout.ends();
            }
            if (--checkCooldown > 0) {
                return false;
            }
            checkCooldown = HerdSocialHandler.SCAN;
            if (horse.getRandom().nextDouble() >= HerdRules.perScan(HerdRules.SPARS_PER_PAIR_PER_DAY, HerdSocialHandler.SCAN)) {
                return false;
            }
            for (Horse other : level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(10.0),
                    h -> h != horse && !h.isBaby() && free(h) && !BOUTS.containsKey(h.getUUID())
                            && HorseRecords.hasRealRecord(h) && HorseRecords.of(h).sex() == Sex.MALE
                            && h.isTamed() == horse.isTamed())) {
                double familiarity = BandLife.relationship(horse, other).map(Relationship::familiarity).orElse(0.0);
                if (familiarity >= 0.3) {
                    long ends = level.getGameTime() + 80 + horse.getRandom().nextInt(60);
                    Bout b = new Bout(horse.getUUID(), other.getUUID(), ends);
                    BOUTS.put(horse.getUUID(), b);
                    BOUTS.put(other.getUUID(), b);
                    partner = other;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return partner != null && partner.isAlive() && BOUTS.containsKey(horse.getUUID()) && free(horse);
        }

        @Override
        public void start() {
            beat = 0;
        }

        @Override
        public void tick() {
            if (partner == null || !(horse.level() instanceof ServerLevel level)) {
                return;
            }
            Bout bout = BOUTS.get(horse.getUUID());
            horse.getLookControl().setLookAt(partner, 30.0F, 30.0F);
            if (bout == null) {
                return;
            }
            if (level.getGameTime() >= bout.ends()) {
                // Settled once, by whichever of the pair ticks first.
                BOUTS.remove(bout.a());
                BOUTS.remove(bout.b());
                boolean iWin = horse.getRandom().nextDouble() < HerdRules.winChance(
                        BandLife.contestant(horse, partner, level.getGameTime()),
                        BandLife.contestant(partner, horse, level.getGameTime()));
                Horse winner = iWin ? horse : partner;
                Horse loser = iWin ? partner : horse;
                BandLife.settle(winner, loser, HerdRules.STAKES_SPAR, level.getGameTime());
                BandLife.retreat(loser, winner, 6.0);
                ActionTrace.log("herd", ActionTrace.describeShort(loser) + " yielded to "
                        + ActionTrace.describeShort(winner) + " in a spar");
                return;
            }
            if (horse.distanceToSqr(partner) > 6.25) {
                horse.getNavigation().moveTo(partner, 1.0);
            } else {
                horse.getNavigation().stop();
            }
            // Rear in turn, and shove: the two halves of neck-wrestling a horse model can show.
            if (++beat % 20 == (bout.a().equals(horse.getUUID()) ? 0 : 10)) {
                horse.standIfPossible();
                Vec3 push = partner.position().subtract(horse.position());
                if (push.lengthSqr() > 1.0e-4 && horse.distanceToSqr(partner) < 9.0) {
                    partner.knockback(0.25, -push.x, -push.z);
                }
            }
        }

        @Override
        public void stop() {
            partner = null;
            horse.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------
    // Dam and foal
    // ------------------------------------------------------------------

    /**
     * A foal stays near its mother, and a mother goes to her foal. A wild foal
     * with no known dam adopts the nearest mare of its band. If something hurts the
     * foal, its dam goes for it - a wild dam for anything, a tamed one for anything
     * but a player.
     */
    public static final class DamAndFoal extends Goal {
        private final Horse horse;
        private Horse other;
        private int checkCooldown;

        DamAndFoal(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!free(horse) || !(horse.level() instanceof ServerLevel level) || --checkCooldown > 0) {
                return false;
            }
            checkCooldown = 20;
            HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
            if (horse.isBaby()) {
                if (social.dam().isEmpty()) {
                    adoptDam(level, social);
                    return false;
                }
                Horse dam = loaded(horse, social.dam().get());
                if (dam != null && horse.distanceToSqr(dam) > 36.0 && horse.distanceToSqr(dam) < 48.0 * 48.0) {
                    other = dam;
                    return true;
                }
                return false;
            }
            if (HorseRecords.of(horse).sex() != Sex.FEMALE) {
                return false;
            }
            for (Horse foal : level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(32.0),
                    h -> h.isBaby() && h.isAlive()
                            && h.getData(ModAttachments.HORSE_SOCIAL.get()).dam().map(horse.getUUID()::equals).orElse(false))) {
                LivingEntity attacker = foal.getLastHurtByMob();
                if (attacker != null && attacker.isAlive() && foal.tickCount - foal.getLastHurtByMobTimestamp() < 100
                        && (!horse.isTamed() || !(attacker instanceof Player))) {
                    horse.setTarget(attacker);      // the melee goal takes it from here
                    return false;
                }
                if (horse.distanceToSqr(foal) > 144.0) {
                    other = foal;
                    return true;
                }
            }
            return false;
        }

        private void adoptDam(ServerLevel level, HorseSocialAttachment social) {
            HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
            if (horse.isTamed() || !care.inWildHerd()) {
                return;
            }
            UUID herd = care.herd().get();
            Horse best = null;
            for (Horse m : HerdSocialHandler.bandMembers(level, herd, horse)) {
                if (!m.isBaby() && HorseRecords.of(m).sex() == Sex.FEMALE
                        && (best == null || m.distanceToSqr(horse) < best.distanceToSqr(horse))) {
                    best = m;
                }
            }
            if (best != null) {
                horse.setData(ModAttachments.HORSE_SOCIAL.get(), social.withDam(Optional.of(best.getUUID())));
            }
        }

        @Override
        public boolean canContinueToUse() {
            return other != null && other.isAlive() && free(horse) && horse.distanceToSqr(other) > 9.0;
        }

        @Override
        public void tick() {
            if (other != null && horse.getNavigation().isDone()) {
                horse.getNavigation().moveTo(other, horse.isBaby() ? 1.15 : 1.0);
            }
        }

        @Override
        public void stop() {
            other = null;
            horse.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------
    // The band stallion
    // ------------------------------------------------------------------

    /**
     * <b>He defends mares, not ground.</b> A wild band stallion keeps to the edge of
     * his band. A mare that strays too far from the others is <i>herded</i>: he comes
     * round behind her and drives her back. An outside stallion who comes close gets
     * a stallion between him and the mares, and if he keeps coming, a short chase.
     */
    public static final class StallionGuard extends Goal {
        private static final double STRAY = 14.0;
        private static final double INTRUDER = 16.0;

        private final Horse horse;
        private Horse subject;
        private boolean herding;
        private int checkCooldown;

        StallionGuard(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (horse.isTamed() || !free(horse) || !(horse.level() instanceof ServerLevel level)
                    || --checkCooldown > 0) {
                return false;
            }
            checkCooldown = 40;
            HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
            if (!HerdSocialHandler.isBandStallion(horse, care)) {
                return false;
            }
            UUID herd = horse.getUUID();
            List<Horse> band = HerdSocialHandler.bandMembers(level, herd, horse);
            if (band.size() < 2) {
                return false;
            }
            Vec3 centre = centre(band);
            for (Horse h : level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(INTRUDER),
                    h -> h != horse && h.isAlive() && !h.isTamed() && !h.isBaby()
                            && HorseRecords.hasRealRecord(h) && HorseRecords.of(h).sex() == Sex.MALE
                            && !h.getData(ModAttachments.HORSE_CARE.get()).herd().map(herd::equals).orElse(false))) {
                subject = h;
                herding = false;
                return true;
            }
            for (Horse m : band) {
                if (m != horse && !m.isBaby() && HorseRecords.of(m).sex() == Sex.FEMALE
                        && m.position().distanceToSqr(centre) > STRAY * STRAY) {
                    subject = m;
                    herding = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return subject != null && subject.isAlive() && free(horse) && horse.distanceToSqr(subject) < 48.0 * 48.0;
        }

        @Override
        public void tick() {
            if (subject == null || !(horse.level() instanceof ServerLevel level)) {
                return;
            }
            horse.getLookControl().setLookAt(subject, 30.0F, 30.0F);
            List<Horse> band = HerdSocialHandler.bandMembers(level, horse.getUUID(), horse);
            Vec3 centre = centre(band);
            if (herding) {
                // Behind the mare, on the side away from the band, then push her in.
                Vec3 out = subject.position().subtract(centre);
                Vec3 behind = subject.position().add(out.lengthSqr() < 1.0e-4 ? Vec3.ZERO : out.normalize().scale(3.0));
                horse.getNavigation().moveTo(behind.x, behind.y, behind.z, 1.25);
                if (horse.distanceToSqr(subject) < 16.0) {
                    subject.getNavigation().moveTo(centre.x, centre.y, centre.z, 1.15);
                    BandLife.threat(level, horse);
                }
                if (subject.position().distanceToSqr(centre) < 36.0) {
                    subject = null;     // she is back
                }
            } else {
                // Between the intruder and the mares; if he keeps coming, run at him.
                double intruderToCentre = subject.position().distanceTo(centre);
                if (intruderToCentre < 8.0) {
                    horse.getNavigation().moveTo(subject, 1.35);
                    if (horse.distanceToSqr(subject) < 9.0) {
                        BandLife.threat(level, horse);
                        horse.standIfPossible();
                        if (!BandLife.inFight(subject)) {
                            BandLife.retreat(subject, horse, 12.0);
                        }
                    }
                } else {
                    Vec3 between = centre.add(subject.position().subtract(centre).scale(0.35));
                    horse.getNavigation().moveTo(between.x, between.y, between.z, 1.1);
                }
                if (intruderToCentre > INTRUDER + 8.0) {
                    subject = null;     // he has gone
                }
            }
        }

        @Override
        public void stop() {
            subject = null;
            horse.getNavigation().stop();
        }

        private static Vec3 centre(List<Horse> band) {
            double x = 0, y = 0, z = 0;
            for (Horse h : band) {
                x += h.getX();
                y += h.getY();
                z += h.getZ();
            }
            int n = Math.max(1, band.size());
            return new Vec3(x / n, y / n, z / n);
        }
    }

    // ------------------------------------------------------------------
    // Displacement
    // ------------------------------------------------------------------

    /**
     * <b>Rank at the hay and the water.</b> A horse standing at food or water, with a
     * horse it clearly outranks next to it, walks in and takes the spot: a threat
     * over its head, and the other steps away. Resource competition as it really
     * is - mostly a shoulder, almost never a kick.
     */
    public static final class Displace extends Goal {
        private final Horse horse;
        private Horse subject;
        private int checkCooldown;

        Displace(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!free(horse) || horse.isBaby() || !(horse.level() instanceof ServerLevel level) || --checkCooldown > 0) {
                return false;
            }
            checkCooldown = 60;
            HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
            if (social.ledger().isEmpty()) {
                return false;
            }
            for (Horse other : level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(6.0),
                    h -> h != horse && h.isAlive() && !h.isVehicle() && HorseRecords.hasRealRecord(h))) {
                if (social.ledger().rankOver(other.getUUID()) >= HerdRules.DISPLACE_MARGIN
                        && atFoodOrWater(level, other.blockPosition())) {
                    subject = other;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return subject != null && subject.isAlive() && free(horse) && horse.distanceToSqr(subject) < 100.0;
        }

        @Override
        public void tick() {
            if (subject == null || !(horse.level() instanceof ServerLevel level)) {
                return;
            }
            horse.getNavigation().moveTo(subject, 1.05);
            if (horse.distanceToSqr(subject) < 5.0) {
                BandLife.threat(level, horse);
                BandLife.retreat(subject, horse, 5.0);
                BandLife.settle(horse, subject, HerdRules.STAKES_DISPLACEMENT, level.getGameTime());
                subject = null;
                checkCooldown = 600;    // one shove, then graze
            }
        }

        @Override
        public void stop() {
            subject = null;
            horse.getNavigation().stop();
        }

        private static boolean atFoodOrWater(ServerLevel level, BlockPos at) {
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                BlockState st = level.getBlockState(p);
                if (st.is(HorseCareHandler.HORSE_FOOD) || st.is(HorseCareHandler.HORSE_WATER)
                        || st.getFluidState().is(FluidTags.WATER)) {
                    // Grass counts as food in that tag, and every field is grass -
                    // so a plain grass block is not a spot worth taking.
                    if (!st.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            && !st.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                            && !st.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Grooming and resting
    // ------------------------------------------------------------------

    /**
     * Grooming partners, now and then, walk up to each other and stand head to tail -
     * the way resting horses keep the flies off each other's faces - with a little
     * sign of contentment. Mostly for the look of a band at rest.
     */
    public static final class GroomAndRest extends Goal {
        private final Horse horse;
        private Horse partner;
        private int remaining;
        private int checkCooldown;

        GroomAndRest(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!free(horse) || --checkCooldown > 0) {
                return false;
            }
            checkCooldown = HerdSocialHandler.SCAN;
            if (horse.getRandom().nextDouble() >= HerdRules.perScan(HerdRules.GROOMS_PER_PAIR_PER_DAY, HerdSocialHandler.SCAN)) {
                return false;
            }
            Optional<Relationship> r = horse.getData(ModAttachments.HORSE_SOCIAL.get()).ledger().groomingPartner();
            if (r.isEmpty()) {
                return false;
            }
            Horse p = loaded(horse, r.get().other());
            if (p == null || horse.distanceToSqr(p) > 144.0 || !free(p)) {
                return false;
            }
            partner = p;
            remaining = 120 + horse.getRandom().nextInt(120);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return partner != null && partner.isAlive() && free(horse) && remaining > 0
                    && horse.distanceToSqr(partner) < 196.0;
        }

        @Override
        public void tick() {
            if (partner == null || !(horse.level() instanceof ServerLevel level)) {
                return;
            }
            remaining--;
            if (horse.distanceToSqr(partner) > 4.0) {
                horse.getNavigation().moveTo(partner, 0.9);
                return;
            }
            horse.getNavigation().stop();
            // Head to tail: face the opposite way to the partner.
            horse.setYRot(partner.getYRot() + 180.0F);
            horse.setYBodyRot(horse.getYRot());
            if (remaining % 40 == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + horse.getBbHeight(),
                        horse.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
            }
        }

        @Override
        public void stop() {
            partner = null;
            horse.getNavigation().stop();
        }
    }
}
