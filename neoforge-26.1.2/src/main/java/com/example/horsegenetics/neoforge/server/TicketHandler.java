package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.TicketItem;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Spending a ticket</b> - right-click your horse with one and it goes to its
 * stall. See {@link TicketItem} for what the tiers mean.
 *
 * <p>Same trap as every other horse interaction: {@code EntityInteract} fires on
 * both sides and vanilla turns any item used on a tamed horse into a mount, so
 * the event is cancelled on <b>both</b> sides and everything happens server-only
 * (see {@link HorseInteractionHandler}).
 *
 * <p>Every refusal says why. A ticket is a consumable a player crafted on
 * purpose, and one that silently does nothing is indistinguishable from a bug -
 * so nothing is spent unless the horse actually moved.
 */
@EventBusSubscriber
public final class TicketHandler {

    /** How far above the stall floor to look for headroom before giving up on a spot. */
    private static final int HEADROOM = 2;

    private TicketHandler() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof TicketItem ticket)) {
            // The blank is the crafting base and does nothing, but it should say
            // so rather than mounting the horse the player just poked.
            if (stack.is(ModItems.BLANK_TICKET.get())) {
                if (!event.getLevel().isClientSide()) {
                    say(event.getEntity(), "A blank ticket is not written on yet - craft it up first.");
                }
                consume(event);
            }
            return;
        }

        Player player = event.getEntity();
        if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
            send(level, horse, player, stack, ticket.tier());
        }
        consume(event);
    }

    private static void send(ServerLevel level, Horse horse, Player player, ItemStack stack,
                             TicketItem.Tier tier) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, "That is not your horse.");
            return;
        }
        StallRecord stall = StallData.get(server).forHorse(horse.getUUID());
        if (stall == null) {
            say(player, "This horse has no stall. Bind a stall sign to it and hang the sign up first.");
            return;
        }
        ServerLevel target = server.getLevel(stall.dimension());
        if (target == null) {
            say(player, "That stall's world is not loaded.");
            return;
        }
        if (!reaches(tier, level.dimension(), stall.dimension())) {
            say(player, refusal(tier, level.dimension(), stall.dimension()));
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Get off first - a horse cannot travel with a rider.");
            return;
        }

        BlockPos landing = landingSpot(target, stall);
        // A puff where it was, so the player sees the horse leave rather than
        // just noticing it has gone.
        level.sendParticles(ParticleTypes.PORTAL, horse.getX(), horse.getY() + 0.8, horse.getZ(),
                24, 0.4, 0.6, 0.4, 0.2);
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        horse.dropLeash();
        horse.teleportTo(target, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
                Set.of(), horse.getYRot(), horse.getXRot(), false);

        target.sendParticles(ParticleTypes.PORTAL, landing.getX() + 0.5, landing.getY() + 0.8,
                landing.getZ() + 0.5, 24, 0.4, 0.6, 0.4, 0.2);
        target.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        say(player, (stall.horseName().isBlank() ? "The horse" : stall.horseName()) + " is back in its stall.");
    }

    /** Does a ticket of this tier reach from {@code from} to {@code to}? */
    private static boolean reaches(TicketItem.Tier tier, net.minecraft.resources.ResourceKey<Level> from,
                                   net.minecraft.resources.ResourceKey<Level> to) {
        return switch (tier) {
            case BASIC -> from.equals(Level.OVERWORLD) && to.equals(Level.OVERWORLD);
            case BOUND -> from.equals(to);
            case INTERDIMENSIONAL -> true;
        };
    }

    private static String refusal(TicketItem.Tier tier, net.minecraft.resources.ResourceKey<Level> from,
                                  net.minecraft.resources.ResourceKey<Level> to) {
        if (tier == TicketItem.Tier.BASIC && from.equals(to)) {
            // Same world, but not the overworld - the next ticket up covers it.
            return "A basic ticket only works in the overworld. A bound ticket would do this.";
        }
        return "This horse's stall is in another world. Only an interdimensional ticket reaches it.";
    }

    /**
     * Where in the stall to put the horse: the middle of the floor if that is
     * standing room, otherwise the first cell in the stall that is.
     *
     * <p>The stall may be the detector's fallback box rather than a real room
     * (see {@link StallDetector}), so this cannot assume the volume is enclosed
     * or even that its middle is empty - it checks, and falls back to the sign
     * itself, which is the one block known to be somewhere the player stood.
     */
    private static BlockPos landingSpot(ServerLevel level, StallRecord stall) {
        BlockPos min = stall.min();
        BlockPos max = stall.max();
        BlockPos middle = new BlockPos(
                (min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
        if (standable(level, middle)) {
            return middle;
        }
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (standable(level, p)) {
                        return p;
                    }
                }
            }
        }
        return stall.signPos();
    }

    /** Room for a horse to stand: this cell and the ones above it are clear. */
    private static boolean standable(ServerLevel level, BlockPos pos) {
        for (int i = 0; i <= HEADROOM; i++) {
            BlockState state = level.getBlockState(pos.above(i));
            if (state.blocksMotion()) {
                return false;
            }
        }
        return true;
    }

    private static boolean ownedBy(Horse horse, UUID playerId) {
        if (!horse.isTamed()) {
            return false;
        }
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        return owner != null && playerId.equals(owner.getUUID());
    }

    private static void say(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
