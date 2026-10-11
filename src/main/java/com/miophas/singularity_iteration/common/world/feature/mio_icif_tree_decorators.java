package com.miophas.singularity_iteration.common.world.feature;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the custom tree decorator types.
 */
@SuppressWarnings("null")
public class mio_icif_tree_decorators {
    // Deferred register for tree decorator types.
public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = 
        DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, Singularity_Iteration.MOD_ID);

    // 注册橡胶木装饰器类型
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<RubberWoodDecorator>> RUBBER_WOOD = 
        TREE_DECORATORS.register("rubber_wood", () -> new TreeDecoratorType<>(RubberWoodDecorator.CODEC));

    /**
     * Registers all tree decorator types.
 */
    public static void register(IEventBus eventBus) {
        TREE_DECORATORS.register(eventBus);
        Singularity_Iteration.LOGGER.info("Tree decorator types registered");
    }
}


