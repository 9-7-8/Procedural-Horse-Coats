package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.horse.HorseRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * What the cowboy asks in emeralds for one of their horses.
 *
 * <h2>Cheap by default</h2>
 * A horse is the <b>early-game</b> purchase this mod is built around - you buy
 * one before you have a farm, not after you have a trading hall - so the
 * default is {@link #DEFAULT_PRICE}, a couple of emeralds, and almost every
 * breed keeps it. The old table priced off {@code Commonness} and ran from ten
 * emeralds to forty, which made the cowboy a shop you walked past for a week.
 *
 * <h2>What a breed may say</h2>
 * A breed that should be dear names its own {@link Breed.PriceRange}
 * ({@code Breed.Builder#price}); everything that does not is
 * {@link #DEFAULT_PRICE}. The <b>range</b> and not the number is deliberate:
 * two horses of a breed are not worth the same, and a stall that lists six
 * papers at exactly two emeralds each reads as a vending machine.
 *
 * <p>The roll inside the range is taken from the <b>horse's own id</b>, so it
 * is the same every time the offer list is rebuilt. {@code Cowboy.updateTrades}
 * rebuilds from scratch on every look, and a price that moved between one
 * glance at the merchant screen and the next would look like the man was
 * haggling with themselves.
 *
 * <p>They only ever stocks pure breeds, so the cross / mixed / feral branches are
 * a floor rather than a real case; they exist because {@link HorseRecord} can
 * carry any label and a hard failure over a price is a bad trade for
 * robustness.
 */
public final class HorsePrices {

    /**
     * What a horse costs when its breed does not say otherwise: one to three
     * emeralds. Owner's call - most breeds should sit here.
     */
    public static final Breed.PriceRange DEFAULT_PRICE = new Breed.PriceRange(1, 3);

    /**
     * What an arcane dealer asks, whatever is standing in front of you: twelve
     * to twenty-eight emeralds, against one to three for an ordinary horse.
     *
     * <p>Dear enough to be a trip you save up for rather than a stop on the way
     * past - a player clearing out his string is spending most of a day's
     * trading - and still inside {@link #MAX_EMERALDS}, so the offer renders.
     */
    public static final Breed.PriceRange ARCANE_PRICE = new Breed.PriceRange(12, 28);

    /** Vanilla's merchant screen cannot show a cost above one stack. */
    private static final int MAX_EMERALDS = 64;

    private HorsePrices() {
    }

    /**
     * Emeralds for one signed transfer paper naming this horse. A gelding goes
     * for three quarters of it, never under one emerald: a riding horse, not a
     * line to breed from.
     */
    public static int emeraldsFor(HorseRecord record) {
        return emeraldsFor(record, false);
    }

    /**
     * What a dealer asks for this horse. {@code arcane} is the man, not the
     * animal: a dealer who sells magic asks {@link #ARCANE_PRICE} for anything
     * off his string.
     *
     * <p>The markup lives here, at the counter, and deliberately not on the
     * horse. §4 of the philosophy is that nothing in the model scores a horse -
     * there is no rarity tier and no "this is the good one" - so a horse's worth
     * is a fact about who is selling it, exactly like every other price in this
     * class. A horse with eleven magical loci that a player bred themselves is
     * worth nothing in particular, and that is correct.
     */
    public static int emeraldsFor(HorseRecord record, boolean arcane) {
        Breed.PriceRange range = arcane ? ARCANE_PRICE : rangeFor(record.lineage());
        int price = roll(range, record.id());
        return record.gelded() ? Math.max(1, price * 3 / 4) : price;
    }

    private static Breed.PriceRange rangeFor(BreedLineage lineage) {
        return switch (lineage.kind()) {
            case PURE -> priceOf(Breeds.get(lineage.components().get(0)));
            // A cross is worth what the dearer of its two lines is worth: an
            // Akhal-Teke cross is not a common horse, whichever way you read it.
            case CROSS -> dearer(
                    priceOf(Breeds.get(lineage.components().get(0))),
                    priceOf(Breeds.get(lineage.components().get(1))));
            // A spliced line is priced off the breed it was spliced from, and
            // not a copper more. What a splice buys you is a horse that looks
            // remarkable; what it costs you is the claim that you bred it, and
            // a dealer who can read a pedigree is exactly who notices.
            case SPLICED -> lineage.components().size() == 1
                    ? priceOf(Breeds.get(lineage.components().get(0)))
                    : dearer(priceOf(Breeds.get(lineage.components().get(0))),
                            priceOf(Breeds.get(lineage.components().get(1))));
            // A magical herd's horse is priced as its breed: the magic is a find, not a pedigree.
            case MAGICAL -> priceOf(Breeds.get(lineage.components().get(0)));
            case MIXED, FERAL -> DEFAULT_PRICE;
        };
    }

    private static Breed.PriceRange priceOf(Breed breed) {
        Optional<Breed.PriceRange> named = breed.price();
        return named.orElse(DEFAULT_PRICE);
    }

    private static Breed.PriceRange dearer(Breed.PriceRange a, Breed.PriceRange b) {
        return a.max() >= b.max() ? a : b;
    }

    /**
     * A price inside the range, fixed for the life of this horse.
     *
     * <p>Mixed off both halves of the id rather than {@code UUID.hashCode()},
     * which folds the two longs together with XOR and hands back something that
     * is not well spread in its low bits - and the low bits are all a range of
     * three ever looks at.
     */
    private static int roll(Breed.PriceRange range, UUID id) {
        long mixed = id.getMostSignificantBits() * 0x9E3779B97F4A7C15L
                + id.getLeastSignificantBits() * 0xC2B2AE3D27D4EB4FL;
        mixed ^= mixed >>> 29;
        int span = range.max() - range.min() + 1;
        int price = range.min() + (int) Math.floorMod(mixed, (long) span);
        return Math.min(MAX_EMERALDS, price);
    }
}
