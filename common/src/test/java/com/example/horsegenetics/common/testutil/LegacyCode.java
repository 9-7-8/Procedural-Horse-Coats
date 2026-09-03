package com.example.horsegenetics.common.testutil;

/**
 * Turns a pre-rewrite <b>positional</b> genotype code (segments in the old
 * hand-written {@code codeOrder}) into a gene-keyed one the current
 * {@code Genotype.parse} accepts. Lets the existing table-of-literals tests keep
 * their readable {@code "E/e-A/a-..."} strings without re-typing every case.
 *
 * <p>The order below is the built-in {@code codeOrder} as it stood before genes
 * gained a {@code priority()} and the orderings became derived. Segments past
 * the end of a shorter string are simply omitted (parsing fills them with wild
 * type).
 */
public final class LegacyCode {

    private static final String[] ORDER = {
            "extension", "agouti", "white", "test", "champagne", "splash", "grey", "cream", "pearl",
            "magic_zebra", "pink_hair", "dun", "silver", "mushroom", "roan", "tobiano", "frame", "sabino"};

    private LegacyCode() {}

    public static String keyed(String positional) {
        String[] segs = positional.split("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segs.length; i++) {
            if (i >= ORDER.length) {
                throw new IllegalArgumentException("legacy code has more than " + ORDER.length + " segments: " + positional);
            }
            if (i > 0) {
                sb.append('-');
            }
            sb.append("horsegenetics.").append(ORDER[i]).append('=').append(segs[i]);
        }
        return sb.toString();
    }
}
