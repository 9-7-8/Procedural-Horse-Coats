package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.network.SpawnCustomHorsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The editor behind the <b>custom horse spawn egg</b>: build a horse gene by
 * gene, watch it, then spawn exactly that horse.
 *
 * <h2>Layout</h2>
 * <b>Left</b> - every registered gene in <b>alphabetical order by display
 * name</b> ({@link Gene#name()}, so the list reads ACAN, Agouti, B4GALT7,
 * Champagne, EDNRB (frame overo), KIT (white spotting)&hellip; rather than in
 * the registry's processing order).
 * <b>Centre</b> - a live 3D horse in the coat the genome makes.
 * <b>Right</b> - age, sex, an epigenetics re-roll, code copy / paste, and spawn.
 *
 * <h2>The list is a catalogue you add from</h2>
 * A gene starts <b>off</b> the horse and is drawn as a plain name;
 * <b>clicking the row adds it</b>, and only then does it grow its two allele
 * buttons and an {@code x} to take it off again. Every gene carrying two
 * baseline buttons from the start was a wall of {@code N/N} that said nothing -
 * a horse carries every locus whether or not you have touched it, so what the
 * list is actually for is picking the handful you want to see.
 *
 * <p><b>A gene is added homozygous for the allele that does something</b> -
 * you clicked it to look at it, not to leave it silent. That means the first
 * allele that is not the gene's {@link Gene#defaultAllele()}, doubled; if a
 * horse cannot carry that pair ({@code KIT}'s nonviable {@code W}
 * homozygotes), the next one that it can, and failing all of them one copy
 * against the baseline. Either copy can then be cycled anywhere, baseline
 * included.
 *
 * <h2>The epigenome is part of what you are looking at</h2>
 * The screen holds a real {@link Epigenome} and previews with it.
 * <b>Reroll epi.</b> draws a new one and the preview redraws - the first place
 * per-allele epigenetics are directly visible, and how you flip through the bay
 * leg heights or grey dapplings one genotype can produce. It is then sent
 * <i>with</i> the genotype and written into the founder record, so the horse
 * that appears is the horse that was on screen; the server used to roll its
 * own, which made the preview a suggestion.
 *
 * <h2>Notes</h2>
 * Everything here is client-only, and the spawn is <b>creative-only, re-checked
 * on the server</b> ({@code ModNetworking.handleSpawnCustomHorse}).
 *
 * <p>The preview follows {@code FamilyTreeScreen.drawHorseModel}: a throwaway
 * client-only {@link Horse} whose render state gets the coat injected directly,
 * deliberately <b>without</b> touching {@link ClientCoatCache} - an edited
 * horse must never leak into how the world renders.
 *
 * <p>Drawing note: this is the retained-mode GUI. Widgets ({@link Button}s) are
 * rendered as part of {@code super.extractRenderState}, so anything this screen
 * paints afterwards lands <b>on top</b> of them - so the backdrop is drawn only
 * where no widget lives.
 */
public final class CustomHorseSpawnScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int LIST_X = 8;
    private static final int LIST_TOP = 40;
    private static final int ALLELE_W = 38;
    private static final int REMOVE_W = 14;
    private static final int RIGHT_W = 96;
    private static final int RIGHT_STEP = 22;
    private static final int PANEL = 0x90000000;

    private boolean baby = false;
    private boolean female = true;
    private int scroll = 0;

    private final List<Row> rows = new ArrayList<>();
    private Epigenome epigenome;

    /** Cached preview, rebuilt only when the genome actually changes. */
    private CoatData previewCoat;
    private String previewKey = "";
    private Horse previewHorse;
    private boolean previewHorseIsBaby;

    /**
     * One gene in the list. {@code added} is held separately rather than
     * inferred from the alleles, so a gene you added and then cycled back to
     * its baseline stays on the horse instead of silently leaving the list.
     */
    private static final class Row {
        final Gene gene;
        boolean added;
        int a;
        int b;

        Row(Gene gene) {
            this.gene = gene;
            int def = indexOf(gene, gene.defaultAllele());
            this.a = def;
            this.b = def;
        }
    }

    public CustomHorseSpawnScreen() {
        super(Component.literal("Custom Horse Spawn Egg"));
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.SEX) {
                continue; // the Mare / Stallion button owns that locus
            }
            rows.add(new Row(gene));
        }
        rows.sort(Comparator.comparing(r -> r.gene.name(), String.CASE_INSENSITIVE_ORDER));
        this.epigenome = rollEpigenome();
    }

    private static int indexOf(Gene gene, Allele allele) {
        List<Allele> as = gene.alleles();
        for (int i = 0; i < as.size(); i++) {
            if (as.get(i).equals(allele)) {
                return i;
            }
        }
        return 0;
    }

    private static Epigenome rollEpigenome() {
        return Epigenome.random(new NeoRng(RandomSource.create()));
    }

    /**
     * What a freshly added gene lands on: <b>homozygous for the first allele
     * that is not the baseline</b> - you added the row to see the gene, not to
     * leave it silent. A pair the gene rules out with {@link Gene#canOccur} is
     * skipped (KIT's four nonviable {@code W} homozygotes), and if no variant
     * homozygote is carryable at all it falls back to one copy against the
     * baseline.
     */
    private static AllelePair variantPair(Gene gene) {
        Allele base = gene.defaultAllele();
        List<Allele> alleles = gene.alleles();
        for (Allele a : alleles) {
            AllelePair homozygous = new AllelePair(a, a);
            if (!a.equals(base) && gene.canOccur(homozygous)) {
                return homozygous;
            }
        }
        for (Allele a : alleles) {
            AllelePair heterozygous = new AllelePair(a, base);
            if (!a.equals(base) && gene.canOccur(heterozygous)) {
                return heterozygous;
            }
        }
        return new AllelePair(base, base);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    /** Wide enough for a gene name and its allele buttons, narrow enough to leave a preview. */
    private int listWidth() {
        return Math.max(150, Math.min(240, this.width - RIGHT_W - 130));
    }

    private int listBottom() {
        return this.height - 44;
    }

    private int rightX() {
        return this.width - RIGHT_W - 8;
    }

    private int previewLeft() {
        return LIST_X + listWidth() + 8;
    }

    private int previewRight() {
        return rightX() - 8;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - LIST_TOP) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    /** Width available for a gene name: the whole row, less the widgets an added row carries. */
    private int nameWidth(boolean added) {
        return added ? listWidth() - 2 * ALLELE_W - REMOVE_W - 10 : listWidth() - 8;
    }

    /** The row under {@code (mouseX, mouseY)}, or {@code -1}. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < LIST_X - 4 || mouseX > LIST_X + listWidth()
                || mouseY < LIST_TOP || mouseY >= LIST_TOP + visibleRows() * ROW_H) {
            return -1;
        }
        int index = scroll + (int) ((mouseY - LIST_TOP) / ROW_H);
        return index < rows.size() ? index : -1;
    }

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        int listW = listWidth();
        int aX = LIST_X + listW - 2 * ALLELE_W - REMOVE_W - 6;
        int bX = LIST_X + listW - ALLELE_W - REMOVE_W - 4;
        int xX = LIST_X + listW - REMOVE_W;

        int visible = visibleRows();
        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            final Row row = rows.get(i);
            if (!row.added) {
                continue; // a gene not on the horse is a plain name; the row itself is the button
            }
            final List<Allele> as = row.gene.alleles();
            int ry = LIST_TOP + (i - scroll) * ROW_H;
            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.a).token()),
                            b -> {
                                row.a = (row.a + 1) % as.size();
                                rebuildWidgets();
                            })
                    .bounds(aX, ry, ALLELE_W, ROW_H - 2).build());
            addRenderableWidget(Button.builder(
                            Component.literal(as.get(row.b).token()),
                            b -> {
                                row.b = (row.b + 1) % as.size();
                                rebuildWidgets();
                            })
                    .bounds(bX, ry, ALLELE_W, ROW_H - 2).build());
            addRenderableWidget(Button.builder(
                            Component.literal("x"),
                            b -> {
                                remove(row);
                                rebuildWidgets();
                            })
                    .bounds(xX, ry, REMOVE_W, ROW_H - 2).build());
        }

        int rx = rightX();
        int ry = LIST_TOP + 4;
        addRenderableWidget(Button.builder(
                        Component.literal(baby ? "Age: Foal" : "Age: Adult"),
                        b -> {
                            baby = !baby;
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(
                        Component.literal(female ? "Sex: Mare" : "Sex: Stallion"),
                        b -> {
                            female = !female;
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(
                        Component.literal("Reroll epi."),
                        b -> {
                            epigenome = rollEpigenome();
                            rebuildWidgets();
                        })
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Copy code"), b -> copyCode())
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Paste code"), b -> pasteCode())
                .bounds(rx, ry, RIGHT_W, 20).build());
        ry += RIGHT_STEP;
        addRenderableWidget(Button.builder(Component.literal("Clear genes"), b -> reset())
                .bounds(rx, ry, RIGHT_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Spawn"), b -> spawn())
                .bounds(rx, this.height - 48, RIGHT_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(rx, this.height - 26, RIGHT_W, 20).build());
    }

    /**
     * Clicking a gene that is not on the horse adds it. The allele buttons and
     * the {@code x} on an added row are real widgets and take their own clicks
     * first, so this only ever sees the empty part of a row.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int index = rowAt(event.x(), event.y());
        if (index >= 0 && !rows.get(index).added) {
            add(rows.get(index));
            rebuildWidgets();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0 && mouseX < LIST_X + listWidth()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------
    // The genome under construction
    // ------------------------------------------------------------------

    private void add(Row row) {
        AllelePair pair = variantPair(row.gene);
        row.a = indexOf(row.gene, pair.first());
        row.b = indexOf(row.gene, pair.second());
        row.added = true;
    }

    private void remove(Row row) {
        row.added = false;
        row.a = indexOf(row.gene, row.gene.defaultAllele());
        row.b = row.a;
    }

    /**
     * Only the added rows are named; {@link Genotype#of} fills every other gene
     * with its default allele, which is what a horse carries there anyway.
     */
    private Genotype genotype() {
        List<AllelePair> pairs = new ArrayList<>();
        for (Row row : rows) {
            if (!row.added) {
                continue;
            }
            List<Allele> as = row.gene.alleles();
            pairs.add(new AllelePair(as.get(row.a), as.get(row.b)));
        }
        return Genotype.of(pairs).withSex(female ? Sex.FEMALE : Sex.MALE);
    }

    private void reset() {
        for (Row row : rows) {
            remove(row);
        }
        epigenome = rollEpigenome();
        rebuildWidgets();
    }

    /**
     * Copy the genotype code out / paste one back in - the round trip
     * {@code wiki/roadmap.html} section 9 asks for, and what turns "look at
     * this horse" into something reproducible. The epigenome is deliberately
     * not in the string: it has its own re-roll button, and a code you can
     * paste into a chat message wants to stay one line.
     */
    private void copyCode() {
        Minecraft.getInstance().keyboardHandler.setClipboard(genotype().toCode());
    }

    private void pasteCode() {
        String code = Minecraft.getInstance().keyboardHandler.getClipboard();
        Genotype parsed;
        try {
            parsed = Genotype.parse(code.trim());
        } catch (RuntimeException e) {
            return; // a clipboard full of something else is not worth a dialog
        }
        for (Row row : rows) {
            AllelePair pair = parsed.pair(row.gene);
            row.a = indexOf(row.gene, pair.first());
            row.b = indexOf(row.gene, pair.second());
            // a locus the pasted horse leaves at its baseline is not "added" -
            // it would otherwise paste back as 30-odd rows of N/N.
            row.added = !pair.homozygousFor(row.gene.defaultAllele());
        }
        female = parsed.sex() == Sex.FEMALE;
        rebuildWidgets();
    }

    private void spawn() {
        ClientPacketDistributor.sendToServer(new SpawnCustomHorsePayload(
                genotype().toCode(), epigenome.toCode(), baby, female));
        onClose();
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    /** The coat the current genome makes, rebuilt only when the genome moves. */
    private CoatData previewCoat() {
        Genotype genotype = genotype();
        String key = genotype.toCode() + '@' + epigenome.toCode();
        if (previewCoat == null || !key.equals(previewKey)) {
            previewCoat = new CoatData(genotype, epigenome);
            previewKey = key;
        }
        return previewCoat;
    }

    private Horse previewHorse() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        if (previewHorse == null) {
            try {
                previewHorse = EntityType.HORSE.create(mc.level, EntitySpawnReason.LOAD);
            } catch (RuntimeException e) {
                return null;
            }
            previewHorseIsBaby = false;
        }
        if (previewHorse != null && previewHorseIsBaby != baby) {
            previewHorse.setBaby(baby);
            previewHorseIsBaby = baby;
        }
        return previewHorse;
    }

    private void drawPreview(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = previewLeft();
        int x1 = previewRight();
        int y0 = LIST_TOP;
        int y1 = this.height - 30;
        int w = x1 - x0;
        int h = y1 - y0;
        if (w < 40 || h < 40) {
            return;
        }
        g.fill(x0, y0, x1, y1, PANEL);

        Horse horse = previewHorse();
        CoatData coat = previewCoat();
        if (horse == null) {
            return;
        }
        int cx = (x0 + x1) / 2;
        int cy = y0 + h * 2 / 3;
        // Same framing ratio the family tree uses (a 50x78 viewport at scale 16).
        float mScale = Math.min(w / 3.1F, h / 4.9F);
        try {
            EntityRenderer<? super Horse, ?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(horse);
            EntityRenderState state = renderer.createRenderState(horse, 1.0F);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof GeneticHorseRenderState gs) {
                gs.coatData = coat;
            }
            float xAngle = (float) Math.atan((cx - mouseX) / 40.0F);
            float yAngle = (float) Math.atan((cy - mouseY) / 40.0F);
            if (state instanceof LivingEntityRenderState ls) {
                ls.bodyRot = 180.0F + xAngle * 42.0F;
                ls.yRot = xAngle * 42.0F;
                ls.xRot = -yAngle * 22.0F;
                ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
                ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
                ls.scale = 1.0F;
            }
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 22.0F * ((float) Math.PI / 180.0F));
            rotation.mul(xRotation);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            g.entity(state, mScale, translation, rotation, xRotation, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        } catch (RuntimeException ignored) {
            // a coat that will not bake should not take the editor down with it
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int listW = listWidth();
        Genotype genotype = genotype();
        int visible = visibleRows();
        int shown = Math.min(rows.size(), scroll + visible) - scroll;
        int hovered = rowAt(mouseX, mouseY);

        // header band - nothing lives above the list or the right column
        g.fill(0, 6, this.width, 32, PANEL);
        g.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 14, 0xFFFFFFFF);

        // the name column only - stop short of an added row's allele buttons
        g.fill(LIST_X - 4, LIST_TOP - 14, LIST_X + nameWidth(true) + 2,
                LIST_TOP + shown * ROW_H, 0x88000000);
        Component header = Component.literal(maxScroll() > 0
                ? "Genes - click to add  (scroll)"
                : "Genes - click to add");
        g.text(this.font, header, LIST_X, LIST_TOP - 12, 0xFF9098A8);

        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            Row row = rows.get(i);
            int ry = LIST_TOP + (i - scroll) * ROW_H;
            if (!row.added) {
                // off the horse: a plain name, the whole row clickable
                if (i == hovered) {
                    g.fill(LIST_X - 4, ry, LIST_X + listW, ry + ROW_H - 2, 0x33FFFFFF);
                }
                drawFitted(g, row.gene.name(), LIST_X, ry + 6, nameWidth(false),
                        i == hovered ? 0xFFFFFFFF : 0xFF9AA0B0);
                continue;
            }
            // on the horse: name, what it expresses, and its two allele buttons
            Expression e = genotype.expressionOf(row.gene);
            boolean expressing = !e.wildType();
            g.fill(LIST_X - 4, ry, LIST_X + nameWidth(true) + 2, ry + ROW_H - 2, 0x33202838);
            drawFitted(g, row.gene.name(), LIST_X, ry + 1, nameWidth(true),
                    expressing ? 0xFF9BE08A : 0xFFC8C8C8);
            drawFitted(g, expressing ? e.name() : "no effect", LIST_X, ry + 10, nameWidth(true),
                    0xFF787888);
        }

        drawPreview(g, mouseX, mouseY);

        // the genome as text, under the list
        g.fill(LIST_X - 4, this.height - 42, LIST_X + listW, this.height - 14, 0x88000000);
        drawFitted(g, GeneCodeDisplay.shortForm(genotype), LIST_X, this.height - 39, listW - 4, 0xFF88CC88);
        drawFitted(g, "epigenetics #" + Long.toHexString(epigenome.visibleFingerprint(genotype)),
                LIST_X, this.height - 26, listW - 4, 0xFF8890A8);
    }

    /** Left-aligned at {@code (x, y)}, scaled down (never up) so the whole string fits {@code maxW}. */
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
}
