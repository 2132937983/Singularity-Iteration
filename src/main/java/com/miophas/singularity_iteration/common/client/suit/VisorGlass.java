// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 0.1.10.31 (S-9, optional): the visor glass of a worn nano or quantum helmet, in first person, under the whole HUD
 * (the HUD is projected on the glass). Client option {@code visor_glass}, off by default; also on the Look page of the
 * equipment console. Rules: {@link VisorGlassRules}; textures made by {@code tools/thrust_textures_1131.py}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class VisorGlass {
    private VisorGlass() {}

    private static final ResourceLocation RIM = tex("rim"), FRINGE = tex("fringe"), STREAK = tex("streak"), DROP = tex("drop"), CRACK = tex("crack");
    private static final ResourceLocation NANO = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_nano_helmet");
    private static final ResourceLocation QUANTUM = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_helmet");
    private static final VisorGlassRules.Drops DROPS = new VisorGlassRules.Drops(1131);
    private static final float STREAK_WIDTH = 0.18F;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/gui/visor/" + name + ".png");
    }

    /** The worn nano or quantum helmet, or empty. */
    static ItemStack helmet(Minecraft mc) {
        if (mc.player == null) return ItemStack.EMPTY;
        ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(head.getItem());
        return NANO.equals(id) || QUANTUM.equals(id) ? head : ItemStack.EMPTY;
    }

    private static boolean active(Minecraft mc) {
        return FcsClientConfig.visorGlass() && mc.player != null && mc.options.getCameraType().isFirstPerson() && !helmet(mc).isEmpty();
    }

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "visor_glass"), VisorGlass::render);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (!active(mc) || mc.level == null) {
            DROPS.clear();
            return;
        }
        BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
        DROPS.tick(mc.level.isRainingAt(eye) || mc.level.isRainingAt(eye.above()));
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !active(mc)) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // the dark rim of the glass and the colour fringes in the corners
        g.setColor(1F, 1F, 1F, VisorGlassRules.RIM_ALPHA);
        g.blit(RIM, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
        g.setColor(1F, 1F, 1F, VisorGlassRules.FRINGE_ALPHA);
        g.blit(FRINGE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
        // the reflection streak that slides as the head turns
        float yaw = mc.player.getViewYRot(partial);
        int sx = Math.round(VisorGlassRules.streakX(yaw, STREAK_WIDTH) * w), sw = Math.round(STREAK_WIDTH * w);
        g.setColor(1F, 1F, 1F, 0.08F);
        g.blit(STREAK, sx, 0, sw, h, 0, 0, 32, 256, 32, 256);
        // raindrops
        for (VisorGlassRules.Drops.Drop d : DROPS.drops()) {
            float a = VisorGlassRules.Drops.alpha(d);
            if (a <= 0F) continue;
            int r = Math.max(2, Math.round(d.size * h));
            g.setColor(1F, 1F, 1F, 0.75F * a);
            g.blit(DROP, Math.round(d.x * w) - r, Math.round(d.y * h) - r, 2 * r, 2 * r, 0, 0, 32, 32, 32, 32);
        }
        // cracks when the helmet is almost empty
        float crack = VisorGlassRules.crack(SuitGlowLayer.charge(helmet(mc)));
        if (crack > 0F) {
            int s = Math.round(h * 0.7F);
            g.setColor(1F, 1F, 1F, 0.7F * crack);
            g.blit(CRACK, 0, h - s, s, s, 0, 0, 256, 256, 256, 256);
        }
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
