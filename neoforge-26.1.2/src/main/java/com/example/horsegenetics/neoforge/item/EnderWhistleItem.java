package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.server.EnderWhistleCalls;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * <b>The ender whistle</b> - bound to one horse for good, and it brings that horse
 * to you from anywhere, across dimensions, as often as you like.
 *
 * <ul>
 *   <li><b>Binding</b> is right-clicking a horse you own with an unbound one - see
 *       {@code server/EnderWhistleCalls#onBind}. It never unbinds.</li>
 *   <li><b>Blowing</b> a bound one calls the horse. Only its <i>current</i> owner
 *       can: the whistle goes with the horse when the papers do, so an old owner
 *       holding it is told the horse no longer answers to them.</li>
 *   <li><b>When the horse dies the whistle turns to dust</b> - the next time a
 *       player is holding it, with a message. Checked once a second here, off
 *       {@code HorseWhereabouts}, so a whistle left in a chest learns it the
 *       moment it is picked up.</li>
 * </ul>
 *
 * <p>Owner's design, 2026-09-13: "It's bound to a specific horse, and can be used
 * infinitely but never unbound. When used, it teleports the bound horse to you,
 * even across dimensions. When the horse dies, the whistle breaks."
 *
 * <p><b>Not verified in-game.</b>
 */
public class EnderWhistleItem extends Item {

    private static final int COOLDOWN_TICKS = 100;

    // Item(Properties) is @Deprecated to nudge modders toward the id-carrying
    // Properties that DeferredRegister.Items#registerItem already supplies here.
    @SuppressWarnings("deprecation")
    public EnderWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
            if (bound == null) {
                player.sendSystemMessage(Component.literal(
                        "This whistle is not bound yet. Right-click one of your horses with it - "
                                + "the binding is permanent."));
                return InteractionResult.SUCCESS;
            }
            if (EnderWhistleCalls.crumbleIfGone(serverPlayer, stack, bound)) {
                return InteractionResult.SUCCESS;
            }
            EnderWhistleCalls.call(serverPlayer, bound);
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The dust check. Once a second is plenty, and it only ever does work for a
     * bound whistle in a player's inventory.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player) || level.getGameTime() % 20 != 0) {
            return;
        }
        BoundHorse bound = stack.get(ModDataComponents.BOUND_HORSE.get());
        if (bound != null) {
            EnderWhistleCalls.crumbleIfGone(player, stack, bound);
        }
    }
}
