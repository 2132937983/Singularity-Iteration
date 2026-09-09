package com.singularity_iteration.mio_icif.energy.leg;
import com.singularity_iteration.mio_icif.api.energy.tile.IMultiEnergySource;
import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import com.singularity_iteration.mio_icif.energy.grid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.io.PrintStream;
import java.util.*;

@SuppressWarnings("null")
public class EnergyCalculator implements IEnergyCalculator {

    private static class GridData {
        boolean active;
        final Map<Node, List<EnergyPath>> energySourceToEnergyPathMap = new IdentityHashMap<>();
        final Map<Node, List<EnergyPath>> sinkToPathsMap = new IdentityHashMap<>();
        final Map<Node, List<EnergyPath>> conductorToPathsMap = new IdentityHashMap<>();
        final List<Node> activeSources = new ArrayList<>();
        final Map<Node, org.apache.commons.lang3.mutable.MutableDouble> activeSinks = new IdentityHashMap<>();
        final Set<EnergyPath> eventPaths = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Node, List<EnergyPath>> pathCache = new IdentityHashMap<>();
        final List<EnergyPath> overloadPaths = new ArrayList<>();
        final Map<Node, org.apache.commons.lang3.mutable.MutableDouble> sinkDemandPool = new IdentityHashMap<>();
        int currentCalcId = -1;
    }

    private static class OptLink {
        final Node nodeA;
        final Node nodeB;
        final double loss;
        final List<Node> skippedNodes;

        OptLink(Node nodeA, Node nodeB, double loss, List<Node> skippedNodes) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
            this.loss = loss;
            this.skippedNodes = skippedNodes;
        }

