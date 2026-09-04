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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
 *   <li>right-click a tamed adult <b>mare in breeding mode</b> with a filled jar
 *       -&gt; a foal is bred immediately from her live genome and the jar's
 *       stored one, through the same {@link HorseBreedingHandler#applyBredFoal}
 *       path natural breeding uses; the jar is consumed and the mare goes on the
 *       vanilla breeding cooldown.</li>
 * </ul>
 *
 * <p>The {@code isInLove()} check is the stand-in for roadmap &sect;15.1's
 * "feed a breeding carrot for a 30-second window" - vanilla love already lasts
 * ~30 s and works in creative, and collecting / impregnating consumes it.
 *
 * <p><b>Not yet</b> (deliberately - this is "begin"): the gate is vanilla love,
 * not one of this mod's breeding carrots (they do nothing yet); no gestation
 * state (the foal is immediate, exactly as vanilla breeding is); and the jar
 * carries no carrot effects. See {@code wiki/roadmap.html} &sect;&sect;14-15 and
 * {@code wiki/verification.html}.
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
                if (!client) message(player, who + " is recorded as a mare - collect seed from a stallion.");
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
        if (!stallion.isTamed()) {
            message(player, HorseRecords.of(stallion).displayName() + " is not tamed.");
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
                record.displayName());

        ItemStack filled = new ItemStack(ModItems.STALLION_SEED_JAR.get());
        filled.set(ModDataComponents.STORED_GENOME.get(), stored);

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
        message(player, "Collected a seed sample from " + record.displayName() + ".");
        return true;
    }

    /** @return true if a foal was bred. */
    private static boolean impregnateMare(Horse mare, Player player, ItemStack jar) {
        StoredGenome stored = jar.get(ModDataComponents.STORED_GENOME.get());
        if (stored == null) {
            message(player, "This seed jar is empty.");
            return false;
        }
        if (!(mare.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!mare.isTamed()) {
            message(player, "Tame the mare first.");
            return false;
        }
        if (!mare.isInLove()) {
            message(player, HorseRecords.of(mare).displayName()
                    + " must be in breeding mode - feed it first.");
            return false;
        }
        if (mare.getAge() > 0) {
            message(player, "The mare is still on breeding cooldown.");
            return false;
        }

        Rng rng = HorseRecords.rng(mare);
        HorseRecord mareRecord = HorseBreedingHandler.ensureParentRecord(mare);
        if (mareRecord.sex() != Sex.FEMALE) {
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

        String[] name = splitName(stored.sourceName());
        HorseRecord sireRecord = HorseRecord
                .founder(stored.sourceId(), name[0], name[1], sireGenome);

        Horse foal = EntityType.HORSE.create(level, EntitySpawnReason.BREEDING);
        if (foal == null) {
            return false;
        }
        foal.setAge(-24000); // newborn
        foal.snapTo(mare.getX(), mare.getY(), mare.getZ(), mare.getYRot(), 0.0F);

        boolean born = HorseBreedingHandler.applyBredFoal(
                foal, mare, mareGenome, mareRecord, sireGenome, sireRecord, player, rng);
        if (!born) {
            // an embryonic lethal - the jar is spent and the mare has used her
            // breeding window, exactly as a real pairing would have, but there
            // is no foal. applyBredFoal has already told the player why.
            foal.discard();
            jar.shrink(1);
            mare.resetLove();
            mare.setAge(6000);
            return true;
        }
        level.addFreshEntity(foal);
        level.broadcastEntityEvent(mare, (byte) 18); // heart particles, like vanilla breeding

        jar.shrink(1);
        mare.resetLove();
        mare.setAge(6000); // vanilla post-breeding cooldown

        message(player, HorseRecords.of(foal).displayName() + " was born.");
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
