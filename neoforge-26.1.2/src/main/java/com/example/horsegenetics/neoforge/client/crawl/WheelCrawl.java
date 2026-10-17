package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.care.CommandWheel;
import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.neoforge.client.CommandWheelScreen;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.animal.equine.Horse;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The command whistle's wheel ({@link CommandWheelScreen}). It has no widgets and no
 * {@code mouseClicked}: a slice is chosen by letting go of the right button over it. So each
 * slice is opened fresh and released on at the point its label is drawn, in the same tick
 * (the wheel's own tick picks as soon as the physical button is up, which in a crawl is
 * at once). The check is the order the server set, read off the horse.
 */
final class WheelCrawl {

    private static final String W = "CommandWheelScreen";

    private WheelCrawl() {
    }

    static void queue() {
        List<HorseOrder> slices = HorseOrders.wheel();
        for (int i = 0; i < slices.size(); i++) {
            slice(() -> DebugMenuCrawl.stallion, i, slices.get(i), false);
        }
        // The dead centre sends nothing.
        HorseOrder[] before = {null};
        serverDo(W, s -> before[0] = order(s, DebugMenuCrawl.stallion));
        then(() -> {
            CommandWheelScreen.openFor(Objects.requireNonNull(MenuCrawlCases.clientHorse(DebugMenuCrawl.stallion)), "crawl");
            Screen s = Objects.requireNonNull(screen());
            s.mouseReleased(event(s.width / 2.0, s.height / 2.0, 1, 0));
        });
        pause(10);
        expect(W, "a release on the dead centre closes the wheel", Crawl::noScreen);
        eventuallyServer(W, "a release on the dead centre orders nothing", 5,
                s -> same("the order", before[0], order(s, DebugMenuCrawl.stallion)));

        // The all-horses wheel orders the mare too.
        slice(() -> DebugMenuCrawl.mare, slices.indexOf(HorseOrder.STAY), HorseOrder.STAY, true);
    }

    private static void slice(java.util.function.Supplier<@Nullable UUID> horse, int i, HorseOrder order, boolean all) {
        String what = (all ? "all-horses wheel " : "wheel ") + "slice \"" + order.label() + "\"";
        boolean[] refused = {false};
        HorseOrder[] before = {null};
        serverDo(W, s -> {
            // From a known start, so a slice that does nothing cannot pass by already being set.
            Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), horse.get());
            if (h != null) {
                h.removeData(ModAttachments.HORSE_ORDER.get());
            }
            before[0] = order(s, horse.get());
            return null;
        });
        then(() -> {
            if (all) {
                CommandWheelScreen.openForAll();
            } else {
                CommandWheelScreen.openFor(Objects.requireNonNull(MenuCrawlCases.clientHorse(horse.get())), "crawl");
            }
            Screen s = Objects.requireNonNull(screen());
            double a = CommandWheel.centreAngle(i, HorseOrders.wheel().size());
            int rx = (Integer) get(CommandWheelScreen.class, "RING_X");
            int ry = (Integer) get(CommandWheelScreen.class, "RING_Y");
            int x = s.width / 2 + (int) Math.round(Math.sin(a) * rx);
            int y = s.height / 2 - (int) Math.round(Math.cos(a) * ry);
            // The label is drawn here; the hit-test must agree that this is the slice.
            set(s, "mouseX", x);
            set(s, "mouseY", y);
            Object hovered = call(s, "hovered");
            check(W, what + " - its label sits on its own hit area", same("the slice under its label", i, hovered));
            refused[0] = call(s, "refusal", order) != null;
            s.mouseReleased(event(x, y, 1, 0));
        });
        pause(10);
        expect(W, what + " - a release closes the wheel", Crawl::noScreen);
        eventuallyServer(W, what + (all ? " - every horse in reach takes the order" : " - the horse takes the order"), 40, s -> {
            HorseOrder now = order(s, horse.get());
            if (refused[0]) {
                return same("a greyed slice's order", before[0], now);
            }
            // Rejoin herd is the default: taking it clears the order rather than storing one.
            if (order == HorseOrder.REJOIN_HERD && now == null) {
                return null;
            }
            return same("the order", order, now);
        });
    }

    private static @Nullable HorseOrder order(net.minecraft.client.server.IntegratedServer s, @Nullable UUID id) {
        Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), id);
        if (h == null || !h.hasData(ModAttachments.HORSE_ORDER.get())) {
            return null;
        }
        return h.getData(ModAttachments.HORSE_ORDER.get()).order();
    }
}
