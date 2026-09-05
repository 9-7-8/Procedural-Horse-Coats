package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A full-window popup, opened from the "View Genes" button on the horse
 * inventory panel: <b>one row per gene this horse carries</b>, with the horse's
 * two allele tokens and the plain-English description of the phenotype that
 * combination actually produces (resolved in genotype context, so it is the
 * outcome that really paints). It exists to make "do the alleles I see match
 * the look I expected" a glance rather than a puzzle.
 *
 * <p>Every registered gene is listed, in processing order ({@link Genes#codeOrder()}),
 * so the silent ones are visible too - they are just dimmed. Closes back to the
 * game like {@link FamilyTreeScreen}.
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

    private final Genotype genotype;
    private final String heading;

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
        this.heading = record.displayName() + " — genes";
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20)
                .build());
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
            if (!always && baseline) {
                continue;
            }
            boolean on = !expr.wildType();           // really changes the coat
            boolean carrier = !on && !baseline;      // carries a variant, silent outcome

            List<String> descLines = GuiText.wrap(this.font, phenotypeText(gene, expr),
                    Math.max(60, descW));
            // column 1 is 2 lines (name + tokens); column 2 is 1 line (outcome
            // name) plus the wrapped description - the row has to clear whichever
            // is taller.
            int lineStep = this.font.lineHeight + 1;
            int rowLines = Math.max(2, 1 + descLines.size());
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

}
