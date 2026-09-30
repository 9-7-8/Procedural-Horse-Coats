package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.List;

/**
 * <b>A baked attached-part mesh, ready to submit.</b> A tree of boxes with
 * <b>no animation of its own</b> - it is posed entirely by the pose stack the layer
 * hands it, because the bone it hangs from is already animated by the horse model.
 *
 * <h2>Why its state is a {@link Slice} and not the horse</h2>
 * {@code submitModel} is generic in the state it carries, and the state it
 * carries is the thing {@link #setupAnim} is called with <b>at draw time</b>. So
 * the type parameter is a free choice, and choosing something about the <i>part</i>
 * rather than the horse's render state is what makes a shared mesh safe.
 *
 * <p>That matters because <b>submits are deferred</b>. {@code submitModel} stores
 * a copy of the pose and a <i>reference</i> to this model, and the batch renderer
 * calls {@code setupAnim(state)} immediately before drawing each one. The obvious
 * way to vary a shared mesh per horse - set {@code visible} or {@code skipDraw} on
 * its parts and then submit - is therefore a trap: the second horse in a frame
 * overwrites the first horse's mesh before either is drawn, and the bug is one
 * horse wearing another's horn. Anything that must be applied by mutation is
 * applied in {@link #setupAnim}, where it runs per submit with that submit's own
 * state.
 *
 * <h2>The first thing that needed it: a two-tone horn</h2>
 * A submit carries one tint, and a two-tone horn needs a different colour on every
 * segment. So such a horn is submitted once per segment, each carrying a
 * {@link Slice} naming the one segment to draw, and {@link #setupAnim} sets
 * {@code skipDraw} on every other box. {@code skipDraw} rather than
 * {@code visible}: a segment is its predecessor's child, and {@code visible=false}
 * would hide everything above it too, where {@code skipDraw} skips the box and
 * still walks its children. A one-colour horn is submitted once with
 * {@link Slice#ALL}. <i>Unverified in game: that setupAnim runs per submit and
 * immediately before its draw is read from the 26.1.2 sources, not watched.</i>
 */
public final class PartModel extends Model<PartModel.Slice> {

    /** Which of the mesh's segments a submit draws. */
    public record Slice(int segment) {
        /** Every segment - a one-colour part. */
        public static final Slice ALL = new Slice(-1);
    }

    private final List<ModelPart> segments;

    /**
     * @param segments the mesh's boxes in generator order, so segment {@code i} is
     *                 the {@code i}th {@code PartNode} - root to tip for a horn
     */
    PartModel(ModelPart root, List<ModelPart> segments) {
        // The render type is a fallback only: every submit names its own, because
        // the same mesh is drawn opaque, translucent (a fading ridden horse) or
        // full-bright (a glowing horn) depending on the horse, not the mesh.
        super(root, RenderTypes::entityCutout);
        this.segments = List.copyOf(segments);
    }

    /** How many segments there are to slice - the layer's loop bound. */
    public int segmentCount() {
        return segments.size();
    }

    /** Every box drawn, or only the one the slice names. Runs per submit, at draw time. */
    @Override
    public void setupAnim(Slice slice) {
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).skipDraw = slice.segment() >= 0 && slice.segment() != i;
        }
    }
}
