// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.client.multiblock.StructureView;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.11.209: the 3D view of a multiblock structure in EMI (the JEI page's view, same renderer). EMI widgets get
 * clicks and keys but no drags or wheel, so: the view spins on its own; a click turns it by 45° (left / right),
 * with Shift it zooms; the arrow keys turn and tilt it, + and - zoom; a click on the layer label shows one layer
 * (left: up, right: down), as in JEI.
 */
final class StructurePreviewWidget extends Widget {
    private static final float TURN = 45F / 1.6F, NUDGE = 15F / 1.6F;   // StructureView.drag turns 1.6° per pixel

    private final StructureBlueprint blueprint;
    private final Bounds bounds;
    private StructureView view;

    StructurePreviewWidget(StructureBlueprint blueprint, int x, int y, int width, int height) {
        this.blueprint = blueprint;
        this.bounds = new Bounds(x, y, width, height);
    }

    /** Built on first use, so a page never opened builds no view. */
    private StructureView view() {
        if (view == null) view = new StructureView(blueprint);
        return view;
    }

    private Component layerLabel() {
        StructureView v = view();
        return v.layer() < 0 ? Component.translatable("jei.mio_icif.structure.layer_all", v.sizeY())
            : Component.translatable("jei.mio_icif.structure.layer", v.layer() + 1, v.sizeY());
    }

    private int labelWidth() {
        return Minecraft.getInstance().font.width(layerLabel()) + 6;
    }

    /** Over the layer label (coordinates of the recipe). */
    private boolean onLabel(int x, int y) {
        return x >= bounds.right() - labelWidth() && x < bounds.right() && y >= bounds.y() && y < bounds.y() + 11;
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = bounds.x(), y = bounds.y(), w = bounds.width(), h = bounds.height();
        graphics.fill(x, y, x + w, y + h, 0x20000000);
        view().render(graphics, x, y, w, h);
        int lw = labelWidth();
        graphics.fill(x + w - lw, y, x + w, y + 11, onLabel(mouseX, mouseY) ? 0xC0405060 : 0x80303840);
        graphics.drawString(Minecraft.getInstance().font, layerLabel(), x + w - lw + 3, y + 2, 0xFFFFFFFF, false);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        Component text = onLabel(mouseX, mouseY) ? Component.translatable("jei.mio_icif.structure.layer_help")
            : Component.translatable("emi.mio_icif.structure.view_help").withStyle(ChatFormatting.GRAY);
        return List.of(ClientTooltipComponent.create(text.getVisualOrderText()));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 && button != 1) return false;
        int sign = button == 0 ? 1 : -1;
        if (onLabel(mouseX, mouseY)) view().stepLayer(sign);
        else if (Screen.hasShiftDown()) view().zoom(sign);
        else view().drag(sign * TURN, 0);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> view().drag(-NUDGE, 0);
            case GLFW.GLFW_KEY_RIGHT -> view().drag(NUDGE, 0);
            case GLFW.GLFW_KEY_UP -> view().drag(0, -NUDGE);
            case GLFW.GLFW_KEY_DOWN -> view().drag(0, NUDGE);
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> view().zoom(1);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> view().zoom(-1);
            default -> { return false; }
        }
        return true;
    }
}
