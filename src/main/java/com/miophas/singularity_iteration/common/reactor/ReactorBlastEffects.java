package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.reactor.NukeExplosionScheduler;
import com.miophas.singularity_iteration.common.blockentity.reactor.NukeExplosionTask;
import com.miophas.singularity_iteration.common.blockentity.reactor.NukeRadiationZoneManager;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Common-only world adapter for committed reactor accident strengths. */
public final class ReactorBlastEffects {
    private ReactorBlastEffects() {}
    public record Prepared(ServerLevel level, BlockPos pos, int radius, NukeExplosionTask task, UUID zone) {
        public void cancel() {
            NukeExplosionScheduler.cancelExplosion(level, task);
            NukeRadiationZoneManager.removeRadiationZone(zone);
        }
        public void present() {
            try {
                Vec3 center = Vec3.atCenterOf(pos);
                mio_icif_Network.sendNuclearExplosionAnimation(level, center.x, center.y, center.z, radius);
            } catch (RuntimeException failure) {
                Singularity_Iteration.LOGGER.error("Reactor blast presentation failed at {}", pos, failure);
            }
        }
    }
    /** Admission precedes source removal. A full queue leaves the armed reactor available for retry. */
    public static Prepared tryPrepare(ServerLevel level, BlockPos pos, float power) {
        if (!Float.isFinite(power) || power <= 0) throw new IllegalArgumentException("Invalid reactor blast power");
        if (!ExplosionWorkScheduler.hasCapacity(level)) return null;
        int radius = Math.max(1, Math.min(2000, (int) Math.ceil(power * 2)));
        int radiusY = Math.max(1, (int) (radius * .7));
        Vec3 center = Vec3.atCenterOf(pos);
        var entities = level.getEntities(null, new AABB(center.x - radius, center.y - radiusY, center.z - radius,
            center.x + radius, center.y + radiusY, center.z + radius));
        float pressurePower = radius * 3F; // Legacy SI reactor blast strength mapping.
        var task = new NukeExplosionTask(level, center, pressurePower, radius, radiusY, entities);
        if (!NukeExplosionScheduler.tryStartExplosion(level, pos, task)) return null;
        UUID zone=null;
        try {
            zone = NukeRadiationZoneManager.createRadiationZone(level, pos, radius, pressurePower);
            com.miophas.singularity_iteration.common.blockentity.reactor.NuclearFalloutManager.register(level,zone,center,radius,radiusY);
            task.bindFallout(zone);
            return new Prepared(level, pos.immutable(), radius, task, zone);
        } catch (RuntimeException failure) {
            NukeExplosionScheduler.cancelExplosion(level, task);
            if(zone!=null)NukeRadiationZoneManager.removeRadiationZone(zone);
            throw failure;
        }
    }
}
