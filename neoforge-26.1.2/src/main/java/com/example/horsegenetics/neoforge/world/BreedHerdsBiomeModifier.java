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
 * breeds ({@code HerdManager.pickWildBreed}); what was missing was a horse there
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
 * <p><b>A world's breed settings count.</b> A shipped breed moved to other
 * biomes, or switched off, in {@code phc/breed-spawning.toml} brings its herds
 * with it, because this asks the registry after the settings are applied. A
 * change made while a server is running reaches the herds on its next start.
 *
 * <p>A horse still needs somewhere to stand, and until {@code spawn_ground}
 * existed that was vanilla's business alone: its animal rule wants grass
 * underfoot and light, so a breed that named a desert or the Nether was added
 * here and then never seen. A breed may now name the floors it lives on - see
 * {@code SpawnGround} and {@code HorseSpawnGround} - which is what makes the
 * two counts below matter.
 *
 * <p><b>Why two weights.</b> A biome on the list is horse country: plains,
 * savanna, a river bank, three to six at a time. A biome reached only because a
 * breed named it is not - it is wherever that one breed lives, often somewhere
 * the list deliberately left out, and a herd of six there reads as a pasture
 * rather than a find. The Nether is the case that forced the split: it has
 * almost no other land {@code CREATURE} entry, so this modifier's weight is
 * very nearly the whole draw, and the overworld's 10 / 3-6 would have made two
 * <i>very rare</i> breeds the commonest animal down there. {@code breed_weight}
 * and its counts are what an off-list biome gets instead. They are deliberately
 * one number for all of them rather than per-breed: how thick a breed is on the
 * ground is {@code commonness}, which the founder draw already reads, and this
 * is only how often the game asks the question at all.
 */
public record BreedHerdsBiomeModifier(HolderSet<Biome> biomes, int weight, int minCount, int maxCount,
                                      int breedWeight, int breedMinCount, int breedMaxCount)
        implements BiomeModifier {

    public static final MapCodec<BreedHerdsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BreedHerdsBiomeModifier::biomes),
            Codec.INT.fieldOf("weight").forGetter(BreedHerdsBiomeModifier::weight),
            Codec.INT.fieldOf("min_count").forGetter(BreedHerdsBiomeModifier::minCount),
            Codec.INT.fieldOf("max_count").forGetter(BreedHerdsBiomeModifier::maxCount),
            Codec.INT.fieldOf("breed_weight").forGetter(BreedHerdsBiomeModifier::breedWeight),
            Codec.INT.fieldOf("breed_min_count").forGetter(BreedHerdsBiomeModifier::breedMinCount),
            Codec.INT.fieldOf("breed_max_count").forGetter(BreedHerdsBiomeModifier::breedMaxCount)
    ).apply(i, BreedHerdsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }
        // The listed biomes are Feral Mixed country; a world that has switched
        // Feral Mixed off there gets no extra horses from the list, only from
        // the breeds that live in it. (Settings are read before this runs:
        // COMMON configs load at startup, biome modifiers at server start.)
        boolean onTheList = biomes.contains(biome);
        boolean feralHerds = onTheList && Breeds.spawnSettings().feral().allowedIn(biomeId(biome));
        if (!feralHerds && !aWildBreedLivesIn(biome)) {
            return;
        }
        // A listed biome is horse country at the full rate even when a breed also
        // names it - the two used to be one number, and it is the list that says
        // "three to six here". Only a biome reached purely through a breed gets
        // the quieter one; see the class note on why they differ.
        boolean listRate = onTheList;
        builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
                listRate ? weight : breedWeight,
                new MobSpawnSettings.SpawnerData(EntityType.HORSE,
                        listRate ? minCount : breedMinCount,
                        listRate ? maxCount : breedMaxCount));
    }

    private static boolean aWildBreedLivesIn(Holder<Biome> biome) {
        String id = biomeId(biome);
        return !id.isEmpty() && !Breeds.forBiome(id, BreedSource.WILD).isEmpty();
    }

    private static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
