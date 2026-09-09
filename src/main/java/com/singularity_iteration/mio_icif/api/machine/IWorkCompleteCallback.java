package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 工作完成回调接口
 *
 * <p>当机器完成一次加工循环时调用此回调。
 * 用于在配方处理完成后执行自定义逻辑（如统计、通知等）。
 *
 * <p>使用示例：
 * <pre>{@code
 * IWorkCompleteCallback callback = (level, pos, output) -> {
 *     logger.info("Machine at " + pos + " completed work, output: " + output);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface IWorkCompleteCallback {

    /**
     * 工作完成时调用
     *
     * @param level 世界
     * @param pos 机器位置
     * @param output 输出物品（可能为空）
     */
    void onWorkComplete(Level level, BlockPos pos, ItemStack output);
}