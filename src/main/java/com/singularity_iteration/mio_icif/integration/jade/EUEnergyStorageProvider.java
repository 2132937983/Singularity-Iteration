package com.singularity_iteration.mio_icif.integration.jade;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.api.machine.IJadeDisplayDelegate;
import com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

/**
 * Jade EU 能量存储显示提供�? * 使用进度条样式显示EU能量，与FE能量显示一�? */
@SuppressWarnings("null")
public enum EUEnergyStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "eu_energy_storage");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        if (data.contains("Energy") && data.contains("MaxEnergy")) {
            long energy = data.getLong("Energy");
            long maxEnergy = data.getLong("MaxEnergy");

            if (maxEnergy > 0) {
                IElementHelper helper = IElementHelper.get();

                float ratio = (float) energy / (float) maxEnergy;

                String currentStr = String.valueOf(energy);
                String maxStr = String.valueOf(maxEnergy);

                currentStr = ChatFormatting.WHITE + currentStr;
                Component text = Component.translatable("jade.mio_icif.energy", currentStr, maxStr);

                ProgressStyle progressStyle = helper.progressStyle()
                        .color(0xFFC4B030, 0xFF8A7A20);
                tooltip.add(helper.progress(ratio, text, progressStyle, BoxStyle.getNestedBox(), true));
            }
        }

        if (data.contains("CableTier")) {
            String tierName = data.getString("CableTier");
            String tierDisplay = tierName;
            if (data.contains("PowerRating")) {
                long powerRating = data.getLong("PowerRating");
                tierDisplay = tierName + " (" + powerRating + " EU/t)";
            }
            Component tierText = Component.translatable("jade.mio_icif.cable_tier", tierDisplay);
            tooltip.add(tierText);
        }

        if (data.contains("DelegateTier")) {
            String delegateTierName = data.getString("DelegateTier");
            String delegateTierDisplay = delegateTierName;
            if (data.contains("DelegatePowerRating")) {
                long delegatePowerRating = data.getLong("DelegatePowerRating");
                delegateTierDisplay = delegateTierName + " (" + delegatePowerRating + " EU/t)";
            }
            Component delegateTierText = Component.translatable("jade.mio_icif.delegate_tier", delegateTierDisplay);
            tooltip.add(delegateTierText);
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        if (blockEntity instanceof IJadeDisplayDelegate delegate) {
            BlockEntity target = delegate.getJadeDisplayTarget();
            if (target != null) {
                IEUEnergyStorage targetStorage = accessor.getLevel().getCapability(
                        EUApi.SIDED, target.getBlockPos(), accessor.getSide());
                if (targetStorage != null) {
                    data.putLong("Energy", targetStorage.getAmount());
                    data.putLong("MaxEnergy", targetStorage.getCapacity());
                }
                if (target instanceof mio_icif_Energy_Block energyBlock) {
                    var tier = energyBlock.getEffectiveCableTier();
                    data.putString("CableTier", tier.getDisplayName());
                    data.putLong("PowerRating", tier.getPowerRating());
                }
                if (blockEntity instanceof mio_icif_Energy_Block delegateBlock) {
                    var delegateTier = delegateBlock.getEffectiveCableTier();
                    data.putString("DelegateTier", delegateTier.getDisplayName());
                    data.putLong("DelegatePowerRating", delegateTier.getPowerRating());
                }
                data.putBoolean("IsDisplayDelegate", true);
                return;
            }
        }

        IEUEnergyStorage energyStorage = accessor.getLevel().getCapability(
                EUApi.SIDED, blockEntity.getBlockPos(), accessor.getSide());

        if (energyStorage != null) {
            long energy = energyStorage.getAmount();
            long maxEnergy = energyStorage.getCapacity();

            data.putLong("Energy", energy);
            data.putLong("MaxEnergy", maxEnergy);
        }

        if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
            var tier = energyBlock.getEffectiveCableTier();
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putLong("PowerRating", tier.getPowerRating());
        } else if (blockEntity instanceof mio_icif_wire wire) {
            var tier = wire.getCableTier();
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putLong("PowerRating", tier.getPowerRating());
        } else if (blockEntity instanceof IGeneratorBlock generator) {
            var tier = generator.getCableTier();
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putLong("PowerRating", tier.getPowerRating());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}