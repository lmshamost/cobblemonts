package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.AbstractBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ItemLuckyBlock extends BaseLuckyBlock {
    private static volatile Map<String, ItemStack> ITEM_CACHE = null;

    public ItemLuckyBlock(AbstractBlock.Settings properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerWorld world, BlockPos pos, PlayerEntity player) {
        Identifier blockId = Registries.BLOCK.getId(this);
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);

        if (config == null) {
            triggerLegacy(world, pos);
            return;
        }

        LuckyWeightManager.WeightEntry<String> selectedEntry = LuckyWeightManager.checkGuarantee(config);
        if (selectedEntry == null) {
            selectedEntry = LuckyWeightManager.selectWeighted(config, getPlayerLuck(player));
        }

        if (selectedEntry != null) {
            String selectedItem = LuckyWeightManager.randomEntry(selectedEntry);
            if (selectedItem != null) {
                ItemStack stack = resolveItem(selectedItem, world);
                if (stack != null && !stack.isEmpty()) {
                    int count = 1 + ThreadLocalRandom.current().nextInt(3);
                    if (stack.getMaxCount() < count) {
                        count = stack.getMaxCount();
                    }
                    stack.setCount(count);
                    dropItem(world, pos, stack);
                    CobblemonTS.LOGGER.info("[LuckyBlock] Dropped {} x{} at {}",
                            stack.getName().getString(), count, pos);
                    return;
                }
            }
        }

        CobblemonTS.LOGGER.warn("[LuckyBlock] Selection empty, dropping diamond as fallback");
        ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + ThreadLocalRandom.current().nextInt(3));
        dropItem(world, pos, fallback);
    }

    // [线性-A06] 解析物品标识符 → ItemStack
    // 支持格式: "modid:item" | "NbtElement:#modid:tag" | "nbt:modid:item@{...}"
    private ItemStack resolveItem(String itemSpec, ServerWorld world) {
        if (ITEM_CACHE != null && ITEM_CACHE.containsKey(itemSpec)) {
            ItemStack cached = ITEM_CACHE.get(itemSpec);
            return cached != null ? cached.copy() : null;
        }

        ItemStack result = null;

        if (itemSpec.startsWith("NbtElement:")) {
            String tagStr = itemSpec.substring(4);
            if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
            Identifier tagId = Identifier.of(tagStr);
            TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
            Optional<RegistryEntryList.Named<Item>> tagHolders = Registries.ITEM.getEntryList(tagKey);
            if (tagHolders.isPresent() && tagHolders.get().size() > 0) {
                List<RegistryEntry<Item>> items = new ArrayList<>(tagHolders.get().stream().toList());
                Item item = items.get(ThreadLocalRandom.current().nextInt(items.size())).value();
                result = new ItemStack(item);
            } else {
                CobblemonTS.LOGGER.warn("[LuckyBlock] Tag {} not found or empty", tagStr);
            }
        }
        else if (itemSpec.startsWith("nbt:")) {
            try {
                String[] parts = itemSpec.substring(4).split("@", 2);
                if (parts.length != 2) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] NBT format error: {}", itemSpec);
                    return null;
                }
                String itemId = parts[0];
                String nbtStr = parts[1];
                String fullNbt = "{id:\"" + itemId + "\",count:1," + nbtStr + "}";
                NbtElement tag = StringNbtReader.parse(fullNbt);
                Optional<ItemStack> parsed = ItemStack.fromNbt(world.getRegistryManager(), tag);
                result = parsed.orElse(null);
                if (result == null) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] NBT item parse failed: {}", itemId);
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.warn("[LuckyBlock] NBT parse exception: {}", e.getMessage());
            }
        }
        else {
            Identifier itemId = Identifier.of(itemSpec);
            Item item = Registries.ITEM.get(itemId);
            if (item != Items.AIR) {
                result = new ItemStack(item);
            } else {
                CobblemonTS.LOGGER.warn("[LuckyBlock] Item not found: {}", itemId);
            }
        }

        if (ITEM_CACHE == null) {
            ITEM_CACHE = new ConcurrentHashMap<>();
        }
        ITEM_CACHE.put(itemSpec, result);
        return result != null ? result.copy() : null;
    }

    // [线性-A07] 旧版兼容 - 无配置时读item_drops.json均匀随机
    private void triggerLegacy(ServerWorld world, BlockPos pos) {
        List<ItemStack> drops = loadLegacyDrops(world);
        if (drops.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] item_drops list empty, dropping diamond");
            ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + ThreadLocalRandom.current().nextInt(3));
            dropItem(world, pos, fallback);
            return;
        }
        ItemStack selected = drops.get(ThreadLocalRandom.current().nextInt(drops.size())).copy();
        int count = 1 + ThreadLocalRandom.current().nextInt(3);
        if (selected.getMaxCount() < count) {
            count = selected.getMaxCount();
        }
        selected.setCount(count);
        dropItem(world, pos, selected);
        CobblemonTS.LOGGER.info("[LuckyBlock] Dropped {} x{} at {}", selected.getName().getString(), count, pos);
    }

    // [线性-A08] 加载旧版item_drops.json
    private List<ItemStack> loadLegacyDrops(ServerWorld world) {
        if (dropCache != null) {
            return dropCache;
        }

        List<ItemStack> drops = new ArrayList<>();
        Identifier jsonPath = Identifier.of(CobblemonTS.MODID, "lucky_blocks/item_drops.json");
        String path = "/data/" + jsonPath.getNamespace() + "/" + jsonPath.getPath();
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                CobblemonTS.LOGGER.error("[LuckyBlock] item_drops.json not found: {}", path);
                return drops;
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                JsonElement elem = array.get(i);
                final int index = i;
                if (!elem.isJsonObject()) continue;

                JsonObject obj = elem.getAsJsonObject();

                if (obj.has("NbtElement")) {
                    String tagStr = obj.get("NbtElement").getAsString();
                    if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                    Identifier tagId = Identifier.of(tagStr);
                    TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
                    Optional<RegistryEntryList.Named<Item>> tagHolders = Registries.ITEM.getEntryList(tagKey);
                    if (tagHolders.isPresent()) {
                        for (RegistryEntry<Item> holder : tagHolders.get()) {
                            drops.add(new ItemStack(holder.value()));
                        }
                        CobblemonTS.LOGGER.info("[LuckyBlock] [legacy] Tag {} loaded {} items", tagStr, tagHolders.get().size());
                    }
                    continue;
                }

                if (obj.has("nbt")) {
                    try {
                        String itemId = obj.get("id").getAsString();
                        String nbtStr = obj.get("nbt").getAsString();
                        String fullNbt = "{id:\"" + itemId + "\",count:1," + nbtStr + "}";
                        NbtElement tag = StringNbtReader.parse(fullNbt);
                        Optional<ItemStack> parsed = ItemStack.fromNbt(world.getRegistryManager(), tag);
                        parsed.ifPresent(drops::add);
                    } catch (Exception e) {
                        CobblemonTS.LOGGER.warn("[LuckyBlock] [legacy] entry {} NBT parse exception: {}", index, e.getMessage());
                    }
                    continue;
                }

                try {
                    DataResult<ItemStack> result = ItemStack.CODEC.parse(
                            world.getRegistryManager().getOps(JsonOps.INSTANCE),
                            elem
                    );
                    if (result.result().isPresent()) {
                        drops.add(result.result().get());
                    }
                } catch (Exception e) {
                    CobblemonTS.LOGGER.warn("[LuckyBlock] [legacy] entry {} parse exception: {}", index, e.getMessage());
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[LuckyBlock] Failed to load item_drops.json", e);
        }

        dropCache = drops;
        CobblemonTS.LOGGER.info("[LuckyBlock] [legacy] Loaded {} drop items", drops.size());
        return drops;
    }

    private static volatile List<ItemStack> dropCache = null;
}