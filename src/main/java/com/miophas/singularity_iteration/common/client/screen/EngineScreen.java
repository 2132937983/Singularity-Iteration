package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.PixelCanvas;
import com.miophas.singularity_iteration.common.energy.EngineGovernor;
import com.miophas.singularity_iteration.common.menu.generator.EngineMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.15: the engine screen (semifluid, advanced semifluid and diesel generators), 248 x 204, an engine room monitor:
 * an exhaust gas temperature bar per cylinder, the tachometer, the coolant and the fuel tank, the readings beside them; a
 * record strip of the load (blue) and exhaust temperature (rust) over the last ~48 s; the output, fuel use, efficiency and
 * state below; the stored energy under the window with the restart (60 %) and stop (98 %) marks.
 *
 * <p>0.1.11.181: the vanilla 176 wide, merged (docs/gui_processing_machines.md, "合并布局"): the engine drawn on the panel
 * (x 8..168, y 15..77). The fuel container over the tank's filler and the empty one under it, the fuel tank, the engine block
 * with a window per cylinder showing its exhaust temperature, the crank turning while it fires, the generator with the
 * tachometer over it, the coolant thermometer, the output cable down to the battery; the record strip under the engine.
 * The readings beside the gauges went into the drawing's tooltip; the readouts' full text shows when a line did not fit.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class EngineScreen<T extends EngineMenu> extends mio_icif_screen<T> {

    private static final int W = 176, STRIP_Y = 145, READ_Y = 80, READ_H = 62;
    private static final int TANK_X = 36, BLOCK_X = 48, BLOCK_Y = 18, BLOCK_W = 62, BLOCK_H = 36, GEN_X = 114, GEN_Y = 34, GEN_W = 26,
        GEN_H = 20, TACH_X = 127, TACH_Y = 25, TACH_R = 8, COOL_X = 158, COOL_Y = 19, COOL_H = 22, REC_X = 50, REC_Y = 60, REC_W = 92, REC_H = 16;
    private static final int RUST = 0xFFB4471F, HOT = 0xFFC0392B, BLUE = 0xFF2A5D8A, GREEN = 0xFF2F6E4F, GRID = 0xFFD9DED9;
    private static final String K = "gui.mio_icif.engine.";
    private List<Component> cutLines = List.of();

    public EngineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = EngineMenu.GUI_WIDTH;
        this.imageHeight = EngineMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.drawManaged(() -> {
            int x = leftPos, y = topPos;
            VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
            double time = PlantMimic.time(partialTick);
            FcsDraw.beginPixels(new PixelCanvas(x + 4, y + 14, W - 8, 64, x + 4, y + 14, 1F, VanillaFrame.PANEL).flat());
            try {
                engine(g, x, y, time);
            } finally {
                FcsDraw.endPixels(g);
            }
            record(g, x + REC_X, y + REC_Y, REC_W, REC_H);
            for (Slot slot : menu.slots) {
                if (slot.x < 0 || slot.x > imageWidth) continue;
                VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
            ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the vanilla font
            readings();
            cutLines = ReadoutPanel.end(g, x + 7, y + READ_Y, W - 14, READ_H);
            strip(g, x, y + STRIP_Y);
        });
    }

    private boolean firing() { return v(0) >= EngineGovernor.WARMING; }

    private double load() { return v(1) / 1000.0; }

    private double coolant() { return v(3) / 10.0; }

    // ---- the engine ----------------------------------------------------------------------------------------------------

    private void engine(GuiGraphics g, int x, int y, double time) {
        int fuel = fuelColour();
        boolean firing = firing();
        // the fuel container over the filler, the empty one under it, the tank and its pipe into the engine
        MachineArt.filler(g, x + EngineMenu.SLOT_X - 2, y + EngineMenu.FUEL_Y + 19, 20);
        MachineArt.pipe(g, x + 28, x + TANK_X - 2, y + 39, v(11) >= 0 ? fuel : 0, -1);
        MachineArt.level(g, x + TANK_X, y + 19, 8, 52, v(10) > 0 ? v(9) / (double) v(10) : 0, fuel);
        MachineArt.pipe(g, x + TANK_X + 10, x + BLOCK_X, y + 30, firing ? fuel : 0, MachineArt.phase(time, firing ? 0.4 : 0));
        // the block: a window per cylinder with its exhaust temperature, the crankcase under them
        MachineArt.iron(g, x + BLOCK_X, y + BLOCK_Y, BLOCK_W, BLOCK_H);
        int cylinders = Math.max(1, Math.min(6, v(14))), cx0 = BLOCK_X + (BLOCK_W - (cylinders * 9 - 2)) / 2;
        for (int c = 0; c < cylinders; c++) {
            double t = EngineGovernor.exhaust(load(), coolant(), firing) + (firing ? 12 * Math.sin(c * 1.7 + time / 8.0) : 0);
            int cx = cx0 + c * 9;
            MachineArt.sunk(g, x + cx, y + BLOCK_Y + 3, 7, 22, 0xFFE3E6E4);
            int h = (int) Math.round(20 * Math.max(0, Math.min(1, t / 600.0)));
            if (h > 0) FcsDraw.fill(g, x + cx + 1, y + BLOCK_Y + 24 - h, x + cx + 6, y + BLOCK_Y + 24, t > 520 ? HOT : RUST);
        }
        MachineArt.sunk(g, x + BLOCK_X + 3, y + BLOCK_Y + 27, BLOCK_W - 6, 6, MachineArt.WINDOW);
        if (firing) {
            int px = (int) Math.floorMod((long) Math.floor(time * 2), (long) (BLOCK_W - 10));
            FcsDraw.fill(g, x + BLOCK_X + 4 + px, y + BLOCK_Y + 28, x + BLOCK_X + 6 + px, y + BLOCK_Y + 32, MachineArt.STEEL_LIGHT);
        }
        // the generator on the crankshaft, the tachometer over it (0..2,000 rpm over 270 degrees)
        MachineArt.shaft(g, x + BLOCK_X + BLOCK_W, x + GEN_X, y + GEN_Y + GEN_H / 2);
        MachineArt.generator(g, x + GEN_X, y + GEN_Y, GEN_W, GEN_H);
        FcsDraw.disc(g, x + TACH_X, y + TACH_Y, TACH_R, MachineArt.STEEL_DARK);
        FcsDraw.disc(g, x + TACH_X, y + TACH_Y, TACH_R - 1, 0xFFE6E9E7);
        for (int k = 0; k <= 10; k += 2) {
            double a = Math.toRadians(135 + 27 * k);
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            FcsDraw.line(g, x + TACH_X + c * (TACH_R - 3), y + TACH_Y + s * (TACH_R - 3), x + TACH_X + c * (TACH_R - 1.5F), y + TACH_Y + s * (TACH_R - 1.5F), 1F,
                k >= 9 ? HOT : MachineArt.STEEL_DARK);
        }
        double a = Math.toRadians(135 + 270 * Math.min(1, v(2) / 2000.0));
        FcsDraw.line(g, x + TACH_X, y + TACH_Y, x + TACH_X + (float) Math.cos(a) * (TACH_R - 2), y + TACH_Y + (float) Math.sin(a) * (TACH_R - 2), 1F, HOT);
        FcsDraw.disc(g, x + TACH_X, y + TACH_Y, 1.2F, MachineArt.IRON_DARK);
        // the coolant (blue while cold, green at working temperature) and the output cable down to the battery
        double coolant = coolant();
        MachineArt.level(g, x + COOL_X, y + COOL_Y, 4, COOL_H, coolant / 110.0, coolant < 70 ? BLUE : GREEN);
        MachineArt.euCable(g, v(4) > 0, time, x + GEN_X + GEN_W, y + GEN_Y + GEN_H / 2, x + 152, y + GEN_Y + GEN_H / 2, x + 152, y + EngineMenu.BATTERY_Y - 2);
    }

    private int fuelColour() {
        if (v(11) < 0) return 0xFF9AA3A1;
        var fluid = BuiltInRegistries.FLUID.byId(v(11));
        String path = BuiltInRegistries.FLUID.getKey(fluid).getPath();
        if (path.contains("diesel")) return 0xFFC9A227;
        if (path.contains("biogas")) return 0xFF7A9A3A;
        if (path.contains("crude")) return 0xFF3B2E25;
        if (path.contains("biomass")) return 0xFF5E7A2E;
        return 0xFF8A8F94;
    }

    // ---- the record strip --------------------------------------------------------------------------------------------

    private void record(GuiGraphics g, float x, float y, float w, float h) {
        MachineArt.sunk(g, (int) x - 1, (int) y - 1, (int) w + 2, (int) h + 2, 0xFFFBFBF6);
        for (int k = 1; k < 4; k++) FcsDraw.line(g, x, y + h * k / 4, x + w, y + h * k / 4, 0.3F, GRID);
        int n = EngineGovernor.SAMPLES, head = v(15);
        int[] load = new int[n], egt = new int[n];
        for (int i = 0; i < n / 2; i++) {
            int word = v(16 + i);
            load[2 * i] = word & 0xFF; egt[2 * i] = (word >>> 8) & 0xFF; load[2 * i + 1] = (word >>> 16) & 0xFF; egt[2 * i + 1] = (word >>> 24) & 0xFF;
        }
        float[] pl = null, pe = null;
        for (int k = 0; k < n; k++) {
            int i = (head + k) % n;
            if (load[i] == 0) { pl = pe = null; continue; }
            float px = x + w * k / (n - 1);
            float[] ql = {px, y + h - h * (load[i] - 1) / 254F}, qe = {px, y + h - h * (egt[i] - 1) / 254F};
            if (pl != null) {
                FcsDraw.line(g, pe[0], pe[1], qe[0], qe[1], 0.7F, RUST);
                FcsDraw.line(g, pl[0], pl[1], ql[0], ql[1], 0.7F, BLUE);
            }
            pl = ql; pe = qe;
        }
    }

    // ---- readings ----------------------------------------------------------------------------------------------------

    private static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    private void readings() {
        ReadoutPanel.add(0, Component.translatable(K + "out_line", f1(v(4) / 100.0), f1(v(5) / 100.0), String.format(Locale.ROOT, "%.2f", v(6) / 1000.0)),
            WindRoseView.INK);
        Component fuel = v(11) < 0 ? Component.translatable(K + "no_fuel")
            : BuiltInRegistries.FLUID.byId(v(11)).getFluidType().getDescription();
        boolean stopped = v(0) <= EngineGovernor.STOPPED_FUEL;   // no efficiency to show while stopped
        ReadoutPanel.add(1, Component.translatable(K + "fuel_line", stopped ? "—" : Math.round(v(7) / 10.0) + "%", stopped ? "—" : f1(v(8) / 10.0))
            .append(" · ").append(fuel).append(String.format(Locale.ROOT, " %,d / %,d mB", v(9), v(10))), WindRoseView.DIM);
        String state; int colour;
        switch (v(0)) {
            case EngineGovernor.STOPPED_FULL -> { state = "state.stopped_full"; colour = BLUE; }
            case EngineGovernor.STOPPED_FUEL -> { state = "state.stopped_fuel"; colour = WindRoseView.DIM; }
            case EngineGovernor.WARMING -> { state = "state.warming"; colour = 0xA0602A; }
            case EngineGovernor.LOW_LOAD -> { state = "state.low_load"; colour = 0xA0602A; }
            default -> { state = "state.running"; colour = GREEN; }
        }
        // 0.1.11.145: the output leads, the state beside it
        ReadoutPanel.headline(Component.literal(f1(v(4) / 100.0) + " EU/t"), Component.translatable(K + state), colour & 0xFFFFFF);
    }

    private void strip(GuiGraphics g, int x, int sy) {
        // 0.1.11.121: vanilla font, furnace-style bar; the governor's start and stop marks in blue
        long e = Integer.toUnsignedLong(v(12)), cap = Math.max(1, Integer.toUnsignedLong(v(13)));
        int[] bar = VanillaFrame.labelledBar(g, x + 8, sy, W - 16, Component.translatable(K + "stored"), Math.min(1.0, e / (double) cap),
            Component.literal(String.format(Locale.ROOT, "%,d / %,d EU", e, cap)));
        for (double mark : new double[]{EngineGovernor.START_BELOW, EngineGovernor.STOP_AT}) {
            int m = (int) (bar[0] + bar[1] * mark);
            if (bar[1] > 0) g.fill(m, sy + 1, m + 1, sy + 10, BLUE);
        }
    }

    /** 0.1.11.181: the gauges' figures (they were written beside the gauges), then what the drawing shows. */
    private List<Component> engineTip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(String.format(Locale.ROOT, "%d rpm · ", v(2))).append(Component.translatable(K + "load", Math.round(load() * 100) + "%"))
            .append(" · ").append(Component.translatable(K + "egt_value", Math.round(EngineGovernor.exhaust(load(), coolant(), firing()))))
            .append(" · ").append(Component.translatable(K + "coolant", String.format(Locale.ROOT, "%.0f", coolant()))));
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): only the gauges' figures; the drawing's how-it-works lines are hidden for now
        // lines.add(Component.translatable(K + "tip.1"));
        // lines.add(Component.translatable(K + "tip.2"));
        return lines;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hoveredSlot != null) return;   // a slot shows its own tooltip only
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the record's legend, the economy notes and the bar's start/stop marks are hidden for now
        if (rx >= REC_X - 1 && rx <= REC_X + REC_W && ry >= REC_Y - 1 && ry <= REC_Y + REC_H) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.3")), mouseX, mouseY);
        } else if (rx >= TANK_X - 2 && rx < 168 && ry >= 15 && ry < 78) {
            g.renderComponentTooltip(this.font, engineTip(), mouseX, mouseY);
        } else if (rx >= 8 && rx < 168 && ry >= READ_Y && ry < READ_Y + READ_H) {
            List<Component> lines = new ArrayList<>(cutLines);
            // lines.add(Component.translatable(K + "tip.4"));
            // lines.add(Component.translatable(K + "tip.5"));
            if (!lines.isEmpty()) g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (rx >= 8 && rx < 168 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.6")), mouseX, mouseY);
        }
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
