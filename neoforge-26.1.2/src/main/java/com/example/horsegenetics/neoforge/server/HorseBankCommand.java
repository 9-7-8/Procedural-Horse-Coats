package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseBankPlan;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.data.HorseSocialAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.StasisBankIndex;
import com.example.horsegenetics.neoforge.item.EmergencyStasisChamberItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>{@code /horsebank}</b> - file all of a player's horses into their own stasis
 * bank in one go (issue #272, from the friends' server, owner 2026-10-24).
 *
 * <p>The plain command takes the player's own horses that are in loaded chunks of
 * their dimension and not being ridden, oldest first ({@link HorseBankPlan}), and
 * puts each into one of the empty chambers the player carries, in the order their
 * client config names ({@code horsebank.chamber_order}), until the empties run out.
 * The filled chambers go straight into the player's own nearest bank within
 * {@code horsebank.search_chunks}. Nothing is force-loaded: a horse in an unloaded
 * chunk is simply not seen. {@code horsebank.allow_non_op} off makes it operator-only.
 *
 * <p>Each horse goes through {@link HorseStasisHandler#swallow}, the one capture
 * path, so a filed horse is exactly a hand-captured one: same whereabouts mark,
 * same snapshot, same sound.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseBankCommand {

    private HorseBankCommand() {
    }

    /** Each online player's chamber order, as their client sent it on join. */
    private static final Map<UUID, List<String>> ORDERS = new ConcurrentHashMap<>();

    /** File what a client says its order is - cleaned, so a forged list names only real kinds. */
    public static void fileOrder(ServerPlayer player, List<String> order) {
        ORDERS.put(player.getUUID(), HorseBankPlan.cleanOrder(order));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ORDERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horsebank")
                .requires(source -> ServerConfig.horseBankNonOp()
                        || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source))
                .executes(c -> plain(c.getSource())));
    }

    private static int plain(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/horsebank files a player's horses; run it as a player."));
            return 0;
        }
        if (!ServerConfig.horseBankNonOp() && !Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
            source.sendFailure(Component.literal("/horsebank is for operators only on this server."));
            return 0;
        }
        ServerLevel level = player.level();
        HorseStasisBankBlockEntity bank = nearestOwnBank(level, player);
        if (bank == null) {
            source.sendFailure(Component.literal("You have no stasis bank of your own within "
                    + ServerConfig.horseBankSearchBlocks() + " blocks. Place one, or stand nearer to it."));
            return 0;
        }
        return file(source, player, level, bank, false);
    }

    /**
     * <b>The player's own nearest bank</b> in their dimension within the search
     * radius, whose chunk is loaded - or null. Read from {@link StasisBankIndex},
     * so no chunk is touched to look.
     */
    static @Nullable HorseStasisBankBlockEntity nearestOwnBank(ServerLevel level, ServerPlayer player) {
        double reach = ServerConfig.horseBankSearchBlocks();
        double best = reach * reach;
        HorseStasisBankBlockEntity found = null;
        for (StasisBankIndex.Bank b : StasisBankIndex.get(level.getServer()).banksOf(player.getUUID())) {
            if (!b.dimension().equals(level.dimension())) {
                continue;
            }
            double d = b.pos().getCenter().distanceToSqr(player.position());
            if (d > best || !level.isLoaded(b.pos())) {
                continue;
            }
            if (level.getBlockEntity(b.pos()) instanceof HorseStasisBankBlockEntity bank) {
                best = d;
                found = bank;
            }
        }
        return found;
    }

    /**
     * Carry the plan out: every one of the player's horses the plan has a chamber
     * for goes into it and into {@code bank}. {@code makeBasics} is the operator's
     * {@code force}: past the player's own empties, a Basic chamber is made.
     */
    static int file(CommandSourceStack source, ServerPlayer player, ServerLevel level,
                    HorseStasisBankBlockEntity bank, boolean makeBasics) {
        UUID owner = player.getUUID();
        Map<UUID, Horse> byId = new HashMap<>();
        List<HorseBankPlan.Candidate> candidates = new ArrayList<>();
        int ridden = 0;
        for (Horse horse : level.getEntities(EntityTypeTest.forClass(Horse.class),
                h -> h.isAlive() && HorseOwnership.isOwner(h, owner))) {
            if (horse.isVehicle()) {
                ridden++;   // passengers would be discarded with it - the hand capture's refusal
                continue;
            }
            HorseSocialAttachment social = horse.getData(ModAttachments.HORSE_SOCIAL.get());
            long born = social.bornKnown() ? social.bornTick() : HorseBankPlan.UNKNOWN_BIRTH;
            byId.put(horse.getUUID(), horse);
            candidates.add(new HorseBankPlan.Candidate(horse.getUUID(), born));
        }
        if (candidates.isEmpty()) {
            source.sendFailure(Component.literal(ridden > 0
                    ? "Every horse of yours nearby has somebody on its back; nothing was filed."
                    : "No horses of yours are loaded in this dimension; nothing was filed."));
            return 0;
        }

        Inventory pack = player.getInventory();
        List<String> order = ORDERS.getOrDefault(owner, HorseBankPlan.DEFAULT_ORDER);
        HorseBankPlan.Plan plan = HorseBankPlan.plan(candidates, emptiesIn(pack), order, makeBasics);

        int filed = 0;
        int made = 0;
        for (HorseBankPlan.Filing filing : plan.filings()) {
            Horse horse = byId.get(filing.horse());
            ItemStack chamber;
            if (filing.madeNew()) {
                chamber = new ItemStack(com.example.horsegenetics.neoforge.item.ModItems.BASIC_STASIS_CHAMBER.get());
            } else {
                chamber = takeEmpty(pack, filing.kind());
                if (chamber.isEmpty()) {
                    continue;   // counted a moment ago; cannot happen unless something moved it
                }
            }
            if (horse.isLeashed()) {
                horse.dropLeash();   // the lead comes back as an item rather than vanishing with the horse
            }
            String name = HorseRecords.of(horse).displayName();
            ItemStack full = HorseStasisHandler.swallow(level, horse, chamber, name);
            bank.chambers().setItem(bank.chambers().firstEmpty(), full);
            filed++;
            if (filing.madeNew()) {
                made++;
            }
            ActionTrace.log("stasis", name + " filed into a " + (filing.madeNew() ? "new basic" : filing.kind())
                    + " chamber in the bank at " + bank.getBlockPos().toShortString() + " by /horsebank ("
                    + player.getGameProfile().name() + ")");
        }

        StringBuilder line = new StringBuilder("Filed ").append(filed).append(filed == 1 ? " horse" : " horses")
                .append(" into your stasis bank at ").append(bank.getBlockPos().toShortString()).append(", oldest first");
        if (made > 0) {
            line.append(" (").append(made).append(" in new Basic chambers)");
        }
        line.append('.');
        if (plan.leftOut() > 0) {
            line.append(' ').append(plan.leftOut()).append(plan.leftOut() == 1 ? " was" : " were")
                    .append(" left out: no empty chambers left in the order you use (")
                    .append(HorseBankPlan.describe(order)).append(").");
        }
        if (ridden > 0) {
            line.append(' ').append(ridden).append(ridden == 1 ? " horse is" : " horses are")
                    .append(" being ridden and stayed out.");
        }
        source.sendSuccess(() -> Component.literal(line.toString()).withStyle(ChatFormatting.GOLD), false);
        return filed;
    }

    /** How many empty chambers of each kind the player carries. */
    private static Map<String, Integer> emptiesIn(Inventory pack) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            String kind = kindOf(stack);
            if (kind != null) {
                out.merge(kind, stack.getCount(), Integer::sum);
            }
        }
        return out;
    }

    /** One empty chamber of this kind off the player's stacks, count 1; EMPTY if there is none. */
    private static ItemStack takeEmpty(Inventory pack, String kind) {
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (kind.equals(kindOf(stack))) {
                ItemStack one = stack.copyWithCount(1);
                stack.shrink(1);
                pack.setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The order-list word for an empty chamber, or null for anything else. */
    private static @Nullable String kindOf(ItemStack stack) {
        if (!(stack.getItem() instanceof StasisChamberItem chamber) || StasisChamberItem.snapshotOf(stack) != null) {
            return null;
        }
        return chamber instanceof EmergencyStasisChamberItem ? HorseBankPlan.EMERGENCY : chamber.tier().id();
    }
}
