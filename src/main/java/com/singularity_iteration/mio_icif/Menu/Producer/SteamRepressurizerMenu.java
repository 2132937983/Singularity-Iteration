package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_repressurizer;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽冝加压机的容器蝜坕类
 */
@SuppressWarnings({"null"}) public class SteamRepressurizerMenu extends mio_icif_machine_menu {

    private static final int INPUT_CELL_X = 8;
    private static final int INPUT_CELL_Y = 17;
    private static final int OUTPUT_CELL_X = 8;
    private static final int OUTPUT_CELL_Y = 53;
    private static final int[] UPGRADE_X = {151, 151};
    private static final int[] UPGRADE_Y = {7, 25};

    // 数杮坌步索引
    private static final int DATA_INPUT_AMOUNT = 0;
    private static final int DATA_INPUT_CAPACITY = 1;
    private static final int DATA_OUTPUT_AMOUNT = 2;
    private static final int DATA_OUTPUT_CAPACITY = 3;
    private static final int DATA_HEAT = 4;
    private static final int DATA_MAX_HEAT = 5;
    private static final int DATA_COUNT = 6;
    private static final int SLOT_COUNT = 4;

    private FluidStack inputFluid = FluidStack.EMPTY;
    private FluidStack outputFluid = FluidStack.EMPTY;

    public SteamRepressurizerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public SteamRepressurizerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SteamRepressurizerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STEAM_REPRESSURIZER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public SteamRepressurizerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_repressurizer blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public SteamRepressurizerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                  @Nullable ContainerData data, @Nullable mio_icif_steam_repressurizer blockEntity) {
        super(mio_icif_menus.STEAM_REPRESSURIZER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int inputAmount, int inputCapacity, int outputAmount, int outputCapacity, int heat, int maxHeat) {
        this.data.set(DATA_INPUT_AMOUNT, inputAmount);
        this.data.set(DATA_INPUT_CAPACITY, inputCapacity);
        this.data.set(DATA_OUTPUT_AMOUNT, outputAmount);
        this.data.set(DATA_OUTPUT_CAPACITY, outputCapacity);
        this.data.set(DATA_HEAT, heat);
        this.data.set(DATA_MAX_HEAT, maxHeat);
    }

    public void setSyncFluids(FluidStack inputFluid, FluidStack outputFluid) {
        this.inputFluid = inputFluid;
        this.outputFluid = outputFluid;
    }

    public int getInputAmount() {
        return this.data.get(DATA_INPUT_AMOUNT);
    }

    public int getInputCapacity() {
        return this.data.get(DATA_INPUT_CAPACITY);
    }

    public int getOutputAmount() {
        return this.data.get(DATA_OUTPUT_AMOUNT);
    }

    public int getOutputCapacity() {
        return this.data.get(DATA_OUTPUT_CAPACITY);
    }

    public int getHeat() {
        return this.data.get(DATA_HEAT);
    }

    public int getMaxHeat() {
        return this.data.get(DATA_MAX_HEAT);
    }

    public FluidStack getInputFluid() {
        return this.inputFluid;
    }

    public FluidStack getOutputFluid() {
        return this.outputFluid;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
            setSyncData(DATA_INPUT_AMOUNT, repressurizer.getInputTank().getFluidAmount());
            setSyncData(DATA_INPUT_CAPACITY, repressurizer.getInputTank().getCapacity());
            setSyncData(DATA_OUTPUT_AMOUNT, repressurizer.getOutputTank().getFluidAmount());
            setSyncData(DATA_OUTPUT_CAPACITY, repressurizer.getOutputTank().getCapacity());
            setSyncData(DATA_HEAT, (int) repressurizer.getHeatStorage().getHeatStored());
            setSyncData(DATA_MAX_HEAT, (int) repressurizer.getHeatStorage().getMaxHeatStored());
        }
    }

    @Override
    protected void addMachineSlots() {
        // 输入坕元�?
        this.addSlot(new SlotItemHandler(itemHandler, 0, INPUT_CELL_X, INPUT_CELL_Y));
        // 输出坕元�?
        this.addSlot(new SlotItemHandler(itemHandler, 1, OUTPUT_CELL_X, OUTPUT_CELL_Y) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
        // 均级�?
        for (int i = 0; i < 2; i++) {
            addUpgradeSlot(itemHandler, 2 + i, UPGRADE_X[i], UPGRADE_Y[i]);
        }
    }

    @Override
    protected int getDataSlotCount() {
        return DATA_COUNT;
    }
}