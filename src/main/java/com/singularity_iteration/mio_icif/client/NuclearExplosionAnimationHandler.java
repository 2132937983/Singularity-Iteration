package com.singularity_iteration.mio_icif.client;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import com.singularity_iteration.mio_icif.particle.mio_icif_Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 核爆炸蘑菇云动画处理器（客户端）
 * 基于 HBM Nuclear Tech 的蘑菇云设计理念:
 * - ParticleMukeCloud: 使用纹理动画的蘑菇云粒子
 * - EntityNukeTorex: 环状对流模拟的复杂蘑菇云实体
 * 
 * 本实现简化了 HBM 的复杂性，但保留了核心视觉效果:
 * 1. 初始火球阶段 (0-15 tick)
 * 2. 烟柱形成阶段 (10-60 tick) - 双曲线形状
 * 3. 菌盖形成阶段 (50-150 tick) - 环状对流
 * 4. 成熟稳定阶段 (140-400 tick) - 蘑菇云完全展开
 * 5. 消散阶段 (390-1700 tick) - 逐渐灰化消散
 */
@SuppressWarnings("null")
public class NuclearExplosionAnimationHandler {

    private static final List<AnimationInstance> activeAnimations = new ArrayList<>();
    private static boolean registered = false;
    @SuppressWarnings("unused")
    private static final Random RANDOM = new Random();

    /**
     * 动画实例
     * 参考HBM 的EntityNukeTorex 设计
     */
    private static class AnimationInstance {
        final double centerX;
        final double centerY;
        final double centerZ;
        final int explosionRadius;
        int currentTick;
        final int maxTick = 3600; // 延长到3 分钟 (3600 ticks)
        
        // 蘑菇云形态参数(参考HBM EntityNukeTorex)
        double coreHeight = 3.0;      // 核心高度
        @SuppressWarnings("unused")
        double convectionHeight = 3.0; // 对流高度
        double torusWidth = 3.0;       // 圆环宽度 (菌盖半径)
        double rollerSize = 1.0;       // 滚轴大小 (菌盖厚度)
        
        // 最后生成的 Y 坐标 (用于追踪地面高度)
        double lastSpawnY = -1;

        AnimationInstance(double centerX, double centerY, double centerZ, int explosionRadius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.explosionRadius = explosionRadius;
            this.currentTick = 0;
        }
    }

    /**
     * 注册事件处理器
     */
    public static void register() {
        if (!registered) {
            NeoForge.EVENT_BUS.register(NuclearExplosionAnimationHandler.class);
            registered = true;
        }
    }

    /**
     * 获取粒子数量倍率
     * 从配置文件读取
     */
    private static double getParticleMultiplier() {
        return Singularity_Iteration_Config.MUSHROOM_CLOUD_PARTICLE_MULTIPLIER.get();
    }

    /**
     * 根据倍率计算实际粒子数量
     * @param baseCount 基础粒子数量
     * @return 实际生成的粒子数量
     */
    private static int calculateParticleCount(int baseCount) {
        double multiplier = getParticleMultiplier();
        if (multiplier <= 0) return 0;
        return (int) (baseCount * multiplier);
    }

    /**
     * 开始一个新的蘑菇云动画
     */
    public static void startAnimation(double centerX, double centerY, double centerZ, int explosionRadius) {
        register(); // 确保已注册
activeAnimations.add(new AnimationInstance(centerX, centerY, centerZ, explosionRadius));
    }

