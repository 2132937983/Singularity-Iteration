package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.InductionElcMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GUI for the induction furnace block. 0.1.9.1: heat gauge (with the heat hold and insulation
 * state) above the slots, and a heat hold switch in the side dock.
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_induction_elc extends mio_icif_screen<InductionElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_adv_induction.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 标准工作进度条位置
    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 35;

    private static final int ENERGY_BAR_X = 44;
    private static final int ENERGY_BAR_Y = 58;

    /** 0.1.9.1 heat gauge: label at HEAT_X, bar from BAR_X to BAR_X + BAR_W. */
    private static final int HEAT_X = 37, BAR_X = 60, HEAT_Y = 22, BAR_W = 91, BAR_H = 6;

    public mio_icif_gui_induction_elc(InductionElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        InductionElcMenu menu = this.menu;

        // 使用基类标准工作进度条方法
        int progress = menu.getProgress1();
        int maxProgress = menu.getMaxProgress1();
        int progressPixels = maxProgress > 0 ? (progress * ARROW_WIDTH) / maxProgress : 0;
        drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

        drawModernEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, menu.getEnergy(), menu.getMaxEnergy());
        drawHeatGauge(guiGraphics, x, y);
    }

    /**
     * Heat gauge: a segmented bar that goes from dull red through orange to white-hot, a glowing
     * tip, a hold marker at 100 % (cyan when the heat hold is on) and an insulation frame (amber)
     * with a Thermal Retention Unit.
     */
    private void drawHeatGauge(GuiGraphics g, int x, int y) {
        int heat = Mth.clamp(menu.getHeatPercent(), 0, 100);
        boolean hold = menu.isHeatHold(), insulated = menu.isInsulated();
        long t = System.currentTimeMillis();
        DspUi.small(g, this.font, "HEAT", x + HEAT_X, y + HEAT_Y + 1, heat >= 100 ? 0xFFB3471A : 0xFF4A5560);
        int bx = x + BAR_X, by = y + HEAT_Y;
        // frame: amber when insulated
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, insulated ? 0xFFC98A2B : 0xFF3A4048);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF14181D);
        int fill = heat * BAR_W / 100;
        for (int i = 0; i < fill; i++) {
            float k = i / (float) BAR_W;
            int c = heatColour(k);
            g.fill(bx + i, by, bx + i + 1, by + BAR_H, c);
        }
        // shimmer on a full, held bar
        if (heat >= 100 && hold) {
            int sweep = (int) ((t / 12) % (BAR_W + 20)) - 10;
            for (int i = Math.max(0, sweep - 6); i < Math.min(BAR_W, sweep + 6); i++) {
                int a = (int) (90 * (1 - Math.abs(i - sweep) / 6F));
                g.fill(bx + i, by, bx + i + 1, by + BAR_H, (a << 24) | 0xFFFFFF);
            }
        }
        // tick marks every 10 %
        for (int i = 1; i < 10; i++) g.fill(bx + i * BAR_W / 10, by + BAR_H - 2, bx + i * BAR_W / 10 + 1, by + BAR_H, 0x66000000);
        // glowing tip
        if (fill > 0 && fill < BAR_W) {
            float pulse = 0.6F + 0.4F * Mth.sin(t / 120F);
            g.fill(bx + fill - 1, by - 1, bx + fill + 1, by + BAR_H + 1, ((int) (pulse * 255) << 24) | 0xFFE7B0);
        }
        // hold marker
        int marker = hold ? (0xFF000000 | (int) (0x40 + 0x30 * (1 + Mth.sin(t / 200F))) << 8 | 0x50E6FF) : 0xFF59626B;
        g.fill(bx + BAR_W + 2, by - 1, bx + BAR_W + 4, by + BAR_H + 1, hold ? 0xFF50E6FF : marker);
        String pct = heat + "%";
        DspUi.small(g, this.font, pct, bx + BAR_W - DspUi.smallWidth(this.font, pct) - 1, by - 7, heat >= 100 ? 0xFFE0571F : 0xFF4A5560);
        if (hold) DspUi.small(g, this.font, "HOLD", bx, by - 7, 0xFF1C8FB3);
        if (insulated) DspUi.small(g, this.font, "INS", bx + 22, by - 7, 0xFFB07A1E);
    }

    /** Heat colour along the bar: dark red, orange, yellow, white-hot. */
    private static int heatColour(float k) {
        int r, gr, b;
        if (k < 0.4F) { float u = k / 0.4F; r = (int) (120 + 110 * u); gr = (int) (20 + 50 * u); b = 18; }
        else if (k < 0.75F) { float u = (k - 0.4F) / 0.35F; r = 230 + (int) (20 * u); gr = (int) (70 + 120 * u); b = (int) (18 + 10 * u); }
        else { float u = (k - 0.75F) / 0.25F; r = 250; gr = (int) (190 + 60 * u); b = (int) (28 + 200 * u); }
        return 0xFF000000 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(gr, 0, 255) << 8 | Mth.clamp(b, 0, 255);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        InductionElcMenu menu = this.menu;

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            int progress = menu.getProgress1();
            int maxProgress = menu.getMaxProgress1();
            guiGraphics.renderTooltip(this.font,
                Component.literal("Progress: " + progress + "/" + maxProgress),
                mouseX - x, mouseY - y);
        }

        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        if (isHovering(mouseX, mouseY, x + HEAT_X, y + HEAT_Y - 8, BAR_X - HEAT_X + BAR_W + 6, BAR_H + 10)) {
            guiGraphics.renderComponentTooltip(this.font, heatTooltip(), mouseX - x, mouseY - y);
        }
    }

    private List<Component> heatTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.mio_icif.induction.heat", menu.getHeatPercent()));
        lines.add(Component.translatable(menu.isHeatHold() ? "gui.mio_icif.induction.hold_on" : "gui.mio_icif.induction.hold_off")
            .withStyle(menu.isHeatHold() ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        int seconds = menu.secondsToFullHeat();
        if (seconds > 0 && menu.getHeatPercent() < 100) {
            lines.add(Component.translatable("gui.mio_icif.induction.eta", seconds / 60, String.format("%02d", seconds % 60)).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(menu.isInsulated() ? "gui.mio_icif.induction.insulated" : "gui.mio_icif.induction.not_insulated")
            .withStyle(menu.isInsulated() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    protected void init() {
        super.init();
        addXpDockButton(() -> this.menu.getStoredXpTenths());
        // 0.1.9.1: heat hold switch
        addDockButton(new DockButton((g, bx, by, hovered) -> {
            boolean on = this.menu.isHeatHold();
            int cx = bx + 8, cy = by + 8;
            long t = System.currentTimeMillis();
            // ring
            int ring = on ? 0xFF1C8FB3 : 0xFF7C848C;
            g.fill(cx - 6, cy - 6, cx + 7, cy - 5, ring);
            g.fill(cx - 6, cy + 6, cx + 7, cy + 7, ring);
            g.fill(cx - 6, cy - 5, cx - 5, cy + 6, ring);
            g.fill(cx + 6, cy - 5, cx + 7, cy + 6, ring);
            // flame
            float flick = on ? 0.85F + 0.15F * Mth.sin(t / 90F) : 1F;
            int outer = on ? 0xFFF2731D : 0xFF9AA0A6, inner = on ? 0xFFFFE08A : 0xFFC8CCD0;
            int h = Math.round(7 * flick);
            for (int i = 0; i < h; i++) {
                int w = Math.max(1, (int) Math.round(3.2 * Math.sin(Math.PI * (i + 1) / (h + 1))));
                g.fill(cx - w, cy + 3 - i, cx + w + 1, cy + 4 - i, outer);
            }
            g.fill(cx - 1, cy + 1, cx + 2, cy + 4, inner);
        }, () -> List.of(
            Component.translatable("gui.mio_icif.induction.hold_title"),
            Component.translatable(this.menu.isHeatHold() ? "gui.mio_icif.induction.hold_on" : "gui.mio_icif.induction.hold_off")
                .withStyle(this.menu.isHeatHold() ? ChatFormatting.AQUA : ChatFormatting.GRAY),
            Component.translatable("gui.mio_icif.induction.hold_tip").withStyle(ChatFormatting.DARK_GRAY)), () -> {
            playClick();
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, InductionElcMenu.BUTTON_HEAT_HOLD);
            }
        }));
    }
}
