// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the compatibility mixins (0.1.9.1) only when their target mod is installed. The
 * mixin class name says which mod: {@code AE*} mixins need Applied Energistics 2.
 */
public class CompatMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("appeng.")) return modLoaded("ae2");
        return true;
    }

    private static boolean modLoaded(String id) {
        try {
            var list = net.neoforged.fml.loading.LoadingModList.get();
            return list != null && list.getModFileById(id) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
