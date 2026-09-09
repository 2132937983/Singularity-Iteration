package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_large_fabricator_tank_entity extends MultiblockEnergyPart {

    private static final long BUFFER_CAPACITY = 0L;
    private static final ICableTier MAX_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");

    public static final int TANK_CAPACITY = 128000;

    protected final FluidTank uuMatterTank;

    public mio_icif_large_fabricator_tank_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.LARGE_FABRICATOR_TANK.get(), BUFFER_CAPACITY, 0, 0, MAX_TIER);
        this.uuMatterTank = new FluidTank(TANK_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.UUMATTER.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_large_fabricator_tank_entity blockEntity) {
        if (level.isClientSide()) return;
    }

    public FluidTank getUuMatterTank() {
        return uuMatterTank;
    }

    @Override
    @Nullable
    public BlockEntity getJadeDisplayTarget() {
        return null;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.large_fabricator_tank");
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return uuMatterTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag fluidTag = new CompoundTag();
        uuMatterTank.writeToNBT(registries, fluidTag);
        tag.put("UuMatterTank", fluidTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("UuMatterTank")) {
            CompoundTag fluidTag = tag.getCompound("UuMatterTank");
            uuMatterTank.readFromNBT(registries, fluidTag);
        }
    }
}