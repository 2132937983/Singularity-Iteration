package com.singularity_iteration.mio_icif.Menu.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_rt_heat_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class RTHeatGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 6;
    public static final int FIRST_PELLET_SLOT = 0;
    public static final int LAST_PELLET_SLOT = 5;

    public RTHeatGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public RTHeatGeneratorMenu(int containerId, Inventory playerInventory,
                                @Nullable mio_icif_rt_heat_generator blockEntity,
                                @Nullable IItemHandler itemHandler,
                                @Nullable ContainerData data) {
        super(mio_icif_menus.RT_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, 4, blockEntity);
        // 注册RTG靶丸快速移动规则：优先放入RTG槽位（0-5）
        addCustomMoveRule(this::isRTGPellet, FIRST_PELLET_SLOT, LAST_PELLET_SLOT + 1);
    }

    private boolean isRTGPellet(ItemStack stack) {
        return stack.is(mio_icif_resources.RTG_PELLET.get());
    }

    public RTHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_rt_heat_generator blockEntity) {
        super(mio_icif_menus.RT_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, blockEntity != null ? blockEntity.getItemHandler() : null, null, 4, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int slotX = 62 + col * 18;
                int slotY = 27 + row * 18;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(mio_icif_resources.RTG_PELLET.get());
                    }
                });
            }
        }
    }

    public int getHeatStored() { return data.get(0); }
    public int getMaxHeatStored() { return data.get(1); }
    public int getHeatRate() { return data.get(2); }
    public int getHeatProgress() { return data.get(3); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_rt_heat_generator gen) {
            this.setSyncData(0, (int) gen.getHeatStorage().getHeatStored());
            this.setSyncData(1, (int) gen.getHeatStorage().getMaxHeatStored());
            this.setSyncData(2, gen.getCurrentHeatRate());
            long heat = gen.getHeatStorage().getHeatStored();
            long maxHeat = gen.getHeatStorage().getMaxHeatStored();
            this.setSyncData(3, maxHeat > 0 ? (int)(heat * 100 / maxHeat) : 0);
        }
    }

}