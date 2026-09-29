// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.api.world.IWindAPI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * {@link IWindAPI} 的默认实现，委托给按维度持久化的 {@link WindSim}。
 * 客户端世界一律返回 0，避免在客户端创建 SavedData。
 */
public final class WindAPIImpl implements IWindAPI {

    @Override
    public double getWindAt(Level level, double height) {
        return level instanceof ServerLevel server ? WindSim.get(server).getWindAt(height) : 0.0;
    }

    @Override
    public int getBaseWindStrength(Level level) {
        return level instanceof ServerLevel server ? WindSim.get(server).getWindStrength() : 0;
    }

    @Override
    public int getWindDirection(Level level) {
        return level instanceof ServerLevel server ? WindSim.get(server).getWindDirection() : 0;
    }
}
