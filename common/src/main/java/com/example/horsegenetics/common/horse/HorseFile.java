package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.Json;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;

import java.util.List;
import java.util.Map;

/**
 * <b>A whole horse as one string</b> - the thing the two gene editors hand out
 * and take back.
 *
 * <p>Both editors used to have two ways to do this and they were not equal. The
 * spawn egg copied a genotype code to the clipboard; the designer wrote a JSON
 * file <i>and</i> copied a genotype code. A genotype code is the alleles and
 * nothing else - paste one and the horse comes back with a different epigenome,
 * a different name and no breed, which for a mod whose whole premise is that
 * two horses with the same genotype look different is a lossy copy dressed up
 * as an exact one. So there is one format now, it is this one, and it carries
 * everything: the alleles, the epigenetics riding on each copy, the name, the
 * sex, the age and the breed label.
 *
 * <h2>What is written for the loader and what is written for the reader</h2>
 * The two code strings plus the four flags are the whole of the horse. The
 * {@code readable} block underneath - the short form, the resolved speed,
 * health, jump and size, the conditions - is <b>for a person</b>, and
 * {@link #read} throws every word of it away. A loader must re-resolve traits
 * rather than trust them, or a re-tuned gene would be unable to reach a saved
 * horse.
 *
 * <h2>Deliberately tolerant</h2>
 * A file naming a gene this build does not have loads with that locus dropped
 * ({@link Genotype#parse} already does that), a missing field reads as its
 * default, and an epigenome that will not parse costs the horse its epigenetics
 * and nothing else. The caller is told what was dropped. Refusing a horse
 * because one field aged badly would make the format useless the first time a
 * gene was renamed.
 */
public record HorseFile(
        String first,
        String last,
        boolean female,
        boolean baby,
        String breed,
        String genotype,
        String epigenome) {

    public static final int FORMAT = 1;

    /**
     * The horse as JSON. {@code generator} names where it came from - a file
     * from the browser designer and a clipboard from the spawn egg are the same
     * format, and it is worth being able to tell which produced a given one.
     */
    public static String write(HorseFile horse, String generator) {
        Genotype gt = Genotype.parse(horse.genotype());
        Epigenome epi;
        try {
            epi = Epigenome.parse(horse.epigenome());
        } catch (RuntimeException e) {
            epi = Epigenome.fromSeed(0L);
        }
        Traits traits = HorseTraits.resolve(gt, epi, !horse.baby());

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format\": ").append(FORMAT).append(",\n");
        sb.append("  \"generator\": ").append(quote(generator)).append(",\n");
        sb.append("  \"name\": { \"first\": ").append(quote(horse.first()))
                .append(", \"last\": ").append(quote(horse.last())).append(" },\n");
        sb.append("  \"sex\": ").append(quote(horse.female() ? "MARE" : "STALLION")).append(",\n");
        sb.append("  \"baby\": ").append(horse.baby()).append(",\n");
        sb.append("  \"breed\": ").append(horse.breed() == null || horse.breed().isEmpty()
                ? "null" : quote(horse.breed())).append(",\n");
        sb.append("  \"genotype\": ").append(quote(horse.genotype())).append(",\n");
        sb.append("  \"epigenome\": ").append(quote(horse.epigenome())).append(",\n");
        sb.append("  \"readable\": {\n");
        sb.append("    \"shortForm\": ").append(quote(GeneCodeDisplay.shortForm(gt))).append(",\n");
        sb.append("    \"speed\": ").append(traits.speed()).append(",\n");
        sb.append("    \"health\": ").append(traits.health()).append(",\n");
        sb.append("    \"jump\": ").append(traits.jump()).append(",\n");
        sb.append("    \"scale\": ").append(traits.scale()).append(",\n");
        sb.append("    \"conditions\": [");
        List<Condition> conditions = traits.conditions();
        for (int i = 0; i < conditions.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(conditions.get(i).name()));
        }
        sb.append("]\n  }\n}\n");
        return sb.toString();
    }

    /**
     * Read one back.
     *
     * @throws IllegalArgumentException if the text is not JSON, is not an
     *         object, or carries no {@code genotype} this build can parse -
     *         the three cases where there is no horse to load at all.
     */
    public static HorseFile read(String text) {
        Object parsed;
        try {
            parsed = Json.parse(text);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("that is not valid JSON: " + e.getMessage(), e);
        }
        if (!(parsed instanceof Map<?, ?> obj)) {
            throw new IllegalArgumentException("a horse file is a JSON object");
        }
        String genotype = str(obj.get("genotype"));
        if (genotype.isEmpty()) {
            throw new IllegalArgumentException("a horse file needs a \"genotype\" code");
        }
        Genotype.parse(genotype);   // fail here rather than half-way through applying it

        String first = "";
        String last = "";
        if (obj.get("name") instanceof Map<?, ?> name) {
            first = str(name.get("first"));
            last = str(name.get("last"));
        }
        String sex = str(obj.get("sex"));
        return new HorseFile(first, last,
                !"STALLION".equalsIgnoreCase(sex),
                Boolean.TRUE.equals(obj.get("baby")),
                str(obj.get("breed")),
                genotype,
                str(obj.get("epigenome")));
    }

    /** A filename for it, from the horse's own name. */
    public static String fileName(String first, String last) {
        String raw = (first + "-" + last).toLowerCase();
        StringBuilder out = new StringBuilder();
        boolean lastWasDash = true;   // so a leading run of junk produces nothing
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (ok) {
                out.append(c);
                lastWasDash = false;
            } else if (!lastWasDash) {
                out.append('-');
                lastWasDash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.setLength(out.length() - 1);
        }
        return (out.length() == 0 ? "horse" : out.toString()) + ".json";
    }

    private static String str(Object o) {
        return o instanceof String s ? s : "";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // Hand-rolled rather than String.format: common/ is
                        // compiled to wasm as well as into the mod, and the
                        // formatter is a large thing to reach for to escape a
                        // control character that should not be here anyway.
                        sb.append("\\u");
                        for (int shift = 12; shift >= 0; shift -= 4) {
                            sb.append("0123456789abcdef".charAt((c >> shift) & 0xF));
                        }
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
