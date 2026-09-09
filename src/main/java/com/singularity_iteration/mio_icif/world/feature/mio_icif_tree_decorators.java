package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义树装饰器类型注册? */
@SuppressWarnings("null")
public class mio_icif_tree_decorators {
    // 创建延迟注册�
public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = 
        DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, Singularity_Iteration.MOD_ID);

    // 注册橡胶木装饰器类型
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<RubberWoodDecorator>> RUBBER_WOOD = 
        TREE_DECORATORS.register("rubber_wood", () -> new TreeDecoratorType<>(RubberWoodDecorator.CODEC));

    /**
     * 注册所有树装饰器类�
 */
    public static void register(IEventBus eventBus) {
        TREE_DECORATORS.register(eventBus);
        Singularity_Iteration.LOGGER.info("Tree decorator types registered");
    }
}


