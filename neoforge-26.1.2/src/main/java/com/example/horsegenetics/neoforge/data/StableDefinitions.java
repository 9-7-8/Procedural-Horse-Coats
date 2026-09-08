package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.stable.StableSpawn;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The stable registry</b> - every generated stable building and the rule for
 * what lives in it, loaded from
 * {@code data/<namespace>/horsegenetics/stable/<id>.json}.
 *
 * <h2>The extension point</h2>
 * Adding a stable is <b>two files and no Java</b>, and that is the whole design:
 *
 * <ol>
 *   <li>a Minecraft structure NBT at
 *       {@code data/<ns>/structure/<id>.nbt}, plus the three vanilla worldgen
 *       files that place it ({@code worldgen/structure/<id>.json},
 *       {@code worldgen/structure_set/<id>.json},
 *       {@code worldgen/template_pool/<id>.json}) - all of which are ordinary
 *       datapack registries this mod has nothing to do with;</li>
 *   <li>a file here, naming that structure and saying what stands in it.</li>
 * </ol>
 *
 * <p>So the <b>generation</b> is vanilla's and the <b>horses</b> are this mod's,
 * and neither half knows about the other beyond a structure id. A pack that
 * ships only the worldgen files gets an empty building; one that ships only
 * this file is ignored until a structure by that id exists. Both failures are
 * quiet and neither breaks a world.
 *
 * <h2>The file</h2>
 * <pre>{@code
 * {
 *   "structure": "horsegenetics:horse_stable",
 *   "name": "Horse Stable",
 *   "min_horses": 7,
 *   "max_horses": 7,
 *   "field": 4,
 *   "breeds": ["friesian"],
 *   "magic": "none",
 *   "rare_natural_genes": 2,
 *   "rare_tier": "rare",
 *   "min_magic": 1,
 *   "max_magic": 11,
 *   "extra_magic_chance": 0.10,
 *   "extra_magic_falloff": 0.5
 * }
 * }</pre>
 * Everything but {@code structure} has a default, so the shortest useful stable
 * file is two lines and gets one ordinary feral horse. The fields, in the order
 * they take effect:
 * <ul>
 *   <li>{@code breeds} - draw from these, or the feral mix when empty;</li>
 *   <li>{@code magic} - {@code none} clears every magical locus,
 *       {@code no_coat} clears only the ones that paint, {@code allowed}
 *       leaves the breed's own draw alone;</li>
 *   <li>{@code rare_natural_genes} / {@code rare_tier} - how many natural loci
 *       at or above that tier to force into a state that shows;</li>
 *   <li>{@code min_magic} .. {@code max_magic} with
 *       {@code extra_magic_chance} and {@code extra_magic_falloff} - the
 *       homozygous magic ladder. A stable that names any of these <b>owns</b>
 *       the horse's magic: the loci are cleared and exactly the rolled number
 *       put back, so the ladder describes the horse and not an addition to it.</li>
 * </ul>
 *
 * <p>{@code field} is how many of the horses stand <b>outside</b> under the sky;
 * the rest go in stalls. It is a target and not a promise - a building with two
 * stalls and a request for three gets two, and the overflow goes to the field
 * ({@code server/StablePopulator} owns that fallback).
 */
@EventBusSubscriber
public final class StableDefinitions {

    /** Where a pack puts one. Relative to {@code data/<namespace>/}. */
    public static final String DIRECTORY = "horsegenetics/stable";

    private static final Codec<StableSpawn.Magic> MAGIC_CODEC =
            Codec.STRING.xmap(StableDefinitions::magicOf, m -> m.name().toLowerCase(java.util.Locale.ROOT));

    private static StableSpawn.Magic magicOf(String s) {
        return switch (s.toLowerCase(java.util.Locale.ROOT)) {
            case "none" -> StableSpawn.Magic.NONE;
            case "no_coat", "no-coat" -> StableSpawn.Magic.NO_COAT;
            default -> StableSpawn.Magic.ALLOWED;
        };
    }

