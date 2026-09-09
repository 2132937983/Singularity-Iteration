package com.singularity_iteration.mio_icif.api.reactor;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import com.singularity_iteration.mio_icif.api.reactor.IReactorAPI.ReactorMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * 核反应堆主控方块应实现的接口。
 *
 * <p>API 层（{@link ReactorAPIImpl}、{@code MachineAPIImpl} 等）通过此接口访问反应堆，
 * 而不直接依赖具体实现类 {@code mio_icif_nuclear_reactor_generator}，
 * 从而避免内部重构（重命名/移动/改字段）时破坏 API 层编译。
 *
 * <p>此接口扩展 {@link IReactor}，将反应堆核心模拟接口与 API 控制接口统一。
 */
public interface IReactorController extends IReactor {

    boolean isRunning();

    @Override
    default long getHeat() {
        return getCurrentHeat();
    }

    /**
     * 设置反应堆热量。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 直接设置热量。此方法抛出
     * {@link UnsupportedOperationException}。
     * 如需修改热量，请使用反应堆内部机制（燃料棒发热、散热片散热）。
     *
     * @param heat 新的热量值
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see IReactorAPI#getReactorCurrentHeat(Level, BlockPos) 查询当前热量
     */
    @Override
    default void setHeat(long heat) {
        throw new UnsupportedOperationException(
            "setHeat is not supported via API. Use reactor internal mechanisms instead.");
    }

    /**
     * 向反应堆添加热量。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 直接添加热量。此方法抛出
     * {@link UnsupportedOperationException}。
     * 如需修改热量，请使用反应堆内部机制（燃料棒发热、散热片散热）。
     *
     * @param heat 要添加的热量值
     * @return 实际添加的热量值
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see IReactorAPI#getReactorCurrentHeat(Level, BlockPos) 查询当前热量
     */
    @Override
    default long addHeat(long heat) {
        throw new UnsupportedOperationException(
            "addHeat is not supported via API. Use reactor internal mechanisms instead.");
    }

    /**
     * 设置反应堆最大热量容量。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 设置最大热量。此方法抛出
     * {@link UnsupportedOperationException}。
     * 最大热量由反应堆结构和连接的核反应仓数量决定。
     *
     * @param maxHeat 新的最大热量容量
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see IReactorAPI#getReactorMaxHeat(Level, BlockPos) 查询最大热量
     */
    @Override
    default void setMaxHeat(long maxHeat) {
        throw new UnsupportedOperationException(
            "setMaxHeat is not supported via API. Max heat is determined by reactor structure.");
    }

    /**
     * 添加散热热量。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 直接散热。此方法抛出
     * {@link UnsupportedOperationException}。
     * 散热由散热片元件自动处理。
     *
     * @param heat 散热量
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see IReactorAPI#getReactorCurrentTemperature(Level, BlockPos) 查询温度（间接反映散热效果）
     */
    @Override
    default void addEmitHeat(long heat) {
        throw new UnsupportedOperationException(
            "addEmitHeat is not supported via API. Heat dissipation is handled by heat vents.");
    }

    default float getHeatEffectModifier() {
        return 1.0f;
    }

    /**
     * <b>受限制操作：</b>设置热量效应修正系数。
     *
     * <p>当前不支持通过 API 直接设置修正系数。此方法抛出
     * {@link UnsupportedOperationException}。
     * 修正系数由反应堆温度自动计算，无需手动设置。
     *
     * @param modifier 修正系数
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see #getHeatEffectModifier() 获取当前修正系数
     */
    default void setHeatEffectModifier(float modifier) {
        throw new UnsupportedOperationException(
            "setHeatEffectModifier is not supported via API. Modifier is computed from temperature.");
    }

    default long getReactorEnergyOutput() {
        return getCurrentEnergyGeneration();
    }

    /**
     * 获取指定位置的物品（网格坐标系）。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 按网格坐标访问物品槽。此方法抛出
     * {@link UnsupportedOperationException}。
     * 如需访问物品，请使用 {@link #getItemHandler()} 获取物品处理器。
     *
     * @param x X 坐标（0-based 列索引）
     * @param y Y 坐标（0-based 行索引）
     * @return 该位置的物品
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see #getItemHandler() 通过物品处理器访问所有槽位
     */
    default ItemStack getItemAt(int x, int y) {
        throw new UnsupportedOperationException(
            "getItemAt is not supported via API. Use getItemHandler() instead.");
    }

    /**
     * 设置指定位置的物品（网格坐标系）。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 按网格坐标设置物品槽。此方法抛出
     * {@link UnsupportedOperationException}。
     * 如需修改物品，请使用 {@link #getItemHandler()} 获取物品处理器。
     *
     * @param x X 坐标（0-based 列索引）
     * @param y Y 坐标（0-based 行索引）
     * @param stack 要放置的物品
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see #getItemHandler() 通过物品处理器访问所有槽位
     */
    default void setItemAt(int x, int y, ItemStack stack) {
        throw new UnsupportedOperationException(
            "setItemAt is not supported via API. Use getItemHandler() instead.");
    }

    /**
     * 引发反应堆爆炸。
     *
     * <p><b>受限制操作：</b>当前不支持通过 API 触发爆炸。此方法抛出
     * {@link UnsupportedOperationException}。
     * 爆炸仅在热量超过最大容量时由内部逻辑自动触发（熔毁）。
     *
     * @throws UnsupportedOperationException 此实现不支持此操作
     * @see IReactorAPI#getReactorCurrentTemperature(Level, BlockPos) 监控温度避免熔毁
     */
    default void explode() {
        throw new UnsupportedOperationException(
            "explode is not supported via API. Explosion is triggered automatically on meltdown.");
    }

    default int getTickRate() {
        return 20;
    }

    default boolean produceEnergy() {
        return isRunning();
    }

    default boolean isFluidCooled() {
        return getApiReactorMode() == ReactorMode.FLUID;
    }

    long getCurrentHeat();

    long getMaxHeat();

    /**
     * 获取反应堆当前温度（摄氏度）。
     *
     * <p>返回 {@code double} 类型以保留温度小数精度（如 20.5°C）。
     *
     * @return 当前温度（摄氏度）
     */
    double getCurrentTemperature();

    long getCurrentEnergyGeneration();

    int getAvailableColumns();

    /**
     * 获取反应堆子方块列表（包括主控方块和连接的核反应仓）。
     *
     * <p>返回的列表包含所有参与能源网络的子方块标记。
     * 电网将分别查询每个子方块的能源属性。
     *
     * @return 子方块标记列表
     * @see EnergyTileEvent.IEnergyTileMarker
     */
    List<? extends EnergyTileEvent.IEnergyTileMarker> getSubTiles();

    boolean isValidFluidReactorStructure();

    IItemHandler getItemHandler();

    IFluidHandler getFluidHandler();

    ReactorMode getApiReactorMode();
}
