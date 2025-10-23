package com.sake.mobfriends.trading;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 交易接口
 */
public interface INpcTrader {
    /**
     * 为指定的玩家打开交易界面。
     */
    void openTradingScreen(Player player);

    /**
     * 获取当前NPC的交易列表。
     * @return 交易列表对象
     */
    MerchantOffers getOffers();

    /**
     * （可选）设置NPC的交易列表，主要用于从管理器加载数据。
     * @param offers 新的交易列表
     */
    void setOffers(MerchantOffers offers);

    // 当交易菜单关闭时，我们需要重置交易状态
    void stopTrading();
}