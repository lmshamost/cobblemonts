package com.cobblemon_ts.mod.pokemoneditor;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public class PokemonEditorScreen extends Screen {
    private final UUID targetPlayerUUID;
    private final String targetPlayerName;
    private final int partySize;
    private final List<PokemonData> partyData = new ArrayList();
    private final List<PokemonData> originalSnapshots = new ArrayList();
    private int selectedSlot = -1;
    private int activeTab = 0;
    private int pendingDeleteSlot = -1;
    private TextFieldWidget sumSpeciesField;
    private TextFieldWidget sumLevelField;
    private TextFieldWidget sumNameField;
    private TextFieldWidget sumNatureField;
    private TextFieldWidget sumAbilityField;
    private TextFieldWidget sumFriendshipField;
    private TextFieldWidget sumFormField;
    private TextFieldWidget sumHeldItemField;
    private TextFieldWidget genderField;
    private ButtonWidget sumShinyButton;
    private ButtonWidget clearSpeciesBtn;
    private ButtonWidget clearExpBtn;
    private ButtonWidget clearNameBtn;
    private ButtonWidget clearNatureBtn;
    private ButtonWidget clearAbilityBtn;
    private ButtonWidget clearFormBtn;
    private ButtonWidget clearHeldItemBtn;
    private ButtonWidget statusCycleBtn;
    private ButtonWidget levelDownBtn;
    private ButtonWidget levelUpBtn;
    private ButtonWidget friendshipDownBtn;
    private ButtonWidget friendshipUpBtn;
    private TextFieldWidget ivHp;
    private TextFieldWidget ivAtk;
    private TextFieldWidget ivDef;
    private TextFieldWidget ivSpA;
    private TextFieldWidget ivSpD;
    private TextFieldWidget ivSpe;
    private TextFieldWidget evHp;
    private TextFieldWidget evAtk;
    private TextFieldWidget evDef;
    private TextFieldWidget evSpA;
    private TextFieldWidget evSpD;
    private TextFieldWidget evSpe;
    private TextFieldWidget move1Field;
    private TextFieldWidget move2Field;
    private TextFieldWidget move3Field;
    private TextFieldWidget move4Field;
    private TextFieldWidget move1PPField;
    private TextFieldWidget move2PPField;
    private TextFieldWidget move3PPField;
    private TextFieldWidget move4PPField;
    private TextFieldWidget move1MaxPPField;
    private TextFieldWidget move2MaxPPField;
    private TextFieldWidget move3MaxPPField;
    private TextFieldWidget move4MaxPPField;
    private TextFieldWidget currentHpField;
    private TextFieldWidget maxHpField;
    // [线性-F01] 招式清除按钮：仅在对战页(tab==1)显示，点击清空对应招式输入框
    private ButtonWidget clearMove1Btn;
    private ButtonWidget clearMove2Btn;
    private ButtonWidget clearMove3Btn;
    private ButtonWidget clearMove4Btn;
    private TextFieldWidget advExpField;
    private TextFieldWidget advUUIDField;
    private TextFieldWidget advRawField;
    private int dataAreaX = 0;
    private int dataAreaY = 0;
    private int dataAreaW = 340;
    private int dataAreaH = 200;
    // [线性-C02] 界面尺寸缩放因子：GUI scale 3/4时缩放UI适配屏幕
    private float uiScaleFactor = 1.0f;
    private ButtonWidget saveButton;
    private ButtonWidget deleteButton;
    private ButtonWidget closeButton;
    private ButtonWidget tab0Btn;
    private ButtonWidget tab1Btn;
    private ButtonWidget tab2Btn;
    private ButtonWidget tab3Btn;
    private final ButtonWidget[] slotButtons = new ButtonWidget[6];
    private List<String> autocompleteItems = new ArrayList();
    private int autocompleteStartY = 0;
    private int autocompleteX = 0;
    private int autocompleteW = 0;
    private int autocompleteActiveField = -1;
    private int autocompleteScrollOffset = 0;
    private String lastSearchQuery = "";
    private ButtonWidget maxPPButton;
    private ButtonWidget advImportBtn;
    private ButtonWidget advExportBtn;
    private ButtonWidget advCopyBtn;
    private ButtonWidget advPasteBtn;
    private ButtonWidget advSaveBtn;
    private ButtonWidget advResetBtn;
    private boolean showImportDialog = false;
    private File[] pendingImportFiles = null;
    private int importDialogScroll = 0;
    private ButtonWidget teamAllBtn = null;
    private boolean viewingAllTeam = false;
    private int advRawScrollOffset = 0;
    private ButtonWidget genderCycleBtn;
    private ButtonWidget themeToggleBtn;
    private ButtonWidget editorAutoCloseBtn;
    private ButtonWidget maxIvButton;
    private ButtonWidget clearEvButton;
    private ButtonWidget fullEvButton;
    private ButtonWidget[] ivUpButtons = new ButtonWidget[6];
    private ButtonWidget[] ivDownButtons = new ButtonWidget[6];
    private ButtonWidget[] evUpButtons = new ButtonWidget[6];
    private ButtonWidget[] evDownButtons = new ButtonWidget[6];
    private int crownIconX;
    private int crownIconY;
    private int crownToggleX;
    private int crownToggleY;
    private boolean crownVisible = true;
    private static final int CROWN_SIZE = 20;
    private static final int TOGGLE_SIZE = 20;
    private ButtonWidget hiddenPowerBtn;
    private String hiddenPowerType = "";
    private static final String[][] STATUS_DISPLAY = new String[][]{
        {"", "cobblemon_ts.editor.status_none"},
        {"poison", "cobblemon_ts.editor.status_poison"},
        {"paralysis", "cobblemon_ts.editor.status_paralysis"},
        {"burn", "cobblemon_ts.editor.status_burn"},
        {"freeze", "cobblemon_ts.editor.status_freeze"},
        {"sleep", "cobblemon_ts.editor.status_sleep"},
        {"bad_poison", "cobblemon_ts.editor.status_bad_poison"}
    };
    private static final String[] HIDDEN_POWER_TYPE_TABLE = new String[]{"fighting", "flying", "poison", "ground", "rock", "bug", "ghost", "steel", "fire", "water", "grass", "electric", "psychic", "ice", "dragon", "dark"};
    private static final String[] HIDDEN_POWER_TYPE_ZH = new String[]{
        "cobblemon_ts.editor.hidden_power.fighting", "cobblemon_ts.editor.hidden_power.flying", "cobblemon_ts.editor.hidden_power.poison",
        "cobblemon_ts.editor.hidden_power.ground", "cobblemon_ts.editor.hidden_power.rock", "cobblemon_ts.editor.hidden_power.bug",
        "cobblemon_ts.editor.hidden_power.ghost", "cobblemon_ts.editor.hidden_power.steel", "cobblemon_ts.editor.hidden_power.fire",
        "cobblemon_ts.editor.hidden_power.water", "cobblemon_ts.editor.hidden_power.grass", "cobblemon_ts.editor.hidden_power.electric",
        "cobblemon_ts.editor.hidden_power.psychic", "cobblemon_ts.editor.hidden_power.ice", "cobblemon_ts.editor.hidden_power.dragon",
        "cobblemon_ts.editor.hidden_power.dark"
    };

    public PokemonEditorScreen(UUID targetPlayerUUID, String targetPlayerName, int partySize) {
        super(Text.translatable("cobblemon_ts.editor.title"));
        this.targetPlayerUUID = targetPlayerUUID;
        this.targetPlayerName = targetPlayerName;
        this.partySize = partySize;

        for(int i = 0; i < partySize; ++i) {
            this.partyData.add(new PokemonData());
            this.originalSnapshots.add(new PokemonData());
        }

    }

    public void updatePokemonData(PokemonEditorNetwork.PokemonDataPacket pkt) {
        if (pkt.slot() >= 0 && pkt.slot() < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(pkt.slot());
            d.clearModified();
            PokemonEditorDebug.infoMoves("[Screen] RECEIVED packet for slot {}: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})", new Object[]{pkt.slot(), pkt.move1(), pkt.move1PP(), pkt.move1MaxPP(), pkt.move2(), pkt.move2PP(), pkt.move2MaxPP(), pkt.move3(), pkt.move3PP(), pkt.move3MaxPP(), pkt.move4(), pkt.move4PP(), pkt.move4MaxPP()});
            d.exists = pkt.exists();
            d.pokemonUUID = pkt.pokemonUUID();
            d.speciesName = this.cleanName(pkt.speciesName());
            d.displayName = pkt.displayName();
            d.formName = this.cleanName(pkt.formName());
            d.level = pkt.level();
            d.experience = pkt.experience();
            d.friendship = pkt.friendship();
            d.shiny = pkt.shiny();
            d.goldenBottleCap = pkt.goldenBottleCap();
            d.ivHp = pkt.ivHp();
            d.ivAtk = pkt.ivAtk();
            d.ivDef = pkt.ivDef();
            d.ivSpA = pkt.ivSpA();
            d.ivSpD = pkt.ivSpD();
            d.ivSpe = pkt.ivSpe();
            d.evHp = pkt.evHp();
            d.evAtk = pkt.evAtk();
            d.evDef = pkt.evDef();
            d.evSpA = pkt.evSpA();
            d.evSpD = pkt.evSpD();
            d.evSpe = pkt.evSpe();
            d.nature = this.cleanName(pkt.nature());
            d.ability = this.cleanName(pkt.ability());
            String rawMove1 = pkt.move1();
            String cleanMove1 = this.cleanName(rawMove1);
            String rawMove2 = pkt.move2();
            String cleanMove2 = this.cleanName(rawMove2);
            String rawMove3 = pkt.move3();
            String cleanMove3 = this.cleanName(rawMove3);
            String rawMove4 = pkt.move4();
            String cleanMove4 = this.cleanName(rawMove4);
            PokemonEditorDebug.infoMoves("[Screen] move cleanName: slot0 raw='{}'->clean='{}' slot1 raw='{}'->clean='{}' slot2 raw='{}'->clean='{}' slot3 raw='{}'->clean='{}'", new Object[]{rawMove1, cleanMove1, rawMove2, cleanMove2, rawMove3, cleanMove3, rawMove4, cleanMove4});
            d.move1 = cleanMove1;
            d.move2 = cleanMove2;
            d.move3 = cleanMove3;
            d.move4 = cleanMove4;
            d.move1PP = pkt.move1PP();
            d.move2PP = pkt.move2PP();
            d.move3PP = pkt.move3PP();
            d.move4PP = pkt.move4PP();
            d.move1MaxPP = pkt.move1MaxPP();
            d.move2MaxPP = pkt.move2MaxPP();
            d.move3MaxPP = pkt.move3MaxPP();
            d.move4MaxPP = pkt.move4MaxPP();
            String[] storedMoves = new String[]{d.move1, d.move2, d.move3, d.move4};

            for(int i = 0; i < 4; ++i) {
                for(int j = i + 1; j < 4; ++j) {
                    if (storedMoves[i] != null && !storedMoves[i].isEmpty() && storedMoves[j] != null && storedMoves[i].equalsIgnoreCase(storedMoves[j])) {
                        PokemonEditorDebug.warnMoves("[Screen] RECEIVED DUPLICATE: slot{} and slot{} both='{}'", new Object[]{i, j, storedMoves[i]});
                    }
                }
            }

            d.currentHp = pkt.currentHp();
            d.maxHp = pkt.maxHp();
            d.heldItem = this.cleanName(pkt.heldItem());
            d.gender = pkt.gender();
            d.status = this.cleanName(pkt.status());
            d.possibleAbilities.clear();
            if (pkt.possibleAbilities() != null && !pkt.possibleAbilities().isEmpty()) {
                for(String ab : pkt.possibleAbilities().split(";")) {
                    if (ab != null && !ab.trim().isEmpty()) {
                        d.possibleAbilities.add(ab.trim());
                    }
                }
            }

            if (pkt.slot() == this.selectedSlot) {
                this.refreshEditFields();
            }

            if (pkt.slot() < 6 && this.slotButtons[pkt.slot()] != null) {
                this.updateSlotButtonLabel(pkt.slot());
            }

        }
    }

    private String cleanName(String s) {
        if (s != null && !s.isEmpty()) {
            s = s.replace("square_brackets", "").replace("args=", "").replace(",,", ",");
            int parenIdx = s.indexOf(40);
            if (parenIdx > 0) {
                s = s.substring(0, parenIdx);
            }

            int bracketIdx = s.indexOf(91);
            if (bracketIdx > 0) {
                s = s.substring(0, bracketIdx);
            }

            int atIdx = s.indexOf(64);
            if (atIdx > 0) {
                s = s.substring(0, atIdx);
            }

            int dollarIdx = s.indexOf(36);
            if (dollarIdx > 0) {
                s = s.substring(0, dollarIdx);
            }

            int colonIdx = s.lastIndexOf(58);
            if (colonIdx > 0 && colonIdx < s.length() - 1) {
                s = s.substring(colonIdx + 1);
            }

            int dotIdx = s.lastIndexOf(46);
            if (dotIdx > 0 && dotIdx < s.length() - 1) {
                s = s.substring(dotIdx + 1);
            }

            if (!s.toLowerCase().contains("class ") && !s.startsWith("com.") && !s.startsWith("net.") && !s.toLowerCase().contains("formdata") && !s.toLowerCase().contains("speciesdata") && !s.toLowerCase().contains("optional") && !s.toLowerCase().contains("holder")) {
                s = s.trim();
                if (s.length() >= 2 && (s.startsWith("'") && s.endsWith("'") || s.startsWith("\"") && s.endsWith("\""))) {
                    s = s.substring(1, s.length() - 1);
                }

                s = s.replaceAll(",\\s*,", ",").trim();
                if (s.endsWith(",")) {
                    s = s.substring(0, s.length() - 1);
                }

                return s.trim();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    protected void init() {
        this.clearChildren();
        int screenW = this.width;
        int screenH = this.height;

        // [线性-C02] 界面尺寸缩放：布局中心(layoutCenterX=320, layoutCenterY=233.5)，缩放后居中屏幕
        // 布局边界：左32(panelX-8) 右608(panelX+panelW+8) 上30(tabBarY) 下437(btnY+btnH)
        // 缩放公式：screenY = (logicalY - layoutCenterY) * scale + screenCenterY
        // 要求上下边界不超出屏幕，scale = min(screenW/layoutW, screenH/layoutH)
        // scale 2(960x540)→factor=1.0, scale 3(640x360)→factor≈0.82, scale 4(480x270)→factor≈0.62
        float layoutW = 576f; // 608-32
        float layoutH = 407f; // 437-30
        this.uiScaleFactor = Math.min(screenW / layoutW, screenH / layoutH);
        if (this.uiScaleFactor > 1.0f) this.uiScaleFactor = 1.0f;
        if (this.uiScaleFactor < 0.4f) this.uiScaleFactor = 0.4f; // 下限保护，防止过小
        int panelX = screenW / 2 - 280;
        int panelY = 60;
        int panelW = 560;
        int panelH = 340;
        int tabBarY = 30;
        int tabBarH = 22;
        int tabW = 140;
        this.tab0Btn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.tab.summary"), (btn) -> this.switchTab(0)).position(panelX, 30).size(140, 22).build();
        this.tab1Btn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.tab.battle"), (btn) -> this.switchTab(1)).position(panelX + 140, 30).size(140, 22).build();
        this.tab2Btn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.tab.stats"), (btn) -> this.switchTab(2)).position(panelX + 280, 30).size(140, 22).build();
        this.tab3Btn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.tab.advanced"), (btn) -> this.switchTab(3)).position(panelX + 420, 30).size(140, 22).build();
        this.addDrawableChild(this.tab0Btn);
        this.addDrawableChild(this.tab1Btn);
        this.addDrawableChild(this.tab2Btn);
        this.addDrawableChild(this.tab3Btn);
        int partyX = panelX + 10;
        int partyY = 70;
        int partyW = 140;
        int slotH = 20;
        int slotGap = 2;

        for(int i = 0; i < Math.min(6, this.partySize); ++i) {
            int slotIdx = i;
            this.slotButtons[i] = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.slot_empty", i + 1), (btn) -> this.selectSlot(slotIdx)).position(partyX, 70 + i * 22).size(140, 20).build();
            this.addDrawableChild(this.slotButtons[i]);
        }

        int lastSlotY = 70 + Math.min(6, this.partySize) * 22;
        this.teamAllBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.team_all"), (btn) -> {
            this.viewingAllTeam = true;
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < this.partyData.size(); ++i) {
                if (i > 0) {
                    sb.append("\n\n");
                }

                sb.append("# ====== \u961f").append(i + 1).append(" ======\n");
                sb.append(this.buildDataString((PokemonData)this.partyData.get(i)));
            }

            this.advRawField.setText(sb.toString());
            ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(0);
            this.advRawField.setSelectionEnd(0);
            this.advRawScrollOffset = 0;
        }).position(partyX, lastSlotY).size(140, 20).build();
        this.teamAllBtn.visible = false;
        this.addDrawableChild(this.teamAllBtn);
        int editX = panelX + 165;
        int editY = 70;
        int editW = 385;
        int labelW = 50;
        int rowY = 88;
        int rowStep = 28;
        this.sumSpeciesField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY, 100, 16, Text.literal(""));
        this.sumSpeciesField.setMaxLength(40);
        this.clearSpeciesBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumSpeciesField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY - 4).size(16, 16).build();
        this.sumLevelField = this.makeIntBox(editX + 50 + 10, rowY + rowStep, 100, 1, 900);
        this.levelDownBtn = ButtonWidget.builder(Text.literal("\u2b07"), (btn) -> {
            boolean shift = Screen.hasShiftDown();
            boolean ctrl = Screen.hasControlDown();
            int cur = this.parseOrDefault(this.sumLevelField, 1);
            int step = 1;
            if (shift && ctrl) {
                step = 900;
            } else if (ctrl) {
                step = 100;
            } else if (shift) {
                step = 10;
            }

            int n = Math.max(1, cur - step);
            this.sumLevelField.setText(String.valueOf(n));
        }).position(editX + 50 + 10 + 100 + 4, rowY + rowStep).size(18, 18).build();
        this.levelUpBtn = ButtonWidget.builder(Text.literal("\u2b06"), (btn) -> {
            boolean shift = Screen.hasShiftDown();
            boolean ctrl = Screen.hasControlDown();
            int cur = this.parseOrDefault(this.sumLevelField, 1);
            int step = 1;
            if (shift && ctrl) {
                step = 900;
            } else if (ctrl) {
                step = 100;
            } else if (shift) {
                step = 10;
            }

            int n = Math.min(900, cur + step);
            this.sumLevelField.setText(String.valueOf(n));
        }).position(editX + 50 + 10 + 100 + 4 + 18 + 2, rowY + rowStep).size(18, 18).build();
        this.sumNameField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY + rowStep * 2, 100, 16, Text.literal(""));
        this.sumNameField.setMaxLength(30);
        this.clearNameBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumNameField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 2 - 4).size(16, 16).build();
        this.sumNatureField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY + rowStep * 3, 100, 16, Text.literal(""));
        this.sumNatureField.setMaxLength(20);
        this.clearNatureBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumNatureField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 3 - 4).size(16, 16).build();
        this.sumAbilityField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY + rowStep * 4, 100, 16, Text.literal(""));
        this.sumAbilityField.setMaxLength(30);
        this.clearAbilityBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumAbilityField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 4 - 4).size(16, 16).build();
        this.sumFriendshipField = this.makeIntBox(editX + 50 + 10, rowY + rowStep * 5, 100, 0, 255);
        this.friendshipDownBtn = ButtonWidget.builder(Text.literal("\u2b07"), (btn) -> {
            boolean shift = Screen.hasShiftDown();
            boolean ctrl = Screen.hasControlDown();
            int cur = this.parseOrDefault(this.sumFriendshipField, 0);
            int step = 1;
            if (shift && ctrl) {
                step = 255;
            } else if (ctrl) {
                step = 100;
            } else if (shift) {
                step = 10;
            }

            int n = Math.max(0, cur - step);
            this.sumFriendshipField.setText(String.valueOf(n));
        }).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 5).size(18, 18).build();
        this.friendshipUpBtn = ButtonWidget.builder(Text.literal("\u2b06"), (btn) -> {
            boolean shift = Screen.hasShiftDown();
            boolean ctrl = Screen.hasControlDown();
            int cur = this.parseOrDefault(this.sumFriendshipField, 0);
            int step = 1;
            if (shift && ctrl) {
                step = 255;
            } else if (ctrl) {
                step = 100;
            } else if (shift) {
                step = 10;
            }

            int n = Math.min(255, cur + step);
            this.sumFriendshipField.setText(String.valueOf(n));
        }).position(editX + 50 + 10 + 100 + 4 + 18 + 2, rowY + rowStep * 5).size(18, 18).build();
        this.sumFormField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY + rowStep * 6, 100, 16, Text.literal(""));
        this.sumFormField.setMaxLength(40);
        this.clearFormBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumFormField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 6 - 4).size(16, 16).build();
        this.sumHeldItemField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, rowY + rowStep * 7, 100, 16, Text.literal(""));
        this.sumHeldItemField.setMaxLength(50);
        this.clearHeldItemBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.sumHeldItemField.setText("")).position(editX + 50 + 10 + 100 + 4, rowY + rowStep * 7 - 4).size(16, 16).build();
        this.genderField = this.makeIntBox(editX + 50 + 10, rowY + rowStep * 8, 60, 0, 2);
        this.genderCycleBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.gender_cycle"), (btn) -> {
            int cur = this.parseOrDefault(this.genderField, 2);
            int next = (cur + 1) % 3;
            this.genderField.setText(String.valueOf(next));
        }).position(editX + 50 + 10 + 65, rowY + rowStep * 8).size(90, 18).build();
        this.sumShinyButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.shiny_off"), (btn) -> {
            if (this.selectedSlot >= 0) {
                ((PokemonData)this.partyData.get(this.selectedSlot)).shiny = !((PokemonData)this.partyData.get(this.selectedSlot)).shiny;
                this.updateShinyButton();
            }

        }).position(editX + 50 + 10, rowY + rowStep * 9).size(110, 18).build();
        this.statusCycleBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.status_none"), (btn) -> {
            if (this.selectedSlot >= 0) {
                this.cycleToNextStatus();
            }

        }).position(editX + 50 + 10 + 115, rowY + rowStep * 9).size(160, 18).build();
        int ivStartY = 110;
        int statRowH = 28;
        int ivLabelW = 35;
        int valueW = 60;
        int arrowW = 18;
        int arrowH = 18;
        int ivColX = editX + 10;
        this.ivHp = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 0, valueW, 0, 31);
        this.ivAtk = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 1, valueW, 0, 31);
        this.ivDef = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 2, valueW, 0, 31);
        this.ivSpA = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 3, valueW, 0, 31);
        this.ivSpD = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 4, valueW, 0, 31);
        this.ivSpe = this.makeIntBox(ivColX + ivLabelW + 10, ivStartY + statRowH * 5, valueW, 0, 31);
        this.ivHp.setChangedListener((s) -> this.refreshHiddenPower());
        this.ivAtk.setChangedListener((s) -> this.refreshHiddenPower());
        this.ivDef.setChangedListener((s) -> this.refreshHiddenPower());
        this.ivSpA.setChangedListener((s) -> this.refreshHiddenPower());
        this.ivSpD.setChangedListener((s) -> this.refreshHiddenPower());
        this.ivSpe.setChangedListener((s) -> this.refreshHiddenPower());
        TextFieldWidget[] ivBoxes = new TextFieldWidget[]{this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe};

        for(int i = 0; i < 6; ++i) {
            int ivIdx = i;
            int arrowBaseX = ivColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            this.ivDownButtons[i] = ButtonWidget.builder(Text.literal("\u2b07"), (btn) -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = this.parseOrDefault(ivBoxes[ivIdx], 0);
                int step = 1;
                if (shift && ctrl) {
                    step = 31;
                } else if (ctrl) {
                    step = 31;
                } else if (shift) {
                    step = 10;
                }

                int n = Math.max(0, cur - step);
                ivBoxes[ivIdx].setText(String.valueOf(n));
            }).position(arrowBaseX, arrowBaseY).size(arrowW, arrowH).build();
            this.ivUpButtons[i] = ButtonWidget.builder(Text.literal("\u2b06"), (btn) -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = this.parseOrDefault(ivBoxes[ivIdx], 0);
                int step = 1;
                if (shift && ctrl) {
                    step = 31;
                } else if (ctrl) {
                    step = 31;
                } else if (shift) {
                    step = 10;
                }

                int n = Math.min(31, cur + step);
                ivBoxes[ivIdx].setText(String.valueOf(n));
            }).position(arrowBaseX + arrowW + 2, arrowBaseY).size(arrowW, arrowH).build();
        }

        this.maxIvButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.max_iv"), (btn) -> {
            for(TextFieldWidget b : ivBoxes) {
                b.setText("31");
            }

        }).position(ivColX + 5, ivStartY + statRowH * 6 + 16).size(140, 22).build();
        int ivTitleW = this.textRenderer.getWidth("IV (0-31)");
        this.crownIconX = ivColX + 60 + ivTitleW / 2 + 5;
        this.crownIconY = 81;
        this.crownToggleX = this.crownIconX + 20 + 4;
        this.crownToggleY = this.crownIconY;
        int hpCalcY = ivStartY + statRowH * 6 + 16 + 28;
        this.hiddenPowerBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.hidden_power"), (btn) -> {
            if (this.selectedSlot >= 0) {
                int hp = this.parseOrDefault(this.ivHp, 0);
                int atk = this.parseOrDefault(this.ivAtk, 0);
                int def = this.parseOrDefault(this.ivDef, 0);
                int spa = this.parseOrDefault(this.ivSpA, 0);
                int spd = this.parseOrDefault(this.ivSpD, 0);
                int spe = this.parseOrDefault(this.ivSpe, 0);
                this.hiddenPowerType = this.calcHiddenPowerType(hp, atk, def, spa, spd, spe);
            }
        }).position(ivColX + 5, hpCalcY).size(80, 20).build();
        int evColX = editX + 180;
        this.evHp = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 0, valueW, 0, 252);
        this.evAtk = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 1, valueW, 0, 252);
        this.evDef = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 2, valueW, 0, 252);
        this.evSpA = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 3, valueW, 0, 252);
        this.evSpD = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 4, valueW, 0, 252);
        this.evSpe = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 5, valueW, 0, 252);
        
        // [优化-L4] EV输入框添加总和限制响应器
        TextFieldWidget[] evBoxes = new TextFieldWidget[]{this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe};
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            evBoxes[i].setChangedListener(s -> {
                // 只在输入完成时校验（避免输入过程中频繁限制）
                if (s.isEmpty() || s.matches("\\d+")) {
                    int total = 0;
                    for (int j = 0; j < 6; j++) {
                        total += this.parseOrDefault(evBoxes[j], 0);
                    }
                    int maxCap = CobblemonTSConfig.getEVTotalCap();
                    if (total > maxCap) {
                        // 超出总和时，将当前输入框值调整到不超出上限
                        int over = total - maxCap;
                        int current = this.parseOrDefault(evBoxes[idx], 0);
                        int newVal = Math.max(0, current - over);
                        evBoxes[idx].setText(String.valueOf(newVal));
                        PokemonEditorDebug.logEditor("EV总和超出上限: " + maxCap + "，自动调整值为: " + newVal);
                    }
                }
            });
        }

        for(int i = 0; i < 6; ++i) {
            int evIdx = i;
            int arrowBaseX = evColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            this.evDownButtons[i] = ButtonWidget.builder(Text.literal("\u2b07"), (btn) -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = this.parseOrDefault(evBoxes[evIdx], 0);
                int step = 1;
                if (shift && ctrl) {
                    step = 255;
                } else if (ctrl) {
                    step = 100;
                } else if (shift) {
                    step = 10;
                }

                int n = Math.max(0, cur - step);
                evBoxes[evIdx].setText(String.valueOf(n));
            }).position(arrowBaseX, arrowBaseY).size(arrowW, arrowH).build();
            this.evUpButtons[i] = ButtonWidget.builder(Text.literal("\u2b06"), (btn) -> {
                boolean shift = Screen.hasShiftDown();
                boolean ctrl = Screen.hasControlDown();
                int cur = this.parseOrDefault(evBoxes[evIdx], 0);
                int total = 0;

                for(int j = 0; j < 6; ++j) {
                    total += this.parseOrDefault(evBoxes[j], 0);
                }

                int otherSum = total - cur;
                int maxCanAdd = Math.min(252, CobblemonTSConfig.getEVTotalCap() - otherSum);
                int step = 1;
                if (shift && ctrl) {
                    step = 252;
                } else if (ctrl) {
                    step = 100;
                } else if (shift) {
                    step = 10;
                }

                int n = Math.min(maxCanAdd, cur + step);
                if (n < 0) {
                    n = 0;
                }

                evBoxes[evIdx].setText(String.valueOf(n));
            }).position(arrowBaseX + arrowW + 2, arrowBaseY).size(arrowW, arrowH).build();
        }

        this.clearEvButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.clear_ev"), (btn) -> {
            for(TextFieldWidget b : evBoxes) {
                b.setText("0");
            }

        }).position(evColX + 5, ivStartY + statRowH * 6 + 16).size(140, 22).build();
        boolean ev252Enabled = Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get());
        if (ev252Enabled) {
            this.fullEvButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.full_ev"), (btn) -> {
                for(TextFieldWidget b : evBoxes) {
                    b.setText("252");
                }

            }).position(evColX + 5, ivStartY + statRowH * 6 + 16 + 28).size(140, 22).build();
        }

        int battleY = 85;
        this.currentHpField = this.makeIntBox(editX + 50 + 10, battleY, 100, 0, 9999);
        this.maxHpField = this.makeIntBox(editX + 50 + 10, battleY + 30, 100, 1, 9999);
        int moveY = battleY + 70;
        int moveLabelW = 50;
        int moveW = 90;
        int ppW = 50;
        int moveFieldX = editX + moveLabelW + 5;
        this.move1Field = new TextFieldWidget(this.textRenderer, moveFieldX, moveY, moveW, 16, Text.literal(""));
        this.move1Field.setMaxLength(30);
        this.move2Field = new TextFieldWidget(this.textRenderer, moveFieldX, moveY + 25, moveW, 16, Text.literal(""));
        this.move2Field.setMaxLength(30);
        this.move3Field = new TextFieldWidget(this.textRenderer, moveFieldX, moveY + 50, moveW, 16, Text.literal(""));
        this.move3Field.setMaxLength(30);
        this.move4Field = new TextFieldWidget(this.textRenderer, moveFieldX, moveY + 75, moveW, 16, Text.literal(""));
        this.move4Field.setMaxLength(30);
        // [线性-F01] 创建招式清除按钮（×），紧贴输入框右侧，Y坐标与背景框底部对齐
        int clearBtnX = moveFieldX + moveW + 2;
        this.clearMove1Btn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.move1Field.setText("")).position(clearBtnX, moveY - 4).size(16, 16).build();
        this.clearMove2Btn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.move2Field.setText("")).position(clearBtnX, moveY + 25 - 4).size(16, 16).build();
        this.clearMove3Btn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.move3Field.setText("")).position(clearBtnX, moveY + 50 - 4).size(16, 16).build();
        this.clearMove4Btn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.move4Field.setText("")).position(clearBtnX, moveY + 75 - 4).size(16, 16).build();
        int ppFieldX = clearBtnX + 16 + 8;
        int maxPPFieldX = ppFieldX + ppW + 8;
        this.move1PPField = this.makeIntBox(ppFieldX, moveY, ppW, 0, 999);
        this.move2PPField = this.makeIntBox(ppFieldX, moveY + 25, ppW, 0, 999);
        this.move3PPField = this.makeIntBox(ppFieldX, moveY + 50, ppW, 0, 999);
        this.move4PPField = this.makeIntBox(ppFieldX, moveY + 75, ppW, 0, 999);
        this.move1MaxPPField = this.makeIntBox(maxPPFieldX, moveY, ppW, 0, 999);
        this.move2MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 25, ppW, 0, 999);
        this.move3MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 50, ppW, 0, 999);
        this.move4MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 75, ppW, 0, 999);
        this.move1MaxPPField.setEditable(true);
        this.move2MaxPPField.setEditable(true);
        this.move3MaxPPField.setEditable(true);
        this.move4MaxPPField.setEditable(true);
        this.maxPPButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.max_pp"), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                String[] names = new String[]{d.move1, d.move2, d.move3, d.move4};
                int[] maxPPs = new int[4];

                for(int i = 0; i < 4; ++i) {
                    int basePP = CobblemonBridge.getBasePP(names[i]);
                    int raisedLimit = basePP > 0 ? (int)Math.floor((double)basePP * 1.6) : 0;
                    if (raisedLimit <= 0) {
                        int[] ms = new int[]{d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP};
                        raisedLimit = ms[i] > 0 ? ms[i] : 0;
                    }

                    maxPPs[i] = raisedLimit;
                }

                if (maxPPs[0] > 0) {
                    d.move1MaxPP = maxPPs[0];
                }

                if (maxPPs[1] > 0) {
                    d.move2MaxPP = maxPPs[1];
                }

                if (maxPPs[2] > 0) {
                    d.move3MaxPP = maxPPs[2];
                }

                if (maxPPs[3] > 0) {
                    d.move4MaxPP = maxPPs[3];
                }

                if (maxPPs[0] > 0) {
                    this.move1MaxPPField.setText(String.valueOf(maxPPs[0]));
                }

                if (maxPPs[1] > 0) {
                    this.move2MaxPPField.setText(String.valueOf(maxPPs[1]));
                }

                if (maxPPs[2] > 0) {
                    this.move3MaxPPField.setText(String.valueOf(maxPPs[2]));
                }

                if (maxPPs[3] > 0) {
                    this.move4MaxPPField.setText(String.valueOf(maxPPs[3]));
                }

            }
        }).position(maxPPFieldX, moveY + 100).size(ppW, 22).build();
        int advY = 85;
        this.advExpField = this.makeIntBox(editX + 50 + 10, advY, 100, 0, 2147483647);
        this.clearExpBtn = ButtonWidget.builder(Text.literal("\u00d7"), (btn) -> this.advExpField.setText("0")).position(editX + 50 + 10 + 100 + 4, advY - 4).size(16, 16).build();
        this.advUUIDField = new TextFieldWidget(this.textRenderer, editX + 50 + 10, advY + 35, 280, 16, Text.literal(""));
        this.advUUIDField.setMaxLength(60);
        this.advUUIDField.setEditable(true);
        this.dataAreaX = editX + 10;
        this.dataAreaY = advY + 80;
        this.dataAreaW = 340;
        this.dataAreaH = 200;
        this.advRawField = new TextFieldWidget(this.textRenderer, this.dataAreaX, this.dataAreaY, this.dataAreaW, 22, Text.literal(""));
        this.advRawField.setMaxLength(20000);
        this.advRawField.setEditable(true);
        this.advRawField.visible = true;
        this.advRawField.setEditableColor(16777215);
        int rightBtnX = editX + 360;
        int rightBtnY = advY + 80;
        int rightBtnW = 80;
        int rightBtnH = 22;
        int rightBtnGap = 8;
        this.advCopyBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.copy"), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                String data = this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot));
                this.client.keyboard.setClipboard(data);
            }

        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 0).size(rightBtnW, rightBtnH).build();
        this.advPasteBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.paste"), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                String text = this.client.keyboard.getClipboard();
                this.applyDataString((PokemonData)this.partyData.get(this.selectedSlot), text);
                this.refreshEditFields();
                if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                    this.advRawField.setText(this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot)));
                    ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(0);
                    this.advRawField.setSelectionEnd(0);
                    this.advRawScrollOffset = 0;
                }
            }

        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 1).size(rightBtnW, rightBtnH).build();
        this.advExportBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.export"), (btn) -> {
            String content = this.advRawField.getText();
            if (content != null && !content.trim().isEmpty()) {
                try {
                    File folder = new File(this.client.runDirectory, "pokemon_exports");
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                    String fileName = "pokemon_export_" + System.currentTimeMillis() + ".txt";
                    File file = new File(folder, fileName);
                    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
                    if (this.client.player != null) {
                        this.client.player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.exported", file.getAbsolutePath()));
                    }
                } catch (Exception e) {
                    if (this.client.player != null) {
                        this.client.player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.export_failed", e.getMessage()));
                    }
                }

            }
        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 2).size(rightBtnW, rightBtnH).build();
        this.advImportBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.import"), (btn) -> {
            File folder = new File(this.client.runDirectory, "pokemon_exports");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File[] files = folder.listFiles((dir, name) -> name.startsWith("pokemon_export_") && name.endsWith(".txt"));
            if (files != null && files.length != 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                this.pendingImportFiles = files;
                this.showImportDialog = true;
            } else {
                if (this.client.player != null) {
                    this.client.player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.no_exports"));
                }

            }
        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 3).size(rightBtnW, rightBtnH).build();
        this.advSaveBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.save"), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                this.applyDataString(d, this.advRawField.getText());
                this.savePokemonData(d);
            }

        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 4).size(rightBtnW, rightBtnH).build();
        this.advResetBtn = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.reset"), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                this.advRawField.setText(this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot)));
                ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(0);
                this.advRawField.setSelectionEnd(0);
                this.advRawScrollOffset = 0;
            }

        }).position(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 5).size(rightBtnW, rightBtnH).build();

        for(TextFieldWidget b : new TextFieldWidget[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
            this.addDrawableChild(b);
        }

        this.addDrawableChild(this.sumShinyButton);
        this.addDrawableChild(this.clearSpeciesBtn);
        this.addDrawableChild(this.clearNameBtn);
        this.addDrawableChild(this.clearNatureBtn);
        this.addDrawableChild(this.clearAbilityBtn);
        this.addDrawableChild(this.clearFormBtn);
        this.addDrawableChild(this.clearHeldItemBtn);
        this.addDrawableChild(this.statusCycleBtn);
        this.addDrawableChild(this.genderCycleBtn);
        this.addDrawableChild(this.levelDownBtn);
        this.addDrawableChild(this.levelUpBtn);
        this.addDrawableChild(this.friendshipDownBtn);
        this.addDrawableChild(this.friendshipUpBtn);
        this.addDrawableChild(this.maxIvButton);
        this.addDrawableChild(this.clearEvButton);
        if (this.fullEvButton != null) {
            this.addDrawableChild(this.fullEvButton);
        }

        this.addDrawableChild(this.hiddenPowerBtn);

        for(int i = 0; i < 6; ++i) {
            if (this.ivUpButtons[i] != null) {
                this.addDrawableChild(this.ivUpButtons[i]);
            }

            if (this.ivDownButtons[i] != null) {
                this.addDrawableChild(this.ivDownButtons[i]);
            }

            if (this.evUpButtons[i] != null) {
                this.addDrawableChild(this.evUpButtons[i]);
            }

            if (this.evDownButtons[i] != null) {
                this.addDrawableChild(this.evDownButtons[i]);
            }
        }

        this.addDrawableChild(this.advCopyBtn);
        this.addDrawableChild(this.advPasteBtn);
        this.addDrawableChild(this.advExportBtn);
        this.addDrawableChild(this.advImportBtn);
        this.addDrawableChild(this.advSaveBtn);
        this.addDrawableChild(this.advResetBtn);
        this.addDrawableChild(this.clearExpBtn);
        this.addDrawableChild(this.maxPPButton);
        // [线性-F01] 添加招式清除按钮到渲染列表
        this.addDrawableChild(this.clearMove1Btn);
        this.addDrawableChild(this.clearMove2Btn);
        this.addDrawableChild(this.clearMove3Btn);
        this.addDrawableChild(this.clearMove4Btn);
        int btnY = 415;
        int btnW = 120;
        int btnH = 22;
        int btnGap = 20;
        int totalBtnW = btnW * 4 + btnGap * 3;
        int btnStartX = panelX + 280 - totalBtnW / 2;
        boolean autoClose = (Boolean)CharmConfig.EDITOR_AUTO_CLOSE.get();
        this.editorAutoCloseBtn = ButtonWidget.builder(autoClose ? Text.translatable("cobblemon_ts.editor.auto_close_on").formatted(autoClose ? Formatting.GREEN : Formatting.RED) : Text.translatable("cobblemon_ts.editor.auto_close_off").formatted(autoClose ? Formatting.GREEN : Formatting.RED), (btn) -> {
            boolean newVal = !(Boolean)CharmConfig.EDITOR_AUTO_CLOSE.get();
            CharmConfig.EDITOR_AUTO_CLOSE.set(newVal);
            // CharmConfig.EDITOR_AUTO_CLOSE.toNbt();
            btn.setMessage(newVal ? Text.translatable("cobblemon_ts.editor.auto_close_on").formatted(Formatting.GREEN) : Text.translatable("cobblemon_ts.editor.auto_close_off").formatted(Formatting.RED));
        }).position(btnStartX + btnW - 26, btnY - 26).size(60, 20).build();
        this.themeToggleBtn = ButtonWidget.builder((Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? Text.translatable("cobblemon_ts.editor.theme_light") : Text.translatable("cobblemon_ts.editor.theme_dark"), (btn) -> {
            boolean newVal = !(Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
            CobblemonTSConfig.LIGHT_EDITOR_THEME.set(newVal);
            // CobblemonTSConfig.LIGHT_EDITOR_THEME.toNbt(); // [YARN1.21] toNbt not in BooleanValue, save handled by CobblemonTSConfig.save()
            btn.setMessage(newVal ? Text.translatable("cobblemon_ts.editor.theme_light") : Text.translatable("cobblemon_ts.editor.theme_dark"));
        }).position(btnStartX, btnY).size(btnW, btnH).build();
        this.saveButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.save"), (btn) -> this.saveChanges()).position(btnStartX + btnW + btnGap, btnY).size(btnW, btnH).build();
        this.deleteButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.delete"), (btn) -> this.deletePokemon()).position(btnStartX + btnW * 2 + btnGap * 2, btnY).size(btnW, btnH).build();
        this.closeButton = ButtonWidget.builder(Text.translatable("cobblemon_ts.editor.close"), (btn) -> this.close()).position(btnStartX + btnW * 3 + btnGap * 3, btnY).size(btnW, btnH).build();
        this.addDrawableChild(this.editorAutoCloseBtn);
        this.addDrawableChild(this.themeToggleBtn);
        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.deleteButton);
        this.addDrawableChild(this.closeButton);
        this.switchTab(this.activeTab);
        if (this.partySize > 0) {
            this.selectSlot(0);
        }

    }

    private TextFieldWidget makeIntBox(int x, int y, int w, int min, int max) {
        TextFieldWidget box = new TextFieldWidget(this.textRenderer, x, y, w, 16, Text.literal(""));
        box.setMaxLength(String.valueOf(max).length());
        box.setTextPredicate((s) -> s.isEmpty() || s.matches("-?\\d+"));
        return box;
    }

    private void drawInputFieldBackgrounds(DrawContext DrawContext, boolean bright) {
        int boxBg = bright ? ColorEditorConfig.editorLightInputBg() : ColorEditorConfig.editorDarkInputBg();
        int boxFrame = bright ? ColorEditorConfig.editorLightInputBorder() : ColorEditorConfig.editorDarkInputBorder();
        int boxTopFocus = bright ? ColorEditorConfig.editorLightTabActive() : ColorEditorConfig.editorDarkTabActive();
        int textColor = bright ? ColorEditorConfig.editorLightInputText() : ColorEditorConfig.editorDarkInputText();

        for(TextFieldWidget b : new TextFieldWidget[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
            if (b != null && b.visible) {
                // [线性-C04] setDrawsBackground(false)去除EditBox自带黑色背景(0xFF000000)
                // bordered时文字XY=(x+4, y+(h-8)/2)，unbordered时文字XY=(x, y)
                // 不修改EditBox位置(避免累加)，改为偏移背景绘制让文字居中于背景框内
                b.setDrawsBackground(false);
                b.setEditableColor(textColor);
                int bx = b.getX();
                int by = b.getY();
                int bw = b.getWidth();
                int bh = b.getHeight();
                int textOffsetY = (bh - 8) / 2; // 垂直居中补偿
                int textOffsetX = 4;            // 水平内边距补偿
                int bgX = bx - textOffsetX;
                int bgY = by - textOffsetY;
                DrawContext.fill(bgX, bgY, bgX + bw, bgY + bh, boxBg);
                DrawContext.fill(bgX, bgY, bgX + bw, bgY + 1, boxFrame);
                DrawContext.fill(bgX, bgY + bh - 1, bgX + bw, bgY + bh, boxFrame);
                DrawContext.fill(bgX, bgY, bgX + 1, bgY + bh, boxFrame);
                DrawContext.fill(bgX + bw - 1, bgY, bgX + bw, bgY + bh, boxFrame);
                if (b.isFocused()) {
                    DrawContext.fill(bgX + 1, bgY + 1, bgX + bw - 1, bgY + 2, boxTopFocus);
                }
            }
        }

    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        boolean sumVisible = tab == 0;

        for(TextFieldWidget b : new TextFieldWidget[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField}) {
            b.visible = sumVisible;
        }

        this.sumShinyButton.visible = sumVisible;
        this.clearSpeciesBtn.visible = sumVisible;
        this.statusCycleBtn.visible = sumVisible;
        this.genderCycleBtn.visible = sumVisible;
        this.clearNameBtn.visible = sumVisible;
        this.clearNatureBtn.visible = sumVisible;
        this.clearAbilityBtn.visible = sumVisible;
        this.clearFormBtn.visible = sumVisible;
        this.clearHeldItemBtn.visible = sumVisible;
        this.levelDownBtn.visible = sumVisible;
        this.levelUpBtn.visible = sumVisible;
        this.friendshipDownBtn.visible = sumVisible;
        this.friendshipUpBtn.visible = sumVisible;
        boolean statsVisible = tab == 2;

        for(TextFieldWidget b : new TextFieldWidget[]{this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe}) {
            b.visible = statsVisible;
        }

        this.maxIvButton.visible = statsVisible;
        if (this.clearEvButton != null) {
            this.clearEvButton.visible = statsVisible;
        }

        if (this.fullEvButton != null) {
            this.fullEvButton.visible = statsVisible;
        }

        this.crownVisible = statsVisible;
        if (this.hiddenPowerBtn != null) {
            this.hiddenPowerBtn.visible = statsVisible;
        }

        for(int i = 0; i < 6; ++i) {
            if (this.ivUpButtons[i] != null) {
                this.ivUpButtons[i].visible = statsVisible;
            }

            if (this.ivDownButtons[i] != null) {
                this.ivDownButtons[i].visible = statsVisible;
            }

            if (this.evUpButtons[i] != null) {
                this.evUpButtons[i].visible = statsVisible;
            }

            if (this.evDownButtons[i] != null) {
                this.evDownButtons[i].visible = statsVisible;
            }
        }

        boolean battleVisible = tab == 1;

        for(TextFieldWidget b : new TextFieldWidget[]{this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField}) {
            b.visible = battleVisible;
        }

        if (this.maxPPButton != null) {
            this.maxPPButton.visible = battleVisible;
        }
        // [线性-F01] 招式清除按钮仅在对战页(tab==1)显示
        if (this.clearMove1Btn != null) this.clearMove1Btn.visible = battleVisible;
        if (this.clearMove2Btn != null) this.clearMove2Btn.visible = battleVisible;
        if (this.clearMove3Btn != null) this.clearMove3Btn.visible = battleVisible;
        if (this.clearMove4Btn != null) this.clearMove4Btn.visible = battleVisible;

        boolean advVisible = tab == 3;

        for(TextFieldWidget b : new TextFieldWidget[]{this.advExpField, this.advUUIDField, this.advRawField}) {
            b.visible = advVisible;
        }

        for(ButtonWidget b : new ButtonWidget[]{this.advCopyBtn, this.advPasteBtn, this.advExportBtn, this.advImportBtn, this.advSaveBtn, this.advResetBtn, this.clearExpBtn}) {
            b.visible = advVisible;
        }

        if (this.teamAllBtn != null) {
            this.teamAllBtn.visible = advVisible;
        }

        if (!advVisible) {
            this.viewingAllTeam = false;
        } else {
            this.advRawScrollOffset = 0;
        }

    }

    private void updateSlotButtonLabel(int slot) {
        if (slot < this.slotButtons.length && this.slotButtons[slot] != null) {
            PokemonData d = (PokemonData)this.partyData.get(slot);
            String label;
            if (d.exists) {
                String display = d.displayName.isEmpty() ? PokemonNameMap.speciesToDisplay(d.speciesName) : d.displayName;
                String shinyText = d.shiny ? " \u2728" : "";
                label = "  Lv" + d.level + " " + display + shinyText + "  ";
            } else {
                label = Text.translatable("cobblemon_ts.editor.slot_empty", slot + 1).getString() + "  ";
            }

            if (slot == this.selectedSlot) {
                label = ">> " + label;
            } else {
                label = "   " + label;
            }

            this.slotButtons[slot].setMessage(Text.literal(label));
        }
    }

    private void selectSlot(int slot) {
        this.selectedSlot = slot;
        this.viewingAllTeam = false;
        this.hiddenPowerType = "";
        if (this.pendingDeleteSlot >= 0 && this.pendingDeleteSlot != slot) {
            PokemonEditorDebug.logEditor("[Screen] selectSlot {} clears pendingDeleteSlot {}", new Object[]{slot, this.pendingDeleteSlot});
            this.pendingDeleteSlot = -1;
        }

        for(int i = 0; i < Math.min(6, this.partySize); ++i) {
            this.updateSlotButtonLabel(i);
        }

        this.refreshEditFields();
    }

    private void refreshEditFields() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            this.originalSnapshots.set(this.selectedSlot, d.snapshot());
            d.clearModified();
            PokemonEditorDebug.infoMoves("[Screen] refreshEditFields START: slot={} exists={} moves=slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})", new Object[]{this.selectedSlot, d.exists, d.move1, d.move1PP, d.move1MaxPP, d.move2, d.move2PP, d.move2MaxPP, d.move3, d.move3PP, d.move3MaxPP, d.move4, d.move4PP, d.move4MaxPP});
            if (!d.exists) {
                this.sumSpeciesField.setText("");
                this.sumLevelField.setText("1");
                this.sumNameField.setText("");
                List<String> natureList = PokemonNameMap.natureDisplayList();
                this.sumNatureField.setText(natureList.isEmpty() ? "" : (String)natureList.get(0));
                this.sumAbilityField.setText("");
                this.sumFriendshipField.setText("0");
                this.sumFormField.setText("");
                this.sumHeldItemField.setText("");
                this.genderField.setText("0");
                this.ivHp.setText("0");
                this.ivAtk.setText("0");
                this.ivDef.setText("0");
                this.ivSpA.setText("0");
                this.ivSpD.setText("0");
                this.ivSpe.setText("0");
                this.evHp.setText("0");
                this.evAtk.setText("0");
                this.evDef.setText("0");
                this.evSpA.setText("0");
                this.evSpD.setText("0");
                this.evSpe.setText("0");
                this.currentHpField.setText("0");
                this.maxHpField.setText("1");
                this.move1Field.setText("");
                this.move2Field.setText("");
                this.move3Field.setText("");
                this.move4Field.setText("");
                this.move1PPField.setText("0");
                this.move2PPField.setText("0");
                this.move3PPField.setText("0");
                this.move4PPField.setText("0");
                this.move1MaxPPField.setText("0");
                this.move2MaxPPField.setText("0");
                this.move3MaxPPField.setText("0");
                this.move4MaxPPField.setText("0");
                this.advExpField.setText("0");
                this.advUUIDField.setText("");
                d.shiny = false;
            } else {
                this.sumSpeciesField.setText(PokemonNameMap.speciesToDisplay(d.speciesName));
                this.sumLevelField.setText(String.valueOf(d.level));
                this.sumNameField.setText(d.displayName);
                this.sumNatureField.setText(PokemonNameMap.natureToDisplay(d.nature));
                this.sumAbilityField.setText(PokemonNameMap.abilityToDisplay(d.ability));
                this.sumFriendshipField.setText(String.valueOf(d.friendship));
                this.sumFormField.setText(PokemonNameMap.formToChinese(d.speciesName, d.formName));
                this.sumHeldItemField.setText(PokemonNameMap.itemToChinese(d.heldItem));
                this.genderField.setText(String.valueOf(d.gender));
                this.ivHp.setText(String.valueOf(d.ivHp));
                this.ivAtk.setText(String.valueOf(d.ivAtk));
                this.ivDef.setText(String.valueOf(d.ivDef));
                this.ivSpA.setText(String.valueOf(d.ivSpA));
                this.ivSpD.setText(String.valueOf(d.ivSpD));
                this.ivSpe.setText(String.valueOf(d.ivSpe));
                this.evHp.setText(String.valueOf(d.evHp));
                this.evAtk.setText(String.valueOf(d.evAtk));
                this.evDef.setText(String.valueOf(d.evDef));
                this.evSpA.setText(String.valueOf(d.evSpA));
                this.evSpD.setText(String.valueOf(d.evSpD));
                this.evSpe.setText(String.valueOf(d.evSpe));
                this.currentHpField.setText(String.valueOf(d.currentHp));
                this.maxHpField.setText(String.valueOf(d.maxHp));
                String m1zh = PokemonNameMap.moveToChinese(d.move1);
                String m2zh = PokemonNameMap.moveToChinese(d.move2);
                String m3zh = PokemonNameMap.moveToChinese(d.move3);
                String m4zh = PokemonNameMap.moveToChinese(d.move4);
                PokemonEditorDebug.infoMoves("[Screen] moveToChinese results: slot0 en='{}'->zh='{}' slot1 en='{}'->zh='{}' slot2 en='{}'->zh='{}' slot3 en='{}'->zh='{}'", new Object[]{d.move1, m1zh, d.move2, m2zh, d.move3, m3zh, d.move4, m4zh});
                this.move1Field.setText(m1zh);
                this.move2Field.setText(m2zh);
                this.move3Field.setText(m3zh);
                this.move4Field.setText(m4zh);
                this.move1PPField.setText(String.valueOf(d.move1PP));
                this.move2PPField.setText(String.valueOf(d.move2PP));
                this.move3PPField.setText(String.valueOf(d.move3PP));
                this.move4PPField.setText(String.valueOf(d.move4PP));
                this.move1MaxPPField.setText(String.valueOf(d.move1MaxPP));
                this.move2MaxPPField.setText(String.valueOf(d.move2MaxPP));
                this.move3MaxPPField.setText(String.valueOf(d.move3MaxPP));
                this.move4MaxPPField.setText(String.valueOf(d.move4MaxPP));
                PokemonEditorDebug.infoMoves("[Screen] AFTER setValue: slot0 field='{}'({}/{}) slot1 field='{}'({}/{}) slot2 field='{}'({}/{}) slot3 field='{}'({}/{})", new Object[]{this.move1Field.getText(), this.move1PPField.getText(), this.move1MaxPPField.getText(), this.move2Field.getText(), this.move2PPField.getText(), this.move2MaxPPField.getText(), this.move3Field.getText(), this.move3PPField.getText(), this.move3MaxPPField.getText(), this.move4Field.getText(), this.move4PPField.getText(), this.move4MaxPPField.getText()});
                this.advExpField.setText(String.valueOf(d.experience));
                this.advUUIDField.setText(d.pokemonUUID == null ? "" : d.pokemonUUID.toString());
            }

            this.advRawField.setText(this.buildDataString(d));
            ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(0);
            this.advRawField.setSelectionEnd(0);
            this.advRawScrollOffset = 0;
            this.updateShinyButton();
            this.updateStatusButton();

            for(TextFieldWidget b : new TextFieldWidget[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
                b.setEditable(true);
            }

            this.sumShinyButton.active = true;
            this.updateSaveButtonState();
            this.deleteButton.active = d.exists;
            this.refreshHiddenPower();
        }
    }

    private void updateSaveButtonState() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            this.saveButton.active = true;
        } else {
            this.saveButton.active = false;
        }
    }

    private void updateShinyButton() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            this.sumShinyButton.setMessage(d.shiny ? Text.translatable("cobblemon_ts.editor.shiny_on") : Text.translatable("cobblemon_ts.editor.shiny_off"));
        }

    }

    private String normalizeStatusId(String raw) {
        if (raw == null) {
            return "";
        } else {
            String cur = raw.toLowerCase().replaceAll("[^a-z_]", "");
            if (cur.equals("frozen")) {
                cur = "freeze";
            }

            if (cur.equals("poison_badly") || cur.equals("badly") || cur.equals("tox")) {
                cur = "bad_poison";
            }

            if (cur.contains("freeze") || cur.contains("frozen")) {
                cur = "freeze";
            }

            if (cur.contains("badly") || cur.contains("poison_bad") || cur.contains("bad_poison")) {
                cur = "bad_poison";
            }

            return cur;
        }
    }

    private void cycleToNextStatus() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            String cur = this.normalizeStatusId(d.status);
            int idx = -1;

            for(int i = 0; i < STATUS_DISPLAY.length; ++i) {
                if (STATUS_DISPLAY[i][0].equals(cur)) {
                    idx = i;
                    break;
                }
            }

            if (idx < 0) {
                idx = 0;
            }

            int next = (idx + 1) % STATUS_DISPLAY.length;
            d.status = STATUS_DISPLAY[next][0];
            this.updateStatusButton();
        }
    }

    // [线性-F01] 状态向左循环：右键点击状态切换按钮时调用
    // 循环顺序：无←剧毒←睡眠←冰冻←烧伤←麻痹←中毒←无
    private void cycleToPrevStatus() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            String cur = this.normalizeStatusId(d.status);
            int idx = -1;

            for(int i = 0; i < STATUS_DISPLAY.length; ++i) {
                if (STATUS_DISPLAY[i][0].equals(cur)) {
                    idx = i;
                    break;
                }
            }

            if (idx < 0) {
                idx = 0;
            }

            int prev = (idx - 1 + STATUS_DISPLAY.length) % STATUS_DISPLAY.length;
            d.status = STATUS_DISPLAY[prev][0];
            this.updateStatusButton();
        }
    }

    private void updateStatusButton() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            String cur = this.normalizeStatusId(d.status);
            String key = "cobblemon_ts.editor.status_none";

            for(String[] row : STATUS_DISPLAY) {
                if (row[0].equals(cur)) {
                    key = row[1];
                    break;
                }
            }

            this.statusCycleBtn.setMessage(Text.translatable(key));
        }
    }

    private void applyMoveImmediate(int moveIdx, String selected) {
        if (this.selectedSlot < 0 || this.selectedSlot >= this.partyData.size()) return;
        PokemonData d = (PokemonData) this.partyData.get(this.selectedSlot);
        String enName = PokemonNameMap.moveToEnglish(selected);
        if (enName != null && !enName.isEmpty()) {
            int basePP = CobblemonBridge.getBasePP(enName);
            switch (moveIdx) {
                case 0: d.move1 = enName; d.move1PP = basePP; d.move1MaxPP = basePP;
                    this.move1PPField.setText(String.valueOf(basePP));
                    this.move1MaxPPField.setText(String.valueOf(basePP));
                    break;
                case 1: d.move2 = enName; d.move2PP = basePP; d.move2MaxPP = basePP;
                    this.move2PPField.setText(String.valueOf(basePP));
                    this.move2MaxPPField.setText(String.valueOf(basePP));
                    break;
                case 2: d.move3 = enName; d.move3PP = basePP; d.move3MaxPP = basePP;
                    this.move3PPField.setText(String.valueOf(basePP));
                    this.move3MaxPPField.setText(String.valueOf(basePP));
                    break;
                case 3: d.move4 = enName; d.move4PP = basePP; d.move4MaxPP = basePP;
                    this.move4PPField.setText(String.valueOf(basePP));
                    this.move4MaxPPField.setText(String.valueOf(basePP));
                    break;
            }
        }
    }

    private int parseOrDefault(TextFieldWidget box, int def) {
        try {
            return Integer.parseInt(box.getText());
        } catch (Exception var4) {
            return def;
        }
    }

    private void saveChanges() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            if (this.pendingDeleteSlot >= 0 && this.pendingDeleteSlot == this.selectedSlot) {
                PokemonEditorDebug.logEditor("[Screen] Save triggers pending delete for slot {}", new Object[]{this.selectedSlot});
                ClientPlayNetworking.send(new PokemonEditorNetwork.DeletePokemonPacket(this.selectedSlot, this.targetPlayerUUID));
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.deleted", this.selectedSlot + 1));
                }

                this.pendingDeleteSlot = -1;
                if ((Boolean)CharmConfig.EDITOR_AUTO_CLOSE.get()) {
                    this.close();
                }

            } else {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                String beforeSpecies = d.speciesName;
                d.speciesName = PokemonNameMap.toEnglish(this.sumSpeciesField.getText());
                PokemonEditorDebug.logEditor("[Screen] species: UI='{}' \u2192 EN='{}' (was '{}')", new Object[]{this.sumSpeciesField.getText(), d.speciesName, beforeSpecies});
                if (!Objects.equals(beforeSpecies, d.speciesName)) {
                    d.experience = 0;
                    if (this.advExpField != null) {
                        this.advExpField.setText("0");
                    }
                    PokemonEditorDebug.logEditor("[Screen] species changed: exp cleared");
                }
                if (d.speciesName != null && !d.speciesName.isEmpty()) {
                    boolean var48 = false;
                } else {
                    boolean var10000 = true;
                }

                d.level = Math.max(1, Math.min(900, this.parseOrDefault(this.sumLevelField, d.level)));
                d.displayName = this.sumNameField.getText();
                PokemonEditorDebug.logEditor("[Screen] displayName: UI='{}' \u2192 EN='{}'", new Object[]{this.sumNameField.getText(), d.displayName});
                d.friendship = Math.max(0, Math.min(255, this.parseOrDefault(this.sumFriendshipField, d.friendship)));
                String beforeNature = d.nature;
                d.nature = PokemonNameMap.natureToEnglish(this.sumNatureField.getText());
                PokemonEditorDebug.logEditor("[Screen] nature: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumNatureField.getText(), d.nature, beforeNature, !Objects.equals(d.nature, beforeNature)});
                String beforeAbility = d.ability;
                d.ability = PokemonNameMap.abilityToEnglish(this.sumAbilityField.getText());
                PokemonEditorDebug.logEditor("[Screen] ability: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumAbilityField.getText(), d.ability, beforeAbility, !Objects.equals(d.ability, beforeAbility)});
                String formFieldVal = this.sumFormField.getText();
                // [线性-F05] 形态字段不为空→查英文ID；为空→清空为普通形态
                if (formFieldVal != null && !formFieldVal.isEmpty() && formFieldVal.chars().allMatch(c -> c < 128)) {
                    d.formName = formFieldVal;
                } else if (formFieldVal != null && !formFieldVal.isEmpty()) {
                    if (formFieldVal.equals(PokemonNameMap.formToDisplay(null, null))) {
                        d.formName = ""; // 普通形态=空ID
                    } else {
                        String enSpecies = d.speciesName; // 已转英文
                        List<String> rawForms = CobblemonBridge.getSpeciesForms(enSpecies);
                        for (String raw : rawForms) {
                            if (formFieldVal.equals(PokemonNameMap.formToDisplay(enSpecies, raw))) {
                                d.formName = raw;
                                PokemonEditorDebug.logEditor("[Screen] form reverse-lookup: '{}' -> '{}'", formFieldVal, raw);
                                break;
                            }
                        }
                    }
                } else {
                    d.formName = ""; // 清除→普通形态
                }
                String beforeHeldItem = d.heldItem;
                d.heldItem = PokemonNameMap.itemToEnglish(this.sumHeldItemField.getText());
                PokemonEditorDebug.logEditor("[Screen] heldItem: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumHeldItemField.getText(), d.heldItem, beforeHeldItem, !Objects.equals(d.heldItem, beforeHeldItem)});
                d.experience = Math.max(0, this.parseOrDefault(this.advExpField, d.experience));
                d.ivHp = Math.max(0, Math.min(31, this.parseOrDefault(this.ivHp, d.ivHp)));
                d.ivAtk = Math.max(0, Math.min(31, this.parseOrDefault(this.ivAtk, d.ivAtk)));
                d.ivDef = Math.max(0, Math.min(31, this.parseOrDefault(this.ivDef, d.ivDef)));
                d.ivSpA = Math.max(0, Math.min(31, this.parseOrDefault(this.ivSpA, d.ivSpA)));
                d.ivSpD = Math.max(0, Math.min(31, this.parseOrDefault(this.ivSpD, d.ivSpD)));
                d.ivSpe = Math.max(0, Math.min(31, this.parseOrDefault(this.ivSpe, d.ivSpe)));
                d.evHp = Math.max(0, Math.min(252, this.parseOrDefault(this.evHp, d.evHp)));
                d.evAtk = Math.max(0, Math.min(252, this.parseOrDefault(this.evAtk, d.evAtk)));
                d.evDef = Math.max(0, Math.min(252, this.parseOrDefault(this.evDef, d.evDef)));
                d.evSpA = Math.max(0, Math.min(252, this.parseOrDefault(this.evSpA, d.evSpA)));
                d.evSpD = Math.max(0, Math.min(252, this.parseOrDefault(this.evSpD, d.evSpD)));
                d.evSpe = Math.max(0, Math.min(252, this.parseOrDefault(this.evSpe, d.evSpe)));
                d.currentHp = Math.max(0, this.parseOrDefault(this.currentHpField, d.currentHp));
                d.maxHp = Math.max(1, this.parseOrDefault(this.maxHpField, d.maxHp));
                d.gender = Math.max(0, Math.min(2, this.parseOrDefault(this.genderField, d.gender)));
                String oldMove1 = d.move1;
                String oldMove2 = d.move2;
                String oldMove3 = d.move3;
                String oldMove4 = d.move4;
                d.move1 = PokemonNameMap.moveToEnglish(this.move1Field.getText());
                d.move2 = PokemonNameMap.moveToEnglish(this.move2Field.getText());
                d.move3 = PokemonNameMap.moveToEnglish(this.move3Field.getText());
                d.move4 = PokemonNameMap.moveToEnglish(this.move4Field.getText());
                int[] rawPP1s = new int[4];
                int[] rawPP2s = new int[4];

                try {
                    rawPP1s[0] = this.parseOrDefault(this.move1PPField, d.move1PP);
                    rawPP1s[1] = this.parseOrDefault(this.move2PPField, d.move2PP);
                    rawPP1s[2] = this.parseOrDefault(this.move3PPField, d.move3PP);
                    rawPP1s[3] = this.parseOrDefault(this.move4PPField, d.move4PP);
                    rawPP2s[0] = this.parseOrDefault(this.move1MaxPPField, d.move1MaxPP);
                    rawPP2s[1] = this.parseOrDefault(this.move2MaxPPField, d.move2MaxPP);
                    rawPP2s[2] = this.parseOrDefault(this.move3MaxPPField, d.move3MaxPP);
                    rawPP2s[3] = this.parseOrDefault(this.move4MaxPPField, d.move4MaxPP);
                } catch (Exception var36) {
                }

                int basePP1 = Math.max(0, CobblemonBridge.getBasePP(d.move1));
                boolean move1Changed = d.move1 != null && !d.move1.isEmpty() && (oldMove1 == null || !oldMove1.equalsIgnoreCase(d.move1));
                int userPP1_1 = Math.max(0, rawPP1s[0]);
                int userPP2_1 = Math.max(0, rawPP2s[0]);
                if (!move1Changed && basePP1 > 0) {
                    int pp2 = userPP2_1 > 0 ? userPP2_1 : basePP1;
                    int pp1 = userPP1_1 > 0 ? userPP1_1 : pp2;
                    if (pp1 > pp2) {
                        pp1 = pp2;
                    }

                    d.move1MaxPP = pp2;
                    d.move1PP = pp1;
                } else {
                    d.move1MaxPP = basePP1;
                    d.move1PP = basePP1;
                }

                int basePP2 = Math.max(0, CobblemonBridge.getBasePP(d.move2));
                boolean move2Changed = d.move2 != null && !d.move2.isEmpty() && (oldMove2 == null || !oldMove2.equalsIgnoreCase(d.move2));
                int userPP1_2 = Math.max(0, rawPP1s[1]);
                int userPP2_2 = Math.max(0, rawPP2s[1]);
                if (!move2Changed && basePP2 > 0) {
                    int pp2 = userPP2_2 > 0 ? userPP2_2 : basePP2;
                    int pp1 = userPP1_2 > 0 ? userPP1_2 : pp2;
                    if (pp1 > pp2) {
                        pp1 = pp2;
                    }

                    d.move2MaxPP = pp2;
                    d.move2PP = pp1;
                } else {
                    d.move2MaxPP = basePP2;
                    d.move2PP = basePP2;
                }

                int basePP3 = Math.max(0, CobblemonBridge.getBasePP(d.move3));
                boolean move3Changed = d.move3 != null && !d.move3.isEmpty() && (oldMove3 == null || !oldMove3.equalsIgnoreCase(d.move3));
                int userPP1_3 = Math.max(0, rawPP1s[2]);
                int userPP2_3 = Math.max(0, rawPP2s[2]);
                if (!move3Changed && basePP3 > 0) {
                    int pp2 = userPP2_3 > 0 ? userPP2_3 : basePP3;
                    int pp1 = userPP1_3 > 0 ? userPP1_3 : pp2;
                    if (pp1 > pp2) {
                        pp1 = pp2;
                    }

                    d.move3MaxPP = pp2;
                    d.move3PP = pp1;
                } else {
                    d.move3MaxPP = basePP3;
                    d.move3PP = basePP3;
                }

                int basePP4 = Math.max(0, CobblemonBridge.getBasePP(d.move4));
                boolean move4Changed = d.move4 != null && !d.move4.isEmpty() && (oldMove4 == null || !oldMove4.equalsIgnoreCase(d.move4));
                int userPP1_4 = Math.max(0, rawPP1s[3]);
                int userPP2_4 = Math.max(0, rawPP2s[3]);
                if (!move4Changed && basePP4 > 0) {
                    int pp2 = userPP2_4 > 0 ? userPP2_4 : basePP4;
                    int pp1 = userPP1_4 > 0 ? userPP1_4 : pp2;
                    if (pp1 > pp2) {
                        pp1 = pp2;
                    }

                    d.move4MaxPP = pp2;
                    d.move4PP = pp1;
                } else {
                    d.move4MaxPP = basePP4;
                    d.move4PP = basePP4;
                }

                LinkedHashSet<String> seenMoves = new LinkedHashSet();
                String[] allMoveNames = new String[]{d.move1, d.move2, d.move3, d.move4};
                int[] var49 = new int[]{d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP};
                var49 = new int[]{d.move1PP, d.move2PP, d.move3PP, d.move4PP};

                for(int i = 0; i < 4; ++i) {
                    String nm = allMoveNames[i];
                    if (nm != null && !nm.isEmpty()) {
                        String lower = nm.toLowerCase();
                        if (seenMoves.contains(lower)) {
                            if (i == 0) {
                                d.move1 = "";
                                d.move1MaxPP = 0;
                                d.move1PP = 0;
                            } else if (i == 1) {
                                d.move2 = "";
                                d.move2MaxPP = 0;
                                d.move2PP = 0;
                            } else if (i == 2) {
                                d.move3 = "";
                                d.move3MaxPP = 0;
                                d.move3PP = 0;
                            } else {
                                d.move4 = "";
                                d.move4MaxPP = 0;
                                d.move4PP = 0;
                            }

                            PokemonEditorDebug.infoMoves("[Screen] DEDUP: slot{} duplicate '{}' \u2192 clear", new Object[]{i, nm});
                        } else {
                            seenMoves.add(lower);
                        }
                    }
                }

                PokemonEditorDebug.infoMoves("[Screen] saveChanges FINAL moves: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})", new Object[]{d.move1, d.move1PP, d.move1MaxPP, d.move2, d.move2PP, d.move2MaxPP, d.move3, d.move3PP, d.move3MaxPP, d.move4, d.move4PP, d.move4MaxPP});
                String[] movesToSend = new String[]{d.move1, d.move2, d.move3, d.move4};

                for(int i = 0; i < 4; ++i) {
                    for(int j = i + 1; j < 4; ++j) {
                        if (movesToSend[i] != null && !movesToSend[i].isEmpty() && movesToSend[j] != null && movesToSend[i].equalsIgnoreCase(movesToSend[j])) {
                            PokemonEditorDebug.warnMoves("[Screen] saveChanges DUPLICATE: slot{} and slot{} both='{}'", new Object[]{i, j, movesToSend[i]});
                        }
                    }
                }

                this.sendUpdatePacket(d, this.advUUIDField.getText().trim());
            }
        }
    }

    private void savePokemonData(PokemonData d) {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            if (d != null) {
                boolean isDelete = d.speciesName == null || d.speciesName.isEmpty();
                if (isDelete) {
                    PokemonEditorDebug.logEditor("[Screen] savePokemonData: delete for slot {}", new Object[]{this.selectedSlot});
                    ClientPlayNetworking.send(new PokemonEditorNetwork.DeletePokemonPacket(this.selectedSlot, this.targetPlayerUUID));
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.deleted", this.selectedSlot + 1));
                    }

                    this.pendingDeleteSlot = -1;
                    PokemonData emptyData = new PokemonData();
                    emptyData.exists = false;
                    this.partyData.set(this.selectedSlot, emptyData);
                    this.refreshEditFields();
                    this.updateSlotButtonLabel(this.selectedSlot);
                } else {
                    this.sendUpdatePacket(d, d.pokemonUUID != null ? d.pokemonUUID.toString() : "");
                }
            }
        }
    }

    private void sendUpdatePacket(PokemonData d, String uuidText) {
        String rawDataText = this.advRawField != null ? this.advRawField.getText() : "";
        if (rawDataText == null) {
            rawDataText = "";
        }

        PokemonData base = (PokemonData)this.originalSnapshots.get(this.selectedSlot);
        Set<String> modifiedFields = d.diffFrom(base);
        if (base != null) {
            PokemonEditorDebug.logEditor("[Screen] FIELD-CHECK slot {}: nature(current='{}' vs base='{}', equal={}, inModified={}) ability(current='{}' vs base='{}', equal={}, inModified={}) heldItem(current='{}' vs base='{}', equal={}, inModified={})", new Object[]{this.selectedSlot, d.nature, base.nature, Objects.equals(d.nature, base.nature), modifiedFields.contains("nature"), d.ability, base.ability, Objects.equals(d.ability, base.ability), modifiedFields.contains("ability"), d.heldItem, base.heldItem, Objects.equals(d.heldItem, base.heldItem), modifiedFields.contains("heldItem")});
        }

        if (base == null || !base.exists) {
            modifiedFields.add("speciesName");
            modifiedFields.add("level");
        }

        String modifiedHeader = "__MODIFIED__=" + String.join(",", modifiedFields) + "\n";
        String combinedRawData = modifiedHeader + rawDataText;
        PokemonEditorDebug.logEditor("[Screen] SENDING packet for slot {}: species={}, level={}, shiny={}, friendship={}, ivHp={}, evHp={}, move1={}, modifiedFields=[{}], hpModified={}, statusModified={}", new Object[]{this.selectedSlot, d.speciesName, d.level, d.shiny, d.friendship, d.ivHp, d.evHp, d.move1, String.join(",", modifiedFields), modifiedFields.contains("currentHp") || modifiedFields.contains("maxHp"), modifiedFields.contains("status")});
        PokemonEditorDebug.infoMoves("[Screen] SENDING move data: slot0='{}'({}/{}) slot1='{}'({}/{}) slot2='{}'({}/{}) slot3='{}'({}/{})", new Object[]{d.move1, d.move1PP, d.move1MaxPP, d.move2, d.move2PP, d.move2MaxPP, d.move3, d.move3PP, d.move3MaxPP, d.move4, d.move4PP, d.move4MaxPP});
        String[] moves = new String[]{d.move1, d.move2, d.move3, d.move4};

        for(int i = 0; i < 4; ++i) {
            for(int j = i + 1; j < 4; ++j) {
                if (moves[i] != null && !moves[i].isEmpty() && moves[j] != null && moves[i].equalsIgnoreCase(moves[j])) {
                    PokemonEditorDebug.warnMoves("[Screen] SENDING DUPLICATE: slot{} and slot{} both='{}'", new Object[]{i, j, moves[i]});
                }
            }
        }

        ClientPlayNetworking.send(new PokemonEditorNetwork.UpdatePokemonPacket(this.selectedSlot, this.targetPlayerUUID, d.speciesName, d.formName, d.level, d.experience, d.displayName, d.shiny, d.friendship, d.goldenBottleCap, d.ivHp, d.ivAtk, d.ivDef, d.ivSpA, d.ivSpD, d.ivSpe, d.evHp, d.evAtk, d.evDef, d.evSpA, d.evSpD, d.evSpe, d.nature, d.ability, d.move1, d.move2, d.move3, d.move4, d.move1PP, d.move2PP, d.move3PP, d.move4PP, d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP, d.currentHp, d.maxHp, d.heldItem, d.gender, uuidText, combinedRawData, d.status));
        this.originalSnapshots.set(this.selectedSlot, d.snapshot());
        this.updateSlotButtonLabel(this.selectedSlot);
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.saved", this.selectedSlot + 1));
        }

        PokemonEditorDebug.logEditor("[Screen] Packet sent for slot {}", new Object[]{this.selectedSlot});
        if ((Boolean)CharmConfig.EDITOR_AUTO_CLOSE.get()) {
            this.close();
        } else {
            PokemonEditorDebug.logEditor("[Screen] auto-close disabled, keeping editor open", new Object[0]);
        }

    }

    private void deletePokemon() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            if (d.exists) {
                // [优化-L5] 添加删除二次确认弹窗
                Text title = Text.translatable("cobblemon_ts.editor.confirm_delete_title");
                Text message = Text.translatable("cobblemon_ts.editor.confirm_delete_msg", this.selectedSlot + 1);

                // 使用原版确认弹窗
                ConfirmScreen confirmScreen = new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            this.pendingDeleteSlot = this.selectedSlot;
                            PokemonEditorDebug.logEditor("[Screen] Delete marker set for slot {} (pending, will delete on save)", new Object[]{this.selectedSlot});
                            if (MinecraftClient.getInstance().player != null) {
                                MinecraftClient.getInstance().player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.marked_delete", this.selectedSlot + 1));
                            }

                            PokemonData emptyData = new PokemonData();
                            emptyData.exists = false;
                            this.partyData.set(this.selectedSlot, emptyData);
                            this.refreshEditFields();
                            this.updateSlotButtonLabel(this.selectedSlot);
                        }
                        // 不管是否确认，都回到编辑器界面
                        MinecraftClient.getInstance().setScreen(this);
                    }, title, message
                );

                MinecraftClient.getInstance().setScreen(confirmScreen);
            }
        }
    }

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
        if (text != null && !text.isEmpty()) {
            PokemonEditorDebug.logEditor("[Screen] applyDataString START: textLen={}", new Object[]{text.length()});

            for(String rawLine : text.split("\n")) {
                String line = rawLine.trim();
                if (!line.isEmpty()) {
                    if (line.startsWith("#")) {
                        String metaContent = line.substring(1).trim();
                        if (metaContent.contains("=")) {
                            int pos = 0;

                            while(pos < metaContent.length()) {
                                int eq = metaContent.indexOf("=", pos);
                                if (eq <= 0) {
                                    break;
                                }

                                int keyStart;
                                for(keyStart = eq - 1; keyStart >= pos && !Character.isWhitespace(metaContent.charAt(keyStart)); --keyStart) {
                                }

                                ++keyStart;
                                String key = metaContent.substring(keyStart, eq).trim().toLowerCase();
                                if (key.isEmpty()) {
                                    pos = eq + 1;
                                } else {
                                    int valStart = eq + 1;
                                    int valEnd = metaContent.length();

                                    for(int scan = valStart; scan < metaContent.length(); ++scan) {
                                        if (Character.isWhitespace(metaContent.charAt(scan))) {
                                            int wordStart = scan + 1;

                                            int wordEnd;
                                            for(wordEnd = wordStart; wordEnd < metaContent.length() && !Character.isWhitespace(metaContent.charAt(wordEnd)) && metaContent.charAt(wordEnd) != '='; ++wordEnd) {
                                            }

                                            if (wordEnd < metaContent.length() && metaContent.charAt(wordEnd) == '=' && wordEnd > wordStart) {
                                                valEnd = scan;
                                                break;
                                            }
                                        }
                                    }

                                    String val = metaContent.substring(valStart, valEnd).trim();

                                    try {
                                        switch (key) {
                                            case "pokemonuuid":
                                            case "uuid":
                                                if (!val.isEmpty()) {
                                                    try {
                                                        d.pokemonUUID = UUID.fromString(val);
                                                    } catch (IllegalArgumentException e) {
                                                        // [优化-L3] UUID格式错误时不崩溃，保留原UUID
                                                        PokemonEditorDebug.logEditor("Invalid UUID format: " + val);
                                                    }
                                                }
                                                break;
                                            case "experience":
                                                d.experience = Math.max(0, Integer.parseInt(val));
                                                break;
                                            case "species":
                                                if (!val.isEmpty()) {
                                                    d.speciesName = val;
                                                }
                                                break;
                                            case "level":
                                                d.level = Math.max(1, Math.min(900, Integer.parseInt(val)));
                                                break;
                                            case "shiny":
                                                d.shiny = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1");
                                                break;
                                            case "goldenbottlecap":
                                                d.goldenBottleCap = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1");
                                                break;
                                            case "gender":
                                                d.gender = Math.max(0, Math.min(2, Integer.parseInt(val)));
                                                break;
                                            case "nature":
                                                if (!val.isEmpty()) {
                                                    d.nature = val;
                                                }
                                                break;
                                            case "ability":
                                                if (!val.isEmpty()) {
                                                    d.ability = val;
                                                }
                                                break;
                                            case "helditem":
                                            case "held_item":
                                                if (!val.isEmpty()) {
                                                    d.heldItem = val;
                                                }
                                                break;
                                            case "status":
                                                if (val != null) {
                                                    d.status = val.trim();
                                                }
                                        }
                                    } catch (Throwable var19) {
                                    }

                                    for(pos = valEnd; pos < metaContent.length() && Character.isWhitespace(metaContent.charAt(pos)); ++pos) {
                                    }
                                }
                            }
                        }
                    } else {
                        int pos = 0;

                        while(pos < line.length()) {
                            int eq = line.indexOf("=", pos);
                            if (eq <= 0) {
                                break;
                            }

                            int keyStart;
                            for(keyStart = eq - 1; keyStart >= pos && !Character.isWhitespace(line.charAt(keyStart)); --keyStart) {
                            }

                            ++keyStart;
                            String key = line.substring(keyStart, eq).trim().toLowerCase();
                            if (key.isEmpty()) {
                                pos = eq + 1;
                            } else {
                                int valStart = eq + 1;
                                int valEnd = line.length();

                                for(int scan = valStart; scan < line.length(); ++scan) {
                                    if (Character.isWhitespace(line.charAt(scan))) {
                                        int wordStart = scan + 1;

                                        int wordEnd;
                                        for(wordEnd = wordStart; wordEnd < line.length() && !Character.isWhitespace(line.charAt(wordEnd)) && line.charAt(wordEnd) != '='; ++wordEnd) {
                                        }

                                        if (wordEnd < line.length() && line.charAt(wordEnd) == '=' && wordEnd > wordStart) {
                                            valEnd = scan;
                                            break;
                                        }
                                    }
                                }

                                String val = line.substring(valStart, valEnd).trim();
                                int hash = val.indexOf("#");
                                if (hash > 0) {
                                    val = val.substring(0, hash).trim();
                                }

                                try {
                                    switch (key) {
                                        case "species":
                                            d.speciesName = PokemonNameMap.toEnglish(val);
                                            break;
                                        case "form":
                                            d.formName = val;
                                            break;
                                        case "level":
                                            d.level = Math.max(1, Math.min(900, Integer.parseInt(val)));
                                            break;
                                        case "experience":
                                            d.experience = Math.max(0, Integer.parseInt(val));
                                            break;
                                        case "friendship":
                                            d.friendship = Math.max(0, Math.min(255, Integer.parseInt(val)));
                                            break;
                                        case "shiny":
                                            d.shiny = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1");
                                            break;
                                        case "gender":
                                            d.gender = Math.max(0, Math.min(2, Integer.parseInt(val)));
                                            break;
                                        case "nature":
                                            d.nature = PokemonNameMap.natureToEnglish(val);
                                            break;
                                        case "ability":
                                            d.ability = PokemonNameMap.abilityToEnglish(val);
                                            break;
                                        case "currenthp":
                                        case "current_hp":
                                            d.currentHp = Math.max(0, Math.min(9999, Integer.parseInt(val)));
                                            break;
                                        case "maxhp":
                                        case "max_hp":
                                            d.maxHp = Math.max(1, Math.min(9999, Integer.parseInt(val)));
                                            break;
                                        case "ivhp":
                                        case "iv_hp":
                                            d.ivHp = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "ivatk":
                                        case "iv_atk":
                                            d.ivAtk = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "ivdef":
                                        case "iv_def":
                                            d.ivDef = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "ivspa":
                                        case "iv_spa":
                                            d.ivSpA = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "ivspd":
                                        case "iv_spd":
                                            d.ivSpD = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "ivspe":
                                        case "iv_spe":
                                            d.ivSpe = Math.max(0, Math.min(31, Integer.parseInt(val)));
                                            break;
                                        case "evhp":
                                        case "ev_hp":
                                            d.evHp = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "evatk":
                                        case "ev_atk":
                                            d.evAtk = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "evdef":
                                        case "ev_def":
                                            d.evDef = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "evspa":
                                        case "ev_spa":
                                            d.evSpA = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "evspd":
                                        case "ev_spd":
                                            d.evSpD = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "evspe":
                                        case "ev_spe":
                                            d.evSpe = Math.max(0, Math.min(252, Integer.parseInt(val)));
                                            break;
                                        case "move1":
                                            d.move1 = PokemonNameMap.moveToEnglish(val);
                                            break;
                                        case "move2":
                                            d.move2 = PokemonNameMap.moveToEnglish(val);
                                            break;
                                        case "move3":
                                            d.move3 = PokemonNameMap.moveToEnglish(val);
                                            break;
                                        case "move4":
                                            d.move4 = PokemonNameMap.moveToEnglish(val);
                                            break;
                                        case "helditem":
                                        case "held_item":
                                            d.heldItem = PokemonNameMap.itemToEnglish(val);
                                            break;
                                        case "status":
                                            if (val != null) {
                                                d.status = val.trim();
                                            }
                                    }
                                } catch (Throwable var20) {
                                }

                                for(pos = valEnd; pos < line.length() && Character.isWhitespace(line.charAt(pos)); ++pos) {
                                }
                            }
                        }
                    }
                }
            }

            PokemonEditorDebug.logEditor("[Screen] applyDataString DONE: species={}, level={}, shiny={}, friendship={}, ivHp={}, move1={}", new Object[]{d.speciesName, d.level, d.shiny, d.friendship, d.ivHp, d.move1});
        } else {
            PokemonEditorDebug.warnEditor("[Screen] applyDataString: empty text, skipping", new Object[0]);
        }
    }

    public void renderBackground(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
    }

    public void render(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        // [线性-C02] 使用标准Minecraft渐变背景替代纯色fill，避免黑色覆盖层遮挡字体
        DrawContext.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);

        // [线性-C02] 缩放矩阵：围绕布局中心(layoutCenterX=320, layoutCenterY=233.5)缩放并居中屏幕
        // layoutCenter = ((32+608)/2, (30+437)/2)，使缩放后UI在屏幕中居中而非贴底
        if (this.uiScaleFactor < 1.0f) {
            float layoutCenterX = 320f; // 布局水平中心 (panelX-8 + panelX+panelW+8)/2
            float layoutCenterY = 233.5f; // 布局垂直中心 (tabBarY + btnY+btnH)/2
            DrawContext.getMatrices().push();
            DrawContext.getMatrices().translate(this.width / 2f, this.height / 2f, 0);
            DrawContext.getMatrices().scale(this.uiScaleFactor, this.uiScaleFactor, 1.0f);
            DrawContext.getMatrices().translate(-layoutCenterX, -layoutCenterY, 0);
        }

        int screenW = this.width;
        this.updateSaveButtonState();
        int labelColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? ColorEditorConfig.editorLightLabel() : ColorEditorConfig.editorDarkLabel();
        boolean isTab0 = this.activeTab == 0;
        boolean isTab1 = this.activeTab == 1;
        boolean isTab2 = this.activeTab == 2;
        boolean isTab3 = this.activeTab == 3;
        // [优化-L1] 已删除render中重复的控件可见性赋值，由switchTab()统一控制，减少每帧冗余计算

        int panelX = screenW / 2 - 280;
        int panelY = 60;
        int panelW = 560;
        int panelH = 340;
        int partyX = panelX + 10;
        int partyY = panelY + 10;
        int editX = panelX + 165;
        int editY = panelY + 10;
        int editW = panelW - 175;
        int containerTop = panelY - 35;
        int containerBot = panelY + panelH + 40;
        int containerL = panelX - 8;
        int containerR = panelX + panelW + 8;
        DrawContext.fill(containerL - 3, containerTop - 3, containerR + 3, containerTop, 855638016);
        DrawContext.fill(containerL - 3, containerBot, containerR + 3, containerBot + 3, 855638016);
        DrawContext.fill(containerL - 3, containerTop, containerL, containerBot, 855638016);
        DrawContext.fill(containerR, containerTop, containerR + 3, containerBot, 855638016);
        boolean editorLight = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
        int containerBg = editorLight ? ColorEditorConfig.editorLightContainerBg() : ColorEditorConfig.editorDarkContainerBg();
        int containerBorder = editorLight ? ColorEditorConfig.editorLightContainerBorder() : ColorEditorConfig.editorDarkContainerBorder();
        DrawContext.fill(containerL, containerTop, containerR, containerBot, containerBg);
        DrawContext.fill(containerL, containerTop, containerR, containerTop + 1, containerBorder);
        DrawContext.fill(containerL, containerBot - 1, containerR, containerBot, containerBorder);
        DrawContext.fill(containerL, containerTop, containerL + 1, containerBot, containerBorder);
        DrawContext.fill(containerR - 1, containerTop, containerR, containerBot, containerBorder);
        int tabBarY = panelY - 30;
        int tabBarH = 22;
        int tabW = panelW / 4;
        int selectedTabX = panelX + this.activeTab * tabW;
        DrawContext.fill(selectedTabX + 10, tabBarY + tabBarH + 1, selectedTabX + tabW - 10, tabBarY + tabBarH + 3, -12877066);
        int dividerX = panelX + 160;
        DrawContext.fill(dividerX, partyY - 5, dividerX + 1, panelY + panelH - 5, -3418655);
        DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.party").getString(), partyX + 55, partyY - 13, labelColor, false);
        String editorTitle = Text.translatable("cobblemon_ts.editor.title_with_player", this.targetPlayerName).getString();
        int editorTitleW = this.textRenderer.getWidth(editorTitle);
        int editorTitleX = panelX + panelW - editorTitleW - 10;
        int maxLeftW = editorTitleX - editX - 20;
        if (!this.viewingAllTeam) {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                if (d.exists) {
                    String title = d.displayName.isEmpty() ? PokemonNameMap.speciesToDisplay(d.speciesName) : d.displayName;
                    String leftText = Text.translatable("cobblemon_ts.editor.label.editing", title + (d.shiny ? " \u2728" : "")).getString();
                    if (this.textRenderer.getWidth(leftText) > maxLeftW && maxLeftW > 20) {
                        String ellipsis = "...";

                        for(int availW = maxLeftW - this.textRenderer.getWidth(ellipsis); this.textRenderer.getWidth(leftText) > availW && leftText.length() > 4; leftText = leftText.substring(0, leftText.length() - 1)) {
                        }

                        leftText = leftText + ellipsis;
                    }

                    DrawContext.drawText(this.textRenderer, leftText, editX, editY - 13, labelColor, false);
                    DrawContext.drawText(this.textRenderer, editorTitle, editorTitleX, editY - 13, -12877066, false);
                } else {
                    DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.empty_slot").getString(), editX, editY - 13, -10193781, false);
                    DrawContext.drawText(this.textRenderer, editorTitle, editorTitleX, editY - 13, -12877066, false);
                }
            } else {
                DrawContext.drawText(this.textRenderer, editorTitle, editorTitleX, editY - 13, -12877066, false);
            }
        }

        if (this.activeTab == 0) {
            int rowY = editY + 18;
            int rowStep = 28;
            String[] labels = new String[]{
                Text.translatable("cobblemon_ts.editor.label.species").getString(),
                Text.translatable("cobblemon_ts.editor.label.level").getString(),
                Text.translatable("cobblemon_ts.editor.label.name").getString(),
                Text.translatable("cobblemon_ts.editor.label.nature").getString(),
                Text.translatable("cobblemon_ts.editor.label.ability").getString(),
                Text.translatable("cobblemon_ts.editor.label.friendship").getString(),
                Text.translatable("cobblemon_ts.editor.label.form").getString(),
                Text.translatable("cobblemon_ts.editor.label.held_item").getString(),
                Text.translatable("cobblemon_ts.editor.label.gender").getString(),
                Text.translatable("cobblemon_ts.editor.label.shiny").getString()
            };

            for(int i = 0; i < labels.length; ++i) {
                int y = rowY + rowStep * i;
                DrawContext.drawText(this.textRenderer, labels[i] + ":", editX, y + 4, labelColor, false);
            }

            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                int gv = this.parseOrDefault(this.genderField, 2);
                String genderText = " (" + (gv == 0 ? Text.translatable("cobblemon_ts.editor.gender.male").getString() : (gv == 1 ? Text.translatable("cobblemon_ts.editor.gender.female").getString() : Text.translatable("cobblemon_ts.editor.gender.none").getString())) + ")";
                // [线性-L10] 性别文字使用标签文字颜色，关闭阴影
                DrawContext.drawText(this.textRenderer, genderText, editX + 50 + 10 + 65 + 100, rowY + rowStep * 8 + 4, labelColor, false);
            }

            int iconX = editX + 50 + 10 + 100 + 4 + 16 + 8;
            int iconY = rowY + rowStep * 7;

            try {
                String heldZh = this.sumHeldItemField.getText();
                if (heldZh != null && !heldZh.trim().isEmpty()) {
                    String heldEn = PokemonNameMap.itemToEnglish(heldZh);
                    String rawId = heldEn;
                    if (heldEn.contains(":")) {
                        int idx = heldEn.lastIndexOf(":");
                        rawId = heldEn.substring(idx + 1);
                    }

                    rawId = rawId.trim().toLowerCase();
                    Item foundItem = null;

                    for (net.minecraft.registry.entry.RegistryEntry<Item> entry : Registries.ITEM.getIndexedEntries()) {
                    Identifier entryId = entry.getKey().get().getValue();
                    String id = entryId.getPath();
                    if (id.equalsIgnoreCase(rawId)) {
                        foundItem = entry.value();
                        break;
                    }
                    }

                    if (foundItem != null && foundItem != Items.AIR) {
                        ItemStack stack = new ItemStack(foundItem);
                        DrawContext.drawItem(stack, iconX, iconY + 1);
                        DrawContext.drawItemInSlot(this.textRenderer, stack, iconX, iconY + 1);
                        PokemonEditorDebug.logEditor("[Screen] heldItem icon: rendered '{}' (UI='{}'\u2192EN='{}') at ({},{})", new Object[]{rawId, heldZh, heldEn, iconX, iconY});
                    } else {
                        PokemonEditorDebug.warnEditor("[Screen] heldItem icon: NOT FOUND - UI='{}' \u2192 EN='{}' \u2192 rawId='{}'", new Object[]{heldZh, heldEn, rawId});
                    }
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("[Screen] heldItem icon render error: {}", new Object[]{t.getMessage()});
            }

            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                if (d.exists) {
                    int cardX = editX + 265;
                    int cardY = rowY - 5;
                    int cardW = 130;
                    int cardH = 120;
                    int frameColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -3418655 : -12102295;
                    int bgColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -197122 : -14800581;
                    int topBarColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -12877066 : -14326805;
                    int cardLabelColor = labelColor;
                    int cardValueColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? ColorEditorConfig.editorLightInputText() : ColorEditorConfig.editorDarkInputText();
                    DrawContext.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, frameColor);
                    DrawContext.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);
                    DrawContext.fill(cardX, cardY, cardX + cardW, cardY + 22, topBarColor);
                    String zhName = d.displayName.isEmpty() ? PokemonNameMap.speciesToDisplay(d.speciesName) : d.displayName;
                    String cardTitle = zhName + (d.shiny ? " \u2728" : "");
                    DrawContext.drawText(this.textRenderer, cardTitle, cardX + cardW / 2 - this.textRenderer.getWidth(cardTitle) / 2, cardY + 7, -1, true);
                    int infoY = cardY + 28;
                    int infoX = cardX + 6;
                    String natureEffect = PokemonNameMap.natureEffectText(d.nature);
                    String natureName = PokemonNameMap.natureToDisplay(d.nature);
                    String natureDisp = natureName + natureEffect;
                    if (natureDisp.length() > 16) {
                        natureDisp = natureDisp.substring(0, 16);
                    }

                    String[][] info = new String[][]{
                        {Text.translatable("cobblemon_ts.editor.label.species").getString(), d.speciesName},
                        {Text.translatable("cobblemon_ts.editor.label.level").getString(), "Lv." + d.level},
                        {Text.translatable("cobblemon_ts.editor.label.gender").getString(), d.gender == 0 ? Text.translatable("cobblemon_ts.editor.gender.male_short").getString() : (d.gender == 1 ? Text.translatable("cobblemon_ts.editor.gender.female_short").getString() : Text.translatable("cobblemon_ts.editor.gender.none_short").getString())},
                        {Text.translatable("cobblemon_ts.editor.label.nature").getString(), natureDisp},
                        {Text.translatable("cobblemon_ts.editor.label.ability").getString(), PokemonNameMap.abilityToDisplay(d.ability)},
                        {Text.translatable("cobblemon_ts.editor.label.form").getString(), PokemonNameMap.formToChinese(d.speciesName, d.formName)}
                    };

                    // [线性-L18] 动态计算标签最大宽度，防止英文本地化后标签与值重叠
                    int maxLabelW = 0;
                    for (int j = 0; j < info.length; j++) {
                        int w = this.textRenderer.getWidth(info[j][0] + ":");
                        if (w > maxLabelW) maxLabelW = w;
                    }
                    int valueX = infoX + maxLabelW + 4;

                    for(int i = 0; i < info.length; ++i) {
                        int yy = infoY + i * 15;
                        DrawContext.drawText(this.textRenderer, info[i][0] + ":", infoX, yy, cardLabelColor, false);
                        String val = info[i][1];
                        if (val != null && val.length() > 16) {
                            val = val.substring(0, 16);
                        }

                        DrawContext.drawText(this.textRenderer, val == null ? "" : val, valueX, yy, cardValueColor, false);
                    }
                }
            }

            int fieldLabelW = 50;
            int fieldX = editX + fieldLabelW + 10;
            int natureY = rowY + rowStep * 3;
            int abilityY = rowY + rowStep * 4;
            int formY = rowY + rowStep * 6;
            int dropdownY = -1;
            int activeX = fieldX;
            int activeW = 230;
            String q = "";
            int newActiveField = -1;
            if (this.sumSpeciesField.isFocused()) {
                dropdownY = rowY + 20;
                activeX = this.sumSpeciesField.getX();
                activeW = this.sumSpeciesField.getWidth();
                q = this.sumSpeciesField.getText().trim().toLowerCase(Locale.ROOT);
                newActiveField = 0;
            } else if (this.sumNatureField.isFocused()) {
                dropdownY = natureY + 20;
                activeX = this.sumNatureField.getX();
                activeW = this.sumNatureField.getWidth();
                q = this.sumNatureField.getText().trim();
                newActiveField = 1;
            } else if (this.sumAbilityField.isFocused()) {
                dropdownY = abilityY + 20;
                activeX = this.sumAbilityField.getX();
                activeW = this.sumAbilityField.getWidth();
                q = this.sumAbilityField.getText().trim();
                newActiveField = 2;
            } else if (this.sumFormField.isFocused()) {
                dropdownY = formY + 20;
                activeX = this.sumFormField.getX();
                activeW = this.sumFormField.getWidth();
                q = this.sumFormField.getText().trim();
                newActiveField = 7;
            } else {
                this.autocompleteStartY = 0;
                this.autocompleteItems.clear();
                this.autocompleteScrollOffset = 0;
                this.autocompleteActiveField = -1;
            }

            if (dropdownY > 0) {
                this.autocompleteStartY = dropdownY;
                this.autocompleteX = activeX;
                this.autocompleteW = activeW;
                boolean searchChanged = newActiveField != this.autocompleteActiveField || !q.equals(this.lastSearchQuery);
                if (searchChanged) {
                    this.autocompleteItems.clear();
                    this.autocompleteScrollOffset = 0;
                    if (newActiveField == 0) {
                        List<String> species = PokemonNameMap.speciesDisplayList();
                        if (q.isEmpty()) {
                            this.autocompleteItems.addAll(species);
                        } else {
                            if (q.matches("\\d+")) {
                                int num = Integer.parseInt(q);
                                String zh = PokemonNameMap.getSpeciesByNumber(num);
                                if (zh != null) {
                                    this.autocompleteItems.add(zh);
                                }

                                try {
                                    Map<Integer, String> numMap = CobblemonBridge.getNationalDexNumberMap();

                                    for(Map.Entry<Integer, String> e : numMap.entrySet()) {
                                        int dexNum = (Integer)e.getKey();
                                        if (dexNum != num && String.valueOf(dexNum).startsWith(q)) {
                                            String z = PokemonNameMap.speciesToDisplay((String)e.getValue());
                                            if (z != null && !this.autocompleteItems.contains(z)) {
                                                this.autocompleteItems.add(z);
                                            }
                                        }
                                    }
                                } catch (Throwable var85) {
                                }
                            }

                            for(String zh : species) {
                                String enName = PokemonNameMap.toEnglish(zh);
                                if ((zh.contains(q) || enName != null && enName.contains(q)) && !this.autocompleteItems.contains(zh)) {
                                    this.autocompleteItems.add(zh);
                                }

                                if (!this.autocompleteItems.contains(zh) && CobblemonBridge.pinyinContains(zh, q)) {
                                    this.autocompleteItems.add(zh);
                                }
                            }
                        }
                    } else if (newActiveField == 1) {
                        List<String> natures = PokemonNameMap.natureDisplayList();
                        if (q.isEmpty()) {
                            this.autocompleteItems.addAll(natures);
                        } else {
                            for (String display : natures) {
                                if (display.contains(q)) {
                                    this.autocompleteItems.add(display);
                                }
                            }
                        }
                    } else if (newActiveField == 2) {
                        List<String> abilities = PokemonNameMap.abilityDisplayList();
                        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                            if (d.exists && d.possibleAbilities != null && !d.possibleAbilities.isEmpty()) {
                                List<String> filteredAbilities = new ArrayList();

                                for(String enAbility : d.possibleAbilities) {
                                    String display = PokemonNameMap.abilityToDisplay(enAbility);
                                    if (display != null && !display.isEmpty() && !filteredAbilities.contains(display)) {
                                        filteredAbilities.add(display);
                                    }
                                }

                                if (!filteredAbilities.isEmpty()) {
                                    abilities = filteredAbilities;
                                }
                            }
                        }

                        if (q.isEmpty()) {
                            this.autocompleteItems.addAll(abilities);
                        } else {
                            for(String zh : abilities) {
                                if (zh.contains(q)) {
                                    this.autocompleteItems.add(zh);
                                }
                            }
                        }
                    } else if (newActiveField == 7) {
                        // [线性-Lform-dropdown] 形态下拉：读取当前物种的所有形态并本地化
                        List<String> forms = new ArrayList<>();
                        forms.add(PokemonNameMap.formToDisplay(null, null));
                        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                            if (d.exists && d.speciesName != null && !d.speciesName.isEmpty()) {
                                String enName = PokemonNameMap.toEnglish(this.sumSpeciesField.getText());
                                if (enName != null && !enName.isEmpty()) {
                                    List<String> rawForms = CobblemonBridge.getSpeciesForms(enName);
                                    for (String raw : rawForms) {
                                        if (raw.equals("normal")) continue; // 普通已添加
                                        String display = PokemonNameMap.formToDisplay(enName, raw);
                                        if (!forms.contains(display)) forms.add(display);
                                    }
                                }
                            }
                        }
                        if (q.isEmpty()) {
                            this.autocompleteItems.addAll(forms);
                        } else {
                            for (String display : forms) {
                                if (display.contains(q)) {
                                    this.autocompleteItems.add(display);
                                }
                            }
                        }
                    }
                }

                this.autocompleteActiveField = newActiveField;
                this.lastSearchQuery = q;
            }
        } else if (this.activeTab == 2) {
            int ivStartY = editY + 40;
            int statRowH = 28;
            int ivColX = editX + 10;
            int evColX = editX + 180;
            String ivTitle = "IV (0-31)";
            DrawContext.drawText(this.textRenderer, ivTitle, ivColX + 60 - this.textRenderer.getWidth(ivTitle) / 2, editY + 15, -15681151, false);
            String evTitle = "EV (0-252)";
            DrawContext.drawText(this.textRenderer, evTitle, evColX + 60 - this.textRenderer.getWidth(evTitle) / 2, editY + 15, -12877066, false);
            String[] statLabels = new String[]{
                Text.translatable("cobblemon_ts.editor.stat.hp").getString(),
                Text.translatable("cobblemon_ts.editor.stat.attack").getString(),
                Text.translatable("cobblemon_ts.editor.stat.defense").getString(),
                Text.translatable("cobblemon_ts.editor.stat.sp_atk").getString(),
                Text.translatable("cobblemon_ts.editor.stat.sp_def").getString(),
                Text.translatable("cobblemon_ts.editor.stat.speed").getString()
            };

            for(int i = 0; i < statLabels.length; ++i) {
                int y = ivStartY + statRowH * i;
                DrawContext.drawText(this.textRenderer, statLabels[i] + ":", ivColX, y + 4, labelColor, false);
                DrawContext.drawText(this.textRenderer, statLabels[i] + ":", evColX, y + 4, labelColor, false);
            }

            int evSum = 0;

            for(TextFieldWidget b : new TextFieldWidget[]{this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe}) {
                evSum += this.parseOrDefault(b, 0);
            }

            int sumY = ivStartY + statRowH * 6 - 4;
            int evCap = CobblemonTSConfig.getEVTotalCap();
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.ev_sum", evSum, evCap).getString(), evColX, sumY, evSum <= evCap ? -12877066 : -1096636, false);
            if (this.selectedSlot >= 0 && this.crownVisible) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                int bgColor = d.goldenBottleCap ? -11141291 : -43691;
                Identifier crownIcon = Identifier.of("cobblemon_ts", "textures/item/gold_crown.png");
                DrawContext.getMatrices().push();
                DrawContext.getMatrices().translate(0.0F, 0.0F, 400.0F);
                DrawContext.drawTexture(crownIcon, this.crownIconX, this.crownIconY, 0.0F, 0.0F, 20, 20, 20, 20);
                DrawContext.getMatrices().pop();
                DrawContext.fill(this.crownToggleX, this.crownToggleY, this.crownToggleX + 20, this.crownToggleY + 20, -16777216 | bgColor);
                String toggleText = d.goldenBottleCap ? "ON" : "OFF";
                int textW = this.textRenderer.getWidth(toggleText);
                DrawContext.drawText(this.textRenderer, toggleText, this.crownToggleX + (20 - textW) / 2, this.crownToggleY + 6, -1, false);
            }

            if (this.selectedSlot >= 0 && this.hiddenPowerType != null && !this.hiddenPowerType.isEmpty()) {
                int hpCalcY = ivStartY + statRowH * 6 + 16 + 28;
                String zhName = this.getHiddenPowerTypeZh();
                String display = zhName + " ";
                int textW = this.textRenderer.getWidth(display);
                int textX = ivColX + 5 + 80 + 10;
                DrawContext.drawText(this.textRenderer, display, textX, hpCalcY + 2, labelColor, false);

                try {
                    Object elementalType = Class.forName("com.cobblemon.mod.common.api.types.ElementalTypes").getMethod("get", String.class).invoke((Object)null, this.hiddenPowerType);
                    if (elementalType != null) {
                        int texX = (Integer)elementalType.getClass().getMethod("getTextureXMultiplier").invoke(elementalType);
                        Identifier sheet = Identifier.of("cobblemon", "textures/gui/types_small.png");
                        DrawContext.getMatrices().push();
                        DrawContext.getMatrices().translate(0.0F, 0.0F, 400.0F);
                        DrawContext.drawTexture(sheet, textX + textW, hpCalcY + 1, (float)(texX * 18), 0.0F, 18, 18, 324, 18);
                        DrawContext.getMatrices().pop();
                    }
                } catch (Exception var84) {
                }
            }
        } else if (this.activeTab == 1) {
            int battleY = editY + 15;
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.current_hp").getString(), editX, battleY + 4, labelColor, false);
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.max_hp").getString(), editX, battleY + 34, labelColor, false);
            int moveY = battleY + 70;
            int moveLabelW = 40;
            int moveW = 90;

            for(int i = 0; i < 4; ++i) {
                int y = moveY + 25 * i;
                DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.move", i + 1).getString(), editX, y + 4, labelColor, false);
            }

            int moveFieldX = editX + moveLabelW + 10;
            int clearBtnX = moveFieldX + moveW + 2;
            int ppLabelX = clearBtnX + 16 + 12;
            int ppFieldW = 50;
            int maxPPLabelX = ppLabelX + ppFieldW + 12;
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.pp").getString(), ppLabelX, moveY - 15, labelColor, false);
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.max_pp").getString(), maxPPLabelX, moveY - 15, -12877066, false);
            int move2Y = moveY + 25;
            int move3Y = moveY + 50;
            int move4Y = moveY + 75;
            int dropdownY = -1;
            int activeX = moveFieldX;
            int activeW = moveW;
            String q = "";
            int newActiveField = -1;
            if (this.move1Field.isFocused()) {
                dropdownY = moveY + 20;
                activeX = this.move1Field.getX();
                activeW = this.move1Field.getWidth();
                q = this.move1Field.getText().trim();
                newActiveField = 3;
            } else if (this.move2Field.isFocused()) {
                dropdownY = move2Y + 20;
                activeX = this.move2Field.getX();
                activeW = this.move2Field.getWidth();
                q = this.move2Field.getText().trim();
                newActiveField = 4;
            } else if (this.move3Field.isFocused()) {
                dropdownY = move3Y + 20;
                activeX = this.move3Field.getX();
                activeW = this.move3Field.getWidth();
                q = this.move3Field.getText().trim();
                newActiveField = 5;
            } else if (this.move4Field.isFocused()) {
                dropdownY = move4Y + 20;
                activeX = this.move4Field.getX();
                activeW = this.move4Field.getWidth();
                q = this.move4Field.getText().trim();
                newActiveField = 6;
            } else {
                this.autocompleteStartY = 0;
                this.autocompleteItems.clear();
                this.autocompleteScrollOffset = 0;
                this.autocompleteActiveField = -1;
            }

            if (dropdownY > 0) {
                this.autocompleteStartY = dropdownY;
                this.autocompleteX = activeX;
                this.autocompleteW = activeW;
                boolean searchChanged = newActiveField != this.autocompleteActiveField || !q.equals(this.lastSearchQuery);
                if (searchChanged) {
                    this.autocompleteItems.clear();
                    this.autocompleteScrollOffset = 0;
                    List<String> allMoves = PokemonNameMap.moveDisplayList();
                    if (q.isEmpty()) {
                        for (String display : allMoves) {
                            this.autocompleteItems.add(display);
                        }
                    } else {
                        String lowerQ = q.toLowerCase(Locale.ROOT);

                        for (String displayName : allMoves) {
                            String enName = PokemonNameMap.moveToEnglish(displayName);
                            if ((displayName.contains(q) || enName != null && enName.contains(lowerQ)) && !this.autocompleteItems.contains(displayName)) {
                                this.autocompleteItems.add(displayName);
                            }

                            if (!this.autocompleteItems.contains(displayName) && PokemonNameMap.isChineseLocale() && CobblemonBridge.pinyinContains(displayName, q)) {
                                this.autocompleteItems.add(displayName);
                            }
                        }
                    }
                }

                this.autocompleteActiveField = newActiveField;
                this.lastSearchQuery = q;
            }
        } else if (this.activeTab == 3) {
            int advY = editY + 15;
            if (!this.viewingAllTeam) {
                DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.experience").getString(), editX, advY + 4, labelColor, false);
                DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.uuid").getString(), editX, advY + 39, labelColor, false);
                DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.data_area").getString(), editX + 10, advY + 64, labelColor, false);
            } else {
                DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.all_team_data").getString(), editX + 10, editY + 4, labelColor, false);
            }
        }

        this.drawInputFieldBackgrounds(DrawContext, editorLight);
        // [线性-C03] 缩放鼠标坐标用于hover检测：传递给super.render()确保按钮高亮正确
        int renderMX = mouseX, renderMY = mouseY;
        if (this.uiScaleFactor < 1.0f) {
            renderMX = (int)((mouseX - this.width / 2.0) / this.uiScaleFactor + 320.0);
            renderMY = (int)((mouseY - this.height / 2.0) / this.uiScaleFactor + 233.5);
        }
        super.render(DrawContext, renderMX, renderMY, partialTick);
        if (this.activeTab == 3 && this.advRawField != null) {
            int boxX = this.dataAreaX;
            int boxY = this.dataAreaY + 25;
            int boxW = this.dataAreaW;
            int boxH = this.dataAreaH - 25;
            int padX = 4;
            int padY = 4;
            int lineH = 10;
            boolean bright = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
            int boxBg = bright ? ColorEditorConfig.editorLightInputBg() : ColorEditorConfig.editorDarkInputBg();
            int boxFrame = bright ? ColorEditorConfig.editorLightInputBorder() : ColorEditorConfig.editorDarkInputBorder();
            int boxTopFocus = bright ? ColorEditorConfig.editorLightTabActive() : ColorEditorConfig.editorDarkTabActive();
            int textColor = bright ? ColorEditorConfig.editorLightInputText() : ColorEditorConfig.editorDarkInputText();
            int cursorColor = textColor;
            int selectionBg = -12877066;
            int selectionFg = -1;
            DrawContext.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxBg);
            DrawContext.fill(boxX, boxY, boxX + boxW, boxY + 1, boxFrame);
            DrawContext.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, boxFrame);
            DrawContext.fill(boxX, boxY, boxX + 1, boxY + boxH, boxFrame);
            DrawContext.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, boxFrame);
            if (this.advRawField.isFocused()) {
                DrawContext.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + 2, boxTopFocus);
            }

            String text = this.advRawField.getText();
            if (text == null || text.isEmpty()) {
                if (this.advRawField.isFocused() && System.currentTimeMillis() / 500L % 2L == 0L) {
                    DrawContext.fill(boxX + padX, boxY + padY - 1, boxX + padX + 1, boxY + padY + lineH - 2, textColor);
                }

                return;
            }

            int maxVisibleLines = (boxH - padY * 2) / lineH;
            int contentMaxW = boxW - padX * 2;
            MultilineLayout layout = this.layoutMultiline(text, contentMaxW);
            List<String> lines = layout.displayLines;
            List<Integer> charOffsets = layout.charOffsets;
            int cursorPos = this.advRawField.getCursor();
            if (cursorPos < 0) {
                cursorPos = 0;
            }

            if (cursorPos > text.length()) {
                cursorPos = text.length();
            }

            int cursorDisplayLine = 0;
            int cursorDisplayCol = 0;

            for(int li = 0; li < lines.size(); ++li) {
                int lineStart = (Integer)charOffsets.get(li);
                int lineEnd = li + 1 < lines.size() ? (Integer)charOffsets.get(li + 1) : text.length();
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

            if (cursorDisplayCol < 0) {
                cursorDisplayCol = 0;
            }

            if (cursorDisplayCol > ((String)lines.get(cursorDisplayLine)).length()) {
                cursorDisplayCol = ((String)lines.get(cursorDisplayLine)).length();
            }

            // [线性-A1] 通过EditBoxAccessor Mixin获取私有字段highlightPos
            int highlightPos = ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).getHighlightPos();

            if (highlightPos < 0) {
                highlightPos = 0;
            }

            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            int selStart = Math.min(cursorPos, highlightPos);
            int selEnd = Math.max(cursorPos, highlightPos);
            boolean hasSelection = selEnd > selStart;
            int selStartDisplayLine = 0;
            int selStartDisplayCol = 0;
            int selEndDisplayLine = 0;
            int selEndDisplayCol = 0;
            if (hasSelection) {
                for(int li = 0; li < lines.size(); ++li) {
                    int lineStart = (Integer)charOffsets.get(li);
                    int lineEnd = li + 1 < lines.size() ? (Integer)charOffsets.get(li + 1) : text.length();
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

                for(int li = 0; li < lines.size(); ++li) {
                    int lineStart = (Integer)charOffsets.get(li);
                    int lineEnd = li + 1 < lines.size() ? (Integer)charOffsets.get(li + 1) : text.length();
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

            int maxOffset = Math.max(0, lines.size() - maxVisibleLines);
            if (this.advRawField.isFocused()) {
                if (cursorDisplayLine < this.advRawScrollOffset) {
                    this.advRawScrollOffset = cursorDisplayLine;
                } else if (cursorDisplayLine >= this.advRawScrollOffset + maxVisibleLines) {
                    this.advRawScrollOffset = cursorDisplayLine - maxVisibleLines + 1;
                }
            }

            if (this.advRawScrollOffset > maxOffset) {
                this.advRawScrollOffset = maxOffset;
            }

            if (this.advRawScrollOffset < 0) {
                this.advRawScrollOffset = 0;
            }

            int scrollOffset = this.advRawScrollOffset;

            for(int i = 0; i < maxVisibleLines && i + scrollOffset < lines.size(); ++i) {
                int displayLineIdx = i + scrollOffset;
                int drawY = boxY + padY + i * lineH;
                String line = (String)lines.get(displayLineIdx);
                if (line == null) {
                    line = "";
                }

                boolean lineSelected = hasSelection && this.advRawField.isFocused() && displayLineIdx >= selStartDisplayLine && displayLineIdx <= selEndDisplayLine;
                if (lineSelected) {
                    boolean isStartLine = displayLineIdx == selStartDisplayLine;
                    boolean isEndLine = displayLineIdx == selEndDisplayLine;
                    int drawX = boxX + padX;
                    int col = 0;
                    if (isStartLine && selStartDisplayCol > 0) {
                        String pre = line.substring(0, Math.min(selStartDisplayCol, line.length()));
                        DrawContext.drawText(this.textRenderer, pre, drawX, drawY, textColor, false);
                        drawX += this.textRenderer.getWidth(pre);
                        col = selStartDisplayCol;
                    }

                    int selEndCol = isEndLine ? selEndDisplayCol : line.length();
                    if (selEndCol > col) {
                        String selText = line.substring(Math.min(col, line.length()), Math.min(selEndCol, line.length()));
                        int bgW = this.textRenderer.getWidth(selText);
                        DrawContext.fill(drawX, drawY - 1, drawX + bgW, drawY + lineH - 2, selectionBg);
                        DrawContext.drawText(this.textRenderer, selText, drawX, drawY, selectionFg, false);
                        drawX += bgW;
                        col = selEndCol;
                    }

                    if (isEndLine && col < line.length()) {
                        String post = line.substring(col);
                        DrawContext.drawText(this.textRenderer, post, drawX, drawY, textColor, false);
                    }
                } else {
                    DrawContext.drawText(this.textRenderer, line, boxX + padX, drawY, textColor, false);
                }

                if (this.advRawField.isFocused() && displayLineIdx == cursorDisplayLine) {
                    String upToCursor = line.substring(0, Math.min(cursorDisplayCol, line.length()));
                    int cursorX = boxX + padX + this.textRenderer.getWidth(upToCursor);
                    int cursorTopY = drawY - 1;
                    int cursorBotY = drawY + lineH - 2;
                    if (System.currentTimeMillis() / 500L % 2L == 0L) {
                        DrawContext.fill(cursorX, cursorTopY, cursorX + 1, cursorBotY, cursorColor);
                    }
                }
            }
        }

        // [线性-Z01] 推Z轴确保下拉框在最上层，避免下方输入框文字穿透
        if ((this.activeTab == 0 || this.activeTab == 1) && !this.autocompleteItems.isEmpty() && this.autocompleteStartY > 0) {
            DrawContext.getMatrices().push();
            DrawContext.getMatrices().translate(0, 0, 300);
            int lineH = 16;
            int maxShown = 8;
            int shown = Math.min(this.autocompleteItems.size(), maxShown);
            int maxOffset = Math.max(0, this.autocompleteItems.size() - maxShown);
            if (this.autocompleteScrollOffset > maxOffset) {
                this.autocompleteScrollOffset = maxOffset;
            }

            if (this.autocompleteScrollOffset < 0) {
                this.autocompleteScrollOffset = 0;
            }

            int listH = shown * lineH;
            int sfX = this.autocompleteX;
            int sY = this.autocompleteStartY;
            DrawContext.fill(sfX - 2, sY - 2, sfX + this.autocompleteW + 2, sY + listH + 2, -1728053248);
            DrawContext.fill(sfX - 1, sY - 1, sfX + this.autocompleteW + 1, sY + listH + 1, -4667430);
            DrawContext.fill(sfX, sY, sfX + this.autocompleteW, sY + listH, -197122);

            for(int i = 0; i < shown; ++i) {
                int itemIdx = this.autocompleteScrollOffset + i;
                if (itemIdx < 0 || itemIdx >= this.autocompleteItems.size()) {
                    break;
                }

                int itemY = sY + i * lineH;
                if (mouseX >= sfX && mouseX < sfX + this.autocompleteW && mouseY >= itemY && mouseY < itemY + lineH) {
                    DrawContext.fill(sfX, itemY, sfX + this.autocompleteW, itemY + lineH, -12877066);
                    DrawContext.drawText(this.textRenderer, (String)this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -1, false);
                } else {
                    DrawContext.drawText(this.textRenderer, (String)this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -14800581, false);
                }
            }

            if (this.autocompleteItems.size() > maxShown) {
                int arrowX = sfX + this.autocompleteW - 12;
                if (this.autocompleteScrollOffset > 0) {
                    DrawContext.drawText(this.textRenderer, "\u2191", arrowX, sY - 12, -12877066, false);
                }

                if (this.autocompleteScrollOffset < maxOffset) {
                    DrawContext.drawText(this.textRenderer, "\u2193", arrowX, sY + listH + 2, -12877066, false);
                }
            }
            DrawContext.getMatrices().pop();
        }

        if (this.showImportDialog && this.pendingImportFiles != null) {
            int dlgW = 480;
            int dlgH = 360;
            int dlgX = (this.width - dlgW) / 2;
            int dlgY = (this.height - dlgH) / 2;
            boolean bright = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
            int dlgBg = bright ? -920071 : -15788246;
            int dlgFrame = bright ? -7035976 : -13418155;
            int dlgTitleBg = bright ? -12877066 : -14326805;
            int dlgText = bright ? -15788246 : -1906448;
            int dlgItemBg = bright ? -197122 : -14800581;
            int dlgHoverBg = bright ? -2364674 : -13418155;
            int dlgSubText = bright ? -12102295 : -7035976;
            DrawContext.fill(0, 0, this.width, this.height, -2013265920);
            DrawContext.fill(dlgX - 2, dlgY - 2, dlgX + dlgW + 2, dlgY + dlgH + 2, -16777216);
            DrawContext.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, dlgBg);
            DrawContext.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 28, dlgTitleBg);
            DrawContext.drawText(this.textRenderer, Text.translatable("cobblemon_ts.editor.label.import_dialog").getString(), dlgX + 12, dlgY + 10, -1, true);
            int closeX = dlgX + dlgW - 28;
            int closeY = dlgY + 6;
            DrawContext.fill(closeX, closeY, closeX + 22, closeY + 16, -4645860);
            DrawContext.drawText(this.textRenderer, "X", closeX + 8, closeY + 4, -1, true);
            int listX = dlgX + 10;
            int listY = dlgY + 38;
            int listW = dlgW - 20;
            int itemH = 44;
            int maxShown = Math.min(7, this.pendingImportFiles.length);
            int maxScroll = Math.max(0, this.pendingImportFiles.length - maxShown);
            if (this.importDialogScroll > maxScroll) {
                this.importDialogScroll = maxScroll;
            }

            if (this.importDialogScroll < 0) {
                this.importDialogScroll = 0;
            }

            for(int i = 0; i < maxShown; ++i) {
                int fileIdx = this.importDialogScroll + i;
                if (fileIdx < 0 || fileIdx >= this.pendingImportFiles.length) {
                    break;
                }

                File f = this.pendingImportFiles[fileIdx];
                int itemY = listY + i * itemH;
                boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= itemY && mouseY < itemY + itemH - 2;
                DrawContext.fill(listX, itemY, listX + listW, itemY + itemH - 2, hover ? dlgHoverBg : dlgItemBg);
                String fName = f.getName();
                DrawContext.drawText(this.textRenderer, fName, listX + 8, itemY + 6, dlgText, true);
                long bytes = f.length();
                String sizeStr;
                if (bytes < 1024L) {
                    sizeStr = bytes + " B";
                } else if (bytes < 1048576L) {
                    sizeStr = String.format("%.1f KB", (double)bytes / 1024.0);
                } else {
                    sizeStr = String.format("%.1f MB", (double)bytes / 1048576.0);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateStr = sdf.format(new Date(f.lastModified()));
                DrawContext.drawText(this.textRenderer, sizeStr + "   |   " + dateStr + "   |   " + f.getParent(), listX + 8, itemY + 22, dlgSubText, true);
            }

            if (this.pendingImportFiles.length > maxShown) {
                int aX = listX + listW - 14;
                if (this.importDialogScroll > 0) {
                    DrawContext.drawText(this.textRenderer, "\u2191", aX, listY - 14, dlgText, true);
                }

                if (this.importDialogScroll < maxScroll) {
                    DrawContext.drawText(this.textRenderer, "\u2193", aX, listY + maxShown * itemH - 12, dlgText, true);
                }
            }
        }

        // [线性-C02] 弹出缩放矩阵
        if (this.uiScaleFactor < 1.0f) {
            DrawContext.getMatrices().pop();
        }
    }

    private MultilineLayout layoutMultiline(String text, int maxWidth) {
        List<String> displayLines = new ArrayList();
        List<Integer> charOffsets = new ArrayList();
        if (text == null) {
            text = "";
        }

        String[] rawLines = text.split("\n", -1);
        int pos = 0;

        for(int ri = 0; ri < rawLines.length; ++ri) {
            String raw = rawLines[ri];
            if (raw.isEmpty()) {
                displayLines.add("");
                charOffsets.add(pos);
                ++pos;
            } else {
                StringBuilder cur = new StringBuilder();
                int curW = 0;
                int lineStart = pos;

                for(int i = 0; i < raw.length(); ++i) {
                    char c = raw.charAt(i);
                    int cw = this.textRenderer.getWidth(String.valueOf(c));
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
                pos += raw.length() + 1;
            }
        }

        return new MultilineLayout(displayLines, charOffsets);
    }

    private int advRawFieldPosFromMouse(double mx, double my) {
        if (this.advRawField == null) {
            return 0;
        } else {
            int boxX = this.dataAreaX;
            int boxY = this.dataAreaY + 25;
            int boxW = this.dataAreaW;
            int boxH = this.dataAreaH - 25;
            int padX = 4;
            int padY = 4;
            int lineH = 10;
            String text = this.advRawField.getText();
            if (text == null) {
                text = "";
            }

            int contentMaxW = boxW - padX * 2;
            int maxVisibleLines = (boxH - padY * 2) / lineH;
            MultilineLayout layout = this.layoutMultiline(text, contentMaxW);
            List<String> lines = layout.displayLines;
            List<Integer> offsets = layout.charOffsets;
            int totalLines = lines.size();
            int scrollOffset = this.advRawScrollOffset;
            int maxOffset = Math.max(0, totalLines - maxVisibleLines);
            if (scrollOffset > maxOffset) {
                scrollOffset = maxOffset;
            }

            if (scrollOffset < 0) {
                scrollOffset = 0;
            }

            int relY = (int)my - boxY - padY;
            if (relY < 0) {
                relY = 0;
            }

            int visibleLineIdx = relY / lineH;
            if (visibleLineIdx < 0) {
                visibleLineIdx = 0;
            }

            if (visibleLineIdx >= maxVisibleLines) {
                visibleLineIdx = maxVisibleLines - 1;
            }

            int lineIdx = visibleLineIdx + scrollOffset;
            if (lineIdx < 0) {
                lineIdx = 0;
            }

            PokemonEditorDebug.logEditor("[Mouse-Click] mx={} my={} box=[{},{},{},{}] relY={} visibleLine={} scrollOffset={} lineIdx={}/{} textLen={}", new Object[]{(int)mx, (int)my, boxX, boxY, boxW, boxH, relY, visibleLineIdx, scrollOffset, lineIdx, totalLines, text.length()});
            if (lineIdx >= lines.size()) {
                PokemonEditorDebug.logEditor("[Mouse-Click] -> end: {}", new Object[]{text.length()});
                return text.length();
            } else {
                int lineStart = (Integer)offsets.get(lineIdx);
                String line = (String)lines.get(lineIdx);
                int relX = (int)mx - boxX - padX;
                if (relX < 0) {
                    relX = 0;
                }

                int accW = 0;
                int charInLine = 0;

                for(int i = 0; i < line.length(); ++i) {
                    int cw = this.textRenderer.getWidth(String.valueOf(line.charAt(i)));
                    if (accW + cw / 2 > relX) {
                        break;
                    }

                    accW += cw;
                    charInLine = i + 1;
                }

                int result = Math.min(text.length(), lineStart + charInLine);
                PokemonEditorDebug.logEditor("[Mouse-Click] -> lineStart={} charInLine={} result={}", new Object[]{lineStart, charInLine, result});
                return result;
            }
        }
    }

    private boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY, double delta, String source) {
        PokemonEditorDebug.log("[Scroll-" + source + "] mouseX=" + (int)mouseX + " mouseY=" + (int)mouseY + " scrollX=" + scrollX + " scrollY=" + scrollY + " delta=" + delta + " activeTab=" + this.activeTab);
        if (this.showImportDialog && this.pendingImportFiles != null) {
            int maxShown = Math.min(7, this.pendingImportFiles.length);
            int maxScroll = Math.max(0, this.pendingImportFiles.length - maxShown);
            double effectiveScroll = Math.abs(scrollY) > 0.01 ? scrollY : delta;
            if (effectiveScroll > 0.0) {
                this.importDialogScroll = Math.max(0, this.importDialogScroll - 1);
            } else if (effectiveScroll < 0.0) {
                this.importDialogScroll = Math.min(maxScroll, this.importDialogScroll + 1);
            }

            PokemonEditorDebug.log("[Scroll-ImportDialog] scroll=" + effectiveScroll + " newScroll=" + this.importDialogScroll + " max=" + maxScroll);
            return true;
        } else if ((this.activeTab == 0 || this.activeTab == 1) && !this.autocompleteItems.isEmpty() && this.autocompleteStartY > 0) {
            int var10000 = this.autocompleteItems.size();
            PokemonEditorDebug.log("[Scroll-Dropdown] items=" + var10000 + " offset=" + this.autocompleteScrollOffset + " scrollY=" + scrollY + " delta=" + delta);
            int maxShown = 8;
            int maxOffset = Math.max(0, this.autocompleteItems.size() - maxShown);
            double effectiveScroll = Math.abs(scrollY) > 0.01 ? scrollY : delta;
            if (effectiveScroll > 0.0) {
                this.autocompleteScrollOffset = Math.max(0, this.autocompleteScrollOffset - 2);
            } else if (effectiveScroll < 0.0) {
                this.autocompleteScrollOffset = Math.min(maxOffset, this.autocompleteScrollOffset + 2);
            }

            PokemonEditorDebug.log("[Scroll-Dropdown] new offset=" + this.autocompleteScrollOffset + " max=" + maxOffset);
            return true;
        } else {
            if (this.activeTab == 3 && this.advRawField != null) {
                int boxX = this.dataAreaX;
                int boxY = this.dataAreaY;
                int boxW = this.dataAreaW;
                int boxH = this.dataAreaH;
                int contentBoxH = this.dataAreaH - 25;
                PokemonEditorDebug.logEditor("[Scroll-DataArea] check: mouseX={} mouseY={} box=[{},{},{},{}]", new Object[]{(int)mouseX, (int)mouseY, boxX, boxY, boxW, boxH});
                if (mouseX >= (double)boxX && mouseX < (double)(boxX + boxW) && mouseY >= (double)boxY && mouseY < (double)(boxY + boxH)) {
                    int padY = 4;
                    int lineH = 10;
                    int contentMaxW = boxW - 8;
                    int maxVisibleLines = (contentBoxH - padY * 2) / lineH;
                    String text = this.advRawField.getText();
                    if (text == null) {
                        text = "";
                    }

                    MultilineLayout layout = this.layoutMultiline(text, contentMaxW);
                    List<Integer> charOffsets = layout.charOffsets;
                    int totalLines = layout.displayLines.size();
                    int maxOffset = Math.max(0, totalLines - maxVisibleLines);
                    int scrollStep = 3;
                    double effectiveScroll = Math.abs(scrollY) > 0.01 ? scrollY : delta;
                    if (effectiveScroll > 0.0) {
                        this.advRawScrollOffset = Math.max(0, this.advRawScrollOffset - scrollStep);
                        PokemonEditorDebug.log("[Scroll-DataArea] UP: offset=" + this.advRawScrollOffset + "/" + maxOffset);
                    } else if (effectiveScroll < 0.0) {
                        this.advRawScrollOffset = Math.min(maxOffset, this.advRawScrollOffset + scrollStep);
                        PokemonEditorDebug.log("[Scroll-DataArea] DOWN: offset=" + this.advRawScrollOffset + "/" + maxOffset);
                    }

                    if (!charOffsets.isEmpty()) {
                        int targetLine = Math.min(this.advRawScrollOffset, charOffsets.size() - 1);
                        int charPos = (Integer)charOffsets.get(targetLine);
                        ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(charPos);
                        this.advRawField.setSelectionEnd(charPos);
                    }

                    return true;
                }
            }

            PokemonEditorDebug.log("[Scroll-" + source + "] not handled, passing to super");
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // [线性-C02] 缩放鼠标坐标：逆变换屏幕坐标→逻辑坐标，围绕布局中心(320, 233.5)
        double mx = mouseX, my = mouseY;
        if (this.uiScaleFactor < 1.0f) {
            mx = (mouseX - this.width / 2.0) / this.uiScaleFactor + 320.0;
            my = (mouseY - this.height / 2.0) / this.uiScaleFactor + 233.5;
        }
        boolean handled = this.handleMouseScroll(mx, my, scrollX, scrollY, scrollY, "4param");
        return handled ? true : super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [线性-C02] 缩放鼠标坐标：逆变换屏幕坐标→逻辑坐标，围绕布局中心(320, 233.5)
        double mx = mouseX, my = mouseY;
        if (this.uiScaleFactor < 1.0f) {
            mx = (mouseX - this.width / 2.0) / this.uiScaleFactor + 320.0;
            my = (mouseY - this.height / 2.0) / this.uiScaleFactor + 233.5;
        }
        PokemonEditorDebug.logEditor("[Mouse-Click] entry activeTab={} mx={} my={} button={} advRawField={} dataArea=[{},{},{},{}]", new Object[]{this.activeTab, (int)mx, (int)my, button, this.advRawField != null ? "non-null" : "null", this.dataAreaX, this.dataAreaY, this.dataAreaW, this.dataAreaH});
        if (this.showImportDialog && this.pendingImportFiles != null) {
            int panelW = 480;
            int panelH = 360;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;
            int listX = panelX + 10;
            int listY = panelY + 38;
            int listW = panelW - 20;
            int itemH = 44;
            int maxShown = Math.min(7, this.pendingImportFiles.length);
            int imx = (int)mx;
            int imy = (int)my;
            int closeX = panelX + panelW - 28;
            int closeY = panelY + 6;
            if (imx >= closeX && imx < closeX + 22 && imy >= closeY && imy < closeY + 16) {
                this.showImportDialog = false;
                this.pendingImportFiles = null;
                return true;
            } else {
                for(int i = 0; i < maxShown; ++i) {
                    int fileIdx = this.importDialogScroll + i;
                    if (fileIdx < 0 || fileIdx >= this.pendingImportFiles.length) {
                        break;
                    }

                    int itemY = listY + i * itemH;
                    if (mx >= listX && mx < listX + listW && my >= itemY && my < itemY + itemH - 2) {
                        try {
                            String text = Files.readString(this.pendingImportFiles[fileIdx].toPath(), StandardCharsets.UTF_8);
                            this.advRawField.setText(text);
                            ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(0);
                            this.advRawField.setSelectionEnd(0);
                            this.advRawScrollOffset = 0;
                            if (this.client.player != null) {
                                File var10001 = this.pendingImportFiles[fileIdx];
                                this.client.player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.loaded", var10001.getName()));
                            }
                        } catch (Exception e) {
                            if (this.client.player != null) {
                                this.client.player.sendMessage(Text.translatable("cobblemon_ts.editor.msg.load_failed", e.getMessage()));
                            }
                        }

                        this.showImportDialog = false;
                        this.pendingImportFiles = null;
                        return true;
                    }
                }

                if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
                    this.showImportDialog = false;
                    this.pendingImportFiles = null;
                }

                return true;
            }
        } else {
            if (this.activeTab == 3 && this.advRawField != null && button == 0) {
                int boxX = this.dataAreaX;
                int boxY = this.dataAreaY;
                int boxW = this.dataAreaW;
                int boxH = this.dataAreaH;
                int imx2 = (int)mx;
                int imy2 = (int)my;
                PokemonEditorDebug.logEditor("[Mouse-Click-Check] activeTab={} button={} mx={} my={} box=[{},{},{},{}] inBox={}", new Object[]{this.activeTab, button, imx2, imy2, boxX, boxY, boxW, boxH, imx2 >= boxX && imx2 < boxX + boxW && imy2 >= boxY && imy2 < boxY + boxH});
                if (imx2 >= boxX && imx2 < boxX + boxW && imy2 >= boxY && imy2 < boxY + boxH) {
                    this.setFocused(this.advRawField);
                    this.advRawField.setFocused(true);
                    int pos = this.advRawFieldPosFromMouse(mx, my);
                    ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(pos);
                    this.advRawField.setSelectionEnd(pos);
                    PokemonEditorDebug.logEditor("[Mouse-Click-Apply] focused=true cursor={}/{}", new Object[]{pos, this.advRawField.getText().length()});
                    return true;
                }
            }

            if ((this.activeTab == 0 || this.activeTab == 1) && !this.autocompleteItems.isEmpty() && button == 0) {
                int lineH = 16;
                int maxShown = 8;
                int imx3 = (int)mx;
                int imy3 = (int)my;
                int listH = Math.min(this.autocompleteItems.size(), maxShown) * lineH;
                if (imx3 >= this.autocompleteX && imx3 < this.autocompleteX + this.autocompleteW && imy3 >= this.autocompleteStartY && imy3 < this.autocompleteStartY + listH) {
                    int idx = this.autocompleteScrollOffset + (imy3 - this.autocompleteStartY) / lineH;
                    if (idx >= 0 && idx < this.autocompleteItems.size()) {
                        String selected = (String)this.autocompleteItems.get(idx);
                        if (this.autocompleteActiveField == 0) {
                            this.sumSpeciesField.setText(selected);
                        } else if (this.autocompleteActiveField == 1) {
                            this.sumNatureField.setText(selected);
                        } else if (this.autocompleteActiveField == 2) {
                            this.sumAbilityField.setText(selected);
                        } else if (this.autocompleteActiveField == 3) {
                            this.move1Field.setText(selected);
                            this.applyMoveImmediate(0, selected);
                        } else if (this.autocompleteActiveField == 4) {
                            this.move2Field.setText(selected);
                            this.applyMoveImmediate(1, selected);
                        } else if (this.autocompleteActiveField == 5) {
                            this.move3Field.setText(selected);
                            this.applyMoveImmediate(2, selected);
                        } else if (this.autocompleteActiveField == 6) {
                            this.move4Field.setText(selected);
                            this.applyMoveImmediate(3, selected);
                        } else if (this.autocompleteActiveField == 7) {
                            this.sumFormField.setText(selected);
                        }

                        this.autocompleteItems.clear();
                        this.autocompleteScrollOffset = 0;
                        return true;
                    }
                }
            }

            if (!this.autocompleteItems.isEmpty()) {
                this.autocompleteItems.clear();
                this.autocompleteScrollOffset = 0;
                int af = this.autocompleteActiveField;
                this.autocompleteActiveField = -1;
                if (af == 0) this.sumSpeciesField.setFocused(false);
                else if (af == 1) this.sumNatureField.setFocused(false);
                else if (af == 2) this.sumAbilityField.setFocused(false);
                else if (af == 3) this.move1Field.setFocused(false);
                else if (af == 4) this.move2Field.setFocused(false);
                else if (af == 5) this.move3Field.setFocused(false);
                else if (af == 6) this.move4Field.setFocused(false);
                else if (af == 7) this.sumFormField.setFocused(false);
            }

            if (this.selectedSlot >= 0 && this.crownVisible) {
                int imx4 = (int)mx;
                int imy4 = (int)my;
                if (imx4 >= this.crownToggleX && imx4 < this.crownToggleX + 20 && imy4 >= this.crownToggleY && imy4 < this.crownToggleY + 20) {
                    PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                    d.goldenBottleCap = !d.goldenBottleCap;
                    d.markModified("goldenBottleCap");
                    return true;
                }
            }

            // [线性-F01] 右键点击性别切换按钮：向左循环（2→1→0→2）
            if (button == 1 && this.genderCycleBtn != null && this.genderCycleBtn.isMouseOver(mx, my)) {
                int cur = this.parseOrDefault(this.genderField, 2);
                int prev = (cur - 1 + 3) % 3; // 向左循环
                this.genderField.setText(String.valueOf(prev));
                return true;
            }

            // [线性-F01] 右键点击状态切换按钮：向左循环（无→剧毒→睡眠→冰冻→烧伤→麻痹→中毒→无）
            if (button == 1 && this.statusCycleBtn != null && this.statusCycleBtn.isMouseOver(mx, my)) {
                if (this.selectedSlot >= 0) {
                    this.cycleToPrevStatus();
                }
                return true;
            }

            return super.mouseClicked(mx, my, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // [线性-C02] 缩放鼠标坐标：逆变换屏幕坐标→逻辑坐标，围绕布局中心(320, 233.5)
        double mx = mouseX, my = mouseY;
        if (this.uiScaleFactor < 1.0f) {
            mx = (mouseX - this.width / 2.0) / this.uiScaleFactor + 320.0;
            my = (mouseY - this.height / 2.0) / this.uiScaleFactor + 233.5;
        }
        if (this.activeTab == 3 && this.advRawField != null && this.advRawField.isFocused() && button == 0) {
            int pos = this.advRawFieldPosFromMouse(mx, my);
            ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(pos);
            return true;
        } else {
            return super.mouseDragged(mx, my, button, dragX, dragY);
        }
    }

    public boolean charTyped(char ch, int modifiers) {
        if (this.advRawField != null && this.advRawField.isFocused() && this.activeTab == 3) {
            boolean handled = this.advRawField.charTyped(ch, modifiers);
            if (handled) {
                PokemonEditorDebug.logEditor("[Char-Typed] ch='{}' handled=true textLen={}", new Object[]{ch, this.advRawField.getText().length()});
            }

            return handled;
        } else {
            return super.charTyped(ch, modifiers);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PokemonEditorDebug.logEditor("[Key-Pressed] keyCode={} activeTab={} advRawFieldFocused={} advRawFieldValueLen={}", new Object[]{keyCode, this.activeTab, this.advRawField != null ? this.advRawField.isFocused() : "null", this.advRawField != null ? this.advRawField.getText().length() : 0});
        if (this.advRawField != null && this.advRawField.isFocused() && this.activeTab == 3) {
            String current = this.advRawField.getText();
            int cursor = this.advRawField.getCursor();
            if (current == null) {
                current = "";
            }

            if (cursor < 0) {
                cursor = 0;
            }

            if (cursor > current.length()) {
                cursor = current.length();
            }

            if (keyCode == 257 || keyCode == 335) {
                String before = current.substring(0, cursor);
                String after = current.substring(cursor);
                this.advRawField.setText(before + "\n" + after);
                ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(cursor + 1);
                this.advRawField.setSelectionEnd(cursor + 1);
                PokemonEditorDebug.logEditor("[Key-Enter] cursor={} newTextLen={}", new Object[]{cursor, before.length() + 1 + after.length()});
                return true;
            }

            if (keyCode == 264 || keyCode == 265) {
                int boxW = this.dataAreaW;
                int contentMaxW = boxW - 8;
                MultilineLayout layout = this.layoutMultiline(current, contentMaxW);
                List<String> lines = layout.displayLines;
                List<Integer> offsets = layout.charOffsets;
                if (lines.isEmpty()) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }

                int curDisplayLine = 0;
                int curCol = 0;

                for(int li = 0; li < lines.size(); ++li) {
                    int lineStart = (Integer)offsets.get(li);
                    int lineEnd = li + 1 < lines.size() ? (Integer)offsets.get(li + 1) : current.length();
                    if (cursor >= lineStart && cursor <= lineEnd) {
                        curDisplayLine = li;
                        curCol = cursor - lineStart;
                        break;
                    }
                }

                int targetLine;
                if (keyCode == 265) {
                    targetLine = Math.max(0, curDisplayLine - 1);
                } else {
                    targetLine = Math.min(lines.size() - 1, curDisplayLine + 1);
                }

                if (targetLine != curDisplayLine) {
                    int targetLineStart = (Integer)offsets.get(targetLine);
                    int targetLineLen = ((String)lines.get(targetLine)).length();
                    int targetCol = Math.min(curCol, targetLineLen);
                    int newCursor = targetLineStart + targetCol;
                    ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(newCursor);
                    this.advRawField.setSelectionEnd(newCursor);
                    int maxVisibleLines = (this.dataAreaH - 25 - 8) / 10;
                    int maxOffset = Math.max(0, lines.size() - maxVisibleLines);
                    if (targetLine < this.advRawScrollOffset) {
                        this.advRawScrollOffset = Math.max(0, targetLine);
                    } else if (targetLine >= this.advRawScrollOffset + maxVisibleLines) {
                        this.advRawScrollOffset = Math.min(maxOffset, targetLine - maxVisibleLines + 1);
                    }

                    PokemonEditorDebug.logEditor("[Key-Arrow] {} curLine={} targetLine={} newCursor={} scroll={}", new Object[]{keyCode == 265 ? "UP" : "DOWN", curDisplayLine, targetLine, newCursor, this.advRawScrollOffset});
                }

                return true;
            }

            if (keyCode == 268 || keyCode == 269) {
                int boxW = this.dataAreaW;
                int contentMaxW = boxW - 8;
                MultilineLayout layout = this.layoutMultiline(current, contentMaxW);
                List<String> lines = layout.displayLines;
                List<Integer> offsets = layout.charOffsets;
                if (!lines.isEmpty()) {
                    int curDisplayLine = 0;

                    for(int li = 0; li < lines.size(); ++li) {
                        int lineStart = (Integer)offsets.get(li);
                        int lineEnd = li + 1 < lines.size() ? (Integer)offsets.get(li + 1) : current.length();
                        if (cursor >= lineStart && cursor <= lineEnd) {
                            curDisplayLine = li;
                            break;
                        }
                    }

                    int newCursor;
                    if (keyCode == 268) {
                        newCursor = (Integer)offsets.get(curDisplayLine);
                    } else if (curDisplayLine + 1 < lines.size()) {
                        newCursor = (Integer)offsets.get(curDisplayLine + 1);
                    } else {
                        newCursor = current.length();
                    }

                    ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(newCursor);
                    this.advRawField.setSelectionEnd(newCursor);
                    PokemonEditorDebug.logEditor("[Key-HomeEnd] {} curLine={} newCursor={}", new Object[]{keyCode == 268 ? "HOME" : "END", curDisplayLine, newCursor});
                    return true;
                }
            }

            if (keyCode == 65 && (modifiers & 2) != 0) {
                ((com.cobblemon_ts.mod.mixin.EditBoxAccessor) this.advRawField).setCursorPosition(current.length());
                this.advRawField.setSelectionEnd(0);
                PokemonEditorDebug.logEditor("[Key-CtrlA] selectAll textLen={}", new Object[]{current.length()});
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean shouldPause() {
        return false;
    }

    private void refreshHiddenPower() {
        if (this.selectedSlot >= 0) {
            int hp = this.parseOrDefault(this.ivHp, 0);
            int atk = this.parseOrDefault(this.ivAtk, 0);
            int def = this.parseOrDefault(this.ivDef, 0);
            int spa = this.parseOrDefault(this.ivSpA, 0);
            int spd = this.parseOrDefault(this.ivSpD, 0);
            int spe = this.parseOrDefault(this.ivSpe, 0);
            this.hiddenPowerType = this.calcHiddenPowerType(hp, atk, def, spa, spd, spe);
        }
    }

    private String calcHiddenPowerType(int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe) {
        int raw = (ivHp & 1) + 2 * (ivAtk & 1) + 4 * (ivDef & 1) + 8 * (ivSpe & 1) + 16 * (ivSpA & 1) + 32 * (ivSpD & 1);
        int typeIdx = raw * 15 / 63;
        if (typeIdx < 0) {
            typeIdx = 0;
        }

        if (typeIdx > 15) {
            typeIdx = 15;
        }

        return HIDDEN_POWER_TYPE_TABLE[typeIdx];
    }

    private String getHiddenPowerTypeZh() {
        if (this.hiddenPowerType != null && !this.hiddenPowerType.isEmpty()) {
            for(int i = 0; i < HIDDEN_POWER_TYPE_TABLE.length; ++i) {
                if (HIDDEN_POWER_TYPE_TABLE[i].equals(this.hiddenPowerType)) {
                    return Text.translatable(HIDDEN_POWER_TYPE_ZH[i]).getString();
                }
            }

            return this.hiddenPowerType;
        } else {
            return "";
        }
    }

    // [线性-L99] 多行文本布局记录类，用于高级数据区文本渲染和光标定位
    private static class MultilineLayout {
        final List<String> displayLines;
        final List<Integer> charOffsets;

        MultilineLayout(List<String> displayLines, List<Integer> charOffsets) {
            this.displayLines = displayLines;
            this.charOffsets = charOffsets;
        }
    }
}