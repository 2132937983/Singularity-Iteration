package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_canner_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class CannerElcMenu extends mio_icif_machine_menu {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int MATERIAL_SLOT = 2;
    public static final int BATTERY_SLOT = 3;
    public static final int UPGRADE_SLOT_START = 4;
    public static final int SLOT_COUNT = 10;

    public CannerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CannerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_canner_elc blockEntity) {
        super(mio_icif_menus.CANNER_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 12);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 41, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return itemHandler.isItemValid(INPUT_SLOT, stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 119, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, MATERIAL_SLOT, 80, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return itemHandler.isItemValid(MATERIAL_SLOT, stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 80) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
            }
        });
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, 152, 26 + i * 17);
        }
    }

    // GUI高度184，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 184 - 82 = 102, 快捷栏Y = 184 - 24 = 160
    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getDataSlotCount() { return 12; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getInputFluidAmount() { return data.get(5); }
    public int getInputFluidCapacity() { return data.get(6); }
    public int getOutputFluidAmount() { return data.get(7); }
    public int getOutputFluidCapacity() { return data.get(8); }
    public int getMode() { return data.get(9); }

    public FluidStack getInputFluid() {
        if (blockEntity instanceof mio_icif_canner_elc canner) {
            return canner.getInputFluid();
        }
        int fluidId = data.get(10);
        if (fluidId == -1) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);
        return fluid != null ? new FluidStack(fluid, getInputFluidAmount()) : FluidStack.EMPTY;
    }

    public FluidStack getOutputFluid() {
        if (blockEntity instanceof mio_icif_canner_elc canner) {
            return canner.getOutputFluid();
        }
        int fluidId = data.get(11);
        if (fluidId == -1) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);
        return fluid != null ? new FluidStack(fluid, getOutputFluidAmount()) : FluidStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity instanceof mio_icif_canner_elc canner && !player.level().isClientSide()) {
            if (id == 0) {
                canner.nextMode();
                return true;
            } else if (id == 1) {
                canner.swapFluids();
                return true;
            } else if (id == 2) {
                canner.clearInputTank();
                return true;
            } else if (id == 3) {
                canner.clearOutputTank();
                return true;
            }
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_canner_elc canner) {
            this.setSyncData(0, canner.getProgress());
            this.setSyncData(1, canner.getMaxProgress());
            this.setSyncData(2, canner.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) canner.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) canner.getEnergyStorage().getCapacity());
            this.setSyncData(5, canner.getInputFluidAmount());
            this.setSyncData(6, canner.getInputFluidCapacity());
            this.setSyncData(7, canner.getOutputFluidAmount());
            this.setSyncData(8, canner.getOutputFluidCapacity());
            this.setSyncData(9, canner.getModeId());
            this.setSyncData(10, canner.getInputFluidTypeId());
            this.setSyncData(11, canner.getOutputFluidTypeId());
        }
    }
}