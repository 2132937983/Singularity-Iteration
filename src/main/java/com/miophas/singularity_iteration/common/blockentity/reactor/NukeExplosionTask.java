package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.runtime.reactor.BlastInput;
import com.miophas.singularity_iteration.core.runtime.reactor.SpiralBlastCursor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

/**
 * Resumable nuclear blast: every advance owns at most one position or entity.
 * Direct scans require loaded chunks; original block-update flags are retained,
 * so vanilla and third-party neighbour callbacks remain outside this scan budget.
 */
@SuppressWarnings("null")
public class NukeExplosionTask {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower;
    private final int radius;
    private Iterator<Entity> entities;
    // Only membership is needed across the ray, natural and radiation phases.
    // A primitive set avoids retaining one boxed Boolean per visited position.
    private final LongSet destroyedBlocks = new LongOpenHashSet();
    private BlockPos destroyPending, updatePending;
    private boolean advancing;

    private final SpiralBlastCursor naturalScan, radiationScan;
    private int currentRay, rayStep;
    private final int rayCount;
    private float remainingRayPower;
    private Vec3 rayDirection;
    private boolean raysComplete, damageComplete, radiationComplete, naturalBlocksCleanupComplete;

    public NukeExplosionTask(ServerLevel level, Vec3 center, float totalPower, int radius, List<Entity> entities) {
        BlastInput.nuke(center.x, center.y, center.z, totalPower, radius);
        this.level = level;
        this.center = center;
        this.totalPower = totalPower;
        this.radius = radius;
        this.entities = entities.iterator();

        // Directions are derived only when their ray starts.
        this.rayCount = Math.max(500, Math.min((int) (4 * Math.PI * radius * radius / 4), 20000));

        // 辐射区半径 = 爆炸半径 * 1.5（比爆炸范围大50%）
        int radiationRadius = (int) (radius * 1.5);
        // 辐射区搜索范围扩大到辐射半径的平方
        this.naturalScan = new SpiralBlastCursor(radius, radius);
        this.radiationScan = new SpiralBlastCursor(radiationRadius, radius);
    }

    /** Compatibility entry point; the shared server scheduler calls advance directly. */
    public void tick() {
        for (int steps = 0; steps < 500 && !isComplete(); steps++) advance();
    }

    /** At most one ray sample, entity or column position, followed by its bounded updates. */
    public boolean advance() {
        if (advancing) return false;
        advancing = true;
        try {
            if (!raysComplete) advanceRay();
            else if (!damageComplete) advanceDamage();
            else if (!naturalBlocksCleanupComplete) advanceNaturalColumn();
            else if (!radiationComplete) advanceRadiationColumn();
            flushBlockDestroyBuffer();
            flushBlockUpdates();
            return isComplete();
        } finally {
            advancing = false;
        }
    }

    private void advanceRay() {
        if (currentRay >= rayCount) { raysComplete = true; return; }
        if (rayStep == 0) {
            remainingRayPower = totalPower;
            double y = 1.0 - (currentRay / (double) (rayCount - 1)) * 2.0;
            double around = Math.sqrt(1.0 - y * y);
            double theta = Math.PI * (3.0 - Math.sqrt(5.0)) * currentRay;
            rayDirection = new Vec3(Math.cos(theta) * around, y, Math.sin(theta) * around).normalize();
        }
        if (rayStep >= radius || remainingRayPower <= 0) { currentRay++; rayStep = 0; return; }
        int distance = rayStep++;
        BlockPos pos = new BlockPos((int) center.x + (int) (rayDirection.x * distance),
                (int) center.y + (int) (rayDirection.y * distance),
                (int) center.z + (int) (rayDirection.z * distance));
        if (destroyedBlocks.contains(pos.asLong())) return;
        var chunk = loadedChunk(pos);
        if (chunk == null) return;
        BlockState state = chunk.getBlockState(pos);
        if (state.isAir()) return;
        Block block = state.getBlock();
        if (isExplosionProofBlock(block, state)) { remainingRayPower = 0; return; }
        float hardness = block.defaultDestroyTime();
        double distanceFactor = 1.0 - (double) distance / radius;
        float resistance = hardness;
        if (isNaturalBlockFast(block, state)) resistance *= 0.05F;
        else if (!state.isSolidRender(level, pos)) resistance *= 0.1F;
        if (hardness >= 30.0f) resistance *= 5.0f;
        else if (hardness >= 10.0f) resistance *= 3.0f;
        else if (hardness >= 5.0f) resistance *= 2.0f;
        remainingRayPower -= Math.max(resistance * 0.3F * (1.0F + (1.0F - (float) distanceFactor) * 2.0F), 0.05F);
        if (remainingRayPower > 0) {
            destroyPending = pos;
            destroyedBlocks.add(pos.asLong());
            updatePending = pos;
        }
    }

