package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.server.ReproHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * <b>A mare's reproductive state in Jade's look-at tooltip.</b> "Pregnant", "In
 * heat", "Nursing", or no line at all.
 *
 * <h2>Why this is allowed to exist</h2>
 * {@link HorsePoweredCompat} refuses to touch another mod's classes, and that
 * has been the rule here. The reason it gives is <i>guessing</i>: a reflective
 * patch written against names the other mod never promised fails silently the
 * first time they rename anything. Jade publishes a plugin API, versions it,
 * and documents it - so building against it is the opposite of a guess, and
 * this is a deliberate exception rather than a drift. It is still
 * <b>compile-only</b>: the jar is never shipped and never required, and nothing
 * in this mod refers to this class. Jade finds it by scanning for
 * {@link WailaPlugin}, so with Jade absent the class is simply never loaded.
 *
 * <h2>Why it says so little</h2>
 * This mod has three readouts about breeding and they are deliberately
 * unequal: the <b>vet's kit</b> reveals twins and which half of a heat she is
 * in, the <b>information screen</b> gives a sentence with a countdown, and this
 * gives a word. Anything more here would make the kit pointless, which is a
 * settled decision - see {@code ReproText.glanceLine} for what each one
 * withholds and why.
 *
 * <p>It is not owner-gated, and that is not an oversight: the same breeding
 * line is already on the information screen of any horse a player can look at,
 * including horses they do not own ({@code HorseInfoInteraction} opens on
 * untamed strangers on purpose). Nothing here is newly visible; it is the same
 * fact, one glance sooner.
 *
 * <h2>Jade on the client but not the server</h2>
 * The heat model is not synced - {@code ModAttachments.HORSE_REPRO} says so
 * outright, and no client needs an embryo's genome. So the real answer comes
 * from {@link #appendServerData}, which only runs if Jade is installed
 * server-side too. When it is not, {@link #appendTooltip} falls back to the one
 * fact the client does hold: {@code ModAttachments.PREGNANT} is a synced
 * boolean, so a client-only Jade still says "Pregnant" and stays quiet about
 * the rest. <b>Unverified in either configuration</b> - see
 * {@code wiki/compatibility.html#verification}.
 */
@WailaPlugin
public class JadeHorsePlugin implements IWailaPlugin {

    /** The provider is stateless, and Jade wants the same instance in both passes. */
    private static final HorseRepro HORSE_REPRO = new HorseRepro();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(HORSE_REPRO, Horse.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(HORSE_REPRO, Horse.class);
    }

    /**
     * Both halves of one line: the server works out the words, the client draws
     * them. One class because the two must agree about the NBT key, and a key
     * that lives in two files is a key that eventually differs in two files.
     */
    private static final class HorseRepro implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

        private static final Identifier UID = Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "repro");

        /** The one key in the server-data tag. Written below, read below. */
        private static final String KEY = "repro_glance";

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (accessor.getEntity() instanceof Horse horse) {
                String line = ReproHandler.glanceLine(horse);
                if (!line.isEmpty()) {
                    // Absent rather than empty: an empty string would still cost
                    // a tag and a packet on every horse a player looks at, and
                    // most horses have nothing to say.
                    tag.putString(KEY, line);
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            String line = accessor.getServerData().getStringOr(KEY, "");
            if (line.isEmpty()) {
                line = clientOnlyFallback(accessor);
            }
            if (!line.isEmpty()) {
                tooltip.add(Component.literal(line));
            }
        }

        /**
         * What can be said with Jade on the client alone. {@code PREGNANT} is
         * the only part of the repro model that is synced, and it is synced so
         * the client can refuse a breeding food without asking the server.
         * Borrowing it here costs nothing and is the difference between a
         * tooltip that half works without Jade on the server and one that is
         * blank.
         */
        private static String clientOnlyFallback(EntityAccessor accessor) {
            if (accessor.isServerConnected() || !(accessor.getEntity() instanceof Horse horse)) {
                // Jade IS on the server and still said nothing: she genuinely
                // has nothing to report, and guessing over the top of that
                // would contradict it.
                return "";
            }
            return ReproHandler.pregnantEitherSide(horse) ? "Pregnant" : "";
        }
    }
}