    /**
     * Flattened rather than nested to match {@link StableSpawn}'s shape, and
     * grouped in the <i>file</i> by the two sub-objects the schema shows. A
     * codec per sub-object would be three codecs for a record with no
     * sub-records in it.
     */
    public static final Codec<StableSpawn> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("structure").forGetter(StableSpawn::structure),
            Codec.STRING.optionalFieldOf("name", "Stable").forGetter(StableSpawn::name),
            Codec.INT.optionalFieldOf("min_horses", 1).forGetter(StableSpawn::minHorses),
            Codec.INT.optionalFieldOf("max_horses", 1).forGetter(StableSpawn::maxHorses),
            Codec.INT.optionalFieldOf("field", 0).forGetter(StableSpawn::fieldHorses),
            Codec.STRING.listOf().optionalFieldOf("breeds", List.of()).forGetter(StableSpawn::breeds),
            MAGIC_CODEC.optionalFieldOf("magic", StableSpawn.Magic.ALLOWED).forGetter(StableSpawn::magic),
            Codec.INT.optionalFieldOf("rare_natural_genes", 0).forGetter(StableSpawn::rareNaturalGenes),
            Codec.STRING.optionalFieldOf("rare_tier", "rare")
                    .forGetter(s -> s.rareTier().name().toLowerCase(java.util.Locale.ROOT)),
            Codec.INT.optionalFieldOf("min_magic", 0).forGetter(StableSpawn::minMagic),
            Codec.INT.optionalFieldOf("max_magic", 0).forGetter(StableSpawn::maxMagic),
            Codec.DOUBLE.optionalFieldOf("extra_magic_chance", 0.10).forGetter(StableSpawn::extraMagicChance),
            Codec.DOUBLE.optionalFieldOf("extra_magic_falloff", 0.5).forGetter(StableSpawn::extraMagicFalloff)
    ).apply(i, (structure, name, min, max, field, breeds, magic, rare, tier,
                minMagic, maxMagic, chance, falloff) ->
            new StableSpawn(structure, name, min, max, field, breeds, magic, rare,
                    GeneRarity.fromString(tier), minMagic, maxMagic, chance, falloff)));

    /** structure id -> the rule. Server-side, replaced wholesale on every reload. */
    private static volatile Map<String, StableSpawn> byStructure = Map.of();

    private StableDefinitions() {
    }

    /** The rule for this structure, or {@code null} if it is not a stable. */
    public static StableSpawn forStructure(Identifier structure) {
        return structure == null ? null : byStructure.get(structure.toString());
    }

    public static Map<String, StableSpawn> all() {
        return byStructure;
    }

    @SubscribeEvent
    static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "stables"), new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener<StableSpawn> {

        private Loader() {
            super(CODEC, net.minecraft.resources.FileToIdConverter.json(DIRECTORY));
        }

        @Override
        protected void apply(Map<Identifier, StableSpawn> parsed, ResourceManager resources, ProfilerFiller profiler) {
            Map<String, StableSpawn> out = new HashMap<>();
            for (Map.Entry<Identifier, StableSpawn> entry : parsed.entrySet()) {
                StableSpawn spawn = entry.getValue();
                StableSpawn clash = out.put(spawn.structure(), spawn);
                if (clash != null) {
                    // Two files claiming one building is a pack conflict, not a
                    // crash. Say which, and take the last - the same thing every
                    // other datapack registry does.
                    HorseGenetics.LOGGER.warn("[Stables] {} is claimed by two stable definitions; "
                            + "using the one from {}", spawn.structure(), entry.getKey());
                }
            }
            byStructure = Map.copyOf(out);
            HorseGenetics.LOGGER.info("[Stables] loaded {} stable definition(s)", out.size());
        }
    }

    /** Parse one file by hand - only the data generator and the tests need this. */
    public static StableSpawn parse(JsonElement json) {
        return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
