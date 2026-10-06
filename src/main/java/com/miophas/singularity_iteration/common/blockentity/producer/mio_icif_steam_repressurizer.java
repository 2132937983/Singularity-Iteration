package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_steam_repressurizer;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.energy.PlatformHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * IC2 2.8.221 steam repressurizer: 10 mB input + 1 HU per batch, producing
 * 16 / 32 mB ordinary steam. Heat is drawn on demand from all six neighbours.
 * IC2's external registered "steam" output is mapped to this mod's ordinary steam.
 */
@SuppressWarnings("null")
public class mio_icif_steam_repressurizer extends AbstractHeatBlockEntity {
    public static final int INPUT_TANK_CAPACITY = 10_000;
    public static final int OUTPUT_TANK_CAPACITY = 10_000;
    public static final int INPUT_BATCH = 10;
    public static final int NORMAL_OUTPUT_BATCH = 16;
    public static final int SUPERHEATED_OUTPUT_BATCH = 32;
    public static final int DATA_COUNT = 8;

    // IC2 can retain < aim HU, then draw aim HU (not aim - currentHeat).
    // At a full input tank aim <= 1000, so its ordinary buffer never exceeds 1999 HU.
    public static final int HEAT_CAPACITY = 2000;
    private static final int SAVE_VERSION = 1;
    // Recovery inventory only: no new items, no upgrades, no slots in the GUI.
    // Existing cells/upgrades remain extractable through automation or block drops.
    private static final SlotLayout LEGACY_LAYOUT = SlotLayout.builder().extra(4).build();

    protected final FluidTank inputTank;
    protected final FluidTank outputTank;
    private final IFluidHandler combinedFluidHandler;
    private boolean isWorking;

    private final ContainerData containerData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> inputTank.getFluidAmount();
                case 1 -> inputTank.getCapacity();
                case 2 -> outputTank.getFluidAmount();
                case 3 -> outputTank.getCapacity();
                case 4 -> (int) Math.min(getHeatStored(), Integer.MAX_VALUE);
                case 5 -> HEAT_CAPACITY;
                case 6 -> getInputFluidTypeId();
                case 7 -> getOutputFluidTypeId();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public mio_icif_steam_repressurizer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get());
    }

    public mio_icif_steam_repressurizer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, LEGACY_LAYOUT,
            new PlatformHeatStorage(HEAT_CAPACITY, 0, 0, 20, 700, 0));
        itemHandler.setValidator((slot, stack, slotType) -> false);
        inputTank = new FluidTank(INPUT_TANK_CAPACITY, fluid ->
            fluid.getFluid() == mio_icif_fluids.STEAM.get()
                || fluid.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get()) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
        outputTank = new FluidTank(OUTPUT_TANK_CAPACITY,
            fluid -> fluid.getFluid() == mio_icif_fluids.STEAM.get()) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
        combinedFluidHandler = FluidTankGroup.inputOutput(inputTank, outputTank);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            mio_icif_steam_repressurizer machine) {
        if (level.isClientSide()) return;
        int batches = machine.inputTank.getFluidAmount() / INPUT_BATCH;
        if (batches > 0 && machine.getHeatStored() < batches) machine.drawHeat(batches);
        machine.isWorking = machine.repressurizeSteam() > 0;
        // Keep the project's lit model, but never claim work without an actual conversion.
        BlockState actual = machine.getBlockState();
        if (actual.getValue(mio_icif_block_steam_repressurizer.LIT) != machine.isWorking) {
            level.setBlock(pos, actual.setValue(mio_icif_block_steam_repressurizer.LIT, machine.isWorking), 3);
        }
    }

    private void drawHeat(int aim) {
        if (level == null) return;
        long remaining = aim;
        for (Direction side : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbour)) continue;
            var source = level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
                neighbour, side.getOpposite());
            if (source == null || !source.canExtractHeat()) continue;
            long request = Math.min(remaining, heatStorage.generateHeatInternal(remaining, true));
            long offered = source.extractHeat(request, true);
            long drawn = source.extractHeat(Math.min(request, Math.max(0, offered)), false);
            if (drawn > 0) {
                generateHeatInternal(drawn, false);
                remaining -= drawn;
                if (remaining <= 0) break;
            }
        }
    }

    private int repressurizeSteam() {
        FluidStack input = inputTank.getFluid();
        if (input.isEmpty() || input.getAmount() < INPUT_BATCH) return 0;
        int outputPerBatch;
        if (input.getFluid() == mio_icif_fluids.STEAM.get()) {
            outputPerBatch = Singularity_Iteration_Config.REPRESSURIZER_STEAM_OUTPUT.get();
        } else if (input.getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get()) {
            outputPerBatch = Singularity_Iteration_Config.REPRESSURIZER_SUPERHEATED_OUTPUT.get();
        } else return 0;
        var product = new FluidStack(mio_icif_fluids.STEAM.get(), outputPerBatch);
        int freeOutput = outputTank.fill(product.copyWithAmount(Integer.MAX_VALUE),
            IFluidHandler.FluidAction.SIMULATE);
        int batches = (int) Math.min(getHeatStored(), Math.min(input.getAmount() / INPUT_BATCH,
            freeOutput / outputPerBatch));
        if (batches <= 0) return 0;
        consumeHeatInternal(batches, false);
        inputTank.drain(batches * INPUT_BATCH, IFluidHandler.FluidAction.EXECUTE);
        outputTank.fill(product.copyWithAmount(batches * outputPerBatch), IFluidHandler.FluidAction.EXECUTE);
        return batches;
    }

    // The source owns its direction/bandwidth; this consumer draws HU instead of accepting
    // speculative pushes. No steam demand means no heat drawn from a generator.
    @Override public long receiveHeat(long amount, boolean simulate) { return 0; }
    @Override public boolean canReceiveHeat() { return false; }
    @Override public boolean isOverheated() { return false; }
    @Override public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return this;
    }
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) { return itemHandler; }
    @Override public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return combinedFluidHandler;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("repressurizerVersion", SAVE_VERSION);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inputTank")) inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        if (tag.contains("outputTank")) outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        if (tag.getInt("repressurizerVersion") < SAVE_VERSION) {
            long extraHeat = Math.max(0, tag.getInt("currentHeat"));
            setHeat(getHeatStored() + Math.min(extraHeat, Long.MAX_VALUE - getHeatStored()));
            // Old implementation incorrectly produced superheated steam. Retain the volume,
            // migrate it to the correct product, and never truncate a legacy >10k mB balance.
            if (!outputTank.isEmpty() && outputTank.getFluid().getFluid() == mio_icif_fluids.SUPERHEATEDSTEAM.get()) {
                outputTank.setFluid(new FluidStack(mio_icif_fluids.STEAM.get(), outputTank.getFluidAmount()));
            }
        }
        isWorking = false;
    }

    @Override public Component getDisplayName() {
        return Component.translatable("container.mio_icif.steam_repressurizer");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.SteamRepressurizerMenu(id, inventory, this);
    }
    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public IFluidHandler getCombinedFluidHandler() { return combinedFluidHandler; }
    @Override public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getContainerData() { return containerData; }
    public boolean isWorking() { return isWorking; }
    public int getInputFluidTypeId() {
        return inputTank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid());
    }
    public int getOutputFluidTypeId() {
        return outputTank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(outputTank.getFluid().getFluid());
    }
}
