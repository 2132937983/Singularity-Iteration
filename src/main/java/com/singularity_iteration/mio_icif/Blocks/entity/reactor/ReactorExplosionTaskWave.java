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
 * 核反应堆爆炸任务 - Wave Propagation（波动传播）算法
 *
 * 特点：
 * - 爆炸以波的形式从中心向外传播
 * - 产生不规则的、撕裂状的爆炸坑（与核弹的球形不同）
 * - 考虑材料阻抗，不同方块有不同的传播速度
 * - 会产生放射状的沟壑和突起
 * - 爆炸边缘更加参差不齐
 */
@SuppressWarnings("null")
public class ReactorExplosionTaskWave {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower;
    private final int radius;
    private final int radiusY;
    private final List<Entity> entities;
    private final Set<BlockPos> destroyedBlocks = new HashSet<>();
    private final Set<BlockPos> modifiedBlocks = new HashSet<>();
    private final RandomSource random;

    // 波动传播参数
    private int currentWave = 0;           // 当前波前半径
    private final int maxWaveRadius;       // 最大波前半径
private final int radiationRadius;     // 辐射区半径（比爆炸半径大50%）
private boolean wavePropagationComplete = false;
    private boolean damageComplete = false;
    private boolean radiationComplete = false;

    // 每个方向的波动能量（用于产生不规则形状）
    private final Map<Integer, Float> waveEnergyByAngle = new HashMap<>();

    // 角度分割数（360度分成多少份）
private static final int ANGLE_SEGMENTS = 144; // 每2.5度一个方向，更密集的水平覆盖

    // HBM风格的辐射区处理参数
    private int radiationProgressN = 1;
    private int radiationProgressNLimit;
    private int radiationShell;
    private int radiationLeg;
    private int radiationElement;
    private int radiationLastX = 0;
    private int radiationLastZ = 0;

    // HBM风格的自然方块清理参数
private int naturalCleanupN = 1;
    private int naturalCleanupNLimit;
    private int naturalCleanupShell;
    private int naturalCleanupLeg;
    private int naturalCleanupElement;
    private int naturalCleanupLastX = 0;
    private int naturalCleanupLastZ = 0;
    private boolean naturalBlocksCleanupComplete = false;

    public ReactorExplosionTaskWave(ServerLevel level, Vec3 center, float totalPower, int radius, int radiusY, List<Entity> entities) {
        this.level = level;
        this.center = center;
        this.totalPower = totalPower;
        this.radius = radius;
        this.radiusY = radiusY;
        this.entities = entities;
        this.random = level.random;
        this.maxWaveRadius = Math.max(radius, radiusY);
        // 辐射区半径 = 爆炸半径 * 1.5（比爆炸范围大50%）
    this.radiationRadius = (int) (Math.max(radius, radiusY) * 1.5);

        // 初始化每个方向的波动能量（产生不规则形状）
    initializeWaveEnergy();

        // 初始化HBM风格处理参数
        // 辐射区使用辐射半径，自然清理使用爆炸半径
        int radiationRadiusSq = this.radiationRadius * this.radiationRadius;
        this.radiationProgressNLimit = radiationRadiusSq * 4;
        this.naturalCleanupNLimit = radius * radius * 4;
    }

    /**
     * 初始化每个方向的波动能量
     * 使用噪声函数产生不规则的爆炸形状
     */
    private void initializeWaveEnergy() {
        for (int i = 0; i < ANGLE_SEGMENTS; i++) {
            double angle = (i * 2.0 * Math.PI) / ANGLE_SEGMENTS;

            // 使用多层正弦波产生不规则形状
            double noise = Math.sin(angle * 3) * 0.3 +
                          Math.sin(angle * 7) * 0.2 +
                          Math.sin(angle * 13) * 0.1;

            // 添加随机扰动
            noise += (random.nextFloat() - 0.5) * 0.4;

            // 计算该方向的最大传播距离（0.85 - 1.15倍半径，更均匀）
        float energyFactor = (float) (1.0 + noise * 0.3);
            energyFactor = Math.max(0.85f, Math.min(1.15f, energyFactor));

            waveEnergyByAngle.put(i, energyFactor);
        }
    }

