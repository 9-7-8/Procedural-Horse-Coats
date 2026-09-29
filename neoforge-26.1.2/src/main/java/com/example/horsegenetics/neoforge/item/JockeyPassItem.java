package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * <b>The jockey pass.</b> A <b>blank</b> one ({@code jockey_pass}) does nothing
 * until you right-click a horse <b>you own</b> with it, which turns it into a
 * <b>bound</b> one ({@code bound_jockey_pass}) carrying that horse's
 * {@link BoundHorse}. Hand the bound pass to somebody else and they feed it to
 * that horse to buy themselves a day in the saddle.
 *
 * <p>All three moments are {@code server/JockeyPassHandler}'s; this class is the
 * tooltip, which is the only part of a pass that has to exist on the client. It
 * carries both ids because the blank and the bound pass want different words and
 * a tooltip that guessed from the component alone would say the wrong thing on a
 * blank one somebody had renamed.
 *
 * <h2>Why the pass binds to the horse and not to the person</h2>
 * The other way round was the obvious design and is worse. A pass made out to
 * <em>Kestrel, for one day</em> is a thing you can hand to whoever turns up to
 * ride, sell at the gate, or post to a friend - which is what a race needs. A
 * pass made out to <em>Kestrel may ride something</em> would have to be bound to
 * a person at the moment it was made, and a jockey who did not show up would
 * take the pass out of the meeting with them.
 *
 * <p>It also means the dangerous half is the half the horse's owner performs.
 * Binding needs ownership; redeeming needs only the pass, exactly as a
 * {@link SignedTransferPaperItem signed transfer paper} does.
 */
public class JockeyPassItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public JockeyPassItem(Properties properties) {
        super(properties);
    }

    /** The horse this pass names, or {@code null} on a blank one. */
    public static @Nullable BoundHorse horseOf(ItemStack stack) {
        return stack.get(ModDataComponents.BOUND_HORSE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        BoundHorse bound = horseOf(stack);
        if (bound == null) {
            adder.accept(Component.literal("Right-click a horse you own to make this out to it.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        adder.accept(Component.literal("Good for: "
                        + (bound.name().isBlank() ? bound.id().toString().substring(0, 8) : bound.name()))
                .withStyle(ChatFormatting.GRAY));
        // Deliberately does not name a length. behaviour.jockey_pass_days is a
        // server setting, and a tooltip is drawn on a client that may be looking
        // at this stack in a creative menu with no server behind it - a hard
        // "one day" here would be a lie on any server that moved the number.
        adder.accept(Component.literal("Feed it to that horse to borrow it for a while.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
