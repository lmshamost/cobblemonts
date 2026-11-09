 package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import com.cobblemon_ts.mod.network.SyncConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChunkBarrierScreen extends AbstractContainerScreen<ChunkBarrierContainer> {

    private static final int LEFT_W = 160;
    private static final int RIGHT_SIZE = 256;
    private static final int PAD = 8;
    private static final int BTN_H = 28;

    private static final int COLOR_POKEMON   = 0xE74C3C;
    private static final int COLOR_NPC       = 0x3498DB;
    private static final int COLOR_HOSTILE   = 0xE67E22;
    private static final int COLOR_PASSIVE   = 0x2ECC71;
    private static final int COLOR_FORCELOAD = 0x9B59B6;
    private static final int COLOR_LUCKY     = 0x444444;
    private static final int COLOR_PLAYER    = 0xFFFFFF;
    private static final int COLOR_GRID      = 0xFF333333;
    private static final int COLOR_HOVER     = 0x55FFFFFF;
    private static final int COLOR_UNLOADED  = 0xFF1a1a1a;
    private static final int COLOR_OFF       = 0xFF888888;

    // [线性-标记布局] tile 内纯色块位置映射（数字=按钮序号）
    // 1=精灵禁止 2=NPC禁止 3=敌对 4=中立 5=区块加载
    // 6=幸运方块(R10) 7=玩家名单(R10)
    // 布局:
    //   col0 col1 col2 col3 col4
    // r0   1    6    5    7    3
    // r1   2    6    5    7    4
    // 色块尺寸按 tileSize 动态切分

    private static final int BIT_POKEMON = ChunkBarrierConfig.BIT_POKEMON;
    private static final int BIT_NPC     = ChunkBarrierConfig.BIT_NPC;
    private static final int BIT_HOSTILE = ChunkBarrierConfig.BIT_HOSTILE;
    private static final int BIT_PASSIVE = ChunkBarrierConfig.BIT_PASSIVE;
    private static final int BIT_LOAD    = ChunkBarrierConfig.BIT_FORCELOAD;
    private static final int BIT_LUCKY   = ChunkBarrierConfig.BIT_LUCKY;
    private static final int BIT_PLAYER  = ChunkBarrierConfig.BIT_PLAYER;

    private static final int BAN_BITS = BIT_POKEMON | BIT_NPC | BIT_HOSTILE | BIT_PASSIVE;
    private static final int FORCE_BITS = BIT_LOAD;

    private static final int[] BITS = {BIT_POKEMON, BIT_NPC, BIT_HOSTILE, BIT_PASSIVE, BIT_LOAD};
    private static final int[] BTN_COLORS = {
            COLOR_POKEMON, COLOR_NPC, COLOR_HOSTILE, COLOR_PASSIVE, COLOR_FORCELOAD
    };
    private static final String[] BTN_KEYS = {
            "cobblemon_ts.chunk_barrier.ban_pokemon",
            "cobblemon_ts.chunk_barrier.ban_npc",
            "cobblemon_ts.chunk_barrier.ban_hostile",
            "cobblemon_ts.chunk_barrier.ban_passive",
            "cobblemon_ts.chunk_barrier.force_load"
    };

    private final Set<Integer> activeBits = new HashSet<>();
    private final Map<Long, Integer> terrainCache = new HashMap<>();
    private int gridSize;
    private int maxRange;

    private int mapLeft, mapTop, mapSize, tileSize;
    private int centerChunkX, centerChunkZ;

    private boolean dragging = false;
    private int dragButton = -1;
    private boolean addMode = true;
    private final Set<Integer> dragTouched = new HashSet<>();

    // [线性-超限] Force Load 超限一次性提示标志（防止刷屏）
    private boolean forceLimitNotified = false;
    // [线性-超限] Ban 超限一次性提示标志
    private boolean banLimitNotified = false;

    // [线性-全局计数] 服务端返回的玩家全服总数（跨所有 BE），null=还没响应
    private int[] globalCounts = null;

    public ChunkBarrierScreen(ChunkBarrierContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = LEFT_W + RIGHT_SIZE + PAD * 3;
        this.imageHeight = 32 + RIGHT_SIZE + PAD * 2 + R10_PANEL_H;
    }

    // [线性-R10] 玩家名单输入框
    private EditBox playerNameInput;
    // [线性-R10] 当前选中的玩家名单条目（右键删除用）
    private int selectedPlayerEntry = -1;
    private static final int TARGET_PLAYER = 0;
    private static final int TARGET_LUCKY = 1;
    private int editingTarget = TARGET_PLAYER;

    private static final int R10_PANEL_H = 132;

    private int cts_origW, cts_origH;
    private float cts_scale = 1.0f;
    private boolean cts_needScale = false;

    private Map<String, Boolean> currentNameMap() {
        return editingTarget == TARGET_LUCKY
                ? this.menu.getConfig().luckyPlayerWhitelist
                : this.menu.getConfig().playerWhitelist;
    }
    private int currentNameColor() {
        return editingTarget == TARGET_LUCKY ? 0xFFFFAA22 : 0xFFFFFFFF;
    }
    private Map<String, Boolean> playerMap() {
        return this.menu.getConfig().playerWhitelist;
    }
    private Map<String, Boolean> luckyMap() {
        return this.menu.getConfig().luckyPlayerWhitelist;
    }
    private int playerNameColor() { return 0xFFFFFFFF; }
    private int luckyNameColor() { return 0xFFFFAA22; }

    private void drawNameListSection(GuiGraphics g, int x, int y, int w, int h,
                                     String title, Map<String, Boolean> map, int nameColor,
                                     int rowH, int colW) {
        var font = this.font;
        int titleY = y + 2;
        g.drawString(font, title, x + 2, titleY, 0xFFAAAAAA);

        int entryStartY = titleY + rowH;
        int areaH = h - rowH - 2;
        if (areaH < 4) return;

        int maxRowsPerCol = Math.max(1, areaH / rowH);
        int entryCount = map.size();
        int cols = Math.max(1, (int) Math.ceil((double) entryCount / maxRowsPerCol));
        cols = Math.min(cols, 3);
        maxRowsPerCol = Math.max(1, Math.min(maxRowsPerCol, (int) Math.ceil((double) entryCount / cols)));

        int idx = 0;
        for (var e : map.entrySet()) {
            int row = idx / cols;
            int col = idx % cols;
            if (row >= maxRowsPerCol) break;
            boolean isWl = e.getValue();
            int tagColor = isWl ? 0xFFFFFFFF : 0xFF666666;
            String tag = isWl
                    ? Component.translatable("cobblemon_ts.chunk_barrier.tag.white").getString()
                    : Component.translatable("cobblemon_ts.chunk_barrier.tag.black").getString();
            String namePart = " " + e.getKey();

            int ex = x + 2 + col * colW;
            int ey = entryStartY + row * rowH;

            g.drawString(font, tag, ex, ey, tagColor);
            int tagW = font.width(tag);
            g.drawString(font, namePart, ex + tagW, ey, nameColor);
            idx++;
        }
        if (map.isEmpty()) {
            g.drawString(font, "(空)", x + 4, entryStartY, 0xFF555555);
        }
    }

    private int sampleChunkColor(Level level, int cx, int cz) {
        ChunkAccess chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return COLOR_UNLOADED;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int totalR = 0, totalG = 0, totalB = 0, count = 0;
        for (int lx = 0; lx < 16; lx += 4) {
            for (int lz = 0; lz < 16; lz += 4) {
                int wx = (cx << 4) + lx;
                int wz = (cz << 4) + lz;
                for (int y = maxY - 1; y >= minY; y--) {
                    var state = chunk.getBlockState(new BlockPos(wx, y, wz));
                    if (state.isAir()) continue;
                    MapColor mc = state.getMapColor(level, new BlockPos(wx, y, wz));
                    if (mc != MapColor.NONE) {
                        totalR += (mc.col >> 16) & 0xFF;
                        totalG += (mc.col >> 8) & 0xFF;
                        totalB += mc.col & 0xFF;
                        count++;
                        break;
                    }
                }
            }
        }
        if (count == 0) return COLOR_UNLOADED;
        return 0xFF000000 | ((totalR / count) << 16) | ((totalG / count) << 8) | (totalB / count);
    }

    private void sampleAllTerrain(Level level) {
        terrainCache.clear();
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int cx = centerChunkX + (gx - gridSize / 2);
                int cz = centerChunkZ + (gz - gridSize / 2);
                long key = ChunkBarrierConfig.chunkKey(cx, cz);
                terrainCache.put(key, sampleChunkColor(level, cx, cz));
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.activeBits.add(BIT_POKEMON);
        this.activeBits.add(BIT_NPC);
        this.activeBits.add(BIT_HOSTILE);
        this.activeBits.add(BIT_PASSIVE);
        this.activeBits.add(BIT_LOAD);
        this.activeBits.add(BIT_PLAYER);
        this.maxRange = CharmConfig.getCbMaxRange();
        this.gridSize = Math.min(CharmConfig.getCbDefaultRange(), maxRange);
        if (gridSize % 2 == 0) gridSize++;

        int baseX = leftPos + PAD;
        boolean ro = isReadOnly();
        ChunkBarrierConfig cfg = this.menu.getConfig();

        if (cfg.banLuckyBlock) this.activeBits.add(BIT_LUCKY);

        int baseY = topPos + 32;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(
                    Component.literal(""),
                    btn -> {
                        int bit = BITS[idx];
                        if (activeBits.contains(bit)) activeBits.remove(bit);
                        else activeBits.add(bit);
                    }
            ).bounds(baseX, baseY + i * (BTN_H + 4), LEFT_W - PAD * 2, BTN_H).build());
        }

        // addMode 切换
        int addModeY = baseY + 5 * (BTN_H + 4);
        addRenderableWidget(Button.builder(
                Component.literal(""),
                btn -> addMode = !addMode
        ).bounds(baseX, addModeY, LEFT_W - PAD * 2, BTN_H).build());

        // R10 开关：放在 addMode 下面，空文字跟批量按钮样式完全一样
        int r10Y1 = addModeY + BTN_H + 4;
        int r10Y2 = r10Y1 + BTN_H + 4;

        addRenderableWidget(Button.builder(
                Component.literal(""),
                btn -> {
                    if (ro) return;
                    boolean newState = !activeBits.contains(BIT_LUCKY);
                    if (newState) activeBits.add(BIT_LUCKY);
                    else activeBits.remove(BIT_LUCKY);
                    this.menu.getConfig().banLuckyBlock = newState;
                    syncToServer();
                }
        ).bounds(baseX, r10Y1, LEFT_W - PAD * 2, BTN_H).build());

        addRenderableWidget(Button.builder(
                Component.literal(""),
                btn -> {
                    if (ro) return;
                    if (activeBits.contains(BIT_PLAYER)) activeBits.remove(BIT_PLAYER);
                    else activeBits.add(BIT_PLAYER);
                }
        ).bounds(baseX, r10Y2, LEFT_W - PAD * 2, BTN_H).build());

        this.centerChunkX = this.menu.getBlockPos().getX() >> 4;
        this.centerChunkZ = this.menu.getBlockPos().getZ() >> 4;
        layoutMap();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) sampleAllTerrain(mc.level);

        // [线性-玩家名单] 输入框 + 添加按钮放底部面板左侧区域
        int panelTop = topPos + imageHeight - R10_PANEL_H - PAD;
        int inputX = leftPos + PAD;
        int inputY = panelTop + 4 + 14 + 2 + 14 + 2 + 14 + 2; // 计数+提示+标题 三行下面

        // EditBox 输入玩家名
        playerNameInput = new EditBox(this.font,
                inputX, inputY, 100, 16,
                Component.translatable("cobblemon_ts.chunk_barrier.player_placeholder"));
        playerNameInput.setMaxLength(32);
        playerNameInput.setVisible(!ro);
        this.addRenderableWidget(playerNameInput);

        // 添加玩家按钮
        addRenderableWidget(Button.builder(
                Component.translatable("cobblemon_ts.chunk_barrier.player_add"),
                btn -> {
                    if (ro) return;
                    String name = playerNameInput.getValue().trim().toLowerCase();
                    if (name.isEmpty()) return;
                    currentNameMap().putIfAbsent(name, true);
                    playerNameInput.setValue("");
                    syncToServer();
                }
        ).bounds(inputX + 105, inputY - 1, 39, 18).build());

        // [线性-SCALE] GUI缩放适配：当image尺寸超出屏幕时整体PoseStack缩小
        requestGlobalCounts();
        if (imageWidth > this.width || imageHeight > this.height) {
            cts_origW = this.width;
            cts_origH = this.height;
            cts_scale = Math.min((float) cts_origW / imageWidth, (float) cts_origH / imageHeight);
            cts_needScale = true;
            this.width = imageWidth;
            this.height = imageHeight;
            this.leftPos = 0;
            this.topPos = 0;
            layoutMap();
        }
    }

    // [线性-全局计数] 向服务端发一次请求，服务端返回后自动更新全局数
    private void requestGlobalCounts() {
        globalCounts = null;
        com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket.resetForNewRequest();
        UUID owner = this.menu.getOwnerId();
        if (owner != null) {
            PacketDistributor.sendToServer(new com.cobblemon_ts.mod.network.RequestChunkCountPacket(owner));
        }
    }

    // [线性-全局计数] 关闭 GUI 时清理 volatile 引用
    @Override
    public void removed() {
        super.removed();
        com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket.clearCache();
    }

    // [线性-R10] 只读判断：非owner打开 → 红色边框只读
    private boolean isReadOnly() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        UUID playerId = mc.player.getUUID();
        UUID ownerId = this.menu.getOwnerId();
        if (ownerId == null) return false;
        return !playerId.equals(ownerId);
    }

    private void layoutMap() {
        this.tileSize = Math.max(16, RIGHT_SIZE / gridSize);
        this.mapSize = tileSize * gridSize;
        this.mapLeft = leftPos + LEFT_W + PAD * 2;
        this.mapTop = topPos + 32;
    }

    private int chunkAt(int mouseX, int mouseY) {
        int relX = mouseX - mapLeft;
        int relY = mouseY - mapTop;
        if (relX < 0 || relY < 0 || relX >= mapSize || relY >= mapSize) return -1;
        return (relY / tileSize) * gridSize + (relX / tileSize);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // [线性-SCALE] 用真实屏幕尺寸画背景
        if (cts_needScale) {
            this.width = cts_origW;
            this.height = cts_origH;
        }
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (cts_needScale) {
            this.width = imageWidth;
            this.height = imageHeight;
            graphics.pose().pushPose();
            float scaledW = imageWidth * cts_scale;
            float scaledH = imageHeight * cts_scale;
            float offsetX = (cts_origW - scaledW) / 2.0f;
            float offsetY = (cts_origH - scaledH) / 2.0f;
            graphics.pose().translate(offsetX / cts_scale, offsetY / cts_scale, 0);
            graphics.pose().scale(cts_scale, cts_scale, 1.0f);
            mouseX /= cts_scale;
            mouseY /= cts_scale;
        }

        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1a1a1a);
        graphics.drawString(font, Component.translatable("container.cobblemon_ts.chunk_barrier").getString(), leftPos + PAD, topPos + PAD, 0xFFFFFF);

        // [线性-R10] 只读红色边框 + 提示
        if (isReadOnly()) {
            int thick = 3;
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + thick, 0xFFFF3333);
            graphics.fill(leftPos, topPos + imageHeight - thick, leftPos + imageWidth, topPos + imageHeight, 0xFFFF3333);
            graphics.fill(leftPos, topPos, leftPos + thick, topPos + imageHeight, 0xFFFF3333);
            graphics.fill(leftPos + imageWidth - thick, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFFF3333);
            graphics.drawString(font,
                    Component.translatable("cobblemon_ts.chunk_barrier.readonly").getString(),
                    leftPos + imageWidth - font.width(Component.translatable("cobblemon_ts.chunk_barrier.readonly").getString()) - PAD,
                    topPos + PAD, 0xFFFF6666);
        }

        for (int i = 0; i < 5; i++) {
            var btn = (Button) this.renderables.get(i);
            boolean selected = activeBits.contains(BITS[i]);
            int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
            String label = Component.translatable(BTN_KEYS[i]).getString();
            String state = selected
                    ? Component.translatable("cobblemon_ts.chunk_barrier.on").getString()
                    : Component.translatable("cobblemon_ts.chunk_barrier.off").getString();

            if (selected) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF000000 | BTN_COLORS[i]);
                graphics.drawString(font, label + state, bx + 8, by + (bh - font.lineHeight) / 2, 0xFF000000 | BTN_COLORS[i]);
            } else {
                int darkR = ((BTN_COLORS[i] >> 16) & 0xFF) / 2;
                int darkG = ((BTN_COLORS[i] >> 8) & 0xFF) / 2;
                int darkB = (BTN_COLORS[i] & 0xFF) / 2;
                int darkCol = 0xFF000000 | (darkR << 16) | (darkG << 8) | darkB;
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, darkCol);
                graphics.drawString(font, label + state, bx + 8, by + (bh - font.lineHeight) / 2, darkCol);
            }
        }

        var modeBtn = (Button) this.renderables.get(5);
        {
            int bx = modeBtn.getX(), by = modeBtn.getY(), bw = modeBtn.getWidth(), bh = modeBtn.getHeight();
            int col = addMode ? 0xFF2ECC71 : 0xFFE74C3C;
            int bg = addMode ? 0xFF1a2a1a : 0xFF2a1a1a;
            graphics.fill(bx, by, bx + bw, by + bh, bg);
            graphics.fill(bx, by, bx + 3, by + bh, col);
            String modeLabel = addMode
                    ? Component.translatable("cobblemon_ts.chunk_barrier.mode_add").getString()
                    : Component.translatable("cobblemon_ts.chunk_barrier.mode_sub").getString();
            graphics.drawString(font, modeLabel, bx + 8, by + (bh - font.lineHeight) / 2, col);
        }

        // R10 开关1：幸运方块标记模式（深色竖线=黑）
        {
            var r10Btn1 = (Button) this.renderables.get(6);
            boolean active = activeBits.contains(BIT_LUCKY);
            int bx = r10Btn1.getX(), by = r10Btn1.getY(), bw = r10Btn1.getWidth(), bh = r10Btn1.getHeight();
            if (active) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF444444);
                graphics.drawString(font,
                        Component.translatable("cobblemon_ts.chunk_barrier.r10_lucky_toggle").getString()
                                + " " + Component.translatable("cobblemon_ts.chunk_barrier.on").getString(),
                        bx + 8, by + (bh - font.lineHeight) / 2, 0xFF888888);
            } else {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF222222);
                graphics.drawString(font,
                        Component.translatable("cobblemon_ts.chunk_barrier.r10_lucky_toggle").getString()
                                + " " + Component.translatable("cobblemon_ts.chunk_barrier.off").getString(),
                        bx + 8, by + (bh - font.lineHeight) / 2, 0xFF555555);
            }
        }

        // R10 开关2：玩家标记模式（白色竖线=白）
        {
            var r10Btn2 = (Button) this.renderables.get(7);
            boolean active = activeBits.contains(BIT_PLAYER);
            int bx = r10Btn2.getX(), by = r10Btn2.getY(), bw = r10Btn2.getWidth(), bh = r10Btn2.getHeight();
            if (active) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFFFFFFFF);
                graphics.drawString(font,
                        Component.translatable("cobblemon_ts.chunk_barrier.r10_player_toggle").getString()
                                + " " + Component.translatable("cobblemon_ts.chunk_barrier.on").getString(),
                        bx + 8, by + (bh - font.lineHeight) / 2, 0xFFFFFFFF);
            } else {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF888888);
                graphics.drawString(font,
                        Component.translatable("cobblemon_ts.chunk_barrier.r10_player_toggle").getString()
                                + " " + Component.translatable("cobblemon_ts.chunk_barrier.off").getString(),
                        bx + 8, by + (bh - font.lineHeight) / 2, 0xFFAAAAAA);
            }
        }

        int half = gridSize / 2;
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int cx = centerChunkX + (gx - half);
                int cz = centerChunkZ + (gz - half);
                int flags = this.menu.getConfig().getFlags(cx, cz);

                int x0 = mapLeft + gx * tileSize;
                int y0 = mapTop + gz * tileSize;
                int idx = gz * gridSize + gx;
                long key = ChunkBarrierConfig.chunkKey(cx, cz);

                int bgColor = terrainCache.getOrDefault(key, COLOR_UNLOADED);
                graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, bgColor);

                if (chunkAt(mouseX, mouseY) == idx) {
                    graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, COLOR_HOVER);
                }

                // [线性-超限] 满限时给可以加禁止位的chunk画半透明灰色遮罩（视觉禁用）
                if (isBanLimitFull() && (flags & BAN_BITS) == 0 && !activeBits.isEmpty()) {
                    graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, 0x99000000);
                }

                // [线性-超限] Force Load 超限遮罩
                if (isForceLimitFull() && (flags & FORCE_BITS) == 0 && activeBits.contains(BIT_LOAD)) {
                    graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, 0x99000000);
                }

                drawChunkMarks(graphics, x0, y0, gx == half && gz == half, flags);
            }
        }

        // [线性-hover] 鼠标 hover 区块时显示 tooltip
        int hoverIdx = chunkAt(mouseX, mouseY);
        if (hoverIdx >= 0) {
            int hgx = hoverIdx % gridSize;
            int hgz = hoverIdx / gridSize;
            int hcx = centerChunkX - gridSize / 2 + hgx;
            int hcz = centerChunkZ - gridSize / 2 + hgz;
            long hkey = ChunkBarrierConfig.chunkKey(hcx, hcz);
            int hflags = this.menu.getConfig().chunkRules.getOrDefault(hkey, (byte)0) & 0xFF;

            java.util.List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.chunk", hcx, hcz));

            if (hflags != 0) {
                String ownerName = this.menu.getOwnerName();
                if (ownerName != null && !ownerName.isEmpty()) {
                    tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.owner_line", ownerName));
                }

                java.util.List<String> wlList = new java.util.ArrayList<>();
                java.util.List<String> blList = new java.util.ArrayList<>();
                for (var e : this.menu.getConfig().playerWhitelist.entrySet()) {
                    if (e.getValue()) wlList.add(e.getKey());
                    else blList.add(e.getKey());
                }
                if (!wlList.isEmpty()) {
                    tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.whitelist",
                            String.join(", ", wlList)));
                }
                if (!blList.isEmpty()) {
                    tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.blacklist",
                            String.join(", ", blList)));
                }
            }

            if ((hflags & BAN_BITS) != 0) {
                tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.ban"));
            }
            if ((hflags & FORCE_BITS) != 0) {
                tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.force_load"));
            }
            if ((hflags & BIT_LUCKY) != 0) {
                tip.add(Component.translatable("cobblemon_ts.chunk_barrier.tip.lucky_ban"));
            }

            if (!tip.isEmpty()) {
                graphics.renderTooltip(font, tip, java.util.Optional.empty(), mouseX + 12, mouseY - 12);
            }
        }

        String dragMode = addMode
                ? Component.translatable("cobblemon_ts.chunk_barrier.drag_add").getString()
                : Component.translatable("cobblemon_ts.chunk_barrier.drag_sub").getString();
        // [线性-R10] hint 显示在地图正下方
        int hintY = topPos + 32 + RIGHT_SIZE + PAD - 4;
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.hint",
                        centerChunkX, centerChunkZ, gridSize, gridSize, dragMode).getString(),
                leftPos + PAD, hintY, 0xFFBBBBBB);

        // [线性-R10] 底部面板：全区域布局
        int panelTop = topPos + imageHeight - R10_PANEL_H - PAD;
        graphics.fill(leftPos, panelTop, leftPos + imageWidth, panelTop + R10_PANEL_H, 0xFF121212);
        ChunkBarrierConfig cfg = this.menu.getConfig();

        // ① 计数行（左侧区域，panelTop + 4）
        int forceCnt = curForceChunkCount();
        int banCnt = curBanChunkCount();
        int forceLimit = CharmConfig.getCbForceloadChunkLimit();
        int banLimit = CharmConfig.getCbBanChunkLimit();
        String forceStr = Component.translatable("cobblemon_ts.chunk_barrier.count.force", forceCnt, forceLimit).getString();
        String banStr = Component.translatable("cobblemon_ts.chunk_barrier.count.ban", banCnt, banLimit).getString();
        int forceColor = forceCnt >= forceLimit ? 0xFFFF6666 : 0xFFAAAAAA;
        int banColor = banCnt >= banLimit ? 0xFFFF6666 : 0xFFAAAAAA;
        graphics.drawString(font, forceStr, leftPos + PAD, panelTop + 4, forceColor);
        graphics.drawString(font, banStr, leftPos + PAD + 90, panelTop + 4, banColor);

        // ② 提示行（panelTop + 4 + 14 + 2 = panelTop + 20）
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.empty_list_hint_line1").getString(),
                leftPos + PAD, panelTop + 20, 0xFF888888);
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.empty_list_hint_line2").getString(),
                leftPos + PAD, panelTop + 32, 0xFF888888);

        String title = editingTarget == TARGET_LUCKY
                ? Component.translatable("cobblemon_ts.chunk_barrier.player_title_lucky").getString()
                : Component.translatable("cobblemon_ts.chunk_barrier.player_title").getString();
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.arrow").getString() + title,
                leftPos + PAD, panelTop + 44, editingTarget == TARGET_LUCKY ? 0xFFFFAA22 : 0xFFFFDD66);

        // ④ EditBox + Button（renderables 自动渲染，位置 init 里已算好）
        // 给输入框区域画独立背景框，让 EditBox 在深色面板上清晰可见
        int inputRowY = panelTop + 50;
        graphics.fill(leftPos + PAD, inputRowY, leftPos + PAD + 144, inputRowY + 20, 0xFF1a1a1a);
        graphics.fill(leftPos + PAD, inputRowY, leftPos + PAD + 144, inputRowY + 1, 0xFF3a3a3a);
        graphics.fill(leftPos + PAD, inputRowY + 19, leftPos + PAD + 144, inputRowY + 20, 0xFF3a3a3a);

        // ⑤ 操作说明（panelTop + 50 + 20 + 4 = panelTop + 74）
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.hint_ops_line1").getString(),
                leftPos + PAD, panelTop + 74, 0xFF666666);
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.hint_ops_line2").getString(),
                leftPos + PAD, panelTop + 86, 0xFF666666);

        int listBoxX = leftPos + LEFT_W + PAD;
        int listBoxY = panelTop + 4;
        int listBoxW = imageWidth - LEFT_W - PAD * 3;
        int listBoxH = R10_PANEL_H - 8;

        graphics.fill(listBoxX, listBoxY, listBoxX + listBoxW, listBoxY + listBoxH, 0xFF1e1e1e);
        graphics.fill(listBoxX, listBoxY, listBoxX + listBoxW, listBoxY + 1, 0xFF3a3a3a);
        graphics.fill(listBoxX, listBoxY + listBoxH - 1, listBoxX + listBoxW, listBoxY + listBoxH, 0xFF3a3a3a);
        graphics.fill(listBoxX, listBoxY, listBoxX + 1, listBoxY + listBoxH, 0xFF3a3a3a);
        graphics.fill(listBoxX + listBoxW - 1, listBoxY, listBoxX + listBoxW, listBoxY + listBoxH, 0xFF3a3a3a);

        int splitY = listBoxY + listBoxH / 2;
        graphics.fill(listBoxX + 1, splitY - 1, listBoxX + listBoxW - 1, splitY, 0xFF3a3a3a);

        if (editingTarget == TARGET_PLAYER) {
            graphics.fill(listBoxX, listBoxY, listBoxX + listBoxW, listBoxY + 1, 0xFFFFFFFF);
            graphics.fill(listBoxX + listBoxW - 1, listBoxY, listBoxX + listBoxW, splitY, 0xFFFFFFFF);
        } else {
            graphics.fill(listBoxX + listBoxW - 1, splitY, listBoxX + listBoxW, listBoxY + listBoxH, 0xFFFFAA22);
            graphics.fill(listBoxX, listBoxY + listBoxH - 1, listBoxX + listBoxW, listBoxY + listBoxH, 0xFFFFAA22);
        }

        int rowH = 14;
        int colW = 100;

        drawNameListSection(graphics, listBoxX, listBoxY, listBoxW, splitY - listBoxY,
                Component.translatable("cobblemon_ts.chunk_barrier.list_title.player").getString(),
                playerMap(), playerNameColor(), rowH, colW);
        drawNameListSection(graphics, listBoxX, splitY, listBoxW, listBoxY + listBoxH - splitY,
                Component.translatable("cobblemon_ts.chunk_barrier.list_title.lucky").getString(),
                luckyMap(), luckyNameColor(), rowH, colW);

        // [线性-渲染widget] 手动渲染 EditBox 和 添加按钮
        if (playerNameInput != null) {
            playerNameInput.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.renderables.size() > 9) {
            ((net.minecraft.client.gui.components.Button) this.renderables.get(9))
                    .render(graphics, mouseX, mouseY, partialTick);
        }

        // [线性-SCALE] 还原 PoseStack
        if (cts_needScale) {
            graphics.pose().popPose();
        }
    }

    private void drawChunkMarks(GuiGraphics g, int x0, int y0, boolean isCenter, int flags) {
        int size = tileSize;
        int x1 = x0 + size;
        int y1 = y0 + size;

        g.fill(x0, y0, x1, y0 + 1, COLOR_GRID);
        g.fill(x0, y0, x0 + 1, y1, COLOR_GRID);

        if (flags == 0 && !isCenter) return;

        // tile 切 2行 × 5列
        int cellW = size / 5;
        int cellH = size / 2;
        int innerPad = Math.max(1, cellW / 4);

        // col0: 按钮1 BIT_POKEMON (row0 正方) + 按钮2 BIT_NPC (row1 正方)
        if ((flags & BIT_POKEMON) != 0) {
            g.fill(x0 + innerPad, y0 + innerPad, x0 + cellW - innerPad, y0 + cellH - innerPad, 0xFF000000 | COLOR_POKEMON);
        }
        if ((flags & BIT_NPC) != 0) {
            g.fill(x0 + innerPad, y0 + cellH + innerPad, x0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_NPC);
        }

        // col1: 幸运方块禁止 - 只有标记了 BIT_LUCKY 的区块才画实心黑
        if ((flags & BIT_LUCKY) != 0) {
            int cx0 = x0 + cellW;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad,
                    0xFF000000 | COLOR_LUCKY);
        }

        // col2: 按钮5 BIT_LOAD (跨两行长条)
        if ((flags & BIT_LOAD) != 0) {
            int cx0 = x0 + cellW * 2;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_FORCELOAD);
        }

        // col3: 玩家禁止 - 只有标记了 BIT_PLAYER 的区块才画实心白
        if ((flags & BIT_PLAYER) != 0) {
            int cx0 = x0 + cellW * 3;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad,
                    0xFF000000 | COLOR_PLAYER);
        }

        // col4: 按钮3 BIT_HOSTILE (row0 正方) + 按钮4 BIT_PASSIVE (row1 正方)
        if ((flags & BIT_HOSTILE) != 0) {
            int cx0 = x0 + cellW * 4;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + cellH - innerPad, 0xFF000000 | COLOR_HOSTILE);
        }
        if ((flags & BIT_PASSIVE) != 0) {
            int cx0 = x0 + cellW * 4;
            g.fill(cx0 + innerPad, y0 + cellH + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_PASSIVE);
        }

        // 中心格子十字准线
        if (isCenter) {
            int col = (flags & BIT_LOAD) != 0 ? (0xFF000000 | COLOR_FORCELOAD) : 0xFFFFFFFF;
            int thick = Math.max(2, size / 14);
            int len = size / 2;
            int midX = x0 + size / 2;
            int midY = y0 + size / 2;
            g.fill(midX - len, midY - thick, midX + len, midY + thick + 1, col);
            g.fill(midX - thick, midY - len, midX + thick + 1, midY + len, col);
        }
    }

    private static final int COLOR_CENTER = 0xFFFFFFFF;

    private void drawThreeStripes(GuiGraphics g, int x0, int y0, int w, int h, int color, int thick) {
        color |= 0xFF000000;
        int step = thick * 3;
        for (int i = 0; i < 3; i++) {
            int offset = i * step;
            for (int dy = 0; dy < h; dy++) {
                int left = x0 + (dy - offset);
                int right = left + thick;
                if (right < x0 || left > x0 + w) continue;
                left = Math.max(x0, left);
                right = Math.min(x0 + w, right);
                g.fill(left, y0 + dy, right, y0 + dy + 1, color);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cts_needScale) { mouseX /= cts_scale; mouseY /= cts_scale; }
        if (handlePlayerListClick((int) mouseX, (int) mouseY, button)) return true;

        if (button == 1) {
            int clearedBit = findLeftButtonBitAt((int) mouseX, (int) mouseY);
            if (clearedBit > 0) {
                if (!isReadOnly()) clearBitInRange(clearedBit);
                return true;
            }
        }

        if (isReadOnly()) return super.mouseClicked(mouseX, mouseY, button);

        int idx = chunkAt((int) mouseX, (int) mouseY);
        if (idx >= 0) {
            // [线性-超限] 如果满限且这个chunk可以加禁止位 → 直接拦截，不进入点击逻辑
            int gx = idx % gridSize;
            int gz = idx / gridSize;
            int cx = centerChunkX + (gx - gridSize / 2);
            int cz = centerChunkZ + (gz - gridSize / 2);
            if (isBanLimitFull() && isChunkAddingBan(cx, cz, button)) {
                if (!banLimitNotified) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable("cobblemon_ts.chunk_barrier.limit.ban",
                                    curBanChunkCount(), CharmConfig.getCbBanChunkLimit()));
                    banLimitNotified = true;
                }
                return true;
            }
            // [线性-超限] Force Load 超限拦截 + 一次性提示
            if (isForceLimitFull() && isChunkAddingForceLoad(cx, cz, button)) {
                if (!forceLimitNotified) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable("cobblemon_ts.chunk_barrier.limit.forceload",
                                    curForceChunkCount(), CharmConfig.getCbForceloadChunkLimit()));
                    forceLimitNotified = true;
                }
                return true;
            }
            dragging = true;
            dragButton = button;
            dragTouched.clear();
            applyChunkClick(idx, button, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePlayerListClick(int mx, int my, int button) {
        int panelTop = topPos + imageHeight - R10_PANEL_H - PAD;
        int listBoxX = leftPos + LEFT_W + PAD;
        int listBoxY = panelTop + 4;
        int listBoxW = imageWidth - LEFT_W - PAD * 3;
        int listBoxH = R10_PANEL_H - 8;
        int rowH = 14;
        int colW = 100;
        int splitY = listBoxY + listBoxH / 2;

        if (mx < listBoxX || mx > listBoxX + listBoxW) return false;
        if (my < listBoxY || my > listBoxY + listBoxH) return false;

        boolean isTop = my < splitY;
        editingTarget = isTop ? TARGET_PLAYER : TARGET_LUCKY;

        int sectionY = isTop ? listBoxY : splitY;
        int sectionH = isTop ? splitY - listBoxY : listBoxY + listBoxH - splitY;
        var map = isTop ? playerMap() : luckyMap();

        int entryStartY = sectionY + 2 + rowH;
        int areaH = sectionH - rowH - 2;
        if (my < entryStartY) return true;

        int maxRowsPerCol = Math.max(1, areaH / rowH);
        int entryCount = map.size();
        int cols = Math.max(1, (int) Math.ceil((double) entryCount / maxRowsPerCol));
        cols = Math.min(cols, 3);
        maxRowsPerCol = Math.max(1, Math.min(maxRowsPerCol, (int) Math.ceil((double) entryCount / cols)));

        int localX = mx - listBoxX - 2;
        int localY = my - entryStartY;
        int col = localX / colW;
        int row = localY / rowH;
        if (col >= cols || row >= maxRowsPerCol) return true;
        int idx = row * cols + col;
        if (idx >= entryCount) return true;

        int i = 0;
        for (var e : map.entrySet()) {
            if (i == idx) {
                String name = e.getKey();
                if (button == 0) {
                    map.put(name, !e.getValue());
                } else if (button == 1) {
                    map.remove(name);
                    selectedPlayerEntry = -1;
                }
                syncToServer();
                return true;
            }
            i++;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (cts_needScale) { mouseX /= cts_scale; mouseY /= cts_scale; dragX /= cts_scale; dragY /= cts_scale; }
        // [线性-R10] 只读模式禁用拖拽
        if (isReadOnly()) return false;
        if (dragging) {
            int idx = chunkAt((int) mouseX, (int) mouseY);
            if (idx >= 0 && !dragTouched.contains(idx)) {
                dragTouched.add(idx);
                // [线性-超限] 拖拽时也拦截
                int gx = idx % gridSize;
                int gz = idx / gridSize;
                int cx = centerChunkX + (gx - gridSize / 2);
                int cz = centerChunkZ + (gz - gridSize / 2);
                if (isBanLimitFull() && isChunkAddingBan(cx, cz, dragButton)) {
                    return true;
                }
                // [线性-超限] Force Load 超限静默拦截（首次点击已提示过）
                if (isForceLimitFull() && isChunkAddingForceLoad(cx, cz, dragButton)) {
                    return true;
                }
                applyChunkClick(idx, dragButton, true);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == dragButton) {
            dragging = false;
            dragTouched.clear();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int findLeftButtonBitAt(int mx, int my) {
        int[] bitsByIndex = {BIT_POKEMON, BIT_NPC, BIT_HOSTILE, BIT_PASSIVE, BIT_LOAD, 0, BIT_LUCKY, BIT_PLAYER};
        for (int i = 0; i < bitsByIndex.length && i < this.renderables.size(); i++) {
            if (bitsByIndex[i] == 0) continue;
            var w = this.renderables.get(i);
            if (w instanceof Button) {
                Button btn = (Button) w;
                if (mx >= btn.getX() && mx <= btn.getX() + btn.getWidth()
                        && my >= btn.getY() && my <= btn.getY() + btn.getHeight()) {
                    return bitsByIndex[i];
                }
            }
        }
        return 0;
    }

    private void clearBitInRange(int bit) {
        ChunkBarrierConfig cfg = this.menu.getConfig();
        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                int cx = centerChunkX + (gx - gridSize / 2);
                int cz = centerChunkZ + (gz - gridSize / 2);
                int cur = cfg.getFlags(cx, cz);
                if ((cur & bit) != 0) {
                    cfg.setFlags(cx, cz, cur & ~bit);
                }
            }
        }
        syncToServer();
    }

    private void applyChunkClick(int idx, int button, boolean isDrag) {
        int gx = idx % gridSize;
        int gz = idx / gridSize;
        int cx = centerChunkX + (gx - gridSize / 2);
        int cz = centerChunkZ + (gz - gridSize / 2);

        if (button == 0) {
            if (!activeBits.isEmpty()) {
                int cur = this.menu.getConfig().getFlags(cx, cz);
                int next = cur;
                if (isDrag) {
                    if (addMode) {
                        for (int bit : activeBits) next |= bit;
                    } else {
                        for (int bit : activeBits) next &= ~bit;
                    }
                } else {
                    boolean hasAll = true;
                    for (int bit : activeBits) {
                        if ((cur & bit) == 0) { hasAll = false; break; }
                    }
                    if (hasAll) {
                        for (int bit : activeBits) next &= ~bit;
                    } else {
                        for (int bit : activeBits) next |= bit;
                    }
                }

                this.menu.getConfig().setFlags(cx, cz, next);
            }
        } else if (button == 1) {
            this.menu.getConfig().setFlags(cx, cz, 0);
        }
        syncToServer();
    }

    private void syncToServer() {
        ChunkBarrierConfig cfg = this.menu.getConfig();
        PacketDistributor.sendToServer(new SyncConfigPayload(
                this.menu.getBlockPos(),
                new HashMap<>(cfg.chunkRules),
                new HashMap<>(cfg.playerWhitelist),
                new HashMap<>(cfg.luckyPlayerWhitelist),
                cfg.banLuckyBlock
        ));
        // [线性-全局计数] 修改后立刻刷新全局计数
        requestGlobalCounts();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (cts_needScale) { mouseX /= cts_scale; mouseY /= cts_scale; }
        // [线性-R10] 只读模式禁用滚轮缩放
        if (isReadOnly()) return false;
        int newSize = gridSize + (vertical > 0 ? 2 : -2);
        if (newSize < 3) newSize = 3;
        if (newSize > maxRange) newSize = maxRange;
        if (newSize % 2 == 0) newSize++;
        if (newSize != gridSize) {
            gridSize = newSize;
            layoutMap();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) sampleAllTerrain(mc.level);
        }
        return true;
    }

    // [线性-回车添加] 输入框聚焦时：回车=添加玩家；功能键转发 EditBox；其他键直接吃掉不让 super 处理（防止 E 关 GUI）
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (playerNameInput != null && playerNameInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                if (!isReadOnly()) {
                    String name = playerNameInput.getValue().trim().toLowerCase();
                    if (!name.isEmpty()) {
                        currentNameMap().putIfAbsent(name, true);
                        playerNameInput.setValue("");
                        syncToServer();
                    }
                }
                return true;
            }
            if (keyCode == 256 || scanCode == 1) {
                playerNameInput.setFocused(false);
                this.onClose();
                return true;
            }
            if (playerNameInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            return true;
        }
        if (keyCode == 256 || scanCode == 1) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // [线性-字符输入] EditBox 普通字符靠 charTyped
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (playerNameInput != null && playerNameInput.isFocused()) {
            if (playerNameInput.charTyped(codePoint, modifiers)) return true;
            return true; // EditBox 不消费的字符也吃掉
        }
        return super.charTyped(codePoint, modifiers);
    }

    // [线性-全局计数] 统计玩家全服禁止区块数
    private int curBanChunkCount() {
        globalCounts = com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket.pollLatest();
        if (globalCounts != null) return globalCounts[0];
        // 还没收到服务端响应，临时用当前 BE 本地数兜底
        int cnt = 0;
        for (var e : this.menu.getConfig().chunkRules.entrySet()) {
            int f = e.getValue() & 0xFF;
            if ((f & BAN_BITS) != 0) cnt++;
        }
        return cnt;
    }

    // [线性-超限] 当前是否已满禁止上限
    private boolean isBanLimitFull() {
        return curBanChunkCount() >= CharmConfig.getCbBanChunkLimit();
    }

    // [线性-超限] 判断某个 chunk 点击是否会加禁止位（左键+activeBits非空+之前没有禁止位）
    private boolean isChunkAddingBan(int cx, int cz, int button) {
        if (button != 0) return false;
        if (activeBits.isEmpty()) return false;
        int cur = this.menu.getConfig().getFlags(cx, cz);
        return (cur & BAN_BITS) == 0;
    }

    // [线性-全局计数] 统计玩家全服 Force Load 区块数
    private int curForceChunkCount() {
        globalCounts = com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket.pollLatest();
        if (globalCounts != null) return globalCounts[1];
        int cnt = 0;
        for (var e : this.menu.getConfig().chunkRules.entrySet()) {
            int f = e.getValue() & 0xFF;
            if ((f & FORCE_BITS) != 0) cnt++;
        }
        return cnt;
    }

    // [线性-超限] 当前 Force Load 是否已满上限
    private boolean isForceLimitFull() {
        return curForceChunkCount() >= CharmConfig.getCbForceloadChunkLimit();
    }

    // [线性-超限] 判断某个 chunk 点击是否会加 Force Load 位
    private boolean isChunkAddingForceLoad(int cx, int cz, int button) {
        if (button != 0) return false;
        int cur = this.menu.getConfig().getFlags(cx, cz);
        // 之前没有 force load 位，且本次 activeBits 里包含 BIT_LOAD
        return (cur & FORCE_BITS) == 0 && activeBits.contains(BIT_LOAD);
    }
}