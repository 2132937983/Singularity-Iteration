package com.miophas.singularity_iteration.core.runtime.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry that maps controller blocks to multiblock validators.
 *
 * <p>The common module registers the built-in structures during startup (GESU, large fabricator, fluid reactor and others).
 * Addons use the same entry point to register their own controller blocks.
 * Structure discovery does not use {@code instanceof} checks on specific machine classes.
 */
public final class MultiblockControllers {

    /**
     * @param validatorFactory makes a new validator instance for each formation attempt; must not be null
     * @param name the structure name for logs and events; must not be null or blank
     */
    public record ControllerDescriptor(Supplier<? extends IMultiblockValidator> validatorFactory, String name) {
        public ControllerDescriptor {
            if (validatorFactory == null) throw new IllegalArgumentException("validator factory");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("structure name");
        }
    }

    private static final Map<Class<? extends Block>, ControllerDescriptor> CONTROLLERS = new HashMap<>();

    private MultiblockControllers() {}

    public static synchronized void register(Class<? extends Block> controllerType,
            Supplier<? extends IMultiblockValidator> validatorFactory, String name) {
        if (controllerType == null) throw new IllegalArgumentException("controller type");
        CONTROLLERS.put(controllerType, new ControllerDescriptor(validatorFactory, name));
    }

    public static synchronized @Nullable ControllerDescriptor descriptorFor(Block block) {
        return block == null ? null : CONTROLLERS.get(block.getClass());
    }

    public static synchronized boolean isRegistered(Class<? extends Block> controllerType) {
        return controllerType != null && CONTROLLERS.containsKey(controllerType);
    }
}