    public void tick() {
        if (!wavePropagationComplete) {
            processWavePropagation(100); // 每tick处理100层波前，加快爆炸速度
        }

        if (wavePropagationComplete && !damageComplete) {
            applyDamage();
            damageComplete = true;
        }

        if (damageComplete && !naturalBlocksCleanupComplete) {
            cleanupNaturalBlocksHBM(150); // HBM风格的自然方块清理
    }

        if (naturalBlocksCleanupComplete && !radiationComplete) {
            processRadiationHBM(150); // HBM风格的辐射区处理
        }

        flushBlockUpdates();
    }

    /**
     * 波动传播核心算法
     * 以波的形式从中心向外传播，每层波前处理一圈方块
 */
    private void processWavePropagation(int wavesPerTick) {
        int processed = 0;

        while (currentWave < maxWaveRadius && processed < wavesPerTick) {
            // 处理当前波前
            processWaveFront(currentWave);

            currentWave++;
            processed++;
        }

        if (currentWave >= maxWaveRadius) {
            wavePropagationComplete = true;
        }
    }

    /**
     * 处理单个波前
     * @param waveRadius 当前波前半径
     */
    private void processWaveFront(int waveRadius) {
        // 计算当前波的威力（随距离衰减）
    float wavePower = totalPower * (1.0f - (float) waveRadius / maxWaveRadius);
        if (wavePower <= 0) return;

        // 在水平面上按角度分割处理
        for (int angleIdx = 0; angleIdx < ANGLE_SEGMENTS; angleIdx++) {
            double baseAngle = (angleIdx * 2.0 * Math.PI) / ANGLE_SEGMENTS;
            float energyFactor = waveEnergyByAngle.getOrDefault(angleIdx, 1.0f);

            // 该方向的最大半径（基于初始半径和能量因子）
            int maxRadiusThisDirection = (int) (radius * energyFactor);
            if (waveRadius > maxRadiusThisDirection) continue; // 超过该方向最大半径则跳过

            // 在该角度方向上处理一列方块
        processDirectionalColumn(baseAngle, waveRadius, wavePower);
        }
    }

    /**
     * 处理特定方向上的一列方块
 * 射线"粗细"，处理3x3区域而不仅是中心点
 * 添加阻挡机制：高硬度方块会衰减爆炸威力
 * @param angle 水平角度
     * @param distance 距离
     * @param power 当前波威力
 */
    private void processDirectionalColumn(double angle, int distance, float power) {
        // 计算水平方向
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;

        // XZ平面位置（中心点）
    int centerXPos = centerX + (int) (cosAngle * distance);
        int centerZPos = centerZ + (int) (sinAngle * distance);

        // Y轴范围（椭球形）- 确保边缘也有足够的垂直范围
    double normalizedDist = (double) distance / radius;
        int yRange;
        if (normalizedDist >= 1.0) {
            yRange = 2; // 边缘至少处理2格高）
    } else {
            yRange = (int) (radiusY * Math.sqrt(1.0 - normalizedDist * normalizedDist));
            yRange = Math.max(yRange, 2); // 至少2格
    }

        // 首先检查该方向是否被防爆方块完全阻挡（从中心向外检查）
        boolean isBlockedByExplosionProof = false;
        for (int checkDist = 1; checkDist <= distance; checkDist++) {
            int checkX = centerX + (int) (cosAngle * checkDist);
            int checkZ = centerZ + (int) (sinAngle * checkDist);
            for (int checkY = centerY - 2; checkY <= centerY + 2; checkY++) {
                BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);
                BlockState checkState = level.getBlockState(checkPos);
                Block checkBlock = checkState.getBlock();
                if (isExplosionProofBlock(checkBlock, checkState)) {
                    isBlockedByExplosionProof = true;
                    break;
                }
            }
            if (isBlockedByExplosionProof) break;
        }

        // 如果该方向被防爆方块阻挡，跳过整个3x3区域
        if (isBlockedByExplosionProof) {
            return;
        }

        // 射线"粗细"：处理3x3水平区域
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                int x = centerXPos + xOffset;
                int z = centerZPos + zOffset;

