package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.client.ClientSetup;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.entity.CombatZombie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CombatZombieRenderer extends HumanoidMobRenderer<CombatZombie, CombatZombieModel<CombatZombie>> {

    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

    public CombatZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new CombatZombieModel<>(context.bakeLayer(CombatZombieModel.LAYER_LOCATION)), 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_ZOMBIE_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_ZOMBIE_OUTER_ARMOR_LAYER)),
                Minecraft.getInstance().getModelManager()
        ));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombatZombie entity) {
        return ZOMBIE_LOCATION;
    }

    // --- 【最终正确修复】 ---
    // 移除旧的、错误的 setupRotations 方法
    // 使用新的、专门用于缩放的 scale 方法
    @Override
    protected void scale(CombatZombie zombie, PoseStack poseStack, float partialTickTime) {
        float scale = zombie.getScale();
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}