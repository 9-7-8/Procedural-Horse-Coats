package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.ModAttributes;
import com.example.horsegenetics.neoforge.server.HorseDraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * <b>Animal-labour mods, and Horse Powered in particular: a horse's genetics
 * decide how hard it works.</b>
 *
 * <p>Horse Powered puts a horse on a block and has it drive machinery. Without
 * this, every horse in the game drives it at exactly the same rate, which makes
 * an entire mod's worth of breeding irrelevant the moment the animal steps onto
 * the treadmill - the case {@code wiki/compatibility.html} called "the most
 * rewarding piece of code on this page".
 *
 * <h2>How the integration actually works</h2>
 * By <b>attribute</b>, not by calling into their code. Every horse carries
 * {@link ModAttributes#DRAFT_POWER}, whose value this class keeps up to date:
 * {@code 1.0} for an ordinary horse, higher for a better one. Any mod that
 * wants to respect horse genetics multiplies its work rate by that attribute
 * and is done - no compile-time dependency on this jar, in either direction,
 * and it keeps working if either mod is absent.
 *
 * <p>{@link HorseDraft#workRate} is the same number for a caller who would
 * rather ask in Java.
 *
 * <h2>Why the number is not pulling ability alone</h2>
 * It is a weighted blend of pulling ability and speed - see
 * {@link com.example.horsegenetics.common.cart.CartDraft#workRate}. Pull is
 * weighted higher, because torque is what a mill wants, but speed still pays.
 * A model that read pull alone would make speed a dump stat on any horse
 * destined for machinery, which is precisely what this was asked not to do.
 *
 * <h2>What is NOT done here, and why</h2>
 * <b>Nothing reaches into Horse Powered's own classes.</b> This mod has never
 * been built against that jar, its block-entity and field names are not known
 * here, and a reflective patch written against guessed names would be a
 * fabrication that fails silently the first time they rename anything. The
 * attribute is real, tested and useful on its own; the half that reads it has
 * to be written against their actual API, by them or by a small addon, and that
 * is recorded as an open item rather than pretended away.
 *
 * <p>The detection below therefore does nothing but <b>say so in the log</b>.
 * That is deliberate: this mod's compat layer is otherwise entirely
 * capability-based with no {@code isLoaded} checks anywhere, and a line in the
 * log is the difference between "the integration is waiting for its other half"
 * and a player assuming it is broken.
 *
 * <p><b>Unverified:</b> the mod ids below are guesses. Horse Powered has never
 * been installed alongside this mod, so no id here has been seen to match. A
 * wrong id costs one absent log line and nothing else.
 */
public final class HorsePoweredCompat {

    /** Ids animal-labour mods are plausibly registered under. See the class note. */
    private static final List<String> LABOUR_MOD_IDS = List.of(
            "horsepowered", "horse_powered", "animalpowered", "horsepower");

    private HorsePoweredCompat() {
    }

    /**
     * Write this horse's work rate onto its {@link ModAttributes#DRAFT_POWER}
     * attribute.
     *
     * <p>Called from {@code HorseRecords.applyTraitsToEntity}, so it is updated
     * in exactly the places every other body stat is - on spawn, on load, and
     * whenever a genome changes - and can never drift out of step with the
     * genotype it is derived from.
     *
     * <p>The <i>base</i> value is set rather than a modifier added: this is not
     * a bonus on top of something, it is the whole number, and a modifier would
     * have to be found and removed again on every re-apply.
     */
    public static void apply(final AbstractHorse horse) {
        final AttributeInstance attr = horse.getAttribute(ModAttributes.DRAFT_POWER);
        if (attr == null) {
            // Not a type the attribute was attached to. Silent: correct for
            // every mob in the game that is not a horse.
            return;
        }
        attr.setBaseValue(HorseDraft.workRate(horse));
    }

    /** One line at startup, so a player can tell the two halves apart. */
    public static void announce() {
        for (final String id : LABOUR_MOD_IDS) {
            if (ModList.get().isLoaded(id)) {
                HorseGenetics.LOGGER.info(
                        "compat: '{}' is present. Every horse carries horsegenetics:draft_power "
                        + "(1.0 = an ordinary horse); a machine that multiplies its work rate by "
                        + "that attribute will respect horse genetics. See wiki/compatibility.html.",
                        id);
                return;
            }
        }
    }
}
