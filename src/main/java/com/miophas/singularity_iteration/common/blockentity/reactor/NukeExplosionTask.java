package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.core.runtime.reactor.BlastInput;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

/** Independent expanding pressure-front blast, driven by the shared server work budget.
 * Each advance samples one loaded position. Four shell caches retain pre-blast resistance,
 * so later rays cannot exploit holes cut by earlier rays. No chunk loads or cubic cleanup scans.
 */
@SuppressWarnings("null")
public class NukeExplosionTask {
    private final ServerLevel level;
    private final Vec3 center;
    private final float totalPower, initialPressure;
    private final int radius, radiusY, rayCount;
    private final Explosion context;
    private final float[] pressure;
    private final double[] dx, dy, dz;
    private final long[] lastPositions;
    private final int[] active;
    private int activeCount;
    private final Long2FloatOpenHashMap[] shells = new Long2FloatOpenHashMap[4];
    private final int minX, maxX, minZ, maxZ;
    private int surfaceChunkX, surfaceChunkZ;
    private NuclearFalloutChunkTask surfaceTask;
    private NuclearFalloutState.Region fallout;
    private NuclearFalloutState falloutState;
    private UUID falloutId;
    private Iterator<Entity> entities;
    private Entity damageTarget;
    private Vec3 damageDirection;
    private double damageDistance, effectiveDamageDistance;
    private int damageStep;
    private float cover;
    private int shell, ray;
    private boolean advancing, damageComplete, raysComplete, surfaceComplete;

