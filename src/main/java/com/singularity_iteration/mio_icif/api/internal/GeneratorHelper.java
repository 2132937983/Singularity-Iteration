package com.singularity_iteration.mio_icif.api.internal;

import com.singularity_iteration.mio_icif.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 发电机 forceStart/forceStop 的共享辅助类
 * 包私有，仅供 API 内部使用
 */
public final class GeneratorHelper {

    private GeneratorHelper() {}

    /**
     * 强制启动发电机
     */
    public static boolean forceStartGenerator(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IBurnControl gen) {
            int maxBurn = gen.getDefaultBurnTime();
            gen.setBurnTime(maxBurn > 0 ? maxBurn : 200);
            return true;
        }

        return false;
    }

    /**
     * 强制停止发电机
     */
    public static boolean forceStopGenerator(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IBurnControl gen) {
            gen.setBurnTime(0);
            return true;
        }

        return false;
    }
}