package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CESU 方块实体
 * 继承mio_icif_Energy_Container
 * 容量00,000 EU，输输出速率28 EU/tick
 * 使用 MV 电缆等级
 */
@SuppressWarnings("null")
public class mio_icif_cesu_entity extends mio_icif_Energy_Container {

    private static final long MV_IO_RATE = CableTier.MV.powerRating;

    /**
     * 构造函数（用于游戏中创建方块实体）
     * 使用 CESU 方块实体类型
     */
    public mio_icif_cesu_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.CESU.get(), 300000L, MV_IO_RATE, MV_IO_RATE, CableTier.MV);
    }

    /**
     * 构造函数（用于方块实体类型注册
     * @param type 方块实体类型
     */
    public mio_icif_cesu_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 300000L, MV_IO_RATE, MV_IO_RATE, CableTier.MV);
    }
}