    public NukeExplosionTask(ServerLevel level, Vec3 center, float totalPower, int radius, List<Entity> entities) {
        this(level, center, totalPower, radius, radius, entities);
    }
    /** Reactor accidents use the legacy flattened shape; ordinary nuclear bombs remain spherical. */
    public NukeExplosionTask(ServerLevel level, Vec3 center, float totalPower, int radius, int radiusY, List<Entity> entities) {
        BlastInput.nuke(center.x, center.y, center.z, totalPower, radius);
        if (radiusY < 0 || radiusY > radius || (radius > 0 && radiusY == 0))
            throw new IllegalArgumentException("Invalid nuclear blast vertical radius");
        this.radiusY = radiusY;
        this.level = level; this.center = center; this.totalPower = totalPower; this.radius = radius;
        initialPressure = (float) (Math.sqrt(Math.max(0, totalPower)) * 8);
        this.entities = entities.iterator();
        damageComplete = raysComplete = surfaceComplete = totalPower <= 0 || radius == 0;
        context = new Explosion(level, null, center.x, center.y, center.z, radius, false, Explosion.BlockInteraction.DESTROY);
        rayCount = Math.max(500, Math.min((int) (4 * Math.PI * radius * radius), 20000));
        pressure = new float[rayCount]; dx = new double[rayCount]; dy = new double[rayCount]; dz = new double[rayCount];
        lastPositions = new long[rayCount];
        active = new int[rayCount]; activeCount = rayCount;
        java.util.Arrays.fill(lastPositions, Long.MIN_VALUE);
        java.util.Arrays.fill(pressure, initialPressure);
        for (int i = 0; i < rayCount; i++) {
            active[i] = i;
            // Include the six cardinal rays as well as the uniformly distributed sphere.
            if (i < 6) {
                int axis = i / 2; double sign = (i & 1) == 0 ? 1 : -1;
                dx[i] = axis == 0 ? sign : 0; dy[i] = axis == 1 ? sign : 0; dz[i] = axis == 2 ? sign : 0;
            } else {
                double y = 1 - 2.0 * (i - 6) / (rayCount - 7);
                double circle = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = Math.PI * (3 - Math.sqrt(5)) * i;
                dx[i] = Math.cos(theta) * circle; dy[i] = y; dz[i] = Math.sin(theta) * circle;
            }
        }
        if (radius > 0) for (int i = 0; i < rayCount; i++) dy[i] *= (double) radiusY / radius;
        for (int i = 0; i < shells.length; i++) shells[i] = new Long2FloatOpenHashMap();
        int extent = (int) Math.ceil(radius * 1.5);
        minX = (int) Math.floor(center.x) - extent; maxX = (int) Math.floor(center.x) + extent;
        minZ = (int) Math.floor(center.z) - extent; maxZ = (int) Math.floor(center.z) + extent;
        surfaceChunkX = minX >> 4; surfaceChunkZ = minZ >> 4;
        if(radius>0)fallout=new NuclearFalloutState.Region(center,radius,radiusY,System.currentTimeMillis());
    }
    public void tick() { for (int i = 0; i < 500 && !isComplete(); i++) advance(); }
    public boolean advance() {
        if (isComplete())return true;
        if(!com.miophas.singularity_iteration.common.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get())return false;
        if (advancing) return false;
        advancing = true;
        try {
            // Read shielding before the blast alters terrain; one LOS sample per step.
            if (!damageComplete) advanceDamage();
            else if (!raysComplete) advanceRay();
            else if (!surfaceComplete) advanceSurface();
            return isComplete();
        } finally { advancing = false; }
    }
    private void advanceRay() {
        if (shell >= radius) { raysComplete = true; return; }
        if (ray >= activeCount) {
            if (activeCount == 0) { raysComplete = true; return; }
            ray = 0; shell++;
            shells[shell % shells.length].clear();
            return;
        }
        int i = active[ray++];
        BlockPos pos = BlockPos.containing(center.x + dx[i] * shell,
            center.y + dy[i] * shell, center.z + dz[i] * shell);
        long key = pos.asLong();
        if (lastPositions[i] == key) return;
        lastPositions[i] = key;
        LevelChunk chunk = loadedChunk(pos);
        if (chunk == null) { extinguish(i); return; }
        BlockState state = chunk.getBlockState(pos);
        float resistance = Float.NaN;
        for (var cache : shells) if (cache.containsKey(key)) { resistance = cache.get(key); break; }
        if (Float.isNaN(resistance)) {
            resistance = resistance(state, pos);
            if (!state.isAir()) shells[shell % shells.length].put(key, resistance);
        }
        if (!Float.isFinite(resistance)) { extinguish(i); return; }
        double distance = (double) shell / radius;
        float loss = (float) ((resistance + .3) * (.5 + 7 * distance * distance));
        pressure[i] -= loss;
        if (pressure[i] <= 0) extinguish(i);
        else if (!state.isAir()) replace(pos, Blocks.AIR.defaultBlockState());
    }
    private void extinguish(int i) {
        pressure[i] = 0;
        active[--ray] = active[--activeCount];
    }
    private float resistance(BlockState state, BlockPos pos) {
        if (state.getDestroySpeed(level, pos) < 0) return Float.POSITIVE_INFINITY;
        if (!state.getFluidState().isEmpty()) return .1F;
        // HBM treats obsidian as dense stone rather than an absolute nuclear shield.
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN))
            return Blocks.STONE.getExplosionResistance() * 3;
        float resistance = state.getExplosionResistance(level, pos, context);
        return Float.isFinite(resistance) && resistance >= 0 ? resistance : Float.POSITIVE_INFINITY;
    }
    private void advanceDamage() {
        if (damageTarget == null) {
            if (!entities.hasNext()) { damageComplete = true; entities = java.util.Collections.emptyIterator(); return; }
            Entity entity = entities.next();
            if (entity.isRemoved() || entity.level() != level || entity.ignoreExplosion(context) || loadedChunk(entity.blockPosition()) == null) return;
            Vec3 displacement = entity.getBoundingBox().getCenter().subtract(center);
            damageDistance = displacement.length();
            effectiveDamageDistance = Math.sqrt(displacement.x * displacement.x + displacement.z * displacement.z
                + Math.pow(displacement.y * radius / Math.max(1.0, radiusY), 2));
            if (effectiveDamageDistance > radius) return;
            damageTarget = entity; damageDirection = displacement.normalize(); damageStep = 1; cover = 0;
            return;
        }
        if (damageStep < damageDistance && cover < initialPressure) {
            BlockPos pos = BlockPos.containing(center.add(damageDirection.scale(damageStep++)));
            LevelChunk chunk = loadedChunk(pos);
            if (chunk == null) cover = Float.POSITIVE_INFINITY;
            else cover += resistance(chunk.getBlockState(pos), pos) * 4;
            return;
        }
        Entity entity = damageTarget; damageTarget = null;
        if (entity.isRemoved() || entity.level() != level) return;
        double exposure = Math.max(0, 1 - cover / initialPressure);
        double impulse = Math.pow(Math.max(0, 1 - effectiveDamageDistance / radius), 2) * exposure;
        if (impulse <= 0) return;
        if (entity instanceof ItemEntity) { entity.discard(); return; }
        entity.hurt(level.damageSources().explosion(context), (float) Math.min(1000000, totalPower * impulse * 5));
        entity.setDeltaMovement(entity.getDeltaMovement().add(damageDirection.scale(impulse * 3)));
        entity.hurtMarked = true;
    }
    /** Link the admitted blast to its persistent footprint before source commit. */
    public void bindFallout(UUID id){
        falloutState=NuclearFalloutState.get(level);fallout=falloutState.regions.get(id);
        if(fallout==null)throw new IllegalArgumentException("Missing fallout footprint");falloutId=id;
    }
    private void advanceSurface() {
        if(surfaceTask!=null){if(surfaceTask.getAsBoolean())surfaceTask=null;return;}
        if(surfaceChunkZ>(maxZ>>4)){
            surfaceComplete=true;
            if(falloutId!=null)NuclearFalloutManager.ready(level,falloutId);
            return;
        }
        int cx=surfaceChunkX,cz=surfaceChunkZ;
        if(++surfaceChunkX>(maxX>>4)){surfaceChunkX=minX>>4;surfaceChunkZ++;}
        if(!fallout.intersectsChunk(cx,cz))return;
        LevelChunk chunk=level.getChunkSource().getChunkNow(cx,cz);
        if(chunk!=null&&fallout.columns.getOrDefault(chunk.getPos().toLong(),0)<256)
            surfaceTask=new NuclearFalloutChunkTask(level,fallout,chunk,falloutState,
                ()->falloutId==null||falloutState.regions.get(falloutId)==fallout);
        // Missing chunks remain incomplete in the saved footprint and resume on a later load.
    }
    private void replace(BlockPos pos, BlockState state) {
        // Do not synchronously fan out neighbour callbacks across a large nuclear crater.
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
    private LevelChunk loadedChunk(BlockPos pos) {
        return level.isOutsideBuildHeight(pos) ? null : level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }
    public boolean isComplete() { return damageComplete && raysComplete && surfaceComplete; }
}
