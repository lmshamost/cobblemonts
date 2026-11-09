package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.ChunkBarrierServerConfig;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import com.cobblemon_ts.mod.network.SyncConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

public class ChunkBarrierScreen extends AbstractContainerScreen<ChunkBarrierContainer> {

    private static final int LEFT_W = 110;
    private static final int RIGHT_SIZE = 256;
    private static final int PAD = 8;
    private static final int BTN_H = 28;

    private static final int COLOR_POKEMON   = 0xE74C3C;
    private static final int COLOR_NPC       = 0x3498DB;
    private static final int COLOR_HOSTILE   = 0xE67E22;
    private static final int COLOR_PASSIVE   = 0x2ECC71;
    private static final int COLOR_FORCELOAD = 0x9B59B6;
    private static final int COLOR_GRID      = 0xFF333333;
    private static final int COLOR_HOVER     = 0x55FFFFFF;
    private static final int COLOR_UNLOADED  = 0xFF1a1a1a;
    private static final int COLOR_OFF       = 0xFF888888;

    private static final int BIT_POKEMON = ChunkBarrierConfig.BIT_POKEMON;
    private static final int BIT_NPC     = ChunkBarrierConfig.BIT_NPC;
    private static final int BIT_HOSTILE = ChunkBarrierConfig.BIT_HOSTILE;
    private static final int BIT_PASSIVE = ChunkBarrierConfig.BIT_PASSIVE;
    private static final int BIT_LOAD    = ChunkBarrierConfig.BIT_FORCELOAD;

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

    public ChunkBarrierScreen(ChunkBarrierContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = LEFT_W + RIGHT_SIZE + PAD * 3;
        this.imageHeight = 32 + RIGHT_SIZE + PAD * 2;
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
        this.maxRange = ChunkBarrierServerConfig.getMaxRange();
        this.gridSize = Math.min(ChunkBarrierServerConfig.getDefaultRange(), maxRange);
        if (gridSize % 2 == 0) gridSize++;

        int baseX = leftPos + PAD;
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

        addRenderableWidget(Button.builder(
                Component.literal(""),
                btn -> addMode = !addMode
        ).bounds(baseX, baseY + 5 * (BTN_H + 4), LEFT_W - PAD * 2, BTN_H).build());

        this.centerChunkX = this.menu.getBlockPos().getX() >> 4;
        this.centerChunkZ = this.menu.getBlockPos().getZ() >> 4;
        layoutMap();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) sampleAllTerrain(mc.level);
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
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1a1a1a);
        graphics.drawString(font, Component.translatable("container.cobblemon_ts.chunk_barrier").getString(), leftPos + PAD, topPos + PAD, 0xFFFFFF);

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

                drawChunkMarks(graphics, x0, y0, gx == half && gz == half, flags);
            }
        }

        String dragMode = addMode
                ? Component.translatable("cobblemon_ts.chunk_barrier.drag_add").getString()
                : Component.translatable("cobblemon_ts.chunk_barrier.drag_sub").getString();
        graphics.drawString(font,
                Component.translatable("cobblemon_ts.chunk_barrier.hint",
                        centerChunkX, centerChunkZ, gridSize, gridSize, dragMode).getString(),
                leftPos + PAD, topPos + imageHeight - 14, 0xFFBBBBBB);
    }

    private void drawChunkMarks(GuiGraphics g, int x0, int y0, boolean isCenter, int flags) {
        int size = tileSize;
        int x1 = x0 + size;
        int y1 = y0 + size;

        g.fill(x0, y0, x1, y0 + 1, COLOR_GRID);
        g.fill(x0, y0, x0 + 1, y1, COLOR_GRID);

        if (isCenter) {
            int col = (flags & BIT_LOAD) != 0 ? (0xFF000000 | COLOR_FORCELOAD) : COLOR_CENTER;
            int thick = Math.max(2, size / 14);
            int len = size / 2;
            int midX = x0 + size / 2;
            int midY = y0 + size / 2;
            g.fill(midX - len, midY - thick, midX + len, midY + thick + 1, col);
            g.fill(midX - thick, midY - len, midX + thick + 1, midY + len, col);
        }

        if (flags == 0) return;

        int cornerSz = Math.max(10, size / 4);
        int thick = Math.max(2, cornerSz / 5);

        if ((flags & BIT_POKEMON) != 0) {
            drawThreeStripes(g, x0 + 1, y0 + 1, cornerSz, cornerSz, COLOR_POKEMON, thick);
        }
        if ((flags & BIT_HOSTILE) != 0) {
            drawThreeStripes(g, x1 - 1 - cornerSz, y0 + 1, cornerSz, cornerSz, COLOR_HOSTILE, thick);
        }
        if ((flags & BIT_PASSIVE) != 0) {
            drawThreeStripes(g, x0 + 1, y1 - 1 - cornerSz, cornerSz, cornerSz, COLOR_PASSIVE, thick);
        }
        if ((flags & BIT_NPC) != 0) {
            drawThreeStripes(g, x1 - 1 - cornerSz, y1 - 1 - cornerSz, cornerSz, cornerSz, COLOR_NPC, thick);
        }

        if ((flags & BIT_LOAD) != 0 && !isCenter) {
            int col = 0xFF000000 | COLOR_FORCELOAD;
            int cx = x0 + size / 2;
            int cy = y0 + size / 2;
            int cthick = Math.max(2, size / 14);
            int clen = size / 3;
            g.fill(cx - clen, cy - cthick, cx + clen, cy + cthick + 1, col);
            g.fill(cx - cthick, cy - clen, cx + cthick + 1, cy + clen, col);
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
        int idx = chunkAt((int) mouseX, (int) mouseY);
        if (idx >= 0) {
            dragging = true;
            dragButton = button;
            dragTouched.clear();
            applyChunkClick(idx, button, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int idx = chunkAt((int) mouseX, (int) mouseY);
            if (idx >= 0 && !dragTouched.contains(idx)) {
                dragTouched.add(idx);
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
                    for (int bit : activeBits) {
                        next = (next & bit) != 0 ? (next & ~bit) : (next | bit);
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
        PacketDistributor.sendToServer(new SyncConfigPayload(
                this.menu.getBlockPos(),
                new HashMap<>(this.menu.getConfig().chunkRules)
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
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
}