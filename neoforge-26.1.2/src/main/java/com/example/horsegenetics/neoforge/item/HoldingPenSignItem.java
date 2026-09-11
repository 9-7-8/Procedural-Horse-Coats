package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.server.StallDebug;
import com.example.horsegenetics.neoforge.server.StallDetector;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

/**
 * <b>The holding pen sign</b> - hang it on the wall of a pen and that pen is
 * where your holding pen tickets send horses. Owner's design (2026-09-10): a
 * general pen for the horse you have just tamed, so it can be sent home before
 * it has a stall of its own.
 *
 * <p>It is a stall sign bound to a <b>player</b> instead of a horse. The room
 * is found the same way ({@link StallDetector}), from either side of the wall,
 * and like a stall it is re-measured every time a ticket is spent - so this
 * stores only where the sign is. One pen per player: hanging a second sign
 * moves the pen, and breaking the sign releases it
 * ({@code StallSignHandler.onSignBroken}).
 */
public class HoldingPenSignItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public HoldingPenSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            player.sendSystemMessage(Component.literal("Place the holding pen sign on a vertical wall face."));
            return InteractionResult.FAIL;
        }
        BlockPos wall = ctx.getClickedPos();
        BlockPos signPos = wall.relative(face);
        if (!level.getBlockState(signPos).canBeReplaced()) {
            player.sendSystemMessage(Component.literal("No room to place the sign there."));
            return InteractionResult.FAIL;
        }

        StallDetector.Result r = StallDetector.forSign(level, wall, face);

        BlockState signState = Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, face);
        level.setBlock(signPos, signState, Block.UPDATE_ALL);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            sign.updateText(t -> t
                    .setMessage(0, Component.literal("Holding pen"))
                    .setMessage(1, Component.literal(player.getGameProfile().name())), true);
        }

        MinecraftServer server = level.getServer();
        if (server != null) {
            StallData data = StallData.get(server);
            PenRecord previous = data.penOf(player.getUUID());
            data.assignPen(new PenRecord(player.getUUID(), level.dimension(), signPos));
            if (previous != null && !(previous.dimension().equals(level.dimension())
                    && previous.signPos().equals(signPos))) {
                player.sendSystemMessage(Component.literal("Your holding pen has moved here. "
                        + "The old sign is just a sign now.").withStyle(ChatFormatting.GRAY));
            }
        }
        if (!player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        if (player instanceof ServerPlayer sp) {
            StallDebug.showOne(sp, new StallRecord(player.getUUID(), "Holding pen", level.dimension(),
                    signPos, r.min(), r.max(), r.blockCount()));
            sp.sendSystemMessage(Component.literal(r.enclosed()
                    ? "Holding pen set - " + r.blockCount() + " blocks, "
                            + r.sizeX() + "x" + r.sizeY() + "x" + r.sizeZ() + "."
                    : "Holding pen set - no walls found, so it is the open ground in front of the sign."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("Hang it on a pen's wall: holding pen tickets send your horses there.")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.literal("One per player - a new sign moves the pen.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
