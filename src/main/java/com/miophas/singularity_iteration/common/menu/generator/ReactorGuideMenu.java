// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.reactor.ReactorLayoutGuide;

/**
 * 0.1.11.76: a reactor menu with the config card slot and the layout guide ({@link ReactorLayoutGuide}). Menu slots
 * 0..53 are the reactor slots.
 */
public interface ReactorGuideMenu {
    /** The synced guide and the result of the last card button. */
    ReactorLayoutGuide.Data guideData();

    /** The menu index of the card slot. */
    int cardSlotIndex();
}
