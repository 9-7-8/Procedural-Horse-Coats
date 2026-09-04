package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Foals that do not make it.</b> A foal whose genotype resolves to
 * {@link Viability#LETHAL_AT_BIRTH} - overo lethal white, fragile foal
 * syndrome, EFIH, naked foal syndrome, skeletal atavism, or a {@code D1/D1}
 * dwarf - is born normally and then dies over a few seconds.
 *
 * <h2>Why born, and why not instant</h2>
 * The foal gets a name, a {@link HorseRecord} and a place in the family tree
 * before it dies, and that is the point of the feature rather than an accident
 * of implementation. A pairing that silently produced nothing would teach a
 * player nothing; a foal that drops dead in the same tick it appears reads as a
 * bug. What teaches them something is a named animal, visibly wrong, with a chat
 * line saying which disorder it was - because that line is a statement about
 * <i>both</i> parents, and it is the moment a pedigree stops being decoration.
 *
 * <h2>Nothing is stored</h2>
 * There is no attachment and no countdown. Being lethal is a property of the
 * genotype, so this handler re-reads it - which means a foal that logs out
 * mid-death still dies on the way back in, and flipping the server setting takes
 * effect immediately in both directions.
 *
 * <h2>The three guards</h2>
 * <ul>
 *   <li><b>Foals only.</b> An adult is never killed by this. The only ways to
 *       get an adult with a lethal genotype are the custom spawn egg and a
 *       hand-written {@code /summon}, and both are debug tools for looking at a
 *       coat - killing the horse you spawned to look at would be obnoxious, and
 *       lethal white is exactly the coat people want to look at.</li>
 *   <li><b>Not in the gallery.</b> The genotype dimension stocks a pen for
 *       lethal white, and it is a viewing gallery, not a fight.</li>
 *   <li><b>Only on {@code health.mode = FULL}.</b> See {@link ServerConfig}.</li>
 * </ul>
 *
 * <h2>It has to out-damage the healing</h2>
 * {@code HorseCareHandler} regenerates a hurt horse near food and water by 1 (or
 * 2 in a herd) every 30 ticks. The damage here is a <b>fraction of the foal's
 * own max health</b> applied once a second, so it beats that comfortably at any
 * health value and kills in about four seconds whether the foal has one heart or
 * ten - and a player cannot save one by standing it next to a hay bale.
 */
@EventBusSubscriber
public final class LethalFoalHandler {

    /** The custom damage type - see {@code data/horsegenetics/damage_type/genetic_defect.json}. */
    public static final ResourceKey<DamageType> GENETIC_DEFECT = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("horsegenetics", "genetic_defect"));

    /** One hit a second: slow enough to see, fast enough not to look stuck. */
    private static final int INTERVAL_TICKS = 20;

    /** Fraction of the foal's max health per hit - four hits to kill, at any size. */
    private static final float DAMAGE_FRACTION = 0.28f;

    /** Floor, so a very frail foal still dies promptly rather than chipping away. */
    private static final float MIN_DAMAGE = 2.0f;

    /** How far a chat line about a lost foal carries. */
    private static final double MESSAGE_RADIUS = 32.0;

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) return;
        if (!(horse.level() instanceof ServerLevel level)) return;
        if (!horse.isBaby() || horse.isDeadOrDying()) return;
        if (!ServerConfig.lethalsActive()) return;
        if (level.dimension().equals(DebugPenManager.DEBUG_LEVEL)) return;
        if (horse.tickCount % INTERVAL_TICKS != 0) return;

        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasGenome()) return;

        Traits traits = HorseRecords.traitsOf(record);
        if (traits.viability() != Viability.LETHAL_AT_BIRTH) return;

        float damage = Math.max(MIN_DAMAGE, horse.getMaxHealth() * DAMAGE_FRACTION);
        horse.hurtServer(level, geneticDefect(level), damage);
    }

    private static DamageSource geneticDefect(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GENETIC_DEFECT));
    }

    /**
     * Tell the player what just happened, at the moment the foal is born rather
     * than when it dies - the death message names the damage type, this names
     * the disorder. Sent to the breeder if there is one, otherwise to whoever is
     * near enough to have seen it.
     */
    public static void announceLethalBirth(Horse foal, HorseRecord record, Traits traits,
                                           @Nullable Player breeder) {
        Condition cause = traits.lethalCondition().orElse(null);
        if (cause == null || !(foal.level() instanceof ServerLevel level)) {
            return;
        }
        Component line = Component.literal(record.displayName() + " was born with "
                + cause.name().toLowerCase() + ". " + cause.description());
        if (!ServerConfig.lethalsActive()) {
            line = Component.literal(record.displayName() + " carries " + cause.name().toLowerCase()
                    + ", but foal deaths are switched off on this server.");
        }
        if (breeder != null) {
            breeder.sendSystemMessage(line);
            return;
        }
        List<Player> nearby = level.getEntitiesOfClass(Player.class,
                new AABB(foal.blockPosition()).inflate(MESSAGE_RADIUS));
        for (Player player : nearby) {
            player.sendSystemMessage(line);
        }
    }

    /**
     * Tell the player why a pairing produced nothing. Without this an embryonic
     * lethal is indistinguishable from a bug - two horses that will breed with
     * anything except each other, for no visible reason.
     */
    public static void announceRefusedPairing(@Nullable Player breeder, Condition cause) {
        if (breeder == null) {
            return;
        }
        breeder.sendSystemMessage(Component.literal(
                "The pairing produced no foal: " + cause.description()));
    }

    private LethalFoalHandler() {
    }
}
