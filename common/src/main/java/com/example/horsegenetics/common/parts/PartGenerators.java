package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The one door from a {@link PartShape} to a list of boxes.</b>
 *
 * <p>Deterministic and game-free, which buys three things at once: the client can
 * bake a mesh whenever it likes and get the same mesh, a unit test can assert the
 * shape of every part the mod can produce without a running game, and the browser
 * designer could preview a horn from the same arithmetic through an
 * {@code @JSExport} rather than a second implementation in JavaScript (hard rule
 * 3).
 *
 * <p>It takes a shape and nothing else on purpose. Everything continuous about a
 * horse's part is applied to the finished mesh as a transform - see
 * {@link AttachedPart} - so this function's output is shared by every horse with
 * the same shape, and there are at most
 * {@code styles x }{@link PartKind#sizeBuckets()} of them per kind.
 */
public final class PartGenerators {

    private PartGenerators() {
    }

    /** The boxes of {@code shape}, parents always before their children. */
    public static List<PartNode> build(PartShape shape) {
        return switch (shape.kind()) {
            case HORN -> HornGenerator.generate(shape.nominalLength(), shape.style());
            case ANTLER_RIGHT, ANTLER_LEFT -> AntlerGenerator.generate(
                    shape.style() / AntlerGenerator.VARIANTS,
                    shape.style() % AntlerGenerator.VARIANTS,
                    shape.size(),
                    shape.kind() == PartKind.ANTLER_LEFT);
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> RamHornGenerator.generate(
                    shape.style() / RamHornGenerator.CURLS,
                    shape.style() % RamHornGenerator.CURLS,
                    shape.size(),
                    shape.kind() == PartKind.RAM_HORN_LEFT);
            case DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT -> DragonHornGenerator.generate(
                    shape.style(), shape.size(), shape.kind() == PartKind.DRAGON_HORN_LEFT);
            case SPINES -> DorsalSpineGenerator.generate(shape.style(), shape.size());
            case SAIL -> SailGenerator.generate(shape.style(), shape.size());
            case PLATES_RIGHT, PLATES_LEFT -> PlateGenerator.generate(
                    shape.style(), shape.size(), shape.kind() == PartKind.PLATES_LEFT);
            // The unicorn horn's chain on the narwhal's own ladder: no new generator.
            case NARWHAL -> HornGenerator.generate(shape.nominalLength(),
                    HornGenerator.narwhalTwist(shape.style()));
            case TUSK_RIGHT, TUSK_LEFT -> TuskGenerator.generate(
                    shape.style(), shape.size(), shape.kind() == PartKind.TUSK_LEFT);
            case SABRE_RIGHT, SABRE_LEFT -> SabreGenerator.generate(
                    shape.style(), shape.size(), shape.kind() == PartKind.SABRE_LEFT);
            case CRYSTALS -> CrystalGenerator.generate(shape.style(), shape.size());
        };
    }

    /**
     * Every shape this mod can ever bake, for a test to walk. Not used at run
     * time - the client bakes lazily, because a world where nobody has bred a
     * unicorn should not hold sixty-four horn meshes.
     */
    public static List<PartShape> allShapes() {
        List<PartShape> out = new ArrayList<>();
        for (PartKind kind : PartKind.values()) {
            for (int style = 0; style < kind.styles(); style++) {
                for (int size = 0; size < kind.sizeBuckets(); size++) {
                    out.add(new PartShape(kind, style, size));
                }
            }
        }
        return out;
    }
}
