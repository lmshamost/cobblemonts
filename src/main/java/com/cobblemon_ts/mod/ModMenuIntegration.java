package com.cobblemon_ts.mod;

/*=================================================================
||  线性依赖: ModMenuApi -> Cloth Config 通用配置界面
||  线性结构: 客户端(HUD/捕捉/主题 CobblemonTSConfig) -> 服务端(EV/战利品/雷达/CharmConfig)
||  线性作用: 装了 Mod Menu + Cloth Config 时打开通用配置 GUI(输入框/开关/列表)
||  线性落盘: 点"保存并退出"触发 saveConsumer 写回内存 -> savingRunnable 两份配置全部落盘
||  线性下游: 无 Mod Menu 时仍可手动编辑 config/cobblemonts/*.json
||  线性说明: 颜色编辑器不在此 GUI，由游戏内构建工具(HudSettingsScreen/ColorEditorScreen)编辑
===================================================================*/

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // 未装 Cloth Config 时不注册配置屏幕，用户可直接编辑 config/cobblemonts/*.json
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return null;
        }
        return this::createConfigScreen;
    }

    // [线性-B1] 构建 Cloth Config 通用配置界面（客户端/服务端两类）
    private Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("CobblemonTS 配置"));
        builder.setSavingRunnable(() -> {
            CobblemonTSConfig.save();   // HUD/捕捉/主题/EV/战利品/雷达 -> cobblemon_ts_global.json
            CharmConfig.save();         // 护符/道具/编辑器/区块禁止器 -> cobblemon_ts.json
        });

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory client = builder.getOrCreateCategory(Text.literal("客户端"));
        ConfigCategory server = builder.getOrCreateCategory(Text.literal("服务端"));

        buildClient(eb, client);
        buildServer(eb, server);

        return builder.build();
    }

    // ============ 客户端：HUD显示 + 捕捉模式 + 亮暗主题（玩家自己可改） ============
    private static void buildClient(ConfigEntryBuilder eb, ConfigCategory cat) {
        // --- HUD 药水效果 ---
        addBool(eb, cat, "药水Buff图标", CobblemonTSConfig.SHOW_EFFECT_ICONS.get(),
                "屏幕右上角显示药水buff图标", v -> CobblemonTSConfig.SHOW_EFFECT_ICONS.set(v));
        addBool(eb, cat, "背包药水图标", CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS.get(),
                "背包界面显示药水buff图标", v -> CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS.set(v));
        addBool(eb, cat, "药水图标缩小", CobblemonTSConfig.HUD_POTION_SHRINK.get(),
                "是否缩小药水buff图标", v -> CobblemonTSConfig.HUD_POTION_SHRINK.set(v));
        addDouble(eb, cat, "药水缩小比例", CobblemonTSConfig.HUD_POTION_SHRINK_SCALE.get(), 0.25, 1.0,
                "药水图标缩放比例（仅缩小开启时生效）", v -> CobblemonTSConfig.HUD_POTION_SHRINK_SCALE.set(v));
        addBool(eb, cat, "隐藏药水闪烁", CobblemonTSConfig.HUD_POTION_HIDE_FLASH.get(),
                "药水快结束时不再闪烁提示", v -> CobblemonTSConfig.HUD_POTION_HIDE_FLASH.set(v));

        // --- HUD 统一彩色条 ---
        addBool(eb, cat, "统一彩色数值条", CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get(),
                "总开关：覆盖所有原版HUD为彩色数值条", v -> CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.set(v));
        addBool(eb, cat, "氧气数值条", CobblemonTSConfig.HUD_UNIFIED_AIR_BAR.get(),
                "显示氧气数值条", v -> CobblemonTSConfig.HUD_UNIFIED_AIR_BAR.set(v));
        addBool(eb, cat, "骑乘生命条", CobblemonTSConfig.HUD_UNIFIED_VEHICLE_BAR.get(),
                "显示骑乘生物生命数值条", v -> CobblemonTSConfig.HUD_UNIFIED_VEHICLE_BAR.set(v));
        addBool(eb, cat, "经验数值条", CobblemonTSConfig.HUD_UNIFIED_XP_BAR.get(),
                "显示经验数值条", v -> CobblemonTSConfig.HUD_UNIFIED_XP_BAR.set(v));

        // --- HUD 逐项开关（统一模式关闭时生效） ---
        addBool(eb, cat, "自定义血条", CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR.get(),
                "用自定义血条替代原版心形", v -> CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR.set(v));
        addBool(eb, cat, "吸收金条覆盖", CobblemonTSConfig.HUD_ABSORPTION_OVERLAY.get(),
                "血条上显示伤害吸收金条", v -> CobblemonTSConfig.HUD_ABSORPTION_OVERLAY.set(v));
        addBool(eb, cat, "自定义盔甲条", CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR.get(),
                "用自定义盔甲条替代原版盔甲图标", v -> CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR.set(v));
        addBool(eb, cat, "自定义饥饿条", CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR.get(),
                "用自定义饥饿条替代原版鸡腿", v -> CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR.set(v));
        addBool(eb, cat, "饱腹金条覆盖", CobblemonTSConfig.HUD_SATURATION_OVERLAY.get(),
                "饥饿条上显示饱腹值金条", v -> CobblemonTSConfig.HUD_SATURATION_OVERLAY.set(v));
        addBool(eb, cat, "自定义经验条", CobblemonTSConfig.HUD_CUSTOM_XP_BAR.get(),
                "用自定义经验值条替代原版经验条", v -> CobblemonTSConfig.HUD_CUSTOM_XP_BAR.set(v));

        // --- HUD 其他 ---
        addBool(eb, cat, "隐藏开关按钮", CobblemonTSConfig.HIDE_TOGGLE_BUTTONS.get(),
                "隐藏背包精灵栏/护符栏的开关按钮", v -> CobblemonTSConfig.HIDE_TOGGLE_BUTTONS.set(v));
        addInt(eb, cat, "数值显示上限", CobblemonTSConfig.HUD_NUMERIC_DISPLAY_MAX.get(), 0, 2147483647,
                "超出此值的属性在数值条上用k/M/G简写", v -> CobblemonTSConfig.HUD_NUMERIC_DISPLAY_MAX.set(v));
        addBool(eb, cat, "强制Summary缩放", CobblemonTSConfig.HUD_FORCE_SUMMARY_GUISCALE.get(),
                "界面尺寸2时强制精灵信息界面使用GUI缩放4", v -> CobblemonTSConfig.HUD_FORCE_SUMMARY_GUISCALE.set(v));
        addBool(eb, cat, "Summary文字缩放", CobblemonTSConfig.HUD_SUMMARY_TEXT_SCALE.get(),
                "低界面尺寸时自动放大Summary文字", v -> CobblemonTSConfig.HUD_SUMMARY_TEXT_SCALE.set(v));

        // --- 捕捉模式 ---
        addBool(eb, cat, "另一种捕捉模式", CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get(),
                "点击捕捉按钮弹出三选一菜单（捕获/恢复/战斗道具）", v -> CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.set(v));

        // --- 亮暗主题 ---
        addBool(eb, cat, "亮色面板主题", CobblemonTSConfig.LIGHT_PANEL_THEME.get(),
                "精灵栏/护符栏使用亮色白底主题", v -> CobblemonTSConfig.LIGHT_PANEL_THEME.set(v));
        addBool(eb, cat, "亮色编辑器主题", CobblemonTSConfig.LIGHT_EDITOR_THEME.get(),
                "宝可梦编辑器使用亮色主题", v -> CobblemonTSConfig.LIGHT_EDITOR_THEME.set(v));
    }

    // ============ 服务端：EV + 战利品/配方/雷达 + CharmConfig（护符/开局道具/道具袋/幸运方块/编辑器/区块禁止器） ============
    private static void buildServer(ConfigEntryBuilder eb, ConfigCategory cat) {
        // --- EV 努力值（服务端能力值计算，服主设置） ---
        addBool(eb, cat, "EV上限252×6", CobblemonTSConfig.ENABLE_UNLIMITED_EVS.get(),
                "开启后每项努力值上限252、总和上限252×6=1512（原版为510）。修改后请重启游戏。",
                v -> CobblemonTSConfig.ENABLE_UNLIMITED_EVS.set(v));

        // --- 战利品 / 配方 / 雷达 ---
        addBool(eb, cat, "护符战利品掉落", CobblemonTSConfig.CHARM_LOOT_ENABLED.get(),
                "护符/学习装置/金色王冠可从战利品箱获取", v -> CobblemonTSConfig.CHARM_LOOT_ENABLED.set(v));
        addBool(eb, cat, "幸运方块配方", CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get(),
                "是否启用幸运方块合成配方", v -> CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.set(v));
        addBool(eb, cat, "精灵雷达", CobblemonTSConfig.POKEMON_RADAR_ENABLED.get(),
                "是否开启精灵雷达功能", v -> CobblemonTSConfig.POKEMON_RADAR_ENABLED.set(v));

        // --- 护符设置 ---
        addDouble(eb, cat, "捕捉护符倍率", CharmConfig.CATCHING_CHARM_BOOST.get(), 1.0, 10.0,
                "捕捉成功率加成倍数 (1.5 = 提升50%)", v -> CharmConfig.CATCHING_CHARM_BOOST.set(v));
        addDouble(eb, cat, "经验护符倍率", CharmConfig.EXP_CHARM_BOOST.get(), 1.0, 10.0,
                "经验获取加成倍数 (1.5 = 提升50%)", v -> CharmConfig.EXP_CHARM_BOOST.set(v));
        addDouble(eb, cat, "圆形护符孵化倍率", CharmConfig.OVAL_CHARM_HATCH_BOOST.get(), 1.0, 10.0,
                "孵化加速倍数 (2.0 = 减少50%时间)", v -> CharmConfig.OVAL_CHARM_HATCH_BOOST.set(v));
        addDouble(eb, cat, "闪光护符倍率", CharmConfig.SHINY_CHARM_BOOST.get(), 1.0, 10000.0,
                "闪光几率加成倍数 (3.0 = 3倍几率)", v -> CharmConfig.SHINY_CHARM_BOOST.set(v));
        addDouble(eb, cat, "学习装置经验百分比", CharmConfig.EXP_ALL_PERCENT.get(), 0.0, 100.0,
                "未出战宝可梦获得的经验百分比 (50.0 = 50%)", v -> CharmConfig.EXP_ALL_PERCENT.set(v));
        addBool(eb, cat, "显示护符消息", CharmConfig.SHOW_CHARM_MESSAGES.get(),
                "是否显示护符使用提示消息", v -> CharmConfig.SHOW_CHARM_MESSAGES.set(v));

        // --- 开局道具 ---
        addBool(eb, cat, "启用开局道具", CharmConfig.START_ITEMS_ENABLED.get(),
                "玩家首次登录时是否自动发放开局道具", v -> CharmConfig.START_ITEMS_ENABLED.set(v));
        addStrList(eb, cat, "开局道具列表", CharmConfig.START_WITH_ITEMS.get(),
                "首次登录自动获得的道具ID列表", v -> CharmConfig.START_WITH_ITEMS.set(v));

        // --- 道具袋 ---
        addStrList(eb, cat, "道具袋黑名单", CharmConfig.POKEBAG_BLACKLIST_TAGS.get(),
                "禁止放入道具袋的物品ID或Tag", v -> CharmConfig.POKEBAG_BLACKLIST_TAGS.set(v));
        addStrList(eb, cat, "道具袋白名单", CharmConfig.POKEBAG_WHITELIST_TAGS.get(),
                "允许放入道具袋的物品ID或Tag", v -> CharmConfig.POKEBAG_WHITELIST_TAGS.set(v));
        addInt(eb, cat, "道具袋槽位", CharmConfig.POKEBAG_SLOTS.get(), 1, 54,
                "道具袋容量 (格子数)", v -> CharmConfig.POKEBAG_SLOTS.set(v));
        addInt(eb, cat, "最大堆叠数量", CharmConfig.POKEBAG_MAX_STACK_SIZE.get(), 1, 8192,
                "道具袋单格最大堆叠数量", v -> CharmConfig.POKEBAG_MAX_STACK_SIZE.set(v));
        addBool(eb, cat, "限制取出数量", CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.get(),
                "是否限制 shift+左键 从道具袋取出的数量", v -> CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.set(v));
        addInt(eb, cat, "取出数量上限", CharmConfig.POKEBAG_EXTRACT_LIMIT.get(), 1, 1024,
                "shift+左键 单次取出道具的最大数量", v -> CharmConfig.POKEBAG_EXTRACT_LIMIT.set(v));

        // --- 幸运方块 ---
        addStrList(eb, cat, "传说宝可梦列表", CharmConfig.LEGENDARY_POKEMON.get(),
                "传说幸运方块可召唤的传说宝可梦", v -> CharmConfig.LEGENDARY_POKEMON.set(v));
        addStrList(eb, cat, "究极异兽列表", CharmConfig.ULTRA_BEAST_POKEMON.get(),
                "究极异兽幸运方块可召唤的异兽", v -> CharmConfig.ULTRA_BEAST_POKEMON.set(v));
        addInt(eb, cat, "最低生成等级", CharmConfig.MIN_SPAWN_LEVEL.get(), 1, 100,
                "随机宝可梦幸运方块生成的最低等级", v -> CharmConfig.MIN_SPAWN_LEVEL.set(v));
        addInt(eb, cat, "最高生成等级", CharmConfig.MAX_SPAWN_LEVEL.get(), 1, 100,
                "随机宝可梦幸运方块生成的最高等级", v -> CharmConfig.MAX_SPAWN_LEVEL.set(v));
        addStrList(eb, cat, "幸运权重表", CharmConfig.LUCKY_WEIGHTS.get(),
                "格式: 方块ID|物品列表|权重|保底次数", v -> CharmConfig.LUCKY_WEIGHTS.set(v));
        addInt(eb, cat, "每级幸运权重", CharmConfig.LUCK_WEIGHT_PER_LEVEL.get(), 0, 1000,
                "每1点幸运值给最低权重物品额外加的权重", v -> CharmConfig.LUCK_WEIGHT_PER_LEVEL.set(v));

        // --- 宝可梦编辑器 ---
        addBool(eb, cat, "编辑器仅创造模式", CharmConfig.REQUIRE_CREATIVE.get(),
                "编辑器仅限创造模式使用（关闭则OP权限即可用）", v -> CharmConfig.REQUIRE_CREATIVE.set(v));
        addBool(eb, cat, "编辑器保存后自动关闭", CharmConfig.EDITOR_AUTO_CLOSE.get(),
                "编辑完成后是否自动关闭编辑器界面", v -> CharmConfig.EDITOR_AUTO_CLOSE.set(v));

        // --- 区块禁止器 ---
        addInt(eb, cat, "区块禁止默认范围", CharmConfig.CB_DEFAULT_RANGE.get(), 3, 16,
                "区块禁止器 GUI 默认显示范围 (奇数)", v -> CharmConfig.CB_DEFAULT_RANGE.set(v));
        addInt(eb, cat, "区块禁止最大范围", CharmConfig.CB_MAX_RANGE.get(), 3, 16,
                "区块禁止器 GUI 允许的最大范围 (奇数)", v -> CharmConfig.CB_MAX_RANGE.set(v));
        addInt(eb, cat, "禁止区块上限", CharmConfig.CB_BAN_CHUNK_LIMIT.get(), 0, 1024,
                "每玩家最多禁止的区块数", v -> CharmConfig.CB_BAN_CHUNK_LIMIT.set(v));
        addInt(eb, cat, "强加载区块上限", CharmConfig.CB_FORCELOAD_CHUNK_LIMIT.get(), 0, 1024,
                "每玩家最多强加载的区块数", v -> CharmConfig.CB_FORCELOAD_CHUNK_LIMIT.set(v));
    }

    // [线性-H1] 数值输入框 (Double)
    private static void addDouble(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                  double value, double min, double max, String tip, Consumer<Double> saver) {
        cat.addEntry(eb.startDoubleField(Text.literal(name), value)
                .setDefaultValue(value)
                .setMin(min).setMax(max)
                .setTooltip(Text.literal(tip))
                .setSaveConsumer(saver)
                .build());
    }

    // [线性-H2] 数值输入框 (Int)
    private static void addInt(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                               int value, int min, int max, String tip, Consumer<Integer> saver) {
        cat.addEntry(eb.startIntField(Text.literal(name), value)
                .setDefaultValue(value)
                .setMin(min).setMax(max)
                .setTooltip(Text.literal(tip))
                .setSaveConsumer(saver)
                .build());
    }

    // [线性-H3] 布尔开关
    private static void addBool(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                boolean value, String tip, Consumer<Boolean> saver) {
        cat.addEntry(eb.startBooleanToggle(Text.literal(name), value)
                .setDefaultValue(value)
                .setTooltip(Text.literal(tip))
                .setSaveConsumer(saver)
                .build());
    }

    // [线性-H4] 字符串列表编辑器
    private static void addStrList(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                   List<String> value, String tip, Consumer<List<String>> saver) {
        cat.addEntry(eb.startStrList(Text.literal(name), value)
                .setDefaultValue(value)
                .setTooltip(Text.literal(tip))
                .setSaveConsumer(saver)
                .build());
    }
}