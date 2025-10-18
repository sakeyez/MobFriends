package com.sake.mobfriends.client.renderer;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.entity.CombatZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

// NeoForge 1.21.1 移植要点:
// 1. 构造函数: 渲染器的构造函数现在接收一个 EntityRendererProvider.Context 参数。
//    你需要在这个构造函数中创建一个模型实例，并传入 context.bakeLayer() 的结果。
// 2. getTextureLocation(): 这个方法返回实体的材质路径。
public class CombatZombieRenderer extends HumanoidMobRenderer<CombatZombie, CombatZombieModel> {

    // 定义材质路径
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(
            MobFriends.MOD_ID, "textures/entity/combat_zombie.png");

    public CombatZombieRenderer(EntityRendererProvider.Context context) {
        // 调用父类构造函数，传入模型实例和阴影大小
        super(context, new CombatZombieModel(context.bakeLayer(CombatZombieModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombatZombie entity) {
        return TEXTURE_LOCATION;
    }
}