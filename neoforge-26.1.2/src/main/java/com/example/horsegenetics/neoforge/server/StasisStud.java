package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.StasisMating;
import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * <b>In-bank breeding</b> - a Spacer chamber marked <i>at stud</i> is a horse
 * turned out into the bank's paddock.
 *
 * <p>{@link StasisMating} owns the rules and is pure; this is the tag surgery
 * and the one moment the feature has to touch the world.
 *
 * <h2>The pregnancy is carried in the chamber; the mare comes out to foal</h2>
 * That split is the whole design, and it is what keeps a bank-bred foal
 * identical to every other foal in the mod. Gestation is free - a
 * {@link Pregnancy} is two absolute ticks, so it advances on a shelf exactly as
 * it would in a field, and nothing has to tick for it. <b>Birth is not free</b>,
 * and it is not free for a good reason: {@code HorseBreedingHandler.populateFoal}
 * names the foal under the dam's owner's naming policy, tames it to her owner,
 * gives it a quarter of her bond, files it in the ancestry database, puts it in
 * her band and tells everyone nearby. Every one of those is a fact about the
 * dam, and reimplementing them against a saved tag would have been a second
 * breeding path to keep in step for ever - which is exactly the kind of drift
 * this repository has a table of rules about.
 *
 * <p>So the bank hands her back to the world at her due date and lets
 * {@link ReproHandler}'s ordinary tick do the birth, within two seconds. She and
 * the foal are standing beside the block when the player next opens it. A bank
 * that cannot find a free spot to put her down keeps her, says so on the Supply
 * tab, and tries again on her next turn - it never foals into a wall, and it
 * never quietly loses a pregnancy to one.
 *
 * <h2>Once per heat, like anywhere else</h2>
 * Owner, 2026-09-24: <i>"only creates a new foal once per the mare's heat, same
 * cycle as on land"</i>. That is {@link com.example.horsegenetics.common.repro.ReproRules#mayTryNaturally},
 * unchanged - a mare gets one attempt per heat and a failed one waits for the
 * next, which is the same thing that stops a paddock breeding flat out. The
 * stallion's three-covers-a-day tiredness applies too, counted on his own stored
 * record, so a single stud in a bank of mares is worse odds after the third.
 */
public final class StasisStud {

    private StasisStud() {
    }

    /** How far from the block a mare may be set down to foal. */
    private static final int FOALING_REACH = 2;

    /**
     * <b>One turn on one at-stud chamber.</b>
     *
     * <p>Only a mare does anything here: she is the one carrying, and she is the
     * one whose heat decides when anything happens. A stallion's own turn is
     * spent being healed like everyone else, and he is found by whichever mare's
     * turn it is.
     *
     * @return {@code true} if anything in the bank changed
     */
    public static boolean turn(ServerLevel level, BlockPos pos, HorseStasisBankBlockEntity bank, int slot) {
        Container chambers = bank.chambers();
        ItemStack chamber = chambers.getItem(slot);
        StasisSnapshot snapshot = StasisChamberItem.snapshotOf(chamber);
        if (snapshot == null) {
            return false;
        }
        CompoundTag horse = snapshot.horse();
        HorseRecord record = StasisCare.record(horse);
        if (record == null) {
            return false;
        }
        long now = level.getGameTime();
        Reproduction repro = StasisCare.repro(horse, snapshot.horseId());

        if (repro.pregnant()) {
            return carry(level, pos, bank, slot, chamber, snapshot, repro, now);
        }
        return cover(level, bank, chambers, slot, chamber, snapshot, record, repro, now);
    }

    // ------------------------------------------------------------------
    // Carrying, and coming out to foal
    // ------------------------------------------------------------------

