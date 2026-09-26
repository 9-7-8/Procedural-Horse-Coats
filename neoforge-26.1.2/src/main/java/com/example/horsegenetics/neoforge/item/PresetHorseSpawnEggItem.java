package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.server.HorseEggSpawner;
import com.example.horsegenetics.neoforge.server.HorseResurrection;
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
 * A <b>preset horse egg</b>: the exact horse the creative editor was showing
 * when the egg was made, kept in an item and spawned on right-click.
 *
 * <p>It exists because the custom spawn egg screen could only ever produce a
 * horse <i>now</i>, in front of you. A build worth keeping - a genotype you
 * spent ten minutes on, the demonstration horse for a bug report, the founder
 * pair for a test line - had to be rebuilt from scratch every time, and there
 * was nothing you could hand to somebody else. This is the "save it" the screen
 * was missing: the same {@code StoredGenome} the stallion seed jar carries, plus
 * whether it was a foal, in an item.
 *
 * <p>Made in the editor, never crafted, and creative-only to make - but not to
 * <b>use</b>. Handing one to a survival player is the point of it being an item.
 *
 * <h2>It is also the resurrection egg</h2>
 * An egg carrying {@link ModDataComponents#RESURRECTION} holds a whole dead
 * horse rather than a genotype, and using it brings <b>that</b> horse back - the
 * same UUID, the same pedigree, the same bond - instead of spawning a founder
 * that looks like it. That is {@code /horseresurrect ... egg}, an operator
 * handing somebody their horse to put down where they want it.
 *
 * <p>It reuses this item rather than introducing one of its own, on the same
 * argument {@code GeneDeathHandler.cloneEgg} already makes for the death drop:
 * the two are the same thing arrived at from different directions, and one of
 * them already has a model, a tooltip and a spawner behind it. The tooltip says
 * which of the two an egg is, because the difference matters enormously to
 * whoever is holding it and nothing else on the item shows it.
 */
public class PresetHorseSpawnEggItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public PresetHorseSpawnEggItem(Properties properties) {
        super(properties);
    }

    public static @Nullable StoredGenome genomeOf(ItemStack stack) {
        return stack.get(ModDataComponents.STORED_GENOME.get());
    }

    /** The dead horse this egg brings back, or {@code null} for an ordinary preset egg. */
    public static @Nullable StasisSnapshot resurrectionOf(ItemStack stack) {
        return stack.get(ModDataComponents.RESURRECTION.get());
    }

    /**
     * <b>One named dead horse, in an egg</b> - what {@code /horseresurrect ...
     * egg} hands over. The snapshot has already been taken out of
     * {@code HorseAfterlife} by the time this is called, so the horse exists in
     * this stack and nowhere else.
     */
    public static ItemStack resurrecting(StasisSnapshot snapshot) {
        ItemStack stack = new ItemStack(ModItems.PRESET_HORSE_SPAWN_EGG.get());
        stack.set(ModDataComponents.RESURRECTION.get(), snapshot);
        return stack;
    }

    private static boolean isBaby(ItemStack stack) {
        Boolean baby = stack.get(ModDataComponents.PRESET_BABY.get());
        return baby != null && baby;
    }

    /** The egg for one built horse - what the editor's "Make egg" button produces. */
    public static ItemStack of(StoredGenome stored, boolean baby) {
        ItemStack stack = new ItemStack(ModItems.PRESET_HORSE_SPAWN_EGG.get());
        stack.set(ModDataComponents.STORED_GENOME.get(), stored);
        if (baby) {
            stack.set(ModDataComponents.PRESET_BABY.get(), Boolean.TRUE);
        }
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        StasisSnapshot resurrection = resurrectionOf(ctx.getItemInHand());
        StoredGenome stored = genomeOf(ctx.getItemInHand());
        if (resurrection == null && stored == null) {
            return InteractionResult.PASS;
        }
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Direction face = ctx.getClickedFace();
        BlockPos at = ctx.getClickedPos();
        BlockPos target = level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                ? at : at.relative(face);
        Vec3 pos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (resurrection != null) {
            return resurrect(ctx, serverLevel, pos, resurrection);
        }

        Horse horse;
        try {
            horse = HorseEggSpawner.spawnPreset(serverLevel, pos,
                    ctx.getPlayer() == null ? 0.0F : ctx.getPlayer().getYRot(),
                    stored, isBaby(ctx.getItemInHand()));
        } catch (RuntimeException e) {
            // A genotype code is only parseable against the gene list that
            // wrote it. An egg made before a gene pack was added or removed is
            // no longer readable, and saying so is better than a crash report.
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendSystemMessage(Component.translatable(
                        "message.horsegenetics.preset_egg.unreadable", e.getMessage()));
            }
            return InteractionResult.FAIL;
        }
        if (horse == null) {
            return InteractionResult.FAIL;
        }
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * <b>Bring back the horse this egg holds.</b> The egg is consumed even in
     * creative: unlike a preset egg, which is a reusable stamp of a genotype,
     * this one holds the only copy of a particular animal and a second use would
     * be a second entity claiming one UUID.
     */
    private static InteractionResult resurrect(UseOnContext ctx, ServerLevel level, Vec3 pos,
                                               StasisSnapshot snapshot) {
        if (HorseResurrection.alreadyAlive(level.getServer(), snapshot)) {
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendSystemMessage(Component.literal(
                                snapshot.horseName() + " is already alive somewhere. "
                                        + "This egg has been spent.")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        Horse horse = HorseResurrection.raise(level, pos,
                ctx.getPlayer() == null ? 0.0F : ctx.getPlayer().getYRot(), snapshot);
        if (horse == null) {
            // restore() has already logged why. Nothing has been consumed, so
            // the horse is still in the egg and the player can try elsewhere.
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendSystemMessage(Component.translatable(
                        "message.horsegenetics.preset_egg.unreadable", snapshot.horseName()));
            }
            return InteractionResult.FAIL;
        }
        ctx.getItemInHand().shrink(1);
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().sendSystemMessage(Component.literal(
                            snapshot.horseName() + " is back.")
                    .withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        StasisSnapshot resurrection = resurrectionOf(stack);
        if (resurrection != null) {
            adder.accept(Component.literal(resurrection.horseName())
                    .withStyle(ChatFormatting.GOLD));
            adder.accept(Component.translatable("item.horsegenetics.preset_horse_spawn_egg.resurrection")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        StoredGenome stored = genomeOf(stack);
        if (stored == null) {
            adder.accept(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BreedLineage lineage = BreedLineage.parse(stored.breed());
        adder.accept(Component.literal(lineage.displayName()
                        + (isBaby(stack) ? " foal" : ""))
                .withStyle(ChatFormatting.GOLD));
        try {
            adder.accept(Component.literal(
                            GeneCodeDisplay.shortForm(Genotype.parse(stored.genotypeCode())))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } catch (RuntimeException e) {
            adder.accept(Component.translatable("item.horsegenetics.preset_horse_spawn_egg.stale")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
