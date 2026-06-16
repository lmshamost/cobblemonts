package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ItemLuckyBlock extends BaseLuckyBlock {
    private static final Random RANDOM = new Random();
    private static volatile List<ItemStack> dropCache = null;

    public ItemLuckyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        // [线性-A01] 加载掉落列表（静态缓存，资源文件不变）
        List<ItemStack> drops = loadDrops(level);
        if (drops.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] item_drops列表为空, 掉落钻石作为保底");
            ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + RANDOM.nextInt(3));
            dropItem(level, pos, fallback);
            return;
        }

        // [线性-A02] 随机选取一个物品掉落
        ItemStack selected = drops.get(RANDOM.nextInt(drops.size())).copy();
        int count = 1 + RANDOM.nextInt(3);
        if (selected.getMaxStackSize() < count) {
            count = selected.getMaxStackSize();
        }
        selected.setCount(count);

        dropItem(level, pos, selected);
        CobblemonTS.LOGGER.info("[LuckyBlock] 掉落{} x{} at {}", selected.getDisplayName().getString(), count, pos);
    }

    /**
     * [线性-A03] 从data/coblemon_ts/lucky_blocks/item_drops.json加载掉落列表
     * 支持三种格式：
     *   {"id": "modid:item_id"}                         → 普通物品，ItemStack.CODEC + JsonOps
     *   {"id": "modid:item_id", "nbt": "{SNBT}"}        → 带NBT物品，TagParser + ItemStack.parse
     *   {"tag": "#namespace:tag_path"}                  → 标签引用，解析标签内所有物品
     */
    private List<ItemStack> loadDrops(ServerLevel level) {
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
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                JsonElement elem = array.get(i);
                final int index = i;
                if (!elem.isJsonObject()) continue;

                JsonObject obj = elem.getAsJsonObject();

                // [线性-A04] 标签引用 {"tag": "#namespace:tag_path"}
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
                        CobblemonTS.LOGGER.info("[LuckyBlock] 标签 {} 加载了{}个物品", tagStr, tagHolders.get().size());
                    } else {
                        CobblemonTS.LOGGER.warn("[LuckyBlock] 标签 {} 未找到", tagStr);
                    }
                    continue;
                }

                // [线性-A05] NBT格式 {"id": "...", "nbt": "{SNBT}"} → TagParser + ItemStack.parse
                if (obj.has("nbt")) {
                    try {
                        String itemId = obj.get("id").getAsString();
                        String nbtStr = obj.get("nbt").getAsString();
                        // 构建完整ItemStack NBT: {id:"...",count:1,components:{...}}
                        // 用户只需提供components部分，我们自动拼接
                        // 拼接完整NBT: {id:"...",count:1,components:{...}}
                        String fullNbt = "{id:\"" + itemId + "\",count:1," + nbtStr + "}";
                        net.minecraft.nbt.Tag tag = TagParser.parseTag(fullNbt);
                        Optional<ItemStack> parsed = ItemStack.parse(level.registryAccess(), tag);
                        if (parsed.isPresent()) {
                            drops.add(parsed.get());
                            CobblemonTS.LOGGER.info("[LuckyBlock] 第{}个NBT物品加载成功: {}", index, itemId);
                        } else {
                            CobblemonTS.LOGGER.warn("[LuckyBlock] 第{}个NBT物品解析失败: {}", index, itemId);
                        }
                    } catch (Exception e) {
                        CobblemonTS.LOGGER.warn("[LuckyBlock] 第{}个NBT物品解析异常: {}", index, e.getMessage());
                    }
                    continue;
                }

                // [线性-A06] 普通物品 {"id": "modid:item_id"} → ItemStack.CODEC + JsonOps
                try {
                    DataResult<ItemStack> result = ItemStack.CODEC.parse(
                            level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                            elem
                    );
                    if (result.error().isPresent()) {
                        CobblemonTS.LOGGER.warn("[LuckyBlock] 第{}个物品CODEC解析失败: {}", index, result.error().get().message());
                    } else {
                        result.result().ifPresent(drops::add);
                    }
                } catch (Exception e) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] 第{}个物品解析异常: {}", index, e.getMessage());
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[LuckyBlock] 加载item_drops.json失败", e);
        }

        dropCache = drops;
        CobblemonTS.LOGGER.info("[LuckyBlock] 共加载{}个掉落物品", drops.size());
        return drops;
    }
}