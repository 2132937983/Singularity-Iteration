// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCallouts;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;

/**
 * 0.1.11.58 (gaze and Iron Man phase P6): the Wingman callouts on screen. Up to three subtitle lines above the squad HUD,
 * newest at the bottom: each types itself out (about 50 characters a second), stays {@link #HOLD} ticks and fades;
 * urgent lines are amber. A chirp with each line (client options {@code wingman_callouts}, {@code wingman_callout_sound};
 * with the subtitles off a line goes to the action bar).
 */
public final class CalloutHud {
    private CalloutHud() {}

    static final int MAX_LINES = 3, HOLD = 70, FADE = 15;
    private static final float CHARS_PER_TICK = 2.5F;

    private record Shown(String text, boolean urgent, int start) { }

    private static final Deque<Shown> LINES = new ArrayDeque<>();

    /** A callout from the server. */
    public static void accept(WingmanCallouts.Line line) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (FcsClientConfig.wingmanCalloutSound())
            mc.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.ORDER_ACK.get(), line.urgent() ? 1.0F : 1.6F, 0.35F));
        if (!FcsClientConfig.wingmanCallouts()) {
            mc.player.displayClientMessage(line.text(), true);
            return;
        }
        LINES.addLast(new Shown(line.text().getString(), line.urgent(), SquadClient.ticks));
        while (LINES.size() > MAX_LINES) LINES.removeFirst();
    }

    public static void clear() { LINES.clear(); }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || LINES.isEmpty()) return;
        float now = SquadClient.ticks + delta.getGameTimeDeltaPartialTick(false);
        LINES.removeIf(l -> now - l.start() > HOLD + FADE);
        int x = 10, y = (int) (g.guiHeight() * 0.38F) - 6 - LINES.size() * 10;
        for (Shown l : LINES) {
            float age = now - l.start();
            float alpha = age < HOLD ? 1F : Mth.clamp(1 - (age - HOLD) / FADE, 0, 1);
            int chars = Math.min(l.text().length(), (int) (age * CHARS_PER_TICK) + 1);
            String typed = l.text().substring(0, chars);
            float w = FcsDraw.width(l.text(), 0.6F);
            g.fill(x - 2, y - 1, (int) (x + w + 6), y + 8, FcsDraw.argb(0x01060C, 0.55F * alpha));
            g.fill(x - 2, y - 1, x - 1, y + 8, FcsDraw.argb(l.urgent() ? 0xFFB347 : 0x5CEBFF, alpha));
            FcsDraw.text(g, "▸ " + typed + (chars < l.text().length() && ((int) age / 3) % 2 == 0 ? "_" : ""), x + 1, y + 1,
                FcsDraw.argb(l.urgent() ? 0xFFC66B : 0x8FE2FF, alpha), 0.6F);
            y += 10;
        }
    }
}