    /**
     * She is in foal. Three possibilities: an early loss is due, the birth is
     * due, or there is nothing to do but wait.
     */
    private static boolean carry(ServerLevel level, BlockPos pos, HorseStasisBankBlockEntity bank, int slot,
                                 ItemStack chamber, StasisSnapshot snapshot, Reproduction repro, long now) {
        Pregnancy pregnancy = repro.pregnancy().orElse(null);
        if (pregnancy == null) {
            return false;
        }

        if (pregnancy.earlyLossDue(now)) {
            // A lethal embryo, lost in the first third - the same loss
            // ReproHandler applies to a live mare, minus the chat line, because
            // there is nobody standing there and the chamber is not the place to
            // find out. The horse's own record still carries what happened.
            Optional<Pregnancy> after = pregnancy.afterEarlyLoss();
            if (!write(chamber, snapshot, repro.withPregnancy(after))) {
                return false;
            }
            ActionTrace.log("stasis", snapshot.horseName() + " lost " + pregnancy.lostEarlyCount()
                    + " of " + pregnancy.embryos().size() + " embryo(s) in a chamber"
                    + (after.isPresent() ? " - the other is still growing" : ""));
            return true;
        }

        if (!pregnancy.due(now)) {
            return false;
        }

        // Due. She has to be an animal for this - see the class note.
        if (HorseStasisHandler.alreadyLoose(level.getServer(), snapshot)) {
            // Only reachable by copying a full chamber in creative. Two entities
            // on one id would corrupt every pedigree that names her.
            return false;
        }
        Vec3 spot = foalingSpot(level, pos);
        if (spot == null) {
            bank.setFoalingBlocked(true);
            return false;
        }
        Horse mare = HorseStasisHandler.release(level, spot, 0.0F, snapshot);
        if (mare == null) {
            return false;
        }
        chamber.remove(ModDataComponents.STASIS_SNAPSHOT.get());
        chamber.remove(ModDataComponents.STASIS_AT_STUD.get());
        bank.setFoalingBlocked(false);
        level.playSound(null, mare.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7F, 1.2F);
        ActionTrace.log("stasis", snapshot.horseName() + " came out of a bank at her due date"
                + " (tick " + pregnancy.dueTick() + ") - ReproHandler foals her on its next scan");
        return true;
    }

