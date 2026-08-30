package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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
                    .serialize(HorseCoatAttachment.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
