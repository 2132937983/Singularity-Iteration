package com.miophas.singularity_iteration.common.client.particle;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.particle.mio_icif_Particles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * 粒子提供者注册（客户端）
 */
@SuppressWarnings({"null", "removal"})
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class mio_icif_ParticleProviders {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // 注册核爆烟雾粒子提供
        event.registerSpriteSet(mio_icif_Particles.NUCLEAR_SMOKE_DARK.get(), NuclearSmokeParticle.DarkProvider::new);
        event.registerSpriteSet(mio_icif_Particles.NUCLEAR_SMOKE_MEDIUM.get(), NuclearSmokeParticle.MediumProvider::new);
        event.registerSpriteSet(mio_icif_Particles.NUCLEAR_SMOKE_LIGHT.get(), NuclearSmokeParticle.LightProvider::new);
        event.registerSpriteSet(mio_icif_Particles.NUCLEAR_GLOW.get(), NuclearSmokeParticle.GlowProvider::new);
    }
}

