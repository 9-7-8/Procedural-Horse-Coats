package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.parts.AttachedPart;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Adds our {@link CoatData} onto vanilla's per-frame horse render state
 * (populated in {@code GeneticHorseRenderer.extractRenderState}, read back in
 * {@code getTextureLocation}). The coat carries the full genotype + epigenetic
 * seed, which is everything {@code GeneticCoatTextureFactory} needs.
 */
public class GeneticHorseRenderState extends HorseRenderState {

    /** Defaults to a plain black horse so an un-extracted state never NPEs. */
    public CoatData coatData = CoatData.DEFAULT;

    /**
     * The horse's breed label ({@code BreedLineage.displayName()}), or
     * {@code null} if no record is cached yet. Purely for the dev-build
     * {@code [coat]} chat line - the baked texture is keyed by genome, not by
     * breed.
     */
    public String breedLabel = null;

    /**
     * The coat texture resolved for this frame: the horse's own once it has been
     * baked, or the shared stand-in while it waits its turn or is beyond
     * {@code coats.detailDistance}. Filled in {@code extractRenderState}; still
     * {@code null} only on a state that renderer never extracted.
     */
    public Identifier coatId = null;

    /**
     * Full-bright mask texture for a {@code glow} gene's emissive coat regions,
     * or {@code null} when no expressed gene wants one. Drawn by
     * {@link EmissiveCoatLayer} on top of the base coat.
     */
    public Identifier emissiveCoatId = null;

    /**
     * The colour of the <b>rescuing braid</b> worn in the mane, as opaque ARGB,
     * or {@code 0} for none. Drawn by {@link BraidLayer}.
     *
     * <p>Zero rather than {@code -1} or a nullable Integer because it is read
     * every frame for every horse on screen and the overwhelmingly common
     * answer is "nothing is worn": a plain int field compared against zero costs
     * nothing, and a fully transparent colour is not a braid anybody could see
     * anyway.
     */
    public int braidMane = 0;

    /** The same for the tail. The two slots are independent - see the handler. */
    public int braidTail = 0;

    /**
     * <b>The geometry this horse's genes grew</b> - a unicorn horn, and in time
     * antlers, spines or crystals. Drawn by {@link AttachedPartLayer}. Empty for
     * all but a few horses in a thousand, and {@link java.util.List#of()} is a
     * shared immutable instance, so the common case costs nothing to carry.
     *
     * <p>Resolved in {@code extractRenderState} rather than in the layer.
     * {@code CutieMarkLayer} asks the genotype afresh on every submit, which folds
     * over the whole registry each time; that is affordable for one emblem and
     * wrong for a pen of twenty horses, so this is read once per frame per horse
     * and the layer only walks the list.
     */
    public List<AttachedPart> parts = List.of();

    /**
     * <b>Whether grown parts are drawn at all</b> - {@code parts.enabled}, resolved
     * once per horse rather than read inside the per-part loop.
     *
     * <p>It gates a <i>submit</i> and nothing else. A horse with a horn and horns
     * switched off still has the horn: it breeds it, drifts it, shows it in the gene
     * list and gets it back the moment the switch does. That is the rule the whole
     * {@code parts.*} block lives under, and it is what makes it safe for two players
     * on one server to disagree.
     */
    public boolean drawParts = true;

    /** The same for the second, full-bright pass - {@code parts.glow}. */
    public boolean drawPartGlow = true;

    /**
     * <b>How solid to draw this horse this frame</b>, 1.0 for all but the one
     * the camera's own player is sitting on. Computed by {@link RiderFade} in
     * {@code extractRenderState}.
     *
     * <p>It lives on the render state rather than being asked for at each draw
     * because <b>everything that draws a piece of this horse has to agree</b> -
     * the coat, the emissive glow, a braid, the saddle and the barding. A value
     * read twice in one frame could differ across a config reload or a camera
     * move, and the visible result would be a saddle at one opacity floating
     * over a horse at another.
     *
     * <p>It is also how {@code mixin/FadingGearMixin} knows to fade at all: the
     * render state is the only thing that reaches vanilla's equipment renderer,
     * so the mixin guards on this type and this field rather than on any state
     * of its own.
     */
    public float fadeAlpha = RiderFade.OPAQUE;

    /**
     * The chests on the near and off flanks, resolved to their item models -
     * empty when nothing hangs there. See {@link PackLayer}.
     */
    public final net.minecraft.client.renderer.item.ItemStackRenderState packLeft =
            new net.minecraft.client.renderer.item.ItemStackRenderState();
    public final net.minecraft.client.renderer.item.ItemStackRenderState packRight =
            new net.minecraft.client.renderer.item.ItemStackRenderState();

    /** Whether anything about this horse should be drawn see-through at all. */
    public boolean isFading() {
        return fadeAlpha < RiderFade.OPAQUE;
    }
}
