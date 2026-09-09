package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_od_scanner_menu;
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
    private static final ResourceLocation SCROLLBAR_TEXTURE =
        ResourceLocation.parse("minecraft:textures/gui/container/creative_inventory/tabs.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 231;

    private static final int TITLE_X = 10;
    private static final int TITLE_Y = 19;

    private static final int RESULT_START_X = 8;
    private static final int RESULT_START_Y = 35;
    private static final int RESULT_LINE_HEIGHT = 12;

    private static final int RESULT_AREA_BOTTOM = 149;
    private static final int MAX_VISIBLE_ROWS = (RESULT_AREA_BOTTOM - RESULT_START_Y) / RESULT_LINE_HEIGHT;

    private static final int COLUMN_COUNT = 2;
    private static final int COLUMN_WIDTH = 80;
    private static final int COLUMN_GAP = 4;

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_X_OFFSET = GUI_WIDTH - SCROLLBAR_WIDTH - 2;

    private List<String> resultLines = new ArrayList<>();
    private float scrollOffset;

    public mio_icif_gui_od_scanner(mio_icif_od_scanner_menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.scrollOffset = 0.0f;

        menu.setOnScanResultsUpdated(this::reloadScanResults);
        loadScanResults();
    }

    private void loadScanResults() {
        Map<String, Integer> results = menu.getScanResults();
        resultLines.clear();

        if (results.isEmpty()) {
            resultLines.add(Component.translatable("gui.mio_icif.od_scanner.no_ore_found").getString());
        } else {
            results.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String key = entry.getKey();
                    Component oreName = key.contains(".") ? Component.translatable(key) : Component.literal(key);
                    String line = entry.getValue() + Component.translatable("gui.mio_icif.od_scanner.ore_count_suffix").getString() + oreName.getString();
                    resultLines.add(line);
                });
        }
        this.scrollOffset = 0;
    }

    private void reloadScanResults() {
        loadScanResults();
    }

    private int getTotalDisplayRows() {
        if (resultLines.isEmpty()) return 0;
        return (resultLines.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
    }

    private int getMaxScroll() {
        if (resultLines.isEmpty()) return 0;
        return getTotalDisplayRows();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (float) verticalAmount));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int guiX = (this.width - this.imageWidth) / 2;
            int guiY = (this.height - this.imageHeight) / 2;
            int scrollbarX = guiX + SCROLLBAR_X_OFFSET;
            int scrollbarTop = guiY + RESULT_START_Y;
            int scrollbarHeight = MAX_VISIBLE_ROWS * RESULT_LINE_HEIGHT;

            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= scrollbarTop && mouseY < scrollbarTop + scrollbarHeight) {
                int maxScroll = getMaxScroll();
                if (maxScroll > 0) {
                    float clickRatio = (float) (mouseY - scrollbarTop) / scrollbarHeight;
                    this.scrollOffset = Math.max(0, Math.min(maxScroll, (int) (clickRatio * (maxScroll + 1))));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        renderScrollbar(guiGraphics, x, y);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int guiX, int guiY) {
        int scrollbarX = guiX + SCROLLBAR_X_OFFSET;
        int scrollbarTop = guiY + RESULT_START_Y;
        int scrollbarHeight = MAX_VISIBLE_ROWS * RESULT_LINE_HEIGHT;
        int maxScroll = getMaxScroll();

        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarTop, 232, 0, SCROLLBAR_WIDTH, scrollbarHeight);

        if (maxScroll > 0) {
            int thumbHeight = Math.max(12, scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll * RESULT_LINE_HEIGHT));
            float scrollRatio = this.scrollOffset / maxScroll;
            int thumbY = scrollbarTop + (int) ((scrollbarHeight - thumbHeight) * scrollRatio);
            guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, thumbY, 244, 0, SCROLLBAR_WIDTH, thumbHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.mio_icif.od_scanner.result_title");
        guiGraphics.drawString(this.font, title, TITLE_X, TITLE_Y, 0x404040, false);

        int startRow = (int) this.scrollOffset;
        int maxScroll = getMaxScroll();

        enableScissorClip(guiGraphics);

        for (int row = 0; row < MAX_VISIBLE_ROWS; row++) {
            int displayRow = startRow + row;
            int y = RESULT_START_Y + row * RESULT_LINE_HEIGHT;

            for (int col = 0; col < COLUMN_COUNT; col++) {
                int lineIndex = displayRow * COLUMN_COUNT + col;
                if (lineIndex >= resultLines.size()) break;

                int x = RESULT_START_X + col * (COLUMN_WIDTH + COLUMN_GAP);
                String text = resultLines.get(lineIndex);

                int maxWidth = COLUMN_WIDTH;
                String trimmed = trimToWidth(text, maxWidth);
                guiGraphics.drawString(this.font, trimmed, x, y, 0x00FF00, false);
            }
        }

        disableScissorClip(guiGraphics);

        if (maxScroll > 0) {
            int totalLines = resultLines.size();
            int firstShown = startRow * COLUMN_COUNT + 1;
            int lastShown = Math.min((startRow + MAX_VISIBLE_ROWS) * COLUMN_COUNT, totalLines);
            String info = firstShown + "-" + lastShown + "/" + totalLines;
            guiGraphics.drawString(this.font, info, RESULT_START_X, RESULT_AREA_BOTTOM + 2, 0x808080, false);
        }
    }

    private String trimToWidth(String text, int maxWidthPx) {
        if (this.font.width(text) <= maxWidthPx) {
            return text;
        }
        while (text.length() > 0 && this.font.width(text) > maxWidthPx) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void enableScissorClip(GuiGraphics guiGraphics) {
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;
        int clipX = guiX + RESULT_START_X;
        int clipY = guiY + RESULT_START_Y;
        int clipWidth = COLUMN_COUNT * COLUMN_WIDTH + (COLUMN_COUNT - 1) * COLUMN_GAP;
        int clipHeight = MAX_VISIBLE_ROWS * RESULT_LINE_HEIGHT;
        guiGraphics.enableScissor(clipX, clipY, clipX + clipWidth, clipY + clipHeight);
    }

    private void disableScissorClip(GuiGraphics guiGraphics) {
        guiGraphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

