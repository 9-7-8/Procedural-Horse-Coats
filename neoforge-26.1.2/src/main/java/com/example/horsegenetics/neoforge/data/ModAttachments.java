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

    private ModAttachments() {
    }
}
