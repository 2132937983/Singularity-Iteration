package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.effect.mio_icif_effects;
import com.singularity_iteration.mio_icif.util.RadiationProtectionUtil;
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

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核弹辐射区域管理器
 * 管理核弹爆炸后产生的24小时持续辐射区域
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class NukeRadiationZoneManager {

    // 辐射区域数据类
public static class RadiationZone {
        public final BlockPos center;
        public final int radius;
        public final float power;
        public final long creationTime;
        public final String dimension;

        public RadiationZone(BlockPos center, int radius, float power, String dimension) {
            this.center = center;
            this.radius = radius;
            this.power = power;
            this.creationTime = System.currentTimeMillis();
            this.dimension = dimension;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime >= RADIATION_DURATION_MS;
        }

        public long getRemainingTime() {
            long remaining = RADIATION_DURATION_MS - (System.currentTimeMillis() - creationTime);
            return Math.max(0, remaining);
        }
    }

    // 辐射持续时间：24小时 = 24 * 60 * 60 * 1000 毫秒
public static final long RADIATION_DURATION_MS = 24L * 60L * 60L * 1000L;
    // 每多少tick 处理一次辐射效果
private static final int TICK_INTERVAL = 20; // 每秒处理一次
// 清理过期区域的间隔
private static final int CLEANUP_INTERVAL = 1200; // 每分钟清理一次
    // 存储所有活跃的辐射区域
    private static final Map<UUID, RadiationZone> RADIATION_ZONES = new ConcurrentHashMap<>();

    /**
     * 创建新的辐射区域
     * @param level 世界
     * @param center 爆炸中心
     * @param radius 爆炸半径
     * @param power 爆炸威力
     * @return 辐射区域的UUID
     */
    public static UUID createRadiationZone(ServerLevel level, BlockPos center, int radius, float power) {
        UUID zoneId = UUID.randomUUID();
        String dimension = level.dimension().location().toString();
        RadiationZone zone = new RadiationZone(center, radius, power, dimension);
        RADIATION_ZONES.put(zoneId, zone);

        Singularity_Iteration.LOGGER.info("[核弹辐射] 创建新的辐射区域: 中心={}, 半径={}, 威力={}, 维度={}",
            center, radius, power, dimension);

        return zoneId;
    }

    /**
     * 移除辐射区域
     * @param zoneId 辐射区域ID
     */
    public static void removeRadiationZone(UUID zoneId) {
        RADIATION_ZONES.remove(zoneId);
    }

    /**
     * 检查位置是否在辐射区域内
 * @param level 世界
     * @param pos 位置
     * @return 如果在辐射区域内返回true
     */
    public static boolean isInRadiationZone(Level level, BlockPos pos) {
        String dimension = level.dimension().location().toString();

        for (RadiationZone zone : RADIATION_ZONES.values()) {
            if (!zone.dimension.equals(dimension)) {
                continue;
            }

            double distance = Math.sqrt(
                Math.pow(pos.getX() - zone.center.getX(), 2) +
                Math.pow(pos.getY() - zone.center.getY(), 2) +
                Math.pow(pos.getZ() - zone.center.getZ(), 2)
            );

            if (distance <= zone.radius) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取位置处的辐射强度
     * @param level 世界
     * @param pos 位置
     * @return 辐射强度，0-1之间
 */
    public static float getRadiationIntensity(Level level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        float maxIntensity = 0.0f;

        for (RadiationZone zone : RADIATION_ZONES.values()) {
            if (!zone.dimension.equals(dimension)) {
                continue;
            }

            double distance = Math.sqrt(
                Math.pow(pos.getX() - zone.center.getX(), 2) +
                Math.pow(pos.getY() - zone.center.getY(), 2) +
                Math.pow(pos.getZ() - zone.center.getZ(), 2)
            );

            if (distance <= zone.radius) {
                // 距离越近，辐射越强
            float intensity = (float) (1.0 - distance / zone.radius);
                // 考虑威力因素
                intensity *= Math.min(zone.power / 1000.0f, 1.0f);
                maxIntensity = Math.max(maxIntensity, intensity);
            }
        }

        return maxIntensity;
    }

    /**
     * 获取所有活跃的辐射区域数量
     */
    public static int getActiveZoneCount() {
        return RADIATION_ZONES.size();
    }

    /**
     * 每tick 处理辐射效果
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();

        // 只在服务端执行
    if (level.isClientSide()) {
            return;
        }

        long gameTime = level.getGameTime();
        String dimension = level.dimension().location().toString();

        // 每秒处理一次辐射效果
    if (gameTime % TICK_INTERVAL == 0) {
            processRadiationEffects((ServerLevel) level, dimension);
        }

        // 每分钟清理一次过期区域
    if (gameTime % CLEANUP_INTERVAL == 0) {
            cleanupExpiredZones();
        }
    }

    /**
     * 处理辐射效果
     */
    private static void processRadiationEffects(ServerLevel level, String dimension) {
        for (Map.Entry<UUID, RadiationZone> entry : RADIATION_ZONES.entrySet()) {
            RadiationZone zone = entry.getValue();

            // 只处理当前维度的区域
            if (!zone.dimension.equals(dimension)) {
                continue;
            }

            // 获取区域内的所有生物
        AABB zoneAABB = new AABB(
                zone.center.getX() - zone.radius, zone.center.getY() - zone.radius, zone.center.getZ() - zone.radius,
                zone.center.getX() + zone.radius, zone.center.getY() + zone.radius, zone.center.getZ() + zone.radius
            );

            var entities = level.getEntities(null, zoneAABB);

            for (var entity : entities) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }

                // 检查是否在辐射区域内
            double distance = Math.sqrt(
                    Math.pow(living.getX() - (zone.center.getX() + 0.5), 2) +
                    Math.pow(living.getY() - (zone.center.getY() + 0.5), 2) +
                    Math.pow(living.getZ() - (zone.center.getZ() + 0.5), 2)
                );

                if (distance > zone.radius) {
                    continue;
                }

                // 检查是否穿着防化服
            if (RadiationProtectionUtil.isWearingFullHazmat(living)) {
                    continue; // 穿着防化服完全免疫辐射
            }

                // 计算辐射伤害
                float distanceFactor = (float) (1.0 - distance / zone.radius);
                float baseDamage = zone.power / 500.0f; // 基于威力的基础伤害
                float damage = baseDamage * distanceFactor;

                // 应用辐射效果
                applyRadiationEffects(living, damage, distanceFactor);
            }
        }
    }

    /**
     * 应用辐射效果到生物
 */
    private static void applyRadiationEffects(LivingEntity living, float damage, float intensity) {
        Level level = living.level();

        // 计算实际伤害（考虑防护）
    float actualDamage = RadiationProtectionUtil.calculateRadiationDamage(living, damage);

        // 施加辐射效果
        int poisonDuration = (int) (100 * intensity); // 5秒 * 强度
        int poisonLevel = intensity > 0.7 ? 2 : (intensity > 0.4 ? 1 : 0);

        if (poisonDuration > 20) {
            living.addEffect(new MobEffectInstance(mio_icif_effects.RADIATION, poisonDuration, poisonLevel, false, true));
        }

        // 高强度辐射施加虚弱效果
    if (intensity > 0.5) {
            int weaknessDuration = (int) (60 * intensity);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessDuration, 0, false, true));
        }

        // 极高强度辐射施加凋零效果
        if (intensity > 0.8) {
            int witherDuration = (int) (40 * intensity);
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, witherDuration, 0, false, true));
        }

        // 造成伤害
        if (actualDamage > 0.0F) {
            living.hurt(new DamageSource(
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(com.singularity_iteration.mio_icif.event.mio_icif_DamageTypes.RADIATION),
                null, null), actualDamage);
        }
    }

    /**
     * 清理过期的辐射区域
 */
    private static void cleanupExpiredZones() {
        Iterator<Map.Entry<UUID, RadiationZone>> iterator = RADIATION_ZONES.entrySet().iterator();
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<UUID, RadiationZone> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            Singularity_Iteration.LOGGER.info("[核弹辐射] 清理了{} 个过期的辐射区域", cleanedCount);
        }
    }

    /**
     * 获取辐射区域的剩余时间（毫秒）
 * @param zoneId 辐射区域ID
     * @return 剩余时间，如果不存在返回0
     */
    public static long getZoneRemainingTime(UUID zoneId) {
        RadiationZone zone = RADIATION_ZONES.get(zoneId);
        return zone != null ? zone.getRemainingTime() : 0;
    }

    /**
     * 清除所有辐射区域（用于调试或重置）
     */
    public static void clearAllZones() {
        RADIATION_ZONES.clear();
        Singularity_Iteration.LOGGER.info("[核弹辐射] 已清除所有辐射区域");
    }
}