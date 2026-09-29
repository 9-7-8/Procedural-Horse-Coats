package com.example.horsegenetics.common.parts;

import com.example.horsegenetics.common.SeededRng;
import java.util.List;

/**
 * The one door the client uses: (kind, size, seed) -> boxes. Deterministic, so the server,
 * the client and the wasm designer would all agree on the shape without shipping it.
 */
public final class PartGenerators {

    private PartGenerators() {
    }

    /** @param size 0..1; means "length" for a horn, "tine count and reach" for antlers, "crystal size" for crystals */
    public static List<PartNode> build(PartKind kind, double size, long seed) {
        SeededRng rng = new SeededRng(seed, kind.name());
        return switch (kind) {
            case HORN -> HornGenerator.generate(HornSize.lengthFor(size), 22f, 3f, 0.15f);
            case ANTLERS -> AntlerGenerator.generate(rng, size);
            case CRYSTALS -> CrystalGenerator.generate(rng, 3 + (int) Math.round(4 * size), size);
        };
    }
}
