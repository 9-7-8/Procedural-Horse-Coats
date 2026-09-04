package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Debug dump: right-click a horse with a piece of paper to print its
 * {@link HorseRecord} (and a few generations of ancestors) to chat. Pure
 * translation - it formats the domain record's fields; all the data comes
 * from Layer 1 via the attachment / SavedData.
 */
@EventBusSubscriber
public final class HorsePaperInspectHandler {

    private static final int ANCESTOR_DEPTH = 3;

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
        sb.append("\n generation: ").append(record.generation());
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

        if (player.level() instanceof ServerLevel level) {
            List<HorseRecord> ancestors = HorseAncestryData.get(level.getServer())
                    .ancestorsOf(record.id(), ANCESTOR_DEPTH);
            sb.append("\n ancestors (").append(ANCESTOR_DEPTH).append(" gen): ");
            sb.append(ancestors.isEmpty()
                    ? "none recorded"
                    : ancestors.stream().map(HorseRecord::displayName).collect(Collectors.joining(", ")));
        }
        return Component.literal(sb.toString());
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
