package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryToastPacket;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bottom-right pop-up: "Suit maintenance complete" when an Armory finishes repairing and
 * recharging everything it stores. Slides in, holds for five seconds, slides out; several
 * notices queue up. Drawn in the Modern-IC panel style of the machine GUIs.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ArmoryToastOverlay {
    private static final int W = 176, H = 38, SLIDE = 250, HOLD = 5000;
    private static final Deque<Entry> QUEUE = new ArrayDeque<>();

    private record Entry(ArmoryToastPacket packet, long[] shownAt) { }

    private ArmoryToastOverlay() {}

    /** True while a notice is on screen (or queued). */
    public static boolean showing() {
        return !QUEUE.isEmpty();
    }

    public static void push(ArmoryToastPacket packet) {
        QUEUE.addLast(new Entry(packet, new long[]{-1}));
        if (QUEUE.size() > 4) QUEUE.removeFirst();
    }

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_toast"),
            ArmoryToastOverlay::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Entry entry = QUEUE.peekFirst();
        if (entry == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        long now = net.minecraft.Util.getMillis();
        if (entry.shownAt[0] < 0) {
            entry.shownAt[0] = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.ARMORY_COMPLETE.get(), 1.0F, 0.9F));
        }
        long age = now - entry.shownAt[0];
        if (age > HOLD + 2L * SLIDE) {
            QUEUE.removeFirst();
            return;
        }
        float in = Mth.clamp(age / (float) SLIDE, 0, 1);
        float out = Mth.clamp((age - SLIDE - HOLD) / (float) SLIDE, 0, 1);
        float slide = easeOut(in) * (1 - easeIn(out));
        int sw = g.guiWidth(), sh = g.guiHeight();
        int x = sw - Math.round(slide * (W + 6));
        int y = sh - H - 6;
        draw(g, mc.font, x, y, entry.packet, age);
    }

    private static void draw(GuiGraphics g, Font font, int x, int y, ArmoryToastPacket p, long age) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        SiGuiTheme.panel(g, x, y, W, H);
        g.fill(x + 1, y + 1, x + 4, y + H - 1, SiGuiTheme.GOOD);            // status stripe
        SiGuiTheme.well(g, x + 7, y + 7, 24, 24);
        g.renderItem(new ItemStack(ArmoryRegistry.ARMORY_ITEM.get()), x + 11, y + 11);
        // check mark badge, pulsing once
        float pulse = 1 + 0.35F * (float) Math.exp(-age / 180.0) * (float) Math.sin(age / 40.0);
        g.pose().pushPose();
        g.pose().translate(x + 27, y + 27, 10);
        g.pose().scale(pulse, pulse, 1);
        g.fill(-4, -4, 5, 5, SiGuiTheme.GOOD);
        g.drawString(font, "✔", -3, -4, 0xFFFFFFFF, false);
        g.pose().popPose();
        g.drawString(font, Component.translatable("toast.mio_icif.armory.done.title"), x + 36, y + 7, SiGuiTheme.TEXT, false);
        String detail = Component.translatable("toast.mio_icif.armory.done.detail", p.repairedPoints(),
            String.format("%,d", p.chargedEu())).getString();
        if (p.manaUsed() > 0) detail += Component.translatable("toast.mio_icif.armory.done.mana", String.format("%,d", p.manaUsed())).getString();
        small(g, font, font.plainSubstrByWidth(detail, (int) ((W - 42) / 0.75F)), x + 36, y + 19, SiGuiTheme.TEXT_SCREEN_DIM);
        String where = p.armory().pos().toShortString();
        small(g, font, where, x + 36, y + 27, SiGuiTheme.TEXT_SCREEN_DIM);
        g.pose().popPose();
    }

    private static void small(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static float easeOut(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); }
    private static float easeIn(float t) { return t * t * t; }

}
