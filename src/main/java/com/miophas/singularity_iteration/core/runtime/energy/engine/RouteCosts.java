// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/** Read-only caller-owned route index; conductor IDs belong to its snapshot. */
public interface RouteCosts {
    /** Invalid IDs are rejected; disconnected valid contacts return false. */
    boolean reaches(int receiverContact);

    /** Rejects unreachable contacts rather than assigning them a numeric cost. */
    long lossMilliTo(int receiverContact);

    default long wholeLossTo(int receiverContact) {
        return lossMilliTo(receiverContact) / 1000;
    }
}
