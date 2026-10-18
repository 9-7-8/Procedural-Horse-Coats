package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.carts.client.screen.PlowScreen;
import com.example.horsegenetics.neoforge.carts.client.screen.SeedDrillScreen;
import com.example.horsegenetics.neoforge.carts.client.screen.WagonScreen;
import com.example.horsegenetics.neoforge.client.EquestrianBenchScreen;
import com.example.horsegenetics.neoforge.client.HorseGearScreen;
import com.example.horsegenetics.neoforge.client.HorsePackScreen;
import com.example.horsegenetics.neoforge.client.HorseStasisBankScreen;
import com.example.horsegenetics.neoforge.client.JumpScreen;
import com.example.horsegenetics.neoforge.client.ResearchShelfScreen;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.menu.EquestrianBenchMenu;
import com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu;
import com.example.horsegenetics.neoforge.network.OpenHorseGearPayload;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import com.example.horsegenetics.neoforge.server.HorsePackHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The container screens - the four menu blocks, the gear and pack menus on a horse, and the
 * carts. Every one gets the slot check ({@link Crawl#slotLayout}: every active slot inside
 * the window, none overlapping), a shift-click of something it takes, read back off the
 * server, and its own buttons, tabs and rows.
 */
final class ContainerCrawl {

    /** GLFW's shift modifier bit. */
    private static final int SHIFT = 1;

    private ContainerCrawl() {
    }

    static void queue() {
        serverDo("containers", s -> {
            var inv = MenuCrawlCases.player(s).getInventory();
            // From empty: the test world's kit fills the inventory, and an item given to a full
            // one - or handed back by a menu as it closes - is dropped and lost to the crawl.
            inv.clearContent();
            inv.add(new ItemStack(Items.SADDLE));
            for (Item item : List.of(Items.BOOK, Items.SPRUCE_PLANKS, Items.RED_DYE, Items.SADDLE, Items.STICK,
                    Items.WHEAT_SEEDS, Items.WATER_BUCKET, Items.WHEAT, Items.COBBLESTONE, Items.OAK_LOG, Items.STONE)) {
                inv.add(new ItemStack(item, item.getDefaultMaxStackSize() > 1 ? 16 : 1));
            }
            return null;
        });
        shelf();
        jump();
        bench();
        bank();
        gear();
        pack();
        cart("PlowScreen", PlowScreen.class, () -> DebugMenuCrawl.plow, Items.COBBLESTONE);
        cart("SeedDrillScreen", SeedDrillScreen.class, () -> DebugMenuCrawl.drill, Items.WHEAT_SEEDS);
        cart("WagonScreen (1 chest)", WagonScreen.class, () -> DebugMenuCrawl.wagonSmall, Items.OAK_LOG);
        cart("WagonScreen (3 chests)", WagonScreen.class, () -> DebugMenuCrawl.wagonBig, Items.STONE);
    }

    // ------------------------------------------------------------------ the research shelf

    private static void shelf() {
        String w = "ResearchShelfScreen";
        openBlock(w, ResearchShelfScreen.class, () -> DebugMenuCrawl.shelf);
        for (String t : List.of("CRAFT", "STORE")) {
            pressAt(w, "the " + t + " tab", () -> tabPoint(ResearchShelfScreen.class, "Tab", t));
            expect(w, "the " + t + " tab shows its slots", () -> {
                Object tab = get(Objects.requireNonNull(screen()), "tab");
                String p = same("tab", t, name(tab));
                if (p != null) {
                    return p;
                }
                // Store shows the 54 paper slots (2..55), Copy the book and the result (0, 1).
                Slot book = menuSlot(0);
                Slot paper = menuSlot(2);
                boolean copy = t.equals("CRAFT");
                return book.isActive() == copy && paper.isActive() == !copy ? null
                        : "book slot active " + book.isActive() + ", paper slot active " + paper.isActive();
            });
            layout(w + " " + t);
        }
        sweep(w, List.of(), List.of());
        pressAt(w, "the Copy tab", () -> tabPoint(ResearchShelfScreen.class, "Tab", "CRAFT"));
        shiftIn(w, Items.BOOK, s -> {
            var sh = shelfAt(s);
            return sh != null && sh.book().getItem(0).is(Items.BOOK) ? null : "no book in the copy slot";
        });
        String[] topic = {""};
        then(() -> {
            List<?> topics = (List<?>) call(menu(), "storedTopics");
            topic[0] = topics.isEmpty() ? "" : String.valueOf(topics.get(topics.size() - 1));
        });
        pressAt(w, "the last row of the gene list", () -> {
            AbstractContainerScreen<?> s = cs();
            List<?> topics = (List<?>) call(menu(), "storedTopics");
            int scroll = (Integer) get(s, "scroll");
            int row = topics.size() - 1 - scroll;
            return row < 0 ? null : new double[] {left(s) + 8 + 40, top(s) + 20 + row * 12 + 6};
        });
        eventuallyServer(w, "a gene row picks what the shelf copies", 40, s -> {
            var sh = shelfAt(s);
            return sh == null ? "no shelf" : same("the shelf's topic", topic[0], sh.selectedTopic());
        });
        close();
    }

    // ------------------------------------------------------------------ the jump

    private static void jump() {
        String w = "JumpScreen";
        openBlock(w, JumpScreen.class, () -> DebugMenuCrawl.jump);
        layout(w);
        sweep(w, List.of("shorter", "taller"), List.of(), b -> List.of(styleButtons()).contains(b));
        String[] styles = {"VERTICAL", "OXER", "CROSSRAILS"};
        for (int i : new int[] {1, 2, 0}) {
            int n = i;
            press(w, "style button " + styles[n], () -> styleButtons()[n]);
            eventuallyServer(w, "style " + styles[n] + " restyles the whole run of jumps", 40, s -> {
                ServerPlayer p = MenuCrawlCases.player(s);
                String a = name(p.level().getBlockState(DebugMenuCrawl.jump).getValue(JumpBlock.STYLE));
                String b = name(p.level().getBlockState(DebugMenuCrawl.jump2).getValue(JumpBlock.STYLE));
                return a.equals(styles[n]) && b.equals(styles[n]) ? null : "this jump " + a + ", the next " + b;
            });
            eventually(w, "the chosen style's button greys out", 20,
                    () -> styleButtons()[n].active ? "still active" : null);
        }
        int[] size = {0};
        serverDo(w, s -> size[0] = jumpAt(s).materials().size());
        pressField(w, "taller");
        eventuallyServer(w, "+ makes the jump taller", 40, s -> same("size", size[0] + 1, jumpAt(s).materials().size()));
        pressField(w, "shorter");
        eventuallyServer(w, "- makes it shorter again", 40, s -> same("size", size[0], jumpAt(s).materials().size()));
        String[] rails = {""};
        serverDo(w, s -> rails[0] = jumpAt(s).materials().rails());
        shiftIn(w, Items.SPRUCE_PLANKS, s -> jumpAt(s).materials().rails().equals(rails[0])
                ? "the rails are still " + rails[0] : null);
        close();
    }

    // ------------------------------------------------------------------ the bench

    private static void bench() {
        String w = "EquestrianBenchScreen";
        int[] saddles = {0};
        serverDo(w, s -> saddles[0] = count(s, Items.SADDLE));
        openBlock(w, EquestrianBenchScreen.class, () -> DebugMenuCrawl.bench);
        layout(w);
        sweep(w, List.of("nameBox"), List.of());
        pressField(w, "nameBox");
        expect(w, "the name box takes focus on a press",
                () -> screen() != null && screen().getFocused() == get(screen(), "nameBox") ? null : "not focused");
        then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "nameBox")).setValue("Crawl Saddle"));
        eventuallyServer(w, "typing a name sends it to the bench", 40, s ->
                MenuCrawlCases.player(s).containerMenu instanceof EquestrianBenchMenu m
                        ? same("the bench's name", "Crawl Saddle", m.saddleName()) : "the bench menu is not open");
        shiftIn(w, Items.SADDLE, s -> serverSlot(s, 0).is(Items.SADDLE) ? null : "no saddle in the tack slot");
        shiftIn(w, Items.RED_DYE, s -> serverSlot(s, 4).isEmpty() ? "no dyed saddle in the result" : null);
        close();
        // Closing without taking the result hands every input back: nothing is spent.
        eventuallyServer(w, "closing the bench gives the saddle back", 40,
                s -> same("saddles in the inventory", saddles[0], count(s, Items.SADDLE)));
    }

    // ------------------------------------------------------------------ the stasis bank

    private static void bank() {
        String w = "HorseStasisBankScreen";
        openBlock(w, HorseStasisBankScreen.class, () -> DebugMenuCrawl.bank);
        for (String t : List.of("BROWSE", "SUPPLY", "CHAMBERS")) {
            pressAt(w, "the " + t + " tab", () -> tabPoint(HorseStasisBankMenu.class, "Tab", t));
            expect(w, "the " + t + " tab opens", () -> same("tab", t, name(get(Objects.requireNonNull(screen()), "tab"))));
            layout(w + " " + t);
            if (t.equals("BROWSE")) {
                sweep(w + " Browse", List.of("filterBox", "buildButton", "savedSearchButton"), List.of());
            } else if (t.equals("CHAMBERS")) {
                sweep(w + " Chambers", List.of("ejectButton"), List.of());
            } else {
                sweep(w + " Supply", List.of(), List.of());
                shiftIn(w, Items.WHEAT, s -> bankAt(s).supplies().isEmpty() ? "nothing went into the supplies" : null);
            }
        }

        // Chambers: Eject empties takes the empty chamber out.
        pressField(w, "ejectButton");
        eventuallyServer(w, "Eject empties takes the empty chamber out of the bank", 40,
                s -> bankAt(s).chambers().getItem(2).isEmpty() ? null : "slot 2 still holds " + bankAt(s).chambers().getItem(2));

        // Browse: a right-click puts a spacer to stud (and is refused for a basic); a left-click withdraws.
        pressAt(w, "the BROWSE tab", () -> tabPoint(HorseStasisBankMenu.class, "Tab", "BROWSE"));
        eventually(w, "the Browse list has the two filed horses", 60, () -> {
            List<?> shown = (List<?>) get(Objects.requireNonNull(screen()), "shown");
            return shown.size() >= 2 ? null : shown.size() + " rows";
        });
        pressAt(w, "the spacer's row, right button", () -> bankRow(0), 1, 0);
        eventuallyServer(w, "a right-click puts a spacer chamber to stud", 40,
                s -> com.example.horsegenetics.neoforge.server.StasisStud.atStud(bankAt(s).chambers().getItem(0))
                        ? null : "not at stud");
        pressAt(w, "the basic chamber's row, right button", () -> bankRow(1), 1, 0);
        expect(w, "a right-click on a basic chamber says it cannot stand at stud",
                () -> String.valueOf(get(Objects.requireNonNull(screen()), "note")).isEmpty() ? "no note" : null);
        pressField(w, "savedSearchButton");
        expect(w, "the saved-search arrow opens the menu",
                () -> (Boolean) call(get(Objects.requireNonNull(screen()), "savedSearches"), "isOpen") ? null : "not open");
        pressAt(w, "outside the menu", () -> new double[] {2, 2});
        pressField(w, "filterBox");
        expect(w, "the filter box takes focus on a press",
                () -> screen() != null && screen().getFocused() == get(screen(), "filterBox") ? null : "not focused");
        pressAt(w, "the basic chamber's row", () -> bankRow(1));
        eventuallyServer(w, "a left-click withdraws that chamber", 40,
                s -> bankAt(s).chambers().getItem(1).isEmpty() ? null : "slot 1 still holds a horse");
        close();
    }

    // ------------------------------------------------------------------ the gear and pack menus

    private static void gear() {
        String w = "HorseGearScreen";
        then(() -> ClientPacketDistributor.sendToServer(new OpenHorseGearPayload(
                Objects.requireNonNull(MenuCrawlCases.clientHorse(DebugMenuCrawl.mare)).getId())));
        eventually(w, "the gear menu opens", 60, () -> isScreen(HorseGearScreen.class));
        layout(w);
        sweep(w, List.of(), List.of());
        shiftIn(w, Items.SADDLE, s -> {
            Horse h = DebugMenuCrawl.horse(MenuCrawlCases.player(s), DebugMenuCrawl.mare);
            return h != null && !HorseTackSlot.SADDLE.on(h).isEmpty() ? null : "she is not saddled";
        });
        float[] yaw = {0};
        then(() -> yaw[0] = (Float) get(Objects.requireNonNull(screen()), "dollYaw"));
        then(() -> {
            AbstractContainerScreen<?> s = cs();
            // Inside the portrait box, low on the flank: the saddle's slot sits over the
            // middle of the back, and a press there is a slot click, not a turn.
            double x = left(s) + 8 + 216 * 0.36;
            double y = top(s) + 18 + 160 * 0.52;
            drag(x, y, 20, 0);
        });
        pause(SETTLE);
        expect(w, "dragging the portrait turns the horse",
                () -> Objects.equals(yaw[0], get(Objects.requireNonNull(screen()), "dollYaw")) ? "unchanged" : null);
        close();
    }

    private static void pack() {
        String w = "HorsePackScreen";
        // A plain chest opens vanilla's chest menu; a decorated pot (one slot) is a size the
        // vanilla menus have no screen for, so it is the way onto this one.
        serverDo(w, s -> {
            ServerPlayer p = MenuCrawlCases.player(s);
            Horse h = Objects.requireNonNull(DebugMenuCrawl.horse(p, DebugMenuCrawl.stallion));
            HorseTackSlot.HARNESS.set(h, new ItemStack(ModItems.IRON_STORAGE_HARNESS.get()));
            HorseTackSlot.SADDLEBAG_LEFT.set(h, new ItemStack(Items.DECORATED_POT));
            HorsePackHandler.open(p, h, HorseTackSlot.SADDLEBAG_LEFT);
            return null;
        });
        eventually(w, "a decorated pot on the flank opens the pack menu", 60, () -> isScreen(HorsePackScreen.class));
        then(() -> {
            if (!(screen() instanceof HorsePackScreen)) {
                skip(w, "the rest - the menu did not open (" + name(screen()) + "), so the checks below fail");
            }
        });
        layout(w);
        sweep(w, List.of(), List.of());
        shiftIn(w, Items.STICK, s -> cs().getMenu().slots.get(0).getItem().isEmpty() ? "the pot is still empty" : null);
        close();
    }

    private static void cart(String w, Class<?> screen, java.util.function.Supplier<@Nullable UUID> id, Item item) {
        serverDo(w, s -> {
            DebugMenuCrawl.openCart(MenuCrawlCases.player(s), id.get());
            return null;
        });
        eventually(w, "the cart's inventory opens", 60, () -> isScreen(screen));
        layout(w);
        sweep(w, List.of(), List.of());
        shiftIn(w, item, s -> {
            Entity e = DebugMenuCrawl.entity(MenuCrawlCases.player(s), id.get());
            if (!(e instanceof Container c)) {
                return "the cart is gone";
            }
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (c.getItem(i).is(item)) {
                    return null;
                }
            }
            return "no " + item + " in the cart";
        });
        close();
    }

    // ------------------------------------------------------------------ helpers

    private static void openBlock(String w, Class<?> screen, java.util.function.Supplier<BlockPos> pos) {
        serverDo(w, s -> {
            DebugMenuCrawl.openBlock(MenuCrawlCases.player(s), pos.get());
            return null;
        });
        eventually(w, "a right-click on the block opens its menu", 60, () -> isScreen(screen));
    }

    private static void layout(String w) {
        pause(SETTLE);
        expect(w, "every slot sits inside the window, none overlapping", () -> {
            Screen s = screen();
            return s instanceof AbstractContainerScreen<?> c ? slotLayout(c) : isScreen(AbstractContainerScreen.class);
        });
    }

    /**
     * Shift-click {@code item} from the player's inventory into the menu - at the slot's
     * screen position, with shift held - and check where it went on the server.
     */
    private static void shiftIn(String w, Item item, Function<IntegratedServer, @Nullable String> landed) {
        pressAt(w, "shift-click " + item + " from the inventory", () -> {
            AbstractContainerScreen<?> s = cs();
            List<Slot> slots = s.getMenu().slots;
            for (Slot slot : slots) {
                if (slot.isActive() && slot.container instanceof net.minecraft.world.entity.player.Inventory
                        && slot.getItem().is(item)) {
                    return slotCentre(s, slot);
                }
            }
            return null;
        }, 0, SHIFT);
        eventuallyServer(w, "a shift-clicked " + item + " goes where it belongs", 40, landed);
    }

    private static int count(IntegratedServer s, Item item) {
        var inv = MenuCrawlCases.player(s).getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    private static AbstractContainerScreen<?> cs() {
        return (AbstractContainerScreen<?>) Objects.requireNonNull(screen(), "no screen");
    }

    private static Object menu() {
        return cs().getMenu();
    }

    private static Slot menuSlot(int i) {
        return cs().getMenu().slots.get(i);
    }

    private static ItemStack serverSlot(IntegratedServer s, int i) {
        return MenuCrawlCases.player(s).containerMenu.slots.get(i).getItem();
    }

    private static double @Nullable [] tabPoint(Class<?> enumOwner, String enumName, String constant) {
        Screen s = Objects.requireNonNull(screen());
        Object want = enumConstant(enumOwner, enumName, constant);
        return scan(s, hitTest(s, "tabAt"), t -> t == want);
    }

    private static Button[] styleButtons() {
        return (Button[]) get(Objects.requireNonNull(screen()), "styleButtons");
    }

    private static double @Nullable [] bankRow(int slot) {
        AbstractContainerScreen<?> s = cs();
        List<?> shown = (List<?>) get(s, "shown");
        int scroll = (Integer) get(s, "scroll");
        for (int i = 0; i < shown.size(); i++) {
            if ((Integer) call(shown.get(i), "slot") == slot) {
                return new double[] {left(s) + 8 + 60, top(s) + 36 + (i - scroll) * 22 + 11};
            }
        }
        return null;
    }

    private static @Nullable EquineResearchShelfBlockEntity shelfAt(IntegratedServer s) {
        return MenuCrawlCases.player(s).level().getBlockEntity(DebugMenuCrawl.shelf) instanceof EquineResearchShelfBlockEntity sh
                ? sh : null;
    }

    private static JumpBlockEntity jumpAt(IntegratedServer s) {
        return (JumpBlockEntity) Objects.requireNonNull(MenuCrawlCases.player(s).level().getBlockEntity(DebugMenuCrawl.jump));
    }

    private static HorseStasisBankBlockEntity bankAt(IntegratedServer s) {
        return (HorseStasisBankBlockEntity) Objects.requireNonNull(
                MenuCrawlCases.player(s).level().getBlockEntity(DebugMenuCrawl.bank));
    }
}
