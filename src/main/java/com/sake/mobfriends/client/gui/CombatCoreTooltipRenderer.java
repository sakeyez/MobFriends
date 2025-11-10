package com.sake.mobfriends.client.gui;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CombatCoreTooltipRenderer implements ClientTooltipComponent {

    private final Entity displayEntity;
    private final Component entityName;

    private final int combatLevel;
    private final int combatHappiness;
    private final int combatLevelCap;
    private final int diningFoodsCount;
    private final int ritualFoodsCount;

    private final Component levelTitle;
    private final Component tasteTitle0;
    private final Component tasteTitle1;
    private final Component tasteTitle2;

    private final int yOffset; // Y轴偏移量

    // --- 【布局常量定义】 ---
    private static final int TOP_PADDING = 4; // 顶部和底部的空白
    private static final int ENTITY_AREA_WIDTH = 50; // 为实体渲染保留的固定宽度
    private static final int ENTITY_AREA_HEIGHT = 60; // 实体画框高度
    private static final int ENTITY_TO_TEXT_PADDING = 4; // 实体和文本之间的空白

    private static final int LINE_HEIGHT = 9; // 文本行高
    private static final int LINE_PADDING = 2; // 文本行间距
    private static final int TOTAL_LINE_SPACING = LINE_HEIGHT + LINE_PADDING; // 11

    private static final int TEXT_COLOR = 0x55FFFF; // 天蓝色 (Aqua)


    public CombatCoreTooltipRenderer(CombatCoreTooltip tooltip) {
        Component entityName1;
        CompoundTag entityTag = tooltip.getEntityTag();
        Level level = Minecraft.getInstance().level;
        this.displayEntity = EntityType.loadEntityRecursive(entityTag, level, e -> e);

        this.combatLevel = tooltip.getCombatLevel();
        this.combatHappiness = tooltip.getCombatHappiness();
        this.combatLevelCap = tooltip.getCombatLevelCap();
        this.diningFoodsCount = tooltip.getDiningFoodsCount();
        this.ritualFoodsCount = tooltip.getRitualFoodsCount();

        this.levelTitle = Component.translatable("tooltip.mob_friends.combat.level_title");
        this.tasteTitle0 = Component.translatable("tooltip.mob_friends.combat.taste_title.0");
        this.tasteTitle1 = Component.translatable("tooltip.mob_friends.combat.taste_title.1");
        this.tasteTitle2 = Component.translatable("tooltip.mob_friends.combat.taste_title.2");

        // (原有的名字加载逻辑)
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

        // --- 【Y轴偏移量】 ---
        int offset = 0;
        if (entityTag.contains("id", 8)) {
            String entityId = entityTag.getString("id");
            if (entityId.equals("mob_friends:combat_zombie")) {
                offset = 6; // 僵尸上移6
            } else if (entityId.equals("mob_friends:combat_creeper")) {
                offset = 10; // 苦力怕上移10
            } else if (entityId.equals("mob_friends:combat_blaze")) {
                offset = 5; // 烈焰人上移5
            }
        }
        this.yOffset = offset;
    }

    @Override
    public int getHeight() {
        int textBlockHeight = (TOTAL_LINE_SPACING * 4) - LINE_PADDING;
        return Math.max(textBlockHeight, ENTITY_AREA_HEIGHT) + TOP_PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        // (此方法保持不变, 布局交换不影响最大宽度的计算)
        int nameWidth = (this.entityName != null) ? font.width(this.entityName) : 0;

        Component levelText = Component.literal(this.levelTitle.getString() + "：" + this.combatLevel);

        Component happinessText;
        if (this.combatLevel >= this.combatLevelCap) {
            happinessText = Component.literal("MAX");
        } else {
            int maxHappiness = (int) (100 * (1.0 + 0.5 * this.ritualFoodsCount));
            happinessText = Component.literal(this.combatHappiness + "/" + maxHappiness);
        }

        Component tasteTitle;
        if (this.diningFoodsCount < 10) {
            tasteTitle = this.tasteTitle0;
        } else if (this.diningFoodsCount < 20) {
            tasteTitle = this.tasteTitle1;
        } else {
            tasteTitle = this.tasteTitle2;
        }
        Component tasteText = Component.literal(tasteTitle.getString() + " (" + this.diningFoodsCount + ")");

        int levelWidth = font.width(levelText);
        int happinessWidth = font.width(happinessText);
        int tasteWidth = font.width(tasteText);

        int maxTextWidth = Math.max(Math.max(nameWidth, levelWidth), Math.max(happinessWidth, tasteWidth));

        return ENTITY_AREA_WIDTH + ENTITY_TO_TEXT_PADDING + maxTextWidth;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {

        int totalHeight = this.getHeight();

        // --- 1. 绘制实体 (左侧) ---
        if (this.displayEntity != null) {
            int entity_x = x + (ENTITY_AREA_WIDTH / 2);
            int scale = 25;
            int entity_y = (y + totalHeight - TOP_PADDING) - this.yOffset;

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

        // --- 2. 绘制所有文本 (右侧) ---
        int text_x = x + ENTITY_AREA_WIDTH + ENTITY_TO_TEXT_PADDING;
        int textBlockHeight = (TOTAL_LINE_SPACING * 4) - LINE_PADDING; // 4行
        int currentY = y + (totalHeight - textBlockHeight) / 2;

        // 第1行：名字 (使用 TEXT_COLOR)
        if (this.entityName != null) {
            guiGraphics.drawString(font, this.entityName, text_x, currentY, TEXT_COLOR, true);
            currentY += TOTAL_LINE_SPACING;
        }

        // --- 【布局修改：第 2 行 (原第3行)】 ---
        // 第2行：幸福感 (使用 TEXT_COLOR)
        Component happinessText;
        if (this.combatLevel >= this.combatLevelCap) {
            happinessText = Component.literal("MAX");
        } else {
            int maxHappiness = (int) (100 * (1.0 + 0.5 * this.ritualFoodsCount));
            String maxHappinessDisplay = String.valueOf(maxHappiness);
            happinessText = Component.literal(this.combatHappiness + "/" + maxHappinessDisplay);
        }
        guiGraphics.drawString(font, happinessText, text_x, currentY, TEXT_COLOR, true);
        currentY += TOTAL_LINE_SPACING;

        // --- 【布局修改：第 3 行 (原第2行)】 ---
        // 第3行：食阶 (彩色)
        int levelColor;
        if (this.combatLevel <= 10) {
            levelColor = 0x55FF55; // 绿色
        } else if (this.combatLevel <= 20) {
            levelColor = 0xAA00AA; // 紫色
        } else {
            levelColor = 0xFFD700; // 金色
        }
        Component levelText = Component.literal(this.levelTitle.getString() + "：" + this.combatLevel);
        guiGraphics.drawString(font, levelText, text_x, currentY, levelColor, true);
        currentY += TOTAL_LINE_SPACING;

        // 第4行：品尝等级 (彩色)
        Component tasteTitle;
        if (this.diningFoodsCount < 10) {
            tasteTitle = this.tasteTitle0;
        } else if (this.diningFoodsCount < 20) {
            tasteTitle = this.tasteTitle1;
        } else {
            tasteTitle = this.tasteTitle2;
        }
        Component tasteText = Component.literal(tasteTitle.getString() + " (" + this.diningFoodsCount + ")");

        int tasteColor;
        if (this.diningFoodsCount < 10) { // 10及以下为绿色
            tasteColor = 0x55FF55;
        } else if (this.diningFoodsCount < 20) { // 11-20为紫色
            tasteColor = 0xAA00AA;
        } else { // 20以上为金色
            tasteColor = 0xFFD700;
        }

        guiGraphics.drawString(font, tasteText, text_x, currentY, tasteColor, true);
    }
}