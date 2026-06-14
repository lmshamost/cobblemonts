package com.cobblemon_ts.mod.ui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.DebugConfig;
import com.cobblemon_ts.mod.network.PartyToPCPacket;
import com.cobblemon_ts.mod.network.ReleasePokemonPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

public class PartyUIState {

    // ===== 统一常量（唯一来源，两个 Mixin 全部引用此处） =====
    public static final int PARTY_WIDTH = 60;
    public static final int SLOT_SIZE = 28;
    public static final int SLOT_SPACING = 3;
    public static final int HEADER_HEIGHT = 16;
    public static final int SLOT_CONTENT_WIDTH = PARTY_WIDTH - 6;     // 54
    public static final int SLOT_MODEL_WIDTH = SLOT_CONTENT_WIDTH - 20; // 34
    // 护符栏：与精灵栏**像素级对齐**（精灵槽位间有3px间距）
    // 宽度 = PARTY_WIDTH = 60
    // 标题高度 = HEADER_HEIGHT = 16（对齐精灵栏"队伍"）
    // 第一个"精灵槽位"位置留空（= SLOT_SIZE + SLOT_SPACING = 31）
    // 然后每个护符单元 = SLOT_SIZE + SLOT_SPACING = 31（与精灵槽位+间距同高，完美对齐）
    // 5个护符 → 从精灵2的Y位置开始依次对应精灵2,3,4,5,6
    public static final int CHARM_DRAWER_MAX_OFFSET = 60;              // 抽屉宽度 = 精灵栏宽度
    public static final int CHARM_ANIM_STEP = 3;
    public static final int ARROW_HITBOX_W = 12;
    public static final int ARROW_HITBOX_H = 12;
    public static final int CHARM_BTN_W = 16;
    public static final int CHARM_BTN_H = 16;
    public static final int ITEM_SLOT_SIZE = 16;
    public static final int CONTEXT_MENU_WIDTH = 70;
    public static final int CONTEXT_MENU_ITEM_HEIGHT = 14;
    public static final int CHARM_DRAWER_MIN_VISIBLE = 40;             // 抽屉展开后才显示内容
    // 护符单元 = 精灵槽位高度 + 间距 = 31px（完美对齐）
    public static final int CHARM_CELL_HEIGHT = SLOT_SIZE + SLOT_SPACING;  // = 31
    // 护符栏内容起始偏移：标题(16) + 空1槽位(31) = 47
    public static int getCharmDrawerContentOffset() {
        return HEADER_HEIGHT + 1 * (SLOT_SIZE + SLOT_SPACING);
    }
    // 护符栏标题高度 = 精灵栏标题高度 = 16
    public static final int CHARM_HEADER_HEIGHT = HEADER_HEIGHT;

    public static final CharmType[] CHARM_TYPES = CharmType.values();

    // ===== 面板主题：亮色白底 / 暗色深灰 =====
    public static final int VANILLA_WHITE_BG = 0xFF_F0F0F0;
    public static final int PANEL_BORDER_GRAY = 0xFF_AAAAAA;
    // 暗色面板颜色（与原有 JAR 一致）
    public static final int DARK_OUTER = 0xCC_0B0B0B;  // -856295947
    public static final int DARK_INNER = 0xCC_161616;  // -855638017

    public static boolean isLightTheme() {
        return CobblemonTSConfig.LIGHT_PANEL_THEME.get();
    }

