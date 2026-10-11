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
 * API entry point of Industrial Craft In Future (mio_icif).
 *
 * <p>This is the main interface for addon mods to work with mio_icif. Through this interface you can access:
 * <ul>
 *   <li>Energy network API - query and use the EU grid</li>
 *   <li>Generator API - register and manage custom generators</li>
 *   <li>Machine API - register and manage custom machines</li>
 *   <li>Machine builder API - use the Builder pattern to create machines with a consistent style</li>
 *   <li>Item API - access mod items and tools</li>
 *   <li>Recipe API - register and query custom recipes</li>
 *   <li>Recipe registration API - register new processing recipes</li>
 *   <li>Fluid API - access mod fluids</li>
 *   <li>Fluid handling API - query and manage machine fluid storage</li>
 *   <li>Crop API - access the crop breeding system</li>
 *   <li>Block API - access mod blocks</li>
 *   <li>Registry API - query all content that the mod registers</li>
 *   <li>Heat API - access the HU heat system</li>
 *   <li>Kinetic API - access the KU kinetic system</li>
 *   <li>Nuclear reactor API - access the reactor component system</li>
 *   <li>Rubber tree API - rubber wood and resin harvest logic</li>
 *   <li>Upgrade API - access the machine upgrade system</li>
 *   <li>Capability API - access EU/HU/KU capability interfaces and factory methods</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * // Get the API instance
 * MioIcifAPI api = MioIcifAPI.instance();
 *
 * // Query the grid state at a position
 * IEnergyNetAPI energyApi = api.getEnergyNetAPI();
 * NodeStats stats = energyApi.getNodeStats(world, pos);
 *
 * // Use the machine builder to create a new machine
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
     * Gets the API instance.
     *
     * @return the MioIcifAPI singleton instance
     */
    static MioIcifAPI instance() {
        return ApiServices.get();
    }

    // ========== Core APIs ==========

    /**
     * Gets the energy network API.
     *
     * @return the energy network API instance
     */
    IEnergyNetAPI getEnergyNetAPI();

    /**
     * Gets the FE-to-EU compatibility API.
     *
     * <p>Gives factory methods that connect FE (Forge Energy) machines to the EU grid.
     * Conversion ratio: 1 EU = 4 FE.
     *
     * @return the FE compatibility API instance
     */
    IFECompatAPI getFECompatAPI();

    /**
     * Gets the generator API.
     *
     * <p><b>Deprecated</b>: a generator is now one type of machine.
     *
     * <h3>Migration guide</h3>
     * <ul>
     *   <li><b>Create a new generator</b>: use {@link #getMachineBuilderAPI()}.{@code createGeneratorBuilder()}</li>
     *   <li><b>Query generator state</b>: use {@link #getMachineAPI()}.{@code getMachineInfo(world, pos)}</li>
     *   <li><b>Start or stop a generator</b>: use {@link #getMachineAPI()}.{@code forceStart/Stop(world, pos)}</li>
     *   <li><b>Query through a capability</b>: use {@code IMioIcifCapabilities.GENERATOR_BLOCK}</li>
     * </ul>
     *
     * <p>Example code:
     * <pre>{@code
     * // Old method (deprecated)
     * IGeneratorAPI genApi = api.getGeneratorAPI();
     * IGeneratorInfo info = genApi.getGeneratorInfo(world, pos);
     *
     * // New method
     * IMachineAPI machineApi = api.getMachineAPI();
     * IMachineInfo info = machineApi.getMachineInfo(world, pos);
     * }</pre>
     *
     * @return the generator API instance (kept only for backward compatibility)
     * @deprecated A generator is now one type of machine. Use {@link #getMachineAPI()} and {@link #getMachineBuilderAPI()}.
     *             A future version will remove this method.
     */
    @Deprecated(forRemoval = true)
    IGeneratorAPI getGeneratorAPI();

    /**
     * Gets the machine API.
     *
     * @return the machine API instance
     */
    IMachineAPI getMachineAPI();

    /**
     * Gets the machine builder API.
     *
     * @return the machine builder API instance
     */
    IMachineBuilderAPI getMachineBuilderAPI();

    // ========== Item and recipe APIs ==========

    /**
     * Gets the item API.
     *
     * @return the item API instance
     */
    IItemAPI getItemAPI();

    /**
     * Gets the recipe API.
     *
     * @return the recipe API instance
     */
    IRecipeAPI getRecipeAPI();

    /**
     * Gets the recipe registration API.
     *
     * @return the recipe registration API instance
     */
    IRecipeRegistrationAPI getRecipeRegistrationAPI();

    // ========== Fluid APIs ==========

    /**
     * Gets the fluid API.
     *
     * @return the fluid API instance
     */
    IFluidAPI getFluidAPI();

    /**
     * Gets the fluid handling API.
     *
     * @return the fluid handling API instance
     */
    IFluidHandlerAPI getFluidHandlerAPI();

    // ========== Special system APIs ==========

    /**
     * Gets the crop API.
     *
     * @return the crop API instance
     */
    ICropAPI getCropAPI();

    /**
     * Gets the block API.
     *
     * @return the block API instance
     */
    IBlockAPI getBlockAPI();

    /**
     * Gets the registry API.
     *
     * @return the registry API instance
     */
    IMioIcifRegistries getRegistries();

    /**
     * Gets the heat API.
     *
     * @return the heat API instance
     */
    IHeatAPI getHeatAPI();

    /**
     * Gets the kinetic API.
     *
     * @return the kinetic API instance
     */
    IKineticAPI getKineticAPI();

    /**
     * Gets the nuclear reactor API.
     *
     * @return the nuclear reactor API instance
     */
    IReactorAPI getReactorAPI();

    /**
     * Gets the global wind API.
     *
     * <p>It gives access to the global wind simulation for each dimension (random-walk base wind + cubic polynomial of height + weather correction).
     * Addon mods can use it to make custom wind generators, wind meters, weather devices and similar items.
     *
     * <p>Example:
     * <pre>{@code
     * IWindAPI wind = api.getWindAPI();
     * double windAt = wind.getWindAt(level, pos.getY() + 0.5);
     * }</pre>
     *
     * @return the global wind API instance
     */
    IWindAPI getWindAPI();

    /**
     * Gets the rubber tree API.
     *
     * <p>It gives access to the check and harvest logic of the rubber tree (resin harvest) system. Addon mods can use it to make
     * custom treetaps or custom rubber wood blocks, or to use the same resin harvest flow again.
     *
     * <p>Example:
     * <pre>{@code
     * IRubberTreeAPI rubber = api.getRubberTreeAPI();
     *
     * // Check if the held item is a treetap, and use the harvest logic again
     * if (rubber.isTreeTap(stack) && rubber.canHarvest(state)) {
     *     RubberHarvestResult result = rubber.tryHarvest(level, pos, state, player, hand);
     * }
     * }</pre>
     *
     * @return the rubber tree API instance
     */
    IRubberTreeAPI getRubberTreeAPI();

    /**
     * Gets the upgrade API.
     *
     * @return the upgrade API instance
     */
    IUpgradeAPI getUpgradeAPI();

    /**
     * Gets the capability API.
     *
     * <p>Gives factory methods for EU/HU/KU energy storage capabilities, so that addon mods can create
     * capability implementations in the mio_icif style.
     *
     * <p>Example:
     * <pre>{@code
     * // Create an EU storage capability
     * IEUStorage storage = api.getCapabilities().createEUStorage(10000, 32, 32, CableTier.LV);
     *
     * // Create an electric item capability
     * IElectricItem electricItem = api.getCapabilities().createElectricItem(100000, 512, 3, true);
     * }</pre>
     *
     * @return the capability API instance
     */
    IMioIcifCapabilities getCapabilities();

    // ========== Version information ==========

    /**
     * Gets the API version.
     *
     * @return the version string
     */
    String getVersion();

    /**
     * Gets the mod ID.
     *
     * @return the mod ID
     */
    String getModId();
}
