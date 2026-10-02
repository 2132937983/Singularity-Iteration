package com.miophas.singularity_iteration.core.api.menu;

import net.minecraft.world.inventory.ContainerData;

/**
 * Marker for container data whose values are already 16-bit words. SI menus send
 * ordinary container data as two 16-bit halves per value (vanilla truncates to a
 * signed short); data implementing this marker keeps vanilla's one-slot-per-value
 * wire layout.
 */
public interface WordContainerData extends ContainerData { }
