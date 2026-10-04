package com.miophas.singularity_iteration.common.menu.storage;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.batbox.mio_icif_gesu_core_entity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class GESUCoreMenu extends mio_icif_base_menu {

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container container;
    private final mio_icif_gesu_core_entity blockEntity;

    public GESUCoreMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public GESUCoreMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_gesu_core_entity blockEntity) {
        super(mio_icif_menus.GESU_CORE_MENU_TYPE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(2);
        this.data = new ContainerData() {
            private final int[] data = new int[6];

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

        // machine slots on the BatBox texture frames: charge (top) and discharge (bottom)
        this.addSlot(new Slot(container, mio_icif_gesu_core_entity.CHARGE_SLOT, 56, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return isChargeable(stack); }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        });
        this.addSlot(new Slot(container, mio_icif_gesu_core_entity.DISCHARGE_SLOT, 56, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return isBattery(stack); }
            @Override public int getMaxStackSize(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.REDSTONE) ? 64 : 1; }
        });
        markMachineSlots(2);

        // armor display slots (feet, legs, chest, head) like the other storage boxes
        EquipmentSlot[] armor = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < 4; i++) {
            EquipmentSlot type = armor[i];
            this.addSlot(new Slot(playerInventory, 36 + i, 8 + i * 18, 84) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == type;
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        markPlayerSlots(4);

        // 玩家背包(3x9)与快捷栏(主物品栏)整体下移 2px
        addPlayerInventory(playerInventory, 114, 172);
        markPlayerSlots(36);
    }

    public long getEnergy() {
        return ((long) this.data.get(0) << 32) | (this.data.get(1) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) this.data.get(2) << 32) | (this.data.get(3) & 0xFFFFFFFFL);
    }

    public int getInputModuleCount() {
        return this.data.get(4);
    }

    public int getOutputModuleCount() {
        return this.data.get(5);
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
        return com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.canDischarge(stack);
    }

    @Override
    protected boolean isChargeable(ItemStack stack) {
        return com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging.canCharge(stack);
    }

    @Override
    protected int getChargeSlotIndex() {
        return 0;
    }

    @Override
    protected int getBatterySlotIndex() {
        return 1;
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
            this.data.set(4, blockEntity.getInputModuleCount());
            this.data.set(5, blockEntity.getOutputIvModuleCount() + blockEntity.getOutputLuvModuleCount());
        }
    }
}