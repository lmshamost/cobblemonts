package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// [线性-A01] 物品幸运方块 - 支持权重+保底系统
// 配置来源: CharmConfig.LUCKY_WEIGHTS, 格式: 方块ID|物品列表|权重|保底
// items支持: "modid:item", "tag:#modid:tag", "nbt:modid:item@{...}" → 带NBT
public class ItemLuckyBlock extends BaseLuckyBlock {
    private static volatile Map<String, ItemStack> ITEM_CACHE = null;

    public ItemLuckyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this);
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);

        // [线性-A02] 无配置 → 回退旧行为: 读item_drops.json均匀随机
        if (config == null) {
            triggerLegacy(level, pos);
            return;
        }

        // [线性-A03] 保底优先检查 → 如果有保底命中直接选
        LuckyWeightManager.WeightEntry<String> selectedEntry = LuckyWeightManager.checkGuarantee(config);
        if (selectedEntry == null) {
            // 保底没中 → 加权随机
            selectedEntry = LuckyWeightManager.selectWeighted(config, getPlayerLuck(player));
        }

        // [线性-A04] 选中一个条目 → 随机选一个物品
        if (selectedEntry != null) {
            String selectedItem = LuckyWeightManager.randomEntry(selectedEntry);
            if (selectedItem != null) {
                ItemStack stack = resolveItem(selectedItem, level);
                if (stack != null && !stack.isEmpty()) {
                    int count = 1 + ThreadLocalRandom.current().nextInt(3);
                    if (stack.getMaxStackSize() < count) {
                        count = stack.getMaxStackSize();
                    }
                    stack.setCount(count);
                    dropItem(level, pos, stack);
                    CobblemonTS.LOGGER.info("[LuckyBlock] 掉落{} x{} at {}",
                            stack.getDisplayName().getString(), count, pos);
                    return;
                }
            }
        }

        // [线性-A05] 失败保底 → 掉落钻石
        CobblemonTS.LOGGER.warn("[LuckyBlock] 选中条目为空, 掉落钻石作为保底");
        ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + ThreadLocalRandom.current().nextInt(3));
        dropItem(level, pos, fallback);
    }

    // [线性-A06] 解析物品标识符 -> ItemStack
    // 支持三种格式:
    //   "modid:item"               → 普通物品
    //   "tag:#modid:tag_name"      → 标签随机选一个
    //   "nbt:modid:item@components:{...}" → 带NBT物品
    private ItemStack resolveItem(String itemSpec, ServerLevel level) {
        // 命中缓存直接返回
        if (ITEM_CACHE != null && ITEM_CACHE.containsKey(itemSpec)) {
            ItemStack cached = ITEM_CACHE.get(itemSpec);
            return cached != null ? cached.copy() : null;
        }

        ItemStack result = null;

        // 标签格式 "tag:#namespace:tag"
        if (itemSpec.startsWith("tag:")) {
            String tagStr = itemSpec.substring(4);
            if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
            ResourceLocation tagId = ResourceLocation.parse(tagStr);
            TagKey<Item> tagKey = ItemTags.create(tagId);
            Optional<HolderSet.Named<Item>> tagHolders = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tagHolders.isPresent() && tagHolders.get().size() > 0) {
                List<Holder<Item>> items = new ArrayList<>(tagHolders.get().stream().toList());
                Item item = items.get(ThreadLocalRandom.current().nextInt(items.size())).value();
                result = new ItemStack(item);
            } else {
                CobblemonTS.LOGGER.warn("[LuckyBlock] 标签 {} 未找到或为空", tagStr);
            }
        }
        // NBT格式 "nbt:modid:item@{...}"
        else if (itemSpec.startsWith("nbt:")) {
            try {
                String[] parts = itemSpec.substring(4).split("@", 2);
                if (parts.length != 2) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] NBT格式错误: {}", itemSpec);
                    return null;
                }
                String itemId = parts[0];
                String nbtStr = parts[1];
                // 拼接完整ItemStack NBT
                String fullNbt = "{id:\"" + itemId + "\",count:1," + nbtStr + "}";
                net.minecraft.nbt.Tag tag = TagParser.parseTag(fullNbt);
                Optional<ItemStack> parsed = ItemStack.parse(level.registryAccess(), tag);
                result = parsed.orElse(null);
                if (result == null) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] NBT物品解析失败: {}", itemId);
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.warn("[LuckyBlock] NBT解析异常: {}", e.getMessage());
            }
        }
        // 普通物品 "modid:item"
        else {
            ResourceLocation itemId = ResourceLocation.parse(itemSpec);
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != Items.AIR) {
                result = new ItemStack(item);
            } else {
                CobblemonTS.LOGGER.warn("[LuckyBlock] 物品不存在: {}", itemId);
            }
        }

        // 缓存结果
        if (ITEM_CACHE == null) {
            ITEM_CACHE = new ConcurrentHashMap<>();
        }
        ITEM_CACHE.put(itemSpec, result);
        return result != null ? result.copy() : null;
    }

    // [线性-A07] 旧版兼容 - 无配置时读item_drops.json均匀随机
    private void triggerLegacy(ServerLevel level, BlockPos pos) {
        List<ItemStack> drops = loadLegacyDrops(level);
        if (drops.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] item_drops列表为空, 掉落钻石作为保底");
            ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + ThreadLocalRandom.current().nextInt(3));
            dropItem(level, pos, fallback);
            return;
        }
        ItemStack selected = drops.get(ThreadLocalRandom.current().nextInt(drops.size())).copy();
        int count = 1 + ThreadLocalRandom.current().nextInt(3);
        if (selected.getMaxStackSize() < count) {
            count = selected.getMaxStackSize();
        }
        selected.setCount(count);
        dropItem(level, pos, selected);
        CobblemonTS.LOGGER.info("[LuckyBlock] 掉落{} x{} at {}", selected.getDisplayName().getString(), count, pos);
    }

    // [线性-A08] 加载旧版item_drops.json（缓存，只读一次）
    private List<ItemStack> loadLegacyDrops(ServerLevel level) {
        if (dropCache != null) {
            return dropCache;
        }

        List<ItemStack> drops = new ArrayList<>();
        ResourceLocation jsonPath = ResourceLocation.fromNamespaceAndPath(CobblemonTS.MODID, "lucky_blocks/item_drops.json");
        String path = "/data/" + jsonPath.getNamespace() + "/" + jsonPath.getPath();
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                CobblemonTS.LOGGER.error("[LuckyBlock] 找不到item_drops.json: {}", path);
                return drops;
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                JsonElement elem = array.get(i);
                final int index = i;
                if (!elem.isJsonObject()) continue;

                JsonObject obj = elem.getAsJsonObject();

                if (obj.has("tag")) {
                    String tagStr = obj.get("tag").getAsString();
                    if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                    ResourceLocation tagId = ResourceLocation.parse(tagStr);
                    TagKey<Item> tagKey = ItemTags.create(tagId);
                    Optional<HolderSet.Named<Item>> tagHolders = BuiltInRegistries.ITEM.getTag(tagKey);
                    if (tagHolders.isPresent()) {
                        for (Holder<Item> holder : tagHolders.get()) {
                            drops.add(new ItemStack(holder.value()));
                        }
                        CobblemonTS.LOGGER.info("[LuckyBlock] [legacy] 标签 {} 加载了{}个物品", tagStr, tagHolders.get().size());
                    }
                    continue;
                }

                if (obj.has("nbt")) {
                    try {
                        String itemId = obj.get("id").getAsString();
                        String nbtStr = obj.get("nbt").getAsString();
                        String fullNbt = "{id:\"" + itemId + "\",count:1," + nbtStr + "}";
                        net.minecraft.nbt.Tag tag = TagParser.parseTag(fullNbt);
                        Optional<ItemStack> parsed = ItemStack.parse(level.registryAccess(), tag);
                        parsed.ifPresent(drops::add);
                    } catch (Exception e) {
                        CobblemonTS.LOGGER.warn("[LuckyBlock] [legacy] 第{}个NBT解析异常: {}", index, e.getMessage());
                    }
                    continue;
                }

                try {
                    DataResult<ItemStack> result = ItemStack.CODEC.parse(
                            level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                            elem
                    );
                    if (result.result().isPresent()) {
                        drops.add(result.result().get());
                    }
                } catch (Exception e) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] [legacy] 第{}个解析异常: {}", index, e.getMessage());
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[LuckyBlock] 加载item_drops.json失败", e);
        }

        dropCache = drops;
        CobblemonTS.LOGGER.info("[LuckyBlock] [legacy] 共加载{}个掉落物品", drops.size());
        return drops;
    }

    private static volatile List<ItemStack> dropCache = null;
}