package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.SpecAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The <b>translator</b> for a data-driven gene's {@code effects} block - the
 * NeoForge half of {@link GeneAbility}. Every tick it reads the abilities a
 * horse's genotype expresses ({@link SpecAbilities#activeFor}) and turns each
 * one into game state: a traversal flag held up, a particle emitter fired, a
 * mob effect kept topped up.
 *
 * <p>Cheap when idle: if no loaded gene declares any ability
 * ({@link SpecAbilities#anyLoaded()} - the default, since no gene file ships)
 * the handler returns immediately.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources. The
 * {@code walk_on_water} implementation in particular is an approximation -
 * surface buoyancy plus "don't sink", not a solid collision plane - and its
 * feel is a guess. {@code attribute} and {@code mob_effect} abilities are parsed
 * and carried but <b>not executed yet</b> (logged once); see
 * {@code wiki/horse-traits.html}.
 *
 * <p>Yields ({@code minecraft:bucket} on a mare, ...) are handled on the
 * interaction event, not here - {@link GeneYieldHandler}.
 */
@EventBusSubscriber
public final class GeneAbilityHandler {

    private GeneAbilityHandler() {}

    /** Per-horse cache of the resolved ability list, keyed by the genetic code it was built from. */
    private static final Map<UUID, Snapshot> CACHE = new ConcurrentHashMap<>();

    /** Effect types that are defined but not translated yet - warned about once each. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private record Snapshot(String code, List<SpecAbilities.Active> abilities) {}

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        Level level = horse.level();
        if (level.isClientSide() || !horse.isAlive()) {
            return;
        }
        if (!SpecAbilities.anyLoaded()) {
            return;
        }
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return; // record not assigned yet - onHorseJoin runs first
        }
        List<SpecAbilities.Active> abilities = resolve(horse, record);
        if (abilities.isEmpty()) {
            return;
        }

        boolean moving = horse.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;

        for (SpecAbilities.Active active : abilities) {
            GeneAbility ability = active.ability();
            if (!conditionHolds(ability.when(), horse, record)) {
                continue;
            }
            switch (ability) {
                case GeneAbility.Traversal t -> applyTraversal(t.flag(), horse);
                case GeneAbility.Emitter e -> maybeEmit(e, horse, (ServerLevel) level, moving);
                case GeneAbility.AttributeMod ignored -> warnUntranslated("attribute", active.geneKey());
                case GeneAbility.SelfEffect ignored -> warnUntranslated("mob_effect", active.geneKey());
                case GeneAbility.Yield ignored -> { /* handled on interaction */ }
            }
        }
    }

    // ------------------------------------------------------------------
    // Traversal flags
    // ------------------------------------------------------------------

    private static void applyTraversal(String flag, Horse horse) {
        switch (flag) {
            case "walk_on_water" -> {
                if (horse.isInWater()) {
                    Vec3 dm = horse.getDeltaMovement();
                    double y = Math.max(dm.y, 0.0);
                    if (horse.isUnderWater()) {
                        y += 0.10; // rise back to the surface
                    }
                    horse.setDeltaMovement(dm.x, Math.min(y, 0.12), dm.z);
                    horse.resetFallDistance();
                    horse.setOnGround(true);
                }
            }
            case "fire_immune" -> horse.clearFire();
            case "fall_immune" -> horse.resetFallDistance();
            case "underwater_breathing" -> horse.setAirSupply(horse.getMaxAirSupply());
            case "walk_on_lava", "water_averse" -> {
                // walk_on_lava needs the same buoyancy trick against lava and a
                // fire-damage cancel; water_averse is an AI-goal weight. Both are
                // defined in the format but not wired here yet.
                warnUntranslated("traversal:" + flag, "(gene)");
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Emitters
    // ------------------------------------------------------------------

    private static void maybeEmit(GeneAbility.Emitter e, Horse horse, ServerLevel level, boolean moving) {
        switch (e.trigger()) {
            case GeneAbility.Trigger.OnMove ignored -> {
                if (!moving || !horse.onGround()) {
                    return;
                }
            }
            case GeneAbility.Trigger.Interval interval -> {
                if (horse.tickCount % interval.ticks() != 0) {
                    return;
                }
            }
            case GeneAbility.Trigger.Continuous ignored -> { }
            case GeneAbility.Trigger.OnInteract ignored -> {
                return; // an emitter never fires on interact
            }
        }
        if (level.getRandom().nextFloat() > e.chance()) {
            return;
        }
        if (!"particle".equals(e.kind())) {
            return; // 'light' is defined but needs a dynamic light source; not wired
        }

        double x = horse.getX();
        double z = horse.getZ();
        double y = switch (e.anchor()) {
            case "body" -> horse.getY() + horse.getBbHeight() * 0.5;
            case "head", "eyes" -> horse.getEyeY();
            default -> horse.getY() + 0.05; // feet
        };
        double spread = switch (e.shape()) {
            case "burst", "ring" -> 0.45;
            case "point" -> 0.06;
            default -> 0.18; // trail
        };
        int count = "burst".equals(e.shape()) ? 6 : 1;

        level.sendParticles(particleFor(e.particle(), e.color()),
                x, y, z, count, spread, spread * 0.5, spread, 0.0);
    }

    private static ParticleOptions particleFor(String id, int color) {
        return switch (id) {
            case "minecraft:splash" -> ParticleTypes.SPLASH;
            case "minecraft:bubble" -> ParticleTypes.BUBBLE;
            case "minecraft:bubble_pop" -> ParticleTypes.BUBBLE_POP;
            case "minecraft:falling_water" -> ParticleTypes.FALLING_WATER;
            case "minecraft:happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
            case "minecraft:end_rod" -> ParticleTypes.END_ROD;
            case "minecraft:soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "minecraft:electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            // "minecraft:dust" and anything else: a coloured dust, which is the
            // one particle that takes the emitter's colour.
            default -> new DustParticleOptions(color, 1.0F);
        };
    }

    // ------------------------------------------------------------------
    // Conditions
    // ------------------------------------------------------------------

    /** Shared with {@link GeneYieldHandler}. Boolean for now (see the architecture's scalar model). */
    static boolean conditionHolds(GeneAbility.Condition c, Horse horse, HorseRecord record) {
        return switch (c) {
            case GeneAbility.Condition.Always ignored -> true;
            case GeneAbility.Condition.Not n -> !conditionHolds(n.term(), horse, record);
            case GeneAbility.Condition.All all -> all.terms().stream().allMatch(t -> conditionHolds(t, horse, record));
            case GeneAbility.Condition.Any any -> any.terms().stream().anyMatch(t -> conditionHolds(t, horse, record));
            case GeneAbility.Condition.Flag f -> flagHolds(f.name(), horse, record) ^ f.negate();
        };
    }

    private static boolean flagHolds(String name, Horse horse, HorseRecord record) {
        Level level = horse.level();
        return switch (name) {
            case "sex_female" -> record.sex() == Sex.FEMALE;
            case "sex_male" -> record.sex() == Sex.MALE;
            case "tamed" -> horse.isTamed();
            case "untamed" -> !horse.isTamed();
            case "adult" -> !horse.isBaby();
            case "baby" -> horse.isBaby();
            case "has_rider" -> horse.isVehicle();
            case "in_water" -> horse.isInWater();
            case "submerged" -> horse.isUnderWater();
            case "on_ground" -> horse.onGround();
            case "on_fire" -> horse.isOnFire();
            case "day" -> level.isBrightOutside();
            case "night" -> !level.isBrightOutside();
            case "raining" -> level.isRaining();
            case "thundering" -> level.isThundering();
            case "sky_visible" -> level.canSeeSkyFromBelowWater(horse.blockPosition());
            default -> false;
        };
    }

    // ------------------------------------------------------------------

    private static List<SpecAbilities.Active> resolve(Horse horse, HorseRecord record) {
        String code = record.geneticCode();
        Snapshot snap = CACHE.get(horse.getUUID());
        if (snap != null && snap.code().equals(code)) {
            return snap.abilities();
        }
        List<SpecAbilities.Active> list;
        try {
            list = SpecAbilities.activeFor(Genotype.parse(code));
        } catch (RuntimeException e) {
            list = List.of();
        }
        if (CACHE.size() > 4096) {
            CACHE.clear(); // dev-mod housekeeping; the list rebuilds on the next tick
        }
        CACHE.put(horse.getUUID(), new Snapshot(code, list));
        return list;
    }

    private static void warnUntranslated(String type, String geneKey) {
        if (WARNED.add(type)) {
            HorseGenetics.LOGGER.info("[genes] effect '{}' (from {}) is defined but not translated to game "
                    + "behaviour yet - see wiki/horse-traits.html", type, geneKey);
        }
    }
}