        Node getNeighbor(Node node) {
            return (this.nodeA == node) ? this.nodeB : this.nodeA;
        }
    }

    private static class OptimizedGraph {
        final Map<Node, List<OptLink>> nodeToLinks;

        OptimizedGraph(Map<Node, List<OptLink>> nodeToLinks) {
            this.nodeToLinks = nodeToLinks;
        }
    }

    @Override
    public void handleGridChange(Grid grid) {
        GridData data = getData(grid);
        updateCache(grid, data);
    }

    @Override
    public boolean runSyncStep(EnergyNetLocal enet) {
        boolean foundAny = false;

        Collection<Tile> sources = enet.getSources();

        for (Tile tile : sources) {
            IEnergySource source = (IEnergySource) tile.getMainTile();
            double amount;
            boolean disabled = tile.isDisabled();
            double offered = source.getOfferedEnergy();

            if (!disabled && (amount = offered) > 0.0D) {
                int tier = source.getSourceTier();
                if (tier < 0) {
                    tile.setSourceData(0.0D, 0);
                    continue;
                }
                foundAny = true;
                int packets = source.getPacketCount();
                double power = EnergyNetGlobal.getPowerFromTier(tier);
                if (source instanceof IMultiEnergySource m && m.sendMultipleEnergyPackets()) {
                    int packetAmount = m.getMultipleEnergyPacketAmount();
                    if (packetAmount > 0) {
                        power = packetAmount;
                        packets = (int) Math.max(1, Math.ceil(amount / (double) packetAmount));
                    }
                }
                amount = Math.min(amount, power * packets);
                tile.setSourceData(amount, packets);
            } else {
                tile.setSourceData(0.0D, 0);
            }
        }
        return foundAny;
    }

    @Override
    public boolean runSyncStep(Grid grid) {
        GridData data = getData(grid);
        if (data == null || !data.active) return false;
        return true;
    }

    @Override
    public void runAsyncStep(Grid grid) {
        GridData data = getData(grid);
        if (data.active) {
            runCalculation(grid, data);
        }
    }

    @Override
    public NodeStats getNodeStats(Tile tile) {
        double in = 0.0D, out = 0.0D, maxOffered = 0.0D;
        for (Node node : tile.getNodes()) {
            Grid grid = node.getGrid();
            if (grid == null) continue;
            GridData data = getData(grid);
            if (data == null || !data.active) continue;
            int calcId = data.currentCalcId;
            Collection<EnergyPath> paths = getPaths(node, data);
            double sum = 0.0D;
            for (EnergyPath path : paths) {
                if (path.lastCalcId != calcId) continue;
                sum += path.energySupplied;
                maxOffered = Math.max(path.maxPacketOffered, maxOffered);
            }
            if (node.getType() == NodeType.Source) {
                out += sum;
            } else if (node.getType() == NodeType.Sink) {
                in += sum;
            } else {
                in += sum;
                out += sum;
            }
        }
        return new NodeStats(in, out, maxOffered);
    }

    @Override
    public void dumpNodeInfo(Node node, String prefix, PrintStream console, PrintStream chat) {
        Grid grid = node.getGrid();
        if (grid == null) {
            chat.printf("%sNode has no grid%n", prefix);
            return;
        }
        GridData data = getData(grid);
        Collection<EnergyPath> paths = getPaths(node, data);

        switch (node.getType()) {
            case Source: chat.printf("%s%d connected sink nodes%n", prefix, paths.size()); break;
            case Sink: chat.printf("%s%d connected source nodes%n", prefix, paths.size()); break;
            case Conductor: chat.printf("%s%d paths across this conductor%n", prefix, paths.size()); break;
        }

        double sum = 0.0D, maxConducted = 0.0D, maxOffered = 0.0D;
        int calcId = data.currentCalcId;
        for (EnergyPath path : paths) {
            if (path.lastCalcId != calcId) continue;
            sum += path.energySupplied;
            maxConducted = Math.max(path.maxPacketConducted, maxConducted);
            maxOffered = Math.max(path.maxPacketOffered, maxOffered);
        }
        chat.printf("%slast tick: %.2f EU, max packet %.2f EU, max offered %.2f EU%n", prefix, sum, maxConducted, maxOffered);
    }

    // ---- Optimized Graph Construction ----

    private static OptimizedGraph buildOptimizedGraph(Collection<Node> nodes) {
        boolean changed;
        Map<Node, List<OptLink>> nodeToLinks = new IdentityHashMap<>();

        for (Node node : nodes) {
            nodeToLinks.put(node, new ArrayList<>());
        }

        for (Node node : nodes) {
            for (NodeLink link : node.getLinks()) {
                if (link.getNodeA() == node) {
                    OptLink optLink = new OptLink(link.getNodeA(), link.getNodeB(), link.getLoss(), new ArrayList<>());
                    List<OptLink> linksA = nodeToLinks.get(link.getNodeA());
                    List<OptLink> linksB = nodeToLinks.get(link.getNodeB());
                    if (linksA != null) linksA.add(optLink);
                    if (linksB != null) linksB.add(optLink);
                }
            }
        }

        do {
            changed = false;
            Iterator<Map.Entry<Node, List<OptLink>>> it = nodeToLinks.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<Node, List<OptLink>> entry = it.next();
                Node node = entry.getKey();
                if (node.getType() != NodeType.Conductor) continue;
                List<OptLink> links = entry.getValue();
                if (links.isEmpty()) {
                    it.remove();
                    changed = true;
                    continue;
                }
                if (links.size() == 1) {
                    OptLink link = links.get(0);
                    Node neighbor = link.getNeighbor(node);
                    List<OptLink> neighborLinks = nodeToLinks.get(neighbor);
                    if (neighborLinks != null) {
                        neighborLinks.remove(link);
                    }
                    it.remove();
                    changed = true;
                    continue;
                }
                if (links.size() == 2) {
                    OptLink link1 = links.get(0);
                    OptLink link2 = links.get(1);
                    Node neighbor1 = link1.getNeighbor(node);
                    Node neighbor2 = link2.getNeighbor(node);

                    List<OptLink> links1 = nodeToLinks.get(neighbor1);
                    List<OptLink> links2 = nodeToLinks.get(neighbor2);
                    if (links1 != null) links1.remove(link1);
                    if (links2 != null) links2.remove(link2);

                    if (neighbor1 != neighbor2) {
                        List<Node> skipped = new ArrayList<>(link1.skippedNodes);
                        skipped.add(node);
                        skipped.addAll(link2.skippedNodes);

                        double combinedLoss = link1.loss + link2.loss;
                        OptLink merged = new OptLink(neighbor1, neighbor2, combinedLoss, skipped);
                        if (links1 != null) links1.add(merged);
                        if (links2 != null) links2.add(merged);
                    }
                    it.remove();
                    changed = true;
                }
            }
        } while (changed);

        return new OptimizedGraph(nodeToLinks);
    }

    // ---- Cache Update (Dijkstra with OptimizedGraph) ----

    private static void updateCache(Grid grid, GridData data) {
        data.active = false;
        data.energySourceToEnergyPathMap.clear();
        data.sinkToPathsMap.clear();
        data.conductorToPathsMap.clear();
        data.activeSources.clear();
        data.activeSinks.clear();
        data.pathCache.clear();
        data.overloadPaths.clear();
        data.currentCalcId = -1;

        Collection<Node> nodes = grid.getNodes();
        if (nodes.size() < 2) {
            return;
        }

        List<Node> sources = new ArrayList<>();
        int sinkCount = 0;
        for (Node node : nodes) {
            if (node.getType() == NodeType.Source) sources.add(node);
            else if (node.getType() == NodeType.Sink) sinkCount++;
        }
        if (sources.isEmpty() || sinkCount == 0) return;

        OptimizedGraph optGraph = buildOptimizedGraph(nodes);
        Map<Node, Node> parentMap = new IdentityHashMap<>();
        Map<Node, OptLink> incomingLinkMap = new IdentityHashMap<>();
        Map<Node, Double> lossMap = new IdentityHashMap<>();
        Map<IEnergyTile, EnergyPath> pathsMap = new LinkedHashMap<>();

        Queue<Node> queue;
        if (sources.size() <= EnergyNetSettings.pathfindingThreshold) {
            queue = new PriorityQueue<>(nodes.size(),
                    (a, b) -> {
                        Double la = lossMap.get(a);
                        Double lb = lossMap.get(b);
                        if (la == null && lb == null) return 0;
                        if (la == null) return 1;
                        if (lb == null) return -1;
                        return Double.compare(la, lb);
                    });
        } else {
            queue = new ArrayDeque<>(nodes.size());
        }

        for (Node srcNode : sources) {
            lossMap.put(srcNode, 0.0D);
            queue.add(srcNode);

            Node node;
            while ((node = queue.poll()) != null) {
                Double nodeLoss = lossMap.get(node);
                if (nodeLoss == null) continue;

                if (node.getType() == NodeType.Sink) {
                    double loss = nodeLoss;
                    IEnergyTile tile = node.getTile().getMainTile();
                    EnergyPath prev = pathsMap.get(tile);
                    if (prev != null && prev.loss <= loss) continue;
                    if (EnergyNetSettings.roundLossDown) loss = Math.floor(loss);
                    pathsMap.put(tile, new EnergyPath(srcNode, node, reconstructPathWithLinks(srcNode, node, parentMap, incomingLinkMap), loss));
                    if (pathsMap.size() == sinkCount) break;
                } else if (node.getType() == NodeType.Conductor || node == srcNode) {
                    double loss = nodeLoss;
                    List<OptLink> optLinks = optGraph.nodeToLinks.get(node);
                    if (optLinks != null) {
                        for (OptLink optLink : optLinks) {
                            Node neighbor = optLink.getNeighbor(node);
                            if (neighbor.getType() == NodeType.Source) {
                                List<EnergyPath> srcPaths = data.energySourceToEnergyPathMap.get(neighbor);
                                if (srcPaths != null && !srcPaths.isEmpty()) {
                                    loss -= optLink.loss;
                                    List<Node> pathToHere = null;
                                    for (EnergyPath cPath : srcPaths) {
                                        double cLoss = loss + cPath.loss;
                                        IEnergyTile t = cPath.target.getTile().getMainTile();
                                        EnergyPath p = pathsMap.get(t);
                                        if (p != null && p.loss <= cLoss) continue;
                                        if (EnergyNetSettings.roundLossDown) cLoss = Math.floor(cLoss);
                                        if (pathToHere == null) {
                                            pathToHere = reconstructPathWithLinks(srcNode, node, parentMap, incomingLinkMap);
                                        }
                                        List<Node> conductors = new ArrayList<>(pathToHere.size() + optLink.skippedNodes.size() + cPath.conductors.size());
                                        conductors.addAll(pathToHere);
                                        conductors.addAll(optLink.skippedNodes);
                                        conductors.addAll(cPath.conductors);
                                        Direction targetDir = cPath.conductors.isEmpty() ? cPath.targetDirection : null;
                                        pathsMap.put(t, new EnergyPath(srcNode, cPath.target, conductors, cLoss, targetDir));
                                    }
                                }
                            } else {
                                double newLoss = loss + optLink.loss;
                                Double prevLoss = lossMap.get(neighbor);
                                if (prevLoss == null || prevLoss > newLoss) {
                                    lossMap.put(neighbor, newLoss);
                                    parentMap.put(neighbor, node);
                                    incomingLinkMap.put(neighbor, optLink);
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }

            if (!pathsMap.isEmpty()) {
                data.energySourceToEnergyPathMap.put(srcNode, new ArrayList<>(pathsMap.values()));
            }
            lossMap.clear();
            parentMap.clear();
            incomingLinkMap.clear();
            pathsMap.clear();
            queue.clear();
        }

        if (!data.energySourceToEnergyPathMap.isEmpty()) {
            data.active = true;
        }

        data.overloadPaths.clear();
        for (Map.Entry<Node, List<EnergyPath>> entry : data.energySourceToEnergyPathMap.entrySet()) {
            Node sourceNode = entry.getKey();
            IEnergySource energySource = (IEnergySource) sourceNode.getTile().getMainTile();
            int sourceTier = energySource.getSourceTier();
            if (sourceTier < 0) continue;
            double sourcePower = EnergyNetGlobal.getPowerFromTier(sourceTier);

            for (EnergyPath path : entry.getValue()) {
                if (sourcePower > path.minConductorBreakdownEnergy ||
                    sourcePower > path.minInsulationBreakdownEnergy) {
                    data.overloadPaths.add(path);
                    continue;
                }
                IEnergySink sink = (IEnergySink) path.target.getTile().getMainTile();
                int sinkTier = sink.getSinkTier();
                if (sinkTier >= 0 && sourcePower > EnergyNetGlobal.getPowerFromTier(sinkTier)) {
                    data.overloadPaths.add(path);
                }
            }
        }

        for (List<EnergyPath> pathList : data.energySourceToEnergyPathMap.values()) {
            for (EnergyPath path : pathList) {
                data.sinkToPathsMap.computeIfAbsent(path.target, k -> new ArrayList<>()).add(path);
                for (Node conductor : path.conductors) {
                    data.conductorToPathsMap.computeIfAbsent(conductor, k -> new ArrayList<>()).add(path);
                }
            }
        }
    }

    private static List<Node> reconstructPathWithLinks(Node srcNode, Node dstNode,
                                                       Map<Node, Node> parentMap,
                                                       Map<Node, OptLink> incomingLinkMap) {
        List<Node> ret = new ArrayList<>();
        Node node = dstNode;

        while (true) {
            Node parent = parentMap.get(node);
            if (parent == null) break;
            OptLink link = incomingLinkMap.get(node);
            if (link != null) {
                List<Node> skipped;
                if (link.nodeA == parent) {
                    skipped = new ArrayList<>(link.skippedNodes);
                    Collections.reverse(skipped);
                } else {
                    skipped = link.skippedNodes;
                }
                ret.addAll(skipped);
            }
            if (parent == srcNode) break;
            ret.add(parent);
            node = parent;
        }

        Collections.reverse(ret);
        return ret;
    }

    // ---- Energy Distribution ----

    private static boolean runCalculation(Grid grid, GridData data) {
        if (!data.active) {
            return false;
        }

        List<Node> activeSources = data.activeSources;
        Map<Node, org.apache.commons.lang3.mutable.MutableDouble> activeSinks = data.activeSinks;
        activeSources.clear();
        activeSinks.clear();
        int calcId = ++data.currentCalcId;

        for (Node node : grid.getNodes()) {
            Tile tile = node.getTile();
            if (tile.isDisabled()) continue;
            if (node.getType() == NodeType.Source
                    && data.energySourceToEnergyPathMap.containsKey(node)
                    && tile.getAmount() > 0.0D) {
                activeSources.add(node);
            } else if (node.getType() == NodeType.Sink) {
                double amount = ((IEnergySink) tile.getMainTile()).getDemandedEnergy();
                if (amount > 0.0D) {
                    org.apache.commons.lang3.mutable.MutableDouble demand = data.sinkDemandPool.get(node);
                    if (demand == null) {
                        demand = new org.apache.commons.lang3.mutable.MutableDouble(amount);
                        data.sinkDemandPool.put(node, demand);
                    } else {
                        demand.setValue(amount);
                    }
                    activeSinks.put(node, demand);
                }
            }
        }

        if (activeSources.isEmpty() || activeSinks.isEmpty()) {
            return false;
        }

        Level world = grid.getEnergyNet().getWorld();
        net.minecraft.util.RandomSource rand = world.random;
        boolean shufflePaths = (world.getGameTime() & 3L) != 0L;

        int sourcesOffset = (activeSources.size() > 1) ? rand.nextInt(activeSources.size()) : 0;

        for (int i = sourcesOffset; i < activeSources.size() && !activeSinks.isEmpty(); i++) {
            distribute(activeSources.get(i), data, shufflePaths, calcId, rand);
        }
        for (int i = 0; i < sourcesOffset && !activeSinks.isEmpty(); i++) {
            distribute(activeSources.get(i), data, shufflePaths, calcId, rand);
        }

        for (EnergyPath path : data.overloadPaths) {
            Node sourceNode = path.source;
            IEnergySource energySource = (IEnergySource) sourceNode.getTile().getMainTile();
            int sourceTier = energySource.getSourceTier();
            if (sourceTier < 0) continue;
            double sourcePower = EnergyNetGlobal.getPowerFromTier(sourceTier);

            if (path.maxPacketConducted < sourcePower) {
                path.maxPacketConducted = sourcePower;
            }
            data.eventPaths.add(path);
        }

        if (!data.eventPaths.isEmpty()) {
            applyCableEffects(data.eventPaths, grid.getEnergyNet().getWorld());
            data.eventPaths.clear();
        }
        return true;
    }

    private static void distribute(Node srcNode, GridData data, boolean shufflePaths, int calcId, net.minecraft.util.RandomSource rand) {
        Tile tile = srcNode.getTile();
        int packetCount = tile.getPacketCount();
        List<EnergyPath> paths = data.energySourceToEnergyPathMap.get(srcNode);
        if (paths == null || paths.isEmpty()) {
            return;
        }

        int pathOffset = (paths.size() > 1 && shufflePaths) ? rand.nextInt(paths.size()) : 0;
        double totalOffer = tile.getAmount();

        double offer;
        if (packetCount == 1) {
            offer = distributeSingle(totalOffer, paths, pathOffset, data, calcId);
        } else {
            offer = distributeMultiple(totalOffer, paths, pathOffset, data, calcId, packetCount, srcNode);
        }

        double used = totalOffer - Math.max(0.0D, offer);
        if (used > 0.0D) {
            tile.setAmount(offer);
            ((IEnergySource) tile.getMainTile()).drawEnergy(used);
        }
    }

    private static double distributeSingle(double offer, List<EnergyPath> paths, int pathOffset, GridData data, int calcId) {
        int i;
        for (i = pathOffset; i < paths.size(); i++) {
            offer -= emit(paths.get(i), offer, data, calcId);
            if (offer <= 0.0D) break;
        }

        for (i = 0; i < pathOffset && offer > 0.0D; i++) {
            offer -= emit(paths.get(i), offer, data, calcId);
        }

        return offer;
    }

    private static double distributeMultiple(double offer, List<EnergyPath> paths, int pathOffset,
                                              GridData data, int calcId, int packetCount, Node srcNode) {
        IEnergySource source = (IEnergySource) srcNode.getTile().getMainTile();
        double power = EnergyNetGlobal.getPowerFromTier(source.getSourceTier());
        if (source instanceof IMultiEnergySource m && m.sendMultipleEnergyPackets()) {
            int packetAmount = m.getMultipleEnergyPacketAmount();
            if (packetAmount > 0) power = packetAmount;
        }
        do {
            double cOffer = Math.min(offer, power);
            double used = cOffer - distributeSingle(cOffer, paths, pathOffset, data, calcId);
            if (used <= 0.0D) break;
            offer -= used;
        } while (--packetCount > 0 && offer > 0.0D);
        return offer;
    }

    private static double emit(EnergyPath path, double offer, GridData data, int calcId) {
        Tile targetTile = path.target.getTile();
        if (targetTile.isDisabled()) {
            return 0.0D;
        }

        double injectAmount = offer - path.loss;
        if (injectAmount <= 0.0D) {
           return 0.0D;
        }

        org.apache.commons.lang3.mutable.MutableDouble sinkDemand = data.activeSinks.get(path.target);
        if (sinkDemand == null) {
            return 0.0D;
        }

        IEnergySink sink = (IEnergySink) targetTile.getMainTile();
        double amount = Math.min(injectAmount, sinkDemand.doubleValue());

        double rejected = sink.injectEnergy(path.targetDirection, amount, EnergyNetGlobal.getTierFromPower(offer));
        if (rejected >= amount) return 0.0D;

        double accepted = amount - rejected;
        double effectiveAmount = accepted + path.loss;

        sinkDemand.subtract(accepted);

        for (Node node : path.conductors) {
            Object mainTile = node.getTile().getMainTile();
            if (mainTile instanceof IEnergyConductor conductor) {
                conductor.onEnergyPass();
            }
        }

        if (path.lastCalcId != calcId) {
            path.lastCalcId = calcId;
            path.energySupplied = 0.0D;
            path.maxPacketConducted = 0.0D;
            path.maxPacketOffered = 0.0D;
        }
        path.energySupplied += accepted;
        path.maxPacketConducted = Math.max(effectiveAmount, path.maxPacketConducted);
        path.maxPacketOffered = Math.max(offer, path.maxPacketOffered);

        int sinkTier = sink.getSinkTier();
        if (effectiveAmount > path.minEffectEnergy || (sinkTier >= 0 && effectiveAmount > EnergyNetGlobal.getPowerFromTier(sinkTier))) {
            data.eventPaths.add(path);
        }
        if (amount >= sinkDemand.doubleValue() || rejected > 0.0D) {
            data.activeSinks.remove(path.target);
        }
        return effectiveAmount;
    }

    // ---- Cable Effects (Overload, Shock, Explosion) ----

    private static void applyCableEffects(Collection<EnergyPath> eventPaths, Level world) {
        if (!Singularity_Iteration_Config.ENERGY_NET_ENABLE_VOLTAGE_OVERLOAD.get()) {
            return;
        }

        Set<Tile> cablesToRemove = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Tile> cablesToStrip = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Tile, org.apache.commons.lang3.mutable.MutableDouble> sinksToExplode = new IdentityHashMap<>();
        Map<net.minecraft.world.entity.LivingEntity, org.apache.commons.lang3.mutable.MutableDouble> shockEnergyMap = new IdentityHashMap<>();

        for (EnergyPath path : eventPaths) {
            double amount = path.maxPacketConducted;

            if (amount > path.minConductorBreakdownEnergy || amount > path.minInsulationBreakdownEnergy) {
                for (Node node : path.conductors) {
                    Tile tile = node.getTile();
                    IEnergyConductor conductor = (IEnergyConductor) tile.getMainTile();
                    if (amount > conductor.getConductorBreakdownEnergy()) {
                        cablesToRemove.add(tile);
                    } else if (amount > conductor.getInsulationBreakdownEnergy()) {
                        cablesToStrip.add(tile);
                    }
                }
            }

            if (amount > path.minInsulationEnergyAbsorption) {
                List<net.minecraft.world.entity.LivingEntity> nearbyEntities = world.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(
                                path.minX - 1, path.minY - 1, path.minZ - 1,
                                path.maxX + 2, path.maxY + 2, path.maxZ + 2));

                if (!nearbyEntities.isEmpty()) {
                    Map<net.minecraft.world.entity.LivingEntity, org.apache.commons.lang3.mutable.MutableDouble> localShockMap = new IdentityHashMap<>();

                    for (Node node : path.conductors) {
                        Tile tile = node.getTile();
                        IEnergyConductor conductor = (IEnergyConductor) tile.getMainTile();
                        if (amount <= conductor.getInsulationEnergyAbsorption()) continue;

                        int shockEnergy = (int) (amount - conductor.getInsulationEnergyAbsorption());

                        for (IEnergyTile subTile : tile.getSubTiles()) {
                            BlockPos pos = EnergyNetGlobal.getPos(subTile);
                            for (net.minecraft.world.entity.LivingEntity entity : nearbyEntities) {
                                org.apache.commons.lang3.mutable.MutableDouble prev = localShockMap.get(entity);
                                if (prev != null && prev.doubleValue() >= shockEnergy) continue;
                                if (!entity.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(
                                        pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                                        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2))) continue;
                                if (prev == null) {
                                    localShockMap.put(entity, new org.apache.commons.lang3.mutable.MutableDouble(shockEnergy));
                                } else {
                                    prev.setValue(shockEnergy);
                                }
                            }
                        }
                    }

                    for (Map.Entry<net.minecraft.world.entity.LivingEntity, org.apache.commons.lang3.mutable.MutableDouble> entry : localShockMap.entrySet()) {
                        org.apache.commons.lang3.mutable.MutableDouble prev = shockEnergyMap.get(entry.getKey());
                        if (prev == null) {
                            shockEnergyMap.put(entry.getKey(), new org.apache.commons.lang3.mutable.MutableDouble(entry.getValue().doubleValue()));
                        } else {
                            prev.add(entry.getValue().doubleValue());
                        }
                    }
                }
            }

            {
                Tile targetTile = path.target.getTile();
                IEnergySink sink = (IEnergySink) targetTile.getMainTile();
                int sinkTier = sink.getSinkTier();
                if (sinkTier >= 0 && amount > EnergyNetGlobal.getPowerFromTier(sinkTier)) {
                    org.apache.commons.lang3.mutable.MutableDouble explodeEnergy = sinksToExplode.get(targetTile);
                    if (explodeEnergy == null) {
                        sinksToExplode.put(targetTile, new org.apache.commons.lang3.mutable.MutableDouble(amount));
                    } else if (explodeEnergy.doubleValue() < amount) {
                        explodeEnergy.setValue(amount);
                    }
                }
            }
        }

        cablesToStrip.removeAll(cablesToRemove);

        for (Tile tile : cablesToRemove) {
            ((IEnergyConductor) tile.getMainTile()).removeConductor();
        }

        for (Tile tile : cablesToStrip) {
            ((IEnergyConductor) tile.getMainTile()).removeInsulation();
        }

        for (Map.Entry<Tile, org.apache.commons.lang3.mutable.MutableDouble> entry : sinksToExplode.entrySet()) {
            Tile tile = entry.getKey();
            double energy = entry.getValue().doubleValue();
            BlockPos pos = EnergyNetGlobal.getPos(tile.getMainTile());
            net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof com.singularity_iteration.mio_icif.api.energy.tile.IExplosionPowerOverride override && !override.shouldExplode()) {
                continue;
            }
            float explosionPower = (float) Math.min(energy / 1024.0D, 2.0D + EnergyNetGlobal.getTierFromPower(energy) * 0.5D);
            if (be instanceof com.singularity_iteration.mio_icif.api.energy.tile.IExplosionPowerOverride override) {
                explosionPower = override.getExplosionPower(EnergyNetGlobal.getTierFromPower(energy), explosionPower);
            }
            world.removeBlock(pos, false);
            if (explosionPower > 0.0F) {
                world.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, explosionPower, Level.ExplosionInteraction.BLOCK);
            }
        }

        for (Map.Entry<net.minecraft.world.entity.LivingEntity, org.apache.commons.lang3.mutable.MutableDouble> entry : shockEnergyMap.entrySet()) {
            net.minecraft.world.entity.LivingEntity entity = entry.getKey();
            double energy = entry.getValue().doubleValue();
            entity.hurt(world.damageSources().lightningBolt(), (float) (energy / 64.0D));
        }
    }

    // ---- Helpers ----

    private static GridData getData(Grid grid) {
        GridData data = grid.getData();
        if (data == null) {
            data = new GridData();
            grid.setData(data);
        }
        return data;
    }

    private static Collection<EnergyPath> getPaths(Node node, GridData data) {
        List<EnergyPath> ret = data.pathCache.get(node);
        if (ret != null) return ret;

        ret = new ArrayList<>();
        if (node.getType() == NodeType.Source) {
            List<EnergyPath> paths = data.energySourceToEnergyPathMap.get(node);
            if (paths != null) ret.addAll(paths);
        } else if (node.getType() == NodeType.Sink) {
            List<EnergyPath> paths = data.sinkToPathsMap.get(node);
            if (paths != null) ret.addAll(paths);
        } else {
            List<EnergyPath> paths = data.conductorToPathsMap.get(node);
            if (paths != null) ret.addAll(paths);
        }
        data.pathCache.put(node, ret);
        return ret;
    }
}