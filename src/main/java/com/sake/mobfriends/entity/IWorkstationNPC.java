package com.sake.mobfriends.entity;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable; // 【【【新增这行 IMPORT】】】

/**
 * 一个接口，所有与工作站绑定的NPC都必须实现它。
 */
public interface IWorkstationNPC {

    /**
     * 由 WorkstationBlockEntity 调用，用于告诉NPC它的家在哪里。
     * @param pos 工作站的方块坐标
     */
    void setWorkstationPos(BlockPos pos);

    /**
     * 用于获取NPC绑定的工作站坐标。
     * @return 工作站的坐标，如果未绑定则为 null
     */
    @Nullable
    BlockPos getWorkstationPos();
}