package com.miophas.singularity_iteration.core.api;



import com.miophas.singularity_iteration.core.api.block.IBlockAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.crop.ICropAPI;
import com.miophas.singularity_iteration.core.api.energy.IEnergyNetAPI;
import com.miophas.singularity_iteration.core.api.energy.IFECompatAPI;
import com.miophas.singularity_iteration.core.api.fluid.IFluidAPI;
import com.miophas.singularity_iteration.core.api.fluid.IFluidHandlerAPI;
import com.miophas.singularity_iteration.core.api.generator.IGeneratorAPI;
import com.miophas.singularity_iteration.core.api.heat.IHeatAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticAPI;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.reactor.IReactorAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeRegistrationAPI;
import com.miophas.singularity_iteration.core.api.registry.IMioIcifRegistries;
import com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI;
import com.miophas.singularity_iteration.core.api.world.IRubberTreeAPI;
import com.miophas.singularity_iteration.core.api.world.IWindAPI;

/**
 * Industrial Craft In Future (mio_icif) 的 API 入口
 *
 * <p>这是附属模组与 mio_icif 交互的主要接口。通过此接口可以访问：
 * <ul>
 *   <li>能源网络 API - 查询和交互 EU 电网</li>
 *   <li>发电机 API - 注册和管理自定义发电机</li>
 *   <li>机器 API - 注册和管理自定义机器</li>
 *   <li>机器构建 API - 使用 Builder 模式创建风格一致的机器</li>
 *   <li>物品 API - 访问模组物品和工具</li>
 *   <li>配方 API - 注册和查询自定义配方</li>
 *   <li>配方注册 API - 注册新的加工配方</li>
 *   <li>流体 API - 访问模组流体</li>
 *   <li>流体处理 API - 查询和管理机器流体存储</li>
 *   <li>作物 API - 访问作物杂交系统</li>
 *   <li>方块 API - 访问模组方块</li>
 *   <li>注册表 API - 查询模组注册的所有内容</li>
 *   <li>热能 API - 访问 HU 热能系统</li>
 *   <li>动能 API - 访问 KU 动能系统</li>
 *   <li>核反应堆 API - 访问反应堆元件系统</li>
 *   <li>橡胶树体系 API - 橡胶木与树脂采集逻辑</li>
 *   <li>升级插件 API - 访问机器升级系统</li>
 *   <li>能力系统 API - 访问 EU/HU/KU 能力接口和工厂方法</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 获取 API 实例
 * MioIcifAPI api = MioIcifAPI.instance();
 *
 * // 查询位置的电网状态
 * IEnergyNetAPI energyApi = api.getEnergyNetAPI();
 * NodeStats stats = energyApi.getNodeStats(world, pos);
 *
 * // 使用机器构建器创建新机器
 * IMachineBuilderAPI builder = api.getMachineBuilderAPI();
 * var machine = builder.createElectricMachineBuilder()
 *     .setName("my_machine")
 *     .setEnergyCapacity(10000)
 *     .buildAndRegister("mymod");
 * }</pre>
 */
public interface MioIcifAPI {

    /** Compatibility series for documented public contracts; independent of the mod artifact version. */
    String API_VERSION = "1.0";

    /** Runtime query (unlike an inlined constant, this identifies the installed provider). */
    default String getApiVersion() { return API_VERSION; }

    com.miophas.singularity_iteration.core.api.machine.IMultiblockAccess getMultiblockAPI();

    /**
     * 获取 API 实例
     *
     * @return MioIcifAPI 单例实例
     */
    static MioIcifAPI instance() {
        return ApiServices.get();
    }

    // ========== 核心 API ==========

    /**
     * 获取能源网络 API
     *
     * @return 能源网络 API 实例
     */
    IEnergyNetAPI getEnergyNetAPI();

    /**
     * 获取 FE↔EU 兼容 API
     *
     * <p>提供将 FE (Forge Energy) 机器接入 EU 电网的工厂方法。
     * 转换比例：1 EU = 4 FE。
     *
     * @return FE 兼容 API 实例
     */
    IFECompatAPI getFECompatAPI();

    /**
     * 获取发电机 API
     *
     * <p><b>已废弃</b>：发电机已统一为机器的一种类型。
     *
     * <h3>迁移指南</h3>
     * <ul>
     *   <li><b>创建新发电机</b>：使用 {@link #getMachineBuilderAPI()}.{@code createGeneratorBuilder()}</li>
     *   <li><b>查询发电机状态</b>：使用 {@link #getMachineAPI()}.{@code getMachineInfo(world, pos)}</li>
     *   <li><b>启停发电机</b>：使用 {@link #getMachineAPI()}.{@code forceStart/Stop(world, pos)}</li>
     *   <li><b>通过Capability查询</b>：使用 {@code IMioIcifCapabilities.GENERATOR_BLOCK}</li>
     * </ul>
     *
     * <p>示例代码：
     * <pre>{@code
     * // 旧方式（废弃）
     * IGeneratorAPI genApi = api.getGeneratorAPI();
     * IGeneratorInfo info = genApi.getGeneratorInfo(world, pos);
     *
     * // 新方式
     * IMachineAPI machineApi = api.getMachineAPI();
     * IMachineInfo info = machineApi.getMachineInfo(world, pos);
     * }</pre>
     *
     * @return 发电机 API 实例（仅保留向后兼容）
     * @deprecated 发电机已统一为机器的一种类型，请使用 {@link #getMachineAPI()} 和 {@link #getMachineBuilderAPI()}。
     *             此方法将在未来版本中移除。
     */
    @Deprecated(forRemoval = true)
    IGeneratorAPI getGeneratorAPI();

