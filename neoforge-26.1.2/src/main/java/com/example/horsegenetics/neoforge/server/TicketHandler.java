package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.item.HoldingPenTicketItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.TicketItem;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.WallSignBlock;
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

    private TicketHandler() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof HoldingPenTicketItem) {
            if (!event.getLevel().isClientSide() && horse.level() instanceof ServerLevel level) {
                sendToPen(level, horse, event.getEntity(), stack);
            }
            consume(event);
            return;
        }
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

        Vec3 landing = landingSpot(target, stall.signPos(), horse);
        if (landing == null) {
            // Nothing is spent and nothing moves. A horse that quietly fails to
            // arrive is indistinguishable from a horse that was deleted, and
            // the version that guessed a spot instead of refusing put one
            // inside a wall, where it suffocated.
            say(player, "There is no room to stand in that stall - check the sign is still up and "
                    + "that the stall has a floor and two blocks of headroom.");
            return;
        }
        arrive(level, target, horse, landing);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        say(player, (stall.horseName().isBlank() ? "The horse" : stall.horseName()) + " is back in its stall.");
    }

    /**
     * <b>A holding pen ticket</b>: any horse the player owns, to the player's one
     * holding pen, from any world. The same checks and the same live landing as
     * a stall ticket, because a pen is a stall that belongs to a player instead
     * of a horse - see {@code HoldingPenSignItem}.
     */
    private static void sendToPen(ServerLevel level, Horse horse, Player player, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, "That is not your horse - tame it first.");
            return;
        }
        PenRecord pen = StallData.get(server).penOf(player.getUUID());
        if (pen == null) {
            say(player, "You have no holding pen yet. Hang a holding pen sign on a pen's wall first.");
            return;
        }
        ServerLevel target = server.getLevel(pen.dimension());
        if (target == null) {
            say(player, "Your holding pen's world is not loaded.");
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Get off first - a horse cannot travel with a rider.");
            return;
        }
        Vec3 landing = landingSpot(target, pen.signPos(), horse);
        if (landing == null) {
            say(player, "There is no room to stand in your holding pen - check the sign is still up and "
                    + "that the pen has a floor and two blocks of headroom.");
            return;
        }
        arrive(level, target, horse, landing);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "The horse";
        say(player, name + " is in your holding pen.");
    }

    /** Move the horse, with a puff and a sound at both ends so the player sees it go. */
    private static void arrive(ServerLevel from, ServerLevel target, Horse horse, Vec3 landing) {
        from.sendParticles(ParticleTypes.PORTAL, horse.getX(), horse.getY() + 0.8, horse.getZ(),
                24, 0.4, 0.6, 0.4, 0.2);
        from.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        horse.dropLeash();
        horse.teleportTo(target, landing.x, landing.y, landing.z,
                Set.of(), horse.getYRot(), horse.getXRot(), false);

        target.sendParticles(ParticleTypes.PORTAL, landing.x, landing.y + 0.8, landing.z,
                24, 0.4, 0.6, 0.4, 0.2);
        target.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
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
     * <b>Where in the stall to put the horse, or {@code null} if nowhere.</b>
     *
     * <p>The stall is measured <b>now</b>, not read back from the record. Two
     * reasons, and the first one cost a horse: the stored span is a bounding
     * box, and a box around any room that is not a plain cuboid contains the
     * walls inside it. The second is that a player rebuilds stalls - re-running
     * the detector means a stall that has been widened, floored or re-fenced
     * since the sign went up is the stall the horse arrives in.
     *
     * <p>The chunk is pulled in first. A horse teleported into unloaded terrain
     * is the failure that looks exactly like a horse that was deleted.
     */
    private static Vec3 landingSpot(ServerLevel level, BlockPos signPos, Horse horse) {
        level.getChunk(signPos); // load it, so what we read is real and the horse arrives somewhere
        BlockState sign = level.getBlockState(signPos);
        if (!(sign.getBlock() instanceof WallSignBlock)) {
            return null; // the sign is gone - so is the stall it was naming
        }
        Direction facing = sign.getValue(WallSignBlock.FACING);
        StallDetector.Result live =
                StallDetector.forSign(level, signPos.relative(facing.getOpposite()), facing);
        return StallDetector.landingSpot(level, live, horse);
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
