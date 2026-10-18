package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.client.ClientPopulation;
import com.example.horsegenetics.neoforge.client.FamilyOverviewScreen;
import com.example.horsegenetics.neoforge.client.FamilyTreeScreen;
import com.example.horsegenetics.neoforge.client.HorseInfoScreen;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The family tree ({@link FamilyTreeScreen}), reached the way a player reaches it - the
 * foal's horse screen, its Family tree tab - and the overview chart behind its Everyone
 * button ({@link FamilyOverviewScreen}). The foal's dam and sire are the nodes to click.
 */
final class FamilyCrawl {

    private static final String T = "FamilyTreeScreen";
    private static final String O = "FamilyOverviewScreen";

    private FamilyCrawl() {
    }

    static void queue() {
        then(() -> set(HorseInfoScreen.class, "lastTab", InfoScreenCrawl.tab("OVERVIEW")));
        InfoScreenCrawl.openOn(() -> DebugMenuCrawl.foal);
        InfoScreenCrawl.pressTab("FAMILY");
        expect(T, "the horse screen's Family tree tab opens the tree", () -> isScreen(FamilyTreeScreen.class));
        sweep(T, List.of("backButton", "homeButton"), List.of("Ancestors", "Descendants", "Overview", "Everyone", "Done"));

        eventually(T, "the tree's answer arrives with the foal's dam in it", 80, () -> node(DebugMenuCrawl.mare) != null
                ? null : "no node for the dam");
        pressAt(T, "the dam's box", () -> node(DebugMenuCrawl.mare));
        expect(T, "a box re-centres the tree on that horse", () -> same("rootId", DebugMenuCrawl.mare, field("rootId")));
        expect(T, "after a re-centre, Back and Home can be pressed", () -> {
            AbstractWidget back = (AbstractWidget) field("backButton");
            AbstractWidget home = (AbstractWidget) field("homeButton");
            return back.active && home.active ? null : "Back " + back.active + ", Home " + home.active;
        });
        pressField(T, "backButton");
        expect(T, "Back goes to the horse before", () -> same("rootId", DebugMenuCrawl.foal, field("rootId")));
        expect(T, "with nothing behind, Back is greyed out",
                () -> ((AbstractWidget) field("backButton")).active ? "still active" : null);
        eventually(T, "the foal's tree is drawn again", 80, () -> node(DebugMenuCrawl.stallion) != null
                ? null : "no node for the sire");
        pressAt(T, "the sire's box", () -> node(DebugMenuCrawl.stallion));
        pressField(T, "homeButton");
        expect(T, "Home goes back to the horse the tree opened on", () -> same("rootId", DebugMenuCrawl.foal, field("rootId")));

        for (String[] m : new String[][] {{"Descendants", "DESCENDANTS"}, {"Overview", "OVERVIEW"}, {"Ancestors", "ANCESTORS"}}) {
            press(T, m[0]);
            expect(T, "\"" + m[0] + "\" shows that view, and its button greys out", () -> {
                String p = same("mode", m[1], name(field("mode")));
                if (p != null) {
                    return p;
                }
                AbstractWidget b = widget(Objects.requireNonNull(screen()), m[0]);
                return b != null && !b.active ? null : "its button is still active";
            });
        }

        press(T, "Everyone");
        expect(T, "Everyone opens the overview chart", () -> isScreen(FamilyOverviewScreen.class));
        overview();
        press(T, "Done");
        expect(T, "Done on the tree goes back to the horse screen", () -> isScreen(HorseInfoScreen.class));
        press(T, "Done");
        expect(T, "and Done there closes it", Crawl::noScreen);
    }

