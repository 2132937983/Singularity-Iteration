// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Ballistic profile lookup (0.1.8.0, public API).
 *
 * <p>Order: an {@link IBallisticWeapon} item, then the registered providers (first non-null
 * wins, in registration order). The fire-control computer then uses the profile it learned from
 * observed shots, then the built-in vanilla profiles. A provider is for items that do not
 * implement {@link IBallisticWeapon}, for example the weapons of a different mod in an addon.
 *
 * <p>Thread: register during mod setup; {@link #find} runs on the client and server threads.
 */
public final class BallisticProfiles {
    private BallisticProfiles() {}

    @FunctionalInterface
    public interface Provider {
        /** The profile for {@code stack}, or null when this provider does not know the item. */
        @Nullable
        BallisticProfile get(ItemStack stack, LivingEntity shooter, int useTicks);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /** The API profile of {@code stack}, or null when neither the item nor a provider knows it. */
    @Nullable
    public static BallisticProfile find(ItemStack stack, LivingEntity shooter, int useTicks) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof IBallisticWeapon weapon) {
            BallisticProfile p = weapon.ballistics(stack, shooter, useTicks);
            if (p != null) return p;
        }
        for (Provider provider : PROVIDERS) {
            BallisticProfile p = provider.get(stack, shooter, useTicks);
            if (p != null) return p;
        }
        return null;
    }
}
