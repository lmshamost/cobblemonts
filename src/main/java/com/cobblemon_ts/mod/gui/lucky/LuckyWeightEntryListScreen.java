 package com.cobblemon_ts.mod.gui.lucky;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.lucky.LuckyWeightManager;
import com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

// [线性-LWE-02] 幸运方块条目列表界面
// 显示选中方块的权重条目, 支持翻页, 点击条目编辑, 添加/删除条目, 保存
@OnlyIn(Dist.CLIENT)
public class LuckyWeightEntryListScreen extends Screen {
    private final ResourceLocation blockId;
    private final String blockDisplayName;
    private final Screen parent;
    private final List<LuckyWeightManager.WeightEntry<?>> entries;
    private int page = 0;
    private static final int PAGE_SIZE = 8;

    private Button prevBtn;
    private Button nextBtn;
    private Button addBtn;
    private Button saveBtn;
    private Button backBtn;

    public LuckyWeightEntryListScreen(ResourceLocation blockId, String blockDisplayName, Screen parent) {
        super(Component.literal("编辑 - " + blockDisplayName));
        this.blockId = blockId;
        this.blockDisplayName = blockDisplayName;
        this.parent = parent;
        // 获取当前配置
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
        this.entries = new ArrayList<>();
        if (config != null) {
            this.entries.addAll(config.allEntries);
        }
    }

    @Override
    protected void init() {
        int listStartY = 50;
        int itemH = 24;
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= totalPages) page = totalPages - 1;

        // 渲染条目按钮
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for (int i = start; i < end; i++) {
            int idx = i;
            int y = listStartY + (i - start) * itemH;
            LuckyWeightManager.WeightEntry<?> entry = entries.get(i);

            // 构建显示文本: 物品名 | 权重:xx | 保底:xx/yy
            String itemsStr = formatEntryItems(entry);
            String guaranteeStr;
            if (entry.guarantee > 0) {
                int remaining = entry.remainingToGuarantee();
                guaranteeStr = "§6保底:" + entry.getCounter() + "/" + entry.guarantee
                        + " §7(还需" + remaining + "次)";
            } else {
                guaranteeStr = "§7保底:无";
            }
            String display = itemsStr + "  §a权重:" + entry.weight + "  " + guaranteeStr;

            this.addRenderableWidget(Button.builder(
                    Component.literal(display),
                    btn -> Minecraft.getInstance().setScreen(
                            new LuckyWeightEditEntryScreen(blockId, blockDisplayName, idx, entry, this)))
                    .pos(this.width / 2 - 180, y)
                    .size(360, itemH - 2)
                    .build());
        }

        // 翻页按钮
        int bottomY = this.height - 60;
        prevBtn = this.addRenderableWidget(Button.builder(
                Component.literal("上一页"),
                btn -> { page--; rebuildWidgets(); })
                .pos(this.width / 2 - 120, bottomY)
                .size(60, 20)
                .build());
        prevBtn.active = page > 0;

        nextBtn = this.addRenderableWidget(Button.builder(
                Component.literal("下一页"),
                btn -> { page++; rebuildWidgets(); })
                .pos(this.width / 2 + 60, bottomY)
                .size(60, 20)
                .build());
        nextBtn.active = (page + 1) * PAGE_SIZE < entries.size();

        // 添加条目按钮
        addBtn = this.addRenderableWidget(Button.builder(
                Component.literal("+ 添加条目"),
                btn -> {
                    // [线性-B03] 添加空条目，同时同步到本地列表和配置缓存
                    LuckyWeightManager.WeightEntry<?> newEntry = new LuckyWeightManager.WeightEntry<>(
                            new ArrayList<>(List.of("")), 1, 0);
                    entries.add(newEntry);
                    // 同步到缓存
                    LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
                    if (config != null) {
                        config.allEntries.add(newEntry);
                    }
                    page = (entries.size() - 1) / PAGE_SIZE;
                    rebuildWidgets();
                })
                .pos(this.width / 2 - 180, bottomY + 25)
                .size(80, 20)
                .build());

        // 保存按钮
        saveBtn = this.addRenderableWidget(Button.builder(
                Component.literal("§a保存配置"),
                btn -> saveConfig())
                .pos(this.width / 2 - 90, bottomY + 25)
                .size(80, 20)
                .build());

