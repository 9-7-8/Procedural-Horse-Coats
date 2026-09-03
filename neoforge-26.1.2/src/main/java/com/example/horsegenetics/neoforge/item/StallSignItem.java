package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.server.StallDebug;
import com.example.horsegenetics.neoforge.server.StallDetector;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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

/**
 * The stall sign. A <b>blank</b> one ({@code stall_sign}) does nothing until you
 * right-click a horse with it (see {@code server/StallSignHandler}), which turns
 * it into a <b>bound</b> one ({@code bound_stall_sign}) carrying a
 * {@link BoundHorse} component.
 *
 * <p>Placing a bound sign against the <b>outside</b> face of a wall
 * ({@link #useOn}) drops a real oak wall sign there with the horse's name on it
 * and runs {@link StallDetector} on the block behind that wall: if it finds an
 * enclosed area (this layer &plusmn; one), that area becomes the horse's stall
 * ({@link StallData}) and its outline is flashed with particles
 * ({@link StallDebug}).
 */
public class StallSignItem extends Item {

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

        // The stall is on the far side of the wall from the sign.
        BlockPos seed = wall.relative(face.getOpposite());
        Optional<StallDetector.Result> found = StallDetector.detect(level, seed);
        if (found.isEmpty()) {
            message(ctx, "No enclosed area behind that block - a stall must be walled in "
                    + "(up to 3 blocks tall, at most " + StallDetector.MAX_BLOCKS + " open blocks).");
            return InteractionResult.FAIL;
        }
        StallDetector.Result r = found.get();

        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, face);
        level.setBlock(signPos, signState, Block.UPDATE_ALL);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            String name = bound.name().isBlank() ? "(unnamed)" : bound.name();
            sign.updateText(t -> t
                    .setMessage(0, Component.literal("Stall"))
                    .setMessage(1, Component.literal(name)), true);
        }

        MinecraftServer server = level.getServer();
        StallRecord record = new StallRecord(
                bound.id(), bound.name(), level.dimension(), signPos, r.min(), r.max(), r.blockCount());
        if (server != null) {
            StallData.get(server).assign(record);
        }

        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            StallDebug.showOne(sp, record);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
        if (bound == null) {
            adder.accept(Component.literal("Right-click a horse to bind it.").withStyle(ChatFormatting.GRAY));
        } else {
            adder.accept(Component.literal("Bound to: "
                    + (bound.name().isBlank() ? bound.id().toString().substring(0, 8) : bound.name()))
                    .withStyle(ChatFormatting.GRAY));
            adder.accept(Component.literal("Place on the outside wall of an enclosed stall.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void message(UseOnContext ctx, String text) {
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().sendSystemMessage(Component.literal(text));
        }
    }
}
