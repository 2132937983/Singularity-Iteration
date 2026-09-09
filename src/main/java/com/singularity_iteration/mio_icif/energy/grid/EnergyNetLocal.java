package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

public class EnergyNetLocal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Level world;
    private final Queue<GridChange> gridChangesQueue = new ArrayDeque<>();
    private final Map<IEnergyTile, GridChange> gridAdditionsMap = new IdentityHashMap<>();
    private final Set<IEnergyTile> ioTilesToNotify = Collections.newSetFromMap(new IdentityHashMap<>());
    private final GridUpdater updater = new GridUpdater(this);
    int nextNodeId;
    int nextGridId;
    final Map<IEnergyTile, Tile> registeredIoTiles = new IdentityHashMap<>();
    public final Map<BlockPos, Tile> registeredTiles = new HashMap<>();
    final Set<Tile> sources = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Grid> grids = new ArrayList<>();
    private boolean hasPendingChanges = false;

    public static EnergyNetLocal create(Level world) {
        return new EnergyNetLocal(world);
    }

    private EnergyNetLocal(Level world) {
        this.world = world;
    }

    IEnergyTile getIoTile(BlockPos pos) {
        Tile tile = getTile(pos);
        if (tile != null)
            return tile.getMainTile();

        IEnergyTile ret = null;
        for (GridChange change : new ArrayList<>(this.gridChangesQueue)) {
            if (change.pos != null && change.pos.equals(pos))
                ret = (change.type == GridChange.Type.REMOVAL) ? null : change.ioTile;
        }
        return ret;
    }

    IEnergyTile getSubTile(BlockPos pos) {
        Tile tile = getTile(pos);
        if (tile != null)
            return tile.getSubTileAt(pos);

        IEnergyTile ret = null;
        for (GridChange change : new ArrayList<>(this.gridChangesQueue)) {
            for (IEnergyTile subtile : (change.subTiles != null) ? change.subTiles : Collections.singletonList(change.ioTile)) {
                if (EnergyNetGlobal.getPos(subtile).equals(pos)) {
                    if (change.type == GridChange.Type.REMOVAL) ret = null;
                    else ret = subtile;
                }
            }
        }
        return ret;
    }

    public Tile getTile(BlockPos pos) {
        return this.registeredTiles.get(pos);
    }

    /**
     * 获取所有已注册方块的位置集合（只读视图）
     * @return 不可修改的位置集合
     */
    public Collection<BlockPos> getAllRegisteredPositions() {
        return Collections.unmodifiableCollection(this.registeredTiles.keySet());
    }

    void addTile(IEnergyTile ioTile, BlockPos pos) {
        GridChange change = new GridChange(GridChange.Type.ADDITION, pos, ioTile);
        GridChange prev;
        if ((prev = this.gridAdditionsMap.put(ioTile, change)) != null) {
            this.gridAdditionsMap.put(ioTile, prev);
            if (EnergyNetSettings.logGridUpdateIssues)
                LOGGER.info("Tile " + ioTile + " was attempted to be queued twice for addition.");
        } else {
            this.gridChangesQueue.add(change);
            this.hasPendingChanges = true;
        }
    }

    void removeTile(IEnergyTile ioTile, BlockPos pos) {
        GridChange addition = this.gridAdditionsMap.remove(ioTile);
        if (addition != null) {
            if (EnergyNetSettings.logGridUpdatesVerbose)
                LOGGER.info("Removing tile " + ioTile + " by cancelling a pending addition.");
            this.gridChangesQueue.remove(addition);
        } else {
            this.gridChangesQueue.add(new GridChange(GridChange.Type.REMOVAL, pos, ioTile));
            this.hasPendingChanges = true;
            Tile tile = this.registeredIoTiles.get(ioTile);
            if (tile != null) {
                tile.setDisabled();
                if (EnergyNetSettings.logGridUpdatesVerbose)
                    LOGGER.info("Disabled tile " + ioTile);
            } else if (EnergyNetSettings.logGridUpdatesVerbose) {
                LOGGER.info("Missing tile " + ioTile);
            }
        }
    }

    public Collection<Tile> getSources() { return this.sources; }

    NodeStats getNodeStats(IEnergyTile ioTile) {
        Tile tile = this.registeredIoTiles.get(ioTile);
        return (tile == null) ? null : EnergyNetGlobal.getCalculator().getNodeStats(tile);
    }

    public boolean sourceHasReachableSinks(IEnergyTile ioTile) {
        Tile tile = this.registeredIoTiles.get(ioTile);
        if (tile == null) return false;
        for (Node node : tile.getNodes()) {
            if (node.getType() == NodeType.Source) {
                Grid grid = node.getGrid();
                if (grid == null) continue;
                NodeStats stats = EnergyNetGlobal.getCalculator().getNodeStats(tile);
                if (stats != null && stats.getEnergyOut() > 0) return true;
                for (Node gridNode : grid.getNodes()) {
                    if (gridNode.getType() == NodeType.Sink && gridNode.getTile() != tile) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    int getAdjacentConnections(IEnergyTile queryTile) {
        Tile tile = this.registeredIoTiles.get(queryTile);
        if (tile == null) return 0;

        BlockPos pos = EnergyNetGlobal.getPos(queryTile);
        int ret = 0;
        for (Node node : new ArrayList<>(tile.getNodes())) {
            for (NodeLink link : new ArrayList<>(node.getLinks())) {
                Node neighbor = link.getNeighbor(node);
                for (IEnergyTile neighborTile : neighbor.getTile().getSubTiles()) {
                    BlockPos neighborPos = EnergyNetGlobal.getPos(neighborTile);
                    Direction dir = getDirBetween(pos, neighborPos);
                    if (dir != null)
                        ret |= 1 << dir.ordinal();
                }
            }
        }
        return ret;
    }

    private static Direction getDirBetween(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int abs = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        if (abs != 1) return null;
        if (dx != 0) return (dx > 0) ? Direction.EAST : Direction.WEST;
        if (dy != 0) return (dy > 0) ? Direction.UP : Direction.DOWN;
        return (dz > 0) ? Direction.SOUTH : Direction.NORTH;
    }

    public Collection<GridInfo> getGridInfos() {
        List<GridInfo> ret = new ArrayList<>();
        for (Grid grid : new ArrayList<>(this.grids))
            ret.add(grid.getInfo());
        return ret;
    }

    public void onTickStart() {
    }

    public void onTickEnd() {
        if (hasPendingChanges) {
            hasPendingChanges = false;
            this.updater.startChangeCalc(this.gridChangesQueue, this.gridAdditionsMap);
        }
        if (!this.ioTilesToNotify.isEmpty()) {
            ChunkSource chunkManager = this.world.getChunkSource();
            int lastX = Integer.MIN_VALUE;
            int lastZ = Integer.MIN_VALUE;
            boolean lastLoaded = false;

            List<IEnergyTile> tilesToNotify = new ArrayList<>(this.ioTilesToNotify);
            this.ioTilesToNotify.clear();

            for (IEnergyTile tile : tilesToNotify) {
                BlockPos pos = EnergyNetGlobal.getPos(tile);
                int x = SectionPos.blockToSectionCoord(pos.getX());
                int z = SectionPos.blockToSectionCoord(pos.getZ());
                if (x != lastX || z != lastZ) {
                    lastLoaded = chunkManager.hasChunk(x, z);
                    lastX = x;
                    lastZ = z;
                }
                if (lastLoaded)
                    tile.onConnectionChange();
            }
        }
        this.updater.startTransferCalc();
    }

    public Level getWorld() { return this.world; }

    int allocateNodeId() { return this.nextNodeId++; }

    int allocateGridId() { return this.nextGridId++; }

    void addTileToNotify(IEnergyTile ioTile) { this.ioTilesToNotify.add(ioTile); }

    void removeTileToNotify(IEnergyTile ioTile) { this.ioTilesToNotify.remove(ioTile); }

    boolean hasGrid(Grid grid) { return this.grids.contains(grid); }

    boolean hasGrids() { return !this.grids.isEmpty(); }

    Collection<Grid> getGrids() { return this.grids; }

    void addGrid(Grid grid) {
        assert !hasGrid(grid);
        this.grids.add(grid);
    }

    void removeGrid(Grid grid) {
        boolean removed = this.grids.remove(grid);
        assert removed;
    }

    void shuffleGrids() { Collections.shuffle(this.grids); }

    void clear() {
        for (Grid grid : new ArrayList<>(this.grids)) {
            grid.destroy();
        }
        this.grids.clear();
        for (Tile tile : new ArrayList<>(this.registeredIoTiles.values())) {
            tile.nodes.clear();
        }
        this.registeredIoTiles.clear();
        this.registeredTiles.clear();
        this.sources.clear();
        this.gridChangesQueue.clear();
        this.gridAdditionsMap.clear();
        this.ioTilesToNotify.clear();
        this.nextNodeId = 0;
        this.nextGridId = 0;
        this.updater.reset();
    }
}