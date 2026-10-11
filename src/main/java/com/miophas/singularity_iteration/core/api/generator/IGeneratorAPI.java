package com.miophas.singularity_iteration.core.api.generator;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;

/**
 * Generator API.
 * 
 * @deprecated A future version will merge this API into MachineAPI. Use IMachineAPI and IGeneratorBuilder instead.
 * 
 * <p>Gives an interface to register and manage custom generators. It lets you:
 * <ul>
 *   <li>Register custom generator types</li>
 *   <li>Query generator information</li>
 *   <li>Manage generator state</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * // Get the API
 * IGeneratorAPI generatorAPI = MioIcifAPI.instance().getGeneratorAPI();
 * 
 * // Register a custom generator
 * generatorAPI.registerGeneratorType("advanced_solar", AdvancedSolarGeneratorBlockEntity::new);
 * 
 * // Query the generator information at a position
 * GeneratorInfo info = generatorAPI.getGeneratorInfo(world, pos);
 * if (info != null) {
 *     long rate = info.getGenerationRate();
 *     boolean active = info.isActive();
 * }
 * }</pre>
 */
@Deprecated
public interface IGeneratorAPI {

    /**
     * Registers a custom generator type.
     * 
     * @param id the generator type ID (unique identifier)
     * @param factory the generator block entity factory
     * @deprecated Use IGeneratorBuilder to register generators
     */
    @Deprecated
    <T extends BlockEntity> void registerGeneratorType(String id, BlockEntityType<T> type);

    /**
     * Checks if there is a generator at the given position.
     * 
     * @param world the world
     * @param pos the block position
     * @return true if there is a generator at the position
     * @deprecated Use IMachineAPI.hasMachine() instead
     */
    @Deprecated
    boolean isGenerator(Level world, BlockPos pos);

    /**
     * Gets the generator information.
     * 
     * @param world the world
     * @param pos the block position
     * @return the generator information, or null if there is no generator
     * @deprecated Use IMachineAPI.getMachineInfo() instead
     */
    @Deprecated
    IGeneratorInfo getGeneratorInfo(Level world, BlockPos pos);

    /**
     * Gets all generator positions in the given world.
     * 
     * @param world the world
     * @return the set of generator positions
     * @deprecated Use IMachineAPI.getAllMachines() instead
     */
    @Deprecated
    Collection<BlockPos> getAllGenerators(Level world);

    /**
     * Gets all generator positions of the given type.
     * 
     * @param world the world
     * @param typeId the generator type ID
     * @return the set of generator positions
     * @deprecated Use IMachineAPI.getMachinesByType() instead
     */
    @Deprecated
    Collection<BlockPos> getGeneratorsByType(Level world, String typeId);

    /**
     * Starts the generator by force (it ignores the usual start conditions).
     * 
     * @param world the world
     * @param pos the block position
     * @return true if the generator started
     * @deprecated Use IMachineAPI.forceStartMachine() instead
     */
    @Deprecated
    boolean forceStart(Level world, BlockPos pos);

    /**
     * Stops the generator by force.
     * 
     * @param world the world
     * @param pos the block position
     * @return true if the generator stopped
     * @deprecated Use IMachineAPI.forceStopMachine() instead
     */
    @Deprecated
    boolean forceStop(Level world, BlockPos pos);

    /**
     * Gets the cable tier of the generator.
     * 
     * @param world the world
     * @param pos the block position
     * @return the cable tier, or null if there is none
     * @deprecated Use IMachineAPI.getEffectiveCableTier() instead
     */
    @Deprecated
    ICableTier getCableTier(Level world, BlockPos pos);
}