package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * 机器信息实现
 */
@SuppressWarnings("null")
public class MachineInfoImpl implements IMachineInfo {

    private static final List<Map.Entry<String, IMachineAPI.MachineType>> TYPE_KEYWORDS = List.of(
        Map.entry("compressor", IMachineAPI.MachineType.COMPRESSOR),
        Map.entry("extractor", IMachineAPI.MachineType.EXTRACTOR),
        Map.entry("macerator", IMachineAPI.MachineType.MACERATOR),
        Map.entry("powder", IMachineAPI.MachineType.MACERATOR),
        Map.entry("smelter", IMachineAPI.MachineType.SMELTER),
        Map.entry("furnace", IMachineAPI.MachineType.SMELTER),
        Map.entry("centrifuge", IMachineAPI.MachineType.CENTRIFUGE),
        Map.entry("washer", IMachineAPI.MachineType.WASHER),
        Map.entry("recycler", IMachineAPI.MachineType.RECYCLER),
        Map.entry("metal_former", IMachineAPI.MachineType.METAL_FORMER),
        Map.entry("electrolyzer", IMachineAPI.MachineType.ELECTROLYZER),
        Map.entry("blender", IMachineAPI.MachineType.BLENDER),
        Map.entry("cutter", IMachineAPI.MachineType.CUTTER),
        Map.entry("block_cutter", IMachineAPI.MachineType.CUTTER),
        Map.entry("welder", IMachineAPI.MachineType.WELDER),
        Map.entry("lathe", IMachineAPI.MachineType.LATHE),
        Map.entry("rolling", IMachineAPI.MachineType.ROLLING),
        Map.entry("extruder", IMachineAPI.MachineType.EXTRUDER),
        Map.entry("fermenter", IMachineAPI.MachineType.FERMENTER),
        Map.entry("canner", IMachineAPI.MachineType.CANNER),
        Map.entry("fluid_solid", IMachineAPI.MachineType.FLUID_SOLID),
        Map.entry("thermal_centrifuge", IMachineAPI.MachineType.THERMAL_CENTRIFUGE),
        Map.entry("laser", IMachineAPI.MachineType.LASER_ENGRAVER),
        Map.entry("assembler", IMachineAPI.MachineType.PRECISION_ASSEMBLER),
        Map.entry("freezer", IMachineAPI.MachineType.VACUUM_FREEZER),
        Map.entry("plasma", IMachineAPI.MachineType.PLASMA_FURNACE),
        Map.entry("fusion", IMachineAPI.MachineType.FUSION_REACTOR),
        Map.entry("matter", IMachineAPI.MachineType.MATTER_FABRICATOR),
        Map.entry("replicator", IMachineAPI.MachineType.REPLICATOR),
        Map.entry("scanner", IMachineAPI.MachineType.SCANNER),
        Map.entry("teleporter", IMachineAPI.MachineType.TELEPORTER),
        Map.entry("pump", IMachineAPI.MachineType.PUMP),
        Map.entry("miner", IMachineAPI.MachineType.MINER),
        Map.entry("induction", IMachineAPI.MachineType.INDUCTION),
        Map.entry("condenser", IMachineAPI.MachineType.CONDENSER),
        Map.entry("terraformer", IMachineAPI.MachineType.TERRAFORMER),
        Map.entry("harvest", IMachineAPI.MachineType.HARVESTER),
        Map.entry("matron", IMachineAPI.MachineType.MATRON),
        Map.entry("magnetizer", IMachineAPI.MachineType.MAGNETIZER),
        Map.entry("tesla", IMachineAPI.MachineType.TESLA_COIL),
        Map.entry("neutron_polymerizer", IMachineAPI.MachineType.NEUTRON_POLYMERIZER),
        Map.entry("generator", IMachineAPI.MachineType.GENERATOR)
    );

    private final Level world;
    private final BlockPos pos;
    private final IEnergyBlock energyBlock;
    private final IMachineAPI.MachineType machineType;
    
 // 存推断的机器类型（volatile 确保多线程可见性）
    private volatile IMachineAPI.MachineType cachedInferredType;
    private volatile boolean inferredTypeComputed = false;

    public MachineInfoImpl(Level world, BlockPos pos, IEnergyBlock energyBlock) {
        this(world, pos, energyBlock, null);
    }

