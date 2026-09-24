package com.example.horsegenetics.neoforge.data.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot condition: <b>the table being rolled matches a regular expression</b>.
 *
 * <pre>
 * { "condition": "horsegenetics:loot_table_matches", "pattern": "[^:]+:chests/.+" }
 * </pre>
 *
 * <h2>Why this exists when NeoForge already ships a table-id condition</h2>
 *
 * <p>{@code neoforge:loot_table_id} tests one exact id, so "put this in chest
 * loot" is written as an {@code any_of} of however many ids somebody thought to
 * list. The mod's three injections were written that way and each named eight to
 * ten vanilla chests - which meant that on a heavily modded server, where most of
 * the chests a player opens belong to somebody else's structures, almost nothing
 * the mod adds to loot was reachable. The owner's call was every chest table, any
 * mod; one regex says that and a list of ids cannot.
 *
 * <p>{@link #ANY_CHEST} is the pattern that says it. Namespace-agnostic on
 * purpose: vanilla and nearly every structure mod put their chest tables under a
 * {@code chests/} path, so matching the path rather than the namespace picks up
 * packs nobody here has installed. It deliberately does <i>not</i> match block,
 * entity, fishing or archaeology tables - a horse armour falling out of a sheep
 * is not what was asked for.
 *
 * <p>A malformed pattern is a decode error rather than an exception, so a
 * datapack that mistypes one logs a line and drops that modifier instead of
 * taking the whole pack down with it.
 */
public record LootTableMatchesCondition(Pattern pattern) implements LootItemCondition {

    /** Any chest table in any namespace - {@code minecraft:chests/…}, {@code somemod:chests/…}. */
    public static final String ANY_CHEST = "[^:]+:chests/.+";

    private static final Codec<Pattern> PATTERN_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(Pattern.compile(s));
                } catch (PatternSyntaxException e) {
                    return DataResult.error(() -> "Not a regular expression: " + s);
                }
            },
            Pattern::pattern);

    public static final MapCodec<LootTableMatchesCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PATTERN_CODEC.fieldOf("pattern").forGetter(LootTableMatchesCondition::pattern)
    ).apply(i, LootTableMatchesCondition::new));

    @Override
    public boolean test(LootContext context) {
        // Whole-string match, like the ids this replaces: a pattern is a table
        // shape, not a substring search, and `find()` would make "chests/" match
        // a block table called `mychests/stone`.
        return pattern.matcher(context.getQueriedLootTableId().toString()).matches();
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return MAP_CODEC;
    }
}
