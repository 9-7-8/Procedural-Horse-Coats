package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.GeneticCodeCombiner;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.ParentStats;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.name.HorseNames;
import com.example.horsegenetics.common.name.NamingPolicy;
import com.example.horsegenetics.neoforge.data.HorseNamingData;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.Embryo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Real breeding. <b>Plain golden carrots give a foal at once; a breeding
 * carrot on either parent gives a pregnancy instead</b> ({@link ReproHandler},
 * {@code common.repro}), and so do the seed jar and a stallion left with a mare
 * ({@link NaturalBreedingHandler}). Both kinds of foal are finished by the same {@link #populateFoal}. On {@link BabyEntitySpawnEvent} (fired from
 * {@code Animal#spawnChildFromBreeding} before the foal is added to the
 * world):
 *
 * <ul>
 *   <li>same-sex pairings are cancelled - breeding needs a mare and a stallion;</li>
 *   <li>the foal's <b>genome</b> is {@link GeneticCodeCombiner#combine}d from
 *       the parents - Mendelian alleles, each one carrying the priority and
 *       epigenetic seed of the exact parent copy it came from, so a foal that
 *       inherits its dam's {@code A} inherits her bay point heights too;</li>
 *   <li>its <b>name</b> keeps one half from a parent and rolls the other, to
 *       the dam's owner's own {@link NamingPolicy} - by default a filly carries
 *       her dam's last name and a colt his sire's ({@link HorseNames#foal});</li>
 *   <li>its generation is {@code 1 + max(dam, sire)};</li>
 *   <li>its <b>body</b> - speed, max health, jump strength and size - is
 *       resolved from the genome it just inherited
 *       ({@link HorseTraits#resolve}) and pushed onto its attributes. It is
 *       <b>not rolled</b>: the uniform draw off the parents' numbers that
 *       used to live here had no genetics in it at all, so two full siblings
 *       could differ by a factor of two and breeding for speed was breeding
 *       for luck;</li>
 *   <li>if the drawn genotype is an <b>embryonic lethal</b> the birth is
 *       cancelled outright; if it is <b>lethal at birth</b> the foal is still
 *       made, named and filed, and {@link LethalFoalHandler} does the rest;</li>
 *   <li>it is credited to the breeding player ({@code bredBy});</li>
 *   <li>if the dam is tamed, the foal is auto-tamed to the dam's owner.</li>
 * </ul>
 *
 * <p>The foal-finishing step is {@link #populateFoal}, shared by the instant
 * foal ({@link #applyBredFoal}) and birth from a pregnancy
 * ({@link #bornFromPregnancy}). The foal's record is written there rather than
 * at join time - the inherited epigenetics can only be read while both genomes
 * are in hand. {@link HorseGeneticsEventHandler} founds one from scratch for
 * everything that arrives without one (wild spawns, {@code /summon}, gallery
 * horses).
 */
@EventBusSubscriber
public final class HorseBreedingHandler {

    @SubscribeEvent
    static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Horse parentA)) return;
        if (!(event.getParentB() instanceof Horse parentB)) return;
        if (!(event.getChild() instanceof Horse child)) return;

        Rng rng = HorseRecords.rng(child);

        HorseRecord recordA = ensureParentRecord(parentA);
        HorseRecord recordB = ensureParentRecord(parentB);

        if (recordA.sex() == recordB.sex()) {
            sameSexAttempt(recordA.sex(), damOrSireName(recordA), damOrSireName(recordB),
                    event.getCausedByPlayer(), child);
            event.setCanceled(true);
            return;
        }

        boolean aIsDam = recordA.sex() == Sex.FEMALE;
        HorseRecord damRecord = aIsDam ? recordA : recordB;
        HorseRecord sireRecord = aIsDam ? recordB : recordA;
        Horse damHorse = aIsDam ? parentA : parentB;
        Horse sireHorse = aIsDam ? parentB : parentA;

        // A GELDING SIRES NOTHING. Vanilla love does not know it, so a mare and a
        // gelding fed golden carrots court like any pair and arrive here.
        if (sireRecord.gelded()) {
            ReproHandler.overlay(event.getCausedByPlayer(), sireRecord.displayName() + " is a gelding - no foal.",
                    ChatFormatting.YELLOW);
            event.setCanceled(true);
            return;
        }

        Genome damGenome = genomeOf(damHorse, damRecord, rng);
        Genome sireGenome = genomeOf(sireHorse, sireRecord, rng);
        Player breeder = event.getCausedByPlayer();

        // A mare already carrying is not bred again. Breeding foods are refused
        // before this (BreedingCooldownHandler); this catches love from anywhere else.
        if (ReproHandler.of(damHorse).pregnant()) {
            event.setCanceled(true);
            return;
        }

        // A CARROT-ARMED PARENT MAKES THIS A PREGNANCY, not a foal (owner,
        // 2026-09-13) - either parent. A mare out of heat simply does not take,
        // and the carrots stay armed for next time. Cancelling is what vanilla
        // expects: Animal.spawnChildFromBreeding resets both parents' love and
        // sets their 6000-tick cooldown on a cancelled BabyEntitySpawnEvent
        // (checked in the patched 26.1.2 source).
        if (ReproHandler.armed(damHorse) || ReproHandler.armed(sireHorse)) {
            Conception.Result result = ReproHandler.breed(damHorse, damRecord, damGenome,
                    sireGenome, sireRecord, sireHorse, java.util.List.of(), breeder);
            ReproHandler.announce(damHorse, breeder, result);
            event.setCanceled(true);
            return;
        }

        // PLAIN GOLDEN CARROTS STAY INSTANT. The one thing fertility may do here
        // is the subfertile allele, and it is only rolled when it could matter,
        // so a pair nobody bred for it draws exactly the foal it always did.
        double foalChance = Conception.vanillaFoalChance(damGenome.genotype(), sireGenome.genotype());
        if (foalChance < 1.0 && rng.nextFloat() >= foalChance) {
            ReproHandler.overlay(breeder, "It didn't take - no foal this time.", ChatFormatting.YELLOW);
            ActionTrace.log("fertility", ActionTrace.describeShort(damHorse) + " x "
                    + ActionTrace.describeShort(sireHorse) + ": a subfertile pairing missed (chance "
                    + foalChance + ")");
            event.setCanceled(true);
            return;
        }

        boolean born = applyBredFoal(child, damHorse,
                damGenome, damRecord, sireGenome, sireRecord, breeder, rng);
        if (!born) {
            // the drawn genotype was an embryonic lethal - the embryo never
            // implants, so there is no foal to add to the world
            event.setCanceled(true);
        }
    }

    /**
     * Populate a just-created foal entity from its two parents' genomes and
     * records: combined genome, generation, varied name, {@code bredBy},
     * dam-owner taming, the {@link HorseRecord}, and the attributes resolved
     * from the genome. Everything except adding the child to the world.
     *
     * <p>Plain golden-carrot breeding only. Every other path makes a pregnancy
     * and finishes its foal through {@link #bornFromPregnancy}.
     *
     * @param child    the foal entity vanilla has just made
     * @param damHorse the live dam - only its tame state / owner is read here
     * @param breeder  the player who caused the breeding, or {@code null}
     * @return {@code false} if the drawn genotype is an embryonic lethal, in
     *         which case nothing was written to {@code child} and the caller
     *         must not add it to the world
     */
    static boolean applyBredFoal(Horse child, Horse damHorse,
                                 Genome damGenome, HorseRecord damRecord,
                                 Genome sireGenome, HorseRecord sireRecord,
                                 @Nullable Player breeder, Rng rng) {
        // No carrot bias on this path: an armed parent never reaches it.
        Genome childGenome = GeneticCodeCombiner.combine(damGenome, sireGenome, rng,
                GameteBias.NONE, GameteBias.NONE);

        // The foal's breed label: same-breed -> that breed, two breeds -> a
        // "A x B cross", cross-of-the-same-pair stays that cross, anything
        // messier -> "Mixed" (see BreedLineage.combine).
        BreedLineage childLineage = BreedLineage.combine(damRecord.lineage(), sireRecord.lineage());

        // The draw happens first and is never conditioned on viability - it is
        // the ordinary Mendelian one, and this only reads its result. That is
        // what keeps the odds honest (one in four for two carriers) and keeps
        // Genotype.breedWith free of any notion of a lethal. The foal's breed is
        // not consulted: a breed shapes its founders and nothing after them, so
        // a cross is whatever it inherited (see BreedStatTargets).
        Traits childTraits = HorseTraits.resolve(childGenome.genotype(),
                childGenome.epigenome(), ServerConfig.healthGeneticsActive());

        // The dev build's account of the draw, written before the viability
        // check so a pairing that comes to nothing still leaves a record of
        // exactly what it drew - which is the case where the log is the only
        // evidence there is.
        BreedingDebug.reportDraw(damRecord, sireRecord, damGenome, sireGenome, childGenome,
                childTraits, breeder);

        // A genotype a gene rules out is an embryonic lethal too (gap 225), with a
        // synthetic cause so the miscarriage line still has something to say.
        java.util.Optional<Condition> nonviable = com.example.horsegenetics.common.genetics.Conceivable
                .failure(childGenome.genotype());
        if ((childTraits.viability() == Viability.LETHAL_AT_CONCEPTION || nonviable.isPresent())
                && ServerConfig.lethalsActive()) {
            Condition cause = childTraits.lethalCondition().or(() -> nonviable).orElse(null);
            if (cause != null) {
                LethalFoalHandler.announceMiscarriage(damHorse, breeder, cause);
            }
            return false;
        }

        populateFoal(child, damHorse, damRecord, childGenome, childLineage.toToken(),
                sireRecord.id(), sireRecord.firstName(), sireRecord.lastName(), sireRecord.generation(),
                HorseRecords.traitsOf(sireRecord), childTraits,
                breeder == null ? "" : breeder.getGameProfile().name(), breeder, rng);
        // Magic out of nowhere: an allele neither parent had to give.
        if (breeder != null && com.example.horsegenetics.common.genetics.Mutation.happened(
                childGenome.genotype(), damGenome.genotype(), sireGenome.genotype())) {
            HorseProgress.complete(breeder, ProgressTask.BREED_MUTATION);
        }
        return true;
    }

    /**
     * <b>Birth at the end of a pregnancy</b> ({@code ReproHandler}). The genome,
     * breed label and viability were all settled at conception and are in the
     * {@link Embryo}; the sire need not be loaded, owned, or alive. The foal is
     * tamed to whoever owns the mare <i>now</i>, and credited to whoever arranged
     * the mating then (owner, 2026-09-13).
     */
    static void bornFromPregnancy(Horse child, Horse damHorse, HorseRecord damRecord, Embryo embryo, Rng rng) {
        boolean health = ServerConfig.healthGeneticsActive();
        Genome childGenome = embryo.foal();
        Genome sire = embryo.sire().genome();
        var server = damHorse.level().getServer();
        Player breeder = embryo.bredBy().isEmpty() || server == null
                ? null : server.getPlayerList().getPlayerByName(embryo.bredBy());
        populateFoal(child, damHorse, damRecord, childGenome, embryo.breedToken(), embryo.sireId(),
                embryo.sireFirstName(), embryo.sireLastName(), embryo.sireGeneration(),
                HorseTraits.resolve(sire.genotype(), sire.epigenome(), health),
                HorseTraits.resolve(childGenome.genotype(), childGenome.epigenome(), health),
                embryo.bredBy(), breeder, rng);
        // The same roll reaches a foal born from a pregnancy, so the same tick does.
        if (breeder != null && damRecord.hasGenome()
                && com.example.horsegenetics.common.genetics.Mutation.happened(
                        childGenome.genotype(), damRecord.genome().genotype(), sire.genotype())) {
            HorseProgress.complete(breeder, ProgressTask.BREED_MUTATION);
        }
    }

    /**
     * Everything about a foal that is not its genome: name, record, taming, body,
     * band and age. Shared by instant golden-carrot breeding and birth from a
     * pregnancy.
     *
     * @param bredBy  the breeder's name, or {@code ""}
     * @param breeder that player if they are online, for the checklist and gene
     *                discovery; {@code null} otherwise
     */
    private static void populateFoal(Horse child, Horse damHorse, HorseRecord damRecord, Genome childGenome,
                                     String breedToken, UUID sireId, String sireFirstName, String sireLastName,
                                     int sireGeneration, Traits sireTraits, Traits childTraits,
                                     String bredBy, @Nullable Player breeder, Rng rng) {
        tickBreedingTasks(breeder, childGenome.genotype());

        int childGeneration = 1 + Math.max(damRecord.generation(), sireGeneration);
        // Whose horses these are decides how the foal is named; a wild birth has
        // nobody and gets the default. The foal's sex comes out of the same
        // Mendelian draw as everything else, so the surname line is genetic too.
        NameParts childName = HorseNames.foal(
                new NameParts(damRecord.firstName(), damRecord.lastName()),
                new NameParts(sireFirstName, sireLastName),
                childGenome.sex(),
                namingPolicyFor(damHorse, breeder),
                HorseRecords.names(), rng);

        // What the parents' bodies were, so the UI can say whether this foal came
        // out above both of them, between, or below.
        ParentStats parentStats = ParentStats.of(HorseRecords.traitsOf(damRecord), sireTraits);

        // No sex argument: the foal's sex came out of the Mendelian draw, like
        // every other locus, and HorseRecord reads it back off the code.
        HorseRecord childRecord = HorseRecord.bred(
                child.getUUID(),
                childName.first(),
                childName.last(),
                childGenome,
                breedToken,
                damRecord.id(),
                sireId,
                childGeneration)
                .withParentStats(parentStats);

        if (!bredBy.isEmpty()) {
            childRecord = childRecord.withBredBy(bredBy);
        }

        // Foals born to a tamed dam are tamed to the dam's owner.
        if (damHorse.isTamed()) {
            child.setTamed(true);
            LivingEntity owner = damHorse.getOwner();
            if (owner != null) {
                child.setOwner(owner);
            }
            if (owner instanceof Player ownerPlayer) {
                childRecord = childRecord.withTamedBy(ownerPlayer.getGameProfile().name());
            }
            inheritBond(damHorse, child);
        }

        HorseRecords.apply(child, childRecord);
        HorseRecords.applyTraitsToEntity(child, childTraits, true);

        // A row in the dam's owner's Log tab. After apply(), so the foal has the
        // name the row will carry; silent for a wild birth, which has no owner.
        // Both breeding paths come through here, so a foal born overnight from a
        // pregnancy is logged exactly like one fed a golden carrot.
        HorseLog.born(child, damHorse, damRecord.displayName(),
                (sireFirstName + " " + sireLastName).trim());

        // Born into its dam's band, if she has one - which is how a wild band
        // grows - and with an age, a dam and a day to leave it, for the herd system.
        HorseCareAttachment damCare = damHorse.getData(ModAttachments.HORSE_CARE.get());
        boolean wildDam = !damHorse.isTamed() && damCare.inWildHerd();
        if (wildDam) {
            child.setData(ModAttachments.HORSE_CARE.get(), child.getData(ModAttachments.HORSE_CARE.get())
                    .withWildHerd(damCare.herd().orElseThrow(), damCare.herdBreed().orElse(""),
                            damCare.herdBand().orElse("TRADITIONAL")));
        }
        child.setData(ModAttachments.HORSE_SOCIAL.get(),
                com.example.horsegenetics.neoforge.data.HorseSocialAttachment.DEFAULT.withBirth(
                        child.level().getGameTime(), java.util.Optional.of(damRecord.id()),
                        wildDam ? damCare.herd() : java.util.Optional.empty(),
                        com.example.horsegenetics.common.herd.HerdRules.disperseAfterDays(childRecord.sex(), rng)));

        // Breeding a foal with a gene discovers that gene for the breeder.
        if (breeder != null) {
            GeneDiscoveryHandler.discoverFrom(breeder, childGenome.genotype());
        }

        if (childTraits.viability() == Viability.LETHAL_AT_BIRTH) {
            LethalFoalHandler.announceLethalBirth(child, childRecord, childTraits, breeder);
        }
    }

    /**
     * Whose naming policy this foal is named under.
     *
     * <p><b>The dam's owner, not the breeder</b>, and the two are not always the
     * same player: a foal is born where its dam is, to whoever owns her, and
     * that is whose herd the name has to sit in. The breeder is the fallback for
     * the case where the dam has no owner but a player made the pairing happen
     * anyway - a tamed stallion put to a wild mare. A wild birth has neither and
     * gets {@link NamingPolicy#DEFAULT}.
     *
     * <p>The dam's owner is looked up by UUID rather than as an entity, because
     * a pregnancy comes due on its own schedule and its owner is frequently
     * offline when it does - which is the whole reason the policy lives in a
     * SavedData and not on the player.
     */
    private static NamingPolicy namingPolicyFor(Horse damHorse, @Nullable Player breeder) {
        var server = damHorse.level().getServer();
        if (server == null) {
            return NamingPolicy.DEFAULT;
        }
        UUID owner = HorseOwnership.ownerId(damHorse);
        if (owner == null && breeder != null) {
            owner = breeder.getUUID();
        }
        return HorseNamingData.get(server).policyFor(owner);
    }


    /**
     * The parent's full genome, straight off its record. A parent whose record
     * predates the epigenome field founds one now, so the foal still inherits
     * real allele copies rather than nothing.
     */
    static Genome genomeOf(Horse parent, HorseRecord record, Rng rng) {
        if (record.hasGenome()) {
            return record.genome();
        }
        Genome genome = Genome.of(record.genotype(), rng);
        HorseRecords.apply(parent, record.withGenome(genome));
        return genome;
    }

    /**
     * Return the parent's record, founding one if this horse somehow arrived
     * without one. There is nothing to backfill any more - a record's stats
     * are derived from the genetic code it already carries.
     */
    /**
     * <b>Two mares, or two stallions, and a player who wants to know why nothing
     * happened.</b> Vanilla does not know horses have a sex, so a same-sex pair
     * fed breeding carrots goes through the whole courtship and reaches this
     * event like any other pair - and the foal is then cancelled here. That is
     * correct, and until now it was also <i>silent</i>: the carrots were spent,
     * the hearts appeared, and nothing came of it with no explanation anywhere.
     *
     * <p>So the attempt is left exactly as it is - they still try - and it now
     * says so. The line is deliberately light rather than an error: nothing has
     * gone wrong, and a red warning for two horses being the same sex would read
     * as a bug in the mod rather than a fact about horses.
     *
     * <p>Only the player who caused it is told. A pair courting in the corner of
     * somebody else's paddock is not everyone's business, and
     * {@code BabyEntitySpawnEvent} hands us exactly the player who fed them.
     */
    /**
     * <b>A foal starts out already part-way fond of you</b> - a quarter of what
     * its dam feels.
     *
     * <p>Starting every foal at zero made a horse born in your own stable, to
     * your own mare, exactly as wary of you as one caught wild that morning,
     * which is backwards: the animal that has never known anyone else should
     * not be the harder one to win over. A quarter rather than all of it
     * because the bond is still the foal's own to earn - this is the head start
     * of having been born to somebody who trusts you, not an inheritance.
     *
     * <p>Written straight onto the attachment rather than through
     * {@code awardBond}, deliberately: that path exists to meter out bond
     * against a daily cap, and a birthright is not a day's worth of care. It
     * would also swallow most of the grant, since the cap is
     * {@link HorseCareAttachment#DAILY_CAP} and a well-bonded dam gives more
     * than that.
     */
    private static void inheritBond(Horse dam, Horse foal) {
        HorseCareAttachment damCare = dam.getData(ModAttachments.HORSE_CARE.get());
        int inherited = damCare.bond() / 4;
        if (inherited <= 0) {
            return;
        }
        HorseCareAttachment foalCare = foal.getData(ModAttachments.HORSE_CARE.get());
        foal.setData(ModAttachments.HORSE_CARE.get(), foalCare.with(
                inherited, foalCare.herd(), foalCare.bondToday(), foalCare.dayStamp(),
                foalCare.bondTicks(), foalCare.togetherTicks()));
    }

    private static void sameSexAttempt(Sex sex, String nameA, String nameB,
                                       Player player, Horse child) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return; // nobody fed them, or we are on the client - nothing to say
        }
        String key = sex == Sex.FEMALE
                ? "message.horsegenetics.breed.same_sex.mares"
                : "message.horsegenetics.breed.same_sex.stallions";
        // One of three, so the joke does not wear out on the second telling.
        // Seeded off the child entity, which is discarded a moment later and is
        // as good a coin as any - and it keeps the choice off the main RNG.
        int variant = Math.floorMod(child.getId(), 3) + 1;
        serverPlayer.sendSystemMessage(Component.translatable(key + "." + variant,
                Component.literal(nameA), Component.literal(nameB)));
    }

    /** A parent's name for a chat line - the barn name if it has one. */
    private static String damOrSireName(HorseRecord record) {
        String name = record.displayName();
        return name == null || name.isBlank() ? "This horse" : name;
    }

    /**
     * <b>What this foal is, as three checklist boxes.</b> Every locus is one of
     * three things - two of the same non-baseline allele, two of the baseline,
     * or one of each - and a player learning the model needs to have <i>seen</i>
     * each before the words mean anything.
     *
     * <p>"Homozygous dominant" is read as two copies of the same non-baseline
     * allele, and "homozygous recessive" as two of the gene's own default. That
     * is the distinction a player can actually observe: the first is the one
     * that breeds true and shows, the second is the one that hides for a
     * generation and then appears. Dominance in the model is per-gene and richer
     * than two words, and the hint on each task says what it really means.
     */
    private static void tickBreedingTasks(@Nullable Player breeder, Genotype foal) {
        if (breeder == null) {
            return;
        }
        HorseProgress.complete(breeder, ProgressTask.BREED_FOAL);
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = foal.pair(gene);
            boolean same = pair.first().token().equals(pair.second().token());
            boolean baseline = pair.homozygousFor(gene.defaultAllele());
            if (!same) {
                HorseProgress.complete(breeder, ProgressTask.FOAL_HETEROZYGOUS);
            } else if (baseline) {
                HorseProgress.complete(breeder, ProgressTask.FOAL_HOM_RECESSIVE);
            } else {
                HorseProgress.complete(breeder, ProgressTask.FOAL_HOM_DOMINANT);
            }
        }
    }

    static HorseRecord ensureParentRecord(Horse parent) {
        if (!HorseRecords.hasRealRecord(parent)) {
            HorseRecord founder = HorseRecords.newFounder(parent, HorseRecords.rng(parent));
            HorseRecords.apply(parent, founder);
            return founder;
        }
        return HorseRecords.of(parent);
    }

    private HorseBreedingHandler() {
    }
}
