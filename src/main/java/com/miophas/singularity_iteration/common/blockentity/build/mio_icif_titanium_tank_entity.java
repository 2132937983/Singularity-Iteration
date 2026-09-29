package com.miophas.singularity_iteration.common.blockentity.build;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 钛储罐：64 桶。通用连通/平衡/持久化逻辑在 {@link AbstractTankBlockEntity}。 */
public class mio_icif_titanium_tank_entity extends AbstractTankBlockEntity<mio_icif_titanium_tank_entity> {

    public static final int TANK_CAPACITY = 64000;

    public mio_icif_titanium_tank_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.TITANIUM_TANK.get(), pos, state, TANK_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_titanium_tank_entity entity) {
    }
}
