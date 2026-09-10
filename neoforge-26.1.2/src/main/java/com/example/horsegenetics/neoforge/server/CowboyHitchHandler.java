package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * A <b>Cowboy Hitch</b> turns the unemployed villager standing by it into a
 * {@link Cowboy}.
 *
 * <h2>Why this is not a job site</h2>
 * Because a cowboy is an <b>entity</b>, not a profession. They have a herd, a stock
 * target and a merchant screen that sells papers rather than goods, and none of
 * that fits on a {@code Villager} - so there is nothing for a profession to
 * point at and no POI for one to claim. The hitch is just a block, and this
 * looks for the block.
 *
 * <p>That is also why there are <b>two</b> posts now. One block tried to hand
 * out both trades by alternating, and it needed the villager it converted to
 * release their job-site ticket - which a discarded villager never does, so the
 * post produced one cowboy and then nothing for ever. A post each is far less
 * clever and simply works: the hitch makes cowboys, the
 * {@linkplain com.example.horsegenetics.neoforge.village.ModPoiTypes table}
 * makes horsemen, and neither has to know about the other.
 *
 * <h2>One cowboy per hitch</h2>
 * Guarded by looking, not by remembering: a hitch with a cowboy already within
 * {@link #ONE_PER} blocks is spoken for. That is self-correcting - kill them and
 * the next villager to wander past the hitch takes over - where a "used" flag on
 * the block would need saving and would go stale the moment they died.
 *
 * <p>The villager is <b>consumed</b>, which is safe here for exactly the reason
 * it was not safe at the old shared post: they hold no ticket on the hitch,
 * because the hitch is not a job site and they never claimed it.
 */
@EventBusSubscriber
public final class CowboyHitchHandler {

    /** How near a villager has to be standing to a hitch to be taken on. */
    private static final int REACH = 6;

    /** A hitch with a cowboy this close already has one. */
    private static final int ONE_PER = 32;

    /** Ticks between looks. A villager taking a job is not urgent. */
    private static final int INTERVAL = 40;

    private CowboyHitchHandler() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel level)
                || villager.isRemoved()
                || villager.isBaby()
                || villager.tickCount % INTERVAL != 0) {
            return;
        }
        // Only the unemployed: a horseman standing next to a hitch is somebody
        // else's, and a farmer has a farm to get back to.
        if (!villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            return;
        }
        BlockPos hitch = freeHitchNear(level, villager);
        if (hitch != null) {
            takeHim(level, villager, hitch);
        }
    }

    /**
     * A hitch within {@link #REACH} of this villager that has no cowboy on it.
     *
     * <p>A small block scan, run once every two seconds and only for villagers
     * with no job at all, which in a village is a handful at most. Cheap enough
     * not to need the POI index - and using the POI index would mean registering
     * the hitch as a point of interest, which is the thing this design is
     * avoiding.
     */
    private static @Nullable BlockPos freeHitchNear(ServerLevel level, Villager villager) {
        BlockPos at = villager.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH, -3, -REACH), at.offset(REACH, 3, REACH))) {
            if (!level.getBlockState(pos).is(ModBlocks.COWBOY_HITCH.get())) {
                continue;
            }
            if (level.getEntitiesOfClass(Cowboy.class, new AABB(pos).inflate(ONE_PER)).isEmpty()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Stand a cowboy up where the villager was, and take the villager off the board. */
    private static void takeHim(ServerLevel level, Villager villager, BlockPos hitch) {
        Cowboy cowboy = ModEntities.COWBOY.get().create(level, EntitySpawnReason.EVENT);
        if (cowboy == null) {
            return;
        }
        cowboy.snapTo(villager.getX(), villager.getY(), villager.getZ(),
                villager.getYRot(), villager.getXRot());
        villager.discard();
        level.addFreshEntity(cowboy);
        DebugAnnounce.sayAt(level, "Cowboy", "a villager took the hitch and became a cowboy",
                hitch, ChatFormatting.YELLOW);
    }
}
