package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import com.example.horsegenetics.neoforge.server.StallDebug;
import com.example.horsegenetics.neoforge.server.StallDetector;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The stall sign. A <b>blank</b> one ({@code stall_sign}) does nothing until you
 * right-click a horse you own with it (see {@code server/StallSignHandler}), which
 * turns it into a <b>bound</b> one ({@code bound_stall_sign}) carrying a
 * {@link BoundHorse} component.
 *
 * <p>Placing a bound sign on a stall wall, from either side ({@link #useOn}), drops
 * a real oak wall sign there with the horse's name on it and runs
 * {@link StallDetector} on both sides of the wall: if it finds an enclosed room,
 * that room becomes the horse's stall ({@link StallData}) and its outline is
 * flashed with particles ({@link StallDebug}). If it does not, the sign refuses.
 *
 * <h2>One sign per horse</h2>
 * A horse has one stall, so it has one sign. Owner, 2026-09-13: <i>"you shouldnt
 * be able to place more than one stall sign bound to a horse. trying to should
 * make it pop off-- trying again will pop off previous sign"</i>. Before that, a
 * second sign quietly moved the stall and left the first one on its wall, naming
 * a horse it no longer answered for.
 */
public class StallSignItem extends Item {

    /**
     * How long the "place it again to move the stall" offer stands, in server
     * ticks. Ten seconds: long enough to walk to another wall, short enough that a
     * sign placed much later is not a move the player has forgotten asking for.
     */
    private static final int CONFIRM_WINDOW_TICKS = 200;

    /** A player who has been told a horse already has a stall, and when. Server thread only. */
    private record PendingMove(UUID horse, int tick) {
    }

    private static final Map<UUID, PendingMove> PENDING_MOVES = new HashMap<>();

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public StallSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
        if (bound == null) {
            return InteractionResult.PASS; // blank sign - nothing to place
        }
        Level level = ctx.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            message(ctx, "Place the stall sign on a vertical wall face.");
            return InteractionResult.FAIL;
        }
        BlockPos wall = ctx.getClickedPos();
        BlockPos signPos = wall.relative(face);
        if (!level.getBlockState(signPos).canBeReplaced()) {
            message(ctx, "No room to place the sign there.");
            return InteractionResult.FAIL;
        }

        // Whichever side of the wall is a room - see StallDetector, which
        // measures the stall rather than passing judgement on it.
        StallDetector.Result r = StallDetector.forSign(level, wall, face);
        if (r == null) {
            // POP OFF RATHER THAN PLACE. Owner, 2026-09-13: "If it doesn't find
            // a good area, it should just pop off and refuse to place." A sign
            // that binds to a made-up box reports a stall the player does not
            // have, and they find out when a horse is standing in a wall.
            message(ctx, "That is not an enclosed stall - close it in on every side, with a gate "
                    + "or a door where you walk in, then try again.");
            return InteractionResult.FAIL;
        }

        String name = bound.name().isBlank() ? "(unnamed)" : bound.name();
        MinecraftServer server = level.getServer();
        StallData data = server == null ? null : StallData.get(server);
        StallRecord previous = data == null ? null : data.forHorse(bound.id());
        BlockPos poppedAt = null;
        if (previous != null) {
            // A SECOND SIGN FOR ONE HORSE. The first attempt refuses and says
            // where the existing stall is; placing again within the window moves
            // the stall here and pops the old sign off. The check is only for
            // enclosed rooms - a spot that is not a stall has already refused
            // above, and offering to move a stall into it would be a lie.
            if (!confirmMove(ctx.getPlayer(), bound.id(), server)) {
                BlockPos at = previous.signPos();
                message(ctx, name + " already has a stall - its sign is at " + at.getX() + ", "
                        + at.getY() + ", " + at.getZ() + ". Place this sign again to move the stall "
                        + "here; the old sign will pop off.");
                return InteractionResult.FAIL;
            }
            poppedAt = popOldSign(server, previous, bound);
        }

        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, face);
        level.setBlock(signPos, signState, Block.UPDATE_ALL);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            sign.updateText(t -> t
                    .setMessage(0, Component.literal("Stall"))
                    .setMessage(1, Component.literal(name)), true);
        }

        StallRecord record = new StallRecord(
                bound.id(), bound.name(), level.dimension(), signPos, r.min(), r.max(), r.blockCount());
        if (data != null) {
            data.assign(record);
        }
        // The task is "give a horse a stall", so it ticks when the horse has
        // one - not back when the sign was bound, which is a sign in a pocket.
        HorseProgress.complete(ctx.getPlayer(), ProgressTask.BUILD_STALL);

        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            StallDebug.showOne(sp, record);
            String size = r.blockCount() + " blocks, " + r.sizeX() + "x" + r.sizeY() + "x" + r.sizeZ() + ".";
            sp.sendSystemMessage(Component.literal(poppedAt == null
                    ? "Stall set for " + name + " - " + size
                    : "Stall moved for " + name + " - " + size + " The old sign popped off at "
                            + poppedAt.getX() + ", " + poppedAt.getY() + ", " + poppedAt.getZ() + "."));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Is this the second try at moving {@code horseId}'s stall, inside the window?
     * The first try records the offer and answers no; a second one for the same
     * horse answers yes and clears it. A try for a different horse replaces the
     * offer, so it can never confirm a move the player was not told about.
     */
    private static boolean confirmMove(@Nullable Player player, UUID horseId, @Nullable MinecraftServer server) {
        if (player == null || server == null) {
            return false;
        }
        int now = server.getTickCount();
        PendingMove pending = PENDING_MOVES.get(player.getUUID());
        if (pending != null && pending.horse().equals(horseId) && now - pending.tick() <= CONFIRM_WINDOW_TICKS) {
            PENDING_MOVES.remove(player.getUUID());
            return true;
        }
        PENDING_MOVES.put(player.getUUID(), new PendingMove(horseId, now));
        return false;
    }

    /**
     * <b>Pop the horse's old sign off its wall</b>, dropping it as the bound sign
     * it was - so moving a stall costs nothing and duplicates nothing: one sign on
     * a wall and one in hand before, one on a wall and one on the floor after.
     * Removed directly rather than broken, so it does not run the break handler
     * that would release the stall this call is about to move.
     *
     * @return where it popped, or {@code null} if there was no sign left there
     */
    private static @Nullable BlockPos popOldSign(MinecraftServer server, StallRecord previous, BoundHorse bound) {
        ServerLevel old = server.getLevel(previous.dimension());
        if (old == null) {
            return null;
        }
        BlockPos pos = previous.signPos();
        if (!(old.getBlockState(pos).getBlock() instanceof WallSignBlock)) {
            return null; // already gone - broken by hand, or built over
        }
        old.removeBlock(pos, false);
        ItemStack popped = new ItemStack(ModItems.BOUND_STALL_SIGN.get());
        popped.set(ModDataComponents.BOUND_HORSE.get(), bound);
        Block.popResource(old, pos, popped);
        return pos;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
        if (bound == null) {
            adder.accept(Component.literal("Right-click a horse you own to bind it.").withStyle(ChatFormatting.GRAY));
        } else {
            adder.accept(Component.literal("Bound to: "
                    + (bound.name().isBlank() ? bound.id().toString().substring(0, 8) : bound.name()))
                    .withStyle(ChatFormatting.GRAY));
            adder.accept(Component.literal("Place on a stall wall, from either side.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void message(UseOnContext ctx, String text) {
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().sendSystemMessage(Component.literal(text));
        }
    }
}
