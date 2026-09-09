package com.singularity_iteration.mio_icif.api.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 充电座方块接口
 * 所有充电座方块都应实现此接口，以便客户端粒子系统自动识别并渲染充电粒子特效
 *
 * <p>附属开发者只需让充电座方块实现此接口，即可自动获得蓝色充电粒子特效支持，
 * 无需修改任何客户端代码。
 */
public interface IChargepadBlock {

    /**
     * 获取充电座当前的激活（LIT）状态
     *
     * @param state 方块状态
     * @return true 如果充电座正在充电（激活状态）
     */
    boolean isCharging(BlockState state);
}