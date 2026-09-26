package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Where each player's stasis banks are, and which of them are insuring
 * anything.</b>
 *
 * <p>An emergency chamber in your pocket is found by walking your inventory,
 * which is free. An emergency chamber <i>filed in a bank</i> is not: the bank is
 * a block somewhere in some dimension, quite possibly in a chunk nobody has
 * loaded for a week. Without this there is no way from "this horse is about to
 * die" to "and there is an empty bottle for it in the cabinet at home" that is
 * not a search of the world, which is the one thing the whole stasis feature
 * exists to avoid.
 *
 * <h2>Two fields, and the second is the point</h2>
 * A row is a bank's <b>position</b> and its <b>owner</b> - who placed it, since a
 * bank is otherwise unowned and spending a stranger's chamber is theft. That much
 * is only a phone book. What makes it cheap is {@link Bank#armed}: the bank
 * publishes, every time its chamber grid changes and again whenever it loads,
 * whether it currently holds an <i>empty emergency chamber</i>. So the common
 * answer - no, none of your banks can help - is a walk of a short list in memory
 * and <b>no chunk is touched at all</b>.
 *
 * <p>{@link Bank#held} is that same trick asked the other way round - <i>which
 * horses are in there</i> - published on the same two occasions, for
 * {@link #bankHolding}. It is a list rather than a flag because the question has
 * a subject; everything else about it is the armed flag's reasoning verbatim.
 *
 * <p>A chunk is loaded only for a bank this index says is armed, which is the
 * moment a horse is about to die and there is genuinely a bottle waiting for it.
 * That is a price worth paying once; paying it to find out the answer is no is
 * not.
 *
 * <h2>The flag can be stale, and the reader copes</h2>
 * The flag is last-known, not live. A bank whose chambers were changed by
 * something that did not go through its own container - there is nothing like
 * that today, but a future mod's pipe would be - can say armed when it is not.
 * That costs one wasted chunk load and the reader finds nothing, which is why
 * every use of this re-reads the real container rather than trusting the flag
 * with a horse.
 *
 * <p><b>Not verified in-game.</b> Written against the 26.1.2 sources.
 */
public final class StasisBankIndex extends SavedData {

    /**
     * One bank.
     *
     * @param owner whoever placed it, or claimed it by opening it while it was
     *              unclaimed; {@code null} for a bank nobody has done either to
     * @param armed it held an empty emergency chamber when it was last looked at
     * @param held  the horses filed here when it was last looked at - see
     *              {@link #bankHolding}
     */
    public record Bank(@Nullable UUID owner, ResourceKey<Level> dimension, BlockPos pos,
                       boolean armed, List<UUID> held) {
        public Bank {
            held = held == null ? List.of() : List.copyOf(held);
        }

        public static final Codec<Bank> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(b -> Optional.ofNullable(b.owner())),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Bank::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Bank::pos),
                Codec.BOOL.optionalFieldOf("armed", false).forGetter(Bank::armed),
                UUIDUtil.CODEC.listOf().optionalFieldOf("held", List.of()).forGetter(Bank::held)
        ).apply(i, (owner, dimension, pos, armed, held) ->
                new Bank(owner.orElse(null), dimension, pos, armed, held)));
    }

    public static final Codec<StasisBankIndex> CODEC = RecordCodecBuilder.create(i -> i.group(
            Bank.CODEC.listOf().fieldOf("banks").forGetter(StasisBankIndex::snapshot)
    ).apply(i, StasisBankIndex::new));

    public static final SavedDataType<StasisBankIndex> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "stasis_bank_index"),
            StasisBankIndex::new,
            CODEC);

    /**
     * Insertion-ordered, so {@link #armedBanksOf} can walk a player's banks
     * newest first: the one you placed last is the one you have most likely just
     * stocked with empties.
     */
    private final Map<String, Bank> banks = new LinkedHashMap<>();

    private StasisBankIndex() {
    }

    private StasisBankIndex(List<Bank> saved) {
        for (Bank bank : saved) {
            banks.put(key(bank.dimension(), bank.pos()), bank);
        }
    }

    public static StasisBankIndex get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Bank> snapshot() {
        return new ArrayList<>(banks.values());
    }

    private static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.identifier() + "@" + pos.asLong();
    }

    /**
     * <b>A bank now exists here.</b> Called when one is placed, when one loads,
     * and when an unclaimed one is opened - all three, because the index has to
     * be right about a world that predates it as well as one that does not.
     *
     * <p>An {@code owner} of {@code null} never overwrites one already recorded:
     * loading a bank must not un-claim it.
     */
    public void record(@Nullable UUID owner, ResourceKey<Level> dimension, BlockPos pos,
                       boolean armed, List<UUID> held) {
        String k = key(dimension, pos);
        Bank before = banks.get(k);
        UUID keep = owner != null ? owner : (before == null ? null : before.owner());
        if (before != null && before.armed() == armed
                && java.util.Objects.equals(before.owner(), keep)
                && before.held().equals(held)) {
            return;
        }
        banks.put(k, new Bank(keep, dimension, pos.immutable(), armed, held));
        setDirty();
    }

    /** It was broken. */
    public void forget(ResourceKey<Level> dimension, BlockPos pos) {
        if (banks.remove(key(dimension, pos)) != null) {
            setDirty();
        }
    }

    /** Who this bank belongs to, or {@code null} for one nobody has claimed. */
    public @Nullable UUID ownerOf(ResourceKey<Level> dimension, BlockPos pos) {
        Bank bank = banks.get(key(dimension, pos));
        return bank == null ? null : bank.owner();
    }

    /**
     * <b>This player's banks that last looked like they could save a horse</b>,
     * newest-placed first.
     *
     * <p>The filter on {@link Bank#armed} is what keeps this off the disk: a
     * player with twenty banks and no emergency chambers in any of them gets an
     * empty list out of a twenty-element walk, and nothing is loaded. Only a
     * bank that says it is holding an empty emergency chamber is worth the chunk
     * it would cost to ask properly.
     */
    public List<Bank> armedBanksOf(UUID owner) {
        List<Bank> out = new ArrayList<>();
        for (Bank bank : banks.values()) {
            if (bank.armed() && owner.equals(bank.owner())) {
                out.add(bank);
            }
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /**
     * <b>Which of this player's banks has that horse filed in it</b>, or
     * {@code null} if none of them says so.
     *
     * <p>The browser's <i>Send home</i> button is what this is for: a horse in a
     * chamber has no entity to reach for and its last sighting is where it went
     * <i>in</i>, not where the bottle ended up, so without this the only way from
     * a row in a table to the jar holding it would be to load every bank the
     * player owns and look. {@link Bank#held} is the same trick as
     * {@link Bank#armed} pointed at a different question: the bank publishes what
     * it is holding every time its grid changes, so the usual answer - no, not in
     * any of them - is a walk of a short list in memory and <b>no chunk is
     * touched at all</b>, and the answer yes names exactly one chunk to load.
     *
     * <p>Like the armed flag, this is <b>last-known and the caller re-reads the
     * real container</b>: a bank that says it holds a horse may have been emptied
     * by something that did not go through its own {@code setChanged}, and the
     * only honest check is opening the grid and looking.
     */
    public @Nullable Bank bankHolding(UUID owner, UUID horse) {
        Bank found = null;
        for (Bank bank : banks.values()) {
            if (owner.equals(bank.owner()) && bank.held().contains(horse)) {
                found = bank;   // newest wins, as armedBanksOf prefers it
            }
        }
        return found;
    }
}
