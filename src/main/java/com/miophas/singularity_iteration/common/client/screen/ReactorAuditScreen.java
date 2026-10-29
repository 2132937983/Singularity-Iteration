// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorAccidentAnalyzer;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox.Melt;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox.Sample;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox.Slot;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.10.12: the reactor accident audit report. Opened by {@code /mio_icif reactor audit <id>} or by
 * using the blackbox item.
 * <ul>
 *   <li>Header: incident number, kind (nuclear blast, local explosion, contained meltdown, removal),
 *       date and time, game day, dimension and position, owner and the players nearby.</li>
 *   <li>Verdict: the main cause with its explanation, then the contributing factors.</li>
 *   <li>Core layout: the slots at the last cycle; melted parts are shown where they were (red, crossed),
 *       worn-out reflectors orange, spent fuel grey; bars show the heat of a part or the fuel left; the
 *       columns the reactor did not have are dark. Every slot has a tooltip.</li>
 *   <li>Heat graph: the hull heat (percent of its limit) over the last cycles, with the heat made and the
 *       heat taken away per cycle as bars; a tooltip per cycle.</li>
 *   <li>Metrics of the last cycle.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ReactorAuditScreen extends Screen {
    private static final int W = 416, H = 252, VERDICT_H = 76;
    private static final int GRID_X = 8, GRID_Y = 134, CELL = 18;
    private static final int GRAPH_X = 184, GRAPH_Y = 136, GRAPH_W = 224, GRAPH_H = 74;
    private static final int RED = 0xFFC8423C, ORANGE = 0xFFD2902A, BLUE = 0xFF3A6EA5;

    private final CompoundTag incident;
    private final List<Sample> samples;
    private final Slot[] layout;
    private final List<Melt> melts;
    private final List<Component[]> findings = new ArrayList<>();   // {title, description}
    private final ItemStack[] icons = new ItemStack[54];
    private final ItemStack[] ghosts = new ItemStack[54];
    private final Slot[] ghostSlots = new Slot[54];
    @org.jetbrains.annotations.Nullable private final Component factors;   // "Contributing factors: ..." or null
    private int left, top, columns;

    public ReactorAuditScreen(CompoundTag incident) {
        super(Component.translatable("gui.mio_icif.reactor_audit.title", incident.getInt("Id")));
        this.incident = incident;
        this.samples = ReactorBlackbox.loadSamples(incident.getList("Samples", Tag.TAG_COMPOUND));
        this.layout = ReactorBlackbox.loadLayout(incident.getList("Layout", Tag.TAG_COMPOUND));
        this.melts = ReactorBlackbox.loadMelts(incident.getList("Melts", Tag.TAG_COMPOUND));
        ListTag list = incident.getList("Findings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var f = list.getCompound(i);
            String code = f.getString("Code");
            var raw = f.getList("Args", Tag.TAG_STRING);
            Object[] args = new Object[raw.size()];
            for (int a = 0; a < raw.size(); a++) args[a] = argument(code, a, raw.getString(a));
            findings.add(new Component[] {Component.translatable("reactor_audit.cause." + code),
                Component.translatable("reactor_audit.cause." + code + ".desc", args)});
        }
        if (findings.size() > 1) {
            MutableComponent all = Component.empty();
            for (int i = 1; i < findings.size(); i++) {
                if (i > 1) all.append(Component.translatable("gui.mio_icif.reactor_audit.separator"));
                all.append(findings.get(i)[0]);
            }
            factors = Component.translatable("gui.mio_icif.reactor_audit.factors", all);
        } else factors = null;
        columns = samples.isEmpty() ? 9 : samples.get(samples.size() - 1).columns();
        for (int i = 0; i < 54; i++) icons[i] = stack(layout[i].item());
        for (var m : melts) if (m.slot() >= 0 && m.slot() < 54 && layout[m.slot()].empty()) {
            ghosts[m.slot()] = stack(m.component().item());
            ghostSlots[m.slot()] = m.component();
        }
    }

    /** Localises the coded arguments: the kinds of melted parts, the reason the liquid cooling stopped. */
    private static Object argument(String code, int index, String value) {
        if (code.equals("component_melted") && index == 1) {
            MutableComponent out = Component.empty();
            boolean first = true;
            for (String part : value.split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                if (!first) out.append(Component.translatable("gui.mio_icif.reactor_audit.separator"));
                out.append(Component.translatable("reactor_audit.kind." + kv[0])).append(" ×" + kv[1]);
                first = false;
            }
            return out;
        }
        if (code.equals("liquid_cooling_lost") && index == 1) return Component.translatable("reactor_audit.detail." + value);
        return value;
    }

    private static ItemStack stack(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        var key = ResourceLocation.tryParse(id);
        return key == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(new SiButton(left + W - 58, top + H - 18, 50, 14, Component.translatable("gui.mio_icif.reactor_audit.close"), b -> onClose()));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left, y = top;
        // header
        g.fillGradient(x + 1, y + 1, x + W - 1, y + 30, 0xFF7A1F1B, 0xFF4A1210);
        g.drawString(font, title, x + 8, y + 5, 0xFFFFFFFF, false);
        Component kind = Component.translatable("reactor_audit.type." + incident.getString("Type"));
        int kw = font.width(kind) + 8;
        g.fill(x + W - 8 - kw, y + 4, x + W - 8, y + 15, 0xFFFFD27A);
        g.drawString(font, kind, x + W - 4 - kw, y + 6, 0xFF4A1210, false);
        String when = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(incident.getLong("Time")));
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.reactor_audit.where", when, incident.getLong("Day"),
            incident.getString("Dim"), incident.getInt("X") + " " + incident.getInt("Y") + " " + incident.getInt("Z")),
            x + 8, y + 18, W - 16, 0xFFF2DDDA, false);
        // owner and witnesses
        String owner = incident.getString("OwnerName");
        ListTag nearby = incident.getList("Nearby", Tag.TAG_STRING);
        var names = new StringBuilder();
        for (int i = 0; i < nearby.size(); i++) names.append(i == 0 ? "" : ", ").append(nearby.getString(i));
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.reactor_audit.people",
            owner.isEmpty() ? Component.translatable("gui.mio_icif.reactor_audit.unknown") : Component.literal(owner),
            names.length() == 0 ? Component.translatable("gui.mio_icif.reactor_audit.nobody") : Component.literal(names.toString())),
            x + 8, y + 34, W - 16, SiGuiTheme.TEXT, false);
        // verdict: the main cause, its text and the contributing factors in six lines (factors at most two)
        SiGuiTheme.well(g, x + 6, y + 45, W - 12, VERDICT_H);
        if (!findings.isEmpty()) {
            var main = findings.get(0);
            g.drawString(font, Component.translatable("gui.mio_icif.reactor_audit.main", main[0]).withStyle(ChatFormatting.BOLD), x + 11, y + 49, RED, false);
            List<FormattedCharSequence> factorLines = factors == null ? List.of() : font.split(factors, W - 24);
            int factorCount = Math.min(2, factorLines.size());
            List<FormattedCharSequence> text = font.split(main[1], W - 24);
            int ly = y + 60;
            for (int i = 0; i < Math.min(6 - factorCount, text.size()); i++, ly += 10)
                g.drawString(font, text.get(i), x + 11, ly, SiGuiTheme.TEXT_SCREEN, false);
            for (int i = 0; i < factorCount; i++, ly += 10)
                g.drawString(font, factorLines.get(i), x + 11, ly, ORANGE, false);
        }
        // layout
        g.drawString(font, Component.translatable("gui.mio_icif.reactor_audit.layout"), x + GRID_X, y + GRID_Y - 10, SiGuiTheme.TEXT, false);
        renderLayout(g, x + GRID_X, y + GRID_Y);
        // graph and metrics
        g.drawString(font, Component.translatable("gui.mio_icif.reactor_audit.graph"), x + GRAPH_X, y + GRID_Y - 10, SiGuiTheme.TEXT, false);
        renderGraph(g, x + GRAPH_X, y + GRAPH_Y);
        renderMetrics(g, x + GRAPH_X, y + GRAPH_Y + GRAPH_H + 4);
        renderTooltips(g, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        SiGuiTheme.panel(g, left, top, W, H);
    }

    private void renderLayout(GuiGraphics g, int gx, int gy) {
        for (int i = 0; i < 54; i++) {
            int cx = gx + (i % 9) * CELL, cy = gy + (i / 9) * CELL;
            if (i % 9 >= columns) { g.fill(cx, cy, cx + 17, cy + 17, 0xFF4A4E53); continue; }
            SiGuiTheme.slot(g, cx, cy);
            Slot slot = layout[i];
            if (!icons[i].isEmpty()) {
                g.renderItem(icons[i], cx + 1, cy + 1);
                if ((slot.flags() & ReactorBlackbox.FLAG_DEPLETED) != 0) g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x80303030);
                bar(g, cx, cy, slot);
            } else if (ghosts[i] != null && !ghosts[i].isEmpty()) {
                g.renderItem(ghosts[i], cx + 1, cy + 1);
                boolean reflector = ghostSlots[i].kind() == Kind.REFLECTOR.ordinal();
                g.fill(cx + 1, cy + 1, cx + 17, cy + 17, reflector ? 0x90D2902A : 0xA0C8423C);
                g.drawString(font, "×", cx + 6, cy + 5, 0xFFFFFFFF, true);
            }
        }
    }

    /** A 2-pixel bar at the bottom of a slot: heat of a heat-storing part (green to red) or fuel left (green). */
    private static void bar(GuiGraphics g, int cx, int cy, Slot slot) {
        double fraction;
        int color;
        if (slot.kind() == Kind.FUEL.ordinal() && slot.maxRemaining() > 0) {
            fraction = slot.remaining() / (double) slot.maxRemaining();
            color = 0xFF3C9A52;
        } else if (slot.capacity() > 0 && heatKind(slot.kind())) {
            fraction = slot.stored() / (double) slot.capacity();
            int r = (int) (60 + 195 * fraction), gr = (int) (190 - 150 * fraction);
            color = 0xFF000000 | r << 16 | gr << 8 | 40;
        } else return;
        int w = (int) Math.round(15 * Math.max(0, Math.min(1, fraction)));
        g.fill(cx + 1, cy + 15, cx + 16, cy + 17, 0xFF202020);
        if (w > 0) g.fill(cx + 1, cy + 15, cx + 1 + w, cy + 17, color);
    }

    private static boolean heatKind(int kind) {
        return kind == Kind.CELL.ordinal() || kind == Kind.CONDENSATOR.ordinal() || kind == Kind.VENT.ordinal() || kind == Kind.EXCHANGER.ordinal()
            || kind == Kind.STEAM_VENT.ordinal();   // 0.1.10.36
    }

    private void renderGraph(GuiGraphics g, int gx, int gy) {
        SiGuiTheme.well(g, gx, gy, GRAPH_W, GRAPH_H);
        if (samples.isEmpty()) {
            g.drawString(font, Component.translatable("gui.mio_icif.reactor_audit.no_data"), gx + 6, gy + GRAPH_H / 2 - 4, SiGuiTheme.TEXT_SCREEN_DIM, false);
            return;
        }
        int n = samples.size();
        int plotTop = gy + 4, plotBottom = gy + GRAPH_H - 14, barsBottom = gy + GRAPH_H - 2;
        double step = (GRAPH_W - 8) / (double) Math.max(1, ReactorBlackbox.SAMPLES);
        int x0 = gx + 4 + (int) Math.round((ReactorBlackbox.SAMPLES - n) * step);
        // 100 % line
        for (int px = gx + 2; px < gx + GRAPH_W - 2; px += 4) g.fill(px, plotTop, px + 2, plotTop + 1, RED);
        long maxBar = 1;
        for (int i = 0; i < n; i++) maxBar = Math.max(maxBar, Math.max(samples.get(i).generated() + samples.get(i).excess(), removed(samples.get(i), i)));
        int prevX = -1, prevY = -1;
        for (int i = 0; i < n; i++) {
            var s = samples.get(i);
            int px = x0 + (int) Math.round(i * step);
            int barW = Math.max(1, (int) step / 2 - 1);
            int gen = (int) Math.round(10.0 * (s.generated() + s.excess()) / maxBar), rem = (int) Math.round(10.0 * removed(s, i) / maxBar);
            g.fill(px, barsBottom - gen, px + barW, barsBottom, ORANGE);
            g.fill(px + barW, barsBottom - rem, px + 2 * barW, barsBottom, BLUE);
            double fraction = s.maxHull() <= 0 ? 0 : Math.min(1.0, s.hullAfter() / (double) s.maxHull());
            int py = plotBottom - (int) Math.round((plotBottom - plotTop) * fraction);
            if (prevX >= 0) line(g, prevX, prevY, px, py, RED);
            g.fill(px - 1, py - 1, px + 1, py + 1, 0xFF7A1F1B);
            prevX = px; prevY = py;
        }
        g.drawString(font, "-" + n + "s", gx + 3, plotBottom + 1, SiGuiTheme.TEXT_SCREEN_DIM, false);
        String zero = "0s";
        g.drawString(font, zero, gx + GRAPH_W - 3 - font.width(zero), plotBottom + 1, SiGuiTheme.TEXT_SCREEN_DIM, false);
    }

    /** The heat a cycle took away: heat in minus the rise of the hull and of the parts. */
    private long removed(Sample s, int index) {
        long componentDelta = index > 0 ? s.componentHeat() - samples.get(index - 1).componentHeat() : 0;
        return Math.max(0, s.generated() + s.excess() - (s.hullAfter() - s.hullBefore()) - componentDelta);
    }

    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), steps = Math.max(dx, dy);
        for (int k = 0; k <= steps; k++) {
            int px = x1 + (steps == 0 ? 0 : (x2 - x1) * k / steps), py = y1 + (steps == 0 ? 0 : (y2 - y1) * k / steps);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private void renderMetrics(GuiGraphics g, int mx, int my) {
        if (samples.isEmpty()) return;
        var s = samples.get(samples.size() - 1);
        int idx = samples.size() - 1;
        Component first = Component.translatable("gui.mio_icif.reactor_audit.metrics1",
            percent(s.hullAfter(), s.maxHull()), s.generated() + s.excess(), removed(s, idx),
            String.format(Locale.ROOT, "%.1f", s.euPerTick()));
        Component second = s.fluidMode()
            ? Component.translatable("gui.mio_icif.reactor_audit.metrics_fluid", s.feeds(), s.columns(), s.coldIn(), s.hotOut(), s.fluidCapacity())
            : Component.translatable("gui.mio_icif.reactor_audit.metrics_standard", s.feeds(), s.columns(), s.coolCold(), s.coolHot(), s.coolUsed());
        mio_icif_screen.drawFitted(g, font, first, mx, my, GRAPH_W, SiGuiTheme.TEXT, false);
        mio_icif_screen.drawFitted(g, font, second, mx, my + 10, GRAPH_W, SiGuiTheme.TEXT, false);
        int in = 0, out = 0;
        for (var sample : samples) { in += sample.automationIn(); out += sample.automationOut(); }
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.reactor_audit.metrics3", s.fuelRods(), s.coolingParts(),
            percent(s.componentHeat(), s.componentCapacity()), in, out), mx, my + 20, GRAPH_W - 56, SiGuiTheme.TEXT, false);
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int gx = left + GRID_X, gy = top + GRID_Y;
        if (mouseX >= gx && mouseX < gx + 9 * CELL && mouseY >= gy && mouseY < gy + 6 * CELL) {
            int i = (mouseY - gy) / CELL * 9 + (mouseX - gx) / CELL;
            var lines = new ArrayList<Component>();
            if (i % 9 >= columns) lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_outside"));
            else if (!icons[i].isEmpty()) {
                lines.add(icons[i].getHoverName());
                describe(layout[i], lines);
            } else if (ghosts[i] != null && !ghosts[i].isEmpty()) {
                lines.add(ghosts[i].getHoverName());
                boolean reflector = ghostSlots[i].kind() == Kind.REFLECTOR.ordinal();
                lines.add(Component.translatable(reflector ? "gui.mio_icif.reactor_audit.slot_expired" : "gui.mio_icif.reactor_audit.slot_melted")
                    .withStyle(ChatFormatting.RED));
                long at = 0;
                for (var m : melts) if (m.slot() == i) at = m.gameTime();
                lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_when", String.format(Locale.ROOT, "%.0f",
                    Math.max(0, incident.getLong("GameTime") - at) / 20.0)).withStyle(ChatFormatting.GRAY));
            } else lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_empty"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        int px = left + GRAPH_X, py = top + GRAPH_Y;
        if (!samples.isEmpty() && mouseX >= px && mouseX < px + GRAPH_W && mouseY >= py && mouseY < py + GRAPH_H) {
            int n = samples.size();
            double step = (GRAPH_W - 8) / (double) ReactorBlackbox.SAMPLES;
            int x0 = px + 4 + (int) Math.round((ReactorBlackbox.SAMPLES - n) * step);
            int i = (int) Math.round((mouseX - x0) / step);
            if (i < 0 || i >= n) return;
            var s = samples.get(i);
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_time", String.format(Locale.ROOT, "%.0f",
                Math.max(0, incident.getLong("GameTime") - s.gameTime()) / 20.0)));
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_hull", s.hullAfter(), s.maxHull(),
                percent(s.hullAfter(), s.maxHull())).withStyle(ChatFormatting.RED));
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_heat", s.generated() + s.excess(), removed(s, i)).withStyle(ChatFormatting.GOLD));
            if (s.melted() > 0) lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_melted", s.melted()).withStyle(ChatFormatting.RED));
            if (s.automationIn() + s.automationOut() > 0)
                lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_automation", s.automationIn(), s.automationOut()));
            if (i > 0 && s.columns() < samples.get(i - 1).columns())
                lines.add(Component.translatable("gui.mio_icif.reactor_audit.cycle_columns", samples.get(i - 1).columns(), s.columns()).withStyle(ChatFormatting.RED));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private static void describe(Slot slot, List<Component> lines) {
        if (slot.kind() >= 0) lines.add(Component.translatable("reactor_audit.kind." + ReactorAccidentAnalyzer.kindName(slot.kind())).withStyle(ChatFormatting.GRAY));
        if (slot.kind() == Kind.FUEL.ordinal() && slot.maxRemaining() > 0)
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_fuel", slot.remaining(), slot.maxRemaining()));
        else if (slot.capacity() > 0 && heatKind(slot.kind()))
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_heat", slot.stored(), slot.capacity(),
                percent(slot.stored(), slot.capacity())));
        else if (slot.kind() == Kind.REFLECTOR.ordinal() && slot.capacity() > 0)
            lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_wear", slot.stored(), slot.capacity()));
        if ((slot.flags() & ReactorBlackbox.FLAG_DEPLETED) != 0) lines.add(Component.translatable("gui.mio_icif.reactor_audit.slot_depleted"));
    }

    /** Percent text, rounded as the analyzer rounds it. */
    private static String percent(long value, long capacity) {
        return capacity <= 0 ? "0" : Long.toString(Math.min(999, Math.round(value * 100.0 / capacity)));
    }
}
