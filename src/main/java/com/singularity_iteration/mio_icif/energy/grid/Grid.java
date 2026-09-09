package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

public class Grid {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final int uid;
    private final EnergyNetLocal enet;
    private final Map<Integer, Node> nodes = new HashMap<>();

private boolean dirty;
    private Object data;

    Grid(EnergyNetLocal enet) {
        this.uid = enet.allocateGridId();
        this.enet = enet;
        enet.addGrid(this);
    }

    public EnergyNetLocal getEnergyNet() { return this.enet; }

    public Node getNode(int id) { return this.nodes.get(id); }

    public Collection<Node> getNodes() { return this.nodes.values(); }

    public boolean clearDirty() {
        if (!this.dirty) return false;
        this.dirty = false;
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() { return (T) this.data; }

    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() { return "Grid " + this.uid; }

    void add(Node node, Collection<Node> neighbors) {
        invalidate();
        assert !this.nodes.isEmpty() || neighbors.isEmpty();
        assert this.nodes.isEmpty() || !neighbors.isEmpty() || node.isExtraNode();
        assert node.links.isEmpty();
        add(node);

        for (Node neighbor : neighbors) {
            assert neighbor != node;
            assert this.nodes.containsKey(neighbor.uid);
            double loss = (node.getInnerLoss() + neighbor.getInnerLoss()) / 2.0D;
            NodeLink link = new NodeLink(node, neighbor, loss);
            node.links.add(link);
            neighbor.links.add(link);
        }
    }

    void remove(Node node) {
        invalidate();
        Iterator<NodeLink> it = node.links.iterator();

        while (it.hasNext()) {
            NodeLink link = it.next();
            Node neighbor = link.getNeighbor(node);
            boolean found = false;
            Iterator<NodeLink> it2 = neighbor.links.iterator();

            while (it2.hasNext()) {
                if (it2.next() == link) {
                    it2.remove();
                    found = true;
                    break;
                }
            }
            if (!found && EnergyNetSettings.logGridUpdateIssues) {
                LOGGER.info(uid + " Link not found in neighbor " + neighbor + " during removal of " + node);
            }
            this.enet.addTileToNotify(neighbor.getTile().getMainTile());
            if (neighbor.links.isEmpty() && neighbor.tile.removeExtraNode(neighbor)) {
                it.remove();
                this.nodes.remove(neighbor.uid);
                neighbor.clearGrid();
            }
        }

        this.nodes.remove(node.uid);
        node.clearGrid();
        int linkCount = node.links.size();
        if (linkCount == 0) {
            assert this.nodes.isEmpty();
            this.enet.removeGrid(this);
        } else if (linkCount > 1 && node.nodeType == NodeType.Conductor) {
            @SuppressWarnings("unchecked")
                Set<Node>[] arrayOfSet = new Set[linkCount];
            int[] mapping = new int[linkCount];
            int gridCount = 0;
            Queue<Node> nodesToCheck = new ArrayDeque<>();

            label145:
            for (int i = 0; i < linkCount; i++) {
                Node neighbor = node.links.get(i).getNeighbor(node);
                if (neighbor.getType() != NodeType.Conductor) {
                    if (neighbor.links.isEmpty()) {
                        arrayOfSet[i] = Collections.singleton(neighbor);
                        gridCount++;
                    } else {
                        mapping[i] = -1;
                    }
                } else {
                    for (int j = 0; j < i; j++) {
                        Set<Node> nodes = arrayOfSet[j];
                        if (nodes != null && nodes.contains(neighbor)) {
                            mapping[i] = j;
                            continue label145;
                        }
                    }
                    Set<Node> connectedNodes = Collections.newSetFromMap(new IdentityHashMap<>());
                    nodesToCheck.add(neighbor);
                    connectedNodes.add(neighbor);

                    Node cNode;
                    while ((cNode = nodesToCheck.poll()) != null) {
                        for (NodeLink link2 : new ArrayList<>(cNode.links)) {
                            Node nNode = link2.getNeighbor(cNode);
                            if (connectedNodes.add(nNode) && nNode.getType() == NodeType.Conductor)
                                nodesToCheck.add(nNode);
                        }
                    }
                    assert !connectedNodes.contains(node);
                    arrayOfSet[i] = connectedNodes;
                    gridCount++;
                }
            }

            assert gridCount > 0;
            if (gridCount <= 1) return;

            for (int i = 1; i < linkCount; i++) {
                Set<Node> connectedNodes = arrayOfSet[i];
if (connectedNodes != null) {
                    Grid grid = new Grid(this.enet);
                    grid.invalidate();

                    for (Node cNode : connectedNodes) {
                        boolean needsExtraNode = false;
                        if (!cNode.links.isEmpty() && cNode.nodeType != NodeType.Conductor) {
                            for (int j = 0; j < i; j++) {
                                Set<Node> ns = arrayOfSet[j];
                                if (ns != null && ns.contains(cNode)) {
                                    needsExtraNode = true;
                                    break;
                                }
                            }
                        }
                        if (needsExtraNode) {
                            Node extraNode = new Node(this.enet.allocateNodeId(), cNode.tile, cNode.nodeType);
                            cNode.tile.addExtraNode(extraNode);
                            Iterator<NodeLink> itx = cNode.links.iterator();
                            while (itx.hasNext()) {
                                NodeLink link2 = itx.next();
                                Node neighbor2 = link2.getNeighbor(cNode);
                                if (connectedNodes.contains(neighbor2)) {
                                    assert neighbor2.nodeType == NodeType.Conductor;
                                    link2.replaceNode(cNode, extraNode);
                                    extraNode.links.add(link2);
                                    itx.remove();
                                }
                            }
                            assert !extraNode.links.isEmpty();
                            grid.add(extraNode);
                            assert extraNode.getGrid() != null;
                        }

                        assert this.nodes.containsKey(cNode.uid);
                        this.nodes.remove(cNode.uid);
                        cNode.clearGrid();
                        grid.add(cNode);
                        assert cNode.getGrid() != null;
                    }
                }
            }
        }
    }

    void merge(Grid grid, Map<Node, Node> nodeReplacements) {
        assert this.enet.hasGrid(grid);
        invalidate();

        for (Node node : new ArrayList<>(grid.nodes.values())) {
            boolean found = false;
            if (node.nodeType != NodeType.Conductor) {
                for (Node node2 : new ArrayList<>(node.tile.nodes)) {
                    if (node2.nodeType == node.nodeType && node2.getGrid() == this) {
                        found = true;
                        for (NodeLink link : new ArrayList<>(node.links)) {
                            link.replaceNode(node, node2);
                            node2.links.add(link);
                        }
                        node2.tile.removeExtraNode(node);
                        nodeReplacements.put(node, node2);
                        break;
                    }
                }
            }
            if (!found) {
                node.clearGrid();
                add(node);
                assert node.getGrid() != null;
            }
        }

        this.enet.removeGrid(grid);
    }

    private void add(Node node) {
        node.setGrid(this);
        Node prev = this.nodes.put(node.uid, node);
        if (prev != null)
            throw new IllegalStateException("duplicate node uid, new " + node + ", old " + prev);
    }

    private void invalidate() { this.dirty = true; }

    void destroy() {
        for (Node node : new ArrayList<>(this.nodes.values())) {
            node.links.clear();
            node.clearGrid();
        }
        this.nodes.clear();
        this.data = null;
    }

    GridInfo getInfo() {
        int complexNodes = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Node node : new ArrayList<>(this.nodes.values())) {
            if (node.links.size() > 2) complexNodes++;

            for (IEnergyTile tile : node.tile.subTiles) {
                BlockPos pos = EnergyNetGlobal.getPos(tile);
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }
        }

        return new GridInfo(this.uid, this.nodes.size(), complexNodes, minX, minY, minZ, maxX, maxY, maxZ);
    }
}

