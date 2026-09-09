package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 核弹爆炸任务 - Bresenham 3D + Octree 优化版本
 *
 * 特点：
 * - 使用八叉树空间分割加速射线追踪
 * - Bresenham 3D算法高效遍历体素
 * - 适合超大规模爆炸（半径1000+）
 * - 比斐波那契球面分布更高效
 */
@SuppressWarnings("null")
public class NukeExplosionTaskOctree {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower;
    private final int radius;
    private final int radiusY;
    private final List<Entity> entities;
    private final Set<BlockPos> destroyedBlocks = new HashSet<>();
    private final Set<BlockPos> modifiedBlocks = new HashSet<>();

    // 八叉树根节点
    private OctreeNode octreeRoot;

    // 射线追踪状态
private int currentRay = 0;
    private int rayCount;
    private boolean raysComplete = false;
    private boolean damageComplete = false;
    private boolean radiationComplete = false;
    private boolean naturalBlocksCleanupComplete = false;

    // Bresenham 3D 射线列表
    private final List<BresenhamRay> bresenhamRays = new ArrayList<>();

    // 辐射区处理参数
private int radiationProgressN = 1;
    private int radiationProgressNLimit;
    private int radiationShell;
    private int radiationLeg;
    private int radiationElement;
    private int radiationLastX = 0;
    private int radiationLastZ = 0;

    // 自然方块清理参数
    private int naturalCleanupN = 1;
    private int naturalCleanupNLimit;
    private int naturalCleanupShell;
    private int naturalCleanupLeg;
    private int naturalCleanupElement;
    private int naturalCleanupLastX = 0;
    private int naturalCleanupLastZ = 0;

    public NukeExplosionTaskOctree(ServerLevel level, Vec3 center, float totalPower, int radius, int radiusY, List<Entity> entities) {
        this.level = level;
        this.center = center;
        this.totalPower = totalPower;
        this.radius = radius;
        this.radiusY = radiusY;
        this.entities = entities;

        // 计算射线数量：基于表面积，增加密度确保覆盖完整
    double surfaceArea = 4 * Math.PI * radius * radiusY;
        this.rayCount = Math.min((int) (surfaceArea / 2), 50000); // 增加射线密度
        this.rayCount = Math.max(rayCount, 2000); // 最少2000条射线
        // 初始化八叉树
        initializeOctree();

        // 生成Bresenham 3D射线
        generateBresenhamRays();

        // 初始化辐射区参数
        this.radiationProgressNLimit = radius * radius * 4;
        this.naturalCleanupNLimit = radius * radius * 4;
    }

    /**
     * 八叉树节点类
     */
    private static class OctreeNode {
        final BlockPos min;  // 最小边界
    final BlockPos max;  // 最大边界
    final int size;      // 边长
        OctreeNode[] children; // 8个子节点
        boolean isSolid;     // 是否全是固体方块
        boolean isEmpty;     // 是否全是空气
        boolean isLeaf;      // 是否为叶子节点
        OctreeNode(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
            this.size = max.getX() - min.getX();
            this.isLeaf = true;
            this.isSolid = false;
            this.isEmpty = true;
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() < max.getX() &&
                   pos.getY() >= min.getY() && pos.getY() < max.getY() &&
                   pos.getZ() >= min.getZ() && pos.getZ() < max.getZ();
        }
    }

    /**
     * Bresenham 3D 射线类
 */
    private static class BresenhamRay {
        final Vec3 direction;
        final int steps;
        final float power;

        BresenhamRay(Vec3 direction, int steps, float power) {
            this.direction = direction;
            this.steps = steps;
            this.power = power;
        }
    }

    /**
     * 初始化八叉树
     */
    private void initializeOctree() {
        int size = Math.max(radius, radiusY) * 2;
        // 确保size是2的幂
        size = Integer.highestOneBit(size - 1) << 1;
        if (size < 16) size = 16;

        BlockPos centerPos = BlockPos.containing(center);
        BlockPos min = new BlockPos(centerPos.getX() - size/2, centerPos.getY() - size/2, centerPos.getZ() - size/2);
        BlockPos max = new BlockPos(centerPos.getX() + size/2, centerPos.getY() + size/2, centerPos.getZ() + size/2);

        octreeRoot = new OctreeNode(min, max);
        buildOctree(octreeRoot, 4); // 最小节点大小为4
    }

