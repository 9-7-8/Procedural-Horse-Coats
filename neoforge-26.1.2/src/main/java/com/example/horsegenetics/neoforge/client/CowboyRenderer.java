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
 * <p>Assembled the way vanilla assembles a farmer - the villager-type skin
 * underneath, the trade's overlay on top ({@link CowboyHatLayer}) - rather than
 * as one flattened texture, so the cowboy and the horseman villager share a
 * single overlay file and cannot drift apart when it is repainted.
 *
 * <p>The geometry is the villager model on the wandering trader's baked layer,
 * which is the same mesh with the hat brim included.
 */
public class CowboyRenderer extends MobRenderer<Cowboy, VillagerRenderState, VillagerModel> {

    /** The body under the hat: an ordinary plains villager. */
    private static final Identifier BASE_SKIN =
            Identifier.withDefaultNamespace("textures/entity/villager/type/plains.png");

    public CowboyRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
        this.addLayer(new CowboyHatLayer(this));
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
