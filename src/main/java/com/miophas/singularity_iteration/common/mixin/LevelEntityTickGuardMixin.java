// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.miophas.singularity_iteration.common.armory.WingmanTickGuard;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 0.1.10.5: an error in the tick of a Wingman, or of an entity that a Wingman made, is handled by
 * {@link WingmanTickGuard} instead of crashing the level. Errors of every other entity are thrown
 * on unchanged, so the vanilla crash report (and NeoForge's {@code removeErroringEntities}) runs
 * as before.
 */
@Mixin(Level.class)
public abstract class LevelEntityTickGuardMixin {
    @WrapOperation(method = "guardEntityTick", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void mio_icif$guardWingmen(Consumer<Object> tick, Object entity, Operation<Void> original) {
        try {
            original.call(tick, entity);
        } catch (Throwable error) {
            if (!WingmanTickGuard.handle(entity, error)) throw error;
        }
    }
}
