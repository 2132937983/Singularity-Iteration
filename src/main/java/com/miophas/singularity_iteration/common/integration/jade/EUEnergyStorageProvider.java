package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.machine.IJadeDisplayDelegate;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import com.miophas.singularity_iteration.core.api.machine.IEnergyContainerBlock;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
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

                String currentStr = ChatFormatting.WHITE + formatEnergy(energy) + " EU";
                String maxStr = formatEnergy(maxEnergy) + " EU";

                Component text = Component.translatable("jade.mio_icif.energy", currentStr, maxStr);

                ProgressStyle progressStyle = helper.progressStyle()
                        .color(0xFF1FA2FF, 0xFF11598C);
                tooltip.add(helper.progress(ratio, text, progressStyle, BoxStyle.getNestedBox(), true));
            }
        }

        if (data.contains("OutputPower")) {
            tooltip.add(Component.translatable("jade.mio_icif.output_power", data.getLong("OutputPower")));
        }

        if (data.contains("WorkPower")) {
            tooltip.add(Component.translatable("jade.mio_icif.work_power", data.getLong("WorkPower")));
        }

        if (data.contains("CableTier")) {
            // 优先使用"能量等级"编号显示（LV = 能量等级1，MV = 能量等级2 ……），
            // 若服务端未同步等级编号则回退为等级名称。
            String tierDisplay = data.contains("TierLevel")
                    ? String.valueOf(data.getInt("TierLevel"))
                    : data.getString("CableTier");
            if (data.contains("PowerRating")) {
                long powerRating = data.getLong("PowerRating");
                tierDisplay += " (" + powerRating + " EU/t)";
            }
            Component tierText = Component.translatable("jade.mio_icif.cable_tier", tierDisplay);
            tooltip.add(tierText);
        }

        if (data.contains("DelegateTier")) {
            // 同样以"能量等级"编号显示，缺失时回退为等级名称。
            String delegateTierDisplay = data.contains("DelegateTierLevel")
                    ? String.valueOf(data.getInt("DelegateTierLevel"))
                    : data.getString("DelegateTier");
            if (data.contains("DelegatePowerRating")) {
                long delegatePowerRating = data.getLong("DelegatePowerRating");
                delegateTierDisplay += " (" + delegatePowerRating + " EU/t)";
            }
            Component delegateTierText = Component.translatable("jade.mio_icif.delegate_tier", delegateTierDisplay);
            tooltip.add(delegateTierText);
        }
    }

    /**
     * 将能量数值格式化为带 K/M/B/G 单位的短字符串，与 EU 电表显示风格保持一致。
     */
    private static String formatEnergy(long energy) {
        if (energy >= 1_000_000_000_000L) {
            return String.format("%.2fG", energy / 1_000_000_000_000.0);
        } else if (energy >= 1_000_000_000L) {
            return String.format("%.2fB", energy / 1_000_000_000.0);
        } else if (energy >= 1_000_000L) {
            return String.format("%.2fM", energy / 1_000_000.0);
        } else if (energy >= 1_000L) {
            return String.format("%.2fK", energy / 1_000.0);
        }
        return String.valueOf(energy);
    }

    /**
     * Power hint for the tooltip: a generator reports the power it currently outputs (which for a
     * dynamic-tier generator follows its live voltage tier), a storage box reports its rated
     * discharge rate, and an ordinary machine reports its effective per-tick consumption.
     */
    private static void appendPower(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof IGeneratorBlock generator) {
            data.putLong("OutputPower", generator.getPowerOutput());
        } else if (blockEntity instanceof IProducerBlock producer) {
            data.putLong("WorkPower", producer.getEffectiveEnergyPerTick());
        } else if (blockEntity instanceof IEnergyContainerBlock container) {
            data.putLong("OutputPower", container.getDischargeRate());
        }
    }

    /**
     * A machine registered with a dynamic tier (IC2 conversion-generator pattern) reports the
     * voltage tier it is producing right now, derived from its live source tier, instead of the
     * static cable tier it was built with. Every other machine keeps its fixed tier.
     */
    private static ICableTier currentTier(BlockEntity blockEntity, ICableTier fallback) {
        if (!EnergyNodeRegistry.dynamicTier(BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock()))
                || !(blockEntity instanceof AbstractEnergyBlockEntity energyBlock)) {
            return fallback;
        }
        int index = energyBlock.getSourceTier() - 1;
        var tiers = CableTier.allTiers();
        return index >= 0 && index < tiers.size() ? tiers.get(index) : fallback;
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
                if (target instanceof AbstractEnergyBlockEntity energyBlock) {
                    var tier = currentTier(target, energyBlock.getEffectiveCableTier());
                    data.putString("CableTier", tier.getDisplayName());
                    data.putInt("TierLevel", tier.getTier() + 1);
                    data.putLong("PowerRating", tier.getPowerRating());
                }
                if (blockEntity instanceof AbstractEnergyBlockEntity delegateBlock) {
                    var delegateTier = delegateBlock.getEffectiveCableTier();
                    data.putString("DelegateTier", delegateTier.getDisplayName());
                    data.putInt("DelegateTierLevel", delegateTier.getTier() + 1);
                    data.putLong("DelegatePowerRating", delegateTier.getPowerRating());
                }
                appendPower(data, target);
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

        if (blockEntity instanceof AbstractEnergyBlockEntity energyBlock) {
            var tier = currentTier(blockEntity, energyBlock.getEffectiveCableTier());
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putInt("TierLevel", tier.getTier() + 1);
            data.putLong("PowerRating", tier.getPowerRating());
        } else if (blockEntity instanceof mio_icif_wire wire) {
            var tier = wire.getCableTier();
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putInt("TierLevel", tier.getTier() + 1);
            data.putLong("PowerRating", tier.getPowerRating());
        } else if (blockEntity instanceof IGeneratorBlock generator) {
            var tier = generator.getCableTier();
            String tierName = tier.getDisplayName();
            data.putString("CableTier", tierName);
            data.putInt("TierLevel", tier.getTier() + 1);
            data.putLong("PowerRating", tier.getPowerRating());
        }

        appendPower(data, blockEntity);
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}