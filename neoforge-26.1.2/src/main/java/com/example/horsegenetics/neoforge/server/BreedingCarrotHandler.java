package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.SpliceCategory;
import com.example.horsegenetics.neoforge.data.CarrotWindowAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Feeding a <b>breeding carrot</b> to a horse (roadmap wiki &sect;14). The
 * carrot puts a temporary {@link CarrotWindowAttachment} on the horse - the
 * same shape as vanilla breeding-mode love, which it also triggers - carrying
 * the carrot's effect tokens. {@link HorseBreedingHandler} reads both parents'
 * windows when a foal is made, folds each into a
 * {@link com.example.horsegenetics.common.genetics.GameteBias}, and clears them.
 *
 * <p>The effect biases the <b>gamete</b> the fed parent contributes; it never
 * rewrites the horse's own genotype (the determinism contract). Each carrot type
 * shows a differently-coloured particle burst as feedback.
 */
@EventBusSubscriber
public final class BreedingCarrotHandler {

    /**
     * The single {@link CarrotEffect} id a base carrot item carries, or
     * {@code null} if {@code item} is not one. Resolved per call - the item
     * holders are not bound when this class is loaded during subscriber
     * registration.
     */
    private static String baseTokenOf(Item item) {
        if (item == ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT.get()) return "epigenetic_splice";
        if (item == ModItems.UNKNOWN_GENE_SPLICE_CARROT.get()) return "gene_splice";
        if (item == ModItems.STABILIZER_CARROT.get()) return "stabilizer";
        if (item == ModItems.MAGNIFIER_CARROT.get()) return "magnifier";
        // The themed splices. Each writes the same token as the plain gene
        // splice with its category appended, so one CarrotEffect handles all
        // six and there is no second code path to keep in step.
        if (item == ModItems.DILUTION_GENE_SPLICE_CARROT.get()) return themed(SpliceCategory.DILUTION);
        if (item == ModItems.WHITE_GENE_SPLICE_CARROT.get()) return themed(SpliceCategory.WHITE);
        if (item == ModItems.MARKING_GENE_SPLICE_CARROT.get()) return themed(SpliceCategory.MARKING);
        if (item == ModItems.PERFORMANCE_GENE_SPLICE_CARROT.get()) return themed(SpliceCategory.PERFORMANCE);
        if (item == ModItems.MAGICAL_GENE_SPLICE_CARROT.get()) return themed(SpliceCategory.MAGICAL);
        return null;
    }

    private static String themed(SpliceCategory category) {
        return new CarrotEffect.GeneSplice(category).id();
    }

    /** Burst colour per carrot family, by first effect token. */
    private static int colourFor(List<String> tokens) {
        String first = tokens.isEmpty() ? "" : tokens.get(0);
        if (first.startsWith("known:")) return 0xFFCF47;
        // The themed splices burst in their own item colour, so the feedback
        // says which carrot went in rather than only that one did.
        if (first.startsWith("gene_splice:")) {
            SpliceCategory category = SpliceCategory.byId(first.substring("gene_splice:".length()));
            if (category != null) {
                return switch (category) {
                    case DILUTION -> 0xE3CE86;
                    case WHITE -> 0xE9EFF6;
                    case MARKING -> 0x9A6A3A;
                    case PERFORMANCE -> 0x2E7BD6;
                    case MAGICAL -> 0x2FC5C5;
                    default -> 0xE05B2B;
                };
            }
        }
        return switch (first) {
            case "epigenetic_splice" -> 0x9B59D0;
            case "gene_splice" -> 0xE05B2B;
            case "stabilizer" -> 0x3F8AE0;
            case "magnifier" -> 0x4CAF50;
            default -> 0xFFFFFF;
        };
    }

    @SubscribeEvent
    static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        List<String> tokens = tokensOf(stack);
        if (tokens.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        boolean client = event.getLevel().isClientSide();

        // Cancel on both sides so vanilla doesn't read it as a mount.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (client) {
            return;
        }

        if (horse.isBaby() || !(horse.level() instanceof ServerLevel level)) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.carrot.too_young"));
            return;
        }

        long now = level.getGameTime();
        CarrotWindowAttachment window = horse.getData(ModAttachments.CARROT_WINDOW.get());
        if (!window.isActiveAt(now)) {
            window = CarrotWindowAttachment.EMPTY;
        }
        horse.setData(ModAttachments.CARROT_WINDOW.get(), window.plus(tokens, now));

        // A carrot also puts the horse in breeding mode, like a golden one.
        horse.setInLove(player);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Stabilizer / magnifier are documented to no-op when the parent has no
        // heterozygous locus - say so rather than let it read as "it worked".
        if ((tokens.contains("stabilizer") || tokens.contains("magnifier")) && allHomozygous(horse)) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.carrot.no_effect"));
        }

        int rgb = colourFor(tokens);
        DustParticleOptions dust = new DustParticleOptions(rgb, 1.2F);
        level.sendParticles(dust, horse.getX(), horse.getY() + horse.getBbHeight() * 0.8, horse.getZ(),
                16, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(ParticleTypes.HEART, horse.getX(), horse.getY() + horse.getBbHeight(), horse.getZ(),
                4, 0.3, 0.3, 0.3, 0.0);
    }

    /** The carrot effect tokens a held stack grants, or an empty list if it is not a carrot. */
    public static List<String> tokensOf(ItemStack stack) {
        List<String> component = stack.get(ModDataComponents.CARROT_EFFECTS.get());
        if (component != null && !component.isEmpty()) {
            return component;
        }
        String base = baseTokenOf(stack.getItem());
        return base == null ? List.of() : List.of(base);
    }

    private static boolean allHomozygous(Horse horse) {
        try {
            Genotype g = Genotype.parse(HorseRecords.of(horse).geneticCode());
            return com.example.horsegenetics.common.genetics.Genes.codeOrder().stream()
                    .allMatch(gene -> g.pair(gene).homozygous());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private BreedingCarrotHandler() {
    }
}
