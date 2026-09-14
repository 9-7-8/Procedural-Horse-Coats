package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.Embryo;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproState;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ArmedCarrotsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Pregnancy in the world</b> - the thin NeoForge side of {@code common.repro}.
 *
 * <ul>
 *   <li>{@link #breed} is the one entry point every pregnancy path calls:
 *       the seed jar, breeding carrots ({@code HorseBreedingHandler}) and a
 *       stallion left with a mare ({@code NaturalBreedingHandler}). The draw and the odds are
 *       {@link Conception#attempt}; this only fetches the facts and stores the
 *       answer.</li>
 *   <li>The tick, every {@link #SCAN} ticks, for a horse that has a record:
 *       an early loss when it is due, birth when the due tick has passed, the
 *       late-pregnancy slowdown, and weaning.</li>
 *   <li>The words: the action-bar lines, and the info screen's one line.</li>
 * </ul>
 *
 * <p>Every time is an absolute game tick. A mare whose chunk was unloaded past
 * her due date foals the next time she is ticked, and the foal is born then
 * (owner, 2026-09-13).
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class ReproHandler {

    private ReproHandler() {
    }

    /** Two seconds. Everything is a deadline, so this only decides how late it can be noticed. */
    static final int SCAN = 40;

    private static final Identifier LATE_PREGNANCY =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "repro/late_pregnancy");

    private static final double TELL_RADIUS = 32.0;

    // ------------------------------------------------------------------
    // The record
    // ------------------------------------------------------------------

    /**
     * The horse's record, <b>without attaching a default</b> - a mare who has
     * never been bred has a cycle (from her id) and nothing to save, and
     * asking about her heat from a goal every few seconds should not write an
     * attachment onto every horse in the world.
     */
    public static Reproduction of(Horse horse) {
        return horse.hasData(ModAttachments.HORSE_REPRO.get())
                ? horse.getData(ModAttachments.HORSE_REPRO.get())
                : Reproduction.fresh(horse.getUUID());
    }

    static void set(Horse horse, Reproduction r) {
        horse.setData(ModAttachments.HORSE_REPRO.get(), r);
    }

    /** Where a mare is now. Meaningless for a stallion - ask only of mares. */
    public static ReproState stateOf(Horse mare) {
        return ReproRules.stateAt(of(mare), mare.level().getGameTime(), ServerConfig.reproTiming());
    }

    public static boolean receptive(Horse mare) {
        return stateOf(mare).receptive();
    }

    // ------------------------------------------------------------------
    // Breeding carrots
    // ------------------------------------------------------------------

    /** The carrot effect tokens waiting on this horse for its next conception. */
    public static List<String> armedTokens(Horse horse) {
        return horse.hasData(ModAttachments.ARMED_CARROTS.get())
                ? horse.getData(ModAttachments.ARMED_CARROTS.get()).effects()
                : List.of();
    }

    public static boolean armed(Horse horse) {
        return !armedTokens(horse).isEmpty();
    }

    public static void disarm(Horse horse) {
        if (armed(horse)) {
            horse.setData(ModAttachments.ARMED_CARROTS.get(), ArmedCarrotsAttachment.EMPTY);
        }
    }

    // ------------------------------------------------------------------
    // Conception
    // ------------------------------------------------------------------

    /**
     * <b>One breeding on the mod's own paths</b> - the entry point for breeding
     * carrots, the seed jar and natural covers. Folds whatever
     * carrot effects are armed on the mare and on the sire (or carried in the
     * jar) into the draw, tries once, and <b>uses the carrots up only if it
     * takes</b> (owner, 2026-09-13).
     *
     * @param liveSire   the stallion when he is standing there, else {@code null}
     * @param jarCarrots the sire's effects when there is no live sire - a jar's
     */
    public static Conception.Result breed(Horse mare, HorseRecord mareRecord, Genome mareGenome,
                                          Genome sireGenome, HorseRecord sireRecord, @Nullable Horse liveSire,
                                          List<CarrotEffect> jarCarrots, @Nullable Player breeder) {
        return breed(mare, mareRecord, mareGenome, sireGenome, sireRecord, liveSire, jarCarrots,
                breeder == null ? "" : breeder.getGameProfile().name(), breeder);
    }

    /**
     * The same, crediting {@code bredBy} rather than the player at hand - a
     * natural cover has nobody at hand, and the foal is credited to the mare's
     * owner.
     */
    public static Conception.Result breed(Horse mare, HorseRecord mareRecord, Genome mareGenome,
                                          Genome sireGenome, HorseRecord sireRecord, @Nullable Horse liveSire,
                                          List<CarrotEffect> jarCarrots, String bredBy, @Nullable Player breeder) {
        Rng rng = HorseRecords.rng(mare);
        List<CarrotEffect> damCarrots = CarrotEffect.parseList(armedTokens(mare));
        List<CarrotEffect> sireCarrots = liveSire != null ? CarrotEffect.parseList(armedTokens(liveSire)) : jarCarrots;
        GameteBias damBias = CarrotEffect.fold(damCarrots, mareGenome.genotype(), rng);
        GameteBias sireBias = CarrotEffect.fold(sireCarrots, sireGenome.genotype(), rng);

        Conception.Result result = tryConceive(mare, mareRecord, mareGenome, sireGenome, sireRecord, liveSire,
                damBias, sireBias, bredBy, breeder);
        boolean took = result.outcome() == Conception.Outcome.CONCEIVED;

        // THE OTHER END OF THE CARROT. Feeding one is separated from its effect
        // by a heat, a breeding and a second horse, so the feed's own log line
        // cannot say whether it did anything. This one can: whether there were
        // effects, whether they folded to anything, and whether they were used.
        if (!damCarrots.isEmpty() || !sireCarrots.isEmpty()) {
            ActionTrace.log("carrot", ActionTrace.describeShort(mare) + " bred with carrot effects "
                    + ids(damCarrots) + " on her and " + ids(sireCarrots) + " on the sire"
                    + (damBias.isNone() && sireBias.isNone() ? " - which folded to NO bias" : "")
                    + (took ? " - used up" : " - still armed, because it did not take"));
        }
        if (took) {
            disarm(mare);
            if (liveSire != null) {
                disarm(liveSire);
            }
        }
        return result;
    }

    private static String ids(List<CarrotEffect> effects) {
        if (effects.isEmpty()) {
            return "(none)";
        }
        List<String> ids = new ArrayList<>();
        for (CarrotEffect e : effects) {
            ids.add(e.id());
        }
        return String.join(", ", ids);
    }

    /**
     * The attempt itself, once the carrots are folded. Stores the pregnancy on
     * the mare if it takes, and counts a cover on {@code liveSire} if the mare
     * was receptive. Says nothing - {@link #announce} does that.
     */
    private static Conception.Result tryConceive(Horse mare, HorseRecord mareRecord, Genome mareGenome,
                                                Genome sireGenome, HorseRecord sireRecord, @Nullable Horse liveSire,
                                                GameteBias damBias, GameteBias sireBias, String bredBy,
                                                @Nullable Player breeder) {
        long now = mare.level().getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        int covers = liveSire == null ? 0 : of(liveSire).coversOn(now, t.dayTicks());
        Conception.Mating mating = new Conception.Mating(mareGenome, mareRecord.lineage(),
                sireGenome, sireRecord.lineage(), sireRecord.id(), sireRecord.firstName(), sireRecord.lastName(),
                sireRecord.generation(), damBias, sireBias, bredBy);
        Conception.Result result = Conception.attempt(mating, of(mare), now, t, covers,
                ServerConfig.healthGeneticsActive(), ServerConfig.lethalsActive(), HorseRecords.rng(mare));

        if (result.outcome() != Conception.Outcome.NOT_RECEPTIVE && liveSire != null) {
            recordCover(liveSire);
        }
        result.pregnancy().ifPresent(p -> {
            set(mare, of(mare).withPregnancy(p));
            for (Embryo e : p.embryos()) {
                Genome foal = e.foal();
                BreedingDebug.reportDraw(mareRecord, sireRecord, mareGenome, sireGenome, foal,
                        HorseTraits.resolve(foal.genotype(), foal.epigenome(), ServerConfig.healthGeneticsActive()),
                        breeder);
            }
        });
        ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " bred to " + sireRecord.displayName()
                + ": " + result.outcome() + String.format(" (chance %.2f)", result.chance())
                + result.pregnancy().map(p -> " - " + p.embryos().size() + " embryo(s), due at tick "
                        + p.dueTick() + (p.hasEarlyLoss() ? ", early loss at tick " + p.lossTick() : ""))
                .orElse(""));
        return result;
    }

    /** A cover or a jar fill, counted against his day. */
    public static void recordCover(Horse stallion) {
        long now = stallion.level().getGameTime();
        set(stallion, of(stallion).withCover(now, ServerConfig.reproTiming().dayTicks()));
    }

    /** How an attempt went, on the action bar of whoever arranged it. */
    public static void announce(Horse mare, @Nullable Player player, Conception.Result result) {
        String name = nameOf(mare);
        switch (result.outcome()) {
            case CONCEIVED -> overlay(player, name + " is pregnant.", ChatFormatting.GREEN);
            case DID_NOT_TAKE -> overlay(player, "It didn't take - " + name + " is not pregnant.",
                    ChatFormatting.YELLOW);
            case NOT_RECEPTIVE -> overlay(player, notReceptive(mare) + " It didn't take.", ChatFormatting.YELLOW);
        }
    }

    public static void overlay(@Nullable Player player, String text, ChatFormatting colour) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(text).withStyle(colour), true);
        }
    }

    /** Why a mare cannot be bred right now, and how long until she can. */
    public static String notReceptive(Horse mare) {
        Reproduction r = of(mare);
        long now = mare.level().getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        String name = nameOf(mare);
        long wait = ReproRules.ticksUntilReceptive(r, now, t);
        return switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT -> name + " is pregnant - " + duration(r.pregnancy().get().ticksLeft(now)) + " to go.";
            case POSTPARTUM -> name + " foaled recently - foal heat in " + duration(wait) + ".";
            case ESTRUS, FOAL_HEAT -> name + " is in heat.";
            case DIESTRUS -> name + " is not in heat - " + duration(wait) + " to go.";
        };
    }

    /** Ticks as real time a player can plan around: minutes, then hours. */
    static String duration(long ticks) {
        long minutes = Math.max(1L, (ticks + 1_199L) / 1_200L);
        if (minutes < 60L) {
            return "about " + minutes + " min";
        }
        long hours = (minutes + 59L) / 60L;
        return "about " + hours + (hours == 1L ? " hour" : " hours");
    }

    /**
     * <b>The info screen's one line</b>, in the Body section (owner, 2026-09-13).
     * Empty for a stallion or a foal. Twins are not revealed - nobody has scanned
     * her.
     */
    public static String breedingLine(Horse horse) {
        if (horse.isBaby() || !HorseRecords.hasRealRecord(horse) || HorseRecords.of(horse).sex() != Sex.FEMALE) {
            return "";
        }
        Reproduction r = of(horse);
        long now = horse.level().getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        String line = switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT -> {
                Pregnancy p = r.pregnancy().get();
                yield "Pregnant - " + duration(p.ticksLeft(now)) + " to go"
                        + (ReproRules.late(p, now) ? ", heavy and slow" : "");
            }
            case POSTPARTUM -> "Recently foaled - foal heat in " + duration(ReproRules.ticksUntilReceptive(r, now, t));
            case FOAL_HEAT -> "In foal heat";
            case ESTRUS -> ReproRules.inPeak(r, now, t) ? "In heat - best time now" : "In heat";
            case DIESTRUS -> "Not in heat - " + duration(ReproRules.ticksUntilReceptive(r, now, t)) + " to go";
        };
        return r.lactating() ? line + " • nursing" : line;
    }

    /**
     * <b>What the vet's kit says</b> - everything the info screen's one line
     * leaves out: twins, where in her heat she is and when it ends, and a
     * stallion's covers today. It does not diagnose a lethal embryo; that is
     * still something a player works out from a pedigree.
     */
    public static List<String> vetReport(Horse horse) {
        List<String> lines = new ArrayList<>();
        HorseRecord record = HorseRecords.of(horse);
        String name = nameOf(horse);
        long now = horse.level().getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = of(horse);
        if (record.sex() == Sex.MALE) {
            if (record.gelded()) {
                lines.add(name + " is a gelding.");
            } else if (horse.isBaby()) {
                lines.add(name + " is a colt, too young to breed.");
            } else {
                lines.add(name + " is an entire stallion: " + r.coversOn(now, t.dayTicks()) + " of "
                        + ReproRules.FREE_COVERS_PER_DAY + " covers made today.");
            }
            return lines;
        }
        if (horse.isBaby()) {
            lines.add(name + " is a filly, too young to breed.");
            return lines;
        }
        switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT -> {
                Pregnancy p = r.pregnancy().get();
                lines.add(name + " is pregnant with " + (p.twins() ? "twins" : "one foal") + " - "
                        + duration(p.ticksLeft(now)) + " to go.");
            }
            case POSTPARTUM -> lines.add(name + " foaled recently. Foal heat begins in "
                    + duration(ReproRules.ticksUntilReceptive(r, now, t)) + ".");
            case FOAL_HEAT -> lines.add(name + " is in foal heat. It ends in "
                    + duration(ReproRules.heatEndsAt(r, now, t) - now) + ".");
            case ESTRUS -> {
                long start = ReproRules.heatStartAt(r, now, t);
                String half = ReproRules.inPeak(r, now, t)
                        ? "in the better half"
                        : "in the first half - the better half starts in "
                                + duration(start + t.estrusTicks() / 2 - now);
                lines.add(name + " is in heat, " + half + ". It ends in "
                        + duration(ReproRules.heatEndsAt(r, now, t) - now) + ".");
            }
            case DIESTRUS -> lines.add(name + " is not in heat. Her next heat begins in "
                    + duration(ReproRules.ticksUntilReceptive(r, now, t)) + ".");
        }
        if (ReproRules.stateAt(r, now, t).receptive() && !ReproRules.mayTryNaturally(r, now, t)) {
            lines.add("A stallion has already covered her this heat.");
        }
        if (r.lactating()) {
            lines.add("She is nursing.");
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // The tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0 || !horse.hasData(ModAttachments.HORSE_REPRO.get())
                || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        long now = level.getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = of(horse);
        Optional<Pregnancy> pregnancy = r.pregnancy();
        if (pregnancy.isPresent()) {
            Pregnancy p = pregnancy.get();
            if (p.earlyLossDue(now)) {
                loseEarly(horse, level, r, p);
            } else if (p.due(now)) {
                foal(horse, level, r, p, now);
            } else {
                lateSpeed(horse, ReproRules.late(p, now));
            }
        } else {
            lateSpeed(horse, false);
        }
        r = of(horse);
        if (r.lactating()) {
            nurse(horse, level, r, now, t);
        }
    }

    private static void loseEarly(Horse mare, ServerLevel level, Reproduction r, Pregnancy p) {
        Condition cause = null;
        for (Embryo e : p.embryos()) {
            if (e.lostEarly()) {
                Genome g = e.foal();
                cause = HorseTraits.resolve(g.genotype(), g.epigenome(), ServerConfig.healthGeneticsActive())
                        .lethalCondition().orElse(null);
                break;
            }
        }
        Optional<Pregnancy> after = p.afterEarlyLoss();
        set(mare, r.withPregnancy(after));
        ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " early loss: " + p.lostEarlyCount()
                + " of " + p.embryos().size() + " embryo(s)" + (cause == null ? "" : " (" + cause.id() + ")"));
        if (after.isEmpty()) {
            lateSpeed(mare, false);
            if (cause != null) {
                LethalFoalHandler.announceMiscarriage(mare, ownerPlayer(mare), cause);
            } else {
                tellNearby(level, mare, Component.literal(nameOf(mare) + " lost the pregnancy.")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tellNearby(level, mare, Component.literal(nameOf(mare) + " lost one of the two foals she was "
                    + "carrying. The other is still growing.").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void foal(Horse mare, ServerLevel level, Reproduction r, Pregnancy p, long now) {
        Rng rng = HorseRecords.rng(mare);
        HorseRecord damRecord = HorseBreedingHandler.ensureParentRecord(mare);
        List<Embryo> embryos = p.embryos();
        int alive = p.twins() ? ReproRules.twinSurvivors(rng) : 1;
        List<UUID> born = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < alive && i < embryos.size(); i++) {
            Horse foal = EntityType.HORSE.create(level, EntitySpawnReason.BREEDING);
            if (foal == null) {
                continue;
            }
            foal.setAge(-24000); // newborn
            foal.snapTo(mare.getX(), mare.getY(), mare.getZ(), mare.getYRot(), 0.0F);
            HorseBreedingHandler.bornFromPregnancy(foal, mare, damRecord, embryos.get(i), rng);
            level.addFreshEntity(foal);
            born.add(foal.getUUID());
            names.add(HorseRecords.of(foal).displayName());
        }
        set(mare, r.foaled(now, born));
        lateSpeed(mare, false);
        level.broadcastEntityEvent(mare, (byte) 18); // hearts, like vanilla breeding

        String name = nameOf(mare);
        String text;
        if (!p.twins()) {
            text = names.isEmpty() ? name + " has foaled." : name + " has foaled: " + names.get(0) + ".";
        } else if (names.size() == 2) {
            text = name + " has foaled twins: " + names.get(0) + " and " + names.get(1) + ".";
        } else if (names.size() == 1) {
            text = name + " carried twins. " + names.get(0) + " was born alive; the other was not.";
        } else {
            text = name + " carried twins, and neither was born alive.";
        }
        tellNearby(level, mare, Component.literal(text).withStyle(ChatFormatting.GOLD));
        ActionTrace.log("fertility", ActionTrace.describeShort(mare) + " foaled " + names.size() + " of "
                + embryos.size() + " (due tick " + p.dueTick() + ", born tick " + now + ")");
    }

    /**
     * Nursing ends when every foal from the birth has grown up, or when none of
     * them has been within {@link ReproRules#NURSING_RADIUS} for a whole day. A
     * foal that is not loaded counts as away - it is not beside her.
     */
    private static void nurse(Horse mare, ServerLevel level, Reproduction r, long now, ReproTiming t) {
        boolean near = false;
        boolean stillAFoal = false;
        double radius2 = ReproRules.NURSING_RADIUS * ReproRules.NURSING_RADIUS;
        for (UUID id : r.nursing()) {
            Entity e = level.getEntity(id);
            if (e instanceof Horse foal && foal.isAlive()) {
                if (!foal.isBaby()) {
                    continue;   // grown up
                }
                stillAFoal = true;
                if (foal.distanceToSqr(mare) <= radius2) {
                    near = true;
                }
            } else {
                stillAFoal = true;  // elsewhere, unloaded, or dead: away
            }
        }
        if (!stillAFoal) {
            set(mare, r.weaned());
        } else if (near) {
            if (r.apartSince() != Reproduction.NEVER) {
                set(mare, r.withApartSince(Reproduction.NEVER));
            }
        } else if (r.apartSince() == Reproduction.NEVER) {
            set(mare, r.withApartSince(now));
        } else if (ReproRules.weanedByAbsence(r.apartSince(), now, t)) {
            set(mare, r.weaned());
        }
    }

    private static void lateSpeed(Horse horse, boolean late) {
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (late) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(LATE_PREGNANCY,
                    -ReproRules.LATE_SPEED_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (speed.hasModifier(LATE_PREGNANCY)) {
            speed.removeModifier(LATE_PREGNANCY);
        }
    }

    // ------------------------------------------------------------------
    // The testing clock
    // ------------------------------------------------------------------

    /**
     * <b>Right-click an adult mare with a clock</b> (debug tools): one step on.
     * Out of heat goes to the peak of a heat; a pregnancy becomes due now; just
     * foaled skips to foal heat. A mare already in heat is left alone - breed her.
     */
    public static void debugStep(Horse mare, Player player) {
        long now = mare.level().getGameTime();
        ReproTiming t = ServerConfig.reproTiming();
        Reproduction r = of(mare);
        String name = nameOf(mare);
        String said;
        switch (ReproRules.stateAt(r, now, t)) {
            case PREGNANT -> {
                Pregnancy p = r.pregnancy().get();
                long conceived = Math.min(p.conceivedTick(), now - 1);
                long loss = p.hasEarlyLoss() ? Math.min(p.lossTick(), now) : Pregnancy.NO_LOSS;
                set(mare, r.withPregnancy(new Pregnancy(p.embryos(), conceived, now, loss)));
                said = p.hasEarlyLoss()
                        ? name + " is due now - but she will lose what she is carrying first (a lethal embryo)."
                        : name + " is due now. She foals within two seconds.";
            }
            case POSTPARTUM -> {
                set(mare, r.withFoaledTick(now - t.postpartumTicks()));
                said = name + " skips ahead to foal heat.";
            }
            case ESTRUS -> {
                if (ReproRules.inPeak(r, now, t)) {
                    said = name + " is already at the peak of her heat - breed her.";
                } else {
                    set(mare, r.withCyclePhase(ReproRules.phaseFor(now, t.estrusTicks() * 3 / 4, t)));
                    said = name + " moves to the peak of her heat.";
                }
            }
            case FOAL_HEAT -> said = name + " is in foal heat - breed her.";
            default -> {
                set(mare, r.withCyclePhase(ReproRules.phaseFor(now, t.estrusTicks() * 3 / 4, t)));
                said = name + " comes into heat, at its peak.";
            }
        }
        player.sendSystemMessage(Component.literal("[clock] " + said).withStyle(ChatFormatting.GOLD));
    }

    // ------------------------------------------------------------------

    static String nameOf(Horse horse) {
        return HorseRecords.hasRealRecord(horse) ? HorseRecords.of(horse).displayName() : "The mare";
    }

    private static @Nullable Player ownerPlayer(Horse horse) {
        LivingEntity owner = horse.getOwner();
        return owner instanceof Player p ? p : null;
    }

    /** Everyone close enough to have seen it, and the owner wherever they are. */
    private static void tellNearby(ServerLevel level, Horse horse, Component line) {
        Player owner = ownerPlayer(horse);
        boolean toldOwner = false;
        for (Player p : level.getEntitiesOfClass(Player.class, horse.getBoundingBox().inflate(TELL_RADIUS))) {
            p.sendSystemMessage(line);
            toldOwner |= p == owner;
        }
        if (!toldOwner && owner != null) {
            owner.sendSystemMessage(line);
        }
    }
}
