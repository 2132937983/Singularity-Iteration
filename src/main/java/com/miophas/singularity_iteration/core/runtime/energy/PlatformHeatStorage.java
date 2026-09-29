// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.heat.HeatStorage;

/** Bind the reviewed numeric implementation to SI's public platform capability. Server-thread owned. */
public final class PlatformHeatStorage extends HeatStorage implements IMioIcifCapabilities.IHeatStorage {
    public PlatformHeatStorage(long capacity, long receive, long extract) { super(capacity, receive, extract); }
    public PlatformHeatStorage(long capacity, long receive, long extract, int baseTemp, int maxTemp, float loss) {
        super(capacity, receive, extract, 0, baseTemp, maxTemp, loss);
    }
    @Override public boolean isOverheated() { return getTemperature() >= 800; }
    /** Loaded heat is an existing balance; capacity governs future admission, not destruction of saved heat. */
    @Override public void setHeat(long heat) { this.heat = Math.max(0, heat); }
    @Override public void setCapacity(long capacity) { this.capacity = Math.max(0, capacity); }
}
