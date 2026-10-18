package com.example.horsegenetics.neoforge.client.crawl;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * <b>Dev-only: one frame of what the screen drew</b>, written down by
 * {@code mixin/GuiCaptureMixin} for the layout audit ({@link LayoutAudit}): every text with
 * its on-screen box, colour, scale and clip; every filled or textured box; in draw order.
 * It records only for the one frame the audit asks for.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class LayoutCapture {

    /** Read by the mixin on every draw call, on the render thread only; true for one frame at a time. */
    public static boolean recording;

    private static boolean pending;
    private static boolean ready;

    /** A screen-space rectangle. */
    record Rect(float x0, float y0, float x1, float y1) {
        float w() {
            return x1 - x0;
        }

        float h() {
            return y1 - y0;
        }

        @Nullable Rect intersect(Rect o) {
            float a = Math.max(x0, o.x0);
            float b = Math.max(y0, o.y0);
            float c = Math.min(x1, o.x1);
            float d = Math.min(y1, o.y1);
            return c > a && d > b ? new Rect(a, b, c, d) : null;
        }

        boolean contains(Rect o) {
            return o.x0 >= x0 - 0.01f && o.y0 >= y0 - 0.01f && o.x1 <= x1 + 0.01f && o.y1 <= y1 + 0.01f;
        }

        boolean contains(float x, float y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** A string drawn: what, where on screen, how big, what colour, under which clip, when. */
    record Text(String text, Rect box, float scale, int color, @Nullable Rect clip, int order) {
    }

    /** A box drawn: where, its colour (or textured), under which clip, when. */
    record Box(Rect box, int color, boolean textured, @Nullable Rect clip, int order) {
    }

    static final List<Text> texts = new ArrayList<>();
    static final List<Box> boxes = new ArrayList<>();
    private static final Deque<Rect> clips = new ArrayDeque<>();
    private static int order;

    private LayoutCapture() {
    }

    /** Ask for the next frame. */
    static void request() {
        pending = true;
        ready = false;
    }

    /** Has the frame asked for been drawn and written down? */
    static boolean ready() {
        return ready;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (pending) {
            pending = false;
            texts.clear();
            boxes.clear();
            clips.clear();
            order = 0;
            recording = true;
        }
    }

    /**
     * The screen's frame ends here: after the screen and every mod's overlay on it (the
     * cowboy's builder draws in Render.Post), before the toasts - an advancement sliding in
     * from off the edge is meant to be off the edge.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (recording) {
            recording = false;
            ready = true;
        }
    }

    /** A frame with no screen render (the screen went away) still ends the recording. */
    @SubscribeEvent
    static void onFrameEnd(RenderFrameEvent.Post event) {
        if (recording) {
            recording = false;
            ready = true;
        }
    }

    // ------------------------------------------------------------------ from the mixin

    public static void text(Font font, FormattedCharSequence str, int x, int y, int color, Matrix3x2f pose) {
        StringBuilder sb = new StringBuilder();
        str.accept((index, style, codepoint) -> {
            sb.appendCodePoint(codepoint);
            return true;
        });
        String s = sb.toString();
        if (s.isBlank()) {
            return;
        }
        Rect box = transform(x, y, x + font.width(str), y + font.lineHeight, pose);
        float scale = (float) Math.sqrt(pose.m00() * pose.m00() + pose.m01() * pose.m01());
        texts.add(new Text(s, box, scale, color, clips.peek(), order++));
    }

    public static void fill(int x0, int y0, int x1, int y1, int color, Matrix3x2f pose) {
        boxes.add(new Box(transform(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1), pose),
                color, false, clips.peek(), order++));
    }

    public static void blit(int x0, int y0, int x1, int y1, Matrix3x2f pose) {
        boxes.add(new Box(transform(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1), pose),
                0, true, clips.peek(), order++));
    }

    public static void scissorOn(int x0, int y0, int x1, int y1, Matrix3x2f pose) {
        Rect r = transform(x0, y0, x1, y1, pose);
        Rect outer = clips.peek();
        // Nested clips intersect, the way vanilla's ScissorStack does.
        Rect clip = outer == null ? r : outer.intersect(r);
        clips.push(clip == null ? new Rect(r.x0(), r.y0(), r.x0(), r.y0()) : clip);
    }

    public static void scissorOff() {
        if (!clips.isEmpty()) {
            clips.pop();
        }
    }

    private static Rect transform(float x0, float y0, float x1, float y1, Matrix3x2f pose) {
        Vector2f a = pose.transformPosition(new Vector2f(x0, y0));
        Vector2f b = pose.transformPosition(new Vector2f(x1, y1));
        return new Rect(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y));
    }
}
