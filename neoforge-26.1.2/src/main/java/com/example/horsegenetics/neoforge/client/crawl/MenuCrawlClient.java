package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Dev-only: the menu crawler.</b> It opens every one of this mod's screens, presses every
 * control on each one at its screen coordinates, and checks that the press did what the
 * control is for. Each check writes one line to the log, {@code [crawl] PASS} /
 * {@code FAIL} / {@code SKIP}, and the run ends with a {@code [crawl] done} tally.
 *
 * <p>Off unless the client was started with {@code -Dhorsegenetics.menuCrawl=true}:
 * {@code gradlew :neoforge-26.1.2:runClientTestWorld -PmenuCrawl}. It runs in the throwaway
 * test world, in the overworld (the yard hop is skipped), and the client closes itself when
 * it is done unless {@code -PmenuCrawl=stay}.
 *
 * <p>What the press reaches is decided by the screen, not the crawler: see {@link Crawl}.
 * Every widget on a screen must be named by the case for that screen - one the case does
 * not know about is a FAIL ("nobody tests this button"), so a button added later cannot
 * quietly go untested.
 *
 * <p>Two things outside the throwaway world are touched, and both are put back: this
 * client's config (saved searches, the gene filter, the tutorial flag - the browser and the
 * horse screen write them) and the system clipboard (the spawn egg's Copy and Paste).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class MenuCrawlClient {

    public static final String PROPERTY = "horsegenetics.menuCrawl";

    /** Ticks after the player appears before anything happens - the world's own setup first. */
    private static final int START_DELAY = 200;

    private static int ticks;
    private static boolean started;
    private static boolean finished;

    private static List<String> savedSearchesBefore;
    private static List<String> geneFilterBefore;
    private static boolean tutorialBefore;
    private static String clipboardBefore = "";

    private MenuCrawlClient() {
    }

    public static boolean enabled() {
        String v = System.getProperty(PROPERTY);
        return v != null && !v.equals("false");
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (finished || !enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (!started) {
            if (++ticks < START_DELAY) {
                return;
            }
            started = true;
            begin();
        }
        Crawl.tick();
        if (Crawl.idle()) {
            finish(mc);
        }
    }

    /**
     * Which code closed a screen, while a crawl runs: a screen that vanishes between two
     * steps is otherwise a mystery in the log. The first frames outside the event bus say.
     */
    @SubscribeEvent
    static void onScreenClosing(net.neoforged.neoforge.client.event.ScreenEvent.Closing event) {
        if (!enabled() || !started || finished) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (StackTraceElement e : new Throwable().getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("net.neoforged.bus") || c.contains("MenuCrawlClient") || c.startsWith("java.")
                    || c.startsWith("jdk.")) {
                continue;
            }
            sb.append(" < ").append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName())
                    .append(':').append(e.getLineNumber());
            if (++shown == 8) {
                break;
            }
        }
        HorseGenetics.LOGGER.info("[crawl] screen closing: {}{}", event.getScreen().getClass().getSimpleName(), sb);
    }

    private static void begin() {
        HorseGenetics.LOGGER.info("[crawl] start");
        snapshot();
        try {
            MenuCrawlCases.queueAll();
        } catch (RuntimeException e) {
            // A case that throws while it is being queued (reading a horse before the yard
            // exists) must fail the run, not crash the client.
            Crawl.fail("crawler", "queueing the cases threw " + e);
            HorseGenetics.LOGGER.warn("[crawl] queueing threw", e);
        }
    }

    private static void finish(Minecraft mc) {
        finished = true;
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        restore();
        HorseGenetics.LOGGER.info("[crawl] done - {} passed, {} failed, {} skipped",
                Crawl.passed, Crawl.failed, Crawl.skipped);
        for (String f : Crawl.failures) {
            HorseGenetics.LOGGER.info("[crawl] failed: {}", f);
        }
        if (!String.valueOf(System.getProperty(PROPERTY)).contains("stay")) {
            mc.stop();
        }
    }

    // ------------------------------------------------------------------ what is put back

    private static void snapshot() {
        savedSearchesBefore = new ArrayList<>(ClientConfig.SAVED_SEARCHES.get());
        geneFilterBefore = new ArrayList<>(ClientConfig.GENE_FILTER.get());
        tutorialBefore = ClientConfig.TUTORIAL_SEEN.get();
        try {
            clipboardBefore = Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (RuntimeException e) {
            clipboardBefore = "";
        }
    }

    private static void restore() {
        try {
            ClientConfig.SAVED_SEARCHES.set(savedSearchesBefore);
            ClientConfig.GENE_FILTER.set(geneFilterBefore);
            ClientConfig.TUTORIAL_SEEN.set(tutorialBefore);
            ClientConfig.SPEC.save();
            Minecraft.getInstance().keyboardHandler.setClipboard(clipboardBefore);
            HorseGenetics.LOGGER.info("[crawl] client config and clipboard put back");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[crawl] could not put the client config back", e);
        }
    }
}
