package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MFE 方块实体
 * 继承mio_icif_Energy_Container
 * 容量,000,000 EU，输输出速率12 EU/tick
 * 使用 HV 电缆等级
 */
@SuppressWarnings("null")
public class mio_icif_mfe_entity extends mio_icif_Energy_Container {

    private static final long HV_IO_RATE = CableTier.HV.powerRating;

    /**
     * 构造函数（用于游戏中创建方块实体）
     * 使用 MFE 方块实体类型
     */
    public mio_icif_mfe_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.MFE.get(), 4000000L, HV_IO_RATE, HV_IO_RATE, CableTier.HV);
    }

    /**
     * 构造函数（用于方块实体类型注册
     * @param type 方块实体类型
     */
    public mio_icif_mfe_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 4000000L, HV_IO_RATE, HV_IO_RATE, CableTier.HV);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.mfe");
    }
}

