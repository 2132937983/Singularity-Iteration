package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 核弹爆炸任务 - 高性能优化版本
 * 使用 FastUtil 和批量处理优化性能
 */
@SuppressWarnings("null")
public class NukeExplosionTask {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower;
    private final int radius;
    private final List<Entity> entities;
    // 使用 FastUtil 的Long2ObjectOpenHashMap 替代 HashSet，性能提升 2-3 倍
private final Long2ObjectMap<Object> destroyedBlocks = new Long2ObjectOpenHashMap<>();
    private final List<BlockPos> modifiedBlocks = new ArrayList<>();
    // 批量处理缓冲区
private final List<BlockPos> blockDestroyBuffer = new ArrayList<>(1000);

    // 辐射区相同
private final int radiationRadius;
    private int radiationProgressN = 1;
    private int radiationProgressNLimit;
    private int radiationShell;
    private int radiationLeg;
    private int radiationElement;
    private int radiationLastX = 0;
    private int radiationLastZ = 0;

    private int currentRay = 0;
    private int rayCount;
    private boolean raysComplete = false;
    private boolean damageComplete = false;
    private boolean radiationComplete = false;
    private boolean naturalBlocksCleanupComplete = false;
    private int naturalCleanupN = 1;
    private int naturalCleanupNLimit;
    private int naturalCleanupShell;
    private int naturalCleanupLeg;
    private int naturalCleanupElement;
    private int naturalCleanupLastX = 0;
    private int naturalCleanupLastZ = 0;

    // 每tick 处理数量 - 大幅提升
    private static final int RAYS_PER_TICK = 500;
    private static final int POSITIONS_PER_TICK = 500;
    private static final int BATCH_SIZE = 1000;

    // 预计算的斐波那契球面方向
    private Vec3[] precomputedDirections;

    public NukeExplosionTask(ServerLevel level, Vec3 center, float totalPower, int radius, List<Entity> entities) {
        this.level = level;
        this.center = center;
        this.totalPower = totalPower;
        this.radius = radius;
        this.entities = entities;

        // 预计算射线方向
    this.rayCount = Math.min((int) (4 * Math.PI * radius * radius / 4), 20000);
        this.rayCount = Math.max(rayCount, 500);
        precomputeDirections();

        // 辐射区半径 = 爆炸半径 * 1.5（比爆炸范围大50%）
    this.radiationRadius = (int) (radius * 1.5);
        // 辐射区搜索范围扩大到辐射半径的平方
    int radiationRadiusSq = this.radiationRadius * this.radiationRadius;
        this.naturalCleanupNLimit = radius * radius * 4;
        this.radiationProgressNLimit = radiationRadiusSq * 4;
    }

