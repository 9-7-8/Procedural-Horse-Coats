package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
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
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
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
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
                case GeneAbility.Traversal t -> {
                    applyTraversal(t.flag(), horse);
                    applyRiderTraversal(t, horse);
                }
                case GeneAbility.Emitter e -> maybeEmit(e, horse, (ServerLevel) level, moving);
                case GeneAbility.AttributeMod am -> applyAttribute(am, horse, active.geneKey());
                case GeneAbility.Breath b -> breathe(b, horse);
                case GeneAbility.MobAura ma -> mobAura(ma, horse, (ServerLevel) level);
                case GeneAbility.Combat c -> setAttackDamage(c, horse);
                case GeneAbility.YieldCharges ignored -> { /* read by GeneYieldHandler, on interaction */ }
                case GeneAbility.OnDeath ignored -> { /* read by GeneDeathHandler, when it dies */ }
                case GeneAbility.NightTemper ignored -> { /* read by NightBehaviourHandler, after dark */ }
                case GeneAbility.NightWatch ignored -> { /* read by NightBehaviourHandler, after dark */ }
                case GeneAbility.ItemDrop ignored -> { /* read by GeneDeathHandler, when it dies */ }
                case GeneAbility.SelfEffect se -> applyMobEffect(se, horse, active.geneKey());
                case GeneAbility.Yield ignored -> { /* handled on interaction */ }
                case GeneAbility.Glow ignored -> { /* reconciled once, after the loop */ }
                case GeneAbility.Healing h -> heal(h, horse, (ServerLevel) level);
                case GeneAbility.Spread sp -> spread(sp, horse, (ServerLevel) level);
                case GeneAbility.Sound so -> maybeSound(so, horse, (ServerLevel) level, moving, active.geneKey());
                case GeneAbility.Produce pr -> maybeProduce(pr, horse, (ServerLevel) level, active.geneKey());
                case GeneAbility.Bond bo -> maybeBond(bo, horse, active.geneKey());
                case GeneAbility.Temper te -> {
                    if (te.trigger() instanceof GeneAbility.Trigger.OnOwnerHurt) {
                        // Not a tick effect - it fires from GeneReactionHandler.
                        // The tick's job is to keep that handler's owner index
                        // warm so it can early-out on every other player's hits.
                        GeneReactionHandler.noteGuardian(horse);
                    } else {
                        temper(te, horse, (ServerLevel) level);
                    }
                }
                case GeneAbility.Summon su -> maybeSummon(su, horse, (ServerLevel) level, active.geneKey());
                case GeneAbility.Ward w -> GeneWardHandler.note(horse, w);
                case GeneAbility.Teleport ignored -> { /* read by GeneReactionHandler, when something hits it */ }
            }
        }

        reconcileGlow(horse, (ServerLevel) level, record, abilities);
        clearAttributes(horse, abilities, record);
    }

    // ------------------------------------------------------------------
    // Attribute modifiers
    // ------------------------------------------------------------------

    /**
     * The attribute names a gene may move, and the vanilla attribute each one
     * is. A <b>closed table</b>, so a gene naming something this build has never
     * heard of gets one log line rather than a silent no-op.
     *
     * <p>{@code swim_speed} is not among them and never was, however much the
     * name suggests itself: vanilla has no such attribute. What it has is
     * {@code water_movement_efficiency} - the share of its land speed a mob
     * keeps in water, which is Depth Strider's attribute and is what "how fast
     * does this horse swim" actually means here.
     */
    private static final Map<String, Holder<Attribute>> ATTRIBUTES = Map.ofEntries(
            Map.entry("movement_speed", Attributes.MOVEMENT_SPEED),
            Map.entry("jump_strength", Attributes.JUMP_STRENGTH),
            Map.entry("max_health", Attributes.MAX_HEALTH),
            Map.entry("armor", Attributes.ARMOR),
            Map.entry("armor_toughness", Attributes.ARMOR_TOUGHNESS),
            Map.entry("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
            Map.entry("step_height", Attributes.STEP_HEIGHT),
            Map.entry("safe_fall_distance", Attributes.SAFE_FALL_DISTANCE),
            Map.entry("scale", Attributes.SCALE),
            Map.entry("water_movement_efficiency", Attributes.WATER_MOVEMENT_EFFICIENCY),
            Map.entry("movement_efficiency", Attributes.MOVEMENT_EFFICIENCY),
            Map.entry("oxygen_bonus", Attributes.OXYGEN_BONUS),
            Map.entry("gravity", Attributes.GRAVITY));

    /**
     * {@code scale} is the one entry in that table a world may switch off. An
     * effect that moves it resizes the model and the hitbox exactly as the size
     * loci do, so it answers to the same {@code body.size} setting - see
     * {@link ServerConfig#bodySizeActive()}. Both {@link #applyAttribute} and
     * {@link #clearAttributes} ask, so a world that flips the setting off has
     * any standing scale modifier taken back off on the next reconcile rather
     * than left frozen on the horse.
     */
    private static boolean attributeAllowed(String attribute) {
        return !"scale".equals(attribute) || ServerConfig.bodySizeActive();
    }

    /**
     * Hold up one attribute modifier while its {@code when} is true.
     *
     * <p><b>Not verified in-game.</b> Written against 26.1.2 sources.
     *
     * <p>The modifier is <b>transient</b> and its id is derived from the gene
     * and the attribute, so re-applying it every tick is idempotent: a transient
     * modifier whose id is already present is replaced rather than stacked,
     * which is exactly what a per-tick reconciliation wants. The removal half is
     * the interesting one - when {@code when} goes false the ability is skipped
     * before it ever reaches here, so nothing would take the modifier off again.
     * {@link #clearAttributes} therefore runs unconditionally afterwards.
     */
    private static void applyAttribute(GeneAbility.AttributeMod mod, Horse horse, String geneKey) {
        Holder<Attribute> attribute = ATTRIBUTES.get(mod.attribute());
        if (attribute == null) {
            warnUntranslated("attribute:" + mod.attribute(), geneKey);
            return;
        }
        if (!attributeAllowed(mod.attribute())) {
            return; // body.size is off; clearAttributes takes off any modifier already up
        }
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance == null) {
            return; // the horse simply has no such attribute; not an error
        }
        AttributeModifier.Operation op = switch (mod.op()) {
            case "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        instance.addOrUpdateTransientModifier(
                new AttributeModifier(attributeModifierId(geneKey, mod.attribute()), mod.amount(), op));
    }

    /** A stable, gene-scoped id, so two genes may move one attribute without fighting. */
    private static Identifier attributeModifierId(String geneKey, String attribute) {
        return Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID,
                "gene/" + geneKey.replace('.', '_') + "/" + attribute);
    }

    /**
     * Take off every gene modifier the horse is <b>not</b> currently asking for.
     *
     * <p>A transient modifier survives until something removes it, and the apply
     * path above only runs while the ability's condition holds - so without this
     * a modifier gated on {@code in_water} would stay on the horse for ever the
     * moment it stepped out of the river.
     */
    private static void clearAttributes(Horse horse, List<HorseAbilities.Active> wanted,
                                        HorseRecord record) {
        Set<Identifier> keep = new HashSet<>();
        for (HorseAbilities.Active active : wanted) {
            if (active.ability() instanceof GeneAbility.AttributeMod mod
                    && attributeAllowed(mod.attribute())
                    && conditionHolds(mod.when(), horse, record)) {
                keep.add(attributeModifierId(active.geneKey(), mod.attribute()));
            }
        }
        for (Holder<Attribute> attribute : ATTRIBUTES.values()) {
            AttributeInstance instance = horse.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (AttributeModifier existing : List.copyOf(instance.getModifiers())) {
                if (existing.id().getNamespace().equals(HorseGenetics.MOD_ID)
                        && !keep.contains(existing.id())) {
                    instance.removeModifier(existing.id());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Breath
    // ------------------------------------------------------------------

    /**
     * How much air each horse is owed but has not been handed yet - because air
     * is an integer and a breath factor is not.
     */
    private static final Map<UUID, Double> BREATH_DEBT = new ConcurrentHashMap<>();

    /**
     * Multiply how long the horse lasts under water.
     *
     * <p>Vanilla takes one point of air per submerged tick. For the horse to
     * last {@code factor} times as long the drain has to be {@code 1 / factor},
     * so every submerged tick this hands back {@code 1 - 1 / factor} - positive
     * for a long-breathed horse, negative for a short-breathed one. One
     * expression covers both directions, which is the reason to write it this
     * way rather than as two cases.
     *
     * <p>Air is an {@code int} and that quantity is not, so the fraction
     * accumulates per horse until it is worth a whole point. The obvious
     * alternative - vanilla's {@code oxygen_bonus} attribute - reaches only one
     * direction, and does it by rolling a die every tick, so two horses with
     * identical genotypes would drown at different times. This locus is meant to
     * be bred for, and a bred number that does not hold still is not one.
     *
     * <p><b>Not verified in-game.</b>
     */
    private static void breathe(GeneAbility.Breath b, Horse horse) {
        UUID id = horse.getUUID();
        if (!horse.isUnderWater() || b.factor() <= 0) {
            BREATH_DEBT.remove(id);
            return;
        }
        double owed = BREATH_DEBT.getOrDefault(id, 0.0) + (1.0 - 1.0 / b.factor());
        int whole = (int) owed;
        if (whole != 0) {
            // Clamped at the top to the horse's own maximum; the bottom is left
            // alone, because vanilla reads a negative air supply as "drowning"
            // and that is exactly what a short-breathed horse should do.
            horse.setAirSupply(Math.min(horse.getMaxAirSupply(), horse.getAirSupply() + whole));
            owed -= whole;
        }
        BREATH_DEBT.put(id, owed);
    }

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    /**
     * What the horse hits for. Horses already have an {@code ATTACK_DAMAGE}
     * attribute and a melee goal - {@link HorseAggroHandler} gave them both so
     * that a wild horse could kick back - so this locus has only to move the
     * number.
     *
     * <p>Written as a <b>base value</b> rather than as a modifier because the
     * gene's number is absolute: it is the damage the horse deals, not an
     * adjustment to something a reader would have to go and look up. Only
     * written when it differs, so the common case is a comparison rather than an
     * attribute write on every tick of every horse.
     */
    private static void setAttackDamage(GeneAbility.Combat c, Horse horse) {
        AttributeInstance instance = horse.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance != null && Math.abs(instance.getBaseValue() - c.damage()) > 1.0e-6) {
            instance.setBaseValue(c.damage());
        }
    }

    // ------------------------------------------------------------------
    // Mob aura
    // ------------------------------------------------------------------

    /**
     * Make mobs keep away from the horse, or fight over it.
     *
     * <p>Both modes beat on the horse's own {@code tickCount} rather than the
     * world's, so a field of them does not do all of its work on one tick - the
     * same reason the healing aura does.
     *
     * <p><b>Repel is a shove, not a wall.</b> A hostile inside the radius has
     * its target cleared if it was the horse or the horse's rider, and is pushed
     * outward. That is deliberately weaker than a real avoidance goal: a goal
     * would have to be added to and taken off every mob that ever walked past,
     * and a horse whose aura stopped expressing would leave the goal behind on
     * everything it had ever met.
     *
     * <p><b>Not verified in-game.</b>
     */
    private static void mobAura(GeneAbility.MobAura aura, Horse horse, ServerLevel level) {
        int interval = Math.max(1, aura.intervalTicks());
        if (horse.tickCount % interval != 0) {
            return;
        }
        AABB box = horse.getBoundingBox().inflate(aura.radius());
        double reachSqr = aura.radius() * aura.radius();
        boolean attract = "attract".equals(aura.mode());
        boolean follow = "follow".equals(aura.mode());
        int touched = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (touched >= aura.maxTargets()) {
                break; // every radius effect is capped - see wiki/making-a-gene.html
            }
            // The group (or the single mob id) decides who this aura is about.
            // Tags, not a hardcoded list - see MobGroups, and the five genes
            // that would otherwise each go blind to modded creatures.
            if (mob == horse || !MobGroups.matches(aura.group(), aura.mob(), mob)) {
                continue;
            }
            // The scan box is a box and the aura is a sphere, so check properly.
            if (mob.distanceToSqr(horse) > reachSqr) {
                continue;
            }
            if (attract) {
                if (mob.hasLineOfSight(horse) && mob.getTarget() != horse) {
                    mob.setTarget(horse);
                    touched++;
                }
                continue;
            }
            if (follow) {
                followHorse(mob, horse);
                touched++;
                continue;
            }
            if (mob.getTarget() == horse || mob.getTarget() == horse.getControllingPassenger()) {
                mob.setTarget(null);
            }
            Vec3 away = mob.position().subtract(horse.position());
            if (away.lengthSqr() < 1.0e-4) {
                away = new Vec3(1, 0, 0); // exactly on top of the horse; any direction will do
            }
            away = away.normalize().scale(0.35);
            mob.setDeltaMovement(mob.getDeltaMovement().add(away.x, 0.05, away.z));
            mob.hurtMarked = true; // tell the client the mob was shoved
            touched++;
        }
    }

    /** How close a follower is content to get before it stops walking. */
    private static final double FOLLOW_STOP = 3.0;

    /**
     * Nudge one mob toward the horse.
     *
     * <p><b>A navigation nudge, not an injected goal.</b> A goal added to an
     * arbitrary mob has to be taken off again, and a horse that dies, despawns
     * or stops expressing would leave it behind on every creature it had ever
     * met - the same lifecycle failure the repel branch already avoids by
     * shoving rather than adding an avoidance goal. Re-issuing a path each beat
     * is stateless and cannot leak.
     *
     * <p>This is also the expensive half of the follow mode: pathfinding is the
     * most costly thing a mob does, so the caller's interval and target cap are
     * doing real work rather than being tidy.
     */
    private static void followHorse(Mob mob, Horse horse) {
        if (mob.distanceToSqr(horse) <= FOLLOW_STOP * FOLLOW_STOP) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(horse, 10.0F, (float) mob.getMaxHeadXRot());
            return;
        }
        mob.getNavigation().moveTo(horse, 1.0);
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
            case "walk_on_lava" -> {
                if (horse.isInLava()) {
                    Vec3 dm = horse.getDeltaMovement();
                    horse.setDeltaMovement(dm.x, Math.min(Math.max(dm.y, 0.0), 0.12), dm.z);
                    horse.resetFallDistance();
                    horse.setOnGround(true);
                    horse.clearFire();
                }
            }
            case "lava_swim" -> {
                // Swimming, not walking: it sinks in and moves through, so the
                // only thing to hold up is the fire damage and a floor under the
                // sink rate so it is a wade rather than a drop.
                if (horse.isInLava()) {
                    Vec3 dm = horse.getDeltaMovement();
                    horse.setDeltaMovement(dm.x, Math.max(dm.y, -0.06), dm.z);
                    horse.resetFallDistance();
                    horse.clearFire();
                }
            }
            case "water_averse" -> {
                // Vanilla's own behaviour, restored: throw the rider in deep
                // water and head out. Deliberately only when actually swimming,
                // so a paddle through a ford does not dismount anybody.
                if (horse.isInWater() && horse.isVehicle() && horse.getFluidHeight(FluidTags.WATER) > 0.6) {
                    horse.ejectPassengers();
                }
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
            case GeneAbility.Trigger.OnHurt ignored -> {
                return; // an emitter does not fire on damage; that is the teleport verb's trigger
            }
            case GeneAbility.Trigger.OnOwnerHurt ignored -> {
                return; // likewise - and this one is not even about this entity
            }
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
        int second = e.color2();
        if (e.cycleTicks() > 0) {
            rgb = cycleHue(horse, e.cycleTicks(), 0.0);
            second = cycleHue(horse, e.cycleTicks(), RAINBOW_LEAD);
        }
        int argb = 0xFF000000 | rgb;
        double data = e.data();
        return switch (e.particle()) {
            // --- carry a colour ---
            case "minecraft:dust" -> new DustParticleOptions(rgb, 1.0F);
            case "minecraft:dust_color_transition" ->
                    new DustColorTransitionOptions(rgb, second, 1.0F);
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
     * How far ahead of the trail's current hue its <b>second</b> colour sits, as
     * a fraction of the circle. A twelfth is one step further round than the
     * six named colours of a rainbow are apart, so a
     * {@code dust_color_transition} fading from here to there reads as "red
     * going orange" rather than as two unrelated colours in one puff.
     */
    private static final double RAINBOW_LEAD = 1.0 / 12.0;

    /**
     * The hue this horse's cycling trail is on right now, {@code offset} of a
     * turn ahead. Counted off the horse's own {@code tickCount} rather than the
     * world's, so two rainbow horses side by side are not in lockstep - the same
     * reason the healing aura beats on its own clock.
     */
    private static int cycleHue(Horse horse, int cycleTicks, double offset) {
        double turn = (double) (horse.tickCount % cycleTicks) / cycleTicks + offset;
        return HairPattern.hsvToRgb(turn - Math.floor(turn), 1.0, 1.0);
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

    /**
     * Light blocks whose glowing horse has left, to be removed on the next
     * server tick. <b>Not</b> cleared inside {@link #onEntityLeave}:
     * {@link EntityLeaveLevelEvent} fires from
     * {@code PersistentEntitySectionManager.stopTracking}, which runs <i>inside</i>
     * {@code DistanceManager.runAllUpdates} - and any {@code getBlockState} there
     * forces a chunk load that re-enters that same pass ("Entity is already
     * tracked!"). Deferring to {@link ServerTickEvent.Post} sidesteps it.
     */
    private record PendingClear(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Queue<PendingClear> PENDING_LIGHT_CLEARS = new ConcurrentLinkedQueue<>();

    /** Queue a glowing horse's light block for removal when it dies, unloads or changes dimension. */
    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        BREATH_DEBT.remove(horse.getUUID());
        BlockPos pos = GLOW_LIGHT.remove(horse.getUUID());
        if (pos != null && event.getLevel() instanceof ServerLevel level) {
            PENDING_LIGHT_CLEARS.add(new PendingClear(level.dimension(), pos.immutable()));
        }
    }

    @SubscribeEvent
    static void drainPendingLightClears(ServerTickEvent.Post event) {
        // Sounds waiting to be cut off ride the same server tick - see
        // tickPendingStops, and the "opening N seconds" rule it implements.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickPendingStops(level);
        }
        PendingClear pending;
        while ((pending = PENDING_LIGHT_CLEARS.poll()) != null) {
            ServerLevel level = event.getServer().getLevel(pending.dimension());
            if (level != null && level.hasChunkAt(pending.pos())) {
                clearLight(level, pending.pos());
            }
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
                break; // every radius effect is capped - see wiki/making-a-gene.html
            }
            if (!matchesHealTarget(h.target(), h.group(), target, horse)) {
                continue;
            }
            // The scan box is a box and the aura is a sphere, so check properly.
            if (target.distanceToSqr(horse) > reachSqr) {
                continue;
            }
            if (amount < 0) {
                // A healing aura with the sign flipped, which is how the
                // cleansing locus damages undead without a second radius verb.
                // The kill is credited to the HORSE, so no player experience and
                // no player-only drops - a settled decision, not an oversight.
                target.hurt(level.damageSources().magic(), -amount);
                healed++;
                level.sendParticles(ParticleTypes.END_ROD, target.getX(),
                        target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        3, 0.25, 0.25, 0.25, 0.0);
                continue;
            }
            if (target.getHealth() >= target.getMaxHealth()) {
                continue;
            }
            target.heal(amount);
            healed++;
            level.sendParticles(ParticleTypes.HEART, target.getX(),
                    target.getY() + target.getBbHeight() * 0.8, target.getZ(), 1, 0.25, 0.25, 0.25, 0.0);
        }
    }

    private static boolean matchesHealTarget(String target, String group, LivingEntity candidate, Horse horse) {
        if ("group".equals(target)) {
            return MobGroups.matches(group, candidate);
        }
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
        BlockState converted = convert(s.cover(), level, target);
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
    /**
     * What one vocabulary word does to one block.
     *
     * <p>The verb takes a <b>word</b> and not a block id precisely so that the
     * judgement about what may be converted stays here, with the code that knows
     * the game's blocks - which is what stops any of these eating something a
     * player placed. {@code melt} deliberately leaves packed and blue ice alone
     * for that reason: those are building materials, not weather.
     */
    private static BlockState convert(String cover, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
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
            case "melt" -> {
                // Snow goes to air; plain ice goes straight to a water SOURCE
                // rather than to air, because an air pocket in a lake floods and
                // flowing water is the expensive part of this whole gene.
                // Packed and blue ice are somebody's floor and are left alone.
                if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                        || state.is(Blocks.POWDER_SNOW)) {
                    yield Blocks.AIR.defaultBlockState();
                }
                if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                    yield Blocks.WATER.defaultBlockState();
                }
                yield null;
            }
            case "sapling" -> {
                // The only word that plants ABOVE the ground rather than
                // converting it, so it is the only one that has to look down.
                if (!state.isAir()) {
                    yield null;
                }
                BlockState below = level.getBlockState(pos.below());
                if (!below.is(Blocks.GRASS_BLOCK) && !below.is(Blocks.DIRT)
                        && !below.is(Blocks.PODZOL) && !below.is(Blocks.COARSE_DIRT)) {
                    yield null;
                }
                yield saplingFor(level, pos);
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
        if ("group".equals(e.target())) {
            applyGroupEffect(e, horse, geneKey);
            return;
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

    /**
     * The one shape of mob effect that leaves the horse and its rider:
     * everything of a group inside a radius.
     *
     * <p><b>The cap here is a packet cap, not a tick cap.</b> A mob effect syncs
     * to every client tracking the entity it lands on, so re-applying one to
     * every living thing in a wide radius on a short refresh is a network flood
     * in a busy cave - which is exactly the cost that does not show up when
     * testing with one horse in a field. As above, the duration outlives the
     * refresh, so the effect can be left to lapse rather than re-stamped on
     * every beat.
     */
    private static void applyGroupEffect(GeneAbility.SelfEffect e, Horse horse, String geneKey) {
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(e.effect())).orElse(null);
        if (effect == null) {
            warnUntranslated("mob_effect:" + e.effect(), geneKey);
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        int refresh = Math.max(1, e.refreshTicks());
        AABB box = horse.getBoundingBox().inflate(e.radius());
        int touched = 0;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (touched >= e.maxTargets()) {
                break;
            }
            if (candidate == horse || !MobGroups.matches(e.group(), candidate)) {
                continue;
            }
            candidate.addEffect(new MobEffectInstance(effect, refresh + 40, e.amplifier(),
                    true, false, false), null);
            touched++;
        }
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
            // full_health is measured against this horse's OWN max, so a
            // genetically frail mare is milkable at her own ceiling, not the
            // species max (roadmap §7 - ties milking to the healing gate).
            case "full_health" -> horse.getHealth() >= horse.getMaxHealth() - 1.0e-3F;
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
            // The three that read the WORLD - sampled on an interval and cached,
            // because a condition is evaluated once per ability per tick and
            // these are a light lookup, a block search and a biome query.
            case "dark", "near_jukebox", "snowing", "hostile_near" -> worldFlag(name, horse);
            default -> false;
        };
    }

    // ------------------------------------------------------------------

    /**
     * <b>What this horse's genes grant, for a handler that is not the tick.</b>
     *
     * <p>The ward runs on every spawn attempt in the world and the reaction
     * handler on every damage event, and neither has a {@link HorseRecord} in
     * hand. Both go through here so they share the one cache rather than
     * re-parsing a genotype per event - which for the ward would mean parsing
     * once per spawn attempt per horse.
     *
     * <p>Returns an empty list for a horse whose record has not been assigned
     * yet, which is the same answer the tick loop gives.
     */
    static List<HorseAbilities.Active> abilitiesOf(Horse horse) {
        if (!HorseAbilities.anyLoaded()) {
            return List.of();
        }
        HorseRecord record = HorseRecords.of(horse);
        return record.hasName() ? resolve(horse, record) : List.of();
    }

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

    // ------------------------------------------------------------------
    // World-reading condition flags
    // ------------------------------------------------------------------

    /** How often the world-reading flags are re-sampled, in ticks. */
    private static final int WORLD_FLAG_SAMPLE_TICKS = 20;

    /** How far a horse looks for a playing jukebox. Small on purpose - it is a block search. */
    private static final int JUKEBOX_RANGE = 8;

    /** Block light at or below this counts as dark. Vanilla's own hostile-spawn threshold. */
    private static final int DARK_LEVEL = 7;

    /** How far {@code hostile_near} looks. The alarm range, and an entity scan rather than a block one. */
    private static final double HOSTILE_NEAR_RANGE = 16.0;

    private record WorldSample(long tick, boolean dark, boolean nearJukebox, boolean snowing,
                               boolean hostileNear) {}

    private static final Map<UUID, WorldSample> WORLD_FLAGS = new ConcurrentHashMap<>();

    /**
     * A world-reading flag, answered from a per-horse sample refreshed every
     * {@link #WORLD_FLAG_SAMPLE_TICKS}.
     *
     * <p>Every other flag in {@code flagHolds} is a getter. These three are a
     * block-light read, a block search and a biome query, and a condition is
     * evaluated <i>once per ability per tick</i> - so a horse expressing three
     * conditioned effects would do three block searches a tick without this.
     * {@code AbilityType.WORLD_FLAGS} is the list of which flags need it.
     */
    private static boolean worldFlag(String name, Horse horse) {
        Level level = horse.level();
        WorldSample sample = WORLD_FLAGS.get(horse.getUUID());
        long now = level.getGameTime();
        if (sample == null || now - sample.tick() >= WORLD_FLAG_SAMPLE_TICKS) {
            // Only what was actually asked for. Computing all four together was
            // the first version and it was a real cost: a caveborn horse asks
            // for "dark" - one block-light read - and was paying for a ~2000
            // block jukebox search and a 16-block entity scan it has no use for,
            // every twenty ticks, for ever. The flags a horse never names are
            // never computed.
            sample = sampleWorld(horse, level, now, name);
            if (WORLD_FLAGS.size() > 4096) {
                WORLD_FLAGS.clear(); // dev-mod housekeeping; it re-samples next tick
            }
            WORLD_FLAGS.put(horse.getUUID(), sample);
        }
        return switch (name) {
            case "dark" -> sample.dark();
            case "near_jukebox" -> sample.nearJukebox();
            case "snowing" -> sample.snowing();
            case "hostile_near" -> sample.hostileNear();
            default -> false;
        };
    }

    /**
     * Sample the world for <b>one</b> flag.
     *
     * <p>The other three are carried forward from whatever the previous sample
     * said, which is deliberately a little stale rather than expensive: a horse
     * that names two world flags pays for both on alternate refreshes, and a
     * horse that names one never computes the other three at all. The costs
     * differ by orders of magnitude - a light read is nothing, the jukebox
     * search is a couple of thousand blocks - so treating them alike was never
     * defensible.
     */
    private static WorldSample sampleWorld(Horse horse, Level level, long now, String wanted) {
        BlockPos at = horse.blockPosition();
        WorldSample prev = WORLD_FLAGS.get(horse.getUUID());

        boolean dark = prev != null && prev.dark();
        boolean snowing = prev != null && prev.snowing();
        boolean jukebox = prev != null && prev.nearJukebox();
        boolean hostile = prev != null && prev.hostileNear();

        if ("dark".equals(wanted)) {
            dark = level.getMaxLocalRawBrightness(at) <= DARK_LEVEL;
        }
        if ("snowing".equals(wanted)) {
            // Snow is not weather in Minecraft - it is rain in a cold biome, so
            // the flag is a biome query and not a level one.
            snowing = level.isRaining()
                    && level.getBiome(at).value().coldEnoughToSnow(at, level.getSeaLevel());
        }
        if ("near_jukebox".equals(wanted)) {
            jukebox = false;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-JUKEBOX_RANGE, -3, -JUKEBOX_RANGE),
                    at.offset(JUKEBOX_RANGE, 3, JUKEBOX_RANGE))) {
                BlockState state = level.getBlockState(p);
                if (state.is(Blocks.JUKEBOX) && state.getValue(JukeboxBlock.HAS_RECORD)) {
                    jukebox = true;
                    break;
                }
            }
        }
        if ("hostile_near".equals(wanted) && level instanceof ServerLevel sl) {
            // An entity scan, which is why this is a sampled flag and not a live one.
            hostile = false;
            AABB box = horse.getBoundingBox().inflate(HOSTILE_NEAR_RANGE);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (MobGroups.isHostile(e)) {
                    hostile = true;
                    break;
                }
            }
        }
        return new WorldSample(now, dark, jukebox, snowing, hostile);
    }

    // ------------------------------------------------------------------
    // Rider-targeted traversal
    // ------------------------------------------------------------------

    /**
     * <b>Flags that reach the player.</b> The sharpest hazard in the effect
     * system, because every other traversal flag touches only the horse - which
     * is state on an entity this mod owns and can reset freely - and this one
     * writes onto something that walks away.
     *
     * <p>So the grant is deliberately <b>per-tick and non-persistent</b>: it
     * puts out a fire, resets a fall distance or tops up air <i>this tick</i>
     * and stores nothing. There is nothing to take back off on dismount because
     * nothing was ever left on, which is the only version of this that cannot
     * become an invulnerability dupe.
     */
    private static void applyRiderTraversal(GeneAbility.Traversal t, Horse horse) {
        if ("self".equals(t.target())) {
            return;
        }
        if (!(horse.getFirstPassenger() instanceof Player rider)) {
            return;
        }
        switch (t.flag()) {
            case "fire_immune", "walk_on_lava", "lava_swim" -> {
                rider.clearFire();
                rider.setRemainingFireTicks(0);
            }
            case "fall_immune" -> rider.resetFallDistance();
            case "underwater_breathing" -> rider.setAirSupply(rider.getMaxAirSupply());
            default -> { }
        }
    }

    /**
     * <b>An immunity is a damage cancel, not a per-tick reset.</b> The tick's
     * {@code clearFire} only stops the <i>burning</i>; standing in lava or a
     * fire block hurts through {@code Entity.lavaHurt} / the block's
     * {@code entityInside} directly, and vanilla's own immunity is a flag on the
     * entity <i>type</i> ({@code isInvulnerableToBase}: {@code IS_FIRE &&
     * fireImmune()}), which a gene cannot set per horse. Owner-observed
     * 2026-09-10: neither the horse nor its rider was protected.
     *
     * <p>Falling has the same shape: fall damage lands inside {@code move()},
     * before the post-tick reset runs, so one tick of terminal velocity still
     * hurt - and a <i>rider's</i> fall damage is the horse's, handed down by
     * {@code AbstractHorse.propagateFallToPassengers}, so resetting the rider's
     * own fall distance never protected them at all.
     *
     * <p>Still stores nothing: the rider case reads the vehicle <i>at the moment
     * of the hit</i>, so stepping off ends it with no state to take back.
     */
    @SubscribeEvent
    static void onTraversalDamage(LivingIncomingDamageEvent event) {
        Set<String> flags;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            flags = FIRE_FLAGS;
        } else if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            flags = FALL_FLAGS;
        } else {
            return;
        }
        LivingEntity hurt = event.getEntity();
        if (hurt.level().isClientSide()) {
            return;
        }
        if (hurt instanceof Horse horse) {
            if (grantsImmunity(horse, flags, false)) {
                event.setCanceled(true);
                if (flags == FIRE_FLAGS) {
                    horse.clearFire();
                }
            }
        } else if (hurt instanceof Player rider
                && rider.getVehicle() instanceof Horse horse
                && horse.getFirstPassenger() == rider
                && grantsImmunity(horse, flags, true)) {
            event.setCanceled(true);
            if (flags == FIRE_FLAGS) {
                rider.clearFire();
            }
        }
    }

    /** The traversal flags that carry fire immunity - the same set the rider tick puts out. */
    private static final Set<String> FIRE_FLAGS = Set.of("fire_immune", "walk_on_lava", "lava_swim");
    private static final Set<String> FALL_FLAGS = Set.of("fall_immune");

    /** Does this horse express one of {@code flags} for itself, or for its rider? */
    private static boolean grantsImmunity(Horse horse, Set<String> flags, boolean forRider) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return false;
        }
        for (HorseAbilities.Active active : resolve(horse, record)) {
            if (!(active.ability() instanceof GeneAbility.Traversal t)) {
                continue;
            }
            if (!flags.contains(t.flag())) {
                continue;
            }
            boolean reaches = forRider ? !"self".equals(t.target()) : !"rider".equals(t.target());
            if (reaches && conditionHolds(t.when(), horse, record)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Sound
    // ------------------------------------------------------------------

    /** Last firing of a per-horse, per-gene cooldown - shared by sound, produce, bond and summon. */
    private static final Map<String, Long> LAST_FIRED = new ConcurrentHashMap<>();

    private static boolean offCooldown(Horse horse, String geneKey, String what, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return true;
        }
        String id = horse.getUUID() + "/" + geneKey + "/" + what;
        long now = horse.level().getGameTime();
        Long last = LAST_FIRED.get(id);
        if (last != null && now - last < cooldownTicks) {
            return false;
        }
        if (LAST_FIRED.size() > 8192) {
            LAST_FIRED.clear();
        }
        LAST_FIRED.put(id, now);
        return true;
    }

    /**
     * A sound, on its trigger and behind its cooldown.
     *
     * <p>The cooldown is the whole safety of this verb. Tick cost is nothing;
     * <i>spam</i> is unbearable within seconds and no volume setting fixes it,
     * so a firing that is off-trigger or on cooldown is dropped before the sound
     * id is even resolved.
     */
    /** The saplings a dryad may plant, picked to suit where it is standing. */
    private static final List<BlockState> SAPLINGS = List.of(
            Blocks.OAK_SAPLING.defaultBlockState(),
            Blocks.BIRCH_SAPLING.defaultBlockState(),
            Blocks.SPRUCE_SAPLING.defaultBlockState(),
            Blocks.JUNGLE_SAPLING.defaultBlockState(),
            Blocks.ACACIA_SAPLING.defaultBlockState(),
            Blocks.DARK_OAK_SAPLING.defaultBlockState());

    /** One of the saplings, chosen from the position so a wood is not all one species. */
    private static BlockState saplingFor(ServerLevel level, BlockPos pos) {
        return SAPLINGS.get(Math.floorMod(pos.hashCode(), SAPLINGS.size()));
    }

    private static void maybeSound(GeneAbility.Sound so, Horse horse, ServerLevel level,
                                   boolean moving, String geneKey) {
        if (!triggerFiresNow(so.trigger(), horse, moving)) {
            return;
        }
        if (!offCooldown(horse, geneKey, "sound", so.cooldownTicks())) {
            return;
        }
        SoundEvent sound = soundOf(so.sound());
        if (sound == null) {
            warnUntranslated("sound:" + so.sound(), geneKey);
            return;
        }
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(), sound,
                SoundSource.NEUTRAL, (float) so.volume(), (float) so.pitch());
        if (so.durationTicks() > 0) {
            STOPPING.put(horse.getUUID() + "/" + geneKey,
                    new PendingStop(level.getGameTime() + so.durationTicks(), so.sound()));
        }
    }

    private record PendingStop(long atTick, String sound) {}

    /** Sounds waiting to be cut off - see {@link #tickPendingStops}. */
    private static final Map<String, PendingStop> STOPPING = new ConcurrentHashMap<>();

    private static SoundEvent soundOf(String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) {
            return null;
        }
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.getValue(rl);
        // A jukebox song is not a SoundEvent in the registry under its own name,
        // so a disc id is resolved through the song registry and its sound taken.
        return registered != null ? registered : null;
    }

    /**
     * Cut off any sound whose duration has run out.
     *
     * <p>A Minecraft sound plays from its beginning or not at all, so "a section
     * of a record" can only ever mean "the opening, then stopped" - and stopping
     * is a packet to everyone who can hear it rather than anything on the sound
     * itself.
     */
    private static void tickPendingStops(ServerLevel level) {
        if (STOPPING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        STOPPING.entrySet().removeIf(e -> {
            if (now < e.getValue().atTick()) {
                return false;
            }
            Identifier rl = Identifier.tryParse(e.getValue().sound());
            if (rl != null) {
                for (ServerPlayer p : level.players()) {
                    p.connection.send(new ClientboundStopSoundPacket(rl, SoundSource.NEUTRAL));
                }
            }
            return true;
        });
    }

    // ------------------------------------------------------------------
    // Produce
    // ------------------------------------------------------------------

    /** How far {@code nearby_cap} looks for what the horse has already dropped. */
    private static final double PRODUCE_CROWD_RANGE = 6.0;

    /**
     * Drop something on a clock.
     *
     * <p>{@code nearbyCap} is the accumulation guard and it is checked
     * <b>before</b> the drop rather than after: a timer that drops an item and
     * never looks is the classic way to fill a chunk with entities, and a
     * pasture of layers running overnight is the case this is written for.
     */
    private static void maybeProduce(GeneAbility.Produce pr, Horse horse, ServerLevel level,
                                      String geneKey) {
        if (!offCooldown(horse, geneKey, "produce", pr.intervalTicks())) {
            return;
        }
        Identifier rl = Identifier.tryParse(pr.item());
        Item item = rl == null ? null : BuiltInRegistries.ITEM.getValue(rl);
        if (item == null || item == Items.AIR) {
            warnUntranslated("produce:" + pr.item(), geneKey);
            return;
        }
        if (pr.nearbyCap() > 0) {
            AABB box = horse.getBoundingBox().inflate(PRODUCE_CROWD_RANGE);
            int lying = 0;
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
                if (e.getItem().is(item)) {
                    lying += e.getItem().getCount();
                    if (lying >= pr.nearbyCap()) {
                        return; // enough of it on the ground already
                    }
                }
            }
        }
        int count = pr.min() + (pr.max() > pr.min()
                ? horse.getRandom().nextInt(pr.max() - pr.min() + 1) : 0);
        horse.spawnAtLocation(level, new ItemStack(item, count));
    }

    // ------------------------------------------------------------------
    // Bond
    // ------------------------------------------------------------------

    /**
     * Bond earned by something other than the player's attention.
     *
     * <p>It goes through {@link HorseCareHandler#addBond}, which is the same path
     * every other source uses and therefore the same <b>daily cap</b>. A source
     * that wrote the attachment directly would be an AFK exploit rather than a
     * feature, and would devalue every other way of earning bond in one change.
     */
    private static void maybeBond(GeneAbility.Bond bo, Horse horse, String geneKey) {
        if (!horse.isTamed()) {
            return; // bond is a relationship; an untamed horse has none to raise
        }
        if (!offCooldown(horse, geneKey, "bond", bo.intervalTicks())) {
            return;
        }
        // awardBondFor already honours the daily cap and syncs the result - which
        // is exactly why it is the path used rather than the attachment directly.
        HorseCareHandler.awardBondFor(horse, bo.amount());
        if (horse.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART,
                    horse.getX(), horse.getY() + horse.getBbHeight(), horse.getZ(),
                    2, 0.3, 0.2, 0.3, 0.0);
        }
    }

    // ------------------------------------------------------------------
    // Temper
    // ------------------------------------------------------------------

    /**
     * Night temper with the night gate lifted out - "aggressive toward this
     * group, while this condition holds".
     *
     * <p>{@code hold} is honoured by only ever targeting what is <b>already</b>
     * inside the radius and clearing the target the moment it leaves: the horse
     * never acquires a target it would have to travel to. That is what keeps a
     * gladiator out of the ravine it would otherwise die in.
     */
    private static void temper(GeneAbility.Temper te, Horse horse, ServerLevel level) {
        if (horse.tickCount % Math.max(1, te.intervalTicks()) != 0) {
            return;
        }
        if (horse.getTarget() != null && te.hold()
                && horse.getTarget().distanceToSqr(horse) > te.radius() * te.radius()) {
            horse.setTarget(null); // it left; do not follow it
        }
        if (!"aggressive".equals(te.mood())) {
            return; // 'flee' is handled by the same goal set as night temper's
        }
        AABB box = horse.getBoundingBox().inflate(te.radius());
        int considered = 0;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (++considered > te.maxTargets()) {
                break;
            }
            if (candidate == horse || candidate == horse.getControllingPassenger()) {
                continue;
            }
            if (!MobGroups.matches(te.towards(), candidate)) {
                continue;
            }
            if (horse.getTarget() == null || !horse.getTarget().isAlive()) {
                horse.setTarget(candidate);
            }
            return;
        }
    }

    // ------------------------------------------------------------------
    // Summon
    // ------------------------------------------------------------------

    /**
     * Top the local population of one mob up to a fixed number.
     *
     * <p>Three things here are requirements rather than choices, and each is on
     * the gene's page as a hazard:
     * <ul>
     *   <li><b>Count first.</b> The rule is "top up to N", not "make N" - getting
     *       that backwards turns a self-limiting locus into unbounded growth.</li>
     *   <li><b>Spawn through the normal path</b>, so {@code FinalizeSpawnEvent}
     *       fires and other mods' claim and spawn protections still apply. A
     *       direct placement bypasses every one of them.</li>
     *   <li><b>Never persistent.</b> What it makes must despawn like anything
     *       else, or two a day from every expressing horse accumulates for ever.</li>
     * </ul>
     */
    private static void maybeSummon(GeneAbility.Summon su, Horse horse, ServerLevel level,
                                     String geneKey) {
        if (!triggerFiresNow(su.trigger(), horse, false)) {
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Identifier rl = Identifier.tryParse(su.mob());
        EntityType<?> type = rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(rl);
        if (type == null || type == EntityType.PIG && !"minecraft:pig".equals(su.mob())) {
            warnUntranslated("summon:" + su.mob(), geneKey);
            return;
        }
        AABB box = horse.getBoundingBox().inflate(su.radius());
        long present = level.getEntities(type, box, e -> e.isAlive()).size();
        if (present >= su.upTo()) {
            return; // already enough of them; this is the whole self-limit
        }
        int wanted = (int) (su.upTo() - present);
        for (int i = 0; i < wanted; i++) {
            BlockPos at = findSpawnSpot(horse, level, su.radius());
            if (at == null) {
                return;
            }
            Entity made = type.spawn(level, at, EntitySpawnReason.NATURAL);
            if (made instanceof Mob mob) {
                // NOT setPersistenceRequired: it must despawn like anything else.
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at),
                        EntitySpawnReason.NATURAL, null);
            }
        }
    }

    private static BlockPos findSpawnSpot(Horse horse, ServerLevel level, double radius) {
        int r = (int) Math.max(1, radius);
        for (int tries = 0; tries < 12; tries++) {
            BlockPos p = horse.blockPosition().offset(
                    horse.getRandom().nextInt(2 * r + 1) - r, 0,
                    horse.getRandom().nextInt(2 * r + 1) - r);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
            if (level.isEmptyBlock(ground) && level.isEmptyBlock(ground.above())) {
                return ground;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------

    /** Whether an ability's trigger fires on this tick. Shared by sound and summon. */
    private static boolean triggerFiresNow(GeneAbility.Trigger trigger, Horse horse, boolean moving) {
        return switch (trigger) {
            case GeneAbility.Trigger.Continuous ignored -> true;
            case GeneAbility.Trigger.OnMove ignored -> moving;
            case GeneAbility.Trigger.Interval in -> horse.tickCount % Math.max(1, in.ticks()) == 0;
            // Event-driven triggers never fire from the tick loop.
            case GeneAbility.Trigger.OnInteract ignored -> false;
            case GeneAbility.Trigger.OnHurt ignored -> false;
            case GeneAbility.Trigger.OnOwnerHurt ignored -> false;
        };
    }
}
