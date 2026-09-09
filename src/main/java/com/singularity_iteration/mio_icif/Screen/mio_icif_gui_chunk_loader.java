package com.singularity_iteration.mio_icif.Screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.singularity_iteration.mio_icif.Menu.Producer.ChunkLoaderMenu;
import com.singularity_iteration.mio_icif.network.ChunkLoaderTogglePacket;
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

    private static final int LOADED_OVERLAY = 0x5500FF00;
    private static final int UNLOADED_OVERLAY = 0x40000000;
    private static final int SELF_COLOR = 0x80FFFF00;

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

    private void rebuildTerrainTexture(ChunkPos selfChunk) {
        if (terrainImage == null || minecraft == null || minecraft.level == null) return;

        for (int ci = -MAP_RADIUS; ci <= MAP_RADIUS; ci++) {
            for (int cj = -MAP_RADIUS; cj <= MAP_RADIUS; cj++) {
                ChunkPos chunkPos = new ChunkPos(selfChunk.x + ci, selfChunk.z + cj);
                ChunkAccess chunk = minecraft.level.getChunk(chunkPos.x, chunkPos.z);

                int baseX = (ci + MAP_RADIUS) * MAP_CELL_SIZE;
                int baseY = (cj + MAP_RADIUS) * MAP_CELL_SIZE;

                for (int cx = 0; cx < 16; cx++) {
                    for (int cz = 0; cz < 16; cz++) {
                        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                            chunkPos.getMinBlockX() + cx, surfaceY, chunkPos.getMinBlockZ() + cz);
                        BlockState state = chunk.getBlockState(pos);

                        if (state.isAir()) {
                            surfaceY--;
                            if (surfaceY < minecraft.level.getMinBuildHeight()) {
                                terrainImage.setPixelRGBA(baseX + cx, baseY + cz, 0);
                                continue;
                            }
                            pos = new net.minecraft.core.BlockPos(pos.getX(), surfaceY, pos.getZ());
                            state = chunk.getBlockState(pos);
                        }

                        MapColor mapColor = state.getMapColor(minecraft.level, pos);
                        int color = 0;
                        if (mapColor != null) {
                            int rgb = mapColor.col;
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;
                            color = 0xFF000000 | (b << 16) | (g << 8) | r;
                        }
                        terrainImage.setPixelRGBA(baseX + cx, baseY + cz, color);
                    }
                }
            }
        }

        terrainTexture.upload();
        terrainDirty = false;
    }

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

        for (int i = -MAP_RADIUS; i <= MAP_RADIUS; i++) {
            for (int j = -MAP_RADIUS; j <= MAP_RADIUS; j++) {
                int cellX = x + MAP_CENTER_X + i * MAP_CELL_SIZE;
                int cellY = y + MAP_CENTER_Y + j * MAP_CELL_SIZE;

                boolean loaded = menu.isChunkLoaded(i, j);
                boolean isSelf = (i == 0 && j == 0);

                if (loaded) amountLoaded++;

                if (isSelf) {
                    guiGraphics.fill(cellX, cellY, cellX + MAP_CELL_SIZE, cellY + MAP_CELL_SIZE, SELF_COLOR);
                } else if (loaded) {
                    guiGraphics.fill(cellX, cellY, cellX + MAP_CELL_SIZE, cellY + MAP_CELL_SIZE, LOADED_OVERLAY);
                } else {
                    guiGraphics.fill(cellX, cellY, cellX + MAP_CELL_SIZE, cellY + MAP_CELL_SIZE, UNLOADED_OVERLAY);
                }
            }
        }

        guiGraphics.drawString(this.font, String.valueOf(amountLoaded), x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y, 4210752, false);
        guiGraphics.drawString(this.font, "/", x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y + 9, 4210752, false);
        guiGraphics.drawString(this.font, String.valueOf(menu.getMaxChunks()), x + CHUNK_COUNT_X, y + CHUNK_COUNT_Y + 18, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int dx = -MAP_RADIUS; dx <= MAP_RADIUS; dx++) {
            for (int dy = -MAP_RADIUS; dy <= MAP_RADIUS; dy++) {
                int cellX = x + MAP_CENTER_X + dx * MAP_CELL_SIZE;
                int cellY = y + MAP_CENTER_Y + dy * MAP_CELL_SIZE;

                if (mouseX > cellX && mouseX <= cellX + MAP_CELL_SIZE &&
                    mouseY > cellY && mouseY <= cellY + MAP_CELL_SIZE) {

                    ChunkLoaderMenu menu = this.getMenu();
                    if (menu != null && menu.getBlockPos() != null && button == 0) {
                        PacketDistributor.sendToServer(new ChunkLoaderTogglePacket(
                            menu.getBlockPos(), dx, dy));
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
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
                guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX, mouseY);
            }
            return;
        }

        for (int dx = -MAP_RADIUS; dx <= MAP_RADIUS; dx++) {
            for (int dy = -MAP_RADIUS; dy <= MAP_RADIUS; dy++) {
                int cellX = x + MAP_CENTER_X + dx * MAP_CELL_SIZE;
                int cellY = y + MAP_CENTER_Y + dy * MAP_CELL_SIZE;

                if (mouseX > cellX && mouseX <= cellX + MAP_CELL_SIZE &&
                    mouseY > cellY && mouseY <= cellY + MAP_CELL_SIZE) {

                    ChunkLoaderMenu menu = this.getMenu();
                    if (menu != null) {
                        ChunkPos selfChunk = menu.getSelfChunkPos();
                        ChunkPos chunk = new ChunkPos(selfChunk.x + dx, selfChunk.z + dy);
                        boolean loaded = menu.isChunkLoaded(dx, dy);
                        boolean isSelf = (dx == 0 && dy == 0);

                        String tooltip;
                        if (isSelf) {
                            tooltip = "Chunk (self) [" + chunk.x + ", " + chunk.z + "]";
                        } else if (loaded) {
                            tooltip = "Chunk (loaded) [" + chunk.x + ", " + chunk.z + "]";
                        } else {
                            tooltip = "Chunk (unloaded) [" + chunk.x + ", " + chunk.z + "]";
                        }
                        guiGraphics.renderTooltip(this.font, Component.literal(tooltip), mouseX, mouseY);
                    }
                    return;
                }
            }
        }
    }
}