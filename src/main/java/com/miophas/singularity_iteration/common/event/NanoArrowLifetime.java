// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_nano_bow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 0.1.11.193 (T0, W-05): a nano bow arrow flies without gravity, and a vanilla arrow only ages once it is
 * stuck. One shot upwards stayed in the air for ever, ticking in its chunk. A nano arrow is removed
 * {@link mio_icif_nano_bow#ARROW_FLIGHT_TICKS} ticks after it was shot (it cannot be picked up anyway).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class NanoArrowLifetime {
    private NanoArrowLifetime() {}

    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractArrow arrow && !arrow.level().isClientSide
                && arrow.tickCount > mio_icif_nano_bow.ARROW_FLIGHT_TICKS && arrow.isNoGravity()
                // 0.1.11.195: only an arrow still in flight; one stuck in a block stands still and can be picked up
                && arrow.getDeltaMovement().lengthSqr() > 1.0E-4
                && arrow.getPersistentData().getBoolean(mio_icif_nano_bow.ARROW_TAG)) {
            arrow.discard();
        }
    }
}
