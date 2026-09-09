package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_megnetizer;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class MagnetizerMenu extends mio_icif_base_menu {

    @SuppressWarnings("unused")
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container container;
    private final mio_icif_megnetizer blockEntity;

    public MagnetizerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public MagnetizerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_megnetizer blockEntity) {
        super(mio_icif_menus.MAGNETIZER_MENU_TYPE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(6);
        this.data = new ContainerData() {
            private final int[] data = new int[3];

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

        // 机器槽位：电池槽
        this.addSlot(new Slot(container, mio_icif_megnetizer.BATTERY_SLOT, 8, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });
        markMachineSlots(1);

        // 显示槽（盔甲槽）
        this.addSlot(new Slot(playerInventory, 36, 45, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean allowModification(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        this.addSlot(new Slot(playerInventory, 39, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean allowModification(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        this.addSlot(new Slot(playerInventory, 38, 152, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean allowModification(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        this.addSlot(new Slot(playerInventory, 37, 152, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean allowModification(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });
        markPlayerSlots(4);

        // 机器槽位：输入槽
        this.addSlot(new Slot(container, mio_icif_megnetizer.INPUT_SLOT, 152, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean allowModification(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });
        markMachineSlots(1);

        // 玩家物品栈?
        addPlayerInventory(playerInventory, 84, 142);
        markPlayerSlots(36); // 3*9背包 + 9快捷�?
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        return api.isBattery(stack) || api.isElectricArmor(stack) ||
               stack.getItem() == Items.REDSTONE;
    }

    @Override
    protected int getBatterySlotIndex() {
        return 0; // Menu中电池槽的索引是0
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public boolean isWorking() {
        return this.data.get(2) == 1;
    }

    public int getEnergyProgressPixels() {
        int energy = this.data.get(0);
        int maxEnergy = this.data.get(1);
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
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getEnergyStorage() != null) {
            int energyM = (int) blockEntity.getEnergyStorage().getAmount();
            int maxEnergyM = (int) blockEntity.getEnergyStorage().getCapacity();
            this.data.set(0, energyM);
            this.data.set(1, maxEnergyM);
            this.data.set(2, blockEntity.isWorking() ? 1 : 0);
        }
    }
}