    /**
     * 递归构建八叉树
 */
    private void buildOctree(OctreeNode node, int minSize) {
        if (node.size <= minSize) {
            node.isLeaf = true;
            analyzeNode(node);
            return;
        }

        // 检查节点区域是否完全在爆炸范围外
    double distToCenter = Math.sqrt(
            Math.pow((node.min.getX() + node.max.getX()) / 2.0 - center.x, 2) +
            Math.pow((node.min.getY() + node.max.getY()) / 2.0 - center.y, 2) +
            Math.pow((node.min.getZ() + node.max.getZ()) / 2.0 - center.z, 2)
        );

        if (distToCenter > Math.max(radius, radiusY) * 1.5) {
            // 完全在爆炸范围外，标记为空并返回
            node.isLeaf = true;
            node.isEmpty = true;
            node.isSolid = false;
            return;
        }

        // 创建8个子节点
        node.children = new OctreeNode[8];
        int halfSize = node.size / 2;

        for (int i = 0; i < 8; i++) {
            int dx = (i & 1) * halfSize;
            int dy = ((i >> 1) & 1) * halfSize;
            int dz = ((i >> 2) & 1) * halfSize;

            BlockPos childMin = new BlockPos(node.min.getX() + dx, node.min.getY() + dy, node.min.getZ() + dz);
            BlockPos childMax = new BlockPos(childMin.getX() + halfSize, childMin.getY() + halfSize, childMin.getZ() + halfSize);

            node.children[i] = new OctreeNode(childMin, childMax);
            buildOctree(node.children[i], minSize);
        }

        node.isLeaf = false;

        // 合并子节点状态
    node.isSolid = true;
        node.isEmpty = true;
        for (OctreeNode child : node.children) {
            if (!child.isSolid) node.isSolid = false;
            if (!child.isEmpty) node.isEmpty = false;
        }
    }

    /**
     * 分析叶子节点的方块状态
 */
    private void analyzeNode(OctreeNode node) {
        int solidCount = 0;
        int emptyCount = 0;
        int total = 0;

        for (int x = node.min.getX(); x < node.max.getX(); x++) {
            for (int y = node.min.getY(); y < node.max.getY(); y++) {
                for (int z = node.min.getZ(); z < node.max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) {
                        emptyCount++;
                    } else {
                        solidCount++;
                    }
                    total++;
                }
            }
        }

