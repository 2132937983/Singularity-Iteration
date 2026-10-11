package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generic energy storage block entity (internal implementation). The API builder and the internal energy storage boxes use it.
 *
 * <p>This is an API-friendly wrapper of {@link AbstractEnergyStorageBlockEntity}.
 * Its constructor takes the {@link ICableTier} interface. Thus outside code does not refer directly to the internal class {@link CableTier}.
 *
 * <p><strong>This is an internal implementation class. Addons must not extend it directly.</strong>
 * Addon developers should use the builder interface {@link com.miophas.singularity_iteration.core.api.machine.builder.IEnergyContainerBuilder}.
 *
 * <p>Example (internal energy storage box):
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
public class GenericEnergyContainerBlockEntity extends AbstractEnergyStorageBlockEntity {

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
        AbstractEnergyStorageBlockEntity.tick(level, pos, state, blockEntity);
    }
}