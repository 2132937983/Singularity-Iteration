package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.runtime.reactor.SpiralBlastCursor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Resumable version of the existing ellipsoidal ray explosion.
 * The historical class name is retained; its active ray path never queried an octree.
 * Direct scans and entity selection only visit loaded chunks. Original flags-3
 * physics are retained: vanilla neighbour updates and third-party callbacks may
 * do additional work or load other chunks, outside this task's scan budget.
 */
@SuppressWarnings("null")
public class NukeExplosionTaskOctree {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower;
    private final int radius;
    private final int radiusY;
    private final int rayCount;
    // BlockPos objects are transient scan values; retain only packed identity
    // keys so large resumable blasts do not pin an object graph per block.
    private final LongSet destroyedBlocks = new LongOpenHashSet();
    private final SpiralBlastCursor naturalScan;
    private final SpiralBlastCursor radiationScan;
    private Iterator<Entity> entities;
    private BresenhamRay ray;
    private int currentRay;
    private int rayStep;
    private float remainingRayPower;
    private boolean raysComplete;
    private boolean damageComplete;
    private boolean naturalBlocksCleanupComplete;
    private boolean radiationComplete;
    private boolean advancing;

    public NukeExplosionTaskOctree(ServerLevel level, Vec3 center, float totalPower,
                                   int radius, int radiusY, List<Entity> entities) {
        com.miophas.singularity_iteration.core.runtime.reactor.BlastInput.octree(center.x, center.y, center.z, totalPower, radius, radiusY);
        this.level = Objects.requireNonNull(level);
        this.center = Objects.requireNonNull(center);
        this.totalPower = totalPower;
        this.radius = radius;
        this.radiusY = radiusY;
        this.entities = Objects.requireNonNull(entities).iterator();
        double surfaceArea = 4 * Math.PI * radius * radiusY;
        this.rayCount = Math.max(2_000, Math.min((int) (surfaceArea / 2), 50_000));
        this.naturalScan = new SpiralBlastCursor(Math.max(0, radius), Math.max(0, radius));
        this.radiationScan = new SpiralBlastCursor(Math.max(0, radius), Math.max(0, radius));
    }

    private record BresenhamRay(Vec3 direction, int steps) { }

    /** Compute only the current ray, with the original direction and length formula. */
    private static BresenhamRay rayAt(int index, int count, int radius, int radiusY) {
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        double y = 1.0 - (index / (double) (count - 1)) * 2.0;
        double radiusAtY = Math.sqrt(1.0 - y * y);
        double theta = phi * index;
        double x = Math.cos(theta) * radiusAtY;
        double z = Math.sin(theta) * radiusAtY;
        y = y * radiusY / Math.max(radius, radiusY);
        Vec3 direction = new Vec3(x, y, z).normalize();
        double scaleX = Math.abs(direction.x) * radius;
        double scaleY = Math.abs(direction.y) * radiusY;
        double scaleZ = Math.abs(direction.z) * radius;
        int steps = (int) Math.sqrt(scaleX * scaleX + scaleY * scaleY + scaleZ * scaleZ);
        return new BresenhamRay(direction, steps);
    }

    private static BlockPos rayPosition(Vec3 center, Vec3 direction, int step) {
        // Keep the original float rounding, including at negative coordinates.
        float x = (float) (center.x + direction.x * step);
        float y = (float) (center.y + direction.y * step);
        float z = (float) (center.z + direction.z * step);
        return BlockPos.containing(x, y, z);
    }

    /** Compatibility entry point; the shared scheduler calls advance directly. */
    public void tick() {
        if (advancing) return;
        for (int step = 0; step < 300 && !isComplete(); step++) advance();
    }

    /**
     * Inspect at most one block position or one entity. Empty rays/columns and phase
     * transitions also consume a step, so skipped work cannot hide an unbounded loop.
     * A throwing world callback is reported and removed by the scheduler, never replayed.
     */
    public boolean advance() {
        if (advancing) return false;
        if (isComplete()) return true;
        advancing = true;
        try {
            if (!raysComplete) advanceRay();
            else if (!damageComplete) advanceDamage();
            else if (!naturalBlocksCleanupComplete) advanceNaturalColumn();
            else advanceRadiationColumn();
            return isComplete();
        } finally {
            advancing = false;
        }
    }

