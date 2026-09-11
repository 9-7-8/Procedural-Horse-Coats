package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedSpawnSettings;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.breed.SpawnTime;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <b>{@code .minecraft/phc/breed-spawning.toml}</b> - where, how often and
 * whether each breed the mod ships spawns, a switch for all of them at once,
 * and the same for Feral Mixed. Read into {@link BreedSpawnSettings} and handed
 * to {@link Breeds#applySpawnSettings}; everything that spawns a horse asks the
 * registry, so nothing else needs to know this file exists.
 *
 * <h2>Why the file is generated from the registry</h2>
 * One section per shipped breed, each defaulting to what that breed's own file
 * says - its weight, its biomes, its hours - so the player edits numbers they
 * can see rather than writing overrides from memory. The spec is built in the
 * mod constructor, after {@link ModBreedSpecs#load()}, because only then is the
 * list complete. A player's drop-in breeds are not listed: they are their own
 * files already.
 *
 * <p>The cost of restating defaults is that a file written by one version keeps
 * that version's values when a later one changes a breed. Deleting a breed's
 * section puts the current defaults back (NeoForge rewrites a missing section).
 *
 * <h2>COMMON, not SERVER</h2>
 * The file is read at startup, before any server exists, which is what the
 * herd biome modifier needs: biome modifiers run once, at server start, and ask
 * the registry which biomes have wild breeds. A SERVER config would also be
 * synced to every client - carrying one section per breed the <i>server</i>
 * ships and drops in, into a client whose own spec was built from its own
 * breeds - for no benefit, since only the server spawns horses. A dedicated
 * server reads its own copy.
 *
 * <p>Edits while the game runs are re-read (NeoForge watches the file) and
 * reach herds, the cowboy, stables and eggs at once; the biome spawn lists are
 * only rebuilt on the next server start.
 */
public final class BreedSpawningConfig {

    /** Beside the breeds folder, in {@code .minecraft/phc/} - see {@link ModBreedSpecs#configFile}. */
    public static final String FILE = ModBreedSpecs.configFile("breed-spawning.toml");

    /**
     * A mutable list on purpose: while writing a fresh file NeoForge tests a
     * missing value with {@code acceptable.contains(null)}, and an immutable
     * {@code List.of} throws on a null probe - a crash at startup, seen.
     */
    private static final List<String> SPAWN_TIMES = new ArrayList<>(java.util.Arrays.asList("any", "day", "night"));

    private static final Pattern BIOME_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private record Section(String id, ModConfigSpec.BooleanValue enabled, ModConfigSpec.DoubleValue weight,
                           ModConfigSpec.ConfigValue<List<? extends String>> biomes,
                           ModConfigSpec.ConfigValue<String> time) {
    }

    private static ModConfigSpec spec;
    private static ModConfigSpec.BooleanValue builtins;
    private static ModConfigSpec.BooleanValue feralEnabled;
    private static ModConfigSpec.ConfigValue<List<? extends String>> feralBiomes;
    private static ModConfigSpec.DoubleValue feralWeight;
    private static final List<Section> SECTIONS = new ArrayList<>();

    private BreedSpawningConfig() {
    }

    /** Build the spec from the shipped breeds. Call once, after {@link ModBreedSpecs#load()}. */
    public static ModConfigSpec build() {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Horse Genetics - which horses your world has.",
                        "",
                        "Every breed the mod ships is listed below with the values its own",
                        "breed file gives it; change any of them. Your own breeds (the .json",
                        "files in the breeds folder beside this file) are not listed - edit",
                        "those files instead. Changes reach herds, the cowboy, stables and",
                        "spawn eggs straight away; which biomes get wild herds at all is",
                        "decided when a world starts, so restart the world after moving a",
                        "breed to new biomes.",
                        "",
                        "A wild horse that has nothing it may be - no breed of its biome at",
                        "this hour, and no Feral Mixed - does not spawn. So switching off the",
                        "shipped breeds and Feral Mixed leaves a world with only your breeds,",
                        "and only where they live.")
                .push("general");
        builtins = b.comment("Whether ANY of the shipped breeds spawn. false switches them all off",
                        "at once - no wild herds, no cowboy stock, no stables, no spawn eggs -",
                        "whatever their own sections below say. Horses you already have keep",
                        "their breed. (default: true)")
                .define("shipped_breeds_enabled", true);
        b.pop();

        b.comment("Feral Mixed: the unbred horse. A lone wild horse is one, and so is a",
                        "herd in a biome where no breed may spawn at that hour. /summon and",
                        "vanilla spawn eggs always make one, whatever this says.")
                .push("feral_mixed");
        feralEnabled = b.comment("Whether a wild horse may be Feral Mixed at all. false: a wild horse",
                        "with no breed to be is not spawned. (default: true)")
                .define("enabled", true);
        feralBiomes = b.comment("The biomes a wild horse may be Feral Mixed in. Empty means every",
                        "biome. Use the ids F3 shows, e.g. \"minecraft:plains\". (default: [])")
                .defineListAllowEmpty("biomes", List.of(), () -> "minecraft:plains", BreedSpawningConfig::isBiomeId);
        feralWeight = b.comment("Its weight against the biome's breeds when a wild herd is founded,",
                        "on the same scale as spawn_weight below. 0 means Feral Mixed only",
                        "where no breed may spawn - how it has always been. (default: 0)")
                .defineInRange("herd_weight", 0.0, 0.0, 1000.0);
        b.pop();

        b.comment("One section per shipped breed.",
                        "  enabled      - false: this breed never spawns, from anywhere.",
                        "  spawn_weight - how often it heads a wild herd against the other",
                        "                 breeds of the same biome. 0 = never wild (the cowboy,",
                        "                 stables and eggs still have it). The commonness names",
                        "                 in breed files are these weights:",
                        "                 " + commonnessScale(),
                        "  biomes       - where its wild herds live. Modded ids work.",
                        "  spawn_time   - \"any\", \"day\" or \"night\": when a herd may be founded.")
                .push("breeds");
        SECTIONS.clear();
        for (Breed breed : Breeds.shipped()) {
            b.comment(breed.name()).push(breed.id());
            SECTIONS.add(new Section(breed.id(),
                    b.define("enabled", true),
                    b.defineInRange("spawn_weight", breed.spawnWeight(), 0.0, 1000.0),
                    b.defineListAllowEmpty("biomes", breed.biomes(), () -> "minecraft:plains",
                            BreedSpawningConfig::isBiomeId),
                    b.defineInList("spawn_time", breed.spawnTime().id(), SPAWN_TIMES)));
            b.pop();
        }
        b.pop();
        spec = b.build();
        return spec;
    }

    private static boolean isBiomeId(Object o) {
        return o instanceof String s && BIOME_ID.matcher(s).matches();
    }

    private static String commonnessScale() {
        StringBuilder sb = new StringBuilder();
        for (Commonness c : Commonness.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(c.name().toLowerCase(Locale.ROOT)).append(' ').append(trim(c.weight));
        }
        return sb.toString();
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /** Mod bus: the file was read (startup) or edited (file watcher). */
    static void onConfig(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading || spec == null || event.getConfig().getSpec() != spec) {
            return;
        }
        Map<String, BreedSpawnSettings.BreedOverride> overrides = new LinkedHashMap<>();
        for (Section s : SECTIONS) {
            SpawnTime time = SpawnTime.byId(s.time().get());
            overrides.put(s.id(), new BreedSpawnSettings.BreedOverride(
                    s.enabled().get(), s.weight().get(), strings(s.biomes().get()), time));
        }
        BreedSpawnSettings.Feral feral = new BreedSpawnSettings.Feral(
                feralEnabled.get(), strings(feralBiomes.get()), feralWeight.get());
        BreedSpawnSettings settings = new BreedSpawnSettings(builtins.get(), overrides, feral);
        Breeds.applySpawnSettings(settings);
        HorseGenetics.LOGGER.info("[breeds] spawn settings read from {}: shipped breeds {}, {} switched off, Feral Mixed {}",
                event.getConfig().getFileName(), settings.builtinsEnabled() ? "on" : "OFF",
                overrides.values().stream().filter(o -> !o.enabled()).count(),
                feral.enabled() ? (feral.biomes().isEmpty() ? "everywhere" : "in " + feral.biomes().size() + " biome(s)") : "OFF");
    }

    private static List<String> strings(List<? extends String> in) {
        return new ArrayList<>(in);
    }

    /** For the mod constructor: register the file. */
    static void register(net.neoforged.fml.ModContainer container, net.neoforged.bus.api.IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, build(), FILE);
        modBus.addListener(ModConfigEvent.Loading.class, BreedSpawningConfig::onConfig);
        modBus.addListener(ModConfigEvent.Reloading.class, BreedSpawningConfig::onConfig);
    }
}
