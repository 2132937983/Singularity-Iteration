package com.singularity_iteration.mio_icif.Menu.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class HeatSourceFluidMenu extends mio_icif_machine_menu {
    public static final int INPUT_BUCKET_SLOT = 0;
    public static final int INPUT_EMPTY_SLOT = 1;
    public static final int OUTPUT_BUCKET_SLOT = 2;
    public static final int OUTPUT_FULL_SLOT = 3;
    public static final int HEAT_CONDUCTOR_START = 4;
    public static final int HEAT_CONDUCTOR_COUNT = 10;
    public static final int EXTRA_SLOT_START = 14;
    public static final int EXTRA_SLOT_COUNT = 3;
    public static final int SLOT_COUNT = 17;

    public HeatSourceFluidMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public HeatSourceFluidMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_heat_source_fluid blockEntity) {
        super(mio_icif_menus.HEAT_SOURCE_FLUID_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 10);
    }

        // 热交换机GUI高度�?04，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 204 - 82 = 122, 快捷栏Y = 204 - 24 = 180
    @Override
    protected int getPlayerInventoryY() { return 122; }

    @Override
    protected int getPlayerHotbarY() { return 180; }

    @Override
    protected int getDataSlotCount() { return 10; }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_BUCKET_SLOT, 8, 103) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.HOTCOOLANT.get());
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_EMPTY_SLOT, 26, 103) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_BUCKET_SLOT, 134, 103) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET) || mio_icif_cells.isFluidCell(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_FULL_SLOT, 152, 103) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = HEAT_CONDUCTOR_START + row * 5 + col;
                int slotX = 46 + col * 17;
                int slotY = 50 + row * 22;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(mio_icif_resources.HEATCONDUCTOR.get());
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return 1;
                    }
                });
            }
        }
        for (int i = 0; i < 3; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, EXTRA_SLOT_START + i, 62 + i * 18, 103));
        }
    }

    @Override
    protected int getBatterySlotIndex() { return -1; }

    public int getInputFluidAmount() { return data.get(0); }
    public int getInputFluidCapacity() { return data.get(1); }
    public int getOutputFluidAmount() { return data.get(2); }
    public int getOutputFluidCapacity() { return data.get(3); }
    public int getHeatStored() { return data.get(4); }
    public int getHeatCapacity() { return data.get(5); }
    public int getCurrentHeatOutput() { return data.get(6); }
    public int getMaxHeatOutput() { return data.get(7); }
    public int getInputFluidId() { return data.get(8); }
    public int getOutputFluidId() { return data.get(9); }

    public FluidStack getInputFluid() {
        int fluidId = getInputFluidId();
        if (fluidId >= 0) {
            return new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId), getInputFluidAmount());
        }
        return FluidStack.EMPTY;
    }

    public FluidStack getOutputFluid() {
        int fluidId = getOutputFluidId();
        if (fluidId >= 0) {
            return new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId), getOutputFluidAmount());
        }
        return FluidStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_heat_source_fluid heatSource) {
            this.setSyncData(0, heatSource.getInputFluidAmount());
            this.setSyncData(1, heatSource.getInputFluidCapacity());
            this.setSyncData(2, heatSource.getOutputFluidAmount());
            this.setSyncData(3, heatSource.getOutputFluidCapacity());
            this.setSyncData(4, (int) heatSource.getHeatStored());
            this.setSyncData(5, (int) heatSource.getHeatCapacity());
            this.setSyncData(6, heatSource.getCurrentHeatOutput());
            this.setSyncData(7, heatSource.getCurrentHeatOutput());
            this.setSyncData(8, net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(heatSource.getInputTank().getFluid().getFluid()));
            this.setSyncData(9, net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(heatSource.getOutputTank().getFluid().getFluid()));
        }
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < SLOT_COUNT) {
                // 从机器槽位移出到玩家物品栏
                if (!this.moveItemStackTo(itemStack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家物品栏移入到机器槽位
                boolean moved = false;

                // 热传导器：一个一个地放入空槽位
                if (itemStack1.is(mio_icif_resources.HEATCONDUCTOR.get())) {
                    moved = moveHeatConductorOneByOne(itemStack1);
                }
                // 输入桶/单元
                else if (itemStack1.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(itemStack1, mio_icif_fluids.HOTCOOLANT.get())) {
                    if (this.moveItemStackTo(itemStack1, INPUT_BUCKET_SLOT, INPUT_BUCKET_SLOT + 1, false)) {
                        moved = true;
                    }
                }
                // 输出空桶/单元
                else if (itemStack1.is(Items.BUCKET) || mio_icif_cells.isFluidCell(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, OUTPUT_BUCKET_SLOT, OUTPUT_BUCKET_SLOT + 1, false)) {
                        moved = true;
                    }
                }
                // 其他物品尝试放入额外槽位
                else {
                    if (this.moveItemStackTo(itemStack1, EXTRA_SLOT_START, EXTRA_SLOT_START + EXTRA_SLOT_COUNT, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }
        }
        return itemstack;
    }

    /**
     * 将热传导器一个一个地放入空槽位
     * @return 是否成功移动了至少一个物品
     */
    private boolean moveHeatConductorOneByOne(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(mio_icif_resources.HEATCONDUCTOR.get())) {
            return false;
        }

        // 找到第一个空的热传导器槽位
        for (int i = 0; i < HEAT_CONDUCTOR_COUNT; i++) {
            int slotIndex = HEAT_CONDUCTOR_START + i;
            net.minecraft.world.inventory.Slot targetSlot = this.slots.get(slotIndex);
            if (targetSlot != null && !targetSlot.hasItem()) {
                // 只移动一个物品到该槽位
                ItemStack singleItem = stack.split(1);
                targetSlot.setByPlayer(singleItem);
                return true;
            }
        }
        return false;
    }
}