package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.block.hugenerator.mio_icif_block_heat_source_fluid;
import com.miophas.singularity_iteration.common.block.kugenerator.mio_icif_Block_Stirling_Kinetic_Generator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin for Industrial Craft In Future.
 * 注册 EU、HU、KU 能量显示支持
 */
@WailaPlugin
@SuppressWarnings("null")
public class ICIFJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Register the EU energy data provider.
    registration.registerBlockDataProvider(
                EUEnergyStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // Register the HU heat data provider.
    registration.registerBlockDataProvider(
                HUHeatStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // Register the KU kinetic energy data provider.
    registration.registerBlockDataProvider(
                KUKineticStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // Register the dual fluid tank data provider.
    registration.registerBlockDataProvider(
                DualFluidStorageProvider.INSTANCE,
                BlockEntity.class
        );
        registration.registerBlockDataProvider(SteamTunnelProvider.INSTANCE,
                com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity.class);   // 0.1.10.39
        registration.registerBlockDataProvider(ReactorStatusProvider.INSTANCE,
                com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator.class);   // 0.1.10.39
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 0.1.10.39: the Steam Tunnel controller (no menu)
        registration.registerBlockComponent(SteamTunnelProvider.INSTANCE,
                com.miophas.singularity_iteration.common.block.generator.mio_icif_block_steam_tunnel.class);
        // 0.1.10.39: why a reactor makes nothing; fluid mode makes hot coolant, no EU
        registration.registerBlockComponent(ReactorStatusProvider.INSTANCE,
                com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator.class);
        // 注册 EU 能量客户端组件提供器
        registration.registerBlockComponent(
                EUEnergyStorageProvider.INSTANCE,
                Block.class
        );

        // 注册 HU 热能客户端组件提供器
        registration.registerBlockComponent(
                HUHeatStorageProvider.INSTANCE,
                Block.class
        );

        // 注册 KU 动能客户端组件提供器
        registration.registerBlockComponent(
                KUKineticStorageProvider.INSTANCE,
                Block.class
        );

        // 注册双流体槽客户端组件提供器（仅针对热交换机等特定方块）
        registration.registerBlockComponent(
                DualFluidStorageProvider.INSTANCE,
                mio_icif_block_heat_source_fluid.class
        );

        // 注册双流体槽客户端组件提供器（针对斯特林动能机）
        registration.registerBlockComponent(
                DualFluidStorageProvider.INSTANCE,
                mio_icif_Block_Stirling_Kinetic_Generator.class
        );

        // 注册双流体槽客户端组件提供器（针对核反应堆）
        registration.registerBlockComponent(
                DualFluidStorageProvider.INSTANCE,
                com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator.class
        );
    }
}