    /**
     * 预计算斐波那契球面方向，避免每tick 重复计算
     */
    private void precomputeDirections() {
        precomputedDirections = new Vec3[rayCount];
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < rayCount; i++) {
            double y = 1.0 - (i / (double) (rayCount - 1)) * 2.0;
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double theta = phi * i;
            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;
            precomputedDirections[i] = new Vec3(x, y, z).normalize();
        }
    }

    public void tick() {
        if (!raysComplete) {
            processRays(RAYS_PER_TICK);
        }

        if (raysComplete && !damageComplete) {
            applyDamage();
            damageComplete = true;
        }

        if (damageComplete && !naturalBlocksCleanupComplete) {
            cleanupNaturalBlocksHBM(POSITIONS_PER_TICK);
        }

        if (naturalBlocksCleanupComplete && !radiationComplete) {
            processRadiationHBM(POSITIONS_PER_TICK);
        }

        // 批量刷新方块更新
        flushBlockUpdates();
    }

    /**
     * 批量发送方块更新到客户端
     */
    private void flushBlockUpdates() {
        if (modifiedBlocks.isEmpty()) return;
        for (BlockPos pos : modifiedBlocks) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        modifiedBlocks.clear();
    }

    /**
     * 批量摧毁方块，减少setBlock 调用次数
     */
    private void flushBlockDestroyBuffer() {
        if (blockDestroyBuffer.isEmpty()) return;
        for (BlockPos pos : blockDestroyBuffer) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        blockDestroyBuffer.clear();
    }

    private void processRays(int raysPerTick) {
        int processed = 0;
        while (currentRay < rayCount && processed < raysPerTick) {
            traceRayAndDestroy(precomputedDirections[currentRay]);
            currentRay++;
            processed++;
        }
        flushBlockDestroyBuffer();
        if (currentRay >= rayCount) {
            raysComplete = true;
        }
    }

    /**
     * 完全阻挡爆炸的方块- 这些方块会完全吸收爆炸射线，无论威力多大
     */
    private boolean isExplosionProofBlock(Block block, BlockState state) {
        // 黑曜石类方块 - 完全防爆
        if (block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.RESPAWN_ANCHOR ||
            block == Blocks.ENCHANTING_TABLE) {
            return true;
        }
        // 不可破坏方块
        float hardness = block.defaultDestroyTime();
        if (hardness < 0) {
            return true;
        }
        return false;
    }

    private void traceRayAndDestroy(Vec3 direction) {
        float remainingPower = totalPower;
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;

        // 使用 MutableBlockPos 减少对象创建
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < radius && remainingPower > 0; i++) {
            int x = centerX + (int) (direction.x * i);
            int y = centerY + (int) (direction.y * i);
            int z = centerZ + (int) (direction.z * i);
            mutablePos.set(x, y, z);

            long posKey = mutablePos.asLong();
            if (destroyedBlocks.containsKey(posKey)) {
                continue;
            }

            BlockState blockState = level.getBlockState(mutablePos);
            if (blockState.isAir()) {
                continue;
            }

            Block block = blockState.getBlock();
            
            // 防爆方块完全吸收爆炸射线，无论威力多大
        if (isExplosionProofBlock(block, blockState)) {
                remainingPower = 0;
                break;
            }

            float hardness = block.defaultDestroyTime();
            double distanceFactor = 1.0 - ((double) i / radius);
            float resistance = hardness;

            if (isNaturalBlockFast(block, blockState)) {
                resistance *= 0.05F;
            } else if (!blockState.isSolidRender(level, mutablePos)) {
                resistance *= 0.1F;
            }
            
            // 高硬度方块（如下界合金、远古残骸）增加衰减
            if (hardness >= 30.0f) {
                resistance *= 5.0f;
            } else if (hardness >= 10.0f) {
                resistance *= 3.0f;
            } else if (hardness >= 5.0f) {
                resistance *= 2.0f;
            }

            float attenuation = resistance * 0.3F * (1.0F + (1.0F - (float) distanceFactor) * 2.0F);
            remainingPower -= Math.max(attenuation, 0.05F);

            if (remainingPower > 0) {
 // 添加到批量缓冲区
                blockDestroyBuffer.add(mutablePos.immutable());
                destroyedBlocks.put(posKey, Boolean.TRUE);
                modifiedBlocks.add(mutablePos.immutable());

 // 当缓冲区满时批量处理
                if (blockDestroyBuffer.size() >= BATCH_SIZE) {
                    flushBlockDestroyBuffer();
                }
            }
        }
    }

    /**
     * 快速自然方块检查- 使用更高效的判断逻辑
     */
    private boolean isNaturalBlockFast(Block block, BlockState state) {
        // 快速路径：直接检查最常见的自然方块
    if (block instanceof LeavesBlock || block instanceof SnowLayerBlock) {
            return true;
        }
        if (block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS ||
            block == Blocks.FERN || block == Blocks.VINE ||
            block == Blocks.DEAD_BUSH || block == Blocks.LILY_PAD) {
            return true;
        }
        // 检查标签
    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) ||
            state.is(BlockTags.CROPS)) {
            return true;
        }
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE) {
            return true;
        }
        return false;
    }

    private void applyDamage() {
        net.minecraft.world.damagesource.DamageSources damageSources = level.damageSources();
        double radiusSq = radius * radius;

        for (Entity entity : entities) {
            Vec3 entityPos = entity.position();
            double distanceSq = center.distanceToSqr(entityPos);

            if (distanceSq <= radiusSq) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                    entity.discard();
                    continue;
                }

                double distanceFactor = 1.0 - Math.sqrt(distanceSq) / radius;
                float entityDamage = totalPower * (float) (distanceFactor * distanceFactor) * 5.0F;

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

    public boolean isComplete() {
        return raysComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }

    // ==================== 自然方块清理 (优化版本) ====================

    private void cleanupNaturalBlocksHBM(int positionsPerTick) {
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;
        int radiusSq = radius * radius;
        int processed = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        while (naturalCleanupN <= naturalCleanupNLimit && processed < positionsPerTick) {
            // HBM 螺旋壳层算法
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

            cleanupNaturalColumnFast(centerX + naturalCleanupLastX, centerY, centerZ + naturalCleanupLastZ,
                    radiusSq, mutablePos);

            naturalCleanupN++;
            processed++;
        }

        flushBlockDestroyBuffer();
        if (naturalCleanupN > naturalCleanupNLimit) {
            naturalBlocksCleanupComplete = true;
        }
    }

    private void cleanupNaturalColumnFast(int x, int yCenter, int z, int radiusSq, BlockPos.MutableBlockPos mutablePos) {
        int distSqXZ = naturalCleanupLastX * naturalCleanupLastX + naturalCleanupLastZ * naturalCleanupLastZ;
        int distY = radiusSq - distSqXZ;
        if (distY <= 0) return;

        int yRange = (int) Math.sqrt(distY);
        for (int y = yRange; y >= -yRange; y--) {
            mutablePos.set(x, yCenter + y, z);
            long posKey = mutablePos.asLong();

            if (destroyedBlocks.containsKey(posKey)) {
                continue;
            }

            BlockState state = level.getBlockState(mutablePos);
            if (isNaturalBlockFast(state.getBlock(), state)) {
                blockDestroyBuffer.add(mutablePos.immutable());
                destroyedBlocks.put(posKey, Boolean.TRUE);
                modifiedBlocks.add(mutablePos.immutable());

                if (blockDestroyBuffer.size() >= BATCH_SIZE) {
                    flushBlockDestroyBuffer();
                }
            }
        }
    }

    // ==================== 辐射区处理(优化版本) ====================

    private void processRadiationHBM(int positionsPerTick) {
        RandomSource rand = level.random;
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;
        int radiusSq = radius * radius;
        int processed = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

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

            processRadiationColumnFast(centerX + radiationLastX, centerY, centerZ + radiationLastZ,
                    radiusSq, rand, mutablePos);

            radiationProgressN++;
            processed++;
        }

        flushBlockDestroyBuffer();
        if (radiationProgressN > radiationProgressNLimit) {
            radiationComplete = true;
        }
    }

    private void processRadiationColumnFast(int x, int yCenter, int z, int radiusSq, RandomSource rand,
                                            BlockPos.MutableBlockPos mutablePos) {
        int distSqXZ = radiationLastX * radiationLastX + radiationLastZ * radiationLastZ;
        int distY = radiusSq - distSqXZ;
        if (distY <= 0) return;

        int yRange = (int) Math.sqrt(distY);
        int edgeRandomness = Math.max(1, radius / 4);
        int edgeRandomnessSq = edgeRandomness * edgeRandomness;

        for (int y = yRange; y >= -yRange; y--) {
            mutablePos.set(x, yCenter + y, z);
            long posKey = mutablePos.asLong();

            if (destroyedBlocks.containsKey(posKey)) {
                continue;
            }

            int distSq = distSqXZ + y * y;
            if (distSq < radiusSq - rand.nextInt(edgeRandomnessSq)) {
                applyRadiationEffectFast(mutablePos, distSq, radiusSq, rand);
            }
        }
    }

    private void applyRadiationEffectFast(BlockPos.MutableBlockPos mutablePos, int distSq, int maxDistSq, RandomSource rand) {
        BlockState state = level.getBlockState(mutablePos);
        Block block = state.getBlock();

        // 在辐射区内，树叶等软方块直接被摧毁
    if (isNaturalBlockFast(block, state)) {
            blockDestroyBuffer.add(mutablePos.immutable());
            destroyedBlocks.put(mutablePos.asLong(), Boolean.TRUE);
            modifiedBlocks.add(mutablePos.immutable());

            if (blockDestroyBuffer.size() >= BATCH_SIZE) {
                flushBlockDestroyBuffer();
            }
            return;
        }

        Block radioactiveBlock = getRadioactiveBlock(block);
        if (radioactiveBlock == null) {
            return;
        }

        double distanceFactor = 1.0 - (double) distSq / maxDistSq;
        int conversionChance = (int) (distanceFactor * 100);
        if (rand.nextInt(100) < conversionChance) {
            level.setBlock(mutablePos, radioactiveBlock.defaultBlockState(), 2);
            modifiedBlocks.add(mutablePos.immutable());
        }
    }

    private Block getRadioactiveBlock(Block block) {
        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            return mio_icif_blocks.BLOCK_RADIATING_DIRT.get();
        } else if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
            return mio_icif_blocks.BLOCK_RADIATING_STONE.get();
        } else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
            return mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get();
        }
        return null;
    }
}