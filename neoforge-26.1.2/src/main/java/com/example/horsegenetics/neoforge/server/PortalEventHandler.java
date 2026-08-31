package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime behaviour for hay-bale portals ({@link HorsePortalManager} owns the
 * geometry side).
 *
 * <ul>
 *   <li><b>Golden carrot on a hay bale</b> (outside the debug dim): light a frame.</li>
 *   <li><b>Standing in a portal:</b> 10 s teleports a player, 3 s teleports a
 *       horse. Both directions. While the timer runs, portal particles swirl
 *       around the entity and a player sees a per-second countdown in chat.</li>
 *   <li><b>Right-click a portal while leading horses:</b> each leashed horse is
 *       nudged into the portal, its lead pops off and drops, and it teleports
 *       on the 3 s timer.</li>
 *   <li><b>Leaving the debug dim</b> (dimension change / logout): the player's
 *       private plot is torn down.</li>
 *   <li><b>Logging in inside the debug dim</b> with no plot (server restarted):
 *       bounced to the overworld spawn so you don't fall through the void.</li>
 * </ul>
 */
@EventBusSubscriber
public final class PortalEventHandler {

    private static final int PLAYER_DWELL_TICKS = 200; // 10 s
    private static final int HORSE_DWELL_TICKS = 60;   // 3 s
    private static final int POST_TELEPORT_GRACE = 100;

    /** How long a horse teleported back to the overworld is invulnerable, to survive the drop. */
    static final int RETURN_INVULN_TICKS = 200; // 10 s

    private static final Map<Integer, Integer> DWELL = new HashMap<>();
    private static final Map<Integer, Integer> COOLDOWN = new HashMap<>();

    /** Last whole-second countdown value shown to each entity, to avoid re-sending the same line. */
    private static final Map<Integer, Integer> LAST_COUNTDOWN = new HashMap<>();

    /** Entity id -> ticks of post-return invulnerability left (returning horses only). */
    private static final Map<Integer, Integer> RETURN_INVULN = new HashMap<>();

    /**
     * Called by {@link HorsePortalManager#placeReturningHorse}: flag the entity
     * invulnerable now and remember to clear it in {@value #RETURN_INVULN_TICKS}
     * ticks. Also parks it in the teleport cooldown for the same window so the
     * dwell timer can't immediately yank it back through the portal it landed on.
     */
    static void grantReturnInvulnerability(Entity entity) {
        entity.setInvulnerable(true);
        RETURN_INVULN.put(entity.getId(), RETURN_INVULN_TICKS);
        COOLDOWN.put(entity.getId(), RETURN_INVULN_TICKS);
    }

    // --- golden-carrot lighting + roped-horse shortcut ---

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack held = event.getItemStack();
        Player player = event.getEntity();

        boolean inDebugDim = level.dimension().equals(DebugPenManager.DEBUG_LEVEL);

