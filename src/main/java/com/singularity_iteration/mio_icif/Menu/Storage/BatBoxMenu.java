package com.singularity_iteration.mio_icif.Menu.Storage;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"})
public class BatBoxMenu extends mio_icif_base_menu {

    @SuppressWarnings("unused")
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Container container;
    private final mio_icif_Energy_Container blockEntity;

    public BatBoxMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public BatBoxMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Energy_Container blockEntity) {
        super(mio_icif_menus.BAT_BOX_MENU_TYPE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(6);
        this.data = new ContainerData() {
            private final int[] data = new int[5];

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

        // 机器槽位：充电槽 + 电池�?
        this.addSlot(new Slot(container, 0, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isChargeable(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });

        this.addSlot(new Slot(container, 1, 56, 53) {
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
        markMachineSlots(2);

        // 显示槽（盔甲槽）- 按装备类型严格限制
        // Slot 0: 靴子 (FEET)
        this.addSlot(new Slot(playerInventory, 36, 8, 84) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.FEET;
            }

            @Override
            public boolean allowModification(Player player) {
                return true;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        // Slot 1: 护腿 (LEGS)
        this.addSlot(new Slot(playerInventory, 37, 26, 84) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.LEGS;
            }

            @Override
            public boolean allowModification(Player player) {
                return true;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        // Slot 2: 胸甲 (CHEST)
        this.addSlot(new Slot(playerInventory, 38, 44, 84) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.CHEST;
            }

            @Override
            public boolean allowModification(Player player) {
                return true;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });

        // Slot 3: 头盔 (HEAD)
        this.addSlot(new Slot(playerInventory, 39, 62, 84) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
            }

            @Override
            public boolean allowModification(Player player) {
                return true;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });
        markPlayerSlots(4);

        // 玩家物品栈?
        addPlayerInventory(playerInventory, 114, 172);
        markPlayerSlots(36); // 3*9背包 + 9快捷�?
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || MioIcifAPI.instance().getItemAPI().isElectricArmor(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    @Override
    protected boolean isChargeable(ItemStack stack) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isBattery(stack)) {
            return !api.isBatteryFull(stack);
        }
        if (api.isElectricArmor(stack)) {
            return api.getElectricArmorStored(stack) < api.getElectricArmorMaxEnergy(stack);
        }
        return false;
    }

    @Override
    protected int getBatterySlotIndex() {
        return 1;
    }

    @Override
    protected int getChargeSlotIndex() {
        return 0;
    }

    public long getEnergy() {
        return ((long) this.data.get(0) << 32) | (this.data.get(1) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) this.data.get(2) << 32) | (this.data.get(3) & 0xFFFFFFFFL);
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
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getEnergyStorage() != null) {
            long energy = blockEntity.getEnergyStorage().getAmount();
            long maxEnergy = blockEntity.getEnergyStorage().getCapacity();
            this.data.set(0, (int) (energy >> 32));
            this.data.set(1, (int) energy);
            this.data.set(2, (int) (maxEnergy >> 32));
            this.data.set(3, (int) maxEnergy);
        }
        if (blockEntity != null) {
            this.data.set(4, blockEntity.getRedstoneMode());
        }
    }
    
    /**
     * 获取当前红石模式
     */
    public int getRedstoneMode() {
        return this.data.get(4);
    }

    /**
     * 切换到下一个红石模式
     */
    public void cycleRedstoneMode() {
        if (blockEntity != null) {
            blockEntity.cycleRedstoneMode();
        }
    }

    /**
     * 处理按钮点击（由客户端发送）
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // 切换红石模式
            cycleRedstoneMode();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    /**
     * 重写快速移动逻辑
     * 当从充电槽移出装备时，优先放入对应的装备槽而不是玩家物品栏
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        // 只处理从充电槽（index 0）移出的情况
        if (index == getChargeSlotIndex()) {
            Slot slot = this.slots.get(index);
            if (slot == null || !slot.hasItem()) {
                return ItemStack.EMPTY;
            }

            ItemStack itemStack1 = slot.getItem().copy();
            if (itemStack1.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // 检查是否是装备物品
            if (itemStack1.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                // 获取装备对应的槽位索引
                int targetSlot = getEquipmentSlotIndex(armorItem);
                if (targetSlot >= 0 && targetSlot < this.slots.size()) {
                    Slot target = this.slots.get(targetSlot);
                    if (target != null && !target.hasItem()) {
                        // 目标装备槽为空，直接放入
                        slot.setByPlayer(ItemStack.EMPTY);
                        target.setByPlayer(itemStack1);
                        return itemStack1;
                    }
                }
            }

            // 如果不是装备或装备槽已满，使用默认逻辑移动到玩家物品栏
            int playerInvStart = 6; // 跳过机器槽(0-1)和显示槽(2-5)
            if (!this.moveItemStackTo(itemStack1, playerInvStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }

            if (itemStack1.getCount() == slot.getItem().getCount()) {
                return ItemStack.EMPTY;
            }
            return itemStack1;
        }

        // 其他槽位使用默认逻辑
        return super.quickMoveStack(player, index);
    }

    /**
     * 获取装备对应的槽位索引
     * 根据ArmorItem类型返回对应的显示槽索引
     * 注意：playerInventory的装备槽索引顺序是：36=FEET, 37=LEGS, 38=CHEST, 39=HEAD
     */
    private int getEquipmentSlotIndex(net.minecraft.world.item.ArmorItem armorItem) {
        net.minecraft.world.entity.EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
        // 显示槽映射: slot 2=FEET(36), slot 3=LEGS(37), slot 4=CHEST(38), slot 5=HEAD(39)
        return switch (equipmentSlot) {
            case HEAD -> 5;   // playerInventory[39]
            case CHEST -> 4;  // playerInventory[38]
            case LEGS -> 3;   // playerInventory[37]
            case FEET -> 2;   // playerInventory[36]
            default -> -1;
        };
    }
}