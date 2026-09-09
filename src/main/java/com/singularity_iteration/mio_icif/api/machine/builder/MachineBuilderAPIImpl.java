package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器构建 API 实现
 *
 * <p>提供完整的机器构建功能，包括：
 * <ul>
 *   <li>存储机器配置参数</li>
 *   <li>创建机器定义</li>
 *   <li>注册机器到系统</li>
 *   <li>验证配置合法性</li>
 * </ul>
 */
public class MachineBuilderAPIImpl implements IMachineBuilderAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineBuilderAPIImpl.class);
    private final Map<String, Map<String, IMachineDefinition>> registeredMachines = new ConcurrentHashMap<>();

    private static final Map<String, String> customMachineTypeRegistry = new ConcurrentHashMap<>();
    
    /**
     * 机器回调注册表（线程安全）。BlockEntityType 被注册表强引用，此处改用 ConcurrentHashMap 确保线程安全。
     */
    private static final Map<BlockEntityType<?>, MachineCallbacks> callbackRegistry = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 注册机器回调
     */
    public static void registerCallbacks(BlockEntityType<?> entityType, MachineCallbacks callbacks) {
        callbackRegistry.put(entityType, callbacks);
    }
    
    /**
     * 获取机器回调
     */
    public static MachineCallbacks getCallbacks(BlockEntityType<?> entityType) {
        return callbackRegistry.get(entityType);
    }
    
    /**
     * 机器回调容器
     */
    public static class MachineCallbacks {
        public final IElectricMachineBuilder.MachineWorkCallback onWorkStart;
        public final IElectricMachineBuilder.MachineWorkCompleteCallback onWorkComplete;
        public final IElectricMachineBuilder.MachineTickCallback onTick;
        
        public MachineCallbacks(IElectricMachineBuilder.MachineWorkCallback onWorkStart,
                                IElectricMachineBuilder.MachineWorkCompleteCallback onWorkComplete,
                                IElectricMachineBuilder.MachineTickCallback onTick) {
            this.onWorkStart = onWorkStart;
            this.onWorkComplete = onWorkComplete;
            this.onTick = onTick;
        }
    }

    @Override
    public IElectricMachineBuilder createElectricMachineBuilder() {
        return new ElectricMachineBuilderImpl();
    }

    @Override
    public IGeneratorBuilder createGeneratorBuilder() {
        return new GeneratorBuilderImpl();
    }

    @Override
    public IHeatMachineBuilder createHeatMachineBuilder() {
        return new HeatMachineBuilderImpl();
    }

    @Override
    public IKineticMachineBuilder createKineticMachineBuilder() {
        return new KineticMachineBuilderImpl();
    }

    @Override
    public IEnergyContainerBuilder createEnergyContainerBuilder() {
        return new EnergyContainerBuilderImpl();
    }

    @Override
    public BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine) {
        if (modId == null || modId.isEmpty()) {
            throw new IllegalArgumentException("modId cannot be null or empty");
        }
        if (machine == null) {
            throw new IllegalArgumentException("machine cannot be null");
        }

        Map<String, IMachineDefinition> machines = registeredMachines.computeIfAbsent(modId, k -> new ConcurrentHashMap<>());
        if (machines.containsKey(machine.getId())) {
            LOGGER.warn("Machine {}:{} is already registered, overwriting", modId, machine.getId());
        }

        machines.put(machine.getId(), machine);
        LOGGER.info("Registered machine: {}:{} with {} slots", modId, machine.getId(),
            machine.getProperties().getSlotLayout().getTotalSlots());
        return machine.getBlockEntityType();
    }

    @Override
    public IMachineDefinition getMachineDefinition(String modId, String machineId) {
        Map<String, IMachineDefinition> machines = registeredMachines.get(modId);
        return machines != null ? machines.get(machineId) : null;
    }

    @Override
    public boolean isMachineRegistered(String modId, String machineId) {
        Map<String, IMachineDefinition> machines = registeredMachines.get(modId);
        return machines != null && machines.containsKey(machineId);
    }

    @Override
    public com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType registerCustomMachineType(String modId, String name) {
        if (modId == null || modId.isEmpty() || name == null || name.isEmpty()) {
            throw new IllegalArgumentException("modId and name must not be empty");
        }
        customMachineTypeRegistry.put(modId + ":" + name, name);
        return com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType.CUSTOM;
    }

    @Override
    public Optional<String> getRegisteredCustomMachineTypeName(String modId, String name) {
        return Optional.ofNullable(customMachineTypeRegistry.get(modId + ":" + name));
    }

    @Override
    public ISlotLayout createStandardLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots) {
        if (inputSlots < 0 || outputSlots < 0 || upgradeSlots < 0) {
            throw new IllegalArgumentException("Slot counts cannot be negative");
        }
        return new SlotLayoutImpl(inputSlots, outputSlots, hasBatterySlot, upgradeSlots, 0, 0);
    }

    @Override
    public ISlotLayout createFluidLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, int fluidTanks, int tankCapacity) {
        if (inputSlots < 0 || outputSlots < 0 || upgradeSlots < 0 || fluidTanks < 0) {
            throw new IllegalArgumentException("Slot/tank counts cannot be negative");
        }
        if (tankCapacity < 0) {
            throw new IllegalArgumentException("Tank capacity cannot be negative");
        }
        return new SlotLayoutImpl(inputSlots, outputSlots, hasBatterySlot, upgradeSlots, fluidTanks, tankCapacity);
    }

    @Override
    public ISlotLayout createLayoutWithSpecialSlots(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, ISlotLayout.SlotType[] specialSlots) {
        if (inputSlots < 0 || outputSlots < 0 || upgradeSlots < 0) {
            throw new IllegalArgumentException("Slot counts cannot be negative");
        }
        // 使用扩展的 SlotLayoutWithSpecialsImpl 来正确处理特殊槽位
        return new SlotLayoutWithSpecialsImpl(inputSlots, outputSlots, hasBatterySlot, upgradeSlots, specialSlots);
    }

    @Override
    public IMachineDefaults getDefaultsForTier(ICableTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException("tier cannot be null");
        }
        return new MachineDefaultsImpl(toCableTier(tier));
    }

    private static ICableTier toCableTier(ICableTier tier) {
        if (tier != null) return tier;
        return IEnergyConductor.LV_TIER;
    }

    // ========== 内部实现类 ==========

    private static class SlotLayoutImpl implements ISlotLayout {
        private final int inputSlots;
        private final int outputSlots;
        private final boolean hasBatterySlot;
        private final int upgradeSlots;
        private final int fluidTanks;
        private final int tankCapacity;

        public SlotLayoutImpl(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, int fluidTanks, int tankCapacity) {
            this.inputSlots = inputSlots;
            this.outputSlots = outputSlots;
            this.hasBatterySlot = hasBatterySlot;
            this.upgradeSlots = upgradeSlots;
            this.fluidTanks = fluidTanks;
            this.tankCapacity = tankCapacity;
        }

        @Override
        public int getTotalSlots() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots;
        }

        @Override
        public int getInputSlotCount() { return inputSlots; }

        @Override
        public int getOutputSlotCount() { return outputSlots; }

        @Override
        public int getBatterySlotIndex() { return hasBatterySlot ? inputSlots + outputSlots : -1; }

        @Override
        public int getUpgradeSlotStart() { return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0); }

        @Override
        public int getUpgradeSlotCount() { return upgradeSlots; }

        @Override
        public boolean hasBatterySlot() { return hasBatterySlot; }

        @Override
        public boolean hasFluidTanks() { return fluidTanks > 0; }

        @Override
        public int getFluidTankCount() { return fluidTanks; }

        @Override
        public int getFluidTankCapacity() { return tankCapacity; }

        @Override
        public SlotType getSlotType(int slot) {
            if (slot < 0 || slot >= getTotalSlots()) return null;
            if (slot < inputSlots) return SlotType.INPUT;
            slot -= inputSlots;
            if (slot < outputSlots) return SlotType.OUTPUT;
            slot -= outputSlots;
            if (hasBatterySlot && slot == 0) return SlotType.BATTERY;
            if (hasBatterySlot) slot -= 1;
            if (slot < upgradeSlots) return SlotType.UPGRADE;
            return SlotType.EXTRA;
        }

        @Override
        public int getStart(SlotType type) {
            return switch (type) {
                case INPUT -> 0;
                case OUTPUT -> inputSlots;
                case BATTERY -> hasBatterySlot ? inputSlots + outputSlots : -1;
                case UPGRADE -> inputSlots + outputSlots + (hasBatterySlot ? 1 : 0);
                default -> -1;
            };
        }

        @Override
        public int getCountByBuilderType(SlotType type) {
            return switch (type) {
                case INPUT -> inputSlots;
                case OUTPUT -> outputSlots;
                case BATTERY -> hasBatterySlot ? 1 : 0;
                case UPGRADE -> upgradeSlots;
                default -> 0;
            };
        }

        @Override
        public int[] getSlotsOfBuilderType(SlotType type) {
            int start = getStart(type);
            int count = getCountByBuilderType(type);
            if (start < 0 || count <= 0) return new int[0];
            int[] slots = new int[count];
            for (int i = 0; i < count; i++) {
                slots[i] = start + i;
            }
            return slots;
        }

        @Override
        public boolean isType(int slot, SlotType type) {
            return getSlotType(slot) == type;
        }
    }

    /**
     * 支持特殊槽位的槽位布局实现
     */
    private static class SlotLayoutWithSpecialsImpl implements ISlotLayout {
        private final int inputSlots;
        private final int outputSlots;
        private final boolean hasBatterySlot;
        private final int upgradeSlots;
        private final ISlotLayout.SlotType[] specialSlots;
        private final int specialSlotsStart;

        public SlotLayoutWithSpecialsImpl(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, ISlotLayout.SlotType[] specialSlots) {
            this.inputSlots = inputSlots;
            this.outputSlots = outputSlots;
            this.hasBatterySlot = hasBatterySlot;
            this.upgradeSlots = upgradeSlots;
            this.specialSlots = specialSlots != null ? specialSlots : new ISlotLayout.SlotType[0];
            this.specialSlotsStart = inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots;
        }

        @Override
        public int getTotalSlots() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots + specialSlots.length;
        }

        @Override
        public int getInputSlotCount() { return inputSlots; }

        @Override
        public int getOutputSlotCount() { return outputSlots; }

        @Override
        public int getBatterySlotIndex() { return hasBatterySlot ? inputSlots + outputSlots : -1; }

        @Override
        public int getUpgradeSlotStart() { return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0); }

        @Override
        public int getUpgradeSlotCount() { return upgradeSlots; }

        @Override
        public boolean hasBatterySlot() { return hasBatterySlot; }

        @Override
        public boolean hasFluidTanks() { return false; }

        @Override
        public int getFluidTankCount() { return 0; }

        @Override
        public int getFluidTankCapacity() { return 0; }

        @Override
        public SlotType getSlotType(int slot) {
            if (slot < 0 || slot >= getTotalSlots()) return null;
            if (slot < inputSlots) return SlotType.INPUT;
            slot -= inputSlots;
            if (slot < outputSlots) return SlotType.OUTPUT;
            slot -= outputSlots;
            if (hasBatterySlot && slot == 0) return SlotType.BATTERY;
            if (hasBatterySlot) slot -= 1;
            if (slot < upgradeSlots) return SlotType.UPGRADE;
            slot -= upgradeSlots;
            if (slot < specialSlots.length) return specialSlots[slot];
            return SlotType.EXTRA;
        }

        @Override
        public int getStart(SlotType type) {
            return switch (type) {
                case INPUT -> 0;
                case OUTPUT -> inputSlots;
                case BATTERY -> hasBatterySlot ? inputSlots + outputSlots : -1;
                case UPGRADE -> inputSlots + outputSlots + (hasBatterySlot ? 1 : 0);
                default -> {
                    // 查找特殊槽位中该类型的起始位置
                    for (int i = 0; i < specialSlots.length; i++) {
                        if (specialSlots[i] == type) {
                            yield specialSlotsStart + i;
                        }
                    }
                    yield -1;
                }
            };
        }

        @Override
        public int getCountByBuilderType(SlotType type) {
            int count = switch (type) {
                case INPUT -> inputSlots;
                case OUTPUT -> outputSlots;
                case BATTERY -> hasBatterySlot ? 1 : 0;
                case UPGRADE -> upgradeSlots;
                default -> 0;
            };
            for (SlotType st : specialSlots) {
                if (st == type) count++;
            }
            return count;
        }

        @Override
        public int[] getSlotsOfBuilderType(SlotType type) {
            int count = getCountByBuilderType(type);
            if (count <= 0) return new int[0];

            if (type == SlotType.INPUT || type == SlotType.OUTPUT || type == SlotType.BATTERY || type == SlotType.UPGRADE) {
                int start = getStart(type);
                if (start < 0) return new int[0];
                int[] slots = new int[count];
                for (int i = 0; i < count; i++) {
                    slots[i] = start + i;
                }
                return slots;
            }

            int[] slots = new int[count];
            int idx = 0;
            for (int i = 0; i < specialSlots.length; i++) {
                if (specialSlots[i] == type) {
                    slots[idx++] = specialSlotsStart + i;
                }
            }
            return slots;
        }

        @Override
        public boolean isType(int slot, SlotType type) {
            return getSlotType(slot) == type;
        }
    }

    private static class MachineDefaultsImpl implements IMachineDefaults {
        private final ICableTier tier;

        public MachineDefaultsImpl(ICableTier tier) {
            this.tier = tier;
        }

        @Override
        public long getDefaultCapacity() {
            return switch (tier.getTier()) {
                case 0 -> 10000;      // LV
                case 1 -> 50000;      // MV
                case 2 -> 200000;     // HV
                case 3 -> 1000000;    // EV
                case 4 -> 5000000;    // IV
                case 5 -> 20000000;   // LuV
                case 6 -> 80000000;   // ZPM
                case 7 -> 320000000;  // UV
                case 8 -> 1280000000L;  // UHV
                case 9 -> 5120000000L;  // UEV
                case 10 -> 20480000000L; // UIV
                case 11 -> 81920000000L; // UXV
                case 12 -> 327680000000L;// OpV
                case 13 -> 1310720000000L;// MAX
                default -> 10000;
            };
        }

        @Override
        public long getDefaultMaxReceive() {
            return tier.getPowerRating();
        }

        @Override
        public long getDefaultMaxExtract() {
            return getDefaultMaxReceive();
        }

        @Override
        public long getDefaultEnergyPerTick() {
            return switch (tier.getTier()) {
                case 0 -> 10;
                case 1 -> 50;
                case 2 -> 200;
                case 3 -> 800;
                case 4 -> 3200;
                case 5 -> 12800;
                case 6 -> 51200;
                case 7 -> 204800;
                case 8 -> 819200;
                case 9 -> 3276800;
                case 10 -> 13107200;
                case 11 -> 52428800;
                case 12 -> 209715200;
                case 13 -> 838860800;
                default -> 10;
            };
        }

        @Override
        public int getDefaultProcessTime() {
            return switch (tier.getTier()) {
                case 0 -> 400;
                case 1 -> 300;
                case 2 -> 200;
                case 3 -> 100;
                case 4 -> 50;
                case 5 -> 25;
                case 6 -> 12;
                case 7, 8, 9, 10, 11, 12, 13 -> 10;
                default -> 400;
            };
        }

        @Override
        public int getDefaultUpgradeSlots() {
            return switch (tier.getTier()) {
                case 0, 1 -> 4;
                case 2, 3 -> 6;
                case 4, 5 -> 8;
                case 6, 7 -> 10;
                case 8, 9, 10, 11, 12, 13 -> 12;
                default -> 4;
            };
        }
    }

    // ========== 机器定义实现 ==========

    private static class MachineDefinitionImpl implements IMachineDefinition {
        private final String id;
        private final String modId;
        private final String translationKey;
        private final BlockEntityType<?> blockEntityType;
        private final MachinePropertiesImpl properties;
        private final IMachineBuilderAPI.MachineConfiguration configuration;

        public MachineDefinitionImpl(String id, String modId, String translationKey,
                                     BlockEntityType<?> blockEntityType, MachinePropertiesImpl properties,
                                     IMachineBuilderAPI.MachineConfiguration configuration) {
            this.id = id;
            this.modId = modId;
            this.translationKey = translationKey;
            this.blockEntityType = blockEntityType;
            this.properties = properties;
            this.configuration = configuration;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getModId() { return modId; }

        @Override
        public String getTranslationKey() { return translationKey; }

        @Override
        public BlockEntityType<?> getBlockEntityType() { return blockEntityType; }

        @Override
        public IMachineProperties getProperties() { return properties; }

        @Override
        public MachineConfiguration getConfiguration() { return configuration; }
    }

    private static class MachinePropertiesImpl implements IMachineProperties {
        private final long energyCapacity;
        private final long maxReceive;
        private final long maxExtract;
        private final long energyPerTick;
        private final int processTime;
        private final ICableTier cableTier;
        private final ISlotLayout slotLayout;
        private final boolean supportsUpgrades;
        private final boolean supportsFluids;

        public MachinePropertiesImpl(long energyCapacity, long maxReceive, long maxExtract,
                                     long energyPerTick, int processTime, ICableTier cableTier,
                                     ISlotLayout slotLayout, boolean supportsUpgrades, boolean supportsFluids) {
            this.energyCapacity = energyCapacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            this.energyPerTick = energyPerTick;
            this.processTime = processTime;
            this.cableTier = cableTier;
            this.slotLayout = slotLayout;
            this.supportsUpgrades = supportsUpgrades;
            this.supportsFluids = supportsFluids;
        }

        @Override
        public long getEnergyCapacity() { return energyCapacity; }

        @Override
        public long getMaxReceive() { return maxReceive; }

        @Override
        public long getMaxExtract() { return maxExtract; }

        @Override
        public long getEnergyPerTick() { return energyPerTick; }

        @Override
        public int getProcessTime() { return processTime; }

        @Override
        public ICableTier getCableTier() { return cableTier; }

        @Override
        public ISlotLayout getSlotLayout() { return slotLayout; }

        @Override
        public boolean supportsUpgrades() { return supportsUpgrades; }

        @Override
        public boolean supportsFluids() { return supportsFluids; }
    }

    // ========== 电力机器构建器实现 ==========

    private static class ElectricMachineBuilderImpl implements IElectricMachineBuilder {
        private String name = "unnamed_machine";
        private String translationKey = "block.unnamed.machine";
        private net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties =
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops();

        private long energyCapacity = 10000;
        private long maxReceive = 32;
        private long maxExtract = 32;
        private long energyPerTick = 10;
        private ICableTier cableTier = IEnergyConductor.LV_TIER;
        private int processTime = 200;
        private net.minecraft.world.item.crafting.RecipeType<?> recipeType = null;
        private boolean supportsUpgrades = false;
        private boolean supportsFluids = false;

        // 跟踪用户是否显式设置了能量相关参数
        // 用于 setCableTier() 判断是否应该覆盖这些值
        private boolean energyCapacitySet = false;
        private boolean maxReceiveSet = false;
        private boolean maxExtractSet = false;

        private int inputSlots = 1;
        private int outputSlots = 1;
        private boolean hasBatterySlot = false;
        private int upgradeSlots = 0;
        private int fluidTanks = 0;
        private int tankCapacity = 0;

        private MachineWorkCallback onWorkStart;
        private MachineWorkCompleteCallback onWorkComplete;
        private MachineTickCallback onTick;
        private net.minecraft.world.level.block.entity.BlockEntityType<?> entityType;
        private com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType;

        @Override
        public IElectricMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public IElectricMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType) {
            this.machineType = machineType;
            return this;
        }

        @Override
        public IElectricMachineBuilder setName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            this.name = name;
            return this;
        }

        @Override
        public IElectricMachineBuilder setTranslationKey(String translationKey) {
            if (translationKey == null || translationKey.isEmpty()) {
                throw new IllegalArgumentException("translationKey cannot be null or empty");
            }
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public IElectricMachineBuilder setBlockProperties(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            if (properties == null) {
                throw new IllegalArgumentException("properties cannot be null");
            }
            this.blockProperties = properties;
            return this;
        }

        @Override
        public IElectricMachineBuilder setEnergyCapacity(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("energyCapacity must be positive");
            }
            this.energyCapacity = capacity;
            this.energyCapacitySet = true;
            return this;
        }

        @Override
        public IElectricMachineBuilder setMaxReceive(long maxReceive) {
            if (maxReceive < 0) {
                throw new IllegalArgumentException("maxReceive cannot be negative");
            }
            this.maxReceive = maxReceive;
            this.maxReceiveSet = true;
            return this;
        }

        @Override
        public IElectricMachineBuilder setMaxExtract(long maxExtract) {
            if (maxExtract < 0) {
                throw new IllegalArgumentException("maxExtract cannot be negative");
            }
            this.maxExtract = maxExtract;
            this.maxExtractSet = true;
            return this;
        }

        @Override
        public IElectricMachineBuilder setEnergyPerTick(long energyPerTick) {
            if (energyPerTick <= 0) {
                throw new IllegalArgumentException("energyPerTick must be positive");
            }
            this.energyPerTick = energyPerTick;
            return this;
        }

        @Override
        public IElectricMachineBuilder setCableTier(ICableTier tier) {
            if (tier == null) {
                throw new IllegalArgumentException("tier cannot be null");
            }
            this.cableTier = toCableTier(tier);
            if (!energyCapacitySet) {
                this.energyCapacity = this.cableTier.getPowerRating() * 100;
            }
            if (!maxReceiveSet) {
                this.maxReceive = this.cableTier.getPowerRating();
            }
            if (!maxExtractSet) {
                this.maxExtract = this.cableTier.getPowerRating();
            }
            return this;
        }

        @Override
        public IElectricMachineBuilder useDefaultsForTier(ICableTier tier) {
            if (tier == null) {
                throw new IllegalArgumentException("tier cannot be null");
            }
            this.cableTier = toCableTier(tier);
            MachineDefaultsImpl defaults = new MachineDefaultsImpl(this.cableTier);
            this.energyCapacity = defaults.getDefaultCapacity();
            this.maxReceive = defaults.getDefaultMaxReceive();
            this.maxExtract = defaults.getDefaultMaxExtract();
            this.energyPerTick = defaults.getDefaultEnergyPerTick();
            this.processTime = defaults.getDefaultProcessTime();
            this.upgradeSlots = defaults.getDefaultUpgradeSlots();
            return this;
        }

        @Override
        public IElectricMachineBuilder setProcessTime(int processTime) {
            if (processTime <= 0) {
                throw new IllegalArgumentException("processTime must be positive");
            }
            this.processTime = processTime;
            return this;
        }

        @Override
        public IElectricMachineBuilder setRecipeType(net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
            this.recipeType = recipeType;
            return this;
        }

        @Override
        public IElectricMachineBuilder setSupportsUpgrades(boolean supportsUpgrades) {
            this.supportsUpgrades = supportsUpgrades;
            return this;
        }

        @Override
        public IElectricMachineBuilder addInputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.inputSlots = Math.max(this.inputSlots, index + 1);
            return this;
        }

        @Override
        public IElectricMachineBuilder addOutputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.outputSlots = Math.max(this.outputSlots, index + 1);
            return this;
        }

        @Override
        public IElectricMachineBuilder addBatterySlot(int index) {
            this.hasBatterySlot = true;
            return this;
        }

        @Override
        public IElectricMachineBuilder addUpgradeSlots(int startIndex, int count) {
            if (startIndex < 0 || count < 0) {
                throw new IllegalArgumentException("slot index and count cannot be negative");
            }
            this.supportsUpgrades = true;
            this.upgradeSlots = count;
            return this;
        }

        @Override
        public IElectricMachineBuilder useStandardLayout(int inputSlots, int outputSlots, boolean hasBattery, int upgradeSlots) {
            if (inputSlots < 0 || outputSlots < 0 || upgradeSlots < 0) {
                throw new IllegalArgumentException("slot counts cannot be negative");
            }
            this.inputSlots = inputSlots;
            this.outputSlots = outputSlots;
            this.hasBatterySlot = hasBattery;
            this.upgradeSlots = upgradeSlots;
            this.supportsUpgrades = upgradeSlots > 0;
            return this;
        }

        @Override
        public IElectricMachineBuilder setSupportsFluids(boolean supportsFluids) {
            this.supportsFluids = supportsFluids;
            return this;
        }

        @Override
        public IElectricMachineBuilder addFluidTank(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("tank capacity must be positive");
            }
            this.supportsFluids = true;
            this.fluidTanks++;
            this.tankCapacity = Math.max(this.tankCapacity, capacity);
            return this;
        }

        @Override
        public IElectricMachineBuilder onWorkStart(MachineWorkCallback callback) {
            this.onWorkStart = callback;
            return this;
        }

        @Override
        public IElectricMachineBuilder onWorkComplete(MachineWorkCompleteCallback callback) {
            this.onWorkComplete = callback;
            return this;
        }

        @Override
        public IElectricMachineBuilder onTick(MachineTickCallback callback) {
            this.onTick = callback;
            return this;
        }

        @Override
        public IMachineDefinition build() {
            return buildInternal("generic");
        }

        @Override
        public IMachineDefinition buildAndRegister(String modId) {
            if (modId == null || modId.isEmpty()) {
                throw new IllegalArgumentException("modId cannot be null or empty");
            }
            IMachineDefinition definition = buildInternal(modId);
            MioIcifAPI.instance().getMachineBuilderAPI().registerMachine(modId, definition);
            return definition;
        }

        private IMachineDefinition buildInternal(String modId) {
            validateConfiguration();

            ISlotLayout layout = new SlotLayoutImpl(
                inputSlots, outputSlots, hasBatterySlot, upgradeSlots, fluidTanks, tankCapacity
            );

            MachinePropertiesImpl properties = new MachinePropertiesImpl(
                energyCapacity, maxReceive, maxExtract, energyPerTick,
                processTime, cableTier, layout, supportsUpgrades, supportsFluids
            );

            IMachineBuilderAPI.MachineConfiguration config = getConfiguration();

            IMachineDefinition definition = new MachineDefinitionImpl(
                name, modId, translationKey, entityType, properties, config
            );
            
            // 注册回调到全局注册表，供 GenericMachineBlockEntity.onLoad() 时注入
            if (entityType != null && (onWorkStart != null || onWorkComplete != null || onTick != null)) {
                MachineBuilderAPIImpl.registerCallbacks(entityType, 
                    new MachineCallbacks(onWorkStart, onWorkComplete, onTick));
            }
            
            return definition;
        }

        private void validateConfiguration() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("machine name must be set");
            }
            if (energyCapacity <= 0) {
                throw new IllegalStateException("energyCapacity must be positive");
            }
            if (processTime <= 0) {
                throw new IllegalStateException("processTime must be positive");
            }
            if (inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots == 0) {
                throw new IllegalStateException("machine must have at least one slot");
            }
            if (entityType == null) {
                throw new IllegalStateException("entityType must be set via withEntityType() before build(). " +
                    "NeoForge requires BlockEntityType to be registered during mod initialization. " +
                    "Example: builder.withEntityType(MY_MACHINE_BLOCK_ENTITY_TYPE.get())");
            }
        }

        /**
         * 获取机器配置，供外部创建 BlockEntityType 使用
         */
        public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
            return new IMachineBuilderAPI.MachineConfiguration(
                name, translationKey, blockProperties,
                energyCapacity, maxReceive, maxExtract, energyPerTick,
                cableTier, processTime, recipeType,
                supportsUpgrades, supportsFluids,
                inputSlots, outputSlots, hasBatterySlot, upgradeSlots,
                fluidTanks, tankCapacity, machineType
            );
        }
    }

    // ========== 发电机构建器实现 ==========

    private static class GeneratorBuilderImpl implements IGeneratorBuilder {
        private String name = "unnamed_generator";
        private String translationKey = "block.unnamed.generator";
        private net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties =
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops();

        private long generationRate = 10;
        private long energyCapacity = 10000;
        private long maxExtract = 32;
        private ICableTier cableTier = IEnergyConductor.LV_TIER;
        private GeneratorType generatorType = GeneratorType.THERMAL;
        private boolean requiresSky = false;
        private boolean dayOnly = false;
        private boolean nightOnly = false;
        private boolean weatherDependent = false;
        private boolean requiresFuel = true;
        private boolean requiresFluidFuel = false;
        private boolean requiresRotor = false;
        private double baseEfficiency = 100.0;
        private boolean heightEfficiency = false;
        private boolean biomeEfficiency = false;

        private int fuelSlots = 1;
        private boolean hasBatterySlot = false;
        private int fluidTanks = 0;
        private int tankCapacity = 0;

        private boolean energyCapacitySet = false;
        private boolean maxExtractSet = false;

        private GenerationCallback onGenerationStart;
        private GenerationCallback onGenerate;
        private FuelConsumeCallback onFuelConsume;
        private net.minecraft.world.level.block.entity.BlockEntityType<?> entityType;
        private com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType;

        @Override
        public IGeneratorBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public IGeneratorBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType) {
            this.machineType = machineType;
            return this;
        }

        @Override
        public IGeneratorBuilder setName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            this.name = name;
            return this;
        }

        @Override
        public IGeneratorBuilder setTranslationKey(String translationKey) {
            if (translationKey == null || translationKey.isEmpty()) {
                throw new IllegalArgumentException("translationKey cannot be null or empty");
            }
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public IGeneratorBuilder setBlockProperties(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            if (properties == null) {
                throw new IllegalArgumentException("properties cannot be null");
            }
            this.blockProperties = properties;
            return this;
        }

        @Override
        public IGeneratorBuilder setGenerationRate(long generationRate) {
            if (generationRate <= 0) {
                throw new IllegalArgumentException("generationRate must be positive");
            }
            this.generationRate = generationRate;
            return this;
        }

        @Override
        public IGeneratorBuilder setEnergyCapacity(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("energyCapacity must be positive");
            }
            this.energyCapacity = capacity;
            this.energyCapacitySet = true;
            return this;
        }

        @Override
        public IGeneratorBuilder setMaxExtract(long maxExtract) {
            if (maxExtract < 0) {
                throw new IllegalArgumentException("maxExtract cannot be negative");
            }
            this.maxExtract = maxExtract;
            this.maxExtractSet = true;
            return this;
        }

        @Override
        public IGeneratorBuilder setCableTier(ICableTier tier) {
            if (tier == null) {
                throw new IllegalArgumentException("tier cannot be null");
            }
            this.cableTier = toCableTier(tier);
            if (!this.energyCapacitySet) {
                this.energyCapacity = this.cableTier.getPowerRating() * 100;
            }
            if (!this.maxExtractSet) {
                this.maxExtract = this.cableTier.getPowerRating();
            }
            return this;
        }

        @Override
        public IGeneratorBuilder useDefaultsForTier(ICableTier tier) {
            if (tier == null) {
                throw new IllegalArgumentException("tier cannot be null");
            }
            this.cableTier = toCableTier(tier);
            MachineDefaultsImpl defaults = new MachineDefaultsImpl(this.cableTier);
            this.energyCapacity = defaults.getDefaultCapacity();
            this.maxExtract = defaults.getDefaultMaxExtract();
            return this;
        }

        @Override
        public IGeneratorBuilder setGeneratorType(GeneratorType type) {
            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }
            this.generatorType = type;
            return this;
        }

        @Override
        public IGeneratorBuilder setRequiresSky(boolean requiresSky) {
            this.requiresSky = requiresSky;
            return this;
        }

        @Override
        public IGeneratorBuilder setDayOnly(boolean dayOnly) {
            this.dayOnly = dayOnly;
            this.nightOnly = false;
            return this;
        }

        @Override
        public IGeneratorBuilder setNightOnly(boolean nightOnly) {
            this.nightOnly = nightOnly;
            this.dayOnly = false;
            return this;
        }

        @Override
        public IGeneratorBuilder setWeatherDependent(boolean weatherDependent) {
            this.weatherDependent = weatherDependent;
            return this;
        }

        @Override
        public IGeneratorBuilder setRequiresFuel(boolean requiresFuel) {
            this.requiresFuel = requiresFuel;
            return this;
        }

        @Override
        public IGeneratorBuilder setRequiresFluidFuel(boolean requiresFluidFuel) {
            this.requiresFluidFuel = requiresFluidFuel;
            return this;
        }

        @Override
        public IGeneratorBuilder setRequiresRotor(boolean requiresRotor) {
            this.requiresRotor = requiresRotor;
            return this;
        }

        @Override
        public IGeneratorBuilder addFuelSlot(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.fuelSlots = Math.max(this.fuelSlots, index + 1);
            return this;
        }

        @Override
        public IGeneratorBuilder addBatterySlot(int index) {
            this.hasBatterySlot = true;
            return this;
        }

        @Override
        public IGeneratorBuilder addFluidTank(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("tank capacity must be positive");
            }
            this.fluidTanks++;
            this.tankCapacity = Math.max(this.tankCapacity, capacity);
            return this;
        }

        @Override
        public IGeneratorBuilder setBaseEfficiency(double efficiency) {
            if (efficiency <= 0 || efficiency > 100) {
                throw new IllegalArgumentException("efficiency must be between 0 and 100");
            }
            this.baseEfficiency = efficiency;
            return this;
        }

        @Override
        public IGeneratorBuilder setHeightEfficiency(boolean heightEfficiency) {
            this.heightEfficiency = heightEfficiency;
            return this;
        }

        @Override
        public IGeneratorBuilder setBiomeEfficiency(boolean biomeEfficiency) {
            this.biomeEfficiency = biomeEfficiency;
            return this;
        }

        @Override
        public IGeneratorBuilder onGenerationStart(GenerationCallback callback) {
            this.onGenerationStart = callback;
            return this;
        }

        @Override
        public IGeneratorBuilder onGenerate(GenerationCallback callback) {
            this.onGenerate = callback;
            return this;
        }

        @Override
        public IGeneratorBuilder onFuelConsume(FuelConsumeCallback callback) {
            this.onFuelConsume = callback;
            return this;
        }

        @Override
        public IMachineDefinition build() {
            return buildInternal("generic");
        }

        @Override
        public IMachineDefinition buildAndRegister(String modId) {
            if (modId == null || modId.isEmpty()) {
                throw new IllegalArgumentException("modId cannot be null or empty");
            }
            IMachineDefinition definition = buildInternal(modId);
            MioIcifAPI.instance().getMachineBuilderAPI().registerMachine(modId, definition);
            return definition;
        }

        private IMachineDefinition buildInternal(String modId) {
            validateConfiguration();

            ISlotLayout layout = new SlotLayoutImpl(
                0, 0, hasBatterySlot, 0, fluidTanks, tankCapacity
            );

            MachinePropertiesImpl properties = new MachinePropertiesImpl(
                energyCapacity, 0, maxExtract, generationRate,
                0, cableTier, layout, false, requiresFluidFuel
            );

            IMachineBuilderAPI.MachineConfiguration config = getConfiguration();

            return new MachineDefinitionImpl(
                name, modId, translationKey, entityType, properties, config
            );
        }

        private void validateConfiguration() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("generator name must be set");
            }
            if (generationRate <= 0) {
                throw new IllegalStateException("generationRate must be positive");
            }
            if (energyCapacity <= 0) {
                throw new IllegalStateException("energyCapacity must be positive");
            }
            if (entityType == null) {
                throw new IllegalStateException("entityType must be set via withEntityType() before build(). " +
                    "NeoForge requires BlockEntityType to be registered during mod initialization. " +
                    "Example: builder.withEntityType(MY_GENERATOR_BLOCK_ENTITY_TYPE.get())");
            }
        }

        /**
         * 获取机器配置，供外部创建 BlockEntityType 使用
         */
        public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
            return new IMachineBuilderAPI.MachineConfiguration(
                name, translationKey, blockProperties,
                energyCapacity, 0, maxExtract, generationRate,
                cableTier, 0, null,
                false, requiresFluidFuel,
                0, 0, hasBatterySlot, 0,
                fluidTanks, tankCapacity, machineType
            );
        }
    }

    // ========== 热能机器构建器实现 ==========

    private static class HeatMachineBuilderImpl implements IHeatMachineBuilder {
        private String name = "unnamed_heat_machine";
        private String translationKey = "block.unnamed.heat_machine";
        private net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties =
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops();

        private long heatCapacity = 10000;
        private long heatOutput = 10;
        private long heatTransferRate = 10;
        private long heatInputRate = 10;
        private long operatingTemperature = 100;
        private long maxTemperature = 1000;
        private boolean requiresFuel = true;
        private boolean requiresCooling = false;
        private String coolantType = "water";

        private int inputSlots = 1;
        private int outputSlots = 1;
        private int fuelSlots = 1;
        private int fluidTanks = 0;
        private int tankCapacity = 0;

        private HeatCallback onHeatGenerate;
        private OverheatCallback onOverheat;
        private net.minecraft.world.level.block.entity.BlockEntityType<?> entityType;
        private com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType;

        @Override
        public IHeatMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public IHeatMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType) {
            this.machineType = machineType;
            return this;
        }

        @Override
        public IHeatMachineBuilder setName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            this.name = name;
            return this;
        }

        @Override
        public IHeatMachineBuilder setTranslationKey(String translationKey) {
            if (translationKey == null || translationKey.isEmpty()) {
                throw new IllegalArgumentException("translationKey cannot be null or empty");
            }
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public IHeatMachineBuilder setBlockProperties(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            if (properties == null) {
                throw new IllegalArgumentException("properties cannot be null");
            }
            this.blockProperties = properties;
            return this;
        }

        @Override
        public IHeatMachineBuilder setHeatCapacity(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("heatCapacity must be positive");
            }
            this.heatCapacity = capacity;
            return this;
        }

        @Override
        public IHeatMachineBuilder setHeatOutput(long heatOutput) {
            if (heatOutput <= 0) {
                throw new IllegalArgumentException("heatOutput must be positive");
            }
            this.heatOutput = heatOutput;
            return this;
        }

        @Override
        public IHeatMachineBuilder setHeatTransferRate(long transferRate) {
            if (transferRate <= 0) {
                throw new IllegalArgumentException("transferRate must be positive");
            }
            this.heatTransferRate = transferRate;
            return this;
        }

        @Override
        public IHeatMachineBuilder setHeatInputRate(long inputRate) {
            if (inputRate <= 0) {
                throw new IllegalArgumentException("inputRate must be positive");
            }
            this.heatInputRate = inputRate;
            return this;
        }

        @Override
        public IHeatMachineBuilder setOperatingTemperature(long temperature) {
            if (temperature <= 0) {
                throw new IllegalArgumentException("operatingTemperature must be positive");
            }
            this.operatingTemperature = temperature;
            return this;
        }

        @Override
        public IHeatMachineBuilder setMaxTemperature(long maxTemperature) {
            if (maxTemperature <= 0) {
                throw new IllegalArgumentException("maxTemperature must be positive");
            }
            this.maxTemperature = maxTemperature;
            return this;
        }

        @Override
        public IHeatMachineBuilder setRequiresFuel(boolean requiresFuel) {
            this.requiresFuel = requiresFuel;
            return this;
        }

        @Override
        public IHeatMachineBuilder setRequiresCooling(boolean requiresCooling) {
            this.requiresCooling = requiresCooling;
            return this;
        }

        @Override
        public IHeatMachineBuilder setCoolantType(String fluidId) {
            if (fluidId == null || fluidId.isEmpty()) {
                throw new IllegalArgumentException("fluidId cannot be null or empty");
            }
            this.coolantType = fluidId;
            return this;
        }

        @Override
        public IHeatMachineBuilder addInputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.inputSlots = Math.max(this.inputSlots, index + 1);
            return this;
        }

        @Override
        public IHeatMachineBuilder addOutputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.outputSlots = Math.max(this.outputSlots, index + 1);
            return this;
        }

        @Override
        public IHeatMachineBuilder addFuelSlot(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.fuelSlots = Math.max(this.fuelSlots, index + 1);
            return this;
        }

        @Override
        public IHeatMachineBuilder addFluidTank(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("tank capacity must be positive");
            }
            this.fluidTanks++;
            this.tankCapacity = Math.max(this.tankCapacity, capacity);
            return this;
        }

        @Override
        public IHeatMachineBuilder onHeatGenerate(HeatCallback callback) {
            this.onHeatGenerate = callback;
            return this;
        }

        @Override
        public IHeatMachineBuilder onOverheat(OverheatCallback callback) {
            this.onOverheat = callback;
            return this;
        }

        @Override
        public IMachineDefinition build() {
            return buildInternal("generic");
        }

        @Override
        public IMachineDefinition buildAndRegister(String modId) {
            if (modId == null || modId.isEmpty()) {
                throw new IllegalArgumentException("modId cannot be null or empty");
            }
            IMachineDefinition definition = buildInternal(modId);
            MioIcifAPI.instance().getMachineBuilderAPI().registerMachine(modId, definition);
            return definition;
        }

        private IMachineDefinition buildInternal(String modId) {
            validateConfiguration();

            ISlotLayout layout = new SlotLayoutImpl(
                inputSlots, outputSlots, false, 0, fluidTanks, tankCapacity
            );

            // 热能机器使用 HU 系统，EU 相关字段为 0 是合理的
            // 但应该将热能参数存储到 properties 中以便查询
            MachinePropertiesImpl properties = new MachinePropertiesImpl(
                0, 0, 0, 0, 0, null, layout, false, fluidTanks > 0
            );

            IMachineBuilderAPI.MachineConfiguration config = getConfiguration();

            return new MachineDefinitionImpl(
                name, modId, translationKey, entityType, properties, config
            );
        }

        private void validateConfiguration() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("machine name must be set");
            }
            if (heatCapacity <= 0) {
                throw new IllegalStateException("heatCapacity must be positive");
            }
            if (maxTemperature <= operatingTemperature) {
                throw new IllegalStateException("maxTemperature must be greater than operatingTemperature");
            }
            if (entityType == null) {
                throw new IllegalStateException("entityType must be set via withEntityType() before build(). " +
                    "NeoForge requires BlockEntityType to be registered during mod initialization. " +
                    "Example: builder.withEntityType(MY_HEAT_MACHINE_BLOCK_ENTITY_TYPE.get())");
            }
        }

        /**
         * 获取机器配置，供外部创建 BlockEntityType 使用
         */
        public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
            return new IMachineBuilderAPI.MachineConfiguration(
                name, translationKey, blockProperties,
                0, 0, 0, 0,
                null, 0, null,
                false, fluidTanks > 0,
                inputSlots, outputSlots, false, 0,
                fluidTanks, tankCapacity, machineType
            );
        }
    }

    // ========== 动能机器构建器实现 ==========

    private static class KineticMachineBuilderImpl implements IKineticMachineBuilder {
        private String name = "unnamed_kinetic_machine";
        private String translationKey = "block.unnamed.kinetic_machine";
        private net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties =
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops();

        private long kineticCapacity = 10000;
        private long kineticOutput = 10;
        private long kineticTransferRate = 10;
        private long kineticInputRate = 10;
        private boolean requiresRotor = false;
        private RotorType rotorType = RotorType.WIND;
        private int rotorWearRate = 1;

        private int rotorSlots = 0;
        private boolean hasBatterySlot = false;
        private int inputSlots = 0;
        private int outputSlots = 0;

        private KineticCallback onKineticGenerate;
        private RotorBreakCallback onRotorBreak;
        private net.minecraft.world.level.block.entity.BlockEntityType<?> entityType;
        private com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType;

        @Override
        public IKineticMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        @Override
        public IKineticMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType) {
            this.machineType = machineType;
            return this;
        }

        @Override
        public IKineticMachineBuilder setName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            this.name = name;
            return this;
        }

        @Override
        public IKineticMachineBuilder setTranslationKey(String translationKey) {
            if (translationKey == null || translationKey.isEmpty()) {
                throw new IllegalArgumentException("translationKey cannot be null or empty");
            }
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public IKineticMachineBuilder setBlockProperties(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            if (properties == null) {
                throw new IllegalArgumentException("properties cannot be null");
            }
            this.blockProperties = properties;
            return this;
        }

        @Override
        public IKineticMachineBuilder setKineticCapacity(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("kineticCapacity must be positive");
            }
            this.kineticCapacity = capacity;
            return this;
        }

        @Override
        public IKineticMachineBuilder setKineticOutput(long kineticOutput) {
            if (kineticOutput <= 0) {
                throw new IllegalArgumentException("kineticOutput must be positive");
            }
            this.kineticOutput = kineticOutput;
            return this;
        }

        @Override
        public IKineticMachineBuilder setKineticTransferRate(long transferRate) {
            if (transferRate <= 0) {
                throw new IllegalArgumentException("transferRate must be positive");
            }
            this.kineticTransferRate = transferRate;
            return this;
        }

        @Override
        public IKineticMachineBuilder setKineticInputRate(long inputRate) {
            if (inputRate <= 0) {
                throw new IllegalArgumentException("inputRate must be positive");
            }
            this.kineticInputRate = inputRate;
            return this;
        }

        @Override
        public IKineticMachineBuilder setRequiresRotor(boolean requiresRotor) {
            this.requiresRotor = requiresRotor;
            return this;
        }

        @Override
        public IKineticMachineBuilder setRotorType(RotorType rotorType) {
            if (rotorType == null) {
                throw new IllegalArgumentException("rotorType cannot be null");
            }
            this.rotorType = rotorType;
            return this;
        }

        @Override
        public IKineticMachineBuilder setRotorWearRate(int wearRate) {
            if (wearRate < 0) {
                throw new IllegalArgumentException("wearRate cannot be negative");
            }
            this.rotorWearRate = wearRate;
            return this;
        }

        @Override
        public IKineticMachineBuilder addRotorSlot(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.requiresRotor = true;
            this.rotorSlots = Math.max(this.rotorSlots, index + 1);
            return this;
        }

        @Override
        public IKineticMachineBuilder addBatterySlot(int index) {
            this.hasBatterySlot = true;
            return this;
        }

        @Override
        public IKineticMachineBuilder addInputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.inputSlots = Math.max(this.inputSlots, index + 1);
            return this;
        }

        @Override
        public IKineticMachineBuilder addOutputSlot(int index, int maxStackSize) {
            if (index < 0) {
                throw new IllegalArgumentException("slot index cannot be negative");
            }
            this.outputSlots = Math.max(this.outputSlots, index + 1);
            return this;
        }

        @Override
        public IKineticMachineBuilder onKineticGenerate(KineticCallback callback) {
            this.onKineticGenerate = callback;
            return this;
        }

        @Override
        public IKineticMachineBuilder onRotorBreak(RotorBreakCallback callback) {
            this.onRotorBreak = callback;
            return this;
        }

        @Override
        public IMachineDefinition build() {
            return buildInternal("generic");
        }

        @Override
        public IMachineDefinition buildAndRegister(String modId) {
            if (modId == null || modId.isEmpty()) {
                throw new IllegalArgumentException("modId cannot be null or empty");
            }
            IMachineDefinition definition = buildInternal(modId);
            MioIcifAPI.instance().getMachineBuilderAPI().registerMachine(modId, definition);
            return definition;
        }

        private IMachineDefinition buildInternal(String modId) {
            validateConfiguration();

            ISlotLayout layout = new SlotLayoutImpl(
                inputSlots, outputSlots, hasBatterySlot, 0, 0, 0
            );

            MachinePropertiesImpl properties = new MachinePropertiesImpl(
                0, 0, 0, 0, 0, null, layout, false, false
            );

            IMachineBuilderAPI.MachineConfiguration config = getConfiguration();

            return new MachineDefinitionImpl(
                name, modId, translationKey, entityType, properties, config
            );
        }

        private void validateConfiguration() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("machine name must be set");
            }
            if (kineticCapacity <= 0) {
                throw new IllegalStateException("kineticCapacity must be positive");
            }
            if (entityType == null) {
                throw new IllegalStateException("entityType must be set via withEntityType() before build(). " +
                    "NeoForge requires BlockEntityType to be registered during mod initialization. " +
                    "Example: builder.withEntityType(MY_KINETIC_MACHINE_BLOCK_ENTITY_TYPE.get())");
            }
        }

        /**
         * 获取机器配置，供外部创建 BlockEntityType 使用
         */
        public IMachineBuilderAPI.MachineConfiguration getConfiguration() {
            return new IMachineBuilderAPI.MachineConfiguration(
                name, translationKey, blockProperties,
                0, 0, 0, 0,
                null, 0, null,
                false, false,
                inputSlots, outputSlots, hasBatterySlot, 0,
                0, 0, machineType
            );
        }
    }

    // ========== 储能方块构建器实现 ==========

    private static class EnergyContainerBuilderImpl implements IEnergyContainerBuilder {
        private String name = "unnamed_container";
        private String translationKey = "block.unnamed.container";
        private long energyCapacity = 40000;
        private long maxReceive = 32;
        private long maxExtract = 32;
        private ICableTier cableTier = IEnergyConductor.LV_TIER;
        private boolean chargepad = false;
        private long maxTransferPerItem = 512;

        @Override
        public IEnergyContainerBuilder setName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            this.name = name;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setTranslationKey(String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("translationKey cannot be null or empty");
            }
            this.translationKey = key;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setEnergyCapacity(long capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.energyCapacity = capacity;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setMaxReceive(long maxReceive) {
            if (maxReceive <= 0) {
                throw new IllegalArgumentException("maxReceive must be positive");
            }
            this.maxReceive = maxReceive;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setMaxExtract(long maxExtract) {
            if (maxExtract <= 0) {
                throw new IllegalArgumentException("maxExtract must be positive");
            }
            this.maxExtract = maxExtract;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setCableTier(ICableTier tier) {
            if (tier == null) {
                throw new IllegalArgumentException("tier cannot be null");
            }
            this.cableTier = tier;
            this.maxReceive = tier.getPowerRating();
            this.maxExtract = tier.getPowerRating();
            return this;
        }

        @Override
        public IEnergyContainerBuilder setChargepad(boolean chargepad) {
            this.chargepad = chargepad;
            return this;
        }

        @Override
        public IEnergyContainerBuilder setMaxTransferPerItem(long maxTransfer) {
            if (maxTransfer <= 0) {
                throw new IllegalArgumentException("maxTransfer must be positive");
            }
            this.maxTransferPerItem = maxTransfer;
            return this;
        }

        @Override
        public String getName() { return name; }

        @Override
        public long getEnergyCapacity() { return energyCapacity; }

        @Override
        public long getMaxReceive() { return maxReceive; }

        @Override
        public long getMaxExtract() { return maxExtract; }

        @Override
        public ICableTier getCableTier() { return cableTier; }

        @Override
        public boolean isChargepad() { return chargepad; }

        @Override
        public long getMaxTransferPerItem() { return maxTransferPerItem; }

        @Override
        public EnergyContainerConfiguration build() {
            validateConfiguration();
            return new EnergyContainerConfigImpl(name, translationKey, energyCapacity, maxReceive, maxExtract, cableTier, chargepad, maxTransferPerItem);
        }

        @Override
        public EnergyContainerConfiguration buildAndRegister(String modId) {
            EnergyContainerConfiguration config = build();
            LOGGER.info("Energy container builder: {} for mod {} (capacity={}, tier={}, chargepad={})",
                name, modId, energyCapacity, cableTier.getName(), chargepad);
            return config;
        }

        private void validateConfiguration() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("name must be set");
            }
            if (energyCapacity <= 0) {
                throw new IllegalStateException("energyCapacity must be positive");
            }
        }

        private static final class EnergyContainerConfigImpl implements EnergyContainerConfiguration {
            private final String name;
            private final String translationKey;
            private final long energyCapacity;
            private final long maxReceive;
            private final long maxExtract;
            private final ICableTier cableTier;
            private final boolean chargepad;
            private final long maxTransferPerItem;

            EnergyContainerConfigImpl(String name, String translationKey,
                long energyCapacity, long maxReceive, long maxExtract,
                ICableTier cableTier, boolean chargepad, long maxTransferPerItem) {
                this.name = name;
                this.translationKey = translationKey;
                this.energyCapacity = energyCapacity;
                this.maxReceive = maxReceive;
                this.maxExtract = maxExtract;
                this.cableTier = cableTier;
                this.chargepad = chargepad;
                this.maxTransferPerItem = maxTransferPerItem;
            }

            @Override public String getName() { return name; }
            @Override public String getTranslationKey() { return translationKey; }
            @Override public long getEnergyCapacity() { return energyCapacity; }
            @Override public long getMaxReceive() { return maxReceive; }
            @Override public long getMaxExtract() { return maxExtract; }
            @Override public ICableTier getCableTier() { return cableTier; }
            @Override public boolean isChargepad() { return chargepad; }
            @Override public long getMaxTransferPerItem() { return maxTransferPerItem; }
        }
    }
}