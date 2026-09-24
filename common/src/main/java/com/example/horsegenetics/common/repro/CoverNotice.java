package com.example.horsegenetics.common.repro;

/**
 * <b>Every sentence an owner reads when a natural cover did not happen</b>, and
 * the rule that stops those sentences becoming a wall of text.
 *
 * <p>Owner's ask: <i>"If a breeding attempt does not go through for any reason,
 * print the failure to the horse's owner in chat"</i>, and, for the crowding
 * case specifically, <i>"if a cover fails because there's too many horses, print
 * it to the chat for that player"</i>. Those are one feature: the cap is one of
 * the reasons.
 *
 * <p>Here rather than in the game module for {@link ReproText}'s and
 * {@link com.example.horsegenetics.common.care.HurtNotice}'s reason - the
 * wording is the feature, so the wording is what a test can pin down. Nothing
 * in here reads a horse or a level: {@code NaturalBreedingHandler} turns a
 * {@link NaturalCover.Decision} into a {@link Reason} and asks for the line.
 *
 * <h2>Not every refusal is an attempt</h2>
 * A mare who is simply between heats, or who has nobody to breed with, has not
 * had a breeding attempt fail - nothing was ever on the table, and those two
 * cover nearly every mare in the world nearly all of the time. Telling an owner
 * "no stallion in reach" every two seconds for every mare they own would train
 * them to stop reading the line that matters. So {@link NaturalCover.Verdict#NOT_NOW}
 * and {@link NaturalCover.Verdict#NO_STALLION} have no {@link Reason}: the
 * reasons here are the ones where a stallion was there, she was in heat, and
 * something the owner can do something about stopped it - plus the two ways the
 * roll itself can come back empty after a real cover.
 */
public final class CoverNotice {

    private CoverNotice() {
    }

    /**
     * <b>How long a mare stays quiet after one line</b>, in ticks - five minutes.
     *
     * <p>Much longer than {@link com.example.horsegenetics.common.care.HurtNotice#QUIET_TICKS},
     * because this is not an alarm: a crowded paddock is still crowded a minute
     * later and the owner has nothing new to act on. The scan asks every two
     * seconds and a heat lasts at least a Minecraft day, so without a long quiet
     * period one capped mare is 150 lines a heat.
     */
    public static final long QUIET_TICKS = 6_000L;

    /**
     * <b>Why she was not covered</b>, or - for the last three - how the cover
     * itself went. One constant per sentence an owner can act on.
     */
    public enum Reason {
        /** {@link NaturalCover.Verdict#CROWDED}: too many horses around her. */
        CROWDED,
        /** Short of {@link ReproRules#COVER_HEALTH}. */
        HURT,
        /** Somebody is on her. */
        RIDDEN,
        /** She is on a lead. */
        LEASHED,
        /** A cowboy's branded stock, which never breeds. */
        COWBOY_STOCK,
        /** She was covered, she was receptive, and the roll came back empty. */
        DID_NOT_TAKE,
        /**
         * She was covered outside the receptive window. Reachable because the
         * heat can end between the scan that started the courtship and the
         * cover three seconds later.
         */
        NOT_RECEPTIVE,
        /** She was covered and is in foal. Not a failure; here so one call site says either. */
        CONCEIVED;

        /** Is this the good one? The game module colours the line by it. */
        public boolean good() {
            return this == CONCEIVED;
        }
    }

    /**
     * May this mare be spoken about again?
     *
     * <p>A <b>changed reason is always worth a line</b> even inside the quiet
     * period: "she is hurt" following "the paddock is full" is news, and the
     * owner who just moved six horses out needs to hear what stopped it this
     * time rather than silence.
     *
     * @param lastReason what she was last spoken about for, or {@code null} for a mare never spoken about
     * @param lastTold   the tick of that line; ignored when {@code lastReason} is {@code null}
     */
    public static boolean dueAgain(Reason lastReason, long lastTold, Reason reason, long now) {
        if (lastReason == null || lastReason != reason) {
            return true;
        }
        return now - lastTold >= QUIET_TICKS || now < lastTold;
    }

    /**
     * <b>The line the owner reads.</b>
     *
     * @param crowd how many horses are within {@link ReproRules#NATURAL_CAP_RADIUS}
     *              of her and what this world allows, for {@link Reason#CROWDED};
     *              ignored by every other reason
     */
    public static String line(String mareName, Reason reason, NaturalCover.Crowd crowd) {
        switch (reason) {
            case CROWDED:
                return mareName + " was not covered: " + crowd.nearby() + " other horses within "
                        + (int) ReproRules.NATURAL_CAP_RADIUS + " blocks of her, and the limit is "
                        + crowd.cap() + ". Move some out of the paddock.";
            case HURT:
                return mareName + " was not covered: she is hurt, and a natural cover needs "
                        + Math.round(ReproRules.COVER_HEALTH * 100.0)
                        + "% health. A horse heals beside water, once it has eaten.";
            case RIDDEN:
                return mareName + " was not covered while she is being ridden.";
            case LEASHED:
                return mareName + " was not covered while she is on a lead.";
            case COWBOY_STOCK:
                return mareName + " is the cowboy's stock, and branded stock does not breed.";
            case DID_NOT_TAKE:
                return "It didn't take - " + mareName + " was covered but is not in foal.";
            case NOT_RECEPTIVE:
                return "It didn't take - " + mareName + " was covered after her heat ended.";
            case CONCEIVED:
            default:
                return mareName + " was covered, and is in foal.";
        }
    }
}
