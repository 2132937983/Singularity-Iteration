package com.singularity_iteration.mio_icif.particle;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 自定义粒子类型注册? */
@SuppressWarnings("null")
public class mio_icif_Particles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, Singularity_Iteration.MOD_ID);

    // 核爆烟雾粒子 - 深色厚重烟雾（烟柱主体）
    public static final Supplier<SimpleParticleType> NUCLEAR_SMOKE_DARK = PARTICLE_TYPES.register(
        "nuclear_smoke_dark",
        () -> new SimpleParticleType(true)
    );

    // 核爆烟雾粒子 - 中等灰色烟雾（过渡区域
public static final Supplier<SimpleParticleType> NUCLEAR_SMOKE_MEDIUM = PARTICLE_TYPES.register(
        "nuclear_smoke_medium",
        () -> new SimpleParticleType(true)
    );

    // 核爆烟雾粒子 - 浅色弥散烟雾（菌盖边缘）
    public static final Supplier<SimpleParticleType> NUCLEAR_SMOKE_LIGHT = PARTICLE_TYPES.register(
        "nuclear_smoke_light",
        () -> new SimpleParticleType(true)
    );

    // 核爆发光粒子 - 模拟高温余辉
    public static final Supplier<SimpleParticleType> NUCLEAR_GLOW = PARTICLE_TYPES.register(
        "nuclear_glow",
        () -> new SimpleParticleType(true)
    );

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}


