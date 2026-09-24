package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.StasisUpkeep;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.compat.HayBales;
import com.example.horsegenetics.neoforge.data.HorseRecordCodecs;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/**
 * <b>Reading and mending a horse without waking it up.</b>
 *
 * <p>The Horse Stasis Bank's upkeep has to change a stored horse's health, and
 * a stored horse is not an entity - it is a {@code saveWithoutId} tag on an
 * item. This class is the only place that reaches into one of those tags, and
 * it exists so that the reaching is done once, in the open, with the field
 * names in one file.
 *
 * <h2>Why not just load the horse, change it and save it again</h2>
 * Because that is precisely the work the whole feature exists not to do.
 * {@code HorseStasisHandler.release} builds a real {@code Horse} out of a tag,
 * and it is the right call when a player is letting one out; doing it once a
 * turn for a chamber nobody is looking at would make a full bank cost more than
 * the fifty-four ticking animals it replaced. So the arithmetic happens in
 * {@link StasisUpkeep} on four numbers, and this class fetches and returns
 * those four numbers.
 *
 * <h2>What it reads, and where each one lives in the tag</h2>
 * <ul>
 *   <li><b>Health</b> - {@code LivingEntity.TAG_HEALTH}, a float at the top level.</li>
 *   <li><b>Max health</b> - resolved from the horse's own record, which is
 *       where {@code HorseRecords.applyTraitsToEntity} gets it from in the
 *       first place. The saved {@code attributes} list is only the fallback,
 *       and {@link #maxHealth} says why in detail: it is very often empty.</li>
 *   <li><b>Hunger</b> - the {@code hunger} attachment, under NeoForge's
 *       {@link AttachmentHolder#ATTACHMENTS_NBT_KEY}. Keyed by the id the
 *       registry holds rather than a string typed here, so renaming the
 *       attachment cannot silently orphan this.</li>
 *   <li><b>Diet</b> - resolved off the {@code horse_record} attachment exactly
 *       as {@link HorseDietHandler#dietOf} resolves it off a live horse, and for
 *       the same reason: a blood-drinker heals by biting, and a bank that
 *       quietly mended one from a hay bale would contradict the field.</li>
 * </ul>
 *
 * <p><b>Nothing here is a second source of truth.</b> Every number is read out
 * of the tag on the turn it is used and written straight back; the bank caches
 * none of it. A chamber whose tag this cannot make sense of is left exactly as
 * it was - the same rule {@code release} follows when a horse will not load.
 */
public final class StasisCare {

    private StasisCare() {
    }

    private static final String MAX_HEALTH_ID = Attributes.MAX_HEALTH.getRegisteredName();

    // ------------------------------------------------------------------
    // One turn
    // ------------------------------------------------------------------

    /**
     * Take one turn of upkeep on the horse in {@code chamber}.
     *
     * @param feed  what is in the bank's feed slot - offered to this horse, and
     *              taken only if this horse would eat it
     * @param water water units the bank has in its meter
     * @return the result, or {@code null} if this stack holds no horse or its
     *         tag could not be read - in which case nothing has been touched
     */
    public static StasisUpkeep.@Nullable Result turn(ItemStack chamber, ItemStack feed, int water) {
        StasisSnapshot snapshot = com.example.horsegenetics.neoforge.item.StasisChamberItem.snapshotOf(chamber);
        if (snapshot == null) {
            return null;
        }
        CompoundTag horse = snapshot.horse();
        HorseRecord record = record(horse);
        float health = horse.getFloatOr(LivingEntity.TAG_HEALTH, 0.0F);
        float maxHealth = maxHealth(horse, record);
        double hunger = hunger(horse);
        HorseDiet diet = diet(record);

        StasisUpkeep.Result result = StasisUpkeep.upkeep(hunger, health, maxHealth, water,
                diet.diet().fedByItems(), mouthful(diet, feed));
        if (!result.changed()) {
            return result;
        }

        // A fresh tag and a fresh component: the snapshot rides on the stack and
        // is compared by value on the way to the client, so mutating the one we
        // were handed would change the horse without the bank's slot ever
        // noticing it had to resync.
        CompoundTag mended = horse.copy();
        mended.putFloat(LivingEntity.TAG_HEALTH, result.health());
        if (!putHunger(mended, result.hunger())) {
            // The horse has no hunger attachment to spend from, which means it
            // was never charged for the health either. Leave it whole rather
            // than heal it for free.
            return new StasisUpkeep.Result(hunger, health, water, false, 0.0);
        }
        chamber.set(com.example.horsegenetics.neoforge.data.ModDataComponents.STASIS_SNAPSHOT.get(),
                new StasisSnapshot(snapshot.horseName(), snapshot.horseId(), mended));
        return result;
    }

    /**
     * <b>What one item out of the feed slot is worth to this horse</b>, or
     * {@code null} if this horse will not eat it.
     *
     * <p>{@link DietFoods#acceptsFromGround} is the question, not
     * {@link DietFoods#accepts}: the bank puts food in front of a horse rather
     * than holding it out by hand, which is what the ground rule describes - an
     * ordinary horse takes anything in {@code #minecraft:horse_food} or anything
     * it would graze, and a narrow diet still walks past everything but its own.
     * That also makes the feed slot work with any other mod's horse food, since
     * the tag is vanilla's and the grazing check is the common tags.
     *
     * <p>The two rungs are the live ones: a bale is a bale
     * ({@link Hunger.Food#HAY}), and everything else is worth what a player's
     * hand is worth ({@link Hunger.Food#HAND}). Inventing a third scale for the
     * bank would have made a hay bale mean two different things in two places.
     */
    public static Hunger.@Nullable Food mouthful(HorseDiet diet, ItemStack feed) {
        if (feed.isEmpty() || !DietFoods.acceptsFromGround(diet, feed)) {
            return null;
        }
        return HayBales.isBale(feed) ? Hunger.Food.HAY : Hunger.Food.HAND;
    }

