// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

/**
 * 0.1.10.11: a debug source whose deliveries never cause overload effects in the independent
 * energy network: no melted cable, no stripped insulation, no shock and no exploding receiver,
 * whatever its packet size. The deliveries are settled, metered and limited by the room and the rated
 * input of the receivers as usual.
 *
 * <p>Meant for creative / test blocks only. The engine reads it on the server thread.
 */
public interface IOverloadFreeEnergySource {
    /** True while the deliveries of this source ignore overload effects. */
    default boolean ignoresOverload() {
        return true;
    }
}
