package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IEnergyPackItem;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 电池背包基类
 * 参考 IC2 原版 ItemArmorBatpack / ItemArmorLappack / ItemArmorAdvBatpack
 *
 * IC2 原版充电背包逻辑：
 * - canProvideEnergy = true：表示背包可以对外供电
 * - 通过 ElectricItem.manager 系统自动为玩家所有电力物品充电
 * - 充电范围：手持物品（主手/副手）、穿戴的其他电力护甲 + 背包中的电力物品
 * - 充电速率受 transferLimit 限制（BatPack=100 EU/t, AdvBatPack=1000 EU/t, Lappack=2500 EU/t）
 * - 不吸收伤害（getDamageAbsorptionRatio = 0, getEnergyPerDamage = 0）
 *
 * 充电优先级（参考 IC2）：
 * 1. 主手物品（玩家正在使用的工具优先充电）
 * 2. 副手物品
 * 3. 穿戴的其他电力护甲（头盔/护腿/靴子）
 * 4. 背包中的电力物品（电池/工具等）
 */
@SuppressWarnings({"null", "deprecation"})
public abstract class mio_icif_chestplate_energy_pack extends mio_icif_armor_elc implements IEnergyPackItem {

    // 每 tick 充电速率（子类自定义，对应 IC2 的 transferLimit）
private final int chargeRate;

    public mio_icif_chestplate_energy_pack(Holder<ArmorMaterial> material, Properties properties,
                                            int maxEnergy, String texturePrefix,
                                            int chargeRate, int armorTier) {
        super(material, Type.CHESTPLATE, properties, maxEnergy, 0, texturePrefix, chargeRate, 0, armorTier);
        this.chargeRate = chargeRate;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }

        // 必须穿在胸甲槽才生效
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate != stack) {
            return;
        }

        // 背包没有能量则不充电
        if (isEmpty(stack)) {
            return;
        }

        // 剩余可分配能量
    int remainingEnergy = chargeRate;

        // 第一步：为主手物品充电（最高优先级，玩家正在使用的工具）
    remainingEnergy = chargeItem(stack, player.getMainHandItem(), remainingEnergy);
        if (remainingEnergy <= 0) return;

        // 第二步：为副手物品充电
    remainingEnergy = chargeItem(stack, player.getOffhandItem(), remainingEnergy);
        if (remainingEnergy <= 0) return;

        // 第三步：为穿戴的其他电力护甲充电（头盔/护腿/靴子）
    EquipmentSlot[] otherArmorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : otherArmorSlots) {
            if (remainingEnergy <= 0) return;
            ItemStack armorStack = player.getItemBySlot(slot);
            if (armorStack.isEmpty() || armorStack.getItem() == this) continue;
            remainingEnergy = chargeItem(stack, armorStack, remainingEnergy);
        }
        if (remainingEnergy <= 0) return;

        // 第四步：为背包中的电力物品充电（电池/工具等）
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (remainingEnergy <= 0) break;
            ItemStack invStack = inventory.getItem(i);
            if (invStack.isEmpty()) continue;
            // 跳过已处理的护甲槽位（它们已经在第三步处理过了）
            if (isEquipmentSlotItem(player, invStack)) continue;
            remainingEnergy = chargeItem(stack, invStack, remainingEnergy);
        }
    }

    /**
     * 检查物品是否在装备槽位中（已通过护甲槽处理）
     */
    private boolean isEquipmentSlotItem(Player player, ItemStack stack) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (player.getItemBySlot(slot) == stack) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试用背包能量为指定物品充电
     * @param packStack 背包物品堆
     * @param targetStack 目标物品堆
     * @param maxTransfer 最大可传输能量
     * @return 剩余可分配能量
     */
    private int chargeItem(ItemStack packStack, ItemStack targetStack, int maxTransfer) {
        if (targetStack.isEmpty() || maxTransfer <= 0) {
            return maxTransfer;
        }

        var api = MioIcifAPI.instance().getItemAPI();

        // 尝试给电力护甲充电
        if (api.isElectricArmor(targetStack)) {
            long currentEnergy = api.getElectricArmorStored(targetStack);
            long maxEnergy = api.getElectricArmorMaxEnergy(targetStack);
            if (currentEnergy >= maxEnergy) {
                return maxTransfer;
            }

            long spaceAvailable = maxEnergy - currentEnergy;
            int transfer = (int) Math.min(maxTransfer, spaceAvailable);
            transfer = Math.min(transfer, (int) getEnergy(packStack));

            if (transfer > 0) {
                api.chargeElectricArmor(targetStack, transfer, false);
                extractEnergy(packStack, transfer);
                return maxTransfer - transfer;
            }
            return maxTransfer;
        }

        // 尝试给电池充电
        if (api.isBattery(targetStack)) {
            long currentEnergy = api.getBatteryStored(targetStack);
            long maxEnergy = api.getBatteryCapacity(targetStack);
            if (currentEnergy >= maxEnergy) {
                return maxTransfer;
            }

            long spaceAvailable = maxEnergy - currentEnergy;
            long transfer = Math.min(maxTransfer, spaceAvailable);
            transfer = Math.min(transfer, getEnergy(packStack));

            if (transfer > 0) {
                api.chargeBattery(targetStack, transfer, false);
                extractEnergy(packStack, (int) transfer);
                return maxTransfer - (int) transfer;
            }
        }

        return maxTransfer;
    }

    /**
     * 充电背包可以对外提供能量（对应 IC2 的 canProvideEnergy = true）
     */
    @Override
    public long getEnergyPerDamage() {
        return 0; // 充电背包不吸收伤害
}
}