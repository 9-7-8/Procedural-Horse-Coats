package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.server.SwimScaling;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Magic swim speed for the horse you are riding.</b> A ridden horse is moved by its rider's client, not by the
 * server, so the server's {@code GeneAbilityHandler} can only scale loose horses. This applies the same
 * {@link SwimScaling} on the client, to the one horse this client moves: the local player's mount. The genes come from
 * {@link ClientHorseRecordCache}, and the factor from the same {@link HorseAbilities} the server resolves.
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
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.level().isClientSide() || !horse.canSimulateMovement()) {
            return;
        }
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record == null || !record.hasGenome()) {
            return;
        }
        if (FACTOR_BY_CODE.size() > 512) {
            FACTOR_BY_CODE.clear();
        }
        double factor = FACTOR_BY_CODE.computeIfAbsent(record.geneticCode() + "|" + record.epigenomeCode(),
                k -> swimFactor(record));
        SwimScaling.apply(horse, factor);
    }

    private static double swimFactor(HorseRecord record) {
        try {
            for (HorseAbilities.Active active : HorseAbilities.activeFor(record.genotype(), record.epigenome())) {
                if (active.ability() instanceof GeneAbility.Swim swim) {
                    return swim.factor();
                }
            }
        } catch (RuntimeException badCode) {
            // a record the client could not parse swims like any horse
        }
        return 1.0;
    }
}