        // 清理空条目按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("§c清理空条目"),
                btn -> {
                    // [线性-B03a] 同时清理本地列表和配置缓存
                    entries.removeIf(e -> e.entries.isEmpty() ||
                            (e.entries.size() == 1 && e.entries.get(0).toString().isEmpty()));
                    LuckyWeightManager.LuckyWeightConfig cfg = LuckyWeightManager.getConfig(blockId);
                    if (cfg != null) {
                        cfg.allEntries.removeIf(e -> e.entries.isEmpty() ||
                                (e.entries.size() == 1 && e.entries.get(0).toString().isEmpty()));
                    }
                    if (page * PAGE_SIZE >= entries.size() && page > 0) page--;
                    rebuildWidgets();
                })
                .pos(this.width / 2, bottomY + 25)
                .size(80, 20)
                .build());

        // 返回按钮
        backBtn = this.addRenderableWidget(Button.builder(
                Component.literal("返回"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .pos(this.width / 2 + 90, bottomY + 25)
                .size(80, 20)
                .build());
    }

    // 格式化条目中的物品列表（支持中文名）
    private String formatEntryItems(LuckyWeightManager.WeightEntry<?> entry) {
        List<String> names = new ArrayList<>();
        for (Object obj : entry.entries) {
            String s = obj.toString();
            if (s.isEmpty()) {
                names.add("§7[空]");
            } else {
                names.add(toDisplayName(s));
            }
        }
        String joined = String.join(", ", names);
        if (joined.length() > 35) {
            joined = joined.substring(0, 32) + "...";
        }
        return joined;
    }

    // 将物品ID/精灵名转为显示名（中文）
    static String toDisplayName(String raw) {
        if (raw == null || raw.isEmpty()) return "§7[空]";
        // 标签格式
        if (raw.startsWith("tag:")) return "§d" + raw;
        // NBT格式
        if (raw.startsWith("nbt:")) {
            String itemId = raw.substring(4).split("@", 2)[0];
            return "§bNBT:" + PokemonNameMap.itemToChinese(itemId);
        }
        // 尝试精灵名转换
        String zhName = PokemonNameMap.toChinese(raw);
        if (!zhName.equals(raw)) {
            return "§e" + zhName;
        }
        // 尝试物品名转换
        String itemZh = PokemonNameMap.itemToChinese(raw);
        if (!itemZh.equals(raw)) {
            return "§f" + itemZh;
        }
        return "§f" + raw;
    }

    // 保存配置到 CharmConfig — 直接写入TOML文件，绕过ModConfigSpec.set()在客户端可能静默失败
    private void saveConfig() {
        // [线性-B02] 先同步本地列表和配置缓存（编辑器可能已修改缓存但未同步到本地副本）
        LuckyWeightManager.LuckyWeightConfig config = LuckyWeightManager.getConfig(blockId);
        entries.clear();
        if (config != null) {
            entries.addAll(config.allEntries);
        }

        // 清理空条目
        entries.removeIf(e -> e.entries.isEmpty() ||
                (e.entries.size() == 1 && e.entries.get(0).toString().isEmpty()));

        // 构建新的配置行列表
        List<String> allLines = new ArrayList<>(CharmConfig.LUCKY_WEIGHTS.get());

        // 移除旧的该方块配置行
        allLines.removeIf(line -> {
            if (line == null || line.isBlank()) return false;
            String[] parts = line.split("\\|", 2);
            if (parts.length < 2) return false;
            ResourceLocation parsed = ResourceLocation.tryParse(parts[0].trim());
            return blockId.equals(parsed);
        });

        // 添加新配置行
        for (LuckyWeightManager.WeightEntry<?> entry : entries) {
            if (entry.entries.isEmpty()) continue;
            String itemsStr = String.join(",", entry.entries.stream()
                    .map(Object::toString).toList());
            String line = blockId + "|" + itemsStr + "|" + entry.weight + "|" + entry.guarantee;
            allLines.add(line);
        }

        // [线性-B04] 保存前先删除所有 common-*.toml.bak，让NeoForge永远只生成 common-1.toml.bak
        com.cobblemon_ts.mod.CobblemonTS.deleteAllBakFiles(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("CoblemonTest/common.toml"));

        CharmConfig.LUCKY_WEIGHTS.set(allLines);

        // 直接写入TOML文件确保客户端保存生效
        java.nio.file.Path configPath = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("CoblemonTest/common.toml");
        try {
            java.nio.file.Files.createDirectories(configPath.getParent());
            String content = java.nio.file.Files.readString(configPath);
            // 构建TOML数组字符串
            StringBuilder tomlArray = new StringBuilder("lucky_weights = [");
            for (int i = 0; i < allLines.size(); i++) {
                if (i > 0) tomlArray.append(", ");
                tomlArray.append("\"").append(allLines.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
            tomlArray.append("]");
            // 替换文件中的lucky_weights行
            String newContent = content.replaceAll("lucky_weights\\s*=\\s*\\[[^]]*\\]",
                    java.util.regex.Matcher.quoteReplacement(tomlArray.toString()));
            if (!newContent.equals(content)) {
                java.nio.file.Files.writeString(configPath, newContent);
            }
        } catch (java.io.IOException e) {
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.warn("[LuckyEditor] 直接写入TOML失败，依赖ModConfigSpec.set兜底", e);
        }

        // 刷新缓存
        LuckyWeightManager.resetCache();

        // 刷新当前列表
        LuckyWeightManager.LuckyWeightConfig refreshed = LuckyWeightManager.getConfig(blockId);
        entries.clear();
        if (refreshed != null) {
            entries.addAll(refreshed.allEntries);
        }
        if (page * PAGE_SIZE >= entries.size() && page > 0) page--;
        rebuildWidgets();

        // 提示玩家
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§a配置已保存! 方块: " + blockDisplayName + " 共" + entries.size() + "条"),
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, 15, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                "§7共" + entries.size() + "条  |  第" + (page + 1) + "/" +
                        Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE) + "页  |  点击条目编辑",
                this.width / 2, 32, 0xAAAAAA);
    }

    // [线性-LWE-02a] 公开刷新方法，供子编辑器回调
    public void refreshEntries() {
        this.rebuildWidgets();
    }

    // [线性-LWE-02b] 公开保存方法，供子编辑器调用
    public void saveConfigAndRefresh() {
        saveConfig();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}