package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.client.ClientHorseRecordCache;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * <b>What the menu crawler runs</b>, in order: build the yard, then one case per screen.
 * Each screen's case is its own class ({@code *Crawl}) and every one of them starts from no
 * screen and ends with none, so a case that goes wrong half-way does not leave the next one
 * standing on its screen.
 */
final class MenuCrawlCases {

    private MenuCrawlCases() {
    }

    static void queueAll() {
        if (LayoutAudit.enabled()) {
            // The audit's own arithmetic, once: black on white is WCAG's 21:1.
            then(() -> check("layout", "the contrast sum is right (white on black is 21:1)",
                    Math.abs(LayoutAudit.contrast(0xFFFFFF, 0x000000) - 21.0) < 0.01 ? null
                            : "it says " + LayoutAudit.contrast(0xFFFFFF, 0x000000)));
        }
        setUp();
        // -PmenuCrawl=queries runs the query crawl alone (it needs nothing the others leave).
        if (String.valueOf(System.getProperty(MenuCrawlClient.PROPERTY)).contains("queries")) {
            QueryCrawl.queue();
            then(() -> pass("crawler", "every case ran to the end"));
            return;
        }
        SpawnScreenCrawl.queue();
        RenameCrawl.queue();
        InfoScreenCrawl.queue();
        FamilyCrawl.queue();
        WheelCrawl.queue();
        BrowserCrawl.queue();
        ContainerCrawl.queue();
        QueryCrawl.queue();
        then(() -> pass("crawler", "every case ran to the end"));
    }

    // ------------------------------------------------------------------ the yard

    private static void setUp() {
        serverDo("setup", s -> {
            DebugMenuCrawl.build(player(s));
            return null;
        });
        pause(40);
        // The founding tick has to have run before a sex is stamped (test-tiers: a record
        // applied before it is rolled over).
        eventuallyServer("setup", "the three horses are founded, tamed and owned", 400,
                s -> DebugMenuCrawl.finishHorses(player(s)) ? null : "not founded yet");
        serverDo("setup", s -> {
            DebugMenuCrawl.stock(player(s));
            DebugMenuCrawl.home(player(s));
            return null;
        });
        eventually("setup", "the client has the mare's record, owned by the player", 400, () -> {
            Horse m = clientHorse(DebugMenuCrawl.mare);
            if (m == null) {
                return "the mare is not on the client";
            }
            HorseRecord r = ClientHorseRecordCache.get(m.getId());
            if (r == null) {
                return "no record for the mare on the client";
            }
            return r.ownedBy(Objects.requireNonNull(mc().player).getUUID()) ? null : "the record has no owner yet";
        });
    }

    // ------------------------------------------------------------------ shared reads

    static ServerPlayer player(IntegratedServer server) {
        return Objects.requireNonNull(server.getPlayerList().getPlayer(Objects.requireNonNull(mc().player).getUUID()),
                "the player is not on the server");
    }

    /** The client's copy of an entity, by UUID. */
    static @Nullable Horse clientHorse(@Nullable UUID id) {
        return clientEntity(id) instanceof Horse h ? h : null;
    }

    static @Nullable Entity clientEntity(@Nullable UUID id) {
        if (id == null || mc().level == null) {
            return null;
        }
        for (Entity e : mc().level.entitiesForRendering()) {
            if (e.getUUID().equals(id)) {
                return e;
            }
        }
        return null;
    }

    static @Nullable HorseRecord clientRecord(@Nullable UUID id) {
        Horse h = clientHorse(id);
        return h == null ? null : ClientHorseRecordCache.get(h.getId());
    }
}
