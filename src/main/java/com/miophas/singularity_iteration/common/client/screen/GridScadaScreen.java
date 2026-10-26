package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot.Category;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot.Device;
import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.menu.wiring.EnergyTerminalMenu;
import com.miophas.singularity_iteration.common.network.EnergyTerminalTogglePacket;
import com.miophas.singularity_iteration.common.network.GridPackets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.10.22 (G7): the monitoring panel (SCADA) of the energy management terminal, a full-screen data panel in four tabs.
 * <ul>
 *   <li><b>Overview</b>: generation, consumption, storage and balance; 112 s trends; the generation by dispatch class
 *       (must-run, fuel, storage) under the dispatch mode of the server; the demand and shortfall of critical loads; the
 *       engine time of this network's windows.</li>
 *   <li><b>Diagram</b>: a single-line diagram: one bus per sub-network (rows by cable rating), transformers, storage
 *       blocks and open breakers as links between buses, the breakers of each bus with their state.</li>
 *   <li><b>Devices</b>: every device with its flows; the load priority (critical / normal / deferrable) of loads and
 *       storage blocks is set here; generators show their dispatch class; remote on / off.</li>
 *   <li><b>Protection</b>: breakers and fuses (state, flow against the rating, thermal state, settings, reset) and the
 *       alarm log of this network.</li>
 * </ul>
 * It reads the same snapshot as the compact terminal screen (one per second) and swaps back to it without closing the
 * menu.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class GridScadaScreen extends AbstractContainerScreen<EnergyTerminalMenu> {
    private static final String K = "gui.mio_icif.grid_panel.";
    private static final int[] CATEGORY_COLOUR = {0xFF63D88A, 0xFFF5A23C, 0xFF5FD3F5, 0xFFA98BF5, 0xFFFFB347};
    private static final int ROW = 18;

    private record Region(int x, int y, int w, int h, Runnable action) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final Inventory inventory;
    private final List<Region> regions = new ArrayList<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private int tab, filter = -1, deviceScroll, alarmScroll, breakerScroll;
    private Component hover;

    public GridScadaScreen(EnergyTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
    }

    @Override
    protected void init() {
        super.init();
        imageWidth = width;
        imageHeight = height;
        leftPos = 0;
        topPos = 0;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (r.inside(mx, my) && r.action() != null) { r.action().run(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int step = (int) -Math.signum(sy);
        if (tab == 2) deviceScroll = Math.max(0, deviceScroll + step);
        if (tab == 3) {
            if (mx < width / 2.0) breakerScroll = Math.max(0, breakerScroll + step);
            else alarmScroll = Math.max(0, alarmScroll + step);
        }
        return true;
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, boolean enabled, Runnable click, int mx, int my) {
        boolean hot = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : enabled ? 0xFF1F2630 : 0xFF161B22);
        g.renderOutline(x, y, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = label.getString();
        if (font.width(text) > w - 4) text = font.plainSubstrByWidth(text, w - 6);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, enabled ? PanelCharts.TEXT : PanelCharts.DIM, false);
        if (enabled) regions.add(new Region(x, y, w, h, click));
    }

    private void send(int kind, BlockPos pos, int value) {
        PacketDistributor.sendToServer(new GridPackets.TerminalAction(menu.containerId, kind, pos, value));
    }

    private ItemStack icon(String blockId) {
        return icons.computeIfAbsent(blockId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.BLOCK.get(rl).asItem());
        });
    }

    private String name(String blockId) {
        ItemStack stack = icon(blockId);
        return stack.isEmpty() ? blockId : stack.getHoverName().getString();
    }

    private static String rate(double euPerTick) { return DspUi.compact(euPerTick) + " EU/t"; }

    private void tip(int x, int y, int w, int h, Component text, int mx, int my) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) hover = text;
    }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        g.fill(0, 0, width, height, 0xE00D1117);
        regions.clear();
        hover = null;
        EnergyNetworkSnapshot s = menu.snapshot();
        header(g, s, mx, my);
        switch (tab) {
            case 0 -> overview(g, s, mx, my);
            case 1 -> diagram(g, s, mx, my);
            case 2 -> devices(g, s, mx, my);
            default -> protection(g, s, mx, my);
        }
        if (hover != null) g.renderTooltip(font, hover, mx, my);
    }

    private void header(GuiGraphics g, EnergyNetworkSnapshot s, int mx, int my) {
        g.fill(0, 0, width, 22, 0xFF161B22);
        Component title = Component.translatable(K + "title");
        g.drawString(font, title, 8, 7, PanelCharts.ACCENT, false);
        int x = 8 + font.width(title) + 12;
        for (int i = 0; i < 4; i++) {
            Component label = Component.translatable(K + "tab." + i);
            int w = font.width(label) + 12;
            final int t = i;
            button(g, x, 4, w, 14, label, tab == i, true, () -> tab = t, mx, my);
            x += w + 3;
        }
        Component compact = Component.translatable(K + "compact");
        int cw = font.width(compact) + 12;
        button(g, width - 8 - cw, 4, cw, 14, compact, false, true,
            () -> minecraft.setScreen(new EnergyTerminalScreen(menu, inventory, getTitle())), mx, my);
        Component scope = Component.translatable(s.global ? "gui.mio_icif.energy_terminal.scope.global" : "gui.mio_icif.energy_terminal.scope.local");
        int sw = font.width(scope) + 12;
        button(g, width - 12 - cw - sw, 4, sw, 14, scope, s.global, true, () -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, s.global ? 0 : 1);
        }, mx, my);
        String mode = Component.translatable(K + "dispatch." + Math.max(0, Math.min(2, s.dispatchMode))).getString();
        PanelCharts.right(g, font, mode, width - 18 - cw - sw, 7, PanelCharts.DIM);
    }

    // ------------------------------------------------------------------ overview

    private void tile(GuiGraphics g, int x, int y, int w, int h, String label, String value, String sub, float fraction, int colour) {
        PanelCharts.panel(g, x, y, w, h);
        g.drawString(font, label, x + 5, y + 4, PanelCharts.DIM, false);
        g.drawString(font, value, x + 5, y + 15, colour, false);
        if (sub != null) g.drawString(font, font.plainSubstrByWidth(sub, w - 10), x + 5, y + 26, PanelCharts.DIM, false);
        if (fraction >= 0) PanelCharts.bar(g, x + 5, y + h - 6, w - 10, 3, fraction, colour, false);
    }

    private void overview(GuiGraphics g, EnergyNetworkSnapshot s, int mx, int my) {
        int top = 28, gap = 6, tw = (width - 12 - 3 * gap) / 4, th = 42;
        tile(g, 6, top, tw, th, Component.translatable(K + "generation").getString(), rate(s.generation),
            Component.translatable(K + "of_capacity", rate(s.generationCapacity)).getString(), s.generationRatio(), CATEGORY_COLOUR[0]);
        tile(g, 6 + tw + gap, top, tw, th, Component.translatable(K + "consumption").getString(), rate(s.consumption),
            Component.translatable(K + "of_demand", rate(s.demand)).getString(), s.demandRatio(), CATEGORY_COLOUR[1]);
        tile(g, 6 + 2 * (tw + gap), top, tw, th, Component.translatable(K + "storage").getString(),
            String.format(Locale.ROOT, "%.1f %%", s.chargeFraction() * 100), DspUi.compact(s.stored) + " / " + DspUi.compact(s.capacity) + " EU",
            s.chargeFraction(), CATEGORY_COLOUR[2]);
        float net = s.generation + s.storageOut - s.consumption - s.storageIn;
        tile(g, 6 + 3 * (tw + gap), top, tw, th, Component.translatable(K + "balance").getString(), (net >= 0 ? "+" : "") + rate(net),
            Component.translatable(K + "storage_flow", rate(s.storageIn), rate(s.storageOut)).getString(), -1, net >= 0 ? PanelCharts.up() : PanelCharts.down());
        // trends
        int cy = top + th + gap, ch = Math.max(60, height - cy - 70), cw = width * 3 / 5;
        PanelCharts.panel(g, 6, cy, cw, ch);
        g.drawString(font, Component.translatable(K + "trend"), 10, cy + 4, PanelCharts.ACCENT, false);
        float max = 1;
        for (int i = 0; i < EnergyNetworkSnapshot.HISTORY; i++) max = Math.max(max, Math.max(s.genHistory[i], s.useHistory[i]));
        int gx = 10, gy = cy + 16, gw = cw - 50, gh = ch - 22;
        DspUi.area(g, gx, gy, gw, gh, s.genHistory, max * 1.1F, CATEGORY_COLOUR[0]);
        DspUi.area(g, gx, gy, gw, gh, s.useHistory, max * 1.1F, CATEGORY_COLOUR[1]);
        DspUi.line(g, gx, gy, gw, gh, s.chargeHistory, 1F, CATEGORY_COLOUR[2]);
        PanelCharts.right(g, font, DspUi.compact(max * 1.1F), 6 + cw - 4, gy, PanelCharts.DIM);
        PanelCharts.right(g, font, "0", 6 + cw - 4, gy + gh - 8, PanelCharts.DIM);
        tip(gx, gy, gw, gh, Component.translatable(K + "trend_help"), mx, my);
        // dispatch stack, critical loads, cost
        int rx = 12 + cw, rw = width - rx - 6;
        PanelCharts.panel(g, rx, cy, rw, ch);
        int y = cy + 4;
        g.drawString(font, Component.translatable(K + "merit"), rx + 4, y, PanelCharts.ACCENT, false);
        y += 12;
        float supply = Math.max(1F, s.generationMustRun + s.generationFuel + s.storageOut);
        float[] parts = {s.generationMustRun, s.generationFuel, s.storageOut};
        int[] colours = {0xFF63D88A, 0xFFF5A23C, 0xFF5FD3F5};
        int bx = rx + 4, bw = rw - 8, at = bx;
        for (int i = 0; i < 3; i++) {
            int len = Math.round(bw * parts[i] / supply);
            if (len > 0) g.fill(at, y, Math.min(bx + bw, at + len), y + 8, colours[i]);
            at += len;
        }
        g.renderOutline(bx - 1, y - 1, bw + 2, 10, PanelCharts.PANEL_LINE);
        y += 12;
        for (int i = 0; i < 3; i++) {
            g.fill(rx + 4, y + 1, rx + 10, y + 7, colours[i]);
            g.drawString(font, Component.translatable(K + "source." + i), rx + 14, y, PanelCharts.TEXT, false);
            PanelCharts.right(g, font, rate(parts[i]), rx + rw - 4, y, PanelCharts.DIM);
            y += 10;
        }
        y += 6;
        g.drawString(font, Component.translatable(K + "critical"), rx + 4, y, PanelCharts.ACCENT, false);
        y += 12;
        boolean short_ = s.criticalShortfall > 0;
        g.drawString(font, Component.translatable(K + "critical_demand", rate(s.criticalDemand)), rx + 4, y, PanelCharts.TEXT, false);
        y += 10;
        g.drawString(font, short_ ? Component.translatable(K + "critical_short", rate(s.criticalShortfall)) : Component.translatable(K + "critical_ok"),
            rx + 4, y, short_ ? PanelCharts.down() : PanelCharts.up(), false);
        y += 16;
        g.drawString(font, Component.translatable(K + "cost"), rx + 4, y, PanelCharts.ACCENT, false);
        y += 12;
        g.drawString(font, Component.translatable(K + "cost_value", String.format(Locale.ROOT, "%.1f", s.costMicros)), rx + 4, y, PanelCharts.TEXT, false);
        y += 11;
        if (y + 16 < cy + ch) {
            long[] spark = new long[EnergyNetworkSnapshot.HISTORY];
            for (int i = 0; i < spark.length; i++) spark[i] = Math.round(s.costHistory[i] * 10);
            PanelCharts.sparkline(g, rx + 4, y, rw - 8, Math.min(24, cy + ch - y - 4), spark);
            tip(rx + 4, y, rw - 8, 24, Component.translatable(K + "cost_help"), mx, my);
        }
        // sub-networks or largest consumers below
        int by = cy + ch + gap, bh = height - by - 6;
        PanelCharts.panel(g, 6, by, width - 12, bh);
        g.drawString(font, Component.translatable(K + "subnets", s.subnets.size(), s.conductors), 10, by + 4, PanelCharts.ACCENT, false);
        int col = 0, colW = Math.max(140, (width - 20) / 4);
        int sy = by + 16;
        for (int i = 0; i < s.subnets.size() && sy + 10 <= by + bh; i++) {
            var n = s.subnets.get(i);
            int sx = 10 + col * colW;
            String text = "#" + (i + 1) + " " + rate(n.generation) + " / " + rate(n.consumption) + "  " + com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(n.ratedPacket);
            g.drawString(font, font.plainSubstrByWidth(text, colW - 6), sx, sy, PanelCharts.TEXT, false);
            if (++col * colW + colW > width - 12) { col = 0; sy += 10; }
        }
    }

    // ------------------------------------------------------------------ single-line diagram

    private void diagram(GuiGraphics g, EnergyNetworkSnapshot s, int mx, int my) {
        int top = 28, bottom = height - 6;
        PanelCharts.panel(g, 6, top, width - 12, bottom - top);
        int n = s.subnets.size();
        if (n == 0) {
            g.drawCenteredString(font, Component.translatable(K + "no_network"), width / 2, height / 2, PanelCharts.DIM);
            return;
        }
        // rows by cable rating, highest first
        List<Long> ratings = new ArrayList<>();
        for (var sn : s.subnets) if (!ratings.contains(sn.ratedPacket)) ratings.add(sn.ratedPacket);
        ratings.sort((a, b) -> Long.compare(b, a));
        int rows = ratings.size();
        int[] rowCount = new int[rows], rowOf = new int[n], colOf = new int[n];
        for (int i = 0; i < n; i++) {
            int r = ratings.indexOf(s.subnets.get(i).ratedPacket);
            rowOf[i] = r;
            colOf[i] = rowCount[r]++;
        }
        int areaH = bottom - top - 20, rowH = Math.max(36, areaH / Math.max(1, rows));
        int[] bx = new int[n], by = new int[n], bw = new int[n];
        for (int i = 0; i < n; i++) {
            int r = rowOf[i], count = rowCount[r];
            int slot = (width - 40) / count;
            bw[i] = Math.max(30, slot - 24);
            bx[i] = 20 + colOf[i] * slot + (slot - bw[i]) / 2;
            by[i] = top + 22 + r * rowH;
        }
        // links: from the device's bus to the other bus, through a symbol
        for (var link : s.links) {
            if (link.device() < 0 || link.device() >= s.devices.size() || link.subnet() < 0 || link.subnet() >= n) continue;
            Device d = s.devices.get(link.device());
            int a = d.subnet(), b = link.subnet();
            if (a < 0 || a >= n) continue;
            int ax = bx[a] + bw[a] / 2, ay = by[a], bx2 = bx[b] + bw[b] / 2, by2 = by[b];
            int midY = (ay + by2) / 2;
            int colour = d.category() == Category.PROTECTION ? PanelCharts.down() : CATEGORY_COLOUR[Math.min(4, d.category().ordinal())];
            PanelCharts.segment(g, ax, ay, ax, midY, colour);
            PanelCharts.segment(g, ax, midY, bx2, midY, colour);
            PanelCharts.segment(g, bx2, midY, bx2, by2, colour);
            int sx = (ax + bx2) / 2 - 5, sy = midY - 5;
            g.fill(sx, sy, sx + 10, sy + 10, 0xFF0D1117);
            g.renderOutline(sx, sy, 10, 10, colour);
            String glyph = switch (d.category()) { case TRANSFORMER -> "T"; case STORAGE -> "S"; case PROTECTION -> "X"; default -> "?"; };
            g.drawString(font, glyph, sx + 3, sy + 1, colour, false);
            tip(sx, sy, 10, 10, Component.literal(name(d.blockId()) + "  " + d.pos().toShortString()), mx, my);
        }
        // buses
        for (int i = 0; i < n; i++) {
            var sn = s.subnets.get(i);
            float load = sn.generation + sn.storageOut <= 0 ? 0 : Math.min(1F, sn.consumption / Math.max(1F, sn.generation + sn.storageOut));
            int colour = sn.demand > 0 && sn.consumption < sn.demand * 0.95F ? PanelCharts.WARN : PanelCharts.ACCENT;
            g.fill(bx[i], by[i] - 1, bx[i] + bw[i], by[i] + 2, colour);
            String label = "#" + (i + 1) + " " + com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(sn.ratedPacket);
            g.drawString(font, font.plainSubstrByWidth(label, bw[i]), bx[i], by[i] - 11, PanelCharts.TEXT, false);
            String flow = rate(sn.generation) + " → " + rate(sn.consumption);
            DspUi.small(g, font, font.plainSubstrByWidth(flow, (int) (bw[i] / 0.75F)), bx[i], by[i] + 5, PanelCharts.DIM);
            // breakers of the bus
            int k = 0;
            for (var d : s.devices) {
                if (d.subnet() != i || d.category() != Category.PROTECTION) continue;
                var breaker = breaker(s, d.pos());
                int c = breaker == null ? PanelCharts.DIM : breaker.state() == 0 ? PanelCharts.up() : breaker.state() == 1 ? PanelCharts.WARN : PanelCharts.down();
                int kx = bx[i] + 2 + k * 9;
                if (kx + 7 > bx[i] + bw[i]) break;
                g.fill(kx, by[i] + 13, kx + 7, by[i] + 20, c);
                tip(kx, by[i] + 13, 7, 7, Component.literal(name(d.blockId()) + " " + d.pos().toShortString()), mx, my);
                k++;
            }
            tip(bx[i], by[i] - 12, bw[i], 16, Component.translatable(K + "bus_tip", i + 1, sn.conductors, sn.devices,
                rate(sn.generation), rate(sn.consumption), rate(sn.demand), String.format(Locale.ROOT, "%.0f%%", load * 100)), mx, my);
        }
        g.drawString(font, Component.translatable(K + "legend"), 10, top + 4, PanelCharts.DIM, false);
    }

    private static EnergyNetworkSnapshot.Breaker breaker(EnergyNetworkSnapshot s, BlockPos pos) {
        for (var b : s.breakers) if (b.pos().equals(pos)) return b;
        return null;
    }

    // ------------------------------------------------------------------ devices

    private void devices(GuiGraphics g, EnergyNetworkSnapshot s, int mx, int my) {
        int top = 28, bottom = height - 6;
        int x = 6;
        String[] keys = {"all", "generators", "consumers", "storage", "protection"};
        int[] ids = {-1, 0, 1, 2, 4};
        int fx = x;
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable(K + "filter." + keys[i]);
            int w = font.width(label) + 12;
            final int id = ids[i];
            button(g, fx, top, w, 14, label, filter == id, true, () -> { filter = id; deviceScroll = 0; }, mx, my);
            fx += w + 3;
        }
        int ty = top + 18;
        PanelCharts.panel(g, x, ty, width - 12, bottom - ty);
        List<Device> rows = new ArrayList<>();
        for (var d : s.devices) if (filter < 0 || d.category().ordinal() == filter) rows.add(d);
        rows.sort((a, b) -> Float.compare(Math.max(b.input(), b.output()), Math.max(a.input(), a.output())));
        int visible = (bottom - ty - 16) / ROW;
        deviceScroll = Math.min(deviceScroll, Math.max(0, rows.size() - visible));
        int cName = x + 24, cPrio = width * 45 / 100, cFlow = width * 58 / 100, cRated = width * 72 / 100, cVolt = width * 82 / 100;
        g.drawString(font, Component.translatable(K + "col.device"), cName, ty + 4, PanelCharts.DIM, false);
        g.drawString(font, Component.translatable(K + "col.priority"), cPrio, ty + 4, PanelCharts.DIM, false);
        g.drawString(font, Component.translatable(K + "col.flow"), cFlow, ty + 4, PanelCharts.DIM, false);
        g.drawString(font, Component.translatable(K + "col.rated"), cRated, ty + 4, PanelCharts.DIM, false);
        g.drawString(font, Component.translatable(K + "col.voltage"), cVolt, ty + 4, PanelCharts.DIM, false);
        for (int i = 0; i < visible && i + deviceScroll < rows.size(); i++) {
            Device d = rows.get(i + deviceScroll);
            int y = ty + 16 + i * ROW;
            if (i % 2 == 0) g.fill(x + 1, y, width - 7, y + ROW, 0x10FFFFFF);
            g.fill(x + 1, y, x + 3, y + ROW, CATEGORY_COLOUR[Math.min(4, d.category().ordinal())]);
            g.renderItem(icon(d.blockId()), x + 5, y + 1);
            g.drawString(font, font.plainSubstrByWidth(name(d.blockId()), cPrio - cName - 6), cName, y + 5, d.disabled() ? PanelCharts.DIM : PanelCharts.TEXT, false);
            tip(cName, y, cPrio - cName, ROW, Component.literal(name(d.blockId()) + "  " + d.pos().toShortString()), mx, my);
            if (d.priority() >= 0) {
                final int next = (d.priority() + 1) % 3;
                int colour = d.priority() == 0 ? PanelCharts.down() : d.priority() == 2 ? PanelCharts.DIM : PanelCharts.TEXT;
                Component label = Component.translatable(K + "priority." + d.priority());
                button(g, cPrio, y + 2, cFlow - cPrio - 8, 14, label, false, true,
                    () -> send(GridPackets.SET_PRIORITY, d.pos(), next), mx, my);
                if (d.priority() == 0) g.renderOutline(cPrio, y + 2, cFlow - cPrio - 8, 14, colour);
            } else if (d.dispatch() >= 0) {
                g.drawString(font, Component.translatable(K + "class." + d.dispatch()), cPrio + 2, y + 5,
                    d.dispatch() == 1 ? 0xFFF5A23C : 0xFF63D88A, false);
            }
            String flow = d.category() == Category.GENERATOR ? rate(d.output())
                : d.category() == Category.STORAGE ? "+" + DspUi.compact(d.input()) + " / -" + DspUi.compact(d.output()) : rate(d.input());
            if (d.category() == Category.PROTECTION) {
                var b = breaker(s, d.pos());
                flow = b == null ? "-" : rate(b.flow());
            }
            g.drawString(font, flow, cFlow, y + 5, PanelCharts.TEXT, false);
            g.drawString(font, d.category() == Category.STORAGE && d.capacity() > 0
                ? String.format(Locale.ROOT, "%.0f %%", 100.0 * d.stored() / d.capacity()) : rate(d.rated()), cRated, y + 5, PanelCharts.DIM, false);
            g.drawString(font, d.voltage() > 0 ? DspUi.compact(d.voltage()) + " " + com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(d.voltage()) : "-", cVolt, y + 5, PanelCharts.DIM, false);
            if (d.switchable()) {
                button(g, width - 52, y + 2, 40, 14, Component.translatable(d.disabled() ? K + "off" : K + "on"), !d.disabled(), true,
                    () -> PacketDistributor.sendToServer(new EnergyTerminalTogglePacket(menu.containerId, d.pos())), mx, my);
            }
        }
        if (rows.size() > visible) PanelCharts.right(g, font, (deviceScroll + 1) + "-" + Math.min(rows.size(), deviceScroll + visible) + " / " + rows.size(),
            width - 10, ty + 4, PanelCharts.DIM);
    }

    // ------------------------------------------------------------------ protection and alarms

    private void protection(GuiGraphics g, EnergyNetworkSnapshot s, int mx, int my) {
        int top = 28, bottom = height - 6, half = (width - 18) / 2;
        PanelCharts.panel(g, 6, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "breakers", s.breakers.size()), 10, top + 4, PanelCharts.ACCENT, false);
        int rowH = 30, visible = (bottom - top - 18) / rowH;
        breakerScroll = Math.min(breakerScroll, Math.max(0, s.breakers.size() - visible));
        for (int i = 0; i < visible && i + breakerScroll < s.breakers.size(); i++) {
            var b = s.breakers.get(i + breakerScroll);
            int y = top + 16 + i * rowH;
            if (i % 2 == 0) g.fill(7, y, 6 + half - 1, y + rowH, 0x10FFFFFF);
            String state = Component.translatable("gui.mio_icif.protection.state." + new String[] {"closed", "tripped", "locked_out", "blown"}[Math.min(3, b.state())]).getString();
            int colour = b.state() == 0 ? PanelCharts.up() : b.state() == 1 ? PanelCharts.WARN : PanelCharts.down();
            String kind = Component.translatable(b.kind() == 1 ? "block.mio_icif.wiring.block_eu_fuse_cable" : "block.mio_icif.wiring.block_eu_breaker_cable").getString();
            g.drawString(font, font.plainSubstrByWidth(kind + "  " + b.pos().toShortString(), half - 90), 10, y + 2, PanelCharts.TEXT, false);
            PanelCharts.right(g, font, state, 6 + half - 6, y + 2, colour);
            float ratio = b.ratedFlow() <= 0 ? 0 : b.flow() / b.ratedFlow();
            int barW = half - 16;
            PanelCharts.bar(g, 10, y + 13, barW, 4, Math.min(1F, ratio / 2F), ratio > 1 ? PanelCharts.down() : PanelCharts.up(), false);
            g.fill(10 + barW / 2, y + 12, 11 + barW / 2, y + 18, PanelCharts.TEXT);   // the rated flow mark (scale 0..2x)
            if (b.heat() > 0) PanelCharts.bar(g, 10, y + 18, barW, 1, b.heat(), PanelCharts.WARN, false);
            String settings = Component.translatable(K + "settings", rate(b.flow()), rate(b.ratedFlow()),
                b.ratedVoltage() > 0 ? DspUi.compact(b.ratedVoltage()) + " EU" : "-", String.format(Locale.ROOT, "%.2f", b.delay() / 20.0)).getString();
            DspUi.small(g, font, font.plainSubstrByWidth(settings, (int) ((half - 70) / 0.75F)), 10, y + 21, PanelCharts.DIM);
            if (b.kind() == 0 && b.state() != 0) {
                button(g, 6 + half - 54, y + 15, 48, 13, Component.translatable("gui.mio_icif.protection.reset"), false, true,
                    () -> send(GridPackets.RESET_PROTECTION, b.pos(), 0), mx, my);
            }
            tip(10, y, half - 70, rowH, Component.translatable(K + "breaker_tip", b.trips(), Component.translatable(
                "gui.mio_icif.protection.cause." + new String[] {"none", "overload", "short_circuit", "overvoltage"}[Math.min(3, b.cause())]),
                b.reclose() ? Component.translatable("gui.mio_icif.protection.reclose_on") : Component.translatable("gui.mio_icif.protection.reclose_off")), mx, my);
        }
        if (s.breakers.isEmpty()) g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "no_breakers").getString(), half - 10),
            10, top + 18, PanelCharts.DIM, false);
        int x = 12 + half;
        PanelCharts.panel(g, x, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "alarms", s.alarms.size()), x + 4, top + 4, PanelCharts.ACCENT, false);
        int lineH = 20, lines = (bottom - top - 18) / lineH;
        alarmScroll = Math.min(alarmScroll, Math.max(0, s.alarms.size() - lines));
        String[] types = {"trip", "reclose", "lockout", "reset", "fuse", "melt", "insulation", "explosion", "shortfall"};
        for (int i = 0; i < lines && i + alarmScroll < s.alarms.size(); i++) {
            var a = s.alarms.get(i + alarmScroll);
            int y = top + 16 + i * lineH;
            String type = types[Math.min(types.length - 1, a.type())];
            int colour = switch (type) { case "reclose", "reset" -> PanelCharts.up(); case "trip", "fuse", "shortfall" -> PanelCharts.WARN; default -> PanelCharts.down(); };
            long ago = Math.max(0, s.gameTime - a.time()) / 20;
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "alarm." + type).getString(), half - 70), x + 4, y, colour, false);
            PanelCharts.right(g, font, Component.translatable(K + "ago", DspUi.duration(ago)).getString(), x + half - 4, y, PanelCharts.DIM);
            String detail = a.pos().toShortString();
            if (type.equals("trip") || type.equals("lockout") || type.equals("fuse"))
                detail += " · " + Component.translatable("gui.mio_icif.protection.cause." + new String[] {"none", "overload", "short_circuit", "overvoltage"}[Math.min(3, a.detail())]).getString()
                    + (a.value() > 0 ? " · " + (a.detail() == 3 ? DspUi.compact(a.value()) + " EU" : rate(a.value())) : "");
            else if (type.equals("melt") || type.equals("insulation")) detail += " · " + Component.translatable(K + "count", a.detail()).getString();
            else if (type.equals("shortfall")) detail += " · " + rate(a.value());
            else if (type.equals("explosion") && a.value() > 0) detail += " · " + DspUi.compact(a.value()) + " EU";
            DspUi.small(g, font, font.plainSubstrByWidth(detail, (int) ((half - 8) / 0.75F)), x + 4, y + 10, PanelCharts.DIM);
        }
        if (s.alarms.isEmpty()) g.drawString(font, Component.translatable(K + "no_alarms"), x + 4, top + 18, PanelCharts.DIM, false);
    }
}
