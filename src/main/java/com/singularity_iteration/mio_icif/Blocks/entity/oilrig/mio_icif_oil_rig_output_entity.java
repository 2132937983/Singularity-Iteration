package com.singularity_iteration.mio_icif.Blocks.entity.oilrig;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_oil_rig_output_entity extends BlockEntity {

    private static final int TANK_CAPACITY = 128000;

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY);

    public mio_icif_oil_rig_output_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.OIL_RIG_OUTPUT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_rig_output_entity blockEntity) {
        if (level.isClientSide()) return;
    }

    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return fluidTank.fill(resource, action);
    }

    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        return fluidTank.drain(maxDrain, action);
    }

    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public int getFluidProgress() {
        if (fluidTank.getCapacity() <= 0) return 0;
        return (fluidTank.getFluidAmount() * 100) / fluidTank.getCapacity();
    }

    @Nullable
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return fluidTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fluidTank", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("fluidTank")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluidTank"));
        }
    }
}