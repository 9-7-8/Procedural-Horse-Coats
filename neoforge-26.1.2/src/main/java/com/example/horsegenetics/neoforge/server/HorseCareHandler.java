package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseCareSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.HorseGenetics.MOD_ID;

/**
 * The <b>one slow tick</b> the roadmap insists gated healing (wiki 7.2) and
 * bond / herds (wiki 13) should share, because all three are proximity scans
 * with the same performance constraint and the horse dimension spawns horses by
 * the hundred. Runs every {@link #SCAN_INTERVAL} ticks, phase-staggered by
 * entity id, and does the expensive block scan <em>only</em> for a horse that is
 * actually hurt.
 *
 * <h4>Gated healing (wiki 7.2)</h4>
 * A hurt horse regenerates {@link #HEAL_AMOUNT} health per scan only while it is
 * within {@link #HEAL_SCAN_RADIUS} blocks of <b>both</b> a block in
 * {@code horsegenetics:horse_water} (or any water fluid) <b>and</b> a block in
 * {@code horsegenetics:horse_food}. Hand-feeding is untouched - vanilla still
 * heals a fed horse with no water requirement, so a stabled horse in a dry
 * biome is never unhealable. A horse in a herd heals at
 * {@link #HERD_HEAL_AMOUNT} (the herd comfort buff, restated as faster regen
 * since the mod has no stamina resource).
 *
 * <h4>Bond (wiki 13)</h4>
 * A tamed horse accrues bond toward {@link HorseCareAttachment#MAX_BOND},
 * capped at {@link HorseCareAttachment#DAILY_CAP} per Minecraft day
 * ({@link HorseCareAttachment#DAY_TICKS} game ticks): proximity to its owner
 * (about +0.5/min), being ridden by its owner (about +1/min), and being
 * hand-fed by its owner ({@link #FEED_BOND} at once). A foal in a herd gains at
 * double rate. {@link BondFollowGoal} reads the resulting tier.
 *
 * <h4>Herds (wiki 13)</h4>
 * A continuously-tracked counter ({@code togetherTicks}) accumulates while
 * another horse is within {@link #HERD_RADIUS} and decays otherwise; crossing
 * {@link HorseCareAttachment#TICKS_TO_FORM_HERD} joins the neighbours' herd (or
 * mints a new id and pulls in herdless neighbours), and decaying to zero leaves
 * it. Alpha is not stored - a consumer computes it on demand.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
@EventBusSubscriber
public final class HorseCareHandler {

    private HorseCareHandler() {
    }

    private static final int SCAN_INTERVAL = 30;
    private static final int HEAL_SCAN_RADIUS = 3;
    private static final int HERD_RADIUS = 10;
    private static final float HEAL_AMOUNT = 1.0F;
    private static final float HERD_HEAL_AMOUNT = 2.0F;
    private static final int FEED_BOND = 2;
    private static final int GOAL_PRIORITY = 4;

    public static final TagKey<Block> HORSE_WATER =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "horse_water"));
    public static final TagKey<Block> HORSE_FOOD =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "horse_food"));

    // ------------------------------------------------------------------
    // The bond behaviour goal, added once per horse.
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onHorseJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof BondFollowGoal) {
                return;
            }
        }
        horse.goalSelector.addGoal(GOAL_PRIORITY, new BondFollowGoal(horse));
        // Wild herd cohesion: a member trails its herd lead so a spawned pack
        // stays together and wanders as a unit (HerdManager sets the herd).
        horse.goalSelector.addGoal(GOAL_PRIORITY + 2, new WildHerdGoal(horse));
    }

    // ------------------------------------------------------------------
    // The shared slow tick.
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!horse.isAlive()) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN_INTERVAL != 0) {
            return;
        }

        HorseCareAttachment before = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseCareAttachment after = before;

        // --- herds: every horse ---
        List<AbstractHorse> nearbyHorses = level.getEntitiesOfClass(AbstractHorse.class,
                horse.getBoundingBox().inflate(HERD_RADIUS),
                h -> h != horse && h.isAlive());
        after = updateHerd(horse, after, nearbyHorses);

        // --- bond: tamed, owner loaded in this level ---
        LivingEntity ownerEntity = horse.getOwner();
        if (horse.isTamed() && ownerEntity instanceof Player owner && owner.level() == level) {
            long add = 0;
            if (horse.distanceToSqr(owner) <= 100.0) {   // ~10 blocks
                add += SCAN_INTERVAL / 2L;               // proximity ~ +0.5/min
            }
            if (horse.getControllingPassenger() == owner) {
                add += SCAN_INTERVAL;                     // riding ~ +1/min
            }
            if (add > 0) {
                long ticks = after.bondTicks() + add;
                int points = 0;
                while (ticks >= HorseCareAttachment.TICKS_PER_BOND_POINT) {
                    ticks -= HorseCareAttachment.TICKS_PER_BOND_POINT;
                    points++;
                }
                after = after.with(after.bond(), after.herd(), after.bondToday(),
                        after.dayStamp(), ticks, after.togetherTicks());
                if (points > 0) {
                    after = awardBond(level, horse, after, points);
                }
            }
        }

        // --- gated healing: only pay for the block scan if hurt ---
        if (horse.getHealth() < horse.getMaxHealth() && nearWaterAndFood(level, horse)) {
            horse.heal(after.inHerd() ? HERD_HEAL_AMOUNT : HEAL_AMOUNT);
            level.sendParticles(ParticleTypes.HEART,
                    horse.getX(), horse.getY() + horse.getBbHeight() * 0.7, horse.getZ(),
                    2, 0.35, 0.30, 0.35, 0.0);
        }

        if (!after.equals(before)) {
            horse.setData(ModAttachments.HORSE_CARE.get(), after);
            if (after.bond() != before.bond() || after.inHerd() != before.inHerd()) {
                syncCare(horse, after);
            }
        }
    }

    // ------------------------------------------------------------------
    // Feeding: immediate bond from the owner's hand.
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!horse.isTamed() || horse.getOwner() != event.getEntity()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !horse.isFood(stack)) {
            return;
        }

        HorseCareAttachment before = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseCareAttachment after = awardBond(level, horse, before, FEED_BOND);
        if (!after.equals(before)) {
            horse.setData(ModAttachments.HORSE_CARE.get(), after);
            syncCare(horse, after);
        }
        // deliberately not cancelled - vanilla feeding (heal / temper / love) proceeds
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static HorseCareAttachment awardBond(ServerLevel level, Horse horse,
                                                 HorseCareAttachment care, int amount) {
        long today = level.getGameTime() / HorseCareAttachment.DAY_TICKS;
        int bondToday = care.bondToday();
        long dayStamp = care.dayStamp();
        if (today != dayStamp) {
            dayStamp = today;
            bondToday = 0;
        }
        int room = HorseCareAttachment.DAILY_CAP - bondToday;
        if (room <= 0) {
            return care.with(care.bond(), care.herd(), bondToday, dayStamp,
                    care.bondTicks(), care.togetherTicks());
        }
        int effective = (care.inHerd() && horse.isBaby()) ? amount * 2 : amount;
        int grant = Math.min(effective, room);
        return care.with(care.bond() + grant, care.herd(), bondToday + grant, dayStamp,
                care.bondTicks(), care.togetherTicks());
    }

    private static HorseCareAttachment updateHerd(Horse horse, HorseCareAttachment care,
                                                  List<AbstractHorse> nearby) {
        // A natural wild herd is owned by HerdManager - the together-timer must
        // not dissolve it or re-home the horse.
        if (care.inWildHerd()) {
            return care;
        }
        long together = care.togetherTicks();
        Optional<UUID> herd = care.herd();

        if (!nearby.isEmpty()) {
            together = Math.min(together + SCAN_INTERVAL, HorseCareAttachment.TICKS_TO_FORM_HERD * 2);
        } else {
            together = Math.max(0, together - SCAN_INTERVAL);
        }

        if (together >= HorseCareAttachment.TICKS_TO_FORM_HERD && herd.isEmpty()) {
            Optional<UUID> existing = Optional.empty();
            for (AbstractHorse other : nearby) {
                Optional<UUID> h = other.getData(ModAttachments.HORSE_CARE.get()).herd();
                if (h.isPresent()) {
                    existing = h;
                    break;
                }
            }
            if (existing.isPresent()) {
                herd = existing;
            } else {
                UUID id = UUID.randomUUID();
                herd = Optional.of(id);
                for (AbstractHorse other : nearby) {
                    HorseCareAttachment oc = other.getData(ModAttachments.HORSE_CARE.get());
                    if (oc.herd().isEmpty()
                            && oc.togetherTicks() >= HorseCareAttachment.TICKS_TO_FORM_HERD / 2) {
                        HorseCareAttachment updated = oc.withHerd(Optional.of(id));
                        other.setData(ModAttachments.HORSE_CARE.get(), updated);
                        if (other instanceof Horse oh) {
                            syncCare(oh, updated);
                        }
                    }
                }
            }
        } else if (together == 0 && herd.isPresent()) {
            herd = Optional.empty();
        }

        return care.with(care.bond(), herd, care.bondToday(), care.dayStamp(),
                care.bondTicks(), together);
    }

    private static boolean nearWaterAndFood(ServerLevel level, Horse horse) {
        AABB box = horse.getBoundingBox().inflate(HEAL_SCAN_RADIUS);
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.floor(box.maxX);
        int y0 = Mth.floor(box.minY);
        int y1 = Mth.floor(box.maxY);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.floor(box.maxZ);
        boolean water = false;
        boolean food = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (water && food) {
                        return true;
                    }
                    BlockState st = level.getBlockState(p.set(x, y, z));
                    if (!water && (st.is(HORSE_WATER) || st.getFluidState().is(FluidTags.WATER))) {
                        water = true;
                    }
                    if (!food && st.is(HORSE_FOOD)) {
                        food = true;
                    }
                }
            }
        }
        return water && food;
    }

    private static void syncCare(Horse horse, HorseCareAttachment care) {
        PacketDistributor.sendToPlayersTrackingEntity(horse,
                new HorseCareSyncPayload(horse.getId(), care.bond(), care.inHerd()));
    }
}
