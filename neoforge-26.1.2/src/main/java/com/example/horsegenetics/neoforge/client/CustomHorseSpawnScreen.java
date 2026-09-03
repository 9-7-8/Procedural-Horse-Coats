package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.neoforge.network.SpawnCustomHorsePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor behind the <b>custom horse spawn egg</b>. Pick an age, a sex and a
 * genome, then <b>Spawn</b>. The genome starts as a plain {@code EEaa} horse -
 * extension {@code E/E} and agouti {@code a/a}, both <b>required</b> and not
 * removable - and every other gene is added with the {@code +} button, which
 * shows a list of the genes not already in the list (including any drop-in genes
 * loaded from {@code config/horsegenetics/genes/}). Each gene row has two allele
 * buttons that cycle through that gene's alleles (so a three-allele gene is
 * fine); a gene left at wild-type / wild-type simply has no effect.
 *
 * <p>Everything here is client-only. On Spawn the genome is turned into a
 * genotype code and sent to the server as a {@link SpawnCustomHorsePayload};
 * the server does the actual spawn and the founder-record bookkeeping.
 *
 * <p>Drawing note: this is the retained-mode GUI. Widgets ({@link Button}s) are
 * rendered as part of {@code super.extractRenderState}, so anything this screen
 * paints afterwards lands <b>on top</b> of them - hence the backdrop is a few
 * narrow strips in the gaps between widgets, never a full-screen fill.
 */
