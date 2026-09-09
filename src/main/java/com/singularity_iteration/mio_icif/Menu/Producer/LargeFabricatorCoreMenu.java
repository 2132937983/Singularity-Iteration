package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_core_entity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class LargeFabricatorCoreMenu extends mio_icif_base_menu {

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container container;
    private final mio_icif_large_fabricator_core_entity blockEntity;

    public static final int ENERGY_BAR_WIDTH = 100;
    public static final int ENERGY_BAR_HEIGHT = 8;

    public LargeFabricatorCoreMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public LargeFabricatorCoreMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_large_fabricator_core_entity blockEntity) {
        super(mio_icif_menus.LARGE_FABRICATOR_CORE_MENU_TYPE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.container = new SimpleContainer(0);
        this.data = new ContainerData() {
            private final int[] data = new int[10];

            @Override
            public int get(int index) {
                return data[index];
            }

            @Override
            public void set(int index, int value) {
                data[index] = value;
            }

            @Override
            public int getCount() {
                return data.length;
            }
        };

        this.addDataSlots(data);

        addPlayerInventory(playerInventory, 84, 142);
        markPlayerSlots(36);
    }

    public long getEnergy() {
        return ((long) this.data.get(0) << 32) | (this.data.get(1) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) this.data.get(2) << 32) | (this.data.get(3) & 0xFFFFFFFFL);
    }

    public int getFluidAmount() {
        return this.data.get(4);
    }

    public int getFluidCapacity() {
        return this.data.get(5);
    }

    public int getScrapValue() {
        return this.data.get(6);
    }

    public int getInputModuleCount() {
        return this.data.get(7);
    }

    public int getTankModuleCount() {
        return this.data.get(8);
    }

    public int getScrapModuleCount() {
        return this.data.get(9);
    }

    public int getEnergyProgressPixels() {
        long energy = getEnergy();
        long maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) {
            return 0;
        }
        return (int) ((energy * ENERGY_BAR_WIDTH) / maxEnergy);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null) {
            long energy = blockEntity.getStoredEnergy();
            long maxEnergy = blockEntity.getEnergyCapacity();
            this.data.set(0, (int) (energy >> 32));
            this.data.set(1, (int) energy);
            this.data.set(2, (int) (maxEnergy >> 32));
            this.data.set(3, (int) maxEnergy);
            this.data.set(4, blockEntity.getTotalUuMatterAmount());
            this.data.set(5, blockEntity.getTotalUuMatterCapacity());
            this.data.set(6, (int) blockEntity.getScrapValue());
            this.data.set(7, blockEntity.getInputModuleCount());
            this.data.set(8, blockEntity.getTankModuleCount());
            this.data.set(9, blockEntity.getScrapModuleCount());
        }
    }
}