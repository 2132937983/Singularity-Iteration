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
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_wind_generator extends mio_icif_screen<WindGeneratorMenu> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 95, STRIP_Y = 113;
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
        VanillaFrame.slot(g, x + WindGeneratorMenu.SLOT_X - 1, y + WindGeneratorMenu.SLOT_Y - 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            VanillaFrame.slot(g, x + WindGeneratorMenu.INVENTORY_X - 1 + c * 18, y + WindGeneratorMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) VanillaFrame.slot(g, x + WindGeneratorMenu.INVENTORY_X - 1 + c * 18, y + WindGeneratorMenu.HOTBAR_Y - 1);

        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, WH + 2, WindRoseView.PAPER);   // 0.1.11.119: the vanilla look
        float u = WindRoseView.u();
        WindRoseView.rose(g, x + WX + 40, y + WY + 31, 22, WindRoseView.bytes(this::v, 10), WindRoseView.bytes(this::v, 14), -1, v(3));
        curve(g, x + WX + 92, y + WY + 6, 108, 46);
        float tx = x + WX + 4, ty = y + WY + 66, lh = 9 * u + 1;
        double weather = v(7) / 100.0;
        WindRoseView.line(g, Component.translatable(K + "base_wind", v(2), String.format(Locale.ROOT, "×%.2f", weather)).append(" · ")
            .append(WindRoseView.bearing(v(3))), tx, ty, WindRoseView.INK, WW - 8);
        WindRoseView.line(g, Component.translatable(K + "height_line", v(4), v(5), v(6)), tx, ty + lh, WindRoseView.DIM, WW - 8);
        boolean generating = v(0) != 0;
        String state = generating ? "state.run" : v(1) > 0 ? "state.full" : v(4) <= v(6) ? "state.too_low" : "state.weak";
        WindRoseView.line(g, Component.translatable(K + "output_eu", v(1)).append(" · ").append(Component.translatable(K + state)),
            tx, ty + 2 * lh, generating ? 0x2F6E4F : WindRoseView.DIM, WW - 8);

        // strip: stored energy
        int sy = y + STRIP_Y;
        Component ec = Component.translatable(K + "stored");
        PumpCrossSection.text(g, ec, x + WX, sy + 6 - 4.5F * u, 0x2A2E33, u);
        float barX = x + WX + AsciiHud.width(ec, u) + 3, barW = x + 241 - 70 - barX;
        long e = Integer.toUnsignedLong(v(8)), cap = Math.max(1, Integer.toUnsignedLong(v(9)));
        g.fill((int) barX - 1, sy + 2, (int) (barX + barW) + 1, sy + 10, SiGuiTheme.SLOT_SHADOW);
        g.fill((int) barX, sy + 3, (int) (barX + barW), sy + 9, SiGuiTheme.SCREEN_BG);
        if (e > 0) g.fillGradient((int) barX, sy + 3, (int) (barX + barW * Math.min(1.0, e / (double) cap)), sy + 9, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        Component pc = Component.literal(e + " / " + cap + " EU");
        PumpCrossSection.text(g, pc, x + 241 - AsciiHud.width(pc, u), sy + 6 - 4.5F * u, 0x2A2E33, u);
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
        Component a = Component.translatable(K + "axis_height", base, base + 192);
        PumpCrossSection.text(g, a, px + pw - AsciiHud.width(a, u), py + ph + 1, WindRoseView.DIM, u);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= WX && rx < WX + 85 && ry >= WY && ry < WY + 62) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.rose"), Component.translatable(K + "tip.omni")), mouseX, mouseY);
        } else if (rx >= WX + 88 && rx < WX + WW && ry >= WY && ry < WY + 62) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.height")), mouseX, mouseY);
        }
    }
}