    /** A place beside the bank a mare can be put down. {@code null} if there is none. */
    private static @Nullable Vec3 foalingSpot(ServerLevel level, BlockPos pos) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(face);
            if (standable(level, side)) {
                return centre(side);
            }
        }
        for (int dx = -FOALING_REACH; dx <= FOALING_REACH; dx++) {
            for (int dz = -FOALING_REACH; dz <= FOALING_REACH; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos at = pos.offset(dx, dy, dz);
                    if (!at.equals(pos) && standable(level, at)) {
                        return centre(at);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Room for a horse: this block and the one above it are both free of
     * collision. A horse is under two blocks tall, so two is the honest check -
     * and putting one down inside a wall is how a released animal ends up
     * suffocating in a cellar.
     */
    private static boolean standable(ServerLevel level, BlockPos at) {
        return level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty();
    }

    private static Vec3 centre(BlockPos at) {
        return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    // ------------------------------------------------------------------
    // Covering
    // ------------------------------------------------------------------

    /** She is not carrying. Is she in heat, and is there anything here to put to her? */
    private static boolean cover(ServerLevel level, HorseStasisBankBlockEntity bank, Container chambers, int slot,
                                 ItemStack chamber, StasisSnapshot snapshot, HorseRecord record,
                                 Reproduction repro, long now) {
        ReproTiming timing = ServerConfig.reproTiming();
        CompoundTag horse = snapshot.horse();
        float health = StasisCare.health(horse);
        float maxHealth = StasisCare.maxHealth(horse, record);

        StasisMating.Verdict verdict = StasisMating.mareVerdict(record, repro, health, maxHealth, now, timing);
        if (verdict != StasisMating.Verdict.READY) {
            return false;
        }

        int sireSlot = findSire(chambers, slot, snapshot.horseId());
        if (sireSlot < 0) {
            return false;
        }
        ItemStack sireChamber = chambers.getItem(sireSlot);
        StasisSnapshot sireSnapshot = StasisChamberItem.snapshotOf(sireChamber);
        HorseRecord sireRecord = sireSnapshot == null ? null : StasisCare.record(sireSnapshot.horse());
        if (sireSnapshot == null || sireRecord == null) {
            return false;
        }
        Reproduction sireRepro = StasisCare.repro(sireSnapshot.horse(), sireSnapshot.horseId());

        Rng rng = new NeoRng(level.getRandom());
        Conception.Result result = Conception.attempt(
                StasisMating.mating(record, sireRecord, ""), repro, now, timing,
                sireRepro.coversOn(now, timing.dayTicks()),
                ServerConfig.healthGeneticsActive(), ServerConfig.lethalsActive(), rng);

        // HER ONE TRY THIS HEAT IS SPENT EITHER WAY. That is what makes a bank a
        // paddock rather than a machine: a cover that did not take waits for her
        // next heat, exactly as it does in a field.
        Reproduction after = repro.withNaturalTry(now);
        if (result.pregnancy().isPresent()) {
            after = after.withPregnancy(result.pregnancy().get());
        }
        if (!write(chamber, snapshot, after)) {
            return false;
        }
        // ...and his day's work is counted, on the same rule ReproHandler uses:
        // an attempt on a mare who was receptive counts, and she was.
        if (write(sireChamber, sireSnapshot, sireRepro.withCover(now, timing.dayTicks()))) {
            chambers.setChanged();
        }

        ActionTrace.log("stasis", snapshot.horseName() + " was covered in a bank by "
                + sireRecord.displayName() + ": " + result.outcome()
                + String.format(" (chance %.2f)", result.chance())
                + result.pregnancy().map(p -> " - due at tick " + p.dueTick()).orElse(""));
        return true;
    }

    /**
     * The first chamber in this bank that could sire a foal on her: at stud, a
     * Spacer, occupied, an entire male with a genome, and not her.
     *
     * <p>First rather than best, and first rather than random, on purpose - a
     * player who wants a particular stallion marks that one and no other, which
     * is a control they already have. Choosing for them would take it away.
     */
    private static int findSire(Container chambers, int mareSlot, java.util.UUID mareId) {
        for (int i = 0; i < chambers.getContainerSize(); i++) {
            if (i == mareSlot) {
                continue;
            }
            ItemStack stack = chambers.getItem(i);
            if (!atStud(stack)) {
                continue;
            }
            StasisSnapshot snapshot = StasisChamberItem.snapshotOf(stack);
            if (snapshot == null || snapshot.horseId().equals(mareId)) {
                continue;
            }
            HorseRecord record = StasisCare.record(snapshot.horse());
            if (record == null) {
                continue;
            }
            CompoundTag horse = snapshot.horse();
            if (StasisMating.canSire(record, StasisCare.health(horse),
                    StasisCare.maxHealth(horse, record))) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // The mark, and the write
    // ------------------------------------------------------------------

    /**
     * <b>Is this chamber turned out into the bank's paddock?</b> The mark is a
     * component on the item, so it rides with the chamber and needs no syncing
     * of its own; the tier is asked again here rather than trusted, because a
     * mark can outlive the chamber it was set on - an upgrade recipe copies
     * components, and so would a downgrade if one ever existed.
     */
    public static boolean atStud(ItemStack chamber) {
        StasisTier tier = HorseStasisBankBlockEntity.tierOf(chamber);
        return tier != null && tier.breedsInBank()
                && Boolean.TRUE.equals(chamber.get(ModDataComponents.STASIS_AT_STUD.get()));
    }

    /** Write a reproductive record back into a chamber, as a fresh snapshot. */
    private static boolean write(ItemStack chamber, StasisSnapshot snapshot, Reproduction repro) {
        CompoundTag mended = snapshot.horse().copy();
        if (!StasisCare.putRepro(mended, repro)) {
            return false;
        }
        chamber.set(ModDataComponents.STASIS_SNAPSHOT.get(),
                new StasisSnapshot(snapshot.horseName(), snapshot.horseId(), mended));
        return true;
    }
}
