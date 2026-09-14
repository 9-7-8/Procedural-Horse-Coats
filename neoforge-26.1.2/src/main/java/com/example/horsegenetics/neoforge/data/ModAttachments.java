package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HorseGenetics.MOD_ID);

    // The Layer-1 HorseRecord itself is the attachment value, and it carries the
    // whole genome - genotype *and* epigenome. There is no separate coat
    // attachment: two attachments both holding the genotype was one fact stored
    // twice, and the epigenome living only on the entity is why the family tree
    // used to draw ancestors from an invented seed rather than their real coat.
    //
    // The default is a sentinel (blank name, blank epigenome) keyed to the holder
    // entity's own UUID; the spawn / breeding handlers replace it with a real
    // record immediately, and HorseRecord.hasGenome() is the "not yet" test.
    // Serialization is HorseRecordCodecs.MAP_CODEC (kept out of the domain type
    // on purpose).
    public static final Supplier<AttachmentType<HorseRecord>> HORSE_RECORD =
            ATTACHMENT_TYPES.register("horse_record", () -> AttachmentType
                    .<HorseRecord>builder(holder -> HorseRecord.unassigned(
                            holder instanceof Entity entity ? entity.getUUID() : new UUID(0L, 0L)))
                    .serialize(HorseRecordCodecs.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Care + social state. Roadmap wiki: 7.2 gated healing, 13 bond and herds.
    // Non-genetic; default is DEFAULT (bond 0, no herd). HorseCareHandler mutates it.
    // copyOnDeath so a re-summoned horse keeps its bond.
    public static final Supplier<AttachmentType<HorseCareAttachment>> HORSE_CARE =
            ATTACHMENT_TYPES.register("horse_care", () -> AttachmentType
                    .builder(() -> HorseCareAttachment.DEFAULT)
                    .serialize(HorseCareAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Social life: age, dam, natal band and the relationship ledger (common.herd).
    // Non-genetic. copyOnDeath like HORSE_CARE, so a re-summoned horse keeps its friends.
    public static final Supplier<AttachmentType<HorseSocialAttachment>> HORSE_SOCIAL =
            ATTACHMENT_TYPES.register("horse_social", () -> AttachmentType
                    .builder(() -> HorseSocialAttachment.DEFAULT)
                    .serialize(HorseSocialAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Fertility and gestation (common.repro): a mare's cycle phase, what she is
    // carrying, her last birth and the foals she nurses; a stallion's covers today.
    // Keyed to the entity id so an unset mare still has a cycle. NOT copyOnDeath:
    // a pregnancy dies with the mare. Not synced - the info screen gets a line of
    // words through the social summary, and no client needs an embryo's genome.
    public static final Supplier<AttachmentType<com.example.horsegenetics.common.repro.Reproduction>> HORSE_REPRO =
            ATTACHMENT_TYPES.register("horse_repro", () -> AttachmentType
                    .<com.example.horsegenetics.common.repro.Reproduction>builder(holder ->
                            com.example.horsegenetics.common.repro.Reproduction.fresh(
                                    holder instanceof Entity entity ? entity.getUUID() : new UUID(0L, 0L)))
                    .serialize(ReproCodecs.MAP_CODEC)
                    .build());

    // Timed-interaction stamps (last shear, last per-gene yield, ...). Gated
    // "once per Minecraft day". Replaces the static cooldown map that used to
    // live in GeneYieldHandler. copyOnDeath so a re-summoned horse keeps them.
    public static final Supplier<AttachmentType<HorseCooldownsAttachment>> HORSE_COOLDOWNS =
            ATTACHMENT_TYPES.register("horse_cooldowns", () -> AttachmentType
                    .builder(() -> HorseCooldownsAttachment.DEFAULT)
                    .serialize(HorseCooldownsAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // Breeding-carrot effects waiting on this horse until its next conception
    // (data/ArmedCarrotsAttachment). They do not expire. NOT copyOnDeath.
    public static final Supplier<AttachmentType<ArmedCarrotsAttachment>> ARMED_CARROTS =
            ATTACHMENT_TYPES.register("armed_carrots", () -> AttachmentType
                    .builder(() -> ArmedCarrotsAttachment.EMPTY)
                    .serialize(ArmedCarrotsAttachment.MAP_CODEC)
                    .build());

    // Whose horse this is while it is nobody's: the cowboy who bred it and has
    // not sold it yet (data/CowboyBrand). Read by CowboyHerdGoal to find the man
    // to follow and by TransferPaperHandler to refuse free taming. Synced,
    // because refusing an interaction only on the server makes the client
    // predict a mount it then has to take back - see CowboyBrand.STREAM_CODEC.
    // NOT copyOnDeath - a re-summoned horse is not still their.
    public static final Supplier<AttachmentType<CowboyBrand>> COWBOY_BRAND =
            ATTACHMENT_TYPES.register("cowboy_brand", () -> AttachmentType
                    .builder(() -> CowboyBrand.NONE)
                    .serialize(CowboyBrand.MAP_CODEC)
                    .sync(CowboyBrand.STREAM_CODEC)
                    .build());

    // The horse a shifted lycanthrope used to be, held on the ANIMAL rather than
    // on a horse - at night there is no horse to hold it. See data/LycanShift.
    // Not copyOnDeath: an animal that dies takes the horse inside it with it.
    public static final Supplier<AttachmentType<LycanShift>> LYCAN_SHIFT =
            ATTACHMENT_TYPES.register("lycan_shift", () -> AttachmentType
                    .builder(() -> LycanShift.NONE)
                    .serialize(LycanShift.MAP_CODEC)
                    .build());

    private ModAttachments() {
    }
}
