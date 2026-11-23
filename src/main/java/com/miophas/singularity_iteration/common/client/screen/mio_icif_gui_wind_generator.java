package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.WindGeneratorMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Wind generator screen. 0.1.11.9: 248 x 204. The wind rose of the dimension's wind field (this small turbine turns
 * with the wind, so no facing needle), and its output against its effective height (height minus the obstacles around
 * it) with the current point; under them the wind, the height and obstacles, the output and the state; under the
 * window the stored energy.
 *
 * <p>0.1.11.181: the vanilla 176 wide, merged (docs/gui_processing_machines.md, "合并布局"): the little turbine drawn on the
 * left, its blades turning while it makes power, the battery at the foot of its mast; the rose and the curve on paper beside
 * it (the curve 58 wide, its height axis named in the tooltip).
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_wind_generator extends mio_icif_screen<WindGeneratorMenu> {

    private static final int STRIP_Y = 145, READ_Y = 80, READ_H = 62, ROSE_X = 64, ROSE_Y = 46, CURVE_X = 98;
    private List<Component> cutLines = List.of();
    private static final String K = "gui.mio_icif.wind.";

    public mio_icif_gui_wind_generator(WindGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WindGeneratorMenu.GUI_WIDTH;
        this.imageHeight = WindGeneratorMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        turbine(g, x, y, PlantMimic.time(partialTick));
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;
            VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        // 0.1.11.181: the rose and the curve on paper beside the turbine
        MachineArt.sunk(g, x + ROSE_X - 30, y + 16, 60, 60, WindRoseView.PAPER);
        WindRoseView.rose(g, x + ROSE_X, y + ROSE_Y, 21, WindRoseView.bytes(this::v, 10), WindRoseView.bytes(this::v, 14), -1, v(3));
        MachineArt.sunk(g, x + CURVE_X, y + 16, 168 - CURVE_X, 60, WindRoseView.PAPER);
        curve(g, x + CURVE_X + 6, y + 24, 58, 36);
        ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the vanilla font
        double weather = v(7) / 100.0;
        ReadoutPanel.add(0, Component.translatable(K + "base_wind", v(2), String.format(Locale.ROOT, "×%.2f", weather)).append(" · ")
            .append(WindRoseView.bearing(v(3))), WindRoseView.INK);
        ReadoutPanel.add(1, Component.translatable(K + "height_line", v(4), v(5), v(6)), WindRoseView.DIM);
        boolean generating = v(0) != 0;
        String state = generating ? "state.run" : v(1) > 0 ? "state.full" : v(4) <= v(6) ? "state.too_low" : "state.weak";
        // 0.1.11.145: the output leads, the state beside it
        ReadoutPanel.headline(Component.literal(v(1) + " EU/t"), Component.translatable(K + state), generating ? 0x2F6E4F : WindRoseView.DIM);
        cutLines = ReadoutPanel.end(g, x + 7, y + READ_Y, 162, READ_H);

        // strip: stored energy (0.1.11.121: vanilla font, furnace-style bar)
        long e = Integer.toUnsignedLong(v(8)), cap = Math.max(1, Integer.toUnsignedLong(v(9)));
        VanillaFrame.labelledBar(g, x + 8, y + STRIP_Y, 160, Component.translatable(K + "stored"), Math.min(1.0, e / (double) cap),
            Component.literal(e + " / " + cap + " EU"));
    }

    /** 0.1.11.181: the turbine on its mast, the blades turning while it makes power; the cable down the mast to the battery. */
    private void turbine(GuiGraphics g, int x, int y, double time) {
        boolean on = v(0) != 0;
        int hx = WindGeneratorMenu.SLOT_X + 8, hy = 26;
        MachineArt.steel(g, x + hx - 2, y + hy + 4, 5, WindGeneratorMenu.SLOT_Y - hy - 5);
        MachineArt.euCable(g, on, time, x + hx, y + hy + 5, x + hx, y + WindGeneratorMenu.SLOT_Y - 2);
        double a = on ? time * 0.25 : 0.4;
        for (int k = 0; k < 3; k++) {
            double b = a + k * 2 * Math.PI / 3;
            FcsDraw.line(g, x + hx, y + hy, x + hx + (float) Math.cos(b) * 11, y + hy + (float) Math.sin(b) * 11, 2F, MachineArt.STEEL_LIGHT);
        }
        MachineArt.steel(g, x + hx - 4, y + hy - 3, 9, 7);
        FcsDraw.disc(g, x + hx, y + hy, 2F, MachineArt.STEEL_DARK);
    }

    /** Output against effective height: p = weather x wind x (h - base) / 750, up to base + 192. */
    private void curve(GuiGraphics g, float px, float py, float pw, float ph) {
        float u = WindRoseView.u();
        FcsDraw.line(g, px, py + ph, px + pw, py + ph, 0.5F, 0xFF5B6867);
        FcsDraw.line(g, px, py, px, py + ph, 0.5F, 0xFF5B6867);
        int base = v(6);
        double k = v(7) / 100.0 * v(2) / 750.0;
        double pmax = Math.max(1, k * 192 * 1.1);
        java.util.function.DoubleUnaryOperator X = h -> px + (h - base) / 192 * pw;
        java.util.function.DoubleUnaryOperator Y = p -> py + ph - p / pmax * ph;
        FcsDraw.line(g, px, (float) Y.applyAsDouble(0), px + pw, (float) Y.applyAsDouble(k * 192), 0.9F, 0xFF23454A);
        int h = Math.max(base, Math.min(base + 192, v(4)));
        FcsDraw.disc(g, (float) X.applyAsDouble(h), (float) Y.applyAsDouble(k * (h - base)), 1.8F, 0xFFC0392B);
        PumpCrossSection.text(g, Component.translatable(K + "axis_eu"), px + 2, py - 1, WindRoseView.DIM, u);
        Component a = Component.literal("Y" + base + "–" + (base + 192));   // 0.1.11.181: the axis named in the tooltip
        PumpCrossSection.text(g, a, px + pw - PumpCrossSection.width(a, u), py + ph + 1, WindRoseView.DIM, u);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (hoveredSlot != null) return;   // 0.1.11.181: a slot shows its own tooltip only
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the rose's and the curve's how-it-works texts are hidden for now (the height range stays)
        if (rx >= ROSE_X - 30 && rx < ROSE_X + 30 && ry >= 16 && ry < 76) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.rose"), Component.translatable(K + "tip.omni")), mouseX, mouseY);
        } else if (rx >= CURVE_X && rx < 168 && ry >= 16 && ry < 76) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.height"),
            //     Component.translatable(K + "axis_height", v(6), v(6) + 192)), mouseX, mouseY);
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "axis_height", v(6), v(6) + 192)), mouseX, mouseY);
        } else if (!cutLines.isEmpty() && rx >= 8 && rx < 168 && ry >= READ_Y && ry < READ_Y + READ_H) {
            g.renderComponentTooltip(this.font, cutLines, mouseX, mouseY);
        }
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