                // 在该XZ位置处理Y轴方向的方块（从外向内处理，检测阻挡）
                // 使用射线追踪方式，遇到高硬度方块会衰减威力
            float remainingPower = power;

                for (int yOffset = -yRange; yOffset <= yRange; yOffset++) {
                    // 添加垂直方向的随机扰动，产生撕裂效果
                    int yNoise = (int) ((random.nextFloat() - 0.5) * yRange * 0.3);
                    int y = centerY + yOffset + yNoise;

                    BlockPos pos = new BlockPos(x, y, z);

                    // 跳过已处理的方块
                    if (destroyedBlocks.contains(pos)) continue;

                    // 距离因子 - 使用椭球体距离计算
                double dx = (x - center.x) / radius;
                    double dy = (y - center.y) / radiusY;
                    double dz = (z - center.z) / radius;
                    double normalizedDistPos = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double distanceFactor = 1.0 - normalizedDistPos;
                    if (distanceFactor <= 0) continue;

                    // 检查方块
                BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    if (isExplosionProofBlock(block, state)) {
                        // 防爆方块（黑曜石、基岩等）完全阻挡爆炸
                    // 完全阻断这条射线的传播
                    remainingPower = 0;
                        break; // 完全停止该方向射线的传播
                    }

                    // 计算破坏概率
                    float hardness = block.defaultDestroyTime();
                    boolean isNatural = isNaturalBlock(state);

                    // 材料阻抗计算
                    float resistance = hardness;
                    if (isNatural) {
                        resistance *= 0.01f; // 自然方块几乎无抵抗
                } else if (!state.isSolidRender(level, pos)) {
                        resistance *= 0.02f; // 非固体方块几乎无抵抗
                    } else {
                        resistance *= 0.1f; // 固体方块大幅降低抵抗
                    }

                    // 破坏概率 = 威力 * 距离因子 * 100 / (抵抗 + 0.01)
                    float destroyChance = (float) (remainingPower * distanceFactor * 100.0f / (resistance + 0.01f));

                    // 添加随机性，产生参差不齐的边缘
                destroyChance *= (0.5f + random.nextFloat() * 1.0f);

                    // 极低的破坏阈值，确保几乎所有方块都被破坏
                if (destroyChance > 0.01f) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        destroyedBlocks.add(pos);
                        modifiedBlocks.add(pos.immutable());

                        // 方块被破坏后，根据硬度衰减剩余威力
                    // 越硬的方块衰减越多
                    float powerLoss = Math.min(0.5f, hardness / 100.0f); // 最高衰减50%
                        remainingPower *= (1.0f - powerLoss);

                        // 如果威力耗尽，停止这条射线的传播
                        if (remainingPower < 0.1f) {
                            break;
                        }
                    } else {
                        // 方块未被破坏，根据硬度衰减威力
                    float powerLoss = Math.min(0.7f, hardness / 50.0f); // 未破坏的方块衰减更多
                        remainingPower *= (1.0f - powerLoss);

                        // 如果威力耗尽，停止这条射线的传播
                        if (remainingPower < 0.1f) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否完全防爆（如黑曜石、基岩等）
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
 * 清理爆炸范围内的自然方块（树叶、草等）
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
        int centerX = (int) center.x;
        int centerZ = (int) center.z;

        // 预检查：从中心到当前位置是否被防爆方块阻挡
        int distXZ = (int) Math.sqrt(distSqXZ);
        if (distXZ > 0) {
            double dirX = naturalCleanupLastX / (double) distXZ;
            double dirZ = naturalCleanupLastZ / (double) distXZ;
            for (int checkDist = 1; checkDist <= distXZ; checkDist++) {
                int checkX = centerX + (int) (dirX * checkDist);
                int checkZ = centerZ + (int) (dirZ * checkDist);
                for (int checkY = yCenter - 2; checkY <= yCenter + 2; checkY++) {
                    BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);
                    BlockState checkState = level.getBlockState(checkPos);
                    Block checkBlock = checkState.getBlock();
                    if (isExplosionProofBlock(checkBlock, checkState)) {
                        return; // 被防爆方块阻挡，跳过整个方向
                    }
                }
            }
        }

