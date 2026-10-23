package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_large_fabricator_core_entity;

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
            private final int[] data = new int[20];   // 10 values, two 16-bit slots each

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

    /**
     * 0.1.10.15: logical value {@code index}, kept in two data slots of 16 bits. Vanilla syncs container
     * data as shorts, so the scrap value (50,000 per scrap) and UU-matter above 32,767 mB showed wrong.
     */
    private int value(int index) {
        return (this.data.get(2 * index + 1) << 16) | (this.data.get(2 * index) & 0xFFFF);
    }

    private void put(int index, int value) {
        this.data.set(2 * index, value & 0xFFFF);
        this.data.set(2 * index + 1, value >>> 16);
    }

    public long getEnergy() {
        return ((long) value(0) << 32) | (value(1) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) value(2) << 32) | (value(3) & 0xFFFFFFFFL);
    }

    public int getFluidAmount() {
        return value(4);
    }

    public int getFluidCapacity() {
        return value(5);
    }

    public int getScrapValue() {
        return value(6);
    }

    public int getInputModuleCount() {
        return value(7);
    }

    public int getTankModuleCount() {
        return value(8);
    }

    public int getScrapModuleCount() {
        return value(9);
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
            put(0, (int) (energy >> 32));
            put(1, (int) energy);
            put(2, (int) (maxEnergy >> 32));
            put(3, (int) maxEnergy);
            put(4, blockEntity.getTotalUuMatterAmount());
            put(5, blockEntity.getTotalUuMatterCapacity());
            put(6, (int) blockEntity.getScrapValue());
            put(7, blockEntity.getInputModuleCount());
            put(8, blockEntity.getTankModuleCount());
            put(9, blockEntity.getScrapModuleCount());
        }
    }
}