package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Item API.
 *
 * <p>Provides an interface for querying mio_icif items and related
 * functionality, including:
 * <ul>
 *   <li>Querying mod items</li>
 *   <li>Checking item types (electric tools, batteries, armor, etc.)</li>
 *   <li>Managing custom durability systems</li>
 *   <li>Battery charge and discharge operations</li>
 *   <li>Fluid cell operations (fill, drain, capacity query)</li>
 *   <li>Jetpack mode and thrust control</li>
 *   <li>Armor feature toggling</li>
 *   <li>Upgrade plugin direction and effects</li>
 * </ul>
 *
 * <h3>Fluid Cell API (Addon 开发者必读)</h3>
 * <p>流体单元 API 提供了完整的流体容器操作支持：
 * <ul>
 *   <li>{@link #isFluidCell(ItemStack)} - 检查物品是否为流体单元</li>
 *   <li>{@link #getFluidCellCapacity(ItemStack)} - 获取单元容量</li>
 *   <li>{@link #getFluidCellContent(ItemStack)} - 获取当前存储的流体</li>
 *   <li>{@link #canFluidCellHold(ItemStack, Fluid)} - 检查是否可以存储指定流体</li>
 *   <li>{@link #fillFluidCell(ItemStack, FluidStack, boolean)} - 填充流体</li>
 *   <li>{@link #drainFluidCell(ItemStack, int, boolean)} - 排出流体</li>
 *   <li>{@link #getFluidCellEmptyContainer(ItemStack)} - 获取排空后的空容器</li>
 *   <li>{@link #getFluidCellFilledContainer(ItemStack, Fluid)} - 获取填充后的满容器</li>
 * </ul>
 * <p>实现 {@link IFluidCellItem} 接口的物品可以：
 * <ul>
 *   <li>通过 Forge Fluid Capability 与其他模组的流体系统交互</li>
 *   <li>右键点击储罐直接传输流体</li>
 *   <li>通过发射器发射/吸取流体</li>
 *   <li>在罐装机中自动处理（无需硬编码映射）</li>
 *   <li>支持部分填充（非固定 1000mB）</li>
 * </ul>
 */
public interface IItemAPI {

    /**
     * Get an item by its ID.
     *
     * @param id the item ID
     * @return the item, or null if it does not exist
     */
    Item getItem(ResourceLocation id);

    /**
     * Check whether an item is an electric tool.
     *
     * @param stack the item stack
     * @return true if it is an electric tool
     */
    boolean isElectricTool(ItemStack stack);

    /**
     * Check whether an item is a battery.
     *
     * @param stack the item stack
     * @return true if it is a battery
     */
    boolean isBattery(ItemStack stack);

    /**
     * Check whether an item is electric armor.
     *
     * @param stack the item stack
     * @return true if it is electric armor
     */
    boolean isElectricArmor(ItemStack stack);

    /**
     * Check whether an item is a reactor component.
     *
     * @param stack the item stack
     * @return true if it is a reactor component
     */
    boolean isReactorComponent(ItemStack stack);

    /**
     * Check whether an item is an upgrade plugin.
     *
     * @param stack the item stack
     * @return true if it is an upgrade plugin
     */
    boolean isUpgrade(ItemStack stack);

    /**
     * Check whether an item can be processed by a lathe.
     *
     * @param stack the item stack
     * @return true if it is a lathe-processable item
     */
    boolean isLatheItem(ItemStack stack);

    /**
     * Check whether an item uses a custom durability system.
     *
     * @param stack the item stack
     * @return true if it uses custom durability
     */
    boolean hasCustomDamage(ItemStack stack);

    /**
     * Get the current custom durability damage value of an item.
     *
     * @param stack the item stack
     * @return the current damage value
     */
    int getCustomDamage(ItemStack stack);

    /**
     * Get the maximum custom durability of an item.
     *
     * @param stack the item stack
     * @return the maximum durability
     */
    int getMaxCustomDamage(ItemStack stack);

    /**
     * Get the maximum energy capacity (EU) of a battery item.
     *
     * @param stack the battery item stack
     * @return the maximum capacity, or 0 for non-battery items
     */
    long getBatteryCapacity(ItemStack stack);

    /**
     * Get the currently stored energy (EU) in a battery item.
     *
     * @param stack the battery item stack
     * @return the currently stored energy, or 0 for non-battery items
     */
    long getBatteryStored(ItemStack stack);

    /**
     * Charge a battery item.
     *
     * @param stack the battery item stack
     * @param amount the amount of energy to charge (EU)
     * @param simulate if true, do not actually modify the item
     * @return the amount of energy actually charged
     */
    long chargeBattery(ItemStack stack, long amount, boolean simulate);

    /**
     * Discharge a battery item.
     *
     * @param stack the battery item stack
     * @param amount the amount of energy to discharge (EU)
     * @param simulate if true, do not actually modify the item
     * @return the amount of energy actually discharged
     */
    long dischargeBattery(ItemStack stack, long amount, boolean simulate);

    // ========== 电动工具 API ==========

    /**
     * 获取电动工具的最大能量容量 (EU)
     *
     * @param stack 工具物品
     * @return 最大容量，非电动工具返回 0
     */
    long getElectricToolMaxEnergy(ItemStack stack);

    /**
     * 获取电动工具当前存储的能量 (EU)
     *
     * @param stack 工具物品
     * @return 当前能量，非电动工具返回 0
     */
    long getElectricToolStored(ItemStack stack);

    /**
     * 获取电动工具每次使用的能量消耗 (EU)
     *
     * @param stack 工具物品
     * @return 每次消耗的能量，非电动工具返回 0
     */
    long getElectricToolEnergyPerUse(ItemStack stack);

    /**
     * 获取电动工具的等级
     *
     * @param stack 工具物品
     * @return 工具等级，非电动工具返回 0
     */
    int getElectricToolTier(ItemStack stack);

    /**
     * 为电动工具充电
     *
     * @param stack 工具物品
     * @param amount 充电量 (EU)
     * @param simulate 是否模拟
     * @return 实际充入的能量
     */
    long chargeElectricTool(ItemStack stack, long amount, boolean simulate);

    /**
     * 从电动工具放电
     *
     * @param stack 工具物品
     * @param amount 放电量 (EU)
     * @param simulate 是否模拟
     * @return 实际放出的能量
     */
    long dischargeElectricTool(ItemStack stack, long amount, boolean simulate);

    /**
     * 检查电动工具是否有足够能量使用一次
     *
     * @param stack 工具物品
     * @return 是否有足够能量
     */
    boolean hasEnoughToolEnergy(ItemStack stack);

    // ========== 电力装甲 API ==========

    /**
     * 获取电力装甲的最大能量容量 (EU)
     *
     * @param stack 装甲物品
     * @return 最大容量，非电力装甲返回 0
     */
    long getElectricArmorMaxEnergy(ItemStack stack);

    /**
     * 获取电力装甲当前存储的能量 (EU)
     *
     * @param stack 装甲物品
     * @return 当前能量，非电力装甲返回 0
     */
    long getElectricArmorStored(ItemStack stack);

    /**
     * 获取电力装甲每 tick 的能量消耗 (EU)
     *
     * @param stack 装甲物品
     * @return 每 tick 消耗，非电力装甲返回 0
     */
    long getElectricArmorEnergyPerTick(ItemStack stack);

    /**
     * 获取电力装甲的等级
     *
     * @param stack 装甲物品
     * @return 装甲等级，非电力装甲返回 0
     */
    int getElectricArmorTier(ItemStack stack);

    /**
     * 为电力装甲充电
     *
     * @param stack 装甲物品
     * @param amount 充电量 (EU)
     * @param simulate 是否模拟
     * @return 实际充入的能量
     */
    long chargeElectricArmor(ItemStack stack, long amount, boolean simulate);

    /**
     * 从电力装甲放电
     *
     * @param stack 装甲物品
     * @param amount 放电量 (EU)
     * @param simulate 是否模拟
     * @return 实际放出的能量
     */
    long dischargeElectricArmor(ItemStack stack, long amount, boolean simulate);

    /**
     * 检查电力装甲是否有足够能量
     *
     * @param stack 装甲物品
     * @return 是否有足够能量
     */
    boolean hasEnoughArmorEnergy(ItemStack stack);

    /**
     * 消耗电力装甲的能量
     *
     * @param stack 装甲物品
     * @param amount 消耗量 (EU)
     * @return 是否成功消耗
     */
    boolean consumeArmorEnergy(ItemStack stack, long amount);

    // ========== 反应堆组件 API ==========

    /**
     * 获取反应堆组件的类型 ID
     *
     * @param stack 物品
     * @return 类型 ID，非反应堆组件返回 null
     */
    String getReactorComponentType(ItemStack stack);

    // ========== 升级插件 API ==========

    /**
     * 获取升级插件的类型
     *
     * @param stack 物品
     * @return 升级类型，非升级插件返回 null
     */
    String getUpgradeType(ItemStack stack);

    /**
     * 获取升级插件是否是定向的
     *
     * @param stack 物品
     * @return 是否定向，非升级插件返回 false
     */
    boolean isUpgradeDirectional(ItemStack stack);

    /**
     * 获取升级插件的方向
     *
     * @param stack 升级插件物品
     * @return 方向，未设置或非定向升级返回 null
     */
    net.minecraft.core.Direction getUpgradeDirection(ItemStack stack);

    /**
     * 获取所有 mod 物品 ID
     *
     * @return 命名空间为 mio_icif 的物品 ID 集合
     */
    Collection<ResourceLocation> getAllItemIds();

    // ========== 电池充放电速率 API ==========

    /**
     * 获取电池的充电速率
     *
     * @param stack 电池物品
     * @return 充电速率 (EU/tick)，非电池物品返回 0
     */
    long getChargeRate(ItemStack stack);

    /**
     * 获取电池的放电速率
     *
     * @param stack 电池物品
     * @return 放电速率 (EU/tick)，非电池物品返回 0
     */
    long getDischargeRate(ItemStack stack);

    /**
     * 检查电池是否已充满
     *
     * @param stack 电池物品
     * @return true 如果已充满
     */
    boolean isBatteryFull(ItemStack stack);

    /**
     * 检查电池是否为空
     *
     * @param stack 电池物品
     * @return true 如果电量为0
     */
    boolean isBatteryEmpty(ItemStack stack);

    // ========== 升级插件效果 API ==========

    /**
     * 获取升级插件的速度加成
     *
     * @param stack 升级插件物品
     * @return 速度加成倍率（值越小越快）
     */
    float getUpgradeSpeedBonus(ItemStack stack);

    /**
     * 获取升级插件的能量加成
     *
     * @param stack 升级插件物品
     * @return 能量消耗倍率
     */
    float getUpgradeEnergyBonus(ItemStack stack);

    /**
     * 获取升级插件的等级
     *
     * @param stack 升级插件物品
     * @return 升级等级 (1-4, 对应 LV-MV-HV-EV)
     */
    int getUpgradeTier(ItemStack stack);

    // ========== 新物品类型查询 API ==========

    /**
     * 检查物品是否为武器
     *
     * @param stack 物品堆
     * @return true 如果是武器
     */
    boolean isWeapon(ItemStack stack);

    /**
     * 检查物品是否为太阳能头盔
     *
     * @param stack 物品堆
     * @return true 如果是太阳能头盔
     */
    boolean isSolarHelmet(ItemStack stack);

    /**
     * 检查物品是否为喷气背包
     *
     * @param stack 物品堆
     * @return true 如果是喷气背包
     */
    boolean isJetpack(ItemStack stack);

    /**
     * 检查物品是否为流体单元
     *
     * @param stack 物品堆
     * @return true 如果是流体单元
     */
    boolean isFluidCell(ItemStack stack);

    /**
     * 获取武器的基础伤害值
     *
     * @param stack 武器物品
     * @return 伤害值，非武器返回 0
     */
    float getWeaponDamage(ItemStack stack);

    /**
     * 获取武器的有效射程（方块数）
     *
     * @param stack 武器物品
     * @return 射程，非武器返回 0
     */
    float getWeaponRange(ItemStack stack);

    /**
     * 获取武器每次射击消耗的能量
     *
     * @param stack 武器物品
     * @return 每次射击能耗（EU），非武器返回 0
     */
    long getWeaponEnergyPerShot(ItemStack stack);

    /**
     * 获取太阳能头盔的发电速率
     *
     * @param stack 太阳能头盔物品
     * @return 每 tick 发电量（EU），非太阳能头盔返回 0
     */
    long getSolarGenerationRate(ItemStack stack);

    /**
     * 获取喷气背包的推力
     *
     * @param stack 喷气背包物品
     * @return 推力值，非喷气背包返回 0
     */
    float getJetpackThrust(ItemStack stack);

    /**
     * 获取喷气背包每 tick 飞行消耗的能量
     *
     * @param stack 喷气背包物品
     * @return 每 tick 飞行能耗（EU），非喷气背包返回 0
     */
    long getJetpackEnergyPerTick(ItemStack stack);

    /**
     * 获取喷气背包当前模式
     *
     * @param stack 喷气背包物品
     * @return 当前模式，非喷气背包返回 OFF
     */
    IJetpackItem.JetpackMode getJetpackMode(ItemStack stack);

    /**
     * 设置喷气背包模式
     *
     * @param stack 喷气背包物品
     * @param mode  目标模式
     */
    void setJetpackMode(ItemStack stack, IJetpackItem.JetpackMode mode);

    /**
     * 获取流体单元的最大容量
     *
     * @param stack 流体单元物品
     * @return 容量（mB），非流体单元返回 0
     */
    int getFluidCellCapacity(ItemStack stack);

    /**
     * 获取流体单元当前存储的流体
     *
     * @param stack 流体单元物品
     * @return 当前流体堆，非流体单元返回空
     */
    net.neoforged.neoforge.fluids.FluidStack getFluidCellContent(ItemStack stack);

    /**
     * 检查流体单元是否可以存储指定流体
     *
     * @param stack 流体单元物品
     * @param fluid 流体类型
     * @return true 如果可以存储
     */
    boolean canFluidCellHold(ItemStack stack, net.minecraft.world.level.material.Fluid fluid);

    /**
     * 向流体单元填充流体
     *
     * @param stack 流体单元物品
     * @param fluid 要填充的流体
     * @param simulate 是否仅模拟
     * @return 实际填充的量（mB）
     */
    int fillFluidCell(ItemStack stack, net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate);

    /**
     * 从流体单元排出流体
     *
     * @param stack 流体单元物品
     * @param amount 要排出的量（mB）
     * @param simulate 是否仅模拟
     * @return 实际排出的流体堆
     */
    net.neoforged.neoforge.fluids.FluidStack drainFluidCell(ItemStack stack, int amount, boolean simulate);

    /**
     * 获取流体单元排空后的空容器
     *
     * @param stack 流体单元物品
     * @return 排空后的空容器，非流体单元返回空
     */
    ItemStack getFluidCellEmptyContainer(ItemStack stack);

    /**
     * 获取流体单元填充后的满容器
     *
     * @param stack 流体单元物品
     * @param fluid 要填充的流体
     * @return 填充后的满容器，非流体单元返回空
     */
    ItemStack getFluidCellFilledContainer(ItemStack stack, net.minecraft.world.level.material.Fluid fluid);

    /**
     * 检查物品是否是动态流体单元（支持任意流体和部分填充）
     *
     * @param stack 物品堆
     * @return true 如果是动态流体单元
     */
    boolean isDynamicFluidCell(ItemStack stack);

    /**
     * 创建动态流体单元并填充指定流体
     *
     * @param fluid 要填充的流体
     * @param amount 填充量（mB）
     * @return 填充后的动态单元，失败返回空
     */
    ItemStack createDynamicFluidCell(net.minecraft.world.level.material.Fluid fluid, int amount);

    // ========== 装甲特性 API ==========

    /**
     * 获取电力装甲的特性列表
     *
     * @param stack 装甲物品
     * @return 特性信息列表，非电力装甲返回空列表
     */
    java.util.List<ArmorFeatureInfo> getArmorFeatures(ItemStack stack);

    /**
     * 切换电力装甲的特性状态
     *
     * @param stack 装甲物品
     * @param featureKey 特性键
     * @return 切换后的状态
     */
    boolean toggleArmorFeature(ItemStack stack, String featureKey);

    /**
     * 获取电力装甲每点伤害消耗的能量
     *
     * @param stack 装甲物品
     * @return 每点伤害消耗的能量，非电力装甲返回 0
     */
    long getEnergyPerDamage(ItemStack stack);

    /**
     * 获取电力装甲在指定装备槽位的伤害吸收比例
     *
     * @param stack 装甲物品
     * @param slot 装备槽位
     * @return 伤害吸收比例 (0.0 ~ 1.0+)，非电力装甲返回 0
     */
    float getDamageAbsorptionRatio(ItemStack stack, EquipmentSlot slot);

    // ========== 能量设置 API (主要用于创造模式标签页) ==========

    /**
     * 设置电池的能量值（不检查上限）
     *
     * @param stack 电池物品
     * @param energy 能量值
     */
    void setBatteryEnergy(ItemStack stack, long energy);

    /**
     * 设置电动工具的能量值（不检查上限）
     *
     * @param stack 工具物品
     * @param energy 能量值
     */
    void setElectricToolEnergy(ItemStack stack, long energy);
}