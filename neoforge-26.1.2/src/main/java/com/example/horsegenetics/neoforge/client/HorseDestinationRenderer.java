package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.network.HorseDestinationsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;

import java.util.List;

/**
 * <b>The F8 highlight's destination lines.</b> Draws an arrow out of every
 * nearby horse to the place it is trying to walk to, coloured by whether it can
 * actually get there, with the goal's name floating above it.
 *
 * <h2>How to read it</h2>
 * <ul>
 *   <li><b>Green</b> - the horse has a path and the path reaches the target.
 *       Normal.</li>
 *   <li><b>Red</b> - the horse has a path and it <i>stops short</i>. The white
 *       box on the line is where the path actually ends. A row of horses with
 *       red arrows converging past a wall onto one crop is the bunching bug,
 *       drawn.</li>
 *   <li><b>Amber</b> - the horse wants that spot and has no path to it at all.</li>
 * </ul>
 * The box only appears when the path stops somewhere meaningfully short of the
 * target, so a healthy green arrow is not cluttered by a marker sitting on its
 * own arrowhead.
 *
 * <h2>Why this is a debug renderer and not a {@code RenderLevelStageEvent}</h2>
 * {@code DebugRenderer.emitGizmos} is called unconditionally from
 * {@code LevelRenderer.extractRenderState} - it is <b>not</b> gated on the F3
 * screen or on a development environment, only the individual vanilla renderers
 * inside it are. So a renderer registered through
 * {@link RegisterDebugRenderersEvent} runs in a real jar, which is the
 * requirement: the testers who need this are on production builds. It also
 * means the gate is entirely ours - nothing is sent unless the player has the
 * highlight on, so nothing is drawn.
 *
 * <p>Drawing goes through {@link Gizmos}, this version's debug-primitive API,
 * which handles the line pipeline, depth and billboarding. It may only be called
 * from inside a bound collector, which {@code emitGizmos} is.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class HorseDestinationRenderer implements DebugRenderer.SimpleDebugRenderer {

    /** Matches the highlight's own radius; past this the arrow is a pixel anyway. */
    private static final double MAX_DRAW_DISTANCE = 96.0;

    /** Below this the path's end and the target are the same place, so no marker. */
    private static final double STOP_MARKER_THRESHOLD = 1.5;

    private static final int GREEN = 0xFF44DD44;
    private static final int RED = 0xFFFF4444;
    private static final int AMBER = 0xFFFFAA22;
    private static final int WHITE = 0xFFEEEEEE;

    private static final float LABEL_SCALE = 0.28F;

    /**
     * UNVERIFIED IN GAME: the registration timing. {@code RegisterDebugRenderersEvent}
     * is posted from {@code DebugRenderer.refreshRendererList()}, which runs
     * from the {@code DebugRenderer} constructor and again whenever the F3
     * debug-entry set changes version. Mod construction is expected to precede
     * the first of those, but that ordering has not been watched happening. If
     * the lines never appear, this is the first thing to check - and the second
     * call site means toggling any F3 debug entry once would make them appear,
     * which is the symptom that would confirm it.
     */
    @SubscribeEvent
    static void onRegisterDebugRenderers(RegisterDebugRenderersEvent event) {
        event.register(new HorseDestinationRenderer());
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ,
                           DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        List<HorseDestinationsPayload.Entry> entries = ClientHorseDestinations.all();
        if (entries.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        // A debug overlay is not allowed to take the client down - the same rule
        // DebugHighlightHandler learned on the server, and this one runs inside
        // the render loop, where an escape is a hard crash rather than a lost
        // tick.
        try {
            for (HorseDestinationsPayload.Entry entry : entries) {
                draw(level, entry, camX, camY, camZ, partialTicks);
            }
        } catch (RuntimeException e) {
            ClientHorseDestinations.clear();
            HorseGenetics.LOGGER.error("[Debug] horse destination lines failed - cleared", e);
        }
    }

    private static void draw(ClientLevel level, HorseDestinationsPayload.Entry entry,
                             double camX, double camY, double camZ, float partialTicks) {
        Entity entity = level.getEntity(entry.entityId());
        if (!(entity instanceof Horse horse)) {
            return;
        }
        // Lerped, so the arrow's tail sits on the horse rather than a tick behind it.
        Vec3 from = horse.getPosition(partialTicks).add(0.0, horse.getBbHeight() * 0.8, 0.0);
        if (from.distanceToSqr(camX, camY, camZ) > MAX_DRAW_DISTANCE * MAX_DRAW_DISTANCE) {
            return;
        }
        Vec3 to = new Vec3(entry.tx(), entry.ty(), entry.tz());
        int colour = colour(entry.state());

        Gizmos.arrow(from, to, colour);
        Gizmos.cuboid(new AABB(to.x - 0.15, to.y - 0.15, to.z - 0.15,
                to.x + 0.15, to.y + 0.15, to.z + 0.15), GizmoStyle.fill(colour));

        // Where the path actually gives up, when that is not where it was aimed.
        if (entry.hasStop()) {
            Vec3 stop = new Vec3(entry.sx(), entry.sy(), entry.sz());
            if (stop.distanceTo(to) > STOP_MARKER_THRESHOLD) {
                Gizmos.cuboid(new AABB(stop.x - 0.2, stop.y - 0.2, stop.z - 0.2,
                                stop.x + 0.2, stop.y + 0.2, stop.z + 0.2),
                        GizmoStyle.stroke(WHITE, 2.0F));
            }
        }

        Gizmos.billboardTextOverMob(horse, 0, entry.label(), colour, LABEL_SCALE);
    }

    private static int colour(byte state) {
        return switch (state) {
            case HorseDestinationsPayload.STATE_REACHES -> GREEN;
            case HorseDestinationsPayload.STATE_BLOCKED -> RED;
            default -> AMBER;
        };
    }
}
