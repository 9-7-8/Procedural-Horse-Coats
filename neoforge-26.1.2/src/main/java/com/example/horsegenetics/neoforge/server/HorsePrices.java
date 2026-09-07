package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.horse.HorseRecord;

/**
 * What the cowboy asks in emeralds for one of his horses.
 *
 * <p>The one place the <b>breed rarity &rarr; price</b> table lives, kept on
 * the Minecraft side for the same reason {@code RarityItems} is: the currency
 * is a gameplay-economy decision, not a fact about a breed, and a breed
 * definition has no business naming a price. Retuning is a one-line change
 * here.
 *
 * <p>Priced off {@link Commonness}, which is the same axis the wild herd roll
 * uses - so the horse you would have to search half a continent for is the one
 * that costs the most to buy, and the price ladder cannot drift away from the
 * rarity ladder.
 *
 * <p>He only ever stocks pure breeds, so the cross / mixed / feral branches are
 * a floor rather than a real case; they exist because {@link HorseRecord} can
 * carry any label and a hard failure over a price is a bad trade for
 * robustness.
 */
public final class HorsePrices {

    /** Vanilla's merchant screen cannot show a cost above one stack. */
    private static final int MAX_EMERALDS = 64;

    private HorsePrices() {
    }

    /** Emeralds for one signed transfer paper naming this horse. */
    public static int emeraldsFor(HorseRecord record) {
        return Math.min(MAX_EMERALDS, forLineage(record.lineage()));
    }

    private static int forLineage(BreedLineage lineage) {
        return switch (lineage.kind()) {
            case PURE -> forBreed(Breeds.get(lineage.components().get(0)));
            // A cross is worth the dearer of its two lines, less a little for
            // not being either of them.
            case CROSS -> Math.max(
                    forBreed(Breeds.get(lineage.components().get(0))),
                    forBreed(Breeds.get(lineage.components().get(1)))) - 4;
            case MIXED, FERAL -> forCommonness(Commonness.COMMON);
        };
    }

    private static int forBreed(Breed breed) {
        return forCommonness(Commonness.forWeight(breed.spawnWeight()));
    }

    private static int forCommonness(Commonness commonness) {
        return switch (commonness) {
            case EXTREMELY_COMMON -> 10;
            case VERY_COMMON -> 12;
            case COMMON -> 15;
            case MODERATE -> 18;
            case UNCOMMON -> 24;
            case RARE -> 32;
            case VERY_RARE -> 40;
        };
    }
}
