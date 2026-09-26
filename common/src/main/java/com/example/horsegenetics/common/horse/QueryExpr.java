package com.example.horsegenetics.common.horse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>The search box is a SQL {@code WHERE} clause.</b> One grammar, parsed once
 * here, used by every place in the mod that searches horses - the browser's
 * <i>My horses</i> and <i>Horse realm</i> tables, the breeding pickers, and the
 * stasis bank - because they all run through {@link HorseQuery}.
 *
 * <h2>Why a clause and not a statement</h2>
 * There is one table and you always want every column of it, so
 * {@code SELECT * FROM horses WHERE} is ceremony in front of the only part that
 * carries information - and it is ceremony you would type into a one-line box
 * before every search, for ever. The box <i>is</i> the {@code WHERE} clause.
 * (Owner, 2026-09-26.) Everything after {@code WHERE} in real SQL works the way
 * you would expect it to:
 *
 * <pre>
 *   healer = hom AND flyer IN (het, hom)
 *   breed LIKE 'Arab%' AND generation &gt; 2 AND NOT lethal
 *   mare AND (bond &gt;= 50 OR tamed)
 * </pre>
 *
 * <h2>The old syntax still works, because it is a subset</h2>
 * Adjacent terms with no operator between them are <b>implicitly ANDed</b>,
 * which is exactly what the filter box did before there were operators - so
 * {@code mare gen&gt;2 -lethal} still means what it always meant, and so does
 * every saved habit and every line of the tab's own help text. {@code -term}
 * survives as shorthand for {@code NOT term} (owner's call); {@code :} survives
 * as a synonym for {@code =}. Nothing that used to work stopped working, which
 * is what let this go in under a screen full of existing tests.
 *
 * <h2>Grammar</h2>
 * <pre>
 *   expr    := or
 *   or      := and (OR and)*
 *   and     := not (AND? not)*          -- AND may be omitted
 *   not     := (NOT | '-') not | atom
 *   atom    := '(' expr ')' | predicate
 *   predicate := word                   -- a flag, or free text
 *              | word op value
 *              | word IN '(' value (',' value)* ')'
 *   op      := '=' | ':' | '!=' | '&lt;&gt;' | '&gt;' | '&gt;=' | '&lt;' | '&lt;=' | LIKE
 * </pre>
 * {@code NOT} binds tighter than {@code AND}, which binds tighter than
 * {@code OR}, as in SQL. Keywords are case-insensitive; quote a word to use one
 * as a value ({@code name = 'or'}).
 *
 * <h2>It never throws</h2>
 * A search box is typed into one character at a time, so <b>every prefix of
 * every valid query is a query somebody is about to have</b> - a dangling
 * {@code AND}, an unclosed bracket, an operator with nothing after it. All of
 * them parse to something that simply matches nothing, and none of them throws
 * into a render loop. That contract predates this class ({@code HorseQuery}'s
 * malformed-term test) and is the reason the parser is lenient rather than
 * strict everywhere it could be either.
 */
public final class QueryExpr {

    private QueryExpr() {
    }

    // ------------------------------------------------------------------
    // The tree
    // ------------------------------------------------------------------

    /** One parsed query. {@code haystack} is passed in because the row's is not free. */
    public interface Node {
        boolean test(HorseListing row, String haystack);
    }

    /** An empty query keeps everything, which is what an empty box should do. */
    static final Node ALWAYS = (row, haystack) -> true;

    /** What a query that cannot mean anything matches. */
    static final Node NEVER = (row, haystack) -> false;

    private record And(List<Node> parts) implements Node {
        @Override
        public boolean test(HorseListing row, String haystack) {
            for (Node part : parts) {
                if (!part.test(row, haystack)) {
                    return false;
                }
            }
            return true;
        }
    }

    private record Or(List<Node> parts) implements Node {
        @Override
        public boolean test(HorseListing row, String haystack) {
            for (Node part : parts) {
                if (part.test(row, haystack)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record Not(Node inner) implements Node {
        @Override
        public boolean test(HorseListing row, String haystack) {
            return !inner.test(row, haystack);
        }
    }

    /** A bare word: a flag like {@code mare}, or free text over the haystack. */
    private record Bare(String word) implements Node {
        @Override
        public boolean test(HorseListing row, String haystack) {
            return HorseQuery.flagOrText(row, word, haystack);
        }
    }

    /** {@code column op value} - the whole of the rest of the language. */
    private record Compare(String column, String op, String value) implements Node {
        @Override
        public boolean test(HorseListing row, String haystack) {
            return HorseQuery.predicate(row, haystack, column, op, value);
        }
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    public static Node parse(String query) {
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return ALWAYS;
        }
        Cursor cursor = new Cursor(tokens);
        Node node = parseOr(cursor);
        // Trailing rubbish - a stray ')' say - is ignored rather than fatal.
        // Half-typed is the normal state of a search box.
        return node;
    }

    /** Where the parser has got to. A field rather than an index passed around. */
    private static final class Cursor {
        private final List<String> tokens;
        private int at;

        Cursor(List<String> tokens) {
            this.tokens = tokens;
        }

        boolean done() {
            return at >= tokens.size();
        }

        String peek() {
            return done() ? null : tokens.get(at);
        }

        String next() {
            return done() ? null : tokens.get(at++);
        }

        /** Take the next token if it is this keyword, case-insensitively. */
        boolean take(String keyword) {
            String token = peek();
            if (token != null && token.equalsIgnoreCase(keyword) && !isQuoted(token)) {
                at++;
                return true;
            }
            return false;
        }
    }

    private static Node parseOr(Cursor cursor) {
        List<Node> parts = new ArrayList<>();
        parts.add(parseAnd(cursor));
        while (cursor.take("OR")) {
            parts.add(parseAnd(cursor));
        }
        return parts.size() == 1 ? parts.get(0) : new Or(parts);
    }

    private static Node parseAnd(Cursor cursor) {
        List<Node> parts = new ArrayList<>();
        parts.add(parseNot(cursor));
        while (true) {
            String token = cursor.peek();
            if (token == null || ")".equals(token)) {
                break;
            }
            if (!isQuoted(token) && token.equalsIgnoreCase("OR")) {
                break;
            }
            // An explicit AND, or two terms side by side - which is how every
            // query written before this class existed is spelled.
            cursor.take("AND");
            if (cursor.done() || ")".equals(cursor.peek())) {
                break;      // a dangling AND: mid-type, not an error
            }
            parts.add(parseNot(cursor));
        }
        return parts.size() == 1 ? parts.get(0) : new And(parts);
    }

    private static Node parseNot(Cursor cursor) {
        if (cursor.take("NOT")) {
            return new Not(parseNot(cursor));
        }
        return parseAtom(cursor);
    }

    private static Node parseAtom(Cursor cursor) {
        String token = cursor.next();
        if (token == null) {
            return NEVER;
        }
        if ("(".equals(token)) {
            Node inner = parseOr(cursor);
            if (")".equals(cursor.peek())) {
                cursor.next();
            }
            return inner;   // an unclosed bracket closes itself at the end
        }
        if (")".equals(token) || ",".equals(token)) {
            return NEVER;   // nothing sensible to do with it
        }
        if (isOperator(token)) {
            // An operator with no column in front of it. Consume whatever it
            // was going to compare, so the rest of the query still parses.
            if (!cursor.done() && !isOperator(cursor.peek())) {
                cursor.next();
            }
            return NEVER;
        }
        // A leading '-' is NOT, kept from the old syntax. Only at the front of a
        // word, so a breed like Anglo-Arab is still one word.
        if (token.length() > 1 && token.charAt(0) == '-' && !isQuoted(token)) {
            return new Not(column(cursor, token.substring(1)));
        }
        return column(cursor, token);
    }

    /** A word, and whatever operator and value follow it. */
    private static Node column(Cursor cursor, String word) {
        String name = unquote(word);
        String token = cursor.peek();
        if (token == null) {
            return new Bare(name);
        }
        if (!isQuoted(token) && token.equalsIgnoreCase("IN")) {
            cursor.next();
            return in(cursor, name);
        }
        if (!isQuoted(token) && token.equalsIgnoreCase("LIKE")) {
            cursor.next();
            String value = cursor.done() ? "" : unquote(cursor.next());
            return new Compare(name, "LIKE", value);
        }
        if (!isOperator(token)) {
            return new Bare(name);
        }
        cursor.next();
        String value = cursor.done() || isOperator(cursor.peek()) || ")".equals(cursor.peek())
                ? "" : unquote(cursor.next());
        if ("!=".equals(token) || "<>".equals(token)) {
            return new Not(new Compare(name, "=", value));
        }
        return new Compare(name, token, value);
    }

    /** {@code column IN (a, b, c)} - an OR over one column, and nothing more. */
    private static Node in(Cursor cursor, String column) {
        boolean bracketed = "(".equals(cursor.peek());
        if (bracketed) {
            cursor.next();
        }
        List<Node> parts = new ArrayList<>();
        while (!cursor.done()) {
            String token = cursor.peek();
            if (")".equals(token)) {
                cursor.next();
                break;
            }
            if (",".equals(token)) {
                cursor.next();
                continue;
            }
            if (isOperator(token)) {
                break;
            }
            parts.add(new Compare(column, "=", unquote(cursor.next())));
            if (!bracketed) {
                break;      // IN without brackets takes exactly one value
            }
        }
        if (parts.isEmpty()) {
            return NEVER;
        }
        return parts.size() == 1 ? parts.get(0) : new Or(parts);
    }

    // ------------------------------------------------------------------
    // Tokenizing
    // ------------------------------------------------------------------

    /**
     * Words, operators, brackets and commas. A quoted run keeps its quotes until
     * {@link #unquote} takes them off, so that {@code 'or'} can be a value
     * rather than the keyword.
     */
    static List<String> tokenize(String query) {
        List<String> out = new ArrayList<>();
        if (query == null) {
            return out;
        }
        int i = 0;
        int n = query.length();
        while (i < n) {
            char c = query.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(' || c == ')' || c == ',') {
                out.add(String.valueOf(c));
                i++;
                continue;
            }
            if (c == '\'' || c == '"') {
                int end = query.indexOf(c, i + 1);
                if (end < 0) {
                    // Unclosed quote: everything after it is the value. Mid-type
                    // again - the closing quote has not been reached yet.
                    out.add(query.substring(i));
                    break;
                }
                out.add(query.substring(i, end + 1));
                i = end + 1;
                continue;
            }
            String operator = operatorAt(query, i);
            if (operator != null) {
                out.add(operator);
                i += operator.length();
                continue;
            }
            int start = i;
            while (i < n) {
                char w = query.charAt(i);
                if (Character.isWhitespace(w) || w == '(' || w == ')' || w == ','
                        || w == '\'' || w == '"' || operatorAt(query, i) != null) {
                    break;
                }
                i++;
            }
            out.add(query.substring(start, i));
        }
        return out;
    }

    /**
     * The operator starting here, longest first, or null.
     *
     * <p>{@code :} is one, which is what keeps {@code gen:3} and
     * {@code genotype:E/e} working; a {@code /} is not, which is what keeps
     * {@code E/e} a single value.
     */
    private static String operatorAt(String query, int i) {
        if (query.startsWith(">=", i) || query.startsWith("<=", i)
                || query.startsWith("!=", i) || query.startsWith("<>", i)) {
            return query.substring(i, i + 2);
        }
        char c = query.charAt(i);
        if (c == '>' || c == '<' || c == '=' || c == ':') {
            return String.valueOf(c);
        }
        return null;
    }

    private static boolean isOperator(String token) {
        return token != null && token.length() <= 2 && !token.isEmpty()
                && operatorAt(token, 0) != null
                && operatorAt(token, 0).length() == token.length();
    }

    private static boolean isQuoted(String token) {
        return token.length() >= 2
                && (token.charAt(0) == '\'' || token.charAt(0) == '"');
    }

    private static String unquote(String token) {
        if (token == null) {
            return "";
        }
        if (!isQuoted(token)) {
            return token;
        }
        char quote = token.charAt(0);
        int end = token.length() - 1;
        if (token.charAt(end) == quote) {
            return token.substring(1, end);
        }
        return token.substring(1);      // unclosed, mid-type
    }

    /**
     * {@code LIKE} the way SQL means it: {@code %} is any run of characters.
     * Folds case, because every other text comparison in the language does and
     * an allele token is never asked for with {@code LIKE}.
     */
    static boolean like(String actual, String pattern) {
        if (actual == null) {
            return false;
        }
        String value = actual.toLowerCase(Locale.ROOT);
        String want = pattern.toLowerCase(Locale.ROOT);
        boolean open = want.startsWith("%");
        boolean close = want.endsWith("%");
        String core = want;
        if (open) {
            core = core.substring(1);
        }
        if (close && !core.isEmpty()) {
            core = core.substring(0, core.length() - 1);
        }
        if (core.isEmpty()) {
            return true;    // '%' on its own is everything
        }
        if (open && close) {
            return value.contains(core);
        }
        if (open) {
            return value.endsWith(core);
        }
        if (close) {
            return value.startsWith(core);
        }
        return value.equals(core);
    }
}
