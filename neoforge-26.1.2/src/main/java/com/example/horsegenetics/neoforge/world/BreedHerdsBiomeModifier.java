package com.example.horsegenetics.neoforge.world;

import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Breeds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * <b>Wild herds, wherever a breed lives.</b> Adds the herd-sized horse spawn to
 * the biomes listed in the modifier's own file <i>and</i> to every biome a
 * registered breed that may come {@link BreedSource#WILD wild} names.
 *
 * <p>This is what makes a drop-in breed a whole breed. A breed file lists its
 * biomes, and before this the herds were a fixed list of vanilla biomes in a
 * data file - so a breed that lived in a dark forest, a swamp or a modded biome
 * loaded, registered, turned up at the cowboy's and in spawn eggs, and was
 * never once seen wild. The herd <i>founder</i> already chose among the biome's
 * breeds ({@code HerdManager.pickHerdBreed}); what was missing was a horse there
 * to found.
 *
 * <p><b>Why a union and not two modifiers.</b> The listed biomes keep their
 * herds even where no breed lives (they become Feral Mixed herds, which is what
 * they always were), and a biome that is both listed and named by a breed is
 * given the spawn <b>once</b>. Two stock {@code add_spawns} modifiers would
 * double the weight wherever they overlapped.
 *
 * <p><b>When it runs.</b> Biome modifiers are applied when a server starts, and
 * the breed registry is complete long before that - {@code ModBreedSpecs.load()}
 * runs from the mod constructor. A breed dropped in while the game is running
 * is picked up on the next start, like everything else in that folder.
 *
 * <p>A horse still needs somewhere to stand: vanilla's animal spawn rule wants
 * grass underfoot and light, so a breed that names a desert or the Nether will
 * rarely or never be seen, whatever this adds.
 */
public record BreedHerdsBiomeModifier(HolderSet<Biome> biomes, int weight, int minCount, int maxCount)
        implements BiomeModifier {

    public static final MapCodec<BreedHerdsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BreedHerdsBiomeModifier::biomes),
            Codec.INT.fieldOf("weight").forGetter(BreedHerdsBiomeModifier::weight),
            Codec.INT.fieldOf("min_count").forGetter(BreedHerdsBiomeModifier::minCount),
            Codec.INT.fieldOf("max_count").forGetter(BreedHerdsBiomeModifier::maxCount)
    ).apply(i, BreedHerdsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }
        if (!biomes.contains(biome) && !aWildBreedLivesIn(biome)) {
            return;
        }
        builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE, weight,
                new MobSpawnSettings.SpawnerData(EntityType.HORSE, minCount, maxCount));
    }

    private static boolean aWildBreedLivesIn(Holder<Biome> biome) {
        String id = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        return !id.isEmpty() && !Breeds.forBiome(id, BreedSource.WILD).isEmpty();
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
