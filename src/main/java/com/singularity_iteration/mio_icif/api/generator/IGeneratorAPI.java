package com.singularity_iteration.mio_icif.api.generator;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;

/**
 * 发电机 API
 * 
 * @deprecated 此 API 将在未来版本中被整合到 MachineAPI 中。请使用 IMachineAPI 和 IGeneratorBuilder 替代。
 * 
 * <p>提供注册和管理自定义发电机的接口，包括：
 * <ul>
 *   <li>注册自定义发电机类型</li>
 *   <li>查询发电机信息</li>
 *   <li>管理发电机状态</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 获取 API
 * IGeneratorAPI generatorAPI = MioIcifAPI.instance().getGeneratorAPI();
 * 
 * // 注册自定义发电机
 * generatorAPI.registerGeneratorType("advanced_solar", AdvancedSolarGeneratorBlockEntity::new);
 * 
 * // 查询位置的发电机信息
 * GeneratorInfo info = generatorAPI.getGeneratorInfo(world, pos);
 * if (info != null) {
 *     long rate = info.getGenerationRate();
 *     boolean active = info.isActive();
 * }
 * }</pre>
 */
@Deprecated
public interface IGeneratorAPI {

    /**
     * 注册自定义发电机类型
     * 
     * @param id 发电机类型 ID（唯一标识符）
     * @param factory 发电机方块实体工厂
     * @deprecated 请使用 IGeneratorBuilder 注册发电机
     */
    @Deprecated
    <T extends BlockEntity> void registerGeneratorType(String id, BlockEntityType<T> type);

    /**
     * 检查指定位置是否有发电机
     * 
     * @param world 世界
     * @param pos 方块位置
     * @return true 如果位置有发电机
     * @deprecated 请使用 IMachineAPI.hasMachine() 替代
     */
    @Deprecated
    boolean isGenerator(Level world, BlockPos pos);

    /**
     * 获取发电机信息
     * 
     * @param world 世界
     * @param pos 方块位置
     * @return 发电机信息，如果没有则返回 null
     * @deprecated 请使用 IMachineAPI.getMachineInfo() 替代
     */
    @Deprecated
    IGeneratorInfo getGeneratorInfo(Level world, BlockPos pos);

    /**
     * 获取指定世界中的所有发电机位置
     * 
     * @param world 世界
     * @return 发电机位置集合
     * @deprecated 请使用 IMachineAPI.getAllMachines() 替代
     */
    @Deprecated
    Collection<BlockPos> getAllGenerators(Level world);

    /**
     * 获取指定类型的所有发电机位置
     * 
     * @param world 世界
     * @param typeId 发电机类型 ID
     * @return 发电机位置集合
     * @deprecated 请使用 IMachineAPI.getMachinesByType() 替代
     */
    @Deprecated
    Collection<BlockPos> getGeneratorsByType(Level world, String typeId);

    /**
     * 强制启动发电机（忽略正常启动条件）
     * 
     * @param world 世界
     * @param pos 方块位置
     * @return true 如果成功启动
     * @deprecated 请使用 IMachineAPI.forceStartMachine() 替代
     */
    @Deprecated
    boolean forceStart(Level world, BlockPos pos);

    /**
     * 强制停止发电机
     * 
     * @param world 世界
     * @param pos 方块位置
     * @return true 如果成功停止
     * @deprecated 请使用 IMachineAPI.forceStopMachine() 替代
     */
    @Deprecated
    boolean forceStop(Level world, BlockPos pos);

    /**
     * 获取发电机的电缆等级
     * 
     * @param world 世界
     * @param pos 方块位置
     * @return 电缆等级，如果没有则返回 null
     * @deprecated 请使用 IMachineAPI.getEffectiveCableTier() 替代
     */
    @Deprecated
    ICableTier getCableTier(Level world, BlockPos pos);
}