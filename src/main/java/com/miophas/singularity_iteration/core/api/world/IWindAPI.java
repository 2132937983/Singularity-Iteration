package com.miophas.singularity_iteration.core.api.world;

import net.minecraft.world.level.Level;

/**
 * Global wind field API.
 *
 * <p>Gives addon mods access to the mio_icif global wind field simulation (WindSim).
 * Each dimension has its own wind field. The save file keeps the wind field.
 * The base wind strength does a random walk.
 * At query time, a cubic polynomial of the height and the weather (thunderstorm x1.5, rain x1.25) convert it.
 * Then a global factor multiplies the result to give the instantaneous wind at that height (MCU).
 *
 * <p>Example:
 * <pre>{@code
 * IWindAPI wind = MioIcifAPI.instance().getWindAPI();
 *
 * // Query the wind at a height (this sets the output of wind kinetic generators and other wind machines)
 * double windAt = wind.getWindAt(level, pos.getY() + 0.5);
 *
 * // Query the base strength and direction of the wind field (no height or weather correction)
 * int base = wind.getBaseWindStrength(level);
 * int direction = wind.getWindDirection(level);
 * }</pre>
 *
 * <p>All methods work only on the server side. For a client world they always return 0.
 * Addon mods can use this API for custom wind generators, anemometers, weather forecast devices and similar.
 */
public interface IWindAPI {

    /**
     * Queries the instantaneous wind of the global wind field of a dimension at a given height.
     *
     * @param level  the server world
     * @param height the query height (usually the Y coordinate of a block or entity)
     * @return the instantaneous wind (MCU), 0 or more; 0 for a client world
     */
    double getWindAt(Level level, double height);

    /**
     * Queries the base wind strength of the wind field.
     * This is the raw random-walk value, with no height or weather correction.
     *
     * @param level the server world
     * @return the base wind strength; 0 for a client world
     */
    int getBaseWindStrength(Level level);

    /**
     * Queries the current wind direction.
     *
     * @param level the server world
     * @return the wind direction angle in degrees (0 to 359); 0 for a client world
     */
    int getWindDirection(Level level);
}
