// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.api.world.IWindAPI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Default implementation of {@link IWindAPI}. It delegates to {@link WindSim}, which is saved for each dimension.
 * On a client level, it always returns 0. This prevents the creation of SavedData on the client.
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
