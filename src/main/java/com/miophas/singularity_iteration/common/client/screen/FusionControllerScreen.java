package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.PixelCanvas;
import com.miophas.singularity_iteration.common.fusion.FusionBlanket;
import com.miophas.singularity_iteration.common.fusion.FusionPlasma;
import com.miophas.singularity_iteration.common.menu.fusion.FusionControllerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.92: the fusion reactor console's screen.
 *
 * <p>0.1.11.138 (fusion overhaul F6): the vanilla-frame layout (344 x 252, docs/gui_processing_machines.md): the blanket grid
 * on the left (the purple edge marks the column facing the plasma, the bar on its right the blanket heat), the fuel and
 * mitigation slots under it; on the right a pixel-art poloidal cross-section of the tokamak (a vertical cut through the
 * torus: the central solenoid in the middle, a D-shaped plasma on each side inside the blanket, the vacuum vessel and the
 * D-shaped toroidal field coils, poloidal field coils above and below, the divertor under the X-point), the readouts and the
 * feed buttons. The figure follows the reactor: frosted coils while they cool, a glowing solenoid while it charges, the
 * plasma's colour by its temperature with a bright H-mode pedestal, the plasma current running round it, neutral beams and
 * microwaves while it is heated, neutrons into the blanket and exhaust out of the divertor while it burns, a flash at each
 * ELM, a red flash and a fading plasma at a disruption, red coils at a quench. Hovering the figure names its parts; hovering
 * the readouts shows the power balance (0.1.11.133-136). Was drawn with SiGuiTheme (300 x 256).
 */
@OnlyIn(Dist.CLIENT)
public class FusionControllerScreen extends mio_icif_screen<FusionControllerMenu> {
    static final String K = "gui.mio_icif.fusion.";
    private static final int W = FusionControllerMenu.GUI_WIDTH, H = FusionControllerMenu.GUI_HEIGHT;
    /** The figure window, the readout window, the feed row, the heat bar (GUI pixels). */
    static final int DX = 176, DY = 18, DW = 160, DH = 74, RY = 95, RH = 54, FEED_Y = 151, HEAT_X = 171;
    private static final String[] PARTS = {"central_solenoid", "toroidal_coil", "poloidal_coil", "vacuum_vessel", "blanket", "divertor", "port_slot",
        "controller", "plasma"};
    private static final String[] STATES = {"off", "cooling", "charging", "heating", "burning", "disrupted", "quenched"};
    private static final String[] CAUSES = {"none", "quench", "structure", "ash", "blanket", "density_limit", "beta_limit"};
    private static final String[] STATUS = {"ok", "unformed", "no_signal", "no_cryo", "no_heater", "no_exhaust", "no_power_port", "no_fuel",
        "blanket_invalid"};

    // the figure's colours
    private static final int VOID = 0xFF0E1522, FLOOR = 0xFF1C2636, CS_CORE = 0xFF2A3140, COPPER = 0xFFB87333, FROST = 0xFF9FD8E8,
        HOT_COIL = 0xFFE0603A, HOT_COIL_DIM = 0xFF8A2E1E, VESSEL = 0xFF2B3644, VESSEL_EDGE = 0xFF6E7A88, BLANKET = 0xFF1D7A6E,
        BLANKET_HIT = 0xFF7FF0DC, CHANNEL = 0xFF0B1018, TUNGSTEN = 0xFF7C8796, PEDESTAL = 0xFFFFB0C8, CURRENT = 0xFFFFE9B0,
        CHARGE = 0xFF5FB4FF, CHARGE_DOT = 0xFFCFE8FF, BEAM = 0xFFFFD34A, MICROWAVE = 0xFFE070FF, NEUTRON = 0xFFFFFFFF, EXHAUST = 0xFF9AA4B0,
        DISRUPT = 0xFFFF5A3A, ELM = 0xFFFFF6C0;

