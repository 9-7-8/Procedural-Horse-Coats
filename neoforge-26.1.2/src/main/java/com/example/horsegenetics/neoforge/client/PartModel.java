package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.parts.AttachedPart;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * <b>A baked attached-part mesh, ready to submit.</b> A tree of boxes with
 * <b>no animation and no state of its own</b> - it is posed entirely by the
 * pose stack the layer hands it, because the bone it hangs from is already
 * animated by the horse model.
 *
 * <h2>Why it is a {@code Model<AttachedPart>} and not an {@code EntityModel}</h2>
 * {@code submitModel} is generic in the state it carries, and the state it
 * carries is the thing {@link #setupAnim} is called with <b>at draw time</b>. So
 * the type parameter is a free choice, and choosing {@link AttachedPart} rather
 * than the horse's render state is what makes a shared mesh safe.
 *
 * <p>That matters because <b>submits are deferred</b>. {@code submitModel} stores
 * a copy of the pose and a <i>reference</i> to this model, and the batch renderer
 * calls {@code setupAnim(state)} immediately before drawing each one. The obvious
 * way to vary a shared mesh per horse - set {@code visible} or {@code xScale} on
 * its parts and then submit - is therefore a trap: the second horse in a frame
 * overwrites the first horse's mesh before either is drawn, and the bug is one
 * horse wearing another's horn. Keying the state to the <i>part</i> means anything
 * that must be applied by mutation can be applied in {@link #setupAnim}, where it
 * runs per submit with that submit's own numbers.
 *
 * <p>Nothing needs that yet: a horn's length, girth, lean, colour and glow are all
 * a pose-stack transform or a submit argument, and a transform is copied into the
 * submit node rather than shared. {@link #setupAnim} is consequently empty, and
 * <b>that is the interesting thing about this class</b> - it is where a per-horse
 * box count would have to go when antlers arrive and their tine count has to be a
 * visibility toggle.
 */
public final class PartModel extends Model<AttachedPart> {

    PartModel(ModelPart root) {
        // The render type is a fallback only: every submit names its own, because
        // the same mesh is drawn opaque, translucent (a fading ridden horse) or
        // full-bright (a glowing horn) depending on the horse, not the mesh.
        super(root, RenderTypes::entityCutout);
    }

    /**
     * <b>Deliberately nothing.</b> The baked pose <i>is</i> the shape - see the
     * class note on why this being empty is a decision rather than a stub, and on
     * what has to go in here rather than at the submit if a part ever varies by
     * mutation.
     */
    @Override
    public void setupAnim(AttachedPart part) {
        // no-op: nothing about this mesh depends on the horse wearing it
    }
}