    public static void drawPanelBg(GuiGraphics graphics, int x, int y, int w, int h) {
        if (isLightTheme()) {
            graphics.fill(x, y, x + w, y + h, VANILLA_WHITE_BG);
            graphics.hLine(x, x + w - 1, y, PANEL_BORDER_GRAY);
            graphics.hLine(x, x + w - 1, y + h - 1, PANEL_BORDER_GRAY);
            graphics.vLine(x, y, y + h - 1, PANEL_BORDER_GRAY);
            graphics.vLine(x + w - 1, y, y + h - 1, PANEL_BORDER_GRAY);
        } else {
            graphics.fill(x, y, x + w, y + h, DARK_OUTER);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, DARK_INNER);
        }
    }

    public static int getPanelTextColor() {
        return isLightTheme() ? 0xFF_333333 : 0xFF_FFFFFF;
    }

    /** 面板总高度 = HEADER_HEIGHT + 6 * (SLOT_SIZE + SLOT_SPACING) + SLOT_SPACING = 205 */
    public static int getPanelHeight() {
        return HEADER_HEIGHT + 6 * (SLOT_SIZE + SLOT_SPACING) + SLOT_SPACING;
    }

    // ===== debug 开关（从独立的 debug 配置文件读取） =====
    public static boolean debugParty() { return Boolean.TRUE.equals(DebugConfig.DEBUG_PARTY.get()); }
    public static boolean debugClick() { return Boolean.TRUE.equals(DebugConfig.DEBUG_CLICK.get()); }
    public static boolean debugCharm() { return Boolean.TRUE.equals(DebugConfig.DEBUG_CHARM.get()); }
    public static boolean debugSlot()  { return Boolean.TRUE.equals(DebugConfig.DEBUG_SLOT.get()); }
    public static boolean debugConfig(){ return Boolean.TRUE.equals(DebugConfig.DEBUG_CONFIG.get()); }

    // 兼容方法名（避免 mixin 调用不存在的 initPartyReflection）
    public static boolean initPartyReflection() { return initBasicReflection(); }
    public static boolean isPartyReflectionReady() { return basicReflectionReady; }

    // ===== 反射：与 coblemon_ts-1.0.1.jar 一致 =====
    private static Method speciesGetTranslatedNameMethod; // Species.getTranslatedName() → Component
    private static Method componentGetStringMethod;       // Component.getString() → String
    private static Method speciesGetNameMethod;           // 兜底: Species.getName() → String
    private static Method speciesResourceIdMethod;        // Species.getResourceIdentifier() → String
    private static Method pokemonSwapHeldItemMethod;      // Pokemon.swapHeldItem(...)
    private static Method pokemonRemoveHeldItemMethod;    // Pokemon.removeHeldItem()
    private static Method summaryOpenMethod;              // Summary.open(Collection, boolean, int)
    private static Object summaryCompanionInstance;
    private static Class<?> summaryClass;                 // Summary.class，用于直接构造实例
    private static boolean basicReflectionReady;

    // [优化-P01] 缓存 getPartyList 中的高频反射方法
    private static java.lang.reflect.Method cachedClientGetSlotsMethod;  // ClientParty.getSlots()
    private static java.lang.reflect.Method cachedClientGetIntMethod;    // ClientParty.get(int)

    public static final Set<String> HEALING_ITEMS = new HashSet<>();
    public static final Set<String> STATUS_CURE_ITEMS = new HashSet<>();
    public static final Set<String> REVIVE_ITEMS = new HashSet<>();
    public static boolean itemSetsReady;

    // ===== 状态管理 =====
    private static final IdentityHashMap<Object, State> UI_STATES = new IdentityHashMap<>();

    // 持久化状态：不随屏幕实例变化丢失（隐藏精灵栏、护符抽屉开关）
    private static boolean persistentPartyHidden = false;
    private static boolean persistentCharmDrawerOut = false;

    public static State getState(Object screen) {
        return UI_STATES.computeIfAbsent(screen, k -> {
            State s = new State();
            // 新 State 实例：从持久化值初始化
            s.partyHidden = persistentPartyHidden;
            s.charmDrawerOut = persistentCharmDrawerOut;
            return s;
        });
    }

    /** 更新持久化隐藏状态（在开关切换时调用） */
    public static void setPersistentPartyHidden(boolean value) {
        persistentPartyHidden = value;
        // 同步更新所有已存在的 State 实例
        for (State s : UI_STATES.values()) s.partyHidden = value;
    }

    /** 更新持久化护符抽屉状态 */
    public static void setPersistentCharmDrawerOut(boolean value) {
        persistentCharmDrawerOut = value;
        // 同步更新所有已存在的 State 实例
        for (State s : UI_STATES.values()) s.charmDrawerOut = value;
    }

    public static class State {
        public boolean charmDrawerOut;
        public int charmDrawerOffset;
        public boolean partyHidden;
        public CharmSlotManager charmManager;
        public int selectedPartySlot = -1;
        public int dragSourceSlot = -1;
        public boolean contextMenuOpen;
        public int contextMenuSlot = -1;
        public int contextMenuX;
        public int contextMenuY;
        public int contextMenuWidth;  // 动态宽度缓存，供点击检测使用
        public int ticksTillClick;
    }

    // ===== 状态生命周期管理 =====

    /** 安全获取 State（不创建新实例），用于渲染层读取 */
    public static State getStateOrNull(Object screen) {
        return UI_STATES.get(screen);
    }

    /** 界面关闭时清理状态，防止内存泄漏和跨界面状态污染 */
    public static void cleanupState(Object screen) {
        UI_STATES.remove(screen);
        if (debugParty()) {
            CobblemonTS.LOGGER.info("[PartyUI] cleaned up state for screen={}, remaining={}",
                    screen.getClass().getSimpleName(), UI_STATES.size());
        }
    }

    /** 重置所有临时交互状态（拖拽、菜单、选中），保留动画/护符状态 */
    public static void resetInteractionState(State state) {
        if (state == null) return;
        state.selectedPartySlot = -1;
        state.dragSourceSlot = -1;
        state.contextMenuOpen = false;
        state.contextMenuSlot = -1;
        state.ticksTillClick = 0;
    }

    // ===== 每 Tick 队伍数据缓存（避免每帧无限重复反射） =====
    private static final int CACHE_VALID_TICKS = 1;
    private static long lastCacheTick = -1;
    private static List<Pokemon> cachedPartyList;

    /** 获取队伍数据（带单 Tick 缓存），返回列表可能为 null 需要判空 */
    public static List<Pokemon> getCachedPartyList() {
        long currentTick = System.currentTimeMillis(); // 用时间戳做简易缓存，跨 Tick 可用
        if (cachedPartyList != null && currentTick - lastCacheTick < 50) { // 50ms 内复用
            return cachedPartyList;
        }
        cachedPartyList = getPartyList();
        lastCacheTick = currentTick;
        return cachedPartyList;
    }

    /** 强制刷新缓存（切换队伍操作后调用） */
    public static void invalidatePartyCache() {
        cachedPartyList = null;
        lastCacheTick = -1;
    }

    // ===== 护符交互逻辑（从 Mixin 抽离到此处统一管理） =====

    /** 护符箭头点击切换 */
    public static boolean handleCharmArrowClick(State state, int leftPos, int topPos, double mouseX, double mouseY) {
        int charmArrowX = getCharmArrowX(state, leftPos);
        int charmArrowY = getCharmArrowY(topPos);
        int charmDrawerOffset = state.charmDrawerOffset;
        int charmArrowHitboxX = charmDrawerOffset > 0 ? getCharmDrawerX(state, leftPos) + charmDrawerOffset - 8 : charmArrowX - 6;
        int charmArrowHitboxY = charmArrowY - 4;

        boolean hit = mouseX >= charmArrowHitboxX && mouseX <= charmArrowHitboxX + PartyUIState.CHARM_BTN_W
                && mouseY >= charmArrowHitboxY && mouseY <= charmArrowHitboxY + PartyUIState.CHARM_BTN_H;
        if (hit) {
            state.charmDrawerOut = !state.charmDrawerOut;
        }
        return hit;
    }

    /** 护符抽屉内点击切换护符状态（布局：顶部标题 + 空1槽位 + (名称+小图标) × 5，与精灵栏对齐） */
    public static boolean handleCharmDrawerClick(State state, int leftPos, int topPos, double mouseX, double mouseY) {
        if (state.charmDrawerOffset <= CHARM_DRAWER_MIN_VISIBLE) return false;
        int charmDrawerX = getCharmDrawerX(state, leftPos);
        // 护符栏顶部 = 精灵栏顶部（与精灵栏同一顶部）
        int charmDrawerY = topPos;  // 与精灵栏同一顶部
        // 总高度 = 标题(16) + 空1个槽位(31) + 5×单元(28) = 187
        int totalHeight = CHARM_HEADER_HEIGHT + 1 * (SLOT_SIZE + SLOT_SPACING)
                + CHARM_TYPES.length * CHARM_CELL_HEIGHT;
        int drawerWidth = state.charmDrawerOffset;

        // 整体点击区域检测
        if (!(mouseX >= charmDrawerX && mouseX <= charmDrawerX + drawerWidth + 2
                && mouseY >= charmDrawerY && mouseY <= charmDrawerY + totalHeight)) {
            return false;
        }

        // 跳过标题和空槽位区域（不响应点击）
        int contentStartY = charmDrawerY + getCharmDrawerContentOffset();
        if (mouseY < contentStartY) return false;

        // 每个护符单元内检测（单元高度 = 31，与精灵槽位+间距同高）
        for (int i = 0; i < CHARM_TYPES.length; i++) {
            int cellY = contentStartY + i * CHARM_CELL_HEIGHT;
            if (mouseY >= cellY && mouseY <= cellY + CHARM_CELL_HEIGHT
                    && mouseX >= charmDrawerX && mouseX <= charmDrawerX + drawerWidth) {
                CharmType type = CHARM_TYPES[i];
                CharmState currentState = state.charmManager != null
                        ? state.charmManager.getCharmState(type)
                        : CharmState.DISABLED;
                if (debugCharm()) {
                    CobblemonTS.LOGGER.info("[PartyUI] charm clicked: type={} currentState={}", type, currentState);
                }
                if (currentState == CharmState.ACTIVE) {
                    state.charmManager.setCharmState(type, CharmState.AVAILABLE);
                } else if (currentState == CharmState.AVAILABLE) {
                    state.charmManager.setCharmState(type, CharmState.ACTIVE);
                }
                return true;
            }
        }
        return false;
    }

    // ===== 异常隔离的交互包装器（单个操作失败不阻断整个背包鼠标） =====

    /** 安全左键点击 */
    public static void safeHandleLeftClick(int slotIndex, State state, Object screen) {
        try {
            handleInteractionLeftClick(slotIndex, state, screen);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] leftClick slot={} failed: {}", slotIndex, t.getMessage());
        }
    }

    /** 安全右键点击 */
    public static void safeHandleRightClick(int slotIndex, State state) {
        try {
            handleInteractionRightClick(slotIndex, state);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] rightClick slot={} failed: {}", slotIndex, t.getMessage());
        }
    }

    /** 安全携带物点击 */
    public static void safeHandleHeldItemClick(int slotIndex, State state) {
        try {
            handleInteractionHeldItemClick(slotIndex, state);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] heldItemClick slot={} failed: {}", slotIndex, t.getMessage());
        }
    }

    /** 安全右键菜单点击 */
    public static void safeHandleContextMenu(double mouseX, double mouseY, int button, State state, Object screen) {
        try {
            handleInteractionContextMenu(mouseX, mouseY, button, state, screen);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] contextMenu click failed: {}", t.getMessage());
            state.contextMenuOpen = false;
            state.contextMenuSlot = -1;
        }
    }

    /** 安全精灵交换 */
    public static void safeSwapPartySlots(int slot1, int slot2) {
        try {
            swapPartySlots(slot1, slot2);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] swap slots {}<->{} failed: {}", slot1, slot2, t.getMessage());
        }
    }

    /** 安全护符切换 */
    public static void safeHandleCharmClick(State state, int leftPos, int topPos, double mouseX, double mouseY) {
        try {
            handleCharmDrawerClick(state, leftPos, topPos, mouseX, mouseY);
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] charm click failed: {}", t.getMessage());
        }
    }

    // ===== 坐标计算 =====
    // 精灵面板固定在 leftPos - PARTY_WIDTH，不随护符动画变化
    public static int getPartyPanelX(int leftPos) {
        return leftPos - PARTY_WIDTH;
    }

    // 护符抽屉 X：精灵面板左侧，随 charmDrawerOffset 变化（展开时向左扩展）
    public static int getCharmDrawerX(State state, int leftPos) {
        int partyPanelX = getPartyPanelX(leftPos);
        return partyPanelX - state.charmDrawerOffset - 2;
    }

    // 护符箭头按钮位置（抽屉右边缘处，显示方向箭头）
    public static int getCharmArrowX(State state, int leftPos) {
        if (state.charmDrawerOffset > 0) {
            return getCharmDrawerX(state, leftPos) + state.charmDrawerOffset - 6;
        }
        return getPartyPanelX(leftPos) - 6;
    }

    public static int getCharmArrowY(int topPos) {
        return topPos + getPanelHeight() - 20;
    }

    public static boolean isInPartyArea(State state, int leftPos, int topPos, double mouseX, double mouseY) {
        int partyX = getPartyPanelX(leftPos);
        return mouseX >= partyX && mouseX <= partyX + PARTY_WIDTH
                && mouseY >= topPos && mouseY <= topPos + getPanelHeight();
    }

    public static boolean isValidScreen(Object screen) {
        if (screen == null) return false;
        // mixin 目标是 EffectRenderingInventoryScreen，所以这里总是 true
        // 除非是被 mixin 之外的其他地方调用
        return true;
    }

    // ===== 极简反射初始化：只找 Species 名称和 Summary 打开方法 =====
    public static boolean initBasicReflection() {
        if (basicReflectionReady) return true;
        try {
            // 1. Species: 精确方法名 getTranslatedName() → Component（与 JAR 完全一致）
            Class<?> speciesClass = Class.forName("com.cobblemon.mod.common.pokemon.Species");
            try {
                speciesGetTranslatedNameMethod = speciesClass.getMethod("getTranslatedName");
                speciesGetTranslatedNameMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // 兜底：遍历方法找带 "translated" 或 "displayname" 的方法
                for (Method m : speciesClass.getMethods()) {
                    String name = m.getName().toLowerCase();
                    if (m.getParameterCount() == 0 && (name.contains("translated") || name.contains("display") || name.contains("showdown"))) {
                        speciesGetTranslatedNameMethod = m;
                        speciesGetTranslatedNameMethod.setAccessible(true);
                        break;
                    }
                }
            }
            // 精确：getResourceIdentifier()
            try {
                speciesResourceIdMethod = speciesClass.getMethod("getResourceIdentifier");
                speciesResourceIdMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
            // 兜底: Species.getName() → String
            for (Method m : speciesClass.getMethods()) {
                if (m.getReturnType() == String.class && m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.equals("getname") || name.equals("name")) {
                        speciesGetNameMethod = m;
                        break;
                    }
                }
            }
            if (speciesGetNameMethod == null) {
                speciesGetNameMethod = Object.class.getMethod("toString");
            }

            // 2. Component.getString() → String（用于从 getTranslatedName() 结果提取字符串）
            if (speciesGetTranslatedNameMethod != null) {
                Class<?> returnType = speciesGetTranslatedNameMethod.getReturnType();
                try {
                    componentGetStringMethod = returnType.getMethod("getString");
                } catch (NoSuchMethodException e) {
                    for (Method m : returnType.getMethods()) {
                        if (m.getName().equals("getString") && m.getParameterCount() == 0) {
                            componentGetStringMethod = m;
                            break;
                        }
                    }
                }
            }

            // 3. Pokemon 的携带物方法
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            for (Method m : pokemonClass.getMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("held") && name.contains("swap")) {
                    pokemonSwapHeldItemMethod = m;
                    pokemonSwapHeldItemMethod.setAccessible(true);
                }
                if (name.contains("held") && (name.contains("remove") || name.contains("clear"))) {
                    pokemonRemoveHeldItemMethod = m;
                    pokemonRemoveHeldItemMethod.setAccessible(true);
                }
            }

            // 4. Summary.open(Collection, boolean, int)
            try {
                summaryClass = Class.forName("com.cobblemon.mod.common.client.gui.summary.Summary");
                for (java.lang.reflect.Field f : summaryClass.getFields()) {
                    if (f.getName().equals("Companion")) {
                        summaryCompanionInstance = f.get(null);
                        break;
                    }
                }
                if (summaryCompanionInstance != null) {
                    for (Method m : summaryCompanionInstance.getClass().getMethods()) {
                        if (m.getName().equals("open") && m.getParameterCount() >= 2) {
                            summaryOpenMethod = m;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                if (debugParty()) CobblemonTS.LOGGER.warn("[PartyUI] Summary not available: {}", e.getMessage());
            }

            initItemSets();
            basicReflectionReady = true;
            if (debugParty()) {
                CobblemonTS.LOGGER.info("[PartyUI] reflection ready (translatedName={}, componentGetString={}, fallbackName={}, swapHeld={}, removeHeld={}, summary={})",
                        speciesGetTranslatedNameMethod != null ? speciesGetTranslatedNameMethod.getName() : "null",
                        componentGetStringMethod != null ? componentGetStringMethod.getName() : "null",
                        speciesGetNameMethod != null ? speciesGetNameMethod.getName() : "null",
                        pokemonSwapHeldItemMethod != null ? pokemonSwapHeldItemMethod.getName() : "null",
                        pokemonRemoveHeldItemMethod != null ? pokemonRemoveHeldItemMethod.getName() : "null",
                        summaryOpenMethod != null ? summaryOpenMethod.getName() : "null");
            }
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] initBasicReflection FAILED: " + t.getMessage(), t);
        }
        return basicReflectionReady;
    }

    public static void initItemSets() {
        if (itemSetsReady) return;
        HEALING_ITEMS.add("cobblemon:potion");
        HEALING_ITEMS.add("cobblemon:super_potion");
        HEALING_ITEMS.add("cobblemon:hyper_potion");
        HEALING_ITEMS.add("cobblemon:max_potion");
        HEALING_ITEMS.add("cobblemon:full_restore");
        STATUS_CURE_ITEMS.add("cobblemon:antidote");
        STATUS_CURE_ITEMS.add("cobblemon:awakening");
        STATUS_CURE_ITEMS.add("cobblemon:burn_heal");
        STATUS_CURE_ITEMS.add("cobblemon:ice_heal");
        STATUS_CURE_ITEMS.add("cobblemon:paralyze_heal");
        STATUS_CURE_ITEMS.add("cobblemon:full_heal");
        STATUS_CURE_ITEMS.add("cobblemon:full_restore");
        STATUS_CURE_ITEMS.add("cobblemon:remedy");
        STATUS_CURE_ITEMS.add("cobblemon:fine_remedy");
        STATUS_CURE_ITEMS.add("cobblemon:superb_remedy");
        STATUS_CURE_ITEMS.add("cobblemon:heal_powder");
        REVIVE_ITEMS.add("cobblemon:revive");
        REVIVE_ITEMS.add("cobblemon:max_revive");
        itemSetsReady = true;
    }

    // ===== 核心：从 Cobblemon API 直接获取队伍 =====
    public static Pokemon getPartyPokemon(int index) {
        List<Pokemon> party = getPartyList();
        if (party != null && index >= 0 && index < party.size()) {
            return party.get(index);
        }
        return null;
    }

    /**
     * 从 Cobblemon 客户端获取队伍 - 反编译确认的标准调用链：
     * CobblemonClient.INSTANCE.getStorage().getParty().getSlots() → List<Pokemon>
     *
     * 为什么用反射取 ClientParty：ClientParty 实现了 KMappedMarker（Kotlin 内部接口），
     * 但 Pokemon 和 Species 是普通类，可以直接调用方法。
     */
    @SuppressWarnings("unchecked")
    public static List<Pokemon> getPartyList() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return emptyParty(6);

            // 1. CobblemonClient.INSTANCE.getStorage() → ClientStorageManager
            Object clientStorage = CobblemonClient.INSTANCE.getStorage();
            if (clientStorage == null) {
                if (debugParty()) CobblemonTS.LOGGER.warn("[PartyUI] clientStorage is null");
                return emptyParty(6);
            }

            // 2. ClientStorageManager.getParty() → ClientParty
            // 用反射避免直接引用 ClientParty 类型（KMappedMarker）
            Object clientParty = null;
            try {
                java.lang.reflect.Method getPartyMethod = clientStorage.getClass().getMethod("getParty");
                clientParty = getPartyMethod.invoke(clientStorage);
            } catch (Throwable t) {
                CobblemonTS.LOGGER.warn("[PartyUI] getParty() failed: {}", t.getMessage());
            }

            if (clientParty == null) {
                if (debugParty()) CobblemonTS.LOGGER.warn("[PartyUI] clientParty is null");
                return emptyParty(6);
            }

            List<Pokemon> result = new ArrayList<>();

            // 3. ClientParty.getSlots() → List<Pokemon>
            try {
                // [优化-P01] 缓存getSlots方法，避免每帧反射查找
                if (cachedClientGetSlotsMethod == null) {
                    cachedClientGetSlotsMethod = clientParty.getClass().getMethod("getSlots");
                }
                Object rawList = cachedClientGetSlotsMethod.invoke(clientParty);
                if (rawList instanceof List<?>) {
                    for (Object o : (List<?>) rawList) {
                        result.add((Pokemon) o);
                    }
                }
            } catch (Throwable t) {
                CobblemonTS.LOGGER.warn("[PartyUI] getSlots() failed: {}", t.getMessage());
            }

            // 回退：ClientParty.get(int)
            if (result.isEmpty()) {
                // [优化-P01] 缓存get(int)方法
                if (cachedClientGetIntMethod == null) {
                    cachedClientGetIntMethod = clientParty.getClass().getMethod("get", int.class);
                }
                for (int i = 0; i < 6; i++) {
                    try {
                        Pokemon p = (Pokemon) cachedClientGetIntMethod.invoke(clientParty, i);
                        result.add(p);
                    } catch (Throwable e) {
                        result.add(null);
                    }
                }
            }

            while (result.size() < 6) result.add(null);
            if (result.size() > 6) result = new ArrayList<>(result.subList(0, 6));

            if (debugParty()) {
                int nonNullCount = 0;
                for (Pokemon p : result) if (p != null) nonNullCount++;
                CobblemonTS.LOGGER.info("[PartyUI] got party: size={} nonNull={}", result.size(), nonNullCount);
            }
            return result;
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] getPartyList FAILED: " + t.getMessage(), t);
            return emptyParty(6);
        }
    }

    private static List<Pokemon> emptyParty(int size) {
        List<Pokemon> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(null);
        return list;
    }

    // ===== Pokemon 信息获取：与 coblemon_ts-1.0.1.jar 反射调用一致 =====

    /**
     * 获取精灵翻译名 - 与 coblemon_ts-1.0.1.jar 字节码完全一致
     * 流程: pokemon.getSpecies() → species.getTranslatedName() → Component.getString()
     * 失败兜底: PokemonNameMap.toChinese(species.getResourceIdentifier())
     */
    public static String getPokemonName(Pokemon pokemon) {
        if (pokemon == null) return "";
        try {
            Object species = pokemon.getSpecies();
            if (species == null) return "";

            // 1. 优先: species.getTranslatedName() → Component.getString() (与 JAR 一致)
            if (speciesGetTranslatedNameMethod != null) {
                Object nameComponent = speciesGetTranslatedNameMethod.invoke(species);
                if (nameComponent != null) {
                    if (componentGetStringMethod == null) {
                        // JAR 懒加载方式：从实际返回的组件实例获取 getString 方法
                        componentGetStringMethod = nameComponent.getClass().getMethod("getString");
                        if (debugParty()) {
                            CobblemonTS.LOGGER.info("[PartyUI] componentGetStringMethod initialized: {}", 
                                componentGetStringMethod);
                        }
                    }
                    if (componentGetStringMethod != null) {
                        String name = (String) componentGetStringMethod.invoke(nameComponent);
                        if (name != null && !name.isEmpty()) {
                            if (debugParty()) {
                                CobblemonTS.LOGGER.info("[PartyUI] getTranslatedName().getString() = \"{}\"", name);
                            }
                            // 如果返回的是资源 key（如 cobblemon.species.bulbasaur），用 PokemonNameMap 兜底
                            if (name.contains("cobblemon.species.") || name.equals(species.toString())) {
                                String speciesId = getSpeciesId(species);
                                String zhName = com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap.toChinese(speciesId);
                                if (zhName != null && !zhName.equals(speciesId)) {
                                    if (debugParty()) {
                                        CobblemonTS.LOGGER.info("[PartyUI] PokemonNameMap.toChinese({}) = \"{}\"", speciesId, zhName);
                                    }
                                    return zhName;
                                }
                            }
                            return name;
                        }
                    }
                }
            }

            // 2. 兜底: 用 species 的资源 ID 查 PokemonNameMap
            String speciesId = getSpeciesId(species);
            String zhName = com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap.toChinese(speciesId);
            if (debugParty()) {
                CobblemonTS.LOGGER.info("[PartyUI] fallback PokemonNameMap.toChinese({}) = \"{}\"", speciesId, zhName);
            }
            if (zhName != null && !zhName.equals(speciesId)) return zhName;
            return species.toString();
        } catch (Throwable t) {
            return "?";
        }
    }

    /** 获取物种英文 ID（如 bulbasaur）—— 与 extractStringId 一致：去掉命名空间前缀 */
    private static String getSpeciesId(Object species) {
        try {
            if (speciesResourceIdMethod == null) {
                speciesResourceIdMethod = species.getClass().getMethod("getResourceIdentifier");
                speciesResourceIdMethod.setAccessible(true);
            }
            Object idObj = speciesResourceIdMethod.invoke(species);
            if (idObj != null) {
                // 优先：如果是 ResourceLocation，尝试调用 getPath()
                try {
                    Method getPathMethod = idObj.getClass().getMethod("getPath");
                    String path = (String) getPathMethod.invoke(idObj);
                    if (path != null) return path.toLowerCase().trim();
                } catch (Throwable ignored) {}
                // 兜底：toString() 后去掉 : 前缀（如 cobblemon:bulbasaur → bulbasaur）
                String s = idObj.toString();
                if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
                return s.toLowerCase().trim();
            }
        } catch (Throwable ignored) {}
        return species.toString().toLowerCase().trim();
    }

    public static int getPokemonLevel(Pokemon pokemon) {
        if (pokemon == null) return 0;
        try {
            return pokemon.getLevel();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getPokemonCurrentHP(Pokemon pokemon) {
        if (pokemon == null) return 0;
        try {
            return pokemon.getCurrentHealth();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getPokemonMaxHP(Pokemon pokemon) {
        if (pokemon == null) return 1;
        try {
            int hp = pokemon.getMaxHealth();
            return hp > 0 ? hp : 1;
        } catch (Throwable t) {
            return 1;
        }
    }

    public static ItemStack getPokemonHeldItem(Pokemon pokemon) {
        if (pokemon == null) return ItemStack.EMPTY;
        try {
            ItemStack item = pokemon.heldItem();
            return item != null ? item : ItemStack.EMPTY;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack getPokemonItem(Pokemon pokemon) {
        if (pokemon == null) return ItemStack.EMPTY;
        try {
            return com.cobblemon.mod.common.item.PokemonItem.from(pokemon);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    public static ResourceLocation getPokemonStatusIcon(Pokemon pokemon) {
        if (pokemon == null) return null;
        try {
            Object status = pokemon.getStatus();
            if (status == null) return null;
            Method getIcon = status.getClass().getMethod("getIcon");
            Object icon = getIcon.invoke(status);
            if (icon instanceof ResourceLocation loc) {
                return loc;
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    // ===== 宝可梦位置交换（通过我们自己的 CustomPacketPayload 发送到服务器） =====
    // 同时做客户端乐观更新：立即更新本地缓存，消除服务器往返导致的闪动
    public static void swapPartySlots(int slot1, int slot2) {
        if (slot1 == slot2 || slot1 < 0 || slot1 >= 6 || slot2 < 0 || slot2 >= 6) return;
        try {
            // 乐观更新：先更新本地缓存，消除闪动
            List<Pokemon> currentParty = getPartyList();
            if (currentParty.size() >= Math.max(slot1, slot2) + 1) {
                Pokemon p1 = currentParty.get(slot1);
                Pokemon p2 = currentParty.get(slot2);
                // ArrayList 支持 set，如果是不可变列表会抛异常（忽略即可）
                try {
                    currentParty.set(slot1, p2);
                    currentParty.set(slot2, p1);
                    cachedPartyList = currentParty;
                } catch (Throwable ignored) {}
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.cobblemon_ts.mod.network.SwapPartySlotsPacket(slot1, slot2));
            if (debugClick()) {
                CobblemonTS.LOGGER.info("[PartyUI] swap slot {} <-> {}: packet sent, local cache updated", slot1, slot2);
            }
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("[PartyUI] swap slots failed: {}", t.getMessage());
        }
    }

    private static boolean trySendCobblemonPacket(Object packet) {
        try {
            // 方式 1: 找 Cobblemon 的 Network 通道
            Class<?> networkClass = Class.forName("com.cobblemon.mod.common.net.messages.PokemonMessagesKt");
            for (Method m : networkClass.getMethods()) {
                if (m.getName().toLowerCase().contains("send") && m.getParameterCount() == 1) {
                    try {
                        m.invoke(null, packet);
                        return true;
                    } catch (Throwable ignored) {}
                }
            }

            // 方式 2: 查找 Cobblemon 主类的 network 通道
            Class<?> cobblemonClass = Cobblemon.class;
            for (java.lang.reflect.Field f : cobblemonClass.getFields()) {
                if (f.getName().toLowerCase().contains("network") || f.getName().toLowerCase().contains("channel")) {
                    try {
                        Object channel = f.get(null);
                        for (Method m : channel.getClass().getMethods()) {
                            if ((m.getName().equals("sendToServer") || m.getName().equals("send")) && m.getParameterCount() == 1) {
                                m.invoke(channel, packet);
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 方式 3: 直接用 NeoForge 网络发送（如果 packet 是 CustomPacketPayload）
            if (packet instanceof net.minecraft.network.protocol.common.custom.CustomPacketPayload) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) packet);
                return true;
            }
        } catch (Throwable t) {
            CobblemonTS.LOGGER.warn("[PartyUI] trySendCobblemonPacket failed: {}", t.getMessage());
        }
        return false;
    }

    // ===== 交互处理 =====
    public static void handleInteractionContextMenu(double mouseX, double mouseY, int button, State state, Object screen) {
        // 无条件日志：记录每一次右键菜单点击事件
        CobblemonTS.LOGGER.info("[PartyUI-Menu] btn={} clickPos=({},{}) menuSlot={} menuPos=({},{}) width={}",
                button, (int) mouseX, (int) mouseY, state.contextMenuSlot, state.contextMenuX, state.contextMenuY, state.contextMenuWidth);

        if (button != 0) {
            state.contextMenuOpen = false;
            return;
        }

        int itemH = CONTEXT_MENU_ITEM_HEIGHT;
        int menuW = state.contextMenuWidth > 0 ? state.contextMenuWidth : 70;
        int menuH = 5 * itemH;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int menuX = state.contextMenuX;
        int menuY = state.contextMenuY;
        if (menuX + menuW > screenW) menuX = screenW - menuW - 4;
        if (menuY + menuH > screenH) menuY = screenH - menuH - 4;
        if (menuX < 0) menuX = 4;
        if (menuY < 0) menuY = 4;

        CobblemonTS.LOGGER.info("[PartyUI-Menu] rect x={} y={} w={} h={}", menuX, menuY, menuW, menuH);

        String[] actionNames = {"SUMMARY", "SET_FIRST", "EVOLVE", "SEND_PC", "RELEASE"};
        for (int i = 0; i < 5; i++) {
            int itemY = menuY + i * itemH;
            if (mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= itemY && mouseY <= itemY + itemH) {
                CobblemonTS.LOGGER.info("[PartyUI-Menu] >>> item {} ({}) clicked at slot {} <<<",
                        i, actionNames[i], state.contextMenuSlot);
                state.contextMenuOpen = false;
                switch (i) {
                    case 0 -> openPokemonSummary(state.contextMenuSlot);
                    case 1 -> moveToSlotZero(state.contextMenuSlot, state);
                    case 2 -> openPokemonEvolution(state.contextMenuSlot);
                    case 3 -> moveToPC(state.contextMenuSlot, state);
                    case 4 -> releasePokemon(state.contextMenuSlot, state);
                }
                return;
            }
        }
        CobblemonTS.LOGGER.info("[PartyUI-Menu] click outside menu bounds, closing");
        state.contextMenuOpen = false;
    }

    public static void handleInteractionLeftClick(int slotIndex, State state, Object screen) {
        ItemStack cursorStack = Minecraft.getInstance().player.containerMenu.getCarried();
        if (!cursorStack.isEmpty()) {
            if (debugSlot()) {
                CobblemonTS.LOGGER.info("[PartyUI] leftClick slot={} item={}", slotIndex, cursorStack.getHoverName().getString());
            }
            handleUseItemOnPokemon(slotIndex, cursorStack, state);
        } else {
            if (debugSlot()) {
                CobblemonTS.LOGGER.info("[PartyUI] leftClick slot={} drag from={}", slotIndex, state.dragSourceSlot);
            }
            if (state.dragSourceSlot >= 0 && state.dragSourceSlot != slotIndex) {
                safeSwapPartySlots(state.dragSourceSlot, slotIndex);
                state.dragSourceSlot = -1;
                state.selectedPartySlot = -1;
                state.ticksTillClick = 10;
            } else {
                state.dragSourceSlot = (state.dragSourceSlot == slotIndex) ? -1 : slotIndex;
                state.selectedPartySlot = state.dragSourceSlot;
            }
        }
    }

    public static void handleInteractionRightClick(int slotIndex, State state) {
        state.contextMenuOpen = true;
        state.contextMenuSlot = slotIndex;
    }

    public static void handleInteractionHeldItemClick(int slotIndex, State state) {
        Pokemon pokemon = getPartyPokemon(slotIndex);
        if (pokemon == null) return;
        try {
            ItemStack cursorStack = Minecraft.getInstance().player.containerMenu.getCarried();
            ItemStack currentHeld = getPokemonHeldItem(pokemon);
            boolean cursorHas = !cursorStack.isEmpty();
            boolean heldHas = !currentHeld.isEmpty();
            if (!cursorHas && !heldHas) return;

            if (debugSlot()) {
                CobblemonTS.LOGGER.info("[PartyUI] heldItemClick slot={} cursor={} held={}", slotIndex,
                        cursorHas ? cursorStack.getCount() + "x" + cursorStack.getHoverName().getString() : "EMPTY",
                        heldHas ? currentHeld.getHoverName().getString() : "EMPTY");
            }

            net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;

            if (!cursorHas && heldHas) {
                player.containerMenu.setCarried(currentHeld.copy());
                if (pokemonRemoveHeldItemMethod != null) pokemonRemoveHeldItemMethod.invoke(pokemon);
            } else if (cursorHas && !heldHas) {
                ItemStack one = cursorStack.copy();
                one.setCount(1);
                if (pokemonSwapHeldItemMethod != null) {
                    int paramCount = pokemonSwapHeldItemMethod.getParameterCount();
                    if (paramCount == 1) {
                        pokemonSwapHeldItemMethod.invoke(pokemon, one);
                    } else if (paramCount == 3) {
                        pokemonSwapHeldItemMethod.invoke(pokemon, one, false, false);
                    }
                }
                cursorStack.shrink(1);
                if (cursorStack.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
            } else {
                ItemStack oldHeld = currentHeld.copy();
                if (pokemonRemoveHeldItemMethod != null) pokemonRemoveHeldItemMethod.invoke(pokemon);
                ItemStack newHeld = cursorStack.copy();
                newHeld.setCount(1);
                if (pokemonSwapHeldItemMethod != null) {
                    int paramCount = pokemonSwapHeldItemMethod.getParameterCount();
                    if (paramCount == 1) {
                        pokemonSwapHeldItemMethod.invoke(pokemon, newHeld);
                    } else if (paramCount == 3) {
                        pokemonSwapHeldItemMethod.invoke(pokemon, newHeld, false, false);
                    }
                }
                cursorStack.shrink(1);
                if (cursorStack.isEmpty()) {
                    player.containerMenu.setCarried(oldHeld);
                } else {
                    if (!player.getInventory().add(oldHeld)) player.drop(oldHeld, false);
                }
            }

            UUID pokemonUuid = pokemon.getUuid();
            com.cobblemon_ts.mod.network.SetHeldItemPacket packet =
                    new com.cobblemon_ts.mod.network.SetHeldItemPacket(pokemonUuid, slotIndex);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
            state.ticksTillClick = 10;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI] Failed to swap held item: " + e.getMessage());
        }
    }

    public static void handleUseItemOnPokemon(int slotIndex, ItemStack cursorStack, State state) {
        Pokemon pokemon = getPartyPokemon(slotIndex);
        if (pokemon == null || cursorStack.isEmpty()) return;
        try {
            boolean altHeld = net.minecraft.client.gui.screens.Screen.hasAltDown();
            boolean shiftHeld = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            int useCount = Math.min(altHeld ? cursorStack.getCount() : 1, cursorStack.getCount());
            UUID pokemonUuid = pokemon.getUuid();
            // [修复-F01] PP Max 的 allMoves 改为 alt/shift 均可触发，与其他物品操作逻辑一致
            boolean isPPMax = BuiltInRegistries.ITEM.getKey(cursorStack.getItem()).toString()
                    .equals("cobblemon:pp_max");
            boolean allMoves = (shiftHeld || altHeld) && isPPMax;
            com.cobblemon_ts.mod.network.UseItemOnPokemonPacket packet =
                    new com.cobblemon_ts.mod.network.UseItemOnPokemonPacket(pokemonUuid,
                            BuiltInRegistries.ITEM.getKey(cursorStack.getItem()).toString(), useCount, allMoves);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);

            // ★ 修复假消耗：不再在客户端抢先shrink
            // 物品消耗统一由服务端UseItemOnPokemonPacket.handleServerSide处理
            // 服务端确认消耗后，背包同步机制会自动更新客户端显示

            if (debugSlot()) {
                CobblemonTS.LOGGER.info("[PartyUI] useItem sent slot={} count={} shift={} allMoves={}",
                        slotIndex, useCount, shiftHeld, allMoves);
            }
            state.ticksTillClick = 10;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI] Failed to use item on pokemon: " + e.getMessage());
        }
    }

    public static void moveToPC(int slotIndex, State state) {
        Pokemon pokemon = getPartyPokemon(slotIndex);
        if (pokemon == null) return;
        try {
            PartyToPCPacket packet = new PartyToPCPacket(pokemon.getUuid());
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
            state.ticksTillClick = 10;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI] moveToPC failed: " + e.getMessage());
        }
    }

    public static void releasePokemon(int slotIndex, State state) {
        ReleasePokemonPacket packet = new ReleasePokemonPacket(slotIndex);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
        state.ticksTillClick = 10;
    }

    public static void moveToSlotZero(int slotIndex, State state) {
        if (slotIndex == 0) return;
        try {
            safeSwapPartySlots(slotIndex, 0);
            state.ticksTillClick = 10;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI] move to slot zero failed: " + e.getMessage());
        }
    }

    public static void openPokemonSummary(int slotIndex) {
        initBasicReflection();
        try {
            if (summaryOpenMethod == null || summaryCompanionInstance == null) {
                if (debugParty()) CobblemonTS.LOGGER.warn("[PartyUI] Summary not available, cannot open");
                return;
            }
            List<Pokemon> party = getPartyList();
            if (slotIndex >= party.size()) return;
            summaryOpenMethod.invoke(summaryCompanionInstance, party, true, slotIndex);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI] open summary failed: " + e.getMessage());
        }
    }

    public static void openPokemonEvolution(int slotIndex) {
        Pokemon pokemon = getPartyPokemon(slotIndex);
        if (pokemon == null) {
            CobblemonTS.LOGGER.warn("[PartyUI-Evol] pokemon null for slot {}", slotIndex);
            return;
        }
        java.util.UUID pokemonUUID = pokemon.getUuid();
        CobblemonTS.LOGGER.info("[PartyUI-Evol] slot={} species={} uuid={}",
                slotIndex, pokemon.getSpecies().showdownId(), pokemonUUID);

        try {
            // 1. 获取所有可能的进化（基于物种定义）
            java.util.List<String> evolutionIds = new java.util.ArrayList<>();
            try {
                Iterable<?> evolutions = pokemon.getEvolutions();
                if (evolutions != null) {
                    for (Object e : evolutions) {
                        try {
                            java.lang.reflect.Method getId = e.getClass().getMethod("getId");
                            Object id = getId.invoke(e);
                            if (id instanceof String) {
                                evolutionIds.add((String) id);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable t) {
                CobblemonTS.LOGGER.warn("[PartyUI-Evol] 直接读取进化列表失败, 尝试从 client controller 获取: {}", t.getMessage());
            }

            // 2. 兜底：从 client EvolutionController 获取当前可用的进化
            if (evolutionIds.isEmpty()) {
                try {
                    Object proxy = pokemon.getEvolutionProxy();
                    if (proxy != null) {
                        java.lang.reflect.Method clientMethod = proxy.getClass().getMethod("client");
                        Object clientController = clientMethod.invoke(proxy);
                        if (clientController instanceof Iterable<?>) {
                            for (Object e : (Iterable<?>) clientController) {
                                try {
                                    java.lang.reflect.Method getId = e.getClass().getMethod("getId");
                                    Object id = getId.invoke(e);
                                    if (id instanceof String) {
                                        evolutionIds.add((String) id);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                } catch (Throwable t) {
                    CobblemonTS.LOGGER.warn("[PartyUI-Evol] 从 client controller 获取进化失败: {}", t.getMessage());
                }
            }

            if (evolutionIds.isEmpty()) {
                CobblemonTS.LOGGER.warn("[PartyUI-Evol] 没有找到任何进化选项");
                return;
            }

            CobblemonTS.LOGGER.info("[PartyUI-Evol] 找到 {} 个进化选项: {}", evolutionIds.size(), evolutionIds);

            // 3. 构造并发送 AcceptEvolutionPacket 到服务端
            Class<?> packetClass = Class.forName(
                    "com.cobblemon.mod.common.net.messages.server.pokemon.update.evolution.AcceptEvolutionPacket");
            java.lang.reflect.Constructor<?> ctorUUID =
                    packetClass.getConstructor(java.util.UUID.class, String.class);
            java.lang.reflect.Method sendToServer = packetClass.getMethod("sendToServer");

            int successCount = 0;
            for (String evolutionId : evolutionIds) {
                try {
                    Object packet = ctorUUID.newInstance(pokemonUUID, evolutionId);
                    sendToServer.invoke(packet);
                    successCount++;
                    CobblemonTS.LOGGER.info("[PartyUI-Evol] 已发送进化请求 -> {}", evolutionId);
                } catch (Throwable t) {
                    CobblemonTS.LOGGER.error("[PartyUI-Evol] 发送进化包失败 ({}): {}", evolutionId, t.getMessage());
                    // 尝试备用构造函数：AcceptEvolutionPacket(Pokemon, EvolutionDisplay)
                    try {
                        java.lang.reflect.Constructor<?> ctorPokemon = null;
                        for (java.lang.reflect.Constructor<?> ctor : packetClass.getConstructors()) {
                            Class<?>[] params = ctor.getParameterTypes();
                            if (params.length == 2 && !params[0].equals(java.util.UUID.class)) {
                                ctorPokemon = ctor;
                                break;
                            }
                        }
                        if (ctorPokemon != null) {
                            Object evolutionObj = null;
                            Iterable<?> evolutions = pokemon.getEvolutions();
                            if (evolutions != null) {
                                for (Object e : evolutions) {
                                    java.lang.reflect.Method getId = e.getClass().getMethod("getId");
                                    Object id = getId.invoke(e);
                                    if (id != null && id.toString().equals(evolutionId)) {
                                        evolutionObj = e;
                                        break;
                                    }
                                }
                            }
                            if (evolutionObj != null) {
                                Object packet = ctorPokemon.newInstance(pokemon, evolutionObj);
                                sendToServer.invoke(packet);
                                successCount++;
                                CobblemonTS.LOGGER.info("[PartyUI-Evol] 备用方式发送进化请求 -> {}", evolutionId);
                            }
                        }
                    } catch (Throwable t2) {
                        CobblemonTS.LOGGER.error("[PartyUI-Evol] 备用方式也失败: {}", t2.getMessage());
                    }
                }
            }

            if (successCount == 0) {
                CobblemonTS.LOGGER.error("[PartyUI-Evol] 所有进化请求都发送失败");
            } else {
                CobblemonTS.LOGGER.info("[PartyUI-Evol] 成功发送 {} 个进化请求", successCount);
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[PartyUI-Evol] 进化流程整体失败: {}", e.getMessage(), e);
        }
    }
}