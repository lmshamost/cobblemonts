package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import com.cobblemon_ts.mod.network.PacketDistributor;
import com.cobblemon_ts.mod.network.RequestChunkCountPacket;
import com.cobblemon_ts.mod.network.SyncChunkCountS2CPacket;
import com.cobblemon_ts.mod.network.SyncConfigPayload;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChunkBarrierScreen extends HandledScreen<ChunkBarrierContainer> {

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

    // [线性-BTN] 保存按钮引用，替代 NeoForge 的 renderables 索引
    private final ButtonWidget[] toggleButtons = new ButtonWidget[5];
    private ButtonWidget modeButton;
    private ButtonWidget luckyToggleButton;
    private ButtonWidget playerToggleButton;
    private ButtonWidget addPlayerButton;

    // [线性-R10] 玩家名单输入框
    private TextFieldWidget playerNameInput;
    // [线性-R10] 当前选中的玩家名单条目（右键删除用）
    private int selectedPlayerEntry = -1;
    private static final int TARGET_PLAYER = 0;
    private static final int TARGET_LUCKY = 1;
    private int editingTarget = TARGET_PLAYER;

    private static final int R10_PANEL_H = 132;

    private int cts_origW, cts_origH;
    private float cts_scale = 1.0f;
    private float cts_offsetX = 0, cts_offsetY = 0;
    private boolean cts_needScale = false;

    public ChunkBarrierScreen(ChunkBarrierContainer container, PlayerInventory inv, Text title) {
        super(container, inv, title);
        this.backgroundWidth = LEFT_W + RIGHT_SIZE + PAD * 3;
        this.backgroundHeight = 32 + RIGHT_SIZE + PAD * 2 + R10_PANEL_H;
    }

    private Map<String, Boolean> currentNameMap() {
        return editingTarget == TARGET_LUCKY
                ? this.handler.getConfig().luckyPlayerWhitelist
                : this.handler.getConfig().playerWhitelist;
    }
    private Map<String, Boolean> playerMap() {
        return this.handler.getConfig().playerWhitelist;
    }
    private Map<String, Boolean> luckyMap() {
        return this.handler.getConfig().luckyPlayerWhitelist;
    }
    private int playerNameColor() { return 0xFFFFFFFF; }
    private int luckyNameColor() { return 0xFFFFAA22; }

    private void drawNameListSection(DrawContext g, int x, int y, int w, int h,
                                     String title, Map<String, Boolean> map, int nameColor,
                                     int rowH, int colW) {
        TextRenderer font = this.textRenderer;
        int titleY = y + 2;
        g.drawText(font, title, x + 2, titleY, 0xFFAAAAAA, false);

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
                    ? Text.translatable("cobblemon_ts.chunk_barrier.tag.white").getString()
                    : Text.translatable("cobblemon_ts.chunk_barrier.tag.black").getString();
            String namePart = " " + e.getKey();

            int ex = x + 2 + col * colW;
            int ey = entryStartY + row * rowH;

            g.drawText(font, tag, ex, ey, tagColor, false);
            int tagW = font.getWidth(tag);
            g.drawText(font, namePart, ex + tagW, ey, nameColor, false);
            idx++;
        }
        if (map.isEmpty()) {
            g.drawText(font, "(空)", x + 4, entryStartY, 0xFF555555, false);
        }
    }

    private int sampleChunkColor(World level, int cx, int cz) {
        WorldChunk chunk = level.isChunkLoaded(cx, cz) ? level.getChunk(cx, cz) : null;
        if (chunk == null) return COLOR_UNLOADED;
        int minY = level.getBottomY();
        int maxY = level.getHeight();
        int totalR = 0, totalG = 0, totalB = 0, count = 0;
        for (int lx = 0; lx < 16; lx += 4) {
            for (int lz = 0; lz < 16; lz += 4) {
                int wx = (cx << 4) + lx;
                int wz = (cz << 4) + lz;
                for (int y = maxY - 1; y >= minY; y--) {
                    var state = chunk.getBlockState(new BlockPos(wx, y, wz));
                    if (state.isAir()) continue;
                    MapColor mc = state.getMapColor(level, new BlockPos(wx, y, wz));
                    if (mc != MapColor.CLEAR) {
                        totalR += (mc.color >> 16) & 0xFF;
                        totalG += (mc.color >> 8) & 0xFF;
                        totalB += mc.color & 0xFF;
                        count++;
                        break;
                    }
                }
            }
        }
        if (count == 0) return COLOR_UNLOADED;
        return 0xFF000000 | ((totalR / count) << 16) | ((totalG / count) << 8) | (totalB / count);
    }

    private void sampleAllTerrain(World level) {
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
        // [线性-SCALE] 在 super.init() 之前拦截
        int realW = this.width;
        int realH = this.height;
        if (backgroundWidth > realW || backgroundHeight > realH) {
            cts_origW = realW;
            cts_origH = realH;
            cts_scale = Math.min((float) realW / backgroundWidth, (float) realH / backgroundHeight);
            cts_offsetX = (realW - backgroundWidth * cts_scale) / 2.0f;
            cts_offsetY = (realH - backgroundHeight * cts_scale) / 2.0f;
            cts_needScale = true;
            this.width = backgroundWidth;
            this.height = backgroundHeight;
        }

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

        int baseX = this.x + PAD;
        boolean ro = isReadOnly();
        ChunkBarrierConfig cfg = this.handler.getConfig();

        if (cfg.banLuckyBlock) this.activeBits.add(BIT_LUCKY);

        int baseY = this.y + 32;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            toggleButtons[i] = addDrawableChild(ButtonWidget.builder(
                    Text.literal(""),
                    btn -> {
                        int bit = BITS[idx];
                        if (activeBits.contains(bit)) activeBits.remove(bit);
                        else activeBits.add(bit);
                    }
            ).dimensions(baseX, baseY + i * (BTN_H + 4), LEFT_W - PAD * 2, BTN_H).build());
        }

        // addMode 切换
        int addModeY = baseY + 5 * (BTN_H + 4);
        modeButton = addDrawableChild(ButtonWidget.builder(
                Text.literal(""),
                btn -> addMode = !addMode
        ).dimensions(baseX, addModeY, LEFT_W - PAD * 2, BTN_H).build());

        // R10 开关
        int r10Y1 = addModeY + BTN_H + 4;
        int r10Y2 = r10Y1 + BTN_H + 4;

        luckyToggleButton = addDrawableChild(ButtonWidget.builder(
                Text.literal(""),
                btn -> {
                    if (ro) return;
                    boolean newState = !activeBits.contains(BIT_LUCKY);
                    if (newState) activeBits.add(BIT_LUCKY);
                    else activeBits.remove(BIT_LUCKY);
                    this.handler.getConfig().banLuckyBlock = newState;
                    syncToServer();
                }
        ).dimensions(baseX, r10Y1, LEFT_W - PAD * 2, BTN_H).build());

        playerToggleButton = addDrawableChild(ButtonWidget.builder(
                Text.literal(""),
                btn -> {
                    if (ro) return;
                    if (activeBits.contains(BIT_PLAYER)) activeBits.remove(BIT_PLAYER);
                    else activeBits.add(BIT_PLAYER);
                }
        ).dimensions(baseX, r10Y2, LEFT_W - PAD * 2, BTN_H).build());

        this.centerChunkX = this.handler.getBlockPos().getX() >> 4;
        this.centerChunkZ = this.handler.getBlockPos().getZ() >> 4;
        layoutMap();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) sampleAllTerrain(mc.world);

        // [线性-玩家名单] 输入框 + 添加按钮
        int panelTop = this.y + backgroundHeight - R10_PANEL_H - PAD;
        int inputX = this.x + PAD;
        int inputY = panelTop + 4 + 14 + 2 + 14 + 2 + 14 + 2;

        playerNameInput = new TextFieldWidget(this.textRenderer,
                inputX, inputY, 100, 16,
                Text.translatable("cobblemon_ts.chunk_barrier.player_placeholder"));
        playerNameInput.setMaxLength(32);
        playerNameInput.setVisible(!ro);
        this.addDrawableChild(playerNameInput);

        addPlayerButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.chunk_barrier.player_add"),
                btn -> {
                    if (ro) return;
                    String name = playerNameInput.getText().trim().toLowerCase();
                    if (name.isEmpty()) return;
                    currentNameMap().putIfAbsent(name, true);
                    playerNameInput.setText("");
                    syncToServer();
                }
        ).dimensions(inputX + 105, inputY - 1, 39, 18).build());

        // [线性-全局计数] 打开 GUI 时立即请求一次全服计数
        requestGlobalCounts();
    }

    // [线性-全局计数] 向服务端发一次请求
    private void requestGlobalCounts() {
        globalCounts = null;
        SyncChunkCountS2CPacket.resetForNewRequest();
        UUID owner = this.handler.getOwnerId();
        if (owner != null) {
            PacketDistributor.sendToServer(new RequestChunkCountPacket(owner));
        }
    }

    // [线性-全局计数] 关闭 GUI 时清理 volatile 引用
    @Override
    public void removed() {
        super.removed();
        SyncChunkCountS2CPacket.clearCache();
    }

    // [线性-R10] 只读判断
    private boolean isReadOnly() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return true;
        UUID playerId = mc.player.getUuid();
        UUID ownerId = this.handler.getOwnerId();
        if (ownerId == null) return false;
        return !playerId.equals(ownerId);
    }

    private void layoutMap() {
        this.tileSize = Math.max(16, RIGHT_SIZE / gridSize);
        this.mapSize = tileSize * gridSize;
        this.mapLeft = this.x + LEFT_W + PAD * 2;
        this.mapTop = this.y + 32;
    }

    private int chunkAt(int mouseX, int mouseY) {
        int relX = mouseX - mapLeft;
        int relY = mouseY - mapTop;
        if (relX < 0 || relY < 0 || relX >= mapSize || relY >= mapSize) return -1;
        return (relY / tileSize) * gridSize + (relX / tileSize);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        // [线性-SCALE] renderBackground 需要真实屏幕尺寸画全屏
        if (cts_needScale) {
            this.width = cts_origW;
            this.height = cts_origH;
        }
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (cts_needScale) {
            this.width = backgroundWidth;
            this.height = backgroundHeight;
            graphics.getMatrices().push();
            float scaledW = backgroundWidth * cts_scale;
            float scaledH = backgroundHeight * cts_scale;
            float offsetX = (cts_origW - scaledW) / 2.0f;
            float offsetY = (cts_origH - scaledH) / 2.0f;
            graphics.getMatrices().translate(offsetX, offsetY, 0.0f);
            graphics.getMatrices().scale(cts_scale, cts_scale, 1.0f);
            mouseX = (int) ((mouseX - offsetX) / cts_scale);
            mouseY = (int) ((mouseY - offsetY) / cts_scale);
        }

        graphics.fill(this.x, this.y, this.x + backgroundWidth, this.y + backgroundHeight, 0xFF1a1a1a);
        graphics.drawText(textRenderer, Text.translatable("container.cobblemon_ts.chunk_barrier").getString(), this.x + PAD, this.y + PAD, 0xFFFFFF, false);

        // [线性-R10] 只读红色边框 + 提示
        if (isReadOnly()) {
            int thick = 3;
            graphics.fill(this.x, this.y, this.x + backgroundWidth, this.y + thick, 0xFFFF3333);
            graphics.fill(this.x, this.y + backgroundHeight - thick, this.x + backgroundWidth, this.y + backgroundHeight, 0xFFFF3333);
            graphics.fill(this.x, this.y, this.x + thick, this.y + backgroundHeight, 0xFFFF3333);
            graphics.fill(this.x + backgroundWidth - thick, this.y, this.x + backgroundWidth, this.y + backgroundHeight, 0xFFFF3333);
            graphics.drawText(textRenderer,
                    Text.translatable("cobblemon_ts.chunk_barrier.readonly").getString(),
                    this.x + backgroundWidth - textRenderer.getWidth(Text.translatable("cobblemon_ts.chunk_barrier.readonly").getString()) - PAD,
                    this.y + PAD, 0xFFFF6666, false);
        }

        for (int i = 0; i < 5; i++) {
            ButtonWidget btn = toggleButtons[i];
            boolean selected = activeBits.contains(BITS[i]);
            int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
            String label = Text.translatable(BTN_KEYS[i]).getString();
            String state = selected
                    ? Text.translatable("cobblemon_ts.chunk_barrier.on").getString()
                    : Text.translatable("cobblemon_ts.chunk_barrier.off").getString();

            if (selected) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF000000 | BTN_COLORS[i]);
                graphics.drawText(textRenderer, label + state, bx + 8, by + (bh - textRenderer.fontHeight) / 2, 0xFF000000 | BTN_COLORS[i], false);
            } else {
                int darkR = ((BTN_COLORS[i] >> 16) & 0xFF) / 2;
                int darkG = ((BTN_COLORS[i] >> 8) & 0xFF) / 2;
                int darkB = (BTN_COLORS[i] & 0xFF) / 2;
                int darkCol = 0xFF000000 | (darkR << 16) | (darkG << 8) | darkB;
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, darkCol);
                graphics.drawText(textRenderer, label + state, bx + 8, by + (bh - textRenderer.fontHeight) / 2, darkCol, false);
            }
        }

        {
            ButtonWidget mBtn = modeButton;
            int bx = mBtn.getX(), by = mBtn.getY(), bw = mBtn.getWidth(), bh = mBtn.getHeight();
            int col = addMode ? 0xFF2ECC71 : 0xFFE74C3C;
            int bg = addMode ? 0xFF1a2a1a : 0xFF2a1a1a;
            graphics.fill(bx, by, bx + bw, by + bh, bg);
            graphics.fill(bx, by, bx + 3, by + bh, col);
            String modeLabel = addMode
                    ? Text.translatable("cobblemon_ts.chunk_barrier.mode_add").getString()
                    : Text.translatable("cobblemon_ts.chunk_barrier.mode_sub").getString();
            graphics.drawText(textRenderer, modeLabel, bx + 8, by + (bh - textRenderer.fontHeight) / 2, col, false);
        }

        // R10 开关1：幸运方块标记模式
        {
            ButtonWidget r10Btn1 = luckyToggleButton;
            boolean active = activeBits.contains(BIT_LUCKY);
            int bx = r10Btn1.getX(), by = r10Btn1.getY(), bw = r10Btn1.getWidth(), bh = r10Btn1.getHeight();
            if (active) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF444444);
                graphics.drawText(textRenderer,
                        Text.translatable("cobblemon_ts.chunk_barrier.r10_lucky_toggle").getString()
                                + " " + Text.translatable("cobblemon_ts.chunk_barrier.on").getString(),
                        bx + 8, by + (bh - textRenderer.fontHeight) / 2, 0xFF888888, false);
            } else {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF222222);
                graphics.drawText(textRenderer,
                        Text.translatable("cobblemon_ts.chunk_barrier.r10_lucky_toggle").getString()
                                + " " + Text.translatable("cobblemon_ts.chunk_barrier.off").getString(),
                        bx + 8, by + (bh - textRenderer.fontHeight) / 2, 0xFF555555, false);
            }
        }

        // R10 开关2：玩家标记模式
        {
            ButtonWidget r10Btn2 = playerToggleButton;
            boolean active = activeBits.contains(BIT_PLAYER);
            int bx = r10Btn2.getX(), by = r10Btn2.getY(), bw = r10Btn2.getWidth(), bh = r10Btn2.getHeight();
            if (active) {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF222222);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFFFFFFFF);
                graphics.drawText(textRenderer,
                        Text.translatable("cobblemon_ts.chunk_barrier.r10_player_toggle").getString()
                                + " " + Text.translatable("cobblemon_ts.chunk_barrier.on").getString(),
                        bx + 8, by + (bh - textRenderer.fontHeight) / 2, 0xFFFFFFFF, false);
            } else {
                graphics.fill(bx, by, bx + bw, by + bh, 0xFF1a1a1a);
                graphics.fill(bx, by, bx + 3, by + bh, 0xFF888888);
                graphics.drawText(textRenderer,
                        Text.translatable("cobblemon_ts.chunk_barrier.r10_player_toggle").getString()
                                + " " + Text.translatable("cobblemon_ts.chunk_barrier.off").getString(),
                        bx + 8, by + (bh - textRenderer.fontHeight) / 2, 0xFFAAAAAA, false);
            }
        }

        int half = gridSize / 2;
        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int cx = centerChunkX + (gx - half);
                int cz = centerChunkZ + (gz - half);
                int flags = this.handler.getConfig().getFlags(cx, cz);

                int x0 = mapLeft + gx * tileSize;
                int y0 = mapTop + gz * tileSize;
                int idx = gz * gridSize + gx;
                long key = ChunkBarrierConfig.chunkKey(cx, cz);

                int bgColor = terrainCache.getOrDefault(key, COLOR_UNLOADED);
                graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, bgColor);

                if (chunkAt(mouseX, mouseY) == idx) {
                    graphics.fill(x0, y0, x0 + tileSize, y0 + tileSize, COLOR_HOVER);
                }

                // [线性-超限] 满限时给可以加禁止位的 chunk 画遮罩
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

        // [线性-hover] tooltip
        int hoverIdx = chunkAt(mouseX, mouseY);
        if (hoverIdx >= 0) {
            int hgx = hoverIdx % gridSize;
            int hgz = hoverIdx / gridSize;
            int hcx = centerChunkX - gridSize / 2 + hgx;
            int hcz = centerChunkZ - gridSize / 2 + hgz;
            long hkey = ChunkBarrierConfig.chunkKey(hcx, hcz);
            int hflags = this.handler.getConfig().chunkRules.getOrDefault(hkey, (byte) 0) & 0xFF;

            java.util.List<Text> tip = new java.util.ArrayList<>();
            tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.chunk", hcx, hcz));

            if (hflags != 0) {
                String ownerName = this.handler.getOwnerName();
                if (ownerName != null && !ownerName.isEmpty()) {
                    tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.owner_line", ownerName));
                }

                java.util.List<String> wlList = new java.util.ArrayList<>();
                java.util.List<String> blList = new java.util.ArrayList<>();
                for (var e : this.handler.getConfig().playerWhitelist.entrySet()) {
                    if (e.getValue()) wlList.add(e.getKey());
                    else blList.add(e.getKey());
                }
                if (!wlList.isEmpty()) {
                    tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.whitelist",
                            String.join(", ", wlList)));
                }
                if (!blList.isEmpty()) {
                    tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.blacklist",
                            String.join(", ", blList)));
                }
            }

            if ((hflags & BAN_BITS) != 0) {
                tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.ban"));
            }
            if ((hflags & FORCE_BITS) != 0) {
                tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.force_load"));
            }
            if ((hflags & BIT_LUCKY) != 0) {
                tip.add(Text.translatable("cobblemon_ts.chunk_barrier.tip.lucky_ban"));
            }

            if (!tip.isEmpty()) {
                graphics.drawTooltip(textRenderer, tip, java.util.Optional.empty(), mouseX + 12, mouseY - 12);
            }
        }

        String dragMode = addMode
                ? Text.translatable("cobblemon_ts.chunk_barrier.drag_add").getString()
                : Text.translatable("cobblemon_ts.chunk_barrier.drag_sub").getString();
        int hintY = this.y + 32 + RIGHT_SIZE + PAD - 4;
        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.hint",
                        centerChunkX, centerChunkZ, gridSize, gridSize, dragMode).getString(),
                this.x + PAD, hintY, 0xFFBBBBBB, false);

        // [线性-R10] 底部面板
        int panelTop = this.y + backgroundHeight - R10_PANEL_H - PAD;
        graphics.fill(this.x, panelTop, this.x + backgroundWidth, panelTop + R10_PANEL_H, 0xFF121212);
        ChunkBarrierConfig cfg = this.handler.getConfig();

        int forceCnt = curForceChunkCount();
        int banCnt = curBanChunkCount();
        int forceLimit = CharmConfig.getCbForceloadChunkLimit();
        int banLimit = CharmConfig.getCbBanChunkLimit();
        String forceStr = Text.translatable("cobblemon_ts.chunk_barrier.count.force", forceCnt, forceLimit).getString();
        String banStr = Text.translatable("cobblemon_ts.chunk_barrier.count.ban", banCnt, banLimit).getString();
        int forceColor = forceCnt >= forceLimit ? 0xFFFF6666 : 0xFFAAAAAA;
        int banColor = banCnt >= banLimit ? 0xFFFF6666 : 0xFFAAAAAA;
        graphics.drawText(textRenderer, forceStr, this.x + PAD, panelTop + 4, forceColor, false);
        graphics.drawText(textRenderer, banStr, this.x + PAD + 90, panelTop + 4, banColor, false);

        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.empty_list_hint_line1").getString(),
                this.x + PAD, panelTop + 20, 0xFF888888, false);
        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.empty_list_hint_line2").getString(),
                this.x + PAD, panelTop + 32, 0xFF888888, false);

        String title = editingTarget == TARGET_LUCKY
                ? Text.translatable("cobblemon_ts.chunk_barrier.player_title_lucky").getString()
                : Text.translatable("cobblemon_ts.chunk_barrier.player_title").getString();
        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.arrow").getString() + title,
                this.x + PAD, panelTop + 44, editingTarget == TARGET_LUCKY ? 0xFFFFAA22 : 0xFFFFDD66, false);

        int inputRowY = panelTop + 50;
        graphics.fill(this.x + PAD, inputRowY, this.x + PAD + 144, inputRowY + 20, 0xFF1a1a1a);
        graphics.fill(this.x + PAD, inputRowY, this.x + PAD + 144, inputRowY + 1, 0xFF3a3a3a);
        graphics.fill(this.x + PAD, inputRowY + 19, this.x + PAD + 144, inputRowY + 20, 0xFF3a3a3a);

        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.hint_ops_line1").getString(),
                this.x + PAD, panelTop + 74, 0xFF666666, false);
        graphics.drawText(textRenderer,
                Text.translatable("cobblemon_ts.chunk_barrier.hint_ops_line2").getString(),
                this.x + PAD, panelTop + 86, 0xFF666666, false);

        int listBoxX = this.x + LEFT_W + PAD;
        int listBoxY = panelTop + 4;
        int listBoxW = backgroundWidth - LEFT_W - PAD * 3;
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
                Text.translatable("cobblemon_ts.chunk_barrier.list_title.player").getString(),
                playerMap(), playerNameColor(), rowH, colW);
        drawNameListSection(graphics, listBoxX, splitY, listBoxW, listBoxY + listBoxH - splitY,
                Text.translatable("cobblemon_ts.chunk_barrier.list_title.lucky").getString(),
                luckyMap(), luckyNameColor(), rowH, colW);

        // [线性-渲染widget] 手动渲染 EditBox 和 添加按钮
        if (playerNameInput != null) {
            playerNameInput.render(graphics, mouseX, mouseY, partialTick);
        }
        if (addPlayerButton != null) {
            addPlayerButton.render(graphics, mouseX, mouseY, partialTick);
        }

        // [线性-SCALE] 还原 MatrixStack
        if (cts_needScale) {
            graphics.getMatrices().pop();
        }
    }

    private void drawChunkMarks(DrawContext g, int x0, int y0, boolean isCenter, int flags) {
        int size = tileSize;
        int x1 = x0 + size;
        int y1 = y0 + size;

        g.fill(x0, y0, x1, y0 + 1, COLOR_GRID);
        g.fill(x0, y0, x0 + 1, y1, COLOR_GRID);

        if (flags == 0 && !isCenter) return;

        int cellW = size / 5;
        int cellH = size / 2;
        int innerPad = Math.max(1, cellW / 4);

        if ((flags & BIT_POKEMON) != 0) {
            g.fill(x0 + innerPad, y0 + innerPad, x0 + cellW - innerPad, y0 + cellH - innerPad, 0xFF000000 | COLOR_POKEMON);
        }
        if ((flags & BIT_NPC) != 0) {
            g.fill(x0 + innerPad, y0 + cellH + innerPad, x0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_NPC);
        }

        if ((flags & BIT_LUCKY) != 0) {
            int cx0 = x0 + cellW;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad,
                    0xFF000000 | COLOR_LUCKY);
        }

        if ((flags & BIT_LOAD) != 0) {
            int cx0 = x0 + cellW * 2;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_FORCELOAD);
        }

        if ((flags & BIT_PLAYER) != 0) {
            int cx0 = x0 + cellW * 3;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad,
                    0xFF000000 | COLOR_PLAYER);
        }

        if ((flags & BIT_HOSTILE) != 0) {
            int cx0 = x0 + cellW * 4;
            g.fill(cx0 + innerPad, y0 + innerPad, cx0 + cellW - innerPad, y0 + cellH - innerPad, 0xFF000000 | COLOR_HOSTILE);
        }
        if ((flags & BIT_PASSIVE) != 0) {
            int cx0 = x0 + cellW * 4;
            g.fill(cx0 + innerPad, y0 + cellH + innerPad, cx0 + cellW - innerPad, y0 + size - innerPad, 0xFF000000 | COLOR_PASSIVE);
        }

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

    @Override
    protected void drawBackground(DrawContext graphics, float partialTick, int mouseX, int mouseY) {}

    @Override
    protected void drawForeground(DrawContext graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cts_needScale) { mouseX = (mouseX - cts_offsetX) / cts_scale; mouseY = (mouseY - cts_offsetY) / cts_scale; }
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
            int gx = idx % gridSize;
            int gz = idx / gridSize;
            int cx = centerChunkX + (gx - gridSize / 2);
            int cz = centerChunkZ + (gz - gridSize / 2);
            if (isBanLimitFull() && isChunkAddingBan(cx, cz, button)) {
                if (!banLimitNotified) {
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.translatable("cobblemon_ts.chunk_barrier.limit.ban",
                                    curBanChunkCount(), CharmConfig.getCbBanChunkLimit()), false);
                    banLimitNotified = true;
                }
                return true;
            }
            if (isForceLimitFull() && isChunkAddingForceLoad(cx, cz, button)) {
                if (!forceLimitNotified) {
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.translatable("cobblemon_ts.chunk_barrier.limit.forceload",
                                    curForceChunkCount(), CharmConfig.getCbForceloadChunkLimit()), false);
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
        int panelTop = this.y + backgroundHeight - R10_PANEL_H - PAD;
        int listBoxX = this.x + LEFT_W + PAD;
        int listBoxY = panelTop + 4;
        int listBoxW = backgroundWidth - LEFT_W - PAD * 3;
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
        if (cts_needScale) { mouseX = (mouseX - cts_offsetX) / cts_scale; mouseY = (mouseY - cts_offsetY) / cts_scale; dragX /= cts_scale; dragY /= cts_scale; }
        if (isReadOnly()) return false;
        if (dragging) {
            int idx = chunkAt((int) mouseX, (int) mouseY);
            if (idx >= 0 && !dragTouched.contains(idx)) {
                dragTouched.add(idx);
                int gx = idx % gridSize;
                int gz = idx / gridSize;
                int cx = centerChunkX + (gx - gridSize / 2);
                int cz = centerChunkZ + (gz - gridSize / 2);
                if (isBanLimitFull() && isChunkAddingBan(cx, cz, dragButton)) {
                    return true;
                }
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
        ButtonWidget[] widgets = {toggleButtons[0], toggleButtons[1], toggleButtons[2], toggleButtons[3], toggleButtons[4], null, luckyToggleButton, playerToggleButton};
        for (int i = 0; i < bitsByIndex.length; i++) {
            if (bitsByIndex[i] == 0 || widgets[i] == null) continue;
            ButtonWidget btn = widgets[i];
            if (mx >= btn.getX() && mx <= btn.getX() + btn.getWidth()
                    && my >= btn.getY() && my <= btn.getY() + btn.getHeight()) {
                return bitsByIndex[i];
            }
        }
        return 0;
    }

    private void clearBitInRange(int bit) {
        ChunkBarrierConfig cfg = this.handler.getConfig();
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
                int cur = this.handler.getConfig().getFlags(cx, cz);
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

                this.handler.getConfig().setFlags(cx, cz, next);
            }
        } else if (button == 1) {
            this.handler.getConfig().setFlags(cx, cz, 0);
        }
        syncToServer();
    }

    private void syncToServer() {
        ChunkBarrierConfig cfg = this.handler.getConfig();
        PacketDistributor.sendToServer(new SyncConfigPayload(
                this.handler.getBlockPos(),
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
        if (cts_needScale) { mouseX = (mouseX - cts_offsetX) / cts_scale; mouseY = (mouseY - cts_offsetY) / cts_scale; }
        if (isReadOnly()) return false;
        int newSize = gridSize + (vertical > 0 ? 2 : -2);
        if (newSize < 3) newSize = 3;
        if (newSize > maxRange) newSize = maxRange;
        if (newSize % 2 == 0) newSize++;
        if (newSize != gridSize) {
            gridSize = newSize;
            layoutMap();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) sampleAllTerrain(mc.world);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (playerNameInput != null && playerNameInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                if (!isReadOnly()) {
                    String name = playerNameInput.getText().trim().toLowerCase();
                    if (!name.isEmpty()) {
                        currentNameMap().putIfAbsent(name, true);
                        playerNameInput.setText("");
                        syncToServer();
                    }
                }
                return true;
            }
            if (keyCode == 256 || scanCode == 1) {
                playerNameInput.setFocused(false);
                this.close();
                return true;
            }
            if (playerNameInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            return true;
        }
        if (keyCode == 256 || scanCode == 1) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (playerNameInput != null && playerNameInput.isFocused()) {
            if (playerNameInput.charTyped(codePoint, modifiers)) return true;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    // [线性-全局计数] 统计玩家全服禁止区块数
    private int curBanChunkCount() {
        globalCounts = SyncChunkCountS2CPacket.pollLatest();
        if (globalCounts != null) return globalCounts[0];
        int cnt = 0;
        for (var e : this.handler.getConfig().chunkRules.entrySet()) {
            int f = e.getValue() & 0xFF;
            if ((f & BAN_BITS) != 0) cnt++;
        }
        return cnt;
    }

    // [线性-超限] 当前是否已满禁止上限
    private boolean isBanLimitFull() {
        return curBanChunkCount() >= CharmConfig.getCbBanChunkLimit();
    }

    // [线性-超限] 判断某个 chunk 点击是否会加禁止位
    private boolean isChunkAddingBan(int cx, int cz, int button) {
        if (button != 0) return false;
        if (activeBits.isEmpty()) return false;
        int cur = this.handler.getConfig().getFlags(cx, cz);
        return (cur & BAN_BITS) == 0;
    }

    // [线性-全局计数] 统计玩家全服 Force Load 区块数
    private int curForceChunkCount() {
        globalCounts = SyncChunkCountS2CPacket.pollLatest();
        if (globalCounts != null) return globalCounts[1];
        int cnt = 0;
        for (var e : this.handler.getConfig().chunkRules.entrySet()) {
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
        int cur = this.handler.getConfig().getFlags(cx, cz);
        return (cur & FORCE_BITS) == 0 && activeBits.contains(BIT_LOAD);
    }
}