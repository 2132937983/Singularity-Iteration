package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_od_scanner_menu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_od_scanner extends mio_icif_screen<mio_icif_od_scanner_menu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_tool_scanner.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 231;

    // result panel of gui_tool_scanner.png: inner area x 9..166, y 18..143
    private static final int TITLE_X = 12, TITLE_Y = 21;
    private static final int LIST_X = 12, LIST_Y = 33, LIST_W = 142, LIST_BOTTOM = 141;
    private static final int LINE_H = 11;
    private static final int ROWS = (LIST_BOTTOM - LIST_Y) / LINE_H;
    private static final int BAR_X = 157, BAR_W = 6;

    private record Line(String name, String count) { }

    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private boolean dragging;

    public mio_icif_gui_od_scanner(mio_icif_od_scanner_menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        menu.setOnScanResultsUpdated(this::loadScanResults);
        loadScanResults();
    }

    private void loadScanResults() {
        Map<String, Integer> results = menu.getScanResults();
        lines.clear();
        results.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .forEach(e -> {
                String key = e.getKey();
                String name = (key.contains(".") ? Component.translatable(key) : Component.literal(key)).getString();
                lines.add(new Line(name, String.format("%,d", e.getValue())));
            });
        scroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, lines.size() - ROWS);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;   // the panel has no room for the "Inventory" label
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overBar(mouseX, mouseY) && maxScroll() > 0) {
            dragging = true;
            dragTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean overBar(double mx, double my) {
        return mx >= leftPos + BAR_X - 1 && mx < leftPos + BAR_X + BAR_W + 1 && my >= topPos + LIST_Y && my < topPos + LIST_BOTTOM;
    }

    private void dragTo(double my) {
        float t = (float) ((my - topPos - LIST_Y) / (LIST_BOTTOM - LIST_Y));
        scroll = Math.max(0, Math.min(maxScroll(), Math.round(t * maxScroll())));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // header
        g.drawString(font, Component.translatable("gui.mio_icif.od_scanner.result_title"), x + TITLE_X, y + TITLE_Y, 0xFF2A2E33, false);
        if (!lines.isEmpty()) {
            String total = String.valueOf(lines.size());
            g.drawString(font, total, x + 163 - font.width(total), y + TITLE_Y, 0xFF808890, false);
        }
        g.fill(x + LIST_X, y + LIST_Y - 3, x + 163, y + LIST_Y - 2, 0xFFC4C8CD);

        if (lines.isEmpty()) {
            Component none = Component.translatable("gui.mio_icif.od_scanner.no_ore_found");
            g.drawString(font, none, x + LIST_X + (LIST_W - font.width(none)) / 2, y + LIST_Y + 40, 0xFF808890, false);
            return;
        }
        g.enableScissor(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_BOTTOM);
        for (int row = 0; row < ROWS && scroll + row < lines.size(); row++) {
            Line line = lines.get(scroll + row);
            int ly = y + LIST_Y + row * LINE_H;
            if (((scroll + row) & 1) == 1) g.fill(x + LIST_X, ly - 1, x + LIST_X + LIST_W, ly + LINE_H - 1, 0xFFE4E7EA);
            int countW = font.width(line.count());
            String name = font.plainSubstrByWidth(line.name(), LIST_W - countW - 8);
            g.drawString(font, name, x + LIST_X + 2, ly + 1, 0xFF2A2E33, false);
            g.drawString(font, line.count(), x + LIST_X + LIST_W - countW - 2, ly + 1, 0xFF2E7D44, false);
        }
        g.disableScissor();

        // scrollbar (only when the list overflows)
        if (maxScroll() > 0) {
            int top = y + LIST_Y, h = LIST_BOTTOM - LIST_Y;
            g.fill(x + BAR_X, top, x + BAR_X + BAR_W, top + h, 0xFFC9CDD2);
            int thumb = Math.max(10, h * ROWS / lines.size());
            int ty = top + (h - thumb) * scroll / maxScroll();
            g.fill(x + BAR_X, ty, x + BAR_X + BAR_W, ty + thumb, 0xFF6B737D);
            g.fill(x + BAR_X + 1, ty + 1, x + BAR_X + BAR_W - 1, ty + thumb - 1, 0xFF8C949E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // everything is drawn in renderBg (absolute coordinates, scissor-safe)
    }
}
