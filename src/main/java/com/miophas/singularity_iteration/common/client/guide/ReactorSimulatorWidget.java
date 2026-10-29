// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.guide;

import com.miophas.singularity_iteration.common.reactor.ReactorInventory;
import com.miophas.singularity_iteration.common.reactor.ReactorSimulation;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Interactive reactor simulator of the manual (0.1.8.0). The guide page shows it through the
 * {@code <ReactorSimulator>} tag.
 *
 * <p>Use: click a component in the palette, then click reactor slots to place it. Right click
 * clears a slot. The chamber buttons set the reactor columns (3 + chambers). The values update at
 * each change from {@link ReactorSimulation} (the model of the reactor block, 100 seconds from a
 * cold start).
 */
@OnlyIn(Dist.CLIENT)
public class ReactorSimulatorWidget extends AbstractWidget {
    public static final int W = 312, H = 180;
    // 0.1.10.36: 17 per row (two rows hold the 33 components; 4 + 17 x 18 = 310 of the 312 px)
    private static final int SLOT = 18, GRID_X = 4, GRID_Y = 14, STATS_X = 172, PALETTE_Y = 128, PER_ROW = 17;
    private static final int TEXT = 0xFF20262C, DIM = 0xFF5C6670, GOOD = 0xFF2E8B47, BAD = 0xFFC0392B, WARN = 0xFFC88A1E, FRAME = 0xFF8A949E;

    private final Profile[] layout = new Profile[54];
    private final String[] keys = new String[54];
    private final String preset;
    private int columns;
    private int selected;
    private ReactorSimulation.Report report;
    /** 0.1.10.19: the same design for a full fuel load (stops at the steady state); it decides the verdict. */
    private ReactorSimulation.Report full;
    private final List<ItemStack> palette = new ArrayList<>();

    public ReactorSimulatorWidget(int columns, String preset) {
        super(0, 0, W, H, Component.translatable("guide.mio_icif.simulator.title"));
        this.preset = preset == null ? "" : preset;
        for (String key : ReactorInventory.PALETTE) palette.add(new ItemStack(BuiltInRegistries.ITEM.get(ReactorInventory.itemId(key))));
        load();
        if (this.preset.isBlank()) this.columns = Math.max(3, Math.min(9, columns));
        recompute();
    }

    private void load() {
        java.util.Arrays.fill(layout, null);
        java.util.Arrays.fill(keys, null);
        int[] cols = {3};
        Profile[] parsed = ReactorSimulation.parse(preset, cols);
        String[] rows = preset.isBlank() ? new String[0] : preset.trim().split("/");
        for (int r = 0; r < rows.length && r < 6; r++) {
            String[] cells = rows[r].split(",");
            for (int c = 0; c < cells.length && c < cols[0]; c++) {
                if (parsed[r * 9 + c] != null) { layout[r * 9 + c] = parsed[r * 9 + c]; keys[r * 9 + c] = cells[c].trim(); }
            }
        }
        columns = cols[0];
    }

    private void recompute() {
        report = ReactorSimulation.run(layout, columns);
        full = ReactorSimulation.fullRun(layout, columns);
    }

