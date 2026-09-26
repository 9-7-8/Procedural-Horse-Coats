package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.LastStand;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * <b>No horse dies to one blow.</b> Damage that would empty a horse's health
 * leaves it on {@code behaviour.last_stand_health} instead, and nothing can
 * touch it for {@code behaviour.last_stand_immunity_ticks} afterwards - then the
 * save is spent until the horse has healed back to
 * {@code behaviour.last_stand_rearm_fraction} of its maximum.
 *
 * <p>Owner's ask: <i>"Horses should never be able to be one-shot and die - any
 * health which would kill them takes them to one health point instead, and they
 * get 120 ticks of immunity to damage, then the immunity disables until they
 * heal back to full health again."</i> All four numbers are settings; the ask is
 * their defaults.
 *
 * <p>The arithmetic is {@link LastStand}, in the game-free module, with
 * {@code LastStandTest} on it. This class is the translation: which horse, what
 * hurt it, whether that is a thing this mod is allowed to let anyone survive,
 * and where the stamp is kept.
 *
 * <h2>Two events, because they are two different refusals</h2>
 * The <b>window</b> is {@link LivingIncomingDamageEvent}, cancelled outright -
 * the same shape {@code GeneAbilityHandler.onTraversalDamage} uses for a gene's
 * fire immunity, and the only one that means <i>no damage happened</i> rather
 * than <i>zero damage happened</i>. The <b>save</b> is
 * {@link LivingDamageEvent.Pre}, because that is the first point at which the
 * number is the one that will actually come off the health: armour and
 * enchantments have been taken out of it by then, and a horse in barding would
 * otherwise be judged on a blow it was never going to take in full.
 *
 * <p>Setting the new damage to zero and the health by hand is exactly how
 * vanilla's totem of undying does it ({@code LivingEntity.checkTotemDeathProtection}
 * is a bare {@code setHealth(1.0F)}), and it is safe in {@code Pre} for a reason
 * worth writing down: {@code actuallyHurt} reads the container back <em>after</em>
 * the event and only subtracts when it is non-zero, so the health set here is the
 * health the horse ends the tick on. The hit still lands in every other respect -
 * hurt animation, hurt sound, knockback and {@code LivingDamageEvent.Post} - so
 * nothing that watches a horse being damaged goes quiet.
 *
 * <h2>What may still kill a horse outright</h2>
 * <ul>
 *   <li><b>A genetic defect.</b> {@code horsegenetics:genetic_defect} is exempt
 *       in both directions, and has to be: {@link LethalFoalHandler} is tuned to
 *       out-damage the care handler's regeneration, so a foal rescued to half a
 *       heart every six seconds would be an <em>immortal</em> lethal foal - the
 *       exact opposite of the mechanic. The same source carries the half-heart a
 *       mare loses to an embryonic lethal, which {@code LethalFoalHandler}
 *       deliberately allows to be her last.</li>
 *   <li><b>Anything in {@code minecraft:bypasses_invulnerability}</b> - the void,
 *       {@code /kill}, and the generic kill {@link LycanthropyHandler} falls back
 *       to when a were-animal dies of something its horse body shrugs off. That
 *       fallback was written for gene immunities and covers this one for free.</li>
 * </ul>
 *
 * <p><b>Every horse, not only a horse with a genome.</b> {@code AbstractHorse},
 * matching {@link HorseHurtNoticeHandler}, {@link HorseDeathNoticeHandler} and
 * the care handler's own herd scan rather than the goal handlers - a rule about
 * not dying should not depend on having been born here. That family is wider
 * than it sounds: donkeys and mules, which this mod breeds, but also vanilla's
 * llamas and camels, which it does not. Deliberate, on the grounds that the
 * mod's consistent answer to "what is a horse" is this class and a narrower one
 * here would be the surprise - but it is the sort of call an owner may want
 * narrowed, so it is written down rather than assumed.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
@EventBusSubscriber
public final class HorseLastStandHandler {

    private HorseLastStandHandler() {
    }

    /**
     * The {@link HorseCooldownsAttachment} key holding the game tick of the last
     * save - absent for a horse whose save is available. Re-using the cooldown
     * store rather than minting an attachment: it is already {@code copyOnDeath},
     * already serialized, and already the place the mod keeps "when did this last
     * happen to this horse".
     */
    private static final String KEY = "last_stand";

