package com.singularity_iteration.mio_icif.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 核爆烟雾粒子 - 自定义大粒子效果
 * 使用原版生物死亡粒子样式，但更大、带颜色
 */
@SuppressWarnings("null")
public class NuclearSmokeParticle extends TextureSheetParticle {

    private float baseScale = 2.0f;
    private final float rotationSpeed;
    @SuppressWarnings("unused")
    private float rotation;
    
    // 烟柱上升速度（再减半）
private static final double RISE_SPEED = 0.04;
    // 扩散速度（再减半）
private static final double SPREAD_SPEED = 0.01;
    
    protected NuclearSmokeParticle(ClientLevel level, double x, double y, double z, 
                                   double vx, double vy, double vz, 
                                   NuclearSmokeType type) {
        super(level, x, y, z, vx, vy, vz);
        
        // 设置粒子基础属性
    this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        // 根据类型设置颜色（现实核爆蘑菇云颜色）
    switch (type) {
            case DARK -> {
                // 深黑色 - 烟柱主体（放射性尘埃和地面烟尘）
            // RGB: 25, 25, 25 (深灰色)
                this.setColor(0.10f, 0.10f, 0.10f);
                this.alpha = 0.95f;
                this.baseScale = 6.0f + level.random.nextFloat() * 4.0f; // 更大，填满空间
            this.lifetime = 1200 + level.random.nextInt(800); // 持续更久 (40-66秒)
            }
            case MEDIUM -> {
                // 中灰色 - 过渡层
            // RGB: 80, 80, 80
                this.setColor(0.31f, 0.31f, 0.31f);
                this.alpha = 0.85f;
                this.baseScale = 5.0f + level.random.nextFloat() * 3.0f; // 更大
                this.lifetime = 1000 + level.random.nextInt(600); // 33-53秒
        }
            case LIGHT -> {
                // 浅灰色 - 菌盖边缘弥散
                // RGB: 140, 140, 140
                this.setColor(0.55f, 0.55f, 0.55f);
                this.alpha = 0.70f;
                this.baseScale = 4.0f + level.random.nextFloat() * 2.0f; // 更大
                this.lifetime = 800 + level.random.nextInt(400); // 26-40秒
        }
            case GLOW -> {
                // 发光效果 - 橙红色高温余晖
            // RGB: 255, 100, 30
                this.setColor(1.0f, 0.39f, 0.12f);
                this.alpha = 0.8f;
                this.baseScale = 3.0f + level.random.nextFloat() * 1.0f; // 更大
                this.lifetime = 200 + level.random.nextInt(100); // 6-10秒
        }
        }
        
        // 初始速度（再减半）
    this.xd = vx * 0.25;
        this.yd = vy + RISE_SPEED * 0.25 * (0.8 + level.random.nextDouble() * 0.4);
        this.zd = vz * 0.25;
        
        // 旋转效果
        this.rotation = level.random.nextFloat() * 360f;
        this.rotationSpeed = (level.random.nextFloat() - 0.5f) * 2f;
        
        // 设置粒子大小
        this.quadSize = baseScale;
        
        // 重力影响很小（烟雾上升）
        this.gravity = -0.02f;
        
        // 无碰撞
    this.hasPhysics = false;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 更新旋转
        this.rotation += rotationSpeed;
        
        // 随时间变化
    float lifeRatio = (float) this.age / this.lifetime;
        
 // 大小随时间变化：先快速膨胀，然后慢扩大，最后消散时缩小
        if (lifeRatio < 0.1f) {
            // 初始快速膨胀
            float expandProgress = lifeRatio / 0.1f;
            this.quadSize = baseScale * (1.0f + expandProgress * 0.5f);
        } else if (lifeRatio < 0.7f) {
 // 稳定期缓慢扩大
        float stableProgress = (lifeRatio - 0.1f) / 0.6f;
            this.quadSize = baseScale * (1.5f + stableProgress * 0.5f);
        } else {
            // 消散期缩小并变透明
            float fadeProgress = (lifeRatio - 0.7f) / 0.3f;
            this.quadSize = baseScale * (2.0f - fadeProgress * 1.5f);
            this.alpha = Math.max(0, this.alpha - 0.01f);
        }
        
        // 上升速度逐渐减慢（更慢）
        this.yd *= 0.998;
        
        // 水平扩散（蘑菇云横向膨胀，更慢）
        double dx = this.x - this.xo;
        double dz = this.z - this.zo;
        double distFromCenter = Math.sqrt(dx * dx + dz * dz);
        if (distFromCenter < 50) {
            this.xd += dx * SPREAD_SPEED * 0.005;
            this.zd += dz * SPREAD_SPEED * 0.005;
        }
        
        // 添加湍流效果（更弱）
        this.xd += (level.random.nextDouble() - 0.5) * 0.005;
        this.zd += (level.random.nextDouble() - 0.5) * 0.005;
        
        // 阻尼（更弱，让粒子运动更持久）
    this.xd *= 0.99;
        this.zd *= 0.99;
    }
    
    @Override
    public float getQuadSize(float partialTick) {
        return this.quadSize;
    }
    
    @Override
    protected int getLightColor(float partialTick) {
        // 发光粒子自发光，其他粒子正常光照
        if (this.rCol > 0.8f && this.gCol < 0.5f) {
            // 发光粒子
            return 15728880; // 最大亮度
    }
        return super.getLightColor(partialTick);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        // 使用半透明渲染，支持alpha混合
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    /**
     * 烟雾类型枚举
     */
    public enum NuclearSmokeType {
        DARK,    // 深黑色 - 烟柱主体
        MEDIUM,  // 中灰色 - 过渡层
    LIGHT,   // 浅灰色 - 边缘弥散
        GLOW     // 发光 - 高温余辉
    }
    
    /**
     * 深色烟雾粒子提供者
 */
    public static class DarkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public DarkProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                       double x, double y, double z, 
                                       double vx, double vy, double vz) {
            NuclearSmokeParticle particle = new NuclearSmokeParticle(level, x, y, z, vx, vy, vz, NuclearSmokeType.DARK);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
    
    /**
     * 中等烟雾粒子提供者
 */
    public static class MediumProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public MediumProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                       double x, double y, double z, 
                                       double vx, double vy, double vz) {
            NuclearSmokeParticle particle = new NuclearSmokeParticle(level, x, y, z, vx, vy, vz, NuclearSmokeType.MEDIUM);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
    
    /**
     * 浅色烟雾粒子提供者
 */
    public static class LightProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public LightProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                       double x, double y, double z, 
                                       double vx, double vy, double vz) {
            NuclearSmokeParticle particle = new NuclearSmokeParticle(level, x, y, z, vx, vy, vz, NuclearSmokeType.LIGHT);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
    
    /**
     * 发光粒子提供者
 */
    public static class GlowProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        
        public GlowProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                       double x, double y, double z, 
                                       double vx, double vy, double vz) {
            NuclearSmokeParticle particle = new NuclearSmokeParticle(level, x, y, z, vx, vy, vz, NuclearSmokeType.GLOW);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
}