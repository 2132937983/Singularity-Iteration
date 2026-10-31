package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.effect.mio_icif_effects;
import com.miophas.singularity_iteration.common.util.RadiationProtectionUtil;
import com.miophas.singularity_iteration.common.util.RadiationShielding;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.Map;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Persistent, world-owned radiation. Overlapping zones apply their strongest effect once. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public class NukeRadiationZoneManager {
    public static final long RADIATION_DURATION_MS = RadiationState.DURATION;
    /** 0.1.10.5: blocks above the eyes that count as the roof of a fallout shelter. */
    static final double ROOF_CHECK = 16;

    /** Retained public value type; it does not register itself or retain a Level. */
    public static class RadiationZone {
        public final BlockPos center;
        public final int radius;
        public final float power;
        public final long creationTime;
        public final String dimension;
        public RadiationZone(BlockPos center, int radius, float power, String dimension) {
            this.center = center.immutable(); this.radius = Math.clamp(radius, 0, 2000);
            this.power = Float.isFinite(power) ? Math.max(0, power) : 0;
            this.creationTime = System.currentTimeMillis(); this.dimension = dimension;
        }
        public boolean isExpired() { return RadiationState.expired(creationTime, System.currentTimeMillis()); }
        public long getRemainingTime() { return remaining(creationTime); }
    }
    public static UUID createRadiationZone(ServerLevel level, BlockPos center, int radius, float power) {
        var zone = new RadiationZone(center, radius, power, level.dimension().location().toString());
        var data = RadiationState.get(level); var id = UUID.randomUUID();
        data.zones.put(id, new RadiationState.Zone(zone.center, zone.radius, zone.power, zone.creationTime));
        data.setDirty(); return id;
    }
    public static void removeRadiationZone(UUID id) {
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        for (var level : server.getAllLevels()) {
            var data = RadiationState.get(level);
            if (data.zones.remove(id) != null) data.setDirty();
            NuclearFalloutManager.remove(level,id);
        }
    }
    private static double distanceSquared(ServerLevel level,UUID id,RadiationState.Zone zone,double x,double y,double z){
        double dx=x-zone.center().getX()-.5,dy=y-zone.center().getY()-.5,dz=z-zone.center().getZ()-.5;
        var footprint=NuclearFalloutState.get(level).regions.get(id);
        if(footprint!=null){
            if(!footprint.processedColumn((int)Math.floor(x),(int)Math.floor(z)))return Double.POSITIVE_INFINITY;
            dy*= (double)footprint.radius/footprint.radiusY;
        }
        return dx*dx+dy*dy+dz*dz;
    }
    private static boolean contains(ServerLevel level,UUID id,RadiationState.Zone zone,double x,double y,double z){
        return distanceSquared(level,id,zone,x,y,z)<=(double)zone.radius()*zone.radius();
    }
    private static float intensity(ServerLevel level,UUID id,RadiationState.Zone zone,double x,double y,double z){
        double distance=distanceSquared(level,id,zone,x,y,z);
        if(distance>(double)zone.radius()*zone.radius())return 0;
        return zone.radius()==0?1:(float)Math.max(0,1-Math.sqrt(distance)/zone.radius());
    }
    public static boolean isInRadiationZone(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel world) || pos == null) return false;
        long now = System.currentTimeMillis();
        for (var entry : RadiationState.get(world).zones.entrySet()) {
            var zone=entry.getValue();
            if (!RadiationState.expired(zone.created(), now) && contains(world,entry.getKey(),zone, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5)) return true;
        }
        return false;
    }
    public static float getRadiationIntensity(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel world) || pos == null) return 0;
        long now = System.currentTimeMillis(); float result = 0;
        for (var entry : RadiationState.get(world).zones.entrySet()) {
            var zone=entry.getValue();
            if (RadiationState.expired(zone.created(), now)) continue;
            result = Math.max(result, intensity(world,entry.getKey(),zone, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) * Math.min(zone.power() / 1000, 1));
        }
        return result;
    }
    public static int getActiveZoneCount() {
        var server = ServerLifecycleHooks.getCurrentServer(); int count = 0;
        if (server != null) for (var level : server.getAllLevels()) for (var zone : RadiationState.get(level).zones.values())
            if (!RadiationState.expired(zone.created(), System.currentTimeMillis())) count++;
        return count;
    }

    private record Geometry(BlockPos center,int radius,UUID footprint){}
    /** Legacy duplicate geometries still share one query; distinct progress masks must not be merged. */
    private static Collection<Map.Entry<UUID,RadiationState.Zone>> strongestGeometry(ServerLevel level,long now){
        var strongest=new LinkedHashMap<Geometry,Map.Entry<UUID,RadiationState.Zone>>();
        var footprints=NuclearFalloutState.get(level).regions;
        for(var entry:RadiationState.get(level).zones.entrySet()){
            var zone=entry.getValue();
            if(RadiationState.expired(zone.created(),now)||!Float.isFinite(zone.power())||zone.power()<=0)continue;
            var geometry=new Geometry(zone.center(),zone.radius(),footprints.containsKey(entry.getKey())?entry.getKey():null);
            var previous=strongest.get(geometry);
            if(previous==null||zone.power()>previous.getValue().power())strongest.put(geometry,entry);
        }
        return strongest.values();
    }
    @SubscribeEvent public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        var data = RadiationState.get(level); long now = System.currentTimeMillis();
        var exposure = new IdentityHashMap<LivingEntity, float[]>();
        for (var entry : strongestGeometry(level, now)) {
            var zone=entry.getValue();
            var c = zone.center(); double r = zone.radius();
            var box = new AABB(c.getX() + .5 - r, c.getY() + .5 - r, c.getZ() + .5 - r,
                c.getX() + .5 + r, c.getY() + .5 + r, c.getZ() + .5 + r);
            for (var entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (RadiationProtectionUtil.isWearingFullHazmat(entity)) continue;
                float factor = intensity(level,entry.getKey(),zone, entity.getX(), entity.getY(), entity.getZ());
                if (factor <= 0) continue;
                factor *= (float) shelterTransmission(level, c, entity);
                if (factor <= 0) continue;
                var value = exposure.computeIfAbsent(entity, ignored -> new float[2]);
                value[0] = Math.max(value[0], zone.power() / 500 * factor);
                value[1] = Math.max(value[1], factor);
            }
        }
        exposure.forEach((entity, value) -> applyRadiationEffects(entity, value[0], value[1]));
    }
    /**
     * Share of a zone's radiation that reaches a creature. Fallout comes from the blast centre and from the sky, so two lines
     * count: the line to the blast centre and the line {@link #ROOF_CHECK} blocks up from the eyes (the roof). 0.1.10.41: the
     * less open line counts, so shielding toward the centre or a shielding roof protects (before, the more open line counted,
     * and outdoors the open sky cancelled every wall).
     */
    static double shelterTransmission(Level level, BlockPos centre, LivingEntity entity) {
        double toCentre = RadiationShielding.transmission(level, RadiationShielding.centre(centre), entity, null);
        if (toCentre <= 0) return 0;
        var eyes = entity.getEyePosition();
        return Math.min(toCentre, RadiationShielding.transmission(level, eyes.add(0, ROOF_CHECK, 0), eyes, null));
    }

    private static void applyRadiationEffects(LivingEntity living, float damage, float intensity) {
        if (!Float.isFinite(damage) || damage <= 0 || intensity <= 0) return;
        float actual = RadiationProtectionUtil.calculateRadiationDamage(living, damage);
        int duration = (int) (100 * intensity);
        if (duration > 20) living.addEffect(new MobEffectInstance(mio_icif_effects.RADIATION, duration, intensity > .7 ? 2 : intensity > .4 ? 1 : 0, false, true));
        if (intensity > .5) living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) (60 * intensity), 0, false, true));
        if (intensity > .8) living.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (40 * intensity), 0, false, true));
        if (actual > 0) living.hurt(new DamageSource(living.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
            .getHolderOrThrow(com.miophas.singularity_iteration.common.event.mio_icif_DamageTypes.RADIATION)), actual);
    }
    private static long remaining(long created) { return Math.clamp(RADIATION_DURATION_MS - (System.currentTimeMillis() - created), 0, RADIATION_DURATION_MS); }
    public static long getZoneRemainingTime(UUID id) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) for (var level : server.getAllLevels()) {
            var zone = RadiationState.get(level).zones.get(id); if (zone != null) return remaining(zone.created());
        }
        return 0;
    }
    public static void clearAllZones() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) for (var level : server.getAllLevels()) {
            var data = RadiationState.get(level);
            for(var id:new java.util.ArrayList<>(data.zones.keySet()))NuclearFalloutManager.remove(level,id);
            data.zones.clear(); data.setDirty();NuclearFalloutManager.clear(level);
        }
    }
}
