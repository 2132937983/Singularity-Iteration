package com.singularity_iteration.mio_icif.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 双流体槽显示提供�? * 支持显示两个流体槽的流体信息
 */
@SuppressWarnings({"null", "removal"})
public enum DualFluidStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "dual_fluid_storage");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        
        // 显示输入流体�?- 总是显示
        if (data.contains("InputFluid") && data.contains("InputFluidAmount") && data.contains("InputFluidCapacity")) {
            String fluidName = data.getString("InputFluid");
            int amount = data.getInt("InputFluidAmount");
            int capacity = data.getInt("InputFluidCapacity");
            
            if (capacity > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.input_fluid", 
                    fluidName, amount, capacity));
            }
        }
        
        // 显示输出流体�?- 总是显示
        if (data.contains("OutputFluid") && data.contains("OutputFluidAmount") && data.contains("OutputFluidCapacity")) {
            String fluidName = data.getString("OutputFluid");
            int amount = data.getInt("OutputFluidAmount");
            int capacity = data.getInt("OutputFluidCapacity");
            
            if (capacity > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.output_fluid", 
                    fluidName, amount, capacity));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        // 尝试获取双流体槽能力
        // 这里使用一个自定义的接口来获取两个流体�
    DualFluidHandler dualHandler = getDualFluidHandler(blockEntity);
        
        if (dualHandler != null) {
            // 输入�?- 总是显示，即使为�
        FluidStack inputFluid = dualHandler.getInputFluid();
            if (!inputFluid.isEmpty()) {
                data.putString("InputFluid", inputFluid.getDisplayName().getString());
                data.putInt("InputFluidAmount", inputFluid.getAmount());
            } else {
                data.putString("InputFluid", "jade.mio_icif.fluid.empty");
                data.putInt("InputFluidAmount", 0);
            }
            data.putInt("InputFluidCapacity", dualHandler.getInputCapacity());
            
            // 输出�?- 总是显示，即使为�
        FluidStack outputFluid = dualHandler.getOutputFluid();
            if (!outputFluid.isEmpty()) {
                data.putString("OutputFluid", outputFluid.getDisplayName().getString());
                data.putInt("OutputFluidAmount", outputFluid.getAmount());
            } else {
                data.putString("OutputFluid", "jade.mio_icif.fluid.empty");
                data.putInt("OutputFluidAmount", 0);
            }
            data.putInt("OutputFluidCapacity", dualHandler.getOutputCapacity());
        }
    }

    /**
     * 获取双流体处理器
     * 尝试从方块实体获取双流体槽信息
 */
    @Nullable
    private DualFluidHandler getDualFluidHandler(BlockEntity blockEntity) {
        // 检查是否是热交换机
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid heatSource) {
            return new HeatSourceFluidHandler(heatSource);
        }
        
        // 检查是否是斯特林动能机
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator stirling) {
            return new StirlingFluidHandler(stirling);
        }

        // 检查是否是核反应堆（流体模式）
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor) {
            if (reactor.getReactorMode() == com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_reactor_mode.FLUID) {
                return new NuclearReactorFluidHandler(reactor);
            }
        }

        return null;
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    /**
     * 双流体处理器接口
     */
    public interface DualFluidHandler {
        FluidStack getInputFluid();
        int getInputCapacity();
        FluidStack getOutputFluid();
        int getOutputCapacity();
    }

    /**
     * 热交换机的流体处理器实现
     */
    private static class HeatSourceFluidHandler implements DualFluidHandler {
        private final com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid heatSource;

        public HeatSourceFluidHandler(com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid heatSource) {
            this.heatSource = heatSource;
        }

        @Override
        public FluidStack getInputFluid() {
            return heatSource.getInputTank().getFluid();
        }

        @Override
        public int getInputCapacity() {
            return heatSource.getInputTank().getCapacity();
        }

        @Override
        public FluidStack getOutputFluid() {
            return heatSource.getOutputTank().getFluid();
        }

        @Override
        public int getOutputCapacity() {
            return heatSource.getOutputTank().getCapacity();
        }
    }

    /**
     * 斯特林动能机的流体处理器实现
     */
    private static class StirlingFluidHandler implements DualFluidHandler {
        private final com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator stirling;

        public StirlingFluidHandler(com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator stirling) {
            this.stirling = stirling;
        }

        @Override
        public FluidStack getInputFluid() {
            return stirling.getWaterTank().getFluid();
        }

        @Override
        public int getInputCapacity() {
            return stirling.getWaterTank().getCapacity();
        }

        @Override
        public FluidStack getOutputFluid() {
            return stirling.getHotWaterTank().getFluid();
        }

        @Override
        public int getOutputCapacity() {
            return stirling.getHotWaterTank().getCapacity();
        }
    }

    /**
     * 核反应堆（流体模式）的流体处理器实现
     */
    private static class NuclearReactorFluidHandler implements DualFluidHandler {
        private final com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor;

        public NuclearReactorFluidHandler(com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor) {
            this.reactor = reactor;
        }

        @Override
        public FluidStack getInputFluid() {
            var fluidHandler = reactor.getFluidHandler();
            return fluidHandler != null ? fluidHandler.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getInputCapacity() {
            var fluidHandler = reactor.getFluidHandler();
            return fluidHandler != null ? fluidHandler.getTankCapacity(0) : 0;
        }

        @Override
        public FluidStack getOutputFluid() {
            var fluidHandler = reactor.getFluidHandler();
            return fluidHandler != null ? fluidHandler.getFluidInTank(1) : FluidStack.EMPTY;
        }

        @Override
        public int getOutputCapacity() {
            var fluidHandler = reactor.getFluidHandler();
            return fluidHandler != null ? fluidHandler.getTankCapacity(1) : 0;
        }
    }
}

