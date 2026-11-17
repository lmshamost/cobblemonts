package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;

/** [线性-L1] 颜色编辑器配置 — Fabric/Gson版本 */
public class ColorEditorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "cobblemonts", "cobblemon_ts_colors.json");

    // ===== 精灵栏/护符栏 亮色主题 =====
    public static final ModConfig.IntWrapper PANEL_LIGHT_BG           = new ModConfig.IntWrapper(0xFFF0F0F0);
    public static final ModConfig.IntWrapper PANEL_LIGHT_BORDER       = new ModConfig.IntWrapper(0xFFAAAAAA);
    public static final ModConfig.IntWrapper PANEL_LIGHT_TEXT         = new ModConfig.IntWrapper(0xFF333333);
    public static final ModConfig.IntWrapper PANEL_LIGHT_SLOT_BG      = new ModConfig.IntWrapper(0xFFF5F5F5);
    public static final ModConfig.IntWrapper PANEL_LIGHT_SLOT_HOVER_BG= new ModConfig.IntWrapper(0xFFE0E0E0);
    public static final ModConfig.IntWrapper PANEL_LIGHT_SLOT_BORDER  = new ModConfig.IntWrapper(0xFFAAAAAA);
    public static final ModConfig.IntWrapper PANEL_LIGHT_SLOT_SELECTED_BORDER = new ModConfig.IntWrapper(0xFF00AAFF);
    public static final ModConfig.IntWrapper PANEL_LIGHT_SLOT_HOVER_BORDER    = new ModConfig.IntWrapper(0xFF777777);

    // ===== 精灵栏/护符栏 暗色主题 =====
    public static final ModConfig.IntWrapper PANEL_DARK_OUTER         = new ModConfig.IntWrapper(0xCC0B0B0B);
    public static final ModConfig.IntWrapper PANEL_DARK_INNER         = new ModConfig.IntWrapper(0xCC161616);
    public static final ModConfig.IntWrapper PANEL_DARK_TEXT          = new ModConfig.IntWrapper(0xFFFFFFFF);
    public static final ModConfig.IntWrapper PANEL_DARK_SLOT_BG       = new ModConfig.IntWrapper(0xFF2A2A2A);
    public static final ModConfig.IntWrapper PANEL_DARK_SLOT_HOVER_BG = new ModConfig.IntWrapper(0xFF3A3A3A);
    public static final ModConfig.IntWrapper PANEL_DARK_SLOT_BORDER   = new ModConfig.IntWrapper(0xFF444444);
    public static final ModConfig.IntWrapper PANEL_DARK_SLOT_SELECTED_BORDER = new ModConfig.IntWrapper(0xFF00AAFF);
    public static final ModConfig.IntWrapper PANEL_DARK_SLOT_HOVER_BORDER    = new ModConfig.IntWrapper(0xFF777777);

    // ===== 宝可梦编辑器 亮色主题 =====
    public static final ModConfig.IntWrapper EDITOR_LIGHT_CONTAINER_BG    = new ModConfig.IntWrapper(0xFFFFFFFF);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_CONTAINER_BORDER= new ModConfig.IntWrapper(0xFFB8C7DA);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_LABEL           = new ModConfig.IntWrapper(0xFF3B82F6);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_INPUT_BG        = new ModConfig.IntWrapper(0xFFFCFDFE);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_INPUT_BORDER    = new ModConfig.IntWrapper(0xFFB8C7DA);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_INPUT_TEXT      = new ModConfig.IntWrapper(0xFF0F172A);
    public static final ModConfig.IntWrapper EDITOR_LIGHT_TAB_ACTIVE      = new ModConfig.IntWrapper(0xFF3B82F6);

    // ===== 宝可梦编辑器 暗色主题 =====
    public static final ModConfig.IntWrapper EDITOR_DARK_CONTAINER_BG    = new ModConfig.IntWrapper(0xFF141414);
    public static final ModConfig.IntWrapper EDITOR_DARK_CONTAINER_BORDER= new ModConfig.IntWrapper(0xFF475569);
    public static final ModConfig.IntWrapper EDITOR_DARK_LABEL           = new ModConfig.IntWrapper(0xFF0032B4);
    public static final ModConfig.IntWrapper EDITOR_DARK_INPUT_BG        = new ModConfig.IntWrapper(0xFF1E293B);
    public static final ModConfig.IntWrapper EDITOR_DARK_INPUT_BORDER    = new ModConfig.IntWrapper(0xFF475569);
    public static final ModConfig.IntWrapper EDITOR_DARK_INPUT_TEXT      = new ModConfig.IntWrapper(0xFFF1F5F9);
    public static final ModConfig.IntWrapper EDITOR_DARK_TAB_ACTIVE      = new ModConfig.IntWrapper(0xFF2563EB);

    /** 加载颜色配置 */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                JsonObject root = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
                if (root != null) {
                    read(root, "panel_light_bg", PANEL_LIGHT_BG);
                    read(root, "panel_light_border", PANEL_LIGHT_BORDER);
                    read(root, "panel_light_text", PANEL_LIGHT_TEXT);
                    read(root, "panel_light_slot_bg", PANEL_LIGHT_SLOT_BG);
                    read(root, "panel_light_slot_hover_bg", PANEL_LIGHT_SLOT_HOVER_BG);
                    read(root, "panel_light_slot_border", PANEL_LIGHT_SLOT_BORDER);
                    read(root, "panel_light_slot_selected_border", PANEL_LIGHT_SLOT_SELECTED_BORDER);
                    read(root, "panel_light_slot_hover_border", PANEL_LIGHT_SLOT_HOVER_BORDER);
                    read(root, "panel_dark_outer", PANEL_DARK_OUTER);
                    read(root, "panel_dark_inner", PANEL_DARK_INNER);
                    read(root, "panel_dark_text", PANEL_DARK_TEXT);
                    read(root, "panel_dark_slot_bg", PANEL_DARK_SLOT_BG);
                    read(root, "panel_dark_slot_hover_bg", PANEL_DARK_SLOT_HOVER_BG);
                    read(root, "panel_dark_slot_border", PANEL_DARK_SLOT_BORDER);
                    read(root, "panel_dark_slot_selected_border", PANEL_DARK_SLOT_SELECTED_BORDER);
                    read(root, "panel_dark_slot_hover_border", PANEL_DARK_SLOT_HOVER_BORDER);
                    read(root, "editor_light_container_bg", EDITOR_LIGHT_CONTAINER_BG);
                    read(root, "editor_light_container_border", EDITOR_LIGHT_CONTAINER_BORDER);
                    read(root, "editor_light_label", EDITOR_LIGHT_LABEL);
                    read(root, "editor_light_input_bg", EDITOR_LIGHT_INPUT_BG);
                    read(root, "editor_light_input_border", EDITOR_LIGHT_INPUT_BORDER);
                    read(root, "editor_light_input_text", EDITOR_LIGHT_INPUT_TEXT);
                    read(root, "editor_light_tab_active", EDITOR_LIGHT_TAB_ACTIVE);
                    read(root, "editor_dark_container_bg", EDITOR_DARK_CONTAINER_BG);
                    read(root, "editor_dark_container_border", EDITOR_DARK_CONTAINER_BORDER);
                    read(root, "editor_dark_label", EDITOR_DARK_LABEL);
                    read(root, "editor_dark_input_bg", EDITOR_DARK_INPUT_BG);
                    read(root, "editor_dark_input_border", EDITOR_DARK_INPUT_BORDER);
                    read(root, "editor_dark_input_text", EDITOR_DARK_INPUT_TEXT);
                    read(root, "editor_dark_tab_active", EDITOR_DARK_TAB_ACTIVE);
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[ColorEditorConfig] Failed to load", e);
            }
        }
        save(); // 生成默认文件
    }

    /** 保存颜色配置 */
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            write(root, "panel_light_bg", PANEL_LIGHT_BG);
            write(root, "panel_light_border", PANEL_LIGHT_BORDER);
            write(root, "panel_light_text", PANEL_LIGHT_TEXT);
            write(root, "panel_light_slot_bg", PANEL_LIGHT_SLOT_BG);
            write(root, "panel_light_slot_hover_bg", PANEL_LIGHT_SLOT_HOVER_BG);
            write(root, "panel_light_slot_border", PANEL_LIGHT_SLOT_BORDER);
            write(root, "panel_light_slot_selected_border", PANEL_LIGHT_SLOT_SELECTED_BORDER);
            write(root, "panel_light_slot_hover_border", PANEL_LIGHT_SLOT_HOVER_BORDER);
            write(root, "panel_dark_outer", PANEL_DARK_OUTER);
            write(root, "panel_dark_inner", PANEL_DARK_INNER);
            write(root, "panel_dark_text", PANEL_DARK_TEXT);
            write(root, "panel_dark_slot_bg", PANEL_DARK_SLOT_BG);
            write(root, "panel_dark_slot_hover_bg", PANEL_DARK_SLOT_HOVER_BG);
            write(root, "panel_dark_slot_border", PANEL_DARK_SLOT_BORDER);
            write(root, "panel_dark_slot_selected_border", PANEL_DARK_SLOT_SELECTED_BORDER);
            write(root, "panel_dark_slot_hover_border", PANEL_DARK_SLOT_HOVER_BORDER);
            write(root, "editor_light_container_bg", EDITOR_LIGHT_CONTAINER_BG);
            write(root, "editor_light_container_border", EDITOR_LIGHT_CONTAINER_BORDER);
            write(root, "editor_light_label", EDITOR_LIGHT_LABEL);
            write(root, "editor_light_input_bg", EDITOR_LIGHT_INPUT_BG);
            write(root, "editor_light_input_border", EDITOR_LIGHT_INPUT_BORDER);
            write(root, "editor_light_input_text", EDITOR_LIGHT_INPUT_TEXT);
            write(root, "editor_light_tab_active", EDITOR_LIGHT_TAB_ACTIVE);
            write(root, "editor_dark_container_bg", EDITOR_DARK_CONTAINER_BG);
            write(root, "editor_dark_container_border", EDITOR_DARK_CONTAINER_BORDER);
            write(root, "editor_dark_label", EDITOR_DARK_LABEL);
            write(root, "editor_dark_input_bg", EDITOR_DARK_INPUT_BG);
            write(root, "editor_dark_input_border", EDITOR_DARK_INPUT_BORDER);
            write(root, "editor_dark_input_text", EDITOR_DARK_INPUT_TEXT);
            write(root, "editor_dark_tab_active", EDITOR_DARK_TAB_ACTIVE);
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[ColorEditorConfig] Failed to save", e);
        }
    }

    private static void read(JsonObject obj, String key, ModConfig.IntWrapper target) {
        if (obj.has(key)) target.set(obj.get(key).getAsInt());
    }
    private static void write(JsonObject obj, String key, ModConfig.IntWrapper src) {
        obj.addProperty(key, src.get());
    }

    // ===== 便捷获取方法（兼容旧调用） =====
    public static int panelLightBg() { return PANEL_LIGHT_BG.get(); }
    public static int panelLightBorder() { return PANEL_LIGHT_BORDER.get(); }
    public static int panelLightText() { return PANEL_LIGHT_TEXT.get(); }
    public static int panelLightSlotBg() { return PANEL_LIGHT_SLOT_BG.get(); }
    public static int panelLightSlotHoverBg() { return PANEL_LIGHT_SLOT_HOVER_BG.get(); }
    public static int panelLightSlotBorder() { return PANEL_LIGHT_SLOT_BORDER.get(); }
    public static int panelLightSlotSelectedBorder() { return PANEL_LIGHT_SLOT_SELECTED_BORDER.get(); }
    public static int panelLightSlotHoverBorder() { return PANEL_LIGHT_SLOT_HOVER_BORDER.get(); }
    public static int panelDarkOuter() { return PANEL_DARK_OUTER.get(); }
    public static int panelDarkInner() { return PANEL_DARK_INNER.get(); }
    public static int panelDarkText() { return PANEL_DARK_TEXT.get(); }
    public static int panelDarkSlotBg() { return PANEL_DARK_SLOT_BG.get(); }
    public static int panelDarkSlotHoverBg() { return PANEL_DARK_SLOT_HOVER_BG.get(); }
    public static int panelDarkSlotBorder() { return PANEL_DARK_SLOT_BORDER.get(); }
    public static int panelDarkSlotSelectedBorder() { return PANEL_DARK_SLOT_SELECTED_BORDER.get(); }
    public static int panelDarkSlotHoverBorder() { return PANEL_DARK_SLOT_HOVER_BORDER.get(); }
    public static int editorLightContainerBg() { return EDITOR_LIGHT_CONTAINER_BG.get(); }
    public static int editorLightContainerBorder() { return EDITOR_LIGHT_CONTAINER_BORDER.get(); }
    public static int editorLightLabel() { return EDITOR_LIGHT_LABEL.get(); }
    public static int editorLightInputBg() { return EDITOR_LIGHT_INPUT_BG.get(); }
    public static int editorLightInputBorder() { return EDITOR_LIGHT_INPUT_BORDER.get(); }
    public static int editorLightInputText() { return EDITOR_LIGHT_INPUT_TEXT.get(); }
    public static int editorLightTabActive() { return EDITOR_LIGHT_TAB_ACTIVE.get(); }
    public static int editorDarkContainerBg() { return EDITOR_DARK_CONTAINER_BG.get(); }
    public static int editorDarkContainerBorder() { return EDITOR_DARK_CONTAINER_BORDER.get(); }
    public static int editorDarkLabel() { return EDITOR_DARK_LABEL.get(); }
    public static int editorDarkInputBg() { return EDITOR_DARK_INPUT_BG.get(); }
    public static int editorDarkInputBorder() { return EDITOR_DARK_INPUT_BORDER.get(); }
    public static int editorDarkInputText() { return EDITOR_DARK_INPUT_TEXT.get(); }
    public static int editorDarkTabActive() { return EDITOR_DARK_TAB_ACTIVE.get(); }
}