package com.example.horsegenetics.common.testutil;

/**
 * Turns a pre-rewrite <b>positional</b> genotype code (segments in the old
 * hand-written {@code codeOrder}) into a gene-keyed one the current
 * {@code Genotype.parse} accepts. Lets the existing table-of-literals tests keep
 * their readable {@code "E/e-A/a-..."} strings without re-typing every case.
 *
 * <p>The order below is a fixed reading order for these literals - roughly the
 * hand-written {@code codeOrder} of an earlier build, with each retired gene's
 * slot handed to the locus that absorbed it: the old {@code cream} and
 * {@code pearl} slots to {@code matp}, {@code white} and {@code sabino} to
 * {@code kit}, {@code splash} to {@code mitf}, and {@code frame} to
 * {@code ednrb} (with {@code pax3} taking the freed tail slot). The literals in
 * the tests moved with them, so a slot's <i>tokens</i> are the new locus's.
 * Segments past the end of a shorter string are simply omitted (parsing fills
 * them with the gene's default allele).
 */
public final class LegacyCode {

    private static final String[] ORDER = {
            "extension", "agouti", "kit", "test", "champagne", "mitf", "grey", "matp",
            "magic_zebra", "pink_hair", "dun", "silver", "mushroom", "roan", "tobiano", "ednrb", "pax3"};

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
