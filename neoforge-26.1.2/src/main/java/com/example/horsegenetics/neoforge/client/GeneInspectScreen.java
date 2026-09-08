package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A full-window popup, opened from the "View Genes" button on the horse
 * inventory panel: <b>one row per gene this horse carries</b>, with the horse's
 * two allele tokens and the plain-English description of the phenotype that
 * combination actually produces (resolved in genotype context, so it is the
 * outcome that really paints). It exists to make "do the alleles I see match
 * the look I expected" a glance rather than a puzzle.
 *
 * <p>Under each gene it also shows <b>the numbers that gene has written on this
 * horse's two allele copies</b> - how much white a splash covers, how much
 * bigger a magically large horse is, what colour a particle trail comes out.
 * Those used to be recoverable only by replaying a PRNG off a stored seed, so
 * "why is this horse this size" had no answer you could look at. They are stored
 * literally now, and this is where you read them.
 *
 * <p>The copy the horse actually <b>expresses</b> is marked; on a heterozygote
 * that is the dominant copy, on a homozygote the higher-priority one. The other
 * copy is still shown, because it is what half this horse's foals will inherit -
 * which is the entire reason to look.
 *
 * <p><b>Active</b> lists the genes this horse carries something at;
 * <b>All</b> adds the baseline ones. Closes back to the game like
 * {@link FamilyTreeScreen}.
 */
public final class GeneInspectScreen extends Screen {

    private static final int PANEL = 0xF00E0E12;
    private static final int PANEL_BORDER = 0xFF3A3A48;
    private static final int HEADER = 0xFFF0F0F0;
    private static final int COL_LABEL = 0xFF8890A8;
    private static final int GENE_NAME = 0xFFDDE2EC;
    private static final int ALLELES = 0xFFE0C070;
    private static final int EXPR_ON = 0xFF9BE08A;
    private static final int EXPR_CARRIER = 0xFFC9B27A;
    private static final int EXPR_OFF = 0xFF6E7686;
    private static final int DESC = 0xFFAAB0C0;
    private static final int DESC_OFF = 0xFF70768A;
    private static final int ROW_LINE = 0x22FFFFFF;

    private static final int VALUE_NAME = 0xFF7F8BA6;
    private static final int VALUE_ON = 0xFFD8C48A;
    private static final int VALUE_OFF = 0xFF6E7686;

    private final Genotype genotype;
    /** This horse's stored numbers, or {@code null} for a record with no genome. */
    private final Epigenome epigenome;
    private final String heading;

    /** False shows only what the horse carries; true adds the baseline loci. */
    private boolean showAll = false;

    private float scroll = 0f;
    private float maxScroll = 0f;

