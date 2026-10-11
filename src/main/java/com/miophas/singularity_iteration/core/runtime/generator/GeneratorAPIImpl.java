package com.miophas.singularity_iteration.core.runtime.generator;

import com.miophas.singularity_iteration.core.api.generator.IGeneratorAPI;
import com.miophas.singularity_iteration.core.api.generator.IGeneratorInfo;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IEnergyBlock;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import com.miophas.singularity_iteration.core.runtime.machine.MachineAPIImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.*;

/**
 * Implementation of the generator API.
 *
 * @deprecated A generator is now one type of machine. Use {@link MachineAPIImpl}.
 *             This class stays for backward compatibility.
 */
@SuppressWarnings("null")
@Deprecated
public class GeneratorAPIImpl implements IGeneratorAPI {

    /**
     * Registers a generator position in the global registry.
     * The main mod must call this when a generator block is placed.
     * @deprecated Use {@link MachineAPIImpl#registerMachine(Level, BlockPos)}
     */
    @Deprecated
    public static void registerGenerator(Level world, BlockPos pos) {
        MachineAPIImpl.registerMachine(world, pos);
    }

    /**
     * Removes a generator position from the global registry.
     * The main mod must call this when a generator block is broken.
     * @deprecated Use {@link MachineAPIImpl#unregisterMachine(Level, BlockPos)}
     */
    @Deprecated
    public static void unregisterGenerator(Level world, BlockPos pos) {
        MachineAPIImpl.unregisterMachine(world, pos);
    }

    /**
     * Removes registry entries of unloaded dimensions.
     * @deprecated Use {@link MachineAPIImpl#onDimensionUnload(Level)}
     */
    @Deprecated
    public static void onDimensionUnload(Level world) {
        MachineAPIImpl.onDimensionUnload(world);
    }

    @Override
    @Deprecated
    public <T extends BlockEntity> void registerGeneratorType(String id, BlockEntityType<T> type) {
        // Generator type registration is deprecated. A generator is now a machine type.
        // Use MachineBuilderAPI to create custom generators.
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
        return com.miophas.singularity_iteration.core.runtime.machine.GeneratorHelper.forceStartGenerator(world, pos);
    }

    @Override
    @Deprecated
    public boolean forceStop(Level world, BlockPos pos) {
        return com.miophas.singularity_iteration.core.runtime.machine.GeneratorHelper.forceStopGenerator(world, pos);
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
     * Implementation of generator information.
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
