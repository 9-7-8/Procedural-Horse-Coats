package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The books an Equine Research Shelf is holding.</b>
 *
 * <h2>A set of gene keys, not a container of items</h2>
 * A research paper is completely described by the one gene it names, and the
 * shelf holds <b>at most one of each</b>, so what it stores is a set of keys.
 * That is not a shortcut - it is the rule made structural. There is no way to
 * express "two papers for flaxen" in this data, so no code has to remember not
 * to, and a shelf can hold every gene in the mod without holding an item stack
 * per gene.
 *
 * <p>Papers become items again at exactly two moments: when a player withdraws
 * one, and when the block is broken.
 *
 * <h2>Order</h2>
 * Kept sorted by gene name so the Store tab reads like a shelf rather than like
 * an insertion log. Unknown keys - a gene that was registered when the paper was
 * filed and is not now - are kept in the file but sorted last and shown by their
 * raw key, because silently dropping somebody's collection because they removed
 * a gene pack would be worse than a row that reads oddly.
 */
public class EquineResearchShelfBlockEntity extends BlockEntity {

    private static final String KEY = "genes";

    private final Set<String> genes = new LinkedHashSet<>();

    public EquineResearchShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_SHELF.get(), pos, state);
    }

    /** Every gene this shelf can copy, in display order. */
    public List<String> storedGenes() {
        List<String> out = new ArrayList<>(genes);
        out.sort(Comparator.comparing(EquineResearchShelfBlockEntity::displayName,
                String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    public boolean stores(String geneKey) {
        return genes.contains(geneKey);
    }

    /** @return false if this shelf already had that gene - one copy of each. */
    public boolean file(String geneKey) {
        if (geneKey == null || geneKey.isEmpty() || !genes.add(geneKey)) {
            return false;
        }
        setChanged();
        return true;
    }

    /** @return false if it was not there. */
    public boolean withdraw(String geneKey) {
        if (!genes.remove(geneKey)) {
            return false;
        }
        setChanged();
        return true;
    }

    /** The name a row shows: the gene's, or its raw key if this build has no such gene. */
    public static String displayName(String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        return gene == null ? geneKey : gene.name();
    }

    /**
     * <b>Drive the copy for whoever has this shelf open.</b> The work lives on
     * the menu (it needs the selected gene and the book slot, neither of which
     * is block state), so the ticker's whole job is to find the menus looking at
     * this block and tick them.
     *
     * <p>Ticking the <i>menu</i> rather than the block is what lets two players
     * at one shelf each copy their own gene onto their own book, which is the
     * behaviour that falls out of the menu owning the slots.
     */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState state, EquineResearchShelfBlockEntity shelf) {
        if (level.isClientSide()) {
            return;
        }
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (player.containerMenu instanceof com.example.horsegenetics.neoforge.menu.ResearchShelfMenu menu
                    && menu.isFor(shelf)) {
                menu.tickCopy();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(KEY, Codec.STRING.listOf(), List.copyOf(genes));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        genes.clear();
        input.read(KEY, Codec.STRING.listOf()).ifPresent(genes::addAll);
    }
}
