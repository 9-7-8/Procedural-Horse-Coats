package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.common.horse.StasisUpkeep;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import com.example.horsegenetics.neoforge.server.DietFoods;
import com.example.horsegenetics.neoforge.server.StasisCare;
import net.minecraft.core.BlockPos;
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
 * <p>Beside it are the three {@link #SUPPLY_SLOTS supply slots} - feed, water,
 * and the empties the water leaves - which are the other half, and the whole of
 * stage four: with something in them the bank <b>slowly mends the horses it is
 * holding</b>. That is the first thing the bank does <i>to</i> a horse rather
 * than merely shelving one, and everything left on the Roadmap tab (the drop
 * buffer, in-bank breeding) is more of the same machinery.
 *
 * <h2>What the tiers do and do not buy</h2>
 * Every {@link StasisTier} stores for free and always will, so <b>any chamber
 * may be filed here</b>: a bank that refused a Basic chamber would have
 * inverted the whole ladder, since the bottom rung is the one that has to be
 * worth using. What the tier gates is what the bank may <i>do</i> with the
 * horse - {@link StasisTier#searchable()} for the Browse tab, and
 * {@link StasisTier#heals()} for the upkeep below.
 *
 * <h2>A bank with nothing to heal does not tick at all</h2>
 * The upkeep's very first question is {@link #healing}, a cached boolean
 * recomputed only when a chamber slot changes: <b>is there one chamber in here
 * that can be healed?</b> A bank of Basic chambers, or of empty ones, or an
 * empty bank, answers no and the ticker returns on that line - nothing is
 * scanned, no tag is read, no supply is touched. (Owner's rule.) That is what
 * keeps the block honest about the premise of the whole feature: shelving a
 * horse has to be cheaper than leaving it in the world, and a bank that woke up
 * fifty-four times a second to find nothing to do would not be.
 *
 * <h2>Slot-shaped, like the research shelf</h2>
 * Modelled on {@link EquineResearchShelfBlockEntity} down to the
 * {@code setChanged} forwarding and the server-only ticker, because it is the
 * same problem: a {@link SimpleContainer} the menu wraps on the server and the
 * vanilla slot sync fills in on the client, plus one number that advances with
 * nobody looking and therefore needs a {@link ContainerData} to reach the
 * screen. Here that number is {@link #water()}.
 *
 * <h2>The supply slots are on the wire; the chambers are not</h2>
 * The bank exposes a NeoForge item-handler capability over the <b>supply slots
 * only</b> - see {@link StasisBankCapability} - so a hopper, a dropper or
 * another mod's pipework can keep the feed and water topped up and carry the
 * empty buckets away. The chamber grid is deliberately unreachable from any of
 * that, which is the same call the block made when it chose to <i>hold</i> a
 * container rather than <i>be</i> one: automating the filing of a live animal is
 * not a thing anybody asked for, and piping hay to one is.
 */
public class HorseStasisBankBlockEntity extends BlockEntity {

    /** A double chest's worth - six rows of nine, the shelf's grid. */
    public static final int SLOTS = 54;

    private final SimpleContainer chambers = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            // The one place the tick gate is recomputed: a chamber went in, came
            // out, or was swapped. Fifty-four stacks looked at when the player
            // moves one, rather than every tick forever.
            HorseStasisBankBlockEntity.this.healing = anyHealable(this);
            HorseStasisBankBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            // Chambers are stacksTo(1) anyway - see ModItems. Saying so here as
            // well keeps a shift-click from ever trying to merge two horses.
            return 1;
        }
    };

    // ------------------------------------------------------------------
    // The supply room: feed in, water in, empty buckets out
    // ------------------------------------------------------------------

    /** Feed, water, empties - in the order the screen draws them. */
    public static final int SUPPLY_SLOTS = 3;
    public static final int FEED_SLOT = 0;
    public static final int WATER_SLOT = 1;
    public static final int EMPTIES_SLOT = 2;

    /** {@link ContainerData} indices - one number, the furnace pattern. */
    public static final int DATA_WATER = 0;
    public static final int DATA_COUNT = 1;

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
         * <p>The empties slot takes nothing: it is an output, and a hopper
         * filling it with buckets the bank never drained would jam the one
         * place the drained ones have to go.
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
     * horse <i>and</i> is of a tier that {@link StasisTier#heals()}. Recomputed
     * when a chamber slot changes and on load, never per tick, and the first
     * line of {@link #tick} returns on it.
     */
    private boolean healing;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_WATER ? water : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_WATER) {
                water = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HorseStasisBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HORSE_STASIS_BANK.get(), pos, state);
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
     * <b>Is there one horse in here the bank could mend?</b> The tick gate, and
     * the Supply tab's "nothing to do" line, ask the same question of the same
     * method so the block and the screen can never disagree about why nothing
     * is happening.
     *
     * <p>Static and container-based, like {@link #occupied}, so the client can
     * ask it of the slots it already has.
     */
    public static boolean anyHealable(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            StasisTier tier = tierOf(stack);
            if (tier != null && tier.heals() && StasisChamberItem.snapshotOf(stack) != null) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // The upkeep
    // ------------------------------------------------------------------

    /**
     * <b>One turn of upkeep, on the block</b> - so a hurt horse mends with
     * nobody looking, exactly as the research shelf copies a paper with its
     * screen shut.
     *
     * <p>Read it as four refusals and then one horse:
     * <ol>
     *   <li><b>Nothing to heal</b> - {@link #healing}, a cached boolean. A bank
     *       of Basic chambers costs one field read a tick and no more.</li>
     *   <li><b>Not this tick</b> - one turn every
     *       {@link StasisUpkeep#HEAL_INTERVAL} ticks, phase-staggered by
     *       position so twenty banks in a stable do not all wake on the same
     *       tick. The same interval, and the same trick,
     *       {@code HorseCareHandler} uses on live horses.</li>
     *   <li><b>Whose turn is it</b> - {@link #cursor} walks the grid one slot a
     *       turn, so the work is one chamber's worth however full the bank is,
     *       and every horse's turn comes round eventually.</li>
     *   <li><b>Is that one hurt</b> - the cheap two-field read in
     *       {@link StasisCare#isHurt}, before anything decodes a record or a
     *       diet.</li>
     * </ol>
     * Only past all four does anything read a genotype, open a bucket or take a
     * mouthful out of the feed slot.
     */
    public static void tick(Level level, BlockPos pos, BlockState state,
                            HorseStasisBankBlockEntity bank) {
        if (!bank.healing) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), StasisUpkeep.HEAL_INTERVAL) != 0) {
            return;
        }

        int slot = bank.nextHealableSlot();
        if (slot < 0) {
            return;
        }
        ItemStack chamber = bank.chambers.getItem(slot);
        if (!StasisCare.isHurt(chamber)) {
            return;
        }

        // Only now, with a hurt horse in front of it, does the bank open a
        // bucket. A bank of well horses never turns one into an empty, which is
        // the same promise StasisUpkeep makes about the feed slot.
        bank.drawWater();

        ItemStack feed = bank.supplies.getItem(FEED_SLOT);
        StasisUpkeep.Result result = StasisCare.turn(chamber, feed, bank.water);
        if (result == null || !result.changed()) {
            return;
        }
        if (result.ate()) {
            bank.supplies.removeItem(FEED_SLOT, 1);
        }
        if (result.water() != bank.water) {
            bank.water = result.water();
        }
        bank.chambers.setChanged();
        bank.setChanged();
    }

    /**
     * Which chamber's turn it is: the next one round the grid that holds a
     * horse the bank may mend. Leaves {@link #cursor} on the slot after it, so
     * the next turn starts where this one stopped.
     *
     * @return the slot, or {@code -1} if a full lap found nothing - which is
     *         possible even past {@link #healing}, since that flag is only
     *         recomputed when a slot changes
     */
    private int nextHealableSlot() {
        for (int step = 0; step < SLOTS; step++) {
            int slot = (cursor + step) % SLOTS;
            ItemStack stack = chambers.getItem(slot);
            StasisTier tier = tierOf(stack);
            if (tier != null && tier.heals() && StasisChamberItem.snapshotOf(stack) != null) {
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
    }

    /**
     * The chambers go under the container helper's own key; the supply slots
     * are written one by one under names of their own, exactly as the research
     * shelf writes its book and its result beside its papers. Two
     * {@code saveAllItems} calls would have overwritten each other.
     */
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, chambers.getItems());
        output.store("feed", ItemStack.OPTIONAL_CODEC, supplies.getItem(FEED_SLOT));
        output.store("water_in", ItemStack.OPTIONAL_CODEC, supplies.getItem(WATER_SLOT));
        output.store("empties", ItemStack.OPTIONAL_CODEC, supplies.getItem(EMPTIES_SLOT));
        output.putInt("water", water);
        output.putInt("cursor", cursor);
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
        water = input.getIntOr("water", 0);
        cursor = Math.floorMod(input.getIntOr("cursor", 0), SLOTS);
        // The gate is derived, not saved: loading the grid does not go through
        // the container's setChanged, so it is recomputed here or a reloaded
        // bank would sit inert until somebody touched a slot.
        healing = anyHealable(chambers);
    }
}
