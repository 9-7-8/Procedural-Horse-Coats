package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.server.HorseDietHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static com.example.horsegenetics.neoforge.HorseGenetics.MOD_ID;

/**
 * <b>Other mods' hay bales - a horse eats them like vanilla's.</b>
 *
 * <p>Asked for Bale of Sugar Cane ({@code baleofsugarcane}), which compresses
 * nine sugar canes into a block built with
 * {@code Properties.ofFullCopy(Blocks.HAY_BLOCK)} - a hay bale in everything but
 * name, which horses nonetheless ignored entirely. Written as a <b>tag</b> and
 * not as a check for that one mod, so the next bale mod joins by datapack and
 * touches no Java: {@code horsegenetics:hay_bales}, block and item, whose only
 * hard-coded member is vanilla's own {@code minecraft:hay_block}.
 *
 * <h2>The three halves of "a horse eats it"</h2>
 * <ol>
 *   <li><b>Grazing</b> - {@code HungerFoodGoal} walks to one and eats it at the
 *       {@code HAY} rung. Driven off {@link #BALE_BLOCKS}.</li>
 *   <li><b>Standing near one</b> - gated healing and the herd's pasture search
 *       read {@code horsegenetics:horse_food}, which now includes the tag.</li>
 *   <li><b>Fed by hand</b> - a tag and this handler, below.</li>
 * </ol>
 *
 * <h2>Why hand-feeding needs Java and not just a tag</h2>
 * Vanilla's {@code AbstractHorse.isFood} is {@code minecraft:horse_food}, a tag,
 * so adding the bale to it is enough to make a horse <i>want</i> it - temptation,
 * taming, the bond grant in {@code HorseCareHandler}, and
 * {@code Horse.mobInteract} taking the food branch instead of mounting the player.
 * But {@code AbstractHorse.handleEating} is a chain of {@code itemStack.is(Items.X)}
 * with no tag anywhere in it, so an unrecognised member of that tag heals nothing,
 * ages nothing, and returns {@code itemUsed = false}: the horse accepts the bale as
 * food and then does nothing with it forever, which is worse than refusing it.
 *
 * <p>So this applies vanilla's own hay-bale numbers - {@link #HEAL} health,
 * {@link #AGE_UP} seconds off a foal, no temper, exactly {@code handleEating}'s
 * {@code Items.HAY_BLOCK} branch - and cancels the interaction.
 * {@code Items.HAY_BLOCK} itself is deliberately left to vanilla: this only picks
 * up the bales vanilla has never heard of.
 *
 * <p><b>A horse that needs neither is left to vanilla</b> rather than given a
 * special case. Vanilla's hay bale grants no temper, so feeding one to a
 * full-health adult does nothing and is not consumed; this bows out in the same
 * state, and the player keeps their nine sugar canes.
 *
 * <p><b>A special-diet horse is not ours either.</b> {@link HorseDietHandler} owns
 * that interaction end to end, healing by the diet's own points and refusing what
 * the diet forbids, so this bows out for one rather than feeding it twice or
 * feeding it the wrong amount - the same division of labour that class already
 * draws with vanilla.
 *
 * <p><b>Unverified against a running game.</b> The ids were read out of
 * {@code baleofsugarcane-neoforge-1.0.0+26.1.2.jar} itself - block
 * {@code baleofsugarcane:baleofsugarcane}, item
 * {@code baleofsugarcane:baleofsugarcane_item}, which do not match each other and
 * are not what either would be guessed to be - but the two mods have never been
 * run together. A wrong id costs an absent optional tag entry and nothing else.
 */
@EventBusSubscriber
public final class HayBales {

    private HayBales() {
    }

    /** Vanilla's hay-bale branch of {@code AbstractHorse.handleEating}, restated. */
    private static final float HEAL = 20.0F;
    private static final int AGE_UP = 180;

    /** Blocks a horse may graze as hay. Vanilla's, plus whatever a datapack adds. */
    public static final TagKey<Block> BALE_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "hay_bales"));
    /** The same bales in the hand. */
    public static final TagKey<Item> BALE_ITEMS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "hay_bales"));

    /** Is this block a bale of hay, whoever added it? */
    public static boolean isBale(BlockState state) {
        return state.is(BALE_BLOCKS);
    }

    /** Is this item a bale of hay, whoever added it? */
    public static boolean isBale(ItemStack stack) {
        return stack.is(BALE_ITEMS);
    }

    /**
     * A bale another mod added - one vanilla's {@code handleEating} will do
     * nothing with. Vanilla's own is excluded so that feeding it stays vanilla's
     * business, byte for byte.
     */
    public static boolean isModdedBale(ItemStack stack) {
        return !stack.is(Items.HAY_BLOCK) && stack.is(BALE_ITEMS);
    }

    @SubscribeEvent
    static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getTarget() instanceof AbstractHorse horse)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!isModdedBale(held)) {
            return;
        }
        // A special diet is HorseDietHandler's from start to finish - see the
        // class note. DietFoods.accepts already lets a wheat-eater have a bale.
        if (horse instanceof Horse h && HorseDietHandler.dietOf(h).isSpecial()) {
            return;
        }

        // Decided from synced state only - health, age, age lock - so both sides
        // reach the same verdict and the client never swings a hand the server
        // then disagrees with.
        boolean heals = horse.getHealth() < horse.getMaxHealth();
        boolean grows = horse.isBaby() && !horse.isAgeLocked();
        if (!heals && !grows) {
            return;     // as vanilla's own bale: nothing happens, nothing is eaten
        }
        consume(event);
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }

        if (heals) {
            horse.heal(HEAL);
        }
        if (grows) {
            horse.ageUp(AGE_UP);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    horse.getX(), horse.getY() + horse.getBbHeight() * 0.5, horse.getZ(),
                    4, 0.4, 0.3, 0.4, 0.0);
        }
        horse.setEating(true);
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        held.consume(1, event.getEntity());
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
