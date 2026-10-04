// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity that belongs to an energy store larger than itself - a multiblock storage core
 * and its I/O ports (GESU). Network tools (energy terminal, scanners) list every member as
 * storage, treat it as a voltage buffer, and read the stored / capacity figures once per owner.
 */
public interface IStorageMember {
    /** The block entity holding the real buffer: the core itself, or a port's formed core (null while unformed). */
    @Nullable
    BlockEntity storageOwner();
}