    private void advanceRay() {
        if (ray == null) {
            if (currentRay >= rayCount) {
                raysComplete = true;
                return;
            }
            ray = rayAt(currentRay++, rayCount, radius, radiusY);
            rayStep = 0;
            remainingRayPower = totalPower;
        }
        if (rayStep >= ray.steps || !(remainingRayPower > 0)) {
            ray = null;
            return;
        }
        int step = rayStep++;
        BlockPos pos = rayPosition(center, ray.direction, step);
        if (destroyedBlocks.contains(pos.asLong()) || !canRead(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        Block block = state.getBlock();
        if (block.defaultDestroyTime() < 0) {
            remainingRayPower = 0;
            return;
        }
        float resistance = block.defaultDestroyTime();
        double distanceFactor = 1.0 - (double) step / ray.steps;
        if (isNaturalBlock(state)) resistance *= 0.1F;
        else if (!state.isSolidRender(level, pos)) resistance *= 0.2F;
        float attenuation = resistance * 0.15F * (0.5F + (1.0F - (float) distanceFactor));
        remainingRayPower -= Math.max(attenuation, 0.02F);
        if (remainingRayPower > 0) destroy(pos);
    }

    private void advanceDamage() {
        if (!entities.hasNext()) {
            entities = Collections.emptyIterator();
            damageComplete = true;
            return;
        }
        Entity entity = entities.next();
        if (entity.isRemoved() || entity.level() != level || !level.hasChunkAt(entity.blockPosition())) return;
        double distance = center.distanceTo(entity.position());
        if (!(distance <= Math.max(radius, radiusY))) return;
        if (entity instanceof ItemEntity) {
            entity.discard();
            return;
        }
        float distanceFactor = (float) Math.pow(1.0 - distance / Math.max(radius, radiusY), 2);
        float damage = totalPower * distanceFactor * 5.0F;
        if (damage > 1.0F) entity.hurt(level.damageSources().explosion(null, null), damage);
    }

    private void advanceNaturalColumn() {
        var step = naturalScan.next();
        if (step == SpiralBlastCursor.Step.DONE) {
            naturalBlocksCleanupComplete = true;
            return;
        }
        if (step != SpiralBlastCursor.Step.POSITION) return;
        BlockPos pos = columnPosition(naturalScan);
        if (destroyedBlocks.contains(pos.asLong()) || !canRead(pos)) return;
        if (isNaturalBlock(level.getBlockState(pos))) destroy(pos);
    }

    private void advanceRadiationColumn() {
        var step = radiationScan.next();
        if (step == SpiralBlastCursor.Step.DONE) {
            radiationComplete = true;
            return;
        }
        if (step != SpiralBlastCursor.Step.POSITION) return;
        BlockPos pos = columnPosition(radiationScan);
        if (destroyedBlocks.contains(pos.asLong()) || !canRead(pos)) return;
        Block block = level.getBlockState(pos).getBlock();
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) {
            if (level.random.nextFloat() < 0.3f)
                level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_STONE.get().defaultBlockState(), 3);
        } else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            if (level.random.nextFloat() < 0.5f)
                level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DIRT.get().defaultBlockState(), 3);
        } else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
            if (level.random.nextFloat() < 0.25f)
                level.setBlock(pos, mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get().defaultBlockState(), 3);
        }
    }

    private BlockPos columnPosition(SpiralBlastCursor scan) {
        return new BlockPos((int) center.x + scan.x(), (int) center.y + scan.y(), (int) center.z + scan.z());
    }

    private boolean canRead(BlockPos pos) {
        // Guards this task's direct access, not arbitrary block/physics callbacks.
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
    }

    private void destroy(BlockPos pos) {
        // Flags 3 already update clients and neighbours for this position. No historical
        // update collection is flushed later, possibly after its chunk has unloaded.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        destroyedBlocks.add(pos.asLong());
    }

    private boolean isNaturalBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof SnowLayerBlock || block == Blocks.SNOW_BLOCK) return true;
        if (block instanceof LeavesBlock || state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS) || block == Blocks.SHORT_GRASS
                || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.LARGE_FERN) return true;
        if (block == Blocks.VINE || state.is(BlockTags.CROPS)) return true;
        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM
                || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK) return true;
        if (block == Blocks.CACTUS || block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO) return true;
        if (block == Blocks.DEAD_BUSH || block == Blocks.LILY_PAD
                || block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS
                || block == Blocks.KELP || block == Blocks.KELP_PLANT) return true;
        return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE;
    }

    public boolean isComplete() {
        return raysComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }
}
