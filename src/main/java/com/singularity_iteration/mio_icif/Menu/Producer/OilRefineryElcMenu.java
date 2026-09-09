package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_oil_refinery_elc;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class OilRefineryElcMenu extends mio_icif_machine_menu {

    public static final int INPUT_CELL_SLOT = 0;
    public static final int INPUT_EMPTY_CELL_SLOT = 1;
    public static final int OUTPUT_EMPTY_CELL_SLOT = 2;
    public static final int OUTPUT_CELL_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;
    public static final int SLOT_COUNT = 9;

    public OilRefineryElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public OilRefineryElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public OilRefineryElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler, @Nullable net.minecraft.world.inventory.ContainerData data) {
        super(mio_icif_menus.OIL_REFINERY_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 11);
    }

    @Override
    protected int getPlayerInventoryY() { return 84; }

    @Override
    protected int getPlayerHotbarY() { return 142; }

    public OilRefineryElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_oil_refinery_elc blockEntity) {
        this(containerId, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null,
            blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public OilRefineryElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler,
                               @Nullable net.minecraft.world.inventory.ContainerData data, @Nullable mio_icif_oil_refinery_elc blockEntity) {
        super(mio_icif_menus.OIL_REFINERY_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            itemHandler, data, 11, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_CELL_SLOT, 25, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
                    return refinery.isItemValidForSlot(INPUT_CELL_SLOT, stack);
                }
                if (mio_icif_cells.isFluidCell(stack)) return true;
                return FluidUtil.getFluidContained(stack).isPresent();
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_EMPTY_CELL_SLOT, 25, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
                    return refinery.isItemValidForSlot(INPUT_EMPTY_CELL_SLOT, stack);
                }
                if (mio_icif_cells.isEmptyCell(stack)) return true;
                if (stack.getItem() == net.minecraft.world.item.Items.BUCKET) return true;
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_EMPTY_CELL_SLOT, 127, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_cells.isEmptyCell(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_CELL_SLOT, 127, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 76, 63) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }
        });
        int upgradeX = 152;
        int[] upgradeY = {8, 26, 44, 62};
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, upgradeX, upgradeY[i]);
        }
    }

    @Override
    protected int getDataSlotCount() { return 11; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getInputFluidAmount() { return data.get(5); }
    public int getInputFluidCapacity() { return data.get(6); }
    public int getOutputFluidAmount() { return data.get(7); }
    public int getOutputFluidCapacity() { return data.get(8); }

    public FluidStack getInputFluid() {
        if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
            return refinery.getInputFluid();
        }
        int fluidId = data.get(9);
        if (fluidId == -1) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);
        return fluid != null ? new FluidStack(fluid, getInputFluidAmount()) : FluidStack.EMPTY;
    }

    public FluidStack getOutputFluid() {
        if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
            return refinery.getOutputFluid();
        }
        int fluidId = data.get(10);
        if (fluidId == -1) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);
        return fluid != null ? new FluidStack(fluid, getOutputFluidAmount()) : FluidStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
            setSyncData(0, refinery.getProgress());
            setSyncData(1, refinery.getMaxProgress());
            setSyncData(2, refinery.isWorking() ? 1 : 0);
            setSyncData(3, (int) refinery.getEnergyStorage().getAmount());
            setSyncData(4, (int) refinery.getEnergyStorage().getCapacity());
            setSyncData(5, refinery.getInputTank().getFluidAmount());
            setSyncData(6, refinery.getInputTank().getCapacity());
            setSyncData(7, refinery.getOutputTank().getFluidAmount());
            setSyncData(8, refinery.getOutputTank().getCapacity());
            setSyncData(9, refinery.getInputFluidTypeId());
            setSyncData(10, refinery.getOutputFluidTypeId());
        }
    }
}