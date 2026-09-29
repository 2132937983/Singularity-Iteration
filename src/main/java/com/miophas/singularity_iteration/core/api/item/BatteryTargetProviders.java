package com.miophas.singularity_iteration.core.api.item;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Optional equipment integrations contribute targets without becoming dependencies of battery items. */
public final class BatteryTargetProviders {
    @FunctionalInterface
    public interface Provider {
        void appendTargets(Player player, List<ItemStack> targets, ItemStack excluded);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    private BatteryTargetProviders() {}

    public static void register(Provider provider) { PROVIDERS.add(Objects.requireNonNull(provider)); }

    public static void appendTargets(Player player, List<ItemStack> targets, ItemStack excluded) {
        for (Provider provider : PROVIDERS) provider.appendTargets(player, targets, excluded);
    }
}
