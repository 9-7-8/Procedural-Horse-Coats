package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.server.SwimScaling;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Magic swim speed for the horse you are riding.</b> A ridden horse is moved by its rider's client, so
 * {@code HorseSwimMixin} runs there for it and asks {@link SwimScaling#factorOf}, which on the client side calls the
 * lookup this installs. The genes come from {@link ClientHorseRecordCache}, and the factor from the same
 * {@link HorseAbilities} the server resolves.
 *
 * <p>Magic swim speed's ability is unconditional ({@code Condition.ALWAYS}), so no condition is evaluated here. A gene
 * that ever gates {@code Swim} on a condition will need this to learn it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientSwimHandler {

    private ClientSwimHandler() {
    }

    /**
     * Swim factor per horse, valid while the cache still hands back the same {@link HorseRecord} instance; 1.0 means
     * no swim gene. Keyed this way rather than on {@code geneticCode + "|" + epigenomeCode} (#203): that key was a
     * ~20KB string built on every call, and this is called every tick from the mixin. A record is immutable and
     * {@link ClientHorseRecordCache} replaces it whole on any change, so instance identity is a sound "unchanged" test.
     */
    private static final Map<Integer, Cached> BY_ENTITY = new HashMap<>();

    private record Cached(HorseRecord source, double factor) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SwimScaling.clientFactor = ClientSwimHandler::factorOf;
    }

    private static double factorOf(Horse horse) {
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null || !record.hasGenome()) {
            return 1.0;
        }
        Cached cached = BY_ENTITY.get(horse.getId());
        if (cached != null && cached.source() == record) {
            return cached.factor();
        }
        if (BY_ENTITY.size() > 512) {
            BY_ENTITY.clear();
        }
        double factor = swimFactor(record);
        BY_ENTITY.put(horse.getId(), new Cached(record, factor));
        return factor;
    }

    private static double swimFactor(HorseRecord record) {
        try {
            for (HorseAbilities.Active active : HorseAbilities.activeFor(record.genotype(), record.epigenome())) {
                if (active.ability() instanceof GeneAbility.Swim swim) {
                    return Math.max(0.05, swim.factor());
                }
            }
        } catch (RuntimeException badCode) {
            // a record the client could not parse swims like any horse
        }
        return 1.0;
    }
}
