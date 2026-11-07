// [线性-R04] 精灵雷达编辑器界面：照搬PokemonEditorScreen下拉栏机制
//   species: 拼音搜索 + 编号搜索 + 中文匹配 + 下拉栏
//   ability: 选中物种后过滤为该物种特性 + 下拉栏
//   nature:  下拉栏
//   全部有: 滚动、hover高亮、点击选择、失去焦点自动关闭
package com.cobblemon_ts.mod.pokemonradar;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import com.cobblemon_ts.mod.pokemoneditor.CobblemonBridge;
import com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class PokemonRadarScreen extends Screen {

    private final PokemonRadarState state;

    // [线性-R04a] 模式开关按钮
    private Button legendaryBtn;
    private Button shinyBtn;
    private Button pokemonBtn;
    private Button npcBtn;               // [线性-R06e]
    private Button closeButton;

    // [线性-R04b] 输入框 + 清除按钮
    private EditBox batchSpeciesField;   private Button clearBatchSpeciesBtn;
    private EditBox singleSpeciesField;  private Button clearSingleSpeciesBtn;
    private EditBox genderField;         private Button clearGenderBtn;
    private EditBox abilityField;        private Button clearAbilityBtn;
    private EditBox natureField;         private Button clearNatureBtn;
    // [线性-R04b2] 等级+个体值
    private EditBox levelField;          private Button clearLevelBtn;
    private EditBox hpField, atkField, defField;
    private EditBox spaField, spdField, speField;
    private Button clearIvBtn;
    // [线性-R06e] NPC名字输入
    private EditBox npcNameField;        private Button clearNpcNameBtn;

    // [线性-R04c] 下拉栏状态（与编辑器同模式）
    private List<String> autocompleteItems = new ArrayList<>();
    private int autocompleteStartY = 0;
    private int autocompleteX = 0;
    private int autocompleteW = 0;
    private int autocompleteActiveField = -1; // 0=species, 1=ability, 2=nature
    private int autocompleteScrollOffset = 0;
    private String lastSearchQuery = "";

    // [线性-R04d] 界面缩放
    private float uiScaleFactor = 1.0f;
    // 布局常量
    private int panelX, panelY, panelW, panelH;
    private int inputStartX, inputStartY, inputW, rowStep;
    private int speciesY, genderY, abilityY, natureY, levelY, ivRow1Y, ivRow2Y;
    private int npcNameY;                  // [线性-R06e]
    private int editorLightBg, editorLightBorder, editorLightLabel;

    public PokemonRadarScreen() {
        super(Component.translatable("coblemon_ts.radar.title"));
        this.state = PokemonRadarState.getInstance();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int screenW = this.width;
        int screenH = this.height;

        float layoutW = 440f;
        float layoutH = 450f;  // [线性-R06-layout] 9行=240+title+close (原420,加NPC行→450)
        this.uiScaleFactor = Math.min(screenW / layoutW, screenH / layoutH);
        if (this.uiScaleFactor > 1.0f) this.uiScaleFactor = 1.0f;
        if (this.uiScaleFactor < 0.4f) this.uiScaleFactor = 0.4f;

        this.panelX = screenW / 2 - 210;
        this.panelW = 420;
        this.panelH = 380;  // [线性-R06-layout] 9行×30 + title/close区域 (原350)
        this.panelY = (screenH - panelH) / 2;  // [线性-R04-layout] 面板垂直居中

        boolean light = CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
        this.editorLightBg = light ? ColorEditorConfig.editorLightContainerBg() : ColorEditorConfig.editorDarkContainerBg();
        this.editorLightBorder = light ? ColorEditorConfig.editorLightContainerBorder() : ColorEditorConfig.editorDarkContainerBorder();
        this.editorLightLabel = light ? ColorEditorConfig.editorLightLabel() : ColorEditorConfig.editorDarkLabel();

        // [线性-R04e] 标题栏区域：四个模式按钮
        int btnY = panelY + 8;
        int btnW = 98;   // [线性-R06e] 3→4按钮,缩窄
        int btnH = 22;
        int btnGap = 4;
        int btnStartX = panelX + (panelW - (btnW * 4 + btnGap * 3)) / 2;

        this.legendaryBtn = Button.builder(getModeButtonText("legendary"), (btn) -> {
            state.setLegendaryMode(!state.isLegendaryMode());
            updateModeButtonLabels();
        }).pos(btnStartX, btnY).size(btnW, btnH).build();
        this.shinyBtn = Button.builder(getModeButtonText("shiny"), (btn) -> {
            state.setShinyMode(!state.isShinyMode());
            updateModeButtonLabels();
        }).pos(btnStartX + btnW + btnGap, btnY).size(btnW, btnH).build();
        this.pokemonBtn = Button.builder(getModeButtonText("pokemon"), (btn) -> {
            state.setPokemonMode(!state.isPokemonMode());
            updateModeButtonLabels();
        }).pos(btnStartX + (btnW + btnGap) * 2, btnY).size(btnW, btnH).build();
        this.npcBtn = Button.builder(getModeButtonText("npc"), (btn) -> {  // [线性-R06g] 三态循环: OFF→RTC→NPC→OFF
            cycleNpcMode();
            updateModeButtonLabels();
        }).pos(btnStartX + (btnW + btnGap) * 3, btnY).size(btnW, btnH).build();
        this.addRenderableWidget(this.legendaryBtn);
        this.addRenderableWidget(this.shinyBtn);
        this.addRenderableWidget(this.pokemonBtn);
        this.addRenderableWidget(this.npcBtn);        // [线性-R06e]
        updateModeButtonLabels();

        // [线性-R04f] 输入框区域
        this.rowStep = 30;
        this.inputStartY = btnY + btnH + 18;
        this.inputStartX = panelX + 105;
        this.inputW = 240;

        int r0 = 0;
        this.speciesY = inputStartY + rowStep * 1;   // 行1：精灵名单个
        int r1 = 1;
        int r2 = 2;
        this.genderY = inputStartY + rowStep * r2;
        int r3 = 3;
        this.abilityY = inputStartY + rowStep * r3;
        int r4 = 4;
        this.natureY = inputStartY + rowStep * r4;
        int r5 = 5;
        this.levelY = inputStartY + rowStep * r5;    // 行5：等级
        int r6 = 6;
        this.ivRow1Y = inputStartY + rowStep * r6;   // 行6：HP-Atk-Def
        int r7 = 7;
        this.ivRow2Y = inputStartY + rowStep * r7;   // 行7：SpA-SpD-Spe
        int r8 = 8;
        this.npcNameY = inputStartY + rowStep * r8;   // [线性-R06e] 行8：NPC名字

        // 精灵名批量（无下拉栏）
        this.batchSpeciesField = makeField(inputStartX, inputStartY + rowStep * r0, 200);
        this.batchSpeciesField.setValue(state.getBatchSpeciesInput());
        this.clearBatchSpeciesBtn = makeClearBtn(inputStartX + inputW + 4, inputStartY + rowStep * r0,
                () -> this.batchSpeciesField.setValue(""));

        // 精灵名单个（有下拉栏）
        this.singleSpeciesField = makeField(inputStartX, speciesY, 200);
        this.singleSpeciesField.setValue(state.getSingleSpeciesInput());
        this.clearSingleSpeciesBtn = makeClearBtn(inputStartX + inputW + 4, speciesY,
                () -> {
                    this.singleSpeciesField.setValue("");
                    state.setSingleSpeciesInput("");
                });

        // 性别
        this.genderField = makeField(inputStartX, genderY, 200);
        this.genderField.setValue(state.getGenderInput());
        this.clearGenderBtn = makeClearBtn(inputStartX + inputW + 4, genderY,
                () -> this.genderField.setValue(""));

        // 特性（有下拉栏）
        this.abilityField = makeField(inputStartX, abilityY, 200);
        this.abilityField.setValue(state.getAbilityInput());
        this.clearAbilityBtn = makeClearBtn(inputStartX + inputW + 4, abilityY,
                () -> this.abilityField.setValue(""));

        // 性格（有下拉栏）
        this.natureField = makeField(inputStartX, natureY, 200);
        this.natureField.setValue(state.getNatureInput());
        this.clearNatureBtn = makeClearBtn(inputStartX + inputW + 4, natureY,
                () -> this.natureField.setValue(""));

        // [线性-R04f2] 等级
        this.levelField = makeField(inputStartX, levelY, 90);
        this.levelField.setValue(state.getLevelInput());
        this.clearLevelBtn = makeClearBtn(inputStartX + 200 + 4, levelY,
                () -> this.levelField.setValue(""));

        // [线性-R04f3] 个体值 行1: HP/Atk/Def
        int ivBoxW = 70; int ivGap = 6;
        this.hpField  = makeField(inputStartX,                ivRow1Y, ivBoxW);
        this.atkField = makeField(inputStartX + ivBoxW + ivGap,   ivRow1Y, ivBoxW);
        this.defField = makeField(inputStartX + (ivBoxW+ivGap)*2, ivRow1Y, ivBoxW);
        this.hpField.setValue(state.getHpIvInput());
        this.atkField.setValue(state.getAtkIvInput());
        this.defField.setValue(state.getDefIvInput());

        // [线性-R04f4] 个体值 行2: SpA/SpD/Spe
        this.spaField = makeField(inputStartX,                ivRow2Y, ivBoxW);
        this.spdField = makeField(inputStartX + ivBoxW + ivGap,   ivRow2Y, ivBoxW);
        this.speField = makeField(inputStartX + (ivBoxW+ivGap)*2, ivRow2Y, ivBoxW);
        this.spaField.setValue(state.getSpaIvInput());
        this.spdField.setValue(state.getSpdIvInput());
        this.speField.setValue(state.getSpeIvInput());
        this.clearIvBtn = makeClearBtn(inputStartX + (ivBoxW+ivGap)*3 + 8, ivRow1Y,
                () -> { hpField.setValue(""); atkField.setValue(""); defField.setValue("");
                        spaField.setValue(""); spdField.setValue(""); speField.setValue(""); });

        // [线性-R06e] NPC名字输入（有下拉栏自动补全）
        this.npcNameField = makeField(inputStartX, npcNameY, 200);
        this.npcNameField.setValue(state.getNpcNameInput());
        this.clearNpcNameBtn = makeClearBtn(inputStartX + inputW + 4, npcNameY,
                () -> { this.npcNameField.setValue(""); state.setNpcNameInput(""); });

        // 关闭按钮
        int closeBtnW = 60;
        int closeBtnH = 20;
        this.closeButton = Button.builder(Component.translatable("coblemon_ts.radar.close"), (btn) -> {
            saveState();
            this.onClose();
        }).pos(panelX + panelW / 2 - closeBtnW / 2, panelY + panelH - 30).size(closeBtnW, closeBtnH).build();
        this.addRenderableWidget(this.closeButton);

        this.addRenderableWidget(this.batchSpeciesField);
        this.addRenderableWidget(this.singleSpeciesField);
        this.addRenderableWidget(this.genderField);
        this.addRenderableWidget(this.abilityField);
        this.addRenderableWidget(this.natureField);
        this.addRenderableWidget(this.clearBatchSpeciesBtn);
        this.addRenderableWidget(this.clearSingleSpeciesBtn);
        this.addRenderableWidget(this.clearGenderBtn);
        this.addRenderableWidget(this.clearAbilityBtn);
        this.addRenderableWidget(this.clearNatureBtn);
        this.addRenderableWidget(this.levelField);
        this.addRenderableWidget(this.clearLevelBtn);
        this.addRenderableWidget(this.hpField); this.addRenderableWidget(this.atkField); this.addRenderableWidget(this.defField);
        this.addRenderableWidget(this.spaField); this.addRenderableWidget(this.spdField); this.addRenderableWidget(this.speField);
        this.addRenderableWidget(this.clearIvBtn);
        this.addRenderableWidget(this.npcNameField);       // [线性-R06e]
        this.addRenderableWidget(this.clearNpcNameBtn);    // [线性-R06e]
    }

    private EditBox makeField(int x, int y, int w) {
        EditBox eb = new EditBox(this.font, x, y, w, 16, Component.literal(""));
        eb.setMaxLength(200);
        eb.setTextShadow(false);
        return eb;
    }

    private Button makeClearBtn(int x, int y, Runnable action) {
        return Button.builder(Component.literal("\u00d7"), (btn) -> action.run())
                .pos(x, y - 4).size(16, 16).build();
    }

    // ==== 模式按钮文字 ====

    private Component getModeButtonText(String mode) {
        boolean on = switch (mode) {
            case "legendary" -> state.isLegendaryMode();
            case "shiny" -> state.isShinyMode();
            case "pokemon" -> state.isPokemonMode();
            case "npc" -> state.isNpcMode();              // [线性-R06e]
            default -> false;
        };
        String key = switch (mode) {
            case "legendary" -> on ? "coblemon_ts.radar.legendary_on" : "coblemon_ts.radar.legendary_off";
            case "shiny" -> on ? "coblemon_ts.radar.shiny_on" : "coblemon_ts.radar.shiny_off";
            case "pokemon" -> on ? "coblemon_ts.radar.pokemon_on" : "coblemon_ts.radar.pokemon_off";
            case "npc" -> {  // [线性-R06g] 三态: OFF(红) / RTC(蓝) / NPC(紫)
                if (!state.isNpcMode()) yield "coblemon_ts.radar.npc_off";
                yield "rtc".equals(state.getNpcSubMode()) ? "coblemon_ts.radar.npc_rtc_on" : "coblemon_ts.radar.npc_on";
            }
            default -> "";
        };
        return Component.translatable(key);
    }

    private void updateModeButtonLabels() {
        if (this.legendaryBtn != null) this.legendaryBtn.setMessage(getModeButtonText("legendary"));
        if (this.shinyBtn != null) this.shinyBtn.setMessage(getModeButtonText("shiny"));
        if (this.pokemonBtn != null) this.pokemonBtn.setMessage(getModeButtonText("pokemon"));
        if (this.npcBtn != null) this.npcBtn.setMessage(getModeButtonText("npc"));  // [线性-R06e]
    }

    // [线性-R06g] NPC按钮三态循环: OFF → RTC → NPC → OFF
    private void cycleNpcMode() {
        if (!state.isNpcMode()) {
            state.setNpcMode(true);
            state.setNpcSubMode("rtc");
        } else if ("rtc".equals(state.getNpcSubMode())) {
            state.setNpcSubMode("npc");
        } else {
            state.setNpcMode(false);
        }
    }

    private void saveState() {
        state.setBatchSpeciesInput(this.batchSpeciesField.getValue());
        state.setSingleSpeciesInput(this.singleSpeciesField.getValue());
        state.setGenderInput(this.genderField.getValue());
        state.setAbilityInput(this.abilityField.getValue());
        state.setNatureInput(this.natureField.getValue());
        state.setLevelInput(this.levelField.getValue());
        state.setHpIvInput(this.hpField.getValue());
        state.setAtkIvInput(this.atkField.getValue());
        state.setDefIvInput(this.defField.getValue());
        state.setSpaIvInput(this.spaField.getValue());
        state.setSpdIvInput(this.spdField.getValue());
        state.setSpeIvInput(this.speField.getValue());
        state.setNpcNameInput(this.npcNameField != null ? this.npcNameField.getValue() : "");  // [线性-R06e]
    }

    // ==== 渲染 ====

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        saveState();
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);

        if (this.uiScaleFactor < 1.0f) {
            // [线性-R04-scalefix] 用面板实际中心做缩放锚点，窗口模式下保持居中
            float panelCenterX = panelX + panelW / 2f;
            float panelCenterY = panelY + panelH / 2f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(this.width / 2f, this.height / 2f, 0);
            guiGraphics.pose().scale(this.uiScaleFactor, this.uiScaleFactor, 1.0f);
            guiGraphics.pose().translate(-panelCenterX, -panelCenterY, 0);
        }

        // 面板背景
        int ct = panelY - 10, cb = panelY + panelH + 10;
        int cl = panelX - 8, cr = panelX + panelW + 8;
        guiGraphics.fill(cl - 3, ct - 3, cr + 3, ct, 0x33000000);
        guiGraphics.fill(cl - 3, cb, cr + 3, cb + 3, 0x33000000);
        guiGraphics.fill(cl - 3, ct, cl, cb, 0x33000000);
        guiGraphics.fill(cr, ct, cr + 3, cb, 0x33000000);
        guiGraphics.fill(cl, ct, cr, cb, editorLightBg);
        guiGraphics.fill(cl, ct, cr, ct + 1, editorLightBorder);
        guiGraphics.fill(cl, cb - 1, cr, cb, editorLightBorder);
        guiGraphics.fill(cl, ct, cl + 1, cb, editorLightBorder);
        guiGraphics.fill(cr - 1, ct, cr, cb, editorLightBorder);

        // 标题
        String title = Component.translatable("coblemon_ts.radar.title").getString();
        guiGraphics.drawCenteredString(this.font, title, panelX + panelW / 2, panelY - 5, editorLightLabel);

        // 标签
        String[] labels = {
            Component.translatable("coblemon_ts.radar.label.batch_species", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.single_species", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.gender", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.ability", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.nature", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.level", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.iv_atk_def", new Object[0]).getString(),
            Component.translatable("coblemon_ts.radar.label.iv_spa_spd_spe", new Object[0]).getString()
        };
        for (int i = 0; i < labels.length; i++) {
            guiGraphics.drawString(this.font, labels[i] + ":", panelX + 12, inputStartY + rowStep * i + 4, editorLightLabel, false);
        }
        // [线性-R06e] NPC名字标签（第8行,不在labels数组中）
        guiGraphics.drawString(this.font,
                Component.translatable("coblemon_ts.radar.label.npc_name").getString() + ":",
                panelX + 12, npcNameY + 4, editorLightLabel, false);

        // [线性-R04j] 个体值子标签：HP-ATK-DEF 和 SPE-SpA-SpD
        int ivBoxW = 70; int ivGap = 6;
        String[] ivLabels1 = {"HP", "ATK", "DEF"};
        String[] ivLabels2 = {"SPE", "SpA", "SpD"};
        for (int j = 0; j < 3; j++) {
            int bx = inputStartX + (ivBoxW + ivGap) * j;
            guiGraphics.drawString(this.font, ivLabels1[j], bx, ivRow1Y - 8, editorLightLabel, false);
            guiGraphics.drawString(this.font, ivLabels2[j], bx, ivRow2Y - 8, editorLightLabel, false);
        }

        // 提示文字
        String hintText = Component.translatable("coblemon_ts.radar.hint", new Object[0]).getString();
        guiGraphics.drawCenteredString(this.font, hintText, panelX + panelW / 2, panelY + panelH + 5, 0xFFAAAAAA);

        // [线性-R04i] 构建+渲染下拉栏
        buildAutocomplete(guiGraphics, mouseX, mouseY);
        renderAutocomplete(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.uiScaleFactor < 1.0f) {
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不画默认背景
    }

    // ==== 下拉栏构建（照搬编辑器逻辑） ====

    /** [线性-R04i1] 在render中检测焦点输入框，构建autocompleteItems列表 */
    private void buildAutocomplete(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int dropdownY = -1;
        int activeX = inputStartX;
        int activeW = inputW;
        String q = "";
        int newActiveField = -1;

        if (this.singleSpeciesField.isFocused()) {
            dropdownY = speciesY + 20;
            activeX = this.singleSpeciesField.getX();
            activeW = this.singleSpeciesField.getWidth();
            q = this.singleSpeciesField.getValue().trim().toLowerCase(Locale.ROOT);
            newActiveField = 0;
        } else if (this.abilityField.isFocused()) {
            dropdownY = abilityY + 20;
            activeX = this.abilityField.getX();
            activeW = this.abilityField.getWidth();
            q = this.abilityField.getValue().trim().toLowerCase(Locale.ROOT);
            newActiveField = 1;
        } else if (this.natureField.isFocused()) {
            dropdownY = natureY + 20;
            activeX = this.natureField.getX();
            activeW = this.natureField.getWidth();
            q = this.natureField.getValue().trim();
            newActiveField = 2;
        } else if (this.npcNameField.isFocused()) {     // [线性-R06e]
            dropdownY = npcNameY + 20;
            activeX = this.npcNameField.getX();
            activeW = this.npcNameField.getWidth();
            q = this.npcNameField.getValue().trim().toLowerCase(Locale.ROOT);
            newActiveField = 3;
        } else {
            this.autocompleteStartY = 0;
            this.autocompleteItems.clear();
            this.autocompleteScrollOffset = 0;
            this.autocompleteActiveField = -1;
            return;
        }

        this.autocompleteStartY = dropdownY;
        this.autocompleteX = activeX;
        this.autocompleteW = activeW;
        boolean searchChanged = newActiveField != this.autocompleteActiveField || !q.equals(this.lastSearchQuery);

        if (searchChanged) {
            this.autocompleteItems.clear();
            this.autocompleteScrollOffset = 0;

            if (newActiveField == 0) {
                // ==== 物种下拉（完全照搬编辑器field=0逻辑） ====
                List<String> species = PokemonNameMap.speciesDisplayList();
                if (q.isEmpty()) {
                    this.autocompleteItems.addAll(species);
                } else {
                    // 纯数字=图鉴编号搜索
                    if (q.matches("\\d+")) {
                        int num = Integer.parseInt(q);
                        String zh = PokemonNameMap.getSpeciesByNumber(num);
                        if (zh != null) this.autocompleteItems.add(zh);
                        try {
                            Map<Integer, String> numMap = CobblemonBridge.getNationalDexNumberMap();
                            for (Map.Entry<Integer, String> e : numMap.entrySet()) {
                                if (e.getKey() != num && String.valueOf(e.getKey()).startsWith(q)) {
                                    String z = PokemonNameMap.speciesToDisplay(e.getValue());
                                    if (z != null && !this.autocompleteItems.contains(z))
                                        this.autocompleteItems.add(z);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                    // 中文/英文/拼音匹配
                    for (String zh : species) {
                        String enName = PokemonNameMap.toEnglish(zh);
                        if ((zh.contains(q) || (enName != null && enName.contains(q)))
                                && !this.autocompleteItems.contains(zh)) {
                            this.autocompleteItems.add(zh);
                        }
                        if (!this.autocompleteItems.contains(zh) && CobblemonBridge.pinyinContains(zh, q)) {
                            this.autocompleteItems.add(zh);
                        }
                    }
                }
            } else if (newActiveField == 1) {
                // ==== 特性下拉：根据物种过滤 ====
                List<String> abilities = PokemonNameMap.abilityDisplayList();
                String speciesInput = this.singleSpeciesField.getValue().trim();
                if (!speciesInput.isEmpty()) {
                    String enName = PokemonNameMap.toEnglish(speciesInput);
                    if (enName != null && !enName.isEmpty()) {
                        List<String> speciesAbilities = PokemonRadarState.getAbilitiesForSpecies(enName);
                        if (!speciesAbilities.isEmpty()) {
                            List<String> filtered = new ArrayList<>();
                            for (String enAb : speciesAbilities) {
                                String display = PokemonNameMap.abilityToDisplay(enAb);
                                if (display != null && !display.isEmpty() && !filtered.contains(display))
                                    filtered.add(display);
                            }
                            if (!filtered.isEmpty()) abilities = filtered;
                        }
                    }
                }
                if (q.isEmpty()) {
                    this.autocompleteItems.addAll(abilities);
                } else {
                    for (String zh : abilities) {
                        if (zh.toLowerCase(Locale.ROOT).contains(q) && !this.autocompleteItems.contains(zh))
                            this.autocompleteItems.add(zh);
                    }
                }
            } else if (newActiveField == 2) {
                // ==== 性格下拉 ====
                List<String> natures = PokemonNameMap.natureDisplayList();
                if (q.isEmpty()) {
                    this.autocompleteItems.addAll(natures);
                } else {
                    for (String display : natures) {
                        if (display.contains(q) && !this.autocompleteItems.contains(display))
                            this.autocompleteItems.add(display);
                    }
                }
            } else if (newActiveField == 3) {
                // [线性-R06e] NPC名字下拉：反射获取所有已加载TrainerMob的显示名
                List<String> npcNames = getNpcDisplayNames();
                if (q.isEmpty()) {
                    this.autocompleteItems.addAll(npcNames);
                } else {
                    for (String name : npcNames) {
                        if (name.toLowerCase(Locale.ROOT).contains(q) && !this.autocompleteItems.contains(name))
                            this.autocompleteItems.add(name);
                    }
                }
            }
        }

        this.autocompleteActiveField = newActiveField;
        this.lastSearchQuery = q;
    }

    /** [线性-R04i2] 下拉框渲染（照搬编辑器） */
    private void renderAutocomplete(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.autocompleteItems.isEmpty() || this.autocompleteStartY <= 0) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        int lineH = 16;
        int maxShown = Math.min(this.autocompleteItems.size(), 8);
        int listH = maxShown * lineH;
        int sfX = this.autocompleteX;
        int sY = this.autocompleteStartY;
        int maxOffset = Math.max(0, this.autocompleteItems.size() - maxShown);
        if (this.autocompleteScrollOffset > maxOffset) this.autocompleteScrollOffset = maxOffset;
        if (this.autocompleteScrollOffset < 0) this.autocompleteScrollOffset = 0;

        guiGraphics.fill(sfX - 2, sY - 2, sfX + this.autocompleteW + 2, sY + listH + 2, -1728053248);
        guiGraphics.fill(sfX - 1, sY - 1, sfX + this.autocompleteW + 1, sY + listH + 1, -4667430);
        guiGraphics.fill(sfX, sY, sfX + this.autocompleteW, sY + listH, -197122);

        for (int i = 0; i < maxShown; i++) {
            int itemIdx = this.autocompleteScrollOffset + i;
            if (itemIdx < 0 || itemIdx >= this.autocompleteItems.size()) break;
            int itemY = sY + i * lineH;
            if (mouseX >= sfX && mouseX < sfX + this.autocompleteW && mouseY >= itemY && mouseY < itemY + lineH) {
                guiGraphics.fill(sfX, itemY, sfX + this.autocompleteW, itemY + lineH, -12877066);
                guiGraphics.drawString(this.font, this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -1, false);
            } else {
                guiGraphics.drawString(this.font, this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -14800581, false);
            }
        }

        // 滚动箭头
        if (this.autocompleteItems.size() > maxShown) {
            int arrowX = sfX + this.autocompleteW - 12;
            if (this.autocompleteScrollOffset > 0)
                guiGraphics.drawString(this.font, "\u2191", arrowX, sY - 12, -12877066, false);
            if (this.autocompleteScrollOffset < maxOffset)
                guiGraphics.drawString(this.font, "\u2193", arrowX, sY + listH + 2, -12877066, false);
        }
        guiGraphics.pose().popPose();
    }

    // ==== 鼠标事件 ====

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [线性-R04l] 下拉栏点击选择
        if (!this.autocompleteItems.isEmpty() && button == 0) {
            int maxShown = Math.min(this.autocompleteItems.size(), 8);
            int lineH = 16;
            int listH = maxShown * lineH;
            int imx = (int) mouseX;
            int imy = (int) mouseY;
            if (imx >= this.autocompleteX && imx < this.autocompleteX + this.autocompleteW
                    && imy >= this.autocompleteStartY && imy < this.autocompleteStartY + listH) {
                int idx = this.autocompleteScrollOffset + (imy - this.autocompleteStartY) / lineH;
                if (idx >= 0 && idx < this.autocompleteItems.size()) {
                    String selected = this.autocompleteItems.get(idx);
                    switch (this.autocompleteActiveField) {
                        case 0 -> { this.singleSpeciesField.setValue(selected); state.setSingleSpeciesInput(selected); }
                        case 1 -> { this.abilityField.setValue(selected); state.setAbilityInput(selected); }
                        case 2 -> { this.natureField.setValue(selected); state.setNatureInput(selected); }
                        case 3 -> { this.npcNameField.setValue(selected); state.setNpcNameInput(selected); }  // [线性-R06e]
                    }
                    this.autocompleteItems.clear();
                    this.autocompleteScrollOffset = 0;
                    this.autocompleteActiveField = -1;
                    return true;
                }
            }
            // 点击下拉框外 → 关闭
            if (this.autocompleteActiveField == 0) this.singleSpeciesField.setFocused(false);
            if (this.autocompleteActiveField == 1) this.abilityField.setFocused(false);
            if (this.autocompleteActiveField == 2) this.natureField.setFocused(false);
            if (this.autocompleteActiveField == 3) this.npcNameField.setFocused(false);  // [线性-R06e]
            this.autocompleteItems.clear();
            this.autocompleteScrollOffset = 0;
            this.autocompleteActiveField = -1;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.autocompleteItems.isEmpty() && this.autocompleteStartY > 0) {
            int maxShown = Math.min(this.autocompleteItems.size(), 8);
            int maxOffset = Math.max(0, this.autocompleteItems.size() - maxShown);
            double effectiveScroll = Math.abs(scrollY) > 0.01 ? scrollY : (scrollX != 0 ? scrollX : 0);
            if (effectiveScroll > 0) {
                this.autocompleteScrollOffset = Math.max(0, this.autocompleteScrollOffset - 2);
            } else if (effectiveScroll < 0) {
                this.autocompleteScrollOffset = Math.min(maxOffset, this.autocompleteScrollOffset + 2);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        saveState();
        state.saveToFile(); // [线性-R08c] 关闭时持久化到文件
        super.onClose();
    }

    // [线性-R06e] 反射获取所有已加载TrainerMob的显示名（无编译依赖zy模组）
    private List<String> getNpcDisplayNames() {
        List<String> names = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return names;
        try {
            Class<?> trainerMobClass = Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob");
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!trainerMobClass.isInstance(entity)) continue;
                String displayName = entity.getName().getString();
                if (!displayName.isEmpty() && !names.contains(displayName))
                    names.add(displayName);
            }
        } catch (ClassNotFoundException ignored) {}
        return names;
    }
}