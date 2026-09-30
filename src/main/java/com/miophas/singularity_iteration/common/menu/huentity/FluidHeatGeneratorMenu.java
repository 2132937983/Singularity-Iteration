package com.miophas.singularity_iteration.common.menu.huentity;

import com.miophas.singularity_iteration.common.blockentity.generator.SemifluidFuels;
import com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_fluid_heat_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class FluidHeatGeneratorMenu extends mio_icif_machine_menu {
    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.FLUID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 6);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_fluid_heat_generator blockEntity) {
        super(mio_icif_menus.FLUID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.createContainerData() : null, 6, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_BUCKET_SLOT, 27, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.isEmpty()) return false;
                if (stack.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
                    return SemifluidFuels.accepts(bucket.content);
                }
                var content = mio_icif_cells.getCellFluid(stack);
                return !content.isEmpty() && SemifluidFuels.accepts(content.getFluid());
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 27, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public int getBurnTime() { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }
    public int getFuelAmount() { return data.get(2); }
    public int getFuelCapacity() { return data.get(3); }
    /** 最近一次实际输出的 HU/t（IC2 transmitHeat 同义，没输出时保持上一次读数）。 */
    public int getTransmitHeat() { return data.get(4); }
    /** 当前燃料的产热能力 HU/t，无燃料为 0。 */
    public int getMaxHeatOutput() { return data.get(5); }

    public int getBurnProgress() {
        int maxBurnTime = getMaxBurnTime();
        if (maxBurnTime <= 0) return 0;
        return ((maxBurnTime - getBurnTime()) * 100) / maxBurnTime;
    }

    public boolean isWorking() {
        // 有合法燃料即在产热；不要用 burnTime（会随燃料罐耗尽才归零）之外的本 tick 状态判断。
        return getMaxHeatOutput() > 0;
    }
}