    /** The plasma's shape: minor radius, elongation and triangularity of each D (window pixels), its centres. */
    static final float A = 10.5F, KAPPA = 1.48F, DELTA = 0.4F;
    static final int CS_X = 75, CY = 36, LEFT_CX = 40, RIGHT_CX = 120;

    private int lastElms = -1;
    private double elmAt = -100, lastTime;

    public FusionControllerScreen(FusionControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = FusionControllerMenu.INV_X;
        inventoryLabelY = FusionControllerMenu.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        String[] labels = {"-4", "-1", "+1", "+4"};
        int[] ids = {2, 0, 1, 3}, xs = {DX, DX + 28, DX + 106, DX + 134};
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> click(id)).bounds(leftPos + xs[i], topPos + FEED_Y, 26, 14).build());
        }
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** 39,200,000 -> 39.2M. */
    static String compact(long v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.ROOT, "%.2fG", v / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        if (a >= 1e4) return String.format(Locale.ROOT, "%.1fk", v / 1e3);
        return Long.toString(v);
    }

    private long v(int i) { return menu.value(i); }

    /** 9,547,583 -> 9.5M (one decimal, for the narrow readout line). */
    static String brief(long v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.ROOT, "%.1fG", v / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.1fM", v / 1e6);
        if (a >= 1e4) return String.format(Locale.ROOT, "%.0fk", v / 1e3);
        return Long.toString(v);
    }

    // ------------------------------------------------------------------ background

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        double time = PlantMimic.time(partialTick);
        g.drawManaged(() -> {
            int x = leftPos, y = topPos;
            VanillaFrame.panel(g, x, y, W, H);
            for (Slot slot : menu.slots) VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
            // the column facing the plasma, and the blanket heat
            g.fill(x + 4, y + FusionControllerMenu.GRID_Y - 1, x + 6, y + FusionControllerMenu.GRID_Y + FusionBlanket.ROWS * 18 - 1, 0xFFC43CFF);
            long hull = v(10), maxHull = Math.max(1, v(11));
            int barTop = y + FusionControllerMenu.GRID_Y - 1, barH = FusionBlanket.ROWS * 18;
            VanillaFrame.inset(g, x + HEAT_X, barTop, 4, barH, 0xFF373737);
            int hot = (int) Math.round((barH - 2) * Math.min(1.0, (double) hull / maxHull));
            if (hot > 0) g.fill(x + HEAT_X + 1, barTop + barH - 1 - hot, x + HEAT_X + 3, barTop + barH - 1, hull * 10 >= maxHull * 7 ? 0xFFE24B4A : 0xFFF2C14E);
            // the figure
            VanillaFrame.inset(g, x + DX - 1, y + DY - 1, DW + 2, DH + 2, VOID);
            FcsDraw.beginPixels(new PixelCanvas(x + DX, y + DY, DW, DH, x + DX, y + DY, 1F, VOID));
            try {
                drawFigure(g, x + DX, y + DY, time);
            } finally {
                FcsDraw.endPixels(g);
            }
            // the readouts
            ReadoutPanel.begin();
            readouts();
            ReadoutPanel.end(g, x + DX - 1, y + RY, DW + 2, RH);
        });
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, VanillaFrame.TITLE, false);
        g.drawString(font, playerInventoryTitle, FusionControllerMenu.INV_X, FusionControllerMenu.INV_Y - 10, VanillaFrame.TITLE, false);
        String[] fuel = {"deuterium", "tritium", "empty", "empty", "coolant"};
        for (int i = 0; i < fuel.length; i++)
            mio_icif_screen.drawFitted(g, font, Component.translatable(K + "slot." + fuel[i]), FusionControllerMenu.FUEL_X[i] - 1,
                FusionControllerMenu.FUEL_Y + 18, 18, VanillaFrame.TITLE, false);
        String feed = v(2) + "/" + FusionPlasma.MAX_FEED;
        g.drawString(font, feed, DX + 80 - font.width(feed) / 2, FEED_Y + 3, v(2) > FusionPlasma.REFERENCE_FEED ? 0xB0452A : VanillaFrame.TITLE, false);
    }

    /** The five readout lines. */
    private void readouts() {
        int state = (int) Math.max(0, Math.min(STATES.length - 1, v(0)));
        boolean plasma = state == 3 || state == 4;
        Component progress = switch (state) {
            case 1 -> Component.translatable(K + "progress.cooling", v(12) * 100 / FusionPlasma.COOLDOWN_TICKS);
            case 2 -> Component.translatable(K + "progress.charging", v(13) * 100 / FusionPlasma.STARTUP_ENERGY);
            case 4 -> v(14) == 0 && v(20) >= 2 ? Component.translatable(K + "progress.steady") : Component.translatable(K + "progress.burn", v(14) / 20);
            case 5, 6 -> Component.translatable(K + "progress.timer", (v(30) + 19) / 20).append(v(43) > 0 && v(43) < CAUSES.length
                ? Component.literal(" ").append(Component.translatable(K + "cause." + CAUSES[(int) v(43)])) : Component.empty());
            default -> Component.empty();
        };
        Component head = Component.translatable(K + "state." + (state == 3 && v(3) > 0 ? "driven" : STATES[state]));
        if (!progress.getString().isEmpty()) head = head.copy().append(" ").append(progress);
        if (plasma) head = head.copy().append(" · ").append(Component.translatable(K + (v(42) == 1 ? "mode.h" : "mode.l")));
        // 0.1.11.146: the net output leads (twice the font size); beside it the state, or what is wrong when something is;
        // under it the plasma, the power balance, and the limits while burning (the fuel otherwise)
        Component side = head;
        int sideColour = state >= 5 ? 0xA32D2D : plasma ? 0x2F6E4F : 0x5F5E5A;
        int status = (int) Math.max(0, Math.min(STATUS.length - 1, v(25)));
        if (status == 1 && v(29) >= 0) {
            int part = (int) Math.min(PARTS.length - 1, v(29));
            side = Component.translatable(K + "unformed_at", v(26), v(27), v(28), Component.translatable(K + "part." + PARTS[part]));
            sideColour = 0xA32D2D;
        } else if (status != 0) {
            side = Component.translatable(K + "status." + STATUS[status]);
            sideColour = 0xA32D2D;
        }
        ReadoutPanel.headline(Component.literal(brief(v(4)) + " EU/t"), side, sideColour);
        double kev = v(1) / 100.0;
        long q = v(16);
        ReadoutPanel.add(1, plasma ? Component.translatable(K + "line_temp", String.format(Locale.ROOT, "%.1f", kev), String.format(Locale.ROOT, "%.2f", kev * 0.116),
            q < 0 ? "∞" : String.format(Locale.ROOT, "%.1f", q / 100.0)) : Component.translatable(K + "line_temp_none"), 0x404040);
        ReadoutPanel.add(2, Component.translatable(K + "line_power", brief(v(3)), brief(v(4))), 0x404040);
        if (plasma) {
            long worst = Math.max(v(40), v(41));
            ReadoutPanel.add(3, Component.translatable(K + "balance.limits", v(40) / 10, v(41) / 10), worst >= 1000 ? 0xA32D2D : worst >= 900 ? 0xBA7517 : 0x5F5E5A);
        } else {
            ReadoutPanel.add(3, Component.translatable(K + "line_fuel", compact(v(8)), compact(v(9)), String.format(Locale.ROOT, "%.2f", v(51) / 100.0)), 0x5F5E5A);
        }
    }

    // ------------------------------------------------------------------ the figure

    /** A point of a D-shaped surface of minor radius {@code a} at poloidal angle {@code t} ({@code side} -1 left, 1 right). */
    static float[] dPoint(float cx, float cy, float a, float kappa, float delta, int side, double t) {
        return new float[]{(float) (cx + side * a * Math.cos(t + delta * Math.sin(t))), (float) (cy + kappa * a * Math.sin(t))};
    }

    /** A filled D, row by row (a triangle fan leaves seams between its triangles). */
    private static void dFill(GuiGraphics g, float cx, float cy, float a, float kappa, float delta, int side, int argb) {
        int n = 96;
        float[][] pts = new float[n][];
        for (int i = 0; i < n; i++) pts[i] = dPoint(cx, cy, a, kappa, delta, side, i / (double) n * 2 * Math.PI);
        int top = (int) Math.floor(cy - kappa * a), bottom = (int) Math.ceil(cy + kappa * a);
        for (int row = top; row <= bottom; row++) {
            float yc = row + 0.5F, lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                float[] p = pts[i], q = pts[(i + 1) % n];
                if ((p[1] <= yc) == (q[1] <= yc)) continue;
                float xi = p[0] + (yc - p[1]) / (q[1] - p[1]) * (q[0] - p[0]);
                lo = Math.min(lo, xi);
                hi = Math.max(hi, xi);
            }
            if (hi >= lo) FcsDraw.fill(g, Math.round(lo), row, Math.round(hi), row + 1, argb);
        }
    }

    private static void dLine(GuiGraphics g, float cx, float cy, float a, float kappa, float delta, int side, float width, int argb) {
        float[] prev = dPoint(cx, cy, a, kappa, delta, side, 0);
        for (int i = 1; i <= 48; i++) {
            float[] p = dPoint(cx, cy, a, kappa, delta, side, i / 48.0 * 2 * Math.PI);
            FcsDraw.line(g, prev[0], prev[1], p[0], p[1], width, argb);
            prev = p;
        }
    }

    private static void px(GuiGraphics g, float x, float y, int argb) {
        FcsDraw.fill(g, (float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(x) + 1, (float) Math.floor(y) + 1, argb);
    }

    /** The plasma's colour at a temperature (keV): dull red, orange, yellow-white. */
    static int heat(double kev) {
        double k = Math.max(0, Math.min(1, kev / 15));
        int r = (int) Math.round(150 + 105 * k), gr = (int) Math.round(40 + 180 * k * k), b = (int) Math.round(60 + 120 * k * k * k);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private void drawFigure(GuiGraphics g, int x, int y, double time) {
        int state = (int) v(0), tick = (int) Math.floor(time);
        double kev = v(1) / 100.0;
        boolean plasma = state == 3 || state == 4, hMode = plasma && v(42) == 1, heating = plasma && v(33) > 0, fusing = plasma && v(3) > 0;
        long cyclotron = v(20), beams = Math.max(0, v(19) - cyclotron);
        // an ELM: the count went up since the last frame
        int elms = (int) v(44);
        if (time < lastTime) elmAt = -100;
        if (lastElms >= 0 && elms > lastElms) elmAt = time;
        lastElms = elms;
        lastTime = time;
        double sinceElm = time - elmAt;
        // a disruption's age (its timer runs down from DISRUPTION_TICKS)
        double disruptAge = state == 5 ? FusionPlasma.DISRUPTION_TICKS - v(30) : -1;

        FcsDraw.fill(g, x, y, x + DW, y + DH, VOID);
        for (int i = 0; i < DW; i += 8) FcsDraw.fill(g, x + i, y + DH - 3, x + i + 4, y + DH - 2, FLOOR);
        // the coils' colour: frosting while they cool, red at a quench
        double cooled = state == 1 ? Math.min(1, v(12) / (double) FusionPlasma.COOLDOWN_TICKS) : 0;
        int coil = state == 6 ? (tick % 10 < 5 ? HOT_COIL : HOT_COIL_DIM) : state == 1 && cooled > 0.15 ? FROST : COPPER;

        // the central solenoid
        FcsDraw.fill(g, x + CS_X, y + 6, x + CS_X + 10, y + DH - 6, CS_CORE);
        for (int yy = 7; yy < DH - 7; yy += 3) FcsDraw.fill(g, x + CS_X + 1, y + yy, x + CS_X + 9, y + yy + 2, state == 6 ? coil : COPPER);
        double charge = state == 2 ? Math.min(1, v(13) / (double) FusionPlasma.STARTUP_ENERGY) : plasma ? 0.6 : 0;
        if (charge > 0) {
            FcsDraw.fill(g, x + CS_X - 1, y + 6, x + CS_X + 11, y + DH - 6, FcsDraw.argb(CHARGE & 0xFFFFFF, (float) (0.6 * charge)));
            for (int i = 0; i < 3; i++) px(g, x + CS_X + 4, y + 8 + Math.floorMod((int) (time * 0.6) + i * 20, DH - 16), CHARGE_DOT);
        }

        for (int side = -1; side <= 1; side += 2) {
            float cx = x + (side < 0 ? LEFT_CX : RIGHT_CX), cy = y + CY;
            // toroidal field coil, vessel, blanket, the channel
            dLine(g, cx, cy, 20, 1.45F, 0.35F, side, 3, coil);
            dFill(g, cx, cy, 15.5F, 1.5F, 0.38F, side, VESSEL);
            dLine(g, cx, cy, 15.5F, 1.5F, 0.38F, side, 1, VESSEL_EDGE);
            dFill(g, cx, cy, 14, 1.5F, 0.38F, side, BLANKET);
            dFill(g, cx, cy, 12, 1.5F, 0.4F, side, CHANNEL);
            // poloidal field coils above and below
            FcsDraw.fill(g, cx + side * 22, y + 4, cx + side * 22 + 5, y + 8, coil);
            FcsDraw.fill(g, cx + side * 22, y + DH - 8, cx + side * 22 + 5, y + DH - 4, coil);
            // the divertor plates under the X-point
            float xp = cx + side * 2;
            boolean strike = hMode && sinceElm < 12;
            FcsDraw.fill(g, xp - 6, cy + 20, xp - 2, cy + 22, strike ? ELM : TUNGSTEN);
            FcsDraw.fill(g, xp + 2, cy + 20, xp + 6, cy + 22, strike ? ELM : TUNGSTEN);
            // the plasma
            double fade = state == 5 ? Math.max(0, 1 - disruptAge / 30.0) : 1;
            double t = state == 5 ? Math.max(0, 15 * fade) : kev;
            if ((plasma || state == 5 && fade > 0) && t >= 0.5) {
                float[] radii = {10.5F, 8, 5.5F, 3};
                for (int i = 0; i < radii.length; i++) {
                    int c = i == 0 && hMode ? 0xFFC0507A : heat(t * (0.5 + i * 0.17));
                    dFill(g, cx, cy, radii[i], KAPPA, DELTA, side, FcsDraw.argb(c & 0xFFFFFF, (float) fade));
                }
                if (hMode) dLine(g, cx, cy, A, KAPPA, DELTA, side, 1, PEDESTAL);
                // the plasma current running round
                for (int i = 0; i < 10; i++) {
                    float[] p = dPoint(cx, cy, 9.5F, KAPPA, DELTA, side, i / 10.0 * 2 * Math.PI + time * 0.05 * side);
                    px(g, p[0], p[1], FcsDraw.argb(CURRENT & 0xFFFFFF, (float) fade));
                }
                // the separatrix legs to the divertor
                FcsDraw.line(g, xp, cy + 16, xp - 4, cy + 20, 1, FcsDraw.argb(PEDESTAL & 0xFFFFFF, (float) (0.8 * fade)));
                FcsDraw.line(g, xp, cy + 16, xp + 4, cy + 20, 1, FcsDraw.argb(PEDESTAL & 0xFFFFFF, (float) (0.8 * fade)));
            }
            if (fusing) {
                // neutrons flying out into the blanket, which sparks where they land; exhaust drawn off the divertor
                for (int i = 0; i < 5; i++) {
                    double ph = Math.floorMod((int) (time * 0.7) + i * 37 + side * 11, 40) / 40.0;
                    double an = i * 1.3 + side + Math.floorDiv((int) time + i * 40, 40);
                    double r = 4 + ph * 12;
                    px(g, (float) (cx + Math.cos(an) * r), (float) (cy + Math.sin(an) * r * 1.4), NEUTRON);
                    if (ph > 0.85) px(g, (float) (cx + Math.cos(an) * 14.5), (float) (cy + Math.sin(an) * 21), BLANKET_HIT);
                }
                for (int i = 0; i < 2; i++)
                    px(g, xp + (i == 0 ? -3 : 3), cy + 22 + Math.floorMod((int) (time * 0.5) + i * 15, 30) * 12 / 30F, EXHAUST);
            }
            // an ELM: the edge flashes
            if (hMode && sinceElm < 12) dLine(g, cx, cy, 11.5F, KAPPA, DELTA, side, 1, FcsDraw.argb(0xFFFFFF, (float) (1 - sinceElm / 12)));
            // a disruption: the heat strikes the first wall
            if (state == 5 && disruptAge < 16) dLine(g, cx, cy, 14, 1.5F, 0.38F, side, 2, FcsDraw.argb(DISRUPT & 0xFFFFFF, (float) (1 - disruptAge / 16)));
        }
        // the heating: a neutral beam from the right, microwaves from above
        if (heating && beams > 0) FcsDraw.dashed(g, x + DW - 2, y + CY - 6, x + RIGHT_CX + 3, y + CY - 2, 3, 1, BEAM);
        if ((heating || plasma && cyclotron >= 2) && cyclotron > 0)
            for (int i = 0; i < 14; i++) px(g, (float) (x + LEFT_CX + Math.sin(i * 1.2 + time * 0.4) * 1.5), y + 2 + i, MICROWAVE);
    }

    // ------------------------------------------------------------------ tooltips

    /** The part of the figure under window pixel ({@code rx}, {@code ry}), as a tip key, or null. */
    static String part(int rx, int ry) {
        if (rx < 0 || ry < 0 || rx >= DW || ry >= DH) return null;
        if (rx >= CS_X && rx < CS_X + 10 && ry >= 6 && ry < DH - 6) return "solenoid";
        for (int side = -1; side <= 1; side += 2) {
            int cx = side < 0 ? LEFT_CX : RIGHT_CX;
            int dx = side * (rx - cx), dy = ry - CY;
            if ((ry < 9 || ry > DH - 9) && Math.abs(rx - (cx + side * 24)) <= 4) return "poloidal";
            if (dy >= 18 && dy <= 23 && Math.abs(rx - (cx + side * 2)) <= 7) return "divertor";
            double e = Math.hypot(dx / 10.5, dy / (10.5 * KAPPA));
            if (e <= 1) return "plasma";
            if (e <= 1.32) return "blanket";
            if (e <= 1.5) return "vessel";
            if (e <= 2.0) return "coil";
        }
        if (rx >= DW - 20 && Math.abs(ry - CY + 4) <= 4) return "nbi";
        if (Math.abs(rx - LEFT_CX) <= 3 && ry < 16) return "ecrh";
        return "cryostat";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        List<Component> lines = null;
        String key = part(rx - DX, ry - DY);
        if (key != null) {
            lines = new ArrayList<>();
            lines.add(Component.translatable(K + "tip." + key + ".title"));
            lines.add(Component.translatable(K + "tip." + key).withStyle(ChatFormatting.GRAY));
            if (key.equals("cryostat")) lines.add(Component.translatable(K + "ports", v(18), v(19), v(23), v(22), v(21), v(24)).withStyle(ChatFormatting.DARK_GRAY));
        } else if (rx >= DX - 1 && ry >= RY && rx < DX + DW + 1 && ry < RY + RH && (v(0) == 3 || v(0) == 4)) {
            lines = balance();
        } else if (rx >= HEAT_X && rx < HEAT_X + 4 && ry >= FusionControllerMenu.GRID_Y - 1 && ry < FusionControllerMenu.GRID_Y + FusionBlanket.ROWS * 18) {
            lines = List.of(Component.translatable(K + "tip.heat", compact(v(10)), compact(v(11))), Component.translatable(K + "tip.input", compact(v(5)), compact(v(6)))
                .withStyle(ChatFormatting.GRAY));
        }
        if (lines != null) {
            // wrapped: the part descriptions (a sentence or two) to 200 px, the power balance's lines to 320
            int wrap = key != null ? 200 : 320;
            List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component c : lines) wrapped.addAll(font.split(c, wrap));
            g.renderTooltip(font, wrapped, mouseX, mouseY);
        }
    }

    /** 0.1.11.133: density, confinement time, triple product against the Lawson criterion, alpha heating against the losses. */
    private List<Component> balance() {
        String B = K + "balance.";
        double n = v(35) / 1000.0, tau = v(36) / 1000.0, triple = v(37) / 1000.0;
        int lawson = (int) Math.round(triple * 1.0e21 / FusionPlasma.LAWSON * 100);
        return List.of(
            Component.translatable(B + "title"),
            Component.translatable(B + "density", String.format(Locale.ROOT, "%.2f", n)).withStyle(ChatFormatting.GRAY),
            Component.translatable(B + "tau", String.format(Locale.ROOT, "%.2f", tau)).withStyle(ChatFormatting.GRAY),
            Component.translatable(B + "triple", String.format(Locale.ROOT, "%.2f", triple), lawson).withStyle(lawson >= 100 ? ChatFormatting.GREEN : ChatFormatting.GRAY),
            Component.translatable(B + "alpha", compact(v(38)), compact(v(39))).withStyle(ChatFormatting.GRAY),
            Component.translatable(B + "heating", compact(v(33))).withStyle(ChatFormatting.GRAY),
            // 0.1.11.134: the confinement mode and the operating limits
            Component.translatable(B + (v(42) == 1 ? "h_mode" : "l_mode"), v(44)).withStyle(v(42) == 1 ? ChatFormatting.AQUA : ChatFormatting.GRAY),
            Component.translatable(B + "limits", v(40) / 10, v(41) / 10).withStyle(Math.max(v(40), v(41)) >= 1000 ? ChatFormatting.RED
                : Math.max(v(40), v(41)) >= 900 ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
            // 0.1.11.135: the plant
            Component.translatable(B + "plant"),
            Component.translatable(B + "thermal", compact(v(45)), FusionBlanket.EFFICIENCY_PERCENT, compact(v(4))).withStyle(ChatFormatting.GRAY),
            Component.translatable(B + "multiplication", String.format(Locale.ROOT, "%.1f", v(46) / 100.0), String.format(Locale.ROOT, "%.2f", v(47) / 100.0))
                .withStyle(ChatFormatting.GRAY),
            // 0.1.11.136: the fuel cycle
            Component.translatable(B + "fuel_cycle", String.format(Locale.ROOT, "%.2f", v(50) / 100.0), Math.round(FusionPlasma.BURN_FRACTION * 100),
                String.format(Locale.ROOT, "%.2f", v(51) / 100.0)).withStyle(ChatFormatting.GRAY),
            Component.translatable(B + "tbr", String.format(Locale.ROOT, "%.2f", v(48) / 100.0)).withStyle(v(48) >= 100 ? ChatFormatting.GREEN : ChatFormatting.GRAY),
            Component.translatable(B + "q_eng", String.format(Locale.ROOT, "%.1f", v(49) / 100.0)).withStyle(ChatFormatting.GRAY));
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