    public GeneInspectScreen(HorseRecord record) {
        super(Component.literal("Genes"));
        Genotype g;
        try {
            g = record.genotype();
        } catch (RuntimeException unparseable) {
            g = Genotype.parse("");
        }
        this.genotype = g;
        Epigenome e;
        try {
            e = record.hasGenome() ? record.epigenome() : null;
        } catch (RuntimeException unparseable) {
            e = null;
        }
        this.epigenome = e;
        this.heading = record.displayName() + " — genes";
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20)
                .build());
        addRenderableWidget(Button.builder(filterLabel(), b -> {
                    showAll = !showAll;
                    scroll = 0f;
                    b.setMessage(filterLabel());
                })
                .bounds(panelRight() - 78, panelTop() + 4, 70, 16)
                .build());
    }

    private Component filterLabel() {
        return Component.literal(showAll ? "All genes" : "Active only");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- geometry ---

    private int panelLeft() {
        return Math.max(8, this.width / 2 - 260);
    }

    private int panelRight() {
        return Math.min(this.width - 8, this.width / 2 + 260);
    }

    private int panelTop() {
        return 24;
    }

    private int panelBottom() {
        return this.height - 34;
    }

    /** x of the divider between the gene/allele column and the expression column. */
    private int splitX() {
        int l = panelLeft();
        return l + Math.max(150, (panelRight() - l) * 34 / 100);
    }

    private int listTop() {
        return panelTop() + 34;
    }

    // --- drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0000000);

        int pl = panelLeft();
        int pr = panelRight();
        int pt = panelTop();
        int pb = panelBottom();
        g.fill(pl, pt, pr, pb, PANEL);
        g.fill(pl, pt, pr, pt + 1, PANEL_BORDER);
        g.fill(pl, pb - 1, pr, pb, PANEL_BORDER);
        g.fill(pl, pt, pl + 1, pb, PANEL_BORDER);
        g.fill(pr - 1, pt, pr, pb, PANEL_BORDER);

        g.text(this.font, Component.literal(GuiText.clip(heading, 60)), pl + 8, pt + 8, HEADER);

        int split = splitX();
        g.text(this.font, Component.literal("Gene / alleles"), pl + 8, pt + 22, COL_LABEL);
        g.text(this.font, Component.literal("Expression"), split + 8, pt + 22, COL_LABEL);
        g.fill(pl + 4, pt + 32, pr - 4, pt + 33, ROW_LINE);

        int top = listTop();
        int viewBottom = pb - 6;
        int nameW = split - (pl + 8) - 6;
        int descW = (pr - 8) - (split + 8);

        g.enableScissor(pl + 1, top, pr - 1, viewBottom);
        int y = top - (int) scroll;
        int contentH = 0;
        int shownRows = 0;
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            Expression expr = genotype.expressionOf(gene);

            // Show sex / extension / agouti always (they define the base horse),
            // and otherwise only a gene the horse carries something other than
            // the plain baseline at - so an expressing coat gene, or a carrier
            // like "nJ" / "nC" that the paper dump also lists. A gene sitting at
            // its baseline is left out.
            boolean always = gene == Genes.SEX || gene == Genes.EXTENSION || gene == Genes.AGOUTI;
            boolean baseline = pair.homozygousFor(gene.defaultAllele());
            if (!always && baseline && !showAll) {
                continue;
            }
            boolean on = !expr.wildType();           // really changes the coat
            boolean carrier = !on && !baseline;      // carries a variant, silent outcome

            List<String> descLines = GuiText.wrap(this.font, phenotypeText(gene, expr),
                    Math.max(60, descW));
            List<String> valueLines = epigeneticLines(gene);
            // column 1 is 2 lines (name + tokens); column 2 is 1 line (outcome
            // name), the wrapped description, then one line per stored value -
            // the row has to clear whichever is taller.
            int lineStep = this.font.lineHeight + 1;
            int rowLines = Math.max(2, 1 + descLines.size() + valueLines.size());
            int rowH = 4 + lineStep * rowLines;

            if (y + rowH >= top && y <= viewBottom) {
                if (on || carrier) {
                    g.fill(pl + 1, y, pr - 1, y + rowH, on ? 0x14FFFFFF : 0x0EFFFFFF);
                }
                g.fill(pl + 4, y + rowH - 1, pr - 4, y + rowH, ROW_LINE);

                // column 1: gene name, then the horse's two tokens
                drawFitted(g, gene.name(), pl + 8, y + 3, nameW, GENE_NAME);
                g.text(this.font,
                        Component.literal(pair.first().token() + " / " + pair.second().token()),
                        pl + 8, y + 3 + this.font.lineHeight + 1, ALLELES);

                // column 2: the outcome name, then its wrapped description
                int nameColour = on ? EXPR_ON : carrier ? EXPR_CARRIER : EXPR_OFF;
                int descColour = on || carrier ? DESC : DESC_OFF;
                g.text(this.font, Component.literal(expr.name()), split + 8, y + 3, nameColour);
                int dy = y + 3 + this.font.lineHeight + 1;
                for (String line : descLines) {
                    g.text(this.font, Component.literal(line), split + 8, dy, descColour);
                    dy += this.font.lineHeight + 1;
                }
                for (String line : valueLines) {
                    g.text(this.font, Component.literal(line), split + 8, dy,
                            on || carrier ? VALUE_ON : VALUE_OFF);
                    dy += this.font.lineHeight + 1;
                }
            }

            g.fill(split, y, split + 1, y + rowH, ROW_LINE);
            y += rowH;
            contentH += rowH;
            shownRows++;
        }
        g.disableScissor();
        if (shownRows == 0) {
            g.text(this.font, Component.literal("This horse carries nothing but the baseline."),
                    pl + 8, top + 6, DESC_OFF);
        }

        maxScroll = Math.max(0f, contentH - (viewBottom - top));
        scroll = Math.max(0f, Math.min(scroll, maxScroll));
        drawScrollbar(g, pr, top, viewBottom, contentH);
    }

    /** The sentence for this gene's outcome, with a sensible fallback. */
    private static String phenotypeText(Gene gene, Expression expr) {
        String d = expr.description();
        if (d != null && !d.isBlank()) {
            return d;
        }
        return expr.wildType()
                ? "Changes nothing about this horse."
                : "(no description written for this outcome)";
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int pr, int top, int viewBottom, int contentH) {
        if (maxScroll <= 0f) {
            return;
        }
        int trackH = viewBottom - top;
        int x1 = pr - 3;
        int x0 = x1 - 3;
        g.fill(x0, top, x1, viewBottom, 0x33FFFFFF);
        int thumbH = Math.max(20, (int) ((long) trackH * trackH / contentH));
        int thumbY = top + Math.round(scroll * (trackH - thumbH) / maxScroll);
        g.fill(x0, thumbY, x1, thumbY + thumbH, 0xAAFFFFFF);
    }

    private void drawFitted(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color) {
        float w = this.font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(this.font, Component.literal(text), x, y, color);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / w);
        g.text(this.font, Component.literal(text), 0, 0, color);
        pose.popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (maxScroll > 0f) {
            scroll = Math.max(0f, Math.min(maxScroll, scroll - (float) sy * 18f));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }


    // ------------------------------------------------------------------
    // The stored numbers
    // ------------------------------------------------------------------

    /**
     * One line per value this gene writes on an allele copy, showing both
     * copies with the expressed one marked:
     *
     * <pre>
     *   cover      &gt; 41%      9%
     *   coronet      .31 .80 .12 .60
     * </pre>
     *
     * Empty for the majority of genes, which declare no schema because their
     * behaviour is fixed by their alleles - two {@code Hlr/Hlr} horses heal
     * identically and there is nothing per-horse to show.
     */
    private List<String> epigeneticLines(Gene gene) {
        EpiSchema schema = gene.epiSchema();
        if (epigenome == null || schema.isEmpty()) {
            return List.of();
        }
        Epigenome.Copies copies = epigenome.copies(gene);
        boolean firstExpressed = epigenome.expressed(gene, genotype) == copies.first();
        EpiValues a = copies.first().values();
        EpiValues b = copies.second().values();

        List<String> out = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (EpiValue v : schema.values()) {
            if (done.contains(v.name())) {
                continue;
            }
            String colour = colourPrefix(schema, v.name());
            if (colour != null) {
                done.add(colour + "_r");
                done.add(colour + "_g");
                done.add(colour + "_b");
                out.add(row(colour, firstExpressed, hex(a.rgb(colour)), hex(b.rgb(colour))));
                continue;
            }
            done.add(v.name());
            out.add(row(v.name(), firstExpressed, show(v, a), show(v, b)));
        }
        return out;
    }

    /** {@code <name>  <A>  <B>}, with a caret on whichever copy the horse shows. */
    private static String row(String name, boolean firstExpressed, String a, String b) {
        return GuiText.clip(name, 18) + "   "
                + (firstExpressed ? "\u203a" : " ") + a + "   "
                + (firstExpressed ? " " : "\u203a") + b;
    }

    /**
     * {@code p} if {@code p_r}, {@code p_g} and {@code p_b} are all declared -
     * a colour, which reads far better as one hex value than as three numbers.
     */
    private static String colourPrefix(EpiSchema schema, String name) {
        if (!name.endsWith("_r")) {
            return null;
        }
        String prefix = name.substring(0, name.length() - 2);
        return schema.indexOf(prefix + "_g") >= 0 && schema.indexOf(prefix + "_b") >= 0
                ? prefix : null;
    }

    private static String hex(int rgb) {
        StringBuilder sb = new StringBuilder(Integer.toHexString(rgb & 0xFFFFFF));
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return "#" + sb;
    }

    /** One value, formatted for a person rather than for a codec. */
    private static String show(EpiValue v, EpiValues values) {
        if (v.kind() == EpiValue.Kind.SEED) {
            // Truncated: the full 16 digits say nothing a person can act on, and
            // the point of showing it at all is "these two horses match / do not".
            String h = Long.toHexString(values.seed(v.name()));
            return "#" + (h.length() > 6 ? h.substring(0, 6) : h);
        }
        if (v.kind() == EpiValue.Kind.CATEGORY) {
            return Integer.toString(values.category(v.name()));
        }
        if (v.arity() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int leg = 0; leg < v.arity(); leg++) {
                if (leg > 0) {
                    sb.append(' ');
                }
                sb.append(num(values.get(v.name(), leg)));
            }
            return sb.toString();
        }
        return num(values.get(v.name()));
    }

    /** Three decimals, trailing zeros trimmed - enough to tell two horses apart. */
    private static String num(double d) {
        long scaled = Math.round(d * 1000);
        StringBuilder sb = new StringBuilder();
        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }
        sb.append(scaled / 1000);
        long frac = scaled % 1000;
        if (frac != 0) {
            String f = Long.toString(frac);
            int end = f.length();
            while (end > 0 && f.charAt(end - 1) == '0') {
                end--;
            }
            sb.append('.');
            for (int pad = f.length(); pad < 3; pad++) {
                sb.append('0');
            }
            sb.append(f, 0, end);
        }
        return sb.toString();
    }
}
