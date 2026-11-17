package com.miophas.singularity_iteration.common.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.miophas.singularity_iteration.common.menu.producer.ChunkLoaderMenu;
import com.miophas.singularity_iteration.common.network.ChunkLoaderTogglePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_chunk_loader extends mio_icif_screen<ChunkLoaderMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_chunk_loader.png");

    private static final int ENERGY_GAUGE_X = 9;
    private static final int ENERGY_GAUGE_Y = 125;

    private static final int MAP_CENTER_X = 89;
    private static final int MAP_CENTER_Y = 80;
    private static final int MAP_CELL_SIZE = 16;
    private static final int MAP_RADIUS = 4;
    private static final int MAP_SIZE = MAP_CELL_SIZE * (MAP_RADIUS * 2 + 1);

    /** 0.1.11.38: cell overlays by {@link #cellState}: loading, selected but not loading, running beside a loading chunk, not selected. */
    private static final int[] CELL_COLOURS = {0x2840FF90, 0x50FF9A20, 0x30FFFFFF, 0x60000000};
    /** Frames of the loading and the waiting cells (the ring and the unselected cells have none). */
    private static final int[] CELL_FRAMES = {0xE060FFA0, 0xF0FFA030, 0, 0};
    private static final int STATE_HELD = 0, STATE_WAITING = 1, STATE_RING = 2, STATE_OFF = 3;
    private static final int SELF_FRAME = 0xFFFFE040;

    private static final int CHUNK_COUNT_X = 8;
    private static final int CHUNK_COUNT_Y = 16;

    private static final int TERRAIN_REFRESH_INTERVAL = 40;

    private NativeImage terrainImage;
    private DynamicTexture terrainTexture;
    private ResourceLocation terrainTextureId;
    private boolean terrainDirty = true;
    private int terrainTickCounter = 0;

    public mio_icif_gui_chunk_loader(ChunkLoaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = ChunkLoaderMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;

        if (terrainImage == null) {
            terrainImage = new NativeImage(MAP_SIZE, MAP_SIZE, true);
            terrainTexture = new DynamicTexture(terrainImage);
            terrainTextureId = ResourceLocation.fromNamespaceAndPath("mio_icif", "dynamic/chunk_loader_terrain_" + System.identityHashCode(this));
            minecraft.getTextureManager().register(terrainTextureId, terrainTexture);
        }
        // 0.1.11.38 (chunk loader audit C2): select the whole 9 x 9 (nearest first, up to the limit) / clear all but the own chunk
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        var all = addRenderableWidget(new com.miophas.singularity_iteration.common.client.screen.widget.SiButton(x + 7, y + 64, 18, 14,
            Component.literal("+"), b -> area(true)));
        all.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.mio_icif.chunk_loader.select_all")));
        var none = addRenderableWidget(new com.miophas.singularity_iteration.common.client.screen.widget.SiButton(x + 7, y + 82, 18, 14,
            Component.literal("−"), b -> area(false)));
        none.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.mio_icif.chunk_loader.clear")));
    }

    private void area(boolean select) {
        if (menu.getBlockPos() != null)
            PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.network.ChunkLoaderAreaPacket(
                menu.getBlockPos(), -MAP_RADIUS, -MAP_RADIUS, MAP_RADIUS, MAP_RADIUS, select));
    }

    @Override
    public void removed() {
        super.removed();
        if (terrainTextureId != null && minecraft != null) {
            minecraft.getTextureManager().release(terrainTextureId);
        }
        if (terrainImage != null) {
            terrainImage.close();
            terrainImage = null;
        }
        terrainTexture = null;
        terrainTextureId = null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        terrainTickCounter++;
        if (terrainTickCounter >= TERRAIN_REFRESH_INTERVAL) {
            terrainTickCounter = 0;
            terrainDirty = true;
        }
    }

    /** A map colour as the ABGR pixel of the terrain image (0 for none). */
    private static int abgr(MapColor mapColor) {
        if (mapColor == null) return 0;
        int rgb = mapColor.col;
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }

    private void rebuildTerrainTexture(ChunkPos selfChunk) {
        if (terrainImage == null || minecraft == null || minecraft.level == null) return;
        // 0.1.11.35: under a roof (the Nether) the top block of every column is the bedrock ceiling, so the map was one
        // flat grey; there each column shows the first block at or below the loader's height (the floor around it)
        boolean ceiling = minecraft.level.dimensionType().hasCeiling();
        int floorStart = menu.getBlockPos() != null ? menu.getBlockPos().getY() : minecraft.level.getMinBuildHeight();
        int minY = minecraft.level.getMinBuildHeight();
        // 0.1.11.38 (chunk loader audit C2): the height of each column, for relief shading as on a vanilla map
        int[] colour = new int[MAP_SIZE * MAP_SIZE], height = new int[MAP_SIZE * MAP_SIZE];
        var cursor = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int ci = -MAP_RADIUS; ci <= MAP_RADIUS; ci++) {
            for (int cj = -MAP_RADIUS; cj <= MAP_RADIUS; cj++) {
                ChunkPos chunkPos = new ChunkPos(selfChunk.x + ci, selfChunk.z + cj);
                ChunkAccess chunk = minecraft.level.getChunk(chunkPos.x, chunkPos.z);
                int baseX = (ci + MAP_RADIUS) * MAP_CELL_SIZE;
                int baseY = (cj + MAP_RADIUS) * MAP_CELL_SIZE;
                for (int cx = 0; cx < 16; cx++) {
                    for (int cz = 0; cz < 16; cz++) {
                        int y = ceiling ? floorStart : chunk.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                        cursor.set(chunkPos.getMinBlockX() + cx, y, chunkPos.getMinBlockZ() + cz);
                        BlockState state = chunk.getBlockState(cursor);
                        while (state.isAir() && y > minY && (ceiling || y >= chunk.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz) - 1)) {
                            cursor.setY(--y);
                            state = chunk.getBlockState(cursor);
                        }
                        int at = (baseY + cz) * MAP_SIZE + baseX + cx;
                        colour[at] = state.isAir() ? 0 : abgr(state.getMapColor(minecraft.level, cursor));
                        height[at] = y;
                    }
                }
            }
        }
        for (int py = 0; py < MAP_SIZE; py++) {
            for (int px = 0; px < MAP_SIZE; px++) {
                int at = py * MAP_SIZE + px, c = colour[at];
                if (c != 0 && py > 0) {
                    int north = height[at - MAP_SIZE];
                    int shade = height[at] > north ? 255 : height[at] < north ? 180 : 220;   // vanilla map brightness steps
                    c = 0xFF000000 | (((c >> 16 & 0xFF) * shade / 255) << 16) | (((c >> 8 & 0xFF) * shade / 255) << 8) | ((c & 0xFF) * shade / 255);
                }
                terrainImage.setPixelRGBA(px, py, c);
            }
        }
        terrainTexture.upload();
        terrainDirty = false;
    }

    /** 0.1.11.38: the state of the cell at offset (dx, dz); see {@link #CELL_COLOURS}. */
    private int cellState(int dx, int dz) {
        if (menu.isChunkHeld(dx, dz)) return STATE_HELD;
        if (menu.isChunkLoaded(dx, dz)) return STATE_WAITING;
        for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++)
            if ((ox != 0 || oz != 0) && Math.abs(dx + ox) <= MAP_RADIUS && Math.abs(dz + oz) <= MAP_RADIUS && menu.isChunkHeld(dx + ox, dz + oz)) return STATE_RING;
        return STATE_OFF;
    }

    private static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        g.fill(x0, y0, x1, y0 + 1, colour); g.fill(x0, y1 - 1, x1, y1, colour);
        g.fill(x0, y0, x0 + 1, y1, colour); g.fill(x1 - 1, y0, x1, y1, colour);
    }

    /** The map cell under the mouse as {dx, dz}, or null. */
    private int[] cellAt(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        int dx = (int) Math.floor((mouseX - x - MAP_CENTER_X) / MAP_CELL_SIZE), dz = (int) Math.floor((mouseY - y - MAP_CENTER_Y) / MAP_CELL_SIZE);
        return Math.abs(dx) <= MAP_RADIUS && Math.abs(dz) <= MAP_RADIUS ? new int[]{dx, dz} : null;
    }

    private int dragButton = -1;
    private int[] dragStart, dragEnd;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ChunkLoaderMenu menu = this.getMenu();
        if (menu == null || menu.getBlockPos() == null) return;

        drawLightningEnergy(guiGraphics, x + ENERGY_GAUGE_X, y + ENERGY_GAUGE_Y, menu.getEnergy(), menu.getMaxEnergy());

        if (minecraft == null || minecraft.level == null) return;

        ChunkPos selfChunk = menu.getSelfChunkPos();

        if (terrainDirty) {
            rebuildTerrainTexture(selfChunk);
        }

        int mapX = x + MAP_CENTER_X - MAP_RADIUS * MAP_CELL_SIZE;
        int mapY = y + MAP_CENTER_Y - MAP_RADIUS * MAP_CELL_SIZE;

        RenderSystem.setShaderTexture(0, terrainTextureId);
        guiGraphics.blit(terrainTextureId, mapX, mapY, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        int amountLoaded = 0;
        // 0.1.11.38 (chunk loader audit C2): four cell states: holds a ticket (loading), selected but not loading (a limit,
        // no energy), running because a loading chunk is next to it (not billed: a ticket also ticks the chunks around
        // it, as /forceload), not selected. The loader's own chunk has a yellow frame.
        for (int i = -MAP_RADIUS; i <= MAP_RADIUS; i++) {
            for (int j = -MAP_RADIUS; j <= MAP_RADIUS; j++) {
                int cellX = x + MAP_CENTER_X + i * MAP_CELL_SIZE;
                int cellY = y + MAP_CENTER_Y + j * MAP_CELL_SIZE;
                if (menu.isChunkLoaded(i, j)) amountLoaded++;
                int state = cellState(i, j);
                guiGraphics.fill(cellX, cellY, cellX + MAP_CELL_SIZE, cellY + MAP_CELL_SIZE, CELL_COLOURS[state]);
                if (CELL_FRAMES[state] != 0) frame(guiGraphics, cellX + 1, cellY + 1, cellX + MAP_CELL_SIZE - 1, cellY + MAP_CELL_SIZE - 1, CELL_FRAMES[state]);
                if (i == 0 && j == 0) frame(guiGraphics, cellX, cellY, cellX + MAP_CELL_SIZE, cellY + MAP_CELL_SIZE, SELF_FRAME);
            }
        }
        if (dragButton >= 0 && dragEnd != null && !java.util.Arrays.equals(dragStart, dragEnd)) {
            int x0 = x + MAP_CENTER_X + Math.min(dragStart[0], dragEnd[0]) * MAP_CELL_SIZE, x1 = x + MAP_CENTER_X + (Math.max(dragStart[0], dragEnd[0]) + 1) * MAP_CELL_SIZE;
            int y0 = y + MAP_CENTER_Y + Math.min(dragStart[1], dragEnd[1]) * MAP_CELL_SIZE, y1 = y + MAP_CENTER_Y + (Math.max(dragStart[1], dragEnd[1]) + 1) * MAP_CELL_SIZE;
            frame(guiGraphics, x0, y0, x1, y1, dragButton == 0 ? 0xFFFFFFFF : 0xFFFF6060);
        }

        guiGraphics.drawString(this.font, String.valueOf(amountLoaded), x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y, 4210752, false);
        guiGraphics.drawString(this.font, "/", x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y + 9, 4210752, false);
        guiGraphics.drawString(this.font, String.valueOf(menu.getMaxChunks()), x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y + 18, 4210752, false);
    }

    /**
     * 0.1.11.38 (chunk loader audit C2): left click toggles a chunk, a left drag selects the rectangle (nearest first, up
     * to the limit), a right click or right drag clears it (the loader's own chunk stays).
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] cell = cellAt(mouseX, mouseY);
        if (cell != null && (button == 0 || button == 1) && menu.getBlockPos() != null) {
            dragButton = button; dragStart = cell; dragEnd = cell;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragButton >= 0 && button == dragButton) {
            int[] cell = cellAt(mouseX, mouseY);
            if (cell != null) dragEnd = cell;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragButton >= 0 && button == dragButton) {
            int[] end = dragEnd != null ? dragEnd : dragStart;
            if (menu.getBlockPos() != null) {
                if (button == 0 && java.util.Arrays.equals(dragStart, end))
                    PacketDistributor.sendToServer(new ChunkLoaderTogglePacket(menu.getBlockPos(), end[0], end[1]));
                else
                    PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.network.ChunkLoaderAreaPacket(
                        menu.getBlockPos(), dragStart[0], dragStart[1], end[0], end[1], button == 0));
            }
            dragButton = -1; dragStart = null; dragEnd = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawTitle(guiGraphics);
        // 0.1.10.11: why the loader does not load (all) its chunks, as a band over the top of the map
        int status = menu.getStatus();
        if (status != com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_ACTIVE
                && status != com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_STARTING) {
            int left = MAP_CENTER_X - MAP_RADIUS * MAP_CELL_SIZE, width = MAP_SIZE;
            int top = MAP_CENTER_Y - MAP_RADIUS * MAP_CELL_SIZE;
            guiGraphics.fill(left, top, left + width, top + 12, 0xB0000000);
            Component text = statusText(status);
            int drawn = Math.min(font.width(text), width - 6);
            int color = status == com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_LIMITED ? 0xFFFFD27A : 0xFFFF8A80;
            drawFitted(guiGraphics, font, text, left + (width - drawn) / 2, top + 2, width - 6, color, false);
        }
    }

    private Component statusText(int status) {
        String[] names = com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_NAMES;
        String name = status >= 0 && status < names.length ? names[status] : "starting";
        if (status == com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_GRACE) {   // 0.1.11.37
            int left = menu.getGraceSecondsLeft();
            return Component.translatable("gui.mio_icif.chunk_loader.status.grace", String.format(java.util.Locale.ROOT, "%d:%02d", left / 60, left % 60));
        }
        return Component.translatable("gui.mio_icif.chunk_loader.status." + name, menu.getForcedChunkCount(), menu.getLoadedChunkCount());
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + ENERGY_GAUGE_X && mouseX <= x + ENERGY_GAUGE_X + LIGHTNING_WIDTH &&
            mouseY >= y + ENERGY_GAUGE_Y && mouseY <= y + ENERGY_GAUGE_Y + LIGHTNING_HEIGHT) {
            ChunkLoaderMenu menu = this.getMenu();
            if (menu != null) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.translatable("gui.mio_icif.chunk_loader.energy", menu.getEnergy(), menu.getMaxEnergy()),
                    Component.translatable("gui.mio_icif.chunk_loader.cost", menu.getEnergyPerTick())), mouseX, mouseY);
            }
            return;
        }

        // 0.1.10.11: the chunk count tells the state and how many selected chunks hold a ticket
        if (mouseX >= x + CHUNK_COUNT_X && mouseX <= x + CHUNK_COUNT_X + 16 && mouseY >= y + CHUNK_COUNT_Y && mouseY <= y + CHUNK_COUNT_Y + 26) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(statusText(menu.getStatus()),
                Component.translatable("gui.mio_icif.chunk_loader.granted", menu.getForcedChunkCount(), menu.getLoadedChunkCount(), menu.getMaxChunks()),
                Component.translatable("gui.mio_icif.chunk_loader.cost", menu.getEnergyPerTick())),
                mouseX, mouseY);
            return;
        }

        int[] cell = cellAt(mouseX, mouseY);
        if (cell != null && dragButton < 0) {
            ChunkPos selfChunk = menu.getSelfChunkPos();
            ChunkPos chunk = new ChunkPos(selfChunk.x + cell[0], selfChunk.z + cell[1]);
            String state = new String[] {"held", "waiting", "ring", "unselected"}[cellState(cell[0], cell[1])];
            var lines = new java.util.ArrayList<Component>();
            lines.add(Component.translatable("gui.mio_icif.chunk_loader.cell." + state, chunk.x, chunk.z));
            if (cell[0] == 0 && cell[1] == 0) lines.add(Component.translatable("gui.mio_icif.chunk_loader.cell.own").withStyle(net.minecraft.ChatFormatting.YELLOW));
            if (state.equals("waiting")) lines.add(statusText(menu.getStatus()).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
            lines.add(Component.translatable("gui.mio_icif.chunk_loader.cell.help").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }
}