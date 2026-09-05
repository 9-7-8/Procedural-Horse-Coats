package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Debug dump: right-click a horse with a piece of paper to print its
 * {@link HorseRecord} plus its <b>herd</b> to chat. Pure translation - it
 * formats domain data; nothing is computed here.
 */
@EventBusSubscriber
public final class HorsePaperInspectHandler {

    private static final double HERD_COUNT_RADIUS = 64.0;

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getItemStack().is(Items.PAPER)) return;
        if (!(event.getTarget() instanceof Horse horse)) return;

        Player player = event.getEntity();
        player.sendSystemMessage(describe(player, horse));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static Component describe(Player player, Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(record.hasName() ? record.displayName() : "(unnamed horse)").append(" ===");
        sb.append("\n id: ").append(record.id());
        sb.append("\n registered name: ").append(record.firstName()).append(" ").append(record.lastName());
        record.barnName().ifPresent(b -> sb.append("\n barn name: ").append(b));
        sb.append("\n ").append(record.sex().label(!horse.isBaby()).toLowerCase());
        sb.append("\n breed: ").append(record.lineage().displayName());
        appendHerd(sb, player, horse);
        sb.append("\n genetic code: ").append(GeneCodeDisplay.shortForm(record.geneticCode()));
        // Resolved from the genotype, honouring the server's health.mode - so a
        // world with the disorders switched off prints the horse it actually has.
        Traits traits = HorseRecords.traitsOf(record);
        sb.append("\n speed: ").append(String.format("%.3f", traits.speed()));
        sb.append("\n health: ").append(String.format("%.1f", traits.health()));
        sb.append("\n jump: ").append(String.format("%.2f", traits.jump()));
        sb.append("\n size: ").append(String.format("%.2f", traits.scale()));
        record.parentStats().ifPresent(ps -> sb
                .append("\n   vs parents: speed ").append(rankWord(ps.rankSpeed(traits.speed())))
                .append(", health ").append(rankWord(ps.rankHealth(traits.health()))));
        if (traits.hasConditions()) {
            sb.append("\n conditions:");
            for (Condition c : traits.conditions()) {
                sb.append("\n   ").append(c.name()).append(" - ").append(c.description());
            }
        }
        if (traits.viability() == Viability.LETHAL_AT_BIRTH) {
            sb.append("\n *** this foal will not survive ***");
        }
        sb.append("\n bred by: ").append(record.bredBy().orElse("(wild)"));
        sb.append("\n tamed by: ").append(record.tamedBy().orElse("(untamed)"));
        sb.append("\n sire: ").append(parentLabel(player, record.fatherId()));
        sb.append("\n dam: ").append(parentLabel(player, record.motherId()));
        return Component.literal(sb.toString());
    }

    /** The natural-herd line: who leads it, what band it is, how big it is. */
    private static void appendHerd(StringBuilder sb, Player player, Horse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (!care.inWildHerd()) {
            sb.append("\n herd: none (solo)");
            return;
        }
        UUID leadId = care.herd().orElseThrow();
        String band = care.herdBand().map(String::toLowerCase).orElse("wild");
        boolean isLead = leadId.equals(horse.getUUID());
        int members = horseCountInHerd(player, horse, leadId);

        sb.append("\n herd: ").append(band).append(" band");
        if (isLead) {
            sb.append(" (this horse is the lead)");
        } else {
            sb.append(" led by ").append(leadName(player, leadId));
        }
        sb.append(", ").append(members).append(members == 1 ? " member nearby" : " members nearby");
    }

    private static int horseCountInHerd(Player player, Horse self, UUID leadId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 1;
        }
        return level.getEntitiesOfClass(Horse.class, self.getBoundingBox().inflate(HERD_COUNT_RADIUS),
                h -> {
                    HorseCareAttachment c = h.getData(ModAttachments.HORSE_CARE.get());
                    return c.inWildHerd() && c.herd().map(leadId::equals).orElse(false);
                }).size();
    }

    private static String leadName(Player player, UUID leadId) {
        if (player.level() instanceof ServerLevel level) {
            Entity e = level.getEntity(leadId);
            if (e instanceof Horse h && HorseRecords.hasRealRecord(h)) {
                return HorseRecords.of(h).displayName();
            }
            return HorseAncestryData.get(level.getServer()).lookup(leadId)
                    .map(HorseRecord::displayName)
                    .orElse("an unloaded horse");
        }
        return leadId.toString();
    }

    private static String rankWord(int rank) {
        return rank > 0 ? "above both" : rank < 0 ? "below both" : "between";
    }

    private static String parentLabel(Player player, Optional<UUID> parentId) {
        if (parentId.isEmpty()) {
            return "unknown";
        }
        UUID id = parentId.get();
        if (player.level() instanceof ServerLevel level) {
            return HorseAncestryData.get(level.getServer()).lookup(id)
                    .map(HorseRecord::displayName)
                    .orElseGet(id::toString);
        }
        return id.toString();
    }

    private HorsePaperInspectHandler() {
    }
}
