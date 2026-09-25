package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.server.EmergencyStasisHandler;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>The chamber that uses itself.</b> Carry one and it watches every horse you
 * own; the instant one of them falls below {@link StasisRescue#DEFAULT_THRESHOLD}
 * of its own health the horse is <b>in the bottle</b>, wherever you and it
 * happen to be. Nothing is clicked and nothing is aimed - the capture is the
 * save, and a horse inside a chamber cannot be hurt at all.
 *
 * <p>The watching itself is {@link EmergencyStasisHandler}, on the damage event,
 * for the same reason ordinary capture lives in {@code HorseStasisHandler}: the
 * item is the words and the marker, and the mechanism belongs where the event
 * is. This class exists so that an inventory walk can ask
 * {@code instanceof EmergencyStasisChamberItem} instead of comparing against a
 * registered item, and so that the tooltip can say what the thing does.
 *
 * <h2>Why it is a subclass and not a fifth tier</h2>
 * It extends {@link StasisChamberItem} at {@link StasisTier#BASIC}, which is the
 * whole of what makes the rest of the feature work on it for free: the bank's
 * {@code isChamber} is an {@code instanceof} on the superclass, so an emergency
 * chamber files, counts, stacks and lists exactly like the basic chamber it was
 * built from, and the Intermediate upgrade - which asks for a chamber at
 * {@code BASIC} and does not care which class it is - will happily trade a
 * caught horse up to an Intermediate chamber with an eye of ender. Nothing in the bank, the Browse tab, the upkeep, the drop buffer or the
 * stud logic knows this class exists, and none of it needed to.
 *
 * <p>A fifth {@code StasisTier} was the alternative and is wrong twice over. The
 * ladder is four rungs of <i>what the bank may do with the horse</i>, cumulative
 * and pinned by {@code StasisTierTest}; self-capture is not that kind of
 * capability, and the emergency chamber is built from the <em>cheapest</em> rung
 * rather than sitting above the most expensive one. A rung that cost eight ender
 * pearls and bought no bank capability at all would be a hole in a ladder the
 * test exists to keep whole.
 *
 * <p>The consequence worth stating: <b>upgrading an emergency chamber spends the
 * emergency.</b> An eye of ender turns it into an ordinary Intermediate chamber
 * carrying the same horse. That is the escape hatch rather than the intended
 * path - letting the horse out hands the emergency chamber straight back, empty
 * and armed again - but it means a rescued horse is never stuck in a chamber the
 * bank cannot look inside.
 */
public class EmergencyStasisChamberItem extends StasisChamberItem {

    public EmergencyStasisChamberItem(Properties properties) {
        super(properties, StasisTier.BASIC, false);
    }

    public EmergencyStasisChamberItem(Properties properties, boolean occupied) {
        super(properties, StasisTier.BASIC, occupied);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        StasisSnapshot snapshot = snapshotOf(stack);
        if (snapshot == null) {
            // Deliberately different words from an empty ordinary chamber, which
            // reads "right-click one of your horses". Doing that with this one
            // works and is not the point of it.
            adder.accept(Component.literal("Armed - carry it.").withStyle(ChatFormatting.AQUA));
            adder.accept(Component.literal(
                    "Takes one of your horses by itself if its health falls below a tenth.")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            adder.accept(Component.literal(snapshot.horseName()).withStyle(ChatFormatting.GOLD));
            adder.accept(Component.literal("Rescued. Right-click the ground to let it out.")
                    .withStyle(ChatFormatting.GRAY));
            // The refusal is the item's one sharp edge, so it is on the tooltip
            // and not only in the chat line that says it after the fact.
            adder.accept(Component.literal("Full - it cannot save a second horse.")
                    .withStyle(ChatFormatting.RED));
        }
        adder.accept(Component.literal(tier().what()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
