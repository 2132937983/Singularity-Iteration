// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.QuantumGeneratorMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.131: the quantum generator screen in the vanilla-frame generator layout (248 x 244, docs/gui_processing_machines.md).
 * The window is a pixel-art cross-section of the machine as its lore tells it (the manual's "background" section): inside the
 * iridium casing a quantum core holds a vacuum; virtual photon pairs flash up in the cavity; while the machine generates, the
 * field coils of the matter fabricator (top and bottom) pull most of them apart, one half flying to the photon resonance panel
 * on the left, the other to the concentrator panel on the right, which flash as they are hit, and packets leave along the
 * cable on the right. With a full storage the core dims, the coils go dark and the pairs only flash up and annihilate.
 * Under it the readouts (output and tier, storage, state, source) and the storage bar; the bolt on the left fills with the
 * storage. Was the solar generator texture with a sun icon and an English tooltip.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_quantum_generator extends MimicScreen<QuantumGeneratorMenu> {
    static final String K = "gui.mio_icif.quantum_generator.";
    private static final String[] TIERS = {"LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UXV", "OpV", "MAX"};

    // the figure, in window pixels (the window is 207 x 62)
    static final int CASE_X0 = 10, CASE_Y0 = 4, CASE_X1 = 197, CASE_Y1 = 58;
    static final int CAV_X0 = 14, CAV_Y0 = 8, CAV_X1 = 193, CAV_Y1 = 54;
    static final int LEFT_X0 = 16, LEFT_X1 = 24, RIGHT_X0 = 183, RIGHT_X1 = 191, PANEL_Y0 = 11, PANEL_Y1 = 51;
    static final int CORE_X = 103, CORE_Y = 31, CORE_R = 5;
    static final int[] COILS = {58, 103, 148};
    /** Pair slots, the ticks of a slot's cycle, the share pulled apart while generating, the speed of the halves (px/tick). */
    static final int PAIRS = 28, CYCLE = 36;
    static final double PULLED = 0.75, SPEED = 1.6;

    private static final int CASING = 0xFF6E7480, CAVITY = 0xFF141A2A, COIL = 0xFF5C6270, COIL_ON = 0xFF5FD3F5, COIL_DIM = 0xFF3F8BA6,
        COIL_OFF = 0xFF2A3140, FIELD = 0xFF22314A, RING = 0xFF3F5A8A, RES = 0xFF2B2F55, RES_CELL = 0xFF5B4FB0, RES_HIT = 0xFFB9A2FF,
        CON = 0xFF3A3326, CON_MIRROR = 0xFFB88A2E, CON_HIT = 0xFFFFE08A, ABSORBER = 0xFF7A4A22, ABSORBER_HIT = 0xFFFF9A4A,
        CORE_EDGE = 0xFF6B56C8, CORE_MID = 0xFFA98BF5, CORE_MID_HI = 0xFFC9B8FF, CORE_DIM = 0xFF8C86B8, PAIR = 0xFFFFFFFF,
        PAIR_FADE = 0xFF7F8AA0, BOLT = 0xFFF2C14E, CABLE = 0xFF8B8B8B;

    public mio_icif_gui_quantum_generator(QuantumGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private int v(int i) { return menu.value(i); }

    private long energy() { return Integer.toUnsignedLong(v(0)); }
    private long capacity() { return Math.max(1, Integer.toUnsignedLong(v(1))); }
    private boolean generating() { return v(5) > 0; }

    static String tierName(int tier) { return TIERS[Math.max(1, Math.min(TIERS.length, tier)) - 1]; }

    static String num(long n) { return String.format(Locale.ROOT, "%,d", n); }

    /** A fixed number in [0, 1) for a slot, a cycle and a channel. */
    static double unit(int slot, long cycle, int channel) {
        long h = slot * 0x9E3779B97F4A7C15L + cycle * 0xC2B2AE3D27D4EB4FL + channel * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        boolean on = generating();
        int tick = (int) Math.floor(time);
        // casing and cavity
        FcsDraw.fill(g, x + CASE_X0, y + CASE_Y0, x + CASE_X1, y + CASE_Y1, CASING);
        FcsDraw.fill(g, x + CAV_X0, y + CAV_Y0, x + CAV_X1, y + CAV_Y1, CAVITY);
        // the field of the coils: dashes drifting outwards from the core
        if (on) for (int row = CAV_Y0 + 6; row < CAV_Y1 - 5; row += 7)
            for (int px = 30; px < 178; px += 6) {
                int dx = Math.floorMod(px < CORE_X ? -tick : tick, 6);
                int fx = px + dx;
                if (fx > LEFT_X1 + 2 && fx < RIGHT_X0 - 3) FcsDraw.fill(g, x + fx, y + row, x + fx + 2, y + row + 1, FIELD);
            }
        // the field coils of the matter fabricator, top and bottom
        for (int cx : COILS) {
            int light = on ? (tick % 10 < 5 ? COIL_ON : COIL_DIM) : COIL_OFF;
            FcsDraw.fill(g, x + cx - 5, y + CAV_Y0, x + cx + 6, y + CAV_Y0 + 3, COIL);
            FcsDraw.fill(g, x + cx - 4, y + CAV_Y0 + 2, x + cx + 5, y + CAV_Y0 + 3, light);
            FcsDraw.fill(g, x + cx - 5, y + CAV_Y1 - 3, x + cx + 6, y + CAV_Y1, COIL);
            FcsDraw.fill(g, x + cx - 4, y + CAV_Y1 - 3, x + cx + 5, y + CAV_Y1 - 2, light);
        }
        // the pairs (worked out first: the panels flash when a half arrives)
        boolean hitLeft = false, hitRight = false;
        int[][] pairs = new int[PAIRS][];
        for (int k = 0; k < PAIRS; k++) {
            double t = time + unit(k, -1, 0) * CYCLE;
            long cycle = (long) Math.floor(t / CYCLE);
            double age = t - cycle * CYCLE;
            int px = 36 + (int) (unit(k, cycle, 1) * 134), py = CAV_Y0 + 5 + (int) (unit(k, cycle, 2) * (CAV_Y1 - CAV_Y0 - 10));
            boolean pulled = on && unit(k, cycle, 3) < PULLED;
            pairs[k] = new int[]{px, py, pulled ? 1 : 0, (int) Math.floor(age * 4)};
            if (pulled) {
                double reachL = 3 + (px - LEFT_X1) / SPEED, reachR = 3 + (RIGHT_X0 - px - 1) / SPEED;
                if (age >= reachL && age < reachL + 4) hitLeft = true;
                if (age >= reachR && age < reachR + 4) hitRight = true;
            }
        }
        // the photon resonance panel (left) and the concentrator with its absorber (right)
        FcsDraw.fill(g, x + LEFT_X0, y + PANEL_Y0, x + LEFT_X1, y + PANEL_Y1, RES);
        for (int r = 0; r < 5; r++) for (int c = 0; c < 2; c++)
            FcsDraw.fill(g, x + LEFT_X0 + 1 + c * 4, y + PANEL_Y0 + 1 + r * 8, x + LEFT_X0 + 4 + c * 4, y + PANEL_Y0 + 8 + r * 8, hitLeft ? RES_HIT : RES_CELL);
        FcsDraw.fill(g, x + RIGHT_X0, y + PANEL_Y0, x + RIGHT_X1, y + PANEL_Y1, CON);
        for (int r = 0; r < 10; r++)
            FcsDraw.fill(g, x + RIGHT_X0 + 1 + (r % 2), y + PANEL_Y0 + 1 + r * 4, x + RIGHT_X1 - 1, y + PANEL_Y0 + 4 + r * 4, hitRight ? CON_HIT : CON_MIRROR);
        FcsDraw.fill(g, x + RIGHT_X1 - 2, y + PANEL_Y0, x + RIGHT_X1, y + PANEL_Y1, hitRight ? ABSORBER_HIT : ABSORBER);
        // the confinement ring round the core
        if (on) {
            int rr = 8 + Math.floorMod(tick / 2, 6);
            for (int a = 0; a < 32; a += 2) {
                int rx = Math.round(CORE_X + (float) Math.cos(a / 32.0 * 2 * Math.PI) * rr * 1.6F);
                int ry = Math.round(CORE_Y + (float) Math.sin(a / 32.0 * 2 * Math.PI) * rr * 0.8F);
                if (ry > CAV_Y0 + 3 && ry < CAV_Y1 - 4) FcsDraw.fill(g, x + rx, y + ry, x + rx + 1, y + ry + 1, RING);
            }
        }
        // the quantum core: a diamond breathing while the machine generates
        boolean bright = on && Math.sin(time / 6) > 0;
        for (int dy = -CORE_R; dy <= CORE_R; dy++) {
            int w = CORE_R - Math.abs(dy);
            for (int dx = -w; dx <= w; dx++) {
                int e = Math.abs(dx) + Math.abs(dy);
                int c = e <= 1 ? (on ? PAIR : CORE_DIM) : e <= 3 ? (bright ? CORE_MID_HI : CORE_MID) : CORE_EDGE;
                FcsDraw.fill(g, x + CORE_X + dx, y + CORE_Y + dy, x + CORE_X + dx + 1, y + CORE_Y + dy + 1, c);
            }
        }
        // the pairs: an annihilating pair flashes up and fades; a pulled pair splits, its halves running to the panels
        for (int[] p : pairs) {
            int px = p[0], py = p[1];
            double age = p[3] / 4.0;
            if (p[2] == 0) {
                if (age < 7) {
                    int c = age < 5 ? PAIR : PAIR_FADE;
                    FcsDraw.fill(g, x + px, y + py, x + px + 2, y + py + 1, c);
                }
                continue;
            }
            int d = (int) Math.round(Math.max(0, age - 3) * SPEED);
            if (d == 0) { FcsDraw.fill(g, x + px, y + py, x + px + 2, y + py + 1, PAIR); continue; }
            int lx = px - d, rx = px + 1 + d;
            if (lx >= LEFT_X1) {
                FcsDraw.fill(g, x + lx, y + py, x + lx + 1, y + py + 1, RES_HIT);
                FcsDraw.fill(g, x + lx + 1, y + py, x + Math.min(px, lx + 3), y + py + 1, RES_CELL);
            }
            if (rx < RIGHT_X0) {
                FcsDraw.fill(g, x + rx, y + py, x + rx + 1, y + py + 1, CON_HIT);
                FcsDraw.fill(g, x + Math.max(px + 1, rx - 2), y + py, x + rx, y + py + 1, CON_MIRROR);
            }
        }
        // the cable out of the casing, packets running along it
        FcsDraw.fill(g, x + CASE_X1, y + 29, x + 207, y + 33, COIL);
        FcsDraw.fill(g, x + CASE_X1, y + 30, x + 207, y + 32, CABLE);
        if (on) for (int k = 0; k < 3; k++) {
            int px = CASE_X1 + Math.floorMod(tick + k * 4, 12);
            if (px < 206) FcsDraw.fill(g, x + px, y + 30, x + px + 1, y + 32, BOLT);
        }

        // the readouts
        int ink = WindRoseView.INK, dim = WindRoseView.DIM;
        line(g, x, y, 0, Component.translatable(K + "line_output", num(v(5)), tierName(v(4))), ink);
        line(g, x, y, 1, Component.translatable(K + "line_stored", num(energy()), num(capacity())), ink);
        String state;
        int colour;
        if (on) { state = "state.run"; colour = 0x2F6E4F; }
        else if (energy() >= capacity()) { state = "state.full"; colour = dim; }
        else { state = "state.off"; colour = 0xC0392B; }
        ReadoutPanel.headline(Component.literal(num(v(5)) + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads
        line(g, x, y, 3, Component.translatable(K + "line_source"), dim);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        double share = Math.min(1.0, energy() / (double) capacity());
        VanillaFrame.bolt(g, leftPos + 13, topPos + 39, share);   // 0.1.11.141: as the processing machines
        bar(g, x, y, Component.translatable(K + "stored"), share, Component.literal(Math.round(share * 100) + "%"));
    }

    /** The tooltip under the mouse (window-relative for the figure), or null. */
    static List<Component> tip(int rx, int ry, int wx, int wy) {
        int fx = rx - wx, fy = ry - wy;
        if (rx >= 14 && rx < 23 && ry >= 38 && ry < 51) return tips("tip.bolt");
        if (fx < 0 || fy < 0 || fx >= 207 || fy >= 62) return null;
        if (Math.abs(fx - CORE_X) + Math.abs(fy - CORE_Y) <= CORE_R + 1) return tips("tip.core");
        if (fx >= LEFT_X0 && fx < LEFT_X1 && fy >= PANEL_Y0 && fy < PANEL_Y1) return tips("tip.resonance");
        if (fx >= RIGHT_X0 && fx < RIGHT_X1 && fy >= PANEL_Y0 && fy < PANEL_Y1) return tips("tip.concentrator");
        for (int cx : COILS)
            if (Math.abs(fx - cx) <= 5 && (fy >= CAV_Y0 && fy < CAV_Y0 + 3 || fy >= CAV_Y1 - 3 && fy < CAV_Y1)) return tips("tip.coil");
        if (fx >= CAV_X0 && fx < CAV_X1 && fy >= CAV_Y0 && fy < CAV_Y1) return tips("tip.cavity");
        if (fx >= CASE_X0 && fx < CASE_X1 && fy >= CASE_Y0 && fy < CASE_Y1) return tips("tip.casing");
        if (fx >= CASE_X1 && fy >= 29 && fy < 33) return tips("tip.cable");
        return null;
    }

    private static List<Component> tips(String key) {
        return List.of(Component.translatable(K + key + ".title"),
            Component.translatable(K + key).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> lines = tip(mouseX - leftPos, mouseY - topPos, wx, wy);
        if (lines != null) g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }
}
