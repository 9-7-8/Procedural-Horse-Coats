package com.example.horsegenetics.common.testutil;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * Builds an {@link EpiValues} for a test - the replacement for handing a gene a
 * {@link FakeRng} with a scripted list of draws.
 *
 * <p>The old shape was positional: a test pinned a face marking by writing
 * {@code floats(0.99f, 0.99f, 0.99f, 0.0f, ...)} and counting, which meant it
 * silently tested something else the moment a gene's draw order moved. Values
 * are named now, so a test says which one it means:
 *
 * <pre>{@code
 * EpiValues bare = Epi.of(WhitePattern.faceSchema(),
 *         "star", 0.99, "stripe", 0.99, "snip", 0.99);
 * }</pre>
 *
 * <p>Anything not named keeps the schema's midpoint, so a test only writes down
 * the values it is actually making a claim about.
 */
public final class Epi {

    private Epi() {
    }

    /** Midpoints, with the named values overridden. Arguments are name, value pairs. */
    public static EpiValues of(EpiSchema schema, Object... nameThenValue) {
        return override(schema.midpoint(), nameThenValue);
    }

    /** A reproducible founder roll, with the named values overridden. */
    public static EpiValues seeded(EpiSchema schema, long seed, Object... nameThenValue) {
        return override(EpiRoll.founder(schema, new SeededRng(seed)), nameThenValue);
    }

    private static EpiValues override(EpiValues values, Object... nameThenValue) {
        if (nameThenValue.length % 2 != 0) {
            throw new IllegalArgumentException("expected name, value pairs");
        }
        for (int i = 0; i < nameThenValue.length; i += 2) {
            String name = (String) nameThenValue[i];
            Object v = nameThenValue[i + 1];
            values = v instanceof Long seed
                    ? values.withSeed(name, seed)
                    : values.with(name, ((Number) v).doubleValue());
        }
        return values;
    }
}
