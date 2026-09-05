package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The <b>translator</b> for a data-driven gene's {@code effects} block - the
 * NeoForge half of {@link GeneAbility}. Every tick it reads the abilities a
 * horse's genotype expresses ({@link HorseAbilities#activeFor}) and turns each
 * one into game state: a traversal flag held up, a particle emitter fired, a
 * mob effect kept topped up.
 *
 * <p>Both kinds of gene reach it. A data-driven gene's {@code effects} block
 * and a built-in gene's
 * {@link com.example.horsegenetics.common.genetics.AbilityContribution} produce
 * the same records, so there is one switch here and not two. Since the magical
 * utility genes were built in, {@link HorseAbilities#anyLoaded()} is always true
 * and the idle short-circuit below no longer fires - the per-horse ability list
 * is cached by genetic code, which is where the cost actually was.
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources. The
 * {@code walk_on_water} implementation in particular is an approximation -
 * surface buoyancy plus "don't sink", not a solid collision plane - and its
 * feel is a guess. {@code mob_effect} is executed - the effect id is resolved
 * against the registry and kept topped up on the {@code self} / {@code rider}
 * target while its {@code when} holds. {@code attribute} is the one verb still
 * parsed but <b>not executed yet</b> (logged once); see
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

    /**
     * Where each glowing horse's {@code minecraft:light} block currently sits, so
     * it can be moved when the horse walks and cleared when the horse leaves.
     * A server restart can orphan an entry (same caveat as the portal plots);
     * {@link #clearLight} tolerates the block already being gone.
     */
    private static final Map<UUID, BlockPos> GLOW_LIGHT = new ConcurrentHashMap<>();

    /**
     * Keyed on <b>both</b> code strings. An ability's magnitude can live on the
     * allele copy (see {@code EpigeneticAbilityContribution}), so two horses
     * with identical alleles are not interchangeable here - caching on the
     * genotype alone would hand one horse another one's colours.
     */
    private record Snapshot(String code, String epigenome, List<HorseAbilities.Active> abilities) {}

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        Level level = horse.level();
        if (level.isClientSide() || !horse.isAlive()) {
            return;
        }
        if (!HorseAbilities.anyLoaded()) {
            return;
        }
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return; // record not assigned yet - onHorseJoin runs first
        }
        List<HorseAbilities.Active> abilities = resolve(horse, record);
        if (abilities.isEmpty()) {
            return;
        }

        boolean moving = horse.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;

        for (HorseAbilities.Active active : abilities) {
            GeneAbility ability = active.ability();
            if (!conditionHolds(ability.when(), horse, record)) {
                continue;
            }
            switch (ability) {
                case GeneAbility.Traversal t -> applyTraversal(t.flag(), horse);
                case GeneAbility.Emitter e -> maybeEmit(e, horse, (ServerLevel) level, moving);
                case GeneAbility.AttributeMod ignored -> warnUntranslated("attribute", active.geneKey());
                case GeneAbility.SelfEffect se -> applyMobEffect(se, horse, active.geneKey());
                case GeneAbility.Yield ignored -> { /* handled on interaction */ }
                case GeneAbility.Glow ignored -> { /* reconciled once, after the loop */ }
                case GeneAbility.Healing h -> heal(h, horse, (ServerLevel) level);
                case GeneAbility.Spread sp -> spread(sp, horse, (ServerLevel) level);
            }
        }

        reconcileGlow(horse, (ServerLevel) level, record, abilities);
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

        double spread = switch (e.shape()) {
            case "burst", "ring" -> 0.45;
            case "point" -> 0.06;
            default -> 0.18; // trail
        };
        ParticleOptions particle = particleFor(e, horse);
        int count = "burst".equals(e.shape()) ? Math.max(6, e.count()) : e.count();

        // A multi-point site is re-picked per particle rather than per firing, so
        // one beat off "hooves" really does come off different hooves.
        for (int i = 0; i < count; i++) {
            Vec3 at = anchorPoint(e.anchor(), horse, level);
            level.sendParticles(particle, at.x, at.y, at.z, 1, spread, spread * 0.5, spread, 0.0);
        }
    }

    /**
     * Where on the horse a firing comes from.
     *
     * <p>The first four anchors are single points, and were all the verb had
     * while its only users were a trail at the feet and an aura round the body.
     * The five <b>body sites</b> below exist for the particle locus, where which
     * part of the horse a trail comes off is a heritable, epigenetic fact - so
     * they have to be real places on the animal rather than one spot with a wide
     * spread. A site covering several points picks one of them per particle.
     *
     * <p>Everything is derived from the live bounding box and yaw rather than
     * from {@code HorseSkinGeometry}: this is a world position, not a texel, and
     * the box has already had the horse's scale attribute applied to it - so a
     * magically enormous horse trails from its own hooves and not from where an
     * ordinary horse's would be.
     */
    private static Vec3 anchorPoint(String anchor, Horse horse, ServerLevel level) {
        double yaw = Math.toRadians(horse.getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = fz;
        double rz = -fx;

        double half = horse.getBbWidth() * 0.5;
        double reach = horse.getBbWidth() * 0.9; // nose-to-centre, roughly
        double foot = horse.getY() + 0.05;
        double back = horse.getY() + horse.getBbHeight() * 0.78;

        return switch (anchor) {
            case "head" -> new Vec3(horse.getX() + fx * reach, horse.getEyeY(), horse.getZ() + fz * reach);
            case "eyes" -> new Vec3(horse.getX(), horse.getEyeY(), horse.getZ());
            case "body" -> new Vec3(horse.getX(), horse.getY() + horse.getBbHeight() * 0.5, horse.getZ());
            case "tail" -> new Vec3(horse.getX() - fx * reach, back, horse.getZ() - fz * reach);
            case "spine" -> {
                double t = level.getRandom().nextDouble() * 2.0 - 1.0; // withers to croup
                yield new Vec3(horse.getX() + fx * reach * t, back, horse.getZ() + fz * reach * t);
            }
            case "hooves", "front_hooves", "back_hooves" -> {
                boolean front = "front_hooves".equals(anchor)
                        || ("hooves".equals(anchor) && level.getRandom().nextBoolean());
                double along = (front ? 1 : -1) * reach * 0.65;
                double across = (level.getRandom().nextBoolean() ? 1 : -1) * half * 0.6;
                yield new Vec3(horse.getX() + fx * along + rx * across, foot,
                        horse.getZ() + fz * along + rz * across);
            }
            default -> new Vec3(horse.getX(), foot, horse.getZ()); // feet
        };
    }

    /**
     * A particle id to a real {@link ParticleOptions}.
     *
     * <p>Most particles are a {@code SimpleParticleType} and ignore everything
     * the emitter carries. The handful that are not take their extra values from
     * the emitter's {@code color} / {@code color2} / {@code data}, which is what
     * those three fields are for: the gene draws all three unconditionally and
     * this switch decides which of them the particle actually wanted.
     *
     * <p><b>The colour-carrying options want ARGB and read the alpha</b>
     * ({@code ColorParticleOption.getAlpha}), so a bare {@code 0xRRGGBB} would be
     * fully transparent and show nothing. {@code DustParticleOptions} is the
     * exception - it takes the plain RGB, which is why the two are written
     * differently here rather than by oversight.
     *
     * <p>An unrecognised id falls back to a coloured dust rather than throwing: a
     * gene file naming a particle this build has never heard of should look odd,
     * not crash a tick.
     */
    private static ParticleOptions particleFor(GeneAbility.Emitter e, Horse horse) {
        int rgb = e.color();
        int argb = 0xFF000000 | rgb;
        double data = e.data();
        return switch (e.particle()) {
            // --- carry a colour ---
            case "minecraft:dust" -> new DustParticleOptions(rgb, 1.0F);
            case "minecraft:dust_color_transition" ->
                    new DustColorTransitionOptions(rgb, e.color2(), 1.0F);
            case "minecraft:effect" -> SpellParticleOption.create(ParticleTypes.EFFECT, argb, 1.0F);
            case "minecraft:instant_effect" ->
                    SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, argb, 1.0F);
            case "minecraft:entity_effect" ->
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);
            case "minecraft:tinted_leaves" ->
                    ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, argb);

            // --- carry the spare number ---
            case "minecraft:shriek" -> new ShriekParticleOption((int) (data * 60));
            case "minecraft:sculk_charge" -> new SculkChargeParticleOptions((float) (data * Math.PI * 2));
            case "minecraft:vibration" -> new VibrationParticleOption(
                    new BlockPositionSource(horse.blockPosition()), 10 + (int) (data * 20));

            // --- carry a block ---
            case "minecraft:block" -> new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.SCULK.defaultBlockState());

            // Everything else is a SimpleParticleType, which carries no data and
            // *is* its own ParticleOptions - so it is looked up in the registry
            // rather than written out. A table of forty case labels would have
            // been forty chances to mistype a field name that does not match its
            // own id (TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS is registered as
            // "trial_spawner_detection_ominous"), and it would go stale the next
            // time the game adds a particle.
            default -> simpleParticle(e.particle(), rgb);
        };
    }

    /**
     * A registered id with no parameters, or a coloured dust if the id is
     * unknown to this build or needs data this method cannot supply. Falling
     * back rather than throwing is deliberate: a gene file naming a particle
     * that does not exist here should look wrong, not kill the tick.
     */
    private static ParticleOptions simpleParticle(String id, int rgb) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(id));
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        warnUntranslated("particle:" + id, "(gene)");
        return new DustParticleOptions(rgb, 1.0F);
    }


    // ------------------------------------------------------------------
    // Glow - a light source that follows the horse
    // ------------------------------------------------------------------

    /**
     * Keep at most one {@code minecraft:light} block trailing a glowing horse.
     * The block is moved only when the horse changes block position and is
     * removed when no {@code glow} ability is currently active. Emissive
     * {@code parts} are a client-render concern and are ignored here.
     */
    private static void reconcileGlow(Horse horse, ServerLevel level, HorseRecord record,
                                      List<HorseAbilities.Active> abilities) {
        int want = 0;
        boolean anyGlow = false;
        for (HorseAbilities.Active active : abilities) {
            if (active.ability() instanceof GeneAbility.Glow glow) {
                anyGlow = true;
                if (glow.light() > 0 && conditionHolds(glow.when(), horse, record)) {
                    want = Math.max(want, glow.light());
                }
            }
        }
        if (!anyGlow) {
            return;
        }

        UUID id = horse.getUUID();
        BlockPos current = GLOW_LIGHT.get(id);

        // Don't litter the read-only gallery dimension with light blocks.
        if (want <= 0 || level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            if (current != null) {
                clearLight(level, current);
                GLOW_LIGHT.remove(id);
            }
            return;
        }

        BlockPos target = BlockPos.containing(horse.getX(), horse.getY() + horse.getBbHeight() * 0.6, horse.getZ());
        if (target.equals(current)) {
            return; // already lit where the horse is
        }
        if (current != null) {
            clearLight(level, current);
        }
        if (level.getBlockState(target).isAir()) {
            level.setBlock(target, Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, want)
                    .setValue(LightBlock.WATERLOGGED, Boolean.FALSE), 2 | 16);
            GLOW_LIGHT.put(id, target);
        } else {
            GLOW_LIGHT.remove(id); // blocked this tick; try again when the horse moves
        }
    }

    private static void clearLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            level.removeBlock(pos, false);
        }
    }

    /** Drop a glowing horse's light block when it dies, unloads or changes dimension. */
    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        BlockPos pos = GLOW_LIGHT.remove(horse.getUUID());
        if (pos != null && !event.getLevel().isClientSide()) {
            clearLight(event.getLevel(), pos);
        }
    }

    // ------------------------------------------------------------------
    // Healing aura
    // ------------------------------------------------------------------

    /**
     * Mend everything the aura catches, on its own beat. Beats are counted off
     * the horse's own {@code tickCount} rather than the world's, so a stable
     * full of healers does not pulse in lockstep - which matters less for the
     * look than for not doing all of the work on one tick.
     */
    private static void heal(GeneAbility.Healing h, Horse horse, ServerLevel level) {
        int interval = Math.max(1, h.intervalTicks());
        if (horse.tickCount % interval != 0) {
            return;
        }
        AABB box = horse.getBoundingBox().inflate(h.radius());
        float amount = (float) h.amount();
        double reachSqr = h.radius() * h.radius();
        int healed = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (healed >= h.maxTargets()) {
                break; // every radius effect is capped - see wiki/gene-effects.html
            }
            if (!matchesHealTarget(h.target(), target, horse)) {
                continue;
            }
            // The scan box is a box and the aura is a sphere, so check properly.
            if (target.distanceToSqr(horse) > reachSqr || target.getHealth() >= target.getMaxHealth()) {
                continue;
            }
            target.heal(amount);
            healed++;
            level.sendParticles(ParticleTypes.HEART, target.getX(),
                    target.getY() + target.getBbHeight() * 0.8, target.getZ(), 1, 0.25, 0.25, 0.25, 0.0);
        }
    }

    private static boolean matchesHealTarget(String target, LivingEntity candidate, Horse horse) {
        return switch (target) {
            case "players" -> candidate instanceof Player;
            case "rider" -> candidate == horse.getControllingPassenger();
            case "self" -> candidate == horse;
            case "animals" -> candidate instanceof Animal;
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // Ground cover spreading
    // ------------------------------------------------------------------

    /**
     * Convert <b>at most one</b> eligible block near the horse per beat. That
     * cap is the design, not a performance dodge: this is a horse changing the
     * ground it walks over, and a player who leaves one standing in a field
     * should come back to a patch rather than to a new biome.
     *
     * <p>One random candidate is picked rather than the nearest eligible block,
     * which also makes the edge of a spread ragged instead of a perfect
     * expanding disc.
     *
     * <p>Skipped in the read-only gallery dimension, for the same reason the
     * glow light block is: nothing there should be able to rewrite the floor.
     */
    private static void spread(GeneAbility.Spread s, Horse horse, ServerLevel level) {
        int interval = Math.max(1, s.intervalTicks());
        if (horse.tickCount % interval != 0) {
            return;
        }
        if (level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            return;
        }
        if (level.getRandom().nextDouble() > s.chance()) {
            return;
        }

        int r = (int) Math.ceil(s.radius());
        BlockPos target = horse.blockPosition().offset(
                level.getRandom().nextInt(2 * r + 1) - r,
                level.getRandom().nextInt(3) - 1,
                level.getRandom().nextInt(2 * r + 1) - r);
        if (!level.isLoaded(target)) {
            return;
        }
        BlockState converted = convert(s.cover(), level.getBlockState(target));
        if (converted == null) {
            return;
        }
        level.setBlockAndUpdate(target, converted);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX() + 0.5, target.getY() + 1.05, target.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.0);
    }

    /**
     * What one cover does to one block, or {@code null} for "leave it alone".
     * The lists are deliberately narrow, and every entry is ground a player
     * would expect to green over - never a block anyone built with. This is the
     * judgement the {@code spread} verb keeps on the game side: the gene names a
     * cover, and knowing that mycelium eats podzol but not deepslate needs the
     * block registry.
     */
    private static BlockState convert(String cover, BlockState state) {
        return switch (cover) {
            case "mycelium" -> {
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                        || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)) {
                    yield Blocks.MYCELIUM.defaultBlockState();
                }
                yield null;
            }
            case "moss" -> {
                if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                        || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                        || state.is(Blocks.GRASS_BLOCK)) {
                    yield Blocks.MOSS_BLOCK.defaultBlockState();
                }
                yield null;
            }
            case "grass" -> {
                if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
                    yield Blocks.GRASS_BLOCK.defaultBlockState();
                }
                yield null;
            }
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Mob effects - the "aura on self / rider" pattern
    // ------------------------------------------------------------------

    private static void applyMobEffect(GeneAbility.SelfEffect e, Horse horse, String geneKey) {
        int refresh = Math.max(1, e.refreshTicks());
        if (horse.tickCount % refresh != 0) {
            return; // only re-apply on the refresh beat
        }
        LivingEntity target = "rider".equals(e.target()) ? horse.getControllingPassenger() : horse;
        if (target == null) {
            return; // "rider" with nobody aboard
        }
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(e.effect())).orElse(null);
        if (effect == null) {
            warnUntranslated("mob_effect:" + e.effect(), geneKey);
            return;
        }
        // Duration outlives one refresh beat so a skipped tick can't flicker it;
        // ambient + hidden particles + no icon so it reads as an innate trait,
        // not a potion. When 'when' goes false the re-apply stops and the effect
        // fades within refresh+20 ticks.
        target.addEffect(new MobEffectInstance(effect, refresh + 20, e.amplifier(), true, false, false), null);
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

    private static List<HorseAbilities.Active> resolve(Horse horse, HorseRecord record) {
        String code = record.geneticCode();
        String epigenomeCode = record.epigenomeCode();
        Snapshot snap = CACHE.get(horse.getUUID());
        if (snap != null && snap.code().equals(code) && snap.epigenome().equals(epigenomeCode)) {
            return snap.abilities();
        }
        List<HorseAbilities.Active> list;
        try {
            list = HorseAbilities.activeFor(Genotype.parse(code), Epigenome.parse(epigenomeCode));
        } catch (RuntimeException e) {
            list = List.of();
        }
        if (CACHE.size() > 4096) {
            CACHE.clear(); // dev-mod housekeeping; the list rebuilds on the next tick
        }
        CACHE.put(horse.getUUID(), new Snapshot(code, epigenomeCode, list));
        return list;
    }

    private static void warnUntranslated(String type, String geneKey) {
        if (WARNED.add(type)) {
            HorseGenetics.LOGGER.info("[genes] effect '{}' (from {}) is defined but not translated to game "
                    + "behaviour yet - see wiki/horse-traits.html", type, geneKey);
        }
    }
}
