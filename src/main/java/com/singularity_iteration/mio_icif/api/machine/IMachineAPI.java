package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器 API（聚合接口）
 *
 * <p>此接口是所有机器子 API 的聚合入口，继承了以下子接口：
 * <ul>
 *   <li>{@link IMachineQueryAPI} - 机器检测、查询和批量检索</li>
 *   <li>{@link IMachineControlAPI} - 机器启停和状态控制</li>
 *   <li>{@link IMachineSlotAPI} - 槽位布局和物品操作</li>
 *   <li>{@link IMachineUpgradeOpsAPI} - 升级插件安装/移除</li>
 *   <li>{@link IUpgradeStatsAPI} - 升级统计信息查询</li>
 *   <li>{@link IGeneratorQueryAPI} - 发电机燃料/燃烧查询</li>
 *   <li>{@link IMultiblockReactorAPI} - 反应堆多方块结构管理</li>
 *   <li>{@link IRedstoneAPI} - 红石信号控制</li>
 * </ul>
 *
 * <p>建议附属模组根据需要使用具体的子接口，而非直接依赖此聚合接口。
 *
 * <p>使用示例：
 * <pre>{@code
 * IMachineAPI machineAPI = MioIcifAPI.instance().getMachineAPI();
 *
 * // 检查位置是否有机器
 * if (machineAPI.isMachine(world, pos)) {
 *     IMachineInfo info = machineAPI.getMachineInfo(world, pos);
 *     boolean working = info.isWorking();
 *     int progress = info.getProgress();
 * }
 * }</pre>
 */
