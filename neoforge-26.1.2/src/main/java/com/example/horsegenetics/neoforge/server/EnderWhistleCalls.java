package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The server half of the <b>ender whistle</b>: binding it, calling the horse, and
 * turning it to dust.
 *
 * <h2>Calling a horse you cannot see</h2>
 * <ol>
 *   <li>If the horse is loaded in <i>any</i> level, it comes at once.</li>
 *   <li>Otherwise {@link HorseWhereabouts} says where it was last seen. That chunk
 *       gets a {@link TicketType#PORTAL} ticket - the one vanilla uses to pull
 *       something through a portal, and it expires on its own - and the call waits
 *       up to {@link #WAIT_TICKS} for the horse's entity section to load.</li>
 *   <li>If it never appears, the player is told the horse was not where it was last
 *       seen, and nothing else happens.</li>
 * </ol>
 * It moves an entity found by UUID and never creates one, so it cannot duplicate a
 * horse.
 *
 * <p><b>Not verified in-game</b>, and the ticket wait in particular is written
 * against 26.1.2 sources rather than seen working: entity sections load a little
 * after their chunk, which is why it waits at all.
 */
@EventBusSubscriber
public final class EnderWhistleCalls {

    private EnderWhistleCalls() {
    }

    /** Five seconds for a far chunk to load its horse. */
    private static final int WAIT_TICKS = 100;
    private static final int TICKET_RADIUS = 2;

    private record Pending(UUID player, BoundHorse horse, long deadline) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    // ------------------------------------------------------------------
    // Binding
    // ------------------------------------------------------------------

    /** Right-click one of your horses with an unbound ender whistle: bound, for good. */
    @SubscribeEvent
    static void onBind(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.ENDER_WHISTLE.get()) || !(event.getTarget() instanceof Horse horse)) {
            return;
        }
        if (stack.has(ModDataComponents.BOUND_HORSE.get())) {
            return;     // already bound - an ordinary click, it never rebinds
        }
        if (!event.getLevel().isClientSide()) {
            String name = HorseRecords.hasRealRecord(horse) ? HorseRecords.of(horse).displayName() : "That horse";
            String refusal = HorseOwnership.bindRefusal(horse, event.getEntity(), name);
            if (refusal != null) {
                event.getEntity().sendSystemMessage(Component.literal(refusal));
            } else {
                stack.set(ModDataComponents.BOUND_HORSE.get(), new BoundHorse(horse.getUUID(), name));
                if (horse.level() instanceof ServerLevel level) {
                    HorseWhereabouts.get(level.getServer())
                            .seen(horse.getUUID(), level.dimension(), horse.blockPosition());
                    level.playSound(null, horse.blockPosition(), SoundEvents.ENDER_EYE_DEATH,
                            SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                event.getEntity().sendSystemMessage(Component.literal(
                        "The whistle hums, and is bound to " + name + " for good."));
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ------------------------------------------------------------------
    // Dust
    // ------------------------------------------------------------------

    /**
     * If the bound horse is dead - or deleted outright, which the ancestry database
     * records by forgetting it - turn the whistle to dust in the player's hands.
     *
     * @return whether it crumbled
     */
    public static boolean crumbleIfGone(ServerPlayer player, ItemStack stack, BoundHorse bound) {
        MinecraftServer server = player.level().getServer();
        boolean dead = HorseWhereabouts.get(server).isDead(bound.id());
        boolean forgotten = HorseAncestryData.get(server).lookup(bound.id()).isEmpty();
        if (!dead && !forgotten) {
            return false;
        }
        stack.shrink(stack.getCount());
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.ASH, player.getX(), player.getEyeY() - 0.4, player.getZ(),
                30, 0.3, 0.2, 0.3, 0.02);
        player.sendSystemMessage(Component.literal("The soulbound whistle for " + nameOf(bound)
                + " crumbles to dust in your hands. " + nameOf(bound) + " is gone."));
        return true;
    }

    // ------------------------------------------------------------------
    // Calling
    // ------------------------------------------------------------------

    public static void call(ServerPlayer player, BoundHorse bound) {
        MinecraftServer server = player.level().getServer();
        Horse loaded = findLoaded(server, bound.id());
        if (loaded != null) {
            deliver(player, loaded, bound);
            return;
        }
        var seen = HorseWhereabouts.get(server).lookup(bound.id());
        ServerLevel there = seen.map(s -> server.getLevel(s.dimension())).orElse(null);
        if (seen.isEmpty() || there == null) {
            player.sendSystemMessage(Component.literal("The whistle echoes, but " + nameOf(bound)
                    + " has not been seen anywhere yet."));
            return;
        }
        BlockPos pos = seen.get().pos();
        there.getChunkSource().addTicketWithRadius(TicketType.PORTAL,
                new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), TICKET_RADIUS);
        PENDING.add(new Pending(player.getUUID(), bound, server.getTickCount() + WAIT_TICKS));
        player.sendSystemMessage(Component.literal("The whistle echoes a long way off..."));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(p.player());
            if (player == null) {
                it.remove();    // they logged off; the ticket expires on its own
                continue;
            }
            Horse horse = findLoaded(server, p.horse().id());
            if (horse != null) {
                it.remove();
                deliver(player, horse, p.horse());
            } else if (server.getTickCount() > p.deadline()) {
                it.remove();
                player.sendSystemMessage(Component.literal("Nothing answers. " + nameOf(p.horse())
                        + " was not where it was last seen."));
            }
        }
    }

    private static Horse findLoaded(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e instanceof Horse horse && horse.isAlive()) {
                return horse;
            }
        }
        return null;
    }

    /**
     * Bring it. The ownership check is here, at call time, not at bind time: a
     * transfer paper changes the owner without touching the whistle, and only the
     * horse's current owner may call it.
     */
    private static void deliver(ServerPlayer player, Horse horse, BoundHorse bound) {
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            player.sendSystemMessage(Component.literal(nameOf(bound) + " no longer answers to you."));
            return;
        }
        if (horse.isVehicle() && horse.getControllingPassenger() != player) {
            player.sendSystemMessage(Component.literal(nameOf(bound) + " has a rider, and stays put."));
            return;
        }
        if (horse == player.getVehicle()) {
            return;     // already under you
        }
        if (horse.isLeashed()) {
            horse.dropLeash();
        }
        horse.getNavigation().stop();

        ServerLevel from = (ServerLevel) horse.level();
        ServerLevel to = player.level();
        Vec3 spot = landing(player);
        from.sendParticles(ParticleTypes.PORTAL, horse.getX(), horse.getY() + 1.0, horse.getZ(),
                40, 0.5, 0.8, 0.5, 0.2);

        if (from == to) {
            horse.snapTo(spot.x, spot.y, spot.z, player.getYRot(), 0.0F);
            horse.setDeltaMovement(Vec3.ZERO);
            horse.fallDistance = 0.0;
        } else {
            // Across dimensions an entity is re-created in the target level, and
            // the returned entity is the one that now exists. Unverified in play.
            Entity moved = horse.teleport(new TeleportTransition(to, spot, Vec3.ZERO,
                    player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
            if (moved == null) {
                player.sendSystemMessage(Component.literal("The whistle sounds, but " + nameOf(bound)
                        + " could not come through."));
                return;
            }
            moved.fallDistance = 0.0;
        }

        to.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        to.sendParticles(ParticleTypes.PORTAL, spot.x, spot.y + 1.0, spot.z, 40, 0.5, 0.8, 0.5, 0.2);
        HorseProgress.complete(player, ProgressTask.WHISTLE_ENDER);
    }

    /** Two blocks in front of the player, nudged onto the ground. */
    private static Vec3 landing(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            flat = new Vec3(1, 0, 0);
        }
        BlockPos p = BlockPos.containing(player.position().add(flat.normalize().scale(2.5)));
        for (int i = 0; i < 3 && level.getBlockState(p.below()).isAir(); i++) {
            p = p.below();
        }
        for (int i = 0; i < 4 && !level.getBlockState(p).isAir(); i++) {
            p = p.above();
        }
        return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    private static String nameOf(BoundHorse bound) {
        return bound.name() == null || bound.name().isBlank() ? "Your horse" : bound.name();
    }
}
