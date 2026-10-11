// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySource;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyTile;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import com.miophas.singularity_iteration.core.runtime.energy.engine.BoundedContactIndex;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DeferredEntries;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DomainDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.PacketAllowance;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.api.energy.IEnergyPacketSource;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.FractionalDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ReceiverOrder;
import com.miophas.singularity_iteration.core.runtime.energy.engine.RouteCosts;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ReceiverContacts;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry;
import com.miophas.singularity_iteration.core.api.energy.IEnergyConductiveEndpoint;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.ContactOrder;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.SmallBlastField;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell;
import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import com.miophas.singularity_iteration.core.api.energy.tile.IOverloadHandler;
import com.miophas.singularity_iteration.core.api.energy.tile.IExplosionPowerOverride;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyConductor;
import com.miophas.singularity_iteration.core.prefab.blockentity.MachinePresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSources;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The default SI energy engine. It moves EU from sources to sinks through
 * conductors, once per server tick, for each loaded dimension.
 *
 * <p>The engine owns no energy itself. Each machine owns its balance in a
 * {@link CustomEUEnergyStorage} (or a {@link NetworkCell} for a transformer).
 * The engine reads quotes of these balances, computes a transfer, and writes
 * the new balances back in one atomic commit. Compatibility entry points such
 * as {@link EnergyNetGlobal} delegate to this engine.
 *
 * <h2>Terms</h2>
 * <ul>
 *   <li><b>Engine:</b> one instance of this class. There is one engine per
 *       {@link MinecraftServer}. {@link #attach(MinecraftServer)} creates it.</li>
 *   <li><b>Grid:</b> one {@code WorldGrid}. It holds all energy state of one
 *       {@link ServerLevel} (dimension). A grid is not one electrical circuit.
 *       One grid can contain many separate circuits.</li>
 *   <li><b>Conductor:</b> a cable block. The {@link ConductorRegistry} stores
 *       each conductor with its loss (in milli-EU per block, 1/1000 EU) and its
 *       face mask. Connected conductors form a <b>component</b>.</li>
 *   <li><b>Endpoint:</b> a block entity with an owned energy storage, or a
 *       transformer. The grid stores endpoints in {@code WorldGrid.machines}.</li>
 *   <li><b>Port:</b> a {@code Port} record. It is a read-only snapshot of one
 *       endpoint for one settlement: balance quote, capacity, face masks and
 *       packet policy.</li>
 *   <li><b>Source:</b> a port with output faces and a packet policy with a
 *       packet size above 0 and a packet count above 0.</li>
 *   <li><b>Sink:</b> a port with input faces. The code also calls a sink a
 *       "receiver". One port can be a source and a sink (for example, a
 *       storage block).</li>
 *   <li><b>Face mask:</b> a 6-bit {@code int}. Bit {@code i} is set when the
 *       face with {@code Direction.ordinal() == i} is open (down, up, north,
 *       south, west, east). The value 63 means all six faces.</li>
 *   <li><b>Frame:</b> a publication frame number. Topology edits observed
 *       during tick {@code ticks} are published in frame {@code ticks + 1}.</li>
 *   <li><b>Foreign endpoint:</b> a block entity of another mod that exposes
 *       Forge Energy (FE). {@link FECompatTile} adapts it. 4 FE equal 1 EU
 *       ({@link FeLedger}).</li>
 * </ul>
 *
 * <h2>How the grid finds sources, sinks, storage and conductors</h2>
 * <p>The engine does not scan the world. {@link PlatformTopology} watches
 * chunk loads, block changes and chunk unloads, and calls the
 * {@link PlatformTopology.Observer} methods of this class:
 * <ol>
 *   <li>{@link #blockChanged} reserves or cancels a placement slot in
 *       {@code ContactOrder}. The slot keeps the placement order of sinks.</li>
 *   <li>{@link #position} reads the block entity at a position in a FULL chunk.
 *       It builds an {@code Element} (block type, conductor loss, tile, routing
 *       signature, face mask). It does not apply the element. It queues the
 *       element in {@code WorldGrid.entries} for the next frame.</li>
 *   <li>{@link #afterLevel} publishes queued elements with
 *       {@code WorldGrid.apply}. An element with {@code conductorLoss >= 0} is a
 *       conductor. An element with {@code conductorLoss == -1} is an endpoint.
 *       A special cable that does not conduct now has {@code conductorLoss == -2}.</li>
 *   <li>{@link #chunkRemoved} forgets every entry of the unloaded chunk.</li>
 * </ol>
 * <p>The engine classifies an endpoint at each settlement in {@code ports}:
 * <ul>
 *   <li>A generator ({@code isPowerSource()} or {@link DemandEnergySource})
 *       has outputs on all faces and no inputs.</li>
 *   <li>A storage block ({@link AbstractEnergyStorageBlockEntity}) uses its own
 *       per-face input and output rules.</li>
 *   <li>An {@link EnergyComponentHost} uses its component port policy.</li>
 *   <li>Every other endpoint is a sink on all faces.</li>
 *   <li>A transformer accepts on one side and emits on the other five sides, or
 *       the reverse. Its mode (step-up or step-down) selects the direction.</li>
 * </ul>
 * <p>The conductor face mask always limits the endpoint face mask.
 *
 * <h2>Per-tick order of work</h2>
 * <ol>
 *   <li>{@code LevelTickEvent.Pre} ({@link #beforeLevel}): refresh special
 *       cable inputs and transformer modes. Then run the START catch-up
 *       settlement, but only for endpoints that the previous END paused.</li>
 *   <li>The level ticks its block entities. Machines fill or drain their own
 *       storage.</li>
 *   <li>{@code LevelTickEvent.Post} ({@link #afterLevel}, low priority):
 *       publish the topology edits of the frame. If no edit exists, settle the
 *       full grid. If edits exist, pause the endpoints near the edits for this
 *       END, settle all other endpoints, and queue the paused endpoints for the
 *       next START catch-up.</li>
 *   <li>{@code ServerTickEvent.Post} ({@link #tick}): increment {@code ticks}.</li>
 * </ol>
 * <p>One settlement ({@code settle(ServerLevel, WorldGrid)}) has three passes
 * in this order:
 * <ol>
 *   <li>Native EU: SI sources to SI sinks.</li>
 *   <li>Export: SI sources to foreign FE sinks ({@code settleForeign}).</li>
 *   <li>Import: foreign FE sources to SI sinks ({@code importForeign}).</li>
 * </ol>
 * <p>Shock damage that the passes collected is applied after all three passes.
 *
 * <h2>Route plans</h2>
 * <p>A dense grid can have thousands of sources and sinks. A full
 * source-by-sink route table does not fit in one tick. The native pass
 * therefore works on <b>windows</b>. A window is a contiguous slice of at most
 * {@value #MAX_PLAN_SOURCES} sources and at most {@value #MAX_PLAN_RECEIVERS}
 * sinks that the sources can reach.
 * <p>A {@code NativeRoutePlan} computes the cheapest route from each origin
 * (a conductor next to a source output face) to each sink of its window. It
 * does this work in small steps. {@code NativeRoutePlan.advance} returns
 * {@code false} when a tick budget is empty. The grid then keeps the plan in
 * {@code WorldGrid.nativePlan}, and the next tick resumes the plan at
 * {@code originIndex}. This is why a plan survives across ticks: a large grid
 * can finish a plan over several ticks and still transfer energy.
 * <p>The engine keeps a plan only while all of these stay the same:
 * <ul>
 *   <li>The conductor snapshot identity ({@code plan.graph == snapshot}).</li>
 *   <li>The routing generation ({@code WorldGrid.portsGeneration}). The engine
 *       increments it when the routing identity of any source or sink changes
 *       (see {@code RouteEndpoint.matches}).</li>
 * </ul>
 * <p>A plan never stores a balance. Balances are quoted again before each
 * commit.
 * <p><b>Cursors.</b> {@code WorldGrid.nativeCursor} selects the first source of
 * the next window. {@code WorldGrid.receiverCursors} selects the first sink of
 * the next window, per first source. The cursors rotate so that each source
 * and each sink gets a turn. The code normalizes each cursor with
 * {@link Math#floorMod} before use.
 * <p><b>Budgets.</b> Each tick has fixed limits: search vertices
 * ({@code nativeSearch}, {@code exportSearch}, {@code importSearch}), route
 * records and array cells ({@code RouteBuildBudget}), and path visits for world
 * effects. A pass that cannot afford the next step returns and resumes on the
 * next tick. It never runs past a budget.
 *
 * <h2>Packets, tiers and overvoltage</h2>
 * <ul>
 *   <li>Each source has an {@link EnergyPacketPolicy}: packet size (EU per
 *       packet), packet count (packets per tick) and a flag that allows a
 *       partial last packet. The packet size times the packet count limits the
 *       source output in EU/t.</li>
 *   <li>The engine freezes the offer of each source once per settlement in a
 *       {@link PacketAllowance}. Later windows and the FE export pass debit the
 *       same allowance. No pass can renew it in the same settlement. The
 *       allowance returns 0 when the packet policy of the source changes during
 *       the settlement.</li>
 *   <li>A tier maps to a packet size with
 *       {@link EnergyNetGlobal#getPowerFromTier}: 8 &times; 4<sup>tier</sup> EU.
 *       Tier 1 is 32 EU, tier 2 is 128 EU, tier 3 is 512 EU.</li>
 *   <li>The <b>delivery voltage</b> of one delivery is the EU that the source
 *       sent for it, before conductor loss. The tier of the source does not set
 *       the voltage. A partial packet has a lower voltage.</li>
 *   <li>Overvoltage effects run only after a successful commit, and only when
 *       {@link IndependentEnergyMode} enables them:
 *     <ul>
 *       <li>Voltage above the conductor breakdown limit: the conductor melts
 *           (the block is removed).</li>
 *       <li>Voltage above the insulation breakdown energy: the insulation is
 *           removed.</li>
 *       <li>Voltage above the insulation absorption: living entities within
 *           1.5 blocks of the conductor get shock damage.</li>
 *       <li>Voltage above the sink input limit, on a path that did not exceed a
 *           conductor limit: the sink explodes. An {@link IOverloadHandler} or an
 *           {@link IExplosionPowerOverride} can stop or change the explosion.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Foreign energy (FE) path</h2>
 * <ul>
 *   <li>{@code discoverForeignBatch} checks up to 128 anchors (conductors and
 *       endpoints) per tick. It adds each valid FE neighbour to
 *       {@code WorldGrid.foreign} (at most 4096 contacts).</li>
 *   <li>The export pass and the import pass visit (source, target) pairs in
 *       round-robin order with {@code transferCursor} and {@code importCursor}.
 *       Each pass visits at most 256 pairs per tick.</li>
 *   <li>A {@code ForeignRoutePlan} finds the cheapest route for one pair. It
 *       resumes across ticks in the same way as a native plan.</li>
 *   <li>FE has no packets. Export uses the remaining packet allowance of the SI
 *       source as its limit. Import records a voltage of 0 for meters.</li>
 *   <li>Before each FE callback, the engine checks {@code foreignRouteCurrent}.
 *       A stale route never reaches a live capability.</li>
 *   <li>The import pass skips a foreign contact that received an export in the
 *       same tick. This stops energy from moving back and forth.</li>
 * </ul>
 *
 * <h2>Failure isolation</h2>
 * <ul>
 *   <li>One endpoint throws while the engine reads it ({@code ports},
 *       {@code refreshQuotes}, {@code valid}): the engine calls
 *       {@code topology.isolateChunk} for the chunk of that endpoint. All other
 *       chunks continue.</li>
 *   <li>A settlement throws in {@link #beforeLevel} or {@link #afterLevel}:
 *       {@code fail(ServerLevel, WorldGrid, RuntimeException)} logs the error
 *       and rebuilds the topology of that dimension only.</li>
 *   <li>The topology reports a failure in {@link #tick}:
 *       {@code fail(RuntimeException)} sets {@code failure} and closes the
 *       topology. All dimensions stop until the server restarts.</li>
 *   <li>A foreign FE callback throws during a transfer: the engine logs a
 *       warning and increments {@code rejected}. An export source is skipped
 *       for the rest of the tick ({@code failedOutputs}). The engine does not
 *       refund energy that the source already prepared.</li>
 *   <li>A foreign query throws: {@code foreignQuery} returns {@code false} and
 *       blocks that contact for the rest of the tick.</li>
 *   <li>The endpoint limit ({@value #ENDPOINT_LIMIT} per dimension) is
 *       reached: the extra endpoints stay disconnected. The engine logs a
 *       warning and does not fail.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>All instance state is confined to the server thread. Public entry points
 * check {@code server.isSameThread()} or {@code ownerThread} and return or
 * throw on another thread. The static {@code SERVERS} map is guarded by the
 * class monitor. Do not call any method of this class from a worker thread.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>A transfer never creates or destroys energy. For each commit, the sum
 *       of debits equals the sum of credits plus the conductor loss. The
 *       delivery trace check in {@code settlePlan} throws when this is false,
 *       and {@link NetworkCell#commit} rejects an unbalanced write set.</li>
 *   <li>A commit is all or nothing. It runs only when {@code valid} confirms
 *       that every involved port still matches the world.</li>
 *   <li>Each cursor stays in {@code [0, size)} of its list when it is used.</li>
 *   <li>A source cannot send more in one settlement than its frozen allowance.</li>
 *   <li>A plan is used only with the conductor snapshot and the routing
 *       generation that built it.</li>
 *   <li>The work of one tick is bounded by the budget constants of this
 *       class.</li>
 * </ul>
 */
public final class IndependentSiEnergy implements PlatformTopology.Observer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IndependentSiEnergy.class);
    /**
     * Endpoint cap per level. Exceeding it keeps extra endpoints disconnected and
     * warns, instead of throwing into fail() which would disable energy transfer
     * for every level until restart (large-storage worlds hit the old 4096 cap).
     * The same cap applies to registrations through {@link #registerTile}.
     */
    private static final int ENDPOINT_LIMIT = 65_536;
    /**
     * Route construction is a server tick operation.  A dense component can
     * otherwise produce a source × receiver × contact cross product and spend
     * seconds in HashMap updates before the distributor ever runs.
     * The limits below bound that work.
     *
     * <p>This constant: maximum route records ({@code NativeRoutePlan.record}
     * calls) per grid per tick.
     */
    private static final int MAX_ROUTE_RECORDS_PER_TICK = 16_384;
    /** Maximum route array cells (one {@code long} loss and one path slot each) per grid per tick. */
    private static final long MAX_ROUTE_ARRAY_CELLS_PER_TICK = 2_000_000L;
    /** Maximum sources and maximum sinks in one native window. */
    private static final int MAX_PLAN_SOURCES = 32, MAX_PLAN_RECEIVERS = 128;
    /** Maximum sum of search weights of all origins of one native plan. */
    private static final long MAX_PLAN_ROUTE_VERTICES = 800_000;
    /**
     * Maximum conductor visits per grid per tick for native world effects. One
     * delivery visits its path twice (effect scan and meter samples), so the
     * code multiplies vertex counts by 2 before it compares them with this value.
     */
    private static final long MAX_NATIVE_PATH_VISITS_PER_TICK = 2_000_000;
    /** One engine per running server. Guarded by the class monitor. */
    private static final Map<MinecraftServer, IndependentSiEnergy> SERVERS = new IdentityHashMap<>();
    /** True after {@link #install()} registered its listener. Guarded by the class monitor. */
    private static boolean installed;

    /**
     * Tells whether this engine controls a block state.
     *
     * @param state the block state to test
     * @return {@code true} when the independent energy mode is on and the
     *         {@link EnergyNodeRegistry} admits the block
     */
    public static boolean controls(BlockState state) {
        return com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()
            && EnergyNodeRegistry.admits(state);
    }

    /**
     * Registers a listener that attaches an engine to each server that starts.
     * The listener attaches only when the independent energy mode is on.
     * A second call has no effect. Call this method once, during mod setup.
     */
    public static synchronized void install() {
        if (installed) return; installed = true;
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
            if (com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()) attach(event.getServer());
        });
    }

    /**
     * Returns the engine of a server and creates it on the first call.
     * The first call also freezes the {@link EnergyNodeRegistry}. Register all
     * node types before the first server starts.
     *
     * @param server the server, on its own thread
     * @return the engine of {@code server}, never {@code null}
     * @throws IllegalStateException when the caller is not on the server thread
     */
    public static synchronized IndependentSiEnergy attach(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Attach on server thread");
        EnergyNodeRegistry.freeze();
        return SERVERS.computeIfAbsent(server, IndependentSiEnergy::new);
    }

    /**
     * Returns the engine of a server.
     *
     * @return the engine, or {@code null} when no engine is attached or the
     *         server stopped
     */
    public static synchronized IndependentSiEnergy current(MinecraftServer server) { return SERVERS.get(server); }

    /** Same as {@link #changed(BlockEntity)}. */
    public static void changed(AbstractEnergyBlockEntity tile) {
        changed((BlockEntity) tile);
    }

    /**
     * Tells the topology that a block entity changed its electrical shape.
     * The topology then observes the position again, and the change is
     * published in the next frame. Call this method when the faces, the role
     * (source or sink) or the identity of an endpoint change. A balance change
     * does not need this call. The method does nothing on a client level or
     * when no engine is attached.
     */
    public static void changed(BlockEntity tile) {
        if (tile.getLevel() instanceof ServerLevel level) {
            var engine = current(level.getServer()); if (engine != null) engine.topology.changed(level, tile.getBlockPos());
        }
    }
    /**
     * Returns the storage that a block entity owns, or {@code null} when it owns
     * none. A {@code null} result means that the tile is not an SI endpoint.
     * The test order matters: a {@link DemandEnergySource} can also be an
     * {@link AbstractEnergyBlockEntity}, and its demand storage is the correct one.
     */
    private static CustomEUEnergyStorage ownedStorage(BlockEntity tile) {
        return tile instanceof DemandEnergySource source ? source.ownedEnergy()
            : tile instanceof EnergyComponentHost host ? host.energyComponent().internalStorage()
            : tile instanceof AbstractEnergyBlockEntity machine ? machine.getEnergyStorageInternal() : null;
    }
    /**
     * Returns the capacity in EU of the storage that a tile owns. It uses the
     * same test order as {@link #ownedStorage}.
     * Precondition: {@code ownedStorage(tile) != null}. Otherwise the final cast
     * throws {@link ClassCastException}.
     */
    private static long ownedCapacity(BlockEntity tile) {
        return tile instanceof DemandEnergySource source ? source.ownedEnergy().getCapacity()
            : tile instanceof EnergyComponentHost host ? host.energyComponent().capacity()
            : ((AbstractEnergyBlockEntity)tile).getEffectiveCapacity();
    }

    /**
     * Returns the shared output budget of a tile. The FE bridge and the native
     * EU pass take output from the same budget, so one tick cannot spend the same
     * energy twice through the two paths.
     *
     * @return the output quote, or {@code null} when the tile has no FE ledger
     */
    private static FeLedger.OutputQuote outputBudget(BlockEntity tile) {
        if (tile instanceof EnergyComponentHost host) return host.energyComponent().internalLedger().quoteNativeOutput();
        return tile instanceof AbstractEnergyBlockEntity block ? block.scexFeBridge().quoteNativeOutput() : null;
    }

    /**
     * Returns the conductor loss of a tile in milli-EU per block (1000 = 1 EU).
     * A loss configured in the {@link EnergyNodeRegistry} has priority over
     * the loss that the tile reports.
     *
     * @return the loss ({@code >= 0}), or {@code null} when the tile is not a
     *         conductor
     * @throws IllegalStateException when the tile reports a loss that is not
     *         finite, is negative, or does not fit in a {@code long} after the
     *         scale to milli-EU
     */
    private static Long conductorLoss(BlockEntity tile) {
        var configured = EnergyNodeRegistry.conductorLoss(BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()));
        if (configured != null) return configured;
        if (!(tile instanceof ICableEnergyNode wire)) return null;
        double loss = wire.getConductionLoss();
        if (!Double.isFinite(loss) || loss < 0 || loss > Long.MAX_VALUE / 1000.0)
            throw new IllegalStateException("Invalid conductor loss at " + tile.getBlockPos());
        // Round up, so that a small nonzero loss never becomes 0 milli-EU.
        return (long)Math.ceil(loss * 1000.0);
    }

    /**
     * Returns the electrical face mask of a tile (bit i = {@code Direction}
     * ordinal i). A face is closed when the cable blocks that direction, or
     * when a colored tile has a color on that face and the loaded neighbour
     * does not accept the connection. An unloaded neighbour does not close
     * the face. The method never loads a chunk.
     * A {@code null} tile returns {@link ConductorRegistry#ALL_FACES}.
     */
    private static int conductorFaces(BlockEntity tile) {
        int result = ConductorRegistry.ALL_FACES;
        if (tile instanceof ICableEnergyNode wire) {
            // get3DDataValue() and ordinal() give the same index for Direction.
            for (Direction side : Direction.values()) if (wire.isDirectionBlocked(side))
                result &= ~(1 << side.get3DDataValue());
        }
        if (tile instanceof com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile colored
                && tile.getLevel() instanceof ServerLevel level) {
            for (Direction side : Direction.values()) {
                if (colored.getEnergyColor(side) == null) continue;
                var at = tile.getBlockPos().relative(side);
                var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                if (chunk != null && !com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile.connects(
                        tile, chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK), side))
                    result &= ~(1 << side.ordinal());
            }
        }
        return result;
    }

    /**
     * Applies a face edit of a conductor at once, without a wait for the next
     * frame. Stale routes through the closed faces stop in the same tick.
     * The method does not load or create a chunk.
     *
     * <p>It always bumps the {@link TopologyClock}. Then it returns early unless
     * all of these are true: the call is on the server thread, the tile is live
     * and network-controlled, the engine runs, and the tile is the entity at its
     * position in a loaded chunk. After these checks, it updates the conductor
     * index only when the grid already registered this tile as the conductor at
     * that position. It then always queues a normal topology observation.
     */
    public static void conductorPortsChanged(BlockEntity tile) {
        TopologyClock.bump(tile.getLevel());
        if (!(tile.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || tile.isRemoved() || ownedStorage(tile) == null || !ownedStorage(tile).scexNetworkControlled()) return;
        var engine = current(level.getServer());
        if (engine == null || engine.closed || !engine.failure.isEmpty()) return;
        var at = tile.getBlockPos();
        var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
        if (chunk == null || chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK) != tile) return;
        var grid = engine.worlds.get(level);
        Long loss = conductorLoss(tile);
        if (grid != null && loss != null && grid.conductorOwners.get(at) == tile
                && grid.conductors.containsRegistered(point(at))) {
            int faces = conductorFaces(tile);
            grid.conductors.put(point(at), loss, faces);
            grid.contactOrder.putConductor(point(at), loss, faces);
        }
        engine.topology.changed(level, at);
    }

    /**
     * Applies a color edit of a conductor. A color change affects both ends of
     * each adjacent connection, so the method also calls
     * {@link #conductorPortsChanged} for each loaded neighbour block entity.
     */
    public static void conductorColorChanged(BlockEntity tile) {
        conductorPortsChanged(tile);
        if (!(tile.getLevel() instanceof ServerLevel level)) return;
        for (Direction side : Direction.values()) {
            var at = tile.getBlockPos().relative(side);
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk != null) {
                var other = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                if (other != null) conductorPortsChanged(other);
            }
        }
    }

    /** {@inheritDoc} Topology callback. Returns the face mask of the block entity at {@code at}. */
    @Override public int conductorFaces(ServerLevel level, LevelChunk chunk, BlockPos at) {
        // The FULL chunk is supplied by the topology. Materializing its own saved
        // block entity does not request or keep another chunk loaded.
        return conductorFaces(chunk.getBlockEntity(at, LevelChunk.EntityCreationType.IMMEDIATE));
    }

    /**
     * Cycles the mode of a transformer when a player sneaks and right-clicks it
     * with an empty main hand. The server decides the mode.
     * Modes: 0 = fixed step-up, 1 = fixed step-down, 2 = redstone-controlled.
     * The method cancels the event only when it changed the mode. A normal
     * right-click passes through, so the block can open its menu.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()
                || event.getEntity().isSpectator() || !event.getEntity().mayBuild()
                || !level.mayInteract(event.getEntity(), event.getPos())) return;
        var at = event.getPos();
        var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
        if (chunk == null || !(chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK)
                instanceof IndependentTransformerBlockEntity transformer)) return;
        // Sneak + empty-hand right-click cycles the mode (server-authoritative).
        // A normal right-click is not intercepted. The block's useWithoutItem then
        // opens the menu, and the menu buttons change the mode through the menu channel.
        if (!event.getEntity().isShiftKeyDown()) return;
        // floorMod keeps the mode in [0, 3) even when a saved mode is out of range.
        int next = Math.floorMod(transformer.savedMode() + 1, 3);
        transformer.setSavedMode(next);
        topology.changed(level, at);
        // 0.1.7.36: translatable text (before: a hard-coded Chinese literal). The fallback text
        // shows when a resource pack has no key, for example on a core-only install.
        String key = next == 0 ? "step_up" : next == 1 ? "step_down" : "redstone_control";
        String fallback = next == 0 ? "Step-Up Mode" : next == 1 ? "Step-Down Mode" : "Redstone Control";
        var label = net.minecraft.network.chat.Component.translatableWithFallback("gui.mio_icif.transformer.mode." + key, fallback);
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback(
            "message.mio_icif.transformer.mode_switched", "Transformer: %s. Sneak + empty-hand right click to switch.", label), true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** The server of this engine. It is {@code null} after {@link #stopped}. */
    private MinecraftServer server;
    /** The id of the thread that created the engine (the server thread). */
    private final long ownerThread = Thread.currentThread().threadId();
    /** Watches chunks and blocks, and calls the observer methods of this engine. */
    private final PlatformTopology topology;
    /** One grid per dimension. A grid is created on the first controlled block it sees. */
    private final Map<ServerLevel, WorldGrid> worlds = new HashMap<>();
    // Record the independent seed in probe metrics so this selection stream can
    // be replayed. It is not a seed or algorithm taken from the reference mod.
    private final long selectionSeed = Long.getLong("scex.independent.selectionSeed", ThreadLocalRandom.current().nextLong());
    private final SplittableRandom selectionRandom = new SplittableRandom(selectionSeed);
    // A separate independent stream keeps visible block effects from changing
    // receiver selection. This is not the reference game's PRNG or seed.
    private final SplittableRandom blockDropRandom = new SplittableRandom(selectionSeed ^ 0x5343455844524f50L);
    /**
     * Counters since the engine started. {@code ticks}: server ticks.
     * {@code commits}: successful commits (native and FE). {@code rejected}:
     * commits that a validity check refused, and FE transfers that threw.
     * {@code debited}, {@code credited}, {@code dissipated}: whole EU parts of
     * the exact totals below.
     */
    private long ticks, commits, rejected, debited, credited, dissipated;
    /** Exact EU totals. Invariant: totalDebit == totalCredit + totalLoss after each commit. */
    private EnergyAmount totalDebit = EnergyAmount.ZERO, totalCredit = EnergyAmount.ZERO, totalLoss = EnergyAmount.ZERO;
    /** Effect counters: deliveries, conductor visits, melted conductors, exploded sinks, blocks removed by blasts. */
    private long deliveryCount, deliveryWireVisits, fusedWires, destroyedReceivers, blastBlocks;
    /** Number of foreign queries that threw. It saturates at {@link Long#MAX_VALUE}. */
    private long foreignQueryFailures;
    /**
     * {@code lastForeignQueryFailure}: text of the last foreign query failure,
     * at most 256 characters. {@code failure}: text of the engine failure.
     * An empty {@code failure} means that the engine runs. A non-empty value
     * stops every dimension until restart.
     */
    private String lastForeignQueryFailure = "", failure = "";
    /** True after the server stopped. A closed engine does no work. */
    private boolean closed;
    /**
     * Returns read-only diagnostics of one dimension for startup investigations.
     * The call does not change engine state.
     *
     * @throws IllegalArgumentException when the level belongs to another server
     *         or the caller is not on the server thread
     */
    public Map<String, Object> startupDiagnostics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalArgumentException("Wrong server context");
        var grid = worlds.get(level);
        return Map.of("topology", topology.metrics(), "ready", topology.ready(level),
            "world_grid", grid != null, "catch_up", grid != null && grid.catchUp,
            "entries", grid == null ? 0 : grid.machines.size(),
            "world_grid_failure", grid == null ? "" : grid.failure,
            "foreign_query_failures", foreignQueryFailures, "last_foreign_query_failure", lastForeignQueryFailure);
    }
    /**
     * A snapshot of the engine counters. All energy values are EU.
     * {@code debited}, {@code credited} and {@code dissipated} are whole EU.
     * The three {@code ...Fraction} values are the fraction parts in
     * {@link EnergyAmount} units. {@code endpoints} counts endpoints in all
     * dimensions.
     */
    public record Metrics(long ticks, long commits, long rejected, long debited, long credited, long dissipated,
                          int dimensions, int endpoints, boolean closed, String failure, long selectionSeed,
                          long deliveryCount, long deliveryWireVisits, long fusedWires, long destroyedReceivers, long blastBlocks,
                          long debitedFraction, long creditedFraction, long dissipatedFraction) { }
    /**
     * Routing work counters of one dimension. They are separate from balances,
     * so a diagnostics read cannot renew a quote.
     * {@code topologyGenerations}: value of {@code portsGeneration}.
     * {@code plansBuilt}: native plans created. {@code plansResumed}: native
     * plans resumed with origins left. {@code budgetYields}: times a native plan
     * stopped because a search budget was empty.
     */
    public record RoutingMetrics(long topologyGenerations, long plansBuilt, long plansResumed, long budgetYields) { }
    /**
     * Returns the routing counters of one dimension, or zeros when it has no grid.
     *
     * @throws IllegalStateException when the level belongs to another server or
     *         the caller is not on the server thread
     */
    public RoutingMetrics routingMetrics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalStateException("Wrong routing diagnostics thread");
        var grid = worlds.get(level);
        return grid == null ? new RoutingMetrics(0, 0, 0, 0)
            : new RoutingMetrics(grid.portsGeneration, grid.plansBuilt, grid.plansResumed, grid.budgetYields);
    }
    /**
     * Returns the settlement timings of one dimension (nanoseconds per tick),
     * or an empty snapshot when it has no grid.
     *
     * @throws IllegalStateException when the level belongs to another server or
     *         the caller is not on the server thread
     */
    public com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings.Snapshot performanceMetrics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalStateException("Wrong diagnostics context");
        var grid = worlds.get(level);
        return grid == null ? new com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings.Snapshot(0,0,0,0,0) : grid.timings.snapshot();
    }
    /**
     * A read-only snapshot of one endpoint for one settlement.
     *
     * <ul>
     *   <li>{@code tile}, {@code position}, {@code state}: the endpoint identity
     *       when the snapshot was taken. {@code valid} compares them with the
     *       world before a commit.</li>
     *   <li>{@code storage}: the owned storage. It is {@code null} for a
     *       transformer.</li>
     *   <li>{@code quote}: the balance quote. A commit writes only when the quote
     *       is still current.</li>
     *   <li>{@code capacity}: storage capacity in EU.</li>
     *   <li>{@code inputs}, {@code outputs}: face masks (0..63).</li>
     *   <li>{@code packet}: packet size in EU ({@code >= 1} for a non-transformer).
     *       {@code packets}: packets per tick. {@code partialPackets}: the source
     *       can send a last packet smaller than {@code packet}.</li>
     *   <li>{@code transformer}: the transformer snapshot, or {@code null}.</li>
     *   <li>{@code outputBudget}: the shared FE/EU output budget, or {@code null}.</li>
     *   <li>{@code potential}: for a {@link DemandEnergySource} in a pass that may
     *       acquire fuel, the EU that the source can make. Otherwise {@code null}.</li>
     *   <li>{@code emitterPositions}: the blocks whose faces carry energy. It is
     *       the tile position, or the contact blocks of a multi-block source.</li>
     * </ul>
     */
    private record Port(BlockEntity tile, BlockPos position, BlockState state,
                        CustomEUEnergyStorage storage, CustomEUEnergyStorage.NetworkQuote quote,
                        long capacity, int inputs, int outputs, long packet, int packets, boolean partialPackets,
                        IndependentTransformerBlockEntity.Snapshot transformer, FeLedger.OutputQuote outputBudget,
                        EnergyAmount potential, List<BlockPos> emitterPositions) {
        /**
         * Returns the EU that this port can offer in this tick: the potential
         * energy (if set) or the stored balance, limited by the output budget.
         */
        EnergyAmount offered() {
            if (potential != null) return outputBudget == null ? potential : outputBudget.limitOffer(potential);
            return outputBudget == null ? quote.exactAmount() : outputBudget.limitOffer(quote.exactAmount());
        }
        /** Returns the packet policy of this port as one value. */
        EnergyPacketPolicy policy() { return new EnergyPacketPolicy(packet, packets, partialPackets); }
        /** Returns the balance that the distributor uses: the potential energy if set, else the stored EU. */
        EnergyAmount balance() { return potential != null ? potential : quote.exactAmount(); }
    }
    /**
     * Identifies one native window inside one settlement. The native pass
     * settles each window at most once per settlement. A repeat key means that
     * the cursors went around once, so the pass stops the loop for that source.
     */
    private record NativeWindowKey(long generation, BlockEntity source, int firstReceiver) { }
    /**
     * One topology entry, as {@code position} observed it.
     * {@code conductorLoss}: milli-EU per block for a conductor ({@code >= 0}),
     * -1 for an endpoint, -2 for a special cable that does not conduct now.
     * {@code routing}: the routing signature of a transformer, else 0.
     * {@code faces}: the face mask. Equality compares {@code tile} by identity,
     * because two different block entities at one position are two entries.
     */
    private record Element(ResourceLocation type, long conductorLoss, BlockEntity tile, int routing, int faces) {
        @Override public boolean equals(Object other) {
            return other instanceof Element e && type.equals(e.type) && conductorLoss == e.conductorLoss && tile == e.tile && routing == e.routing && faces == e.faces;
        }
        @Override public int hashCode() { return 31 * type.hashCode() + Long.hashCode(conductorLoss) + System.identityHashCode(tile) + routing + 31 * faces; }
    }
    /**
     * All energy state of one dimension. The engine creates one grid per
     * {@link ServerLevel} and closes it when the level unloads or the server
     * stops. A grid can contain many separate circuits. Only the server thread
     * reads or writes a grid.
     *
     * <p><b>Two layers.</b> {@code entries} holds the observed entries. They are
     * not visible to routing yet. {@link #apply} publishes changes into the
     * routing indexes ({@code conductors}, {@code contactOrder}, {@code machines}
     * and the side maps). Only published data affects a transfer.
     *
     * <p><b>Generation counters.</b>
     * <ul>
     *   <li>{@code endpointEdits}: increments when {@code apply} publishes a
     *       change or an effect removes an endpoint. Cached port lists are then
     *       stale.</li>
     *   <li>{@code balanceWrites}: increments on each native balance write.
     *       Cached quotes are then stale.</li>
     *   <li>{@code portsGeneration}: increments when the routing identity of the
     *       source or sink lists changes. Plans of an older generation are
     *       discarded.</li>
     * </ul>
     * <p>The counters only increase. A cache stores the value it was built from,
     * and compares it with the live value.
     */
    private static final class WorldGrid implements AutoCloseable {
        /** Observed entries per position, published one frame later. Bounded to 104,096 entries. */
        final DeferredEntries<BlockPos, Element> entries = new DeferredEntries<>(104_096, 65_536);
        // Retain small independent networks without a 32-source cyclic scan miss.
        // Route arrays remain bounded by the previous 32 x 100,000-node maximum.
        final ConductorRegistry conductors = new ConductorRegistry(100_000, 4096, 3_200_000);
        // Separate reservations prevent a permanently busy native grid from
        // starving imports or exports. Total search admission is <=400k
        // component vertices per world tick, including START/END and queries.
        final ConductorRegistry.SearchBudget nativeSearch = new ConductorRegistry.SearchBudget(200_000);
        final ConductorRegistry.SearchBudget exportSearch = new ConductorRegistry.SearchBudget(100_000);
        final ConductorRegistry.SearchBudget importSearch = new ConductorRegistry.SearchBudget(100_000);
        /** Route build limits of this grid for the current tick. */
        final RouteBuildBudget routeBudget = new RouteBuildBudget();
        /** The native plan in progress, or {@code null}. A plan with origins left resumes on the next tick. */
        NativeRoutePlan nativePlan;
        /** Routing identities of all sources and sinks for {@code portsGeneration}, or {@code null}. */
        RoutingView routingView;
        /** Routing generation. It only increases. See the class comment. */
        long portsGeneration;
        /** Diagnostics counters. See {@link RoutingMetrics}. */
        long plansBuilt, plansResumed, budgetYields;
        /** Bumped whenever the endpoint/conductor sets change. Cached port lists must then be rebuilt. */
        long endpointEdits;
        /** FE route plans in progress for the export pass and the import pass, or {@code null}. */
        ForeignRoutePlan exportPlan, importPlan;
        /**
         * Index of the first source of the next native window. Any {@code int}
         * value is allowed in storage. The code applies {@code floorMod} with the
         * source count before use.
         */
        int nativeCursor;
        /**
         * Per first source of a window: the index of the first sink of the next
         * window. The plan constructor applies {@code floorMod} with the reachable
         * sink count before use.
         */
        final Map<BlockPos, Integer> receiverCursors = new HashMap<>();
        /** Placement order of endpoints and conductors. It orders sinks for generators with two or more contacts. */
        final ContactOrder contactOrder;
        /** @param constructionSeed the seed of the contact order stream */
        WorldGrid(long constructionSeed) { contactOrder = new ContactOrder(104_096, 4096, constructionSeed); }
        /**
         * Published endpoints by position, in publication order. At most
         * {@link #ENDPOINT_LIMIT} entries. A dual tile ({@link IEnergyConductiveEndpoint})
         * is a conductor and an endpoint at the same time.
         */
        final Map<BlockPos, BlockEntity> machines = new LinkedHashMap<>();
        /** Number of endpoints and registrations that the limit refused. Used to throttle the warning log. */
        long endpointLimitDrops, registrationLimitDrops;
        /** Published routing signature per transformer. A transformer with another live signature is not used. */
        final Map<BlockPos, Integer> transformerRouting = new HashMap<>();
        /** Published transformers, for the mode refresh in {@code beforeLevel}. */
        final Map<BlockPos, IndependentTransformerBlockEntity> transformers = new LinkedHashMap<>();
        /** Published special cables (switching and detector cables). */
        final Map<BlockPos, IndependentSpecialCableBlockEntity> specialCables = new LinkedHashMap<>();
        /** Special cable state: last observed value and last published value per position. */
        final Map<BlockPos, Element> specialObserved = new HashMap<>(), specialPublished = new HashMap<>();
        /** Special cable changes per publication frame. {@code observeSpecial} throws when more than 4 frames are pending. */
        final java.util.NavigableMap<Long, Map<BlockPos, Element>> specialPending = new java.util.TreeMap<>();
        /**
         * Per generator: the conductors that touched it when it was published.
         * {@code NativeRoutePlan.origins} uses this to keep two original contacts
         * as separate route entries.
         */
        final Map<BlockPos, Set<BlockPos>> initialGeneratorContacts = new HashMap<>();
        /** Number of published conductors per chunk ({@link ChunkPos#asLong} key). No zero values are kept. */
        final Map<Long, Integer> conductorChunks = new HashMap<>();
        /** Block type of each published conductor. Effects use it to find limits. */
        final Map<BlockPos, ResourceLocation> conductorTypes = new HashMap<>();
        /** Block entity of each published conductor. */
        final Map<BlockPos, BlockEntity> conductorOwners = new HashMap<>();
        /** Tiles registered through {@link EnergyNetGlobal#addTile}. At most {@link #ENDPOINT_LIMIT} entries. */
        final Map<BlockPos, IEnergyTile> registered = new LinkedHashMap<>();
        /**
         * {@code catchUpEndpoints}: endpoints that an END paused. The next START
         * settles them. {@code effectEndpoints}: endpoints near a world effect.
         * They pause at the END of the effect pause frame.
         */
        final Set<BlockPos> catchUpEndpoints = new HashSet<>(), effectEndpoints = new HashSet<>();
        /** Settlement durations. */
        final com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings timings = new com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings();
        /**
         * The scope of a scoped settlement. {@code null} means no limit. Only
         * {@code settleScoped} sets these fields, and it always resets them to
         * {@code null} in a {@code finally} block.
         */
        Set<BlockPos> includedEndpoints, excludedEndpoints;
        /** Shock damage collected in one settlement. It is applied and cleared at the end of the settlement. */
        final com.miophas.singularity_iteration.core.runtime.energy.engine.ShockAccumulator<ShockRoute, LivingEntity> shocks =
            new com.miophas.singularity_iteration.core.runtime.energy.engine.ShockAccumulator<>();
        /** Known foreign FE contacts. At most 4096 entries. */
        final BoundedContactIndex<ForeignContact, FECompatTile> foreign = new BoundedContactIndex<>(4096);
        /** The tick of the last foreign contact maintenance. Maintenance runs once per tick. */
        long foreignMaintenanceTick = Long.MIN_VALUE;
        /** True during foreign discovery. It stops a callback from starting a second discovery. */
        boolean discoveringForeign;
        /** Foreign contacts whose query threw in {@code queryTick}. At most 4096 entries. */
        final Set<ForeignContact> failedQueries = new HashSet<>();
        /** The tick that {@code failedQueries} belongs to. */
        long queryTick = Long.MIN_VALUE;
        /**
         * Meter samples of {@code sampleTick}: EU in, EU out, and the highest
         * voltage per position. The values for a node are sums over the tick.
         */
        final Map<BlockPos, NodeStats> samples = new HashMap<>();
        /** EU that each source sent in {@code sampleTick}, native and FE together. */
        final Map<BlockPos, EnergyAmount> packetSpent = new HashMap<>();
        /** Sources whose FE export threw in {@code sampleTick}. The export pass skips them. */
        final Set<BlockPos> failedOutputs = new HashSet<>();
        /** EU that each transformer received in {@code sampleTick}. It limits FE import to the input limit. */
        final Map<BlockPos, EnergyAmount> transformerReceived = new HashMap<>();
        /** Anchor positions for foreign discovery: conductors, then endpoints. */
        List<BlockPos> scanAnchors = List.of();
        /** Foreign contacts that received an export in {@code sampleTick}. The import pass skips them. */
        final Set<BlockPos> exportedThisTick = new HashSet<>();
        /** Positions whose balances the latest native commit wrote (its window's ports). */
        final Set<BlockPos> lastCommitted = new HashSet<>();
        /** Bumped on every native balance write (commit or demand acquisition). It drives the quote refresh. */
        long balanceWrites;
        /**
         * Round-robin cursors: next discovery anchor, next export pair, next
         * import pair. Each use applies {@code floorMod} first, and each advance
         * wraps to 0 at the end. The stored value is therefore in {@code [0, size)}
         * after each advance.
         */
        int scanCursor, transferCursor, importCursor;
        /** True when {@code scanAnchors} must be rebuilt (an anchor was added or removed). */
        boolean scanDirty = true;
        // Chunk availability is queried by native settlement and both FE
        // directions in the same server tick.  Re-evaluating every registered
        // conductor for each pass was a hot O(chunks * passes) path.  The
        // topology observer revokes this value when a chunk is removed. Normal
        // ticket changes are consequently observed on the next tick at the
        // latest. All transaction leases still recheck isCurrent().
        long accessibilityTick = Long.MIN_VALUE;
        /** The topology readiness revision of the cached accessibility result. */
        long accessibilityRevision = Long.MIN_VALUE;
        /** The cached accessibility result. Valid only for {@code accessibilityTick} and {@code accessibilityRevision}. */
        boolean accessibilityResult;
        /**
         * The server tick of {@code samples}, {@code packetSpent}, {@code failedOutputs},
         * {@code transformerReceived} and {@code exportedThisTick}. {@code Long.MIN_VALUE}
         * means "never sampled". Do not subtract from it without a sentinel test.
         */
        long sampleTick = Long.MIN_VALUE;
        /** True when {@code catchUpEndpoints} has entries for the next START. */
        boolean catchUp;
        /** True after {@link #close()}. */
        boolean closed;
        /**
         * Failure text of this grid. Empty means no failure. Many methods skip a
         * grid with a non-empty value. Note: no code in this file assigns it.
         */
        String failure = "";
        /** The frame whose END pauses {@code effectEndpoints}, or -1 for none. */
        long effectPauseFrame = -1;
        /**
         * Queues one special cable observation for publication in {@code frame + 1}.
         * An observation equal to the last one is ignored.
         * Unlike ordinary physical add/remove coalescing, measured splitter
         * pulses preserve the observed off/on transitions in consecutive frames.
         *
         * @param value the new entry, or {@code null} when the special cable is gone
         * @throws IllegalStateException when more than 100,000 positions are
         *         observed or more than 4 frames are pending
         */
        void observeSpecial(long frame, BlockPos at, Element value) {
            if (java.util.Objects.equals(specialObserved.get(at), value)) return;
            if (value == null) specialObserved.remove(at); else specialObserved.put(at, value);
            if (specialObserved.size() > 100_000 || specialPending.size() > 4)
                throw new IllegalStateException("Special cable queue limit reached");
            specialPending.computeIfAbsent(frame + 1, ignored -> new LinkedHashMap<>()).put(at, value);
        }
        /**
         * Publishes all special cable changes of frames up to {@code frame}.
         * An enabling change is published only when the cable still conducts now.
         *
         * @return the changes to give to {@link #apply}, in frame order
         */
        List<DeferredEntries.Change<BlockPos, Element>> advanceSpecial(long frame) {
            var result = new ArrayList<DeferredEntries.Change<BlockPos, Element>>();
            while (!specialPending.isEmpty() && specialPending.firstKey() <= frame) {
                for (var item : specialPending.pollFirstEntry().getValue().entrySet()) {
                    var at = item.getKey(); var before = specialPublished.get(at); var after = item.getValue();
                    if (after != null && after.conductorLoss >= 0) {
                        var current = specialObserved.get(at);
                        // A transient enabling pulse which ended before publication
                        // must not create a conducting entry for the START catch-up.
                        if (current == null || current.tile != after.tile || current.conductorLoss < 0) continue;
                    }
                    if (after == null) specialPublished.remove(at); else specialPublished.put(at, after);
                    if (!java.util.Objects.equals(before, after)) result.add(new DeferredEntries.Change<>(at, before, after));
                }
            }
            return result;
        }
        /**
         * Removes every observed and published entry whose position matches.
         * It does not change the routing indexes. Give the result to
         * {@link #apply} to remove the entries from routing.
         *
         * @return one removal change per published entry that matched
         */
        List<DeferredEntries.Change<BlockPos, Element>> forget(java.util.function.Predicate<BlockPos> predicate) {
            var result = new ArrayList<>(entries.forgetIf(predicate));
            var iterator = specialPublished.entrySet().iterator();
            while (iterator.hasNext()) {
                var item = iterator.next();
                if (predicate.test(item.getKey())) { result.add(new DeferredEntries.Change<>(item.getKey(), item.getValue(), null)); iterator.remove(); }
            }
            specialObserved.keySet().removeIf(predicate);
            specialPending.values().forEach(batch -> batch.keySet().removeIf(predicate));
            specialPending.values().removeIf(Map::isEmpty);
            return result;
        }
        /**
         * Publishes changes into the routing indexes. This is the only method that
         * adds conductors and endpoints to routing.
         *
         * <p>For each change:
         * <ol>
         *   <li>Fast path: a conductor whose loss, tile, type and routing stay the
         *       same gets only a face update. Its other state is kept.</li>
         *   <li>Otherwise the method removes all state of the old entry, and then
         *       adds the new entry, if there is one.</li>
         *   <li>A new endpoint beyond {@link #ENDPOINT_LIMIT} is not added. The
         *       method logs a warning and continues.</li>
         *   <li>A new generator records its initial conductor contacts.</li>
         * </ol>
         * <p>Postcondition: {@code endpointEdits} increased when {@code changes}
         * is not empty, and the accessibility cache is invalid.
         */
        void apply(List<DeferredEntries.Change<BlockPos, Element>> changes) {
            if (!changes.isEmpty()) {
                endpointEdits++;
                // A publication can add/remove a conductor after an earlier
                // accessibility pass in this tick. Do not let a later route
                // query reuse the old chunk mask.
                accessibilityTick = Long.MIN_VALUE;
            }
            for (var change : changes) {
                var at = change.key(); long chunk = ChunkPos.asLong(at);
                var before = change.before(); var next = change.after();
                // Face/routing updates do not change the discovery work set.
                // Restarting here would starve anchors beyond the scan budget.
                boolean wasAnchor = conductorOwners.containsKey(at) || machines.containsKey(at);
                boolean isAnchor = next != null && (next.conductorLoss >= 0
                        || !(next.tile instanceof IndependentSpecialCableBlockEntity));
                if (wasAnchor != isAnchor) scanDirty = true;
                if (before != null && next != null && before.conductorLoss >= 0
                        && before.conductorLoss == next.conductorLoss && before.tile == next.tile
                        && before.type.equals(next.type) && before.routing == next.routing) {
                    conductors.put(point(at), next.conductorLoss, next.faces);
                    if (next.tile instanceof IEnergyConductiveEndpoint) {
                        // The fast path only refreshed the conductor index. A dual tile
                        // keeps its endpoint registration and its port.
                        machines.put(at, next.tile);
                        contactOrder.putEndpoint(point(at));
                    } else {
                        contactOrder.putConductor(point(at), next.conductorLoss, next.faces);
                    }
                    continue;
                }
                contactOrder.remove(point(at));
                if (change.before() != null && change.before().conductorLoss >= 0) {
                    for (Direction side : Direction.values()) {
                        var initial = initialGeneratorContacts.get(at.relative(side));
                        if (initial != null) initial.remove(at);
                    }
                    conductors.remove(point(at));
                    conductorTypes.remove(at);
                    conductorOwners.remove(at);
                    // A null result removes the key, so the map never holds a zero count.
                    conductorChunks.compute(chunk, (key, count) -> count == 1 ? null : count - 1);
                }
                machines.remove(at);
                receiverCursors.remove(at);
                transformerRouting.remove(at);
                transformers.remove(at);
                specialCables.remove(at);
                initialGeneratorContacts.remove(at);
                var after = change.after();
                if (after == null) continue;
                if (after.tile instanceof IndependentSpecialCableBlockEntity cable) specialCables.put(at, cable);
                if (after.conductorLoss >= 0) {
                    conductors.put(point(at), after.conductorLoss, after.faces);
                    conductorTypes.put(at, after.type);
                    conductorOwners.put(at, after.tile);
                    conductorChunks.merge(chunk, 1, Integer::sum);
                    if (after.tile instanceof IEnergyConductiveEndpoint) {
                        // IC2 dual-node layout: the position carries routes to endpoints
                        // beyond it and still owns a port, so its balance keeps charging.
                        machines.put(at, after.tile);
                        contactOrder.putEndpoint(point(at));
                    } else {
                        contactOrder.putConductor(point(at), after.conductorLoss, after.faces);
                    }
                } else if (!(after.tile instanceof IndependentSpecialCableBlockEntity)) {
                    if (machines.size() >= ENDPOINT_LIMIT) {
                        endpointLimitDrops++;
                        if (endpointLimitDrops == 1 || endpointLimitDrops % 1000 == 0)
                            LOG.warn("Endpoint limit {} reached on one level; extra endpoints stay disconnected (drops={})",
                                ENDPOINT_LIMIT, endpointLimitDrops);
                        continue;
                    }
                    machines.put(at, after.tile);
                    contactOrder.putEndpoint(point(at));
                    if (after.tile instanceof IndependentTransformerBlockEntity transformer) {
                        transformerRouting.put(at, after.routing); transformers.put(at, transformer);
                    }
                    if (EnergyNodeRegistry.isGenerator(after.type)) {
                        var initial = new HashSet<BlockPos>();
                        for (Direction side : Direction.values()) {
                            var neighbour = at.relative(side);
                            if (conductors.permitsRegistered(point(neighbour), side.getOpposite().get3DDataValue())) initial.add(neighbour);
                        }
                        initialGeneratorContacts.put(at, initial);
                    }
                }
            }
        }
        /** Releases all state of this grid. A second call has no effect. Do not use the grid after this call. */
        @Override public void close() {
            if (closed) return;
            closed = true;
            nativePlan = null; routingView = null; exportPlan = null; importPlan = null; receiverCursors.clear();
            catchUpEndpoints.clear(); effectEndpoints.clear(); shocks.clear(); includedEndpoints = null; excludedEndpoints = null;
            registered.clear(); foreign.clear(); failedQueries.clear(); samples.clear(); packetSpent.clear(); scanAnchors = List.of();
            entries.close(); conductors.close(); contactOrder.close(); machines.clear(); transformerRouting.clear(); transformers.clear(); specialCables.clear(); specialObserved.clear(); specialPublished.clear(); specialPending.clear(); initialGeneratorContacts.clear(); conductorChunks.clear(); conductorTypes.clear(); conductorOwners.clear(); catchUp = false; effectPauseFrame = -1;
        }
    }
    /**
     * Creates the engine of a server and registers it on the event bus.
     * Only {@link #attach} calls this constructor, on the server thread.
     * Topology limits: 100,000 conductor nodes per level, 32 cached route
     * sources per snapshot, 65,536 queued changes, 4096 work units per tick.
     */
    private IndependentSiEnergy(MinecraftServer server) {
        this.server = server;
        topology = new PlatformTopology(server, EnergyNodeRegistry.conductors(), 100_000, 32, 65_536, 4096, this);
        NeoForge.EVENT_BUS.register(this);
    }
    /**
     * Returns a snapshot of the engine counters.
     *
     * @throws IllegalStateException when the caller is not the thread that
     *         created the engine
     */
    public Metrics metrics() {
        if (Thread.currentThread().threadId() != ownerThread) throw new IllegalStateException("Read engine metrics on server thread");
        return new Metrics(ticks, commits, rejected, debited, credited, dissipated,
            worlds.size(), worlds.values().stream().mapToInt(grid -> grid.machines.size()).sum(), closed, failure, selectionSeed,
            deliveryCount, deliveryWireVisits, fusedWires, destroyedReceivers, blastBlocks,
            totalDebit.fraction(), totalCredit.fraction(), totalLoss.fraction());
    }
    /**
     * Topology callback for a block state change. It reserves a placement slot
     * in {@code ContactOrder} when the new state is controlled, and cancels the
     * slot when it is not. The slot keeps the order in which blocks were placed.
     * The method creates the grid of the level on the first controlled block.
     * It does not publish anything. {@link #position} observes the entry later.
     */
    @Override
    public void blockChanged(ServerLevel level, BlockPos at, BlockState before, BlockState after) {
        boolean recognized = controls(after);
        var grid = worlds.get(level);
        if (grid != null && !grid.failure.isEmpty()) return;
        if (recognized) {
            if (grid == null) { grid = new WorldGrid(selectionSeed ^ 0x5343455848495354L); worlds.put(level, grid); }
            grid.contactOrder.reservePlacement(point(at));
        } else if (grid != null) grid.contactOrder.cancelPlacement(point(at));
    }
    /**
     * Topology callback: observes one position in a FULL chunk.
     *
     * <p>The method builds an {@code Element} for the block entity at
     * {@code at}, and queues it for the next publication frame. It queues
     * {@code null} when the block is no longer an SI node. It never publishes
     * the element itself. {@link #afterLevel} publishes it.
     *
     * <p>The frame is {@code max(ticks + 1, advancedFrame + 1)}. A late
     * observation after the END decision of this tick therefore goes to the
     * next frame, never to a frame that was already published.
     *
     * <p>Special cables go to the special queue ({@code observeSpecial}), all
     * other entries to {@code entries}.
     *
     * @throws IllegalStateException when a controlled block has no SI block
     *         entity of the required kind. The topology then isolates the chunk.
     */
    @Override
    public void position(ServerLevel level, LevelChunk chunk, BlockPos at) {
        var state = chunk.getBlockState(at); var type = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        var loss = controls(state) ? EnergyNodeRegistry.conductorLoss(type) : null; Element element = null;
        var grid = worlds.get(level);
        if (grid != null && !grid.failure.isEmpty()) return;
        // The chunk is already FULL. This can materialize its saved block entity,
        // without loading another chunk or renewing a world lookup ticket.
        if (loss != null || controls(state)) {
            var tile = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.IMMEDIATE);
            // SI also has non-electrical block entities. Ownership, not a registry-name
            // whitelist, determines which ones participate in this network.
            if (tile == null || !(tile instanceof IndependentTransformerBlockEntity)
                    && !(tile instanceof IndependentSpecialCableBlockEntity) && ownedStorage(tile) == null) {
                if (grid != null) {
                    long frame = Math.max(ticks + 1, grid.entries.metrics().advancedFrame() + 1);
                    if (grid.specialObserved.containsKey(at)) grid.observeSpecial(frame, at.immutable(), null);
                    grid.entries.observe(frame, at.immutable(), null);
                }
                return;
            }
            loss = conductorLoss(tile);
            if (EnergyNodeRegistry.specialCable(type) && !(tile instanceof IndependentSpecialCableBlockEntity))
                throw new IllegalStateException("Special conductor lacks independent entity at " + at);
            if (loss == null && !(tile instanceof IndependentTransformerBlockEntity)
                    && (ownedStorage(tile) == null || !ownedStorage(tile).scexNetworkControlled()))
                throw new IllegalStateException("Controlled endpoint lacks the new storage boundary at " + at);
            int routing = 0;
            if (tile instanceof IndependentTransformerBlockEntity transformer) {
                routing = transformer.routingSignature();
            }
            if (tile instanceof IndependentSpecialCableBlockEntity cable) {
                // Sampling a topology entry is read-only. Visible state updates
                // occur at onLoad or the server world tick boundary.
                if (!cable.conductsNow()) loss = -2L;
            }
            element = new Element(type, loss == null ? -1 : loss, tile, routing, conductorFaces(tile));
            if (grid == null) { grid = new WorldGrid(selectionSeed ^ 0x5343455848495354L); worlds.put(level, grid); }
        }
        // A late server-post observation belongs to the next publication frame
        // if the world's END decision has already been made.
        if (grid != null && grid.failure.isEmpty()) {
            long frame = Math.max(ticks + 1, grid.entries.metrics().advancedFrame() + 1);
            if (element != null && element.tile instanceof IndependentSpecialCableBlockEntity || grid.specialObserved.containsKey(at)) {
                grid.observeSpecial(frame, at.immutable(), element != null && element.tile instanceof IndependentSpecialCableBlockEntity ? element : null);
                if (element == null || element.tile instanceof IndependentSpecialCableBlockEntity) return;
            }
            grid.entries.observe(frame, at.immutable(), element);
        }
    }
    /**
     * Topology callback for a chunk unload. It removes all routing state,
     * registrations, foreign contacts, placement slots and catch-up entries of
     * the chunk at once. It also invalidates the accessibility cache.
     * The {@code >> 4} shifts convert block coordinates to chunk coordinates.
     */
    @Override
    public void chunkRemoved(ServerLevel level, int chunkX, int chunkZ) {
        var grid = worlds.get(level);
        if (grid != null && grid.failure.isEmpty()) {
            grid.apply(grid.forget(at -> (at.getX() >> 4) == chunkX && (at.getZ() >> 4) == chunkZ));
            grid.registered.keySet().removeIf(at -> (at.getX() >> 4) == chunkX && (at.getZ() >> 4) == chunkZ);
            grid.foreign.removeIf(contact -> (contact.at.getX() >> 4) == chunkX && (contact.at.getZ() >> 4) == chunkZ);
            grid.contactOrder.forgetPlacements(at -> (at.x() >> 4) == chunkX && (at.z() >> 4) == chunkZ);
            grid.catchUpEndpoints.removeIf(at -> (at.getX() >> 4) == chunkX && (at.getZ() >> 4) == chunkZ);
            grid.catchUp = !grid.catchUpEndpoints.isEmpty();
            grid.accessibilityTick = Long.MIN_VALUE;
        }
    }
    /** Topology callback for a level unload. It closes and removes the grid of the level. */
    @Override public void levelRemoved(ServerLevel level) { var grid = worlds.remove(level); if (grid != null) grid.close(); }
    /** Topology callback for a full reset. It closes and removes all grids. */
    @Override public void cleared() { worlds.values().forEach(WorldGrid::close); worlds.clear(); }
    /**
     * START of a level tick. It runs before the block entities of the level tick.
     *
     * <ol>
     *   <li>Refreshes the input of each special cable in a ticking chunk. A
     *       change queues a topology observation.</li>
     *   <li>Refreshes the mode of each transformer in a ticking chunk. A change
     *       of mode or routing signature queues a topology observation.</li>
     *   <li>When {@code catchUp} is set, settles only {@code catchUpEndpoints}.
     *       These are the endpoints that the previous END paused. The catch-up
     *       gives them the transfer that they missed.</li>
     * </ol>
     * <p>The method clears {@code catchUp} before the settlement. A failed
     * catch-up is therefore not repeated. A {@link RuntimeException} rebuilds
     * the topology of this level only.
     */
    @SubscribeEvent
    public void beforeLevel(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server || closed || !failure.isEmpty()) return;
        var grid = worlds.get(level);
        if (grid == null || !grid.failure.isEmpty()) return;
        for (var item : grid.specialCables.entrySet()) {
            var at = item.getKey(); var cable = item.getValue();
            if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null || cable.isRemoved()
                    || !level.shouldTickBlocksAt(ChunkPos.asLong(at))) continue;
            if (cable.refreshInput()) topology.changed(level, at);
        }
        for (var item : grid.transformers.entrySet()) {
            var transformer = item.getValue();
            var at = item.getKey();
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null || transformer.isRemoved() || !level.shouldTickBlocksAt(ChunkPos.asLong(at))) continue;
            boolean changed = transformer.refreshMode();
            if (changed || !Integer.valueOf(transformer.routingSignature()).equals(grid.transformerRouting.get(at))) topology.changed(level, at);
        }
        if (!grid.catchUp) return;
        grid.catchUp = false;
        try {
            if (!topology.failure().isEmpty()) throw new IllegalStateException(topology.failure());
            settleScoped(level, grid, Set.copyOf(grid.catchUpEndpoints), null);
            grid.catchUpEndpoints.clear();
        } catch (RuntimeException error) { fail(level, grid, error); }
    }
    /**
     * End of a server tick, after all levels ticked. It increments
     * {@code ticks}, which advances the publication frame. A topology failure
     * stops the whole engine here.
     */
    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        if (event.getServer() != server || closed || !failure.isEmpty()) return;
        ticks++;
        if (!topology.failure().isEmpty()) fail(new IllegalStateException(topology.failure()));
    }
    /**
     * END of a level tick. It runs after the block entities of the level ticked.
     * Low priority lets other END listeners run first.
     *
     * <ol>
     *   <li>Samples each detector cable in a ticking chunk.</li>
     *   <li>Takes the changes of frame {@code ticks + 1} from {@code entries}
     *       and from the special cable queue.</li>
     *   <li>No change and no effect pause: settles the full grid.</li>
     *   <li>Otherwise: computes the endpoints near the changes before and after
     *       {@code apply}, and takes the union. These endpoints pause for this
     *       END. The method adds them to {@code catchUpEndpoints}, so the next
     *       START settles them. Then it settles all other endpoints. Separate
     *       circuits are therefore not paused, and they do not get a second
     *       START transfer.</li>
     * </ol>
     * <p>A {@link RuntimeException} rebuilds the topology of this level only.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void afterLevel(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server || closed || !failure.isEmpty()) return;
        var grid = worlds.get(level);
        if (grid == null || !grid.failure.isEmpty() || !topology.operational(level)) return;
        try {
            if (!topology.failure().isEmpty()) throw new IllegalStateException(topology.failure());
            for (var cable : grid.specialCables.values()) {
                var at = cable.getBlockPos();
                if (!cable.isRemoved() && level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) != null
                        && level.shouldTickBlocksAt(ChunkPos.asLong(at))) cable.sampleDetector();
            }
            var changes = new ArrayList<>(grid.entries.advance(ticks + 1));
            changes.addAll(grid.advanceSpecial(ticks + 1));
            // A world effect (fuse, blast) set this frame as its pause frame. Consume it once.
            boolean effectPause = grid.effectPauseFrame == ticks + 1;
            if (effectPause) grid.effectPauseFrame = -1;
            if (changes.isEmpty() && !effectPause) settle(level, grid);
            else {
                var changedPositions = new HashSet<BlockPos>();
                for (var change : changes) changedPositions.add(change.key());
                // Endpoints near the old topology: they lose a connection.
                var affected = affectedEndpoints(grid, changedPositions);
                if (effectPause) { affected.addAll(grid.effectEndpoints); grid.effectEndpoints.clear(); }
                grid.apply(changes);
                // Endpoints near the new topology: they gain a connection.
                affected.addAll(affectedEndpoints(grid, changedPositions));
                // Preserve pause/catch-up for the edited electrical neighbourhood.
                // Independent circuits neither pause nor get duplicate START output.
                grid.catchUpEndpoints.addAll(affected);
                grid.catchUp = !grid.catchUpEndpoints.isEmpty();
                settleScoped(level, grid, null, affected);
            }
        } catch (RuntimeException error) { fail(level, grid, error); }
    }
    /**
     * Stops the whole engine. It records the failure text, logs the error and
     * closes the topology. All event handlers then return at once. Only a
     * server restart starts the engine again. Use this method only for a
     * failure that affects all dimensions.
     */
    private void fail(RuntimeException error) { failure = error.toString(); LOG.error("SI energy engine stopped after an internal error", error); topology.close(); }
    /**
     * Returns the endpoints that a set of changed positions can affect:
     * <ul>
     *   <li>each changed position and its six neighbours</li>
     *   <li>each endpoint with an emitter face that touches a conductor
     *       component that contains a changed position or a neighbour of one</li>
     * </ul>
     * The result uses the current published conductor snapshot. Call it before
     * and after {@code apply} to cover the old and the new topology. Cost:
     * O(changes + endpoints &times; emitters &times; 6).
     *
     * @return a new mutable set. The caller can add to it.
     */
    private static HashSet<BlockPos> affectedEndpoints(WorldGrid grid, Set<BlockPos> changes) {
        var result = new HashSet<BlockPos>(changes);
        var graph = grid.conductors.snapshot(); var components = new HashSet<Integer>();
        for (var at : changes) {
            if (graph.contains(point(at))) components.add(graph.componentOf(point(at)));
            for (var side : Direction.values()) {
                var next = at.relative(side); result.add(next);
                if (graph.contains(point(next))) components.add(graph.componentOf(point(next)));
            }
        }
        for (var entry : grid.machines.entrySet()) {
            for (var emitter : emissionPositions(entry.getValue())) for (var side : Direction.values()) {
                var contact = point(emitter.relative(side));
                if (graph.contains(contact) && components.contains(graph.componentOf(contact))) result.add(entry.getKey());
            }
        }
        return result;
    }
    /**
     * Runs one settlement on a subset of the endpoints.
     *
     * @param included only these endpoints take part, or {@code null} for all
     * @param excluded these endpoints do not take part, or {@code null} for none
     */
    private void settleScoped(ServerLevel level, WorldGrid grid, Set<BlockPos> included, Set<BlockPos> excluded) {
        grid.includedEndpoints = included; grid.excludedEndpoints = excluded;
        try { settle(level, grid); }
        // Always reset the scope, so a failed settlement cannot leak it to the next one.
        finally { grid.includedEndpoints = null; grid.excludedEndpoints = null; }
    }
    /** Tells whether the endpoint at {@code at} is inside the current settlement scope. */
    private static boolean participates(WorldGrid grid, BlockPos at) {
        return (grid.includedEndpoints == null || grid.includedEndpoints.contains(at))
            && (grid.excludedEndpoints == null || !grid.excludedEndpoints.contains(at));
    }
    /** Returns the recovery counters of the topology. */
    public PlatformTopology.RecoveryMetrics topologyRecoveryMetrics() { return topology.recoveryMetrics(); }
    /**
     * Isolates a failure to one dimension. It logs the error, drops the pending
     * catch-up and asks the topology to rebuild the level. Other dimensions
     * continue. The rebuild observes all positions again, so the grid recovers
     * without a restart.
     */
    private void fail(ServerLevel level, WorldGrid grid, RuntimeException error) {
        com.mojang.logging.LogUtils.getLogger().error("SI energy rebuilding {} after an isolated failure", level.dimension().location(), error);
        grid.catchUp = false;
        topology.rebuild(level, error.toString());
    }
    /**
     * Tells whether {@code grid} is still the live grid of a live level of this
     * engine. A {@code false} result means that a callback unloaded the level
     * or closed the engine. The caller must then stop at once.
     */
    private boolean currentWorld(ServerLevel level, WorldGrid grid) {
        return !closed && grid.failure.isEmpty() && server != null
            && worlds.get(level) == grid && server.getLevel(level.dimension()) == level;
    }
    /**
     * Tells whether the grid can transfer energy now, and updates the active
     * flag of each conductor chunk.
     *
     * <p>A conductor chunk is active when it is loaded, ticks blocks and is ready
     * in the topology. Routes through an inactive chunk are not used, but the
     * published conductors stay. An inactive chunk does not stop the full
     * dimension.
     *
     * <p>The result is cached for one server tick and one topology readiness
     * revision. Code that adds or removes a conductor sets
     * {@code accessibilityTick} to {@code Long.MIN_VALUE} to force a new check.
     *
     * @return {@code false} when {@link #currentWorld} is false or the topology
     *         of the level is not operational, else {@code true}
     */
    private boolean accessible(ServerLevel level, WorldGrid grid) {
        if (!currentWorld(level, grid) || !topology.operational(level)) return false;
        long tick = server.getTickCount();
        long readiness = topology.readinessRevision();
        if (grid.accessibilityTick == tick && grid.accessibilityRevision == readiness) return grid.accessibilityResult;
        // Chunk accessibility revokes routes without deleting the published wire history.
        // An unrelated inactive component must not pause this entire dimension.
        // No branch sets this value to false. Per-chunk state goes to setChunkActive.
        boolean result = true;
        for (long chunk : grid.conductorChunks.keySet()) {
            boolean available = level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null
                && level.shouldTickBlocksAt(chunk) && topology.chunkReady(level, chunk);
            grid.conductors.setChunkActive(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), available);
        }
        grid.accessibilityResult = result;
        grid.accessibilityTick = tick;
        grid.accessibilityRevision = readiness;
        return result;
    }
    /**
     * Runs one full settlement of a grid: native EU, then FE export, then FE
     * import. The current settlement scope ({@code participates}) applies.
     *
     * <p>The per-tick maps ({@code samples}, {@code packetSpent},
     * {@code failedOutputs}, {@code transformerReceived},
     * {@code exportedThisTick}) are cleared only on the first settlement of a
     * server tick. A START catch-up and the END settlement of one tick therefore
     * share them. The FE export pass reads {@code packetSpent}, so FE export
     * cannot exceed the packet limit of one tick.
     *
     * <p>The three passes share one {@code allowances} map. The native pass fills
     * it. The export pass debits it. Each settlement creates a new map. A START
     * catch-up therefore gives the paused endpoints the allowance of the tick
     * that they missed.
     *
     * <p>Shock damage is applied in the {@code finally} block. Damage that a pass
     * collected before an exception is still applied.
     */
    private void settle(ServerLevel level, WorldGrid grid) {
        if (grid.sampleTick != server.getTickCount()) {
            grid.sampleTick = server.getTickCount(); grid.samples.clear(); grid.packetSpent.clear(); grid.failedOutputs.clear(); grid.transformerReceived.clear(); grid.exportedThisTick.clear();
        }
        grid.shocks.clear();
        long started = System.nanoTime();
        var allowances = new IdentityHashMap<BlockEntity, PacketAllowance>();
        try {
            settle(level, grid, true, allowances);
            settleForeign(level, grid, allowances);
            importForeign(level, grid);
        } finally {
            grid.shocks.apply((entity, damage) -> {
                if (entity.isAlive()) entity.hurt(level.damageSources().lightningBolt(), damage);
            });
            grid.timings.add(server.getTickCount(), System.nanoTime() - started);
        }
    }
    /**
     * The native EU pass. It settles windows of sources and sinks until every
     * source had its turn, a budget is empty, or 128 batches ran.
     *
     * <p><b>One batch:</b>
     * <ol>
     *   <li>Get the port lists. Batch 0 builds them and freezes one
     *       {@link PacketAllowance} per source. A later batch re-quotes only the
     *       ports that the last commit wrote ({@code refreshInPlace}). It falls
     *       back to a full re-quote when this is not possible.</li>
     *   <li>Check the routing view. When the routing identity changed,
     *       increment {@code portsGeneration}. Old plans are then invalid.</li>
     *   <li>Resume {@code grid.nativePlan}, or build a new plan from the first
     *       source at {@code nativeCursor} that was not visited.</li>
     *   <li>Skip a window that this settlement already settled, or whose sources
     *       offer nothing.</li>
     *   <li>Advance the plan. Return when the search budget is empty. The plan
     *       stays in the grid and resumes on the next tick.</li>
     *   <li>Settle the window ({@code settlePlan}). Advance the cursors.</li>
     * </ol>
     *
     * <p><b>visited</b> holds the sources that are done in this settlement. A
     * source is done when its allowance is empty or when the window covered all
     * its reachable sinks. The pass stops when every source is visited.
     *
     * <p>Callers must not reuse {@code allowances} across ticks. A source would
     * then get a second allowance.
     *
     * @param prepareDemand {@code true} lets demand sources acquire fuel for a
     *        routable demand
     * @param allowances output: one frozen allowance per source tile
     */
    private void settle(ServerLevel level, WorldGrid grid, boolean prepareDemand,
            IdentityHashMap<BlockEntity, PacketAllowance> allowances) {
        if (!accessible(level, grid)) return;
        grid.nativeSearch.reset(server.getTickCount());
        grid.routeBudget.reset(server.getTickCount());
        var visited = new HashSet<BlockPos>();
        var windows = new HashSet<NativeWindowKey>();
        // Port lists are built once per tick. A commit only changes the balances
        // of its own window, so afterwards only those ports are re-quoted in
        // place (O(window)), instead of re-quoting every machine in the
        // dimension and rebuilding all lists for each of up to 128 batches.
        ArrayList<Port> cachedPorts = null;
        ArrayList<Port> cachedSources = null;
        ArrayList<Port> cachedSinks = null;
        HashMap<BlockPos, Port> cachedByPosition = null;
        HashMap<BlockPos, Integer> portIndex = null, sourceIndex = null, sinkIndex = null;
        long cachedAtCommits = -1, cachedAtEdits = -1;
        boolean rebuildLists = true, checkView = true;
        ConductorRegistry.Snapshot lastSnapshot = null;
        // Freeze each source offer once. Receiver windows and source groups
        // rotate independently. Neither can renew a source's output allowance.
        for (int batch = 0; batch < 128; batch++) {
            if (cachedPorts == null) {
                cachedPorts = new ArrayList<>(ports(level, grid, prepareDemand));
                for (var port : cachedPorts) if (port.outputs != 0)
                    allowances.put(port.tile, new PacketAllowance(port.policy(), port.offered()));
                rebuildLists = true;
            } else if (grid.balanceWrites != cachedAtCommits) {
                // A balance write happened since the cache was built. An endpoint
                // edit, or a write with no recorded positions, forces a full re-quote.
                if (grid.endpointEdits != cachedAtEdits || grid.lastCommitted.isEmpty()
                        || !refreshInPlace(level, grid, prepareDemand, grid.lastCommitted, cachedPorts, cachedSources, cachedSinks,
                            cachedByPosition, portIndex, sourceIndex, sinkIndex)) {
                    cachedPorts = new ArrayList<>(refreshQuotes(level, grid, cachedPorts, prepareDemand));
                    rebuildLists = true;
                }
            }
            grid.lastCommitted.clear();
            cachedAtCommits = grid.balanceWrites;
            cachedAtEdits = grid.endpointEdits;
            if (rebuildLists) {
                cachedSources = new ArrayList<>(); cachedSinks = new ArrayList<>();
                cachedByPosition = new HashMap<>(); portIndex = new HashMap<>(); sourceIndex = new HashMap<>(); sinkIndex = new HashMap<>();
                for (int i = 0; i < cachedPorts.size(); i++) {
                    var port = cachedPorts.get(i);
                    cachedByPosition.put(port.position, port); portIndex.put(port.position, i);
                    if (isRoutedSource(port)) { sourceIndex.put(port.position, cachedSources.size()); cachedSources.add(port); }
                    if (port.inputs != 0) { sinkIndex.put(port.position, cachedSinks.size()); cachedSinks.add(port); }
                }
                rebuildLists = false; checkView = true;
            }
            var ports = cachedPorts;
            var allSources = cachedSources;
            var allSinks = cachedSinks;
            // Early return before any modulo: the cursor math below divides by allSources.size().
            if (allSources.isEmpty() || allSinks.isEmpty()) { grid.nativePlan = null; return; }
            var snapshot = grid.conductors.snapshot();
            if (snapshot != lastSnapshot) { lastSnapshot = snapshot; checkView = true; }
            // Balances change every tick. Routing identities usually do not.
            // Preserve incomplete plans until topology, directions or owners change.
            // The O(ports) comparison runs only when the lists or topology changed.
            if (checkView) {
                if (grid.routingView == null || !grid.routingView.matches(snapshot, allSources, allSinks)) {
                    grid.portsGeneration++;
                    var sourceIds = allSources.stream().map(RouteEndpoint::of).toList();
                    var sinkIds = allSinks.stream().map(RouteEndpoint::of).toList();
                    var contacts = new ReceiverContacts(snapshot, sinkIds.stream()
                        .map(p -> new ReceiverContacts.Endpoint(point(p.position), p.inputs)).toList());
                    grid.routingView = new RoutingView(snapshot, sourceIds, sinkIds, contacts, grid.portsGeneration);
                }
                checkView = false;
            }
            var plan = grid.nativePlan;
            // Shape guard: drop a plan whose window does not fit inside the live lists.
            if (plan != null) {
                int planSrcEnd = plan.firstSource + plan.sources.size();
                int planSnkEnd = plan.firstReceiver + plan.sinks.size();
                if (plan.allSources.size() != allSources.size()
                        || plan.allSinks.size() != allSinks.size()
                        || planSrcEnd > allSources.size()
                        || planSnkEnd > allSinks.size()) {
                    plan = null; grid.nativePlan = null;
                }
            }
            // A plan that contains a finished source would settle that source twice.
            if (plan != null && plan.sources.stream().anyMatch(p -> visited.contains(p.position))) plan = null;
            if (plan == null || plan.portsGeneration != grid.portsGeneration || plan.graph != snapshot) {
                // floorMod maps any stored cursor (also a negative or stale one) into [0, size).
                int start = Math.floorMod(grid.nativeCursor, allSources.size());
                int skipped = 0;
                // Find the first source that is not visited. Return when all are visited.
                while (visited.contains(allSources.get(start).position)) {
                    if (++skipped == allSources.size()) return;
                    start = (start + 1) % allSources.size();
                }
                plan = new NativeRoutePlan(snapshot, allSources, allSinks, start, grid);
                grid.plansBuilt++;
                grid.nativePlan = plan;
            } else if (plan.originIndex < plan.origins.size()) {
                grid.plansResumed++;
            }
            var windowKey = new NativeWindowKey(grid.portsGeneration, plan.sources.getFirst().tile, plan.firstReceiver);
            // This window was already settled in this settlement: the receiver cursor
            // wrapped. Mark its sources as done and move to the next source group.
            if (windows.contains(windowKey)) {
                visited.addAll(plan.sources.stream().map(RouteEndpoint::position).toList());
                grid.nativeCursor = plan.nextSource; grid.nativePlan = null;
                continue;
            }
            boolean offering = false;
            // The plan window is quoted from its own generation. Clamp to the
            // live list so a generation mismatch degrades to a skipped window
            // instead of an IndexOutOfBounds that halts the whole grid.
            int offeredEnd = Math.min(plan.firstSource + plan.sources.size(), allSources.size());
            for (int i = plan.firstSource; i < offeredEnd; i++) {
                var source = allSources.get(i);
                offering |= !windowOffer(source, allowances, plan.packetLimit).isZero();
            }
            if (!offering) {
                visited.addAll(plan.sources.stream().map(RouteEndpoint::position).toList());
                grid.nativeCursor = plan.nextSource; grid.nativePlan = null;
                if (visited.size() == allSources.size()) return;
                continue;
            }
            // Budget empty: keep grid.nativePlan as it is, so the next tick resumes at originIndex.
            if (!plan.advance(grid.nativeSearch, grid.routeBudget)) { grid.budgetYields++; return; }
            // Reserve the eligibility and priority vectors (including copies)
            // before finalizing, not after performing an unbounded allocation.
            if (!grid.routeBudget.reserveArrays(plan.arrayCells * 3)) return;
            if (!settlePlan(level, grid, prepareDemand, ports, plan, cachedByPosition, allowances)) return;
            windows.add(windowKey);
            // The sources are done when every allowance is empty, or when this
            // window started at sink 0 and covered all reachable sinks (or no sink
            // is reachable: then firstReceiver == nextReceiver == 0).
            boolean exhausted = true;
            for (var endpoint : plan.sources) {
                var allowance = allowances.get(endpoint.tile);
                exhausted &= allowance == null || allowance.remaining().isZero();
            }
            if (exhausted || plan.nextReceiver == plan.firstReceiver)
                visited.addAll(plan.sources.stream().map(RouteEndpoint::position).toList());
            grid.receiverCursors.put(plan.sources.getFirst().position, plan.nextReceiver);
            grid.nativeCursor = plan.nextSource;
            // Keep a complete single-window plan hot. A multi-window cycle
            // releases old paths before admitting another working set.
            if (plan.nextSource != plan.firstSource || plan.nextReceiver != plan.firstReceiver)
                grid.nativePlan = null;
            if (visited.size() == allSources.size()) return;
            if (!accessible(level, grid)) return;
        }
    }
    /** A port is a source when it has an output face, a packet size above 0 and a packet count above 0. */
    private static boolean isRoutedSource(Port port) { return port.outputs != 0 && port.packet > 0 && port.packets > 0; }

    /**
     * Re-quotes only the ports a commit touched and patches them into every cached
     * list. Returns false (caller falls back to a full refresh) if a port dropped
     * out, or its list membership or routing identity changed.
     *
     * <p>The method is all or nothing. It checks every touched port first, and
     * changes the lists only when all checks pass. A {@code false} result leaves
     * the lists unchanged. The indexes of the lists do not change, so the
     * routing view and the plan stay valid.
     */
    private boolean refreshInPlace(ServerLevel level, WorldGrid grid, boolean prepareDemand, Set<BlockPos> touched,
            ArrayList<Port> ports, ArrayList<Port> sources, ArrayList<Port> sinks, HashMap<BlockPos, Port> byPosition,
            HashMap<BlockPos, Integer> portIndex, HashMap<BlockPos, Integer> sourceIndex, HashMap<BlockPos, Integer> sinkIndex) {
        if (ports == null || portIndex == null) return false;
        var replacements = new ArrayList<Port>(touched.size());
        for (var at : touched) {
            Integer index = portIndex.get(at);
            if (index == null) return false;
            var old = ports.get(index);
            var fresh = refreshQuotes(level, grid, List.of(old), prepareDemand);
            if (fresh.size() != 1) return false;
            var next = fresh.getFirst();
            if (isRoutedSource(old) != isRoutedSource(next) || (old.inputs != 0) != (next.inputs != 0)
                    || !RouteEndpoint.of(old).matches(next)) return false;
            replacements.add(next);
        }
        for (var next : replacements) {
            var at = next.position;
            ports.set(portIndex.get(at), next);
            byPosition.put(at, next);
            Integer source = sourceIndex.get(at); if (source != null) sources.set(source, next);
            Integer sink = sinkIndex.get(at); if (sink != null) sinks.set(sink, next);
        }
        return true;
    }

    /**
     * Returns the EU that a source can offer to one window: the remaining
     * frozen allowance, limited by the live offer and by
     * {@code min(packetLimit, packet count)} packets.
     *
     * @return {@link EnergyAmount#ZERO} when the source has no allowance, or
     *         when its packet policy differs from the frozen policy
     */
    private static EnergyAmount windowOffer(Port port, IdentityHashMap<BlockEntity, PacketAllowance> allowances, int packetLimit) {
        var allowance = allowances.get(port.tile);
        return allowance == null ? EnergyAmount.ZERO : allowance.quote(port.policy(), port.offered(), packetLimit);
    }
    /**
     * Settles one finished native window: allocates energy, commits the new
     * balances, and applies world effects.
     *
     * <p><b>Steps:</b>
     * <ol>
     *   <li>Map the window to live ports. Return {@code true} with no transfer
     *       when a port changed its routing identity or the conductor snapshot
     *       is not current.</li>
     *   <li>Build the distribution input: sink room (EU below capacity), source
     *       balances and packet policies, window offers ({@code windowOffer}),
     *       and one domain per conductor component ({@code domains}).</li>
     *   <li>Allocate with {@link FractionalDistributor}. The result gives one
     *       debit per source, one credit per sink and the conductor loss.</li>
     *   <li>Demand sources (with {@code prepareDemand}): when the debit of a
     *       {@link DemandEnergySource} is above its stored EU, the source makes
     *       fuel into EU first ({@code prepareEnergy}). Then the method re-quotes
     *       the window and calls itself once with {@code prepareDemand == false}.
     *       The second call settles only real stored EU.</li>
     *   <li>Reserve the path visits for the effect scan and the meter samples.
     *       Return {@code false} when the budget cannot pay. Nothing is written
     *       then.</li>
     *   <li>Trace each delivery along its path. Collect melt, insulation, shock
     *       and sink explosion candidates. Check that the trace totals equal the
     *       allocation totals.</li>
     *   <li>Commit all balance writes and the output budget debits in one atomic
     *       call. The commit runs only when {@code valid} confirms the window.</li>
     *   <li>After a successful commit only: debit the allowances, record meter
     *       samples, notify conductors and detectors, update counters, and apply
     *       world effects.</li>
     * </ol>
     *
     * <p><b>Conservation:</b> the sum of source debits equals the sum of sink
     * credits plus {@code round.dissipated()}. A mismatch throws
     * {@link IllegalStateException}. The commit applies all writes or none.
     *
     * @return {@code true} when the window is finished (with or without a
     *         transfer). {@code false} when a tick budget is empty. The caller
     *         must then stop the pass for this tick.
     */
    private boolean settlePlan(ServerLevel level, WorldGrid grid, boolean prepareDemand,
            List<Port> ports, NativeRoutePlan plan, HashMap<BlockPos, Port> byPosition,
            IdentityHashMap<BlockEntity, PacketAllowance> allowances) {
        var sources = plan.sources.stream().map(p -> byPosition.get(p.position)).toList();
        var sinks = plan.sinks.stream().map(p -> byPosition.get(p.position)).toList();
        if (!plan.matchesSelected(sources, sinks) || !grid.conductors.isCurrent(plan.graph)) return true;
        // Only this window's ports are read or written by this settlement, so
        // validation and writes are scoped to them (was: every port in the world,
        // where one unrelated stale quote rejected the whole window).
        var involvedSet = new java.util.LinkedHashSet<Port>(sources); involvedSet.addAll(sinks);
        final List<Port> involved = List.copyOf(involvedSet);
        var snapshot = plan.graph; var room = new ArrayList<EnergyAmount>();
        int[] receivers = new int[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) { room.add(sinks.get(i).quote.exactAmount().roomBelow(sinks.get(i).capacity)); receivers[i] = i; }
        var quotes = sources.stream().map(source -> new DomainDistributor.Source(source.balance().whole(), source.packet,
            source.partialPackets, source.packets)).toList();
        var budgets = sources.stream().map(source -> windowOffer(source, allowances, plan.packetLimit)).toList();
        var effectPaths = new ArrayList<List<ConductorRegistry.Path[]>>();
        var domains = domains(plan, sources, sinks, snapshot, receivers, level.getGameTime(), grid, effectPaths, budgets);
        var shared = new ArrayList<DomainDistributor.SharedStorage>();
        var sinkIndices = new IdentityHashMap<Port, Integer>();
        for (int i = 0; i < sinks.size(); i++) sinkIndices.put(sinks.get(i), i);
        // A port that is a source and a sink in this window (for example, a
        // storage block) shares one balance. The distributor must not let it
        // receive energy that it sends in the same round.
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i); var receiver = sinkIndices.get(source);
            if (source.potential == null && receiver != null)
                shared.add(new DomainDistributor.SharedStorage(i, receiver, source.capacity));
        }
        var round = FractionalDistributor.allocateQuotedTraced(quotes, sources.stream().map(Port::balance).toList(),
            domains, receivers, room, shared, budgets, selectionRandom);
        // Net balance change per port, in EnergyAmount units. A port that is a
        // source and a sink gets one merged delta.
        var deltas = new IdentityHashMap<Port, java.math.BigInteger>();
        var outputDebits = new ArrayList<FeLedger.OutputDebit>();
        EnergyAmount loss = round.dissipated(), debit = EnergyAmount.ZERO, credit = EnergyAmount.ZERO;
        for (int i = 0; i < sources.size(); i++) {
            var value = round.debit(i); debit = debit.add(value);
            deltas.merge(sources.get(i), value.units().negate(), java.math.BigInteger::add);
            if (!value.isZero() && sources.get(i).outputBudget != null)
                outputDebits.add(new FeLedger.OutputDebit(sources.get(i).outputBudget, value));
        }
        for (int receiver = 0; receiver < sinks.size(); receiver++) {
            var value = round.credit(receiver); credit = credit.add(value);
            deltas.merge(sinks.get(receiver), value.units(), java.math.BigInteger::add);
        }
        // Nothing to move: the window is finished without a write.
        if (debit.isZero()) return true;
        // External HU is acquired only for a routable, nonzero demand. It is credited to owned EU first.
        // A fresh bounded second plan then settles only actual owned balances, never simulated fuel.
        boolean acquired = false;
        if (prepareDemand) {
            if (!valid(level, grid, snapshot, involved)) { rejected++; return true; }
            for (int i = 0; i < sources.size(); i++) {
                var source = sources.get(i);
                if (source.tile instanceof DemandEnergySource demand && round.debit(i).compareTo(source.quote.exactAmount()) > 0) {
                    demand.prepareEnergy(round.debit(i)); acquired = true;
                }
            }
            if (acquired) {
                // Acquisition already changed owned balances: cached quotes of these
                // ports are stale whether or not the follow-up commit happens.
                grid.balanceWrites++;
                for (var port : involved) grid.lastCommitted.add(port.position);
                if (grid.routeBudget.reserveArrays(plan.arrayCells * 3)) {
                    var requoted = refreshQuotes(level, grid, involved, false);
                    var repositioned = new HashMap<BlockPos, Port>();
                    for (var port : requoted) repositioned.put(port.position, port);
                    // Recursion depth is 1: the second call has prepareDemand == false.
                    return settlePlan(level, grid, false, requoted, plan, repositioned, allowances);
                }
                // The acquired EU stays in the owned storage. A later window or tick can send it.
                return false;
            }
        }
        // Each delivery visits its path twice: once for effects, once for meter samples.
        long pathWork = 0;
        for (var delivery : round.deliveries()) {
            var path = effectPaths.get(delivery.domain()).get(delivery.entry())[delivery.receiver()];
            if (path != null) pathWork += 2L * path.vertexCount();
        }
        // No debit or world effect is committed until BOTH path passes fit.
        // The constructor limits each window so it can fit a fresh tick.
        if (!grid.routeBudget.reserveVisits(pathWork, 0)) return false;
        EnergyAmount traceDebit = EnergyAmount.ZERO, traceCredit = EnergyAmount.ZERO, traceLoss = EnergyAmount.ZERO;
        long[] wireVisits = {0};
        var fusePlan = new LinkedHashMap<BlockPos, ResourceLocation>();
        var stripPlan = new LinkedHashMap<BlockPos, ResourceLocation>();
        var receiverPlan = new LinkedHashMap<Port, Double>();
        var poweredConductors = new java.util.LinkedHashSet<IEnergyConductor>();
        var detectorDeliveries = new HashSet<IndependentSpecialCableBlockEntity>();
        var shockPlan = new LinkedHashMap<ShockRoute, Map<BlockPos, Double>>();
        for (var delivery : round.deliveries()) {
            traceDebit = traceDebit.add(delivery.sourceDebit());
            traceCredit = traceCredit.add(delivery.credit());
            traceLoss = traceLoss.add(delivery.pathLoss());
            var path = effectPaths.get(delivery.domain()).get(delivery.entry())[delivery.receiver()];
            double[] weakestMeasuredLimit = {Double.POSITIVE_INFINITY};
            // Effects use the largest actual packet on the path, including loss.
            // A source's tier only caps its offer. A partial delivery is not a full packet.
            double deliveryVoltage = delivery.sourceDebit().toDouble();
            if (path == null) {
                if (!delivery.pathLoss().isZero()) throw new IllegalStateException("Direct delivery has conductor loss");
            } else {
                if (!com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyLoss.fromMilli(path.lossMilli()).equals(delivery.pathLoss())) throw new IllegalStateException("Delivery path and quoted loss differ");
                path.visit(position -> {
                    wireVisits[0] = Math.incrementExact(wireVisits[0]);
                    var at = new BlockPos(position.x(), position.y(), position.z());
                    var type = grid.conductorTypes.get(at);
                    if (type == null) throw new IllegalStateException("Published conductor has no material identity");
                    var special = grid.specialCables.get(at);
                    if (special != null && special.detector()) detectorDeliveries.add(special);
                    Long conductorLimit = tierFuseLimit(type);
                    if (conductorLimit == null) conductorLimit = EnergyNodeRegistry.fuseLimit(type);
                    if (conductorLimit != null) {
                        weakestMeasuredLimit[0] = Math.min(weakestMeasuredLimit[0], conductorLimit);
                        if (IndependentEnergyMode.enetCableMeltdown() && deliveryVoltage > conductorLimit) {
                            fusePlan.put(at, type);
                        }
                    }
                    var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                    if (chunk != null) {
                        var be = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                        if (be instanceof IEnergyConductor conductor) {
                            poweredConductors.add(conductor);
                            weakestMeasuredLimit[0] = Math.min(weakestMeasuredLimit[0], conductor.getInsulationBreakdownEnergy());
                            if (IndependentEnergyMode.enetCableMeltdown() && !fusePlan.containsKey(at) && deliveryVoltage > conductor.getInsulationBreakdownEnergy()) {
                                stripPlan.put(at, type);
                            }
                            if (IndependentEnergyMode.enetShockDamage() && deliveryVoltage > conductor.getInsulationEnergyAbsorption()) {
                                double shockEnergy = deliveryVoltage - conductor.getInsulationEnergyAbsorption();
                                shockPlan.computeIfAbsent(new ShockRoute(sources.get(delivery.source()).position,
                                    sinks.get(delivery.receiver()).position), ignored -> new LinkedHashMap<>())
                                    .merge(at, shockEnergy, Math::max);
                            }
                        }
                    }
                });
            }
            var sink = sinks.get(delivery.receiver());
            var receiverType = BuiltInRegistries.BLOCK.getKey(sink.state.getBlock());
            Long receiverLimit = tierReceiverLimit(sink.tile);
            if (receiverLimit == null) receiverLimit = EnergyNodeRegistry.storagePacket(receiverType);
            if (sink.transformer != null) receiverLimit = Long.valueOf(sink.transformer.limits().inputLimit());
            else if (receiverLimit == null && EnergyNodeRegistry.standardProcessor(receiverType)) receiverLimit = Long.valueOf(sink.quote.maxReceive());
            // weakestMeasuredLimit holds the lowest conductor breakdown limit and the
            // lowest insulation breakdown energy on the path. A delivery above it does
            // not explode the sink. This test does not depend on enetCableMeltdown().
            boolean fused = deliveryVoltage > weakestMeasuredLimit[0];
            if (IndependentEnergyMode.enetExplosions() && receiverLimit != null && !fused
                    && deliveryVoltage > receiverLimit) {
                receiverPlan.merge(sink, deliveryVoltage, Math::max);
            }
        }
        if (!traceDebit.equals(debit) || !traceCredit.equals(credit) || !traceLoss.equals(loss))
            throw new IllegalStateException("Delivery trace does not reconcile with the round");
        // Transformers store EU in a NetworkCell. All other ports use their owned storage.
        // A transformer always gets a write, also with a zero delta.
        var writes = new ArrayList<CustomEUEnergyStorage.NetworkWrite>();
        var additional = new ArrayList<NetworkCell.Write>();
        for (var port : involved) {
            var delta = deltas.getOrDefault(port, java.math.BigInteger.ZERO);
            var next = EnergyAmount.fromUnits(port.quote.exactAmount().units().add(delta));
            if (port.transformer != null) additional.add(new NetworkCell.Write(port.transformer.energy(), next));
            else if (delta.signum() != 0) writes.add(new CustomEUEnergyStorage.NetworkWrite(port.storage, port.quote, next));
        }
        if (FeLedger.commitNativeOutput(outputDebits, budgetCurrent -> CustomEUEnergyStorage.scexCommitNetwork(
                writes, additional, loss, () -> valid(level, grid, snapshot, involved) && budgetCurrent.getAsBoolean()))) {
            // The commit succeeded. Only from here may the method spend allowances or change the world.
            for (int i = 0; i < sources.size(); i++) {
                var allowance = allowances.get(sources.get(i).tile);
                if (allowance != null) allowance.debit(round.debit(i));
            }
            for (var delivery : round.deliveries()) {
                var source = sources.get(delivery.source());
                var sink = sinks.get(delivery.receiver());
                double sent = delivery.sourceDebit().toDouble(), received = delivery.credit().toDouble();
                double voltage = sent;
                sample(grid, source.position, 0, received, voltage);
                sample(grid, sink.position, received, 0, voltage);
                if (sink.transformer != null) grid.transformerReceived.merge(sink.position, delivery.credit(), EnergyAmount::add);
                grid.packetSpent.merge(source.position, delivery.sourceDebit(), EnergyAmount::add);
                var path = effectPaths.get(delivery.domain()).get(delivery.entry())[delivery.receiver()];
                if (path != null) path.visit(at -> sample(grid, new BlockPos(at.x(), at.y(), at.z()), received, received, voltage));
            }
            poweredConductors.forEach(IEnergyConductor::onEnergyPass);
            detectorDeliveries.forEach(IndependentSpecialCableBlockEntity::delivered);
            for (var port : involved) if (port.transformer != null && deltas.getOrDefault(port, java.math.BigInteger.ZERO).signum() != 0)
                ((IndependentTransformerBlockEntity) port.tile).markNetworkChanged();
            commits++; grid.balanceWrites++;
            for (var port : involved) grid.lastCommitted.add(port.position);
            totalDebit = totalDebit.add(debit); totalCredit = totalCredit.add(credit); totalLoss = totalLoss.add(loss);
            debited = totalDebit.whole(); credited = totalCredit.whole(); dissipated = totalLoss.whole();
            deliveryCount = Math.addExact(deliveryCount, round.deliveries().size());
            deliveryWireVisits = Math.addExact(deliveryWireVisits, wireVisits[0]);
            var removedByEffects = new HashSet<BlockPos>();
            var effectPositions = new HashSet<BlockPos>(fusePlan.keySet()); effectPositions.addAll(stripPlan.keySet());
            receiverPlan.keySet().forEach(port -> effectPositions.add(port.position));
            if (!effectPositions.isEmpty()) grid.effectEndpoints.addAll(affectedEndpoints(grid, effectPositions));
            shockPlan.forEach((route, exposures) -> collectShockDamage(level, grid, route, exposures));
            removedByEffects.addAll(fuseConductors(level, grid, fusePlan));
            stripPlan.keySet().removeAll(fusePlan.keySet());
            removedByEffects.addAll(stripInsulation(level, grid, stripPlan));
            for (var overload : receiverPlan.entrySet()) {
                var sink = overload.getKey();
                var at = sink.position;
                var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                if (chunk == null || chunk.getBlockEntity(at) != sink.tile || !chunk.getBlockState(at).equals(sink.state)) continue;
                int overloadTier = EnergyNetGlobal.getTierFromPower(overload.getValue());
                // An overload handler that returns true handles the overload itself. No explosion follows.
                if (sink.tile instanceof IOverloadHandler handler && handler.onOverload(overloadTier)) continue;
                // The check above already proved realBe == sink.tile, so the realBe branches
                // below run only for the same object.
                var realBe = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                if (realBe != null && realBe != sink.tile && realBe instanceof IOverloadHandler handler && handler.onOverload(overloadTier)) continue;
                // Note: no code below reads explosionPower. The blast uses fixed small-blast rules.
                // Only shouldExplode() of an override has an effect.
                float explosionPower = 2.5F;
                if (sink.tile instanceof IExplosionPowerOverride override) {
                    if (!override.shouldExplode()) continue;
                    explosionPower = override.getExplosionPower(overloadTier, explosionPower);
                } else if (realBe != null && realBe instanceof IExplosionPowerOverride override) {
                    if (!override.shouldExplode()) continue;
                    explosionPower = override.getExplosionPower(overloadTier, explosionPower);
                }
                if (level.removeBlock(at, false)) {
                    grid.endpointEdits++;
                    grid.machines.remove(at);
                    grid.contactOrder.remove(point(at));
                    grid.initialGeneratorContacts.remove(at);
                    topology.changed(level, at);
                    removedByEffects.add(at);
                    destroyedReceivers = Math.incrementExact(destroyedReceivers);
                    double cx = at.getX() + 0.5, cy = at.getY() + 0.5, cz = at.getZ() + 0.5;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, cx, cy, cz, 20, 0.2, 0.2, 0.2, 0.05);
                    level.playSound(null, at, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.2F, 1.0F);
                    applySmallEntityBlast(level, at);
                    applySmallBlockBlast(level, grid, at, removedByEffects);
                    com.miophas.singularity_iteration.core.api.advancement.MioAchievements
                        .at(level, at, "industrial_accident", "machine_exploded");
                }
            }
            if (!removedByEffects.isEmpty()) {
                // Our own completed removals are already known this frame.
                // Retire them together so a later observer echo cannot delay
                // the measured next-END pause by an additional frame.
                grid.apply(grid.forget(removedByEffects::contains));
                // A blast during a START catch-up still schedules the next
                // frame's END pause. Consuming a boolean in this same frame's
                // END had made later independent inputs one tick too early.
                grid.effectPauseFrame = ticks + 2;
            }
        } else rejected++;
        return true;
    }
    /**
     * Builds one {@code Port} for each published endpoint in the settlement
     * scope.
     *
     * <p>An endpoint is skipped when its chunk is not loaded, does not tick
     * blocks or is not ready, when its tile was removed or replaced, or (for a
     * transformer) when its mode is not valid or its live routing signature is
     * not the published one. A new transformer mode therefore takes effect only
     * after the same delayed publication as other electrical edits.
     *
     * <p>Failure isolation: an exception for one endpoint isolates its chunk.
     * The final {@code removeIf} drops all ports of chunks that were isolated
     * during the loop.
     *
     * <p>The loop iterates a copy of {@code machines}, because a tile callback
     * can change the map.
     *
     * @param prepareDemand {@code true} quotes the potential energy of demand sources
     * @return a new list in {@code machines} order
     */
    private List<Port> ports(ServerLevel level, WorldGrid grid, boolean prepareDemand) {
        var ports = new ArrayList<Port>();
        for (var entry : List.copyOf(grid.machines.entrySet())) {
            try {
            BlockPos at = entry.getKey(); var tile = entry.getValue();
            if (!participates(grid, at)) continue;
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null || !level.shouldTickBlocksAt(ChunkPos.asLong(at)) || !topology.chunkReady(level, ChunkPos.asLong(at)) || tile.isRemoved()
                || chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK) != tile) continue;
            if (tile instanceof IndependentTransformerBlockEntity transformer) {
                if (!transformer.validMode()) continue;
                // Visible mode changes revoke old quotes immediately. The new sides
                // join only after the same delayed publication as electrical edits.
                if (!Integer.valueOf(transformer.routingSignature()).equals(grid.transformerRouting.get(at))) continue;
                var offered = transformer.snapshot(transformer.stepUpNow());
                if (offered.isEmpty()) continue;
                var saved = offered.orElseThrow(); var limits = saved.limits();
                var property = saved.state().getBlock().getStateDefinition().getProperty("facing");
                if (property == null || !(saved.state().getValue(property) instanceof Direction facing))
                    throw new IllegalStateException("Transformer facing missing");
                // high: the facing side only. low: the other five sides (63 = all six faces).
                int high = 1 << facing.ordinal(), low = 63 ^ high;
                int input = saved.stepUp() ? low : high, output = saved.stepUp() ? high : low;
                var quote = new CustomEUEnergyStorage.NetworkQuote(level, at, saved.energy().amount(),
                    limits.capacity(), limits.inputLimit(), limits.outputPacket(), limits.outputPacket(), true, true, saved.energy());
                ports.add(new Port(tile, at, saved.state(), null, quote, limits.capacity(), input, output,
                    limits.outputPacket(), limits.outputPackets(), false, saved, null, null, List.of(at)));
                continue;
            }
            var machine = tile;
            // Default role: a sink on all six faces. The tests below override it.
            int inputs = 63, outputs = 0;
            var type = BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock());
            boolean generator = tile instanceof AbstractEnergyBlockEntity block && block.isPowerSource()
                || tile instanceof DemandEnergySource;
            if (generator) { inputs = 0; outputs = 63; }
            if (tile instanceof DemandEnergySource demand) outputs &= demand.outputFaces();
            if (tile instanceof AbstractEnergyStorageBlockEntity storageBox) {
                inputs = 0;
                for (var side : Direction.values()) {
                    if (storageBox.canProvidePowerFromSide(side)) outputs |= 1 << side.ordinal();
                    if (storageBox.canConsumePowerFromSide(side)) inputs |= 1 << side.ordinal();
                }
            }
            if (tile instanceof EnergyComponentHost host) {
                var policy = host.energyComponent().ports();
                inputs = policy.inputs();
                outputs = policy.outputs();
            }
            var storage = ownedStorage(machine); var quote = storage.scexNetworkQuote();
            int electricalFaces = conductorFaces(tile);
            inputs &= electricalFaces; outputs &= electricalFaces;
            if (!quote.outputEnabled()) outputs = 0;
            if (tile instanceof com.miophas.singularity_iteration.core.api.energy.IRemoteSwitchable remote
                    && remote.isRemotelyDisabled()) { inputs = 0; outputs = 0; }   // remotely switched off
            // Original binary observations distinguish generator residual offers
            // from the BatBox full-packet reserve rule, including a 1 EU offer.
            // Extended solar packet limits are SI candidate settings, pending loaded reference runs.
            var outputBudget = outputs != 0 ? outputBudget(machine) : null;
            var packets = packetPolicy(machine, type, quote);
            var potential = prepareDemand && machine instanceof DemandEnergySource demand ? demand.potentialEnergy() : null;
            ports.add(new Port(tile, at, tile.getBlockState(), storage, quote, ownedCapacity(machine), inputs, outputs,
                packets.packetSize(), packets.packetCount(), packets.partialPackets(), null, outputBudget, potential, emissionPositions(tile)));
            } catch (RuntimeException error) { topology.isolateChunk(level, entry.getKey(), error); }
        }
        ports.removeIf(port -> !topology.chunkReady(level, ChunkPos.asLong(port.position)) || grid.machines.get(port.position) != port.tile);
        return ports;
    }
    /** Rebuilds only the live quote, packet policy and FE output allowance of each cached port.
     * The expensive per-machine skeleton — chunk checks, face masks, packet
     * tiers, emitter positions — is commit-invariant within one tick and is
     * reused as-is, so a many-endpoint world no longer rewalks every machine,
     * chunk and registry key for each settlement batch. Ports whose entity was
     * removed or lost its storage boundary drop out, mirroring ports().
     *
     * <p>The face masks stay as cached. A face edit publishes a topology change,
     * which increments {@code endpointEdits} and forces a full {@code ports} call.
     * An exception for one port isolates its chunk, as in {@code ports}. */
    private List<Port> refreshQuotes(ServerLevel level, WorldGrid grid, List<Port> cached, boolean prepareDemand) {
        var refreshed = new ArrayList<Port>(cached.size());
        for (var port : cached) {
            try {
            var tile = port.tile();
            if (tile.isRemoved() || !topology.chunkReady(level, ChunkPos.asLong(port.position))) continue;
            if (tile instanceof IndependentTransformerBlockEntity transformer) {
                var offered = transformer.snapshot(transformer.stepUpNow());
                if (offered.isEmpty()) continue;
                var saved = offered.orElseThrow(); var limits = saved.limits();
                var quote = new CustomEUEnergyStorage.NetworkQuote(level, port.position(), saved.energy().amount(),
                    limits.capacity(), limits.inputLimit(), limits.outputPacket(), limits.outputPacket(), true, true, saved.energy());
                refreshed.add(new Port(tile, port.position(), port.state(), null, quote, limits.capacity(),
                    port.inputs(), port.outputs(), limits.outputPacket(), limits.outputPackets(), false, saved, null, null, port.emitterPositions()));
                continue;
            }
            var storage = ownedStorage(tile);
            if (storage == null) continue;
            var quote = storage.scexNetworkQuote();
            var type = BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock());
            var outputBudget = port.outputs() != 0 ? outputBudget(tile) : null;
            var packets = packetPolicy(tile, type, quote);
            var potential = prepareDemand && tile instanceof DemandEnergySource demand ? demand.potentialEnergy() : null;
            refreshed.add(new Port(tile, port.position(), port.state(), storage, quote, ownedCapacity(tile),
                port.inputs(), port.outputs(), packets.packetSize(), packets.packetCount(), packets.partialPackets(), null, outputBudget, potential, port.emitterPositions()));
            } catch (RuntimeException error) { topology.isolateChunk(level, port.position, error); }
        }
        refreshed.removeIf(port -> !topology.chunkReady(level, ChunkPos.asLong(port.position)) || grid.machines.get(port.position) != port.tile);
        return refreshed;
    }
    /**
     * One face of a foreign block. {@code at}: the foreign block.
     * {@code input}: the face of the foreign block that touches the SI side.
     */
    private record ForeignContact(BlockPos at, Direction input) { }
    /**
     * A selected route. {@code lossMilli}: total loss in milli-EU.
     * {@code path}: the conductor path, or {@code null} for a direct face contact
     * with no conductor (loss 0).
     */
    private record Route(long lossMilli, ConductorRegistry.Path path) { }
    /**
     * Resumes one foreign pair, including all candidate faces, without quoting energy.
     *
     * <p>The plan tests each (emitter, output face) combination in order. The
     * {@code contact} field is the index of the next combination:
     * {@code contact / 6} is the emitter index, {@code contact % 6} is the face
     * ordinal. It runs from 0 to {@code emitters.size() * 6}. For each
     * combination, the plan tests each open input face of the receiver and
     * keeps the route with the lowest loss in {@code best}.
     *
     * <p>A direct face contact (the emitter face touches the receiver) ends the
     * search at once with loss 0.
     *
     * <p>The plan is bound to one conductor snapshot. Use {@link #matches} before
     * resuming it.
     */
    private static final class ForeignRoutePlan {
        final ConductorRegistry.Snapshot graph;
        final List<BlockPos> emitters;
        final int outputs, inputs;
        final BlockPos receiver;
        int contact;
        Route best;
        Direction bestInput;
        ForeignRoutePlan(ConductorRegistry.Snapshot graph, List<BlockPos> emitters, int outputs, BlockPos receiver, int inputs) {
            this.graph = graph; this.emitters = List.copyOf(emitters); this.outputs = outputs;
            this.receiver = receiver; this.inputs = inputs;
        }
        /** Tells whether this plan was built for the same snapshot, emitters, masks and receiver. */
        boolean matches(ConductorRegistry.Snapshot snapshot, List<BlockPos> from, int outputMask, BlockPos to, int inputMask) {
            return graph == snapshot && emitters.equals(from) && outputs == outputMask && receiver.equals(to) && inputs == inputMask;
        }
        /**
         * Continues the search.
         *
         * @return {@code true} when the search is complete ({@code best} is the
         *         result, or {@code null} when no route exists). {@code false}
         *         when the budget cannot pay for the next search. {@code contact}
         *         then stays at the same combination, and a later call retries it.
         */
        boolean advance(ConductorRegistry.SearchBudget budget) {
            while (contact < emitters.size() * 6) {
                int current = contact; var output = Direction.values()[current % 6];
                var start = point(emitters.get(current / 6).relative(output));
                if ((outputs & (1 << output.ordinal())) == 0) { contact++; continue; }
                if (start.equals(point(receiver)) && (inputs & (1 << output.getOpposite().ordinal())) != 0) {
                    best = new Route(0, null); bestInput = output.getOpposite(); contact = emitters.size() * 6; return true;
                }
                if (!graph.contains(start) || !graph.permits(start, output.getOpposite().ordinal())) { contact++; continue; }
                ConductorRegistry.RouteIndex paths = null;
                for (var side : Direction.values()) {
                    if ((inputs & (1 << side.ordinal())) == 0) continue;
                    var end = point(receiver.relative(side));
                    if (!graph.contains(end) || !graph.permits(end, side.getOpposite().ordinal())
                            || graph.componentOf(start) != graph.componentOf(end)) continue;
                    if (paths == null) {
                        // Search lazily: only when at least one receiver face is in the same component.
                        paths = graph.routesFrom(start, budget);
                        if (paths == null) return false;
                    }
                    int vertex = graph.vertex(end);
                    if (!paths.reaches(vertex)) continue;
                    long loss = paths.lossMilliTo(vertex);
                    if (best == null || loss < best.lossMilli) {
                        best = new Route(loss, paths.path(vertex)); bestInput = side;
                    }
                }
                contact++;
            }
            return true;
        }
    }
    /** A position-only tile. {@code tileAt} returns it for an SI node that is not an {@link IEnergyTile}. */
    private record LocatedNode(ServerLevel getWorld, BlockPos getPos) implements ILocatableTile { }

    /**
     * Registration is a notification. Only loaded, owned entities enter the native transaction.
     *
     * <p>The method stores the tile in {@code registered} for {@link #tileAt}
     * and queues a topology observation of {@code at}. It does nothing when the
     * engine is closed, the level belongs to another server, or the caller is not
     * on the server thread. At {@link #ENDPOINT_LIMIT} registrations, a new
     * position is ignored with a throttled warning.
     */
    public void registerTile(ServerLevel level, BlockPos at, IEnergyTile tile) {
        if (closed || level.getServer() != server || !server.isSameThread()) return;
        var grid = worlds.computeIfAbsent(level, ignored -> new WorldGrid(selectionSeed ^ 0x5343455848495354L));
        if (grid.registered.size() >= ENDPOINT_LIMIT && !grid.registered.containsKey(at)) {
            grid.registrationLimitDrops++;
            if (grid.registrationLimitDrops == 1 || grid.registrationLimitDrops % 1000 == 0)
                LOG.warn("Registered endpoint limit {} reached on one level; extra registrations ignored (drops={})",
                    ENDPOINT_LIMIT, grid.registrationLimitDrops);
            return;
        }
        grid.registered.put(at.immutable(), tile);
        topology.changed(level, at);
    }
    /**
     * Removes every registration of {@code tile} (by identity) in all
     * dimensions. When the tile is a block entity, the method also queues a
     * topology observation of its position. Cost: O(registrations).
     */
    public void unregisterTile(IEnergyTile tile) {
        for (var grid : worlds.values()) grid.registered.values().removeIf(value -> value == tile);
        if (tile instanceof BlockEntity block) changed(block);
    }
    /**
     * Returns the energy tile at a loaded position.
     * Search order: published endpoint, published conductor, registered tile.
     * A published node that is not an {@link IEnergyTile} returns a
     * {@code LocatedNode}.
     *
     * @return the tile, or {@code null} when the position is not loaded, holds
     *         no node, or holds a removed or invalid tile
     */
    public IEnergyTile tileAt(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (grid == null || !loaded(level, at)) return null;
        var entity = grid.machines.get(at);
        if (entity == null) entity = grid.conductorOwners.get(at);
        if (entity != null && !entity.isRemoved())
            return entity instanceof IEnergyTile tile ? tile : new LocatedNode(level, at.immutable());
        var registered = grid.registered.get(at);
        if (registered instanceof FECompatTile foreign && !foreign.isValid()) return null;
        if (registered instanceof BlockEntity block && block.isRemoved()) return null;
        return registered;
    }
    /**
     * Returns the meter sample of a position from the current or the previous
     * server tick. Values: EU in, EU out, highest voltage (EU per packet).
     *
     * @return the sample, or {@code null} when no grid exists, the position is
     *         not loaded, the samples are older than one tick, or the position
     *         moved no energy
     */
    public NodeStats nodeStats(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        // sampleTick starts at Long.MIN_VALUE: test the sentinel before subtracting (overflow)
        return grid == null || !loaded(level, at) || grid.sampleTick == Long.MIN_VALUE
            || server.getTickCount() - grid.sampleTick > 1 ? null : grid.samples.get(at);
    }

    /** Query after level settlement. An idle registered node contributes a zero tick,
     * never the previous tick's packet. The existing sparse query remains unchanged. */
    public NodeStats currentTickNodeStats(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (closed || !failure.isEmpty() || grid == null || !grid.failure.isEmpty() || tileAt(level, at) == null) return null;
        NodeStats measured = grid.sampleTick == server.getTickCount() ? grid.samples.get(at) : null;
        return measured == null ? new NodeStats(0, 0, 0) : measured;
    }
    /**
     * Adds one meter sample for a position. Input and output add up over the
     * tick. Voltage keeps the highest value of the tick.
     *
     * @param input EU that entered the position
     * @param output EU that left the position
     * @param voltage EU per packet, or 0 for FE
     */
    private static void sample(WorldGrid grid, BlockPos at, double input, double output, double voltage) {
        var before = grid.samples.get(at);
        grid.samples.put(at.immutable(), before == null ? new NodeStats(input, output, voltage)
            : new NodeStats(before.energyIn() + input, before.energyOut() + output, Math.max(before.voltage(), voltage)));
    }
    /**
     * Returns the faces of {@code at} that have an electrical connection now.
     * A face connects when both sides have the face open and the neighbour is a
     * published conductor, an SI endpoint, or a valid foreign FE block.
     * This query builds a full port list. Do not call it every tick for many
     * positions.
     *
     * @return an immutable set, empty when the grid is not accessible
     */
    public Set<Direction> connections(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (grid == null || !loaded(level, at) || !accessible(level, grid)) return Set.of();
        var result = java.util.EnumSet.noneOf(Direction.class);
        var graph = grid.conductors.snapshot();
        int own = graph.contains(point(at)) ? conductorFaces(grid.conductorOwners.get(at)) : 0;
        // Build the port view once. This API is queried by the compatibility
        // layer for each face. Scanning all machines again inside the neighbour
        // loop made one connection query O(6 * machineCount) and allocated a
        // fresh Port list for every checked machine.
        var portsByPosition = new HashMap<BlockPos, Port>();
        for (var port : ports(level, grid, false)) portsByPosition.put(port.position, port);
        var ownPort = portsByPosition.get(at);
        if (ownPort != null) own = ownPort.inputs | ownPort.outputs;
        for (Direction side : Direction.values()) {
            if ((own & (1 << side.ordinal())) == 0) continue;
            var neighbour = at.relative(side);
            if (graph.contains(point(neighbour)) && graph.permits(point(neighbour), side.getOpposite().ordinal())) result.add(side);
            else {
                var neighbourPort = portsByPosition.get(neighbour);
                if (neighbourPort != null
                        && ((neighbourPort.inputs | neighbourPort.outputs) & (1 << side.getOpposite().ordinal())) != 0) {
                    result.add(side);
                    continue;
                }
                if (grid.machines.containsKey(neighbour)) continue;
                if (loaded(level, neighbour) && foreignQuery(grid, new ForeignContact(neighbour, side.getOpposite()),
                    () -> new FECompatTile(level, neighbour, side).isValid())) result.add(side);
            }
        }
        return Set.copyOf(result);
    }
    /**
     * Tells whether the source at {@code at} can reach a sink now. A sink
     * counts when it is not full, or when it is a foreign FE block that can
     * receive. Reachability only tests connected components. It does not test
     * loss or packet size. This query can run a foreign discovery batch.
     */
    public boolean hasReachableSink(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (grid == null || !accessible(level, grid)) return false;
        var ports = ports(level, grid, false);
        var graph = grid.conductors.snapshot();
        for (var source : ports) if (source.position.equals(at) && source.outputs != 0) {
            for (var sink : ports) if (sink.tile != source.tile && sink.inputs != 0
                    && sink.quote.exactAmount().compareTo(EnergyAmount.of(sink.capacity)) < 0)
                for (var side : Direction.values()) if ((sink.inputs & (1 << side.ordinal())) != 0
                    && hasRoute(source, sink.position, side, graph)) return true;
            discoverForeign(level, grid);
            // Reuse membership only. Capability callbacks may mutate the world,
            // so never retain a live map iterator across an external query.
            for (var target : grid.foreign.snapshot()) {
                if (!foreignQuery(grid, target.getKey(), target.getValue()::canReceive) || !accessible(level, grid)) continue;
                graph = grid.conductors.snapshot();
                if (hasRoute(source, target.getKey().at, target.getKey().input, graph)) return true;
            }
        }
        return false;
    }
    /** Tells whether the chunk of {@code at} is loaded and ticks blocks. It never loads a chunk. */
    private static boolean loaded(ServerLevel level, BlockPos at) {
        return level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) != null
            && level.shouldTickBlocksAt(ChunkPos.asLong(at));
    }
    /**
     * Runs one query against a foreign block and contains its failures.
     *
     * <p>A query that throws returns {@code false}. The contact then stays
     * blocked until the next server tick. The method records the failure count
     * and the last failure text. After 4096 failed contacts in one tick, all
     * queries return {@code false} until the next tick. This limits the cost of
     * a broken mod.
     *
     * @return the query result, or {@code false} when the contact is blocked or the query threw
     */
    private boolean foreignQuery(WorldGrid grid, ForeignContact contact, java.util.function.BooleanSupplier query) {
        if (grid.queryTick != server.getTickCount()) {
            grid.queryTick = server.getTickCount(); grid.failedQueries.clear();
        }
        if (grid.failedQueries.contains(contact) || grid.failedQueries.size() >= 4096) return false;
        try { return query.getAsBoolean(); }
        catch (RuntimeException error) {
            grid.failedQueries.add(contact);
            if (foreignQueryFailures < Long.MAX_VALUE) foreignQueryFailures++;
            String detail = contact.at + " " + error.getClass().getSimpleName() + ": " + error.getMessage();
            lastForeignQueryFailure = detail.substring(0, Math.min(256, detail.length()));
            return false;
        }
    }
    /**
     * Runs one foreign discovery batch. The {@code discoveringForeign} flag
     * blocks re-entry: a foreign callback can call back into the engine, and a
     * nested batch would change the cursor and the index during the outer batch.
     */
    private void discoverForeign(ServerLevel level, WorldGrid grid) {
        if (grid.discoveringForeign || !currentWorld(level, grid)) return;
        grid.discoveringForeign = true;
        try {
            discoverForeignBatch(level, grid);
        } finally {
            grid.discoveringForeign = false;
        }
    }
    /**
     * Finds foreign FE blocks next to SI nodes, in bounded steps.
     *
     * <ol>
     *   <li>Rebuild {@code scanAnchors} when {@code scanDirty} is set, and reset
     *       the cursor to 0.</li>
     *   <li>Once per tick: check up to 128 known contacts, and drop the invalid
     *       ones.</li>
     *   <li>Check the six neighbours of up to 128 anchors, starting at
     *       {@code scanCursor}. A neighbour becomes a contact when it is loaded,
     *       is not an SI node, and its FE adapter is valid.</li>
     * </ol>
     * <p>Each foreign callback can change the world. The method checks
     * {@code currentWorld} after the callbacks and stops when it is false.
     */
    private void discoverForeignBatch(ServerLevel level, WorldGrid grid) {
        if (grid.scanDirty) {
            var anchors = new ArrayList<BlockPos>(grid.conductorOwners.keySet()); anchors.addAll(grid.machines.keySet());
            grid.scanAnchors = List.copyOf(anchors); grid.scanDirty = false; grid.scanCursor = 0;
        }
        // Maintenance is shared by START/END settlement and reachability queries.
        // Stale entries may wait for their turn, but every transfer still checks
        // foreignRouteCurrent immediately before committing to a live capability.
        if (grid.foreignMaintenanceTick != server.getTickCount()) {
            grid.foreignMaintenanceTick = server.getTickCount();
            grid.foreign.maintain(128, (contact, adapter) -> foreignQuery(grid, contact, adapter::isValid));
        }
        if (!currentWorld(level, grid)) return;
        // count == 0 when there is no anchor, so the floorMod below never divides by 0.
        int count = Math.min(128, grid.scanAnchors.size());
        int anchorCount = grid.scanAnchors.size();
        for (int i = 0; i < count; i++) {
            // Normalize before advancing so a very long-running server can
            // cross Integer.MAX_VALUE without a negative index or a skipped
            // anchor.  Advancing from the normalized index preserves the
            // original round-robin order even when the in-memory cursor was
            // corrupted by an older build.
            int anchorIndex = Math.floorMod(grid.scanCursor, anchorCount);
            var anchor = grid.scanAnchors.get(anchorIndex);
            grid.scanCursor = anchorIndex + 1 == anchorCount ? 0 : anchorIndex + 1;
            for (var side : Direction.values()) {
                var at = anchor.relative(side);
                if (!loaded(level, at) || grid.machines.containsKey(at) || grid.conductorOwners.containsKey(at)) continue;
                // loaded() returned true, so getChunkNow() is not null here.
                var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                var entity = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                // An unpublished native entity must never become a foreign callback route.
                if (ownedStorage(entity) != null && ownedStorage(entity).scexNetworkControlled()
                    || entity instanceof IndependentTransformerBlockEntity
                    || entity instanceof DemandEnergySource || entity instanceof IndependentSpecialCableBlockEntity) continue;
                var contact = new ForeignContact(at.immutable(), side.getOpposite());
                if (grid.foreign.containsKey(contact) || grid.foreign.size() >= 4096) continue;
                var adapter = new FECompatTile(level, at, side);
                boolean valid = foreignQuery(grid, contact, adapter::isValid);
                if (!currentWorld(level, grid)) return;
                if (valid) grid.foreign.putIfAbsent(contact, adapter);
            }
        }
        accessible(level, grid); // One refresh after the complete batch of external discovery callbacks.
    }
    /**
     * Reachability needs connected components, not a shortest-path search.
     *
     * @param receiver the receiving block
     * @param input the face of {@code receiver} that must take the energy
     * @return {@code true} when an open output face of the source touches the
     *         receiver face directly, or when both touch conductors of one component
     */
    private static boolean hasRoute(Port source, BlockPos receiver, Direction input, ConductorRegistry.Snapshot graph) {
        for (var emitter : source.emitterPositions) for (var output : Direction.values()) {
            if ((source.outputs & (1 << output.ordinal())) == 0) continue;
            var start = emitter.relative(output);
            if (start.equals(receiver) && input == output.getOpposite()) return true;
            var end = receiver.relative(input);
            if (!graph.contains(point(start)) || !graph.permits(point(start), output.getOpposite().ordinal())
                || !graph.contains(point(end)) || !graph.permits(point(end), input.getOpposite().ordinal())) continue;
            if (graph.componentOf(point(start)) == graph.componentOf(point(end))) return true;
        }
        return false;
    }
    /**
     * Tells whether an FE route is still safe to use. The FE paths call it just
     * before each foreign callback and each commit.
     *
     * <p>It checks the foreign contact (still indexed, still valid), the grid
     * (accessible, same conductor snapshot), and the SI port (loaded, same tile,
     * same state, same emitters, same output faces, same transformer routing).
     * The contact is checked twice around the validity query, because the query
     * itself can change the index.
     *
     * @param source the SI port of the route. For an import it is the SI sink.
     */
    private boolean foreignRouteCurrent(ServerLevel level, WorldGrid grid, ConductorRegistry.Snapshot graph, Port source,
            Map.Entry<ForeignContact, FECompatTile> target) {
        if (!grid.foreign.isCurrent(target) || !foreignQuery(grid, target.getKey(), target.getValue()::isValid)
            || !grid.foreign.isCurrent(target)
            || !accessible(level, grid) || !grid.conductors.isCurrent(graph)
            || !loaded(level, source.position) || source.tile.isRemoved()
            || source.tile.getBlockState() != source.state || grid.machines.get(source.position) != source.tile) return false;
        if (!source.emitterPositions.equals(emissionPositions(source.tile))) return false;
        if (source.tile instanceof DemandEnergySource demand && (source.outputs & ~demand.outputFaces()) != 0) return false;
        if (source.tile instanceof IndependentTransformerBlockEntity transformer)
            return transformer.validMode() && transformer.routingSignature() == grid.transformerRouting.getOrDefault(source.position, -1);
        return true;
    }
    /**
     * The FE export pass: SI sources to foreign FE sinks.
     *
     * <p>The pass visits (source, target) pairs in round-robin order. Pair
     * {@code index} maps to source {@code index / targets} and target
     * {@code index % targets}. The pass makes at most
     * {@code min(256, pairs * maxPacketCount)} attempts per tick.
     *
     * <p><b>Limit per pair.</b> The FE limit is the smallest of: the packet room
     * of the tick ({@code packet * packets - packetSpent}), the one-packet window
     * offer of the frozen allowance, and the native output budget. The pass
     * skips the pair when the route loss is equal to or more than the limit.
     *
     * <p><b>Transfer.</b> A demand source first makes fuel into EU for the amount
     * that the target wants. Then the source pushes FE through a
     * {@link RoutedFeTarget}, which takes the route loss and re-checks
     * {@code foreignRouteCurrent}. After a nonzero transfer, the pass debits the
     * allowance, records samples, and applies conductor effects.
     *
     * <p><b>Return points.</b> The pass returns when the grid is no longer
     * accessible or the snapshot changed, when a budget is empty (the pair plan
     * stays in {@code exportPlan} and resumes), and when an effect removed a
     * conductor (the snapshot is then stale).
     *
     * <p>A callback that throws marks the source in {@code failedOutputs} for
     * the rest of the tick. The engine does not refund energy that a demand
     * source already prepared. That energy stays in the source storage.
     */
    private void settleForeign(ServerLevel level, WorldGrid grid, IdentityHashMap<BlockEntity, PacketAllowance> allowances) {
        if (!accessible(level, grid)) return;
        grid.exportSearch.reset(server.getTickCount());
        discoverForeign(level, grid);
        var sources = ports(level, grid, true).stream().filter(port -> port.outputs != 0 && port.packet > 0 && port.packets > 0).toList();
        if (sources.isEmpty() || grid.foreign.isEmpty()) return;
        var targets = grid.foreign.snapshot();
        var graph = grid.conductors.snapshot();
        int pairs = boundedPairCount(sources.size(), targets.size());
        int packets = sources.stream().mapToInt(Port::packets).max().orElse(1);
        int attempts = (int) Math.min(256L, (long) pairs * packets);
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (!accessible(level, grid) || !grid.conductors.isCurrent(graph)) { grid.exportPlan = null; return; }
            // Normalize before advancing.  The old post-increment sequence
            // could turn Integer.MAX_VALUE into Integer.MIN_VALUE, leaving a
            // negative cursor in a long-running server.  Keeping the cursor
            // in [0, pairs) also repairs a value persisted by an older build.
            int index = Math.floorMod(grid.transferCursor, pairs);
            var source = sources.get(index / targets.size()); var entry = targets.get(index % targets.size());
            var target = entry.getValue();
            if (grid.failedOutputs.contains(source.position)) {
                grid.transferCursor = index + 1 == pairs ? 0 : index + 1;
                grid.exportPlan = null; continue;
            }
            int inputMask = 1 << entry.getKey().input.ordinal();
            var plan = grid.exportPlan;
            if (plan == null || !plan.matches(graph, source.emitterPositions, source.outputs, entry.getKey().at, inputMask))
                grid.exportPlan = plan = new ForeignRoutePlan(graph, source.emitterPositions, source.outputs, entry.getKey().at, inputMask);
            // Budget empty: keep exportPlan and the cursor, so the next tick resumes this pair.
            if (!plan.advance(grid.exportSearch)) return;
            // Visit budget kind 1 = export.
            if (plan.best != null && plan.best.path != null
                    && !grid.routeBudget.reserveVisits(plan.best.path.vertexCount(), 1)) return;
            grid.exportPlan = null;
            grid.transferCursor = index + 1 == pairs ? 0 : index + 1;
            var route = plan.best;
            if (route == null) continue;
            // Saturating multiply: packet * packets in EU/t, capped at Long.MAX_VALUE.
            long allowance = source.packet > Long.MAX_VALUE / source.packets ? Long.MAX_VALUE : source.packet * source.packets;
            var remaining = grid.packetSpent.getOrDefault(source.position, EnergyAmount.ZERO).roomBelow(allowance);
            // Native EU and engine-driven FE exports share the initial packet
            // offer. In particular a full-packet source cannot leak a sub-packet
            // through FE, or renew an already spent native allowance here.
            remaining = remaining.min(windowOffer(source, allowances, 1));
            int limit = FeLedger.fe(remaining);
            var nativeBudget = outputBudget(source.tile);
            if (nativeBudget != null) limit = Math.min(limit, FeLedger.fe(nativeBudget.allowance()));
            if (limit == 0) continue;
            int lossFe = com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyLoss.feLoss(route.lossMilli);
            if (lossFe >= limit) continue;
            java.util.function.BooleanSupplier current = () -> foreignRouteCurrent(level, grid, graph, source, entry);
            var routed = new RoutedFeTarget(target, limit, lossFe, current);
            try {
                if (source.tile instanceof DemandEnergySource demand && current.getAsBoolean()) {
                    int wanted = routed.receiveEnergy(limit, true);
                    if (wanted > 0) demand.prepareEnergy(FeLedger.eu(wanted));
                }
                int sent;
                if (source.tile instanceof EnergyComponentHost host) sent = host.energyComponent().internalLedger().push(routed, current);
                else if (source.tile instanceof AbstractEnergyBlockEntity machine) sent = machine.scexFeBridge().pushRouted(routed, current);
                else if (source.tile instanceof IndependentTransformerBlockEntity transformer) sent = pushTransformer(transformer, routed, current);
                else continue;
                if (sent == 0) continue;
                // Not null: limit > 0 above needs a nonzero windowOffer, which needs an allowance.
                allowances.get(source.tile).debit(FeLedger.eu(sent));
                grid.exportedThisTick.add(entry.getKey().at);
                grid.packetSpent.merge(source.position, FeLedger.eu(sent), EnergyAmount::add);
                // FE to EU for meters: 4 FE = 1 EU. The voltage is the EU that the source sent.
                double output = sent / 4.0, input = routed.delivered() / 4.0;
                double voltage = output;
                sample(grid, source.position, 0, input, voltage);
                sample(grid, entry.getKey().at, input, 0, voltage);
                var fuses = new LinkedHashMap<BlockPos, ResourceLocation>();
                var strips = new LinkedHashMap<BlockPos, ResourceLocation>();
                var shocks = new LinkedHashMap<BlockPos, Double>();
                if (route.path != null && accessible(level, grid) && grid.conductors.isCurrent(graph)) route.path.visit(at -> {
                    var pos = new BlockPos(at.x(), at.y(), at.z()); sample(grid, pos, input, input, voltage);
                    var type = grid.conductorTypes.get(pos); var fuseLimit = tierFuseLimit(type);
                    if (fuseLimit == null) fuseLimit = EnergyNodeRegistry.fuseLimit(type);
                    if (IndependentEnergyMode.enetCableMeltdown() && fuseLimit != null && voltage > fuseLimit) fuses.put(pos, type);
                    var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                    if (chunk != null) {
                        var be = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                        if (be instanceof IEnergyConductor conductor) {
                            conductor.onEnergyPass();
                            if (IndependentEnergyMode.enetCableMeltdown() && !fuses.containsKey(pos) && voltage > conductor.getInsulationBreakdownEnergy()) strips.put(pos, type);
                            if (IndependentEnergyMode.enetShockDamage() && voltage > conductor.getInsulationEnergyAbsorption()) {
                                double shockEnergy = voltage - conductor.getInsulationEnergyAbsorption();
                                shocks.merge(pos, shockEnergy, Math::max);
                            }
                        }
                    }
                    var detector = grid.specialCables.get(pos); if (detector != null && detector.detector()) detector.delivered();
                });
                commits++; totalDebit = totalDebit.add(FeLedger.eu(sent)); totalCredit = totalCredit.add(FeLedger.eu(routed.delivered()));
                totalLoss = totalLoss.add(FeLedger.eu(sent - routed.delivered()));
                debited = totalDebit.whole(); credited = totalCredit.whole(); dissipated = totalLoss.whole();
                var effectPositions = new HashSet<BlockPos>(fuses.keySet()); effectPositions.addAll(strips.keySet());
                if (!effectPositions.isEmpty()) grid.effectEndpoints.addAll(affectedEndpoints(grid, effectPositions));
                collectShockDamage(level, grid, new ShockRoute(source.position, entry.getKey().at), shocks);
                var removed = fuseConductors(level, grid, fuses);
                strips.keySet().removeAll(fuses.keySet());
                removed.addAll(stripInsulation(level, grid, strips));
                if (!removed.isEmpty()) {
                    grid.apply(grid.forget(removed::contains)); grid.effectPauseFrame = ticks + 2;
                    return; // The prior graph lease was revoked by the physical fuse effects.
                }
            } catch (RuntimeException foreignFailure) {
                // A foreign callback may have accepted before throwing. The pre-debit stays
                // charged. The native graph keeps running and does not invent a refund.
                grid.failedOutputs.add(source.position);
                rejected++; LOG.warn("Foreign energy endpoint threw during transfer; skipped", foreignFailure);
            }
        }
    }
    /**
     * The FE import pass: foreign FE sources to SI sinks.
     *
     * <p>The pass visits (foreign source, SI sink) pairs in round-robin order,
     * at most {@code min(256, pairs)} per tick. It skips a foreign contact that
     * received an export in this tick, so energy cannot cycle between the two
     * passes.
     *
     * <p>The sink pulls FE through its own ledger or bridge. The loss in FE is
     * taken from the extracted amount: {@code received = max(0, extracted - lossFe)}.
     * A transformer sink is limited to its input limit per tick
     * ({@code transformerReceived}).
     *
     * <p>FE has no packets. The pass records a voltage of 0 and has no
     * overvoltage effects. A callback that throws is logged and skipped.
     */
    private void importForeign(ServerLevel level, WorldGrid grid) {
        if (!accessible(level, grid) || grid.foreign.isEmpty()) return;
        grid.importSearch.reset(server.getTickCount());
        var sinks = ports(level, grid, false).stream().filter(port -> port.inputs != 0).toList();
        if (sinks.isEmpty()) return;
        var sources = grid.foreign.snapshot();
        var graph = grid.conductors.snapshot();
        int pairs = boundedPairCount(sources.size(), sinks.size());
        for (int attempt = 0; attempt < Math.min(256, pairs); attempt++) {
            if (!accessible(level, grid) || !grid.conductors.isCurrent(graph)) { grid.importPlan = null; return; }
            // Use the same bounded advancement as exports. Never rely on an
            // overflowing post-increment to detect the wrap point.
            int index = Math.floorMod(grid.importCursor, pairs);
            var source = sources.get(index / sinks.size()); var sink = sinks.get(index % sinks.size());
            if (grid.exportedThisTick.contains(source.getKey().at)) {
                grid.importCursor = index + 1 == pairs ? 0 : index + 1;
                grid.importPlan = null; continue;
            }
            var emitters = List.of(source.getKey().at); int outputs = 1 << source.getKey().input.ordinal();
            var plan = grid.importPlan;
            if (plan == null || !plan.matches(graph, emitters, outputs, sink.position, sink.inputs))
                grid.importPlan = plan = new ForeignRoutePlan(graph, emitters, outputs, sink.position, sink.inputs);
            // Budget empty: keep importPlan and the cursor, so the next tick resumes this pair.
            if (!plan.advance(grid.importSearch)) return;
            // Visit budget kind 2 = import.
            if (plan.best != null && plan.best.path != null
                    && !grid.routeBudget.reserveVisits(plan.best.path.vertexCount(), 2)) return;
            grid.importPlan = null;
            grid.importCursor = index + 1 == pairs ? 0 : index + 1;
            Route selected = plan.best; Direction selectedSide = plan.bestInput;
            // feLoss returns Integer.MAX_VALUE for a loss too large to carry any FE.
            if (selected == null || com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyLoss.feLoss(selected.lossMilli) == Integer.MAX_VALUE) continue;
            final var route = selected; final var inputSide = selectedSide;
            int lossFe = com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyLoss.feLoss(route.lossMilli);
            java.util.function.BooleanSupplier current = () -> foreignRouteCurrent(level, grid, graph, sink, source);
            try {
                if (!source.getValue().canExtract()) continue;
                int extracted;
                if (sink.tile instanceof EnergyComponentHost host)
                    extracted = host.energyComponent().internalLedger().pull(source.getValue(), lossFe,
                        () -> host.energyComponent().ports().accepts(inputSide) && current.getAsBoolean());
                else if (sink.tile instanceof AbstractEnergyBlockEntity machine)
                    extracted = machine.scexFeBridge().pullRouted(source.getValue(), inputSide, lossFe, current);
                else if (sink.tile instanceof IndependentTransformerBlockEntity transformer)
                    extracted = pullTransformer(transformer, source.getValue(), lossFe,
                        grid.transformerReceived.getOrDefault(sink.position, EnergyAmount.ZERO), current);
                else continue;
                if (extracted == 0) continue;
                int received = Math.max(0, extracted - lossFe);
                if (sink.transformer != null && received > 0)
                    grid.transformerReceived.merge(sink.position, FeLedger.eu(received), EnergyAmount::add);
                double output = extracted / 4.0, input = received / 4.0;
                // FE has no packet voltage. Meters use zero for this unpacketized intake.
                sample(grid, source.getKey().at, 0, output, 0);
                sample(grid, sink.position, input, 0, 0);
                if (route.path != null && accessible(level, grid) && grid.conductors.isCurrent(graph)) route.path.visit(at -> {
                    var pos = new BlockPos(at.x(), at.y(), at.z()); sample(grid, pos, output, output, 0);
                    var detector = grid.specialCables.get(pos); if (detector != null && detector.detector()) detector.delivered();
                });
                commits++; totalDebit = totalDebit.add(FeLedger.eu(extracted)); totalCredit = totalCredit.add(FeLedger.eu(received));
                totalLoss = totalLoss.add(FeLedger.eu(extracted - received));
                debited = totalDebit.whole(); credited = totalCredit.whole(); dissipated = totalLoss.whole();
            } catch (RuntimeException foreignFailure) { rejected++; LOG.warn("Foreign energy endpoint threw during transfer; skipped", foreignFailure); }
        }
    }

    /**
     * Pair iteration is deliberately capped: only a bounded number of routes is
     * sampled per tick, so representing a larger Cartesian product exactly would
     * add an overflow failure without improving scheduling fairness.
     */
    private static int boundedPairCount(int left, int right) {
        if (left <= 0 || right <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (long) left * right);
    }
    /**
     * Pulls FE from a foreign source into a transformer.
     *
     * <p>Room is the smallest of the free capacity and the input limit minus
     * {@code receivedThisTick}. The method asks for {@code room + lossFe}, first
     * as a simulation, then for real. The loss stays with the route. The
     * transformer gets {@code extracted - lossFe} EU-equivalent.
     *
     * <p>The credit goes through a temporary {@link NetworkCell} that holds the
     * accepted EU. The commit moves it from that cell into the transformer cell,
     * so the commit is balanced.
     *
     * @return the FE that the foreign source gave up ({@code >= 0}). It includes the loss.
     * @throws IllegalStateException when the foreign source reports an invalid
     *         amount, or the transformer changed during the callback
     */
    private static int pullTransformer(IndependentTransformerBlockEntity transformer, FECompatTile source, int lossFe,
            EnergyAmount receivedThisTick, java.util.function.BooleanSupplier current) {
        var before = transformer.snapshot(transformer.stepUpNow()).orElse(null);
        if (before == null || !current.getAsBoolean() || !source.canExtract()) return 0;
        int room = FeLedger.fe(before.energy().exactAmount().roomBelow(before.limits().capacity())
            .min(receivedThisTick.roomBelow(before.limits().inputLimit())));
        if (room == 0) return 0;
        // long math, then clamp: room + lossFe can be above Integer.MAX_VALUE.
        int maximum = (int)Math.min(Integer.MAX_VALUE, (long)room + lossFe);
        int quoted = source.extractEnergy(maximum, true);
        if (quoted <= lossFe || quoted > maximum || !current.getAsBoolean()
            || !transformer.isCurrent(before, transformer.stepUpNow())) return 0;
        int extracted = source.extractEnergy(quoted, false);
        if (extracted < 0 || extracted > quoted) throw new IllegalStateException("Invalid transformer FE input receipt");
        int accepted = Math.max(0, extracted - lossFe);
        // All extracted FE went to loss. There is nothing to credit.
        if (accepted == 0) return extracted;
        var fresh = transformer.snapshot(transformer.stepUpNow()).orElse(null);
        if (fresh == null) throw new IllegalStateException("Transformer removed during foreign extraction");
        var acquired = new NetworkCell(FeLedger.eu(accepted));
        if (!NetworkCell.commit(List.of(new NetworkCell.Write(acquired.quote(), EnergyAmount.ZERO),
                new NetworkCell.Write(fresh.energy(), fresh.energy().exactAmount().add(FeLedger.eu(accepted)))),
                EnergyAmount.ZERO, () -> true)) throw new IllegalStateException("Transformer input lease changed");
        transformer.markNetworkChanged();
        return extracted;
    }

    /**
     * Pushes FE from a transformer into a routed foreign target.
     *
     * <ol>
     *   <li>The transformer must hold at least one output packet.</li>
     *   <li>Simulate the target to get the offered FE.</li>
     *   <li>Commit the debit from the transformer cell first. The debit is in
     *       flight until the target accepts it.</li>
     *   <li>Call the target for real.</li>
     *   <li>When the target accepts less, refund the difference through a
     *       temporary {@link NetworkCell}.</li>
     * </ol>
     *
     * @return the FE that left the transformer
     * @throws IllegalStateException when the refund commit fails
     */
    private static int pushTransformer(IndependentTransformerBlockEntity transformer, RoutedFeTarget target,
            java.util.function.BooleanSupplier current) {
        var saved = transformer.snapshot(transformer.stepUpNow()).orElse(null);
        if (saved == null || saved.energy().exactAmount().whole() < saved.limits().outputPacket() || !target.canReceive()) return 0;
        int offered = target.receiveEnergy(FeLedger.fe(saved.energy().exactAmount()), true);
        if (offered <= 0) return 0;
        var debit = FeLedger.eu(offered);
        if (!NetworkCell.commit(List.of(new NetworkCell.Write(saved.energy(), saved.energy().exactAmount().subtract(debit))),
            debit, () -> current.getAsBoolean() && transformer.isCurrent(saved, transformer.stepUpNow()))) return 0;
        transformer.markNetworkChanged();
        int accepted = target.receiveEnergy(offered, false);
        if (accepted < offered) {
            var fresh = transformer.snapshot(transformer.stepUpNow()).orElse(null);
            // Removed during callback. The original comment said "reserved energy stays with
            // the removed source". This is not accurate: the debit is already committed, and no
            // refund follows. The caller books (offered - delivered) as loss.
            if (fresh == null) return offered;
            var refund = FeLedger.eu(offered - accepted); var escrow = new NetworkCell(refund);
            if (!NetworkCell.commit(List.of(new NetworkCell.Write(fresh.energy(), fresh.energy().exactAmount().add(refund)),
                    new NetworkCell.Write(escrow.quote(), EnergyAmount.ZERO)), EnergyAmount.ZERO, () -> true))
                throw new IllegalStateException("Transformer refund lease changed");
            transformer.markNetworkChanged();
        }
        return accepted;
    }

    /**
     * Melts conductors: removes each planned block that still has the planned
     * block type, and plays smoke and sound. The method removes each melted
     * conductor from the conductor index at once, so no later route in this
     * tick uses it.
     *
     * @return the removed positions. The caller must give them to
     *         {@code forget} and {@code apply}.
     */
    private Set<BlockPos> fuseConductors(ServerLevel level, WorldGrid grid, Map<BlockPos, ResourceLocation> fusePlan) {
        var removed = new HashSet<BlockPos>();
        var cableBreakSound = MachinePresentation.cableBreakSound();
        for (var fuse : fusePlan.entrySet()) {
            var at = fuse.getKey();
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null || !BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(at).getBlock()).equals(fuse.getValue())) continue;
            if (level.removeBlock(at, false)) {
                grid.conductors.remove(point(at)); grid.contactOrder.remove(point(at));
                grid.accessibilityTick = Long.MIN_VALUE;
                topology.changed(level, at); removed.add(at);
                fusedWires = Math.incrementExact(fusedWires);
                double cx = at.getX() + 0.5, cy = at.getY() + 0.5, cz = at.getZ() + 0.5;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, cx, cy, cz, 30, 0.1, 0.1, 0.1, 0.05);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, cx, cy, cz, 15, 0.15, 0.15, 0.15, 0.03);
                if (cableBreakSound != null) {
                    level.playSound(null, at, cableBreakSound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
        return removed;
    }

    /**
     * Removes the insulation of each planned conductor that is still loaded.
     * The block stays.
     *
     * @return the changed positions. The caller forgets them, and the topology
     *         observes them again.
     */
    private Set<BlockPos> stripInsulation(ServerLevel level, WorldGrid grid, Map<BlockPos, ResourceLocation> stripPlan) {
        if (!IndependentEnergyMode.enetCableMeltdown()) return Set.of();
        var stripped = new HashSet<BlockPos>();
        for (var entry : stripPlan.entrySet()) {
            var at = entry.getKey();
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null) continue;
            var be = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
            if (be instanceof IEnergyConductor conductor) {
                conductor.removeInsulation();
                stripped.add(at);
            }
        }
        return stripped;
    }

    /** The (source, sink) pair of a delivery. Shock damage is grouped by this key. */
    private record ShockRoute(BlockPos source, BlockPos receiver) { }
    /**
     * Collects shock damage for living, non-spectator entities within 1.5 blocks
     * of each exposed conductor. It only collects. {@code settle} applies the
     * damage at the end of the settlement.
     *
     * @param shockPlan exposed conductor position to energy above the insulation absorption, in EU
     */
    private void collectShockDamage(ServerLevel level, WorldGrid grid, ShockRoute route, Map<BlockPos, Double> shockPlan) {
        if (!IndependentEnergyMode.enetShockDamage()) return;
        for (var entry : shockPlan.entrySet()) {
            var at = entry.getKey();
            double shockEnergy = entry.getValue();
            var origin = net.minecraft.world.phys.Vec3.atCenterOf(at);
            var box = new AABB(origin.x - 1.5, origin.y - 1.5, origin.z - 1.5,
                origin.x + 1.5, origin.y + 1.5, origin.z + 1.5);
            var nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !entity.isSpectator());
            for (var entity : nearby) {
                grid.shocks.add(route, entity, shockEnergy);
            }
        }
    }

    /**
     * Maps a packet size in EU to a tier: up to 32 is 0, up to 128 is 1, up to
     * 512 is 2, up to 2048 is 3, up to 8192 is 4, then one tier per factor 4,
     * capped at 13. Note: no code in this class calls this method.
     */
    private static int tierFromPower(long power) {
        if (power <= 32) return 0;
        if (power <= 128) return 1;
        if (power <= 512) return 2;
        if (power <= 2048) return 3;
        if (power <= 8192) return 4;
        int tier = 5;
        long threshold = 32768;
        while (threshold < power && tier < 13) { threshold *= 4; tier++; }
        return tier;
    }

    /**
     * Damages and pushes living entities within 6.5 blocks of an exploded sink.
     * {@link SmallBlastField} computes the damage per entity and treats each
     * non-air block (and each unloaded block) as an obstacle. The field is
     * sampled before {@code applySmallBlockBlast} removes blocks.
     */
    private static void applySmallEntityBlast(ServerLevel level, BlockPos center) {
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(center);
        var box = new net.minecraft.world.phys.AABB(origin.x - 6.5, origin.y - 6.5, origin.z - 6.5,
            origin.x + 6.5, origin.y + 6.5, origin.z + 6.5);
        var entities = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
            entity -> entity.isAlive() && !entity.isSpectator());
        if (entities.isEmpty()) return;
        // Sample before neighbour destruction: glass and stone shields both
        // obstruct the measured field even when the glass is then removed.
        var field = new SmallBlastField(origin.x, origin.y, origin.z, (x, y, z) -> {
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            return chunk == null || !chunk.getBlockState(new BlockPos(x, y, z)).isAir();
        });
        var source = level.damageSources().explosion(null, null);
        for (var entity : entities) {
            int damage = field.damageAt(entity.getX(), entity.getY(), entity.getZ());
            if (damage == 0 || !entity.hurt(source, damage)) continue;
            var direction = entity.position().subtract(origin).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(damage * 0.021875)));
            entity.hurtMarked = true;
        }
    }
    /**
     * Removes weak blocks next to an exploded sink.
     *
     * <p>Candidates are the 18 face and edge neighbours (squared distance 1 or 2).
     * A block is removed when it is breakable and either the registry marks it
     * as measured, or its explosion resistance is in {@code [0, 1]}. Each drop
     * is kept with probability 3/4, from {@code blockDropRandom}.
     *
     * @param removed output: the method adds each removed position
     */
    private void applySmallBlockBlast(ServerLevel level, WorldGrid grid, BlockPos center, Set<BlockPos> removed) {
        // Independent block-only hypothesis from frozen material/position
        // observations: the 18 face/edge neighbours, evaluated individually.
        // The resistance cutoff matches the measured vanilla materials. Values
        // between 1 and 1.5 and other modded blocks remain unverified.
        // R25's new 272-block sample retained 207 dirt items, supporting the frozen
        // three-in-four drop hypothesis for this small blast. Vanilla loot
        // rules still decide the item, and doTileDrops remains authoritative.
        // Entity fields run first. Sounds and other strengths remain separate.
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            int distanceSquared = dx * dx + dy * dy + dz * dz;
            // 0 is the center. 3 is a corner. Keep faces (1) and edges (2).
            if (distanceSquared == 0 || distanceSquared > 2) continue;
            var at = center.offset(dx, dy, dz);
            var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null) continue;
            var state = chunk.getBlockState(at);
            if (state.isAir() || state.getDestroySpeed(level, at) < 0) continue;
            var type = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            float resistance = state.getBlock().getExplosionResistance();
            if (!EnergyNodeRegistry.measuredBlast(type)
                    && (!Float.isFinite(resistance) || resistance < 0 || resistance > 1.0F)) continue;
            var drops = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                ? net.minecraft.world.level.block.Block.getDrops(state, level, at,
                    chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK), null, net.minecraft.world.item.ItemStack.EMPTY)
                : List.<net.minecraft.world.item.ItemStack>of();
            if (level.removeBlock(at, false)) {
                grid.conductors.remove(point(at));
                grid.contactOrder.remove(point(at));
                grid.accessibilityTick = Long.MIN_VALUE;
                grid.machines.remove(at);
                grid.initialGeneratorContacts.remove(at);
                topology.changed(level, at);
                removed.add(at);
                blastBlocks = Math.incrementExact(blastBlocks);
                // nextInt(4) != 0 is true for 3 of 4 values: keep 3/4 of the drops.
                for (var drop : drops) if (blockDropRandom.nextInt(4) != 0)
                    net.minecraft.world.level.block.Block.popResource(level, at, drop);
            }
        }
    }
    /**
     * Tells whether a set of ports still matches the world. A commit runs only
     * when this method returns {@code true}.
     *
     * <p>For each port it checks:
     * <ul>
     *   <li>a loaded, ticking and ready chunk</li>
     *   <li>the same tile and the same block state</li>
     *   <li>the same emitters</li>
     *   <li>for a demand source, the same output faces</li>
     *   <li>for a component host, the same face policy</li>
     *   <li>for a transformer, a current snapshot</li>
     *   <li>for other ports, the same capacity, quote and packet policy</li>
     * </ul>
     *
     * <p>An exception for one port isolates its chunk and returns {@code false}.
     */
    private boolean valid(ServerLevel level, WorldGrid grid, ConductorRegistry.Snapshot snapshot, List<Port> ports) {
        // The independent published registry intentionally preserves one final
        // packet after a wire edit, as measured. Its lease is still current.
        // Source/sink identity, all fresh quotes and chunk access remain mandatory.
        if (!accessible(level, grid) || !grid.conductors.isCurrent(snapshot)) return false;
        for (var port : ports) {
            try {
            var chunk = level.getChunkSource().getChunkNow(port.position.getX() >> 4, port.position.getZ() >> 4);
            if (chunk == null || !level.shouldTickBlocksAt(ChunkPos.asLong(port.position))
                || !topology.chunkReady(level, ChunkPos.asLong(port.position)) || port.tile.isRemoved()
                || chunk.getBlockEntity(port.position, LevelChunk.EntityCreationType.CHECK) != port.tile
                || chunk.getBlockState(port.position) != port.state) return false;
            if (!port.emitterPositions.equals(emissionPositions(port.tile))) return false;
            if (port.tile instanceof DemandEnergySource demand && (port.outputs & ~demand.outputFaces()) != 0) return false;
            if (port.tile instanceof EnergyComponentHost host) {
                var policy = host.energyComponent().ports();
                int outputs = port.quote.outputEnabled() ? policy.outputs() : 0;
                int faces = conductorFaces(port.tile);
                if (port.inputs != (policy.inputs() & faces) || port.outputs != (outputs & faces)) return false;
            }
            if (port.transformer != null) {
                var transformer = (IndependentTransformerBlockEntity) port.tile;
                if (!transformer.validMode() || !transformer.isCurrent(port.transformer, transformer.stepUpNow())) return false;
            } else if (ownedCapacity(port.tile) != port.capacity
                    || !port.storage.scexNetworkQuote().equals(port.quote)
                    || !port.policy().equals(packetPolicy(port.tile, BuiltInRegistries.BLOCK.getKey(port.state.getBlock()), port.quote))) return false;
            } catch (RuntimeException error) {
                topology.isolateChunk(level, port.position, error);
                return false;
            }
        }
        return true;
    }
    /**
     * Returns the blocks whose faces carry energy for a tile: the contact
     * positions of a multi-block source, else the tile position only.
     */
    private static List<BlockPos> emissionPositions(BlockEntity tile){
        return tile instanceof com.miophas.singularity_iteration.core.api.energy.IEnergyEmissionSource reactor
            ?reactor.electricalContactPositions():List.of(tile.getBlockPos());
    }
    /**
     * Returns the input limit of a sink in EU per packet from its tier, or
     * {@code null} when the tile has no tier. A packet above this limit can
     * explode the sink.
     */
    private static Long tierReceiverLimit(BlockEntity tile) {
        if (tile instanceof EnergyComponentHost host) return host.energyComponent().tier().getPowerRating();
        if (tile instanceof com.miophas.singularity_iteration.core.api.machine.IEnergyBlock block) return block.getEffectiveCableTier().getPowerRating();
        return null;
    }
    /**
     * Nominal per-packet offer ceiling. A static profile keeps its registered
     * packet. A dynamic generator uses its live source tier. Partial deliveries
     * keep their actual size in the trace: effects and meter voltage must never
     * substitute this ceiling for the energy that was actually transferred.
     */
    private static long portPacket(BlockEntity tile, ResourceLocation type,
            CustomEUEnergyStorage.NetworkQuote quote) {
        long fallback = EnergyNodeRegistry.generatorPacket(type,
            tierReceiverLimit(tile) != null ? tierReceiverLimit(tile)
                : EnergyNodeRegistry.storagePacket(type, Math.max(0L, quote.output() > 0 ? quote.output() : quote.maxExtract())));
        if (!EnergyNodeRegistry.dynamicTier(type) || !(tile instanceof IEnergySource source)) return fallback;
        int tier = source.getSourceTier();
        if (tier < 0) return fallback;
        long packet = (long) Math.floor(EnergyNetGlobal.getPowerFromTier(tier));
        return packet > 0 ? packet : fallback;
    }
    /**
     * Returns the packet policy of a tile.
     * <ul>
     *   <li>An {@link IEnergyPacketSource} supplies its own policy. It must not be {@code null}.</li>
     *   <li>Otherwise the packet size is {@code portPacket}, at least 1 EU.</li>
     *   <li>The packet count is the source packet count, clamped to
     *       {@code [0, EnergyPacketPolicy.MAX_PACKETS_PER_ROUND]}, or 1 for a non-source.</li>
     *   <li>Generators and demand sources can send a partial last packet.
     *       Storage blocks send full packets only.</li>
     * </ul>
     */
    private static EnergyPacketPolicy packetPolicy(BlockEntity tile, ResourceLocation type,
            CustomEUEnergyStorage.NetworkQuote quote) {
        if (tile instanceof IEnergyPacketSource provider)
            return java.util.Objects.requireNonNull(provider.getEnergyPacketPolicy(), "energy packet policy");
        int count = tile instanceof IEnergySource source
            ? Math.max(0, Math.min(EnergyPacketPolicy.MAX_PACKETS_PER_ROUND, source.getPacketCount())) : 1;
        boolean partial = tile instanceof DemandEnergySource
            || tile instanceof EnergyComponentHost host && host.energyComponent().ports().generator()
            || tile instanceof AbstractEnergyBlockEntity block && block.isPowerSource();
        return new EnergyPacketPolicy(Math.max(1, portPacket(tile, type, quote)), count, partial);
    }
    /**
     * Returns the breakdown limit in EU per packet of a cable block from its
     * cable tier, or {@code null} when the block is not a tiered cable. A packet
     * above this limit melts the cable.
     */
    private static Long tierFuseLimit(ResourceLocation type) {
        var block = BuiltInRegistries.BLOCK.get(type);
        if (block instanceof com.miophas.singularity_iteration.core.api.energy.ICableBlock cable)
            return cable.getCableTier().getConductorBreakdownEnergy();
        return null;
    }
    /** Converts a block position to a conductor registry position. */
    private static ConductorRegistry.Position point(BlockPos at) { return new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ()); }
    /**
     * Converts the route table of a finished plan into distributor domains.
     *
     * <p>A <b>domain</b> is one group of routes that share energy. There is one
     * domain per conductor component (key {@code >= 0}), and one domain per
     * direct face contact (key {@code < 0}, see {@code NativeRoutePlan.advance}).
     *
     * <p>Each domain entry is one (source, emitter group). Its key is
     * {@code 7 * source + slot}. {@code key / 7} is the source index in the
     * window. {@code key % 7} is a face ordinal (0..5) for a separate contact
     * of a two-contact generator, or 6 for all faces of the source together.
     *
     * <p>For each entry the method builds:
     * <ul>
     *   <li>the loss to each sink ({@link RouteCosts})</li>
     *   <li>the eligible sinks: reachable, and the whole loss is below the packet
     *       size of the source</li>
     *   <li>a sink priority order ({@link ReceiverOrder}). A separate contact
     *       entry orders sinks by loss. A generator with two or more contacts in
     *       one component orders sinks by {@code contactOrder}. Others keep the
     *       window order.</li>
     * </ul>
     * <p>Entries of sources with a zero window offer are left out.
     *
     * @param effectPaths output: per domain, the path arrays of its entries, in
     *        the same order as the domain entries. {@code settlePlan} uses it to
     *        find the path of each delivery.
     */
    private List<DomainDistributor.Domain> domains(NativeRoutePlan plan, List<Port> sources, List<Port> sinks,
            ConductorRegistry.Snapshot graph, int[] receivers, long worldTime, WorldGrid grid,
            List<List<ConductorRegistry.Path[]>> effectPaths, List<EnergyAmount> budgets) {
        var result = new ArrayList<DomainDistributor.Domain>();
        for (var domainItem : plan.grouped.entrySet()) {
            var domain = new LinkedHashMap<Long, Map.Entry<long[], ConductorRegistry.Path[]>>();
            for (var entry : domainItem.getValue().entrySet()) {
                int source = (int) (entry.getKey() / 7);
                if (!budgets.get(source).isZero())
                    domain.put(entry.getKey(), entry.getValue());
            }
            if (domain.isEmpty()) continue;
            var selectedPaths = new ArrayList<ConductorRegistry.Path[]>();
            int[] ids = new int[domain.size()]; int[][] priorities = new int[domain.size()][];
            var routes = new ArrayList<RouteCosts>(); int index = 0; boolean shared = false;
            for (var entry : domain.entrySet()) {
                int source = (int) (entry.getKey() / 7); long[] losses = entry.getValue().getKey();
                selectedPaths.add(entry.getValue().getValue());
                // Slot 0..5: one separate face contact. Slot 6: all faces of the source together.
                boolean contactEntry = entry.getKey() % 7 != 6; shared |= contactEntry;
                // losses[i] == -1 means "sink i not reachable" (see NativeRoutePlan.record).
                RouteCosts costs = new RouteCosts() {
                    @Override public boolean reaches(int contact) { return losses[contact] >= 0; }
                    @Override public long lossMilliTo(int contact) {
                        if (!reaches(contact)) throw new IllegalArgumentException("Unreachable endpoint");
                        return losses[contact];
                    }
                };
                boolean[] eligible = new boolean[sinks.size()]; int eligibleCount = 0;
                for (int receiver = 0; receiver < eligible.length; receiver++) {
                    // Full connected receivers retain their random offset.
                    eligible[receiver] = costs.reaches(receiver) && costs.wholeLossTo(receiver) < sources.get(source).packet;
                    if (eligible[receiver]) eligibleCount++;
                }
                ids[index] = source; routes.add(costs);
                int[] registration = receivers;
                if (eligibleCount > 1 && contactEntry) {
                    registration = Arrays.stream(receivers).boxed().sorted(java.util.Comparator.comparingLong(
                        receiver -> costs.reaches(receiver) ? costs.lossMilliTo(receiver) : Long.MAX_VALUE))
                        .mapToInt(Integer::intValue).toArray();
                }
                if (eligibleCount > 1 && !contactEntry && domainItem.getKey() >= 0
                        && EnergyNodeRegistry.isGenerator(BuiltInRegistries.BLOCK.getKey(sources.get(source).state.getBlock()))) {
                    var sourcePort = sources.get(source);
                    ConductorRegistry.Position contact = null; int contacts = 0;
                    for (Direction side : Direction.values()) {
                        var near = point(sourcePort.position.relative(side));
                        if ((sourcePort.outputs & (1 << side.ordinal())) != 0 && graph.contains(near)
                                && graph.permits(near, side.getOpposite().get3DDataValue()) && graph.componentOf(near) == domainItem.getKey()) { contact = near; contacts++; }
                    }
                    if (contacts > 1) {
                        var order = grid.contactOrder.receivers(point(sourcePort.position), contact);
                        if (!order.isEmpty()) {
                            var ranks = new HashMap<ConductorRegistry.Position, Integer>();
                            for (int i = 0; i < order.size(); i++) ranks.put(order.get(i), i);
                            registration = Arrays.stream(receivers).boxed().sorted(java.util.Comparator.comparingInt(
                                receiver -> ranks.getOrDefault(point(sinks.get(receiver).position), Integer.MAX_VALUE)))
                                .mapToInt(Integer::intValue).toArray();
                        }
                    }
                }
                priorities[index++] = ReceiverOrder.create(registration, eligible, worldTime, selectionRandom);
            }
            result.add(shared ? DomainDistributor.Domain.withSharedSourceContacts(ids, routes, priorities)
                : new DomainDistributor.Domain(ids, routes, priorities));
            effectPaths.add(selectedPaths);
        }
        return result;
    }
    /**
     * Only topology identity is retained between ticks, never a mutable energy quote.
     * A plan or a routing view stores these records, never a {@code Port}.
     */
    private record RouteEndpoint(BlockPos position, BlockEntity tile, int inputs, int outputs,
            List<BlockPos> emitters, EnergyPacketPolicy packets) {
        static RouteEndpoint of(Port port) {
            return new RouteEndpoint(port.position, port.tile, port.inputs, port.outputs, List.copyOf(port.emitterPositions), port.policy());
        }
        /*
         * Routing identity: what the route plan depends on. Packet size and the
         * partial-packet flag are allocation inputs, read live from the Port at
         * settlement. Dynamic-tier generators (kinetic, steam, ...) change packet
         * size almost every tick. Including it here rebuilt the global routing
         * view and discarded every in-progress plan each tick, so large networks
         * never finished a plan and stopped charging. Packet count still sizes
         * the window's effect bound, so it stays part of the identity.
         */
        boolean matches(Port port) {
            return port != null && tile == port.tile && position.equals(port.position)
                && inputs == port.inputs && outputs == port.outputs && emitters.equals(port.emitterPositions)
                && packets.packetCount() == port.policy().packetCount();
        }
    }
    /**
     * One start point of a native route search: the block in front of one open
     * output face of one emitter of a source.
     * {@code source}: source index in the window. {@code output}: face ordinal.
     * {@code position}: the block in front of the face. {@code component}: its
     * conductor component, or -1 when it is not a usable conductor.
     * {@code emitter}: the entry key {@code 7 * source + slot} (see {@code domains}).
     */
    private record NativeOrigin(int source, int output, ConductorRegistry.Position position, int component, long emitter) { }
    /**
     * The routing identities of all sources and all sinks of a grid, for one
     * conductor snapshot and one routing generation. {@code contacts} indexes
     * which sinks touch which conductor and component. Plans of the same
     * generation share this view, so a new window does not rebuild the index.
     */
    private record RoutingView(ConductorRegistry.Snapshot graph, List<RouteEndpoint> sources,
            List<RouteEndpoint> sinks, ReceiverContacts contacts, long portsGeneration) {
        /** O(1) check: same snapshot object and same generation. */
        boolean isCurrent(ConductorRegistry.Snapshot snapshot, long generation) {
            return graph == snapshot && portsGeneration == generation;
        }
        /** O(ports) check: same snapshot and the same routing identity for each list element. */
        boolean matches(ConductorRegistry.Snapshot snapshot, List<Port> sourcePorts, List<Port> sinkPorts) {
            return graph == snapshot && NativeRoutePlan.matches(sources, sourcePorts) && NativeRoutePlan.matches(sinks, sinkPorts);
        }
    }

    /**
     * Complete a bounded source/receiver window before permitting any transfer.
     *
     * <p><b>Window.</b> The constructor selects the sources
     * {@code allSources[firstSource, nextSource)} (at most
     * {@value #MAX_PLAN_SOURCES}, and within the vertex limits), and up to
     * {@code receiverLimit} reachable sinks starting at reachable ordinal
     * {@code firstReceiver}. The sink window does not wrap. When it reaches the
     * last reachable sink, {@code nextReceiver} is 0.
     *
     * <p><b>Route table.</b> {@code grouped} maps domain key to entry key to a
     * pair of arrays, one slot per window sink: the best loss (milli-EU, -1 for
     * unreachable) and the path ({@code null} for a direct contact).
     *
     * <p><b>Resumption.</b> {@link #advance} fills the table one candidate at a
     * time. Its state is {@code originIndex}, {@code candidates},
     * {@code paths} and {@code candidateReady}. When a budget is empty, it
     * returns {@code false} with this state intact, and the next call continues
     * at the same candidate. A candidate is never recorded twice.
     *
     * <p><b>Bounds.</b>
     * <ul>
     *   <li>{@code packetLimit >= 1} and {@code receiverLimit >= 1}, else the
     *       constructor throws.</li>
     *   <li>{@code effectVertices * 2 * packetLimit * receiverLimit} is at most
     *       {@link #MAX_NATIVE_PATH_VISITS_PER_TICK}. The effect work of one
     *       window therefore fits into a fresh tick.</li>
     *   <li>{@code 0 <= firstReceiver < count} when {@code count > 0}.</li>
     * </ul>
     *
     * <p>A plan holds routing identities only. It must be used only while
     * {@code graph} is the current snapshot and {@code portsGeneration} is the
     * current generation.
     */
    private static final class NativeRoutePlan {
        /** The conductor snapshot that this plan was built for. */
        final ConductorRegistry.Snapshot graph;
        /** {@code allSources}, {@code allSinks}: the full lists of the routing view. {@code sources}, {@code sinks}: this window. */
        final List<RouteEndpoint> allSources, allSinks, sources, sinks;
        /** All search start points of the window sources, in source order. */
        final List<NativeOrigin> origins = new ArrayList<>();
        /** Sink contact index for the window sinks only. Sink indexes are window indexes. */
        final ReceiverContacts contacts;
        /** Route table: domain key, then entry key, then (losses, paths) per window sink. */
        final LinkedHashMap<Long, LinkedHashMap<Long, Map.Entry<long[], ConductorRegistry.Path[]>>> grouped = new LinkedHashMap<>();
        /**
         * Window bounds and cursor values for the next window. {@code nextSource}
         * and {@code nextReceiver} are 0 after the last element.
         * {@code packetLimit}: packets per source in this window ({@code >= 1}).
         */
        final int firstSource, nextSource, firstReceiver, nextReceiver, packetLimit;
        /** The routing generation that this plan was built for. */
        final long portsGeneration;
        /** Index of the origin in progress. {@code origins.size()} means complete. */
        int originIndex;
        /** Route array cells this plan allocated (one per window sink per new entry). */
        long arrayCells;
        /** Sink candidates of the current origin, or {@code null} before the first candidate. */
        ReceiverContacts.Candidates candidates;
        /** Search result from the current origin, or {@code null} when not searched yet. */
        ConductorRegistry.RouteIndex paths;
        /** True when {@code candidates} points at a candidate that is not recorded yet. */
        boolean candidateReady;

        /**
         * Builds a window that starts at source index {@code first}. It does no
         * route search. Call {@link #advance} for that.
         *
         * @param first a source index in {@code [0, sourcePorts.size())}
         * @throws IllegalStateException when one source alone exceeds the effect bound
         */
        NativeRoutePlan(ConductorRegistry.Snapshot graph, List<Port> sourcePorts, List<Port> sinkPorts,
                int first, WorldGrid grid) {
            this.graph = graph;
            var view = grid.routingView;
            // settle validates graph and endpoint identities before constructing
            // a window. Keep the shape guard here as well: a stale list after
            // removal must never produce an out-of-bounds window slice.
            if (view != null && view.isCurrent(graph, grid.portsGeneration)
                    && view.sources.size() == sourcePorts.size()
                    && view.sinks.size() == sinkPorts.size()) {
                // O(1) generation check passed — reuse existing view
            } else {
                var sourceIds = sourcePorts.stream().map(RouteEndpoint::of).toList();
                var sinkIds = sinkPorts.stream().map(RouteEndpoint::of).toList();
                var contactIndex = new ReceiverContacts(graph, sinkIds.stream()
                    .map(p -> new ReceiverContacts.Endpoint(point(p.position), p.inputs)).toList());
                grid.routingView = view = new RoutingView(graph, sourceIds, sinkIds, contactIndex, grid.portsGeneration);
            }
            allSources = view.sources; allSinks = view.sinks;
            firstSource = first;
            portsGeneration = grid.portsGeneration;
            int end = first; long vertices = 0, effectVertices = 0;
            while (end < allSources.size() && end - first < MAX_PLAN_SOURCES) {
                var next = origins(allSources.get(end), end - first, graph, grid);
                long weight = 0, effectWeight = 0;
                var entries = new HashSet<Map.Entry<Integer, Long>>();
                for (var origin : next) if (origin.component >= 0) {
                    // Search admission uses the compressed search size. Effect
                    // path visits stay bounded by physical conductor count.
                    weight += graph.searchWeight(origin.position);
                    if (entries.add(Map.entry(origin.component, origin.emitter))) effectWeight += graph.componentSize(origin.position);
                }
                // Always admit one source, including a multi-block reactor.
                // Its finite contact set can then be searched over several ticks.
                if (end > first && (vertices + weight > MAX_PLAN_ROUTE_VERTICES
                        || (effectVertices + effectWeight) * 2 > MAX_NATIVE_PATH_VISITS_PER_TICK)) break;
                vertices += weight; effectVertices += effectWeight; origins.addAll(next); end++;
            }
            sources = List.copyOf(allSources.subList(first, end));
            int declaredPackets = sources.stream().mapToInt(p -> p.packets.packetCount()).max().orElse(1);
            // Math.max(1, ...) avoids a division by 0 for a window with no wired origin.
            packetLimit = (int) Math.min(declaredPackets, MAX_NATIVE_PATH_VISITS_PER_TICK / Math.max(1, effectVertices * 2));
            if (packetLimit < 1) throw new IllegalStateException("Single packet path exceeds its bound");
            // Wrap to 0 after the last source, so nativeCursor stays in [0, size).
            nextSource = end == allSources.size() ? 0 : end;
            // Mark every sink that one origin touches directly or shares a component with.
            var globalContacts = view.contacts;
            var reachable = new java.util.BitSet(allSinks.size());
            var components = new HashSet<Integer>();
            for (var origin : origins) {
                for (int receiver : globalContacts.at(origin.position)) reachable.set(receiver);
                if (components.add(origin.component))
                    for (var contact : globalContacts.inComponent(origin.component)) reachable.set(contact.receiver());
            }
            int count = reachable.cardinality();
            // The stored cursor can be from an older, larger sink set. floorMod brings it
            // into [0, count). The count == 0 test avoids floorMod by 0.
            firstReceiver = count == 0 ? 0 : Math.floorMod(grid.receiverCursors.getOrDefault(sources.getFirst().position, 0), count);
            int receiverLimit = (int) Math.min(MAX_PLAN_RECEIVERS,
                MAX_NATIVE_PATH_VISITS_PER_TICK / Math.max(1, effectVertices * 2 * packetLimit));
            // Bound effect work by actual policy, including addon multi-packet
            // sources. Large paths admit fewer packets instead of stalling forever.
            if (receiverLimit < 1) throw new IllegalStateException("Single-source path window exceeds its bound");
            // Take reachable sinks in index order, skip the first firstReceiver of them,
            // and stop at receiverLimit. The selection does not wrap to the start.
            var selected = new ArrayList<RouteEndpoint>(); int ordinal = 0;
            for (int receiver = reachable.nextSetBit(0); receiver >= 0; receiver = reachable.nextSetBit(receiver + 1)) {
                if (ordinal++ < firstReceiver) continue;
                selected.add(allSinks.get(receiver));
                if (selected.size() == receiverLimit) break;
            }
            sinks = List.copyOf(selected);
            nextReceiver = firstReceiver + sinks.size() == count ? 0 : firstReceiver + sinks.size();
            contacts = new ReceiverContacts(graph, sinks.stream()
                .map(p -> new ReceiverContacts.Endpoint(point(p.position), p.inputs)).toList());
        }

        /**
         * Returns the search start points of one source: one per (emitter, open
         * output face), except faces that point into another emitter of the same
         * source.
         *
         * <p>Normally all origins of a source share entry slot 6. A single-block
         * generator gets one separate slot per face (slot = face ordinal) only
         * when it has exactly two wired output faces, both are its initial
         * contacts, and both are in the same component. The two contacts then
         * act as two separate outputs.
         *
         * @param id the source index in the window
         */
        private static List<NativeOrigin> origins(RouteEndpoint source, int id,
                ConductorRegistry.Snapshot graph, WorldGrid grid) {
            var initial = grid.initialGeneratorContacts.getOrDefault(source.position, Set.of());
            int count = 0, component = -1; boolean same = true;
            for (var side : Direction.values()) {
                if ((source.outputs & (1 << side.ordinal())) == 0) continue;
                var at = source.position.relative(side); var point = point(at);
                if (!graph.contains(point) || !graph.permits(point, side.getOpposite().ordinal())) continue;
                count++; if (!initial.contains(at)) same = false;
                int found = graph.componentOf(point);
                if (component < 0) component = found; else if (component != found) same = false;
            }
            boolean separate = source.emitters.size() == 1 && initial.size() == 2 && count == 2 && same;
            var result = new ArrayList<NativeOrigin>();
            for (var emitter : source.emitters) for (var output : Direction.values()) {
                if ((source.outputs & (1 << output.ordinal())) == 0) continue;
                var at = emitter.relative(output);
                if (source.emitters.contains(at)) continue;
                var position = point(at);
                boolean wired = graph.contains(position) && graph.permits(position, output.getOpposite().ordinal());
                result.add(new NativeOrigin(id, output.ordinal(), position, wired ? graph.componentOf(position) : -1,
                    7L * id + (separate ? output.ordinal() : 6)));
            }
            return result;
        }
        /** Element-wise routing identity check of two lists of the same order. A {@code null} port does not match. */
        private static boolean matches(List<RouteEndpoint> saved, List<Port> current) {
            if (saved.size() != current.size()) return false;
            for (int i = 0; i < saved.size(); i++) if (!saved.get(i).matches(current.get(i))) return false;
            return true;
        }
        /** Checks the snapshot and the full lists. */
        boolean matches(ConductorRegistry.Snapshot snapshot, List<Port> sourcePorts, List<Port> sinkPorts) {
            return graph == snapshot && matches(allSources, sourcePorts) && matches(allSinks, sinkPorts);
        }
        /** Checks the window lists only. {@code settlePlan} uses it before a transfer. */
        boolean matchesSelected(List<Port> sourcePorts, List<Port> sinkPorts) {
            return matches(sources, sourcePorts) && matches(sinks, sinkPorts);
        }
        /** Returns the array cells that a first record of (domain, entry) would allocate: window sink count, or 0 when the entry exists. */
        private int newCells(long domain, long emitter) {
            var entries = grouped.get(domain);
            return entries == null || !entries.containsKey(emitter) ? sinks.size() : 0;
        }
        /**
         * Records one route. The first record of an entry allocates its arrays
         * (all losses -1). A later record replaces a slot only with a lower loss.
         * The caller must reserve {@link #newCells} first.
         */
        private void record(long domain, long emitter, int receiver, long loss, ConductorRegistry.Path path) {
            var entries = grouped.computeIfAbsent(domain, ignored -> new LinkedHashMap<>());
            var entry = entries.get(emitter);
            if (entry == null) {
                long[] losses = new long[sinks.size()]; Arrays.fill(losses, -1);
                entry = Map.entry(losses, new ConductorRegistry.Path[sinks.size()]);
                entries.put(emitter, entry); arrayCells += sinks.size();
            }
            if (entry.getKey()[receiver] < 0 || loss < entry.getKey()[receiver]) {
                entry.getKey()[receiver] = loss; entry.getValue()[receiver] = path;
            }
        }
        /**
         * Continues the route search of this window.
         *
         * <p>For each origin and each candidate sink of that origin: search once
         * from the origin (lazily), select the reachable sink contact with the
         * lowest loss, and record it. A direct face contact (the sink is the
         * block in front of the output face, and its opposite face accepts) also
         * gets a record in its own direct domain with loss 0.
         *
         * @return {@code true} when all origins are done. {@code false} when the
         *         search budget or the record budget is empty. The state then
         *         stays at the same candidate.
         */
        boolean advance(ConductorRegistry.SearchBudget searches, RouteBuildBudget budget) {
            while (originIndex < origins.size()) {
                var origin = origins.get(originIndex);
                if (candidates == null) candidates = contacts.candidates(origin.position, origin.component);
                if (!candidateReady) {
                    if (!candidates.next()) { originIndex++; candidates = null; paths = null; continue; }
                    candidateReady = true;
                }
                int receiver = candidates.receiver(); var sink = sinks.get(receiver);
                // A port never routes energy to itself.
                boolean distinct = sources.get(origin.source).tile != sink.tile;
                // Direction ordinals come in opposite pairs (0/1, 2/3, 4/5), so
                // output ^ 1 is the opposite face: the sink face that touches the source.
                boolean direct = distinct && candidates.direct() && (sink.inputs & (1 << (origin.output ^ 1))) != 0;
                if (distinct && candidates.contactCount() > 0 && paths == null) {
                    paths = graph.routesFrom(origin.position, searches);
                    if (paths == null) return false;
                }
                long bestLoss = Long.MAX_VALUE; int bestVertex = -1;
                if (distinct) for (int i = 0; i < candidates.contactCount(); i++) {
                    int vertex = candidates.contact(i).vertex();
                    if (paths.reaches(vertex) && (bestVertex < 0 || paths.lossMilliTo(vertex) < bestLoss)) {
                        bestLoss = paths.lossMilliTo(vertex); bestVertex = vertex;
                    }
                }
                // Direct domain keys are negative and unique per (source, sink):
                // -1 - source * sinkCount - receiver, with receiver < sinkCount.
                // Component keys are >= 0, so the two key ranges never overlap.
                long directDomain = -1L - (long) origin.source * sinks.size() - receiver;
                int cells = (direct ? newCells(directDomain, 7L * origin.source + 6) : 0)
                    + (bestVertex >= 0 ? newCells(origin.component, origin.emitter) : 0);
                // Reserve before recording. On failure nothing was recorded, and
                // candidateReady stays true, so the next call retries this candidate.
                if (!budget.consumeRecord(cells)) return false;
                if (direct) record(directDomain, 7L * origin.source + 6, receiver, 0, null);
                // All faces of this receiver are considered before yielding.
                // Later origins can still improve it before the plan is ready.
                if (bestVertex >= 0) record(origin.component, origin.emitter, receiver, bestLoss, paths.path(bestVertex));
                candidateReady = false;
            }
            return true;
        }
    }

    /**
     * Per-tick limits for route building and path visits of one grid.
     * Each {@code reserve} or {@code consume} method either takes the full
     * amount and returns {@code true}, or takes nothing and returns
     * {@code false}. The remaining values never go below 0.
     */
    private static final class RouteBuildBudget {
        /** The tick of the current allowance. {@code Long.MIN_VALUE} means never reset. */
        private long tick = Long.MIN_VALUE;
        /** Route records left in this tick. Range {@code [0, MAX_ROUTE_RECORDS_PER_TICK]}. */
        private int records;
        /** Route array cells left in this tick. Range {@code [0, MAX_ROUTE_ARRAY_CELLS_PER_TICK]}. */
        private long arrayCells;
        /** Path visits left in this tick, by kind: 0 = native, 1 = FE export, 2 = FE import. */
        private final long[] pathVisits = new long[3];
        /** Refills all limits when {@code now} is a new tick. A second call in the same tick has no effect. */
        void reset(long now) {
            if (tick == now) return;
            tick = now; records = MAX_ROUTE_RECORDS_PER_TICK; arrayCells = MAX_ROUTE_ARRAY_CELLS_PER_TICK;
            pathVisits[0] = MAX_NATIVE_PATH_VISITS_PER_TICK; pathVisits[1] = 200_000; pathVisits[2] = 200_000;
        }
        /** Takes one record and {@code cells} array cells, or nothing. */
        boolean consumeRecord(int cells) {
            if (records == 0 || !reserveArrays(cells)) return false;
            records--; return true;
        }
        /** Takes {@code cells} array cells, or nothing. A negative value is refused. */
        boolean reserveArrays(long cells) {
            if (cells < 0 || arrayCells < cells) return false;
            arrayCells -= cells; return true;
        }
        /**
         * Takes {@code visits} path visits of one kind, or nothing.
         * Note: {@code settleForeign} and {@code importForeign} do not call
         * {@link #reset}. They use the allowance that the native pass reset in
         * the same settlement.
         *
         * @param kind 0 = native, 1 = FE export, 2 = FE import
         */
        boolean reserveVisits(long visits, int kind) {
            if (visits < 0 || visits > pathVisits[kind]) return false;
            pathVisits[kind] -= visits; return true;
        }
    }
    /**
     * Shuts the engine down when its server stops. It closes the topology and
     * all grids, marks the engine closed, removes it from {@code SERVERS}, drops
     * the server reference and unregisters from the event bus. The engine is
     * not usable after this call. The next server start creates a new engine.
     */
    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        if (event.getServer() != server) return;
        topology.close(); cleared(); closed = true;
        synchronized (IndependentSiEnergy.class) { SERVERS.remove(server); }
        server = null;
        NeoForge.EVENT_BUS.unregister(this);
    }
}
