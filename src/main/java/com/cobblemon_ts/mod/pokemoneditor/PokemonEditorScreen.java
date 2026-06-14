package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PokemonEditorScreen extends Screen {

    private final UUID targetPlayerUUID;
    private final String targetPlayerName;
    private final int partySize;
    private final List<PokemonData> partyData = new ArrayList<>();
    // ★ 保存每个槽位的原始值快照（用于检测用户修改了哪些字段）
    // 在 refreshEditFields 时保存；在 saveChanges 时比较当前值与快照生成 modifiedFields
    private final List<PokemonData> originalSnapshots = new ArrayList<>();

    private int selectedSlot = -1;
    private int activeTab = 0; // 0=概要, 1=对战, 2=能力, 3=高级
    private int pendingDeleteSlot = -1; // 标记待删除的槽位（点击"删除"时标记，保存时才真正删除）

    // ========== 概要 Tab 字段 ==========
    private EditBox sumSpeciesField, sumLevelField, sumNameField;
    private EditBox sumNatureField, sumAbilityField, sumFriendshipField, sumFormField;
    private EditBox sumHeldItemField, genderField;
    private Button sumShinyButton;
    private Button clearNameBtn; // 清空昵称按钮（×）
    private Button statusCycleBtn; // 状态切换：无状态 → 中毒 → 麻痹 → 烧伤 → 冰冻 → 睡眠 → 剧毒
    private Button levelDownBtn, levelUpBtn, friendshipDownBtn, friendshipUpBtn;

    // ========== 能力 Tab 字段 ==========
    private EditBox ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe;
    private EditBox evHp, evAtk, evDef, evSpA, evSpD, evSpe;

    // ========== 对战 Tab 字段 ==========
    private EditBox move1Field, move2Field, move3Field, move4Field;
    private EditBox move1PPField, move2PPField, move3PPField, move4PPField;
    private EditBox move1MaxPPField, move2MaxPPField, move3MaxPPField, move4MaxPPField;
    private EditBox currentHpField, maxHpField;

    // ========== 高级 Tab 字段 ==========
    private EditBox advExpField, advUUIDField;
    private EditBox advRawField;
    // 数据区在屏幕上的实际显示位置（advRawField 在屏幕外，这些是自定义绘制的真实坐标）
    private int dataAreaX = 0;
    private int dataAreaY = 0;
    private int dataAreaW = 340;
    private int dataAreaH = 200;

    // ========== 通用按钮 ==========
    private Button saveButton, deleteButton, closeButton;
    private Button tab0Btn, tab1Btn, tab2Btn, tab3Btn;
    private final Button[] slotButtons = new Button[6];

    // ========== 搜索补全下拉（物种、性格、特性共享渲染） ==========
    private java.util.List<String> autocompleteItems = new java.util.ArrayList<>();
    private int autocompleteStartY = 0;
    private int autocompleteX = 0;
    private int autocompleteW = 0;
    private int autocompleteActiveField = -1; // 0=物种 1=性格 2=特性 3/4/5/6=招式1/2/3/4 -1=无
    private int autocompleteScrollOffset = 0; // 下拉列表的滚动偏移
    private String lastSearchQuery = ""; // 上一次搜索关键词，避免每一帧重建列表

    // ========== 对战 Tab 按钮 ==========
    private Button maxPPButton;

    // ========== 高级 Tab 按钮（右侧纵向排列，只在高级Tab显示）==========
    private Button advImportBtn, advExportBtn, advCopyBtn, advPasteBtn, advSaveBtn, advResetBtn;
    // ========== 导入文件选择对话框 ==========
    private boolean showImportDialog = false;
    private java.io.File[] pendingImportFiles = null;
    private int importDialogScroll = 0;
    // ========== 全队查看模式（只在高级 Tab 生效）==========
    private Button teamAllBtn = null;
    private boolean viewingAllTeam = false;
    // ========== 数据区（advRawField）鼠标滚轮滚动偏移 ==========
    private int advRawScrollOffset = 0;
    // ========== 性别切换按钮 ==========
    private Button genderCycleBtn;
    // ========== 深色/亮色主题切换按钮 ==========
    private Button themeToggleBtn;
    // 主题由 PokemonEditorConfig.BRIGHT_THEME 控制，退出游戏再打开也保持
    // ========== 保存后自动关闭开关（小方块，红=关/不自动关闭，绿=开/自动关闭） ==========
    private Button editorAutoCloseBtn;
    // ========== IV/EV 快捷按钮 ==========
    private Button maxIvButton;
    private Button clearEvButton;
    private Button fullEvButton;  // 满努力按钮（仅 EV_EFFORT_CAP_252X6 开启时显示）
    private Button[] ivUpButtons = new Button[6];
    private Button[] ivDownButtons = new Button[6];
    private Button[] evUpButtons = new Button[6];
    private Button[] evDownButtons = new Button[6];
    // ★ 金色王冠：图标位置 + 开关按钮位置（分离设计，避免按钮自带渲染覆盖）
    private int crownIconX, crownIconY;
    private int crownToggleX, crownToggleY;
    private boolean crownVisible = true;
    private static final int CROWN_SIZE = 20;
    private static final int TOGGLE_SIZE = 20;
    // ★ 觉醒力量计算器按钮 + 显示文字
    private Button hiddenPowerBtn;
    private String hiddenPowerType = ""; // 当前觉醒力量属性ID

    public PokemonEditorScreen(UUID targetPlayerUUID, String targetPlayerName, int partySize) {
        super(Component.literal("宝可梦编辑器"));
        this.targetPlayerUUID = targetPlayerUUID;
        this.targetPlayerName = targetPlayerName;
        this.partySize = partySize;
        for (int i = 0; i < partySize; i++) {
            partyData.add(new PokemonData());
            originalSnapshots.add(new PokemonData());
        }
    }

    // ============================================================
    // 【客户端】接收服务端返回的宝可梦数据，更新本地 partyData 缓存
    // 流程：打开编辑器 → 服务端读取宝可梦 → 发送 PokemonDataPacket → 此方法更新 → 刷新 UI
    // ============================================================
    public void updatePokemonData(PokemonEditorNetwork.PokemonDataPacket pkt) {
        // 1. 边界检查：槽位索引必须在 0~partyData.size() 范围内
        if (pkt.slot() < 0 || pkt.slot() >= partyData.size()) return;
        PokemonData d = partyData.get(pkt.slot());

        // ★ 关键：从服务端读回的是原始数据，清除修改记录（这不是用户改的）
        d.clearModified();

        // ★ DEBUG: 打印收到的完整数据包（特别是 moves 部分）
        PokemonEditorDebug.infoMoves("[Screen] RECEIVED packet for slot {}: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})",
                pkt.slot(),
                pkt.move1(), pkt.move1PP(), pkt.move1MaxPP(),
                pkt.move2(), pkt.move2PP(), pkt.move2MaxPP(),
                pkt.move3(), pkt.move3PP(), pkt.move3MaxPP(),
                pkt.move4(), pkt.move4PP(), pkt.move4MaxPP());

        // 2. 基本属性：是否存在、UUID、物种、显示名、形态
        d.exists = pkt.exists();
        d.pokemonUUID = pkt.pokemonUUID();
        d.speciesName = cleanName(pkt.speciesName());
        d.displayName = pkt.displayName();
        d.formName = cleanName(pkt.formName());

        // 3. 等级/经验/亲密度/闪光
        d.level = pkt.level();
        d.experience = pkt.experience();
        d.friendship = pkt.friendship();
        d.shiny = pkt.shiny();
        d.goldenBottleCap = pkt.goldenBottleCap();

        // 4. 6 项个体值 (IV) 和 6 项努力值 (EV)
        d.ivHp = pkt.ivHp(); d.ivAtk = pkt.ivAtk(); d.ivDef = pkt.ivDef();
        d.ivSpA = pkt.ivSpA(); d.ivSpD = pkt.ivSpD(); d.ivSpe = pkt.ivSpe();
        d.evHp = pkt.evHp(); d.evAtk = pkt.evAtk(); d.evDef = pkt.evDef();
        d.evSpA = pkt.evSpA(); d.evSpD = pkt.evSpD(); d.evSpe = pkt.evSpe();

        // 5. 特性/性格
        d.nature = cleanName(pkt.nature());
        d.ability = cleanName(pkt.ability());

        // 6. 4 个技能槽：技能名 + 当前 PP + 最大 PP
        // ★ DEBUG: 分别打印原始值和 cleanName 后的结果
        String rawMove1 = pkt.move1();
        String cleanMove1 = cleanName(rawMove1);
        String rawMove2 = pkt.move2();
        String cleanMove2 = cleanName(rawMove2);
        String rawMove3 = pkt.move3();
        String cleanMove3 = cleanName(rawMove3);
        String rawMove4 = pkt.move4();
        String cleanMove4 = cleanName(rawMove4);
        PokemonEditorDebug.infoMoves("[Screen] move cleanName: slot0 raw='{}'->clean='{}' slot1 raw='{}'->clean='{}' slot2 raw='{}'->clean='{}' slot3 raw='{}'->clean='{}'",
                rawMove1, cleanMove1, rawMove2, cleanMove2, rawMove3, cleanMove3, rawMove4, cleanMove4);

        d.move1 = cleanMove1; d.move2 = cleanMove2;
        d.move3 = cleanMove3; d.move4 = cleanMove4;
        d.move1PP = pkt.move1PP(); d.move2PP = pkt.move2PP();
        d.move3PP = pkt.move3PP(); d.move4PP = pkt.move4PP();
        d.move1MaxPP = pkt.move1MaxPP(); d.move2MaxPP = pkt.move2MaxPP();
        d.move3MaxPP = pkt.move3MaxPP(); d.move4MaxPP = pkt.move4MaxPP();

        // ★ DEBUG: 检查是否有重复技能
        String[] storedMoves = {d.move1, d.move2, d.move3, d.move4};
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                if (storedMoves[i] != null && !storedMoves[i].isEmpty()
                        && storedMoves[j] != null && storedMoves[i].equalsIgnoreCase(storedMoves[j])) {
                    PokemonEditorDebug.warnMoves("[Screen] RECEIVED DUPLICATE: slot{} and slot{} both='{}'",
                            i, j, storedMoves[i]);
                }
            }
        }

        // 7. HP / 携带物 / 性别 / 状态
        d.currentHp = pkt.currentHp(); d.maxHp = pkt.maxHp();
        d.heldItem = cleanName(pkt.heldItem());
        d.gender = pkt.gender();
        d.status = cleanName(pkt.status());

        // 8. 可能特性列表（分号分隔，用于下拉选择）
        d.possibleAbilities.clear();
        if (pkt.possibleAbilities() != null && !pkt.possibleAbilities().isEmpty()) {
            for (String ab : pkt.possibleAbilities().split(";")) {
                if (ab != null && !ab.trim().isEmpty()) {
                    d.possibleAbilities.add(ab.trim());
                }
            }
        }

        // 9. 如果更新的是当前选中的槽位，刷新编辑区显示
        if (pkt.slot() == selectedSlot) refreshEditFields();
        // 10. 如果是前 6 槽（队伍槽），更新槽位按钮显示文字
        if (pkt.slot() < 6 && slotButtons[pkt.slot()] != null) updateSlotButtonLabel(pkt.slot());
    }

    private String cleanName(String s) {
        if (s == null || s.isEmpty()) return "";
        // 清理 Cobblemon 特有 toString 垃圾
        s = s.replace("square_brackets", "").replace("args=", "").replace(",,", ",");
        // 去掉括号及其内容
        int parenIdx = s.indexOf('(');
        if (parenIdx > 0) s = s.substring(0, parenIdx);
        int bracketIdx = s.indexOf('[');
        if (bracketIdx > 0) s = s.substring(0, bracketIdx);
        int atIdx = s.indexOf('@');
        if (atIdx > 0) s = s.substring(0, atIdx);
        int dollarIdx = s.indexOf('$');
        if (dollarIdx > 0) s = s.substring(0, dollarIdx);
        int colonIdx = s.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < s.length() - 1) s = s.substring(colonIdx + 1);
        int dotIdx = s.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < s.length() - 1) s = s.substring(dotIdx + 1);
        if (s.toLowerCase().contains("class ") || s.startsWith("com.") || s.startsWith("net.")
                || s.toLowerCase().contains("formdata") || s.toLowerCase().contains("speciesdata")
                || s.toLowerCase().contains("optional") || s.toLowerCase().contains("holder")) {
            return "";
        }
        s = s.trim();
        if (s.length() >= 2 && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))) {
            s = s.substring(1, s.length() - 1);
        }
        // 清理残留逗号
        s = s.replaceAll(",\\s*,", ",").trim();
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
        return s.trim();
    }

    @Override
    protected void init() {
        clearWidgets();

        int screenW = this.width;
        int screenH = this.height;

        // ========== 统一坐标系：紧凑尺寸 ==========
        // 整个编辑器的主面板（不包括 Tab 和底部按钮）
        final int panelX = screenW / 2 - 280;
        final int panelY = 60;
        final int panelW = 560;
        final int panelH = 340;

        // ========== Tab 按钮 (在主面板上方) ==========
        final int tabBarY = panelY - 30;
        final int tabBarH = 22;
        final int tabW = panelW / 4;
        final int tabStartX = panelX;

        tab0Btn = Button.builder(Component.literal("  概要  "),
                btn -> switchTab(0))
                .pos(tabStartX, tabBarY)
                .size(tabW, tabBarH)
                .build();
        tab1Btn = Button.builder(Component.literal("  对战  "),
                btn -> switchTab(1))
                .pos(tabStartX + tabW, tabBarY)
                .size(tabW, tabBarH)
                .build();
        tab2Btn = Button.builder(Component.literal("  能力  "),
                btn -> switchTab(2))
                .pos(tabStartX + tabW * 2, tabBarY)
                .size(tabW, tabBarH)
                .build();
        tab3Btn = Button.builder(Component.literal("  高级  "),
                btn -> switchTab(3))
                .pos(tabStartX + tabW * 3, tabBarY)
                .size(tabW, tabBarH)
                .build();
        this.addRenderableWidget(tab0Btn);
        this.addRenderableWidget(tab1Btn);
        this.addRenderableWidget(tab2Btn);
        this.addRenderableWidget(tab3Btn);

        // ========== 左侧队伍槽位区 (在主面板内，缩小尺寸) ==========
        final int partyX = panelX + 10;
        final int partyY = panelY + 10;
        final int partyW = 140;           // 原180 → 缩小至140
        final int slotH = 20;              // 原26 → 缩小至20
        final int slotGap = 2;              // 原3 → 缩小至2
        for (int i = 0; i < Math.min(6, partySize); i++) {
            final int slot = i;
            slotButtons[i] = Button.builder(
                    Component.literal("  队伍 " + (i + 1) + "  (空)  "),
                    btn -> selectSlot(slot))
                    .pos(partyX, partyY + i * (slotH + slotGap))
                    .size(partyW, slotH)
                    .build();
            this.addRenderableWidget(slotButtons[i]);
        }
        // 全队按钮（在第6只下面）
        final int lastSlotY = partyY + Math.min(6, partySize) * (slotH + slotGap);
        teamAllBtn = Button.builder(Component.literal("  全队  "),
                btn -> {
                    viewingAllTeam = true;
                    // 把6只宝可梦的数据合并到 advRawField
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < partyData.size(); i++) {
                        if (i > 0) sb.append("\n\n");
                        sb.append("# ====== 队").append(i + 1).append(" ======\n");
                        sb.append(buildDataString(partyData.get(i)));
                    }
                    advRawField.setValue(sb.toString());
                    advRawField.setCursorPosition(0);       // 重置光标到文本开头
                    advRawField.setHighlightPos(0);            // 清除选中区域
                    advRawScrollOffset = 0; // 切换到全队模式时重置数据区滚动位置
                })
                .pos(partyX, lastSlotY)
                .size(partyW, slotH)
                .build();
        teamAllBtn.visible = false; // 初始隐藏，只在高级 Tab 时由 switchTab 显示
        this.addRenderableWidget(teamAllBtn);

        // ========== 右侧编辑区 (在主面板内，因左侧缩小，可左移一些) ==========
        final int editX = panelX + 165;  // 原200 → 左移
        final int editY = panelY + 10;
        final int editW = panelW - 175;
        final int labelW = 50;

        // ---------- 概要 Tab ----------
        int rowY = editY + 18;
        int rowStep = 28;
        sumSpeciesField = new EditBox(this.font, editX + labelW + 10, rowY, 230, 18, Component.literal(""));
        sumSpeciesField.setMaxLength(40);
        sumLevelField = makeIntBox(editX + labelW + 10, rowY + rowStep, 100, 1, 900);
        // 等级上下箭头
        levelDownBtn = Button.builder(Component.literal("\u2B07"),
                btn -> {
                    boolean shift = Screen.hasShiftDown();
                    boolean ctrl = Screen.hasControlDown();
                    int cur = parseOrDefault(sumLevelField, 1);
                    int step = 1;
                    if (shift && ctrl) step = 900;
                    else if (ctrl) step = 100;
                    else if (shift) step = 10;
                    int n = Math.max(1, cur - step);
                    sumLevelField.setValue(String.valueOf(n));
                })
                .pos(editX + labelW + 10 + 100 + 4, rowY + rowStep)
                .size(18, 18)
                .build();
        levelUpBtn = Button.builder(Component.literal("\u2B06"),
                btn -> {
                    boolean shift = Screen.hasShiftDown();
                    boolean ctrl = Screen.hasControlDown();
                    int cur = parseOrDefault(sumLevelField, 1);
                    int step = 1;
                    if (shift && ctrl) step = 900;
                    else if (ctrl) step = 100;
                    else if (shift) step = 10;
                    int n = Math.min(900, cur + step);
                    sumLevelField.setValue(String.valueOf(n));
                })
                .pos(editX + labelW + 10 + 100 + 4 + 18 + 2, rowY + rowStep)
                .size(18, 18)
                .build();
        sumNameField = new EditBox(this.font, editX + labelW + 10, rowY + rowStep * 2, 160, 18, Component.literal(""));
        sumNameField.setMaxLength(30);
        // 昵称清空按钮（×）
        clearNameBtn = Button.builder(Component.literal("\u00D7"),
                btn -> sumNameField.setValue(""))
                .pos(editX + labelW + 10 + 160 + 4, rowY + rowStep * 2)
                .size(18, 18)
                .build();
        sumNatureField = new EditBox(this.font, editX + labelW + 10, rowY + rowStep * 3, 160, 18, Component.literal(""));
        sumNatureField.setMaxLength(20);
        sumAbilityField = new EditBox(this.font, editX + labelW + 10, rowY + rowStep * 4, 160, 18, Component.literal(""));
        sumAbilityField.setMaxLength(30);
        sumFriendshipField = makeIntBox(editX + labelW + 10, rowY + rowStep * 5, 100, 0, 255);
        // 亲密度上下箭头
        friendshipDownBtn = Button.builder(Component.literal("\u2B07"),
                btn -> {
                    boolean shift = Screen.hasShiftDown();
                    boolean ctrl = Screen.hasControlDown();
                    int cur = parseOrDefault(sumFriendshipField, 0);
                    int step = 1;
                    if (shift && ctrl) step = 255;
                    else if (ctrl) step = 100;
                    else if (shift) step = 10;
                    int n = Math.max(0, cur - step);
                    sumFriendshipField.setValue(String.valueOf(n));
                })
                .pos(editX + labelW + 10 + 100 + 4, rowY + rowStep * 5)
                .size(18, 18)
                .build();
        friendshipUpBtn = Button.builder(Component.literal("\u2B06"),
                btn -> {
                    boolean shift = Screen.hasShiftDown();
                    boolean ctrl = Screen.hasControlDown();
                    int cur = parseOrDefault(sumFriendshipField, 0);
                    int step = 1;
                    if (shift && ctrl) step = 255;
                    else if (ctrl) step = 100;
                    else if (shift) step = 10;
                    int n = Math.min(255, cur + step);
                    sumFriendshipField.setValue(String.valueOf(n));
                })
                .pos(editX + labelW + 10 + 100 + 4 + 18 + 2, rowY + rowStep * 5)
                .size(18, 18)
                .build();
        sumFormField = new EditBox(this.font, editX + labelW + 10, rowY + rowStep * 6, 160, 18, Component.literal(""));
        sumFormField.setMaxLength(40);
        sumHeldItemField = new EditBox(this.font, editX + labelW + 10, rowY + rowStep * 7, 160, 18, Component.literal(""));
        sumHeldItemField.setMaxLength(50);
        // 性别：0=雄性 1=雌性 2=无性别，用数字输入框+一个切换按钮
        genderField = makeIntBox(editX + labelW + 10, rowY + rowStep * 8, 60, 0, 2);
        genderCycleBtn = Button.builder(Component.literal("  \u21BB 切换  "),
                btn -> {
                    int cur = parseOrDefault(genderField, 2);
                    int next = (cur + 1) % 3;
                    genderField.setValue(String.valueOf(next));
                })
                .pos(editX + labelW + 10 + 65, rowY + rowStep * 8)
                .size(90, 18)
                .build();
        sumShinyButton = Button.builder(Component.literal("  闪光: 关  "),
                btn -> { if (selectedSlot >= 0) { partyData.get(selectedSlot).shiny = !partyData.get(selectedSlot).shiny; updateShinyButton(); }})
                .pos(editX + labelW + 10, rowY + rowStep * 9)
                .size(110, 18)
                .build();
        // 状态下拉（无/中毒/麻痹/烧伤/冰冻/睡眠/剧毒）- 点击循环切换
        statusCycleBtn = Button.builder(Component.literal("  状态: 无  "),
                btn -> { if (selectedSlot >= 0) cycleToNextStatus(); })
                .pos(editX + labelW + 10 + 115, rowY + rowStep * 9)
                .size(160, 18)
                .build();

        // ---------- 能力 Tab ----------
        int ivStartY = editY + 40;
        int statRowH = 28;
        int ivLabelW = 35;
        int valueW = 60;
        int arrowW = 18;
        int arrowH = 18;
        // IV 左列
        int ivColX = editX + 10;
        ivHp  = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 0, valueW, 0, 31);
        ivAtk = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 1, valueW, 0, 31);
        ivDef = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 2, valueW, 0, 31);
        ivSpA = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 3, valueW, 0, 31);
        ivSpD = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 4, valueW, 0, 31);
        ivSpe = makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 5, valueW, 0, 31);
        // ★ 觉醒力量实时计算：IV输入框值变化时自动刷新（事件驱动，非每帧轮询）
        ivHp.setResponder(s -> refreshHiddenPower());
        ivAtk.setResponder(s -> refreshHiddenPower());
        ivDef.setResponder(s -> refreshHiddenPower());
        ivSpA.setResponder(s -> refreshHiddenPower());
        ivSpD.setResponder(s -> refreshHiddenPower());
        ivSpe.setResponder(s -> refreshHiddenPower());
        // IV的上下箭头 + 一键满IV按钮（支持快捷键：Shift=±10，Ctrl=±100，Ctrl+Shift=到上限
        EditBox[] ivBoxes = {ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe};
        for (int i = 0; i < 6; i++) {
            final int fi = i;
            int arrowBaseX = ivColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            ivDownButtons[i] = Button.builder(Component.literal("\u2B07"),
                    btn -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = parseOrDefault(ivBoxes[fi], 0);
                int step = 1;
                if (shift && ctrl) step = 31;           // Ctrl+Shift：直接到 0
                else if (ctrl) step = 31;                // Ctrl：直接到 0（IV 最大 31）
                else if (shift) step = 10;                 // Shift：-10
                int n = Math.max(0, cur - step);
                ivBoxes[fi].setValue(String.valueOf(n));
            })
            .pos(arrowBaseX, arrowBaseY)
            .size(arrowW, arrowH)
            .build();
            ivUpButtons[i] = Button.builder(Component.literal("\u2B06"),
                    btn -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = parseOrDefault(ivBoxes[fi], 0);
                int step = 1;
                if (shift && ctrl) step = 31;              // Ctrl+Shift：直接到 31
                else if (ctrl) step = 31;                    // Ctrl：直接到 31（IV 上限 31）
                else if (shift) step = 10;                      // Shift：+10
                int n = Math.min(31, cur + step);
                ivBoxes[fi].setValue(String.valueOf(n));
            })
            .pos(arrowBaseX + arrowW + 2, arrowBaseY)
            .size(arrowW, arrowH)
            .build();
        }
        maxIvButton = Button.builder(Component.literal(" \u2605 满IV 31"),
                btn -> { for (EditBox b : ivBoxes) b.setValue("31"); })
                .pos(ivColX + 5, ivStartY + statRowH * 6 + 16)
                .size(140, 22)
                .build();

        // ★ 金色王冠：图标(静态材质) + 开关按钮(红色=关，绿色=开) 分离设计
        int ivTitleW = this.font.width("IV (0-31)");
        crownIconX = ivColX + 60 + ivTitleW / 2 + 5;
        crownIconY = editY + 11;
        crownToggleX = crownIconX + CROWN_SIZE + 4; // 紧挨图标右边
        crownToggleY = crownIconY;

    // ★ 觉醒力量计算器（与满IV按钮左边对齐，Y不变）
    int hpCalcY = ivStartY + statRowH * 6 + 16 + 28; // 满IV按钮下方
    hiddenPowerBtn = Button.builder(Component.literal("觉醒力量"),
            btn -> {
                if (selectedSlot < 0) return;
                // ★ 从编辑器输入框读取当前IV值，实时计算觉醒力量
                int hp  = parseOrDefault(ivHp, 0);
                int atk = parseOrDefault(ivAtk, 0);
                int def = parseOrDefault(ivDef, 0);
                int spa = parseOrDefault(ivSpA, 0);
                int spd = parseOrDefault(ivSpD, 0);
                int spe = parseOrDefault(ivSpe, 0);
                hiddenPowerType = calcHiddenPowerType(hp, atk, def, spa, spd, spe);
            })
            .pos(ivColX + 5, hpCalcY) // ★ 与满IV按钮左边对齐
            .size(80, 20)
            .build();
        // EV 右列
        int evColX = editX + 260;
        evHp  = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 0, valueW, 0, 252);
        evAtk = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 1, valueW, 0, 252);
        evDef = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 2, valueW, 0, 252);
        evSpA = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 3, valueW, 0, 252);
        evSpD = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 4, valueW, 0, 252);
        evSpe = makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 5, valueW, 0, 252);
        // EV的上下箭头（注意EV总和限制为510，增加按钮）
        EditBox[] evBoxes = {evHp, evAtk, evDef, evSpA, evSpD, evSpe};
        for (int i = 0; i < 6; i++) {
            final int fi = i;
            int arrowBaseX = evColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            // EV 向下按钮
            evDownButtons[i] = Button.builder(Component.literal("\u2B07"),
                    btn -> {
                        boolean shift = Screen.hasShiftDown();
                        boolean ctrl = Screen.hasControlDown();
                        int cur = parseOrDefault(evBoxes[fi], 0);
                        int step = 1;
                        if (shift && ctrl) step = 255;             // Ctrl+Shift：直接到 0
                        else if (ctrl) step = 100;                    // Ctrl：-100
                        else if (shift) step = 10;                     // Shift：-10
                        int n = Math.max(0, cur - step);
                        evBoxes[fi].setValue(String.valueOf(n));
                    })
                    .pos(arrowBaseX, arrowBaseY)
                    .size(arrowW, arrowH)
                    .build();
            // EV 向上按钮（考虑总和 510 上限）
            evUpButtons[i] = Button.builder(Component.literal("\u2B06"),
                    btn -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = parseOrDefault(evBoxes[fi], 0);
                int total = 0;
                for (int j = 0; j < 6; j++) total += parseOrDefault(evBoxes[j], 0);
                int otherSum = total - cur;
                int maxCanAdd = Math.min(252, CobblemonTSConfig.getEVTotalCap() - otherSum);
                int step = 1;
                if (shift && ctrl) step = 252;                        // Ctrl+Shift：直接到上限
                else if (ctrl) step = 100;                             // Ctrl：+100
                else if (shift) step = 10;                               // Shift：+10
                int n = Math.min(maxCanAdd, cur + step);
                if (n < 0) n = 0;
                evBoxes[fi].setValue(String.valueOf(n));
            })
            .pos(arrowBaseX + arrowW + 2, arrowBaseY)
            .size(arrowW, arrowH)
            .build();
        }
        // 清零努力按钮（往下移，让出空间给 EV 总和显示）
        clearEvButton = Button.builder(Component.literal(" \u274C 清零努力"),
                btn -> { for (EditBox b : evBoxes) b.setValue("0"); })
                .pos(evColX + 5, ivStartY + statRowH * 6 + 16)
                .size(140, 22)
                .build();
        // 满努力按钮（仅在 EV_EFFORT_CAP_252X6 开启时创建并显示，点击将 6 项 EV 全部设为 252）
        // 位置：清零努力按钮下方
        boolean ev252Enabled = Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get());
        if (ev252Enabled) {
            fullEvButton = Button.builder(Component.literal(" \u2728 满努力 252"),
                    btn -> { for (EditBox b : evBoxes) b.setValue("252"); })
                    .pos(evColX + 5, ivStartY + statRowH * 6 + 16 + 28)
                    .size(140, 22)
                    .build();
        }

        // ---------- 对战 Tab ----------
        int battleY = editY + 15;
        currentHpField = makeIntBox(editX + labelW + 10, battleY, 100, 0, 9999);
        maxHpField = makeIntBox(editX + labelW + 10, battleY + 30, 100, 1, 9999);
        int moveY = battleY + 70;
        int moveLabelW = 50; // "招式1"标签宽度
        int moveW = 180;
        int ppW = 60;
        // 招式输入框向右偏移，给标签留位置
        int moveFieldX = editX + moveLabelW + 5;
        move1Field = new EditBox(this.font, moveFieldX, moveY, moveW, 18, Component.literal(""));
        move1Field.setMaxLength(30);
        move2Field = new EditBox(this.font, moveFieldX, moveY + 25, moveW, 18, Component.literal(""));
        move2Field.setMaxLength(30);
        move3Field = new EditBox(this.font, moveFieldX, moveY + 50, moveW, 18, Component.literal(""));
        move3Field.setMaxLength(30);
        move4Field = new EditBox(this.font, moveFieldX, moveY + 75, moveW, 18, Component.literal(""));
        move4Field.setMaxLength(30);
        // PP字段位置
        int ppFieldX = moveFieldX + moveW + 10;
        move1PPField = makeIntBox(ppFieldX, moveY, ppW, 0, 999);
        move2PPField = makeIntBox(ppFieldX, moveY + 25, ppW, 0, 999);
        move3PPField = makeIntBox(ppFieldX, moveY + 50, ppW, 0, 999);
        move4PPField = makeIntBox(ppFieldX, moveY + 75, ppW, 0, 999);
        // 最大PP显示框（只读，跟PP框一样大小，放在PP框右边）
        int maxPPFieldX = ppFieldX + ppW + 10;
        move1MaxPPField = makeIntBox(maxPPFieldX, moveY, ppW, 0, 999);
        move2MaxPPField = makeIntBox(maxPPFieldX, moveY + 25, ppW, 0, 999);
        move3MaxPPField = makeIntBox(maxPPFieldX, moveY + 50, ppW, 0, 999);
        move4MaxPPField = makeIntBox(maxPPFieldX, moveY + 75, ppW, 0, 999);
        move1MaxPPField.setEditable(true);
        move2MaxPPField.setEditable(true);
        move3MaxPPField.setEditable(true);
        move4MaxPPField.setEditable(true);
        // 最大PP按钮：跟PP格子一样大小，放在最大PP显示框的下方
        maxPPButton = Button.builder(Component.literal(" 最大"),
                btn -> {
                    if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
                    PokemonData d = partyData.get(selectedSlot);
                    String[] names = {d.move1, d.move2, d.move3, d.move4};
                    int[] maxPPs = new int[4];
                    for (int i = 0; i < 4; i++) {
                        int basePP = CobblemonBridge.getBasePP(names[i]);
                        int raisedLimit = basePP > 0 ? (int) Math.floor(basePP * 1.6) : 0;
                        if (raisedLimit <= 0) {
                            int[] ms = {d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP};
                            raisedLimit = ms[i] > 0 ? ms[i] : 0;
                        }
                        maxPPs[i] = raisedLimit;
                    }
                    // ★ 只设置最大 PP，不改当前 PP
                    if (maxPPs[0] > 0) d.move1MaxPP = maxPPs[0];
                    if (maxPPs[1] > 0) d.move2MaxPP = maxPPs[1];
                    if (maxPPs[2] > 0) d.move3MaxPP = maxPPs[2];
                    if (maxPPs[3] > 0) d.move4MaxPP = maxPPs[3];
                    if (maxPPs[0] > 0) move1MaxPPField.setValue(String.valueOf(maxPPs[0]));
                    if (maxPPs[1] > 0) move2MaxPPField.setValue(String.valueOf(maxPPs[1]));
                    if (maxPPs[2] > 0) move3MaxPPField.setValue(String.valueOf(maxPPs[2]));
                    if (maxPPs[3] > 0) move4MaxPPField.setValue(String.valueOf(maxPPs[3]));
                })
                .pos(maxPPFieldX, moveY + 100)
                .size(ppW, 22)
                .build();

        // ---------- 高级 Tab ----------
        int advY = editY + 15;
        advExpField = makeIntBox(editX + labelW + 10, advY, 120, 0, Integer.MAX_VALUE);
        advUUIDField = new EditBox(this.font, editX + labelW + 10, advY + 35, 280, 18, Component.literal(""));
        advUUIDField.setMaxLength(60);
        advUUIDField.setEditable(true);
        // 记录数据区在屏幕上的实际显示位置
        dataAreaX = editX + 10;
        dataAreaY = advY + 80;
        dataAreaW = 340;
        dataAreaH = 200;

        // advRawField 作为数据区第一行的单行 EditBox（高度22，接收用户输入）
        // 我们的自定义多行渲染会在它下方从第二行开始显示完整数据
        advRawField = new EditBox(this.font, dataAreaX, dataAreaY, dataAreaW, 22, Component.literal(""));
        advRawField.setMaxLength(20000);
        advRawField.setEditable(true);
        advRawField.visible = true;
        advRawField.setBordered(true);
        advRawField.setTextColorUneditable(16777215);
        // 复制/粘贴按钮（在 advRawField 上方）→ 改为右侧纵向排列的6个功能按钮
        // 位置：advRawField 的右侧，纵向排列
        int rightBtnX = editX + 360;   // advRawField 右边
        int rightBtnY = advY + 80;     // 和 advRawField 顶部对齐
        int rightBtnW = 80;
        int rightBtnH = 22;
        int rightBtnGap = 8;

        advCopyBtn = Button.builder(Component.literal(" 复制 "),
                btn -> {
                    if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                        String data = buildDataString(partyData.get(selectedSlot));
                        minecraft.keyboardHandler.setClipboard(data);
                    }
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 0)
                .size(rightBtnW, rightBtnH)
                .build();
        advPasteBtn = Button.builder(Component.literal(" 粘贴 "),
                btn -> {
                    if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                        String text = minecraft.keyboardHandler.getClipboard();
                        applyDataString(partyData.get(selectedSlot), text);
                        refreshEditFields();
                        // 重新载入数据到 advRawField
                        if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                            advRawField.setValue(buildDataString(partyData.get(selectedSlot)));
                            advRawField.setCursorPosition(0);
                            advRawField.setHighlightPos(0);
                            advRawScrollOffset = 0;
                        }
                    }
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 1)
                .size(rightBtnW, rightBtnH)
                .build();
        advExportBtn = Button.builder(Component.literal(" 导出 "),
                btn -> {
                    // 导出：把数据区内容保存为 .txt 到 .minecraft/pokemon_exports/ 目录
                    String content = advRawField.getValue();
                    if (content == null || content.trim().isEmpty()) return;
                    try {
                        java.io.File folder = new java.io.File(minecraft.gameDirectory, "pokemon_exports");
                        if (!folder.exists()) folder.mkdirs();
                        String fileName = "pokemon_export_" + System.currentTimeMillis() + ".txt";
                        java.io.File file = new java.io.File(folder, fileName);
                        java.nio.file.Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
                        // 在聊天栏提示
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("已保存: " + file.getAbsolutePath()));
                        }
                    } catch (Exception e) {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("保存失败: " + e.getMessage()));
                        }
                    }
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 2)
                .size(rightBtnW, rightBtnH)
                .build();
        advImportBtn = Button.builder(Component.literal(" 导入 "),
                btn -> {
                    // 导入：列出 .minecraft/pokemon_exports/ 下所有 pokemon_export_*.txt，让玩家在简易对话框选
                    java.io.File folder = new java.io.File(minecraft.gameDirectory, "pokemon_exports");
                    if (!folder.exists()) folder.mkdirs();
                    java.io.File[] files = folder.listFiles((dir, name) ->
                            name.startsWith("pokemon_export_") && name.endsWith(".txt"));
                    if (files == null || files.length == 0) {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("pokemon_exports 文件夹里没有 pokemon_export_*.txt 文件"));
                        }
                        return;
                    }
                    // 按修改时间排序（最新的在前）
                    java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    // 弹出我们自己的简易文件选择界面
                    pendingImportFiles = files;
                    showImportDialog = true;
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 3)
                .size(rightBtnW, rightBtnH)
                .build();
        advSaveBtn = Button.builder(Component.literal(" 保存 "),
                btn -> {
                    // 保存 = 先用文本框内容更新 PokemonData，然后直接用 PokemonData 发数据包
                    // （不经过 saveChanges()，避免 GUI 字段覆盖高级 Tab 的编辑）
                    if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                        PokemonData d = partyData.get(selectedSlot);
                        applyDataString(d, advRawField.getValue());
                        savePokemonData(d);
                    }
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 4)
                .size(rightBtnW, rightBtnH)
                .build();
        advResetBtn = Button.builder(Component.literal(" 重置 "),
                btn -> {
                    // 重置 = 重新从宝可梦数据载入
                    if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                        advRawField.setValue(buildDataString(partyData.get(selectedSlot)));
                        advRawField.setCursorPosition(0);
                        advRawField.setHighlightPos(0);
                        advRawScrollOffset = 0;
                    }
                })
                .pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 5)
                .size(rightBtnW, rightBtnH)
                .build();

        // 注册所有（advRawField 现在也加入 children，
        // 因为它是标准 EditBox，需要接收鼠标/键盘事件）
        for (EditBox b : new EditBox[] {
                sumSpeciesField, sumLevelField, sumNameField, sumNatureField, sumAbilityField, sumFriendshipField,
                sumFormField, sumHeldItemField, genderField,
                ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe,
                evHp, evAtk, evDef, evSpA, evSpD, evSpe,
                currentHpField, maxHpField,
                move1Field, move2Field, move3Field, move4Field,
                move1PPField, move2PPField, move3PPField, move4PPField,
                move1MaxPPField, move2MaxPPField, move3MaxPPField, move4MaxPPField,
                advExpField, advUUIDField, advRawField}) {
            this.addRenderableWidget(b);
        }
        this.addRenderableWidget(sumShinyButton);
        this.addRenderableWidget(clearNameBtn);
        this.addRenderableWidget(statusCycleBtn);
        this.addRenderableWidget(genderCycleBtn);
        // 注册等级和亲密度箭头
        this.addRenderableWidget(levelDownBtn);
        this.addRenderableWidget(levelUpBtn);
        this.addRenderableWidget(friendshipDownBtn);
        this.addRenderableWidget(friendshipUpBtn);
        // 注册IV/EV快捷按钮
        this.addRenderableWidget(maxIvButton);
        this.addRenderableWidget(clearEvButton);
        if (fullEvButton != null) this.addRenderableWidget(fullEvButton);
        this.addRenderableWidget(hiddenPowerBtn);     // ★ 觉醒力量计算器按钮
        for (int i = 0; i < 6; i++) {
            if (ivUpButtons[i] != null) this.addRenderableWidget(ivUpButtons[i]);
            if (ivDownButtons[i] != null) this.addRenderableWidget(ivDownButtons[i]);
            if (evUpButtons[i] != null) this.addRenderableWidget(evUpButtons[i]);
            if (evDownButtons[i] != null) this.addRenderableWidget(evDownButtons[i]);
        }
        // 注册高级Tab按钮（只在高级Tab显示）
        this.addRenderableWidget(advCopyBtn);
        this.addRenderableWidget(advPasteBtn);
        this.addRenderableWidget(advExportBtn);
        this.addRenderableWidget(advImportBtn);
        this.addRenderableWidget(advSaveBtn);
        this.addRenderableWidget(advResetBtn);
        // 最大PP按钮（只在对战Tab显示）
        this.addRenderableWidget(maxPPButton);

        // ========== 底部按钮 (在主面板下方) ==========
        int btnY = panelY + panelH + 15;
        int btnW = 120;
        int btnH = 22;
        int btnGap = 20;
        int totalBtnW = btnW * 4 + btnGap * 3;
        int btnStartX = panelX + panelW / 2 - totalBtnW / 2;

        // 自动关闭开关（小正方形，红色=关闭（不自动关闭），绿色=开启（自动关闭），位于主题按钮上方）
        boolean autoClose = PokemonEditorConfig.EDITOR_AUTO_CLOSE.get();
        editorAutoCloseBtn = Button.builder(
                Component.literal(autoClose ? "\u25A0" : "\u25A0")
                        .withStyle(autoClose ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED),
                btn -> {
                    boolean newVal = !PokemonEditorConfig.EDITOR_AUTO_CLOSE.get();
                    PokemonEditorConfig.EDITOR_AUTO_CLOSE.set(newVal);
                    PokemonEditorConfig.EDITOR_AUTO_CLOSE.save();
                    btn.setMessage(Component.literal("\u25A0")
                            .withStyle(newVal ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
                })
                .pos(btnStartX + btnW - 26, btnY - 26)
                .size(22, 22)
                .build();

        // 主题切换按钮（第1个）
        themeToggleBtn = Button.builder(Component.literal(PokemonEditorConfig.BRIGHT_THEME.get() ? "  \uD83C\uDF19 深色  " : "  \u2600\uFE0F 亮色  "),
                btn -> {
                    boolean newVal = !PokemonEditorConfig.BRIGHT_THEME.get();
                    PokemonEditorConfig.BRIGHT_THEME.set(newVal);
                    PokemonEditorConfig.BRIGHT_THEME.save(); // 立即写入配置文件
                    btn.setMessage(Component.literal(newVal ? "  \uD83C\uDF19 深色  " : "  \u2600\uFE0F 亮色  "));
                })
                .pos(btnStartX, btnY)
                .size(btnW, btnH)
                .build();

        saveButton = Button.builder(Component.literal("  保存  "),
                btn -> saveChanges())
                .pos(btnStartX + btnW + btnGap, btnY)
                .size(btnW, btnH)
                .build();
        deleteButton = Button.builder(Component.literal("  删除  "),
                btn -> deletePokemon())
                .pos(btnStartX + btnW * 2 + btnGap * 2, btnY)
                .size(btnW, btnH)
                .build();
        closeButton = Button.builder(Component.literal("  关闭  "),
                btn -> this.onClose())
                .pos(btnStartX + btnW * 3 + btnGap * 3, btnY)
                .size(btnW, btnH)
                .build();
        this.addRenderableWidget(editorAutoCloseBtn);
        this.addRenderableWidget(themeToggleBtn);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(closeButton);

        switchTab(activeTab);
        if (partySize > 0) selectSlot(0);
    }

    private EditBox makeIntBox(int x, int y, int w, int min, int max) {
        EditBox box = new EditBox(this.font, x, y, w, 18, Component.literal(""));
        box.setMaxLength(String.valueOf(max).length());
        box.setFilter(s -> s.isEmpty() || s.matches("-?\\d+"));
        return box;
    }

    // ============================================================
    // Tab 页切换：根据 tab 参数显示/隐藏对应 Tab 的所有控件
    // Tab 索引：0=概要、1=对战、2=能力、3=高级
    // ============================================================
    private void switchTab(int tab) {
        activeTab = tab;

        // ★概要 Tab（物种、等级、名字、性格、特性、亲密度、形态、携带物、性别、闪光）
        boolean sumVisible = (tab == 0);
        for (EditBox b : new EditBox[] {sumSpeciesField, sumLevelField, sumNameField, sumNatureField, sumAbilityField,
                sumFriendshipField, sumFormField, sumHeldItemField, genderField}) {
            b.visible = sumVisible;
        }
        sumShinyButton.visible = sumVisible;
        statusCycleBtn.visible = sumVisible;
        genderCycleBtn.visible = sumVisible;

        // ★能力 Tab（6 项 IV + 6 项 EV + 快捷按钮）
        boolean statsVisible = (tab == 2);
        for (EditBox b : new EditBox[] {ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe,
                evHp, evAtk, evDef, evSpA, evSpD, evSpe}) {
            b.visible = statsVisible;
        }
        maxIvButton.visible = statsVisible;
        if (clearEvButton != null) clearEvButton.visible = statsVisible;
        if (fullEvButton != null) fullEvButton.visible = statsVisible;
        crownVisible = statsVisible;
        if (hiddenPowerBtn != null) hiddenPowerBtn.visible = statsVisible;
        // 每个属性的 +/- 按钮
        for (int i = 0; i < 6; i++) {
            if (ivUpButtons[i] != null) ivUpButtons[i].visible = statsVisible;
            if (ivDownButtons[i] != null) ivDownButtons[i].visible = statsVisible;
            if (evUpButtons[i] != null) evUpButtons[i].visible = statsVisible;
            if (evDownButtons[i] != null) evDownButtons[i].visible = statsVisible;
        }

        // ★对战 Tab（HP、4 个技能 + PP）
        boolean battleVisible = (tab == 1);
        for (EditBox b : new EditBox[] {currentHpField, maxHpField,
                move1Field, move2Field, move3Field, move4Field,
                move1PPField, move2PPField, move3PPField, move4PPField,
                move1MaxPPField, move2MaxPPField, move3MaxPPField, move4MaxPPField}) {
            b.visible = battleVisible;
        }
        if (maxPPButton != null) maxPPButton.visible = battleVisible;

        // ★高级 Tab（经验、UUID、原始数据编辑区 + 6 个操作按钮）
        boolean advVisible = (tab == 3);
        for (EditBox b : new EditBox[] {advExpField, advUUIDField, advRawField}) {
            b.visible = advVisible;
        }
        // 右侧 6 个按钮：复制/粘贴/导出/导入/保存/重置
        for (Button b : new Button[] {advCopyBtn, advPasteBtn, advExportBtn, advImportBtn, advSaveBtn, advResetBtn}) {
            b.visible = advVisible;
        }
        // "编辑整队" 按钮只在高级 Tab 显示
        if (teamAllBtn != null) {
            teamAllBtn.visible = advVisible;
        }
        // 切出高级 Tab 时重置编辑整队状态；切进高级 Tab 时重置数据区滚动位置
        if (!advVisible) {
            viewingAllTeam = false;
        } else {
            advRawScrollOffset = 0;
        }
    }

    // ============================================================
    // 更新槽位按钮显示文字：格式 "Lv99 皮卡丘 ✨"，当前选中的前面加 ">>"
    // ============================================================
    private void updateSlotButtonLabel(int slot) {
        if (slot >= slotButtons.length || slotButtons[slot] == null) return;
        PokemonData d = partyData.get(slot);
        String label;
        if (d.exists) {
            // 有宝可梦：显示名（中文映射优先，否则英文） + ✨ 闪光标记
            String display = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
            String shinyText = d.shiny ? " \u2728" : "";
            label = "  Lv" + d.level + " " + display + shinyText + "  ";
        } else {
            // 空槽位：显示 "队1(空)"
            label = "  队" + (slot + 1) + "(空)  ";
        }
        // 当前选中槽位：前面加 ">> " 标记
        if (slot == selectedSlot) {
            label = ">> " + label;
        } else {
            label = "   " + label;
        }
        slotButtons[slot].setMessage(Component.literal(label));
    }

    private void selectSlot(int slot) {
        this.selectedSlot = slot;
        this.viewingAllTeam = false; // 切回单只模式
        this.hiddenPowerType = "";    // ★ 清空觉醒力量计算结果
        // 切换槽位时清除待删除标记（避免误删）
        if (pendingDeleteSlot >= 0 && pendingDeleteSlot != slot) {
            PokemonEditorDebug.logEditor("[Screen] selectSlot {} clears pendingDeleteSlot {}", slot, pendingDeleteSlot);
            pendingDeleteSlot = -1;
        }
        for (int i = 0; i < Math.min(6, partySize); i++) {
            updateSlotButtonLabel(i);
        }
        refreshEditFields();
    }

    private void refreshEditFields() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
        PokemonData d = partyData.get(selectedSlot);

        // ★ 保存原始值快照（刷新 UI 字段前记录，用户修改后可比较）
        originalSnapshots.set(selectedSlot, d.snapshot());
        d.clearModified();

        // ★ DEBUG: 打印 PokemonData 中当前存储的 moves（渲染前）
        PokemonEditorDebug.infoMoves("[Screen] refreshEditFields START: slot={} exists={} moves=slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})",
                selectedSlot, d.exists,
                d.move1, d.move1PP, d.move1MaxPP,
                d.move2, d.move2PP, d.move2MaxPP,
                d.move3, d.move3PP, d.move3MaxPP,
                d.move4, d.move4PP, d.move4MaxPP);

        if (!d.exists) {
            // 空槽位：填充默认值
            sumSpeciesField.setValue("");
            sumLevelField.setValue("1");
            sumNameField.setValue("");
            List<String> natureList = PokemonNameMap.natureList();
            sumNatureField.setValue(natureList.isEmpty() ? "" : natureList.get(0));
            sumAbilityField.setValue("");
            sumFriendshipField.setValue("0");
            sumFormField.setValue("");
            sumHeldItemField.setValue("");
            genderField.setValue("0");

            // 能力默认最小值
            ivHp.setValue("0"); ivAtk.setValue("0"); ivDef.setValue("0");
            ivSpA.setValue("0"); ivSpD.setValue("0"); ivSpe.setValue("0");
            evHp.setValue("0"); evAtk.setValue("0"); evDef.setValue("0");
            evSpA.setValue("0"); evSpD.setValue("0"); evSpe.setValue("0");

            // 对战默认值
            currentHpField.setValue("0");
            maxHpField.setValue("1");
            move1Field.setValue(""); move2Field.setValue("");
            move3Field.setValue(""); move4Field.setValue("");
            move1PPField.setValue("0"); move2PPField.setValue("0");
            move3PPField.setValue("0"); move4PPField.setValue("0");
            move1MaxPPField.setValue("0"); move2MaxPPField.setValue("0");
            move3MaxPPField.setValue("0"); move4MaxPPField.setValue("0");

            // 高级默认值
            advExpField.setValue("0");
            advUUIDField.setValue("");
            d.shiny = false;
        } else {
            // 已有精灵：保持原值（物种字段显示中文；性格、特性、携带物也显示中文）
            sumSpeciesField.setValue(PokemonNameMap.toChinese(d.speciesName));
            sumLevelField.setValue(String.valueOf(d.level));
            sumNameField.setValue(d.displayName);
            sumNatureField.setValue(PokemonNameMap.natureToChinese(d.nature));
            sumAbilityField.setValue(PokemonNameMap.abilityToChinese(d.ability));
            sumFriendshipField.setValue(String.valueOf(d.friendship));
            sumFormField.setValue(d.formName);
            sumHeldItemField.setValue(PokemonNameMap.itemToChinese(d.heldItem));
            genderField.setValue(String.valueOf(d.gender));

            // 能力
            ivHp.setValue(String.valueOf(d.ivHp)); ivAtk.setValue(String.valueOf(d.ivAtk));
            ivDef.setValue(String.valueOf(d.ivDef)); ivSpA.setValue(String.valueOf(d.ivSpA));
            ivSpD.setValue(String.valueOf(d.ivSpD)); ivSpe.setValue(String.valueOf(d.ivSpe));
            evHp.setValue(String.valueOf(d.evHp)); evAtk.setValue(String.valueOf(d.evAtk));
            evDef.setValue(String.valueOf(d.evDef)); evSpA.setValue(String.valueOf(d.evSpA));
            evSpD.setValue(String.valueOf(d.evSpD)); evSpe.setValue(String.valueOf(d.evSpe));

            // 对战（招式名称显示中文，PP显示当前值）
            currentHpField.setValue(String.valueOf(d.currentHp));
            maxHpField.setValue(String.valueOf(d.maxHp));
            // ★ DEBUG: 先计算中文名，再设置到 field，方便追踪 moveToChinese 的结果
            String m1zh = PokemonNameMap.moveToChinese(d.move1);
            String m2zh = PokemonNameMap.moveToChinese(d.move2);
            String m3zh = PokemonNameMap.moveToChinese(d.move3);
            String m4zh = PokemonNameMap.moveToChinese(d.move4);
            PokemonEditorDebug.infoMoves("[Screen] moveToChinese results: slot0 en='{}'->zh='{}' slot1 en='{}'->zh='{}' slot2 en='{}'->zh='{}' slot3 en='{}'->zh='{}'",
                    d.move1, m1zh, d.move2, m2zh, d.move3, m3zh, d.move4, m4zh);
            move1Field.setValue(m1zh);
            move2Field.setValue(m2zh);
            move3Field.setValue(m3zh);
            move4Field.setValue(m4zh);
            move1PPField.setValue(String.valueOf(d.move1PP));
            move2PPField.setValue(String.valueOf(d.move2PP));
            move3PPField.setValue(String.valueOf(d.move3PP));
            move4PPField.setValue(String.valueOf(d.move4PP));
            move1MaxPPField.setValue(String.valueOf(d.move1MaxPP));
            move2MaxPPField.setValue(String.valueOf(d.move2MaxPP));
            move3MaxPPField.setValue(String.valueOf(d.move3MaxPP));
            move4MaxPPField.setValue(String.valueOf(d.move4MaxPP));
            // ★ DEBUG: 渲染后立即读取 field 的值，确认设置成功
            PokemonEditorDebug.infoMoves("[Screen] AFTER setValue: slot0 field='{}'({}/{}) slot1 field='{}'({}/{}) slot2 field='{}'({}/{}) slot3 field='{}'({}/{})",
                    move1Field.getValue(), move1PPField.getValue(), move1MaxPPField.getValue(),
                    move2Field.getValue(), move2PPField.getValue(), move2MaxPPField.getValue(),
                    move3Field.getValue(), move3PPField.getValue(), move3MaxPPField.getValue(),
                    move4Field.getValue(), move4PPField.getValue(), move4MaxPPField.getValue());

            // 高级
            advExpField.setValue(String.valueOf(d.experience));
            advUUIDField.setValue(d.pokemonUUID == null ? "" : d.pokemonUUID.toString());
        }

        advRawField.setValue(buildDataString(d));
        advRawField.setCursorPosition(0);       // 重置光标到文本开头（第一行第一个字符）
        advRawField.setHighlightPos(0);            // 清除选中区域
        advRawScrollOffset = 0; // 切换槽位时重置数据区滚动位置

        updateShinyButton();
        updateStatusButton();

        // 所有槽位都可编辑；空槽位的保存按钮需要物种字段不为空
        for (EditBox b : new EditBox[] {
                sumSpeciesField, sumLevelField, sumNameField, sumNatureField, sumAbilityField,
                sumFriendshipField, sumFormField, sumHeldItemField, genderField,
                ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe,
                evHp, evAtk, evDef, evSpA, evSpD, evSpe,
                currentHpField, maxHpField,
                move1Field, move2Field, move3Field, move4Field,
                move1PPField, move2PPField, move3PPField, move4PPField,
                move1MaxPPField, move2MaxPPField, move3MaxPPField, move4MaxPPField,
                advExpField, advUUIDField, advRawField}) {
            b.setEditable(true);
        }
        sumShinyButton.active = true;
        // 保存按钮：已有精灵总是可保存；空槽位需要物种字段非空
        updateSaveButtonState();
        deleteButton.active = d.exists;
        // ★ 切换槽位后刷新觉醒力量（IV输入框已全部加载完毕）
        refreshHiddenPower();
    }

    private void updateSaveButtonState() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) {
            saveButton.active = false;
            return;
        }
        PokemonData d = partyData.get(selectedSlot);
        // 已有精灵：可保存；空槽位：也可保存（即删除该槽位）
        saveButton.active = true;
    }

    private void updateShinyButton() {
        if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
            PokemonData d = partyData.get(selectedSlot);
            sumShinyButton.setMessage(Component.literal("  闪光: " + (d.shiny ? "开 \u2728" : "关") + "  "));
        }
    }

    // 状态 ID 归一化（frozen → freeze, poison_badly → bad_poison 等）
    private String normalizeStatusId(String raw) {
        if (raw == null) return "";
        String cur = raw.toLowerCase().replaceAll("[^a-z_]", "");
        if (cur.equals("frozen")) cur = "freeze";
        if (cur.equals("poison_badly") || cur.equals("badly") || cur.equals("tox")) cur = "bad_poison";
        if (cur.contains("freeze") || cur.contains("frozen")) cur = "freeze";
        if (cur.contains("badly") || cur.contains("poison_bad") || cur.contains("bad_poison")) cur = "bad_poison";
        return cur;
    }

    // 状态显示文本映射（Cobblemon status key → 中文显示）
    private static final String[][] STATUS_DISPLAY = {
            {"", "  状态: 无  "},
            {"poison", "  状态: 中毒  "},
            {"paralysis", "  状态: 麻痹  "},
            {"burn", "  状态: 烧伤  "},
            {"freeze", "  状态: 冰冻  "},
            {"sleep", "  状态: 睡眠  "},
            {"bad_poison", "  状态: 剧毒  "}
    };

    // 当前 status -> 下一个 status（循环）
    private void cycleToNextStatus() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
        PokemonData d = partyData.get(selectedSlot);
        String cur = normalizeStatusId(d.status);
        int idx = -1;
        for (int i = 0; i < STATUS_DISPLAY.length; i++) {
            if (STATUS_DISPLAY[i][0].equals(cur)) { idx = i; break; }
        }
        if (idx < 0) idx = 0;
        int next = (idx + 1) % STATUS_DISPLAY.length;
        d.status = STATUS_DISPLAY[next][0];
        updateStatusButton();
    }

    private void updateStatusButton() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
        PokemonData d = partyData.get(selectedSlot);
        String cur = normalizeStatusId(d.status);
        String display = "  状态: 无  ";
        for (String[] row : STATUS_DISPLAY) {
            if (row[0].equals(cur)) { display = row[1]; break; }
        }
        statusCycleBtn.setMessage(Component.literal(display));
    }

    private int parseOrDefault(EditBox box, int def) {
        try { return Integer.parseInt(box.getValue()); } catch (Exception e) { return def; }
    }

    private void saveChanges() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;

        // 检查是否有待删除的槽位（点击"删除"标记，保存时才真正执行）
        if (pendingDeleteSlot >= 0 && pendingDeleteSlot == selectedSlot) {
            PokemonEditorDebug.logEditor("[Screen] Save triggers pending delete for slot {}", selectedSlot);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new PokemonEditorNetwork.DeletePokemonPacket(selectedSlot, targetPlayerUUID));
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("[宝可梦编辑器] 已删除队伍 " + (selectedSlot + 1)));
            }
            pendingDeleteSlot = -1; // 清除标记
            // 关闭编辑器（根据 EDITOR_AUTO_CLOSE 配置决定）
            if (PokemonEditorConfig.EDITOR_AUTO_CLOSE.get()) {
                onClose();
            }
            return;
        }

        PokemonData d = partyData.get(selectedSlot);

        // 物种字段：中文 → 英文（Cobblemon需要英文名）
        String beforeSpecies = d.speciesName;
        d.speciesName = PokemonNameMap.toEnglish(sumSpeciesField.getValue());
        PokemonEditorDebug.logEditor("[Screen] species: UI='{}' → EN='{}' (was '{}')",
                sumSpeciesField.getValue(), d.speciesName, beforeSpecies);

        // 空物种名 = 空覆盖（删除该槽位）
        // 无论 d.exists 是否为 true，只要 speciesName 空了，就表示清空
        // 统一发 UpdatePacket，服务端识别空 speciesName → 调 deletePokemonFromSlot
        boolean isDelete = (d.speciesName == null || d.speciesName.isEmpty());
        d.level = Math.max(1, Math.min(900, parseOrDefault(sumLevelField, d.level)));
        d.displayName = sumNameField.getValue();
        PokemonEditorDebug.logEditor("[Screen] displayName: UI='{}' → EN='{}'",
                sumNameField.getValue(), d.displayName);
        d.friendship = Math.max(0, Math.min(255, parseOrDefault(sumFriendshipField, d.friendship)));
        String beforeNature = d.nature;
        d.nature = PokemonNameMap.natureToEnglish(sumNatureField.getValue());
        PokemonEditorDebug.logEditor("[Screen] nature: UI='{}' → EN='{}' (was '{}', changed={})",
                sumNatureField.getValue(), d.nature, beforeNature,
                !java.util.Objects.equals(d.nature, beforeNature));
        String beforeAbility = d.ability;
        d.ability = PokemonNameMap.abilityToEnglish(sumAbilityField.getValue());
        PokemonEditorDebug.logEditor("[Screen] ability: UI='{}' → EN='{}' (was '{}', changed={})",
                sumAbilityField.getValue(), d.ability, beforeAbility,
                !java.util.Objects.equals(d.ability, beforeAbility));
        d.formName = sumFormField.getValue();
        String beforeHeldItem = d.heldItem;
        d.heldItem = PokemonNameMap.itemToEnglish(sumHeldItemField.getValue());
        PokemonEditorDebug.logEditor("[Screen] heldItem: UI='{}' → EN='{}' (was '{}', changed={})",
                sumHeldItemField.getValue(), d.heldItem, beforeHeldItem,
                !java.util.Objects.equals(d.heldItem, beforeHeldItem));
        d.experience = Math.max(0, parseOrDefault(advExpField, d.experience));

        d.ivHp = Math.max(0, Math.min(31, parseOrDefault(ivHp, d.ivHp)));
        d.ivAtk = Math.max(0, Math.min(31, parseOrDefault(ivAtk, d.ivAtk)));
        d.ivDef = Math.max(0, Math.min(31, parseOrDefault(ivDef, d.ivDef)));
        d.ivSpA = Math.max(0, Math.min(31, parseOrDefault(ivSpA, d.ivSpA)));
        d.ivSpD = Math.max(0, Math.min(31, parseOrDefault(ivSpD, d.ivSpD)));
        d.ivSpe = Math.max(0, Math.min(31, parseOrDefault(ivSpe, d.ivSpe)));

        d.evHp = Math.max(0, Math.min(252, parseOrDefault(evHp, d.evHp)));
        d.evAtk = Math.max(0, Math.min(252, parseOrDefault(evAtk, d.evAtk)));
        d.evDef = Math.max(0, Math.min(252, parseOrDefault(evDef, d.evDef)));
        d.evSpA = Math.max(0, Math.min(252, parseOrDefault(evSpA, d.evSpA)));
        d.evSpD = Math.max(0, Math.min(252, parseOrDefault(evSpD, d.evSpD)));
        d.evSpe = Math.max(0, Math.min(252, parseOrDefault(evSpe, d.evSpe)));

        d.currentHp = Math.max(0, parseOrDefault(currentHpField, d.currentHp));
        d.maxHp = Math.max(1, parseOrDefault(maxHpField, d.maxHp));

        // 性别：从 genderField 读（数字0-2）
        d.gender = Math.max(0, Math.min(2, parseOrDefault(genderField, d.gender)));

        // ★ 技能相关：读取用户输入 + 智能默认 PP
        // PP1 = 当前剩余 PP（moveXPPField）
        // PP2 = 当前上限（moveXMaxPPField，现在可编辑）
        // 保留原来的 moveX（用于判断是否改变了技能名）
        String oldMove1 = d.move1;
        String oldMove2 = d.move2;
        String oldMove3 = d.move3;
        String oldMove4 = d.move4;
        d.move1 = PokemonNameMap.moveToEnglish(move1Field.getValue());
        d.move2 = PokemonNameMap.moveToEnglish(move2Field.getValue());
        d.move3 = PokemonNameMap.moveToEnglish(move3Field.getValue());
        d.move4 = PokemonNameMap.moveToEnglish(move4Field.getValue());

        // 读取用户输入：PP1（当前剩余）和 PP2（当前上限）
        int[] rawPP1s = new int[4];
        int[] rawPP2s = new int[4];
        try {
            rawPP1s[0] = parseOrDefault(move1PPField, d.move1PP);
            rawPP1s[1] = parseOrDefault(move2PPField, d.move2PP);
            rawPP1s[2] = parseOrDefault(move3PPField, d.move3PP);
            rawPP1s[3] = parseOrDefault(move4PPField, d.move4PP);
            rawPP2s[0] = parseOrDefault(move1MaxPPField, d.move1MaxPP);
            rawPP2s[1] = parseOrDefault(move2MaxPPField, d.move2MaxPP);
            rawPP2s[2] = parseOrDefault(move3MaxPPField, d.move3MaxPP);
            rawPP2s[3] = parseOrDefault(move4MaxPPField, d.move4MaxPP);
        } catch (Exception ignored) {}

        // ★ 核心规则：
        // 1) 技能改变时：PP1 = PP2 = basePP（新技能默认无提升，满血）
        // 2) 技能未改变时：优先用用户输入；PP1为空/0时 PP1=PP2；PP2为空/0时 PP2=basePP
        // 3) PP1 不能超过 PP2
        // move1
        int basePP1 = Math.max(0, CobblemonBridge.getBasePP(d.move1));
        boolean move1Changed = (d.move1 != null && !d.move1.isEmpty() && (oldMove1 == null || !oldMove1.equalsIgnoreCase(d.move1)));
        int userPP1_1 = Math.max(0, rawPP1s[0]);
        int userPP2_1 = Math.max(0, rawPP2s[0]);
        if (move1Changed || basePP1 <= 0) {
            // 技能改变或找不到 basePP：默认无提升，PP1 = PP2 = basePP
            d.move1MaxPP = basePP1;
            d.move1PP = basePP1;
        } else {
            // 技能未改变：用用户输入
            int pp2 = (userPP2_1 > 0) ? userPP2_1 : basePP1;
            int pp1 = (userPP1_1 > 0) ? userPP1_1 : pp2;
            if (pp1 > pp2) pp1 = pp2;
            d.move1MaxPP = pp2;
            d.move1PP = pp1;
        }
        // move2
        int basePP2 = Math.max(0, CobblemonBridge.getBasePP(d.move2));
        boolean move2Changed = (d.move2 != null && !d.move2.isEmpty() && (oldMove2 == null || !oldMove2.equalsIgnoreCase(d.move2)));
        int userPP1_2 = Math.max(0, rawPP1s[1]);
        int userPP2_2 = Math.max(0, rawPP2s[1]);
        if (move2Changed || basePP2 <= 0) {
            d.move2MaxPP = basePP2;
            d.move2PP = basePP2;
        } else {
            int pp2 = (userPP2_2 > 0) ? userPP2_2 : basePP2;
            int pp1 = (userPP1_2 > 0) ? userPP1_2 : pp2;
            if (pp1 > pp2) pp1 = pp2;
            d.move2MaxPP = pp2;
            d.move2PP = pp1;
        }
        // move3
        int basePP3 = Math.max(0, CobblemonBridge.getBasePP(d.move3));
        boolean move3Changed = (d.move3 != null && !d.move3.isEmpty() && (oldMove3 == null || !oldMove3.equalsIgnoreCase(d.move3)));
        int userPP1_3 = Math.max(0, rawPP1s[2]);
        int userPP2_3 = Math.max(0, rawPP2s[2]);
        if (move3Changed || basePP3 <= 0) {
            d.move3MaxPP = basePP3;
            d.move3PP = basePP3;
        } else {
            int pp2 = (userPP2_3 > 0) ? userPP2_3 : basePP3;
            int pp1 = (userPP1_3 > 0) ? userPP1_3 : pp2;
            if (pp1 > pp2) pp1 = pp2;
            d.move3MaxPP = pp2;
            d.move3PP = pp1;
        }
        // move4
        int basePP4 = Math.max(0, CobblemonBridge.getBasePP(d.move4));
        boolean move4Changed = (d.move4 != null && !d.move4.isEmpty() && (oldMove4 == null || !oldMove4.equalsIgnoreCase(d.move4)));
        int userPP1_4 = Math.max(0, rawPP1s[3]);
        int userPP2_4 = Math.max(0, rawPP2s[3]);
        if (move4Changed || basePP4 <= 0) {
            d.move4MaxPP = basePP4;
            d.move4PP = basePP4;
        } else {
            int pp2 = (userPP2_4 > 0) ? userPP2_4 : basePP4;
            int pp1 = (userPP1_4 > 0) ? userPP1_4 : pp2;
            if (pp1 > pp2) pp1 = pp2;
            d.move4MaxPP = pp2;
            d.move4PP = pp1;
        }

        // ★ 去重：只保留第一个出现的技能名（slot0 优先 > slot1 > slot2 > slot3）
        //    如果后面的槽位有与前面相同的技能名，则清空该槽位
        java.util.LinkedHashSet<String> seenMoves = new java.util.LinkedHashSet<>();
        String[] allMoveNames = {d.move1, d.move2, d.move3, d.move4};
        int[] allMaxPPs = {d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP};
        int[] allPPs = {d.move1PP, d.move2PP, d.move3PP, d.move4PP};

        for (int i = 0; i < 4; i++) {
            String nm = allMoveNames[i];
            if (nm != null && !nm.isEmpty()) {
            String lower = nm.toLowerCase();
            if (seenMoves.contains(lower)) {
                // 重复了，清空此槽位
                if (i == 0) { d.move1 = ""; d.move1MaxPP = 0; d.move1PP = 0; }
                else if (i == 1) { d.move2 = ""; d.move2MaxPP = 0; d.move2PP = 0; }
                else if (i == 2) { d.move3 = ""; d.move3MaxPP = 0; d.move3PP = 0; }
                else { d.move4 = ""; d.move4MaxPP = 0; d.move4PP = 0; }
                PokemonEditorDebug.infoMoves("[Screen] DEDUP: slot{} duplicate '{}' → clear",
                        i, nm);
            } else {
                seenMoves.add(lower);
            }
            }
        }

        // ★ DEBUG: saveChanges 完成后，打印 PokemonData 中的 moves（即将发送）
        PokemonEditorDebug.infoMoves("[Screen] saveChanges FINAL moves: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})",
                d.move1, d.move1PP, d.move1MaxPP,
                d.move2, d.move2PP, d.move2MaxPP,
                d.move3, d.move3PP, d.move3MaxPP,
                d.move4, d.move4PP, d.move4MaxPP);
        // 检查是否有重复技能
        String[] movesToSend = {d.move1, d.move2, d.move3, d.move4};
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                if (movesToSend[i] != null && !movesToSend[i].isEmpty()
                        && movesToSend[j] != null && movesToSend[i].equalsIgnoreCase(movesToSend[j])) {
                    PokemonEditorDebug.warnMoves("[Screen] saveChanges DUPLICATE: slot{} and slot{} both='{}'",
                            i, j, movesToSend[i]);
                }
            }
        }

        // 发数据包（公共方法
        sendUpdatePacket(d, advUUIDField.getValue().trim());
    }

    // 直接用 PokemonData 发 UpdatePacket（不读取 GUI 字段，适合高级 Tab 保存）
    private void savePokemonData(PokemonData d) {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
        if (d == null) return;

        // 空物种名 = 删除该槽位（标记待删除，下次保存时执行）
        // 高级Tab的"保存"按钮直接执行删除
        boolean isDelete = (d.speciesName == null || d.speciesName.isEmpty());
        if (isDelete) {
            PokemonEditorDebug.logEditor("[Screen] savePokemonData: delete for slot {}", selectedSlot);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new PokemonEditorNetwork.DeletePokemonPacket(selectedSlot, targetPlayerUUID));
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("[宝可梦编辑器] 已删除队伍 " + (selectedSlot + 1)));
            }
            pendingDeleteSlot = -1;
            // 清除本地数据
            PokemonData emptyData = new PokemonData();
            emptyData.exists = false;
            partyData.set(selectedSlot, emptyData);
            refreshEditFields();
            updateSlotButtonLabel(selectedSlot);
            return;
        }

        sendUpdatePacket(d, (d.pokemonUUID != null) ? d.pokemonUUID.toString() : "");
    }

    // 公共：发送 UpdatePokemonPacket + 更新 UI
    private void sendUpdatePacket(PokemonData d, String uuidText) {
        // 数据区内容（key=value 格式文本，发给服务端留作参考
        String rawDataText = (advRawField != null) ? advRawField.getValue() : "";
        if (rawDataText == null) rawDataText = "";

        // ★ 关键：比较当前 PokemonData 与原始快照，确定用户修改了哪些字段
        // 这将告诉服务端只修改这些字段，而不是全部
        PokemonData base = originalSnapshots.get(selectedSlot);
        java.util.Set<String> modifiedFields = d.diffFrom(base);
        // ★ 关键：打印 nature/ability/heldItem 的详细对比（调试这些字段不生效的核心问题）
        if (base != null) {
            PokemonEditorDebug.logEditor("[Screen] FIELD-CHECK slot {}: nature(current='{}' vs base='{}', equal={}, inModified={}) ability(current='{}' vs base='{}', equal={}, inModified={}) heldItem(current='{}' vs base='{}', equal={}, inModified={})",
                    selectedSlot,
                    d.nature, base.nature, java.util.Objects.equals(d.nature, base.nature), modifiedFields.contains("nature"),
                    d.ability, base.ability, java.util.Objects.equals(d.ability, base.ability), modifiedFields.contains("ability"),
                    d.heldItem, base.heldItem, java.util.Objects.equals(d.heldItem, base.heldItem), modifiedFields.contains("heldItem"));
        }
        // 特殊：空槽位时，所有字段都是修改过的
        if (base == null || !base.exists) {
            modifiedFields.add("speciesName");
            modifiedFields.add("level");
        }

        // 将 modifiedFields 编码到 rawDataText 开头，格式：
        //   __MODIFIED__=speciesName,level,ivHp,status\n原始数据
        // 服务端会解析这一行，提取修改字段列表
        String modifiedHeader = "__MODIFIED__=" + String.join(",", modifiedFields) + "\n";
        String combinedRawData = modifiedHeader + rawDataText;

        PokemonEditorDebug.logEditor("[Screen] SENDING packet for slot {}: species={}, level={}, shiny={}, friendship={}, ivHp={}, evHp={}, move1={}, modifiedFields=[{}], hpModified={}, statusModified={}",
                selectedSlot, d.speciesName, d.level, d.shiny, d.friendship,
                d.ivHp, d.evHp, d.move1,
                String.join(",", modifiedFields),
                modifiedFields.contains("currentHp") || modifiedFields.contains("maxHp"),
                modifiedFields.contains("status"));
        // ★详细的 move 数据 debug
        PokemonEditorDebug.infoMoves("[Screen] SENDING move data: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})",
                d.move1, d.move1PP, d.move1MaxPP,
                d.move2, d.move2PP, d.move2MaxPP,
                d.move3, d.move3PP, d.move3MaxPP,
                d.move4, d.move4PP, d.move4MaxPP);
        // 检查是否有重复技能
        String[] moves = {d.move1, d.move2, d.move3, d.move4};
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                if (moves[i] != null && !moves[i].isEmpty()
                        && moves[j] != null && moves[i].equalsIgnoreCase(moves[j])) {
                    PokemonEditorDebug.warnMoves("[Screen] SENDING DUPLICATE: slot{} and slot{} both='{}'",
                            i, j, moves[i]);
                }
            }
        }

        // 发送 UpdatePokemonPacket（使用 combinedRawData 包含 modifiedFields）
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new PokemonEditorNetwork.UpdatePokemonPacket(
                        selectedSlot, targetPlayerUUID,
                        d.speciesName, d.formName,
                        d.level, d.experience, d.displayName, d.shiny, d.friendship,
                        d.goldenBottleCap,
                        d.ivHp, d.ivAtk, d.ivDef, d.ivSpA, d.ivSpD, d.ivSpe,
                        d.evHp, d.evAtk, d.evDef, d.evSpA, d.evSpD, d.evSpe,
                        d.nature, d.ability,
                        d.move1, d.move2, d.move3, d.move4,
                        d.move1PP, d.move2PP, d.move3PP, d.move4PP,
                        d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP,
                        d.currentHp, d.maxHp, d.heldItem,
                        d.gender,
                        uuidText,           // pokemonUUID
                        combinedRawData,    // rawJsonData（包含 modifiedFields 头）
                        d.status));          // status

        // 更新原始快照为当前数据（保存后视为新的原始状态）
        originalSnapshots.set(selectedSlot, d.snapshot());

        updateSlotButtonLabel(selectedSlot);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("[宝可梦编辑器] 已保存队伍 " + (selectedSlot + 1)));
        }
        PokemonEditorDebug.logEditor("[Screen] Packet sent for slot {}", selectedSlot);
        
        // 关闭编辑器（根据 EDITOR_AUTO_CLOSE 配置决定是否自动关闭）
        if (PokemonEditorConfig.EDITOR_AUTO_CLOSE.get()) {
            onClose();
        } else {
            PokemonEditorDebug.logEditor("[Screen] auto-close disabled, keeping editor open");
        }
    }

    private void deletePokemon() {
        if (selectedSlot < 0 || selectedSlot >= partyData.size()) return;
        PokemonData d = partyData.get(selectedSlot);
        if (!d.exists) return;

        // 标记待删除（不立即发包，保存时才真正删除）
        pendingDeleteSlot = selectedSlot;
        PokemonEditorDebug.logEditor("[Screen] Delete marker set for slot {} (pending, will delete on save)", selectedSlot);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("[宝可梦编辑器] 已标记队伍 " + (selectedSlot + 1) + " 待删除（点击保存生效）"));
        }

        // 客户端立即清空本地数据并刷新 UI，显示空槽位
        PokemonData emptyData = new PokemonData();
        emptyData.exists = false;
        partyData.set(selectedSlot, emptyData);

        // 刷新编辑区（空槽位）
        refreshEditFields();
        // 刷新槽位按钮显示
        updateSlotButtonLabel(selectedSlot);
    }

    // ========== 数据序列化/反序列化：支持高级 Tab 复制/粘贴 ==========
    // 格式: 第一行是元数据行（# 开头，压缩显示关键信息，蓝色显示）
    // 空一行后是 key=value 数据结构
    // 解析时：中文字段名和英文 key 都能识别
    private String buildDataString(PokemonData d) {
        StringBuilder sb = new StringBuilder();
        sb.append("species=").append(d.speciesName).append("\n");
        sb.append("form=").append(d.formName).append("\n");
        sb.append("level=").append(d.level).append("\n");
        sb.append("friendship=").append(d.friendship).append("\n");
        sb.append("shiny=").append(d.shiny ? "true" : "false").append("\n");
        sb.append("goldenBottleCap=").append(d.goldenBottleCap ? "true" : "false").append("\n");
        sb.append("gender=").append(d.gender).append("\n");
        sb.append("nature=").append(d.nature).append("\n");
        sb.append("ability=").append(d.ability).append("\n");
        sb.append("currentHp=").append(d.currentHp).append("\n");
        sb.append("maxHp=").append(d.maxHp).append("\n");
        sb.append("ivHp=").append(d.ivHp).append("\n");
        sb.append("ivAtk=").append(d.ivAtk).append("\n");
        sb.append("ivDef=").append(d.ivDef).append("\n");
        sb.append("ivSpA=").append(d.ivSpA).append("\n");
        sb.append("ivSpD=").append(d.ivSpD).append("\n");
        sb.append("ivSpe=").append(d.ivSpe).append("\n");
        sb.append("evHp=").append(d.evHp).append("\n");
        sb.append("evAtk=").append(d.evAtk).append("\n");
        sb.append("evDef=").append(d.evDef).append("\n");
        sb.append("evSpA=").append(d.evSpA).append("\n");
        sb.append("evSpD=").append(d.evSpD).append("\n");
        sb.append("evSpe=").append(d.evSpe).append("\n");
        sb.append("move1=").append(d.move1).append("\n");
        sb.append("move2=").append(d.move2).append("\n");
        sb.append("move3=").append(d.move3).append("\n");
        sb.append("move4=").append(d.move4).append("\n");
        sb.append("heldItem=").append(d.heldItem).append("\n");
        sb.append("status=").append(d.status).append("\n");
        return sb.toString();
    }

    private void applyDataString(PokemonData d, String text) {
        if (text == null || text.isEmpty()) {
            PokemonEditorDebug.warnEditor("[Screen] applyDataString: empty text, skipping");
            return;
        }
        PokemonEditorDebug.logEditor("[Screen] applyDataString START: textLen={}", text.length());
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // 元数据行：# 开头但后面有 key=value（比如 # pokemonUUID=xxx  experience=xxx）
            if (line.startsWith("#")) {
                String metaContent = line.substring(1).trim();
                if (metaContent.contains("=")) {
                    // 解析元数据行的 key=value
                    int pos = 0;
                    while (pos < metaContent.length()) {
                        int eq = metaContent.indexOf("=", pos);
                        if (eq <= 0) break;
                        int keyStart = eq - 1;
                        while (keyStart >= pos && !Character.isWhitespace(metaContent.charAt(keyStart))) keyStart--;
                        keyStart++;
                        String key = metaContent.substring(keyStart, eq).trim().toLowerCase();
                        if (key.isEmpty()) { pos = eq + 1; continue; }
                        int valStart = eq + 1;
                        int valEnd = metaContent.length();
                        int scan = valStart;
                        while (scan < metaContent.length()) {
                            if (Character.isWhitespace(metaContent.charAt(scan))) {
                                int wordStart = scan + 1;
                                int wordEnd = wordStart;
                                while (wordEnd < metaContent.length() && !Character.isWhitespace(metaContent.charAt(wordEnd)) && metaContent.charAt(wordEnd) != '=') {
                                    wordEnd++;
                                }
                                if (wordEnd < metaContent.length() && metaContent.charAt(wordEnd) == '=' && wordEnd > wordStart) {
                                    valEnd = scan;
                                    break;
                                }
                            }
                            scan++;
                        }
                        String val = metaContent.substring(valStart, valEnd).trim();
                        try {
                            switch (key) {
                                case "pokemonuuid":
                                case "uuid": if (!val.isEmpty()) d.pokemonUUID = java.util.UUID.fromString(val); break;
                                case "experience": d.experience = Math.max(0, Integer.parseInt(val)); break;
                                case "species": if (!val.isEmpty()) d.speciesName = val; break;
                                case "level": d.level = Math.max(1, Math.min(900, Integer.parseInt(val))); break;
                                case "shiny": d.shiny = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1"); break;
                                case "goldenbottlecap": d.goldenBottleCap = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1"); break;
                                case "gender": d.gender = Math.max(0, Math.min(2, Integer.parseInt(val))); break;
                                case "nature": if (!val.isEmpty()) d.nature = val; break;
                                case "ability": if (!val.isEmpty()) d.ability = val; break;
                                case "helditem": case "held_item": if (!val.isEmpty()) d.heldItem = val; break;
                                case "status": if (val != null) d.status = val.trim(); break;
                            }
                        } catch (Throwable ignored) {}
                        pos = valEnd;
                        while (pos < metaContent.length() && Character.isWhitespace(metaContent.charAt(pos))) pos++;
                    }
                }
                continue; // 元数据行处理完跳过
            }

            // 在一行内查找所有 key=value 对（支持"ivHp=31 ivAtk=20"这种一行多值）
            // 策略：每次找第一个 '='，key 是前面的单词，value 是到下一个 'word=' 之前的部分
            int pos = 0;
            while (pos < line.length()) {
                int eq = line.indexOf("=", pos);
                if (eq <= 0) break;

                // 找 key：从 pos 到 eq，取最后一个空白后的单词
                int keyStart = eq - 1;
                while (keyStart >= pos && !Character.isWhitespace(line.charAt(keyStart))) keyStart--;
                keyStart++;
                String key = line.substring(keyStart, eq).trim().toLowerCase();
                if (key.isEmpty()) { pos = eq + 1; continue; }

                // 找 value：从 eq+1 开始，到下一个 "word=" 模式之前
                int valStart = eq + 1;
                int valEnd = line.length();
                // 找下一个 " Xxx=" 模式（空白 + 单词 + =）
                int scan = valStart;
                while (scan < line.length()) {
                    if (Character.isWhitespace(line.charAt(scan))) {
                        // 尝试匹配：空白+单词+=
                        int wordStart = scan + 1;
                        int wordEnd = wordStart;
                        while (wordEnd < line.length() && !Character.isWhitespace(line.charAt(wordEnd)) && line.charAt(wordEnd) != '=') {
                            wordEnd++;
                        }
                        if (wordEnd < line.length() && line.charAt(wordEnd) == '=' && wordEnd > wordStart) {
                            // 发现下一个 key=，则当前 value 到 scan 为止
                            valEnd = scan;
                            break;
                        }
                    }
                    scan++;
                }
                String val = line.substring(valStart, valEnd).trim();
                // 去掉行尾注释
                int hash = val.indexOf("#");
                if (hash > 0) val = val.substring(0, hash).trim();

                // 应用这个 key=value
                try {
                    switch (key) {
                        case "species": d.speciesName = PokemonNameMap.toEnglish(val); break;
                        case "form": d.formName = val; break;
                        case "level": d.level = Math.max(1, Math.min(900, Integer.parseInt(val))); break;
                        case "experience": d.experience = Math.max(0, Integer.parseInt(val)); break;
                        case "friendship": d.friendship = Math.max(0, Math.min(255, Integer.parseInt(val))); break;
                        case "shiny": d.shiny = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1"); break;
                        case "gender": d.gender = Math.max(0, Math.min(2, Integer.parseInt(val))); break;
                        case "nature": d.nature = PokemonNameMap.natureToEnglish(val); break;
                        case "ability": d.ability = PokemonNameMap.abilityToEnglish(val); break;
                        case "currenthp":
                        case "current_hp": d.currentHp = Math.max(0, Math.min(9999, Integer.parseInt(val))); break;
                        case "maxhp":
                        case "max_hp": d.maxHp = Math.max(1, Math.min(9999, Integer.parseInt(val))); break;
                        case "ivhp": case "iv_hp": d.ivHp = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "ivatk": case "iv_atk": d.ivAtk = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "ivdef": case "iv_def": d.ivDef = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "ivspa": case "iv_spa": d.ivSpA = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "ivspd": case "iv_spd": d.ivSpD = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "ivspe": case "iv_spe": d.ivSpe = Math.max(0, Math.min(31, Integer.parseInt(val))); break;
                        case "evhp": case "ev_hp": d.evHp = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "evatk": case "ev_atk": d.evAtk = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "evdef": case "ev_def": d.evDef = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "evspa": case "ev_spa": d.evSpA = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "evspd": case "ev_spd": d.evSpD = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "evspe": case "ev_spe": d.evSpe = Math.max(0, Math.min(252, Integer.parseInt(val))); break;
                        case "move1": d.move1 = PokemonNameMap.moveToEnglish(val); break;
                        case "move2": d.move2 = PokemonNameMap.moveToEnglish(val); break;
                        case "move3": d.move3 = PokemonNameMap.moveToEnglish(val); break;
                        case "move4": d.move4 = PokemonNameMap.moveToEnglish(val); break;
                        case "helditem": case "held_item": d.heldItem = PokemonNameMap.itemToEnglish(val); break;
                        case "status": if (val != null) d.status = val.trim(); break;
                    }
                } catch (Throwable ignored) {}

                pos = valEnd;
                // 跳过空白
                while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) pos++;
            }
        }
        PokemonEditorDebug.logEditor("[Screen] applyDataString DONE: species={}, level={}, shiny={}, friendship={}, ivHp={}, move1={}",
                d.speciesName, d.level, d.shiny, d.friendship, d.ivHp, d.move1);
    }

    // ========== 背景：不做全屏纯黑遮罩，只渲染编辑器容器范围 ==========
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 只做一层淡色半透明遮罩覆盖场景，不做纯黑
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC0B1220);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int screenW = this.width;

        // 动态更新保存按钮状态（空槽位需物种字段不为空）
        updateSaveButtonState();

        // ========== 主题颜色：亮色=亮蓝，深色=暗灰（读取配置文件，跨启动保存） ==========
        int labelColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFF3B82F6 : 0xFF1E293B;

        // ========== Tab 可见性控制：根据 activeTab 显示/隐藏组件 ==========
        // 概要 Tab (activeTab==0): 显示物种/等级/昵称/性格/特性/亲密度/形态/携带物/性别/闪光
        // 能力 Tab (activeTab==2): 显示 IV/EV
        // 对战 Tab (activeTab==1): 显示当前HP/最大HP/招式
        // 高级 Tab (activeTab==3): 显示经验/UUID/数据区+复制粘贴按钮
        boolean isTab0 = (activeTab == 0);
        boolean isTab1 = (activeTab == 1);
        boolean isTab2 = (activeTab == 2);
        boolean isTab3 = (activeTab == 3);

        if (sumSpeciesField != null) {
            boolean show = isTab0;
            sumSpeciesField.visible = show; sumLevelField.visible = show;
            sumNameField.visible = show; sumNatureField.visible = show;
            sumAbilityField.visible = show; sumFriendshipField.visible = show;
            sumFormField.visible = show; sumHeldItemField.visible = show;
            genderField.visible = show; genderCycleBtn.visible = show;
            sumShinyButton.visible = show;
            if (clearNameBtn != null) clearNameBtn.visible = show;
            if (levelDownBtn != null) levelDownBtn.visible = show;
            if (levelUpBtn != null) levelUpBtn.visible = show;
            if (friendshipDownBtn != null) friendshipDownBtn.visible = show;
            if (friendshipUpBtn != null) friendshipUpBtn.visible = show;
        }
        if (ivHp != null) {
            boolean show = isTab2;
            ivHp.visible = show; ivAtk.visible = show; ivDef.visible = show;
            ivSpA.visible = show; ivSpD.visible = show; ivSpe.visible = show;
            evHp.visible = show; evAtk.visible = show; evDef.visible = show;
            evSpA.visible = show; evSpD.visible = show; evSpe.visible = show;
            for (int i = 0; i < 6; i++) {
                if (ivUpButtons[i] != null) ivUpButtons[i].visible = show;
                if (ivDownButtons[i] != null) ivDownButtons[i].visible = show;
                if (evUpButtons[i] != null) evUpButtons[i].visible = show;
                if (evDownButtons[i] != null) evDownButtons[i].visible = show;
            }
            if (maxIvButton != null) maxIvButton.visible = show;
            if (clearEvButton != null) clearEvButton.visible = show;
            if (fullEvButton != null) fullEvButton.visible = show;
            crownVisible = show;
            if (hiddenPowerBtn != null) hiddenPowerBtn.visible = show;
        }
        if (currentHpField != null) {
            boolean show = isTab1;
            currentHpField.visible = show; maxHpField.visible = show;
            move1Field.visible = show; move2Field.visible = show;
            move3Field.visible = show; move4Field.visible = show;
            move1PPField.visible = show; move2PPField.visible = show;
            move3PPField.visible = show; move4PPField.visible = show;
        }
        if (advExpField != null) {
            boolean show = isTab3;
            // 全队模式：隐藏经验和 UUID 输入框
            if (viewingAllTeam) {
                advExpField.visible = false;
                advUUIDField.visible = false;
            } else {
                advExpField.visible = show;
                advUUIDField.visible = show;
            }
            // advRawField 在高级 Tab 中保持可见（接收键盘事件）；非高级 Tab 中隐藏
            advRawField.visible = show;
            if (advCopyBtn != null) advCopyBtn.visible = show;
            if (advPasteBtn != null) advPasteBtn.visible = show;
            if (advExportBtn != null) advExportBtn.visible = show;
            if (advImportBtn != null) advImportBtn.visible = show;
            if (advSaveBtn != null) advSaveBtn.visible = show;
            if (advResetBtn != null) advResetBtn.visible = show;
            if (teamAllBtn != null) teamAllBtn.visible = show;
        }

        // 1. 底层遮罩：半透明深蓝灰（仅挡住场景，不渲染全屏幕纯黑）
        //    只在编辑器周边区域做淡阴影，不覆盖全屏
        int panelX = screenW / 2 - 280;
        int panelY = 60;
        int panelW = 560;
        int panelH = 340;
        int partyX = panelX + 10;
        int partyY = panelY + 10;
        int editX = panelX + 165;
        int editY = panelY + 10;
        int editW = panelW - 175;

        // ========== 单个统一容器：编辑器面板范围（只渲染我们用到的区域，不铺满全屏）==========
        int containerTop = panelY - 35;     // Tab栏顶
        int containerBot = panelY + panelH + 40;  // 底部按钮底
        int containerL = panelX - 8;
        int containerR = panelX + panelW + 8;

        // 容器外阴影（4个方向，不用纯黑）
        guiGraphics.fill(containerL - 3, containerTop - 3, containerR + 3, containerTop, 0x33000000);
        guiGraphics.fill(containerL - 3, containerBot, containerR + 3, containerBot + 3, 0x33000000);
        guiGraphics.fill(containerL - 3, containerTop, containerL, containerBot, 0x33000000);
        guiGraphics.fill(containerR, containerTop, containerR + 3, containerBot, 0x33000000);

        // 主容器背景：淡蓝白色
        guiGraphics.fill(containerL, containerTop, containerR, containerBot, 0xFFF0F4FA);

        // 容器边框：淡蓝灰色细线
        guiGraphics.fill(containerL, containerTop, containerR, containerTop + 1, 0xFFB8C7DA);
        guiGraphics.fill(containerL, containerBot - 1, containerR, containerBot, 0xFFB8C7DA);
        guiGraphics.fill(containerL, containerTop, containerL + 1, containerBot, 0xFFB8C7DA);
        guiGraphics.fill(containerR - 1, containerTop, containerR, containerBot, 0xFFB8C7DA);

        // 顶部标题已移除，移到"正在编辑"右侧显示（见下方）

        // Tab栏选中下划线（蓝色）
        int tabBarY = panelY - 30;
        int tabBarH = 22;
        int tabW = panelW / 4;
        int tabStartX = panelX;
        int selectedTabX = tabStartX + activeTab * tabW;
        guiGraphics.fill(selectedTabX + 10, tabBarY + tabBarH + 1, selectedTabX + tabW - 10, tabBarY + tabBarH + 3, 0xFF3B82F6);

        // 队伍区与编辑区之间一条细竖线（仅做信息分隔）
        int dividerX = panelX + 160;
        guiGraphics.fill(dividerX, partyY - 5, dividerX + 1, panelY + panelH - 5, 0xFFCBD5E1);

        // 队伍区标题（亮色/深色由主题决定）
        guiGraphics.drawString(this.font, "队伍", partyX + 55, partyY - 13, labelColor, true);

        // 当前选中槽位信息（颜色由主题决定），标题"宝可梦编辑器-玩家名"显示在右边（不会重叠）
        // 先计算右侧编辑器标题的位置，确保左侧"正在编辑"不会和它重叠
        String editorTitle = "宝可梦编辑器 - " + targetPlayerName;
        int editorTitleW = this.font.width(editorTitle);
        int editorTitleX = panelX + panelW - editorTitleW - 10;  // 右侧标题的X（最靠左的位置）
        // "正在编辑"文字的最大可用宽度 = 右侧标题X - 编辑区左边缘 - 20像素间距
        int maxLeftW = editorTitleX - editX - 20;

        if (!viewingAllTeam) {
            if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                PokemonData d = partyData.get(selectedSlot);
                if (d.exists) {
                    String title = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
                    String leftText = "正在编辑: " + title + (d.shiny ? " \u2728" : "");
                    if (this.font.width(leftText) > maxLeftW && maxLeftW > 20) {
                        String ellipsis = "...";
                        int availW = maxLeftW - this.font.width(ellipsis);
                        while (this.font.width(leftText) > availW && leftText.length() > 4) {
                            leftText = leftText.substring(0, leftText.length() - 1);
                        }
                        leftText = leftText + ellipsis;
                    }
                    guiGraphics.drawString(this.font, leftText, editX, editY - 13, labelColor, true);
                    guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, 0xFF3B82F6, true);
                } else {
                    guiGraphics.drawString(this.font, "(空槽位)", editX, editY - 13, 0xFF64748B, true);
                    guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, 0xFF3B82F6, true);
                }
            } else {
                guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, 0xFF3B82F6, true);
            }
        }

        // ========== 各Tab内容 ==========
        if (activeTab == 0) {
            int rowY = editY + 18;
            int rowStep = 28;
            String[] labels = {"物种", "等级", "昵称", "性格", "特性", "亲密度", "形态", "携带物", "性别", "闪光"};
            for (int i = 0; i < labels.length; i++) {
                int y = rowY + rowStep * i;
                guiGraphics.drawString(this.font, labels[i] + ":", editX, y + 4, labelColor, true);
            }
            // 在性别输入框右侧显示文本说明（0=雄性 1=雌性 2=无性别）
            if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                int gv = parseOrDefault(genderField, 2);
                String genderText = " (" + (gv == 0 ? "\u2642 雄性" : gv == 1 ? "\u2640 雌性" : "\u26AA 无性别") + ")";
                int genderTextColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFFCBD5E1 : 0xFF475569;
                guiGraphics.drawString(this.font, genderText, editX + 50 + 10 + 65 + 100, rowY + rowStep * 8 + 4, genderTextColor, true);
            }

            // ★ 携带物图标渲染（在携带物输入框右侧）
            int iconX = editX + 50 + 10 + 160 + 8;
            int iconY = rowY + rowStep * 7;
            try {
                String heldZh = sumHeldItemField.getValue();
                if (heldZh != null && !heldZh.trim().isEmpty()) {
                    String heldEn = PokemonNameMap.itemToEnglish(heldZh);
                    String rawId = heldEn;
                    if (rawId.contains(":")) {
                        int idx = rawId.lastIndexOf(":");
                        rawId = rawId.substring(idx + 1);
                    }
                    rawId = rawId.trim().toLowerCase();
                    net.minecraft.world.item.Item foundItem = null;
                    for (Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item>, net.minecraft.world.item.Item> e : net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet()) {
                        String id = e.getKey().location().getPath();
                        if (id.equalsIgnoreCase(rawId)) {
                            foundItem = e.getValue();
                            break;
                        }
                    }
                    if (foundItem != null && foundItem != net.minecraft.world.item.Items.AIR) {
                        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(foundItem);
                        guiGraphics.renderItem(stack, iconX, iconY + 1);
                        guiGraphics.renderItemDecorations(this.font, stack, iconX, iconY + 1);
                        PokemonEditorDebug.logEditor("[Screen] heldItem icon: rendered '{}' (UI='{}'→EN='{}') at ({},{})",
                                rawId, heldZh, heldEn, iconX, iconY);
                    } else {
                        PokemonEditorDebug.warnEditor("[Screen] heldItem icon: NOT FOUND - UI='{}' → EN='{}' → rawId='{}'",
                                heldZh, heldEn, rawId);
                    }
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("[Screen] heldItem icon render error: {}", t.getMessage());
            }
            // ========== 右侧宝可梦信息卡 ==========
            if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                PokemonData d = partyData.get(selectedSlot);
                if (d.exists) {
                    int cardX = editX + 335;
                    int cardY = rowY - 5;
                    int cardW = 120;
                    int cardH = 130;
                    // 卡片外框
                    int frameColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFFCBD5E1 : 0xFF475569;
                    int bgColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFFFCFDFE : 0xFF1E293B;
                    int topBarColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFF3B82F6 : 0xFF2563EB;
                    int labelGrey = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFF94A3B8 : 0xFF64748B;
                    int valueColor = PokemonEditorConfig.BRIGHT_THEME.get() ? 0xFFE2E8F0 : 0xFF1E293B;
                    guiGraphics.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, frameColor);
                    guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);
                    // 卡片顶部蓝色横条
                    guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + 22, topBarColor);
                    String zhName = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
                    // 顶部名称（白字，带阴影）
                    String cardTitle = zhName + (d.shiny ? " \u2728" : "");
                    guiGraphics.drawString(this.font, cardTitle,
                            cardX + cardW / 2 - this.font.width(cardTitle) / 2, cardY + 7, 0xFFFFFFFF, true);
                    // 信息区：物种英文、等级、特性、性格
                    int infoY = cardY + 30;
                    int infoX = cardX + 10;
                    // 性格效果（中文描述
                    String natureEffect = PokemonNameMap.natureEffectText(d.nature);
                    String natureDisp = PokemonNameMap.natureToChinese(d.nature) + natureEffect;
                    // 超长截断
                    if (natureDisp.length() > 14) natureDisp = natureDisp.substring(0, 14);
                    String[][] info = {
                        {"物种", d.speciesName},
                        {"等级", "Lv." + d.level},
                        {"性别", d.gender == 0 ? "\u2642 雄" : d.gender == 1 ? "\u2640 雌" : "\u26AA 无"},
                        {"性格", natureDisp},
                        {"特性", PokemonNameMap.abilityToChinese(d.ability)},
                        {"形态", d.formName.isEmpty() ? "普通" : d.formName}
                    };
                    for (int i = 0; i < info.length; i++) {
                        int yy = infoY + i * 18;
                        guiGraphics.drawString(this.font, info[i][0] + ":", infoX, yy, labelGrey, true);
                        String val = info[i][1];
                        if (val != null && val.length() > 14) val = val.substring(0, 14);
                        guiGraphics.drawString(this.font, val == null ? "" : val, infoX + 40, yy, valueColor, true);
                    }
                }
            }

            // ========== 物种/性格/特性字段共享下拉 ==========
            int fieldLabelW = 50;
            int fieldX = editX + fieldLabelW + 10;
            int speciesY = rowY;                // 行0: 物种
            int natureY = rowY + rowStep * 3;   // 行3: 性格
            int abilityY = rowY + rowStep * 4;  // 行4: 特性

            int dropdownY = -1;
            int activeX = fieldX;
            int activeW = 230;
            String q = "";
            int newActiveField = -1;

            if (sumSpeciesField.isFocused()) {
                dropdownY = speciesY + 20;
                activeX = sumSpeciesField.getX();
                activeW = sumSpeciesField.getWidth();
                q = sumSpeciesField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
                newActiveField = 0;
            } else if (sumNatureField.isFocused()) {
                dropdownY = natureY + 20;
                activeX = sumNatureField.getX();
                activeW = sumNatureField.getWidth();
                q = sumNatureField.getValue().trim();
                newActiveField = 1;
            } else if (sumAbilityField.isFocused()) {
                dropdownY = abilityY + 20;
                activeX = sumAbilityField.getX();
                activeW = sumAbilityField.getWidth();
                q = sumAbilityField.getValue().trim();
                newActiveField = 2;
            } else {
                // 没有聚焦字段，清空下拉
                autocompleteStartY = 0;
                autocompleteItems.clear();
                autocompleteScrollOffset = 0;
                autocompleteActiveField = -1;
            }

            if (dropdownY > 0) {
                autocompleteStartY = dropdownY;
                autocompleteX = activeX;
                autocompleteW = activeW;
                // 只有当激活字段或搜索内容变化时才重建列表并重置滚动
                boolean searchChanged = (newActiveField != autocompleteActiveField) || !q.equals(lastSearchQuery);
                if (searchChanged) {
                    autocompleteItems.clear();
                    autocompleteScrollOffset = 0;
                    // ====== 重建列表内容（只在搜索条件变化时构建一次）
                    if (newActiveField == 0) {
                        java.util.List<String> species = PokemonNameMap.speciesList();
                        if (!q.isEmpty()) {
                            // [线性-L15] 图鉴序号搜索：输入纯数字时匹配全国图鉴编号
                            if (q.matches("\\d+")) {
                                int num = Integer.parseInt(q);
                                String zh = PokemonNameMap.getSpeciesByNumber(num);
                                if (zh != null) autocompleteItems.add(zh);
                                // 模糊匹配：以输入数字开头的图鉴编号（如输入"1"匹配1,10-19,100-199）
                                try {
                                    java.util.Map<Integer, String> numMap = CobblemonBridge.getNationalDexNumberMap();
                                    for (java.util.Map.Entry<Integer, String> e : numMap.entrySet()) {
                                        int dexNum = e.getKey();
                                        if (dexNum != num && String.valueOf(dexNum).startsWith(q)) {
                                            String z = PokemonNameMap.toChinese(e.getValue());
                                            if (z != null && !autocompleteItems.contains(z)) autocompleteItems.add(z);
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            for (String zh : species) {
                                String enName = PokemonNameMap.toEnglish(zh);
                                if (zh.contains(q) || (enName != null && enName.contains(q))) {
                                    if (!autocompleteItems.contains(zh)) autocompleteItems.add(zh);
                                }
                                // [线性-L15] 拼音搜索：利用 jecharacters 的 PinIn 引擎
                                if (!autocompleteItems.contains(zh)) {
                                    try {
                                        if (me.towdium.jecharacters.utils.Match.contains(zh, q)) {
                                            autocompleteItems.add(zh);
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        } else {
                            autocompleteItems.addAll(species);
                        }
                    } else if (newActiveField == 1) {
                        java.util.List<String> natures = PokemonNameMap.natureList();
                        if (q.isEmpty()) {
                            autocompleteItems.addAll(natures);
                        } else {
                            for (String zh : natures) {
                                if (zh.contains(q)) autocompleteItems.add(zh);
                            }
                        }
                    } else if (newActiveField == 2) {
                        java.util.List<String> abilities = PokemonNameMap.abilityList();
                        if (selectedSlot >= 0 && selectedSlot < partyData.size()) {
                            PokemonData d = partyData.get(selectedSlot);
                            if (d.exists && d.possibleAbilities != null && !d.possibleAbilities.isEmpty()) {
                                java.util.List<String> zhAbilities = new java.util.ArrayList<>();
                                for (String enAbility : d.possibleAbilities) {
                                    String zh = PokemonNameMap.abilityToChinese(enAbility);
                                    if (zh != null && !zh.isEmpty() && !zhAbilities.contains(zh)) zhAbilities.add(zh);
                                }
                                if (!zhAbilities.isEmpty()) abilities = zhAbilities;
                            }
                        }
                        if (q.isEmpty()) {
                            autocompleteItems.addAll(abilities);
                        } else {
                            for (String zh : abilities) {
                                if (zh.contains(q)) autocompleteItems.add(zh);
                            }
                        }
                    }
                }
                autocompleteActiveField = newActiveField;
                lastSearchQuery = q;
            }

            // 下拉列表不在此处绘制（移到 super.render 之后，确保在最上层）
        } else if (activeTab == 2) {
            int ivStartY = editY + 40;
            int statRowH = 28;
            int ivColX = editX + 10;
            int evColX = editX + 260;

            String ivTitle = "IV (0-31)";
            guiGraphics.drawString(this.font, ivTitle,
                    ivColX + 60 - this.font.width(ivTitle) / 2, editY + 15, 0xFF10B981, true);
            String evTitle = "EV (0-252)";
            guiGraphics.drawString(this.font, evTitle,
                    evColX + 60 - this.font.width(evTitle) / 2, editY + 15, 0xFF3B82F6, true);

            String[] statLabels = {"HP", "攻击", "防御", "特攻", "特防", "速度"};
            for (int i = 0; i < statLabels.length; i++) {
                int y = ivStartY + statRowH * i;
                guiGraphics.drawString(this.font, statLabels[i] + ":", ivColX, y + 4, labelColor, true);
                guiGraphics.drawString(this.font, statLabels[i] + ":", evColX, y + 4, labelColor, true);
            }
            // EV总和显示（在 stat 行和按钮之间）
            int evSum = 0;
            for (EditBox b : new EditBox[]{evHp, evAtk, evDef, evSpA, evSpD, evSpe}) {
                evSum += parseOrDefault(b, 0);
            }
            int sumY = ivStartY + statRowH * 6 - 4;
            int evCap = CobblemonTSConfig.getEVTotalCap();
            guiGraphics.drawString(this.font, "EV总和: " + evSum + "/" + evCap, evColX, sumY, (evSum <= evCap ? 0xFF3B82F6 : 0xFFEF4444), true);

            // ★ 金色王冠：图标(静态材质) + 开关按钮(红色=关，绿色=开) 分离渲染
        if (selectedSlot >= 0 && crownVisible) {
            PokemonData d = partyData.get(selectedSlot);
            int bgColor = d.goldenBottleCap ? 0xFF55FF55 : 0xFFFF5555; // 亮绿=开，亮红=关
            
            // ① 绘制金色王冠材质图标
            ResourceLocation crownIcon = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "textures/item/gold_crown.png");
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 400);
            guiGraphics.blit(crownIcon, crownIconX, crownIconY, 0, 0, CROWN_SIZE, CROWN_SIZE, CROWN_SIZE, CROWN_SIZE);
            guiGraphics.pose().popPose();
            
            // ② 绘制开关按钮(有色矩形，紧挨图标右边)
            guiGraphics.fill(crownToggleX, crownToggleY, crownToggleX + TOGGLE_SIZE, crownToggleY + TOGGLE_SIZE, 0xFF000000 | bgColor);
            // 按钮内文字：ON/OFF
            String toggleText = d.goldenBottleCap ? "ON" : "OFF";
            int textW = this.font.width(toggleText);
            guiGraphics.drawString(this.font, toggleText,
                    crownToggleX + (TOGGLE_SIZE - textW) / 2,
                    crownToggleY + (TOGGLE_SIZE - 8) / 2,
                    0xFFFFFFFF, false);
        }

        // ★ 觉醒力量显示文本
        if (selectedSlot >= 0 && hiddenPowerType != null && !hiddenPowerType.isEmpty()) {
            int hpCalcY = ivStartY + statRowH * 6 + 16 + 28;
            String zhName = getHiddenPowerTypeZh();
            String display = zhName + " ";
            int textW = this.font.width(display);
            int textX = ivColX + 5 + 80 + 10; // 按钮右边
            guiGraphics.drawString(this.font, display, textX, hpCalcY + 2, labelColor, true);
            // ★ 绘制类型图标（紧挨文字右边，使用cobblemon的types_small精灵图）
            try {
                Object elementalType = Class.forName("com.cobblemon.mod.common.api.types.ElementalTypes")
                        .getMethod("get", String.class).invoke(null, hiddenPowerType);
                if (elementalType != null) {
                    int texX = (int) elementalType.getClass().getMethod("getTextureXMultiplier").invoke(elementalType);
                    ResourceLocation sheet = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/types_small.png");
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 400);
                    guiGraphics.blit(sheet, textX + textW, hpCalcY + 1, texX * 18, 0, 18, 18, 324, 18);
                    guiGraphics.pose().popPose();
                }
            } catch (Exception ignored) {}
        }
        } else if (activeTab == 1) {
            int battleY = editY + 15;
            guiGraphics.drawString(this.font, "当前HP:", editX, battleY + 4, labelColor, true);
            guiGraphics.drawString(this.font, "最大HP:", editX, battleY + 34, labelColor, true);
            int moveY = battleY + 70;
            int moveLabelW = 40; // 预留标签宽度，避免"招式1"被输入框挡住
            int moveW = 180;
            for (int i = 0; i < 4; i++) {
                int y = moveY + 25 * i;
                guiGraphics.drawString(this.font, "招式" + (i + 1) + ":", editX, y + 4, labelColor, true);
            }
            guiGraphics.drawString(this.font, "PP", editX + moveLabelW + moveW + 20, moveY - 9, labelColor, true);
            guiGraphics.drawString(this.font, "最大PP", editX + moveLabelW + moveW + 10 + 60 + 10 + 20, moveY - 9, 0xFF3B82F6, true);

            // ========== 招式字段共享下拉 ==========
            int moveFieldX = editX + moveLabelW + 10;
            int move1Y = moveY;
            int move2Y = moveY + 25;
            int move3Y = moveY + 50;
            int move4Y = moveY + 75;

            int dropdownY = -1;
            int activeX = moveFieldX;
            int activeW = moveW;
            String q = "";
            int newActiveField = -1;

            if (move1Field.isFocused()) {
                dropdownY = move1Y + 20;
                activeX = move1Field.getX();
                activeW = move1Field.getWidth();
                q = move1Field.getValue().trim();
                newActiveField = 3;
            } else if (move2Field.isFocused()) {
                dropdownY = move2Y + 20;
                activeX = move2Field.getX();
                activeW = move2Field.getWidth();
                q = move2Field.getValue().trim();
                newActiveField = 4;
            } else if (move3Field.isFocused()) {
                dropdownY = move3Y + 20;
                activeX = move3Field.getX();
                activeW = move3Field.getWidth();
                q = move3Field.getValue().trim();
                newActiveField = 5;
            } else if (move4Field.isFocused()) {
                dropdownY = move4Y + 20;
                activeX = move4Field.getX();
                activeW = move4Field.getWidth();
                q = move4Field.getValue().trim();
                newActiveField = 6;
            } else {
                autocompleteStartY = 0;
                autocompleteItems.clear();
                autocompleteScrollOffset = 0;
                autocompleteActiveField = -1;
            }

            if (dropdownY > 0) {
                autocompleteStartY = dropdownY;
                autocompleteX = activeX;
                autocompleteW = activeW;
                boolean searchChanged = (newActiveField != autocompleteActiveField) || !q.equals(lastSearchQuery);
                if (searchChanged) {
                    autocompleteItems.clear();
                    autocompleteScrollOffset = 0;
                    // 动态从 Cobblemon 获取完整招式列表（MOVE_EN_TO_ZH 会被自动填充）
                    java.util.List<String> allMoves = PokemonNameMap.moveList();
                    if (q.isEmpty()) {
                        for (String zh : allMoves) {
                            autocompleteItems.add(zh);
                        }
                    } else {
                        String lowerQ = q.toLowerCase(java.util.Locale.ROOT);
                        for (String zhName : allMoves) {
                            String enName = PokemonNameMap.moveToEnglish(zhName);
                            // 中文搜索 + 英文搜索
                            if (zhName.contains(q) || (enName != null && enName.contains(lowerQ))) {
                                if (!autocompleteItems.contains(zhName)) autocompleteItems.add(zhName);
                            }
                            // [线性-L15] 拼音搜索：利用 jecharacters 的 PinIn 引擎
                            if (!autocompleteItems.contains(zhName)) {
                                try {
                                    if (me.towdium.jecharacters.utils.Match.contains(zhName, q)) {
                                        autocompleteItems.add(zhName);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
                autocompleteActiveField = newActiveField;
                lastSearchQuery = q;
            }
        } else if (activeTab == 3) {
            int advY = editY + 15;
            if (!viewingAllTeam) {
                guiGraphics.drawString(this.font, "经验值:", editX, advY + 4, labelColor, true);
                guiGraphics.drawString(this.font, "UUID:", editX, advY + 39, labelColor, true);
                guiGraphics.drawString(this.font, "数据区（可编辑/复制/粘贴）:", editX + 10, advY + 64, labelColor, true);
            } else {
                // 全队模式：顶部显示"全队数据"提示
                guiGraphics.drawString(this.font, "全队数据（可编辑/复制/粘贴）:", editX + 10, editY + 4, labelColor, true);
            }
        }

        // advRawField 在数据区位置，我们的自定义多行渲染会覆盖它的默认居中单行渲染
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ========== 高级 Tab 数据区：自定义多行渲染 ==========
        // advRawField 是数据区第一行的单行 EditBox，下方从第二行开始显示完整的多行数据
        if (activeTab == 3 && advRawField != null) {
            int boxX = dataAreaX;
            int boxY = dataAreaY + 25;   // 从第二行开始（第一行留给 advRawField 单行编辑框）
            int boxW = dataAreaW;
            int boxH = dataAreaH - 25;   // 相应减少高度
            int padX = 4;
            int padY = 4;
            int lineH = 10;

            boolean bright = PokemonEditorConfig.BRIGHT_THEME.get();
            int boxBg = bright ? 0xFFFCFDFE : 0xFF1E293B;
            int boxFrame = bright ? 0xFFB8C7DA : 0xFF475569;
            int boxTopFocus = bright ? 0xFF3B82F6 : 0xFF2563EB;
            int textColor = bright ? 0xFF0F172A : 0xFFF1F5F9;
            int cursorColor = textColor;
            int selectionBg = 0xFF3B82F6;
            int selectionFg = 0xFFFFFFFF;

            // 1. 背景 + 边框
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxBg);
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, boxFrame);
            guiGraphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, boxFrame);
            guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxH, boxFrame);
            guiGraphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, boxFrame);
            if (advRawField.isFocused()) {
                guiGraphics.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + 2, boxTopFocus);
            }

            String text = advRawField.getValue();
            if (text == null || text.isEmpty()) {
                // 空文本但有焦点时绘制光标
                if (advRawField.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                    guiGraphics.fill(boxX + padX, boxY + padY - 1, boxX + padX + 1, boxY + padY + lineH - 2, cursorColor);
                }
                return;
            }

            int maxVisibleLines = (boxH - padY * 2) / lineH;
            int contentMaxW = boxW - padX * 2;
            MultilineLayout layout = layoutMultiline(text, contentMaxW);
            java.util.List<String> lines = layout.displayLines;
            java.util.List<Integer> charOffsets = layout.charOffsets;

            // 2. 计算光标位置
            int cursorPos = advRawField.getCursorPosition();
            if (cursorPos < 0) cursorPos = 0;
            if (cursorPos > text.length()) cursorPos = text.length();
            int cursorDisplayLine = 0;
            int cursorDisplayCol = 0;
            for (int li = 0; li < lines.size(); li++) {
                int lineStart = charOffsets.get(li);
                int lineEnd = (li + 1 < lines.size()) ? charOffsets.get(li + 1) : text.length();
                if (cursorPos >= lineStart && cursorPos <= lineEnd) {
                    cursorDisplayLine = li;
                    cursorDisplayCol = cursorPos - lineStart;
                    break;
                }
                if (li == lines.size() - 1) {
                    cursorDisplayLine = li;
                    cursorDisplayCol = cursorPos - lineStart;
                }
            }
            if (cursorDisplayCol < 0) cursorDisplayCol = 0;
            if (cursorDisplayCol > lines.get(cursorDisplayLine).length()) cursorDisplayCol = lines.get(cursorDisplayLine).length();

            // 3. 计算选中位置（反射拿 EditBox 内部的 highlightPos）
            int highlightPos = cursorPos;
            try {
                java.lang.reflect.Field hpField = EditBox.class.getDeclaredField("highlightPos");
                hpField.setAccessible(true);
                highlightPos = hpField.getInt(advRawField);
            } catch (Throwable ignored) {}
            if (highlightPos < 0) highlightPos = 0;
            if (highlightPos > text.length()) highlightPos = text.length();
            int selStart = Math.min(cursorPos, highlightPos);
            int selEnd = Math.max(cursorPos, highlightPos);
            boolean hasSelection = (selEnd > selStart);

            // 4. 计算选中起点/终点在第几行第几列
            int selStartDisplayLine = 0;
            int selStartDisplayCol = 0;
            int selEndDisplayLine = 0;
            int selEndDisplayCol = 0;
            if (hasSelection) {
                for (int li = 0; li < lines.size(); li++) {
                    int lineStart = charOffsets.get(li);
                    int lineEnd = (li + 1 < lines.size()) ? charOffsets.get(li + 1) : text.length();
                    if (selStart >= lineStart && selStart <= lineEnd) {
                        selStartDisplayLine = li;
                        selStartDisplayCol = selStart - lineStart;
                        break;
                    }
                    if (li == lines.size() - 1) {
                        selStartDisplayLine = li;
                        selStartDisplayCol = selStart - lineStart;
                    }
                }
                for (int li = 0; li < lines.size(); li++) {
                    int lineStart = charOffsets.get(li);
                    int lineEnd = (li + 1 < lines.size()) ? charOffsets.get(li + 1) : text.length();
                    if (selEnd >= lineStart && selEnd <= lineEnd) {
                        selEndDisplayLine = li;
                        selEndDisplayCol = selEnd - lineStart;
                        break;
                    }
                    if (li == lines.size() - 1) {
                        selEndDisplayLine = li;
                        selEndDisplayCol = selEnd - lineStart;
                    }
                }
            }

            // 5. 滚动逻辑：选中时跟随光标位置（而非选中起点），光标超出可视区才滚动
            int maxOffset = Math.max(0, lines.size() - maxVisibleLines);
            if (advRawField.isFocused()) {
                if (cursorDisplayLine < advRawScrollOffset) {
                    advRawScrollOffset = cursorDisplayLine;
                } else if (cursorDisplayLine >= advRawScrollOffset + maxVisibleLines) {
                    advRawScrollOffset = cursorDisplayLine - maxVisibleLines + 1;
                }
            }
            if (advRawScrollOffset > maxOffset) advRawScrollOffset = maxOffset;
            if (advRawScrollOffset < 0) advRawScrollOffset = 0;
            int scrollOffset = advRawScrollOffset;

            // 6. 逐行绘制
            for (int i = 0; i < maxVisibleLines && (i + scrollOffset) < lines.size(); i++) {
                int displayLineIdx = i + scrollOffset;
                int drawY = boxY + padY + i * lineH;
                String line = lines.get(displayLineIdx);
                if (line == null) line = "";

                boolean lineSelected = hasSelection && advRawField.isFocused() &&
                        displayLineIdx >= selStartDisplayLine && displayLineIdx <= selEndDisplayLine;

                if (lineSelected) {
                    // ---------- 这一行部分/全部被选中 ----------
                    boolean isStartLine = (displayLineIdx == selStartDisplayLine);
                    boolean isEndLine = (displayLineIdx == selEndDisplayLine);
                    int drawX = boxX + padX;
                    int col = 0;

                    // 起点行：先画非选中部分
                    if (isStartLine && selStartDisplayCol > 0) {
                        String pre = line.substring(0, Math.min(selStartDisplayCol, line.length()));
                        guiGraphics.drawString(this.font, pre, drawX, drawY, textColor, false);
                        drawX += this.font.width(pre);
                        col = selStartDisplayCol;
                    }
                    // 选中部分
                    int selEndCol = isEndLine ? selEndDisplayCol : line.length();
                    if (selEndCol > col) {
                        String selText = line.substring(Math.min(col, line.length()), Math.min(selEndCol, line.length()));
                        int bgW = this.font.width(selText);
                        // 先画蓝色背景
                        guiGraphics.fill(drawX, drawY - 1, drawX + bgW, drawY + lineH - 2, selectionBg);
                        // 再画白色文字
                        guiGraphics.drawString(this.font, selText, drawX, drawY, selectionFg, false);
                        drawX += bgW;
                        col = selEndCol;
                    }
                    // 终点行：后面非选中部分
                    if (isEndLine && col < line.length()) {
                        String post = line.substring(col);
                        guiGraphics.drawString(this.font, post, drawX, drawY, textColor, false);
                    }
                } else {
                    // ---------- 这一行没被选中 ----------
                    guiGraphics.drawString(this.font, line, boxX + padX, drawY, textColor, false);
                }

                // 7. 绘制光标
                if (advRawField.isFocused() && displayLineIdx == cursorDisplayLine) {
                    String upToCursor = line.substring(0, Math.min(cursorDisplayCol, line.length()));
                    int cursorX = boxX + padX + this.font.width(upToCursor);
                    int cursorTopY = drawY - 1;
                    int cursorBotY = drawY + lineH - 2;
                    if ((System.currentTimeMillis() / 500) % 2 == 0) {
                        guiGraphics.fill(cursorX, cursorTopY, cursorX + 1, cursorBotY, cursorColor);
                    }
                }
            }
        }

        // ========== 下拉列表：在 super.render 之后绘制，确保在最上层（支持滚轮滚动）==========
        if ((activeTab == 0 || activeTab == 1) && !autocompleteItems.isEmpty() && autocompleteStartY > 0) {
            int lineH = 16;
            int maxShown = 8;
            int shown = Math.min(autocompleteItems.size(), maxShown);
            // 限制滚动偏移的合法范围
            int maxOffset = Math.max(0, autocompleteItems.size() - maxShown);
            if (autocompleteScrollOffset > maxOffset) autocompleteScrollOffset = maxOffset;
            if (autocompleteScrollOffset < 0) autocompleteScrollOffset = 0;
            int listH = shown * lineH;
            int sfX = autocompleteX;
            int sY = autocompleteStartY;
            // 外边框（阴影）
            guiGraphics.fill(sfX - 2, sY - 2, sfX + autocompleteW + 2, sY + listH + 2, 0x99000000);
            guiGraphics.fill(sfX - 1, sY - 1, sfX + autocompleteW + 1, sY + listH + 1, 0xFFB8C7DA);
            // 白底
            guiGraphics.fill(sfX, sY, sfX + autocompleteW, sY + listH, 0xFFFCFDFE);
            for (int i = 0; i < shown; i++) {
                int itemIdx = autocompleteScrollOffset + i;
                if (itemIdx < 0 || itemIdx >= autocompleteItems.size()) break;
                int itemY = sY + i * lineH;
                if (mouseX >= sfX && mouseX < sfX + autocompleteW && mouseY >= itemY && mouseY < itemY + lineH) {
                    // 悬停高亮
                    guiGraphics.fill(sfX, itemY, sfX + autocompleteW, itemY + lineH, 0xFF3B82F6);
                    guiGraphics.drawString(this.font, autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, 0xFFFFFFFF, true);
                } else {
                    guiGraphics.drawString(this.font, autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, 0xFF1E293B, true);
                }
            }
            // 如果有更多项，显示滚动提示（小箭头）
            if (autocompleteItems.size() > maxShown) {
                int arrowX = sfX + autocompleteW - 12;
                if (autocompleteScrollOffset > 0) {
                    guiGraphics.drawString(this.font, "\u2191", arrowX, sY - 12, 0xFF3B82F6, true);
                }
                if (autocompleteScrollOffset < maxOffset) {
                    guiGraphics.drawString(this.font, "\u2193", arrowX, sY + listH + 2, 0xFF3B82F6, true);
                }
            }
        }

        // ========== 导入文件选择对话框（最上层）==========
        if (showImportDialog && pendingImportFiles != null) {
            int dlgW = 480;
            int dlgH = 360;
            int dlgX = (this.width - dlgW) / 2;
            int dlgY = (this.height - dlgH) / 2;

            boolean bright = PokemonEditorConfig.BRIGHT_THEME.get();
            int dlgBg = bright ? 0xFFF1F5F9 : 0xFF0F172A;
            int dlgFrame = bright ? 0xFF94A3B8 : 0xFF334155;
            int dlgTitleBg = bright ? 0xFF3B82F6 : 0xFF2563EB;
            int dlgText = bright ? 0xFF0F172A : 0xFFE2E8F0;
            int dlgItemBg = bright ? 0xFFFCFDFE : 0xFF1E293B;
            int dlgHoverBg = bright ? 0xFFDBEAFE : 0xFF334155;
            int dlgSubText = bright ? 0xFF475569 : 0xFF94A3B8;

            // 半透明遮罩
            guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);
            // 对话框边框
            guiGraphics.fill(dlgX - 2, dlgY - 2, dlgX + dlgW + 2, dlgY + dlgH + 2, 0xFF000000);
            guiGraphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, dlgBg);
            guiGraphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 28, dlgTitleBg);
            // 标题
            guiGraphics.drawString(this.font, "选择要导入的文件（最新的在前）", dlgX + 12, dlgY + 10, 0xFFFFFFFF, true);

            // 关闭按钮 X（在标题栏右边）
            int closeX = dlgX + dlgW - 28;
            int closeY = dlgY + 6;
            guiGraphics.fill(closeX, closeY, closeX + 22, closeY + 16, 0xFFB91C1C);
            guiGraphics.drawString(this.font, "X", closeX + 8, closeY + 4, 0xFFFFFFFF, true);

            // 文件列表区
            int listX = dlgX + 10;
            int listY = dlgY + 38;
            int listW = dlgW - 20;
            int itemH = 44;
            int maxShown = Math.min(7, pendingImportFiles.length);

            int maxScroll = Math.max(0, pendingImportFiles.length - maxShown);
            if (importDialogScroll > maxScroll) importDialogScroll = maxScroll;
            if (importDialogScroll < 0) importDialogScroll = 0;

            for (int i = 0; i < maxShown; i++) {
                int fileIdx = importDialogScroll + i;
                if (fileIdx < 0 || fileIdx >= pendingImportFiles.length) break;
                java.io.File f = pendingImportFiles[fileIdx];
                int itemY = listY + i * itemH;
                boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= itemY && mouseY < itemY + itemH - 2;
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 2, hover ? dlgHoverBg : dlgItemBg);
                String fName = f.getName();
                guiGraphics.drawString(this.font, fName, listX + 8, itemY + 6, dlgText, true);
                long bytes = f.length();
                String sizeStr;
                if (bytes < 1024) sizeStr = bytes + " B";
                else if (bytes < 1024 * 1024) sizeStr = String.format("%.1f KB", bytes / 1024.0);
                else sizeStr = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateStr = sdf.format(new java.util.Date(f.lastModified()));
                guiGraphics.drawString(this.font, sizeStr + "   |   " + dateStr + "   |   " + f.getParent(), listX + 8, itemY + 22, dlgSubText, true);
            }
            // 更多项提示
            if (pendingImportFiles.length > maxShown) {
                int aX = listX + listW - 14;
                if (importDialogScroll > 0) guiGraphics.drawString(this.font, "\u2191", aX, listY - 14, dlgText, true);
                if (importDialogScroll < maxScroll) guiGraphics.drawString(this.font, "\u2193", aX, listY + maxShown * itemH - 12, dlgText, true);
            }
        }
    }

    // ========== advRawField 多行辅助：把文本按宽度拆成显示行，同时记录每行在原文本中的字符偏移 ==========
    // displayLines[i] = 第 i 个显示行的文本
    // charOffsets[i] = 该显示行第一个字符在原文本中的位置（含前面的 \n）
    private static class MultilineLayout {
        final java.util.List<String> displayLines;
        final java.util.List<Integer> charOffsets;
        MultilineLayout(java.util.List<String> l, java.util.List<Integer> o) { displayLines = l; charOffsets = o; }
    }
    private MultilineLayout layoutMultiline(String text, int maxWidth) {
        java.util.List<String> displayLines = new java.util.ArrayList<>();
        java.util.List<Integer> charOffsets = new java.util.ArrayList<>();
        if (text == null) text = "";
        String[] rawLines = text.split("\n", -1);
        int pos = 0;
        for (int ri = 0; ri < rawLines.length; ri++) {
            String raw = rawLines[ri];
            if (raw.isEmpty()) {
                displayLines.add("");
                charOffsets.add(pos);
                pos += 1; // 这个 \n 本身
                continue;
            }
            StringBuilder cur = new StringBuilder();
            int curW = 0;
            int lineStart = pos;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                int cw = this.font.width(String.valueOf(c));
                if (curW + cw > maxWidth && cur.length() > 0) {
                    displayLines.add(cur.toString());
                    charOffsets.add(lineStart);
                    cur = new StringBuilder();
                    curW = 0;
                    lineStart = pos + i;
                }
                cur.append(c);
                curW += cw;
            }
            displayLines.add(cur.toString());
            charOffsets.add(lineStart);
            pos += raw.length() + 1; // +1 for \n
        }
        return new MultilineLayout(displayLines, charOffsets);
    }

    // 计算 advRawField 中鼠标 (mx,my) 对应的字符位置（返回 0..text.length()）
    // 关键：使用与渲染完全相同的 layoutMultiline 逻辑 + advRawScrollOffset
    private int advRawFieldPosFromMouse(double mx, double my) {
        if (advRawField == null) return 0;
        // 使用数据区的实际显示坐标（多行内容从第二行开始，第一行是 advRawField 单行编辑框）
        int boxX = dataAreaX;
        int boxY = dataAreaY + 25;   // 与渲染一致：多行内容从第二行开始
        int boxW = dataAreaW;
        int boxH = dataAreaH - 25;   // 与渲染一致：高度相应减少
        int padX = 4;
        int padY = 4;
        int lineH = 10;
        String text = advRawField.getValue();
        if (text == null) text = "";
        int contentMaxW = boxW - padX * 2;
        int maxVisibleLines = (boxH - padY * 2) / lineH;

        // 关键：使用与渲染完全相同的 layoutMultiline 拆行
        MultilineLayout layout = layoutMultiline(text, contentMaxW);
        java.util.List<String> lines = layout.displayLines;
        java.util.List<Integer> offsets = layout.charOffsets;
        int totalLines = lines.size();

        // 关键：使用存储的 advRawScrollOffset（与渲染逻辑一致）
        int scrollOffset = advRawScrollOffset;
        int maxOffset = Math.max(0, totalLines - maxVisibleLines);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        // 鼠标在哪一行（相对于可见区域）
        int relY = (int) my - boxY - padY;
        if (relY < 0) relY = 0;
        // 点击在可见区域的哪一行
        int visibleLineIdx = relY / lineH;
        if (visibleLineIdx < 0) visibleLineIdx = 0;
        if (visibleLineIdx >= maxVisibleLines) visibleLineIdx = maxVisibleLines - 1;
        // 加上滚动偏移 → 实际显示行索引
        int lineIdx = visibleLineIdx + scrollOffset;
        if (lineIdx < 0) lineIdx = 0;

        PokemonEditorDebug.logEditor("[Mouse-Click] mx={} my={} box=[{},{},{},{}] relY={} visibleLine={} scrollOffset={} lineIdx={}/{} textLen={}",
                (int)mx, (int)my, boxX, boxY, boxW, boxH,
                relY, visibleLineIdx, scrollOffset, lineIdx, totalLines,
                text.length());

        if (lineIdx >= lines.size()) {
            PokemonEditorDebug.logEditor("[Mouse-Click] -> end: {}", text.length());
            return text.length();
        }
        // 在这一行内部，根据 X 坐标找字符位置
        int lineStart = offsets.get(lineIdx);
        String line = lines.get(lineIdx);
        int relX = (int) mx - boxX - padX;
        if (relX < 0) relX = 0;
        int accW = 0;
        int charInLine = 0;
        for (int i = 0; i < line.length(); i++) {
            int cw = this.font.width(String.valueOf(line.charAt(i)));
            if (accW + cw / 2 > relX) break;
            accW += cw;
            charInLine = i + 1;
        }
        int result = Math.min(text.length(), lineStart + charInLine);
        PokemonEditorDebug.logEditor("[Mouse-Click] -> lineStart={} charInLine={} result={}", lineStart, charInLine, result);
        return result;
    }

    // ========== 统一的滚动处理逻辑（被两个签名的 mouseScrolled 调用）==========
    private boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY, double delta, String source) {
        PokemonEditorDebug.log("[Scroll-" + source + "] mouseX=" + (int)mouseX + " mouseY=" + (int)mouseY +
                " scrollX=" + scrollX + " scrollY=" + scrollY + " delta=" + delta + " activeTab=" + activeTab);

        // 1. 导入对话框
        if (showImportDialog && pendingImportFiles != null) {
            int maxShown = Math.min(7, pendingImportFiles.length);
            int maxScroll = Math.max(0, pendingImportFiles.length - maxShown);
            // 使用 scrollY 的方向判断：scrollY > 0 向上滚，scrollY < 0 向下滚
            // 某些环境 delta 更可靠，所以两者都检查
            double effectiveScroll = (Math.abs(scrollY) > 0.01) ? scrollY : delta;
            if (effectiveScroll > 0) {
                importDialogScroll = Math.max(0, importDialogScroll - 1);
            } else if (effectiveScroll < 0) {
                importDialogScroll = Math.min(maxScroll, importDialogScroll + 1);
            }
            PokemonEditorDebug.log("[Scroll-ImportDialog] scroll=" + effectiveScroll + " newScroll=" + importDialogScroll + " max=" + maxScroll);
            return true;
        }
        // 2. Tab 0/1 下拉列表滚轮（物种/性格/特性/招式）
        if ((activeTab == 0 || activeTab == 1) && !autocompleteItems.isEmpty() && autocompleteStartY > 0) {
            PokemonEditorDebug.log("[Scroll-Dropdown] items=" + autocompleteItems.size() + " offset=" + autocompleteScrollOffset + " scrollY=" + scrollY + " delta=" + delta);
            int maxShown = 8;
            int maxOffset = Math.max(0, autocompleteItems.size() - maxShown);
            double effectiveScroll = (Math.abs(scrollY) > 0.01) ? scrollY : delta;
            if (effectiveScroll > 0) {
                autocompleteScrollOffset = Math.max(0, autocompleteScrollOffset - 2);
            } else if (effectiveScroll < 0) {
                autocompleteScrollOffset = Math.min(maxOffset, autocompleteScrollOffset + 2);
            }
            PokemonEditorDebug.log("[Scroll-Dropdown] new offset=" + autocompleteScrollOffset + " max=" + maxOffset);
            return true;
        }
        // 3. 数据区（advRawField）鼠标滚轮滚动
        if (activeTab == 3 && advRawField != null) {
            // 使用数据区的实际显示坐标（滚动检测覆盖整个数据区）
            int boxX = dataAreaX;
            int boxY = dataAreaY;
            int boxW = dataAreaW;
            int boxH = dataAreaH;
            // 但多行内容从第二行开始，计算可见行数时使用减少后的高度
            int contentBoxH = dataAreaH - 25;
            PokemonEditorDebug.logEditor("[Scroll-DataArea] check: mouseX={} mouseY={} box=[{},{},{},{}]",
                    (int)mouseX, (int)mouseY, boxX, boxY, boxW, boxH);
            if (mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY && mouseY < boxY + boxH) {
                // 使用共用的拆行逻辑，计算滚动后同步移动光标到可见区域
                int padY = 4;
                int lineH = 10;
                int contentMaxW = boxW - 8;
                int maxVisibleLines = (contentBoxH - padY * 2) / lineH;
                String text = advRawField.getValue();
                if (text == null) text = "";
                MultilineLayout layout = layoutMultiline(text, contentMaxW);
                java.util.List<Integer> charOffsets = layout.charOffsets;
                int totalLines = layout.displayLines.size();
                int maxOffset = Math.max(0, totalLines - maxVisibleLines);
                int scrollStep = 3;
                double effectiveScroll = (Math.abs(scrollY) > 0.01) ? scrollY : delta;
                if (effectiveScroll > 0) {
                    advRawScrollOffset = Math.max(0, advRawScrollOffset - scrollStep);
                    PokemonEditorDebug.log("[Scroll-DataArea] UP: offset=" + advRawScrollOffset + "/" + maxOffset);
                } else if (effectiveScroll < 0) {
                    advRawScrollOffset = Math.min(maxOffset, advRawScrollOffset + scrollStep);
                    PokemonEditorDebug.log("[Scroll-DataArea] DOWN: offset=" + advRawScrollOffset + "/" + maxOffset);
                }
                // 关键：同步光标到可见区域的第一行，这样 render 中的光标定位不会把你拉回去
                if (!charOffsets.isEmpty()) {
                    int targetLine = Math.min(advRawScrollOffset, charOffsets.size() - 1);
                    int charPos = charOffsets.get(targetLine);
                    advRawField.setCursorPosition(charPos);
                    advRawField.setHighlightPos(charPos);
                }
                return true;
            }
        }
        PokemonEditorDebug.log("[Scroll-" + source + "] not handled, passing to super");
        return false; // 交由各签名方法继续处理 super
    }

    // ========== NeoForge 1.21 鼠标滚轮事件（4 参数签名）==========
    // 注：编译确认 ContainerEventHandler 只定义了 4 参数版本
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean handled = handleMouseScroll(mouseX, mouseY, scrollX, scrollY, scrollY, "4param");
        if (handled) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ========== 搜索补全下拉：鼠标点击选择（根据当前激活字段写入对应输入框，支持滚动偏移）==========
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PokemonEditorDebug.logEditor("[Mouse-Click] entry activeTab={} mx={} my={} button={} advRawField={} dataArea=[{},{},{},{}]",
                activeTab, (int)mouseX, (int)mouseY, button,
                advRawField != null ? "non-null" : "null",
                dataAreaX, dataAreaY, dataAreaW, dataAreaH);
        // 导入对话框：打开时拦截所有点击
        if (showImportDialog && pendingImportFiles != null) {
            int panelW = 480;
            int panelH = 360;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;
            int listX = panelX + 10;
            int listY = panelY + 38;
            int listW = panelW - 20;
            int itemH = 44;
            int maxShown = Math.min(7, pendingImportFiles.length);
            int mx = (int) mouseX, my = (int) mouseY;
            // 关闭按钮 X
            int closeX = panelX + panelW - 28;
            int closeY = panelY + 6;
            if (mx >= closeX && mx < closeX + 22 && my >= closeY && my < closeY + 16) {
                showImportDialog = false;
                pendingImportFiles = null;
                return true;
            }
            // 点击文件项：载入
            for (int i = 0; i < maxShown; i++) {
                int fileIdx = importDialogScroll + i;
                if (fileIdx < 0 || fileIdx >= pendingImportFiles.length) break;
                int itemY = listY + i * itemH;
                if (mx >= listX && mx < listX + listW && my >= itemY && my < itemY + itemH - 2) {
                    try {
                        String text = java.nio.file.Files.readString(pendingImportFiles[fileIdx].toPath(), java.nio.charset.StandardCharsets.UTF_8);
                        advRawField.setValue(text);
                        advRawField.setCursorPosition(0);
                        advRawField.setHighlightPos(0);
                        advRawScrollOffset = 0;
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("已载入: " + pendingImportFiles[fileIdx].getName()));
                        }
                    } catch (Exception e) {
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("读取失败: " + e.getMessage()));
                        }
                    }
                    showImportDialog = false;
                    pendingImportFiles = null;
                    return true;
                }
            }
            // 对话框外点击 = 关闭
            if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
                showImportDialog = false;
                pendingImportFiles = null;
            }
            return true;
        }
        // advRawField 多行点击：自己算鼠标位置（使用数据区实际显示坐标）
        if (activeTab == 3 && advRawField != null && button == 0) {
            int boxX = dataAreaX;
            int boxY = dataAreaY;
            int boxW = dataAreaW;
            int boxH = dataAreaH;
            int mx = (int) mouseX, my = (int) mouseY;
            PokemonEditorDebug.logEditor("[Mouse-Click-Check] activeTab={} button={} mx={} my={} box=[{},{},{},{}] inBox={}",
                    activeTab, button, mx, my,
                    boxX, boxY, boxW, boxH,
                    mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH);
            if (mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH) {
                this.setFocused(advRawField);
                advRawField.setFocused(true);
                int pos = advRawFieldPosFromMouse(mouseX, mouseY);
                advRawField.setCursorPosition(pos);
                advRawField.setHighlightPos(pos); // 新点击 → 取消选择
                PokemonEditorDebug.logEditor("[Mouse-Click-Apply] focused=true cursor={}/{}", pos, advRawField.getValue().length());
                return true;
            }
        }
        if ((activeTab == 0 || activeTab == 1) && !autocompleteItems.isEmpty() && button == 0) {
            int lineH = 16;
            int maxShown = 8;
            int mx = (int) mouseX;
            int my = (int) mouseY;
            int listH = Math.min(autocompleteItems.size(), maxShown) * lineH;
            if (mx >= autocompleteX && mx < autocompleteX + autocompleteW
                    && my >= autocompleteStartY && my < autocompleteStartY + listH) {
                int idx = autocompleteScrollOffset + (my - autocompleteStartY) / lineH;
                if (idx >= 0 && idx < autocompleteItems.size()) {
                    String selected = autocompleteItems.get(idx);
                    if (autocompleteActiveField == 0) {
                        sumSpeciesField.setValue(selected);
                    } else if (autocompleteActiveField == 1) {
                        sumNatureField.setValue(selected);
                    } else if (autocompleteActiveField == 2) {
                        sumAbilityField.setValue(selected);
                    } else if (autocompleteActiveField == 3) {
                        move1Field.setValue(selected);
                    } else if (autocompleteActiveField == 4) {
                        move2Field.setValue(selected);
                    } else if (autocompleteActiveField == 5) {
                        move3Field.setValue(selected);
                    } else if (autocompleteActiveField == 6) {
                        move4Field.setValue(selected);
                    }
                    autocompleteItems.clear();
                    autocompleteScrollOffset = 0;
                    return true;
                }
            }
        }
        // ★ 金色王冠开关按钮点击检测
        if (selectedSlot >= 0 && crownVisible) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (mx >= crownToggleX && mx < crownToggleX + TOGGLE_SIZE
                    && my >= crownToggleY && my < crownToggleY + TOGGLE_SIZE) {
                PokemonData d = partyData.get(selectedSlot);
                d.goldenBottleCap = !d.goldenBottleCap;
                d.markModified("goldenBottleCap");
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // advRawField 鼠标拖动：按住左键移动 = 扩展选择区域
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeTab == 3 && advRawField != null && advRawField.isFocused() && button == 0) {
            int pos = advRawFieldPosFromMouse(mouseX, mouseY);
            advRawField.setCursorPosition(pos); // highlightPos 保持为鼠标按下时的位置 → 自然形成选择区
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        // 当数据区有焦点时，直接把字符输入传递给 advRawField（绕过 Screen 的默认逻辑）
        if (advRawField != null && advRawField.isFocused() && activeTab == 3) {
            boolean handled = advRawField.charTyped(ch, modifiers);
            if (handled) {
                PokemonEditorDebug.logEditor("[Char-Typed] ch='{}' handled=true textLen={}", ch, advRawField.getValue().length());
            }
            return handled;
        }
        return super.charTyped(ch, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PokemonEditorDebug.logEditor("[Key-Pressed] keyCode={} activeTab={} advRawFieldFocused={} advRawFieldValueLen={}",
                keyCode, activeTab,
                advRawField != null ? advRawField.isFocused() : "null",
                advRawField != null ? advRawField.getValue().length() : 0);
        // advRawField 多行编辑：完整的多行键盘支持
        if (advRawField != null && advRawField.isFocused() && activeTab == 3) {
            String current = advRawField.getValue();
            int cursor = advRawField.getCursorPosition();
            if (current == null) current = "";
            if (cursor < 0) cursor = 0;
            if (cursor > current.length()) cursor = current.length();

            // 1. Enter / 小键盘 Enter：插入换行符
            if (keyCode == 257 || keyCode == 335) {
                String before = current.substring(0, cursor);
                String after = current.substring(cursor);
                advRawField.setValue(before + "\n" + after);
                advRawField.setCursorPosition(cursor + 1);
                advRawField.setHighlightPos(cursor + 1);
                PokemonEditorDebug.logEditor("[Key-Enter] cursor={} newTextLen={}", cursor, before.length() + 1 + after.length());
                return true;
            }

            // 2. 上下箭头键：按显示行移动光标（而不是按字符）
            if (keyCode == 264 || keyCode == 265) { // 264=向下箭头, 265=向上箭头
                // 先计算当前光标所在的显示行和列
                int boxW = dataAreaW;
                int contentMaxW = boxW - 8; // padX * 2 = 8
                MultilineLayout layout = layoutMultiline(current, contentMaxW);
                java.util.List<String> lines = layout.displayLines;
                java.util.List<Integer> offsets = layout.charOffsets;
                if (lines.isEmpty()) return super.keyPressed(keyCode, scanCode, modifiers);

                // 找到当前光标在哪个显示行
                int curDisplayLine = 0;
                int curCol = 0;
                for (int li = 0; li < lines.size(); li++) {
                    int lineStart = offsets.get(li);
                    int lineEnd = (li + 1 < lines.size()) ? offsets.get(li + 1) : current.length();
                    if (cursor >= lineStart && cursor <= lineEnd) {
                        curDisplayLine = li;
                        curCol = cursor - lineStart;
                        break;
                    }
                }

                // 计算目标行
                int targetLine;
                if (keyCode == 265) { // 向上
                    targetLine = Math.max(0, curDisplayLine - 1);
                } else { // 向下
                    targetLine = Math.min(lines.size() - 1, curDisplayLine + 1);
                }

                // 移动光标到目标行的相同列
                if (targetLine != curDisplayLine) {
                    int targetLineStart = offsets.get(targetLine);
                    int targetLineLen = lines.get(targetLine).length();
                    int targetCol = Math.min(curCol, targetLineLen);
                    int newCursor = targetLineStart + targetCol;
                    advRawField.setCursorPosition(newCursor);
                    advRawField.setHighlightPos(newCursor);
                    // 同步滚动位置（确保光标可见，多行内容从第二行开始，高度减少25）
                    int maxVisibleLines = (dataAreaH - 25 - 8) / 10; // (contentBoxH - padY*2) / lineH
                    int maxOffset = Math.max(0, lines.size() - maxVisibleLines);
                    if (targetLine < advRawScrollOffset) {
                        advRawScrollOffset = Math.max(0, targetLine);
                    } else if (targetLine >= advRawScrollOffset + maxVisibleLines) {
                        advRawScrollOffset = Math.min(maxOffset, targetLine - maxVisibleLines + 1);
                    }
                    PokemonEditorDebug.logEditor("[Key-Arrow] {} curLine={} targetLine={} newCursor={} scroll={}",
                            keyCode == 265 ? "UP" : "DOWN",
                            curDisplayLine, targetLine, newCursor, advRawScrollOffset);
                }
                return true;
            }

            // 3. Home/End：跳转到当前显示行的开头/结尾
            if (keyCode == 268 || keyCode == 269) { // 268=Home, 269=End
                int boxW = dataAreaW;
                int contentMaxW = boxW - 8;
                MultilineLayout layout = layoutMultiline(current, contentMaxW);
                java.util.List<String> lines = layout.displayLines;
                java.util.List<Integer> offsets = layout.charOffsets;
                if (!lines.isEmpty()) {
                    // 找到当前光标所在显示行
                    int curDisplayLine = 0;
                    for (int li = 0; li < lines.size(); li++) {
                        int lineStart = offsets.get(li);
                        int lineEnd = (li + 1 < lines.size()) ? offsets.get(li + 1) : current.length();
                        if (cursor >= lineStart && cursor <= lineEnd) {
                            curDisplayLine = li;
                            break;
                        }
                    }
                    int newCursor;
                    if (keyCode == 268) { // Home
                        newCursor = offsets.get(curDisplayLine);
                    } else { // End
                        if (curDisplayLine + 1 < lines.size()) {
                            newCursor = offsets.get(curDisplayLine + 1);
                        } else {
                            newCursor = current.length();
                        }
                    }
                    advRawField.setCursorPosition(newCursor);
                    advRawField.setHighlightPos(newCursor);
                    PokemonEditorDebug.logEditor("[Key-HomeEnd] {} curLine={} newCursor={}",
                            keyCode == 268 ? "HOME" : "END",
                            curDisplayLine, newCursor);
                    return true;
                }
            }

            // 4. Ctrl+A：全选（EditBox 默认可能不支持多行全选）
            if (keyCode == 65 && (modifiers & 2) != 0) { // Ctrl+A
                advRawField.setCursorPosition(current.length());
                advRawField.setHighlightPos(0);
                PokemonEditorDebug.logEditor("[Key-CtrlA] selectAll textLen={}", current.length());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================
    // ★ 觉醒力量属性计算器 (Gen 2-7 公式)
    // 公式: typeIdx = floor(((ivHp%2) + 2*(ivAtk%2) + 4*(ivDef%2) + 8*(ivSpe%2) + 16*(ivSpA%2) + 32*(ivSpD%2)) * 15 / 63)
    // 结果索引 → cobblemon type ID 映射
    // =====================================================================
    private static final String[] HIDDEN_POWER_TYPE_TABLE = {
            "fighting",  // 0
            "flying",    // 1
            "poison",    // 2
            "ground",    // 3
            "rock",      // 4
            "bug",       // 5
            "ghost",     // 6
            "steel",     // 7
            "fire",      // 8
            "water",     // 9
            "grass",     // 10
            "electric",  // 11
            "psychic",   // 12
            "ice",       // 13
            "dragon",    // 14
            "dark"       // 15
    };

    private static final String[] HIDDEN_POWER_TYPE_ZH = {
            "格斗", "飞行", "毒", "地面", "岩石", "虫", "幽灵",
            "钢", "火", "水", "草", "电", "超能力", "冰", "龙", "恶"
    };

    private void refreshHiddenPower() {
        if (selectedSlot < 0) return;
        int hp  = parseOrDefault(ivHp, 0);
        int atk = parseOrDefault(ivAtk, 0);
        int def = parseOrDefault(ivDef, 0);
        int spa = parseOrDefault(ivSpA, 0);
        int spd = parseOrDefault(ivSpD, 0);
        int spe = parseOrDefault(ivSpe, 0);
        hiddenPowerType = calcHiddenPowerType(hp, atk, def, spa, spd, spe);
    }

    private String calcHiddenPowerType(int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe) {
        int raw = (ivHp & 1) + 2 * (ivAtk & 1) + 4 * (ivDef & 1) + 8 * (ivSpe & 1) + 16 * (ivSpA & 1) + 32 * (ivSpD & 1);
        int typeIdx = (raw * 15) / 63;
        if (typeIdx < 0) typeIdx = 0;
        if (typeIdx > 15) typeIdx = 15;
        return HIDDEN_POWER_TYPE_TABLE[typeIdx];
    }

    private String getHiddenPowerTypeZh() {
        if (hiddenPowerType == null || hiddenPowerType.isEmpty()) return "";
        for (int i = 0; i < HIDDEN_POWER_TYPE_TABLE.length; i++) {
            if (HIDDEN_POWER_TYPE_TABLE[i].equals(hiddenPowerType)) {
                return HIDDEN_POWER_TYPE_ZH[i];
            }
        }
        return hiddenPowerType;
    }
}