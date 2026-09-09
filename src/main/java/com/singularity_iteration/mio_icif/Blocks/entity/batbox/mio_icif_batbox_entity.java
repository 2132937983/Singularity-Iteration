package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BatBox 方块实体
 * 继承mio_icif_Energy_Container，基础储能设备
 * 容量0,000 EU，输输出速率2 EU/tick
 * 使用 LV 电缆等级
 */
@SuppressWarnings("null")
public class mio_icif_batbox_entity extends mio_icif_Energy_Container {

    private static final long LV_IO_RATE = CableTier.LV.powerRating;

    /**
     * 构造函数（用于游戏中创建方块实体）
     * 使用 BatBox 方块实体类型
     */
    public mio_icif_batbox_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.BATBOX.get(), 40000L, LV_IO_RATE, LV_IO_RATE, CableTier.LV);
    }

    /**
     * 构造函数（用于方块实体类型注册
     * @param type 方块实体类型
     */
    public mio_icif_batbox_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 40000L, LV_IO_RATE, LV_IO_RATE, CableTier.LV);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.batbox");
    }
}

