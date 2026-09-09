package com.singularity_iteration.mio_icif.Blocks.entity.pipe;

import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_pipe_fluid_extract extends mio_icif_pipe_fluid implements IEnergySink {

    public static final String PIPE_TYPE = "fluid_extract";

    private Direction extractFacing = Direction.NORTH;


    public static final int BASE_TRANSFER_RATE = 10;
    public static final int MAX_TRANSFER_RATE = Integer.MAX_VALUE;

    public static final double EU_PER_MB = 0.168;
    public static final int MAX_EU_CONSUMPTION = 32;
    public static final long ENERGY_CAPACITY = 64;
    public static final long MAX_RECEIVE = 32;

    private int currentTransferRate = BASE_TRANSFER_RATE;
    private long storedEnergy = 0;
    private boolean energyRegistered = false;

    public mio_icif_pipe_fluid_extract(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public mio_icif_pipe_fluid_extract(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : mio_icif_block_entities.PIPE_WATER_EXTRACT_ENTITY_TYPE.get(), pos, state);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            energyRegistered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    @Override
    protected void doTransfer() {
        if (level == null || level.isClientSide()) return;

        syncFacingFromBlockState(getBlockState());

        updatePowerState();

        boolean wasEmpty = bufferFluid.isEmpty();

        extractFromBack();

        outputToFront();

        boolean isEmptyNow = bufferFluid.isEmpty();
        if (wasEmpty != isEmptyNow) {
            updateBlockState();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }


    private void updatePowerState() {
        if (storedEnergy > 0) {
            int speedBoost = (int) (storedEnergy / EU_PER_MB);
            currentTransferRate = Math.min(BASE_TRANSFER_RATE + speedBoost, MAX_TRANSFER_RATE);
            
            int speedAboveBase = currentTransferRate - BASE_TRANSFER_RATE;
            if (speedAboveBase > 0) {
                long energyConsumed = (long) (speedAboveBase * EU_PER_MB);
                storedEnergy = Math.max(0, storedEnergy - energyConsumed);
            }
        } else {
            currentTransferRate = BASE_TRANSFER_RATE;
        }
    }


    public int getCurrentTransferRate() {
        return currentTransferRate;
    }


    private void extractFromBack() {
        if (bufferFluid.getAmount() >= FLUID_CAPACITY) return;

        Direction extractDir = getExtractFacing();
        BlockPos adjacentPos = worldPosition.relative(extractDir);

        BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
        if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) return;

        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, adjacentPos, extractDir.getOpposite()
        );
        if (handler == null) return;

        int space = FLUID_CAPACITY - bufferFluid.getAmount();
        if (space <= 0) return;
        int toExtract = Math.min(space, currentTransferRate);

        FluidStack simulated = handler.drain(toExtract, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || simulated.getAmount() <= 0) return;
        if (!bufferFluid.isEmpty() && !bufferFluid.is(simulated.getFluid())) return;

        FluidStack drained = handler.drain(toExtract, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() <= 0) return;

        if (bufferFluid.isEmpty()) {
            bufferFluid = drained.copy();
        } else {
            bufferFluid.grow(drained.getAmount());
        }
        setChanged();
    }


    private void outputToFront() {
        if (bufferFluid.isEmpty()) return;

        Direction outputDir = getExtractFacing().getOpposite();
        BlockPos adjacentPos = worldPosition.relative(outputDir);

        BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
        if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) return;

        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, adjacentPos, outputDir.getOpposite()
        );
        if (handler == null) return;

        FluidStack fillTest = bufferFluid.copy();
        fillTest.setAmount(1);
        int fillResult = handler.fill(fillTest, IFluidHandler.FluidAction.SIMULATE);
        if (fillResult <= 0) return;

        int toTransfer = Math.min(bufferFluid.getAmount(), currentTransferRate);
        FluidStack toDrain = bufferFluid.copy();
        toDrain.setAmount(toTransfer);

        int cAmount;
        int amount = toTransfer;
        FluidStack ret;
        do {
            ret = toDrain.copy();
            ret.setAmount(amount);
            cAmount = handler.fill(ret, IFluidHandler.FluidAction.SIMULATE);
            if (cAmount > amount) {
                throw new IllegalStateException("Fill exceeded requested amount");
            }
            amount = cAmount;
        } while (amount != ret.getAmount() && amount > 0);

        if (amount <= 0) return;

        FluidStack drained = drainFromPipeInternal(amount, false);
        if (drained.isEmpty() || drained.getAmount() != amount) {
            throw new IllegalStateException("Drain inconsistent");
        }

        int filled = handler.fill(drained.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled != drained.getAmount()) {
            throw new IllegalStateException("Fill inconsistent");
        }
    }

    private FluidStack drainFromPipeInternal(int maxAmount, boolean simulate) {
        if (bufferFluid.isEmpty() || maxAmount <= 0) return FluidStack.EMPTY;

        int toDrain = Math.min(maxAmount, bufferFluid.getAmount());
        FluidStack result = new FluidStack(bufferFluid.getFluid(), toDrain);

        if (!simulate) {
            bufferFluid.shrink(toDrain);
            if (bufferFluid.isEmpty()) {
                bufferFluid = FluidStack.EMPTY;
            }
            setChanged();
        }

        return result;
    }

    @Override
    public double getDemandedEnergy() {
        long spaceAvailable = ENERGY_CAPACITY - storedEnergy;
        if (spaceAvailable <= 0) return 0;
        return Math.min(spaceAvailable, MAX_EU_CONSUMPTION);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double canReceive = Math.min(amount, MAX_RECEIVE);
        double overflow = amount - canReceive;
        storedEnergy += canReceive;
        if (storedEnergy > ENERGY_CAPACITY) {
            overflow += storedEnergy - ENERGY_CAPACITY;
            storedEnergy = ENERGY_CAPACITY;
        }
        return overflow;
    }

    @Override
    public int getSinkTier() {
        return EnergyNetGlobal.cableTierToSourceTier(CableTier.LV);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return true;
    }


    @Override
    protected boolean canConnectTo(BlockPos pos, Direction direction) {
        Direction extractDir = getExtractFacing();
        Direction outputDir = extractDir.getOpposite();

        if (direction != extractDir && direction != outputDir) {
            return false;
        }

        if (level == null) return false;

        BlockEntity adjacentEntity = level.getBlockEntity(pos);

        if (direction == extractDir) {
            if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) {
                return false;
            }
            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite()
            );
            return handler != null || adjacentEntity instanceof mio_icif_pipe_fluid;
        }

        if (direction == outputDir) {
            if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) {
                return false;
            }
            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite()
            );
            return handler != null || adjacentEntity instanceof mio_icif_pipe_fluid;
        }

        return false;
    }

    public Direction getExtractFacing() {
        return extractFacing;
    }

    public void setExtractFacing(Direction facing) {
        this.extractFacing = facing;
        setChanged();
    }

    @Override
    public String getPipeTypeString() {
        return PIPE_TYPE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("extract_facing", extractFacing.getName());
        tag.putInt("current_transfer_rate", currentTransferRate);
        tag.putLong("stored_energy", storedEnergy);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("extract_facing", CompoundTag.TAG_STRING)) {
            extractFacing = Direction.byName(tag.getString("extract_facing"));
            if (extractFacing == null) {
                extractFacing = Direction.NORTH;
            }
        }
        if (tag.contains("current_transfer_rate", CompoundTag.TAG_INT)) {
            currentTransferRate = tag.getInt("current_transfer_rate");
        }
        if (tag.contains("stored_energy", CompoundTag.TAG_LONG)) {
            storedEnergy = tag.getLong("stored_energy");
        }
        this.needsUpdate = true;
    }

    public void syncFacingFromBlockState(BlockState state) {
        if (state.hasProperty(mio_icif_block_pipe_water_extract.FACING)) {
            Direction facing = state.getValue(mio_icif_block_pipe_water_extract.FACING);
            Direction expectedExtract = facing.getOpposite();
            if (this.extractFacing != expectedExtract) {
                this.extractFacing = expectedExtract;
                this.needsUpdate = true;
                setChanged();
            }
        }
    }
}

