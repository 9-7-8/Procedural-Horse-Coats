package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.server.HorseEggSpawner;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * A <b>breed spawn egg</b>: one right-click, one foundation horse of one breed.
 *
 * <h2>One item, not forty-nine</h2>
 * The breed is a {@code breed} data component, so there is a single registered
 * item and every breed is a stack of it. That is not a shortcut - it is what
 * makes the feature work for <i>player-added</i> breeds. A breed dropped into
 * {@code config/horsegenetics/breeds/} after the jar was built has no registry
 * entry, no model file and no lang key, and could never have an item of its own;
 * with the breed on the component it gets an egg on exactly the same terms as
 * the built-in ones, which is the whole promise of making breeds data.
 *
 * <p>The texture is vanilla's horse spawn egg, deliberately. Forty-nine
 * hand-tinted eggs is a lot of art for a thing whose identity is written on the
 * tooltip anyway, and a player who has one in hand wants to know <i>which</i>
 * breed, which is a word, not a colour.
 *
 * <h2>Not craftable</h2>
 * There is no recipe. Eggs come from dungeon chests
 * ({@code AddBreedSpawnEggModifier}) and from the horseman, dear. A breed may
 * opt out of having one at all by leaving {@code spawn_egg} out of its
 * {@code spawn} list, and {@link Breeds#FERAL_MIXED} has none by construction -
 * "a horse of no particular breeding" is what an ordinary vanilla spawn egg
 * already gives you.
 */
public class BreedSpawnEggItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public BreedSpawnEggItem(Properties properties) {
        super(properties);
    }

    /** The breed a stack names, or {@code null} if the component is missing or unknown. */
    public static @Nullable Breed breedOf(ItemStack stack) {
        String id = stack.get(ModDataComponents.BREED_ID.get());
        if (id == null || id.isEmpty()) {
            return null;
        }
        Breed breed = Breeds.get(id);
        return breed == Breeds.FERAL_MIXED ? null : breed;
    }

    /** An egg for {@code breed} - what the loot function and the horseman's trade hand out. */
    public static ItemStack of(Breed breed) {
        ItemStack stack = new ItemStack(ModItems.BREED_SPAWN_EGG.get());
        stack.set(ModDataComponents.BREED_ID.get(), breed.id());
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Breed breed = breedOf(ctx.getItemInHand());
        if (breed == null) {
            return InteractionResult.PASS;   // a blank egg is inert, not a crash
        }
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Direction face = ctx.getClickedFace();
        BlockPos at = ctx.getClickedPos();
        // Vanilla's rule: into the block clicked if it is one you can stand in,
        // otherwise onto the face you clicked.
        BlockPos target = level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                ? at : at.relative(face);
        Vec3 pos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        Horse horse = HorseEggSpawner.spawnFounder(serverLevel, pos,
                ctx.getPlayer() == null ? 0.0F : ctx.getPlayer().getYRot(), breed);
        if (horse == null) {
            return InteractionResult.FAIL;
        }
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        Breed breed = breedOf(stack);
        return breed == null
                ? super.getName(stack)
                : Component.translatable("item.horsegenetics.breed_spawn_egg.named",
                        Component.literal(breed.name()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        Breed breed = breedOf(stack);
        if (breed == null) {
            adder.accept(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        adder.accept(Component.literal(breed.name()).withStyle(ChatFormatting.GOLD));
        adder.accept(Component.literal(
                        (breed.magical() ? "Magical" : "Natural") + " · "
                                + Commonness.forWeight(breed.spawnWeight())
                                .name().toLowerCase(Locale.ROOT).replace('_', ' '))
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.horsegenetics.breed_spawn_egg.foundation")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
