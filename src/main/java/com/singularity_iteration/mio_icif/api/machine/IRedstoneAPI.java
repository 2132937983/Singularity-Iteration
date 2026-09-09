package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 红石控制 API
 *
 * <p>提供机器红石信号控制功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IRedstoneAPI {

    // ========== 枚举版本（推荐） ==========

    /**
     * 获取机器的红石模式（枚举版本）
     * @return 红石模式，如果方块不是机器则返回 NONE
     */
    @NotNull
    IMachineAPI.RedstoneMode getRedstoneModeEnum(Level world, BlockPos pos);

    /**
     * 设置机器的红石模式（枚举版本）
     */
    void setRedstoneModeEnum(Level world, BlockPos pos, IMachineAPI.RedstoneMode mode);

    /**
     * 检查红石信号是否允许机器工作
     */
    boolean isRedstoneAllowed(Level world, BlockPos pos);

    // ========== int 版本（向后兼容） ==========

    /**
     * 获取储能方块的红石模式
     *
     * @return 红石模式 (0-7)，非储能方块返回 -1
     */
    int getRedstoneMode(Level world, BlockPos pos);

    /**
     * 设置储能方块的红石模式
     *
     * @param mode 红石模式 (0-7)
     * @return 是否成功设置
     */
    boolean setRedstoneMode(Level world, BlockPos pos, int mode);

    /**
     * 获取红石模式名称
     *
     * @param mode 红石模式 (0-7)
     * @return 模式名称
     */
    String getRedstoneModeName(int mode);

    /**
     * 检查储能方块是否正在输出红石信号
     */
    boolean isEmittingRedstone(Level world, BlockPos pos);

    /**
     * 获取储能方块的红石信号强度
     *
     * @return 信号强度 (0-15)
     */
    int getRedstoneSignalStrength(Level world, BlockPos pos);

    /**
     * 检查储能方块是否接收到红石输入信号
     */
    boolean hasRedstoneInput(Level world, BlockPos pos);

    /**
     * 检查储能方块是否允许能量输出
     */
    boolean isEnergyOutputEnabled(Level world, BlockPos pos);
}