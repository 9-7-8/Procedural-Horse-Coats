package com.example.horsegenetics.neoforge.server;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;

/**
 * <b>Saying so when food did nothing.</b>
 *
 * <h2>The bug this closes, which was never a refusal</h2>
 * Feeding a horse its favourite, or a special-diet horse the one thing it eats,
 * has always worked. What it did not do is <i>report</i>. Both paths heal the
 * horse and award bond, and both of those decline silently when there is
 * nothing to give: {@code heal} on a horse at full health is a no-op, and
 * {@link HorseCareHandler#awardBondFor} returns without moving anything for a
 * horse at {@code MAX_BOND} or one that has had its daily cap. So a player
 * feeding a healthy, well-bonded horse spent the item, heard the eat sound, saw
 * the hearts, and had <b>no way to tell</b> whether the locus had done anything
 * at all - which reads exactly like a gene that does not work.
 *
 * <p>So the food is still eaten - it was never the refusing that was wrong -
 * and the player is simply told. <b>Nothing is gated on this</b>: there is no
 * new condition under which feeding fails, and a horse that needs neither the
 * health nor the bond still takes the food, because "this animal will not
 * accept a carrot from you because it is too happy" is a worse lie than
 * silence was.
 *
 * <p>The message is the action bar rather than chat: it is a confirmation
 * nobody needs to keep, and feeding a stable of horses one after another should
 * not fill the log.
 */
final class FeedFeedback {

    private FeedFeedback() {
    }

    /**
     * The horse ate it and got nothing out of it. Called only when
     * <b>both</b> halves declined - full health and no bond movement - because
     * either one alone is a real effect worth not talking over.
     */
    static void ateButDidNotNeedIt(Player player, Horse horse) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) {
            return;
        }
        server.sendSystemMessage(Component.literal(horse.getName().getString())
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" eats it happily, but did not seem to need it.")
                        .withStyle(ChatFormatting.GRAY)), true);
    }
}