    /**
     * 客户端tick 事件处理
     * 每tick 更新所有活跃的动画
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (activeAnimations.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return;

        ClientLevel level = minecraft.level;

        Iterator<AnimationInstance> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            AnimationInstance anim = iterator.next();
            
            // 生成当前 tick 的粒子
    spawnMushroomCloudFrame(level, anim);
            
            // 更新 tick
            anim.currentTick++;
            
            // 动画结束，移除
    if (anim.currentTick >= anim.maxTick) {
                iterator.remove();
            }
        }
    }

    /**
     * 生成蘑菇云的一帧
     * 参考HBM 的EntityNukeTorex 设计，实现环状对流模拟
     */
    private static void spawnMushroomCloudFrame(ClientLevel level, AnimationInstance anim) {
        @SuppressWarnings("unused")
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        @SuppressWarnings("unused")
        double centerZ = anim.centerZ;
        int tick = anim.currentTick;
        int explosionRadius = anim.explosionRadius;
        
        // 更新地面追踪高度 (类似 HBM 的lastSpawnY)
        if (anim.lastSpawnY < 0) {
            anim.lastSpawnY = centerY;
        }
        
        // 更新蘑菇云形态参数(类似 HBM EntityNukeTorex.onUpdate)
        double simSpeed = getSimulationSpeed(anim);
        anim.coreHeight += 0.15 / simSpeed;
        anim.torusWidth += 0.05 / simSpeed;
        anim.rollerSize = anim.torusWidth * 0.35;
        anim.convectionHeight = anim.coreHeight + anim.rollerSize;
        
        // 阶段 1: 初始火球 (0-20 tick)
        if (tick < 20) {
            spawnFireballPhase(level, anim, tick, explosionRadius);
        }
        
        // 阶段 2: 烟柱形成 (10-80 tick) - 双曲线形状，类似 HBM 的stem formation
        if (tick >= 10 && tick < 80) {
            spawnStemFormationPhase(level, anim, tick, explosionRadius);
        }
        
        // 阶段 3: 菌盖形成 (60-200 tick) - 环状对流，参考HBM 的torus convection
        if (tick >= 60 && tick < 200) {
            spawnCapFormationPhase(level, anim, tick, explosionRadius);
        }
        
        // 阶段 4: 成熟稳定 (180-2400 tick) - 完全展开的蘑菇云，保持很长时间
if (tick >= 180 && tick < 2400) {
            spawnMatureCloudPhase(level, anim, tick, explosionRadius);
        }
        
        // 阶段 5: 消散 (2400-3600 tick) - 逐渐灰化
        if (tick >= 2400) {
            spawnDissipationPhase(level, anim, tick, explosionRadius);
        }
    }
    
    /**
     * 计算模拟速度 (类似 HBM EntityNukeTorex.getSimulationSpeed)
     */
    private static double getSimulationSpeed(AnimationInstance anim) {
        int lifetime = anim.maxTick;
        int simSlow = lifetime / 4;
        int simStop = lifetime / 2;
        int life = anim.currentTick;
        
        if (life > simStop) {
            return 0.0;
        }
        
        if (life > simSlow) {
            return 1.0 - ((double)(life - simSlow) / (double)(simStop - simSlow));
        }
        
        return 1.0;
    }

