package com.example.horsegenetics.common.genetics.spec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A rebake must not delete a checklist or a plan, and nothing else notices
 * when it does.</b> {@link GeneWikiTool} rewrites a generated gene page whole, so
 * anything a person wrote on one has to be carried across - the <b>Verified</b>
 * records under {@code id="verified"}, and every page-local tab, which is where
 * hard rules 7 and 9 put a checklist and a plan.
 *
 * <p>This exists because that failed three times, the same way. A bake on
 * 2026-09-15 emptied thirteen pages' <b>Verified</b> sections, which is what
 * {@link GeneWikiTool#verifiedBlock} was written to stop - and it shipped with no
 * test, so a bake on 2026-09-21 deleted the <b>Verification</b> tabs of twenty-four
 * pages and one <b>Roadmap</b> tab instead. No failure said anything: the page is
 * still a valid page and the build is still green. The only evidence was a count
 * dropping in one tool nobody has to run and a broken fragment in another.
 *
 * <h2>Why the two indexes are the assertion</h2>
 * The obvious test - "does the extractor extract?" - passed through every one of
 * those failures, because what broke was never the extraction. It was a caller
 * that did not ask for it. So what is asserted here is the agreement between
 * <b>two checked-in artefacts generated from each other</b>: each index lists one
 * row per page carrying that tab, and every page it names must still carry it. A
 * bake that strips twenty-four tabs leaves the index naming pages that have none,
 * and this goes red - the failure, stated as itself.
 *
 * <p>It fails in the other direction too, when a tab is added and the index is
 * not re-baked (CLAUDE.md's regeneration table). That is the same defect from the
 * other end: an index that disagrees with the pages is worse than no index,
 * because it is trusted.
 */
class GeneWikiCarryTest {

    private static final Path WIKI = Path.of("..", "wiki");

    private static boolean wikiIsBesideUs() {
        return Files.isDirectory(WIKI);
    }

    @Test
    void everyPageTheVerificationIndexNamesStillCarriesItsTab() throws IOException {
        assertIndexAgreesWithPages("verification.html", "verification",
                "bake-verification-index.mjs");
    }

    @Test
    void everyPageTheRoadmapIndexNamesStillCarriesItsTab() throws IOException {
        assertIndexAgreesWithPages("roadmap.html", "roadmap", "bake-roadmap-index.mjs");
    }

    /**
     * Both directions of one invariant: no row without its tab, no tab without
     * its row.
     *
     * @param indexPage the generated index, e.g. {@code verification.html}
     * @param tab       the {@code data-tab} it collects
     * @param bake      the tool that writes the index, named in the failure
     */
    private void assertIndexAgreesWithPages(String indexPage, String tab, String bake)
            throws IOException {
        if (!wikiIsBesideUs()) {
            return;   // a checkout without the wiki beside it
        }
        Path index = WIKI.resolve(indexPage);
        if (!Files.isRegularFile(index)) {
            return;
        }
        String generated = between(Files.readString(index, StandardCharsets.UTF_8));
        Pattern row = Pattern.compile("<a href=\"([^\"#]+)#" + tab + "\">");
        String marker = "data-tab=\"" + tab + "\"";

        var indexed = new TreeSet<String>();
        List<String> broken = new ArrayList<>();
        Matcher m = row.matcher(generated);
        while (m.find()) {
            indexed.add(m.group(1));
            Path page = WIKI.resolve(m.group(1));
            if (!Files.isRegularFile(page)) {
                broken.add(m.group(1) + " (no such page)");
                continue;
            }
            String html = Files.readString(page, StandardCharsets.UTF_8);
            if (!html.contains(marker)) {
                broken.add(m.group(1) + " (indexed, but the " + tab + " tab is gone - a bake ate it?)");
            } else if (!GeneWikiTool.keptPanels(html).contains(marker)
                    && html.contains(GeneWikiTool.GENERATED)) {
                broken.add(m.group(1) + " (a rebake of this generated page would not carry it)");
            }
        }
        assertTrue(indexed.size() > 0, indexPage + "'s generated span lists nothing");
        assertEquals(List.of(), broken,
                indexPage + " names pages whose " + tab + " tab is not there. Restore the tabs and "
                        + "then re-run " + bake + " - never the bake first, which would make the loss "
                        + "permanent and silent");

        var unlisted = new TreeSet<String>();
        try (var pages = Files.list(WIKI)) {
            for (Path page : pages.toList()) {
                String name = page.getFileName().toString();
                if (!name.endsWith(".html") || name.equals(indexPage)) {
                    continue;
                }
                if (Files.readString(page, StandardCharsets.UTF_8).contains(marker)
                        && !indexed.contains(name)) {
                    unlisted.add(name);
                }
            }
        }
        assertEquals(new TreeSet<String>(), unlisted,
                "these pages carry a " + tab + " tab that " + indexPage + " does not list - "
                        + "run node wiki/tools/" + bake);
    }

    /** Every generated gene page's Verified records survive a rebake. */
    @Test
    void everyVerifiedBlockOnAGeneratedPageIsCarried() throws IOException {
        if (!wikiIsBesideUs()) {
            return;
        }
        List<String> lost = new ArrayList<>();
        try (var pages = Files.list(WIKI)) {
            for (Path page : pages.toList()) {
                String name = page.getFileName().toString();
                if (!name.startsWith("gene-") || !name.endsWith(".html")) {
                    continue;
                }
                String html = Files.readString(page, StandardCharsets.UTF_8);
                if (!html.contains(GeneWikiTool.GENERATED) || !html.contains("<h3 id=\"verified\">")) {
                    continue;
                }
                if (GeneWikiTool.verifiedBlock(html).isBlank()) {
                    lost.add(name);
                }
            }
        }
        assertEquals(List.of(), lost, "Verified records a rebake would delete");
    }

    /**
     * The carry takes each page-local panel from its own {@code <section>}, keeps
     * the three the tool writes out of it, and does not care which tab it is -
     * that last part is what makes the next dismantled list survive by default.
     */
    @Test
    void theCarryTakesEveryPageLocalTabAndNothingElse() {
        String page = "<section class=\"tab-panel\" data-tab=\"coding\">\n"
                + "<p>the coding tab</p>\n</section>\n\n"
                + "<section id=\"verification\" class=\"tab-panel\" data-tab=\"verification\">\n"
                + "<div class=\"note verify-summary\"><p>one sentence</p></div>\n</section>\n\n"
                + "<section id=\"roadmap\" class=\"tab-panel\" data-tab=\"roadmap\">\n"
                + "<div class=\"note roadmap-summary\"><p>the plan</p></div>\n</section>\n";
        String carried = GeneWikiTool.keptPanels(page);
        assertFalse(carried.contains("the coding tab"), "the carry reached into a generated tab");
        assertTrue(carried.contains("one sentence"), "the verification tab was dropped");
        assertTrue(carried.contains("the plan"), "the roadmap tab was dropped");
        assertTrue(carried.startsWith("<section"), "a panel must start at its own <section>");
        assertTrue(carried.trim().endsWith("</section>"), "a panel must include its </section>");
        // In document order, so the tab bar keeps the order the author wrote.
        assertTrue(carried.indexOf("verify-summary") < carried.indexOf("roadmap-summary"),
                "the panels came back out of order");
        assertEquals("", GeneWikiTool.keptPanels("<p>a page with no tabs at all</p>"));
        assertEquals("", GeneWikiTool.keptPanels(
                "<section class=\"tab-panel\" data-tab=\"science\">only a view</section>"));
    }

    /** What lies between an index's generated markers, or the whole page if it has none. */
    private static String between(String index) {
        int a = index.indexOf("<!-- BEGIN generated");
        int b = index.indexOf("<!-- END generated -->");
        return a >= 0 && b > a ? index.substring(a, b) : index;
    }
}
