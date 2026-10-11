// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsProjection;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only cleanup of static caches (0.1.7.36).
 *
 * <p>Why: some client caches are keyed by entity id or hold the camera of the last frame. Entity
 * ids are reused in a new world, so old entries can attach to a different entity. These caches
 * must be cleared at logout and pruned while the world runs.
 *
 * <p>Contract: at logout, {@link FcsProjection#reset} and {@link ManeuverModeClient#pruneVisuals}
 * (all entries). Every {@link #PRUNE_INTERVAL} client ticks, the maneuver visuals of players
 * that left the level are removed.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ClientSessionCleanup {
    private ClientSessionCleanup() {}

    /** Client ticks between two prunes (5 s). */
    static final int PRUNE_INTERVAL = 100;
    private static int ticks;

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FcsProjection.reset();
        ManeuverModeClient.pruneVisuals(null);
        ticks = 0;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (++ticks < PRUNE_INTERVAL) return;
        ticks = 0;
        ManeuverModeClient.pruneVisuals(Minecraft.getInstance().level);
    }
}
