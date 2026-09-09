package com.singularity_iteration.mio_icif.api.energy;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * 能源网络 API
 *
 * <p>提供与 mio_icif EU 电网交互的接口，包括：
 * <ul>
 *   <li>查询电网节点的统计信息</li>
 *   <li>注册/注销自定义能源方块</li>
 *   <li>查询电网连接状态</li>
 * </ul>
 *
 * <p>注册方式：
 * <ul>
 *   <li>事件驱动：通过 {@link com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent} 注册</li>
 *   <li>直接注册：通过 {@link #addTile(Level, EnergyTileEvent.IEnergyTileMarker)} 和 {@link #removeTile(Level, EnergyTileEvent.IEnergyTileMarker)} 注册</li>
 * </ul>
 */
public interface IEnergyNetAPI {

    /**
     * 将能源方块添加到电网。
     * <p>
     * 对应 IC2 1.12.2 的 {@code IEnergyNet.addTile()}。
     *
     * @param world 世界
     * @param tile  能源方块标记
     */
    void addTile(Level world, EnergyTileEvent.IEnergyTileMarker tile);

    /**
     * 从电网中移除能源方块。
     * <p>
     * 对应 IC2 1.12.2 的 {@code IEnergyNet.removeTile()}。
     *
     * @param world 世界
     * @param tile  能源方块标记
     */
    void removeTile(Level world, EnergyTileEvent.IEnergyTileMarker tile);

    /**
     * 获取指定位置的电网节点统计信息
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 节点统计信息，如果位置没有电网节点则返回 null
     */
    @org.jetbrains.annotations.Nullable
    NodeStats getNodeStats(Level world, BlockPos pos);

    /**
     * 检查指定位置是否有电网节点
     *
     * @param world 世界
     * @param pos   方块位置
     * @return true 如果位置有电网节点
     */
    boolean hasEnergyTile(Level world, BlockPos pos);

    /**
     * 获取指定位置的能源方块
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 能源方块接口，如果没有则返回 null
     */
    IEnergyTileAccess getEnergyTile(Level world, BlockPos pos);

    /**
     * 获取指定位置的子能源方块（用于多方块结构）
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 子能源方块接口，如果没有则返回 null
     */
    IEnergyTileAccess getSubTile(Level world, BlockPos pos);

    /**
     * 获取能源方块所在的世界。
     * <p>
     * 对应 IC2 1.12.2 的 {@code IEnergyNet.getWorld()}。
     *
     * @param tile 能源方块标记
     * @return 所在世界，如果未注册则返回 null
     */
    Level getWorld(EnergyTileEvent.IEnergyTileMarker tile);

    /**
     * 获取能源方块的位置。
     * <p>
     * 对应 IC2 1.12.2 的 {@code IEnergyNet.getPos()}。
     *
     * @param tile 能源方块标记
     * @return 方块位置，如果未注册则返回 null
     */
    BlockPos getPos(EnergyTileEvent.IEnergyTileMarker tile);

    /**
     * IC2 风格 tier 转换为功率
     *
     * @param tier 等级 (0-13+)
     * @return 功率值 (EU/t)
     */
    double getPowerFromTier(int tier);

    /**
     * 将功率转换为 IC2 风格 tier
     *
     * @param power 功率值 (EU/t)
     * @return 等级
     */
    int getTierFromPower(double power);

    /**
     * CableTier 转换为源等级
     *
     * @param tier 电缆等级
     * @return 源等级
     */
    int cableTierToSourceTier(ICableTier tier);

    /**
     * 获取指定位置的所有连接方向
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 连接方向集合
     */
    Collection<Direction> getConnections(Level world, BlockPos pos);

    // ========== 能源方块类型查询 API ==========

    /**
     * 检查能源方块是否是电源（发电/输出能量）
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 是否是电源
     */
    boolean isEnergySource(Level world, BlockPos pos);

    /**
     * 检查能源方块是否是用电器（消耗能量）
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 是否是用电器
     */
    boolean isEnergySink(Level world, BlockPos pos);

    /**
     * 检查能源方块是否是导体（电缆）
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 是否是导体
     */
    boolean isEnergyConductor(Level world, BlockPos pos);

    /**
     * 获取电源的输出等级
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 输出等级，非电源返回 0
     */
    int getSourceTier(Level world, BlockPos pos);

    /**
     * 获取用电器的输入等级
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 输入等级，非用电器返回 0
     */
    int getSinkTier(Level world, BlockPos pos);

    /**
     * 获取电源当前提供的能量
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 提供的能量 (EU)
     */
    long getOfferedEnergy(Level world, BlockPos pos);

    /**
     * 获取用电器需求的能量
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 需求的能量 (EU)
     */
    long getDemandedEnergy(Level world, BlockPos pos);

    /**
     * 检查用电器是否接受来自指定方向的能量
     *
     * @param world     世界
     * @param pos       方块位置
     * @param direction 能量来源方向
     * @return 是否接受
     */
    boolean acceptsEnergyFrom(Level world, BlockPos pos, Direction direction);

    /**
     * 检查用电器是否向指定方向输出能量
     *
     * @param world     世界
     * @param pos       方块位置
     * @param direction 能量输出方向
     * @return 是否输出
     */
    boolean emitsEnergyTo(Level world, BlockPos pos, Direction direction);

    // ========== 电网统计 API ==========

    /**
     * 获取指定世界中所有电网节点的位置
     *
     * @param world 世界
     * @return 所有电网节点位置
     */
    Collection<BlockPos> getAllEnergyTiles(Level world);

    /**
     * 获取与指定节点属于同一电网的所有节点
     *
     * @param world 世界
     * @param pos   起始节点位置
     * @return 同一电网的所有节点位置
     */
    Collection<BlockPos> getConnectedNodes(Level world, BlockPos pos);

    /**
     * 获取电网的总能量存储
     *
     * @param world 世界
     * @param pos   任意节点位置
     * @return 电网总存储能量 (EU)
     */
    long getGridTotalEnergy(Level world, BlockPos pos);

    /**
     * 获取电网的总能量容量
     *
     * @param world 世界
     * @param pos   任意节点位置
     * @return 电网总容量 (EU)
     */
    long getGridTotalCapacity(Level world, BlockPos pos);

    /**
     * 根据名称获取电缆等级。
     * <p>
     * 标准等级名称：{@code "lv"}, {@code "mv"}, {@code "hv"}, {@code "ev"}, {@code "iv"}, {@code "luv"},
     * {@code "zpmv"}, {@code "uv"}, {@code "uhv"}, {@code "uev"}, {@code "uiv"}, {@code "uxv"},
     * {@code "opv"}, {@code "max"}。
     *
     * @param name 电缆等级名称（小写）
     * @return 电缆等级，如果名称不存在则返回 LV
     */
    ICableTier getCableTier(String name);

    /**
     * 获取所有已注册的电缆等级，按电压等级升序排列。
     *
     * @return 不可变的电缆等级列表
     */
    java.util.List<ICableTier> getAllCableTiers();

    /**
     * 获取默认（LV）电缆等级。
     *
     * @return LV 电缆等级
     */
    ICableTier getDefaultCableTier();
}