package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.horse.StasisUpkeep;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Row AT: keeping horses - and keeping them in a bank</b> (2026-10-01). Two pens, every check a clockwork one:
 * built, started and answered with nobody at the keyboard, each ending in one {@code CLOCKWORK} line.
 *
 * <table>
 *   <tr><th>half</th><th>pen</th><th>footprint</th><th>answers by</th></tr>
 *   <tr><td>west</td><td>KEEPING HORSES</td><td>x0..x0+17, z0+1..z0+6</td><td>30 s (owned); ~20 s to 3 min (sold)</td></tr>
 *   <tr><td>east</td><td>STASIS BANK</td><td>x0+25..x0+44, z0+1..z0+12</td><td>10 s (break) to 20 min (mend)</td></tr>
 * </table>
 *
 * <h2>KEEPING HORSES (west)</h2>
 *
 * <p><b>1. An owned horse never despawns.</b> {@code wiki/horse-care.html}, open check 300: <i>"nothing in this mod
 * stops an owned horse despawning, and whether vanilla does is unproven ... if it does not, it is the worst failure a
 * horse mod can have and the fix is one call."</i> The page asked for the joined sources; they are on disk
 * ({@code neoforge-26.1.2/build/moddev/artifacts/minecraft-patched-26.1.2.100-sources.jar}) and this is what
 * 26.1.2's {@code Mob.checkDespawn} says, read 2026-10-01:
 * <pre>
 *   if (EventHooks.checkMobDespawn(this)) return;                 // NeoForge MobDespawnEvent - this mod has no subscriber
 *   if (PEACEFUL &amp;&amp; !getType().isAllowedInPeaceful()) discard();
 *   else if (!isPersistenceRequired() &amp;&amp; !requiresCustomPersistence()) {   // requiresCustom = passenger || leashed
 *       nearest player ... if (dist &gt; despawn distance &amp;&amp; removeWhenFarAway(distSqr)) discard();
 *       ... noActionTime &gt; 600, 1 in 800, dist &gt; no-despawn distance &amp;&amp; removeWhenFarAway(distSqr) -&gt; discard();
 *   }
 * </pre>
 * Every distance removal is ANDed with {@code removeWhenFarAway}, and {@code Animal.removeWhenFarAway} returns
 * {@code false} unconditionally (neither {@code AbstractHorse} nor {@code Horse} overrides it). So by the sources no
 * horse, owned or wild, is ever removed for distance. A server can't watch a despawn that never happens, so the pen
 * doesn't wait for one. It spawns a horse tamed and owned exactly as {@code HorseGiveCommand} and the old clockwork jar
 * pen did it ({@code setTamed(true)} + {@code setOwner(hands)}, the yard hands' fixed UUID as the owner), and reads the
 * three predicates off the live entity. <b>PASS:</b> the horse is alive, owned by the hands, allowed in peaceful, and
 * at least one of {@code isPersistenceRequired()}, {@code requiresCustomPersistence()} or
 * {@code !removeWhenFarAway(1e6)} holds - so no branch of {@code checkDespawn} can remove it. <b>FAIL:</b> all three
 * allow removal. An untamed horse spawned by COMMAND beside it gives the same three numbers for contrast in the
 * detail. 30 seconds. This does not cover another mod's {@code MobDespawnEvent} returning ALLOW.
 *
 * <p><b>2. A horse sold to a wandering trader leaves with him.</b> {@code wiki/villagers.html}, the trader sale:
 * <i>"when he despawns, the horse must go with him and not be left standing there as an unowned persistent animal
 * accumulating in the world."</i> A trader and a horse in one glass cell. The hands own the horse, put a lead on it,
 * crouch and click the trader - {@code TraderSale.onEntityInteract}'s real path, through {@code Player.interactOn},
 * so {@code leadAway} runs exactly as a player's sale would run it. Then {@code setDespawnDelay(200)} makes his own
 * {@code maybeDespawn} discard him ten seconds later. Forty ticks after he is gone the horse is looked up by UUID.
 * <b>PASS:</b> the horse is gone too. <b>FAIL:</b> he is gone and the horse is still standing there - its
 * persistence, tamed flag, owner and leash are in the detail. <b>INCONCLUSIVE:</b> the sale never happened (what
 * the hands were told is in the detail), or he never despawned within 3 minutes.
 * <br>The brief offered a weaker PASS, "or at least no longer persistent". It is left out on purpose: a horse that
 * is not persistent still never despawns for distance (check 1's reading of {@code Animal.removeWhenFarAway}), so it
 * would still be "left standing there" and add to the pile. The persistence flag goes in the detail for the reader.
 * <br>What the sources predict: {@code leadAway} sets persistence, and nothing in this mod reacts to the trader
 * leaving. {@code Leashable.tickLeash} just drops a lead whose holder can no longer interact, so a FAIL is the
 * expected result. If it fails, that's a real gap in {@code TraderSale}, not in the pen.
 *
 * <h2>STASIS BANK (east)</h2>
 * {@code wiki/horse-stasis.html}. Every chamber is filled by the mod's own capture path,
 * {@link HorseStasisHandler#swallow}, with a real horse that has been in the world for three seconds (the founding
 * tick has run, as the bank gametest insists). The filled stacks go into the bank's real chamber grid. Slots 54 and up
 * are below the screen's six visible rows ({@code HorseStasisBankMenu.VISIBLE_ROWS}), and 206 is the very last slot.
 *
 * <p><b>a. No hopper reaches a chamber.</b> Verification tab: <i>"No hopper can reach a chamber, in either direction.
 * This is the one that matters: a hopper that can drain a bank of horses is a disaster, not a nuisance."</i> The setup,
 * top to bottom: a hopper of 16 hay bales, a bank holding three Basic chambers with horses (slots 0, 54, 206) and one
 * empty Intermediate chamber (slot 1, the stackable kind), a hopper under it, and a chest under that.
 * {@code StasisBankCapability} exposes only the goods container, so the top hopper should feed the feed slot and the
 * bottom one may pull from the goods. The hay is the control: if none leaves the top hopper, no hopper is talking to
 * the bank at all, and the chamber count proves nothing (INCONCLUSIVE). <b>PASS:</b> after 2 minutes the chest and the
 * lower hopper hold no chamber, and the bank still files the same chambers with the same horses in slots 0, 54 and
 * 206. <b>FAIL:</b> a chamber left the grid. Where the hay ended up (feed slot, or drained on into the chest) is in
 * the detail. A lower hopper that empties the feed slot would be a finding of its own, but not this claim.
 *
 * <p><b>b. Breaking the bank drops every chamber.</b> <i>"The one worth being paranoid about: break it with chambers
 * inside. Pass: every chamber drops."</i> A bank in a glass cell holds three Basic chambers with horses, in slots 0,
 * 30 and 200. {@code level.destroyBlock(pos, true)} runs five seconds after capture, and two seconds later every
 * {@link ItemEntity} in the cell is read. <b>PASS:</b> the three horse UUIDs all come back on dropped chambers whose
 * {@code STASIS_SNAPSHOT} component still carries a non-empty entity tag. <b>FAIL:</b> any one is missing.
 *
 * <p><b>c. Fire and lava.</b> <i>"Set fire to a bank, and drop another in lava, each with a chamber inside. Pass:
 * neither block is destroyed."</i> Two sealed stone basins, each with a bank (a filled Basic chamber inside) in the
 * middle. One has fire on netherrack in the eight cells around it, re-lit every 10 s. The other has eight lava sources.
 * <b>The catch, read in the 26.1.2 sources:</b> {@code FireBlock.tick} and {@code LavaFluid.randomTick} both do nothing
 * unless {@code ServerLevel.canSpreadFireAround}, which wants a player within the {@code fire_spread_radius_around_player}
 * gamerule (default 128). An unattended yard usually has none, so surviving 3 minutes alone could prove nothing. The
 * verdict therefore rests on the exact predicates those two code paths use. Fire burns a block only when
 * {@code random.nextInt(chance) < state.getFlammability(level, pos, face)} ({@code checkBurnOut}), so a flammability
 * of 0 on all six faces means it never can. Lava only ever ignites the air beside a block for which
 * {@code state.ignitedByLava(level, pos, face)} holds, and it never destroys a solid block itself. <b>PASS</b>
 * (each its own check, 3 minutes): the bank still stands with its horse still in it, and the predicate is 0 / false on
 * every face. <b>FAIL:</b> the block is gone, or a face is flammable. Whether live fire or lava ever ticked is in the
 * detail.
 *
 * <p><b>d. The bank actually mends a horse.</b> Verification tab, "The bank actually mends a horse": <i>"Hurt a
 * horse, capture it into an Intermediate chamber, file it, put a hay bale and a water bucket in the supply slots and
 * walk away. Come back: is its health up?"</i>, <i>"The empty bucket comes back, into the third slot"</i>, and <i>"A
 * bank of nothing but Basic chambers must consume nothing at all."</i> Each horse goes in at 30% health with hunger 60,
 * so it will eat. The stored health is read where the bank itself reads it: {@code StasisCare.health} on the chamber's
 * {@code STASIS_SNAPSHOT} tag, and {@code StasisCare.maxHealth} for the bar. A turn comes every
 * {@link StasisUpkeep#HEAL_INTERVAL} ticks, so a lone chamber should be mended within a minute. The Intermediate bank is
 * polled every 30 s for up to 20 minutes. <b>PASS:</b> the stored health rose, the water bucket was drawn, and the slot
 * {@code drawWater} fills holds an empty bucket (vanilla's water bucket declares {@code Items.BUCKET} as its crafting
 * remainder). <b>FAIL:</b> the health never rose in 20 minutes, or it rose with no bucket back. The Basic bank has the
 * same supplies and its own check at 5 minutes. <b>PASS:</b> the hay, the water bucket and the stored health are all
 * exactly as they went in, and the empties slot is empty.
 *
 * <p>Every API here was copied from a use elsewhere in this repo, or read in the 26.1.2 sources jar where noted. The
 * few that are only in the sources are marked {@code UNVERIFIED} at the call.
 */
final class DebugYardKeep {

    private DebugYardKeep() {
    }

    // ------------------------------------------------------------------
    // Check names
    // ------------------------------------------------------------------

    static final String OWNED = "KEEPING HORSES - an owned horse can never be removed for distance (horse-care check 300)";
    static final String SOLD = "KEEPING HORSES - a horse sold to a wandering trader goes when he despawns (villagers)";
    static final String HOPPERS = "STASIS BANK - no hopper reaches a chamber, in either direction";
    static final String BROKEN = "STASIS BANK - a broken bank drops every chamber, each still holding its horse";
    static final String FIRE = "STASIS BANK - a bank in fire is not destroyed";
    static final String LAVA = "STASIS BANK - a bank in lava is not destroyed";
    static final String MENDS = "STASIS BANK - an Intermediate bank mends a stored horse from hay and water, and the bucket comes back";
    static final String BASIC_IDLE = "STASIS BANK - a Basic bank with the same supplies consumes nothing";

    /** Ticks a bank horse stands in the world before capture, so its founding tick has run. */
    private static final long CAPTURE_AT = 60L;
    private static final int HAY_IN_HOPPER = 16;
    /** The high chamber slots: 54 is the first one past the six visible rows, 206 the last of all. */
    private static final int[] HOPPER_SLOTS = {0, 54, HorseStasisBankBlockEntity.SLOTS - 1};
    private static final int[] BREAK_SLOTS = {0, 30, 200};
    private static final float HURT_TO = 0.3F;
    private static final double HUNGER_AT = 60.0;

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    static void build(ServerLevel level, int gy, int x0, int z0) {
        try {
            for (String c : List.of(OWNED, SOLD, HOPPERS, BROKEN, FIRE, LAVA, MENDS, BASIC_IDLE)) {
                DebugYardClockwork.expect(c);
            }
            keeping(level, gy, x0, z0);
            stasis(level, gy, x0 + 25, z0);
            ActionTrace.log("test yard", "row AT built (KEEPING HORSES west, STASIS BANK east)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AT (KEEPING HORSES / STASIS BANK) failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // KEEPING HORSES
    // ------------------------------------------------------------------

    private static void keeping(ServerLevel level, int gy, int x0, int z0) {
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("KEEPING HORSES", "owned: can it", "ever despawn? sold:", "gone with trader?"));

        // 1. Owned horse, and its untamed contrast, each in a 4x4 cell (a 2x2 floor).
        DebugYardClockwork.cell(level, gy, x0 + 1, x0 + 4, z0 + 1, z0 + 4);
        DebugYardClockwork.cell(level, gy, x0 + 6, x0 + 9, z0 + 1, z0 + 4);
        glow(level, gy, x0 + 2, z0 + 2);
        glow(level, gy, x0 + 7, z0 + 2);
        Horse owned = DebugYardClockwork.horse(level, gy, x0 + 3.0, z0 + 3.0, Sex.FEMALE, "", "OWNED");
        Horse wild = DebugYardUnattended.horse(level, gy, x0 + 8.0, z0 + 3.0, Sex.FEMALE, "", false, "WILD (contrast)");
        DebugYardClockwork.Hands owner = new DebugYardClockwork.Hands(level);
        if (owned != null) {
            // HorseGiveCommand's way of making a horse somebody's, and the old jar pen's: proven in the yard.
            owned.setTamed(true);
            owned.setOwner(owner);
        }
        UUID ownedId = owned == null ? null : owned.getUUID();
        UUID wildId = wild == null ? null : wild.getUUID();
        UUID handsId = owner.getUUID();
        DebugYardHerd.after(level, 600, () -> ownedVerdict(level, ownedId, wildId, handsId));

        // 2. The sale: a trader and a horse in one 7x6 cell (a 5x4 floor).
        DebugYardClockwork.cell(level, gy, x0 + 11, x0 + 17, z0 + 1, z0 + 6);
        glow(level, gy, x0 + 14, z0 + 3);
        WanderingTrader trader = EntityType.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
        Horse sold = DebugYardClockwork.horse(level, gy, x0 + 15.0, z0 + 4.0, Sex.MALE, "", "SOLD TO TRADER");
        if (trader == null || sold == null) {
            DebugYardClockwork.inconclusive(SOLD, "trader " + (trader != null) + ", horse " + (sold != null)
                    + " - one failed to spawn");
            return;
        }
        trader.snapTo(x0 + 13.0, gy + 1, z0 + 3.0, 0.0F, 0.0F);
        // Only his own despawn timer may take him: without this the vanilla distance rule could also discard
        // him if a player stood far off, and the check would be reading the wrong exit. maybeDespawn ignores it.
        trader.setPersistenceRequired();
        trader.setCustomName(Component.literal("BUYER"));
        level.addFreshEntity(trader);
        DebugYardHerd.after(level, CAPTURE_AT, () -> sell(level, trader, sold));
    }

    private static void ownedVerdict(ServerLevel level, @Nullable UUID ownedId, @Nullable UUID wildId, UUID handsId) {
        Horse owned = ownedId == null ? null : level.getEntity(ownedId) instanceof Horse h && h.isAlive() ? h : null;
        Horse wild = wildId == null ? null : level.getEntity(wildId) instanceof Horse h && h.isAlive() ? h : null;
        if (owned == null) {
            DebugYardClockwork.inconclusive(OWNED, "the owned horse is missing at 30 s (spawned " + (ownedId != null)
                    + ") - nothing to read");
            return;
        }
        boolean isOwned = HorseOwnership.isOwner(owned, handsId);
        if (!isOwned) {
            DebugYardClockwork.inconclusive(OWNED, "the horse did not end up owned by the hands (tamed "
                    + owned.isTamed() + ", owner " + HorseOwnership.ownerId(owned) + ") - the setup failed");
            return;
        }
        boolean safe = owned.isPersistenceRequired() || owned.requiresCustomPersistence()
                || !owned.removeWhenFarAway(1.0e6);
        boolean peaceful = owned.getType().isAllowedInPeaceful();
        DebugYardClockwork.verdict(OWNED, safe && peaceful,
                "owned horse " + ActionTrace.describeShort(owned) + ": " + predicates(owned)
                        + " | untamed COMMAND horse: " + (wild == null ? "missing" : predicates(wild))
                        + " | Mob.checkDespawn (26.1.2 sources) discards for distance only when NOT persistenceRequired"
                        + " AND NOT requiresCustomPersistence AND removeWhenFarAway(distSqr); this mod has no"
                        + " MobDespawnEvent subscriber");
    }

    /**
     * UNVERIFIED in this repo (read in the 26.1.2 sources, nothing here calls them yet): {@code isPersistenceRequired},
     * {@code requiresCustomPersistence}, {@code removeWhenFarAway} (only overridden, by {@code Cowboy}) and
     * {@code EntityType.isAllowedInPeaceful}. All four are public in {@code Mob} / {@code EntityType}.
     */
    private static String predicates(Horse h) {
        return "persistenceRequired " + h.isPersistenceRequired()
                + ", requiresCustomPersistence " + h.requiresCustomPersistence()
                + ", removeWhenFarAway(1e6) " + h.removeWhenFarAway(1.0e6)
                + ", allowedInPeaceful " + h.getType().isAllowedInPeaceful()
                + ", tamed " + h.isTamed() + ", leashed " + h.isLeashed();
    }

    /**
     * Sell the horse to the trader the way a player does: own it, lead it, crouch, click him. Both the lead and the
     * click happen in this one step because the hands are not in the level: {@code Leashable.tickLeash} would drop a
     * lead held by them on the horse's next tick, since a FakePlayer that never joined cannot "interact with the level".
     */
    private static void sell(ServerLevel level, WanderingTrader trader, Horse horse) {
        if (!trader.isAlive() || !horse.isAlive()) {
            DebugYardClockwork.inconclusive(SOLD, "before the sale: trader alive " + trader.isAlive()
                    + ", horse alive " + horse.isAlive());
            return;
        }
        if (horse.isBaby()) {
            DebugYardClockwork.inconclusive(SOLD, "the stocked horse is a foal, which he refuses - the setup failed");
            return;
        }
        DebugYardClockwork.Hands h = DebugYardClockwork.hands(level, horse, ItemStack.EMPTY, false);
        horse.setTamed(true);
        horse.setOwner(h);
        horse.setLeashedTo(h, true);
        // setShiftKeyDown: the old clockwork vet's-kit pen used exactly this to crouch the hands.
        h.setShiftKeyDown(true);
        DebugYardClockwork.use(h, trader);

        boolean onHisRope = horse.isLeashed() && horse.getLeashHolder() == trader;
        int paid = h.count(Items.EMERALD);
        if (!onHisRope || horse.isTamed()) {
            DebugYardClockwork.inconclusive(SOLD, "the sale did not happen: leashed " + horse.isLeashed()
                    + ", holder is the trader " + onHisRope + ", tamed " + horse.isTamed() + ", emeralds " + paid
                    + ", hasRealRecord " + HorseRecords.hasRealRecord(horse) + "; " + h.said());
            return;
        }
        String sale = "sold for " + paid + " emeralds, on his rope, tamed " + horse.isTamed()
                + ", persistent " + horse.isPersistenceRequired() + "; ";
        // UNVERIFIED in this repo (read in the 26.1.2 sources): WanderingTrader.setDespawnDelay is public, and his
        // aiStep's maybeDespawn discards him when it counts down to 0 while he is not trading.
        trader.setDespawnDelay(200);
        UUID traderId = trader.getUUID();
        UUID horseId = horse.getUUID();
        long deadline = level.getGameTime() + 3600L;
        DebugYardHerd.after(level, 20, () -> waitForHimToGo(level, traderId, horseId, deadline, sale));
    }

    private static void waitForHimToGo(ServerLevel level, UUID traderId, UUID horseId, long deadline, String sale) {
        Entity him = level.getEntity(traderId);
        if (him == null || !him.isAlive()) {
            // Two seconds for the horse's own leash tick to notice.
            DebugYardHerd.after(level, 40, () -> soldVerdict(level, horseId, sale));
            return;
        }
        if (level.getGameTime() >= deadline) {
            Horse horse = level.getEntity(horseId) instanceof Horse x ? x : null;
            DebugYardClockwork.inconclusive(SOLD, sale + "the trader never despawned in 3 minutes (despawnDelay now "
                    + ((WanderingTrader) him).getDespawnDelay() + "); horse alive "
                    + (horse != null && horse.isAlive()));
            return;
        }
        DebugYardHerd.after(level, 20, () -> waitForHimToGo(level, traderId, horseId, deadline, sale));
    }

    private static void soldVerdict(ServerLevel level, UUID horseId, String sale) {
        Entity e = level.getEntity(horseId);
        Horse horse = e instanceof Horse h && h.isAlive() ? h : null;
        if (horse == null) {
            DebugYardClockwork.verdict(SOLD, true, sale + "the trader despawned and the horse went with him"
                    + " (no live entity with its UUID)");
            return;
        }
        Entity holder = horse.getLeashHolder();
        DebugYardClockwork.verdict(SOLD, false, sale + "the trader despawned and the horse is still standing there: "
                + ActionTrace.describeShort(horse) + ", persistent " + horse.isPersistenceRequired()
                + ", tamed " + horse.isTamed() + ", owner " + HorseOwnership.ownerId(horse)
                + ", record owner " + HorseRecords.of(horse).ownerId()
                + ", leashed " + horse.isLeashed() + (holder == null ? "" : " to " + holder.getType())
                + ", removeWhenFarAway(1e6) " + horse.removeWhenFarAway(1.0e6)
                + " - nothing will ever remove it");
    }

    // ------------------------------------------------------------------
    // STASIS BANK
    // ------------------------------------------------------------------

    /** The ten horses the bank checks swallow, in the order {@link #capture} hands them out. */
    private static final int HORSES = 10;

    private static void stasis(ServerLevel level, int gy, int e, int z0) {
        DebugPenManager.placeSign(level, new BlockPos(e + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("STASIS BANK", "hoppers, breaking,", "fire, lava, and", "does it mend?"));

        // a. The hopper column: chest, hopper, bank, hopper. Hoppers face down by default.
        BlockPos chest = new BlockPos(e + 1, gy + 1, z0 + 2);
        BlockPos lowHopper = chest.above();
        BlockPos hopperBank = lowHopper.above();
        BlockPos topHopper = hopperBank.above();
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(lowHopper, Blocks.HOPPER.defaultBlockState(), 3);
        bank(level, hopperBank);
        level.setBlock(topHopper, Blocks.HOPPER.defaultBlockState(), 3);

        // b. A bank to break, in a glass cell so every drop stays where it can be counted.
        DebugYardClockwork.cell(level, gy, e + 3, e + 7, z0 + 1, z0 + 5);
        glow(level, gy, e + 4, z0 + 2);
        BlockPos breakBank = new BlockPos(e + 5, gy + 1, z0 + 3);
        bank(level, breakBank);

        // c. Fire and lava, each in a sealed stone basin.
        BlockPos fireBank = basin(level, gy, e + 11, z0 + 3, Blocks.NETHERRACK.defaultBlockState());
        BlockPos lavaBank = basin(level, gy, e + 17, z0 + 3, Blocks.STONE.defaultBlockState());

        // d. Two banks side by side, open air.
        BlockPos mendBank = new BlockPos(e + 2, gy + 1, z0 + 9);
        BlockPos basicBank = new BlockPos(e + 5, gy + 1, z0 + 9);
        bank(level, mendBank);
        bank(level, basicBank);

        // The ten horses wait in a holding cell until their founding tick has run.
        DebugYardClockwork.cell(level, gy, e + 9, e + 19, z0 + 7, z0 + 12);
        glow(level, gy, e + 11, z0 + 9);
        glow(level, gy, e + 16, z0 + 9);
        List<Horse> horses = new ArrayList<>();
        for (int i = 0; i < HORSES; i++) {
            double x = e + 10.5 + 2 * (i % 5);
            double z = z0 + (i < 5 ? 8.7 : 10.7);
            horses.add(DebugYardUnattended.horse(level, gy, x, z, i % 2 == 0 ? Sex.FEMALE : Sex.MALE, "", false,
                    "BANK HORSE " + (i + 1)));
        }

        DebugYardHerd.after(level, CAPTURE_AT, () -> {
            hoppers(level, horses.subList(0, 3), hopperBank, topHopper, lowHopper, chest);
            breaking(level, gy, e, z0, horses.subList(3, 6), breakBank);
            burning(level, horses.get(6), fireBank, FIRE, true);
            burning(level, horses.get(7), lavaBank, LAVA, false);
            mending(level, horses.get(8), horses.get(9), mendBank, basicBank);
        });
    }

    /** Place a bank and hand back its block entity, or null. */
    private static @Nullable HorseStasisBankBlockEntity bank(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.HORSE_STASIS_BANK.get().defaultBlockState(), 3);
        return bankAt(level, pos);
    }

    private static @Nullable HorseStasisBankBlockEntity bankAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HorseStasisBankBlockEntity b ? b : null;
    }

    /**
     * The capture: {@link HorseStasisHandler#swallow}, the one place a live horse becomes a chamber. The hunger is
     * set first so the tag carries the attachment, which {@code StasisCare.putHunger} will not invent (the bank
     * gametest found the same thing). Returns the filled chamber, or null if the horse is not there.
     */
    private static @Nullable ItemStack capture(ServerLevel level, @Nullable Horse horse, Item emptyChamber,
                                               float health, double hunger) {
        if (horse == null || !horse.isAlive()) {
            return null;
        }
        if (health < 1.0F) {
            horse.setHealth(horse.getMaxHealth() * health);
        }
        horse.setData(ModAttachments.HUNGER.get(), hunger);
        String name = HorseRecords.of(horse).displayName();
        return HorseStasisHandler.swallow(level, horse, new ItemStack(emptyChamber), name);
    }

    private static @Nullable UUID horseIn(ItemStack stack) {
        StasisSnapshot s = StasisChamberItem.snapshotOf(stack);
        return s == null ? null : s.horseId();
    }

    // --- a. hoppers ---

    private static void hoppers(ServerLevel level, List<Horse> horses, BlockPos bankPos, BlockPos topPos,
                                BlockPos lowPos, BlockPos chestPos) {
        HorseStasisBankBlockEntity bank = bankAt(level, bankPos);
        UUID[] expected = new UUID[HOPPER_SLOTS.length];
        for (int i = 0; i < HOPPER_SLOTS.length; i++) {
            ItemStack filled = capture(level, horses.get(i), ModItems.BASIC_STASIS_CHAMBER.get(), 1.0F, Hunger.FULL);
            if (bank == null || filled == null) {
                DebugYardClockwork.inconclusive(HOPPERS, "bank " + (bank != null) + ", chamber " + i + " filled "
                        + (filled != null) + " - setup failed");
                return;
            }
            expected[i] = horseIn(filled);
            bank.chambers().setItem(HOPPER_SLOTS[i], filled);
        }
        bank.chambers().setItem(1, new ItemStack(ModItems.INTERMEDIATE_STASIS_CHAMBER.get()));
        // UNVERIFIED in this repo: hopper and chest block entities as plain Containers. Both are vanilla
        // BaseContainerBlockEntity subclasses, so it should hold, and a miss makes this INCONCLUSIVE, not wrong.
        if (!(level.getBlockEntity(topPos) instanceof Container top)) {
            DebugYardClockwork.inconclusive(HOPPERS, "the top hopper has no container block entity");
            return;
        }
        top.setItem(0, new ItemStack(Items.HAY_BLOCK, HAY_IN_HOPPER));
        int filed0 = HorseStasisBankBlockEntity.filed(bank.chambers());
        int occupied0 = HorseStasisBankBlockEntity.occupied(bank.chambers());

        DebugYardHerd.after(level, 2400, () -> {
            HorseStasisBankBlockEntity b = bankAt(level, bankPos);
            Container topNow = level.getBlockEntity(topPos) instanceof Container c ? c : null;
            Container low = level.getBlockEntity(lowPos) instanceof Container c ? c : null;
            Container chest = level.getBlockEntity(chestPos) instanceof Container c ? c : null;
            if (b == null || topNow == null || low == null || chest == null) {
                DebugYardClockwork.inconclusive(HOPPERS, "after 2 min: bank " + (b != null) + ", top hopper "
                        + (topNow != null) + ", low hopper " + (low != null) + ", chest " + (chest != null));
                return;
            }
            int hayLeftTop = countOf(topNow, Items.HAY_BLOCK);
            ItemStack feed = b.supplies().getItem(HorseStasisBankBlockEntity.FEED_SLOT);
            int hayInFeed = feed.is(Items.HAY_BLOCK) ? feed.getCount() : 0;
            int hayBelow = countOf(low, Items.HAY_BLOCK) + countOf(chest, Items.HAY_BLOCK);
            int out = chambersIn(low) + chambersIn(chest);
            int filed = HorseStasisBankBlockEntity.filed(b.chambers());
            int occupied = HorseStasisBankBlockEntity.occupied(b.chambers());
            boolean sameHorses = true;
            StringBuilder slots = new StringBuilder();
            for (int i = 0; i < HOPPER_SLOTS.length; i++) {
                UUID there = horseIn(b.chambers().getItem(HOPPER_SLOTS[i]));
                boolean same = expected[i] != null && expected[i].equals(there);
                sameHorses &= same;
                slots.append(slots.length() == 0 ? "" : ", ").append("slot ").append(HOPPER_SLOTS[i])
                        .append(same ? " same horse" : " CHANGED (" + there + ")");
            }
            String detail = "hay: " + (HAY_IN_HOPPER - hayLeftTop) + "/" + HAY_IN_HOPPER + " left the top hopper, "
                    + hayInFeed + " in the feed slot, " + hayBelow + " drained on into the low hopper + chest"
                    + " | chambers: filed " + filed0 + " -> " + filed + ", occupied " + occupied0 + " -> " + occupied
                    + ", " + out + " in the low hopper + chest; " + slots;
            if (hayLeftTop == HAY_IN_HOPPER) {
                DebugYardClockwork.inconclusive(HOPPERS, "the top hopper moved nothing into the bank, so no hopper"
                        + " is connected and the chamber count shows nothing | " + detail);
                return;
            }
            DebugYardClockwork.verdict(HOPPERS, out == 0 && filed == filed0 && occupied == occupied0 && sameHorses,
                    detail);
        });
    }

    private static int countOf(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static int chambersIn(Container c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (HorseStasisBankBlockEntity.isChamber(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // --- b. breaking ---

    private static void breaking(ServerLevel level, int gy, int e, int z0, List<Horse> horses, BlockPos pos) {
        HorseStasisBankBlockEntity bank = bankAt(level, pos);
        Set<UUID> expected = new HashSet<>();
        for (int i = 0; i < BREAK_SLOTS.length; i++) {
            ItemStack filled = capture(level, horses.get(i), ModItems.BASIC_STASIS_CHAMBER.get(), 1.0F, Hunger.FULL);
            if (bank == null || filled == null) {
                DebugYardClockwork.inconclusive(BROKEN, "bank " + (bank != null) + ", chamber " + i + " filled "
                        + (filled != null) + " - setup failed");
                return;
            }
            expected.add(horseIn(filled));
            bank.chambers().setItem(BREAK_SLOTS[i], filled);
        }
        DebugYardHerd.after(level, 100, () -> {
            HorseStasisBankBlockEntity b = bankAt(level, pos);
            if (b == null) {
                DebugYardClockwork.inconclusive(BROKEN, "the bank was gone before it was broken");
                return;
            }
            int filedBefore = HorseStasisBankBlockEntity.occupied(b.chambers());
            boolean destroyed = level.destroyBlock(pos, true);
            DebugYardHerd.after(level, 40, () -> {
                Set<UUID> found = new HashSet<>();
                int chamberItems = 0;
                int emptyTags = 0;
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                        DebugTestYard.box(e + 3, gy, z0 + 1, e + 8, gy + 4, z0 + 6))) {
                    ItemStack s = item.getItem();
                    if (!HorseStasisBankBlockEntity.isChamber(s)) {
                        continue;
                    }
                    chamberItems += s.getCount();
                    StasisSnapshot snap = StasisChamberItem.snapshotOf(s);
                    if (snap == null) {
                        continue;
                    }
                    CompoundTag tag = snap.horse();
                    if (tag == null || tag.isEmpty()) {
                        emptyTags++;
                    } else {
                        found.add(snap.horseId());
                    }
                }
                boolean stillThere = level.getBlockState(pos).is(ModBlocks.HORSE_STASIS_BANK.get());
                String detail = "occupied before " + filedBefore + " (slots " + BREAK_SLOTS[0] + ", " + BREAK_SLOTS[1]
                        + ", " + BREAK_SLOTS[2] + "), destroyBlock returned " + destroyed + ", block still a bank "
                        + stillThere + "; dropped chamber items " + chamberItems + ", carrying " + found.size()
                        + " of the " + expected.size() + " horses, " + emptyTags + " with an empty entity tag";
                if (stillThere) {
                    DebugYardClockwork.inconclusive(BROKEN, "the bank was not broken | " + detail);
                    return;
                }
                DebugYardClockwork.verdict(BROKEN, found.containsAll(expected) && emptyTags == 0, detail);
            });
        });
    }

    // --- c. fire and lava ---

    /**
     * A sealed 5x5 stone basin centred on {@code (cx, cz)}: walls three high, a stone lid, a bank in the middle and
     * {@code ring} as the floor of the eight cells round it. Returns where the bank is.
     */
    private static BlockPos basin(ServerLevel level, int gy, int cx, int cz, BlockState ring) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean centre = dx == 0 && dz == 0;
                int x = cx + dx;
                int z = cz + dz;
                DebugPenManager.groundColumn(level, x, gy, z, edge || centre ? stone : ring);
                for (int y = gy + 1; y <= gy + 3; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), edge ? stone : air);
                }
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 4, z), stone);
            }
        }
        BlockPos at = new BlockPos(cx, gy + 1, cz);
        bank(level, at);
        return at;
    }

    private static List<BlockPos> ringAround(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    out.add(centre.offset(dx, 0, dz));
                }
            }
        }
        return out;
    }

    /** What the fire or lava did over its three minutes. */
    private static final class Exposure {
        int checks;
        int present;
        boolean ticked;
    }

    private static void burning(ServerLevel level, @Nullable Horse horse, BlockPos pos, String check, boolean fire) {
        HorseStasisBankBlockEntity bank = bankAt(level, pos);
        ItemStack filled = capture(level, horse, ModItems.BASIC_STASIS_CHAMBER.get(), 1.0F, Hunger.FULL);
        if (bank == null || filled == null) {
            DebugYardClockwork.inconclusive(check, "bank " + (bank != null) + ", chamber filled " + (filled != null)
                    + " - setup failed");
            return;
        }
        UUID inside = horseIn(filled);
        bank.chambers().setItem(0, filled);
        List<BlockPos> ring = ringAround(pos);
        BlockState what = fire ? Blocks.FIRE.defaultBlockState() : Blocks.LAVA.defaultBlockState();
        for (BlockPos p : ring) {
            level.setBlock(p, what, 3);
        }
        Exposure seen = new Exposure();
        long end = level.getGameTime() + 3600L;
        tend(level, pos, ring, what, fire, seen, end, check, inside);
    }

    /** Every 10 s: count what is still burning, note whether it could tick, re-light the fire, and at the end judge. */
    private static void tend(ServerLevel level, BlockPos pos, List<BlockPos> ring, BlockState what, boolean fire,
                             Exposure seen, long end, String check, UUID inside) {
        DebugYardHerd.after(level, 200, () -> {
            seen.checks++;
            // UNVERIFIED in this repo (read in the 26.1.2 sources): ServerLevel.canSpreadFireAround is public, and
            // both FireBlock.tick and LavaFluid.randomTick return at once without it.
            seen.ticked |= level.canSpreadFireAround(ring.get(0));
            for (BlockPos p : ring) {
                if (level.getBlockState(p).is(what.getBlock())) {
                    seen.present++;
                } else if (fire) {
                    level.setBlock(p, what, 3);
                }
            }
            if (level.getGameTime() < end) {
                tend(level, pos, ring, what, fire, seen, end, check, inside);
                return;
            }
            judgeBurning(level, pos, fire, seen, check, inside);
        });
    }

    private static void judgeBurning(ServerLevel level, BlockPos pos, boolean fire, Exposure seen, String check,
                                     UUID inside) {
        BlockState state = level.getBlockState(pos);
        boolean standing = state.is(ModBlocks.HORSE_STASIS_BANK.get());
        HorseStasisBankBlockEntity bank = bankAt(level, pos);
        boolean horseStill = bank != null && inside != null
                && inside.equals(horseIn(bank.chambers().getItem(0)));
        int worst = 0;
        boolean lavaLights = false;
        if (standing) {
            for (Direction face : Direction.values()) {
                // UNVERIFIED in this repo (read in the 26.1.2 sources): the NeoForge BlockState extensions
                // getFlammability(level, pos, face) - FireBlock.checkBurnOut's own call - and
                // ignitedByLava(level, pos, face) - LavaFluid.isFlammable's own call.
                worst = Math.max(worst, state.getFlammability(level, pos, face));
                lavaLights |= state.ignitedByLava(level, pos, face);
            }
        }
        int radius = level.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        String detail = "after 3 min: bank standing " + standing + ", its horse still in slot 0 " + horseStill
                + "; " + (fire ? "fire" : "lava") + " present in " + seen.present + " of " + (seen.checks * 8)
                + " ring readings (every 10 s" + (fire ? ", re-lit each time" : "") + "); could tick "
                + seen.ticked + " (fire_spread_radius_around_player " + radius
                + (seen.ticked ? ")" : ", and no player that close, so live " + (fire ? "fire" : "lava")
                + " never ran and the verdict rests on the predicate)")
                + "; max getFlammability over 6 faces " + worst + ", ignitedByLava on any face " + lavaLights;
        boolean pass = standing && horseStill && (fire ? worst == 0 : !lavaLights);
        DebugYardClockwork.verdict(check, pass, detail);
    }

    // --- d. mending ---

    private static void mending(ServerLevel level, @Nullable Horse hurt, @Nullable Horse idle, BlockPos mendPos,
                                BlockPos basicPos) {
        HorseStasisBankBlockEntity mend = bankAt(level, mendPos);
        HorseStasisBankBlockEntity basic = bankAt(level, basicPos);
        ItemStack a = capture(level, hurt, ModItems.INTERMEDIATE_STASIS_CHAMBER.get(), HURT_TO, HUNGER_AT);
        ItemStack b = capture(level, idle, ModItems.BASIC_STASIS_CHAMBER.get(), HURT_TO, HUNGER_AT);

        if (mend == null || a == null) {
            DebugYardClockwork.inconclusive(MENDS, "bank " + (mend != null) + ", chamber filled " + (a != null)
                    + " - setup failed");
        } else {
            Reading r0 = Reading.of(a);
            if (r0 == null || r0.max <= 0.0F || r0.health >= r0.max) {
                DebugYardClockwork.inconclusive(MENDS, "the stored horse does not read as hurt (" + r0
                        + ") - nothing for the bank to mend");
            } else {
                stock(mend);
                mend.chambers().setItem(0, a);
                long end = level.getGameTime() + 24_000L;
                pollMend(level, mendPos, r0, end);
            }
        }

        if (basic == null || b == null) {
            DebugYardClockwork.inconclusive(BASIC_IDLE, "bank " + (basic != null) + ", chamber filled " + (b != null)
                    + " - setup failed");
        } else {
            Reading r0 = Reading.of(b);
            stock(basic);
            basic.chambers().setItem(0, b);
            DebugYardHerd.after(level, 6000, () -> {
                HorseStasisBankBlockEntity bank = bankAt(level, basicPos);
                if (bank == null || r0 == null) {
                    DebugYardClockwork.inconclusive(BASIC_IDLE, "bank " + (bank != null) + ", first reading " + r0);
                    return;
                }
                Reading r = Reading.of(bank.chambers().getItem(0));
                Supplies s = Supplies.of(bank);
                boolean same = r != null && Math.abs(r.health - r0.health) < 0.01F
                        && Math.abs(r.hunger - r0.hunger) < 0.01;
                DebugYardClockwork.verdict(BASIC_IDLE,
                        same && s.hay == 1 && s.waterBuckets == 1 && s.empties == 0 && bank.water() == 0,
                        "after 5 min: stored " + r0 + " -> " + r + "; " + s + ", water meter " + bank.water());
            });
        }
    }

    /** One hay bale and one water bucket, straight into the supply slots. */
    private static void stock(HorseStasisBankBlockEntity bank) {
        bank.supplies().setItem(HorseStasisBankBlockEntity.FEED_SLOT, new ItemStack(Items.HAY_BLOCK));
        bank.supplies().setItem(HorseStasisBankBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET));
    }

    private static void pollMend(ServerLevel level, BlockPos pos, Reading r0, long end) {
        DebugYardHerd.after(level, 600, () -> {
            HorseStasisBankBlockEntity bank = bankAt(level, pos);
            if (bank == null) {
                DebugYardClockwork.inconclusive(MENDS, "the bank is gone; first reading " + r0);
                return;
            }
            Reading r = Reading.of(bank.chambers().getItem(0));
            Supplies s = Supplies.of(bank);
            String detail = "stored " + r0 + " -> " + r + "; " + s + ", water meter " + bank.water()
                    + " (" + StasisUpkeep.WATER_PER_BUCKET + " a bucket), turn every " + StasisUpkeep.HEAL_INTERVAL
                    + " ticks";
            if (r != null && r.health > r0.health + 0.5F) {
                boolean bucketBack = s.emptyBuckets == 1;
                boolean drew = s.waterBuckets == 0;
                DebugYardClockwork.verdict(MENDS, bucketBack && drew, "health rose; bucket drawn " + drew
                        + ", empty bucket in the empties slot " + bucketBack + " | " + detail);
                return;
            }
            if (level.getGameTime() >= end) {
                DebugYardClockwork.verdict(MENDS, false, "20 minutes and the stored health never rose | " + detail);
                return;
            }
            pollMend(level, pos, r0, end);
        });
    }

    /** The numbers the bank itself reads off a chamber's tag. */
    private record Reading(float health, float max, double hunger) {
        static @Nullable Reading of(ItemStack chamber) {
            StasisSnapshot s = StasisChamberItem.snapshotOf(chamber);
            if (s == null) {
                return null;
            }
            CompoundTag tag = s.horse();
            return new Reading(StasisCare.health(tag), StasisCare.maxHealth(tag, StasisCare.record(tag)),
                    StasisCare.hunger(tag));
        }

        @Override
        public String toString() {
            return String.format("health %.1f/%.1f hunger %.1f", health, max, hunger);
        }
    }

    /** What is in a bank's three supply slots. */
    private record Supplies(int hay, int waterBuckets, int empties, int emptyBuckets) {
        static Supplies of(HorseStasisBankBlockEntity bank) {
            ItemStack feed = bank.supplies().getItem(HorseStasisBankBlockEntity.FEED_SLOT);
            ItemStack water = bank.supplies().getItem(HorseStasisBankBlockEntity.WATER_SLOT);
            ItemStack empties = bank.supplies().getItem(HorseStasisBankBlockEntity.EMPTIES_SLOT);
            return new Supplies(feed.is(Items.HAY_BLOCK) ? feed.getCount() : 0,
                    water.is(Items.WATER_BUCKET) ? water.getCount() : 0,
                    empties.getCount(), empties.is(Items.BUCKET) ? empties.getCount() : 0);
        }

        @Override
        public String toString() {
            return "feed slot hay " + hay + ", water slot buckets " + waterBuckets + ", empties slot " + empties
                    + " (empty buckets " + emptyBuckets + ")";
        }
    }

    // ------------------------------------------------------------------

    /** Glowstone in a cell's stone-brick lid, so nothing spawns in the dark under it. */
    private static void glow(ServerLevel level, int gy, int x, int z) {
        DebugPenManager.fastSet(level, new BlockPos(x, gy + 3, z), Blocks.GLOWSTONE.defaultBlockState());
    }
}
