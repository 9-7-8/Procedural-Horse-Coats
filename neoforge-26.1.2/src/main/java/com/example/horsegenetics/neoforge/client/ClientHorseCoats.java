package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.neoforge.network.HorseCoatBatchPayload;
import com.example.horsegenetics.neoforge.network.HorseCoatRequestPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Coats for the browser's little horses</b>, fetched a screenful at a time
 * and keyed by <b>record UUID</b>.
 *
 * <p>Deliberately <b>not</b> {@link ClientCoatCache}, which is keyed by entity
 * id and belongs to the renderer: half the horses in a stable have no entity on
 * this client at all, and the standing rule for anything that wants to draw a
 * horse in a screen is to leave that cache alone. This one is the browser's,
 * lives for the session, and is dropped on disconnect like every other cache in
 * this package.
 *
 * <h2>Ask once</h2>
 * {@link #asked} is what stops a render loop asking sixty times a second for a
 * horse that has not answered yet - and it is keyed on the <em>request</em>, not
 * on the reply, so a horse the server cannot answer for (no record, no genome)
 * is asked for once and then left alone rather than re-requested forever.
 */
public final class ClientHorseCoats {

    private static final Map<UUID, CoatData> COATS = new HashMap<>();
    private static final Set<UUID> ASKED = new HashSet<>();

    private ClientHorseCoats() {
    }

    /** The coat, or {@code null} if it has not arrived (or never will). */
    public static CoatData get(UUID id) {
        return id == null ? null : COATS.get(id);
    }

    /**
     * Make sure these horses' coats are on their way. Pass the rows currently on
     * screen; ones already held or already asked about cost nothing, and the
     * rest go out as one request.
     */
    public static void request(List<HorseListing> rows) {
        List<UUID> wanted = null;
        for (HorseListing row : rows) {
            UUID id = row.id();
            if (COATS.containsKey(id) || ASKED.contains(id)) {
                continue;
            }
            if (wanted == null) {
                wanted = new ArrayList<>();
            }
            wanted.add(id);
            ASKED.add(id);
            if (wanted.size() >= HorseCoatRequestPayload.MAX_IDS) {
                break;
            }
        }
        if (wanted != null) {
            ClientPacketDistributor.sendToServer(new HorseCoatRequestPayload(List.copyOf(wanted)));
        }
    }

    /**
     * Pair each returned epigenome with the genotype the roster already holds.
     * A row whose genotype is gone (the roster refreshed underneath the reply)
     * is dropped rather than guessed at.
     */
    public static void accept(List<HorseCoatBatchPayload.Entry> entries) {
        for (HorseCoatBatchPayload.Entry entry : entries) {
            HorseListing row = ClientHorseRoster.byId(entry.id());
            if (row == null) {
                continue;
            }
            try {
                Genotype genotype = row.genotype();
                Epigenome epigenome = Epigenome.parse(entry.epigenomeCode());
                COATS.put(entry.id(), new CoatData(genotype, epigenome));
            } catch (RuntimeException unparseable) {
                // One bad code must not cost the player the rest of the screen.
            }
        }
    }

    public static void clear() {
        COATS.clear();
        ASKED.clear();
    }
}
