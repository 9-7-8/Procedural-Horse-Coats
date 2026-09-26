package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Who has already been paid for which horse in the realm, and who is still
 * owed.</b> The ledger behind {@code /horsebounty} - see
 * {@code server/RealmBountyCommand}.
 *
 * <h2>Why a ledger and not a one-shot</h2>
 * The command exists because turning a horse out only started paying on
 * 2026-09-26, and every horse released before that was given away for nothing.
 * A back-payment is the kind of thing that gets run twice - by a second
 * operator, after a restart, or on purpose once more horses have arrived - so it
 * has to be <b>idempotent per horse</b> rather than per run. The ledger is what
 * makes running it again safe: it pays for the horses not in it, adds them, and
 * says so.
 *
 * <p>Keyed by <b>horse</b>, not by player. A player who donated forty horses
 * should be paid forty times, and the question "has this horse been paid for"
 * is the only one with a stable answer - a player's total is a sum of it.
 *
 * <h2>Owed, for people who are not here</h2>
 * The payment rains emeralds on its recipient, which needs the recipient to be
 * standing somewhere. Most of the people who donated a horse to a shared field
 * are offline when an operator gets round to this. So a payment to somebody
 * absent is <b>recorded as owed</b> and rains on them the moment they next log
 * in; the horse is marked paid either way, because it has been - the debt has
 * simply moved from the field to this file.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class RealmBounty extends SavedData {

    /** One settled line of the ledger: this horse was paid for, to this person. */
    public record Paid(UUID horse, UUID player, String playerName, int emeralds) {

        public static final Codec<Paid> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("horse").forGetter(Paid::horse),
                UUIDUtil.CODEC.fieldOf("player").forGetter(Paid::player),
                Codec.STRING.fieldOf("name").forGetter(Paid::playerName),
                Codec.INT.fieldOf("emeralds").forGetter(Paid::emeralds)
        ).apply(i, Paid::new));
    }

    private final Map<UUID, Paid> paid = new LinkedHashMap<>();
    private final Map<UUID, Integer> owed = new HashMap<>();

    public static final Codec<RealmBounty> CODEC = RecordCodecBuilder.create(i -> i.group(
            Paid.CODEC.listOf().optionalFieldOf("paid", List.of()).forGetter(RealmBounty::ledger),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                    .optionalFieldOf("owed", Map.of()).forGetter(b -> b.owed)
    ).apply(i, RealmBounty::new));

    public static final SavedDataType<RealmBounty> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_bounty"),
            RealmBounty::new,
            CODEC);

    private RealmBounty() {
    }

    private RealmBounty(List<Paid> paid, Map<UUID, Integer> owed) {
        for (Paid line : paid) {
            this.paid.put(line.horse(), line);
        }
        this.owed.putAll(owed);
    }

    public static RealmBounty get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Every settled line, oldest first. What the command prints. */
    public List<Paid> ledger() {
        return new ArrayList<>(paid.values());
    }

    /** Has this horse already been paid for? */
    public boolean isPaid(UUID horse) {
        return paid.containsKey(horse);
    }

    /** Settle one horse. {@code delivered} false means the recipient was away. */
    public void settle(UUID horse, UUID player, String playerName, int emeralds,
                       boolean delivered) {
        paid.put(horse, new Paid(horse, player, playerName, emeralds));
        if (!delivered) {
            owed.merge(player, emeralds, Integer::sum);
        }
        setDirty();
    }

    /** What this player is owed from payments made while they were away. */
    public int owedTo(UUID player) {
        return owed.getOrDefault(player, 0);
    }

    /** They have it now. */
    public void clearOwed(UUID player) {
        if (owed.remove(player) != null) {
            setDirty();
        }
    }
}
