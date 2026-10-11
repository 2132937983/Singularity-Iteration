package com.miophas.singularity_iteration.core.runtime.machine;

import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import com.miophas.singularity_iteration.core.api.machine.IEnergyBlock;
import com.miophas.singularity_iteration.core.api.machine.IEnergyContainerBlock;
import com.miophas.singularity_iteration.core.api.machine.IEnergyConverter;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.api.machine.IMachineConfigurable;
import com.miophas.singularity_iteration.core.api.machine.IMachineControlAPI;
import com.miophas.singularity_iteration.core.api.machine.IMachineInfo;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockReactorAPI;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IPipeBlock;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.api.machine.IRedstoneAPI;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.ISlotType;
import com.miophas.singularity_iteration.core.api.machine.ITransformerBlock;
import com.miophas.singularity_iteration.core.runtime.machine.MachineInfoImpl;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
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
 * Machine API implementation.
 */
@SuppressWarnings("null")
public class MachineAPIImpl implements IMachineAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineAPIImpl.class);

    // Global machine position registry. It is grouped by dimension ResourceKey. This prevents memory leaks from held Level references.
    // The main mod keeps it up to date: it calls registerMachine/unregisterMachine when a machine block is placed or broken.
    private static final Map<ResourceKey<Level>, Set<BlockPos>> machineRegistry = new ConcurrentHashMap<>();
    
    // Time stamp of the last cleanup, for each dimension. Thus the cleanup interval of dimension A does not affect dimension B.
    private static final Map<ResourceKey<Level>, Long> lastCleanupTime = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 12000; // Clean up one time each 10 minutes (at 20 TPS).

    /** Default cable tier for the client side and fallback cases. It prevents the start of the lazy API load chain. */
    private static final ICableTier DEFAULT_CABLE_TIER = com.miophas.singularity_iteration.core.api.energy.tile.IEnergyConductor.LV_TIER;

    /**
     * Adds a machine position to the global registry.
     *
     * <p><b>This method is an internal API. Add-on mods must not call it.</b>
     * The energy network manages machine registration automatically through {@code EnergyTileLoadEvent}.
     *
     * @param world the world
     * @param pos the machine position
     * @apiNote Internal — only the energy network calls this method.
     */
    public static void registerMachine(Level world, BlockPos pos) {
        machineRegistry.computeIfAbsent(world.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /**
     * Removes a machine position from the global registry.
     *
     * <p><b>This method is an internal API. Add-on mods must not call it.</b>
     * The energy network manages machine removal automatically through {@code EnergyTileUnloadEvent}.
     *
     * @param world the world
     * @param pos the machine position
     * @apiNote Internal — only the energy network calls this method.
     */
    public static void unregisterMachine(Level world, BlockPos pos) {
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * Removes the registry entries of an unloaded dimension.
     */
    public static void onDimensionUnload(Level world) {
        machineRegistry.remove(world.dimension());
        lastCleanupTime.remove(world.dimension());
    }

    /**
     * Clears all machine registries. Call this method when the server stops.
     * 
     * <p>This method releases the memory of the static registries. This prevents memory leaks.
     * Call it in ServerStoppingEvent or in a similar lifecycle event.
     */
    public static void clearRegistry() {
        int totalSize = machineRegistry.values().stream().mapToInt(Set::size).sum();
        machineRegistry.clear();
        lastCleanupTime.clear();
        LOGGER.info("Cleared machine registry ({} entries)", totalSize);
    }
    
    /**
     * Removes stale entries from time to time (positions that are registered but have no block entity).
     * Call this method regularly from a tick event. It does the cleanup only after CLEANUP_INTERVAL_TICKS.
     */
    public static void cleanupStaleEntries(Level world) {
        long currentTime = world.getGameTime();
        long lastTime = lastCleanupTime.getOrDefault(world.dimension(), 0L);
        if (currentTime - lastTime < CLEANUP_INTERVAL_TICKS) {
            return; // The cleanup interval is not complete. Skip.
        }
        
        lastCleanupTime.put(world.dimension(), currentTime);
        Set<BlockPos> set = machineRegistry.get(world.dimension());
        if (set == null || set.isEmpty()) {
            return;
        }
        
        // Collect the stale entries to remove.
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : set) {
            if (world.getBlockEntity(pos) == null) {
                toRemove.add(pos);
            }
        }
        
        // Remove them in one operation.
        if (!toRemove.isEmpty()) {
            set.removeAll(toRemove);
            LOGGER.debug("Cleaned up {} stale machine entries in dimension {}", toRemove.size(), world.dimension().location());
        }
    }

    /**
     * Gets all machine positions in the given world (internal method for GeneratorAPIImpl).
     *
     * <p>The return value is an unmodifiable snapshot copy. The caller cannot change the internal registry.
     *
     * @return a snapshot copy of the machine positions, or an empty set if the dimension is not registered
     * @apiNote Internal — only GeneratorAPIImpl calls this method.
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

        // Check the special block types first. This makes sure that machineType is correct.
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

        // Try to handle it as a producer.
        if (be instanceof IProducerBlock producer) {
            producer.forceStartWork();
            return true;
        }

        // Generator logic: use IBurnControl to force a start.
        if (be instanceof com.miophas.singularity_iteration.core.api.machine.IBurnControl gen) {
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

        // Try to handle it as a producer.
        if (be instanceof IProducerBlock producer) {
            producer.forceStopWork();
            return true;
        }

        // Generator logic: use IBurnControl to force a stop.
        if (be instanceof com.miophas.singularity_iteration.core.api.machine.IBurnControl gen) {
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

    // ========== Machine work information API ==========

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

    // ========== Generator API ==========

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

    // ========== Redstone control API ==========

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
            // English API text (0.1.7.36: before, hard-coded Chinese). UI code uses translation keys.
            case 0 -> "No redstone control";
            case 1 -> "Signal when full";
            case 2 -> "Signal when partly charged";
            case 3 -> "Signal when not full";
            case 4 -> "Signal when empty";
            case 5 -> "Redstone stops output";
            case 6 -> "Conditional output";
            case 7 -> "Redstone signal output";
            default -> "Unknown mode";
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

    // ========== Upgrade combination statistics API ==========

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

    // ========== Reactor multiblock API ==========
    // Access to the internal implementation goes through the IReactorController / IMultiblockStructure / IAccessHatch interfaces.
    // This class does not refer to the concrete classes directly. Add-on mods must still use MioIcifAPI.instance().getReactorAPI() first.

    // ========== IMachineControlAPI implementation ==========

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

    // ========== IMultiblockReactorAPI implementation ==========

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
                // Return the coolant fill fraction (0.0 to 1.0).
                int capacity = fluidHandler.getTankCapacity(0);
                if (capacity > 0) {
                    int amount = fluidHandler.getFluidInTank(0).getAmount();
                    return (double) amount / capacity;
                }
            }
        }
        return 0;
    }

    // ========== IRedstoneAPI implementation ==========

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
        // Check for a redstone signal.
        boolean hasSignal = world.hasNeighborSignal(pos);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IProducerBlock) {
            // Use the redstone mode to find if work is allowed.
            RedstoneMode mode = getRedstoneModeEnum(world, pos);
            return switch (mode) {
                case NONE -> true; // No mode. Work is always allowed.
                case FULL -> hasSignal; // Work when there is a signal.
                case NOT_FULL -> !hasSignal; // Work when there is no signal.
                default -> true;
            };
        }
        return !hasSignal; // Default: work is allowed when there is no signal.
    }

    // ========== Internal wrapper classes ==========

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
        public long getHeatCapacity(long baseCapacity) {
            return internal.getHeatCapacity(baseCapacity);
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

    // ==================== IProducerBlock adapter ====================

    /**
     * Gives an {@link IMachineInfo} view for {@link IProducerBlock} implementations that do not extend {@link AbstractEnergyBlockEntity}
     * (for example the solar distiller, the lathe and the steam kinetic generator).
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
     * IMachineInfo adapter for an IEnergyBlock with a fixed MachineType.
     * Use it for transformers, energy converters and other blocks that must have a specified MachineType.
     * Thus it does not depend on the IMachineConfigurable configuration.
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
     * IMachineInfo adapter for pipe blocks.
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