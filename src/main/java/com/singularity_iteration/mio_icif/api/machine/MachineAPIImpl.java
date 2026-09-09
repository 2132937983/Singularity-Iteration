package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器 API 实现
 */
@SuppressWarnings("null")
public class MachineAPIImpl implements IMachineAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineAPIImpl.class);

    // 全局机器位置注册表（按维度 ResourceKey 分组，避免持有 Level 对象引用导致内存泄漏）
    // 由主模组在机器方块放置/破坏时调用 registerMachine/unregisterMachine 维护
    private static final Map<ResourceKey<Level>, Set<BlockPos>> machineRegistry = new ConcurrentHashMap<>();
    
    // 上次清理时间戳（按维度记录，避免维度 A 的清理间隔影响维度 B）
    private static final Map<ResourceKey<Level>, Long> lastCleanupTime = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 12000; // 每 10 分钟清理一次（假设 20 TPS）

    /** 客户端侧和降级场景使用的默认电缆等级，避免触发 API 懒加载链 */
    private static final ICableTier DEFAULT_CABLE_TIER = com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor.LV_TIER;

    /**
     * 注册机器位置到全局注册表。
     *
     * <p><b>此方法为内部 API，附属模组不应调用。</b>
     * 机器注册由能量网络通过 {@code EnergyTileLoadEvent} 自动管理。
     *
     * @param world 世界
     * @param pos 机器位置
     * @apiNote Internal — 仅供能量网络调用
     */
    public static void registerMachine(Level world, BlockPos pos) {
        machineRegistry.computeIfAbsent(world.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /**
     * 从全局注册表移除机器位置。
     *
     * <p><b>此方法为内部 API，附属模组不应调用。</b>
     * 机器注销由能量网络通过 {@code EnergyTileUnloadEvent} 自动管理。
     *
     * @param world 世界
     * @param pos 机器位置
     * @apiNote Internal — 仅供能量网络调用
     */
    public static void unregisterMachine(Level world, BlockPos pos) {
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * 清理已卸载维度的注册表条目
     */
    public static void onDimensionUnload(Level world) {
        machineRegistry.remove(world.dimension());
        lastCleanupTime.remove(world.dimension());
    }

    /**
     * 清空所有机器注册表（服务端停止时调用）
     * 
     * <p>此方法用于释放静态注册表占用的内存，防止内存泄漏。
     * 应在 ServerStoppingEvent 或类似生命周期事件中调用。
     */
    public static void clearRegistry() {
        int totalSize = machineRegistry.values().stream().mapToInt(Set::size).sum();
        machineRegistry.clear();
        lastCleanupTime.clear();
        LOGGER.info("Cleared machine registry ({} entries)", totalSize);
    }
    
    /**
     * 定期清理僵尸条目（方块已不存在但仍注册的位置）
     * 此方法应在 tick 事件中定期调用
     */
    public static void cleanupStaleEntries(Level world) {
        long currentTime = world.getGameTime();
        long lastTime = lastCleanupTime.getOrDefault(world.dimension(), 0L);
        if (currentTime - lastTime < CLEANUP_INTERVAL_TICKS) {
            return; // 未到清理间隔，跳过
        }
        
        lastCleanupTime.put(world.dimension(), currentTime);
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        if (set == null || set.isEmpty()) {
            return;
        }
        
        // 收集需要移除的僵尸条目
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : set) {
            if (world.getBlockEntity(pos) == null) {
                toRemove.add(pos);
            }
        }
        
        // 批量移除
        if (!toRemove.isEmpty()) {
            set.removeAll(toRemove);
            LOGGER.debug("Cleaned up {} stale machine entries in dimension {}", toRemove.size(), world.dimension().location());
        }
    }

    /**
     * 获取指定世界中的所有机器位置（内部方法，供 GeneratorAPIImpl 使用）。
     *
     * <p>返回的是不可修改的快照副本，调用者无法修改内部注册表。
     *
     * @return 机器位置集合的快照副本，如果维度未注册则返回空集合
     * @apiNote Internal — 仅供 GeneratorAPIImpl 调用
     */
    public static Set<BlockPos> getAllMachinePositions(Level world) {
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        return set != null ? Set.copyOf(set) : Set.of();
    }

    @Override
    public boolean isMachine(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof IEnergyBlock
            || be instanceof IGeneratorBlock
            || be instanceof IProducerBlock
            || be instanceof IPipeBlock;
    }

    @Override
    public Optional<IMachineInfo> getMachineInfo(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();

        BlockEntity be = world.getBlockEntity(pos);

        // 优先检查特殊方块类型，确保 machineType 正确
        if (be instanceof ITransformerBlock transformer) {
            return Optional.of(new EnergyBlockInfoWithFixedType(world, pos, transformer, MachineType.TRANSFORMER));
        }
        if (be instanceof IEnergyConverter converter) {
            return Optional.of(new EnergyBlockInfoWithFixedType(world, pos, converter, MachineType.ENERGY_CONVERTER));
        }

        if (be instanceof IEnergyBlock energyBlock) {
            IMachineAPI.MachineType machineType = null;
            if (be instanceof IMachineConfigurable configurable) {
                var config = configurable.getConfiguration();
                if (config != null) {
                    machineType = config.machineType();
                }
            }
            return Optional.of(new MachineInfoImpl(world, pos, energyBlock, machineType));
        }
        if (be instanceof IGeneratorBlock generator) {
            return Optional.of(new GeneratorMachineInfoImpl(world, pos, generator));
        }
        if (be instanceof IProducerBlock producer) {
            return Optional.of(new ProducerMachineInfoImpl(world, pos, producer));
        }
        if (be instanceof IPipeBlock pipe) {
            return Optional.of(new PipeMachineInfoImpl(world, pos, pipe));
        }
        return Optional.empty();
    }

    @Override
    public Collection<BlockPos> getMachinesByType(Level world, MachineType type) {
        if (world.isClientSide) return Collections.emptyList();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : getAllMachines(world)) {
            Optional<IMachineInfo> infoOpt = getMachineInfo(world, pos);
            if (infoOpt.isPresent() && infoOpt.get().getMachineType() == type) {
                result.add(pos);
            }
        }
        return result;
    }

    @Override
    public Collection<BlockPos> getAllMachines(Level world) {
        if (world.isClientSide) return Collections.emptyList();
        
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        if (set == null) return Collections.emptyList();
        
        List<BlockPos> snapshot = new ArrayList<>(set);
        
        List<BlockPos> toRemove = new ArrayList<>();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : snapshot) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof IEnergyBlock || be instanceof IGeneratorBlock || be instanceof IProducerBlock || be instanceof IPipeBlock) {
                result.add(pos);
            } else {
                toRemove.add(pos);
            }
        }
        set.removeAll(toRemove);
        return result;
    }

    @Override
    public boolean forceStart(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;

        // 尝试作为 producer 处理
        if (be instanceof IProducerBlock producer) {
            producer.forceStartWork();
            return true;
        }

        // 发电机逻辑：通过 IBurnControl 强制启动
        if (be instanceof com.singularity_iteration.mio_icif.api.machine.IBurnControl gen) {
            int maxBurn = gen.getDefaultBurnTime();
            gen.setBurnTime(maxBurn > 0 ? maxBurn : 200);
            return true;
        }
        return false;
    }

    @Override
    public boolean forceStop(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;

        // 尝试作为 producer 处理
        if (be instanceof IProducerBlock producer) {
            producer.forceStopWork();
            return true;
        }

        // 发电机逻辑：通过 IBurnControl 强制停止
        if (be instanceof com.singularity_iteration.mio_icif.api.machine.IBurnControl gen) {
            gen.setBurnTime(0);
            return true;
        }
        return false;
    }

    @Override
    public double getSpeedMultiplier(Level world, BlockPos pos) {
        if (world.isClientSide) return 1.0;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? 1.0 / stats.getProcessTimeMultiplier() : 1.0;
        }
        return 1.0;
    }

    @Override
    public double getEnergyMultiplier(Level world, BlockPos pos) {
        if (world.isClientSide) return 1.0;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getEnergyUsageMultiplier() : 1.0;
        }
        return 1.0;
    }

    @Override
    public ICableTier getCableTier(Level world, BlockPos pos) {
        if (world.isClientSide) return DEFAULT_CABLE_TIER;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyBlock energyBlock) {
            return energyBlock.getEnergyStorage().getCableTier();
        }
        return DEFAULT_CABLE_TIER;
    }

    @Override
    public boolean supportsUpgrades(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getUpgradeSlotCount() > 0;
        }
        return false;
    }

    @Override
    public Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos) {
        if (world.isClientSide) return Collections.emptyList();

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
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

    @Override
    public boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            int start = producer.getUpgradeSlotStart();
            int count = producer.getUpgradeSlotCount();
            for (int i = 0; i < count; i++) {
                int slot = start + i;
                if (handler.getStackInSlot(slot).isEmpty()) {
                    ItemStack remainder = handler.insertItem(slot, upgrade, false);
                    return remainder.isEmpty() || remainder.getCount() < upgrade.getCount();
                }
            }
            LOGGER.warn("No empty upgrade slot available in machine at {}", pos);
        }
        return false;
    }

    @Override
    public ItemStack removeUpgrade(Level world, BlockPos pos, int slot) {
        if (world.isClientSide) return ItemStack.EMPTY;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            int start = producer.getUpgradeSlotStart();
            int count = producer.getUpgradeSlotCount();
            if (slot >= 0 && slot < count) {
                int actualSlot = start + slot;
                ItemStack existing = handler.getStackInSlot(actualSlot);
                if (!existing.isEmpty()) {
                    ItemStack extracted = handler.extractItem(actualSlot, existing.getCount(), false);
                    return extracted;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // ========== 机器工作信息 API ==========

    @Override
    public int getMachineProgress(Level world, BlockPos pos) {
        return getProgress(world, pos);
    }

    @Override
    public int getMachineMaxProgress(Level world, BlockPos pos) {
        return getMaxProgress(world, pos);
    }

    @Override
    public long getMachineEnergyPerTick(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getEnergyPerTick();
        }
        return 0;
    }

    @Override
    public Optional<ISlotLayout> getMachineSlotLayout(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return Optional.of(new SlotLayoutWrapper(producer.getSlotLayout()));
        }
        return Optional.empty();
    }

    @Override
    public ItemStack getMachineSlotItem(Level world, BlockPos pos, int slot) {
        if (world.isClientSide) return ItemStack.EMPTY;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            if (slot >= 0 && slot < handler.getSlots()) {
                return handler.getStackInSlot(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean setMachineSlotItem(Level world, BlockPos pos, int slot, ItemStack stack) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            IItemHandler handler = producer.getItemHandler();
            if (slot >= 0 && slot < handler.getSlots()) {
                ItemStack existing = handler.getStackInSlot(slot);
                if (!existing.isEmpty()) {
                    handler.extractItem(slot, existing.getCount(), false);
                }
                ItemStack remainder = handler.insertItem(slot, stack, false);
                return remainder.isEmpty() || remainder.getCount() < stack.getCount();
            }
        }
        return false;
    }

    @Override
    public int getMachineSlotCount(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getContainerSize();
        }
        return 0;
    }

    // ========== 发电机专用 API ==========

    @Override
    public boolean isGeneratorBurning(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            return gen.isBurning();
        }
        return false;
    }

    @Override
    public int getGeneratorBurnTime(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            return gen.getBurnTime();
        }
        return 0;
    }

    @Override
    public int getGeneratorBurnDuration(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            return gen.getMaxBurnTime();
        }
        return 0;
    }

    @Override
    public long getGeneratorEnergyRate(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            return gen.getPowerOutput();
        }
        return 0;
    }

    @Override
    public ItemStack getGeneratorFuelSlot(Level world, BlockPos pos) {
        if (world.isClientSide) return ItemStack.EMPTY;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            return gen.getFuelSlotItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean setGeneratorFuelSlot(Level world, BlockPos pos, ItemStack stack) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IGeneratorBlock gen) {
            IItemHandler handler = gen.getItemHandler();
            if (handler != null && handler.getSlots() > 0) {
                ItemStack remainder = handler.insertItem(0, stack, false);
                return remainder.isEmpty() || remainder.getCount() < stack.getCount();
            }
        }
        return false;
    }

    // ========== 红石控制 API ==========

    @Override
    public int getRedstoneMode(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getRedstoneMode().getId();
        }
        if (be instanceof IEnergyContainerBlock container) {
            return container.getRedstoneMode();
        }
        return 0;
    }

    @Override
    public boolean setRedstoneMode(Level world, BlockPos pos, int mode) {
        if (world.isClientSide || mode < 0 || mode >= 8) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            producer.setRedstoneMode(IMachineAPI.RedstoneMode.fromId(mode));
            return true;
        }
        if (be instanceof IEnergyContainerBlock container) {
            container.setRedstoneMode((byte) mode);
            return true;
        }
        return false;
    }

    @Override
    public String getRedstoneModeName(int mode) {
        return switch (mode) {
            case 0 -> "无红石控制";
            case 1 -> "满电输出信号";
            case 2 -> "中间范围信号";
            case 3 -> "未满输出信号";
            case 4 -> "空电输出信号";
            case 5 -> "红石停止输出";
            case 6 -> "条件输出";
            case 7 -> "红石信号输出";
            default -> "未知模式";
        };
    }

    @Override
    public boolean isEmittingRedstone(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyContainerBlock container) {
            return container.shouldEmitRedstone();
        }
        return false;
    }

    @Override
    public int getRedstoneSignalStrength(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyContainerBlock container) {
            return container.getRedstoneSignalStrength();
        }
        return 0;
    }

    @Override
    public boolean hasRedstoneInput(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyContainerBlock container) {
            return container.hasRedstoneInput();
        }
        return false;
    }

    @Override
    public boolean isEnergyOutputEnabled(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyContainerBlock container) {
            return container.shouldEmitEnergy();
        }
        return false;
    }

    // ========== 升级组合统计 API ==========

    @Override
    public Optional<IMachineUpgradeStats> getUpgradeStats(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return Optional.of(new UpgradeStatsWrapper(producer.getUpgradeStats()));
        }
        return Optional.empty();
    }

    @Override
    @Deprecated
    public double getEffectiveSpeedMultiplier(Level world, BlockPos pos) {
        return getEffectiveProcessTimeMultiplier(world, pos);
    }

    @Override
    public double getEffectiveProcessTimeMultiplier(Level world, BlockPos pos) {
        if (world.isClientSide) return 1.0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getProcessTimeMultiplier() : 1.0;
        }
        return 1.0;
    }

    @Override
    public double getEffectiveEnergyMultiplier(Level world, BlockPos pos) {
        if (world.isClientSide) return 1.0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getEnergyUsageMultiplier() : 1.0;
        }
        return 1.0;
    }

    @Override
    public Optional<ICableTier> getEffectiveCableTier(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            if (stats == null) return Optional.empty();
            var storage = producer.getEnergyStorage();
            if (storage == null) return Optional.empty();
            return Optional.of(stats.getEffectiveCableTier(storage.getCableTier()));
        }
        return Optional.empty();
    }

    @Override
    public long getEnergyCapacityBonus(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getEnergyCapacityBonus() : 0;
        }
        return 0;
    }

    @Override
    public int getEffectiveProcessTicks(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getProcessTicks(producer.getBaseMaxProgress()) : producer.getBaseMaxProgress();
        }
        return 0;
    }

    @Override
    public long getEffectiveEnergyPerTick(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            var stats = producer.getUpgradeStats();
            return stats != null ? stats.getEnergyPerTick(producer.getEnergyPerTick()) : producer.getEnergyPerTick();
        }
        return 0;
    }

    // ========== 反应堆多方块 API ==========
    // 已通过 IReactorController / IMultiblockStructure / IAccessHatch 接口访问内部实现，
    // 不再直接引用具体类。附属模组仍应优先使用 MioIcifAPI.instance().getReactorAPI()。

    // ========== IMachineControlAPI 实现 ==========

    @Override
    public boolean startMachine(Level world, BlockPos pos) {
        return forceStart(world, pos);
    }

    @Override
    public boolean stopMachine(Level world, BlockPos pos) {
        return forceStop(world, pos);
    }

    @Override
    public int getProgress(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getProgress();
        }
        return 0;
    }

    @Override
    public int getMaxProgress(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getMaxProgress();
        }
        return 0;
    }

    @Override
    public boolean isWorking(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.isWorking();
        }
        return false;
    }

    @Override
    public Optional<MachineType> getMachineType(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        Optional<IMachineInfo> infoOpt = getMachineInfo(world, pos);
        return infoOpt.map(IMachineInfo::getMachineType);
    }

    @Override
    public Optional<IMachineConfiguration> getMachineConfiguration(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IEnergyBlock energyBlock) {
            return Optional.of(new MachineConfigurationImpl(energyBlock));
        }
        if (be instanceof IGeneratorBlock generator) {
            return Optional.of(new GeneratorConfigurationImpl(generator));
        }
        return Optional.empty();
    }

    // ========== IMultiblockReactorAPI 实现 ==========

    @Override
    public Optional<IMultiblockInfo> getReactorInfo(Level world, BlockPos pos) {
        if (world.isClientSide) return Optional.empty();
        var structure = IMultiblockStructure.find(world, pos);
        if (structure == null) return Optional.empty();
        return Optional.of(new MultiblockInfoWrapper(structure));
    }

    @Override
    public boolean isReactorController(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof IReactorController;
    }

    @Override
    public double getReactorTemperature(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getCurrentTemperature();
        }
        return 0;
    }

    @Override
    public double getReactorCoolantLevel(Level world, BlockPos pos) {
        if (world.isClientSide) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            var fluidHandler = reactor.getFluidHandler();
            if (fluidHandler != null) {
                // 返回冷却液填充百分比
                int capacity = fluidHandler.getTankCapacity(0);
                if (capacity > 0) {
                    int amount = fluidHandler.getFluidInTank(0).getAmount();
                    return (double) amount / capacity;
                }
            }
        }
        return 0;
    }

    // ========== IRedstoneAPI 实现 ==========

    @Override
    public RedstoneMode getRedstoneModeEnum(Level world, BlockPos pos) {
        if (world.isClientSide) return RedstoneMode.NONE;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            return producer.getRedstoneMode();
        }
        if (be instanceof IEnergyContainerBlock container) {
            return RedstoneMode.fromId(container.getRedstoneMode());
        }
        return RedstoneMode.NONE;
    }

    @Override
    public void setRedstoneModeEnum(Level world, BlockPos pos, RedstoneMode mode) {
        if (world.isClientSide) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock producer) {
            producer.setRedstoneMode(mode);
            return;
        }
        if (be instanceof IEnergyContainerBlock container) {
            container.setRedstoneMode((byte) mode.getId());
        }
    }

    @Override
    public boolean isRedstoneAllowed(Level world, BlockPos pos) {
        if (world.isClientSide) return true;
        // 检查是否有红石信号
        boolean hasSignal = world.hasNeighborSignal(pos);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock) {
            // 根据红石模式判断是否允许工作
            RedstoneMode mode = getRedstoneModeEnum(world, pos);
            return switch (mode) {
                case NONE -> true; // 无模式，始终允许
                case FULL -> hasSignal; // 有信号时工作
                case NOT_FULL -> !hasSignal; // 无信号时工作
                default -> true;
            };
        }
        return !hasSignal; // 默认无信号时允许工作
    }

    // ========== 内部包装类 ==========

    @SuppressWarnings("null")
    private static class SlotLayoutWrapper implements ISlotLayout {
        private final ISlotLayout delegate;

        SlotLayoutWrapper(ISlotLayout delegate) {
            if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
            this.delegate = delegate;
        }

        @Override
        public int getTotalCount() {
            return delegate.getTotalCount();
        }

        @Override
        public ISlotType getType(int slot) {
            return delegate.getType(slot);
        }

        @Override
        public int getCount(ISlotType type) {
            return delegate.getCount(type);
        }

        @Override
        public int[] getSlotsOfType(ISlotType type) {
            return delegate.getSlotsOfType(type);
        }

        @Override
        public int[] getInputSlots() {
            return delegate.getInputSlots();
        }

        @Override
        public int[] getOutputSlots() {
            return delegate.getOutputSlots();
        }

        @Override
        public int[] getBatterySlots() {
            return delegate.getBatterySlots();
        }

        @Override
        public int[] getUpgradeSlots() {
            return delegate.getUpgradeSlots();
        }

        @Override
        public int getInputCount() {
            return delegate.getInputCount();
        }

        @Override
        public int getOutputCount() {
            return delegate.getOutputCount();
        }

        @Override
        public int getBatteryCount() {
            return delegate.getBatteryCount();
        }

        @Override
        public int getUpgradeCount() {
            return delegate.getUpgradeCount();
        }
    }

    @SuppressWarnings("null")
    private static class MachineConfigurationImpl implements IMachineConfiguration {
        private final IEnergyBlock energyBlock;

        MachineConfigurationImpl(IEnergyBlock energyBlock) {
            this.energyBlock = energyBlock;
        }

        @Override
        public MachineType getMachineType() {
            return energyBlock.getMachineType();
        }

        @Override
        public ICableTier getCableTier() {
            return energyBlock.getEnergyStorage().getCableTier();
        }

        @Override
        public long getEnergyCapacity() {
            return energyBlock.getEnergyStorage().getCapacity();
        }

        @Override
        public long getMaxReceive() {
            return energyBlock.getEnergyStorage().getMaxReceive();
        }

        @Override
        public long getMaxExtract() {
            return energyBlock.getEnergyStorage().getMaxExtract();
        }

        @Override
        public int getBaseProgress() {
            if (energyBlock instanceof IProducerBlock producer) {
                return producer.getBaseMaxProgress();
            }
            return 0;
        }

        @Override
        public long getBaseEnergyPerTick() {
            if (energyBlock instanceof IProducerBlock producer) {
                return producer.getEnergyPerTick();
            }
            if (energyBlock instanceof IGeneratorBlock gen) {
                return gen.getPowerOutput();
            }
            return 0;
        }

        @Override
        public int getSlotCount() {
            if (energyBlock instanceof IProducerBlock producer) {
                return producer.getContainerSize();
            }
            if (energyBlock instanceof IGeneratorBlock gen) {
                IItemHandler handler = gen.getItemHandler();
                return handler != null ? handler.getSlots() : 0;
            }
            return 0;
        }
    }

    private static class GeneratorMachineInfoImpl implements IMachineInfo {
        private final Level world;
        private final BlockPos pos;
        private final IGeneratorBlock generator;

        GeneratorMachineInfoImpl(Level world, BlockPos pos, IGeneratorBlock generator) {
            this.world = world;
            this.pos = pos;
            this.generator = generator;
        }

        @Override public Level getWorld() { return world; }
        @Override public BlockPos getPos() { return pos; }
        @Override public IMachineAPI.MachineType getMachineType() { return IMachineAPI.MachineType.GENERATOR; }
        @Override public boolean isWorking() { return generator.isBurning(); }
        @Override public int getProgress() { return 0; }
        @Override public int getMaxProgress() { return 0; }
        @Override public long getEnergyPerTick() { return generator.getPowerOutput(); }
        @Override public long getBaseEnergyPerTick() { return generator.getPowerOutput(); }
        @Override public ICableTier getCableTier() { return generator.getCableTier(); }
        @Override public long getRuntimeTicks() { return generator.getMaxBurnTime() - generator.getBurnTime(); }
        @Override public long getTotalProcessed() { return generator.getTotalGenerated(); }
        @Override public double getSpeedMultiplier() { return 1.0; }
        @Override public double getEnergyMultiplier() { return 1.0; }
        @Override public boolean supportsUpgrades() { return false; }
        @Override public Collection<ItemStack> getInstalledUpgrades() { return Collections.emptyList(); }

        @Override
        public long getStoredEnergy() {
            if (generator instanceof IEnergyBlock eb) return eb.getEnergyStorage().getAmount();
            return 0;
        }

        @Override
        public long getMaxEnergy() {
            if (generator instanceof IEnergyBlock eb) return eb.getEnergyStorage().getCapacity();
            return 0;
        }

        @Override
        public ItemStack getInputItem() {
            IItemHandler handler = generator.getItemHandler();
            return (handler != null && handler.getSlots() > 0) ? handler.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack getOutputItem() {
            IItemHandler handler = generator.getItemHandler();
            return (handler != null && handler.getSlots() > 1) ? handler.getStackInSlot(1) : ItemStack.EMPTY;
        }

        @Override
        public Collection<ItemStack> getInputItems() {
            IItemHandler handler = generator.getItemHandler();
            if (handler == null) return Collections.emptyList();
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) items.add(stack);
            }
            return items;
        }

        @Override
        public Collection<ItemStack> getOutputItems() {
            return Collections.emptyList();
        }
    }

    private static class GeneratorConfigurationImpl implements IMachineConfiguration {
        private final IGeneratorBlock generator;

        GeneratorConfigurationImpl(IGeneratorBlock generator) {
            this.generator = generator;
        }

        @Override public MachineType getMachineType() { return MachineType.GENERATOR; }
        @Override public ICableTier getCableTier() { return generator.getCableTier(); }
        @Override public long getEnergyCapacity() {
            if (generator instanceof IEnergyBlock eb) return eb.getEnergyStorage().getCapacity();
            return 0;
        }
        @Override public long getMaxReceive() {
            if (generator instanceof IEnergyBlock eb) return eb.getEnergyStorage().getMaxReceive();
            return 0;
        }
        @Override public long getMaxExtract() {
            if (generator instanceof IEnergyBlock eb) return eb.getEnergyStorage().getMaxExtract();
            return 0;
        }
        @Override public int getBaseProgress() { return 0; }
        @Override public long getBaseEnergyPerTick() { return generator.getPowerOutput(); }
        @Override public int getSlotCount() {
            IItemHandler handler = generator.getItemHandler();
            return handler != null ? handler.getSlots() : 0;
        }
    }

    @SuppressWarnings("null")
    private static class UpgradeStatsWrapper implements IMachineUpgradeStats {
        private final IMachineUpgradeStats internal;

        UpgradeStatsWrapper(IMachineUpgradeStats internal) {
            this.internal = internal;
        }

        @Override
        public int getOverclockerCount() {
            return internal.getOverclockerCount();
        }

        @Override
        public int getEnergyStorageCount() {
            return internal.getEnergyStorageCount();
        }

        @Override
        public int getTransformerCount() {
            return internal.getTransformerCount();
        }

        @Override
        public int getEjectorCount() {
            return internal.getEjectorCount();
        }

        @Override
        public int getPullingCount() {
            return internal.getPullingCount();
        }

        @Override
        public int getFluidEjectorCount() {
            return internal.getFluidEjectorCount();
        }

        @Override
        public int getFluidPullingCount() {
            return internal.getFluidPullingCount();
        }

        @Override
        public boolean isRedstoneInverted() {
            return internal.isRedstoneInverted();
        }

        @Override
        public List<net.minecraft.core.Direction> getEjectorDirections() {
            return internal.getEjectorDirections();
        }

        @Override
        public List<net.minecraft.core.Direction> getPullingDirections() {
            return internal.getPullingDirections();
        }

        @Override
        public List<net.minecraft.core.Direction> getFluidEjectorDirections() {
            return internal.getFluidEjectorDirections();
        }

        @Override
        public List<net.minecraft.core.Direction> getFluidPullingDirections() {
            return internal.getFluidPullingDirections();
        }

        @Override
        public double getProcessTimeMultiplier() {
            return internal.getProcessTimeMultiplier();
        }

        @Override
        public double getEnergyUsageMultiplier() {
            return internal.getEnergyUsageMultiplier();
        }

        @Override
        public long getEnergyPerTick(long baseEnergyPerTick) {
            return internal.getEnergyPerTick(baseEnergyPerTick);
        }

        @Override
        public int getMaxProgress(int baseMaxProgress) {
            return internal.getMaxProgress(baseMaxProgress);
        }

        @Override
        public int getProcessTicks(int baseTicks) {
            return internal.getProcessTicks(baseTicks);
        }

        @Override
        public long getEnergyCapacityBonus() {
            return internal.getEnergyCapacityBonus();
        }

        @Override
        public ICableTier getEffectiveCableTier(ICableTier baseTier) {
            return internal.getEffectiveCableTier(baseTier);
        }
    }

    @SuppressWarnings("null")
    private static class MultiblockInfoWrapper implements IMultiblockInfo {
        private final IMultiblockStructure internal;

        MultiblockInfoWrapper(IMultiblockStructure internal) {
            this.internal = internal;
        }

        @Override
        public boolean isStructureValid() {
            return internal.isValid();
        }

        @Override
        public int getStructureBlockCount() {
            return internal.getStructureBlocks().size();
        }

        @Override
        public java.util.Set<BlockPos> getStructureBlocks() {
            return internal.getStructureBlocks();
        }

        @Override
        public long getFormationTime() {
            return internal.getFormationTime();
        }

        @Override
        public Map<String, Object> getStructureData() {
            return internal.getStructureData();
        }

        @Override
        public Set<BlockPos> getRedstonePorts() {
            return internal.getRedstonePorts();
        }
    }

    // ==================== IProducerBlock 适配器 ====================

    /**
     * 为不继承 {@link mio_icif_Energy_Block} 的 {@link IProducerBlock} 实现
     * （如太阳能蒸馝机、车床、蒸汽动能发生机等）提供 {@link IMachineInfo} 视图。
     */
    private static class ProducerMachineInfoImpl implements IMachineInfo {
        private final Level world;
        private final BlockPos pos;
        private final IProducerBlock producer;

        ProducerMachineInfoImpl(Level world, BlockPos pos, IProducerBlock producer) {
            this.world = world;
            this.pos = pos;
            this.producer = producer;
        }

        @Override
        public Level getWorld() { return world; }

        @Override
        public BlockPos getPos() { return pos; }

        @Override
        public IMachineAPI.MachineType getMachineType() {
            return IMachineAPI.MachineType.CUSTOM;
        }

        @Override
        public long getStoredEnergy() {
            var storage = producer.getEnergyStorage();
            return storage != null ? storage.getAmount() : 0L;
        }

        @Override
        public long getMaxEnergy() {
            var storage = producer.getEnergyStorage();
            return storage != null ? storage.getCapacity() : 0L;
        }

        @Override
        public int getProgress() { return producer.getProgress(); }

        @Override
        public int getMaxProgress() { return producer.getMaxProgress(); }

        @Override
        public boolean isWorking() { return producer.isWorking(); }

        @Override
        public long getEnergyPerTick() { return producer.getEnergyPerTick(); }

        @Override
        public long getBaseEnergyPerTick() { return producer.getEnergyPerTick(); }

        @Override
        public ICableTier getCableTier() {
            return DEFAULT_CABLE_TIER;
        }

        @Override
        public ItemStack getInputItem() {
            IItemHandler handler = producer.getItemHandler();
            if (handler == null) return ItemStack.EMPTY;
            int[] inputs = producer.getSlotLayout().getInputSlots();
            if (inputs.length > 0) return handler.getStackInSlot(inputs[0]);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getOutputItem() {
            IItemHandler handler = producer.getItemHandler();
            if (handler == null) return ItemStack.EMPTY;
            int[] outputs = producer.getSlotLayout().getOutputSlots();
            if (outputs.length > 0) return handler.getStackInSlot(outputs[0]);
            return ItemStack.EMPTY;
        }

        @Override
        public Collection<ItemStack> getInputItems() {
            IItemHandler handler = producer.getItemHandler();
            if (handler == null) return Collections.emptyList();
            List<ItemStack> result = new ArrayList<>();
            for (int slot : producer.getSlotLayout().getInputSlots()) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) result.add(stack);
            }
            return result;
        }

        @Override
        public Collection<ItemStack> getOutputItems() {
            IItemHandler handler = producer.getItemHandler();
            if (handler == null) return Collections.emptyList();
            List<ItemStack> result = new ArrayList<>();
            for (int slot : producer.getSlotLayout().getOutputSlots()) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) result.add(stack);
            }
            return result;
        }

        @Override
        public long getRuntimeTicks() { return 0; }

        @Override
        public long getTotalProcessed() { return producer.getTotalProcessed(); }

        @Override
        public double getSpeedMultiplier() { return 1.0; }

        @Override
        public double getEnergyMultiplier() { return 1.0; }

        @Override
        public boolean supportsUpgrades() {
            return producer.getUpgradeSlotCount() > 0;
        }

        @Override
        public Collection<ItemStack> getInstalledUpgrades() {
            return producer.getUpgrades();
        }
    }

    /**
     * 带固定 MachineType 的 IEnergyBlock IMachineInfo 适配器。
     * 用于变压器/能量转换器等需要强制指定 MachineType 的场景，
     * 避免依赖 IMachineConfigurable 配置。
     */
    private static class EnergyBlockInfoWithFixedType implements IMachineInfo {
        private final Level world;
        private final BlockPos pos;
        private final IEnergyBlock energyBlock;
        private final MachineType fixedType;

        EnergyBlockInfoWithFixedType(Level world, BlockPos pos, IEnergyBlock energyBlock, MachineType fixedType) {
            this.world = world;
            this.pos = pos;
            this.energyBlock = energyBlock;
            this.fixedType = fixedType;
        }

        @Override public Level getWorld() { return world; }
        @Override public BlockPos getPos() { return pos; }
        @Override public MachineType getMachineType() { return fixedType; }
        @Override public long getStoredEnergy() { return energyBlock.getEnergyStorage().getAmount(); }
        @Override public long getMaxEnergy() { return energyBlock.getEnergyStorage().getCapacity(); }
        @Override public boolean isWorking() { return true; }
        @Override public int getProgress() { return 0; }
        @Override public int getMaxProgress() { return 0; }
        @Override public long getEnergyPerTick() { return 0; }
        @Override public long getBaseEnergyPerTick() { return 0; }
        @Override public ICableTier getCableTier() { return energyBlock.getEnergyStorage().getCableTier(); }
        @Override public long getRuntimeTicks() { return 0; }
        @Override public long getTotalProcessed() { return 0; }
        @Override public double getSpeedMultiplier() { return 1.0; }
        @Override public double getEnergyMultiplier() { return 1.0; }
        @Override public boolean supportsUpgrades() { return false; }
        @Override public Collection<ItemStack> getInstalledUpgrades() { return Collections.emptyList(); }
        @Override public ItemStack getInputItem() { return ItemStack.EMPTY; }
        @Override public ItemStack getOutputItem() { return ItemStack.EMPTY; }
        @Override public Collection<ItemStack> getInputItems() { return Collections.emptyList(); }
        @Override public Collection<ItemStack> getOutputItems() { return Collections.emptyList(); }
    }

    /**
     * 管道方块的 IMachineInfo 适配器
     */
    private static class PipeMachineInfoImpl implements IMachineInfo {
        private final Level world;
        private final BlockPos pos;
        private final IPipeBlock pipe;

        PipeMachineInfoImpl(Level world, BlockPos pos, IPipeBlock pipe) {
            this.world = world;
            this.pos = pos;
            this.pipe = pipe;
        }

        @Override public Level getWorld() { return world; }
        @Override public BlockPos getPos() { return pos; }
        @Override public IMachineAPI.MachineType getMachineType() { return IMachineAPI.MachineType.PIPE; }
        @Override public boolean isWorking() { return !pipe.isBlocked(); }
        @Override public int getProgress() { return 0; }
        @Override public int getMaxProgress() { return 0; }
        @Override public long getEnergyPerTick() { return 0; }
        @Override public long getBaseEnergyPerTick() { return 0; }
        @Override public ICableTier getCableTier() { return DEFAULT_CABLE_TIER; }
        @Override public long getRuntimeTicks() { return 0; }
        @Override public long getTotalProcessed() { return 0; }
        @Override public double getSpeedMultiplier() { return 1.0; }
        @Override public double getEnergyMultiplier() { return 1.0; }
        @Override public boolean supportsUpgrades() { return false; }
        @Override public Collection<ItemStack> getInstalledUpgrades() { return Collections.emptyList(); }
        @Override public long getStoredEnergy() { return 0; }
        @Override public long getMaxEnergy() { return 0; }
        @Override public ItemStack getInputItem() { return ItemStack.EMPTY; }
        @Override public ItemStack getOutputItem() { return ItemStack.EMPTY; }
        @Override public Collection<ItemStack> getInputItems() { return Collections.emptyList(); }
        @Override public Collection<ItemStack> getOutputItems() { return Collections.emptyList(); }
    }
}