        node.isEmpty = emptyCount == total;
        node.isSolid = solidCount == total;
    }

    /**
     * 生成Bresenham 3D射线
     * 使用改进的球面分布算法
 */
    private void generateBresenhamRays() {
        // 使用黄金角螺旋分布在球面上生成射线起点
    double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // 黄金角
        for (int i = 0; i < rayCount; i++) {
            double y = 1.0 - (i / (double) (rayCount - 1)) * 2.0;
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double theta = phi * i;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            // 椭球体调整
        y = y * radiusY / Math.max(radius, radiusY);

            Vec3 direction = new Vec3(x, y, z).normalize();

            // 计算射线步数（根据方向调整）
            double scaleX = Math.abs(direction.x) * radius;
            double scaleY = Math.abs(direction.y) * radiusY;
            double scaleZ = Math.abs(direction.z) * radius;
            int steps = (int) Math.sqrt(scaleX * scaleX + scaleY * scaleY + scaleZ * scaleZ);

            bresenhamRays.add(new BresenhamRay(direction, steps, totalPower));
        }
    }

    public void tick() {
        if (!raysComplete) {
            processRaysOptimized(300); // 每tick处理300条射线，加快爆炸速度
        }

        if (raysComplete && !damageComplete) {
            applyDamage();
            damageComplete = true;
        }

        if (damageComplete && !naturalBlocksCleanupComplete) {
            cleanupNaturalBlocksHBM(150);
        }

        if (naturalBlocksCleanupComplete && !radiationComplete) {
            processRadiationHBM(150);
        }

        flushBlockUpdates();
    }

    /**
     * 使用八叉树加速的射线处理
     */
    private void processRaysOptimized(int raysPerTick) {
        int processed = 0;

        while (currentRay < rayCount && processed < raysPerTick) {
            BresenhamRay ray = bresenhamRays.get(currentRay);
            traceBresenhamRay(ray);

            currentRay++;
            processed++;
        }

        if (currentRay >= rayCount) {
            raysComplete = true;
        }
    }

    /**
     * Bresenham 3D 射线追踪
     * 修复：增强穿透力，确保爆炸范围完整
 */
    private void traceBresenhamRay(BresenhamRay ray) {
        float remainingPower = ray.power;

        int step = 0;
        while (step < ray.steps && remainingPower > 0) {
            float x = (float) (center.x + ray.direction.x * step);
            float y = (float) (center.y + ray.direction.y * step);
            float z = (float) (center.z + ray.direction.z * step);

            BlockPos pos = BlockPos.containing(x, y, z);

            // 跳过已摧毁的方块
            if (destroyedBlocks.contains(pos)) {
                step++;
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                Block block = state.getBlock();

                // 不可破坏方块（基岩等）
            if (block.defaultDestroyTime() < 0) {
                    remainingPower = 0;
                    break;
                }

                float hardness = block.defaultDestroyTime();
                boolean isNatural = isNaturalBlock(state);

                // 距离因子：越近威力越大
            double distanceFactor = 1.0 - ((double) step / ray.steps);
                
                // 计算抵抗值
            float resistance = hardness;
                if (isNatural) {
                    resistance *= 0.1F; // 自然方块更容易破坏
            } else if (!state.isSolidRender(level, pos)) {
                    resistance *= 0.2F; // 非固体方块也容易破坏
                }

                // 衰减计算：距离越近，衰减越小
                float attenuation = resistance * 0.15F * (0.5F + (1.0F - (float) distanceFactor));
                remainingPower -= Math.max(attenuation, 0.02F);

                // 只要有剩余威力就摧毁方块
                if (remainingPower > 0) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    destroyedBlocks.add(pos);
                    modifiedBlocks.add(pos.immutable());
                }
            }

            step++;
        }
    }

    /**
     * 八叉树查询结果
 */
    private enum OctreeQueryResult {
        EMPTY,      // 全是空气
        SOLID,      // 全是固体
        MIXED       // 混合状态，需要详细检查
}

    /**
     * 查询八叉树
 */
    @SuppressWarnings("unused")
    private OctreeQueryResult queryOctree(BlockPos pos) {
        return queryOctreeNode(octreeRoot, pos);
    }

    private OctreeQueryResult queryOctreeNode(OctreeNode node, BlockPos pos) {
        if (!node.contains(pos)) {
            return null;
        }

        if (node.isLeaf) {
            if (node.isEmpty) return OctreeQueryResult.EMPTY;
            if (node.isSolid) return OctreeQueryResult.SOLID;
            return OctreeQueryResult.MIXED;
        }

        // 递归查询子节点
    for (OctreeNode child : node.children) {
            if (child != null && child.contains(pos)) {
                return queryOctreeNode(child, pos);
            }
        }

        return OctreeQueryResult.MIXED;
    }

    /**
     * 检查是否是自然类方块
 */
    private boolean isNaturalBlock(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof SnowLayerBlock || block == Blocks.SNOW_BLOCK) return true;
        if (block instanceof LeavesBlock || state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS) ||
            state.is(BlockTags.TALL_FLOWERS) || block == Blocks.SHORT_GRASS ||
            block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.LARGE_FERN) return true;
        if (block == Blocks.VINE) return true;
        if (state.is(BlockTags.CROPS)) return true;
        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM ||
            block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK) return true;
        if (block == Blocks.CACTUS || block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO) return true;
        if (block == Blocks.DEAD_BUSH || block == Blocks.LILY_PAD ||
            block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS ||
            block == Blocks.KELP || block == Blocks.KELP_PLANT) return true;
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return true;

        return false;
    }

    /**
     * 应用实体伤害
     */
    private void applyDamage() {
        net.minecraft.world.damagesource.DamageSources damageSources = level.damageSources();
        for (Entity entity : entities) {
            Vec3 entityPos = entity.position();
            double distance = center.distanceTo(entityPos);

            if (distance <= Math.max(radius, radiusY)) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                    entity.discard();
                    continue;
                }

                float distanceFactor = (float) Math.pow(1.0 - (distance / Math.max(radius, radiusY)), 2);
                float entityDamage = totalPower * distanceFactor * 5.0F;

                if (entityDamage > 1.0F) {
                    if (entity instanceof LivingEntity living) {
                        living.hurt(damageSources.explosion(null, null), entityDamage);
                    } else {
                        entity.hurt(damageSources.explosion(null, null), entityDamage);
                    }
                }
            }
        }
    }

    /**
     * HBM风格的自然方块清理
 */
    private void cleanupNaturalBlocksHBM(int positionsPerTick) {
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;
        int radiusSq = radius * radius;

        int processed = 0;

        while (naturalCleanupN <= naturalCleanupNLimit && processed < positionsPerTick) {
            naturalCleanupShell = (int) Math.floor((Math.sqrt(naturalCleanupN) + 1) / 2);
            int shell2 = naturalCleanupShell * 2;
            naturalCleanupLeg = (int) Math.floor((naturalCleanupN - (shell2 - 1) * (shell2 - 1)) / shell2);
            naturalCleanupElement = (naturalCleanupN - (shell2 - 1) * (shell2 - 1)) - shell2 * naturalCleanupLeg - naturalCleanupShell + 1;

            naturalCleanupLastX = naturalCleanupLeg == 0 ? naturalCleanupShell :
                                  naturalCleanupLeg == 1 ? -naturalCleanupElement :
                                  naturalCleanupLeg == 2 ? -naturalCleanupShell : naturalCleanupElement;
            naturalCleanupLastZ = naturalCleanupLeg == 0 ? naturalCleanupElement :
                                  naturalCleanupLeg == 1 ? naturalCleanupShell :
                                  naturalCleanupLeg == 2 ? -naturalCleanupElement : -naturalCleanupShell;

            cleanupNaturalColumn(centerX + naturalCleanupLastX, centerY, centerZ + naturalCleanupLastZ, radiusSq);

            naturalCleanupN++;
            processed++;
        }

        if (naturalCleanupN > naturalCleanupNLimit) {
            naturalBlocksCleanupComplete = true;
        }
    }

    private void cleanupNaturalColumn(int x, int yCenter, int z, int radiusSq) {
        int distSqXZ = naturalCleanupLastX * naturalCleanupLastX + naturalCleanupLastZ * naturalCleanupLastZ;
        int distY = radiusSq - distSqXZ;

        if (distY <= 0) return;

        int yRange = (int) Math.sqrt(distY);

        for (int y = yRange; y >= -yRange; y--) {
            BlockPos pos = new BlockPos(x, yCenter + y, z);

            if (destroyedBlocks.contains(pos)) continue;

            BlockState state = level.getBlockState(pos);

            if (isNaturalBlock(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                destroyedBlocks.add(pos);
                modifiedBlocks.add(pos.immutable());
            }
        }
    }

    /**
     * HBM风格的辐射区处理
     */
    private void processRadiationHBM(int positionsPerTick) {
        RandomSource rand = level.random;
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;
        int radiusSq = radius * radius;

        int processed = 0;

        while (radiationProgressN <= radiationProgressNLimit && processed < positionsPerTick) {
            radiationShell = (int) Math.floor((Math.sqrt(radiationProgressN) + 1) / 2);
            int shell2 = radiationShell * 2;
            radiationLeg = (int) Math.floor((radiationProgressN - (shell2 - 1) * (shell2 - 1)) / shell2);
            radiationElement = (radiationProgressN - (shell2 - 1) * (shell2 - 1)) - shell2 * radiationLeg - radiationShell + 1;

            radiationLastX = radiationLeg == 0 ? radiationShell :
                             radiationLeg == 1 ? -radiationElement :
                             radiationLeg == 2 ? -radiationShell : radiationElement;
            radiationLastZ = radiationLeg == 0 ? radiationElement :
                             radiationLeg == 1 ? radiationShell :
                             radiationLeg == 2 ? -radiationElement : -radiationShell;

            processRadiationColumn(centerX + radiationLastX, centerY, centerZ + radiationLastZ, radiusSq, rand);

            radiationProgressN++;
            processed++;
        }

        if (radiationProgressN > radiationProgressNLimit) {
            radiationComplete = true;
        }
    }

    private void processRadiationColumn(int x, int yCenter, int z, int radiusSq, RandomSource rand) {
        int distSqXZ = radiationLastX * radiationLastX + radiationLastZ * radiationLastZ;
        int distY = radiusSq - distSqXZ;

        if (distY <= 0) return;

        int yRange = (int) Math.sqrt(distY);

        for (int y = yRange; y >= -yRange; y--) {
            BlockPos pos = new BlockPos(x, yCenter + y, z);

            if (destroyedBlocks.contains(pos)) continue;

            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            // 转换方块为辐射版本
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
                if (rand.nextFloat() < 0.3f) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_STONE.get().defaultBlockState(), 3);
                    modifiedBlocks.add(pos.immutable());
                }
            } else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                if (rand.nextFloat() < 0.5f) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DIRT.get().defaultBlockState(), 3);
                    modifiedBlocks.add(pos.immutable());
                }
            } else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
                if (rand.nextFloat() < 0.25f) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get().defaultBlockState(), 3);
                    modifiedBlocks.add(pos.immutable());
                }
            }
        }
    }

    private void flushBlockUpdates() {
        for (BlockPos pos : modifiedBlocks) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        modifiedBlocks.clear();
    }

    public boolean isComplete() {
        return raysComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }
}