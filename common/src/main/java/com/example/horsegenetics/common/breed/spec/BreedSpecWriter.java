package com.example.horsegenetics.common.breed.spec;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedHerd;
import com.example.horsegenetics.common.breed.BreedBands;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.breed.SpawnTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Writes a {@link Breed} back out as the JSON {@link BreedSpecParser} reads.
 *
 * <p>It exists for two jobs and they pull the same way: it is how the 48
 * built-in breeds became files in the first place (one run of
 * {@code :common:bakeBreedFiles}, rather than 48 hand translations of an
 * 885-line Java class), and it is how the breed designer's "export" button will
 * one day be checked - a file that does not round-trip through
 * {@code parse -> write -> parse} is a file the game and the tool disagree
 * about.
 *
 * <p><b>It omits every field that is at its default.</b> A breed file should
 * read as the handful of decisions someone actually made, not as a form with
 * every box filled in; a file that spells out {@code "commonness": "moderate"} tells
 * a later reader that the tier was chosen, which is a lie.
 */
public final class BreedSpecWriter {

    private BreedSpecWriter() {
    }

    public static String write(Breed breed) {
        StringBuilder out = new StringBuilder(1024);
        out.append("{\n");
        List<String> fields = new ArrayList<>();

        fields.add(field("id", quote(breed.id())));
        fields.add(field("name", quote(breed.name())));
        if (!breed.description().isEmpty()) {
            fields.add(field("description", quote(breed.description())));
        }
        if (breed.magical()) {
            fields.add(field("kind", quote("magical")));
        }
        if (!breed.magicalVariant()) {
            fields.add(field("magical_variant", "false"));   // true is the default, and is never written
        }
        Commonness tier = Commonness.forWeight(breed.spawnWeight());
        if (tier.weight == breed.spawnWeight()) {
            if (tier != Commonness.MODERATE) {
                fields.add(field("commonness", quote(tier.name().toLowerCase(java.util.Locale.ROOT))));
            }
        } else {
            fields.add(field("spawn_weight", number(breed.spawnWeight())));
        }
        if (!breed.sources().equals(BreedSource.ALL)) {
            List<String> tokens = new ArrayList<>();
            for (BreedSource s : BreedSource.values()) {
                if (breed.sources().contains(s)) {
                    tokens.add(quote(s.id()));
                }
            }
            fields.add(field("spawn", inlineArray(tokens)));
        }
        if (breed.spawnTime() != SpawnTime.ANY) {
            fields.add(field("spawn_time", quote(breed.spawnTime().id())));
        }
        String herd = herd(breed.herd());
        if (herd != null) {
            fields.add(field("herd", herd));
        }
        if (!breed.biomes().isEmpty()) {
            fields.add(field("biomes", blockArray(quoteAll(breed.biomes()), 2)));
        }
        Optional<Breed.PriceRange> price = breed.price();
        if (price.isPresent()) {
            fields.add(field("price", inlineArray(List.of(
                    String.valueOf(price.get().min()), String.valueOf(price.get().max())))));
        }
        String stats = stats(breed.scores());
        if (stats != null) {
            fields.add(field("stats", stats));
        }
        if (!breed.genePools().isEmpty()) {
            fields.add(field("genes", genes(breed.genePools(), 2)));
        }
        if (!breed.strains().isEmpty()) {
            fields.add(field("strains", strains(breed.strains())));
        }
        if (!breed.bands().isEmpty()) {
            fields.add(field("bands", bands(breed.bands())));
        }
        if (!breed.notes().isEmpty()) {
            fields.add(field("notes", blockArray(quoteAll(breed.notes()), 2)));
        }

        out.append(String.join(",\n", fields));
        out.append("\n}\n");
        return out.toString();
    }

    // ------------------------------------------------------------------

