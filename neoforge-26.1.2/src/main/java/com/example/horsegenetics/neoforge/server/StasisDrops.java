package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.StasisProduce;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>What an Advanced chamber's horse has made since its last turn</b> - the
 * bank's drop buffer, on the tag side.
 *
 * <p>{@link StasisProduce} decides <i>what</i> and <i>how often</i>, off the
 * same genome and the same {@code produce} ability a live horse is read from.
 * This class does the two things that need a game: turning an item id into an
 * item, and stamping the horse's own cooldown attachment so the clock survives
 * a restart.
 *
 * <h2>Nothing is produced until there is somewhere to put it</h2>
 * The two halves are deliberately separate calls. {@link #due} says what is
 * owed without changing anything; the bank finds room for all of it, and only
 * then does {@link #stamp} mark it as paid. A buffer with one free slot and a
 * horse that owes two stacks therefore produces <b>nothing</b> this turn and
 * tries again on the next - rather than half a yield, and rather than a yield
 * quietly dropped on the floor of a block that has no floor. A live horse can
 * drop its egg and walk away; a bank cannot, so it waits.
 *
 * <p>The mirror of that is {@link StasisCare}: same shape, same rule about not
 * touching a tag it could not finish writing.
 */
public final class StasisDrops {

    private StasisDrops() {
    }

    /**
     * What one chamber owes the buffer, as real stacks, with the gene keys that
     * owe them.
     *
     * @param stacks   one per gene that is due - never empty when this is returned
     * @param geneKeys the same genes, for {@link #stamp}
     */
    public record Batch(List<ItemStack> stacks, List<String> geneKeys) {
    }

    /**
     * Read what this chamber's horse has made. Nothing is written.
     *
     * @return {@code null} when the chamber is empty, holds a horse with no
     *         readable papers, or simply owes nothing - which is the common case
     *         and costs one walk of the ability list
     */
    public static @Nullable Batch due(ItemStack chamber, long now, Rng rng) {
        StasisSnapshot snapshot = StasisChamberItem.snapshotOf(chamber);
        if (snapshot == null) {
            return null;
        }
        CompoundTag horse = snapshot.horse();
        HorseRecord record = StasisCare.record(horse);
        if (record == null || !record.hasGenome()) {
            // A horse with no papers of ours has no genome to read abilities
            // from. It is still stored, still healed, still listed - it just
            // makes nothing, which is what it would do in a field too.
            return null;
        }

        Genotype genotype;
        Epigenome epigenome;
        try {
            genotype = Genotype.parse(record.geneticCode());
            epigenome = Epigenome.parse(record.epigenomeCode());
        } catch (RuntimeException unreadable) {
            return null;
        }

        List<StasisProduce.Yield> owed = StasisProduce.due(genotype, epigenome, record.sex(),
                !StasisCare.isBaby(horse), now, StasisCare.cooldowns(horse).lastByKey(), rng);
        if (owed.isEmpty()) {
            return null;
        }

        List<ItemStack> stacks = new ArrayList<>(owed.size());
        List<String> keys = new ArrayList<>(owed.size());
        for (StasisProduce.Yield yield : owed) {
            Item item = itemOf(yield);
            if (item == null) {
                // The same refusal GeneAbilityHandler makes for a live horse: an
                // id nothing is registered under produces nothing, and says so
                // once rather than every turn for ever.
                continue;
            }
            stacks.add(new ItemStack(item, yield.count()));
            keys.add(yield.geneKey());
        }
        return stacks.isEmpty() ? null : new Batch(List.copyOf(stacks), List.copyOf(keys));
    }

    /**
     * <b>Mark a batch as paid</b>, on the horse's own cooldown attachment, and
     * put the amended horse back in the chamber.
     *
     * <p>Called only once the bank has actually taken the items. A fresh tag and
     * a fresh component, for {@link StasisCare#turn}'s reason: the snapshot rides
     * on the stack and is compared by value on the way to the client, so mutating
     * the one we were handed would change the horse without the slot noticing it
     * had to resync.
     *
     * @return {@code false} if the stamp could not be written, in which case
     *         nothing was changed and the caller must not keep the items
     */
    public static boolean stamp(ItemStack chamber, List<String> geneKeys, long now) {
        StasisSnapshot snapshot = StasisChamberItem.snapshotOf(chamber);
        if (snapshot == null || geneKeys.isEmpty()) {
            return false;
        }
        CompoundTag mended = snapshot.horse().copy();
        HorseCooldownsAttachment cooldowns = StasisCare.cooldowns(mended);
        for (String geneKey : geneKeys) {
            cooldowns = cooldowns.stamp(StasisProduce.key(geneKey), now);
        }
        if (!StasisCare.putCooldowns(mended, cooldowns)) {
            return false;
        }
        chamber.set(ModDataComponents.STASIS_SNAPSHOT.get(),
                new StasisSnapshot(snapshot.horseName(), snapshot.horseId(), mended));
        return true;
    }

    private static @Nullable Item itemOf(StasisProduce.Yield yield) {
        Identifier id = Identifier.tryParse(yield.item());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            HorseGenetics.LOGGER.warn("[stasis] {} would produce '{}', which is not an item here",
                    yield.geneKey(), yield.item());
            return null;
        }
        return item;
    }
}
