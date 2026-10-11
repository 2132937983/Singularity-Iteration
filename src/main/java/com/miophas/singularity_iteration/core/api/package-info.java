/**
 * mio_icif API package.
 *
 * <p>This is the main interface for addon mods to work with mio_icif.
 * Use {@link com.miophas.singularity_iteration.core.api.MioIcifAPI} to get all API instances.
 *
 * <h2>Usage guide</h2>
 * <ul>
 *   <li>Always access the API through its interfaces. Do not depend directly on implementation classes ({@code *Impl}).</li>
 *   <li>{@link com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity}
 *       is in the {@code core.prefab.blockentity} package. To change its behavior, use the callbacks of
 *       {@link com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI}.
 *       Do not extend it directly.</li>
 *   <li>Implementation classes can change or be removed in any version. Backward compatibility is not guaranteed.</li>
 * </ul>
 *
 * <h2>Package structure</h2>
 * <ul>
 *   <li>{@code api} - core API entry point and interfaces</li>
 *   <li>{@code api.machine} - machine API</li>
 *   <li>{@code api.machine.builder} - machine builder API</li>
 *   <li>{@code api.energy} - energy network API</li>
 *   <li>{@code api.item} - item API</li>
 *   <li>{@code api.recipe} - recipe API</li>
 *   <li>{@code api.fluid} - fluid API</li>
 *   <li>{@code api.crop} - crop API</li>
 *   <li>{@code api.block} - block API (includes the rubber wood interface {@link com.miophas.singularity_iteration.core.api.block.IRubberWood})</li>
 *   <li>{@code api.world} - world system API (wind, rubber trees)</li>
 *   <li>{@code api.registry} - registry API</li>
 *   <li>{@code api.heat} - heat API</li>
 *   <li>{@code api.kinetic} - kinetic API</li>
 *   <li>{@code api.reactor} - nuclear reactor API</li>
 *   <li>{@code api.upgrade} - upgrade API</li>
 *   <li>{@code api.capability} - capability system API</li>
 *   <li>{@code api.event} - event API</li>
 *   <li>{@code api.example} - usage examples</li>
 * </ul>
 */
package com.miophas.singularity_iteration.core.api;
