// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuOutputPortBlockEntity;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.menu.wiring.TesuOutputMenu;
import com.miophas.singularity_iteration.common.network.TesuOutputPacket;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.11.191: the TESU output port's screen, after the Creative Generator's: a read-out (output, voltage, EU sent in the last
 * tick, the TESU's energy), a field for any output up to one MAX packet (k/M/G suffixes), a key for each voltage's full packet,
 * x10 / /10 and the on/off switch. The voltage follows the output (the lowest from IV that holds it).
 * <pre>
 *   TESU Output Port
 *   +----------------------------------+
 *   | Output / Voltage / Sent / TESU (o)|
 *   +----------------------------------+
 *   [ field          ] [Set] [On]
 *   hint
 *   [IV ][LuV][ZPM][UV ][UHV]
 *   [UEV][UIV][UXV][OPV][MAX]
 *   [x10][/10]   voltage follows
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class TesuOutputScreen extends AbstractContainerScreen<TesuOutputMenu> {
    private static final int W = 196, H = 150, WELL_Y = 18, WELL_H = 46;
    private EditBox rateBox;
    private SiButton switchButton;
    private final SiButton[] voltages = new SiButton[TesuOutputMenu.VOLTAGES];
    private boolean invalid;
    private int pendingRefresh;

    public TesuOutputScreen(TesuOutputMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    static String format(long value) { return String.format(Locale.ROOT, "%,d", value); }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        rateBox = new EditBox(font, x + 9, y + 69, 104, 12, Component.translatable("gui.mio_icif.creative_generator.field"));
        rateBox.setMaxLength(24);
        rateBox.setValue(Long.toString(menu.rate()));
        rateBox.setResponder(text -> invalid = false);
        addRenderableWidget(rateBox);
        addRenderableWidget(new SiButton(x + 118, y + 68, 34, 14, Component.translatable("gui.mio_icif.creative_generator.set"), b -> submit()));
        switchButton = addRenderableWidget(new SiButton(x + 155, y + 68, 33, 14, Component.empty(), b -> click(TesuOutputMenu.BUTTON_SWITCH)));
        for (int i = 0; i < TesuOutputMenu.VOLTAGES; i++) {
            int id = i;
            var tier = TesuOutputPortBlockEntity.tierOf(i);
            voltages[i] = addRenderableWidget(new SiButton(x + 8 + (i % 5) * 36, y + 96 + (i / 5) * 16, 35, 14, Component.literal(tier.getDisplayName()), b -> click(id)));
            voltages[i].setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.tesu_output.preset", tier.getDisplayName(), format(tier.getPowerRating()))));
        }
        addRenderableWidget(new SiButton(x + 8, y + 130, 35, 14, Component.literal("x10"), b -> click(TesuOutputMenu.BUTTON_TIMES_TEN)));
        addRenderableWidget(new SiButton(x + 44, y + 130, 35, 14, Component.literal("/10"), b -> click(TesuOutputMenu.BUTTON_DIVIDE_TEN)));
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        pendingRefresh = 6;   // the field shows the new output once the server has applied it
    }

    private void submit() {
        long rate = TesuOutputMenu.parseRate(rateBox.getValue());
        if (rate < 0) { invalid = true; return; }
        PacketDistributor.sendToServer(new TesuOutputPacket(menu.containerId, rate));
        pendingRefresh = 6;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (pendingRefresh > 0 && --pendingRefresh == 0 && !rateBox.isFocused()) rateBox.setValue(Long.toString(menu.rate()));
        switchButton.setMessage(Component.translatable(menu.enabled() ? "gui.mio_icif.creative_generator.on" : "gui.mio_icif.creative_generator.off"));
        for (int i = 0; i < voltages.length; i++)   // the key of a voltage's full packet stays pressed while it is the output
            voltages[i].selected(menu.rate() == TesuOutputPortBlockEntity.tierOf(i).getPowerRating());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rateBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { submit(); return true; }
            // typing must not close the screen through the inventory key
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) return rateBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, W, H);
        SiGuiTheme.well(g, x + 8, y + WELL_Y, W - 16, WELL_H);
        int lamp = !menu.formed() ? SiGuiTheme.BAD : !menu.enabled() || menu.rate() == 0 ? SiGuiTheme.LED_OFF
            : menu.lastSent() > 0 ? SiGuiTheme.GOOD : SiGuiTheme.WARN;
        SiGuiTheme.led(g, x + W - 16, y + WELL_Y + 4, lamp);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        mio_icif_screen.drawFitted(g, font, title, 8, 6, W - 16, SiGuiTheme.TEXT, false);
        var tier = TesuOutputPortBlockEntity.tierOf(menu.tierIndex());
        int ty = WELL_Y + 4;
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.tesu_output.output", format(menu.rate())), 12, ty, W - 34, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.tesu_output.voltage", tier.getDisplayName(), format(tier.getPowerRating())),
            12, ty + 10, W - 24, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.tesu_output.sent", format(menu.lastSent())), 12, ty + 20, W - 24, SiGuiTheme.TEXT_SCREEN, false);
        Component tesu = menu.formed() ? Component.translatable("gui.mio_icif.tesu_output.stored", format(menu.stored()), format(menu.capacity()))
            : Component.translatable("gui.mio_icif.tesu_output.unformed");
        mio_icif_screen.drawFitted(g, font, tesu, 12, ty + 30, W - 24, menu.formed() ? SiGuiTheme.TEXT_SCREEN_DIM : SiGuiTheme.BAD, false);
        Component state = Component.translatable(invalid ? "gui.mio_icif.tesu_output.invalid" : "gui.mio_icif.tesu_output.hint");
        mio_icif_screen.drawFitted(g, font, state, 9, 85, W - 18, invalid ? SiGuiTheme.BAD : SiGuiTheme.TEXT, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.tesu_output.auto"), 84, 133, W - 92, SiGuiTheme.TEXT, false);
    }
}