    private static void overview() {
        eventually(O, "the population answer arrives", 80, () -> ClientPopulation.received() ? null : "not yet");
        pause(SETTLE);
        sweep(O, List.of("showEveryoneButton"), List.of("Refresh", "Fit", "Done"));

        int[] version = {0};
        then(() -> version[0] = ClientPopulation.version());
        // The server answers one question of a kind per second (ModNetworking.mayAnswer);
        // a second ask inside that is dropped on purpose.
        pause(25);
        press(O, "Refresh");
        eventually(O, "Refresh asks again and a new answer arrives", 80,
                () -> ClientPopulation.version() > version[0] ? null : "same version " + version[0]);

        float[] fitted = {0};
        pause(SETTLE);
        then(() -> fitted[0] = (Float) field("zoom"));
        then(() -> {
            Screen s = Objects.requireNonNull(screen());
            wheel(s, s.width / 2.0, s.height / 2.0, 0, 3);
            if (Objects.equals(fitted[0], get(s, "zoom"))) {
                // Already fitted at the most zoomed-in it goes: the other way, then.
                wheel(s, s.width / 2.0, s.height / 2.0, 0, -6);
            }
        });
        pause(SETTLE);
        expect(O, "the wheel zooms the chart", () -> Objects.equals(fitted[0], field("zoom")) ? "zoom unchanged" : null);
        press(O, "Fit");
        expect(O, "Fit puts the zoom back to the fitted view", () -> same("zoom", fitted[0], field("zoom")));

        // With the wild horses in it the chart is wider than the window: drag it so the dam
        // is in the middle - which is also the pan being tested.
        then(() -> {
            Screen s = Objects.requireNonNull(screen());
            for (Object n : (List<?>) get(s, "nodes")) {
                if (DebugMenuCrawl.mare.equals(call(call(n, "entry"), "id"))) {
                    int sx = (Integer) call(s, "screenX", call(n, "x"));
                    int sy = (Integer) call(s, "screenY", call(n, "y"));
                    float panX = (Float) get(s, "panX");
                    drag(s.width / 2.0, s.height - 40, s.width / 2.0 - sx, (s.height / 2.0) - sy);
                    check(O, "dragging the chart pans it",
                            Objects.equals(panX, get(s, "panX")) ? "panX unchanged" : null);
                    return;
                }
            }
            fail(O, "the dam is not in the population chart");
        });
        pause(SETTLE);
        pressAt(O, "the dam's box", () -> {
            Screen s = Objects.requireNonNull(screen());
            return scan(s, hitTest(s, "nodeAt"), n -> DebugMenuCrawl.mare.equals(call(call(n, "entry"), "id")));
        });
        expect(O, "a box focuses the chart on that horse's family", () -> same("focusId", DebugMenuCrawl.mare, field("focusId")));
        expect(O, "and Show everyone appears", () -> ((AbstractWidget) field("showEveryoneButton")).visible ? null : "hidden");
        pressField(O, "showEveryoneButton");
        expect(O, "Show everyone clears the focus", () -> field("focusId") == null ? null : "focusId " + field("focusId"));
        expect(O, "and hides itself", () -> ((AbstractWidget) field("showEveryoneButton")).visible ? "still shown" : null);

        press(O, "Done");
        expect(O, "Done goes back to the tree", () -> isScreen(FamilyTreeScreen.class));
    }

    private static @Nullable Object field(String name) {
        return get(Objects.requireNonNull(screen(), "no screen"), name);
    }

    /** The centre of {@code id}'s box in the tree, if it is drawn (and not the subject). */
    private static double @Nullable [] node(@Nullable UUID id) {
        Screen s = screen();
        if (!(s instanceof FamilyTreeScreen) || id == null) {
            return null;
        }
        int w = (Integer) get(s, "boxW");
        int h = (Integer) get(s, "boxH");
        for (Object n : (List<?>) get(s, "nodes")) {
            if (id.equals(call(n, "id")) && call(n, "record") != null && !id.equals(get(s, "rootId"))) {
                return new double[] {(Integer) call(n, "x") + w / 2.0, (Integer) call(n, "y") + h / 2.0};
            }
        }
        return null;
    }
}
