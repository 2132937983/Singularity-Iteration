package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.SolarPhysics;
import com.miophas.singularity_iteration.common.menu.generator.SolarPanelMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.14: the screen of every solar panel, 248 x 204. A pyranometer record of today: the irradiance on the panel
 * through the day (amber, with the clear-sky envelope dashed and the 700 W/m² clipping level dotted) and the output
 * (blue, rated output at the top); a red line at the time now. 0.1.11.101: the dotted level is the panel's own full-output
 * level; a concentrator (route A) shows its direct-light share and cooling instead of the clouds, and its own tips.
 * 0.1.11.102: a photon resonance panel (route B) shows the night sky factor, "night sky" for its night output, and its own tips. Under it the time, irradiance and clear sky; clouds, cell
 * temperature and shade; output, today's energy and the state. Under the window the stored energy.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class SolarPanelScreen<T extends SolarPanelMenu> extends mio_icif_screen<T> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 62, STRIP_Y = 145;   // 0.1.11.121: the readouts under the window
    private static final int CX = WX + 24, CY = WY + 6, CW = 176, CH = 44;
    private static final int AMBER = 0xFFD49A12, AMBER_FILL = 0x40D49A12, ENVELOPE = 0xFFB8A76A, BLUE = 0xFF2A5D8A,
        RED = 0xFFC0392B, GRID = 0xFFD9DED9;
    private static final String K = "gui.mio_icif.solar.";

    public SolarPanelScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SolarPanelMenu.GUI_WIDTH;
        this.imageHeight = SolarPanelMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    /** 0.1.11.101: the panel's full-output level (W/m²), its profile flags and its cooling share. */
    private int clipLevel() { int c = v(mio_icif_solar_generator.DATA_PROFILE); return c > 0 ? c : (int) SolarPhysics.CLIP; }
    private boolean concentrator() { return (v(mio_icif_solar_generator.DATA_PROFILE + 1) & 1) != 0; }
    private boolean heldCell() { return (v(mio_icif_solar_generator.DATA_PROFILE + 1) & 2) != 0; }
    /** 0.1.11.102: a photon resonance panel (route B). */
    private boolean resonance() { return (v(mio_icif_solar_generator.DATA_PROFILE + 1) & 4) != 0; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        int slots = menu.slots.size() - 36;
        for (int i = 0; i < slots; i++) VanillaFrame.slot(g, x + SolarPanelMenu.SLOT_X - 1, y + SolarPanelMenu.SLOT_Y - 1 + i * SolarPanelMenu.SLOT_STEP);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            VanillaFrame.slot(g, x + SolarPanelMenu.INVENTORY_X - 1 + c * 18, y + SolarPanelMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) VanillaFrame.slot(g, x + SolarPanelMenu.INVENTORY_X - 1 + c * 18, y + SolarPanelMenu.HOTBAR_Y - 1);

        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, WH + 2, WindRoseView.PAPER);   // 0.1.11.119: the vanilla look
        chart(g, x + CX, y + CY);
        ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the readout window, vanilla font
        readings(g, x + WX + 4, y + WY + 61);
        ReadoutPanel.end(g, x + WX - 1, y + WY + WH + 3, WW + 2, 62);
        strip(g, x, y + STRIP_Y);
    }

    // ---- the record ---------------------------------------------------------------------------------------------------

    private static float X(float px, double dayTime) { return px + (float) (dayTime / 24000.0 * CW); }
    private static float Yg(float py, double wm2) { return py + CH - (float) (Math.min(1000, Math.max(0, wm2)) / 1000.0 * CH); }
    private static float Yo(float py, double share) { return py + CH - (float) (Math.min(1, Math.max(0, share)) * CH); }

    private void chart(GuiGraphics g, float px, float py) {
        float u = WindRoseView.u();
        for (int k = 0; k <= 4; k++) FcsDraw.line(g, px, py + CH * k / 4F, px + CW, py + CH * k / 4F, 0.3F, GRID);
        for (int k = 0; k <= 8; k++) FcsDraw.line(g, px + CW * k / 8F, py, px + CW * k / 8F, py + CH, 0.3F, GRID);
        FcsDraw.line(g, px, py + CH, px + CW, py + CH, 0.5F, 0xFF5B6867);
        // clipping level and clear-sky envelope
        float clip = Yg(py, clipLevel());
        for (float cx = px; cx < px + CW; cx += 3) FcsDraw.line(g, cx, clip, cx + 1, clip, 0.4F, 0xFF9AA3A1);
        float[] prev = null;
        for (int t = 0; t <= 12000; t += 250) {
            float[] q = {X(px, t), Yg(py, SolarPhysics.clearSky(SolarPhysics.elevation(t)))};
            if (prev != null && (t / 250) % 2 == 0) FcsDraw.line(g, prev[0], prev[1], q[0], q[1], 0.5F, ENVELOPE);
            prev = q;
        }
        // today's record
        int n = mio_icif_solar_generator.SAMPLES;
        int[] gb = new int[n], ob = new int[n];
        for (int i = 0; i < n / 2; i++) {
            int w = v(16 + i);
            gb[2 * i] = w & 0xFF; ob[2 * i] = (w >>> 8) & 0xFF; gb[2 * i + 1] = (w >>> 16) & 0xFF; ob[2 * i + 1] = (w >>> 24) & 0xFF;
        }
        int now = v(9);
        float[] pg = null, po = null;
        for (int i = 0; i <= n; i++) {
            boolean live = i == n || i == now / mio_icif_solar_generator.SAMPLE;
            double wm2, share;
            double t;
            if (i < n && !live) {
                if (gb[i] == 0) { pg = po = null; continue; }
                wm2 = (gb[i] - 1) / 254.0 * 1016; share = (ob[i] - 1) / 254.0; t = (i + 0.5) * mio_icif_solar_generator.SAMPLE;
            } else {
                if (i == n) break;
                wm2 = v(0); share = v(3) > 0 ? v(2) / (double) v(3) : 0; t = now;
            }
            float qx = X(px, t), qg = Yg(py, wm2), qo = Yo(py, share);
            if (pg != null) {
                FcsDraw.quad(g, pg[0], pg[1], pg[0], py + CH, qx, py + CH, qx, qg, AMBER_FILL);
                FcsDraw.line(g, pg[0], pg[1], qx, qg, 0.8F, AMBER);
                FcsDraw.line(g, po[0], po[1], qx, qo, 0.7F, BLUE);
            }
            pg = new float[]{qx, qg};
            po = new float[]{qx, qo};
            if (live) break;
        }
        float nx = X(px, now);
        FcsDraw.line(g, nx, py, nx, py + CH, 0.6F, RED);
        // axes
        String[] hours = {"06", "12", "18", "24", "06"};
        for (int k = 0; k < 5; k++) {
            Component c = Component.literal(hours[k]);
            PumpCrossSection.text(g, c, px + CW * k / 4F - AsciiHud.width(c, u) / 2, py + CH + 1, WindRoseView.DIM, u);
        }
        for (int k : new int[]{0, 500, 1000}) {
            Component c = Component.literal(String.valueOf(k));
            PumpCrossSection.text(g, c, px - 2 - AsciiHud.width(c, u), Yg(py, k) - 4 * u, WindRoseView.DIM, u);
        }
        Component full = Component.translatable(K + "rated");
        PumpCrossSection.text(g, full, px + CW - AsciiHud.width(full, u) - 1, py + 1, BLUE & 0xFFFFFF, u);
    }

    private static String clock(int dayTime) {
        int h = (int) ((dayTime / 1000 + 6) % 24), m = (int) ((dayTime % 1000) * 60 / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    private void readings(GuiGraphics g, float tx, float ty) {
        float u = WindRoseView.u(), lh = 9 * u + 1, w = WW - 8;
        WindRoseView.line(g, Component.translatable(K + "sun_line", clock(v(9)), v(0), v(1)), tx, ty, WindRoseView.INK, w);
        String shade = switch (v(8)) {
            case mio_icif_solar_generator.OPEN -> "shade.open";
            case mio_icif_solar_generator.DIFFUSE -> concentrator() ? "shade.diffuse_none" : "shade.diffuse";
            case mio_icif_solar_generator.NO_SKY -> "shade.no_sky";
            default -> "shade.dark";
        };
        net.minecraft.network.chat.MutableComponent env = concentrator()
            ? Component.translatable(K + "env_line_direct", f2(v(7) / 1000.0), f1(v(5) / 10.0), f3(v(6) / 1000.0))
                .append(" · ").append(heldCell() ? Component.translatable(K + "cooling_held")
                    : Component.translatable(K + "cooling", Math.round(v(mio_icif_solar_generator.DATA_PROFILE + 2) / 10.0)))
            : Component.translatable(K + "env_line", f2(v(7) / 1000.0), f1(v(5) / 10.0), f3(v(6) / 1000.0));
        if (resonance()) env.append(" · ").append(Component.translatable(K + "night_sky", f2(v(mio_icif_solar_generator.DATA_PROFILE + 3) / 1000.0)));
        WindRoseView.line(g, env.append(" · ").append(Component.translatable(K + shade)), tx, ty + lh, WindRoseView.DIM, w);
        double out = v(2) / 100.0, rated = v(3) / 100.0;
        boolean full = v(10) >= v(11) && v(11) > 0;
        String state; int colour;
        if (v(15) != 0) { state = resonance() ? "state.sky" : "state.core"; colour = 0x2A5D8A; }
        else if (out <= 0 && v(13) <= 0) { state = "state.night"; colour = WindRoseView.DIM; }
        else if (out <= 0) { state = "state.none"; colour = WindRoseView.DIM; }
        else if (full) { state = "state.full"; colour = WindRoseView.DIM; }
        else if (out >= rated - 0.01) { state = "state.clipped"; colour = 0x2F6E4F; }
        else { state = "state.run"; colour = 0x2F6E4F; }
        WindRoseView.line(g, Component.translatable(K + "out_line", f1(out), f1(rated), String.format(Locale.ROOT, "%,d", Integer.toUnsignedLong(v(12))))
            .append(" · ").append(Component.translatable(K + state)), tx, ty + 2 * lh, colour, w);
    }

    private static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private static String f3(double v) { return String.format(Locale.ROOT, "%.3f", v); }

    private void strip(GuiGraphics g, int x, int sy) {
        // 0.1.11.121: vanilla font, furnace-style bar
        long e = Integer.toUnsignedLong(v(10)), cap = Math.max(1, Integer.toUnsignedLong(v(11)));
        VanillaFrame.labelledBar(g, x + WX, sy, 241 - WX, Component.translatable(K + "stored"), Math.min(1.0, e / (double) cap),
            Component.literal(String.format(Locale.ROOT, "%,d / %,d EU", e, cap)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= WX && rx < WX + WW && ry >= WY && ry < WY + 58) {
            String route = concentrator() ? "tip.a." : resonance() ? "tip.b." : "tip.";
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.1"), Component.translatable(K + "tip.2"),
                Component.translatable(K + "tip.3", clipLevel()), Component.translatable(K + route + "4")), mouseX, mouseY);
        } else if (rx >= WX && rx < WX + WW && ry >= WY + 58 && ry < WY + WH) {
            String route = concentrator() ? "tip.a." : resonance() ? "tip.b." : "tip.";
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + route + "5"), Component.translatable(K + route + "6")), mouseX, mouseY);
        }
    }
}
