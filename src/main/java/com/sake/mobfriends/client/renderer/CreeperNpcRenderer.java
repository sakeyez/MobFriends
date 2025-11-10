package com.sake.mobfriends.client.renderer;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.model.CreeperNpcModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import org.jetbrains.annotations.NotNull;

public class CreeperNpcRenderer extends MobRenderer<Creeper, CreeperNpcModel<Creeper>> {

    // 【【重要！】】
    // 你需要创建这张新贴图，它应该在:
    // main/resources/assets/mob_friends/textures/entity/creeper_npc_engineer.png
    // 这张贴图应该是 64x64，包含原版苦力怕的皮肤，
    // 并在 (0, 32) 的位置画上你的护目镜贴图
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(
            MobFriends.MOD_ID, "textures/entity/creeper_npc_engineer.png");

    public CreeperNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperNpcModel<>(context.bakeLayer(CreeperNpcModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Creeper entity) {
        return TEXTURE_LOCATION;
    }
}