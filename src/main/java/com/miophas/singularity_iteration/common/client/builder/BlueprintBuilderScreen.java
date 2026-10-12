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

/**
 * Blueprint Builder screen (0.1.8.0), drawn in the shared Modern-IC theme without a texture:
 * blueprint source (Create schematic slot or schematics folder file), placement controls
 * (rotation, mirror, offset), start / pause / stop, work area preview, progress, missing
 * materials and the active drones.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class BlueprintBuilderScreen extends mio_icif_screen<BlueprintBuilderMenu> {
    private static final int SX = 28, SY = 16, SW = 141, SH = 76;          // status screen
    private static final int BAR_X = 28, BAR_Y = 140, BAR_W = 141, BAR_H = 4;

    private SiButton startButton, previewButton;

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
        // row 1: file selection and load
        add(x + 28, y + 95, 12, "<", BlueprintBuilderBlockEntity.A_FILE_PREV, 0, "file.prev");
        add(x + 113, y + 95, 12, ">", BlueprintBuilderBlockEntity.A_FILE_NEXT, 0, "file.next");
        add(x + 127, y + 95, 42, Component.translatable("gui.mio_icif.builder.load").getString(), BlueprintBuilderBlockEntity.A_LOAD, 0, "load");
        // row 2: rotation, mirror, offset
        add(x + 28, y + 109, 22, "R", BlueprintBuilderBlockEntity.A_ROTATE, 0, "rotate");
        add(x + 52, y + 109, 22, "M", BlueprintBuilderBlockEntity.A_MIRROR, 0, "mirror");
        String[] axes = {"X", "Y", "Z"};
        for (int a = 0; a < 3; a++) {
            add(x + 78 + a * 31, y + 109, 14, axes[a] + "-", BlueprintBuilderBlockEntity.A_OFFSET, a * 2, "offset");
            add(x + 93 + a * 31, y + 109, 14, axes[a] + "+", BlueprintBuilderBlockEntity.A_OFFSET, a * 2 + 1, "offset");
        }
        // row 3: start/pause, stop, preview
        startButton = add(x + 28, y + 123, 50, "", BlueprintBuilderBlockEntity.A_START, 0, "start");
        add(x + 80, y + 123, 40, Component.translatable("gui.mio_icif.builder.stop").getString(), BlueprintBuilderBlockEntity.A_STOP, 0, "stop");
        previewButton = add(x + 122, y + 123, 47, "", BlueprintBuilderBlockEntity.A_PREVIEW, 0, "preview");
        send(BlueprintBuilderBlockEntity.A_REFRESH, 0);
    }

    private SiButton add(int x, int y, int w, String label, int action, int index, String tip) {
        SiButton b = addRenderableWidget(new SiButton(x, y, w, 12, Component.literal(label), btn -> send(action, index)));
        b.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.builder." + tip + ".tip")));
        return b;
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
        if (previewButton != null) previewButton.setMessage(Component.translatable(menu.preview()
            ? "gui.mio_icif.builder.preview.on" : "gui.mio_icif.builder.preview.off"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        SiGuiTheme.well(g, x + SX, y + SY, SW, SH);
        SiGuiTheme.slot(g, x + BlueprintBuilderMenu.SCHEMATIC_X - 1, y + BlueprintBuilderMenu.SCHEMATIC_Y - 1);
        SiGuiTheme.slot(g, x + BlueprintBuilderMenu.BATTERY_X - 1, y + BlueprintBuilderMenu.BATTERY_Y - 1);
        SiGuiTheme.well(g, x + 41, y + 95, 71, 12);                         // file name field
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 151 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 209);
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, (double) menu.energy() / BlueprintBuilderBlockEntity.CAPACITY, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        int total = Math.max(1, menu.total());
        SiGuiTheme.bar(g, x + SX + 4, y + SY + 30, SW - 8, 4, (double) menu.doneCount() / total, 0xFF6FE3A0, 0xFF3C9A52);
    }

    @Override
    protected int statusLampColor() { return 0; }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, SiGuiTheme.TEXT, false);
        BlueprintBuilderBlockEntity be = builder();
        int tx = SX + 4, ty = SY + 4;
        String name = be == null || be.blueprintName().isEmpty() ? Component.translatable("gui.mio_icif.builder.no_blueprint").getString() : be.blueprintName();
        String source = Component.translatable("gui.mio_icif.builder.source." + menu.source()).getString();
        small(g, font.plainSubstrByWidth(name, (int) ((SW - 8) / 0.75F)), tx, ty, SiGuiTheme.TEXT_SCREEN);
        small(g, source, tx, ty + 8, SiGuiTheme.TEXT_SCREEN_DIM);
        int st = menu.status();
        int color = st == BlueprintBuilderBlockEntity.RUNNING ? SiGuiTheme.GOOD : st == BlueprintBuilderBlockEntity.TOO_FAR
            || st == BlueprintBuilderBlockEntity.READ_ERROR ? SiGuiTheme.BAD : st == BlueprintBuilderBlockEntity.DONE ? SiGuiTheme.ACCENT : SiGuiTheme.WARN;
        small(g, Component.translatable("gui.mio_icif.builder.status." + st, BlueprintBuilderBlockEntity.RANGE).getString(), tx, ty + 16, color);
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
        // file name field and placement settings
        String file = be == null || be.files().isEmpty() ? Component.translatable("gui.mio_icif.builder.no_files").getString()
            : be.files().get(Math.min(menu.fileIndex(), be.files().size() - 1));
        small(g, font.plainSubstrByWidth(file, (int) (67 / 0.75F)), 43, 98, SiGuiTheme.TEXT_SCREEN);
        String place = Component.translatable("gui.mio_icif.builder.placement", menu.rotation() * 90,
            Component.translatable("gui.mio_icif.builder.mirror." + menu.mirror()).getString(),
            menu.offset(0), menu.offset(1), menu.offset(2)).getString();
        small(g, place, SX, 136, SiGuiTheme.TEXT_SCREEN_DIM);
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
