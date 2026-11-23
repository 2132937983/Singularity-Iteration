// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.miophas.singularity_iteration.common.builder.BlueprintBuilderMenu;
import com.miophas.singularity_iteration.common.builder.BuilderActionPacket;
import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_screen;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint Builder screen (0.1.8.0), drawn in the shared Modern-IC theme without a texture:
 * blueprint source (Create schematic slot or schematics folder file), placement controls
 * (rotation, mirror, offset), start / pause / stop, work area preview, progress, missing
 * materials and the active drones.
 *
 * <p>0.1.11.182: the Upload button opens a list of this computer's {@code schematics} folder over the status window; a click
 * uploads that file to the server ({@link BlueprintUploadClient}) and the builder loads it. In single player (and on the
 * host of a LAN world) the folder is the server's own, already in the list, and the button stays off.
 *
 * <p>0.1.11.188: the Catalog button opens the multiblock structures (the build plans of JEI's "Multiblock Structures": tokamak,
 * TESU, steam tunnel, ...); 0.1.11.189: in a screen of their own with the structure in 3D ({@link StructureLibraryScreen}).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class BlueprintBuilderScreen extends mio_icif_screen<BlueprintBuilderMenu> {
    private static final int SX = 28, SY = 16, SW = 141, SH = 76;          // status screen
    private static final int BAR_X = 28, BAR_Y = 144, BAR_W = 141, BAR_H = 4;   // 0.1.11.183: under the placement line

    private SiButton startButton, previewButton, uploadButton;
    /** 0.1.11.182: the picker of local files over the status window, its rows and the first row shown. */
    private boolean picking;
    private List<BlueprintUploadClient.LocalFile> localFiles = List.of();
    private int scroll;
    @Nullable private Component pickError;
    private static final int ROW_H = 9, ROWS = 6, LIST_Y = SY + 13;

    public BlueprintBuilderScreen(BlueprintBuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = BlueprintBuilderMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelY = 142;
        int x = leftPos, y = topPos;
        // row 1: file selection, load and (0.1.11.182) upload from this computer
        add(x + 28, y + 95, 12, "<", BlueprintBuilderBlockEntity.A_FILE_PREV, 0, "file.prev");
        add(x + 97, y + 95, 12, ">", BlueprintBuilderBlockEntity.A_FILE_NEXT, 0, "file.next");
        add(x + 111, y + 95, 28, Component.translatable("gui.mio_icif.builder.load").getString(), BlueprintBuilderBlockEntity.A_LOAD, 0, "load");
        uploadButton = addRenderableWidget(new SiButton(x + 141, y + 95, 28, 12, Component.translatable("gui.mio_icif.builder.upload"), b -> togglePicker()));
        boolean local = minecraft != null && minecraft.isLocalServer();
        uploadButton.active = !local;
        uploadButton.setTooltip(Tooltip.create(Component.translatable(local ? "gui.mio_icif.builder.upload.local" : "gui.mio_icif.builder.upload.tip")));
        // row 2: rotation, mirror, (0.1.11.188) the structure catalog, and (0.1.11.185) placing in the world
        add(x + 28, y + 109, 17, "R", BlueprintBuilderBlockEntity.A_ROTATE, 0, "rotate");
        add(x + 47, y + 109, 17, "M", BlueprintBuilderBlockEntity.A_MIRROR, 0, "mirror");
        SiButton multiblock = addRenderableWidget(new SiButton(x + 66, y + 109, 44, 12, Component.translatable("gui.mio_icif.builder.structures"),
            b -> openCatalog()));
        multiblock.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.builder.structures.tip")));
        SiButton adjust = addRenderableWidget(new SiButton(x + 112, y + 109, 57, 12, Component.translatable("gui.mio_icif.builder.adjust"), b -> placeInWorld()));
        adjust.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.builder.adjust.tip")));
        // row 3: start/pause, stop, preview
        startButton = add(x + 28, y + 123, 50, "", BlueprintBuilderBlockEntity.A_START, 0, "start");
        add(x + 80, y + 123, 40, Component.translatable("gui.mio_icif.builder.stop").getString(), BlueprintBuilderBlockEntity.A_STOP, 0, "stop");
        // 0.1.11.184: the area button cycles blocks, layer, frame only, hidden (the block views are this client's)
        previewButton = addRenderableWidget(new SiButton(x + 122, y + 123, 47, 12, Component.empty(), b -> cyclePreview()));
        previewButton.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.builder.preview.tip")));
        send(BlueprintBuilderBlockEntity.A_REFRESH, 0);
    }

    private SiButton add(int x, int y, int w, String label, int action, int index, String tip) {
        SiButton b = addRenderableWidget(new SiButton(x, y, w, 12, Component.literal(label), btn -> send(action, index)));
        b.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.builder." + tip + ".tip")));
        return b;
    }

    /**
     * 0.1.11.184: hidden → blocks (the area is shown for everyone, the blocks still to place on this client) → this layer
     * only → the frame only → hidden.
     */
    private void cyclePreview() {
        if (!menu.preview()) {
            BuilderGhosts.mode = BuilderGhosts.BLOCKS;
            send(BlueprintBuilderBlockEntity.A_PREVIEW, 0);
        } else if (BuilderGhosts.mode == BuilderGhosts.BLOCKS) {
            BuilderGhosts.mode = BuilderGhosts.LAYER;
        } else if (BuilderGhosts.mode == BuilderGhosts.LAYER) {
            BuilderGhosts.mode = BuilderGhosts.FRAME;
        } else {
            BuilderGhosts.mode = BuilderGhosts.BLOCKS;
            send(BlueprintBuilderBlockEntity.A_PREVIEW, 0);
        }
    }

    /** 0.1.11.185: closes the screen and places the blueprint in the world ({@link BuilderAdjust}). */
    private void placeInWorld() {
        BlueprintBuilderBlockEntity be = builder();
        if (be == null || minecraft == null || minecraft.player == null) return;
        BuilderAdjust.enter(be);
        if (BuilderAdjust.active()) minecraft.player.closeContainer();
    }

    private void togglePicker() {
        picking = !picking;
        scroll = 0;
        pickError = null;
        if (picking) localFiles = BlueprintUploadClient.localFiles();
    }

    /**
     * 0.1.11.189: the structure catalog in a screen of its own (the builder's menu stays open behind it); it starts on the
     * structure the builder has, if any.
     */
    private void openCatalog() {
        if (minecraft == null) return;
        BlueprintBuilderBlockEntity be = builder();
        picking = false;
        minecraft.setScreen(new StructureLibraryScreen(this, menu, be != null && menu.source() == 4 ? be.blueprintName() : null));
    }

    /** The picker row under the mouse (an index into the local files), or -1. */
    private int rowAt(double mouseX, double mouseY) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        if (mx < SX + 2 || mx >= SX + SW - 2 || my < LIST_Y || my >= LIST_Y + ROWS * ROW_H) return -1;
        int i = scroll + (int) ((my - LIST_Y) / ROW_H);
        return i < localFiles.size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picking && button == 0 && over(SX, SY, SW, SH, mouseX, mouseY)) {
            int i = rowAt(mouseX, mouseY);
            if (i >= 0) {
                pickError = BlueprintUploadClient.start(menu.containerId, localFiles.get(i).name());
                if (pickError == null) picking = false;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (picking && over(SX, SY, SW, SH, mouseX, mouseY)) {
            scroll = Math.max(0, Math.min(Math.max(0, localFiles.size() - ROWS), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void send(int action, int index) {
        PacketDistributor.sendToServer(new BuilderActionPacket(menu.containerId, action, index));
    }

    private BlueprintBuilderBlockEntity builder() {
        if (minecraft == null || minecraft.level == null || menu.pos() == null) return null;
        return minecraft.level.getBlockEntity(menu.pos()) instanceof BlueprintBuilderBlockEntity b ? b : null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (startButton != null) startButton.setMessage(Component.translatable(menu.status() == BlueprintBuilderBlockEntity.RUNNING
            ? "gui.mio_icif.builder.pause" : "gui.mio_icif.builder.start"));
        if (previewButton != null) previewButton.setMessage(Component.translatable(!menu.preview() ? "gui.mio_icif.builder.preview.off"
            : BuilderGhosts.mode == BuilderGhosts.BLOCKS ? "gui.mio_icif.builder.preview.blocks"
            : BuilderGhosts.mode == BuilderGhosts.LAYER ? "gui.mio_icif.builder.preview.layer" : "gui.mio_icif.builder.preview.on"));
        if (uploadButton != null && minecraft != null && !minecraft.isLocalServer()) uploadButton.active = !BlueprintUploadClient.busy();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        SiGuiTheme.well(g, x + SX, y + SY, SW, SH);
        SiGuiTheme.slot(g, x + BlueprintBuilderMenu.SCHEMATIC_X - 1, y + BlueprintBuilderMenu.SCHEMATIC_Y - 1);
        SiGuiTheme.slot(g, x + BlueprintBuilderMenu.BATTERY_X - 1, y + BlueprintBuilderMenu.BATTERY_Y - 1);
        SiGuiTheme.well(g, x + 41, y + 95, 55, 12);                         // file name field (0.1.11.182: narrower, room for Upload)
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 151 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 209);
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, (double) menu.energy() / BlueprintBuilderBlockEntity.CAPACITY, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        int total = Math.max(1, menu.total());
        if (!picking)   // 0.1.11.189: part of the status window; it showed through the file list
            SiGuiTheme.bar(g, x + SX + 4, y + SY + 30, SW - 8, 4, (double) menu.doneCount() / total, 0xFF6FE3A0, 0xFF3C9A52);
    }

    @Override
    protected int statusLampColor() { return 0; }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, SiGuiTheme.TEXT, false);
        BlueprintBuilderBlockEntity be = builder();
        if (picking) picker(g, mouseX, mouseY);
        else status(g, be);
        // file name field (0.1.11.182: the upload's progress while one runs) and placement settings
        String file;
        float up = BlueprintUploadClient.progress();
        if (up >= 0) file = Component.translatable(BlueprintUploadClient.finishing() ? "gui.mio_icif.builder.upload.checking"
            : "gui.mio_icif.builder.upload.progress", Math.round(up * 100)).getString();
        else file = be == null || be.files().isEmpty() ? Component.translatable("gui.mio_icif.builder.no_files").getString()
            : be.files().get(Math.min(menu.fileIndex(), be.files().size() - 1));
        small(g, font.plainSubstrByWidth(file, (int) (51 / 0.75F)), 43, 98, up >= 0 ? SiGuiTheme.ACCENT : SiGuiTheme.TEXT_SCREEN);
        String place = Component.translatable("gui.mio_icif.builder.placement", menu.rotation() * 90,
            Component.translatable("gui.mio_icif.builder.mirror." + menu.mirror()).getString(),
            menu.offset(0), menu.offset(1), menu.offset(2)).getString();
        small(g, place, SX, 136, SiGuiTheme.TEXT_SCREEN_DIM);
    }

    /** 0.1.11.182: this computer's schematics folder over the status window: a row a file with its size; a click uploads it. */
    private void picker(GuiGraphics g, int mouseX, int mouseY) {
        int tx = SX + 4;
        small(g, Component.translatable("gui.mio_icif.builder.upload.title").getString(), tx, SY + 4, SiGuiTheme.ACCENT);
        int count = localFiles.size();
        if (count == 0) {
            small(g, Component.translatable("gui.mio_icif.builder.upload.none").getString(), tx, LIST_Y + 2, SiGuiTheme.TEXT_SCREEN_DIM);
            return;
        }
        int hover = rowAt(mouseX, mouseY);
        for (int r = 0; r < ROWS && scroll + r < count; r++) {
            var f = localFiles.get(scroll + r);
            String name = f.name();
            String size = f.size() < 0 ? "?" : f.size() < 1024 ? f.size() + " B" : String.format(java.util.Locale.ROOT, "%,d KB", (f.size() + 1023) / 1024);
            int ry = LIST_Y + r * ROW_H;
            if (scroll + r == hover) g.fill(SX + 2, ry - 1, SX + SW - 2, ry + ROW_H - 1, 0x30FFFFFF);
            int sw = (int) Math.ceil(font.width(size) * 0.75F);
            small(g, font.plainSubstrByWidth(name, (int) ((SW - 14 - sw) / 0.75F)), tx, ry + 1, SiGuiTheme.TEXT_SCREEN);
            small(g, size, SX + SW - 4 - sw, ry + 1, SiGuiTheme.TEXT_SCREEN_DIM);
        }
        if (count > ROWS)
            small(g, (scroll + 1) + "-" + Math.min(count, scroll + ROWS) + " / " + count, SX + SW - 40, SY + 4, SiGuiTheme.TEXT_SCREEN_DIM);
        if (pickError != null) small(g, font.plainSubstrByWidth(pickError.getString(), (int) ((SW - 8) / 0.75F)), tx, SY + SH - 8, SiGuiTheme.BAD);
    }

    private void status(GuiGraphics g, @Nullable BlueprintBuilderBlockEntity be) {
        int tx = SX + 4, ty = SY + 4;
        String name = be == null || be.blueprintName().isEmpty() ? Component.translatable("gui.mio_icif.builder.no_blueprint").getString()
            : menu.source() == 4 ? Component.translatable(be.blueprintName()).getString()   // 0.1.11.188: a structure's title key
            : be.blueprintName();
        String source = Component.translatable("gui.mio_icif.builder.source." + menu.source()).getString();
        small(g, font.plainSubstrByWidth(name, (int) ((SW - 8) / 0.75F)), tx, ty, SiGuiTheme.TEXT_SCREEN);
        small(g, source, tx, ty + 8, SiGuiTheme.TEXT_SCREEN_DIM);
        int st = menu.status();
        int color = st == BlueprintBuilderBlockEntity.RUNNING ? SiGuiTheme.GOOD : st == BlueprintBuilderBlockEntity.TOO_FAR
            || st == BlueprintBuilderBlockEntity.READ_ERROR || st == BlueprintBuilderBlockEntity.TOO_LARGE ? SiGuiTheme.BAD : st == BlueprintBuilderBlockEntity.DONE ? SiGuiTheme.ACCENT : SiGuiTheme.WARN;
        Component status = Component.translatable("gui.mio_icif.builder.status." + st, BlueprintBuilderBlockEntity.RANGE);
        small(g, font.plainSubstrByWidth(status.getString(), (int) ((SW - 8) / 0.75F)), tx, ty + 16, color);
        // 0.1.8.4: materials that replicators are making for this build (UU supply)
        if (be != null && !be.replicatingNames().isEmpty()) {
            small(g, Component.translatable("gui.mio_icif.builder.replicating").getString(), tx, ty + 24, SiGuiTheme.ACCENT);
            int ix = tx + 52;
            for (String entry : be.replicatingNames()) {
                ItemStack stack = stackOf(entry);
                if (stack.isEmpty()) continue;
                g.pose().pushPose();
                g.pose().translate(ix, ty + 21, 0);
                g.pose().scale(0.5F, 0.5F, 1);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
                small(g, entry.substring(entry.indexOf('=') + 1), ix + 9, ty + 24, SiGuiTheme.TEXT_SCREEN);
                ix += 22;
            }
        }
        small(g, Component.translatable("gui.mio_icif.builder.progress", menu.doneCount(), menu.total(), menu.placed()).getString(), tx, ty + 36, SiGuiTheme.TEXT_SCREEN);
        small(g, Component.translatable("gui.mio_icif.builder.obstructed", menu.obstructed()).getString(), tx, ty + 44, menu.obstructed() > 0 ? SiGuiTheme.WARN : SiGuiTheme.TEXT_SCREEN_DIM);
        small(g, Component.translatable("gui.mio_icif.builder.drones", menu.drones(), BlueprintBuilderBlockEntity.DRONES).getString(), tx + 76, ty + 44, SiGuiTheme.TEXT_SCREEN_DIM);
        // missing materials: up to four icons with counts
        if (be != null && !be.missingNames().isEmpty()) {
            small(g, Component.translatable("gui.mio_icif.builder.missing").getString(), tx, ty + 54, SiGuiTheme.BAD);
            int ix = tx + 40;
            for (String entry : be.missingNames()) {
                ItemStack stack = stackOf(entry);
                if (stack.isEmpty()) continue;
                g.pose().pushPose();
                g.pose().translate(ix, ty + 51, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
                small(g, entry.substring(entry.indexOf('=') + 1), ix + 12, ty + 56, SiGuiTheme.TEXT_SCREEN);
                ix += 24;
            }
        }
    }

    private static ItemStack stackOf(String entry) {
        int eq = entry.indexOf('=');
        ResourceLocation id = ResourceLocation.tryParse(eq < 0 ? entry : entry.substring(0, eq));
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (over(BAR_X, BAR_Y - 1, BAR_W, BAR_H + 2, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.builder.energy", String.format("%,d", menu.energy()),
                String.format("%,d", BlueprintBuilderBlockEntity.CAPACITY), BlueprintBuilderBlockEntity.EU_PER_BLOCK), mouseX, mouseY);
            return;
        }
        if (picking) return;   // 0.1.11.182: the picker is over the status window
        if (over(SX, SY, SW, SH, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mio_icif.builder.help.title").withStyle(ChatFormatting.WHITE));
            for (int i = 1; i <= 4; i++) {
                for (var line : font.getSplitter().splitLines(Component.translatable("gui.mio_icif.builder.help." + i).getString(), 220,
                        net.minecraft.network.chat.Style.EMPTY)) lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