    /** Is the horse in this chamber hurt at all? The cheap question, asked first. */
    public static boolean isHurt(ItemStack chamber) {
        StasisSnapshot snapshot = com.example.horsegenetics.neoforge.item.StasisChamberItem.snapshotOf(chamber);
        if (snapshot == null) {
            return false;
        }
        CompoundTag horse = snapshot.horse();
        float health = horse.getFloatOr(LivingEntity.TAG_HEALTH, 0.0F);
        float max = maxHealth(horse, record(horse));
        return health > 0.0F && max > 0.0F && health < max;
    }

    // ------------------------------------------------------------------
    // Reading the tag
    // ------------------------------------------------------------------

    /**
     * <b>The horse's maximum health</b>, or {@code 0} if nothing in the tag
     * says - which {@link StasisUpkeep#upkeep} treats as "leave this one
     * alone".
     *
     * <p><b>The record is asked first, and that is not an optimisation.</b> A
     * saved entity's {@code attributes} list holds only the attribute instances
     * something has actually <i>materialised</i> - {@code AttributeMap} creates
     * one on demand and {@code getValue} reads the type's supplier without
     * creating anything - so a horse can be saved with an empty attributes list
     * and a perfectly good maximum health. The bank's gametest caught exactly
     * that on its first run: a 53-health horse whose saved list was {@code []}.
     * Reading the list alone would have made every bank decide every horse was
     * unhurt and heal nothing, for ever, silently.
     *
     * <p>{@code HorseRecords.applyTraitsToEntity} writes {@code traits.health()}
     * into the base value, so the record is where the number comes <i>from</i>
     * and the attributes list is a copy of it that may or may not exist. The
     * list is still consulted, second, for a horse with no record of ours -
     * an unconverted vanilla one, whose bar is then whatever was saved.
     */
    public static float maxHealth(CompoundTag horse, @Nullable HorseRecord record) {
        if (record != null && record.hasGenome()) {
            try {
                return (float) HorseRecords.traitsOf(record).health();
            } catch (RuntimeException unreadable) {
                // Fall through to the tag - a record that will not resolve is
                // not a reason to refuse to mend the horse.
            }
        }
        ListTag attributes = horse.getListOrEmpty(LivingEntity.TAG_ATTRIBUTES);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag entry = attributes.getCompound(i).orElse(null);
            if (entry != null && MAX_HEALTH_ID.equals(entry.getStringOr("id", ""))) {
                return (float) entry.getDoubleOr("base", 0.0);
            }
        }
        return 0.0F;
    }

    /** The horse's hunger, or {@link Hunger#FULL} for a tag that never stored one. */
    public static double hunger(CompoundTag horse) {
        CompoundTag stored = attachment(horse, ModAttachments.HUNGER.get());
        return stored == null ? Hunger.FULL : stored.getDoubleOr("hunger", Hunger.FULL);
    }

    /** This horse's papers, or {@code null} for a tag that carries none. */
    public static @Nullable HorseRecord record(CompoundTag horse) {
        CompoundTag stored = attachment(horse, ModAttachments.HORSE_RECORD.get());
        if (stored == null) {
            return null;
        }
        try {
            return HorseRecordCodecs.CODEC.parse(NbtOps.INSTANCE, stored).result().orElse(null);
        } catch (RuntimeException bad) {
            return null;
        }
    }

    /**
     * This horse's diet, or {@link HorseDiet#NORMAL} for anything without a
     * readable record. The same defensive shape, and the same two parses, as
     * {@link HorseDietHandler#dietOf} uses on a live horse.
     */
    public static HorseDiet diet(@Nullable HorseRecord record) {
        if (record == null || !record.hasGenome()) {
            return HorseDiet.NORMAL;
        }
        try {
            return HorseDiet.resolve(Genotype.parse(record.geneticCode()),
                    Epigenome.parse(record.epigenomeCode()));
        } catch (RuntimeException bad) {
            return HorseDiet.NORMAL;
        }
    }

    // ------------------------------------------------------------------
    // Attachments, by the id the registry holds
    // ------------------------------------------------------------------

    private static @Nullable CompoundTag attachment(CompoundTag horse, AttachmentType<?> type) {
        String key = idOf(type);
        if (key == null) {
            return null;
        }
        return horse.getCompoundOrEmpty(AttachmentHolder.ATTACHMENTS_NBT_KEY)
                .getCompound(key).orElse(null);
    }

    /**
     * Write the hunger back, and say whether there was one to write. False for a
     * horse that carries no hunger attachment at all: the bank must not invent
     * one, because inventing it is the same as healing that horse for free.
     */
    private static boolean putHunger(CompoundTag horse, double hunger) {
        String key = idOf(ModAttachments.HUNGER.get());
        if (key == null) {
            return false;
        }
        Tag attachments = horse.get(AttachmentHolder.ATTACHMENTS_NBT_KEY);
        if (!(attachments instanceof CompoundTag all)) {
            return false;
        }
        Tag stored = all.get(key);
        if (!(stored instanceof CompoundTag one)) {
            return false;
        }
        one.putDouble("hunger", hunger);
        return true;
    }

    private static @Nullable String idOf(AttachmentType<?> type) {
        Identifier id = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type);
        if (id == null) {
            HorseGenetics.LOGGER.warn("[stasis] an attachment the bank needs is not registered");
            return null;
        }
        return id.toString();
    }
}
