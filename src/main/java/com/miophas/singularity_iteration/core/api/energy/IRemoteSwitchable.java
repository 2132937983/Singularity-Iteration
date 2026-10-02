// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

/**
 * An energy endpoint that can be switched off remotely (e.g. from an energy
 * management terminal). A disabled endpoint keeps its stored energy but is cut from
 * the grid: it neither receives nor emits network energy, and processing machines
 * stop working until switched back on.
 */
public interface IRemoteSwitchable {
    boolean isRemotelyDisabled();

    void setRemotelyDisabled(boolean disabled);
}