    /**
     * 阶段 1：初始火球和底部烟尘
     * 参考HBM 的ParticleMukeFlash 和ParticleMukeCloud 设计
     * 巨大的火球从地面升起，裹挟着大量黑色烟尘
     */
    private static void spawnFireballPhase(ClientLevel level, AnimationInstance anim, int tick, int explosionRadius) {
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        double centerZ = anim.centerZ;
        double progress = tick / 20.0;
        RandomSource rand = level.random;
        
        // 火球核心参数
        double fireballRadius = explosionRadius * 0.3 * (0.5 + progress * 0.5);
        double fireballHeight = progress * 8;
        
        // 火球核心 - 高密度粒子(参考HBM 的ParticleMukeCloud 生成方式)
        int coreParticles = calculateParticleCount(500);
        for (int i = 0; i < coreParticles; i++) {
            // 球坐标随机分布
    double theta = rand.nextDouble() * Math.PI * 2;
            double phi = rand.nextDouble() * Math.PI;
            double r = fireballRadius * Math.pow(rand.nextDouble(), 0.33);
            
            double x = centerX + r * Math.sin(phi) * Math.cos(theta);
            double y = centerY + fireballHeight + r * Math.cos(phi);
            double z = centerZ + r * Math.sin(phi) * Math.sin(theta);
            
            // 火球内部使用深色烟雾 + 发光粒子混合
            if (rand.nextFloat() < 0.7) {
                spawnHeavySmoke(level, x, y, z, 0.4);
            } else {
                spawnGlowParticle(level, x, y, z, 0.6);
            }
        }
        
        // 底部烟尘柱开始形成(双曲线形状，参考HBM 的stem formation)
        double downwardDepth = explosionRadius * 0.3;
        int stemParticles = calculateParticleCount(300);
        for (int i = 0; i < stemParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (fireballHeight * 2 + downwardDepth);
            
            // 双曲线形状：底部宽，向上收窄 (类似 HBM 的设计)
            double normalizedHeight = (h + downwardDepth) / (fireballHeight * 2 + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = fireballRadius * 0.15 + (fireballRadius * 0.5 - fireballRadius * 0.15) * hyperbolicFactor;
            
            double r = currentRadius * Math.sqrt(rand.nextDouble());
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnHeavySmoke(level, x, y, z, 0.35);
        }
        
        // 火球顶部开始形成菌盖雏形(为下一阶段做准备)
        int capParticles = calculateParticleCount(200);
        for (int i = 0; i < capParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = rand.nextDouble() * Math.PI * 0.5; // 上半球
    double rH = fireballRadius * (0.5 + progress * 0.3) * Math.pow(rand.nextDouble(), 0.33);
            double rV = fireballRadius * 0.3 * Math.pow(rand.nextDouble(), 0.33);
            
            double x = centerX + rH * Math.cos(theta);
            double y = centerY + fireballHeight + fireballRadius * 0.5 + rV * Math.cos(phi);
            double z = centerZ + rH * Math.sin(theta);
            
            spawnMediumSmoke(level, x, y, z, 0.3);
        }
    }
    
    /**
     * 生成发光粒子 (用于火球和高温效果)
     */
    private static void spawnGlowParticle(ClientLevel level, double x, double y, double z, double density) {
        if (level.random.nextDouble() > density) return;
        
        double vx = (level.random.nextDouble() - 0.5) * 0.3;
        double vy = level.random.nextDouble() * 0.2;
        double vz = (level.random.nextDouble() - 0.5) * 0.3;
        
        level.addParticle(mio_icif_Particles.NUCLEAR_GLOW.get(), x, y, z, vx, vy, vz);
    }

    /**
     * 阶段 2：烟柱（茎部）形成
     * 参考HBM 的双曲线形状设计，粗壮的黑色柱状烟云
     */
    private static void spawnStemFormationPhase(ClientLevel level, AnimationInstance anim, int tick, int explosionRadius) {
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        double centerZ = anim.centerZ;
        int progress = tick - 10;
        double stemHeight = 10 + progress * 1.2;
        double maxStemRadius = explosionRadius * 0.25;
        double minStemRadius = explosionRadius * 0.08;
        double downwardDepth = explosionRadius * 0.5;
        RandomSource rand = level.random;
        
        // 主烟柱- 双曲线形状(类似 HBM 的设计)
        int mainParticles = calculateParticleCount(800);
        for (int i = 0; i < mainParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (stemHeight + downwardDepth);
            
            // 双曲线形状：底部宽，向上收窄
            double normalizedHeight = (h + downwardDepth) / (stemHeight + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = minStemRadius + (maxStemRadius - minStemRadius) * hyperbolicFactor;
            
            double r = currentRadius * Math.sqrt(rand.nextDouble());
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnHeavySmoke(level, x, y, z, 0.4);
        }
        
        // 烟柱边缘弥散效果
        int edgeParticles = calculateParticleCount(200);
        for (int i = 0; i < edgeParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (stemHeight + downwardDepth);
            
            double normalizedHeight = (h + downwardDepth) / (stemHeight + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = minStemRadius + (maxStemRadius - minStemRadius) * hyperbolicFactor;
            
            double r = currentRadius * (0.8 + rand.nextDouble() * 0.4);
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnMediumSmoke(level, x, y, z, 0.25);
        }
        
        // 烟柱顶部翻卷效果 (过渡区)
        double capBaseY = centerY + stemHeight;
        double topRadius = minStemRadius;
        int topParticles = calculateParticleCount(300);
        for (int i = 0; i < topParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double r = topRadius * (0.5 + rand.nextDouble());
            double y = capBaseY + rand.nextDouble() * 3 - 1.5;
            
            double x = centerX + Math.cos(angle) * r;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnHeavySmoke(level, x, y, z, 0.35);
        }
    }

    /**
     * 阶段 3：菌盖（云团）膨胀形成
     * 参考HBM 的环状对流设计，顶部急剧膨胀的巨大伞状云团
     */
    private static void spawnCapFormationPhase(ClientLevel level, AnimationInstance anim, int tick, int explosionRadius) {
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        double centerZ = anim.centerZ;
        int progress = tick - 60;
        double stemHeight = 20 + progress * 0.8;
        double capCenterY = centerY + stemHeight;
        double capRadius = explosionRadius * 0.5 + progress * 0.8;
        double capHeight = 15 + progress * 0.5;
        RandomSource rand = level.random;
        
        // 菌盖主体 - 半球形(类似 HBM 的torus convection)
        int mainCapParticles = calculateParticleCount(600);
        for (int i = 0; i < mainCapParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = rand.nextDouble() * Math.PI * 0.5; // 上半球
    
            double rH = capRadius * Math.pow(rand.nextDouble(), 0.33);
            double rV = capHeight * Math.pow(rand.nextDouble(), 0.33);
            
            double x = centerX + rH * Math.sin(phi) * Math.cos(theta);
            double y = capCenterY + rV * Math.cos(phi);
            double z = centerZ + rH * Math.sin(phi) * Math.sin(theta);
            
            spawnHeavySmoke(level, x, y, z, 0.4);
        }
        
        // 菌盖下半部分凹陷效果 (向内卷曲)
        int bottomCapParticles = calculateParticleCount(200);
        for (int i = 0; i < bottomCapParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double normalizedDist = rand.nextDouble();
            double rH = capRadius * (0.3 + normalizedDist * 0.9);
            double yOffset = -capHeight * 0.3 - normalizedDist * capHeight * 0.5;
            double y = capCenterY + yOffset;
            
            double x = centerX + rH * Math.cos(theta);
            double z = centerZ + rH * Math.sin(theta);
            
            spawnMediumSmoke(level, x, y, z, 0.3);
        }
        
        // 菌盖边缘蓬松效果
        int edgeCapParticles = calculateParticleCount(250);
        for (int i = 0; i < edgeCapParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double rH = capRadius * (0.85 + rand.nextDouble() * 0.4);
            double rV = capHeight * (0.3 + rand.nextDouble() * 0.6);
            
            double x = centerX + rH * Math.cos(theta);
            double y = capCenterY + rV * (0.5 + rand.nextDouble() * 0.4);
            double z = centerZ + rH * Math.sin(theta);
            
            spawnLightSmoke(level, x, y, z, 0.25);
        }
        
        // 持续生成烟柱 (保持连接)
        int stemParticles = calculateParticleCount(150);
        double maxStemRadius = explosionRadius * 0.25;
        double minStemRadius = explosionRadius * 0.08;
        double downwardDepth = explosionRadius * 0.5;
        
        for (int i = 0; i < stemParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (stemHeight + downwardDepth);
            
            double normalizedHeight = (h + downwardDepth) / (stemHeight + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = minStemRadius + (maxStemRadius - minStemRadius) * hyperbolicFactor;
            
            double r = currentRadius * Math.sqrt(rand.nextDouble());
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnHeavySmoke(level, x, y, z, 0.35);
        }
    }

    /**
     * 阶段 4：蘑菇云完全成型 (成熟稳定阶段)
     * 参考HBM 的EntityNukeTorex 成熟阶段设计
     */
    private static void spawnMatureCloudPhase(ClientLevel level, AnimationInstance anim, int tick, int explosionRadius) {
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        double centerZ = anim.centerZ;
        int progress = tick - 180;
        double stemHeight = 30 + progress * 0.2;
        double maxStemRadius = explosionRadius * 0.25;
        double minStemRadius = explosionRadius * 0.08;
        double capCenterY = centerY + stemHeight;
        double capRadius = explosionRadius * 1.0 + progress * 0.3;
        double capHeight = 25 + progress * 0.15;
        RandomSource rand = level.random;
        
        // 菌盖持续膨胀 - 半球形主体
int mainParticles = calculateParticleCount(500);
        for (int i = 0; i < mainParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = rand.nextDouble() * Math.PI * 0.5;
            double rH = capRadius * Math.pow(rand.nextDouble(), 0.33);
            double rV = capHeight * Math.pow(rand.nextDouble(), 0.33);
            
            double x = centerX + rH * Math.sin(phi) * Math.cos(theta);
            double y = capCenterY + rV * Math.cos(phi);
            double z = centerZ + rH * Math.sin(phi) * Math.sin(theta);
            
            // 混合不同颜色的粒子
    double particleRand = rand.nextDouble();
            if (particleRand < 0.5) {
                spawnHeavySmoke(level, x, y, z, 0.35);
            } else if (particleRand < 0.8) {
                spawnMediumSmoke(level, x, y, z, 0.3);
            } else {
                spawnLightSmoke(level, x, y, z, 0.25);
            }
        }
        
        // 菌盖下半部分凹陷效果
        int bottomParticles = calculateParticleCount(150);
        for (int i = 0; i < bottomParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double normalizedDist = rand.nextDouble();
            double rH = capRadius * (0.3 + normalizedDist * 0.9);
            double yOffset = -capHeight * 0.3 - normalizedDist * capHeight * 0.5;
            double y = capCenterY + yOffset;
            
            double x = centerX + rH * Math.cos(theta);
            double z = centerZ + rH * Math.sin(theta);
            
            spawnMediumSmoke(level, x, y, z, 0.28);
        }
        
        // 菌盖边缘弥散效果
        int edgeParticles = calculateParticleCount(200);
        for (int i = 0; i < edgeParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double rH = capRadius * (0.85 + rand.nextDouble() * 0.4);
            double rV = capHeight * (0.3 + rand.nextDouble() * 0.6);
            
            double x = centerX + rH * Math.cos(theta);
            double y = capCenterY + rV * (0.5 + rand.nextDouble() * 0.4);
            double z = centerZ + rH * Math.sin(theta);
            
            spawnLightSmoke(level, x, y, z, 0.22);
        }
        
        // 持续生成烟柱
        int stemParticles = calculateParticleCount(120);
        double downwardDepth = explosionRadius * 0.5;
        
        for (int i = 0; i < stemParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (stemHeight + downwardDepth);
            
            double normalizedHeight = (h + downwardDepth) / (stemHeight + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = minStemRadius + (maxStemRadius - minStemRadius) * hyperbolicFactor;
            
            double r = currentRadius * Math.sqrt(rand.nextDouble());
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnHeavySmoke(level, x, y, z, 0.3);
        }
    }

    /**
     * 阶段 5：消散
     * 参考HBM 的粒子消散逻辑，逐渐灰化
     */
    private static void spawnDissipationPhase(ClientLevel level, AnimationInstance anim, int tick, int explosionRadius) {
        double centerX = anim.centerX;
        double centerY = anim.centerY;
        double centerZ = anim.centerZ;
        int progress = tick - 480;
        double dissipationFactor = 1.0 - Math.min(1.0, progress / 1200.0);
        RandomSource rand = level.random;
        
        double stemHeight = 50 + progress * 0.015;
        double maxStemRadius = explosionRadius * 0.25;
        double minStemRadius = explosionRadius * 0.08;
        double capCenterY = centerY + stemHeight;
        double capRadius = explosionRadius * 1.4 + progress * 0.02;
        double capHeight = 35 + progress * 0.01;
        
        // 消散的菌盖
int capParticles = (int) (calculateParticleCount(400) * dissipationFactor);
        for (int i = 0; i < capParticles; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = rand.nextDouble() * Math.PI * 0.5;
            double rH = capRadius * Math.pow(rand.nextDouble(), 0.33);
            double rV = capHeight * Math.pow(rand.nextDouble(), 0.33);
            
            double x = centerX + rH * Math.sin(phi) * Math.cos(theta);
            double y = capCenterY + rV * Math.cos(phi);
            double z = centerZ + rH * Math.sin(phi) * Math.sin(theta);
            
            // 逐渐过渡到浅色
    double particleRand = rand.nextDouble();
            if (particleRand < dissipationFactor * 0.3) {
                spawnHeavySmoke(level, x, y, z, 0.2 * dissipationFactor);
            } else if (particleRand < dissipationFactor * 0.7) {
                spawnMediumSmoke(level, x, y, z, 0.18 * dissipationFactor);
            } else {
                spawnLightSmoke(level, x, y, z, 0.15 * dissipationFactor);
            }
        }
        
        // 消散的烟柱
double downwardDepth = explosionRadius * 0.5;
        int stemParticles = (int) (calculateParticleCount(100) * dissipationFactor);
        
        for (int i = 0; i < stemParticles; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double h = -downwardDepth + rand.nextDouble() * (stemHeight + downwardDepth);
            
            double normalizedHeight = (h + downwardDepth) / (stemHeight + downwardDepth);
            double hyperbolicFactor = Math.sqrt(1.0 - 0.7 * normalizedHeight * normalizedHeight);
            double currentRadius = minStemRadius + (maxStemRadius - minStemRadius) * hyperbolicFactor;
            
            double r = currentRadius * Math.sqrt(rand.nextDouble());
            
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + h;
            double z = centerZ + Math.sin(angle) * r;
            
            spawnMediumSmoke(level, x, y, z, 0.15 * dissipationFactor);
        }
    }

    /**
     * 生成厚重的黑色烟雾(用于主体)
     * 参考HBM 的ParticleMukeCloud 设计，使用深色厚重烟雾
     */
    private static void spawnHeavySmoke(ClientLevel level, double x, double y, double z, double velocityY) {
        double rand = level.random.nextDouble();
        double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

        if (rand < 0.6) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_DARK.get(), x + offsetX, y, z + offsetZ, 0, velocityY, 0);
        } else if (rand < 0.9) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_MEDIUM.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 0.9, 0);
        } else {
            level.addParticle(mio_icif_Particles.NUCLEAR_GLOW.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 1.2, 0);
        }
    }

    /**
     * 生成轻薄的灰色烟雾(用于边缘弥散)
     */
    private static void spawnLightSmoke(ClientLevel level, double x, double y, double z, double velocityY) {
        double rand = level.random.nextDouble();
        double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
        double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;

        if (rand < 0.5) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_LIGHT.get(), x + offsetX, y, z + offsetZ, 0, velocityY, 0);
        } else if (rand < 0.8) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_MEDIUM.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 0.8, 0);
        } else {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_DARK.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 0.7, 0);
        }
    }

    /**
     * 生成中等灰色烟雾 (用于过渡区域)
     */
    private static void spawnMediumSmoke(ClientLevel level, double x, double y, double z, double velocityY) {
        double rand = level.random.nextDouble();
        double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
        double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

        if (rand < 0.6) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_MEDIUM.get(), x + offsetX, y, z + offsetZ, 0, velocityY, 0);
        } else if (rand < 0.9) {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_DARK.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 0.9, 0);
        } else {
            level.addParticle(mio_icif_Particles.NUCLEAR_SMOKE_LIGHT.get(), x + offsetX, y, z + offsetZ, 0, velocityY * 0.8, 0);
        }
    }

}