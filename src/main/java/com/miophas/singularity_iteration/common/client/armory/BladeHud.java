// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.blade.BladeCore;
import com.miophas.singularity_iteration.common.armory.blade.BladeResources;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import com.miophas.singularity_iteration.common.client.suit.VisorHudPrimitives;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumCore;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 0.1.11.233 (SlashBlade collab B1): a small card right of the hotbar while a collab blade is in the main hand: its EU,
 * and the blade's own resource (nano: edge heat; quantum: the suit's coherence, or EU only without a quantum core;
 * singularity: mass and the containment). Reads the synced {@link BladeCore} off the held stack; draws nothing for any
 * other item. No SlashBlade class is named here.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class BladeHud {
    private BladeHud() {}

    static final int W = 132, H = 34;
    private static final int ORANGE = 0xFF7A1F, CYAN = 0x4DE6FF, MAGENTA = 0xFF2BD6, RED = 0xFF4D45, GREY = 0x8A96A3;

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blade_hud"), BladeHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        var stack = mc.player.getMainHandItem();
        CollabBlade kind = CollabBlade.of(stack);
        if (kind == null) return;
        BladeCore c = CollabBlade.core(stack);
        if (c == null) return;
        int x = g.guiWidth() / 2 + 98, y = g.guiHeight() - H - 2;
        draw(g, mc.font, x, y, kind, c, mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false),
            kind == CollabBlade.QUANTUM && QuantumCore.corePiece(mc.player) != null,
            SuitQuantumCore.coherence(mc.player), SuitQuantumCore.coherenceMax(mc.player), SuitQuantumCore.decoherent(mc.player));
    }

    /** The card; {@code core}: the wearer has a quantum core (the quantum blade's slash art is paid with coherence). */
    static void draw(GuiGraphics g, Font font, int x, int y, CollabBlade kind, BladeCore c, float time,
                     boolean core, float coherence, float coherenceMax, boolean decoherent) {
        int accent = switch (kind) {
            case NANO -> ORANGE;
            case QUANTUM -> CYAN;
            case SINGULARITY -> MAGENTA;
        };
        VisorHudPrimitives.glassCard(g, x, y, W, H, accent, 0.55F, 0.62F);
        // EU (right of the name, which gives way to it)
        float eu = c.eu() / (float) kind.euMax;
        String euText = c.eu() >= 1_000_000 ? String.format(java.util.Locale.ROOT, "%.1fM EU", c.eu() / 1_000_000.0) : c.eu() >= 1000 ? (c.eu() / 1000) + "k EU" : c.eu() + " EU";
        int euWidth = Math.round(font.width(euText) * 0.75F);
        String name = font.plainSubstrByWidth(Component.translatable("item.mio_icif." + kind.id).getString(), W - 12 - euWidth);
        g.drawString(font, name, x + 4, y + 3, 0xFF000000 | accent, false);
        textRight(g, font, euText, x + W - 4, y + 4, GREY);
        bar(g, x + 4, y + 13, W - 8, eu, eu < saShare(kind) ? RED : 0xEAF6FF);
        // the blade's own resource
        int ry = y + 22;
        switch (kind) {
            case NANO -> {
                float heat = c.heat() / CollabBlade.NANO_HEAT_CAP;
                boolean blink = c.overheated() && ((int) (time / 4)) % 2 == 0;
                bar(g, x + 4, ry + 6, W - 8, heat, c.overheated() ? (blink ? RED : ORANGE) : ORANGE);
                label(g, font, c.overheated() ? "hud.mio_icif.blade.overheated" : "hud.mio_icif.blade.heat", x + 4, ry - 2,
                    c.overheated() ? RED : GREY, Math.round(c.heat()));
            }
            case QUANTUM -> {
                if (core) {
                    float share = coherenceMax <= 0 ? 0 : coherence / coherenceMax;
                    bar(g, x + 4, ry + 6, W - 8, share, decoherent || coherence < CollabBlade.QUANTUM_SA_COHERENCE ? RED : CYAN);
                    label(g, font, decoherent ? "hud.mio_icif.blade.decoherent" : "hud.mio_icif.blade.coherence", x + 4, ry - 2,
                        decoherent ? RED : GREY, Math.round(coherence));
                } else {
                    label(g, font, "hud.mio_icif.blade.no_core", x + 4, ry, GREY);
                }
            }
            case SINGULARITY -> {
                float mass = c.mass() / CollabBlade.MASS_MAX;
                bar(g, x + 4, ry + 6, W - 8, mass, MAGENTA);
                long ticks = BladeResources.containmentTicks(c);
                if (c.unstable()) {
                    boolean blink = ((int) (time / 3)) % 2 == 0;
                    label(g, font, "hud.mio_icif.blade.unstable", x + 4, ry - 2, blink ? RED : MAGENTA, Math.round(c.mass()));
                } else if (ticks == Long.MAX_VALUE) {
                    label(g, font, "hud.mio_icif.blade.mass", x + 4, ry - 2, GREY, Math.round(c.mass()));
                } else {
                    label(g, font, "hud.mio_icif.blade.contained", x + 4, ry - 2, GREY, Math.round(c.mass()), String.format(java.util.Locale.ROOT, "%d:%02d", ticks / 20 / 60, ticks / 20 % 60));
                }
            }
        }
    }

    /** The EU share one slash art needs (the bar turns red below it). */
    private static float saShare(CollabBlade kind) {
        return kind.saEu / (float) kind.euMax;
    }

    private static void bar(GuiGraphics g, int x, int y, int w, float share, int rgb) {
        share = Mth.clamp(share, 0, 1);
        g.fill(x, y, x + w, y + 3, 0xA0101820);
        int filled = Math.round(w * share);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + 3, 0xFF000000 | rgb);
            g.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        }
    }

    private static void label(GuiGraphics g, Font font, String key, int x, int y, int rgb, Object... args) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(key, args).getString(), Math.round((W - 8) / 0.75F)), 0, 0, 0xFF000000 | rgb, false);
        g.pose().popPose();
    }

    private static void textRight(GuiGraphics g, Font font, String s, int right, int y, int rgb) {
        g.pose().pushPose();
        g.pose().translate(right - font.width(s) * 0.75F, y + 1, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, s, 0, 0, 0xFF000000 | rgb, false);
        g.pose().popPose();
    }
}
