package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * Vanilla's {@code HorseRenderer} is {@code final} in 26.1.2, so we extend
 * {@code AbstractHorseRenderer} directly and replicate its constructor.
 *
 * <p>Since the coat rework <b>every</b> horse - adult <i>and</i> foal - renders
 * a {@code GeneticCoatTextureFactory}-generated 128px texture, the adult on
 * {@link HdHorseModel} and the foal on {@link HdBabyHorseModel} (both 128px,
 * per-part UV). The models are handed straight to the super constructor as the
 * adult / baby model, so there's no per-entity model swap.
 *
 * <p><b>Both textures are resolved once, in {@link #extractRenderState}</b>,
 * and read back from the state - the coat by {@link #getTextureLocation}, the
 * glow mask by {@link EmissiveCoatLayer}. The factory rations new bakes and a
 * horse beyond {@code coats.detailDistance} may not start one; see
 * {@link GeneticCoatTextureFactory} for the freeze that made both necessary.
 */
public class GeneticHorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {

    public GeneticHorseRenderer(EntityRendererProvider.Context context) {
        super(context,
                new HdHorseModel(context.bakeLayer(ClientSetup.HD_HORSE)),
                new HdBabyHorseModel(context.bakeLayer(ClientSetup.HD_HORSE_BABY)));
        // NO HorseMarkingLayer: vanilla's white/roan marking overlays
        // (horse_markings_white.png etc.) would paint a big white patch over the
        // generated coat - a wild horse or foal that rolled Markings.WHITE then
        // renders as a flat white horse. All white markings in this mod come
        // from the splash gene inside the coat texture instead.
        //
        // The emissive layer, on the other hand, IS ours: it redraws a glow
        // gene's emissive coat regions (Suntouched's mane) at full brightness.
        this.addLayer(new EmissiveCoatLayer(this));
        // The cutie-mark emblem, drawn last so it sits on top of the coat and
        // every white pattern (a no-op unless the horse is Cutmrk/Cutmrk).
        this.addLayer(new CutieMarkLayer(this));
        // After the marks: a braid is worked into the hair and sits on top of
        // everything the coat did, the same way the emissive pass does.
        this.addLayer(new BraidLayer(this));
        // Grown parts - a unicorn's horn - after every pass that touches the coat
        // and before the tack. The position is a real choice and not an accident of
        // where the line was typed: a part is SEPARATE GEOMETRY rather than an
        // overlay, so it must not be drawn between a coat pass and the pass that
        // corrects it; and it goes before the armour and saddle layers because those
        // are vanilla's bakes of the whole horse and a part is a small thing sitting
        // proud of it. The dorsal spines were the part that could have made the
        // order matter, and it still holds: the layer hides the spines under a drawn
        // saddle or a rider (SaddleZone), so the saddle never has to be drawn over
        // one. A saddle pad, when something draws it, is the next case to check.
        this.addLayer(new AttachedPartLayer(this));
        this.addLayer(new HarnessLayer(this));
        this.addLayer(new PackLayer(this));
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem,
                new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)),
                null,
                2
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_SADDLE,
                // A phantom saddle is how a top-bond horse is steered bare;
                // it is not something the player put there and must not be
                // drawn. See ModDataComponents.PHANTOM_SADDLE.
                state -> state.saddle != null
                        && state.saddle.has(ModDataComponents.PHANTOM_SADDLE.get())
                        ? net.minecraft.world.item.ItemStack.EMPTY
                        : state.saddle,
                new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)),
                null,
                2
            )
        );
    }

    @Override
    public GeneticHorseRenderState createRenderState() {
        return new GeneticHorseRenderState();
    }

    @Override
    public void extractRenderState(Horse horse, HorseRenderState renderState, float partialTick) {
        // Timed for the world-exit summary (#205): the per-horse, per-frame cost
        // is the one number that says whether the coat-key caches paid. A bake
        // started inside this call is the bake budget's cost, not this method's,
        // so it is taken back out.
        long started = System.nanoTime();
        long bakedBefore = GeneticCoatTextureFactory.bakeNanos();
        extractTimed(horse, renderState, partialTick);
        GeneticCoatTextureFactory.noteExtract(System.nanoTime() - started
                - (GeneticCoatTextureFactory.bakeNanos() - bakedBefore));
    }

    /**
     * The breed label per horse, valid while {@link ClientHorseRecordCache} still hands
     * back the same record instance (#203). It feeds nothing but the dev-build
     * {@code [coat]} line written when a bake happens, yet it was computed every
     * frame for every horse - a {@code BreedLineage.parse} plus a
     * {@code Breeds.displayName} lookup. A record is immutable and replaced whole on
     * any change, so identity is a sound "unchanged" test. Render thread only.
     */
    private static final java.util.Map<Integer, LabelMemo> BREED_LABELS = new java.util.HashMap<>();

    private record LabelMemo(com.example.horsegenetics.common.horse.HorseRecord source, String label) {
    }

    private static String breedLabelOf(int entityId,
                                       com.example.horsegenetics.common.horse.HorseRecord rec) {
        if (rec == null) {
            return null;
        }
        LabelMemo memo = BREED_LABELS.get(entityId);
        if (memo != null && memo.source() == rec) {
            return memo.label();
        }
        if (BREED_LABELS.size() > 512) {
            BREED_LABELS.clear();
        }
        String label = rec.lineage().displayName();
        BREED_LABELS.put(entityId, new LabelMemo(rec, label));
        return label;
    }

    private void extractTimed(Horse horse, HorseRenderState renderState, float partialTick) {
        super.extractRenderState(horse, renderState, partialTick);
        stretchGaitToSize(renderState);
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            CoatData coatData = ClientCoatCache.get(horse.getId());
            if (coatData != null) {
                geneticState.coatData = coatData;
            }
            com.example.horsegenetics.common.horse.HorseRecord rec =
                    ClientHorseRecordCache.get(horse.getId());
            geneticState.breedLabel = breedLabelOf(horse.getId(), rec);
            GeneticCoatTextureFactory.Resolved textures = GeneticCoatTextureFactory.resolve(
                    geneticState.coatData, renderState.isBaby,
                    geneticState.breedLabel, withinDetailDistance(renderState));
            geneticState.coatId = textures.coat();
            geneticState.emissiveCoatId = textures.glow();
            // Gear is a synced attachment, so the client has the worn stacks
            // without a packet of this layer's own - see HorseGear.
            geneticState.braidMane = braidColour(horse,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.MANE);
            geneticState.braidTail = braidColour(horse,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.TAIL);
            // Once per frame per horse, and once only: every layer reads the
            // field rather than asking again, so that the coat, the glow, a
            // braid and the gear cannot disagree about how solid this horse is.
            geneticState.fadeAlpha = RiderFade.alphaFor(horse);
            net.minecraft.world.item.ItemStack harness =
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.HARNESS.on(horse);
            if (harness.getItem() instanceof com.example.horsegenetics.neoforge.item.StorageHarnessItem worn) {
                geneticState.harnessLeather = 0xFF000000 | (net.minecraft.world.item.component.DyedItemColor
                        .getOrDefault(harness, HarnessLayer.UNDYED) & 0x00FFFFFF);
                geneticState.harnessMetal = HarnessLayer.metal(worn.tier());
            } else {
                geneticState.harnessLeather = 0;
                geneticState.harnessMetal = 0;
            }
            // The chests on its flanks, as whatever items they are. NONE: no
            // display transform, so PackLayer places an untouched unit block.
            this.itemModelResolver.updateForLiving(geneticState.packLeft,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_LEFT.on(horse),
                    net.minecraft.world.item.ItemDisplayContext.NONE, horse);
            this.itemModelResolver.updateForLiving(geneticState.packRight,
                    com.example.horsegenetics.neoforge.entity.HorseTackSlot.SADDLEBAG_RIGHT.on(horse),
                    net.minecraft.world.item.ItemDisplayContext.NONE, horse);
            // The same discipline for grown parts: every layer reads the field.
            // Resolving them walks the genotype and the epigenome, and that walk
            // is memoised on the CoatData (#205), so it runs once per horse, not
            // once per frame - the field alone only stopped it running per layer.
            // The config switches are resolved here too, so a disabled part costs
            // one field test in the layer and not a config read per part.
            geneticState.parts = geneticState.coatData.grownParts();
            geneticState.drawParts = ClientConfig.parts()
                    && withinPartsDistance(renderState);
            geneticState.drawPartGlow = ClientConfig.partsGlow();
        }
    }

    /**
     * <b>A fading horse is drawn on a translucent pipeline, because a tint
     * alone would not fade it.</b>
     *
     * <p>The model's own render type is a <i>cutout</i>
     * ({@code RenderPipelines.ENTITY_CUTOUT_NO_CULL}), and a cutout pipeline
     * carries no {@code BlendFunction} at all - it discards a fragment below
     * an alpha of 0.1 and draws everything above it fully opaque. Feeding it a
     * half-transparent tint does not make a faint horse; it makes an ordinary
     * solid horse, and then at a low enough alpha a horse that vanishes
     * outright. {@code ENTITY_TRANSLUCENT} is the same shader with
     * {@code BlendFunction.TRANSLUCENT} on its colour target, and is already
     * no-cull, which is what a see-through animal wants anyway.
     *
     * <p>Only swapped while actually fading. Translucent geometry is sorted and
     * blended rather than depth-tested outright, so drawing every horse in the
     * world that way would cost something and change how they look for no
     * reason.
     */
    @Override
    protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(
            HorseRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        if (isBodyVisible && !forceTransparent
                && state instanceof GeneticHorseRenderState genetic && genetic.isFading()) {
            return net.minecraft.client.renderer.rendertype.RenderTypes
                    .entityTranslucent(this.getTextureLocation(state));
        }
        return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    /**
     * And the alpha itself. Vanilla multiplies this into the model colour
     * ({@code ARGB.multiply(baseColor, getModelTint(state))}), so a white tint
     * with a reduced alpha means "the same coat, fainter" - which is exactly
     * right for a generated texture whose colours are the whole point of it.
     */
    @Override
    protected int getModelTint(HorseRenderState state) {
        if (state instanceof GeneticHorseRenderState genetic && genetic.isFading()) {
            return RiderFade.tint(genetic.fadeAlpha);
        }
        return super.getModelTint(state);
    }

    /**
     * The opaque ARGB of the rescuing braid in this slot, or {@code 0} if there
     * is not one. Vanilla's {@code dyed_color} is the whole of a braid's
     * appearance - see {@code RescuingBraidItem} on why it is not the mod's own
     * three-zone tint.
     */
    private static int braidColour(Horse horse,
                                   com.example.horsegenetics.neoforge.entity.HorseTackSlot slot) {
        net.minecraft.world.item.ItemStack worn = slot.on(horse);
        if (!(worn.getItem() instanceof com.example.horsegenetics.neoforge.item.RescuingBraidItem)) {
            return 0;
        }
        return 0xFF000000 | (net.minecraft.world.item.component.DyedItemColor.getOrDefault(
                worn, BRAID_UNDYED) & 0x00FFFFFF);
    }

    /**
     * What an undyed braid is: the colour of horse hair, which is what one is
     * made of. Matches the {@code default} in the item's own model definition,
     * and the two are a pair - a braid that looked one colour in the hand and
     * another on the horse would read as a bug.
     */
    private static final int BRAID_UNDYED = 0xA05740;

    /**
     * <b>Close enough to be worth a coat of its own?</b> {@code distanceToCameraSq}
     * is written by vanilla's {@code EntityRenderer.extractRenderState}, which
     * the call to {@code super} above has already run (checked in bytecode,
     * 2026-09-13 - not assumed).
     *
     * <p>This only decides whether a horse may <i>start</i> a bake. One whose
     * coat already exists keeps it at any range, so walking back and forth
     * across the line swaps nothing and nothing flickers.
     */
    private static boolean withinDetailDistance(HorseRenderState renderState) {
        double blocks = ClientConfig.coatDetailDistance();
        return renderState.distanceToCameraSq <= blocks * blocks;
    }

    /**
     * <b>Close enough to draw the horn?</b> Same field and same reasoning as
     * {@link #withinDetailDistance}, on its own {@code parts.detailDistance} key
     * because the two are trading different things: a coat past the line wears a
     * stand-in, and a part past it is simply not drawn.
     *
     * <p>No hysteresis is needed here and there is a real difference behind that. A
     * coat keeps whatever it has been given at any range, because swapping a texture
     * back and forth across the line would flicker; a part that is not submitted
     * leaves nothing behind to flicker against, so it may appear and disappear on
     * the line itself.
     */
    private static boolean withinPartsDistance(HorseRenderState renderState) {
        double blocks = ClientConfig.partsDetailDistance();
        return renderState.distanceToCameraSq <= blocks * blocks;
    }

    /**
     * <b>Make a scaled horse take proportionally longer strides.</b> Without
     * this, a horse from the magical size locus walks with its feet sliding
     * along the ground.
     *
     * <p>Vanilla advances the leg-swing phase (<code>walkAnimationPos</code>)
     * from the <b>world distance the entity moved</b> and nothing else -
     * {@code LivingEntity.updateWalkAnimation} is {@code min(distance * 4, 1)}
     * fed into {@code walkAnimation.update(...)}. The only size compensation
     * anywhere in it is a hard-coded {@code isBaby() ? 3.0F : 1.0F}: a foal's
     * legs are short, so they cycle three times as fast for the same ground.
     * Nothing consults {@link net.minecraft.world.entity.ai.attributes.Attributes#SCALE},
     * because before this mod nothing changed it.
     *
     * <p>So a horse rendered at twice the size covers ground at its ordinary
     * speed while its legs - now twice as long - swing at the ordinary rate. Its
     * feet have to slide to keep up. The bigger the horse, the worse it looks,
     * and a tiny horse gets the mirror image: legs windmilling far faster than
     * the ground goes by.
     *
     * <p>Dividing the phase by the render scale is the whole fix. The phase is a
     * monotonic accumulator, so scaling it after the fact is identical to having
     * accumulated it at {@code 1/scale} the rate, and the scale is a constant of
     * the horse's genotype so the division never jumps mid-stride. Amplitude
     * (<code>walkAnimationSpeed</code>) is deliberately left alone: it is a 0-1
     * multiplier on an angle, and an angle already scales with the model.
     *
     * <p>Nothing happens at scale 1, which is every horse in a world where the
     * size locus is switched off.
     */
    private static void stretchGaitToSize(HorseRenderState renderState) {
        float scale = renderState.scale;
        if (scale > 0.0F && scale != 1.0F) {
            renderState.walkAnimationPos /= scale;
        }
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState renderState) {
        if (renderState instanceof GeneticHorseRenderState geneticState) {
            if (geneticState.coatId != null) {
                return geneticState.coatId;
            }
            return coatTextureFor(geneticState.coatData, renderState.isBaby, geneticState.breedLabel);
        }
        return coatTextureFor(CoatData.DEFAULT, renderState.isBaby, null);
    }

    /**
     * <b>Put a coat on a render state that did not come from a live horse.</b>
     * The one supported way for a GUI to draw a specific genome.
     *
     * <p>A screen builds its model horse as a throwaway client-side entity that
     * was never in the world, so {@link #extractRenderState} finds nothing for
     * it in {@link ClientCoatCache} and resolves {@link #coatId} from
     * {@link CoatData#DEFAULT} - the plain black wild type. Assigning
     * {@code coatData} afterwards then changes nothing, because
     * {@link #getTextureLocation} prefers the already-resolved {@code coatId}
     * and never looks at the coat again. That is how every ancestor in the
     * family tree came out the same black horse.
     *
     * <p>So the coat and the two texture ids have to move together, and they do
     * it here rather than in four screens that each got it right once and then
     * drifted. Baked at full detail - nothing on a screen is far away - but
     * still through the budget, so a pedigree of thirty strangers does not bake
     * thirty coats in one frame; the ones that have to wait show the stand-in
     * and are asked for again next frame.
     *
     * @param state a render state fresh from {@code createRenderState}; anything
     *              that is not one of ours is ignored
     * @param coat  the genome to wear, or {@code null} to leave the state alone
     */
    public static void applyCoat(EntityRenderState state, CoatData coat) {
        if (coat == null || !(state instanceof GeneticHorseRenderState geneticState)) {
            return;
        }
        geneticState.coatData = coat;
        GeneticCoatTextureFactory.Resolved textures = GeneticCoatTextureFactory.resolve(
                coat, geneticState.isBaby,
                geneticState.breedLabel, true);
        geneticState.coatId = textures.coat();
        geneticState.emissiveCoatId = textures.glow();
        // And the grown parts, for the same reason the two texture ids are here
        // rather than in four screens: a screen's model horse was never in the world,
        // so extractRenderState never ran for it. A pedigree of unicorns drawn
        // without this would be a wall of hornless horses, which is a lie about what
        // the genome says and exactly the bug the coat ids were moved here to stop.
        // Nothing on a screen is far away, so the distance gate does not apply.
        geneticState.parts = coat.grownParts();
        geneticState.drawParts = ClientConfig.parts();
        geneticState.drawPartGlow = ClientConfig.partsGlow();
    }

    /** The generated coat texture for one horse - shared with the family-tree node. */
    public static Identifier coatTextureFor(CoatData coatData, boolean baby) {
        return coatTextureFor(coatData, baby, null);
    }

    public static Identifier coatTextureFor(CoatData coatData, boolean baby, String breedLabel) {
        return GeneticCoatTextureFactory.getOrCreate(coatData, baby, breedLabel);
    }
}
