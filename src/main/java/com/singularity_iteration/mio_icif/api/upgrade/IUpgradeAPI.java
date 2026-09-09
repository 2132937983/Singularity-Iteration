package com.singularity_iteration.mio_icif.api.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * 升级插件 API
 *
 * <p>提供管理机器升级插件的接口，包括：
 * <ul>
 *   <li>查询升级类型和效果</li>
 *   <li>安装/移除升级</li>
 *   <li>计算升级后的属性</li>
 * </ul>
 */
public interface IUpgradeAPI {

    /**
     * 升级类型枚举
     */
    enum UpgradeType {
        OVERCLOCKER,        // 超频升级 - 加速但增加能耗
        TRANSFORMER,        // 变压器升级 - 提高电压等级
        ENERGY_STORAGE,     // 能量存储升级 - 增加能量容量
        EJECTOR,            // 弹出升级 - 自动输出物品
        IMPORT,             // 导入升级 - 自动输入物品
        FLUID_EJECTOR,      // 流体弹出升级 - 自动输出流体
        FLUID_IMPORT,       // 流体导入升级 - 自动输入流体
        REDSTONE_SIGNAL,    // 红石信号升级 - 红石控制
        ADVANCED_CIRCUIT,   // 高级电路升级 - 增加处理槽位
        MOLECULAR_TRANSFORM, // 分子变换升级 - 改变配方
        QUANTUM_CORE,       // 量子核心升级 - 大幅提升性能
        CUSTOM              // 自定义升级
    }

    /**
     * 升级效果接口
     */
    interface IUpgradeEffect {
        /**
         * 获取速度倍率加成
         */
        double getSpeedMultiplier();

        /**
         * 获取能量消耗倍率
         */
        double getEnergyMultiplier();

        /**
         * 获取额外能量容量
         */
        long getExtraCapacity();

        /**
         * 获取额外输出槽位
         */
        int getExtraOutputSlots();

        /**
         * 是否支持自动输出
         */
        boolean hasAutoEject();

        /**
         * 是否支持自动输入
         */
        boolean hasAutoImport();

        /**
         * 是否支持红石控制
         */
        boolean hasRedstoneControl();

        /**
         * 获取电压等级提升
         */
        int getTierUpgrade();
    }

    /**
     * 检查机器是否支持升级
     */
    boolean supportsUpgrades(Level world, BlockPos pos);

    /**
     * 获取机器最大升级槽位数
     */
    int getMaxUpgradeSlots(Level world, BlockPos pos);

    /**
     * 获取已安装的升级
     */
    Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos);

    /**
     * 获取升级类型
     */
    UpgradeType getUpgradeType(ItemStack upgrade);

    /**
     * 获取升级效果
     */
    IUpgradeEffect getUpgradeEffect(ItemStack upgrade);

    /**
     * 计算多个升级的组合效果
     */
    IUpgradeEffect calculateCombinedEffect(Collection<ItemStack> upgrades);

    /**
     * 安装升级
     */
    boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * 移除升级
     */
    ItemStack removeUpgrade(Level world, BlockPos pos, int slot);

    /**
     * 检查是否可以安装此升级
     */
    boolean canInstallUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * 获取升级限制数量
     */
    int getUpgradeLimit(UpgradeType type);
}