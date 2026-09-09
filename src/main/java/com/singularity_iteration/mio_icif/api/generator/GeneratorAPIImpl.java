package com.singularity_iteration.mio_icif.api.generator;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IEnergyBlock;
import com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock;
import com.singularity_iteration.mio_icif.api.machine.MachineAPIImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.*;

/**
 * 发电机 API 实现
 *
 * @deprecated 发电机已统一为机器的一种类型，请使用 {@link MachineAPIImpl}。
 *             此类别保留以向后兼容。
 */
@SuppressWarnings("null")
@Deprecated
public class GeneratorAPIImpl implements IGeneratorAPI {

    /**
     * 注册发电机位置到全局注册表
     * 应由主模组在发电机方块放置时调用
     * @deprecated 请使用 {@link MachineAPIImpl#registerMachine(Level, BlockPos)}
     */
    @Deprecated
    public static void registerGenerator(Level world, BlockPos pos) {
        MachineAPIImpl.registerMachine(world, pos);
    }

    /**
     * 从全局注册表移除发电机位置
     * 应由主模组在发电机方块破坏时调用
     * @deprecated 请使用 {@link MachineAPIImpl#unregisterMachine(Level, BlockPos)}
     */
    @Deprecated
    public static void unregisterGenerator(Level world, BlockPos pos) {
        MachineAPIImpl.unregisterMachine(world, pos);
    }

    /**
     * 清理已卸载维度的注册表条目
     * @deprecated 请使用 {@link MachineAPIImpl#onDimensionUnload(Level)}
     */
    @Deprecated
    public static void onDimensionUnload(Level world) {
        MachineAPIImpl.onDimensionUnload(world);
    }

    @Override
    @Deprecated
    public <T extends BlockEntity> void registerGeneratorType(String id, BlockEntityType<T> type) {
        // 发电机类型注册已废弃，发电机统一为机器类型
        // 请使用 MachineBuilderAPI 创建自定义发电机
    }

    @Override
    @Deprecated
    public boolean isGenerator(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof IGeneratorBlock;
    }

    @Override
    @Deprecated
    public IGeneratorInfo getGeneratorInfo(Level world, BlockPos pos) {
        if (world.isClientSide) return null;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IGeneratorBlock generator)) {
            return null;
        }

        return new GeneratorInfoImpl(world, pos, generator);
    }

    @Override
    @Deprecated
    public Collection<BlockPos> getAllGenerators(Level world) {
        if (world.isClientSide) return Collections.emptyList();
        Set<BlockPos> allMachines = MachineAPIImpl.getAllMachinePositions(world);
        if (allMachines == null) return Collections.emptyList();

        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : allMachines) {
            if (world.getBlockEntity(pos) instanceof IGeneratorBlock) {
                result.add(pos);
            }
        }
        return result;
    }

    @Override
    @Deprecated
    public Collection<BlockPos> getGeneratorsByType(Level world, String typeId) {
        if (world.isClientSide) return Collections.emptyList();
        return getAllGenerators(world);
    }

    @Override
    @Deprecated
    public boolean forceStart(Level world, BlockPos pos) {
        return com.singularity_iteration.mio_icif.api.internal.GeneratorHelper.forceStartGenerator(world, pos);
    }

    @Override
    @Deprecated
    public boolean forceStop(Level world, BlockPos pos) {
        return com.singularity_iteration.mio_icif.api.internal.GeneratorHelper.forceStopGenerator(world, pos);
    }

    @Override
    @Deprecated
    public ICableTier getCableTier(Level world, BlockPos pos) {
        if (world.isClientSide) return null;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IGeneratorBlock generator)) {
            return null;
        }

        return generator.getCableTier();
    }

    /**
     * 发电机信息实现
     */
    private static class GeneratorInfoImpl implements IGeneratorInfo {
        private final Level world;
        private final BlockPos pos;
        private final IGeneratorBlock generator;

        public GeneratorInfoImpl(Level world, BlockPos pos, IGeneratorBlock generator) {
            this.world = world;
            this.pos = pos;
            this.generator = generator;
        }

        @Override
        public Level getWorld() {
            return world;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public String getTypeId() {
            return generator.getGeneratorTypeId();
        }

        @Override
        public long getGenerationRate() {
            return generator.getGenerationRate();
        }

        @Override
        public long getStoredEnergy() {
            if (generator instanceof IEnergyBlock energyBlock) {
                return energyBlock.getEnergyStorage().getAmount();
            }
            return 0;
        }

        @Override
        public long getMaxEnergy() {
            if (generator instanceof IEnergyBlock energyBlock) {
                return energyBlock.getEnergyStorage().getCapacity();
            }
            return 0;
        }

        @Override
        public ICableTier getCableTier() {
            return generator.getCableTier();
        }

        @Override
        public boolean isActive() {
            return generator.isGenerating();
        }

        @Override
        public long getOutputRate() {
            return generator.getGenerationRate();
        }

        @Override
        public double getEfficiency() {
            int maxBurn = generator.getMaxBurnTime();
            if (maxBurn <= 0) return 100.0;
            if (generator.getBurnTime() <= 0) return 0.0;
            return Math.min(100.0, (double) generator.getBurnTime() / maxBurn * 100.0);
        }

        @Override
        public long getRuntimeTicks() {
            return generator.getBurnTime();
        }

        @Override
        public long getTotalGenerated() {
            return generator.getTotalGenerated();
        }
    }
}
