package com.sake.mobfriends.client.gui;

import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ZombieCoreTooltipRenderer implements ClientTooltipComponent {

    private final Entity displayEntity;
    private final Component entityName;

    // --- 【布局常量定义】 ---
    private static final int TOP_PADDING = 2; // 名字距离画框顶部的空白
    private static final int NAME_AREA_HEIGHT = 10; // 给名字本身留出的高度
    private static final int ENTITY_AREA_HEIGHT = 55; // 给实体渲染留出的高度
    // --- 【核心修改 #1：添加一个新的常量来控制间距】 ---
    // 你可以随意修改这个值，比如改成 10, 15，来拉开名字和僵尸的距离
    private static final int NAME_TO_ENTITY_PADDING = 5; // 名字和实体之间的空白

    public ZombieCoreTooltipRenderer(ZombieCoreTooltip tooltip) {
        Component entityName1;
        CompoundTag entityTag = tooltip.getEntityTag();
        Level level = Minecraft.getInstance().level;
        this.displayEntity = EntityType.loadEntityRecursive(entityTag, level, e -> e);

        if (entityTag.contains("CustomName", 8) && level != null) {
            try {
                entityName1 = Component.Serializer.fromJson(entityTag.getString("CustomName"), level.registryAccess());
            } catch (Exception e) {
                entityName1 = this.displayEntity != null ? this.displayEntity.getName() : null;
            }
        } else if (this.displayEntity != null) {
            entityName1 = this.displayEntity.getName();
        } else {
            entityName1 = null;
        }
        this.entityName = entityName1;
    }

    @Override
    public int getHeight() {
        // 总高度 = 顶部空白 + 名字高度 + 名字与实体间空白 + 实体高度
        return TOP_PADDING + NAME_AREA_HEIGHT + NAME_TO_ENTITY_PADDING + ENTITY_AREA_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int nameWidth = this.entityName != null ? font.width(this.entityName) : 0;
        return Math.max(nameWidth, 50);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        // 绘制名字
        if (this.entityName != null) {
            int nameWidth = font.width(this.entityName);
            // 将名字绘制在顶部居中，Y坐标 = y + 顶部空白
            guiGraphics.drawString(font, this.entityName, x + (this.getWidth(font) - nameWidth) / 2, y + TOP_PADDING, 0xFFFFFFFF, true);
        }

        if (this.displayEntity == null) {
            return;
        }

        // --- 【核心修改 #2：重新计算实体Y坐标】 ---
        // 实体渲染的基准点Y坐标 = y + 顶部空白 + 名字高度 + 名字与实体间空白 + 实体高度 - 一个小偏移量
        int entity_x = x + this.getWidth(font) / 2;
        int entity_y = y + TOP_PADDING + NAME_AREA_HEIGHT + NAME_TO_ENTITY_PADDING + ENTITY_AREA_HEIGHT - 5;
        int scale = 25;

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(entity_x, entity_y, 50.0);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));

        float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float smoothTime = Minecraft.getInstance().level.getGameTime() + partialTicks;
        float rotation = smoothTime / 150.0F * 360.0F;
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(rotation));

        if (this.displayEntity instanceof LivingEntity livingEntity) {
            livingEntity.yBodyRot = 0;
            livingEntity.yHeadRot = 0;
        }

        dispatcher.setRenderShadow(false);
        dispatcher.render(this.displayEntity, 0.0, 0.0, 0.0, 0.0F, 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), LightTexture.FULL_BRIGHT);
        dispatcher.setRenderShadow(true);

        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }
}