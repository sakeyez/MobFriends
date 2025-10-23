package com.sake.mobfriends.trading;

import com.google.gson.annotations.SerializedName;


public class TradeOffer {
    @SerializedName("buyA")
    public String buyA; // 第一个购买物品的ID，例如 "minecraft:rotten_flesh"
    @SerializedName("buyACount")
    public int buyACount = 1; // 数量，默认为1
    @SerializedName("buyB")
    public String buyB; // 第二个购买物品的ID（可选）
    @SerializedName("buyBCount")
    public int buyBCount = 1;
    @SerializedName("sell")
    public String sell; // 售卖物品的ID
    @SerializedName("sellCount")
    public int sellCount = 1;
    @SerializedName("maxUses")
    public int maxUses = 10; // 最大交易次数
    @SerializedName("xp")
    public int xp = 2; // 交易给予的经验值
    @SerializedName("priceMultiplier")
    public float priceMultiplier = 0.05F; // 价格乘数（用于需求变化）
}