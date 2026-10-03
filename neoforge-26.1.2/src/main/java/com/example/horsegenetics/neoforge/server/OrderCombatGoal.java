package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseOrderAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * <b>The combat orders' quarry</b>: Hunt monsters and Defend me (command whistle, Piece 3),
 * and Guard here (Piece 2) - which is Hunt monsters with {@code orders.guard_radius} as its
 * reach, centred on its spot like a hunter's.
 * A TARGET goal - it only ever picks or drops the horse's target. The chase and the swing
 * are {@link HorseMeleeGoal}'s, as for every other fight a horse has, and the way back
 * is {@link OrderStayGoal} (a hunter's spot) or {@link OrderFollowGoal} (a defender's
 * player), which {@code HorseMeleeGoal} at 3 out-ranks while there is something to hit.
 *
 * <h2>Leashed pursuit, and only for an order</h2>
 * The aggression locus holds a radius and never chases (wiki/gene-aggression.html). The
 * owner reopened that for orders only, on 2026-10-03: an ordered horse may go after a
 * monster, but only one inside the leash - within {@code orders.hunt_radius} of its spot,
 * or {@code orders.defend_radius} of its player - and it lets go once the quarry or the
 * horse drifts {@link HorseOrders#LEASH_SLACK_BLOCKS} past that, or once it is hurt below
 * {@code orders.break_off_health}. The rules are {@link HorseOrders.Leash}.
 *
 * <h2>What it may pick</h2>
 * A monster ({@link MobGroups#isHostile}), never a player, never one in the
 * {@link #NEVER_TARGET} tag (creepers: a horse that kicks a creeper is a hole in the
 * ground), never one passification vetoes, and only one the horse can see, as the
 * aggression locus does. Never anybody's animal either: a tamed zombie or skeleton horse
 * counts as a monster by category, and a stablemate is not quarry. And only one the
 * horse may attack at all ({@code canAttack}): {@code Mob.getTarget()} answers null for
 * any other, so a goal that picked one would pick it again every second and never fight
 * (found by the order_hunt_picks_a_monster gametest, whose husk was invulnerable). Scanned on the locus's beat with its cap, from the goal of the
 * ordered horse itself - never from a damage event, which is the mistake
 * {@code GeneReactionHandler}'s owner-hurt note records.
 *
 * <h2>It adds, never calms</h2>
 * It drops only the target it picked. A horse whose genes made it go for something else
 * keeps doing so, under any order; this goal does not start while the horse already has
 * a live target, and stands down rather than overwrite one.
 *
 * <p>Paused, not cleared, while ridden or on a lead, like every order goal.
 *
 * <p>Priority {@link #PRIORITY} in the TARGET selector. The only other target goal a horse
 * has, {@code WildHorseForgetTargetGoal} (1), runs for untamed horses alone, and only
 * tamed horses take orders, so the two never meet (read in HorseAggroHandler, which
 * settles the treatment's UNVERIFIED 5: nothing clears a tamed horse's target by itself).
 */
public final class OrderCombatGoal extends Goal {

    public static final int PRIORITY = 2;

    /** Monsters no combat order ever picks. Packs may add to it; it holds minecraft:creeper. */
    public static final TagKey<EntityType<?>> NEVER_TARGET = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "command_never_target"));

    private final Horse horse;
    private LivingEntity quarry;

    /** The genetic code {@link #fighter} was last worked out for, so it is parsed once per horse. */
    private String fighterFor;
    private boolean fighter;

    public OrderCombatGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // The beat first, staggered by entity id: a scan a second per ordered horse.
        if ((horse.tickCount + horse.getId()) % HorseOrders.COMBAT_SCAN_TICKS != 0) {
            return false;
        }
        HorseOrderAttachment o = ordered();
        if (o == null) {
            return false;
        }
        LivingEntity current = horse.getTarget();
        if (current != null && current.isAlive()) {
            return false; // already fighting something - its genes', or ours from a moment ago
        }
        Vec3 centre = centre(o);
        if (centre == null) {
            return false;
        }
        HorseOrders.Leash leash = ServerConfig.orderLeash(o.order());
        if (leash.breaksOff(horse.getHealth(), horse.getMaxHealth())) {
            return false;
        }
        quarry = pick(centre, leash);
        return quarry != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (quarry == null || !quarry.isAlive() || horse.getTarget() != quarry) {
            return false;
        }
        HorseOrderAttachment o = ordered();
        Vec3 centre = o == null ? null : centre(o);
        if (centre == null) {
            return false;
        }
        return ServerConfig.orderLeash(o.order()).keepsFighting(quarry.distanceToSqr(centre),
                horse.distanceToSqr(centre), horse.getHealth(), horse.getMaxHealth());
    }

    @Override
    public void start() {
        horse.setTarget(quarry);
    }

    @Override
    public void stop() {
        // Only what this goal picked: a target the genes chose in the meantime is theirs.
        if (quarry != null && horse.getTarget() == quarry) {
            horse.setTarget(null);
            horse.getNavigation().stop();
        }
        quarry = null;
    }

    /** The combat order standing on a horse free to carry it out, or null. */
    private HorseOrderAttachment ordered() {
        if (horse.isVehicle() || horse.isLeashed() || !horse.isAlive()) {
            return null;
        }
        HorseOrderAttachment o = HorseOrdering.current(horse);
        if (!o.order().combat() || !fighter()) {
            return null;
        }
        return o;
    }

    /**
     * Re-checked here and not only when the order was given: an order outlives a change
     * to the horse's record (a gene carrot), and the gate is the genes, not the moment.
     */
    private boolean fighter() {
        var record = HorseRecords.of(horse);
        String code = record == null ? null : record.geneticCode() + "|" + record.epigenomeCode();
        if (code != null && !code.equals(fighterFor)) {
            fighterFor = code;
            fighter = HorseOrdering.fighter(record);
        }
        return code != null && fighter;
    }

    /** The middle of the leash: the hunter's or guard's spot, or the defender's player if they are here. */
    private Vec3 centre(HorseOrderAttachment o) {
        if (o.order() == HorseOrder.DEFEND_ME) {
            ServerPlayer p = HorseOrdering.orderedByHere(horse, o);
            return p == null || p.level() != horse.level() ? null : p.position();
        }
        if (o.anchor().isEmpty()) {
            return null;
        }
        BlockPos a = o.anchor().get();
        return new Vec3(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
    }

    /** The nearest monster to the centre that the leash allows and the horse can see. */
    private LivingEntity pick(Vec3 centre, HorseOrders.Leash leash) {
        AABB box = new AABB(centre, centre).inflate(leash.radius());
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        int considered = 0;
        for (LivingEntity candidate : horse.level().getEntitiesOfClass(LivingEntity.class, box,
                c -> c != horse && c.isAlive() && eligible(c))) {
            if (++considered > HorseOrders.COMBAT_MAX_TARGETS) {
                break;
            }
            double d = candidate.distanceToSqr(centre);
            if (d < bestD && leash.mayEngage(d, horse.getHealth(), horse.getMaxHealth())
                    && horse.hasLineOfSight(candidate)) {
                bestD = d;
                best = candidate;
            }
        }
        return best;
    }

    private boolean eligible(LivingEntity candidate) {
        return !(candidate instanceof Player)
                && !(candidate instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerReference() != null)
                && !(candidate instanceof net.minecraft.world.entity.animal.equine.AbstractHorse h && h.isTamed())
                && MobGroups.isHostile(candidate)
                && horse.canAttack(candidate)
                && !candidate.getType().builtInRegistryHolder().is(NEVER_TARGET)
                && !Passification.suppresses(horse, candidate);
    }
}
