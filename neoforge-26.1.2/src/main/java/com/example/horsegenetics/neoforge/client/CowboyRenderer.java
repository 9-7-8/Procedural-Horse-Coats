package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link Cowboy}: a plains villager wearing the horseman's hat.
 *
 * <p>Assembled the way vanilla assembles a farmer - the bare villager skin
 * underneath, the biome robe and the trade's overlay on top
 * ({@link CowboyOverlayLayer}) - rather than as one flattened texture, so the
 * cowboy and the horseman villager share a single overlay file and cannot drift
 * apart when it is repainted.
 *
 * <p><b>{@code villager.png} is the base, and it has to be.</b> This used to
 * point at {@code textures/entity/villager/type/plains.png}, which sounds like
 * the plains villager's skin and is not: the type textures are <i>overlays</i>,
 * a robe with a hole where the head goes. Using one as the base skin drew a
 * cowboy with no face at all, and the hat overlay on top hid how the hole got
 * there.
 */
public class CowboyRenderer extends MobRenderer<Cowboy, VillagerRenderState, VillagerModel> {

    /** The body under the clothes - the bare villager, face included. */
    private static final Identifier BASE_SKIN =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public CowboyRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
        // The robe pass rides a second, head-less copy of the mesh, because the
        // horseman overlay is a `hat: full` profession - see CowboyOverlayLayer.
        this.addLayer(new CowboyOverlayLayer(
                this, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_NO_HAT))));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return BASE_SKIN;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(Cowboy entity, VillagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.isUnhappy = entity.getUnhappyCounter() > 0;
    }
}
