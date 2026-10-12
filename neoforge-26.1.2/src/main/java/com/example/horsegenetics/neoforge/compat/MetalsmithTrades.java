package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <b>What the generated pack adds to the equestrian metalsmith's stock</b>:
 * the modded horse armours ({@link GeneratedArmour}) and the modded horse shoes
 * ({@link GeneratedShoes}), gathered here so that each tier file is written once.
 *
 * <h2>The trades are rewritten, not appended to</h2>
 * {@code minecraft:trade_set} is a datapack <i>registry</i>, so two packs
 * declaring the same tier do not merge - the higher one wins outright. The
 * generated tier file therefore has to carry the shipped trades as well as the
 * new ones, and it gets them by <b>reading the shipped file off our own
 * classpath</b> rather than by keeping a second copy of the list here. Edit the
 * metalsmith's tiers and this follows; it cannot go stale.
 *
 * <p>For the same reason there is one of these per pack build and not one per
 * family: armour and shoes each writing {@code level_3.json} would be the
 * second silently replacing the first.
 */
final class MetalsmithTrades {

    private static final String NS = HorseGenetics.MOD_ID;
    private static final String METALSMITH = "equestrian_metalsmith";

    /** The ids added to each tier, 1 to 5; null where nothing was. */
    private final JsonArray[] added = new JsonArray[6];

    /**
     * Write one "sells {@code id} for emeralds" trade and list it at {@code tier}.
     *
     * @param tier 1 to 5, the metalsmith's level
     * @param id   the path of one of this mod's items
     */
    void sell(Map<String, byte[]> files, int tier, String id, int emeralds, int maxUses, int xp) {
        JsonObject wants = new JsonObject();
        wants.addProperty("id", "minecraft:emerald");
        wants.addProperty("count", emeralds);
        JsonObject gives = new JsonObject();
        gives.addProperty("id", NS + ":" + id);
        JsonObject trade = new JsonObject();
        trade.add("wants", wants);
        trade.add("gives", gives);
        trade.addProperty("max_uses", maxUses);
        trade.addProperty("xp", xp);
        GeneratedGates.put(files,
                "data/" + NS + "/villager_trade/" + METALSMITH + "/level_" + tier + "/sell_" + id + ".json",
                trade);
        if (added[tier] == null) {
            added[tier] = new JsonArray();
        }
        added[tier].add(NS + ":" + METALSMITH + "/level_" + tier + "/sell_" + id);
    }

    /** Write every tier file that gained a trade. Call once, after every family has sold. */
    void emit(Map<String, byte[]> files) {
        for (int tier = 1; tier <= 5; tier++) {
            if (added[tier] != null) {
                emitTradeSet(files, tier, added[tier]);
            }
        }
    }

    /**
     * The tier file, shipped trades plus generated ones.
     *
     * <p>Read off our own classpath rather than restated here - see the class
     * note. If the shipped file cannot be read the generated trades are written
     * on their own, which loses the shipped trades from that tier but keeps the
     * modded ones; the alternative is a tier that is empty for everybody.
     */
    private static void emitTradeSet(Map<String, byte[]> files, int tier, JsonArray added) {
        String path = "/data/" + NS + "/trade_set/" + METALSMITH + "/level_" + tier + ".json";
        JsonObject tierFile = null;
        try (InputStream in = MetalsmithTrades.class.getResourceAsStream(path)) {
            if (in != null) {
                JsonElement parsed = JsonParser.parseString(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    tierFile = parsed.getAsJsonObject();
                }
            }
        } catch (Exception unreadable) {
            HorseGenetics.LOGGER.warn("compat: could not read the shipped {} - "
                    + "its modded armours and shoes will be the only trades in that tier", path, unreadable);
        }
        if (tierFile == null) {
            tierFile = new JsonObject();
            tierFile.addProperty("amount", 3.0);
            tierFile.add("trades", new JsonArray());
            tierFile.addProperty("random_sequence", NS + ":trade_set/" + METALSMITH + "/level_" + tier);
        }
        JsonArray trades = tierFile.getAsJsonArray("trades");
        for (JsonElement id : added) {
            trades.add(id);
        }
        GeneratedGates.put(files,
                "data/" + NS + "/trade_set/" + METALSMITH + "/level_" + tier + ".json", tierFile);
    }
}
