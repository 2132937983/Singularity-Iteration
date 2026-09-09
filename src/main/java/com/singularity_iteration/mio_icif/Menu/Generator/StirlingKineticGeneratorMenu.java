package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 斯特林动能发生机的容器菜单类
 */
@SuppressWarnings({"null"}) public class StirlingKineticGeneratorMenu extends mio_icif_generator_menu {

    // 数据同步索引
    private static final int DATA_WATER_AMOUNT = 0;
    private static final int DATA_HOT_WATER_AMOUNT = 1;
    private static final int DATA_MAX_FLUID = 2;
    private static final int DATA_BUFFERED_HEAT = 3;
    private static final int DATA_PROGRESS = 4;
    private static final int DATA_MAX_PROGRESS = 5;
    private static final int DATA_COUNT = 6;
    private static final int SLOT_COUNT = 7;  // extra(2) + upgrade(3) + extra(2) = 7个槽位

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STIRLING_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Stirling_Kinetic_Generator blockEntity) {
        super(mio_icif_menus.STIRLING_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null, null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int waterAmount, int hotWaterAmount, int maxFluid, int bufferedHeat, int progress, int maxProgress) {
        this.data.set(DATA_WATER_AMOUNT, waterAmount);
        this.data.set(DATA_HOT_WATER_AMOUNT, hotWaterAmount);
        this.data.set(DATA_MAX_FLUID, maxFluid);
        this.data.set(DATA_BUFFERED_HEAT, bufferedHeat);
        this.data.set(DATA_PROGRESS, progress);
        this.data.set(DATA_MAX_PROGRESS, maxProgress);
    }

    public int getWaterAmount() {
        return this.data.get(DATA_WATER_AMOUNT);
    }

    public int getHotWaterAmount() {
        return this.data.get(DATA_HOT_WATER_AMOUNT);
    }

    public int getMaxFluidAmount() {
        return this.data.get(DATA_MAX_FLUID);
    }

    public int getBufferedHeat() {
        return this.data.get(DATA_BUFFERED_HEAT);
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
            setSyncData(
                generator.getWaterAmount(),
                generator.getHotWaterAmount(),
                generator.getMaxFluidAmount(),
                generator.getBufferedHeat(),
                generator.getProgress(),
                generator.getMaxProgress()
            );
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
        // 斯特林动能发生机槽位布局:
        // 0-1: 水桶输入、空桶输出 (8,65) 和 (26,65)
        // 2-4: 插件槽 (62,65) 3个并排
        // 5-6: 物品槽 (134,65) 2个并排
        if (itemHandler != null) {
            // 水桶输入槽 (0)
            this.addSlot(new SlotItemHandler(itemHandler, 0, 8, 65));
            // 空桶输出槽 (1)
            this.addSlot(new SlotItemHandler(itemHandler, 1, 26, 65));
            // 插件槽 (2-4)
            for (int i = 0; i < 3; i++) {
                this.addSlot(new SlotItemHandler(itemHandler, 2 + i, 62 + i * 18, 65));
            }
            // 物品槽 (5-6)
            for (int i = 0; i < 2; i++) {
                this.addSlot(new SlotItemHandler(itemHandler, 5 + i, 134 + i * 18, 65));
            }
        }
    }
}