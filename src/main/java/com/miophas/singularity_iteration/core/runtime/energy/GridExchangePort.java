// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import net.minecraft.core.Direction;

/**
 * 0.1.11.1: an SI endpoint that exchanges energy with another mod's network (the grid-tie interface). One face
 * ({@link #exchangeFace()}) faces the other network; the other faces join the SI grid. The engine reads the SI faces and
 * the dispatch rules of the endpoint from this interface instead of from the generator and storage flags.
 *
 * <ul>
 *   <li>Faces: bit {@code 1 << direction.ordinal()} per face. The exchange face is never an SI face.</li>
 *   <li>As a receiver the endpoint is in the storage class (it takes the surplus after the loads). With
 *       {@link #exchangeSurplusOnly()} it takes nothing from storage sources.</li>
 *   <li>As a source it is a generator (rank 0, charges storage), or with {@link #exchangeLoadsOnly()} a storage source
 *       that supplies loads only (no storage charging).</li>
 * </ul>
 * Not a public API: the interface may change with the energy engine.
 */
public interface GridExchangePort {
    /** The face toward the other network. */
    Direction exchangeFace();

    /** SI faces that take energy from the grid. */
    int exchangeInputs();

    /** SI faces that give energy to the grid. */
    int exchangeOutputs();

    /** True: as a source, it supplies loads only and is dispatched with the storage blocks. */
    boolean exchangeLoadsOnly();

    /** True: as a receiver, it takes no energy from storage sources (only the surplus of the generators). */
    boolean exchangeSurplusOnly();
}
