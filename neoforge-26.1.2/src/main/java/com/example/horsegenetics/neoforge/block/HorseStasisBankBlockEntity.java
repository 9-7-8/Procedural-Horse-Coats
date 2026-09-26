package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.common.horse.StasisUpkeep;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.StasisBankIndex;
import com.example.horsegenetics.neoforge.item.EmergencyStasisChamberItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.example.horsegenetics.neoforge.server.DietFoods;
import com.example.horsegenetics.neoforge.server.StasisCare;
import com.example.horsegenetics.neoforge.server.StasisDrops;
import com.example.horsegenetics.neoforge.server.StasisStud;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>A Horse Stasis Bank: a filing cabinet for horses.</b>
 *
 * <h2>A chest with a filter on it, and a feed room behind it</h2>
 * {@link #SLOTS} slots that take nothing but a
 * {@link StasisChamberItem stasis chamber}, empty or full. Put chambers in, take
 * them out, break the block and they all drop. Nothing in <i>that</i> half knows
 * or cares what is inside a chamber - a horse in stasis is a data component on
 * an item, and the grid never unpacks one.
 *
 * <p>Beside it is the {@link #SUPPLY_SLOTS goods container}: feed, water and the
 * empties the water leaves, and then the nine-slot <b>drop buffer</b> those
 * horses fill. With something in the first two the bank <b>slowly mends the
 * horses it is holding</b>; with an Advanced chamber in the grid it also
 * collects what that horse makes.
 *
 * <h2>Three things the bank does to a horse, and the rung that buys each</h2>
 * Every {@link StasisTier} stores for free and always will, so <b>any chamber
 * may be filed here</b>: a bank that refused a Basic chamber would have
 * inverted the whole ladder, since the bottom rung is the one that has to be
 * worth using. What the tier gates is what the bank may <i>do</i>:
 * {@link StasisTier#searchable()} for the Browse tab,
 * {@link StasisTier#heals()} for the upkeep, {@link StasisTier#collectsDrops()}
 * for the buffer, and {@link StasisTier#breedsInBank()} for a chamber turned out
 * at stud ({@link StasisStud}).
 *
 * <h2>A bank with nothing to do does not tick at all</h2>
 * The tick's very first question is {@link #working}, a cached boolean
 * recomputed only when a chamber slot changes: <b>is there one chamber in here
 * the bank could do anything with?</b> A bank of Basic chambers, or of empty
 * ones, or an empty bank, answers no and the ticker returns on that line -
 * nothing is scanned, no tag is read, no supply is touched. (Owner's rule.) That
 * is what keeps the block honest about the premise of the whole feature:
 * shelving a horse has to be cheaper than leaving it in the world, and a bank
 * that woke up once per chamber per tick to find nothing to do would not be.
 *
 * <h2>Slot-shaped, like the research shelf</h2>
 * Modelled on {@link EquineResearchShelfBlockEntity} down to the
 * {@code setChanged} forwarding and the server-only ticker, because it is the
 * same problem: a {@link SimpleContainer} the menu wraps on the server and the
 * vanilla slot sync fills in on the client, plus the numbers that advance with
 * nobody looking and therefore need a {@link ContainerData} to reach the screen.
 * Here those are {@link #water()} and {@link #foalingBlocked()}.
 *
 * <h2>The goods are on the wire; the chambers are not</h2>
 * The bank exposes a NeoForge item-handler capability over the <b>goods
 * container only</b> - see {@link StasisBankCapability} - so a hopper, a dropper
 * or another mod's pipework can keep the feed and water topped up, carry the
 * empty buckets away, and empty the drop buffer. The chamber grid is
 * deliberately unreachable from any of that, which is the same call the block
 * made when it chose to <i>hold</i> a container rather than <i>be</i> one:
 * automating the filing of a live animal is not a thing anybody asked for, and
 * piping hay to one is.
 */
public class HorseStasisBankBlockEntity extends BlockEntity {

    /**
     * <b>Nine to a row - a chest's own pitch - and twenty-three rows of them.</b>
     *
     * <p>Far more rows than any window can show, which is the whole reason the
     * Chambers tab scrolls: the grid is {@link #ROWS} rows deep and
     * {@code HorseStasisBankMenu.VISIBLE_ROWS} of them are on screen at a time.
     * The count is a multiple of {@link #COLS} on purpose - a ragged last row of
     * two slots in a grid of nine reads as a bug rather than as a limit.
     *
     * <p>A bank this deep does not cost more to run: the tick still does
     * <b>one chamber's worth of work per turn</b> ({@link #tick}), so the price
     * of the extra rows is paid in how long a full bank takes to come round to
     * any one horse, not in what it costs the server. What it <i>does</i> cost is
     * bandwidth on open - see the page's Verification tab.
     */
    public static final int COLS = 9;
    public static final int ROWS = 23;
    public static final int SLOTS = COLS * ROWS;

    private final SimpleContainer chambers = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            // The one place the tick gate is recomputed: a chamber went in, came
            // out, or was swapped. The whole grid looked at when the player moves
            // one stack, rather than every tick forever.
            HorseStasisBankBlockEntity.this.working = anyWork(this);
            if (!anyAtStud(this)) {
                // A bank with nothing at stud has no mare who could be waiting
                // to foal, so the line about the room round the block has to go
                // with her - including when the player's answer to it was to
                // take her out and let her out by hand.
                HorseStasisBankBlockEntity.this.foalingBlocked = false;
            }
            HorseStasisBankBlockEntity.this.publish();
            HorseStasisBankBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            // OCCUPIED chambers are stacksTo(1) - see ModItems - but EMPTY ones
            // stack, so this is no longer merely belt and braces: it is the only
            // thing keeping a stack of empties out of a chamber slot. That
            // matters because EmergencyStasisHandler writes a filled chamber
            // straight back into the slot it found an empty one in, which would
            // destroy the rest of the stack. A slot that holds at most one is
            // what makes that write safe.
            return 1;
        }
    };

    // ------------------------------------------------------------------
    // The goods: feed and water in, empty buckets and drops out
    // ------------------------------------------------------------------

    /** Feed, water, empties - in the order the screen draws them. */
    public static final int FEED_SLOT = 0;
    public static final int WATER_SLOT = 1;
    public static final int EMPTIES_SLOT = 2;

    /**
     * <b>The drop buffer</b> - nine slots, a crafting grid's worth, filled by
     * whatever an {@link StasisTier#collectsDrops() Advanced} chamber's horse
     * would have dropped in a field.
     *
     * <p>In the same container as the supply slots rather than beside it,
     * deliberately: the item-handler capability wraps <i>one</i> container, and
     * the slot rules are written once in {@link SimpleContainer#canPlaceItem} for
     * the menu and the pipes both. A hopper under the bank therefore pulls eggs
     * out for free, and nothing can push anything into these nine.
     */
    public static final int FIRST_DROP_SLOT = 3;
    public static final int DROP_SLOTS = 9;

    /** Everything in the goods container: the three supply slots, then the buffer. */
    public static final int SUPPLY_SLOTS = FIRST_DROP_SLOT + DROP_SLOTS;

    /** {@link ContainerData} indices - the furnace pattern. */
    public static final int DATA_WATER = 0;
    public static final int DATA_FOALING = 1;
    public static final int DATA_COUNT = 2;

    private final SimpleContainer supplies = new SimpleContainer(SUPPLY_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            HorseStasisBankBlockEntity.this.setChanged();
        }

        /**
         * <b>What each slot takes</b> - and the gate pipes obey too, since
         * NeoForge's container wrapper asks exactly this before inserting.
         *
         * <p>The empties slot and the nine buffer slots take nothing: they are
         * outputs, and a hopper filling one with buckets the bank never drained,
         * or with eggs no horse here laid, would jam the one place the real ones
         * have to go.
         */
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            switch (slot) {
                case FEED_SLOT:
                    return isFeed(stack);
                case WATER_SLOT:
                    return isWater(stack);
                default:
                    return false;
            }
        }
    };

    /**
     * <b>Water the bank has left</b>, in health points it can pay for - see
     * {@link StasisUpkeep#WATER_PER_BUCKET}. A meter rather than a bucket per
     * horse: a horse in a field drinks from a pond that does not run out, and
     * charging one whole bucket for one mouthful of healing would have made the
     * feature absurd to supply.
     */
    private int water;

    /**
     * Which chamber gets the next turn. <b>One chamber per turn, in
     * rotation</b> - the whole of why a full bank is no more expensive than an
     * empty one. Saved, so a reloaded bank picks up where it left off rather
     * than always starting at slot zero and favouring the top-left horse.
     */
    private int cursor;

    /**
     * <b>The tick gate.</b> True only while some chamber in the grid holds a
     * horse the bank could heal, collect from, or breed. Recomputed when a
     * chamber slot changes and on load, never per tick, and the first line of
     * {@link #tick} returns on it.
     */
    private boolean working;

    /**
     * <b>A mare is due and there is nowhere to put her down.</b> The one thing
     * the bank can want that a player cannot see from the slots, so the Supply
     * tab says it - see {@link StasisStud}, which will not foal a horse into a
     * wall.
     *
     * <p>Not saved: it is a fact about the room, recomputed the next time her
     * turn comes round, and a reloaded bank that says nothing for a minute is
     * better than one that remembers a wall somebody has since knocked down.
     */
    private boolean foalingBlocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case DATA_WATER:
                    return water;
                case DATA_FOALING:
                    return foalingBlocked ? 1 : 0;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_WATER:
                    water = value;
                    break;
                case DATA_FOALING:
                    foalingBlocked = value != 0;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /**
     * <b>Whoever placed this bank</b>, or first opened it while it was
     * unclaimed. A bank is otherwise unowned, and the only thing this is for is
     * the emergency chamber: an empty one filed here insures <i>this</i> player's
     * horses, and spending a stranger's bottle would be theft. It grants nothing
     * else - anyone who can reach the block can still open it, file a chamber and
     * take one out, exactly as before.
     */
    private @Nullable java.util.UUID placedBy;

    public HorseStasisBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HORSE_STASIS_BANK.get(), pos, state);
    }

    /**
     * Set once, when the block is placed - {@link HorseStasisBankBlock#setPlacedBy}.
     */
    public void setPlacedBy(java.util.UUID player) {
        placedBy = player;
        publish();
        setChanged();
    }

    /**
     * <b>Claim an unowned bank by opening it.</b> The migration path, and the
     * only one there is: a bank placed before any of this existed has nobody
     * recorded against it, and would quietly never insure anything. Opening it
     * once fixes that. A bank that already has an owner is untouched, so this
     * cannot be used to take one over.
     */
    public void claimIfUnowned(java.util.UUID player) {
        if (placedBy == null) {
            setPlacedBy(player);
        }
    }

    public @Nullable java.util.UUID placedBy() {
        return placedBy;
    }

    /**
     * <b>Tell {@link StasisBankIndex} where this bank is and whether it is
     * insuring anything.</b> Called when the grid changes and when the bank
     * loads, which between them are every moment the answer can move.
     *
     * <p>The flag is the whole reason the emergency chamber can consult a bank at
     * all without searching the world: a rescue walks the index, and only a bank
     * that said yes here is worth loading a chunk for.
     */
    private void publish() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        StasisBankIndex.get(server.getServer())
                .record(placedBy, server.dimension(), getBlockPos(),
                        anyArmedEmergency(chambers), heldHorses(chambers));
    }

    /**
     * <b>Which horses are filed here</b>, for the index - the browser's
     * <i>Send home</i> button is what asks. The ids come off the snapshot
     * component, which is already on every stack, so this decodes no entity tag:
     * the whole reason {@code StasisSnapshot} unpacks the id at all is so a
     * question like this costs a field read per slot.
     */
    public static java.util.List<java.util.UUID> heldHorses(Container container) {
        java.util.List<java.util.UUID> out = new java.util.ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            var snapshot = StasisChamberItem.snapshotOf(container.getItem(i));
            if (snapshot != null) {
                out.add(snapshot.horseId());
            }
        }
        return out;
    }

    /**
     * <b>Is there an empty emergency chamber filed here?</b> The one question the
     * index caches, asked over the grid the player can see rather than anything
     * derived - an emergency chamber with a horse already in it is not insurance,
     * it is storage.
     */
    public static boolean anyArmedEmergency(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof EmergencyStasisChamberItem
                    && StasisChamberItem.snapshotOf(stack) == null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // A bank that has never been touched since the index shipped, or one in a
        // world restored from a backup, is right again from the first time its
        // chunk loads.
        publish();
    }

    public SimpleContainer chambers() {
        return chambers;
    }

    public SimpleContainer supplies() {
        return supplies;
    }

    public ContainerData data() {
        return data;
    }

    /** Water left, in health points it can pay for. */
    public int water() {
        return water;
    }

    /** A mare is due and the room around the block is full. */
    public boolean foalingBlocked() {
        return foalingBlocked;
    }

    /** Set from {@link StasisStud} when a birth is refused, or goes through. */
    public void setFoalingBlocked(boolean blocked) {
        if (foalingBlocked != blocked) {
            foalingBlocked = blocked;
            setChanged();
        }
    }

    /** Anything somebody could plausibly be trying to feed a horse - {@link DietFoods#isFeedAttempt}. */
    public static boolean isFeed(ItemStack stack) {
        return DietFoods.isFeedAttempt(stack);
    }

    /**
     * A container of water - {@code c:buckets/water}, so another mod's bucket
     * works without being named here. The same ingredient
     * {@code StasisChamberRecipe} already matches, for the same reason.
     */
    public static boolean isWater(ItemStack stack) {
        return stack.is(Tags.Items.BUCKETS_WATER);
    }

    /**
     * Is this a stasis chamber - the one thing the bank takes? Any tier, and
     * empty or occupied: storage is free at every tier, and a rack of spare
     * empties beside the full ones is the obvious way to use one.
     */
    public static boolean isChamber(ItemStack stack) {
        return stack.getItem() instanceof StasisChamberItem;
    }

    /**
     * How many of {@code container}'s chambers have a horse in them.
     *
     * <p>Static and container-based so the <i>client</i> can call it: it has the
     * synced slots but no block entity, and the whole snapshot rides on the stack
     * for the creative pick-block reason {@code StasisSnapshot} spells out. That
     * makes a count free on both sides rather than something to sync.
     */
    public static int occupied(Container container) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (isChamber(stack) && StasisChamberItem.snapshotOf(stack) != null) {
                n++;
            }
        }
        return n;
    }

    /** How many chambers are filed here at all, full or empty. */
    public static int filed(Container container) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isChamber(container.getItem(i))) {
                n++;
            }
        }
        return n;
    }

    /** The tier of the chamber in this stack, or {@code null} if it is not one. */
    public static @Nullable StasisTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof StasisChamberItem chamber ? chamber.tier() : null;
    }

    /**
     * <b>Is there one horse in here the bank could mend?</b> The Supply tab's
     * "nothing to do" line asks this of the same method the tick gate folds in,
     * so the block and the screen can never disagree about why nothing is
     * happening.
     *
     * <p>Static and container-based, like {@link #occupied}, so the client can
     * ask it of the slots it already has.
     */
    public static boolean anyHealable(Container container) {
        return anyOccupied(container, StasisTier::heals);
    }

    /** Is there one horse in here whose drops the bank collects? */
    public static boolean anyCollecting(Container container) {
        return anyOccupied(container, StasisTier::collectsDrops);
    }

    /** Is there one chamber in here turned out at stud? */
    public static boolean anyAtStud(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (StasisStud.atStud(container.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>Is there anything in here for the bank to do?</b> The tick gate, folded
     * from the three jobs so a bank of Basic chambers still costs one field read
     * a tick and no more.
     */
    public static boolean anyWork(Container container) {
        return anyHealable(container) || anyCollecting(container) || anyAtStud(container);
    }

    private static boolean anyOccupied(Container container, java.util.function.Predicate<StasisTier> what) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            StasisTier tier = tierOf(stack);
            if (tier != null && what.test(tier) && StasisChamberItem.snapshotOf(stack) != null) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // The turn
    // ------------------------------------------------------------------

    /**
     * <b>One turn, on the block</b> - so a hurt horse mends, a hen-gened one
     * lays and a mare in heat is covered with nobody looking, exactly as the
     * research shelf copies a paper with its screen shut.
     *
     * <p>Read it as three refusals and then one horse:
     * <ol>
     *   <li><b>Nothing to do</b> - {@link #working}, a cached boolean. A bank
     *       of Basic chambers costs one field read a tick and no more.</li>
     *   <li><b>Not this tick</b> - one turn every
     *       {@link StasisUpkeep#HEAL_INTERVAL} ticks, phase-staggered by
     *       position so twenty banks in a stable do not all wake on the same
     *       tick. The same interval, and the same trick,
     *       {@code HorseCareHandler} uses on live horses.</li>
     *   <li><b>Whose turn is it</b> - {@link #cursor} walks the grid one slot a
     *       turn, so the work is one chamber's worth however full the bank is,
     *       and every horse's turn comes round eventually.</li>
     * </ol>
     * Past those, that one chamber gets each of the jobs its tier has bought,
     * and each one refuses again on its own terms: the upkeep on
     * {@link StasisCare#isHurt} before it opens a bucket, the buffer on whether
     * anything is owed, and the stud on whether she is in heat at all.
     *
     * <p><b>The whole turn is one chamber's worth of work</b>, which is the
     * promise the feature is built on. Reading a stored horse's abilities costs
     * about what a live horse's own tick costs - and a live horse pays it every
     * tick, where a shelved one pays it once every
     * {@link StasisUpkeep#HEAL_INTERVAL} at its very busiest, in a bank holding
     * exactly one chamber.
     */
    public static void tick(Level level, BlockPos pos, BlockState state,
                            HorseStasisBankBlockEntity bank) {
        if (!bank.working) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), StasisUpkeep.HEAL_INTERVAL) != 0) {
            return;
        }

        int slot = bank.nextTendableSlot();
        if (slot < 0) {
            return;
        }
        ItemStack chamber = bank.chambers.getItem(slot);
        StasisTier tier = tierOf(chamber);
        if (tier == null) {
            return;
        }
        boolean changed = false;

        if (tier.heals()) {
            changed |= bank.mend(chamber);
        }
        if (tier.collectsDrops()) {
            changed |= bank.collect(chamber, level);
        }
        if (StasisStud.atStud(chamber) && level instanceof ServerLevel server) {
            changed |= StasisStud.turn(server, pos, bank, slot);
        }

        if (changed) {
            bank.chambers.setChanged();
            bank.setChanged();
        }
    }

    /**
     * Feed and heal the horse in this chamber, if it is hurt at all.
     *
     * <p>The cheap two-field read in {@link StasisCare#isHurt} comes first,
     * before anything decodes a record or resolves a diet - and a bank of well
     * horses never turns a bucket into an empty, which is the same promise
     * {@link StasisUpkeep} makes about the feed slot.
     */
    private boolean mend(ItemStack chamber) {
        if (!StasisCare.isHurt(chamber)) {
            return false;
        }
        drawWater();

        ItemStack feed = supplies.getItem(FEED_SLOT);
        StasisUpkeep.Result result = StasisCare.turn(chamber, feed, water);
        if (result == null || !result.changed()) {
            return false;
        }
        if (result.ate()) {
            supplies.removeItem(FEED_SLOT, 1);
        }
        if (result.water() != water) {
            water = result.water();
        }
        return true;
    }

    /**
     * <b>Take whatever this horse has made into the buffer</b> - and take
     * <i>all</i> of it or none.
     *
     * <p>The dry run is the point. A live horse drops its egg on the ground and
     * walks away; a bank has no ground, so a yield it cannot hold would simply
     * cease to exist. Instead the cooldown is only stamped once the items are
     * actually in, so a full buffer pauses production rather than silently
     * eating it, and emptying the buffer starts it again on the next turn.
     */
    private boolean collect(ItemStack chamber, Level level) {
        Rng rng = new NeoRng(level.getRandom());
        StasisDrops.Batch batch = StasisDrops.due(chamber, level.getGameTime(), rng);
        if (batch == null || !roomFor(batch.stacks())) {
            return false;
        }
        if (!StasisDrops.stamp(chamber, batch.geneKeys(), level.getGameTime())) {
            return false;
        }
        for (ItemStack stack : batch.stacks()) {
            store(stack.copy());
        }
        supplies.setChanged();
        return true;
    }

    /** Would every one of these fit? Asked before anything is moved. */
    private boolean roomFor(List<ItemStack> stacks) {
        SimpleContainer trial = new SimpleContainer(DROP_SLOTS);
        for (int i = 0; i < DROP_SLOTS; i++) {
            trial.setItem(i, supplies.getItem(FIRST_DROP_SLOT + i).copy());
        }
        for (ItemStack stack : stacks) {
            if (!trial.addItem(stack.copy()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Put one stack into the buffer. Only called once {@link #roomFor} has said yes. */
    private void store(ItemStack stack) {
        for (int i = 0; i < DROP_SLOTS && !stack.isEmpty(); i++) {
            int slot = FIRST_DROP_SLOT + i;
            ItemStack there = supplies.getItem(slot);
            if (there.isEmpty()) {
                supplies.setItem(slot, stack.split(stack.getCount()));
            } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                int room = Math.min(there.getMaxStackSize(), supplies.getMaxStackSize()) - there.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    there.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
    }

    /**
     * Which chamber's turn it is: the next one round the grid the bank could do
     * something with. Leaves {@link #cursor} on the slot after it, so the next
     * turn starts where this one stopped.
     *
     * @return the slot, or {@code -1} if a full lap found nothing - which is
     *         possible even past {@link #working}, since that flag is only
     *         recomputed when a slot changes
     */
    private int nextTendableSlot() {
        for (int step = 0; step < SLOTS; step++) {
            int slot = (cursor + step) % SLOTS;
            ItemStack stack = chambers.getItem(slot);
            StasisTier tier = tierOf(stack);
            if (tier == null || StasisChamberItem.snapshotOf(stack) == null) {
                continue;
            }
            if (tier.heals() || tier.collectsDrops() || StasisStud.atStud(stack)) {
                cursor = (slot + 1) % SLOTS;
                return slot;
            }
        }
        return -1;
    }

    /**
     * <b>Fill the meter from the water slot</b>, and leave the container behind
     * in the empties slot.
     *
     * <p>Only when the meter is dry, so a half-used bucket's worth is never
     * thrown away to make room for a whole one - and only when there is
     * somewhere for the empty to go, so the bank never destroys a bucket it
     * cannot hand back. The remainder is the stack's <b>own declared crafting
     * remainder</b>, which is how a modded water container comes back as that
     * mod's empty one rather than as a vanilla bucket - the same rule, for the
     * same reason, as {@code StasisChamberRecipe.getRemainingItems}.
     */
    private void drawWater() {
        if (water > 0) {
            return;
        }
        ItemStack source = supplies.getItem(WATER_SLOT);
        if (!isWater(source)) {
            return;
        }
        ItemStackTemplate declared = source.getCraftingRemainder();
        ItemStack empty = declared != null ? declared.create() : ItemStack.EMPTY;
        ItemStack waiting = supplies.getItem(EMPTIES_SLOT);
        if (!empty.isEmpty()) {
            if (waiting.isEmpty()) {
                supplies.setItem(EMPTIES_SLOT, empty);
            } else if (ItemStack.isSameItemSameComponents(waiting, empty)
                    && waiting.getCount() < waiting.getMaxStackSize()) {
                waiting.grow(1);
                supplies.setChanged();
            } else {
                // Nowhere to put the empty: leave the water where it is rather
                // than drink a bucket and destroy it.
                return;
            }
        }
        source.shrink(1);
        supplies.setChanged();
        water = StasisUpkeep.WATER_PER_BUCKET;
        setChanged();
    }

    /**
     * <b>Give every chamber back when the bank is broken</b> - here, where
     * vanilla's chests do it, because this is the last moment the block entity
     * still exists.
     *
     * <p>Worth being blunt about what is at stake: each of these stacks may be a
     * pedigreed horse. Dropping them is the only acceptable behaviour, and it is
     * why the block is deliberately not flammable - see
     * {@link HorseStasisBankBlock#bankProperties()}.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, chambers);
            Containers.dropContents(this.level, pos, supplies);
        }
        if (this.level instanceof ServerLevel server) {
            // Out of the index here rather than lazily on the next rescue: a
            // stale row is a chunk loaded to look at a block that is not there,
            // and this is the one moment it is certainly gone.
            StasisBankIndex.get(server.getServer()).forget(server.dimension(), pos);
        }
    }

    /**
     * The chambers go under the container helper's own key; the goods slots are
     * written one by one under names of their own, exactly as the research shelf
     * writes its book and its result beside its papers. Two {@code saveAllItems}
     * calls would have overwritten each other.
     */
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, chambers.getItems());
        output.store("feed", ItemStack.OPTIONAL_CODEC, supplies.getItem(FEED_SLOT));
        output.store("water_in", ItemStack.OPTIONAL_CODEC, supplies.getItem(WATER_SLOT));
        output.store("empties", ItemStack.OPTIONAL_CODEC, supplies.getItem(EMPTIES_SLOT));
        for (int i = 0; i < DROP_SLOTS; i++) {
            output.store("drop_" + i, ItemStack.OPTIONAL_CODEC, supplies.getItem(FIRST_DROP_SLOT + i));
        }
        output.putInt("water", water);
        output.putInt("cursor", cursor);
        if (placedBy != null) {
            output.store("placed_by", net.minecraft.core.UUIDUtil.CODEC, placedBy);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        chambers.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, chambers.getItems());
        supplies.getItems().set(FEED_SLOT,
                input.read("feed", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        supplies.getItems().set(WATER_SLOT,
                input.read("water_in", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        supplies.getItems().set(EMPTIES_SLOT,
                input.read("empties", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < DROP_SLOTS; i++) {
            supplies.getItems().set(FIRST_DROP_SLOT + i,
                    input.read("drop_" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        water = input.getIntOr("water", 0);
        cursor = Math.floorMod(input.getIntOr("cursor", 0), SLOTS);
        placedBy = input.read("placed_by", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        // The gate is derived, not saved: loading the grid does not go through
        // the container's setChanged, so it is recomputed here or a reloaded
        // bank would sit inert until somebody touched a slot.
        working = anyWork(chambers);
    }
}
