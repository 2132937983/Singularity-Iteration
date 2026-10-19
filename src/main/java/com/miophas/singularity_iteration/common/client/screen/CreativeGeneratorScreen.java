// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.menu.generator.CreativeGeneratorMenu;
import com.miophas.singularity_iteration.common.network.CreativeGeneratorPacket;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.10.11: Creative Generator screen. A read-out (output, EU sent in the last tick, state), a text
 * field for any output up to one trillion EU/t (k/M/G/T suffixes), preset keys, x10 / /10 and the
 * on/off switch.
 */
@OnlyIn(Dist.CLIENT)
public class CreativeGeneratorScreen extends AbstractContainerScreen<CreativeGeneratorMenu> {
    private static final int W = 196, H = 132;
    private EditBox rateBox;
    private SiButton switchButton;
    private boolean invalid;

    public CreativeGeneratorScreen(CreativeGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        rateBox = new EditBox(font, x + 9, y + 55, 104, 12, Component.translatable("gui.mio_icif.creative_generator.field"));
        rateBox.setMaxLength(24);
        rateBox.setValue(Long.toString(menu.rate()));
        rateBox.setResponder(text -> invalid = false);
        addRenderableWidget(rateBox);
        addRenderableWidget(new SiButton(x + 118, y + 54, 34, 14, Component.translatable("gui.mio_icif.creative_generator.set"), b -> submit()));
        switchButton = addRenderableWidget(new SiButton(x + 155, y + 54, 33, 14, Component.empty(),
            b -> click(CreativeGeneratorMenu.BUTTON_SWITCH)));
        for (int i = 0; i < CreativeGeneratorMenu.PRESETS.length; i++) {
            int id = i;
            long preset = CreativeGeneratorMenu.PRESETS[i];
            addRenderableWidget(new SiButton(x + 8 + (i % 4) * 45, y + 74 + (i / 4) * 16, 43, 14,
                Component.literal(preset >= 1_000_000 ? preset / 1_000_000 + "M" : format(preset)), b -> click(id)));
        }
        addRenderableWidget(new SiButton(x + 8, y + 108, 43, 14, Component.literal("x10"), b -> click(CreativeGeneratorMenu.BUTTON_TIMES_TEN)));
        addRenderableWidget(new SiButton(x + 53, y + 108, 43, 14, Component.literal("/10"), b -> click(CreativeGeneratorMenu.BUTTON_DIVIDE_TEN)));
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        // the field shows the new output once the server has applied it
        pendingRefresh = 6;
    }

    private int pendingRefresh;

    private void submit() {
        long rate = CreativeGeneratorMenu.parseRate(rateBox.getValue());
        if (rate < 0) { invalid = true; return; }
        PacketDistributor.sendToServer(new CreativeGeneratorPacket(menu.containerId, rate));
        pendingRefresh = 6;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (pendingRefresh > 0 && --pendingRefresh == 0 && !rateBox.isFocused()) rateBox.setValue(Long.toString(menu.rate()));
        switchButton.setMessage(Component.translatable(menu.enabled() ? "gui.mio_icif.creative_generator.on" : "gui.mio_icif.creative_generator.off"));
        switchButton.selected(false);
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
        SiGuiTheme.panel(g, x, y, W, H);
        SiGuiTheme.well(g, x + 8, y + 18, W - 16, 32);
        int lamp = !menu.enabled() || menu.rate() == 0 ? SiGuiTheme.LED_OFF : menu.lastSent() > 0 ? SiGuiTheme.GOOD : SiGuiTheme.WARN;
        SiGuiTheme.led(g, x + W - 16, y + 22, lamp);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        mio_icif_screen.drawFitted(g, font, title, 8, 6, W - 16, SiGuiTheme.TEXT, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.creative_generator.output", format(menu.rate())),
            12, 22, W - 34, SiGuiTheme.TEXT_SCREEN, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.creative_generator.sent", format(menu.lastSent())),
            12, 32, W - 24, SiGuiTheme.TEXT_SCREEN, false);
        Component state = Component.translatable(invalid ? "gui.mio_icif.creative_generator.invalid" : "gui.mio_icif.creative_generator.hint");
        mio_icif_screen.drawFitted(g, font, state, 12, 41, W - 24, invalid ? SiGuiTheme.BAD : SiGuiTheme.TEXT_SCREEN_DIM, false);
        mio_icif_screen.drawFitted(g, font, Component.translatable("gui.mio_icif.creative_generator.no_limits"),
            100, 111, W - 108, SiGuiTheme.TEXT, false);
    }
}
