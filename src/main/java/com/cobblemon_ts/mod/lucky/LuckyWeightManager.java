package com.cobblemon_ts.mod.lucky;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// [线性-W01] 权重与保底管理器 - 所有幸运方块共用
// 配置格式: 方块ID|物品列表(逗号分隔)|权重(int)|保底(int)
// 从 CharmConfig.LUCKY_WEIGHTS 读取，首次触发时懒加载
// 权重=0 → 不参与加权随机, 仅靠保底
// 保底=0 → 无保底
// [线性-W01-NEW] 方块类型区分：
//   lucky_block / shiny_lucky_block → 全精灵池：所有宝可梦参与加权随机，未配置默认权重100
//   legendary_lucky_block / ultra_beast_lucky_block → 列表池：仅配置的宝可梦参与
//   item_lucky_block → 列表池：仅配置的物品参与
// 动态幸运加成: 仿原版loot_tables的quality机制
//   efective_weight = base_weight + floor(luck_weight_per_level * luck)
//   加成只作用于最低权重条目（并列最低全部加成），默认池不参与加成
// 线程安全: ThreadLocalRandom + ConcurrentHashMap + AtomicInteger
public class LuckyWeightManager {
    private static volatile Map<ResourceLocation, LuckyWeightConfig> CONFIG_CACHE = null;

    // [线性-W01b] 全精灵池方块ID（所有宝可梦默认权重100参与随机）
    private static final Set<String> ALL_POKEMON_BLOCK_IDS = Set.of(
            "coblemon_ts:lucky_block", "coblemon_ts:shiny_lucky_block");

    public static class WeightEntry<T> {
        public final List<T> entries;
        public final int weight;
        public final int guarantee;
        public final AtomicInteger counter;

        public WeightEntry(List<T> entries, int weight, int guarantee) {
            this.entries = entries;
            this.weight = weight;
            this.guarantee = guarantee;
            this.counter = guarantee > 0 ? new AtomicInteger(0) : null;
        }

        // [线性-W01a] 获取当前保底计数（供编辑器显示）
        public int getCounter() {
            return counter != null ? counter.get() : 0;
        }

        // [线性-W01b] 计算距离保底还差多少
        public int remainingToGuarantee() {
            if (guarantee <= 0 || counter == null) return -1;
            return Math.max(0, guarantee - counter.get());
        }
    }

    public static class LuckyWeightConfig {
        public final List<WeightEntry<?>> allEntries;       // [线性-W01c] 玩家配置的条目（编辑器可见、可保存）
        public final int totalWeight;                       // [线性-W01d] 总权重 = 配置条目权重和 + 默认池权重
        public final WeightEntry<String> defaultEntry;      // [线性-W01e] 全精灵池默认条目（未配置宝可梦，权重=100×数量，不保存到文件）

        LuckyWeightConfig(List<WeightEntry<?>> allEntries, int totalWeight, WeightEntry<String> defaultEntry) {
            this.allEntries = allEntries;
            this.totalWeight = totalWeight;
            this.defaultEntry = defaultEntry;
        }
    }

    // [线性-W02] 懒加载 - 从CharmConfig.LUCKY_WEIGHTS解析
    private static synchronized void loadConfigIfNeeded() {
        if (CONFIG_CACHE != null) {
            return;
        }
        CONFIG_CACHE = new ConcurrentHashMap<>();

        List<? extends String> rawLines = CharmConfig.LUCKY_WEIGHTS.get();
        if (rawLines.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyWeight] lucky_weights配置为空");
            return;
        }

