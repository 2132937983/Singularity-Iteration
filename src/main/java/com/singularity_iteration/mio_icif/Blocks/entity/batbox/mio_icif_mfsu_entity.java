package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MFSU 方块实体
 * 继承mio_icif_Energy_Container，拥有巨大的能量容量
 * 容量0,000,000 EU，输输出速率048 EU/tick
 * 使用 EV 电缆等级
 * 对齐原版 IC2：tier=4, output=2048, maxStorage=40000000
 */
@SuppressWarnings("null")
public class mio_icif_mfsu_entity extends mio_icif_Energy_Container {

    private static final long EV_IO_RATE = CableTier.EV.powerRating;

    /**
     * 构造函数（用于游戏中创建方块实体）
     * 使用 MFSU 方块实体类型
     */
    public mio_icif_mfsu_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.MFSU.get(), 40000000L, EV_IO_RATE, EV_IO_RATE, CableTier.EV);
    }

    /**
     * 构造函数（用于方块实体类型注册     * @param type 方块实体类型
     */
    public mio_icif_mfsu_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 40000000L, EV_IO_RATE, EV_IO_RATE, CableTier.EV);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.mfsu");
    }
}