public interface IMachineAPI
        extends IMachineQueryAPI, IMachineControlAPI, IMachineSlotAPI,
                IMachineUpgradeOpsAPI, IUpgradeStatsAPI, IGeneratorQueryAPI,
                IMultiblockReactorAPI, IRedstoneAPI {

    // ========== 内部类型 ==========

    /**
     * 机器类型枚举
     */
    enum MachineType {
        GENERATOR,      // 发电机
        PROCESSOR,      // 加工机器（压缩机、提取机等）
        SMELTER,        // 熔炉机
        MACERATOR,      // 打粉机
        COMPRESSOR,     // 压缩机
        EXTRACTOR,      // 提取机
        CENTRIFUGE,     // 离心机
        WASHER,         // 洗矿机
        RECYCLER,       // 回收机
        METAL_FORMER,   // 金属成型机
        ELECTROLYZER,   // 电解机
        BLENDER,        // 搅拌机
        CUTTER,         // 切割机
        WELDER,         // 焊接机
        LATHE,          // 车床
        ROLLING,        // 卷板机
        EXTRUDER,       // 压膜机
        FERMENTER,      // 发酵机
        CANNER,         // Canner
        FLUID_SOLID,    // 流体固体灌装机
        THERMAL_CENTRIFUGE, // 热能离心机
        LASER_ENGRAVER, // 激光雕刻机
        PRECISION_ASSEMBLER, // 精密组装机
        VACUUM_FREEZER, // 真空冷冻机
        PLASMA_FURNACE, // 等离子炉
        FUSION_REACTOR, // 聚变反应堆
        MATTER_FABRICATOR, // 物质生成机
        REPLICATOR,     // 复制机
        SCANNER,        // 扫描机
        TELEPORTER,     // 传送机
        PUMP,           // 泵
        MINER,          // 采矿机
        INDUCTION,      // 感应炉
        CONDENSER,      // 冷凝机
        TERRAFORMER,    // 地形改造机
        HARVESTER,      // 收割机
        MATRON,         // 物质复制机
        MAGNETIZER,     // 磁化机
        TESLA_COIL,     // 特斯拉线圈
        ADVANCED_MINER, // 高级采矿机
        TRANSFORMER,    // 变压器
        PIPE,           // 管道
        ENERGY_CONVERTER, // 能量转换器
        NEUTRON_POLYMERIZER, // 粒子聚合发生器
        CUSTOM          // 自定义类型
    }

    /**
     * 红石模式枚举
     */
    enum RedstoneMode {
        /** 无红石控制 */
        NONE(0),
        /** 严格满电时发出信号 (100%) */
        FULL(1),
        /** 能量在中间范围时发出信号 */
        PARTIAL(2),
        /** 能量未满时发出信号 */
        NOT_FULL(3),
        /** 严格空电时发出信号 (0%) */
        EMPTY(4),
        /** 接收到红石信号时停止输出 */
        INVERTED(5),
        /** 接收到红石信号时停止输出，或严格满电时继续输出 */
        CONDITIONAL(6),
        /** 接收到红石信号时才输出能量 */
        REDSTONE_ON(7);

        private final int id;

        RedstoneMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static RedstoneMode fromId(int id) {
            for (RedstoneMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return NONE;
        }
    }

    enum MetalFormerMode {
        ROLLING(0),
        CUTTING(1),
        EXTRUDING(2);

        private final int id;

        MetalFormerMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static MetalFormerMode fromId(int id) {
            for (MetalFormerMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return ROLLING;
        }
    }

    /**
     * 槽位类型枚举
     */
    enum SlotType implements ISlotType {
        INPUT,
        OUTPUT,
        UPGRADE,
        TOOL,
        SCANNER,
        MEMORY,
        FLUID_INPUT,
        FLUID_OUTPUT,
        FUEL,
        BATTERY,
        EXTRA,
        ROTOR,
        TURBINE,
        REACTOR,
        COIL,
        HEATING,
        NUCLEAR,
        DRILL,
        MINING_PIPE,
        FILTER,
        HEAT_CONDUCTOR,
        HAMMER_TOOL,
        HAMMER_INPUT,
        HAMMER_OUTPUT,
        CUTTER_TOOL,
        CUTTER_INPUT,
        CUTTER_OUTPUT,
        RTG_PELLET,
        CUSTOM;

        @Override
        public String getName() {
            return name().toLowerCase();
        }

        @Override
        public boolean isInput() {
            return this == INPUT || this == FLUID_INPUT || this == HAMMER_INPUT || this == CUTTER_INPUT;
        }

        @Override
        public boolean isOutput() {
            return this == OUTPUT || this == FLUID_OUTPUT || this == HAMMER_OUTPUT || this == CUTTER_OUTPUT;
        }

        @Override
        public boolean isBattery() {
            return this == BATTERY;
        }

        @Override
        public boolean isUpgrade() {
            return this == UPGRADE;
        }

        @Override
        public boolean isExtra() {
            return this == EXTRA;
        }
    }

    /**
     * 多方块结构信息接口
     */
    interface IMultiblockInfo {
        boolean isStructureValid();
        int getStructureBlockCount();
        java.util.Set<BlockPos> getStructureBlocks();
        long getFormationTime();
        java.util.Map<String, Object> getStructureData();
        java.util.Set<BlockPos> getRedstonePorts();
    }

    /**
     * 机器配置接口
     */
    interface IMachineConfiguration {
        MachineType getMachineType();
        ICableTier getCableTier();
        long getEnergyCapacity();
        long getMaxReceive();
        long getMaxExtract();
        int getBaseProgress();
        long getBaseEnergyPerTick();
        int getSlotCount();
    }

    // ========== 可扩展机器类型注册 ==========

    /**
     * 机器类型元数据记录。
     *
     * <p>当 {@link MachineType#CUSTOM} 不足以描述自定义机器时，
     * 可通过 {@link MachineTypeRegistry#register(MachineTypeInfo)} 注册带元数据的类型。
     *
     * @param id          唯一类型标识符（建议格式：{@code modid:type_name}）
     * @param displayName 显示名称（用于 HUD/日志）
     * @param parentType  父类型（用于分类和兼容性判断）
     * @param metadata    附加元数据
     */
    record MachineTypeInfo(
        String id,
        String displayName,
        MachineType parentType,
        Map<String, Object> metadata
    ) {
        /**
         * 简化构造器（无元数据）。
         */
        public MachineTypeInfo(String id, String displayName, MachineType parentType) {
            this(id, displayName, parentType, Collections.emptyMap());
        }
    }

    /**
     * 自定义机器类型注册表。
     *
     * <p>提供运行时注册和查询自定义机器类型的机制，
     * 解决 {@link MachineType} 枚举不可扩展的问题。
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 注册自定义类型
     * MachineTypeRegistry.register(new IMachineAPI.MachineTypeInfo(
     *     "mymod:quantum_assembler",
     *     "量子组装机",
     *     IMachineAPI.MachineType.PROCESSOR
     * ));
     *
     * // 查询类型信息
     * Optional<IMachineAPI.MachineTypeInfo> info = MachineTypeRegistry.get("mymod:quantum_assembler");
     * }</pre>
     */
    final class MachineTypeRegistry {
        private static final Map<String, MachineTypeInfo> REGISTRY = new ConcurrentHashMap<>();

        private MachineTypeRegistry() {
        }

        /**
         * 注册一个自定义机器类型。
         *
         * @param info 类型元数据
         * @return 之前的值（如果已存在），否则 null
         */
        public static MachineTypeInfo register(MachineTypeInfo info) {
            return REGISTRY.put(info.id(), info);
        }

        /**
         * 根据 ID 获取已注册的类型信息。
         *
         * @param id 类型 ID
         * @return 类型信息（可能为空）
         */
        public static java.util.Optional<MachineTypeInfo> get(String id) {
            return java.util.Optional.ofNullable(REGISTRY.get(id));
        }

        /**
         * 获取所有已注册的自定义类型。
         *
         * @return 不可变的注册表副本
         */
        public static Map<String, MachineTypeInfo> getAll() {
            return Collections.unmodifiableMap(REGISTRY);
        }

        /**
         * 检查指定 ID 的类型是否已注册。
         */
        public static boolean isRegistered(String id) {
            return REGISTRY.containsKey(id);
        }
    }
}