    public MachineInfoImpl(Level world, BlockPos pos, IEnergyBlock energyBlock, @Nullable IMachineAPI.MachineType machineType) {
        this.world = world;
        this.pos = pos;
        this.energyBlock = energyBlock;
        this.machineType = machineType;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public IMachineAPI.MachineType getMachineType() {
        // 优先使用构造函数传入的 machineType
        if (machineType != null) {
            return machineType;
        }

        // 检查 IMachineConfigurable 的配置
        if (energyBlock instanceof IMachineConfigurable configurable) {
            var config = configurable.getConfiguration();
            if (config != null && config.machineType() != null) {
                return config.machineType();
            }
            return IMachineAPI.MachineType.CUSTOM;
        }

        // 优先使用实现类报告的机器类型
        IMachineAPI.MachineType reportedType = energyBlock.getMachineType();
        if (reportedType != null) {
            return reportedType;
        }

 // 跨敤缂撳瓨閬垮厤閲嶅�嶅瓧绗︿覆鍖归厤
        if (!inferredTypeComputed) {
            cachedInferredType = inferMachineType();
            inferredTypeComputed = true;
        }
        return cachedInferredType;
    }

    /**
     * 推断机器类型（基于注册表 ID 和类名）
     * @return 推断的机器类型
     */
    private IMachineAPI.MachineType inferMachineType() {
        // 基于注册表 ID 推断（混淆安全，注册表 ID 不会被混淆）
        String registryId = BuiltInRegistries.BLOCK.getKey(energyBlock.getBlockState().getBlock()).getPath().toLowerCase(Locale.ROOT);
        IMachineAPI.MachineType result = lookupByKeywords(registryId);
        if (result != null) return result;

        // 基于类名推断（向后兼容，混淆后可能失效）
        String className = energyBlock.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        result = lookupByKeywords(className);
        return result != null ? result : IMachineAPI.MachineType.CUSTOM;
    }

    private static IMachineAPI.MachineType lookupByKeywords(String text) {
        // 优先精确匹配（整个字符串等于关键词）
        for (Map.Entry<String, IMachineAPI.MachineType> entry : TYPE_KEYWORDS) {
            if (text.equals(entry.getKey())) return entry.getValue();
        }
        // 其次模糊匹配（字符串包含关键词）
        for (Map.Entry<String, IMachineAPI.MachineType> entry : TYPE_KEYWORDS) {
            if (text.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    @Override
    public long getStoredEnergy() {
        return energyBlock.getEnergyStorage().getAmount();
    }

    @Override
    public long getMaxEnergy() {
        return energyBlock.getEnergyStorage().getCapacity();
    }

    @Override
    public int getProgress() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getProgress();
        }
        return 0;
    }

    @Override
    public int getMaxProgress() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getMaxProgress();
        }
        return 0;
    }

    @Override
    public boolean isWorking() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.isWorking();
        }
        return false;
    }

    @Override
    public long getEnergyPerTick() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getEffectiveEnergyPerTick();
        }
        return 0;
    }

    @Override
    public long getBaseEnergyPerTick() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getEnergyPerTick();
        }
        return 0;
    }

    @Override
    public ICableTier getCableTier() {
        return energyBlock.getEnergyStorage().getCableTier();
    }

    @Override
    public ItemStack getInputItem() {
        if (energyBlock instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            int[] inputSlots = producer.getSlotLayout().getInputSlots();
            if (inputSlots.length > 0) {
                return handler.getStackInSlot(inputSlots[0]);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getOutputItem() {
        if (energyBlock instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            int[] outputSlots = producer.getSlotLayout().getOutputSlots();
            if (outputSlots.length > 0) {
                return handler.getStackInSlot(outputSlots[0]);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Collection<ItemStack> getInputItems() {
        if (energyBlock instanceof IProducerBlock producer) {
            List<ItemStack> items = new ArrayList<>();
            IItemHandler handler = producer.getItemHandler();
            int[] inputSlots = producer.getSlotLayout().getInputSlots();
            for (int slot : inputSlots) {
                if (!producer.getSlotLayout().getType(slot).isInput()) {
                    continue;
                }
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<ItemStack> getOutputItems() {
        if (energyBlock instanceof IProducerBlock producer) {
            List<ItemStack> items = new ArrayList<>();
            IItemHandler handler = producer.getItemHandler();
            int[] outputSlots = producer.getSlotLayout().getOutputSlots();
            for (int slot : outputSlots) {
                if (!producer.getSlotLayout().getType(slot).isOutput()) {
                    continue;
                }
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    @Override
    public long getRuntimeTicks() {
        // 返回当前操作的已运行时间（即当前进度）
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getProgress();
        }
        return 0;
    }

    @Override
    public long getTotalProcessed() {
        // 通过 producer 方块实体获取总处理量
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getTotalProcessed();
        }
        return 0;
    }

    @Override
    public double getSpeedMultiplier() {
        if (energyBlock instanceof IProducerBlock producer) {
            return 1.0 / producer.getUpgradeStats().getProcessTimeMultiplier();
        }
        return 1.0;
    }

    @Override
    public double getEnergyMultiplier() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getUpgradeStats().getEnergyUsageMultiplier();
        }
        return 1.0;
    }

    @Override
    public boolean supportsUpgrades() {
        if (energyBlock instanceof IProducerBlock producer) {
            return producer.getUpgradeSlotCount() > 0;
        }
        return false;
    }

    @Override
    public Collection<ItemStack> getInstalledUpgrades() {
        if (energyBlock instanceof IProducerBlock producer) {
            List<ItemStack> upgrades = new ArrayList<>();
            int start = producer.getUpgradeSlotStart();
            int count = producer.getUpgradeSlotCount();
            IItemHandler handler = producer.getItemHandler();
            for (int i = 0; i < count; i++) {
                ItemStack stack = handler.getStackInSlot(start + i);
                if (!stack.isEmpty()) {
                    upgrades.add(stack);
                }
            }
            return upgrades;
        }
        return Collections.emptyList();
    }
}