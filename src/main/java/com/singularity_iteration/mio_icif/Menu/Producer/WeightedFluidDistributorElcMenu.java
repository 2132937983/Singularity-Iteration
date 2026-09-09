package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_weighted_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class WeightedFluidDistributorElcMenu extends mio_icif_machine_menu {

    public static final int FLUID_INPUT_SLOT = 0;
    public static final int FLUID_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    private final BlockPos blockPos;

    public WeightedFluidDistributorElcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public WeightedFluidDistributorElcMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
            playerInventory.player.level().getBlockEntity(pos) instanceof mio_icif_weighted_fluid_distributor_elc be ? be : null);
    }

    public WeightedFluidDistributorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_weighted_fluid_distributor_elc blockEntity) {
        super(mio_icif_menus.WEIGHTED_FLUID_DISTRIBUTOR_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 11);
        this.blockPos = blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_INPUT_SLOT, 8, 108) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, FLUID_OUTPUT_SLOT, 152, 108) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    @Override
    protected int getPlayerInventoryY() { return 129; }

    @Override
    protected int getPlayerHotbarY() { return 187; }

    @Override
    protected int getBatterySlotIndex() { return -1; }

    @Override
    protected int getUpgradeSlotStart() { return -1; }

    @Override
    protected int getUpgradeSlotCount() { return 0; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getFacingOrdinal() { return data.get(5); }
    public int getPriorityCount() { return data.get(6); }
    public boolean isActive() { return data.get(7) == 1; }
    public int getFluidAmount() { return data.get(8); }
    public int getFluidCapacity() { return data.get(9); }
    public int getFluidId() { return data.get(10); }

    public Direction getFacing() {
        int ord = getFacingOrdinal();
        if (ord >= 0 && ord < Direction.values().length) return Direction.values()[ord];
        return Direction.NORTH;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_weighted_fluid_distributor_elc botter) {
            this.setSyncData(0, botter.getProgress());
            this.setSyncData(1, botter.getMaxProgress());
            this.setSyncData(2, botter.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) botter.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) botter.getEnergyStorage().getCapacity());
            this.setSyncData(5, botter.getFacing().get3DDataValue());
            this.setSyncData(6, botter.getPriority().size());
            this.setSyncData(7, botter.getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_weighted_fluid_distributor_elc.LIT) ? 1 : 0);
            this.setSyncData(8, botter.getFluidTank().getFluidAmount());
            this.setSyncData(9, botter.getFluidTank().getCapacity());
            FluidStack fluid = botter.getFluidTank().getFluid();
            this.setSyncData(10, fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        }
    }
}