        if (state.is(Blocks.HAY_BLOCK) && held.is(Items.GOLDEN_CARROT) && !inDebugDim) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (HorsePortalManager.tryLightPortal(level, pos)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
                player.sendSystemMessage(Component.literal("The hay-bale portal flares open."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "No valid hay-bale frame here - build a vertical rectangle of hay bales "
                                + "(inside 2-21 wide, 3-21 tall) and click a frame block with a golden carrot."));
            }
            return;
        }

        if (state.getBlock() instanceof HayPortalBlock) {
            List<Mob> led = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12.0),
                    m -> m.isLeashed() && m.getLeashHolder() == player);
            if (!led.isEmpty()) {
                for (Mob mob : led) {
                    mob.snapTo(pos.getX() + 0.5, (double) pos.getY(), pos.getZ() + 0.5, mob.getYRot(), 0.0F);
                    mob.dropLeash();                       // lead pops off, drops as an item
                    DWELL.put(mob.getId(), HORSE_DWELL_TICKS - 1); // teleports on the next portal tick
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    // --- dwell timers ---

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        boolean isPlayer = entity instanceof ServerPlayer;
        if (!isPlayer && !(entity instanceof AbstractHorse)) return;

        int id = entity.getId();

        Integer invuln = RETURN_INVULN.get(id);
        if (invuln != null) {
            if (invuln <= 1) {
                RETURN_INVULN.remove(id);
                entity.setInvulnerable(false);
            } else {
                RETURN_INVULN.put(id, invuln - 1);
            }
        }

        Integer cd = COOLDOWN.get(id);
        if (cd != null) {
            if (cd <= 1) {
                COOLDOWN.remove(id);
            } else {
                COOLDOWN.put(id, cd - 1);
            }
            DWELL.remove(id);
            LAST_COUNTDOWN.remove(id);
            return;
        }

        BlockPos at = portalBlockUnder(level, entity);
        if (at == null) {
            DWELL.remove(id);
            LAST_COUNTDOWN.remove(id);
            return;
        }

        int threshold = isPlayer ? PLAYER_DWELL_TICKS : HORSE_DWELL_TICKS;
        int dwell = DWELL.merge(id, 1, Integer::sum);

        // swirling portal particles that thicken as the timer runs down
        spawnPortalSwirl(level, entity, dwell, threshold);

        if (entity instanceof ServerPlayer p) {
            boolean leavingHorseDim = level.dimension().equals(DebugPenManager.DEBUG_LEVEL);
            if (dwell == 1) {
                p.sendSystemMessage(Component.literal(leavingHorseDim
                        ? "The hay-bale portal grabs hold. Stand still to leave the horse dimension - "
                                + "anything you leave behind here is lost forever, but every tamed horse "
                                + "comes back with you."
                        : "The hay-bale portal grabs hold. Stand still to be pulled through to the "
                                + "horse dimension."));
            }
            int secondsLeft = ceilDiv(threshold - dwell, 20);
            Integer shown = LAST_COUNTDOWN.get(id);
            if (secondsLeft >= 1 && secondsLeft <= 5 && (shown == null || shown != secondsLeft)) {
                LAST_COUNTDOWN.put(id, secondsLeft);
                p.sendSystemMessage(Component.literal("Portal → " + secondsLeft
                        + (secondsLeft == 1 ? " second..." : " seconds...")));
            }
        }

        if (dwell >= threshold) {
            DWELL.remove(id);
            LAST_COUNTDOWN.remove(id);
            COOLDOWN.put(id, POST_TELEPORT_GRACE);
            HorsePortalManager.teleportThroughPortal(entity, level, at);
        }
    }

    private static int ceilDiv(int a, int b) {
        return Math.max(0, (a + b - 1) / b);
    }

    /** Gold "dust" particle for the portal swirl (vanilla has no gold portal particle). */
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(0xFFD24A, 1.1F);
    private static final DustParticleOptions GOLD_DUST_BRIGHT = new DustParticleOptions(0xFFF0B0, 1.3F);

    /** Ring of gold particles around {@code entity}, denser the closer the teleport is. */
    private static void spawnPortalSwirl(ServerLevel level, Entity entity, int dwell, int threshold) {
        double progress = Math.min(1.0, (double) dwell / threshold);
        int count = 3 + (int) (progress * 9);
        double radius = 0.9 + progress * 0.5;
        double angleBase = entity.tickCount * 0.5;
        for (int i = 0; i < count; i++) {
            double a = angleBase + i * (Math.PI * 2.0 / count);
            double r = radius * (0.6 + level.getRandom().nextDouble() * 0.4);
            double x = entity.getX() + Math.cos(a) * r;
            double z = entity.getZ() + Math.sin(a) * r;
            double y = entity.getY() + level.getRandom().nextDouble() * 1.8;
            level.sendParticles(GOLD_DUST, x, y, z, 1,
                    -Math.cos(a) * 0.3, 0.08, -Math.sin(a) * 0.3, 0.0);
        }
        if (progress > 0.5) {
            level.sendParticles(GOLD_DUST_BRIGHT,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    (int) (progress * 6), 0.3, 0.5, 0.3, 0.02);
        }
    }

    private static BlockPos portalBlockUnder(ServerLevel level, Entity entity) {
        BlockPos feet = entity.blockPosition();
        if (level.getBlockState(feet).getBlock() instanceof HayPortalBlock) {
            return feet.immutable();
        }
        BlockPos mid = BlockPos.containing(entity.getX(), entity.getY() + 0.9, entity.getZ());
        if (level.getBlockState(mid).getBlock() instanceof HayPortalBlock) {
            return mid.immutable();
        }
        return null;
    }

    // --- plot lifecycle ---

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getFrom().equals(DebugPenManager.DEBUG_LEVEL)
                && !event.getTo().equals(DebugPenManager.DEBUG_LEVEL)
                && player.level().getServer() != null) {
            DebugPenManager.leave(player.level().getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getServer() != null) {
            DebugPenManager.leave(player.level().getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) return;
        if (player.level().getServer() == null) return;

        ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            BlockPos s = overworld.getRespawnData().pos();
            player.teleportTo(overworld, s.getX() + 0.5, s.getY(), s.getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        }
    }

    private PortalEventHandler() {
    }
}
