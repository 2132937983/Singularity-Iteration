// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin.compat;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import com.miophas.singularity_iteration.common.item.EnergyStackDiff;
import java.util.List;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 crafting patterns accept charged and empty electric items alike (0.1.9.1).
 *
 * <p>An AE2 crafting pattern stores its inputs as exact keys, item components included. The
 * stored charge of an SI battery, energy crystal or electric tool is a component, so a pattern
 * encoded with an empty battery did not take a charged one (and the other way round). With
 * substitutions on, AE2 also assembles the recipe with the candidate and compares the result
 * exactly; the charge-carrying recipes put the input charge on the result, so that test failed
 * too. A candidate that differs from the pattern input only in its charge is now valid. AE2 then
 * crafts the encoded output, so the crafting job gets exactly the item it waits for.
 */
@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class AECraftingPatternMixin {
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void mio_icif$chargeAgnostic(int slot, AEItemKey key, Level level, CallbackInfoReturnable<Boolean> cir) {
        // the public accessor, not a shadow: a changed field name in a later AE2 must not break loading
        List<GenericStack> sparseInputs = ((AECraftingPattern) (Object) this).getSparseInputs();
        if (key == null || slot < 0 || slot >= sparseInputs.size()) return;
        GenericStack want = sparseInputs.get(slot);
        if (want == null || !(want.what() instanceof AEItemKey wanted) || wanted.getItem() != key.getItem()) return;
        if (EnergyStackDiff.sameIgnoringCharge(wanted.toStack(), key.toStack())) cir.setReturnValue(true);
    }
}
