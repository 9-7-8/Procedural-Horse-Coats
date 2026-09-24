package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.server.HorseStasisHandler;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * <b>A horse, in a bottle.</b> Right-click one of your horses with an empty
 * chamber and it goes in; right-click the ground with a full one and it comes
 * back out, in one piece, as the same horse - same bond, same gear, same
 * pregnancy, same id in every pedigree that names it.
 *
 * <p>While it is in there the horse <b>does not exist as an entity</b>. It does
 * not tick, does not get hungry, does not grow up and costs the server nothing.
 * That is the entire point of the feature: a player with two hundred horses can
 * shelve the ones they are not using instead of leaving two hundred animals
 * ticking forever. See {@code wiki/horse-stasis.html}.
 *
 * <p><b>The chamber is not spent.</b> Letting a horse out hands the same chamber
 * back, empty and ready for another - owner's call, 2026-09-24. One chamber
 * holds one horse at a time, which is what "single use" on the wiki page meant.
 *
 * <p>Four of these are registered, one per {@link StasisTier}, differing only in
 * the tier they carry. The tier buys nothing at all until the Horse Stasis Bank
 * exists - storage is free at every tier and always will be - so an empty
 * {@code BASIC} chamber and an empty {@code SPACER} chamber do the same job
 * today, and the tooltip says which is which rather than pretending otherwise.
 *
 * <p>Like {@code TicketItem}, the item is only the tier and the words: capture
 * lives in {@link HorseStasisHandler}, because the right-click-a-horse event has
 * to be cancelled on both sides before vanilla turns it into a mount. Release is
 * here, since nothing hijacks a right-click on a block.
 */
public class StasisChamberItem extends Item {

    private final StasisTier tier;

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public StasisChamberItem(Properties properties, StasisTier tier) {
        super(properties);
        this.tier = tier;
    }

    public StasisTier tier() {
        return tier;
    }

    /** The horse in this chamber, or {@code null} for an empty one. */
    public static @Nullable StasisSnapshot snapshotOf(ItemStack stack) {
        return stack.get(ModDataComponents.STASIS_SNAPSHOT.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        StasisSnapshot snapshot = snapshotOf(stack);
        if (snapshot == null) {
            // An empty chamber is used on a horse, not on the floor. Passing
            // lets the block be used normally rather than eating the click.
            return InteractionResult.PASS;
        }
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        MinecraftServer server = serverLevel.getServer();
        if (HorseStasisHandler.alreadyLoose(server, snapshot)) {
            // Only reachable by copying a full chamber, which is a creative
            // keystroke. Two entities on one UUID would corrupt the pedigree.
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendSystemMessage(Component.literal(
                        snapshot.horseName() + " is already out in the world - this chamber is a copy."));
            }
            return InteractionResult.FAIL;
        }

        Vec3 pos = HorseStasisHandler.standingSpot(level, ctx.getClickedPos(), ctx.getClickedFace());
        float yRot = ctx.getPlayer() == null ? 0.0F : ctx.getPlayer().getYRot();
        Horse horse = HorseStasisHandler.release(serverLevel, pos, yRot, snapshot);
        if (horse == null) {
            // Nothing consumed: a horse that cannot be restored stays in the
            // chamber, where a later build may still be able to read it.
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendSystemMessage(Component.literal(
                        snapshot.horseName() + " could not be let out - the chamber is unchanged.")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // The chamber comes back empty rather than being spent.
        stack.remove(ModDataComponents.STASIS_SNAPSHOT.get());
        serverLevel.playSound(null, horse.blockPosition(), SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        StasisSnapshot snapshot = snapshotOf(stack);
        if (snapshot == null) {
            adder.accept(Component.literal("Empty - right-click one of your horses.")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            adder.accept(Component.literal(snapshot.horseName()).withStyle(ChatFormatting.GOLD));
            adder.accept(Component.literal("In stasis. Right-click the ground to let it out.")
                    .withStyle(ChatFormatting.GRAY));
        }
        adder.accept(Component.literal(tier.what()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
