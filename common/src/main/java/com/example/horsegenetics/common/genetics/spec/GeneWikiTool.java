package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Genes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes the wiki's <b>magical gene family pages</b> - one page per family,
 * each listing its genes alphabetically with the icon
 * {@code GeneIconTool} baked for it.
 *
 * <h2>Why these pages are generated and the others are not</h2>
 * Every other page on the wiki is hand-written, and should be: prose about a
 * system is not derivable from the system. These are the exception because
 * there are <b>eighty-odd</b> of the things and everything a reader needs about
 * one of them - its name, its alleles, what each combination does and looks
 * like - is already written down, in the gene's own file, in the same words the
 * game shows. Copying that into HTML by hand would produce eighty-odd
 * opportunities for the wiki to say something the mod does not.
 *
 * <p>So a gene that wants a page of its own still gets one, hand-written, and
 * the family page links to it. What the family page guarantees is that no gene
 * is <i>missing</i> and none of them is described wrongly.
 *
 * <h2>The family is the priority band</h2>
 * A gene declares no family. It does not need to: the magical priority bands
 * were laid out as families in the first place - spots together, strokes
 * together, the things that read the coat beneath them last - because the paint
 * order and the taxonomy want the same grouping for the same reason. {@link
 * #FAMILIES} is that table, and it is the only place the two are joined up.
 *
 * <p>{@code ./gradlew :common:bakeGeneWikiPages} - output dir is arg 0.
 */
public final class GeneWikiTool {

    /** Lowest priority in the band, to the family it names and the page it writes. */
    private static final TreeMap<Integer, String[]> FAMILIES = new TreeMap<>();

    static {
        // { slug, title, lede }
        FAMILIES.put(200, new String[]{"genes-magic-ground", "Ground and strong white",
                "Genes that replace the base coat rather than mark it - a white horse with the "
                        + "colour breaking through, a coat confined to a handful of regions, a "
                        + "field split down the middle."});
        FAMILIES.put(250, new String[]{"genes-magic-fields", "Fields and regions",
                "Genes that divide the horse into areas - a blanket off the topline, a band round "
                        + "the neck, the underside against the back, a wash with no edge anywhere."});
        FAMILIES.put(300, new String[]{"genes-magic-spots", "Spots and rings",
                "Genes made of countable marks: spots, rosettes, annuli, crescents, chains of "
                        + "disks, and haloes with something dark inside them."});
        FAMILIES.put(330, new String[]{"genes-magic-speckle", "Speckle and dust",
                "Genes made of stipple - fields of fine points that drift into drifts and thin out "
                        + "rather than ending, and the markings built on top of them."});
        FAMILIES.put(360, new String[]{"genes-magic-lines", "Lines and strokes",
                "Genes made of strokes: scratches, contour lines, riblines, brindle nets, "
                        + "filigree, and the two that are stripes but insist they are not."});
        FAMILIES.put(400, new String[]{"genes-magic-hair", "Mane and tail",
                "Genes that touch only the hair - gradients down its length, bands across it, "
                        + "and the spectrum run root to tip."});
        FAMILIES.put(430, new String[]{"genes-magic-modifiers", "Colour modifiers",
                "Genes with no shape of their own. Each reads what the horse already is and "
                        + "changes it - which is why they paint last, and why a plain horse shows "
                        + "some of them not at all."});
    }

    private GeneWikiTool() {}

    public static void main(String[] args) throws IOException {
        Path wiki = Path.of(args.length > 0 ? args[0] : "wiki");
        Map<String[], List<SpecGene>> byFamily = new LinkedHashMap<>();
        for (String[] family : FAMILIES.values()) {
            byFamily.put(family, new ArrayList<>());
        }
        for (SpecGene gene : Genes.loaded()) {
            byFamily.get(familyOf(gene.priority())).add(gene);
        }

        List<String> nav = new ArrayList<>();
        for (Map.Entry<String[], List<SpecGene>> e : byFamily.entrySet()) {
            String[] family = e.getKey();
            List<SpecGene> genes = new ArrayList<>(e.getValue());
            genes.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            Path out = wiki.resolve(family[0] + ".html");
            Files.writeString(out, page(family, genes), StandardCharsets.UTF_8);
            System.out.println("wrote " + out.getFileName() + "  (" + genes.size() + " genes)");
            nav.add("                { href: \"" + family[0] + ".html\", text: \"" + family[1]
                    + "\", kind: \"magical\", views: [\"gameplay\",\"coding\"] }");
        }
        System.out.println();
        System.out.println("pages.js entries (alphabetical by title, paste into the magical section):");
        nav.sort(String::compareTo);
        System.out.println(String.join(",\n", nav));
    }

    private static String[] familyOf(int priority) {
        Map.Entry<Integer, String[]> e = FAMILIES.floorEntry(priority);
        return e == null ? FAMILIES.firstEntry().getValue() : e.getValue();
    }

    // ------------------------------------------------------------------

    private static String page(String[] family, List<SpecGene> genes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>").append(esc(family[1])).append(" &middot; Horse Genetics Wiki</title>\n")
                .append("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/")
                .append("prism/1.29.0/themes/prism-tomorrow.min.css\">\n")
                .append("<link rel=\"stylesheet\" href=\"styles.css\">\n")
                .append("<script defer src=\"pages.js\"></script>\n")
                .append("<script defer src=\"tabs.js\"></script>\n")
                .append("<script defer src=\"nav.js\"></script>\n")
                .append("<script defer src=\"search.js\"></script>\n")
                .append("</head>\n<body>\n<main class=\"content\">\n<article class=\"doc\">\n\n");

        sb.append("<p class=\"eyebrow magical\">Magical genes <span class=\"sep\">/</span> ")
                .append(genes.size()).append(" in this family</p>\n\n");
        sb.append("<h1>").append(esc(family[1])).append("</h1>\n\n");
        sb.append("<p class=\"lede\">\n    ").append(esc(family[2])).append("\n</p>\n\n");
        sb.append("<p class=\"note\">\n    <strong>This page is generated.</strong> Every word below "
                + "comes out of the gene's own\n    file - the same text the game shows - and the "
                + "picture beside each one is that\n    gene on a standard bay, baked through the "
                + "real coat pipeline. Regenerate with\n    <code>:common:bakeGeneWikiPages</code> "
                + "after adding or changing a gene; see\n    <code>GeneWikiTool</code> for why "
                + "these pages in particular are not hand-written.\n</p>\n\n");

        sb.append("<section class=\"tab-panel\" data-tab=\"gameplay\">\n\n");
        for (SpecGene gene : genes) {
            sb.append(card(gene));
        }
        sb.append("</section>\n\n");

        sb.append("<section class=\"tab-panel\" data-tab=\"coding\">\n\n");
        sb.append("<h2 id=\"files\">The files</h2>\n\n");
        sb.append("<p>\n    Each of these is a JSON file in\n    "
                + "<code>common/src/main/resources/horsegenetics/genes/</code>, listed in the\n    "
                + "<code>index.json</code> beside them, and loaded by <code>Genes</code>' class\n    "
                + "initialiser. None of them is a Java class; see\n    "
                + "<a href=\"gene-format.html\">the gene file format</a> for what the masks and "
                + "ops mean.\n</p>\n\n");
        sb.append("<div class=\"table-wrap\">\n<table class=\"facts\">\n<thead><tr>")
                .append("<th>Gene</th><th>File</th><th>Priority</th><th>Rarity</th>")
                .append("<th>Masks and ops it uses</th></tr></thead>\n<tbody>\n");
        for (SpecGene gene : genes) {
            String slug = slug(gene);
            sb.append("<tr><td><strong>").append(esc(gene.name())).append("</strong></td>")
                    .append("<td><code>").append(slug).append(".json</code></td>")
                    .append("<td>").append(gene.priority()).append("</td>")
                    .append("<td>").append(gene.rarity().name().toLowerCase(Locale.ROOT)).append("</td>")
                    .append("<td>").append(esc(String.join(", ", vocabulary(gene)))).append("</td></tr>\n");
        }
        sb.append("</tbody>\n</table>\n</div>\n\n</section>\n\n");

        sb.append("</article>\n</main>\n")
                .append("<script defer src=\"gene-preview/gene-preview.js\"></script>\n")
                .append("<script defer src=\"gene-inheritance/gene-inheritance.js\"></script>\n")
                .append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String card(SpecGene gene) {
        String slug = slug(gene);
        StringBuilder sb = new StringBuilder();
        sb.append("<h2 id=\"").append(slug.replace('_', '-')).append("\">")
                .append(esc(gene.name())).append("</h2>\n\n");
        sb.append("<div class=\"gene-card\">\n");
        sb.append("<img class=\"gene-card-icon\" src=\"assets/gene-icons/").append(slug)
                .append(".png\" alt=\"").append(esc(gene.name()))
                .append(" on a bay horse\" width=\"150\" loading=\"lazy\">\n");
        sb.append("<div class=\"gene-card-body\">\n");
        if (!gene.spec().blurb().isBlank()) {
            sb.append("<p>").append(esc(gene.spec().blurb())).append("</p>\n");
        }
        sb.append("<p class=\"gene-card-alleles\">");
        List<String> tokens = new ArrayList<>();
        for (Allele a : gene.alleles()) {
            tokens.add("<code>" + esc(a.token()) + "</code>");
        }
        sb.append(String.join(" &middot; ", tokens)).append("</p>\n");
        sb.append("</div>\n</div>\n\n");

        sb.append("<div class=\"table-wrap\">\n<table class=\"facts\">\n<tbody>\n");
        for (GeneSpec.ExpressionSpec e : gene.spec().expressions()) {
            if (e.description().isBlank()) {
                continue;
            }
            sb.append("<tr><th>").append(esc(e.name())).append("</th><td>")
                    .append(esc(e.description())).append("</td></tr>\n");
        }
        sb.append("</tbody>\n</table>\n</div>\n\n");
        sb.append("<div class=\"gene-preview\" data-gene=\"").append(gene.key()).append("\"></div>\n\n");
        return sb.toString();
    }

    /** Every mask and op the gene's layers actually use - the "what is it made of" column. */
    private static List<String> vocabulary(SpecGene gene) {
        Set<String> out = new LinkedHashSet<>();
        for (GeneSpec.ExpressionSpec e : gene.spec().expressions()) {
            for (GeneSpec.Layer layer : e.layers()) {
                for (GeneSpec.Mask mask : layer.masks()) {
                    out.add(mask.type().name());
                }
                out.add(layer.op().type().name());
                if (layer.emissive()) {
                    out.add("emissive");
                }
            }
        }
        List<String> sorted = new ArrayList<>(out);
        sorted.sort(String::compareTo);
        return sorted;
    }

    private static String slug(SpecGene gene) {
        return gene.key().substring(gene.key().indexOf('.') + 1);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
