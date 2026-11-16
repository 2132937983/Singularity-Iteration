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
 * Pump screen. 0.1.11.7: 248 x 222. The tank on the left, the cross-section of the pool or basin in the middle
 * ({@link PumpCrossSection}), the readings on the right (buckets in the pool, time to empty it, flow, lift, the fluid's
 * temperature, density and viscosity), the controls under the view (pump / discharge, waterlogged blocks, keep level,
 * target height) and the battery and container slots in one row (the upgrade slots sit in the side dock).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_pump_elc extends mio_icif_screen<PumpElcMenu> {

    private static final int TANK_X = 8, TANK_Y = 16, TANK_W = 16, TANK_H = 80;
    private static final int ENERGY_X = 25, ENERGY_W = 3;
    private static final int VIEW_X = 31, VIEW_Y = 16, VIEW_W = 146, VIEW_H = 80;
    private static final int INFO_X = 180, INFO_W = 61;
    private static final int CTRL_Y = 101, CTRL_H = 12;
    private static final String K = "gui.mio_icif.pump.";

    /** Controls: x, width, button id (-1 = a value box). */
    private static final int[][] CONTROLS = {
        {31, 44, 0}, {78, 40, 1},
        {121, 10, -2}, {133, 30, -1}, {165, 10, -3},
        {180, 10, 8}, {192, 37, -4}, {231, 10, 9},
    };

    private final PumpCrossSection view = new PumpCrossSection();

    public mio_icif_gui_pump_elc(PumpElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PumpElcMenu.GUI_WIDTH;
        this.imageHeight = PumpElcMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    private boolean discharge() { return v(7) == mio_icif_pump_elc.MODE_DISCHARGE; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        // machine slots: battery, container in -> out, four upgrades
        int sy = y + PumpElcMenu.SLOT_ROW_Y - 1;
        SiGuiTheme.slot(g, x + PumpElcMenu.BATTERY_X - 1, sy);
        SiGuiTheme.slot(g, x + PumpElcMenu.CONTAINER_INPUT_X - 1, sy);
        SiGuiTheme.slot(g, x + PumpElcMenu.OUTPUT_X - 1, sy);
        PumpCrossSection.text(g, Component.literal("›"), x + 120, y + PumpElcMenu.SLOT_ROW_Y + 4, 0x80868D, u());
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            SiGuiTheme.slot(g, x + PumpElcMenu.INVENTORY_X - 1 + c * 18, y + PumpElcMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + PumpElcMenu.INVENTORY_X - 1 + c * 18, y + PumpElcMenu.HOTBAR_Y - 1);

        // tank and energy
        g.fill(x + TANK_X - 1, y + TANK_Y - 1, x + TANK_X + TANK_W + 1, y + TANK_Y + TANK_H + 1, SiGuiTheme.SLOT_SHADOW);
        g.fill(x + TANK_X, y + TANK_Y, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, SiGuiTheme.SCREEN_BG);
        FluidStack tank = menu.getFluidStack();
        if (!tank.isEmpty()) mio_icif_GuiUtils.renderFluidBar(g, x + TANK_X, y + TANK_Y, TANK_W, TANK_H, tank, menu.getFluidCapacity());
        for (int i = 1; i < 8; i++) g.fill(x + TANK_X + (i % 2 == 0 ? 9 : 12), y + TANK_Y + i * 10, x + TANK_X + TANK_W, y + TANK_Y + i * 10 + 1, 0x80303438);
        g.fill(x + ENERGY_X - 1, y + TANK_Y - 1, x + ENERGY_X + ENERGY_W + 1, y + TANK_Y + TANK_H + 1, SiGuiTheme.SLOT_SHADOW);
        g.fill(x + ENERGY_X, y + TANK_Y, x + ENERGY_X + ENERGY_W, y + TANK_Y + TANK_H, SiGuiTheme.SCREEN_BG);
        long e = Integer.toUnsignedLong(menu.getEnergy()), max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        int eh = (int) Math.round(TANK_H * Math.min(1.0, e / (double) max));
        if (eh > 0) g.fillGradient(x + ENERGY_X, y + TANK_Y + TANK_H - eh, x + ENERGY_X + ENERGY_W, y + TANK_Y + TANK_H, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);

        // the cross-section
        g.fill(x + VIEW_X - 1, y + VIEW_Y - 1, x + VIEW_X + VIEW_W + 1, y + VIEW_Y + VIEW_H + 1, SiGuiTheme.SLOT_SHADOW);
        view.render(g, x + VIEW_X, y + VIEW_Y, VIEW_W, VIEW_H, data());

        // the readings
        g.fill(x + INFO_X - 1, y + VIEW_Y - 1, x + INFO_X + INFO_W + 1, y + VIEW_Y + VIEW_H + 1, SiGuiTheme.SLOT_SHADOW);
        g.fill(x + INFO_X, y + VIEW_Y, x + INFO_X + INFO_W, y + VIEW_Y + VIEW_H, SiGuiTheme.SCREEN_BG);
        info(g, x + INFO_X + 2, y + VIEW_Y + 2, INFO_W - 4);

        controls(g, x, y, mouseX, mouseY);
    }

    private float u() {
        return (float) Math.min(1.0, 2.0 / Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale()));
    }

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

    private void info(GuiGraphics g, float x, float y, float w) {
        float u = u(), lh = 9 * u + 1.5F;
        int flags = v(12);
        boolean dis = discharge();
        Component mode = Component.translatable(K + (dis ? "mode.discharge" : "mode.pump"));
        String stateKey = (flags & mio_icif_pump_elc.FLAG_HOLD) != 0 ? "state.hold"
            : (flags & mio_icif_pump_elc.FLAG_TANK_FULL) != 0 ? "state.tank_full"
            : (flags & mio_icif_pump_elc.FLAG_NO_BUCKET) != 0 ? "state.no_bucket"
            : (flags & mio_icif_pump_elc.FLAG_NO_PLACE) != 0 ? "state.no_place"
            : dis && (flags & mio_icif_pump_elc.FLAG_BASIN_FULL) != 0 ? "state.basin_full"
            : isWorkingNow() ? "state.run" : "state.idle";
        Component state = Component.translatable(K + stateKey);
        boolean warn = !stateKey.equals("state.run") && !stateKey.equals("state.idle");
        line(g, mode, x, y, 0x2A2E33, w - AsciiHud.width(state, u) - 2);
        PumpCrossSection.text(g, state, x + w - AsciiHud.width(state, u), y, warn ? 0xC8423C : stateKey.equals("state.run") ? 0x3C9A52 : 0x80868D, u);
        y += lh;
        Fluid fluid = bodyFluid();
        line(g, fluid == null ? Component.literal("—") : fluid.getFluidType().getDescription(), x, y, 0x2A2E33, w);
        y += lh;
        int count = v(11), ppt = Math.max(1, v(14));
        if ((flags & mio_icif_pump_elc.FLAG_SCANNING) != 0) {
            line(g, Component.translatable(K + "scanning"), x, y, 0x80868D, w);
            y += lh;
        } else if ((flags & mio_icif_pump_elc.FLAG_NO_BODY) != 0 || count < 0) {
            line(g, Component.translatable(K + (dis ? "no_basin" : "no_body")), x, y, 0x80868D, w);
            y += lh;
        } else if (dis) {
            line(g, Component.translatable(K + "basin_free", grouped(count)), x, y, 0x2A2E33, w);
            y += lh;
            line(g, Component.translatable(K + "eta", time(count / (double) ppt)), x, y, 0x2A2E33, w);
            y += lh;
        } else if ((flags & mio_icif_pump_elc.FLAG_INFINITE) != 0) {
            line(g, Component.translatable(K + "infinite"), x, y, 0x3A6EA5, w);
            y += lh;
            line(g, Component.translatable(K + "eta_never"), x, y, 0x80868D, w);
            y += lh;
        } else {
            boolean capped = (flags & mio_icif_pump_elc.FLAG_CAPPED) != 0;
            line(g, Component.translatable(K + (capped ? "pool_capped" : "pool"), grouped(count)), x, y, 0x2A2E33, w);
            y += lh;
            int left = Math.max(0, count - v(8));
            Component eta = (flags & mio_icif_pump_elc.FLAG_TANK_FULL) != 0 ? Component.translatable(K + "eta_tank_full")
                : Component.translatable(K + (capped ? "eta_more" : "eta"), time(left / (double) ppt));
            line(g, eta, x, y, 0x2A2E33, w);
            y += lh;
        }
        line(g, Component.translatable(K + "flow", String.format(Locale.ROOT, ppt == 1 ? "%.0f" : "%.1f", (double) ppt)), x, y, 0x80868D, w);
        y += lh;
        int lift = v(13);
        if (lift != Integer.MIN_VALUE) {
            Component lc = dis ? Component.translatable(K + "drop", String.valueOf(Math.max(0, -lift)))
                : lift <= 0 ? Component.translatable(K + "lift", String.valueOf(-lift)) : Component.translatable(K + "gravity", String.valueOf(lift));
            line(g, lc, x, y, 0x80868D, w);
            y += lh;
        }
        if (fluid != null) {
            var type = fluid.getFluidType();
            line(g, Component.translatable(K + "props", String.valueOf(type.getTemperature()), String.valueOf(type.getDensity())), x, y, 0x80868D, w);
            y += lh;
            line(g, Component.translatable(K + "viscosity", String.format(Locale.ROOT, "%.1f", type.getViscosity() / 1000.0)), x, y, 0x80868D, w);
        }
    }

    private void line(GuiGraphics g, Component c, float x, float y, int rgb, float maxW) {
        float u = u();
        if (AsciiHud.width(c, u) > maxW) {
            String s = c.getString();
            int n = s.length();
            while (n > 1 && AsciiHud.width(s.substring(0, n) + "…", u) > maxW) n--;
            c = Component.literal(s.substring(0, n) + "…");
        }
        PumpCrossSection.text(g, c, x, y, rgb, u);
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

    private void controls(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        float u = u();
        boolean dis = discharge();
        for (int[] c : CONTROLS) {
            int bx = x + c[0], by = y + CTRL_Y;
            boolean button = c[2] != -1 && c[2] != -4;
            boolean active = !(c[2] == -2 || c[2] == -3 || c[2] == -1) || !dis;
            if (c[2] == 8 || c[2] == 9 || c[2] == -4) active = dis;
            boolean hover = button && active && mouseX >= bx && mouseX < bx + c[1] && mouseY >= by && mouseY < by + CTRL_H;
            if (button) {
                g.fill(bx, by, bx + c[1], by + CTRL_H, SiGuiTheme.OUTLINE);
                g.fill(bx + 1, by + 1, bx + c[1] - 1, by + CTRL_H - 1, hover ? 0xFFF4F6F8 : active ? 0xFFE6E8EA : 0xFFD0D3D6);
                g.fill(bx + 1, by + CTRL_H - 2, bx + c[1] - 1, by + CTRL_H - 1, SiGuiTheme.BEVEL_LO);
            } else {
                g.fill(bx, by, bx + c[1], by + CTRL_H, SiGuiTheme.SLOT_SHADOW);
                g.fill(bx + 1, by + 1, bx + c[1] - 1, by + CTRL_H - 1, SiGuiTheme.SCREEN_BG);
            }
            Component label = switch (c[2]) {
                case 0 -> Component.translatable(K + (dis ? "mode.discharge" : "mode.pump"));
                case 1 -> Component.translatable(K + (v(9) != 0 ? "waterlogged.on" : "waterlogged.off"));
                case -2, 8 -> Component.literal("−");
                case -3, 9 -> Component.literal("+");
                case -1 -> Component.translatable(K + "keep.value", grouped(v(8)));
                default -> Component.translatable(K + "target.value", (v(10) >= 0 ? "+" : "") + v(10));
            };
            int col = active ? 0x2A2E33 : 0x9AA0A6;
            PumpCrossSection.text(g, label, bx + c[1] / 2F - AsciiHud.width(label, u) / 2, by + CTRL_H / 2F - 4.5F * u, col, u);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean dis = discharge();
            for (int[] c : CONTROLS) {
                int bx = leftPos + c[0], by = topPos + CTRL_Y;
                if (mouseX < bx || mouseX >= bx + c[1] || mouseY < by || mouseY >= by + CTRL_H) continue;
                int id = c[2];
                if (id == -2 || id == -3) {
                    if (dis) return true;
                    int step = Screen.hasControlDown() ? 6 : Screen.hasShiftDown() ? 4 : 2;
                    id = step + (id == -3 ? 1 : 0);
                } else if ((id == 8 || id == 9) && !dis) return true;
                if (id < 0) return true;
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> tip = new ArrayList<>();
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= TANK_X && rx < TANK_X + TANK_W && ry >= TANK_Y && ry < TANK_Y + TANK_H) {
            FluidStack f = menu.getFluidStack();
            tip.add(f.isEmpty() ? Component.translatable(K + "tip.tank_empty")
                : Component.translatable(K + "tip.tank", f.getHoverName(), grouped(menu.getFluidAmount()), grouped(menu.getFluidCapacity())));
        } else if (rx >= ENERGY_X && rx < ENERGY_X + ENERGY_W && ry >= TANK_Y && ry < TANK_Y + TANK_H) {
            tip.add(Component.translatable(K + "tip.energy", Integer.toUnsignedLong(menu.getEnergy()), Integer.toUnsignedLong(menu.getMaxEnergy())));
        } else if (ry >= CTRL_Y && ry < CTRL_Y + CTRL_H) {
            for (int[] c : CONTROLS) {
                if (rx < c[0] || rx >= c[0] + c[1]) continue;
                String key = switch (c[2]) {
                    case 0 -> "tip.mode";
                    case 1 -> "tip.waterlogged";
                    case -2, -3, -1 -> "tip.keep";
                    default -> "tip.target";
                };
                tip.add(Component.translatable(K + key));
                if (key.equals("tip.keep")) tip.add(Component.translatable(K + "tip.keep_step"));
            }
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
    }
}
