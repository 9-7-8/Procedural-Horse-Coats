package com.example.horsegenetics.neoforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * <b>What one jump is made of.</b> Holds nothing but a
 * {@link JumpMaterials} - the wood of the rails and the wood of the standards.
 *
 * <h2>A data-only block entity, and it must not become anything else</h2>
 * No ticker, no inventory, no menu state. A course is hundreds of these, and a
 * block entity that ticks is a block entity that costs something per tick
 * forever. The screen that edits it is a {@code ContainerLevelAccess} menu in
 * the shape of the Tack Dyeing Bench, which holds no state of its own.
 *
 * <h2>This is the first block entity in the mod that syncs to the client</h2>
 * It has to be: the model reads the woods to decide which textures to draw, and
 * the model runs client-side. Nothing else here had that problem, so
 * {@link #getUpdatePacket} and {@link #getUpdateTag} are new ground in this
 * codebase - a repo-wide search for them found nothing before this class.
 *
 * <p><b>The classic omission this is written to avoid:</b> syncing the data is
 * not enough. The chunk section has already been meshed with the old woods, and
 * nothing re-meshes it just because a block entity changed. So
 * {@link #setMaterials} asks for a model-data refresh <i>and</i> tells the level
 * the block changed, which is what actually dirties the section. Without the
 * second half the jump keeps its old wood until something unrelated happens
 * nearby - which looks exactly like "the repaint did not work".
 */
public class JumpBlockEntity extends BlockEntity {

    /**
     * The key the model reads out of {@link ModelData}.
     *
     * <p>Whatever goes in here <b>must be immutable</b>: model data is read on
     * chunk-meshing worker threads, off the main thread, from a snapshot taken
     * of the region. {@link JumpMaterials} is a record of two strings, which
     * satisfies that by construction.
     */
    public static final ModelProperty<JumpMaterials> MATERIALS = new ModelProperty<>();

    private JumpMaterials materials = JumpMaterials.DEFAULT;

    public JumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JUMP.get(), pos, state);
    }

    public JumpMaterials materials() {
        return this.materials;
    }

    /**
     * Change what this jump is made of, and make the change visible.
     *
     * <p>Three things, and all three are needed - see the class note on the
     * omission this avoids.
     */
    public void setMaterials(JumpMaterials materials) {
        this.materials = materials;
        this.setChanged();
        if (this.level != null) {
            // Sends the update packet to anybody tracking the chunk...
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(),
                    this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            // ...and on the client, drops the cached model data so the section
            // is re-meshed with the new woods.
            if (this.level.isClientSide()) {
                this.requestModelDataUpdate();
            }
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(MATERIALS, this.materials);
    }

    // --- save / load --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("materials", JumpMaterials.CODEC, this.materials);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.materials = input.read("materials", JumpMaterials.CODEC)
                .orElse(JumpMaterials.DEFAULT);
    }

    // --- the item form ------------------------------------------------------
    //
    // These three are what carry the two woods across the break-and-replace
    // round trip. The loot table copies the components out with
    // minecraft:copy_components (source: block_entity), which reads
    // collectImplicitComponents; placing the item back applies them through
    // applyImplicitComponents. Miss either half and a birch-railed jump comes
    // back oak, which reads as "the screen did not save".

    @Override
    protected void applyImplicitComponents(
            net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.materials = JumpMaterials.fromComponents(components);
    }

    @Override
    protected void collectImplicitComponents(
            net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        this.materials.writeTo(builder);
    }

    /**
     * Keep the two woods out of the item's <i>saved NBT</i> as well as in its
     * components.
     *
     * <p>Without this they are written twice - once as components and once in
     * the block-entity data the stack carries - and the copy in the NBT wins on
     * placement, so an edit made through the screen after the item was stamped
     * would be quietly undone. Vanilla's containers all do this for the same
     * reason.
     */
    @Override
    public void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("materials");
    }

    // --- sync ---------------------------------------------------------------

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Sent when the chunk is first sent to a client, as opposed to when the
     * block changes. Both paths are needed: without this one a jump is the
     * default wood until somebody edits it, which is the failure that only
     * shows up to a second player joining.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /**
     * The client has just been handed new data - re-mesh.
     *
     * <p>NeoForge routes both the update packet and the chunk's update tag
     * through here after loading, so this is the one place that has to ask for
     * the refresh on the receiving side.
     */
    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        this.requestModelDataUpdate();
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        this.requestModelDataUpdate();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(),
                    this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }
}
