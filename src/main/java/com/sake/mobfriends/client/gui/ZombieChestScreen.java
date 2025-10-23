package com.sake.mobfriends.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ZombieChestScreen extends AbstractContainerScreen<ZombieChestMenu> {
    // 【修正】使用 ResourceLocation.fromNamespaceAndPath
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public ZombieChestScreen(ZombieChestMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageHeight = 114 + 3 * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick); // 【修正】renderBackground 会在 super.render 中被调用
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        pGuiGraphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, 3 * 18 + 17);
        pGuiGraphics.blit(CONTAINER_BACKGROUND, x, y + 3 * 18 + 17, 0, 126, this.imageWidth, 96);
    }
}