package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.PumpSurvey;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.menu.producer.PumpElcMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Pump screen. 0.1.11.7: 248 x 222. 0.1.11.147 (calm layout, docs/gui_processing_machines.md): the tank on the left
 * with the energy bolt under it, the cross-section of the pool or basin across the top ({@link PumpCrossSection}) with
 * the pump / discharge and waterlogged switches as icons in its corner; under it the main readout (buckets in the pool
 * or room in the basin, twice the font size) with the state beside it, then the fluid, flow and lift, and the time
 * left; the keep-level and target-height steppers sit in the slot row beside the battery and container slots.
 *
 * <p>0.1.11.181: the vanilla 176 x 240, merged (docs/gui_processing_machines.md, "合并布局"): the cross-section across the top
 * (160 wide, was 210) with its two icons in the corner; under it the pump's own row: the battery with the bolt on it, the
 * tank, its pipe down to the filler with the empty container on one side and the filled one on the other, the keep-level
 * and target-height setters on the right (their names in the tooltips); then the readouts.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_pump_elc extends mio_icif_screen<PumpElcMenu> {

    private static final int TANK_X = 36, TANK_Y = 79, TANK_W = 14, TANK_H = 30;
    private static final int VIEW_X = 8, VIEW_Y = 16, VIEW_W = 160, VIEW_H = 56;
    private static final int BOLT_X = 13, BOLT_Y = 78, HEAD_Y = 114, LINE1_Y = 133, LINE2_Y = 143;
    private static final int STEP_X = 112, KEEP_Y = 80, TARGET_Y = 97, STEP = 11, FILLER_X = 75, FILLER_Y = 88;
    private static final String K = "gui.mio_icif.pump.";
    private static final String[] ICON_DROP = {"...#...", "..###..", ".#####.", "#######", "#######", ".#####.", "..###.."};

    /** Clickable controls: x, y, w, h, button id (-2 / -3: keep level down / up; 8 / 9: target height down / up). */
    private static final int[][] CONTROLS = {
        {VIEW_X + VIEW_W - 26, VIEW_Y + 2, 11, 11, 0}, {VIEW_X + VIEW_W - 13, VIEW_Y + 2, 11, 11, 1},
        {STEP_X, KEEP_Y, STEP, STEP, -2}, {168 - STEP, KEEP_Y, STEP, STEP, -3},
        {STEP_X, TARGET_Y, STEP, STEP, 8}, {168 - STEP, TARGET_Y, STEP, STEP, 9},
    };

    private final PumpCrossSection view = new PumpCrossSection();

    public mio_icif_gui_pump_elc(PumpElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PumpElcMenu.GUI_WIDTH;
        this.imageHeight = PumpElcMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    private boolean discharge() { return v(7) == mio_icif_pump_elc.MODE_DISCHARGE; }

    /** 0.1.11.147: the calm layout (title on the left, no lamp or upgrade hint, window text in the vanilla font). */
    @Override
    protected boolean calmChrome() { return true; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;   // docked upgrade slots are drawn by the dock
            VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        // 0.1.11.181: the tank, its pipe down to the filler between the two container slots, the stored energy on the battery
        FluidStack tank = menu.getFluidStack();
        MachineArt.tank(g, x + TANK_X, y + TANK_Y, TANK_W, TANK_H, tank, menu.getFluidCapacity());
        int filler = (int) ((FILLER_X + 8));
        MachineArt.pipe(g, x + TANK_X + TANK_W + 2, x + filler + 2, y + 82, tank.isEmpty() ? 0 : MachineArt.WATER, -1);
        MachineArt.pipeDown(g, x + filler, y + 82, y + FILLER_Y, tank.isEmpty() ? 0 : MachineArt.WATER, -1);
        MachineArt.filler(g, x + FILLER_X, y + FILLER_Y, PumpElcMenu.OUTPUT_X - 1 - FILLER_X);
        long e = Integer.toUnsignedLong(menu.getEnergy()), max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        VanillaFrame.bolt(g, x + BOLT_X, y + BOLT_Y, e / (double) max);
        // the cross-section
        VanillaFrame.inset(g, x + VIEW_X - 1, y + VIEW_Y - 1, VIEW_W + 2, VIEW_H + 2, VanillaFrame.SLOT);
        view.render(g, x + VIEW_X, y + VIEW_Y, VIEW_W, VIEW_H, data());
        readings(g, x, y);
        controls(g, x, y, mouseX, mouseY);
    }

    private float u() { return WindRoseView.u(); }

    private PumpCrossSection.Data data() {
        byte[] low = new byte[PumpSurvey.COLUMNS], high = new byte[PumpSurvey.COLUMNS];
        for (int i = 0; i < 8; i++) {
            int a = v(19 + i), b = v(27 + i);
            for (int k = 0; k < 4; k++) { low[i * 4 + k] = (byte) (a >>> (8 * k)); high[i * 4 + k] = (byte) (b >>> (8 * k)); }
        }
        int cols = v(17);
        int active = (cols >> 8) & 0xFF;
        return new PumpCrossSection.Data(v(7), v(11), v(12), v(15), v(16), low, high, cols & 0xFF, active == 0xFF ? -1 : active,
            v(13), bodyFluid(), menu.getProgress(), menu.getProgress() > 0 || isWorkingNow(), v(8), v(10));
    }

    private boolean isWorkingNow() { return menu.getProgress() > 0; }

    private Fluid bodyFluid() {
        int id = v(18);
        if (id < 0) return null;
        Fluid f = BuiltInRegistries.FLUID.byId(id);
        return f == null || f.isSame(net.minecraft.world.level.material.Fluids.EMPTY) ? null : f;
    }

    // ------------------------------------------------------------------ readings

    private void readings(GuiGraphics g, int x, int y) {
        int flags = v(12);
        boolean dis = discharge();
        String stateKey = (flags & mio_icif_pump_elc.FLAG_HOLD) != 0 ? "state.hold"
            : (flags & mio_icif_pump_elc.FLAG_TANK_FULL) != 0 ? "state.tank_full"
            : (flags & mio_icif_pump_elc.FLAG_NO_BUCKET) != 0 ? "state.no_bucket"
            : (flags & mio_icif_pump_elc.FLAG_NO_PLACE) != 0 ? "state.no_place"
            : dis && (flags & mio_icif_pump_elc.FLAG_BASIN_FULL) != 0 ? "state.basin_full"
            : isWorkingNow() ? "state.run" : "state.idle";
        boolean warn = !stateKey.equals("state.run") && !stateKey.equals("state.idle");
        int stateColour = warn ? 0xC0392B : stateKey.equals("state.run") ? 0x2F6E4F : 0x5B6867;
        int count = v(11), ppt = Math.max(1, v(14));
        boolean scanning = (flags & mio_icif_pump_elc.FLAG_SCANNING) != 0, none = (flags & mio_icif_pump_elc.FLAG_NO_BODY) != 0 || count < 0;
        boolean infinite = !dis && (flags & mio_icif_pump_elc.FLAG_INFINITE) != 0, capped = (flags & mio_icif_pump_elc.FLAG_CAPPED) != 0;
        // the main readout: the buckets to pump (or the room to fill), else the mode
        Component head = scanning || none ? Component.translatable(K + (dis ? "mode.discharge" : "mode.pump"))
            : dis ? Component.translatable(K + "basin_free", grouped(count))
            : infinite ? Component.translatable(K + "infinite")
            : Component.translatable(K + (capped ? "pool_capped" : "pool"), grouped(count));
        Component side = Component.translatable(K + stateKey);
        int left = x + VIEW_X - 1, right = x + VIEW_X + VIEW_W + 1;   // 0.1.11.181: x 7..169
        VanillaFrame.bigText(g, fit(head, (right - left - font.width(side) - 8) / 2), left, y + HEAD_Y, 0x1C2524);
        g.drawString(font, side, right - font.width(side), y + HEAD_Y + 4, stateColour, false);
        // the fluid, flow and lift; then the time left (or why there is none)
        Fluid fluid = bodyFluid();
        net.minecraft.network.chat.MutableComponent l1 = (fluid == null ? Component.literal("\u2014") : fluid.getFluidType().getDescription().copy())
            .append(" \u00B7 ").append(Component.translatable(K + "flow", String.format(Locale.ROOT, ppt == 1 ? "%.0f" : "%.1f", (double) ppt)));
        int lift = v(13);
        if (lift != Integer.MIN_VALUE) l1.append(" \u00B7 ").append(dis ? Component.translatable(K + "drop", String.valueOf(Math.max(0, -lift)))
            : lift <= 0 ? Component.translatable(K + "lift", String.valueOf(-lift)) : Component.translatable(K + "gravity", String.valueOf(lift)));
        g.drawString(font, fit(l1, right - left), left, y + LINE1_Y, ReadoutPanel.TEXT, false);
        Component l2;
        if (scanning) l2 = Component.translatable(K + "scanning");
        else if (none) l2 = Component.translatable(K + (dis ? "no_basin" : "no_body"));
        else if (dis) l2 = Component.translatable(K + "eta", time(count / (double) ppt));
        else if (infinite) l2 = Component.translatable(K + "eta_never");
        else if ((flags & mio_icif_pump_elc.FLAG_TANK_FULL) != 0) l2 = Component.translatable(K + "eta_tank_full");
        else l2 = Component.translatable(K + (capped ? "eta_more" : "eta"), time(Math.max(0, count - v(8)) / (double) ppt));
        if (fluid != null) l2 = l2.copy().append(" \u00B7 ").append(Component.translatable(K + "props",
            String.valueOf(fluid.getFluidType().getTemperature()), String.valueOf(fluid.getFluidType().getDensity())));
        g.drawString(font, fit(l2, right - left), left, y + LINE2_Y, ReadoutPanel.MUTED, false);
    }

    /** {@code c} cut to {@code maxW} pixels with an ellipsis. */
    private Component fit(Component c, int maxW) {
        if (font.width(c) <= maxW) return c;
        return Component.literal(font.plainSubstrByWidth(c.getString(), Math.max(0, maxW - font.width("\u2026"))) + "\u2026");
    }

    private static String grouped(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    private static String time(double seconds) {
        long s = Math.round(seconds);
        if (s >= 3600) return Component.translatable(K + "time.hm", s / 3600, (s % 3600) / 60).getString();
        if (s >= 60) return Component.translatable(K + "time.ms", s / 60, s % 60).getString();
        return Component.translatable(K + "time.s", s).getString();
    }

    // ------------------------------------------------------------------ controls

    /** Whether control {@code id} works in the current mode (keep level: pumping; target height: discharging). */
    private boolean active(int id) {
        boolean dis = discharge();
        return id == -2 || id == -3 ? !dis : id == 8 || id == 9 ? dis : true;
    }

    private void controls(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        boolean dis = discharge();
        for (int[] c : CONTROLS) {
            int bx = x + c[0], by = y + c[1];
            boolean hover = mouseX >= bx && mouseX < bx + c[2] && mouseY >= by && mouseY < by + c[3];
            switch (c[4]) {
                case 0 -> icon(g, bx, by, ProcessInstrumentScreen.ICON_CYCLE, dis, hover);
                case 1 -> icon(g, bx, by, ICON_DROP, v(9) != 0, hover);
                default -> stepButton(g, bx, by, c[4] == -2 || c[4] == 8 ? "\u2212" : "+", active(c[4]), hover);
            }
        }
        // the setter values between their buttons (0.1.11.181: the keep level as the bare number, its name in the tooltip)
        Component keep = Component.literal(grouped(v(8)));
        Component target = Component.translatable(K + "target.value", (v(10) >= 0 ? "+" : "") + v(10));
        int mid = STEP_X + STEP + (168 - STEP - STEP_X - STEP) / 2;
        g.drawString(font, keep, x + mid - font.width(keep) / 2, y + KEEP_Y + 2, dis ? 0x9AA0A6 : 0x1C2524, false);
        g.drawString(font, target, x + mid - font.width(target) / 2, y + TARGET_Y + 2, dis ? 0x1C2524 : 0x9AA0A6, false);
    }

    /** A small flat icon button (on: pressed in, white). */
    private static void icon(GuiGraphics g, int x, int y, String[] rows, boolean on, boolean hover) {
        if (on || hover) g.fill(x, y, x + 11, y + 11, on ? 0xFF6B7176 : 0x30FFFFFF);
        int ink = on ? 0xFFFFFFFF : 0xFF3C4245;
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++) if (rows[j].charAt(i) == '#') g.fill(x + 2 + i, y + 2 + j, x + 3 + i, y + 3 + j, ink);
    }

    /** A small vanilla raised button with a sign; greyed when it does nothing in this mode. */
    private void stepButton(GuiGraphics g, int x, int y, String sign, boolean active, boolean hover) {
        g.fill(x, y, x + STEP, y + STEP, VanillaFrame.SHADOW);
        g.fill(x, y, x + STEP - 1, y + STEP - 1, VanillaFrame.LIGHT);
        g.fill(x + 1, y + 1, x + STEP - 1, y + STEP - 1, active && hover ? 0xFFD8D8E8 : 0xFFB8B8B8);
        g.drawString(font, sign, x + (STEP - font.width(sign) + 1) / 2, y + 2, active ? 0x404040 : 0x9AA0A6, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int[] c : CONTROLS) {
                int bx = leftPos + c[0], by = topPos + c[1];
                if (mouseX < bx || mouseX >= bx + c[2] || mouseY < by || mouseY >= by + c[3]) continue;
                int id = c[4];
                if (!active(id)) return true;
                if (id == -2 || id == -3) {
                    int step = Screen.hasControlDown() ? 6 : Screen.hasShiftDown() ? 4 : 2;
                    id = step + (id == -3 ? 1 : 0);
                }
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> tip = new ArrayList<>();
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (hoveredSlot != null) return;   // 0.1.11.181: a slot shows its own tooltip only
        if (rx >= TANK_X - 2 && rx < TANK_X + TANK_W + 2 && ry >= TANK_Y - 2 && ry < TANK_Y + TANK_H + 2) {
            FluidStack f = menu.getFluidStack();
            tip.add(f.isEmpty() ? Component.translatable(K + "tip.tank_empty")
                : Component.translatable(K + "tip.tank", f.getHoverName(), grouped(menu.getFluidAmount()), grouped(menu.getFluidCapacity())));
        } else if (rx >= BOLT_X - 1 && rx < BOLT_X + 10 && ry >= BOLT_Y - 1 && ry < BOLT_Y + 14) {
            tip.add(Component.translatable(K + "tip.energy", Integer.toUnsignedLong(menu.getEnergy()), Integer.toUnsignedLong(menu.getMaxEnergy())));
        } else {
            for (int[] c : CONTROLS) {
                if (rx < c[0] || rx >= c[0] + c[2] || ry < c[1] || ry >= c[1] + c[3]) continue;
                switch (c[4]) {
                    case 0 -> { tip.add(Component.translatable(K + (discharge() ? "mode.discharge" : "mode.pump"))); tip.add(Component.translatable(K + "tip.mode")); }
                    case 1 -> { tip.add(Component.translatable(K + (v(9) != 0 ? "waterlogged.on" : "waterlogged.off"))); tip.add(Component.translatable(K + "tip.waterlogged")); }
                    case -2, -3 -> { tip.add(Component.translatable(K + "keep.value", grouped(v(8)))); tip.add(Component.translatable(K + "tip.keep"));
                        tip.add(Component.translatable(K + "tip.keep_step")); }
                    default -> tip.add(Component.translatable(K + "tip.target"));
                }
            }
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
    }
}
