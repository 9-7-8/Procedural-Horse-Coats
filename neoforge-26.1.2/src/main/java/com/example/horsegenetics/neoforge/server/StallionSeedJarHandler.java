package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.common.repro.Conception;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The stallion seed jar, first slice (roadmap wiki &sect;15.1).
 *
 * <ul>
 *   <li>right-click a tamed adult <b>stallion that is in breeding mode</b>
 *       (fed a carrot / apple etc. - {@code isInLove()}) with an
 *       {@code empty_seed_jar} -&gt; the jar becomes a {@code stallion_seed_jar}
 *       stamped with a {@link StoredGenome} (his genotype + epigenome, sex,
 *       UUID, name, speed / health), and his love state is consumed;</li>
 *   <li>right-click an adult <b>mare you own, in heat</b> with a filled jar -&gt;
 *       one conception attempt ({@link ReproHandler#tryConceive}) from her live
 *       genome and the jar's stored one. If it takes she is <b>pregnant</b> and
 *       the foal comes at the due date. The jar is spent either way; a mare out
 *       of heat refuses it and it is kept.</li>
 * </ul>
 *
 * <p>Filling the jar still takes the stallion's vanilla love, and counts as one
 * of his covers for the day.
 *
 * <p><b>Not yet:</b> the jar carries no carrot effects. See
 * {@code wiki/roadmap.html#ivf} and {@code wiki/fertility.html}.
 *
 * <p>Mirrors {@link HorseInteractionHandler}'s pattern: cancel the interaction
 * on <b>both</b> sides so the client doesn't predict a mount, do the mutation
 * server-side only.
 */
@EventBusSubscriber
public final class StallionSeedJarHandler {

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) return;

        ItemStack stack = event.getItemStack();
        boolean empty = stack.is(ModItems.EMPTY_SEED_JAR.get());
        boolean filled = stack.is(ModItems.STALLION_SEED_JAR.get());
        if (!empty && !filled) return;

        Player player = event.getEntity();
        boolean client = event.getLevel().isClientSide();

        if (!HorseRecords.hasRealRecord(horse)) {
            return; // record not assigned yet - let the join handler run first
        }
        if (horse.isBaby()) {
            if (!client) message(player, "That horse is too young.");
            consume(event, InteractionResult.FAIL);
            return;
        }

        String who = HorseRecords.of(horse).displayName();
        Sex sex = HorseRecords.of(horse).sex();
        if (empty) {
            if (sex == Sex.MALE) {
                consume(event, client ? InteractionResult.SUCCESS
                        : result(collectFromStallion(horse, player, event.getHand(), stack)));
            } else {
                if (!client) message(player, wrongEnd(horse, who));
                consume(event, InteractionResult.FAIL);
            }
        } else {
            if (sex == Sex.FEMALE) {
                consume(event, client ? InteractionResult.SUCCESS
                        : result(impregnateMare(horse, player, stack)));
            } else {
                if (!client) message(player, who + " is recorded as a stallion - use the jar on a mare.");
                consume(event, InteractionResult.FAIL);
            }
        }
    }

    private static InteractionResult result(boolean ok) {
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** @return true if a sample was collected. */
    private static boolean collectFromStallion(Horse stallion, Player player, InteractionHand hand, ItemStack emptyJar) {
        // YOUR stallion, not merely a tamed one, and creative is no exception.
        // Owner, 2026-09-13: "you shouldn't be able to make any kind of bound
        // item to a horse you don't own, even in creative mode." A jar carries
        // one named stallion's genome away, so it is a bound item. This asked
        // only isTamed(), which any stranger's riding horse passes.
        String refusal = HorseOwnership.bindRefusal(stallion, player, HorseRecords.of(stallion).displayName());
        if (refusal != null) {
            message(player, refusal);
            return false;
        }
        if (HorseRecords.of(stallion).gelded()) {
            message(player, HorseRecords.of(stallion).displayName() + " is a gelding - there is nothing to collect.");
            return false;
        }
        if (!stallion.isInLove()) {
            message(player, HorseRecords.of(stallion).displayName()
                    + " must be in breeding mode - feed it first.");
            return false;
        }
        HorseRecord record = HorseRecords.of(stallion);
        Genome genome = genomeOf(stallion, record);

        StoredGenome stored = new StoredGenome(
                genome.genotypeCode(),
                genome.epigenomeCode(),
                stallion.getUUID(),
                record.displayName(),
                record.breed().orElse(""),
                ReproHandler.armedTokens(stallion));   // his armed carrots go into the jar

        ItemStack filled = new ItemStack(ModItems.STALLION_SEED_JAR.get());
        filled.set(ModDataComponents.STORED_GENOME.get(), stored);
        HorseProgress.complete(player, ProgressTask.FILL_SEED_JAR);

        // Transform the jar in the player's hand - even in creative: this is an
        // item transform, not a cost, and leaving the empty jar in hand reads as
        // "nothing happened". Only spill to the inventory if the held stack was >1.
        if (emptyJar.getCount() <= 1) {
            player.setItemInHand(hand, filled);
        } else {
            emptyJar.shrink(1);
            if (!player.addItem(filled)) {
                player.drop(filled, false);
            }
        }
        stallion.resetLove(); // consume the breeding window, like a real pairing does
        ReproHandler.recordCover(stallion); // a fill counts against his three a day
        ReproHandler.disarm(stallion);      // ...and his carrots left with the jar
        message(player, "Collected a seed sample from " + record.displayName() + ".");
        return true;
    }

    /** @return true if a foal was bred. */
    /**
     * <b>An empty stallion jar, held out to a mare.</b> The refusal that used to
     * be here explained the mistake, which nobody needed: a player who has just
     * done this knows what a mare is and has simply grabbed the wrong horse.
     * What it did not do was make the mod any fun to be wrong in front of.
     *
     * <p>Rotated on the horse's own randomness, so the same mare is not sarcastic
     * in the same way twice running.
     */
    private static String wrongEnd(Horse horse, String who) {
        String[] lines = {
                who + " is a mare. She has nothing to give you, and she is aware of what you asked.",
                "The jar says stallion. " + who + " is not one. Between the two of you, one can read.",
                who + " declines, on the grounds of not being a stallion.",
                "You hold an empty jar under a mare and wait. Nothing about this is going to work.",
        };
        return lines[horse.getRandom().nextInt(lines.length)];
    }

    private static boolean impregnateMare(Horse mare, Player player, ItemStack jar) {
        StoredGenome stored = jar.get(ModDataComponents.STORED_GENOME.get());
        if (stored == null) {
            message(player, "This seed jar is empty.");
            return false;
        }
        if (!(mare.level() instanceof ServerLevel level)) {
            return false;
        }
        // THE MARE MUST BE YOURS; the stallion need not be. Owner, 2026-09-13:
        // "stallion seed should be only be able to be CREATED by the owner, but
        // it can be USED on any mare the player owns, even if they didn't own the
        // stallion." Making a jar binds a stallion to an item, so it is the
        // owner's act; using one is breeding your own mare, and a jar that has
        // changed hands is exactly how a stallion's line is meant to travel.
        // This asked only isTamed(), so a jar could be used on a stranger's mare.
        String mareName = HorseRecords.of(mare).displayName();
        if (!mare.isTamed()) {
            message(player, mareName + " is not tamed yet - tame her first.");
            return false;
        }
        if (!HorseOwnership.isOwner(mare, player.getUUID())) {
            message(player, mareName + " is not your mare - a seed jar can only be used on a mare you own.");
            return false;
        }
        Rng rng = HorseRecords.rng(mare);
        HorseRecord mareRecord = HorseBreedingHandler.ensureParentRecord(mare);
        if (mareRecord.sex() != Sex.FEMALE) {
            return false;
        }
        // HEAT IS THE WINDOW. This used to ask for vanilla love, the stand-in for
        // a timed window; owner, 2026-09-13: heat replaces it, and a mare out of
        // heat refuses the jar without spending it.
        if (!ReproHandler.receptive(mare)) {
            ReproHandler.overlay(player, ReproHandler.notReceptive(mare), ChatFormatting.YELLOW);
            return false;
        }
        Genome mareGenome = HorseBreedingHandler.genomeOf(mare, mareRecord, rng);

        Genome sireGenome;
        try {
            sireGenome = stored.sample().genome();
        } catch (RuntimeException e) {
            message(player, "This seed jar's genome can't be read in this world.");
            return false;
        }

        // THE REAL SIRE'S RECORD, when the world still has it - every horse's record
        // is filed in the ancestry data - so the foal's generation counts his and
        // its name draws on his real names, not a barn name split in two. The
        // genome stays the jar's: it is what was collected. A stallion whose
        // record is gone falls back to what the jar itself remembers.
        String[] name = splitName(stored.sourceName());
        final Genome jarGenome = sireGenome;
        HorseRecord sireRecord = com.example.horsegenetics.neoforge.data.HorseAncestryData.get(level.getServer())
                .lookup(stored.sourceId())
                .map(r -> r.withGenome(jarGenome))
                .orElseGet(() -> HorseRecord.founder(stored.sourceId(), name[0], name[1], jarGenome, stored.breed()));

        // A pregnancy, not a foal. The stallion's cover was counted when the
        // jar was filled, so none is counted here. A jar that does not take is
        // still spent - it was a real try. The mare's armed carrots and the
        // jar's both act on the draw; hers are used up only if it takes.
        Conception.Result result = ReproHandler.breed(mare, mareRecord, mareGenome, sireGenome, sireRecord,
                null, stored.carrots(), player);
        jar.shrink(1);
        level.broadcastEntityEvent(mare, (byte) 18); // heart particles, like vanilla breeding
        ReproHandler.announce(mare, player, result);
        return true;
    }

    /** The horse's stored genome, founding one if its record predates the field. */
    private static Genome genomeOf(Horse horse, HorseRecord record) {
        if (record.hasGenome()) {
            return record.genome();
        }
        Genome genome = Genome.of(record.genotype(), HorseRecords.rng(horse));
        HorseRecords.apply(horse, record.withGenome(genome));
        return genome;
    }

    private static String[] splitName(String full) {
        String s = full == null ? "" : full.strip();
        int space = s.indexOf(' ');
        if (space < 0) {
            return new String[] {s.isEmpty() ? "Unknown" : s, ""};
        }
        return new String[] {s.substring(0, space).strip(), s.substring(space + 1).strip()};
    }

    private static void message(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private StallionSeedJarHandler() {
    }
}
