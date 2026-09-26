package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>A golden carrot always works.</b> Feed one to each of two adult horses of
 * opposite sex and you get a foal - there is no other condition, and this class
 * is what clears the two that vanilla imposes before the food is even eaten.
 *
 * <h2>The rule (owner, and it is the whole design)</h2>
 * <blockquote>The golden carrot should ALWAYS make two horses have a baby - the
 * only limit being age (both adults) and being opposite sex.</blockquote>
 *
 * So: no breeding cooldown, no taming, no full-health requirement, no gelding,
 * no subfertility roll, no heat cycle, and a mare who is already pregnant
 * breeds anyway. The other half of that is in
 * {@code HorseBreedingHandler.onBabySpawn} (which drops the gelding, pregnancy
 * and fertility gates) and {@code mixin/HorseAlwaysParentMixin} (which drops
 * vanilla's tamed + full-health test). <b>A golden carrot never makes a
 * pregnancy either</b> - it makes a foal, instantly, even from a mare who is
 * carrying one; the gestation system belongs to natural cover and the seed jar.
 *
 * <h2>What this class does, in order</h2>
 * <ol>
 *   <li><b>Zeroes the cooldown.</b> Vanilla stores an adult's breeding cooldown
 *       in {@code AgeableMob.age} as a positive tick count ({@code setAge(6000)}
 *       on each parent after a foal), and {@code handleEating} only sets love
 *       when {@code getAge() == 0}. Setting it to zero is therefore the whole
 *       of "no cooldown" - no state of our own, and nothing to keep in step.</li>
 *   <li><b>Puts an untamed adult in love itself.</b> Vanilla's
 *       {@code AbstractHorse.handleEating} sets love only on a
 *       <i>tamed</i> horse, so a wild one eats the carrot for the temper and
 *       nothing happens. There is no hook inside that method worth taking for
 *       one boolean, so this does the feed for that case: love, consume,
 *       cancel.</li>
 * </ol>
 *
 * <p>A tamed horse is left entirely to vanilla - it already does the right
 * thing once the age is zero - so the common path adds one field write and no
 * behaviour of its own.
 *
 * <h2>What this used to do</h2>
 * It refused the feed and reported the time remaining ("4m 12s to go"), and
 * refused a pregnant mare outright. Both were right when a cooldown was a rule;
 * with the rule gone they are a message about a thing that no longer happens.
 * The bug that motivated it is still closed, differently: vanilla ate the item
 * and did nothing, and now there is no state in which feeding does nothing.
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
        // A foal's age is NEGATIVE and counts up to zero as it grows. Only a
        // positive age is a cooldown, and only that is cleared: "both adults"
        // is a limit the owner kept, so a baby must stay a baby.
        if (horse.isBaby()) {
            return;
        }
        if (horse.getAge() > 0) {
            horse.setAge(0);
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        // The untamed case. Vanilla will not set love here, so nothing below
        // would ever happen and the carrot would be eaten for the temper alone.
        if (!horse.isTamed() && !horse.isInLove()) {
            horse.setInLove(event.getEntity());
            if (!event.getEntity().getAbilities().instabuild) {
                stack.shrink(1);
            }
            // Cancelled on BOTH sides: the client must not predict a mount, and
            // the server must not let anything downstream eat the stack twice.
            // See HorseInteractionHandler for the same rule.
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    /**
     * Vanilla's love items ({@link DietFoods#isVanillaLoveItem}). This mod's
     * breeding carrots are not here: one arms the horse until its next
     * conception ({@code BreedingCarrotHandler}), so there is no wrong moment to
     * feed one and nothing to clear before it.
     */
    private static boolean isBreedingFood(ItemStack stack) {
        return DietFoods.isVanillaLoveItem(stack);
    }
}
