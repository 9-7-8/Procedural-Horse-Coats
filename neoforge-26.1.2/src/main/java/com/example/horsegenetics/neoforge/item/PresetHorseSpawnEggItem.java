package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.server.HorseEggSpawner;
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
 */
public class PresetHorseSpawnEggItem extends Item {

    @SuppressWarnings("deprecation") // Item(Properties) - DeferredRegister supplies the id-carrying Properties
    public PresetHorseSpawnEggItem(Properties properties) {
        super(properties);
    }

    public static @Nullable StoredGenome genomeOf(ItemStack stack) {
        return stack.get(ModDataComponents.STORED_GENOME.get());
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
        StoredGenome stored = genomeOf(ctx.getItemInHand());
        if (stored == null) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
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
