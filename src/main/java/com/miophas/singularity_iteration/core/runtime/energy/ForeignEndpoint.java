// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell;
import com.miophas.singularity_iteration.core.runtime.energy.engine.StorageHysteresis;
import com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 0.1.11.1: a block of another mod next to the SI grid, as one port of the native fair round. Until 0.1.11.0 two
 * passes after the native pass moved FE pair by pair (export, then import) with the energy that the native pass left.
 *
 * <p>The engine settles a foreign block like an SI endpoint. Its balance is a staging cell of the engine:
 * <ul>
 *   <li>As a receiver, a commit credits the cell; the engine then hands the whole FE of the cell to the block
 *       ({@code receiveEnergy}). What the block refuses waits in the cell for the next tick (its room counts it).</li>
 *   <li>As a source, the engine takes FE from the block ({@code extractEnergy}) only for a planned, routable debit, into
 *       the cell, and settles the window again with the cell (as for a source that makes its energy on demand).</li>
 *   <li>Less than one FE stays in the cell between ticks (1 FE = 1/4 EU), so no energy is rounded away.</li>
 * </ul>
 * One snapshot per settlement frame: the faces that receive and extract, the FE the block would take and give (one
 * simulated call per face), its state of charge, and the packet size of the frame (a frozen allowance needs a fixed
 * packet policy). A block that receives and extracts is a battery and gets a role from {@link StorageHysteresis}.
 *
 * <p>Server thread only. The engine owns every field.
 */
final class ForeignEndpoint {
    /** The foreign block. */
    final BlockPos at;
    /** The block entity of the block when the endpoint was made. A new block entity makes a new endpoint. */
    final BlockEntity tile;
    /** The emitter list of the port: the block itself. */
    final List<BlockPos> emitters;
    /** The staging balance in EU: credited and not handed over yet, or taken and not sent yet. */
    final NetworkCell cell = new NetworkCell(EnergyAmount.ZERO);
    /** The kept adapter of each face of the block that touches the SI grid (by {@code Direction} ordinal), or null. */
    final FECompatTile[] adapters = new FECompatTile[6];
    /** The frame of the snapshot below. */
    long frame = Long.MIN_VALUE;
    /** The block state of the frame. */
    BlockState state;
    /** Faces whose adapter can receive, and faces whose adapter can extract, in this frame. */
    int receiveFaces, extractFaces;
    /** The faces of the port in this frame: the role of a battery closes one direction. */
    int inputs, outputs;
    /** FE the block would take and give in this frame (simulated before any transfer). */
    int acceptFe, extractFe;
    /** FE handed to the block and taken from it in this frame. */
    int deliveredFe, extractedFe;
    /** The packet size of the frame in EU (fixed while the frame lasts). */
    long packet = 1;
    /** The role of a battery, or {@code null} for a block that only receives or only extracts. */
    StorageHysteresis.Role role;
    /** True after the engine dropped this endpoint. */
    boolean retired;

    ForeignEndpoint(BlockPos at, BlockEntity tile) {
        this.at = at.immutable();
        this.tile = tile;
        this.emitters = List.of(this.at);
    }

    /** True for a block that receives and extracts (a battery). */
    boolean battery() { return receiveFaces != 0 && extractFaces != 0; }

    /** The staged balance. */
    EnergyAmount held() { return cell.quote().exactAmount(); }

    /**
     * The capacity of the port in this frame: the FE the block still takes in this frame, in whole EU. The room of the
     * port is this capacity minus the staged balance.
     */
    long capacity() {
        if (inputs == 0 || acceptFe <= deliveredFe) return 0;
        return (acceptFe - (long) deliveredFe) / FeLedger.FE_PER_EU;
    }

    /** The EU this port can give in this frame: the staged balance and what the block still gives. */
    EnergyAmount potential() {
        var held = held();
        if (outputs == 0 || extractFe <= extractedFe) return held;
        return held.add(FeLedger.eu(extractFe - extractedFe));
    }

    /** True when the staged balance waits for the block (it receives and is not a discharging battery). */
    boolean hands() {
        return receiveFaces != 0 && role != StorageHysteresis.Role.DISCHARGE;
    }
}
