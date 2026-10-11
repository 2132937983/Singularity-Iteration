package com.miophas.singularity_iteration.core.api.fluid;


import net.minecraft.world.level.material.Fluid;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Fluid API.
 *
 * <p>Use this interface to access mio_icif fluids. It lets you:
 * <ul>
 *   <li>Query the fluids of the mod.</li>
 *   <li>Get fluid properties.</li>
 *   <li>Check if a fluid is a gas.</li>
 * </ul>
 *
 * <h3>Create a custom gas fluid</h3>
 * <p>Addon developers can use {@code GasFluidSource} and {@code GasFluidFlowing} to create
 * a gas fluid. A gas fluid moves up, disappears at a height limit, and spreads horizontally:
 * <pre>{@code
 * // Define the fluid properties
 * public static final BaseFlowingFluid.Properties MY_GAS_PROPERTIES =
 *     new BaseFlowingFluid.Properties(
 *         () -> MY_GAS, () -> MY_GAS_FLOWING,
 *         () -> MY_GAS_BLOCK
 *     ).canMultiply(true).flowSpeed(4).levelDecreasePerBlock(1);
 *
 * // Register the gas fluid
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS = FLUIDS.register("my_gas",
 *     () -> new GasFluidSource(MY_GAS_PROPERTIES, 320));
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS_FLOWING = FLUIDS.register("my_gas_flowing",
 *     () -> new GasFluidFlowing(MY_GAS_PROPERTIES, 320));
 * }</pre>
 *
 * @see net.neoforged.neoforge.fluids.BaseFlowingFluid
 * @see net.neoforged.neoforge.fluids.BaseFlowingFluid
 */
public interface IFluidAPI {

    /**
     * Gets a fluid by its ID.
     *
     * @param id the fluid ID
     * @return the fluid, or null if it does not exist
     */
    Fluid getFluid(ResourceLocation id);

    /**
     * Gets the biogas fluid.
     *
     * @return the biogas fluid
     */
    Fluid getBiogas();

    /**
     * Gets the hot water fluid.
     *
     * @return the hot water fluid
     */
    Fluid getHotWater();

    /**
     * Gets the biomass fluid.
     *
     * @return the biomass fluid
     */
    Fluid getBiomass();

    /**
     * Gets the construction foam fluid.
     *
     * @return the construction foam fluid
     */
    Fluid getConstructionFoam();

    /**
     * Gets the coolant fluid.
     *
     * @return the coolant fluid
     */
    Fluid getCoolant();

    /**
     * Gets the distilled water fluid.
     *
     * @return the distilled water fluid
     */
    Fluid getDistilledWater();

    /**
     * Gets the hot coolant fluid.
     *
     * @return the hot coolant fluid
     */
    Fluid getHotCoolant();

    /**
     * Gets the pahoehoe lava fluid.
     *
     * @return the pahoehoe lava fluid
     */
    Fluid getPahoehoeLava();

    /**
     * Gets the steam fluid.
     *
     * @return the steam fluid
     */
    Fluid getSteam();

    /**
     * Gets the superheated steam fluid.
     *
     * @return the superheated steam fluid
     */
    Fluid getSuperheatedSteam();

    /**
     * Gets the UU matter fluid.
     *
     * @return the UU matter fluid
     */
    Fluid getUUMatter();

    /**
     * Gets the air fluid.
     *
     * @return the air fluid
     */
    Fluid getAir();

    /**
     * Gets the IDs of all registered fluids.
     *
     * @return the collection of fluid IDs
     */
    Collection<ResourceLocation> getAllFluidIds();

    /**
     * Checks if the fluid is a gas fluid (a fluid that moves up).
     *
     * <p>Gas fluids include the biogas, steam, superheated steam and compressed air of this mod.
     * They also include addon gas fluids that use {@code GasFluidSource} or {@code GasFluidFlowing}.
     *
     * @param fluid the fluid
     * @return true if the fluid is a gas fluid
     */
    boolean isGasFluid(Fluid fluid);
}