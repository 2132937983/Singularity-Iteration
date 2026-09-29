package com.miophas.singularity_iteration.core.runtime.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 控制方块到多方块验证器的注册表。
 *
 * <p>common 在启动装配时注册内置结构（GESU、大型制造机、流体反应堆等），
 * 附属用同一入口注册自己的控制方块。结构发现不再对具体机器类做
 * {@code instanceof} 判断。
 */
public final class MultiblockControllers {

    /**
     * @param validatorFactory 每次尝试成新建一个验证器实例
     * @param name 日志与事件使用的结构名
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
