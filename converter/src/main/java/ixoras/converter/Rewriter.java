package ixoras.converter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The one rewrite rule (owner, Decided in the rename treatment): every id-shaped token that starts with the
 * old namespace followed by ':' or '.' becomes the new one, in any NBT string value or key and in any text
 * file, including text a player typed (book pages, signs, custom names).
 *
 * <p>Anchored: the token must not be preceded by a letter, digit, '_', '.' or '/', so a class name such as
 * "com.example.horsegenetics.x", the word "xhorsegenetics:" and a path "assets/horsegenetics/" are not touched
 * by the rule. A genotype code such as "horsegenetics.extension=E/e-horsegenetics.agouti=A/a" is: every
 * token in it follows the start of the string, '-' or ';', which are deliberately NOT in the excluded set.
 * (The treatment's Open about a longer word is answered this way; the building session may tighten it after
 * reading real saved text.)
 */
final class Rewriter {
    private final Pattern pattern;
    private final String replacement;

    /** Tokens rewritten so far (not thread safe: one converter run, one thread). */
    long count;

    Rewriter(String oldNs, String newNs) {
        this.pattern = Pattern.compile("(?<![A-Za-z0-9_./])" + Pattern.quote(oldNs) + "(?=[:.])");
        this.replacement = Matcher.quoteReplacement(newNs);
    }

    boolean has(String s) {
        return pattern.matcher(s).find();
    }

    String rewrite(String s) {
        Matcher m = pattern.matcher(s);
        if (!m.find()) {
            return s;
        }
        m.reset();
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            count++;
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Rewrite every string value and compound key under {@code v}, in place where possible; returns the result. */
    Object rewriteTag(Object v) throws IOException {
        if (v instanceof String) {
            return rewrite((String) v);
        }
        if (v instanceof Nbt.NList) {
            Nbt.NList l = (Nbt.NList) v;
            if (l.type == Nbt.STRING || l.type == Nbt.LIST || l.type == Nbt.COMPOUND) {
                for (int i = 0; i < l.items.size(); i++) {
                    l.items.set(i, rewriteTag(l.items.get(i)));
                }
            }
            return l;
        }
        if (v instanceof Nbt.Compound) {
            Nbt.Compound c = (Nbt.Compound) v;
            Nbt.Compound out = new Nbt.Compound();
            for (java.util.Map.Entry<String, Object> e : c.entrySet()) {
                out.put(rewrite(e.getKey()), rewriteTag(e.getValue()));
            }
            c.clear();
            c.putAll(out);
            return c;
        }
        return v;
    }
}
