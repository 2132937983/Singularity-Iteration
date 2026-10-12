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
 * <p><b>EN:</b> API entry point of Singularity Iteration (mod id {@code mio_icif}). Addon mods use this
 * interface to get the sub-APIs:
 * <ul>
 *   <li>Energy network, FE compatibility, capabilities (EU / HU / KU storage factories).</li>
 *   <li>Machines and the machine builder (consistent machine style); generators are machines.</li>
 *   <li>Items, recipes and recipe registration; fluids and fluid handling.</li>
 *   <li>Crops (breeding system), blocks, registries, heat (HU), kinetic energy (KU),
 *       nuclear reactor components, wind, rubber trees, upgrades.</li>
 * </ul>
 * Get the instance with {@link #instance()}. Call the API on the server thread unless a
 * sub-API gives a different rule.
 *
 * <p><b>ZH:</b> 奇点迭代（模组 ID {@code mio_icif}）的 API 入口。附属模组通过本接口获取各子 API：
 * <ul>
 *   <li>能源网络、FE 兼容、能力（EU / HU / KU 存储工厂）。</li>
 *   <li>机器与机器构建器（统一的机器风格）；发电机也是机器。</li>
 *   <li>物品、配方与配方注册；流体与流体处理。</li>
 *   <li>作物（杂交系统）、方块、注册表、热能（HU）、动能（KU）、核反应堆组件、风力、橡胶树、升级。</li>
 * </ul>
 * 用 {@link #instance()} 获取实例。除非子 API 另有规定，请在服务器线程上调用。
 *
 * <p><b>JA:</b> 奇点迭代（MOD ID {@code mio_icif}）の API 入口。アドオン MOD はこのインターフェースから各サブ API を得る：
 * <ul>
 *   <li>エネルギーネットワーク、FE 互換、ケイパビリティ（EU / HU / KU ストレージのファクトリ）。</li>
 *   <li>機械と機械ビルダー（統一された機械スタイル）。発電機も機械の一種。</li>
 *   <li>アイテム、レシピとレシピ登録、液体と液体処理。</li>
 *   <li>作物（交配システム）、ブロック、レジストリ、熱（HU）、運動エネルギー（KU）、原子炉部品、風、ゴムの木、アップグレード。</li>
 * </ul>
 * インスタンスは {@link #instance()} で得る。サブ API に別の規則がない限り、サーバースレッドで呼ぶ。
 *
 * <p>Example / 示例 / 例:
 * <pre>{@code
 * MioIcifAPI api = MioIcifAPI.instance();
 * IEnergyNetAPI energyApi = api.getEnergyNetAPI();
 * NodeStats stats = energyApi.getNodeStats(world, pos);
 *
 * IMachineBuilderAPI builder = api.getMachineBuilderAPI();
 * var machine = builder.createElectricMachineBuilder()
 *     .setName("my_machine")
 *     .setEnergyCapacity(10000)
 *     .buildAndRegister("mymod");
 * }</pre>
 */
public interface MioIcifAPI {

    /**
     * <p><b>EN:</b> Compatibility series of the documented public contracts. It does not follow the mod version.
     * <p><b>ZH:</b> 已文档化公共约定的兼容系列号，与模组版本号无关。
     * <p><b>JA:</b> 文書化された公開契約の互換シリーズ番号。MOD のバージョンとは独立している。
     */
    String API_VERSION = "1.0";

    /**
     * <p><b>EN:</b> Runtime query of the API version. Unlike the inlined constant, it identifies the installed provider.
     * <p><b>ZH:</b> 运行时查询 API 版本。与编译期内联的常量不同，它反映已安装的实现。
     * <p><b>JA:</b> 実行時に API バージョンを問い合わせる。インライン化される定数と違い、導入済みの実装を示す。
     */
    default String getApiVersion() { return API_VERSION; }

    /**
     * <p><b>EN:</b> Returns the multiblock access API.
     * <p><b>ZH:</b> 返回多方块结构访问 API。
     * <p><b>JA:</b> マルチブロック構造アクセス API を返す。
     */
    com.miophas.singularity_iteration.core.api.machine.IMultiblockAccess getMultiblockAPI();

    /**
     * <p><b>EN:</b> Returns the API instance (singleton).
     * <p><b>ZH:</b> 返回 API 实例（单例）。
     * <p><b>JA:</b> API インスタンス（シングルトン）を返す。
     *
     * @return the MioIcifAPI singleton instance
     */
    static MioIcifAPI instance() {
        return ApiServices.get();
    }

    // ========== Core APIs ==========

    /**
     * <p><b>EN:</b> Returns the energy network API (EU grid queries and use).
     * <p><b>ZH:</b> 返回能源网络 API（查询与使用 EU 电网）。
     * <p><b>JA:</b> エネルギーネットワーク API（EU 電力網の照会と利用）を返す。
     *
     * @return the energy network API instance
     */
    IEnergyNetAPI getEnergyNetAPI();

    /**
     * <p><b>EN:</b> Returns the FE compatibility API. It gives factory methods that connect FE
     * (Forge Energy) machines to the EU grid. Conversion ratio: 1 EU = 4 FE.
     * <p><b>ZH:</b> 返回 FE 兼容 API，提供把 FE（Forge Energy）机器接入 EU 电网的工厂方法。换算比例：1 EU = 4 FE。
     * <p><b>JA:</b> FE 互換 API を返す。FE（Forge Energy）の機械を EU 電力網へつなぐファクトリメソッドを提供する。換算比：1 EU = 4 FE。
     *
     * @return the FE compatibility API instance
     */
    IFECompatAPI getFECompatAPI();

    /**
     * <p><b>EN:</b> Returns the generator API. Deprecated: a generator is now one type of machine.
     * Migration: create a generator with {@link #getMachineBuilderAPI()}{@code .createGeneratorBuilder()};
     * query it with {@link #getMachineAPI()}{@code .getMachineInfo(world, pos)}; start or stop it with
     * {@code forceStart/Stop(world, pos)}; or use the capability {@code IMioIcifCapabilities.GENERATOR_BLOCK}.
     * <p><b>ZH:</b> 返回发电机 API。已弃用：发电机现在是机器的一种。迁移方法：用 {@link #getMachineBuilderAPI()}{@code .createGeneratorBuilder()}
     * 创建；用 {@link #getMachineAPI()}{@code .getMachineInfo(world, pos)} 查询；用 {@code forceStart/Stop(world, pos)} 启停；
     * 或使用能力 {@code IMioIcifCapabilities.GENERATOR_BLOCK}。
     * <p><b>JA:</b> 発電機 API を返す。非推奨：発電機は現在、機械の一種である。移行方法：{@link #getMachineBuilderAPI()}{@code .createGeneratorBuilder()}
     * で作成し、{@link #getMachineAPI()}{@code .getMachineInfo(world, pos)} で照会し、{@code forceStart/Stop(world, pos)} で起動・停止する。
     * またはケイパビリティ {@code IMioIcifCapabilities.GENERATOR_BLOCK} を使う。
     *
     * <pre>{@code
     * // Old (deprecated) / 旧 / 旧方式
     * IGeneratorInfo info = api.getGeneratorAPI().getGeneratorInfo(world, pos);
     * // New / 新 / 新方式
     * IMachineInfo info = api.getMachineAPI().getMachineInfo(world, pos);
     * }</pre>
     *
     * @return the generator API instance (kept only for backward compatibility)
     * @deprecated A generator is now one type of machine. Use {@link #getMachineAPI()} and {@link #getMachineBuilderAPI()}.
     *             A future version will remove this method.
     */
    @Deprecated(forRemoval = true)
    IGeneratorAPI getGeneratorAPI();

    /**
     * <p><b>EN:</b> Returns the machine API (machine information, start and stop).
     * <p><b>ZH:</b> 返回机器 API（机器信息、启动与停止）。
     * <p><b>JA:</b> 機械 API（機械情報、起動と停止）を返す。
     *
     * @return the machine API instance
     */
    IMachineAPI getMachineAPI();

    /**
     * <p><b>EN:</b> Returns the machine builder API (builder pattern for machines in a consistent style).
     * <p><b>ZH:</b> 返回机器构建器 API（以构建器模式创建风格统一的机器）。
     * <p><b>JA:</b> 機械ビルダー API（ビルダーパターンで統一スタイルの機械を作る）を返す。
     *
     * @return the machine builder API instance
     */
    IMachineBuilderAPI getMachineBuilderAPI();

    // ========== Item and recipe APIs ==========

    /**
     * <p><b>EN:</b> Returns the item API (mod items and tools).
     * <p><b>ZH:</b> 返回物品 API（模组物品与工具）。
     * <p><b>JA:</b> アイテム API（MOD のアイテムと道具）を返す。
     *
     * @return the item API instance
     */
    IItemAPI getItemAPI();

    /**
     * <p><b>EN:</b> Returns the recipe API (query recipes).
     * <p><b>ZH:</b> 返回配方 API（查询配方）。
     * <p><b>JA:</b> レシピ API（レシピの照会）を返す。
     *
     * @return the recipe API instance
     */
    IRecipeAPI getRecipeAPI();

    /**
     * <p><b>EN:</b> Returns the recipe registration API (add processing recipes).
     * <p><b>ZH:</b> 返回配方注册 API（添加加工配方）。
     * <p><b>JA:</b> レシピ登録 API（加工レシピの追加）を返す。
     *
     * @return the recipe registration API instance
     */
    IRecipeRegistrationAPI getRecipeRegistrationAPI();

    // ========== Fluid APIs ==========

    /**
     * <p><b>EN:</b> Returns the fluid API (mod fluids).
     * <p><b>ZH:</b> 返回流体 API（模组流体）。
     * <p><b>JA:</b> 液体 API（MOD の液体）を返す。
     *
     * @return the fluid API instance
     */
    IFluidAPI getFluidAPI();

    /**
     * <p><b>EN:</b> Returns the fluid handling API (query and manage machine fluid storage).
     * <p><b>ZH:</b> 返回流体处理 API（查询与管理机器流体存储）。
     * <p><b>JA:</b> 液体処理 API（機械の液体貯蔵の照会と管理）を返す。
     *
     * @return the fluid handling API instance
     */
    IFluidHandlerAPI getFluidHandlerAPI();

    // ========== Special system APIs ==========

    /**
     * <p><b>EN:</b> Returns the crop API (plant registry, base seeds, crossing).
     * <p><b>ZH:</b> 返回作物 API（植物注册、基础种子、杂交）。
     * <p><b>JA:</b> 作物 API（植物の登録、基本種子、交配）を返す。
     *
     * @return the crop API instance
     */
    ICropAPI getCropAPI();

    /**
     * <p><b>EN:</b> Returns the block API (mod blocks).
     * <p><b>ZH:</b> 返回方块 API（模组方块）。
     * <p><b>JA:</b> ブロック API（MOD のブロック）を返す。
     *
     * @return the block API instance
     */
    IBlockAPI getBlockAPI();

    /**
     * <p><b>EN:</b> Returns the registry API (all content that the mod registers).
     * <p><b>ZH:</b> 返回注册表 API（模组注册的全部内容）。
     * <p><b>JA:</b> レジストリ API（MOD が登録するすべてのコンテンツ）を返す。
     *
     * @return the registry API instance
     */
    IMioIcifRegistries getRegistries();

    /**
     * <p><b>EN:</b> Returns the heat API (HU heat system).
     * <p><b>ZH:</b> 返回热能 API（HU 热能系统）。
     * <p><b>JA:</b> 熱 API（HU 熱システム）を返す。
     *
     * @return the heat API instance
     */
    IHeatAPI getHeatAPI();

    /**
     * <p><b>EN:</b> Returns the kinetic API (KU kinetic system).
     * <p><b>ZH:</b> 返回动能 API（KU 动能系统）。
     * <p><b>JA:</b> 運動エネルギー API（KU システム）を返す。
     *
     * @return the kinetic API instance
     */
    IKineticAPI getKineticAPI();

    /**
     * <p><b>EN:</b> Returns the nuclear reactor API (reactor component system).
     * <p><b>ZH:</b> 返回核反应堆 API（反应堆组件系统）。
     * <p><b>JA:</b> 原子炉 API（原子炉部品システム）を返す。
     *
     * @return the nuclear reactor API instance
     */
    IReactorAPI getReactorAPI();

    /**
     * <p><b>EN:</b> Returns the global wind API. The wind of each dimension is a random-walk base
     * wind, a cubic polynomial of the height and a weather correction. Use it for custom wind
     * generators, wind meters and weather devices.
     * <p><b>ZH:</b> 返回全局风力 API。每个维度的风力 = 随机游走的基础风 + 高度的三次多项式 + 天气修正。可用于自定义风力发电机、风速计与气象设备。
     * <p><b>JA:</b> 全体の風 API を返す。各ディメンションの風はランダムウォークの基本風、高さの 3 次多項式、天候補正の合計。
     * 独自の風力発電機、風速計、気象装置に使える。
     *
     * <pre>{@code
     * double windAt = api.getWindAPI().getWindAt(level, pos.getY() + 0.5);
     * }</pre>
     *
     * @return the global wind API instance
     */
    IWindAPI getWindAPI();

    /**
     * <p><b>EN:</b> Returns the rubber tree API: the check and harvest logic of resin. Use it for
     * custom treetaps or rubber wood blocks, or to use the same harvest flow again.
     * <p><b>ZH:</b> 返回橡胶树 API：树脂的检测与采集逻辑。可用于自定义树脂提取器或橡胶木方块，或复用同一采集流程。
     * <p><b>JA:</b> ゴムの木 API を返す：樹脂の判定と採取ロジック。独自のツリータップやゴム木ブロック、同じ採取処理の再利用に使える。
     *
     * <pre>{@code
     * IRubberTreeAPI rubber = api.getRubberTreeAPI();
     * if (rubber.isTreeTap(stack) && rubber.canHarvest(state)) {
     *     RubberHarvestResult result = rubber.tryHarvest(level, pos, state, player, hand);
     * }
     * }</pre>
     *
     * @return the rubber tree API instance
     */
    IRubberTreeAPI getRubberTreeAPI();

    /**
     * <p><b>EN:</b> Returns the upgrade API (machine upgrade system).
     * <p><b>ZH:</b> 返回升级 API（机器升级系统）。
     * <p><b>JA:</b> アップグレード API（機械のアップグレードシステム）を返す。
     *
     * @return the upgrade API instance
     */
    IUpgradeAPI getUpgradeAPI();

    /**
     * <p><b>EN:</b> Returns the capability API: factory methods for EU / HU / KU storage capabilities
     * in the mio_icif style.
     * <p><b>ZH:</b> 返回能力 API：以 mio_icif 风格创建 EU / HU / KU 存储能力的工厂方法。
     * <p><b>JA:</b> ケイパビリティ API を返す：mio_icif 方式の EU / HU / KU ストレージ・ケイパビリティを作るファクトリメソッド。
     *
     * <pre>{@code
     * IEUStorage storage = api.getCapabilities().createEUStorage(10000, 32, 32, CableTier.LV);
     * IElectricItem electricItem = api.getCapabilities().createElectricItem(100000, 512, 3, true);
     * }</pre>
     *
     * @return the capability API instance
     */
    IMioIcifCapabilities getCapabilities();

    // ========== Version information ==========

    /**
     * <p><b>EN:</b> Returns the version string of the installed mod.
     * <p><b>ZH:</b> 返回已安装模组的版本字符串。
     * <p><b>JA:</b> 導入済み MOD のバージョン文字列を返す。
     *
     * @return the version string
     */
    String getVersion();

    /**
     * <p><b>EN:</b> Returns the mod id ({@code mio_icif}).
     * <p><b>ZH:</b> 返回模组 ID（{@code mio_icif}）。
     * <p><b>JA:</b> MOD ID（{@code mio_icif}）を返す。
     *
     * @return the mod ID
     */
    String getModId();
}
