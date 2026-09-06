package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.network.RenameHorsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The rename window, opened by right-clicking a horse with any name tag. Two
 * fields - first name and last name - prefilled with the horse's current
 * registered name. Confirming sends {@link RenameHorsePayload}; the server
 * consumes one name tag. Either part may be blank, but not both.
 */
public final class HorseRenameScreen extends Screen {

    private static final int W = 220;
    private static final int H = 118;
    private static final int PANEL = 0xF0121218;
    private static final int BORDER = 0xFF3C3C4A;
    private static final int LABEL = 0xFF8890A8;

    private final int entityId;
    private EditBox firstBox;
    private EditBox lastBox;
    private Button rename;

    public HorseRenameScreen(int entityId) {
        super(Component.translatable("gui.horsegenetics.rename_horse"));
        this.entityId = entityId;
    }

    /** Open the window for {@code entityId} - the client end of {@code OpenHorseRenamePayload}. */
    public static void open(int entityId) {
        Minecraft.getInstance().setScreen(new HorseRenameScreen(entityId));
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return (this.height - H) / 2;
    }

    @Override
    protected void init() {
        HorseRecord record = ClientHorseRecordCache.get(entityId);
        String first = record != null ? record.firstName() : "";
        String last = record != null ? record.lastName() : "";

        int l = left();
        int t = top();

        firstBox = new EditBox(this.font, l + 12, t + 30, W - 24, 16, Component.literal("First name"));
        firstBox.setMaxLength(40);
        firstBox.setValue(first);
        addRenderableWidget(firstBox);

        lastBox = new EditBox(this.font, l + 12, t + 62, W - 24, 16, Component.literal("Last name"));
        lastBox.setMaxLength(40);
        lastBox.setValue(last);
        addRenderableWidget(lastBox);

        rename = Button.builder(Component.translatable("gui.horsegenetics.rename_confirm"), b -> confirm())
                .bounds(l + 12, t + H - 26, (W - 30) / 2, 20).build();
        addRenderableWidget(rename);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(l + 12 + (W - 30) / 2 + 6, t + H - 26, (W - 30) / 2, 20).build());

        setInitialFocus(firstBox);
    }

    private void confirm() {
        String first = firstBox.getValue().strip();
        String last = lastBox.getValue().strip();
        if (first.isEmpty() && last.isEmpty()) {
            return; // not both blank
        }
        ClientPacketDistributor.sendToServer(new RenameHorsePayload(entityId, first, last));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int l = left();
        int t = top();
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, BORDER);
        g.fill(l, t, l + W, t + H, PANEL);
        g.text(this.font, this.title, l + 12, t + 10, 0xFFF2F2F6, false);
        g.text(this.font, Component.literal("First name"), l + 12, t + 21, LABEL, false);
        g.text(this.font, Component.literal("Last name"), l + 12, t + 53, LABEL, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
