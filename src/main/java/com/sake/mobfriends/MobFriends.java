package com.sake.mobfriends;

import com.mojang.logging.LogUtils;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.client.ClientSetup;
// import com.sake.mobfriends.config.MobFriendsConfig; // <-- 暂时注释掉
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.init.ModTriggers; // <-- 确保导入
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

@Mod(MobFriends.MOD_ID)
public class MobFriends {
    public static final String MOD_ID = "mob_friends";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MobFriends(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModTriggers.TRIGGERS.register(modEventBus); // <-- 修复1: 添加这行来注册触发器

        // 修复2: 暂时注释掉Config注册，我们稍后处理
        // modEventBus.addListener(MobFriendsConfig::register);

        // 修复3: 正确的触发器注册监听方式
        modEventBus.addListener(ModTriggers::register);

        if (FMLLoader.getDist().isClient()) {
            // 修复4: 暂时注释掉ClientSetup，之后再修复它
            // modEventBus.addListener(ClientSetup::init);
        }
    }
}