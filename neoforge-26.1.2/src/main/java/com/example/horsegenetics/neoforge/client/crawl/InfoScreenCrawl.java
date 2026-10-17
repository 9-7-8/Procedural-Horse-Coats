package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.client.ClientOffspring;
import com.example.horsegenetics.neoforge.client.HorseGearScreen;
import com.example.horsegenetics.neoforge.client.HorseInfoScreen;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The horse screen ({@link HorseInfoScreen}), opened on the player's own mare: every tab of
 * the strip, the barn name, the two gene-list filters and the pinned-loci menu, a tack slot
 * and its picker (put on, take off), the doll, Dress, a foal's link on the Offspring tab,
 * Ride, and Done. The Family tree tab is {@link FamilyCrawl}'s.
 */
final class InfoScreenCrawl {

    private static final String W = "HorseInfoScreen";

    private static final List<String> STANDING = List.of("Done");

    private InfoScreenCrawl() {
    }

    static void queue() {
        serverDo(W, s -> MenuCrawlCases.player(s).getInventory().add(new ItemStack(Items.SADDLE)));
        then(() -> set(HorseInfoScreen.class, "lastTab", tab("OVERVIEW")));
        openOnMare();

        // Overview: the barn name, Ride (pressed last - it closes the screen).
        sweep(W + " Overview", List.of("barnBox", "setBarnButton", "rideButton"), STANDING);
        pressField(W, "barnBox");
        expect(W, "the barn-name box takes focus on a press",
                () -> screen() != null && screen().getFocused() == get(screen(), "barnBox") ? null : "not focused");
        then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "barnBox")).setValue("Crawl Barn"));
        pressField(W, "setBarnButton");
        eventuallyServer(W, "Set gives the horse its barn name on the server", 40, s -> {
            Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), DebugMenuCrawl.mare);
            return h != null && HorseRecords.of(h).barnName().filter("Crawl Barn"::equals).isPresent()
                    ? null : "no barn name";
        });

        // Every tab of the strip (Family tree is a screen of its own - FamilyCrawl).
        for (String t : List.of("GEAR", "GENES", "HEALTH", "COAT", "OTHER", "OFFSPRING", "OVERVIEW")) {
            pressTab(t);
            expect(W, "the " + t + " tab opens its page", () -> same("tab", t, name(field("tab"))));
        }

        genes();
        gear();
        offspring();

        // Ride: the screen closes and the player is on the horse.
        pressTab("OVERVIEW");
        pressField(W, "rideButton");
        expect(W, "Ride closes the screen", Crawl::noScreen);
        eventuallyServer(W, "Ride puts the player on the horse", 40, s -> {
            var p = MenuCrawlCases.player(s);
            return p.getVehicle() != null && p.getVehicle().getUUID().equals(DebugMenuCrawl.mare) ? null : "not riding her";
        });
        serverDo(W, s -> {
            MenuCrawlCases.player(s).stopRiding();
            DebugMenuCrawl.home(MenuCrawlCases.player(s));
            return null;
        });
        pause(10);

        openOnMare();
        press(W, "Done");
        expect(W, "Done closes the screen (opened from the world)", Crawl::noScreen);
    }

    private static void genes() {
        pressTab("GENES");
        sweep(W + " Genes", List.of("baselineFilterButton", "geneFilterButton"), STANDING);
        boolean[] hide = {false};
        then(() -> hide[0] = (Boolean) get(HorseInfoScreen.class, "hideBaseline"));
        pressField(W, "baselineFilterButton");
        expect(W, "the baseline filter flips what the gene list shows",
                () -> same("hideBaseline", !hide[0], get(HorseInfoScreen.class, "hideBaseline")));
        pressField(W, "baselineFilterButton");
        expect(W, "the baseline filter again flips it back",
                () -> same("hideBaseline", hide[0], get(HorseInfoScreen.class, "hideBaseline")));

        then(() -> ClientConfig.setGeneFilter(List.of()));
        pressField(W, "geneFilterButton");
        expect(W, "the gene filter opens the pinned-loci menu", () -> pickerOpen() ? null : "the menu did not open");
        pressAt(W, "pinned-loci row 1", () -> pickerRow(1));
        expect(W, "a pinned-loci row pins that gene, and the menu stays open",
                () -> ClientConfig.geneFilter().size() == 1 && pickerOpen() ? null
                        : "filter " + ClientConfig.geneFilter() + ", open " + pickerOpen());
        expect(W, "the filter button says one is pinned",
                () -> label((net.minecraft.client.gui.components.AbstractWidget) field("geneFilterButton")).contains("1")
                        ? null : "it reads " + label((net.minecraft.client.gui.components.AbstractWidget) field("geneFilterButton")));
        pressAt(W, "pinned-loci row 0", () -> pickerRow(0));
        expect(W, "row 0 unpins every gene and closes the menu",
                () -> ClientConfig.geneFilter().isEmpty() && !pickerOpen() ? null
                        : "filter " + ClientConfig.geneFilter() + ", open " + pickerOpen());
        pressField(W, "geneFilterButton");
        pressAt(W, "outside the menu", () -> new double[] {2, Objects.requireNonNull(screen()).height - 2});
        expect(W, "a press outside the pinned-loci menu closes it", () -> pickerOpen() ? "still open" : null);
    }

    private static void gear() {
        pressTab("GEAR");
        sweep(W + " Gear", List.of("dressButton"), STANDING);

        // A tack slot opens its picker; the picker's row puts the saddle on.
        pressAt(W, "the saddle slot", () -> tackSlot(HorseTackSlot.SADDLE));
        expect(W, "a tack slot opens its picker", () -> same("pickerSlot", "SADDLE", name(field("pickerSlot"))));
        pressAt(W, "picker row 0 (the saddle in the inventory)", () -> pickerRowGear(0));
        expect(W, "a picker row closes the picker", () -> field("pickerSlot") == null ? null : "still open");
        eventuallyServer(W, "the picked saddle goes on the horse", 40,
                s -> saddle(s).isEmpty() ? "she is not saddled" : null);
        pause(10);
        pressAt(W, "the saddle slot again", () -> tackSlot(HorseTackSlot.SADDLE));
        pressAt(W, "picker row 0 (Take off)", () -> pickerRowGear(0));
        eventuallyServer(W, "Take off takes the saddle off", 40,
                s -> saddle(s).isEmpty() ? null : "still saddled");

        // The doll turns when dragged.
        float[] yaw = {0};
        then(() -> yaw[0] = (Float) field("dollYaw"));
        then(() -> {
            Object box = field("dollBox");
            if (box == null) {
                fail(W, "the doll has no box to drag");
                return;
            }
            double[] c = centreOfBox(box);
            drag(c[0], c[1], 20, 0);
        });
        pause(SETTLE);
        expect(W, "dragging the doll turns the horse",
                () -> Objects.equals(yaw[0], field("dollYaw")) ? "dollYaw still " + yaw[0] : null);

        // Dress opens the gear menu, and closing it comes back here.
        pressField(W, "dressButton");
        eventually(W, "Dress opens the gear menu", 40, () -> isScreen(HorseGearScreen.class));
        then(() -> Objects.requireNonNull(screen()).onClose());
        pause(SETTLE);
        expect(W, "closing the gear menu comes back to the horse screen", () -> isScreen(HorseInfoScreen.class));
    }

    private static void offspring() {
        pressTab("OFFSPRING");
        sweep(W + " Offspring", List.of("offspringRefreshButton"), STANDING);
        then(ClientOffspring::clear);
        pressField(W, "offspringRefreshButton");
        eventually(W, "Refresh asks for the offspring and the answer arrives", 60,
                () -> same("the offspring answer's root", DebugMenuCrawl.mare, ClientOffspring.rootId()));
        pause(SETTLE);
        // The foal is hers: its portrait is a link to its own horse screen.
        pressAt(W, "the foal's portrait", () -> {
            List<?> links = (List<?>) field("linkHits");
            for (Object hit : Objects.requireNonNull(links)) {
                if (Objects.equals(call(hit, "id"), DebugMenuCrawl.foal)) {
                    return centreOfBox(hit);
                }
            }
            return null;
        });
        eventually(W, "the foal's portrait opens the foal's horse screen", 80, () -> {
            Screen s = screen();
            if (!(s instanceof HorseInfoScreen)) {
                return isScreen(HorseInfoScreen.class);
            }
            HorseRecord r = (HorseRecord) get(s, "record");
            return same("the screen's horse", DebugMenuCrawl.foal, r.id());
        });
        press(W, "Done");
        expect(W, "Done on a linked horse comes back to the horse it came from", () -> {
            Screen s = screen();
            return s instanceof HorseInfoScreen
                    ? same("the screen's horse", DebugMenuCrawl.mare, ((HorseRecord) get(s, "record")).id())
                    : isScreen(HorseInfoScreen.class);
        });
    }

    // ------------------------------------------------------------------ helpers

    static void openOnMare() {
        open(() -> new HorseInfoScreen(Objects.requireNonNull(MenuCrawlCases.clientRecord(DebugMenuCrawl.mare)),
                MenuCrawlCases.clientHorse(DebugMenuCrawl.mare), null));
    }

    /** A supplier, not the id: the yard's horses do not exist yet when the steps are queued. */
    static void openOn(java.util.function.Supplier<@Nullable UUID> id) {
        open(() -> new HorseInfoScreen(Objects.requireNonNull(MenuCrawlCases.clientRecord(id.get())),
                MenuCrawlCases.clientHorse(id.get()), null));
    }

    static Object tab(String name) {
        return enumConstant(HorseInfoScreen.class, "Tab", name);
    }

    static void pressTab(String name) {
        pressAt(W, "the " + name + " tab", () -> {
            Screen s = Objects.requireNonNull(screen());
            return scan(s, hitTest(s, "tabAt"), t -> t == tab(name));
        });
    }

    private static @Nullable Object field(String name) {
        return get(Objects.requireNonNull(screen(), "no screen"), name);
    }

    private static boolean pickerOpen() {
        return (Boolean) call(Objects.requireNonNull(field("genePicker")), "isOpen");
    }

    private static double[] pickerRow(int i) {
        Object picker = Objects.requireNonNull(field("genePicker"));
        int x = (Integer) get(picker, "x");
        int y = (Integer) get(picker, "y");
        int scroll = (Integer) get(picker, "scroll");
        return new double[] {x + 30, y + (i - scroll) * 12 + 6};
    }

    private static double @Nullable [] tackSlot(HorseTackSlot slot) {
        for (Object hit : (List<?>) Objects.requireNonNull(field("tackHits"))) {
            if (call(hit, "slot") == slot) {
                return new double[] {(Integer) call(hit, "x") + 9, (Integer) call(hit, "y") + 9};
            }
        }
        return null;
    }

    private static double[] pickerRowGear(int i) {
        int x = (Integer) Objects.requireNonNull(field("pickerX"));
        int y = (Integer) Objects.requireNonNull(field("pickerY"));
        int scroll = (Integer) Objects.requireNonNull(field("pickerScroll"));
        return new double[] {x + 40, y + (i - scroll) * 18 + 9};
    }

    private static ItemStack saddle(net.minecraft.client.server.IntegratedServer s) {
        Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), DebugMenuCrawl.mare);
        return h == null ? ItemStack.EMPTY : HorseTackSlot.SADDLE.on(h);
    }
}
