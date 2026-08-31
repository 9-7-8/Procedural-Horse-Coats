package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
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

    // Default value is a placeholder only - HorseGeneticsEventHandler assigns a real,
    // freshly-rolled genotype the instant a horse without one is spawned/ticked, so no
    // horse should ever be seen or saved with this default in practice.
    public static final Supplier<AttachmentType<HorseCoatAttachment>> HORSE_COAT =
            ATTACHMENT_TYPES.register("horse_coat", () -> AttachmentType
                    .builder(() -> new HorseCoatAttachment("eeaa", CoatPhenotype.CHESTNUT, 0f))
                    .serialize(HorseCoatAttachment.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    // The Layer-1 HorseRecord itself is the attachment value. The default is a
    // sentinel (blank name) keyed to the holder entity's own UUID; the spawn /
    // breeding handlers replace it with a real record immediately. Serialization
    // is HorseRecordCodecs.MAP_CODEC (kept out of the domain type on purpose).
    public static final Supplier<AttachmentType<HorseRecord>> HORSE_RECORD =
            ATTACHMENT_TYPES.register("horse_record", () -> AttachmentType
                    .<HorseRecord>builder(holder -> HorseRecord.founder(
                            holder instanceof Entity entity ? entity.getUUID() : new UUID(0L, 0L),
                            Sex.FEMALE, "", "", "eeaa"))
                    .serialize(HorseRecordCodecs.MAP_CODEC)
                    .build());

    private ModAttachments() {
    }
}
