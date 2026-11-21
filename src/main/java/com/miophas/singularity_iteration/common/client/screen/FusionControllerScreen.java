package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.fusion.FusionBlanket;
import com.miophas.singularity_iteration.common.fusion.FusionPlasma;
import com.miophas.singularity_iteration.common.menu.fusion.FusionControllerMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.92: the fusion reactor controller's screen, drawn in code with {@link SiGuiTheme}: the blanket grid (column 1 faces
 * the plasma), the fuel and ice slots, and on the right the plasma (state, temperature, Q, fusion power, output), the feed rate
 * buttons, the stores, the ports and the first problem to fix.
 */
@OnlyIn(Dist.CLIENT)
public class FusionControllerScreen extends AbstractContainerScreen<FusionControllerMenu> {
    private static final int W = 300, H = 256, INFO_X = 176, INFO_Y = 16, INFO_W = 116, INFO_H = 112;
    private static final String[] PARTS = {"central_solenoid", "toroidal_coil", "poloidal_coil", "vacuum_vessel", "blanket", "divertor", "port_slot",
        "controller", "plasma"};
    private static final String[] STATES = {"off", "cooling", "charging", "heating", "burning", "disrupted", "quenched"};
    private static final String[] CAUSES = {"none", "quench", "structure", "ash", "blanket", "density_limit", "beta_limit"};
    private static final String[] STATUS = {"ok", "unformed", "no_signal", "no_cryo", "no_heater", "no_exhaust", "no_power_port", "no_fuel",
        "blanket_invalid"};

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
        int[] ids = {2, 0, 1, 3};
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            addRenderableWidget(new SiButton(leftPos + INFO_X + i * 29, topPos + 132, 27, 14, Component.literal(labels[i]), b -> click(id)));
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        // 0.1.11.133: the plasma's power balance over the readouts
        int rx = mouseX - leftPos - INFO_X, ry = mouseY - topPos - INFO_Y;
        if (rx >= 0 && ry >= 0 && rx < INFO_W && ry < INFO_H - 16 && v(0) >= 3 && v(0) <= 4) g.renderComponentTooltip(font, balance(), mouseX, mouseY);
    }

    /** 0.1.11.133: density, confinement time, triple product against the Lawson criterion, alpha heating against the losses. */
    private java.util.List<Component> balance() {
        String K = "gui.mio_icif.fusion.balance.";
        double n = v(35) / 1000.0, tau = v(36) / 1000.0, triple = v(37) / 1000.0;
        int lawson = (int) Math.round(triple * 1.0e21 / FusionPlasma.LAWSON * 100);
        return java.util.List.of(
            Component.translatable(K + "title"),
            Component.translatable(K + "density", String.format(Locale.ROOT, "%.2f", n)).withStyle(net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "tau", String.format(Locale.ROOT, "%.2f", tau)).withStyle(net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "triple", String.format(Locale.ROOT, "%.2f", triple), lawson)
                .withStyle(lawson >= 100 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "alpha", compact(v(38)), compact(v(39))).withStyle(net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "heating", compact(v(33))).withStyle(net.minecraft.ChatFormatting.GRAY),
            // 0.1.11.134: the confinement mode and the operating limits
            Component.translatable(K + (v(42) == 1 ? "h_mode" : "l_mode"), v(44)).withStyle(v(42) == 1 ? net.minecraft.ChatFormatting.AQUA : net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "limits", v(40) / 10, v(41) / 10).withStyle(Math.max(v(40), v(41)) >= 1000 ? net.minecraft.ChatFormatting.RED
                : Math.max(v(40), v(41)) >= 900 ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.GRAY),
            // 0.1.11.135: the plant: the blanket's heat, the steam cycle, multiplication, breeding, engineering Q
            Component.translatable(K + "plant"),
            Component.translatable(K + "thermal", compact(v(45)), FusionBlanket.EFFICIENCY_PERCENT, compact(v(4))).withStyle(net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "multiplication", String.format(Locale.ROOT, "%.1f", v(46) / 100.0), String.format(Locale.ROOT, "%.2f", v(47) / 100.0))
                .withStyle(net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "tbr", String.format(Locale.ROOT, "%.2f", v(48) / 100.0))
                .withStyle(v(48) >= 100 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY),
            Component.translatable(K + "q_eng", String.format(Locale.ROOT, "%.1f", v(49) / 100.0)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, W, H);
        for (int row = 0; row < FusionBlanket.ROWS; row++)
            for (int col = 0; col < FusionBlanket.COLUMNS; col++)
                SiGuiTheme.slot(g, x + FusionControllerMenu.GRID_X + col * 18 - 1, y + FusionControllerMenu.GRID_Y + row * 18 - 1);
        // the first column faces the plasma
        g.fill(x + FusionControllerMenu.GRID_X - 4, y + FusionControllerMenu.GRID_Y - 1, x + FusionControllerMenu.GRID_X - 2,
            y + FusionControllerMenu.GRID_Y + FusionBlanket.ROWS * 18 - 1, 0xFFC43CFF);
        for (int fx : FusionControllerMenu.FUEL_X) SiGuiTheme.slot(g, x + fx - 1, y + FusionControllerMenu.FUEL_Y - 1);
        SiGuiTheme.well(g, x + INFO_X, y + INFO_Y, INFO_W, INFO_H);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                SiGuiTheme.slot(g, x + FusionControllerMenu.INV_X + col * 18 - 1, y + FusionControllerMenu.INV_Y + row * 18 - 1);
        for (int col = 0; col < 9; col++) SiGuiTheme.slot(g, x + FusionControllerMenu.INV_X + col * 18 - 1, y + FusionControllerMenu.HOTBAR_Y - 1);
        // the input store and the blanket heat
        long stored = v(5), capacity = Math.max(1, v(6)), hull = v(10), maxHull = Math.max(1, v(11));
        SiGuiTheme.bar(g, x + INFO_X + 4, y + INFO_Y + INFO_H - 14, INFO_W - 8, 4, (double) stored / capacity, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        SiGuiTheme.bar(g, x + INFO_X + 4, y + INFO_Y + INFO_H - 7, INFO_W - 8, 4, (double) hull / maxHull, 0xFFFFC23D, 0xFFC8423C);
        int state = (int) v(0);
        SiGuiTheme.led(g, x + W - 14, y + 6, state == 4 ? SiGuiTheme.GOOD : state == 3 || state == 1 || state == 2 ? SiGuiTheme.WARN
            : state >= 5 ? SiGuiTheme.BAD : SiGuiTheme.LED_OFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, SiGuiTheme.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SiGuiTheme.TEXT, false);
        int line = 0, tx = INFO_X + 4, ty = INFO_Y + 4, tw = INFO_W - 8;
        int state = (int) Math.max(0, Math.min(STATES.length - 1, v(0)));
        // 0.1.11.133: a plasma that fuses without being ignited burns driven by the heating ports
        Component stateText = Component.translatable("gui.mio_icif.fusion.state." + (state == 3 && v(3) > 0 ? "driven" : STATES[state]));
        Component progress = switch (state) {
            case 1 -> Component.translatable("gui.mio_icif.fusion.progress.cooling", v(12) * 100 / FusionPlasma.COOLDOWN_TICKS);
            case 2 -> Component.translatable("gui.mio_icif.fusion.progress.charging", v(13) * 100 / FusionPlasma.STARTUP_ENERGY);
            // two electron cyclotron heaters drive the current: a steady burn, its pulse clock never starts
            case 4 -> v(14) == 0 && v(20) >= 2 ? Component.translatable("gui.mio_icif.fusion.progress.steady")
                : Component.translatable("gui.mio_icif.fusion.progress.burn", v(14) / 20);
            // 0.1.11.134: a disruption says why it came
            case 5, 6 -> Component.translatable("gui.mio_icif.fusion.progress.timer", (v(30) + 19) / 20).append(v(43) > 0 && v(43) < CAUSES.length
                ? Component.literal(" ").append(Component.translatable("gui.mio_icif.fusion.cause." + CAUSES[(int) v(43)])) : Component.empty());
            default -> Component.empty();
        };
        mio_icif_screen.drawFitted(g, font, stateText.copy().append(" ").append(progress), tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        double kev = v(1) / 100.0;
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.temperature", String.format(Locale.ROOT, "%.1f", kev),
            String.format(Locale.ROOT, "%.2f", kev * 0.116)), tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        long q = v(16);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.q", q < 0 ? "∞" : String.format(Locale.ROOT, "%.1f", q / 100.0)),
            tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.fusion", compact(v(3))), tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.output", compact(v(4))), tx, ty + 10 * line++, tw,
            v(4) >= FusionPlasma.FULL_POWER ? SiGuiTheme.GOOD : SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.feed", v(2), FusionPlasma.MAX_FEED), tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.fuel", compact(v(8)), compact(v(9))), tx, ty + 10 * line++, tw, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.ports", v(18), v(19), v(23), v(22), v(21), v(24)), tx, ty + 10 * line++, tw,
            SiGuiTheme.TEXT_SCREEN_DIM, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.stores", compact(v(5)), compact(v(10)), compact(v(11))), tx, ty + 10 * line++, tw,
            SiGuiTheme.TEXT_SCREEN_DIM, false);
        // the fuel slot labels
        String[] fuel = {"deuterium", "tritium", "empty", "empty", "coolant"};   // 0.1.11.94: coolant cells (was ice)
        for (int i = 0; i < fuel.length; i++)
            mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.fusion.slot." + fuel[i]), FusionControllerMenu.FUEL_X[i] - 1,
                FusionControllerMenu.FUEL_Y + 19, 18, SiGuiTheme.TEXT, false);
        // the first problem
        int status = (int) Math.max(0, Math.min(STATUS.length - 1, v(25)));
        Component problem;
        if (status == 1 && v(29) >= 0) {
            int part = (int) Math.min(PARTS.length - 1, v(29));
            problem = Component.translatable("gui.mio_icif.fusion.unformed_at", v(26), v(27), v(28),
                Component.translatable("gui.mio_icif.fusion.part." + PARTS[part]));
        } else {
            problem = Component.translatable("gui.mio_icif.fusion.status." + STATUS[status]);
        }
        // its own row under the feed buttons (0.1.11.93: it ran into them)
        mio_icif_screen.drawFitted(g, font, problem, 140, FusionControllerMenu.FUEL_Y + 17, W - 148, status == 0 ? SiGuiTheme.GOOD : SiGuiTheme.BAD, false);
    }
}
