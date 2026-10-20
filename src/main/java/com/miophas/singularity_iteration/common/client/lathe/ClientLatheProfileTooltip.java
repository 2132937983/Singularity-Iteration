package com.miophas.singularity_iteration.common.client.lathe;

import com.miophas.singularity_iteration.common.item.lathe.LatheProfileTooltip;
import com.miophas.singularity_iteration.common.item.lathe.LatheShapes;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The profile of a lathe workpiece at half the lathe GUI's size (60 x 16 px) over a faint band of
 * the uncut blank, with the thickness of every position under it: grey uncut, yellow cut, red at
 * the minimum of 1.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public final class ClientLatheProfileTooltip implements ClientTooltipComponent {

    private static final int SEGMENT = LatheProfileRenderer.SEGMENT_LENGTH / 2;
    private static final int LENGTH = SEGMENT * LatheProfileRenderer.POSITIONS;
    private static final int BAND = LatheProfileRenderer.RENDER_WIDTH / 2;

    private final ItemStack stack;

    public ClientLatheProfileTooltip(LatheProfileTooltip tooltip) {
        this.stack = tooltip.stack();
    }

    @Override
    public int getHeight() {
        return BAND + 2 + 9 + 2;
    }

    @Override
    public int getWidth(Font font) {
        return LENGTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (!(stack.getItem() instanceof ILatheItem lathe)) return;
        int[] state = lathe.getCurrentState(stack);
        int width = lathe.getWidth(stack);
        if (state.length < LatheProfileRenderer.POSITIONS) return;

        // the uncut blank as a faint band, teal for a finished part
        guiGraphics.fill(x, y, x + LENGTH, y + BAND, LatheShapes.of(stack).isPresent() ? 0x4055FFFF : 0x30FFFFFF);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.5F, 0.5F, 1F);
        LatheProfileRenderer.renderGui(guiGraphics, stack, 0, 0);
        guiGraphics.pose().popPose();

        for (int j = 0; j < LatheProfileRenderer.POSITIONS; j++) {
            String text = Integer.toString(state[j]);
            int color = state[j] >= width ? 0xAAAAAA : state[j] <= 1 ? 0xFF5555 : 0xFFFF55;
            guiGraphics.drawString(font, text, x + SEGMENT * j + (SEGMENT - font.width(text)) / 2, y + BAND + 2, color, true);
        }
    }
}