    private static String stats(Breed.StatScores scores) {
        if (scores.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        scores.speed().ifPresent(r -> parts.add(field("speed", range(r), 4)));
        scores.jump().ifPresent(r -> parts.add(field("jump", range(r), 4)));
        scores.health().ifPresent(r -> parts.add(field("health", range(r), 4)));
        scores.size().ifPresent(r -> parts.add(field("size", range(r), 4)));
        return "{\n" + String.join(",\n", parts) + "\n  }";
    }

    /** The {@code herd} block, or {@code null} when the breed founds herds the ordinary way. */
    private static String herd(BreedHerd h) {
        List<String> parts = new ArrayList<>();
        if (h.bachelorChance() != BreedHerd.DEFAULT_BACHELOR_CHANCE) {
            parts.add(field("bachelor_chance", number(h.bachelorChance()), 4));
        }
        if (!h.traditional().isDefault(false)) {
            parts.add(field("traditional", band(h.traditional()), 4));
        }
        if (!h.bachelor().isDefault(true)) {
            parts.add(field("bachelor", band(h.bachelor()), 4));
        }
        return parts.isEmpty() ? null : "{\n" + String.join(",\n", parts) + "\n  }";
    }

    /** One band's two ranges; a zero-width range reads as the single number it is. */
    private static String band(BreedHerd.Band b) {
        return "{ \"stallions\": " + counts(b.minStallions(), b.maxStallions())
                + ", \"mares\": " + counts(b.minMares(), b.maxMares()) + " }";
    }

    private static String counts(int lo, int hi) {
        return lo == hi ? String.valueOf(lo) : "[" + lo + ", " + hi + "]";
    }

    private static String range(Breed.Range r) {
        return r.isPoint() ? number(r.lo()) : inlineArray(List.of(number(r.lo()), number(r.hi())));
    }

    /** A {@code genes} object whose closing brace sits at {@code indent} spaces. */
    private static String genes(Map<String, List<Breed.Combo>> pools, int indent) {
        String keyPad = " ".repeat(indent + 2);
        String comboPad = " ".repeat(indent + 4);
        List<String> byGene = new ArrayList<>();
        for (Map.Entry<String, List<Breed.Combo>> e : pools.entrySet()) {
            List<String> combos = new ArrayList<>();
            for (Breed.Combo c : e.getValue()) {
                combos.add(comboPad + "{ \"pair\": " + quote(c.a() + "/" + c.b())
                        + ", \"weight\": " + number(c.weight()) + " }");
            }
            byGene.add(keyPad + quote(e.getKey()) + ": [\n"
                    + String.join(",\n", combos) + "\n" + keyPad + "]");
        }
        return "{\n" + String.join(",\n", byGene) + "\n" + " ".repeat(indent) + "}";
    }

    private static String strains(List<Breed.Strain> strains) {
        List<String> out = new ArrayList<>();
        for (Breed.Strain s : strains) {
            List<String> parts = new ArrayList<>();
            parts.add(field("name", quote(s.name()), 6));
            parts.add(field("weight", number(s.weight()), 6));
            if (!s.genePools().isEmpty()) {
                parts.add(field("genes", genes(s.genePools(), 6), 6));
            }
            out.add("    {\n" + String.join(",\n", parts) + "\n    }");
        }
        return "[\n" + String.join(",\n", out) + "\n  ]";
    }

    private static String bands(BreedBands bands) {
        List<String> byGene = new ArrayList<>();
        for (String key : bands.genes()) {
            List<String> values = new ArrayList<>();
            for (Map.Entry<String, BreedBands.Band> e : bands.forGene(key).entrySet()) {
                BreedBands.Band band = e.getValue();
                // A zero-width band is a lock, and reads as one: a single number.
                values.add("      " + quote(e.getKey()) + ": " + (band.lo() == band.hi()
                        ? number(band.lo())
                        : inlineArray(List.of(number(band.lo()), number(band.hi())))));
            }
            for (Map.Entry<String, Long> e : bands.seedsFor(key).entrySet()) {
                // A string, because a JSON number is a double and most seeds do
                // not survive the trip through one.
                values.add("      " + quote(e.getKey()) + ": " + quote(String.valueOf(e.getValue())));
            }
            byGene.add("    " + quote(key) + ": {\n" + String.join(",\n", values) + "\n    }");
        }
        return "{\n" + String.join(",\n", byGene) + "\n  }";
    }

    // ------------------------------------------------------------------

    private static String field(String key, String value) {
        return field(key, value, 2);
    }

    private static String field(String key, String value, int indent) {
        return " ".repeat(indent) + quote(key) + ": " + value;
    }

    private static List<String> quoteAll(Iterable<String> values) {
        List<String> out = new ArrayList<>();
        for (String s : values) {
            out.add(quote(s));
        }
        return out;
    }

    private static String inlineArray(List<String> values) {
        return "[" + String.join(", ", values) + "]";
    }

    private static String blockArray(List<String> values, int indent) {
        String pad = " ".repeat(indent + 2);
        return "[\n" + pad + String.join(",\n" + pad, values) + "\n" + " ".repeat(indent) + "]";
    }

    /** The shortest exact spelling - {@code 9} rather than {@code 9.0}, which reads as a measurement. */
    static String number(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        String s = String.valueOf(d);
        return s;
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2);
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // Hand-rolled rather than String.format: this class is
                        // compiled to WebAssembly for the breed designer, and
                        // TeaVM links the reachable graph - a whole formatter
                        // pulled in for a case that never fires is a large
                        // dependency bought for nothing.
                        out.append("\\u00").append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
