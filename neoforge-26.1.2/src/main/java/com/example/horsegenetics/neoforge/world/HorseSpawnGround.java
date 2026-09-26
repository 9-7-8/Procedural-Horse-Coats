package com.example.horsegenetics.neoforge.world;

import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.SpawnGround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * <b>Lets a breed spawn on the floor it actually lives on.</b> The host half of
 * {@link SpawnGround}: widens {@link EntityType#HORSE}'s placement rule so a
 * breed that named extra floor blocks can be founded on them.
 *
 * <h2>What was wrong</h2>
 *
 * <p>Vanilla registers the horse with {@code Animal::checkAnimalSpawnRules},
 * which is two tests against the <i>position</i>:
 *
 * <pre>{@code
 * level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && brightEnoughToSpawn
 * }</pre>
 *
 * <p>and {@code #minecraft:animals_spawnable_on} in this SDK holds exactly one
 * block, {@code minecraft:grass_block}. A breed file naming a biome only ever
 * got as far as {@code BreedHerdsBiomeModifier} adding the spawn entry there;
 * whether a horse could stand up in it was never the breed's to say. Generated
 * Nether terrain has no grass block at all, so the Netherhorse and the Nightmare
 * named five biomes each, were added to all ten, and spawned in none of them -
 * not rarely, never. The breed egg was the only route to either animal.
 *
 * <h2>Why OR and not REPLACE</h2>
 *
 * <p>{@link RegisterSpawnPlacementsEvent.Operation#OR} leaves vanilla's rule
 * running and adds an alternative beside it, so this can only ever <i>add</i> a
 * legal position. Lit grass keeps working for every horse everywhere, including
 * for a breed that named extra floors; a world with no breed naming any floor
 * behaves exactly as before, because {@link Breeds#wildGroundAllows} returns
 * false for every position. {@code REPLACE} would have made this class
 * responsible for reimplementing the vanilla rule correctly, which is a much
 * worse bargain for the same result.
 *
 * <p><b>It is per-position, so it is on the hot path.</b> Every creature spawn
 * attempt anywhere reaches this. The biome lookup is a {@code Holder} the caller
 * already has, and {@link Breeds#wildGroundAllows} walks the registry - so the
 * cheap test comes first: a biome no wild breed named a floor for is rejected
 * before anything is looked up.
 *
 * <p><b>API note:</b> a {@code SpawnPredicate} takes a
 * {@code ServerLevelAccessor}, not a {@code LevelAccessor}, and this SDK keeps
 * the horse classes in {@code net.minecraft.world.entity.animal.equine} rather
 * than {@code ...animal.horse} - see {@code wiki/api-notes.html}.
 *
 * <p><b>Unverified:</b> that {@code EntitySpawnReason.ignoresLightRequirements}
 * is the right reading of "bright" for the OR branch. Vanilla computes it as
 * {@code ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos)};
 * this mirrors that rather than assuming the light matters for a command-placed
 * or structure-placed horse, which would be stricter than vanilla is on grass.
 */
public final class HorseSpawnGround {

    public static void listen(IEventBus modEventBus) {
        modEventBus.addListener(RegisterSpawnPlacementsEvent.class, HorseSpawnGround::onRegister);
    }

    private static void onRegister(RegisterSpawnPlacementsEvent event) {
        event.register(EntityType.HORSE, HorseSpawnGround::breedFloorAllows,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    /**
     * The extra permission: is the block under {@code pos} one a wild breed of
     * this biome named, at a light that breed accepts?
     */
    private static boolean breedFloorAllows(EntityType<Horse> type, ServerLevelAccessor level,
                                            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        String biome = biomeId(level.getBiome(pos));
        if (biome.isEmpty() || Breeds.wildFloors(biome).isEmpty()) {
            return false;
        }
        String floor = blockId(level, pos.below());
        boolean bright = EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8;
        return Breeds.wildGroundAllows(biome, floor, bright);
    }

    private static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    private static String blockId(ServerLevelAccessor level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private HorseSpawnGround() {
    }
}
