package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_fermenter_elc;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class FermenterElcMenu extends mio_icif_machine_menu {
    public static final int BIOMASS_CELL_SLOT = 0;
    public static final int EMPTY_CELL_SLOT = 1;
    public static final int BIOGAS_CELL_SLOT = 2;
    public static final int FERTILIZER_SLOT = 3;
    public static final int UPGRADE_SLOT_START = 4;
    public static final int EMPTY_CELL_INPUT_SLOT = 6;
    // 对齐 IC2：2 个升级槽，总槽位数 2(生物质上下) + 2(输出) + 2(升级) + 1(沼气空容器输入) = 7
    public static final int SLOT_COUNT = 7;

    public FermenterElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public FermenterElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public FermenterElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler, @Nullable net.minecraft.world.inventory.ContainerData data) {
        super(mio_icif_menus.FERMENTER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 9);
        addCustomMoveRule(stack -> mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOMASS.get()), BIOMASS_CELL_SLOT, BIOMASS_CELL_SLOT + 1);
        addCustomMoveRule(stack -> mio_icif_cells.isEmptyCell(stack), EMPTY_CELL_INPUT_SLOT, EMPTY_CELL_INPUT_SLOT + 1);
    }

    // GUI高度184，按照原版IC2逻辑调整玩家物哝栝佝�?
    // 背包Y = 184 - 82 = 102, 快杷栝Y = 184 - 24 = 160
    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    public FermenterElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_fermenter_elc blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public FermenterElcMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler,
                             @Nullable net.minecraft.world.inventory.ContainerData data, @Nullable mio_icif_fermenter_elc blockEntity) {
        super(mio_icif_menus.FERMENTER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, 9, blockEntity);
        addCustomMoveRule(stack -> mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOMASS.get()), BIOMASS_CELL_SLOT, BIOMASS_CELL_SLOT + 1);
        addCustomMoveRule(stack -> mio_icif_cells.isEmptyCell(stack), EMPTY_CELL_INPUT_SLOT, EMPTY_CELL_INPUT_SLOT + 1);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BIOMASS_CELL_SLOT, 14, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOMASS.get());
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_CELL_SLOT, 14, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_cells.isEmptyCell(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_CELL_INPUT_SLOT, 148, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_cells.isEmptyCell(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BIOGAS_CELL_SLOT, 148, 61) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, FERTILIZER_SLOT, 86, 83) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        int[] upgradeX = {125, 143};
        int[] upgradeY = {83, 83};
        for (int i = 0; i < 2; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, upgradeX[i], upgradeY[i]);
        }
    }

    @Override
    protected int getDataSlotCount() { return 9; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getHeat() { return data.get(3); }
    public int getMaxHeat() { return data.get(4); }
    public int getBiomassAmount() { return data.get(5); }
    public int getBiomassCapacity() { return data.get(6); }
    public int getBiogasAmount() { return data.get(7); }
    public int getBiogasCapacity() { return data.get(8); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_fermenter_elc fermenter) {
            setSyncData(0, fermenter.getProgress());
            setSyncData(1, fermenter.getMaxProgress());
            setSyncData(2, fermenter.isWorking() ? 1 : 0);
            setSyncData(3, (int) fermenter.getHeatStored());
            setSyncData(4, fermenter.getHeatCapacity());
            setSyncData(5, fermenter.getBiomassAmount());
            setSyncData(6, fermenter.getBiomassCapacity());
            setSyncData(7, fermenter.getBiogasAmount());
            setSyncData(8, fermenter.getBiogasCapacity());
        }
    }

}