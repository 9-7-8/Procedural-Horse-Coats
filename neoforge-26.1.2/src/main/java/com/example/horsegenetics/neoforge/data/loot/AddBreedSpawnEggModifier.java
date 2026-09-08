package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.neoforge.item.BreedSpawnEggItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Chest-loot injection of <b>breed spawn eggs</b>. With probability
 * {@code chance} it adds one egg for a breed drawn weighted by that breed's own
 * commonness ({@link BreedPool}), to whatever loot the datapack JSON's
 * conditions match.
 *
 * <p>The twin of {@link AddResearchPaperModifier}, and the reason both exist
 * rather than one parameterised thing: a paper is knowledge and an egg is an
 * animal, they belong in different chests at different rates, and a datapack
 * ought to be able to turn one off without the other.
 *
 * <p>A breed egg is the only way to get a foundation horse of a breed that does
 * not live near you, so this is deliberately dungeon-flavoured loot rather than
 * village loot: it should read as a find, not as a supply.
 */
public class AddBreedSpawnEggModifier extends LootModifier {

    public static final MapCodec<AddBreedSpawnEggModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(inst.group(
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("chance", 0.06F)
                            .forGetter(m -> m.chance),
                    com.mojang.serialization.Codec.STRING.optionalFieldOf("commonest", "extremely_common")
                            .forGetter(m -> m.commonest.name().toLowerCase(java.util.Locale.ROOT)),
                    com.mojang.serialization.Codec.STRING.optionalFieldOf("rarest", "very_rare")
                            .forGetter(m -> m.rarest.name().toLowerCase(java.util.Locale.ROOT))
            )).apply(inst, AddBreedSpawnEggModifier::new));

    private final float chance;
    private final Commonness commonest;
    private final Commonness rarest;

    public AddBreedSpawnEggModifier(LootItemCondition[] conditions, int priority,
                                    float chance, String commonest, String rarest) {
        super(conditions, priority);
        this.chance = chance;
        this.commonest = Commonness.valueOf(commonest.toUpperCase(java.util.Locale.ROOT));
        this.rarest = Commonness.valueOf(rarest.toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource rng = context.getRandom();
        if (rng.nextFloat() >= chance) {
            return loot;
        }
        Breed breed = BreedPool.draw(rng, commonest, rarest);
        if (breed == null) {
            return loot;
        }
        loot.add(BreedSpawnEggItem.of(breed));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
