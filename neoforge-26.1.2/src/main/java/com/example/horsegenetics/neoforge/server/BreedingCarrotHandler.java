package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.SpliceCategory;
import net.minecraft.ChatFormatting;
import com.example.horsegenetics.neoforge.data.ArmedCarrotsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.server.HorseProgress;
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
 * carrot <b>arms</b> the horse ({@link ArmedCarrotsAttachment}) with its effect
 * tokens, at any time and for as long as it takes. Whenever that horse next
 * breeds through the mod's own paths, {@link ReproHandler#breed} folds each
 * parent's effects into a
 * {@link com.example.horsegenetics.common.genetics.GameteBias}, and uses them up
 * only if the mare conceives. An armed parent also turns golden-carrot breeding
 * into a pregnancy rather than an instant foal.
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

        // A CARROT ARMS THE HORSE UNTIL ITS NEXT CONCEPTION (owner, 2026-09-13).
        // It used to open a 30-second window and put the horse in love. Now it
        // waits on the horse - in heat or out of it, through a breeding that did
        // not take - and only one that takes uses it up. Breeding is a separate
        // act: golden carrots, a seed jar, or a stallion left with her in heat.
        ArmedCarrotsAttachment armed = horse.getData(ModAttachments.ARMED_CARROTS.get());
        horse.setData(ModAttachments.ARMED_CARROTS.get(), armed.plus(tokens));
        tickCarrotTasks(player, tokens);
        ReproHandler.overlay(player, ReproHandler.nameOf(horse) + " will pass this on at the next breeding "
                + "that takes.", ChatFormatting.GREEN);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Stabilizer / magnifier are documented to no-op when the parent has no
        // heterozygous locus - say so rather than let it read as "it worked".
        if ((tokens.contains("stabilizer") || tokens.contains("magnifier")) && allHomozygous(horse)) {
            player.sendSystemMessage(Component.translatable("message.horsegenetics.carrot.no_effect"));
            // The one case where nothing happening is CORRECT, written down so
            // it cannot be mistaken for the item being dead.
            ActionTrace.log("carrot", ActionTrace.describeShort(horse)
                    + " is homozygous everywhere, so that carrot has nothing to do - "
                    + "this is the documented no-op, not a failure");
        }

        // SAY WHICH CARROT, TO WHICH HORSE, AND FOR HOW LONG. Owner, on the
        // new carrot bench: "you might need to put in more logging to detect
        // the different carrot uses."
        //
        // She is describing the hardest thing about this whole item family.
        // There are ELEVEN of them, every one is an orange carrot fed to a
        // horse, and every one produces the same animation - hearts, a puff of
        // dust, the horse in love. The only visible difference is the colour of
        // the dust, and two of them (stabilizer, magnifier) legitimately do
        // NOTHING AT ALL on a homozygous parent. So "did that carrot work" has
        // three indistinguishable answers from inside the game: it worked, it
        // correctly declined, or the item is not wired up.
        //
        // Worse, the effect does not happen here. A carrot ARMS the horse and
        // the genetics only happen at the next conception, which may be a heat,
        // a breeding and a second horse later. Logging the feed alone would
        // still leave that gap, so ReproHandler.breed logs the other end.
        ActionTrace.log("carrot", player.getName().getString() + " fed "
                + stack.getItem().getName(stack).getString() + " to "
                + ActionTrace.describeShort(horse) + " - effects "
                + String.join(", ", tokens) + ", armed until its next conception");

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

    /**
     * One checklist task per kind of carrot fed. Reads the effect ids the carrot
     * actually carries rather than the item, so a combined carrot ticks
     * everything it is made of - which is right, because feeding one really did
     * do all of those things.
     */
    private static void tickCarrotTasks(Player player, List<String> tokens) {
        for (String token : tokens) {
            if (token.startsWith("known:")) {
                HorseProgress.complete(player, ProgressTask.USE_GENE_CARROT);
            } else if (token.equals("stabilizer")) {
                HorseProgress.complete(player, ProgressTask.CARROT_STABILIZER);
            } else if (token.equals("magnifier")) {
                HorseProgress.complete(player, ProgressTask.CARROT_MAGNIFIER);
            } else if (token.equals("epigenetic_splice")) {
                HorseProgress.complete(player, ProgressTask.CARROT_UNKNOWN_EPIGENETIC);
            } else if (token.startsWith("gene_splice")) {
                HorseProgress.complete(player, ProgressTask.CARROT_UNKNOWN_GENE);
                // A themed splice IS an unknown gene splice with the pool
                // narrowed - same effect, same token with a category on the end -
                // so it ticks the general box as well as its own. Ticking only
                // the themed one would leave a player who owns every themed
                // carrot unable to finish the sub-chapter.
                if (token.startsWith("gene_splice:")) {
                    SpliceCategory themed =
                            SpliceCategory.byId(token.substring("gene_splice:".length()));
                    if (themed != null) {
                        switch (themed) {
                            case DILUTION -> HorseProgress.complete(player, ProgressTask.CARROT_DILUTION);
                            case WHITE -> HorseProgress.complete(player, ProgressTask.CARROT_WHITE);
                            case MARKING -> HorseProgress.complete(player, ProgressTask.CARROT_MARKING);
                            case PERFORMANCE -> HorseProgress.complete(player, ProgressTask.CARROT_PERFORMANCE);
                            case MAGICAL -> HorseProgress.complete(player, ProgressTask.CARROT_MAGICAL);
                            case ANY -> {
                                // the plain splice, already credited above
                            }
                        }
                    }
                }
            }
        }
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