        for (int y = yRange; y >= -yRange; y--) {
            BlockPos pos = new BlockPos(x, yCenter + y, z);

            // 检查是否被防爆方块阻挡
            BlockState checkState = level.getBlockState(pos);
            Block checkBlock = checkState.getBlock();
            if (isExplosionProofBlock(checkBlock, checkState)) {
                // 遇到防爆方块，停止该方向的清理
            break;
            }

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
     * 使用螺旋扫描模式处理辐射方块转换
     */
    private void processRadiationHBM(int positionsPerTick) {
        int centerX = (int) center.x;
        int centerY = (int) center.y;
        int centerZ = (int) center.z;
        // 使用辐射半径（比爆炸半径大50%）
    int radiusSq = radiationRadius * radiationRadius;

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

            processRadiationColumn(centerX + radiationLastX, centerY, centerZ + radiationLastZ, radiusSq);

            radiationProgressN++;
            processed++;
        }

        if (radiationProgressN > radiationProgressNLimit) {
            radiationComplete = true;
        }
    }

    private void processRadiationColumn(int x, int yCenter, int z, int radiusSq) {
        int distSqXZ = radiationLastX * radiationLastX + radiationLastZ * radiationLastZ;
        int distY = radiusSq - distSqXZ;

        if (distY <= 0) return;

        int yRange = (int) Math.sqrt(distY);
        int centerX = (int) center.x;
        int centerZ = (int) center.z;

        // 预检查：从中心到当前位置是否被防爆方块阻挡
    int distXZ = (int) Math.sqrt(distSqXZ);
        if (distXZ > 0) {
            double dirX = radiationLastX / (double) distXZ;
            double dirZ = radiationLastZ / (double) distXZ;
            for (int checkDist = 1; checkDist <= distXZ; checkDist++) {
                int checkX = centerX + (int) (dirX * checkDist);
                int checkZ = centerZ + (int) (dirZ * checkDist);
                for (int checkY = yCenter - 2; checkY <= yCenter + 2; checkY++) {
                    BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);
                    BlockState checkState = level.getBlockState(checkPos);
                    Block checkBlock = checkState.getBlock();
                    if (isExplosionProofBlock(checkBlock, checkState)) {
                        return; // 被防爆方块阻挡，跳过整个方向
                    }
                }
            }
        }

        for (int y = yRange; y >= -yRange; y--) {
            BlockPos pos = new BlockPos(x, yCenter + y, z);

            // 检查是否被防爆方块阻挡
            BlockState checkState = level.getBlockState(pos);
            Block checkBlock = checkState.getBlock();
            if (isExplosionProofBlock(checkBlock, checkState)) {
                // 遇到防爆方块，停止该方向的辐射转换
            break;
            }

            if (destroyedBlocks.contains(pos)) continue;

            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            // 计算距离因子 - 使用正态分布风格的高斯衰减
            double distFromCenter = Math.sqrt(distSqXZ + y * y);
            // 使用辐射半径进行归一化，使得辐射区外圈（爆炸半径~辐射半径）仍有效果
        double normalizedDist = distFromCenter / Math.sqrt(radiusSq); // 0.0 ~ 1.0（基于辐射半径）
            
            // 高斯衰减：中心衰减率最高，边缘衰减较慢
            // 使用 exp(-x^2 / (2*sigma^2)) 形式，sigma=1.0 使得衰减更平缓
        double gaussianFactor = Math.exp(-(normalizedDist * normalizedDist) / 2.0);
            
            // 基础概率 - 大幅提高
            float baseChance;
            if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
                baseChance = 0.95f;
            } else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                baseChance = 0.98f; // 草方块/泥土几乎必定被辐射
        } else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
                baseChance = 0.9f;
            } else {
                continue;
            }

            // 根据高斯衰减调整概率
            // 中心区域：接近100%，边缘：仍有较高概率
            float convertChance = baseChance * (float) gaussianFactor;

            if (random.nextFloat() < convertChance) {
                if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_STONE.get().defaultBlockState(), 3);
                } else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DIRT.get().defaultBlockState(), 3);
                } else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
                    level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get().defaultBlockState(), 3);
                }
                modifiedBlocks.add(pos.immutable());
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
        return wavePropagationComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }
}