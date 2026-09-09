package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_distributor_elc;
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
public class FluidDistributorElcMenu extends mio_icif_machine_menu {

    public static final int FLUID_INPUT_SLOT = 0;
    public static final int FLUID_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    @Nullable
    private BlockPos blockPos;

    public FluidDistributorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (mio_icif_fluid_distributor_elc) null);
    }

    public FluidDistributorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_fluid_distributor_elc blockEntity) {
        super(mio_icif_menus.FLUID_DISTRIBUTOR_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 4);
        if (blockEntity != null) {
            this.blockPos = blockEntity.getBlockPos();
        }
    }

    public FluidDistributorElcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, (mio_icif_fluid_distributor_elc) null);
        if (data != null) {
            this.blockPos = data.readBlockPos();
        }
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_INPUT_SLOT, 9, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_OUTPUT_SLOT, 9, 72) {
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
    protected int getBatterySlotIndex() { return -1; }

    @Override
    protected int getUpgradeSlotStart() { return -1; }

    @Override
    protected int getUpgradeSlotCount() { return 0; }

    public boolean isActive() { return data.get(0) == 1; }
    public int getFluidAmount() { return data.get(1); }
    public int getFluidCapacity() { return data.get(2); }
    public int getFluidId() { return data.get(3); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_fluid_distributor_elc dist) {
            this.setSyncData(0, dist.isActive() ? 1 : 0);
            this.setSyncData(1, dist.getFluidTank().getFluidAmount());
            this.setSyncData(2, dist.getFluidTank().getCapacity());
            FluidStack fluid = dist.getFluidTank().getFluid();
            this.setSyncData(3, fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        }
    }
}