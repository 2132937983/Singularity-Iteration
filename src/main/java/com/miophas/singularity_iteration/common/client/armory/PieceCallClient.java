// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.11.57 (gaze and Iron Man phase P5, decision A): the piece key (default J; V, the planned key, is the nano suit's
 * primary burst) opens the piece wheel ({@link PieceCallScreen}): hold it, point at a position, let go.
 */
public final class PieceCallClient {
    private PieceCallClient() {}

    public static KeyMapping KEY;

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            KEY = new KeyMapping("key.mio_icif.piece_call", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
                "key.category.mio_icif");
            event.register(KEY);
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (KEY != null && KEY.consumeClick()) {
                if (mc.player == null || mc.screen != null) continue;
                mc.setScreen(new PieceCallScreen());
            }
        }
    }
}
