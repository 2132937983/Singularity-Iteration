package com.singularity_iteration.mio_icif.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 修改 Container 的最大堆叠数限制
 * 允许容器中的物品堆叠数超�?99
 */
@Mixin(Container.class)
public interface ContainerMaxStackMixin {
    
    @ModifyReturnValue(
        method = "getMaxStackSize()I",
        at = @At("RETURN")
    )
    default int modifyMaxStackSize(int original) {
        // 如果原始值是 99（默认容器限制），则改为 9999
        // 否则保持原值不变（允许物品自定义的堆叠数生效）
        if (original == 99) {
            return 9999;
        }
        return original;
    }
}


