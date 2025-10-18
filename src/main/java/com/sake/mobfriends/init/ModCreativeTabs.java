package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// NeoForge 1.21.1 移植要点:
// 1. 注册表类型: 创造模式物品栏的注册表现在是 Registries.CREATIVE_MODE_TAB。
// 2. CreativeModeTab.Builder: 使用 Builder 模式来构建你的物品栏。
//    - title(): 设置物品栏的标题 (会根据语言文件自动翻译)。
//    - icon(): 设置物品栏的图标。
//    - displayItems(): 定义哪些物品会显示在这个物品栏中。
// 3. accept(): 在 displayItems 中，调用 pOutput.accept() 来添加物品。
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MobFriends.MOD_ID);

    public static final Supplier<CreativeModeTab> MOB_FRIENDS_TAB = CREATIVE_MODE_TABS.register("mob_friends_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.mob_friends_tab")) // 标题，需要在 en_us.json 和 zh_cn.json 中定义
                    .icon(() -> new ItemStack(ModItems.ZOMBIE_TOKEN.get())) // 设置图标为僵尸谢意
                    .displayItems((pParameters, pOutput) -> {
                        // 在这里添加所有你的物品
                        pOutput.accept(ModItems.ZOMBIE_CORE.get());
                        pOutput.accept(ModItems.BROKEN_ZOMBIE_CORE.get());
                        pOutput.accept(ModItems.WITHER_CORE.get());
                        pOutput.accept(ModItems.BROKEN_WITHER_CORE.get());

                        pOutput.accept(ModItems.ZOMBIE_TOKEN.get());
                        pOutput.accept(ModItems.SKELETON_TOKEN.get());
                        pOutput.accept(ModItems.CREEPER_TOKEN.get());
                        pOutput.accept(ModItems.ENDERMAN_TOKEN.get());
                        pOutput.accept(ModItems.SLIME_TOKEN.get());
                        pOutput.accept(ModItems.BLAZE_TOKEN.get());

                        pOutput.accept(ModItems.COIN.get());
                        pOutput.accept(ModItems.POWDER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}