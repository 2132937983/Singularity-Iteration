package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_meter_menu;
import com.miophas.singularity_iteration.common.menu.tool.mio_icif_meter_menu.MeterMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * EU meter / voltage detector, styled as a holographic oscilloscope:
 * <ul>
 *   <li>scope: the last 6 s of throughput (EU/t delivered into endpoints, cyan area) and the
 *       packet voltage (orange step line) on the network around the target;</li>
 *   <li>read-outs: voltage tier and packet size, current in amperes (packets per tick =
 *       throughput / packet voltage), average throughput, the cables' rated packet;</li>
 *   <li>the IC2 instrument (mode in / out / gain / voltage tier with average, max / min and the
 *       measuring time) and the network inventory (cables, generators, consumers, storage,
 *       transformers).</li>
 * </ul>
 * Used on a machine it measures that node (IC2 behaviour); used on a cable, special cable,
 * transformer or terminal it measures the network through it.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_eu_meter extends mio_icif_screen<mio_icif_meter_menu> {
    private static final int SX = 7, SY = 15, SW = 162, SH = 44;
    private static final int MODE_Y = 90, MODE_W = 38, MODE_H = 11;
    private static final int RESET_X = 128, RESET_Y = 117, RESET_W = 41, RESET_H = 11;

    public mio_icif_gui_eu_meter(mio_icif_meter_menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 219;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 95;
    }

    @Override
    protected boolean showUpgradeHint() { return false; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        // player inventory wells
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 135 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + 7 + c * 18, y + 193);
        // scope
        DspUi.screen(g, x + SX, y + SY, SW, SH);
        float[] tp = menu.scopeThroughput(), volt = menu.scopeVoltage();
        float maxTp = 1, maxV = 1;
        for (int i = 0; i < tp.length; i++) { maxTp = Math.max(maxTp, tp[i]); maxV = Math.max(maxV, volt[i]); }
        int cx = x + SX + 2, cy = y + SY + 9, cw = SW - 4, ch = SH - 12;
        for (int gx = 1; gx < 6; gx++) for (int gy = cy; gy < cy + ch; gy += 2) g.fill(cx + cw * gx / 6, gy, cx + cw * gx / 6 + 1, gy + 1, 0x30FFFFFF);
        DspUi.area(g, cx, cy, cw, ch, tp, maxTp * 1.15F, DspUi.CYAN);
        // voltage: step line
        float step = cw / (float) tp.length;
        int prev = -1;
        for (int i = 0; i < volt.length; i++) {
            int x0 = cx + (int) (i * step), x1 = cx + (int) ((i + 1) * step);
            int vy = cy + ch - 1 - Math.round((ch - 1) * Math.min(1F, volt[i] / (maxV * 1.15F)));
            if (prev >= 0 && prev != vy) g.fill(x0, Math.min(prev, vy), x0 + 1, Math.max(prev, vy) + 1, DspUi.ORANGE);
            g.fill(x0, vy, Math.max(x0 + 1, x1), vy + 1, DspUi.ORANGE);
            prev = vy;
        }
        // read-out panels
        DspUi.screen(g, x + 7, y + 62, 52, 25);
        DspUi.screen(g, x + 62, y + 62, 52, 25);
        DspUi.screen(g, x + 117, y + 62, 52, 25);
        DspUi.screen(g, x + 7, y + 104, 118, 25);
        // IC2 mode buttons
        MeterMode mode = menu.getMode();
        MeterMode[] modes = MeterMode.values();
        for (int i = 0; i < modes.length; i++) {
            int bx = x + 7 + i * (MODE_W + 3), by = y + MODE_Y;
            boolean on = modes[i] == mode, hot = mouseX >= bx && mouseX < bx + MODE_W && mouseY >= by && mouseY < by + MODE_H;
            g.fill(bx, by, bx + MODE_W, by + MODE_H, on ? 0xFF13212F : SiGuiTheme.OUTLINE);
            g.renderOutline(bx, by, MODE_W, MODE_H, on ? DspUi.CYAN : hot ? SiGuiTheme.ACCENT : 0xFF8A9096);
            if (!on) g.fillGradient(bx + 1, by + 1, bx + MODE_W - 1, by + MODE_H - 1, hot ? 0xFFF3F8FC : 0xFFE6E9EC, hot ? 0xFFC9DCEE : 0xFFC4C8CD);
        }
        boolean hotReset = mouseX >= x + RESET_X && mouseX < x + RESET_X + RESET_W && mouseY >= y + RESET_Y && mouseY < y + RESET_Y + RESET_H;
        g.fill(x + RESET_X, y + RESET_Y, x + RESET_X + RESET_W, y + RESET_Y + RESET_H, SiGuiTheme.OUTLINE);
        g.fillGradient(x + RESET_X + 1, y + RESET_Y + 1, x + RESET_X + RESET_W - 1, y + RESET_Y + RESET_H - 1,
            hotReset ? 0xFFF3F8FC : 0xFFE6E9EC, hotReset ? 0xFFC9DCEE : 0xFFC4C8CD);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, SiGuiTheme.TEXT, false);
        var net = menu.clientNetwork();
        String target = net == null ? "" : Component.translatable(net.conductor() ? "item.mio_icif.item_tool_meter.gui.target_network"
            : "item.mio_icif.item_tool_meter.gui.target_node").getString();
        DspUi.small(g, font, target, imageWidth - 8 - DspUi.smallWidth(font, target), 6, SiGuiTheme.TEXT);
        // scope legend
        DspUi.small(g, font, "EU/t", SX + 3, SY + 2, DspUi.CYAN);
        DspUi.small(g, font, "V", SX + 22, SY + 2, DspUi.ORANGE);
        String span = "6 s";
        DspUi.small(g, font, span, SX + SW - 3 - DspUi.smallWidth(font, span), SY + 2, DspUi.TEXT_DIM);
        // live values from the scope window
        float[] tp = menu.scopeThroughput(), volt = menu.scopeVoltage();
        int n = Math.max(1, Math.min(20, menu.scopeFilled()));
        float avgTp = 0, maxV = 0;
        for (int i = tp.length - n; i < tp.length; i++) { avgTp += tp[i]; maxV = Math.max(maxV, volt[i]); }
        avgTp /= n;
        String tier = com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(maxV);
        readout(g, 7, Component.translatable("item.mio_icif.item_tool_meter.gui.voltage").getString(), tier, maxV > 0 ? DspUi.compact(maxV) + " V" : "-", DspUi.ORANGE);
        float amps = maxV > 0 ? avgTp / maxV : 0;
        readout(g, 62, Component.translatable("item.mio_icif.item_tool_meter.gui.current").getString(), String.format("%.2f A", amps),
            Component.translatable("item.mio_icif.item_tool_meter.gui.packets").getString(), DspUi.GREEN);
        readout(g, 117, Component.translatable("item.mio_icif.item_tool_meter.gui.throughput").getString(), DspUi.compact(avgTp),
            "EU/t", DspUi.CYAN);
        // IC2 instrument
        MeterMode mode = menu.getMode();
        MeterMode[] modes = MeterMode.values();
        String[] keys = {"in", "out", "gain", "tier"};
        for (int i = 0; i < modes.length; i++) {
            String label = Component.translatable("item.mio_icif.item_tool_meter.gui.mode_short." + keys[i]).getString();
            int bx = 7 + i * (MODE_W + 3);
            DspUi.small(g, font, label, bx + (MODE_W - DspUi.smallWidth(font, label)) / 2, MODE_Y + 3, modes[i] == mode ? DspUi.CYAN : SiGuiTheme.TEXT_SCREEN);
        }
        String unit = menu.isVoltageMode() ? "" : "EU/t";
        String avg = (menu.isVoltageMode() ? "Tier " : "") + formatSI(menu.getResultAvg()) + unit;
        DspUi.small(g, font, font.plainSubstrByWidth("\u2248" + avg, 54), 128, 104, SiGuiTheme.TEXT);
        DspUi.small(g, font, font.plainSubstrByWidth("\u2191" + formatSI(menu.getResultMax()) + " \u2193" + formatSI(menu.getResultMin()), 54), 128, 110, SiGuiTheme.TEXT_SCREEN_DIM);
        String reset = Component.translatable("item.mio_icif.item_tool_meter.gui.reset").getString();
        DspUi.small(g, font, reset, RESET_X + (RESET_W - DspUi.smallWidth(font, reset)) / 2, RESET_Y + 3, SiGuiTheme.TEXT_SCREEN);
        String cycle = Component.translatable("item.mio_icif.item_tool_meter.gui.cycle", menu.getResultCount() / 20).getString();
        DspUi.small(g, font, font.plainSubstrByWidth(cycle, 70), SX + 32, SY + 2, DspUi.TEXT_DIM);
        // network inventory
        if (net != null) {
            int fit = (int) ((118 - 6) / 0.75F);
            DspUi.small(g, font, font.plainSubstrByWidth(Component.translatable("item.mio_icif.item_tool_meter.gui.network", net.conductors(), net.subnets()).getString(), fit), 10, 107, DspUi.TEXT);
            DspUi.small(g, font, font.plainSubstrByWidth(Component.translatable("item.mio_icif.item_tool_meter.gui.nodes", net.generators(), net.consumers(), net.storages(), net.transformers()).getString(), fit), 10, 114, DspUi.TEXT_DIM);
            String rated = net.ratedPacket() > 0 ? Component.translatable("item.mio_icif.item_tool_meter.gui.rated",
                com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(net.ratedPacket()), DspUi.compact(net.ratedPacket())).getString() : "";
            boolean over = net.ratedPacket() > 0 && maxV > net.ratedPacket();
            DspUi.small(g, font, font.plainSubstrByWidth(rated, fit), 10, 121, over ? DspUi.RED : DspUi.CYAN_DIM);
        } else {
            DspUi.small(g, font, Component.translatable("item.mio_icif.item_tool_meter.gui.scanning").getString(), 10, 110, DspUi.TEXT_DIM);
        }
        // no "Inventory" caption: the network panel occupies that band (slots start right below it)
    }

    private void readout(GuiGraphics g, int x, String label, String value, String sub, int color) {
        DspUi.small(g, font, label, x + 3, 64, DspUi.TEXT_DIM);
        g.drawString(font, font.plainSubstrByWidth(value, 48), x + 3, 70, color, false);
        DspUi.small(g, font, font.plainSubstrByWidth(sub, 60), x + 3, 80, DspUi.TEXT_DIM);
    }

    private String formatSI(double value) {
        if (value == 0) return "0 ";
        if (!Double.isFinite(value)) return "— ";
        // IC2 uses six significant digits, including sub-unit SI prefixes.
        int exponent = (int) Math.floor(Math.log10(Math.abs(value)) / 3) * 3;
        var scaled = java.math.BigDecimal.valueOf(value).scaleByPowerOfTen(-exponent)
            .round(new java.math.MathContext(6));
        if (scaled.abs().compareTo(java.math.BigDecimal.valueOf(1000)) >= 0) {
            exponent += 3;
            scaled = scaled.scaleByPowerOfTen(-3);
        }
        String prefix = switch (exponent) {
            case -24 -> "y"; case -21 -> "z"; case -18 -> "a"; case -15 -> "f";
            case -12 -> "p"; case -9 -> "n"; case -6 -> "µ"; case -3 -> "m";
            case 0 -> ""; case 3 -> "k"; case 6 -> "M"; case 9 -> "G";
            case 12 -> "T"; case 15 -> "P"; case 18 -> "E"; case 21 -> "Z"; case 24 -> "Y";
            default -> "E" + exponent;
        };
        return scaled.stripTrailingZeros().toPlainString() + " " + prefix;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int relX = (int) mouseX - leftPos, relY = (int) mouseY - topPos;
            MeterMode[] modes = MeterMode.values();
            for (int i = 0; i < modes.length; i++) {
                int bx = 7 + i * (MODE_W + 3);
                if (relX >= bx && relX < bx + MODE_W && relY >= MODE_Y && relY < MODE_Y + MODE_H) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, modes[i].ordinal());
                    playClick();
                    return true;
                }
            }
            if (relX >= RESET_X && relX < RESET_X + RESET_W && relY >= RESET_Y && relY < RESET_Y + RESET_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // what "max ... EU/t" on the cable-rating line means
        var net = menu.clientNetwork();
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (net != null && net.ratedPacket() > 0 && rx >= 7 && rx < 125 && ry >= 119 && ry < 129)
            g.renderTooltip(font, Component.translatable("item.mio_icif.item_tool_meter.gui.rated.tip"), mouseX, mouseY);
    }
}
