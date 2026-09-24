package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.horse.StasisTier;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * <b>A Horse Stasis Bank: a filing cabinet for horses.</b>
 *
 * <h2>Today it is a chest with a filter on it, and that is the whole of stage
 * two</h2>
 * {@link #SLOTS} slots that take nothing but a
 * {@link StasisChamberItem stasis chamber}, empty or full. Put chambers in, take
 * them out, break the block and they all drop. Nothing here knows or cares what
 * is <i>inside</i> a chamber - a horse in stasis is a data component on an item,
 * and this block never unpacks one.
 *
 * <p>That is deliberate and it is the cheap half on purpose. Everything the bank
 * is eventually for - the Browse tab, passive healing, the drop buffer, in-bank
 * breeding - needs the bank to <i>act</i> on a horse that is not a loaded entity,
 * and all of it stands on being able to shelve a chamber first. See the build
 * order on {@code wiki/horse-stasis.html}'s Roadmap tab; stages three onwards add
 * containers and a second tab beside {@link #chambers()}, they do not change it.
 *
 * <h2>What the tiers do and do not buy</h2>
 * Every {@link StasisTier} stores for free and always will, so <b>this block
 * treats all four chambers alike</b>: the tier gates what the unbuilt Browse tab
 * may show, not whether a chamber may be filed. A bank that refused a Basic
 * chamber would have inverted the whole ladder - the bottom rung is the one that
 * has to be worth using.
 *
 * <h2>Slot-shaped, like the research shelf</h2>
 * Modelled on {@link EquineResearchShelfBlockEntity} down to the
 * {@code setChanged} forwarding, because it is the same problem: a
 * {@link SimpleContainer} the menu wraps on the server and the vanilla slot sync
 * fills in on the client. No {@code ContainerData} and no ticker - there is
 * nothing here that advances with nobody looking, which is exactly what will
 * change when passive healing arrives.
 */
public class HorseStasisBankBlockEntity extends BlockEntity {

    /** A double chest's worth - six rows of nine, the shelf's grid. */
    public static final int SLOTS = 54;

    private final SimpleContainer chambers = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            HorseStasisBankBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            // Chambers are stacksTo(1) anyway - see ModItems. Saying so here as
            // well keeps a shift-click from ever trying to merge two horses.
            return 1;
        }
    };

    public HorseStasisBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HORSE_STASIS_BANK.get(), pos, state);
    }

    public SimpleContainer chambers() {
        return chambers;
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
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, chambers.getItems());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        chambers.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, chambers.getItems());
    }
}
