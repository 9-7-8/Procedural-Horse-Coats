package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Refuse a breeding food while the horse is still on its breeding cooldown,
 * and say how long is left.</b>
 *
 * <h2>The bug this closes</h2>
 * Vanilla eats the item and does nothing. {@code Animal.mobInteract} only puts
 * a horse in love when {@code getAge() == 0}, but the food is consumed on the
 * way past regardless - so feeding a golden carrot to a horse that bred four
 * minutes ago costs you the carrot, produces no hearts, and tells you nothing
 * at all. Playtesters read that as breeding being broken, which is a fair
 * reading of what they were shown.
 *
 * <p>So: the interaction is cancelled before vanilla can eat anything, and the
 * player is told <i>which</i> horse, and <i>how long</i>. The horse keeps its
 * cooldown and the player keeps their carrot.
 *
 * <h2>Where the number comes from</h2>
 * Vanilla stores an adult's breeding cooldown in {@code AgeableMob.age} as a
 * <b>positive</b> tick count that counts down one per tick ({@code setAge(6000)}
 * on each parent after a successful breeding). So {@code getAge()} <i>is</i> the
 * remaining cooldown in ticks, and it is only positive while the cooldown is
 * running - a foal's age is negative and an ordinary adult's is zero. No state
 * of our own is needed, which is why this reads vanilla's field rather than
 * keeping a timer beside it.
 *
 * <h2>What counts as a breeding food</h2>
 * Both halves of the question, because a player does not distinguish them:
 * vanilla's love items ({@link DietFoods#isVanillaLoveItem}) and
 * <b>this mod's breeding carrots</b>, which are ordinary items that only mean
 * anything at the moment of breeding. Feeding one of those on cooldown is worse
 * than losing a golden carrot - a Known Gene Splice carrot is an expensive,
 * hand-made object, and silently eating one is the version of this bug that
 * actually costs something.
 *
 * <p><b>{@link EventPriority#HIGH}</b> so this runs before the diet and yield
 * handlers: the point is that nothing else gets to consume the stack first.
 */
@EventBusSubscriber
public final class BreedingCooldownHandler {

    private BreedingCooldownHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!isBreedingFood(stack)) {
            return;
        }
        // A foal's age is negative - that is "too young", not "on cooldown",
        // and vanilla's own baby path already handles being fed. Only a
        // positive age is a cooldown.
        int remaining = horse.getAge();
        if (remaining <= 0) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            event.getEntity().sendSystemMessage(Component.literal(
                            horse.getName().getString() + " cannot breed yet - "
                                    + describe(remaining) + " to go.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        // Cancelled on BOTH sides: the client must not predict a mount or an
        // eat, and the server must not let anything downstream swallow the
        // stack. See HorseInteractionHandler for the same rule.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Vanilla's love items, plus every breeding carrot this mod adds. The
     * Known Gene Splice carrot is matched by its component rather than by
     * identity, because it is one item parameterised by {@code carrot_effects}.
     */
    private static boolean isBreedingFood(ItemStack stack) {
        return DietFoods.isVanillaLoveItem(stack)
                || stack.is(ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT.get())
                || stack.is(ModItems.UNKNOWN_GENE_SPLICE_CARROT.get())
                || stack.is(ModItems.STABILIZER_CARROT.get())
                || stack.is(ModItems.MAGNIFIER_CARROT.get())
                || stack.has(ModDataComponents.CARROT_EFFECTS.get());
    }

    /**
     * Ticks as something a player can act on. Minecraft ticks are 20 a second,
     * and the cooldown is minutes, so this rounds to whole seconds and only
     * mentions minutes when there is at least one - "4m 12s", "38s".
     */
    static String describe(int ticks) {
        int seconds = Math.max(1, (ticks + 19) / 20);
        int minutes = seconds / 60;
        int rest = seconds % 60;
        return minutes > 0 ? minutes + "m " + rest + "s" : rest + "s";
    }
}