    private void advanceDamage() {
        if (!entities.hasNext()) {
            entities = java.util.Collections.emptyIterator();
            damageComplete = true;
            return;
        }
        Entity entity = entities.next();
        if (entity.isRemoved() || entity.level() != level || !isLoaded(entity.blockPosition())) return;
        double distanceSq = center.distanceToSqr(entity.position());
        if (distanceSq > (double) radius * radius) return;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) { entity.discard(); return; }
        double distanceFactor = 1.0 - Math.sqrt(distanceSq) / radius;
        float damage = totalPower * (float) (distanceFactor * distanceFactor) * 5.0F;
        if (damage > 1.0F) entity.hurt(level.damageSources().explosion(null, null), damage);
    }

    private void advanceNaturalColumn() {
        var step = naturalScan.next();
        if (step == SpiralBlastCursor.Step.DONE) { naturalBlocksCleanupComplete = true; return; }
        if (step != SpiralBlastCursor.Step.POSITION) return;
        BlockPos pos = new BlockPos((int) center.x + naturalScan.x(),
                (int) center.y + naturalScan.y(), (int) center.z + naturalScan.z());
        if (destroyedBlocks.contains(pos.asLong())) return;
        var chunk = loadedChunk(pos);
        if (chunk == null) return;
        BlockState state = chunk.getBlockState(pos);
        if (isNaturalBlockFast(state.getBlock(), state)) {
            destroyPending = pos;
            destroyedBlocks.add(pos.asLong());
            updatePending = pos;
        }
    }

    private void advanceRadiationColumn() {
        var step = radiationScan.next();
        if (step == SpiralBlastCursor.Step.DONE) { radiationComplete = true; return; }
        if (step != SpiralBlastCursor.Step.POSITION) return;
        var pos = new BlockPos.MutableBlockPos((int) center.x + radiationScan.x(),
                (int) center.y + radiationScan.y(), (int) center.z + radiationScan.z());
        if (destroyedBlocks.contains(pos.asLong())) return;
        var chunk = loadedChunk(pos);
        if (chunk == null) return;
        long distanceSquared = (long) radiationScan.x() * radiationScan.x()
                + (long) radiationScan.z() * radiationScan.z()
                + (long) radiationScan.y() * radiationScan.y();
        int edge = Math.max(1, radius / 4);
        long radiusSquared = (long) radius * radius;
        if (distanceSquared < radiusSquared - level.random.nextInt(edge * edge))
            applyRadiationEffectFast(pos, distanceSquared, radiusSquared, level.random, chunk.getBlockState(pos));
    }

    /** Publish only the position produced by the current bounded step. */
    private void flushBlockUpdates() {
        BlockPos pos = updatePending;
        updatePending = null;
        if (pos == null) return;
        var chunk = loadedChunk(pos);
        if (chunk == null) return;
        BlockState state = chunk.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    /** Apply only the position produced by the current bounded step. */
    private void flushBlockDestroyBuffer() {
        BlockPos pos = destroyPending;
        destroyPending = null;
        if (pos != null && loadedChunk(pos) != null) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private boolean isLoaded(BlockPos pos) {
        return loadedChunk(pos) != null;
    }

    /** Resolve the loaded chunk once per bounded step; never triggers a load. */
    private LevelChunk loadedChunk(BlockPos pos) {
        return level.isOutsideBuildHeight(pos) ? null
                : level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
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

    public boolean isComplete() {
        return raysComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }

    private void applyRadiationEffectFast(BlockPos.MutableBlockPos mutablePos, long distSq, long maxDistSq,
                                           RandomSource rand, BlockState state) {
        Block block = state.getBlock();

        // 在辐射区内，树叶等软方块直接被摧毁
        if (isNaturalBlockFast(block, state)) {
            destroyPending = mutablePos.immutable();
            destroyedBlocks.add(mutablePos.asLong());
            updatePending = mutablePos.immutable();

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
            updatePending = mutablePos.immutable();
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