    // ------------------------------------------------------------------ drawing
    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX(), y0 = getY();
        g.fill(x0, y0, x0 + W, y0 + H, 0x30FFFFFF);
        g.renderOutline(x0, y0, W, H, FRAME);
        small(g, font, Component.translatable("guide.mio_icif.simulator.chambers", columns - 3).getString(), x0 + GRID_X, y0 + 4, TEXT);
        button(g, font, x0 + 70, y0 + 2, "-", mouseX, mouseY);
        button(g, font, x0 + 82, y0 + 2, "+", mouseX, mouseY);
        button(g, font, x0 + 96, y0 + 2, Component.translatable("guide.mio_icif.simulator.clear").getString(), mouseX, mouseY);
        button(g, font, x0 + 128, y0 + 2, Component.translatable("guide.mio_icif.simulator.reset").getString(), mouseX, mouseY);
        // reactor grid
        String hover = null;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x0 + GRID_X + c * SLOT, sy = y0 + GRID_Y + r * SLOT;
                boolean active = c < columns;
                g.fill(sx, sy, sx + 16, sy + 16, active ? 0xFF8B9299 : 0x40707880);
                g.fill(sx + 1, sy + 1, sx + 16, sy + 16, active ? 0xFFC6CCD2 : 0x30707880);
                String key = keys[r * 9 + c];
                if (active && key != null) g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ReactorInventory.itemId(key))), sx, sy);
                if (active && mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    g.fill(sx, sy, sx + 16, sy + 16, 0x50FFFFFF);
                    if (key != null) hover = itemName(key);
                }
            }
        }
        // palette
        for (int i = 0; i < palette.size(); i++) {
            int sx = x0 + GRID_X + (i % PER_ROW) * SLOT, sy = y0 + PALETTE_Y + (i / PER_ROW) * SLOT;
            if (i == selected) g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF3A6EA5);
            g.renderItem(palette.get(i), sx, sy);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) hover = palette.get(i).getHoverName().getString();
        }
        // values
        ReactorSimulation.Report rep = report;
        int ty = y0 + GRID_Y;
        small(g, font, Component.translatable("guide.mio_icif.simulator.output", String.format("%.1f", rep.euPerTick())).getString(), x0 + STATS_X, ty, TEXT);
        small(g, font, Component.translatable("guide.mio_icif.simulator.heat", rep.heatPerCycle()).getString(), x0 + STATS_X, ty + 10, TEXT);
        small(g, font, Component.translatable("guide.mio_icif.simulator.vent", rep.ventPerCycle()).getString(), x0 + STATS_X, ty + 20, TEXT);
        small(g, font, Component.translatable("guide.mio_icif.simulator.hull", rep.hullHeat(), rep.maxHull()).getString(), x0 + STATS_X, ty + 30, TEXT);
        small(g, font, Component.translatable("guide.mio_icif.simulator.trend", String.format("%+.1f", rep.hullTrend())).getString(), x0 + STATS_X, ty + 40,
            rep.hullTrend() > 0 ? WARN : TEXT);
        small(g, font, Component.translatable("guide.mio_icif.simulator.margin", Math.round(rep.margin() * 100)).getString(), x0 + STATS_X, ty + 50, TEXT);
        if (rep.melted() > 0) small(g, font, Component.translatable("guide.mio_icif.simulator.melted", rep.melted()).getString(), x0 + STATS_X, ty + 60, BAD);
        String verdict;
        int color;
        // 0.1.10.19: 100 s can call a design safe while its vents still heat up; the full run sees them melt
        ReactorSimulation.Report life = full;
        if (life.stable()) { verdict = Component.translatable("guide.mio_icif.simulator.safe").getString(); color = GOOD; }
        else if (life.secondsToMeltdown() >= 0) { verdict = Component.translatable("guide.mio_icif.simulator.meltdown", life.secondsToMeltdown()).getString(); color = BAD; }
        else if (life.firstMeltCycle() >= 0) { verdict = Component.translatable("guide.mio_icif.simulator.melts_later", life.firstMeltCycle()).getString(); color = WARN; }
        else { verdict = Component.translatable("guide.mio_icif.simulator.unsafe").getString(); color = WARN; }
        g.fill(x0 + STATS_X - 2, ty + 74, x0 + W - 4, ty + 88, (color & 0x00FFFFFF) | 0x30000000);
        small(g, font, verdict, x0 + STATS_X, ty + 78, color);
        String help = hover != null ? hover : Component.translatable("guide.mio_icif.simulator.help").getString();
        small(g, font, font.plainSubstrByWidth(help, (int) ((W - STATS_X - 6) / 0.75F)), x0 + STATS_X, ty + 96, DIM);
        small(g, font, Component.translatable("guide.mio_icif.simulator.selected", palette.get(selected).getHoverName().getString()).getString(),
            x0 + STATS_X, ty + 106, DIM);
    }

    private String itemName(String key) {
        int i = ReactorInventory.PALETTE.indexOf(key);
        return i >= 0 ? palette.get(i).getHoverName().getString() : key;
    }

    private static void button(GuiGraphics g, Font font, int x, int y, String label, int mx, int my) {
        int w = Math.max(10, (int) (font.width(label) * 0.75F) + 6);
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + 10;
        g.fill(x, y, x + w, y + 10, hot ? 0xFF7EA0C6 : 0xFFB8C2CC);
        g.renderOutline(x, y, w, 10, FRAME);
        small(g, font, label, x + 3, y + 2, TEXT);
    }

    private static int buttonWidth(Font font, String label) {
        return Math.max(10, (int) (font.width(label) * 0.75F) + 6);
    }

    private static void small(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isMouseOver(mx, my)) return false;
        Font font = Minecraft.getInstance().font;
        int x0 = getX(), y0 = getY();
        if (my >= y0 + 2 && my < y0 + 12) {
            if (hit(mx, x0 + 70, 10)) { columns = Math.max(3, columns - 1); changed(); return true; }
            if (hit(mx, x0 + 82, 10)) { columns = Math.min(9, columns + 1); changed(); return true; }
            if (hit(mx, x0 + 96, buttonWidth(font, Component.translatable("guide.mio_icif.simulator.clear").getString()))) {
                java.util.Arrays.fill(layout, null); java.util.Arrays.fill(keys, null); changed(); return true;
            }
            if (hit(mx, x0 + 128, buttonWidth(font, Component.translatable("guide.mio_icif.simulator.reset").getString()))) { load(); changed(); return true; }
        }
        int c = (int) Math.floor((mx - x0 - GRID_X) / SLOT), r = (int) Math.floor((my - y0 - GRID_Y) / SLOT);
        if (c >= 0 && c < columns && r >= 0 && r < 6 && my < y0 + GRID_Y + 6 * SLOT) {
            int i = r * 9 + c;
            if (button == 1) { layout[i] = null; keys[i] = null; }
            else { keys[i] = ReactorInventory.PALETTE.get(selected); layout[i] = ReactorInventory.profile(keys[i]); }
            changed();
            return true;
        }
        int pc = (int) Math.floor((mx - x0 - GRID_X) / SLOT), pr = (int) Math.floor((my - y0 - PALETTE_Y) / SLOT);
        int index = pr * PER_ROW + pc;
        if (pc >= 0 && pc < PER_ROW && pr >= 0 && index < palette.size()) { selected = index; click(); return true; }
        return false;
    }

    private static boolean hit(double mx, int x, int w) { return mx >= x && mx < x + w; }

    private void changed() {
        recompute();
        click();
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.4F, 0.2F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
