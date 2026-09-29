package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.runtime.reactor.BlastInput;
import com.miophas.singularity_iteration.core.runtime.reactor.SpiralBlastCursor;
import java.util.List;
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
import net.minecraft.world.phys.Vec3;

/** Resumable reactor wave, entity damage, natural cleanup and radiation, in that order. */
@SuppressWarnings("null")
public class ReactorExplosionTaskWave {
    private static final int ANGLE_SEGMENTS = 144;
    private static final int COMPATIBILITY_STEPS_PER_TICK = 100;
    private final ServerLevel level;
    private final Vec3 center;
    private final int centerX, centerY, centerZ;
    private final float totalPower;
    private final int radius, radiusY, maxWaveRadius, radiationRadius;
    private final List<Entity> entities;
    private final RandomSource random;
    // Cross-phase de-duplication needs only packed coordinates.  Keeping a
    // primitive set substantially lowers retained memory for large waves.
    private final LongSet destroyedBlocks = new LongOpenHashSet();
    private final float[] waveEnergyByAngle = new float[ANGLE_SEGMENTS];
    private final SpiralBlastCursor naturalCursor, radiationCursor;
    private final HeightCursor waveHeight = new HeightCursor();
    private final ProofCursor proof = new ProofCursor();
    private boolean wavePropagationComplete, damageComplete, naturalBlocksCleanupComplete, radiationComplete;
    private boolean advancing;
    private int entityIndex;

    // wave -> angle -> x offset -> z offset -> ascending y offset
    private int currentWave, angleIndex;
    private boolean waveColumnOpen;
    private int waveX, waveZ, waveYRange, waveXOffset, waveZOffset;
    private float wavePower, remainingPower;

    // Every directional precheck is itself suspended after one checked position.
    private boolean checkingProof;

    public ReactorExplosionTaskWave(ServerLevel level, Vec3 center, float totalPower,
            int radius, int radiusY, List<Entity> entities) {
        BlastInput.wave(center.x, center.y, center.z, totalPower, radius, radiusY);
        this.level = level;
        this.center = center;
        this.centerX = (int) center.x;
        this.centerY = (int) center.y;
        this.centerZ = (int) center.z;
        this.totalPower = totalPower;
        this.radius = radius;
        this.radiusY = radiusY;
        this.entities = entities;
        this.random = level.random;
        this.maxWaveRadius = Math.max(radius, radiusY);
        this.radiationRadius = (int) (maxWaveRadius * 1.5);
        this.naturalCursor = new SpiralBlastCursor(radius, radius);
        this.radiationCursor = new SpiralBlastCursor(radiationRadius, radiationRadius);
        initializeWaveEnergy();
    }

    private void initializeWaveEnergy() {
        for (int i = 0; i < ANGLE_SEGMENTS; i++) {
            double angle = (i * 2.0 * Math.PI) / ANGLE_SEGMENTS;
            double noise = Math.sin(angle * 3) * 0.3
                    + Math.sin(angle * 7) * 0.2 + Math.sin(angle * 13) * 0.1;
            noise += (random.nextFloat() - 0.5) * 0.4;
            float energyFactor = (float) (1.0 + noise * 0.3);
            waveEnergyByAngle[i] = Math.max(0.85f, Math.min(1.15f, energyFactor));
        }
    }

    /** Compatibility entry point; the shared scheduler uses advance directly. */
    public void tick() {
        for (int i = 0; i < COMPATIBILITY_STEPS_PER_TICK && !advance(); i++) { }
    }

    /** At most one checked block position, one entity, or one cursor transition. */
    public boolean advance() {
        // Block/entity callbacks can call back into the task before the current write returns.
        if (advancing) return isComplete();
        advancing = true;
        try {
            if (!wavePropagationComplete) advanceWave();
            else if (!damageComplete) advanceDamage();
            else if (!naturalBlocksCleanupComplete) advanceNaturalCleanup();
            else if (!radiationComplete) advanceRadiation();
            return isComplete();
        } finally {
            advancing = false;
        }
    }

