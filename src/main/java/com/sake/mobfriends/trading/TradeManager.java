package com.sake.mobfriends.trading;

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
 * 交易管理器
 * 继承 SimplePreparableReloadListener，使其成为游戏数据加载/重载流程的一部分。
 * 泛型 <Map<ResourceLocation, MerchantOffers>> 定义了 prepare 方法的返回值和 apply 方法的输入值类型。
 */
public class TradeManager extends SimplePreparableReloadListener<Map<ResourceLocation, MerchantOffers>> {
    private static final Gson GSON = new Gson();
    // 定义了我们的交易文件必须放在 data/<namespace>/trades/ 目录下
    private static final String TRADES_FOLDER = "trades";
    // 一个静态的Map，用于存储所有加载好的交易数据，键是NPC的实体ID
    private static Map<ResourceLocation, MerchantOffers> trades = new HashMap<>();


    @Override
    protected Map<ResourceLocation, MerchantOffers> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MerchantOffers> preparedTrades = new HashMap<>();
        Map<ResourceLocation, JsonElement> tradeFiles = new HashMap<>();

        // 1. 扫描所有数据包中 "trades" 文件夹下所有以 ".json" 结尾的文件
        resourceManager.listResources(TRADES_FOLDER, location -> location.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                // 2. 用GSON将文件内容读成一个通用的JsonElement，并存起来
                tradeFiles.put(location, GSON.fromJson(reader, JsonElement.class));
            } catch (Exception e) {
                MobFriends.LOGGER.error("Failed to read trade file: {}", location, e);
            }
        });

        // 3. 遍历所有读取到的JSON文件
        tradeFiles.forEach((location, jsonElement) -> {
            try {
                JsonObject json = jsonElement.getAsJsonObject();
                MerchantOffers offers = new MerchantOffers(); // 创建一个空的交易列表
                // 4. 遍历JSON中名为 "trades" 的数组
                for (JsonElement tradeElement : json.getAsJsonArray("trades")) {
                    // 5. 将每个交易条目用GSON转换成我们的TradeOffer对象
                    TradeOffer tradeOfferData = GSON.fromJson(tradeElement, TradeOffer.class);
                    // 6. 将TradeOffer对象转换成游戏真正认识的MerchantOffer对象，并添加到列表
                    offers.add(createMerchantOffer(tradeOfferData));
                }
                // 7. 从文件名推断出这个交易文件对应哪个NPC
                String path = location.getPath();
                String entityName = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);
                ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, entityName);
                preparedTrades.put(entityId, offers); // 存入准备好的Map

            } catch (Exception e) {
                MobFriends.LOGGER.error("Failed to parse trade file: {}", location, e);
            }
        });

        return preparedTrades;
    }

    /**
     * 应用阶段：在主线程执行，将准备好的数据应用到游戏中。
     */
    @Override
    protected void apply(Map<ResourceLocation, MerchantOffers> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 简单地将准备好的数据赋值给静态变量，使其全局可用
        trades = object;
        MobFriends.LOGGER.info("Loaded {} NPC trade files.", trades.size());
    }

    /**
     * 辅助方法：将我们的数据模型 TradeOffer 转换成原版的 MerchantOffer。
     */
    private static MerchantOffer createMerchantOffer(TradeOffer data) {
        // 从物品ID（例如 "minecraft:stick"）获取真实的Item对象
        Item buyAItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.buyA));
        ItemCost buyA = new ItemCost(buyAItem, data.buyACount);

        Optional<ItemCost> buyB = Optional.empty(); // 1. 先准备一个默认的“空盒子”
        if (data.buyB != null && !data.buyB.isEmpty()) {
            Item buyBItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.buyB));
            // 2. 先创建出要放进盒子的 ItemCost 对象
            ItemCost itembuyB = new ItemCost(buyBItem, data.buyBCount);
            // 3. 用 Optional.of() 工厂方法把东西装进盒子
            buyB = Optional.of(itembuyB);
        }

        // 创建要出售的物品 (这部分是正确的)
        Item sellItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.sell));
        ItemStack sell = new ItemStack(sellItem, data.sellCount);

        // 根据我们找到的“说明书”，调用正确的构造函数
        // public MerchantOffer(ItemCost baseCostA, Optional<ItemCost> buyB, ItemStack result, int uses, int maxUses, int xp, float priceMultiplier)
        return new MerchantOffer(buyA, buyB, sell, 0, data.maxUses, data.xp, data.priceMultiplier);
    }

    /**
     * 公共静态方法：使用正确的 copy() 方法获取交易副本。
     */
    public static MerchantOffers getOffersFor(ResourceLocation entityTypeId) {
        MerchantOffers originalOffers = trades.get(entityTypeId);
        if (originalOffers == null) {
            return new MerchantOffers();
        }
        MerchantOffers offersCopy = new MerchantOffers();
        for (MerchantOffer offer : originalOffers) {
            // 使用我们之前在class文件中找到的 copy() 方法
            offersCopy.add(offer.copy());
        }
        return offersCopy;
    }

}