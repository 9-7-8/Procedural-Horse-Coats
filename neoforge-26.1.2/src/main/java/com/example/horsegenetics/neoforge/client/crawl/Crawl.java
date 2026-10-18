package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <b>Dev-only: the menu crawler's toolkit</b> - a queue of steps run over client ticks, the
 * click itself, and the reads a check needs. {@link MenuCrawlClient} drives it and
 * {@link MenuCrawlCases} is what it runs.
 *
 * <h2>A click is a click</h2>
 * Every press goes through {@code screen.mouseClicked} and {@code mouseReleased} at screen
 * coordinates, never through a widget's {@code onPress}. The bug that made the action trace
 * ("Make egg does nothing", 2026-09-12) was a widget sitting on top of a button and eating
 * the press; a test that called the handler directly would have passed it. So a custom
 * region (a tab, a row, a chip) is found by asking the screen's own private hit-test where it
 * is ({@link #scan}), and then clicked like any other point - the screen's own dispatch order
 * decides what the press reaches.
 *
 * <h2>Reflection, and why that is fine here</h2>
 * The screens keep their state private, and growing test accessors on sixteen of them would
 * be more code than the crawler. Fields and methods are read by name. A rename makes the
 * check that reads it FAIL with "no field x on Y" - loud, and one edit to mend - rather than
 * pass on a value it could not see.
 */
final class Crawl {

    private Crawl() {
    }

    // ------------------------------------------------------------------ results

    static int passed;
    static int failed;
    static int skipped;
    static final List<String> failures = new ArrayList<>();

    static void pass(String where, String what) {
        passed++;
        HorseGenetics.LOGGER.info("[crawl] PASS {} | {}", where, what);
    }

    static void fail(String where, String what) {
        failed++;
        failures.add(where + " | " + what);
        HorseGenetics.LOGGER.warn("[crawl] FAIL {} | {}", where, what);
    }

    static void skip(String where, String why) {
        skipped++;
        HorseGenetics.LOGGER.info("[crawl] SKIP {} | {}", where, why);
    }

    /** {@code null} is a pass; anything else is the reason it failed. */
    static void check(String where, String what, @Nullable String problem) {
        if (problem == null) {
            pass(where, what);
        } else {
            fail(where, what + " - " + problem);
        }
    }

    // ------------------------------------------------------------------ steps

    /**
     * One thing to do on the client thread. It returns how many ticks to wait before the
     * next step runs (0 = straight on, in the same tick), or {@link #AGAIN} to be called
     * again next tick (a wait for a condition).
     */
    interface Step {
        int run();
    }

    static final int AGAIN = -1;

    static final Deque<Step> queue = new ArrayDeque<>();
    private static int wait;
    private static int againFor;

    /** Run one tick's worth of steps. */
    static void tick() {
        if (wait > 0) {
            wait--;
            return;
        }
        int budget = 200;
        while (!queue.isEmpty() && budget-- > 0) {
            // Taken off before it runs, so a step may drop the ones after it (skipUntil).
            Step step = queue.pollFirst();
            int r;
            try {
                r = step.run();
            } catch (Throwable t) {
                fail("crawler", "a step threw " + t);
                HorseGenetics.LOGGER.warn("[crawl] the step that threw", t);
                againFor = 0;
                continue;
            }
            if (r == AGAIN) {
                queue.addFirst(step);
                againFor++;
                return;
            }
            againFor = 0;
            if (r > 0) {
                wait = r;
                return;
            }
        }
    }

    /** How many ticks the step at the head has been waiting - for a {@link #until} timeout. */
    static int waited() {
        return againFor;
    }

    static boolean idle() {
        return queue.isEmpty();
    }

    // ------------------------------------------------------------------ the screen

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    static @Nullable Screen screen() {
        return mc().screen;
    }

    static IntegratedServer server() {
        return Objects.requireNonNull(mc().getSingleplayerServer(), "the crawl runs in singleplayer only");
    }

    /** Run {@code job} on the server thread; the future completes there. */
    static <T> CompletableFuture<T> onServer(Function<IntegratedServer, T> job) {
        IntegratedServer server = server();
        return server.submit(() -> job.apply(server));
    }

    static MouseButtonEvent event(double x, double y, int button, int modifiers) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
    }

    /** Press and release the left button at (x, y), as the mouse would. */
    static void click(double x, double y) {
        click(x, y, 0, 0);
    }

    static void click(double x, double y, int button, int modifiers) {
        Screen s = Objects.requireNonNull(screen(), "no screen to click");
        MouseButtonEvent e = event(x, y, button, modifiers);
        // The way MouseHandler.onButton hands a press to a screen: NeoForge's Pre event,
        // the screen, then Post. Mods hang click handlers off those events (the cowboy's
        // saved-search menu does), so calling mouseClicked alone would miss them.
        if (!net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPre(s, e, false)) {
            boolean handled = s.mouseClicked(e, false);
            net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPost(s, e, false, handled);
        }
        // The press may have replaced the screen; the release goes to whatever is up now,
        // as it would for a real mouse.
        Screen after = screen();
        if (after != null && !net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPre(after, e)) {
            boolean handled = after.mouseReleased(e);
            net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPost(after, e, handled);
        }
    }

    /** Type {@code text} into whatever has the keyboard, a character at a time, as the keyboard would. */
    static void type(String text) {
        text.codePoints().forEach(cp -> {
            Screen s = Objects.requireNonNull(screen(), "no screen to type on");
            var e = new net.minecraft.client.input.CharacterEvent(cp);
            if (!net.neoforged.neoforge.client.ClientHooks.onScreenCharTypedPre(s, e)) {
                s.charTyped(e);
                net.neoforged.neoforge.client.ClientHooks.onScreenCharTypedPost(s, e);
            }
        });
    }

    /** Press and let go of one key (a GLFW key code), as the keyboard would. */
    static void key(int glfwKey) {
        Screen s = Objects.requireNonNull(screen(), "no screen for a key");
        var e = new net.minecraft.client.input.KeyEvent(glfwKey, 0, 0);
        if (!net.neoforged.neoforge.client.ClientHooks.onScreenKeyPressedPre(s, e)) {
            boolean handled = s.keyPressed(e);
            if (!handled) {
                net.neoforged.neoforge.client.ClientHooks.onScreenKeyPressedPost(s, e);
            }
        }
    }

    /**
     * Turn the wheel at (x, y), as MouseHandler.onScroll would: NeoForge's MouseScrolled.Pre
     * event, the screen, then Post. Its own hook reads the position off the physical mouse,
     * so the event is posted here with the crawler's point instead. Mods scroll their
     * overlays off that event (the cowboy's builder does); mouseScrolled alone misses them.
     */
    static void wheel(Screen s, double x, double y, double dx, double dy) {
        var pre = new net.neoforged.neoforge.client.event.ScreenEvent.MouseScrolled.Pre(s, x, y, dx, dy);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(pre).isCanceled()) {
            return;
        }
        if (!s.mouseScrolled(x, y, dx, dy)) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.client.event.ScreenEvent.MouseScrolled.Post(s, x, y, dx, dy));
        }
    }

    static final int ENTER = 257;
    static final int ESCAPE = 256;

    /** Press at (x, y), drag by (dx, dy), release there - a turn of a portrait, a pan. */
    static void drag(double x, double y, double dx, double dy) {
        Screen s = Objects.requireNonNull(screen(), "no screen to drag on");
        s.mouseClicked(event(x, y, 0, 0), false);
        s.mouseDragged(event(x + dx, y + dy, 0, 0), dx, dy);
        s.mouseReleased(event(x + dx, y + dy, 0, 0));
    }

    // ------------------------------------------------------------------ widgets

    static String label(AbstractWidget w) {
        // An edit box's message is its narration title, not anything on screen.
        return w instanceof EditBox ? "<edit box>" : w.getMessage().getString();
    }

    /** Every widget on the screen (a field-less button is found here, not by name). */
    static List<AbstractWidget> widgets(Screen s) {
        List<AbstractWidget> out = new ArrayList<>();
        for (GuiEventListener l : s.children()) {
            if (l instanceof AbstractWidget w) {
                out.add(w);
            }
        }
        return out;
    }

    static List<AbstractWidget> visibleWidgets(Screen s) {
        List<AbstractWidget> out = new ArrayList<>();
        for (AbstractWidget w : widgets(s)) {
            if (w.visible) {
                out.add(w);
            }
        }
        return out;
    }

    /** The first visible widget whose label starts with {@code prefix}, or null. */
    static @Nullable AbstractWidget widget(Screen s, String prefix) {
        for (AbstractWidget w : visibleWidgets(s)) {
            if (label(w).startsWith(prefix)) {
                return w;
            }
        }
        return null;
    }

    static double cx(AbstractWidget w) {
        return w.getX() + w.getWidth() / 2.0;
    }

    static double cy(AbstractWidget w) {
        return w.getY() + w.getHeight() / 2.0;
    }

    /**
     * <b>Is the widget the thing a press at its centre reaches?</b> Null if it is; otherwise
     * what is on top of it. Vanilla dispatch gives the press to the first child under the
     * point ({@code ContainerEventHandler.getChildAt}), so a widget added earlier and
     * overlapping takes it. This is the "Make egg does nothing" check.
     */
    static @Nullable String covered(Screen s, AbstractWidget w) {
        double x = cx(w);
        double y = cy(w);
        GuiEventListener top = s.getChildAt(x, y).orElse(null);
        if (top == w) {
            return null;
        }
        if (top == null) {
            return "nothing takes a press at its centre (" + (int) x + "," + (int) y + ")";
        }
        return "a press at its centre goes to "
                + (top instanceof AbstractWidget aw ? "\"" + label(aw) + "\"" : top.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------ custom regions

    /**
     * <b>Where on screen does the screen's own hit-test say {@code want}?</b> Walks the
     * screen in 2-px steps asking {@code hit(x, y)} and returns the centre of the matching
     * points, or null. Using the screen's own geometry means the crawler never copies a
     * layout constant - a layout change moves the point, not the test.
     */
    static double @Nullable [] scan(Screen s, BiFunction<Double, Double, Object> hit, Predicate<Object> want) {
        int[] box = scanBox(s, hit, want);
        return box == null ? null : new double[] {(box[0] + box[2]) / 2.0, (box[1] + box[3]) / 2.0};
    }

    /**
     * {@link #scan}'s bounding box {x0, y0, x1, y1}, for a region whose middle belongs to
     * something else (a table row, whose middle is its Send home button).
     */
    static int @Nullable [] scanBox(Screen s, BiFunction<Double, Double, Object> hit, Predicate<Object> want) {
        double sx = 0;
        double sy = 0;
        int n = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int y = 0; y < s.height; y += 2) {
            for (int x = 0; x < s.width; x += 2) {
                Object got = hit.apply((double) x, (double) y);
                if (got != null && want.test(got)) {
                    n++;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (n == 0) {
            return null;
        }
        // The box, not the points' mean: a region is a rectangle, and the mean of a clipped
        // one drifts toward its bigger half.
        return new int[] {minX, minY, maxX, maxY};
    }

    /** A private hit-test method of the screen, as a function {@link #scan} can call. */
    static BiFunction<Double, Double, Object> hitTest(Object owner, String method, Object... extra) {
        return (x, y) -> {
            Object[] args = new Object[2 + extra.length];
            args[0] = x;
            args[1] = y;
            System.arraycopy(extra, 0, args, 2, extra.length);
            Object r = call(owner, method, args);
            if (r instanceof Integer i && i < 0) {
                return null;
            }
            if (r instanceof Boolean b && !b) {
                return null;
            }
            return r;
        };
    }

    /** The centre of a record with int components x0, y0, x1, y1 (the browser's hit boxes). */
    static double[] centreOfBox(Object box) {
        int x0 = (Integer) call(box, "x0");
        int y0 = (Integer) call(box, "y0");
        int x1 = (Integer) call(box, "x1");
        int y1 = (Integer) call(box, "y1");
        return new double[] {(x0 + x1) / 2.0, (y0 + y1) / 2.0};
    }

    // ------------------------------------------------------------------ container slots

    /**
     * <b>Every active slot is inside the window, and no two overlap.</b> Null if so. A slot
     * placed off the window can never be clicked (the wagon's, #260), and two on one spot
     * mean one of them never can.
     */
    static @Nullable String slotLayout(AbstractContainerScreen<?> s) {
        int w = (Integer) get(s, "imageWidth");
        int h = (Integer) get(s, "imageHeight");
        List<Slot> active = new ArrayList<>();
        for (Slot slot : s.getMenu().slots) {
            if (slot.isActive()) {
                active.add(slot);
            }
        }
        List<String> outside = new ArrayList<>();
        for (Slot slot : active) {
            if (slot.x < 0 || slot.y < 0 || slot.x + 16 > w || slot.y + 16 > h) {
                outside.add(slot.index + "@" + slot.x + "," + slot.y);
            }
        }
        List<String> overlaps = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                Slot a = active.get(i);
                Slot b = active.get(j);
                if (Math.abs(a.x - b.x) < 16 && Math.abs(a.y - b.y) < 16) {
                    overlaps.add(a.index + "/" + b.index);
                }
            }
        }
        if (outside.isEmpty() && overlaps.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!outside.isEmpty()) {
            sb.append(outside.size()).append(" slot(s) outside the ").append(w).append("x").append(h)
                    .append(" window: ").append(head(outside));
        }
        if (!overlaps.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(overlaps.size()).append(" overlapping pair(s): ").append(head(overlaps));
        }
        return sb.toString();
    }

    private static String head(List<String> items) {
        return items.size() <= 6 ? items.toString() : items.subList(0, 6) + " ...";
    }

    /** The screen-space centre of a menu slot. */
    static double[] slotCentre(AbstractContainerScreen<?> s, Slot slot) {
        return new double[] {left(s) + slot.x + 8, top(s) + slot.y + 8};
    }

    /** The window's corner. Read off the fields: NeoForge's getGuiLeft() is marked for removal. */
    static int left(AbstractContainerScreen<?> s) {
        return (Integer) get(s, "leftPos");
    }

    static int top(AbstractContainerScreen<?> s) {
        return (Integer) get(s, "topPos");
    }

    // ------------------------------------------------------------------ reflection

    static Object get(Object owner, String name) {
        Field f = field(owner instanceof Class<?> c ? c : owner.getClass(), name);
        try {
            return f.get(owner instanceof Class<?> ? null : owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static void set(Object owner, String name, @Nullable Object value) {
        Field f = field(owner instanceof Class<?> c ? c : owner.getClass(), name);
        try {
            f.set(owner instanceof Class<?> ? null : owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // up a level
            }
        }
        throw new IllegalStateException("no field " + name + " on " + type.getSimpleName());
    }

    /** Call a (private) method by name with these arguments; boxed primitives match their types. */
    static Object call(Object owner, String name, Object... args) {
        Class<?> type = owner instanceof Class<?> c ? c : owner.getClass();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length || !fits(m, args)) {
                    continue;
                }
                m.setAccessible(true);
                try {
                    return m.invoke(owner instanceof Class<?> ? null : owner, args);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(name + " threw " + e.getCause(), e);
                }
            }
        }
        throw new IllegalStateException("no method " + name + "/" + args.length + " on " + type.getSimpleName());
    }

    private static boolean fits(Method m, Object[] args) {
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            if (args[i] == null) {
                if (p[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> want = box(p[i]);
            if (!want.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) {
            return c;
        }
        if (c == int.class) {
            return Integer.class;
        }
        if (c == double.class) {
            return Double.class;
        }
        if (c == boolean.class) {
            return Boolean.class;
        }
        if (c == float.class) {
            return Float.class;
        }
        if (c == long.class) {
            return Long.class;
        }
        return c;
    }

    /** An enum constant of a (private) nested enum, by its owner and simple names. */
    static Object enumConstant(Class<?> owner, String enumName, String constant) {
        for (Class<?> c : owner.getDeclaredClasses()) {
            if (c.getSimpleName().equals(enumName) && c.isEnum()) {
                for (Object o : c.getEnumConstants()) {
                    if (((Enum<?>) o).name().equals(constant)) {
                        return o;
                    }
                }
            }
        }
        throw new IllegalStateException("no " + enumName + "." + constant + " on " + owner.getSimpleName());
    }

    static String name(@Nullable Object o) {
        return o instanceof Enum<?> e ? e.name() : String.valueOf(o);
    }

    // ------------------------------------------------------------------ step builders

    /** A place in the queue that {@link #skipUnless} can skip to. */
    static Step marker() {
        Step m = () -> 0;
        return m;
    }

    /**
     * If {@code ok} is false when this step's turn comes, drop every step after it up to
     * and including {@code until} - the rest of a case that cannot mean anything now (a
     * query that could not be built has nothing to save or apply), logged as one SKIP
     * rather than a cascade of FAILs.
     */
    static void skipUnless(java.util.function.BooleanSupplier ok, Step until, String where, String why) {
        queue.add(() -> {
            if (!ok.getAsBoolean()) {
                skip(where, why);
                while (!queue.isEmpty() && queue.pollFirst() != until) {
                    // dropped
                }
            }
            return 0;
        });
    }

    /** Put a marker made by {@link #marker} into the queue here. */
    static void place(Step marker) {
        queue.add(marker);
    }

    /** Do {@code job} and go straight on. */
    static void then(Runnable job) {
        queue.add(() -> {
            job.run();
            return 0;
        });
    }

    /** Wait {@code ticks} client ticks - frames render in between, which builds hit boxes. */
    static void pause(int ticks) {
        queue.add(() -> ticks);
    }

    /** Wait until {@code cond}, or fail {@code what} after {@code timeout} ticks and go on. */
    static void until(String where, String what, int timeout, Supplier<Boolean> cond) {
        queue.add(() -> {
            boolean ok;
            try {
                ok = cond.get();
            } catch (RuntimeException e) {
                ok = false;
            }
            if (ok) {
                return 0;
            }
            if (waited() >= timeout) {
                fail(where, what + " - not within " + timeout + " ticks");
                return 0;
            }
            return AGAIN;
        });
    }

    /** A check run when its turn comes: {@code problem} returns null for a pass. */
    static void expect(String where, String what, Supplier<@Nullable String> problem) {
        queue.add(() -> {
            String p;
            try {
                p = problem.get();
            } catch (RuntimeException e) {
                p = "the check threw " + e;
            }
            check(where, what, p);
            return 0;
        });
    }

    /**
     * A check that waits: pass as soon as {@code problem} returns null, fail with its last
     * answer after {@code timeout} ticks. For an effect that crosses to the server and back.
     */
    static void eventually(String where, String what, int timeout, Supplier<@Nullable String> problem) {
        String[] last = {null};
        queue.add(() -> {
            String p;
            try {
                p = problem.get();
            } catch (RuntimeException e) {
                p = "the check threw " + e;
            }
            if (p == null) {
                pass(where, what);
                return 0;
            }
            last[0] = p;
            if (waited() >= timeout) {
                fail(where, what + " - " + last[0] + " (after " + timeout + " ticks)");
                return 0;
            }
            return AGAIN;
        });
    }

    /**
     * {@link #eventually}, with the question asked on the server thread. The client never
     * blocks on the server: each poll posts the read and looks at the answer a tick later.
     */
    static void eventuallyServer(String where, String what, int timeout,
                                 Function<IntegratedServer, @Nullable String> problem) {
        @SuppressWarnings("unchecked")
        CompletableFuture<String>[] pending = new CompletableFuture[1];
        String[] last = {"never answered"};
        queue.add(() -> {
            if (pending[0] == null) {
                pending[0] = onServer(s -> {
                    try {
                        String p = problem.apply(s);
                        return p == null ? "" : p;
                    } catch (RuntimeException e) {
                        return "the check threw " + e;
                    }
                });
            }
            if (pending[0].isDone()) {
                String p = pending[0].join();
                pending[0] = null;
                if (p.isEmpty()) {
                    pass(where, what);
                    return 0;
                }
                last[0] = p;
            }
            if (waited() >= timeout) {
                fail(where, what + " - " + last[0] + " (after " + timeout + " ticks)");
                return 0;
            }
            return AGAIN;
        });
    }

    /** Run {@code job} on the server and wait (without blocking) until it has run. */
    static void serverDo(String where, Function<IntegratedServer, ?> job) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Object>[] pending = new CompletableFuture[1];
        queue.add(() -> {
            if (pending[0] == null) {
                pending[0] = onServer(s -> {
                    Object r = job.apply(s);
                    return r;
                });
            }
            if (pending[0].isDone()) {
                if (pending[0].isCompletedExceptionally()) {
                    try {
                        pending[0].join();
                    } catch (RuntimeException e) {
                        fail(where, "the server step threw " + e.getCause());
                    }
                }
                return 0;
            }
            if (waited() >= 200) {
                fail(where, "the server step never ran");
                return 0;
            }
            return AGAIN;
        });
    }

    // ------------------------------------------------------------------ pressing

    /** Ticks to let a press settle and a frame or two draw (hit boxes are built while drawing). */
    static final int SETTLE = 3;

    /**
     * Press the widget {@code find} returns, at its centre. A covered widget is a FAIL, and
     * the press still goes where the mouse would have sent it - the effect check after it
     * then fails too, which is the honest reading of a covered button.
     */
    static void press(String where, String what, Supplier<@Nullable AbstractWidget> find) {
        queue.add(() -> {
            Screen s = screen();
            AbstractWidget w = s == null ? null : find.get();
            if (w == null) {
                fail(where, what + " - not on screen to press");
                return SETTLE;
            }
            if (!w.active) {
                fail(where, what + " - is greyed out");
            }
            String cover = covered(s, w);
            if (cover != null) {
                fail(where, what + " - covered: " + cover);
            }
            click(cx(w), cy(w));
            return SETTLE;
        });
    }

    /** Press the first visible widget whose label starts with {@code prefix}. */
    static void press(String where, String prefix) {
        press(where, "\"" + prefix + "\"", () -> widget(Objects.requireNonNull(screen()), prefix));
    }

    /** Press a widget held in a (private) field of the screen. */
    static void pressField(String where, String field) {
        press(where, field, () -> {
            Object w = get(Objects.requireNonNull(screen()), field);
            return w instanceof AbstractWidget aw && aw.visible ? aw : null;
        });
    }

    /** Press at a point the screen's own geometry gives ({@link #scan}, a hit-box list). */
    static void pressAt(String where, String what, Supplier<double @Nullable []> point) {
        pressAt(where, what, point, 0, 0);
    }

    static void pressAt(String where, String what, Supplier<double @Nullable []> point, int button, int modifiers) {
        queue.add(() -> {
            double[] p = screen() == null ? null : point.get();
            if (p == null) {
                fail(where, what + " - not on screen to press");
                return SETTLE;
            }
            click(p[0], p[1], button, modifiers);
            return SETTLE;
        });
    }

    /**
     * <b>Every visible widget is one the case knows, and none is covered.</b> A widget is
     * known if a named field holds it or its label starts with one of {@code prefixes}.
     * An unknown one is a FAIL: a button nobody tests is the thing this crawler is for.
     */
    static void sweep(String where, List<String> fields, List<String> prefixes) {
        sweep(where, fields, prefixes, w -> false);
    }

    /** {@link #sweep}, with {@code also} naming widgets no field or prefix can (a gene row's allele). */
    static void sweep(String where, List<String> fields, List<String> prefixes, Predicate<AbstractWidget> also) {
        LayoutAudit.audit(where);
        queue.add(() -> {
            Screen s = screen();
            if (s == null) {
                fail(where, "sweep - no screen up");
                return 0;
            }
            List<Object> held = new ArrayList<>();
            for (String f : fields) {
                try {
                    held.add(get(s, f));
                } catch (IllegalStateException e) {
                    fail(where, "sweep - " + e.getMessage());
                }
            }
            List<AbstractWidget> shown = visibleWidgets(s);
            int ok = 0;
            for (AbstractWidget w : shown) {
                boolean known = held.contains(w) || also.test(w);
                for (String p : prefixes) {
                    known |= label(w).startsWith(p);
                }
                if (!known) {
                    fail(where, "unspecified control \"" + label(w) + "\" at " + w.getX() + "," + w.getY()
                            + " - no case presses it");
                    continue;
                }
                // A greyed widget takes no press by design (isMouseOver needs it active),
                // so "nothing takes a press here" is the right answer for one, not a cover.
                String cover = w.active ? covered(s, w) : null;
                if (cover != null) {
                    fail(where, "\"" + label(w) + "\" covered: " + cover);
                    continue;
                }
                ok++;
            }
            pass(where, "sweep - " + ok + " of " + shown.size() + " visible controls known and reachable");
            return 0;
        });
    }

    /** Open {@code screen} on the client and let it draw. */
    static void open(Supplier<Screen> screen) {
        queue.add(() -> {
            mc().setScreen(screen.get());
            return SETTLE + 2;
        });
    }

    static void close() {
        queue.add(() -> {
            // The way Escape closes: onClose, which for a container also tells the server
            // (setScreen(null) alone leaves the menu open there, and the next menu the
            // server opens is refused or shut). A screen with a parent goes back to it,
            // so it is pressed until nothing is up.
            for (int i = 0; i < 5 && screen() != null; i++) {
                Objects.requireNonNull(screen()).onClose();
            }
            if (screen() != null) {
                mc().setScreen(null);
            }
            return SETTLE;
        });
        // A container's close reaches the server as a packet, and the server closes
        // whatever menu is open when it lands - so a menu opened before it arrives is shut
        // by it. Wait for the server to be back on the player's own inventory.
        eventuallyServer("crawler", "the last menu has closed on the server", 60, s -> {
            var p = s.getPlayerList().getPlayer(java.util.Objects.requireNonNull(mc().player).getUUID());
            return p == null || p.containerMenu == p.inventoryMenu ? null : "still open";
        });
    }

    /** Equal, or a sentence saying how not. */
    static @Nullable String same(String what, @Nullable Object want, @Nullable Object got) {
        return Objects.equals(want, got) ? null : what + " is " + name(got) + ", wanted " + name(want);
    }

    static @Nullable String isScreen(Class<?> want) {
        Screen s = screen();
        if (want == null) {
            return s == null ? null : "the screen is " + s.getClass().getSimpleName() + ", wanted none";
        }
        return want.isInstance(s) ? null
                : "the screen is " + (s == null ? "none" : s.getClass().getSimpleName()) + ", wanted "
                        + want.getSimpleName();
    }

    static @Nullable String noScreen() {
        Screen s = screen();
        return s == null ? null : "the screen is still " + s.getClass().getSimpleName();
    }
}
