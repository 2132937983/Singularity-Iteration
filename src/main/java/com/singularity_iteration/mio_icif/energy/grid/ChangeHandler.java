package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker;
import com.singularity_iteration.mio_icif.api.energy.tile.IColoredEnergyTile;

import java.util.*;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

class ChangeHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    static boolean prepareSync(EnergyNetLocal enet, GridChange change) {
        Level world = enet.getWorld();
        GridChange.Type type = change.type;
        IEnergyTile ioTile = change.ioTile;
        BlockPos pos = java.util.Objects.requireNonNull(change.pos);

        if (EnergyNetGlobal.getWorld(ioTile) != world) {
            return false;
        }
        if (type != GridChange.Type.REMOVAL && !EnergyNetGlobal.getPos(ioTile).equals(pos)) {
            return false;
        }
        if (type != GridChange.Type.REMOVAL && !world.isLoaded(pos)) {
            return false;
        }
        if (type != GridChange.Type.REMOVAL && ioTile instanceof BlockEntity && ((BlockEntity) ioTile).isRemoved()) {
            return false;
        }

        if (type == GridChange.Type.ADDITION) {
            // 优先检查内部 IMetaDelegate（返回 List<IEnergyTile>，可直接使用）
            // 否则检查 API 层 IMetaDelegate（返回 List<? extends IEnergyTileMarker>，需转换）
            // 如果两者都不实现，将自身作为唯一的 subTile
            if (ioTile instanceof IMetaDelegate md) {
                change.subTiles = new ArrayList<>(md.getSubTiles());
                if (change.subTiles.isEmpty())
                    throw new RuntimeException("Tile " + ioTile + " must return at least 1 sub tile for IMetaDelegate.getSubTiles().");
            } else if (ioTile instanceof com.singularity_iteration.mio_icif.api.energy.tile.IMetaDelegate amd) {
                change.subTiles = new ArrayList<>();
                for (IEnergyTileMarker sub : amd.getSubTiles()) {
                    if (sub instanceof BlockEntity be) {
                        IEnergyTile t = EnergyNetGlobal.getTile(be.getLevel(), be.getBlockPos());
                        if (t != null) change.subTiles.add(t);
                    }
                }
                if (change.subTiles.isEmpty()) change.subTiles = Arrays.asList(ioTile);
            } else {
                change.subTiles = Arrays.asList(ioTile);
            }
        }

