// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

/**
 * 0.1.10.38: where a tool HUD panel (Crop Analyzer, EU meter) goes. Both used to sit below and right of the crosshair, which on
 * common screen sizes is where the chat is, and they were drawn under the chat. They are now GUI layers above the chat, and with
 * the anchor AUTO (client config {@code tool_hud.anchor}) the panel keeps clear of the chat area: below and right of the crosshair
 * when that is free, else above the crosshair, else right of the chat, else the top-right corner.
 */
public final class ToolHudPlacement {
    private ToolHudPlacement() {}

    private static final int MARGIN = 4;

    /** The top-left corner {x, y} of a panel of the outer size {@code w} x {@code h} (GUI-scaled pixels). */
    public static int[] place(Minecraft mc, int w, int h) {
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        int[] below = clamp(screenW / 2 + 11, screenH / 2 + 5, w, h, screenW, screenH);
        int[] above = clamp(screenW / 2 + 11, screenH / 2 - 8 - h, w, h, screenW, screenH);
        int[] topRight = clamp(screenW - w - MARGIN, MARGIN, w, h, screenW, screenH);
        return switch (FcsClientConfig.toolHudAnchor()) {
            case BELOW_CROSSHAIR -> below;
            case ABOVE_CROSSHAIR -> above;
            case TOP_LEFT -> clamp(MARGIN, MARGIN, w, h, screenW, screenH);
            case TOP_RIGHT -> topRight;
            case AUTO -> {
                int[] chat = chatArea(mc, screenH);
                if (!overlaps(below, w, h, chat)) yield below;
                if (!overlaps(above, w, h, chat)) yield above;
                int[] beside = clamp(chat[2] + 6, below[1], w, h, screenW, screenH);
                yield overlaps(beside, w, h, chat) ? topRight : beside;
            }
        };
    }

    /** The chat area {left, top, right, bottom}: the unfocused chat lines above the input line, as vanilla draws them. */
    static int[] chatArea(Minecraft mc, int screenH) {
        ChatComponent chat = mc.gui.getChat();
        double scale = chat.getScale();
        int right = (int) Math.ceil(chat.getWidth() * scale) + 12;
        int top = screenH - 40 - (int) Math.ceil(chat.getHeight() * scale) - 2;
        return new int[]{0, top, right, screenH - 14};
    }

    static boolean overlaps(int[] at, int w, int h, int[] area) {
        return at[0] < area[2] && at[0] + w > area[0] && at[1] < area[3] && at[1] + h > area[1];
    }

    private static int[] clamp(int x, int y, int w, int h, int screenW, int screenH) {
        return new int[]{Math.max(MARGIN, Math.min(x, screenW - w - MARGIN)), Math.max(MARGIN, Math.min(y, screenH - h - MARGIN))};
    }
}
