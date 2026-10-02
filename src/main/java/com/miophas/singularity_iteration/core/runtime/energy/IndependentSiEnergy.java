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
 * Default SI energy engine with independently owned numeric storage and public
 * world/capability APIs. Compatibility entrypoints delegate to this same graph.
 */
public final class IndependentSiEnergy implements PlatformTopology.Observer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IndependentSiEnergy.class);
    // Endpoint cap per level. Exceeding it keeps extra endpoints disconnected and
    // warns, instead of throwing into fail() which would disable energy transfer
    // for every level until restart (large-storage worlds hit the old 4096 cap).
    private static final int ENDPOINT_LIMIT = 65_536;
    /**
     * Route construction is a server tick operation.  A dense component can
     * otherwise produce a source × receiver × contact cross product and spend
     * seconds in HashMap updates before the distributor ever runs.
     */
    private static final int MAX_ROUTE_RECORDS_PER_TICK = 16_384;
    private static final long MAX_ROUTE_ARRAY_CELLS_PER_TICK = 2_000_000L;
    private static final int MAX_PLAN_SOURCES = 32, MAX_PLAN_RECEIVERS = 128;
    private static final long MAX_PLAN_ROUTE_VERTICES = 800_000;
    private static final long MAX_NATIVE_PATH_VISITS_PER_TICK = 2_000_000;
    private static final Map<MinecraftServer, IndependentSiEnergy> SERVERS = new IdentityHashMap<>();
    private static boolean installed;
    public static boolean controls(BlockState state) {
        return com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()
            && EnergyNodeRegistry.admits(state);
    }
    public static synchronized void install() {
        if (installed) return; installed = true;
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
            if (com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()) attach(event.getServer());
        });
    }
    public static synchronized IndependentSiEnergy attach(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Attach on server thread");
        EnergyNodeRegistry.freeze();
        return SERVERS.computeIfAbsent(server, IndependentSiEnergy::new);
    }
    public static synchronized IndependentSiEnergy current(MinecraftServer server) { return SERVERS.get(server); }
    public static void changed(AbstractEnergyBlockEntity tile) {
        changed((BlockEntity) tile);
    }
    public static void changed(BlockEntity tile) {
        if (tile.getLevel() instanceof ServerLevel level) {
            var engine = current(level.getServer()); if (engine != null) engine.topology.changed(level, tile.getBlockPos());
        }
    }
    private static CustomEUEnergyStorage ownedStorage(BlockEntity tile) {
        return tile instanceof DemandEnergySource source ? source.ownedEnergy()
            : tile instanceof EnergyComponentHost host ? host.energyComponent().internalStorage()
            : tile instanceof AbstractEnergyBlockEntity machine ? machine.getEnergyStorageInternal() : null;
    }
    private static long ownedCapacity(BlockEntity tile) {
        return tile instanceof DemandEnergySource source ? source.ownedEnergy().getCapacity()
            : tile instanceof EnergyComponentHost host ? host.energyComponent().capacity()
            : ((AbstractEnergyBlockEntity)tile).getEffectiveCapacity();
    }

    private static FeLedger.OutputQuote outputBudget(BlockEntity tile) {
        if (tile instanceof EnergyComponentHost host) return host.energyComponent().internalLedger().quoteNativeOutput();
        return tile instanceof AbstractEnergyBlockEntity block ? block.scexFeBridge().quoteNativeOutput() : null;
    }

    private static Long conductorLoss(BlockEntity tile) {
        var configured = EnergyNodeRegistry.conductorLoss(BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()));
        if (configured != null) return configured;
        if (!(tile instanceof ICableEnergyNode wire)) return null;
        double loss = wire.getConductionLoss();
        if (!Double.isFinite(loss) || loss < 0 || loss > Long.MAX_VALUE / 1000.0)
            throw new IllegalStateException("Invalid conductor loss at " + tile.getBlockPos());
        return (long)Math.ceil(loss * 1000.0);
    }

    private static int conductorFaces(BlockEntity tile) {
        int result = ConductorRegistry.ALL_FACES;
        if (tile instanceof ICableEnergyNode wire) {
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

    /** Port edits revoke stale routes immediately; no chunk is acquired or created. */
    public static void conductorPortsChanged(BlockEntity tile) {
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

    /** Color changes affect both ends of each adjacent electrical connection. */
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

    @Override public int conductorFaces(ServerLevel level, LevelChunk chunk, BlockPos at) {
        // The FULL chunk is supplied by the topology. Materializing its own saved
        // block entity does not request or keep another chunk loaded.
        return conductorFaces(chunk.getBlockEntity(at, LevelChunk.EntityCreationType.IMMEDIATE));
    }

    /** Server-authoritative mode selection through the ordinary block interaction event. */
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
        // 蹲下空手右键 = 循环切换模式（服务端权威）；普通右键不拦截，
        // 让方块的 useWithoutItem 打开界面，模式按钮走菜单通道。
        if (!event.getEntity().isShiftKeyDown()) return;
        int next = Math.floorMod(transformer.savedMode() + 1, 3);
        transformer.setSavedMode(next);
        topology.changed(level, at);
        String label = next == 0 ? "固定升压" : next == 1 ? "固定降压" : "红石自动";
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal("变压器：" + label + "；蹲下空手右键切换"), true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private MinecraftServer server;
    private final long ownerThread = Thread.currentThread().threadId();
    private final PlatformTopology topology;
    private final Map<ServerLevel, WorldGrid> worlds = new HashMap<>();
    // Record the independent seed in probe metrics so this selection stream can
    // be replayed. It is not a seed or algorithm taken from the reference mod.
    private final long selectionSeed = Long.getLong("scex.independent.selectionSeed", ThreadLocalRandom.current().nextLong());
    private final SplittableRandom selectionRandom = new SplittableRandom(selectionSeed);
    // A separate independent stream keeps visible block effects from changing
    // receiver selection. This is not the reference game's PRNG or seed.
    private final SplittableRandom blockDropRandom = new SplittableRandom(selectionSeed ^ 0x5343455844524f50L);
    private long ticks, commits, rejected, debited, credited, dissipated;
    private EnergyAmount totalDebit = EnergyAmount.ZERO, totalCredit = EnergyAmount.ZERO, totalLoss = EnergyAmount.ZERO;
    private long deliveryCount, deliveryWireVisits, fusedWires, destroyedReceivers, blastBlocks;
    private long foreignQueryFailures;
    private String lastForeignQueryFailure = "", failure = "";
    private boolean closed;
    /** Read-only public-platform diagnostics for bounded startup investigations. */
    public Map<String, Object> startupDiagnostics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalArgumentException("Wrong server context");
        var grid = worlds.get(level);
        return Map.of("topology", topology.metrics(), "ready", topology.ready(level),
            "world_grid", grid != null, "catch_up", grid != null && grid.catchUp,
            "entries", grid == null ? 0 : grid.machines.size(),
            "world_grid_failure", grid == null ? "" : grid.failure,
            "foreign_query_failures", foreignQueryFailures, "last_foreign_query_failure", lastForeignQueryFailure);
    }
    public record Metrics(long ticks, long commits, long rejected, long debited, long credited, long dissipated,
                          int dimensions, int endpoints, boolean closed, String failure, long selectionSeed,
                          long deliveryCount, long deliveryWireVisits, long fusedWires, long destroyedReceivers, long blastBlocks,
                          long debitedFraction, long creditedFraction, long dissipatedFraction) { }
    /** Routing work, separate from balances so diagnostics cannot renew a quote. */
    public record RoutingMetrics(long topologyGenerations, long plansBuilt, long plansResumed, long budgetYields) { }
    public RoutingMetrics routingMetrics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalStateException("Wrong routing diagnostics thread");
        var grid = worlds.get(level);
        return grid == null ? new RoutingMetrics(0, 0, 0, 0)
            : new RoutingMetrics(grid.portsGeneration, grid.plansBuilt, grid.plansResumed, grid.budgetYields);
    }
    public com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings.Snapshot performanceMetrics(ServerLevel level) {
        if (level.getServer() != server || !server.isSameThread()) throw new IllegalStateException("Wrong diagnostics context");
        var grid = worlds.get(level);
        return grid == null ? new com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings.Snapshot(0,0,0,0,0) : grid.timings.snapshot();
    }
    private record Port(BlockEntity tile, BlockPos position, BlockState state,
                        CustomEUEnergyStorage storage, CustomEUEnergyStorage.NetworkQuote quote,
                        long capacity, int inputs, int outputs, long packet, int packets, boolean partialPackets,
                        IndependentTransformerBlockEntity.Snapshot transformer, FeLedger.OutputQuote outputBudget,
                        EnergyAmount potential, List<BlockPos> emitterPositions) {
        EnergyAmount offered() {
            if (potential != null) return outputBudget == null ? potential : outputBudget.limitOffer(potential);
            return outputBudget == null ? quote.exactAmount() : outputBudget.limitOffer(quote.exactAmount());
        }
        EnergyPacketPolicy policy() { return new EnergyPacketPolicy(packet, packets, partialPackets); }
        EnergyAmount balance() { return potential != null ? potential : quote.exactAmount(); }
    }
    private record NativeWindowKey(long generation, BlockEntity source, int firstReceiver) { }
    private record Element(ResourceLocation type, long conductorLoss, BlockEntity tile, int routing, int faces) {
        @Override public boolean equals(Object other) {
            return other instanceof Element e && type.equals(e.type) && conductorLoss == e.conductorLoss && tile == e.tile && routing == e.routing && faces == e.faces;
        }
        @Override public int hashCode() { return 31 * type.hashCode() + Long.hashCode(conductorLoss) + System.identityHashCode(tile) + routing + 31 * faces; }
    }
    private static final class WorldGrid implements AutoCloseable {
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
        final RouteBuildBudget routeBudget = new RouteBuildBudget();
        NativeRoutePlan nativePlan;
        RoutingView routingView;
        long portsGeneration;
        long plansBuilt, plansResumed, budgetYields;
        /** Bumped whenever the endpoint/conductor sets change; cached port lists must then be rebuilt. */
        long endpointEdits;
        ForeignRoutePlan exportPlan, importPlan;
        int nativeCursor;
        final Map<BlockPos, Integer> receiverCursors = new HashMap<>();
        final ContactOrder contactOrder;
        WorldGrid(long constructionSeed) { contactOrder = new ContactOrder(104_096, 4096, constructionSeed); }
        final Map<BlockPos, BlockEntity> machines = new LinkedHashMap<>();
        long endpointLimitDrops, registrationLimitDrops;
        final Map<BlockPos, Integer> transformerRouting = new HashMap<>();
        final Map<BlockPos, IndependentTransformerBlockEntity> transformers = new LinkedHashMap<>();
        final Map<BlockPos, IndependentSpecialCableBlockEntity> specialCables = new LinkedHashMap<>();
        final Map<BlockPos, Element> specialObserved = new HashMap<>(), specialPublished = new HashMap<>();
        final java.util.NavigableMap<Long, Map<BlockPos, Element>> specialPending = new java.util.TreeMap<>();
        final Map<BlockPos, Set<BlockPos>> initialGeneratorContacts = new HashMap<>();
        final Map<Long, Integer> conductorChunks = new HashMap<>();
        final Map<BlockPos, ResourceLocation> conductorTypes = new HashMap<>();
        final Map<BlockPos, BlockEntity> conductorOwners = new HashMap<>();
        final Map<BlockPos, IEnergyTile> registered = new LinkedHashMap<>();
        final Set<BlockPos> catchUpEndpoints = new HashSet<>(), effectEndpoints = new HashSet<>();
        final com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings timings = new com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyTimings();
        Set<BlockPos> includedEndpoints, excludedEndpoints;
        final com.miophas.singularity_iteration.core.runtime.energy.engine.ShockAccumulator<ShockRoute, LivingEntity> shocks =
            new com.miophas.singularity_iteration.core.runtime.energy.engine.ShockAccumulator<>();
        final BoundedContactIndex<ForeignContact, FECompatTile> foreign = new BoundedContactIndex<>(4096);
        long foreignMaintenanceTick = Long.MIN_VALUE;
        boolean discoveringForeign;
        final Set<ForeignContact> failedQueries = new HashSet<>();
        long queryTick = Long.MIN_VALUE;
        final Map<BlockPos, NodeStats> samples = new HashMap<>();
        final Map<BlockPos, EnergyAmount> packetSpent = new HashMap<>();
        final Set<BlockPos> failedOutputs = new HashSet<>();
        final Map<BlockPos, EnergyAmount> transformerReceived = new HashMap<>();
        List<BlockPos> scanAnchors = List.of();
        final Set<BlockPos> exportedThisTick = new HashSet<>();
        /** Positions whose balances the latest native commit wrote (its window's ports). */
        final Set<BlockPos> lastCommitted = new HashSet<>();
        /** Bumped on every native balance write (commit or demand acquisition); drives quote refresh. */
        long balanceWrites;
        int scanCursor, transferCursor, importCursor;
        boolean scanDirty = true;
        // Chunk availability is queried by native settlement and both FE
        // directions in the same server tick.  Re-evaluating every registered
        // conductor for each pass was a hot O(chunks * passes) path.  The
        // topology observer revokes this value when a chunk is removed; normal
        // ticket changes are consequently observed on the next tick at the
        // latest, while all transaction leases still recheck isCurrent().
        long accessibilityTick = Long.MIN_VALUE;
        long accessibilityRevision = Long.MIN_VALUE;
        boolean accessibilityResult;
        long sampleTick = Long.MIN_VALUE;
        boolean catchUp;
        boolean closed;
        String failure = "";
        long effectPauseFrame = -1;
        // Unlike ordinary physical add/remove coalescing, measured splitter
        // pulses preserve the observed off/on transitions in consecutive frames.
        void observeSpecial(long frame, BlockPos at, Element value) {
            if (java.util.Objects.equals(specialObserved.get(at), value)) return;
            if (value == null) specialObserved.remove(at); else specialObserved.put(at, value);
            if (specialObserved.size() > 100_000 || specialPending.size() > 4)
                throw new IllegalStateException("Special cable queue limit reached");
            specialPending.computeIfAbsent(frame + 1, ignored -> new LinkedHashMap<>()).put(at, value);
        }
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
                        // The fast path only refreshed the conductor index; a dual tile
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
        @Override public void close() {
            if (closed) return;
            closed = true;
            nativePlan = null; routingView = null; exportPlan = null; importPlan = null; receiverCursors.clear();
            catchUpEndpoints.clear(); effectEndpoints.clear(); shocks.clear(); includedEndpoints = null; excludedEndpoints = null;
            registered.clear(); foreign.clear(); failedQueries.clear(); samples.clear(); packetSpent.clear(); scanAnchors = List.of();
            entries.close(); conductors.close(); contactOrder.close(); machines.clear(); transformerRouting.clear(); transformers.clear(); specialCables.clear(); specialObserved.clear(); specialPublished.clear(); specialPending.clear(); initialGeneratorContacts.clear(); conductorChunks.clear(); conductorTypes.clear(); conductorOwners.clear(); catchUp = false; effectPauseFrame = -1;
        }
    }
    private IndependentSiEnergy(MinecraftServer server) {
        this.server = server;
        topology = new PlatformTopology(server, EnergyNodeRegistry.conductors(), 100_000, 32, 65_536, 4096, this);
        NeoForge.EVENT_BUS.register(this);
    }
    public Metrics metrics() {
        if (Thread.currentThread().threadId() != ownerThread) throw new IllegalStateException("Read engine metrics on server thread");
        return new Metrics(ticks, commits, rejected, debited, credited, dissipated,
            worlds.size(), worlds.values().stream().mapToInt(grid -> grid.machines.size()).sum(), closed, failure, selectionSeed,
            deliveryCount, deliveryWireVisits, fusedWires, destroyedReceivers, blastBlocks,
            totalDebit.fraction(), totalCredit.fraction(), totalLoss.fraction());
    }
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
    @Override public void levelRemoved(ServerLevel level) { var grid = worlds.remove(level); if (grid != null) grid.close(); }
    @Override public void cleared() { worlds.values().forEach(WorldGrid::close); worlds.clear(); }
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
    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        if (event.getServer() != server || closed || !failure.isEmpty()) return;
        ticks++;
        if (!topology.failure().isEmpty()) fail(new IllegalStateException(topology.failure()));
    }
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
            boolean effectPause = grid.effectPauseFrame == ticks + 1;
            if (effectPause) grid.effectPauseFrame = -1;
            if (changes.isEmpty() && !effectPause) settle(level, grid);
            else {
                var changedPositions = new HashSet<BlockPos>();
                for (var change : changes) changedPositions.add(change.key());
                var affected = affectedEndpoints(grid, changedPositions);
                if (effectPause) { affected.addAll(grid.effectEndpoints); grid.effectEndpoints.clear(); }
                grid.apply(changes);
                affected.addAll(affectedEndpoints(grid, changedPositions));
                // Preserve pause/catch-up for the edited electrical neighbourhood.
                // Independent circuits neither pause nor get duplicate START output.
                grid.catchUpEndpoints.addAll(affected);
                grid.catchUp = !grid.catchUpEndpoints.isEmpty();
                settleScoped(level, grid, null, affected);
            }
        } catch (RuntimeException error) { fail(level, grid, error); }
    }
    private void fail(RuntimeException error) { failure = error.toString(); error.printStackTrace(); topology.close(); }
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
    private void settleScoped(ServerLevel level, WorldGrid grid, Set<BlockPos> included, Set<BlockPos> excluded) {
        grid.includedEndpoints = included; grid.excludedEndpoints = excluded;
        try { settle(level, grid); }
        finally { grid.includedEndpoints = null; grid.excludedEndpoints = null; }
    }
    private static boolean participates(WorldGrid grid, BlockPos at) {
        return (grid.includedEndpoints == null || grid.includedEndpoints.contains(at))
            && (grid.excludedEndpoints == null || !grid.excludedEndpoints.contains(at));
    }
    public PlatformTopology.RecoveryMetrics topologyRecoveryMetrics() { return topology.recoveryMetrics(); }
    private void fail(ServerLevel level, WorldGrid grid, RuntimeException error) {
        com.mojang.logging.LogUtils.getLogger().error("SI energy rebuilding {} after an isolated failure", level.dimension().location(), error);
        grid.catchUp = false;
        topology.rebuild(level, error.toString());
    }
    private boolean currentWorld(ServerLevel level, WorldGrid grid) {
        return !closed && grid.failure.isEmpty() && server != null
            && worlds.get(level) == grid && server.getLevel(level.dimension()) == level;
    }
    private boolean accessible(ServerLevel level, WorldGrid grid) {
        if (!currentWorld(level, grid) || !topology.operational(level)) return false;
        long tick = server.getTickCount();
        long readiness = topology.readinessRevision();
        if (grid.accessibilityTick == tick && grid.accessibilityRevision == readiness) return grid.accessibilityResult;
        // Chunk accessibility revokes routes without deleting the published wire history.
        // An unrelated inactive component must not pause this entire dimension.
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
        // rotate independently; neither can renew a source's output allowance.
        for (int batch = 0; batch < 128; batch++) {
            if (cachedPorts == null) {
                cachedPorts = new ArrayList<>(ports(level, grid, prepareDemand));
                for (var port : cachedPorts) if (port.outputs != 0)
                    allowances.put(port.tile, new PacketAllowance(port.policy(), port.offered()));
                rebuildLists = true;
            } else if (grid.balanceWrites != cachedAtCommits) {
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
            if (allSources.isEmpty() || allSinks.isEmpty()) { grid.nativePlan = null; return; }
            var snapshot = grid.conductors.snapshot();
            if (snapshot != lastSnapshot) { lastSnapshot = snapshot; checkView = true; }
            // Balances change every tick; routing identities usually do not.
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
            if (plan != null && plan.sources.stream().anyMatch(p -> visited.contains(p.position))) plan = null;
            if (plan == null || plan.portsGeneration != grid.portsGeneration || plan.graph != snapshot) {
                int start = Math.floorMod(grid.nativeCursor, allSources.size());
                int skipped = 0;
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
            if (windows.contains(windowKey)) {
                visited.addAll(plan.sources.stream().map(RouteEndpoint::position).toList());
                grid.nativeCursor = plan.nextSource; grid.nativePlan = null;
                continue;
            }
            boolean offering = false;
            // The plan window is quoted from its own generation; clamp to the
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
            if (!plan.advance(grid.nativeSearch, grid.routeBudget)) { grid.budgetYields++; return; }
            // Reserve the eligibility and priority vectors (including copies)
            // before finalizing, not after performing an unbounded allocation.
            if (!grid.routeBudget.reserveArrays(plan.arrayCells * 3)) return;
            if (!settlePlan(level, grid, prepareDemand, ports, plan, cachedByPosition, allowances)) return;
            windows.add(windowKey);
            boolean exhausted = true;
            for (var endpoint : plan.sources) {
                var allowance = allowances.get(endpoint.tile);
                exhausted &= allowance == null || allowance.remaining().isZero();
            }
            if (exhausted || plan.nextReceiver == plan.firstReceiver)
                visited.addAll(plan.sources.stream().map(RouteEndpoint::position).toList());
            grid.receiverCursors.put(plan.sources.getFirst().position, plan.nextReceiver);
            grid.nativeCursor = plan.nextSource;
            // Keep a complete single-window plan hot; a multi-window cycle
            // releases old paths before admitting another working set.
            if (plan.nextSource != plan.firstSource || plan.nextReceiver != plan.firstReceiver)
                grid.nativePlan = null;
            if (visited.size() == allSources.size()) return;
            if (!accessible(level, grid)) return;
        }
    }
    private static boolean isRoutedSource(Port port) { return port.outputs != 0 && port.packet > 0 && port.packets > 0; }

    /**
     * Re-quotes only the ports a commit touched and patches them into every cached
     * list. Returns false (caller falls back to a full refresh) if a port dropped
     * out, or its list membership or routing identity changed.
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

    private static EnergyAmount windowOffer(Port port, IdentityHashMap<BlockEntity, PacketAllowance> allowances, int packetLimit) {
        var allowance = allowances.get(port.tile);
        return allowance == null ? EnergyAmount.ZERO : allowance.quote(port.policy(), port.offered(), packetLimit);
    }
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
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i); var receiver = sinkIndices.get(source);
            if (source.potential == null && receiver != null)
                shared.add(new DomainDistributor.SharedStorage(i, receiver, source.capacity));
        }
        var round = FractionalDistributor.allocateQuotedTraced(quotes, sources.stream().map(Port::balance).toList(),
            domains, receivers, room, shared, budgets, selectionRandom);
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
        if (debit.isZero()) return true;
        // External HU is acquired only for a routable, nonzero demand. It is credited to owned EU first;
        // a fresh bounded second plan then settles only actual owned balances, never simulated fuel.
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
                    return settlePlan(level, grid, false, requoted, plan, repositioned, allowances);
                }
                return false;
            }
        }
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
            // A source's tier only caps its offer; a partial delivery is not a full packet.
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
            boolean fused = deliveryVoltage > weakestMeasuredLimit[0];
            if (IndependentEnergyMode.enetExplosions() && receiverLimit != null && !fused
                    && deliveryVoltage > receiverLimit) {
                receiverPlan.merge(sink, deliveryVoltage, Math::max);
            }
        }
        if (!traceDebit.equals(debit) || !traceCredit.equals(credit) || !traceLoss.equals(loss))
            throw new IllegalStateException("Delivery trace does not reconcile with the round");
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
                if (sink.tile instanceof IOverloadHandler handler && handler.onOverload(overloadTier)) continue;
                var realBe = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                if (realBe != null && realBe != sink.tile && realBe instanceof IOverloadHandler handler && handler.onOverload(overloadTier)) continue;
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
                // Visible mode changes revoke old quotes immediately; the new sides
                // join only after the same delayed publication as electrical edits.
                if (!Integer.valueOf(transformer.routingSignature()).equals(grid.transformerRouting.get(at))) continue;
                var offered = transformer.snapshot(transformer.stepUpNow());
                if (offered.isEmpty()) continue;
                var saved = offered.orElseThrow(); var limits = saved.limits();
                var property = saved.state().getBlock().getStateDefinition().getProperty("facing");
                if (property == null || !(saved.state().getValue(property) instanceof Direction facing))
                    throw new IllegalStateException("Transformer facing missing");
                int high = 1 << facing.ordinal(), low = 63 ^ high;
                int input = saved.stepUp() ? low : high, output = saved.stepUp() ? high : low;
                var quote = new CustomEUEnergyStorage.NetworkQuote(level, at, saved.energy().amount(),
                    limits.capacity(), limits.inputLimit(), limits.outputPacket(), limits.outputPacket(), true, true, saved.energy());
                ports.add(new Port(tile, at, saved.state(), null, quote, limits.capacity(), input, output,
                    limits.outputPacket(), limits.outputPackets(), false, saved, null, null, List.of(at)));
                continue;
            }
            var machine = tile;
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
    /** Rebuilds only the live quote and FE output allowance of each cached port.
     * The expensive per-machine skeleton — chunk checks, face masks, packet
     * tiers, emitter positions — is commit-invariant within one tick and is
     * reused as-is, so a many-endpoint world no longer rewalks every machine,
     * chunk and registry key for each settlement batch. Ports whose entity was
     * removed or lost its storage boundary drop out, mirroring ports(). */
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
    private record ForeignContact(BlockPos at, Direction input) { }
    private record Route(long lossMilli, ConductorRegistry.Path path) { }
    /** Resumes one foreign pair, including all candidate faces, without quoting energy. */
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
        boolean matches(ConductorRegistry.Snapshot snapshot, List<BlockPos> from, int outputMask, BlockPos to, int inputMask) {
            return graph == snapshot && emitters.equals(from) && outputs == outputMask && receiver.equals(to) && inputs == inputMask;
        }
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
    private record LocatedNode(ServerLevel getWorld, BlockPos getPos) implements ILocatableTile { }

    /** Registration is a notification; only loaded, owned entities enter the native transaction. */
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
    public void unregisterTile(IEnergyTile tile) {
        for (var grid : worlds.values()) grid.registered.values().removeIf(value -> value == tile);
        if (tile instanceof BlockEntity block) changed(block);
    }
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
    public NodeStats nodeStats(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        return grid == null || !loaded(level, at) || server.getTickCount() - grid.sampleTick > 1
            ? null : grid.samples.get(at);
    }

    /** Query after level settlement. An idle registered node contributes a zero tick,
     * never the previous tick's packet. The existing sparse query remains unchanged. */
    public NodeStats currentTickNodeStats(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (closed || !failure.isEmpty() || grid == null || !grid.failure.isEmpty() || tileAt(level, at) == null) return null;
        NodeStats measured = grid.sampleTick == server.getTickCount() ? grid.samples.get(at) : null;
        return measured == null ? new NodeStats(0, 0, 0) : measured;
    }
    private static void sample(WorldGrid grid, BlockPos at, double input, double output, double voltage) {
        var before = grid.samples.get(at);
        grid.samples.put(at.immutable(), before == null ? new NodeStats(input, output, voltage)
            : new NodeStats(before.energyIn() + input, before.energyOut() + output, Math.max(before.voltage(), voltage)));
    }
    public Set<Direction> connections(ServerLevel level, BlockPos at) {
        var grid = worlds.get(level);
        if (grid == null || !loaded(level, at) || !accessible(level, grid)) return Set.of();
        var result = java.util.EnumSet.noneOf(Direction.class);
        var graph = grid.conductors.snapshot();
        int own = graph.contains(point(at)) ? conductorFaces(grid.conductorOwners.get(at)) : 0;
        // Build the port view once. This API is queried by the compatibility
        // layer for each face; scanning all machines again inside the neighbour
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
    private static boolean loaded(ServerLevel level, BlockPos at) {
        return level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) != null
            && level.shouldTickBlocksAt(ChunkPos.asLong(at));
    }
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
    private void discoverForeign(ServerLevel level, WorldGrid grid) {
        if (grid.discoveringForeign || !currentWorld(level, grid)) return;
        grid.discoveringForeign = true;
        try {
            discoverForeignBatch(level, grid);
        } finally {
            grid.discoveringForeign = false;
        }
    }
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
    /** Reachability needs connected components, not a shortest-path search. */
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
            if (!plan.advance(grid.exportSearch)) return;
            if (plan.best != null && plan.best.path != null
                    && !grid.routeBudget.reserveVisits(plan.best.path.vertexCount(), 1)) return;
            grid.exportPlan = null;
            grid.transferCursor = index + 1 == pairs ? 0 : index + 1;
            var route = plan.best;
            if (route == null) continue;
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
                allowances.get(source.tile).debit(FeLedger.eu(sent));
                grid.exportedThisTick.add(entry.getKey().at);
                grid.packetSpent.merge(source.position, FeLedger.eu(sent), EnergyAmount::add);
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
                // charged; the native graph keeps running and does not invent a refund.
                grid.failedOutputs.add(source.position);
                rejected++; foreignFailure.printStackTrace();
            }
        }
    }
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
            // Use the same bounded advancement as exports; never rely on an
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
            if (!plan.advance(grid.importSearch)) return;
            if (plan.best != null && plan.best.path != null
                    && !grid.routeBudget.reserveVisits(plan.best.path.vertexCount(), 2)) return;
            grid.importPlan = null;
            grid.importCursor = index + 1 == pairs ? 0 : index + 1;
            Route selected = plan.best; Direction selectedSide = plan.bestInput;
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
                // FE has no packet voltage; meters use zero for this unpacketized intake.
                sample(grid, source.getKey().at, 0, output, 0);
                sample(grid, sink.position, input, 0, 0);
                if (route.path != null && accessible(level, grid) && grid.conductors.isCurrent(graph)) route.path.visit(at -> {
                    var pos = new BlockPos(at.x(), at.y(), at.z()); sample(grid, pos, output, output, 0);
                    var detector = grid.specialCables.get(pos); if (detector != null && detector.detector()) detector.delivered();
                });
                commits++; totalDebit = totalDebit.add(FeLedger.eu(extracted)); totalCredit = totalCredit.add(FeLedger.eu(received));
                totalLoss = totalLoss.add(FeLedger.eu(extracted - received));
                debited = totalDebit.whole(); credited = totalCredit.whole(); dissipated = totalLoss.whole();
            } catch (RuntimeException foreignFailure) { rejected++; foreignFailure.printStackTrace(); }
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
    private static int pullTransformer(IndependentTransformerBlockEntity transformer, FECompatTile source, int lossFe,
            EnergyAmount receivedThisTick, java.util.function.BooleanSupplier current) {
        var before = transformer.snapshot(transformer.stepUpNow()).orElse(null);
        if (before == null || !current.getAsBoolean() || !source.canExtract()) return 0;
        int room = FeLedger.fe(before.energy().exactAmount().roomBelow(before.limits().capacity())
            .min(receivedThisTick.roomBelow(before.limits().inputLimit())));
        if (room == 0) return 0;
        int maximum = (int)Math.min(Integer.MAX_VALUE, (long)room + lossFe);
        int quoted = source.extractEnergy(maximum, true);
        if (quoted <= lossFe || quoted > maximum || !current.getAsBoolean()
            || !transformer.isCurrent(before, transformer.stepUpNow())) return 0;
        int extracted = source.extractEnergy(quoted, false);
        if (extracted < 0 || extracted > quoted) throw new IllegalStateException("Invalid transformer FE input receipt");
        int accepted = Math.max(0, extracted - lossFe);
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
            if (fresh == null) return offered; // Removed during callback: reserved energy stays with the removed source.
            var refund = FeLedger.eu(offered - accepted); var escrow = new NetworkCell(refund);
            if (!NetworkCell.commit(List.of(new NetworkCell.Write(fresh.energy(), fresh.energy().exactAmount().add(refund)),
                    new NetworkCell.Write(escrow.quote(), EnergyAmount.ZERO)), EnergyAmount.ZERO, () -> true))
                throw new IllegalStateException("Transformer refund lease changed");
            transformer.markNetworkChanged();
        }
        return accepted;
    }

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

    private record ShockRoute(BlockPos source, BlockPos receiver) { }
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
    private void applySmallBlockBlast(ServerLevel level, WorldGrid grid, BlockPos center, Set<BlockPos> removed) {
        // Independent block-only hypothesis from frozen material/position
        // observations: the 18 face/edge neighbours, evaluated individually.
        // The resistance cutoff matches the measured vanilla materials; values
        // between 1 and 1.5 and other modded blocks remain unverified.
        // R25's new272-block sample retained207dirt items, supporting the frozen
        // three-in-four drop hypothesis for this small blast. Vanilla loot
        // rules still decide the item, and doTileDrops remains authoritative.
        // Entity fields run first; sounds and other strengths remain separate.
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            int distanceSquared = dx * dx + dy * dy + dz * dz;
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
                for (var drop : drops) if (blockDropRandom.nextInt(4) != 0)
                    net.minecraft.world.level.block.Block.popResource(level, at, drop);
            }
        }
    }
    private boolean valid(ServerLevel level, WorldGrid grid, ConductorRegistry.Snapshot snapshot, List<Port> ports) {
        // The independent published registry intentionally preserves one final
        // packet after a wire edit, as measured. Its lease is still current;
        // source/sink identity, all fresh quotes and chunk access remain mandatory.
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
    private static List<BlockPos> emissionPositions(BlockEntity tile){
        return tile instanceof com.miophas.singularity_iteration.core.api.energy.IEnergyEmissionSource reactor
            ?reactor.electricalContactPositions():List.of(tile.getBlockPos());
    }
    private static Long tierReceiverLimit(BlockEntity tile) {
        if (tile instanceof EnergyComponentHost host) return host.energyComponent().tier().getPowerRating();
        if (tile instanceof com.miophas.singularity_iteration.core.api.machine.IEnergyBlock block) return block.getEffectiveCableTier().getPowerRating();
        return null;
    }
    /**
     * Nominal per-packet offer ceiling. A static profile keeps its registered
     * packet; a dynamic generator uses its live source tier. Partial deliveries
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
    private static Long tierFuseLimit(ResourceLocation type) {
        var block = BuiltInRegistries.BLOCK.get(type);
        if (block instanceof com.miophas.singularity_iteration.core.api.energy.ICableBlock cable)
            return cable.getCableTier().getConductorBreakdownEnergy();
        return null;
    }
    private static ConductorRegistry.Position point(BlockPos at) { return new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ()); }
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
                boolean contactEntry = entry.getKey() % 7 != 6; shared |= contactEntry;
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
    /** Only topology identity is retained between ticks, never a mutable energy quote. */
    private record RouteEndpoint(BlockPos position, BlockEntity tile, int inputs, int outputs,
            List<BlockPos> emitters, EnergyPacketPolicy packets) {
        static RouteEndpoint of(Port port) {
            return new RouteEndpoint(port.position, port.tile, port.inputs, port.outputs, List.copyOf(port.emitterPositions), port.policy());
        }
        /*
         * Routing identity: what the route plan depends on. Packet size and the
         * partial-packet flag are allocation inputs, read live from the Port at
         * settlement. Dynamic-tier generators (kinetic, steam, ...) change packet
         * size almost every tick; including it here rebuilt the global routing
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
    private record NativeOrigin(int source, int output, ConductorRegistry.Position position, int component, long emitter) { }
    private record RoutingView(ConductorRegistry.Snapshot graph, List<RouteEndpoint> sources,
            List<RouteEndpoint> sinks, ReceiverContacts contacts, long portsGeneration) {
        boolean isCurrent(ConductorRegistry.Snapshot snapshot, long generation) {
            return graph == snapshot && portsGeneration == generation;
        }
        boolean matches(ConductorRegistry.Snapshot snapshot, List<Port> sourcePorts, List<Port> sinkPorts) {
            return graph == snapshot && NativeRoutePlan.matches(sources, sourcePorts) && NativeRoutePlan.matches(sinks, sinkPorts);
        }
    }

    /** Complete a bounded source/receiver window before permitting any transfer. */
    private static final class NativeRoutePlan {
        final ConductorRegistry.Snapshot graph;
        final List<RouteEndpoint> allSources, allSinks, sources, sinks;
        final List<NativeOrigin> origins = new ArrayList<>();
        final ReceiverContacts contacts;
        final LinkedHashMap<Long, LinkedHashMap<Long, Map.Entry<long[], ConductorRegistry.Path[]>>> grouped = new LinkedHashMap<>();
        final int firstSource, nextSource, firstReceiver, nextReceiver, packetLimit;
        final long portsGeneration;
        int originIndex;
        long arrayCells;
        ReceiverContacts.Candidates candidates;
        ConductorRegistry.RouteIndex paths;
        boolean candidateReady;

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
                    // Search admission uses the compressed search size; effect
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
            packetLimit = (int) Math.min(declaredPackets, MAX_NATIVE_PATH_VISITS_PER_TICK / Math.max(1, effectVertices * 2));
            if (packetLimit < 1) throw new IllegalStateException("Single packet path exceeds its bound");
            nextSource = end == allSources.size() ? 0 : end;
            var globalContacts = view.contacts;
            var reachable = new java.util.BitSet(allSinks.size());
            var components = new HashSet<Integer>();
            for (var origin : origins) {
                for (int receiver : globalContacts.at(origin.position)) reachable.set(receiver);
                if (components.add(origin.component))
                    for (var contact : globalContacts.inComponent(origin.component)) reachable.set(contact.receiver());
            }
            int count = reachable.cardinality();
            firstReceiver = count == 0 ? 0 : Math.floorMod(grid.receiverCursors.getOrDefault(sources.getFirst().position, 0), count);
            int receiverLimit = (int) Math.min(MAX_PLAN_RECEIVERS,
                MAX_NATIVE_PATH_VISITS_PER_TICK / Math.max(1, effectVertices * 2 * packetLimit));
            // Bound effect work by actual policy, including addon multi-packet
            // sources. Large paths admit fewer packets instead of stalling forever.
            if (receiverLimit < 1) throw new IllegalStateException("Single-source path window exceeds its bound");
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
        private static boolean matches(List<RouteEndpoint> saved, List<Port> current) {
            if (saved.size() != current.size()) return false;
            for (int i = 0; i < saved.size(); i++) if (!saved.get(i).matches(current.get(i))) return false;
            return true;
        }
        boolean matches(ConductorRegistry.Snapshot snapshot, List<Port> sourcePorts, List<Port> sinkPorts) {
            return graph == snapshot && matches(allSources, sourcePorts) && matches(allSinks, sinkPorts);
        }
        boolean matchesSelected(List<Port> sourcePorts, List<Port> sinkPorts) {
            return matches(sources, sourcePorts) && matches(sinks, sinkPorts);
        }
        private int newCells(long domain, long emitter) {
            var entries = grouped.get(domain);
            return entries == null || !entries.containsKey(emitter) ? sinks.size() : 0;
        }
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
        boolean advance(ConductorRegistry.SearchBudget searches, RouteBuildBudget budget) {
            while (originIndex < origins.size()) {
                var origin = origins.get(originIndex);
                if (candidates == null) candidates = contacts.candidates(origin.position, origin.component);
                if (!candidateReady) {
                    if (!candidates.next()) { originIndex++; candidates = null; paths = null; continue; }
                    candidateReady = true;
                }
                int receiver = candidates.receiver(); var sink = sinks.get(receiver);
                boolean distinct = sources.get(origin.source).tile != sink.tile;
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
                long directDomain = -1L - (long) origin.source * sinks.size() - receiver;
                int cells = (direct ? newCells(directDomain, 7L * origin.source + 6) : 0)
                    + (bestVertex >= 0 ? newCells(origin.component, origin.emitter) : 0);
                if (!budget.consumeRecord(cells)) return false;
                if (direct) record(directDomain, 7L * origin.source + 6, receiver, 0, null);
                // All faces of this receiver are considered before yielding;
                // later origins can still improve it before the plan is ready.
                if (bestVertex >= 0) record(origin.component, origin.emitter, receiver, bestLoss, paths.path(bestVertex));
                candidateReady = false;
            }
            return true;
        }
    }

    private static final class RouteBuildBudget {
        private long tick = Long.MIN_VALUE;
        private int records;
        private long arrayCells;
        private final long[] pathVisits = new long[3];
        void reset(long now) {
            if (tick == now) return;
            tick = now; records = MAX_ROUTE_RECORDS_PER_TICK; arrayCells = MAX_ROUTE_ARRAY_CELLS_PER_TICK;
            pathVisits[0] = MAX_NATIVE_PATH_VISITS_PER_TICK; pathVisits[1] = 200_000; pathVisits[2] = 200_000;
        }
        boolean consumeRecord(int cells) {
            if (records == 0 || !reserveArrays(cells)) return false;
            records--; return true;
        }
        boolean reserveArrays(long cells) {
            if (cells < 0 || arrayCells < cells) return false;
            arrayCells -= cells; return true;
        }
        boolean reserveVisits(long visits, int kind) {
            if (visits < 0 || visits > pathVisits[kind]) return false;
            pathVisits[kind] -= visits; return true;
        }
    }
    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        if (event.getServer() != server) return;
        topology.close(); cleared(); closed = true;
        synchronized (IndependentSiEnergy.class) { SERVERS.remove(server); }
        server = null;
        NeoForge.EVENT_BUS.unregister(this);
    }
}
