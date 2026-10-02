package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registrations for the laser defence towers (matrix core renderer). */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LaserTowerClient {
    private LaserTowerClient() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(mio_icif_block_entities.LASER_DEFENSE_TOWER_ENTITY_TYPE.get(), MatrixTowerRenderer::new);
        event.registerBlockEntityRenderer(mio_icif_block_entities.SKY_PATROL_LASER_TOWER_ENTITY_TYPE.get(), MatrixTowerRenderer::new);
    }
}
