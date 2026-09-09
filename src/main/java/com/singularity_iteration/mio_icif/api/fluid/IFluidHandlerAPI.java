package com.singularity_iteration.mio_icif.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;

/**
 * 流体处理 API
 *
 * <p>提供查询和管理机器流体存储的接口。
 */

public interface IFluidHandlerAPI {

    /**
     * 检查指定位置是否有流体存储能力
     */
    boolean hasFluidHandler(Level world, BlockPos pos);

    /**
     * 获取流体存储信息
     */
    IFluidTankInfo getFluidTankInfo(Level world, BlockPos pos, int tank);

    /**
     * 获取所有流体存储槽信息
     */
    Collection<IFluidTankInfo> getAllFluidTanks(Level world, BlockPos pos);

    /**
     * 获取流体存储槽数量
     */
    int getTankCount(Level world, BlockPos pos);

    /**
     * 向指定槽位填充流体
     * 
     * <p>注意：此方法会先检查指定 tank 是否可以接受该流体（通过 isFluidValid），
     * 但实际填充时 NeoForge 的 IFluidHandler.fill() 会自动路由流体到第一个可接受的槽位。
     * 因此 tank 参数仅用于验证，无法强制填充到特定槽位。
     * 
     * <p>如果需要精确控制填充目标槽位，建议直接使用 NeoForge 的 IFluidHandler capability。
     * 
     * @param world 世界
     * @param pos 方块位置
     * @param tank 目标槽位索引（仅用于验证流体兼容性）
     * @param fluid 要填充的流体
     * @param simulate 是否仅模拟
     * @return 实际填充的流体量
     */
    int fillFluid(Level world, BlockPos pos, int tank, FluidStack fluid, boolean simulate);

    /**
     * 从指定槽位抽取流体
     */
    FluidStack drainFluid(Level world, BlockPos pos, int tank, int maxDrain, boolean simulate);

    /**
     * 获取流体存储接口
     */
    public interface IFluidTankInfo {
        /**
         * 获取槽位索引
         */
        int getTankIndex();

        /**
         * 获取当前流体
         */
        FluidStack getFluid();

        /**
         * 获取当前流体数量
         */
        int getAmount();

        /**
         * 获取容量
         */
        int getCapacity();

        /**
         * 获取填充百分比
         */
        default double getFillPercent() {
            if (getCapacity() <= 0) return 0.0;
            return (double) getAmount() / getCapacity();
        }

        /**
         * 是否为空
         */
        default boolean isEmpty() {
            return getFluid().isEmpty() || getAmount() <= 0;
        }

        /**
         * 是否已满
         */
        default boolean isFull() {
            return getAmount() >= getCapacity();
        }
    }
}