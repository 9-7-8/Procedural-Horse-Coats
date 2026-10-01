package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.genes.MobRoster;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>What a Chaos seed can name</b> - the modded mobs this server has loaded, in
 * one sorted list per mob locus, built from the live entity registry.
 *
 * <p>The three mob loci ({@code LycanGene}, {@code PackLeaderGene},
 * {@code SpawnerGene}) keep their hand-written vanilla alleles; their one
 * {@code Cha} allele carries a seed on the copy, and this class turns that seed
 * into an {@link EntityType}: {@link MobRoster#chaosPick seed modulo the list
 * size}. See {@code MobRoster}'s Chaos section for why it is one allele and not
 * one per mob.
 *
 * <h2>Who is on each list</h2>
 * All three start from every registered entity type that is <b>not</b>
 * {@code minecraft:} or this mod's own, can be summoned, is not excluded by the server config
 * ({@code chaos.exclude_mods}, {@code chaos.exclude_ids}), and builds into a
 * {@link Mob} that is not a horse - a horse that becomes a horse at night is not
 * a gene, here as in the vanilla roster. Then:
 * <ul>
 *   <li>{@link Use#LYCAN} - only a body a horse can be found in again:
 *       {@link LycanthropyHandler#isWearable}, the same check the shift itself
 *       makes, so the two cannot disagree.</li>
 *   <li>{@link Use#PACK} - peaceful: not a {@code MONSTER} spawn category, and
 *       not hostile on inspection ({@link MobGroups#isHostile}). Swimmers and
 *       fliers stay in, as they do on the vanilla list - a follower walks
 *       <i>beside</i> the horse.</li>
 *   <li>{@link Use#SPAWNER} - any of them, monsters and bosses included. The
 *       config is how a server takes a boss out.</li>
 * </ul>
 *
 * <h2>Built once, lazily</h2>
 * Deciding the lists means constructing one instance of every modded entity
 * type, which is not free in a large pack, so it happens on the first Chaos
 * lookup rather than at startup - a world with no Chaos horse never pays. The
 * lists are kept until the exclude lists change. An entity type that throws
 * while being built is skipped and logged once; another mod's constructor is
 * not allowed to take a horse's tick down with it.
 *
 * <p><b>Unverified in game:</b> that every modded mob can be built with
 * {@link EntitySpawnReason#CONVERSION} outside a spawn (the Lycan shift already
 * builds vanilla mobs this way), and that a built-and-never-added entity leaves
 * nothing behind. Both are on {@code wiki/gene-lycan.html}'s Verification tab.
 */
public final class ChaosRoster {

    private ChaosRoster() {
    }

    /** Which locus is asking - each has its own rule. */
    public enum Use { LYCAN, PACK, SPAWNER }

    private static Map<Use, List<EntityType<?>>> lists;
    private static List<String> builtFor;
    private static final Set<String> WARNED = new HashSet<>();

    /**
     * The entity type {@code seed} names for {@code use}, or {@code null} when
     * the list is empty - no modded mob installed, or none that passes. A
     * {@code null} is a gene that does nothing, never an error.
     */
    public static synchronized EntityType<?> pick(Use use, long seed, ServerLevel level) {
        List<EntityType<?>> list = list(use, level);
        int i = MobRoster.chaosPick(seed, list.size());
        return i < 0 ? null : list.get(i);
    }

    /** The {@code namespace:path} id of what {@code seed} names, or {@code null}. */
    public static String pickId(Use use, long seed, ServerLevel level) {
        EntityType<?> type = pick(use, seed, level);
        return type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    /** {@code use}'s whole list, sorted by id. Built on first call. */
    public static synchronized List<EntityType<?>> list(Use use, ServerLevel level) {
        List<String> config = configKey();
        if (lists == null || !config.equals(builtFor)) {
            lists = build(level);
            builtFor = config;
        }
        return lists.get(use);
    }

    /** Log, once per key, that a Chaos allele had nothing to resolve to. */
    public static void warnEmpty(Use use, String horse) {
        if (WARNED.add("empty:" + use)) {
            HorseGenetics.LOGGER.info("[chaos] no modded animal available for {} - {} does nothing "
                    + "(no mod adds one that passes, or the config excludes them all)", use, horse);
        }
    }

    private static List<String> configKey() {
        List<String> key = new ArrayList<>(ServerConfig.chaosExcludeMods());
        key.add("|");
        key.addAll(ServerConfig.chaosExcludeIds());
        return key;
    }

    /**
     * <b>Which lists {@code type} belongs on</b>, by the rules in the class note -
     * asked of a real instance, so a modded mob answers for itself. Takes no
     * notice of namespace or config: that filtering is {@link #build}'s, which is
     * what lets the gametest put vanilla mobs through the same rules.
     */
    public static Set<Use> classify(EntityType<?> type, ServerLevel level) {
        Set<Use> uses = java.util.EnumSet.noneOf(Use.class);
        String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        Entity built;
        try {
            built = type.create(level, EntitySpawnReason.CONVERSION);
        } catch (Throwable broken) {
            if (WARNED.add("build:" + id)) {
                HorseGenetics.LOGGER.warn("[chaos] skipping {}: it could not be built ({})", id, broken.toString());
            }
            return uses;
        }
        if (!(built instanceof Mob mob) || mob instanceof AbstractHorse) {
            if (built != null) {
                built.discard();
            }
            return uses;
        }
        try {
            uses.add(Use.SPAWNER);
            if (type.getCategory() != MobCategory.MONSTER && !MobGroups.isHostile(mob)) {
                uses.add(Use.PACK);
            }
            if (LycanthropyHandler.isWearable(type, mob)) {
                uses.add(Use.LYCAN);
            }
        } catch (Throwable broken) {
            if (WARNED.add("classify:" + id)) {
                HorseGenetics.LOGGER.warn("[chaos] skipping {}: it could not be inspected ({})", id, broken.toString());
            }
            uses.clear();
        } finally {
            mob.discard();
        }
        return uses;
    }

    private static Map<Use, List<EntityType<?>>> build(ServerLevel level) {
        Set<String> excludedMods = new HashSet<>(ServerConfig.chaosExcludeMods());
        Set<String> excludedIds = new HashSet<>(ServerConfig.chaosExcludeIds());

        List<EntityType<?>> candidates = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            // Not vanilla (those have alleles of their own), not ours (carts and
            // their postilions are machinery, not animals), and nothing marked
            // unsummonable - an entity /summon refuses is an internal one.
            if (id == null || "minecraft".equals(id.getNamespace())
                    || HorseGenetics.MOD_ID.equals(id.getNamespace()) || !type.canSummon()
                    || excludedMods.contains(id.getNamespace()) || excludedIds.contains(id.toString())) {
                continue;
            }
            candidates.add(type);
        }
        // By id, so the same pack always gives the same list whatever order
        // the mods happened to register in.
        candidates.sort(Comparator.comparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()));

        Map<Use, List<EntityType<?>>> out = new EnumMap<>(Use.class);
        for (Use use : Use.values()) {
            out.put(use, new ArrayList<>());
        }
        for (EntityType<?> type : candidates) {
            for (Use use : classify(type, level)) {
                out.get(use).add(type);
            }
        }
        for (Use use : Use.values()) {
            out.put(use, List.copyOf(out.get(use)));
        }
        HorseGenetics.LOGGER.info("[chaos] modded mobs a Chaos allele can name: {} lycan, {} pack leader, {} spawner",
                out.get(Use.LYCAN).size(), out.get(Use.PACK).size(), out.get(Use.SPAWNER).size());
        return out;
    }
}
