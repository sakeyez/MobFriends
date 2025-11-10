package com.sake.mobfriends.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

/**
 * 这是一个 SavedData 类，用于在世界级别存储唯一的、共享的史莱姆NPC背包。
 */
public class SlimeInventoryManager extends SavedData {

    private static final String DATA_NAME = "slime_npc_inventory";
    private final SimpleContainer sharedInventory = new SimpleContainer(27);

    /**
     * 这是从NBT加载数据时调用的构造函数
     */
    private SlimeInventoryManager(CompoundTag tag, HolderLookup.Provider registries) {
        // 从NBT加载背包内容
        this.sharedInventory.fromTag(tag.getList("Inventory", 10), registries);
    }

    /**
     * 这是创建新数据时调用的构造函数
     */
    private SlimeInventoryManager() {
        // 只是一个空背包，什么也不用做
    }

    /**
     * SavedData.Factory 定义了如何创建和加载这个数据
     */
    private static final SavedData.Factory<SlimeInventoryManager> FACTORY = new SavedData.Factory<>(
            SlimeInventoryManager::new,         // 用于创建新实例 () -> new SlimeInventoryManager()
            SlimeInventoryManager::load,        // 用于从NBT加载 (CompoundTag, RegistryAccess) -> load()
            null // (数据修复器，我们不需要)
    );

    /**
     * 这是从NBT加载数据的静态方法
     */
    public static SlimeInventoryManager load(CompoundTag tag, HolderLookup.Provider registries) {
        return new SlimeInventoryManager(tag, registries);
    }

    /**
     * 这是将数据保存到NBT的实例方法
     */
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        // 将背包内容写入NBT
        tag.put("Inventory", this.sharedInventory.createTag(registries));
        return tag;
    }

    /**
     * 【关键】这是我们从任何地方获取共享背包的入口
     */
    public static SlimeInventoryManager get(ServerLevel level) {
        // 获取主世界(Overworld)的数据存储
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();

        // 尝试获取，如果不存在就使用 FACTORY 创建一个新的
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * 公共方法，让 SlimeNpcEntity 能获取到这个背包
     */
    public SimpleContainer getInventory() {
        return this.sharedInventory;
    }
}