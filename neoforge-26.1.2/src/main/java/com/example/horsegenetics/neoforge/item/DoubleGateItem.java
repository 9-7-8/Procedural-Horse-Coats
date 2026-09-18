package com.example.horsegenetics.neoforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * The item for a <a href="#">double gate</a>, which exists for one line of text.
 *
 * <h2>Why a whole class for a message</h2>
 * A gate two blocks wide refuses to place when neither side of the block you
 * clicked is free, because half a double gate is not a thing and leaving one
 * standing would be worse. That refusal is correct and it was <b>silent</b>:
 * nothing placed, nothing consumed, nothing said. From the far side of the
 * screen that is indistinguishable from a dead item, and it cost real time -
 * the owner reported it as <i>"if you only place the first one, it's
 * invisible"</i>, which sent the first round of diagnosis chasing a rendering
 * bug that did not exist.
 *
 * <p>{@link com.example.horsegenetics.neoforge.item.StallSignItem} already set
 * the precedent: it explains every refusal it makes, for exactly this reason.
 *
 * <h2>Telling our refusal apart from every other one</h2>
 * {@link BlockItem#place} returns a failure for plenty of ordinary reasons - you
 * clicked a solid block, you are out of reach, the spot is protected - and a
 * message on all of them would be noise. {@link BlockPlaceContext#canPlace()}
 * is the discriminator: it asks whether the clicked position itself could be
 * replaced. If that is true and the placement <em>still</em> failed, the only
 * thing left that could have refused is
 * {@code DoubleFenceGateBlock.getStateForPlacement} finding nowhere for the
 * second block.
 */
public class DoubleGateItem extends BlockItem {

    public DoubleGateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        // Server side only: place() runs on both, and a message sent from each
        // is a message shown twice.
        if (!result.consumesAction()
                && !context.getLevel().isClientSide()
                && context.canPlace()
                && context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(
                    "No room for a gate two blocks wide - it needs a free block to one side or the other."));
        }
        return result;
    }
}
