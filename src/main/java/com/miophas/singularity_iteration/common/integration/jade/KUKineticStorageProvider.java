package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Kinetic_Generator_elc;
import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Stirling_Kinetic_Generator;
import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Water_Kinetic_Generator;
import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticStorage;
import com.miophas.singularity_iteration.core.api.kinetic.KUCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade provider that shows KU kinetic storage.
 * It follows the EUEnergyStorageProvider implementation.
 * It shows KU information for storage blocks and generator blocks.
 */
@SuppressWarnings("null")
public enum KUKineticStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "ku_kinetic_storage");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        
        // Check for the generator type (shows the output rate).
    if (data.contains("IsGenerator") && data.getBoolean("IsGenerator")) {
            int output = data.getInt("CurrentOutput");
            tooltip.add(Component.translatable("jade.mio_icif.kinetic.generating", output));
            return;
        }
        
        // Show the normal storage type.
    if (data.contains("Kinetic") && data.contains("MaxKinetic")) {
            int kinetic = data.getInt("Kinetic");
            int maxKinetic = data.getInt("MaxKinetic");

            if (maxKinetic > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.kinetic", kinetic, maxKinetic));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        // Check for a wind kinetic generator.
    if (blockEntity instanceof mio_icif_Wind_Kinetic_Generator windGen) {
            data.putBoolean("IsGenerator", true);
            data.putInt("CurrentOutput", windGen.getCurrentKineticOutput());
            return;
        }
        
        // Check for a water kinetic generator.
    if (blockEntity instanceof mio_icif_Water_Kinetic_Generator waterGen) {
            data.putBoolean("IsGenerator", true);
            data.putInt("CurrentOutput", waterGen.getCurrentKineticOutput());
            return;
        }
        
        // 检查是否是电力动能量
    if (blockEntity instanceof mio_icif_Kinetic_Generator_elc elcGen) {
            data.putBoolean("IsGenerator", true);
            data.putInt("CurrentOutput", elcGen.getKineticGeneration());
            return;
        }
        
        // 检查是否是斯特林动能发生机
        if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator stirlingGen) {
            data.putBoolean("IsGenerator", true);
            // 斯特林动能发生机每次操作产生固定12 KU，如果正在工作就显示12
            int output = stirlingGen.isWorking() ? mio_icif_Stirling_Kinetic_Generator.KU_PER_OPERATION : 0;
            data.putInt("CurrentOutput", output);
            return;
        }

        // 普通动能存储方法
    IKineticStorage kineticStorage = accessor.getLevel().getCapability(KUCapabilities.KineticStorage.BLOCK, blockEntity.getBlockPos(), accessor.getSide());

        if (kineticStorage != null) {
            long kinetic = kineticStorage.getKineticStored();
            long maxKinetic = kineticStorage.getMaxKineticStored();

            data.putLong("Kinetic", kinetic);
            data.putLong("MaxKinetic", maxKinetic);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}