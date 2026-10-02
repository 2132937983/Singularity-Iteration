// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

/** What the modern solar GUI reads from a solar generator menu. */
public interface ModernSolarView {
    int getEnergy();
    int getMaxEnergy();
    boolean isGenerating();
    int getLiveOutput();
}
