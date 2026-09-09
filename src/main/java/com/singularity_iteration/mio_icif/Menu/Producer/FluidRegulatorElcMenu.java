package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_regulator_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class FluidRegulatorElcMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int FLUID_INPUT_SLOT = 1;
    public static final int FLUID_OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private final BlockPos blockPos;

    public FluidRegulatorElcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public FluidRegulatorElcMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
            playerInventory.player.level().getBlockEntity(pos) instanceof mio_icif_fluid_regulator_elc be ? be : null);
    }

    public FluidRegulatorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_fluid_regulator_elc blockEntity) {
        super(mio_icif_menus.FLUID_REGULATOR_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 11);
        this.blockPos = blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_INPUT_SLOT, 58, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_OUTPUT_SLOT, 58, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getUpgradeSlotStart() { return -1; }

    @Override
    protected int getUpgradeSlotCount() { return 0; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getOutputmb() { return data.get(5); }
    public int getMode() { return data.get(6); }
    public boolean isActive() { return data.get(7) == 1; }
    public int getFluidAmount() { return data.get(8); }
    public int getFluidCapacity() { return data.get(9); }
    public int getFluidId() { return data.get(10); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_fluid_regulator_elc reg) {
            this.setSyncData(0, reg.getProgress());
            this.setSyncData(1, reg.getMaxProgress());
            this.setSyncData(2, reg.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) reg.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) reg.getEnergyStorage().getCapacity());
            this.setSyncData(5, reg.getOutputmb());
            this.setSyncData(6, reg.getMode());
            this.setSyncData(7, reg.getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_regulator_elc.LIT) ? 1 : 0);
            this.setSyncData(8, reg.getFluidTank().getFluidAmount());
            this.setSyncData(9, reg.getFluidTank().getCapacity());
            FluidStack fluid = reg.getFluidTank().getFluid();
            this.setSyncData(10, fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        }
    }
}