package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 电动工具抽象基类，提供完整的默认能量存储和消耗实现。
 *
 * <p>Addon 开发者只需继承此类并指定构造参数即可创建一个功能完整的电动工具，
 * 无需访问任何 mio_icif 内部类。
 *
 * <h3>能量存储机制</h3>
 * <p>继承 {@link AbstractBattery} 的统一 DataComponent 能量存储（{@code mio_icif:battery_energy}），
 * 不依赖耐久度系统。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class MyElectricDrill extends AbstractElectricTool {
 *     public MyElectricDrill() {
 *         super(new Properties(), 30000, 0, 128, 100, 3);
 *     }
 * }
 * }</pre>
 *
 * <h3>自动提供的功能</h3>
 * <ul>
 *   <li>能量存储（基于统一 DataComponent）</li>
 *   <li>能量条渲染（绿→红渐变）</li>
 *   <li>能量 Tooltip 显示</li>
 *   <li>Shift 右键给装备充电</li>
 *   <li>充电器兼容（通过 IBatteryItem 接口）</li>
 *   <li>防止原版耐久消耗</li>
 *   <li>能量消耗与检查</li>
 *   <li>手持物品栏序列化/反序列化</li>
 * </ul>
 */
public class AbstractElectricTool extends AbstractBattery implements IElectricToolItem {

    private final long energyPerUse;
    private final int toolTier;

    /**
     * 创建电动工具（不可堆叠）。
     *
     * @param properties    物品属性
     * @param maxEnergy     最大能量容量 (EU)
     * @param initialEnergy 初始能量 (EU)
     * @param chargeRate    充电速率 (EU/tick)
     * @param energyPerUse  每次使用的能量消耗 (EU)
     * @param toolTier      工具等级
     */
    protected AbstractElectricTool(Properties properties, long maxEnergy, long initialEnergy,
                                   long chargeRate, long energyPerUse, int toolTier) {
        super(properties, maxEnergy, initialEnergy, chargeRate);
        this.energyPerUse = energyPerUse;
        this.toolTier = toolTier;
    }

    // ==================== IC2 chargeFromArmor 等价实现 ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        EnergyPackCharging.trackHolder(stack, entity);
    }

    /**
     * IC2 1.12.2 原版工具在 {@code ElectricItemManager.use()} 中会先调用 {@code chargeFromArmor()}：
     * 工具在读取/消耗能量前先从穿戴的充电背包取电。本项目各工具存在"先读 getEnergy 检查、
     * 再 consumeEnergy"的多种写法，因此在能量读取处统一补电（消耗路径必然会先读能量）。
     */
    @Override
    public long getEnergy(ItemStack stack) {
        EnergyPackCharging.pullFromArmor(stack);
        return super.getEnergy(stack);
    }

    // ==================== IElectricToolItem 实现 ====================

    @Override
    public long getEnergyPerUse() {
        return energyPerUse;
    }

    @Override
    public int getToolTier() {
        return toolTier;
    }

    @Override
    public boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= energyPerUse;
    }

    // ==================== 能量消耗 ====================

    /**
     * 检查是否有足够能量使用指定量
     *
     * @param stack  物品堆
     * @param amount 需要的能量
     * @return 是否有足够能量
     */
    public boolean hasEnoughEnergy(ItemStack stack, long amount) {
        return amount >= 0 && getEnergy(stack) >= amount;
    }

    /**
     * 消耗一次使用的能量
     *
     * @param stack 物品堆
     * @return 是否成功消耗
     */
    public boolean consumeEnergy(ItemStack stack) {
        return consumeEnergy(stack, energyPerUse);
    }

    /**
     * 消耗指定量的能量
     *
     * @param stack  物品堆
     * @param amount 消耗量
     * @return 是否成功消耗
     */
    public boolean consumeEnergy(ItemStack stack, long amount) {
        return com.miophas.singularity_iteration.core.api.item.BatteryTransfer.consume(stack, this, amount);
    }

    /**
     * 获取能量剩余百分比 (0.0 ~ 1.0)
     */
    public float getDurabilityPercent(ItemStack stack) {
        return (float) getEnergy(stack) / getMaxEnergy();
    }

    /**
     * 获取剩余使用次数
     */
    public long getRemainingUses(ItemStack stack) {
        return energyPerUse <= 0 ? 0 : getEnergy(stack) / energyPerUse;
    }

    /**
     * 检查工具是否可以使用（有能量）
     */
    public boolean canUse(ItemStack stack) {
        return !isEmpty(stack);
    }

    // ==================== 防止原版耐久消耗 ====================

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<Item> onBroken) {
        return 0;
    }

    // ==================== 手持物品栏序列化 ====================

    /**
     * 将手持物品栏保存到物品的 CUSTOM_DATA 中。
     * 适用于电钻等带有内置物品栏的电动工具。
     *
     * @param containerStack 容器物品堆
     * @param inventory      物品栏数组
     * @param registries     注册表访问器
     */
    public static void saveHandHeldInventory(ItemStack containerStack, ItemStack[] inventory, net.minecraft.core.HolderLookup.Provider registries) {
        if (containerStack.isEmpty()) return;

        CompoundTag rootTag;
        CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            rootTag = customData.copyTag();
        } else {
            rootTag = new CompoundTag();
        }

        ListTag contentList = new ListTag();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null && !inventory[i].isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putInt("Slot", i);
                Tag itemTag = inventory[i].save(registries);
                if (itemTag instanceof CompoundTag compound) {
                    slotNbt.put("Item", compound);
                }
                contentList.add(slotNbt);
            }
        }
        rootTag.put("HandHeldItems", contentList);

        containerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    /**
     * 从物品的 CUSTOM_DATA 中加载手持物品栏。
     *
     * @param containerStack 容器物品堆
     * @param inventory      物品栏数组（输出参数）
     * @param registries     注册表访问器
     */
    public static void loadHandHeldInventory(ItemStack containerStack, ItemStack[] inventory, net.minecraft.core.HolderLookup.Provider registries) {
        java.util.Arrays.fill(inventory, ItemStack.EMPTY);
        if (containerStack.isEmpty()) return;

        CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag rootTag = customData.copyTag();
        if (!rootTag.contains("HandHeldItems", Tag.TAG_LIST)) return;

        ListTag contentList = rootTag.getList("HandHeldItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < contentList.size(); i++) {
            CompoundTag slotNbt = contentList.getCompound(i);
            if (!slotNbt.contains("Slot", Tag.TAG_BYTE) && !slotNbt.contains("Slot", Tag.TAG_INT)) continue;
            int slot = slotNbt.contains("Slot", Tag.TAG_BYTE) ? Byte.toUnsignedInt(slotNbt.getByte("Slot")) : slotNbt.getInt("Slot");
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = ItemStack.parseOptional(registries, slotNbt.getCompound("Item"));
            }
        }
    }
}