    /**
     * <b>The window.</b> Runs before anything has been subtracted, and refuses
     * the whole hit.
     *
     * <p>It re-arms too, because this is the one place that is guaranteed to run
     * whenever the answer could matter: "has it healed back to full" is a fact
     * about the horse rather than an event, so reading it here costs nothing
     * while nothing is attacking and cannot go stale in an unloaded chunk.
     */
    @SubscribeEvent
    static void onIncoming(LivingIncomingDamageEvent event) {
        // Cheapest question first: this fires for every hit anything in the
        // world takes, and almost none of them are a horse.
        if (!(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ServerConfig.lastStand() || unsurvivable(event.getSource())) {
            return;
        }

        HorseCooldownsAttachment cooldowns = horse.getData(ModAttachments.HORSE_COOLDOWNS.get());
        long spentAt = cooldowns.last(KEY);
        if (!LastStand.armed(spentAt)
                && LastStand.rearms(horse.getHealth(), horse.getMaxHealth(),
                        ServerConfig.lastStandRearmFraction())) {
            horse.setData(ModAttachments.HORSE_COOLDOWNS.get(), cooldowns.clear(KEY));
            spentAt = LastStand.NEVER;
        }
        if (LastStand.immune(spentAt, level.getGameTime(), ServerConfig.lastStandImmunityTicks())) {
            event.setCanceled(true);
        }
    }

    /**
     * <b>The save.</b> Runs on the number that is about to come off the health,
     * which is why it is here and not on the incoming event.
     */
    @SubscribeEvent
    static void onKillingBlow(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        // EVERY REFUSAL IS LOGGED FROM HERE DOWN, and only when the blow would
        // have been fatal - so the log stays quiet until a horse actually dies,
        // and then says which of the four reasons it was.
        //
        // Added because "last stand didn't fire, it just died instantly" was
        // reported off a high fall (owner, 2026-09-25) and there was no way to
        // tell from the log which branch had declined: the save writes an
        // ActionTrace line on SUCCESS and said nothing on any refusal, so a
        // failure and a feature that was never reached looked identical. Fall
        // damage is not in minecraft:bypasses_invulnerability and the arithmetic
        // checks out on paper, so the next occurrence is what has to say.
        float blow = event.getNewDamage();
        boolean wouldBeFatal = LastStand.fatal(blow, horse.getHealth());
        if (!ServerConfig.lastStand() || unsurvivable(event.getSource())) {
            if (wouldBeFatal) {
                ActionTrace.log("last-stand", ActionTrace.describeShort(horse) + " was NOT saved from "
                        + event.getSource().getMsgId() + " (" + blow + " vs " + horse.getHealth()
                        + " health): " + (ServerConfig.lastStand()
                                ? "that damage type is exempt"
                                : "behaviour.last_stand is off in phc/server.toml"));
            }
            return;
        }
        if (!wouldBeFatal) {
            return;
        }
        HorseCooldownsAttachment cooldowns = horse.getData(ModAttachments.HORSE_COOLDOWNS.get());
        if (!LastStand.armed(cooldowns.last(KEY))) {
            // Spent, and not healed since - this one lands. The commonest
            // legitimate reason a save "did not fire", and the one that reads as
            // a bug because the previous save may have been minutes ago.
            ActionTrace.log("last-stand", ActionTrace.describeShort(horse) + " was NOT saved from "
                    + event.getSource().getMsgId() + " (" + blow + " vs " + horse.getHealth()
                    + " health): its save was already spent at tick " + cooldowns.last(KEY)
                    + " and it has not healed back to "
                    + ServerConfig.lastStandRearmFraction() + " of maximum since");
            return;
        }

        long now = level.getGameTime();
        horse.setData(ModAttachments.HORSE_COOLDOWNS.get(), cooldowns.stamp(KEY, now));
        event.setNewDamage(0.0F);
        horse.setHealth(LastStand.healthLeft(ServerConfig.lastStandHealth(), horse.getMaxHealth()));

        // Vanilla's own vocabulary for "that should have killed it", so a player
        // who has ever held a totem reads this without being told. For everyone
        // watching, not only the owner - the rescue is the visible event.
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.6, horse.getZ(),
                24, 0.4, 0.5, 0.4, 0.2);

        announce(horse);
        ActionTrace.log("last-stand", ActionTrace.describeShort(horse) + " survived "
                + event.getSource().getMsgId() + " (" + blow + " of "
                + event.getOriginalDamage() + " would have landed) and is immune for "
                + ServerConfig.lastStandImmunityTicks() + " ticks from " + now);
    }

    /**
     * Tell the owner, once, what just happened and what it costs.
     *
     * <p>Deliberately <b>not</b> behind {@code notices.owned_horse_damage}: that
     * switch exists to stop a pen on fire becoming a wall of text, and this is the
     * opposite kind of line. A horse can only be saved once per recovery, so it
     * cannot repeat without the player healing one to full in between - and a
     * horse that walks out of an explosion on half a heart with nothing said reads
     * as the mod failing to kill it, which is {@link LethalFoalHandler}'s "the
     * chat line is the feature" argument pointed the other way.
     */
    private static void announce(AbstractHorse horse) {
        LivingEntity owner = horse.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;         // wild, or an owner who is not here to be alarmed
        }
        player.sendSystemMessage(Component.literal(
                LastStand.survived(HorseNotices.name(horse), ServerConfig.lastStandImmunityTicks()))
                .withStyle(ChatFormatting.RED));
    }

    /**
     * <b>Is this a death the save is not allowed to postpone?</b> The two
     * exemptions have nothing in common except that - see the class notes for
     * why each one is here.
     */
    private static boolean unsurvivable(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(LethalFoalHandler.GENETIC_DEFECT);
    }
}
