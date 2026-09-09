package com.singularity_iteration.mio_icif.integration.jade;

import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_source_fluid;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Stirling_Kinetic_Generator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Industrial Craft In Future �?Jade 插件
 * 注册 EU、HU、KU 能量显示支持
 */
@WailaPlugin
@SuppressWarnings("null")
public class ICIFJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 注册 EU 能量数据提供�
    registration.registerBlockDataProvider(
                EUEnergyStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // 注册 HU 热能数据提供�
    registration.registerBlockDataProvider(
                HUHeatStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // 注册 KU 动能数据提供�
    registration.registerBlockDataProvider(
                KUKineticStorageProvider.INSTANCE,
                BlockEntity.class
        );

        // 注册双流体槽数据提供�
    registration.registerBlockDataProvider(
                DualFluidStorageProvider.INSTANCE,
                BlockEntity.class
        );
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
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
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator.class
        );
    }
}


