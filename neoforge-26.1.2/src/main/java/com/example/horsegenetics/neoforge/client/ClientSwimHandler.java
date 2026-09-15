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

    /** Swim factor per genetic code plus epigenome (the magnitude lives on the allele copy); 1.0 means no swim gene. */
    private static final Map<String, Double> FACTOR_BY_CODE = new HashMap<>();

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SwimScaling.clientFactor = ClientSwimHandler::factorOf;
    }

    private static double factorOf(Horse horse) {
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null || !record.hasGenome()) {
            return 1.0;
        }
        if (FACTOR_BY_CODE.size() > 512) {
            FACTOR_BY_CODE.clear();
        }
        return FACTOR_BY_CODE.computeIfAbsent(record.geneticCode() + "|" + record.epigenomeCode(), k -> swimFactor(record));
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