        // 按方块ID分组: blockId -> [entryStrings]
        Map<ResourceLocation, List<String>> grouped = new LinkedHashMap<>();
        for (String line : rawLines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 4);
            if (parts.length < 4) {
                CobblemonTS.LOGGER.warn("[LuckyWeight] 格式错误(需要4个|分隔): {}", line);
                continue;
            }
            ResourceLocation blockId = ResourceLocation.tryParse(parts[0].trim());
            if (blockId == null) {
                CobblemonTS.LOGGER.warn("[LuckyWeight] 无效方块ID: {}", parts[0]);
                continue;
            }
            grouped.computeIfAbsent(blockId, k -> new ArrayList<>()).add(line);
        }

        // 构建每个方块的配置
        for (Map.Entry<ResourceLocation, List<String>> entry : grouped.entrySet()) {
            ResourceLocation blockId = entry.getKey();
            List<WeightEntry<?>> entryList = new ArrayList<>();
            Set<String> seenIds = new HashSet<>(); // 去重: 同一id只保留第一次出现

            for (String line : entry.getValue()) {
                String[] parts = line.split("\\|", 4);
                String itemsStr = parts[1].trim();
                int weight;
                int guarantee;
                try { weight = Integer.parseInt(parts[2].trim()); } catch (NumberFormatException e) { weight = 0; }
                try { guarantee = Integer.parseInt(parts[3].trim()); } catch (NumberFormatException e) { guarantee = 0; }

                // 解析物品列表
                List<String> items = new ArrayList<>();
                for (String item : itemsStr.split(",")) {
                    item = item.trim();
                    if (item.isEmpty()) continue;
                    // 去重: 只保留第一次出现
                    if (seenIds.add(item)) {
                        items.add(item);
                    }
                }
                if (!items.isEmpty()) {
                    entryList.add(new WeightEntry<>(items, weight, guarantee));
                }
            }

            int totalWeight = 0;
            for (WeightEntry<?> e : entryList) {
                if (e.weight > 0) {
                    totalWeight += e.weight;
                }
            }

            // [线性-W02a] 全精灵池方块：所有未配置的宝可梦默认权重100加入加权随机
            WeightEntry<String> defaultEntry = null;
            if (ALL_POKEMON_BLOCK_IDS.contains(blockId.toString())) {
                // 收集已被配置的宝可梦ID
                Set<String> configuredIds = new HashSet<>();
                for (WeightEntry<?> we : entryList) {
                    for (Object obj : we.entries) {
                        configuredIds.add(obj.toString().toLowerCase());
                    }
                }
                // 获取所有宝可梦物种，过滤出未配置的
                List<String> unconfigured = new ArrayList<>();
                try {
                    List<String> allSpecies = com.cobblemon_ts.mod.pokemoneditor.CobblemonBridge.getAllSpeciesIds();
                    for (String species : allSpecies) {
                        if (!configuredIds.contains(species.toLowerCase())) {
                            unconfigured.add(species.toLowerCase());
                        }
                    }
                } catch (Throwable t) {
                    CobblemonTS.LOGGER.warn("[LuckyWeight] 获取全精灵列表失败，全精灵池方块将仅使用配置条目", t);
                }
                if (!unconfigured.isEmpty()) {
                    // [线性-W02b] 默认条目权重 = 100 × 未配置精灵数（每个精灵个体权重100）
                    int defaultWeight = 100 * unconfigured.size();
                    defaultEntry = new WeightEntry<>(unconfigured, defaultWeight, 0);
                    totalWeight += defaultWeight;
                    CobblemonTS.LOGGER.info("[LuckyWeight] 全精灵池方块 {} 默认池: {}个精灵, 权重={}",
                            blockId, unconfigured.size(), defaultWeight);
                }
            }

            CONFIG_CACHE.put(blockId, new LuckyWeightConfig(entryList, totalWeight, defaultEntry));
            CobblemonTS.LOGGER.info("[LuckyWeight] 方块 {} 加载了{}个配置条目(+默认池?), 总权重{}",
                    blockId, entryList.size(), totalWeight);
        }

        CobblemonTS.LOGGER.info("[LuckyWeight] 共加载{}个幸运方块的权重配置", CONFIG_CACHE.size());
    }

    // [线性-W03] 获取指定方块的配置
    public static LuckyWeightConfig getConfig(ResourceLocation blockId) {
        loadConfigIfNeeded();
        return CONFIG_CACHE.get(blockId);
    }

    // [线性-W04] 保底检查 - 线程安全, AtomicInteger
    // 遍历所有guarantee>0的条目, 计数器+1, 达到阈值则命中并重置
    @SuppressWarnings("unchecked")
    public static <T> WeightEntry<T> checkGuarantee(LuckyWeightConfig config) {
        if (config == null || config.allEntries.isEmpty()) {
            return null;
        }
        for (WeightEntry<?> entry : config.allEntries) {
            if (entry.guarantee <= 0 || entry.counter == null) {
                continue;
            }
            int count = entry.counter.incrementAndGet();
            if (count >= entry.guarantee) {
                entry.counter.set(0);
                return (WeightEntry<T>) entry;
            }
        }
        return null;
    }

    // [线性-W05] 加权随机 + 动态幸运加成（仿原版loot_tables的quality机制）
    // 公式: effective_weight = base_weight + floor(luck_weight_per_level * luck)
    // 加成只作用于配置条目中非零权重最低的条目（并列最低全部加成）
    // 默认池不参与幸运加成，保持基础权重100
    // 无加成时直接走普通加权随机，O(n)性能
    @SuppressWarnings("unchecked")
    public static <T> WeightEntry<T> selectWeighted(LuckyWeightConfig config, double luck) {
        if (config == null || (config.allEntries.isEmpty() && config.defaultEntry == null)) {
            return null;
        }

        int boost = 0;
        int minWeight = 0;

        // [线性-W05a] 计算幸运加成（仅作用于配置条目，默认池不参与）
        if (luck > 0 && config.totalWeight > 0) {
            int luckPerLevel = CharmConfig.LUCK_WEIGHT_PER_LEVEL.get();
            if (luckPerLevel > 0) {
                boost = (int) Math.floor(luckPerLevel * luck); // floor(quality * luck)
                if (boost > 0) {
                    // 找到配置条目中非零权重的最小值（不包括默认池）
                    minWeight = Integer.MAX_VALUE;
                    for (WeightEntry<?> e : config.allEntries) {
                        if (e.weight > 0 && e.weight < minWeight) {
                            minWeight = e.weight;
                        }
                    }
                }
            }
        }

        // [线性-W05b] 计算调整后的总权重（含默认池）
        int adjustedTotal = 0;
        if (boost > 0) {
            for (WeightEntry<?> e : config.allEntries) {
                if (e.weight > 0) {
                    adjustedTotal += (e.weight == minWeight) ? e.weight + boost : e.weight;
                }
            }
            // 默认池权重不参与加成
            if (config.defaultEntry != null && config.defaultEntry.weight > 0) {
                adjustedTotal += config.defaultEntry.weight;
            }
        } else {
            adjustedTotal = config.totalWeight;
        }

        if (adjustedTotal <= 0) {
            return null;
        }

        // [线性-W05c] 加权随机: 区间累加命中（先配置条目，再默认池）
        int target = ThreadLocalRandom.current().nextInt(adjustedTotal);
        int current = 0;
        for (WeightEntry<?> entry : config.allEntries) {
            if (entry.weight <= 0) continue;
            int effective = (boost > 0 && entry.weight == minWeight) ? entry.weight + boost : entry.weight;
            current += effective;
            if (target < current) {
                return (WeightEntry<T>) entry;
            }
        }
        // [线性-W05d] 命中默认池：从所有未配置精灵中均匀随机选一个
        if (config.defaultEntry != null && config.defaultEntry.weight > 0) {
            current += config.defaultEntry.weight;
            if (target < current) {
                // 直接从默认池列表中随机选一个（每个精灵权重相同）
                T selected = (T) randomEntry(config.defaultEntry);
                return new WeightEntry<>((List<T>) List.of(selected), 0, 0);
            }
        }
        // 兜底: 返回默认池（如果存在）
        if (config.defaultEntry != null) {
            return (WeightEntry<T>) config.defaultEntry;
        }
        for (int i = config.allEntries.size() - 1; i >= 0; i--) {
            WeightEntry<?> e = config.allEntries.get(i);
            if (e.weight > 0) return (WeightEntry<T>) e;
        }
        return null;
    }

    // [线性-W06] 从条目中随机选一个
    public static <T> T randomEntry(WeightEntry<T> entry) {
        if (entry == null || entry.entries.isEmpty()) {
            return null;
        }
        return entry.entries.get(ThreadLocalRandom.current().nextInt(entry.entries.size()));
    }

    // [线性-W07] 重置缓存 - 配置变更时调用
    public static void resetCache() {
        CONFIG_CACHE = null;
    }
}