        return true;
    }

    static void applyAddition(EnergyNetLocal enet, IEnergyTile ioTile, BlockPos pos, List<IEnergyTile> subTiles, Collection<GridChange> pendingChanges) {
        if (enet.registeredIoTiles.containsKey(ioTile)) {
        } else {
            for (IEnergyTile subTile : subTiles) {
                BlockPos subPos = EnergyNetGlobal.getPos(subTile);
                Tile prev;
                if ((prev = enet.registeredTiles.get(subPos)) != null) {
                    IEnergyTile prevIoTile = prev.getMainTile();
                    boolean found = false;
                    Iterator<GridChange> it = pendingChanges.iterator();
                    while (it.hasNext()) {
                        GridChange pendingChange = it.next();
                        if (pendingChange.type == GridChange.Type.REMOVAL && pendingChange.ioTile == prevIoTile) {
                            found = true;
                            it.remove();
                            applyRemoval(enet, pendingChange.ioTile, pendingChange.pos);
                            assert !enet.registeredTiles.containsKey(subPos);
                            break;
                        }
                        if (pendingChange.type == GridChange.Type.ADDITION && pendingChange.ioTile == prevIoTile)
                            break;
                    }
                    if (!found) {
                        return;
                    }
                }
            }

            Tile tile = new Tile(enet, ioTile, subTiles);
            enet.registeredIoTiles.put(ioTile, tile);
            if (ioTile instanceof IEnergySource)
                enet.sources.add(tile);

            if (EnergyNetSettings.logGridUpdatesVerbose) {
                LOGGER.debug("Added tile: {} at {} isSource={} isSink={} isConductor={} nodes={}",
                    ioTile.getClass().getSimpleName(), EnergyNetGlobal.getPos(ioTile),
                    ioTile instanceof IEnergySource, ioTile instanceof IEnergySink,
                    ioTile instanceof IEnergyConductor, tile.getNodes().size());
            }

            for (IEnergyTile subTile : subTiles) {
                BlockPos subPos = EnergyNetGlobal.getPos(subTile);
                enet.registeredTiles.put(subPos, tile);
            }

            addTileToGrids(enet, tile);
        }
    }

    private static void addTileToGrids(EnergyNetLocal enet, Tile tile) {
        List<Node> extraNodes = new ArrayList<>();
        IEnergyTile ioTile = tile.getMainTile();

        for (Node node : new ArrayList<>(tile.nodes)) {
            Grid grid;
            Map<Node, Node> neighborReplacements;
            ListIterator<Node> it;
            List<List<Node>> neighborGroups;
            int i;

            List<Node> neighbors = new ArrayList<>();
            Level world = enet.getWorld();

            for (IEnergyTile subTile : tile.subTiles) {
                for (Direction dir : Direction.values()) {
                    BlockPos coords = EnergyNetGlobal.getPos(subTile).relative(
                            java.util.Objects.requireNonNull(dir));
                    if (!world.isLoaded(coords)) continue;
                    Tile neighborTile = enet.registeredTiles.get(coords);
                    if (neighborTile != null && neighborTile != node.tile) {
                        for (Node neighbor : new ArrayList<>(neighborTile.nodes)) {
                            if (!neighbor.isExtraNode()) {
                                IEnergyTile neighborIoTile = neighbor.tile.getMainTile();
                                boolean canEmit = false;
                                if ((node.nodeType == NodeType.Source || node.nodeType == NodeType.Conductor) && neighbor.nodeType != NodeType.Source) {
                                    IEnergyEmitter emitter = (subTile instanceof IEnergyEmitter) ? (IEnergyEmitter) subTile : (IEnergyEmitter) ioTile;
                                    IEnergyTile neighborSubTe = neighborTile.getSubTileAt(coords);
                                    IEnergyAcceptor acceptor = (neighborSubTe instanceof IEnergyAcceptor) ? (IEnergyAcceptor) neighborSubTe : (IEnergyAcceptor) neighborIoTile;

                                    canEmit = emitter.emitsToEnergyAcceptor(neighborIoTile, dir) && acceptor.acceptsEnergyFrom(ioTile, dir.getOpposite());
                                    if (canEmit && !colorsCompatible(subTile, dir, neighborSubTe, dir.getOpposite()))
                                        canEmit = false;
                                }

                                boolean canAccept = false;
                                if (!canEmit && (node.nodeType == NodeType.Sink || node.nodeType == NodeType.Conductor) && neighbor.nodeType != NodeType.Sink) {
                                    IEnergyAcceptor acceptor = (subTile instanceof IEnergyAcceptor) ? (IEnergyAcceptor) subTile : (IEnergyAcceptor) ioTile;
                                    IEnergyTile neighborSubTe = neighborTile.getSubTileAt(coords);
                                    IEnergyEmitter emitter = (neighborSubTe instanceof IEnergyEmitter) ? (IEnergyEmitter) neighborSubTe : (IEnergyEmitter) neighborIoTile;

                                    canAccept = acceptor.acceptsEnergyFrom(neighborIoTile, dir) && emitter.emitsToEnergyAcceptor(ioTile, dir.getOpposite());
                                    if (canAccept && !colorsCompatible(neighborSubTe, dir.getOpposite(), subTile, dir))
                                        canAccept = false;
                                }

                                if (canEmit || canAccept)
                                    neighbors.add(neighbor);
                            }
                        }
                    }
                }
            }

            if (neighbors.isEmpty()) {
                Grid grid1 = new Grid(enet);
                grid1.add(node, neighbors);
                continue;
            }

            switch (node.nodeType) {
                case Conductor:
                    grid = null;
                    for (Node neighbor : neighbors) {
                        if (neighbor.nodeType == NodeType.Conductor || neighbor.links.isEmpty()) {
                            grid = neighbor.getGrid();
                            break;
                        }
                    }
                    if (grid == null) {
                        grid = new Grid(enet);
                    }

                    neighborReplacements = new HashMap<>();
                    it = neighbors.listIterator();

                    while (it.hasNext()) {
                        Node neighbor = it.next();
                        if (neighbor.getGrid() != grid) {
                            if (neighbor.nodeType != NodeType.Conductor && !neighbor.links.isEmpty()) {
                                boolean found = false;
                                for (int j = 0; j < it.previousIndex(); j++) {
                                    Node neighbor2 = neighbors.get(j);
                                    if (neighbor2.tile == neighbor.tile && neighbor2.nodeType == neighbor.nodeType && neighbor2.getGrid() == grid) {
                                        found = true;
                                        it.set(neighbor2);
                                        break;
                                    }
                                }
                                if (!found) {
                                    neighbor = new Node(enet.allocateNodeId(), neighbor.tile, neighbor.nodeType);
                                    neighbor.tile.addExtraNode(neighbor);
                                    grid.add(neighbor, Collections.emptyList());
                                    it.set(neighbor);
                                    assert neighbor.getGrid() != null;
                                }
                                continue;
                            }
                            grid.merge(neighbor.getGrid(), neighborReplacements);
                        }
                    }

                    it = neighbors.listIterator();
                    while (it.hasNext()) {
                        Node neighbor = it.next();
                        Node replacement = neighborReplacements.get(neighbor);
                        if (replacement != null) {
                            neighbor = replacement;
                            it.set(replacement);
                        }
                        assert neighbor.getGrid() == grid;
                    }

                    grid.add(node, neighbors);
                    assert node.getGrid() != null;
                    break;

                case Sink:
                case Source:
                    neighborGroups = new ArrayList<>();
                    for (Node neighbor : neighbors) {
                        boolean found = false;
                        for (List<Node> nodeList : neighborGroups) {
                            Node neighbor2 = nodeList.get(0);
                            if (neighbor2.nodeType == NodeType.Conductor && neighbor2.getGrid() == neighbor.getGrid()) {
                                nodeList.add(neighbor);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            List<Node> nodeList = new ArrayList<>();
                            nodeList.add(neighbor);
                            neighborGroups.add(nodeList);
                        }
                    }

                    assert !neighborGroups.isEmpty();

                    for (i = 0; i < neighborGroups.size(); i++) {
                        Node currentNode;
                        List<Node> nodeList = neighborGroups.get(i);
                        Node neighbor = nodeList.get(0);
                        if (neighbor.nodeType != NodeType.Conductor && !neighbor.links.isEmpty()) {
                            assert nodeList.size() == 1;
                            neighbor = new Node(enet.allocateNodeId(), neighbor.tile, neighbor.nodeType);
                            neighbor.tile.addExtraNode(neighbor);
                            (new Grid(enet)).add(neighbor, Collections.emptyList());
                            nodeList.set(0, neighbor);
                            assert neighbor.getGrid() != null;
                        }

                        if (i == 0) {
                            currentNode = node;
                        } else {
                            currentNode = new Node(enet.allocateNodeId(), tile, node.nodeType);
                            currentNode.setExtraNode(true);
                            extraNodes.add(currentNode);
                        }

                        neighbor.getGrid().add(currentNode, nodeList);
                        assert currentNode.getGrid() != null;
                    }
                    break;
            }

            enet.addTileToNotify(ioTile);
            for (Node neighbor : neighbors)
                enet.addTileToNotify(neighbor.getTile().getMainTile());
        }

        for (Node node : extraNodes)
            tile.addExtraNode(node);
    }

    static void applyRemoval(EnergyNetLocal enet, IEnergyTile ioTile, BlockPos pos) {
        Tile tile = enet.registeredIoTiles.remove(ioTile);
        if (tile == null) {
        } else {
            assert tile.getMainTile() == ioTile;
            if (ioTile instanceof IEnergySource)
                enet.sources.remove(tile);

            for (IEnergyTile subTile : tile.subTiles) {
                BlockPos subPos = EnergyNetGlobal.getPos(subTile);
                enet.registeredTiles.remove(subPos);
            }

            removeTileFromGrids(tile);
            enet.removeTileToNotify(ioTile);
        }
    }

    private static void removeTileFromGrids(Tile tile) {
        for (Node node : new ArrayList<>(tile.nodes)) {
            Grid grid = node.getGrid();
            if (grid != null) {
                grid.remove(node);
            }
        }
    }

    /**
     * 检查两个相邻能源方块的颜色是否兼容（电缆染色）。
     * 仅当两端都实现 {@link IColoredEnergyTile} 且两边都指定了颜色且颜色不同，才拒绝连接。
     * 任一侧为 null（默认/无颜色）表示接受任意连接。
     */
    private static boolean colorsCompatible(IEnergyTile a, Direction aDir, IEnergyTile b, Direction bDir) {
        if (a instanceof IColoredEnergyTile ca && b instanceof IColoredEnergyTile cb) {
            DyeColor ca0 = ca.getColor(aDir);
            DyeColor cb0 = cb.getColor(bDir);
            return !(ca0 != null && cb0 != null && ca0 != cb0);
        }
        return true;
    }
}