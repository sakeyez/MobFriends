package com.sake.mobfriends.trading;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 交易管理器 (V2 - 等级系统重构)
 * * 现在从子文件夹加载交易，例如:
 * data/mob_friends/trades/zombie_npc/level_1.json
 * data/mob_friends/trades/zombie_npc/level_2.json
 * ...
 */
public class TradeManager extends SimplePreparableReloadListener<Map<ResourceLocation, Map<Integer, MerchantOffers>>> {
    private static final Gson GSON = new Gson();
    // 扫描 trades/ 文件夹
    private static final String TRADES_FOLDER = "trades";
    // 一个静态的Map，用于存储所有加载好的交易数据
    // 结构：Map<NpcId, Map<Level, TradeList>>
    private static Map<ResourceLocation, Map<Integer, MerchantOffers>> trades = ImmutableMap.of();

    @Override
    protected Map<ResourceLocation, Map<Integer, MerchantOffers>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // 临时的、可变的Map
        Map<ResourceLocation, Map<Integer, MerchantOffers>> preparedTrades = new HashMap<>();

        // 1. 扫描所有 "trades" 文件夹下的 .json 文件
        resourceManager.listResources(TRADES_FOLDER, location -> location.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                String path = location.getPath(); // 例如: "trades/zombie_npc/level_1.json"

                // 2. 解析路径
                String[] parts = path.split("/"); // ["trades", "zombie_npc", "level_1.json"]
                if (parts.length != 3) {
                    MobFriends.LOGGER.warn("发现无效的交易文件路径，已跳过: {}", path);
                    return;
                }

                // 3. 获取 Entity ID
                String entityName = parts[1]; // "zombie_npc"
                ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, entityName);

                // 4. 获取 Level
                String levelFileName = parts[2].replace(".json", ""); // "level_1"
                if (!levelFileName.startsWith("level_")) {
                    MobFriends.LOGGER.warn("无效的交易文件名，必须是 'level_X.json': {}", path);
                    return;
                }
                int level = Integer.parseInt(levelFileName.substring(6)); // 1
                if (level < 1 || level > 5) {
                    MobFriends.LOGGER.warn("无效的交易等级 (必须 1-5): {}", path);
                    return;
                }

                // 5. 解析交易文件
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                MerchantOffers offers = new MerchantOffers(); // 创建一个空的交易列表

                for (JsonElement tradeElement : json.getAsJsonArray("trades")) {
                    TradeOffer tradeOfferData = GSON.fromJson(tradeElement, TradeOffer.class);
                    offers.add(createMerchantOffer(tradeOfferData));
                }

                // 6. 存入Map
                // a) 获取这个NPC的等级Map (如果不存在则创建)
                Map<Integer, MerchantOffers> npcLevelMap = preparedTrades.computeIfAbsent(entityId, k -> new HashMap<>());
                // b) 将这个等级的交易列表存入
                npcLevelMap.put(level, offers);

            } catch (Exception e) {
                MobFriends.LOGGER.error("无法读取或解析交易文件: {}", location, e);
            }
        });

        return preparedTrades;
    }

    /**
     * 应用阶段：在主线程执行
     */
    @Override
    protected void apply(Map<ResourceLocation, Map<Integer, MerchantOffers>> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 将准备好的数据设为不可变，并赋值给静态变量
        ImmutableMap.Builder<ResourceLocation, Map<Integer, MerchantOffers>> builder = ImmutableMap.builder();
        object.forEach((npcId, levelMap) -> {
            builder.put(npcId, ImmutableMap.copyOf(levelMap));
        });
        trades = builder.build();

        MobFriends.LOGGER.info("加载了 {} 个NPC的交易等级数据。", trades.size());
    }

    /**
     * 辅助方法：(保持不变)
     */
    private static MerchantOffer createMerchantOffer(TradeOffer data) {
        Item buyAItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.buyA));
        ItemCost buyA = new ItemCost(buyAItem, data.buyACount);

        Optional<ItemCost> buyB = Optional.empty();
        if (data.buyB != null && !data.buyB.isEmpty()) {
            Item buyBItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.buyB));
            ItemCost itembuyB = new ItemCost(buyBItem, data.buyBCount);
            buyB = Optional.of(itembuyB);
        }

        Item sellItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.sell));
        ItemStack sell = new ItemStack(sellItem, data.sellCount);

        return new MerchantOffer(buyA, buyB, sell, 0, data.maxUses, data.xp, data.priceMultiplier);
    }

    /**
     * 【【【核心修改：getOffersFor 现在需要等级】】】
     * * @param entityTypeId NPC的ID
     * @param level 想要的交易等级 (1-5)
     * @return 该等级的交易列表副本
     */
    public static MerchantOffers getOffersFor(ResourceLocation entityTypeId, int level) {
        // 1. 获取这个NPC的所有等级
        Map<Integer, MerchantOffers> levelMap = trades.get(entityTypeId);
        if (levelMap == null) {
            MobFriends.LOGGER.warn("没有为 {} 找到任何交易等级数据。", entityTypeId);
            return new MerchantOffers(); // 返回空列表
        }

        // 2. 获取这个等级的原始交易
        MerchantOffers originalOffers = levelMap.get(level);
        if (originalOffers == null) {
            MobFriends.LOGGER.warn("没有为 {} 找到等级 {} 的交易数据。", entityTypeId, level);
            return new MerchantOffers(); // 返回空列表
        }

        // 3. 返回一个副本 (和以前一样)
        MerchantOffers offersCopy = new MerchantOffers();
        for (MerchantOffer offer : originalOffers) {
            offersCopy.add(offer.copy());
        }
        return offersCopy;
    }
}