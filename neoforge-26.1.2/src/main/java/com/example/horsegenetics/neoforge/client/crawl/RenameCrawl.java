package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.client.HorseRenameScreen;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.List;
import java.util.Objects;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/** The name-tag screen ({@link HorseRenameScreen}): both boxes, Rename (refused blank, then real), Cancel. */
final class RenameCrawl {

    private static final String W = "HorseRenameScreen";

    private RenameCrawl() {
    }

    static void queue() {
        serverDo(W, s -> {
            DebugMenuCrawl.hold(MenuCrawlCases.player(s), DebugMenuCrawl.nameTag());
            return null;
        });
        open(() -> new HorseRenameScreen(Objects.requireNonNull(MenuCrawlCases.clientHorse(DebugMenuCrawl.mare)).getId()));
        sweep(W, List.of("firstBox", "lastBox", "rename"), List.of("Cancel"));

        pressField(W, "lastBox");
        expect(W, "the last-name box takes focus on a press",
                () -> screen() != null && screen().getFocused() == get(screen(), "lastBox") ? null : "not focused");
        pressField(W, "firstBox");
        expect(W, "the first-name box takes focus on a press",
                () -> screen() != null && screen().getFocused() == get(screen(), "firstBox") ? null : "not focused");

        then(() -> {
            box("firstBox").setValue("");
            box("lastBox").setValue("");
        });
        pressField(W, "rename");
        expect(W, "Rename with both names blank does nothing and stays open", () -> isScreen(HorseRenameScreen.class));

        then(() -> {
            box("firstBox").setValue("Crawler");
            box("lastBox").setValue("Tested");
        });
        pressField(W, "rename");
        expect(W, "Rename closes the screen", Crawl::noScreen);
        eventuallyServer(W, "Rename renames the horse on the server", 40, s -> {
            Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), DebugMenuCrawl.mare);
            if (h == null) {
                return "the mare is gone";
            }
            String first = HorseRecords.of(h).firstName();
            return "Crawler".equals(first) ? null : "her first name is " + first;
        });

        open(() -> new HorseRenameScreen(Objects.requireNonNull(MenuCrawlCases.clientHorse(DebugMenuCrawl.mare)).getId()));
        press(W, "Cancel");
        expect(W, "Cancel closes the screen", Crawl::noScreen);
        serverDo(W, s -> {
            DebugMenuCrawl.hold(MenuCrawlCases.player(s), net.minecraft.world.item.ItemStack.EMPTY);
            return null;
        });
    }

    private static EditBox box(String field) {
        return (EditBox) get(Objects.requireNonNull(screen()), field);
    }
}