public final class CustomHorseSpawnScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int ROW_H = 24;
    private static final int ROWS_TOP = 80;
    private static final int STRIP = 0x90000000;

    private boolean baby = false;
    private boolean female = true;
    private boolean picking = false;
    private int scroll = 0;

    private final List<Row> rows = new ArrayList<>();

    /** One gene in the genome list. {@code a} / {@code b} index into {@link Gene#alleles()}. */
    private static final class Row {
        final Gene gene;
        final boolean fixed;
        int a;
        int b;

        Row(Gene gene, int a, int b, boolean fixed) {
            this.gene = gene;
            this.a = a;
            this.b = b;
            this.fixed = fixed;
        }
    }

    public CustomHorseSpawnScreen() {
        super(Component.literal("Custom Horse Spawn Egg"));
        rows.add(new Row(Genes.EXTENSION, indexOf(Genes.EXTENSION, "E"), indexOf(Genes.EXTENSION, "E"), true));
        rows.add(new Row(Genes.AGOUTI, indexOf(Genes.AGOUTI, "a"), indexOf(Genes.AGOUTI, "a"), true));
    }

    private static int indexOf(Gene gene, String token) {
        List<Allele> as = gene.alleles();
        for (int i = 0; i < as.size(); i++) {
            if (as.get(i).token().equals(token)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (picking) {
            initPicker();
        } else {
            initMain();
        }
    }

    // ------------------------------------------------------------------
    // Main editor
    // ------------------------------------------------------------------

    private int rowsBottom() {
        return this.height - 70;
    }

    private int maxVisibleRows() {
        return Math.max(1, (rowsBottom() - ROWS_TOP) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - maxVisibleRows());
    }

    private void initMain() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        int cx = this.width / 2;
        int left = cx - PANEL_W / 2;
        int top = 40;

        addRenderableWidget(Button.builder(
                        Component.literal("Age: " + (baby ? "Foal" : "Adult")),
                        b -> {
                            baby = !baby;
                            rebuildWidgets();
                        })
                .bounds(left, top, 145, 20).build());
        addRenderableWidget(Button.builder(
                        Component.literal("Sex: " + (female ? "Mare / Filly" : "Stallion / Colt")),
                        b -> {
                            female = !female;
                            rebuildWidgets();
                        })
                .bounds(left + 155, top, 145, 20).build());

        int visible = maxVisibleRows();
        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            final Row row = rows.get(i);
            final List<Allele> as = row.gene.alleles();
            int ry = ROWS_TOP + (i - scroll) * ROW_H;

            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.a).token()),
                            b -> {
                                row.a = (row.a + 1) % as.size();
                                rebuildWidgets();
                            })
                    .bounds(left + 118, ry, 56, 20).build());
            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.b).token()),
                            b -> {
                                row.b = (row.b + 1) % as.size();
                                rebuildWidgets();
                            })
                    .bounds(left + 180, ry, 56, 20).build());

            if (!row.fixed) {
                addRenderableWidget(Button.builder(
                                Component.literal("x"),
                                b -> {
                                    rows.remove(row);
                                    rebuildWidgets();
                                })
                        .bounds(left + 244, ry, 20, 20).build());
            }
        }

        int addY = this.height - 64;
        if (!availableGenes().isEmpty()) {
            addRenderableWidget(Button.builder(
                            Component.literal("+  Add gene"),
                            b -> {
                                picking = true;
                                rebuildWidgets();
                            })
                    .bounds(left, addY, 130, 20).build());
        }

        int by = this.height - 26;
        addRenderableWidget(Button.builder(Component.literal("Spawn"), b -> spawn())
                .bounds(cx - 105, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx + 5, by, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!picking && maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------
    // Gene picker
    // ------------------------------------------------------------------

    private void initPicker() {
        int cx = this.width / 2;
        List<Gene> available = availableGenes();

        int listTop = 44;
        int listBottom = this.height - 40;
        int perColumn = Math.max(1, (listBottom - listTop) / 22);
        int columns = Math.max(1, (available.size() + perColumn - 1) / perColumn);
        int columnW = Math.min(170, Math.max(90, (this.width - 20) / columns));
        int startX = cx - columns * columnW / 2;

        for (int i = 0; i < available.size(); i++) {
            final Gene gene = available.get(i);
            int col = i / perColumn;
            int rowInCol = i % perColumn;
            addRenderableWidget(Button.builder(
                            Component.literal(prettyName(gene)),
                            b -> {
                                addGene(gene);
                                picking = false;
                                rebuildWidgets();
                            })
                    .bounds(startX + col * columnW, listTop + rowInCol * 22, columnW - 6, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
                    picking = false;
                    rebuildWidgets();
                })
                .bounds(cx - 50, this.height - 30, 100, 20).build());
    }

    private void addGene(Gene gene) {
        // Default a freshly added gene to homozygous for the allele that *does*
        // something (the first non-wild-type allele, which for a well-formed
        // gene is the most dominant one) - you added the row to see the gene,
        // not to leave it silent.
        List<Allele> as = gene.alleles();
        int variant = 0;
        for (int i = 0; i < as.size(); i++) {
            if (!as.get(i).equals(gene.defaultAllele())) {
                variant = i;
                break;
            }
        }
        rows.add(new Row(gene, variant, variant, false));
        scroll = maxScroll(); // jump to the newly added row
    }

    private List<Gene> availableGenes() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI) {
                continue;
            }
            boolean present = false;
            for (Row row : rows) {
                if (row.gene == gene) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                out.add(gene);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Spawn
    // ------------------------------------------------------------------

    private void spawn() {
        ClientPacketDistributor.sendToServer(new SpawnCustomHorsePayload(buildCode(), baby, female));
        onClose();
    }

    private String buildCode() {
        List<AllelePair> pairs = new ArrayList<>();
        for (Row row : rows) {
            List<Allele> as = row.gene.alleles();
            pairs.add(new AllelePair(as.get(row.a), as.get(row.b)));
        }
        return Genotype.of(pairs).toCode();
    }

    private String previewShort() {
        try {
            return GeneCodeDisplay.shortForm(Genotype.parse(buildCode()));
        } catch (RuntimeException e) {
            return "(invalid)";
        }
    }

    private static String prettyName(Gene gene) {
        String key = gene.key();
        String s = key.substring(key.lastIndexOf('.') + 1).replace('_', ' ');
        return s.isEmpty() ? key : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------
    // Drawing - narrow strips in the widget gaps, never a full-screen fill
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;

        // header band (nothing else lives above y=38 / y=44)
        g.fill(0, 8, this.width, 34, STRIP);
        g.text(this.font, this.title, cx - this.font.width(this.title) / 2, 16, 0xFFFFFFFF);

        if (picking) {
            Component sub = Component.literal(
                    availableGenes().isEmpty() ? "Every gene is already in the list" : "Pick a gene to add");
            g.text(this.font, sub, cx - this.font.width(sub) / 2, 26, 0xFFB0B0B0);
            return;
        }

        int left = cx - PANEL_W / 2;

        // strip behind the gene-name column - stops well before the allele buttons at left+118
        int visible = maxVisibleRows();
        int shown = Math.min(rows.size(), scroll + visible) - scroll;
        g.fill(left - 6, ROWS_TOP - 18, left + 112, ROWS_TOP + shown * ROW_H, 0x88000000);

        Component header = Component.literal(maxScroll() > 0
                ? "Genome  (scroll for more)"
                : "Genome  -  E and A required, the rest optional");
        g.text(this.font, header, left, ROWS_TOP - 14, 0xFF9098A8);

        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            Row row = rows.get(i);
            int ry = ROWS_TOP + (i - scroll) * ROW_H;
            g.text(this.font, Component.literal(prettyName(row.gene)),
                    left, ry + 6, row.fixed ? 0xFFFFFFFF : 0xFFD8D8D8);
            g.text(this.font, Component.literal("/"), left + 176, ry + 6, 0xFF707078);
        }

        // strip behind the preview line - above the Spawn/Cancel row at height-26
        g.fill(left - 6, this.height - 44, left + 240, this.height - 30, 0x88000000);
        g.text(this.font, Component.literal(previewShort()), left, this.height - 40, 0xFF88CC88);
    }
}
