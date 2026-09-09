package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_rt_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * RTG发电机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class RTGeneratorMenu extends mio_icif_generator_menu {

    // 槽位索引
    public static final int SLOT_COUNT = 6;
    public static final int FIRST_PELLET_SLOT = 0;
    public static final int LAST_PELLET_SLOT = 5;

    // 槽位布局参数
    private static final int SLOT_START_X = 36;
    private static final int SLOT_START_Y = 36;
    private static final int SLOT_SPACING_X = 18;
    private static final int SLOT_SPACING_Y = 18;

    // 数据同步索引
    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_GENERATION_RATE = 2;
    private static final int DATA_ENERGY_PROGRESS = 3;
    private static final int DATA_COUNT = 4;

    public RTGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public RTGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public RTGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.RT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public RTGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_rt_generator blockEntity, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.RT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT, blockEntity);
        // 注册RTG靶丸快速移动规则：优先放入RTG槽位（0-5）
        addCustomMoveRule(this::isRTGPellet, FIRST_PELLET_SLOT, LAST_PELLET_SLOT + 1);
    }

    private boolean isRTGPellet(ItemStack stack) {
        return stack.is(mio_icif_resources.RTG_PELLET.get());
    }

    public void setSyncData(int energy, int maxEnergy, int generationRate, int energyProgress) {
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_GENERATION_RATE, generationRate);
        this.data.set(DATA_ENERGY_PROGRESS, energyProgress);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getGenerationRate() {
        return this.data.get(DATA_GENERATION_RATE);
    }

    public int getEnergyProgress() {
        return this.data.get(DATA_ENERGY_PROGRESS);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_rt_generator generator) {
            long energy = generator.getEnergyStorage().getAmount();
            long capacity = generator.getEnergyStorage().getCapacity();
            setSyncData(DATA_ENERGY, (int) energy);
            setSyncData(DATA_MAX_ENERGY, (int) capacity);
            setSyncData(DATA_GENERATION_RATE, (int) generator.getCurrentGenerationRate());
            setSyncData(DATA_ENERGY_PROGRESS, capacity > 0 ? (int) (energy * 100 / capacity) : 0);
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
        // 添加6个靶丸槽位?(2�?x 3�?，第一个槽位在(36, 36)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int slotX = SLOT_START_X + col * SLOT_SPACING_X;
                int slotY = SLOT_START_Y + row * SLOT_SPACING_Y;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(mio_icif_resources.RTG_PELLET.get());
                    }
                });
            }
        }
    }
}