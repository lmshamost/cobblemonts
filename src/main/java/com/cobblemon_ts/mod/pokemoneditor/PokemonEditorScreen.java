//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.cobblemon_ts.mod.pokemoneditor;

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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class PokemonEditorScreen extends Screen {
    private final UUID targetPlayerUUID;
    private final String targetPlayerName;
    private final int partySize;
    private final List<PokemonData> partyData = new ArrayList();
    private final List<PokemonData> originalSnapshots = new ArrayList();
    private int selectedSlot = -1;
    private int activeTab = 0;
    private int pendingDeleteSlot = -1;
    private EditBox sumSpeciesField;
    private EditBox sumLevelField;
    private EditBox sumNameField;
    private EditBox sumNatureField;
    private EditBox sumAbilityField;
    private EditBox sumFriendshipField;
    private EditBox sumFormField;
    private EditBox sumHeldItemField;
    private EditBox genderField;
    private Button sumShinyButton;
    private Button clearSpeciesBtn;
    private Button clearNameBtn;
    private Button clearNatureBtn;
    private Button clearAbilityBtn;
    private Button statusCycleBtn;
    private Button levelDownBtn;
    private Button levelUpBtn;
    private Button friendshipDownBtn;
    private Button friendshipUpBtn;
    private EditBox ivHp;
    private EditBox ivAtk;
    private EditBox ivDef;
    private EditBox ivSpA;
    private EditBox ivSpD;
    private EditBox ivSpe;
    private EditBox evHp;
    private EditBox evAtk;
    private EditBox evDef;
    private EditBox evSpA;
    private EditBox evSpD;
    private EditBox evSpe;
    private EditBox move1Field;
    private EditBox move2Field;
    private EditBox move3Field;
    private EditBox move4Field;
    private EditBox move1PPField;
    private EditBox move2PPField;
    private EditBox move3PPField;
    private EditBox move4PPField;
    private EditBox move1MaxPPField;
    private EditBox move2MaxPPField;
    private EditBox move3MaxPPField;
    private EditBox move4MaxPPField;
    private EditBox currentHpField;
    private EditBox maxHpField;
    private EditBox advExpField;
    private EditBox advUUIDField;
    private EditBox advRawField;
    private int dataAreaX = 0;
    private int dataAreaY = 0;
    private int dataAreaW = 340;
    private int dataAreaH = 200;
    private Button saveButton;
    private Button deleteButton;
    private Button closeButton;
    private Button tab0Btn;
    private Button tab1Btn;
    private Button tab2Btn;
    private Button tab3Btn;
    private final Button[] slotButtons = new Button[6];
    private List<String> autocompleteItems = new ArrayList();
    private int autocompleteStartY = 0;
    private int autocompleteX = 0;
    private int autocompleteW = 0;
    private int autocompleteActiveField = -1;
    private int autocompleteScrollOffset = 0;
    private String lastSearchQuery = "";
    private Button maxPPButton;
    private Button advImportBtn;
    private Button advExportBtn;
    private Button advCopyBtn;
    private Button advPasteBtn;
    private Button advSaveBtn;
    private Button advResetBtn;
    private boolean showImportDialog = false;
    private File[] pendingImportFiles = null;
    private int importDialogScroll = 0;
    private Button teamAllBtn = null;
    private boolean viewingAllTeam = false;
    private int advRawScrollOffset = 0;
    private Button genderCycleBtn;
    private Button themeToggleBtn;
    private Button editorAutoCloseBtn;
    private Button maxIvButton;
    private Button clearEvButton;
    private Button fullEvButton;
    private Button[] ivUpButtons = new Button[6];
    private Button[] ivDownButtons = new Button[6];
    private Button[] evUpButtons = new Button[6];
    private Button[] evDownButtons = new Button[6];
    private int crownIconX;
    private int crownIconY;
    private int crownToggleX;
    private int crownToggleY;
    private boolean crownVisible = true;
    private static final int CROWN_SIZE = 20;
    private static final int TOGGLE_SIZE = 20;
    private Button hiddenPowerBtn;
    private String hiddenPowerType = "";
    private static final String[][] STATUS_DISPLAY = new String[][]{{"", "  \u72b6\u6001: \u65e0  "}, {"poison", "  \u72b6\u6001: \u4e2d\u6bd2  "}, {"paralysis", "  \u72b6\u6001: \u9ebb\u75f9  "}, {"burn", "  \u72b6\u6001: \u70e7\u4f24  "}, {"freeze", "  \u72b6\u6001: \u51b0\u51bb  "}, {"sleep", "  \u72b6\u6001: \u7761\u7720  "}, {"bad_poison", "  \u72b6\u6001: \u5267\u6bd2  "}};
    private static final String[] HIDDEN_POWER_TYPE_TABLE = new String[]{"fighting", "flying", "poison", "ground", "rock", "bug", "ghost", "steel", "fire", "water", "grass", "electric", "psychic", "ice", "dragon", "dark"};
    private static final String[] HIDDEN_POWER_TYPE_ZH = new String[]{"\u683c\u6597", "\u98de\u884c", "\u6bd2", "\u5730\u9762", "\u5ca9\u77f3", "\u866b", "\u5e7d\u7075", "\u94a2", "\u706b", "\u6c34", "\u8349", "\u7535", "\u8d85\u80fd\u529b", "\u51b0", "\u9f99", "\u6076"};

    public PokemonEditorScreen(UUID targetPlayerUUID, String targetPlayerName, int partySize) {
        super(Component.literal("\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668"));
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
        this.clearWidgets();
        int screenW = this.width;
        int screenH = this.height;
        int panelX = screenW / 2 - 280;
        int panelY = 60;
        int panelW = 560;
        int panelH = 340;
        int tabBarY = 30;
        int tabBarH = 22;
        int tabW = 140;
        this.tab0Btn = Button.builder(Component.literal("  \u6982\u8981  "), (btn) -> this.switchTab(0)).pos(panelX, 30).size(140, 22).build();
        this.tab1Btn = Button.builder(Component.literal("  \u5bf9\u6218  "), (btn) -> this.switchTab(1)).pos(panelX + 140, 30).size(140, 22).build();
        this.tab2Btn = Button.builder(Component.literal("  \u80fd\u529b  "), (btn) -> this.switchTab(2)).pos(panelX + 280, 30).size(140, 22).build();
        this.tab3Btn = Button.builder(Component.literal("  \u9ad8\u7ea7  "), (btn) -> this.switchTab(3)).pos(panelX + 420, 30).size(140, 22).build();
        this.addRenderableWidget(this.tab0Btn);
        this.addRenderableWidget(this.tab1Btn);
        this.addRenderableWidget(this.tab2Btn);
        this.addRenderableWidget(this.tab3Btn);
        int partyX = panelX + 10;
        int partyY = 70;
        int partyW = 140;
        int slotH = 20;
        int slotGap = 2;

        for(int i = 0; i < Math.min(6, this.partySize); ++i) {
            int slotIdx = i;
            this.slotButtons[i] = Button.builder(Component.literal("  \u961f\u4f0d " + (i + 1) + "  (\u7a7a)  "), (btn) -> this.selectSlot(slotIdx)).pos(partyX, 70 + i * 22).size(140, 20).build();
            this.addRenderableWidget(this.slotButtons[i]);
        }

        int lastSlotY = 70 + Math.min(6, this.partySize) * 22;
        this.teamAllBtn = Button.builder(Component.literal("  \u5168\u961f  "), (btn) -> {
            this.viewingAllTeam = true;
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < this.partyData.size(); ++i) {
                if (i > 0) {
                    sb.append("\n\n");
                }

                sb.append("# ====== \u961f").append(i + 1).append(" ======\n");
                sb.append(this.buildDataString((PokemonData)this.partyData.get(i)));
            }

            this.advRawField.setValue(sb.toString());
            this.advRawField.setCursorPosition(0);
            this.advRawField.setHighlightPos(0);
            this.advRawScrollOffset = 0;
        }).pos(partyX, lastSlotY).size(140, 20).build();
        this.teamAllBtn.visible = false;
        this.addRenderableWidget(this.teamAllBtn);
        int editX = panelX + 165;
        int editY = 70;
        int editW = 385;
        int labelW = 50;
        int rowY = 88;
        int rowStep = 28;
        this.sumSpeciesField = new EditBox(this.font, editX + 50 + 10, rowY, 100, 16, Component.literal(""));
        this.sumSpeciesField.setMaxLength(40);
        this.sumSpeciesField.setTextShadow(false);
        this.clearSpeciesBtn = Button.builder(Component.literal("\u00d7"), (btn) -> this.sumSpeciesField.setValue("")).pos(editX + 50 + 10 + 100 + 4, rowY).size(18, 18).build();
        this.sumLevelField = this.makeIntBox(editX + 50 + 10, rowY + rowStep, 100, 1, 900);
        this.levelDownBtn = Button.builder(Component.literal("\u2b07"), (btn) -> {
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
            this.sumLevelField.setValue(String.valueOf(n));
        }).pos(editX + 50 + 10 + 100 + 4, rowY + rowStep).size(18, 18).build();
        this.levelUpBtn = Button.builder(Component.literal("\u2b06"), (btn) -> {
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
            this.sumLevelField.setValue(String.valueOf(n));
        }).pos(editX + 50 + 10 + 100 + 4 + 18 + 2, rowY + rowStep).size(18, 18).build();
        this.sumNameField = new EditBox(this.font, editX + 50 + 10, rowY + rowStep * 2, 100, 16, Component.literal(""));
        this.sumNameField.setMaxLength(30);
        this.sumNameField.setTextShadow(false);
        this.clearNameBtn = Button.builder(Component.literal("\u00d7"), (btn) -> this.sumNameField.setValue("")).pos(editX + 50 + 10 + 100 + 4, rowY + rowStep * 2).size(18, 18).build();
        this.sumNatureField = new EditBox(this.font, editX + 50 + 10, rowY + rowStep * 3, 100, 16, Component.literal(""));
        this.sumNatureField.setMaxLength(20);
        this.sumNatureField.setTextShadow(false);
        this.clearNatureBtn = Button.builder(Component.literal("\u00d7"), (btn) -> this.sumNatureField.setValue("")).pos(editX + 50 + 10 + 100 + 4, rowY + rowStep * 3).size(18, 18).build();
        this.sumAbilityField = new EditBox(this.font, editX + 50 + 10, rowY + rowStep * 4, 100, 16, Component.literal(""));
        this.sumAbilityField.setMaxLength(30);
        this.sumAbilityField.setTextShadow(false);
        this.clearAbilityBtn = Button.builder(Component.literal("\u00d7"), (btn) -> this.sumAbilityField.setValue("")).pos(editX + 50 + 10 + 100 + 4, rowY + rowStep * 4).size(18, 18).build();
        this.sumFriendshipField = this.makeIntBox(editX + 50 + 10, rowY + rowStep * 5, 100, 0, 255);
        this.friendshipDownBtn = Button.builder(Component.literal("\u2b07"), (btn) -> {
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
            this.sumFriendshipField.setValue(String.valueOf(n));
        }).pos(editX + 50 + 10 + 100 + 4, rowY + rowStep * 5).size(18, 18).build();
        this.friendshipUpBtn = Button.builder(Component.literal("\u2b06"), (btn) -> {
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
            this.sumFriendshipField.setValue(String.valueOf(n));
        }).pos(editX + 50 + 10 + 100 + 4 + 18 + 2, rowY + rowStep * 5).size(18, 18).build();
        this.sumFormField = new EditBox(this.font, editX + 50 + 10, rowY + rowStep * 6, 160, 16, Component.literal(""));
        this.sumFormField.setMaxLength(40);
        this.sumFormField.setTextShadow(false);
        this.sumHeldItemField = new EditBox(this.font, editX + 50 + 10, rowY + rowStep * 7, 160, 16, Component.literal(""));
        this.sumHeldItemField.setMaxLength(50);
        this.sumHeldItemField.setTextShadow(false);
        this.genderField = this.makeIntBox(editX + 50 + 10, rowY + rowStep * 8, 60, 0, 2);
        this.genderCycleBtn = Button.builder(Component.literal("  \u21bb \u5207\u6362  "), (btn) -> {
            int cur = this.parseOrDefault(this.genderField, 2);
            int next = (cur + 1) % 3;
            this.genderField.setValue(String.valueOf(next));
        }).pos(editX + 50 + 10 + 65, rowY + rowStep * 8).size(90, 18).build();
        this.sumShinyButton = Button.builder(Component.literal("  \u95ea\u5149: \u5173  "), (btn) -> {
            if (this.selectedSlot >= 0) {
                ((PokemonData)this.partyData.get(this.selectedSlot)).shiny = !((PokemonData)this.partyData.get(this.selectedSlot)).shiny;
                this.updateShinyButton();
            }

        }).pos(editX + 50 + 10, rowY + rowStep * 9).size(110, 18).build();
        this.statusCycleBtn = Button.builder(Component.literal("  \u72b6\u6001: \u65e0  "), (btn) -> {
            if (this.selectedSlot >= 0) {
                this.cycleToNextStatus();
            }

        }).pos(editX + 50 + 10 + 115, rowY + rowStep * 9).size(160, 18).build();
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
        this.ivHp.setResponder((s) -> this.refreshHiddenPower());
        this.ivAtk.setResponder((s) -> this.refreshHiddenPower());
        this.ivDef.setResponder((s) -> this.refreshHiddenPower());
        this.ivSpA.setResponder((s) -> this.refreshHiddenPower());
        this.ivSpD.setResponder((s) -> this.refreshHiddenPower());
        this.ivSpe.setResponder((s) -> this.refreshHiddenPower());
        EditBox[] ivBoxes = new EditBox[]{this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe};

        for(int i = 0; i < 6; ++i) {
            int ivIdx = i;
            int arrowBaseX = ivColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            this.ivDownButtons[i] = Button.builder(Component.literal("\u2b07"), (btn) -> {
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
                ivBoxes[ivIdx].setValue(String.valueOf(n));
            }).pos(arrowBaseX, arrowBaseY).size(arrowW, arrowH).build();
            this.ivUpButtons[i] = Button.builder(Component.literal("\u2b06"), (btn) -> {
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
                ivBoxes[ivIdx].setValue(String.valueOf(n));
            }).pos(arrowBaseX + arrowW + 2, arrowBaseY).size(arrowW, arrowH).build();
        }

        this.maxIvButton = Button.builder(Component.literal(" \u2605 \u6ee1IV 31"), (btn) -> {
            for(EditBox b : ivBoxes) {
                b.setValue("31");
            }

        }).pos(ivColX + 5, ivStartY + statRowH * 6 + 16).size(140, 22).build();
        int ivTitleW = this.font.width("IV (0-31)");
        this.crownIconX = ivColX + 60 + ivTitleW / 2 + 5;
        this.crownIconY = 81;
        this.crownToggleX = this.crownIconX + 20 + 4;
        this.crownToggleY = this.crownIconY;
        int hpCalcY = ivStartY + statRowH * 6 + 16 + 28;
        this.hiddenPowerBtn = Button.builder(Component.literal("\u89c9\u9192\u529b\u91cf"), (btn) -> {
            if (this.selectedSlot >= 0) {
                int hp = this.parseOrDefault(this.ivHp, 0);
                int atk = this.parseOrDefault(this.ivAtk, 0);
                int def = this.parseOrDefault(this.ivDef, 0);
                int spa = this.parseOrDefault(this.ivSpA, 0);
                int spd = this.parseOrDefault(this.ivSpD, 0);
                int spe = this.parseOrDefault(this.ivSpe, 0);
                this.hiddenPowerType = this.calcHiddenPowerType(hp, atk, def, spa, spd, spe);
            }
        }).pos(ivColX + 5, hpCalcY).size(80, 20).build();
        int evColX = editX + 260;
        this.evHp = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 0, valueW, 0, 252);
        this.evAtk = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 1, valueW, 0, 252);
        this.evDef = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 2, valueW, 0, 252);
        this.evSpA = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 3, valueW, 0, 252);
        this.evSpD = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 4, valueW, 0, 252);
        this.evSpe = this.makeIntBox(evColX + ivLabelW + 10, ivStartY + statRowH * 5, valueW, 0, 252);
        
        // [优化-L4] EV输入框添加总和限制响应器
        EditBox[] evBoxes = new EditBox[]{this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe};
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            evBoxes[i].setResponder(s -> {
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
                        evBoxes[idx].setValue(String.valueOf(newVal));
                        PokemonEditorDebug.logEditor("EV总和超出上限: " + maxCap + "，自动调整值为: " + newVal);
                    }
                }
            });
        }

        for(int i = 0; i < 6; ++i) {
            int evIdx = i;
            int arrowBaseX = evColX + ivLabelW + 10 + valueW + 4;
            int arrowBaseY = ivStartY + statRowH * i;
            this.evDownButtons[i] = Button.builder(Component.literal("\u2b07"), (btn) -> {
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
                evBoxes[evIdx].setValue(String.valueOf(n));
            }).pos(arrowBaseX, arrowBaseY).size(arrowW, arrowH).build();
            this.evUpButtons[i] = Button.builder(Component.literal("\u2b06"), (btn) -> {
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

                evBoxes[evIdx].setValue(String.valueOf(n));
            }).pos(arrowBaseX + arrowW + 2, arrowBaseY).size(arrowW, arrowH).build();
        }

        this.clearEvButton = Button.builder(Component.literal(" \u274c \u6e05\u96f6\u52aa\u529b"), (btn) -> {
            for(EditBox b : evBoxes) {
                b.setValue("0");
            }

        }).pos(evColX + 5, ivStartY + statRowH * 6 + 16).size(140, 22).build();
        boolean ev252Enabled = Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get());
        if (ev252Enabled) {
            this.fullEvButton = Button.builder(Component.literal(" \u2728 \u6ee1\u52aa\u529b 252"), (btn) -> {
                for(EditBox b : evBoxes) {
                    b.setValue("252");
                }

            }).pos(evColX + 5, ivStartY + statRowH * 6 + 16 + 28).size(140, 22).build();
        }

        int battleY = 85;
        this.currentHpField = this.makeIntBox(editX + 50 + 10, battleY, 100, 0, 9999);
        this.maxHpField = this.makeIntBox(editX + 50 + 10, battleY + 30, 100, 1, 9999);
        int moveY = battleY + 70;
        int moveLabelW = 50;
        int moveW = 180;
        int ppW = 60;
        int moveFieldX = editX + moveLabelW + 5;
        this.move1Field = new EditBox(this.font, moveFieldX, moveY, moveW, 16, Component.literal(""));
        this.move1Field.setMaxLength(30);
        this.move1Field.setTextShadow(false);
        this.move2Field = new EditBox(this.font, moveFieldX, moveY + 25, moveW, 16, Component.literal(""));
        this.move2Field.setMaxLength(30);
        this.move2Field.setTextShadow(false);
        this.move3Field = new EditBox(this.font, moveFieldX, moveY + 50, moveW, 16, Component.literal(""));
        this.move3Field.setMaxLength(30);
        this.move3Field.setTextShadow(false);
        this.move4Field = new EditBox(this.font, moveFieldX, moveY + 75, moveW, 16, Component.literal(""));
        this.move4Field.setMaxLength(30);
        this.move4Field.setTextShadow(false);
        int ppFieldX = moveFieldX + moveW + 10;
        this.move1PPField = this.makeIntBox(ppFieldX, moveY, ppW, 0, 999);
        this.move2PPField = this.makeIntBox(ppFieldX, moveY + 25, ppW, 0, 999);
        this.move3PPField = this.makeIntBox(ppFieldX, moveY + 50, ppW, 0, 999);
        this.move4PPField = this.makeIntBox(ppFieldX, moveY + 75, ppW, 0, 999);
        int maxPPFieldX = ppFieldX + ppW + 10;
        this.move1MaxPPField = this.makeIntBox(maxPPFieldX, moveY, ppW, 0, 999);
        this.move2MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 25, ppW, 0, 999);
        this.move3MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 50, ppW, 0, 999);
        this.move4MaxPPField = this.makeIntBox(maxPPFieldX, moveY + 75, ppW, 0, 999);
        this.move1MaxPPField.setEditable(true);
        this.move2MaxPPField.setEditable(true);
        this.move3MaxPPField.setEditable(true);
        this.move4MaxPPField.setEditable(true);
        this.maxPPButton = Button.builder(Component.literal(" \u6700\u5927"), (btn) -> {
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
                    this.move1MaxPPField.setValue(String.valueOf(maxPPs[0]));
                }

                if (maxPPs[1] > 0) {
                    this.move2MaxPPField.setValue(String.valueOf(maxPPs[1]));
                }

                if (maxPPs[2] > 0) {
                    this.move3MaxPPField.setValue(String.valueOf(maxPPs[2]));
                }

                if (maxPPs[3] > 0) {
                    this.move4MaxPPField.setValue(String.valueOf(maxPPs[3]));
                }

            }
        }).pos(maxPPFieldX, moveY + 100).size(ppW, 22).build();
        int advY = 85;
        this.advExpField = this.makeIntBox(editX + 50 + 10, advY, 120, 0, 2147483647);
        this.advUUIDField = new EditBox(this.font, editX + 50 + 10, advY + 35, 280, 16, Component.literal(""));
        this.advUUIDField.setMaxLength(60);
        this.advUUIDField.setEditable(true);
        this.advUUIDField.setTextShadow(false);
        this.dataAreaX = editX + 10;
        this.dataAreaY = advY + 80;
        this.dataAreaW = 340;
        this.dataAreaH = 200;
        this.advRawField = new EditBox(this.font, this.dataAreaX, this.dataAreaY, this.dataAreaW, 22, Component.literal(""));
        this.advRawField.setMaxLength(20000);
        this.advRawField.setEditable(true);
        this.advRawField.visible = true;
        this.advRawField.setTextShadow(false);
        this.advRawField.setTextColorUneditable(16777215);
        int rightBtnX = editX + 360;
        int rightBtnY = advY + 80;
        int rightBtnW = 80;
        int rightBtnH = 22;
        int rightBtnGap = 8;
        this.advCopyBtn = Button.builder(Component.literal(" \u590d\u5236 "), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                String data = this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot));
                this.minecraft.keyboardHandler.setClipboard(data);
            }

        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 0).size(rightBtnW, rightBtnH).build();
        this.advPasteBtn = Button.builder(Component.literal(" \u7c98\u8d34 "), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                String text = this.minecraft.keyboardHandler.getClipboard();
                this.applyDataString((PokemonData)this.partyData.get(this.selectedSlot), text);
                this.refreshEditFields();
                if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                    this.advRawField.setValue(this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot)));
                    this.advRawField.setCursorPosition(0);
                    this.advRawField.setHighlightPos(0);
                    this.advRawScrollOffset = 0;
                }
            }

        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 1).size(rightBtnW, rightBtnH).build();
        this.advExportBtn = Button.builder(Component.literal(" \u5bfc\u51fa "), (btn) -> {
            String content = this.advRawField.getValue();
            if (content != null && !content.trim().isEmpty()) {
                try {
                    File folder = new File(this.minecraft.gameDirectory, "pokemon_exports");
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                    String fileName = "pokemon_export_" + System.currentTimeMillis() + ".txt";
                    File file = new File(folder, fileName);
                    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
                    if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("\u5df2\u4fdd\u5b58: " + file.getAbsolutePath()));
                    }
                } catch (Exception e) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("\u4fdd\u5b58\u5931\u8d25: " + e.getMessage()));
                    }
                }

            }
        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 2).size(rightBtnW, rightBtnH).build();
        this.advImportBtn = Button.builder(Component.literal(" \u5bfc\u5165 "), (btn) -> {
            File folder = new File(this.minecraft.gameDirectory, "pokemon_exports");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File[] files = folder.listFiles((dir, name) -> name.startsWith("pokemon_export_") && name.endsWith(".txt"));
            if (files != null && files.length != 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                this.pendingImportFiles = files;
                this.showImportDialog = true;
            } else {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.literal("pokemon_exports \u6587\u4ef6\u5939\u91cc\u6ca1\u6709 pokemon_export_*.txt \u6587\u4ef6"));
                }

            }
        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 3).size(rightBtnW, rightBtnH).build();
        this.advSaveBtn = Button.builder(Component.literal(" \u4fdd\u5b58 "), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                this.applyDataString(d, this.advRawField.getValue());
                this.savePokemonData(d);
            }

        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 4).size(rightBtnW, rightBtnH).build();
        this.advResetBtn = Button.builder(Component.literal(" \u91cd\u7f6e "), (btn) -> {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                this.advRawField.setValue(this.buildDataString((PokemonData)this.partyData.get(this.selectedSlot)));
                this.advRawField.setCursorPosition(0);
                this.advRawField.setHighlightPos(0);
                this.advRawScrollOffset = 0;
            }

        }).pos(rightBtnX, rightBtnY + (rightBtnH + rightBtnGap) * 5).size(rightBtnW, rightBtnH).build();

        for(EditBox b : new EditBox[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
            this.addRenderableWidget(b);
        }

        this.addRenderableWidget(this.sumShinyButton);
        this.addRenderableWidget(this.clearSpeciesBtn);
        this.addRenderableWidget(this.clearNameBtn);
        this.addRenderableWidget(this.clearNatureBtn);
        this.addRenderableWidget(this.clearAbilityBtn);
        this.addRenderableWidget(this.statusCycleBtn);
        this.addRenderableWidget(this.genderCycleBtn);
        this.addRenderableWidget(this.levelDownBtn);
        this.addRenderableWidget(this.levelUpBtn);
        this.addRenderableWidget(this.friendshipDownBtn);
        this.addRenderableWidget(this.friendshipUpBtn);
        this.addRenderableWidget(this.maxIvButton);
        this.addRenderableWidget(this.clearEvButton);
        if (this.fullEvButton != null) {
            this.addRenderableWidget(this.fullEvButton);
        }

        this.addRenderableWidget(this.hiddenPowerBtn);

        for(int i = 0; i < 6; ++i) {
            if (this.ivUpButtons[i] != null) {
                this.addRenderableWidget(this.ivUpButtons[i]);
            }

            if (this.ivDownButtons[i] != null) {
                this.addRenderableWidget(this.ivDownButtons[i]);
            }

            if (this.evUpButtons[i] != null) {
                this.addRenderableWidget(this.evUpButtons[i]);
            }

            if (this.evDownButtons[i] != null) {
                this.addRenderableWidget(this.evDownButtons[i]);
            }
        }

        this.addRenderableWidget(this.advCopyBtn);
        this.addRenderableWidget(this.advPasteBtn);
        this.addRenderableWidget(this.advExportBtn);
        this.addRenderableWidget(this.advImportBtn);
        this.addRenderableWidget(this.advSaveBtn);
        this.addRenderableWidget(this.advResetBtn);
        this.addRenderableWidget(this.maxPPButton);
        int btnY = 415;
        int btnW = 120;
        int btnH = 22;
        int btnGap = 20;
        int totalBtnW = btnW * 4 + btnGap * 3;
        int btnStartX = panelX + 280 - totalBtnW / 2;
        boolean autoClose = (Boolean)PokemonEditorConfig.EDITOR_AUTO_CLOSE.get();
        this.editorAutoCloseBtn = Button.builder(Component.literal(autoClose ? "\u25a0" : "\u25a0").withStyle(autoClose ? ChatFormatting.GREEN : ChatFormatting.RED), (btn) -> {
            boolean newVal = !(Boolean)PokemonEditorConfig.EDITOR_AUTO_CLOSE.get();
            PokemonEditorConfig.EDITOR_AUTO_CLOSE.set(newVal);
            PokemonEditorConfig.EDITOR_AUTO_CLOSE.save();
            btn.setMessage(Component.literal("\u25a0").withStyle(newVal ? ChatFormatting.GREEN : ChatFormatting.RED));
        }).pos(btnStartX + btnW - 26, btnY - 26).size(22, 22).build();
        this.themeToggleBtn = Button.builder(Component.literal((Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? "  \u2600\ufe0f \u4eae\u8272  " : "  \ud83c\udf19 \u6df1\u8272  "), (btn) -> {
            boolean newVal = !(Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
            CobblemonTSConfig.LIGHT_EDITOR_THEME.set(newVal);
            CobblemonTSConfig.LIGHT_EDITOR_THEME.save();
            btn.setMessage(Component.literal(newVal ? "  \u2600\ufe0f \u4eae\u8272  " : "  \ud83c\udf19 \u6df1\u8272  "));
        }).pos(btnStartX, btnY).size(btnW, btnH).build();
        this.saveButton = Button.builder(Component.literal("  \u4fdd\u5b58  "), (btn) -> this.saveChanges()).pos(btnStartX + btnW + btnGap, btnY).size(btnW, btnH).build();
        this.deleteButton = Button.builder(Component.literal("  \u5220\u9664  "), (btn) -> this.deletePokemon()).pos(btnStartX + btnW * 2 + btnGap * 2, btnY).size(btnW, btnH).build();
        this.closeButton = Button.builder(Component.literal("  \u5173\u95ed  "), (btn) -> this.onClose()).pos(btnStartX + btnW * 3 + btnGap * 3, btnY).size(btnW, btnH).build();
        this.addRenderableWidget(this.editorAutoCloseBtn);
        this.addRenderableWidget(this.themeToggleBtn);
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.deleteButton);
        this.addRenderableWidget(this.closeButton);
        this.switchTab(this.activeTab);
        if (this.partySize > 0) {
            this.selectSlot(0);
        }

    }

    private EditBox makeIntBox(int x, int y, int w, int min, int max) {
        EditBox box = new EditBox(this.font, x, y, w, 16, Component.literal(""));
        box.setMaxLength(String.valueOf(max).length());
        box.setFilter((s) -> s.isEmpty() || s.matches("-?\\d+"));
        box.setTextShadow(false);
        return box;
    }

    private void drawInputFieldBackgrounds(GuiGraphics guiGraphics, boolean bright) {
        int boxBg = bright ? ColorEditorConfig.editorLightInputBg() : ColorEditorConfig.editorDarkInputBg();
        int boxFrame = bright ? ColorEditorConfig.editorLightInputBorder() : ColorEditorConfig.editorDarkInputBorder();
        int boxTopFocus = bright ? ColorEditorConfig.editorLightTabActive() : ColorEditorConfig.editorDarkTabActive();
        int textColor = bright ? ColorEditorConfig.editorLightInputText() : ColorEditorConfig.editorDarkInputText();

        for(EditBox b : new EditBox[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
            if (b != null && b.visible) {
                b.setTextColor(textColor);
                int bx = b.getX();
                int by = b.getY();
                int bw = b.getWidth();
                int bh = b.getHeight();
                guiGraphics.fill(bx, by, bx + bw, by + bh, boxBg);
                guiGraphics.fill(bx, by, bx + bw, by + 1, boxFrame);
                guiGraphics.fill(bx, by + bh - 1, bx + bw, by + bh, boxFrame);
                guiGraphics.fill(bx, by, bx + 1, by + bh, boxFrame);
                guiGraphics.fill(bx + bw - 1, by, bx + bw, by + bh, boxFrame);
                if (b.isFocused()) {
                    guiGraphics.fill(bx + 1, by + 1, bx + bw - 1, by + 2, boxTopFocus);
                }
            }
        }

    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        boolean sumVisible = tab == 0;

        for(EditBox b : new EditBox[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField}) {
            b.visible = sumVisible;
        }

        this.sumShinyButton.visible = sumVisible;
        this.clearSpeciesBtn.visible = sumVisible;
        this.statusCycleBtn.visible = sumVisible;
        this.genderCycleBtn.visible = sumVisible;
        this.clearNameBtn.visible = sumVisible;
        this.clearNatureBtn.visible = sumVisible;
        this.clearAbilityBtn.visible = sumVisible;
        this.levelDownBtn.visible = sumVisible;
        this.levelUpBtn.visible = sumVisible;
        this.friendshipDownBtn.visible = sumVisible;
        this.friendshipUpBtn.visible = sumVisible;
        boolean statsVisible = tab == 2;

        for(EditBox b : new EditBox[]{this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe}) {
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

        for(EditBox b : new EditBox[]{this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField}) {
            b.visible = battleVisible;
        }

        if (this.maxPPButton != null) {
            this.maxPPButton.visible = battleVisible;
        }

        boolean advVisible = tab == 3;

        for(EditBox b : new EditBox[]{this.advExpField, this.advUUIDField, this.advRawField}) {
            b.visible = advVisible;
        }

        for(Button b : new Button[]{this.advCopyBtn, this.advPasteBtn, this.advExportBtn, this.advImportBtn, this.advSaveBtn, this.advResetBtn}) {
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
                String display = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
                String shinyText = d.shiny ? " \u2728" : "";
                label = "  Lv" + d.level + " " + display + shinyText + "  ";
            } else {
                label = "  \u961f" + (slot + 1) + "(\u7a7a)  ";
            }

            if (slot == this.selectedSlot) {
                label = ">> " + label;
            } else {
                label = "   " + label;
            }

            this.slotButtons[slot].setMessage(Component.literal(label));
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
                this.sumSpeciesField.setValue("");
                this.sumLevelField.setValue("1");
                this.sumNameField.setValue("");
                List<String> natureList = PokemonNameMap.natureList();
                this.sumNatureField.setValue(natureList.isEmpty() ? "" : (String)natureList.get(0));
                this.sumAbilityField.setValue("");
                this.sumFriendshipField.setValue("0");
                this.sumFormField.setValue("");
                this.sumHeldItemField.setValue("");
                this.genderField.setValue("0");
                this.ivHp.setValue("0");
                this.ivAtk.setValue("0");
                this.ivDef.setValue("0");
                this.ivSpA.setValue("0");
                this.ivSpD.setValue("0");
                this.ivSpe.setValue("0");
                this.evHp.setValue("0");
                this.evAtk.setValue("0");
                this.evDef.setValue("0");
                this.evSpA.setValue("0");
                this.evSpD.setValue("0");
                this.evSpe.setValue("0");
                this.currentHpField.setValue("0");
                this.maxHpField.setValue("1");
                this.move1Field.setValue("");
                this.move2Field.setValue("");
                this.move3Field.setValue("");
                this.move4Field.setValue("");
                this.move1PPField.setValue("0");
                this.move2PPField.setValue("0");
                this.move3PPField.setValue("0");
                this.move4PPField.setValue("0");
                this.move1MaxPPField.setValue("0");
                this.move2MaxPPField.setValue("0");
                this.move3MaxPPField.setValue("0");
                this.move4MaxPPField.setValue("0");
                this.advExpField.setValue("0");
                this.advUUIDField.setValue("");
                d.shiny = false;
            } else {
                this.sumSpeciesField.setValue(PokemonNameMap.toChinese(d.speciesName));
                this.sumLevelField.setValue(String.valueOf(d.level));
                this.sumNameField.setValue(d.displayName);
                this.sumNatureField.setValue(PokemonNameMap.natureToChinese(d.nature));
                this.sumAbilityField.setValue(PokemonNameMap.abilityToChinese(d.ability));
                this.sumFriendshipField.setValue(String.valueOf(d.friendship));
                this.sumFormField.setValue(d.formName);
                this.sumHeldItemField.setValue(PokemonNameMap.itemToChinese(d.heldItem));
                this.genderField.setValue(String.valueOf(d.gender));
                this.ivHp.setValue(String.valueOf(d.ivHp));
                this.ivAtk.setValue(String.valueOf(d.ivAtk));
                this.ivDef.setValue(String.valueOf(d.ivDef));
                this.ivSpA.setValue(String.valueOf(d.ivSpA));
                this.ivSpD.setValue(String.valueOf(d.ivSpD));
                this.ivSpe.setValue(String.valueOf(d.ivSpe));
                this.evHp.setValue(String.valueOf(d.evHp));
                this.evAtk.setValue(String.valueOf(d.evAtk));
                this.evDef.setValue(String.valueOf(d.evDef));
                this.evSpA.setValue(String.valueOf(d.evSpA));
                this.evSpD.setValue(String.valueOf(d.evSpD));
                this.evSpe.setValue(String.valueOf(d.evSpe));
                this.currentHpField.setValue(String.valueOf(d.currentHp));
                this.maxHpField.setValue(String.valueOf(d.maxHp));
                String m1zh = PokemonNameMap.moveToChinese(d.move1);
                String m2zh = PokemonNameMap.moveToChinese(d.move2);
                String m3zh = PokemonNameMap.moveToChinese(d.move3);
                String m4zh = PokemonNameMap.moveToChinese(d.move4);
                PokemonEditorDebug.infoMoves("[Screen] moveToChinese results: slot0 en='{}'->zh='{}' slot1 en='{}'->zh='{}' slot2 en='{}'->zh='{}' slot3 en='{}'->zh='{}'", new Object[]{d.move1, m1zh, d.move2, m2zh, d.move3, m3zh, d.move4, m4zh});
                this.move1Field.setValue(m1zh);
                this.move2Field.setValue(m2zh);
                this.move3Field.setValue(m3zh);
                this.move4Field.setValue(m4zh);
                this.move1PPField.setValue(String.valueOf(d.move1PP));
                this.move2PPField.setValue(String.valueOf(d.move2PP));
                this.move3PPField.setValue(String.valueOf(d.move3PP));
                this.move4PPField.setValue(String.valueOf(d.move4PP));
                this.move1MaxPPField.setValue(String.valueOf(d.move1MaxPP));
                this.move2MaxPPField.setValue(String.valueOf(d.move2MaxPP));
                this.move3MaxPPField.setValue(String.valueOf(d.move3MaxPP));
                this.move4MaxPPField.setValue(String.valueOf(d.move4MaxPP));
                PokemonEditorDebug.infoMoves("[Screen] AFTER setValue: slot0 field='{}'({}/{}) slot1 field='{}'({}/{}) slot2 field='{}'({}/{}) slot3 field='{}'({}/{})", new Object[]{this.move1Field.getValue(), this.move1PPField.getValue(), this.move1MaxPPField.getValue(), this.move2Field.getValue(), this.move2PPField.getValue(), this.move2MaxPPField.getValue(), this.move3Field.getValue(), this.move3PPField.getValue(), this.move3MaxPPField.getValue(), this.move4Field.getValue(), this.move4PPField.getValue(), this.move4MaxPPField.getValue()});
                this.advExpField.setValue(String.valueOf(d.experience));
                this.advUUIDField.setValue(d.pokemonUUID == null ? "" : d.pokemonUUID.toString());
            }

            this.advRawField.setValue(this.buildDataString(d));
            this.advRawField.setCursorPosition(0);
            this.advRawField.setHighlightPos(0);
            this.advRawScrollOffset = 0;
            this.updateShinyButton();
            this.updateStatusButton();

            for(EditBox b : new EditBox[]{this.sumSpeciesField, this.sumLevelField, this.sumNameField, this.sumNatureField, this.sumAbilityField, this.sumFriendshipField, this.sumFormField, this.sumHeldItemField, this.genderField, this.ivHp, this.ivAtk, this.ivDef, this.ivSpA, this.ivSpD, this.ivSpe, this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe, this.currentHpField, this.maxHpField, this.move1Field, this.move2Field, this.move3Field, this.move4Field, this.move1PPField, this.move2PPField, this.move3PPField, this.move4PPField, this.move1MaxPPField, this.move2MaxPPField, this.move3MaxPPField, this.move4MaxPPField, this.advExpField, this.advUUIDField, this.advRawField}) {
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
            this.sumShinyButton.setMessage(Component.literal("  \u95ea\u5149: " + (d.shiny ? "\u5f00 \u2728" : "\u5173") + "  "));
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

    private void updateStatusButton() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            String cur = this.normalizeStatusId(d.status);
            String display = "  \u72b6\u6001: \u65e0  ";

            for(String[] row : STATUS_DISPLAY) {
                if (row[0].equals(cur)) {
                    display = row[1];
                    break;
                }
            }

            this.statusCycleBtn.setMessage(Component.literal(display));
        }
    }

    private int parseOrDefault(EditBox box, int def) {
        try {
            return Integer.parseInt(box.getValue());
        } catch (Exception var4) {
            return def;
        }
    }

    private void saveChanges() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            if (this.pendingDeleteSlot >= 0 && this.pendingDeleteSlot == this.selectedSlot) {
                PokemonEditorDebug.logEditor("[Screen] Save triggers pending delete for slot {}", new Object[]{this.selectedSlot});
                PacketDistributor.sendToServer(new PokemonEditorNetwork.DeletePokemonPacket(this.selectedSlot, this.targetPlayerUUID), new CustomPacketPayload[0]);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("[\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668] \u5df2\u5220\u9664\u961f\u4f0d " + (this.selectedSlot + 1)));
                }

                this.pendingDeleteSlot = -1;
                if ((Boolean)PokemonEditorConfig.EDITOR_AUTO_CLOSE.get()) {
                    this.onClose();
                }

            } else {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                String beforeSpecies = d.speciesName;
                d.speciesName = PokemonNameMap.toEnglish(this.sumSpeciesField.getValue());
                PokemonEditorDebug.logEditor("[Screen] species: UI='{}' \u2192 EN='{}' (was '{}')", new Object[]{this.sumSpeciesField.getValue(), d.speciesName, beforeSpecies});
                if (d.speciesName != null && !d.speciesName.isEmpty()) {
                    boolean var48 = false;
                } else {
                    boolean var10000 = true;
                }

                d.level = Math.max(1, Math.min(900, this.parseOrDefault(this.sumLevelField, d.level)));
                d.displayName = this.sumNameField.getValue();
                PokemonEditorDebug.logEditor("[Screen] displayName: UI='{}' \u2192 EN='{}'", new Object[]{this.sumNameField.getValue(), d.displayName});
                d.friendship = Math.max(0, Math.min(255, this.parseOrDefault(this.sumFriendshipField, d.friendship)));
                String beforeNature = d.nature;
                d.nature = PokemonNameMap.natureToEnglish(this.sumNatureField.getValue());
                PokemonEditorDebug.logEditor("[Screen] nature: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumNatureField.getValue(), d.nature, beforeNature, !Objects.equals(d.nature, beforeNature)});
                String beforeAbility = d.ability;
                d.ability = PokemonNameMap.abilityToEnglish(this.sumAbilityField.getValue());
                PokemonEditorDebug.logEditor("[Screen] ability: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumAbilityField.getValue(), d.ability, beforeAbility, !Objects.equals(d.ability, beforeAbility)});
                d.formName = this.sumFormField.getValue();
                String beforeHeldItem = d.heldItem;
                d.heldItem = PokemonNameMap.itemToEnglish(this.sumHeldItemField.getValue());
                PokemonEditorDebug.logEditor("[Screen] heldItem: UI='{}' \u2192 EN='{}' (was '{}', changed={})", new Object[]{this.sumHeldItemField.getValue(), d.heldItem, beforeHeldItem, !Objects.equals(d.heldItem, beforeHeldItem)});
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
                d.move1 = PokemonNameMap.moveToEnglish(this.move1Field.getValue());
                d.move2 = PokemonNameMap.moveToEnglish(this.move2Field.getValue());
                d.move3 = PokemonNameMap.moveToEnglish(this.move3Field.getValue());
                d.move4 = PokemonNameMap.moveToEnglish(this.move4Field.getValue());
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

                this.sendUpdatePacket(d, this.advUUIDField.getValue().trim());
            }
        }
    }

    private void savePokemonData(PokemonData d) {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            if (d != null) {
                boolean isDelete = d.speciesName == null || d.speciesName.isEmpty();
                if (isDelete) {
                    PokemonEditorDebug.logEditor("[Screen] savePokemonData: delete for slot {}", new Object[]{this.selectedSlot});
                    PacketDistributor.sendToServer(new PokemonEditorNetwork.DeletePokemonPacket(this.selectedSlot, this.targetPlayerUUID), new CustomPacketPayload[0]);
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("[\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668] \u5df2\u5220\u9664\u961f\u4f0d " + (this.selectedSlot + 1)));
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
        String rawDataText = this.advRawField != null ? this.advRawField.getValue() : "";
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

        PacketDistributor.sendToServer(new PokemonEditorNetwork.UpdatePokemonPacket(this.selectedSlot, this.targetPlayerUUID, d.speciesName, d.formName, d.level, d.experience, d.displayName, d.shiny, d.friendship, d.goldenBottleCap, d.ivHp, d.ivAtk, d.ivDef, d.ivSpA, d.ivSpD, d.ivSpe, d.evHp, d.evAtk, d.evDef, d.evSpA, d.evSpD, d.evSpe, d.nature, d.ability, d.move1, d.move2, d.move3, d.move4, d.move1PP, d.move2PP, d.move3PP, d.move4PP, d.move1MaxPP, d.move2MaxPP, d.move3MaxPP, d.move4MaxPP, d.currentHp, d.maxHp, d.heldItem, d.gender, uuidText, combinedRawData, d.status), new CustomPacketPayload[0]);
        this.originalSnapshots.set(this.selectedSlot, d.snapshot());
        this.updateSlotButtonLabel(this.selectedSlot);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("[\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668] \u5df2\u4fdd\u5b58\u961f\u4f0d " + (this.selectedSlot + 1)));
        }

        PokemonEditorDebug.logEditor("[Screen] Packet sent for slot {}", new Object[]{this.selectedSlot});
        if ((Boolean)PokemonEditorConfig.EDITOR_AUTO_CLOSE.get()) {
            this.onClose();
        } else {
            PokemonEditorDebug.logEditor("[Screen] auto-close disabled, keeping editor open", new Object[0]);
        }

    }

    private void deletePokemon() {
        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
            if (d.exists) {
                // [优化-L5] 添加删除二次确认弹窗
                Component title = Component.literal("\u786e\u8ba4\u5220\u9664\u5b9d\u53ef\u68a6");
                Component message = Component.literal("\u786e\u5b9e\u8981\u5220\u9664\u961f\u4f0d " + (this.selectedSlot + 1) + " \u7684\u5b9d\u53ef\u68a6\u5417\uff1f\n\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\uff01");

                // 使用原版确认弹窗
                ConfirmScreen confirmScreen = new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            this.pendingDeleteSlot = this.selectedSlot;
                            PokemonEditorDebug.logEditor("[Screen] Delete marker set for slot {} (pending, will delete on save)", new Object[]{this.selectedSlot});
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendSystemMessage(Component.literal("[\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668] \u5df2\u6807\u8bb0\u961f\u4f0d " + (this.selectedSlot + 1) + " \u5f85\u5220\u9664\uff08\u70b9\u51fb\u4fdd\u5b58\u751f\u6548\uff09"));
                            }

                            PokemonData emptyData = new PokemonData();
                            emptyData.exists = false;
                            this.partyData.set(this.selectedSlot, emptyData);
                            this.refreshEditFields();
                            this.updateSlotButtonLabel(this.selectedSlot);
                        }
                        // 不管是否确认，都回到编辑器界面
                        Minecraft.getInstance().setScreen(this);
                    }, title, message
                );

                Minecraft.getInstance().setScreen(confirmScreen);
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

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, -870704594);
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
        guiGraphics.fill(containerL - 3, containerTop - 3, containerR + 3, containerTop, 855638016);
        guiGraphics.fill(containerL - 3, containerBot, containerR + 3, containerBot + 3, 855638016);
        guiGraphics.fill(containerL - 3, containerTop, containerL, containerBot, 855638016);
        guiGraphics.fill(containerR, containerTop, containerR + 3, containerBot, 855638016);
        boolean editorLight = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get();
        int containerBg = editorLight ? ColorEditorConfig.editorLightContainerBg() : ColorEditorConfig.editorDarkContainerBg();
        int containerBorder = editorLight ? ColorEditorConfig.editorLightContainerBorder() : ColorEditorConfig.editorDarkContainerBorder();
        guiGraphics.fill(containerL, containerTop, containerR, containerBot, containerBg);
        guiGraphics.fill(containerL, containerTop, containerR, containerTop + 1, containerBorder);
        guiGraphics.fill(containerL, containerBot - 1, containerR, containerBot, containerBorder);
        guiGraphics.fill(containerL, containerTop, containerL + 1, containerBot, containerBorder);
        guiGraphics.fill(containerR - 1, containerTop, containerR, containerBot, containerBorder);
        int tabBarY = panelY - 30;
        int tabBarH = 22;
        int tabW = panelW / 4;
        int selectedTabX = panelX + this.activeTab * tabW;
        guiGraphics.fill(selectedTabX + 10, tabBarY + tabBarH + 1, selectedTabX + tabW - 10, tabBarY + tabBarH + 3, -12877066);
        int dividerX = panelX + 160;
        guiGraphics.fill(dividerX, partyY - 5, dividerX + 1, panelY + panelH - 5, -3418655);
        guiGraphics.drawString(this.font, "队伍", partyX + 55, partyY - 13, labelColor, false);
        String editorTitle = "\u5b9d\u53ef\u68a6\u7f16\u8f91\u5668 - " + this.targetPlayerName;
        int editorTitleW = this.font.width(editorTitle);
        int editorTitleX = panelX + panelW - editorTitleW - 10;
        int maxLeftW = editorTitleX - editX - 20;
        if (!this.viewingAllTeam) {
            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                if (d.exists) {
                    String title = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
                    String leftText = "\u6b63\u5728\u7f16\u8f91: " + title + (d.shiny ? " \u2728" : "");
                    if (this.font.width(leftText) > maxLeftW && maxLeftW > 20) {
                        String ellipsis = "...";

                        for(int availW = maxLeftW - this.font.width(ellipsis); this.font.width(leftText) > availW && leftText.length() > 4; leftText = leftText.substring(0, leftText.length() - 1)) {
                        }

                        leftText = leftText + ellipsis;
                    }

                    guiGraphics.drawString(this.font, leftText, editX, editY - 13, labelColor, false);
                    guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, -12877066, true);
                } else {
                    guiGraphics.drawString(this.font, "(\u7a7a\u69fd\u4f4d)", editX, editY - 13, -10193781, true);
                    guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, -12877066, true);
                }
            } else {
                guiGraphics.drawString(this.font, editorTitle, editorTitleX, editY - 13, -12877066, true);
            }
        }

        if (this.activeTab == 0) {
            int rowY = editY + 18;
            int rowStep = 28;
            String[] labels = new String[]{"\u7269\u79cd", "\u7b49\u7ea7", "\u6635\u79f0", "\u6027\u683c", "\u7279\u6027", "\u4eb2\u5bc6\u5ea6", "\u5f62\u6001", "\u643a\u5e26\u7269", "\u6027\u522b", "\u95ea\u5149"};

            for(int i = 0; i < labels.length; ++i) {
                int y = rowY + rowStep * i;
                guiGraphics.drawString(this.font, labels[i] + ":", editX, y + 4, labelColor, false);
            }

            if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                int gv = this.parseOrDefault(this.genderField, 2);
                String genderText = " (" + (gv == 0 ? "\u2642 \u96c4\u6027" : (gv == 1 ? "\u2640 \u96cc\u6027" : "\u26aa \u65e0\u6027\u522b")) + ")";
                // [线性-L10] 性别文字使用标签文字颜色，关闭阴影
                guiGraphics.drawString(this.font, genderText, editX + 50 + 10 + 65 + 100, rowY + rowStep * 8 + 4, labelColor, false);
            }

            int iconX = editX + 50 + 10 + 160 + 8;
            int iconY = rowY + rowStep * 7;

            try {
                String heldZh = this.sumHeldItemField.getValue();
                if (heldZh != null && !heldZh.trim().isEmpty()) {
                    String heldEn = PokemonNameMap.itemToEnglish(heldZh);
                    String rawId = heldEn;
                    if (heldEn.contains(":")) {
                        int idx = heldEn.lastIndexOf(":");
                        rawId = heldEn.substring(idx + 1);
                    }

                    rawId = rawId.trim().toLowerCase();
                    Item foundItem = null;

                    for(Map.Entry<ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
                        String id = ((ResourceKey)e.getKey()).location().getPath();
                        if (id.equalsIgnoreCase(rawId)) {
                            foundItem = (Item)e.getValue();
                            break;
                        }
                    }

                    if (foundItem != null && foundItem != Items.AIR) {
                        ItemStack stack = new ItemStack(foundItem);
                        guiGraphics.renderItem(stack, iconX, iconY + 1);
                        guiGraphics.renderItemDecorations(this.font, stack, iconX, iconY + 1);
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
                    int cardX = editX + 335;
                    int cardY = rowY - 5;
                    int cardW = 120;
                    int cardH = 130;
                    int frameColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -3418655 : -12102295;
                    int bgColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -197122 : -14800581;
                    int topBarColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -12877066 : -14326805;
                    int labelGrey = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -7035976 : -10193781;
                    int valueColor = (Boolean)CobblemonTSConfig.LIGHT_EDITOR_THEME.get() ? -1906448 : -14800581;
                    guiGraphics.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, frameColor);
                    guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);
                    guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + 22, topBarColor);
                    String zhName = d.displayName.isEmpty() ? PokemonNameMap.toChinese(d.speciesName) : d.displayName;
                    String cardTitle = zhName + (d.shiny ? " \u2728" : "");
                    guiGraphics.drawString(this.font, cardTitle, cardX + cardW / 2 - this.font.width(cardTitle) / 2, cardY + 7, -1, true);
                    int infoY = cardY + 30;
                    int infoX = cardX + 10;
                    String natureEffect = PokemonNameMap.natureEffectText(d.nature);
                    String var10000 = PokemonNameMap.natureToChinese(d.nature);
                    String natureDisp = var10000 + natureEffect;
                    if (natureDisp.length() > 14) {
                        natureDisp = natureDisp.substring(0, 14);
                    }

                    String[][] info = new String[][]{{"\u7269\u79cd", d.speciesName}, {"\u7b49\u7ea7", "Lv." + d.level}, {"\u6027\u522b", d.gender == 0 ? "\u2642 \u96c4" : (d.gender == 1 ? "\u2640 \u96cc" : "\u26aa \u65e0")}, {"\u6027\u683c", natureDisp}, {"\u7279\u6027", PokemonNameMap.abilityToChinese(d.ability)}, {"\u5f62\u6001", d.formName.isEmpty() ? "\u666e\u901a" : d.formName}};

                    for(int i = 0; i < info.length; ++i) {
                        int yy = infoY + i * 18;
                        guiGraphics.drawString(this.font, info[i][0] + ":", infoX, yy, labelGrey, true);
                        String val = info[i][1];
                        if (val != null && val.length() > 14) {
                            val = val.substring(0, 14);
                        }

                        guiGraphics.drawString(this.font, val == null ? "" : val, infoX + 40, yy, valueColor, true);
                    }
                }
            }

            int fieldLabelW = 50;
            int fieldX = editX + fieldLabelW + 10;
            int natureY = rowY + rowStep * 3;
            int abilityY = rowY + rowStep * 4;
            int dropdownY = -1;
            int activeX = fieldX;
            int activeW = 230;
            String q = "";
            int newActiveField = -1;
            if (this.sumSpeciesField.isFocused()) {
                dropdownY = rowY + 20;
                activeX = this.sumSpeciesField.getX();
                activeW = this.sumSpeciesField.getWidth();
                q = this.sumSpeciesField.getValue().trim().toLowerCase(Locale.ROOT);
                newActiveField = 0;
            } else if (this.sumNatureField.isFocused()) {
                dropdownY = natureY + 20;
                activeX = this.sumNatureField.getX();
                activeW = this.sumNatureField.getWidth();
                q = this.sumNatureField.getValue().trim();
                newActiveField = 1;
            } else if (this.sumAbilityField.isFocused()) {
                dropdownY = abilityY + 20;
                activeX = this.sumAbilityField.getX();
                activeW = this.sumAbilityField.getWidth();
                q = this.sumAbilityField.getValue().trim();
                newActiveField = 2;
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
                        List<String> species = PokemonNameMap.speciesList();
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
                                            String z = PokemonNameMap.toChinese((String)e.getValue());
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
                        List<String> natures = PokemonNameMap.natureList();
                        if (q.isEmpty()) {
                            this.autocompleteItems.addAll(natures);
                        } else {
                            for(String zh : natures) {
                                if (zh.contains(q)) {
                                    this.autocompleteItems.add(zh);
                                }
                            }
                        }
                    } else if (newActiveField == 2) {
                        List<String> abilities = PokemonNameMap.abilityList();
                        if (this.selectedSlot >= 0 && this.selectedSlot < this.partyData.size()) {
                            PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                            if (d.exists && d.possibleAbilities != null && !d.possibleAbilities.isEmpty()) {
                                List<String> zhAbilities = new ArrayList();

                                for(String enAbility : d.possibleAbilities) {
                                    String zh = PokemonNameMap.abilityToChinese(enAbility);
                                    if (zh != null && !zh.isEmpty() && !zhAbilities.contains(zh)) {
                                        zhAbilities.add(zh);
                                    }
                                }

                                if (!zhAbilities.isEmpty()) {
                                    abilities = zhAbilities;
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
                    }
                }

                this.autocompleteActiveField = newActiveField;
                this.lastSearchQuery = q;
            }
        } else if (this.activeTab == 2) {
            int ivStartY = editY + 40;
            int statRowH = 28;
            int ivColX = editX + 10;
            int evColX = editX + 260;
            String ivTitle = "IV (0-31)";
            guiGraphics.drawString(this.font, ivTitle, ivColX + 60 - this.font.width(ivTitle) / 2, editY + 15, -15681151, true);
            String evTitle = "EV (0-252)";
            guiGraphics.drawString(this.font, evTitle, evColX + 60 - this.font.width(evTitle) / 2, editY + 15, -12877066, true);
            String[] statLabels = new String[]{"HP", "\u653b\u51fb", "\u9632\u5fa1", "\u7279\u653b", "\u7279\u9632", "\u901f\u5ea6"};

            for(int i = 0; i < statLabels.length; ++i) {
                int y = ivStartY + statRowH * i;
                guiGraphics.drawString(this.font, statLabels[i] + ":", ivColX, y + 4, labelColor, false);
                guiGraphics.drawString(this.font, statLabels[i] + ":", evColX, y + 4, labelColor, false);
            }

            int evSum = 0;

            for(EditBox b : new EditBox[]{this.evHp, this.evAtk, this.evDef, this.evSpA, this.evSpD, this.evSpe}) {
                evSum += this.parseOrDefault(b, 0);
            }

            int sumY = ivStartY + statRowH * 6 - 4;
            int evCap = CobblemonTSConfig.getEVTotalCap();
            guiGraphics.drawString(this.font, "EV\u603b\u548c: " + evSum + "/" + evCap, evColX, sumY, evSum <= evCap ? -12877066 : -1096636, true);
            if (this.selectedSlot >= 0 && this.crownVisible) {
                PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                int bgColor = d.goldenBottleCap ? -11141291 : -43691;
                ResourceLocation crownIcon = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "textures/item/gold_crown.png");
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
                guiGraphics.blit(crownIcon, this.crownIconX, this.crownIconY, 0.0F, 0.0F, 20, 20, 20, 20);
                guiGraphics.pose().popPose();
                guiGraphics.fill(this.crownToggleX, this.crownToggleY, this.crownToggleX + 20, this.crownToggleY + 20, -16777216 | bgColor);
                String toggleText = d.goldenBottleCap ? "ON" : "OFF";
                int textW = this.font.width(toggleText);
                guiGraphics.drawString(this.font, toggleText, this.crownToggleX + (20 - textW) / 2, this.crownToggleY + 6, -1, false);
            }

            if (this.selectedSlot >= 0 && this.hiddenPowerType != null && !this.hiddenPowerType.isEmpty()) {
                int hpCalcY = ivStartY + statRowH * 6 + 16 + 28;
                String zhName = this.getHiddenPowerTypeZh();
                String display = zhName + " ";
                int textW = this.font.width(display);
                int textX = ivColX + 5 + 80 + 10;
                guiGraphics.drawString(this.font, display, textX, hpCalcY + 2, labelColor, false);

                try {
                    Object elementalType = Class.forName("com.cobblemon.mod.common.api.types.ElementalTypes").getMethod("get", String.class).invoke((Object)null, this.hiddenPowerType);
                    if (elementalType != null) {
                        int texX = (Integer)elementalType.getClass().getMethod("getTextureXMultiplier").invoke(elementalType);
                        ResourceLocation sheet = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/types_small.png");
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
                        guiGraphics.blit(sheet, textX + textW, hpCalcY + 1, (float)(texX * 18), 0.0F, 18, 18, 324, 18);
                        guiGraphics.pose().popPose();
                    }
                } catch (Exception var84) {
                }
            }
        } else if (this.activeTab == 1) {
            int battleY = editY + 15;
            guiGraphics.drawString(this.font, "当前HP:", editX, battleY + 4, labelColor, false);
            guiGraphics.drawString(this.font, "最大HP:", editX, battleY + 34, labelColor, false);
            int moveY = battleY + 70;
            int moveLabelW = 40;
            int moveW = 180;

            for(int i = 0; i < 4; ++i) {
                int y = moveY + 25 * i;
                guiGraphics.drawString(this.font, "\u62db\u5f0f" + (i + 1) + ":", editX, y + 4, labelColor, false);
            }

            guiGraphics.drawString(this.font, "PP", editX + moveLabelW + moveW + 20, moveY - 9, labelColor, false);
            guiGraphics.drawString(this.font, "\u6700\u5927PP", editX + moveLabelW + moveW + 10 + 60 + 10 + 20, moveY - 9, -12877066, true);
            int moveFieldX = editX + moveLabelW + 10;
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
                q = this.move1Field.getValue().trim();
                newActiveField = 3;
            } else if (this.move2Field.isFocused()) {
                dropdownY = move2Y + 20;
                activeX = this.move2Field.getX();
                activeW = this.move2Field.getWidth();
                q = this.move2Field.getValue().trim();
                newActiveField = 4;
            } else if (this.move3Field.isFocused()) {
                dropdownY = move3Y + 20;
                activeX = this.move3Field.getX();
                activeW = this.move3Field.getWidth();
                q = this.move3Field.getValue().trim();
                newActiveField = 5;
            } else if (this.move4Field.isFocused()) {
                dropdownY = move4Y + 20;
                activeX = this.move4Field.getX();
                activeW = this.move4Field.getWidth();
                q = this.move4Field.getValue().trim();
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
                    List<String> allMoves = PokemonNameMap.moveList();
                    if (q.isEmpty()) {
                        for(String zh : allMoves) {
                            this.autocompleteItems.add(zh);
                        }
                    } else {
                        String lowerQ = q.toLowerCase(Locale.ROOT);

                        for(String zhName : allMoves) {
                            String enName = PokemonNameMap.moveToEnglish(zhName);
                            if ((zhName.contains(q) || enName != null && enName.contains(lowerQ)) && !this.autocompleteItems.contains(zhName)) {
                                this.autocompleteItems.add(zhName);
                            }

                            if (!this.autocompleteItems.contains(zhName) && CobblemonBridge.pinyinContains(zhName, q)) {
                                this.autocompleteItems.add(zhName);
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
                guiGraphics.drawString(this.font, "\u7ecf\u9a8c\u503c:", editX, advY + 4, labelColor, false);
                guiGraphics.drawString(this.font, "UUID:", editX, advY + 39, labelColor, false);
                guiGraphics.drawString(this.font, "\u6570\u636e\u533a\uff08\u53ef\u7f16\u8f91/\u590d\u5236/\u7c98\u8d34\uff09:", editX + 10, advY + 64, labelColor, false);
            } else {
                guiGraphics.drawString(this.font, "\u5168\u961f\u6570\u636e\uff08\u53ef\u7f16\u8f91/\u590d\u5236/\u7c98\u8d34\uff09:", editX + 10, editY + 4, labelColor, false);
            }
        }

        this.drawInputFieldBackgrounds(guiGraphics, editorLight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, boxBg);
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, boxFrame);
            guiGraphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, boxFrame);
            guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxH, boxFrame);
            guiGraphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, boxFrame);
            if (this.advRawField.isFocused()) {
                guiGraphics.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + 2, boxTopFocus);
            }

            String text = this.advRawField.getValue();
            if (text == null || text.isEmpty()) {
                if (this.advRawField.isFocused() && System.currentTimeMillis() / 500L % 2L == 0L) {
                    guiGraphics.fill(boxX + padX, boxY + padY - 1, boxX + padX + 1, boxY + padY + lineH - 2, textColor);
                }

                return;
            }

            int maxVisibleLines = (boxH - padY * 2) / lineH;
            int contentMaxW = boxW - padX * 2;
            MultilineLayout layout = this.layoutMultiline(text, contentMaxW);
            List<String> lines = layout.displayLines;
            List<Integer> charOffsets = layout.charOffsets;
            int cursorPos = this.advRawField.getCursorPosition();
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
                        guiGraphics.drawString(this.font, pre, drawX, drawY, textColor, false);
                        drawX += this.font.width(pre);
                        col = selStartDisplayCol;
                    }

                    int selEndCol = isEndLine ? selEndDisplayCol : line.length();
                    if (selEndCol > col) {
                        String selText = line.substring(Math.min(col, line.length()), Math.min(selEndCol, line.length()));
                        int bgW = this.font.width(selText);
                        guiGraphics.fill(drawX, drawY - 1, drawX + bgW, drawY + lineH - 2, selectionBg);
                        guiGraphics.drawString(this.font, selText, drawX, drawY, selectionFg, false);
                        drawX += bgW;
                        col = selEndCol;
                    }

                    if (isEndLine && col < line.length()) {
                        String post = line.substring(col);
                        guiGraphics.drawString(this.font, post, drawX, drawY, textColor, false);
                    }
                } else {
                    guiGraphics.drawString(this.font, line, boxX + padX, drawY, textColor, false);
                }

                if (this.advRawField.isFocused() && displayLineIdx == cursorDisplayLine) {
                    String upToCursor = line.substring(0, Math.min(cursorDisplayCol, line.length()));
                    int cursorX = boxX + padX + this.font.width(upToCursor);
                    int cursorTopY = drawY - 1;
                    int cursorBotY = drawY + lineH - 2;
                    if (System.currentTimeMillis() / 500L % 2L == 0L) {
                        guiGraphics.fill(cursorX, cursorTopY, cursorX + 1, cursorBotY, cursorColor);
                    }
                }
            }
        }

        if ((this.activeTab == 0 || this.activeTab == 1) && !this.autocompleteItems.isEmpty() && this.autocompleteStartY > 0) {
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
            guiGraphics.fill(sfX - 2, sY - 2, sfX + this.autocompleteW + 2, sY + listH + 2, -1728053248);
            guiGraphics.fill(sfX - 1, sY - 1, sfX + this.autocompleteW + 1, sY + listH + 1, -4667430);
            guiGraphics.fill(sfX, sY, sfX + this.autocompleteW, sY + listH, -197122);

            for(int i = 0; i < shown; ++i) {
                int itemIdx = this.autocompleteScrollOffset + i;
                if (itemIdx < 0 || itemIdx >= this.autocompleteItems.size()) {
                    break;
                }

                int itemY = sY + i * lineH;
                if (mouseX >= sfX && mouseX < sfX + this.autocompleteW && mouseY >= itemY && mouseY < itemY + lineH) {
                    guiGraphics.fill(sfX, itemY, sfX + this.autocompleteW, itemY + lineH, -12877066);
                    guiGraphics.drawString(this.font, (String)this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -1, false);
                } else {
                    guiGraphics.drawString(this.font, (String)this.autocompleteItems.get(itemIdx), sfX + 6, itemY + 4, -14800581, false);
                }
            }

            if (this.autocompleteItems.size() > maxShown) {
                int arrowX = sfX + this.autocompleteW - 12;
                if (this.autocompleteScrollOffset > 0) {
                    guiGraphics.drawString(this.font, "\u2191", arrowX, sY - 12, -12877066, false);
                }

                if (this.autocompleteScrollOffset < maxOffset) {
                    guiGraphics.drawString(this.font, "\u2193", arrowX, sY + listH + 2, -12877066, false);
                }
            }
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
            guiGraphics.fill(0, 0, this.width, this.height, -2013265920);
            guiGraphics.fill(dlgX - 2, dlgY - 2, dlgX + dlgW + 2, dlgY + dlgH + 2, -16777216);
            guiGraphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, dlgBg);
            guiGraphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 28, dlgTitleBg);
            guiGraphics.drawString(this.font, "\u9009\u62e9\u8981\u5bfc\u5165\u7684\u6587\u4ef6\uff08\u6700\u65b0\u7684\u5728\u524d\uff09", dlgX + 12, dlgY + 10, -1, true);
            int closeX = dlgX + dlgW - 28;
            int closeY = dlgY + 6;
            guiGraphics.fill(closeX, closeY, closeX + 22, closeY + 16, -4645860);
            guiGraphics.drawString(this.font, "X", closeX + 8, closeY + 4, -1, true);
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
                guiGraphics.fill(listX, itemY, listX + listW, itemY + itemH - 2, hover ? dlgHoverBg : dlgItemBg);
                String fName = f.getName();
                guiGraphics.drawString(this.font, fName, listX + 8, itemY + 6, dlgText, true);
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
                guiGraphics.drawString(this.font, sizeStr + "   |   " + dateStr + "   |   " + f.getParent(), listX + 8, itemY + 22, dlgSubText, true);
            }

            if (this.pendingImportFiles.length > maxShown) {
                int aX = listX + listW - 14;
                if (this.importDialogScroll > 0) {
                    guiGraphics.drawString(this.font, "\u2191", aX, listY - 14, dlgText, true);
                }

                if (this.importDialogScroll < maxScroll) {
                    guiGraphics.drawString(this.font, "\u2193", aX, listY + maxShown * itemH - 12, dlgText, true);
                }
            }
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
            String text = this.advRawField.getValue();
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
                    int cw = this.font.width(String.valueOf(line.charAt(i)));
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
                    String text = this.advRawField.getValue();
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
                        this.advRawField.setCursorPosition(charPos);
                        this.advRawField.setHighlightPos(charPos);
                    }

                    return true;
                }
            }

            PokemonEditorDebug.log("[Scroll-" + source + "] not handled, passing to super");
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean handled = this.handleMouseScroll(mouseX, mouseY, scrollX, scrollY, scrollY, "4param");
        return handled ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PokemonEditorDebug.logEditor("[Mouse-Click] entry activeTab={} mx={} my={} button={} advRawField={} dataArea=[{},{},{},{}]", new Object[]{this.activeTab, (int)mouseX, (int)mouseY, button, this.advRawField != null ? "non-null" : "null", this.dataAreaX, this.dataAreaY, this.dataAreaW, this.dataAreaH});
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
            int mx = (int)mouseX;
            int my = (int)mouseY;
            int closeX = panelX + panelW - 28;
            int closeY = panelY + 6;
            if (mx >= closeX && mx < closeX + 22 && my >= closeY && my < closeY + 16) {
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
                            this.advRawField.setValue(text);
                            this.advRawField.setCursorPosition(0);
                            this.advRawField.setHighlightPos(0);
                            this.advRawScrollOffset = 0;
                            if (this.minecraft.player != null) {
                                File var10001 = this.pendingImportFiles[fileIdx];
                                this.minecraft.player.sendSystemMessage(Component.literal("\u5df2\u8f7d\u5165: " + var10001.getName()));
                            }
                        } catch (Exception e) {
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendSystemMessage(Component.literal("\u8bfb\u53d6\u5931\u8d25: " + e.getMessage()));
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
                int mx = (int)mouseX;
                int my = (int)mouseY;
                PokemonEditorDebug.logEditor("[Mouse-Click-Check] activeTab={} button={} mx={} my={} box=[{},{},{},{}] inBox={}", new Object[]{this.activeTab, button, mx, my, boxX, boxY, boxW, boxH, mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH});
                if (mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH) {
                    this.setFocused(this.advRawField);
                    this.advRawField.setFocused(true);
                    int pos = this.advRawFieldPosFromMouse(mouseX, mouseY);
                    this.advRawField.setCursorPosition(pos);
                    this.advRawField.setHighlightPos(pos);
                    PokemonEditorDebug.logEditor("[Mouse-Click-Apply] focused=true cursor={}/{}", new Object[]{pos, this.advRawField.getValue().length()});
                    return true;
                }
            }

            if ((this.activeTab == 0 || this.activeTab == 1) && !this.autocompleteItems.isEmpty() && button == 0) {
                int lineH = 16;
                int maxShown = 8;
                int mx = (int)mouseX;
                int my = (int)mouseY;
                int listH = Math.min(this.autocompleteItems.size(), maxShown) * lineH;
                if (mx >= this.autocompleteX && mx < this.autocompleteX + this.autocompleteW && my >= this.autocompleteStartY && my < this.autocompleteStartY + listH) {
                    int idx = this.autocompleteScrollOffset + (my - this.autocompleteStartY) / lineH;
                    if (idx >= 0 && idx < this.autocompleteItems.size()) {
                        String selected = (String)this.autocompleteItems.get(idx);
                        if (this.autocompleteActiveField == 0) {
                            this.sumSpeciesField.setValue(selected);
                        } else if (this.autocompleteActiveField == 1) {
                            this.sumNatureField.setValue(selected);
                        } else if (this.autocompleteActiveField == 2) {
                            this.sumAbilityField.setValue(selected);
                        } else if (this.autocompleteActiveField == 3) {
                            this.move1Field.setValue(selected);
                        } else if (this.autocompleteActiveField == 4) {
                            this.move2Field.setValue(selected);
                        } else if (this.autocompleteActiveField == 5) {
                            this.move3Field.setValue(selected);
                        } else if (this.autocompleteActiveField == 6) {
                            this.move4Field.setValue(selected);
                        }

                        this.autocompleteItems.clear();
                        this.autocompleteScrollOffset = 0;
                        return true;
                    }
                }
            }

            if (this.selectedSlot >= 0 && this.crownVisible) {
                int mx = (int)mouseX;
                int my = (int)mouseY;
                if (mx >= this.crownToggleX && mx < this.crownToggleX + 20 && my >= this.crownToggleY && my < this.crownToggleY + 20) {
                    PokemonData d = (PokemonData)this.partyData.get(this.selectedSlot);
                    d.goldenBottleCap = !d.goldenBottleCap;
                    d.markModified("goldenBottleCap");
                    return true;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.activeTab == 3 && this.advRawField != null && this.advRawField.isFocused() && button == 0) {
            int pos = this.advRawFieldPosFromMouse(mouseX, mouseY);
            this.advRawField.setCursorPosition(pos);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    public boolean charTyped(char ch, int modifiers) {
        if (this.advRawField != null && this.advRawField.isFocused() && this.activeTab == 3) {
            boolean handled = this.advRawField.charTyped(ch, modifiers);
            if (handled) {
                PokemonEditorDebug.logEditor("[Char-Typed] ch='{}' handled=true textLen={}", new Object[]{ch, this.advRawField.getValue().length()});
            }

            return handled;
        } else {
            return super.charTyped(ch, modifiers);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PokemonEditorDebug.logEditor("[Key-Pressed] keyCode={} activeTab={} advRawFieldFocused={} advRawFieldValueLen={}", new Object[]{keyCode, this.activeTab, this.advRawField != null ? this.advRawField.isFocused() : "null", this.advRawField != null ? this.advRawField.getValue().length() : 0});
        if (this.advRawField != null && this.advRawField.isFocused() && this.activeTab == 3) {
            String current = this.advRawField.getValue();
            int cursor = this.advRawField.getCursorPosition();
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
                this.advRawField.setValue(before + "\n" + after);
                this.advRawField.setCursorPosition(cursor + 1);
                this.advRawField.setHighlightPos(cursor + 1);
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
                    this.advRawField.setCursorPosition(newCursor);
                    this.advRawField.setHighlightPos(newCursor);
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

                    this.advRawField.setCursorPosition(newCursor);
                    this.advRawField.setHighlightPos(newCursor);
                    PokemonEditorDebug.logEditor("[Key-HomeEnd] {} curLine={} newCursor={}", new Object[]{keyCode == 268 ? "HOME" : "END", curDisplayLine, newCursor});
                    return true;
                }
            }

            if (keyCode == 65 && (modifiers & 2) != 0) {
                this.advRawField.setCursorPosition(current.length());
                this.advRawField.setHighlightPos(0);
                PokemonEditorDebug.logEditor("[Key-CtrlA] selectAll textLen={}", new Object[]{current.length()});
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean isPauseScreen() {
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
                    return HIDDEN_POWER_TYPE_ZH[i];
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