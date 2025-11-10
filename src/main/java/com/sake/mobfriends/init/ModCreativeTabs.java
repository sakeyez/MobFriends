package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


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
                        pOutput.accept(ModItems.WITHER_CORE.get());
                        pOutput.accept(ModItems.CREEPER_CORE.get());
                        pOutput.accept(ModItems.BLAZE_CORE.get());

                        pOutput.accept(ModItems.ZOMBIE_TOKEN.get());
                        pOutput.accept(ModItems.SKELETON_TOKEN.get());
                        pOutput.accept(ModItems.CREEPER_TOKEN.get());
                        pOutput.accept(ModItems.ENDERMAN_TOKEN.get());
                        pOutput.accept(ModItems.SLIME_TOKEN.get());
                        pOutput.accept(ModItems.BLAZE_TOKEN.get());

                        pOutput.accept(ModItems.COIN.get());
                        pOutput.accept(ModItems.POWDER.get());

                        pOutput.accept(ModItems.SOUL_BAOZI.get());

                        pOutput.accept(ModItems.ZOMBIE_NPC_SPAWN_EGG.get());
                        pOutput.accept(ModItems.SKELETON_NPC_SPAWN_EGG.get());
                        pOutput.accept(ModItems.CREEPER_NPC_SPAWN_EGG.get());
                        pOutput.accept(ModItems.ENDERMAN_NPC_SPAWN_EGG.get());
                        pOutput.accept(ModItems.SLIME_NPC_SPAWN_EGG.get());
                        pOutput.accept(ModItems.BLAZE_NPC_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}