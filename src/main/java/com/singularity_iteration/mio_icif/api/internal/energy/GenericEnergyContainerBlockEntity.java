package com.singularity_iteration.mio_icif.api.internal.energy;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 通用储能方块实体（内部实现）- 供 API 构建器和内部储电箱使用
 *
 * <p>这是 {@link mio_icif_Energy_Container} 的 API 友好包装，
 * 提供 {@link ICableTier} 接口参数的构造函数，避免外部直接引用内部枚举 {@link CableTier}。
 *
 * <p><strong>这是内部实现类，不应被 Addon 直接扩展。</strong>
 * Addon 开发者应该使用 {@link com.singularity_iteration.mio_icif.api.machine.builder.IEnergyContainerBuilder} 构建器接口。
 *
 * <p>使用示例（内部储电箱）：
 * <pre>{@code
 * public class MyContainerEntity extends GenericEnergyContainerBlockEntity {
 *     private static final ICableTier TIER =
 *         MioIcifAPI.instance().getEnergyNetAPI().getCableTier("hv");
 *
 *     public MyContainerEntity(BlockPos pos, BlockState state) {
 *         super(pos, state, MY_TYPE.get(), 1000000, 512, 512, TIER);
 *     }
 * }
 * }</pre>
 */
public class GenericEnergyContainerBlockEntity extends mio_icif_Energy_Container {

    public GenericEnergyContainerBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                             long capacity, long maxReceive, long maxExtract,
                                             ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, CableTier.fromICableTier(cableTier));
    }

    public GenericEnergyContainerBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                             long capacity, long maxReceive, long maxExtract,
                                             CableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    public GenericEnergyContainerBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public static void containerTick(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                     net.minecraft.world.level.block.state.BlockState state,
                                     GenericEnergyContainerBlockEntity blockEntity) {
        mio_icif_Energy_Container.tick(level, pos, state, blockEntity);
    }
}