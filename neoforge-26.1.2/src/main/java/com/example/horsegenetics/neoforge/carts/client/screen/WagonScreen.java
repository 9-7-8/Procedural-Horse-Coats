/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.client.screen;

import com.example.horsegenetics.neoforge.carts.HorseCarts;
import com.example.horsegenetics.neoforge.carts.container.WagonMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class WagonScreen extends AbstractContainerScreen<@NotNull WagonMenu> {
    private static final Identifier CONTAINER_BACKGROUND = HorseCarts.resLoc("textures/gui/container/wagon.png");
    private final int containerRows;

    public WagonScreen(WagonMenu chestMenu, Inventory inventory, Component component) {
        super(chestMenu, inventory, component, imageWidth(chestMenu), imageHeight(chestMenu));
        this.containerRows = chestMenu.getRowCount();
        // Use the un-clamped height (114 + rows*18) for the label offset, matching the original layout.
        this.inventoryLabelY = (114 + this.containerRows * 18) - 94;
        if (this.containerRows >= 12) {
            this.inventoryLabelY = this.inventoryLabelY - 54;
            this.inventoryLabelX += 27;
        }
    }

    private static int imageWidth(WagonMenu menu) {
        return menu.getRowCount() >= 12 ? 176 + 54 : 176;
    }

    private static int imageHeight(WagonMenu menu) {
        return menu.getRowCount() >= 12 ? 114 + 9 * 18 : 114 + menu.getRowCount() * 18;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (this.containerRows < 12) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    x, y, 0, 0, this.imageWidth, 17, 256, 256);
            for (int k = 0; k < this.containerRows; k++) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                        x, y + k * 18 + 17, 0, 17, this.imageWidth, 18, 256, 256);
            }
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    x, y + this.containerRows * 18 + 17, 0, 36, this.imageWidth, 96, 256, 256);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    x, y, 0, 132, 230, 17, 256, 256);
            for (int k = 0; k < 9; k++) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                        x, y + k * 18 + 17, 0, 17 + 132, 230, 18, 256, 256);
            }
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    x, y + 9 * 18 + 17, 0, 167, 230, 17, 256, 256);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    x + 27, y + 9 * 18 + 17 + 13, 0, 49, this.imageWidth, 82, 256, 256);
        }
    }


}