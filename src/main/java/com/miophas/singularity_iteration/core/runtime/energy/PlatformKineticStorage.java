// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.kinetic.KineticStorage;

/** Bind the reviewed numeric implementation to SI's public platform capability. Server-thread owned. */
public final class PlatformKineticStorage extends KineticStorage implements IMioIcifCapabilities.IKineticStorage {
    public PlatformKineticStorage(long capacity, long receive, long extract, int maxRPM, float friction) {
        super(capacity, receive, extract, maxRPM, friction);
    }
    @Override public boolean isOverspeed() { return getRPM() >= 8000; }
}
