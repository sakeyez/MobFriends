package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.client.renderer.MultiBufferSource; // 确保导入

public class ZombieNpcRenderer extends ZombieRenderer {

    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

    public ZombieNpcRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /**
     * 【【【核心修复】】】
     * 移除了 @Override 注解，以匹配你项目中其他渲染器（如 CombatZombieRenderer）的写法。
     */
    // 【修改】移除了 @Override
    public void render(Zombie pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {

        if (pEntity.isPassenger()) {
            pPoseStack.translate(0.0D, -0.7D, 0.0D);
        }

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    // getTextureLocation 确实需要 @Override
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Zombie pEntity) {
        return ZOMBIE_LOCATION;
    }
}