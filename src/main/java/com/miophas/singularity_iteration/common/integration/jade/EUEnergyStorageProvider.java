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
 * Jade provider for the EU energy storage display.
 * It shows EU as a progress bar, in the same style as the FE display.
 */
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

        // 0.1.11.36 (chunk loader audit L-9): chunks held now, selected, the state and the EU it pays per tick
        if (data.contains("ChunkLoaderStatus")) {
            String[] names = com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader.STATUS_NAMES;
            int status = data.getInt("ChunkLoaderStatus");
            String name = status >= 0 && status < names.length ? names[status] : "starting";
            tooltip.add(Component.translatable("jade.mio_icif.chunk_loader", data.getInt("ChunkLoaderHeld"), data.getInt("ChunkLoaderSelected"),
                Component.translatable("gui.mio_icif.chunk_loader.state." + name), data.getLong("ChunkLoaderCost")));
        }

        if (data.contains("CableTier")) {
            // 优先使用"能量等级"编号显示（LV = 能量等级1，MV = 能量等级2 ……），
            // 若服务端未同步等级编号则回退为等级名称。
            // 0.1.11.30 (cable audit D-2): the number with the tier name the HUD and the guide use, and the rating as what it
            // is, the largest packet (it read "32 EU/t"; the unlimited tier printed Long.MAX_VALUE)
            String tierDisplay = tierText(data, "TierLevel", "CableTier", "PowerRating");
            Component tierText = Component.translatable("jade.mio_icif.cable_tier", tierDisplay);
            tooltip.add(tierText);
        }

        if (data.contains("DelegateTier")) {
            // 同样以"能量等级"编号显示，缺失时回退为等级名称。
            String delegateTierDisplay = tierText(data, "DelegateTierLevel", "DelegateTier", "DelegatePowerRating");
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
        if (blockEntity instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader loader) {
            data.putInt("ChunkLoaderStatus", loader.status());
            data.putInt("ChunkLoaderHeld", loader.getForcedChunkCount());
            data.putInt("ChunkLoaderSelected", loader.getLoadedChunkCount());
            data.putLong("ChunkLoaderCost", loader.getEnergyPerTickPaid());
            return;
        }
        if (blockEntity instanceof IGeneratorBlock generator) {
            data.putLong("OutputPower", generator.getPowerOutput());
        } else if (blockEntity instanceof IProducerBlock producer) {
            data.putLong("WorkPower", producer.getDisplayedEnergyPerTick());
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

    /** "1 · LV (packets up to 32 EU)"; the name alone when the number is missing; "∞" for an unlimited tier. */
    private static String tierText(net.minecraft.nbt.CompoundTag data, String levelKey, String nameKey, String ratingKey) {
        String name = data.getString(nameKey);
        String shown = name;
        try { shown = CableTier.getTier(name).getDisplayName(); } catch (java.util.NoSuchElementException unknown) { /* the stored name */ }
        String text = data.contains(levelKey) ? data.getInt(levelKey) + " · " + shown : shown;
        if (data.contains(ratingKey)) {
            long rating = data.getLong(ratingKey);
            String packet = rating >= Long.MAX_VALUE / 2 ? "∞" : String.format(java.util.Locale.ROOT, "%,d", rating);
            text += " " + Component.translatable("jade.mio_icif.tier_packet", packet).getString();
        }
        return text;
    }
}