    /**
     * 获取机器 API
     *
     * @return 机器 API 实例
     */
    IMachineAPI getMachineAPI();

    /**
     * 获取机器构建 API
     *
     * @return 机器构建 API 实例
     */
    IMachineBuilderAPI getMachineBuilderAPI();

    // ========== 物品与配方 API ==========

    /**
     * 获取物品 API
     *
     * @return 物品 API 实例
     */
    IItemAPI getItemAPI();

    /**
     * 获取配方 API
     *
     * @return 配方 API 实例
     */
    IRecipeAPI getRecipeAPI();

    /**
     * 获取配方注册 API
     *
     * @return 配方注册 API 实例
     */
    IRecipeRegistrationAPI getRecipeRegistrationAPI();

    // ========== 流体 API ==========

    /**
     * 获取流体 API
     *
     * @return 流体 API 实例
     */
    IFluidAPI getFluidAPI();

    /**
     * 获取流体处理 API
     *
     * @return 流体处理 API 实例
     */
    IFluidHandlerAPI getFluidHandlerAPI();

    // ========== 特殊系统 API ==========

    /**
     * 获取作物 API
     *
     * @return 作物 API 实例
     */
    ICropAPI getCropAPI();

    /**
     * 获取方块 API
     *
     * @return 方块 API 实例
     */
    IBlockAPI getBlockAPI();

    /**
     * 获取注册表 API
     *
     * @return 注册表 API 实例
     */
    IMioIcifRegistries getRegistries();

    /**
     * 获取热能 API
     *
     * @return 热能 API 实例
     */
    IHeatAPI getHeatAPI();

    /**
     * 获取动能 API
     *
     * @return 动能 API 实例
     */
    IKineticAPI getKineticAPI();

    /**
     * 获取核反应堆 API
     *
     * @return 核反应堆 API 实例
     */
    IReactorAPI getReactorAPI();

    /**
     * 获取全局风场 API
     *
     * <p>暴露按维度维护的全局风场模拟（随机游走基础风力 + 高度三次多项式 + 天气修正），
     * 附属模组可据此实现自定义风力发电机、风力计、气象设备等。
     *
     * <p>使用示例：
     * <pre>{@code
     * IWindAPI wind = api.getWindAPI();
     * double windAt = wind.getWindAt(level, pos.getY() + 0.5);
     * }</pre>
     *
     * @return 全局风场 API 实例
     */
    IWindAPI getWindAPI();

    /**
     * 获取橡胶树体系 API
     *
     * <p>暴露橡胶树（树脂采集）系统的判定与采集逻辑，便于附属模组实现
     * 自定义树液采集器、自定义橡胶木方块，或复用统一的树脂采集流程。
     *
     * <p>使用示例：
     * <pre>{@code
     * IRubberTreeAPI rubber = api.getRubberTreeAPI();
     *
     * // 判断手中物品是否为树液采集器，并复用采集逻辑
     * if (rubber.isTreeTap(stack) && rubber.canHarvest(state)) {
     *     RubberHarvestResult result = rubber.tryHarvest(level, pos, state, player, hand);
     * }
     * }</pre>
     *
     * @return 橡胶树体系 API 实例
     */
    IRubberTreeAPI getRubberTreeAPI();

    /**
     * 获取升级插件 API
     *
     * @return 升级插件 API 实例
     */
    IUpgradeAPI getUpgradeAPI();

    /**
     * 获取能力系统 API
     *
     * <p>提供 EU/HU/KU 能量存储能力的工厂方法，让附属模组可以创建
     * 符合 mio_icif 风格的能力实现。
     *
     * <p>使用示例：
     * <pre>{@code
     * // 创建 EU 存储能力
     * IEUStorage storage = api.getCapabilities().createEUStorage(10000, 32, 32, CableTier.LV);
     *
     * // 创建电动物品能力
     * IElectricItem electricItem = api.getCapabilities().createElectricItem(100000, 512, 3, true);
     * }</pre>
     *
     * @return 能力系统 API 实例
     */
    IMioIcifCapabilities getCapabilities();

    // ========== 版本信息 ==========

    /**
     * 获取 API 版本
     *
     * @return 版本号字符串
     */
    String getVersion();

    /**
     * 获取模组 ID
     *
     * @return 模组 ID
     */
    String getModId();
}
