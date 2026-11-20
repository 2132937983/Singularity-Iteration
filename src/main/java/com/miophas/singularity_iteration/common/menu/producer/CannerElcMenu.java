package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_canner_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private final Level recipeLevel;

    public CannerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CannerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_canner_elc blockEntity) {
        super(mio_icif_menus.CANNER_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 15);
        this.recipeLevel = playerInventory.player.level();
        // The two fluid handler slots have no GUI slots.
        markMachineSlots(8);
        markPlayerSlots(36);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 10, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !isBattery(stack) && !isUpgrade(stack)
                    && mio_icif_canner_elc.acceptsCannerInput(recipeLevel, admissionMode(), stack);
            }
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return mayPlace(stack) ? Math.min(getMaxStackSize(), stack.getMaxStackSize()) : 0;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 61, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, MATERIAL_SLOT, 30, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                ItemStack inputCan = itemHandler.getStackInSlot(INPUT_SLOT);
                return mio_icif_canner_elc.acceptsCannerMaterial(recipeLevel, admissionMode(), getInputFluid(), inputCan, stack);
            }
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return mayPlace(stack) ? Math.min(getMaxStackSize(), stack.getMaxStackSize()) : 0;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 10, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.normal.mio_icif_bat;
            }
        });
        // 与原版 IC2 ContainerCanner 一致：升级槽 (152, 26)，间距 18
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, 152, 8 + i * 18);
        }
    }

    // ---- 0.1.11.116: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 240, INVENTORY_X = 44, INVENTORY_Y = 160, HOTBAR_Y = 218;

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getDataSlotCount() { return 15; }

    /** 0.1.11.116: data 12 status (ProcessStatus), 13 EU/t, 14 stored energy ‰ (0-11 as before). */
    public int getStatus() { return data.get(12); }
    public int getPower() { return data.get(13); }
    public int getEnergyPermille() { return data.get(14); }

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

    private mio_icif_canner_elc.Mode admissionMode() {
        return blockEntity instanceof mio_icif_canner_elc canner
            ? canner.getMode() : mio_icif_canner_elc.Mode.fromId(getMode());
    }

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
            this.setSyncData(12, canner.processStatus());
            this.setSyncData(13, (int) Math.min(Short.MAX_VALUE, canner.getEffectiveEnergyPerTick()));
            this.setSyncData(14, (int) Math.round(1000.0 * canner.getEnergyStorage().getAmount() / Math.max(1, canner.getEnergyStorage().getCapacity())));
        }
    }
}