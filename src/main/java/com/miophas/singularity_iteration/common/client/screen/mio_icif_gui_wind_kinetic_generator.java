package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WindPhysics;
import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.WindKineticGeneratorMenu;
import com.miophas.singularity_iteration.core.api.item.IKineticRotor;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Wind kinetic generator screen. 0.1.11.9: 248 x 204 with the two charts a wind engineer reads: the wind rose (where
 * the wind came from lately and how strong, the rotor's facing against the wind's bearing now) and this rotor's power
 * curve (cut-in, cut-out, the current operating point; above cut-out the curve drops to 0 with storm protection, or
 * keeps rising in red while the rotor wears). Under them the wind and its factors (facing, wake, obstruction), the
 * output and the state; under the window the storm protection switch and the rotor's health.
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_wind_kinetic_generator extends mio_icif_screen<WindKineticGeneratorMenu> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 62, STRIP_Y = 145, STORM_W = 88;   // 0.1.11.121: wider for the vanilla font
    private static final String K = "gui.mio_icif.wind.";

    public mio_icif_gui_wind_kinetic_generator(WindKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WindKineticGeneratorMenu.GUI_WIDTH;
        this.imageHeight = WindKineticGeneratorMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        VanillaFrame.slot(g, x + WindKineticGeneratorMenu.SLOT_X - 1, y + WindKineticGeneratorMenu.SLOT_Y - 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            VanillaFrame.slot(g, x + WindKineticGeneratorMenu.INVENTORY_X - 1 + c * 18, y + WindKineticGeneratorMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) VanillaFrame.slot(g, x + WindKineticGeneratorMenu.INVENTORY_X - 1 + c * 18, y + WindKineticGeneratorMenu.HOTBAR_Y - 1);

        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, WH + 2, WindRoseView.PAPER);   // 0.1.11.119: the vanilla look
        float u = WindRoseView.u();
        WindRoseView.rose(g, x + WX + 40, y + WY + 31, 22, WindRoseView.bytes(this::v, 11), WindRoseView.bytes(this::v, 15), v(6), v(5));
        ItemStack rotorStack = menu.getSlot(0).getItem();
        curve(g, x + WX + 92, y + WY + 6, 108, 46, rotorStack);
        // readings
        ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the readout window, vanilla font
        float tx = x + WX + 4, ty = y + WY + 66, lh = 9 * u + 1;
        WindRoseView.line(g, Component.translatable(K + "wind_line", v(3), v(4)).append(" · ").append(WindRoseView.bearing(v(5))), tx, ty, WindRoseView.INK, WW - 8);
        WindRoseView.line(g, Component.translatable(K + "factors", WindRoseView.factor(v(7)), WindRoseView.factor(v(8)), WindRoseView.factor(v(9))),
            tx, ty + lh, WindRoseView.DIM, WW - 8);
        int status = v(0);
        String key = switch (status) {
            case mio_icif_Wind_Kinetic_Generator.STATUS_MISSING_ROTOR -> "state.no_rotor";
            case mio_icif_Wind_Kinetic_Generator.STATUS_BLOCKED_ROTOR -> "state.blocked";
            case mio_icif_Wind_Kinetic_Generator.STATUS_WEAK_WIND -> "state.weak";
            case mio_icif_Wind_Kinetic_Generator.STATUS_FEATHERED -> "state.feathered";
            case mio_icif_Wind_Kinetic_Generator.STATUS_OVERLOAD -> "state.overload";
            default -> "state.run";
        };
        int col = status == mio_icif_Wind_Kinetic_Generator.STATUS_OVERLOAD || status == mio_icif_Wind_Kinetic_Generator.STATUS_BLOCKED_ROTOR
            ? WindRoseView.RED : status == mio_icif_Wind_Kinetic_Generator.STATUS_GENERATING ? 0x2F6E4F : WindRoseView.DIM;
        WindRoseView.line(g, Component.translatable(K + "output_ku", v(1)).append(" · ").append(Component.translatable(K + key)), tx, ty + 2 * lh, col, WW - 8);

        ReadoutPanel.end(g, x + WX - 1, y + WY + WH + 3, WW + 2, 62);

        // strip: storm protection, rotor health (0.1.11.121: a vanilla button, vanilla font, furnace-style bar)
        int sy = y + STRIP_Y, bx = x + WX;
        boolean hover = mouseX >= bx && mouseX < bx + STORM_W && mouseY >= sy && mouseY < sy + 12;
        boolean storm = v(10) != 0;
        if (storm) {
            VanillaFrame.inset(g, bx, sy, STORM_W, 12, hover ? 0xFF7A7A7A : 0xFF6B6B6B);
        } else {
            g.fill(bx, sy, bx + STORM_W, sy + 12, VanillaFrame.SHADOW);
            g.fill(bx, sy, bx + STORM_W - 1, sy + 11, VanillaFrame.LIGHT);
            g.fill(bx + 1, sy + 1, bx + STORM_W - 1, sy + 11, hover ? 0xFFD8D8E8 : 0xFFB8B8B8);
        }
        Component sc = Component.translatable(K + (storm ? "storm.on" : "storm.off"));
        g.drawString(font, sc, bx + (STORM_W - font.width(sc)) / 2, sy + 2, storm ? 0xFFFFFF : VanillaFrame.TITLE, false);
        int health = v(2);
        VanillaFrame.labelledBar(g, bx + STORM_W + 6, sy, 241 - WX - STORM_W - 6, Component.translatable(K + "rotor_health"), health / 100.0,
            Component.literal(health + "%"));
    }

    /** The rotor's power curve in (px, py, pw, ph) with the operating point. */
    private void curve(GuiGraphics g, float px, float py, float pw, float ph, ItemStack rotorStack) {
        float u = WindRoseView.u();
        FcsDraw.line(g, px, py + ph, px + pw, py + ph, 0.5F, 0xFF5B6867);
        FcsDraw.line(g, px, py, px, py + ph, 0.5F, 0xFF5B6867);
        if (!(rotorStack.getItem() instanceof IKineticRotor rotor)) {
            Component c = Component.translatable(K + "insert_rotor");
            PumpCrossSection.text(g, c, px + pw / 2 - AsciiHud.width(c, u) / 2, py + ph / 2 - 4 * u, WindRoseView.DIM, u);
            return;
        }
        int min = rotor.getMinWindStrength(rotorStack), max = rotor.getMaxWindStrength(rotorStack);
        float eff = rotor.getEfficiency(rotorStack);
        boolean storm = v(10) != 0;
        float vmax = max * 1.4F, pmax = vmax * eff * WindPhysics.OUTPUT_MODIFIER * 1.05F;
        java.util.function.DoubleUnaryOperator X = w -> px + w / vmax * pw;
        java.util.function.DoubleUnaryOperator Y = p -> py + ph - p / pmax * ph;
        for (int[] mark : new int[][]{{min, 0}, {max, 1}}) {
            float mx = (float) X.applyAsDouble(mark[0]);
            FcsDraw.line(g, mx, py, mx, py + ph, 0.35F, 0xFFB6BFBE);
            Component c = Component.translatable(K + (mark[1] == 0 ? "cut_in" : "cut_out"), mark[0]);
            PumpCrossSection.text(g, c, mx - AsciiHud.width(c, u) / 2, py + ph + 1, WindRoseView.DIM, u);
        }
        int ink = 0xFF23454A;
        float pMin = min * eff * WindPhysics.OUTPUT_MODIFIER, pMax = max * eff * WindPhysics.OUTPUT_MODIFIER;
        FcsDraw.line(g, px, (float) Y.applyAsDouble(0), (float) X.applyAsDouble(min), (float) Y.applyAsDouble(0), 0.9F, ink);
        FcsDraw.line(g, (float) X.applyAsDouble(min), (float) Y.applyAsDouble(0), (float) X.applyAsDouble(min), (float) Y.applyAsDouble(pMin), 0.9F, ink);
        FcsDraw.line(g, (float) X.applyAsDouble(min), (float) Y.applyAsDouble(pMin), (float) X.applyAsDouble(max), (float) Y.applyAsDouble(pMax), 0.9F, ink);
        if (storm) {
            FcsDraw.line(g, (float) X.applyAsDouble(max), (float) Y.applyAsDouble(pMax), (float) X.applyAsDouble(max), (float) Y.applyAsDouble(0), 0.9F, ink);
            FcsDraw.line(g, (float) X.applyAsDouble(max), (float) Y.applyAsDouble(0), px + pw, (float) Y.applyAsDouble(0), 0.9F, ink);
        } else {
            WindRoseView.dashed(g, (float) X.applyAsDouble(max), (float) Y.applyAsDouble(pMax), px + pw,
                (float) Y.applyAsDouble(vmax * eff * WindPhysics.OUTPUT_MODIFIER), 0xFFC0392B);
        }
        float wind = Math.min(vmax, v(3));
        FcsDraw.disc(g, (float) X.applyAsDouble(wind), (float) Y.applyAsDouble(v(1)), 1.8F, 0xFFC0392B);
        PumpCrossSection.text(g, Component.translatable(K + "axis_ku"), px + 2, py - 1, WindRoseView.DIM, u);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bx = leftPos + WX, by = topPos + STRIP_Y;
        if (button == 0 && mouseX >= bx && mouseX < bx + STORM_W && mouseY >= by && mouseY < by + 12) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
            playClick();
            return true;
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
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= WX && rx < WX + STORM_W && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.storm"), Component.translatable(K + "tip.storm2")), mouseX, mouseY);
        } else if (rx >= WX && rx < WX + 85 && ry >= WY && ry < WY + 62) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.rose"), Component.translatable(K + "tip.facing")), mouseX, mouseY);
        } else if (rx >= WX + 88 && rx < WX + WW && ry >= WY && ry < WY + 62) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.curve")), mouseX, mouseY);
        } else if (rx >= WX && rx < WX + WW && ry >= WY + 62 && ry < WY + WH) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.factors"), Component.translatable(K + "tip.wake")), mouseX, mouseY);
        }
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