    private void advanceWave() {
        if (!waveColumnOpen) {
            if (currentWave >= maxWaveRadius) {
                wavePropagationComplete = true;
                return;
            }
            if (angleIndex >= ANGLE_SEGMENTS) {
                currentWave++;
                angleIndex = 0;
                return;
            }
            wavePower = totalPower * (1.0f - (float) currentWave / maxWaveRadius);
            if (wavePower <= 0) {
                currentWave++;
                angleIndex = 0;
                return;
            }
            int angle = angleIndex++;
            if (currentWave > (int) (radius * waveEnergyByAngle[angle])) return;
            double baseAngle = (angle * 2.0 * Math.PI) / ANGLE_SEGMENTS;
            double cos = Math.cos(baseAngle), sin = Math.sin(baseAngle);
            waveX = centerX + (int) (cos * currentWave);
            waveZ = centerZ + (int) (sin * currentWave);
            double normalizedDistance = (double) currentWave / radius;
            waveYRange = normalizedDistance >= 1.0 ? 2
                    : Math.max((int) (radiusY * Math.sqrt(1.0 - normalizedDistance * normalizedDistance)), 2);
            waveXOffset = waveZOffset = -1;
            waveHeight.reset(waveYRange);
            remainingPower = wavePower;
            waveColumnOpen = true;
            beginProof(cos, sin, currentWave);
            return;
        }
        if (checkingProof) {
            if (advanceProof()) {
                waveColumnOpen = false;
                checkingProof = false;
            }
            return;
        }
        if (!waveHeight.advance()) {
            if (++waveZOffset > 1) {
                waveZOffset = -1;
                waveXOffset++;
            }
            if (waveXOffset > 1) {
                waveColumnOpen = false;
                return;
            }
            waveHeight.reset(waveYRange);
            remainingPower = wavePower;
            return;
        }

        int x = waveX + waveXOffset, z = waveZ + waveZOffset;
        int yNoise = (int) ((random.nextFloat() - 0.5) * waveYRange * 0.3);
        int y = centerY + waveHeight.y() + yNoise;
        BlockPos pos = new BlockPos(x, y, z);
        if (destroyedBlocks.contains(pos.asLong())) return;
        double dx = (x - center.x) / radius;
        double dy = (y - center.y) / radiusY;
        double dz = (z - center.z) / radius;
        double normalizedDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double distanceFactor = 1.0 - normalizedDistance;
        if (distanceFactor <= 0) return;
        BlockState state = loadedState(pos);
        if (state == null || state.isAir()) return;
        Block block = state.getBlock();
        if (isExplosionProofBlock(block)) {
            remainingPower = 0;
            waveHeight.skip();
            return;
        }
        float hardness = block.defaultDestroyTime();
        float resistance = hardness;
        if (isNaturalBlock(state)) resistance *= 0.01f;
        else if (!state.isSolidRender(level, pos)) resistance *= 0.02f;
        else resistance *= 0.1f;
        float destroyChance = (float) (remainingPower * distanceFactor * 100.0f / (resistance + 0.01f));
        destroyChance *= (0.5f + random.nextFloat() * 1.0f);
        if (destroyChance > 0.01f) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            destroyedBlocks.add(pos.asLong());
            remainingPower *= 1.0f - Math.min(0.5f, hardness / 100.0f);
        } else {
            remainingPower *= 1.0f - Math.min(0.7f, hardness / 50.0f);
        }
        if (remainingPower < 0.1f) waveHeight.skip();
    }

    private void beginProof(double dirX, double dirZ, int distance) {
        proof.reset(dirX, dirZ, distance);
        checkingProof = !proof.isComplete();
    }

    /** True means the current column is completely blocked. Never scans a whole ray. */
    private boolean advanceProof() {
        if (!proof.advance()) {
            checkingProof = false;
            return false;
        }
        BlockPos pos = new BlockPos(centerX + proof.x(), centerY + proof.y(), centerZ + proof.z());
        checkingProof = !proof.isComplete();
        BlockState state = loadedState(pos);
        return state != null && isExplosionProofBlock(state.getBlock());
    }

    private BlockState loadedState(BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return null;
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    private boolean isExplosionProofBlock(Block block) {
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.NETHERITE_BLOCK || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.ENCHANTING_TABLE || block.defaultDestroyTime() < 0;
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

    private void advanceDamage() {
        if (entityIndex >= entities.size()) {
            damageComplete = true;
            return;
        }
        Entity entity = entities.get(entityIndex++);
        if (entity == null || entity.isRemoved() || entity.level() != level) return;
        BlockPos at = entity.blockPosition();
        if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null) return;
        double distance = center.distanceTo(entity.position());
        if (distance > maxWaveRadius) return;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
            entity.discard();
            return;
        }
        float distanceFactor = (float) Math.pow(1.0 - distance / maxWaveRadius, 2);
        float damage = totalPower * distanceFactor * 5.0F;
        if (damage > 1.0F) entity.hurt(level.damageSources().explosion(null, null), damage);
    }

    private void beginSpiralProof(SpiralBlastCursor cursor) {
        if (!cursor.hasColumn()) return;
        int distance = (int) Math.sqrt((double) cursor.x() * cursor.x() + (double) cursor.z() * cursor.z());
        if (distance > 0) beginProof(cursor.x() / (double) distance, cursor.z() / (double) distance, distance);
    }

    private void advanceNaturalCleanup() {
        if (checkingProof) {
            if (advanceProof()) {
                checkingProof = false;
                naturalCursor.skipColumn();
            }
            return;
        }
        switch (naturalCursor.next()) {
            case DONE -> naturalBlocksCleanupComplete = true;
            case COLUMN -> beginSpiralProof(naturalCursor);
            case POSITION -> {
                BlockPos pos = new BlockPos(centerX + naturalCursor.x(), centerY + naturalCursor.y(), centerZ + naturalCursor.z());
                BlockState state = loadedState(pos);
                if (state == null) return;
                if (isExplosionProofBlock(state.getBlock())) {
                    naturalCursor.skipColumn();
                    return;
                }
                if (destroyedBlocks.contains(pos.asLong())) return;
                if (isNaturalBlock(state)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    destroyedBlocks.add(pos.asLong());
                }
            }
        }
    }

    private void advanceRadiation() {
        if (checkingProof) {
            if (advanceProof()) {
                checkingProof = false;
                radiationCursor.skipColumn();
            }
            return;
        }
        switch (radiationCursor.next()) {
            case DONE -> radiationComplete = true;
            case COLUMN -> beginSpiralProof(radiationCursor);
            case POSITION -> convertRadiationPosition();
        }
    }

    private void convertRadiationPosition() {
        int x = radiationCursor.x(), y = radiationCursor.y(), z = radiationCursor.z();
        BlockPos pos = new BlockPos(centerX + x, centerY + y, centerZ + z);
        BlockState state = loadedState(pos);
        if (state == null) return;
        Block block = state.getBlock();
        if (isExplosionProofBlock(block)) {
            radiationCursor.skipColumn();
            return;
        }
        if (destroyedBlocks.contains(pos.asLong())) return;
        double distFromCenter = Math.sqrt((double) x * x + (double) z * z + (double) y * y);
        double normalizedDistance = distFromCenter / Math.sqrt((double) radiationRadius * radiationRadius);
        double gaussianFactor = Math.exp(-(normalizedDistance * normalizedDistance) / 2.0);
        float baseChance;
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) baseChance = 0.95f;
        else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) baseChance = 0.98f;
        else if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) baseChance = 0.9f;
        else return;
        float convertChance = baseChance * (float) gaussianFactor;
        if (random.nextFloat() >= convertChance) return;
        Block converted;
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) converted = mio_icif_blocks.BLOCK_RADIATING_STONE.get();
        else if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) converted = mio_icif_blocks.BLOCK_RADIATING_DIRT.get();
        else converted = mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get();
        level.setBlock(pos, converted.defaultBlockState(), Block.UPDATE_ALL);
    }

    public boolean isComplete() {
        return wavePropagationComplete && damageComplete && naturalBlocksCleanupComplete && radiationComplete;
    }

    /** Allocation-free inclusive ascending height traversal, including safe terminal sentinels. */
    public static final class HeightCursor {
        private long next;
        private int limit, y;

        public void reset(int extent) {
            if (extent < 0) throw new IllegalArgumentException("Negative height extent");
            next = -(long) extent;
            limit = extent;
        }

        public boolean advance() {
            if (next > limit) return false;
            y = (int) next++;
            return true;
        }

        public void skip() { next = (long) limit + 1; }
        public int y() { return y; }
    }

    /** One sample of the original distance-major, five-high shielding ray per advance. */
    public static final class ProofCursor {
        private double dirX, dirZ;
        private long next, count;
        private int x, y, z;

        public void reset(double dirX, double dirZ, int distance) {
            if (distance < 0) throw new IllegalArgumentException("Negative proof distance");
            this.dirX = dirX;
            this.dirZ = dirZ;
            next = 0;
            count = (long) distance * 5;
        }

        public boolean advance() {
            if (isComplete()) return false;
            long step = next++;
            long distance = step / 5 + 1;
            x = (int) (dirX * distance);
            z = (int) (dirZ * distance);
            y = (int) (step % 5) - 2;
            return true;
        }

        public boolean isComplete() { return next >= count; }
        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
    }
}
