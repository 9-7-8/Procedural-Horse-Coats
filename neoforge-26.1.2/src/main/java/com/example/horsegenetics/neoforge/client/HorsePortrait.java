package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * <b>One horse, drawn small.</b> The row portrait the horse browser's tables
 * use, in the coat the horse really has.
 *
 * <h2>Two throwaway horses for the whole screen</h2>
 * A render state is built per draw and the coat is pushed into it, so the
 * {@code Horse} itself is nothing but a shape to hang that on - which means one
 * adult and one foal serve a table of two hundred rows. That is the difference
 * that matters at table scale: {@code FamilyTreeScreen} keeps a
 * {@code Horse} per record, which is right for seven boxes and would be two
 * hundred entities here.
 *
 * <p>They are client-only entities that are never added to a level, exactly as
 * the family tree's are, and they are dropped with the rest of the browser's
 * caches on disconnect.
 *
 * <h2>What it draws when it cannot draw a horse</h2>
 * A flat square of the coat texture, which is the same fallback the family tree
 * uses: the coat is the information, the model is the presentation, and losing
 * the second is not a reason to lose the first. With no coat at all - the
 * epigenome has not arrived yet - it draws an empty well, so a row does not jump
 * when the answer lands.
 */
public final class HorsePortrait {

    /** Eyeballed against {@code FamilyTreeScreen}: the horse model is big. */
    private static final float MODEL_SCALE_PER_PIXEL = 16.0F / 78.0F;

    private static final int WELL = 0xFF101018;

    private static Horse adult;
    private static Horse foal;

    private HorsePortrait() {
    }

    /**
     * Draw the horse in {@code coat} inside the box at {@code (x, y)}, facing
     * the cursor. {@code coat} may be null.
     */
    public static void draw(GuiGraphicsExtractor g, CoatData coat, boolean baby,
                            int x, int y, int w, int h, int mouseX, int mouseY) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        float yawDeg = (float) Math.atan((cx - mouseX) / 30.0F) * 42.0F;
        float pitchDeg = (float) Math.atan((cy - mouseY) / 30.0F) * 22.0F;
        drawPosed(g, coat, baby, x, y, w, h, yawDeg, pitchDeg);
    }

    /**
     * The same horse at an angle the caller picks rather than one the pointer
     * picks. The Gear tab's paper doll needs this: its slots are positioned
     * anatomically, so the horse under them has to hold still - a portrait that
     * swings to follow the cursor would put the near fore leg somewhere
     * different from the boot slot that belongs to it.
     *
     * @param yawDeg   0 faces the viewer; around 50 is a three-quarter view,
     *                 which is the one that separates all four legs.
     * @param pitchDeg positive tips the horse's nose down toward the viewer.
     */
    public static void drawPosed(GuiGraphicsExtractor g, CoatData coat, boolean baby,
                                 int x, int y, int w, int h, float yawDeg, float pitchDeg) {
        g.fill(x, y, x + w, y + h, WELL);
        if (coat == null) {
            return;
        }
        Horse horse = model(baby);
        if (horse == null || !drawModel(g, horse, coat, x, y, w, h, yawDeg, pitchDeg)) {
            drawSwatch(g, coat, x, y, w, h);
        }
    }

    /** @return false when the render state could not be built, so the caller falls back. */
    private static boolean drawModel(GuiGraphicsExtractor g, Horse horse, CoatData coat,
                                     int x, int y, int w, int h, float yawDeg, float pitchDeg) {
        try {
            EntityRenderer<? super Horse, ?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse);
            EntityRenderState state = renderer.createRenderState(horse, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            // Coat and texture ids together - see GeneticHorseRenderer.applyCoat.
            // Setting gs.coatData alone silently draws the default black horse.
            GeneticHorseRenderer.applyCoat(state, coat);
            if (state instanceof LivingEntityRenderState ls) {
                // The horse's own SCALE attribute is thrown away here on
                // purpose: a table row is a fixed box, and a draught horse
                // drawn twice the size of a pony would break the grid rather
                // than tell the reader something the Size column does not.
                ls.bodyRot = 180.0F + yawDeg;
                ls.yRot = yawDeg;
                ls.xRot = -pitchDeg;
                ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
                ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
                ls.scale = 1.0F;
            }
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(pitchDeg * ((float) Math.PI / 180.0F));
            rotation.mul(xRotation);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            g.entity(state, h * MODEL_SCALE_PER_PIXEL, translation, rotation, xRotation,
                    x, y, x + w, y + h);
            return true;
        } catch (RuntimeException unrenderable) {
            return false;
        }
    }

    private static void drawSwatch(GuiGraphicsExtractor g, CoatData coat, int x, int y, int w, int h) {
        try {
            Identifier texture = GeneticHorseRenderer.coatTextureFor(coat, false);
            g.blit(texture, x, y, x + w, y + h, 0.0f, 1.0f, 0.0f, 1.0f);
        } catch (RuntimeException unavailable) {
            // The well already drawn is the answer.
        }
    }

    private static Horse model(boolean baby) {
        Horse existing = baby ? foal : adult;
        if (existing != null) {
            return existing;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        try {
            Horse h = EntityType.HORSE.create(mc.level, EntitySpawnReason.LOAD);
            if (h == null) {
                return null;
            }
            h.setBaby(baby);
            if (baby) {
                foal = h;
            } else {
                adult = h;
            }
            return h;
        } catch (RuntimeException uncreatable) {
            return null;
        }
    }

    /** Dropped on disconnect - the two horses belong to that world's client level. */
    public static void clear() {
        adult = null;
        foal = null;
    }
}
